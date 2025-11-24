# LogMine Benchmarks

Performance benchmarks for LogMine using JMH (Java Microbenchmark Harness).

## Running Benchmarks

```bash
# Run all benchmarks
./gradlew :logmine-benchmarks:jmh

# Run specific benchmark
./gradlew :logmine-benchmarks:jmh -Pjmh.includes='.*extractPatterns.*'

# Quick run (fewer iterations)
./gradlew :logmine-benchmarks:jmh -Pjmh.warmupIterations=1 -Pjmh.iterations=2
```

## What's Measured

### End-to-End Performance
- **extractPatterns**: Core algorithm throughput (logs/second)
- **addLogsIncremental**: Streaming mode performance
- **matchNewLogs**: Pattern matching speed for real-time classification
- **extractPatternsWithStats**: Complete workflow with statistics

### Test Parameters
- **Log Volume**: 1K, 10K, 50K logs
- **Processing Mode**: BATCH vs STREAMING
- **Log Distribution**: 80% common patterns, 20% rare (realistic workload)

## Results Location

After running benchmarks:
```
build/reports/jmh/results.json    # Machine-readable results
build/results/jmh/                # Human-readable results
```

## Expected Performance

Actual benchmark results (Apple M-series, Java 21):
- **Throughput**: ~8,000 logs/second STREAMING (single thread)
- **Scalability**: O(n×m) - inherent to clustering algorithm
- **Memory**: Constant for STREAMING mode

## Interpreting Results

JMH reports throughput in ops/sec:
- **Higher is better** for throughput benchmarks
- Look at **Score** column for average performance
- Check **Error** margin for variance
- Compare **BATCH vs STREAMING** modes for your use case

## Baseline Results

Run `./gradlew :logmine-benchmarks:jmh` on your hardware to establish baselines.
Store results to detect performance regressions in CI/CD.

## Tips

1. **Close other applications** before benchmarking
2. **Run multiple times** - results vary based on JVM warmup
3. **Use fixed CPU frequency** if possible (disable turbo boost)
4. **Don't run on laptops** with power saving mode


