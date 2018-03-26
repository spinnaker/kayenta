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

import org.scalatest.FunSuite
import junit.framework.TestCase.assertEquals

class MannWhitneySuite extends FunSuite {

  private val E = 0.000001

  test("returns expected values") {
    val params = MannWhitneyParams(mu = 0.0, confidenceLevel = 0.95, controlData = Array(1.0, 2.0, 3.0, 4.0), experimentData = Array(10.0, 20.0, 30.0, 40.0))
    val mw = new MannWhitney
    val result = mw.eval(params)
    assertEquals(6.0, result.confidenceInterval.head, E)
    assertEquals(39.0, result.confidenceInterval.last, E)
    assertEquals(22.5, result.estimate, E)
    //assertEquals(0.02857142857142857, result.pValue, E)
  }

  //todo: Remove all tests below...  temporary dummy tests for adhoc tie-outs during development
  import org.ddahl.rscala.RClient
  test("tie out with R version"){
    val params =
      MannWhitneyParams(
        mu = 0.0,
        confidenceLevel = 0.95,
        controlData =
          Array(1.5, 6.0, 5.0, 2.5, 2.5, 2.0, 2.0, 3.5, 3.0, 3.0, 3.0, 2.0, 3.0, 8.0, 4.0, 2.5, 3.0, 2.5, 2.5, 2.0, 2.5, 2.0, 2.0, 2.5, 3.5, 3.0, 1.5, 5.0, 2.5, 5.5, 2.0, 2.5, 3.5, 2.0, 2.5, 4.0, 2.0, 2.0, 2.0, 2.0, 5.5, 4.5, 4.0, 3.0, 3.0, 3.5, 2.5, 3.0, 2.5, 4.0, 6.0, 2.0, 5.0, 6.0, 3.5, 3.0, 54.5, 3.0, 5.0, 6.0, 3.5, 4.5, 2.5, 2.0, 6.0, 5.0, 2.5, 3.5, 2.0, 3.0, 2.5, 3.5, 3.5, 2.5, 3.0, 4.5, 3.0, 15.5, 5.0, 2.5, 3.0, 4.0, 3.5, 4.0, 4.0, 2.0, 4.5, 2.5, 4.0, 3.0),
        experimentData =
          Array(2.5, 2.5, 2.0, 2.5, 3.5, 2.0, 3.5, 1.5, 3.0, 3.0, 2.5, 3.5, 2.0, 2.5, 2.0, 2.0, 5.0, 3.0, 2.0, 1.5, 2.0, 4.0, 3.0, 2.5, 3.0, 2.0, 2.0, 2.0, 2.5, 3.5, 1.5, 1.5, 3.5, 2.0, 2.5, 2.5, 3.0, 3.5, 2.5, 6.5, 2.5, 4.0, 2.5, 3.0, 2.5, 6.5, 2.5, 3.0, 1.5, 2.0, 2.5, 3.0, 7.5, 1.5, 2.0, 4.0, 2.5, 2.0, 2.5, 3.5, 4.0, 2.5, 3.0, 2.5, 2.0, 3.5, 3.0, 16.5, 3.0, 3.0, 7.0, 4.5, 5.0, 2.0, 3.5, 6.0, 1.5, 3.0, 2.5, 4.0, 3.0, 1.5, 5.0, 3.0, 2.5, 5.5, 3.0, 1.5, 2.5, 6.0)
      )
    val R = RClient()
    R.set("conf.level", params.confidenceLevel)
    R.set("x", params.experimentData)
    R.set("y", params.controlData)
    R.eval(
      """
        |res <- wilcox.test(x,y,conf.int=TRUE,mu=%s,conf.level=%s)
        |pval <- res$p.value
        |lcint <- res$conf.int[1]
        |hcint <- res$conf.int[2]
        |est <- res$estimate
        |""".stripMargin.format(params.mu.toString, params.confidenceLevel.toString)
    )

    println("ci low " + R.get("lcint")._1.asInstanceOf[Double])
    println("ci high " + R.get("hcint")._1.asInstanceOf[Double])
    println("est " + R.get("est")._1.asInstanceOf[Double])
    println("pval " + R.get("pval")._1.asInstanceOf[Double])

    val mw = new MannWhitney
    val result = mw.eval(params)

    println("ci low " + result.confidenceInterval.head)
    println("ci high " + result.confidenceInterval.last)
    println("est " + result.estimate)
    println("pval " + result.pValue)

  }


  import org.apache.commons.math3.stat.ranking.{NaNStrategy, NaturalRanking, TiesStrategy}
  import org.apache.commons.math3.analysis.solvers.BracketingNthOrderBrentSolver
  import org.apache.commons.math3.analysis.UnivariateFunction
  import org.apache.commons.math3.distribution.NormalDistribution
  test("tie out intermediate variables"){
    val experimentData = Array(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.6666666666666572, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.7037037037036811, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.5625000000000142, 0.0, 0.0, 0.0, 0.0, 3.3333333333333428, 0.0, 1.7543859649122737, 0.0, 1.8518518518518476, 0.0, 0.0, 1.8518518518518476, 0.0, 0.0, 1.5151515151515156, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.7857142857142776, 1.3333333333333286, 0.0, 3.1250000000000142, 1.9607843137255117, 1.4492753623188293, 2.4390243902438868, 1.6393442622950687, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.6129032258064484, 1.9607843137255117, 1.5873015873015817, 0.0, 0.0, 2.8169014084507182, 0.0, 0.0, 3.0303030303030454, 0.0, 0.0, 3.3898305084745743, 0.0, 0.0, 1.234567901234584, 0.0, 0.0, 0.0, 0.0, 1.7241379310344769, 0.0, 0.0, 0.0, 0.0, 1.6949152542372872, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.470588235294116, 0.0, 0.0, 1.3698630136986196, 3.225806451612911, 1.7543859649122879, 3.4482758620689538, 0.0, 3.3333333333333428, 0.0, 0.0, 0.0, 1.818181818181813, 0.0, 0.0, 1.4285714285714448, 0.0, 0.0, 1.8867924528301927, 2.0000000000000284, 0.0, 0.0, 0.0, 2.985074626865682, 0.0, 0.0, 1.6666666666666714, 0.0, 0.0, 1.5625, 1.9230769230769198, 0.0, 1.8518518518518619, 0.0, 2.7027027027027088, 0.0, 0.0, 0.0, 1.3513513513513544, 0.0, 1.818181818181813, 0.0, 1.6666666666666572, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.818181818181813, 0.0, 1.4285714285714164, 0.0, 0.0, 4.1095890410959015, 0.0, 0.0, 0.0, 1.8518518518518619, 0.0, 0.0, 1.8518518518518476, 1.3888888888888857, 0.0, 0.0, 0.0, 0.0, 1.8867924528301785, 1.4285714285714164, 0.0, 0.0, 1.5625, 1.8867924528301927, 0.0, 0.0, 0.0, 0.0, 2.040816326530603, 1.818181818181813, 0.0, 0.0, 1.6393442622950687, 0.0, 1.7241379310344769, 1.5151515151515156, 2.564102564102555, 0.0, 0.0, 0.0, 0.0, 5.5555555555555571, 0.0, 0.0, 4.0816326530612059, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.5714285714285836, 0.0, 2.040816326530603, 3.1746031746031633, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    val controlData = Array(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.5625000000000142, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.3888888888888857, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.5384615384615614, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.4925373134328339, 0.0, 0.0, 0.0, 1.6949152542373014, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.470588235294116, 0.0, 0.0, 0.0, 0.0, 1.7543859649122879, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.8518518518518619, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.6393442622950687, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.7241379310344769, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.5873015873015817, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.4925373134328339, 0.0, 0.0, 0.0, 3.1249999999999858, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.4925373134328339, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.9850746268656536, 0.0, 2.5000000000000284, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.7857142857142776, 0.0, 0.0, 0.0)

    val params =
      MannWhitneyParams(
        mu = 0.0,
        confidenceLevel = 0.95,
        controlData = controlData,
        experimentData = experimentData
      )
    val R = RClient()
    R.set("conf.level", params.confidenceLevel)
    R.set("x", params.experimentData)
    R.set("y", params.controlData)

    R.eval(
      """
        |alpha <- 1 - conf.level
        |mumin <- min(x) - max(y)
        |mumax <- max(x) - min(y)
        |
        |n.x <- as.double(length(x))
        |n.y <- as.double(length(y))
        |correct <- TRUE
        |alternative <- "two.sided"
        |
        |wdiff <- function(d, zq) {
        |                    dr <- rank(c(x - d, y))
        |                    NTIES.CI <- table(dr)
        |                    dz <- (sum(dr[seq_along(x)])
        |                           - n.x * (n.x + 1) / 2 - n.x * n.y / 2)
        |		                CORRECTION.CI <-
        |			                 if(correct) {
        |                            switch(alternative,
        |                                   "two.sided" = sign(dz) * 0.5,
        |                                   "greater" = 0.5,
        |                                   "less" = -0.5)
        |			                 } else 0
        |                    SIGMA.CI <- sqrt((n.x * n.y / 12) *
        |                                     ((n.x + n.y + 1)
        |                                      - sum(NTIES.CI^3 - NTIES.CI)
        |                                      / ((n.x + n.y) * (n.x + n.y - 1))))
        |                    if (SIGMA.CI == 0)
        |                        stop("cannot compute confidence interval when all observations are tied", call.=FALSE)
        |                    (dz - CORRECTION.CI) / SIGMA.CI - zq
        |                }
        |root <- function(zq) {
        |                    ## in extreme cases we need to return endpoints,
        |                    ## e.g.  wilcox.test(1, 2:60, conf.int=TRUE)
        |                    f.lower <- wdiff(mumin, zq)
        |                    if(f.lower <= 0) return(mumin)
        |                    f.upper <- wdiff(mumax, zq)
        |                    if(f.upper >= 0) return(mumax)
        |                    uniroot(wdiff, c(mumin, mumax),
        |                            f.lower = f.lower, f.upper = f.upper,
        |                            tol = 1e-4, zq = zq)$root
        |                }
        |
        |zq.lower <- qnorm(alpha/2, lower.tail = FALSE)
        |zq.upper <- qnorm(alpha/2)
        |fl1 <- wdiff(mumin, zq.lower)
        |fu1 <- wdiff(mumax, zq.lower)
        |fl2 <- wdiff(mumin, zq.upper)
        |fu2 <- wdiff(mumax, zq.upper)
        |l <- root(zq = qnorm(alpha/2, lower.tail = FALSE))
        |u <- root(zq = qnorm(alpha/2))
        |est <- uniroot(wdiff, c(mumin, mumax),
        |                         tol = 1e-4, zq = 0)$root
        |""".stripMargin
    )

    println("mumin " + R.get("mumin")._1.asInstanceOf[Double])
    println("mumax " + R.get("mumax")._1.asInstanceOf[Double])
    println("zq.lower " + R.get("zq.lower")._1.asInstanceOf[Double])
    println("zq.upper " + R.get("zq.upper")._1.asInstanceOf[Double])
    println("f.lower1 " + R.get("fl1")._1.asInstanceOf[Double])
    println("f.upper1 " + R.get("fu1")._1.asInstanceOf[Double])
    println("f.lower2 " + R.get("fl2")._1.asInstanceOf[Double])
    println("f.upper2 " + R.get("fu2")._1.asInstanceOf[Double])
    println("ci.lower " + R.get("l")._1.asInstanceOf[Double])
    println("ci.upper " + R.get("u")._1.asInstanceOf[Double])
    println("est " + R.get("est")._1.asInstanceOf[Double])

    val confidenceLevel1 = params.confidenceLevel
    val x = params.experimentData
    val y = params.controlData
    val xLen = x.length.toDouble
    val yLen = y.length.toDouble

    val alpha: Double = 1.0 - confidenceLevel1
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
      if (sigmaCi == 0) throw new MannWhitneyException("cannot compute confidence interval when all observations are tied")
      (dz - correctionCi) / sigmaCi - quantile
    }

    val wilcoxonDiffWrapper = (zq: Double) => new UnivariateFunction {
      override def value(input: Double): Double = wilcoxonDiff(input, zq)
    }

    val zqLower = new NormalDistribution(0,1).inverseCumulativeProbability(alpha/2) * -1
    val zqUpper = new NormalDistribution(0,1).inverseCumulativeProbability(alpha/2)
    val fLower1 = wilcoxonDiff(muMin, zqLower)
    val fUpper1 = wilcoxonDiff(muMax, zqLower)
    val fLower2 = wilcoxonDiff(muMin, zqUpper)
    val fUpper2 = wilcoxonDiff(muMax, zqUpper)
    val ciLower = new BracketingNthOrderBrentSolver().solve(1000, wilcoxonDiffWrapper(zqLower), muMin, muMax)
    val ciUpper = new BracketingNthOrderBrentSolver().solve(1000, wilcoxonDiffWrapper(zqUpper), muMin, muMax)
    val est = new BracketingNthOrderBrentSolver().solve(1000, wilcoxonDiffWrapper(0), muMin, muMax)

    //1e-4, 5
    //diff: -3.5347077141 × 10-5
    //

    println("---------------")
    println("muMin " + muMin)
    println("muMax " + muMax)
    println("zqLower " + zqLower)
    println("zqUpper " + zqUpper)
    println("fLower1 " + fLower1)
    println("fUpper1 " + fUpper1)
    println("fLower2 " + fLower2)
    println("fUpper2 " + fUpper2)
    println("ciLower " + ciLower)
    println("ciUpper " + ciUpper)
    println("est " + est)

  }
}
