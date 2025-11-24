# Library Usage Guide

This guide shows how to integrate LogMine into your application.


## Basic API

### LogMine Class

The main facade for most users.

```java
import org.swengdev.logmine.LogMine;
import org.swengdev.logmine.LogPattern;
import org.swengdev.logmine.ProcessingMode;

// Create instance (default: batch mode, threshold=0.5, minClusterSize=2)
LogMine logMine = new LogMine();

// Or configure
LogMine logMine = new LogMine(0.6, 3); // threshold, minClusterSize
LogMine streaming = new LogMine(ProcessingMode.STREAMING);

// Add logs
logMine.addLog("Log message");
logMine.addLogs(List.of("Log 1", "Log 2", "Log 3"));

// Extract patterns
List<LogPattern> patterns = logMine.extractPatterns();
List<LogPattern> cached = logMine.getCurrentPatterns(); // Faster, returns cached

// Anomaly detection
boolean isAnomaly = logMine.isAnomaly("CRITICAL ERROR");
LogPattern match = logMine.matchPattern("Normal log");

// Maintenance
logMine.clear(); // Clear logs and patterns
LogMine.Stats stats = logMine.getStats(); // Get statistics
```

### LogMineProcessor Class

Lower-level API for advanced use cases.

```java
import org.swengdev.logmine.LogMineProcessor;
import org.swengdev.logmine.LogMineConfig;

LogMineConfig config = LogMineConfig.builder()
    .withSimilarityThreshold(0.7)
    .withMinClusterSize(5)
    .build();

LogMineProcessor processor = new LogMineProcessor(config);

// Process logs
List<LogPattern> patterns = processor.process(logs);

// Streaming
processor.processLogIncremental("Log message");

// Get results
List<LogPattern> patterns = processor.getPatterns();
LogMineProcessor.ProcessingStats stats = processor.getStats();
```

## Integration Patterns

### REST API

```java
@RestController
@RequestMapping("/api/logs")
public class LogController {
    private final LogMine logMine = new LogMine(0.6, 3);
    
    @PostMapping
    public void ingest(@RequestBody LogRequest request) {
        logMine.addLog(request.getMessage());
        
        if (logMine.isAnomaly(request.getMessage())) {
            alertService.notify(request.getMessage());
        }
    }
    
    @GetMapping("/patterns")
    public List<LogPattern> patterns() {
        return logMine.extractPatterns();
    }
    
    @GetMapping("/stats")
    public Stats stats() {
        return logMine.getStats();
    }
}
```

### Scheduled Processing

```java
@Service
public class LogService {
    private final LogMine logMine = new LogMine();
    
    public void addLog(String message) {
        logMine.addLog(message);
    }
    
    @Scheduled(fixedDelay = 300000) // Every 5 minutes
    public void processPatterns() {
        List<LogPattern> patterns = logMine.extractPatterns();
        patternRepository.saveAll(patterns);
    }
    
    @Scheduled(cron = "0 0 0 * * *") // Daily
    public void cleanup() {
        logMine.clear();
    }
}
```

### Message Queue Consumer

```java
@Service
public class LogConsumer {
    private final LogMine logMine = new LogMine(ProcessingMode.STREAMING);
    
    @KafkaListener(topics = "logs")
    public void consume(String log) {
        logMine.addLog(log);
        
        // Patterns updated automatically in streaming mode
        if (logMine.getPatternCount() > 0) {
            metricsService.recordPatternCount(logMine.getPatternCount());
        }
    }
}
```

### Batch Processing

```java
@Service
public class BatchProcessor {
    public void processBatch(List<String> logs) {
        LogMine logMine = new LogMine(0.5, 5);
        logMine.addLogs(logs);
        
        List<LogPattern> patterns = logMine.extractPatterns();
        
        return patterns.stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
}
```

## Thread Safety

`LogMine` is fully thread-safe using read-write locks.

```java
LogMine logMine = new LogMine();

// Multiple threads can safely add logs
ExecutorService executor = Executors.newFixedThreadPool(10);
for (int i = 0; i < 100; i++) {
    executor.submit(() -> logMine.addLog("Log message"));
}

// While another thread extracts patterns
scheduler.scheduleAtFixedRate(
    () -> logMine.extractPatterns(),
    5, 5, TimeUnit.MINUTES
);
```

## Performance Considerations

### Pattern Extraction Frequency

Don't extract on every log - it's expensive.

```java
// Bad - too frequent
logMine.addLog(message);
logMine.extractPatterns(); // ❌

// Good - periodic
logMine.addLog(message);
if (logMine.getLogCount() % 1000 == 0) {
    logMine.extractPatterns(); // ✓
}

// Good - scheduled
@Scheduled(fixedRate = 300000)
public void extract() {
    logMine.extractPatterns(); // ✓
}
```

### Memory Management

```java
// Clear old data periodically
@Scheduled(cron = "0 0 * * * *") // Every hour
public void cleanup() {
    List<LogPattern> patterns = logMine.getCurrentPatterns();
    savePatterns(patterns);
    logMine.clear();
}
```

### Batch Operations

```java
// Prefer batch additions
List<String> batch = collectLogs();
logMine.addLogs(batch); // ✓

// Instead of
for (String log : batch) {
    logMine.addLog(log); // Less efficient
}
```

## LogPattern API

```java
LogPattern pattern = ...;

// Pattern information
String signature = pattern.getSignature();          // Pattern string
int support = pattern.getSupportCount();            // How many logs matched
double specificity = pattern.getSpecificity();      // 0.0-1.0 (how specific)
List<String> tokens = pattern.getTokens();          // Token list
String patternId = pattern.getPatternId();          // Unique ID

// Short ID for display
String shortId = pattern.getShortPatternId(); // First 8 chars
```

## Error Handling

```java
try {
    logMine.addLog(message);
} catch (Exception e) {
    logger.error("Failed to add log", e);
    // LogMine handles errors internally, but be defensive
}

// For critical systems
try {
    List<LogPattern> patterns = logMine.extractPatterns();
} catch (OutOfMemoryError e) {
    logger.error("OOM during pattern extraction", e);
    logMine.clear();
    // Retry with smaller batch
}
```

## Testing

### Unit Tests

```java
@Test
public void testPatternExtraction() {
    LogMine logMine = new LogMine(0.5, 2);
    
    logMine.addLog("INFO User alice logged in");
    logMine.addLog("INFO User bob logged in");
    logMine.addLog("ERROR Database timeout");
    logMine.addLog("ERROR Database timeout");
    
    List<LogPattern> patterns = logMine.extractPatterns();
    
    assertEquals(2, patterns.size());
    assertTrue(patterns.get(0).getSupportCount() >= 2);
}

@Test
public void testAnomalyDetection() {
    LogMine logMine = new LogMine();
    
    logMine.addLog("INFO Normal log");
    logMine.addLog("INFO Normal log");
    logMine.extractPatterns();
    
    assertFalse(logMine.isAnomaly("INFO Normal log"));
    assertTrue(logMine.isAnomaly("CRITICAL UNKNOWN ERROR"));
}
```

### Integration Tests

```java
@SpringBootTest
public class LogServiceTest {
    @Autowired
    private LogService logService;
    
    @Test
    public void testEndToEnd() {
        // Add logs
        for (int i = 0; i < 100; i++) {
            logService.addLog("Test log " + i);
        }
        
        // Process
        logService.processPatterns();
        
        // Verify
        assertTrue(logService.getPatternCount() > 0);
    }
}
```

## Common Mistakes

### Mistake 1: Extracting Too Frequently

```java
// ❌ Don't do this
@PostMapping("/log")
public void receiveLog(String log) {
    logMine.addLog(log);
    logMine.extractPatterns(); // Called on every request!
}

// ✓ Do this instead
@PostMapping("/log")
public void receiveLog(String log) {
    logMine.addLog(log);
}

@Scheduled(fixedDelay = 60000)
public void extractPatterns() {
    logMine.extractPatterns();
}
```

### Mistake 2: Not Clearing Old Data

```java
// ❌ Memory grows unbounded
LogMine logMine = new LogMine();
// Keep adding logs forever...

// ✓ Clear periodically
@Scheduled(cron = "0 0 0 * * *")
public void cleanup() {
    logMine.clear();
}
```

### Mistake 3: Wrong Configuration

```java
// ❌ Too strict - creates too many patterns
LogMine logMine = new LogMine(0.95, 1);

// ❌ Too lenient - groups everything together
LogMine logMine = new LogMine(0.1, 1);

// ✓ Start balanced
LogMine logMine = new LogMine(0.5, 3);
// Then adjust based on results
```

## Best Practices

1. **Initialize once** - Create LogMine at startup, not per request
2. **Extract periodically** - Not on every log
3. **Monitor memory** - Clear old data regularly
4. **Start with defaults** - Tune based on results
5. **Test with real data** - Sample your actual logs
6. **Handle anomalies** - Set up alerts for unknown patterns
7. **Document config** - Explain why you chose specific thresholds

## Support

- Configuration: See `CONFIGURATION_GUIDE.md`
- Algorithm details: See `UNDERSTANDING_THE_ALGORITHM.md`
- Examples: Check `src/test/java/` for test examples
