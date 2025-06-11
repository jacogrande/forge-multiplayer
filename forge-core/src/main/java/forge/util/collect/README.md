# Collections Domain

Specialized collection implementations optimized for MTG game data and performance.

## Key Classes

- **`FCollection<T>`** - Hybrid List/Set maintaining insertion order with uniqueness
- **`FCollectionView<T>`** - Read-only view interfaces for collections
- **`FCollectionReader`** - Utilities for populating collections from data sources

## Main Features

- **Hybrid Behavior**: Implements both List and Set interfaces
- **Order Preservation**: Maintains insertion order while ensuring uniqueness
- **Thread Safety**: Designed for concurrent access patterns
- **Memory Efficient**: Optimized storage for game object collections
- **Type Safe**: Generic implementation with compile-time type checking

## Architecture

```
Collection<T> ← FCollection<T> → List<T>
                     ↓              ↑
              FCollectionView<T> → Set<T>
```

## Use Cases

- **Card Collections**: Maintaining unique card lists with order
- **Player Collections**: Game objects owned by players
- **Zone Contents**: Battlefield, hand, graveyard card tracking
- **Ability Lists**: Ordered lists of unique abilities per card

## Performance Characteristics

- **O(1)** - Add/remove operations (amortized)
- **O(1)** - Contains checks via internal Set
- **O(n)** - Iteration in insertion order
- **Memory**: Single storage with dual access patterns

## Integration Points

- Used throughout `CardPool` and `Deck` classes
- Foundation for game engine zone management
- Supports serialization for persistence
- Thread-safe for concurrent game operations