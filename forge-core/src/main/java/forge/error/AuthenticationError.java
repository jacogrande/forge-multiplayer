package forge.error;

import java.util.HashMap;
import java.util.Map;

/**
 * Error thrown when authentication fails during connection establishment.
 * This includes invalid credentials, expired tokens, or authentication protocol failures.
 */
public class AuthenticationError extends SecurityError {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Authentication failure reasons.
     */
    public enum Reason {
        /**
         * Invalid username or password.
         */
        INVALID_CREDENTIALS("Invalid username or password"),
        
        /**
         * Authentication token has expired.
         */
        TOKEN_EXPIRED("Authentication token expired"),
        
        /**
         * Authentication token is invalid.
         */
        TOKEN_INVALID("Invalid authentication token"),
        
        /**
         * Account is locked or suspended.
         */
        ACCOUNT_LOCKED("Account is locked"),
        
        /**
         * Authentication protocol error.
         */
        PROTOCOL_ERROR("Authentication protocol error"),
        
        /**
         * Server authentication configuration error.
         */
        SERVER_ERROR("Server authentication error");
        
        private final String description;
        
        Reason(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final Reason reason;
    private final String username;
    
    /**
     * Creates a new AuthenticationError.
     * 
     * @param reason Authentication failure reason
     * @param username Username that failed authentication (may be null)
     */
    public AuthenticationError(Reason reason, String username) {
        this(reason, username, null);
    }
    
    /**
     * Creates a new AuthenticationError with cause.
     * 
     * @param reason Authentication failure reason
     * @param username Username that failed authentication (may be null)
     * @param cause Underlying cause
     */
    public AuthenticationError(Reason reason, String username, Throwable cause) {
        super(String.format("Authentication failed: %s", reason.getDescription()),
              reason == Reason.SERVER_ERROR, cause, createContext(reason, username));
        this.reason = reason;
        this.username = username;
    }
    
    private static Map<String, Object> createContext(Reason reason, String username) {
        Map<String, Object> context = new HashMap<>();
        context.put("reason", reason);
        if (username != null) {
            context.put("username", username);
        }
        context.put("authType", Type.AUTHENTICATION);
        return context;
    }
    
    /**
     * Gets the authentication failure reason.
     * 
     * @return Failure reason
     */
    public Reason getReason() {
        return reason;
    }
    
    /**
     * Gets the username that failed authentication.
     * 
     * @return Username or null if not applicable
     */
    public String getUsername() {
        return username;
    }
    
    @Override
    public String getUserMessage() {
        switch (reason) {
            case INVALID_CREDENTIALS:
                return "Invalid username or password. Please check your credentials and try again.";
            case TOKEN_EXPIRED:
                return "Your session has expired. Please log in again.";
            case TOKEN_INVALID:
                return "Authentication failed. Please log in again.";
            case ACCOUNT_LOCKED:
                return "Your account has been locked. Please contact support.";
            case PROTOCOL_ERROR:
            case SERVER_ERROR:
                return "Authentication service is temporarily unavailable. Please try again later.";
            default:
                return "Authentication failed: " + reason.getDescription();
        }
    }
    
    @Override
    public RecoveryStrategy getRecommendedRecoveryStrategy() {
        switch (reason) {
            case TOKEN_EXPIRED:
            case TOKEN_INVALID:
                return RecoveryStrategy.USER_INTERVENTION; // Re-login required
            case SERVER_ERROR:
                return RecoveryStrategy.RETRY; // Temporary issue
            default:
                return RecoveryStrategy.NONE; // User must fix credentials
        }
    }
}