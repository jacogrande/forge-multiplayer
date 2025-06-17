package forge.gui.network;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Console-based reconnection notifier for command-line interfaces and testing.
 * Displays reconnection progress and status to System.out.
 */
public class ConsoleReconnectionNotifier extends ReconnectionNotificationAdapter {
    
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private final boolean verbose;
    
    /**
     * Creates a console notifier with default verbosity (false).
     */
    public ConsoleReconnectionNotifier() {
        this(false);
    }
    
    /**
     * Creates a console notifier with specified verbosity.
     * 
     * @param verbose If true, shows detailed progress messages
     */
    public ConsoleReconnectionNotifier(boolean verbose) {
        this.verbose = verbose;
    }
    
    @Override
    public void onReconnectionStarted(DisconnectReason reason, int maxAttempts) {
        printMessage("🔴 CONNECTION LOST", reason.getDescription());
        printMessage("⏳ RECONNECTING", "Will attempt up to " + maxAttempts + " times");
        System.out.println();
    }
    
    @Override
    public void onReconnectionAttempt(int attemptNumber, int maxAttempts, long delayMs) {
        if (attemptNumber == 1) {
            printMessage("🔄 ATTEMPT " + attemptNumber + "/" + maxAttempts, "Connecting...");
        } else {
            printMessage("🔄 ATTEMPT " + attemptNumber + "/" + maxAttempts, 
                        "Retrying in " + (delayMs / 1000) + " seconds...");
        }
    }
    
    @Override
    public void onReconnectionProgress(int attemptNumber, double progress, String status) {
        if (verbose) {
            int progressBar = (int) (progress * 20);
            StringBuilder bar = new StringBuilder("[");
            for (int i = 0; i < 20; i++) {
                if (i < progressBar) {
                    bar.append("█");
                } else {
                    bar.append("░");
                }
            }
            bar.append("] ");
            bar.append(String.format("%.0f%%", progress * 100));
            
            System.out.print("\r" + bar + " " + status);
            if (progress >= 1.0) {
                System.out.println(); // New line when complete
            }
        }
    }
    
    @Override
    public void onReconnectionFailed(int attemptNumber, ReconnectionException exception, boolean willRetry) {
        if (verbose || !willRetry) {
            String status = willRetry ? "Failed, will retry..." : "Failed, no more retries";
            printMessage("❌ ATTEMPT " + attemptNumber + " FAILED", status);
            if (verbose) {
                System.out.println("   Reason: " + exception.getMessage());
            }
        }
    }
    
    @Override
    public void onReconnectionSucceeded(int attemptNumber, long totalDurationMs) {
        System.out.println(); // Clear any progress line
        printMessage("✅ RECONNECTED", "Success after " + attemptNumber + " attempts");
        printMessage("⏱️  DURATION", formatDuration(totalDurationMs));
        System.out.println();
    }
    
    @Override
    public void onReconnectionGivenUp(ReconnectionException finalException, long totalDurationMs) {
        System.out.println(); // Clear any progress line
        printMessage("❌ RECONNECTION FAILED", "All attempts exhausted");
        printMessage("⏱️  DURATION", formatDuration(totalDurationMs));
        printMessage("💡 SUGGESTION", "Check your network connection and try again");
        System.out.println();
    }
    
    /**
     * Prints a formatted message with timestamp.
     */
    private void printMessage(String label, String message) {
        String timestamp = TIME_FORMAT.format(new Date());
        System.out.printf("[%s] %-25s %s%n", timestamp, label + ":", message);
    }
    
    /**
     * Formats a duration in milliseconds to a human-readable string.
     */
    private String formatDuration(long milliseconds) {
        if (milliseconds < 1000) {
            return milliseconds + "ms";
        } else if (milliseconds < 60000) {
            return String.format("%.1f seconds", milliseconds / 1000.0);
        } else {
            long minutes = milliseconds / 60000;
            long seconds = (milliseconds % 60000) / 1000;
            return String.format("%d minutes %d seconds", minutes, seconds);
        }
    }
}