package forge.error;

import java.util.HashMap;
import java.util.Map;

/**
 * Error thrown when security validation fails for game actions or data.
 * This includes invalid moves, tampered data, or protocol violations that suggest cheating.
 */
public class ValidationError extends SecurityError {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Types of validation failures.
     */
    public enum ValidationType {
        /**
         * Game action is invalid according to rules.
         */
        INVALID_ACTION("Invalid game action"),
        
        /**
         * Data integrity check failed.
         */
        DATA_INTEGRITY("Data integrity violation"),
        
        /**
         * Message signature verification failed.
         */
        SIGNATURE_MISMATCH("Message signature mismatch"),
        
        /**
         * Player attempted to access hidden information.
         */
        HIDDEN_INFORMATION("Attempted access to hidden information"),
        
        /**
         * Game state checksum mismatch.
         */
        STATE_MISMATCH("Game state mismatch"),
        
        /**
         * Action performed out of turn or without priority.
         */
        TURN_VIOLATION("Turn order violation"),
        
        /**
         * Rate limit exceeded (possible spam/DOS).
         */
        RATE_LIMIT("Rate limit exceeded");
        
        private final String description;
        
        ValidationType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final ValidationType validationType;
    private final String details;
    private final int playerId;
    
    /**
     * Creates a new ValidationError.
     * 
     * @param validationType Type of validation failure
     * @param playerId ID of the player whose action failed validation
     * @param details Additional details about the failure
     */
    public ValidationError(ValidationType validationType, int playerId, String details) {
        this(validationType, playerId, details, null);
    }
    
    /**
     * Creates a new ValidationError with cause.
     * 
     * @param validationType Type of validation failure
     * @param playerId ID of the player whose action failed validation
     * @param details Additional details about the failure
     * @param cause Underlying cause
     */
    public ValidationError(ValidationType validationType, int playerId, String details, Throwable cause) {
        super(String.format("Validation failed [Player %d]: %s - %s", 
                          playerId, validationType.getDescription(), details),
              Severity.WARN, // Most validation errors are warnings unless they indicate cheating
              false, cause, createContext(validationType, playerId, details));
        this.validationType = validationType;
        this.playerId = playerId;
        this.details = details;
    }
    
    private static Map<String, Object> createContext(ValidationType type, int playerId, String details) {
        Map<String, Object> context = new HashMap<>();
        context.put("validationType", type);
        context.put("playerId", playerId);
        context.put("details", details);
        context.put("timestamp", System.currentTimeMillis());
        return context;
    }
    
    /**
     * Gets the type of validation failure.
     * 
     * @return Validation type
     */
    public ValidationType getValidationType() {
        return validationType;
    }
    
    /**
     * Gets additional details about the validation failure.
     * 
     * @return Failure details
     */
    public String getDetails() {
        return details;
    }
    
    /**
     * Gets the ID of the player whose action failed validation.
     * 
     * @return Player ID
     */
    public int getPlayerId() {
        return playerId;
    }
    
    @Override
    public String getUserMessage() {
        switch (validationType) {
            case INVALID_ACTION:
                return "That action is not allowed at this time.";
            case DATA_INTEGRITY:
            case SIGNATURE_MISMATCH:
                return "Data validation failed. Please reconnect to the game.";
            case HIDDEN_INFORMATION:
                return "You cannot access that information.";
            case STATE_MISMATCH:
                return "Your game state is out of sync. Resyncing...";
            case TURN_VIOLATION:
                return "It's not your turn to perform that action.";
            case RATE_LIMIT:
                return "Too many actions. Please slow down.";
            default:
                return "Action validation failed: " + details;
        }
    }
    
    @Override
    public NetworkError.Severity getSeverity() {
        // Upgrade severity for potential cheating attempts
        switch (validationType) {
            case DATA_INTEGRITY:
            case SIGNATURE_MISMATCH:
            case HIDDEN_INFORMATION:
                return Severity.CRITICAL;
            default:
                return super.getSeverity();
        }
    }
    
    @Override
    public RecoveryStrategy getRecommendedRecoveryStrategy() {
        switch (validationType) {
            case STATE_MISMATCH:
                return RecoveryStrategy.RESYNC;
            case RATE_LIMIT:
                return RecoveryStrategy.RETRY; // With backoff
            default:
                return RecoveryStrategy.NONE;
        }
    }
}