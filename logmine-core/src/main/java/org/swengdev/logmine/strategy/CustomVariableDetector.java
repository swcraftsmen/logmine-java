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

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Customizable variable detector that uses user-defined patterns and constants. Allows fine-grained
 * control over what should be treated as variable.
 */
public class CustomVariableDetector implements VariableDetector {

  private final Set<Pattern> variablePatterns;
  private final Set<String> constantTokens;
  private final boolean defaultToVariable;

  /**
   * Creates a custom detector.
   *
   * @param variablePatterns Patterns that identify variable tokens
   * @param constantTokens Tokens that should always be treated as constants
   * @param defaultToVariable If true, unknown tokens are treated as variables; if false, as
   *     constants
   */
  public CustomVariableDetector(
      Set<Pattern> variablePatterns, Set<String> constantTokens, boolean defaultToVariable) {
    this.variablePatterns = new HashSet<>(variablePatterns);
    this.constantTokens = new HashSet<>(constantTokens);
    this.defaultToVariable = defaultToVariable;
  }

  /**
   * Builder for creating custom detectors.
   *
   * <p>Provides a fluent API for configuring custom variable detection rules.
   */
  public static class Builder {
    private Set<Pattern> variablePatterns = new HashSet<>();
    private Set<String> constantTokens = new HashSet<>();
    private boolean defaultToVariable = false;

    /** Creates a new Builder with default settings. */
    public Builder() {
      // Default constructor
    }

    /**
     * Adds a regex pattern that identifies variable tokens.
     *
     * @param regex Regular expression pattern
     * @return this Builder instance
     */
    public Builder addVariablePattern(String regex) {
      variablePatterns.add(Pattern.compile(regex));
      return this;
    }

    /**
     * Adds a compiled Pattern that identifies variable tokens.
     *
     * @param pattern Compiled regex Pattern
     * @return this Builder instance
     */
    public Builder addVariablePattern(Pattern pattern) {
      variablePatterns.add(pattern);
      return this;
    }

    /**
     * Adds a token that should always be treated as constant.
     *
     * @param token The constant token
     * @return this Builder instance
     */
    public Builder addConstantToken(String token) {
      constantTokens.add(token);
      return this;
    }

    /**
     * Sets the default behavior for unmatched tokens.
     *
     * @param defaultToVariable If true, treat unknown tokens as variables
     * @return this Builder instance
     */
    public Builder setDefaultToVariable(boolean defaultToVariable) {
      this.defaultToVariable = defaultToVariable;
      return this;
    }

    /**
     * Builds the CustomVariableDetector with the configured rules.
     *
     * @return A new CustomVariableDetector instance
     */
    public CustomVariableDetector build() {
      return new CustomVariableDetector(variablePatterns, constantTokens, defaultToVariable);
    }
  }

  @Override
  public boolean isVariable(String token) {
    if (token == null || token.isEmpty()) {
      return false;
    }

    // Check if it's explicitly a constant
    if (constantTokens.contains(token)) {
      return false;
    }

    // Check if it matches any variable pattern
    for (Pattern pattern : variablePatterns) {
      if (pattern.matcher(token).matches()) {
        return true;
      }
    }

    // Use default behavior for unknown tokens
    return defaultToVariable;
  }

  @Override
  public boolean tokensMatch(String token1, String token2) {
    if (token1.equals(token2)) {
      return true;
    }

    // If both are variables, they match
    if (isVariable(token1) && isVariable(token2)) {
      return true;
    }

    return false;
  }

  @Override
  public String getDescription() {
    return "Custom Variable Detector - "
        + variablePatterns.size()
        + " patterns, "
        + constantTokens.size()
        + " constants";
  }
}
