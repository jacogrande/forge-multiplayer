package forge.error;

import java.util.HashMap;
import java.util.Map;

/**
 * Error thrown when a message cannot be parsed or is structurally invalid.
 * This includes serialization errors, truncated messages, or corrupted data.
 */
public class MalformedMessageError extends ProtocolError {
    
    private static final long serialVersionUID = 1L;
    
    private final String messageType;
    private final int expectedSize;
    private final int actualSize;
    private final String parseError;
    
    /**
     * Creates a new MalformedMessageError.
     * 
     * @param messageType Type of message that was malformed
     * @param parseError Description of the parsing error
     */
    public MalformedMessageError(String messageType, String parseError) {
        this(messageType, parseError, -1, -1, null);
    }
    
    /**
     * Creates a new MalformedMessageError with size information.
     * 
     * @param messageType Type of message that was malformed
     * @param parseError Description of the parsing error
     * @param expectedSize Expected message size
     * @param actualSize Actual message size
     */
    public MalformedMessageError(String messageType, String parseError, int expectedSize, int actualSize) {
        this(messageType, parseError, expectedSize, actualSize, null);
    }
    
    /**
     * Creates a new MalformedMessageError with cause.
     * 
     * @param messageType Type of message that was malformed
     * @param parseError Description of the parsing error
     * @param expectedSize Expected message size
     * @param actualSize Actual message size
     * @param cause Underlying cause
     */
    public MalformedMessageError(String messageType, String parseError, int expectedSize, 
                                int actualSize, Throwable cause) {
        super(String.format("Malformed %s message: %s", messageType, parseError),
              true, cause, createContext(messageType, parseError, expectedSize, actualSize));
        this.messageType = messageType;
        this.parseError = parseError;
        this.expectedSize = expectedSize;
        this.actualSize = actualSize;
    }
    
    private static Map<String, Object> createContext(String type, String error, int expected, int actual) {
        Map<String, Object> context = new HashMap<>();
        context.put("messageType", type);
        context.put("parseError", error);
        if (expected >= 0) {
            context.put("expectedSize", expected);
        }
        if (actual >= 0) {
            context.put("actualSize", actual);
        }
        return context;
    }
    
    /**
     * Gets the type of message that was malformed.
     * 
     * @return Message type
     */
    public String getMessageType() {
        return messageType;
    }
    
    /**
     * Gets the parsing error description.
     * 
     * @return Parse error description
     */
    public String getParseError() {
        return parseError;
    }
    
    /**
     * Gets the expected message size.
     * 
     * @return Expected size or -1 if not available
     */
    public int getExpectedSize() {
        return expectedSize;
    }
    
    /**
     * Gets the actual message size.
     * 
     * @return Actual size or -1 if not available
     */
    public int getActualSize() {
        return actualSize;
    }
    
    /**
     * Determines if this is a size mismatch error.
     * 
     * @return true if expected and actual sizes are different
     */
    public boolean isSizeMismatch() {
        return expectedSize >= 0 && actualSize >= 0 && expectedSize != actualSize;
    }
    
    @Override
    public String getUserMessage() {
        if (isSizeMismatch()) {
            return "Received a corrupted message from the server. Connection may be unstable.";
        } else {
            return "Received an invalid message from the server. This may indicate a protocol issue.";
        }
    }
    
    @Override
    public RecoveryStrategy getRecommendedRecoveryStrategy() {
        return RecoveryStrategy.RETRY; // Message may be retransmitted successfully
    }
}