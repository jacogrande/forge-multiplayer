package forge.util.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.JavaSerializer;

import forge.card.CardType;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostShard;
import forge.deck.Deck;
import forge.deck.CardPool;
import forge.item.PaperCard;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Kryo-based implementation of NetworkProtocol for high-performance serialization.
 * 
 * This implementation uses Kryo 5.x for fast binary serialization with custom
 * serializers for Forge-specific objects. Includes GZIP compression for large messages.
 * 
 * Performance target: 10x faster than Java standard serialization
 * Size target: 50%+ smaller than Java standard serialization
 */
public class KryoNetworkProtocol implements NetworkProtocol {
    
    private static final int COMPRESSION_THRESHOLD = 1024; // 1KB
    private static final byte[] GZIP_MAGIC = {(byte) 0x1f, (byte) 0x8b}; // GZIP magic number
    
    // Thread-local Kryo instances for thread safety
    private final ThreadLocal<Kryo> kryoThreadLocal = new ThreadLocal<Kryo>() {
        @Override
        protected Kryo initialValue() {
            return createKryoInstance();
        }
    };
    
    @Override
    public byte[] serialize(Object obj) throws SerializationException {
        if (obj == null) {
            throw new SerializationException("Cannot serialize null object", "Kryo", "serialize");
        }
        
        try {
            Kryo kryo = kryoThreadLocal.get();
            
            // Serialize to byte array using Kryo
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (Output output = new Output(baos)) {
                kryo.writeObject(output, obj);
            }
            
            byte[] serializedData = baos.toByteArray();
            
            // Apply compression if data is large enough
            if (serializedData.length >= COMPRESSION_THRESHOLD) {
                return compressData(serializedData);
            } else {
                return serializedData;
            }
        } catch (Exception e) {
            throw new SerializationException("Failed to serialize object: " + e.getMessage(), e, "Kryo", "serialize");
        }
    }
    
    @Override
    public <T> T deserialize(byte[] data, Class<T> type) throws SerializationException {
        if (data == null || data.length == 0) {
            throw new SerializationException("Cannot deserialize null or empty data", "Kryo", "deserialize");
        }
        
        try {
            Kryo kryo = kryoThreadLocal.get();
            
            // Check if data is compressed and decompress if needed
            byte[] decompressedData = isCompressed(data) ? decompressData(data) : data;
            
            // Deserialize the object using Kryo
            try (Input input = new Input(decompressedData)) {
                T result = kryo.readObject(input, type);
                return result;
            }
        } catch (Exception e) {
            throw new SerializationException("Failed to deserialize object: " + e.getMessage(), e, "Kryo", "deserialize");
        }
    }
    
    @Override
    public boolean isCompressed(byte[] data) {
        if (data == null || data.length < 2) {
            return false;
        }
        
        // Check for GZIP magic number
        return data[0] == GZIP_MAGIC[0] && data[1] == GZIP_MAGIC[1];
    }
    
    @Override
    public String getProtocolName() {
        return "Kryo Test Protocol";
    }
    
    @Override
    public String getProtocolVersion() {
        return "1.0";
    }
    
    @Override
    public boolean supportsVersion(String version) {
        return "1.0".equals(version);
    }
    
    /**
     * Creates and configures a new Kryo instance with Forge-specific serializers.
     * 
     * @return Configured Kryo instance
     */
    private Kryo createKryoInstance() {
        Kryo kryo = new Kryo();
        
        // Configure Kryo for better performance and compatibility
        kryo.setReferences(true); // Enable object references for shared objects
        kryo.setRegistrationRequired(false); // Allow unregistered classes for flexibility
        kryo.setInstantiatorStrategy(new org.objenesis.strategy.StdInstantiatorStrategy());
        
        // Register common classes for better performance
        registerCommonClasses(kryo);
        
        // Register custom serializers for Forge-specific objects
        registerForgeSerializers(kryo);
        
        return kryo;
    }
    
    /**
     * Registers commonly used Java classes for optimized serialization.
     * 
     * @param kryo Kryo instance to configure
     */
    private void registerCommonClasses(Kryo kryo) {
        // Register basic Java types
        kryo.register(String.class);
        kryo.register(Integer.class);
        kryo.register(Long.class);
        kryo.register(Boolean.class);
        kryo.register(Double.class);
        kryo.register(Float.class);
        kryo.register(byte[].class);
        
        // Register common collection types
        kryo.register(java.util.ArrayList.class);
        kryo.register(java.util.HashMap.class);
        kryo.register(java.util.HashSet.class);
        kryo.register(java.util.TreeMap.class);
        kryo.register(java.util.TreeSet.class);
        kryo.register(java.util.LinkedHashMap.class);
        kryo.register(java.util.LinkedHashSet.class);
        kryo.register(java.util.EnumMap.class);
        kryo.register(java.util.EnumSet.class);
    }
    
    /**
     * Registers custom serializers for Forge-specific objects.
     * 
     * @param kryo Kryo instance to configure
     */
    private void registerForgeSerializers(Kryo kryo) {
        try {
            // For complex objects that require proper initialization, use Java serialization
            kryo.register(CardType.class, new JavaSerializer());
            kryo.register(Deck.class, new JavaSerializer());
            kryo.register(CardPool.class, new JavaSerializer());
            kryo.register(ManaCost.class, new JavaSerializer());
            kryo.register(PaperCard.class, new JavaSerializer());
            
            // Simple objects can use default Kryo serialization
            kryo.register(ManaCostShard.class);
            
            // For objects that might not serialize well with Kryo, fall back to Java serialization
            // This is a fallback for complex objects until custom serializers are implemented
            kryo.addDefaultSerializer(Serializable.class, JavaSerializer.class);
        } catch (Exception e) {
            // If registration fails, Kryo will use default serialization
            // This ensures the protocol is robust even if some classes aren't available
        }
    }
    
    /**
     * Compresses data using GZIP compression.
     * 
     * @param data Data to compress
     * @return Compressed data
     * @throws IOException if compression fails
     */
    private byte[] compressData(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(data);
            gzos.finish();
        }
        return baos.toByteArray();
    }
    
    /**
     * Decompresses GZIP-compressed data.
     * 
     * @param compressedData Compressed data
     * @return Decompressed data
     * @throws IOException if decompression fails
     */
    private byte[] decompressData(byte[] compressedData) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(compressedData);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try (GZIPInputStream gzis = new GZIPInputStream(bais)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = gzis.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
        }
        
        return baos.toByteArray();
    }
}