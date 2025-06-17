package forge.gui.network;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Comprehensive test suite for ConnectionState enum and its state transition validation.
 * Tests valid transitions, invalid transition prevention, and concurrent state handling.
 */
public class ConnectionStateTest {
    
    @Test
    public void testValidStateTransitions() {
        // CONNECTING -> CONNECTED
        Assert.assertTrue(ConnectionState.CONNECTING.canTransitionTo(ConnectionState.CONNECTED),
            "Should allow transition from CONNECTING to CONNECTED");
        
        // CONNECTING -> DISCONNECTED
        Assert.assertTrue(ConnectionState.CONNECTING.canTransitionTo(ConnectionState.DISCONNECTED),
            "Should allow transition from CONNECTING to DISCONNECTED");
        
        // CONNECTED -> DISCONNECTED
        Assert.assertTrue(ConnectionState.CONNECTED.canTransitionTo(ConnectionState.DISCONNECTED),
            "Should allow transition from CONNECTED to DISCONNECTED");
        
        // CONNECTED -> RECONNECTING
        Assert.assertTrue(ConnectionState.CONNECTED.canTransitionTo(ConnectionState.RECONNECTING),
            "Should allow transition from CONNECTED to RECONNECTING");
        
        // DISCONNECTED -> CONNECTING
        Assert.assertTrue(ConnectionState.DISCONNECTED.canTransitionTo(ConnectionState.CONNECTING),
            "Should allow transition from DISCONNECTED to CONNECTING");
        
        // DISCONNECTED -> RECONNECTING
        Assert.assertTrue(ConnectionState.DISCONNECTED.canTransitionTo(ConnectionState.RECONNECTING),
            "Should allow transition from DISCONNECTED to RECONNECTING");
        
        // RECONNECTING -> CONNECTED
        Assert.assertTrue(ConnectionState.RECONNECTING.canTransitionTo(ConnectionState.CONNECTED),
            "Should allow transition from RECONNECTING to CONNECTED");
        
        // RECONNECTING -> DISCONNECTED
        Assert.assertTrue(ConnectionState.RECONNECTING.canTransitionTo(ConnectionState.DISCONNECTED),
            "Should allow transition from RECONNECTING to DISCONNECTED");
    }
    
    @Test
    public void testInvalidStateTransitions() {
        // CONNECTING -> RECONNECTING (invalid)
        Assert.assertFalse(ConnectionState.CONNECTING.canTransitionTo(ConnectionState.RECONNECTING),
            "Should not allow transition from CONNECTING to RECONNECTING");
        
        // CONNECTED -> CONNECTING (invalid)
        Assert.assertFalse(ConnectionState.CONNECTED.canTransitionTo(ConnectionState.CONNECTING),
            "Should not allow transition from CONNECTED to CONNECTING");
        
        // Self-transitions should be invalid
        Assert.assertFalse(ConnectionState.CONNECTING.canTransitionTo(ConnectionState.CONNECTING),
            "Should not allow self-transition for CONNECTING");
        Assert.assertFalse(ConnectionState.CONNECTED.canTransitionTo(ConnectionState.CONNECTED),
            "Should not allow self-transition for CONNECTED");
        Assert.assertFalse(ConnectionState.DISCONNECTED.canTransitionTo(ConnectionState.DISCONNECTED),
            "Should not allow self-transition for DISCONNECTED");
        Assert.assertFalse(ConnectionState.RECONNECTING.canTransitionTo(ConnectionState.RECONNECTING),
            "Should not allow self-transition for RECONNECTING");
    }
    
    @Test
    public void testStateEventGeneration() {
        // Test that all states have proper descriptions
        for (ConnectionState state : ConnectionState.values()) {
            Assert.assertNotNull(state.getDescription(),
                "State " + state + " should have a description");
            Assert.assertFalse(state.getDescription().isEmpty(),
                "State " + state + " description should not be empty");
        }
    }
    
    @Test
    public void testConcurrentStateChanges() {
        // Test that state transition validation is thread-safe
        // This is a basic test to ensure the enum methods don't have race conditions
        ConnectionState state = ConnectionState.CONNECTING;
        
        // Simulate concurrent access to transition validation
        boolean[] results = new boolean[2];
        Thread thread1 = new Thread(() -> {
            results[0] = state.canTransitionTo(ConnectionState.CONNECTED);
        });
        Thread thread2 = new Thread(() -> {
            results[1] = state.canTransitionTo(ConnectionState.CONNECTED);
        });
        
        thread1.start();
        thread2.start();
        
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            Assert.fail("Thread interrupted during concurrent state test");
        }
        
        // Both threads should get the same result
        Assert.assertEquals(results[0], results[1],
            "Concurrent state transition checks should be consistent");
        Assert.assertTrue(results[0],
            "CONNECTING should allow transition to CONNECTED");
    }
    
    @Test
    public void testStateDescriptions() {
        // Verify that state descriptions match expected values
        Assert.assertEquals(ConnectionState.CONNECTING.getDescription(), "Establishing connection");
        Assert.assertEquals(ConnectionState.CONNECTED.getDescription(), "Connection established");
        Assert.assertEquals(ConnectionState.DISCONNECTED.getDescription(), "Connection closed");
        Assert.assertEquals(ConnectionState.RECONNECTING.getDescription(), "Attempting to reconnect");
    }
    
    @Test
    public void testNullStateTransition() {
        // Test handling of null state transitions
        Assert.assertFalse(ConnectionState.CONNECTING.canTransitionTo(null),
            "Should not allow transition to null state");
        Assert.assertFalse(ConnectionState.CONNECTED.canTransitionTo(null),
            "Should not allow transition to null state");
        Assert.assertFalse(ConnectionState.DISCONNECTED.canTransitionTo(null),
            "Should not allow transition to null state");
        Assert.assertFalse(ConnectionState.RECONNECTING.canTransitionTo(null),
            "Should not allow transition to null state");
    }
}