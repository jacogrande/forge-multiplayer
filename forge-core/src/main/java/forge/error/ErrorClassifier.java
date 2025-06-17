package forge.error;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Utility class for classifying exceptions into appropriate NetworkError types.
 * This class maps various exception types to the corresponding NetworkError hierarchy.
 */
public class ErrorClassifier {
    
    private static final Logger logger = Logger.getLogger(ErrorClassifier.class.getName());
    
    /**
     * Classifies a generic exception into a NetworkError.
     * 
     * @param exception The exception to classify
     * @return NetworkError representing the classified exception
     */
    public static NetworkError classify(Throwable exception) {
        return classify(exception, null);
    }
    
    /**
     * Classifies a generic exception into a NetworkError with additional context.
     * 
     * @param exception The exception to classify
     * @param contextProvider Function that provides additional context based on the exception
     * @return NetworkError representing the classified exception
     */
    public static NetworkError classify(Throwable exception, Function<Throwable, String> contextProvider) {
        if (exception == null) {
            return new UnknownApplicationError("Null exception received", null);
        }
        
        // If it's already a NetworkError, return as-is
        if (exception instanceof NetworkError) {
            return (NetworkError) exception;
        }
        
        String context = contextProvider != null ? contextProvider.apply(exception) : null;
        
        // Connection-related exceptions
        if (exception instanceof ConnectException) {
            return classifyConnectionException((ConnectException) exception, context);
        }
        
        if (exception instanceof SocketTimeoutException) {
            return classifyTimeoutException((SocketTimeoutException) exception, context);
        }
        
        if (exception instanceof TimeoutException) {
            return classifyTimeoutException((TimeoutException) exception, context);
        }
        
        // Serialization-related exceptions
        if (isSerializationException(exception)) {
            return classifySerializationException(exception, context);
        }
        
        // Security-related exceptions
        if (isSecurityException(exception)) {
            return classifySecurityException(exception, context);
        }
        
        // Resource-related exceptions
        if (isResourceException(exception)) {
            return classifyResourceException(exception, context);
        }
        
        // Configuration-related exceptions
        if (isConfigurationException(exception)) {
            return classifyConfigurationException(exception, context);
        }
        
        // Game state exceptions (custom logic based on message/stack trace)
        Optional<NetworkError> gameStateError = classifyGameStateException(exception, context);
        if (gameStateError.isPresent()) {
            return gameStateError.get();
        }
        
        // Protocol exceptions (custom logic based on message/stack trace)
        Optional<NetworkError> protocolError = classifyProtocolException(exception, context);
        if (protocolError.isPresent()) {
            return protocolError.get();
        }
        
        // Default: Unknown application error
        return new UnknownApplicationError(
            String.format("Unclassified exception: %s", exception.getMessage()),
            exception
        );
    }
    
    /**
     * Classifies connection exceptions.
     */
    private static NetworkError classifyConnectionException(ConnectException exception, String context) {
        String message = exception.getMessage();
        if (message != null) {
            if (message.toLowerCase().contains("refused")) {
                return new ConnectionRefusedError("localhost", 0, exception);
            } else if (message.toLowerCase().contains("timeout")) {
                return new ConnectionTimeoutError("localhost", 0, 30000, exception);
            }
        }
        
        // Default connection error
        return new ConnectionRefusedError("unknown", 0, exception);
    }
    
    /**
     * Classifies timeout exceptions.
     */
    private static NetworkError classifyTimeoutException(Exception exception, String context) {
        // SocketTimeoutException is always connection-related
        if (exception instanceof java.net.SocketTimeoutException || 
            (context != null && context.contains("connection"))) {
            return new ConnectionTimeoutError("unknown", 0, 30000, exception);
        }
        
        // Generic application timeout
        return new UnknownApplicationError("Operation timed out", exception);
    }
    
    /**
     * Classifies serialization-related exceptions.
     */
    private static NetworkError classifySerializationException(Throwable exception, String context) {
        String message = exception.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            if (lowerMessage.contains("corrupt") || lowerMessage.contains("invalid data")) {
                return new MalformedMessageError("unknown", "Data corruption detected", -1, -1, exception);
            } else if (lowerMessage.contains("version")) {
                return new UnknownMessageError("unknown", "unknown", -1, exception);
            }
        }
        
        // Generic serialization error (enhance existing class)
        if (exception instanceof forge.util.serialization.SerializationException) {
            forge.util.serialization.SerializationException se = 
                (forge.util.serialization.SerializationException) exception;
            return new MalformedMessageError(se.getProtocol(), se.getOperation(), -1, -1, exception);
        }
        
        return new MalformedMessageError("unknown", exception.getMessage(), -1, -1, exception);
    }
    
    /**
     * Classifies security-related exceptions.
     */
    private static NetworkError classifySecurityException(Throwable exception, String context) {
        String message = exception.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            if (lowerMessage.contains("authentication") || lowerMessage.contains("login")) {
                return new AuthenticationError(
                    AuthenticationError.Reason.INVALID_CREDENTIALS, 
                    null, exception
                );
            } else if (lowerMessage.contains("authorization") || lowerMessage.contains("permission")) {
                return new AuthorizationError("unknown action", "unknown resource", null, exception);
            }
        }
        
        // Default to validation error
        return new ValidationError(
            ValidationError.ValidationType.DATA_INTEGRITY, 
            -1, 
            exception.getMessage(), 
            exception
        );
    }
    
    /**
     * Classifies resource-related exceptions.
     */
    private static NetworkError classifyResourceException(Throwable exception, String context) {
        String message = exception.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            if (lowerMessage.contains("memory") || lowerMessage.contains("heap")) {
                return new ResourceError(ResourceError.ResourceType.MEMORY, "JVM heap", -1, -1, exception);
            } else if (lowerMessage.contains("disk") || lowerMessage.contains("space")) {
                return new ResourceError(ResourceError.ResourceType.DISK_SPACE, "file system", -1, -1, exception);
            } else if (lowerMessage.contains("connection") && lowerMessage.contains("limit")) {
                return new ResourceError(ResourceError.ResourceType.CONNECTION_LIMIT, "network connections", -1, -1, exception);
            }
        }
        
        return new ResourceError(ResourceError.ResourceType.CPU, "system resources", -1, -1, exception);
    }
    
    /**
     * Classifies configuration-related exceptions.
     */
    private static NetworkError classifyConfigurationException(Throwable exception, String context) {
        return new ConfigurationError(
            "unknown", 
            null, 
            null, 
            context, 
            exception.getMessage(), 
            exception
        );
    }
    
    /**
     * Attempts to classify game state exceptions based on message content.
     */
    private static Optional<NetworkError> classifyGameStateException(Throwable exception, String context) {
        String message = exception.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            if (lowerMessage.contains("sync") || lowerMessage.contains("synchroniz")) {
                return Optional.of(new StateSyncError(0, 0, 0, message, exception));
            } else if (lowerMessage.contains("corrupt") && 
                      (lowerMessage.contains("game") || lowerMessage.contains("state"))) {
                return Optional.of(new StateCorruptionError(
                    StateCorruptionError.CorruptionType.DATA_STRUCTURE,
                    "game state",
                    message,
                    exception
                ));
            } else if (lowerMessage.contains("transition") || lowerMessage.contains("phase")) {
                return Optional.of(new StateTransitionError(
                    "unknown",
                    "unknown", 
                    "state transition",
                    message,
                    exception
                ));
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Attempts to classify protocol exceptions based on message content.
     */
    private static Optional<NetworkError> classifyProtocolException(Throwable exception, String context) {
        String message = exception.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            if (lowerMessage.contains("unknown message") || lowerMessage.contains("unsupported")) {
                return Optional.of(new UnknownMessageError("unknown", "unknown", -1, exception));
            } else if (lowerMessage.contains("malformed") || lowerMessage.contains("parse")) {
                return Optional.of(new MalformedMessageError("unknown", message, -1, -1, exception));
            } else if (lowerMessage.contains("sequence") || lowerMessage.contains("order")) {
                return Optional.of(new MessageSequenceError(
                    MessageSequenceError.SequenceErrorType.OUT_OF_ORDER,
                    "unknown",
                    0, 0,
                    exception
                ));
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Determines if an exception is serialization-related.
     */
    private static boolean isSerializationException(Throwable exception) {
        return exception instanceof forge.util.serialization.SerializationException ||
               exception.getClass().getName().contains("Serialization") ||
               exception.getClass().getName().contains("JSON") ||
               exception.getClass().getName().contains("XML") ||
               (exception.getMessage() != null && 
                exception.getMessage().toLowerCase().contains("serializ"));
    }
    
    /**
     * Determines if an exception is security-related.
     */
    private static boolean isSecurityException(Throwable exception) {
        return exception instanceof SecurityException ||
               exception.getClass().getName().contains("Security") ||
               exception.getClass().getName().contains("Auth") ||
               (exception.getMessage() != null && 
                (exception.getMessage().toLowerCase().contains("security") ||
                 exception.getMessage().toLowerCase().contains("auth")));
    }
    
    /**
     * Determines if an exception is resource-related.
     */
    private static boolean isResourceException(Throwable exception) {
        return exception instanceof OutOfMemoryError ||
               exception.getClass().getName().contains("Resource") ||
               (exception.getMessage() != null && 
                (exception.getMessage().toLowerCase().contains("memory") ||
                 exception.getMessage().toLowerCase().contains("resource") ||
                 exception.getMessage().toLowerCase().contains("limit")));
    }
    
    /**
     * Determines if an exception is configuration-related.
     */
    private static boolean isConfigurationException(Throwable exception) {
        return exception.getClass().getName().contains("Config") ||
               (exception.getMessage() != null && 
                exception.getMessage().toLowerCase().contains("config"));
    }
    
    /**
     * Default application error for unclassified exceptions.
     */
    private static class UnknownApplicationError extends ApplicationError {
        private static final long serialVersionUID = 1L;
        
        UnknownApplicationError(String message, Throwable cause) {
            super(message, true, cause);
        }
        
        @Override
        public String getUserMessage() {
            return "An unexpected error occurred. Please try again.";
        }
    }
}