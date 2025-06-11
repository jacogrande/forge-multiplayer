# Forge Mobile Development Environment

The `forge-gui-mobile-dev` module provides a desktop development environment for testing the mobile GUI implementation of MTG Forge. It uses LibGDX with LWJGL3 backend to create a desktop window that runs the mobile interface, allowing developers to test mobile UI features without deploying to actual mobile devices.

## Table of Contents

- [Overview](#overview)
- [Purpose and Architecture](#purpose-and-architecture)
- [Setup and Installation](#setup-and-installation)
- [Development Workflow](#development-workflow)
- [Configuration Options](#configuration-options)
- [Testing Mobile Features](#testing-mobile-features)
- [Building and Distribution](#building-and-distribution)
- [Platform Differences](#platform-differences)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

## Overview

### What It Provides

The forge-gui-mobile-dev module serves as a bridge between desktop development and mobile deployment:

- **Desktop Testing Environment**: Run mobile UI on desktop for rapid development iteration
- **LibGDX Desktop Backend**: Uses LWJGL3 to provide OpenGL rendering on desktop
- **Adventure Mode Testing**: Specifically configured for testing Adventure mode features
- **Mobile UI Simulation**: Simulates tablet interface and touch interactions with mouse
- **Cross-Platform Development**: Test mobile features on Windows, macOS, and Linux

### Key Benefits

- **Faster Development Cycle**: No need to deploy to mobile devices for testing
- **Debugging Support**: Full desktop debugging tools available
- **Performance Profiling**: Desktop profiling tools for mobile code optimization
- **Screen Resolution Testing**: Test different mobile screen sizes and orientations
- **Input Method Testing**: Simulate touch interactions using mouse and keyboard

## Purpose and Architecture

### Development Bridge

```
forge-gui-mobile-dev serves as development interface:

Desktop Development → Mobile Testing → Mobile Deployment
     ↓                    ↓                 ↓
   IDE/Debugger     Desktop Window    Android/iOS
   Full Tooling     Mobile UI Test    Production
```

### Architecture Components

1. **Main Entry Point**: `forge.app.Main` - Desktop application launcher
2. **GameLauncher**: Configures LWJGL3 application with mobile-optimized settings
3. **DesktopAdapter**: Implements `IDeviceAdapter` to simulate mobile device capabilities
4. **LibGDX Backend**: LWJGL3 backend provides OpenGL rendering and input handling
5. **Mobile GUI**: Runs actual `forge-gui-mobile` code in desktop environment

### Dependencies

```
forge-gui-mobile-dev depends on:
├── forge-gui-mobile        # Mobile UI implementation
├── LibGDX Core             # Game development framework
├── LWJGL3 Backend          # Desktop OpenGL backend
├── LibGDX Controllers      # Gamepad support
├── Commons CLI             # Command-line argument parsing
└── Native Libraries        # Platform-specific natives
```

## Setup and Installation

### System Requirements

- **Java 17+** (OpenJDK or Oracle JDK)
- **Memory**: 4GB RAM minimum for development
- **Graphics**: OpenGL 3.0+ compatible graphics card
- **Storage**: 2GB for development environment and assets

### Quick Start

1. **Build and Run:**
   ```bash
   cd forge-gui-mobile-dev
   mvn clean package
   java -jar target/forge-gui-mobile-dev-*-jar-with-dependencies.jar
   ```

2. **Platform-Specific Launchers:**
   ```bash
   # Windows
   forge-adventure.exe
   
   # macOS/Linux
   ./forge-adventure.sh
   ```

### Development Environment

```bash
# For active development with hot reload
mvn exec:java -Dexec.mainClass="forge.app.Main"

# With specific JVM arguments
java --add-opens java.desktop/java.awt=ALL-UNNAMED \
     -jar forge-gui-mobile-dev.jar
```

## Development Workflow

### Mobile UI Development Process

1. **Code Changes**: Modify mobile UI code in `forge-gui-mobile`
2. **Quick Test**: Run mobile-dev to test changes immediately
3. **Debug**: Use desktop debugging tools to identify issues
4. **Iterate**: Rapid development cycle without mobile deployment
5. **Deploy**: Test on actual devices after desktop validation

### Key Development Features

**Window Configuration:**
```java
// Configure for mobile testing
config.setWindowedMode(1920, 1080);  // Standard tablet resolution
config.setResizable(false);          // Fixed size like mobile
config.setTitle("Forge Mobile Dev"); // Clear identification
```

**Device Simulation:**
- Touch interactions mapped to mouse clicks
- Tablet mode enabled by default (`isTablet() returns true`)
- Landscape/portrait mode switching via configuration files
- Mobile-specific resource loading and caching

### Testing Different Configurations

**Screen Orientations:**
```bash
# Create orientation switch file
echo "1" > switch_orientation.ini  # Landscape mode
rm switch_orientation.ini          # Portrait mode
```

**Resolution Testing:**
- Modify `Config.instance().getSettingData().width/height`
- Test different tablet and phone resolutions
- Validate UI scaling and layout responsiveness

## Configuration Options

### Application Configuration

The mobile-dev environment supports various configuration options:

**Display Settings:**
- `fullScreen`: Enable/disable fullscreen mode
- `width/height`: Set window dimensions for testing
- `autoIconify`: Minimize behavior in fullscreen
- `hdpiMode`: High-DPI rendering mode

**Development Options:**
```java
// Asset directory selection
String assetsDir = Files.exists(Paths.get("./res")) ? "./" : "../forge-gui/";

// Device simulation parameters
boolean isTablet = true;          // Simulate tablet interface
boolean hasInternet = true;       // Simulate network connectivity
String downloadsDir = "Downloads/"; // Simulated downloads folder
```

### Platform-Specific Configuration

**macOS Optimization:**
```java
// Fix for macOS without -XstartOnFirstThread
if (SharedLibraryLoader.isMac) {
    Configuration.GLFW_LIBRARY_NAME.set("glfw_async");
}
```

**Windows Integration:**
- Launch4j configuration for executable generation
- Windows-specific icon and metadata
- Registry integration for file associations

## Testing Mobile Features

### Input Testing

**Touch Simulation:**
- Mouse clicks simulate touch taps
- Mouse drag simulates touch gestures
- Scroll wheel simulates pinch-to-zoom
- Keyboard shortcuts for mobile-specific actions

**Gamepad Support:**
- Connected gamepads are recognized
- Mobile gamepad mappings can be tested
- Input latency and responsiveness validation

### UI Component Testing

**Mobile-Specific Components:**
- Touch-optimized button sizes
- Swipe gesture recognition
- Pull-to-refresh functionality
- Mobile navigation patterns

**Adventure Mode Features:**
- RPG-style adventure interface
- Character progression systems
- Mobile-optimized inventory management
- Touch-friendly dungeon exploration

### Performance Testing

**Rendering Performance:**
- OpenGL rendering pipeline validation
- Frame rate monitoring and optimization
- Memory usage profiling with desktop tools
- Battery usage simulation (via CPU/GPU monitoring)

## Building and Distribution

### Development Build

```bash
# Quick development build
mvn clean package

# With all dependencies included
mvn clean package assembly:single
```

### Platform-Specific Distributions

**Windows Executable:**
```bash
mvn clean package  # Generates forge-adventure.exe
```

**Cross-Platform JAR:**
```bash
# Self-contained JAR with all dependencies
target/forge-gui-mobile-dev-*-jar-with-dependencies.jar
```

### Build Artifacts

The build process generates:
- `forge-adventure.exe` - Windows executable
- `forge-adventure.sh` - Unix shell script
- `forge-adventure.command` - macOS command file
- JAR with dependencies for manual execution

## Platform Differences

### Desktop vs Mobile Behavior

**Simulated Mobile Features:**
- File system access limited to mobile-appropriate directories
- Network connectivity always reported as available
- Touch input mapped to mouse interactions
- Orientation changes via configuration files

**Desktop-Specific Features:**
- Full file system access for development
- Desktop clipboard integration
- Window management and resizing
- Desktop notification systems

**Adventure Mode Specific:**
- Adventure mode resources and assets
- RPG progression systems
- Mobile-optimized UI layouts
- Touch-friendly controls and navigation

### Development vs Production

**Development Environment:**
- Full debugging capabilities
- Hot reload and rapid iteration
- Desktop development tools integration
- Verbose logging and error reporting

**Production Mobile:**
- Optimized resource loading
- Battery usage optimization
- Platform-specific input handling
- Mobile security restrictions

## Troubleshooting

### Common Issues

**LWJGL3 Startup Problems:**
```bash
# macOS specific - add JVM argument
-XstartOnFirstThread

# Or use async GLFW (automatic in GameLauncher)
Configuration.GLFW_LIBRARY_NAME.set("glfw_async");
```

**Graphics Issues:**
```bash
# Force software rendering
-Dorg.lwjgl.opengl.Display.allowSoftwareOpenGL=true

# OpenGL debugging
-Dorg.lwjgl.util.Debug=true
```

**Memory Issues:**
```bash
# Increase heap size for development
-Xmx8192m

# Enable memory debugging
-XX:+PrintGCDetails -XX:+PrintGCTimeStamps
```

### Mobile Simulation Issues

**Touch Input Not Working:**
- Verify mouse events are being translated correctly
- Check LibGDX input processor configuration
- Ensure mobile touch handlers are registered

**Asset Loading Problems:**
- Verify assets directory path configuration
- Check mobile-specific resource loading
- Ensure fallback assets are available

**Performance Issues:**
- Desktop OpenGL drivers may behave differently than mobile
- Monitor CPU/GPU usage during development
- Profile with desktop tools but validate on mobile

### Development Environment Issues

**Build Problems:**
```bash
# Clean and rebuild dependencies
mvn clean install -U

# Skip tests if failing
mvn clean package -DskipTests
```

**IDE Integration:**
- Configure IDE to use correct Java module system settings
- Add required `--add-opens` arguments to run configurations
- Set up debugging with mobile-specific breakpoints

## Contributing

### Development Guidelines

- Test changes in mobile-dev before mobile deployment
- Maintain compatibility with actual mobile platforms
- Use mobile-appropriate UI patterns and interactions
- Optimize for both desktop testing and mobile performance

### Testing Requirements

- Verify functionality in mobile-dev environment
- Test on actual mobile devices before merging
- Validate performance across different screen sizes
- Ensure touch interactions work correctly

### Code Contributions

**Mobile-Specific Features:**
1. Implement in `forge-gui-mobile` module
2. Test using `forge-gui-mobile-dev`
3. Validate on actual mobile devices
4. Update documentation for new features

**Cross-Platform Compatibility:**
- Ensure features work in both desktop and mobile environments
- Use appropriate abstractions for platform differences
- Test across all supported platforms

---

For more information about the mobile GUI implementation, see [`forge-gui-mobile/README.md`](../forge-gui-mobile/README.md).

For desktop implementation details, see [`forge-gui-desktop/README.md`](../forge-gui-desktop/README.md).