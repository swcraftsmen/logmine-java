# Understanding the LogMine Algorithm

Visual, step-by-step explanation with examples.

---

## 📄 Research Paper

This algorithm is based on:

**Hamooni, H., Debnath, B., Xu, J., Zhang, H., Jiang, G., & Mueen, A. (2016).** *LogMine: Fast Pattern Recognition for Log Analytics.* CIKM 2016. 

📎 **Paper:** [https://www.cs.unm.edu/~mueen/Papers/LogMine.pdf](https://www.cs.unm.edu/~mueen/Papers/LogMine.pdf)

---

## 🎯 THE BIG PICTURE

```
INPUT: Thousands of messy log messages
   ↓
STEP 1: Group similar logs together (CLUSTERING)
   ↓
STEP 2: Find common patterns in each group (PATTERN EXTRACTION)
   ↓
STEP 3: Rank patterns by frequency (SORTING)
   ↓
OUTPUT: Clean, organized patterns
```

---

## 📚 STEP 1: CLUSTERING (Grouping Similar Logs)

### What is it?
Putting similar log messages into the same bucket.

### How does it work?

**Input:**
```
Log 1: "2015-07-09 10:22:12 INFO user=john action=login"
Log 2: "2015-07-09 14:35:01 INFO user=alice action=login"
Log 3: "2015-07-09 16:45:23 ERROR database connection failed"
Log 4: "2015-07-09 18:12:45 INFO user=bob action=login"
```

**Process:** Compare each log to existing clusters

```
📦 Cluster 1: LOGIN EVENTS
   ├─ Log 1: INFO user=john action=login
   ├─ Log 2: INFO user=alice action=login
   └─ Log 4: INFO user=bob action=login

📦 Cluster 2: DATABASE ERRORS
   └─ Log 3: ERROR database connection failed
```

**Key Concept: SIMILARITY**
- Logs 1, 2, 4 are similar (same structure, different usernames)
- Log 3 is different (completely different message)

### The "Edit Distance" Magic

**Question:** How similar are these?
```
Log A: "INFO user=john"
Log B: "INFO user=alice"
```

**Answer:** Count how many changes needed:
- "john" → "alice" = 1 change
- Everything else is identical
- Similarity = Very High! ✅

**Compare to:**
```
Log A: "INFO user=john"
Log C: "ERROR database failed"
```
- Need to change: INFO→ERROR, user→database, john→failed
- Similarity = Very Low! ❌

---

## 🔍 STEP 2: PATTERN EXTRACTION (Finding the Template)

### What is it?
For each cluster, identify what STAYS THE SAME vs what CHANGES.

### Example:

**Cluster Contents:**
```
1. "2015-07-09 10:22:12 INFO user=john action=login"
2. "2015-07-09 14:35:01 INFO user=alice action=login"
3. "2015-07-09 18:12:45 INFO user=bob action=login"
```

**Analysis (token by token):**
```
Position 0: "2015-07-09"  "2015-07-09"  "2015-07-09"  → DIFFERENT dates → ***
Position 1: "10:22:12"    "14:35:01"    "18:12:45"    → DIFFERENT times → ***
Position 2: "INFO"        "INFO"        "INFO"        → SAME! → Keep "INFO"
Position 3: "user=john"   "user=alice"  "user=bob"    → DIFFERENT users → ***
Position 4: "action=login" "action=login" "action=login" → SAME! → Keep "action=login"
```

**Extracted Pattern:**
```
*** *** INFO user=*** action=login
```

**What this means:**
- `***` = Variable part (changes)
- `INFO`, `action=login` = Constant part (always there)

---

## 📊 STEP 3: RANKING (Sorting by Importance)

### What is it?
Patterns that appear MORE often are MORE important.

### Example:

```
Pattern A: "*** INFO user=*** action=login"     → Appears 1,000 times
Pattern B: "*** ERROR database connection"       → Appears 50 times  
Pattern C: "*** WARNING disk space low"          → Appears 5 times
```

**Sorted Output:**
```
1. "*** INFO user=*** action=login"     (support: 1000) ← Most common
2. "*** ERROR database connection"      (support: 50)
3. "*** WARNING disk space low"         (support: 5)
```

---

## 🎛️ CONFIGURATION: The Knobs You Can Turn

### 1. **Similarity Threshold** (0.0 to 1.0)

Controls how strict clustering is:

```
THRESHOLD = 0.9 (Very Strict)
├─ Only VERY similar logs cluster together
├─ Result: MANY clusters, MANY specific patterns
└─ Use when: You know your log format

THRESHOLD = 0.5 (Balanced) ← DEFAULT
├─ Reasonably similar logs cluster together
├─ Result: Moderate clusters, useful patterns
└─ Use when: Mixed log sources

THRESHOLD = 0.3 (Very Loose)
├─ Even somewhat different logs cluster together
├─ Result: FEW clusters, GENERAL patterns
└─ Use when: Very diverse, unknown sources
```

**Visual Example:**

```
With Threshold = 0.8 (Strict):
📦 Cluster 1: "INFO user login"
📦 Cluster 2: "INFO user logout"
📦 Cluster 3: "WARNING user login"
→ 3 separate patterns (very specific)

With Threshold = 0.4 (Loose):
📦 Cluster 1: "*** user ***"
→ 1 general pattern (covers all above)
```

### 2. **Minimum Cluster Size**

Filters out rare/noisy patterns:

```
MIN_SIZE = 1
├─ Keep patterns that appear even once
└─ Use for: Anomaly detection, finding rare events

MIN_SIZE = 3
├─ Only keep patterns appearing 3+ times
└─ Use for: Normal monitoring, filter noise

MIN_SIZE = 10
├─ Only keep frequent patterns
└─ Use for: High-traffic systems, reliable alerts
```

### 3. **Normalization** (Pre-processing)

Replace common variable parts BEFORE clustering:

```
BEFORE Normalization:
- "Request from 192.168.1.100"
- "Request from 192.168.1.101"
- "Request from 10.0.0.50"
→ These look DIFFERENT (different IPs)
→ Result: 3 separate patterns

AFTER IP Normalization:
- "Request from <IP>"
- "Request from <IP>"
- "Request from <IP>"
→ These look IDENTICAL
→ Result: 1 pattern! ✅
```

**Types of Normalization:**
- `normalizeTimestamps`: `2015-07-09 10:22:12` → `<TIMESTAMP>`
- `normalizeIPs`: `192.168.1.1` → `<IP>`
- `normalizeNumbers`: `1234` → `<NUM>`
- `normalizePaths`: `/usr/local/bin` → `<PATH>`
- `normalizeUrls`: `http://example.com` → `<URL>`

---

## 🌲 HIERARCHICAL PATTERNS

### What is it?
Extract patterns at MULTIPLE levels of detail simultaneously.

### Example:

**Same Logs, Different Thresholds:**

```
Level 1 (Threshold = 0.8) - VERY SPECIFIC:
1. "*** INFO [worker-1] Processing request from user ***"
2. "*** INFO [worker-2] Processing request from user ***"
3. "*** INFO [worker-3] Processing request from user ***"
→ 3 patterns (distinguishes worker threads)

Level 2 (Threshold = 0.5) - MODERATE:
1. "*** INFO [***] Processing request from user ***"
→ 1 pattern (worker thread becomes wildcard)

Level 3 (Threshold = 0.3) - GENERAL:
1. "*** INFO [***] *** *** *** user ***"
→ 1 very general pattern
```

**When to use each level:**
- **Level 1 (Specific)**: Debugging, finding exact issues
- **Level 2 (Moderate)**: Dashboards, monitoring
- **Level 3 (General)**: High-level overview, reporting

---

## 💡 PRACTICAL USE CASES

### Use Case 1: Unknown Log Sources

**Problem:** You receive logs from 10 different systems, don't know their formats.

**Solution:**
```java
// Use multi-source config with lenient threshold
LogMineProcessor processor = new LogMineProcessor(0.5, 2);
List<LogPattern> patterns = processor.process(allLogs);

// LogMine automatically discovers ALL formats!
```

### Use Case 2: Anomaly Detection

**Problem:** Find unusual/suspicious log messages.

**Solution:**
```java
// Train on normal logs
LogMineProcessor processor = new LogMineProcessor(0.6, 3);
processor.process(normalLogs);

// Test new logs
for (String log : newLogs) {
    if (processor.matchPattern(log) == null) {
        alert("ANOMALY: " + log);
    }
}
```

### Use Case 3: Log Compression

**Problem:** Store 1 million logs efficiently.

**Solution:**
```java
// Extract patterns
processor.process(millionLogs);

// Store just patterns + counts
// "*** INFO user=*** action=login" → 500,000 occurrences
// "*** ERROR database timeout" → 1,000 occurrences

// Saved 99% space! ✅
```

---

## 🚀 QUICK START

### For Unknown/Mixed Sources:

```java
// Just set threshold and min size
LogMineProcessor processor = new LogMineProcessor(0.5, 2);
List<LogPattern> patterns = processor.process(yourLogs);

// That's it! LogMine figures out the rest.
```

### For Multiple Detail Levels:

```java
// Try 3 thresholds
double[] thresholds = {0.8, 0.5, 0.3};

for (double t : thresholds) {
    LogMineProcessor p = new LogMineProcessor(t, 2);
    List<LogPattern> patterns = p.process(logs);
    System.out.println("Threshold " + t + ": " + patterns.size() + " patterns");
}

// Pick the level that works best for you!
```

---

## 🎓 KEY TAKEAWAYS

1. **No Prior Knowledge Needed**
   - Don't need to know log formats
   - Don't need to write regexes
   - Just provide logs!

2. **Two Main Knobs**
   - Similarity Threshold (0.3-0.8)
   - Minimum Cluster Size (1-10)

3. **Optional Enhancements**
   - Normalization (helps clustering)
   - Hierarchical patterns (multiple detail levels)

4. **Start Simple, Tune Later**
   - Begin with defaults (0.5 threshold, size 2)
   - Too many patterns? Increase threshold
   - Too few patterns? Decrease threshold

---

## 📖 Further Reading

- Full paper: https://www.cs.unm.edu/~mueen/Papers/LogMine.pdf
- Configuration guide: See `CONFIGURATION.md`
- Multi-source demo: Run `MultiSourceDemo.java`

**Remember:** LogMine is designed to be UNSUPERVISED and CONFIGURABLE. You don't need to understand every detail—just tune 1-2 parameters and it works! 🎉

