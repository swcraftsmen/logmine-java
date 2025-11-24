# Hierarchical Patterns Explained

This guide explains how hierarchical patterns work and why they're useful.

## Table of Contents
1. [What Are Hierarchical Patterns?](#what-are-hierarchical-patterns)
2. [Why Use Hierarchical Patterns?](#why-use-hierarchical-patterns)
3. [How It Works](#how-it-works)
4. [Visual Example](#visual-example)
5. [Use Cases](#use-cases)
6. [Configuration](#configuration)
7. [Code Examples](#code-examples)
8. [Best Practices](#best-practices)

---

## What Are Hierarchical Patterns?

**Hierarchical patterns** are log patterns extracted at **multiple levels of specificity**.

Think of it like a **zoom lens**:
- **Zoomed out (Level 0)**: See the big picture (coarse patterns)
- **Normal view (Level 1)**: See balanced detail (medium patterns)
- **Zoomed in (Level 2)**: See fine details (specific patterns)

### Single-Level Patterns (Standard)

```
Pattern: "User * logged in from *"
  - Matches: "User alice logged in from 192.168.1.1"
  - Matches: "User bob logged in from mobile"
  - Matches: "User charlie logged in from web"
```

**Problem**: What if you want BOTH a high-level overview AND detailed breakdown?

### Hierarchical Patterns (Solution)

```
Level 0 (Coarse):  "User * *"
  └─ Level 1 (Medium):  "User * logged in"
      └─ Level 2 (Fine):  "User * logged in from web"
      └─ Level 2 (Fine):  "User * logged in from mobile"
  └─ Level 1 (Medium):  "User * logged out"
```

**Benefit**: Choose the right level of detail for your use case!

---

## Why Use Hierarchical Patterns?

### Problem: Different Users Need Different Granularity

**Dashboard User**: "Just show me high-level patterns!"
```
❌ 5,000 patterns → Information overload!
✅ 20 coarse patterns → Perfect overview!
```

**DevOps Engineer**: "I need detailed patterns for alerts!"
```
❌ 20 coarse patterns → Not specific enough!
✅ 200 medium patterns → Perfect for alerts!
```

**Developer Debugging**: "I need to see exact patterns!"
```
❌ 200 medium patterns → Missing details!
✅ 1,000 fine patterns → Perfect for debugging!
```

### Solution: Hierarchical Patterns

Extract patterns at **multiple thresholds** simultaneously:

| Level | Threshold | Pattern Count | Use Case |
|-------|-----------|---------------|----------|
| 0 (Coarse) | 0.5 | 10-50 | Dashboard, overview |
| 1 (Medium) | 0.7 | 50-200 | Alerts, monitoring |
| 2 (Fine) | 0.9 | 200-1000+ | Debugging, analysis |

**Each user gets the granularity they need!**

---

## How It Works

### Step 1: Configure Thresholds

```java
LogMineConfig config = LogMineConfig.builder()
    .enableHierarchicalPatterns(true)
    .addHierarchyThreshold(0.5)   // Level 0: Coarse
    .addHierarchyThreshold(0.7)   // Level 1: Medium
    .addHierarchyThreshold(0.9)   // Level 2: Fine
    .build();
```

### Step 2: Extract Patterns at Each Level

The algorithm processes logs **three times** with different thresholds:

```
Threshold 0.5 (Lenient clustering):
  → Few large clusters
  → Fewer, more general patterns
  → Level 0 patterns

Threshold 0.7 (Balanced clustering):
  → Moderate clusters
  → Balanced patterns
  → Level 1 patterns

Threshold 0.9 (Strict clustering):
  → Many small clusters
  → More specific patterns
  → Level 2 patterns
```

### Step 3: Build Hierarchy

The algorithm links patterns from different levels:

```
Coarse Pattern (Level 0)
  ├─ Medium Pattern (Level 1)
  │   ├─ Fine Pattern (Level 2)
  │   └─ Fine Pattern (Level 2)
  └─ Medium Pattern (Level 1)
      └─ Fine Pattern (Level 2)
```

**Each child is a more specific version of its parent!**

---

## Visual Example

### Input Logs

```
1.  INFO User alice logged in successfully from web
2.  INFO User bob logged in successfully from web
3.  INFO User charlie logged in successfully from mobile
4.  INFO User dave logged in successfully from mobile
5.  WARN User eve login failed invalid_password from web
6.  WARN User frank login failed invalid_password from web
7.  ERROR User grace login failed account_locked from mobile
8.  ERROR User henry login failed account_locked from mobile
```

### Level 0: Coarse Patterns (threshold=0.5)

Very lenient clustering → Few general patterns:

```
Pattern 1: "* User * * from *"
  Support: 8 logs
  └─ All logs cluster together (very general)
```

### Level 1: Medium Patterns (threshold=0.7)

Balanced clustering → More specific patterns:

```
Pattern 1.1: "* User * logged in successfully from *"
  Support: 4 logs (logs 1-4)
  Parent: Pattern 1

Pattern 1.2: "* User * login failed * from *"
  Support: 4 logs (logs 5-8)
  Parent: Pattern 1
```

### Level 2: Fine Patterns (threshold=0.9)

Strict clustering → Very specific patterns:

```
Pattern 1.1.1: "INFO User * logged in successfully from web"
  Support: 2 logs (logs 1-2)
  Parent: Pattern 1.1

Pattern 1.1.2: "INFO User * logged in successfully from mobile"
  Support: 2 logs (logs 3-4)
  Parent: Pattern 1.1

Pattern 1.2.1: "WARN User * login failed invalid_password from *"
  Support: 2 logs (logs 5-6)
  Parent: Pattern 1.2

Pattern 1.2.2: "ERROR User * login failed account_locked from *"
  Support: 2 logs (logs 7-8)
  Parent: Pattern 1.2
```

### Hierarchy Tree

```
Level 0 [Coarse]:
└─ "* User * * from *" (8 logs)
    │
    Level 1 [Medium]:
    ├─ "* User * logged in successfully from *" (4 logs)
    │   │
    │   Level 2 [Fine]:
    │   ├─ "INFO User * logged in successfully from web" (2 logs)
    │   └─ "INFO User * logged in successfully from mobile" (2 logs)
    │
    └─ "* User * login failed * from *" (4 logs)
        │
        Level 2 [Fine]:
        ├─ "WARN User * login failed invalid_password from *" (2 logs)
        └─ "ERROR User * login failed account_locked from *" (2 logs)
```

---

## Use Cases

### Use Case 1: Dashboard - High-Level Overview

**Goal**: Show executives high-level trends

```java
List<HierarchicalPattern> hierarchy = processor.extractHierarchicalPatterns();

// Get only Level 0 (coarse) patterns
for (HierarchicalPattern root : hierarchy) {
    LogPattern pattern = root.getPattern();
    System.out.printf("%s - seen %d times\n", 
        pattern.getSignature(), 
        pattern.getSupportCount());
}
```

**Output**:
```
* User * * from * - seen 8 times
* Connection * from * - seen 3 times
* Request * returned * - seen 5 times
```

**Result**: Clean, high-level overview! ✅

---

### Use Case 2: Alerts - Balanced Detail

**Goal**: Configure alerts on specific patterns

```java
List<HierarchicalPattern> hierarchy = processor.extractHierarchicalPatterns();

// Get Level 1 (medium) patterns
for (HierarchicalPattern root : hierarchy) {
    List<LogPattern> mediumPatterns = root.getPatternsAtLevel(1);
    
    for (LogPattern pattern : mediumPatterns) {
        // Configure alert if pattern matches error condition
        if (pattern.getSignature().contains("failed")) {
            setupAlert(pattern);
        }
    }
}
```

**Output**:
```
Alert: "* User * login failed * from *" (4 occurrences)
Alert: "* Connection * failed timeout" (2 occurrences)
```

**Result**: Right level of specificity for alerts! ✅

---

### Use Case 3: Debugging - Fine Details

**Goal**: Find exact error patterns for debugging

```java
List<HierarchicalPattern> hierarchy = processor.extractHierarchicalPatterns();

// Get all leaf patterns (most specific)
for (HierarchicalPattern root : hierarchy) {
    List<LogPattern> leafPatterns = root.getLeafPatterns();
    
    for (LogPattern pattern : leafPatterns) {
        if (pattern.getSignature().contains("ERROR")) {
            System.out.println(pattern.getSignature());
        }
    }
}
```

**Output**:
```
ERROR User * login failed account_locked from mobile
ERROR Connection * failed timeout after NUM ms
ERROR Database query failed deadlock on table *
```

**Result**: Exact patterns for debugging! ✅

---

### Use Case 4: Drill-Down Navigation

**Goal**: Let users drill down from coarse to fine

```java
// Start with coarse patterns
HierarchicalPattern root = hierarchy.get(0);
System.out.println("Level 0: " + root.getPattern().getSignature());

// User clicks to see details → show children
for (HierarchicalPattern child : root.getChildren()) {
    System.out.println("  Level 1: " + child.getPattern().getSignature());
    
    // User clicks again → show fine details
    for (HierarchicalPattern grandchild : child.getChildren()) {
        System.out.println("    Level 2: " + grandchild.getPattern().getSignature());
    }
}
```

**Output**:
```
Level 0: * User * * from *
  Level 1: * User * logged in successfully from *
    Level 2: INFO User * logged in successfully from web
    Level 2: INFO User * logged in successfully from mobile
  Level 1: * User * login failed * from *
    Level 2: WARN User * login failed invalid_password from *
    Level 2: ERROR User * login failed account_locked from *
```

**Result**: Interactive exploration of patterns! ✅

---

## Configuration

### Basic Configuration

```java
LogMineConfig config = LogMineConfig.builder()
    .enableHierarchicalPatterns(true)
    .addHierarchyThreshold(0.5)   // Level 0
    .addHierarchyThreshold(0.7)   // Level 1
    .addHierarchyThreshold(0.9)   // Level 2
    .build();
```

### Custom Thresholds

You can define as many levels as you need:

```java
LogMineConfig config = LogMineConfig.builder()
    .enableHierarchicalPatterns(true)
    .addHierarchyThreshold(0.4)   // Level 0: Very coarse
    .addHierarchyThreshold(0.6)   // Level 1: Coarse
    .addHierarchyThreshold(0.7)   // Level 2: Medium
    .addHierarchyThreshold(0.85)  // Level 3: Fine
    .addHierarchyThreshold(0.95)  // Level 4: Very fine
    .build();
```

### Threshold Guidelines

| Threshold | Clustering | Pattern Count | Best For |
|-----------|------------|---------------|----------|
| 0.3-0.5 | Very lenient | 5-20 | Executive dashboard |
| 0.5-0.7 | Lenient | 20-100 | High-level monitoring |
| 0.7-0.8 | Balanced | 100-500 | Alerts, routing |
| 0.8-0.9 | Strict | 500-2000 | Detailed analysis |
| 0.9-1.0 | Very strict | 2000+ | Debugging, forensics |

---

## Code Examples

### Example 1: Extract Hierarchical Patterns

```java
// Configure processor with hierarchical patterns
LogMineConfig config = LogMineConfig.builder()
    .normalizeTimestamps(true)
    .normalizeIPs(true)
    .normalizeNumbers(true)
    .enableHierarchicalPatterns(true)
    .addHierarchyThreshold(0.5)
    .addHierarchyThreshold(0.7)
    .addHierarchyThreshold(0.9)
    .build();

LogMineProcessor processor = new LogMineProcessor(config);

// Process logs
List<String> logs = Arrays.asList(
    "INFO User alice logged in from 192.168.1.1",
    "INFO User bob logged in from 10.0.0.1",
    "WARN User charlie login failed from 192.168.1.5",
    "WARN User dave login failed from 10.0.0.5"
);
processor.process(logs);

// Extract hierarchical patterns
List<HierarchicalPattern> hierarchy = processor.extractHierarchicalPatterns();
```

### Example 2: Get Patterns at Specific Level

```java
// Get coarse patterns (Level 0)
List<LogPattern> coarsePatterns = new ArrayList<>();
for (HierarchicalPattern root : hierarchy) {
    coarsePatterns.addAll(root.getPatternsAtLevel(0));
}

// Get medium patterns (Level 1)
List<LogPattern> mediumPatterns = new ArrayList<>();
for (HierarchicalPattern root : hierarchy) {
    mediumPatterns.addAll(root.getPatternsAtLevel(1));
}

// Get fine patterns (Level 2)
List<LogPattern> finePatterns = new ArrayList<>();
for (HierarchicalPattern root : hierarchy) {
    finePatterns.addAll(root.getPatternsAtLevel(2));
}
```

### Example 3: Navigate Hierarchy

```java
// Start at root
HierarchicalPattern root = hierarchy.get(0);

System.out.println("Root pattern: " + root.getPattern().getSignature());
System.out.println("Support count: " + root.getPattern().getSupportCount());
System.out.println("Is root: " + root.isRoot());
System.out.println("Is leaf: " + root.isLeaf());
System.out.println("Child count: " + root.getChildren().size());

// Navigate to children
for (HierarchicalPattern child : root.getChildren()) {
    System.out.println("  Child: " + child.getPattern().getSignature());
    
    // Navigate to grandchildren
    for (HierarchicalPattern grandchild : child.getChildren()) {
        System.out.println("    Grandchild: " + grandchild.getPattern().getSignature());
    }
}
```

### Example 4: Get Path from Root

```java
// Get a leaf pattern
HierarchicalPattern root = hierarchy.get(0);
HierarchicalPattern leaf = root.getLeafPatterns().get(0);

// Get path from root to this leaf
List<LogPattern> path = leaf.getPathFromRoot();

System.out.println("Pattern evolution:");
for (int i = 0; i < path.size(); i++) {
    String indent = "  ".repeat(i);
    System.out.println(indent + "Level " + i + ": " + path.get(i).getSignature());
}
```

**Output**:
```
Pattern evolution:
Level 0: * User * *
  Level 1: * User * logged in
    Level 2: INFO User * logged in from web
```

---

## Best Practices

### 1. Use 3 Levels for Most Cases

```java
.addHierarchyThreshold(0.5)   // Coarse
.addHierarchyThreshold(0.7)   // Medium  ← Default
.addHierarchyThreshold(0.9)   // Fine
```

**Why**: Balances overview, monitoring, and debugging needs.

### 2. Adjust Based on Log Diversity

**Uniform logs** (same format):
```java
.addHierarchyThreshold(0.7)   // Less aggressive thresholds
.addHierarchyThreshold(0.85)
```

**Diverse logs** (many formats):
```java
.addHierarchyThreshold(0.4)   // More aggressive thresholds
.addHierarchyThreshold(0.6)
.addHierarchyThreshold(0.8)
```

### 3. Cache Hierarchies (Expensive to Compute)

```java
// Extract once
private List<HierarchicalPattern> cachedHierarchy = null;

public List<HierarchicalPattern> getHierarchy() {
    if (cachedHierarchy == null) {
        cachedHierarchy = processor.extractHierarchicalPatterns();
    }
    return cachedHierarchy;
}

// Invalidate when logs change
public void addLog(String log) {
    processor.processLogIncremental(log);
    cachedHierarchy = null;  // Invalidate cache
}
```

### 4. Use Appropriate Level for Each Use Case

| Use Case | Recommended Level | Why |
|----------|------------------|-----|
| Dashboard | 0 (Coarse) | Clean overview |
| Monitoring | 1 (Medium) | Balance detail/noise |
| Alerts | 1 (Medium) | Specific enough to act |
| Debugging | 2 (Fine) | Maximum detail |
| Forensics | Leaf patterns | Exact matches |

### 5. Enable Preprocessing for Better Hierarchies

```java
LogMineConfig config = LogMineConfig.builder()
    // Preprocessing makes hierarchies more meaningful
    .normalizeTimestamps(true)
    .normalizeIPs(true)
    .normalizeNumbers(true)
    .caseSensitive(false)
    
    // Then enable hierarchical patterns
    .enableHierarchicalPatterns(true)
    .addHierarchyThreshold(0.5)
    .addHierarchyThreshold(0.7)
    .addHierarchyThreshold(0.9)
    .build();
```

---

## Performance Considerations

### Computational Cost

Hierarchical patterns are **expensive** to compute:

```
Single-level:  O(n) - Process logs once
Hierarchical:  O(k * n) - Process logs k times (k = number of thresholds)
```

**For 3 levels**: ~3x slower than single-level extraction

### When to Use

✅ **Use hierarchical patterns when**:
- You need multiple granularity levels
- Users have different detail requirements
- Building interactive drill-down UIs
- Logs are processed in batch mode

❌ **Skip hierarchical patterns when**:
- You only need one granularity level
- Performance is critical
- Real-time streaming with tight latency requirements
- Memory is constrained

### Optimization Tips

1. **Extract periodically, not on every log**:
   ```java
   @Scheduled(fixedRate = 300000)  // Every 5 minutes
   public void updateHierarchy() {
       hierarchy = processor.extractHierarchicalPatterns();
   }
   ```

2. **Limit threshold count**:
   ```java
   // ✅ GOOD: 3 thresholds
   .addHierarchyThreshold(0.5)
   .addHierarchyThreshold(0.7)
   .addHierarchyThreshold(0.9)
   
   // ❌ BAD: 10 thresholds (too slow!)
   ```

3. **Use streaming mode for base patterns**:
   ```java
   // Process logs in streaming mode
   logMine = new LogMine(ProcessingMode.STREAMING, ...);
   
   // Extract hierarchy only when needed
   hierarchy = processor.extractHierarchicalPatterns();
   ```

---

## Troubleshooting

### Issue: Too Many Patterns at Coarse Level

**Symptom**: Level 0 has 100+ patterns (should be 10-50)

**Cause**: Threshold too high

**Solution**: Lower the coarse threshold
```java
// Before
.addHierarchyThreshold(0.7)  // Too strict

// After
.addHierarchyThreshold(0.5)  // More lenient
```

---

### Issue: Not Enough Patterns at Fine Level

**Symptom**: Level 2 has only 10 patterns (should be 100+)

**Cause**: Threshold too low

**Solution**: Raise the fine threshold
```java
// Before
.addHierarchyThreshold(0.7)  // Too lenient

// After
.addHierarchyThreshold(0.9)  // More strict
```

---

### Issue: Hierarchy is Flat (No Parent-Child Relationships)

**Symptom**: All patterns are at one level

**Cause**: Thresholds too similar

**Solution**: Increase threshold gaps
```java
// Before (BAD)
.addHierarchyThreshold(0.7)
.addHierarchyThreshold(0.72)  // Too close!
.addHierarchyThreshold(0.75)  // Too close!

// After (GOOD)
.addHierarchyThreshold(0.5)   // Big gaps
.addHierarchyThreshold(0.7)
.addHierarchyThreshold(0.9)
```

---

## Summary

**Hierarchical patterns provide multiple views of your logs**:

- **Level 0 (Coarse)**: High-level overview, few patterns
- **Level 1 (Medium)**: Balanced detail, moderate patterns
- **Level 2 (Fine)**: Detailed analysis, many patterns

**Use them when**:
- Different users need different granularity
- Building drill-down UIs
- Need both overview and detail

**Key configuration**:
```java
.enableHierarchicalPatterns(true)
.addHierarchyThreshold(0.5)  // Coarse
.addHierarchyThreshold(0.7)  // Medium
.addHierarchyThreshold(0.9)  // Fine
```

**Result**: One API, multiple granularity levels! 🎯

---

## See Also

- [Configuration Guide](CONFIGURATION_GUIDE.md) - Threshold tuning
- [Multi-Format Support](MULTI_FORMAT_SUPPORT.md) - Per-source hierarchies
- `HierarchicalPatternTest.java` - Unit tests with examples

