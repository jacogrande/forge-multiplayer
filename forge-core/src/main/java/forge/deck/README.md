# Deck Domain

The deck domain provides comprehensive deck construction, management, and validation for all MTG formats.

## Key Classes

- **`Deck`** - Multi-section deck with main, sideboard, commander sections
- **`CardPool`** - Card collection with quantities and statistical analysis
- **`DeckSection`** - Type-safe deck sections with format-specific validation
- **`DeckBase`** - Abstract foundation for deck-like objects

## Main Functionality

- **Multi-Section Support**: Main deck, sideboard, commander, planes, schemes
- **Format Validation**: Commander, Standard, Limited deck rules
- **Art Harmonization**: Intelligent art selection for visual coherence
- **Statistical Analysis**: CMC curves, color distribution, type breakdown
- **Performance Optimization**: Deferred loading for large collections

## Architecture

```
Deck → DeckSection → CardPool
  ↓        ↓          ↓
Art     Validation  Statistics
Prefs
```

## Key Features

- **Section Management**: Automatic card placement in appropriate sections
- **Smart Art Selection**: Harmonizes artwork across deck sections
- **Validation Rules**: Format-specific deck construction constraints
- **Lazy Loading**: Deferred deck loading for performance
- **Commander Support**: Special handling for singleton formats

## Format Support

- **Standard/Modern**: 60+ card main deck, 15 card sideboard
- **Commander**: 100 card singleton with commander zone
- **Limited**: Draft and sealed deck configurations
- **Casual Variants**: Planechase, Archenemy support

## Integration Points

- Heavy integration with `CardDb` for card resolution
- Uses `StaticData` for format legality and art preferences
- Connected to `item.PaperCard` for physical representations
- Supports multiple storage backends for persistence