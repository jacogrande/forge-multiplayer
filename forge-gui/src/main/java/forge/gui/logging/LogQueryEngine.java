package forge.gui.logging;

import forge.gui.logging.LogAnalyzer.LogEntry;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Advanced query engine for structured log files.
 * Provides SQL-like querying capabilities for JSON log entries with support for
 * filtering, sorting, aggregation, and multi-file queries.
 * 
 * Features:
 * - Complex filtering by multiple criteria
 * - Time range queries
 * - Field-based queries with type-aware comparisons
 * - Regular expression searches
 * - Multi-file queries
 * - Query result statistics and performance metrics
 * - Fluent query builder API
 */
public class LogQueryEngine {
    
    private final LogAnalyzer analyzer;
    
    public LogQueryEngine() {
        this.analyzer = new LogAnalyzer();
    }
    
    /**
     * Executes a log query and returns the results.
     * 
     * @param query The query to execute
     * @return QueryResult containing matching entries and metadata
     * @throws Exception if query execution fails
     */
    public QueryResult execute(LogQuery query) throws Exception {
        long startTime = System.currentTimeMillis();
        
        List<LogEntry> allEntries = new ArrayList<>();
        
        // Load entries from all specified files
        if (query.getFiles() != null && !query.getFiles().isEmpty()) {
            for (File file : query.getFiles()) {
                allEntries.addAll(loadEntriesFromFile(file));
            }
        } else if (query.getFile() != null) {
            allEntries.addAll(loadEntriesFromFile(query.getFile()));
        } else {
            throw new IllegalArgumentException("No files specified in query");
        }
        
        int processedEntries = allEntries.size();
        
        // Apply filters
        List<LogEntry> filteredEntries = allEntries.stream()
                .filter(createFilterPredicate(query))
                .collect(Collectors.toList());
        
        // Apply sorting
        if (query.getSortBy() != null) {
            filteredEntries.sort(createComparator(query));
        }
        
        // Apply limit
        boolean truncated = false;
        if (query.getLimit() > 0 && filteredEntries.size() > query.getLimit()) {
            filteredEntries = filteredEntries.subList(0, query.getLimit());
            truncated = true;
        }
        
        long executionTime = System.currentTimeMillis() - startTime;
        
        // Generate statistics
        Map<String, Object> statistics = generateStatistics(allEntries);
        
        return new QueryResult(filteredEntries, truncated, executionTime, 
                             processedEntries, filteredEntries.size(), statistics);
    }
    
    private List<LogEntry> loadEntriesFromFile(File file) throws IOException {
        if (!file.exists()) {
            throw new IOException("File not found: " + file.getAbsolutePath());
        }
        
        try (FileReader reader = new FileReader(file)) {
            return analyzer.parseLogStream(reader);
        }
    }
    
    private Predicate<LogEntry> createFilterPredicate(LogQuery query) {
        List<Predicate<LogEntry>> predicates = new ArrayList<>();
        
        // Level filter
        if (query.getLevel() != null) {
            predicates.add(entry -> query.getLevel().equals(entry.getLevel()));
        }
        
        // Event type filter
        if (query.getEventType() != null) {
            predicates.add(entry -> query.getEventType().equals(entry.getEventType()));
        }
        
        // Component filter
        if (query.getComponent() != null) {
            predicates.add(entry -> entry.getLogger() != null && 
                                   entry.getLogger().contains(query.getComponent()));
        }
        
        // Session ID filter
        if (query.getSessionId() != null) {
            predicates.add(entry -> query.getSessionId().equals(entry.getSessionId()));
        }
        
        // Correlation ID filter
        if (query.getCorrelationId() != null) {
            predicates.add(entry -> query.getCorrelationId().equals(entry.getCorrelationId()));
        }
        
        // Time range filter
        if (query.getStartTime() != null || query.getEndTime() != null) {
            predicates.add(entry -> {
                Instant timestamp = entry.getTimestamp();
                if (timestamp == null) return false;
                
                if (query.getStartTime() != null && timestamp.isBefore(query.getStartTime())) {
                    return false;
                }
                if (query.getEndTime() != null && !timestamp.isBefore(query.getEndTime())) {
                    return false;
                }
                return true;
            });
        }
        
        // Message contains filter
        if (query.getMessageContains() != null) {
            predicates.add(entry -> entry.getMessage() != null && 
                                   entry.getMessage().contains(query.getMessageContains()));
        }
        
        // Message regex filter
        if (query.getMessageRegex() != null) {
            Pattern pattern = Pattern.compile(query.getMessageRegex());
            predicates.add(entry -> entry.getMessage() != null && 
                                   pattern.matcher(entry.getMessage()).find());
        }
        
        // Field filters
        for (Map.Entry<String, Object> fieldFilter : query.getFieldFilters().entrySet()) {
            String fieldName = fieldFilter.getKey();
            Object expectedValue = fieldFilter.getValue();
            
            predicates.add(entry -> {
                Object actualValue = entry.getFields().get(fieldName);
                return Objects.equals(expectedValue, actualValue);
            });
        }
        
        // Field range filters
        for (LogQuery.FieldRangeFilter rangeFilter : query.getFieldRangeFilters()) {
            predicates.add(entry -> {
                Object value = entry.getFields().get(rangeFilter.getFieldName());
                if (!(value instanceof Number)) return false;
                
                double numValue = ((Number) value).doubleValue();
                return numValue >= rangeFilter.getMinValue() && numValue <= rangeFilter.getMaxValue();
            });
        }
        
        // Field exists filters
        for (String fieldName : query.getFieldExistsFilters()) {
            predicates.add(entry -> entry.getFields().containsKey(fieldName));
        }
        
        // Combine all predicates with AND logic
        return predicates.stream().reduce(Predicate::and).orElse(entry -> true);
    }
    
    private Comparator<LogEntry> createComparator(LogQuery query) {
        Comparator<LogEntry> comparator;
        
        switch (query.getSortBy()) {
            case "timestamp":
                comparator = Comparator.comparing(LogEntry::getTimestamp, 
                    Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "level":
                comparator = Comparator.comparing(LogEntry::getLevel, 
                    Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "eventType":
                comparator = Comparator.comparing(LogEntry::getEventType, 
                    Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            case "logger":
                comparator = Comparator.comparing(LogEntry::getLogger, 
                    Comparator.nullsLast(Comparator.naturalOrder()));
                break;
            default:
                // Sort by field value
                comparator = (e1, e2) -> {
                    Object v1 = e1.getFields().get(query.getSortBy());
                    Object v2 = e2.getFields().get(query.getSortBy());
                    
                    if (v1 == null && v2 == null) return 0;
                    if (v1 == null) return 1;
                    if (v2 == null) return -1;
                    
                    if (v1 instanceof Comparable && v2 instanceof Comparable) {
                        return ((Comparable) v1).compareTo(v2);
                    }
                    
                    return v1.toString().compareTo(v2.toString());
                };
                break;
        }
        
        if (query.getSortOrder() == LogQuery.SortOrder.DESC) {
            comparator = comparator.reversed();
        }
        
        return comparator;
    }
    
    private Map<String, Object> generateStatistics(List<LogEntry> entries) {
        Map<String, Object> stats = new HashMap<>();
        
        // Basic counts
        stats.put("totalEntries", (long) entries.size());
        
        // Level counts
        Map<String, Long> levelCounts = entries.stream()
                .collect(Collectors.groupingBy(
                    entry -> entry.getLevel() != null ? entry.getLevel() : "unknown",
                    Collectors.counting()));
        
        stats.put("traceCount", levelCounts.getOrDefault("TRACE", 0L));
        stats.put("debugCount", levelCounts.getOrDefault("DEBUG", 0L));
        stats.put("infoCount", levelCounts.getOrDefault("INFO", 0L));
        stats.put("warnCount", levelCounts.getOrDefault("WARN", 0L));
        stats.put("errorCount", levelCounts.getOrDefault("ERROR", 0L));
        stats.put("criticalCount", levelCounts.getOrDefault("CRITICAL", 0L));
        
        // Event type counts
        Map<String, Long> eventTypeCounts = entries.stream()
                .filter(entry -> entry.getEventType() != null)
                .collect(Collectors.groupingBy(LogEntry::getEventType, Collectors.counting()));
        
        stats.put("eventTypes", eventTypeCounts);
        
        // Time range
        Optional<Instant> minTime = entries.stream()
                .map(LogEntry::getTimestamp)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder());
        
        Optional<Instant> maxTime = entries.stream()
                .map(LogEntry::getTimestamp)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder());
        
        minTime.ifPresent(time -> stats.put("earliestTimestamp", time.toString()));
        maxTime.ifPresent(time -> stats.put("latestTimestamp", time.toString()));
        
        return stats;
    }
    
    /**
     * Represents a log query with filtering, sorting, and limiting criteria.
     */
    public static class LogQuery {
        private File file;
        private List<File> files;
        private String level;
        private String eventType;
        private String component;
        private String sessionId;
        private String correlationId;
        private Instant startTime;
        private Instant endTime;
        private String messageContains;
        private String messageRegex;
        private Map<String, Object> fieldFilters = new HashMap<>();
        private List<FieldRangeFilter> fieldRangeFilters = new ArrayList<>();
        private List<String> fieldExistsFilters = new ArrayList<>();
        private int limit = 0; // 0 means no limit
        private String sortBy;
        private SortOrder sortOrder = SortOrder.ASC;
        
        public enum SortOrder { ASC, DESC }
        
        public static Builder builder() {
            return new Builder();
        }
        
        // Getters
        public File getFile() { return file; }
        public List<File> getFiles() { return files; }
        public String getLevel() { return level; }
        public String getEventType() { return eventType; }
        public String getComponent() { return component; }
        public String getSessionId() { return sessionId; }
        public String getCorrelationId() { return correlationId; }
        public Instant getStartTime() { return startTime; }
        public Instant getEndTime() { return endTime; }
        public String getMessageContains() { return messageContains; }
        public String getMessageRegex() { return messageRegex; }
        public Map<String, Object> getFieldFilters() { return fieldFilters; }
        public List<FieldRangeFilter> getFieldRangeFilters() { return fieldRangeFilters; }
        public List<String> getFieldExistsFilters() { return fieldExistsFilters; }
        public int getLimit() { return limit; }
        public String getSortBy() { return sortBy; }
        public SortOrder getSortOrder() { return sortOrder; }
        
        public static class FieldRangeFilter {
            private final String fieldName;
            private final double minValue;
            private final double maxValue;
            
            public FieldRangeFilter(String fieldName, double minValue, double maxValue) {
                this.fieldName = fieldName;
                this.minValue = minValue;
                this.maxValue = maxValue;
            }
            
            public String getFieldName() { return fieldName; }
            public double getMinValue() { return minValue; }
            public double getMaxValue() { return maxValue; }
        }
        
        public static class Builder {
            private LogQuery query = new LogQuery();
            
            public Builder file(File file) {
                query.file = file;
                return this;
            }
            
            public Builder files(List<File> files) {
                query.files = files;
                return this;
            }
            
            public Builder level(String level) {
                query.level = level;
                return this;
            }
            
            public Builder eventType(String eventType) {
                query.eventType = eventType;
                return this;
            }
            
            public Builder component(String component) {
                query.component = component;
                return this;
            }
            
            public Builder sessionId(String sessionId) {
                query.sessionId = sessionId;
                return this;
            }
            
            public Builder correlationId(String correlationId) {
                query.correlationId = correlationId;
                return this;
            }
            
            public Builder timeRange(Instant start, Instant end) {
                query.startTime = start;
                query.endTime = end;
                return this;
            }
            
            public Builder messageContains(String text) {
                query.messageContains = text;
                return this;
            }
            
            public Builder messageRegex(String regex) {
                query.messageRegex = regex;
                return this;
            }
            
            public Builder field(String name, Object value) {
                query.fieldFilters.put(name, value);
                return this;
            }
            
            public Builder fieldRange(String name, double min, double max) {
                query.fieldRangeFilters.add(new FieldRangeFilter(name, min, max));
                return this;
            }
            
            public Builder fieldExists(String name) {
                query.fieldExistsFilters.add(name);
                return this;
            }
            
            public Builder limit(int limit) {
                query.limit = limit;
                return this;
            }
            
            public Builder sortBy(String field) {
                query.sortBy = field;
                return this;
            }
            
            public Builder sortOrder(SortOrder order) {
                query.sortOrder = order;
                return this;
            }
            
            public LogQuery build() {
                return query;
            }
        }
    }
    
    /**
     * Results of a log query execution.
     */
    public static class QueryResult {
        private final List<LogEntry> entries;
        private final boolean truncated;
        private final long executionTimeMs;
        private final int processedEntries;
        private final int matchedEntries;
        private final Map<String, Object> statistics;
        
        public QueryResult(List<LogEntry> entries, boolean truncated, long executionTimeMs,
                          int processedEntries, int matchedEntries, Map<String, Object> statistics) {
            this.entries = Collections.unmodifiableList(entries);
            this.truncated = truncated;
            this.executionTimeMs = executionTimeMs;
            this.processedEntries = processedEntries;
            this.matchedEntries = matchedEntries;
            this.statistics = Collections.unmodifiableMap(statistics);
        }
        
        public List<LogEntry> getEntries() { return entries; }
        public boolean isTruncated() { return truncated; }
        public long getExecutionTimeMs() { return executionTimeMs; }
        public int getProcessedEntries() { return processedEntries; }
        public int getMatchedEntries() { return matchedEntries; }
        public Map<String, Object> getStatistics() { return statistics; }
        
        @Override
        public String toString() {
            return String.format("QueryResult[entries=%d, truncated=%s, executionTime=%dms]",
                               entries.size(), truncated, executionTimeMs);
        }
    }
}