package forge.game.security;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;

/**
 * Utility class for validating and filtering game state information based on player perspectives.
 * Centralizes security logic for multiplayer game state management.
 */
public class SecurityValidator {
    
    /**
     * Determines if a player can see the contents of another player's zone.
     * 
     * @param viewer The player attempting to view the zone
     * @param zoneOwner The player who owns the zone
     * @param zone The type of zone being viewed
     * @return true if the viewer can see the zone contents, false if restricted
     */
    public boolean canPlayerSeeZone(PlayerView viewer, PlayerView zoneOwner, ZoneType zone) {
        if (viewer == null || zoneOwner == null || zone == null) {
            return false;
        }
        
        // Players can always see their own zones
        if (viewer.equals(zoneOwner)) {
            return true;
        }
        
        // Public zones are visible to all players
        if (zone.isKnown()) {
            return true;
        }
        
        // Hidden zones are generally not visible to opponents
        // Exception: Some cards may grant special viewing permissions
        return false;
    }
    
    /**
     * Determines if a player can see a specific card based on current game rules.
     * 
     * @param viewer The player attempting to view the card
     * @param card The card being viewed
     * @return true if the viewer can see the card, false if it should be hidden
     */
    public boolean canPlayerSeeCard(PlayerView viewer, CardView card) {
        if (viewer == null || card == null) {
            return false;
        }
        
        // Use existing CardView security logic
        return card.canBeShownTo(viewer);
    }
    
    /**
     * Counts how many cards in a collection are visible to a specific player.
     * 
     * @param viewer The player whose perspective to use
     * @param cards The collection of cards to count
     * @return The number of cards visible to the viewer
     */
    public int getVisibleCardCount(PlayerView viewer, FCollectionView<CardView> cards) {
        if (viewer == null || cards == null) {
            return 0;
        }
        
        int count = 0;
        for (CardView card : cards) {
            if (canPlayerSeeCard(viewer, card)) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Returns the total count of cards in a collection, regardless of visibility.
     * Used for showing hand size, library size, etc. to opponents.
     * 
     * @param cards The collection of cards to count
     * @return The total number of cards in the collection
     */
    public int getTotalCardCount(FCollectionView<CardView> cards) {
        return cards == null ? 0 : cards.size();
    }
    
    /**
     * Filters a collection of cards to only those visible to a specific player.
     * 
     * @param viewer The player whose perspective to use for filtering
     * @param cards The collection of cards to filter
     * @return A new collection containing only cards visible to the viewer
     */
    public FCollectionView<CardView> filterVisibleCards(PlayerView viewer, FCollectionView<CardView> cards) {
        if (viewer == null || cards == null) {
            return new FCollection<>();
        }
        
        FCollection<CardView> visibleCards = new FCollection<>();
        for (CardView card : cards) {
            if (canPlayerSeeCard(viewer, card)) {
                visibleCards.add(card);
            }
        }
        return visibleCards;
    }
    
    /**
     * Creates an empty collection to represent hidden cards.
     * Used when opponents should know a zone exists but not see its contents.
     * 
     * @return An empty card collection
     */
    public FCollectionView<CardView> createHiddenCardCollection() {
        return new FCollection<>();
    }
    
    /**
     * Determines if a zone should show its card count to opponents.
     * Most hidden zones (hand, library) show count but not contents.
     * 
     * @param zone The zone type to check
     * @return true if opponents should see the card count, false otherwise
     */
    public boolean shouldShowCardCount(ZoneType zone) {
        switch (zone) {
            case Hand:
            case Library:
            case Sideboard:
            case SchemeDeck:
            case PlanarDeck:
            case AttractionDeck:
            case ContraptionDeck:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Validates that a player action doesn't reference hidden information they shouldn't see.
     * 
     * @param actor The player performing the action
     * @param targetCard The card being targeted (if any)
     * @return true if the action is valid from a security perspective
     */
    public boolean validatePlayerAction(PlayerView actor, CardView targetCard) {
        if (actor == null) {
            return false;
        }
        
        // If no target card, action is generally valid
        if (targetCard == null) {
            return true;
        }
        
        // Player can only target cards they can see
        return canPlayerSeeCard(actor, targetCard);
    }
    
    /**
     * Determines the appropriate perspective for one player viewing another player's information.
     * 
     * @param viewer The player doing the viewing
     * @param viewed The player being viewed
     * @return The appropriate perspective for this viewing relationship
     */
    public PlayerPerspective getPerspectiveForViewing(PlayerView viewer, PlayerView viewed) {
        if (viewer == null || viewed == null) {
            return PlayerPerspective.SPECTATOR;
        }
        
        if (viewer.equals(viewed)) {
            return PlayerPerspective.OWNER;
        } else {
            return PlayerPerspective.OPPONENT;
        }
    }
}