package forge.gui.error;

import forge.error.NetworkError;
import forge.gui.network.NetworkEventLogger;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Advanced user notification system for network errors.
 * Provides priority-based notifications, progress tracking, user interaction,
 * and comprehensive notification management for network error scenarios.
 * 
 * Features:
 * - Priority-based notification queue management
 * - Progress tracking for long-running recovery operations
 * - User choice dialogs for recovery decisions
 * - Notification history and dismissal management
 * - Platform-agnostic notification rendering
 * - Concurrent notification handling with queue limits
 */
public class NetworkErrorNotificationManager implements ErrorRecoveryManager.UserNotificationManager {
    
    private static final NetworkEventLogger logger = NetworkEventLogger.forComponent("NotificationManager");
    
    private final NotificationRenderer renderer;
    private final UserInteractionHandler interactionHandler;
    private final ExecutorService notificationExecutor;
    private final ScheduledExecutorService cleanupExecutor;
    
    // Configuration
    private int maxQueueSize = 50;
    private long defaultNotificationDuration = 5000; // 5 seconds
    private long defaultUserChoiceTimeout = 30000; // 30 seconds
    
    // State management
    private final PriorityQueue<QueuedNotification> notificationQueue;
    private final Map<String, ProgressTracker> activeProgressTrackers = new ConcurrentHashMap<>();
    private final List<NotificationHistoryEntry> notificationHistory = new ArrayList<>();
    private final AtomicLong notificationIdCounter = new AtomicLong(0);
    
    // Metrics
    private final AtomicInteger totalNotifications = new AtomicInteger(0);
    private final AtomicInteger dismissedNotifications = new AtomicInteger(0);
    private final AtomicInteger userChoiceRequests = new AtomicInteger(0);
    
    /**
     * Notification priority levels.
     */
    public enum NotificationPriority {
        LOW(1),
        NORMAL(2),
        HIGH(3),
        CRITICAL(4);
        
        private final int level;
        
        NotificationPriority(int level) {
            this.level = level;
        }
        
        public int getLevel() {
            return level;
        }
    }
    
    /**
     * Creates a new NetworkErrorNotificationManager.
     * 
     * @param renderer The notification renderer implementation
     * @param interactionHandler The user interaction handler implementation
     */
    public NetworkErrorNotificationManager(NotificationRenderer renderer, 
                                           UserInteractionHandler interactionHandler) {
        this.renderer = renderer;
        this.interactionHandler = interactionHandler;
        
        // Priority queue orders by priority level (highest first)
        this.notificationQueue = new PriorityQueue<>((n1, n2) -> 
            Integer.compare(n2.getPriority().getLevel(), n1.getPriority().getLevel()));
        
        this.notificationExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "NotificationProcessor-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
        
        this.cleanupExecutor = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "NotificationCleanup-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
        
        // Start background processing
        startNotificationProcessor();
        scheduleCleanupTasks();
        
        logger.logEvent(NetworkEventLogger.EventType.PERFORMANCE,
                       NetworkEventLogger.Severity.INFO,
                       "NetworkErrorNotificationManager initialized")
               .withField("maxQueueSize", maxQueueSize)
               .withField("defaultDuration", defaultNotificationDuration)
               .log();
    }
    
    // ErrorRecoveryManager.UserNotificationManager implementation
    
    @Override
    public void notifyUser(String message) {
        notifyUser(message, NotificationPriority.NORMAL);
    }
    
    @Override
    public String requestUserChoice(String prompt, List<String> options) {
        return requestUserChoice(prompt, options, defaultUserChoiceTimeout);
    }
    
    // Extended notification methods
    
    /**
     * Notifies the user with a specific priority.
     * 
     * @param message The notification message
     * @param priority The notification priority
     */
    public void notifyUser(String message, NotificationPriority priority) {
        notifyUser(message, priority, defaultNotificationDuration);
    }
    
    /**
     * Notifies the user with priority and duration.
     * 
     * @param message The notification message
     * @param priority The notification priority
     * @param durationMs How long to display the notification
     */
    public void notifyUser(String message, NotificationPriority priority, long durationMs) {
        String id = generateNotificationId();
        QueuedNotification notification = new QueuedNotification(id, message, priority, durationMs);
        
        synchronized (notificationQueue) {
            // Enforce queue size limit
            if (notificationQueue.size() >= maxQueueSize) {
                // Remove lowest priority notification
                Iterator<QueuedNotification> iterator = notificationQueue.iterator();
                QueuedNotification lowest = null;
                while (iterator.hasNext()) {
                    QueuedNotification current = iterator.next();
                    if (lowest == null || current.getPriority().getLevel() < lowest.getPriority().getLevel()) {
                        lowest = current;
                    }
                }
                if (lowest != null) {
                    notificationQueue.remove(lowest);
                    logger.logEvent(NetworkEventLogger.EventType.PERFORMANCE,
                                   NetworkEventLogger.Severity.DEBUG,
                                   "Removed notification due to queue limit")
                           .withField("removedPriority", lowest.getPriority())
                           .log();
                }
            }
            
            notificationQueue.offer(notification);
            notificationQueue.notifyAll();
        }
        
        totalNotifications.incrementAndGet();
        addToHistory(message, priority);
        
        logger.logEvent(NetworkEventLogger.EventType.PERFORMANCE,
                       NetworkEventLogger.Severity.DEBUG,
                       "Queued notification")
               .withField("priority", priority)
               .withField("queueSize", notificationQueue.size())
               .log();
    }
    
    /**
     * Notifies the user for a specific error with appropriate template.
     * 
     * @param error The network error
     */
    public void notifyForError(NetworkError error) {
        NotificationPriority priority = mapSeverityToPriority(error.getSeverity());
        String message = createErrorMessage(error);
        notifyUser(message, priority);
    }
    
    /**
     * Shows a modal notification that requires user acknowledgment.
     * 
     * @param title The notification title
     * @param message The notification message
     */
    public void showModalNotification(String title, String message) {
        try {
            renderer.showModalNotification(title, message);
            addToHistory(title + ": " + message, NotificationPriority.CRITICAL);
            
            logger.logEvent(NetworkEventLogger.EventType.PERFORMANCE,
                           NetworkEventLogger.Severity.INFO,
                           "Showed modal notification")
                   .withField("title", title)
                   .log();
                   
        } catch (Exception e) {
            logger.logError("modal_notification_display", e);
        }
    }
    
    /**
     * Starts progress tracking for a long-running operation.
     * 
     * @param message Initial progress message
     * @return Progress tracker ID
     */
    public String startProgressTracking(String message) {
        String id = generateNotificationId();
        ProgressTracker tracker = new ProgressTracker(id, message);
        activeProgressTrackers.put(id, tracker);
        
        renderer.showProgressNotification(id, message, 0, "Starting...");
        
        logger.logEvent(NetworkEventLogger.EventType.PERFORMANCE,
                       NetworkEventLogger.Severity.DEBUG,
                       "Started progress tracking")
               .withField("progressId", id)
               .withField("message", message)
               .log();
        
        return id;
    }
    
    /**
     * Updates progress for an active operation.
     * 
     * @param progressId The progress tracker ID
     * @param progress Progress percentage (0-100)
     * @param message Progress message
     */
    public void updateProgress(String progressId, int progress, String message) {
        ProgressTracker tracker = activeProgressTrackers.get(progressId);
        if (tracker != null) {
            tracker.updateProgress(progress, message);
            renderer.showProgressNotification(progressId, message, progress, 
                "Progress: " + progress + "%");
        }
    }
    
    /**
     * Completes progress tracking.
     * 
     * @param progressId The progress tracker ID
     * @param completionMessage Final completion message
     */
    public void completeProgress(String progressId, String completionMessage) {
        ProgressTracker tracker = activeProgressTrackers.remove(progressId);
        if (tracker != null) {
            renderer.showProgressNotification(progressId, completionMessage, 100, "Completed");
            
            // Schedule removal of progress notification
            cleanupExecutor.schedule(() -> {
                try {
                    renderer.dismissNotification(progressId);
                } catch (Exception e) {
                    logger.logError("progress_cleanup", e);
                }
            }, 3, TimeUnit.SECONDS);
            
            logger.logEvent(NetworkEventLogger.EventType.PERFORMANCE,
                           NetworkEventLogger.Severity.DEBUG,
                           "Completed progress tracking")
                   .withField("progressId", progressId)
                   .withField("duration", tracker.getDurationMs())
                   .log();
        }
    }
    
    /**
     * Requests a choice from the user with timeout.
     * 
     * @param prompt The choice prompt
     * @param options Available options
     * @param timeoutMs Timeout in milliseconds
     * @return The user's choice
     */
    public String requestUserChoice(String prompt, List<String> options, long timeoutMs) {
        userChoiceRequests.incrementAndGet();
        
        try {
            String result = interactionHandler.requestUserChoice(prompt, options, timeoutMs);
            
            logger.logEvent(NetworkEventLogger.EventType.PERFORMANCE,
                           NetworkEventLogger.Severity.DEBUG,
                           "User choice completed")
                   .withField("choice", result)
                   .withField("options", options.size())
                   .log();
            
            return result;
            
        } catch (Exception e) {
            logger.logError("user_choice_request", e);
            return options.isEmpty() ? "DEFAULT" : options.get(0);
        }
    }
    
    /**
     * Dismisses a specific notification.
     * 
     * @param notificationId The notification ID to dismiss
     */
    public void dismissNotification(String notificationId) {
        try {
            renderer.dismissNotification(notificationId);
            dismissedNotifications.incrementAndGet();
            
            logger.logEvent(NetworkEventLogger.EventType.PERFORMANCE,
                           NetworkEventLogger.Severity.DEBUG,
                           "Dismissed notification")
                   .withField("notificationId", notificationId)
                   .log();
                   
        } catch (Exception e) {
            logger.logError("notification_dismissal", e);
        }
    }
    
    /**
     * Gets the notification history.
     * 
     * @return List of notification history entries
     */
    public List<NotificationHistoryEntry> getNotificationHistory() {
        synchronized (notificationHistory) {
            return new ArrayList<>(notificationHistory);
        }
    }
    
    /**
     * Sets the maximum queue size.
     * 
     * @param maxSize Maximum number of queued notifications
     */
    public void setMaxQueueSize(int maxSize) {
        this.maxQueueSize = Math.max(1, maxSize);
        logger.logEvent(NetworkEventLogger.EventType.PERFORMANCE,
                       NetworkEventLogger.Severity.INFO,
                       "Updated max queue size to {}", maxSize)
               .log();
    }
    
    /**
     * Shuts down the notification manager.
     */
    public void shutdown() {
        logger.logEvent(NetworkEventLogger.EventType.PERFORMANCE,
                       NetworkEventLogger.Severity.INFO,
                       "Shutting down notification manager")
               .withField("totalNotifications", totalNotifications.get())
               .withField("userChoiceRequests", userChoiceRequests.get())
               .log();
        
        // Cancel active progress trackers
        for (String progressId : activeProgressTrackers.keySet()) {
            completeProgress(progressId, "Operation cancelled");
        }
        
        // Clear all notifications
        try {
            renderer.clearAllNotifications();
        } catch (Exception e) {
            logger.logError("notification_cleanup", e);
        }
        
        // Shutdown executors
        notificationExecutor.shutdown();
        cleanupExecutor.shutdown();
        
        try {
            if (!notificationExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                notificationExecutor.shutdownNow();
            }
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            notificationExecutor.shutdownNow();
            cleanupExecutor.shutdownNow();
        }
    }
    
    private void startNotificationProcessor() {
        notificationExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    QueuedNotification notification;
                    synchronized (notificationQueue) {
                        while (notificationQueue.isEmpty()) {
                            notificationQueue.wait();
                        }
                        notification = notificationQueue.poll();
                    }
                    
                    if (notification != null) {
                        processNotification(notification);
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.logError("notification_processing", e);
                }
            }
        });
    }
    
    private void processNotification(QueuedNotification notification) {
        try {
            renderer.displayNotification(
                notification.getId(),
                notification.getMessage(),
                notification.getPriority(),
                notification.getDurationMs()
            );
            
            // Schedule automatic dismissal for non-critical notifications
            if (notification.getPriority() != NotificationPriority.CRITICAL && 
                notification.getDurationMs() > 0) {
                cleanupExecutor.schedule(() -> {
                    try {
                        renderer.dismissNotification(notification.getId());
                    } catch (Exception e) {
                        logger.logError("auto_dismiss", e);
                    }
                }, notification.getDurationMs(), TimeUnit.MILLISECONDS);
            }
            
        } catch (Exception e) {
            logger.logError("notification_display", e);
        }
    }
    
    private void scheduleCleanupTasks() {
        // Clean up old history entries every hour
        cleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                cleanupNotificationHistory();
            } catch (Exception e) {
                logger.logError("history_cleanup", e);
            }
        }, 1, 1, TimeUnit.HOURS);
    }
    
    private void cleanupNotificationHistory() {
        synchronized (notificationHistory) {
            // Keep only last 100 entries
            if (notificationHistory.size() > 100) {
                notificationHistory.subList(0, notificationHistory.size() - 100).clear();
            }
        }
    }
    
    private String generateNotificationId() {
        return "notification_" + notificationIdCounter.incrementAndGet();
    }
    
    private NotificationPriority mapSeverityToPriority(NetworkError.Severity severity) {
        switch (severity) {
            case INFO: return NotificationPriority.LOW;
            case WARN: return NotificationPriority.NORMAL;
            case ERROR: return NotificationPriority.HIGH;
            case CRITICAL: return NotificationPriority.CRITICAL;
            default: return NotificationPriority.NORMAL;
        }
    }
    
    private String createErrorMessage(NetworkError error) {
        // Use templates for common error types
        String errorType = error.getClass().getSimpleName().toLowerCase();
        
        if (errorType.contains("timeout")) {
            return "Connection timed out: " + error.getUserMessage();
        } else if (errorType.contains("authentication")) {
            return "Authentication failed: " + error.getUserMessage();
        } else if (errorType.contains("connection")) {
            return "Connection error: " + error.getUserMessage();
        } else {
            return error.getUserMessage();
        }
    }
    
    private void addToHistory(String message, NotificationPriority priority) {
        synchronized (notificationHistory) {
            notificationHistory.add(new NotificationHistoryEntry(message, priority, Instant.now()));
        }
    }
    
    /**
     * Queued notification representation.
     */
    private static class QueuedNotification {
        private final String id;
        private final String message;
        private final NotificationPriority priority;
        private final long durationMs;
        private final Instant timestamp;
        
        QueuedNotification(String id, String message, NotificationPriority priority, long durationMs) {
            this.id = id;
            this.message = message;
            this.priority = priority;
            this.durationMs = durationMs;
            this.timestamp = Instant.now();
        }
        
        public String getId() { return id; }
        public String getMessage() { return message; }
        public NotificationPriority getPriority() { return priority; }
        public long getDurationMs() { return durationMs; }
        public Instant getTimestamp() { return timestamp; }
    }
    
    /**
     * Progress tracking state.
     */
    private static class ProgressTracker {
        private final String id;
        private final String initialMessage;
        private final Instant startTime;
        private volatile int currentProgress = 0;
        private volatile String currentMessage;
        
        ProgressTracker(String id, String initialMessage) {
            this.id = id;
            this.initialMessage = initialMessage;
            this.currentMessage = initialMessage;
            this.startTime = Instant.now();
        }
        
        public void updateProgress(int progress, String message) {
            this.currentProgress = Math.max(0, Math.min(100, progress));
            this.currentMessage = message;
        }
        
        public String getId() { return id; }
        public String getInitialMessage() { return initialMessage; }
        public int getCurrentProgress() { return currentProgress; }
        public String getCurrentMessage() { return currentMessage; }
        public long getDurationMs() { return System.currentTimeMillis() - startTime.toEpochMilli(); }
    }
    
    /**
     * Notification history entry.
     */
    public static class NotificationHistoryEntry {
        private final String message;
        private final NotificationPriority priority;
        private final Instant timestamp;
        
        public NotificationHistoryEntry(String message, NotificationPriority priority, Instant timestamp) {
            this.message = message;
            this.priority = priority;
            this.timestamp = timestamp;
        }
        
        public String getMessage() { return message; }
        public NotificationPriority getPriority() { return priority; }
        public Instant getTimestamp() { return timestamp; }
    }
    
    /**
     * Interface for platform-specific notification rendering.
     */
    public interface NotificationRenderer {
        /**
         * Displays a notification to the user.
         * 
         * @param id Unique notification identifier
         * @param message The notification message
         * @param priority The notification priority
         * @param durationMs How long to display the notification (0 = indefinite)
         */
        void displayNotification(String id, String message, NotificationPriority priority, long durationMs);
        
        /**
         * Shows a progress notification.
         * 
         * @param id Progress tracker identifier
         * @param message Progress message
         * @param progress Progress percentage (0-100)
         * @param details Additional progress details
         */
        void showProgressNotification(String id, String message, int progress, String details);
        
        /**
         * Shows a modal notification that blocks user interaction.
         * 
         * @param title The notification title
         * @param message The notification message
         */
        void showModalNotification(String title, String message);
        
        /**
         * Dismisses a specific notification.
         * 
         * @param id The notification identifier
         */
        void dismissNotification(String id);
        
        /**
         * Clears all notifications.
         */
        void clearAllNotifications();
    }
    
    /**
     * Interface for user interaction handling.
     */
    public interface UserInteractionHandler {
        /**
         * Requests a choice from the user.
         * 
         * @param prompt The choice prompt
         * @param options Available options
         * @param timeoutMs Timeout in milliseconds
         * @return The user's choice
         */
        String requestUserChoice(String prompt, List<String> options, long timeoutMs);
        
        /**
         * Requests user confirmation.
         * 
         * @param message The confirmation message
         * @param timeoutMs Timeout in milliseconds
         * @return true if confirmed
         */
        boolean confirmAction(String message, long timeoutMs);
        
        /**
         * Requests text input from the user.
         * 
         * @param prompt The input prompt
         * @param defaultValue Default value
         * @param timeoutMs Timeout in milliseconds
         * @return The user's input
         */
        String requestInput(String prompt, String defaultValue, long timeoutMs);
    }
}