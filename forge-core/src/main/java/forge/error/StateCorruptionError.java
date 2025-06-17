package forge.error;

import java.util.HashMap;
import java.util.Map;

/**
 * Error thrown when game state is detected to be corrupted or invalid.
 * This is a critical error that usually requires game restart or state recovery from backup.
 */
public class StateCorruptionError extends GameStateError {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Types of state corruption.
     */
    public enum CorruptionType {
        /**
         * Player data is corrupted (life totals, counters, etc).
         */
        PLAYER_DATA("Player data corruption"),
        
        /**
         * Card data is corrupted or references invalid cards.
         */
        CARD_DATA("Card data corruption"),
        
        /**
         * Zone contents are invalid (wrong card count, null references).
         */
        ZONE_CORRUPTION("Zone corruption"),
        
        /**
         * Game rules or phase data is corrupted.
         */
        RULES_CORRUPTION("Game rules corruption"),
        
        /**
         * Stack contains invalid spells or abilities.
         */
        STACK_CORRUPTION("Stack corruption"),
        
        /**
         * General data structure corruption.
         */
        DATA_STRUCTURE("Data structure corruption");
        
        private final String description;
        
        CorruptionType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private final CorruptionType corruptionType;
    private final String affectedComponent;
    private final String diagnosticInfo;
    
    /**
     * Creates a new StateCorruptionError.
     * 
     * @param corruptionType Type of corruption detected
     * @param affectedComponent Component or subsystem affected
     * @param diagnosticInfo Additional diagnostic information
     */
    public StateCorruptionError(CorruptionType corruptionType, String affectedComponent, String diagnosticInfo) {
        this(corruptionType, affectedComponent, diagnosticInfo, null);
    }
    
    /**
     * Creates a new StateCorruptionError with cause.
     * 
     * @param corruptionType Type of corruption detected
     * @param affectedComponent Component or subsystem affected
     * @param diagnosticInfo Additional diagnostic information
     * @param cause Underlying cause
     */
    public StateCorruptionError(CorruptionType corruptionType, String affectedComponent, 
                               String diagnosticInfo, Throwable cause) {
        super(String.format("Game state corruption detected: %s in %s", 
                          corruptionType.getDescription(), affectedComponent),
              Severity.CRITICAL, false, cause, 
              createContext(corruptionType, affectedComponent, diagnosticInfo));
        this.corruptionType = corruptionType;
        this.affectedComponent = affectedComponent;
        this.diagnosticInfo = diagnosticInfo;
    }
    
    private static Map<String, Object> createContext(CorruptionType type, String component, String diagnostic) {
        Map<String, Object> context = new HashMap<>();
        context.put("corruptionType", type);
        context.put("affectedComponent", component);
        context.put("diagnosticInfo", diagnostic);
        context.put("critical", true);
        return context;
    }
    
    /**
     * Gets the type of corruption detected.
     * 
     * @return Corruption type
     */
    public CorruptionType getCorruptionType() {
        return corruptionType;
    }
    
    /**
     * Gets the affected component or subsystem.
     * 
     * @return Component name
     */
    public String getAffectedComponent() {
        return affectedComponent;
    }
    
    /**
     * Gets additional diagnostic information.
     * 
     * @return Diagnostic details
     */
    public String getDiagnosticInfo() {
        return diagnosticInfo;
    }
    
    @Override
    public String getUserMessage() {
        return "A critical error has occurred in the game. The game state is corrupted and cannot continue. " +
               "Please restart the game.";
    }
    
    @Override
    public RecoveryStrategy getRecommendedRecoveryStrategy() {
        return RecoveryStrategy.NONE; // Game must be restarted
    }
}