package forge.gamemodes.net.server;

import forge.LobbyPlayer;
import forge.ai.GameState;
import forge.deck.CardPool;
import forge.game.GameEntityView;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.IHasIcon;
import forge.game.player.PlayerView;
import forge.game.security.SecureGameState;
import forge.game.spellability.SpellAbilityView;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.AbstractGuiGame;
import forge.gamemodes.net.GameProtocolSender;
import forge.gamemodes.net.ProtocolMethod;
import forge.gamemodes.net.event.GuiGameEvent;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.player.PlayerZoneUpdate;
import forge.player.PlayerZoneUpdates;
import forge.trackable.TrackableCollection;
import forge.util.ITriggerEvent;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Secure decorator for NetGuiGame that adds security filtering to all network communications.
 * This class wraps the standard NetGuiGame and applies player-specific filtering to all
 * game data sent over the network.
 */
public class SecureNetGuiGame extends AbstractGuiGame {
    
    private static final Logger logger = Logger.getLogger(SecureNetGuiGame.class.getName());
    
    private final GameProtocolSender sender;
    private final IToClient client;
    private final SecureGameState secureGameState;
    private final PlayerView playerPerspective;
    private final SecureNetworkIntegration integration;
    
    /**
     * Create a secure network GUI for a specific player.
     * 
     * @param client The network client connection
     * @param secureGameState The secure game state manager
     * @param playerPerspective The player this GUI represents
     */
    public SecureNetGuiGame(IToClient client, SecureGameState secureGameState, PlayerView playerPerspective) {
        this.client = client;
        this.sender = new GameProtocolSender(client);
        this.secureGameState = secureGameState;
        this.playerPerspective = playerPerspective;
        this.integration = new SecureNetworkIntegration();
    }
    
    /**
     * Send a method call with security filtering applied.
     */
    private void securelyend(ProtocolMethod method, Object... args) {
        try {
            // Create the event
            GuiGameEvent event = new GuiGameEvent(method, args);
            
            // Apply security filtering
            GuiGameEvent filteredEvent = integration.filterGuiGameEvent(event, playerPerspective, 
                secureGameState.getAuthoritativeGame());
            
            // Send the filtered event
            client.send(filteredEvent);
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error in secure send for method: " + method, e);
            // Fallback to original sender for critical operations
            sender.send(method, args);
        }
    }
    
    /**
     * Send a method call and wait for response with security filtering.
     */
    @SuppressWarnings("unchecked")
    private <T> T sendAndWaitSecurely(ProtocolMethod method, Object... args) {
        try {
            // Create the event
            GuiGameEvent event = new GuiGameEvent(method, args);
            
            // Apply security filtering
            GuiGameEvent filteredEvent = integration.filterGuiGameEvent(event, playerPerspective, 
                secureGameState.getAuthoritativeGame());
            
            // Send and wait for response
            return (T) client.sendAndWait(filteredEvent);
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error in secure sendAndWait for method: " + method, e);
            // Fallback to original sender
            return sender.sendAndWait(method, args);
        }
    }
    
    /**
     * Get a player-specific filtered game view.
     */
    private GameView getFilteredGameView() {
        if (secureGameState == null || playerPerspective == null) {
            return getGameView();
        }
        
        try {
            // Get player index from the game's player list
            int playerIndex = secureGameState.getAuthoritativeGame().getPlayers().indexOf(
                secureGameState.getAuthoritativeGame().getPlayer(playerPerspective.getId()));
            return secureGameState.getPlayerView(playerIndex);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error getting filtered game view", e);
            return getGameView();
        }
    }
    
    // Override all NetGuiGame methods to add security filtering
    
    public void updateGameView() {
        securelyend(ProtocolMethod.setGameView, getFilteredGameView());
    }
    
    @Override
    public void setGameView(GameView gameView) {
        super.setGameView(gameView);
        updateGameView();
    }
    
    @Override
    public void openView(TrackableCollection<PlayerView> myPlayers) {
        // Filter the player views based on what this player should see
        securelyend(ProtocolMethod.openView, myPlayers);
        updateGameView();
    }
    
    @Override
    public void afterGameEnd() {
        securelyend(ProtocolMethod.afterGameEnd);
    }
    
    @Override
    public void showCombat() {
        securelyend(ProtocolMethod.showCombat);
    }
    
    @Override
    public void showPromptMessage(PlayerView playerView, String message) {
        updateGameView();
        securelyend(ProtocolMethod.showPromptMessage, playerView, message);
    }
    
    @Override
    public void showCardPromptMessage(PlayerView playerView, String message, CardView card) {
        updateGameView();
        securelyend(ProtocolMethod.showCardPromptMessage, playerView, message, card);
    }
    
    @Override
    public void updateButtons(PlayerView owner, String label1, String label2, boolean enable1, boolean enable2, boolean focus1) {
        securelyend(ProtocolMethod.updateButtons, owner, label1, label2, enable1, enable2, focus1);
    }
    
    @Override
    public void flashIncorrectAction() {
        securelyend(ProtocolMethod.flashIncorrectAction);
    }
    
    @Override
    public void alertUser() {
        securelyend(ProtocolMethod.alertUser);
    }
    
    @Override
    public void updatePhase(boolean saveState) {
        updateGameView();
        securelyend(ProtocolMethod.updatePhase, saveState);
    }
    
    @Override
    public void updateTurn(PlayerView player) {
        updateGameView();
        securelyend(ProtocolMethod.updateTurn, player);
    }
    
    @Override
    public void updatePlayerControl() {
        updateGameView();
        securelyend(ProtocolMethod.updatePlayerControl);
    }
    
    @Override
    public void enableOverlay() {
        securelyend(ProtocolMethod.enableOverlay);
    }
    
    @Override
    public void disableOverlay() {
        securelyend(ProtocolMethod.disableOverlay);
    }
    
    @Override
    public void finishGame() {
        securelyend(ProtocolMethod.finishGame);
    }
    
    @Override
    public void showManaPool(PlayerView player) {
        securelyend(ProtocolMethod.showManaPool, player);
    }
    
    @Override
    public void hideManaPool(PlayerView player) {
        securelyend(ProtocolMethod.hideManaPool, player);
    }
    
    @Override
    public void updateStack() {
        updateGameView();
        securelyend(ProtocolMethod.updateStack);
    }
    
    @Override
    public void updateZones(Iterable<PlayerZoneUpdate> zonesToUpdate) {
        updateGameView();
        securelyend(ProtocolMethod.updateZones, zonesToUpdate);
    }
    
    @Override
    public Iterable<PlayerZoneUpdate> tempShowZones(PlayerView controller, Iterable<PlayerZoneUpdate> zonesToUpdate) {
        updateGameView();
        return sendAndWaitSecurely(ProtocolMethod.tempShowZones, controller, zonesToUpdate);
    }
    
    @Override
    public void hideZones(PlayerView controller, Iterable<PlayerZoneUpdate> zonesToUpdate) {
        updateGameView();
        securelyend(ProtocolMethod.hideZones, controller, zonesToUpdate);
    }
    
    @Override
    public void updateCards(Iterable<CardView> cards) {
        updateGameView();
        securelyend(ProtocolMethod.updateCards, cards);
    }
    
    @Override
    public void updateManaPool(Iterable<PlayerView> manaPoolUpdate) {
        updateGameView();
        securelyend(ProtocolMethod.updateManaPool, manaPoolUpdate);
    }
    
    @Override
    public void updateLives(Iterable<PlayerView> livesUpdate) {
        updateGameView();
        securelyend(ProtocolMethod.updateLives, livesUpdate);
    }
    
    @Override
    public void updateShards(Iterable<PlayerView> shardsUpdate) {
        // Mobile adventure local game only - no network filtering needed
    }
    
    @Override
    public void setPanelSelection(CardView hostCard) {
        updateGameView();
        securelyend(ProtocolMethod.setPanelSelection, hostCard);
    }
    
    @Override
    public void refreshField() {
        updateGameView();
        securelyend(ProtocolMethod.refreshField);
    }
    
    @Override
    public GameState getGamestate() {
        return null; // Not applicable for network games
    }
    
    @Override
    public SpellAbilityView getAbilityToPlay(CardView hostCard, List<SpellAbilityView> abilities, ITriggerEvent triggerEvent) {
        return sendAndWaitSecurely(ProtocolMethod.getAbilityToPlay, hostCard, abilities, null);
    }
    
    @Override
    public Map<CardView, Integer> assignCombatDamage(CardView attacker, List<CardView> blockers, int damage, GameEntityView defender, boolean overrideOrder, boolean maySkip) {
        return sendAndWaitSecurely(ProtocolMethod.assignCombatDamage, attacker, blockers, damage, defender, overrideOrder, maySkip);
    }
    
    @Override
    public Map<Object, Integer> assignGenericAmount(CardView effectSource, Map<Object, Integer> targets, int amount, boolean atLeastOne, String amountLabel) {
        return sendAndWaitSecurely(ProtocolMethod.assignGenericAmount, effectSource, targets, amount, atLeastOne, amountLabel);
    }
    
    @Override
    public void message(String message, String title) {
        securelyend(ProtocolMethod.message, message, title);
    }
    
    @Override
    public void showErrorDialog(String message, String title) {
        securelyend(ProtocolMethod.showErrorDialog, message, title);
    }
    
    @Override
    public boolean showConfirmDialog(String message, String title, String yesButtonText, String noButtonText, boolean defaultYes) {
        return sendAndWaitSecurely(ProtocolMethod.showConfirmDialog, message, title, yesButtonText, noButtonText, defaultYes);
    }
    
    @Override
    public int showOptionDialog(String message, String title, FSkinProp icon, List<String> options, int defaultOption) {
        return sendAndWaitSecurely(ProtocolMethod.showOptionDialog, message, title, icon, options, defaultOption);
    }
    
    @Override
    public String showInputDialog(String message, String title, FSkinProp icon, String initialInput, List<String> inputOptions, boolean isNumeric) {
        return sendAndWaitSecurely(ProtocolMethod.showInputDialog, message, title, icon, initialInput, inputOptions, isNumeric);
    }
    
    @Override
    public boolean confirm(CardView c, String question, boolean defaultIsYes, List<String> options) {
        return sendAndWaitSecurely(ProtocolMethod.confirm, c, question, defaultIsYes, options);
    }
    
    @Override
    public <T> List<T> getChoices(String message, int min, int max, List<T> choices, List<T> selected, Function<T, String> display) {
        return sendAndWaitSecurely(ProtocolMethod.getChoices, message, min, max, choices, selected, display);
    }
    
    @Override
    public <T> List<T> order(String title, String top, int remainingObjectsMin, int remainingObjectsMax, List<T> sourceChoices, List<T> destChoices, CardView referenceCard, boolean sideboardingMode) {
        return sendAndWaitSecurely(ProtocolMethod.order, title, top, remainingObjectsMin, remainingObjectsMax, sourceChoices, destChoices, referenceCard, sideboardingMode);
    }
    
    @Override
    public List<PaperCard> sideboard(CardPool sideboard, CardPool main, String message) {
        return sendAndWaitSecurely(ProtocolMethod.sideboard, sideboard, main, message);
    }
    
    @Override
    public GameEntityView chooseSingleEntityForEffect(String title, List<? extends GameEntityView> optionList, DelayedReveal delayedReveal, boolean isOptional) {
        return sendAndWaitSecurely(ProtocolMethod.chooseSingleEntityForEffect, title, optionList, delayedReveal, isOptional);
    }
    
    @Override
    public List<GameEntityView> chooseEntitiesForEffect(String title, List<? extends GameEntityView> optionList, int min, int max, DelayedReveal delayedReveal) {
        return sendAndWaitSecurely(ProtocolMethod.chooseEntitiesForEffect, title, optionList, min, max, delayedReveal);
    }
    
    @Override
    public List<CardView> manipulateCardList(String title, Iterable<CardView> cards, Iterable<CardView> manipulable, boolean toTop, boolean toBottom, boolean toAnywhere) {
        return sendAndWaitSecurely(ProtocolMethod.manipulateCardList, title, cards, manipulable, toTop, toBottom, toAnywhere);
    }
    
    @Override
    public void setCard(CardView card) {
        updateGameView();
        securelyend(ProtocolMethod.setCard, card);
    }
    
    @Override
    public void setSelectables(Iterable<CardView> cards) {
        updateGameView();
        securelyend(ProtocolMethod.setSelectables, cards);
    }
    
    @Override
    public void clearSelectables() {
        updateGameView();
        securelyend(ProtocolMethod.clearSelectables);
    }
    
    @Override
    public void setPlayerAvatar(LobbyPlayer player, IHasIcon ihi) {
        // Avatar setting doesn't need security filtering
    }
    
    @Override
    public PlayerZoneUpdates openZones(PlayerView controller, Collection<ZoneType> zones, Map<PlayerView, Object> players, boolean backupLastZones) {
        updateGameView();
        return sendAndWaitSecurely(ProtocolMethod.openZones, controller, zones, players, backupLastZones);
    }
    
    @Override
    public void restoreOldZones(PlayerView playerView, PlayerZoneUpdates playerZoneUpdates) {
        securelyend(ProtocolMethod.restoreOldZones, playerView, playerZoneUpdates);
    }
    
    @Override
    public boolean isUiSetToSkipPhase(PlayerView playerTurn, PhaseType phase) {
        return sendAndWaitSecurely(ProtocolMethod.isUiSetToSkipPhase, playerTurn, phase);
    }
    
    @Override
    protected void updateCurrentPlayer(PlayerView player) {
        // Current player updates are handled through updateGameView
    }
    
    /**
     * Get the player perspective for this secure GUI.
     */
    public PlayerView getPlayerPerspective() {
        return playerPerspective;
    }
    
    /**
     * Get the secure game state manager.
     */
    public SecureGameState getSecureGameState() {
        return secureGameState;
    }
}