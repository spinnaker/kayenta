package com.netflix.kayenta.mannwhitney

import org.scalatest.FunSuite
import junit.framework.TestCase.assertEquals

class MannWhitneySuite extends FunSuite {

  private val E = 0.000001

  test("returns expected values") {
    val params = MannWhitneyParams.builder.confidenceLevel(0.95).controlData(Array[Double](1.0, 2.0, 3.0, 4.0)).experimentData(Array[Double](10.0, 20.0, 30.0, 40.0)).mu(0).build
    val mw = new MannWhitney
    val result = mw.eval(params)
    assertEquals(6.0, result.getConfidenceInterval.head, E)
    assertEquals(39.0, result.getConfidenceInterval.last, E)
    assertEquals(22.5, result.getEstimate, E)
    assertEquals(0.02857142857142857, result.getPValue, E)
  }
}
