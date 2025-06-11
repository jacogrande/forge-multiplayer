# Item Domain

The item domain represents physical MTG products including cards, boosters, and sealed products.

## Key Classes

- **`PaperCard`** - Lightweight physical card with edition and art data
- **`IPaperCard`** - Interface defining card contract for different implementations
- **`BoosterPack`** - Booster pack representation with generation logic
- **`SealedTemplate`** - Templates for sealed product configuration
- **`InventoryItem`** - Base interface for tradeable game items

## Main Functionality

- **Physical Cards**: Specific printings with edition, art, and foil information
- **Sealed Products**: Booster packs, boxes, and other sealed items
- **Image Management**: Card image keys and artwork organization
- **Inventory System**: Base framework for collection management
- **Product Generation**: Template-based sealed product creation

## Architecture

```
InventoryItem
    ↓
IPaperCard → PaperCard
    ↓           ↓
BoosterPack  Image Keys
```

## Key Features

- **Edition Specific**: Cards tied to specific MTG sets and printings
- **Art Variants**: Support for multiple artworks per card
- **Foil Support**: Premium card variants and special treatments
- **Collector Info**: Collector numbers, artist names, rarity
- **Image Keys**: Standardized image identification system

## Sealed Product Support

- **Booster Packs**: Standard 15-card booster generation
- **Booster Boxes**: Multi-pack sealed products
- **Special Products**: Commander decks, bundles, special releases
- **Template System**: Configurable product definitions

## Integration Points

- Implements physical layer for `CardRules` from card domain
- Used extensively by `Deck` and `CardPool` for card management
- Connected to storage systems for collection persistence
- Integrates with image systems for card display