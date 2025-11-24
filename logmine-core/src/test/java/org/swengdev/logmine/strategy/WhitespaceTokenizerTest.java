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

/** Tests for WhitespaceTokenizer. */
public class WhitespaceTokenizerTest {

  private final TokenizerStrategy tokenizer = new WhitespaceTokenizer();

  @Test
  public void testBasicTokenization() {
    List<String> tokens = tokenizer.tokenize("INFO User logged in");

    assertEquals(4, tokens.size());
    assertEquals("INFO", tokens.get(0));
    assertEquals("User", tokens.get(1));
    assertEquals("logged", tokens.get(2));
    assertEquals("in", tokens.get(3));
  }

  @Test
  public void testMultipleSpaces() {
    List<String> tokens = tokenizer.tokenize("INFO  User   logged");

    assertEquals(3, tokens.size());
    assertEquals("INFO", tokens.get(0));
    assertEquals("User", tokens.get(1));
    assertEquals("logged", tokens.get(2));
  }

  @Test
  public void testTabsAndSpaces() {
    List<String> tokens = tokenizer.tokenize("INFO\tUser\t\tlogged");

    assertEquals(3, tokens.size());
  }

  @Test
  public void testEmptyString() {
    List<String> tokens = tokenizer.tokenize("");

    assertEquals(0, tokens.size());
  }

  @Test
  public void testSingleWord() {
    List<String> tokens = tokenizer.tokenize("SingleWord");

    assertEquals(1, tokens.size());
    assertEquals("SingleWord", tokens.get(0));
  }

  @Test
  public void testLeadingAndTrailingSpaces() {
    List<String> tokens = tokenizer.tokenize("  Leading and trailing  ");

    assertEquals(3, tokens.size());
    assertEquals("Leading", tokens.get(0));
  }
}
