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

package com.netflix.kayenta.judge.netflix

import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.base.Charsets
import com.netflix.kayenta.canary.{CanaryClassifierThresholdsConfig, CanaryConfig, CombinedCanaryResultStrategy}
import com.netflix.kayenta.metrics.{MetricSet, MetricSetPair}
import org.apache.commons.io.IOUtils
import org.scalatest.FunSuite
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader
import org.springframework.test.context.ContextConfiguration

import scala.collection.JavaConverters._

@Configuration
class TestConfig {}

@ContextConfiguration(classes = Array(classOf[TestConfig]))
class JudgeSuite extends FunSuite with TestContextManagement {
  @Autowired
  private val resourceLoader: ResourceLoader = null

  private val objectMapper = new ObjectMapper()
    .setSerializationInclusion(NON_NULL)
    .disable(FAIL_ON_UNKNOWN_PROPERTIES)

  private def getFileContent(filename: String) = try {
    val inputStream = resourceLoader.getResource("classpath:" + filename).getInputStream
    try
      IOUtils.toString(inputStream, Charsets.UTF_8.name)
    finally if (inputStream != null)
      inputStream.close()
  }

  private def getConfig(filename: String) = {
    val contents = getFileContent(filename)
    objectMapper.readValue(contents, classOf[CanaryConfig])
  }

  private def getMetricSet(filename: String) = {
    val contents = getFileContent(filename)
    objectMapper.readValue(contents, classOf[MetricSet])
  }

  test("Judge Integration Test"){
    val judge = new NetflixJudge()
    val config = getConfig("test-config.json")
    val experimentValues = List[java.lang.Double](10.0,20.0,30.0,40.0).asJava
    val controlValues = List[java.lang.Double](1.0,2.0,3.0,4.0).asJava

    val resultStrategy = CombinedCanaryResultStrategy.AVERAGE
    val thresholds = CanaryClassifierThresholdsConfig.builder().marginal(75.0).pass(95.0).build()

    val cpuMetric = MetricSetPair.builder()
      .name("cpu")
      .tags(Map("x"->"1").asJava)
      .value("control", controlValues)
      .value("experiment", experimentValues)
      .build()

    val requestMetric = MetricSetPair.builder()
      .name("requests")
      .tags(Map("y"->"1").asJava)
      .value("control", controlValues)
      .value("experiment", experimentValues)
      .build()

    val noDataMetric = MetricSetPair.builder()
      .name("foo")
      .tags(Map("z"->"1").asJava)
      .value("control", List[java.lang.Double]().asJava)
      .value("experiment", List[java.lang.Double]().asJava)
      .build()

    val metricPairs = List(cpuMetric, requestMetric, noDataMetric).asJava
    val result = judge.judge(config, resultStrategy, thresholds, metricPairs)

  }
}
