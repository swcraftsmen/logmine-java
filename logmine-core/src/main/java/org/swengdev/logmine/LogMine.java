/*
 * Copyright 2024 Zachary Huang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.swengdev.logmine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Main facade class for using LogMine as a library in backend services.
 *
 * <p>LogMine provides unsupervised log pattern extraction using clustering and pattern recognition
 * algorithms. It automatically discovers patterns in log data without requiring predefined
 * templates or regular expressions.
 *
 * <h2>Processing Modes</h2>
 *
 * <p>LogMine supports two processing modes:
 *
 * <ul>
 *   <li><b>STREAMING:</b> Process logs immediately without storage (production use)
 *       <ul>
 *         <li>Memory: O(k) where k = number of clusters (constant)
 *         <li>Throughput: ~8,000 logs/second single thread
 *         <li>Use case: Real-time monitoring, unlimited log volumes
 *       </ul>
 *   <li><b>BATCH:</b> Store logs for batch processing (research/analysis)
 *       <ul>
 *         <li>Memory: O(n) where n = number of logs (with configurable limit)
 *         <li>Throughput: 158,000 logs/second collection, ~2-8 ops/s processing
 *         <li>Use case: Historical analysis, experimentation
 *       </ul>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 *
 * <p>This class is thread-safe and can handle concurrent operations using {@link ReadWriteLock}.
 * Multiple threads can safely call {@link #addLog(String)} and {@link #getCurrentPatterns()}
 * concurrently.
 *
 * <h2>Usage Examples</h2>
 *
 * <h3>Streaming Mode (Production)</h3>
 *
 * <pre>{@code
 * // Create LogMine in streaming mode
 * LogMine logMine = new LogMine(ProcessingMode.STREAMING);
 *
 * // Process logs as they arrive (no storage)
 * logMine.addLog("GET /api/users/123 HTTP/1.1 200");
 * logMine.addLog("GET /api/users/456 HTTP/1.1 200");
 *
 * // Get current patterns (always available)
 * List<LogPattern> patterns = logMine.getCurrentPatterns();
 * patterns.forEach(p -> System.out.println(p.getSignature()));
 *
 * // Check for anomalies in real-time
 * if (logMine.isAnomaly("CRITICAL: Unknown error")) {
 *     alertService.sendAlert();
 * }
 * }</pre>
 *
 * <h3>Batch Mode (Research)</h3>
 *
 * <pre>{@code
 * // Create LogMine in batch mode
 * LogMine logMine = new LogMine(ProcessingMode.BATCH);
 *
 * // Collect logs
 * logMine.addLog("INFO: User login successful");
 * logMine.addLog("INFO: User logout successful");
 *
 * // Process all at once
 * List<LogPattern> patterns = logMine.extractPatterns();
 * }</pre>
 *
 * <h3>Custom Configuration</h3>
 *
 * <pre>{@code
 * LogMineConfig config = LogMineConfig.builder()
 *     .withSimilarityThreshold(0.6)
 *     .withMinClusterSize(3)
 *     .build();
 *
 * LogMineProcessor processor = new LogMineProcessor(config);
 * LogMine logMine = new LogMine(ProcessingMode.STREAMING, processor, 100000);
 * }</pre>
 *
 * @author Zachary Huang
 * @version 1.0
 * @since 1.0
 * @see LogPattern
 * @see LogMineConfig
 * @see ProcessingMode
 */
public class LogMine {

  private final ProcessingMode mode;
  private final LogMineProcessor processor;
  private final List<String> collectedLogs; // Only used in BATCH mode
  private final int maxLogsInMemory;
  private final ReadWriteLock lock;
  private List<LogPattern> currentPatterns;
  private boolean patternsStale;
  private int lastPatternUpdateCount; // Track when patterns were last updated

  /**
   * Creates a LogMine instance with default configuration in BATCH mode.
   *
   * <p>Default settings:
   *
   * <ul>
   *   <li>Processing mode: {@link ProcessingMode#BATCH}
   *   <li>Similarity threshold: 0.5 (balanced)
   *   <li>Minimum cluster size: 2 logs
   *   <li>Maximum logs in memory: 100,000
   * </ul>
   *
   * <p>This constructor is suitable for most log types and general-purpose use.
   *
   * @see #LogMine(ProcessingMode)
   * @see #LogMine(double, int)
   */
  public LogMine() {
    this(ProcessingMode.BATCH, new LogMineProcessor(0.5, 2), 100000);
  }

  /**
   * Creates a LogMine instance with specified processing mode.
   *
   * <p>Use {@link ProcessingMode#STREAMING} for production deployments with unlimited log volumes.
   * Use {@link ProcessingMode#BATCH} for research, analysis, and experimentation.
   *
   * @param mode the processing mode to use; must not be {@code null}
   * @throws NullPointerException if {@code mode} is {@code null}
   * @see ProcessingMode
   */
  public LogMine(ProcessingMode mode) {
    this(mode, new LogMineProcessor(0.5, 2), 100000);
  }

  /**
   * Creates a LogMine instance with custom similarity threshold.
   *
   * @param similarityThreshold Controls clustering strictness (0.3-0.8) - 0.7-0.8: Strict, many
   *     specific patterns - 0.5: Balanced (recommended) - 0.3-0.4: Lenient, fewer general patterns
   */
  public LogMine(double similarityThreshold) {
    this(ProcessingMode.BATCH, new LogMineProcessor(similarityThreshold, 2), 100000);
  }

  /**
   * Creates a LogMine instance with custom configuration.
   *
   * @param similarityThreshold Similarity threshold (0.0-1.0)
   * @param minClusterSize Minimum logs to form a pattern (filters noise)
   */
  public LogMine(double similarityThreshold, int minClusterSize) {
    this(ProcessingMode.BATCH, new LogMineProcessor(similarityThreshold, minClusterSize), 100000);
  }

  /**
   * Creates a LogMine instance with custom processor.
   *
   * @param processor Pre-configured LogMineProcessor
   */
  public LogMine(LogMineProcessor processor) {
    this(ProcessingMode.BATCH, processor, 100000);
  }

  /**
   * Creates a LogMine instance with full configuration.
   *
   * @param mode STREAMING or BATCH processing mode
   * @param processor Pre-configured LogMineProcessor
   * @param maxLogsInMemory Maximum logs to keep in memory (BATCH mode only)
   */
  public LogMine(ProcessingMode mode, LogMineProcessor processor, int maxLogsInMemory) {
    this.mode = mode;
    this.processor = processor;
    this.maxLogsInMemory = maxLogsInMemory;
    this.collectedLogs = mode == ProcessingMode.BATCH ? new ArrayList<>() : null;
    this.lock = new ReentrantReadWriteLock();
    this.currentPatterns = new ArrayList<>();
    this.patternsStale = true;
    this.lastPatternUpdateCount = 0;
  }

  /**
   * Adds a log message for processing.
   *
   * <p>The behavior depends on the processing mode:
   *
   * <ul>
   *   <li><b>STREAMING:</b> Processes the log immediately and updates patterns without storing the
   *       raw log message. Memory usage remains constant.
   *   <li><b>BATCH:</b> Stores the log message in memory for later batch processing. Call {@link
   *       #extractPatterns()} to process all collected logs.
   * </ul>
   *
   * <p>This method is thread-safe and can be called concurrently from multiple threads. It uses a
   * write lock internally to ensure data consistency.
   *
   * <p><b>Performance characteristics:</b>
   *
   * <ul>
   *   <li>STREAMING: 0.01-0.1 ms per log (immediate processing)
   *   <li>BATCH: &lt;0.001 ms per log (just appends to list)
   * </ul>
   *
   * <p>Logs that are {@code null}, empty, or longer than 10,000 characters are handled gracefully
   * (truncated or ignored).
   *
   * @param logMessage the log message to process; may be {@code null} or empty (in which case it's
   *     ignored)
   * @see #addLogs(List)
   * @see #extractPatterns()
   * @see #getCurrentPatterns()
   */
  public void addLog(String logMessage) {
    if (logMessage == null || logMessage.trim().isEmpty()) {
      return;
    }

    // Validate log size
    if (logMessage.length() > 10000) {
      logMessage = logMessage.substring(0, 10000);
    }

    lock.writeLock().lock();
    try {
      if (mode == ProcessingMode.STREAMING) {
        // Process immediately without storing
        processLogStreaming(logMessage);
      } else {
        // Store for batch processing
        collectedLogs.add(logMessage);

        // Apply memory limit in BATCH mode
        if (collectedLogs.size() > maxLogsInMemory) {
          int removeCount = collectedLogs.size() - maxLogsInMemory;
          collectedLogs.subList(0, removeCount).clear();
        }

        patternsStale = true;
      }
    } catch (Exception e) {
      // Don't crash on errors
      System.err.println("Error adding log: " + e.getMessage());
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Process log immediately in streaming mode. Updates clusters incrementally without storing raw
   * log.
   */
  private void processLogStreaming(String logMessage) {
    // Process log and update patterns incrementally
    processor.processLogIncremental(logMessage);

    // Only copy patterns if they were actually regenerated (every 50 messages)
    // This avoids expensive list copying on every addLog() call
    int currentCount = processor.getStats().getTotalMessages();
    if (currentPatterns.isEmpty() || currentCount != lastPatternUpdateCount) {
      // Check if patterns were updated (happens every 50 logs or when empty)
      if (currentPatterns.isEmpty() || currentCount % 50 == 0 || currentCount == 1) {
        currentPatterns = processor.getPatterns();
        lastPatternUpdateCount = currentCount;
      }
    }
    patternsStale = false;
  }

  /**
   * Process multiple logs in streaming mode efficiently. Updates patterns once after all logs are
   * processed.
   */
  private void processLogsStreamingBulk(List<String> logMessages) {
    // Process all logs first (fast - no pattern copying)
    for (String log : logMessages) {
      if (log != null && !log.trim().isEmpty()) {
        String validatedLog = log.length() > 10000 ? log.substring(0, 10000) : log;
        processor.processLogIncremental(validatedLog);
      }
    }

    // Update patterns once at the end (single copy operation)
    currentPatterns = processor.getPatterns();
    lastPatternUpdateCount = processor.getStats().getTotalMessages();
    patternsStale = false;
  }

  /**
   * Adds multiple log messages at once. More efficient than calling addLog() multiple times.
   *
   * <p>In STREAMING mode, this method is significantly faster than calling addLog() in a loop
   * because it processes all logs first and then updates patterns once at the end, avoiding
   * repeated pattern list copying.
   *
   * @param logMessages List of log messages to add
   */
  public void addLogs(List<String> logMessages) {
    if (logMessages == null || logMessages.isEmpty()) {
      return;
    }

    lock.writeLock().lock();
    try {
      if (mode == ProcessingMode.STREAMING) {
        // Optimized bulk processing: process all logs, then update patterns once
        // This is 10-20x faster than calling addLog() in a loop
        processLogsStreamingBulk(logMessages);
      } else {
        // Store for batch processing
        collectedLogs.addAll(logMessages);

        // Apply memory limit
        if (collectedLogs.size() > maxLogsInMemory) {
          int removeCount = collectedLogs.size() - maxLogsInMemory;
          collectedLogs.subList(0, removeCount).clear();
        }

        patternsStale = true;
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Extracts patterns from collected logs.
   *
   * <p>The behavior depends on the processing mode:
   *
   * <ul>
   *   <li><b>STREAMING:</b> Returns the current patterns immediately. Patterns are always
   *       up-to-date as they're updated with each {@link #addLog(String)} call. This operation is
   *       very fast (O(1)).
   *   <li><b>BATCH:</b> Processes all collected logs and extracts patterns. This is an expensive
   *       operation (O(n*k) where n=logs, k=clusters) and should be called periodically, not on
   *       every log.
   * </ul>
   *
   * <p>Patterns are sorted by support count (frequency) in descending order. The most common
   * patterns appear first in the list.
   *
   * <p><b>Performance characteristics:</b>
   *
   * <ul>
   *   <li>STREAMING: Instant (patterns are cached)
   *   <li>BATCH: O(n*k) where n = number of logs, k = number of clusters
   * </ul>
   *
   * <p><b>Example:</b>
   *
   * <pre>{@code
   * List<LogPattern> patterns = logMine.extractPatterns();
   * for (LogPattern pattern : patterns) {
   *     System.out.printf("Pattern: %s (support: %d, specificity: %.2f)%n",
   *         pattern.getSignature(),
   *         pattern.getSupportCount(),
   *         pattern.getSpecificity());
   * }
   * }</pre>
   *
   * @return a list of extracted patterns sorted by support count (most frequent first); never
   *     {@code null}, but may be empty if no patterns have been discovered
   * @see #getCurrentPatterns()
   * @see #addLog(String)
   * @see LogPattern
   */
  public List<LogPattern> extractPatterns() {
    lock.writeLock().lock();
    try {
      if (mode == ProcessingMode.STREAMING) {
        // Patterns are always up-to-date in streaming mode
        return new ArrayList<>(processor.getPatterns());
      } else {
        // Batch mode - process all logs
        if (patternsStale && collectedLogs != null && !collectedLogs.isEmpty()) {
          currentPatterns = processor.process(new ArrayList<>(collectedLogs));
          patternsStale = false;
        }
        return new ArrayList<>(currentPatterns);
      }
    } catch (Exception e) {
      System.err.println("Error extracting patterns: " + e.getMessage());
      return new ArrayList<>();
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Gets the current patterns without re-processing. Fast operation, returns cached patterns.
   *
   * <p>In STREAMING mode, this method uses lazy updates: if patterns haven't been refreshed
   * recently (due to the 50-message batching optimization), it will update them before returning.
   * This ensures patterns are always fresh without the overhead of updating on every log.
   *
   * <p>In BATCH mode, returns cached patterns without processing. Call {@link #extractPatterns()}
   * explicitly to process logs.
   *
   * @return List of current patterns (may be stale in BATCH mode if new logs added)
   */
  public List<LogPattern> getCurrentPatterns() {
    // Try read lock first for the common case (patterns are fresh)
    lock.readLock().lock();
    try {
      // Only apply lazy updates in STREAMING mode
      if (mode == ProcessingMode.STREAMING && shouldUpdatePatternsStreaming()) {
        // Need to update - upgrade to write lock
        lock.readLock().unlock();
        lock.writeLock().lock();
        try {
          // Double-check after acquiring write lock
          if (shouldUpdatePatternsStreaming()) {
            updatePatternsStreaming();
          }
          // Downgrade to read lock
          lock.readLock().lock();
        } finally {
          lock.writeLock().unlock();
        }
      }
      return new ArrayList<>(currentPatterns);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Checks if patterns should be updated in STREAMING mode. Patterns need updating if there are new
   * messages since last update (due to the 50-message batching optimization).
   *
   * @return true if patterns need updating in STREAMING mode
   */
  private boolean shouldUpdatePatternsStreaming() {
    // In streaming mode, check if there are new messages since last update
    int currentCount = processor.getStats().getTotalMessages();
    return currentCount != lastPatternUpdateCount && currentCount > 0;
  }

  /** Updates patterns in STREAMING mode. Should be called with write lock held. */
  private void updatePatternsStreaming() {
    // In streaming mode, just refresh the pattern list
    currentPatterns = processor.getPatterns();
    lastPatternUpdateCount = processor.getStats().getTotalMessages();
    patternsStale = false;
  }

  /**
   * Checks if a log message is anomalous (doesn't match any known pattern). Useful for real-time
   * anomaly detection.
   *
   * <p>Note: Patterns must be extracted at least once before this can detect anomalies.
   *
   * @param logMessage The log message to check
   * @return true if the log is anomalous (no pattern match)
   */
  public boolean isAnomaly(String logMessage) {
    lock.readLock().lock();
    try {
      if (currentPatterns.isEmpty()) {
        return false; // Can't detect anomalies without patterns
      }
      return processor.matchPattern(logMessage) == null;
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Finds the pattern that matches a given log message.
   *
   * @param logMessage The log message to match
   * @return The matching pattern, or null if no match
   */
  public LogPattern matchPattern(String logMessage) {
    lock.readLock().lock();
    try {
      return processor.matchPattern(logMessage);
    } finally {
      lock.readLock().unlock();
    }
  }

  /** Clears all collected logs and patterns. Useful for starting fresh or managing memory. */
  public void clear() {
    lock.writeLock().lock();
    try {
      if (collectedLogs != null) {
        collectedLogs.clear();
      }
      processor.clear();
      currentPatterns.clear();
      patternsStale = true;
      lastPatternUpdateCount = 0;
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Gets the current processing mode.
   *
   * @return STREAMING or BATCH
   */
  public ProcessingMode getMode() {
    return mode;
  }

  /**
   * Checks if in streaming mode.
   *
   * @return true if streaming, false if batch
   */
  public boolean isStreaming() {
    return mode == ProcessingMode.STREAMING;
  }

  /**
   * Checks if in batch mode.
   *
   * @return true if batch, false if streaming
   */
  public boolean isBatch() {
    return mode == ProcessingMode.BATCH;
  }

  /**
   * Gets statistics about the collected logs and patterns.
   *
   * @return Statistics object with metrics
   */
  public Stats getStats() {
    lock.readLock().lock();
    try {
      int logCount = getLogCount();
      return new Stats(mode, logCount, currentPatterns.size(), patternsStale, processor.getStats());
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Gets the number of collected logs.
   *
   * <p>In STREAMING mode: Returns number of logs processed (from clusters) In BATCH mode: Returns
   * number of logs currently stored in memory
   *
   * @return Number of logs
   */
  public int getLogCount() {
    lock.readLock().lock();
    try {
      if (mode == ProcessingMode.STREAMING) {
        // In streaming mode, use processing stats to get total message count
        return processor.getStats().getTotalMessages();
      } else {
        // In batch mode, count stored logs
        return collectedLogs != null ? collectedLogs.size() : 0;
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Gets the number of extracted patterns.
   *
   * @return Number of patterns
   */
  public int getPatternCount() {
    lock.readLock().lock();
    try {
      return currentPatterns.size();
    } finally {
      lock.readLock().unlock();
    }
  }

  /** Statistics about the LogMine instance. */
  public static class Stats {
    private final ProcessingMode mode;
    private final int totalLogs;
    private final int patternCount;
    private final boolean patternsNeedUpdate;
    private final LogMineProcessor.ProcessingStats processingStats;

    /**
     * Creates statistics snapshot.
     *
     * @param mode The processing mode in use
     * @param totalLogs Total number of logs processed or collected
     * @param patternCount Number of patterns currently extracted
     * @param patternsNeedUpdate Whether patterns need to be updated
     * @param processingStats Detailed processing statistics
     */
    public Stats(
        ProcessingMode mode,
        int totalLogs,
        int patternCount,
        boolean patternsNeedUpdate,
        LogMineProcessor.ProcessingStats processingStats) {
      this.mode = mode;
      this.totalLogs = totalLogs;
      this.patternCount = patternCount;
      this.patternsNeedUpdate = patternsNeedUpdate;
      this.processingStats = processingStats;
    }

    /**
     * Gets the processing mode.
     *
     * @return The ProcessingMode
     */
    public ProcessingMode getMode() {
      return mode;
    }

    /**
     * Gets the total number of logs processed or collected.
     *
     * @return Total log count
     */
    public int getTotalLogs() {
      return totalLogs;
    }

    /**
     * Gets the number of patterns extracted.
     *
     * @return Pattern count
     */
    public int getPatternCount() {
      return patternCount;
    }

    /**
     * Checks if patterns need to be updated.
     *
     * @return true if patterns are stale and need updating
     */
    public boolean isPatternsNeedUpdate() {
      return patternsNeedUpdate;
    }

    /**
     * Gets detailed processing statistics.
     *
     * @return ProcessingStats instance with detailed metrics
     */
    public LogMineProcessor.ProcessingStats getProcessingStats() {
      return processingStats;
    }

    @Override
    public String toString() {
      return String.format(
          "LogMine Statistics:\n"
              + "  Processing Mode: %s\n"
              + "  Total Logs: %d\n"
              + "  Patterns Extracted: %d\n"
              + "  Patterns Need Update: %s\n"
              + "  %s",
          mode,
          totalLogs,
          patternCount,
          patternsNeedUpdate,
          processingStats != null
              ? processingStats.toString().replace("Processing Statistics:", "Processing Details:")
              : "");
    }
  }
}
