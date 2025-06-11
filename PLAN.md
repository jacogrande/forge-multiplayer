# LAN Multiplayer Support Implementation Plan

## Executive Summary

This document outlines the technical plan for implementing comprehensive LAN multiplayer support in MTG Forge, including real-time player movement, competitive gameplay, and collaborative drafting. The implementation will leverage Forge's existing networking foundation while adding robust state synchronization, security measures, and enhanced user experience features.

## Table of Contents

- [Current State Analysis](#current-state-analysis)
- [Technical Approach Evaluation](#technical-approach-evaluation)
- [Recommended Architecture](#recommended-architecture)
- [Implementation Phases](#implementation-phases)
- [Detailed Technical Specifications](#detailed-technical-specifications)
- [Risk Analysis and Mitigation](#risk-analysis-and-mitigation)
- [Testing Strategy](#testing-strategy)
- [Success Criteria](#success-criteria)

## Current State Analysis

### Existing Infrastructure Strengths

**Networking Foundation:**
- Netty 4.1.115.Final already integrated for network communication
- Basic client-server architecture with `FServerManager` and `FGameClient`
- Event-driven messaging system via `NetEvent` classes
- UPnP support for automatic port forwarding

**Game Architecture:**
- Clean separation between game logic (`forge-game`) and UI (`forge-gui-*`)
- Abstract `PlayerController` system supporting Human, AI, and extensible types
- Comprehensive event system using Guava EventBus
- Robust game state management with `GameView` objects for safe UI representation
- Thread-safe design with proper EDT marshalling

**Real-time Capabilities:**
- Adventure mode with sprite-based movement system (`PlayerSprite`)
- 60 FPS render loop with delta-time animation
- Collision detection and smooth interpolated movement
- Existing position tracking and sprite management

**Draft System:**
- Complete `BoosterDraft` implementation supporting 8 players
- Pack passing logic with alternating directions
- AI integration for non-human players
- Hybrid support for mixed human/AI drafts
- Draft state persistence and management

### Current Limitations

**Network Implementation:**
- Limited to basic message passing
- Java serialization (slow and verbose)
- No state synchronization or consistency checking
- Missing security and anti-cheat measures
- No support for reconnection or error recovery

**Multiplayer Gaps:**
- No remote player sprite synchronization
- No support for remote human players in drafts (currently AI-only)
- Missing lobby and session management
- No spectator mode or replay functionality
- No hybrid human/AI draft support over network

## Technical Approach Evaluation

### Approach 1: Pure Peer-to-Peer

**Architecture:** Direct client-to-client communication with consensus mechanisms.

**Pros:**
- No single point of failure
- Lower average latency
- Distributed resource usage

**Cons:**
- Complex hidden information management
- Difficult consensus for MTG rules
- Vulnerable to cheating
- Complex state synchronization
- Network topology management overhead

**Verdict:** ❌ Rejected - Too complex for MTG's hidden information requirements

### Approach 2: Enhanced Client-Server (Recommended)

**Architecture:** Authoritative server with lightweight clients using efficient protocols.

**Pros:**
- Single source of truth for game state
- Natural fit for MTG's turn-based nature
- Easier to implement security measures
- Builds on existing Netty infrastructure
- Simpler development and debugging

**Cons:**
- Server hosting requirements
- Single point of failure
- Slight latency for non-host players

**Verdict:** ✅ Selected - Best balance of complexity, security, and functionality

### Approach 3: Hybrid Mesh Network

**Architecture:** Peer-to-peer for movement, client-server for game rules.

**Pros:**
- Optimized for different data types
- Lower latency for movement
- Authoritative game rules

**Cons:**
- Complex dual-protocol management
- Increased implementation complexity
- Difficult to maintain consistency

**Verdict:** ❌ Rejected - Complexity outweighs benefits for LAN use case

## Recommended Architecture

### High-Level Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Client A      │────▶│  Server/Host    │◀────│   Client B      │
│                 │     │                 │     │                 │
│ PlayerController│     │ Authoritative   │     │ PlayerController│
│ RemoteProxy     │     │ Game State      │     │ RemoteProxy     │
│                 │     │                 │     │                 │
│ Local UI        │     │ Security Layer  │     │ Local UI        │
│ Sprite Renderer │     │ State Manager   │     │ Sprite Renderer │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

### Key Components

**Enhanced Server (`forge-game`):**
- `MultiplayerGameServer`: Authoritative game state manager
- `RemotePlayerManager`: Track and synchronize remote player states
- `SecureGameState`: Filter hidden information per player
- `NetworkEventDispatcher`: Efficient event broadcasting
- `ReconnectionManager`: Handle client disconnections gracefully

**Enhanced Client (`forge-gui`):**
- `PlayerControllerRemote`: Proxy for remote player actions
- `NetworkGameView`: Synchronized view of remote game state
- `RemotePlayerRenderer`: Display other players' sprites and actions
- `LobbyManager`: Pre-game session management
- `NetworkDraftController`: Synchronized draft participation

**Shared Protocol (`forge-core`):**
- `NetworkProtocol`: Efficient binary message format
- `StateSync`: Delta-based state synchronization
- `SecurityProtocol`: Encrypted communication and validation
- `MessageTypes`: Comprehensive message type definitions

### Network Protocol Design

**Message Categories:**
1. **Connection Management**: Handshake, heartbeat, disconnect
2. **Lobby Operations**: Player ready, game start, settings sync
3. **Game State**: Full sync, delta updates, checksums
4. **Player Actions**: Spell casting, targeting, phase actions
5. **Real-time Updates**: Movement, animations, UI feedback
6. **Draft Operations**: Pack passing, pick synchronization
7. **Administrative**: Chat, spectator mode, replay data

**State Synchronization Strategy:**
- Full state transfer on initial connection
- Delta-compressed updates during gameplay
- Periodic checksum validation (every 30 seconds)
- Automatic resync on consistency failures
- Client prediction with server validation

## Implementation Phases

### Phase 1: Foundation Enhancement (Weeks 1-4)

**Objectives:**
- Replace Java serialization with Kryo for 10x performance improvement
- Implement robust connection management with auto-reconnect
- Create secure state filtering system
- Add comprehensive error handling and logging

**Key Deliverables:**

1. **Enhanced Serialization System**
```java
public class OptimizedNetworkProtocol {
    private static final Kryo kryo = new Kryo();
    
    public byte[] serialize(Object obj) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Output output = new Output(stream);
        kryo.writeObject(output, obj);
        output.close();
        return stream.toByteArray();
    }
    
    public <T> T deserialize(byte[] data, Class<T> type) {
        Input input = new Input(new ByteArrayInputStream(data));
        return kryo.readObject(input, type);
    }
}
```

2. **Secure Game State Manager**
```java
public class SecureGameState {
    private final Game authoritativeGame;
    
    public GameView getPlayerView(int playerIndex) {
        GameView view = new GameView(authoritativeGame);
        // Filter hidden information
        view.filterHandsExcept(playerIndex);
        view.filterLibraryExcept(playerIndex, true); // Keep revealed cards
        view.filterFaceDownCards(playerIndex);
        return view;
    }
    
    public boolean validatePlayerAction(PlayerAction action, int playerIndex) {
        // Verify player can see targeted cards
        // Check turn/priority restrictions
        // Validate against current game state
        return SecurityValidator.isActionValid(action, playerIndex, authoritativeGame);
    }
}
```

3. **Connection Resilience System**
```java
public class ReconnectionManager {
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final long RECONNECT_DELAY_MS = 2000;
    
    public void handleClientDisconnect(RemoteClient client) {
        // Pause game for non-active players
        if (!isActivePlayer(client)) {
            pauseGameForPlayer(client);
        }
        
        // Start reconnection timer
        scheduleReconnectionTimeout(client, 60_000); // 60 second timeout
    }
    
    public void handleReconnection(RemoteClient client) {
        // Send full game state
        GameView playerView = secureState.getPlayerView(client.getPlayerIndex());
        client.send(new FullStateSyncMessage(playerView));
        
        // Resume game
        resumeGameForPlayer(client);
    }
}
```

### Phase 2: Core Multiplayer Gameplay (Weeks 5-8)

**Objectives:**
- Implement `PlayerControllerRemote` for network players
- Create real-time state synchronization
- Add turn-based multiplayer game support
- Implement spectator mode

**Key Deliverables:**

1. **Remote Player Controller**
```java
public class PlayerControllerRemote extends PlayerController {
    private final NetworkClient networkClient;
    private final CompletableFuture<SpellAbility> pendingSpellChoice;
    
    @Override
    public SpellAbility chooseSpellAbilityToPlay() {
        // Send request to remote player
        networkClient.send(new RequestSpellChoiceMessage());
        
        // Wait for response with timeout
        try {
            return pendingSpellChoice.get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            // Auto-pass priority on timeout
            return null;
        }
    }
    
    @Override
    public List<Card> chooseCardsToDiscardFrom(List<Card> options) {
        RequestCardChoiceMessage request = new RequestCardChoiceMessage(
            options.stream().map(Card::getId).collect(toList())
        );
        networkClient.send(request);
        return waitForCardChoiceResponse();
    }
}
```

2. **Game State Synchronization**
```java
public class GameStateSynchronizer {
    private long lastSyncVersion = 0;
    private final Map<Integer, GameStateChecksum> playerChecksums = new HashMap<>();
    
    public void broadcastGameUpdate(GameEvent event) {
        StateDelta delta = StateDelta.fromEvent(event);
        
        for (RemoteClient client : activeClients) {
            GameView playerView = filterViewForPlayer(delta, client.getPlayerIndex());
            client.send(new DeltaUpdateMessage(delta.version, playerView));
        }
        
        lastSyncVersion = delta.version;
    }
    
    public void validateGameStateConsistency() {
        GameStateChecksum serverChecksum = GameStateChecksum.compute(authoritativeGame);
        
        for (RemoteClient client : activeClients) {
            client.send(new ChecksumValidationMessage(serverChecksum));
        }
        
        // Check for mismatches and trigger resync if needed
        scheduleMismatchDetection();
    }
}
```

3. **Multiplayer Game Setup**
```java
public class MultiplayerGameServer extends FServerManager {
    private final Map<String, RegisteredPlayer> lobbyPlayers = new ConcurrentHashMap<>();
    private volatile GameLobby currentLobby;
    
    public void createLobby(String hostName, GameFormat format) {
        currentLobby = new GameLobby(hostName, format);
        currentLobby.addPlayer(createLocalPlayer(hostName));
        broadcastLobbyUpdate();
    }
    
    public void handlePlayerJoin(RemoteClient client, String playerName) {
        if (currentLobby == null || currentLobby.isFull()) {
            client.send(new JoinRejectedMessage("Lobby full or unavailable"));
            return;
        }
        
        RegisteredPlayer player = new RegisteredPlayer(playerName)
            .setController(new PlayerControllerRemote(client));
        
        currentLobby.addPlayer(player);
        broadcastLobbyUpdate();
    }
    
    private void broadcastLobbyUpdate() {
        LobbyUpdateMessage update = new LobbyUpdateMessage(currentLobby.getState());
        for (RemoteClient client : connectedClients) {
            client.send(update);
        }
    }
}
```

### Phase 3: Real-time Movement System (Weeks 9-10)

**Objectives:**
- Synchronize player sprite positions across clients
- Implement smooth movement interpolation
- Add collision detection for multiplayer scenarios
- Create adventure mode multiplayer support

**Key Deliverables:**

1. **Network Player Sprite System**
```java
public class NetworkPlayerSprite extends PlayerSprite {
    private Vector2 serverPosition;
    private Vector2 clientPosition;
    private long lastUpdateTime;
    private static final float INTERPOLATION_SPEED = 5.0f;
    
    public void updateFromNetwork(Vector2 newPosition, long timestamp) {
        this.serverPosition = newPosition;
        this.lastUpdateTime = timestamp;
    }
    
    @Override
    public void update(float deltaTime) {
        if (isRemotePlayer()) {
            // Interpolate toward server position
            clientPosition = clientPosition.lerp(serverPosition, 
                INTERPOLATION_SPEED * deltaTime);
            setPosition(clientPosition);
        } else {
            // Normal local player update
            super.update(deltaTime);
            
            // Broadcast position if changed significantly
            if (positionChangedSignificantly()) {
                broadcastPosition();
            }
        }
    }
    
    private void broadcastPosition() {
        PlayerMovementMessage message = new PlayerMovementMessage(
            playerId, getPosition(), System.currentTimeMillis()
        );
        networkClient.send(message);
    }
}
```

2. **Movement Synchronization**
```java
public class MovementSynchronizer {
    private static final float MIN_POSITION_DELTA = 2.0f; // pixels
    private static final long UPDATE_INTERVAL_MS = 50; // 20 FPS for movement
    
    private final Map<Integer, PlayerMovementState> playerStates = new HashMap<>();
    private long lastBroadcast = 0;
    
    public void handlePlayerMovement(int playerId, Vector2 newPosition) {
        PlayerMovementState state = playerStates.get(playerId);
        
        if (state == null || state.position.dst(newPosition) > MIN_POSITION_DELTA) {
            state = new PlayerMovementState(newPosition, System.currentTimeMillis());
            playerStates.put(playerId, state);
            
            // Throttle broadcasts to avoid spam
            if (System.currentTimeMillis() - lastBroadcast > UPDATE_INTERVAL_MS) {
                broadcastMovementUpdate(playerId, state);
                lastBroadcast = System.currentTimeMillis();
            }
        }
    }
    
    private void broadcastMovementUpdate(int playerId, PlayerMovementState state) {
        PlayerMovementMessage message = new PlayerMovementMessage(
            playerId, state.position, state.timestamp
        );
        
        for (RemoteClient client : clients.values()) {
            if (client.getPlayerIndex() != playerId) {
                client.send(message);
            }
        }
    }
}
```

### Phase 4: Multiplayer Draft System (Weeks 11-12)

**Objectives:**
- Support hybrid drafts with both remote human players and AI players
- Synchronize pack contents and pick timing across all participants
- Implement draft lobby with player ready states and AI slot configuration
- Add draft spectator functionality
- Maintain seamless experience regardless of human/AI mix

**Key Deliverables:**

1. **Hybrid Network Draft Manager**
```java
public class NetworkDraftManager extends BoosterDraft {
    private final Map<Integer, RemoteClient> humanPlayers = new HashMap<>();
    private final Map<Integer, PlayerControllerAi> aiPlayers = new HashMap<>();
    private final Map<Integer, DraftPack> currentPacks = new HashMap<>();
    private volatile boolean waitingForPicks = false;
    
    @Override
    public void startDraft() {
        // Verify all human players are connected, AI players are always ready
        if (!allHumanPlayersReady()) {
            broadcastMessage("Waiting for all human players to be ready...");
            return;
        }
        
        // Generate synchronized packs for all participants
        generateInitialPacks();
        
        // Start first pick round
        startPickRound();
    }
    
    public void addAiPlayer(int seat, PlayerControllerAi aiController) {
        aiPlayers.put(seat, aiController);
        // AI players don't need network synchronization
        broadcastLobbyUpdate();
    }
    
    public void addHumanPlayer(int seat, RemoteClient client) {
        humanPlayers.put(seat, client);
        broadcastLobbyUpdate();
    }
    
    private void startPickRound() {
        waitingForPicks = true;
        Set<Integer> pendingPicks = new HashSet<>();
        
        // Send pick requests to human players
        for (Map.Entry<Integer, RemoteClient> entry : humanPlayers.entrySet()) {
            int seat = entry.getKey();
            RemoteClient client = entry.getValue();
            DraftPack pack = currentPacks.get(seat);
            
            DraftPickRequestMessage request = new DraftPickRequestMessage(
                pack.getContents(), getRemainingTime()
            );
            client.send(request);
            pendingPicks.add(seat);
        }
        
        // Process AI picks immediately (but still synchronously)
        for (Map.Entry<Integer, PlayerControllerAi> entry : aiPlayers.entrySet()) {
            int seat = entry.getKey();
            PlayerControllerAi ai = entry.getValue();
            DraftPack pack = currentPacks.get(seat);
            
            // AI makes pick instantly but we wait for human players
            PaperCard aiPick = ai.chooseCardFromDraftPack(pack);
            recordPick(seat, aiPick);
        }
        
        // Start pick timer (only affects human players)
        schedulePickTimeout(pendingPicks);
    }
    
    public void handlePickReceived(int playerIndex, String cardName) {
        if (!waitingForPicks || !humanPlayers.containsKey(playerIndex)) {
            return; // Pick received too late or from non-human player
        }
        
        DraftPack pack = currentPacks.get(playerIndex);
        PaperCard picked = pack.pickCard(cardName);
        
        if (picked == null) {
            // Invalid pick - request again
            requestPickAgain(playerIndex, "Invalid card selection");
            return;
        }
        
        // Record human pick
        recordPick(playerIndex, picked);
        
        // Check if all human picks received (AI picks already processed)
        if (allHumanPicksReceived()) {
            processPacks();
        }
    }
    
    private boolean allHumanPicksReceived() {
        // Check if all human players have made their picks
        for (int seat : humanPlayers.keySet()) {
            if (!hasPickForSeat(seat)) {
                return false;
            }
        }
        return true;
    }
    
    private void processPacks() {
        waitingForPicks = false;
        
        // Pass packs to next players
        passPacksToNextPlayers();
        
        // Check if round is complete
        if (allPacksEmpty()) {
            startNextBooster();
        } else {
            startPickRound();
        }
    }
}
```

2. **Hybrid Draft Lobby System**
```java
public class DraftLobby {
    private final List<DraftParticipant> participants = new ArrayList<>(8);
    private final DraftSettings settings;
    private volatile DraftState state = DraftState.WAITING_FOR_PLAYERS;
    
    public enum DraftState {
        WAITING_FOR_PLAYERS, ALL_READY, DRAFTING, COMPLETE
    }
    
    public enum ParticipantType {
        HUMAN, AI_EASY, AI_MEDIUM, AI_HARD
    }
    
    public static class DraftParticipant {
        private final String name;
        private final ParticipantType type;
        private final RemoteClient client; // null for AI
        private boolean ready;
        
        public DraftParticipant(String name, ParticipantType type, RemoteClient client) {
            this.name = name;
            this.type = type;
            this.client = client;
            this.ready = (type != ParticipantType.HUMAN); // AI always ready
        }
    }
    
    public void addHumanPlayer(RemoteClient client, String playerName) {
        if (participants.size() >= 8) {
            client.send(new DraftJoinRejectedMessage("Draft full"));
            return;
        }
        
        DraftParticipant participant = new DraftParticipant(playerName, ParticipantType.HUMAN, client);
        participants.add(participant);
        
        broadcastLobbyUpdate();
        checkReadyToStart();
    }
    
    public void addAiPlayer(String aiName, ParticipantType aiType) {
        if (participants.size() >= 8 || aiType == ParticipantType.HUMAN) {
            return; // Invalid operation
        }
        
        DraftParticipant aiParticipant = new DraftParticipant(aiName, aiType, null);
        participants.add(aiParticipant);
        
        broadcastLobbyUpdate();
        checkReadyToStart();
    }
    
    public void fillRemainingWithAI(ParticipantType defaultAiType) {
        while (participants.size() < 8) {
            String aiName = "AI Player " + (participants.size() + 1);
            addAiPlayer(aiName, defaultAiType);
        }
    }
    
    public void handlePlayerReady(RemoteClient client, boolean ready) {
        DraftParticipant participant = findParticipant(client);
        if (participant != null && participant.type == ParticipantType.HUMAN) {
            participant.setReady(ready);
            broadcastLobbyUpdate();
            checkReadyToStart();
        }
    }
    
    private void checkReadyToStart() {
        if (allHumanPlayersReady() && participants.size() >= 2) {
            // Can start with any number of participants (2-8)
            // Missing slots will be filled with AI if desired
            if (participants.size() < 8 && settings.isAutoFillWithAI()) {
                fillRemainingWithAI(settings.getDefaultAiDifficulty());
            }
            startDraft();
        }
    }
    
    private boolean allHumanPlayersReady() {
        return participants.stream()
            .filter(p -> p.type == ParticipantType.HUMAN)
            .allMatch(p -> p.ready);
    }
    
    private void broadcastLobbyUpdate() {
        DraftLobbyUpdateMessage update = new DraftLobbyUpdateMessage(
            participants.stream().map(DraftParticipant::toInfo).collect(toList()),
            state,
            settings
        );
        
        // Only send to human participants
        for (DraftParticipant participant : participants) {
            if (participant.type == ParticipantType.HUMAN && participant.client != null) {
                participant.client.send(update);
            }
        }
    }
}
```

### Phase 5: Polish and Optimization (Weeks 13-14)

**Objectives:**
- Implement chat system for multiplayer games
- Add replay functionality for multiplayer matches
- Optimize network performance and bandwidth usage
- Add advanced lobby features (game settings, player kicks, etc.)

**Key Deliverables:**

1. **Chat System**
```java
public class MultiplayerChatManager {
    private final List<ChatMessage> chatHistory = new ArrayList<>();
    private final Map<Integer, Boolean> playerMuteStates = new HashMap<>();
    
    public void handleChatMessage(RemoteClient sender, String message) {
        // Validate message (length, profanity filter, rate limiting)
        if (!isValidMessage(sender, message)) {
            return;
        }
        
        ChatMessage chatMessage = new ChatMessage(
            sender.getPlayerName(),
            message,
            System.currentTimeMillis(),
            ChatMessage.Type.PLAYER
        );
        
        chatHistory.add(chatMessage);
        
        // Broadcast to all players except muted ones
        broadcastChatMessage(chatMessage, sender);
    }
    
    public void sendSystemMessage(String message) {
        ChatMessage systemMessage = new ChatMessage(
            "System",
            message,
            System.currentTimeMillis(),
            ChatMessage.Type.SYSTEM
        );
        
        chatHistory.add(systemMessage);
        broadcastChatMessage(systemMessage, null);
    }
}
```

2. **Replay System**
```java
public class MultiplayerReplayManager {
    private final List<GameEvent> eventLog = new ArrayList<>();
    private final GameReplayMetadata metadata;
    
    public void recordEvent(GameEvent event) {
        // Record all game events with timestamps
        TimestampedEvent timestamped = new TimestampedEvent(
            event,
            System.currentTimeMillis(),
            getCurrentGameState().getStateVersion()
        );
        
        eventLog.add(timestamped);
    }
    
    public void saveReplay(String filename) {
        GameReplay replay = new GameReplay(
            metadata,
            eventLog,
            getPlayerProfiles(),
            getDeckLists()
        );
        
        // Compress and save replay file
        try (FileOutputStream fos = new FileOutputStream(filename + ".forge-replay")) {
            GZIPOutputStream gzip = new GZIPOutputStream(fos);
            ObjectOutputStream oos = new ObjectOutputStream(gzip);
            oos.writeObject(replay);
            oos.close();
        }
    }
    
    public void shareReplay(GameReplay replay) {
        // Send replay to all players
        for (RemoteClient client : connectedClients) {
            client.send(new ReplayShareMessage(replay));
        }
    }
}
```

3. **Performance Optimization**
```java
public class NetworkOptimizer {
    private static final int MESSAGE_BATCH_SIZE = 10;
    private static final long BATCH_TIMEOUT_MS = 16; // ~60 FPS
    
    private final Queue<NetworkMessage> messageQueue = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService batchExecutor;
    
    public void queueMessage(NetworkMessage message) {
        messageQueue.offer(message);
        
        // Trigger immediate send for high-priority messages
        if (message.getPriority() == Priority.IMMEDIATE) {
            flushMessageQueue();
        }
    }
    
    private void flushMessageQueue() {
        List<NetworkMessage> batch = new ArrayList<>();
        
        // Collect up to BATCH_SIZE messages
        for (int i = 0; i < MESSAGE_BATCH_SIZE && !messageQueue.isEmpty(); i++) {
            batch.add(messageQueue.poll());
        }
        
        if (!batch.isEmpty()) {
            // Compress and send batch
            BatchMessage batchMessage = new BatchMessage(batch);
            sendToClients(batchMessage);
        }
    }
    
    // Auto-flush every 16ms to maintain 60 FPS responsiveness
    private void startBatchTimer() {
        batchExecutor.scheduleAtFixedRate(
            this::flushMessageQueue,
            BATCH_TIMEOUT_MS,
            BATCH_TIMEOUT_MS,
            TimeUnit.MILLISECONDS
        );
    }
}
```

## Detailed Technical Specifications

### Network Protocol Specification

**Message Format:**
```
Header (8 bytes):
[Message Type][Length][Checksum][Flags]
   4 bytes    2 bytes  2 bytes  8 bits

Payload (Variable):
[Serialized Data using Kryo]
```

**Compression:**
- Messages > 1KB: GZIP compression
- Batch messages: Always compressed
- Movement updates: No compression (frequent, small)

**Security:**
- Optional TLS encryption for sensitive data
- Message integrity verification with checksums
- Action validation on server side
- Rate limiting: 100 messages/second per client

### Performance Targets

**Latency:**
- Game actions: < 100ms end-to-end
- Movement updates: < 50ms
- Chat messages: < 200ms

**Bandwidth:**
- Typical gameplay: < 10 KB/s per player
- Peak (complex game states): < 50 KB/s per player
- Movement tracking: < 2 KB/s per player

**Scalability:**
- Support up to 8 players per game
- Support up to 4 concurrent games per server
- Memory usage: < 500MB for full 8-player game

### File Structure Changes

**New Modules:**
```
forge-network/
├── src/main/java/forge/network/
│   ├── client/
│   │   ├── NetworkGameClient.java
│   │   ├── RemotePlayerController.java
│   │   └── ClientStateManager.java
│   ├── server/
│   │   ├── MultiplayerGameServer.java
│   │   ├── GameSessionManager.java
│   │   └── SecurityValidator.java
│   ├── protocol/
│   │   ├── NetworkProtocol.java
│   │   ├── MessageTypes.java
│   │   └── StateSync.java
│   └── shared/
│       ├── NetworkMessage.java
│       ├── GameStateChecksum.java
│       └── PlayerMovementState.java
```

**Enhanced Existing Modules:**

`forge-game/`:
- Enhanced `PlayerController` with network awareness
- New `MultiplayerGame` class extending `Game`
- Updated `BoosterDraft` for network support

`forge-gui/`:
- New `MultiplayerLobbyScreen`
- Enhanced `GameView` with network synchronization
- Updated sprite rendering for remote players

## Risk Analysis and Mitigation

### High-Risk Areas

**1. Network Latency and Reliability**
- **Risk:** Poor user experience due to lag or disconnections
- **Mitigation:** 
  - Client-side prediction for UI responsiveness
  - Automatic reconnection with state recovery
  - Graceful degradation for high-latency connections
  - Local caching of game state

**2. Game State Desynchronization**
- **Risk:** Players see different game states, leading to invalid moves
- **Mitigation:**
  - Periodic checksum validation
  - Authoritative server validation
  - Automatic resync on mismatch detection
  - Transaction-based action processing

**3. Security and Cheating**
- **Risk:** Players exploiting network communication to cheat
- **Mitigation:**
  - Server-side validation of all actions
  - Hidden information filtering
  - Action rate limiting
  - Encrypted communication for sensitive data

**4. Performance and Scalability**
- **Risk:** Poor performance with multiple players or complex game states
- **Mitigation:**
  - Delta-based state synchronization
  - Message batching and compression
  - Efficient serialization (Kryo instead of Java)
  - Performance monitoring and alerting

### Medium-Risk Areas

**5. Cross-Platform Compatibility**
- **Risk:** Network features may not work consistently across platforms
- **Mitigation:**
  - Thorough testing on all supported platforms
  - Use of platform-agnostic networking libraries
  - Standardized protocol implementation

**6. User Experience Complexity**
- **Risk:** Multiplayer features may be too complex for average users
- **Mitigation:**
  - Intuitive lobby and connection interfaces
  - Clear error messages and help documentation
  - Progressive feature disclosure

## Testing Strategy

### Unit Testing
```java
@Test
public class NetworkProtocolTest {
    @Test
    public void testMessageSerialization() {
        PlayerActionMessage original = new PlayerActionMessage(/*...*/);
        byte[] serialized = protocol.serialize(original);
        PlayerActionMessage deserialized = protocol.deserialize(serialized, PlayerActionMessage.class);
        assertEquals(original, deserialized);
    }
    
    @Test
    public void testStateSync() {
        GameState initial = createTestGameState();
        GameState modified = modifyGameState(initial);
        StateDelta delta = StateDelta.between(initial, modified);
        GameState reconstructed = initial.applyDelta(delta);
        assertEquals(modified, reconstructed);
    }
}
```

### Integration Testing
- Network connection establishment and teardown
- Multi-client game session management
- Hybrid draft synchronization with mixed human and AI players
- AI behavior consistency between local and network drafts
- Reconnection and state recovery scenarios

### Performance Testing
- Bandwidth usage measurement under various scenarios
- Latency testing with simulated network conditions
- Memory usage profiling for long-running games
- Stress testing with maximum player counts

### User Acceptance Testing
- Lobby creation and joining workflows
- Complete multiplayer game sessions
- Draft tournaments with multiple participants
- Error recovery and user feedback scenarios

## Success Criteria

### Functional Requirements
- ✅ Players can host and join LAN games through lobby system
- ✅ Real-time sprite movement synchronization with < 50ms latency
- ✅ Complete MTG games playable with remote opponents
- ✅ Hybrid multiplayer draft support with both human players and AI, including pack passing synchronization
- ✅ Spectator mode for observing games
- ✅ Chat system for player communication
- ✅ Graceful handling of network disconnections

### Performance Requirements
- ✅ Support 8 simultaneous players with < 100ms action latency
- ✅ Bandwidth usage < 10 KB/s per player during normal gameplay
- ✅ Memory usage increase < 200MB for multiplayer features
- ✅ 99% uptime for game sessions under normal network conditions

### Quality Requirements
- ✅ Zero game state desynchronization in testing
- ✅ Comprehensive error handling with user-friendly messages
- ✅ Cross-platform compatibility (Windows, Mac, Linux)
- ✅ Backward compatibility with single-player functionality

### User Experience Requirements
- ✅ Intuitive lobby interface requiring < 30 seconds to join games
- ✅ Clear visual indicators for network status and player actions
- ✅ Seamless transition between single-player and multiplayer modes
- ✅ Replay functionality for multiplayer games

This comprehensive plan provides a roadmap for implementing robust LAN multiplayer support in MTG Forge while maintaining the high quality and performance standards of the existing codebase. The phased approach allows for iterative development and testing, ensuring each component is solid before building the next layer of functionality.