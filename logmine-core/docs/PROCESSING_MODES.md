# LogMine Processing Modes

LogMine supports two processing modes: **STREAMING** and **BATCH**. Each mode is optimized for different use cases.

## Quick Comparison

| Feature | STREAMING | BATCH |
|---------|-----------|-------|
| **Memory Usage** | O(k) - constant | O(n) - grows with logs |
| **Raw Log Storage** | No | Yes |
| **Processing** | Immediate/Online | Deferred/Offline |
| **Pattern Availability** | Always current | After extraction |
| **Re-processing** | Not supported | Supported |
| **Use Case** | Production monitoring | Research/Analysis |
| **Max Log Volume** | Unlimited | Limited by memory |

**Where:**
- `k` = number of clusters/patterns (typically 10-100)
- `n` = number of logs (can be millions)

## STREAMING Mode

### Overview
Processes logs **immediately** without storing raw log messages. Patterns are updated in real-time.

### Characteristics
- **No memory usage for raw logs** - Only cluster representatives stored
- **Instant pattern updates** - Always available via `getCurrentPatterns()`
- **Unlimited log volumes** - Can process millions/billions of logs
- **Real-time monitoring** - Immediate anomaly detection
- **No re-processing** - Cannot change parameters after processing
- **No raw log access** - Original logs not stored

### Memory Footprint
```
Memory ≈ (Number of Clusters) × (Avg Cluster Size) × (Avg Token Count)
       ≈ 50 clusters × 1 representative × 20 tokens ≈ 1000 LogMessage objects
```

### Usage

```java
// Create in streaming mode
LogMine logMine = new LogMine(ProcessingMode.STREAMING);

// Process logs as they arrive
logMine.addLog(incomingLog);  // Processed immediately, not stored

// Patterns are always current
List<LogPattern> patterns = logMine.getCurrentPatterns();  // No processing needed

// Check for anomalies in real-time
boolean isAnomaly = logMine.isAnomaly(newLog);
```

### Best For
- Production log monitoring
- Real-time alerting systems
- High-volume log processing
- Memory-constrained environments
- Streaming data pipelines

### Example: Production Web Server

```java
// Initialize once at startup
LogMine logMine = new LogMine(ProcessingMode.STREAMING);

// In your log processing endpoint
@PostMapping("/logs/ingest")
public void ingestLog(@RequestBody LogEvent event) {
    // Process immediately
    logMine.addLog(event.getMessage());
    
    // Check for anomalies
    if (logMine.isAnomaly(event.getMessage())) {
        alertingService.sendAlert("Anomalous log detected");
    }
}

// Background thread for pattern reporting
@Scheduled(fixedRate = 60000)  // Every minute
public void reportPatterns() {
    List<LogPattern> patterns = logMine.getCurrentPatterns();
    metricsService.recordPatternCount(patterns.size());
}
```

## BATCH Mode

### Overview
**Stores** raw logs in memory and processes them together when `extractPatterns()` is called.

### Characteristics
- **Re-processing supported** - Can extract patterns multiple times
- **Raw log access** - All original logs available
- **Parameter tuning** - Experiment with different thresholds
- **Historical analysis** - Analyze bounded datasets
- **Memory usage grows** - Stores all logs in memory
- **Manual processing** - Must call `extractPatterns()` explicitly

### Memory Footprint
```
Memory = (Number of Logs) × (Avg Log Length)
       = 1M logs × 200 chars ≈ 200MB
```

Plus memory limits:
```java
// Configurable max logs in memory (default: 100,000)
LogMine logMine = new LogMine(
    ProcessingMode.BATCH,
    processor,
    50000  // Keep max 50k logs
);
```

### Usage

```java
// Create in batch mode (default)
LogMine logMine = new LogMine(ProcessingMode.BATCH);
// or explicitly:
LogMine logMine = new LogMine(ProcessingMode.BATCH);

// Collect logs
logMine.addLog(log1);
logMine.addLog(log2);
logMine.addLogs(logBatch);

// Process all at once
List<LogPattern> patterns = logMine.extractPatterns();

// Can re-process with different configuration
logMine.clear();
logMine.addLogs(logs);
patterns = logMine.extractPatterns();
```

### Best For
- Research and experimentation
- Parameter tuning and optimization
- Historical log analysis
- Bounded datasets (known size)
- Offline processing

### Example: Research and Analysis

```java
// Experiment with different similarity thresholds
double[] thresholds = {0.3, 0.5, 0.7, 0.9};

List<String> historicalLogs = loadHistoricalLogs();

for (double threshold : thresholds) {
    LogMine logMine = new LogMine(threshold, 2);
    logMine.addLogs(historicalLogs);
    
    List<LogPattern> patterns = logMine.extractPatterns();
    
    System.out.printf("Threshold %.1f: %d patterns\n", 
        threshold, patterns.size());
    
    // Analyze pattern quality
    analyzePatterns(patterns);
}
```

## Switching Between Modes

Modes are **immutable** - set at initialization time:

```java
// Streaming
LogMine streaming = new LogMine(ProcessingMode.STREAMING);

// Batch
LogMine batch = new LogMine(ProcessingMode.BATCH);

// Check current mode
if (logMine.isStreaming()) {
    // Streaming-specific logic
}

if (logMine.isBatch()) {
    // Batch-specific logic
}
```

## Decision Guide

### Use STREAMING when:
- Processing production logs in real-time
- Log volume is very high or unknown
- Memory is constrained
- Need instant pattern updates
- Building monitoring/alerting systems
- Deploying in cloud with auto-scaling

### Use BATCH when:
- Analyzing historical logs (bounded dataset)
- Experimenting with parameters
- Researching log patterns
- Need to access original logs
- Working in Jupyter notebooks
- One-time analysis or reports

## Performance Characteristics

### Streaming Mode

**Throughput:**
```
~8,000 logs/second (single thread)
Scales with cores (thread-safe)
```

**Latency:**
```
~0.1ms per log (immediate processing)
Patterns always available (cached)
```

**Memory:**
```
Constant regardless of log count
Typically 10-50MB for cluster storage
```

### Batch Mode

**Throughput:**
```
Collection: ~158,000 logs/second (just storing)
Processing: ~2-8 ops/second (full pattern extraction)
```

**Latency:**
```
Collection: Instant (just adds to list)
Processing: O(n) - grows with log count
```

**Memory:**
```
Grows linearly with log count
~200 bytes per log message
Configurable max limit
```

## Advanced Patterns

### Hybrid Approach: Streaming + Periodic Export

```java
LogMine streaming = new LogMine(ProcessingMode.STREAMING);

// Process in real-time
streaming.addLog(log);

// Periodically export patterns for analysis
@Scheduled(cron = "0 0 * * * *")  // Every hour
public void exportPatterns() {
    List<LogPattern> patterns = streaming.getCurrentPatterns();
    
    // Store patterns for historical analysis
    patternRepository.save(patterns);
    
    // Can create batch analysis from stored patterns
    LogMine batch = new LogMine(ProcessingMode.BATCH);
    // Analyze patterns over time...
}
```

### Progressive Batch Processing

```java
LogMine batch = new LogMine(ProcessingMode.BATCH);

// Process large log files in chunks
try (Stream<String> lines = Files.lines(hugeLogFile)) {
    lines
        .filter(line -> !line.isEmpty())
        .forEach(batch::addLog);
        
    // Extract patterns when done
    List<LogPattern> patterns = batch.extractPatterns();
}
```

### Mode-Agnostic Code

```java
public void processLogs(LogMine logMine, List<String> logs) {
    // Add logs (works for both modes)
    logs.forEach(logMine::addLog);
    
    // Get patterns (handles both modes correctly)
    List<LogPattern> patterns = logMine.extractPatterns();
    
    // Mode-specific optimization
    if (logMine.isStreaming()) {
        // Patterns are always fresh, can skip extractPatterns()
        patterns = logMine.getCurrentPatterns();
    }
}
```

## Configuration

### Streaming with Custom Config

```java
LogMineConfig config = LogMineConfig.builder()
    .withSimilarityThreshold(0.6)
    .withMinClusterSize(3)
    .withMaxTokens(50)
    .build();

LogMineProcessor processor = new LogMineProcessor(config);
LogMine logMine = new LogMine(
    ProcessingMode.STREAMING,
    processor,
    0  // maxLogsInMemory ignored in streaming mode
);
```

### Batch with Memory Limit

```java
LogMine logMine = new LogMine(
    ProcessingMode.BATCH,
    new LogMineProcessor(0.5, 2),
    50000  // Keep max 50k logs (oldest dropped first)
);
```

## Real-World Examples

### 1. High-Traffic Web Application (Streaming)

```java
@Service
public class LogAnalyticsService {
    private final LogMine logMine = new LogMine(ProcessingMode.STREAMING);
    
    public void processAccessLog(HttpRequest request) {
        String log = formatAccessLog(request);
        logMine.addLog(log);
        
        // Real-time monitoring
        if (logMine.isAnomaly(log)) {
            incidentService.createIncident(log);
        }
    }
    
    @Scheduled(fixedRate = 300000)  // Every 5 minutes
    public void reportMetrics() {
        LogMine.Stats stats = logMine.getStats();
        metricsPublisher.publish(stats);
    }
}
```

### 2. Security Log Analysis (Batch)

```java
@Service
public class SecurityAuditService {
    public AuditReport analyzeLogs(LocalDate date) {
        // Load day's logs
        List<String> logs = logRepository.findByDate(date);
        
        // Batch analysis
        LogMine logMine = new LogMine(ProcessingMode.BATCH);
        logMine.addLogs(logs);
        
        List<LogPattern> patterns = logMine.extractPatterns();
        
        // Look for suspicious patterns
        List<LogPattern> suspicious = patterns.stream()
            .filter(p -> p.getSupportCount() < 5)
            .collect(Collectors.toList());
        
        return AuditReport.builder()
            .date(date)
            .totalLogs(logs.size())
            .patterns(patterns.size())
            .suspicious(suspicious)
            .build();
    }
}
```

## Common Pitfalls

### ❌ Wrong: Using Batch for Production Streaming

```java
// DON'T DO THIS
LogMine batch = new LogMine(ProcessingMode.BATCH);

while (true) {
    String log = receiveLog();
    batch.addLog(log);  // Memory grows forever!
}
```

### ✅ Right: Use Streaming for Unbounded Data

```java
LogMine streaming = new LogMine(ProcessingMode.STREAMING);

while (true) {
    String log = receiveLog();
    streaming.addLog(log);  // Constant memory
}
```

### ❌ Wrong: Not Calling extractPatterns() in Batch

```java
LogMine batch = new LogMine(ProcessingMode.BATCH);
batch.addLogs(logs);
// Forgot to call extractPatterns()!
boolean anomaly = batch.isAnomaly(log);  // Uses stale/empty patterns
```

### ✅ Right: Extract Patterns Before Using

```java
LogMine batch = new LogMine(ProcessingMode.BATCH);
batch.addLogs(logs);
batch.extractPatterns();  // Process first!
boolean anomaly = batch.isAnomaly(log);  // Now works correctly
```

## See Also

- [Quick Start Guide](QUICK_START.md)
- [Configuration Guide](CONFIGURATION_GUIDE.md)
- [Library Usage](LIBRARY_USAGE.md)
- [Architecture Design](ARCHITECTURE_DESIGN.md)

**Examples:**
- `StreamingExample.java` - Production streaming demo
- `BatchExample.java` - Research and analysis demo
- `ModeComparisonExample.java` - Side-by-side comparison

