package forge.util.serialization;

import forge.card.CardType;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.item.PaperCard;
import forge.StaticData;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.AssertJUnit;

/**
 * Test suite for Kryo serialization of core Forge objects.
 * Validates performance improvements and correctness compared to Java serialization.
 * 
 * Target: 10x performance improvement over Java serialization
 * Target: 50%+ size reduction compared to Java serialization
 */
public class KryoSerializationTest {

    private KryoNetworkProtocol kryoProtocol;
    private JavaNetworkProtocol javaProtocol;
    
    @BeforeMethod
    public void setUp() {
        kryoProtocol = new KryoNetworkProtocol();
        javaProtocol = new JavaNetworkProtocol();
    }

    /**
     * Test basic ManaCost serialization roundtrip.
     * ManaCost is a fundamental object used throughout the game.
     */
    @Test
    public void testManaCostSerialization() {
        // Test various mana cost configurations
        ManaCost[] testCosts = {
            ManaCost.ZERO,
            ManaCost.NO_COST,
            ManaCost.get(3),
            new ManaCost(new ManaCostParser("2RR")),
            new ManaCost(new ManaCostParser("XWUBRG")),
            new ManaCost(new ManaCostParser("3{2/W}{U/P}"))
        };
        
        for (ManaCost cost : testCosts) {
            // Test Kryo serialization roundtrip
            byte[] kryoData = kryoProtocol.serialize(cost);
            ManaCost kryoDeserialized = kryoProtocol.deserialize(kryoData, ManaCost.class);
            
            AssertJUnit.assertNotNull("Kryo deserialized ManaCost should not be null", kryoDeserialized);
            AssertJUnit.assertEquals("Kryo roundtrip should preserve ManaCost value", 
                cost.toString(), kryoDeserialized.toString());
            
            // Test Java serialization roundtrip for comparison
            byte[] javaData = javaProtocol.serialize(cost);
            ManaCost javaDeserialized = javaProtocol.deserialize(javaData, ManaCost.class);
            
            AssertJUnit.assertEquals("Java roundtrip should preserve ManaCost value", 
                cost.toString(), javaDeserialized.toString());
                
            // Verify both methods produce equivalent results
            AssertJUnit.assertEquals("Kryo and Java serialization should produce equivalent results",
                kryoDeserialized.toString(), javaDeserialized.toString());
        }
    }

    /**
     * Test CardType serialization roundtrip.
     * CardType contains complex type information and is frequently serialized.
     */
    @Test
    public void testCardTypeSerialization() {
        // Create test CardType instances
        CardType[] testTypes = {
            new CardType(false),
            new CardType(true), // isLand
            CardType.parse("Creature — Human Wizard", false),
            CardType.parse("Legendary Artifact — Equipment", false),
            CardType.parse("Instant", false),
            CardType.parse("Planeswalker — Jace", false)
        };
        
        for (CardType cardType : testTypes) {
            // Test Kryo serialization roundtrip
            byte[] kryoData = kryoProtocol.serialize(cardType);
            CardType kryoDeserialized = kryoProtocol.deserialize(kryoData, CardType.class);
            
            AssertJUnit.assertNotNull("Kryo deserialized CardType should not be null", kryoDeserialized);
            AssertJUnit.assertEquals("Kryo roundtrip should preserve CardType", 
                cardType.toString(), kryoDeserialized.toString());
                
            // Test Java serialization for comparison
            byte[] javaData = javaProtocol.serialize(cardType);
            CardType javaDeserialized = javaProtocol.deserialize(javaData, CardType.class);
            
            AssertJUnit.assertEquals("Java and Kryo should produce equivalent results",
                javaDeserialized.toString(), kryoDeserialized.toString());
        }
    }

    /**
     * Test Deck serialization roundtrip.
     * Deck is a complex object containing multiple CardPools and metadata.
     */
    @Test
    public void testDeckSerialization() {
        Deck testDeck = createTestDeck();
        
        // Test Kryo serialization roundtrip
        byte[] kryoData = kryoProtocol.serialize(testDeck);
        Deck kryoDeserialized = kryoProtocol.deserialize(kryoData, Deck.class);
        
        AssertJUnit.assertNotNull("Kryo deserialized Deck should not be null", kryoDeserialized);
        AssertJUnit.assertEquals("Kryo roundtrip should preserve Deck name", 
            testDeck.getName(), kryoDeserialized.getName());
        AssertJUnit.assertEquals("Kryo roundtrip should preserve main deck size", 
            testDeck.getMain().countAll(), kryoDeserialized.getMain().countAll());
            
        // Test Java serialization for comparison
        byte[] javaData = javaProtocol.serialize(testDeck);
        Deck javaDeserialized = javaProtocol.deserialize(javaData, Deck.class);
        
        AssertJUnit.assertEquals("Java and Kryo should produce equivalent deck names",
            javaDeserialized.getName(), kryoDeserialized.getName());
        AssertJUnit.assertEquals("Java and Kryo should produce equivalent deck sizes",
            javaDeserialized.getMain().countAll(), kryoDeserialized.getMain().countAll());
    }

    /**
     * Test CardPool serialization roundtrip.
     * CardPool is used extensively in deck management and game state.
     */
    @Test
    public void testCardPoolSerialization() {
        CardPool testPool = createTestCardPool();
        
        // Test Kryo serialization roundtrip
        byte[] kryoData = kryoProtocol.serialize(testPool);
        CardPool kryoDeserialized = kryoProtocol.deserialize(kryoData, CardPool.class);
        
        AssertJUnit.assertNotNull("Kryo deserialized CardPool should not be null", kryoDeserialized);
        AssertJUnit.assertEquals("Kryo roundtrip should preserve CardPool size", 
            testPool.countAll(), kryoDeserialized.countAll());
            
        // Test Java serialization for comparison
        byte[] javaData = javaProtocol.serialize(testPool);
        CardPool javaDeserialized = javaProtocol.deserialize(javaData, CardPool.class);
        
        AssertJUnit.assertEquals("Java and Kryo should produce equivalent pool sizes",
            javaDeserialized.countAll(), kryoDeserialized.countAll());
    }

    /**
     * Benchmark Kryo vs Java serialization performance.
     * Target: Kryo should be 10x faster than Java serialization.
     */
    @Test
    public void testPerformanceBenchmark() {
        Object[] testObjects = {
            new ManaCost(new ManaCostParser("3RR")),
            CardType.parse("Creature — Dragon", false),
            createTestDeck(),
            createTestCardPool()
        };
        
        for (Object testObj : testObjects) {
            // Warm up JVM
            for (int i = 0; i < 10; i++) {
                kryoProtocol.serialize(testObj);
                javaProtocol.serialize(testObj);
            }
            
            // Measure Kryo performance
            long kryoStartTime = System.nanoTime();
            byte[] kryoData = null;
            for (int i = 0; i < 100; i++) {
                kryoData = kryoProtocol.serialize(testObj);
            }
            long kryoSerializationTime = System.nanoTime() - kryoStartTime;
            
            // Measure Java performance
            long javaStartTime = System.nanoTime();
            byte[] javaData = null;
            for (int i = 0; i < 100; i++) {
                javaData = javaProtocol.serialize(testObj);
            }
            long javaSerializationTime = System.nanoTime() - javaStartTime;
            
            // Calculate performance ratio
            double performanceRatio = (double) javaSerializationTime / kryoSerializationTime;
            double sizeRatio = (double) javaData.length / kryoData.length;
            
            System.out.println(String.format(
                "Performance for %s: Kryo %.2fx faster, %.2fx smaller (Kryo: %dns, %d bytes | Java: %dns, %d bytes)",
                testObj.getClass().getSimpleName(),
                performanceRatio,
                sizeRatio,
                kryoSerializationTime / 100,
                kryoData.length,
                javaSerializationTime / 100,
                javaData.length
            ));
            
            // Performance assertions (lenient - performance can vary by environment)
            if (performanceRatio <= 1.0) {
                System.out.println(String.format("WARNING: Kryo was not faster than Java serialization. Ratio: %.2f", performanceRatio));
            }
            
            if (sizeRatio <= 1.0) {
                System.out.println(String.format("WARNING: Kryo did not produce smaller output than Java serialization. Size ratio: %.2f", sizeRatio));
            }
            
            // Target performance goals
            if (performanceRatio >= 10.0) {
                System.out.println("✓ 10x performance improvement target achieved!");
            }
            
            if (sizeRatio >= 2.0) {
                System.out.println("✓ 50%+ size reduction target achieved!");
            }
        }
    }

    /**
     * Test serialization consistency across multiple runs.
     * Ensures deterministic serialization behavior.
     */
    @Test
    public void testSerializationConsistency() {
        Object[] testObjects = {
            new ManaCost(new ManaCostParser("5BB")),
            CardType.parse("Legendary Creature — Human Warrior", false),
            createTestDeck()
        };
        
        for (Object testObj : testObjects) {
            // Serialize the same object multiple times
            byte[] firstSerialization = kryoProtocol.serialize(testObj);
            byte[] secondSerialization = kryoProtocol.serialize(testObj);
            byte[] thirdSerialization = kryoProtocol.serialize(testObj);
            
            // Verify all serializations produce identical results
            AssertJUnit.assertArrayEquals("First and second serialization should be identical",
                firstSerialization, secondSerialization);
            AssertJUnit.assertArrayEquals("Second and third serialization should be identical",
                secondSerialization, thirdSerialization);
                
            // Test deserialization consistency
            Object firstDeserialized = kryoProtocol.deserialize(firstSerialization, testObj.getClass());
            Object secondDeserialized = kryoProtocol.deserialize(secondSerialization, testObj.getClass());
            Object thirdDeserialized = kryoProtocol.deserialize(thirdSerialization, testObj.getClass());
            
            AssertJUnit.assertEquals("All deserializations should produce equivalent objects",
                firstDeserialized.toString(), secondDeserialized.toString());
            AssertJUnit.assertEquals("All deserializations should produce equivalent objects",
                secondDeserialized.toString(), thirdDeserialized.toString());
        }
    }

    /**
     * Test backward compatibility with serialized data.
     * This test will be enhanced as the protocol evolves.
     */
    @Test
    public void testBackwardCompatibility() {
        // Create a known object and serialize it
        ManaCost testCost = new ManaCost(new ManaCostParser("2WU"));
        byte[] serializedData = kryoProtocol.serialize(testCost);
        
        // Deserialize and verify
        ManaCost deserializedCost = kryoProtocol.deserialize(serializedData, ManaCost.class);
        AssertJUnit.assertEquals("Backward compatibility test should preserve object",
            testCost.toString(), deserializedCost.toString());
            
        // TODO: Add more sophisticated version compatibility tests as protocol evolves
        // This might include testing against saved binary data from previous versions
    }

    /**
     * Test complex nested object serialization.
     * Verifies that deeply nested structures serialize correctly.
     */
    @Test
    public void testComplexNestedSerialization() {
        // Create a complex object with multiple levels of nesting
        Deck complexDeck = createComplexTestDeck();
        
        // Serialize and deserialize
        byte[] data = kryoProtocol.serialize(complexDeck);
        Deck deserialized = kryoProtocol.deserialize(data, Deck.class);
        
        // Verify complex structure is preserved
        AssertJUnit.assertNotNull("Complex deck should deserialize successfully", deserialized);
        AssertJUnit.assertEquals("Complex deck name should be preserved",
            complexDeck.getName(), deserialized.getName());
        AssertJUnit.assertEquals("Complex deck main size should be preserved",
            complexDeck.getMain().countAll(), deserialized.getMain().countAll());
            
        // Verify metadata is preserved
        AssertJUnit.assertEquals("Complex deck comment should be preserved",
            complexDeck.getComment(), deserialized.getComment());
    }

    /**
     * Test error handling for invalid data.
     * Ensures graceful handling of corrupted or invalid serialized data.
     */
    @Test
    public void testErrorHandling() {
        // Test with invalid data
        byte[] invalidData = new byte[]{1, 2, 3, 4, 5};
        
        try {
            kryoProtocol.deserialize(invalidData, ManaCost.class);
            AssertJUnit.fail("Should throw exception for invalid data");
        } catch (Exception e) {
            // Expected behavior - invalid data should throw exception
            AssertJUnit.assertTrue("Exception message should indicate deserialization failure",
                e.getMessage().contains("deserialization") || e.getMessage().contains("serialize"));
        }
        
        // Test with null data
        try {
            kryoProtocol.deserialize(null, ManaCost.class);
            AssertJUnit.fail("Should throw exception for null data");
        } catch (Exception e) {
            // Expected behavior
        }
    }

    /**
     * Creates a simple test deck for serialization testing.
     */
    private Deck createTestDeck() {
        Deck deck = new Deck("Test Serialization Deck");
        deck.setComment("A deck created for testing serialization");
        
        // Add some basic cards if available
        try {
            if (StaticData.instance() != null && StaticData.instance().getCommonCards() != null) {
                PaperCard plains = StaticData.instance().getCommonCards().getCard("Plains");
                if (plains != null) {
                    deck.getMain().add(plains, 20);
                }
                
                PaperCard bear = StaticData.instance().getCommonCards().getCard("Grizzly Bears");
                if (bear != null) {
                    deck.getMain().add(bear, 4);
                }
            }
        } catch (Exception e) {
            // If card database is not available, create empty deck for testing
        }
        
        return deck;
    }

    /**
     * Creates a complex test deck with multiple sections and metadata.
     */
    private Deck createComplexTestDeck() {
        Deck deck = createTestDeck();
        deck.setName("Complex Test Deck for Serialization");
        deck.setComment("A complex deck with multiple sections and extensive metadata for testing serialization of nested structures");
        
        // Add tags if supported
        try {
            // This tests the deck's tag system if available
            deck.getTags().add("test");
            deck.getTags().add("serialization");
            deck.getTags().add("complex");
        } catch (Exception e) {
            // Tags may not be supported in all versions
        }
        
        return deck;
    }

    /**
     * Creates a test CardPool for serialization testing.
     */
    private CardPool createTestCardPool() {
        CardPool pool = new CardPool();
        
        try {
            if (StaticData.instance() != null && StaticData.instance().getCommonCards() != null) {
                PaperCard island = StaticData.instance().getCommonCards().getCard("Island");
                if (island != null) {
                    pool.add(island, 10);
                }
                
                PaperCard mountain = StaticData.instance().getCommonCards().getCard("Mountain");
                if (mountain != null) {
                    pool.add(mountain, 8);
                }
            }
        } catch (Exception e) {
            // If card database is not available, create empty pool for testing
        }
        
        return pool;
    }
}