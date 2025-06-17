package forge.error;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Context object containing information needed for error recovery.
 * This provides access to system state, configuration, and recovery resources.
 */
public class RecoveryContext {
    
    private final Map<String, Object> context;
    private final Object connectionManager;
    private final Object gameStateManager;
    private final Object configurationProvider;
    
    /**
     * Creates a new RecoveryContext.
     * 
     * @param connectionManager Connection management interface (may be null)
     * @param gameStateManager Game state management interface (may be null)
     * @param configurationProvider Configuration access interface (may be null)
     */
    public RecoveryContext(Object connectionManager, Object gameStateManager, Object configurationProvider) {
        this.context = new HashMap<>();
        this.connectionManager = connectionManager;
        this.gameStateManager = gameStateManager;
        this.configurationProvider = configurationProvider;
    }
    
    /**
     * Creates a new RecoveryContext with additional context data.
     * 
     * @param connectionManager Connection management interface (may be null)
     * @param gameStateManager Game state management interface (may be null)
     * @param configurationProvider Configuration access interface (may be null)
     * @param initialContext Initial context data
     */
    public RecoveryContext(Object connectionManager, Object gameStateManager, 
                          Object configurationProvider, Map<String, Object> initialContext) {
        this.context = new HashMap<>(initialContext != null ? initialContext : Collections.emptyMap());
        this.connectionManager = connectionManager;
        this.gameStateManager = gameStateManager;
        this.configurationProvider = configurationProvider;
    }
    
    /**
     * Gets the connection manager if available.
     * 
     * @param type Expected type of the connection manager
     * @param <T> Type parameter
     * @return Optional containing the connection manager if available and of correct type
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getConnectionManager(Class<T> type) {
        if (connectionManager != null && type.isInstance(connectionManager)) {
            return Optional.of((T) connectionManager);
        }
        return Optional.empty();
    }
    
    /**
     * Gets the game state manager if available.
     * 
     * @param type Expected type of the game state manager
     * @param <T> Type parameter
     * @return Optional containing the game state manager if available and of correct type
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getGameStateManager(Class<T> type) {
        if (gameStateManager != null && type.isInstance(gameStateManager)) {
            return Optional.of((T) gameStateManager);
        }
        return Optional.empty();
    }
    
    /**
     * Gets the configuration provider if available.
     * 
     * @param type Expected type of the configuration provider
     * @param <T> Type parameter
     * @return Optional containing the configuration provider if available and of correct type
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getConfigurationProvider(Class<T> type) {
        if (configurationProvider != null && type.isInstance(configurationProvider)) {
            return Optional.of((T) configurationProvider);
        }
        return Optional.empty();
    }
    
    /**
     * Sets a context value.
     * 
     * @param key Context key
     * @param value Context value
     * @return This context for chaining
     */
    public RecoveryContext put(String key, Object value) {
        context.put(key, value);
        return this;
    }
    
    /**
     * Gets a context value.
     * 
     * @param key Context key
     * @return Optional containing the value if present
     */
    public Optional<Object> get(String key) {
        return Optional.ofNullable(context.get(key));
    }
    
    /**
     * Gets a typed context value.
     * 
     * @param key Context key
     * @param type Expected type
     * @param <T> Type parameter
     * @return Optional containing the typed value if present and of correct type
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        Object value = context.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of((T) value);
        }
        return Optional.empty();
    }
    
    /**
     * Gets all context data.
     * 
     * @return Unmodifiable map of context data
     */
    public Map<String, Object> getAll() {
        return Collections.unmodifiableMap(context);
    }
    
    /**
     * Checks if the context contains a specific key.
     * 
     * @param key Context key
     * @return true if the key is present
     */
    public boolean containsKey(String key) {
        return context.containsKey(key);
    }
    
    /**
     * Removes a context value.
     * 
     * @param key Context key
     * @return The removed value or null if not present
     */
    public Object remove(String key) {
        return context.remove(key);
    }
    
    /**
     * Clears all context data.
     */
    public void clear() {
        context.clear();
    }
    
    /**
     * Creates a copy of this context with additional data.
     * 
     * @param additionalContext Additional context data
     * @return New RecoveryContext with combined data
     */
    public RecoveryContext withAdditionalContext(Map<String, Object> additionalContext) {
        Map<String, Object> combined = new HashMap<>(this.context);
        if (additionalContext != null) {
            combined.putAll(additionalContext);
        }
        return new RecoveryContext(connectionManager, gameStateManager, configurationProvider, combined);
    }
    
    @Override
    public String toString() {
        return String.format("RecoveryContext{context=%s, hasConnectionManager=%s, hasGameStateManager=%s, hasConfigurationProvider=%s}",
                           context, connectionManager != null, gameStateManager != null, configurationProvider != null);
    }
}