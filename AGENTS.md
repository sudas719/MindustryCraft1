# Repository Guidelines

## Project Structure & Module Organization
- `core/`: main game logic, content definitions, and assets in `core/assets`.
- `desktop/`: desktop launcher and platform glue; `server/`: headless server.
- `android/` and `ios/`: platform builds (Android only if an SDK is configured).
- `annotations/`: annotation processors; `tools/`: build-time tooling (e.g., sprite packing).
- `tests/`: JUnit test module; resources in `tests/src/test/resources`.
- Generated code (e.g., `mindustry.gen`) is produced at build time¡ªdo not edit by hand.

## Build, Test, and Development Commands
- JDK 17 is required.
- Windows uses `gradlew`, macOS/Linux uses `./gradlew`.
- Run the game: `./gradlew desktop:run`
- Build desktop JAR: `./gradlew desktop:dist` (output: `desktop/build/libs/Mindustry.jar`)
- Build server JAR: `./gradlew server:dist` (output: `server/build/libs/server-release.jar`)
- Pack sprites: `./gradlew tools:pack`
- Run all tests: `./gradlew test`
- Run a specific test: `./gradlew tests:test --tests PowerTests`
- Pass game args: `./gradlew desktop:run -Pargs='["debug"]'`

## Coding Style & Naming Conventions
- Import the IntelliJ code style from `.github/Mindustry-CodeStyle-IJ.xml`.
- 4-space indentation, same-line braces, no spaces around parentheses (e.g., `if(condition){`).
- `camelCase` for fields/methods/constants/enums; `PascalCase` for types; no underscores.
- Prefer wildcard imports and keep Javadocs single-line when possible.
- Platform rules: avoid `java.util.function`, `java.util.stream`, `forEach`, and `java.awt`.
  Use `arc.func`, `arc.struct`, and `arc.files.Fi` instead.
- Performance: avoid allocations in hot paths; use `Pools` and `Tmp` helpers.

## Testing Guidelines
- Tests live in `tests/src/test/java` and generally end with `*Tests.java`.
- Use JUnit 5; ensure new gameplay features work in-game before submitting.

## Commit & Pull Request Guidelines
- Commit messages are short, sentence-case summaries without prefixes
  (examples: `Map submission updates`, `buffer fix`).
- Follow the checklist in `.github/pull_request_template.md`.
- Include build/test notes and in-game verification; add screenshots for UI/visual changes.
- For translation or server list updates, note it explicitly in the PR description.

## Configuration Notes
- The Android module is included only when `ANDROID_HOME` or `local.properties` with `sdk.dir` exists.
