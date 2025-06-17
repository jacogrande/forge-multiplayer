package forge.gui.network;

import java.util.logging.Logger;

/**
 * Adapter class that provides default implementations for ReconnectionObserver methods.
 * UI components can extend this class and override only the methods they need.
 */
public abstract class ReconnectionNotificationAdapter implements ReconnectionObserver {
    
    private static final Logger logger = Logger.getLogger(ReconnectionNotificationAdapter.class.getName());
    
    @Override
    public void onReconnectionStarted(DisconnectReason reason, int maxAttempts) {
        logger.info("Reconnection started: " + reason.getDescription() + " (max " + maxAttempts + " attempts)");
    }
    
    @Override
    public void onReconnectionAttempt(int attemptNumber, int maxAttempts, long delayMs) {
        logger.fine("Reconnection attempt " + attemptNumber + "/" + maxAttempts + " (delay: " + delayMs + "ms)");
    }
    
    @Override
    public void onReconnectionProgress(int attemptNumber, double progress, String status) {
        logger.finest("Reconnection progress: " + status + " (" + (progress * 100) + "%)");
    }
    
    @Override
    public void onReconnectionFailed(int attemptNumber, ReconnectionException exception, boolean willRetry) {
        if (willRetry) {
            logger.warning("Reconnection attempt " + attemptNumber + " failed: " + exception.getMessage() + " (will retry)");
        } else {
            logger.severe("Reconnection attempt " + attemptNumber + " failed: " + exception.getMessage() + " (no more retries)");
        }
    }
    
    @Override
    public void onReconnectionSucceeded(int attemptNumber, long totalDurationMs) {
        logger.info("Reconnection successful after " + attemptNumber + " attempts (" + totalDurationMs + "ms total)");
    }
    
    @Override
    public void onReconnectionGivenUp(ReconnectionException finalException, long totalDurationMs) {
        logger.severe("Reconnection abandoned: " + finalException.getMessage() + " (tried for " + totalDurationMs + "ms)");
    }
}