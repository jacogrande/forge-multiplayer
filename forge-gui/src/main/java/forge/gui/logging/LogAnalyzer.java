package forge.gui.logging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility for parsing and analyzing structured log files from the Forge network logging system.
 * Provides methods to parse JSON log entries, filter them by various criteria, and extract
 * analytics and summaries for debugging and monitoring purposes.
 * 
 * Features:
 * - Parse structured JSON log entries
 * - Filter logs by level, event type, time range, component, etc.
 * - Extract error summaries and performance metrics
 * - Analyze connection patterns and success rates
 * - Group entries by session for correlation analysis
 */
public class LogAnalyzer {
    
    private final ObjectMapper objectMapper;
    
    public LogAnalyzer() {
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Parses a single JSON log entry string into a LogEntry object.
     * 
     * @param jsonLogLine The JSON log entry as a string
     * @return A LogEntry object containing parsed data
     * @throws LogParseException if the JSON is invalid or required fields are missing
     */
    public LogEntry parseLogEntry(String jsonLogLine) throws LogParseException {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonLogLine);
            return parseLogEntry(rootNode);
        } catch (IOException e) {
            throw new LogParseException("Failed to parse JSON log entry: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parses a stream of JSON log entries.
     * 
     * @param reader Reader providing the log data
     * @return List of successfully parsed LogEntry objects
     * @throws IOException if there's an error reading from the stream
     */
    public List<LogEntry> parseLogStream(Reader reader) throws IOException {
        List<LogEntry> entries = new ArrayList<>();
        
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            StringBuilder jsonBuffer = new StringBuilder();
            int braceDepth = 0;
            
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                
                // Handle multi-line JSON entries
                jsonBuffer.append(line);
                
                // Count braces to detect complete JSON objects
                for (char c : line.toCharArray()) {
                    if (c == '{') braceDepth++;
                    else if (c == '}') braceDepth--;
                }
                
                // When braces are balanced, we have a complete JSON object
                if (braceDepth == 0 && jsonBuffer.length() > 0) {
                    try {
                        LogEntry entry = parseLogEntry(jsonBuffer.toString());
                        entries.add(entry);
                    } catch (LogParseException e) {
                        // Skip malformed entries but continue processing
                        System.err.println("Skipping malformed log entry: " + e.getMessage());
                    }
                    jsonBuffer.setLength(0);
                }
            }
        }
        
        return entries;
    }
    
    /**
     * Filters log entries by log level.
     * 
     * @param entries List of log entries to filter
     * @param level Log level to filter by (e.g., "INFO", "ERROR")
     * @return Filtered list of entries
     */
    public List<LogEntry> filterByLevel(List<LogEntry> entries, String level) {
        return entries.stream()
                .filter(entry -> level.equals(entry.getLevel()))
                .collect(Collectors.toList());
    }
    
    /**
     * Filters log entries by event type.
     * 
     * @param entries List of log entries to filter
     * @param eventType Event type to filter by (e.g., "connection", "error")
     * @return Filtered list of entries
     */
    public List<LogEntry> filterByEventType(List<LogEntry> entries, String eventType) {
        return entries.stream()
                .filter(entry -> eventType.equals(entry.getEventType()))
                .collect(Collectors.toList());
    }
    
    /**
     * Filters log entries by time range.
     * 
     * @param entries List of log entries to filter
     * @param start Start of time range (inclusive)
     * @param end End of time range (exclusive)
     * @return Filtered list of entries
     */
    public List<LogEntry> filterByTimeRange(List<LogEntry> entries, Instant start, Instant end) {
        return entries.stream()
                .filter(entry -> {
                    Instant timestamp = entry.getTimestamp();
                    return timestamp != null && 
                           !timestamp.isBefore(start) && 
                           timestamp.isBefore(end);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Filters log entries by correlation ID.
     * 
     * @param entries List of log entries to filter
     * @param correlationId Correlation ID to filter by
     * @return Filtered list of entries
     */
    public List<LogEntry> filterByCorrelationId(List<LogEntry> entries, String correlationId) {
        return entries.stream()
                .filter(entry -> correlationId.equals(entry.getCorrelationId()))
                .collect(Collectors.toList());
    }
    
    /**
     * Filters log entries by component name.
     * 
     * @param entries List of log entries to filter
     * @param component Component name to filter by (e.g., "FGameClient")
     * @return Filtered list of entries
     */
    public List<LogEntry> filterByComponent(List<LogEntry> entries, String component) {
        return entries.stream()
                .filter(entry -> entry.getLogger() != null && 
                                entry.getLogger().contains(component))
                .collect(Collectors.toList());
    }
    
    /**
     * Searches log entries by message content.
     * 
     * @param entries List of log entries to search
     * @param searchTerm Term to search for in messages
     * @return List of entries containing the search term
     */
    public List<LogEntry> searchMessages(List<LogEntry> entries, String searchTerm) {
        return entries.stream()
                .filter(entry -> entry.getMessage() != null && 
                                entry.getMessage().contains(searchTerm))
                .collect(Collectors.toList());
    }
    
    /**
     * Extracts a summary of errors by type.
     * 
     * @param entries List of log entries to analyze
     * @return Map of error types to their occurrence counts
     */
    public Map<String, Integer> extractErrorSummary(List<LogEntry> entries) {
        Map<String, Integer> errorSummary = new HashMap<>();
        
        entries.stream()
                .filter(entry -> "ERROR".equals(entry.getLevel()))
                .forEach(entry -> {
                    Object errorType = entry.getFields().get("errorType");
                    if (errorType != null) {
                        String type = errorType.toString();
                        errorSummary.merge(type, 1, Integer::sum);
                    }
                });
        
        return errorSummary;
    }
    
    /**
     * Extracts performance metrics from log entries.
     * 
     * @param entries List of log entries to analyze
     * @return Map of metric names to their values
     */
    public Map<String, Double> extractPerformanceMetrics(List<LogEntry> entries) {
        Map<String, Double> metrics = new HashMap<>();
        
        entries.stream()
                .filter(entry -> "performance".equals(entry.getLogger()))
                .forEach(entry -> {
                    Object metric = entry.getFields().get("metric");
                    Object value = entry.getFields().get("value");
                    
                    if (metric != null && value != null) {
                        try {
                            double numericValue = Double.parseDouble(value.toString());
                            metrics.put(metric.toString(), numericValue);
                        } catch (NumberFormatException e) {
                            // Skip non-numeric metrics
                        }
                    }
                });
        
        return metrics;
    }
    
    /**
     * Groups log entries by session ID.
     * 
     * @param entries List of log entries to group
     * @return Map of session IDs to their associated log entries
     */
    public Map<String, List<LogEntry>> groupBySession(List<LogEntry> entries) {
        return entries.stream()
                .collect(Collectors.groupingBy(entry -> {
                    String sessionId = entry.getSessionId();
                    return sessionId != null ? sessionId : "null";
                }));
    }
    
    /**
     * Analyzes connection events and provides statistics.
     * 
     * @param entries List of log entries to analyze
     * @return ConnectionAnalysis object with connection statistics
     */
    public ConnectionAnalysis analyzeConnectionEvents(List<LogEntry> entries) {
        List<LogEntry> connectionEntries = filterByEventType(entries, "connection");
        
        int totalAttempts = connectionEntries.size();
        int successful = 0;
        long totalDuration = 0;
        
        for (LogEntry entry : connectionEntries) {
            Object success = entry.getFields().get("success");
            if (Boolean.TRUE.equals(success)) {
                successful++;
            }
            
            Object duration = entry.getFields().get("durationMs");
            if (duration instanceof Number) {
                totalDuration += ((Number) duration).longValue();
            }
        }
        
        double successRate = totalAttempts > 0 ? (double) successful / totalAttempts * 100.0 : 0.0;
        double averageTime = totalAttempts > 0 ? (double) totalDuration / totalAttempts : 0.0;
        
        return new ConnectionAnalysis(totalAttempts, successful, totalAttempts - successful, 
                                    successRate, averageTime);
    }
    
    private LogEntry parseLogEntry(JsonNode rootNode) throws LogParseException {
        try {
            // Extract standard fields
            String timestamp = getStringField(rootNode, "timestamp");
            String level = getStringField(rootNode, "level");
            String logger = getStringField(rootNode, "logger");
            String message = getStringField(rootNode, "message");
            String eventType = getStringField(rootNode, "eventType");
            String correlationId = getStringField(rootNode, "correlationId");
            String sessionId = getStringField(rootNode, "sessionId");
            
            // Parse timestamp
            Instant timestampInstant = null;
            if (timestamp != null) {
                try {
                    timestampInstant = Instant.parse(timestamp);
                } catch (DateTimeParseException e) {
                    throw new LogParseException("Invalid timestamp format: " + timestamp, e);
                }
            }
            
            // Extract all fields as a map
            Map<String, Object> fields = new HashMap<>();
            rootNode.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();
                
                if (!isStandardField(key)) {
                    fields.put(key, extractValue(value));
                }
            });
            
            return new LogEntry(timestampInstant, level, logger, message, eventType, 
                              correlationId, sessionId, fields);
                              
        } catch (Exception e) {
            throw new LogParseException("Failed to parse log entry: " + e.getMessage(), e);
        }
    }
    
    private String getStringField(JsonNode node, String fieldName) {
        JsonNode fieldNode = node.get(fieldName);
        return fieldNode != null && !fieldNode.isNull() ? fieldNode.asText() : null;
    }
    
    private boolean isStandardField(String fieldName) {
        return Set.of("timestamp", "level", "logger", "message", "eventType", 
                     "correlationId", "sessionId").contains(fieldName);
    }
    
    private Object extractValue(JsonNode node) {
        if (node.isNull()) {
            return null;
        } else if (node.isBoolean()) {
            return node.asBoolean();
        } else if (node.isInt()) {
            return node.asInt();
        } else if (node.isLong()) {
            return node.asLong();
        } else if (node.isDouble()) {
            return node.asDouble();
        } else {
            return node.asText();
        }
    }
    
    /**
     * Represents a parsed log entry with structured data.
     */
    public static class LogEntry {
        private final Instant timestamp;
        private final String level;
        private final String logger;
        private final String message;
        private final String eventType;
        private final String correlationId;
        private final String sessionId;
        private final Map<String, Object> fields;
        
        public LogEntry(Instant timestamp, String level, String logger, String message,
                       String eventType, String correlationId, String sessionId,
                       Map<String, Object> fields) {
            this.timestamp = timestamp;
            this.level = level;
            this.logger = logger;
            this.message = message;
            this.eventType = eventType;
            this.correlationId = correlationId;
            this.sessionId = sessionId;
            this.fields = fields != null ? new HashMap<>(fields) : new HashMap<>();
        }
        
        public Instant getTimestamp() { return timestamp; }
        public String getLevel() { return level; }
        public String getLogger() { return logger; }
        public String getMessage() { return message; }
        public String getEventType() { return eventType; }
        public String getCorrelationId() { return correlationId; }
        public String getSessionId() { return sessionId; }
        public Map<String, Object> getFields() { return new HashMap<>(fields); }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            LogEntry other = (LogEntry) obj;
            return Objects.equals(timestamp, other.timestamp) &&
                   Objects.equals(level, other.level) &&
                   Objects.equals(logger, other.logger) &&
                   Objects.equals(message, other.message) &&
                   Objects.equals(eventType, other.eventType) &&
                   Objects.equals(correlationId, other.correlationId) &&
                   Objects.equals(sessionId, other.sessionId) &&
                   Objects.equals(fields, other.fields);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(timestamp, level, logger, message, eventType, 
                              correlationId, sessionId, fields);
        }
        
        @Override
        public String toString() {
            return String.format("LogEntry[%s %s %s: %s]", 
                               timestamp, level, eventType, message);
        }
    }
    
    /**
     * Analysis results for connection events.
     */
    public static class ConnectionAnalysis {
        private final int totalAttempts;
        private final int successfulAttempts;
        private final int failedAttempts;
        private final double successRate;
        private final double averageConnectionTime;
        
        public ConnectionAnalysis(int totalAttempts, int successfulAttempts, int failedAttempts,
                                double successRate, double averageConnectionTime) {
            this.totalAttempts = totalAttempts;
            this.successfulAttempts = successfulAttempts;
            this.failedAttempts = failedAttempts;
            this.successRate = successRate;
            this.averageConnectionTime = averageConnectionTime;
        }
        
        public int getTotalAttempts() { return totalAttempts; }
        public int getSuccessfulAttempts() { return successfulAttempts; }
        public int getFailedAttempts() { return failedAttempts; }
        public double getSuccessRate() { return successRate; }
        public double getAverageConnectionTime() { return averageConnectionTime; }
        
        @Override
        public String toString() {
            return String.format("ConnectionAnalysis[attempts=%d, success=%.1f%%, avgTime=%.1fms]",
                               totalAttempts, successRate, averageConnectionTime);
        }
    }
    
    /**
     * Exception thrown when log parsing fails.
     */
    public static class LogParseException extends Exception {
        public LogParseException(String message) {
            super(message);
        }
        
        public LogParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}