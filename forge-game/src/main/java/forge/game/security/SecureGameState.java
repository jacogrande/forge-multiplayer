package forge.game.security;

import forge.game.Game;
import forge.game.GameView;
import forge.game.player.actions.PlayerAction;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Secure game state manager that provides authoritative game state management
 * with player-specific filtering and action validation for multiplayer games.
 * 
 * This class serves as the central authority for:
 * - Providing filtered game views per player perspective
 * - Validating player actions to prevent cheating
 * - Managing state synchronization and caching
 * - Enforcing security boundaries for hidden information
 */
public class SecureGameState {
    
    private final Game authoritativeGame;
    private final SecurityValidator validator;
    private final ActionValidator actionValidator;
    private final ConcurrentHashMap<Integer, PlayerViewCache> playerCaches;
    private final AtomicLong stateVersion;
    private final ScheduledExecutorService cacheCleanup;
    private final List<StateChangeListener> listeners;
    
    // Performance tuning constants
    private static final long CACHE_CLEANUP_INTERVAL_MS = 30_000; // 30 seconds
    private static final long CACHE_EXPIRY_TIME_MS = 300_000; // 5 minutes
    
    /**
     * Interface for listening to state changes in the secure game state.
     */
    public interface StateChangeListener {
        void onStateChanged(long newVersion);
    }
    
    /**
     * Creates a new SecureGameState wrapping the given authoritative game.
     * 
     * @param authoritativeGame The game instance to secure and manage
     */
    public SecureGameState(Game authoritativeGame) {
        this.authoritativeGame = authoritativeGame;
        this.validator = new SecurityValidator();
        this.actionValidator = new ActionValidator(validator);
        this.playerCaches = new ConcurrentHashMap<>();
        this.stateVersion = new AtomicLong(0);
        this.listeners = new CopyOnWriteArrayList<>();
        
        // Start background cache cleanup
        this.cacheCleanup = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SecureGameState-CacheCleanup");
            t.setDaemon(true);
            return t;
        });
        
        cacheCleanup.scheduleAtFixedRate(
            this::cleanupStaleViews, 
            CACHE_CLEANUP_INTERVAL_MS, 
            CACHE_CLEANUP_INTERVAL_MS, 
            TimeUnit.MILLISECONDS
        );
    }
    
    /**
     * Gets a filtered game view for the specified player.
     * This view will only contain information that the player is allowed to see
     * according to MTG rules and security policies.
     * 
     * @param playerIndex The index of the player requesting the view
     * @return A filtered GameView appropriate for the player, or null if invalid player index
     */
    public GameView getPlayerView(int playerIndex) {
        if (!isValidPlayerIndex(playerIndex)) {
            return null;
        }
        
        PlayerViewCache cache = playerCaches.computeIfAbsent(playerIndex, 
            k -> new PlayerViewCache(k));
        
        return cache.getView(authoritativeGame, validator, stateVersion.get());
    }
    
    /**
     * Validates whether a player action is legal and authorized.
     * This includes checking turn/priority restrictions, visibility rules,
     * and game state legality.
     * 
     * @param action The player action to validate
     * @param playerIndex The index of the player attempting the action
     * @return true if the action is valid and authorized, false otherwise
     */
    public boolean validatePlayerAction(PlayerAction action, int playerIndex) {
        // Null safety and basic validation
        if (action == null) {
            return false;
        }
        
        if (!isValidPlayerIndex(playerIndex)) {
            return false;
        }
        
        // Cannot act in a finished game
        if (authoritativeGame.isGameOver()) {
            return false;
        }
        
        // Delegate to action validator for specific validation
        return actionValidator.validateAction(action, playerIndex, authoritativeGame);
    }
    
    /**
     * Invalidates the cached view for a specific player.
     * This should be called when the game state changes in a way that affects
     * what the player can see.
     * 
     * @param playerIndex The player whose cache should be invalidated
     */
    public void invalidatePlayerCache(int playerIndex) {
        PlayerViewCache cache = playerCaches.get(playerIndex);
        if (cache != null) {
            cache.invalidate();
        }
    }
    
    /**
     * Invalidates all player caches and increments the state version.
     * This should be called whenever the game state changes.
     */
    public void invalidateAllCaches() {
        stateVersion.incrementAndGet();
        playerCaches.values().forEach(PlayerViewCache::invalidate);
        notifyStateChange();
    }
    
    /**
     * Gets the current state version number.
     * This can be used to track when the game state has changed.
     * 
     * @return The current state version
     */
    public long getStateVersion() {
        return stateVersion.get();
    }
    
    /**
     * Adds a listener for state change notifications.
     * 
     * @param listener The listener to add
     */
    public void addStateChangeListener(StateChangeListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    /**
     * Removes a state change listener.
     * 
     * @param listener The listener to remove
     */
    public void removeStateChangeListener(StateChangeListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Gets the underlying authoritative game instance.
     * WARNING: This should only be used by trusted game engine code.
     * Direct access bypasses all security filtering.
     * 
     * @return The authoritative game instance
     */
    public Game getAuthoritativeGame() {
        return authoritativeGame;
    }
    
    /**
     * Gets the number of active players in the game.
     * 
     * @return The number of players
     */
    public int getPlayerCount() {
        return authoritativeGame.getPlayers().size();
    }
    
    /**
     * Checks if a player index is valid for this game.
     * 
     * @param playerIndex The player index to check
     * @return true if the index is valid, false otherwise
     */
    private boolean isValidPlayerIndex(int playerIndex) {
        return playerIndex >= 0 && playerIndex < authoritativeGame.getPlayers().size();
    }
    
    /**
     * Notifies all listeners that the state has changed.
     */
    private void notifyStateChange() {
        long currentVersion = stateVersion.get();
        for (StateChangeListener listener : listeners) {
            try {
                listener.onStateChanged(currentVersion);
            } catch (Exception e) {
                // Log error but don't let listener exceptions break the game
                System.err.println("Error notifying state change listener: " + e.getMessage());
            }
        }
    }
    
    /**
     * Cleans up stale cached views to prevent memory leaks.
     * Called periodically by background thread.
     */
    private void cleanupStaleViews() {
        long currentTime = System.currentTimeMillis();
        playerCaches.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().getLastAccessTime() > CACHE_EXPIRY_TIME_MS);
    }
    
    /**
     * Shuts down the secure game state manager and cleans up resources.
     * This should be called when the game is finished.
     */
    public void shutdown() {
        if (cacheCleanup != null && !cacheCleanup.isShutdown()) {
            cacheCleanup.shutdown();
            try {
                if (!cacheCleanup.awaitTermination(5, TimeUnit.SECONDS)) {
                    cacheCleanup.shutdownNow();
                }
            } catch (InterruptedException e) {
                cacheCleanup.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        playerCaches.clear();
        listeners.clear();
    }
}