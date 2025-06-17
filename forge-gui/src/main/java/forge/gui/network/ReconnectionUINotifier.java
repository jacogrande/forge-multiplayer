package forge.gui.network;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * UI notification manager for reconnection events.
 * Provides a bridge between the reconnection system and UI components
 * that need to display status to users.
 */
public class ReconnectionUINotifier extends ReconnectionNotificationAdapter {
    
    // UI notification types
    public enum NotificationType {
        INFO,
        WARNING,
        ERROR,
        SUCCESS
    }
    
    /**
     * Represents a notification to be displayed to the user.
     */
    public static class Notification {
        private final NotificationType type;
        private final String title;
        private final String message;
        private final long timestamp;
        private final boolean isPersistent;
        
        public Notification(NotificationType type, String title, String message, boolean isPersistent) {
            this.type = type;
            this.title = title;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
            this.isPersistent = isPersistent;
        }
        
        // Getters
        public NotificationType getType() { return type; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
        public boolean isPersistent() { return isPersistent; }
    }
    
    // Notification listeners (UI components)
    private final List<Consumer<Notification>> notificationListeners = new CopyOnWriteArrayList<>();
    
    // State tracking
    private volatile boolean isReconnecting = false;
    private volatile int currentAttempt = 0;
    private volatile int maxAttempts = 0;
    private volatile DisconnectReason disconnectReason;
    
    /**
     * Adds a notification listener.
     * 
     * @param listener The listener to receive notifications
     */
    public void addNotificationListener(Consumer<Notification> listener) {
        if (listener != null) {
            notificationListeners.add(listener);
        }
    }
    
    /**
     * Removes a notification listener.
     * 
     * @param listener The listener to remove
     */
    public void removeNotificationListener(Consumer<Notification> listener) {
        notificationListeners.remove(listener);
    }
    
    /**
     * Sends a notification to all registered listeners.
     */
    private void notifyListeners(Notification notification) {
        for (Consumer<Notification> listener : notificationListeners) {
            try {
                listener.accept(notification);
            } catch (Exception e) {
                // Don't let listener exceptions break notification
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public void onReconnectionStarted(DisconnectReason reason, int maxAttempts) {
        super.onReconnectionStarted(reason, maxAttempts);
        
        this.isReconnecting = true;
        this.disconnectReason = reason;
        this.maxAttempts = maxAttempts;
        this.currentAttempt = 0;
        
        String title = "Connection Lost";
        String message = reason.getDescription() + ". Attempting to reconnect...";
        
        notifyListeners(new Notification(NotificationType.WARNING, title, message, true));
    }
    
    @Override
    public void onReconnectionAttempt(int attemptNumber, int maxAttempts, long delayMs) {
        super.onReconnectionAttempt(attemptNumber, maxAttempts, delayMs);
        
        this.currentAttempt = attemptNumber;
        
        // Only show notification for attempts after the first
        if (attemptNumber > 1) {
            String title = "Reconnecting...";
            String message = String.format("Attempt %d of %d (next in %d seconds)", 
                                         attemptNumber, maxAttempts, delayMs / 1000);
            
            notifyListeners(new Notification(NotificationType.INFO, title, message, false));
        }
    }
    
    @Override
    public void onReconnectionProgress(int attemptNumber, double progress, String status) {
        super.onReconnectionProgress(attemptNumber, progress, status);
        
        // Only notify for significant progress milestones
        if (progress >= 0.5) {
            String title = "Reconnecting...";
            String message = status;
            
            notifyListeners(new Notification(NotificationType.INFO, title, message, false));
        }
    }
    
    @Override
    public void onReconnectionFailed(int attemptNumber, ReconnectionException exception, boolean willRetry) {
        super.onReconnectionFailed(attemptNumber, exception, willRetry);
        
        if (!willRetry) {
            // Final failure
            String title = "Reconnection Failed";
            String message = "Failed to reconnect after " + attemptNumber + " attempts. " + exception.getMessage();
            
            notifyListeners(new Notification(NotificationType.ERROR, title, message, true));
        }
    }
    
    @Override
    public void onReconnectionSucceeded(int attemptNumber, long totalDurationMs) {
        super.onReconnectionSucceeded(attemptNumber, totalDurationMs);
        
        this.isReconnecting = false;
        
        String title = "Connected";
        String message = String.format("Successfully reconnected after %d attempts", attemptNumber);
        
        notifyListeners(new Notification(NotificationType.SUCCESS, title, message, false));
    }
    
    @Override
    public void onReconnectionGivenUp(ReconnectionException finalException, long totalDurationMs) {
        super.onReconnectionGivenUp(finalException, totalDurationMs);
        
        this.isReconnecting = false;
        
        String title = "Connection Lost";
        String message = "Unable to reconnect to server. " + finalException.getMessage() + 
                        "\n\nPlease check your network connection and try again.";
        
        notifyListeners(new Notification(NotificationType.ERROR, title, message, true));
    }
    
    /**
     * Gets the current reconnection status.
     * 
     * @return true if currently reconnecting
     */
    public boolean isReconnecting() {
        return isReconnecting;
    }
    
    /**
     * Gets the current attempt number.
     * 
     * @return The current attempt number (0 if not reconnecting)
     */
    public int getCurrentAttempt() {
        return currentAttempt;
    }
    
    /**
     * Gets the maximum number of attempts.
     * 
     * @return The maximum attempts that will be made
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }
    
    /**
     * Gets the reason for the current reconnection.
     * 
     * @return The disconnect reason, or null if not reconnecting
     */
    public DisconnectReason getDisconnectReason() {
        return disconnectReason;
    }
    
    /**
     * Creates a formatted status string for UI display.
     * 
     * @return A human-readable status string
     */
    public String getStatusString() {
        if (!isReconnecting) {
            return "Connected";
        }
        
        if (currentAttempt == 0) {
            return "Connecting...";
        }
        
        return String.format("Reconnecting... (Attempt %d/%d)", currentAttempt, maxAttempts);
    }
}