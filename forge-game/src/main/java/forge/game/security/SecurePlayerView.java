package forge.game.security;

import forge.game.card.CardView;
import forge.game.card.CounterEnumType;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

/**
 * A security-enhanced wrapper around PlayerView that filters information based on viewing perspective.
 * This class ensures that hidden information is properly concealed from opponents while maintaining
 * full visibility for the player themselves.
 */
public class SecurePlayerView {
    private final PlayerView wrappedPlayer;
    private final PlayerPerspective perspective;
    private final SecurityValidator validator;
    
    /**
     * Creates a new SecurePlayerView wrapper.
     * 
     * @param player The PlayerView to wrap and filter
     * @param perspective The perspective from which this player is being viewed
     * @param validator The security validator to use for filtering decisions
     */
    public SecurePlayerView(PlayerView player, PlayerPerspective perspective, SecurityValidator validator) {
        this.wrappedPlayer = player;
        this.perspective = perspective;
        this.validator = validator;
    }
    
    /**
     * Gets the underlying PlayerView (for cases where full access is needed).
     * 
     * @return The wrapped PlayerView
     */
    public PlayerView getPlayerView() {
        return wrappedPlayer;
    }
    
    /**
     * Gets the perspective from which this player is being viewed.
     * 
     * @return The viewing perspective
     */
    public PlayerPerspective getPerspective() {
        return perspective;
    }
    
    // ========== Zone Access Methods (Filtered) ==========
    
    /**
     * Gets the player's hand, filtered based on viewing perspective.
     * - OWNER: Full hand contents
     * - OPPONENT/SPECTATOR: Empty collection (but count available via getHandSize)
     * - ADMIN: Full hand contents
     * 
     * @return Filtered hand contents
     */
    public FCollectionView<CardView> getHand() {
        if (perspective.canSeeOwnerInformation()) {
            return validator.filterVisibleCards(wrappedPlayer, wrappedPlayer.getHand());
        } else {
            // Opponents see empty hand but can get count via getHandSize()
            return validator.createHiddenCardCollection();
        }
    }
    
    /**
     * Gets the player's hand size (always visible to opponents).
     * 
     * @return The number of cards in hand
     */
    public int getHandSize() {
        return wrappedPlayer.getHandSize();
    }
    
    /**
     * Gets the player's library, filtered based on viewing perspective.
     * - OWNER: Visible cards only (revealed cards, scried cards, etc.)
     * - OPPONENT/SPECTATOR: Empty collection
     * - ADMIN: All cards
     * 
     * @return Filtered library contents
     */
    public FCollectionView<CardView> getLibrary() {
        if (perspective == PlayerPerspective.ADMIN) {
            return wrappedPlayer.getLibrary();
        } else if (perspective.canSeeOwnerInformation()) {
            // Owner can see revealed library cards
            return validator.filterVisibleCards(wrappedPlayer, wrappedPlayer.getLibrary());
        } else {
            // Opponents cannot see library contents
            return validator.createHiddenCardCollection();
        }
    }
    
    /**
     * Gets the player's library size (visible to all players).
     * 
     * @return The number of cards in library
     */
    public int getLibrarySize() {
        return wrappedPlayer.getZoneSize(ZoneType.Library);
    }
    
    /**
     * Gets the player's battlefield, filtered for card visibility.
     * Battlefield is public, but individual cards may have face-down restrictions.
     * 
     * @return Filtered battlefield contents
     */
    public FCollectionView<CardView> getBattlefield() {
        return validator.filterVisibleCards(wrappedPlayer, wrappedPlayer.getBattlefield());
    }
    
    /**
     * Gets the player's graveyard (public zone, all cards visible).
     * 
     * @return Graveyard contents
     */
    public FCollectionView<CardView> getGraveyard() {
        return wrappedPlayer.getGraveyard();
    }
    
    /**
     * Gets the player's exile zone, filtered for face-down cards.
     * 
     * @return Filtered exile contents
     */
    public FCollectionView<CardView> getExile() {
        return validator.filterVisibleCards(wrappedPlayer, wrappedPlayer.getExile());
    }
    
    /**
     * Gets the player's command zone (public zone, all cards visible).
     * 
     * @return Command zone contents
     */
    public FCollectionView<CardView> getCommand() {
        return wrappedPlayer.getCommand();
    }
    
    /**
     * Gets cards from a specific zone, applying appropriate filtering.
     * 
     * @param zone The zone to get cards from
     * @return Filtered cards from the specified zone
     */
    public FCollectionView<CardView> getCards(ZoneType zone) {
        FCollectionView<CardView> allCards = wrappedPlayer.getCards(zone);
        if (allCards == null) {
            return validator.createHiddenCardCollection();
        }
        
        // Apply zone-based filtering
        if (zone.isHidden() && !perspective.canSeeOwnerInformation()) {
            // Hidden zone, opponent perspective
            return validator.createHiddenCardCollection();
        } else {
            // Public zone or owner perspective - filter individual cards
            return validator.filterVisibleCards(wrappedPlayer, allCards);
        }
    }
    
    /**
     * Gets the size of a specific zone.
     * 
     * @param zone The zone to get the size for
     * @return The zone size (respecting visibility rules)
     */
    public int getZoneSize(ZoneType zone) {
        if (zone.isHidden() && !perspective.canSeeOwnerInformation()) {
            // For hidden zones, show count if appropriate
            if (validator.shouldShowCardCount(zone)) {
                return wrappedPlayer.getZoneSize(zone);
            } else {
                return 0; // Don't reveal count for some hidden zones
            }
        } else {
            return wrappedPlayer.getZoneSize(zone);
        }
    }
    
    // ========== Player Information Methods (Pass-through) ==========
    
    /**
     * Gets the player's name (always visible).
     * 
     * @return The player's lobby name
     */
    public String getLobbyPlayerName() {
        return wrappedPlayer.getLobbyPlayerName();
    }
    
    /**
     * Gets the player's life total (always visible).
     * 
     * @return Current life total
     */
    public int getLife() {
        return wrappedPlayer.getLife();
    }
    
    /**
     * Gets the player's poison counters (always visible).
     * 
     * @return Current poison counter count
     */
    public int getPoisonCounters() {
        return wrappedPlayer.getCounters(CounterEnumType.POISON);
    }
    
    /**
     * Checks if this player is an AI (always visible).
     * 
     * @return true if AI player, false if human
     */
    public boolean isAI() {
        return wrappedPlayer.isAI();
    }
    
    /**
     * Gets the player's maximum hand size (always visible).
     * 
     * @return Maximum hand size
     */
    public int getMaxHandSize() {
        return wrappedPlayer.getMaxHandSize();
    }
    
    /**
     * Checks if player has unlimited hand size (always visible).
     * 
     * @return true if unlimited hand size, false otherwise
     */
    public boolean hasUnlimitedHandSize() {
        return wrappedPlayer.hasUnlimitedHandSize();
    }
    
    /**
     * Checks if this player has priority (always visible).
     * 
     * @return true if player has priority, false otherwise
     */
    public boolean hasPriority() {
        return wrappedPlayer.getHasPriority();
    }
    
    /**
     * Checks if this player is an opponent of another player.
     * 
     * @param other The other player to check against
     * @return true if opponents, false otherwise
     */
    public boolean isOpponentOf(PlayerView other) {
        return wrappedPlayer.isOpponentOf(other);
    }
    
    /**
     * Gets opponents of this player (always visible).
     * 
     * @return Collection of opponent PlayerViews
     */
    public FCollectionView<PlayerView> getOpponents() {
        return wrappedPlayer.getOpponents();
    }
    
    // ========== Equality and Identity ==========
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        
        if (obj instanceof SecurePlayerView) {
            SecurePlayerView other = (SecurePlayerView) obj;
            if (wrappedPlayer == null && other.wrappedPlayer == null) {
                return true;
            }
            if (wrappedPlayer == null || other.wrappedPlayer == null) {
                return false;
            }
            return wrappedPlayer.equals(other.wrappedPlayer);
        } else if (obj instanceof PlayerView) {
            if (wrappedPlayer == null) {
                return false;
            }
            return wrappedPlayer.equals(obj);
        }
        
        return false;
    }
    
    @Override
    public int hashCode() {
        return wrappedPlayer == null ? 0 : wrappedPlayer.hashCode();
    }
    
    @Override
    public String toString() {
        String playerName = (wrappedPlayer != null) ? wrappedPlayer.getLobbyPlayerName() : "null";
        return String.format("SecurePlayerView[%s, perspective=%s]", playerName, perspective);
    }
}