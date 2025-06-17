package forge.util.serialization;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for creating and managing NetworkProtocol instances.
 * 
 * Provides centralized access to different serialization protocols with
 * caching and configuration management. Supports easy switching between
 * protocols for performance comparison and gradual migration.
 */
public class NetworkProtocolFactory {
    
    /**
     * Available protocol types.
     */
    public enum ProtocolType {
        KRYO("kryo"),
        JAVA("java");
        
        private final String identifier;
        
        ProtocolType(String identifier) {
            this.identifier = identifier;
        }
        
        public String getIdentifier() {
            return identifier;
        }
        
        public static ProtocolType fromIdentifier(String identifier) {
            for (ProtocolType type : values()) {
                if (type.identifier.equals(identifier)) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Unknown protocol type: " + identifier);
        }
    }
    
    // Cache protocol instances for reuse
    private static final ConcurrentHashMap<ProtocolType, NetworkProtocol> protocolCache = new ConcurrentHashMap<>();
    
    // Default protocol type
    private static volatile ProtocolType defaultProtocol = ProtocolType.KRYO;
    
    /**
     * Gets the default network protocol instance.
     * 
     * @return Default NetworkProtocol instance
     */
    public static NetworkProtocol getDefault() {
        return getProtocol(defaultProtocol);
    }
    
    /**
     * Gets a network protocol instance for the specified type.
     * 
     * @param type Protocol type
     * @return NetworkProtocol instance
     */
    public static NetworkProtocol getProtocol(ProtocolType type) {
        return protocolCache.computeIfAbsent(type, NetworkProtocolFactory::createProtocol);
    }
    
    /**
     * Gets a network protocol instance by identifier string.
     * 
     * @param identifier Protocol identifier
     * @return NetworkProtocol instance
     */
    public static NetworkProtocol getProtocol(String identifier) {
        ProtocolType type = ProtocolType.fromIdentifier(identifier);
        return getProtocol(type);
    }
    
    /**
     * Sets the default protocol type.
     * 
     * @param type New default protocol type
     */
    public static void setDefaultProtocol(ProtocolType type) {
        defaultProtocol = type;
    }
    
    /**
     * Creates a new protocol instance for the specified type.
     * 
     * @param type Protocol type
     * @return New NetworkProtocol instance
     */
    private static NetworkProtocol createProtocol(ProtocolType type) {
        switch (type) {
            case KRYO:
                return new KryoNetworkProtocol();
            case JAVA:
                return new JavaNetworkProtocol();
            default:
                throw new IllegalArgumentException("Unsupported protocol type: " + type);
        }
    }
    
    /**
     * Clears the protocol cache, forcing recreation of instances.
     * Useful for testing or configuration changes.
     */
    public static void clearCache() {
        protocolCache.clear();
    }
    
    /**
     * Gets information about available protocols.
     * 
     * @return Array of available protocol types
     */
    public static ProtocolType[] getAvailableProtocols() {
        return ProtocolType.values();
    }
    
    /**
     * Checks if a protocol type is available.
     * 
     * @param type Protocol type to check
     * @return true if available, false otherwise
     */
    public static boolean isProtocolAvailable(ProtocolType type) {
        try {
            createProtocol(type);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}