package forge.error;

import java.util.HashMap;
import java.util.Map;

/**
 * Error thrown when game state synchronization between client and server fails.
 * This typically occurs when the client and server have divergent views of the game state.
 */
public class StateSyncError extends GameStateError {
    
    private static final long serialVersionUID = 1L;
    
    private final long clientStateVersion;
    private final long serverStateVersion;
    private final String checksumMismatch;
    private final int gameId;
    
    /**
     * Creates a new StateSyncError.
     * 
     * @param gameId Game identifier
     * @param clientStateVersion Client's state version
     * @param serverStateVersion Server's state version
     */
    public StateSyncError(int gameId, long clientStateVersion, long serverStateVersion) {
        this(gameId, clientStateVersion, serverStateVersion, null, null);
    }
    
    /**
     * Creates a new StateSyncError with checksum information.
     * 
     * @param gameId Game identifier
     * @param clientStateVersion Client's state version
     * @param serverStateVersion Server's state version
     * @param checksumMismatch Details about checksum mismatch
     */
    public StateSyncError(int gameId, long clientStateVersion, long serverStateVersion, String checksumMismatch) {
        this(gameId, clientStateVersion, serverStateVersion, checksumMismatch, null);
    }
    
    /**
     * Creates a new StateSyncError with cause.
     * 
     * @param gameId Game identifier
     * @param clientStateVersion Client's state version
     * @param serverStateVersion Server's state version
     * @param checksumMismatch Details about checksum mismatch
     * @param cause Underlying cause
     */
    public StateSyncError(int gameId, long clientStateVersion, long serverStateVersion, 
                         String checksumMismatch, Throwable cause) {
        super(String.format("Game state synchronization failed for game %d. " +
                          "Client version: %d, Server version: %d", 
                          gameId, clientStateVersion, serverStateVersion),
              true, cause, createContext(gameId, clientStateVersion, serverStateVersion, checksumMismatch));
        this.gameId = gameId;
        this.clientStateVersion = clientStateVersion;
        this.serverStateVersion = serverStateVersion;
        this.checksumMismatch = checksumMismatch;
    }
    
    private static Map<String, Object> createContext(int gameId, long clientVersion, long serverVersion, String checksum) {
        Map<String, Object> context = new HashMap<>();
        context.put("gameId", gameId);
        context.put("clientStateVersion", clientVersion);
        context.put("serverStateVersion", serverVersion);
        if (checksum != null) {
            context.put("checksumMismatch", checksum);
        }
        context.put("versionDiff", Math.abs(serverVersion - clientVersion));
        return context;
    }
    
    /**
     * Gets the game identifier.
     * 
     * @return Game ID
     */
    public int getGameId() {
        return gameId;
    }
    
    /**
     * Gets the client's state version.
     * 
     * @return Client state version
     */
    public long getClientStateVersion() {
        return clientStateVersion;
    }
    
    /**
     * Gets the server's state version.
     * 
     * @return Server state version
     */
    public long getServerStateVersion() {
        return serverStateVersion;
    }
    
    /**
     * Gets the checksum mismatch details if available.
     * 
     * @return Checksum mismatch details or null
     */
    public String getChecksumMismatch() {
        return checksumMismatch;
    }
    
    /**
     * Determines if the client is ahead of the server.
     * 
     * @return true if client version is greater than server version
     */
    public boolean isClientAhead() {
        return clientStateVersion > serverStateVersion;
    }
    
    /**
     * Gets the version difference between client and server.
     * 
     * @return Absolute difference in versions
     */
    public long getVersionDifference() {
        return Math.abs(serverStateVersion - clientStateVersion);
    }
    
    @Override
    public String getUserMessage() {
        return "Your game is out of sync with the server. Attempting to resynchronize...";
    }
    
    @Override
    public RecoveryStrategy getRecommendedRecoveryStrategy() {
        return RecoveryStrategy.RESYNC;
    }
}