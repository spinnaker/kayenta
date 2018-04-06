/*
 * Copyright 2018 Netflix, Inc.
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
  import MannWhitney._

  def eval(params: MannWhitneyParams): MannWhitneyResult = synchronized {
    val pValue = calculatePValue(params.controlData, params.experimentData)
    val (confidenceInterval, estimate) =
      calculateConfidenceInterval(params.controlData, params.experimentData, params.confidenceLevel, params.mu)
    MannWhitneyResult(confidenceInterval, pValue, estimate)
  }

  protected def calculatePValue(distribution1: Array[Double], distribution2: Array[Double]): Double =
    new MannWhitneyUTest().mannWhitneyUTest(distribution1, distribution2)


  /*
  * Derived from the R Wilcoxon Test implementation (asymptotic, two-sample confidence interval logic only)
  */
  protected def calculateConfidenceInterval(distribution1: Array[Double],
                                  distribution2: Array[Double],
                                  confidenceLevel: Double,
                                  mu: Double): (Array[Double], Double) = {
    val y = distribution1
    val x = distribution2
    val xLen = x.length.toDouble
    val yLen = y.length.toDouble

    val alpha: Double = 1.0 - confidenceLevel
    val muMin: Double = x.min - y.max
    val muMax: Double = x.max - y.min

    val wilcoxonDiffWrapper = (zq: Double) => new UnivariateFunction {
      override def value(input: Double): Double = wilcoxonDiff(input, zq, x, y)
    }

    def findRoot(zq: Double): Double = {
      val fLower = wilcoxonDiff(muMin, zq, x, y)
      val fUpper = wilcoxonDiff(muMax, zq, x, y)
      if (fLower <= 0) muMin
      else if (fUpper >= 0) muMax
      else KayentaBrentSolver.brentDirect(muMin, muMax, fLower, fUpper, wilcoxonDiffWrapper(zq))
    }

    val zQuant = new NormalDistribution(0,1).inverseCumulativeProbability(alpha/2)
    val confidenceInterval: Array[Double] =
      Array(
        findRoot(zQuant * -1),
        findRoot(zQuant)
      )
    val fLower = wilcoxonDiff(muMin, 0, x, y)
    val fUpper = wilcoxonDiff(muMax, 0, x, y)

    val estimate = KayentaBrentSolver.brentDirect(muMin, muMax, fLower, fUpper, wilcoxonDiffWrapper(0))
    (confidenceInterval, estimate)
  }


  //todo: REMOVE  for debug purposes only
  import org.ddahl.rscala.RClient
  protected def calculateConfidenceInterval2(distribution1: Array[Double],
                                            distribution2: Array[Double],
                                            confidenceLevel: Double,
                                            mu: Double): (Array[Double], Double) = {
    val R = RClient()
    R.set("conf.level", confidenceLevel)
    R.set("x", distribution2)
    R.set("y", distribution1)
    R.eval(
      """
        |res <- wilcox.test(x,y,conf.int=TRUE,mu=%s,conf.level=%s)
        |pval <- res$p.value
        |lcint <- res$conf.int[1]
        |hcint <- res$conf.int[2]
        |est <- res$estimate
        |""".stripMargin.format(mu.toString, confidenceLevel.toString)
    )
    (Array(R.get("lcint")._1.asInstanceOf[Double], R.get("hcint")._1.asInstanceOf[Double]), R.get("est")._1.asInstanceOf[Double])
  }

}

object MannWhitney {
  def wilcoxonDiff(mu: Double,
                   quantile: Double,
                   x: Array[Double],
                   y: Array[Double]): Double = {
    val xLen = x.length.toDouble
    val yLen = y.length.toDouble

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
    if (sigmaCi == 0) throw new MannWhitneyException("cannot compute confidence interval when all observations are tied")
    (dz - correctionCi) / sigmaCi - quantile
  }
}
