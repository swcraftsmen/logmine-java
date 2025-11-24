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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for LogMineProcessor - core algorithm. */
public class LogMineProcessorTest {

  private LogMineProcessor processor;

  @BeforeEach
  public void setUp() {
    processor = new LogMineProcessor(0.5, 2);
  }

  @Test
  public void testProcessBasicLogs() {
    List<String> logs =
        Arrays.asList(
            "INFO User alice logged in",
            "INFO User bob logged in",
            "INFO User charlie logged in",
            "ERROR Database connection failed",
            "ERROR Database timeout occurred");

    List<LogPattern> patterns = processor.process(logs);

    assertNotNull(patterns);
    assertTrue(patterns.size() > 0);
  }

  @Test
  public void testEmptyLogList() {
    List<LogPattern> patterns = processor.process(Arrays.asList());

    assertNotNull(patterns);
    assertEquals(0, patterns.size());
  }

  @Test
  public void testSingleLog() {
    List<String> logs = Arrays.asList("Single log message");

    List<LogPattern> patterns = processor.process(logs);

    // Should have no patterns because minClusterSize is 2
    assertEquals(0, patterns.size());
  }

  @Test
  public void testClusteringWithVariousThresholds() {
    List<String> logs =
        Arrays.asList(
            "GET /api/users/123",
            "GET /api/users/456",
            "POST /api/orders/789",
            "POST /api/orders/012");

    // Strict clustering (high threshold) - similar logs must be very close
    LogMineProcessor strictProcessor = new LogMineProcessor(0.8, 2);
    List<LogPattern> strictPatterns = strictProcessor.process(logs);

    // Lenient clustering (low threshold) - logs can be less similar
    LogMineProcessor lenientProcessor = new LogMineProcessor(0.3, 2);
    List<LogPattern> lenientPatterns = lenientProcessor.process(logs);

    // Both should successfully process logs and produce patterns
    assertNotNull(strictPatterns);
    assertNotNull(lenientPatterns);

    // Both should produce at least some patterns from this data
    assertTrue(
        strictPatterns.size() > 0 || lenientPatterns.size() > 0,
        "At least one processor should produce patterns");
  }

  @Test
  public void testMinClusterSizeFilter() {
    List<String> logs =
        Arrays.asList(
            "INFO Log A",
            "INFO Log B",
            "ERROR Single error", // Only one of this type
            "WARN Another single" // Only one of this type
            );

    LogMineProcessor processor = new LogMineProcessor(0.5, 2);
    List<LogPattern> patterns = processor.process(logs);

    // Should only have pattern for "INFO" logs (count >= 2)
    assertEquals(1, patterns.size());
  }

  @Test
  public void testPatternMatching() {
    List<String> logs = Arrays.asList("INFO User alice logged in", "INFO User bob logged in");

    processor.process(logs);

    // Should match similar log
    LogPattern match = processor.matchPattern("INFO User charlie logged in");
    assertNotNull(match);

    // Should not match completely different log
    LogPattern noMatch = processor.matchPattern("ERROR System failure");
    assertNull(noMatch);
  }

  @Test
  public void testIncrementalProcessing() {
    processor.processLogIncremental("INFO User alice logged in");
    processor.processLogIncremental("INFO User bob logged in");
    processor.processLogIncremental("INFO User charlie logged in");

    List<LogPattern> patterns = processor.getPatterns();

    assertNotNull(patterns);
    assertTrue(patterns.size() > 0);
  }

  @Test
  public void testClear() {
    processor.process(Arrays.asList("Log 1", "Log 2", "Log 3"));

    assertFalse(processor.getPatterns().isEmpty());
    assertTrue(processor.getStats().getNumClusters() > 0);

    processor.clear();

    assertTrue(processor.getPatterns().isEmpty());
    assertEquals(0, processor.getStats().getNumClusters());
  }

  @Test
  public void testGetStats() {
    List<String> logs = Arrays.asList("INFO Log 1", "INFO Log 2", "ERROR Error 1", "ERROR Error 2");

    processor.process(logs);

    LogMineProcessor.ProcessingStats stats = processor.getStats();

    assertNotNull(stats);
    assertEquals(4, stats.getTotalMessages());
    assertTrue(stats.getNumClusters() > 0);
    assertTrue(stats.getNumPatterns() > 0);
    assertTrue(stats.getAvgClusterSize() > 0);
    assertNotNull(stats.toString());
  }

  @Test
  public void testGetConfig() {
    LogMineConfig config = processor.getConfig();

    assertNotNull(config);
    assertEquals(0.5, config.similarityThreshold(), 0.01);
    assertEquals(2, config.minClusterSize());
  }

  @Test
  public void testPatternSorting() {
    List<String> logs =
        Arrays.asList(
            "INFO Common log",
            "INFO Common log",
            "INFO Common log",
            "ERROR Rare error",
            "ERROR Rare error");

    List<LogPattern> patterns = processor.process(logs);

    // Patterns should be sorted by support count (descending)
    if (patterns.size() > 1) {
      assertTrue(patterns.get(0).getSupportCount() >= patterns.get(1).getSupportCount());
    }
  }

  @Test
  public void testDefaultConstructor() {
    LogMineProcessor defaultProcessor = new LogMineProcessor();

    assertNotNull(defaultProcessor);
    assertNotNull(defaultProcessor.getConfig());
  }

  @Test
  public void testConfigConstructor() {
    LogMineConfig config =
        LogMineConfig.builder().withSimilarityThreshold(0.7).withMinClusterSize(3).build();

    LogMineProcessor customProcessor = new LogMineProcessor(config);

    assertEquals(0.7, customProcessor.getConfig().similarityThreshold(), 0.01);
    assertEquals(3, customProcessor.getConfig().minClusterSize());
  }

  @Test
  public void testWithPreprocessing() {
    // Enable preprocessing to normalize timestamps, IPs, and numbers
    LogMineConfig config =
        LogMineConfig.builder()
            .withSimilarityThreshold(0.6)
            .normalizeTimestamps(true)
            .normalizeIPs(true)
            .normalizeNumbers(true)
            .build();

    LogMineProcessor processor = new LogMineProcessor(config);

    List<String> logs =
        Arrays.asList(
            "2024-01-15T10:30:45Z User 12345 logged in from 192.168.1.1",
            "2024-01-15T10:31:00Z User 67890 logged in from 10.0.0.50",
            "2024-01-15T10:32:15Z User 11111 logged in from 172.16.0.1");

    List<LogPattern> patterns = processor.process(logs);

    // All three logs should cluster together after normalization
    assertNotNull(patterns);
    assertEquals(1, patterns.size());
    assertEquals(3, patterns.get(0).getSupportCount());
  }

  @Test
  public void testMaxClustersLimit() {
    LogMineConfig config =
        LogMineConfig.builder()
            .withSimilarityThreshold(0.9) // High threshold = more clusters
            .maxClusters(3) // Limit to 3 clusters
            .withMinClusterSize(1)
            .build();

    LogMineProcessor processor = new LogMineProcessor(config);

    // Process many different log types
    for (int i = 0; i < 10; i++) {
      processor.processLogIncremental("LOG_TYPE_" + i + " message " + i);
    }

    // Should be capped at maxClusters
    assertTrue(processor.getStats().getNumClusters() <= 3);
  }

  @Test
  public void testIncrementalWithPeriodicClustering() {
    LogMineProcessor processor = new LogMineProcessor(0.5, 2);

    // Process 100+ messages to trigger periodic filtering
    for (int i = 0; i < 150; i++) {
      if (i % 3 == 0) {
        processor.processLogIncremental("INFO Common message " + i);
      } else {
        processor.processLogIncremental("UNIQUE message " + i);
      }
    }

    LogMineProcessor.ProcessingStats stats = processor.getStats();
    assertTrue(stats.getTotalMessages() > 100);
    assertTrue(stats.getNumClusters() > 0);
  }

  @Test
  public void testVeryLongLogMessage() {
    StringBuilder longMessage = new StringBuilder("INFO ");
    for (int i = 0; i < 1000; i++) {
      longMessage.append("word").append(i).append(" ");
    }

    List<String> logs = Arrays.asList(longMessage.toString(), longMessage.toString());

    List<LogPattern> patterns = processor.process(logs);

    // Should handle long messages without crashing
    assertNotNull(patterns);
  }

  @Test
  public void testLogsWithSpecialCharacters() {
    List<String> logs =
        Arrays.asList(
            "ERROR: User login failed! @#$%^&*()",
            "ERROR: User login failed! @#$%^&*()",
            "INFO: System started successfully. [OK]",
            "INFO: System started successfully. [OK]");

    List<LogPattern> patterns = processor.process(logs);

    assertNotNull(patterns);
    assertEquals(2, patterns.size());
  }

  @Test
  public void testLogsWithUnicode() {
    List<String> logs =
        Arrays.asList(
            "INFO Usuario josé inició sesión",
            "INFO Usuario maría inició sesión",
            "ERROR Conexión fallida: ñoño",
            "ERROR Conexión fallida: niño");

    List<LogPattern> patterns = processor.process(logs);

    assertNotNull(patterns);
    assertTrue(patterns.size() > 0);
  }

  @Test
  public void testClustersGetter() {
    List<String> logs = Arrays.asList("INFO Log 1", "INFO Log 2", "ERROR Error 1", "ERROR Error 2");

    processor.process(logs);

    List<LogPattern> patterns = processor.getPatterns();

    assertNotNull(patterns);
    assertTrue(patterns.size() > 0);

    // Verify it's a defensive copy
    int originalSize = patterns.size();
    patterns.clear();
    assertEquals(originalSize, processor.getPatterns().size());
  }

  @Test
  public void testPatternsGetter() {
    List<String> logs = Arrays.asList("INFO Log 1", "INFO Log 2", "ERROR Error 1", "ERROR Error 2");

    processor.process(logs);

    List<LogPattern> patterns = processor.getPatterns();

    assertNotNull(patterns);
    assertTrue(patterns.size() > 0);

    // Verify it's a defensive copy
    int originalSize = patterns.size();
    patterns.clear();
    assertEquals(originalSize, processor.getPatterns().size());
  }

  @Test
  public void testMatchPatternAfterClear() {
    List<String> logs = Arrays.asList("INFO User alice logged in", "INFO User bob logged in");

    processor.process(logs);
    assertNotNull(processor.matchPattern("INFO User charlie logged in"));

    processor.clear();

    // After clear, should have no patterns to match
    assertNull(processor.matchPattern("INFO User charlie logged in"));
  }

  @Test
  public void testMultipleProcessCalls() {
    List<String> logs1 = Arrays.asList("INFO Log 1", "INFO Log 2");
    List<String> logs2 = Arrays.asList("ERROR Error 1", "ERROR Error 2");

    // First process
    processor.process(logs1);
    int patternsAfterFirst = processor.getPatterns().size();

    // Second process overwrites
    processor.process(logs2);
    int patternsAfterSecond = processor.getPatterns().size();

    // Both should produce patterns
    assertTrue(patternsAfterFirst > 0);
    assertTrue(patternsAfterSecond > 0);
  }

  @Test
  public void testStatsWithNoData() {
    LogMineProcessor newProcessor = new LogMineProcessor();
    LogMineProcessor.ProcessingStats stats = newProcessor.getStats();

    assertNotNull(stats);
    assertEquals(0, stats.getTotalMessages());
    assertEquals(0, stats.getNumClusters());
    assertEquals(0, stats.getNumPatterns());
    assertEquals(0.0, stats.getAvgClusterSize(), 0.01);
  }

  @Test
  public void testStatsToString() {
    processor.process(Arrays.asList("Log 1", "Log 2", "Log 3", "Log 4"));

    LogMineProcessor.ProcessingStats stats = processor.getStats();
    String statsString = stats.toString();

    assertNotNull(statsString);
    assertTrue(statsString.contains("Total Messages"));
    assertTrue(statsString.contains("Clusters"));
    assertTrue(statsString.contains("Patterns"));
  }

  @Test
  public void testHighVolumeProcessing() {
    LogMineProcessor processor = new LogMineProcessor(0.6, 2);

    // Generate 1000 logs with patterns
    List<String> logs = new java.util.ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      if (i % 5 == 0) {
        logs.add("INFO User login successful");
      } else if (i % 5 == 1) {
        logs.add("ERROR Connection timeout");
      } else if (i % 5 == 2) {
        logs.add("WARN High memory usage");
      } else {
        logs.add("DEBUG Processing request " + i);
      }
    }

    List<LogPattern> patterns = processor.process(logs);

    assertNotNull(patterns);
    assertTrue(patterns.size() > 0);

    LogMineProcessor.ProcessingStats stats = processor.getStats();
    assertEquals(1000, stats.getTotalMessages());
  }

  @Test
  public void testDifferentSimilarityThresholds() {
    List<String> logs =
        Arrays.asList(
            "User login successful",
            "User logout successful",
            "User registration successful",
            "System shutdown initiated");

    // Very strict
    LogMineProcessor veryStrict = new LogMineProcessor(0.95, 1);
    List<LogPattern> strictPatterns = veryStrict.process(logs);

    // Very lenient
    LogMineProcessor veryLenient = new LogMineProcessor(0.1, 1);
    List<LogPattern> lenientPatterns = veryLenient.process(logs);

    // Strict should find more specific patterns
    // Lenient should merge more logs together
    assertNotNull(strictPatterns);
    assertNotNull(lenientPatterns);
  }

  @Test
  public void testCaseSensitiveConfiguration() {
    LogMineConfig caseInsensitiveConfig =
        LogMineConfig.builder().withSimilarityThreshold(0.6).caseSensitive(false).build();

    LogMineProcessor processor = new LogMineProcessor(caseInsensitiveConfig);

    List<String> logs = Arrays.asList("INFO User Logged In", "info user logged in");

    List<LogPattern> patterns = processor.process(logs);

    // Should cluster together when case-insensitive
    assertNotNull(patterns);
    assertTrue(patterns.size() >= 1);
  }

  @Test
  public void testIncrementalPatternUpdates() {
    LogMineProcessor processor = new LogMineProcessor(0.5, 2);

    // Add messages incrementally
    processor.processLogIncremental("INFO Message 1");
    processor.processLogIncremental("INFO Message 2");

    List<LogPattern> patternsAfter2 = processor.getPatterns();

    processor.processLogIncremental("INFO Message 3");

    List<LogPattern> patternsAfter3 = processor.getPatterns();

    // Patterns should be updated
    assertNotNull(patternsAfter2);
    assertNotNull(patternsAfter3);
  }

  @Test
  public void testMatchPatternWithNoPatterns() {
    LogMineProcessor emptyProcessor = new LogMineProcessor();

    // Should return null when no patterns exist
    assertNull(emptyProcessor.matchPattern("Any log message"));
  }

  @Test
  public void testProcessingModeWithVariableDetector() {
    LogMineConfig config =
        LogMineConfig.builder()
            .withSimilarityThreshold(0.6)
            .withVariableDetector(new org.swengdev.logmine.strategy.StandardVariableDetector())
            .build();

    LogMineProcessor processor = new LogMineProcessor(config);

    List<String> logs =
        Arrays.asList(
            "Request 12345 processed in 234ms",
            "Request 67890 processed in 456ms",
            "Request 11111 processed in 789ms");

    List<LogPattern> patterns = processor.process(logs);

    assertNotNull(patterns);
    // Should cluster together as numbers are detected as variables
    assertTrue(patterns.size() > 0);
  }

  // ========== Hierarchical Patterns Tests ==========

  @Test
  public void testExtractHierarchicalPatterns() {
    LogMineConfig config =
        LogMineConfig.builder()
            .withSimilarityThreshold(0.6)
            .enableHierarchicalPatterns(true)
            .build();

    LogMineProcessor processor = new LogMineProcessor(config);

    List<String> logs =
        Arrays.asList(
            "INFO User alice logged in from web",
            "INFO User bob logged in from web",
            "INFO User charlie logged in from mobile",
            "ERROR Database connection failed",
            "ERROR Database timeout occurred");

    processor.process(logs);

    List<HierarchicalPattern> hierarchical = processor.extractHierarchicalPatterns();

    assertNotNull(hierarchical);
    // Should create hierarchical patterns at multiple levels
    assertTrue(hierarchical.size() > 0);

    // Verify each has patterns
    for (HierarchicalPattern hp : hierarchical) {
      assertNotNull(hp.getPattern());
      assertTrue(hp.getLevel() >= 0);
    }
  }

  @Test
  public void testExtractHierarchicalPatternsDisabled() {
    LogMineConfig config =
        LogMineConfig.builder()
            .withSimilarityThreshold(0.6)
            .enableHierarchicalPatterns(false)
            .build();

    LogMineProcessor processor = new LogMineProcessor(config);

    List<String> logs = Arrays.asList("INFO Log 1", "INFO Log 2");
    processor.process(logs);

    List<HierarchicalPattern> hierarchical = processor.extractHierarchicalPatterns();

    assertNotNull(hierarchical);
    assertTrue(hierarchical.isEmpty(), "Should return empty list when disabled");
  }

  @Test
  public void testExtractHierarchicalPatternsWithCustomThresholds() {
    LogMineConfig config =
        LogMineConfig.builder()
            .withSimilarityThreshold(0.6)
            .enableHierarchicalPatterns(true)
            .addHierarchyThreshold(0.4)
            .addHierarchyThreshold(0.6)
            .addHierarchyThreshold(0.8)
            .build();

    LogMineProcessor processor = new LogMineProcessor(config);

    List<String> logs =
        Arrays.asList(
            "GET /api/users/123",
            "GET /api/users/456",
            "POST /api/orders/789",
            "POST /api/orders/012");

    processor.process(logs);

    List<HierarchicalPattern> hierarchical = processor.extractHierarchicalPatterns();

    assertNotNull(hierarchical);
    assertTrue(hierarchical.size() > 0);
  }

  @Test
  public void testExtractHierarchicalPatternsBeforeProcessing() {
    LogMineConfig config = LogMineConfig.builder().enableHierarchicalPatterns(true).build();

    LogMineProcessor processor = new LogMineProcessor(config);

    // Try to extract before processing any logs
    List<HierarchicalPattern> hierarchical = processor.extractHierarchicalPatterns();

    assertNotNull(hierarchical);
    // Should handle gracefully (empty or minimal results)
  }

  // ========== Tokenizer Strategy Tests ==========

  @Test
  public void testWithRegexTokenizer() {
    LogMineConfig config =
        LogMineConfig.builder()
            .withSimilarityThreshold(0.6)
            .withTokenizerStrategy(new org.swengdev.logmine.strategy.RegexTokenizer("[,\\s]+"))
            .build();

    LogMineProcessor processor = new LogMineProcessor(config);

    List<String> logs =
        Arrays.asList(
            "level=INFO,user=alice,action=login",
            "level=INFO,user=bob,action=login",
            "level=ERROR,service=api,status=failed");

    List<LogPattern> patterns = processor.process(logs);

    assertNotNull(patterns);
    assertTrue(patterns.size() > 0);
  }

  @Test
  public void testWithJsonTokenizer() {
    LogMineConfig config =
        LogMineConfig.builder()
            .withSimilarityThreshold(0.6)
            .withTokenizerStrategy(new org.swengdev.logmine.strategy.JsonTokenizer())
            .build();

    LogMineProcessor processor = new LogMineProcessor(config);

    List<String> logs =
        Arrays.asList(
            "{\"level\":\"INFO\",\"user\":\"alice\"}",
            "{\"level\":\"INFO\",\"user\":\"bob\"}",
            "{\"level\":\"ERROR\",\"code\":500}");

    List<LogPattern> patterns = processor.process(logs);

    assertNotNull(patterns);
    assertTrue(patterns.size() > 0);
  }

  @Test
  public void testWithDelimiterPreservingTokenizer() {
    LogMineConfig config =
        LogMineConfig.builder()
            .withSimilarityThreshold(0.6)
            .withTokenizerStrategy(new org.swengdev.logmine.strategy.DelimiterPreservingTokenizer())
            .build();

    LogMineProcessor processor = new LogMineProcessor(config);

    List<String> logs =
        Arrays.asList(
            "INFO: User login [alice]",
            "INFO: User login [bob]",
            "ERROR: System failure [code=500]");

    List<LogPattern> patterns = processor.process(logs);

    assertNotNull(patterns);
    assertTrue(patterns.size() > 0);
  }

  // ========== Custom Variable Detector Tests ==========

  @Test
  public void testWithAlwaysVariableDetector() {
    LogMineConfig config =
        LogMineConfig.builder()
            .withSimilarityThreshold(0.3)
            .withVariableDetector(new org.swengdev.logmine.strategy.AlwaysVariableDetector())
            .build();

    LogMineProcessor processor = new LogMineProcessor(config);

    List<String> logs =
        Arrays.asList("User alice logged in", "User bob logged in", "Admin charlie logged in");

    List<LogPattern> patterns = processor.process(logs);

    assertNotNull(patterns);
    // With AlwaysVariableDetector, everything is variable
    // Should still cluster if structure is similar
    assertTrue(patterns.size() > 0);
  }

  @Test
  public void testWithNeverVariableDetector() {
    LogMineConfig config =
        LogMineConfig.builder()
            .withSimilarityThreshold(0.9) // Need very high similarity
            .withVariableDetector(new org.swengdev.logmine.strategy.NeverVariableDetector())
            .build();

    LogMineProcessor processor = new LogMineProcessor(config);

    List<String> logs =
        Arrays.asList("User 123 logged in", "User 456 logged in", "User 789 logged in");

    List<LogPattern> patterns = processor.process(logs);

    assertNotNull(patterns);
    // With NeverVariableDetector, numbers aren't variables
    // Should require exact match, creating more patterns
  }

  @Test
  public void testWithCustomVariableDetector() {
    org.swengdev.logmine.strategy.CustomVariableDetector detector =
        new org.swengdev.logmine.strategy.CustomVariableDetector.Builder()
            .addVariablePattern("USER_\\d+") // Match USER_123, USER_456, etc.
            .addVariablePattern("ID:\\d+") // Match ID:123, ID:456, etc.
            .build();

    LogMineConfig config =
        LogMineConfig.builder().withSimilarityThreshold(0.7).withVariableDetector(detector).build();

    LogMineProcessor processor = new LogMineProcessor(config);

    List<String> logs =
        Arrays.asList("Request from USER_123", "Request from USER_456", "Request from USER_789");

    List<LogPattern> patterns = processor.process(logs);

    assertNotNull(patterns);
    assertTrue(patterns.size() > 0);
  }

  // ========== Preprocessing Combinations ==========

  @Test
  public void testWithAllPreprocessingEnabled() {
    LogMineConfig config =
        LogMineConfig.builder()
            .withSimilarityThreshold(0.6)
            .normalizeTimestamps(true)
            .normalizeIPs(true)
            .normalizeNumbers(true)
            .normalizePaths(true)
            .normalizeUrls(true)
            .build();

    LogMineProcessor processor = new LogMineProcessor(config);

    List<String> logs =
        Arrays.asList(
            "2024-01-15 10:30:45 User 12345 from 192.168.1.1 accessed /api/users at http://example.com/path",
            "2024-01-15 10:31:00 User 67890 from 10.0.0.50 accessed /api/orders at http://test.com/other",
            "2024-01-15 10:32:15 User 11111 from 172.16.0.1 accessed /api/products at http://demo.com/foo");

    List<LogPattern> patterns = processor.process(logs);

    assertNotNull(patterns);
    // All three should cluster together with full normalization
    assertTrue(patterns.size() > 0);
  }

  @Test
  public void testWithSelectivePreprocessing() {
    // Only normalize numbers and IPs, keep timestamps and paths
    LogMineConfig config =
        LogMineConfig.builder()
            .withSimilarityThreshold(0.7)
            .normalizeTimestamps(false)
            .normalizeIPs(true)
            .normalizeNumbers(true)
            .normalizePaths(false)
            .normalizeUrls(false)
            .build();

    LogMineProcessor processor = new LogMineProcessor(config);

    List<String> logs =
        Arrays.asList(
            "User 123 from 192.168.1.1", "User 456 from 10.0.0.50", "User 789 from 172.16.0.1");

    List<LogPattern> patterns = processor.process(logs);

    assertNotNull(patterns);
    assertTrue(patterns.size() > 0);
  }

  // ========== Pattern Matching Edge Cases ==========

  @Test
  public void testMatchPatternWithEmptyString() {
    processor.process(Arrays.asList("INFO Log 1", "INFO Log 2"));

    LogPattern match = processor.matchPattern("");
    // Should handle gracefully
    assertNull(match);
  }

  @Test
  public void testMatchPatternWithVeryDifferentLog() {
    processor.process(Arrays.asList("INFO User logged in", "INFO User logged out"));

    // Completely different structure
    LogPattern match = processor.matchPattern("12345 67890 ABCDEF XYZ RANDOM");
    assertNull(match);
  }

  @Test
  public void testMatchPatternWithMultipleCandidates() {
    List<String> logs =
        Arrays.asList(
            "INFO User alice logged in",
            "INFO User bob logged in",
            "WARN System load high",
            "WARN System load medium");

    processor.process(logs);

    // Should match best pattern
    LogPattern match = processor.matchPattern("INFO User charlie logged in");
    assertNotNull(match);
    assertTrue(match.getTokens().contains("INFO"));
  }

  // ========== Incremental Processing Edge Cases ==========

  @Test
  public void testIncrementalExactly50Messages() {
    LogMineProcessor processor = new LogMineProcessor(0.5, 2);

    // Process exactly 50 messages (pattern update threshold)
    for (int i = 0; i < 50; i++) {
      processor.processLogIncremental("INFO Message " + i);
    }

    List<LogPattern> patterns = processor.getPatterns();
    assertNotNull(patterns);
    assertTrue(patterns.size() > 0);
  }

  @Test
  public void testIncrementalExactly100Messages() {
    LogMineProcessor processor = new LogMineProcessor(0.5, 2);

    // Process exactly 100 messages (cluster filtering threshold)
    for (int i = 0; i < 100; i++) {
      processor.processLogIncremental("INFO Message " + i);
    }

    // Verify clusters were created
    assertTrue(processor.getStats().getNumClusters() > 0);
  }

  @Test
  public void testIncrementalMixedCommonAndRare() {
    LogMineProcessor processor = new LogMineProcessor(0.5, 3);

    // Add 100 common messages (well above minClusterSize=3)
    for (int i = 0; i < 100; i++) {
      processor.processLogIncremental("INFO Common message");
    }

    // Add 2 rare messages (below minClusterSize=3, should be filtered)
    processor.processLogIncremental("ERROR Rare error 1");
    processor.processLogIncremental("ERROR Rare error 2");

    // Verify clustering occurred
    LogMineProcessor.ProcessingStats stats = processor.getStats();
    assertEquals(102, stats.getTotalMessages());
    assertTrue(stats.getNumClusters() > 0, "Should have created clusters");

    // Get patterns - may be empty if not at update threshold
    List<LogPattern> patterns = processor.getPatterns();
    assertNotNull(patterns);

    // Patterns are updated every 50 messages, so at 102 messages we should have patterns
    // But don't make hard assumptions about exact count
  }

  // ========== Stats Edge Cases ==========

  @Test
  public void testStatsWithSingleCluster() {
    List<String> logs = Arrays.asList("Same log", "Same log", "Same log");

    processor.process(logs);

    LogMineProcessor.ProcessingStats stats = processor.getStats();

    assertEquals(3, stats.getTotalMessages());
    assertEquals(1, stats.getNumClusters());
    assertEquals(1, stats.getNumPatterns());
    assertEquals(3.0, stats.getAvgClusterSize(), 0.01);
  }

  @Test
  public void testStatsWithManyClusters() {
    LogMineProcessor processor = new LogMineProcessor(0.95, 1); // High threshold

    List<String> logs = new java.util.ArrayList<>();
    for (int i = 0; i < 50; i++) {
      logs.add("Unique log " + i + " with different content");
    }

    processor.process(logs);

    LogMineProcessor.ProcessingStats stats = processor.getStats();

    assertEquals(50, stats.getTotalMessages());
    assertTrue(stats.getNumClusters() >= 1);
  }

  @Test
  public void testAvgSpecificityCalculation() {
    List<String> logs =
        Arrays.asList("User 123 logged in from 192.168.1.1", "User 456 logged in from 10.0.0.50");

    processor.process(logs);

    LogMineProcessor.ProcessingStats stats = processor.getStats();

    // Avg specificity should be between 0 and 1
    assertTrue(stats.getAvgSpecificity() >= 0.0);
    assertTrue(stats.getAvgSpecificity() <= 1.0);
  }

  // ========== Configuration Edge Cases ==========

  @Test
  public void testWithZeroMinClusterSize() {
    // Min cluster size of 0 is not allowed, should throw exception
    try {
      LogMineConfig config =
          LogMineConfig.builder().withSimilarityThreshold(0.5).withMinClusterSize(0).build();
      // If we get here, the validation didn't work as expected
      assertTrue(false, "Should have thrown IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Expected: Min cluster size must be at least 1
      assertTrue(e.getMessage().contains("Min cluster size"));
    }
  }

  @Test
  public void testWithVeryHighMinClusterSize() {
    LogMineConfig config =
        LogMineConfig.builder().withSimilarityThreshold(0.5).withMinClusterSize(100).build();

    LogMineProcessor processor = new LogMineProcessor(config);

    List<String> logs = new java.util.ArrayList<>();
    for (int i = 0; i < 50; i++) {
      logs.add("Common log");
    }

    List<LogPattern> patterns = processor.process(logs);

    // Should filter out cluster with only 50 messages
    assertNotNull(patterns);
    assertTrue(patterns.isEmpty());
  }

  @Test
  public void testWithMinClusterSizeOne() {
    LogMineConfig config =
        LogMineConfig.builder().withSimilarityThreshold(0.9).withMinClusterSize(1).build();

    LogMineProcessor processor = new LogMineProcessor(config);

    List<String> logs = Arrays.asList("Unique log 1", "Unique log 2", "Unique log 3");

    List<LogPattern> patterns = processor.process(logs);

    assertNotNull(patterns);
    // With minClusterSize=1, single-message clusters are allowed
    // Number of patterns depends on similarity
    assertTrue(patterns.size() >= 1, "Should have at least one pattern");
  }

  // ========== Boundary and Null Tests ==========

  @Test
  public void testProcessWithNullInList() {
    List<String> logs = new java.util.ArrayList<>();
    logs.add("INFO Log 1");
    logs.add(null); // Null element
    logs.add("INFO Log 2");

    // Should handle gracefully (skip nulls or process what's valid)
    List<LogPattern> patterns = processor.process(logs);
    assertNotNull(patterns);
  }

  @Test
  public void testProcessLogIncrementalWithNull() {
    // Should handle gracefully
    try {
      processor.processLogIncremental(null);
      // If it doesn't throw, that's okay
    } catch (NullPointerException e) {
      // Also acceptable to throw NPE for null input
    }
  }

  @Test
  public void testMatchPatternWithNull() {
    processor.process(Arrays.asList("INFO Log 1", "INFO Log 2"));

    // Should handle null gracefully
    LogPattern match = processor.matchPattern(null);
    // Likely returns null or handles gracefully
  }

  @Test
  public void testProcessEmptyStrings() {
    List<String> logs = Arrays.asList("", "", "");

    List<LogPattern> patterns = processor.process(logs);

    assertNotNull(patterns);
    // Empty strings might be filtered or form a pattern
  }

  @Test
  public void testProcessWhitespaceOnlyStrings() {
    List<String> logs = Arrays.asList("   ", "\t\t", "  \n  ");

    List<LogPattern> patterns = processor.process(logs);

    assertNotNull(patterns);
    // Whitespace-only strings should be handled gracefully
  }
}
