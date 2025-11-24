# LogMine Core Documentation

This directory contains documentation for using LogMine as a library.

## Getting Started

Start here if you're new to LogMine:

1. **[QUICK_START.md](QUICK_START.md)** - Get running in 10 minutes
2. **[LIBRARY_USAGE.md](LIBRARY_USAGE.md)** - Integration patterns and API reference
3. **[CONFIGURATION_GUIDE.md](CONFIGURATION_GUIDE.md)** - Configuration options explained

## Understanding LogMine

**[UNDERSTANDING_THE_ALGORITHM.md](UNDERSTANDING_THE_ALGORITHM.md)** - How the algorithm works internally

## Advanced Topics

- **[PROCESSING_MODES.md](PROCESSING_MODES.md)** - Streaming vs Batch mode explained
- **[HIERARCHICAL_PATTERNS.md](HIERARCHICAL_PATTERNS.md)** - Multi-level pattern extraction
- **[MULTI_FORMAT_SUPPORT.md](MULTI_FORMAT_SUPPORT.md)** - Handling different log formats

## Quick Reference

### Basic Usage

```java
import org.swengdev.logmine.LogMine;
import org.swengdev.logmine.LogPattern;

// Initialize
LogMine logMine = new LogMine();

// Add logs
logMine.addLog("2024-01-15 INFO User logged in");

// Extract patterns
List<LogPattern> patterns = logMine.extractPatterns();
```

### Custom Configuration

```java
import org.swengdev.logmine.LogMineConfig;
import org.swengdev.logmine.LogMineProcessor;

LogMineConfig config = LogMineConfig.builder()
    .withSimilarityThreshold(0.6)
    .withMinClusterSize(5)
    .normalizeTimestamps(true)
    .normalizeIPs(true)
    .build();

LogMineProcessor processor = new LogMineProcessor(config);
```

### Streaming Mode

```java
import org.swengdev.logmine.ProcessingMode;

LogMine streaming = new LogMine(ProcessingMode.STREAMING);
streaming.addLog("Log message");
// Patterns updated automatically
```

## API Summary

### Main Classes

- **`LogMine`** - High-level facade, recommended for most users
- **`LogMineProcessor`** - Lower-level API for advanced use cases
- **`LogMineConfig`** - Configuration builder

### Result Classes

- **`LogPattern`** - Extracted pattern with support count and specificity
- **`HierarchicalPattern`** - Multi-level patterns

### Strategy Interfaces

- **`TokenizerStrategy`** - How to split logs into tokens
- **`VariableDetector`** - What parts are variable vs constant

### Built-in Implementations

**Tokenizers:**
- `WhitespaceTokenizer` - Split on spaces (default)
- `RegexTokenizer` - Custom regex splitting
- `JsonTokenizer` - Parse JSON logs
- `DelimiterPreservingTokenizer` - Preserve delimiters like `=`, `:`

**Variable Detectors:**
- `StandardVariableDetector` - Detect numbers, IPs, timestamps, UUIDs (default)
- `CustomVariableDetector` - User-defined patterns
- `AlwaysVariableDetector` - Everything is variable
- `NeverVariableDetector` - Nothing is variable

## Common Questions

**Q: How do I choose the similarity threshold?**

A: Start with 0.5 (balanced). If you get too many patterns, increase to 0.7. If too few, decrease to 0.3.

**Q: Should I use streaming or batch mode?**

A: Use streaming for production (real-time, constant memory). Use batch for analysis/research (can reprocess).

**Q: How often should I extract patterns?**

A: Not on every log! Use a schedule (every 5 minutes) or threshold (every 1000 logs).

**Q: How do I handle different log formats?**

A: Create separate LogMine instances for each format, or use different tokenizers/detectors.

## Examples

See the test files in `src/test/java/` for working examples:

- `LogMineTest.java` - Basic usage examples
- `LogMineProcessorTest.java` - Advanced processing
- `LogMineConfigTest.java` - Configuration examples

## Support

For issues or questions:
- Check the documentation in this directory
- Review test files for examples
- See the main README in the project root

## License

Apache License 2.0 - See LICENSE file in project root



