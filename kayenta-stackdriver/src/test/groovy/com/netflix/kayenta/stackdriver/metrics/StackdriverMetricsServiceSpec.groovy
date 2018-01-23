/*
 * Copyright 2017 Google, Inc.
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

package com.netflix.kayenta.stackdriver.metrics

import com.netflix.kayenta.canary.CanaryConfig
import com.netflix.kayenta.canary.providers.StackdriverCanaryMetricSetQueryConfig
import com.netflix.kayenta.stackdriver.canary.StackdriverCanaryScope
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

class StackdriverMetricsServiceSpec extends Specification {

  @Subject
  StackdriverMetricsService stackdriverMetricsService = StackdriverMetricsService.builder().build()

  @Unroll
  void "Referenced template #customFilterTemplate expands properly"() {
    given:
    CanaryConfig canaryConfig = CanaryConfig.builder().templates(templates).build()
    StackdriverCanaryMetricSetQueryConfig stackdriverCanaryMetricSetQueryConfig =
      StackdriverCanaryMetricSetQueryConfig.builder().customFilterTemplate(customFilterTemplate).build()
    StackdriverCanaryScope stackdriverCanaryScope = new StackdriverCanaryScope(extendedScopeParams: scopeParams)

    expect:
    stackdriverMetricsService.expandCustomFilter(canaryConfig, stackdriverCanaryMetricSetQueryConfig, stackdriverCanaryScope) == expectedExpandedTemplate

    where:
    templates                                             | customFilterTemplate | scopeParams       || expectedExpandedTemplate
    ["my-template": 'A test: key1=${key1}.']              | "my-template"        | [key1: "value-1"] || "A test: key1=value-1."
    ["my-template-1": 'A test: key1=${key1}.',
     "my-template-2": 'A test: key2=${key2}.']            | "my-template-2"      | [key2: "value-2"] || "A test: key2=value-2."
    ["my-template-1": 'A test: key1=${key1}.',
     "my-template-2": 'A test: key2=${key2}.']            | "my-template-1"      | [key1: "value-1"] || "A test: key1=value-1."
    ["my-template": 'A test: key1=${key1} key2=${key2}.'] | "my-template"        | [key1: "value-1",
                                                                                    key2: "value-2"] || "A test: key1=value-1 key2=value-2."
    ["my-template": 'A test: key1=something1.']           | "my-template"        | null              || "A test: key1=something1."
    ["my-template": 'A test: key1=something1.']           | "my-template"        | [:]               || "A test: key1=something1."
  }

  @Unroll
  void "Custom filter takes precedence over custom filter template #customFilterTemplate"() {
    given:
    CanaryConfig canaryConfig = CanaryConfig.builder().templates(templates).build()
    StackdriverCanaryMetricSetQueryConfig stackdriverCanaryMetricSetQueryConfig =
      StackdriverCanaryMetricSetQueryConfig.builder().customFilterTemplate(customFilterTemplate).customFilter(customFilter).build()
    StackdriverCanaryScope stackdriverCanaryScope = new StackdriverCanaryScope(extendedScopeParams: scopeParams)

    expect:
    stackdriverMetricsService.expandCustomFilter(canaryConfig, stackdriverCanaryMetricSetQueryConfig, stackdriverCanaryScope) == expectedExpandedTemplate

    where:
    templates                                             | customFilterTemplate | customFilter          | scopeParams       || expectedExpandedTemplate
    ["my-template": 'A test: key1=${key1}.']              | "my-template"        | "An explicit filter." | [key1: "value-1"] || "An explicit filter."
    ["my-template-1": 'A test: key1=${key1}.',
     "my-template-2": 'A test: key2=${key2}.']            | "my-template-2"      | "An explicit filter." | [key2: "value-2"] || "An explicit filter."
    ["my-template-1": 'A test: key1=${key1}.',
     "my-template-2": 'A test: key2=${key2}.']            | "my-template-1"      | "An explicit filter." | [key1: "value-1"] || "An explicit filter."
    ["my-template": 'A test: key1=${key1} key2=${key2}.'] | "my-template"        | "An explicit filter." | [key1: "value-1",
                                                                                                            key2: "value-2"] || "An explicit filter."
    ["my-template": 'A test: key1=something1.']           | "my-template"        | "An explicit filter." | null              || "An explicit filter."
    ["my-template": 'A test: key1=something1.']           | "my-template"        | "An explicit filter." | [:]               || "An explicit filter."
  }

  @Unroll
  void "Missing template, no templates, or missing variable all throw exceptions"() {
    given:
    CanaryConfig canaryConfig = CanaryConfig.builder().templates(templates).build()
    StackdriverCanaryMetricSetQueryConfig stackdriverCanaryMetricSetQueryConfig =
      StackdriverCanaryMetricSetQueryConfig.builder().customFilterTemplate(customFilterTemplate).build()
    StackdriverCanaryScope stackdriverCanaryScope = new StackdriverCanaryScope(extendedScopeParams: scopeParams)

    when:
    stackdriverMetricsService.expandCustomFilter(canaryConfig, stackdriverCanaryMetricSetQueryConfig, stackdriverCanaryScope)

    then:
    thrown IllegalArgumentException

    where:
    templates                                             | customFilterTemplate | scopeParams
    ["my-template-1": 'A test: key1=${key1}.',
     "my-template-2": 'A test: key2=${key2}.']            | "my-template-x"      | null
    [:]                                                   | "my-template-x"      | null
    null                                                  | "my-template-x"      | null
    ["my-template": 'A test: key1=${key1} key2=${key2}.'] | "my-template"        | [key3: "value-3",
                                                                                    key4: "value-4"]
  }

  @Unroll
  void "Can use predefined variables in custom filter template"() {
    given:
    CanaryConfig canaryConfig = CanaryConfig.builder().templates(templates).build()
    StackdriverCanaryMetricSetQueryConfig stackdriverCanaryMetricSetQueryConfig =
      StackdriverCanaryMetricSetQueryConfig.builder().customFilterTemplate(customFilterTemplate).build()
    StackdriverCanaryScope stackdriverCanaryScope = new StackdriverCanaryScope(scope: scope, extendedScopeParams: scopeParams)

    expect:
    stackdriverMetricsService.expandCustomFilter(canaryConfig, stackdriverCanaryMetricSetQueryConfig, stackdriverCanaryScope) == expectedExpandedTemplate

    where:
    templates                                                     | customFilterTemplate | scope            | scopeParams       || expectedExpandedTemplate
    ["my-template": 'A test: myGroupName=${scope} key1=${key1}.'] | "my-template"        | "myapp-dev-v001" | [key1: "value-1"] || "A test: myGroupName=myapp-dev-v001 key1=value-1."
    ["my-template": 'A test: myGroupName=${scope} key1=${key1}.'] | "my-template"        | "myapp-dev-v002" | [key1: "value-1"] || "A test: myGroupName=myapp-dev-v002 key1=value-1."
  }
}
