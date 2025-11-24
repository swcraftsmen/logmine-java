# Quick Start

This guide gets you started with LogMine in under 10 minutes.

## What is LogMine?

LogMine automatically discovers patterns in log messages without requiring templates or regular expressions. It uses clustering to group similar logs and extracts common patterns from each group.

## Installation

Add to your `build.gradle`:

```gradle
dependencies {
    implementation project(':logmine-core')
}
```

Or build from source:

```bash
git clone https://github.com/yourorg/logmine
cd logmine
./gradlew build
```

## Basic Usage

### Simple Example

```java
import org.swengdev.logmine.LogMine;
import org.swengdev.logmine.LogPattern;
import java.util.List;

// Create a LogMine instance
LogMine logMine = new LogMine();

// Add some logs
logMine.addLog("2024-01-15 INFO User alice logged in");
logMine.addLog("2024-01-15 INFO User bob logged in");
logMine.addLog("2024-01-15 ERROR Database connection failed");
logMine.addLog("2024-01-15 ERROR Database connection failed");

// Extract patterns
List<LogPattern> patterns = logMine.extractPatterns();

// Print results
for (LogPattern pattern : patterns) {
    System.out.println(pattern.getSignature() + 
        " (appeared " + pattern.getSupportCount() + " times)");
}
```

Output:
```
INFO User * logged in (appeared 2 times)
ERROR Database connection failed (appeared 2 times)
```

## Configuration

### Adjusting Sensitivity

The similarity threshold controls how strict the clustering is:

```java
// Strict: Only very similar logs cluster together
LogMine logMine = new LogMine(0.8, 2);

// Balanced: Moderate similarity (recommended)
LogMine logMine = new LogMine(0.5, 2);

// Lenient: Even somewhat different logs cluster together
LogMine logMine = new LogMine(0.3, 2);
```

The second parameter is minimum cluster size - patterns that appear fewer times are filtered out.

### Streaming vs Batch Mode

```java
import org.swengdev.logmine.ProcessingMode;

// Streaming: Process logs immediately, don't store them
LogMine streaming = new LogMine(ProcessingMode.STREAMING);
streaming.addLog("Log message");
// Patterns are updated automatically

// Batch: Store logs, process later
LogMine batch = new LogMine(ProcessingMode.BATCH);
batch.addLog("Log message");
List<LogPattern> patterns = batch.extractPatterns(); // Process all at once
```

## Common Patterns

### Check for Anomalies

```java
// Extract patterns first
logMine.extractPatterns();

// Check if a new log matches known patterns
if (logMine.isAnomaly("CRITICAL SYSTEM FAILURE")) {
    // Send alert
    alertService.notify("Unknown log pattern detected");
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
    
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void processPatterns() {
        List<LogPattern> patterns = logMine.extractPatterns();
        // Store patterns, update metrics, etc.
    }
}
```

## Next Steps

- Read [CONFIGURATION_GUIDE.md](CONFIGURATION_GUIDE.md) for advanced configuration options
- See [LIBRARY_USAGE.md](LIBRARY_USAGE.md) for integration patterns
- Check [UNDERSTANDING_THE_ALGORITHM.md](UNDERSTANDING_THE_ALGORITHM.md) to understand how it works

## Troubleshooting

**Too many patterns**
- Increase similarity threshold: `new LogMine(0.7, 3)`
- Increase minimum cluster size: `new LogMine(0.5, 10)`

**Too few patterns**
- Decrease similarity threshold: `new LogMine(0.3, 2)`

**Out of memory**
- Clear periodically: `logMine.clear()`
- Use streaming mode: `new LogMine(ProcessingMode.STREAMING)`
