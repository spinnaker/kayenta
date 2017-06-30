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
import com.netflix.kayenta.metrics.MetricSet
import org.apache.commons.io.IOUtils
import org.scalatest.FunSuite
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader
import org.springframework.test.context.ContextConfiguration

@Configuration
class TestConfig {}

@ContextConfiguration(classes = Array(classOf[TestConfig]))
class NetflixJudgeSuite extends FunSuite with TestContextManagement {
  @Autowired
  private val resourceLoader: ResourceLoader = _

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

  test("Load object, confirm output matches") {
    val item = getConfig("foo.json")
  }
}
