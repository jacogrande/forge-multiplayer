# Storage Domain

Flexible persistence layer providing abstract storage interfaces for all forge-core data.

## Key Classes

- **`IStorage<T>`** - Generic storage interface for data persistence
- **`StorageBase<T>`** - Abstract base implementation with template methods
- **`StorageReaderBase<T>`** - Read-only storage foundation
- **`StorageImmediatelySerialized<T>`** - Real-time persistence storage

## Storage Implementations

- **File-Based Storage**: Directory-based persistence with configurable serialization
- **Memory Storage**: In-memory implementations for testing and caching
- **Extensible Storage**: Framework for custom storage backends

## Architecture

```
IStorage<T>
    ↓
StorageBase<T> → StorageReaderBase<T>
    ↓                    ↓
File Storage      Memory Storage
```

## Key Features

- **Generic Design**: Type-safe storage for any serializable object
- **Multiple Backends**: File system, memory, and extensible implementations
- **Lazy Loading**: On-demand data loading for performance
- **Serialization**: Pluggable serialization strategies
- **Error Handling**: Robust error recovery and validation

## Serialization Support

- **Custom Serializers**: `IItemSerializer<T>` interface
- **Built-in Formats**: Text, binary, and structured data
- **Encoding Aware**: Proper character encoding handling
- **Validation**: Data integrity checking on load/save

## Use Cases

- **Card Database**: Persistent storage of card definitions
- **Deck Collections**: User deck storage and management
- **Configuration**: Application settings and preferences
- **Custom Content**: User-created cards, formats, and rules

## Integration Points

- Used by `CardDb` and `TokenDb` for persistent storage
- Supports `Deck` and `CardPool` serialization
- Foundation for configuration and user data
- Extensible for custom storage requirements