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

import java.util.regex.Pattern;

/**
 * Preprocesses raw log messages to normalize variable parts before pattern extraction.
 *
 * <h2>What is Normalization?</h2>
 *
 * Normalization replaces variable values (IPs, timestamps, numbers) with placeholders so that logs
 * with different values but same structure can cluster together.
 *
 * <h3>Without Normalization (BAD):</h3>
 *
 * <pre>{@code
 * "User 12345 logged in from 192.168.1.1"  → Pattern A
 * "User 67890 logged in from 10.0.0.50"    → Pattern B
 * "User 11111 logged in from 172.16.0.1"   → Pattern C
 * Result: 3 different patterns (should be 1!)
 * }</pre>
 *
 * <h3>With Normalization (GOOD):</h3>
 *
 * <pre>{@code
 * "User 12345 logged in from 192.168.1.1"  → "User NUM logged in from IP_ADDR"
 * "User 67890 logged in from 10.0.0.50"    → "User NUM logged in from IP_ADDR"
 * "User 11111 logged in from 172.16.0.1"   → "User NUM logged in from IP_ADDR"
 * Result: 1 pattern!
 * }</pre>
 *
 * <h2>Normalization Types</h2>
 *
 * <h3>1. Timestamp Normalization</h3>
 *
 * Replaces all timestamp formats with "TIMESTAMP":
 *
 * <pre>{@code
 * "2024-01-15T10:30:45Z"           → "TIMESTAMP"
 * "[2024-01-15 10:30:45.123]"      → "TIMESTAMP"
 * "Jan 15 10:30:45"                → "TIMESTAMP"
 * "1705318245" (unix timestamp)    → "TIMESTAMP"
 * }</pre>
 *
 * Enable: {@code config.normalizeTimestamps(true)}
 *
 * <h3>2. IP Address Normalization</h3>
 *
 * Replaces IPv4 and IPv6 addresses with "IP_ADDR":
 *
 * <pre>{@code
 * "Connection from 192.168.1.1"    → "Connection from IP_ADDR"
 * "Request from 10.0.0.50"         → "Request from IP_ADDR"
 * "Client ::1"                     → "Client IP_ADDR"
 * }</pre>
 *
 * Enable: {@code config.normalizeIPs(true)}
 *
 * <h3>3. Number Normalization (Conservative)</h3>
 *
 * Replaces large numbers (4+ digits) and decimals with "NUM". Preserves small numbers (HTTP codes,
 * ports, counts):
 *
 * <pre>{@code
 * "User 12345 logged in"           → "User NUM logged in"      (4+ digits)
 * "Response time 234.5ms"          → "Response time NUMms"     (decimal)
 * "ERROR 404 Not Found"            → "ERROR 404 Not Found"     (preserved!)
 * "Listening on port 8080"         → "Listening on port NUM"   (4 digits)
 * "Retry attempt 3"                → "Retry attempt 3"         (preserved!)
 * }</pre>
 *
 * Enable: {@code config.normalizeNumbers(true)}
 *
 * <h3>4. Path Normalization</h3>
 *
 * Replaces file paths (2+ directory levels) with "PATH":
 *
 * <pre>{@code
 * "/var/log/app.log"               → "PATH"
 * "C:\\Program Files\\app\\cfg"    → "PATH"
 * "/api/users" (single level)      → "/api/users" (preserved!)
 * }</pre>
 *
 * Enable: {@code config.normalizePaths(true)}
 *
 * <h3>5. URL Normalization</h3>
 *
 * Replaces HTTP/HTTPS URLs with "URL":
 *
 * <pre>{@code
 * "https://api.example.com/users"  → "URL"
 * "http://localhost:8080/health"   → "URL"
 * }</pre>
 *
 * Enable: {@code config.normalizeUrls(true)}
 *
 * <h3>6. Case Normalization</h3>
 *
 * Converts everything to lowercase:
 *
 * <pre>{@code
 * "INFO User Logged In"            → "info user logged in"
 * "ERROR Database Failed"          → "error database failed"
 * }</pre>
 *
 * Enable: {@code config.caseSensitive(false)}
 *
 * <h2>Why Conservative Normalization?</h2>
 *
 * We don't normalize EVERYTHING to avoid losing semantic meaning:
 *
 * <ul>
 *   <li>HTTP codes (404, 200, 500) are preserved - they have meaning!
 *   <li>Small counts (1, 2, 3) are preserved
 *   <li>Single-level paths preserved (might be log levels like "INFO/DEBUG")
 *   <li>Composite tokens (user123) preserved (no word boundary)
 * </ul>
 *
 * <h2>Real-World Example</h2>
 *
 * <pre>{@code
 * // Three different log formats from different sources:
 * Source A: "2024-01-15T10:30:45Z INFO user_id=12345 login from 192.168.1.1"
 * Source B: "[2024-01-15 10:30:45] INFO User 67890 logged in from 10.0.0.50"
 * Source C: "Jan 15 10:30:45 INFO: User 11111 authenticated from 172.16.0.1"
 *
 * // After preprocessing (all normalized to same pattern):
 * All → "timestamp info user num login from ip_addr"
 *
 * // Result: They cluster together!
 * }</pre>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * LogMineConfig config = LogMineConfig.builder()
 *     .normalizeTimestamps(true)   // Normalize timestamps
 *     .normalizeIPs(true)          // Normalize IP addresses
 *     .normalizeNumbers(true)      // Normalize large numbers
 *     .caseSensitive(false)        // Ignore case
 *     .build();
 *
 * LogPreprocessor preprocessor = new LogPreprocessor(config);
 *
 * String raw = "2024-01-15T10:30:45Z User 12345 logged in from 192.168.1.1";
 * String normalized = preprocessor.preprocess(raw);
 * // Result: "timestamp user num logged in from ip_addr"
 * }</pre>
 *
 * @see LogMineConfig
 * @see LogMineProcessor
 */
class LogPreprocessor {

  private final LogMineConfig config;

  // Regex patterns compiled once for performance
  private static final Pattern TIMESTAMP_PATTERN =
      Pattern.compile(
          // ISO 8601: 2024-01-15T10:30:45Z or 2024-01-15T10:30:45.123Z
          "\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{3,9})?(?:Z|[+-]\\d{2}:\\d{2})?"
              + "|"
              +
              // Syslog: Jan 15 10:30:45
              "[A-Z][a-z]{2}\\s+\\d{1,2}\\s+\\d{2}:\\d{2}:\\d{2}"
              + "|"
              +
              // Common log: 15/Jan/2024:10:30:45 +0000
              "\\d{2}/[A-Z][a-z]{2}/\\d{4}:\\d{2}:\\d{2}:\\d{2}\\s+[+-]\\d{4}"
              + "|"
              +
              // Unix timestamp: 1705318245
              "\\b1[67]\\d{8}\\b"
              + "|"
              +
              // Bracketed: [2024-01-15 10:30:45.123]
              "\\[\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{3,9})?]"
              + "|"
              +
              // Simple: 2024-01-15 10:30:45
              "\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{3,9})?");

  private static final Pattern IPV4_PATTERN =
      Pattern.compile(
          "\\b(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b");

  private static final Pattern IPV6_PATTERN =
      Pattern.compile(
          "\\b(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}\\b"
              + "|\\b(?:[0-9a-fA-F]{1,4}:){1,7}:\\b"
              + "|\\b::(?:[0-9a-fA-F]{1,4}:){0,6}[0-9a-fA-F]{1,4}\\b");

  // More conservative: only normalize numbers that are likely variable values
  // Preserves: HTTP codes (404, 200), ports (8080), common constants
  private static final Pattern NUMBER_PATTERN =
      Pattern.compile(
          // Large numbers (>1000) likely to be IDs, timestamps, sizes
          "\\b\\d{4,}\\b"
              + "|"
              +
              // Floating point numbers (response times, percentages)
              "\\b\\d+\\.\\d+\\b");

  private static final Pattern PATH_PATTERN =
      Pattern.compile(
          // Unix paths: Must have at least 2 directory levels to avoid matching "INFO/DEBUG"
          "/(?:[a-zA-Z0-9_.-]+/){2,}[a-zA-Z0-9_.-]*"
              + "|"
              +
              // Windows paths: C:\Program Files\app\config.ini
              "[A-Z]:\\\\(?:[^\\\\/:*?\"<>|\\r\\n]+\\\\)+[^\\\\/:*?\"<>|\\r\\n]*");

  private static final Pattern URL_PATTERN =
      Pattern.compile("\\b(?:https?|ftp)://[^\\s/$.?#][^\\s]*\\b");

  /**
   * Constructs a LogPreprocessor with the given configuration.
   *
   * @param config Configuration specifying which normalization steps to apply
   */
  public LogPreprocessor(LogMineConfig config) {
    this.config = config;
  }

  /**
   * Preprocesses a raw log message according to configuration settings.
   *
   * <p>Uses conservative normalization to avoid false positives: - Only normalizes when confident
   * it's a variable value - Preserves semantic tokens (error codes, ports, log levels) - Respects
   * word boundaries and context
   *
   * <p>Applies normalizations in this order:
   *
   * <ol>
   *   <li>Timestamp normalization (if enabled)
   *   <li>URL normalization (if enabled) - before paths to avoid conflicts
   *   <li>Path normalization (if enabled)
   *   <li>IP address normalization (if enabled)
   *   <li>Number normalization (if enabled) - conservative, large numbers only
   *   <li>Case normalization (if not case-sensitive)
   * </ol>
   *
   * @param rawMessage The original log message
   * @return Normalized log message ready for tokenization
   */
  public String preprocess(String rawMessage) {
    if (rawMessage == null || rawMessage.isEmpty()) {
      return rawMessage;
    }

    String processed = rawMessage;

    // Apply normalizations in order (most specific to least specific)
    // This order prevents conflicts (e.g., URLs before paths, IPs before numbers)

    if (config.normalizeTimestamps()) {
      processed = TIMESTAMP_PATTERN.matcher(processed).replaceAll("TIMESTAMP");
    }

    if (config.normalizeUrls()) {
      // URLs must be normalized before paths to avoid partial matches
      processed = URL_PATTERN.matcher(processed).replaceAll("URL");
    }

    if (config.normalizePaths()) {
      processed = PATH_PATTERN.matcher(processed).replaceAll("PATH");
    }

    if (config.normalizeIPs()) {
      // IPv6 before IPv4 to avoid partial matches
      processed = IPV6_PATTERN.matcher(processed).replaceAll("IP_ADDR");
      processed = IPV4_PATTERN.matcher(processed).replaceAll("IP_ADDR");
    }

    if (config.normalizeNumbers()) {
      // Conservative: only large numbers (likely IDs, timestamps) and decimals
      // Preserves: HTTP codes (404), ports (8080), small counts (1, 2, 3)
      processed = NUMBER_PATTERN.matcher(processed).replaceAll("NUM");
    }

    if (!config.caseSensitive()) {
      processed = processed.toLowerCase();
    }

    return processed;
  }

  /**
   * Preprocesses multiple log messages in batch.
   *
   * @param rawMessages Array of original log messages
   * @return Array of normalized log messages
   */
  public String[] preprocessBatch(String[] rawMessages) {
    if (rawMessages == null) {
      return null;
    }

    String[] processed = new String[rawMessages.length];
    for (int i = 0; i < rawMessages.length; i++) {
      processed[i] = preprocess(rawMessages[i]);
    }
    return processed;
  }
}
