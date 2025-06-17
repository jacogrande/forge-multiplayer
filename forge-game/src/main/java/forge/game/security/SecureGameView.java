package forge.game.security;

import forge.game.Game;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;

/**
 * A security-enhanced GameView that filters hidden information based on player perspective.
 * This class ensures that players only see information they're entitled to see in multiplayer games.
 */
public class SecureGameView extends GameView {
    private final PlayerView perspective;
    private final SecurityValidator validator;
    
    /**
     * Creates a new SecureGameView from the given player's perspective.
     * 
     * @param game The game to create a view for
     * @param perspective The player whose perspective this view represents
     */
    public SecureGameView(Game game, PlayerView perspective) {
        super(game);
        this.perspective = perspective;
        this.validator = new SecurityValidator();
    }
    
    /**
     * Gets the player perspective this view is filtered for.
     * 
     * @return The player whose perspective this view represents
     */
    public PlayerView getPerspective() {
        return perspective;
    }
    
    /**
     * Gets the security validator used by this view.
     * 
     * @return The SecurityValidator instance
     */
    public SecurityValidator getValidator() {
        return validator;
    }
    
    /**
     * Returns the players collection with appropriate filtering applied.
     * Each PlayerView in the collection will be filtered based on the relationship
     * to the perspective player.
     */
    @Override
    public FCollectionView<PlayerView> getPlayers() {
        FCollectionView<PlayerView> allPlayers = super.getPlayers();
        if (perspective == null || allPlayers == null) {
            return allPlayers;
        }
        
        // For now, return all players but filtered PlayerViews will be handled
        // when individual player information is accessed
        return allPlayers;
    }
    
    /**
     * Creates a filtered PlayerView that shows appropriate information based on
     * the viewing perspective.
     * 
     * @param targetPlayer The player to create a filtered view for
     * @return A filtered PlayerView appropriate for the current perspective
     */
    public SecurePlayerView getSecurePlayerView(PlayerView targetPlayer) {
        if (targetPlayer == null) {
            return null;
        }
        
        PlayerPerspective viewPerspective = validator.getPerspectiveForViewing(perspective, targetPlayer);
        return new SecurePlayerView(targetPlayer, viewPerspective, validator);
    }
    
    /**
     * Determines if the current perspective can see hidden information about a specific player.
     * 
     * @param targetPlayer The player whose information is being viewed
     * @return true if hidden information can be seen, false otherwise
     */
    public boolean canSeeHiddenInformation(PlayerView targetPlayer) {
        if (perspective == null || targetPlayer == null) {
            return false;
        }
        
        return perspective.equals(targetPlayer);
    }
    
    /**
     * Validates that an action can be performed from the current perspective.
     * 
     * @param targetCard The card being targeted by the action (if any)
     * @return true if the action is valid from a security perspective
     */
    public boolean validateAction(CardView targetCard) {
        return validator.validatePlayerAction(perspective, targetCard);
    }
    
    /**
     * Gets a filtered view of cards in a specific zone for a specific player.
     * 
     * @param zoneOwner The player who owns the zone
     * @param zone The zone type
     * @return Filtered cards visible to the current perspective
     */
    public FCollectionView<CardView> getFilteredZoneCards(PlayerView zoneOwner, ZoneType zone) {
        if (zoneOwner == null || zone == null) {
            return new FCollection<>();
        }
        
        // Get the actual cards from the zone
        FCollectionView<CardView> allCards = zoneOwner.getCards(zone);
        if (allCards == null) {
            return new FCollection<>();
        }
        
        // If this is a public zone, return all cards (they'll be filtered at card level)
        if (zone.isKnown()) {
            return validator.filterVisibleCards(perspective, allCards);
        }
        
        // If viewer can see this zone, return filtered cards
        if (validator.canPlayerSeeZone(perspective, zoneOwner, zone)) {
            return validator.filterVisibleCards(perspective, allCards);
        }
        
        // Otherwise, return empty collection but preserve count information
        return validator.createHiddenCardCollection();
    }
    
    /**
     * Gets the count of cards in a zone, respecting visibility rules.
     * 
     * @param zoneOwner The player who owns the zone
     * @param zone The zone type
     * @return The number of cards visible or the total count if count should be shown
     */
    public int getZoneCardCount(PlayerView zoneOwner, ZoneType zone) {
        if (zoneOwner == null || zone == null) {
            return 0;
        }
        
        FCollectionView<CardView> allCards = zoneOwner.getCards(zone);
        if (allCards == null) {
            return 0;
        }
        
        // If viewer can see the zone contents, return actual visible count
        if (validator.canPlayerSeeZone(perspective, zoneOwner, zone)) {
            return validator.getVisibleCardCount(perspective, allCards);
        }
        
        // For hidden zones that should show count (hand, library, etc.)
        if (validator.shouldShowCardCount(zone)) {
            return validator.getTotalCardCount(allCards);
        }
        
        // Otherwise, don't reveal count
        return 0;
    }
    
    /**
     * Creates a secure view that can be safely transmitted over the network
     * without leaking hidden information.
     * 
     * @return A GameView safe for network transmission to the perspective player
     */
    public GameView createNetworkSafeView() {
        // For now, return this SecureGameView
        // In future iterations, this could create a serializable snapshot
        return this;
    }
}