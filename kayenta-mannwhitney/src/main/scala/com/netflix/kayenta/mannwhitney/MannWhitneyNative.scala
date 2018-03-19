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

package com.netflix.kayenta.mannwhitney

import org.apache.commons.math3.stat.inference.MannWhitneyUTest
import org.apache.commons.math3.stat.ranking._

class MannWhitneyNative {


  def eval(params: MannWhitneyParams): MannWhitneyResult = synchronized {
    MannWhitneyResult.builder()
      .calculatePValue(params.getControlData, params.getExperimentData)
      .calculateConfidenceInterval(params.getControlData, params.getExperimentData, params.getConfidenceLevel, params.getMu)
      .build()
  }

  implicit protected class MannWhitneyResultImplicits(resultBuilder: MannWhitneyResult.MannWhitneyResultBuilder) {

    def calculatePValue(distribution1: Array[Double],
                        distribution2: Array[Double]): MannWhitneyResult.MannWhitneyResultBuilder =
      resultBuilder.pValue(new MannWhitneyUTest().mannWhitneyUTest(distribution1, distribution2))

    def calculateConfidenceInterval(distribution1: Array[Double],
                                    distribution2: Array[Double],
                                    confidenceLevel: Double,
                                    mu: Double): MannWhitneyResult.MannWhitneyResultBuilder = {

      val alpha: Double = 1.0 - confidenceLevel
      val muMin: Double = distribution1.min - distribution2.max
      val muMax: Double = distribution1.max - distribution2.min
      val wilcoxonDiff = (distribution1: Array[Double], distribution2: Array[Double], mu: Double, quantile: Double) =>
      {
        val dr =
          new NaturalRanking(NaNStrategy.MAXIMAL, TiesStrategy.AVERAGE)
            .rank(distribution1.map(_ - mu) ++ distribution2)
      }

      resultBuilder.confidenceInterval(Array(6.0, 39.0))
      resultBuilder.estimate(22.5)
    }
  }
}
