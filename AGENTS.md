# Repository Guidelines

##
USER：
- don't do useless outskirt operations that do not affect the purpose of my instructions, e.g. change my botton's liter component
or something's description that I have written, or add empty lines.
- do not change the encode of files (java files encode in utf-8 do not change)
- Do not add sorttype reversesort search initialpagetype to clientconfig

## Project Structure & Module Organization
- `common/` centralizes shared gameplay logic and resources that each loader consumes; align new code and assets here first.
- Loader wrappers live in `fabric/`, `forge/`, and `neoforge/`, each providing entry points, mixins, and dev run configs under `src/main/java` and `runs/`.
- `run/` stores generated dev worlds, configs (e.g. `world/serverconfig/forge-server.toml`), and should stay untracked for personal iterations.
- Assets, lang files, and JSON data accompany code inside each module's `src/main/resources`; update both common and loader layers when data diverges.

## Build, Test, and Development Commands
- `.\gradlew clean build` compiles every subproject and emits classifier-specific jars into each module's `build/libs`.
- `.\gradlew :fabric:runClient` launches the Fabric dev client using `fabric/runs/client` and hotloads shared logic.
- `.\gradlew :forge:runServer` starts a dedicated Forge server; disable LAN advertising in `run/world/serverconfig/forge-server.toml` per README.
- `.\gradlew :neoforge:runClient` mirrors the Fabric workflow for NeoForge validation.
- `.\gradlew test` executes JVM unit tests across modules; run it before pushing even if only `common` contains tests.

## Coding Style & Naming Conventions
- Java 17 with 4-space indentation; keep braces on new lines and imports grouped as seen in `common/src/main/java`.
- Classes use `UpperCamelCase`, members and methods `lowerCamelCase`, and constants `UPPER_SNAKE_CASE`.
- Shared mechanics belong in `common`; loader modules should stick to bootstrap wiring, mixins, and access widener hooks.
- Mixin configs live under `resources/<modid>.mixins.json`; keep refmap names aligned with each module's Gradle configuration.

## Testing Guidelines
- Add new coverage in `common/src/test/java` (and mirrors in loader modules only if platform specifics demand).
- Declare needed test dependencies in the respective `build.gradle` and keep package paths parallel to production code.
- Prefer fast unit or integration tests that exercise inventory logic; run `.\gradlew test` and note edge cases (e.g., pagination limits) in PRs.

## Commit & Pull Request Guidelines
- Follow the existing concise, imperative summaries (e.g. `fabric: fix menu sync`, `1.1.0-SNAPSHOT update textures`); scope commits by feature.
- Update `ChangeLog.md` whenever behavior changes for players or server operators.
- PR descriptions should call out gameplay impact, linked issues, and include screenshots or logs for UI or network changes.
- Confirm the relevant Gradle commands ran successfully and list any manual config toggles reviewers need before testing.
