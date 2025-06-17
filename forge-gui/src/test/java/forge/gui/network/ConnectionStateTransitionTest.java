package forge.gui.network;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive test suite for connection state transitions and the event system.
 * Tests all valid transitions, invalid transition prevention, and event ordering.
 */
public class ConnectionStateTransitionTest {
    
    private ConnectionStateMachine stateMachine;
    private TestStateTransitionObserver observer;
    
    @BeforeMethod
    public void setUp() {
        stateMachine = new ConnectionStateMachine();
        observer = new TestStateTransitionObserver();
        stateMachine.addObserver(observer);
    }
    
    @AfterMethod
    public void tearDown() {
        stateMachine = null;
        observer = null;
    }
    
    @Test
    public void testConnectingToConnected() {
        String clientId = "test-client";
        
        // Initialize in CONNECTING state
        stateMachine.initializeClient(clientId, ConnectionState.CONNECTING);
        observer.reset();
        
        // Transition to CONNECTED
        boolean success = stateMachine.transitionState(clientId, ConnectionState.CONNECTED);
        
        Assert.assertTrue(success, "Transition from CONNECTING to CONNECTED should succeed");
        Assert.assertEquals(stateMachine.getCurrentState(clientId), ConnectionState.CONNECTED);
        
        // Verify event was fired
        Assert.assertEquals(observer.getTransitionCount(), 1);
        Assert.assertEquals(observer.getLastTransition().getClientId(), clientId);
        Assert.assertEquals(observer.getLastTransition().getFromState(), ConnectionState.CONNECTING);
        Assert.assertEquals(observer.getLastTransition().getToState(), ConnectionState.CONNECTED);
    }
    
    @Test
    public void testConnectedToDisconnected() {
        String clientId = "disconnect-test";
        
        stateMachine.initializeClient(clientId, ConnectionState.CONNECTED);
        observer.reset();
        
        boolean success = stateMachine.transitionState(clientId, ConnectionState.DISCONNECTED);
        
        Assert.assertTrue(success, "Transition from CONNECTED to DISCONNECTED should succeed");
        Assert.assertEquals(stateMachine.getCurrentState(clientId), ConnectionState.DISCONNECTED);
    }
    
    @Test
    public void testDisconnectedToReconnecting() {
        String clientId = "reconnect-test";
        
        stateMachine.initializeClient(clientId, ConnectionState.DISCONNECTED);
        observer.reset();
        
        boolean success = stateMachine.transitionState(clientId, ConnectionState.RECONNECTING);
        
        Assert.assertTrue(success, "Transition from DISCONNECTED to RECONNECTING should succeed");
        Assert.assertEquals(stateMachine.getCurrentState(clientId), ConnectionState.RECONNECTING);
    }
    
    @Test
    public void testReconnectingToConnected() {
        String clientId = "reconnect-success-test";
        
        stateMachine.initializeClient(clientId, ConnectionState.RECONNECTING);
        observer.reset();
        
        boolean success = stateMachine.transitionState(clientId, ConnectionState.CONNECTED);
        
        Assert.assertTrue(success, "Transition from RECONNECTING to CONNECTED should succeed");
        Assert.assertEquals(stateMachine.getCurrentState(clientId), ConnectionState.CONNECTED);
    }
    
    @Test
    public void testInvalidTransitionPrevention() {
        String clientId = "invalid-test";
        
        // Start in CONNECTING state
        stateMachine.initializeClient(clientId, ConnectionState.CONNECTING);
        observer.reset();
        
        // Try invalid transition CONNECTING -> RECONNECTING
        boolean success = stateMachine.transitionState(clientId, ConnectionState.RECONNECTING);
        
        Assert.assertFalse(success, "Invalid transition should be rejected");
        Assert.assertEquals(stateMachine.getCurrentState(clientId), ConnectionState.CONNECTING,
            "State should remain unchanged after invalid transition");
        
        // No event should be fired for invalid transition
        Assert.assertEquals(observer.getTransitionCount(), 0,
            "No event should be fired for invalid transition");
    }
    
    @Test
    public void testCompleteConnectionLifecycle() {
        String clientId = "lifecycle-test";
        
        // Complete connection lifecycle
        stateMachine.initializeClient(clientId, ConnectionState.DISCONNECTED);
        observer.reset();
        
        // DISCONNECTED -> CONNECTING
        stateMachine.transitionState(clientId, ConnectionState.CONNECTING);
        Assert.assertEquals(observer.getTransitionCount(), 1);
        
        // CONNECTING -> CONNECTED
        stateMachine.transitionState(clientId, ConnectionState.CONNECTED);
        Assert.assertEquals(observer.getTransitionCount(), 2);
        
        // CONNECTED -> RECONNECTING (simulating temporary disconnect)
        stateMachine.transitionState(clientId, ConnectionState.RECONNECTING);
        Assert.assertEquals(observer.getTransitionCount(), 3);
        
        // RECONNECTING -> CONNECTED (successful reconnect)
        stateMachine.transitionState(clientId, ConnectionState.CONNECTED);
        Assert.assertEquals(observer.getTransitionCount(), 4);
        
        // CONNECTED -> DISCONNECTED (final disconnect)
        stateMachine.transitionState(clientId, ConnectionState.DISCONNECTED);
        Assert.assertEquals(observer.getTransitionCount(), 5);
        
        // Verify final state
        Assert.assertEquals(stateMachine.getCurrentState(clientId), ConnectionState.DISCONNECTED);
    }
    
    @Test
    public void testEventOrderGuarantees() throws InterruptedException {
        String clientId = "event-order-test";
        int transitionCount = 20;
        
        stateMachine.initializeClient(clientId, ConnectionState.DISCONNECTED);
        observer.reset();
        
        // Perform rapid transitions
        for (int i = 0; i < transitionCount; i++) {
            if (i % 2 == 0) {
                stateMachine.transitionState(clientId, ConnectionState.CONNECTING);
            } else {
                stateMachine.transitionState(clientId, ConnectionState.DISCONNECTED);
            }
        }
        
        // Verify all events were received in order
        Assert.assertEquals(observer.getTransitionCount(), transitionCount);
        
        List<StateTransition> transitions = observer.getAllTransitions();
        for (int i = 0; i < transitions.size(); i++) {
            StateTransition transition = transitions.get(i);
            Assert.assertEquals(transition.getSequenceNumber(), i + 1,
                "Events should be received in sequence order");
        }
    }
    
    @Test
    public void testObserverRegistrationManagement() {
        String clientId = "observer-test";
        TestStateTransitionObserver secondObserver = new TestStateTransitionObserver();
        
        // Add second observer
        stateMachine.addObserver(secondObserver);
        
        stateMachine.initializeClient(clientId, ConnectionState.DISCONNECTED);
        stateMachine.transitionState(clientId, ConnectionState.CONNECTING);
        
        // Both observers should receive events
        Assert.assertEquals(observer.getTransitionCount(), 1);
        Assert.assertEquals(secondObserver.getTransitionCount(), 1);
        
        // Remove first observer
        stateMachine.removeObserver(observer);
        observer.reset();
        secondObserver.reset();
        
        stateMachine.transitionState(clientId, ConnectionState.CONNECTED);
        
        // Only second observer should receive event
        Assert.assertEquals(observer.getTransitionCount(), 0);
        Assert.assertEquals(secondObserver.getTransitionCount(), 1);
    }
    
    @Test
    public void testEventBatchingForRapidChanges() throws InterruptedException {
        String clientId = "batching-test";
        
        stateMachine.initializeClient(clientId, ConnectionState.DISCONNECTED);
        observer.reset();
        
        // Enable batching mode for rapid transitions
        stateMachine.setBatchingEnabled(true);
        
        // Perform rapid transitions within batching window
        stateMachine.transitionState(clientId, ConnectionState.CONNECTING);
        stateMachine.transitionState(clientId, ConnectionState.CONNECTED);
        stateMachine.transitionState(clientId, ConnectionState.DISCONNECTED);
        
        // Wait for batch processing
        Thread.sleep(50);
        
        // With batching, we might receive fewer individual events
        // but the final state should be correct
        Assert.assertEquals(stateMachine.getCurrentState(clientId), ConnectionState.DISCONNECTED);
        Assert.assertTrue(observer.getTransitionCount() >= 1,
            "Should receive at least one batched event");
    }
    
    @Test
    public void testConcurrentStateTransitions() throws InterruptedException {
        int clientCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completeLatch = new CountDownLatch(clientCount);
        
        // Initialize clients
        for (int i = 0; i < clientCount; i++) {
            stateMachine.initializeClient("client-" + i, ConnectionState.DISCONNECTED);
        }
        observer.reset();
        
        // Create threads for concurrent transitions
        for (int i = 0; i < clientCount; i++) {
            final String clientId = "client-" + i;
            Thread thread = new Thread(() -> {
                try {
                    startLatch.await();
                    
                    // Perform sequence of transitions
                    stateMachine.transitionState(clientId, ConnectionState.CONNECTING);
                    stateMachine.transitionState(clientId, ConnectionState.CONNECTED);
                    stateMachine.transitionState(clientId, ConnectionState.DISCONNECTED);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    completeLatch.countDown();
                }
            });
            thread.start();
        }
        
        // Start all transitions
        startLatch.countDown();
        
        // Wait for completion
        boolean completed = completeLatch.await(5, TimeUnit.SECONDS);
        Assert.assertTrue(completed, "All concurrent transitions should complete");
        
        // Verify all clients ended in correct state
        for (int i = 0; i < clientCount; i++) {
            Assert.assertEquals(stateMachine.getCurrentState("client-" + i), ConnectionState.DISCONNECTED);
        }
        
        // Verify total number of events
        Assert.assertEquals(observer.getTransitionCount(), clientCount * 3);
    }
    
    /**
     * Test observer implementation for capturing state transitions.
     */
    private static class TestStateTransitionObserver implements StateTransitionObserver {
        private final List<StateTransition> transitions = new ArrayList<>();
        private int transitionCount = 0;
        
        @Override
        public void onStateTransition(StateTransition transition) {
            transitions.add(transition);
            transitionCount++;
        }
        
        public int getTransitionCount() {
            return transitionCount;
        }
        
        public StateTransition getLastTransition() {
            return transitions.isEmpty() ? null : transitions.get(transitions.size() - 1);
        }
        
        public List<StateTransition> getAllTransitions() {
            return new ArrayList<>(transitions);
        }
        
        public void reset() {
            transitions.clear();
            transitionCount = 0;
        }
    }
}