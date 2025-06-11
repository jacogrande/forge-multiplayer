# Utilities Domain

The utilities domain provides comprehensive support infrastructure for all other domains in forge-core.

## Key Subpackages

### Collections (`collect/`)
- **`FCollection`** - Hybrid List/Set maintaining order with uniqueness
- **`FCollectionView`** - Read-only collection interfaces
- **Specialized Collections** - Game-optimized data structures

### Storage (`storage/`)
- **`IStorage`** - Generic storage interface for data persistence
- **`StorageBase`** - Abstract storage implementation
- **File Readers** - File-based storage and serialization

### Maps (`maps/`)
- **`MapToAmount`** - Quantity tracking for cards/items
- **`EnumMapOfLists`** - Optimized enum-keyed collections
- **Specialized Maps** - Game-specific map implementations

## Key Utility Classes

- **`TextUtil`** - String manipulation and formatting
- **`FileUtil`** - Cross-platform file I/O operations
- **`ImageUtil`** - Image processing and key generation
- **`Localizer`** - Multi-language internationalization support
- **`MyRandom`** - Enhanced random number generation
- **`DateUtil`** - Date operations for card releases

## Main Functionality

- **File Operations**: Safe reading/writing with encoding detection
- **Collections**: Performance-optimized data structures for game data
- **Text Processing**: String utilities with MTG-specific formatting
- **Storage Abstraction**: Flexible persistence layer
- **Internationalization**: Multi-language support with real-time switching
- **Image Management**: Standardized image key generation

## Architecture

```
Core Utils (Text, File, Image)
    ↓
Storage Layer (IStorage implementations)
    ↓
Collections (FCollection, specialized maps)
    ↓
Language Support (Localizer, encoding)
```

## Key Features

- **Cross-Platform**: File operations work on Windows, Mac, Linux
- **Thread-Safe**: Collections designed for concurrent access
- **Performance**: Optimized for frequent game operations
- **Extensible**: Interface-based design for custom implementations
- **Encoding Smart**: Automatic character encoding detection

## Integration Points

- Used by all other domains for basic operations
- Provides foundation for card and deck persistence
- Supports image management across the application
- Enables internationalization for global user base