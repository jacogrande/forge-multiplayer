package forge.util.serialization;

/**
 * Exception thrown when serialization or deserialization operations fail.
 * Provides detailed error information for debugging network protocol issues.
 */
public class SerializationException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    private final String protocol;
    private final String operation;
    
    /**
     * Creates a new SerializationException with a message.
     * 
     * @param message Error message
     */
    public SerializationException(String message) {
        super(message);
        this.protocol = "unknown";
        this.operation = "unknown";
    }
    
    /**
     * Creates a new SerializationException with a message and cause.
     * 
     * @param message Error message
     * @param cause Underlying cause
     */
    public SerializationException(String message, Throwable cause) {
        super(message, cause);
        this.protocol = "unknown";
        this.operation = "unknown";
    }
    
    /**
     * Creates a new SerializationException with detailed context.
     * 
     * @param message Error message
     * @param protocol Name of the serialization protocol
     * @param operation Operation that failed (serialize/deserialize)
     */
    public SerializationException(String message, String protocol, String operation) {
        super(message);
        this.protocol = protocol != null ? protocol : "unknown";
        this.operation = operation != null ? operation : "unknown";
    }
    
    /**
     * Creates a new SerializationException with detailed context and cause.
     * 
     * @param message Error message
     * @param cause Underlying cause
     * @param protocol Name of the serialization protocol
     * @param operation Operation that failed (serialize/deserialize)
     */
    public SerializationException(String message, Throwable cause, String protocol, String operation) {
        super(message, cause);
        this.protocol = protocol != null ? protocol : "unknown";
        this.operation = operation != null ? operation : "unknown";
    }
    
    /**
     * Gets the name of the protocol that caused this exception.
     * 
     * @return Protocol name
     */
    public String getProtocol() {
        return protocol;
    }
    
    /**
     * Gets the operation that was being performed when this exception occurred.
     * 
     * @return Operation name (serialize/deserialize/etc.)
     */
    public String getOperation() {
        return operation;
    }
    
    @Override
    public String getMessage() {
        String baseMessage = super.getMessage();
        if ("unknown".equals(protocol) && "unknown".equals(operation)) {
            return baseMessage;
        }
        return String.format("[%s:%s] %s", protocol, operation, baseMessage);
    }
}