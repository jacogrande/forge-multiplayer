package forge.util.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import forge.card.CardType;

/**
 * Optimized Kryo serializer for CardType objects.
 * 
 * CardType objects are frequently transmitted and contain complex type information.
 * This serializer uses CardType's string representation for efficient and safe serialization.
 * 
 * Note: We avoid using toString() during serialization since it may trigger 
 * initialization issues with partially constructed objects.
 */
public class CardTypeSerializer extends Serializer<CardType> {
    
    @Override
    public void write(Kryo kryo, Output output, CardType cardType) {
        if (cardType == null) {
            output.writeString(null);
            output.writeBoolean(false); // incomplete flag
            return;
        }
        
        try {
            // Serialize using the CardType's string representation
            output.writeString(cardType.toString());
            // Also preserve the incomplete flag if possible
            // For safety, default to false if we can't access it
            output.writeBoolean(false); 
        } catch (Exception e) {
            // If toString() fails, fall back to empty string with incomplete=true
            output.writeString("");
            output.writeBoolean(true);
        }
    }
    
    @Override
    public CardType read(Kryo kryo, Input input, Class<? extends CardType> type) {
        String typeString = input.readString();
        boolean incomplete = input.readBoolean();
        
        if (typeString == null) {
            return null;
        }
        
        // Handle empty string case
        if (typeString.isEmpty()) {
            return new CardType(incomplete);
        }
        
        try {
            // Parse the CardType from its string representation
            return CardType.parse(typeString, incomplete);
        } catch (Exception e) {
            // If parsing fails, return an empty CardType
            return new CardType(true); // Mark as incomplete if parsing failed
        }
    }
    
    @Override
    public CardType copy(Kryo kryo, CardType original) {
        if (original == null) {
            return null;
        }
        
        try {
            // Create a copy using the copy constructor
            return new CardType(original);
        } catch (Exception e) {
            // If copy fails, create empty CardType
            return new CardType(true);
        }
    }
}