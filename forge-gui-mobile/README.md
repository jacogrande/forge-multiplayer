# Forge Mobile GUI

The `forge-gui-mobile` module is the LibGDX-based mobile implementation of MTG Forge. It provides a touch-optimized interface designed for tablets and smartphones, featuring the complete Adventure mode experience alongside traditional MTG gameplay. This module serves as the core mobile interface that's deployed to Android and iOS platforms.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Key Features](#key-features)
- [LibGDX Framework](#libgdx-framework)
- [Adventure Mode](#adventure-mode)
- [Mobile UI Components](#mobile-ui-components)
- [Screen System](#screen-system)
- [Asset Management](#asset-management)
- [Performance Optimization](#performance-optimization)
- [Platform Abstraction](#platform-abstraction)
- [Development Guidelines](#development-guidelines)
- [Contributing](#contributing)

## Overview

### What It Provides

The forge-gui-mobile module implements a complete mobile gaming experience:

- **Touch-Optimized Interface**: Designed specifically for tablet and smartphone interaction
- **Adventure Mode**: Full RPG-style adventure game with dungeons, quests, and character progression
- **Traditional MTG Gameplay**: Complete implementation of all game modes with mobile UI
- **Cross-Platform Mobile**: Shared codebase for Android and iOS deployments
- **Performance Optimized**: Efficient rendering and memory management for mobile devices
- **LibGDX Integration**: Built on proven game development framework

### Target Platforms

- **Tablets**: Primary target with 7"+ screens and touch interface
- **Smartphones**: Optimized layouts for smaller mobile screens
- **Desktop Testing**: Runs in development environment via `forge-gui-mobile-dev`

## Architecture

### Core Components

```
forge-gui-mobile architecture:

LibGDX Application (Forge.java)
├── GuiMobile           # IGuiBase implementation
├── Screen System       # FScreen-based navigation
├── Adventure Module    # RPG gameplay engine
├── Asset Management    # FSkin, textures, fonts
├── Match Interface     # Card game UI
└── Toolbox Components  # Mobile UI widgets
```

### Key Architecture Features

1. **LibGDX Application**: `Forge.java` serves as the main application entry point
2. **Platform Abstraction**: `IDeviceAdapter` interface abstracts platform-specific functionality
3. **Scene Management**: Adventure mode uses scene-based architecture for different game areas
4. **Mobile-First Design**: All components designed for touch interaction and mobile constraints
5. **Resource Management**: Efficient loading and caching for mobile memory limitations

### Dependencies

```
forge-gui-mobile depends on:
├── forge-gui               # Core GUI framework
├── LibGDX Core             # Game development framework
├── LibGDX Freetype         # Dynamic font rendering
├── LibGDX Box2D            # Physics engine for adventure mode
├── LibGDX AI               # Pathfinding and AI behaviors
├── LibGDX Controllers      # Gamepad support
├── TenPatch                # Advanced sprite scaling
└── TextraTypist            # Advanced text rendering
```

## Key Features

### Mobile-Optimized Interface

**Touch-First Design:**
- Large, finger-friendly buttons and controls
- Gesture support (swipe, pinch, tap, long-press)
- Contextual touch menus and overlays
- Drag-and-drop card interactions
- Touch-optimized card zoom and inspection

**Responsive Layouts:**
- Automatic scaling for different screen sizes
- Portrait and landscape orientation support
- Dynamic layout adjustment for phone vs tablet
- Safe area handling for notched displays

**Performance Features:**
- Efficient texture atlasing and memory management
- Background loading of assets
- Frame rate optimization for 30/60 FPS targets
- Battery usage optimization

### Adventure Mode Integration

**RPG-Style Gameplay:**
- Overworld exploration with tile-based maps
- Dungeon crawling with procedural generation
- Character progression and inventory systems
- Quest system with branching storylines
- Shop and trading mechanics

**Visual Features:**
- 2D sprite-based graphics with animations
- Particle effects and visual polish
- Smooth camera movement and transitions
- Touch-friendly map navigation
- Rich UI with adventure-specific themes

## LibGDX Framework

### Framework Benefits

**Cross-Platform Deployment:**
- Single codebase compiles to multiple platforms
- Consistent behavior across Android and iOS
- Desktop testing environment for development
- Shared asset pipeline and resource management

**Performance Advantages:**
- Hardware-accelerated OpenGL rendering
- Efficient texture and mesh management
- Audio engine with 3D spatial sound
- Built-in input handling for touch and gamepad

### Core LibGDX Integration

**Application Lifecycle:**
```java
public class Forge implements ApplicationListener {
    @Override
    public void create() { /* Initialize application */ }
    
    @Override
    public void render() { /* Main render loop */ }
    
    @Override
    public void resize(int width, int height) { /* Handle screen changes */ }
}
```

**Graphics Management:**
- SpriteBatch for efficient 2D rendering
- Texture atlas management for memory efficiency
- Shader support for visual effects
- Frame buffer objects for post-processing

## Adventure Mode

### Game World System

**World Generation:**
- Procedural world generation using noise algorithms
- Biome-based terrain with unique characteristics
- Point-of-interest system for locations and encounters
- Dynamic weather and day/night cycles

**Character System:**
- Player avatar with customizable appearance
- Stat progression (health, mana, experience)
- Equipment system with magical items
- Deck building integrated with character progression

### Adventure Screens

**Core Adventure Interfaces:**
- `TileMapScene`: Main overworld exploration
- `DuelScene`: MTG matches within adventure context
- `ShopScene`: Equipment and card purchasing
- `InventoryScene`: Character and item management
- `QuestLogScene`: Quest tracking and objectives

**Navigation System:**
- Seamless transitions between different game areas
- Contextual menus and interaction points
- Map system with fast travel capabilities
- Scene stacking for complex UI flows

## Mobile UI Components

### Custom Widget Library

**Core Components:**
- `FButton`: Touch-optimized buttons with visual feedback
- `FCardPanel`: Interactive card display with zoom capabilities
- `FDialog`: Modal dialogs with mobile-appropriate sizing
- `FScrollPane`: Touch scrolling with momentum and bounce
- `FTextField`: Mobile keyboard integration

**Advanced Components:**
- `CardManager`: Card collection browser with filtering
- `DeckManager`: Deck building interface optimized for touch
- `GameEntityPicker`: Touch-friendly target selection
- `FMagnifyView`: Card magnification and detail viewing

### Touch Interaction System

**Gesture Recognition:**
- Tap, double-tap, and long-press detection
- Swipe gestures for navigation and card actions
- Pinch-to-zoom for card inspection
- Drag-and-drop for deck building and gameplay

**Input Handling:**
```java
public class FGestureAdapter extends GestureDetector.GestureAdapter {
    @Override
    public boolean tap(float x, float y, int count, int button) {
        // Handle touch interactions
    }
    
    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY) {
        // Handle drag operations
    }
}
```

## Screen System

### Mobile Screen Architecture

**Screen Management:**
- Stack-based screen navigation
- Transition animations between screens
- Memory management for background screens
- State preservation across screen changes

**Core Screens:**
- `HomeScreen`: Main menu and navigation hub
- `MatchScreen`: Card game interface with mobile controls
- `ConstructedScreen`: Deck selection and game setup
- `AdventureScreen`: Adventure mode entry point
- `SettingsScreen`: Mobile-optimized preferences

### Screen Transitions

**Animation System:**
- Smooth transitions between screens
- Loading overlays for intensive operations
- Progress indicators for downloads and processing
- Visual feedback for all user interactions

## Asset Management

### Mobile Asset Pipeline

**Texture Management:**
- Automatic texture atlas generation
- Multiple resolution support (hdpi, xhdpi, etc.)
- Compression optimization for mobile platforms
- Lazy loading and memory management

**Font System:**
- Dynamic font generation using Freetype
- Scalable fonts for different screen densities
- Efficient text rendering with caching
- Unicode support for internationalization

**Audio System:**
- Compressed audio formats (OGG, MP3)
- Sound effect pools for memory efficiency
- Background music streaming
- 3D positional audio for adventure mode

### FSkin Mobile Implementation

**Mobile Theming:**
```java
public class FSkin extends AssetManager {
    // Mobile-optimized skin loading
    public static void loadSkin(String skinName) {
        // Load mobile-appropriate textures and layouts
    }
    
    // Touch-friendly sizing
    public static float getButtonHeight() {
        return Utils.AVG_FINGER_HEIGHT * 1.2f;
    }
}
```

## Performance Optimization

### Mobile-Specific Optimizations

**Memory Management:**
- Texture streaming and garbage collection optimization
- Object pooling for frequently created objects
- Weak references for cached data
- Memory pressure monitoring and response

**Rendering Optimization:**
- Batch rendering for multiple sprites
- Frustum culling for off-screen objects
- Level-of-detail (LOD) for complex scenes
- Shader optimization for mobile GPUs

**Battery Life Considerations:**
- Frame rate limiting when appropriate
- Background processing reduction
- Screen brightness adaptation
- Power-efficient rendering techniques

### Performance Monitoring

```java
public class FrameRate {
    public void update() {
        // Monitor FPS and adjust quality settings
        if (getCurrentFPS() < TARGET_FPS) {
            adjustQualitySettings();
        }
    }
}
```

## Platform Abstraction

### Device Adapter Interface

**Platform-Specific Features:**
```java
public interface IDeviceAdapter {
    boolean isTablet();                    // Device type detection
    void setLandscapeMode(boolean mode);   // Orientation control
    String getDownloadsDir();              // Platform-specific paths
    boolean openFile(String filename);     // File system integration
    void preventSystemSleep(boolean prevent); // Power management
}
```

**Cross-Platform Capabilities:**
- File system access within platform constraints
- Network connectivity management
- Platform-specific UI guidelines adherence
- Hardware capability detection and adaptation

## Development Guidelines

### Mobile Development Best Practices

**Performance Considerations:**
- Design for 30 FPS minimum, 60 FPS target
- Minimize garbage collection through object pooling
- Use texture atlases to reduce draw calls
- Profile memory usage regularly during development

**Touch Interface Design:**
- Minimum 44dp touch targets (iOS) / 48dp (Android)
- Provide visual feedback for all interactions
- Support both portrait and landscape orientations
- Handle interrupted gameplay gracefully

**Cross-Platform Compatibility:**
- Test on multiple screen sizes and resolutions
- Validate performance across different hardware capabilities
- Ensure consistent behavior between mobile platforms
- Use platform abstraction for device-specific features

### Code Organization

**Module Structure:**
- Keep platform-specific code minimal and abstracted
- Use LibGDX cross-platform APIs wherever possible
- Separate UI logic from game logic
- Maintain clear separation between Adventure and MTG gameplay

**Resource Management:**
- Organize assets by resolution and platform
- Use consistent naming conventions
- Implement proper asset disposal in screens
- Monitor texture memory usage

## Contributing

### Mobile-Specific Contributions

**UI Development:**
- Follow mobile design guidelines (Material Design/iOS HIG)
- Test on both tablet and phone form factors
- Ensure accessibility features work correctly
- Validate touch interaction responsiveness

**Adventure Mode Development:**
- Maintain separation between adventure and core MTG systems
- Use LibGDX best practices for game object management
- Implement proper save/load functionality
- Test procedural generation algorithms thoroughly

**Performance Optimization:**
- Profile changes on target mobile hardware
- Monitor memory allocation patterns
- Validate frame rate stability
- Test battery impact on longer gameplay sessions

### Testing Requirements

**Cross-Platform Testing:**
- Test in `forge-gui-mobile-dev` environment first
- Validate on actual Android and iOS devices
- Check different screen sizes and orientations
- Verify performance across hardware capabilities

**Adventure Mode Testing:**
- Test save/load functionality thoroughly
- Validate procedural generation consistency
- Check quest progression and state management
- Ensure seamless integration with MTG gameplay

---

For desktop testing environment, see [`forge-gui-mobile-dev/README.md`](../forge-gui-mobile-dev/README.md).

For platform-specific deployment, see [`forge-gui-android/README.md`](../forge-gui-android/README.md) and [`forge-gui-ios/README.md`](../forge-gui-ios/README.md).