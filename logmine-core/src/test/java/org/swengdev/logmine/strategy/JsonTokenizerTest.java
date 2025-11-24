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

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for JSON log tokenization. */
public class JsonTokenizerTest {

  private JsonTokenizer tokenizer;

  @BeforeEach
  public void setUp() {
    tokenizer = new JsonTokenizer();
  }

  @Test
  public void testSimpleJsonObject() {
    String json = "{\"level\":\"INFO\",\"message\":\"User logged in\"}";
    List<String> tokens = tokenizer.tokenize(json);

    assertNotNull(tokens);
    assertTrue(tokens.contains("{"));
    assertTrue(tokens.contains("}"));
    assertTrue(tokens.contains("level"));
    assertTrue(tokens.contains("INFO"));
    assertTrue(tokens.contains("message"));
    assertTrue(tokens.contains("User logged in"));
    assertTrue(tokens.contains(":"));
  }

  @Test
  public void testMultipleFields() {
    String json = "{\"level\":\"ERROR\",\"user\":\"alice\",\"action\":\"delete\"}";
    List<String> tokens = tokenizer.tokenize(json);

    assertNotNull(tokens);
    assertEquals("{", tokens.get(0));
    assertTrue(tokens.contains("level"));
    assertTrue(tokens.contains("ERROR"));
    assertTrue(tokens.contains("user"));
    assertTrue(tokens.contains("alice"));
    assertTrue(tokens.contains("action"));
    assertTrue(tokens.contains("delete"));
    assertEquals("}", tokens.get(tokens.size() - 1));
  }

  @Test
  public void testJsonWithNumbers() {
    String json = "{\"status\":200,\"duration\":123}";
    List<String> tokens = tokenizer.tokenize(json);

    assertNotNull(tokens);
    assertTrue(tokens.contains("status"));
    assertTrue(tokens.contains("200"));
    assertTrue(tokens.contains("duration"));
    assertTrue(tokens.contains("123"));
  }

  @Test
  public void testJsonWithSpaces() {
    String json = "{ \"level\" : \"INFO\" , \"message\" : \"Test\" }";
    List<String> tokens = tokenizer.tokenize(json);

    assertNotNull(tokens);
    assertTrue(tokens.contains("level"));
    assertTrue(tokens.contains("INFO"));
    assertTrue(tokens.contains("message"));
    assertTrue(tokens.contains("Test"));
  }

  @Test
  public void testEmptyJson() {
    String json = "{}";
    List<String> tokens = tokenizer.tokenize(json);

    assertNotNull(tokens);
    assertEquals(2, tokens.size());
    assertEquals("{", tokens.get(0));
    assertEquals("}", tokens.get(1));
  }

  @Test
  public void testNullInput() {
    List<String> tokens = tokenizer.tokenize(null);
    assertNotNull(tokens);
    assertTrue(tokens.isEmpty());
  }

  @Test
  public void testEmptyString() {
    List<String> tokens = tokenizer.tokenize("");
    assertNotNull(tokens);
    assertTrue(tokens.isEmpty());
  }

  @Test
  public void testWhitespaceOnly() {
    List<String> tokens = tokenizer.tokenize("   ");
    assertNotNull(tokens);
    assertTrue(tokens.isEmpty());
  }

  @Test
  public void testNonJsonInput() {
    // Should fall back to whitespace tokenization
    String text = "INFO User logged in successfully";
    List<String> tokens = tokenizer.tokenize(text);

    assertNotNull(tokens);
    assertEquals(List.of("INFO", "User", "logged", "in", "successfully"), tokens);
  }

  @Test
  public void testJsonWithQuotedComma() {
    String json = "{\"message\":\"Hello, world\",\"level\":\"INFO\"}";
    List<String> tokens = tokenizer.tokenize(json);

    assertNotNull(tokens);
    assertTrue(tokens.contains("message"));
    assertTrue(tokens.contains("Hello, world"), "Should preserve comma in quoted string");
    assertTrue(tokens.contains("level"));
    assertTrue(tokens.contains("INFO"));
  }

  @Test
  public void testJsonWithQuotedColon() {
    String json = "{\"timestamp\":\"12:30:45\",\"event\":\"login\"}";
    List<String> tokens = tokenizer.tokenize(json);

    assertNotNull(tokens);
    assertTrue(tokens.contains("timestamp"));
    assertTrue(tokens.contains("12:30:45"), "Should preserve colons in quoted string");
    assertTrue(tokens.contains("event"));
    assertTrue(tokens.contains("login"));
  }

  @Test
  public void testJsonWithEscapedQuotes() {
    String json = "{\"message\":\"He said \\\"hello\\\"\"}";
    List<String> tokens = tokenizer.tokenize(json);

    assertNotNull(tokens);
    assertTrue(tokens.contains("message"));
    // Escaped quotes should be preserved
    assertTrue(tokens.stream().anyMatch(t -> t.contains("hello")), "Should handle escaped quotes");
  }

  @Test
  public void testJsonWithSpecialCharacters() {
    String json = "{\"path\":\"/usr/local/bin\",\"url\":\"https://api.example.com\"}";
    List<String> tokens = tokenizer.tokenize(json);

    assertNotNull(tokens);
    assertTrue(tokens.contains("path"));
    assertTrue(tokens.contains("/usr/local/bin"));
    assertTrue(tokens.contains("url"));
    assertTrue(tokens.contains("https://api.example.com"));
  }

  @Test
  public void testSingleField() {
    String json = "{\"key\":\"value\"}";
    List<String> tokens = tokenizer.tokenize(json);

    assertNotNull(tokens);
    assertTrue(tokens.contains("{"));
    assertTrue(tokens.contains("key"));
    assertTrue(tokens.contains(":"));
    assertTrue(tokens.contains("value"));
    assertTrue(tokens.contains("}"));
    assertFalse(tokens.contains(","), "Single field should not have trailing comma");
  }

  @Test
  public void testJsonWithBooleans() {
    String json = "{\"success\":true,\"enabled\":false}";
    List<String> tokens = tokenizer.tokenize(json);

    assertNotNull(tokens);
    assertTrue(tokens.contains("success"));
    assertTrue(tokens.contains("true"));
    assertTrue(tokens.contains("enabled"));
    assertTrue(tokens.contains("false"));
  }

  @Test
  public void testJsonWithEmptyValues() {
    String json = "{\"field1\":\"\",\"field2\":\"value\"}";
    List<String> tokens = tokenizer.tokenize(json);

    assertNotNull(tokens);
    assertTrue(tokens.contains("field1"));
    assertTrue(tokens.contains("field2"));
    assertTrue(tokens.contains("value"));
  }

  @Test
  public void testMalformedJsonMissingClosingBrace() {
    String json = "{\"level\":\"INFO\"";
    List<String> tokens = tokenizer.tokenize(json);

    // Should fall back to whitespace tokenization
    assertNotNull(tokens);
    assertFalse(tokens.isEmpty());
  }

  @Test
  public void testMalformedJsonMissingOpeningBrace() {
    String json = "\"level\":\"INFO\"}";
    List<String> tokens = tokenizer.tokenize(json);

    // Should fall back to whitespace tokenization
    assertNotNull(tokens);
    assertFalse(tokens.isEmpty());
  }

  @Test
  public void testDescription() {
    String description = tokenizer.getDescription();
    assertNotNull(description);
    assertTrue(description.toLowerCase().contains("json"));
  }

  @Test
  public void testJsonWithNestedQuotes() {
    String json = "{\"query\":\"SELECT * FROM users WHERE name='alice'\"}";
    List<String> tokens = tokenizer.tokenize(json);

    assertNotNull(tokens);
    assertTrue(tokens.contains("query"));
    assertTrue(
        tokens.stream().anyMatch(t -> t.contains("SELECT")),
        "Should preserve nested quotes in value");
  }

  @Test
  public void testJsonWithUnicodeCharacters() {
    String json = "{\"name\":\"José\",\"city\":\"São Paulo\"}";
    List<String> tokens = tokenizer.tokenize(json);

    assertNotNull(tokens);
    assertTrue(tokens.contains("name"));
    assertTrue(tokens.contains("José"));
    assertTrue(tokens.contains("city"));
    assertTrue(tokens.contains("São Paulo"));
  }

  @Test
  public void testJsonWithLargeNumbers() {
    String json = "{\"timestamp\":1638360000000,\"value\":999999999999}";
    List<String> tokens = tokenizer.tokenize(json);

    assertNotNull(tokens);
    assertTrue(tokens.contains("timestamp"));
    assertTrue(tokens.contains("1638360000000"));
    assertTrue(tokens.contains("value"));
    assertTrue(tokens.contains("999999999999"));
  }

  @Test
  public void testJsonLikeStringWithoutBraces() {
    String json = "level:INFO,message:Test";
    List<String> tokens = tokenizer.tokenize(json);

    // Should fall back to whitespace tokenization
    assertNotNull(tokens);
    // Will be treated as single token since no whitespace
    assertFalse(tokens.isEmpty());
  }

  @Test
  public void testRealWorldLogExample() {
    String json =
        "{\"timestamp\":\"2024-11-24T10:30:00Z\",\"level\":\"ERROR\",\"service\":\"api\","
            + "\"message\":\"Database connection failed\",\"error\":\"Connection timeout\"}";
    List<String> tokens = tokenizer.tokenize(json);

    assertNotNull(tokens);
    assertTrue(tokens.contains("{"));
    assertTrue(tokens.contains("timestamp"));
    assertTrue(tokens.contains("2024-11-24T10:30:00Z"));
    assertTrue(tokens.contains("level"));
    assertTrue(tokens.contains("ERROR"));
    assertTrue(tokens.contains("service"));
    assertTrue(tokens.contains("api"));
    assertTrue(tokens.contains("message"));
    assertTrue(tokens.contains("Database connection failed"));
    assertTrue(tokens.contains("error"));
    assertTrue(tokens.contains("Connection timeout"));
    assertTrue(tokens.contains("}"));
  }
}
