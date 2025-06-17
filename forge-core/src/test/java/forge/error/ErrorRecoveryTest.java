package forge.error;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import static org.testng.Assert.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit tests for error recovery strategies and related components.
 */
public class ErrorRecoveryTest {
    
    private RecoveryContext testContext;
    private ConnectionTimeoutError testError;
    
    @BeforeMethod
    public void setUp() {
        testContext = new RecoveryContext(null, null, null);
        testError = new ConnectionTimeoutError("localhost", 8080, 5000);
    }
    
    @Test
    public void testRecoveryContextCreation() {
        String mockConnectionManager = "connection-manager";
        String mockGameStateManager = "game-state-manager";
        String mockConfigProvider = "config-provider";
        
        RecoveryContext context = new RecoveryContext(mockConnectionManager, mockGameStateManager, mockConfigProvider);
        
        assertTrue(context.getConnectionManager(String.class).isPresent());
        assertEquals(context.getConnectionManager(String.class).get(), mockConnectionManager);
        
        assertTrue(context.getGameStateManager(String.class).isPresent());
        assertEquals(context.getGameStateManager(String.class).get(), mockGameStateManager);
        
        assertTrue(context.getConfigurationProvider(String.class).isPresent());
        assertEquals(context.getConfigurationProvider(String.class).get(), mockConfigProvider);
        
        // Test wrong type
        assertFalse(context.getConnectionManager(Integer.class).isPresent());
    }
    
    @Test
    public void testRecoveryContextOperations() {
        testContext.put("key1", "value1");
        testContext.put("key2", 42);
        
        assertTrue(testContext.get("key1").isPresent());
        assertEquals(testContext.get("key1").get(), "value1");
        
        assertTrue(testContext.get("key2", Integer.class).isPresent());
        assertEquals(testContext.get("key2", Integer.class).get(), Integer.valueOf(42));
        
        assertFalse(testContext.get("nonexistent").isPresent());
        assertFalse(testContext.get("key1", Integer.class).isPresent());
        
        assertTrue(testContext.containsKey("key1"));
        assertFalse(testContext.containsKey("nonexistent"));
        
        Object removed = testContext.remove("key1");
        assertEquals(removed, "value1");
        assertFalse(testContext.containsKey("key1"));
    }
    
    @Test
    public void testRecoveryResultCreation() {
        RecoveryResult success = RecoveryResult.success("Operation succeeded", 1000);
        assertTrue(success.isSuccess());
        assertFalse(success.isFailure());
        assertFalse(success.canRetry());
        assertEquals(success.getMessage(), "Operation succeeded");
        assertEquals(success.getDurationMs(), 1000);
        assertFalse(success.getCause().isPresent());
        
        Exception cause = new RuntimeException("Test exception");
        RecoveryResult failure = RecoveryResult.failure("Operation failed", 2000, cause);
        assertFalse(failure.isSuccess());
        assertTrue(failure.isFailure());
        assertFalse(failure.canRetry());
        assertTrue(failure.getCause().isPresent());
        assertEquals(failure.getCause().get(), cause);
        
        RecoveryResult retry = RecoveryResult.retry("Need to retry", 500);
        assertFalse(retry.isSuccess());
        assertFalse(retry.isFailure());
        assertTrue(retry.canRetry());
        
        RecoveryResult partial = RecoveryResult.partialSuccess("Partially recovered", 1500, null);
        assertFalse(partial.isSuccess());
        assertTrue(partial.isPartialSuccess());
        
        RecoveryResult notApplicable = RecoveryResult.notApplicable("Strategy not applicable");
        assertEquals(notApplicable.getType(), RecoveryResult.Type.NOT_APPLICABLE);
        assertEquals(notApplicable.getDurationMs(), 0);
    }
    
    @Test
    public void testRecoveryResultWithMetadata() {
        RecoveryResult result = RecoveryResult.success("Success", 1000);
        RecoveryResult withMetadata = result.withMetadata("attempts", 3);
        
        assertTrue(withMetadata.getMetadata("attempts", Integer.class).isPresent());
        assertEquals(withMetadata.getMetadata("attempts", Integer.class).get(), Integer.valueOf(3));
        
        assertFalse(result.getMetadata("attempts").isPresent()); // Original unchanged
    }
    
    @Test
    public void testRetryRecoveryStrategy() throws ExecutionException, InterruptedException {
        AtomicInteger attempts = new AtomicInteger(0);
        AtomicBoolean shouldSucceed = new AtomicBoolean(false);
        
        RetryRecoveryStrategy strategy = new RetryRecoveryStrategy(() -> {
            attempts.incrementAndGet();
            return shouldSucceed.get();
        }, 3, 100, 2.0, 5000);
        
        assertTrue(strategy.canRecover(testError));
        assertEquals(strategy.getRecoveryType(), NetworkError.RecoveryStrategy.RETRY);
        assertEquals(strategy.getMaxAttempts(), 3);
        
        // Test failure scenario
        CompletableFuture<RecoveryResult> future = strategy.attemptRecovery(testError, testContext);
        RecoveryResult result = future.get();
        
        assertTrue(result.isFailure());
        assertEquals(attempts.get(), 3); // Should try 3 times
        
        // Test success scenario
        attempts.set(0);
        shouldSucceed.set(true);
        future = strategy.attemptRecovery(testError, testContext);
        result = future.get();
        
        assertTrue(result.isSuccess());
        assertEquals(attempts.get(), 1); // Should succeed on first try
    }
    
    @Test
    public void testReconnectRecoveryStrategy() throws ExecutionException, InterruptedException {
        AtomicBoolean reconnectSuccess = new AtomicBoolean(true);
        
        ReconnectRecoveryStrategy strategy = new ReconnectRecoveryStrategy(() -> reconnectSuccess.get(), 2, 200);
        
        assertTrue(strategy.canRecover(testError));
        assertEquals(strategy.getRecoveryType(), NetworkError.RecoveryStrategy.RECONNECT);
        assertEquals(strategy.getMaxAttempts(), 2);
        
        CompletableFuture<RecoveryResult> future = strategy.attemptRecovery(testError, testContext);
        RecoveryResult result = future.get();
        
        assertTrue(result.isSuccess());
        assertTrue(result.getMetadata("strategy", String.class).isPresent());
        assertEquals(result.getMetadata("strategy", String.class).get(), "reconnect");
    }
    
    @Test
    public void testResyncRecoveryStrategy() throws ExecutionException, InterruptedException {
        StateSyncError syncError = new StateSyncError(1, 100, 105);
        AtomicBoolean resyncSuccess = new AtomicBoolean(true);
        
        ResyncRecoveryStrategy strategy = new ResyncRecoveryStrategy(() -> resyncSuccess.get(), 2);
        
        assertTrue(strategy.canRecover(syncError));
        assertEquals(strategy.getRecoveryType(), NetworkError.RecoveryStrategy.RESYNC);
        
        CompletableFuture<RecoveryResult> future = strategy.attemptRecovery(syncError, testContext);
        RecoveryResult result = future.get();
        
        assertTrue(result.isSuccess());
        assertTrue(result.getMetadata("strategy", String.class).isPresent());
        assertEquals(result.getMetadata("strategy", String.class).get(), "resync");
    }
    
    @Test
    public void testFallbackRecoveryStrategy() throws ExecutionException, InterruptedException {
        ResourceError resourceError = new ResourceError(ResourceError.ResourceType.MEMORY, "heap");
        AtomicBoolean fallbackSuccess = new AtomicBoolean(true);
        
        FallbackRecoveryStrategy strategy = new FallbackRecoveryStrategy(
            () -> fallbackSuccess.get(), 
            "Use reduced quality mode"
        );
        
        assertFalse(strategy.canRecover(resourceError)); // FallbackRecoveryStrategy is generic, not specific to resource errors
        assertEquals(strategy.getRecoveryType(), NetworkError.RecoveryStrategy.FALLBACK);
        assertEquals(strategy.getMaxAttempts(), 1);
        
        CompletableFuture<RecoveryResult> future = strategy.attemptRecovery(resourceError, testContext);
        RecoveryResult result = future.get();
        
        // RecoveryResult.partialSuccess returns a PARTIAL_SUCCESS type, not just success
        assertEquals(result.getType(), RecoveryResult.Type.PARTIAL_SUCCESS);
        assertTrue(result.getMetadata("fallbackDescription", String.class).isPresent());
        assertEquals(result.getMetadata("fallbackDescription", String.class).get(), "Use reduced quality mode");
    }
    
    @Test
    public void testRecoveryStrategyPriorities() {
        RetryRecoveryStrategy retry = new RetryRecoveryStrategy(() -> true);
        ReconnectRecoveryStrategy reconnect = new ReconnectRecoveryStrategy(() -> true);
        ResyncRecoveryStrategy resync = new ResyncRecoveryStrategy(() -> true);
        FallbackRecoveryStrategy fallback = new FallbackRecoveryStrategy(() -> true, "fallback");
        
        assertTrue(resync.getPriority() > reconnect.getPriority());
        assertTrue(reconnect.getPriority() > retry.getPriority());
        assertTrue(retry.getPriority() > fallback.getPriority());
    }
    
    @Test
    public void testRecoveryStrategyDelayCalculation() {
        RetryRecoveryStrategy strategy = new RetryRecoveryStrategy(() -> true, 5, 1000, 2.0, 30000);
        
        assertEquals(strategy.getRetryDelayMs(1), 1000);   // First attempt
        assertEquals(strategy.getRetryDelayMs(2), 2000);   // Second attempt
        assertEquals(strategy.getRetryDelayMs(3), 4000);   // Third attempt
        assertEquals(strategy.getRetryDelayMs(4), 8000);   // Fourth attempt
        assertEquals(strategy.getRetryDelayMs(5), 16000);  // Fifth attempt
        
        // Test that it caps at max delay
        RetryRecoveryStrategy cappedStrategy = new RetryRecoveryStrategy(() -> true, 10, 1000, 2.0, 5000);
        assertEquals(cappedStrategy.getRetryDelayMs(10), 5000); // Should be capped
    }
}