package forge.error;

import java.util.HashMap;
import java.util.Map;

/**
 * Error thrown when messages are received out of sequence or with invalid sequence numbers.
 * This indicates potential network reordering, dropped messages, or replay attacks.
 */
public class MessageSequenceError extends ProtocolError {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Types of sequence errors.
     */
    public enum SequenceErrorType {
        /**
         * Message received out of order.
         */
        OUT_OF_ORDER("Message received out of order"),
        
        /**
         * Duplicate message received.
         */
        DUPLICATE("Duplicate message received"),
        
        /**
         * Gap in sequence (missing messages).
         */
        SEQUENCE_GAP("Gap in message sequence"),
        
        /**
         * Sequence number overflow or invalid.
         */
        INVALID_SEQUENCE("Invalid sequence number"),
        
        /**
         * Message too old (potential replay attack).
         */
        TOO_OLD("Message too old");
        
        private final String description;
        
        SequenceErrorType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final SequenceErrorType sequenceErrorType;
    private final long expectedSequence;
    private final long receivedSequence;
    private final String messageType;
    
    /**
     * Creates a new MessageSequenceError.
     * 
     * @param sequenceErrorType Type of sequence error
     * @param messageType Type of message affected
     * @param expectedSequence Expected sequence number
     * @param receivedSequence Received sequence number
     */
    public MessageSequenceError(SequenceErrorType sequenceErrorType, String messageType,
                               long expectedSequence, long receivedSequence) {
        this(sequenceErrorType, messageType, expectedSequence, receivedSequence, null);
    }
    
    /**
     * Creates a new MessageSequenceError with cause.
     * 
     * @param sequenceErrorType Type of sequence error
     * @param messageType Type of message affected
     * @param expectedSequence Expected sequence number
     * @param receivedSequence Received sequence number
     * @param cause Underlying cause
     */
    public MessageSequenceError(SequenceErrorType sequenceErrorType, String messageType,
                               long expectedSequence, long receivedSequence, Throwable cause) {
        super(String.format("%s for %s: expected %d, received %d", 
                          sequenceErrorType.getDescription(), messageType, 
                          expectedSequence, receivedSequence),
              true, cause, createContext(sequenceErrorType, messageType, expectedSequence, receivedSequence));
        this.sequenceErrorType = sequenceErrorType;
        this.messageType = messageType;
        this.expectedSequence = expectedSequence;
        this.receivedSequence = receivedSequence;
    }
    
    private static Map<String, Object> createContext(SequenceErrorType type, String messageType, 
                                                    long expected, long received) {
        Map<String, Object> context = new HashMap<>();
        context.put("sequenceErrorType", type);
        context.put("messageType", messageType);
        context.put("expectedSequence", expected);
        context.put("receivedSequence", received);
        context.put("sequenceDiff", received - expected);
        return context;
    }
    
    /**
     * Gets the type of sequence error.
     * 
     * @return Sequence error type
     */
    public SequenceErrorType getSequenceErrorType() {
        return sequenceErrorType;
    }
    
    /**
     * Gets the type of message affected.
     * 
     * @return Message type
     */
    public String getMessageType() {
        return messageType;
    }
    
    /**
     * Gets the expected sequence number.
     * 
     * @return Expected sequence
     */
    public long getExpectedSequence() {
        return expectedSequence;
    }
    
    /**
     * Gets the received sequence number.
     * 
     * @return Received sequence
     */
    public long getReceivedSequence() {
        return receivedSequence;
    }
    
    /**
     * Gets the difference between received and expected sequence.
     * 
     * @return Sequence difference (positive if ahead, negative if behind)
     */
    public long getSequenceDifference() {
        return receivedSequence - expectedSequence;
    }
    
    @Override
    public String getUserMessage() {
        switch (sequenceErrorType) {
            case OUT_OF_ORDER:
                return "Messages are being received out of order. Network connection may be unstable.";
            case DUPLICATE:
                return "Duplicate message received. This may indicate a network issue.";
            case SEQUENCE_GAP:
                return "Some messages may have been lost. Attempting to recover...";
            case INVALID_SEQUENCE:
            case TOO_OLD:
                return "Invalid message sequence detected. Connection may be compromised.";
            default:
                return "Message sequence error: " + sequenceErrorType.getDescription();
        }
    }
    
    @Override
    public NetworkError.Severity getSeverity() {
        // Potential security issues should be treated as critical
        switch (sequenceErrorType) {
            case TOO_OLD:
            case INVALID_SEQUENCE:
                return Severity.CRITICAL;
            default:
                return super.getSeverity();
        }
    }
    
    @Override
    public RecoveryStrategy getRecommendedRecoveryStrategy() {
        switch (sequenceErrorType) {
            case OUT_OF_ORDER:
            case DUPLICATE:
                return RecoveryStrategy.RETRY; // May resolve with time
            case SEQUENCE_GAP:
                return RecoveryStrategy.RESYNC; // Need to fill gaps
            case TOO_OLD:
            case INVALID_SEQUENCE:
                return RecoveryStrategy.RECONNECT; // Potential security issue
            default:
                return RecoveryStrategy.RETRY;
        }
    }
}