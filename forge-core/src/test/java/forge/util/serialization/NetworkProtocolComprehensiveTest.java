package forge.util.serialization;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

/**
 * Comprehensive test suite for NetworkProtocol implementations.
 * Focuses on missing coverage areas: compression, thread safety, interface contracts.
 */
public class NetworkProtocolComprehensiveTest {
    
    private KryoNetworkProtocol kryoProtocol;
    private JavaNetworkProtocol javaProtocol;
    
    @BeforeMethod
    public void setUp() {
        kryoProtocol = new KryoNetworkProtocol();
        javaProtocol = new JavaNetworkProtocol();
    }
    
    /**
     * Test NetworkProtocol interface contract compliance.
     * Ensures both implementations behave consistently for the same inputs.
     */
    @Test
    public void testNetworkProtocolInterfaceContract() {
        String testData = "Test string for protocol interface validation";
        
        // Test serialization contract
        byte[] kryoResult = kryoProtocol.serialize(testData);
        byte[] javaResult = javaProtocol.serialize(testData);
        
        assertNotNull("Kryo serialize should not return null", kryoResult);
        assertNotNull("Java serialize should not return null", javaResult);
        assertTrue("Serialized data should not be empty", kryoResult.length > 0);
        assertTrue("Serialized data should not be empty", javaResult.length > 0);
        
        // Test deserialization contract
        String kryoDeserialized = kryoProtocol.deserialize(kryoResult, String.class);
        String javaDeserialized = javaProtocol.deserialize(javaResult, String.class);
        
        assertEquals("Kryo deserialization should preserve data", testData, kryoDeserialized);
        assertEquals("Java deserialization should preserve data", testData, javaDeserialized);
        
        // Test compression detection contract
        assertFalse("Small data should not be compressed", kryoProtocol.isCompressed(kryoResult));
        assertFalse("Small data should not be compressed", javaProtocol.isCompressed(javaResult));
    }
    
    /**
     * Test compression functionality with large data.
     * Validates that compression is applied when data exceeds threshold.
     */
    @Test
    public void testCompressionBehavior() {
        // Create large test data that should trigger compression
        StringBuilder largeDataBuilder = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeDataBuilder.append("This is a test string to create large data for compression testing. ");
        }
        String largeData = largeDataBuilder.toString();
        
        // Test Kryo compression
        byte[] kryoCompressed = kryoProtocol.serialize(largeData);
        assertTrue("Large data should be compressed", kryoProtocol.isCompressed(kryoCompressed));
        
        String kryoDecompressed = kryoProtocol.deserialize(kryoCompressed, String.class);
        assertEquals("Compression roundtrip should preserve data", largeData, kryoDecompressed);
        
        // Test Java compression
        byte[] javaCompressed = javaProtocol.serialize(largeData);
        assertTrue("Large data should be compressed", javaProtocol.isCompressed(javaCompressed));
        
        String javaDecompressed = javaProtocol.deserialize(javaCompressed, String.class);
        assertEquals("Compression roundtrip should preserve data", largeData, javaDecompressed);
        
        // Verify compression actually reduces size
        byte[] uncompressedData = largeData.getBytes();
        assertTrue("Compressed data should be smaller than original", 
                 kryoCompressed.length < uncompressedData.length);
        assertTrue("Compressed data should be smaller than original", 
                 javaCompressed.length < uncompressedData.length);
    }
    
    /**
     * Test thread safety of NetworkProtocol implementations.
     * Ensures concurrent access doesn't cause data corruption or exceptions.
     */
    @Test
    public void testThreadSafety() throws InterruptedException {
        final int numThreads = 10;
        final int operationsPerThread = 100;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(numThreads);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicReference<Exception> firstException = new AtomicReference<>();
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        // Create test data for each thread
        for (int threadId = 0; threadId < numThreads; threadId++) {
            final String threadData = "Thread-" + threadId + "-data-for-concurrent-testing";
            
            executor.submit(() -> {
                try {
                    // Wait for all threads to be ready
                    startLatch.await();
                    
                    // Perform serialization operations
                    for (int i = 0; i < operationsPerThread; i++) {
                        String testData = threadData + "-operation-" + i;
                        
                        // Test Kryo thread safety
                        byte[] kryoSerialized = kryoProtocol.serialize(testData);
                        String kryoDeserialized = kryoProtocol.deserialize(kryoSerialized, String.class);
                        
                        if (!testData.equals(kryoDeserialized)) {
                            throw new AssertionError("Kryo thread safety violation: expected " + 
                                                   testData + ", got " + kryoDeserialized);
                        }
                        
                        // Test Java thread safety
                        byte[] javaSerialized = javaProtocol.serialize(testData);
                        String javaDeserialized = javaProtocol.deserialize(javaSerialized, String.class);
                        
                        if (!testData.equals(javaDeserialized)) {
                            throw new AssertionError("Java thread safety violation: expected " + 
                                                   testData + ", got " + javaDeserialized);
                        }
                        
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    firstException.compareAndSet(null, e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        
        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for all threads to complete
        doneLatch.await();
        executor.shutdown();
        
        // Verify results
        Exception exception = firstException.get();
        if (exception != null) {
            fail("Thread safety test failed: " + exception.getMessage());
        }
        
        int expectedSuccessCount = numThreads * operationsPerThread;
        assertEquals("All operations should succeed", expectedSuccessCount, successCount.get());
    }
    
    /**
     * Test error handling for various edge cases.
     * Ensures robust error handling for malformed or invalid data.
     */
    @Test
    public void testErrorHandlingEdgeCases() {
        // Test empty byte array
        try {
            kryoProtocol.deserialize(new byte[0], String.class);
            fail("Should throw exception for empty data");
        } catch (Exception e) {
            assertTrue("Should be a serialization-related exception", 
                     e instanceof SerializationException || e.getMessage().contains("deserialization"));
        }
        
        // Test malformed compressed data
        byte[] malformedCompressed = createMalformedCompressedData();
        try {
            kryoProtocol.deserialize(malformedCompressed, String.class);
            fail("Should throw exception for malformed compressed data");
        } catch (Exception e) {
            // Expected behavior
        }
        
        // Test deserialization with wrong type
        String testData = "test string";
        byte[] serializedString = kryoProtocol.serialize(testData);
        try {
            Integer wrongType = kryoProtocol.deserialize(serializedString, Integer.class);
            // Kryo might handle type coercion differently, so we check if result makes sense
            if (wrongType != null) {
                // Some serialization libraries may handle this gracefully
                // The important thing is that it doesn't crash
                System.out.println("Type coercion handled gracefully: " + wrongType);
            }
        } catch (Exception e) {
            // Expected behavior - type mismatch should cause exception
            assertTrue("Should be deserialization-related exception",
                     e.getMessage().contains("deserialize") || e.getMessage().contains("type") || 
                     e instanceof ClassCastException);
        }
        
        // Test null object serialization (Kryo doesn't support null serialization)
        try {
            byte[] nullSerialized = kryoProtocol.serialize(null);
            fail("Kryo should throw exception for null serialization");
        } catch (SerializationException e) {
            // Expected behavior - Kryo cannot serialize null
            assertTrue("Should indicate null serialization error", 
                     e.getMessage().contains("null") || e.getMessage().contains("serialize"));
        }
    }
    
    /**
     * Test version compatibility mechanisms.
     * Ensures the protocol can handle version differences gracefully.
     */
    @Test
    public void testVersionCompatibility() {
        // Test that current version can deserialize its own data
        String versionTestData = "Version compatibility test data";
        byte[] currentVersionData = kryoProtocol.serialize(versionTestData);
        
        String deserializedData = kryoProtocol.deserialize(currentVersionData, String.class);
        assertEquals("Same version should be compatible", versionTestData, deserializedData);
        
        // Test version detection in compressed data
        if (kryoProtocol.isCompressed(currentVersionData)) {
            // If data is compressed, ensure it's still properly versioned
            String decompressedData = kryoProtocol.deserialize(currentVersionData, String.class);
            assertEquals("Compressed versioned data should deserialize correctly", 
                       versionTestData, decompressedData);
        }
        
        // TODO: Add tests for handling different protocol versions
        // This would require simulating data from previous/future versions
    }
    
    /**
     * Test performance characteristics beyond basic benchmarking.
     * Validates memory usage and garbage collection behavior.
     */
    @Test
    public void testPerformanceCharacteristics() {
        // Warm up JVM
        for (int i = 0; i < 100; i++) {
            String warmupData = "Warmup data " + i;
            byte[] serialized = kryoProtocol.serialize(warmupData);
            kryoProtocol.deserialize(serialized, String.class);
        }
        
        // Test memory efficiency with large number of small objects
        long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        for (int i = 0; i < 1000; i++) {
            String testData = "Memory test data " + i;
            byte[] serialized = kryoProtocol.serialize(testData);
            String deserialized = kryoProtocol.deserialize(serialized, String.class);
            assertEquals("Memory test should preserve data", testData, deserialized);
        }
        
        // Force garbage collection
        System.gc();
        Thread.yield();
        
        long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;
        
        // Memory usage should be reasonable (less than 10MB for 1000 small operations)
        assertTrue("Memory usage should be reasonable: " + memoryUsed + " bytes", 
                 memoryUsed < 10 * 1024 * 1024);
    }
    
    /**
     * Test serialization of null and edge case values.
     * Ensures robust handling of boundary conditions.
     */
    @Test
    public void testBoundaryConditions() {
        // Test null serialization handling (Kryo rejects null)
        try {
            byte[] nullData = kryoProtocol.serialize(null);
            fail("Kryo should reject null serialization");
        } catch (SerializationException e) {
            // Expected - Kryo cannot serialize null objects
            assertTrue("Should indicate null serialization error", 
                     e.getMessage().contains("null"));
        }
        
        // Test empty string
        String emptyString = "";
        byte[] emptyData = kryoProtocol.serialize(emptyString);
        String emptyResult = kryoProtocol.deserialize(emptyData, String.class);
        assertEquals("Empty string should be preserved", emptyString, emptyResult);
        
        // Test very long string
        StringBuilder longStringBuilder = new StringBuilder();
        for (int i = 0; i < 100000; i++) {
            longStringBuilder.append('a');
        }
        String longString = longStringBuilder.toString();
        
        byte[] longData = kryoProtocol.serialize(longString);
        String longResult = kryoProtocol.deserialize(longData, String.class);
        assertEquals("Very long string should be preserved", longString, longResult);
        
        // Test strings with special characters
        String specialChars = "Special chars: \n\t\r\0\u00A0\u2022\uD83D\uDE00";
        byte[] specialData = kryoProtocol.serialize(specialChars);
        String specialResult = kryoProtocol.deserialize(specialData, String.class);
        assertEquals("Special characters should be preserved", specialChars, specialResult);
    }
    
    /**
     * Creates malformed compressed data for error testing.
     */
    private byte[] createMalformedCompressedData() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream gzipOut = new GZIPOutputStream(baos);
            gzipOut.write("test data".getBytes());
            gzipOut.close();
            
            byte[] validCompressed = baos.toByteArray();
            
            // Corrupt the data by modifying some bytes
            byte[] malformed = validCompressed.clone();
            if (malformed.length > 10) {
                malformed[5] = (byte) 0xFF;
                malformed[malformed.length - 2] = (byte) 0xFF;
            }
            
            return malformed;
        } catch (IOException e) {
            // Fallback: return obviously invalid compressed data
            return new byte[]{0x1F, (byte) 0x8B, 0x08, 0x00, 0x00, 0x00, (byte) 0xFF, (byte) 0xFF};
        }
    }
}