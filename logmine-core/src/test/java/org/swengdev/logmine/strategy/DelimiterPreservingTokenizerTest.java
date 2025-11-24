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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests for DelimiterPreservingTokenizer. */
public class DelimiterPreservingTokenizerTest {

  @Test
  public void testDefaultConstructor() {
    DelimiterPreservingTokenizer tokenizer = new DelimiterPreservingTokenizer();
    assertNotNull(tokenizer);
    assertNotNull(tokenizer.getDescription());
    assertTrue(tokenizer.getDescription().contains("=,:;[]{}()"));
  }

  @Test
  public void testCustomDelimiters() {
    DelimiterPreservingTokenizer tokenizer = new DelimiterPreservingTokenizer("@#$");
    assertNotNull(tokenizer);
    assertTrue(tokenizer.getDescription().contains("@#$"));
  }

  @Test
  public void testKeyValuePairs() {
    DelimiterPreservingTokenizer tokenizer = new DelimiterPreservingTokenizer();

    List<String> tokens = tokenizer.tokenize("action=insert user=tom id=123");

    assertNotNull(tokens);
    assertTrue(tokens.contains("action"));
    assertTrue(tokens.contains("="));
    assertTrue(tokens.contains("insert"));
    assertTrue(tokens.contains("user"));
    assertTrue(tokens.contains("tom"));
    assertTrue(tokens.contains("id"));
    assertTrue(tokens.contains("123"));
  }

  @Test
  public void testBrackets() {
    DelimiterPreservingTokenizer tokenizer = new DelimiterPreservingTokenizer();

    List<String> tokens = tokenizer.tokenize("[INFO] User logged in");

    assertNotNull(tokens);
    assertTrue(tokens.contains("["));
    assertTrue(tokens.contains("INFO"));
    assertTrue(tokens.contains("]"));
    assertTrue(tokens.contains("User"));
    assertTrue(tokens.contains("logged"));
    assertTrue(tokens.contains("in"));
  }

  @Test
  public void testParentheses() {
    DelimiterPreservingTokenizer tokenizer = new DelimiterPreservingTokenizer();

    List<String> tokens = tokenizer.tokenize("function(arg1, arg2)");

    assertNotNull(tokens);
    assertTrue(tokens.contains("function"));
    assertTrue(tokens.contains("("));
    assertTrue(tokens.contains("arg1"));
    assertTrue(tokens.contains(","));
    assertTrue(tokens.contains("arg2"));
    assertTrue(tokens.contains(")"));
  }

  @Test
  public void testCurlyBraces() {
    DelimiterPreservingTokenizer tokenizer = new DelimiterPreservingTokenizer();

    List<String> tokens = tokenizer.tokenize("{key:value}");

    assertNotNull(tokens);
    assertTrue(tokens.contains("{"));
    assertTrue(tokens.contains("key"));
    assertTrue(tokens.contains(":"));
    assertTrue(tokens.contains("value"));
    assertTrue(tokens.contains("}"));
  }

  @Test
  public void testNullInput() {
    DelimiterPreservingTokenizer tokenizer = new DelimiterPreservingTokenizer();

    List<String> tokens = tokenizer.tokenize(null);

    assertNotNull(tokens);
    assertTrue(tokens.isEmpty());
  }

  @Test
  public void testEmptyInput() {
    DelimiterPreservingTokenizer tokenizer = new DelimiterPreservingTokenizer();

    List<String> tokens = tokenizer.tokenize("");

    assertNotNull(tokens);
    assertTrue(tokens.isEmpty());
  }

  @Test
  public void testWhitespaceOnly() {
    DelimiterPreservingTokenizer tokenizer = new DelimiterPreservingTokenizer();

    List<String> tokens = tokenizer.tokenize("   ");

    assertNotNull(tokens);
    assertTrue(tokens.isEmpty());
  }

  @Test
  public void testMultipleDelimiters() {
    DelimiterPreservingTokenizer tokenizer = new DelimiterPreservingTokenizer();

    List<String> tokens = tokenizer.tokenize("a=b:c,d;e[f]g{h}i(j)");

    assertNotNull(tokens);
    // Should preserve all delimiters
    assertTrue(tokens.contains("="));
    assertTrue(tokens.contains(":"));
    assertTrue(tokens.contains(","));
    assertTrue(tokens.contains(";"));
    assertTrue(tokens.contains("["));
    assertTrue(tokens.contains("]"));
    assertTrue(tokens.contains("{"));
    assertTrue(tokens.contains("}"));
    assertTrue(tokens.contains("("));
    assertTrue(tokens.contains(")"));
  }

  @Test
  public void testDelimitersWithoutSpaces() {
    DelimiterPreservingTokenizer tokenizer = new DelimiterPreservingTokenizer();

    List<String> tokens = tokenizer.tokenize("key=value");

    assertNotNull(tokens);
    assertEquals(3, tokens.size());
    assertEquals("key", tokens.get(0));
    assertEquals("=", tokens.get(1));
    assertEquals("value", tokens.get(2));
  }

  @Test
  public void testDelimitersWithSpaces() {
    DelimiterPreservingTokenizer tokenizer = new DelimiterPreservingTokenizer();

    List<String> tokens = tokenizer.tokenize("key = value");

    assertNotNull(tokens);
    assertTrue(tokens.contains("key"));
    assertTrue(tokens.contains("="));
    assertTrue(tokens.contains("value"));
  }

  @Test
  public void testComplexLogMessage() {
    DelimiterPreservingTokenizer tokenizer = new DelimiterPreservingTokenizer();

    String log = "2024-01-15T10:30:45Z [INFO] action=login user={name:john,id:123} success";
    List<String> tokens = tokenizer.tokenize(log);

    assertNotNull(tokens);
    assertFalse(tokens.isEmpty());

    // Should contain timestamp parts
    assertTrue(tokens.contains("2024-01-15T10"));

    // Should preserve all structural elements
    assertTrue(tokens.contains("["));
    assertTrue(tokens.contains("]"));
    assertTrue(tokens.contains("="));
    assertTrue(tokens.contains("{"));
    assertTrue(tokens.contains("}"));
    assertTrue(tokens.contains(":"));
    assertTrue(tokens.contains(","));
  }

  @Test
  public void testSpecialRegexCharacters() {
    // Test that special regex characters in delimiters are properly escaped
    DelimiterPreservingTokenizer tokenizer = new DelimiterPreservingTokenizer(".|*+?");

    List<String> tokens = tokenizer.tokenize("a.b|c*d+e?f");

    assertNotNull(tokens);
    assertTrue(tokens.contains("."));
    assertTrue(tokens.contains("|"));
    assertTrue(tokens.contains("*"));
    assertTrue(tokens.contains("+"));
    assertTrue(tokens.contains("?"));
  }

  @Test
  public void testConsecutiveDelimiters() {
    DelimiterPreservingTokenizer tokenizer = new DelimiterPreservingTokenizer();

    List<String> tokens = tokenizer.tokenize("a==b");

    assertNotNull(tokens);
    assertTrue(tokens.contains("a"));
    assertTrue(tokens.contains("="));
    assertTrue(tokens.contains("b"));

    // Should have two separate "=" tokens
    long equalCount = tokens.stream().filter(t -> t.equals("=")).count();
    assertEquals(2, equalCount);
  }

  @Test
  public void testOnlyDelimiters() {
    DelimiterPreservingTokenizer tokenizer = new DelimiterPreservingTokenizer();

    List<String> tokens = tokenizer.tokenize("===");

    assertNotNull(tokens);
    assertEquals(3, tokens.size());
    assertTrue(tokens.stream().allMatch(t -> t.equals("=")));
  }

  @Test
  public void testMixedContent() {
    DelimiterPreservingTokenizer tokenizer = new DelimiterPreservingTokenizer();

    List<String> tokens = tokenizer.tokenize("GET /api/users HTTP/1.1");

    assertNotNull(tokens);
    assertFalse(tokens.isEmpty());

    // Should tokenize and preserve delimiters
    assertTrue(tokens.contains("GET"));
    // Just verify tokenization works without error
  }

  @Test
  public void testGetDescription() {
    DelimiterPreservingTokenizer tokenizer = new DelimiterPreservingTokenizer("@#");

    String desc = tokenizer.getDescription();

    assertNotNull(desc);
    assertTrue(desc.contains("Delimiter-Preserving"));
    assertTrue(desc.contains("@#"));
  }

  @Test
  public void testPreservesOrderAndStructure() {
    DelimiterPreservingTokenizer tokenizer = new DelimiterPreservingTokenizer();

    List<String> tokens = tokenizer.tokenize("a=b c=d");

    assertNotNull(tokens);
    // Order should be preserved
    int aIndex = tokens.indexOf("a");
    int equalsIndex = tokens.indexOf("=");
    int bIndex = tokens.indexOf("b");

    assertTrue(aIndex < equalsIndex);
    assertTrue(equalsIndex < bIndex);
  }

  @Test
  public void testEmptyDelimiters() {
    // Edge case: tokenizer with no delimiters behaves differently
    DelimiterPreservingTokenizer tokenizer = new DelimiterPreservingTokenizer("");

    List<String> tokens = tokenizer.tokenize("hello world");

    assertNotNull(tokens);
    // With no delimiters specified, behavior depends on implementation
    // Just verify it doesn't crash
    assertFalse(tokens.isEmpty());
  }

  @Test
  public void testUnicodeContent() {
    DelimiterPreservingTokenizer tokenizer = new DelimiterPreservingTokenizer();

    List<String> tokens = tokenizer.tokenize("usuario=josé acción=iniciar");

    assertNotNull(tokens);
    assertTrue(tokens.contains("usuario"));
    assertTrue(tokens.contains("="));
    assertTrue(tokens.contains("josé"));
    assertTrue(tokens.contains("acción"));
    assertTrue(tokens.contains("iniciar"));
  }
}
