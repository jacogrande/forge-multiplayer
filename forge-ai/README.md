# Forge AI Module

The `forge-ai` module provides sophisticated artificial intelligence for Magic: The Gathering gameplay in Forge. It implements a comprehensive AI system capable of playing MTG at a competitive level across all game formats, with configurable personalities and advanced decision-making algorithms.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [AI Decision-Making Process](#ai-decision-making-process)
- [Configuration and Personalities](#configuration-and-personalities)
- [Key Components](#key-components)
- [Integration with Game Engine](#integration-with-game-engine)
- [Extending the AI](#extending-the-ai)
- [Performance Considerations](#performance-considerations)
- [Testing and Debugging](#testing-and-debugging)

## Overview

The Forge AI system is designed to provide challenging, varied, and strategically sound opponents for MTG gameplay. Key features include:

- **Single Unified AI** with configurable personality profiles
- **100+ Ability Implementations** covering virtually all MTG mechanics
- **Advanced Simulation Engine** for look-ahead analysis
- **Sophisticated Combat System** with threat assessment
- **Memory and Learning** capabilities
- **Performance Optimized** with timeout handling and efficient algorithms

## Architecture

The AI follows a layered, modular architecture:

```
┌─────────────────────────────────────┐
│           Game Interface            │
│  PlayerControllerAi, LobbyPlayerAi  │
├─────────────────────────────────────┤
│         Decision Controller         │
│      AiController, AiLogic         │
├─────────────────────────────────────┤
│        Specialized Systems          │
│  Combat, Simulation, Evaluation     │
├─────────────────────────────────────┤
│         Ability Handlers            │
│   SpellAbilityAi implementations    │
├─────────────────────────────────────┤
│         Utility Layer               │
│  ComputerUtil*, Memory, Helpers     │
└─────────────────────────────────────┘
```

### Core Dependencies

- `forge-core`: Basic game utilities and data structures
- `forge-game`: Game engine, rules, and card mechanics
- `apache-commons-math3`: Mathematical utilities for evaluations

## AI Decision-Making Process

The AI uses a sophisticated multi-stage decision process:

### 1. Legality and Cost Assessment
```java
// Example: Can the AI play this spell?
if (!ability.canPlay() || !ComputerUtilCost.canPayCost(ability, controller, simulate)) {
    return AiPlayDecision.CantPlaySuit;
}
```

### 2. Timing Evaluation
- Phase-appropriate decisions (combat tricks in combat, sorceries in main phase)
- Resource conservation for optimal timing
- Opponent's mana and responses consideration

### 3. Target Selection and Evaluation
```java
// Sophisticated targeting with multiple criteria
List<GameObject> targets = ability.getTargets();
GameObject bestTarget = ComputerUtilCard.getBestAiTarget(targets, ability);
```

### 4. Strategic Value Assessment
- Board state evaluation
- Card advantage calculations
- Life total considerations
- Win/loss condition analysis

### 5. Simulation and Look-Ahead
```java
// Simulate potential outcomes
GameCopier copier = new GameCopier(game);
Game simulation = copier.makeCopy();
// Evaluate multiple scenarios
```

## Configuration and Personalities

The AI supports extensive customization through `.ai` profile files located in `forge-gui/res/ai/`:

### Available Profiles

- **Default.ai**: Balanced, standard play
- **Cautious.ai**: Conservative, defensive approach
- **Reckless.ai**: Aggressive, high-risk strategies
- **Experimental.ai**: Testing ground for new features

### Key Configuration Parameters

#### Combat Behavior
```properties
PLAY_AGGRO=50                    # Aggression level (0-100)
CHANCE_TO_ATTACK_INTO_TRADE=65   # Trading willingness
TRY_TO_AVOID_ATTACKING_INTO_CERTAIN_BLOCK=85
```

#### Spell Usage
```properties
MIN_SPELL_CMC_TO_COUNTER=2       # Counterspell thresholds
CHANCE_TO_COUNTER_CMC_2=80       # CMC-specific counter rates
HOLD_X_DAMAGE_SPELLS_WITH_3_MANA=25
```

#### Resource Management
```properties
HOLD_LAND_DROP_FOR_MAIN2_IF_UNUSED=80
RESERVE_MANA_FOR_MAIN2_CHANCE=60
```

### Creating Custom Profiles

1. Copy an existing `.ai` file
2. Modify parameters to suit desired behavior
3. Test with different deck archetypes
4. Fine-tune based on performance

## Key Components

### AiController
Central coordinator managing all AI decisions:
```java
public class AiController {
    // Manages decision flow, memory, and simulation
    private AiCardMemory cardMemory;
    private GameStateEvaluator evaluator;
    private GameSimulator simulator;
}
```

### SpellAbilityAi Hierarchy
Base class for all ability-specific AI logic:
```java
public abstract class SpellAbilityAi {
    protected abstract boolean canPlayAi(Player ai, SpellAbility sa);
    protected abstract boolean doTriggerAi(Player ai, SpellAbility sa, boolean mandatory);
}
```

### Combat System
Sophisticated combat evaluation:
```java
public class AiAttackController {
    // Evaluates all possible attack scenarios
    // Considers opponent's likely blocks
    // Optimizes for damage while minimizing losses
}
```

### Memory System
Tracks game state and decisions:
```java
public class AiCardMemory {
    private final Map<Set<Card>, CardCollection> cardSets;
    // MANDATORY_ATTACKERS, HELD_MANA_SOURCES, etc.
}
```

## Integration with Game Engine

### Player Controller Integration
```java
public class PlayerControllerAi extends PlayerController {
    @Override
    public SpellAbility chooseSpellAbilityToPlay() {
        return aiController.chooseSpellAbilityToPlay();
    }
    
    @Override
    public List<Card> chooseCardsToDiscardFrom(List<Card> list) {
        return ComputerUtil.getCardsToDiscard(list, player);
    }
}
```

### Factory Pattern for AI Creation
```java
public class LobbyPlayerAi implements IGameEntitiesFactory {
    public Player createPlayer(String name, int index) {
        return new Player(name, this, index);
    }
}
```

## Extending the AI

### Adding New Ability Support

1. **Create Ability-Specific AI Class**:
```java
public class MyAbilityAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAi(Player ai, SpellAbility sa) {
        // Implement specific logic for this ability
    }
}
```

2. **Register in SpellApiToAi**:
```java
map.put(ApiType.MyAbility, MyAbilityAi.class);
```

3. **Add Configuration Parameters**:
```properties
# In .ai profile files
MY_ABILITY_PREFERENCE=75
MIN_TARGETS_FOR_MY_ABILITY=2
```

### Implementing Custom Evaluation Logic

```java
public class CustomEvaluator {
    public static int evaluateCustomMetric(Card card, Player ai) {
        // Custom evaluation logic
        int value = 0;
        if (card.hasKeyword("Flying")) value += 50;
        if (card.getCMC() <= 2) value += 25;
        return value;
    }
}
```

### Adding Memory Categories

```java
// In AiCardMemory
public static final String MY_CUSTOM_SET = "MyCustomSet";

// Usage in AI logic
aiController.getCardMemory().remember(card, AiCardMemory.MY_CUSTOM_SET);
```

## Performance Considerations

### Timeout Handling
All AI decisions have configurable timeouts to prevent game freezing:
```java
public class SimulationController {
    private static final int DEFAULT_TIMEOUT_MILLIS = 5000;
    // Handles timeout gracefully with best available decision
}
```

### Efficient Game Copying
```java
public class GameCopier {
    // Optimized copying for simulation
    // Only copies necessary game state
    // Reuses immutable objects where possible
}
```

### Memory Management
- Card memory cleanup after games
- Efficient data structures for large game states
- Garbage collection friendly object creation

### Optimization Tips

1. **Profile Before Optimizing**: Use timing logs to identify bottlenecks
2. **Limit Simulation Depth**: Balance accuracy vs. performance
3. **Cache Evaluations**: Store expensive calculations
4. **Batch Operations**: Group similar decisions when possible

## Testing and Debugging

### AI-Specific Testing
```java
@Test
public void testAiDecisionMaking() {
    Game game = TestGameFactory.createGame();
    PlayerControllerAi ai = new PlayerControllerAi(game, player);
    
    // Set up specific game state
    SpellAbility decision = ai.chooseSpellAbilityToPlay();
    
    // Assert expected behavior
    assertEquals(expectedDecision, decision);
}
```

### Debugging Tools

1. **AI Logging**: Enable detailed decision logging
```java
// Set logging level for AI decisions
Logger.getLogger("forge.ai").setLevel(Level.DEBUG);
```

2. **Simulation Traces**: Track simulation outcomes
3. **Memory Inspection**: Monitor card memory state
4. **Profile Comparison**: A/B test different configurations

### Common Debugging Scenarios

- **AI Not Playing Spells**: Check mana costs and timing restrictions
- **Poor Target Selection**: Verify targeting logic and evaluations
- **Infinite Loops**: Implement proper termination conditions
- **Performance Issues**: Profile simulation depth and evaluation complexity

## Best Practices

### Code Organization
- Keep ability-specific logic in dedicated classes
- Use composition over inheritance where appropriate
- Maintain clear separation between evaluation and execution

### Configuration Management
- Document all AI parameters with clear descriptions
- Use meaningful default values
- Test parameter changes across multiple scenarios

### Performance Guidelines
- Avoid deep recursion in simulation
- Cache expensive evaluations
- Use appropriate data structures for lookups
- Handle edge cases gracefully

### Testing Recommendations
- Test against various deck archetypes
- Validate performance under different game states
- Ensure AI behavior matches profile expectations
- Test timeout handling and graceful degradation

## Contributing

When contributing to the AI system:

1. **Follow Existing Patterns**: Use established architectural patterns
2. **Add Comprehensive Tests**: Include unit and integration tests
3. **Document Configuration**: Update AI profiles with new parameters
4. **Performance Testing**: Ensure changes don't degrade performance
5. **Profile Validation**: Test with all AI personalities

The Forge AI system is designed to be extensible and maintainable. By following these guidelines and understanding the core architecture, developers can effectively enhance and customize the AI to provide even better gameplay experiences.