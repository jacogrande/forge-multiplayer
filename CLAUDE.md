# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**MTG Forge** is a comprehensive Java-based Magic: The Gathering rules engine and game implementation. The project provides full MTG gameplay with AI opponents, supporting multiple platforms (desktop, Android, iOS) through a sophisticated multi-module Maven architecture.

## Development Commands

### Build Commands
```bash
# Standard build for Windows/Linux
mvn -U -B clean -P windows-linux install

# macOS build with DMG generation
mvn -U -B clean -P osx install

# Run tests (may require virtual display)
mvn -U -B clean test

# Build specific module
mvn -U -B clean install -pl forge-game
```

### Running the Application
```bash
# Desktop version (from forge-gui-desktop)
java -jar target/forge-gui-desktop-*-jar-with-dependencies.jar

# Mobile development version (from forge-gui-mobile-dev) 
java -jar target/forge-gui-mobile-dev-*-jar-with-dependencies.jar
```

### Testing
```bash
# Run all tests
mvn test

# Run tests for specific module
mvn test -pl forge-game

# Run specific test class
mvn test -Dtest=GameSimulationTest
```

## Architecture Overview

### Multi-Module Structure
The project follows a layered architecture with clear separation:

#### Core Foundation
- **forge-core**: Foundational data structures, card database management, deck construction, internationalization, and cross-platform utilities
- **forge-game**: Complete MTG rules engine with event-driven architecture, zone management, and comprehensive ability system
- **forge-ai**: Single unified AI with 100+ ability implementations, configurable personalities, and advanced simulation

#### GUI Implementation
- **forge-gui**: Platform-agnostic GUI framework providing game mode implementations, data management, and UI abstractions
- **forge-gui-desktop**: Java Swing implementation with native desktop integration, advanced UI features, and cross-platform distribution
- **forge-gui-mobile**: LibGDX-based mobile interface for Android and iOS
- **forge-gui-android**: Android-specific implementation with mobile optimizations

#### Specialized Tools
- **adventure-editor**: Content creation tools for adventure mode
- **forge-lda**: Deck generation algorithms and analysis
- **forge-installer**: Cross-platform installation packages

### Key Technologies
- **Java 17+** with extensive JVM module system integration and `--add-opens` arguments for reflection
- **Maven** for build management with platform-specific profiles (`windows-linux`, `osx`)
- **Java Swing** for desktop GUI with FSkin theming system and custom components
- **LibGDX** for mobile platforms with cross-platform asset management
- **TestNG** for comprehensive testing including game simulation tests
- **Netty** for multiplayer networking and client-server architecture

### Card Scripting System
Cards are defined using a custom text-based format in `forge-gui/res/cardsfolder/`. Each card script defines abilities, triggers, and game mechanics:

```
Name:Card Name
ManaCost:1 R
Types:Instant
A:SP$ DealDamage | Cost$ 1 R | ValidTgts$ Any | TgtPrompt$ Select target | NumDmg$ 3
Oracle:Deal 3 damage to any target.
```

## Development Patterns

### Code Organization
- **Game Logic**: Centralized in `forge-game` module with event-driven architecture using Observer, Command, and State patterns
- **Platform Abstraction**: GUI modules implement `IGuiBase`/`IGuiGame` interfaces defined in `forge-gui` using Bridge pattern
- **Data Management**: Card database, deck management, and persistence handled in `forge-core` with sophisticated storage abstractions
- **AI Architecture**: Layered decision-making system with configurable personalities via `.ai` profile files

### Core Design Patterns
- **Model-View-Controller**: Clear separation between data models, platform-specific views, and game controllers
- **Strategy Pattern**: AI personalities, game modes, and deck generation algorithms are pluggable implementations
- **Factory Pattern**: AbilityFactory creates spell/ability implementations, extensive use for complex object creation
- **Observer Pattern**: Event system for game state changes, UI updates, achievement tracking, and trigger management
- **Bridge Pattern**: Platform abstraction allows same game logic across Swing desktop and LibGDX mobile interfaces

### Testing Strategy
- **Game Simulation Tests**: Full MTG game scenarios in `forge-game/src/test/` with complex card interaction validation
- **Unit Tests**: Component-level testing across all modules with TestNG framework
- **AI Testing**: Automated gameplay validation with configurable AI personalities and decision-making verification
- **Integration Tests**: Multi-component interaction testing including spell resolution, combat, and zone changes
- **Performance Tests**: Memory usage monitoring, UI responsiveness, and large game state handling

### Memory Management
- Application requires **4GB+ heap space** for full functionality with 20,000+ card database and image caching
- Extensive use of lazy loading, weak references, and efficient data structures (`FCollection`, `ItemPool`)
- Custom collection classes optimized for MTG data patterns with type-safe operations
- Platform-specific memory optimizations for mobile vs desktop environments

## Build Profiles and Platform Considerations

### Desktop Development
- Primary development target using `windows-linux` profile
- Requires Java 17+ with extensive `--add-opens` arguments for module access
- Memory requirements: minimum 4GB heap for comfortable development

### Mobile Development
- Uses LibGDX framework for cross-platform compatibility
- `forge-gui-mobile-dev` provides desktop testing environment for mobile UI
- Asset downloading system for mobile platforms to manage app size

### Cross-Platform Challenges
- Java module system compatibility requires careful dependency management
- Platform-specific packaging and distribution (JAR, APK, IPA, DMG)
- Different UI frameworks (Swing vs LibGDX) requiring abstraction layers

## Card Database and Content

The project includes comprehensive Magic: The Gathering content:
- **20,000+ cards** with full rules implementation using custom script format
- **Complete set data** including booster configurations and edition metadata
- **Game formats** (Standard, Legacy, Modern, Commander, etc.) with format-specific card legality
- **Draft environments** with 70+ pre-configured cubes including MTGO Legacy/Vintage cubes
- **Game Modes**: Quest mode campaigns, Planar Conquest, Limited formats (Draft/Sealed), Tournament system

### Card Implementation System
- **Card Scripts**: Custom text format in `forge-gui/res/cardsfolder/` defining abilities, costs, and effects
- **Ability System**: 100+ effect types with modular `SpellAbilityEffect` implementations
- **Zone Management**: Complete MTG zone tracking (Battlefield, Hand, Library, Graveyard, Exile, Command, etc.)
- **Trigger System**: 80+ trigger types covering all MTG interactions with event-driven processing
- **Static Effects**: Implements MTG's 8-layer dependency system with timestamp ordering

### AI and Gameplay
- **AI Personalities**: Configurable via `.ai` profile files (Default, Cautious, Reckless, Experimental)
- **Decision Engine**: Multi-stage AI decision process with legality checking, timing evaluation, and simulation
- **Combat System**: Sophisticated combat evaluation with threat assessment and optimal play
- **Memory System**: AI learning and adaptation capabilities with game state tracking

Card content is primarily in `forge-gui/res/` with scripts in `cardsfolder/` and metadata in `editions/`.

## CI/CD and Quality

### Automated Testing
- GitHub Actions workflows for testing on every push/PR
- Virtual framebuffer setup for headless GUI testing
- Comprehensive test suites covering game rules and AI behavior

### Code Quality
- Checkstyle integration for code style enforcement
- Maven Enforcer for dependency management
- Sentry integration for error tracking in production builds