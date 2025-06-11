# Forge GUI Domain

The `forge-gui` module is the core GUI framework and shared game management system for MTG Forge. It serves as a platform-agnostic foundation that supports multiple GUI implementations (desktop Swing, mobile LibGDX, Android, iOS) while providing comprehensive game logic, deck management, and user interface abstractions.

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Module Structure](#module-structure)
- [Platform Abstraction](#platform-abstraction)
- [Game Modes](#game-modes)
- [Data Management](#data-management)
- [Development Setup](#development-setup)
- [Key Design Patterns](#key-design-patterns)
- [Testing](#testing)
- [Contributing](#contributing)

## Architecture Overview

### Core Responsibilities

The `forge-gui` module provides:

1. **Platform Abstraction Layer**: Interfaces that allow different UI implementations (Swing, LibGDX, etc.) to share common functionality
2. **Game Logic Coordination**: Manages game state, player actions, and GUI events
3. **Data Models**: Centralized models for decks, preferences, achievements, and game collections
4. **Game Mode Implementations**: Quest mode, limited formats, tournaments, and planar conquest
5. **Content Management**: Card database, sets, cubes, and user content
6. **Network Support**: Client/server architecture for multiplayer games

### Dependencies

```
forge-gui depends on:
├── forge-core      # Core utilities and data structures
├── forge-game      # Game rules engine  
├── forge-ai        # AI player implementations
├── Netty           # Network communication
├── XStream         # XML serialization
└── Jetty           # HTTP server functionality
```

## Module Structure

### Key Source Packages

```
src/main/java/forge/
├── deck/               # Deck generation and management
├── gamemodes/          # Game mode implementations
│   ├── gauntlet/       # Gauntlet challenges
│   ├── limited/        # Draft and sealed formats
│   ├── match/          # Game instance management
│   ├── net/            # Network multiplayer
│   ├── planarconquest/ # Planar conquest campaign
│   ├── puzzle/         # Puzzle challenges
│   ├── quest/          # Quest mode campaign
│   └── tournament/     # Tournament system
├── gui/                # GUI abstractions and utilities
│   ├── interfaces/     # Platform abstraction interfaces
│   ├── card/          # Card display and scripting
│   ├── control/       # Game control and playback
│   └── util/          # GUI utilities
├── interfaces/         # Core system interfaces
├── itemmanager/        # Collection management framework
├── localinstance/      # Local game instance management
│   ├── achievements/   # Achievement system
│   ├── properties/     # Configuration management
│   └── skin/          # UI theming system
├── model/              # Core data models
├── player/             # Human player controllers
├── sound/              # Audio system abstraction
└── util/               # Shared utilities
```

### Resource Structure

```
res/
├── adventure/          # Adventure mode content
├── ai/                 # AI behavior profiles
├── blockdata/          # Set and draft configuration
├── cardsfolder/        # Card script database
├── conquest/           # Planar conquest data
├── cube/               # Draft cube definitions
├── deckgendecks/       # Deck generation data
├── defaults/           # Default UI layouts
├── draft/              # Draft environment configs
├── editions/           # Set definitions and metadata
└── tools/              # Development utilities
```

## Platform Abstraction

The forge-gui module implements a sophisticated platform abstraction that allows the same game logic to run on different UI frameworks:

### Core Abstraction Interfaces

- **`IGuiBase`**: Primary platform interface providing UI primitives, file operations, and platform-specific functionality
- **`IGuiGame`**: Game-specific interface for match UI, player interactions, and game state display
- **`IMayViewCards`**: Permission system for card visibility in multiplayer scenarios

### Platform Detection

The `GuiBase` class provides runtime platform detection:

```java
// Platform configuration examples
GuiBase.setInterface(platformSpecificImplementation);
GuiBase.setIsAndroid(true);  // For Android builds
GuiBase.setIsAdventureMode(true);  // Adventure mode toggle
```

### UI Threading

Thread-safe UI operations are handled through:
- `FThreads.invokeInEdtLater()` - Asynchronous UI updates
- `FThreads.invokeInEdtAndWait()` - Synchronous UI operations

## Game Modes

### Quest Mode (`forge.gamemodes.quest`)

A campaign system where players progress through battles to unlock cards and content:

- **Quest Controller**: Manages progression, deck building, and opponent selection
- **Bazaar System**: In-game shop for purchasing cards and items
- **World System**: Multiple quest worlds with different themes and challenges
- **Reward System**: Card unlocks, special abilities, and progression rewards

### Limited Formats (`forge.gamemodes.limited`)

Draft and sealed play implementations:

- **Booster Draft**: 8-player draft with AI opponents
- **Sealed Deck**: Generate sealed pools and build decks
- **Cube Draft**: Support for 70+ predefined cubes
- **Winston Draft**: 2-player draft format
- **Chaos Draft**: Mixed-set drafting

### Planar Conquest (`forge.gamemodes.planarconquest`)

Campaign mode inspired by Magic: the Gathering's multiverse:

- **Plane System**: Travel between different planes with unique rules
- **Commander Format**: All games use Commander/EDH format
- **Progression System**: Unlock new planes and commanders
- **Battle System**: Territory control and conquest mechanics

### Tournament System (`forge.gamemodes.tournament`)

Structured competitive play:

- **Swiss Tournaments**: Round-robin style tournaments
- **Bracket Tournaments**: Single/double elimination
- **Gauntlet Mode**: Sequential challenges against themed decks

## Data Management

### Model Architecture (`forge.model.FModel`)

The central data model provides access to:

```java
FModel.getMagicDb()           // Card database access
FModel.getDecks()             // Deck collections
FModel.getPreferences()       // User preferences
FModel.getQuest()             // Quest mode data
FModel.getAchievements()      // Achievement tracking
```

### Card Database Integration

Card data is loaded from multiple sources:
- **Core Cards**: `res/cardsfolder/` - 20,000+ card scripts
- **Tokens**: Token creature definitions
- **Custom Cards**: User-defined card extensions
- **Set Data**: `res/editions/` - Complete Magic set information

### Card Script Format

Cards are defined using a custom script format:

```
Name:Lightning Bolt
ManaCost:R
Types:Instant
A:SP$ DealDamage | ValidTgts$ Any | TgtPrompt$ Select target | NumDmg$ 3
Oracle:Lightning Bolt deals 3 damage to any target.
```

### Preferences and Configuration

Hierarchical preferences system:
- **ForgePreferences**: Global application settings
- **QuestPreferences**: Quest mode specific settings
- **DeckPreferences**: Deck building and collection preferences
- **ConquestPreferences**: Planar conquest settings

## Development Setup

### Building

```bash
# Build the forge-gui module
mvn clean install

# Build with specific profile
mvn clean install -P windows-linux

# Run tests
mvn test
```

### Running

The forge-gui module cannot be run independently. It requires a platform-specific implementation:

```bash
# For desktop development, use forge-gui-desktop
cd ../forge-gui-desktop
mvn exec:java

# For mobile development, use forge-gui-mobile-dev
cd ../forge-gui-mobile-dev
mvn exec:java
```

### Configuration

Copy `forge.profile.properties.example` to `forge.profile.properties` and configure:
- User data directories
- Asset locations
- Performance settings
- Debug options

## Key Design Patterns

### Model-View-Controller (MVC)

- **Models**: Data classes in `forge.model` package
- **Views**: Platform-specific implementations of `IGuiBase` and `IGuiGame`
- **Controllers**: Game logic in `forge.gamemodes` and player controllers

### Observer Pattern

Extensive use of observers for:
- Game state changes
- UI updates
- Achievement progress
- Preference changes

### Strategy Pattern

- **AI Profiles**: Different AI behaviors for various difficulty levels
- **Game Modes**: Pluggable game type implementations
- **Deck Generators**: Various deck building algorithms

### Bridge Pattern

The platform abstraction layer uses the bridge pattern to separate GUI interface from platform-specific implementations.

## Testing

### Test Structure

```bash
# Run all tests
mvn test

# Run specific test categories
mvn test -Dtest=*Test
mvn test -Dtest=*IntegrationTest
```

### Test Categories

- **Game Logic Tests**: Validate game rule implementations
- **Deck Generation Tests**: Verify deck building algorithms
- **Achievement Tests**: Test achievement trigger conditions
- **Preference Tests**: Configuration management validation

### Performance Testing

The module includes performance testing for:
- Large deck collections
- Memory usage in long-running games
- AI decision-making speed
- Card database loading

## Key Components Reference

### Core Classes

- **`FModel`**: Central data model and service locator
- **`GuiBase`**: Platform abstraction and global state
- **`AbstractGuiGame`**: Base implementation for game interfaces
- **`CardCollections`**: Deck and collection management
- **`GamePlayerUtil`**: Player utilities and preferences

### Game Controllers

- **`QuestController`**: Quest mode progression and state
- **`ConquestController`**: Planar conquest campaign management
- **`TournamentController`**: Tournament bracket and pairing logic
- **`HostedMatch`**: Individual game instance management

### Network Components

- **`FGameClient`/`ServerGameLobby`**: Client/server architecture
- **`NetConnectUtil`**: Network discovery and connection
- **`ProtocolMethod`**: Network message definitions

## Contributing

### Code Style

- Follow existing Java conventions
- Use dependency injection through FModel
- Implement proper error handling and logging
- Add unit tests for new functionality

### Adding New Game Modes

1. Create package under `forge.gamemodes`
2. Implement controller and data classes
3. Add UI integration points
4. Update FModel initialization
5. Add appropriate tests

### Extending Platform Support

1. Implement `IGuiBase` and `IGuiGame` interfaces
2. Handle platform-specific threading
3. Implement file I/O and preferences storage
4. Add platform detection in GuiBase
5. Test across different screen sizes/input methods

### Performance Considerations

- Card database loading is memory-intensive
- Large collections require pagination
- Network operations should be asynchronous
- Image loading should be lazy and cached

## Memory Management

The forge-gui module manages significant amounts of data:

- **Card Database**: 20,000+ cards with full rule text
- **Image Caching**: Card art and UI graphics
- **Game History**: Match replays and statistics
- **Collection Data**: Large deck and collection storage

### Optimization Strategies

- Lazy loading of card pools
- Weak references for cached images
- Preference-based memory limits
- Garbage collection tuning for large heaps

---

For platform-specific implementation details, see:
- `forge-gui-desktop/README.md` - Desktop Swing implementation
- `forge-gui-mobile/README.md` - Mobile LibGDX implementation
- `forge-gui-android/README.md` - Android-specific features