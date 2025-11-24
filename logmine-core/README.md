# LogMine - Fast Pattern Recognition for Log Analytics

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Build](https://img.shields.io/badge/build-passing-brightgreen.svg)]()

**Unsupervised log pattern extraction library for real-time log analytics.**

LogMine automatically discovers patterns in log data without predefined templates, making it perfect for monitoring unknown or changing log formats.

---

## 🎯 Key Features

- ✅ **Unsupervised Learning** - No templates needed, discovers patterns automatically
- ✅ **Real-Time Processing** - Streaming mode processes unlimited logs with constant memory
- ✅ **Zero Dependencies** - Core library has no external dependencies
- ✅ **Configurable** - Supports any log format (JSON, syslog, Apache, custom)
- ✅ **Thread-Safe** - Concurrent processing with ReadWriteLock
- ✅ **Stable Pattern IDs** - Content-based hashing for storage and correlation
- ✅ **Performance** - STREAMING: 8K logs/s; BATCH: 158K logs/s collection

---

## Multiple Log Sources?

Processing logs from **different sources with completely different formats**?

- **[Multi-Format Support Guide](docs/MULTI_FORMAT_SUPPORT.md)** - Handles completely different log formats
- **[Hierarchical Patterns Guide](docs/HIERARCHICAL_PATTERNS.md)** - Multiple granularity levels (coarse/medium/fine)

## 🚀 Quick Start

### CLI Tool (Easiest)

**Analyze logs from command line:**
```bash
# Install
cd logmine-cli && ../gradlew installDist

# Use it
cat application.log | logmine-cli
logmine-cli -m 0.4 application.log

# See full CLI documentation
cd logmine-cli && cat README.md
```

### Library Installation

> **Note:** Not yet published to Maven Central. For now, build from source.

**Build and Install Locally:**
```bash
./gradlew :logmine-core:publishToMavenLocal
```

**Then add to your project:**

**Gradle:**
```kotlin
repositories {
    mavenLocal()
}

dependencies {
    implementation("org.swengdev.logmine:logmine-core:unspecified")
}
```

**Maven:**
```xml
<dependency>
    <groupId>org.swengdev.logmine</groupId>
    <artifactId>logmine-core</artifactId>
    <version>unspecified</version>
</dependency>
```

### Basic Library Usage

```java
import org.swengdev.logmine.*;

// Create LogMine instance (streaming mode for production)
LogMine logMine = new LogMine(ProcessingMode.STREAMING);

// Process logs
logMine.addLog("GET /api/users/123 HTTP/1.1 200 45ms");
logMine.addLog("GET /api/users/456 HTTP/1.1 200 67ms");
logMine.addLog("POST /api/login HTTP/1.1 200 102ms");

// Get discovered patterns
List<LogPattern> patterns = logMine.getCurrentPatterns();

patterns.forEach(pattern -> {
    System.out.printf("Pattern: %s (support: %d)\n", 
        pattern.getSignature(), 
        pattern.getSupportCount());
});

// Output:
// Pattern: GET * HTTP/1.1 * * (support: 2)
// Pattern: POST * HTTP/1.1 * * (support: 1)
```

---

## How It Works

### 1. Clustering
Groups similar log messages together based on edit distance.

```
Input Logs:
  "GET /api/users/123 HTTP/1.1 200"
  "GET /api/users/456 HTTP/1.1 200"
  "POST /api/login HTTP/1.1 401"

Clusters:
  Cluster 1: [log1, log2] → Similar structure
  Cluster 2: [log3]       → Different pattern
```

### 2. Pattern Extraction
Identifies constant and variable parts within each cluster.

```
Cluster 1:
  "GET /api/users/123 HTTP/1.1 200"
  "GET /api/users/456 HTTP/1.1 200"
  
Pattern: "GET /api/users/* HTTP/1.1 *"
         (constant)  (var) (constant) (var)
```

### 3. Pattern Ranking
Sorts patterns by frequency (support count).

```
Pattern: "GET * HTTP/1.1 *"     → Support: 1250 (most common)
Pattern: "POST * HTTP/1.1 *"    → Support: 850
Pattern: "ERROR: Database *"     → Support: 120
```

---

## Use Cases

### 1. Real-Time Monitoring

```java
@Service
public class LogMonitoringService {
    private final LogMine logMine = new LogMine(ProcessingMode.STREAMING);
    
    @PostMapping("/logs/ingest")
    public void ingestLog(@RequestBody String log) {
        logMine.addLog(log);
        
        // Detect anomalies in real-time
        if (logMine.isAnomaly(log)) {
            alertService.sendAlert("Anomalous log detected: " + log);
        }
    }
    
    @GetMapping("/patterns")
    public List<LogPattern> getTopPatterns() {
        return logMine.getCurrentPatterns();
    }
}
```

### 2. Multi-Source Log Analytics

```java
// Configure for different log formats using builder
LogMineConfig apacheConfig = LogMineConfig.builder()
    .withNormalizeTimestamps(true)
    .withNormalizeIps(true)
    .withNormalizeUrls(true)
    .build();

LogMineConfig jsonConfig = LogMineConfig.builder()
    .withTokenizerStrategy(new JsonTokenizer())
    .build();

// Create separate instances for each format
LogMine apache = new LogMine(ProcessingMode.STREAMING, new LogMineProcessor(apacheConfig), 100000);
LogMine json = new LogMine(ProcessingMode.STREAMING, new LogMineProcessor(jsonConfig), 100000);

// Each handles its own format
```

### 3. Pattern IDs for Storage and Correlation

```java
// Extract patterns with stable IDs
List<LogPattern> patterns = logMine.extractPatterns();

for (LogPattern pattern : patterns) {
    String patternId = pattern.getPatternId();       // Full hash (SHA-256)
    String shortId = pattern.getShortPatternId();    // First 16 chars for display
    String signature = pattern.getSignature();       // Human-readable
    int support = pattern.getSupportCount();         // Frequency
    
    // Store in your database (PostgreSQL, MongoDB, etc.)
    // patternId is stable - same pattern = same ID
}

// Match new logs against patterns
LogPattern match = logMine.matchPattern("GET /api/users/789 HTTP/1.1 200");
if (match != null) {
    String matchedPatternId = match.getPatternId();
    // Use matchedPatternId to correlate with stored patterns
}
```

**Use cases for pattern IDs:**
- **Storage:** Persist patterns across application restarts
- **Deduplication:** Same pattern from different LogMine instances gets same ID
- **Correlation:** Link raw logs to their pattern for analytics
- **Tracking:** Monitor pattern frequency over time
- **Alerting:** Trigger alerts when specific pattern IDs appear

---

## Processing Modes

| Mode | Use Case | Memory | Storage |
|------|----------|--------|---------|
| **STREAMING** | Production | O(k) clusters | No raw logs |
| **BATCH** | Research | O(n) logs | Stores logs |

### Streaming Mode

```java
LogMine logMine = new LogMine(ProcessingMode.STREAMING);
// ✅ Constant memory usage
// ✅ Processes unlimited logs
// ✅ Real-time pattern updates
// ✅ ~8K logs/second single thread
```

### Batch Mode

```java
LogMine logMine = new LogMine(ProcessingMode.BATCH);
// ✅ Store logs for analysis
// ✅ Re-process with different parameters
// ✅ Experimentation friendly
// ⚠️ Memory grows with log count
```

---

## Configuration

### Configuration Examples

```java
// Web server logs (Apache/Nginx)
LogMineConfig webConfig = LogMineConfig.builder()
    .withNormalizeTimestamps(true)
    .withNormalizeIps(true)
    .withNormalizeUrls(true)
    .withNormalizePaths(true)
    .build();

// JSON logs
LogMineConfig jsonConfig = LogMineConfig.builder()
    .withTokenizerStrategy(new JsonTokenizer())
    .build();

// Application logs with custom variables
LogMineConfig appConfig = LogMineConfig.builder()
    .withNormalizeTimestamps(true)
    .withNormalizeNumbers(true)
    .withVariableDetector(
        CustomVariableDetector.builder()
            .addVariablePattern("\\b[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\b")
            .build()
    )
    .build();
```

### Custom Configuration

```java
LogMineConfig config = LogMineConfig.builder()
    .withSimilarityThreshold(0.6)      // Clustering strictness (0.3-0.8)
    .withMinClusterSize(3)             // Minimum logs per pattern
    .withTokenizerStrategy(new RegexTokenizer("\\s+"))
    .withVariableDetector(new StandardVariableDetector())
    .withMaxTokens(50)                 // Max tokens per log
    .build();

LogMine logMine = new LogMine(config);
```

---

## Performance Benchmarks

Real-world JMH benchmark results on Apple M-series (Java 21, single thread):

### Throughput (Operations/Second)

| Operation | Mode | 1K logs | 10K logs | 50K logs | Notes |
|-----------|------|---------|----------|----------|-------|
| **addLogsBulk** | BATCH | 158,327 | 105,163 | 43,381 | Ultra-fast collection |
| **addLogsBulk** | STREAMING | 7.7 | 0.78 | 0.16 | Real-time processing |
| **addLogsIncremental** | BATCH | 171 | 17 | 3.4 | One-by-one collection |
| **addLogsIncremental** | STREAMING | 7.9 | 0.78 | 0.16 | One-by-one processing |
| **extractPatterns** | STREAMING | 7.9 | 0.78 | 0.16 | Always fresh |
| **matchNewLogs** | STREAMING | 14.6 | 1.4 | 0.29 | Fast pattern matching |

### What This Means

**STREAMING Mode (Production):**
- ✅ **~8,000 logs/second** sustained throughput
- ✅ Constant memory usage (O(k) clusters)
- ✅ Real-time pattern updates
- ✅ Perfect for live monitoring

**BATCH Mode (Analysis):**
- ✅ **158K logs/second** collection (just stores)
- ✅ ~2-8 ops/second for full pattern extraction
- ✅ Best for historical analysis
- ⚠️ Memory grows with log count

### Real-World Performance

**Single Thread (Actual Benchmarks):**
```
STREAMING: ~8,000 logs/second
BATCH collection: ~158,000 logs/second (storage only)
BATCH processing: ~500-2,000 logs/second (full pattern extraction)
```

**Multi-threaded (Estimated):**
```
4 cores: ~25-30K logs/s STREAMING (with proper parallelization)
Note: Requires proper work distribution and thread-safe usage
```

**Typical Latencies:**
```
addLog() STREAMING: ~0.13 ms per log
addLog() BATCH: ~0.006 ms per log (just storage)
getCurrentPatterns(): Instant (cached)
matchPattern(): ~0.07 ms per match
```

### Bottleneck

Performance is limited by the **clustering algorithm** (O(n×m)):
- Each log is compared against existing clusters
- Edit distance calculations are CPU-intensive
- This is inherent to the LogMine algorithm

To improve throughput:
- Use multiple threads/instances
- Deploy across multiple nodes
- Consider approximate matching for extreme scale

### Benchmark Details

Full benchmarks available in `logmine-benchmarks/`:
```bash
./gradlew :logmine-benchmarks:jmh
```

Environment:
- JVM: OpenJDK 21 (Amazon Corretto)
- JMH: 1.37 (2 forks, 3 warmup, 5 measurement iterations)
- Hardware: Apple M-series processor

---

## 📚 Documentation

### Quick Links

- **[Quick Start Guide](docs/QUICK_START.md)** - Get started in 5 minutes
- **[Configuration Guide](docs/CONFIGURATION_GUIDE.md)** - Detailed configuration options
- **[Processing Modes](docs/PROCESSING_MODES.md)** - Streaming vs Batch explained

### Advanced

- **[Understanding the Algorithm](docs/UNDERSTANDING_THE_ALGORITHM.md)** - Deep dive into LogMine
- **[Library Usage](docs/LIBRARY_USAGE.md)** - Advanced usage patterns
- **[Multi-Format Support](docs/MULTI_FORMAT_SUPPORT.md)** - Handle different log formats
- **[Hierarchical Patterns](docs/HIERARCHICAL_PATTERNS.md)** - Multiple granularity levels

### Optional Modules

**LogMine Benchmarks** (`logmine-benchmarks/`) - JMH performance benchmarks:
- Real-world throughput measurements
- STREAMING vs BATCH comparisons
- Performance baseline tracking
- See [BENCHMARKS.md](../logmine-benchmarks/BENCHMARKS.md) for details

---

## Examples

### Example 1: Pattern Discovery

```java
LogMine logMine = new LogMine(ProcessingMode.STREAMING);

// Process sample logs
String[] logs = {
    "INFO: User login successful user_id=123",
    "INFO: User login successful user_id=456",
    "ERROR: Database timeout host=db1.example.com",
    "ERROR: Database timeout host=db2.example.com",
    "WARN: High memory usage heap=1.8GB"
};

for (String log : logs) {
    logMine.addLog(log);
}

// Get patterns
List<LogPattern> patterns = logMine.getCurrentPatterns();

// Output:
// Pattern: INFO: User login successful user_id=* (support: 2)
// Pattern: ERROR: Database timeout host=* (support: 2)
// Pattern: WARN: High memory usage heap=* (support: 1)
```

### Example 2: Anomaly Detection

```java
// Train on normal logs
LogMine logMine = new LogMine();
List<String> normalLogs = loadTrainingData();
normalLogs.forEach(logMine::addLog);

// Extract patterns
List<LogPattern> patterns = logMine.extractPatterns();

// Check new logs
String newLog = "CRITICAL: Nuclear meltdown imminent!";
boolean isAnomaly = logMine.isAnomaly(newLog);

if (isAnomaly) {
    alertService.sendAlert("Anomalous log detected!");
}
```

### Example 3: Multi-Format Processing

```java
// JSON logs with JsonTokenizer
LogMineConfig jsonConfig = LogMineConfig.builder()
    .withTokenizerStrategy(new JsonTokenizer())
    .build();
LogMine jsonMine = new LogMine(ProcessingMode.STREAMING, new LogMineProcessor(jsonConfig), 100000);
jsonMine.addLog("{\"level\":\"INFO\",\"msg\":\"Request processed\"}");

// Web server logs with normalization
LogMineConfig webConfig = LogMineConfig.builder()
    .withNormalizeIps(true)
    .withNormalizeUrls(true)
    .build();
LogMine webMine = new LogMine(ProcessingMode.STREAMING, new LogMineProcessor(webConfig), 100000);
webMine.addLog("127.0.0.1 - - [10/Oct/2023:13:55:36] \"GET /index.html HTTP/1.1\" 200");

// Each extracts patterns specific to its format
```

---

## Testing

```bash
# Run tests
./gradlew test

# Run specific test
./gradlew test --tests LogMineTest

# Generate coverage report
./gradlew jacocoTestReport
```

---

## Building

```bash
# Build library JAR
./gradlew build

# Install to local Maven repository
./gradlew publishToMavenLocal

# Generate documentation
./gradlew javadoc
```

**Output:** `build/libs/logmine-1.0.0.jar`

---

## Contributing

We welcome contributions! Areas for improvement:

- Additional tokenizer strategies
- More variable detectors
- Performance optimizations
- Reference implementations for storage systems
- Additional examples

---

## License

Copyright 2024 Zachary Huang

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

See [LICENSE](LICENSE) file for full details.

---

## 🎓 Research & Citation

This library implements the algorithm described in:

**Hamooni, H., Debnath, B., Xu, J., Zhang, H., Jiang, G., & Mueen, A. (2016).** *LogMine: Fast Pattern Recognition for Log Analytics.* In Proceedings of the 25th ACM International Conference on Information and Knowledge Management (CIKM '16), October 24-28, 2016, Indianapolis, IN, USA. ACM, New York, NY, USA, 10 pages. DOI: [10.1145/2983323.2983358](https://doi.org/10.1145/2983323.2983358)

### BibTeX Citation

```bibtex
@inproceedings{hamooni2016logmine,
  title={LogMine: Fast Pattern Recognition for Log Analytics},
  author={Hamooni, Hossein and Debnath, Biplob and Xu, Jianwu and Zhang, Hui and Jiang, Guofei and Mueen, Abdullah},
  booktitle={Proceedings of the 25th ACM International on Conference on Information and Knowledge Management},
  pages={1573--1582},
  year={2016},
  organization={ACM},
  doi={10.1145/2983323.2983358},
  url={https://www.cs.unm.edu/~mueen/Papers/LogMine.pdf}
}
```

### Abstract

LogMine is an unsupervised pattern recognition algorithm for log analytics that:
- Uses a novel clustering method based on weighted edit distance
- Extracts patterns automatically without predefined templates
- Achieves sub-linear time complexity for pattern extraction
- Supports hierarchical pattern discovery

### Key Contributions

1. **Weighted Edit Distance** - Accounts for log message structure
2. **Online Clustering** - Processes logs in streaming fashion
3. **Hierarchical Patterns** - Multiple levels of abstraction
4. **Fast Performance** - Scalable to large log volumes

### Paper Links

- **PDF:** [https://www.cs.unm.edu/~mueen/Papers/LogMine.pdf](https://www.cs.unm.edu/~mueen/Papers/LogMine.pdf)
- **DOI:** [https://doi.org/10.1145/2983323.2983358](https://doi.org/10.1145/2983323.2983358)
- **ACM DL:** [ACM Digital Library](https://dl.acm.org/doi/10.1145/2983323.2983358)

---

## 🙏 Acknowledgments

- **Hamooni et al.** for the original LogMine algorithm and research paper
- **All contributors and users** who help improve this library

### Credits

This implementation is based on the algorithm described in the research paper but includes:
- Modern Java implementation
- Production-ready features (thread-safety, streaming mode)
- Flexible configuration system
- Comprehensive test suite

---

## 📮 Contact & Support

- **Issues:** [GitHub Issues](https://github.com/swcraftsmen/logmine/issues)
- **Documentation:** [docs/](docs/)

---

## 🚀 Quick Links

| Resource | Link |
|----------|------|
| **Quick Start** | [docs/QUICK_START.md](docs/QUICK_START.md) |
| **Configuration** | [docs/CONFIGURATION_GUIDE.md](docs/CONFIGURATION_GUIDE.md) |
| **Examples** | [logmine-examples/](logmine-examples/) |
| **Performance** | [docs/PROCESSING_MODES.md](docs/PROCESSING_MODES.md) |

