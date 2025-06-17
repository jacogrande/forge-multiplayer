package forge.gamemodes.net.server;

import forge.game.Game;
import forge.game.GameView;
import forge.game.player.actions.PlayerAction;
import forge.game.security.SecureGameState;
import forge.gamemodes.match.GameLobby;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Manages secure game instances for multiplayer sessions.
 * Provides a centralized way to create, manage, and secure games in the network environment.
 * 
 * This manager ensures that:
 * - All multiplayer games have proper security state management
 * - Player actions are validated consistently across all games
 * - Game views are properly filtered for each player
 * - Resources are cleaned up when games end
 */
public class SecureGameManager {
    
    private static final Logger logger = Logger.getLogger(SecureGameManager.class.getName());
    
    private static SecureGameManager instance;
    private final ConcurrentHashMap<Long, SecureGameInstance> activeGames = new ConcurrentHashMap<>();
    private final AtomicLong gameIdCounter = new AtomicLong(0);
    
    /**
     * Represents a secure game instance with its associated metadata.
     */
    public static class SecureGameInstance {
        private final long gameId;
        private final Game game;
        private final SecureGameState secureState;
        private final long createdTime;
        private volatile long lastActivityTime;
        
        public SecureGameInstance(long gameId, Game game) {
            this.gameId = gameId;
            this.game = game;
            this.secureState = new SecureGameState(game);
            this.createdTime = System.currentTimeMillis();
            this.lastActivityTime = this.createdTime;
        }
        
        public long getGameId() { return gameId; }
        public Game getGame() { return game; }
        public SecureGameState getSecureState() { return secureState; }
        public long getCreatedTime() { return createdTime; }
        public long getLastActivityTime() { return lastActivityTime; }
        
        public void updateActivityTime() {
            this.lastActivityTime = System.currentTimeMillis();
        }
        
        public void shutdown() {
            secureState.shutdown();
        }
    }
    
    private SecureGameManager() {
        // Start cleanup thread for stale games
        startCleanupThread();
    }
    
    /**
     * Gets the singleton instance of the SecureGameManager.
     * 
     * @return The SecureGameManager instance
     */
    public static synchronized SecureGameManager getInstance() {
        if (instance == null) {
            instance = new SecureGameManager();
        }
        return instance;
    }
    
    /**
     * Creates a new secure game instance from a lobby configuration.
     * 
     * @param lobby The game lobby with player configuration
     * @return The ID of the created secure game instance
     */
    public long createSecureGame(GameLobby lobby) {
        if (lobby == null) {
            throw new IllegalArgumentException("Game lobby cannot be null");
        }
        
        long gameId = gameIdCounter.incrementAndGet();
        
        try {
            // Create the game from the lobby (this would be the existing game creation logic)
            Game game = createGameFromLobby(lobby);
            
            // Wrap it with security
            SecureGameInstance secureInstance = new SecureGameInstance(gameId, game);
            activeGames.put(gameId, secureInstance);
            
            logger.info(String.format("Created secure game instance %d with %d players", 
                                     gameId, game.getPlayers().size()));
            
            return gameId;
            
        } catch (Exception e) {
            logger.severe(String.format("Failed to create secure game instance %d: %s", 
                                       gameId, e.getMessage()));
            throw new RuntimeException("Failed to create secure game", e);
        }
    }
    
    /**
     * Gets a secure game instance by ID.
     * 
     * @param gameId The ID of the game to retrieve
     * @return The secure game instance, or null if not found
     */
    public SecureGameInstance getSecureGame(long gameId) {
        SecureGameInstance instance = activeGames.get(gameId);
        if (instance != null) {
            instance.updateActivityTime();
        }
        return instance;
    }
    
    /**
     * Validates a player action for a specific game.
     * 
     * @param gameId The ID of the game
     * @param action The player action to validate
     * @param playerIndex The index of the player attempting the action
     * @return true if the action is valid and authorized
     */
    public boolean validatePlayerAction(long gameId, PlayerAction action, int playerIndex) {
        SecureGameInstance instance = getSecureGame(gameId);
        if (instance == null) {
            logger.warning(String.format("Action validation failed: game %d not found", gameId));
            return false;
        }
        
        return instance.getSecureState().validatePlayerAction(action, playerIndex);
    }
    
    /**
     * Gets a secure, filtered game view for a specific player.
     * 
     * @param gameId The ID of the game
     * @param playerIndex The index of the player requesting the view
     * @return A filtered GameView for the player, or null if game not found
     */
    public GameView getPlayerView(long gameId, int playerIndex) {
        SecureGameInstance instance = getSecureGame(gameId);
        if (instance == null) {
            logger.warning(String.format("View request failed: game %d not found", gameId));
            return null;
        }
        
        return instance.getSecureState().getPlayerView(playerIndex);
    }
    
    /**
     * Notifies that a game's state has changed and caches should be invalidated.
     * 
     * @param gameId The ID of the game that changed
     */
    public void notifyGameStateChanged(long gameId) {
        SecureGameInstance instance = getSecureGame(gameId);
        if (instance != null) {
            instance.getSecureState().invalidateAllCaches();
        }
    }
    
    /**
     * Ends and cleans up a secure game instance.
     * 
     * @param gameId The ID of the game to end
     */
    public void endGame(long gameId) {
        SecureGameInstance instance = activeGames.remove(gameId);
        if (instance != null) {
            instance.shutdown();
            logger.info(String.format("Ended and cleaned up secure game instance %d", gameId));
        }
    }
    
    /**
     * Gets the number of currently active secure games.
     * 
     * @return The number of active games
     */
    public int getActiveGameCount() {
        return activeGames.size();
    }
    
    /**
     * Shuts down the secure game manager and cleans up all active games.
     */
    public void shutdown() {
        logger.info(String.format("Shutting down SecureGameManager with %d active games", 
                                 activeGames.size()));
        
        for (SecureGameInstance instance : activeGames.values()) {
            instance.shutdown();
        }
        activeGames.clear();
    }
    
    /**
     * Creates a Game instance from a GameLobby configuration.
     * This is a placeholder for the actual game creation logic.
     * 
     * @param lobby The game lobby configuration
     * @return A new Game instance
     */
    private Game createGameFromLobby(GameLobby lobby) {
        // This would integrate with the existing game creation logic
        // For now, return null to indicate this needs implementation
        
        // The actual implementation would:
        // 1. Extract player configurations from the lobby
        // 2. Create RegisteredPlayer instances  
        // 3. Set up game rules based on lobby settings
        // 4. Create and initialize the Game instance
        // 5. Set up player controllers (human/AI/remote)
        
        throw new UnsupportedOperationException("Game creation from lobby not yet implemented");
    }
    
    /**
     * Starts a background thread to clean up stale game instances.
     */
    private void startCleanupThread() {
        Thread cleanupThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(60_000); // Check every minute
                    cleanupStaleGames();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        cleanupThread.setDaemon(true);
        cleanupThread.setName("SecureGameManager-Cleanup");
        cleanupThread.start();
    }
    
    /**
     * Cleans up games that have been inactive for too long.
     */
    private void cleanupStaleGames() {
        long currentTime = System.currentTimeMillis();
        long maxInactiveTime = 30 * 60 * 1000; // 30 minutes
        
        activeGames.entrySet().removeIf(entry -> {
            SecureGameInstance instance = entry.getValue();
            boolean isStale = (currentTime - instance.getLastActivityTime()) > maxInactiveTime;
            
            if (isStale) {
                logger.info(String.format("Cleaning up stale game instance %d", entry.getKey()));
                instance.shutdown();
            }
            
            return isStale;
        });
    }
}