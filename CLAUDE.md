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

- **Core Layer**: `forge-core` (data structures, utilities), `forge-game` (rules engine), `forge-ai` (AI logic)
- **GUI Layer**: `forge-gui` (common UI), platform-specific implementations (`forge-gui-desktop`, `forge-gui-android`, etc.)
- **Tools**: `adventure-editor` (content creation), `forge-lda` (deck generation)

### Key Technologies
- **Java 17+** with extensive JVM module system integration
- **Maven** for build management with complex profiles
- **Java Swing** for desktop GUI, **LibGDX** for mobile platforms
- **TestNG** for testing, **Netty** for networking

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
- **Game Logic**: Centralized in `forge-game` module with comprehensive rules engine
- **Platform Abstraction**: GUI modules implement platform-specific interfaces defined in `forge-gui`
- **Data Management**: Card database, deck management, and persistence handled in `forge-core`

### Testing Strategy
- **Game Simulation Tests**: Full game scenarios in `forge-game/src/test/`
- **Unit Tests**: Component-level testing across all modules
- **AI Testing**: Automated gameplay validation for computer opponents

### Memory Management
- Application requires **4GB+ heap space** for full functionality
- Extensive use of caching for card images and game objects
- Custom collection classes (`FCollection`) for performance optimization

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
- **20,000+ cards** with full rules implementation
- **Complete set data** including booster configurations
- **Game formats** (Standard, Legacy, Modern, Commander, etc.)
- **Draft environments** with 70+ pre-configured cubes

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