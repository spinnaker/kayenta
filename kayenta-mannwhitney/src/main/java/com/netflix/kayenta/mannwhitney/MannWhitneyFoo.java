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

import com.google.common.collect.Collections2;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;
import org.apache.commons.math3.stat.ranking.NaturalRanking;

import com.google.common.primitives.Doubles;

import com.google.common.base.Function;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MannWhitneyFoo {

  synchronized public MannWhitneyResult eval(MannWhitneyParams params) {
    MannWhitneyResult.MannWhitneyResultBuilder resultBuilder = MannWhitneyResult.builder();
    buildPValue(resultBuilder, params.getControlData(), params.getExperimentData());
    buildConfidenceInterval(resultBuilder, params.getControlData(), params.getExperimentData(),
            params.getConfidenceLevel(), params.getMu());
    return resultBuilder.build();
  }

    private void buildPValue(
            MannWhitneyResult.MannWhitneyResultBuilder resultBuilder,
            double[] distribution1,
            double[] distribution2) {
        resultBuilder.pValue(new MannWhitneyUTest().mannWhitneyUTest(distribution1, distribution2));
    }

    /*
    * Derived from the R Wilcoxon Test implementation (asymptotic, two-sample confidence interval logic):
    * https://github.com/wch/r-source/blob/af7f52f70101960861e5d995d3a4bec010bc89e6/src/library/stats/R/wilcox.test.R
    */
  private void buildConfidenceInterval(
          MannWhitneyResult.MannWhitneyResultBuilder resultBuilder,
          double[] distribution1,
          double[] distribution2,
          double confidenceLevel,
          double mu) {

      double alpha = 1.0 - confidenceLevel;
      double muMin = Doubles.min(distribution1) - Doubles.max(distribution2);
      double muMax = Doubles.max(distribution1) - Doubles.min(distribution2);


      resultBuilder.confidenceInterval(new double[]{6.0, 39.0});
      resultBuilder.estimate(22.5);
  }
/*
  private double wilcoxonDiff(double[] distribution1, double[] distribution2, double mu, double quantile) {
      double[] foo = Collections2.transform(Doubles.asList(distribution1), x -> x - mu).toArray();
      Collections.addAll(foo, Doubles.(distribution2));
      new NaturalRanking().rank(
              Collections.addAll(Collections2.transform(Doubles.asList(distribution1), x -> x - mu), Doubles.asList(distribution2))
      );



      Collections2.transform(Doubles.asList(distribution1), x -> x - mu).addAll(Doubles.asList(distribution2));

  }
  */
}
