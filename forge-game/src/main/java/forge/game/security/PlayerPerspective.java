package forge.game.security;

/**
 * Defines different perspectives for viewing game state in multiplayer scenarios.
 * Each perspective determines what information should be visible to a player.
 */
public enum PlayerPerspective {
    /**
     * Player viewing their own information - sees everything they're entitled to see.
     * Includes their own hand, library top when revealed, face-down cards they control, etc.
     */
    OWNER,
    
    /**
     * Player viewing opponent information - sees only public information.
     * Opponent hands show count only, libraries are hidden, face-down cards show backs only.
     */
    OPPONENT,
    
    /**
     * Spectator viewing game - sees only public information visible to all.
     * No access to any hidden zones or face-down information.
     */
    SPECTATOR,
    
    /**
     * Administrative/debug view - sees all information regardless of normal rules.
     * Used for testing, debugging, and administrative purposes only.
     */
    ADMIN;
    
    /**
     * Determines if this perspective allows seeing another player's hidden information.
     * 
     * @return true if this perspective can see hidden information, false otherwise
     */
    public boolean canSeeHiddenInformation() {
        return this == ADMIN;
    }
    
    /**
     * Determines if this perspective allows seeing opponent-specific information.
     * 
     * @return true if can see opponent information, false if restricted to public only
     */
    public boolean canSeeOpponentInformation() {
        return this == ADMIN;
    }
    
    /**
     * Determines if this perspective allows seeing owner-specific information.
     * 
     * @return true if can see owner information, false if restricted
     */
    public boolean canSeeOwnerInformation() {
        return this == OWNER || this == ADMIN;
    }
}