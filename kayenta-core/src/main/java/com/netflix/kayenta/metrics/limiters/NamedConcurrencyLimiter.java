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

import java.util.HashMap;

//
// Maintain a list of ConcurrencyLimiters by name.  If the name used in any
// method does not exist, it will be created with defaultConcurrencyLimit.
// If a specific limit should be set, use setConcurrencyLimit(limiterName, concurrencyLimit)
// to ensure it exists with a specific setting.
//
// All calls are synchronized, and should operate quickly for reasonably short lists
// of tags within the ConcurrencyLimiter.
//
public class NamedConcurrencyLimiter {
  private HashMap<String, ConcurrencyLimiter> limiters;

  synchronized public long getDefaultConcurrencyLimit() {
    return defaultConcurrencyLimit;
  }

  synchronized public void setDefaultConcurrencyLimit(long defaultConcurrencyLimit) {
    this.defaultConcurrencyLimit = defaultConcurrencyLimit;
  }

  private long defaultConcurrencyLimit;

  public NamedConcurrencyLimiter(long defaultConcurrencyLimit) {
    this.defaultConcurrencyLimit = defaultConcurrencyLimit;
    limiters = new HashMap<>();
  }

  synchronized public void setConcurrencyLimit(String limiterName, long concurrencyLimit) {
    ensureExists(limiterName).setConcurrencyLimit(concurrencyLimit);
  }

  synchronized public long getConcurrencyLimit(String limiterName) {
    return ensureExists(limiterName).getConcurrencyLimit();
  }

  synchronized public void register(String limiterName, String tag) {
    ensureExists(limiterName).register(tag);
  }

  synchronized public void release(String limiterName, String tag) {
    ensureExists(limiterName).release(tag);
  }

  synchronized public void cancel(String limiterName, String tag) {
    ensureExists(limiterName).cancel(tag);
  }

  synchronized public long waitcount(String limiterName) {
    return ensureExists(limiterName).waitcount();
  }

  synchronized public boolean canRun(String limiterName, String tag) {
    return ensureExists(limiterName).canRun(tag);
  }

  private ConcurrencyLimiter ensureExists(String limiterName) {
    limiters.computeIfAbsent(limiterName, name -> new ConcurrencyLimiter(defaultConcurrencyLimit));
    return limiters.get(limiterName);
  }

}
