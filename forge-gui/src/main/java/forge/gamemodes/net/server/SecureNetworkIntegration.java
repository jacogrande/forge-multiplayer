package forge.gamemodes.net.server;

import forge.game.Game;
import forge.game.GameView;
import forge.game.player.PlayerView;
import forge.game.security.SecureGameState;
import forge.interfaces.IGameController;
import forge.gamemodes.net.ProtocolMethod;
import forge.gamemodes.net.event.GuiGameEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Bridge component that integrates the security system with the existing network code.
 * This class provides a non-invasive way to add security validation to network operations
 * while maintaining backward compatibility.
 */
public class SecureNetworkIntegration {
    
    private static final Logger logger = Logger.getLogger(SecureNetworkIntegration.class.getName());
    
    private final Map<Game, SecureGameState> gameSecurityStates = new ConcurrentHashMap<>();
    private boolean securityEnabled = true;
    private volatile long validationTimeNanos = 0;
    
    /**
     * Enable or disable security validation.
     * When disabled, all operations pass through without filtering.
     */
    public void enableSecurity(boolean enabled) {
        this.securityEnabled = enabled;
        logger.info("Network security " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Check if security is currently enabled.
     */
    public boolean isSecurityEnabled() {
        return securityEnabled;
    }
    
    /**
     * Register a game for security management.
     * This should be called when a new multiplayer game starts.
     */
    public void registerGame(Game game) {
        if (!securityEnabled) {
            return;
        }
        
        try {
            SecureGameState secureState = new SecureGameState(game);
            gameSecurityStates.put(game, secureState);
            logger.info("Registered game for security management: " + game.getId());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to register game for security", e);
        }
    }
    
    /**
     * Unregister a game from security management.
     * This should be called when a multiplayer game ends.
     */
    public void unregisterGame(Game game) {
        SecureGameState removed = gameSecurityStates.remove(game);
        if (removed != null) {
            logger.info("Unregistered game from security management: " + game.getId());
        }
    }
    
    /**
     * Get the secure game state for a given game.
     */
    public SecureGameState getSecureGameState(Game game) {
        return gameSecurityStates.get(game);
    }
    
    /**
     * Validate a player action through the security system.
     * Returns true if the action is allowed, false otherwise.
     */
    public boolean validatePlayerAction(Object action, int playerIndex, Game game) {
        if (!securityEnabled) {
            return true; // Allow all actions when security is disabled
        }
        
        long startTime = System.nanoTime();
        
        try {
            SecureGameState secureState = getSecureGameState(game);
            if (secureState == null) {
                logger.warning("No secure game state found for action validation");
                return false;
            }
            
            // For now, we'll do basic validation based on the action type
            // The actual implementation would need proper action type checking
            if (action instanceof forge.game.player.actions.PlayerAction) {
                return secureState.validatePlayerAction((forge.game.player.actions.PlayerAction) action, playerIndex);
            } else {
                // For non-PlayerAction objects, do basic validation
                logger.fine("Non-PlayerAction object validated: " + (action != null ? action.getClass().getSimpleName() : "null"));
                return true; // Allow other types of actions for now
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during action validation", e);
            return false;
        } finally {
            validationTimeNanos = System.nanoTime() - startTime;
        }
    }
    
    /**
     * Filter a game view for a specific player's perspective.
     * Returns a filtered view if security is enabled, otherwise returns the original view.
     */
    public GameView filterGameView(GameView originalView, PlayerView forPlayer, Game game) {
        if (!securityEnabled || originalView == null || forPlayer == null) {
            return originalView;
        }
        
        try {
            SecureGameState secureState = getSecureGameState(game);
            if (secureState == null) {
                logger.warning("No secure game state found for view filtering");
                return originalView;
            }
            
            // Get player-specific view - need to find the player index from the game
            int playerIndex = game.getPlayers().indexOf(game.getPlayer(forPlayer.getId()));
            return secureState.getPlayerView(playerIndex);
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during view filtering", e);
            return originalView;
        }
    }
    
    /**
     * Filter a GUI game event for a specific player.
     * This modifies the event arguments if they contain sensitive information.
     */
    public GuiGameEvent filterGuiGameEvent(GuiGameEvent event, PlayerView forPlayer, Game game) {
        if (!securityEnabled || event == null || forPlayer == null) {
            return event;
        }
        
        try {
            ProtocolMethod method = event.getMethod();
            Object[] args = event.getObjects();
            
            // Filter specific methods that may contain sensitive data
            switch (method) {
                case setGameView:
                    if (args.length > 0 && args[0] instanceof GameView) {
                        GameView filteredView = filterGameView((GameView) args[0], forPlayer, game);
                        args[0] = filteredView;
                    }
                    break;
                    
                case updateCards:
                case updateZones:
                case tempShowZones:
                    // These methods may contain card information that needs filtering
                    // For now, we'll let them pass through
                    // TODO: Implement card-level filtering if needed
                    break;
                    
                default:
                    // Most other methods don't need filtering
                    break;
            }
            
            return new GuiGameEvent(method, args);
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during event filtering", e);
            return event;
        }
    }
    
    /**
     * Validate that a player is allowed to control a specific game controller.
     */
    public boolean validatePlayerController(IGameController controller, PlayerView player, Game game) {
        if (!securityEnabled) {
            return true;
        }
        
        try {
            SecureGameState secureState = getSecureGameState(game);
            if (secureState == null) {
                return false;
            }
            
            // Basic validation - players can always control their own actions
            // TODO: Implement more sophisticated controller validation if needed
            return player != null;
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during controller validation", e);
            return false;
        }
    }
    
    /**
     * Get performance metrics for security validation.
     */
    public SecurityMetrics getMetrics() {
        return new SecurityMetrics(
            securityEnabled,
            gameSecurityStates.size(),
            validationTimeNanos / 1_000_000.0 // Convert to milliseconds
        );
    }
    
    /**
     * Security performance metrics
     */
    public static class SecurityMetrics {
        private final boolean enabled;
        private final int activeGames;
        private final double lastValidationTimeMs;
        
        public SecurityMetrics(boolean enabled, int activeGames, double lastValidationTimeMs) {
            this.enabled = enabled;
            this.activeGames = activeGames;
            this.lastValidationTimeMs = lastValidationTimeMs;
        }
        
        public boolean isEnabled() { return enabled; }
        public int getActiveGames() { return activeGames; }
        public double getLastValidationTimeMs() { return lastValidationTimeMs; }
        
        @Override
        public String toString() {
            return String.format("SecurityMetrics{enabled=%s, activeGames=%d, lastValidationMs=%.2f}", 
                enabled, activeGames, lastValidationTimeMs);
        }
    }
}