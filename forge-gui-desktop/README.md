# Forge Desktop GUI

The `forge-gui-desktop` module is the Java Swing-based desktop implementation of MTG Forge. It provides a comprehensive desktop gaming experience with advanced UI features, cross-platform compatibility, and platform-specific integrations for Windows, macOS, and Linux.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Key Features](#key-features)
- [Setup and Installation](#setup-and-installation)
- [Development Environment](#development-environment)
- [UI Framework](#ui-framework)
- [Platform-Specific Features](#platform-specific-features)
- [Building and Distribution](#building-and-distribution)
- [Testing](#testing)
- [Performance Optimization](#performance-optimization)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

## Overview

### What It Provides

The forge-gui-desktop module implements the desktop-specific interface for MTG Forge using Java Swing:

- **Complete Swing GUI**: Full-featured desktop interface with native look and feel
- **Multi-Platform Support**: Windows, macOS, and Linux distributions
- **Advanced Game Interface**: Sophisticated match UI with drag-and-drop, keyboard shortcuts, and visual effects
- **Deck Editor**: Comprehensive deck building tools with filtering, statistics, and import/export
- **Screen System**: Modular screen framework supporting multiple game modes and layouts
- **Platform Integration**: Native file dialogs, system clipboard, and OS-specific features

### Architecture

```
forge-gui-desktop implements IGuiBase interface from forge-gui
├── GuiDesktop.java         # Main platform interface implementation
├── screens/                # UI screens (home, match, deck editor, etc.)
├── toolbox/               # Reusable Swing components
├── gui/framework/         # Screen management and layout system
├── view/                  # Application entry point and window management
└── control/               # User interaction and keyboard handling
```

## Architecture

### Core Components

1. **GuiDesktop**: Implements `IGuiBase` interface, providing Swing-specific implementations for all platform-abstracted operations
2. **Screen Framework**: Modular system for managing different application screens (home, deck editor, match, etc.)
3. **FSkin System**: Comprehensive theming and styling framework with support for custom skins
4. **Toolbox Components**: Custom Swing components that integrate with the FSkin system
5. **Control Layer**: Manages user input, keyboard shortcuts, and application state

### Dependencies

```
forge-gui-desktop depends on:
├── forge-gui               # Core GUI framework and game logic
├── forge-core              # Utilities and data structures  
├── forge-game              # Game rules engine
├── forge-ai                # AI implementations
├── MigLayout               # Advanced layout manager
├── Java Image Scaling      # High-quality image scaling
├── JLayer                  # Audio playback support
└── FreeMarker              # Template processing
```

## Key Features

### Desktop-Specific Advantages

**Rich User Interface:**
- Native drag-and-drop for cards and deck building
- Context menus with right-click functionality
- Keyboard shortcuts for all major actions
- Multi-monitor support with window positioning
- Resizable layouts with persistent preferences

**Advanced Match Interface:**
- Zoomable card view with detailed inspection
- Multiple zone displays (hand, battlefield, graveyard, etc.)
- Stack visualization with spell/ability targeting
- Combat assignment with visual feedback
- Auto-yield system for streamlined gameplay

**Comprehensive Deck Editor:**
- Multi-column card browsing with advanced filtering
- Real-time deck statistics and mana curve analysis
- Import from various formats (text, MTGO, Arena)
- Batch operations for deck management
- Collection tracking and wishlist functionality

### Cross-Platform Features

**Windows Integration:**
- Launch4j executable generation
- Windows-specific file associations
- Native look and feel with system theming
- Registry integration for file types

**macOS Integration:**
- DMG distribution with drag-to-install
- macOS app bundle with proper metadata
- Menu bar integration following macOS conventions
- Retina display support with high-DPI rendering

**Linux Support:**
- Shell script launcher with proper Java detection
- Desktop integration files
- GTK+ look and feel support
- Package manager compatibility

## Setup and Installation

### System Requirements

- **Java 17+** (OpenJDK or Oracle JDK)
- **Memory**: 4GB RAM minimum, 8GB recommended
- **Storage**: 500MB for application, 2GB+ for full card image cache
- **Display**: 1024x768 minimum, 1920x1080+ recommended

### Quick Start

1. **Download and Run:**
   ```bash
   # Download the latest release
   # Extract and run platform-specific launcher:
   
   # Windows
   forge.exe
   
   # macOS  
   open Forge.app
   
   # Linux
   ./forge.sh
   ```

2. **First Launch:**
   - Application will download card database and images
   - Configure preferences in Settings menu
   - Import or create your first deck

## Development Environment

### Building from Source

```bash
# Clone the repository
git clone https://github.com/Card-Forge/forge.git
cd forge

# Build the desktop application
mvn clean install -pl forge-gui-desktop

# Run from target directory
cd forge-gui-desktop/target
java -jar forge-gui-desktop-*-jar-with-dependencies.jar
```

### Development Setup

```bash
# For active development
cd forge-gui-desktop

# Run with full debugging
mvn exec:java -Dexec.mainClass="forge.view.Main" -Dexec.args=""

# Build with specific platform profile
mvn clean install -P osx  # macOS DMG generation
mvn clean install -P windows-linux  # Standard JAR
```

### Required JVM Arguments

The application requires specific JVM arguments for proper operation:

```bash
-Xmx4096m                              # 4GB heap space
-Dfile.encoding=UTF-8                  # UTF-8 encoding
-Dio.netty.tryReflectionSetAccessible=true

# Module system compatibility (Java 9+)
--add-opens java.desktop/java.beans=ALL-UNNAMED
--add-opens java.desktop/javax.swing.border=ALL-UNNAMED  
--add-opens java.desktop/javax.swing.event=ALL-UNNAMED
--add-opens java.desktop/sun.swing=ALL-UNNAMED
# ... (see pom.xml for complete list)
```

## UI Framework

### Screen System

The application uses a modular screen system defined in `FScreen.java`:

```java
// Core screens
FScreen.HOME_SCREEN              // Main navigation
FScreen.DECK_EDITOR_CONSTRUCTED  // Deck building
FScreen.MATCH_SCREEN            // Game interface
FScreen.QUEST_BAZAAR            // Quest mode shop
FScreen.WORKSHOP_SCREEN         // Card workshop
```

Each screen consists of:
- **View (V)**: UI layout and components
- **Controller (C)**: Business logic and event handling
- **Layout**: Persistent window arrangement

### Custom Components

**FButton, FLabel, FPanel, etc.**: Themed Swing components that integrate with FSkin system

**Advanced Controls:**
- `FCardPanel`: Interactive card display with zoom and context menus
- `ItemManager`: Generic collection browser with filtering and sorting
- `DeckEditor`: Comprehensive deck building interface
- `MatchUI`: Complex game state visualization

### Theming and Skinning

```java
FSkin.loadSkin(skinFile);           // Load custom skin
FSkin.getColor(FSkinColor.CLR_*);   // Access themed colors
FSkin.getIcon(FSkinProp.ICO_*);     // Access themed icons
```

Skins define:
- Color schemes for all UI elements
- Icons and button graphics
- Background textures and patterns
- Font specifications

## Platform-Specific Features

### Windows Features

- **Executable Generation**: Launch4j creates `forge.exe` with proper metadata
- **File Associations**: Associate `.dck` and `.o8d` files with Forge
- **System Integration**: Windows-specific error reporting and crash handling

### macOS Features

- **App Bundle**: Proper `.app` structure with Info.plist metadata
- **DMG Distribution**: Installer disk image with background and layout
- **Menu Integration**: Native macOS menu bar behavior
- **High-DPI Support**: Retina display scaling and rendering

### Linux Features

- **Desktop Integration**: `.desktop` files for application launchers
- **Package Distribution**: Compatible with various package managers
- **Theme Integration**: Respects system GTK+ themes where possible

## Building and Distribution

### Standard Build

```bash
# Build JAR with dependencies
mvn clean package

# Output: target/forge-gui-desktop-*-jar-with-dependencies.jar
```

### Platform-Specific Builds

**Windows Distribution:**
```bash
mvn clean package
# Generates: forge.exe, launch scripts
```

**macOS Distribution:**
```bash
mvn clean package -P osx
# Generates: Forge.app bundle and DMG installer
```

**Linux Distribution:**
```bash
mvn clean package -P linux
# Generates: JAR with shell scripts
```

### Release Process

1. **Version Update**: Update version in parent POM
2. **Changelog**: Git changelog plugin generates CHANGES.txt
3. **Platform Builds**: Execute builds for all target platforms
4. **Testing**: Verify functionality on each platform
5. **Distribution**: Upload to release channels

## Testing

### Test Categories

**Unit Tests:**
- Component testing with PowerMock and TestNG
- Game simulation tests for rule verification
- UI component tests with Swing test framework

**Integration Tests:**
- Full game scenario testing
- Deck import/export validation
- Network multiplayer functionality

**Performance Tests:**
- Memory usage with large collections
- UI responsiveness during long operations
- Startup time optimization

### Running Tests

```bash
# All tests
mvn test

# Specific test categories
mvn test -Dtest="*UITest"
mvn test -Dtest="*SimulationTest"

# GUI tests (requires display)
mvn test -Dtest="*GUITest" -Djava.awt.headless=false
```

## Performance Optimization

### Memory Management

**Large Collections**: The application manages significant amounts of data:
- 20,000+ card database with full art
- Large deck collections
- Game history and statistics
- Image caching system

**Optimization Strategies:**
- Lazy loading of card images
- Weak references for cached data
- Configurable memory limits
- Garbage collection tuning

### Rendering Performance

**Image Scaling**: High-quality image scaling with caching
**UI Responsiveness**: Background loading for expensive operations
**Platform Optimization**: Platform-specific rendering optimizations

### Configuration Options

```properties
# Memory settings
forge.memory.imageCache=512MB
forge.memory.cardDatabase=1GB

# Performance settings  
forge.ui.animation=true
forge.ui.cardImageQuality=high
forge.network.timeout=30000
```

## Troubleshooting

### Common Issues

**Memory Errors:**
```bash
# Increase heap size
java -Xmx8192m -jar forge-gui-desktop.jar
```

**Display Issues:**
```bash
# Force software rendering
java -Dsun.java2d.d3d=false -jar forge-gui-desktop.jar

# OpenGL acceleration
java -Dsun.java2d.opengl=true -jar forge-gui-desktop.jar
```

**Module System Issues (Java 9+):**
- Ensure all `--add-opens` arguments are included
- Check for missing module dependencies
- Verify compatibility with JVM version

### Debug Mode

```bash
# Enable debug logging
java -Dforge.debug=true -jar forge-gui-desktop.jar

# Profile memory usage
java -Xms4096m -Xmx4096m -XX:+PrintGCDetails -jar forge-gui-desktop.jar
```

### Platform-Specific Issues

**Windows:**
- Verify Java installation and PATH
- Check Windows Defender exclusions
- Ensure write permissions for app directory

**macOS:**
- Allow app to run (System Preferences > Security)
- Verify Java installation: `/usr/libexec/java_home`
- Check Gatekeeper restrictions

**Linux:**
- Install required dependencies: `openjdk-17-jdk`
- Check display environment: `DISPLAY` variable
- Verify font rendering libraries

## Contributing

### Code Style

- Follow existing Swing patterns and conventions
- Use FSkin theming for all new components
- Implement proper event dispatch thread handling
- Add unit tests for new functionality

### Adding New Screens

1. Create View class extending appropriate base
2. Create Controller class implementing screen logic
3. Add screen definition to `FScreen.java`
4. Implement layout persistence if needed
5. Add navigation and menu integration

### UI Component Development

1. Extend appropriate Swing component
2. Integrate with FSkin theming system
3. Add mouse and keyboard event handling
4. Support accessibility features
5. Test with different look and feels

### Platform Integration

When adding platform-specific features:
- Use appropriate detection (`OperatingSystem` class)
- Graceful degradation on unsupported platforms
- Test across all supported operating systems
- Document platform-specific requirements

### Performance Considerations

- Profile memory usage with large datasets
- Optimize image loading and caching
- Use background threads for expensive operations
- Minimize Event Dispatch Thread blocking
- Test with different screen resolutions and DPI settings

---

For more information about the underlying GUI framework, see [`forge-gui/README.md`](../forge-gui/README.md).

For mobile implementations, see [`forge-gui-mobile/README.md`](../forge-gui-mobile/README.md).