package forge.gui.network;

/**
 * Exception thrown when a game session has expired and cannot be recovered.
 * This typically occurs when the server has cleaned up the game state
 * after a prolonged disconnection.
 */
public class GameSessionExpiredException extends ReconnectionException {
    
    private final String sessionId;
    private final long expirationTime;
    
    public GameSessionExpiredException(String sessionId, long expirationTime) {
        super(String.format("Game session %s expired at %d", sessionId, expirationTime), 
              DisconnectReason.GAME_ERROR);
        this.sessionId = sessionId;
        this.expirationTime = expirationTime;
    }
    
    public GameSessionExpiredException(String sessionId, long expirationTime, Throwable cause) {
        super(String.format("Game session %s expired at %d", sessionId, expirationTime), 
              DisconnectReason.GAME_ERROR, cause);
        this.sessionId = sessionId;
        this.expirationTime = expirationTime;
    }
    
    /**
     * Gets the ID of the expired session.
     * 
     * @return The session ID
     */
    public String getSessionId() {
        return sessionId;
    }
    
    /**
     * Gets the time when the session expired.
     * 
     * @return Expiration timestamp
     */
    public long getExpirationTime() {
        return expirationTime;
    }
    
    @Override
    public boolean shouldRetry() {
        return false; // Cannot retry with expired session
    }
}