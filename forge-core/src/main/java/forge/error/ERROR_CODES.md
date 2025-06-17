# Network Error Classification and Recovery Guide

## Overview

This document provides comprehensive documentation for the network error classification system implemented in Task 4.1. The system provides structured error handling with automatic recovery strategies for the Forge multiplayer networking infrastructure.

## Error Code Format

All error codes follow the format: `TYPE-YYYYMMDD-NNNNNN`

- **TYPE**: 3-letter error type code
- **YYYYMMDD**: Date when the error occurred  
- **NNNNNN**: 6-digit unique sequence number

Example: `CON-20241216-000001`

## Error Type Codes

| Code | Type | Description |
|------|------|-------------|
| CON | Connection | Network connection-related errors |
| SER | Serialization | Data serialization/deserialization errors |
| SEC | Security | Security and authentication errors |
| GST | Game State | Game state synchronization errors |
| PRO | Protocol | Network protocol violations |
| TIM | Timeout | Operation timeout errors |
| AUT | Authentication | Authentication and authorization failures |
| APP | Application | Application-level errors |

## Error Hierarchy

### Connection Errors (CON)

#### ConnectionTimeoutError
- **Cause**: Connection attempt exceeds timeout threshold
- **Context**: host, port, timeoutMs
- **Recovery**: Retry with exponential backoff
- **User Action**: Check network connectivity

#### ConnectionRefusedError  
- **Cause**: Server actively refuses connection
- **Context**: host, port, reason
- **Recovery**: Retry with delay
- **User Action**: Verify server is running and port is correct

#### ConnectionLostError
- **Cause**: Established connection lost unexpectedly
- **Context**: reason, connectionDurationMs
- **Recovery**: Automatic reconnection
- **User Action**: Check network stability

### Security Errors (SEC)

#### AuthenticationError
- **Cause**: Login credentials invalid or expired
- **Context**: reason, username, authType
- **Recovery**: User intervention required
- **User Action**: Re-enter credentials or contact support

#### AuthorizationError
- **Cause**: Insufficient permissions for action
- **Context**: action, resource, requiredPermission
- **Recovery**: None (user needs proper permissions)
- **User Action**: Contact administrator for permission

#### ValidationError
- **Cause**: Game action validation failed
- **Context**: validationType, playerId, details
- **Recovery**: Resync or reject action
- **User Action**: Follow game rules

### Game State Errors (GST)

#### StateSyncError
- **Cause**: Client and server game states diverged
- **Context**: gameId, clientStateVersion, serverStateVersion
- **Recovery**: Automatic state resynchronization
- **User Action**: Wait for resync to complete

#### StateCorruptionError
- **Cause**: Game state data corrupted
- **Context**: corruptionType, affectedComponent, diagnosticInfo
- **Recovery**: None (requires game restart)
- **User Action**: Restart game

#### StateTransitionError
- **Cause**: Invalid game state transition attempted
- **Context**: fromState, toState, transitionType, violatedRule
- **Recovery**: Resync to valid state
- **User Action**: Follow proper game sequence

### Protocol Errors (PRO)

#### UnknownMessageError
- **Cause**: Received unsupported message type
- **Context**: messageType, protocolVersion, messageSize
- **Recovery**: Reconnect (may resolve version mismatch)
- **User Action**: Ensure client/server versions match

#### MalformedMessageError
- **Cause**: Message structure invalid or corrupted
- **Context**: messageType, parseError, expectedSize, actualSize
- **Recovery**: Request retransmission
- **User Action**: Check network stability

#### MessageSequenceError
- **Cause**: Messages received out of order
- **Context**: sequenceErrorType, expectedSequence, receivedSequence
- **Recovery**: Depends on type (retry, resync, or reconnect)
- **User Action**: Check network quality

### Application Errors (APP)

#### ResourceError
- **Cause**: System resources exhausted
- **Context**: resourceType, currentUsage, maxAvailable
- **Recovery**: Fallback to reduced functionality
- **User Action**: Free resources or upgrade system

#### ConfigurationError
- **Cause**: Invalid configuration settings
- **Context**: configurationKey, configurationValue, expectedFormat
- **Recovery**: User intervention required
- **User Action**: Fix configuration file

## Severity Levels

### INFO (Level 0)
- Informational messages
- No action required
- Examples: Successful recovery, status updates

### WARN (Level 1)  
- Warning conditions requiring attention
- May impact functionality
- Examples: Rate limiting, minor validation failures

### ERROR (Level 2)
- Error conditions impacting functionality
- Usually recoverable
- Examples: Connection failures, sync errors

### CRITICAL (Level 3)
- Critical errors that may cause system failure
- Often non-recoverable
- Examples: Security violations, data corruption

## Recovery Strategies

### Retry Strategy
- **Use Cases**: Transient errors, network timeouts
- **Mechanism**: Exponential backoff with jitter
- **Default Config**: 3 attempts, 1s initial delay, 2x multiplier
- **Max Delay**: 30 seconds

### Reconnect Strategy  
- **Use Cases**: Connection lost, server unavailable
- **Mechanism**: Full connection re-establishment
- **Default Config**: 5 attempts, 2s base delay
- **Features**: Session state preservation

### Resync Strategy
- **Use Cases**: Game state desynchronization
- **Mechanism**: Full state transfer from server
- **Default Config**: 2 attempts, 1s delay
- **Features**: Integrity validation

### Fallback Strategy
- **Use Cases**: Resource limitations, feature unavailable
- **Mechanism**: Graceful degradation
- **Default Config**: Single attempt
- **Features**: Reduced functionality mode

### No Recovery
- **Use Cases**: Critical errors, security violations
- **Mechanism**: Error logged, user notified
- **User Action Required**: Manual intervention

## Usage Examples

### Basic Error Handling

```java
try {
    // Network operation
    performNetworkOperation();
} catch (Exception e) {
    NetworkError error = ErrorClassifier.classify(e);
    errorRouter.routeError(error);
}
```

### Custom Recovery Context

```java
RecoveryContext context = new RecoveryContext(
    connectionManager, 
    gameStateManager, 
    configProvider
);

errorRouter.routeError(error, Collections.emptyMap(), context);
```

### Error Observer Implementation

```java
public class GameErrorHandler implements ErrorRouter.ErrorObserver {
    @Override
    public void onErrorOccurred(NetworkError error, Map<String, Object> context) {
        // Log error
        logger.log(error.getSeverity().getLevel(), error.getTechnicalMessage());
        
        // Show user message for severe errors
        if (error.getSeverity().isMoreSevereThan(Severity.WARN)) {
            showUserNotification(error.getUserMessage());
        }
    }
    
    @Override
    public void onRecoveryCompleted(NetworkError error, ErrorRecoveryStrategy strategy, 
                                   RecoveryResult result, RecoveryContext context) {
        if (result.isSuccess()) {
            showUserNotification("Connection restored");
        } else if (!result.canRetry()) {
            showUserNotification("Unable to recover: " + result.getMessage());
        }
    }
}
```

### Custom Recovery Strategy

```java
public class CustomRecoveryStrategy implements ErrorRecoveryStrategy {
    @Override
    public boolean canRecover(NetworkError error) {
        return error instanceof MyCustomError && error.isRecoverable();
    }
    
    @Override
    public CompletableFuture<RecoveryResult> attemptRecovery(NetworkError error, RecoveryContext context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                performCustomRecovery(error, context);
                return RecoveryResult.success("Custom recovery successful", 1000);
            } catch (Exception e) {
                return RecoveryResult.failure("Custom recovery failed", 1000, e);
            }
        });
    }
    
    @Override
    public RecoveryStrategy getRecoveryType() {
        return RecoveryStrategy.FALLBACK;
    }
}
```

## Error Statistics and Monitoring

### Error Statistics

The `ErrorRouter` automatically tracks statistics for each error type:

- **Count**: Total number of occurrences
- **Recovery Attempts**: Number of recovery attempts
- **Recovery Successes**: Number of successful recoveries
- **Success Rate**: Percentage of successful recoveries
- **First/Last Occurrence**: Timestamps
- **Highest Severity**: Most severe error seen

### Accessing Statistics

```java
// Get statistics for specific error type
Optional<ErrorStatistics> stats = errorRouter.getStatistics("ConnectionTimeoutError");
if (stats.isPresent()) {
    System.out.println("Success rate: " + stats.get().getRecoverySuccessRate());
}

// Get overall system statistics
Map<String, Object> overallStats = errorRouter.getOverallStatistics();
System.out.println("Total errors handled: " + overallStats.get("totalErrorsHandled"));
```

## Best Practices

### Error Handling
1. Always classify exceptions using `ErrorClassifier`
2. Provide meaningful context when routing errors
3. Use appropriate recovery contexts for automated recovery
4. Log all errors with appropriate severity levels

### Recovery Strategy Design
1. Implement idempotent recovery operations
2. Use appropriate timeouts and retry limits
3. Provide clear user feedback during recovery
4. Design for graceful degradation when recovery fails

### Performance Considerations
1. Recovery operations should be asynchronous
2. Avoid blocking the main game thread during recovery
3. Monitor recovery success rates and adjust strategies
4. Clear error statistics periodically to prevent memory growth

### Security Considerations  
1. Never log sensitive information in error messages
2. Filter error details before sending to remote clients
3. Rate limit error reporting to prevent denial of service
4. Validate all error context data before processing

## Integration with Existing Systems

### Phase 1 Integration
The error classification system integrates with existing Phase 1 components:

- **Serialization**: Enhances existing `SerializationException` handling
- **Connection Management**: Works with `ReconnectionManager`
- **State Recovery**: Integrates with `GameStateRecoveryManager`

### Phase 2 Preparation
The system is designed to support Phase 2 multiplayer features:

- **Remote PlayerController**: Error handling for remote player actions
- **Real-time Sync**: State error detection and recovery
- **Draft System**: Multiplayer draft error handling
- **Spectator Mode**: Error handling for observer connections

## Troubleshooting Guide

### Common Issues

**High Connection Error Rates**
- Check network stability
- Verify server availability
- Review connection timeout settings
- Monitor server resource usage

**Frequent State Sync Errors**
- Check for game logic bugs
- Verify serialization compatibility  
- Monitor network packet loss
- Review game state complexity

**Security Validation Failures**
- Check for client tampering
- Verify message integrity
- Review authentication flow
- Monitor for potential attacks

**Recovery Strategy Failures**
- Review strategy implementations
- Check recovery timeouts
- Verify context availability
- Monitor resource constraints