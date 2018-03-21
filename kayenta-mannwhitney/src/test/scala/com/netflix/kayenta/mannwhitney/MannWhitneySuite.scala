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

  //todo: Remove...  temporary dummy test for adhoc tie-outs during development
  import org.ddahl.rscala.RClient
  test("foo"){
    val params = MannWhitneyParams(mu = 0.0, confidenceLevel = 0.95, controlData = Array(1.0, 2.0, 3.0, 4.0), experimentData = Array(10.0, 20.0, 30.0, 40.0))
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
}
