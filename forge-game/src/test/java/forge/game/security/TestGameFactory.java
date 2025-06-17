package forge.game.security;

import forge.LobbyPlayer;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameEndReason;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.player.IGameEntitiesFactory;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.player.RegisteredPlayer;
import forge.util.Localizer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory class for creating test Game instances with controllable configurations.
 * Provides methods to create games with specific numbers of players and states
 * for testing the SecureGameState system.
 */
public class TestGameFactory {
    
    private static boolean localizerInitialized = false;
    
    /**
     * Initializes the Localizer for test environments.
     * This method ensures that GameType enum initialization doesn't fail
     * due to missing resource bundles.
     */
    private static synchronized void initializeLocalizerForTests() {
        if (localizerInitialized) {
            return;
        }
        
        try {
            Localizer localizer = Localizer.getInstance();
            
            // Try to find the language files in various possible locations
            String[] possiblePaths = {
                "../forge-gui/res/languages/",
                "../../forge-gui/res/languages/",
                "../../../forge-gui/res/languages/",
                "forge-gui/res/languages/",
                "src/main/resources/languages/"
            };
            
            String languagesDir = null;
            for (String path : possiblePaths) {
                File dir = new File(path);
                if (dir.exists() && dir.isDirectory()) {
                    File enUsFile = new File(dir, "en-US.properties");
                    if (enUsFile.exists()) {
                        languagesDir = path;
                        break;
                    }
                }
            }
            
            if (languagesDir != null) {
                localizer.initialize("en-US", languagesDir);
                System.out.println("Localizer initialized successfully for tests with directory: " + languagesDir);
            } else {
                System.out.println("Warning: Could not find language resources, using fallback initialization");
                // Try to initialize with empty directory - this may work in some cases
                localizer.initialize("en-US", "");
            }
            
            localizerInitialized = true;
        } catch (Exception e) {
            System.out.println("Localizer initialization failed (this may be expected in test environment): " + e.getMessage());
            localizerInitialized = true; // Mark as initialized to avoid repeated attempts
        }
    }
    
    /**
     * Creates a test game with the specified number of players.
     * 
     * @param playerCount The number of players to include in the game
     * @return A configured Game instance for testing
     */
    public Game createTestGame(int playerCount) {
        // Initialize localizer before any GameType access
        initializeLocalizerForTests();
        
        if (playerCount < 1 || playerCount > 8) {
            throw new IllegalArgumentException("Player count must be between 1 and 8");
        }
        
        // Create game rules
        GameRules rules = new GameRules(GameType.Constructed);
        rules.setGamesPerMatch(1);
        
        // Create match
        List<RegisteredPlayer> registeredPlayers = createRegisteredPlayers(playerCount);
        Match match = new Match(rules, registeredPlayers, "Test Match");
        
        // Create and start the game
        Game game = new Game(registeredPlayers, rules, match);
        
        // Initialize basic game state
        initializeGameState(game);
        
        // NOTE: We skip starting the game to avoid controller dependencies in tests
        // This means phases/priority won't be initialized, but SecureGameState should still work
        // match.startGame(game);
        
        return game;
    }
    
    /**
     * Creates a test game with specific player configurations.
     * 
     * @param playerNames Array of player names
     * @return A configured Game instance
     */
    public Game createTestGameWithNames(String[] playerNames) {
        // Initialize localizer before any GameType access
        initializeLocalizerForTests();
        
        if (playerNames == null || playerNames.length == 0) {
            throw new IllegalArgumentException("Must provide at least one player name");
        }
        
        GameRules rules = new GameRules(GameType.Constructed);
        rules.setGamesPerMatch(1);
        
        List<RegisteredPlayer> registeredPlayers = new ArrayList<>();
        for (int i = 0; i < playerNames.length; i++) {
            LobbyPlayer lobbyPlayer = new TestLobbyPlayer(playerNames[i]);
            Deck deck = createTestDeck("Test Deck " + (i + 1));
            RegisteredPlayer regPlayer = new RegisteredPlayer(deck);
            regPlayer.setPlayer(lobbyPlayer);
            registeredPlayers.add(regPlayer);
        }
        
        Match match = new Match(rules, registeredPlayers, "Test Match");
        Game game = new Game(registeredPlayers, rules, match);
        
        initializeGameState(game);
        
        // NOTE: We skip starting the game to avoid controller dependencies in tests
        // This means phases/priority won't be initialized, but SecureGameState should still work
        // match.startGame(game);
        
        return game;
    }
    
    /**
     * Sets the game over state for testing.
     * 
     * @param game The game to modify
     * @param gameOver Whether the game should be marked as over
     */
    public void setGameOver(Game game, boolean gameOver) {
        if (game == null) {
            return;
        }
        
        if (gameOver) {
            // Use Game's built-in setGameOver method with a test reason
            // This properly sets the game state and triggers all necessary cleanup
            game.setGameOver(GameEndReason.Draw);
        }
        // Note: There's no way to "un-end" a game once it's over, which is correct
        // for testing purposes - if you need a non-ended game, create a new one
    }
    
    /**
     * Creates a list of registered players for testing.
     * 
     * @param count The number of players to create
     * @return List of registered players
     */
    private List<RegisteredPlayer> createRegisteredPlayers(int count) {
        List<RegisteredPlayer> players = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            // Create lobby player
            LobbyPlayer lobbyPlayer = new TestLobbyPlayer("Test Player " + (i + 1));
            
            // Create test deck
            Deck deck = createTestDeck("Test Deck " + (i + 1));
            
            // Create registered player
            RegisteredPlayer regPlayer = new RegisteredPlayer(deck);
            regPlayer.setPlayer(lobbyPlayer);
            
            players.add(regPlayer);
        }
        
        return players;
    }
    
    /**
     * Creates a simple test deck for players.
     * 
     * @param deckName The name for the deck
     * @return A basic test deck
     */
    private Deck createTestDeck(String deckName) {
        Deck deck = new Deck(deckName);
        
        // Add some basic cards to make a valid deck
        // Note: This is simplified - real deck creation would need
        // actual card data and proper deck construction rules
        
        try {
            // Try to create basic lands - this might fail if card database
            // is not available in test environment
            for (int i = 0; i < 24; i++) {
                // Add basic lands - this is pseudo-code as actual
                // implementation would depend on available card database
                // deck.getMain().add("Plains", 1);
            }
            
            // Add some spells
            for (int i = 0; i < 36; i++) {
                // Add basic spells - pseudo-code
                // deck.getMain().add("Lightning Bolt", 1);
            }
        } catch (Exception e) {
            // If we can't add real cards, create empty deck
            // Tests should still work with empty decks for basic functionality
        }
        
        return deck;
    }
    
    /**
     * Initializes basic game state for testing.
     * 
     * @param game The game to initialize
     */
    private void initializeGameState(Game game) {
        if (game == null) {
            return;
        }
        
        try {
            // Initialize players with basic game state
            for (Player player : game.getPlayers()) {
                // Set starting life total
                player.setLife(20, null);
                
                // Initialize zones
                // Note: This is simplified initialization
                // Real game initialization would involve shuffling libraries,
                // drawing opening hands, etc.
            }
            
            // Start the game phases
            // Note: This might need to be done differently depending on
            // how Game initialization works
            
        } catch (Exception e) {
            // If initialization fails, at least we have a basic Game object
            // Some tests may still work even without full initialization
        }
    }
    
    /**
     * Creates a game in a specific state for testing particular scenarios.
     * 
     * @param playerCount Number of players
     * @param gameState The desired game state
     * @return Configured game instance
     */
    public Game createGameInState(int playerCount, TestGameState gameState) {
        // Initialize localizer before any GameType access
        initializeLocalizerForTests();
        
        Game game = createTestGame(playerCount);
        
        switch (gameState) {
            case GAME_START:
                // Game is already in start state
                break;
                
            case MID_GAME:
                // Advance game to middle state
                advanceGameToMidGame(game);
                break;
                
            case GAME_OVER:
                setGameOver(game, true);
                break;
                
            default:
                // Default to start state
                break;
        }
        
        return game;
    }
    
    /**
     * Advances a game to a mid-game state for testing.
     * 
     * @param game The game to advance
     */
    private void advanceGameToMidGame(Game game) {
        // This would implement advancing the game through several turns
        // to create a more complex game state for testing
        
        // For now, this is a placeholder
        // Real implementation would simulate turns, play cards, etc.
    }
    
    /**
     * Enumeration of test game states.
     */
    public enum TestGameState {
        GAME_START,
        MID_GAME,
        GAME_OVER
    }
    
    /**
     * Simple test implementation of LobbyPlayer that implements IGameEntitiesFactory.
     * This is used to avoid dependency on forge-ai module for testing.
     */
    private static class TestLobbyPlayer extends LobbyPlayer implements IGameEntitiesFactory {
        
        public TestLobbyPlayer(String name) {
            super(name);
        }
        
        @Override
        public void hear(LobbyPlayer player, String message) {
            // Test implementation - does nothing
        }
        
        @Override
        public PlayerController createMindSlaveController(Player master, Player slave) {
            // Return the slave's existing controller for simplicity
            return slave.getController();
        }
        
        @Override
        public Player createIngamePlayer(Game game, int id) {
            // Create a basic player instance without controller
            // Since we're not starting the game, controller operations should be avoided
            return new Player(getName(), game, id);
        }
    }
}