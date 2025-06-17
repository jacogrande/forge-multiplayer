package forge.error;

import java.util.HashMap;
import java.util.Map;

/**
 * Error thrown when a network connection attempt times out.
 * This typically occurs when the server is unreachable or network latency is too high.
 */
public class ConnectionTimeoutError extends ConnectionError {
    
    private static final long serialVersionUID = 1L;
    
    private final long timeoutMs;
    private final String host;
    private final int port;
    
    /**
     * Creates a new ConnectionTimeoutError.
     * 
     * @param host Target host
     * @param port Target port
     * @param timeoutMs Timeout duration in milliseconds
     */
    public ConnectionTimeoutError(String host, int port, long timeoutMs) {
        this(host, port, timeoutMs, null);
    }
    
    /**
     * Creates a new ConnectionTimeoutError with cause.
     * 
     * @param host Target host
     * @param port Target port
     * @param timeoutMs Timeout duration in milliseconds
     * @param cause Underlying cause
     */
    public ConnectionTimeoutError(String host, int port, long timeoutMs, Throwable cause) {
        super(String.format("Connection to %s:%d timed out after %d ms", host, port, timeoutMs),
              true, cause, createContext(host, port, timeoutMs));
        this.host = host;
        this.port = port;
        this.timeoutMs = timeoutMs;
    }
    
    private static Map<String, Object> createContext(String host, int port, long timeoutMs) {
        Map<String, Object> context = new HashMap<>();
        context.put("host", host);
        context.put("port", port);
        context.put("timeoutMs", timeoutMs);
        return context;
    }
    
    /**
     * Gets the timeout duration.
     * 
     * @return Timeout in milliseconds
     */
    public long getTimeoutMs() {
        return timeoutMs;
    }
    
    /**
     * Gets the target host.
     * 
     * @return Host address
     */
    public String getHost() {
        return host;
    }
    
    /**
     * Gets the target port.
     * 
     * @return Port number
     */
    public int getPort() {
        return port;
    }
    
    @Override
    public String getUserMessage() {
        return String.format("Connection timed out. The server at %s:%d is not responding. " +
                           "Please check your network connection and try again.", host, port);
    }
    
    @Override
    public RecoveryStrategy getRecommendedRecoveryStrategy() {
        return RecoveryStrategy.RETRY;
    }
}