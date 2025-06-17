package forge.util.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import forge.deck.CardPool;
import forge.item.PaperCard;

import java.util.Map;

/**
 * Optimized Kryo serializer for CardPool objects.
 * 
 * CardPool objects are collections of cards with quantities, commonly used
 * in decks and various game constructs. This serializer optimizes for
 * efficient storage of card references and quantities.
 */
public class CardPoolSerializer extends Serializer<CardPool> {
    
    @Override
    public void write(Kryo kryo, Output output, CardPool cardPool) {
        if (cardPool == null) {
            output.writeInt(-1);
            return;
        }
        
        // Count the entries first to write the size
        int size = 0;
        for (Map.Entry<PaperCard, Integer> entry : cardPool) {
            size++;
        }
        
        // Write the number of unique cards
        output.writeInt(size);
        
        // Write each card and its quantity using the iterator
        for (Map.Entry<PaperCard, Integer> entry : cardPool) {
            PaperCard card = entry.getKey();
            Integer quantity = entry.getValue();
            
            // Write the card using Kryo's standard serialization
            kryo.writeObject(output, card);
            
            // Write the quantity efficiently
            output.writeVarInt(quantity, true);
        }
    }
    
    @Override
    public CardPool read(Kryo kryo, Input input, Class<? extends CardPool> type) {
        int size = input.readInt();
        if (size == -1) {
            return null;
        }
        
        CardPool cardPool = new CardPool();
        
        // Read each card and quantity
        for (int i = 0; i < size; i++) {
            PaperCard card = kryo.readObject(input, PaperCard.class);
            int quantity = input.readVarInt(true);
            
            cardPool.add(card, quantity);
        }
        
        return cardPool;
    }
    
    @Override
    public CardPool copy(Kryo kryo, CardPool original) {
        if (original == null) {
            return null;
        }
        
        // CardPool has an efficient copy constructor
        CardPool copy = new CardPool();
        copy.addAll(original);
        return copy;
    }
}