package forge.error;

import java.util.HashMap;
import java.util.Map;

/**
 * Error thrown when a connection is actively refused by the target server.
 * This typically indicates the server is not running or not accepting connections on the specified port.
 */
public class ConnectionRefusedError extends ConnectionError {
    
    private static final long serialVersionUID = 1L;
    
    private final String host;
    private final int port;
    
    /**
     * Creates a new ConnectionRefusedError.
     * 
     * @param host Target host
     * @param port Target port
     */
    public ConnectionRefusedError(String host, int port) {
        this(host, port, null);
    }
    
    /**
     * Creates a new ConnectionRefusedError with cause.
     * 
     * @param host Target host
     * @param port Target port
     * @param cause Underlying cause
     */
    public ConnectionRefusedError(String host, int port, Throwable cause) {
        super(String.format("Connection refused by %s:%d", host, port),
              Severity.ERROR, true, cause, createContext(host, port));
        this.host = host;
        this.port = port;
    }
    
    private static Map<String, Object> createContext(String host, int port) {
        Map<String, Object> context = new HashMap<>();
        context.put("host", host);
        context.put("port", port);
        context.put("reason", "CONNECTION_REFUSED");
        return context;
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
        return String.format("Unable to connect to server at %s:%d. " +
                           "The server may not be running or may be configured to use a different port.", 
                           host, port);
    }
    
    @Override
    public RecoveryStrategy getRecommendedRecoveryStrategy() {
        return RecoveryStrategy.RETRY;
    }
}