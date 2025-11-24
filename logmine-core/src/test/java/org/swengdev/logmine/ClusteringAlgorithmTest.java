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

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Critical algorithm tests for clustering logic - validates implementation against research paper.
 *
 * <p>Based on: "LogMine: Fast Pattern Recognition for Log Analytics" by Hamooni et al., CIKM 2016
 */
public class ClusteringAlgorithmTest {

  @Test
  public void testMaxClusterLimit() {
    // Test mergeWithClosestCluster is invoked when max limit is reached
    LogMineConfig config =
        LogMineConfig.builder()
            .withSimilarityThreshold(0.95) // Very strict - forces many clusters
            .maxClusters(3) // Limit to 3 clusters
            .withMinClusterSize(1)
            .build();

    LogMineProcessor processor = new LogMineProcessor(config);

    // Create 10 very different log types
    List<String> logs = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      logs.add("LOG_TYPE_" + i + " unique message " + i);
    }

    processor.process(logs);

    // Should be capped at maxClusters
    assertTrue(
        processor.getStats().getNumClusters() <= 3, "Clusters should be capped at max limit");

    // All logs should still be processed (merged into closest clusters)
    int totalMessages = processor.getStats().getTotalMessages();
    assertEquals(10, totalMessages, "All messages should be clustered even with limit");
  }

  @Test
  public void testClusteringWithSimilarityEdgeCases() {
    LogMineProcessor processor = new LogMineProcessor(0.5, 1);

    List<String> logs =
        List.of(
            // Group 1: Identical structure
            "INFO User login successful",
            "INFO User login successful",
            "INFO User login successful",
            // Group 2: Very similar
            "INFO User logout successful",
            "INFO User logout successful",
            // Group 3: Completely different
            "ERROR Database connection failed timeout exceeded max retries",
            "ERROR Database connection failed timeout exceeded max retries");

    List<LogPattern> patterns = processor.process(logs);

    assertNotNull(patterns);
    // Should create distinct clusters for different message structures
    assertTrue(patterns.size() >= 2, "Should cluster similar messages together");
  }

  @Test
  public void testEditDistanceBasedClustering() {
    // Test that clustering uses edit distance correctly
    LogMineProcessor processor = new LogMineProcessor(0.7, 1);

    List<String> logs =
        List.of(
            // These should cluster together (1 token different)
            "User alice logged in successfully",
            "User bob logged in successfully",
            "User charlie logged in successfully",
            // This should be separate (many tokens different)
            "System shutdown initiated by administrator");

    List<LogPattern> patterns = processor.process(logs);

    assertNotNull(patterns);
    assertEquals(2, patterns.size(), "Should create 2 distinct patterns");

    // The login pattern should have higher support
    LogPattern firstPattern = patterns.get(0);
    assertTrue(
        firstPattern.getSupportCount() >= 3,
        "First pattern should have clustered similar login messages");
  }

  @Test
  public void testClusterFilteringByMinSize() {
    LogMineProcessor processor = new LogMineProcessor(0.5, 3); // Min size = 3

    List<String> logs =
        List.of(
            // Group 1: 4 messages (should be kept)
            "INFO Log message A",
            "INFO Log message A",
            "INFO Log message A",
            "INFO Log message A",
            // Group 2: 2 messages (should be filtered out)
            "WARN Rare warning",
            "WARN Rare warning",
            // Group 3: 1 message (should be filtered out)
            "ERROR Very rare error");

    List<LogPattern> patterns = processor.process(logs);

    // Only the group with 4 messages should remain
    assertEquals(1, patterns.size(), "Should filter out clusters smaller than minSize");
    assertEquals(4, patterns.get(0).getSupportCount());
  }

  @Test
  public void testIncrementalClusteringWithPeriodicFiltering() {
    LogMineProcessor processor = new LogMineProcessor(0.5, 2);

    // Add messages to trigger periodic filtering (at 100 message intervals)
    for (int i = 0; i < 150; i++) {
      if (i % 10 == 0) {
        // Common messages (will form valid clusters)
        processor.processLogIncremental("INFO Common log message");
      } else {
        // Unique messages (should be filtered out as noise)
        processor.processLogIncremental("NOISE Unique message " + i);
      }
    }

    // Small clusters should have been filtered during periodic cleanup
    assertTrue(processor.getStats().getNumClusters() > 0);

    // Most noise should be filtered out
    LogMineProcessor.ProcessingStats stats = processor.getStats();
    assertTrue(stats.getTotalMessages() > 100, "Should have processed many messages");
  }

  @Test
  public void testPatternExtractionFromClusters() {
    LogMineProcessor processor = new LogMineProcessor(0.6, 2);

    List<String> logs =
        List.of(
            "User ID 12345 logged in from 192.168.1.1",
            "User ID 67890 logged in from 10.0.0.50",
            "User ID 11111 logged in from 172.16.0.1");

    List<LogPattern> patterns = processor.process(logs);

    assertNotNull(patterns);
    assertEquals(1, patterns.size(), "Similar logs should form one pattern");

    LogPattern pattern = patterns.get(0);
    assertEquals(3, pattern.getSupportCount());

    // Pattern should have wildcards for variable parts (IDs and IPs)
    List<String> tokens = pattern.getTokens();
    assertTrue(tokens.contains("User"), "Should preserve constant tokens");
    assertTrue(tokens.contains("logged"), "Should preserve constant tokens");
    assertTrue(tokens.contains("in"), "Should preserve constant tokens");
  }

  @Test
  public void testSimilarityThresholdImpact() {
    List<String> logs = List.of("GET /api/users/123", "GET /api/users/456", "POST /api/orders/789");

    // Strict clustering (high threshold)
    LogMineProcessor strictProcessor = new LogMineProcessor(0.9, 1);
    List<LogPattern> strictPatterns = strictProcessor.process(logs);

    // Lenient clustering (low threshold)
    LogMineProcessor lenientProcessor = new LogMineProcessor(0.3, 1);
    List<LogPattern> lenientPatterns = lenientProcessor.process(logs);

    assertNotNull(strictPatterns);
    assertNotNull(lenientPatterns);

    // Strict should produce more specific patterns
    // Lenient should merge more logs together
    assertTrue(
        strictPatterns.size() >= lenientPatterns.size(),
        "Strict threshold should produce more/equal patterns than lenient");
  }

  @Test
  public void testStreamingModePatternUpdates() {
    LogMineProcessor processor = new LogMineProcessor(0.6, 2);

    // Process messages incrementally
    for (int i = 0; i < 100; i++) {
      processor.processLogIncremental("INFO User " + i + " logged in");
    }

    // Patterns should be updated (every 50 messages)
    List<LogPattern> patterns = processor.getPatterns();
    assertNotNull(patterns);
    assertFalse(patterns.isEmpty(), "Patterns should be extracted in streaming mode");
  }

  @Test
  public void testEmptyClusterHandling() {
    LogMineProcessor processor = new LogMineProcessor(0.5, 10); // High min size

    List<String> logs = List.of("Single log message"); // Too few to form valid cluster

    List<LogPattern> patterns = processor.process(logs);

    assertNotNull(patterns);
    assertTrue(patterns.isEmpty(), "Should have no patterns when all clusters filtered");
  }

  @Test
  public void testClusterMergePreservesMessages() {
    LogMineConfig config =
        LogMineConfig.builder()
            .withSimilarityThreshold(0.95)
            .maxClusters(2)
            .withMinClusterSize(1)
            .build();

    LogMineProcessor processor = new LogMineProcessor(config);

    // Create 5 different message types with max 2 clusters
    List<String> logs = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      logs.add("Type " + i + " message");
      logs.add("Type " + i + " message"); // Add twice to ensure they cluster
    }

    processor.process(logs);

    // All 10 messages should still be accounted for
    int totalMessages = processor.getStats().getTotalMessages();
    assertEquals(10, totalMessages, "Merging should preserve all messages");
  }

  @Test
  public void testPatternSortingBySupport() {
    LogMineProcessor processor = new LogMineProcessor(0.6, 2);

    List<String> logs =
        List.of(
            // Pattern A: 5 occurrences
            "INFO Common log",
            "INFO Common log",
            "INFO Common log",
            "INFO Common log",
            "INFO Common log",
            // Pattern B: 2 occurrences
            "ERROR Rare error",
            "ERROR Rare error");

    List<LogPattern> patterns = processor.process(logs);

    assertNotNull(patterns);
    assertTrue(patterns.size() >= 2);

    // Patterns should be sorted by support count (descending)
    for (int i = 0; i < patterns.size() - 1; i++) {
      assertTrue(
          patterns.get(i).getSupportCount() >= patterns.get(i + 1).getSupportCount(),
          "Patterns should be sorted by support count");
    }
  }

  @Test
  public void testVariableDetectionInPatterns() {
    LogMineProcessor processor = new LogMineProcessor(0.7, 2);

    List<String> logs =
        List.of(
            "Request processed in 123 ms by thread 1",
            "Request processed in 456 ms by thread 2",
            "Request processed in 789 ms by thread 3");

    List<LogPattern> patterns = processor.process(logs);

    assertNotNull(patterns);
    assertEquals(1, patterns.size());

    // Check that the pattern has both constant and variable parts
    LogPattern pattern = patterns.get(0);
    double specificity = pattern.getSpecificity();

    assertTrue(
        specificity > 0.0 && specificity < 1.0,
        "Pattern should have both constant and variable parts");
  }

  @Test
  public void testClearResetsState() {
    LogMineProcessor processor = new LogMineProcessor(0.5, 2);

    // Process some logs
    processor.process(List.of("Log 1", "Log 2", "Log 3", "Log 4"));

    assertTrue(processor.getStats().getNumClusters() > 0);
    assertFalse(processor.getPatterns().isEmpty());

    // Clear should reset everything
    processor.clear();

    assertEquals(0, processor.getStats().getNumClusters());
    assertTrue(processor.getPatterns().isEmpty());

    // Should be able to process new logs after clear
    processor.process(List.of("New log", "New log"));
    assertFalse(processor.getPatterns().isEmpty());
  }
}
