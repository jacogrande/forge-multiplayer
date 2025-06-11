# Forge Core Module

The `forge-core` module is the foundational layer of the Forge Magic: The Gathering implementation, providing essential data structures, services, and utilities that all other modules depend on. It handles card database management, deck construction, core data types, file I/O, internationalization, and performance-critical utilities.

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Card Database System](#card-database-system)
- [Deck Construction and Management](#deck-construction-and-management)
- [Core Data Structures](#core-data-structures)
- [Mana System](#mana-system)
- [Card Type System](#card-type-system)
- [Storage and Persistence](#storage-and-persistence)
- [Internationalization](#internationalization)
- [Utility Systems](#utility-systems)
- [Performance Considerations](#performance-considerations)
- [Integration Patterns](#integration-patterns)
- [Extending the Core](#extending-the-core)

## Overview

The Forge Core module provides the foundational infrastructure for the entire Forge project, featuring:

- **Card Database Management**: Complete MTG card database with smart art selection
- **Deck Construction**: Comprehensive deck building and validation system
- **Type-Safe Collections**: Specialized collections optimized for game data
- **Mana System**: Complete implementation of MTG's mana cost system
- **Internationalization**: Multi-language support with real-time switching
- **Storage Abstraction**: Flexible file and data storage systems
- **Performance Optimization**: Lazy loading, caching, and efficient algorithms
- **Cross-Platform Support**: Works across desktop, mobile, and web platforms

### Core Dependencies

```xml
<dependencies>
    <dependency>
        <groupId>com.google.guava</groupId>
        <artifactId>guava</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-lang3</artifactId>
    </dependency>
    <dependency>
        <groupId>commons-codec</groupId>
        <artifactId>commons-codec</artifactId>
    </dependency>
</dependencies>
```

## Architecture

The core module follows a layered architecture with clear separation of concerns:

```
┌─────────────────────────────────────┐
│         Application Layer           │
│    StaticData, GlobalFogServer      │
├─────────────────────────────────────┤
│        Business Logic Layer         │
│   CardDb, DeckManager, Localizer    │
├─────────────────────────────────────┤
│         Data Model Layer            │
│  Card, Deck, ManaCost, CardType     │
├─────────────────────────────────────┤
│        Collection Layer             │
│ FCollection, ItemPool, Specialized  │
├─────────────────────────────────────┤
│         Storage Layer               │
│  StorageBase, FileUtil, Serializers │
├─────────────────────────────────────┤
│         Utility Layer               │
│  TextUtil, MyRandom, DateUtil, etc. │
└─────────────────────────────────────┘
```

### Key Design Principles

- **Immutability**: Core data structures are immutable for thread safety
- **Lazy Loading**: Data loaded on-demand to optimize startup performance
- **Interface Segregation**: Clean contracts between components
- **Performance First**: Optimized data structures and algorithms
- **Extensibility**: Plugin points for custom content and behavior

## Card Database System

### CardDb - Central Card Database

The `CardDb` class manages the complete MTG card database:

```java
public final class CardDb implements ICardDatabase {
    private final Map<String, PaperCard> allCards;
    private final Map<String, PaperCard> uniqueCards;
    private final Multimap<String, PaperCard> variantCards;
    
    // Smart card lookup with art preferences
    public PaperCard getCard(String cardName, String setCode, int artIndex) {
        return getCardArtPreference(cardName, setCode, artIndex, false);
    }
    
    // Advanced search capabilities
    public Collection<PaperCard> getUniqueCards() { /* ... */ }
    public Collection<PaperCard> getVariants(PaperCard card) { /* ... */ }
}
```

### StaticData - Global Data Manager

Central orchestrator for all game-invariant data:

```java
public class StaticData {
    private static StaticData instance;
    
    // Core databases
    private final CardDb commonCards;
    private final CardDb variantCards;
    private final CardDb customCards;
    
    // Edition and format management
    private final QuestController questController;
    private final FormatManager formatManager;
    
    public static StaticData instance() {
        if (instance == null) {
            instance = new StaticData();
        }
        return instance;
    }
}
```

### Card Art Management

Sophisticated art preference system:

```java
public class CardArtPreference {
    // Art selection strategies
    public enum ArtPreference {
        LATEST_ART_ALL_EDITIONS,
        LATEST_ART_CORE_EXPANSIONS_REPRINT,
        ORIGINAL_ART_CORE_EXPANSIONS,
        ORIGINAL_ART_ALL_EDITIONS
    }
    
    public PaperCard getPreferredArt(String cardName) {
        // Apply user preferences and availability
        return selectOptimalArt(cardName, getCurrentPreference());
    }
}
```

### Edition Management

```java
public class CardEdition implements Comparable<CardEdition> {
    private final String code;
    private final String name;
    private final Date date;
    private final Type type;
    
    public enum Type {
        CORE, EXPANSION, REPRINT, SPECIAL, 
        MASTERS, COMMANDER, PLANECHASE, ARCHENEMY
    }
    
    // Smart edition comparison for card art selection
    @Override
    public int compareTo(CardEdition other) {
        return this.date.compareTo(other.date);
    }
}
```

## Deck Construction and Management

### Deck - Comprehensive Deck Representation

```java
public class Deck implements Iterable<Entry<DeckSection, CardPool>> {
    private final Map<DeckSection, CardPool> parts;
    private String name;
    private String description;
    private boolean isDeferred; // For performance optimization
    
    // Multi-section deck support
    public CardPool getMain() { return get(DeckSection.Main); }
    public CardPool getSideboard() { return get(DeckSection.Sideboard); }
    public CardPool getCommander() { return get(DeckSection.Commander); }
    
    // Smart card management
    public void addCard(PaperCard card, DeckSection section, int qty) {
        ensureSection(section).add(card, qty);
        harmonizeArtInDeck(); // Optimize art selection
    }
}
```

### DeckSection - Type-Safe Section Management

```java
public enum DeckSection {
    Main("Main", true, true),
    Sideboard("Sideboard", true, true),
    Commander("Commander", false, false),
    Planes("Planar Deck", false, true),
    Schemes("Scheme Deck", false, true),
    Conspiracy("Conspiracy", false, false),
    Attraction("Attraction", false, true),
    Dungeon("Dungeon", false, false);
    
    private final String displayName;
    private final boolean isPlayerOwned;
    private final boolean allowsDuplicates;
    
    public boolean validate(PaperCard card, int quantity) {
        // Section-specific validation logic
        if (!allowsDuplicates && quantity > 1) return false;
        // Additional card type validation
        return true;
    }
}
```

### CardPool - Sophisticated Card Collection

```java
public class CardPool extends ItemPool<PaperCard> {
    // Statistical analysis
    public int getCMCCount(int cmc) { /* ... */ }
    public int getColorCount(byte color) { /* ... */ }
    public double getAverageCMC() { /* ... */ }
    
    // Advanced filtering
    public CardPool getValidCards(Predicate<PaperCard> filter) { /* ... */ }
    
    // Edition analysis
    public String getPivotEdition() {
        // Find the most common edition for art harmonization
        return findMostFrequentEdition();
    }
}
```

## Core Data Structures

### FCollection - Hybrid Collection

Implements both Set and List interfaces for optimal game data handling:

```java
public class FCollection<T> implements Set<T>, List<T>, Iterable<T> {
    private final List<T> list;
    private final Set<T> set;
    
    // Maintains insertion order while ensuring uniqueness
    @Override
    public boolean add(T element) {
        if (set.add(element)) {
            list.add(element);
            return true;
        }
        return false;
    }
    
    // Thread-safe iteration
    public FCollection<T> threadSafeIterable() {
        return new FCollection<>(this);
    }
}
```

### ItemPool - Generic Pool Implementation

```java
public class ItemPool<T> implements Iterable<Entry<T, Integer>> {
    private final Map<T, Integer> items;
    
    public void add(T item, int quantity) {
        items.merge(item, quantity, Integer::sum);
    }
    
    public int count(T item) {
        return items.getOrDefault(item, 0);
    }
    
    // Efficient filtering and manipulation
    public ItemPool<T> getView(Predicate<T> filter) { /* ... */ }
}
```

### Specialized Collections

```java
// Enum-keyed multi-maps for performance
public class EnumMapOfLists<K extends Enum<K>, V> {
    private final Map<K, List<V>> map;
    
    public void add(K key, V value) {
        map.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
    }
}

// Quantity tracking maps
public class MapToAmount<T> extends HashMap<T, Integer> {
    public void addToAmount(T key, int amount) {
        merge(key, amount, Integer::sum);
    }
}
```

## Mana System

### ManaCost - Immutable Mana Cost Representation

```java
public final class ManaCost implements Comparable<ManaCost> {
    private final byte[] costs; // Efficient storage
    private final String stringValue; // Cached string representation
    
    // Cost calculations
    public int getCMC() { /* Convert to total mana value */ }
    public byte getColorProfile() { /* WUBRG color mask */ }
    public boolean canBePaidWith(byte availableColors) { /* ... */ }
    
    // Arithmetic operations
    public ManaCost add(ManaCost other) { /* ... */ }
    public ManaCost subtract(ManaCost other) { /* ... */ }
    
    // Serialization support
    @Override
    public String toString() { return stringValue; }
}
```

### ManaCostParser - Robust Parsing

```java
public class ManaCostParser {
    private static final Pattern MANA_SYMBOL = Pattern.compile("\\{([^}]+)\\}");
    
    public static ManaCost parse(String manaCostStr) {
        if (manaCostStr == null || manaCostStr.isEmpty()) {
            return ManaCost.ZERO;
        }
        
        List<ManaCostShard> shards = new ArrayList<>();
        Matcher matcher = MANA_SYMBOL.matcher(manaCostStr);
        
        while (matcher.find()) {
            String symbol = matcher.group(1);
            shards.add(parseSymbol(symbol));
        }
        
        return new ManaCost(shards);
    }
    
    private static ManaCostShard parseSymbol(String symbol) {
        // Handle complex symbols: hybrid, Phyrexian, etc.
        // Support for new symbol types as they're introduced
    }
}
```

### Mana Symbol Support

```java
public enum ManaCostShard {
    // Basic mana
    WHITE(1, ManaAtom.WHITE),
    BLUE(1, ManaAtom.BLUE),
    BLACK(1, ManaAtom.BLACK),
    RED(1, ManaAtom.RED),
    GREEN(1, ManaAtom.GREEN),
    
    // Generic and colorless
    GENERIC(1),
    COLORLESS(1, ManaAtom.COLORLESS),
    
    // Hybrid mana
    WU(1, ManaAtom.WHITE, ManaAtom.BLUE),
    WB(1, ManaAtom.WHITE, ManaAtom.BLACK),
    // ... all hybrid combinations
    
    // Phyrexian mana
    PW(1, ManaAtom.WHITE, ManaAtom.PHYREXIAN),
    PU(1, ManaAtom.BLUE, ManaAtom.PHYREXIAN);
    // ... all Phyrexian combinations
}
```

## Card Type System

### CardType - Comprehensive Type System

```java
public final class CardType {
    private final EnumSet<CoreType> coreTypes;
    private final EnumSet<Supertype> supertypes;
    private final Set<String> subtypes;
    private final String calculatedType; // Cached string
    
    // Immutable operations return new instances
    public CardType add(CoreType type) {
        EnumSet<CoreType> newTypes = EnumSet.copyOf(coreTypes);
        newTypes.add(type);
        return new CardType(newTypes, supertypes, subtypes);
    }
    
    // Type checking
    public boolean isCreature() { return coreTypes.contains(CoreType.Creature); }
    public boolean isPlaneswalker() { return coreTypes.contains(CoreType.Planeswalker); }
    public boolean hasSubtype(String subtype) { return subtypes.contains(subtype); }
    
    // Dynamic type changes for game effects
    public CardType removeType(CoreType type) { /* ... */ }
    public CardType addSubtype(String subtype) { /* ... */ }
}
```

### Type Enums

```java
public enum CoreType {
    Artifact, Creature, Enchantment, Instant, Land, 
    Planeswalker, Sorcery, Tribal, Phenomenon, Plane, 
    Scheme, Vanguard, Conspiracy, Battle, Kindred,
    Attraction, Dungeon
}

public enum Supertype {
    Basic, Legendary, Snow, World, Elite, Host, Ongoing
}
```

## Storage and Persistence

### StorageBase - Generic Storage Abstraction

```java
public abstract class StorageBase<T> implements IStorage<T> {
    protected final String name;
    protected final IItemSerializer<T> serializer;
    
    // Template method pattern
    public final void save(T item) {
        beforeSave(item);
        doSave(item, serializer.serialize(item));
        afterSave(item);
    }
    
    protected abstract void doSave(T item, String data);
    protected void beforeSave(T item) { /* Hook */ }
    protected void afterSave(T item) { /* Hook */ }
}
```

### File Storage Implementation

```java
public class StorageReaderFile<T> extends StorageBase<T> {
    private final String directory;
    private final String fileExtension;
    
    @Override
    protected void doSave(T item, String data) {
        String filename = getFilename(item);
        FileUtil.writeFile(new File(directory, filename), data);
    }
    
    @Override
    public T load(String name) {
        File file = new File(directory, name + fileExtension);
        if (file.exists()) {
            String data = FileUtil.readFileToString(file);
            return serializer.deserialize(data);
        }
        return null;
    }
}
```

### Nested Storage for Complex Data

```java
public class StorageImmediatelySerialized<T> extends StorageBase<T> {
    // Real-time persistence for critical data
    @Override
    protected void afterSave(T item) {
        // Immediate flush to disk
        sync();
    }
}
```

## Internationalization

### Localizer - Comprehensive i18n Support

```java
public class Localizer {
    private static final Map<String, String> translations = new HashMap<>();
    private static final Set<LocalizationChangeObserver> observers = new HashSet<>();
    
    // Multi-language support
    public static void setLanguage(String languageCode) {
        loadLanguageFile(languageCode);
        notifyObservers();
    }
    
    // Translation with fallback
    public static String getInstance(String key) {
        String translation = translations.get(key);
        return translation != null ? translation : key; // Fallback to key
    }
    
    // Dynamic translation updates
    public static void addTranslation(String key, String value) {
        translations.put(key, value);
        notifyObservers();
    }
    
    // Observer pattern for UI updates
    public static void addChangeObserver(LocalizationChangeObserver observer) {
        observers.add(observer);
    }
}
```

### Encoding Detection and Conversion

```java
public class EncodingUtil {
    public static String detectAndConvert(byte[] data) {
        // Try common encodings
        String[] encodings = {"UTF-8", "ISO-8859-1", "GBK", "Shift_JIS"};
        
        for (String encoding : encodings) {
            try {
                CharsetDecoder decoder = Charset.forName(encoding).newDecoder();
                decoder.onMalformedInput(CodingErrorAction.REPORT);
                decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
                
                return decoder.decode(ByteBuffer.wrap(data)).toString();
            } catch (Exception e) {
                // Try next encoding
            }
        }
        
        // Fallback to UTF-8 with replacement
        return new String(data, StandardCharsets.UTF_8);
    }
}
```

## Utility Systems

### FileUtil - Comprehensive File Operations

```java
public class FileUtil {
    // Safe file reading with encoding detection
    public static String readFileToString(File file) {
        try {
            byte[] data = Files.readAllBytes(file.toPath());
            return EncodingUtil.detectAndConvert(data);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + file, e);
        }
    }
    
    // Safe file writing with proper encoding
    public static void writeFile(File file, String content) {
        writeFile(file, content, StandardCharsets.UTF_8);
    }
    
    // URL content fetching with timeout
    public static String readUrlToString(String url, int timeoutMs) {
        // Safe HTTP/HTTPS content retrieval
        // Proper timeout handling
        // Error recovery
    }
    
    // Directory operations
    public static void ensureDirectoryExists(File directory) { /* ... */ }
    public static List<File> findFiles(File directory, String extension) { /* ... */ }
}
```

### TextUtil - String Manipulation Utilities

```java
public class TextUtil {
    // String formatting for game display
    public static String fastReplace(String source, String find, String replace) {
        // Optimized string replacement for frequent operations
    }
    
    // Number formatting
    public static String formatNumber(int number) {
        return NumberFormat.getInstance().format(number);
    }
    
    // Case-insensitive comparisons
    public static boolean containsIgnoreCase(String text, String substring) { /* ... */ }
    
    // String array utilities
    public static String join(String[] array, String delimiter) { /* ... */ }
}
```

### MyRandom - Enhanced Random Generation

```java
public class MyRandom extends Random {
    // Deterministic randomization for testing
    public static void setSeed(long seed) {
        random = new MyRandom(seed);
    }
    
    // Weighted random selection
    public static <T> T weightedRandom(Map<T, Integer> weights) {
        int totalWeight = weights.values().stream().mapToInt(Integer::intValue).sum();
        int randomValue = random.nextInt(totalWeight);
        
        int currentWeight = 0;
        for (Map.Entry<T, Integer> entry : weights.entrySet()) {
            currentWeight += entry.getValue();
            if (randomValue < currentWeight) {
                return entry.getKey();
            }
        }
        
        return null; // Should never reach here
    }
}
```

## Performance Considerations

### Memory Optimization

```java
// String interning for frequently used values
public class CardProperty {
    private static final Map<String, String> internedValues = new ConcurrentHashMap<>();
    
    public static String intern(String value) {
        return internedValues.computeIfAbsent(value, k -> k);
    }
}

// Lazy initialization of expensive operations
public class CardDb {
    private final Supplier<Map<String, PaperCard>> expensiveIndex = 
        Suppliers.memoize(this::buildExpensiveIndex);
    
    public Map<String, PaperCard> getExpensiveIndex() {
        return expensiveIndex.get();
    }
}
```

### Startup Performance

```java
// Deferred loading for large datasets
public class DeferredCardLoader {
    private final Map<String, Supplier<PaperCard>> deferredCards = new HashMap<>();
    
    public void registerDeferredCard(String name, Supplier<PaperCard> loader) {
        deferredCards.put(name, loader);
    }
    
    public PaperCard getCard(String name) {
        Supplier<PaperCard> loader = deferredCards.get(name);
        return loader != null ? loader.get() : null;
    }
}
```

### Caching Strategies

```java
// Immutable objects with cached calculations
public final class ManaCost {
    private final byte[] costs;
    private final String stringValue; // Computed once, cached forever
    private final int cmc; // Computed once, cached forever
    
    private ManaCost(byte[] costs) {
        this.costs = costs.clone();
        this.cmc = calculateCMC();
        this.stringValue = buildStringValue();
    }
}
```

## Integration Patterns

### Interface-Based Design

```java
// Clean contracts for different implementations
public interface ICardDatabase {
    PaperCard getCard(String name);
    Collection<PaperCard> getAllCards();
    boolean contains(String name);
}

public interface IStorage<T> {
    void save(T item);
    T load(String name);
    Collection<String> list();
    void delete(String name);
}
```

### Observer Pattern for Updates

```java
public interface LocalizationChangeObserver {
    void localizationChanged();
}

public interface CardDatabaseChangeObserver {
    void cardAdded(PaperCard card);
    void cardRemoved(String cardName);
    void databaseReloaded();
}
```

### Factory Pattern for Complex Objects

```java
public class DeckFactory {
    public static Deck createEmptyDeck(String name) {
        Deck deck = new Deck(name);
        // Initialize with appropriate sections
        return deck;
    }
    
    public static Deck createCommanderDeck(PaperCard commander) {
        Deck deck = createEmptyDeck("Commander Deck");
        deck.get(DeckSection.Commander).add(commander);
        return deck;
    }
}
```

## Extending the Core

### Adding New Card Properties

```java
public enum CardProperty {
    // Existing properties
    NAME, MANA_COST, TYPES, POWER, TOUGHNESS,
    
    // Add new property
    MY_CUSTOM_PROPERTY;
    
    // Update property handling
    public Object getDefaultValue() {
        switch (this) {
            case MY_CUSTOM_PROPERTY: return "default_value";
            default: return null;
        }
    }
}
```

### Custom Storage Implementations

```java
public class DatabaseStorage<T> extends StorageBase<T> {
    private final DataSource dataSource;
    
    @Override
    protected void doSave(T item, String data) {
        // Store in database instead of file system
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement(
                "INSERT OR REPLACE INTO items (name, data) VALUES (?, ?)");
            stmt.setString(1, getItemName(item));
            stmt.setString(2, data);
            stmt.executeUpdate();
        }
    }
}
```

### Custom Collection Types

```java
public class TimestampedCollection<T> extends FCollection<T> {
    private final Map<T, Long> timestamps = new HashMap<>();
    
    @Override
    public boolean add(T element) {
        boolean added = super.add(element);
        if (added) {
            timestamps.put(element, System.currentTimeMillis());
        }
        return added;
    }
    
    public List<T> getItemsAfter(long timestamp) {
        return stream()
                .filter(item -> timestamps.get(item) > timestamp)
                .collect(Collectors.toList());
    }
}
```

### Adding New Mana Symbol Types

```java
// 1. Add to ManaCostShard enum
public enum ManaCostShard {
    // ... existing shards ...
    MY_NEW_SYMBOL(1, ManaAtom.MY_NEW_TYPE);
}

// 2. Update parser
public class ManaCostParser {
    private static ManaCostShard parseSymbol(String symbol) {
        switch (symbol.toUpperCase()) {
            // ... existing cases ...
            case "MY": return ManaCostShard.MY_NEW_SYMBOL;
            default: throw new IllegalArgumentException("Unknown mana symbol: " + symbol);
        }
    }
}
```

## Best Practices for Contributors

### Code Organization

- Follow package-by-feature organization
- Keep related functionality together
- Use meaningful package and class names
- Maintain clear separation of concerns

### Performance Guidelines

- Prefer immutable objects for thread safety
- Use lazy initialization for expensive operations
- Cache calculated values when appropriate
- Choose efficient data structures for the use case

### Testing Requirements

- Unit tests for all public methods
- Performance tests for critical paths
- Integration tests for complex interactions
- Mock external dependencies appropriately

### Documentation Standards

- Comprehensive JavaDoc for public APIs
- Code comments for complex algorithms
- Update README for architectural changes
- Provide usage examples for new features

The Forge Core module serves as the solid foundation that enables the entire Forge project to function efficiently and reliably. Its well-designed architecture, performance optimizations, and extensible patterns make it an excellent example of foundational software engineering that balances immediate needs with long-term maintainability and growth.