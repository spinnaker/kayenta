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

package com.netflix.kayenta.mannwhitney;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class MannWhitneyFooTest {

  private static final double E = 0.000001;

  @Test
  public void testMannWhitney() {
    MannWhitneyParams params = MannWhitneyParams.builder()
            .confidenceLevel(0.95)
            .controlData(new double[]{1.0, 2.0, 3.0, 4.0})
            .experimentData(new double[]{10.0,20.0,30.0,40.0})
            .mu(0)
            .build();
    MannWhitneyFoo mw = new MannWhitneyFoo();
    MannWhitneyResult result = mw.eval(params);
    assertEquals(6.0, result.getConfidenceInterval()[0],E);
    assertEquals(39.0, result.getConfidenceInterval()[1],E);
    assertEquals(22.5, result.getEstimate(),E);
    assertEquals(0.02857142857142857, result.getPValue(),E);
  }
}
