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
import org.apache.commons.math3.analysis.UnivariateFunction
import org.apache.commons.math3.analysis.solvers.{BracketingNthOrderBrentSolver, BrentSolver}
import org.apache.commons.math3.distribution.NormalDistribution
import org.apache.commons.math3.stat.ranking._

class MannWhitney {


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

    /*
    * Derived from the R Wilcoxon Test implementation (asymptotic, two-sample confidence interval logic):
    * https://github.com/wch/r-source/blob/af7f52f70101960861e5d995d3a4bec010bc89e6/src/library/stats/R/wilcox.test.R
    */
    def calculateConfidenceInterval(distribution1: Array[Double],
                                    distribution2: Array[Double],
                                    confidenceLevel: Double,
                                    mu: Double): MannWhitneyResult.MannWhitneyResultBuilder = {
      val y = distribution1
      val x = distribution2
      val xLen = x.length.toDouble
      val yLen = y.length.toDouble

      val alpha: Double = 1.0 - confidenceLevel
      val muMin: Double = x.min - y.max
      val muMax: Double = x.max - y.min

      val wilcoxonDiff = (mu: Double, quantile: Double) => {
        val dr = new NaturalRanking(NaNStrategy.MAXIMAL, TiesStrategy.AVERAGE).rank(x.map(_ - mu) ++ y)
        val ntiesCi = dr.groupBy(identity).mapValues(_.length)
        val dz = {
          for (e <- x.indices) yield dr(e)
        }.sum - xLen * (xLen + 1) / 2 - xLen * yLen / 2
        val correctionCi = (if (dz.signum.isNaN) 0 else dz.signum) * 0.5 // assumes correct = true & alternative = 'two.sided'
        val sigmaCi = Math.sqrt(
          (xLen * yLen / 12) *
            (
              (xLen + yLen + 1)
                - ntiesCi.mapValues(v => Math.pow(v, 3) - v).values.sum
                / ((xLen + yLen) * (xLen + yLen - 1))
              )
          )
        if (sigmaCi == 0) throw new Exception("cannot compute confidence interval when all observations are tied")
        (dz - correctionCi) / sigmaCi - quantile
      }

      val wilcoxonDiffWrapper = (zq: Double) => new UnivariateFunction {
        override def value(input: Double): Double = wilcoxonDiff(input, zq)
      }

      def findRoot(f1: (Double, Double) => Double, zq: Double): Double = {
        val fLower = wilcoxonDiff(muMin, zq)
        val fUpper = wilcoxonDiff(muMax, zq)
        if (fLower <= 0) muMin
        else if (fUpper >= 0) muMax
        else new BracketingNthOrderBrentSolver().solve(1000, wilcoxonDiffWrapper(zq), muMin, muMax)
      }

      val zQuant = new NormalDistribution(0,1).inverseCumulativeProbability(alpha/2)
      val confidenceInterval: Array[Double] =
        Array(
          findRoot(wilcoxonDiff, zQuant * -1),
          findRoot(wilcoxonDiff, zQuant)
        )

      resultBuilder.confidenceInterval(confidenceInterval)
      resultBuilder.estimate(new BracketingNthOrderBrentSolver().solve(1000, wilcoxonDiffWrapper(0), muMin, muMax))
    }
  }
}
