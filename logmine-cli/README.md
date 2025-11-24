# LogMine CLI - Log Pattern Analyzer

A command-line tool for analyzing log files and identifying common patterns using unsupervised clustering.

## Features

- ✅ **Stdin/File Input** - Read from pipes or files
- ✅ **Colorful Output** - ANSI colored terminal output for better readability  
- ✅ **Custom Variables** - Define regex patterns to normalize logs
- ✅ **Flexible Clustering** - Adjust granularity with `--max-dist`
- ✅ **Multiple Formats** - JSON output for programmatic use
- ✅ **Fast Processing** - Powered by the LogMine library
- ✅ **Zero Config** - Works out of the box with sensible defaults

## Installation

### From Source

```bash
cd logmine-cli
../gradlew installDist
```

The executable will be in `build/install/logmine-cli/bin/logmine-cli`

### Build Uber JAR

```bash
../gradlew jar
java -jar build/libs/logmine-cli-1.0.0.jar --help
```

## Quick Start

### Basic Usage

```bash
# Analyze logs from stdin
cat myapp.log | logmine-cli

# Analyze log files directly
logmine-cli application.log

# Analyze multiple files
logmine-cli app1.log app2.log app3.log
```

### Example Output

```
══════════════════════════════════════════════════════════
  LogMine Analysis Results
══════════════════════════════════════════════════════════
 Total messages: 1000
 Clusters found: 5

Cluster #1 │ 450 messages
  Pattern: User <*> logged in from <*>
  ID: a3f9c2

Cluster #2 │ 320 messages
  Pattern: Error processing request <*> status code <*>
  ID: b7d4e1

Cluster #3 │ 150 messages
  Pattern: Database query took <*> ms
  ID: c8e5f2
```

## Command-Line Options

### Input Options

```bash
logmine-cli [FILES...]         # Analyze one or more files
cat file.log | logmine-cli     # Read from stdin (no files specified)
```

### Clustering Options

```bash
-m, --max-dist <THRESHOLD>
    Similarity threshold for clustering (0.0-1.0)
    Lower values create more granular clusters
    Default: 0.6
    
    Example:
      logmine-cli -m 0.3 app.log    # More detailed clusters
      logmine-cli -m 0.8 app.log    # Broader clusters
```

### Variable Definition

```bash
-v, --variables <VAR>
    Define custom variables to normalize logs before clustering
    Format: 'name:/regex/'
    Can be specified multiple times
    
    Examples:
      # Replace timestamps
      logmine-cli -v '<time>:/\d{2}:\d{2}:\d{2}/' app.log
      
      # Replace IP addresses
      logmine-cli -v '<ip>:/\d+\.\d+\.\d+\.\d+/' app.log
      
      # Multiple variables
      logmine-cli \
        -v '<time>:/\d{2}:\d{2}:\d{2}/' \
        -v '<ip>:/\d+\.\d+\.\d+\.\d+/' \
        -v '<uuid>:/[0-9a-f-]{36}/' \
        app.log
```

### Output Options

```bash
-i, --min-members <COUNT>
    Minimum number of messages in a cluster to display
    Default: 2
    
    Example:
      logmine-cli -i 10 app.log     # Only show clusters with 10+ messages

-p, --pattern-placeholder <TEXT>
    Use custom placeholder for variable parts
    Default: <*>
    
    Example:
      logmine-cli -p '___' app.log
      # Output: User ___ logged in from ___

-s, --sorted <ORDER>
    Sort clusters by size: 'desc' (default) or 'asc'
    
    Example:
      logmine-cli -s asc app.log    # Show smallest clusters first

--max-patterns <COUNT>
    Limit number of patterns displayed
    
    Example:
      logmine-cli --max-patterns 10 app.log

--no-color
    Disable ANSI colored output
    
    Example:
      logmine-cli --no-color app.log > report.txt

--json
    Output results in JSON format
    
    Example:
      logmine-cli --json app.log | jq '.patterns[0]'
```

### Utility Options

```bash
-h, --help              Show help message
-V, --version           Show version information
--verbose               Show detailed processing information
```

## Usage Examples

### Example 1: Basic Log Analysis

```bash
cat /var/log/application.log | logmine-cli
```

### Example 2: Fine-Grained Clustering

```bash
logmine-cli -m 0.2 application.log
```

Lower `max-dist` creates more specific patterns:
- Good for: Detailed debugging, finding rare patterns
- Trade-off: More clusters, potentially fragmented

### Example 3: Normalizing Timestamps and IPs

```bash
logmine-cli \
  -v '<time>:/\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}/' \
  -v '<ip>:/\d+\.\d+\.\d+\.\d+/' \
  webserver.log
```

**Before normalization:**
```
2024-01-15 10:30:45 Request from 192.168.1.100
2024-01-15 10:30:46 Request from 192.168.1.101
2024-01-15 10:30:47 Request from 10.0.0.50
```

**After normalization (grouped into one pattern):**
```
Cluster #1 │ 3 messages
  Pattern: <time> Request from <ip>
```

### Example 4: JSON Output for Integration

```bash
logmine-cli --json app.log | jq '.'
```

Output:
```json
{
  "total_messages": 1000,
  "clusters_found": 5,
  "patterns": [
    {
      "pattern_id": "a3f9c2d1",
      "pattern": "User <*> logged in from <*>",
      "count": 450,
      "percentage": 45.00
    },
    {
      "pattern_id": "b7d4e1f8",
      "pattern": "Error processing request <*>",
      "count": 320,
      "percentage": 32.00
    }
  ]
}
```

### Example 5: Filtering Small Clusters

```bash
logmine-cli -i 50 application.log
```

Only shows patterns that appear 50+ times, hiding noise.

### Example 6: Real-Time Log Monitoring

```bash
tail -f /var/log/application.log | logmine-cli --verbose
```

Analyze logs as they arrive (batch processing every N lines for efficiency).

### Example 7: Custom Placeholders

```bash
logmine-cli -p '---' application.log
```

Output:
```
Pattern: User --- logged in from ---
Pattern: Error processing request --- status code ---
```

### Example 8: Multiple Log Files

```bash
logmine-cli \
  /var/log/app1.log \
  /var/log/app2.log \
  /var/log/app3.log
```

Analyzes all files together, finding common patterns across sources.

## Advanced Patterns

### Apache/Nginx Access Logs

```bash
logmine-cli \
  -m 0.4 \
  -v '<ip>:/\d+\.\d+\.\d+\.\d+/' \
  -v '<timestamp>:/\[.*?\]/' \
  -v '<url>:/"[^"]*"/' \
  -v '<status>:/\s\d{3}\s/' \
  -v '<size>:/\s\d+$/' \
  /var/log/nginx/access.log
```

### Application Logs with Stack Traces

```bash
logmine-cli \
  -m 0.5 \
  -v '<timestamp>:/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}/' \
  -v '<level>:/ERROR|WARN|INFO|DEBUG/' \
  -v '<exception>:/\w+Exception/' \
  -v '<line>:/:\d+/' \
  application.log
```

### JSON Logs

For JSON logs, pre-process with `jq`:

```bash
cat app.json | jq -r '.message' | logmine-cli -m 0.3
```

## Performance Tips

1. **Use Variables**: Normalize high-cardinality fields (IDs, timestamps) to reduce clusters
2. **Adjust max-dist**: Start with 0.6, tune based on your log structure
3. **Filter Early**: Use `grep` to filter logs before analysis for faster processing
4. **Batch Processing**: For huge files, split and analyze in chunks

```bash
# Fast analysis of specific error patterns
grep "ERROR" huge-app.log | logmine-cli -m 0.4
```

## Integration Examples

### Shell Script for Daily Reports

```bash
#!/bin/bash
DATE=$(date +%Y-%m-%d)
LOG_FILE="/var/log/app.log"
REPORT_FILE="/var/reports/patterns-$DATE.txt"

logmine-cli \
  --no-color \
  -m 0.5 \
  -i 10 \
  "$LOG_FILE" > "$REPORT_FILE"

echo "Report saved to $REPORT_FILE"
```

## Troubleshooting

### Colors Not Showing on Windows

```bash
# Install Windows Terminal or use:
logmine-cli --no-color app.log
```

### Out of Memory for Large Files

```bash
# Process in chunks
split -l 100000 huge.log chunk-
for f in chunk-*; do logmine-cli "$f" >> results.txt; done
rm chunk-*
```

### Permission Denied

```bash
chmod +x logmine-cli
```

## Development

### Build from Source

```bash
git clone https://github.com/swcraftsmen/logmine
cd logmine/logmine-cli
../gradlew build
```

### Run Tests

```bash
../gradlew test
```

### Create Distribution

```bash
../gradlew installDist
# Executable in: build/install/logmine-cli/bin/
```

## License

Apache License 2.0 - see [LICENSE](../LICENSE) for details.

## Related Projects

- **LogMine Core Library**: [../README.md](../README.md)
- **LogMine Examples**: [../logmine-examples/](../logmine-examples/)
- **Original Python CLI**: https://github.com/trungdq88/logmine
- **Research Paper**: https://www.cs.unm.edu/~mueen/Papers/LogMine.pdf

## Support

- **Issues**: https://github.com/swcraftsmen/logmine/issues
- **Discussions**: https://github.com/swcraftsmen/logmine/discussions
- **Documentation**: https://swengdev.org

---

**Happy Log Mining!** 🪵⛏️

