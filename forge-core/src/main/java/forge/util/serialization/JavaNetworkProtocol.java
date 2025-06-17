package forge.util.serialization;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Java standard serialization implementation of NetworkProtocol.
 * Used as a baseline for performance comparison with other protocols.
 * 
 * This implementation uses standard Java serialization with optional GZIP compression
 * for objects over a certain size threshold.
 */
public class JavaNetworkProtocol implements NetworkProtocol {
    
    private static final int COMPRESSION_THRESHOLD = 1024; // 1KB
    private static final byte[] GZIP_MAGIC = {(byte) 0x1f, (byte) 0x8b}; // GZIP magic number
    
    @Override
    public byte[] serialize(Object obj) throws SerializationException {
        if (obj == null) {
            throw new SerializationException("Cannot serialize null object", "Java", "serialize");
        }
        
        try {
            // First, serialize to byte array
            byte[] serializedData = serializeToBytes(obj);
            
            // Apply compression if data is large enough
            if (serializedData.length >= COMPRESSION_THRESHOLD) {
                return compressData(serializedData);
            } else {
                return serializedData;
            }
        } catch (IOException e) {
            throw new SerializationException("Failed to serialize object: " + e.getMessage(), e, "Java", "serialize");
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] data, Class<T> type) throws SerializationException {
        if (data == null || data.length == 0) {
            throw new SerializationException("Cannot deserialize null or empty data", "Java", "deserialize");
        }
        
        try {
            // Check if data is compressed and decompress if needed
            byte[] decompressedData = isCompressed(data) ? decompressData(data) : data;
            
            // Deserialize the object
            Object result = deserializeFromBytes(decompressedData);
            
            // Verify the type matches what's expected
            if (result != null && !type.isAssignableFrom(result.getClass())) {
                throw new SerializationException(
                    String.format("Deserialized object type %s does not match expected type %s", 
                        result.getClass().getName(), type.getName()),
                    "Java", "deserialize"
                );
            }
            
            return (T) result;
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Failed to deserialize object: " + e.getMessage(), e, "Java", "deserialize");
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
        return "Java Standard Serialization";
    }
    
    @Override
    public String getProtocolVersion() {
        return "1.0";
    }
    
    @Override
    public boolean supportsVersion(String version) {
        // Java serialization is compatible with version 1.0 only
        return "1.0".equals(version);
    }
    
    /**
     * Serializes an object to a byte array using standard Java serialization.
     * 
     * @param obj Object to serialize
     * @return Serialized byte array
     * @throws IOException if serialization fails
     */
    private byte[] serializeToBytes(Object obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
            oos.flush();
        }
        return baos.toByteArray();
    }
    
    /**
     * Deserializes an object from a byte array using standard Java deserialization.
     * 
     * @param data Serialized byte array
     * @return Deserialized object
     * @throws IOException if I/O fails
     * @throws ClassNotFoundException if class cannot be found
     */
    private Object deserializeFromBytes(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            return ois.readObject();
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