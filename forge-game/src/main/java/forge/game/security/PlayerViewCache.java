package forge.game.security;

import forge.game.Game;
import forge.game.player.Player;
import forge.game.player.PlayerView;

/**
 * Manages cached game views for individual players with version-based invalidation.
 * Provides efficient view caching while ensuring views are updated when the game state changes.
 */
public class PlayerViewCache {
    
    private volatile SecureGameView cachedView;
    private volatile long cacheVersion = -1;
    private volatile long lastAccessTime;
    private final int playerIndex;
    
    /**
     * Creates a new player view cache for the specified player.
     * 
     * @param playerIndex The index of the player this cache serves
     */
    public PlayerViewCache(int playerIndex) {
        this.playerIndex = playerIndex;
        this.lastAccessTime = System.currentTimeMillis();
    }
    
    /**
     * Gets the current view for this player, using cache if valid or creating a new one.
     * 
     * @param game The current game state
     * @param validator The security validator to use for filtering
     * @param currentVersion The current state version
     * @return A secure game view for this player
     */
    public SecureGameView getView(Game game, SecurityValidator validator, long currentVersion) {
        this.lastAccessTime = System.currentTimeMillis();
        
        // Check if we need to update the cached view
        if (cachedView == null || cacheVersion != currentVersion) {
            synchronized (this) {
                // Double-check pattern for thread safety
                if (cachedView == null || cacheVersion != currentVersion) {
                    cachedView = createSecureView(game, validator);
                    cacheVersion = currentVersion;
                }
            }
        }
        
        return cachedView;
    }
    
    /**
     * Invalidates the cached view, forcing it to be regenerated on next access.
     */
    public void invalidate() {
        synchronized (this) {
            cachedView = null;
            cacheVersion = -1;
        }
    }
    
    /**
     * Gets the last time this cache was accessed.
     * Used for cleanup of stale caches.
     * 
     * @return The last access time in milliseconds
     */
    public long getLastAccessTime() {
        return lastAccessTime;
    }
    
    /**
     * Gets the player index this cache serves.
     * 
     * @return The player index
     */
    public int getPlayerIndex() {
        return playerIndex;
    }
    
    /**
     * Gets the current cache version.
     * 
     * @return The version of the currently cached view, or -1 if no cached view
     */
    public long getCacheVersion() {
        return cacheVersion;
    }
    
    /**
     * Checks if the cache is currently valid for the given version.
     * 
     * @param currentVersion The current state version to check against
     * @return true if the cache is valid, false if it needs updating
     */
    public boolean isValidForVersion(long currentVersion) {
        return cachedView != null && cacheVersion == currentVersion;
    }
    
    /**
     * Creates a new secure game view for this player.
     * 
     * @param game The current game state
     * @param validator The security validator to use
     * @return A new secure game view filtered for this player
     */
    private SecureGameView createSecureView(Game game, SecurityValidator validator) {
        // Get the player this view is for
        if (playerIndex < 0 || playerIndex >= game.getPlayers().size()) {
            return null;
        }
        
        Player viewingPlayer = game.getPlayer(playerIndex);
        if (viewingPlayer == null) {
            return null;
        }
        
        // Determine the appropriate perspective for this player
        PlayerPerspective perspective = determinePerspective(viewingPlayer, game);
        
        // Create the secure view with the appropriate filtering
        return new SecureGameView(game, PlayerView.get(viewingPlayer));
    }
    
    /**
     * Determines the appropriate perspective for a player viewing the game.
     * 
     * @param viewingPlayer The player who will see this view
     * @param game The current game state
     * @return The appropriate player perspective
     */
    private PlayerPerspective determinePerspective(Player viewingPlayer, Game game) {
        // If the game is over, everyone gets spectator view (all public info visible)
        if (game.isGameOver()) {
            return PlayerPerspective.SPECTATOR;
        }
        
        // If the player has lost, they get spectator view
        if (viewingPlayer.hasLost()) {
            return PlayerPerspective.SPECTATOR;
        }
        
        // Active players get owner perspective (can see their own hidden info)
        return PlayerPerspective.OWNER;
    }
    
    @Override
    public String toString() {
        return String.format("PlayerViewCache[player=%d, version=%d, lastAccess=%d]", 
                           playerIndex, cacheVersion, lastAccessTime);
    }
}