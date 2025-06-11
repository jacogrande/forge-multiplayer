# Card Domain

The card domain handles core card representation, database management, and MTG-specific card properties.

## Key Classes

- **`CardDb`** - Primary card database with smart lookup and art preferences
- **`CardRules`** - Complete card rules and property definitions
- **`CardEdition`** - MTG set/edition representation with metadata
- **`CardType`** - MTG type system (creatures, instants, etc.)
- **`ColorSet`** - Color identity and mana color management

## Main Functionality

- **Card Database**: Comprehensive card lookup with multiple search strategies
- **Art Management**: Sophisticated art preference system (latest vs original)
- **Edition Handling**: Set-based filtering and smart art selection
- **Type System**: Complete MTG card type, supertype, and subtype support
- **Color System**: WUBRG color combinations and identity tracking

## Architecture

```
CardDb → CardRules → CardType/ColorSet
   ↓         ↓
CardEdition  Card Properties
```

## Key Features

- **Smart Lookup**: Search by name, set, art index, collector number
- **Art Preferences**: User-configurable art selection strategies
- **Lazy Loading**: Performance-optimized card loading
- **Thread Safety**: Concurrent access to card database
- **Format Support**: Integration with format legality checking

## Integration Points

- Used by `Deck` for card pool management
- Integrates with `StaticData` for global access
- Connected to `item` package for physical card representations
- Supports storage layer for persistence