package forge.util.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import forge.card.mana.ManaCost;

/**
 * Optimized Kryo serializer for ManaCost objects.
 * 
 * ManaCost objects are immutable and frequently transmitted, so this serializer
 * optimizes for minimal size and maximum speed by using ManaCost's built-in
 * serialization format.
 */
public class ManaCostSerializer extends Serializer<ManaCost> {
    
    @Override
    public void write(Kryo kryo, Output output, ManaCost manaCost) {
        if (manaCost == null) {
            output.writeString(null);
            return;
        }
        
        // Use ManaCost's built-in efficient serialization format
        String serializedForm = ManaCost.serialize(manaCost);
        output.writeString(serializedForm);
    }
    
    @Override
    public ManaCost read(Kryo kryo, Input input, Class<? extends ManaCost> type) {
        String serializedForm = input.readString();
        if (serializedForm == null) {
            return null;
        }
        
        // Use ManaCost's built-in deserialization
        return ManaCost.deserialize(serializedForm);
    }
    
    @Override
    public ManaCost copy(Kryo kryo, ManaCost original) {
        // ManaCost is immutable, so we can return the same instance
        return original;
    }
}