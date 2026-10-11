# Windows release packaging

MojiBattleEX uses the Java 21 `jpackage` executable selected through Gradle's Java toolchain. The output includes a private Java runtime, so players do not need to install Java.

## Portable ZIP — no WiX required

From PowerShell at the project root:

```powershell
.\packaging\package-windows.ps1 -Type portable
```

Output:

```text
lwjgl3\build\jpackage\distributions\MojiBattleEX-0.1.0-windows-x64.zip
```

Extract the ZIP and run `MojiBattleEX.exe`. Keep the complete extracted folder together.

## EXE installer

Install WiX Toolset and make its command-line tools available on `PATH`, then run:

```powershell
.\packaging\package-windows.ps1 -Type installer
```

Output:

```text
lwjgl3\build\jpackage\installer\MojiBattleEX-0.1.0.exe
```

The installer is per-user and provides an installation-directory chooser, Start menu entry, and desktop shortcut. Its stable upgrade UUID must not be changed between future versions.

## Complete release

With WiX available:

```powershell
.\packaging\package-windows.ps1 -Type all
```

This creates the portable ZIP, EXE installer, and `SHA256SUMS.txt` under `lwjgl3\build\jpackage`.

Before publishing a new version, update `version` in the root `build.gradle.kts`. Build each operating-system package on that operating system; jpackage does not cross-package Windows installers from macOS or Linux.
