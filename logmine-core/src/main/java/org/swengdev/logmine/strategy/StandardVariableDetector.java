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

import java.util.regex.Pattern;

/**
 * Standard variable detector that considers numbers, timestamps, IPs, UUIDs, etc. as variables.
 * This is the default detector suitable for most log formats.
 */
public class StandardVariableDetector implements VariableDetector {

  private static final Pattern NUMBER_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?$");
  private static final Pattern TIMESTAMP_PATTERN =
      Pattern.compile("^\\d{4}-\\d{2}-\\d{2}|\\d{2}:\\d{2}:\\d{2}|\\d+,\\d+$");
  private static final Pattern IP_PATTERN =
      Pattern.compile("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$");
  private static final Pattern UUID_PATTERN =
      Pattern.compile(
          "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
  private static final Pattern HEX_PATTERN = Pattern.compile("^0x[0-9a-fA-F]+$");
  private static final Pattern HASH_PATTERN = Pattern.compile("^[0-9a-fA-F]{32,}$");

  private final boolean detectNumbers;
  private final boolean detectTimestamps;
  private final boolean detectIPs;
  private final boolean detectUUIDs;
  private final boolean detectHashes;

  /** Creates a detector with all detection enabled. */
  public StandardVariableDetector() {
    this(true, true, true, true, true);
  }

  /**
   * Creates a detector with custom detection settings.
   *
   * @param detectNumbers Whether to detect numbers as variables
   * @param detectTimestamps Whether to detect timestamps as variables
   * @param detectIPs Whether to detect IP addresses as variables
   * @param detectUUIDs Whether to detect UUIDs as variables
   * @param detectHashes Whether to detect hashes as variables
   */
  public StandardVariableDetector(
      boolean detectNumbers,
      boolean detectTimestamps,
      boolean detectIPs,
      boolean detectUUIDs,
      boolean detectHashes) {
    this.detectNumbers = detectNumbers;
    this.detectTimestamps = detectTimestamps;
    this.detectIPs = detectIPs;
    this.detectUUIDs = detectUUIDs;
    this.detectHashes = detectHashes;
  }

  @Override
  public boolean isVariable(String token) {
    if (token == null || token.isEmpty()) {
      return false;
    }

    if (detectNumbers && NUMBER_PATTERN.matcher(token).matches()) {
      return true;
    }

    if (detectTimestamps && TIMESTAMP_PATTERN.matcher(token).matches()) {
      return true;
    }

    if (detectIPs && IP_PATTERN.matcher(token).matches()) {
      return true;
    }

    if (detectUUIDs && UUID_PATTERN.matcher(token).matches()) {
      return true;
    }

    if (detectHashes
        && (HEX_PATTERN.matcher(token).matches() || HASH_PATTERN.matcher(token).matches())) {
      return true;
    }

    return false;
  }

  @Override
  public boolean tokensMatch(String token1, String token2) {
    if (token1.equals(token2)) {
      return true;
    }

    // If both are variables of the same type, consider them matching
    if (isVariable(token1) && isVariable(token2)) {
      // Check if they're the same type of variable
      if (detectNumbers
          && NUMBER_PATTERN.matcher(token1).matches()
          && NUMBER_PATTERN.matcher(token2).matches()) {
        return true;
      }
      if (detectTimestamps
          && TIMESTAMP_PATTERN.matcher(token1).matches()
          && TIMESTAMP_PATTERN.matcher(token2).matches()) {
        return true;
      }
      if (detectIPs
          && IP_PATTERN.matcher(token1).matches()
          && IP_PATTERN.matcher(token2).matches()) {
        return true;
      }
      if (detectUUIDs
          && UUID_PATTERN.matcher(token1).matches()
          && UUID_PATTERN.matcher(token2).matches()) {
        return true;
      }
    }

    return false;
  }

  @Override
  public String getDescription() {
    return "Standard Variable Detector - Detects numbers, timestamps, IPs, UUIDs, and hashes";
  }
}
