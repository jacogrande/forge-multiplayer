package forge.error;

import java.util.HashMap;
import java.util.Map;

/**
 * Error thrown when system resources are exhausted or unavailable.
 * This includes memory, disk space, network capacity, or other limited resources.
 */
public class ResourceError extends ApplicationError {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Types of resource errors.
     */
    public enum ResourceType {
        /**
         * Insufficient memory available.
         */
        MEMORY("Insufficient memory"),
        
        /**
         * Insufficient disk space.
         */
        DISK_SPACE("Insufficient disk space"),
        
        /**
         * Network bandwidth exhausted.
         */
        NETWORK_BANDWIDTH("Network bandwidth exhausted"),
        
        /**
         * Too many open connections.
         */
        CONNECTION_LIMIT("Connection limit exceeded"),
        
        /**
         * CPU resources exhausted.
         */
        CPU("CPU resources exhausted"),
        
        /**
         * File handle limit reached.
         */
        FILE_HANDLES("File handle limit exceeded"),
        
        /**
         * Thread pool exhausted.
         */
        THREAD_POOL("Thread pool exhausted");
        
        private final String description;
        
        ResourceType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final ResourceType resourceType;
    private final long currentUsage;
    private final long maxAvailable;
    private final String resourceName;
    
    /**
     * Creates a new ResourceError.
     * 
     * @param resourceType Type of resource that is exhausted
     * @param resourceName Name of the specific resource
     */
    public ResourceError(ResourceType resourceType, String resourceName) {
        this(resourceType, resourceName, -1, -1, null);
    }
    
    /**
     * Creates a new ResourceError with usage information.
     * 
     * @param resourceType Type of resource that is exhausted
     * @param resourceName Name of the specific resource
     * @param currentUsage Current resource usage
     * @param maxAvailable Maximum available resource
     */
    public ResourceError(ResourceType resourceType, String resourceName, 
                        long currentUsage, long maxAvailable) {
        this(resourceType, resourceName, currentUsage, maxAvailable, null);
    }
    
    /**
     * Creates a new ResourceError with cause.
     * 
     * @param resourceType Type of resource that is exhausted
     * @param resourceName Name of the specific resource
     * @param currentUsage Current resource usage
     * @param maxAvailable Maximum available resource
     * @param cause Underlying cause
     */
    public ResourceError(ResourceType resourceType, String resourceName, 
                        long currentUsage, long maxAvailable, Throwable cause) {
        super(String.format("%s: %s", resourceType.getDescription(), resourceName),
              resourceType == ResourceType.MEMORY ? false : true, // Memory errors usually not recoverable
              cause, createContext(resourceType, resourceName, currentUsage, maxAvailable));
        this.resourceType = resourceType;
        this.resourceName = resourceName;
        this.currentUsage = currentUsage;
        this.maxAvailable = maxAvailable;
    }
    
    private static Map<String, Object> createContext(ResourceType type, String name, long current, long max) {
        Map<String, Object> context = new HashMap<>();
        context.put("resourceType", type);
        context.put("resourceName", name);
        if (current >= 0) {
            context.put("currentUsage", current);
        }
        if (max >= 0) {
            context.put("maxAvailable", max);
        }
        if (current >= 0 && max >= 0) {
            context.put("usagePercent", (double) current / max * 100);
        }
        return context;
    }
    
    /**
     * Gets the type of resource that is exhausted.
     * 
     * @return Resource type
     */
    public ResourceType getResourceType() {
        return resourceType;
    }
    
    /**
     * Gets the name of the specific resource.
     * 
     * @return Resource name
     */
    public String getResourceName() {
        return resourceName;
    }
    
    /**
     * Gets the current resource usage.
     * 
     * @return Current usage or -1 if not available
     */
    public long getCurrentUsage() {
        return currentUsage;
    }
    
    /**
     * Gets the maximum available resource.
     * 
     * @return Maximum available or -1 if not available
     */
    public long getMaxAvailable() {
        return maxAvailable;
    }
    
    /**
     * Calculates the usage percentage.
     * 
     * @return Usage percentage or -1 if not calculable
     */
    public double getUsagePercentage() {
        if (currentUsage >= 0 && maxAvailable > 0) {
            return (double) currentUsage / maxAvailable * 100;
        }
        return -1;
    }
    
    @Override
    public String getUserMessage() {
        switch (resourceType) {
            case MEMORY:
                return "The application is running low on memory. Please close other applications and try again.";
            case DISK_SPACE:
                return "Insufficient disk space available. Please free up some disk space and try again.";
            case NETWORK_BANDWIDTH:
                return "Network bandwidth is insufficient. Please reduce network usage and try again.";
            case CONNECTION_LIMIT:
                return "Maximum number of connections reached. Please wait for others to disconnect.";
            case CPU:
                return "System is under heavy load. Please wait and try again.";
            case FILE_HANDLES:
                return "Too many files are open. Please close unnecessary applications.";
            case THREAD_POOL:
                return "System is busy processing other requests. Please wait and try again.";
            default:
                return String.format("Resource unavailable: %s", resourceType.getDescription());
        }
    }
    
    @Override
    public RecoveryStrategy getRecommendedRecoveryStrategy() {
        switch (resourceType) {
            case MEMORY:
                return RecoveryStrategy.NONE; // Requires user intervention
            case CONNECTION_LIMIT:
            case CPU:
            case THREAD_POOL:
                return RecoveryStrategy.RETRY; // May resolve over time
            default:
                return RecoveryStrategy.FALLBACK;
        }
    }
}