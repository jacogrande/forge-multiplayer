# Forge Testing Guide

This document provides comprehensive instructions for running tests in the MTG Forge project, including setup, troubleshooting, and resolution of common issues.

## Table of Contents
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Maven ${revision} Property Resolution](#maven-revision-property-resolution)
- [Running Tests](#running-tests)
- [Common Errors and Solutions](#common-errors-and-solutions)
- [Module-Specific Testing](#module-specific-testing)
- [CI/CD Testing](#cicd-testing)
- [Performance Testing](#performance-testing)
- [Troubleshooting](#troubleshooting)

## Prerequisites

### Required Software
- **Java**: Version 17 or higher (required for module system compatibility)
- **Maven**: Version 3.8.1 or higher (required for CI-friendly versioning)
- **Memory**: 4GB+ heap space recommended for full test suite

### Verify Installation
```bash
java --version    # Should show Java 17+
mvn --version     # Should show Maven 3.8.1+
```

### Environment Setup
For GUI tests on headless systems:
```bash
export DISPLAY=":1"
Xvfb :1 -screen 0 800x600x8 &
```

## Quick Start

### Basic Test Run
```bash
# Run all tests with checkstyle disabled (recommended for development)
mvn clean test -Dcheckstyle.skip=true

# Run tests for specific module
mvn test -pl forge-gui -Dcheckstyle.skip=true

# Run specific test class
mvn test -pl forge-gui -Dtest=ConnectionStateTest -Dcheckstyle.skip=true
```

### Skip Problematic Tests (Development)
```bash
# Skip performance tests that may be environment-dependent
mvn test -Dcheckstyle.skip=true -Dtest='!KryoSerializationTest'

# Skip all tests, just compile
mvn clean compile -Dcheckstyle.skip=true
```

## Maven ${revision} Property Resolution

### The Problem
Forge uses CI-friendly versioning with `${revision}` property, which can cause dependency resolution failures:
```
Failed to read artifact descriptor for forge:forge-core:jar:2.0.05-SNAPSHOT
Could not find artifact forge:forge:pom:${revision}
```

### The Solution
The project uses the Maven Flatten Plugin to resolve CI-friendly versions. **This issue has been fixed** in the current configuration.

#### Verification Steps
1. **Check Flatten Plugin Configuration** (already configured correctly):
```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>flatten-maven-plugin</artifactId>
    <version>1.6.0</version>
    <executions>
        <execution>
            <id>flatten</id>
            <phase>process-resources</phase> <!-- Must be process-resources, not deploy -->
            <goals>
                <goal>flatten</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <updatePomFile>true</updatePomFile>
        <flattenMode>resolveCiFriendliesOnly</flattenMode>
    </configuration>
</plugin>
```

2. **Clean Corrupted Repository** (if issues persist):
```bash
# Remove any corrupted ${revision} directories
rm -rf ~/.m2/repository/forge/forge/'${revision}'
rm -rf ~/.m2/repository/forge/
```

3. **Install Dependencies in Correct Order**:
```bash
# Install parent POM first
mvn clean install -N -Dcheckstyle.skip=true -DskipTests

# Install dependency modules
mvn clean install -pl forge-core,forge-game,forge-ai -Dcheckstyle.skip=true -DskipTests

# Now tests should work
mvn test -pl forge-gui -Dcheckstyle.skip=true
```

#### Verifying the Fix
```bash
# This should generate .flattened-pom.xml with resolved version
mvn clean process-resources -Dcheckstyle.skip=true

# Check the flattened POM contains resolved version (not ${revision})
cat .flattened-pom.xml | grep version
```

## Running Tests

### All Tests
```bash
# Full test suite (may take several minutes)
mvn clean test

# With checkstyle disabled (faster)
mvn clean test -Dcheckstyle.skip=true

# Parallel execution (faster but may cause timing-sensitive test failures)
mvn clean test -T 1C -Dcheckstyle.skip=true
```

### Module-Specific Tests
```bash
# Core module tests
mvn test -pl forge-core -Dcheckstyle.skip=true

# Game module tests  
mvn test -pl forge-game -Dcheckstyle.skip=true

# GUI module tests
mvn test -pl forge-gui -Dcheckstyle.skip=true

# Desktop GUI tests (requires display)
mvn test -pl forge-gui-desktop -Dcheckstyle.skip=true
```

### Specific Test Classes
```bash
# Single test class
mvn test -pl forge-gui -Dtest=ConnectionStateTest -Dcheckstyle.skip=true

# Multiple test classes
mvn test -pl forge-gui -Dtest=ConnectionStateTest,ReconnectionManagerTest -Dcheckstyle.skip=true

# Test pattern matching
mvn test -pl forge-gui -Dtest='*Connection*' -Dcheckstyle.skip=true
```

### Test with Specific JVM Arguments
```bash
# Run with extra memory and module access
mvn test -Dcheckstyle.skip=true \
  -Dmaven.surefire.jvmArguments="-Xmx8g --add-opens java.base/java.lang=ALL-UNNAMED"
```

## Common Errors and Solutions

### 1. TestNG Not Found
**Error**: `package org.testng does not exist`

**Solution**: Add TestNG dependency to module's pom.xml:
```xml
<dependency>
    <groupId>org.testng</groupId>
    <artifactId>testng</artifactId>
    <version>7.10.2</version>
    <scope>test</scope>
</dependency>
```

### 2. Module Access Violations
**Error**: `Unable to make field accessible: module java.base does not "opens java.lang" to unnamed module`

**Solution**: Already configured in parent POM with proper `--add-opens` arguments. If issues persist:
```bash
mvn test -Dmaven.surefire.jvmArguments="--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED"
```

### 3. GUI Tests on Headless Systems
**Error**: `java.awt.HeadlessException`

**Solution**: Set up virtual framebuffer:
```bash
export DISPLAY=":1"
Xvfb :1 -screen 0 800x600x8 &
mvn test -pl forge-gui-desktop -Dcheckstyle.skip=true
```

### 4. Performance Test Failures
**Error**: `Kryo should be faster than Java serialization. Ratio: 0.43`

**Solution**: Performance tests are environment-dependent. Skip or adjust:
```bash
# Skip performance tests
mvn test -Dtest='!KryoSerializationTest' -Dcheckstyle.skip=true

# Or run with more warmup iterations (modify test code)
```

### 5. Checkstyle Violations
**Error**: `There are 5 errors reported by Checkstyle`

**Solution**: Either fix violations or skip checkstyle:
```bash
# Skip checkstyle (recommended for development)
mvn test -Dcheckstyle.skip=true

# Run checkstyle separately to see violations
mvn checkstyle:check
```

### 6. Memory Issues
**Error**: `java.lang.OutOfMemoryError: Java heap space`

**Solution**: Increase memory allocation:
```bash
export MAVEN_OPTS="-Xmx8g -XX:MaxMetaspaceSize=512m"
mvn test -Dcheckstyle.skip=true
```

## Module-Specific Testing

### forge-core
**Focus**: Serialization, utilities, basic data structures
```bash
mvn test -pl forge-core -Dcheckstyle.skip=true
```
**Key Tests**: `KryoSerializationTest` (performance-sensitive)

### forge-game  
**Focus**: Game rules engine, player mechanics, security
```bash
mvn test -pl forge-game -Dcheckstyle.skip=true
```
**Key Tests**: Game simulation, security framework tests

### forge-gui
**Focus**: Network layer, connection management, UI abstractions
```bash
mvn test -pl forge-gui -Dcheckstyle.skip=true
```
**Key Tests**: `ConnectionStateTest`, `ReconnectionManagerTest`

### forge-gui-desktop
**Focus**: Swing GUI components, desktop integration
```bash
# Requires display or virtual framebuffer
mvn test -pl forge-gui-desktop -Dcheckstyle.skip=true
```

## CI/CD Testing

### GitHub Actions Workflow
The project uses `.github/workflows/test-build.yaml` for automated testing:

```yaml
# Simplified workflow for local reproduction
- uses: actions/setup-java@v3
  with:
    java-version: '17'
    
- name: Setup virtual framebuffer
  run: |
    export DISPLAY=":1"
    Xvfb :1 -screen 0 800x600x8 &
    
- name: Run tests
  run: mvn -U -B clean test
```

### Local CI Simulation
```bash
# Simulate CI environment locally
export DISPLAY=":1"
Xvfb :1 -screen 0 800x600x8 &
mvn -U -B clean test -Dcheckstyle.skip=true
```

## Performance Testing

### Running Performance Tests
```bash
# Run with optimal JVM settings
mvn test -pl forge-core -Dtest=KryoSerializationTest \
  -Dmaven.surefire.jvmArguments="-Xmx8g -XX:+UseG1GC"
```

### Understanding Performance Requirements
- **Kryo vs Java Serialization**: Kryo should be >10x faster
- **Memory Usage**: Kryo should use ≤50% memory of Java serialization
- **Consistency**: All objects should deserialize to equivalent states

### Debugging Performance Issues
```bash
# Run with JVM profiling
mvn test -pl forge-core -Dtest=KryoSerializationTest \
  -Dmaven.surefire.jvmArguments="-XX:+PrintGCDetails -XX:+PrintGCTimeStamps"
```

## Troubleshooting

### Test Discovery Issues
```bash
# Verify test compilation
mvn test-compile -pl forge-gui

# List discovered tests
mvn test -pl forge-gui -Dtest=ConnectionStateTest -DdryRun=true
```

### Dependency Issues
```bash
# Check dependency tree
mvn dependency:tree -pl forge-gui

# Force dependency reload
mvn clean install -U -Dcheckstyle.skip=true
```

### IDE Integration
For IntelliJ IDEA:
1. Import as Maven project
2. Set Project SDK to Java 17+
3. In Run Configurations, add VM options:
   ```
   --add-opens java.base/java.lang=ALL-UNNAMED
   --add-opens java.base/java.util=ALL-UNNAMED
   ```

### Debug Logging
```bash
# Enable Maven debug output
mvn test -X -pl forge-gui -Dtest=ConnectionStateTest

# Enable TestNG verbose output
mvn test -pl forge-gui -Dtest=ConnectionStateTest \
  -Dmaven.surefire.jvmArguments="-Dtestng.verbose=10"
```

### Clean Build Environment
```bash
# Nuclear option: clean everything and rebuild
mvn clean install -Dcheckstyle.skip=true -DskipTests
rm -rf ~/.m2/repository/forge/
mvn clean install -pl forge-core,forge-game,forge-ai -Dcheckstyle.skip=true -DskipTests
mvn test -pl forge-gui -Dcheckstyle.skip=true
```

## Best Practices

### For Development
1. Always use `-Dcheckstyle.skip=true` during active development
2. Run module-specific tests rather than full suite for faster feedback
3. Use virtual framebuffer for GUI tests in headless environments
4. Increase memory allocation for comprehensive test runs

### For CI/CD
1. Use `-U` flag to force update snapshots
2. Use `-B` flag for batch mode (non-interactive)
3. Set up virtual framebuffer for GUI tests
4. Consider parallel execution with `-T 1C` for speed

### For Contributing
1. Run full test suite before submitting PRs
2. Fix checkstyle violations in your code changes
3. Add tests for new functionality
4. Ensure tests pass in clean environment

## Quick Reference

```bash
# Essential commands for daily development
mvn test -pl forge-gui -Dcheckstyle.skip=true                    # Quick module test
mvn test -pl forge-gui -Dtest=ConnectionStateTest -Dcheckstyle.skip=true  # Single test
mvn clean install -Dcheckstyle.skip=true -DskipTests            # Build without testing
mvn clean test -Dcheckstyle.skip=true                           # Full test suite

# Troubleshooting commands
rm -rf ~/.m2/repository/forge/ && mvn clean install -N -DskipTests  # Reset dependencies
mvn test -X -pl forge-gui -Dtest=YourTest                       # Debug test execution
export MAVEN_OPTS="-Xmx8g" && mvn test -Dcheckstyle.skip=true   # Increase memory
```