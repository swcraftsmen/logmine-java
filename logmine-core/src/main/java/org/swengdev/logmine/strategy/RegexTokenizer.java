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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tokenizer that uses a custom regex pattern to extract tokens. Most flexible option for complex
 * log formats.
 *
 * <p>Example with pattern "\\w+|\\S+": "2015-07-09 10:22:12,235 INFO" Tokens: ["2015", "07", "09",
 * "10", "22", "12", "235", "INFO"]
 */
public class RegexTokenizer implements TokenizerStrategy {

  private final Pattern pattern;
  private final String patternString;

  /**
   * Creates a tokenizer that extracts tokens matching the given pattern.
   *
   * @param regex Regular expression pattern to match tokens
   */
  public RegexTokenizer(String regex) {
    this.patternString = regex;
    this.pattern = Pattern.compile(regex);
  }

  /** Creates a tokenizer with a default pattern that matches words and non-whitespace. */
  public RegexTokenizer() {
    this("\\S+"); // Match any non-whitespace sequence
  }

  @Override
  public List<String> tokenize(String message) {
    List<String> tokens = new ArrayList<>();

    if (message == null || message.isEmpty()) {
      return tokens;
    }

    Matcher matcher = pattern.matcher(message);
    while (matcher.find()) {
      tokens.add(matcher.group());
    }

    return tokens;
  }

  @Override
  public String getDescription() {
    return "Regex Tokenizer - Pattern: " + patternString;
  }
}
