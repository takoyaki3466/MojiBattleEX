package jp.takoa.mojibattle.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerMapping;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.math.Vector2;
import jp.takoa.mojibattle.domain.PlayerId;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Joy-Conの検出、P1〜P4割り当て、横持ち入力のPlayerCommand変換を担当する。
 *
 * <p>SDLのmini-gamepadマッピングを使用するため、左右どちらのJoy-Conも横持ち時の
 * スティックがleft X/Yへ、SL/SRがleft/right shoulderへ正規化される。</p>
 */
public final class JoyConInputManager {
    static final float DEAD_ZONE = .2f;

    private final DeviceProvider provider;
    private final boolean diagnostics = Boolean.getBoolean("mojibattle.controllerDiagnostics");
    private final EnumMap<PlayerId, Assignment> assignments = new EnumMap<>(PlayerId.class);
    private final Map<String, PlayerId> preferredSlots = new HashMap<>();
    private Set<String> connectedDeviceIds = Set.of();
    private String inventorySnapshot = "";
    private List<Device> unassigned = List.of();

    public JoyConInputManager() {
        this(new GdxDeviceProvider());
    }

    JoyConInputManager(DeviceProvider provider) {
        this.provider = provider;

        for (PlayerId player : PlayerId.values()) {
            assignments.put(player, new Assignment());
        }
    }

    /**
     * 現在の接続一覧を反映し、既存割り当てを可能な限り維持する。
     */
    public void refresh() {
        List<Device> allDevices = provider.devices().stream().filter(Device::connected).limit(32).toList();
        logConnectionChanges(allDevices);

        List<Device> candidates = allDevices.stream().filter(device -> sideOf(device.name()) != null).limit(16).toList();
        Set<String> usedIds = new HashSet<>();

        for (Assignment assignment : assignments.values()) {
            Device same = findById(candidates, assignment.deviceId, usedIds);
            assignment.device = same;

            if (same != null) {
                usedIds.add(same.id());

            } else {
                assignment.clearButtons();
            }
        }

        List<Device> remaining = candidates.stream().filter(device -> !usedIds.contains(device.id())).toList();
        for (Device device : remaining) {
            PlayerId slot = preferredSlots.get(device.id());
            if (slot != null && assignments.get(slot).device == null) {
                assign(slot, device);
                usedIds.add(device.id());
            }
        }
        for (Device device : remaining) {
            if (usedIds.contains(device.id())) {
                continue;
            }

            PlayerId slot = matchingDisconnectedSlot(device);

            if (slot == null) {
                slot = replaceableSlot();
            }

            if (slot == null) {
                break;
            }

            assign(slot, device);
            usedIds.add(device.id());
        }

        unassigned = candidates.stream().filter(device -> !usedIds.contains(device.id())).toList();
        logInventory(allDevices);
    }

    /**
     * 現在押されているボタンを基準状態にし、画面遷移直後の誤発動を防ぐ。
     */
    public void resetInputState() {
        refresh();
        assignments.values().forEach(Assignment::primeButtons);
    }

    /**
     * 指定プレイヤーのJoy-Con入力を一度だけ消費する。未接続時は常にidleを返す。
     */
    public PlayerCommand consume(PlayerId player) {
        Assignment assignment = assignments.get(player);
        if (assignment == null || assignment.device == null || !assignment.device.connected()) {
            if (assignment != null) {
                assignment.device = null;
                assignment.clearButtons();
            }
            return PlayerCommand.idle();
        }

        Device device = assignment.device;

        if (diagnostics) {
            logRawInput(player, assignment, device);
        }

        Vector2 movement = movement(device);

        boolean skillNow = mainSkillPressed(device);
        boolean supportSkillNow = supportSkillPressed(device);
        boolean ultimateNow = slPressed(device);
        boolean supportUltimateNow = srPressed(device);

        ButtonPhase skillPhase = assignment.skill.update(skillNow);
        ButtonPhase supportSkillPhase = assignment.supportSkill.update(supportSkillNow);
        ButtonPhase ultimatePhase = assignment.ultimate.update(ultimateNow);
        ButtonPhase supportUltimatePhase = assignment.supportUltimate.update(supportUltimateNow);

        boolean movementActive = movement.len2() > .001f;

        if (diagnostics && Gdx.app != null && (movementActive != assignment.movementActive || skillPhase.changed() || supportSkillPhase.changed() || ultimatePhase.changed() || supportUltimatePhase.changed())) {
            Gdx.app.log("JoyConSemantic", "[JoyConInputManager] " + player.label() + " movement=" + movement + ", mainSkill=" + skillPhase + ", mainUltimate=" + ultimatePhase + ", supportSkill=" + supportSkillPhase + ", supportUltimate=" + supportUltimatePhase);
        }

        assignment.movementActive = movementActive;

        PlayerCommand command = new PlayerCommand(movement, skillPhase.pressed(), ultimatePhase.pressed(), supportSkillPhase.pressed(), supportUltimatePhase.pressed());

        if (diagnostics && Gdx.app != null && (movementActive || command.useSkill() || command.useUltimate() || command.useSupportSkill() || command.useSupportUltimate())) {
            Gdx.app.log("JoyConCommand", "[PlayerCommand] " + player.label() + " " + command);
        }
        return command;
    }

    /**
     * UI表示用に現在の入力を読み取る。ボタンの押下エッジは更新せず、戦闘入力を消費しない。
     */
    public List<JoyConLiveState> liveStates() {
        refresh();
        List<JoyConLiveState> result = new ArrayList<>(PlayerId.values().length);

        for (PlayerId player : PlayerId.values()) {
            Assignment assignment = assignments.get(player);
            Device device = assignment.device;

            if (device == null || !device.connected()) {
                result.add(new JoyConLiveState(player, assignment.deviceId, assignment.name, assignment.side, false, new Vector2(), false, false, false, false));
                continue;
            }


            result.add(new JoyConLiveState(player, assignment.deviceId, assignment.name, assignment.side, true, movement(device), mainSkillPressed(device), slPressed(device), supportSkillPressed(device), srPressed(device)));
        }
        return List.copyOf(result);
    }

    public int unassignedCount() {
        refresh();
        return unassigned.size();
    }

    /**
     * 設定画面から、指定プレイヤーの割り当てを次の接続済みJoy-Conへ切り替える。
     */
    public void cycleAssignment(PlayerId player) {
        refresh();
        List<Device> devices = provider.devices().stream().filter(Device::connected).filter(device -> sideOf(device.name()) != null).limit(16).toList();

        if (devices.isEmpty()) {
            return;
        }

        Assignment targetAssignment = assignments.get(player);
        int currentIndex = targetAssignment.device == null ? -1 : devices.indexOf(targetAssignment.device);

        Device next = devices.get((currentIndex + 1) % devices.size());

        if (next == targetAssignment.device) {
            return;
        }

        PlayerId previousOwner = ownerOf(next);
        Device previousDevice = targetAssignment.device;

        if (previousOwner != null) {
            Assignment previousAssignment = assignments.get(previousOwner);
            if (previousDevice == null) {
                previousAssignment.forgetDevice();

            } else {
                assign(previousOwner, previousDevice);
            }
        }
        assign(player, next);
        refresh();
    }

    private PlayerId ownerOf(Device device) {
        for (PlayerId player : PlayerId.values()) {
            if (assignments.get(player).device == device) {
                return player;
            }
        }
        return null;
    }

    private void assign(PlayerId player, Device device) {
        Assignment assignment = assignments.get(player);

        assignment.device = device;
        assignment.deviceId = device.id();
        assignment.name = device.name();
        assignment.side = sideOf(device.name());

        preferredSlots.put(device.id(), player);
        assignment.primeButtons();

        if (Gdx.app != null) {
            Gdx.app.log("JoyCon", "Assigned " + assignment.name + " (" + assignment.side + ") to " + player.label() + (diagnostics ? "; buttons=" + device.minButtonIndex() + ".." + device.maxButtonIndex() + ", axes=" + device.axisCount() + ", mapping=" + device.mapping() : ""));
        }
    }

    private static void logRawInput(PlayerId player, Assignment assignment, Device device) {
        StringBuilder snapshot = new StringBuilder("axes[");
        for (int axis = 0; axis < device.axisCount(); axis++) {
            if (axis > 0) {
                snapshot.append(',');
            }
            snapshot.append(axis).append('=').append(String.format(Locale.ROOT, "%.3f", device.axis(axis)));
        }

        snapshot.append("] buttons[");

        for (int button = device.minButtonIndex(); button <= device.maxButtonIndex(); button++) {
            if (button > device.minButtonIndex()) {
                snapshot.append(',');
            }
            snapshot.append(button).append('=').append(device.button(button) ? '1' : '0');
        }

        snapshot.append("] semantic[SL=").append(slPressed(device) ? '1' : '0').append(",SR=").append(srPressed(device) ? '1' : '0').append(']');
        String value = snapshot.toString();

        if (!value.equals(assignment.rawSnapshot) && Gdx.app != null) {
            Gdx.app.log("JoyConRaw", "[gdx-controllers] " + player.label() + " id=" + device.id() + " name=" + device.name() + " " + value);
            assignment.rawSnapshot = value;
        }
    }

    private void logConnectionChanges(List<Device> devices) {
        if (!diagnostics || Gdx.app == null) {
            return;
        }

        Set<String> now = devices.stream().map(Device::id).collect(java.util.stream.Collectors.toSet());
        for (Device device : devices) {
            if (!connectedDeviceIds.contains(device.id())) {
                Gdx.app.log("JoyConDevice", "[gdx-controllers] connected id=" + device.id() + ", name=" + device.name() + ", joyCon=" + sideOf(device.name()) + ", axes=" + device.axisCount() + ", buttons=" + device.minButtonIndex() + ".." + device.maxButtonIndex() + ", mapping=" + device.mapping());
            }
        }

        for (String id : connectedDeviceIds) {
            if (!now.contains(id)) {
                Gdx.app.log("JoyConDevice", "[gdx-controllers] disconnected id=" + id);
            }
        }

        connectedDeviceIds = Set.copyOf(now);
    }

    private void logInventory(List<Device> devices) {
        if (!diagnostics || Gdx.app == null) {
            return;
        }

        String value = devices.stream().map(device -> device.id() + "=" + device.name() + "/" + sideOf(device.name()) + "/assigned=" + ownerOf(device)).toList().toString();

        if (!value.equals(inventorySnapshot)) {
            Gdx.app.log("JoyConInventory", "[gdx-controllers] count=" + devices.size() + " " + value);
            inventorySnapshot = value;
        }
    }

    private PlayerId matchingDisconnectedSlot(Device device) {
        JoyConSide side = sideOf(device.name());

        for (PlayerId player : PlayerId.values()) {
            Assignment assignment = assignments.get(player);
            if (assignment.device == null && assignment.side == side && assignment.name.equals(device.name())) {
                return player;
            }
        }

        return null;
    }

    private PlayerId replaceableSlot() {
        for (PlayerId player : PlayerId.values()) {
            Assignment assignment = assignments.get(player);
            if (assignment.device == null && assignment.deviceId.isEmpty()) {
                return player;
            }
        }

        for (PlayerId player : PlayerId.values()) {
            if (assignments.get(player).device == null) {
                return player;
            }
        }

        return null;
    }

    private static Device findById(List<Device> devices, String id, Set<String> usedIds) {
        if (id.isEmpty()) {
            return null;
        }

        for (Device device : devices) {
            if (!usedIds.contains(device.id()) && device.id().equals(id)) {
                return device;
            }
        }

        return null;
    }

    static JoyConSide sideOf(String name) {
        if (name == null) {
            return null;
        }

        String normalized = name.toLowerCase(Locale.ROOT).replace("_", " ");

        if ((!normalized.contains("joy-con") && !normalized.contains("joycon")) || normalized.contains("joy-con 2") || normalized.contains("joycon 2") || normalized.contains("l/r") || normalized.contains("pair")) {
            return null;
        }

        if (normalized.contains("(l)") || normalized.matches(".*joy-?con[- (]+left.*")) {
            return JoyConSide.LEFT;
        }

        if (normalized.contains("(r)") || normalized.matches(".*joy-?con[- (]+right.*")) {
            return JoyConSide.RIGHT;
        }

        return null;
    }

    static Vector2 movement(Device device) {
        float x = device.axis(device.mapping().axisLeftX());
        float y = -device.axis(device.mapping().axisLeftY());

        float length = (float) Math.sqrt(x * x + y * y);

        if (length <= DEAD_ZONE) {
            return new Vector2();
        }

        float scaledLength = Math.min(1f, (length - DEAD_ZONE) / (1f - DEAD_ZONE));

        return new Vector2(x / length * scaledLength, y / length * scaledLength);
    }

    private static boolean mainSkillPressed(Device device) {
        Mapping mapping = device.mapping();
        return button(device, mapping.buttonA()) || button(device, mapping.buttonX()) || button(device, mapping.buttonDpadDown()) || button(device, mapping.buttonDpadUp());
    }

    private static boolean supportSkillPressed(Device device) {
        Mapping mapping = device.mapping();
        return button(device, mapping.buttonB()) || button(device, mapping.buttonY()) || button(device, mapping.buttonDpadRight()) || button(device, mapping.buttonDpadLeft());
    }

    private static boolean slPressed(Device device) {
        return button(device, device.mapping().buttonL1());
    }

    private static boolean srPressed(Device device) {
        return button(device, device.mapping().buttonR1());
    }

    private static boolean button(Device device, int id) {
        return id >= 0 && device.button(id);
    }

    interface DeviceProvider {
        List<Device> devices();
    }

    interface Device {
        String id();

        String name();

        JoyConSide side();

        boolean connected();

        boolean button(int id);

        float axis(int id);

        int minButtonIndex();

        int maxButtonIndex();

        int axisCount();

        Mapping mapping();
    }

    record Mapping(int axisLeftX, int axisLeftY, int buttonA, int buttonB, int buttonX, int buttonY, int buttonL1,
                   int buttonR1, int buttonDpadUp, int buttonDpadDown, int buttonDpadLeft, int buttonDpadRight) {
    }

    private static final class Assignment {
        private Device device;
        private String deviceId = "";
        private String name = "";
        private JoyConSide side;

        private final ButtonEdge skill = new ButtonEdge();
        private final ButtonEdge supportSkill = new ButtonEdge();
        private final ButtonEdge ultimate = new ButtonEdge();
        private final ButtonEdge supportUltimate = new ButtonEdge();

        private boolean movementActive;
        private String rawSnapshot = "";

        private void primeButtons() {
            if (device == null) {
                clearButtons();
                return;
            }

            skill.prime(mainSkillPressed(device));
            supportSkill.prime(supportSkillPressed(device));
            ultimate.prime(slPressed(device));
            supportUltimate.prime(srPressed(device));
        }

        private void clearButtons() {
            skill.clear();
            supportSkill.clear();
            ultimate.clear();
            supportUltimate.clear();

            movementActive = false;
            rawSnapshot = "";
        }

        private void forgetDevice() {
            device = null;
            deviceId = "";
            name = "";
            side = null;
            clearButtons();
        }
    }

    enum ButtonPhase {
        UP(false, false),
        PRESSED(true, true),
        HELD(true, false),
        RELEASED(false, false);

        private final boolean held;
        private final boolean pressed;

        ButtonPhase(boolean held, boolean pressed) {
            this.held = held;
            this.pressed = pressed;
        }

        boolean held() {
            return held;
        }

        boolean pressed() {
            return pressed;
        }

        boolean changed() {
            return this == PRESSED || this == RELEASED;
        }
    }

    private static final class ButtonEdge {
        private boolean held;

        private ButtonPhase update(boolean now) {
            ButtonPhase phase = now ? (held ? ButtonPhase.HELD : ButtonPhase.PRESSED) : (held ? ButtonPhase.RELEASED : ButtonPhase.UP);
            held = now;
            return phase;
        }

        private void prime(boolean now) {
            held = now;
        }

        private void clear() {
            held = false;
        }
    }

    private static final class GdxDeviceProvider implements DeviceProvider {
        private final IdentityHashMap<Controller, GdxDevice> wrappers = new IdentityHashMap<>();
        private boolean unavailableLogged;

        @Override
        public List<Device> devices() {
            try {
                List<Device> result = new ArrayList<>();

                for (Controller controller : Controllers.getControllers()) {
                    if (controller.isConnected()) {
                        result.add(wrappers.computeIfAbsent(controller, GdxDevice::new));
                    }
                }

                wrappers.keySet().removeIf(controller -> !controller.isConnected());
                return List.copyOf(result);

            } catch (RuntimeException | LinkageError error) {
                if (!unavailableLogged && Gdx.app != null) {
                    Gdx.app.error("JoyCon", "Controller backend is unavailable; keyboard input remains usable", error);
                    unavailableLogged = true;
                }

                return List.of();
            }
        }
    }

    private static final class GdxDevice implements Device {
        private final Controller controller;
        private final Mapping mapping;
        private final JoyConSide side;

        private GdxDevice(Controller controller) {
            this.controller = controller;
            ControllerMapping source = controller.getMapping();

            mapping = new Mapping(source.axisLeftX, source.axisLeftY, source.buttonA, source.buttonB, source.buttonX, source.buttonY, source.buttonL1, source.buttonR1, source.buttonDpadUp, source.buttonDpadDown, source.buttonDpadLeft, source.buttonDpadRight);
            side = sideOf(controller.getName());
        }

        @Override
        public String id() {
            return controller.getUniqueId();
        }

        @Override
        public String name() {
            return controller.getName();
        }

        @Override
        public JoyConSide side() {
            return side;
        }

        @Override
        public boolean connected() {
            return controller.isConnected();
        }

        @Override
        public boolean button(int id) {
            return controller.getButton(id);
        }

        @Override
        public float axis(int id) {
            return id < 0 ? 0 : controller.getAxis(id);
        }

        @Override
        public int minButtonIndex() {
            return controller.getMinButtonIndex();
        }

        @Override
        public int maxButtonIndex() {
            return controller.getMaxButtonIndex();
        }

        @Override
        public int axisCount() {
            return controller.getAxisCount();
        }

        @Override
        public Mapping mapping() {
            return mapping;
        }
    }
}
