package com.netflix.kayenta.mannwhitney

import org.scalatest.FunSuite
import junit.framework.TestCase.assertEquals
import org.apache.commons.math3.analysis.solvers.BracketingNthOrderBrentSolver

class MannWhitneySuite extends FunSuite {

  private val E = 0.000001

  test("returns expected values") {
    val params = MannWhitneyParams.builder.confidenceLevel(0.95).controlData(Array[Double](1.0, 2.0, 3.0, 4.0)).experimentData(Array[Double](10.0, 20.0, 30.0, 40.0)).mu(0).build
    val mw = new MannWhitney
    val result = mw.eval(params)
    assertEquals(6.0, result.getConfidenceInterval.head, E)
    assertEquals(39.0, result.getConfidenceInterval.last, E)
    assertEquals(22.5, result.getEstimate, E)
    //assertEquals(0.02857142857142857, result.getPValue, E)
  }

  import org.ddahl.rscala.RClient
  test("foo"){
    val params = MannWhitneyParams.builder
      .confidenceLevel(0.95)
      .controlData(Array(
        20.758113589510348, 22.802059629464559, 20.612340363492208, 20.973274654749986,
        23.469237330498622, 21.53133988954805, 21.767353687439805, 18.308050730374301,
        19.062145167827566, 25.879498436001779, 18.982629726336128, 19.387920182269266,
        18.995398251246982, 24.228420230932805, 20.050985172694688, 18.2340451091756,
        17.870515494460435, 23.058103863806618, 18.400259567501134, 16.971352091177167,
        19.467529066052151, 17.90465423342031, 17.553948617825831, 16.104611843907193,
        24.38171081433039, 15.919294339922635, 20.479409626304697, 19.08307201492406,
        17.455788050722866, 18.853826823235142
      ))
      .experimentData(Array(
        9.7767343120109462, 10.587071850477695, 7.8375506388901766, 9.3243326886113884,
        8.3774505409784421, 8.9359264399606282, 8.2707663324728511, 9.7484308126716925,
        11.623746715547156, 9.2753442679633942, 8.1538047749171518, 10.562020975754294,
        13.498135575451835, 11.054419256110332, 6.9538903590055092, 12.521008342195479,
        9.0216358603546443, 4.9234856699531662, 11.251360203607172, 11.598449508040337,
        8.435232204668182, 11.110228516867279, 10.70063380164755, 8.9212773652115249,
        10.086167067942661, 11.42107515483087, 9.3792347813644952, 7.7980320148552593,
        10.62339807275746, 11.909642062063739
      ))
      .mu(0)
      .build
    val R = RClient()
    R.set("conf.level", params.getConfidenceLevel)
    R.set("x", params.getExperimentData)
    R.set("y", params.getControlData)
    R.eval(
      """
        |res <- wilcox.test(x,y,conf.int=TRUE,mu=%s,conf.level=%s)
        |pval <- res$p.value
        |lcint <- res$conf.int[1]
        |hcint <- res$conf.int[2]
        |est <- res$estimate
        |""".stripMargin.format(params.getMu.toString, params.getConfidenceLevel.toString)
    )

    println("ci low " + R.get("lcint")._1.asInstanceOf[Double])
    println("ci high " + R.get("hcint")._1.asInstanceOf[Double])
    println("est " + R.get("est")._1.asInstanceOf[Double])
    println("pval " + R.get("pval")._1.asInstanceOf[Double])

    val mw = new MannWhitney
    val result = mw.eval(params)

    println("ci low " + result.getConfidenceInterval.head)
    println("ci high " + result.getConfidenceInterval.last)
    println("est " + result.getEstimate)
    println("pval " + result.getPValue)

  }

  import org.apache.commons.math3.stat.ranking.{NaNStrategy, NaturalRanking, TiesStrategy}
  test("bar"){
    val params = MannWhitneyParams.builder
      .confidenceLevel(0.95)
      .controlData(Array(
        20.758113589510348, 22.802059629464559, 20.612340363492208, 20.973274654749986,
        23.469237330498622, 21.53133988954805, 21.767353687439805, 18.308050730374301,
        19.062145167827566, 25.879498436001779, 18.982629726336128, 19.387920182269266,
        18.995398251246982, 24.228420230932805, 20.050985172694688, 18.2340451091756,
        17.870515494460435, 23.058103863806618, 18.400259567501134, 16.971352091177167,
        19.467529066052151, 17.90465423342031, 17.553948617825831, 16.104611843907193,
        24.38171081433039, 15.919294339922635, 20.479409626304697, 19.08307201492406,
        17.455788050722866, 18.853826823235142
      ))
      .experimentData(Array(
        9.7767343120109462, 10.587071850477695, 7.8375506388901766, 9.3243326886113884,
        8.3774505409784421, 8.9359264399606282, 8.2707663324728511, 9.7484308126716925,
        11.623746715547156, 9.2753442679633942, 8.1538047749171518, 10.562020975754294,
        13.498135575451835, 11.054419256110332, 6.9538903590055092, 12.521008342195479,
        9.0216358603546443, 4.9234856699531662, 11.251360203607172, 11.598449508040337,
        8.435232204668182, 11.110228516867279, 10.70063380164755, 8.9212773652115249,
        10.086167067942661, 11.42107515483087, 9.3792347813644952, 7.7980320148552593,
        10.62339807275746, 11.909642062063739
      ))
      .mu(0)
      .build
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
        |zq <- qnorm(alpha/2)
        |f.lower <- wdiff(mumin, zq)
        |f.upper <- wdiff(mumax, zq)
        |blah <- uniroot(wdiff, c(mumin, mumax),
        |                            f.lower = f.lower, f.upper = f.upper,
        |                            tol = 1e-4, zq = zq)$root
        |""".stripMargin
    )

    println("zq " + R.get("zq")._1.asInstanceOf[Double])
    println("f.lower " + R.get("f.lower")._1.asInstanceOf[Double])
    println("f.upper " + R.get("f.upper")._1.asInstanceOf[Double])
    println("blah " + R.get("blah")._1.asInstanceOf[Double])

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
    import org.apache.commons.math3.analysis.UnivariateFunction
    import org.apache.commons.math3.analysis.solvers.BrentSolver
    import org.apache.commons.math3.distribution.NormalDistribution
    val zq = new NormalDistribution(0,1).inverseCumulativeProbability(alpha/2) //-1.9599639845400536 //alpha/2
    val fLower = wilcoxonDiff(x,y,muMin, zq)
    val fUpper = wilcoxonDiff(x,y,muMax, zq)
    val f = new UnivariateFunction {
      override def value(x2: Double): Double = wilcoxonDiff(x, y, x2, zq)
    }
    //val blah = new BrentSolver(1000, 1e-4).solve(1000, f, muMin, muMax)
    val blah = new BracketingNthOrderBrentSolver().solve(1000, f, muMin, muMax)

    println("zq " + zq)
    println("fLower " + fLower)
    println("fUpper " + fUpper)
    println("blah " + blah)

  }
}
