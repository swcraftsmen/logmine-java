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

/**
 * Strategy interface for detecting if a token should be considered variable. This allows
 * customization of what gets replaced with wildcards in patterns.
 */
public interface VariableDetector {

  /**
   * Determines if a token should be considered a variable part. Variable parts will be replaced
   * with wildcards in patterns.
   *
   * @param token The token to check
   * @return true if the token should be treated as variable
   */
  boolean isVariable(String token);

  /**
   * Determines if two tokens should be considered matching/equivalent. This is used during
   * similarity calculation.
   *
   * @param token1 First token
   * @param token2 Second token
   * @return true if tokens should be considered matching
   */
  boolean tokensMatch(String token1, String token2);

  /**
   * Returns a description of this variable detection strategy.
   *
   * @return A human-readable description
   */
  String getDescription();
}
