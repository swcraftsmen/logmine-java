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
import java.util.stream.Collectors;
import org.swengdev.logmine.strategy.TokenizerStrategy;
import org.swengdev.logmine.strategy.VariableDetector;

/**
 * Main LogMine processor that implements the pattern recognition algorithm.
 *
 * <p>The algorithm works in three main steps: 1. Clustering: Group similar log messages together 2.
 * Pattern Extraction: Extract patterns from each cluster 3. Pattern Ranking: Rank patterns by
 * support count
 *
 * <p>Based on the paper: "LogMine: Fast Pattern Recognition for Log Analytics" by Hamooni et al.,
 * CIKM 2016
 */
public class LogMineProcessor {
  private final LogMineConfig config;
  private List<LogCluster> clusters;
  private List<LogPattern> patterns;

  /**
   * Creates a LogMine processor with custom configuration.
   *
   * @param config Configuration for the processor
   */
  public LogMineProcessor(LogMineConfig config) {
    this.config = config;
    this.clusters = new ArrayList<>();
    this.patterns = new ArrayList<>();
  }

  /** Creates a LogMine processor with default configuration. */
  public LogMineProcessor() {
    this(LogMineConfig.defaults());
  }

  /**
   * Creates a LogMine processor with specified threshold and cluster size. Uses default tokenizer
   * and variable detector.
   *
   * @param similarityThreshold Minimum similarity (0.0 to 1.0) for messages to be in same cluster
   * @param minClusterSize Minimum number of messages to form a valid cluster
   */
  public LogMineProcessor(double similarityThreshold, int minClusterSize) {
    this(
        LogMineConfig.builder()
            .withSimilarityThreshold(similarityThreshold)
            .withMinClusterSize(minClusterSize)
            .build());
  }

  /**
   * Processes a list of log messages and extracts patterns.
   *
   * @param logMessages List of log messages to process
   * @return List of extracted patterns, sorted by support count
   */
  public List<LogPattern> process(List<String> logMessages) {
    TokenizerStrategy tokenizer = config.tokenizerStrategy();
    VariableDetector variableDetector = config.variableDetector();

    // Create preprocessor if any normalization is enabled
    LogPreprocessor preprocessor = createPreprocessor();

    // Convert strings to LogMessage objects using configured tokenizer
    List<LogMessage> messages =
        logMessages.stream()
            .map(
                rawMessage -> {
                  // Preprocess to normalize different log formats
                  String processed =
                      preprocessor != null ? preprocessor.preprocess(rawMessage) : rawMessage;
                  return new LogMessage(
                      rawMessage, tokenizer.tokenize(processed), variableDetector);
                })
            .collect(Collectors.toList());

    // Step 1: Cluster similar messages
    clusterMessages(messages);

    // Step 2: Extract patterns from clusters
    extractPatterns();

    // Step 3: Sort patterns by support count (descending)
    patterns.sort((p1, p2) -> Integer.compare(p2.getSupportCount(), p1.getSupportCount()));

    return patterns;
  }

  /**
   * Clusters log messages using a greedy online clustering approach. Each message is assigned to
   * the first cluster where it's similar enough, or creates a new cluster if no suitable cluster
   * exists.
   */
  private void clusterMessages(List<LogMessage> messages) {
    clusters = new ArrayList<>();
    double threshold = config.similarityThreshold();
    VariableDetector variableDetector = config.variableDetector();
    int maxClusters = config.maxClusters();

    for (LogMessage message : messages) {
      boolean clustered = false;

      // Try to add message to an existing cluster
      for (LogCluster cluster : clusters) {
        if (cluster.addMessage(message, threshold)) {
          clustered = true;
          break;
        }
      }

      // If not clustered, create a new cluster (with limit)
      if (!clustered) {
        if (clusters.size() < maxClusters) {
          clusters.add(new LogCluster(message, variableDetector));
        } else {
          // At max capacity, merge with closest cluster (relaxed threshold)
          mergeWithClosestCluster(message, threshold * 0.8);
        }
      }
    }

    // Filter out clusters that are too small
    int minSize = config.minClusterSize();
    clusters =
        clusters.stream().filter(cluster -> cluster.size() >= minSize).collect(Collectors.toList());
  }

  /** Extracts patterns from each cluster. */
  private void extractPatterns() {
    patterns = clusters.stream().map(LogCluster::generatePattern).collect(Collectors.toList());
  }

  /**
   * Matches a new log message against extracted patterns. Returns the best matching pattern or null
   * if no pattern matches.
   *
   * @param logMessage The log message to match
   * @return The matching LogPattern, or null if no pattern matches
   */
  public LogPattern matchPattern(String logMessage) {
    TokenizerStrategy tokenizer = config.tokenizerStrategy();
    VariableDetector variableDetector = config.variableDetector();

    // Preprocess to normalize different log formats
    LogPreprocessor preprocessor = createPreprocessor();
    String processed = preprocessor != null ? preprocessor.preprocess(logMessage) : logMessage;

    LogMessage message =
        new LogMessage(logMessage, tokenizer.tokenize(processed), variableDetector);

    for (LogPattern pattern : patterns) {
      if (pattern.matches(message)) {
        return pattern;
      }
    }

    return null;
  }

  /**
   * Returns the configuration used by this processor.
   *
   * @return The LogMineConfig instance
   */
  public LogMineConfig getConfig() {
    return config;
  }

  /**
   * Processes a single log message incrementally (streaming mode). Updates clusters and patterns
   * without storing the raw log.
   *
   * @param logMessage Raw log message to process
   */
  public void processLogIncremental(String logMessage) {
    TokenizerStrategy tokenizer = config.tokenizerStrategy();
    VariableDetector variableDetector = config.variableDetector();
    double threshold = config.similarityThreshold();

    // Preprocess to normalize different log formats
    LogPreprocessor preprocessor = createPreprocessor();
    String processed = preprocessor != null ? preprocessor.preprocess(logMessage) : logMessage;

    // Create LogMessage
    LogMessage message =
        new LogMessage(logMessage, tokenizer.tokenize(processed), variableDetector);

    boolean clustered = false;

    // Try to add to existing cluster
    for (LogCluster cluster : clusters) {
      if (cluster.addMessage(message, threshold)) {
        clustered = true;
        break;
      }
    }

    // Create new cluster if needed (with max cluster limit)
    if (!clustered) {
      if (clusters.size() < config.maxClusters()) {
        clusters.add(new LogCluster(message, variableDetector));
      } else {
        // At max capacity, merge with closest cluster
        mergeWithClosestCluster(message, threshold * 0.8);
      }
    }

    // Filter small clusters periodically (every 100 messages)
    if (getStats().getTotalMessages() % 100 == 0) {
      int minSize = config.minClusterSize();
      clusters.removeIf(cluster -> cluster.size() < minSize);
    }

    // Update patterns: immediately on first call, then every 50 messages for performance
    int totalMessages = getStats().getTotalMessages();
    if (patterns.isEmpty() || totalMessages % 50 == 0) {
      extractPatterns();
      patterns.sort((p1, p2) -> Integer.compare(p2.getSupportCount(), p1.getSupportCount()));
    }
  }

  /** Clears all clusters and patterns. Useful for resetting the processor state. */
  public void clear() {
    clusters.clear();
    patterns.clear();
  }

  /**
   * Returns statistics about the clustering and pattern extraction.
   *
   * @return Processing statistics
   */
  public ProcessingStats getStats() {
    int totalMessages = clusters.stream().mapToInt(LogCluster::size).sum();

    double avgClusterSize = clusters.isEmpty() ? 0 : (double) totalMessages / clusters.size();

    double avgSpecificity =
        patterns.stream().mapToDouble(LogPattern::getSpecificity).average().orElse(0.0);

    return new ProcessingStats(
        totalMessages, clusters.size(), patterns.size(), avgClusterSize, avgSpecificity);
  }

  /**
   * Gets all clusters created during processing.
   *
   * <p><b>Deprecated:</b> This method exposes internal clustering implementation details and will
   * be removed in a future version. Users should work with {@link LogPattern} objects instead,
   * which provide all necessary information about extracted patterns without exposing internal
   * clustering structures.
   *
   * <p>If you need cluster statistics, use {@link #getStats()} which provides aggregate information
   * about clustering results.
   *
   * @return A defensive copy of the cluster list
   * @deprecated since 1.1, will be removed in 2.0. Use {@link #getPatterns()} and {@link
   *     #getStats()} instead.
   */
  @Deprecated(since = "1.1", forRemoval = true)
  public List<LogCluster> getClusters() {
    return new ArrayList<>(clusters);
  }

  /**
   * Gets all patterns extracted during processing.
   *
   * @return A defensive copy of the pattern list
   */
  public List<LogPattern> getPatterns() {
    return new ArrayList<>(patterns);
  }

  /**
   * Statistics about the processing results.
   *
   * <p>Provides metrics about clustering quality and pattern extraction.
   */
  public static class ProcessingStats {
    private final int totalMessages;
    private final int numClusters;
    private final int numPatterns;
    private final double avgClusterSize;
    private final double avgSpecificity;

    /**
     * Creates processing statistics.
     *
     * @param totalMessages Total number of messages processed
     * @param numClusters Number of clusters created
     * @param numPatterns Number of patterns extracted
     * @param avgClusterSize Average size of clusters
     * @param avgSpecificity Average specificity of patterns
     */
    public ProcessingStats(
        int totalMessages,
        int numClusters,
        int numPatterns,
        double avgClusterSize,
        double avgSpecificity) {
      this.totalMessages = totalMessages;
      this.numClusters = numClusters;
      this.numPatterns = numPatterns;
      this.avgClusterSize = avgClusterSize;
      this.avgSpecificity = avgSpecificity;
    }

    /**
     * Gets the total number of messages processed.
     *
     * @return Total message count
     */
    public int getTotalMessages() {
      return totalMessages;
    }

    /**
     * Gets the number of clusters.
     *
     * @return Cluster count
     */
    public int getNumClusters() {
      return numClusters;
    }

    /**
     * Gets the number of patterns extracted.
     *
     * @return Pattern count
     */
    public int getNumPatterns() {
      return numPatterns;
    }

    /**
     * Gets the average cluster size.
     *
     * @return Average number of messages per cluster
     */
    public double getAvgClusterSize() {
      return avgClusterSize;
    }

    /**
     * Gets the average pattern specificity.
     *
     * @return Average specificity score (0.0-1.0)
     */
    public double getAvgSpecificity() {
      return avgSpecificity;
    }

    @Override
    public String toString() {
      return String.format(
          "Processing Statistics:\n"
              + "  Total Messages: %d\n"
              + "  Number of Clusters: %d\n"
              + "  Number of Patterns: %d\n"
              + "  Average Cluster Size: %.2f\n"
              + "  Average Pattern Specificity: %.2f",
          totalMessages, numClusters, numPatterns, avgClusterSize, avgSpecificity);
    }
  }

  /**
   * Creates a preprocessor if any normalization is enabled in config. Returns null if no
   * preprocessing is needed.
   */
  private LogPreprocessor createPreprocessor() {
    // Check if any normalization is enabled
    if (config.normalizeTimestamps()
        || config.normalizeIPs()
        || config.normalizeNumbers()
        || config.normalizePaths()
        || config.normalizeUrls()
        || !config.caseSensitive()) {
      return new LogPreprocessor(config);
    }
    return null;
  }

  /**
   * Merges a message with the closest cluster when max cluster limit is reached. Uses a relaxed
   * threshold to ensure the message gets clustered.
   */
  private void mergeWithClosestCluster(LogMessage message, double relaxedThreshold) {
    LogCluster closestCluster = null;
    double highestSimilarity = -1.0;

    for (LogCluster cluster : clusters) {
      double similarity = cluster.calculateSimilarity(message);
      if (similarity > highestSimilarity) {
        highestSimilarity = similarity;
        closestCluster = cluster;
      }
    }

    if (closestCluster != null) {
      // Force add to closest cluster regardless of threshold
      closestCluster.addMessage(message, 0.0);
    }
  }

  /**
   * Extracts hierarchical patterns at multiple levels of specificity. This is useful for SaaS
   * platforms where customers need different views: - Level 0 (coarse): High-level overview for
   * dashboards - Level 1 (medium): Balanced detail for alerts - Level 2 (fine): Detailed patterns
   * for debugging
   *
   * @return List of hierarchical patterns (root nodes)
   */
  public List<HierarchicalPattern> extractHierarchicalPatterns() {
    if (!config.enableHierarchicalPatterns()) {
      return List.of();
    }

    List<Double> thresholds = config.hierarchyThresholds();
    if (thresholds.isEmpty()) {
      // Default thresholds: coarse (0.5), medium (0.7), fine (0.9)
      thresholds = List.of(0.5, 0.7, 0.9);
    }

    // Extract patterns at each threshold level
    List<List<LogPattern>> patternLevels = new ArrayList<>();
    LogMineConfig originalConfig = config;

    for (double threshold : thresholds) {
      // Create temporary config with this threshold
      LogMineConfig levelConfig =
          LogMineConfig.builder()
              .withSimilarityThreshold(threshold)
              .withMinClusterSize(originalConfig.minClusterSize())
              .maxClusters(originalConfig.maxClusters())
              .withTokenizerStrategy(originalConfig.tokenizerStrategy())
              .withVariableDetector(originalConfig.variableDetector())
              .build();

      // Process with this threshold
      LogMineProcessor levelProcessor = new LogMineProcessor(levelConfig);

      // Re-cluster with current messages
      List<String> currentMessages =
          clusters.stream()
              .flatMap(cluster -> cluster.getMessages().stream())
              .map(LogMessage::getRawMessage)
              .collect(Collectors.toList());

      List<LogPattern> levelPatterns = levelProcessor.process(currentMessages);
      patternLevels.add(levelPatterns);
    }

    // Build hierarchy
    return buildHierarchy(patternLevels, thresholds);
  }

  /** Builds a hierarchical structure from patterns extracted at different thresholds. */
  private List<HierarchicalPattern> buildHierarchy(
      List<List<LogPattern>> patternLevels, List<Double> thresholds) {

    if (patternLevels.isEmpty()) {
      return List.of();
    }

    // Start with the coarsest level (level 0)
    List<HierarchicalPattern> roots = new ArrayList<>();
    List<LogPattern> coarsePatterns = patternLevels.get(0);

    for (LogPattern pattern : coarsePatterns) {
      roots.add(new HierarchicalPattern(0, thresholds.get(0), pattern, null));
    }

    // Add finer levels as children
    for (int level = 1; level < patternLevels.size(); level++) {
      List<LogPattern> currentLevelPatterns = patternLevels.get(level);
      List<HierarchicalPattern> previousLevel =
          (level == 1) ? roots : getAllNodesAtLevel(roots, level - 1);

      for (LogPattern finePattern : currentLevelPatterns) {
        // Find best parent from previous level
        HierarchicalPattern bestParent = findBestParent(finePattern, previousLevel);

        if (bestParent != null) {
          HierarchicalPattern child =
              new HierarchicalPattern(level, thresholds.get(level), finePattern, bestParent);
          bestParent.addChild(child);
        }
      }
    }

    return roots;
  }

  /** Gets all hierarchical pattern nodes at a specific level. */
  private List<HierarchicalPattern> getAllNodesAtLevel(
      List<HierarchicalPattern> roots, int targetLevel) {

    List<HierarchicalPattern> result = new ArrayList<>();
    for (HierarchicalPattern root : roots) {
      collectNodesAtLevel(root, targetLevel, result);
    }
    return result;
  }

  private void collectNodesAtLevel(
      HierarchicalPattern node, int targetLevel, List<HierarchicalPattern> result) {

    if (node.getLevel() == targetLevel) {
      result.add(node);
    }
    for (HierarchicalPattern child : node.getChildren()) {
      collectNodesAtLevel(child, targetLevel, result);
    }
  }

  /**
   * Finds the best parent pattern from a coarser level for a fine pattern. Uses pattern similarity
   * to determine the best match.
   */
  private HierarchicalPattern findBestParent(
      LogPattern finePattern, List<HierarchicalPattern> candidateParents) {

    HierarchicalPattern bestParent = null;
    int maxCommonTokens = 0;

    for (HierarchicalPattern candidate : candidateParents) {
      int commonTokens =
          countCommonTokens(finePattern.getTokens(), candidate.getPattern().getTokens());

      if (commonTokens > maxCommonTokens) {
        maxCommonTokens = commonTokens;
        bestParent = candidate;
      }
    }

    return bestParent;
  }

  /** Counts common tokens between two patterns (ignoring wildcards). */
  private int countCommonTokens(List<String> tokens1, List<String> tokens2) {
    int count = 0;
    int minLen = Math.min(tokens1.size(), tokens2.size());

    for (int i = 0; i < minLen; i++) {
      String t1 = tokens1.get(i);
      String t2 = tokens2.get(i);

      if (!t1.equals("*") && !t2.equals("*") && t1.equals(t2)) {
        count++;
      }
    }

    return count;
  }
}
