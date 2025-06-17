package forge.gui.logging;

import forge.gui.logging.LogAnalyzer.LogEntry;
import forge.gui.logging.LogAnalyzer.LogParseException;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import static org.testng.Assert.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.io.StringReader;

/**
 * TDD tests for LogAnalyzer utility.
 * These tests define the expected behavior before implementation.
 */
public class LogAnalyzerTest {
    
    private LogAnalyzer analyzer;
    private String sampleJsonLog;
    private String multiLineJsonLog;
    
    @BeforeMethod
    public void setUp() {
        analyzer = new LogAnalyzer();
        
        // Sample structured log entry
        sampleJsonLog = "{\n" +
            "  \"timestamp\": \"2025-01-15T10:30:45.123Z\",\n" +
            "  \"level\": \"INFO\",\n" +
            "  \"logger\": \"forge.gamemodes.net.FGameClient\",\n" +
            "  \"message\": \"Connection to localhost:7777 succeeded in 1500ms\",\n" +
            "  \"eventType\": \"connection\",\n" +
            "  \"host\": \"localhost\",\n" +
            "  \"port\": 7777,\n" +
            "  \"success\": true,\n" +
            "  \"durationMs\": 1500,\n" +
            "  \"correlationId\": \"abc12345\",\n" +
            "  \"sessionId\": \"session-123\"\n" +
            "}\n";
        
        // Multiple log entries
        multiLineJsonLog = sampleJsonLog +
            "{\n" +
            "  \"timestamp\": \"2025-01-15T10:31:00.456Z\",\n" +
            "  \"level\": \"ERROR\",\n" +
            "  \"logger\": \"forge.gamemodes.net.FServerManager\",\n" +
            "  \"message\": \"Connection timeout occurred\",\n" +
            "  \"eventType\": \"error\",\n" +
            "  \"errorType\": \"TIMEOUT\",\n" +
            "  \"correlationId\": \"def67890\"\n" +
            "}\n";
    }
    
    @Test
    public void testParseSingleLogEntry() throws Exception {
        LogEntry entry = analyzer.parseLogEntry(sampleJsonLog);
        
        assertNotNull(entry);
        assertEquals("2025-01-15T10:30:45.123Z", entry.getTimestamp().toString());
        assertEquals("INFO", entry.getLevel());
        assertEquals("forge.gamemodes.net.FGameClient", entry.getLogger());
        assertEquals("Connection to localhost:7777 succeeded in 1500ms", entry.getMessage());
        assertEquals("connection", entry.getEventType());
        assertEquals("abc12345", entry.getCorrelationId());
        assertEquals("session-123", entry.getSessionId());
    }
    
    @Test
    public void testParseLogEntryFields() throws Exception {
        LogEntry entry = analyzer.parseLogEntry(sampleJsonLog);
        
        Map<String, Object> fields = entry.getFields();
        
        assertEquals("localhost", fields.get("host"));
        assertEquals(7777, fields.get("port"));
        assertEquals(true, fields.get("success"));
        assertEquals(1500, fields.get("durationMs"));
    }
    
    @Test
    public void testParseMultipleLogEntries() throws Exception {
        StringReader reader = new StringReader(multiLineJsonLog);
        List<LogEntry> entries = analyzer.parseLogStream(reader);
        
        assertEquals(2, entries.size());
        
        LogEntry first = entries.get(0);
        assertEquals("INFO", first.getLevel());
        assertEquals("connection", first.getEventType());
        
        LogEntry second = entries.get(1);
        assertEquals("ERROR", second.getLevel());
        assertEquals("error", second.getEventType());
        assertEquals("TIMEOUT", second.getFields().get("errorType"));
    }
    
    @Test
    public void testFilterByLevel() throws Exception {
        StringReader reader = new StringReader(multiLineJsonLog);
        List<LogEntry> entries = analyzer.parseLogStream(reader);
        
        List<LogEntry> errorEntries = analyzer.filterByLevel(entries, "ERROR");
        assertEquals(1, errorEntries.size());
        assertEquals("ERROR", errorEntries.get(0).getLevel());
        
        List<LogEntry> infoEntries = analyzer.filterByLevel(entries, "INFO");
        assertEquals(1, infoEntries.size());
        assertEquals("INFO", infoEntries.get(0).getLevel());
    }
    
    @Test
    public void testFilterByEventType() throws Exception {
        StringReader reader = new StringReader(multiLineJsonLog);
        List<LogEntry> entries = analyzer.parseLogStream(reader);
        
        List<LogEntry> connectionEntries = analyzer.filterByEventType(entries, "connection");
        assertEquals(1, connectionEntries.size());
        assertEquals("connection", connectionEntries.get(0).getEventType());
        
        List<LogEntry> errorEntries = analyzer.filterByEventType(entries, "error");
        assertEquals(1, errorEntries.size());
        assertEquals("error", errorEntries.get(0).getEventType());
    }
    
    @Test
    public void testFilterByTimeRange() throws Exception {
        StringReader reader = new StringReader(multiLineJsonLog);
        List<LogEntry> entries = analyzer.parseLogStream(reader);
        
        Instant start = Instant.parse("2025-01-15T10:30:00Z");
        Instant end = Instant.parse("2025-01-15T10:30:50Z");
        
        List<LogEntry> filteredEntries = analyzer.filterByTimeRange(entries, start, end);
        assertEquals(1, filteredEntries.size());
        assertEquals("connection", filteredEntries.get(0).getEventType());
    }
    
    @Test
    public void testFilterByCorrelationId() throws Exception {
        StringReader reader = new StringReader(multiLineJsonLog);
        List<LogEntry> entries = analyzer.parseLogStream(reader);
        
        List<LogEntry> correlatedEntries = analyzer.filterByCorrelationId(entries, "abc12345");
        assertEquals(1, correlatedEntries.size());
        assertEquals("abc12345", correlatedEntries.get(0).getCorrelationId());
    }
    
    @Test
    public void testFilterByComponent() throws Exception {
        StringReader reader = new StringReader(multiLineJsonLog);
        List<LogEntry> entries = analyzer.parseLogStream(reader);
        
        List<LogEntry> clientEntries = analyzer.filterByComponent(entries, "FGameClient");
        assertEquals(1, clientEntries.size());
        assertTrue(clientEntries.get(0).getLogger().contains("FGameClient"));
        
        List<LogEntry> serverEntries = analyzer.filterByComponent(entries, "FServerManager");
        assertEquals(1, serverEntries.size());
        assertTrue(serverEntries.get(0).getLogger().contains("FServerManager"));
    }
    
    @Test
    public void testSearchMessages() throws Exception {
        StringReader reader = new StringReader(multiLineJsonLog);
        List<LogEntry> entries = analyzer.parseLogStream(reader);
        
        List<LogEntry> connectionMessages = analyzer.searchMessages(entries, "Connection");
        assertEquals(2, connectionMessages.size()); // Both contain "Connection"
        
        List<LogEntry> successMessages = analyzer.searchMessages(entries, "succeeded");
        assertEquals(1, successMessages.size());
        assertEquals("connection", successMessages.get(0).getEventType());
    }
    
    @Test
    public void testExtractErrorSummary() throws Exception {
        String errorLog = multiLineJsonLog + 
            "{\n" +
            "  \"timestamp\": \"2025-01-15T10:31:30.789Z\",\n" +
            "  \"level\": \"ERROR\",\n" +
            "  \"logger\": \"forge.gamemodes.net.FGameClient\",\n" +
            "  \"message\": \"Serialization failed\",\n" +
            "  \"eventType\": \"error\",\n" +
            "  \"errorType\": \"SERIALIZATION\"\n" +
            "}\n";
        
        StringReader reader = new StringReader(errorLog);
        List<LogEntry> entries = analyzer.parseLogStream(reader);
        
        Map<String, Integer> errorSummary = analyzer.extractErrorSummary(entries);
        
        assertEquals(Integer.valueOf(1), errorSummary.get("TIMEOUT"));
        assertEquals(Integer.valueOf(1), errorSummary.get("SERIALIZATION"));
        assertEquals(2, errorSummary.size());
    }
    
    @Test
    public void testExtractPerformanceMetrics() throws Exception {
        String perfLog = "{\n" +
            "  \"timestamp\": \"2025-01-15T10:32:00.000Z\",\n" +
            "  \"level\": \"INFO\",\n" +
            "  \"logger\": \"performance\",\n" +
            "  \"message\": \"Performance metric: connection_time = 1500.0 ms\",\n" +
            "  \"metric\": \"connection_time\",\n" +
            "  \"value\": \"1500.0\",\n" +
            "  \"unit\": \"ms\",\n" +
            "  \"component\": \"FGameClient\"\n" +
            "}\n";
        
        StringReader reader = new StringReader(perfLog);
        List<LogEntry> entries = analyzer.parseLogStream(reader);
        
        Map<String, Double> metrics = analyzer.extractPerformanceMetrics(entries);
        
        assertEquals(Double.valueOf(1500.0), metrics.get("connection_time"));
    }
    
    @Test
    public void testGroupBySession() throws Exception {
        String sessionLog = multiLineJsonLog +
            "{\n" +
            "  \"timestamp\": \"2025-01-15T10:31:45.000Z\",\n" +
            "  \"level\": \"INFO\",\n" +
            "  \"logger\": \"forge.gamemodes.net.FGameClient\",\n" +
            "  \"message\": \"Game state updated\",\n" +
            "  \"eventType\": \"game_state_sync\",\n" +
            "  \"sessionId\": \"session-123\"\n" +
            "}\n";
        
        StringReader reader = new StringReader(sessionLog);
        List<LogEntry> entries = analyzer.parseLogStream(reader);
        
        Map<String, List<LogEntry>> sessionGroups = analyzer.groupBySession(entries);
        
        assertTrue(sessionGroups.containsKey("session-123"));
        assertEquals(2, sessionGroups.get("session-123").size());
        assertTrue(sessionGroups.containsKey("null")); // Entry without sessionId
        assertEquals(1, sessionGroups.get("null").size());
    }
    
    @Test
    public void testAnalyzeConnectionEvents() throws Exception {
        String connectionLog = sampleJsonLog +
            "{\n" +
            "  \"timestamp\": \"2025-01-15T10:31:15.000Z\",\n" +
            "  \"level\": \"WARN\",\n" +
            "  \"logger\": \"forge.gamemodes.net.FGameClient\",\n" +
            "  \"message\": \"Connection to localhost:7777 failed in 3000ms\",\n" +
            "  \"eventType\": \"connection\",\n" +
            "  \"host\": \"localhost\",\n" +
            "  \"port\": 7777,\n" +
            "  \"success\": false,\n" +
            "  \"durationMs\": 3000\n" +
            "}\n";
        
        StringReader reader = new StringReader(connectionLog);
        List<LogEntry> entries = analyzer.parseLogStream(reader);
        
        LogAnalyzer.ConnectionAnalysis analysis = analyzer.analyzeConnectionEvents(entries);
        
        assertEquals(2, analysis.getTotalAttempts());
        assertEquals(1, analysis.getSuccessfulAttempts());
        assertEquals(1, analysis.getFailedAttempts());
        assertEquals(50.0, analysis.getSuccessRate(), 0.1);
        assertEquals(2250.0, analysis.getAverageConnectionTime(), 0.1); // (1500 + 3000) / 2
    }
    
    @Test(expectedExceptions = LogParseException.class)
    public void testInvalidJsonHandling() throws Exception {
        String invalidJson = "{ invalid json content }";
        analyzer.parseLogEntry(invalidJson);
    }
    
    @Test
    public void testEmptyLogHandling() throws Exception {
        StringReader reader = new StringReader("");
        List<LogEntry> entries = analyzer.parseLogStream(reader);
        
        assertTrue(entries.isEmpty());
    }
    
    @Test
    public void testMalformedJsonSkipping() throws Exception {
        String mixedLog = sampleJsonLog +
            "{ malformed json }\n" +
            "{\n" +
            "  \"timestamp\": \"2025-01-15T10:32:00.000Z\",\n" +
            "  \"level\": \"INFO\",\n" +
            "  \"logger\": \"test\",\n" +
            "  \"message\": \"Valid entry\"\n" +
            "}\n";
        
        StringReader reader = new StringReader(mixedLog);
        List<LogEntry> entries = analyzer.parseLogStream(reader);
        
        assertEquals(2, entries.size()); // Should skip malformed entry
        assertEquals("Valid entry", entries.get(1).getMessage());
    }
    
    @Test
    public void testLogEntryEquality() throws Exception {
        LogEntry entry1 = analyzer.parseLogEntry(sampleJsonLog);
        LogEntry entry2 = analyzer.parseLogEntry(sampleJsonLog);
        
        assertEquals(entry1, entry2);
        assertEquals(entry1.hashCode(), entry2.hashCode());
    }
    
    @Test
    public void testLogEntryToString() throws Exception {
        LogEntry entry = analyzer.parseLogEntry(sampleJsonLog);
        String str = entry.toString();
        
        assertTrue(str.contains("2025-01-15T10:30:45.123Z"));
        assertTrue(str.contains("INFO"));
        assertTrue(str.contains("connection"));
    }
}