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
 * Represents a single log message with its tokens and metadata.
 *
 * <p><b>Internal API:</b> This class is package-private and not intended for direct use by library
 * users. Users should work with {@link LogPattern} objects returned by {@link LogMineProcessor} or
 * {@link LogMine}.
 */
class LogMessage {
  private final String rawMessage;
  private final String processedMessage;
  private final List<String> tokens;
  private final int length;
  private final VariableDetector variableDetector;

  /**
   * Creates a log message without preprocessing.
   *
   * @param rawMessage The original log message text
   * @param tokens The tokenized representation of the message
   * @param variableDetector Strategy for detecting variable parts in tokens
   */
  public LogMessage(String rawMessage, List<String> tokens, VariableDetector variableDetector) {
    this.rawMessage = rawMessage;
    this.processedMessage = rawMessage; // No preprocessing in this constructor
    this.tokens = new ArrayList<>(tokens);
    this.length = tokens.size();
    this.variableDetector = variableDetector;
  }

  /**
   * Creates a log message with preprocessing applied.
   *
   * @param rawMessage The original log message text
   * @param processedMessage The preprocessed version of the message
   * @param tokens The tokenized representation of the message
   * @param variableDetector Strategy for detecting variable parts in tokens
   */
  public LogMessage(
      String rawMessage,
      String processedMessage,
      List<String> tokens,
      VariableDetector variableDetector) {
    this.rawMessage = rawMessage;
    this.processedMessage = processedMessage;
    this.tokens = new ArrayList<>(tokens);
    this.length = tokens.size();
    this.variableDetector = variableDetector;
  }

  /**
   * Calculates the edit distance between this log message and another. Uses a simplified version
   * based on token-level comparison.
   *
   * @param other The other log message to compare with
   * @return The edit distance (number of token-level edits needed)
   */
  public int editDistance(LogMessage other) {
    int m = this.tokens.size();
    int n = other.tokens.size();

    // Create DP table
    int[][] dp = new int[m + 1][n + 1];

    // Initialize base cases
    for (int i = 0; i <= m; i++) {
      dp[i][0] = i;
    }
    for (int j = 0; j <= n; j++) {
      dp[0][j] = j;
    }

    // Fill DP table
    for (int i = 1; i <= m; i++) {
      for (int j = 1; j <= n; j++) {
        if (variableDetector.tokensMatch(this.tokens.get(i - 1), other.tokens.get(j - 1))) {
          dp[i][j] = dp[i - 1][j - 1];
        } else {
          dp[i][j] =
              1
                  + Math.min(
                      dp[i - 1][j], // deletion
                      Math.min(
                          dp[i][j - 1], // insertion
                          dp[i - 1][j - 1] // substitution
                          ));
        }
      }
    }

    return dp[m][n];
  }

  /**
   * Calculates similarity score with another log message. Returns a value between 0 and 1, where 1
   * means identical.
   *
   * @param other The other log message to compare with
   * @return Similarity score (0.0 to 1.0, where 1.0 means identical)
   */
  public double similarity(LogMessage other) {
    int maxLen = Math.max(this.length, other.length);
    if (maxLen == 0) {
      return 1.0;
    }

    int distance = editDistance(other);
    return 1.0 - ((double) distance / maxLen);
  }

  /**
   * Gets the original, unprocessed log message text.
   *
   * @return The raw log message
   */
  public String getRawMessage() {
    return rawMessage;
  }

  /**
   * Gets the preprocessed log message text.
   *
   * @return The processed log message
   */
  public String getProcessedMessage() {
    return processedMessage;
  }

  /**
   * Gets the tokens that make up this log message.
   *
   * @return A defensive copy of the token list
   */
  public List<String> getTokens() {
    return new ArrayList<>(tokens);
  }

  /**
   * Gets the number of tokens in this log message.
   *
   * @return The token count
   */
  public int getLength() {
    return length;
  }

  @Override
  public String toString() {
    return rawMessage;
  }
}
