# Phase 1 Testing Documentation

## Overview

This document provides comprehensive testing procedures and acceptance criteria for all Phase 1 multiplayer networking features implemented in Forge MTG. Phase 1 focuses on robust error handling, structured logging, and user feedback systems essential for reliable multiplayer gameplay.

## Table of Contents

1. [Test Environment Setup](#test-environment-setup)
2. [Task 4.1: Comprehensive Error Classification System](#task-41-comprehensive-error-classification-system)
3. [Task 4.2: Structured Logging System](#task-42-structured-logging-system)
4. [Task 4.3: Error Recovery and User Feedback](#task-43-error-recovery-and-user-feedback)
5. [Integration Testing](#integration-testing)
6. [Performance Validation](#performance-validation)
7. [Regression Testing](#regression-testing)

---

## Test Environment Setup

### Prerequisites

```bash
# Ensure Java 17+ is installed
java -version

# Build the project
mvn clean compile -P windows-linux

# Run core tests to verify base functionality
mvn test -pl forge-core
```

### Test Data Requirements

- Network simulation tools for connection failures
- Log file samples for parsing validation
- Mock error scenarios for recovery testing
- Performance baseline measurements

---

## Task 4.1: Comprehensive Error Classification System

### Overview

Implements a hierarchical error classification system with 40+ specific error types organized into 6 major categories.

### Acceptance Criteria

#### AC-4.1.1: Error Hierarchy Structure

**Criteria**: All error types must inherit from NetworkError base class with proper categorization

**Test Procedure**:

```java
// Verify error hierarchy
@Test
public void testErrorHierarchy() {
    // Connection Errors
    assertTrue(ConnectionTimeoutError.class.getSuperclass() == ConnectionError.class);
    assertTrue(ConnectionRefusedError.class.getSuperclass() == ConnectionError.class);
    assertTrue(ConnectionLostError.class.getSuperclass() == ConnectionError.class);

    // Security Errors
    assertTrue(AuthenticationError.class.getSuperclass() == SecurityError.class);
    assertTrue(AuthorizationError.class.getSuperclass() == SecurityError.class);
    assertTrue(ValidationError.class.getSuperclass() == SecurityError.class);

    // Game State Errors
    assertTrue(StateSyncError.class.getSuperclass() == GameStateError.class);
    assertTrue(StateCorruptionError.class.getSuperclass() == GameStateError.class);
    assertTrue(StateTransitionError.class.getSuperclass() == GameStateError.class);

    // Protocol Errors
    assertTrue(UnknownMessageError.class.getSuperclass() == ProtocolError.class);
    assertTrue(MalformedMessageError.class.getSuperclass() == ProtocolError.class);
    assertTrue(SequenceError.class.getSuperclass() == ProtocolError.class);

    // Application Errors
    assertTrue(ResourceExhaustionError.class.getSuperclass() == ApplicationError.class);
    assertTrue(ConfigurationError.class.getSuperclass() == ApplicationError.class);
}
```

**Expected Results**:

- All error classes extend appropriate parent classes
- 6 major error categories are properly defined
- 40+ specific error types are available

#### AC-4.1.2: Unique Error Codes

**Criteria**: Each error type must have a unique, human-readable error code

**Test Procedure**:

```java
@Test
public void testUniqueErrorCodes() {
    Set<String> errorCodes = new HashSet<>();

    // Connection errors
    ConnectionTimeoutError timeout = new ConnectionTimeoutError("host", 7777, 5000);
    assertTrue(timeout.getErrorCode().startsWith("CON-"));
    assertTrue(errorCodes.add(timeout.getErrorCode().substring(0, 3))); // CON

    // Security errors
    AuthenticationError auth = new AuthenticationError(AuthenticationError.Reason.INVALID_CREDENTIALS, "user");
    assertTrue(auth.getErrorCode().startsWith("SEC-"));
    assertTrue(errorCodes.add(auth.getErrorCode().substring(0, 3))); // SEC

    // Game state errors
    StateCorruptionError corruption = new StateCorruptionError(StateCorruptionError.CorruptionType.DATA_STRUCTURE, "component", "details");
    assertTrue(corruption.getErrorCode().startsWith("GST-"));
    assertTrue(errorCodes.add(corruption.getErrorCode().substring(0, 3))); // GST

    // Verify uniqueness across all error types
    assertEquals("Error code prefixes must be unique", 6, errorCodes.size());
}
```

**Expected Results**:

- Error codes follow format: `{CATEGORY}-{TIMESTAMP}-{SEQUENCE}`
- All error codes are unique and traceable
- Error codes are human-readable and meaningful

#### AC-4.1.3: Contextual Information

**Criteria**: Each error must include relevant context for debugging and recovery

**Test Procedure**:

```java
@Test
public void testErrorContext() {
    // Test connection timeout context
    ConnectionTimeoutError timeout = new ConnectionTimeoutError("localhost", 7777, 5000);
    Map<String, Object> context = timeout.getContext();
    assertEquals("localhost", context.get("host"));
    assertEquals(7777, context.get("port"));
    assertEquals(5000, context.get("timeoutMs"));

    // Test authentication error context
    AuthenticationError auth = new AuthenticationError(AuthenticationError.Reason.INVALID_CREDENTIALS, "testuser");
    context = auth.getContext();
    assertEquals(AuthenticationError.Reason.INVALID_CREDENTIALS, context.get("reason"));
    assertEquals("testuser", context.get("username"));

    // Test state corruption context
    StateCorruptionError corruption = new StateCorruptionError(
        StateCorruptionError.CorruptionType.PLAYER_DATA, "component", "details");
    context = corruption.getContext();
    assertEquals(StateCorruptionError.CorruptionType.PLAYER_DATA, context.get("corruptionType"));
    assertEquals("component", context.get("affectedComponent"));
    assertEquals("details", context.get("diagnosticInfo"));
}
```

**Expected Results**:

- Context maps contain all relevant debugging information
- Context is strongly typed and structured
- Context includes timestamps and system state information

#### AC-4.1.4: Recovery Strategy Recommendations

**Criteria**: Each error must provide appropriate recovery strategy recommendations

**Test Procedure**:

```java
@Test
public void testRecoveryStrategies() {
    // Recoverable errors should suggest retry
    ConnectionTimeoutError timeout = new ConnectionTimeoutError("localhost", 7777, 5000);
    assertEquals(NetworkError.RecoveryStrategy.RETRY, timeout.getRecommendedRecoveryStrategy());
    assertTrue(timeout.isRecoverable());

    // Security errors may require user intervention
    AuthenticationError auth = new AuthenticationError(AuthenticationError.Reason.INVALID_CREDENTIALS, "user");
    assertEquals(NetworkError.RecoveryStrategy.USER_INTERVENTION, auth.getRecommendedRecoveryStrategy());

    // Critical errors should not be recoverable
    StateCorruptionError corruption = new StateCorruptionError(
        StateCorruptionError.CorruptionType.DATA_STRUCTURE, "component", "details");
    assertEquals(NetworkError.RecoveryStrategy.NONE, corruption.getRecommendedRecoveryStrategy());
    assertFalse(corruption.isRecoverable());
}
```

**Expected Results**:

- Recovery strategies are appropriate for each error type
- Critical errors are marked as non-recoverable
- Temporary errors suggest retry strategies

#### AC-4.1.5: Error Router Integration

**Criteria**: ErrorRouter must correctly classify and route errors to appropriate handlers

**Test Procedure**:

```java
@Test
public void testErrorRouting() {
    ErrorRouter router = new ErrorRouter();
    TestErrorObserver observer = new TestErrorObserver();
    TestRecoveryStrategy strategy = new TestRecoveryStrategy();

    router.addObserver(observer);
    router.addRecoveryStrategy(strategy);

    // Route different error types
    ConnectionTimeoutError timeout = new ConnectionTimeoutError("localhost", 7777, 5000);
    AuthenticationError auth = new AuthenticationError(AuthenticationError.Reason.INVALID_CREDENTIALS, "user");

    router.routeError(timeout);
    router.routeError(auth);

    // Verify observer received errors
    assertEquals(2, observer.getObservedErrors().size());
    assertEquals(timeout, observer.getObservedErrors().get(0));
    assertEquals(auth, observer.getObservedErrors().get(1));

    // Verify statistics
    ErrorRouter.ErrorStatistics stats = router.getStatistics();
    assertEquals(2, stats.getTotalErrors());
    assertEquals(1, stats.getErrorsByType().get(ConnectionError.class.getSimpleName()));
    assertEquals(1, stats.getErrorsByType().get(SecurityError.class.getSimpleName()));
}
```

**Expected Results**:

- All errors are properly routed to observers
- Error statistics are accurately maintained
- Recovery strategies are invoked for recoverable errors

---

## Task 4.2: Structured Logging System

### Overview

Implements JSON-based structured logging with performance monitoring, log analysis tools, and SLF4J integration.

### Acceptance Criteria

#### AC-4.2.1: JSON Log Format

**Criteria**: All network events must be logged in valid JSON format with required fields

**Test Procedure**:

```java
@Test
public void testJSONLogFormat() throws Exception {
    // Configure JSON logging
    NetworkEventLogger logger = NetworkEventLogger.getInstance();

    // Create test output stream
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    // Log a network event
    Map<String, Object> eventData = new HashMap<>();
    eventData.put("eventType", "CONNECTION_ESTABLISHED");
    eventData.put("remoteHost", "localhost");
    eventData.put("port", 7777);
    eventData.put("duration", 150L);

    logger.logEvent("NETWORK", "Connection established successfully", eventData);

    // Capture and parse log output
    String logOutput = outputStream.toString();
    ObjectMapper mapper = new ObjectMapper();
    JsonNode logEntry = mapper.readTree(logOutput.split("\n")[0]);

    // Verify required fields
    assertTrue(logEntry.has("timestamp"));
    assertTrue(logEntry.has("level"));
    assertTrue(logEntry.has("component"));
    assertTrue(logEntry.has("message"));
    assertTrue(logEntry.has("eventData"));

    // Verify event data structure
    JsonNode eventNode = logEntry.get("eventData");
    assertEquals("CONNECTION_ESTABLISHED", eventNode.get("eventType").asText());
    assertEquals("localhost", eventNode.get("remoteHost").asText());
    assertEquals(7777, eventNode.get("port").asInt());
    assertEquals(150L, eventNode.get("duration").asLong());
}
```

**Expected Results**:

- All log entries are valid JSON objects
- Required fields (timestamp, level, component, message) are present
- Event data is properly structured and typed

#### AC-4.2.2: Performance Metrics Integration

**Criteria**: Performance metrics must be automatically captured and logged with sub-millisecond precision

**Test Procedure**:

```java
@Test
public void testPerformanceMetrics() throws Exception {
    NetworkMetrics metrics = NetworkMetrics.getInstance();

    // Start timing a network operation
    long startTime = System.nanoTime();
    NetworkMetrics.Timer timer = metrics.startTimer("CONNECTION_ATTEMPT");

    // Simulate network operation
    Thread.sleep(100);

    // Stop timer and capture metrics
    timer.stop();
    long endTime = System.nanoTime();

    // Verify metrics collection
    NetworkMetrics.MetricsSummary summary = metrics.getSummary();
    assertTrue(summary.getOperationCount("CONNECTION_ATTEMPT") > 0);

    long recordedDuration = summary.getAverageDuration("CONNECTION_ATTEMPT");
    assertTrue("Duration should be >= 100ms", recordedDuration >= 100_000_000); // 100ms in nanoseconds
    assertTrue("Duration should be < 200ms", recordedDuration < 200_000_000); // 200ms in nanoseconds

    // Verify sub-millisecond precision
    assertTrue("Should have nanosecond precision", recordedDuration % 1_000_000 != 0);
}
```

**Expected Results**:

- Performance timers capture durations with nanosecond precision
- Metrics are automatically integrated into log entries
- Performance data is available for analysis

#### AC-4.2.3: Log Analysis Tools

**Criteria**: LogAnalyzer must parse and analyze structured logs with filtering capabilities

**Test Procedure**:

```java
@Test
public void testLogAnalysis() throws Exception {
    // Create test log data
    String testLog = """
        {"timestamp":"2025-06-16T13:30:00.123Z","level":"INFO","component":"NETWORK","message":"Connection established","eventData":{"eventType":"CONNECTION_ESTABLISHED","duration":150}}
        {"timestamp":"2025-06-16T13:30:01.456Z","level":"ERROR","component":"NETWORK","message":"Connection failed","eventData":{"eventType":"CONNECTION_FAILED","error":"TIMEOUT"}}
        {"timestamp":"2025-06-16T13:30:02.789Z","level":"INFO","component":"GAME","message":"Game state updated","eventData":{"eventType":"STATE_UPDATE","playerId":1}}
        """;

    LogAnalyzer analyzer = new LogAnalyzer();
    List<LogAnalyzer.LogEntry> entries = analyzer.parseLogData(new StringReader(testLog));

    // Verify parsing
    assertEquals(3, entries.size());

    // Test filtering by component
    List<LogAnalyzer.LogEntry> networkEntries = analyzer.filterByComponent(entries, "NETWORK");
    assertEquals(2, networkEntries.size());

    // Test filtering by level
    List<LogAnalyzer.LogEntry> errorEntries = analyzer.filterByLevel(entries, "ERROR");
    assertEquals(1, errorEntries.size());
    assertEquals("Connection failed", errorEntries.get(0).getMessage());

    // Test time-based filtering
    Instant startTime = Instant.parse("2025-06-16T13:30:01.000Z");
    Instant endTime = Instant.parse("2025-06-16T13:30:03.000Z");
    List<LogAnalyzer.LogEntry> timeFiltered = analyzer.filterByTimeRange(entries, startTime, endTime);
    assertEquals(2, timeFiltered.size());
}
```

**Expected Results**:

- LogAnalyzer correctly parses JSON log entries
- Filtering works by component, level, and time range
- Analysis tools provide insights into system behavior

#### AC-4.2.4: Log Query Engine

**Criteria**: LogQueryEngine must support complex queries across log files

**Test Procedure**:

```java
@Test
public void testLogQueryEngine() throws Exception {
    LogQueryEngine queryEngine = new LogQueryEngine();

    // Create test log file
    File tempLogFile = File.createTempFile("test", ".log");
    try (FileWriter writer = new FileWriter(tempLogFile)) {
        writer.write("""
            {"timestamp":"2025-06-16T13:30:00.123Z","level":"INFO","component":"NETWORK","message":"Connection established","eventData":{"eventType":"CONNECTION_ESTABLISHED","duration":150}}
            {"timestamp":"2025-06-16T13:30:01.456Z","level":"ERROR","component":"NETWORK","message":"Connection failed","eventData":{"eventType":"CONNECTION_FAILED","error":"TIMEOUT"}}
            {"timestamp":"2025-06-16T13:30:02.789Z","level":"INFO","component":"GAME","message":"Game state updated","eventData":{"eventType":"STATE_UPDATE","playerId":1}}
            """);
    }

    // Build complex query
    LogQueryEngine.LogQuery query = LogQueryEngine.LogQuery.builder()
        .component("NETWORK")
        .level("ERROR")
        .eventType("CONNECTION_FAILED")
        .timeRange(Instant.parse("2025-06-16T13:30:00.000Z"), Instant.parse("2025-06-16T13:31:00.000Z"))
        .build();

    // Execute query
    LogQueryEngine.QueryResult result = queryEngine.executeQuery(tempLogFile, query);

    // Verify results
    assertEquals(1, result.getMatchingEntries().size());
    LogAnalyzer.LogEntry entry = result.getMatchingEntries().get(0);
    assertEquals("Connection failed", entry.getMessage());
    assertEquals("ERROR", entry.getLevel());
    assertEquals("NETWORK", entry.getComponent());

    // Verify query statistics
    assertEquals(3, result.getTotalEntriesScanned());
    assertEquals(1, result.getMatchingCount());
    assertTrue(result.getQueryDuration() > 0);

    tempLogFile.delete();
}
```

**Expected Results**:

- Complex queries execute successfully
- Query results are accurate and complete
- Query performance is tracked and reported

#### AC-4.2.5: SLF4J Integration

**Criteria**: Existing java.util.logging usage must be bridged to SLF4J without code changes

**Test Procedure**:

```java
@Test
public void testSLF4JBridge() {
    // Initialize logging bridge
    LoggingBridge.initialize();

    // Use standard java.util.logging
    java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger("TestLogger");

    // Capture SLF4J output
    TestLogAppender appender = new TestLogAppender();

    // Log with JUL - should be bridged to SLF4J
    julLogger.info("Test message from JUL");
    julLogger.warning("Warning message from JUL");
    julLogger.severe("Error message from JUL");

    // Verify messages were bridged
    List<String> logMessages = appender.getMessages();
    assertTrue(logMessages.stream().anyMatch(msg -> msg.contains("Test message from JUL")));
    assertTrue(logMessages.stream().anyMatch(msg -> msg.contains("Warning message from JUL")));
    assertTrue(logMessages.stream().anyMatch(msg -> msg.contains("Error message from JUL")));

    // Verify log levels were mapped correctly
    assertTrue(logMessages.stream().anyMatch(msg -> msg.contains("INFO")));
    assertTrue(logMessages.stream().anyMatch(msg -> msg.contains("WARN")));
    assertTrue(logMessages.stream().anyMatch(msg -> msg.contains("ERROR")));
}
```

**Expected Results**:

- JUL messages are automatically bridged to SLF4J
- Log levels are correctly mapped
- No code changes required for existing logging

#### AC-4.2.6: Performance Requirements

**Criteria**: Logging overhead must be < 1ms per entry under normal load

**Test Procedure**:

```java
@Test
public void testLoggingPerformance() {
    NetworkEventLogger logger = NetworkEventLogger.getInstance();
    int iterations = 1000;

    // Warm up JVM
    for (int i = 0; i < 100; i++) {
        logger.logEvent("TEST", "Warmup message", Collections.emptyMap());
    }

    // Measure logging performance
    long startTime = System.nanoTime();

    for (int i = 0; i < iterations; i++) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("iteration", i);
        eventData.put("timestamp", System.currentTimeMillis());
        eventData.put("data", "test data " + i);

        logger.logEvent("PERFORMANCE_TEST", "Performance test message " + i, eventData);
    }

    long endTime = System.nanoTime();
    long totalDuration = endTime - startTime;
    long averageDurationPerEntry = totalDuration / iterations;

    // Verify performance requirement: < 1ms per entry
    assertTrue("Average logging time should be < 1ms per entry. Actual: " +
               (averageDurationPerEntry / 1_000_000.0) + "ms",
               averageDurationPerEntry < 1_000_000); // 1ms in nanoseconds

    System.out.println("Average logging time per entry: " + (averageDurationPerEntry / 1_000_000.0) + "ms");
}
```

**Expected Results**:

- Average logging time is less than 1ms per entry
- Performance is consistent under load
- No significant memory leaks during extended logging

---

## Task 4.3: Error Recovery and User Feedback

### Overview

Implements automatic error recovery with user notification system and 5-second recovery target for common errors.

### Acceptance Criteria

#### AC-4.3.1: Automatic Error Recovery

**Criteria**: System must automatically attempt recovery for common network errors without user intervention

**Test Procedure**:

```java
@Test
public void testAutomaticErrorRecovery() throws Exception {
    ErrorRouter errorRouter = new ErrorRouter();
    TestUserNotificationManager notificationManager = new TestUserNotificationManager();
    ErrorRecoveryManager recoveryManager = new ErrorRecoveryManager(errorRouter, notificationManager);

    // Set up test recovery strategy
    TestRecoveryStrategy strategy = new TestRecoveryStrategy();
    strategy.setShouldSucceed(true);
    strategy.setCanRecoverResult(true);
    errorRouter.addRecoveryStrategy(strategy);

    // Create recoverable error
    ConnectionTimeoutError error = new ConnectionTimeoutError("localhost", 7777, 5000);

    // Attempt recovery
    CompletableFuture<Optional<RecoveryResult>> future = recoveryManager.handleError(error);
    Optional<RecoveryResult> result = future.get(10, TimeUnit.SECONDS);

    // Verify automatic recovery succeeded
    assertTrue("Recovery should have succeeded", result.isPresent());
    assertTrue("Recovery result should indicate success", result.get().isSuccess());
    assertEquals(1, strategy.getRecoveryAttempts());

    // Verify user was notified
    assertTrue("User should be notified of recovery progress", notificationManager.wasNotified());
    List<String> notifications = notificationManager.getNotifications();
    assertTrue("Should notify recovery success",
               notifications.stream().anyMatch(n -> n.contains("Recovery successful")));
}
```

**Expected Results**:

- Common errors trigger automatic recovery attempts
- Recovery succeeds when possible
- User receives progress notifications

#### AC-4.3.2: Progressive Recovery Strategies

**Criteria**: System must try multiple recovery strategies in priority order

**Test Procedure**:

```java
@Test
public void testProgressiveRecoveryStrategies() throws Exception {
    ErrorRouter errorRouter = new ErrorRouter();
    TestUserNotificationManager notificationManager = new TestUserNotificationManager();
    ErrorRecoveryManager recoveryManager = new ErrorRecoveryManager(errorRouter, notificationManager);

    // Set up multiple recovery strategies with different priorities
    TestRecoveryStrategy primaryStrategy = new TestRecoveryStrategy("Primary", 80);
    TestRecoveryStrategy fallbackStrategy = new TestRecoveryStrategy("Fallback", 60);
    TestRecoveryStrategy lastResortStrategy = new TestRecoveryStrategy("LastResort", 40);

    // Primary fails, fallback succeeds
    primaryStrategy.setShouldSucceed(false);
    primaryStrategy.setCanRecoverResult(true);
    fallbackStrategy.setShouldSucceed(true);
    fallbackStrategy.setCanRecoverResult(true);
    lastResortStrategy.setShouldSucceed(true);
    lastResortStrategy.setCanRecoverResult(true);

    errorRouter.addRecoveryStrategy(primaryStrategy);
    errorRouter.addRecoveryStrategy(fallbackStrategy);
    errorRouter.addRecoveryStrategy(lastResortStrategy);

    // Attempt recovery
    ConnectionTimeoutError error = new ConnectionTimeoutError("localhost", 7777, 5000);
    CompletableFuture<Optional<RecoveryResult>> future = recoveryManager.handleError(error);
    Optional<RecoveryResult> result = future.get(10, TimeUnit.SECONDS);

    // Verify progressive strategy execution
    assertTrue("Recovery should succeed with fallback strategy", result.isPresent());
    assertTrue("Recovery result should indicate success", result.get().isSuccess());

    // Verify strategies were tried in priority order
    assertEquals("Primary strategy should be attempted", 1, primaryStrategy.getRecoveryAttempts());
    assertEquals("Fallback strategy should be attempted", 1, fallbackStrategy.getRecoveryAttempts());
    assertEquals("Last resort should not be needed", 0, lastResortStrategy.getRecoveryAttempts());

    // Verify user notifications about strategy progression
    List<String> notifications = notificationManager.getNotifications();
    assertTrue("Should notify about trying alternative recovery",
               notifications.stream().anyMatch(n -> n.contains("Trying alternative recovery")));
}
```

**Expected Results**:

- Multiple strategies are attempted in priority order
- Recovery stops when a strategy succeeds
- User is informed about strategy progression

#### AC-4.3.3: User Choice Integration

**Criteria**: For certain error types, users must be able to choose recovery options

**Test Procedure**:

```java
@Test
public void testUserChoiceIntegration() throws Exception {
    ErrorRouter errorRouter = new ErrorRouter();
    TestUserNotificationManager notificationManager = new TestUserNotificationManager();
    ErrorRecoveryManager recoveryManager = new ErrorRecoveryManager(errorRouter, notificationManager);

    // Enable user choice for security errors
    recoveryManager.enableUserChoiceForErrorType(SecurityError.class);

    // Set up user choice response
    notificationManager.setUserChoice("RETRY_WITH_CREDENTIALS");

    // Create authentication error
    AuthenticationError error = new AuthenticationError(AuthenticationError.Reason.INVALID_CREDENTIALS, "testuser");

    // Attempt recovery with user choice
    CompletableFuture<Optional<RecoveryResult>> future = recoveryManager.handleError(error);

    // Allow time for user choice prompt
    Thread.sleep(100);

    // Verify user was prompted for choice
    assertTrue("User should be prompted for choice", notificationManager.wasUserChoiceRequested());
    assertEquals("How would you like to handle this authentication error?",
                notificationManager.getLastUserChoicePrompt());

    // Complete recovery
    Optional<RecoveryResult> result = future.get(5, TimeUnit.SECONDS);
    assertTrue("Recovery should complete", result.isPresent());
}
```

**Expected Results**:

- Users are prompted for choices on appropriate errors
- User selections are integrated into recovery process
- Choice prompts are clear and actionable

#### AC-4.3.4: Recovery Performance Target

**Criteria**: Common errors must be recovered within 5 seconds

**Test Procedure**:

```java
@Test
public void testRecoveryPerformanceTarget() throws Exception {
    ErrorRouter errorRouter = new ErrorRouter();
    TestUserNotificationManager notificationManager = new TestUserNotificationManager();
    ErrorRecoveryManager recoveryManager = new ErrorRecoveryManager(errorRouter, notificationManager);

    // Set up fast recovery strategy
    TestRecoveryStrategy strategy = new TestRecoveryStrategy();
    strategy.setShouldSucceed(true);
    strategy.setCanRecoverResult(true);
    strategy.setRecoveryDelay(1000); // 1 second simulated recovery time
    errorRouter.addRecoveryStrategy(strategy);

    // Test recovery time for common error
    ConnectionTimeoutError error = new ConnectionTimeoutError("localhost", 7777, 5000);

    long startTime = System.currentTimeMillis();
    CompletableFuture<Optional<RecoveryResult>> future = recoveryManager.handleError(error);
    Optional<RecoveryResult> result = future.get(10, TimeUnit.SECONDS);
    long totalTime = System.currentTimeMillis() - startTime;

    // Verify performance target
    assertTrue("Recovery should complete within 5 seconds. Actual: " + totalTime + "ms",
               totalTime < 5000);
    assertTrue("Recovery should succeed", result.isPresent());
    assertTrue("Recovery result should indicate success", result.get().isSuccess());

    // Verify recovery duration is tracked
    assertTrue("Recovery duration should be tracked", result.get().getDurationMs() >= 1000);
    assertTrue("Recovery duration should be reasonable", result.get().getDurationMs() < 2000);
}
```

**Expected Results**:

- Recovery completes within 5-second target
- Performance is tracked and reported
- Common errors meet performance requirements

#### AC-4.3.5: User Notification System

**Criteria**: Users must receive clear, prioritized notifications about errors and recovery progress

**Test Procedure**:

```java
@Test
public void testUserNotificationSystem() {
    TestNotificationRenderer renderer = new TestNotificationRenderer();
    TestUserInteractionHandler interactionHandler = new TestUserInteractionHandler();
    NetworkErrorNotificationManager notificationManager =
        new NetworkErrorNotificationManager(renderer, interactionHandler);

    // Test priority-based notifications
    notificationManager.notifyUser("Low priority message",
                                   NetworkErrorNotificationManager.NotificationPriority.LOW);
    notificationManager.notifyUser("Critical system error",
                                   NetworkErrorNotificationManager.NotificationPriority.CRITICAL);
    notificationManager.notifyUser("Normal information",
                                   NetworkErrorNotificationManager.NotificationPriority.NORMAL);

    List<TestNotificationRenderer.TestNotification> notifications = renderer.getDisplayedNotifications();
    assertEquals(3, notifications.size());

    // Verify priority ordering (CRITICAL, NORMAL, LOW)
    assertEquals(NetworkErrorNotificationManager.NotificationPriority.CRITICAL,
                notifications.get(0).getPriority());
    assertEquals(NetworkErrorNotificationManager.NotificationPriority.NORMAL,
                notifications.get(1).getPriority());
    assertEquals(NetworkErrorNotificationManager.NotificationPriority.LOW,
                notifications.get(2).getPriority());

    // Test progress tracking
    String operationId = notificationManager.startProgressTracking("Reconnecting to server...");
    notificationManager.updateProgress(operationId, 25, "Establishing connection...");
    notificationManager.updateProgress(operationId, 50, "Authenticating...");
    notificationManager.updateProgress(operationId, 75, "Synchronizing game state...");
    notificationManager.completeProgress(operationId, "Reconnection successful!");

    List<TestNotificationRenderer.ProgressUpdate> progressUpdates = renderer.getProgressUpdates();
    assertEquals(4, progressUpdates.size()); // 3 updates + 1 completion
    assertEquals(25, progressUpdates.get(0).getProgress());
    assertEquals(100, progressUpdates.get(3).getProgress());

    // Test user interaction
    interactionHandler.setNextChoice("Retry");
    String choice = notificationManager.requestUserChoice(
        "Connection failed. How would you like to proceed?",
        Arrays.asList("Retry", "Skip", "Abort"));
    assertEquals("Retry", choice);
    assertTrue(interactionHandler.wasChoiceRequested());
}
```

**Expected Results**:

- Notifications are displayed in priority order
- Progress tracking works correctly
- User interactions are properly handled

#### AC-4.3.6: Error Reporting for Debugging

**Criteria**: System must generate comprehensive error reports for debugging purposes

**Test Procedure**:

```java
@Test
public void testErrorReporting() throws Exception {
    ErrorRouter errorRouter = new ErrorRouter();
    TestUserNotificationManager notificationManager = new TestUserNotificationManager();
    ErrorRecoveryManager recoveryManager = new ErrorRecoveryManager(errorRouter, notificationManager);

    // Set up failing recovery strategy
    TestRecoveryStrategy strategy = new TestRecoveryStrategy();
    strategy.setShouldSucceed(false);
    strategy.setCanRecoverResult(true);
    errorRouter.addRecoveryStrategy(strategy);

    // Create error with cause
    ConnectionTimeoutError error = new ConnectionTimeoutError("localhost", 7777, 5000,
        new RuntimeException("Underlying network failure"));

    // Attempt recovery
    CompletableFuture<Optional<RecoveryResult>> future = recoveryManager.handleError(error);
    future.get(5, TimeUnit.SECONDS);

    // Verify error report was generated
    ErrorRecoveryManager.ErrorReport report = recoveryManager.getLastErrorReport();
    assertNotNull("Error report should be generated", report);
    assertEquals("Error code should match", error.getErrorCode(), report.getErrorCode());
    assertNotNull("Stack trace should be included", report.getStackTrace());
    assertTrue("Stack trace should contain cause", report.getStackTrace().contains("RuntimeException"));
    assertNotNull("System info should be included", report.getSystemInfo());
    assertTrue("Recovery attempts should be tracked", report.getRecoveryAttempts() > 0);
    assertFalse("Recovery should have failed", report.isRecoverySuccessful());

    // Verify system information is comprehensive
    Map<String, Object> systemInfo = report.getSystemInfo();
    assertTrue("Should include Java version", systemInfo.containsKey("javaVersion"));
    assertTrue("Should include OS info", systemInfo.containsKey("osName"));
    assertTrue("Should include memory info", systemInfo.containsKey("memoryUsage"));
    assertTrue("Should include timestamp", systemInfo.containsKey("timestamp"));
}
```

**Expected Results**:

- Comprehensive error reports are generated
- Reports include stack traces, system info, and recovery attempts
- Reports are useful for debugging and support

---

## Integration Testing

### Overview

Validates that all Phase 1 components work together seamlessly in realistic scenarios.

### Test Scenarios

#### Scenario 1: Complete Error Recovery Flow

**Objective**: Verify end-to-end error recovery from detection to resolution

**Test Steps**:

1. Simulate network connection failure during game
2. Verify error is properly classified and logged
3. Confirm automatic recovery is attempted
4. Validate user notifications are sent
5. Ensure successful recovery restores functionality
6. Check that performance metrics are captured

**Expected Results**:

- Error is classified correctly as ConnectionTimeoutError
- JSON log entry is created with all required fields
- Recovery completes within 5-second target
- User receives appropriate notifications
- Game continues without data loss

#### Scenario 2: Multi-User Error Handling

**Objective**: Test error handling when multiple users experience issues simultaneously

**Test Steps**:

1. Set up 4-player game session
2. Simulate different error types for each player
3. Verify each error is handled independently
4. Confirm no cross-contamination of recovery attempts
5. Validate concurrent notification handling

**Expected Results**:

- All errors are processed concurrently
- Recovery strategies don't interfere with each other
- All users receive appropriate notifications
- System performance remains stable

#### Scenario 3: Cascading Error Recovery

**Objective**: Test system behavior when recovery attempts trigger additional errors

**Test Steps**:

1. Simulate primary connection failure
2. Configure first recovery strategy to fail
3. Verify fallback strategy is attempted
4. Simulate fallback strategy causing authentication error
5. Confirm both errors are handled appropriately

**Expected Results**:

- Primary and secondary errors are both logged
- Recovery strategies are attempted in proper order
- User receives updates about recovery progression
- Final recovery succeeds or appropriate failure notification is sent

---

## Performance Validation

### Overview

Ensures all Phase 1 features meet specified performance requirements under various load conditions.

### Performance Tests

#### Test 1: Logging Performance Under Load

**Target**: < 1ms per log entry under 1000 entries/second

**Test Procedure**:

```java
@Test
public void testLoggingPerformanceUnderLoad() throws Exception {
    NetworkEventLogger logger = NetworkEventLogger.getInstance();
    int concurrentThreads = 10;
    int entriesPerThread = 100;
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch completionLatch = new CountDownLatch(concurrentThreads);

    List<Long> threadDurations = Collections.synchronizedList(new ArrayList<>());

    // Start concurrent logging threads
    for (int i = 0; i < concurrentThreads; i++) {
        final int threadId = i;
        new Thread(() -> {
            try {
                startLatch.await();
                long threadStart = System.nanoTime();

                for (int j = 0; j < entriesPerThread; j++) {
                    Map<String, Object> eventData = new HashMap<>();
                    eventData.put("threadId", threadId);
                    eventData.put("entryId", j);
                    eventData.put("timestamp", System.currentTimeMillis());

                    logger.logEvent("PERFORMANCE_TEST",
                                   String.format("Thread %d entry %d", threadId, j),
                                   eventData);
                }

                long threadEnd = System.nanoTime();
                threadDurations.add(threadEnd - threadStart);
                completionLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    // Start all threads simultaneously
    startLatch.countDown();

    // Wait for completion
    assertTrue("All threads should complete within 30 seconds",
               completionLatch.await(30, TimeUnit.SECONDS));

    // Calculate performance metrics
    long totalEntries = concurrentThreads * entriesPerThread;
    long maxThreadDuration = threadDurations.stream().mapToLong(Long::longValue).max().orElse(0);
    double averageTimePerEntry = (double) maxThreadDuration / entriesPerThread / 1_000_000; // Convert to ms

    System.out.println(String.format("Logged %d entries with max thread time %.2fms per entry",
                                     totalEntries, averageTimePerEntry));

    // Verify performance requirement
    assertTrue("Average time per entry should be < 1ms. Actual: " + averageTimePerEntry + "ms",
               averageTimePerEntry < 1.0);
}
```

#### Test 2: Error Recovery Performance

**Target**: 95% of common errors recovered within 5 seconds

**Test Procedure**:

```java
@Test
public void testErrorRecoveryPerformance() throws Exception {
    ErrorRouter errorRouter = new ErrorRouter();
    TestUserNotificationManager notificationManager = new TestUserNotificationManager();
    ErrorRecoveryManager recoveryManager = new ErrorRecoveryManager(errorRouter, notificationManager);

    // Set up fast recovery strategy
    TestRecoveryStrategy strategy = new TestRecoveryStrategy();
    strategy.setShouldSucceed(true);
    strategy.setCanRecoverResult(true);
    strategy.setRecoveryDelay(2000); // 2 second recovery time
    errorRouter.addRecoveryStrategy(strategy);

    int totalTests = 100;
    int successfulRecoveries = 0;
    List<Long> recoveryTimes = new ArrayList<>();

    for (int i = 0; i < totalTests; i++) {
        ConnectionTimeoutError error = new ConnectionTimeoutError("localhost", 7777 + i, 5000);

        long startTime = System.currentTimeMillis();
        CompletableFuture<Optional<RecoveryResult>> future = recoveryManager.handleError(error);
        Optional<RecoveryResult> result = future.get(10, TimeUnit.SECONDS);
        long recoveryTime = System.currentTimeMillis() - startTime;

        if (result.isPresent() && result.get().isSuccess() && recoveryTime <= 5000) {
            successfulRecoveries++;
        }
        recoveryTimes.add(recoveryTime);
    }

    double successRate = (double) successfulRecoveries / totalTests * 100;
    long averageRecoveryTime = recoveryTimes.stream().mapToLong(Long::longValue).sum() / totalTests;

    System.out.println(String.format("Recovery success rate: %.1f%%, average time: %dms",
                                     successRate, averageRecoveryTime));

    // Verify performance requirements
    assertTrue("Success rate should be >= 95%. Actual: " + successRate + "%",
               successRate >= 95.0);
    assertTrue("Average recovery time should be <= 5000ms. Actual: " + averageRecoveryTime + "ms",
               averageRecoveryTime <= 5000);
}
```

#### Test 3: Concurrent Error Handling

**Target**: Handle 50+ concurrent errors without degradation

**Test Procedure**:

```java
@Test
public void testConcurrentErrorHandling() throws Exception {
    ErrorRouter errorRouter = new ErrorRouter();
    TestUserNotificationManager notificationManager = new TestUserNotificationManager();
    ErrorRecoveryManager recoveryManager = new ErrorRecoveryManager(errorRouter, notificationManager);

    TestRecoveryStrategy strategy = new TestRecoveryStrategy();
    strategy.setShouldSucceed(true);
    strategy.setCanRecoverResult(true);
    strategy.setRecoveryDelay(100); // Fast recovery
    errorRouter.addRecoveryStrategy(strategy);

    int concurrentErrors = 50;
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch completionLatch = new CountDownLatch(concurrentErrors);
    List<CompletableFuture<Optional<RecoveryResult>>> futures = new ArrayList<>();

    // Submit concurrent errors
    for (int i = 0; i < concurrentErrors; i++) {
        final int errorId = i;
        futures.add(CompletableFuture.supplyAsync(() -> {
            try {
                startLatch.await();
                ConnectionTimeoutError error = new ConnectionTimeoutError("host" + errorId, 7777 + errorId, 5000);
                return recoveryManager.handleError(error).get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                return Optional.<RecoveryResult>empty();
            } finally {
                completionLatch.countDown();
            }
        }));
    }

    long startTime = System.currentTimeMillis();
    startLatch.countDown();

    // Wait for all to complete
    assertTrue("All errors should be processed within 30 seconds",
               completionLatch.await(30, TimeUnit.SECONDS));
    long totalTime = System.currentTimeMillis() - startTime;

    // Verify all recoveries completed
    int successfulRecoveries = 0;
    for (CompletableFuture<Optional<RecoveryResult>> future : futures) {
        Optional<RecoveryResult> result = future.get();
        if (result.isPresent() && result.get().isSuccess()) {
            successfulRecoveries++;
        }
    }

    System.out.println(String.format("Processed %d concurrent errors in %dms, %d successful",
                                     concurrentErrors, totalTime, successfulRecoveries));

    // Verify performance requirements
    assertEquals("All errors should be processed successfully", concurrentErrors, successfulRecoveries);
    assertTrue("Total processing time should be reasonable", totalTime < 10000); // 10 seconds max
}
```

---

## Regression Testing

### Overview

Ensures Phase 1 implementation doesn't break existing Forge functionality.

### Test Categories

#### Game Engine Tests

- Verify core game mechanics still function correctly
- Confirm deck loading and card interactions work
- Test AI opponent behavior is unchanged
- Validate game state management

#### User Interface Tests

- Check that all existing UI elements display correctly
- Verify menu navigation and game controls
- Test settings and preferences functionality
- Confirm visual themes and layouts

#### Data Persistence Tests

- Validate save/load game functionality
- Test configuration file handling
- Verify deck import/export works
- Check user statistics tracking

#### Network Infrastructure Tests

- Confirm existing network code still compiles
- Test that network interfaces are preserved
- Verify no performance degradation in non-multiplayer modes

### Test Execution

```bash
# Run full regression test suite
mvn clean test -P windows-linux

# Run specific module tests
mvn test -pl forge-core
mvn test -pl forge-game
mvn test -pl forge-gui

# Verify build integrity
mvn clean compile -P windows-linux
```

### Success Criteria

- All existing tests continue to pass
- No new compilation errors or warnings
- Application starts and basic functionality works
- Performance characteristics are maintained

---

## Test Reporting and Documentation

### Test Results Format

Each test execution should generate:

- **Test Summary Report**: Pass/fail counts, execution time, coverage metrics
- **Error Analysis**: Details of any failures with stack traces and context
- **Performance Report**: Timing data, throughput measurements, resource usage
- **Integration Report**: End-to-end scenario results and user experience validation

### Continuous Integration

Tests should be integrated into CI/CD pipeline:

```yaml
# Example GitHub Actions workflow
name: Phase 1 Testing
on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 17
        uses: actions/setup-java@v2
        with:
          java-version: "17"
      - name: Run Phase 1 Tests
        run: mvn clean test -P windows-linux
      - name: Generate Test Report
        run: mvn surefire-report:report
      - name: Archive Test Results
        uses: actions/upload-artifact@v2
        with:
          name: test-results
          path: target/surefire-reports/
```

### Sign-Off Criteria

Phase 1 is considered complete when:

- [ ] All acceptance criteria tests pass
- [ ] Performance requirements are met
- [ ] Integration scenarios work end-to-end
- [ ] Regression tests show no degradation
- [ ] Documentation is complete and accurate
- [ ] Code review and approval completed

This comprehensive testing approach ensures Phase 1 delivers robust, high-quality multiplayer networking foundations for Forge MTG.
