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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests for StandardVariableDetector. */
public class StandardVariableDetectorTest {

  private final VariableDetector detector = new StandardVariableDetector();

  @Test
  public void testNumbers() {
    assertTrue(detector.isVariable("123"));
    assertTrue(detector.isVariable("45.67"));
    assertTrue(detector.isVariable("0"));
  }

  @Test
  public void testTimestamps() {
    assertTrue(detector.isVariable("2024-01-15"));
    assertTrue(detector.isVariable("12:34:56"));
    // StandardVariableDetector doesn't match full ISO 8601 format
    // Only matches: date, time, or comma-separated numbers separately
    assertFalse(detector.isVariable("2024-01-15T12:34:56"));
    assertTrue(detector.isVariable("123,456")); // Matches \\d+,\\d+
  }

  @Test
  public void testIpAddresses() {
    assertTrue(detector.isVariable("192.168.1.1"));
    assertTrue(detector.isVariable("10.0.0.1"));
  }

  @Test
  public void testUuids() {
    assertTrue(detector.isVariable("550e8400-e29b-41d4-a716-446655440000"));
  }

  @Test
  public void testHexStrings() {
    assertTrue(detector.isVariable("0x1a2b3c"));
    assertTrue(detector.isVariable("0xDEADBEEF"));
  }

  @Test
  public void testPaths() {
    // StandardVariableDetector does NOT detect paths
    assertFalse(detector.isVariable("/api/users/123"));
    assertFalse(detector.isVariable("/var/log/app.log"));
  }

  @Test
  public void testEmails() {
    // StandardVariableDetector does NOT detect emails
    assertFalse(detector.isVariable("user@example.com"));
    assertFalse(detector.isVariable("test.user@domain.co.uk"));
  }

  @Test
  public void testUrls() {
    // StandardVariableDetector does NOT detect URLs
    assertFalse(detector.isVariable("http://example.com"));
    assertFalse(detector.isVariable("https://api.example.com/v1/users"));
  }

  @Test
  public void testConstants() {
    assertFalse(detector.isVariable("INFO"));
    assertFalse(detector.isVariable("ERROR"));
    assertFalse(detector.isVariable("User"));
    assertFalse(detector.isVariable("logged"));
  }

  @Test
  public void testEmptyString() {
    assertFalse(detector.isVariable(""));
  }

  @Test
  public void testNull() {
    assertFalse(detector.isVariable(null));
  }

  @Test
  public void testMixedContent() {
    // StandardVariableDetector requires FULL match, not partial
    assertFalse(detector.isVariable("user123")); // Not a pure number
    assertFalse(detector.isVariable("UserName")); // Just text
    assertTrue(detector.isVariable("123")); // Pure number
  }

  // ========== Constructor Tests ==========

  @Test
  public void testConstructorOnlyNumbers() {
    VariableDetector detector = new StandardVariableDetector(true, false, false, false, false);

    assertTrue(detector.isVariable("123"));
    assertTrue(detector.isVariable("45.67"));
    assertFalse(detector.isVariable("192.168.1.1")); // IP disabled
    assertFalse(detector.isVariable("2024-01-15")); // Timestamp disabled
    assertFalse(detector.isVariable("550e8400-e29b-41d4-a716-446655440000")); // UUID disabled
  }

  @Test
  public void testConstructorOnlyTimestamps() {
    VariableDetector detector = new StandardVariableDetector(false, true, false, false, false);

    assertTrue(detector.isVariable("2024-01-15"));
    assertTrue(detector.isVariable("12:34:56"));
    assertFalse(detector.isVariable("123")); // Number disabled
    assertFalse(detector.isVariable("192.168.1.1")); // IP disabled
  }

  @Test
  public void testConstructorOnlyIPs() {
    VariableDetector detector = new StandardVariableDetector(false, false, true, false, false);

    assertTrue(detector.isVariable("192.168.1.1"));
    assertTrue(detector.isVariable("10.0.0.1"));
    assertFalse(detector.isVariable("123")); // Number disabled
    assertFalse(detector.isVariable("2024-01-15")); // Timestamp disabled
  }

  @Test
  public void testConstructorOnlyUUIDs() {
    VariableDetector detector = new StandardVariableDetector(false, false, false, true, false);

    assertTrue(detector.isVariable("550e8400-e29b-41d4-a716-446655440000"));
    assertFalse(detector.isVariable("123")); // Number disabled
    assertFalse(detector.isVariable("192.168.1.1")); // IP disabled
  }

  @Test
  public void testConstructorOnlyHashes() {
    VariableDetector detector = new StandardVariableDetector(false, false, false, false, true);

    assertTrue(detector.isVariable("0x1a2b3c"));
    assertTrue(detector.isVariable("abcdef1234567890abcdef1234567890")); // 32-char hash
    assertFalse(detector.isVariable("123")); // Number disabled
    assertFalse(detector.isVariable("192.168.1.1")); // IP disabled
  }

  @Test
  public void testConstructorAllDisabled() {
    VariableDetector detector = new StandardVariableDetector(false, false, false, false, false);

    assertFalse(detector.isVariable("123"));
    assertFalse(detector.isVariable("192.168.1.1"));
    assertFalse(detector.isVariable("2024-01-15"));
    assertFalse(detector.isVariable("550e8400-e29b-41d4-a716-446655440000"));
    assertFalse(detector.isVariable("0xABCD"));
  }

  // ========== tokensMatch() Tests ==========

  @Test
  public void testTokensMatchIdentical() {
    assertTrue(detector.tokensMatch("INFO", "INFO"));
    assertTrue(detector.tokensMatch("logged", "logged"));
  }

  @Test
  public void testTokensMatchDifferentNumbers() {
    // Both are numbers, so they match
    assertTrue(detector.tokensMatch("123", "456"));
    assertTrue(detector.tokensMatch("1.5", "2.7"));
    assertTrue(detector.tokensMatch("0", "999"));
  }

  @Test
  public void testTokensMatchDifferentTimestamps() {
    // Both are timestamps, so they match
    assertTrue(detector.tokensMatch("2024-01-15", "2023-12-25"));
    assertTrue(detector.tokensMatch("10:30:00", "15:45:30"));
  }

  @Test
  public void testTokensMatchDifferentIPs() {
    // Both are IPs, so they match
    assertTrue(detector.tokensMatch("192.168.1.1", "10.0.0.1"));
    assertTrue(detector.tokensMatch("172.16.0.1", "8.8.8.8"));
  }

  @Test
  public void testTokensMatchDifferentUUIDs() {
    // Both are UUIDs, so they match
    assertTrue(
        detector.tokensMatch(
            "550e8400-e29b-41d4-a716-446655440000", "123e4567-e89b-12d3-a456-426614174000"));
  }

  @Test
  public void testTokensMatchDifferentTypes() {
    // Different variable types don't match
    assertFalse(detector.tokensMatch("123", "192.168.1.1")); // Number vs IP
    assertFalse(detector.tokensMatch("2024-01-15", "123")); // Timestamp vs Number
    assertFalse(detector.tokensMatch("192.168.1.1", "2024-01-15")); // IP vs Timestamp
  }

  @Test
  public void testTokensMatchConstantVsVariable() {
    assertFalse(detector.tokensMatch("INFO", "123")); // Constant vs Number
    assertFalse(detector.tokensMatch("192.168.1.1", "localhost")); // IP vs Constant
  }

  @Test
  public void testTokensMatchDifferentConstants() {
    assertFalse(detector.tokensMatch("INFO", "ERROR"));
    assertFalse(detector.tokensMatch("User", "Admin"));
  }

  @Test
  public void testTokensMatchWithDisabledDetection() {
    VariableDetector detector = new StandardVariableDetector(false, false, true, false, false);

    // IPs still match
    assertTrue(detector.tokensMatch("192.168.1.1", "10.0.0.1"));

    // Numbers don't match because detection is disabled
    assertFalse(detector.tokensMatch("123", "456"));
  }

  // ========== Edge Cases for Hash Detection ==========

  @Test
  public void testLongHashes() {
    // MD5 (32 chars)
    assertTrue(detector.isVariable("5d41402abc4b2a76b9719d911017c592"));
    // SHA-256 (64 chars)
    assertTrue(
        detector.isVariable("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"));
  }

  @Test
  public void testShortHexNotVariable() {
    // Less than 32 chars without 0x prefix should not be detected
    assertFalse(detector.isVariable("abc123def"));
    assertFalse(detector.isVariable("1234abcd"));
  }

  @Test
  public void testHexWithPrefix() {
    assertTrue(detector.isVariable("0xDEADBEEF"));
    assertTrue(detector.isVariable("0x1a2b3c4d5e6f"));
    assertFalse(detector.isVariable("0x")); // Just prefix
    assertFalse(detector.isVariable("0xGHIJ")); // Invalid hex chars
  }

  // ========== Additional Timestamp Tests ==========

  @Test
  public void testCommaSeparatedNumbers() {
    // Part of TIMESTAMP_PATTERN: \\d+,\\d+
    assertTrue(detector.isVariable("123,456"));
    assertTrue(detector.isVariable("1,2"));
  }

  @Test
  public void testPartialDates() {
    assertTrue(detector.isVariable("2024-01-15"));
    assertFalse(detector.isVariable("2024-1-5")); // Missing leading zeros
    assertFalse(detector.isVariable("24-01-15")); // 2-digit year
  }

  @Test
  public void testPartialTimes() {
    assertTrue(detector.isVariable("12:34:56"));
    assertFalse(detector.isVariable("1:2:3")); // Missing leading zeros
    assertFalse(detector.isVariable("12:34")); // Only hour:minute
  }

  // ========== Negative Number Tests ==========

  @Test
  public void testNegativeNumbers() {
    assertTrue(detector.isVariable("-123"));
    assertTrue(detector.isVariable("-45.67"));
    assertFalse(detector.isVariable("--123")); // Double negative
  }

  // ========== IP Address Edge Cases ==========

  @Test
  public void testInvalidIPAddresses() {
    // Note: StandardVariableDetector uses simple regex, not strict validation
    // It matches pattern \d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3} without range checking
    assertTrue(detector.isVariable("256.1.1.1")); // Matches pattern (no range validation)
    assertFalse(detector.isVariable("192.168.1")); // Too few octets
    assertFalse(detector.isVariable("192.168.1.1.1")); // Too many octets
    assertFalse(detector.isVariable("::1")); // IPv6
  }

  @Test
  public void testValidIPAddressVariants() {
    assertTrue(detector.isVariable("0.0.0.0"));
    assertTrue(detector.isVariable("255.255.255.255"));
    assertTrue(detector.isVariable("127.0.0.1"));
  }

  // ========== UUID Edge Cases ==========

  @Test
  public void testInvalidUUIDs() {
    assertFalse(detector.isVariable("550e8400-e29b-41d4-a716")); // Too short
    // Note: This actually matches because the pattern only checks format, not extra content
    assertTrue(detector.isVariable("550e8400-e29b-41d4-a716-446655440000")); // Valid UUID format
    // Note: UUID without dashes (32 hex chars) matches as a HASH instead
    assertTrue(detector.isVariable("550e8400e29b41d4a716446655440000")); // Detected as hash
    assertFalse(detector.isVariable("ZZZZZZZZ-e29b-41d4-a716-446655440000")); // Invalid hex
  }

  @Test
  public void testUUIDCaseInsensitive() {
    assertTrue(detector.isVariable("550e8400-e29b-41d4-a716-446655440000")); // lowercase
    assertTrue(detector.isVariable("550E8400-E29B-41D4-A716-446655440000")); // uppercase
    assertTrue(detector.isVariable("550e8400-E29B-41d4-A716-446655440000")); // mixed
  }

  // ========== Description Test ==========

  @Test
  public void testGetDescription() {
    String description = detector.getDescription();
    assertNotNull(description);
    assertTrue(description.contains("Standard"));
  }

  @Test
  public void testGetDescriptionForCustomDetector() {
    VariableDetector customDetector = new StandardVariableDetector(true, false, true, false, false);
    String description = customDetector.getDescription();
    assertNotNull(description);
    // Description is static, doesn't change based on configuration
    assertTrue(description.contains("Standard"));
  }
}
