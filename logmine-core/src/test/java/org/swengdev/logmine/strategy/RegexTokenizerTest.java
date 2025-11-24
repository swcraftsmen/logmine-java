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

import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests for RegexTokenizer. */
public class RegexTokenizerTest {

  @Test
  public void testWhitespaceRegex() {
    // RegexTokenizer MATCHES the pattern, it doesn't split by it
    // To get words, match non-whitespace
    TokenizerStrategy tokenizer = new RegexTokenizer("\\S+");
    List<String> tokens = tokenizer.tokenize("INFO User logged in");

    assertEquals(4, tokens.size());
    assertEquals("INFO", tokens.get(0));
    assertEquals("User", tokens.get(1));
  }

  @Test
  public void testCommaDelimiter() {
    // To split by comma, match everything except comma
    TokenizerStrategy tokenizer = new RegexTokenizer("[^,]+");
    List<String> tokens = tokenizer.tokenize("one,two,three");

    assertEquals(3, tokens.size());
    assertEquals("one", tokens.get(0));
    assertEquals("two", tokens.get(1));
    assertEquals("three", tokens.get(2));
  }

  @Test
  public void testPipeDelimiter() {
    // To split by pipe, match everything except pipe
    TokenizerStrategy tokenizer = new RegexTokenizer("[^|]+");
    List<String> tokens = tokenizer.tokenize("field1|field2|field3");

    assertEquals(3, tokens.size());
    assertEquals("field1", tokens.get(0));
    assertEquals("field2", tokens.get(1));
    assertEquals("field3", tokens.get(2));
  }

  @Test
  public void testMultiCharDelimiter() {
    // To split by colon with optional spaces, match everything except colon
    TokenizerStrategy tokenizer = new RegexTokenizer("[^:]+");
    List<String> tokens = tokenizer.tokenize("key1:value1:key2:value2");

    assertEquals(4, tokens.size());
    assertEquals("key1", tokens.get(0).trim());
    assertEquals("value1", tokens.get(1).trim());
  }

  @Test
  public void testEmptyString() {
    TokenizerStrategy tokenizer = new RegexTokenizer("\\S+");
    List<String> tokens = tokenizer.tokenize("");

    assertEquals(0, tokens.size());
  }

  @Test
  public void testWordTokens() {
    // Match word characters
    TokenizerStrategy tokenizer = new RegexTokenizer("\\w+");
    List<String> tokens = tokenizer.tokenize("hello-world 123");

    // Should match: hello, world, 123
    assertEquals(3, tokens.size());
    assertEquals("hello", tokens.get(0));
    assertEquals("world", tokens.get(1));
    assertEquals("123", tokens.get(2));
  }
}
