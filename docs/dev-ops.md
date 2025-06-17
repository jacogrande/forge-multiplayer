# MTG Forge Development Operations Guide

This document provides essential information for developers working on MTG Forge, including build procedures, testing guidelines, and common troubleshooting steps.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Build System](#build-system)
- [Testing](#testing)
- [Development Workflow](#development-workflow)
- [Troubleshooting](#troubleshooting)
- [Common Tasks](#common-tasks)

## Prerequisites

### Required Software
- **Java 17+** - Required for compilation and runtime
- **Apache Maven 3.6+** - Build and dependency management
- **Git** - Version control

### Environment Setup
```bash
# Verify Java version
java -version

# Verify Maven version  
mvn -version

# Check available memory (recommended 4GB+ heap)
java -XX:+PrintFlagsFinal -version | grep HeapSize
```

## Build System

MTG Forge uses a multi-module Maven architecture with CI-friendly versioning.

### Project Structure
```
forge/
├── forge-core/          # Core utilities and data structures
├── forge-game/          # Game rules engine and logic
├── forge-ai/            # AI player implementations
├── forge-gui/           # Common GUI components
├── forge-gui-desktop/   # Desktop application
├── forge-gui-mobile/    # Mobile GUI components
└── pom.xml             # Parent POM with version management
```

### Version Management
The project uses Maven's CI-friendly versioning with the `${revision}` property:
- Version is defined in the parent POM: `<revision>${versionCode}${snapshotName}</revision>`
- Current version: `2.0.05-SNAPSHOT`

### Basic Build Commands

#### Clean Build (Recommended)
```bash
# Full clean build without tests
mvn clean install -DskipTests -Dcheckstyle.skip=true

# Full clean build with tests (takes longer)
mvn clean install -Dcheckstyle.skip=true
```

#### Module-Specific Builds
```bash
# Build specific module with dependencies
mvn clean install -pl forge-game -am -DskipTests -Dcheckstyle.skip=true

# Build only the module (without dependencies)
mvn clean install -pl forge-game -DskipTests -Dcheckstyle.skip=true
```

#### Dependency Resolution Issues
If you encounter `${revision}` dependency resolution errors:

```bash
# 1. Clear Maven cache
rm -rf ~/.m2/repository/forge/

# 2. Install parent POM first
mvn clean install -N -Dcheckstyle.skip=true

# 3. Build dependencies in order
mvn clean install -pl forge-core -am -DskipTests -Dcheckstyle.skip=true
mvn clean install -pl forge-game -am -DskipTests -Dcheckstyle.skip=true
```

## Testing

### Test Framework
- **TestNG** - Primary testing framework
- **Test Location**: `src/test/java/` in each module
- **Reports**: Generated in `target/surefire-reports/`

### Running Tests

#### All Tests
```bash
# Run all tests in project
mvn test -Dcheckstyle.skip=true

# Run tests with dependency resolution
mvn test -am -Dcheckstyle.skip=true
```

#### Module-Specific Tests
```bash
# Run tests for specific module (recommended approach)
mvn test -pl forge-game -am -Dcheckstyle.skip=true

# Run specific test class
mvn test -pl forge-game -am -Dtest=SecurityFrameworkIntegrationTest -Dcheckstyle.skip=true

# Run tests matching pattern
mvn test -pl forge-game -am -Dtest="*Security*Test" -Dcheckstyle.skip=true
```

#### Test Options
```bash
# Skip tests that don't exist (useful for pattern matching)
-Dsurefire.failIfNoSpecifiedTests=false

# Run tests quietly (less verbose output)
-q

# Enable debug output for test failures
-X
```

### Test Writing Guidelines

#### Example Test Structure
```java
package forge.game.security;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.*;

public class ExampleTest {
    
    private SecurityValidator validator;
    
    @BeforeMethod
    public void setUp() {
        validator = new SecurityValidator();
    }
    
    @Test
    public void testBasicFunctionality() {
        // Arrange
        PlayerPerspective perspective = PlayerPerspective.OWNER;
        
        // Act
        boolean result = perspective.canSeeOwnerInformation();
        
        // Assert
        assertTrue("Owner should see own information", result);
    }
}
```

#### Best Practices
- Use descriptive test method names
- Follow Arrange-Act-Assert pattern
- Handle null cases and edge conditions
- Test both positive and negative scenarios
- Use meaningful assertion messages

## Development Workflow

### Creating New Features

1. **Create Feature Branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Implement Changes**
   - Write code following existing patterns
   - Add comprehensive tests
   - Update documentation if needed

3. **Test Changes**
   ```bash
   # Compile and test your specific module
   mvn clean test -pl forge-game -am -Dcheckstyle.skip=true
   ```

4. **Full Validation**
   ```bash
   # Run full build to ensure no regressions
   mvn clean install -Dcheckstyle.skip=true
   ```

### Code Quality

#### Checkstyle
```bash
# Run checkstyle validation
mvn checkstyle:check

# Skip checkstyle (for development)
-Dcheckstyle.skip=true
```

#### Memory Considerations
- MTG Forge requires **4GB+ heap space** for full functionality
- Use appropriate JVM flags for development:
  ```bash
  export MAVEN_OPTS="-Xmx4g -XX:+UseG1GC"
  ```

## Troubleshooting

### Common Issues and Solutions

#### 1. Maven Dependency Resolution Errors
**Error**: `Could not find artifact forge:forge:pom:${revision}`

**Solution**:
```bash
# Clear cache and rebuild
rm -rf ~/.m2/repository/forge/
mvn clean install -N -Dcheckstyle.skip=true
mvn clean install -pl forge-core -am -DskipTests -Dcheckstyle.skip=true
```

#### 2. Test Execution Failures
**Error**: Tests fail with `ExceptionInInitializerError` or localization issues

**Solution**:
- Use reactor builds: `mvn test -pl forge-game -am`
- Ensure proper module dependencies are built first
- Check that resource files are available

#### 3. OutOfMemoryError
**Error**: `java.lang.OutOfMemoryError: Java heap space`

**Solution**:
```bash
# Increase heap size
export MAVEN_OPTS="-Xmx4g"
mvn clean install -Dcheckstyle.skip=true
```

#### 4. Compilation Errors
**Error**: Various compilation issues after pulling changes

**Solution**:
```bash
# Clean rebuild
mvn clean compile -Dcheckstyle.skip=true

# If issues persist, clean workspace
git clean -fd
mvn clean install -DskipTests -Dcheckstyle.skip=true
```

## Common Tasks

### Quick Reference Commands

```bash
# Fast development cycle (compile only)
mvn compile -pl forge-game -am -Dcheckstyle.skip=true

# Run specific test during development
mvn test -pl forge-game -am -Dtest=YourTestClass -Dcheckstyle.skip=true -q

# Full clean build (CI-equivalent)
mvn clean install -Dcheckstyle.skip=true

# Package for distribution
mvn clean package -P windows-linux -Dcheckstyle.skip=true

# Generate test reports
mvn surefire-report:report -pl forge-game
```

### IDE Integration

#### IntelliJ IDEA
- Import as Maven project
- Set JDK to Java 17+
- Configure heap size: `-Xmx4g`
- Enable annotation processing

#### Eclipse
- Import existing Maven projects
- Set compiler compliance level to 17
- Configure JVM arguments for increased memory

### Performance Tips

1. **Use module-specific builds** during development
2. **Skip checkstyle** for faster iteration (`-Dcheckstyle.skip=true`)
3. **Use parallel builds** when possible (`-T 1C`)
4. **Skip tests** during compilation-only phases (`-DskipTests`)

### Git Workflow

```bash
# Before starting work
git pull origin master

# Create feature branch
git checkout -b feature/your-feature

# Regular commits
git add .
git commit -m "feat: add security framework tests"

# Push and create PR
git push origin feature/your-feature
```

## Additional Resources

- **CLAUDE.md** - Project-specific guidance for AI assistance
- **README.md** - General project information
- **PLAN.md** - Development roadmap
- **TODO.md** - Current task list

For build profiles and platform-specific instructions, see the main README.md file.