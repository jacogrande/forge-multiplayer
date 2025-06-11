# Forge Game Engine

The `forge-game` module is the core Magic: The Gathering rules engine that implements the complete MTG comprehensive rules. It provides a sophisticated, event-driven game engine capable of handling all official MTG mechanics, formats, and complex card interactions with full rules enforcement.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Game State Management](#game-state-management)
- [Card and Ability System](#card-and-ability-system)
- [Rules Engine Implementation](#rules-engine-implementation)
- [Event-Driven Architecture](#event-driven-architecture)
- [Zone Management](#zone-management)
- [Combat System](#combat-system)
- [Mana and Cost System](#mana-and-cost-system)
- [Static Effects and Layers](#static-effects-and-layers)
- [Integration Points](#integration-points)
- [Performance Considerations](#performance-considerations)
- [Testing Strategy](#testing-strategy)
- [Extending the Engine](#extending-the-engine)

## Overview

The Forge Game Engine is a complete implementation of Magic: The Gathering's rules system, featuring:

- **Complete MTG Rules Implementation**: Handles all official MTG mechanics and interactions
- **Event-Driven Architecture**: Flexible trigger and replacement effect system
- **Multi-Player Support**: Native support for 2+ player games
- **Zone Management**: Comprehensive tracking of all MTG zones
- **Layered Effects System**: Implements MTG's 8-layer dependency system
- **AI Integration**: Clean interfaces for artificial intelligence players
- **Performance Optimized**: Efficient handling of complex game states
- **Extensible Design**: Easy addition of new cards and mechanics

### Core Dependencies

```xml
<dependencies>
    <dependency>
        <groupId>forge</groupId>
        <artifactId>forge-core</artifactId>
    </dependency>
    <dependency>
        <groupId>com.esotericsoftware</groupId>
        <artifactId>minlog</artifactId>
    </dependency>
    <dependency>
        <groupId>org.jgrapht</groupId>
        <artifactId>jgrapht-core</artifactId>
    </dependency>
</dependencies>
```

## Architecture

The game engine follows a layered, event-driven architecture with clear separation of concerns:

```
┌─────────────────────────────────────┐
│            Game Controller          │
│     Game, GameController, Rules     │
├─────────────────────────────────────┤
│         Player Management           │
│  Player, PlayerCollection, Actions  │
├─────────────────────────────────────┤
│        Phase & Turn System          │
│   PhaseHandler, TurnHandler, Stack  │
├─────────────────────────────────────┤
│          Card System                │
│  Card, CardState, SpellAbility      │
├─────────────────────────────────────┤
│         Event System                │
│ TriggerHandler, ReplacementHandler  │
├─────────────────────────────────────┤
│         Zone Management             │
│  Battlefield, Hand, Library, etc.   │
├─────────────────────────────────────┤
│        Effects & Abilities          │
│  AbilityFactory, StaticEffects      │
└─────────────────────────────────────┘
```

### Key Design Patterns

- **State Pattern**: Phase and turn management
- **Observer Pattern**: Event system and triggers
- **Command Pattern**: Delayed execution and stack management
- **Factory Pattern**: Ability and effect creation
- **Strategy Pattern**: Different game formats and variants
- **Composite Pattern**: Complex card abilities and effects

## Game State Management

### Core Game Class

The `Game` class serves as the central state container:

```java
public class Game {
    private final GameRules rules;                    // Game format and rules
    private final PlayerCollection allPlayers;       // All players in game
    private final PhaseHandler phaseHandler;         // Turn/phase management
    private final StaticEffects staticEffects;       // Continuous effects
    private final TriggerHandler triggerHandler;     // Trigger management
    private final ReplacementHandler replacementHandler; // Replacement effects
    private final EventBus events;                   // Event communication
    private final GameLog gameLog;                   // Game history
    private final GameOutcome outcome;               // Win/loss tracking
}
```

### Game Lifecycle

```java
// Game initialization
Game game = new Game(rules, players);
game.getAction().startGame();

// Main game loop
while (!game.isGameOver()) {
    game.getPhaseHandler().handlePhase();
    game.getAction().checkStateBased();
}

// Game cleanup
game.cleanUp();
```

### State-Based Actions

Automatic rule enforcement through state-based actions:
- Creature damage/destruction
- Planeswalker loyalty
- Legend rule enforcement
- Token cleanup
- Win/loss condition checking

## Card and Ability System

### Card Architecture

Cards support complex multi-state representations:

```java
public class Card {
    private final List<CardState> states;      // Double-faced, flip cards
    private CardState currentState;            // Active card face
    private Map<CardProperty, Object> props;   // Dynamic properties
    private final Set<String> keywords;        // Keyword abilities
    private final Map<String, String> sVars;   // Script variables
}
```

### Card States and Transformations

```java
// Double-faced card support
if (card.isDoubleFaced()) {
    CardState otherSide = card.getAlternateState();
    card.setState(otherSide, true);
}

// Split card handling
if (card.isSplitCard()) {
    CardState leftSplit = card.getState(CardStateName.LeftSplit);
    CardState rightSplit = card.getState(CardStateName.RightSplit);
}
```

### Ability System

Comprehensive ability framework:

```java
public abstract class SpellAbility {
    protected Cost cost;                    // Mana and additional costs
    protected TargetRestrictions targetRestrictions; // Targeting rules
    protected List<AbilitySubType> effects; // What the ability does
    protected Map<String, String> params;   // Ability parameters
    
    public abstract boolean canPlay();
    public abstract void resolve();
}
```

### Effect Framework

Modular effect system with 100+ effect types:

```java
// Example effect implementation
public class DamageEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        int damage = AbilityUtils.calculateAmount(sa.getHostCard(), "NumDmg", sa);
        List<GameObject> targets = getTargets(sa);
        
        for (GameObject target : targets) {
            target.addDamage(damage, sa.getHostCard(), sa);
        }
    }
}
```

## Rules Engine Implementation

### Phase and Turn Management

The `PhaseHandler` implements MTG's complex turn structure:

```java
public class PhaseHandler {
    private PhaseType currentPhase = PhaseType.UNTAP;
    private Player activePlayer;
    private int turnNumber = 0;
    
    public void nextPhase() {
        // Handle phase transitions
        // Check for phase skipping effects
        // Manage priority and timing
        // Process end-of-phase effects
    }
}
```

### Priority System

```java
public void passPriority() {
    Player nextPlayer = getNextPlayerWithPriority();
    if (nextPlayer == null) {
        // All players passed - resolve top of stack
        resolveStackItem();
    } else {
        setPlayerWithPriority(nextPlayer);
    }
}
```

### Stack Management

```java
public class SpellStack extends FCollectionView<SpellAbility> {
    public void add(SpellAbility ability) {
        // Add to top of stack
        // Trigger spell/ability cast events
        // Check for responses
    }
    
    public void resolve() {
        SpellAbility topItem = peekFirst();
        // Check if targets are still legal
        // Apply replacement effects
        // Resolve the ability
        removeFirst();
    }
}
```

## Event-Driven Architecture

### Event System

The game uses multiple event mechanisms for different purposes:

```java
// EventBus for general game events
private final EventBus events = new EventBus("game events");

public void fireEvent(final Event event) {
    events.post(event);
}

// Trigger system for MTG-specific triggers
public void runTrigger(TriggerType type, Map<AbilityKey, Object> runParams) {
    List<Trigger> triggers = triggerHandler.collectTriggers(type);
    for (Trigger trigger : triggers) {
        if (trigger.requirementsCheck(runParams)) {
            triggerHandler.registerDelayedTrigger(trigger, runParams);
        }
    }
}
```

### Trigger Types

80+ trigger types covering all MTG interactions:

```java
public enum TriggerType {
    ChangesZone,           // Enters/leaves battlefield
    Taps,                  // Creature/permanent taps
    Attacks,               // Creature attacks
    DamageDone,            // Damage dealt
    SpellCast,             // Spell cast
    AbilityActivated,      // Activated ability
    CounterAdded,          // Counters added/removed
    // ... 70+ more trigger types
}
```

### Replacement Effects

```java
public class ReplacementHandler {
    public ReplacementResult run(ReplacementType event, Map<AbilityKey, Object> runParams) {
        List<ReplacementEffect> effects = getApplicableReplacements(event);
        
        // Apply replacement effects in timestamp order
        for (ReplacementEffect effect : effects) {
            if (effect.canReplace(runParams)) {
                return effect.replace(runParams);
            }
        }
        
        return ReplacementResult.NotReplaced;
    }
}
```

## Zone Management

Comprehensive zone tracking for all MTG zones:

### Zone Types

```java
public enum ZoneType {
    Battlefield,    // Permanents in play
    Hand,          // Cards in hand
    Library,       // Player's deck
    Graveyard,     // Discard pile
    Stack,         // Spells/abilities resolving
    Exile,         // Exiled cards
    Command,       // Command zone (commanders, emblems)
    Sideboard,     // Sideboard cards
    PlanarDeck,    // Planechase planes
    SchemeDeck,    // Archenemy schemes
    Ante           // Ante zone (legacy)
}
```

### Zone Change Tracking

```java
public class ZoneChangeInfo {
    private final Card card;
    private final ZoneType origin;
    private final ZoneType destination;
    private final Player newController;
    
    // Last Known Information (LKI) tracking
    private final CardState lastKnownInfo;
}
```

### Last Known Information System

```java
public CardState getLastKnownInformation(Card card) {
    ZoneChangeInfo lki = card.getLastKnownInformation();
    return lki != null ? lki.getLastKnownInfo() : card.getCurrentState();
}
```

## Combat System

Comprehensive combat implementation supporting all MTG combat rules:

### Combat Structure

```java
public class Combat {
    private final Map<Card, GameEntity> attackersMap;     // Attacker -> Defender
    private final Map<Card, CardCollection> blockersMap; // Attacker -> Blockers
    private final List<Card> attackers;                   // Attack order
    private final CombatDamageMap damageMap;             // Damage assignments
    
    public void declareAttackers(List<Card> attackers) {
        // Validate legal attackers
        // Check attack restrictions
        // Trigger attack events
    }
    
    public void declareBlockers(Map<Card, CardCollection> blocks) {
        // Validate legal blocks
        // Order multiple blockers
        // Trigger block events
    }
}
```

### Damage Assignment

```java
public class CombatDamageMap {
    public void assignDamage(Card source, List<Card> targets, int totalDamage) {
        // Implement MTG damage assignment rules
        // Handle trample, deathtouch, lifelink
        // Order damage to multiple blockers
    }
}
```

## Mana and Cost System

### Mana Pool Management

```java
public class ManaPool {
    private final Map<Byte, Integer> manaMap;  // Color -> Amount
    
    public boolean canPayCost(ManaCost cost) {
        // Check if mana pool can pay cost
        // Handle generic mana requirements
        // Account for mana restrictions
    }
    
    public void payManaFromPool(ManaCost cost, boolean isActivatedAbility) {
        // Remove mana from pool
        // Apply mana burn if enabled
        // Handle special mana types
    }
}
```

### Cost System

Flexible cost framework supporting all MTG cost types:

```java
public abstract class Cost {
    public abstract boolean canPay(SpellAbility ability);
    public abstract boolean payAsDecided(Player player, PaymentDecision decision);
    public abstract String toString();
}

// Specific cost types
public class CostPayMana extends Cost { /* Mana costs */ }
public class CostTap extends Cost { /* Tap costs */ }
public class CostSacrifice extends Cost { /* Sacrifice costs */ }
public class CostDiscard extends Cost { /* Discard costs */ }
// ... many more cost types
```

### Alternative Costs and Cost Reduction

```java
public class SpellAbility {
    private Cost alternativeCost;          // Flashback, madness, etc.
    private List<CostReduction> reductions; // Convoke, delve, etc.
    
    public ManaCost getManaCostToPay() {
        ManaCost cost = getManaCost();
        
        // Apply cost reductions
        for (CostReduction reduction : reductions) {
            cost = reduction.reduce(cost);
        }
        
        return cost;
    }
}
```

## Static Effects and Layers

Implementation of MTG's layered dependency system:

### Layer System

```java
public enum StaticEffectLayer {
    COPY(1),              // Copy effects
    CONTROL(2),           // Control-changing effects
    TEXT(3),              // Text-changing effects  
    TYPE(4),              // Type-changing effects
    COLOR(5),             // Color-changing effects
    ABILITY(6),           // Ability-adding/removing effects
    SETPT(7a),            // Power/toughness setting effects
    MODIFYPT(7b);         // Power/toughness modifying effects
    
    private final int layerNumber;
}
```

### Static Effect Application

```java
public class StaticEffects {
    public void processStaticEffects() {
        // Apply effects in layer order
        for (StaticEffectLayer layer : StaticEffectLayer.values()) {
            List<StaticEffect> layerEffects = getEffectsInLayer(layer);
            
            // Sort by timestamp within layer
            Collections.sort(layerEffects, TIMESTAMP_COMPARATOR);
            
            // Apply effects in timestamp order
            for (StaticEffect effect : layerEffects) {
                effect.apply();
            }
        }
    }
}
```

### Timestamp System

```java
public class Card {
    private long timestamp = getNextTimestamp();
    
    private static long getNextTimestamp() {
        return System.nanoTime(); // Unique ordering for all cards
    }
}
```

## Integration Points

### AI Integration

Clean interfaces for AI decision-making:

```java
public abstract class PlayerController {
    // Decision points for AI/human players
    public abstract SpellAbility chooseSpellAbilityToPlay();
    public abstract List<Card> chooseCardsToDiscardFrom(List<Card> list);
    public abstract Card chooseSingleCard(List<Card> list, String message);
    public abstract boolean confirmAction(String message);
    // ... many more decision points
}
```

### GUI Integration

View system for UI updates:

```java
public interface IGameView {
    void updateZone(ZoneType zone, Player owner);
    void updateCard(Card card);
    void showMessage(String message);
    void highlightCard(Card card);
    // ... UI update methods
}
```

### Game State Queries

Comprehensive game state inspection:

```java
public class GameState {
    public List<Card> getCardsIn(ZoneType zone, Player player);
    public List<Card> getCreaturesInPlay();
    public List<SpellAbility> getPlayableSpells(Player player);
    public boolean isCardPlayable(Card card, Player player);
    // ... many query methods
}
```

## Performance Considerations

### Optimization Strategies

```java
// Lazy initialization for expensive computations
private final Supplier<List<Card>> playableCards = 
    Suppliers.memoize(() -> calculatePlayableCards());

// Efficient collections for game objects
public class CardCollection extends FCollection<Card> {
    // Optimized for frequent add/remove operations
    // Memory-efficient storage
    // Fast filtering and searching
}

// Caching for repeated calculations
private final Map<Card, Integer> cardPowerCache = new HashMap<>();

public int getCardPower(Card card) {
    return cardPowerCache.computeIfAbsent(card, this::calculateCardPower);
}
```

### Memory Management

```java
public void cleanup() {
    // Clear references for garbage collection
    cardPowerCache.clear();
    staticEffects.cleanup();
    triggerHandler.cleanup();
    
    // Clean up circular references
    for (Player player : players) {
        player.cleanup();
    }
}
```

### Scalability Features

- **Multiplayer Support**: Native support for 2+ players with efficient state management
- **Complex Interactions**: Handles arbitrary card combinations without performance degradation
- **Memory Efficiency**: Optimized data structures for large game states
- **Event Batching**: Groups related events to reduce processing overhead

## Testing Strategy

### Test Architecture

```java
@Test
public class GameEngineTest {
    private Game game;
    private Player player1, player2;
    
    @BeforeMethod
    public void setUp() {
        List<RegisteredPlayer> players = createTestPlayers();
        GameRules rules = new GameRules(GameType.Regular);
        game = new Game(players, rules, null);
    }
    
    @Test
    public void testBasicGameplay() {
        // Test basic game flow
        game.getAction().startGame();
        assertFalse(game.isGameOver());
        
        // Simulate turns and actions
        // Verify game state changes
    }
}
```

### Test Categories

1. **Unit Tests**: Individual component testing
   - AbilityKey string conversion
   - ManaCost parsing and calculations
   - Card property management

2. **Integration Tests**: Multi-component interaction
   - Spell resolution with triggers
   - Combat damage assignment
   - Zone change effects

3. **Rules Tests**: MTG-specific rules validation
   - Complex card interactions
   - Layer system correctness
   - Priority and timing rules

4. **Performance Tests**: Scalability and efficiency
   - Large game state handling
   - Memory usage monitoring
   - Response time validation

### Testing Complex Interactions

```java
@Test
public void testComplexCardInteraction() {
    // Set up specific game state
    Card creature = addCardToPlay("Lightning Bolt", player1);
    Card target = addCardToPlay("Grizzly Bears", player2);
    
    // Execute interaction
    SpellAbility bolt = creature.getFirstSpellAbility();
    bolt.getTargets().add(target);
    bolt.resolve();
    
    // Verify results
    assertTrue(target.isInZone(ZoneType.Graveyard));
    assertEquals(3, target.getDamage());
}
```

## Extending the Engine

### Adding New Card Mechanics

1. **Create Ability Implementation**:
```java
public class MyMechanicAbility extends SpellAbility {
    @Override
    public boolean canPlay() {
        // Define when this ability can be activated
        return super.canPlay() && customConditions();
    }
    
    @Override
    public void resolve() {
        // Define what the ability does
        performCustomEffect();
    }
}
```

2. **Register in AbilityFactory**:
```java
// In AbilityFactory
map.put("MyMechanic", MyMechanicAbility.class);
```

3. **Add Card Script Support**:
```
Name:New Card
ManaCost:2 G
Types:Creature Elf
PT:2/2
A:AB$ MyMechanic | Cost$ T | CustomParam$ Value
```

### Adding New Trigger Types

```java
// 1. Add to TriggerType enum
public enum TriggerType {
    // ... existing triggers ...
    MyCustomTrigger,
}

// 2. Implement trigger logic
public class TriggerMyCustom extends Trigger {
    @Override
    public boolean performTest(Map<AbilityKey, Object> runParams) {
        // Define when this trigger fires
        return checkCustomConditions(runParams);
    }
}

// 3. Register trigger
TriggerHandler.registerTrigger(TriggerType.MyCustomTrigger, TriggerMyCustom.class);
```

### Adding New Zones

```java
// 1. Add to ZoneType enum
public enum ZoneType {
    // ... existing zones ...
    MyCustomZone,
}

// 2. Implement zone behavior
public class MyCustomZone extends PlayerZone {
    @Override
    public boolean isKnown() {
        return true; // Whether contents are public
    }
    
    @Override
    public boolean isOrdered() {
        return false; // Whether order matters
    }
}
```

### Performance Best Practices

1. **Minimize Object Creation**: Reuse objects where possible
2. **Use Efficient Collections**: Choose appropriate data structures
3. **Cache Expensive Calculations**: Store results of complex computations  
4. **Batch Event Processing**: Group related events together
5. **Lazy Evaluation**: Defer expensive operations until needed
6. **Memory Management**: Clean up references promptly

### Code Organization Guidelines

1. **Single Responsibility**: Each class should have one clear purpose
2. **Interface Segregation**: Use specific interfaces rather than large ones
3. **Dependency Injection**: Use constructor injection for dependencies
4. **Immutability**: Use immutable objects where possible
5. **Error Handling**: Provide clear error messages and recovery paths

## Best Practices for Contributors

### Code Style

- Follow existing naming conventions
- Use meaningful variable and method names
- Add comprehensive JavaDoc for public methods
- Include unit tests for new functionality
- Maintain backward compatibility where possible

### Testing Requirements

- Unit tests for all new classes and methods
- Integration tests for complex interactions
- Performance tests for potentially expensive operations
- Regression tests for bug fixes

### Documentation Standards

- Update this README for architectural changes
- Document new mechanics and their interactions
- Provide examples for complex functionality
- Maintain accurate API documentation

The Forge Game Engine represents a mature, comprehensive implementation of the Magic: The Gathering rules system. Its sophisticated architecture, performance optimizations, and extensible design make it suitable for a wide range of applications from casual gaming to competitive AI research. By understanding these architectural principles and following the established patterns, developers can effectively contribute to and extend this powerful game engine.