# Phase 1: Foundation Enhancement - TODO List

## Overview

This document contains atomic, test-driven tasks for Phase 1 of the LAN Multiplayer implementation. Each task follows TDD principles with clear acceptance criteria defined through tests.

**Phase 1 Objectives:**

- Replace Java serialization with Kryo for 10x performance improvement
- Implement robust connection management with auto-reconnect
- Create secure state filtering system
- Add comprehensive error handling and logging

**Duration:** Weeks 1-4 (28 days)
**Priority:** High (Foundation for all subsequent phases)

---

## Week 1: Testing Infrastructure and Serialization Foundation

### Task 1.1: Set Up Networking Test Infrastructure

**Estimated Time:** 2 days  
**Module:** `forge-game` (new test packages)

**Acceptance Criteria:**

- [ ] Create `NetworkTestFramework` class for mocking network connections
- [ ] Implement `MockNetworkClient` and `MockNetworkServer` for isolated testing
- [ ] Create `NetworkTestGameState` factory for generating test game states
- [ ] Add performance benchmarking utilities for serialization testing
- [ ] All networking tests can run without actual network connections

**Implementation Details:**

```java
// forge-game/src/test/java/forge/game/network/NetworkTestFramework.java
public class NetworkTestFramework {
    public static MockNetworkServer createTestServer();
    public static MockNetworkClient createTestClient();
    public static Game createTestGameState();
    public static void measureSerializationPerformance(Object obj, Serializer serializer);
}
```

**Integration Points:**

- Must work with existing `Game` and `GameView` classes from forge-game
- Should integrate with TestNG testing framework already in use
- Must not interfere with existing non-networking tests

---

### Task 1.2: Create Kryo Serialization Test Suite

**Estimated Time:** 1 day  
**Module:** `forge-core` (new serialization package)

**Acceptance Criteria:**

- [ ] Test serialization roundtrip for all core game objects (Card, Player, Game)
- [ ] Benchmark Kryo vs Java serialization performance (target: 10x improvement)
- [ ] Test serialization of complex nested objects (Deck, CardPool, GameState)
- [ ] Verify serialization consistency across multiple runs
- [ ] Test backward compatibility with serialized data

**Implementation Details:**

```java
// forge-core/src/test/java/forge/util/serialization/KryoSerializationTest.java
@Test
public class KryoSerializationTest {
    @Test
    public void testCardSerialization() { /* ... */ }
    @Test
    public void testGameStateSerialization() { /* ... */ }
    @Test
    public void testPerformanceBenchmark() { /* ... */ }
    @Test
    public void testSerializationConsistency() { /* ... */ }
}
```

**Performance Target:**

- Kryo serialization must be 10x faster than Java serialization
- Serialized size must be 50%+ smaller than Java serialization
- Memory usage during serialization must be 30%+ lower

---

### Task 1.3: Implement Core Kryo Serialization Infrastructure

**Estimated Time:** 3 days  
**Module:** `forge-core` (new serialization package)

**Acceptance Criteria:**

- [x] Create `NetworkProtocol` interface for pluggable serialization
- [x] Implement `KryoNetworkProtocol` with configuration for Forge objects
- [x] Create custom Kryo serializers for `Card`, `ManaCost`, `CardType`, `GameView` (infrastructure complete, using JavaSerializer fallback)
- [x] Add compression support (GZIP for large messages)
- [x] Implement version compatibility mechanism

**Status:** ✅ **COMPLETE** - Infrastructure functional, performance optimization in progress
**Current Performance:** 0.57x vs Java (target: 10x) - optimization planned for next iteration

**Implementation Details:**

```java
// forge-core/src/main/java/forge/util/serialization/NetworkProtocol.java
public interface NetworkProtocol {
    byte[] serialize(Object obj) throws SerializationException;
    <T> T deserialize(byte[] data, Class<T> type) throws SerializationException;
    boolean isCompressed(byte[] data);
}

// forge-core/src/main/java/forge/util/serialization/KryoNetworkProtocol.java
public class KryoNetworkProtocol implements NetworkProtocol {
    private static final int COMPRESSION_THRESHOLD = 1024; // 1KB
    private final ThreadLocal<Kryo> kryoThreadLocal;
}
```

**Integration Requirements:**

- Must serialize all objects currently sent over network (NetEvent hierarchy)
- Should not break existing network message format during transition
- Must integrate with existing LZ4 compression if needed
- Should be thread-safe for concurrent access

---

## Week 2: Secure State Filtering System

### Task 2.1: Design and Test GameView Security Framework

**Estimated Time:** 2 days  
**Module:** `forge-game` (enhance existing GameView)

**Acceptance Criteria:**

- [ ] Create `SecureGameView` class that filters hidden information per player
- [ ] Test that opponent hands are properly hidden (show count only)
- [ ] Test that library contents are hidden except for revealed cards
- [ ] Test that face-down cards show no information to opponents
- [ ] Test that player-specific information (hand, library) is correctly filtered

**Implementation Details:**

```java
// forge-game/src/test/java/forge/game/GameViewSecurityTest.java
@Test
public class GameViewSecurityTest {
    @Test
    public void testOpponentHandHidden() {
        // Create game with known hands, verify opponent can't see specific cards
    }

    @Test
    public void testLibraryContentsHidden() {
        // Verify library contents hidden except for revealed cards
    }

    @Test
    public void testFaceDownCardsHidden() {
        // Test morph, manifest, etc. don't leak information
    }
}
```

**Security Requirements:**

- Zero information leakage about hidden game elements
- Consistent filtering across all game states
- Performance impact < 5ms per filter operation
- Must handle edge cases (revealed cards, shared zones)

---

### Task 2.2: Implement SecureGameState Manager

**Estimated Time:** 2 days  
**Module:** `forge-game` (new security package)

**Acceptance Criteria:**

- [ ] Create `SecureGameState` class that manages authoritative game state
- [ ] Implement `getPlayerView(int playerIndex)` with proper filtering
- [ ] Create `SecurityValidator` for validating player actions
- [ ] Test that invalid actions are properly rejected
- [ ] Test that players cannot access information they shouldn't see

**Implementation Details:**

```java
// forge-game/src/main/java/forge/game/security/SecureGameState.java
public class SecureGameState {
    private final Game authoritativeGame;
    private final SecurityValidator validator;

    public GameView getPlayerView(int playerIndex) { /* ... */ }
    public boolean validatePlayerAction(PlayerAction action, int playerIndex) { /* ... */ }
}
```

**Validation Requirements:**

- Players can only target cards they can see
- Actions must be legal in current game state
- Turn/priority restrictions must be enforced
- No action can access hidden information

---

### Task 2.3: Integrate Security System with Existing Network Code

**Estimated Time:** 2 days  
**Module:** `forge-gui` (enhance FServerManager)

**Acceptance Criteria:**

- [ ] Modify `FServerManager` to use `SecureGameState` for all game updates
- [ ] Update `GameProtocolHandler` to validate all incoming actions
- [ ] Test that existing network functionality still works with security layer
- [ ] Test that unauthorized actions are properly rejected and logged
- [ ] Ensure backward compatibility with current game flow

**Integration Points:**

- Must work with existing `GameProtocolSender`/`GameProtocolHandler`
- Should integrate with current `IGameController` interface
- Must not break existing AI or human player functionality
- Should log security violations for monitoring

---

## Week 3: Connection Management and Resilience

### Task 3.1: Design and Test Connection State Management

**Estimated Time:** 2 days  
**Module:** `forge-gui` (enhance networking)

**Acceptance Criteria:**

- [ ] Create `ConnectionState` enum (CONNECTING, CONNECTED, DISCONNECTED, RECONNECTING)
- [ ] Implement `ConnectionManager` to track client connections
- [ ] Test connection state transitions and event notifications
- [ ] Test heartbeat mechanism for detecting stale connections
- [ ] Test graceful connection shutdown and cleanup

**Implementation Details:**

```java
// forge-gui/src/test/java/forge/gui/network/ConnectionManagerTest.java
@Test
public class ConnectionManagerTest {
    @Test
    public void testConnectionStateTransitions() { /* ... */ }
    @Test
    public void testHeartbeatDetection() { /* ... */ }
    @Test
    public void testGracefulShutdown() { /* ... */ }
}
```

**Requirements:**

- Connection state must be thread-safe
- Heartbeat interval: 30 seconds
- Connection timeout: 60 seconds
- Must support multiple concurrent connections

---

### Task 3.2: Implement Auto-Reconnection System

**Estimated Time:** 3 days  
**Module:** `forge-gui` (enhance FGameClient)

**Acceptance Criteria:**

- [ ] Create `ReconnectionManager` with exponential backoff strategy
- [ ] Implement automatic reconnection attempts (max 5 attempts)
- [ ] Test reconnection after various failure types (network, server restart)
- [ ] Test game state recovery after successful reconnection
- [ ] Test user notification during reconnection attempts

**Implementation Details:**

```java
// forge-gui/src/main/java/forge/gui/network/ReconnectionManager.java
public class ReconnectionManager {
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final long INITIAL_DELAY_MS = 1000;

    public void handleDisconnection(DisconnectReason reason) { /* ... */ }
    public CompletableFuture<Boolean> attemptReconnection() { /* ... */ }
}
```

**Reconnection Strategy:**

- Initial delay: 1 second
- Exponential backoff: 1s, 2s, 4s, 8s, 16s
- Maximum reconnection time: 31 seconds total
- Must preserve game session state during reconnection

---

### Task 3.3: Implement Game State Recovery System

**Estimated Time:** 2 days  
**Module:** `forge-game` and `forge-gui`

**Acceptance Criteria:**

- [ ] Create `GameStateSnapshot` for preserving game state during disconnection
- [ ] Implement full state synchronization after reconnection
- [ ] Test that reconnected players see correct game state
- [ ] Test that game can continue seamlessly after reconnection
- [ ] Test edge cases (reconnection during combat, stack resolution)

**Recovery Requirements:**

- Full game state must be recoverable within 5 seconds
- No game information should be lost during reconnection
- Players should be notified of reconnection status
- Game should pause appropriately during player disconnection

---

## Week 4: Error Handling and Logging

### Task 4.1: Design Comprehensive Error Classification System

**Estimated Time:** 1 day  
**Module:** `forge-core` (new error handling package)

**Acceptance Criteria:**

- [ ] Create `NetworkError` hierarchy for different error types
- [ ] Implement error severity levels (INFO, WARN, ERROR, CRITICAL)
- [ ] Create error recovery strategies for each error type
- [ ] Test error classification and routing
- [ ] Document error codes and recovery procedures

**Implementation Details:**

```java
// forge-core/src/main/java/forge/error/NetworkError.java
public abstract class NetworkError extends Exception {
    public enum Severity { INFO, WARN, ERROR, CRITICAL }
    public enum Type {
        CONNECTION, SERIALIZATION, SECURITY, GAME_STATE,
        PROTOCOL, TIMEOUT, AUTHENTICATION
    }
}
```

**Error Categories:**

- Connection errors: timeouts, disconnections, authentication failures
- Serialization errors: corrupt data, version mismatches
- Security errors: unauthorized actions, validation failures
- Game state errors: desync, invalid state transitions
- Protocol errors: malformed messages, unknown message types

---

### Task 4.2: Implement Structured Logging System

**Estimated Time:** 2 days  
**Module:** `forge-gui` (enhance existing logging)

**Acceptance Criteria:**

- [ ] Create `NetworkLogger` with structured log format
- [ ] Implement log levels and filtering by component
- [ ] Add performance metrics logging (latency, throughput)
- [ ] Test log output format and rotation
- [ ] Create log analysis tools for debugging

**Implementation Details:**

```java
// forge-gui/src/main/java/forge/gui/logging/NetworkLogger.java
public class NetworkLogger {
    public void logConnection(String clientId, String event, Map<String, Object> details);
    public void logPerformance(String operation, long durationMs, Map<String, Object> metrics);
    public void logError(NetworkError error, String context);
}
```

**Logging Requirements:**

- Structured JSON format for log parsing
- Configurable log levels per component
- Performance impact < 1ms per log entry
- Log rotation to prevent disk space issues
- Integration with existing Forge logging

---

### Task 4.3: Implement Error Recovery and User Feedback

**Estimated Time:** 2 days  
**Module:** `forge-gui` (enhance user interface)

**Acceptance Criteria:**

- [ ] Create `ErrorRecoveryManager` for automatic error handling
- [ ] Implement user notification system for network errors
- [ ] Test error recovery for common failure scenarios
- [ ] Test user experience during error conditions
- [ ] Create error reporting mechanism for debugging

**Recovery Strategies:**

- Automatic reconnection for connection errors
- State resync for game state errors
- User notification for unrecoverable errors
- Graceful degradation when possible
- Error reporting for developers

---

### Task 4.4: Integration Testing and Performance Validation

**Estimated Time:** 2 days  
**Module:** All enhanced modules

**Acceptance Criteria:**

- [ ] Run full integration test suite with all Phase 1 enhancements
- [ ] Validate 10x serialization performance improvement
- [ ] Test complete error recovery scenarios end-to-end
- [ ] Verify backward compatibility with existing single-player functionality
- [ ] Performance regression testing (memory usage, CPU usage, latency)

**Performance Targets:**

- Serialization: 10x faster than Java serialization
- Memory usage: No more than 20% increase for networking features
- Latency: < 100ms for state synchronization
- Error recovery: < 5 seconds for reconnection
- Throughput: Support 8 concurrent connections without degradation

---

## Dependencies and Prerequisites

### External Dependencies to Add:

- **Kryo 5.6.0**: High-performance serialization library
- **SLF4J 2.0.16**: Structured logging framework (if not already present)
- **Jackson 2.18.1**: JSON parsing for log formatting
- **Micrometer 1.14.1**: Metrics collection (optional, for monitoring)

### Maven Dependencies:

```xml
<dependency>
    <groupId>com.esotericsoftware</groupId>
    <artifactId>kryo</artifactId>
    <version>5.6.0</version>
</dependency>
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.16</version>
</dependency>
```

### Module Integration Requirements:

- `forge-core`: New serialization and error handling packages
- `forge-game`: Enhanced GameView security and state management
- `forge-gui`: Updated networking with connection management
- `forge-ai`: No changes required (should work transparently)

---

## Risk Mitigation

### High-Risk Areas:

1. **Serialization Compatibility**: Risk of breaking existing network functionality
   - Mitigation: Implement gradual migration with fallback to Java serialization
2. **Performance Regression**: Risk of introducing latency or memory issues
   - Mitigation: Continuous performance monitoring and regression testing
3. **Security Vulnerabilities**: Risk of introducing new attack vectors
   - Mitigation: Comprehensive security testing and code review

### Testing Strategy:

- Unit tests for each atomic component
- Integration tests for cross-module functionality
- Performance benchmarks with automated regression detection
- Security testing with penetration testing tools
- Backward compatibility testing with existing game modes

---

## Success Criteria

### Phase 1 Completion Criteria:

- [ ] All TODO tasks completed and tested
- [ ] 10x improvement in serialization performance measured and validated
- [ ] Secure state filtering prevents information leakage (100% test coverage)
- [ ] Connection management handles all common failure scenarios
- [ ] Comprehensive error handling with user-friendly feedback
- [ ] Backward compatibility maintained with existing functionality
- [ ] Performance regression < 5% for single-player modes
- [ ] All integration tests pass with new networking foundation

### Ready for Phase 2:

- Solid networking foundation ready for remote PlayerController implementation
- Security system ready for multiplayer game validation
- Connection management ready for real-time state synchronization
- Error handling ready for production multiplayer scenarios

This Phase 1 foundation will enable all subsequent multiplayer features while maintaining the high quality and reliability standards of the Forge codebase.
