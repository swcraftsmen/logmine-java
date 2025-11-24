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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Generates deterministic, content-based identifiers for log patterns.
 *
 * <p>This ensures the same pattern discovered by different nodes gets the same identifier,
 * preventing duplicates in distributed systems.
 *
 * <p>Thread-safe and stateless.
 *
 * <p><b>Internal API:</b> This class is package-private. Pattern IDs are automatically generated
 * and managed by {@link LogPattern}. Users don't need to interact with this class directly.
 */
class PatternIdentifier {

  /** Private constructor to prevent instantiation of utility class. */
  private PatternIdentifier() {
    throw new UnsupportedOperationException("Utility class");
  }

  // Thread-local MessageDigest for lock-free hashing
  private static final ThreadLocal<MessageDigest> SHA256 =
      ThreadLocal.withInitial(
          () -> {
            try {
              return MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
              throw new RuntimeException("SHA-256 not available", e);
            }
          });

  // Pre-compiled pattern for wildcard detection (performance optimization)
  private static final Pattern WILDCARD_PATTERN = Pattern.compile("<[^>]+>");

  /**
   * Generates a unique identifier for a pattern based on its content. Same pattern = same ID,
   * regardless of which node discovered it.
   *
   * @param pattern The log pattern
   * @return Deterministic identifier (base64-encoded SHA-256 hash)
   * @throws NullPointerException if pattern is null
   */
  public static String generateId(LogPattern pattern) {
    Objects.requireNonNull(pattern, "LogPattern cannot be null");
    return generateIdFromTokens(pattern.getTokens());
  }

  /**
   * Generates a unique identifier from pattern tokens.
   *
   * @param tokens Pattern tokens (e.g., ["GET", "*", "HTTP/1.1", "*"])
   * @return Deterministic identifier
   * @throws NullPointerException if tokens is null
   */
  public static String generateIdFromTokens(List<String> tokens) {
    Objects.requireNonNull(tokens, "Tokens cannot be null");

    // Normalize the pattern representation
    String normalized = normalizePattern(tokens);

    // Generate SHA-256 hash
    byte[] hash = sha256(normalized);

    // Encode as URL-safe base64 (suitable for database keys)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
  }

  /**
   * Generates a shorter, more readable identifier (first 16 chars of hash). Use for display
   * purposes, but full hash for uniqueness guarantees.
   *
   * @param pattern The log pattern
   * @return Short identifier (e.g., "a3f7k9m2p1q8")
   * @throws NullPointerException if pattern is null
   */
  public static String generateShortId(LogPattern pattern) {
    Objects.requireNonNull(pattern, "LogPattern cannot be null");
    String fullId = generateId(pattern);
    return fullId.substring(0, Math.min(16, fullId.length()));
  }

  /**
   * Generates a human-readable signature for a pattern. Same as pattern.toString() but normalized
   * for consistency.
   *
   * @param pattern The log pattern
   * @return Pattern signature (e.g., "GET * HTTP/1.1 *")
   * @throws NullPointerException if pattern is null
   */
  public static String generateSignature(LogPattern pattern) {
    Objects.requireNonNull(pattern, "LogPattern cannot be null");
    return String.join(" ", pattern.getTokens());
  }

  /**
   * Normalizes pattern tokens to a canonical string representation. Ensures consistent hashing
   * across nodes.
   */
  private static String normalizePattern(List<String> tokens) {
    // Join with null byte separator (won't appear in log tokens)
    // This prevents collision between ["a b", "c"] and ["a", "b c"]
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < tokens.size(); i++) {
      if (i > 0) {
        sb.append('\0');
      }
      // Normalize wildcards to consistent representation
      String token = tokens.get(i);
      if (isWildcard(token)) {
        sb.append("*");
      } else {
        sb.append(token);
      }
    }
    return sb.toString();
  }

  /**
   * Checks if a token represents a wildcard/variable. Optimized to avoid regex compilation on every
   * call.
   */
  private static boolean isWildcard(String token) {
    // Fast path: check common wildcards first
    if ("*".equals(token) || "<*>".equals(token)) {
      return true;
    }
    // Slow path: check with pre-compiled pattern
    return WILDCARD_PATTERN.matcher(token).matches();
  }

  /** Computes SHA-256 hash of a string. Thread-safe without synchronization using ThreadLocal. */
  private static byte[] sha256(String input) {
    MessageDigest digest = SHA256.get();
    digest.reset();
    return digest.digest(input.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Generates a composite key for storing patterns with metadata. Includes pattern ID and optional
   * attributes like source, environment.
   *
   * @param pattern The log pattern
   * @param source Source identifier (e.g., "app-1", "service-auth")
   * @param environment Environment (e.g., "prod", "staging")
   * @return Composite key combining pattern ID, signature, source, and environment
   */
  public static CompositeKey generateCompositeKey(
      LogPattern pattern, String source, String environment) {
    return new CompositeKey(generateId(pattern), generateSignature(pattern), source, environment);
  }

  /**
   * Composite key for pattern storage with metadata.
   *
   * @param patternId Unique identifier for the pattern
   * @param signature Human-readable pattern signature
   * @param source Source identifier (e.g., "app-1", "service-auth")
   * @param environment Environment (e.g., "prod", "staging")
   */
  public record CompositeKey(
      String patternId, String signature, String source, String environment) {

    /**
     * Generates a unique storage key combining all attributes. Format: patternId:source:environment
     *
     * @return Storage key string
     */
    public String toStorageKey() {
      return String.format("%s:%s:%s", patternId, source, environment);
    }

    /**
     * Generates a global key (ignoring source/environment). Use for global pattern aggregation
     * across all sources.
     *
     * @return Global key (just the pattern ID)
     */
    public String toGlobalKey() {
      return patternId;
    }

    @Override
    public String toString() {
      return String.format(
          "CompositeKey{id=%s, sig=%s, src=%s, env=%s}", patternId, signature, source, environment);
    }
  }
}
