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

import org.ddahl.rscala.RClient

class MannWhitneyR {

  def eval(params: MannWhitneyParams): MannWhitneyResult = synchronized {
    val R = RClient()
    R.set("experimentData", params.getExperimentData)
    R.set("controlData", params.getControlData)

    R.eval(
      """
        |res <- wilcox.test(experimentData, controlData,conf.int=TRUE,mu=%s,conf.level=%s)
        |pval <- res$p.value
        |lcint <- res$conf.int[1]
        |hcint <- res$conf.int[2]
        |est <- res$estimate
        |""".stripMargin
      .format(params.getMu.toString, params.getConfidenceLevel.toString))

    MannWhitneyResult.builder()
      .pValue(R.get("pval")._1.asInstanceOf[Double])
      .confidenceInterval(Array(R.get("lcint")._1.asInstanceOf[Double], R.get("hcint")._1.asInstanceOf[Double]))
      .estimate(R.get("est")._1.asInstanceOf[Double])
      .build()
  }
}

