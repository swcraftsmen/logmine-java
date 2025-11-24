package org.swengdev.logmine.benchmarks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.swengdev.logmine.LogMine;
import org.swengdev.logmine.LogMineConfig;
import org.swengdev.logmine.ProcessingMode;

/**
 * End-to-end benchmarks for LogMine.
 *
 * <p>Measures real-world performance scenarios:
 *
 * <ul>
 *   <li>Batch processing (analyze historical logs)
 *   <li>Streaming processing (real-time log analysis)
 *   <li>Pattern extraction throughput
 *   <li>Memory efficiency
 * </ul>
 *
 * <p>Based on the LogMine paper (CIKM 2016) performance evaluation.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
@Fork(2)
public class LogMineBenchmark {

  // Realistic log patterns from common applications
  private static final String[] LOG_TEMPLATES = {
    "2024-11-24 12:34:56 INFO User %s logged in from %s",
    "2024-11-24 12:34:57 WARN Failed login attempt for user %s from %s",
    "2024-11-24 12:34:58 DEBUG Database query executed in %dms: SELECT * FROM users WHERE id=%d",
    "2024-11-24 12:34:59 INFO HTTP %s %s responded with status %d in %dms",
    "2024-11-24 12:35:00 DEBUG Cache miss for key %s, fetching from database",
    "2024-11-24 12:35:01 INFO Memory usage: %dMB heap, %dMB off-heap",
    "2024-11-24 12:35:02 INFO Processing request %s for user %s",
    "2024-11-24 12:35:03 INFO Transaction %s completed successfully in %dms",
    "2024-11-24 12:35:04 ERROR Error processing order %s: %s",
    "2024-11-24 12:35:05 INFO Background job %s started",
  };

  @Param({"1000", "10000", "50000"})
  private int logCount;

  @Param({"BATCH", "STREAMING"})
  private ProcessingMode mode;

  private List<String> logs;
  private LogMine logMine;

  @Setup(Level.Trial)
  public void setupTrial() {
    // Use public constructor - LogMine with ProcessingMode
    logMine = new LogMine(mode);
  }

  @Setup(Level.Iteration)
  public void setupIteration() {
    logs = generateRealisticLogs(logCount);
    logMine.clear(); // Reset state between iterations
  }

  /**
   * Benchmark: Extract patterns from logs (primary use case)
   *
   * <p>This measures the core algorithm performance as described in the paper.
   */
  @Benchmark
  public void extractPatterns(Blackhole blackhole) {
    logMine.addLogs(logs);
    var patterns = logMine.extractPatterns();
    blackhole.consume(patterns);
  }

  /**
   * Benchmark: Add logs incrementally one-at-a-time (streaming scenario)
   *
   * <p>Simulates real-time log ingestion where logs arrive individually.
   */
  @Benchmark
  public void addLogsIncremental(Blackhole blackhole) {
    for (String log : logs) {
      logMine.addLog(log);
    }
    var patterns = logMine.getCurrentPatterns();
    blackhole.consume(patterns);
  }

  /**
   * Benchmark: Add logs in bulk (optimized streaming scenario)
   *
   * <p>Simulates batch ingestion where logs are added in bulk. In STREAMING mode, this should be
   * significantly faster than addLogsIncremental() because patterns are updated once instead of
   * checking on every log.
   */
  @Benchmark
  public void addLogsBulk(Blackhole blackhole) {
    logMine.addLogs(logs);
    var patterns = logMine.getCurrentPatterns();
    blackhole.consume(patterns);
  }

  /**
   * Benchmark: Pattern matching for incoming logs
   *
   * <p>After building patterns, how fast can we classify new logs?
   */
  @Benchmark
  public void matchNewLogs(Blackhole blackhole) {
    // Pre-build patterns
    logMine.addLogs(logs.subList(0, logs.size() / 2));
    logMine.extractPatterns();

    // Benchmark matching against new logs
    for (int i = logs.size() / 2; i < logs.size(); i++) {
      var pattern = logMine.matchPattern(logs.get(i));
      blackhole.consume(pattern);
    }
  }

  /**
   * Benchmark: End-to-end with stats
   *
   * <p>Realistic scenario: extract patterns and get statistics.
   */
  @Benchmark
  public void extractPatternsWithStats(Blackhole blackhole) {
    logMine.addLogs(logs);
    var patterns = logMine.extractPatterns();
    var stats = logMine.getStats();
    blackhole.consume(patterns);
    blackhole.consume(stats);
  }

  /** Generate realistic logs with variety and noise */
  private List<String> generateRealisticLogs(int count) {
    List<String> result = new ArrayList<>(count);
    Random random = new Random(42); // Fixed seed for reproducibility

    // 80% common patterns, 20% rare patterns (realistic distribution)
    int commonCount = (int) (count * 0.8);

    for (int i = 0; i < commonCount; i++) {
      // Pick from first 5 templates (common patterns)
      String template = LOG_TEMPLATES[random.nextInt(5)];
      result.add(fillTemplate(template, random));
    }

    for (int i = commonCount; i < count; i++) {
      // Pick from all templates (rare patterns)
      String template = LOG_TEMPLATES[random.nextInt(LOG_TEMPLATES.length)];
      result.add(fillTemplate(template, random));
    }

    return result;
  }

  private String fillTemplate(String template, Random random) {
    String result = template;

    // Replace all %s with usernames
    while (result.contains("%s")) {
      result = result.replaceFirst("%s", generateValue(random));
    }

    // Replace all %d with numbers
    while (result.contains("%d")) {
      result = result.replaceFirst("%d", String.valueOf(random.nextInt(1000)));
    }

    return result;
  }

  private String generateValue(Random random) {
    String[] values = {
      "user" + random.nextInt(100),
      "192.168.1." + random.nextInt(255),
      "/api/v1/endpoint" + random.nextInt(10),
      "req-" + random.nextInt(10000),
      "txn-" + random.nextInt(10000),
      "order-" + random.nextInt(10000),
      "job-" + random.nextInt(100),
      "GET",
      "POST",
      "Connection timeout",
      "Invalid input"
    };
    return values[random.nextInt(values.length)];
  }
}

