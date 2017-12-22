/*
 * Copyright 2017 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.kayenta.metrics.limiters;

import org.junit.Test;

import static junit.framework.TestCase.*;

public class NamedConcurrencyLimiterTest {
  @Test
  public void addAndRemoveWorks() {
    NamedConcurrencyLimiter cl = new NamedConcurrencyLimiter(5);

    cl.register("x", "one");
    assertEquals(1, cl.waitcount("x"));

    cl.register("x", "one"); // duplicate add should not affect count
    assertEquals(1, cl.waitcount("x"));

    cl.register("x", "two"); // add a second one
    assertEquals(2, cl.waitcount("x"));

    cl.cancel("x", "one"); // cancel one of them
    assertEquals(1, cl.waitcount("x"));

    cl.cancel("x", "three"); // key doesn't exist, should not modify list
    assertEquals(1, cl.waitcount("x"));
  }

  @Test
  public void canRunWorks() {
    NamedConcurrencyLimiter cl = new NamedConcurrencyLimiter(5);

    cl.register("x", "one");
    cl.register("x", "two");
    cl.register("x", "three");
    cl.register("x", "four");
    cl.register("x", "five");
    cl.register("x", "six");
    cl.register("x", "seven");
    cl.register("x", "eight");
    cl.register("x", "nine");
    cl.register("x", "ten");

    assertTrue(cl.canRun("x", "one"));
    assertTrue(cl.canRun("x", "two"));
    assertTrue(cl.canRun("x", "three"));
    assertTrue(cl.canRun("x", "four"));
    assertTrue(cl.canRun("x", "five"));
    assertFalse(cl.canRun("x", "six"));
    assertFalse(cl.canRun("x", "seven"));
    assertFalse(cl.canRun("x", "eight"));
    assertFalse(cl.canRun("x", "nine"));
    assertFalse(cl.canRun("x", "ten"));

    cl.release("x", "one");
    assertTrue(cl.canRun("x", "six"));
    assertFalse(cl.canRun("x", "seven"));
    assertFalse(cl.canRun("x", "eight"));
    assertFalse(cl.canRun("x", "nine"));
    assertFalse(cl.canRun("x", "ten"));
  }

  @Test(expected = IllegalStateException.class)
  public void testRemovingTooManyTimes() {
    NamedConcurrencyLimiter cl = new NamedConcurrencyLimiter(5);

    cl.register("x", "one");
    cl.release("x", "one");
    cl.release("x", "one");
  }

  @Test
  public void testSetConcurrency() {
    NamedConcurrencyLimiter cl = new NamedConcurrencyLimiter(1);

    cl.register("x", "one");
    cl.register("x", "two");
    assertTrue(cl.canRun("x", "one"));
    assertFalse(cl.canRun("x", "two"));
    cl.setConcurrencyLimit("x", 2);
    assertTrue(cl.canRun("x", "two"));
  }

}
