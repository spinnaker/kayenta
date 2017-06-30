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
import com.netflix.kayenta.canary.CanaryConfig
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
class NetflixJudgeSuite extends FunSuite with TestContextManagement {
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

  test("Interquartile Range Test"){
    val judge = new NetflixJudge()
    val inputData = Array(1.0, 1.0, 1.0, 1.0, 1.0, 20.0, 1.0, 1.0, 1.0, 1.0, 1.0)
    val result = judge.iqr(inputData)
    val expected = (1.0, 20.0)

    assert(expected === result)
  }

  test("Interquartile Range Empty Array Test"){
    val judge = new NetflixJudge()
    val inputData = Array[Double]()
    val (lower, upper) = judge.iqr(inputData)

    assert(lower.isNaN)
    assert(upper.isNaN)
  }

  test("Judge Integration Test"){
    val judge = new NetflixJudge()

    val config = CanaryConfig.builder().build()
    val tags = Map("x"->"x").asJava
    val experimentValues = List[java.lang.Double](10.0,20.0,30.0,40.0).asJava
    val controlValues = List[java.lang.Double](1.0,2.0,3.0,4.0).asJava

    val metricPair = MetricSetPair.builder()
      .name("test-metric")
      .tags(tags)
      .value("control", controlValues)
      .value("experiment", experimentValues)
      .build()

    val metricPairs = List(metricPair).asJava
    val result = judge.judge(config, metricPairs)

  }


}
