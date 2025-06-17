package forge.gui.network;

/**
 * Observer interface for receiving detailed state transition notifications.
 * Provides more detailed information than ConnectionStateObserver.
 */
public interface StateTransitionObserver {
    
    /**
     * Called when a state transition occurs.
     * 
     * @param transition The detailed transition information
     */
    void onStateTransition(StateTransition transition);
}