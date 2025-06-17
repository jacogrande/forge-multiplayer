package forge.error;

import java.util.HashMap;
import java.util.Map;

/**
 * Error thrown when an invalid game state transition is attempted.
 * This includes illegal phase transitions, invalid turn sequences, or rule violations.
 */
public class StateTransitionError extends GameStateError {
    
    private static final long serialVersionUID = 1L;
    
    private final String fromState;
    private final String toState;
    private final String transitionType;
    private final String violatedRule;
    
    /**
     * Creates a new StateTransitionError.
     * 
     * @param fromState Current state
     * @param toState Attempted new state
     * @param transitionType Type of transition (phase, turn, etc)
     */
    public StateTransitionError(String fromState, String toState, String transitionType) {
        this(fromState, toState, transitionType, null, null);
    }
    
    /**
     * Creates a new StateTransitionError with violated rule.
     * 
     * @param fromState Current state
     * @param toState Attempted new state
     * @param transitionType Type of transition
     * @param violatedRule Rule that was violated
     */
    public StateTransitionError(String fromState, String toState, String transitionType, String violatedRule) {
        this(fromState, toState, transitionType, violatedRule, null);
    }
    
    /**
     * Creates a new StateTransitionError with cause.
     * 
     * @param fromState Current state
     * @param toState Attempted new state
     * @param transitionType Type of transition
     * @param violatedRule Rule that was violated
     * @param cause Underlying cause
     */
    public StateTransitionError(String fromState, String toState, String transitionType, 
                               String violatedRule, Throwable cause) {
        super(String.format("Invalid state transition: %s -> %s (%s)", 
                          fromState, toState, transitionType),
              true, cause, createContext(fromState, toState, transitionType, violatedRule));
        this.fromState = fromState;
        this.toState = toState;
        this.transitionType = transitionType;
        this.violatedRule = violatedRule;
    }
    
    private static Map<String, Object> createContext(String from, String to, String type, String rule) {
        Map<String, Object> context = new HashMap<>();
        context.put("fromState", from);
        context.put("toState", to);
        context.put("transitionType", type);
        if (rule != null) {
            context.put("violatedRule", rule);
        }
        return context;
    }
    
    /**
     * Gets the current state before transition.
     * 
     * @return Current state
     */
    public String getFromState() {
        return fromState;
    }
    
    /**
     * Gets the attempted new state.
     * 
     * @return Target state
     */
    public String getToState() {
        return toState;
    }
    
    /**
     * Gets the type of transition attempted.
     * 
     * @return Transition type
     */
    public String getTransitionType() {
        return transitionType;
    }
    
    /**
     * Gets the rule that was violated, if specified.
     * 
     * @return Violated rule or null
     */
    public String getViolatedRule() {
        return violatedRule;
    }
    
    @Override
    public String getUserMessage() {
        if (violatedRule != null) {
            return String.format("Invalid game action: %s. This violates the rule: %s", 
                               transitionType, violatedRule);
        } else {
            return String.format("Invalid game action: Cannot transition from %s to %s.", 
                               fromState, toState);
        }
    }
    
    @Override
    public RecoveryStrategy getRecommendedRecoveryStrategy() {
        return RecoveryStrategy.RESYNC; // Resync to get valid state
    }
}