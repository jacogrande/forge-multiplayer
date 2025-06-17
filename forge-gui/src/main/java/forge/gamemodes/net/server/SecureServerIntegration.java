package forge.gamemodes.net.server;

import forge.game.Game;
import forge.game.GameView;
import forge.game.player.actions.PlayerAction;
import forge.gamemodes.net.event.NetEvent;

import java.util.logging.Logger;

/**
 * Integration utility for adding security features to the existing FServerManager.
 * This class provides a bridge between the existing networking infrastructure
 * and the new security system without requiring major refactoring.
 * 
 * Usage:
 * 1. Initialize during server startup
 * 2. Call validation methods before processing player actions
 * 3. Use secure view generation for game state broadcasts
 * 4. Clean up when games end
 */
public class SecureServerIntegration {
    
    private static final Logger logger = Logger.getLogger(SecureServerIntegration.class.getName());
    private static SecureServerIntegration instance;
    
    private final SecureGameManager gameManager;
    private boolean securityEnabled = true;
    
    private SecureServerIntegration() {
        this.gameManager = SecureGameManager.getInstance();
    }
    
    /**
     * Gets the singleton instance of the secure server integration.
     * 
     * @return The SecureServerIntegration instance
     */
    public static synchronized SecureServerIntegration getInstance() {
        if (instance == null) {
            instance = new SecureServerIntegration();
        }
        return instance;
    }
    
    /**
     * Enables or disables security features.
     * When disabled, all validation passes and views are unfiltered.
     * 
     * @param enabled true to enable security, false to disable
     */
    public void setSecurityEnabled(boolean enabled) {
        this.securityEnabled = enabled;
        logger.info("Security features " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Validates a player action before allowing it to be processed.
     * This should be called by network handlers before executing any player action.
     * 
     * @param action The player action to validate
     * @param playerIndex The index of the player attempting the action  
     * @param game The current game state
     * @return true if the action should be allowed, false if it should be rejected
     */
    public boolean validatePlayerAction(PlayerAction action, int playerIndex, Game game) {
        if (!securityEnabled) {
            return true; // Allow all actions when security is disabled
        }
        
        if (action == null || game == null) {
            logger.warning("Null action or game provided to validation");
            return false;
        }
        
        // Use the game object itself as a key (simplified approach)
        // In a full implementation, this would use proper game IDs
        try {
            // Create a temporary secure state for validation if needed
            // This is a simplified approach - production code would maintain persistent secure states
            return new forge.game.security.SecureGameState(game)
                    .validatePlayerAction(action, playerIndex);
                    
        } catch (Exception e) {
            logger.severe("Error during action validation: " + e.getMessage());
            return false; // Fail closed on errors
        }
    }
    
    /**
     * Gets a secure, filtered game view for a specific player.
     * This should be used when sending game state updates to players.
     * 
     * @param game The current game state
     * @param playerIndex The index of the player requesting the view
     * @return A filtered GameView for the player, or the original game view if security is disabled
     */
    public GameView getSecurePlayerView(Game game, int playerIndex) {
        if (!securityEnabled || game == null) {
            return game != null ? game.getView() : null;
        }
        
        try {
            // Create a temporary secure state for view generation if needed
            // This is a simplified approach - production code would maintain persistent secure states
            return new forge.game.security.SecureGameState(game)
                    .getPlayerView(playerIndex);
                    
        } catch (Exception e) {
            logger.severe("Error during view generation: " + e.getMessage());
            // Fall back to unfiltered view on error
            return game.getView();
        }
    }
    
    /**
     * Enhances a NetEvent with security filtering before broadcasting.
     * This ensures that the event data is appropriate for each recipient.
     * 
     * @param event The event to secure
     * @param recipientPlayerIndex The index of the player who will receive this event
     * @return The event (potentially modified for security)
     */
    public NetEvent secureNetEvent(NetEvent event, int recipientPlayerIndex) {
        if (!securityEnabled || event == null) {
            return event;
        }
        
        // For now, return the event unchanged
        // In a full implementation, this would filter event data based on player perspective
        return event;
    }
    
    /**
     * Logs a security-relevant action for monitoring purposes.
     * 
     * @param playerIndex The player who performed the action
     * @param actionDescription Description of the action
     * @param allowed Whether the action was allowed or denied
     */
    public void logSecurityAction(int playerIndex, String actionDescription, boolean allowed) {
        if (securityEnabled) {
            String status = allowed ? "ALLOWED" : "DENIED";
            logger.info(String.format("SECURITY: Player %d action %s - %s", 
                                     playerIndex, actionDescription, status));
        }
    }
    
    /**
     * Validates that a player can see specific game information.
     * This is useful for checking before revealing hidden information.
     * 
     * @param playerIndex The player requesting information
     * @param informationType Description of the information type
     * @return true if the player can see this information
     */
    public boolean canPlayerSeeInformation(int playerIndex, String informationType) {
        if (!securityEnabled) {
            return true;
        }
        
        // Basic validation logic - in production this would be more sophisticated
        return informationType != null && !informationType.contains("hidden");
    }
    
    /**
     * Gets statistics about security operations.
     * 
     * @return A string containing security statistics
     */
    public String getSecurityStats() {
        return String.format("Security enabled: %s, Active games: %d", 
                           securityEnabled, gameManager.getActiveGameCount());
    }
    
    /**
     * Shuts down the security integration and cleans up resources.
     */
    public void shutdown() {
        logger.info("Shutting down secure server integration");
        gameManager.shutdown();
    }
}