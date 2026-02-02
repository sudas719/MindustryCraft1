# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Mindustry is an automation tower defense RTS game written in Java. The codebase uses the Arc framework (a custom game framework) and requires JDK 17.

## Build Commands

### Running and Building

**Windows:**
- Run game: `gradlew desktop:run`
- Build desktop JAR: `gradlew desktop:dist` (output: `desktop/build/libs/Mindustry.jar`)
- Build server: `gradlew server:dist` (output: `server/build/libs/server-release.jar`)
- Pack sprites: `gradlew tools:pack`

**Linux/Mac:**
- Run game: `./gradlew desktop:run`
- Build desktop JAR: `./gradlew desktop:dist`
- Build server: `./gradlew server:dist`
- Pack sprites: `./gradlew tools:pack`

### Testing

- Run all tests: `./gradlew test`
- Run specific test: `./gradlew tests:test --tests ClassName`
- Run checks: `./gradlew check`

### Running with Arguments

Pass arguments to the game using `-Pargs`:
```bash
./gradlew desktop:run -Pargs='["debug"]'
```

## Architecture

### Module Structure

- **core**: Main game logic, entities, content definitions, world/block system
- **desktop**: Desktop launcher and platform-specific code
- **server**: Headless server implementation
- **android**: Android platform code (requires Android SDK)
- **ios**: iOS platform code (RoboVM)
- **annotations**: Annotation processors for code generation
- **tools**: Build-time tools (sprite packing, code generation)
- **tests**: Test suite

### Code Generation System

The `mindustry.gen` package is **generated at build time** and does not exist in source:

- **Entity classes** (`Unit`, `Building`, `Bullet`, etc.): Generated from component classes in `mindustry.entities.comp` using the `@Component` annotation system. Components define reusable behaviors that are composed into entity types.
- **Network packets** (`Call`, `*Packet` classes): Generated from methods marked with `@Remote` annotation.
- **Asset references** (`Sounds`, `Musics`, `Tex`, `Icon`): Generated from files in asset folders.

Entity definitions combine components in `mindustry.content.UnitTypes` and similar content classes.

### Entity Component System

Mindustry uses a custom ECS implemented via annotation processing:

1. **Components** (`mindustry.entities.comp`): Define reusable behaviors (e.g., `HealthComp`, `VelComp`, `WeaponsComp`)
2. **Annotations** (`@Component`, `@Replace`, `@Import`, `@SyncField`): Control code generation
3. **Entity definitions**: Combine components to create concrete entity types
4. **Generated code**: Annotation processor creates final entity classes in `mindustry.gen`

### Content System

Game content (blocks, items, units, etc.) is defined in `mindustry.content`:
- `Blocks.java`: All block definitions
- `Items.java`: Resource items
- `Liquids.java`: Liquid types
- `UnitTypes.java`: Unit definitions with component composition
- `Bullets.java`: Projectile types
- `StatusEffects.java`: Status effect definitions
- Tech trees: `SerpuloTechTree.java`, `ErekirTechTree.java`

### Block System

Blocks are defined in `mindustry.world.blocks` with specialized categories:
- `defense`: Turrets, walls
- `distribution`: Conveyors, routers, bridges
- `production`: Drills, pumps, crafters
- `power`: Generators, batteries, nodes
- `liquid`: Conduits, tanks
- `units`: Factories, reconstructors
- `logic`: Processors, memory cells
- `storage`: Vaults, containers

Each block type extends `Block` and has an inner `Building` class for runtime state.

### Arc Framework Integration

Mindustry uses the Arc framework (custom game engine):
- Use `arc.struct` collections (`Seq`, `ObjectMap`, `ObjectSet`) instead of Java collections
- Use `arc.func` functional interfaces instead of `java.util.function`
- Use `arc.files.Fi` for file operations
- The Arc version is specified by `archash` in `gradle.properties`
- Local Arc development: Place Arc repository in parent directory (auto-detected)

### Mod System

Mods extend the `Mod` class (`mindustry.mod.Mod`):
- `init()`: Called after all mods are loaded
- `loadContent()`: Load custom content (blocks, items, units)
- `registerServerCommands()`: Add server console commands
- `registerClientCommands()`: Add in-game player commands

## Code Style Requirements

### Formatting
- No spaces around parentheses: `if(condition){`
- Same-line braces
- 4 spaces indentation
- `camelCase` for everything (constants, enums, variables)
- No underscores in names
- Wildcard imports: `import some.package.*`
- Single-line javadoc when possible: `/** @return description */`
- Short method/variable names preferred

### Platform Compatibility
- **Do not use** `java.util.function`, `java.util.stream`, or `forEach` (not supported on Android/iOS)
- **Do not use** `java.awt` (removed during JRE minimization)
- Use `arc.func` functional interfaces instead
- Use `arc.struct` collections instead of `java.util` collections

### Performance
- Avoid allocations in main loop - use `Pools` for object reuse
- Use `Tmp` variables for temporary calculations
- Make lists static and clear them on reuse
- Avoid boxed types (`Integer`, `Boolean`) - use specialized collections (`IntSeq`, `IntMap`)

### Code Organization
- Avoid unnecessary getters/setters - use public fields
- Don't create methods unless code is large or reused 2+ times
- No braceless if/else statements (except one-liners: `if(cond) return;`)

## Important Notes

- Sprite packing runs automatically before first run if sprites are missing
- The game requires sprites to be packed before running - this happens via `:tools:pack`
- IntelliJ IDEA is the recommended IDE
- Import the code style file: `.github/Mindustry-CodeStyle-IJ.xml`
- Always test changes by running the game before submitting
- Contact maintainer (Anuken) on Discord before large changes
