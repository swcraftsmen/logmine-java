# Configuration Guide

LogMine is designed to work well with default settings, but you can customize its behavior for your specific log types.

## Basic Configuration

### Default Settings

```java
// Uses default similarity threshold (0.5) and minimum cluster size (2)
LogMineProcessor processor = new LogMineProcessor();
```

### Custom Thresholds

```java
// Constructor: threshold, min cluster size
LogMineProcessor processor = new LogMineProcessor(0.7, 5);
```

### Using Config Builder

```java
LogMineConfig config = LogMineConfig.builder()
    .withSimilarityThreshold(0.6)
    .withMinClusterSize(3)
    .maxClusters(1000)
    .build();

LogMineProcessor processor = new LogMineProcessor(config);
```

## Configuration Parameters

### Similarity Threshold

Controls how similar logs must be to cluster together.

**Range:** 0.0 to 1.0

- **0.8-1.0:** Strict - only very similar logs cluster (many specific patterns)
- **0.5-0.7:** Balanced - good for most use cases
- **0.3-0.4:** Lenient - groups more logs together (fewer general patterns)

```java
LogMineConfig config = LogMineConfig.builder()
    .withSimilarityThreshold(0.65)
    .build();
```

### Minimum Cluster Size

Filters out rare patterns.

```java
LogMineConfig config = LogMineConfig.builder()
    .withMinClusterSize(5)  // Only keep patterns that appear 5+ times
    .build();
```

### Max Clusters

Limits the number of clusters to prevent unbounded growth.

```java
LogMineConfig config = LogMineConfig.builder()
    .maxClusters(500)  // Cap at 500 clusters
    .build();
```

## Tokenizer Strategies

Tokenizers split log messages into pieces for comparison.

### WhitespaceTokenizer (Default)

Splits on whitespace - good for most logs.

```java
import org.swengdev.logmine.strategy.WhitespaceTokenizer;

LogMineConfig config = LogMineConfig.builder()
    .withTokenizerStrategy(new WhitespaceTokenizer())
    .build();
```

Input: `2024-01-15 INFO User logged in`  
Tokens: `["2024-01-15", "INFO", "User", "logged", "in"]`

### DelimiterPreservingTokenizer

Preserves delimiters like `=`, `:`, etc. Useful for key-value logs.

```java
import org.swengdev.logmine.strategy.DelimiterPreservingTokenizer;

// Default delimiters: = : , ; | [ ] ( ) { }
LogMineConfig config = LogMineConfig.builder()
    .withTokenizerStrategy(new DelimiterPreservingTokenizer())
    .build();

// Custom delimiters
config = LogMineConfig.builder()
    .withTokenizerStrategy(new DelimiterPreservingTokenizer("=,:"))
    .build();
```

Input: `action=login user=alice`  
Tokens: `["action", "=", "login", "user", "=", "alice"]`

### RegexTokenizer

Most flexible - use custom regex.

```java
import org.swengdev.logmine.strategy.RegexTokenizer;

// Split on commas and spaces
LogMineConfig config = LogMineConfig.builder()
    .withTokenizerStrategy(new RegexTokenizer("[,\\s]+"))
    .build();
```

### JsonTokenizer

For JSON-formatted logs.

```java
import org.swengdev.logmine.strategy.JsonTokenizer;

LogMineConfig config = LogMineConfig.builder()
    .withTokenizerStrategy(new JsonTokenizer())
    .build();
```

Input: `{"level":"INFO","user":"alice"}`  
Tokens: `["{", "level", ":", "INFO", ",", "user", ":", "alice", "}"]`

## Variable Detectors

Variable detectors identify which parts of logs should be treated as wildcards.

### StandardVariableDetector (Default)

Detects common variable types: numbers, timestamps, IPs, UUIDs, hashes.

```java
import org.swengdev.logmine.strategy.StandardVariableDetector;

LogMineConfig config = LogMineConfig.builder()
    .withVariableDetector(new StandardVariableDetector())
    .build();
```

**Customization:**

```java
// Only detect numbers and timestamps
new StandardVariableDetector(
    true,   // detectNumbers
    true,   // detectTimestamps
    false,  // detectIPs
    false,  // detectUUIDs
    false   // detectHashes
)
```

### CustomVariableDetector

Define your own patterns.

```java
import org.swengdev.logmine.strategy.CustomVariableDetector;

CustomVariableDetector detector = new CustomVariableDetector.Builder()
    .addVariablePattern("\\d+")           // Numbers
    .addVariablePattern("user:\\w+")      // User IDs
    .addConstantToken("GET")
    .addConstantToken("POST")
    .setDefaultToVariable(false)
    .build();

LogMineConfig config = LogMineConfig.builder()
    .withVariableDetector(detector)
    .build();
```

### NeverVariableDetector

Treats everything as constant - no wildcards.

```java
import org.swengdev.logmine.strategy.NeverVariableDetector;

LogMineConfig config = LogMineConfig.builder()
    .withVariableDetector(new NeverVariableDetector())
    .withSimilarityThreshold(1.0)  // Exact matching
    .build();
```

### AlwaysVariableDetector

Treats everything as variable - maximum wildcards.

```java
import org.swengdev.logmine.strategy.AlwaysVariableDetector;

LogMineConfig config = LogMineConfig.builder()
    .withVariableDetector(new AlwaysVariableDetector())
    .build();
```

## Preprocessing

LogMine can normalize logs before processing to improve clustering.

```java
LogMineConfig config = LogMineConfig.builder()
    .normalizeTimestamps(true)  // Replace timestamps with TIMESTAMP
    .normalizeIPs(true)          // Replace IPs with IP_ADDR
    .normalizeNumbers(true)      // Replace numbers with NUM
    .normalizePaths(true)        // Replace file paths with PATH
    .normalizeUrls(true)         // Replace URLs with URL
    .build();
```

**Example:**
```
Before: "2024-01-15 10:30:00 User 12345 from 192.168.1.1 accessed /api/users"
After:  "TIMESTAMP User NUM from IP_ADDR accessed PATH"
```

## Processing Modes

### Batch Mode (Default)

Stores logs in memory, processes them together.

```java
LogMineConfig config = LogMineConfig.builder()
    .processingMode(ProcessingMode.BATCH)
    .build();
```

### Streaming Mode

Processes logs immediately without storing them.

```java
LogMineConfig config = LogMineConfig.builder()
    .processingMode(ProcessingMode.STREAMING)
    .build();
```

## Hierarchical Patterns

Extract patterns at multiple levels of detail.

```java
LogMineConfig config = LogMineConfig.builder()
    .enableHierarchicalPatterns(true)
    .addHierarchyThreshold(0.4)  // Coarse
    .addHierarchyThreshold(0.6)  // Medium
    .addHierarchyThreshold(0.8)  // Fine
    .build();

LogMineProcessor processor = new LogMineProcessor(config);
processor.process(logs);

List<HierarchicalPattern> patterns = processor.extractHierarchicalPatterns();
```

## Real-World Examples

### Web Server Logs

```java
LogMineConfig config = LogMineConfig.builder()
    .withSimilarityThreshold(0.7)
    .withMinClusterSize(5)
    .withTokenizerStrategy(new WhitespaceTokenizer())
    .normalizeTimestamps(true)
    .normalizeIPs(true)
    .normalizeNumbers(true)
    .build();
```

### JSON Application Logs

```java
LogMineConfig config = LogMineConfig.builder()
    .withSimilarityThreshold(0.6)
    .withMinClusterSize(3)
    .withTokenizerStrategy(new JsonTokenizer())
    .withVariableDetector(new StandardVariableDetector())
    .build();
```

### Key-Value Logs

```java
LogMineConfig config = LogMineConfig.builder()
    .withSimilarityThreshold(0.6)
    .withTokenizerStrategy(new DelimiterPreservingTokenizer("=:,"))
    .normalizeNumbers(true)
    .build();
```

### Security Logs

```java
// Keep rare patterns (potential threats), be strict
LogMineConfig config = LogMineConfig.builder()
    .withSimilarityThreshold(0.8)
    .withMinClusterSize(1)  // Don't filter rare events
    .withVariableDetector(new StandardVariableDetector(
        true,  // numbers (ports, IDs)
        true,  // timestamps
        true,  // IPs
        false, // UUIDs
        true   // hashes
    ))
    .build();
```

## Tips

1. **Start with defaults** and adjust based on results
2. **Test with sample data** before processing millions of logs
3. **Monitor pattern count** - if you get thousands of patterns, increase threshold or min cluster size
4. **Use preprocessing** for logs with lots of variable data (IPs, timestamps, etc.)
5. **Document your config** - future maintainers will thank you

## Troubleshooting

**Problem: Too many patterns**

Solutions:
- Increase `withSimilarityThreshold()`
- Increase `withMinClusterSize()`
- Enable preprocessing (`normalizeTimestamps()`, etc.)

**Problem: Too few patterns**

Solutions:
- Decrease `withSimilarityThreshold()`
- Check if tokenizer is splitting correctly
- Verify variable detector isn't too aggressive

**Problem: Patterns don't make sense**

Solutions:
- Print tokenizer output to debug: `tokenizer.tokenize(log)`
- Review variable detector rules
- Try different tokenizer strategies
