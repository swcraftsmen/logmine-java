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
import java.util.Objects;

/**
 * Represents a hierarchical pattern extracted at multiple levels of specificity.
 *
 * <p>In SaaS platforms with diverse log sources, hierarchical patterns provide:
 *
 * <ul>
 *   <li><b>Level 0 (Coarse):</b> High-level view, fewer patterns, good for dashboards
 *   <li><b>Level 1 (Medium):</b> Balanced view, moderate detail
 *   <li><b>Level 2 (Fine):</b> Detailed view, more patterns, good for debugging
 * </ul>
 *
 * <p>Example hierarchy:
 *
 * <pre>{@code
 * Level 0 (threshold=0.5): "User * * from *"
 *   ├─ Level 1 (threshold=0.7): "User * logged in from *"
 *   │    ├─ Level 2 (threshold=0.9): "User * logged in from * via web"
 *   │    └─ Level 2 (threshold=0.9): "User * logged in from * via mobile"
 *   └─ Level 1 (threshold=0.7): "User * logged out from *"
 * }</pre>
 *
 * <p>Use cases:
 *
 * <ul>
 *   <li>Customer dashboard: Show Level 0 patterns (overview)
 *   <li>Alert configuration: Use Level 1 patterns (balanced)
 *   <li>Debugging: Use Level 2 patterns (detailed)
 * </ul>
 *
 * <p>Thread-safety: This class is immutable and thread-safe.
 *
 * @see LogPattern
 * @see LogMineProcessor#extractHierarchicalPatterns()
 */
public class HierarchicalPattern {

  private final int level;
  private final double threshold;
  private final LogPattern pattern;
  private final List<HierarchicalPattern> children;
  private final HierarchicalPattern parent;

  /**
   * Constructs a hierarchical pattern node.
   *
   * @param level Hierarchy level (0 = coarsest, higher = more specific)
   * @param threshold Similarity threshold used at this level
   * @param pattern The log pattern at this level
   * @param parent Parent pattern (null for root)
   */
  public HierarchicalPattern(
      int level, double threshold, LogPattern pattern, HierarchicalPattern parent) {
    if (level < 0) {
      throw new IllegalArgumentException("Level must be non-negative");
    }
    if (threshold < 0.0 || threshold > 1.0) {
      throw new IllegalArgumentException("Threshold must be between 0.0 and 1.0");
    }
    Objects.requireNonNull(pattern, "Pattern cannot be null");

    this.level = level;
    this.threshold = threshold;
    this.pattern = pattern;
    this.parent = parent;
    this.children = new ArrayList<>();
  }

  /**
   * Adds a child pattern at a more specific level.
   *
   * @param child Child pattern to add
   */
  public void addChild(HierarchicalPattern child) {
    if (child.level <= this.level) {
      throw new IllegalArgumentException("Child level must be greater than parent level");
    }
    this.children.add(child);
  }

  /**
   * Gets the hierarchy level.
   *
   * @return Level (0 = coarsest, higher = more specific)
   */
  public int getLevel() {
    return level;
  }

  /**
   * Gets the similarity threshold used at this level.
   *
   * @return Threshold value between 0.0 and 1.0
   */
  public double getThreshold() {
    return threshold;
  }

  /**
   * Gets the log pattern at this level.
   *
   * @return LogPattern instance
   */
  public LogPattern getPattern() {
    return pattern;
  }

  /**
   * Gets the parent pattern (coarser level).
   *
   * @return Parent pattern or null if this is a root
   */
  public HierarchicalPattern getParent() {
    return parent;
  }

  /**
   * Gets child patterns (more specific levels).
   *
   * @return Unmodifiable list of children
   */
  public List<HierarchicalPattern> getChildren() {
    return List.copyOf(children);
  }

  /**
   * Checks if this is a root pattern (no parent).
   *
   * @return true if root, false otherwise
   */
  public boolean isRoot() {
    return parent == null;
  }

  /**
   * Checks if this is a leaf pattern (no children).
   *
   * @return true if leaf, false otherwise
   */
  public boolean isLeaf() {
    return children.isEmpty();
  }

  /**
   * Gets the number of descendants (children + their children, etc.).
   *
   * @return Total number of descendants
   */
  public int getDescendantCount() {
    int count = children.size();
    for (HierarchicalPattern child : children) {
      count += child.getDescendantCount();
    }
    return count;
  }

  /**
   * Gets all patterns at a specific level in this subtree.
   *
   * @param targetLevel The level to retrieve
   * @return List of patterns at the specified level
   */
  public List<LogPattern> getPatternsAtLevel(int targetLevel) {
    List<LogPattern> result = new ArrayList<>();
    collectPatternsAtLevel(targetLevel, result);
    return result;
  }

  private void collectPatternsAtLevel(int targetLevel, List<LogPattern> result) {
    if (this.level == targetLevel) {
      result.add(this.pattern);
    }
    for (HierarchicalPattern child : children) {
      child.collectPatternsAtLevel(targetLevel, result);
    }
  }

  /**
   * Gets all leaf patterns (most specific) in this subtree.
   *
   * @return List of leaf patterns
   */
  public List<LogPattern> getLeafPatterns() {
    List<LogPattern> result = new ArrayList<>();
    collectLeafPatterns(result);
    return result;
  }

  private void collectLeafPatterns(List<LogPattern> result) {
    if (isLeaf()) {
      result.add(this.pattern);
    } else {
      for (HierarchicalPattern child : children) {
        child.collectLeafPatterns(result);
      }
    }
  }

  /**
   * Gets the path from root to this pattern.
   *
   * @return List of patterns from root to this node
   */
  public List<LogPattern> getPathFromRoot() {
    List<LogPattern> path = new ArrayList<>();
    HierarchicalPattern current = this;
    while (current != null) {
      path.add(0, current.pattern);
      current = current.parent;
    }
    return path;
  }

  /**
   * Returns a string representation showing the hierarchy.
   *
   * @return Hierarchical string representation
   */
  @Override
  public String toString() {
    return toString(0);
  }

  private String toString(int indent) {
    StringBuilder sb = new StringBuilder();
    sb.append("  ".repeat(indent));
    sb.append(
        String.format(
            "L%d (t=%.2f): %s [support=%d]%n",
            level, threshold, pattern.getSignature(), pattern.getSupportCount()));

    for (HierarchicalPattern child : children) {
      sb.append(child.toString(indent + 1));
    }

    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    HierarchicalPattern that = (HierarchicalPattern) o;
    return level == that.level
        && Double.compare(that.threshold, threshold) == 0
        && Objects.equals(pattern, that.pattern);
  }

  @Override
  public int hashCode() {
    return Objects.hash(level, threshold, pattern);
  }
}
