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

package org.swengdev.logmine.strategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Tokenizer that splits on delimiters but preserves them. Good for structured logs with key=value
 * pairs.
 *
 * <p>Example: "action=insert user=tom id=123" Tokens: ["action", "=", "insert", "user", "=", "tom",
 * "id", "=", "123"]
 */
public class DelimiterPreservingTokenizer implements TokenizerStrategy {

  private final String delimiters;

  /** Creates a tokenizer with default delimiters. */
  public DelimiterPreservingTokenizer() {
    this("=,:;[]{}()");
  }

  /**
   * Creates a tokenizer with custom delimiters.
   *
   * @param delimiters String containing all delimiter characters
   */
  public DelimiterPreservingTokenizer(String delimiters) {
    this.delimiters = delimiters;
  }

  @Override
  public List<String> tokenize(String message) {
    List<String> tokens = new ArrayList<>();

    if (message == null || message.isEmpty()) {
      return tokens;
    }

    // Build regex pattern that splits on whitespace and delimiters while preserving them
    String delimiterPattern = buildDelimiterPattern();
    String[] parts = message.split("(?<=\\s)|(?=\\s)|" + delimiterPattern);

    for (String part : parts) {
      if (!part.isEmpty() && !part.matches("\\s+")) {
        tokens.add(part);
      }
    }

    return tokens;
  }

  private String buildDelimiterPattern() {
    StringBuilder pattern = new StringBuilder();
    for (char delimiter : delimiters.toCharArray()) {
      String escaped = escapeRegex(delimiter);
      pattern.append("(?<=").append(escaped).append(")|(?=").append(escaped).append(")|");
    }
    // Remove trailing |
    if (pattern.length() > 0) {
      pattern.setLength(pattern.length() - 1);
    }
    return pattern.toString();
  }

  private String escapeRegex(char c) {
    String special = ".^$*+?()[]{}|\\";
    if (special.indexOf(c) >= 0) {
      return "\\" + c;
    }
    return String.valueOf(c);
  }

  @Override
  public String getDescription() {
    return "Delimiter-Preserving Tokenizer - Splits on delimiters: " + delimiters;
  }
}
