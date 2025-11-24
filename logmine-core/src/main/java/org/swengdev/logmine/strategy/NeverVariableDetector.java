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
 * A variable detector that treats everything as a constant. Useful when you want exact pattern
 * matching without wildcards.
 */
public class NeverVariableDetector implements VariableDetector {

  /** Creates a new NeverVariableDetector. */
  public NeverVariableDetector() {
    // Default constructor
  }

  @Override
  public boolean isVariable(String token) {
    return false; // Nothing is variable
  }

  @Override
  public boolean tokensMatch(String token1, String token2) {
    return token1.equals(token2); // Only exact matches
  }

  @Override
  public String getDescription() {
    return "Never Variable Detector - All tokens treated as constants";
  }
}
