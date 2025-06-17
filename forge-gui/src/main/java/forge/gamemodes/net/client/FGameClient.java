package forge.gamemodes.net.client;

import com.google.common.collect.Lists;
import forge.game.player.PlayerView;
import forge.gamemodes.net.CompatibleObjectDecoder;
import forge.gamemodes.net.CompatibleObjectEncoder;
import forge.gamemodes.net.ReplyPool;
import forge.gamemodes.net.event.IdentifiableNetEvent;
import forge.gamemodes.net.event.LobbyUpdateEvent;
import forge.gamemodes.net.event.MessageEvent;
import forge.gamemodes.net.event.NetEvent;
import forge.gui.interfaces.IGuiGame;
import forge.gui.network.*;
import forge.interfaces.ILobbyListener;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.logging.Level;

import forge.gui.network.NetworkEventLogger;
import forge.gui.network.NetworkMetrics;

public class FGameClient implements IToServer {
    private static final Logger logger = Logger.getLogger(FGameClient.class.getName());
    private static final NetworkEventLogger networkLogger = NetworkEventLogger.forComponent("FGameClient");
    private static final NetworkMetrics metrics = NetworkMetrics.getInstance();
    
    private final IGuiGame clientGui;
    private final String hostname;
    private final Integer port;
    private final String username;
    private final String roomKey;
    private final List<ILobbyListener> lobbyListeners = Lists.newArrayList();
    private final ReplyPool replies = new ReplyPool();
    
    // Connection management
    private volatile Channel channel;
    private volatile EventLoopGroup eventLoopGroup;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicBoolean isReconnectionEnabled = new AtomicBoolean(true);
    
    // Reconnection infrastructure
    private final ReconnectionManager reconnectionManager;
    private final List<ReconnectionObserver> reconnectionObservers = Lists.newArrayList();
    
    // Session and correlation tracking
    private volatile String sessionId;
    private volatile String correlationId;
    private volatile NetworkMetrics.SessionMetrics sessionMetrics;

    public FGameClient(String username, String roomKey, IGuiGame clientGui, String hostname, int port) {
        this.clientGui = clientGui;
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.roomKey = roomKey;
        
        // Initialize session tracking
        this.sessionId = java.util.UUID.randomUUID().toString().substring(0, 8);
        this.sessionMetrics = metrics.startSession(sessionId, username);
        
        // Initialize reconnection manager
        this.reconnectionManager = new ReconnectionManager(new ClientReconnectionHandler());
        
        // Set up structured logging context
        networkLogger.setSessionContext(sessionId, username, null);
        
        logger.info("Created FGameClient for " + username + " connecting to " + hostname + ":" + port);
        networkLogger.logEvent(NetworkEventLogger.EventType.SESSION, NetworkEventLogger.Severity.INFO,
                "Client created for user {} connecting to {}:{}",
                username, hostname, port)
                .withField("hostname", hostname)
                .withField("port", port)
                .withField("username", username)
                .withField("roomKey", roomKey)
                .log();
    }

    final IGuiGame getGui() {
        return clientGui;
    }
    final ReplyPool getReplyPool() {
        return replies;
    }

    public void connect() {
        connect(false);
    }
    
    private boolean connect(boolean isReconnection) {
        if (isConnected.get() && channel != null && channel.isActive()) {
            logger.info("Already connected to " + hostname + ":" + port);
            networkLogger.logEvent(NetworkEventLogger.EventType.CONNECTION, NetworkEventLogger.Severity.INFO,
                    "Already connected to {}:{}", hostname, port)
                    .withField("hostname", hostname)
                    .withField("port", port)
                    .withField("isReconnection", isReconnection)
                    .log();
            return true;
        }
        
        long startTime = System.currentTimeMillis();
        correlationId = networkLogger.startCorrelation();
        
        try {
            // Close existing connection if any
            if (channel != null) {
                channel.close();
            }
            if (eventLoopGroup != null) {
                eventLoopGroup.shutdownGracefully();
            }
            
            eventLoopGroup = new NioEventLoopGroup();
            final Bootstrap b = new Bootstrap()
             .group(eventLoopGroup)
             .channel(NioSocketChannel.class)
             .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(final SocketChannel ch) throws Exception {
                    final ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast(
                            new CompatibleObjectEncoder(),
                            new CompatibleObjectDecoder(9766*1024, ClassResolvers.cacheDisabled(null)),
                            new MessageHandler(),
                            new LobbyUpdateHandler(),
                            new ConnectionMonitorHandler(),
                            new GameClientHandler(FGameClient.this));
                }
             });

            // Start the connection attempt.
            ChannelFuture connectFuture = b.connect(this.hostname, this.port);
            channel = connectFuture.sync().channel();
            
            long durationMs = System.currentTimeMillis() - startTime;
            isConnected.set(true);
            
            // Log successful connection
            logger.info((isReconnection ? "Reconnected" : "Connected") + " to " + hostname + ":" + port);
            networkLogger.logConnection(hostname, port, true, durationMs);
            metrics.recordConnectionAttempt(true, durationMs);
            
            // Monitor for disconnection
            final ChannelFuture closeFuture = channel.closeFuture();
            new Thread(() -> {
                try {
                    closeFuture.sync();
                } catch (final InterruptedException e) {
                    logger.log(Level.FINE, "Connection monitoring interrupted", e);
                } finally {
                    handleDisconnection();
                    if (eventLoopGroup != null) {
                        eventLoopGroup.shutdownGracefully();
                    }
                }
            }, "FGameClient-Monitor").start();
            
            return true;
            
        } catch (final InterruptedException e) {
            long durationMs = System.currentTimeMillis() - startTime;
            logger.log(Level.WARNING, "Connection interrupted", e);
            networkLogger.logConnection(hostname, port, false, durationMs);
            networkLogger.logError("CONNECTION_INTERRUPTED", e);
            metrics.recordConnectionAttempt(false, durationMs);
            metrics.recordNetworkError("INTERRUPTED");
            Thread.currentThread().interrupt();
            return false;
        } catch (final Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            logger.log(Level.WARNING, "Connection failed", e);
            networkLogger.logConnection(hostname, port, false, durationMs);
            networkLogger.logError("CONNECTION_FAILED", e);
            metrics.recordConnectionAttempt(false, durationMs);
            metrics.recordNetworkError("CONNECTION_FAILED");
            return false;
        } finally {
            if (correlationId != null) {
                networkLogger.endCorrelation(correlationId);
            }
        }
    }

    public void close() {
        close(DisconnectReason.USER_INITIATED);
    }
    
    public void close(DisconnectReason reason) {
        isConnected.set(false);
        
        // Disable auto-reconnection for user-initiated close
        if (reason == DisconnectReason.USER_INITIATED) {
            isReconnectionEnabled.set(false);
        }
        
        if (channel != null) {
            channel.close();
        }
        
        if (eventLoopGroup != null) {
            eventLoopGroup.shutdownGracefully();
        }
        
        // Shutdown reconnection manager
        if (reconnectionManager != null) {
            reconnectionManager.shutdown();
        }
        
        // End session tracking
        if (sessionMetrics != null) {
            metrics.endSession(sessionId);
        }
        
        // Clear logging context
        networkLogger.clearSessionContext();
        
        logger.info("FGameClient closed due to: " + reason.getDescription());
        networkLogger.logDisconnection(reason.getDescription(), reason != DisconnectReason.USER_INITIATED);
    }
    
    /**
     * Handles disconnection and potentially triggers reconnection.
     */
    private void handleDisconnection() {
        if (!isConnected.compareAndSet(true, false)) {
            return; // Already handled
        }
        
        logger.warning("Connection lost to " + hostname + ":" + port);
        
        // Notify lobby listeners of disconnection
        for (final ILobbyListener listener : lobbyListeners) {
            listener.close();
        }
        
        // Determine disconnect reason (simplified)
        DisconnectReason reason = determineDisconnectReason();
        
        // Attempt reconnection if enabled and appropriate
        if (isReconnectionEnabled.get() && reason.shouldAutoReconnect()) {
            logger.info("Attempting automatic reconnection due to: " + reason.getDescription());
            reconnectionManager.attemptReconnection(reason);
        } else {
            logger.info("Automatic reconnection not attempted for: " + reason.getDescription());
        }
    }
    
    /**
     * Determines the reason for disconnection based on current context.
     * This is a simplified implementation - in practice, this could be more sophisticated.
     */
    private DisconnectReason determineDisconnectReason() {
        if (!isReconnectionEnabled.get()) {
            return DisconnectReason.USER_INITIATED;
        }
        
        // For now, assume network error - could be enhanced to detect specific causes
        return DisconnectReason.NETWORK_ERROR;
    }
    
    /**
     * Enables or disables automatic reconnection.
     * 
     * @param enabled true to enable auto-reconnection
     */
    public void setReconnectionEnabled(boolean enabled) {
        isReconnectionEnabled.set(enabled);
        logger.info("Automatic reconnection " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Checks if automatic reconnection is enabled.
     * 
     * @return true if auto-reconnection is enabled
     */
    public boolean isReconnectionEnabled() {
        return isReconnectionEnabled.get();
    }
    
    /**
     * Checks if currently connected to the server.
     * 
     * @return true if connected
     */
    public boolean isConnected() {
        return isConnected.get() && channel != null && channel.isActive();
    }
    
    /**
     * Adds an observer for reconnection events.
     * 
     * @param observer The observer to add
     */
    public void addReconnectionObserver(ReconnectionObserver observer) {
        if (observer != null) {
            reconnectionObservers.add(observer);
            reconnectionManager.addObserver(observer);
        }
    }
    
    /**
     * Removes a reconnection observer.
     * 
     * @param observer The observer to remove
     */
    public void removeReconnectionObserver(ReconnectionObserver observer) {
        if (observer != null) {
            reconnectionObservers.remove(observer);
            reconnectionManager.removeObserver(observer);
        }
    }

    @Override
    public void send(final NetEvent event) {
        if (!isConnected()) {
            logger.warning("Cannot send event - not connected: " + event);
            networkLogger.logEvent(NetworkEventLogger.EventType.ERROR, NetworkEventLogger.Severity.WARN,
                    "Cannot send event - not connected: {}", event.getClass().getSimpleName())
                    .withField("eventType", event.getClass().getSimpleName())
                    .withField("connected", false)
                    .log();
            return;
        }
        
        try {
            System.out.println("Client sent " + event);
            
            // Estimate message size (rough approximation)
            int estimatedSize = event.toString().length() * 2; // Simple estimation
            
            channel.writeAndFlush(event);
            
            // Track successful message sending
            networkLogger.logMessageSent(event.getClass().getSimpleName(), estimatedSize, true);
            metrics.recordMessageSent(event.getClass().getSimpleName(), estimatedSize);
            if (sessionMetrics != null) {
                sessionMetrics.recordMessage(estimatedSize);
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to send event: " + event, e);
            networkLogger.logMessageSent(event.getClass().getSimpleName(), 0, false);
            networkLogger.logError("MESSAGE_SEND_FAILED", e);
            metrics.recordNetworkError("MESSAGE_SEND_FAILED");
            if (sessionMetrics != null) {
                sessionMetrics.recordError();
            }
        }
    }

    @Override
    public Object sendAndWait(final IdentifiableNetEvent event) throws TimeoutException {
        if (!isConnected()) {
            throw new IllegalStateException("Cannot send event - not connected");
        }
        
        replies.initialize(event.getId());
        send(event);
        
        // Wait for reply
        return replies.get(event.getId());
    }

    List<ILobbyListener> getLobbyListeners() {
        return lobbyListeners;
    }

    public void addLobbyListener(final ILobbyListener listener) {
        lobbyListeners.add(listener);
    }

    void setGameControllers(final Iterable<PlayerView> myPlayers) {
        for (final PlayerView p : myPlayers) {
            clientGui.setOriginalGameController(p, new NetGameController(this));
        }
    }

    private class MessageHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
            long startTime = System.currentTimeMillis();
            
            if (msg instanceof MessageEvent) {
                final MessageEvent event = (MessageEvent) msg;
                for (final ILobbyListener listener : lobbyListeners) {
                    listener.message(event.getSource(), event.getMessage());
                }
                
                // Log message reception
                long processingTime = System.currentTimeMillis() - startTime;
                int estimatedSize = event.getMessage().length() * 2;
                networkLogger.logMessageReceived("MessageEvent", estimatedSize, processingTime);
                metrics.recordMessageReceived("MessageEvent", estimatedSize, processingTime);
                if (sessionMetrics != null) {
                    sessionMetrics.recordMessage(estimatedSize);
                }
            }
            super.channelRead(ctx, msg);
        }
    }

    private class LobbyUpdateHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
            if (msg instanceof LobbyUpdateEvent) {
                for (final ILobbyListener listener : lobbyListeners) {
                    final LobbyUpdateEvent event = (LobbyUpdateEvent) msg;
                    listener.update(event.getState(), event.getSlot());
                }
            }
            super.channelRead(ctx, msg);
        }

        @Override
        public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
            for (final ILobbyListener listener : lobbyListeners) {
                listener.close();
            }
            super.channelInactive(ctx);
        }
    }
    
    /**
     * Handler to monitor connection status and detect disconnections.
     */
    private class ConnectionMonitorHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelInactive(final ChannelHandlerContext ctx) throws Exception {
            logger.info("Channel became inactive: " + ctx.channel());
            // handleDisconnection() will be called by the connection monitor thread
            super.channelInactive(ctx);
        }
        
        @Override
        public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) throws Exception {
            logger.log(Level.WARNING, "Connection exception", cause);
            ctx.close(); // This will trigger channelInactive
        }
    }
    
    /**
     * Implementation of ReconnectionHandler for the client.
     */
    private class ClientReconnectionHandler implements ReconnectionManager.ReconnectionHandler {
        @Override
        public CompletableFuture<Boolean> attemptConnection() {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    logger.fine("Attempting connection to " + hostname + ":" + port);
                    return connect(true);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Connection attempt failed", e);
                    return false;
                }
            });
        }
        
        @Override
        public boolean isConnected() {
            return FGameClient.this.isConnected();
        }
        
        @Override
        public void prepareForReconnection() {
            logger.fine("Preparing for reconnection");
            
            // Clear any pending replies
            // replies.clear(); // If this method exists
            
            // Reset connection state
            isConnected.set(false);
        }
        
        @Override
        public CompletableFuture<Boolean> requestGameStateSync() {
            // For client-side, we typically receive state from server
            // This would send a request to the server to sync state
            return CompletableFuture.completedFuture(true);
        }
        
        @Override
        public forge.game.Game getCurrentGame() {
            // Get the current game from the GUI
            if (clientGui != null && clientGui.getGameView() != null) {
                return clientGui.getGameView().getGame();
            }
            return null;
        }
    }
}
