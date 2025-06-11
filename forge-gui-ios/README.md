# Forge iOS GUI

The `forge-gui-ios` module is the iOS-specific deployment target for MTG Forge mobile application. It uses RoboVM to compile Java code to native iOS binaries, enabling deployment to iOS devices and the App Store. This module wraps the LibGDX mobile interface for iOS-specific integration, handling platform requirements, device capabilities, and App Store distribution.

## Table of Contents

- [Overview](#overview)
- [RoboVM Integration](#robovm-integration)
- [iOS Platform Features](#ios-platform-features)
- [Build System](#build-system)
- [Device Compatibility](#device-compatibility)
- [App Store Requirements](#app-store-requirements)
- [Development Workflow](#development-workflow)
- [Distribution](#distribution)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

## Overview

### What It Provides

The forge-gui-ios module serves as the iOS application wrapper:

- **Native iOS Application**: Complete iOS app packaging with proper iOS lifecycle management
- **RoboVM Java-to-Native Compilation**: Java code compiled to native ARM binaries
- **LibGDX iOS Backend**: Integration with LibGDX's iOS backend for OpenGL rendering
- **iOS Platform Integration**: Native iOS features like clipboard, file access, and device detection
- **App Store Distribution**: Apple App Store compatible packaging and metadata
- **Universal App Support**: Compatible with both iPhone and iPad devices

### Architecture

```
forge-gui-ios deployment structure:

iOS Application (Main.java)
├── RoboVM Compiler          # Java-to-native compilation
├── LibGDX iOS Backend       # OpenGL ES rendering
├── forge-gui-mobile         # Core mobile interface
├── iOS Adapter              # Platform-specific implementations
├── Native Libraries         # ARM/iOS native components
└── iOS Resources            # App icons, plist, certificates
```

## RoboVM Integration

### Java-to-Native Compilation

**RoboVM Ahead-of-Time Compilation:**
- Compiles Java bytecode directly to native ARM machine code
- No JVM runtime required on iOS devices
- Enables native performance and iOS platform integration
- Supports full Java 17 language features

**Compilation Process:**
```java
public class Main extends IOSApplication.Delegate {
    @Override
    protected IOSApplication createApplication() {
        // Initialize LibGDX with iOS backend
        IOSApplicationConfiguration config = new IOSApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;
        
        // Create Forge application with iOS adapter
        ApplicationListener app = Forge.getApp(
            new IOSClipboard(), 
            new IOSAdapter(), 
            assetsDir, 
            false, false, 0, false, 0, "", ""
        );
        
        return new IOSApplication(app, config);
    }
}
```

### iOS-Specific Adaptations

**Platform Constraints:**
- Apps cannot restart or exit programmatically (iOS security model)
- Limited background processing capabilities
- Sandboxed file system access
- App Store review requirements for all functionality

**iOS Device Detection:**
```java
@Override
public boolean isTablet() {
    // Simple device type detection based on screen orientation
    return Gdx.graphics.getWidth() > Gdx.graphics.getHeight();
}
```

## iOS Platform Features

### iOS System Integration

**Clipboard Integration:**
```java
private static final class IOSClipboard implements Clipboard {
    @Override
    public boolean hasContents() {
        return UIPasteboard.getGeneralPasteboard().toString().length() > 0;
    }
    
    @Override
    public String getContents() {
        return UIPasteboard.getGeneralPasteboard().getString();
    }
    
    @Override
    public void setContents(final String contents) {
        UIPasteboard.getGeneralPasteboard().setString(contents);
    }
}
```

**File System Access:**
- Sandboxed application directory for game data
- Documents directory for user-accessible files
- Read-only access to application bundle resources
- Shared container support for app extensions

**Device Capabilities:**
- iPhone and iPad universal app support
- Retina display detection and scaling
- iOS version compatibility (iOS 12.0+)
- Hardware capability detection (memory, graphics)

### iOS User Interface Integration

**iOS-Specific UI Elements:**
- Native iOS keyboard integration
- Share sheet support for exporting content
- iOS notification support for background operations
- Proper handling of iOS Safe Areas (iPhone X+ notch support)

**App Lifecycle Management:**
```java
// iOS app lifecycle handled by IOSApplication.Delegate
@Override
protected IOSApplication createApplication() {
    // App initialization
}

// iOS automatically handles:
// - applicationDidBecomeActive
// - applicationWillResignActive  
// - applicationDidEnterBackground
// - applicationWillEnterForeground
```

## Build System

### RoboVM Build Configuration

The module uses RoboVM for cross-compilation to iOS:

**Core RoboVM Configuration (robovm.xml):**
```xml
<config>
    <executableName>${app.executable}</executableName>
    <mainClass>forge.ios.Main</mainClass>
    <os>ios</os>
    <arch>thumbv7</arch>
    <target>ios</target>
    <iosInfoPList>Info.plist.xml</iosInfoPList>
</config>
```

**Native Library Integration:**
- `libgdx.a`: LibGDX native iOS library
- `libObjectAL.a`: OpenAL audio library for iOS
- `libgdx-freetype.a`: FreeType font rendering

**iOS Frameworks:**
- UIKit: iOS user interface framework
- OpenGLES: GPU-accelerated graphics rendering
- QuartzCore: Core animation and graphics
- AudioToolbox: Audio playback and recording
- AVFoundation: Media capture and playback

### Maven Build Integration

**Dependencies:**
```xml
<dependencies>
    <!-- Core Forge modules -->
    <dependency>
        <groupId>forge</groupId>
        <artifactId>forge-gui-mobile</artifactId>
    </dependency>
    
    <!-- RoboVM iOS backend -->
    <dependency>
        <groupId>com.badlogicgames.gdx</groupId>
        <artifactId>gdx-backend-robovm</artifactId>
        <version>1.13.5</version>
    </dependency>
</dependencies>
```

### Build Commands

**Note: iOS builds require macOS and Xcode**

```bash
# Compile for iOS simulator (development)
mvn robovm:iphone-sim

# Compile for iOS device (testing)
mvn robovm:ios-device

# Create iOS App Store package
mvn robovm:create-ipa

# Install on connected device
mvn robovm:ios-device -Drobovm.device.name="Device Name"
```

## Device Compatibility

### Supported iOS Devices

**iOS Version Requirements:**
- iOS 12.0 minimum (ARMv7/ARM64 architecture)
- iOS 15.0 recommended for full feature support
- Universal app supporting both iPhone and iPad

**Device Categories:**
- **iPhone**: 4.7"+ screens with touch optimization
- **iPad**: 9.7"+ screens with tablet-specific layouts
- **iPad Pro**: Large screen layouts with enhanced features

**Hardware Requirements:**
- ARM64 processor (iPhone 5s+, iPad Air+)
- 2GB+ RAM recommended for optimal performance
- 500MB+ available storage for full card database

### Performance Scaling

**iOS-Specific Optimizations:**
- Metal graphics API support for enhanced performance
- Automatic memory management with iOS memory pressure handling
- Background processing limitations compliance
- Battery usage optimization for mobile gaming

**Graphics Adaptation:**
- Retina display support with automatic scaling
- Frame rate targeting (30/60 FPS) based on device capability
- Shader optimization for iOS GPU architectures
- Thermal throttling awareness and adaptation

## App Store Requirements

### Apple App Store Compliance

**App Store Guidelines:**
- Content rating appropriate for Magic: The Gathering gameplay
- No prohibited content or functionality
- Proper handling of user data and privacy
- Accessibility compliance (VoiceOver, Dynamic Type)

**Technical Requirements:**
- 64-bit binary support (ARM64)
- iOS deployment target compliance
- App Transport Security (ATS) for network communications
- Privacy usage descriptions for required permissions

**App Store Metadata:**
```xml
<!-- Info.plist.xml configuration -->
<key>CFBundleDisplayName</key>
<string>MTG Forge</string>

<key>CFBundleIdentifier</key>
<string>com.cardforge.forge</string>

<key>UIDeviceFamily</key>
<array>
    <integer>1</integer>  <!-- iPhone -->
    <integer>2</integer>  <!-- iPad -->
</array>
```

### Privacy and Permissions

**Required Permissions:**
- Network access for content downloads and updates
- Local storage for game data and card database
- Camera access (if QR code scanning implemented)

**Privacy Compliance:**
- No personal data collection without explicit consent
- Local data storage with user control
- Transparent privacy policy for any data usage

## Development Workflow

### Development Environment Setup

**Prerequisites:**
```bash
# macOS with Xcode (required for iOS development)
xcode-select --install

# RoboVM setup
# Install via Maven - no separate installation required
```

**iOS Simulator Testing:**
```bash
# Build and run in iOS Simulator
mvn clean package robovm:iphone-sim

# Run specific simulator device
mvn robovm:iphone-sim -Drobovm.device.name="iPhone 14 Pro"
```

### Device Testing

**iOS Device Deployment:**
```bash
# Install on connected device (requires provisioning profile)
mvn robovm:ios-device

# Monitor device logs
ios-deploy --debug --bundle target/MyApp.app
```

**Development Certificates:**
- Apple Developer Account required for device testing
- Development provisioning profiles for team devices
- Distribution certificates for App Store submission

### Debugging and Profiling

**Debugging Options:**
```bash
# Enable debug mode in RoboVM
mvn robovm:ios-device -Drobovm.debug=true

# Connect debugger to running app
lldb target/MyApp.app/MyApp
```

**Performance Profiling:**
- Xcode Instruments for CPU and memory profiling
- iOS device performance monitoring
- Battery usage analysis
- Graphics performance measurement

## Distribution

### App Store Distribution

**App Store Submission Process:**
1. **Code Signing**: Configure distribution certificates and provisioning profiles
2. **Archive Build**: Create release build with App Store configuration
3. **Validation**: Use Xcode or Application Loader to validate binary
4. **Upload**: Submit to App Store Connect for review
5. **Review Process**: Apple review for compliance and functionality
6. **Release**: Publish to App Store after approval

**Build Configuration:**
```bash
# Create App Store distribution build
mvn clean package robovm:create-ipa -Production

# Archive includes:
# - Signed application binary
# - dSYM files for crash reporting
# - App Store metadata
```

### Alternative Distribution

**TestFlight Beta Testing:**
- Internal testing for development team
- External testing for beta users
- Automatic distribution to test groups
- Crash reporting and feedback collection

**Enterprise Distribution:**
- Apple Developer Enterprise Program
- Internal distribution within organizations
- No App Store review required
- Limited to enterprise use cases

## Troubleshooting

### Common Build Issues

**RoboVM Compilation Problems:**
```bash
# Clear RoboVM cache
rm -rf ~/.robovm/cache

# Clean and rebuild
mvn clean package

# Verbose compilation output
mvn robovm:ios-device -Drobovm.debug=true
```

**iOS Deployment Issues:**
```bash
# Check code signing
security find-identity -v -p codesigning

# Verify provisioning profiles
ls ~/Library/MobileDevice/Provisioning\ Profiles/

# Reset iOS Simulator
xcrun simctl erase all
```

### Runtime Issues

**Memory Management:**
- Monitor memory usage on actual iOS devices
- Test with iOS memory pressure conditions
- Validate garbage collection performance
- Check for memory leaks in Instruments

**Performance Issues:**
- Profile with Xcode Instruments on target devices
- Check thermal throttling on sustained gameplay
- Validate frame rate stability across device types
- Test battery impact during extended sessions

**iOS-Specific Crashes:**
- Check crash logs in Xcode Organizer
- Validate symbol information in dSYM files
- Test iOS version compatibility
- Verify proper iOS lifecycle handling

### Development Environment Issues

**Xcode Integration:**
```bash
# Reset Xcode development settings
rm -rf ~/Library/Developer/Xcode/DerivedData

# Update Xcode command line tools
xcode-select --install

# Verify iOS SDK installation
xcodebuild -showsdks
```

**Certificate and Provisioning:**
- Verify Apple Developer Account status
- Check certificate expiration dates
- Ensure provisioning profiles match app bundle ID
- Validate device UDID registration

## Contributing

### iOS Development Guidelines

**Platform-Specific Considerations:**
- Follow iOS Human Interface Guidelines
- Test on both iPhone and iPad form factors
- Ensure proper iOS accessibility support
- Validate App Store guideline compliance

**Code Contributions:**
- Understand RoboVM limitations and capabilities
- Test thoroughly on physical iOS devices
- Maintain iOS-specific adapter implementations
- Follow Apple's recommended practices for iOS development

**Performance Requirements:**
- Profile on target iOS hardware (older devices)
- Validate memory usage patterns on iOS
- Test thermal characteristics during gameplay
- Ensure battery life meets iOS user expectations

### Testing Requirements

**Cross-Device Testing:**
- Test on multiple iOS device types and sizes
- Validate across different iOS versions
- Check performance on older hardware
- Verify App Store submission compatibility

**Platform Integration Testing:**
- iOS-specific features (clipboard, file sharing)
- App lifecycle and background/foreground transitions
- iOS system integration (notifications, Share sheet)
- Hardware capability detection and adaptation

### Release Process

**Pre-Release Checklist:**
- [ ] All automated tests passing on iOS
- [ ] Manual testing on target iOS devices
- [ ] Performance validation completed
- [ ] App Store guidelines compliance verified
- [ ] Privacy policy and metadata updated
- [ ] Code signing and certificates current

**Release Deployment:**
1. Create distribution build with proper certificates
2. Upload to App Store Connect
3. Submit for App Store review
4. Monitor crash reports and user feedback
5. Prepare updates if needed

---

For mobile interface details, see [`forge-gui-mobile/README.md`](../forge-gui-mobile/README.md).

For development testing, see [`forge-gui-mobile-dev/README.md`](../forge-gui-mobile-dev/README.md).