package forge.util.serialization;

/**
 * Interface for pluggable serialization protocols in network communication.
 * Provides abstraction over different serialization mechanisms (Java, Kryo, etc.)
 * to allow performance comparison and easy protocol switching.
 */
public interface NetworkProtocol {
    
    /**
     * Serializes an object to a byte array for network transmission.
     * 
     * @param obj Object to serialize
     * @return Serialized byte array
     * @throws SerializationException if serialization fails
     */
    byte[] serialize(Object obj) throws SerializationException;
    
    /**
     * Deserializes a byte array back to an object of the specified type.
     * 
     * @param data Serialized byte array
     * @param type Expected type of the deserialized object
     * @param <T> Type parameter
     * @return Deserialized object
     * @throws SerializationException if deserialization fails
     */
    <T> T deserialize(byte[] data, Class<T> type) throws SerializationException;
    
    /**
     * Checks if the given data appears to be compressed.
     * Used to determine if decompression is needed before deserialization.
     * 
     * @param data Byte array to check
     * @return true if data appears to be compressed, false otherwise
     */
    boolean isCompressed(byte[] data);
    
    /**
     * Gets the name of this serialization protocol.
     * Used for logging and debugging purposes.
     * 
     * @return Human-readable protocol name
     */
    String getProtocolName();
    
    /**
     * Gets the protocol version.
     * Used for backward compatibility and version checking.
     * 
     * @return Protocol version string
     */
    String getProtocolVersion();
    
    /**
     * Checks if this protocol supports the specified version.
     * Used for version compatibility validation.
     * 
     * @param version Version string to check
     * @return true if version is supported, false otherwise
     */
    boolean supportsVersion(String version);
}