package forge.gui.logging;

import forge.gui.logging.LogQueryEngine.LogQuery;
import forge.gui.logging.LogQueryEngine.QueryResult;
import forge.gui.logging.LogAnalyzer.LogEntry;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import static org.testng.Assert.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * TDD tests for LogQueryEngine.
 * These tests define the expected behavior for advanced log querying capabilities.
 */
public class LogQueryEngineTest {
    
    private LogQueryEngine queryEngine;
    private File tempLogFile;
    private String sampleLogData;
    
    @BeforeMethod
    public void setUp() throws IOException {
        queryEngine = new LogQueryEngine();
        
        // Create sample log data
        sampleLogData = 
            "{\n" +
            "  \"timestamp\": \"2025-01-15T10:30:00.000Z\",\n" +
            "  \"level\": \"INFO\",\n" +
            "  \"logger\": \"forge.gamemodes.net.FGameClient\",\n" +
            "  \"message\": \"Connection established to localhost:7777\",\n" +
            "  \"eventType\": \"connection\",\n" +
            "  \"host\": \"localhost\",\n" +
            "  \"port\": 7777,\n" +
            "  \"success\": true,\n" +
            "  \"durationMs\": 1500,\n" +
            "  \"correlationId\": \"abc123\",\n" +
            "  \"sessionId\": \"session-1\"\n" +
            "}\n" +
            "{\n" +
            "  \"timestamp\": \"2025-01-15T10:31:00.000Z\",\n" +
            "  \"level\": \"ERROR\",\n" +
            "  \"logger\": \"forge.gamemodes.net.FServerManager\",\n" +
            "  \"message\": \"Authentication failed for user testuser\",\n" +
            "  \"eventType\": \"security\",\n" +
            "  \"errorType\": \"AUTHENTICATION\",\n" +
            "  \"username\": \"testuser\",\n" +
            "  \"correlationId\": \"def456\",\n" +
            "  \"sessionId\": \"session-2\"\n" +
            "}\n" +
            "{\n" +
            "  \"timestamp\": \"2025-01-15T10:32:00.000Z\",\n" +
            "  \"level\": \"INFO\",\n" +
            "  \"logger\": \"forge.gamemodes.net.FGameClient\",\n" +
            "  \"message\": \"Game state synchronized successfully\",\n" +
            "  \"eventType\": \"game_state_sync\",\n" +
            "  \"operation\": \"RESTORE\",\n" +
            "  \"stateSizeBytes\": 65536,\n" +
            "  \"correlationId\": \"abc123\",\n" +
            "  \"sessionId\": \"session-1\"\n" +
            "}\n";
        
        // Create temporary log file
        tempLogFile = File.createTempFile("test-log", ".json");
        tempLogFile.deleteOnExit();
        
        try (FileWriter writer = new FileWriter(tempLogFile)) {
            writer.write(sampleLogData);
        }
    }
    
    @Test
    public void testBasicQuery() throws Exception {
        LogQuery query = LogQuery.builder()
            .file(tempLogFile)
            .level("INFO")
            .build();
        
        QueryResult result = queryEngine.execute(query);
        
        assertNotNull(result);
        assertEquals(2, result.getEntries().size());
        assertTrue(result.getEntries().stream().allMatch(e -> "INFO".equals(e.getLevel())));
    }
    
    @Test
    public void testQueryByEventType() throws Exception {
        LogQuery query = LogQuery.builder()
            .file(tempLogFile)
            .eventType("connection")
            .build();
        
        QueryResult result = queryEngine.execute(query);
        
        assertEquals(1, result.getEntries().size());
        assertEquals("connection", result.getEntries().get(0).getEventType());
    }
    
    @Test
    public void testQueryByTimeRange() throws Exception {
        Instant start = Instant.parse("2025-01-15T10:30:30.000Z");
        Instant end = Instant.parse("2025-01-15T10:31:30.000Z");
        
        LogQuery query = LogQuery.builder()
            .file(tempLogFile)
            .timeRange(start, end)
            .build();
        
        QueryResult result = queryEngine.execute(query);
        
        assertEquals(1, result.getEntries().size());
        assertEquals("security", result.getEntries().get(0).getEventType());
    }
    
    @Test
    public void testQueryByCorrelationId() throws Exception {
        LogQuery query = LogQuery.builder()
            .file(tempLogFile)
            .correlationId("abc123")
            .build();
        
        QueryResult result = queryEngine.execute(query);
        
        assertEquals(2, result.getEntries().size());
        assertTrue(result.getEntries().stream()
                 .allMatch(e -> "abc123".equals(e.getCorrelationId())));
    }
    
    @Test
    public void testQueryByComponent() throws Exception {
        LogQuery query = LogQuery.builder()
            .file(tempLogFile)
            .component("FGameClient")
            .build();
        
        QueryResult result = queryEngine.execute(query);
        
        assertEquals(2, result.getEntries().size());
        assertTrue(result.getEntries().stream()
                 .allMatch(e -> e.getLogger().contains("FGameClient")));
    }
    
    @Test
    public void testQueryBySessionId() throws Exception {
        LogQuery query = LogQuery.builder()
            .file(tempLogFile)
            .sessionId("session-1")
            .build();
        
        QueryResult result = queryEngine.execute(query);
        
        assertEquals(2, result.getEntries().size());
        assertTrue(result.getEntries().stream()
                 .allMatch(e -> "session-1".equals(e.getSessionId())));
    }
    
    @Test
    public void testMessageSearch() throws Exception {
        LogQuery query = LogQuery.builder()
            .file(tempLogFile)
            .messageContains("Authentication")
            .build();
        
        QueryResult result = queryEngine.execute(query);
        
        assertEquals(1, result.getEntries().size());
        assertTrue(result.getEntries().get(0).getMessage().contains("Authentication"));
    }
    
    @Test
    public void testFieldQuery() throws Exception {
        LogQuery query = LogQuery.builder()
            .file(tempLogFile)
            .field("success", true)
            .build();
        
        QueryResult result = queryEngine.execute(query);
        
        assertEquals(1, result.getEntries().size());
        assertEquals(Boolean.TRUE, result.getEntries().get(0).getFields().get("success"));
    }
    
    @Test
    public void testNumericFieldRange() throws Exception {
        LogQuery query = LogQuery.builder()
            .file(tempLogFile)
            .fieldRange("port", 7000, 8000)
            .build();
        
        QueryResult result = queryEngine.execute(query);
        
        assertEquals(1, result.getEntries().size());
        assertEquals(7777, result.getEntries().get(0).getFields().get("port"));
    }
    
    @Test
    public void testCombinedQuery() throws Exception {
        LogQuery query = LogQuery.builder()
            .file(tempLogFile)
            .level("INFO")
            .component("FGameClient")
            .sessionId("session-1")
            .build();
        
        QueryResult result = queryEngine.execute(query);
        
        assertEquals(2, result.getEntries().size());
        assertTrue(result.getEntries().stream()
                 .allMatch(e -> "INFO".equals(e.getLevel()) && 
                              e.getLogger().contains("FGameClient") &&
                              "session-1".equals(e.getSessionId())));
    }
    
    @Test
    public void testQueryWithLimit() throws Exception {
        LogQuery query = LogQuery.builder()
            .file(tempLogFile)
            .limit(2)
            .build();
        
        QueryResult result = queryEngine.execute(query);
        
        assertEquals(2, result.getEntries().size());
        assertTrue(result.isTruncated());
    }
    
    @Test
    public void testQueryWithSorting() throws Exception {
        LogQuery query = LogQuery.builder()
            .file(tempLogFile)
            .sortBy("timestamp")
            .sortOrder(LogQuery.SortOrder.DESC)
            .build();
        
        QueryResult result = queryEngine.execute(query);
        
        assertEquals(3, result.getEntries().size());
        
        // Should be sorted by timestamp descending
        List<LogEntry> entries = result.getEntries();
        assertTrue(entries.get(0).getTimestamp().isAfter(entries.get(1).getTimestamp()));
        assertTrue(entries.get(1).getTimestamp().isAfter(entries.get(2).getTimestamp()));
    }
    
    @Test
    public void testQueryStatistics() throws Exception {
        LogQuery query = LogQuery.builder()
            .file(tempLogFile)
            .build();
        
        QueryResult result = queryEngine.execute(query);
        
        Map<String, Object> stats = result.getStatistics();
        
        assertEquals(3L, stats.get("totalEntries"));
        assertEquals(2L, stats.get("infoCount"));
        assertEquals(1L, stats.get("errorCount"));
        assertEquals(0L, stats.get("warnCount"));
        
        Map<String, Long> eventTypes = (Map<String, Long>) stats.get("eventTypes");
        assertEquals(Long.valueOf(1), eventTypes.get("connection"));
        assertEquals(Long.valueOf(1), eventTypes.get("security"));
        assertEquals(Long.valueOf(1), eventTypes.get("game_state_sync"));
    }
    
    @Test
    public void testQueryPerformanceMetrics() throws Exception {
        LogQuery query = LogQuery.builder()
            .file(tempLogFile)
            .build();
        
        QueryResult result = queryEngine.execute(query);
        
        assertTrue(result.getExecutionTimeMs() >= 0);
        assertTrue(result.getProcessedEntries() >= 0);
        assertTrue(result.getMatchedEntries() >= 0);
    }
    
    @Test
    public void testRegexMessageSearch() throws Exception {
        LogQuery query = LogQuery.builder()
            .file(tempLogFile)
            .messageRegex("Connection.*localhost:\\d+")
            .build();
        
        QueryResult result = queryEngine.execute(query);
        
        assertEquals(1, result.getEntries().size());
        assertTrue(result.getEntries().get(0).getMessage().matches("Connection.*localhost:\\d+"));
    }
    
    @Test
    public void testFieldExists() throws Exception {
        LogQuery query = LogQuery.builder()
            .file(tempLogFile)
            .fieldExists("durationMs")
            .build();
        
        QueryResult result = queryEngine.execute(query);
        
        assertEquals(1, result.getEntries().size());
        assertTrue(result.getEntries().get(0).getFields().containsKey("durationMs"));
    }
    
    @Test
    public void testMultipleFiles() throws Exception {
        // Create second log file
        File tempLogFile2 = File.createTempFile("test-log2", ".json");
        tempLogFile2.deleteOnExit();
        
        String additionalData = 
            "{\n" +
            "  \"timestamp\": \"2025-01-15T10:33:00.000Z\",\n" +
            "  \"level\": \"WARN\",\n" +
            "  \"logger\": \"forge.gamemodes.net.FGameClient\",\n" +
            "  \"message\": \"Connection unstable\",\n" +
            "  \"eventType\": \"connection\"\n" +
            "}\n";
        
        try (FileWriter writer = new FileWriter(tempLogFile2)) {
            writer.write(additionalData);
        }
        
        LogQuery query = LogQuery.builder()
            .files(List.of(tempLogFile, tempLogFile2))
            .level("WARN")
            .build();
        
        QueryResult result = queryEngine.execute(query);
        
        assertEquals(1, result.getEntries().size());
        assertEquals("WARN", result.getEntries().get(0).getLevel());
    }
    
    @Test
    public void testQueryBuilder() {
        LogQuery query = LogQuery.builder()
            .file(tempLogFile)
            .level("INFO")
            .eventType("connection")
            .component("FGameClient")
            .sessionId("session-1")
            .correlationId("abc123")
            .messageContains("Connection")
            .field("success", true)
            .fieldRange("port", 7000, 8000)
            .fieldExists("durationMs")
            .timeRange(Instant.parse("2025-01-15T10:00:00.000Z"), 
                      Instant.parse("2025-01-15T11:00:00.000Z"))
            .limit(10)
            .sortBy("timestamp")
            .sortOrder(LogQuery.SortOrder.ASC)
            .build();
        
        assertNotNull(query);
        assertEquals("INFO", query.getLevel());
        assertEquals("connection", query.getEventType());
        assertEquals("FGameClient", query.getComponent());
        assertEquals("session-1", query.getSessionId());
        assertEquals("abc123", query.getCorrelationId());
        assertEquals(10, query.getLimit());
        assertEquals("timestamp", query.getSortBy());
        assertEquals(LogQuery.SortOrder.ASC, query.getSortOrder());
    }
    
    @Test
    public void testEmptyQuery() throws Exception {
        LogQuery query = LogQuery.builder()
            .file(tempLogFile)
            .level("DEBUG") // No DEBUG entries in sample data
            .build();
        
        QueryResult result = queryEngine.execute(query);
        
        assertEquals(0, result.getEntries().size());
        assertFalse(result.isTruncated());
        assertEquals(0, result.getMatchedEntries());
        assertTrue(result.getProcessedEntries() > 0);
    }
    
    @Test
    public void testInvalidFile() {
        File nonExistentFile = new File("/non/existent/file.log");
        
        LogQuery query = LogQuery.builder()
            .file(nonExistentFile)
            .build();
        
        assertThrows(Exception.class, () -> queryEngine.execute(query));
    }
    
    @Test
    public void testQueryResultToString() throws Exception {
        LogQuery query = LogQuery.builder()
            .file(tempLogFile)
            .limit(1)
            .build();
        
        QueryResult result = queryEngine.execute(query);
        String str = result.toString();
        
        assertTrue(str.contains("entries=1"));
        assertTrue(str.contains("executionTime"));
    }
}