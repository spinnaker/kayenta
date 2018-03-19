package com.netflix.kayenta.mannwhitney

import org.scalatest.FunSuite
import junit.framework.TestCase.assertEquals
import org.apache.commons.math3.stat.ranking.{NaNStrategy, NaturalRanking, TiesStrategy}

class MannWhitneyRSuite extends FunSuite{

  private val E = 0.000001

  test("returns expected values"){
    val params = MannWhitneyParams.builder.confidenceLevel(0.95).controlData(Array[Double](1.0, 2.0, 3.0, 4.0)).experimentData(Array[Double](10.0, 20.0, 30.0, 40.0)).mu(0).build
    val mw = new MannWhitneyR
    val result = mw.eval(params)
    assertEquals(6.0, result.getConfidenceInterval.head, E)
    assertEquals(39.0, result.getConfidenceInterval.last, E)
    assertEquals(22.5, result.getEstimate, E)
    assertEquals(0.02857142857142857, result.getPValue, E)
  }

  import org.ddahl.rscala.RClient
  test("foo"){
    val params = MannWhitneyParams.builder.confidenceLevel(0.95).controlData(Array[Double](1.0, 2.0, 3.0, 4.0)).experimentData(Array[Double](10.0, 20.0, 30.0, 40.0)).mu(0).build
    val R = RClient()
    R.set("conf.level", params.getConfidenceLevel)
    R.set("x", params.getExperimentData)
    R.set("y", params.getControlData)

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
        |
        |blah <- wdiff(mumin, alpha/2)
        |""".stripMargin
    )

    //println(R.get("foo")._2)
    //println(R.get("baz")._2)
    println("break")
    println(R.get("blah")._1.asInstanceOf[Double])


    val confidenceLevel = params.getConfidenceLevel
    val x = params.getExperimentData
    val y = params.getControlData
    val xLen = x.length.toDouble
    val yLen = y.length.toDouble

    val alpha: Double = 1.0 - confidenceLevel
    val muMin: Double = x.min - y.max
    val muMax: Double = x.max - y.min

    def wilcoxonDiff(x: Array[Double], y: Array[Double], mu: Double, quantile: Double) = {
      val dr = new NaturalRanking(NaNStrategy.MAXIMAL, TiesStrategy.AVERAGE).rank(x.map(_ - mu) ++ y)
      val ntiesCi = dr.groupBy(identity).mapValues(_.length)
      val dz = {
        for (e <- x.indices) yield dr(e)
      }.sum - xLen * (xLen + 1) / 2 - xLen * yLen / 2
      val correctionCi = (if (dz.signum.isNaN) 0 else dz.signum) * 0.5 // assumes correct = true & alternative = 'two.sided'
      val blah = xLen * yLen / 12
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
    val blah = wilcoxonDiff(x, y, muMin, alpha/2)

    println("break")
    println(blah)


  }
}

/*

            if(conf.int) {
                ## Asymptotic confidence interval for the location
                ## parameter mean(x) - mean(y) in the two-sample case
                ## (cf. one-sample case).
                ##
                ## Algorithm not published, for a documentation see the
                ## one-sample case.
                alpha <- 1 - conf.level
                mumin <- min(x) - max(y)
                mumax <- max(x) - min(y)
                wdiff <- function(d, zq) {
                    dr <- rank(c(x - d, y))
                    NTIES.CI <- table(dr)
                    dz <- (sum(dr[seq_along(x)])
                           - n.x * (n.x + 1) / 2 - n.x * n.y / 2)
		                CORRECTION.CI <-
			                 if(correct) {
                            switch(alternative,
                                   "two.sided" = sign(dz) * 0.5,
                                   "greater" = 0.5,
                                   "less" = -0.5)
			                 } else 0
                    SIGMA.CI <- sqrt((n.x * n.y / 12) *
                                     ((n.x + n.y + 1)
                                      - sum(NTIES.CI^3 - NTIES.CI)
                                      / ((n.x + n.y) * (n.x + n.y - 1))))
                    if (SIGMA.CI == 0)
                        stop("cannot compute confidence interval when all observations are tied", call.=FALSE)
                    (dz - CORRECTION.CI) / SIGMA.CI - zq
                }
                root <- function(zq) {
                    ## in extreme cases we need to return endpoints,
                    ## e.g.  wilcox.test(1, 2:60, conf.int=TRUE)
                    f.lower <- wdiff(mumin, zq)
                    if(f.lower <= 0) return(mumin)
                    f.upper <- wdiff(mumax, zq)
                    if(f.upper >= 0) return(mumax)
                    uniroot(wdiff, c(mumin, mumax),
                            f.lower = f.lower, f.upper = f.upper,
                            tol = 1e-4, zq = zq)$root
                }
                cint <- switch(alternative,
                               "two.sided" = {
                                   l <- root(zq = qnorm(alpha/2, lower.tail = FALSE))
                                   u <- root(zq = qnorm(alpha/2))
                                   c(l, u)
                               },
                               "greater" = {
                                   l <- root(zq = qnorm(alpha, lower.tail = FALSE))
                                   c(l, +Inf)
                               },
                               "less" = {
                                   u <- root(zq = qnorm(alpha))
                                   c(-Inf, u)
                               })
                attr(cint, "conf.level") <- conf.level
		            correct <- FALSE # no continuity correction for estimate
		            ESTIMATE <- c("difference in location" =
			                         uniroot(wdiff, c(mumin, mumax), tol = 1e-4,
				                              zq = 0)$root)
            }

 */