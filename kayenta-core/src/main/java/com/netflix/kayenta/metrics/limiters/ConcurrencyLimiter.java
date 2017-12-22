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

import java.util.LinkedList;

//
// This class implements a concurrency limit where slots are guaranteed based
// on order.  It's non-blocking, and intended to be used where tasks run,
// check if they can get a resource, and if not requeue themselves and try
// again later, such as with Orca.
//
// There is no synchronization provided; if needed, it should be external.
//
public class ConcurrencyLimiter {
  public long getConcurrencyLimit() {
    return concurrencyLimit;
  }

  public void setConcurrencyLimit(long concurrencyLimit) {
    this.concurrencyLimit = concurrencyLimit;
  }

  private long concurrencyLimit;
  private long running;

  private LinkedList<String> concurrencyWaitList;

  public ConcurrencyLimiter(long concurrencyLimit) {
    this.concurrencyLimit = concurrencyLimit;
    concurrencyWaitList = new LinkedList<>();
    running = 0;
  }

  public void register(String tag) {
    if (!concurrencyWaitList.contains(tag)) {
      concurrencyWaitList.addLast(tag);
    }
  }

  public void cancel(String tag) {
    concurrencyWaitList.remove(tag);
  }

  public long waitcount() {
    return concurrencyWaitList.size();
  }

  public void release(String tag) {
    if (running <= 0) {
      throw new IllegalStateException("attempt to release tag '" + tag + "' would reduce running count below zero");
    }
    running -= 1;
  }

  public boolean canRun(String tag) {
    // If it doesn't exist, add it.
    if (!concurrencyWaitList.contains(tag)) {
      concurrencyWaitList.addLast(tag);
    }

    long remainingSlots = concurrencyLimit - running;
    // if there are no slots available, just return false.
    if (remainingSlots <= 0) {
      return false;
    }

    // if the potential for a slot exists, search for it.
    if (concurrencyWaitList.indexOf(tag) < remainingSlots) {
      concurrencyWaitList.remove(tag);
      running += 1;
      return true;
    }

    return false;
  }
}
