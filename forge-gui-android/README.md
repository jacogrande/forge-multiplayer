# Forge Android GUI

The `forge-gui-android` module is the Android-specific deployment target for MTG Forge mobile application. It packages the LibGDX mobile interface into an Android APK, handling platform-specific integration, permissions, device adaptation, and Google Play Store distribution requirements.

## Table of Contents

- [Overview](#overview)
- [Android Integration](#android-integration)
- [Build System](#build-system)
- [Platform Features](#platform-features)
- [Device Compatibility](#device-compatibility)
- [Permissions and Security](#permissions-and-security)
- [Performance Optimization](#performance-optimization)
- [Distribution](#distribution)
- [Development Workflow](#development-workflow)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

## Overview

### What It Provides

The forge-gui-android module serves as the Android application wrapper:

- **Android Application**: Complete APK packaging with proper Android lifecycle management
- **Platform Integration**: Native Android features like file access, networking, and hardware detection
- **LibGDX Android Backend**: Integration with LibGDX's Android backend for OpenGL rendering
- **Device Adaptation**: Automatic adaptation for phones, tablets, and different Android versions
- **Store Distribution**: Google Play Store compatible packaging and metadata
- **Permission Management**: Proper handling of Android runtime permissions

### Architecture

```
forge-gui-android deployment structure:

Android Application (Main.java)
├── LibGDX Android Backend    # OpenGL ES rendering
├── forge-gui-mobile         # Core mobile interface
├── Android Adapter          # Platform-specific implementations
├── Asset Management         # Android-specific file handling
└── Native Libraries         # ARM/x86 native components
```

## Android Integration

### Application Structure

**Main Components:**
- `Main.java`: Primary Android Activity implementing LibGDX lifecycle
- `Launcher.java`: Application entry point and permission handling
- `AndroidAdapter`: Implementation of `IDeviceAdapter` for Android-specific features
- Native libraries for different CPU architectures (ARM, x86)

**Android Lifecycle Integration:**
```java
public class Main extends AndroidApplication {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize LibGDX with Android backend
        // Handle permissions and device detection
        // Configure graphics and audio settings
    }
    
    @Override
    protected void onResume() {
        // Resume game state and audio
    }
    
    @Override
    protected void onPause() {
        // Pause game and save state
    }
}
```

### Device Adaptation

**Automatic Device Detection:**
- Tablet vs Phone classification based on screen size (7+ inches = tablet)
- Hardware capability detection (RAM, OpenGL version, CPU architecture)
- Android version adaptation (API 26+ support)
- Network connectivity monitoring (WiFi, cellular, airplane mode)

**Platform-Specific Features:**
- Android file system integration with scoped storage (Android 11+)
- System clipboard access with proper permissions
- Hardware gamepad detection and configuration
- Native file opening and sharing capabilities

## Build System

### Maven Configuration

The module uses Maven with Android-specific plugins:

**Core Build Profiles:**
- `android-debug`: Development builds with debug signing
- `android-test-build`: Test builds with embedded debug keystore
- `android-release-build`: Production builds with release signing
- `android-release-upload`: Automated FTP upload for distribution

### Dependencies

```xml
<dependencies>
    <!-- Core Forge modules -->
    <dependency>
        <groupId>forge</groupId>
        <artifactId>forge-gui-mobile</artifactId>
    </dependency>
    
    <!-- Android-specific libraries -->
    <dependency>
        <groupId>com.badlogicgames.gdx</groupId>
        <artifactId>gdx-backend-android</artifactId>
    </dependency>
    
    <!-- Error reporting -->
    <dependency>
        <groupId>io.sentry</groupId>
        <artifactId>sentry-android</artifactId>
    </dependency>
    
    <!-- File provider for sharing -->
    <dependency>
        <groupId>de.cketti.fileprovider</groupId>
        <artifactId>public-fileprovider</artifactId>
    </dependency>
</dependencies>
```

### Build Commands

```bash
# Debug build for testing
mvn clean package -P android-debug

# Release build for distribution
mvn clean package -P android-release-build

# Test build with debug signing
mvn clean package -P android-test-build

# Full release with upload
mvn clean package -P android-release-upload
```

## Platform Features

### Android-Specific Capabilities

**File System Access:**
- Scoped storage compliance for Android 11+
- External storage management with proper permissions
- Downloads directory access for imports/exports
- Asset packaging and extraction

**Network Integration:**
- Connectivity status monitoring
- WiFi vs cellular detection
- Network capability assessment
- Automatic updates and downloads

**Hardware Integration:**
- Device specification detection (RAM, CPU, screen size)
- Gamepad and controller support
- Orientation change handling
- Performance monitoring and adaptation

### Android UI Integration

**System Integration:**
- Android splash screen and loading animations
- Immersive mode support for full-screen gaming
- Proper handling of system UI (status bar, navigation bar)
- Android notification support for background operations

**Permission Handling:**
```java
// Dynamic permission checking for Android 6+
private boolean checkPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Android 13+ granular media permissions
        return checkMediaPermissions();
    } else {
        // Legacy storage permissions
        return checkStoragePermission();
    }
}
```

## Device Compatibility

### Supported Android Versions

**Minimum Requirements:**
- Android 8.0 (API 26) minimum
- Android 11 (API 30) recommended
- 64-bit architecture support (ARM64, x86_64)
- OpenGL ES 3.0+ for graphics

**Device Categories:**
- **Phones**: 5"+ screens with touch optimization
- **Tablets**: 7"+ screens with tablet-specific layouts
- **Foldables**: Adaptive layouts for changing screen configurations
- **Chromebooks**: Android app support on Chrome OS

### Performance Scaling

**Memory Management:**
- Automatic quality adjustment based on available RAM
- Texture compression for different GPU architectures
- Background processing limitations on low-end devices
- Garbage collection optimization for mobile

**Graphics Adaptation:**
- Automatic resolution scaling based on screen density
- Frame rate targeting (30/60 FPS) based on device capability
- Shader complexity adjustment for different GPU generations
- Battery usage optimization

## Permissions and Security

### Required Permissions

**Storage Access:**
```xml
<!-- Android 13+ granular permissions -->
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />

<!-- Legacy storage permissions -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

**Network Access:**
```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
```

**System Features:**
```xml
<uses-permission android:name="android.permission.VIBRATE"/>
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>
<uses-feature android:name="android.hardware.gamepad" android:required="false"/>
```

### Security Considerations

**Data Protection:**
- Local data encryption for sensitive game saves
- Secure network communication for online features
- Proper handling of user-generated content
- Privacy compliance with data collection

**App Security:**
- ProGuard code obfuscation for release builds
- Secure keystore management for app signing
- Runtime application self-protection (RASP)
- Sentry integration for crash reporting and security monitoring

## Performance Optimization

### Android-Specific Optimizations

**Memory Management:**
- Large heap allocation for card database
- Native memory usage for graphics assets
- Background memory cleanup when app is paused
- OOM (Out of Memory) prevention strategies

**Battery Optimization:**
- Frame rate limiting when appropriate
- Background processing reduction
- Wake lock management for preventing system sleep
- CPU frequency scaling based on game requirements

**Storage Optimization:**
- Asset compression and streaming
- Card image caching strategies
- Database optimization for mobile storage
- Temporary file cleanup

### Build Optimizations

**APK Size Reduction:**
- ProGuard shrinking and obfuscation
- Asset compression and optimization
- Native library stripping for unused architectures
- Multi-APK support for different device types

**Performance Profiling:**
```bash
# Enable performance profiling
mvn package -P android-debug -Dprofile=true

# Monitor memory usage
adb shell dumpsys meminfo com.forge.app

# GPU profiling
adb shell setprop debug.egl.profiler 1
```

## Distribution

### Google Play Store

**Store Requirements:**
- Target SDK 34+ (Android 14)
- 64-bit architecture support mandatory
- App Bundle (.aab) format preferred
- Privacy policy and data usage disclosure

**Store Optimization:**
- App store metadata and screenshots
- Feature graphics and promotional materials
- Localization for multiple markets
- A/B testing for store listing optimization

### Alternative Distribution

**Direct APK Distribution:**
- Self-signed APK for beta testing
- F-Droid compatibility considerations
- Enterprise distribution methods
- Sideloading instructions and warnings

## Development Workflow

### Development Environment Setup

**Prerequisites:**
```bash
# Android SDK installation
export ANDROID_HOME=/path/to/android-sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

# Build tools and platform
sdkmanager "platforms;android-35" "build-tools;35.0.0"
```

**Development Build:**
```bash
# Quick development iteration
mvn clean package -P android-debug
adb install target/forge-android-debug.apk

# Live debugging
adb logcat | grep "forge"
```

### Testing and Validation

**Device Testing:**
- Physical device testing on multiple Android versions
- Emulator testing for different screen sizes
- Performance testing on low-end devices
- Battery usage validation over extended sessions

**Automated Testing:**
```bash
# Unit tests
mvn test

# Integration tests with Robolectric
mvn test -Dtest=AndroidIntegrationTest

# UI automation tests
mvn test -Dtest=AndroidUITest
```

## Troubleshooting

### Common Build Issues

**Maven Build Problems:**
```bash
# Clean build cache
mvn clean

# Update dependencies
mvn dependency:resolve -U

# Skip tests for quick builds
mvn package -DskipTests
```

**Android SDK Issues:**
```bash
# Verify SDK configuration
mvn android:help

# Update build tools
sdkmanager --update

# License acceptance
sdkmanager --licenses
```

### Runtime Issues

**Permission Errors:**
- Verify runtime permissions are granted in device settings
- Check Android version compatibility for permission model
- Validate storage access patterns for scoped storage

**Performance Issues:**
- Monitor device temperature and throttling
- Check available RAM and storage space
- Validate graphics driver compatibility
- Adjust quality settings for device capabilities

**Network Connectivity:**
- Verify network permissions in manifest
- Check firewall and VPN configurations
- Validate network security configurations
- Test with different connection types (WiFi, cellular)

### Debugging Tools

**Android Debugging:**
```bash
# Application logs
adb logcat -s "forge"

# System information
adb shell getprop

# Memory monitoring
adb shell dumpsys meminfo com.forge.app

# GPU debugging
adb shell setprop debug.egl.trace gl
```

## Contributing

### Android-Specific Development

**Platform Guidelines:**
- Follow Android development best practices
- Maintain compatibility with supported Android versions
- Test thoroughly across different device configurations
- Optimize for both phone and tablet form factors

**Code Contributions:**
- Use Android Studio for native Android development
- Follow Material Design guidelines where appropriate
- Implement proper Android lifecycle management
- Add comprehensive error handling for Android-specific issues

**Testing Requirements:**
- Test on physical devices, not just emulators
- Validate across different Android versions (8.0+)
- Check performance on low-end devices
- Verify battery usage and thermal characteristics

### Release Process

**Pre-Release Checklist:**
- [ ] All automated tests passing
- [ ] Manual testing on target devices
- [ ] Performance validation completed
- [ ] Security scan passed
- [ ] Store metadata updated
- [ ] Release notes prepared

**Release Deployment:**
1. Create release build with proper signing
2. Upload to Google Play Console
3. Submit for review
4. Monitor crash reports and user feedback
5. Prepare hotfix releases if needed

---

For mobile interface details, see [`forge-gui-mobile/README.md`](../forge-gui-mobile/README.md).

For development testing, see [`forge-gui-mobile-dev/README.md`](../forge-gui-mobile-dev/README.md).