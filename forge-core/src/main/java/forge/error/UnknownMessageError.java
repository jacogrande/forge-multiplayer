package forge.error;

import java.util.HashMap;
import java.util.Map;

/**
 * Error thrown when an unknown or unsupported message type is received.
 * This typically indicates a protocol version mismatch or corrupted message.
 */
public class UnknownMessageError extends ProtocolError {
    
    private static final long serialVersionUID = 1L;
    
    private final String messageType;
    private final String protocolVersion;
    private final int messageSize;
    
    /**
     * Creates a new UnknownMessageError.
     * 
     * @param messageType The unknown message type
     * @param protocolVersion Protocol version in use
     */
    public UnknownMessageError(String messageType, String protocolVersion) {
        this(messageType, protocolVersion, -1, null);
    }
    
    /**
     * Creates a new UnknownMessageError with message size.
     * 
     * @param messageType The unknown message type
     * @param protocolVersion Protocol version in use
     * @param messageSize Size of the message in bytes
     */
    public UnknownMessageError(String messageType, String protocolVersion, int messageSize) {
        this(messageType, protocolVersion, messageSize, null);
    }
    
    /**
     * Creates a new UnknownMessageError with cause.
     * 
     * @param messageType The unknown message type
     * @param protocolVersion Protocol version in use
     * @param messageSize Size of the message in bytes
     * @param cause Underlying cause
     */
    public UnknownMessageError(String messageType, String protocolVersion, int messageSize, Throwable cause) {
        super(String.format("Unknown message type '%s' for protocol version %s", 
                          messageType, protocolVersion),
              true, cause, createContext(messageType, protocolVersion, messageSize));
        this.messageType = messageType;
        this.protocolVersion = protocolVersion;
        this.messageSize = messageSize;
    }
    
    private static Map<String, Object> createContext(String type, String version, int size) {
        Map<String, Object> context = new HashMap<>();
        context.put("messageType", type);
        context.put("protocolVersion", version);
        if (size >= 0) {
            context.put("messageSize", size);
        }
        return context;
    }
    
    /**
     * Gets the unknown message type.
     * 
     * @return Message type
     */
    public String getMessageType() {
        return messageType;
    }
    
    /**
     * Gets the protocol version.
     * 
     * @return Protocol version
     */
    public String getProtocolVersion() {
        return protocolVersion;
    }
    
    /**
     * Gets the message size in bytes.
     * 
     * @return Message size or -1 if not available
     */
    public int getMessageSize() {
        return messageSize;
    }
    
    @Override
    public String getUserMessage() {
        return "Received an unknown message from the server. This may indicate a protocol version mismatch. " +
               "Please ensure both client and server are using the same version.";
    }
    
    @Override
    public RecoveryStrategy getRecommendedRecoveryStrategy() {
        return RecoveryStrategy.RECONNECT; // May resolve version mismatch
    }
}