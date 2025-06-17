package forge.util.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import forge.deck.Deck;
import forge.deck.CardPool;
import forge.deck.DeckSection;

import java.util.Map;

/**
 * Optimized Kryo serializer for Deck objects.
 * 
 * Deck objects are complex containers with multiple sections (main, sideboard, etc.)
 * and metadata. This serializer optimizes for the common case where most sections
 * are empty or small.
 */
public class DeckSerializer extends Serializer<Deck> {
    
    @Override
    public void write(Kryo kryo, Output output, Deck deck) {
        if (deck == null) {
            output.writeString(null);
            return;
        }
        
        // Write basic metadata
        output.writeString(deck.getName());
        output.writeString(deck.getComment()); // Description/comment
        
        // Write deck sections efficiently
        // Count non-empty sections first
        int nonEmptySections = 0;
        for (Map.Entry<DeckSection, CardPool> entry : deck) {
            if (entry.getValue() != null && entry.getValue().countAll() > 0) {
                nonEmptySections++;
            }
        }
        
        output.writeInt(nonEmptySections);
        
        // Write only non-empty sections
        for (Map.Entry<DeckSection, CardPool> entry : deck) {
            CardPool pool = entry.getValue();
            if (pool != null && pool.countAll() > 0) {
                // Write section type
                kryo.writeObject(output, entry.getKey());
                
                // Write the CardPool
                kryo.writeObject(output, pool);
            }
        }
        
        // Write tags if present
        if (deck.getTags().isEmpty()) {
            output.writeInt(0);
        } else {
            output.writeInt(deck.getTags().size());
            for (String tag : deck.getTags()) {
                output.writeString(tag);
            }
        }
    }
    
    @Override
    public Deck read(Kryo kryo, Input input, Class<? extends Deck> type) {
        String name = input.readString();
        if (name == null) {
            return null;
        }
        
        String comment = input.readString();
        
        // Create deck with name
        Deck deck = new Deck(name);
        if (comment != null) {
            deck.setComment(comment);
        }
        
        // Read deck sections
        int sectionCount = input.readInt();
        for (int i = 0; i < sectionCount; i++) {
            DeckSection section = kryo.readObject(input, DeckSection.class);
            CardPool pool = kryo.readObject(input, CardPool.class);
            
            // Add cards to the appropriate section
            if (pool != null) {
                deck.get(section).addAll(pool);
            }
        }
        
        // Read tags
        int tagCount = input.readInt();
        for (int i = 0; i < tagCount; i++) {
            String tag = input.readString();
            deck.getTags().add(tag);
        }
        
        return deck;
    }
    
    @Override
    public Deck copy(Kryo kryo, Deck original) {
        if (original == null) {
            return null;
        }
        
        // Create a copy using Deck's copy constructor pattern
        Deck copy = new Deck(original.getName());
        copy.setComment(original.getComment());
        
        // Copy all sections
        for (Map.Entry<DeckSection, CardPool> entry : original) {
            if (entry.getValue() != null && entry.getValue().countAll() > 0) {
                copy.get(entry.getKey()).addAll(entry.getValue());
            }
        }
        
        // Copy tags
        copy.getTags().addAll(original.getTags());
        
        return copy;
    }
}