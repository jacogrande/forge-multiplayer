package forge.gui.error;

import forge.error.*;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterMethod;
import static org.testng.Assert.*;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test suite for NetworkErrorNotificationManager.
 * Validates user notification system for network errors with comprehensive
 * testing of notification priorities, user interaction, and platform integration.
 */
public class NetworkErrorNotificationManagerTest {
    
    private NetworkErrorNotificationManager notificationManager;
    private TestNotificationRenderer renderer;
    private TestUserInteractionHandler interactionHandler;
    
    @BeforeMethod
    public void setUp() {
        renderer = new TestNotificationRenderer();
        interactionHandler = new TestUserInteractionHandler();
        notificationManager = new NetworkErrorNotificationManager(renderer, interactionHandler);
    }
    
    @AfterMethod
    public void tearDown() {
        if (notificationManager != null) {
            notificationManager.shutdown();
        }
    }
    
    @Test
    public void testBasicNotificationDisplay() {
        // Acceptance Criteria: User receives clear notifications for network errors
        String message = "Connection to server lost";
        
        notificationManager.notifyUser(message);
        
        // Verify notification was displayed
        assertEquals(1, renderer.getDisplayedNotifications().size());
        assertTrue(renderer.getDisplayedNotifications().get(0).getMessage().contains(message));
        assertEquals(NetworkErrorNotificationManager.NotificationPriority.NORMAL, 
                    renderer.getDisplayedNotifications().get(0).getPriority());
    }
    
    @Test
    public void testNotificationPrioritySystem() {
        // Acceptance Criteria: Critical errors get priority display
        notificationManager.notifyUser("Low priority message", 
                                     NetworkErrorNotificationManager.NotificationPriority.LOW);
        notificationManager.notifyUser("Critical security error", 
                                     NetworkErrorNotificationManager.NotificationPriority.CRITICAL);
        notificationManager.notifyUser("Normal message", 
                                     NetworkErrorNotificationManager.NotificationPriority.NORMAL);
        
        List<TestNotificationRenderer.TestNotification> notifications = renderer.getDisplayedNotifications();
        assertEquals(3, notifications.size());
        
        // Should be displayed in priority order: CRITICAL, NORMAL, LOW
        assertEquals(NetworkErrorNotificationManager.NotificationPriority.CRITICAL, 
                    notifications.get(0).getPriority());
        assertEquals(NetworkErrorNotificationManager.NotificationPriority.NORMAL, 
                    notifications.get(1).getPriority());
        assertEquals(NetworkErrorNotificationManager.NotificationPriority.LOW, 
                    notifications.get(2).getPriority());
    }
    
    @Test
    public void testProgressTrackingNotifications() {
        // Acceptance Criteria: Users see progress during long recovery operations
        String operationId = notificationManager.startProgressTracking("Reconnecting to server...");
        
        // Update progress
        notificationManager.updateProgress(operationId, 25, "Establishing connection...");
        notificationManager.updateProgress(operationId, 50, "Authenticating...");
        notificationManager.updateProgress(operationId, 75, "Synchronizing game state...");
        notificationManager.completeProgress(operationId, "Reconnection successful!");
        
        // Verify progress updates were shown
        List<TestNotificationRenderer.ProgressUpdate> progressUpdates = renderer.getProgressUpdates();
        assertEquals(4, progressUpdates.size()); // 3 updates + 1 completion
        
        assertEquals(25, progressUpdates.get(0).getProgress());
        assertEquals("Establishing connection...", progressUpdates.get(0).getMessage());
        assertEquals(100, progressUpdates.get(3).getProgress());
        assertEquals("Reconnection successful!", progressUpdates.get(3).getMessage());
    }
    
    @Test
    public void testUserChoiceDialog() {
        // Acceptance Criteria: Users can make recovery decisions
        List<String> options = Arrays.asList("Retry", "Skip", "Abort");
        String prompt = "Connection failed. How would you like to proceed?";
        
        // Set expected user choice
        interactionHandler.setNextChoice("Retry");
        
        String result = notificationManager.requestUserChoice(prompt, options);
        
        assertEquals("Retry", result);
        assertTrue(interactionHandler.wasChoiceRequested());
        assertEquals(prompt, interactionHandler.getLastPrompt());
        assertEquals(options, interactionHandler.getLastOptions());
    }
    
    @Test
    public void testModalNotificationBlocking() throws InterruptedException {
        // Acceptance Criteria: Critical notifications get user attention
        AtomicBoolean modalShown = new AtomicBoolean(false);
        CountDownLatch modalLatch = new CountDownLatch(1);
        
        renderer.setModalCallback(() -> {
            modalShown.set(true);
            modalLatch.countDown();
        });
        
        notificationManager.showModalNotification("Critical system error", 
                                                "The game connection has been permanently lost.");
        
        assertTrue(modalLatch.await(2, TimeUnit.SECONDS));
        assertTrue(modalShown.get());
    }
    
    @Test
    public void testNotificationHistory() {
        // Acceptance Criteria: Users can review notification history
        notificationManager.notifyUser("First notification");
        notificationManager.notifyUser("Second notification");
        notificationManager.notifyUser("Third notification");
        
        List<NetworkErrorNotificationManager.NotificationHistoryEntry> history = 
            notificationManager.getNotificationHistory();
        
        assertEquals(3, history.size());
        assertEquals("First notification", history.get(0).getMessage());
        assertEquals("Second notification", history.get(1).getMessage());
        assertEquals("Third notification", history.get(2).getMessage());
        
        // Verify timestamps are in chronological order
        assertTrue(history.get(0).getTimestamp().isBefore(history.get(1).getTimestamp()));
        assertTrue(history.get(1).getTimestamp().isBefore(history.get(2).getTimestamp()));
    }
    
    @Test
    public void testNotificationDismissal() {
        // Acceptance Criteria: Users can dismiss non-critical notifications
        notificationManager.notifyUser("Dismissible message", 
                                     NetworkErrorNotificationManager.NotificationPriority.NORMAL);
        
        String notificationId = renderer.getDisplayedNotifications().get(0).getId();
        notificationManager.dismissNotification(notificationId);
        
        assertTrue(renderer.wasDismissed(notificationId));
    }
    
    @Test
    public void testConcurrentNotificationHandling() throws InterruptedException {
        // Acceptance Criteria: System handles multiple concurrent notifications
        int notificationCount = 10;
        CountDownLatch latch = new CountDownLatch(notificationCount);
        
        // Send notifications from multiple threads
        for (int i = 0; i < notificationCount; i++) {
            final int index = i;
            new Thread(() -> {
                notificationManager.notifyUser("Concurrent notification " + index);
                latch.countDown();
            }).start();
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        // All notifications should be processed
        assertEquals(notificationCount, renderer.getDisplayedNotifications().size());
        
        // Verify no notifications were lost
        Set<String> messageNumbers = new HashSet<>();
        for (TestNotificationRenderer.TestNotification notification : renderer.getDisplayedNotifications()) {
            String message = notification.getMessage();
            String number = message.substring(message.lastIndexOf(' ') + 1);
            messageNumbers.add(number);
        }
        assertEquals(notificationCount, messageNumbers.size());
    }
    
    @Test
    public void testNotificationQueueManagement() {
        // Acceptance Criteria: System manages notification queue to prevent overflow
        notificationManager.setMaxQueueSize(3);
        
        // Add more notifications than queue size
        for (int i = 0; i < 5; i++) {
            notificationManager.notifyUser("Notification " + i);
        }
        
        // Should only keep the most recent notifications
        List<TestNotificationRenderer.TestNotification> notifications = renderer.getDisplayedNotifications();
        assertEquals(3, notifications.size());
        
        // Should have kept the last 3 notifications
        assertTrue(notifications.get(0).getMessage().contains("Notification 2"));
        assertTrue(notifications.get(1).getMessage().contains("Notification 3"));
        assertTrue(notifications.get(2).getMessage().contains("Notification 4"));
    }
    
    @Test
    public void testUserAttentionManagement() {
        // Acceptance Criteria: System manages user attention appropriately
        notificationManager.notifyUser("Background notification", 
                                     NetworkErrorNotificationManager.NotificationPriority.LOW);
        assertFalse(renderer.wasAttentionRequested());
        
        notificationManager.notifyUser("Important notification", 
                                     NetworkErrorNotificationManager.NotificationPriority.HIGH);
        assertTrue(renderer.wasAttentionRequested());
        
        notificationManager.notifyUser("Critical notification", 
                                     NetworkErrorNotificationManager.NotificationPriority.CRITICAL);
        assertTrue(renderer.wasUrgentAttentionRequested());
    }
    
    @Test
    public void testNotificationTemplates() {
        // Acceptance Criteria: System provides templates for common error scenarios
        ConnectionTimeoutError timeoutError = new ConnectionTimeoutError("localhost", 7777, 5000);
        AuthenticationError authError = new AuthenticationError(AuthenticationError.Reason.INVALID_CREDENTIALS, "testuser");
        
        notificationManager.notifyForError(timeoutError);
        notificationManager.notifyForError(authError);
        
        List<TestNotificationRenderer.TestNotification> notifications = renderer.getDisplayedNotifications();
        assertEquals(2, notifications.size());
        
        // Should use appropriate templates
        assertTrue(notifications.get(0).getMessage().contains("connection"));
        assertTrue(notifications.get(0).getMessage().contains("timed out"));
        assertTrue(notifications.get(1).getMessage().contains("authentication"));
        assertTrue(notifications.get(1).getMessage().contains("credentials"));
    }
    
    /**
     * Test implementation of notification renderer.
     */
    private static class TestNotificationRenderer implements NetworkErrorNotificationManager.NotificationRenderer {
        private final List<TestNotification> displayedNotifications = new ArrayList<>();
        private final List<ProgressUpdate> progressUpdates = new ArrayList<>();
        private final Set<String> dismissedNotifications = new HashSet<>();
        private boolean attentionRequested = false;
        private boolean urgentAttentionRequested = false;
        private Runnable modalCallback;
        
        @Override
        public void displayNotification(String id, String message, 
                                      NetworkErrorNotificationManager.NotificationPriority priority,
                                      long durationMs) {
            displayedNotifications.add(new TestNotification(id, message, priority, durationMs));
            
            // Simulate attention requests
            if (priority == NetworkErrorNotificationManager.NotificationPriority.HIGH) {
                attentionRequested = true;
            } else if (priority == NetworkErrorNotificationManager.NotificationPriority.CRITICAL) {
                urgentAttentionRequested = true;
            }
        }
        
        @Override
        public void showProgressNotification(String id, String message, int progress, String details) {
            progressUpdates.add(new ProgressUpdate(id, message, progress, details));
        }
        
        @Override
        public void showModalNotification(String title, String message) {
            if (modalCallback != null) {
                modalCallback.run();
            }
        }
        
        @Override
        public void dismissNotification(String id) {
            dismissedNotifications.add(id);
        }
        
        @Override
        public void clearAllNotifications() {
            displayedNotifications.clear();
            progressUpdates.clear();
            dismissedNotifications.clear();
        }
        
        public List<TestNotification> getDisplayedNotifications() { return new ArrayList<>(displayedNotifications); }
        public List<ProgressUpdate> getProgressUpdates() { return new ArrayList<>(progressUpdates); }
        public boolean wasDismissed(String id) { return dismissedNotifications.contains(id); }
        public boolean wasAttentionRequested() { return attentionRequested; }
        public boolean wasUrgentAttentionRequested() { return urgentAttentionRequested; }
        public void setModalCallback(Runnable callback) { this.modalCallback = callback; }
        
        static class TestNotification {
            private final String id;
            private final String message;
            private final NetworkErrorNotificationManager.NotificationPriority priority;
            private final long durationMs;
            
            TestNotification(String id, String message, 
                           NetworkErrorNotificationManager.NotificationPriority priority, long durationMs) {
                this.id = id;
                this.message = message;
                this.priority = priority;
                this.durationMs = durationMs;
            }
            
            public String getId() { return id; }
            public String getMessage() { return message; }
            public NetworkErrorNotificationManager.NotificationPriority getPriority() { return priority; }
            public long getDurationMs() { return durationMs; }
        }
        
        static class ProgressUpdate {
            private final String id;
            private final String message;
            private final int progress;
            private final String details;
            
            ProgressUpdate(String id, String message, int progress, String details) {
                this.id = id;
                this.message = message;
                this.progress = progress;
                this.details = details;
            }
            
            public String getId() { return id; }
            public String getMessage() { return message; }
            public int getProgress() { return progress; }
            public String getDetails() { return details; }
        }
    }
    
    /**
     * Test implementation of user interaction handler.
     */
    private static class TestUserInteractionHandler implements NetworkErrorNotificationManager.UserInteractionHandler {
        private final AtomicBoolean choiceRequested = new AtomicBoolean(false);
        private final AtomicReference<String> lastPrompt = new AtomicReference<>();
        private final AtomicReference<List<String>> lastOptions = new AtomicReference<>();
        private String nextChoice = "DEFAULT";
        
        @Override
        public String requestUserChoice(String prompt, List<String> options, long timeoutMs) {
            choiceRequested.set(true);
            lastPrompt.set(prompt);
            lastOptions.set(new ArrayList<>(options));
            return nextChoice;
        }
        
        @Override
        public boolean confirmAction(String message, long timeoutMs) {
            return true; // Default to confirm
        }
        
        @Override
        public String requestInput(String prompt, String defaultValue, long timeoutMs) {
            return defaultValue;
        }
        
        public boolean wasChoiceRequested() { return choiceRequested.get(); }
        public String getLastPrompt() { return lastPrompt.get(); }
        public List<String> getLastOptions() { return lastOptions.get(); }
        public void setNextChoice(String choice) { this.nextChoice = choice; }
    }
}