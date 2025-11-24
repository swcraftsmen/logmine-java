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
import org.swengdev.logmine.strategy.DelimiterPreservingTokenizer;
import org.swengdev.logmine.strategy.StandardVariableDetector;
import org.swengdev.logmine.strategy.TokenizerStrategy;
import org.swengdev.logmine.strategy.VariableDetector;

/**
 * Configuration record for LogMine processor. Immutable configuration with builder pattern for
 * convenience. Allows customization for handling logs from multiple heterogeneous sources.
 *
 * @param similarityThreshold Clustering similarity threshold (0.0-1.0)
 * @param minClusterSize Minimum number of messages required to form a cluster
 * @param maxClusters Maximum number of clusters to maintain
 * @param tokenizerStrategy Strategy for tokenizing log messages
 * @param variableDetector Strategy for detecting variable parts in tokens
 * @param tokenizationStrategy Tokenization strategy for different log formats
 * @param customDelimiters Custom delimiters for tokenization (when using CUSTOM strategy)
 * @param normalizeTimestamps Whether to normalize timestamps in preprocessing
 * @param normalizeIPs Whether to normalize IP addresses in preprocessing
 * @param normalizeNumbers Whether to normalize numbers in preprocessing
 * @param normalizePaths Whether to normalize file paths in preprocessing
 * @param normalizeUrls Whether to normalize URLs in preprocessing
 * @param caseSensitive Whether pattern matching is case-sensitive
 * @param minPatternLength Minimum length of extracted patterns
 * @param maxPatternLength Maximum length of extracted patterns
 * @param minPatternSpecificity Minimum specificity threshold for patterns (0.0-1.0)
 * @param ignoreTokens List of tokens to ignore during pattern extraction
 * @param enableHierarchicalPatterns Whether to enable hierarchical pattern extraction
 * @param hierarchyThresholds Thresholds for hierarchical pattern levels
 */
public record LogMineConfig(
    // Clustering configuration
    double similarityThreshold,
    int minClusterSize,
    int maxClusters,

    // Strategy configuration
    TokenizerStrategy tokenizerStrategy,
    VariableDetector variableDetector,

    // Tokenization configuration
    TokenizationStrategy tokenizationStrategy,
    String customDelimiters,

    // Pre-processing configuration
    boolean normalizeTimestamps,
    boolean normalizeIPs,
    boolean normalizeNumbers,
    boolean normalizePaths,
    boolean normalizeUrls,
    boolean caseSensitive,

    // Pattern extraction configuration
    int minPatternLength,
    int maxPatternLength,
    double minPatternSpecificity,
    List<String> ignoreTokens,

    // Hierarchical pattern configuration
    boolean enableHierarchicalPatterns,
    List<Double> hierarchyThresholds) {

  /** Compact constructor with validation. */
  public LogMineConfig {
    // Validate similarity threshold
    if (similarityThreshold < 0.0 || similarityThreshold > 1.0) {
      throw new IllegalArgumentException("Similarity threshold must be between 0.0 and 1.0");
    }

    // Validate min cluster size
    if (minClusterSize < 1) {
      throw new IllegalArgumentException("Min cluster size must be at least 1");
    }

    // Validate max clusters
    if (maxClusters < 1) {
      throw new IllegalArgumentException("Max clusters must be at least 1");
    }

    // Validate strategies
    if (tokenizerStrategy == null) {
      throw new IllegalArgumentException("Tokenizer strategy cannot be null");
    }
    if (variableDetector == null) {
      throw new IllegalArgumentException("Variable detector cannot be null");
    }
    if (tokenizationStrategy == null) {
      throw new IllegalArgumentException("Tokenization strategy cannot be null");
    }

    // Validate pattern lengths
    if (minPatternLength < 1) {
      throw new IllegalArgumentException("Min pattern length must be at least 1");
    }
    if (maxPatternLength < minPatternLength) {
      throw new IllegalArgumentException("Max pattern length must be >= min pattern length");
    }

    // Validate pattern specificity
    if (minPatternSpecificity < 0.0 || minPatternSpecificity > 1.0) {
      throw new IllegalArgumentException("Min pattern specificity must be between 0.0 and 1.0");
    }

    // Make defensive copies of mutable collections
    ignoreTokens = List.copyOf(ignoreTokens != null ? ignoreTokens : List.of());
    hierarchyThresholds =
        List.copyOf(hierarchyThresholds != null ? hierarchyThresholds : List.of());
  }

  /**
   * Creates a default configuration suitable for most log types.
   *
   * @return A LogMineConfig with balanced default settings
   */
  public static LogMineConfig defaultConfig() {
    return builder().build();
  }

  /**
   * Alias for defaultConfig() to match expected API.
   *
   * @return A LogMineConfig with balanced default settings
   */
  public static LogMineConfig defaults() {
    return defaultConfig();
  }

  /**
   * Creates a new builder for custom configuration.
   *
   * @return A new Builder instance with default settings
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a configuration optimized for web server logs (Apache, Nginx, etc.).
   *
   * @return A LogMineConfig optimized for web server log analysis
   */
  public static LogMineConfig webServerConfig() {
    return builder()
        .similarityThreshold(0.7)
        .normalizeIPs(true)
        .normalizeTimestamps(true)
        .normalizeNumbers(true)
        .normalizeUrls(true)
        .build();
  }

  /**
   * Creates a configuration optimized for application logs (Java, Python, etc.).
   *
   * @return A LogMineConfig optimized for application log analysis
   */
  public static LogMineConfig applicationLogConfig() {
    return builder()
        .similarityThreshold(0.6)
        .normalizeTimestamps(true)
        .normalizeNumbers(true)
        .normalizePaths(true)
        .caseSensitive(false)
        .build();
  }

  /**
   * Creates a configuration optimized for system logs (syslog, systemd, etc.).
   *
   * @return A LogMineConfig optimized for system log analysis
   */
  public static LogMineConfig systemLogConfig() {
    return builder()
        .similarityThreshold(0.65)
        .normalizeTimestamps(true)
        .normalizeIPs(true)
        .normalizeNumbers(true)
        .minClusterSize(2)
        .build();
  }

  /**
   * Creates a configuration for handling multiple heterogeneous log sources.
   *
   * @return A LogMineConfig optimized for multi-source log analysis
   */
  public static LogMineConfig multiSourceConfig() {
    return builder()
        .similarityThreshold(0.5) // More lenient for diverse sources
        .normalizeTimestamps(true)
        .normalizeIPs(true)
        .normalizeNumbers(true)
        .normalizePaths(true)
        .normalizeUrls(true)
        .caseSensitive(false)
        .enableHierarchicalPatterns(true)
        .addHierarchyThreshold(0.8) // Specific patterns
        .addHierarchyThreshold(0.5) // Medium patterns
        .addHierarchyThreshold(0.3) // General patterns
        .build();
  }

  /** Tokenization strategies for different log formats. */
  public enum TokenizationStrategy {
    /** Default: split on whitespace and common delimiters */
    DEFAULT,
    /** Split only on whitespace */
    WHITESPACE_ONLY,
    /** Split on custom delimiters */
    CUSTOM,
    /** Smart tokenization that preserves quoted strings */
    SMART,
    /** CSV-style tokenization */
    CSV,
    /** JSON log tokenization */
    JSON
  }

  /**
   * Builder for LogMineConfig.
   *
   * <p>Provides a fluent API for constructing LogMineConfig instances with custom settings.
   */
  public static class Builder {
    private double similarityThreshold = 0.5;
    private int minClusterSize = 1;
    private int maxClusters = Integer.MAX_VALUE;
    private TokenizerStrategy tokenizerStrategy = new DelimiterPreservingTokenizer();
    private VariableDetector variableDetector = new StandardVariableDetector();
    private TokenizationStrategy tokenizationStrategy = TokenizationStrategy.DEFAULT;
    private String customDelimiters = "";
    private boolean normalizeTimestamps = false;
    private boolean normalizeIPs = false;
    private boolean normalizeNumbers = false;
    private boolean normalizePaths = false;
    private boolean normalizeUrls = false;
    private boolean caseSensitive = true;
    private int minPatternLength = 1;
    private int maxPatternLength = Integer.MAX_VALUE;
    private double minPatternSpecificity = 0.0;
    private final List<String> ignoreTokens = new ArrayList<>();
    private boolean enableHierarchicalPatterns = false;
    private final List<Double> hierarchyThresholds = new ArrayList<>();

    /** Creates a new Builder with default configuration settings. */
    public Builder() {
      // Default constructor
    }

    /**
     * Sets the similarity threshold for clustering.
     *
     * @param threshold Similarity threshold (0.0-1.0)
     * @return this Builder instance
     */
    public Builder similarityThreshold(double threshold) {
      if (threshold < 0.0 || threshold > 1.0) {
        throw new IllegalArgumentException("Similarity threshold must be between 0.0 and 1.0");
      }
      this.similarityThreshold = threshold;
      return this;
    }

    /**
     * Sets the similarity threshold (alias for similarityThreshold).
     *
     * @param threshold Similarity threshold (0.0-1.0)
     * @return this Builder instance
     */
    public Builder withSimilarityThreshold(double threshold) {
      return similarityThreshold(threshold);
    }

    /**
     * Sets the minimum cluster size (alias for minClusterSize).
     *
     * @param size Minimum number of messages required to form a cluster
     * @return this Builder instance
     */
    public Builder withMinClusterSize(int size) {
      return minClusterSize(size);
    }

    /**
     * Sets the tokenizer strategy (alias for withTokenizerStrategy).
     *
     * @param tokenizer The tokenizer strategy to use
     * @return this Builder instance
     */
    public Builder withTokenizer(TokenizerStrategy tokenizer) {
      return withTokenizerStrategy(tokenizer);
    }

    /**
     * Sets the tokenizer strategy for splitting log messages into tokens.
     *
     * @param tokenizer The tokenizer strategy to use
     * @return this Builder instance
     */
    public Builder withTokenizerStrategy(TokenizerStrategy tokenizer) {
      if (tokenizer == null) {
        throw new IllegalArgumentException("Tokenizer strategy cannot be null");
      }
      this.tokenizerStrategy = tokenizer;
      return this;
    }

    /**
     * Sets the variable detector for identifying variable parts in tokens.
     *
     * @param detector The variable detector to use
     * @return this Builder instance
     */
    public Builder withVariableDetector(VariableDetector detector) {
      if (detector == null) {
        throw new IllegalArgumentException("Variable detector cannot be null");
      }
      this.variableDetector = detector;
      return this;
    }

    /**
     * Sets the minimum cluster size.
     *
     * @param size Minimum number of messages required to form a cluster
     * @return this Builder instance
     */
    public Builder minClusterSize(int size) {
      if (size < 1) {
        throw new IllegalArgumentException("Min cluster size must be at least 1");
      }
      this.minClusterSize = size;
      return this;
    }

    /**
     * Sets the maximum number of clusters to maintain.
     *
     * @param max Maximum number of clusters
     * @return this Builder instance
     */
    public Builder maxClusters(int max) {
      this.maxClusters = max;
      return this;
    }

    /**
     * Sets the tokenization strategy for different log formats.
     *
     * @param strategy The tokenization strategy to use
     * @return this Builder instance
     */
    public Builder tokenizationStrategy(TokenizationStrategy strategy) {
      this.tokenizationStrategy = strategy;
      return this;
    }

    /**
     * Sets custom delimiters for tokenization.
     *
     * @param delimiters String of delimiter characters
     * @return this Builder instance
     */
    public Builder customDelimiters(String delimiters) {
      this.customDelimiters = delimiters;
      this.tokenizationStrategy = TokenizationStrategy.CUSTOM;
      return this;
    }

    /**
     * Enables or disables timestamp normalization.
     *
     * @param normalize Whether to normalize timestamps
     * @return this Builder instance
     */
    public Builder normalizeTimestamps(boolean normalize) {
      this.normalizeTimestamps = normalize;
      return this;
    }

    /**
     * Enables or disables IP address normalization.
     *
     * @param normalize Whether to normalize IP addresses
     * @return this Builder instance
     */
    public Builder normalizeIPs(boolean normalize) {
      this.normalizeIPs = normalize;
      return this;
    }

    /**
     * Enables or disables number normalization.
     *
     * @param normalize Whether to normalize numbers
     * @return this Builder instance
     */
    public Builder normalizeNumbers(boolean normalize) {
      this.normalizeNumbers = normalize;
      return this;
    }

    /**
     * Enables or disables file path normalization.
     *
     * @param normalize Whether to normalize file paths
     * @return this Builder instance
     */
    public Builder normalizePaths(boolean normalize) {
      this.normalizePaths = normalize;
      return this;
    }

    /**
     * Enables or disables URL normalization.
     *
     * @param normalize Whether to normalize URLs
     * @return this Builder instance
     */
    public Builder normalizeUrls(boolean normalize) {
      this.normalizeUrls = normalize;
      return this;
    }

    /**
     * Sets whether pattern matching is case-sensitive.
     *
     * @param sensitive Whether matching should be case-sensitive
     * @return this Builder instance
     */
    public Builder caseSensitive(boolean sensitive) {
      this.caseSensitive = sensitive;
      return this;
    }

    /**
     * Sets the minimum pattern length in tokens.
     *
     * @param length Minimum number of tokens in a pattern
     * @return this Builder instance
     */
    public Builder minPatternLength(int length) {
      this.minPatternLength = length;
      return this;
    }

    /**
     * Sets the maximum pattern length in tokens.
     *
     * @param length Maximum number of tokens in a pattern
     * @return this Builder instance
     */
    public Builder maxPatternLength(int length) {
      this.maxPatternLength = length;
      return this;
    }

    /**
     * Sets the minimum pattern specificity threshold.
     *
     * @param specificity Minimum specificity (0.0-1.0)
     * @return this Builder instance
     */
    public Builder minPatternSpecificity(double specificity) {
      this.minPatternSpecificity = specificity;
      return this;
    }

    /**
     * Adds a token to ignore during pattern extraction.
     *
     * @param token Token to ignore
     * @return this Builder instance
     */
    public Builder ignoreToken(String token) {
      this.ignoreTokens.add(token);
      return this;
    }

    /**
     * Adds multiple tokens to ignore during pattern extraction.
     *
     * @param tokens List of tokens to ignore
     * @return this Builder instance
     */
    public Builder ignoreTokens(List<String> tokens) {
      this.ignoreTokens.addAll(tokens);
      return this;
    }

    /**
     * Enables or disables hierarchical pattern extraction.
     *
     * @param enable Whether to enable hierarchical patterns
     * @return this Builder instance
     */
    public Builder enableHierarchicalPatterns(boolean enable) {
      this.enableHierarchicalPatterns = enable;
      return this;
    }

    /**
     * Adds a threshold for hierarchical pattern levels.
     *
     * @param threshold Threshold value (0.0-1.0)
     * @return this Builder instance
     */
    public Builder addHierarchyThreshold(double threshold) {
      this.hierarchyThresholds.add(threshold);
      return this;
    }

    /**
     * Builds the LogMineConfig instance with the configured settings.
     *
     * @return A new LogMineConfig instance
     */
    public LogMineConfig build() {
      return new LogMineConfig(
          similarityThreshold,
          minClusterSize,
          maxClusters,
          tokenizerStrategy,
          variableDetector,
          tokenizationStrategy,
          customDelimiters,
          normalizeTimestamps,
          normalizeIPs,
          normalizeNumbers,
          normalizePaths,
          normalizeUrls,
          caseSensitive,
          minPatternLength,
          maxPatternLength,
          minPatternSpecificity,
          ignoreTokens,
          enableHierarchicalPatterns,
          hierarchyThresholds);
    }
  }
}
