package forge.error;

import java.util.HashMap;
import java.util.Map;

/**
 * Error thrown when a user lacks the necessary permissions to perform an action.
 * This is distinct from authentication - the user is authenticated but not authorized.
 */
public class AuthorizationError extends SecurityError {
    
    private static final long serialVersionUID = 1L;
    
    private final String action;
    private final String resource;
    private final String requiredPermission;
    
    /**
     * Creates a new AuthorizationError.
     * 
     * @param action The action that was denied
     * @param resource The resource being accessed
     */
    public AuthorizationError(String action, String resource) {
        this(action, resource, null, null);
    }
    
    /**
     * Creates a new AuthorizationError with required permission.
     * 
     * @param action The action that was denied
     * @param resource The resource being accessed
     * @param requiredPermission The permission that was required
     */
    public AuthorizationError(String action, String resource, String requiredPermission) {
        this(action, resource, requiredPermission, null);
    }
    
    /**
     * Creates a new AuthorizationError with cause.
     * 
     * @param action The action that was denied
     * @param resource The resource being accessed
     * @param requiredPermission The permission that was required
     * @param cause Underlying cause
     */
    public AuthorizationError(String action, String resource, String requiredPermission, Throwable cause) {
        super(String.format("Not authorized to %s on %s", action, resource),
              false, cause, createContext(action, resource, requiredPermission));
        this.action = action;
        this.resource = resource;
        this.requiredPermission = requiredPermission;
    }
    
    private static Map<String, Object> createContext(String action, String resource, String requiredPermission) {
        Map<String, Object> context = new HashMap<>();
        context.put("action", action);
        context.put("resource", resource);
        if (requiredPermission != null) {
            context.put("requiredPermission", requiredPermission);
        }
        return context;
    }
    
    /**
     * Gets the action that was denied.
     * 
     * @return Denied action
     */
    public String getAction() {
        return action;
    }
    
    /**
     * Gets the resource that was being accessed.
     * 
     * @return Resource identifier
     */
    public String getResource() {
        return resource;
    }
    
    /**
     * Gets the permission that was required.
     * 
     * @return Required permission or null if not specified
     */
    public String getRequiredPermission() {
        return requiredPermission;
    }
    
    @Override
    public String getUserMessage() {
        if (requiredPermission != null) {
            return String.format("You don't have permission to %s. Required permission: %s", 
                               action, requiredPermission);
        } else {
            return String.format("You don't have permission to %s.", action);
        }
    }
    
    @Override
    public RecoveryStrategy getRecommendedRecoveryStrategy() {
        return RecoveryStrategy.NONE; // User needs proper permissions
    }
}