package forge.gui.logging;

import forge.gui.network.NetworkEventLogger;
import forge.gui.network.NetworkMetrics;
import forge.gui.util.LoggingInitializer;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import static org.testng.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Performance validation tests for the logging system.
 * Ensures that logging operations meet the < 1ms per entry requirement.
 */
public class LoggingPerformanceTest {
    
    private NetworkEventLogger logger;
    private NetworkMetrics metrics;
    
    @BeforeClass
    public void setUpClass() {
        LoggingInitializer.initialize();
    }
    
    @BeforeMethod
    public void setUp() {
        logger = NetworkEventLogger.forComponent("PerformanceTest");
        metrics = NetworkMetrics.getInstance();
        metrics.reset();
    }
    
    @Test
    public void testBasicLoggingPerformance() {
        int iterations = 1000;
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            logger.logConnection("localhost", 7777, true, 1500);
        }
        
        long endTime = System.nanoTime();
        double averageTimeMs = (endTime - startTime) / 1_000_000.0 / iterations;
        
        assertTrue(averageTimeMs < 1.0, 
                  String.format("Basic logging took %.3fms per entry, should be < 1.0ms", averageTimeMs));
        
        System.out.printf("Basic logging performance: %.3fms per entry (target: < 1.0ms)%n", averageTimeMs);
    }
    
    @Test
    public void testStructuredLoggingPerformance() {
        int iterations = 1000;
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            logger.logEvent(NetworkEventLogger.EventType.GAME_STATE_SYNC,
                           NetworkEventLogger.Severity.INFO,
                           "Performance test entry {}", i)
                   .withField("testField", "testValue")
                   .withField("iteration", i)
                   .withField("timestamp", System.currentTimeMillis())
                   .log();
        }
        
        long endTime = System.nanoTime();
        double averageTimeMs = (endTime - startTime) / 1_000_000.0 / iterations;
        
        assertTrue(averageTimeMs < 1.0, 
                  String.format("Structured logging took %.3fms per entry, should be < 1.0ms", averageTimeMs));
        
        System.out.printf("Structured logging performance: %.3fms per entry (target: < 1.0ms)%n", averageTimeMs);
    }
    
    @Test
    public void testMetricsRecordingPerformance() {
        int iterations = 10000; // More iterations for lightweight metrics
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            metrics.recordConnectionAttempt(true, 1500);
            metrics.recordMessageSent("TEST", 1024);
            metrics.recordLatency(50);
        }
        
        long endTime = System.nanoTime();
        double averageTimeMs = (endTime - startTime) / 1_000_000.0 / iterations / 3; // 3 operations per iteration
        
        assertTrue(averageTimeMs < 0.1, 
                  String.format("Metrics recording took %.3fms per operation, should be < 0.1ms", averageTimeMs));
        
        System.out.printf("Metrics recording performance: %.3fms per operation (target: < 0.1ms)%n", averageTimeMs);
    }
    
    @Test
    public void testConcurrentLoggingPerformance() throws InterruptedException {
        int numThreads = 4;
        int iterationsPerThread = 250; // Total 1000 entries
        CountDownLatch latch = new CountDownLatch(numThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        long startTime = System.nanoTime();
        
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < iterationsPerThread; i++) {
                        logger.logEvent(NetworkEventLogger.EventType.CONNECTION,
                                       NetworkEventLogger.Severity.INFO,
                                       "Thread {} iteration {}", threadId, i)
                               .withField("threadId", threadId)
                               .withField("iteration", i)
                               .log();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Concurrent logging should complete within 10 seconds");
        
        long endTime = System.nanoTime();
        double averageTimeMs = (endTime - startTime) / 1_000_000.0 / (numThreads * iterationsPerThread);
        
        assertTrue(averageTimeMs < 1.0, 
                  String.format("Concurrent logging took %.3fms per entry, should be < 1.0ms", averageTimeMs));
        
        System.out.printf("Concurrent logging performance: %.3fms per entry (target: < 1.0ms)%n", averageTimeMs);
        
        executor.shutdown();
    }
    
    @Test
    public void testErrorLoggingPerformance() {
        int iterations = 500; // Fewer iterations as error logging includes stack traces
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            Exception testException = new RuntimeException("Performance test exception " + i);
            logger.logError("performance_test_operation", testException);
        }
        
        long endTime = System.nanoTime();
        double averageTimeMs = (endTime - startTime) / 1_000_000.0 / iterations;
        
        assertTrue(averageTimeMs < 2.0, 
                  String.format("Error logging took %.3fms per entry, should be < 2.0ms", averageTimeMs));
        
        System.out.printf("Error logging performance: %.3fms per entry (target: < 2.0ms)%n", averageTimeMs);
    }
    
    @Test
    public void testPerformanceMetricsLogging() {
        int iterations = 1000;
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            logger.logPerformanceMetric("test_metric_" + (i % 10), i * 1.5, "units");
        }
        
        long endTime = System.nanoTime();
        double averageTimeMs = (endTime - startTime) / 1_000_000.0 / iterations;
        
        assertTrue(averageTimeMs < 1.0, 
                  String.format("Performance metrics logging took %.3fms per entry, should be < 1.0ms", averageTimeMs));
        
        System.out.printf("Performance metrics logging: %.3fms per entry (target: < 1.0ms)%n", averageTimeMs);
    }
    
    @Test
    public void testMemoryUsageDuringLogging() {
        Runtime runtime = Runtime.getRuntime();
        
        // Force garbage collection and get baseline
        System.gc();
        Thread.yield();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Perform logging operations
        int iterations = 5000;
        for (int i = 0; i < iterations; i++) {
            logger.logEvent(NetworkEventLogger.EventType.CONNECTION,
                           NetworkEventLogger.Severity.INFO,
                           "Memory test entry {}", i)
                   .withField("iteration", i)
                   .withField("timestamp", System.currentTimeMillis())
                   .log();
            
            // Occasionally record metrics
            if (i % 10 == 0) {
                metrics.recordConnectionAttempt(true, 1000);
                metrics.recordMessageSent("MEMORY_TEST", 512);
            }
        }
        
        // Force garbage collection and measure final memory
        System.gc();
        Thread.yield();
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        
        long memoryIncrease = finalMemory - initialMemory;
        double memoryPerEntry = (double) memoryIncrease / iterations;
        
        System.out.printf("Memory usage: %d bytes total increase, %.1f bytes per entry%n", 
                         memoryIncrease, memoryPerEntry);
        
        // Memory increase should be reasonable (less than 1KB per entry)
        assertTrue(memoryPerEntry < 1024, 
                  String.format("Memory usage of %.1f bytes per entry is too high", memoryPerEntry));
    }
    
    @Test
    public void testLoggingUnderLoad() {
        // Simulate high-load scenario
        int iterations = 2000;
        long startTime = System.nanoTime();
        
        for (int i = 0; i < iterations; i++) {
            // Mix different types of logging
            switch (i % 4) {
                case 0:
                    logger.logConnection("server" + (i % 5), 7777 + (i % 10), i % 2 == 0, 1000 + i);
                    break;
                case 1:
                    logger.logMessageSent("MESSAGE_TYPE_" + (i % 3), 512 + i, i % 3 != 0);
                    break;
                case 2:
                    logger.logGameStateSync("OPERATION_" + (i % 2), i % 2 == 0, 100 + i, 1024 * (i % 5));
                    break;
                case 3:
                    logger.logPerformanceMetric("load_test_metric", i * 0.1, "units");
                    break;
            }
            
            // Record metrics
            metrics.recordLatency(50 + (i % 100));
            if (i % 20 == 0) {
                metrics.recordNetworkError("LOAD_TEST_ERROR");
            }
        }
        
        long endTime = System.nanoTime();
        double averageTimeMs = (endTime - startTime) / 1_000_000.0 / iterations;
        
        assertTrue(averageTimeMs < 1.5, 
                  String.format("Logging under load took %.3fms per entry, should be < 1.5ms", averageTimeMs));
        
        System.out.printf("Logging under load performance: %.3fms per entry (target: < 1.5ms)%n", averageTimeMs);
    }
    
    @Test
    public void testLoggingSystemHealthCheck() {
        // Verify the logging system is healthy and responsive
        assertTrue(LoggingInitializer.isInitialized(), "Logging system should be initialized");
        assertTrue(LoggingInitializer.healthCheck(), "Logging system should pass health check");
        
        // Test rapid successive calls
        long startTime = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            logger.logHeartbeat(true, null);
        }
        long endTime = System.nanoTime();
        
        double averageTimeMs = (endTime - startTime) / 1_000_000.0 / 100;
        assertTrue(averageTimeMs < 0.5, 
                  String.format("Rapid heartbeat logging took %.3fms per entry, should be < 0.5ms", averageTimeMs));
        
        System.out.printf("Rapid heartbeat logging: %.3fms per entry (target: < 0.5ms)%n", averageTimeMs);
    }
}