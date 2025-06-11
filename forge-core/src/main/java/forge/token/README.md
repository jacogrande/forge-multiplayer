# Token Domain

The token domain provides specialized handling for token creatures and objects created during MTG gameplay.

## Key Classes

- **`TokenDb`** - Database for token definitions and artwork
- **`PaperToken`** - Physical token representation with edition data
- **`ITokenDatabase`** - Interface for token database implementations

## Main Functionality

- **Token Storage**: Comprehensive database of token definitions
- **Edition Variants**: Set-specific token artwork and properties
- **Token Lookup**: Search tokens by name, set, and characteristics
- **Image Management**: Token artwork keys and display coordination

## Architecture

```
ITokenDatabase → TokenDb → PaperToken
                    ↓
              Token Artwork
```

## Key Features

- **Comprehensive Coverage**: All official MTG tokens from sets
- **Edition Tracking**: Tokens tied to specific MTG releases
- **Art Variants**: Multiple artwork options for popular tokens
- **Search Capabilities**: Find tokens by name, colors, types
- **Integration Ready**: Consistent interface with card database

## Token Types Supported

- **Creature Tokens**: Standard creature tokens with P/T
- **Artifact Tokens**: Treasure, Food, Clue tokens
- **Special Tokens**: Emblems, planes, and other game objects
- **Custom Tokens**: Support for user-defined token types

## Integration Points

- Parallel structure to `CardDb` for consistency
- Uses `CardEdition` for set-specific token variants
- Connected to `StaticData` for global token access
- Integrates with game engine for token creation during play
- Supports storage layer for token database persistence