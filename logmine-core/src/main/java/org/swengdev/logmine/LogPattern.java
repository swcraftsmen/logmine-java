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
import org.swengdev.logmine.strategy.VariableDetector;

/**
 * Represents a log pattern extracted from a cluster of similar log messages. Patterns use wildcards
 * (***) to represent variable parts.
 */
public class LogPattern {
  private final List<String> patternTokens;
  private final int supportCount;
  private final String patternString;
  private final VariableDetector variableDetector;

  /**
   * Creates a new log pattern.
   *
   * @param patternTokens The tokens that make up the pattern (including wildcards)
   * @param supportCount The number of log messages that match this pattern
   * @param variableDetector Strategy for detecting variable parts in tokens
   */
  public LogPattern(
      List<String> patternTokens, int supportCount, VariableDetector variableDetector) {
    this.patternTokens = new ArrayList<>(patternTokens);
    this.supportCount = supportCount;
    this.patternString = String.join("", patternTokens);
    this.variableDetector = variableDetector;
  }

  /**
   * Creates a pattern from a list of log messages by identifying constant and variable parts.
   *
   * @param messages The log messages to create a pattern from
   * @param variableDetector Strategy for detecting variable parts in tokens
   * @return A new LogPattern representing the messages
   */
  public static LogPattern createFromMessages(
      List<LogMessage> messages, VariableDetector variableDetector) {
    if (messages.isEmpty()) {
      return new LogPattern(new ArrayList<>(), 0, variableDetector);
    }

    if (messages.size() == 1) {
      // For single message, still apply variable detection
      List<String> tokens = messages.getFirst().getTokens();
      List<String> patternTokens = new ArrayList<>();
      for (String token : tokens) {
        // Even single messages can have variables detected (e.g., timestamps, IDs)
        if (variableDetector.isVariable(token)) {
          patternTokens.add("***");
        } else {
          patternTokens.add(token);
        }
      }
      return new LogPattern(patternTokens, 1, variableDetector);
    }

    // Use the first message as a template
    List<String> patternTokens = new ArrayList<>(messages.getFirst().getTokens());

    // For each token position, check if it's constant across all messages
    for (int i = 0; i < patternTokens.size(); i++) {
      boolean isConstant = true;
      String firstToken = patternTokens.get(i);

      // Check if this token is inherently variable (e.g., timestamp, number)
      if (variableDetector.isVariable(firstToken)) {
        isConstant = false;
      } else {
        // Check if it varies across messages
        for (int j = 1; j < messages.size(); j++) {
          List<String> tokens = messages.get(j).getTokens();

          // Handle messages with different lengths
          if (i >= tokens.size() || !firstToken.equals(tokens.get(i))) {
            isConstant = false;
            break;
          }
        }
      }

      // If this position is variable, replace with wildcard
      if (!isConstant) {
        patternTokens.set(i, "***");
      }
    }

    return new LogPattern(patternTokens, messages.size(), variableDetector);
  }

  /**
   * Checks if a log message matches this pattern.
   *
   * @param message The log message to check
   * @return true if the message matches this pattern, false otherwise
   */
  public boolean matches(LogMessage message) {
    List<String> tokens = message.getTokens();

    if (tokens.size() != patternTokens.size()) {
      return false;
    }

    for (int i = 0; i < patternTokens.size(); i++) {
      String patternToken = patternTokens.get(i);
      String messageToken = tokens.get(i);

      // Wildcard matches anything
      if (patternToken.equals("***")) {
        continue;
      }

      // Otherwise, tokens must match exactly
      if (!patternToken.equals(messageToken)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Returns the specificity of this pattern (ratio of constant tokens to total tokens). Higher
   * values mean more specific patterns.
   *
   * @return Specificity score (0.0-1.0, where 1.0 means all constant tokens)
   */
  public double getSpecificity() {
    if (patternTokens.isEmpty()) {
      return 0.0;
    }

    long constantTokens = patternTokens.stream().filter(token -> !token.equals("***")).count();

    return (double) constantTokens / patternTokens.size();
  }

  /**
   * Gets the tokens that make up this pattern.
   *
   * @return A defensive copy of the pattern tokens
   */
  public List<String> getPatternTokens() {
    return new ArrayList<>(patternTokens);
  }

  /**
   * Alias for getPatternTokens() for convenience.
   *
   * @return A defensive copy of the pattern tokens
   */
  public List<String> getTokens() {
    return getPatternTokens();
  }

  /**
   * Gets the number of log messages that support this pattern.
   *
   * @return The support count
   */
  public int getSupportCount() {
    return supportCount;
  }

  /**
   * Gets the pattern as a single string.
   *
   * @return The pattern string with tokens joined together
   */
  public String getPatternString() {
    return patternString;
  }

  /**
   * Gets the unique pattern identifier (content-based hash). Useful for deduplication in
   * distributed systems.
   *
   * @return Unique pattern identifier
   */
  public String getPatternId() {
    return PatternIdentifier.generateId(this);
  }

  /**
   * Gets a short version of the pattern ID (first 16 characters). Useful for display purposes.
   *
   * @return Short pattern identifier
   */
  public String getShortPatternId() {
    return PatternIdentifier.generateShortId(this);
  }

  /**
   * Gets a human-readable signature for this pattern.
   *
   * @return Pattern signature
   */
  public String getSignature() {
    return PatternIdentifier.generateSignature(this);
  }

  /**
   * Gets the variable detector used by this pattern.
   *
   * @return The variable detector
   */
  public VariableDetector getVariableDetector() {
    return variableDetector;
  }

  @Override
  public String toString() {
    return patternString + " (support: " + supportCount + ")";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    LogPattern other = (LogPattern) obj;
    return patternString.equals(other.patternString);
  }

  @Override
  public int hashCode() {
    return patternString.hashCode();
  }
}
