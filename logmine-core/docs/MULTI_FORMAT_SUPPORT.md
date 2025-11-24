# Multi-Format Log Support

LogMine is designed to handle **completely different log formats** from different sources simultaneously.

## The Core Principle

**Each log source can have its own patterns and configuration.**

This means:
- ✅ Web server logs (Apache format) → Own patterns
- ✅ Database logs (SQL queries) → Own patterns  
- ✅ Application logs (JSON) → Own patterns
- ✅ Syslog (RFC 5424) → Own patterns
- ✅ Custom formats → Own patterns

**No cross-contamination between sources!**

## Three Strategies

### Strategy 1: Per-Source Processors (Complete Isolation) ✅ RECOMMENDED

Each source gets its own `LogMine` instance with its own configuration:

```java
Map<String, LogMine> processors = new HashMap<>();

// Web server - Apache/Nginx format
processors.put("web-server", new LogMine(
    ProcessingMode.STREAMING,
    new LogMineProcessor(LogMineConfig.builder()
        .normalizeIPs(true)
        .normalizeTimestamps(true)
        .withSimilarityThreshold(0.7)
        .build()),
    50_000
));

// Database - SQL query logs
processors.put("database", new LogMine(
    ProcessingMode.STREAMING,
    new LogMineProcessor(LogMineConfig.builder()
        .normalizeNumbers(true)
        .normalizeTimestamps(true)
        .withSimilarityThreshold(0.8)
        .build()),
    50_000
));

// Application - JSON logs
processors.put("application", new LogMine(
    ProcessingMode.STREAMING,
    new LogMineProcessor(LogMineConfig.builder()
        .normalizeTimestamps(true)
        .normalizeNumbers(true)
        .caseSensitive(false)
        .build()),
    50_000
));

// Process logs
processors.get("web-server").addLog("192.168.1.1 - - [15/Jan/2024] GET /api/users 200");
processors.get("database").addLog("2024-01-15 SELECT * FROM users WHERE id = 12345");
processors.get("application").addLog("{\"timestamp\":\"2024-01-15T10:30:45Z\",\"level\":\"INFO\"}");

// Get patterns per source
List<LogPattern> webPatterns = processors.get("web-server").getCurrentPatterns();
List<LogPattern> dbPatterns = processors.get("database").getCurrentPatterns();
List<LogPattern> appPatterns = processors.get("application").getCurrentPatterns();
```

**Pros**:
- ✅ **Complete isolation**: Each source has its own patterns
- ✅ **Per-source configuration**: Different thresholds, normalization rules
- ✅ **No interference**: Web server patterns don't affect database patterns
- ✅ **Clear separation**: Easy to understand and debug

**Cons**:
- ❌ Memory usage scales with number of sources (~115 MB per active source)

**Use when**:
- You have < 1000 active sources
- You want complete isolation between sources
- Different sources need different configurations

---

### Strategy 2: Shared Processor with Source Tagging

One `LogMine` instance, but prefix logs with source tags:

```java
LogMine logMine = new LogMine(
    ProcessingMode.STREAMING,
    new LogMineProcessor(LogMineConfig.builder()
        .normalizeTimestamps(true)
        .normalizeIPs(true)
        .normalizeNumbers(true)
        .build()),
    100_000
);

// Tag each log with source
logMine.addLog("[src:web] 192.168.1.1 - - [15/Jan/2024] GET /api/users 200");
logMine.addLog("[src:db] 2024-01-15 SELECT * FROM users WHERE id = 12345");
logMine.addLog("[src:app] {\"timestamp\":\"2024-01-15T10:30:45Z\"}");

// Filter patterns by source
List<LogPattern> webPatterns = logMine.getCurrentPatterns().stream()
    .filter(p -> p.getSignature().contains("[src:web]"))
    .collect(Collectors.toList());

List<LogPattern> dbPatterns = logMine.getCurrentPatterns().stream()
    .filter(p -> p.getSignature().contains("[src:db]"))
    .collect(Collectors.toList());
```

**Pros**:
- ✅ Lower memory usage (single instance)
- ✅ Simpler lifecycle management

**Cons**:
- ❌ All sources use same configuration
- ❌ Patterns from different sources may influence each other slightly

**Use when**:
- You have 1000+ sources
- Memory is constrained
- All sources can use similar configuration

---

### Strategy 3: Preprocessing for Format Normalization

Let preprocessing handle format differences:

```java
LogMineConfig config = LogMineConfig.builder()
    .normalizeTimestamps(true)    // All timestamps → TIMESTAMP
    .normalizeIPs(true)            // All IPs → IP_ADDR
    .normalizeNumbers(true)        // All numbers → NUM
    .normalizeUrls(true)
    .normalizePaths(true)
    .caseSensitive(false)
    .build();

LogMine logMine = new LogMine(ProcessingMode.STREAMING, 
                               new LogMineProcessor(config), 
                               100_000);

// Different formats, but preprocessing normalizes them
logMine.addLog("192.168.1.1 [15/Jan/2024:10:30:45] GET /api/users 200");
logMine.addLog("2024-01-15T10:30:45Z GET /api/users returned 200");
logMine.addLog("{\"timestamp\":\"2024-01-15T10:30:45Z\",\"path\":\"/api/users\",\"status\":200}");

// These cluster together because they're semantically similar
```

**Result**: Logs with similar meaning cluster together, even if formats differ!

```
Pattern: "TIMESTAMP GET /api/users NUM"
- Matches: Apache logs, structured logs, JSON logs
- Benefit: Reduces pattern count, finds semantic similarities
```

**Pros**:
- ✅ Finds semantic similarities across formats
- ✅ Reduces pattern count
- ✅ Good for high-level analysis

**Cons**:
- ❌ Loses format-specific details
- ❌ May over-cluster if formats are too different

**Use when**:
- You want to find common patterns across sources
- Format differences are not important
- High-level overview is the goal

---

## Real-World Example

See `MultiFormatDemo.java` in the `logmine-examples/` subproject for a complete working example:

```bash
# Run the demo
cd logmine-examples
./gradlew runMultiFormat
```

**Output shows**:
- Web Server patterns (Apache/Nginx format)
- Database patterns (SQL query format)
- Application patterns (custom format)

**All completely separate!** No cross-contamination.

---

## Per-Source Configuration

Different sources often need different settings:

```java
// Web server: strict clustering, normalize IPs
LogMineConfig webConfig = LogMineConfig.builder()
    .withSimilarityThreshold(0.8)     // Strict
    .normalizeIPs(true)
    .normalizeTimestamps(true)
    .build();

// Database: lenient clustering, normalize numbers
LogMineConfig dbConfig = LogMineConfig.builder()
    .withSimilarityThreshold(0.6)     // Lenient
    .normalizeNumbers(true)
    .normalizeTimestamps(true)
    .build();

// Application: case-insensitive, normalize everything
LogMineConfig appConfig = LogMineConfig.builder()
    .withSimilarityThreshold(0.7)
    .normalizeTimestamps(true)
    .normalizeIPs(true)
    .normalizeNumbers(true)
    .caseSensitive(false)
    .build();
```

---

## Architecture Example

Here's a production-ready service:

```java
@Service
public class MultiSourceLogService {
    
    private final ConcurrentHashMap<String, SourceProcessor> processors = new ConcurrentHashMap<>();
    
    static class SourceProcessor {
        final LogMine logMine;
        final LogMineConfig config;
        final String format;
        
        SourceProcessor(LogMineConfig config, String format) {
            this.config = config;
            this.format = format;
            this.logMine = new LogMine(
                ProcessingMode.STREAMING,
                new LogMineProcessor(config),
                50_000
            );
        }
    }
    
    public void registerSource(String sourceId, LogMineConfig config, String format) {
        processors.put(sourceId, new SourceProcessor(config, format));
    }
    
    public void processLog(String sourceId, String logMessage) {
        SourceProcessor processor = processors.get(sourceId);
        if (processor == null) {
            // Auto-register with default config
            processor = new SourceProcessor(
                LogMineConfig.defaults(), 
                "Unknown"
            );
            processors.put(sourceId, processor);
        }
        processor.logMine.addLog(logMessage);
    }
    
    public List<LogPattern> getPatterns(String sourceId) {
        SourceProcessor processor = processors.get(sourceId);
        return processor != null 
            ? processor.logMine.getCurrentPatterns() 
            : List.of();
    }
    
    public Map<String, List<LogPattern>> getAllPatterns() {
        Map<String, List<LogPattern>> result = new HashMap<>();
        processors.forEach((sourceId, processor) -> {
            result.put(sourceId, processor.logMine.getCurrentPatterns());
        });
        return result;
    }
    
    // Export patterns per source (optional - see logmine-export/ subproject)
    @Scheduled(fixedRate = 300000)
    public void exportPatterns() {
        processors.forEach((sourceId, processor) -> {
            List<LogPattern> patterns = processor.logMine.getCurrentPatterns();
            
            // Store patterns however you want:
            // - Database
            // - Message queue
            // - Search engine
            // - Or use logmine-export module for standardized interfaces
            
            patterns.forEach(pattern -> {
                myStorage.save(pattern);
            });
        });
    }
    
    // Cleanup inactive sources
    @Scheduled(fixedRate = 3600000)
    public void cleanupInactive() {
        long oneHourAgo = System.currentTimeMillis() - 3600000;
        processors.entrySet().removeIf(entry -> 
            entry.getValue().logMine.getLastActivityTime() < oneHourAgo
        );
    }
}
```

---

## Key Takeaways

1. **Each source is independent**: Web server patterns don't affect database patterns
2. **Per-source configuration**: Different thresholds, normalization rules
3. **Complete isolation**: Use Strategy 1 (per-source processors)
4. **Memory vs. Isolation trade-off**: 
   - Few sources (< 100) → Per-source processors
   - Many sources (> 1000) → Shared processor with tagging
5. **Preprocessing helps**: Normalizes different formats for better clustering
6. **Production-ready**: Export patterns per source, cleanup inactive sources

---

## See Also

- [Configuration Guide](CONFIGURATION_GUIDE.md) - Per-source configuration examples
- [Processing Modes](PROCESSING_MODES.md) - Streaming vs. Batch for each source
- `logmine-examples/` - Working examples with different formats

---

**Bottom Line**: LogMine is designed from the ground up to handle completely different log formats from different sources. Each source maintains its own patterns with no cross-contamination! 🎯

