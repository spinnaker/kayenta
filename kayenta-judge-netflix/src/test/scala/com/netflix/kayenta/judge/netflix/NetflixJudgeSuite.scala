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
import com.netflix.kayenta.judge.netflix.detectors.{IQRDetector, KSigmaDetector}
import com.netflix.kayenta.judge.netflix.Transforms.{removeNaNs, removeOutliers}
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

  test("Judge Integration Test"){
    val judge = new NetflixJudge()

    val config = getConfig("test-config.json")
    val tags = Map("x"->"x").asJava
    val experimentValues = List[java.lang.Double](10.0,20.0,30.0,40.0).asJava
    val controlValues = List[java.lang.Double](1.0,2.0,3.0,4.0).asJava

    val metricPair = MetricSetPair.builder()
      .name("cpu")
      .tags(tags)
      .value("control", controlValues)
      .value("experiment", experimentValues)
      .build()

    val metricPairs = List(metricPair).asJava
    val result = judge.judge(config, metricPairs)

  }

  test("KSigma Detection"){

    val testData = Array(1.0, 1.0, 1.0, 1.0, 1.0, 20.0, 1.0, 1.0, 1.0, 1.0, 1.0)
    val truth = Array(false, false, false, false, false, true, false, false, false, false, false)

    val detector = new KSigmaDetector(k = 3.0)
    val result = detector.detect(testData)
    assert(result === truth)
  }

  test("KSigma Two Sided"){

    val testData = Array(1.0, 1.0, 1.0, 5.0, -1.0, -1.0, -1.0, -5.0)
    val truth = Array(false, false, false, true, false, false, false, true)

    val detector = new KSigmaDetector(1.0)
    val result = detector.detect(testData)
    assert(result === truth)
  }

  test("IQR Detection"){

    val testData = Array(21.0, 23.0, 24.0, 25.0, 50.0, 29.0, 23.0, 21.0)
    val truth = Array(false, false, false, false, true, false, false, false)

    val detector = new IQRDetector(factor = 1.5)
    val result = detector.detect(testData)
    assert(result === truth)
  }

  test("IQR Detect Two Sided"){

    val testData = Array(1.0, 1.0, 1.0, 5.0, -1.0, -1.0, -1.0, -5.0)
    val truth = Array(false, false, false, true, false, false, false, true)

    val detector = new IQRDetector(factor = 1.5)
    val result = detector.detect(testData)
    assert(result === truth)
  }

  test("IQR Reduce Sensitivity"){

    val testData = Array(1.0, 1.0, 1.0, 1.0, 1.0, 20.0, 1.0, 1.0, 1.0, 1.0, 1.0)
    val truth = Array(false, false, false, false, false, false, false, false, false, false, false)

    val detector = new IQRDetector(factor = 3.0, reduceSensitivity=true)
    val result = detector.detect(testData)
    assert(result === truth)
  }

  test("IQR Empty Data"){

    val testData = Array[Double]()
    val truth = Array[Boolean]()

    val detector = new IQRDetector(factor = 1.5)
    val result = detector.detect(testData)
    assert(result === truth)
  }

  test("NaN Removal Single"){

    val testData = Array(0.0, 1.0, Double.NaN, 1.0, 0.0)
    val truth = Array(0.0, 1.0, 1.0, 0.0)

    val result = Transforms.removeNaNs(testData)
    assert(result === truth)
  }

  test("NaN Remove Multiple"){

    val testData = Array(Double.NaN, Double.NaN, Double.NaN)
    val truth = Array[Double]()

    val result = removeNaNs(testData)
    assert(result === truth)
  }

  test("IQR Outlier Removal"){

    val testData = Array(21.0, 23.0, 24.0, 25.0, 50.0, 29.0, 23.0, 21.0)
    val truth = Array(21.0, 23.0, 24.0, 25.0, 29.0, 23.0, 21.0)

    val detector = new IQRDetector(factor = 1.5)
    val result = removeOutliers(testData, detector)
    assert(result === truth)
  }

  test("KSigma Outlier Removal"){

    val testData = Array(1.0, 1.0, 1.0, 1.0, 1.0, 20.0, 1.0, 1.0, 1.0, 1.0, 1.0)
    val truth = Array(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0)

    val detector = new KSigmaDetector(k = 3.0)
    val result = removeOutliers(testData, detector)
    assert(result === truth)
  }



}
