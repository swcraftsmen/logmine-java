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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for HierarchicalPattern. */
public class HierarchicalPatternTest {

  @Test
  public void testBasicCreation() {
    LogPattern pattern = createTestPattern("User * logged in");
    HierarchicalPattern hp = new HierarchicalPattern(0, 0.5, pattern, null);

    assertEquals(0, hp.getLevel());
    assertEquals(0.5, hp.getThreshold(), 0.001);
    assertEquals(pattern, hp.getPattern());
    assertNull(hp.getParent());
    assertTrue(hp.isRoot());
    assertTrue(hp.isLeaf());
  }

  @Test
  public void testHierarchyWithChildren() {
    // Level 0 (coarse)
    LogPattern coarsePattern = createTestPattern("User * *");
    HierarchicalPattern root = new HierarchicalPattern(0, 0.5, coarsePattern, null);

    // Level 1 (medium)
    LogPattern mediumPattern = createTestPattern("User * logged in");
    HierarchicalPattern child = new HierarchicalPattern(1, 0.7, mediumPattern, root);
    root.addChild(child);

    // Level 2 (fine)
    LogPattern finePattern = createTestPattern("User * logged in from *");
    HierarchicalPattern grandchild = new HierarchicalPattern(2, 0.9, finePattern, child);
    child.addChild(grandchild);

    // Verify root
    assertTrue(root.isRoot());
    assertFalse(root.isLeaf());
    assertEquals(1, root.getChildren().size());
    assertEquals(2, root.getDescendantCount());

    // Verify child
    assertFalse(child.isRoot());
    assertFalse(child.isLeaf());
    assertEquals(root, child.getParent());
    assertEquals(1, child.getChildren().size());
    assertEquals(1, child.getDescendantCount());

    // Verify grandchild
    assertFalse(grandchild.isRoot());
    assertTrue(grandchild.isLeaf());
    assertEquals(child, grandchild.getParent());
    assertEquals(0, grandchild.getChildren().size());
    assertEquals(0, grandchild.getDescendantCount());
  }

  @Test
  public void testGetPatternsAtLevel() {
    // Build hierarchy
    LogPattern l0Pattern = createTestPattern("User *");
    HierarchicalPattern root = new HierarchicalPattern(0, 0.5, l0Pattern, null);

    LogPattern l1Pattern1 = createTestPattern("User * logged in");
    HierarchicalPattern child1 = new HierarchicalPattern(1, 0.7, l1Pattern1, root);
    root.addChild(child1);

    LogPattern l1Pattern2 = createTestPattern("User * logged out");
    HierarchicalPattern child2 = new HierarchicalPattern(1, 0.7, l1Pattern2, root);
    root.addChild(child2);

    LogPattern l2Pattern = createTestPattern("User * logged in from *");
    HierarchicalPattern grandchild = new HierarchicalPattern(2, 0.9, l2Pattern, child1);
    child1.addChild(grandchild);

    // Test level 0
    List<LogPattern> level0 = root.getPatternsAtLevel(0);
    assertEquals(1, level0.size());
    assertEquals(l0Pattern, level0.get(0));

    // Test level 1
    List<LogPattern> level1 = root.getPatternsAtLevel(1);
    assertEquals(2, level1.size());
    assertTrue(level1.contains(l1Pattern1));
    assertTrue(level1.contains(l1Pattern2));

    // Test level 2
    List<LogPattern> level2 = root.getPatternsAtLevel(2);
    assertEquals(1, level2.size());
    assertEquals(l2Pattern, level2.get(0));
  }

  @Test
  public void testGetLeafPatterns() {
    // Build hierarchy
    LogPattern l0Pattern = createTestPattern("User *");
    HierarchicalPattern root = new HierarchicalPattern(0, 0.5, l0Pattern, null);

    LogPattern l1Pattern1 = createTestPattern("User * logged in");
    HierarchicalPattern child1 = new HierarchicalPattern(1, 0.7, l1Pattern1, root);
    root.addChild(child1);

    LogPattern l1Pattern2 = createTestPattern("User * logged out");
    HierarchicalPattern child2 = new HierarchicalPattern(1, 0.7, l1Pattern2, root);
    root.addChild(child2);

    LogPattern l2Pattern = createTestPattern("User * logged in from *");
    HierarchicalPattern grandchild = new HierarchicalPattern(2, 0.9, l2Pattern, child1);
    child1.addChild(grandchild);

    // Get leaf patterns (most specific)
    List<LogPattern> leaves = root.getLeafPatterns();

    // Should have 2 leaves: grandchild and child2
    assertEquals(2, leaves.size());
    assertTrue(leaves.contains(l2Pattern));
    assertTrue(leaves.contains(l1Pattern2));
  }

  @Test
  public void testGetPathFromRoot() {
    // Build hierarchy
    LogPattern l0Pattern = createTestPattern("User *");
    HierarchicalPattern root = new HierarchicalPattern(0, 0.5, l0Pattern, null);

    LogPattern l1Pattern = createTestPattern("User * logged in");
    HierarchicalPattern child = new HierarchicalPattern(1, 0.7, l1Pattern, root);
    root.addChild(child);

    LogPattern l2Pattern = createTestPattern("User * logged in from *");
    HierarchicalPattern grandchild = new HierarchicalPattern(2, 0.9, l2Pattern, child);
    child.addChild(grandchild);

    // Get path from root to grandchild
    List<LogPattern> path = grandchild.getPathFromRoot();

    assertEquals(3, path.size());
    assertEquals(l0Pattern, path.get(0));
    assertEquals(l1Pattern, path.get(1));
    assertEquals(l2Pattern, path.get(2));
  }

  @Test
  public void testInvalidLevel() {
    LogPattern pattern = createTestPattern("Test");

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new HierarchicalPattern(-1, 0.5, pattern, null);
        });
  }

  @Test
  public void testInvalidThreshold() {
    LogPattern pattern = createTestPattern("Test");

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new HierarchicalPattern(0, -0.1, pattern, null);
        });

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new HierarchicalPattern(0, 1.5, pattern, null);
        });
  }

  @Test
  public void testNullPattern() {
    assertThrows(
        NullPointerException.class,
        () -> {
          new HierarchicalPattern(0, 0.5, null, null);
        });
  }

  @Test
  public void testInvalidChildLevel() {
    LogPattern pattern1 = createTestPattern("User *");
    HierarchicalPattern parent = new HierarchicalPattern(1, 0.7, pattern1, null);

    LogPattern pattern2 = createTestPattern("User * logged in");
    HierarchicalPattern child = new HierarchicalPattern(0, 0.5, pattern2, parent);

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          parent.addChild(child);
        });
  }

  @Test
  public void testToString() {
    LogPattern pattern = createTestPattern("User * logged in");
    HierarchicalPattern hp = new HierarchicalPattern(1, 0.7, pattern, null);

    String str = hp.toString();

    assertTrue(str.contains("L1"));
    assertTrue(str.contains("0.70"));
    assertTrue(str.contains("User * logged in"));
  }

  @Test
  public void testEqualsAndHashCode() {
    LogPattern pattern1 = createTestPattern("User * logged in");
    LogPattern pattern2 = createTestPattern("User * logged in");

    HierarchicalPattern hp1 = new HierarchicalPattern(1, 0.7, pattern1, null);
    HierarchicalPattern hp2 = new HierarchicalPattern(1, 0.7, pattern2, null);
    HierarchicalPattern hp3 = new HierarchicalPattern(2, 0.7, pattern1, null);

    assertEquals(hp1, hp2);
    assertEquals(hp1.hashCode(), hp2.hashCode());
    assertNotEquals(hp1, hp3);
  }

  @Test
  public void testSaaSUseCase() {
    // Simulate SaaS dashboard with different granularity levels

    // Level 0 (Dashboard - high-level overview)
    LogPattern dashboardPattern = createTestPattern("* * *");
    HierarchicalPattern dashboard = new HierarchicalPattern(0, 0.5, dashboardPattern, null);

    // Level 1 (Alerts - balanced detail)
    LogPattern alertPattern = createTestPattern("* User * *");
    HierarchicalPattern alert = new HierarchicalPattern(1, 0.7, alertPattern, dashboard);
    dashboard.addChild(alert);

    // Level 2 (Debugging - detailed)
    LogPattern debugPattern = createTestPattern("ERROR User * login failed");
    HierarchicalPattern debug = new HierarchicalPattern(2, 0.9, debugPattern, alert);
    alert.addChild(debug);

    // Verify customers can get appropriate level
    assertEquals(0, dashboard.getLevel()); // For dashboard
    assertEquals(1, alert.getLevel()); // For alert config
    assertEquals(2, debug.getLevel()); // For debugging

    // Verify customers can drill down
    List<LogPattern> path = debug.getPathFromRoot();
    assertEquals(3, path.size());
  }

  // ========== Edge Case Tests ==========

  @Test
  public void testGetPatternsAtNonExistentLevel() {
    LogPattern pattern = createTestPattern("User *");
    HierarchicalPattern root = new HierarchicalPattern(0, 0.5, pattern, null);

    // Query for non-existent levels
    List<LogPattern> level5 = root.getPatternsAtLevel(5);
    assertNotNull(level5);
    assertTrue(level5.isEmpty());

    List<LogPattern> level99 = root.getPatternsAtLevel(99);
    assertNotNull(level99);
    assertTrue(level99.isEmpty());
  }

  @Test
  public void testBoundaryThresholds() {
    LogPattern pattern = createTestPattern("Test");

    // Threshold = 0.0 (minimum)
    HierarchicalPattern hp1 = new HierarchicalPattern(0, 0.0, pattern, null);
    assertEquals(0.0, hp1.getThreshold(), 0.001);

    // Threshold = 1.0 (maximum)
    HierarchicalPattern hp2 = new HierarchicalPattern(0, 1.0, pattern, null);
    assertEquals(1.0, hp2.getThreshold(), 0.001);
  }

  @Test
  public void testDeepHierarchy() {
    // Create a deep hierarchy (10 levels)
    LogPattern pattern0 = createTestPattern("Level 0");
    HierarchicalPattern current = new HierarchicalPattern(0, 0.5, pattern0, null);
    HierarchicalPattern root = current;

    for (int i = 1; i < 10; i++) {
      LogPattern pattern = createTestPattern("Level " + i);
      HierarchicalPattern child = new HierarchicalPattern(i, 0.5 + (i * 0.04), pattern, current);
      current.addChild(child);
      current = child;
    }

    // Verify depth
    assertEquals(9, root.getDescendantCount());

    // Verify path from deepest node
    List<LogPattern> path = current.getPathFromRoot();
    assertEquals(10, path.size());

    // Verify leaf
    List<LogPattern> leaves = root.getLeafPatterns();
    assertEquals(1, leaves.size());
  }

  @Test
  public void testWideHierarchy() {
    // Create a wide hierarchy (1 root, 10 children)
    LogPattern rootPattern = createTestPattern("Root");
    HierarchicalPattern root = new HierarchicalPattern(0, 0.5, rootPattern, null);

    for (int i = 0; i < 10; i++) {
      LogPattern childPattern = createTestPattern("Child " + i);
      HierarchicalPattern child = new HierarchicalPattern(1, 0.7, childPattern, root);
      root.addChild(child);
    }

    // Verify children count
    assertEquals(10, root.getChildren().size());
    assertEquals(10, root.getDescendantCount());

    // All children should be leaves
    List<LogPattern> leaves = root.getLeafPatterns();
    assertEquals(10, leaves.size());

    // Level 1 should have 10 patterns
    List<LogPattern> level1 = root.getPatternsAtLevel(1);
    assertEquals(10, level1.size());
  }

  @Test
  public void testComplexMultiBranchHierarchy() {
    // Create a complex hierarchy with multiple branches
    LogPattern rootPattern = createTestPattern("*");
    HierarchicalPattern root = new HierarchicalPattern(0, 0.5, rootPattern, null);

    // Branch 1: Error logs
    LogPattern errorPattern = createTestPattern("ERROR *");
    HierarchicalPattern errorBranch = new HierarchicalPattern(1, 0.7, errorPattern, root);
    root.addChild(errorBranch);

    LogPattern dbErrorPattern = createTestPattern("ERROR Database *");
    HierarchicalPattern dbError = new HierarchicalPattern(2, 0.9, dbErrorPattern, errorBranch);
    errorBranch.addChild(dbError);

    LogPattern apiErrorPattern = createTestPattern("ERROR API *");
    HierarchicalPattern apiError = new HierarchicalPattern(2, 0.9, apiErrorPattern, errorBranch);
    errorBranch.addChild(apiError);

    // Branch 2: Info logs
    LogPattern infoPattern = createTestPattern("INFO *");
    HierarchicalPattern infoBranch = new HierarchicalPattern(1, 0.7, infoPattern, root);
    root.addChild(infoBranch);

    LogPattern userInfoPattern = createTestPattern("INFO User *");
    HierarchicalPattern userInfo = new HierarchicalPattern(2, 0.9, userInfoPattern, infoBranch);
    infoBranch.addChild(userInfo);

    // Verify structure
    assertEquals(2, root.getChildren().size()); // ERROR and INFO branches
    assertEquals(5, root.getDescendantCount()); // Total descendants
    assertFalse(root.isLeaf());
    assertTrue(root.isRoot());

    // Verify leaves (only level 2 nodes)
    List<LogPattern> leaves = root.getLeafPatterns();
    assertEquals(3, leaves.size());
    assertTrue(leaves.contains(dbErrorPattern));
    assertTrue(leaves.contains(apiErrorPattern));
    assertTrue(leaves.contains(userInfoPattern));

    // Verify level 2 patterns
    List<LogPattern> level2 = root.getPatternsAtLevel(2);
    assertEquals(3, level2.size());
  }

  @Test
  public void testEqualsWithNull() {
    LogPattern pattern = createTestPattern("Test");
    HierarchicalPattern hp = new HierarchicalPattern(0, 0.5, pattern, null);

    assertNotEquals(null, hp);
    assertNotEquals(hp, null);
  }

  @Test
  public void testEqualsWithDifferentType() {
    LogPattern pattern = createTestPattern("Test");
    HierarchicalPattern hp = new HierarchicalPattern(0, 0.5, pattern, null);

    assertNotEquals(hp, "String");
    assertNotEquals(hp, 123);
    assertNotEquals(hp, new Object());
  }

  @Test
  public void testEqualsSameInstance() {
    LogPattern pattern = createTestPattern("Test");
    HierarchicalPattern hp = new HierarchicalPattern(0, 0.5, pattern, null);

    assertEquals(hp, hp);
    assertEquals(hp.hashCode(), hp.hashCode());
  }

  @Test
  public void testEqualsDifferentThreshold() {
    LogPattern pattern1 = createTestPattern("Test");
    LogPattern pattern2 = createTestPattern("Test");

    HierarchicalPattern hp1 = new HierarchicalPattern(0, 0.5, pattern1, null);
    HierarchicalPattern hp2 = new HierarchicalPattern(0, 0.7, pattern2, null);

    assertNotEquals(hp1, hp2);
  }

  @Test
  public void testEqualsDifferentPattern() {
    LogPattern pattern1 = createTestPattern("Test 1");
    LogPattern pattern2 = createTestPattern("Test 2");

    HierarchicalPattern hp1 = new HierarchicalPattern(0, 0.5, pattern1, null);
    HierarchicalPattern hp2 = new HierarchicalPattern(0, 0.5, pattern2, null);

    assertNotEquals(hp1, hp2);
  }

  @Test
  public void testSingleNodeHierarchy() {
    LogPattern pattern = createTestPattern("Single");
    HierarchicalPattern single = new HierarchicalPattern(0, 0.5, pattern, null);

    assertTrue(single.isRoot());
    assertTrue(single.isLeaf());
    assertEquals(0, single.getChildren().size());
    assertEquals(0, single.getDescendantCount());
    assertNull(single.getParent());

    // Leaf patterns should contain just this pattern
    List<LogPattern> leaves = single.getLeafPatterns();
    assertEquals(1, leaves.size());
    assertEquals(pattern, leaves.get(0));

    // Path should contain just this pattern
    List<LogPattern> path = single.getPathFromRoot();
    assertEquals(1, path.size());
    assertEquals(pattern, path.get(0));
  }

  @Test
  public void testGetPathFromRootForRoot() {
    LogPattern pattern = createTestPattern("Root");
    HierarchicalPattern root = new HierarchicalPattern(0, 0.5, pattern, null);

    List<LogPattern> path = root.getPathFromRoot();
    assertEquals(1, path.size());
    assertEquals(pattern, path.get(0));
  }

  @Test
  public void testDescendantCountWithMultipleLevels() {
    LogPattern l0 = createTestPattern("L0");
    HierarchicalPattern root = new HierarchicalPattern(0, 0.5, l0, null);

    // Add 2 children at level 1
    for (int i = 0; i < 2; i++) {
      LogPattern l1 = createTestPattern("L1_" + i);
      HierarchicalPattern child = new HierarchicalPattern(1, 0.7, l1, root);
      root.addChild(child);

      // Add 3 grandchildren for each child
      for (int j = 0; j < 3; j++) {
        LogPattern l2 = createTestPattern("L2_" + i + "_" + j);
        HierarchicalPattern grandchild = new HierarchicalPattern(2, 0.9, l2, child);
        child.addChild(grandchild);
      }
    }

    // Total: 2 children + 6 grandchildren = 8 descendants
    assertEquals(8, root.getDescendantCount());
  }

  @Test
  public void testToStringWithDifferentLevels() {
    LogPattern pattern = createTestPattern("Test pattern");

    HierarchicalPattern l0 = new HierarchicalPattern(0, 0.5, pattern, null);
    assertTrue(l0.toString().contains("L0"));
    assertTrue(l0.toString().contains("0.50"));

    HierarchicalPattern l5 = new HierarchicalPattern(5, 0.95, pattern, null);
    assertTrue(l5.toString().contains("L5"));
    assertTrue(l5.toString().contains("0.95"));
  }

  @Test
  public void testImmutableChildren() {
    LogPattern rootPattern = createTestPattern("Root");
    HierarchicalPattern root = new HierarchicalPattern(0, 0.5, rootPattern, null);

    LogPattern childPattern = createTestPattern("Child");
    HierarchicalPattern child = new HierarchicalPattern(1, 0.7, childPattern, root);
    root.addChild(child);

    // Get children list
    List<HierarchicalPattern> children = root.getChildren();
    assertEquals(1, children.size());

    // Attempting to modify the returned list should not affect the internal state
    // (depends on implementation - this tests the contract)
    int originalSize = children.size();
    assertThrows(
        UnsupportedOperationException.class,
        () -> {
          children.clear();
        });

    // Verify internal state unchanged
    assertEquals(1, root.getChildren().size());
  }

  /** Helper method to create a test pattern. */
  private LogPattern createTestPattern(String patternString) {
    List<String> tokens = Arrays.asList(patternString.split(" "));
    return new LogPattern(tokens, 1, new org.swengdev.logmine.strategy.StandardVariableDetector());
  }
}
