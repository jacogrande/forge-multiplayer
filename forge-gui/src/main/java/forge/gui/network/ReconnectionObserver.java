package forge.gui.network;

/**
 * Observer interface for receiving notifications about reconnection progress.
 * Allows UI components to provide user feedback during reconnection attempts.
 */
public interface ReconnectionObserver {
    
    /**
     * Called when a reconnection process begins.
     * 
     * @param reason The reason for the disconnection
     * @param maxAttempts The maximum number of attempts that will be made
     */
    void onReconnectionStarted(DisconnectReason reason, int maxAttempts);
    
    /**
     * Called for each reconnection attempt.
     * 
     * @param attemptNumber The current attempt number (1-based)
     * @param maxAttempts The maximum number of attempts
     * @param delayMs The delay before this attempt
     */
    void onReconnectionAttempt(int attemptNumber, int maxAttempts, long delayMs);
    
    /**
     * Called to report progress during a reconnection attempt.
     * 
     * @param attemptNumber The current attempt number
     * @param progress A value between 0.0 and 1.0 indicating progress
     * @param status A human-readable status message
     */
    void onReconnectionProgress(int attemptNumber, double progress, String status);
    
    /**
     * Called when a reconnection attempt fails.
     * 
     * @param attemptNumber The failed attempt number
     * @param exception The exception that caused the failure
     * @param willRetry true if another attempt will be made
     */
    void onReconnectionFailed(int attemptNumber, ReconnectionException exception, boolean willRetry);
    
    /**
     * Called when reconnection succeeds.
     * 
     * @param attemptNumber The successful attempt number
     * @param totalDurationMs The total time taken for all attempts
     */
    void onReconnectionSucceeded(int attemptNumber, long totalDurationMs);
    
    /**
     * Called when all reconnection attempts have been exhausted.
     * 
     * @param finalException The final exception that caused failure
     * @param totalDurationMs The total time spent on all attempts
     */
    void onReconnectionGivenUp(ReconnectionException finalException, long totalDurationMs);
}