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
 * Tokenizer for JSON-formatted logs. Extracts keys and values separately.
 *
 * <p>Example: {"level":"INFO","message":"User logged in","user":"tom"} Tokens: ["{", "level", ":",
 * "INFO", ",", "message", ":", "User logged in", ",", "user", ":", "tom", "}"]
 *
 * <p>Note: This is a simplified JSON tokenizer. For production use, consider using a proper JSON
 * parser.
 */
public class JsonTokenizer implements TokenizerStrategy {

  /** Creates a new JsonTokenizer. */
  public JsonTokenizer() {
    // Default constructor
  }

  @Override
  public List<String> tokenize(String message) {
    List<String> tokens = new ArrayList<>();

    if (message == null || message.trim().isEmpty()) {
      return tokens;
    }

    // Remove outer braces if present
    String trimmed = message.trim();
    if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
      tokens.add("{");
      trimmed = trimmed.substring(1, trimmed.length() - 1);

      // Simple parsing: split on commas (but not in quoted strings)
      List<String> pairs = splitRespectingQuotes(trimmed, ',');

      for (String pair : pairs) {
        if (!pair.trim().isEmpty()) {
          // Split key:value
          List<String> keyValue = splitRespectingQuotes(pair, ':');
          if (keyValue.size() >= 2) {
            String key = keyValue.get(0).trim().replace("\"", "");
            String value = keyValue.get(1).trim().replace("\"", "");

            tokens.add(key);
            tokens.add(":");
            tokens.add(value);
            tokens.add(",");
          }
        }
      }

      // Remove trailing comma
      if (!tokens.isEmpty() && tokens.get(tokens.size() - 1).equals(",")) {
        tokens.remove(tokens.size() - 1);
      }

      tokens.add("}");
    } else {
      // Not a JSON object, fall back to whitespace tokenization
      for (String token : message.split("\\s+")) {
        if (!token.isEmpty()) {
          tokens.add(token);
        }
      }
    }

    return tokens;
  }

  private List<String> splitRespectingQuotes(String str, char delimiter) {
    List<String> result = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean inQuotes = false;

    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);

      if (c == '"' && (i == 0 || str.charAt(i - 1) != '\\')) {
        inQuotes = !inQuotes;
        current.append(c);
      } else if (c == delimiter && !inQuotes) {
        result.add(current.toString());
        current = new StringBuilder();
      } else {
        current.append(c);
      }
    }

    if (current.length() > 0) {
      result.add(current.toString());
    }

    return result;
  }

  @Override
  public String getDescription() {
    return "JSON Tokenizer - Extracts keys and values from JSON logs";
  }
}
