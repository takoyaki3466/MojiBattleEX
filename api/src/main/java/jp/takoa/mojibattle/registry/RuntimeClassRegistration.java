package jp.takoa.mojibattle.registry;

import jp.takoa.mojibattle.ability.config.annotations.ConfigView;
import jp.takoa.mojibattle.ability.config.RecordConfigParser;
import jp.takoa.mojibattle.ability.runtime.AbilityFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Runtimeクラスの検証、Config解析、個体生成を能力種別から独立して扱う。
 */
final class RuntimeClassRegistration<R> {
    private final String abilityId;
    private final Class<R> runtimeContract;
    private final Constructor<?> constructor;
    private final RecordConfigParser<?> configParser;
    private final Consumer<ConfigView> additionalValidator;

    RuntimeClassRegistration(String abilityId, Class<R> runtimeContract, Class<? extends R> runtimeType, Consumer<ConfigView> additionalValidator) {
        this.abilityId = Objects.requireNonNull(abilityId, "Ability ID is required");
        this.runtimeContract = Objects.requireNonNull(runtimeContract, "Runtime contract is required");

        Objects.requireNonNull(runtimeType, "Runtime type is required");

        this.additionalValidator = Objects.requireNonNull(additionalValidator, "Validator is required");

        if (!runtimeContract.isAssignableFrom(runtimeType)) {
            throw new IllegalArgumentException(runtimeType.getName() + " must implement " + runtimeContract.getName());
        }

        if (runtimeType.isInterface() || Modifier.isAbstract(runtimeType.getModifiers()) || !Modifier.isPublic(runtimeType.getModifiers())) {
            throw new IllegalArgumentException("Runtime type must be public and concrete: " + runtimeType.getName());
        }

        Constructor<?> candidate = getConstructor(runtimeType);
        constructor = candidate;
        configParser = parser(candidate.getParameterTypes()[0]);
    }

    private static <R> Constructor<?> getConstructor(Class<? extends R> runtimeType) {
        Constructor<?> candidate = null;
        for (Constructor<?> current : runtimeType.getConstructors()) {
            if (current.getParameterCount() == 1 && current.getParameterTypes()[0].isRecord()) {
                if (candidate != null) {
                    throw new IllegalArgumentException("Ambiguous public config constructors: " + runtimeType.getName());
                }
                candidate = current;
            }
        }
        if (candidate == null) {
            throw new IllegalArgumentException("Public single-record constructor required: " + runtimeType.getName());
        }
        return candidate;
    }

    AbilityFactory<? extends R> factory() {
        return source -> {
            Object config = configParser.parse(source);
            additionalValidator.accept(source);
            try {
                Object runtime = constructor.newInstance(config);
                return runtimeContract.cast(runtime);
            } catch (InstantiationException | IllegalAccessException exception) {
                throw new IllegalStateException("Cannot construct runtime for " + abilityId, exception);
            } catch (InvocationTargetException exception) {
                Throwable cause = exception.getCause();
                if (cause instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new IllegalStateException("Runtime constructor failed for " + abilityId, cause);
            }
        };
    }

    Consumer<ConfigView> validator() {
        return source -> {
            configParser.parse(source);
            additionalValidator.accept(source);
        };
    }

    Object[] displayValues(ConfigView source, float multiplier) {
        Object[] values = configParser.displayValues(source, multiplier);
        additionalValidator.accept(source);
        return values;
    }

    private static <C> RecordConfigParser<C> parser(Class<C> configType) {
        return new RecordConfigParser<>(configType);
    }
}
