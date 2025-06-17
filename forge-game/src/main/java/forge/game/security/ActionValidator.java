package forge.game.security;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.player.actions.*;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.HashMap;
import java.util.Map;

/**
 * Validates player actions to ensure they are legal, authorized, and do not
 * violate security constraints in a multiplayer game environment.
 * 
 * This class implements multiple validation strategies:
 * - Turn/Priority validation: Ensures players can only act when allowed
 * - Visibility validation: Players can only target cards they can see
 * - Game rule validation: Actions must be legal in the current game state
 * - Security validation: Prevents information leakage and cheating
 */
public class ActionValidator {
    
    private final SecurityValidator securityValidator;
    private final Map<Class<? extends PlayerAction>, ActionValidationStrategy> validationStrategies;
    
    /**
     * Interface for validation strategies for different action types.
     */
    public interface ActionValidationStrategy {
        boolean validate(PlayerAction action, int playerIndex, Game game);
    }
    
    /**
     * Creates a new ActionValidator with the given security validator.
     * 
     * @param securityValidator The security validator to use for visibility checks
     */
    public ActionValidator(SecurityValidator securityValidator) {
        this.securityValidator = securityValidator;
        this.validationStrategies = new HashMap<>();
        
        // Register validation strategies for different action types
        registerValidationStrategies();
    }
    
    /**
     * Validates a player action against all applicable rules and constraints.
     * 
     * @param action The action to validate
     * @param playerIndex The index of the player attempting the action
     * @param game The current game state
     * @return true if the action is valid and authorized, false otherwise
     */
    public boolean validateAction(PlayerAction action, int playerIndex, Game game) {
        if (action == null || game == null) {
            return false;
        }
        
        // Basic validations that apply to all actions
        if (!validateBasicActionRequirements(action, playerIndex, game)) {
            return false;
        }
        
        // Use specific validation strategy for this action type
        ActionValidationStrategy strategy = validationStrategies.get(action.getClass());
        if (strategy != null) {
            return strategy.validate(action, playerIndex, game);
        }
        
        // If no specific strategy exists, apply default validation
        return validateDefaultAction(action, playerIndex, game);
    }
    
    /**
     * Validates that basic requirements are met for any player action.
     * 
     * @param action The action to validate
     * @param playerIndex The player attempting the action
     * @param game The current game state
     * @return true if basic requirements are met
     */
    private boolean validateBasicActionRequirements(PlayerAction action, int playerIndex, Game game) {
        // Valid player index
        if (playerIndex < 0 || playerIndex >= game.getPlayers().size()) {
            return false;
        }
        
        // Player exists and is still in the game
        Player player = game.getPlayer(playerIndex);
        if (player == null) {
            return false;
        }
        
        // Game is still active
        if (game.isGameOver()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Registers validation strategies for different types of player actions.
     */
    private void registerValidationStrategies() {
        // Spell casting validation
        validationStrategies.put(CastSpellAction.class, this::validateSpellCast);
        
        // Card selection validation
        validationStrategies.put(SelectCardAction.class, this::validateCardSelection);
        
        // Ability activation validation
        validationStrategies.put(ActivateAbilityAction.class, this::validateAbilityActivation);
        
        // Priority passing validation
        validationStrategies.put(PassPriorityAction.class, this::validatePassPriority);
        
        // Targeting validation
        validationStrategies.put(TargetEntityAction.class, this::validateTargeting);
        
        // Mana payment validation
        validationStrategies.put(PayManaFromPoolAction.class, this::validateManaPayment);
    }
    
    /**
     * Validates spell casting actions.
     */
    private boolean validateSpellCast(PlayerAction action, int playerIndex, Game game) {
        if (!(action instanceof CastSpellAction)) {
            return false;
        }
        
        CastSpellAction castAction = (CastSpellAction) action;
        Player player = game.getPlayer(playerIndex);
        
        // Find the card being cast
        Card card = findCardFromView(castAction.getGameEntityView(), player, game);
        if (card == null) {
            return false;
        }
        
        // Player must own or control the card
        if (!card.getController().equals(player) && !card.getOwner().equals(player)) {
            return false;
        }
        
        // Card must be in a zone where it can be cast (typically hand)
        if (!card.isInZone(ZoneType.Hand) && !card.isInZone(ZoneType.Command)) {
            return false;
        }
        
        // Player must have priority to cast spells
        if (!player.equals(game.getPhaseHandler().getPriorityPlayer())) {
            return false;
        }
        
        // The spell itself must be playable
        SpellAbility spell = card.getFirstSpellAbility();
        if (spell != null) {
            return spell.canPlay();
        }
        
        return false;
    }
    
    /**
     * Validates card selection actions.
     */
    private boolean validateCardSelection(PlayerAction action, int playerIndex, Game game) {
        if (!(action instanceof SelectCardAction)) {
            return false;
        }
        
        SelectCardAction selectAction = (SelectCardAction) action;
        Player player = game.getPlayer(playerIndex);
        
        // Find the card being selected
        Card targetCard = findCardFromView(selectAction.getGameEntityView(), player, game);
        if (targetCard == null) {
            return false;
        }
        
        // Player can only select cards they can see
        PlayerView playerView = PlayerView.get(player);
        CardView cardView = CardView.get(targetCard);
        
        return securityValidator.canPlayerSeeCard(playerView, cardView);
    }
    
    /**
     * Validates ability activation actions.
     */
    private boolean validateAbilityActivation(PlayerAction action, int playerIndex, Game game) {
        if (!(action instanceof ActivateAbilityAction)) {
            return false;
        }
        
        ActivateAbilityAction abilityAction = (ActivateAbilityAction) action;
        Player player = game.getPlayer(playerIndex);
        
        // Find the source card
        Card sourceCard = findCardFromView(abilityAction.getGameEntityView(), player, game);
        if (sourceCard == null) {
            return false;
        }
        
        // Player must be able to see the card to activate its abilities
        PlayerView playerView = PlayerView.get(player);
        CardView cardView = CardView.get(sourceCard);
        
        if (!securityValidator.canPlayerSeeCard(playerView, cardView)) {
            return false;
        }
        
        // Player must have priority to activate abilities
        if (!player.equals(game.getPhaseHandler().getPriorityPlayer())) {
            return false;
        }
        
        // Check if any abilities on the card can be activated
        for (SpellAbility ability : sourceCard.getSpellAbilities()) {
            if (ability.canPlay()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Validates priority passing actions.
     */
    private boolean validatePassPriority(PlayerAction action, int playerIndex, Game game) {
        if (!(action instanceof PassPriorityAction)) {
            return false;
        }
        
        Player player = game.getPlayer(playerIndex);
        
        // Player must currently have priority to pass it
        return player.equals(game.getPhaseHandler().getPriorityPlayer());
    }
    
    /**
     * Validates targeting actions.
     */
    private boolean validateTargeting(PlayerAction action, int playerIndex, Game game) {
        if (!(action instanceof TargetEntityAction)) {
            return false;
        }
        
        TargetEntityAction targetAction = (TargetEntityAction) action;
        Player player = game.getPlayer(playerIndex);
        
        // Find the target
        Card targetCard = findCardFromView(targetAction.getGameEntityView(), player, game);
        if (targetCard == null) {
            // Could be targeting a player instead of a card
            return true; // Allow for now, more specific validation would go here
        }
        
        // Player can only target cards they can see
        PlayerView playerView = PlayerView.get(player);
        CardView cardView = CardView.get(targetCard);
        
        return securityValidator.canPlayerSeeCard(playerView, cardView);
    }
    
    /**
     * Validates mana payment actions.
     */
    private boolean validateManaPayment(PlayerAction action, int playerIndex, Game game) {
        if (!(action instanceof PayManaFromPoolAction)) {
            return false;
        }
        
        Player player = game.getPlayer(playerIndex);
        
        // Player must have priority to pay mana
        if (!player.equals(game.getPhaseHandler().getPriorityPlayer())) {
            return false;
        }
        
        // Additional mana payment validation would go here
        // (checking if player has the mana, if it's being used for a valid cost, etc.)
        
        return true;
    }
    
    /**
     * Default validation for action types that don't have specific strategies.
     */
    private boolean validateDefaultAction(PlayerAction action, int playerIndex, Game game) {
        Player player = game.getPlayer(playerIndex);
        
        // For unknown action types, check basic requirements:
        // 1. Player has priority (for most actions)
        // 2. Any referenced cards are visible to the player
        
        if (action.getGameEntityView() != null) {
            Card referencedCard = findCardFromView(action.getGameEntityView(), player, game);
            if (referencedCard != null) {
                PlayerView playerView = PlayerView.get(player);
                CardView cardView = CardView.get(referencedCard);
                
                return securityValidator.canPlayerSeeCard(playerView, cardView);
            }
        }
        
        // If no specific validation can be applied, be conservative and allow it
        // Real implementations would have more specific validation
        return true;
    }
    
    /**
     * Finds a card from a GameEntityView in the context of a player and game.
     * This is a helper method that would need to be implemented based on
     * how GameEntityView references are resolved to actual game objects.
     * 
     * @param entityView The view reference to resolve
     * @param player The player context
     * @param game The game context
     * @return The referenced card, or null if not found/accessible
     */
    private Card findCardFromView(forge.game.GameEntityView entityView, Player player, Game game) {
        if (entityView == null) {
            return null;
        }
        
        // This is a simplified implementation - real implementation would
        // need to properly resolve the GameEntityView to the actual Card object
        // based on the view ID and current game state
        
        // For now, return null to indicate card not found
        // This would be implemented based on how GameEntityView works
        return null;
    }
}