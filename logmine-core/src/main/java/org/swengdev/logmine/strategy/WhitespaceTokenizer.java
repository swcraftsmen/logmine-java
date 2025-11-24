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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Simple tokenizer that splits on whitespace. Best for simple log formats like syslog.
 *
 * <p>Example: "2015-07-09 10:22:12 INFO User logged in" Tokens: ["2015-07-09", "10:22:12", "INFO",
 * "User", "logged", "in"]
 */
public class WhitespaceTokenizer implements TokenizerStrategy {

  /** Creates a new WhitespaceTokenizer. */
  public WhitespaceTokenizer() {
    // Default constructor
  }

  @Override
  public List<String> tokenize(String message) {
    if (message == null || message.trim().isEmpty()) {
      return new ArrayList<>();
    }

    return Arrays.stream(message.split("\\s+"))
        .filter(token -> !token.isEmpty())
        .collect(Collectors.toList());
  }

  @Override
  public String getDescription() {
    return "Whitespace Tokenizer - Splits on whitespace characters";
  }
}
