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

public class ConcurrencyLimiterTest {
  @Test
  public void addAndRemoveWorks() {
    ConcurrencyLimiter cl = new ConcurrencyLimiter(5);

    cl.register("one");
    assertEquals(1, cl.waitcount());

    cl.register("one"); // duplicate add should not affect count
    assertEquals(1, cl.waitcount());

    cl.register("two"); // add a second one
    assertEquals(2, cl.waitcount());

    cl.cancel("one"); // cancel one of them
    assertEquals(1, cl.waitcount());

    cl.cancel("three"); // key doesn't exist, should not modify list
    assertEquals(1, cl.waitcount());
  }

  @Test
  public void canRunWorks() {
    ConcurrencyLimiter cl = new ConcurrencyLimiter(5);

    cl.register("one");
    cl.register("two");
    cl.register("three");
    cl.register("four");
    cl.register("five");
    cl.register("six");
    cl.register("seven");
    cl.register("eight");
    cl.register("nine");
    cl.register("ten");

    assertTrue(cl.canRun("one"));
    assertTrue(cl.canRun("two"));
    assertTrue(cl.canRun("three"));
    assertTrue(cl.canRun("four"));
    assertTrue(cl.canRun("five"));
    assertFalse(cl.canRun("six"));
    assertFalse(cl.canRun("seven"));
    assertFalse(cl.canRun("eight"));
    assertFalse(cl.canRun("nine"));
    assertFalse(cl.canRun("ten"));

    cl.release("one");
    assertTrue(cl.canRun("six"));
    assertFalse(cl.canRun("seven"));
    assertFalse(cl.canRun("eight"));
    assertFalse(cl.canRun("nine"));
    assertFalse(cl.canRun("ten"));
  }

  @Test(expected = IllegalStateException.class)
  public void testRemovingTooManyTimes() {
    ConcurrencyLimiter cl = new ConcurrencyLimiter(5);

    cl.register("one");
    cl.release("one");
    cl.release("one");
  }

  @Test
  public void testSetConcurrency() {
    ConcurrencyLimiter cl = new ConcurrencyLimiter(1);

    cl.register("one");
    cl.register("two");
    assertTrue(cl.canRun("one"));
    assertFalse(cl.canRun("two"));
    cl.setConcurrencyLimit(2);
    assertTrue(cl.canRun("two"));
  }

}
