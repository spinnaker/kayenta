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

import java.util

import com.netflix.kayenta.canary.{CanaryClassifierThresholdsConfig, CanaryConfig, CanaryJudge}
import com.netflix.kayenta.canary.results.{CanaryAnalysisResult, CanaryJudgeMetricClassification, CanaryJudgeResult}
import com.netflix.kayenta.judge.netflix.classifiers.MannWhitneyClassifier
import com.netflix.kayenta.judge.netflix.detectors.IQRDetector
import com.netflix.kayenta.metrics.MetricSetPair
import com.netflix.kayenta.r.MannWhitney
import org.apache.commons.math3.stat.StatUtils

import scala.collection.JavaConverters._

class NetflixJudge extends CanaryJudge {

  val mw = new MannWhitney()

  override def judge(canaryConfig: CanaryConfig, orchestratorScoreThresholds: CanaryClassifierThresholdsConfig, metricSetPairList: util.List[MetricSetPair]): CanaryJudgeResult = {

    val result = CanaryJudgeResult.builder().build()

    //Metric Classification
    val metricResults = metricSetPairList.asScala.toList.map{ metricPair => classifyMetric(canaryConfig, metricPair)}

    //Metric Group Scoring
    val groupResults = calculateGroupScore(canaryConfig, metricResults)

    //Disconnect from RServe
    mw.disconnect()

    result
  }

  /**
    * Metric Classification
    * @param canaryConfig
    * @param metric
    * @return
    */
  def classifyMetric(canaryConfig: CanaryConfig, metric: MetricSetPair): CanaryAnalysisResult ={

    val metricConfig = canaryConfig.getMetrics.asScala.find(m => m.getName == metric.getName) match {
      case Some(config) => config
      case None => throw new IllegalArgumentException(s"Could not find metric config for ${metric.getName}")
    }

    val experimentValues = metric.getValues.get("experiment").asScala.map(_.toDouble).toArray
    val controlValues = metric.getValues.get("control").asScala.map(_.toDouble).toArray

    val experiment = Metric(metric.getName, experimentValues, label = "canary")
    val control = Metric(metric.getName, controlValues, label = "baseline")
    val metricPair = MetricPair(experiment, control)

    val validateNoData = checkNoData(metricPair)
    val validateAllNaNs = checkAllNaNs(metricPair)

    //=============================================
    // Metric Validation
    // ============================================
    //Validate the input data for empty data arrays
    if (!validateNoData.valid){
      //todo: return metric result
      //validateNoData.reason
    }

    //Validate the input data and check for all NaN values
    if (!validateAllNaNs.valid){
      //todo: return metric result
      //validateNoData.reason
    }

    //=============================================
    // Metric Transformation
    // ============================================
    //Remove NaN Values
    val transformedMetricPair = Transforms.removeNaNs(metricPair)

    //Remove Outliers
    val detector = new IQRDetector(factor = 3.0, reduceSensitivity = true)
    val cleanedMetricPair = Transforms.removeOutliers(transformedMetricPair, detector)

    //=============================================
    // Metric Statistics
    // ============================================
    val experimentStats = calculateStatistics(cleanedMetricPair.experiment)
    val controlStats = calculateStatistics(cleanedMetricPair.control)

    //=============================================
    // Metric Classification
    // ============================================
    //Use the Mann-Whitney MCA Algorithm to compare the experiment and control populations
    val mannWhitney = new MannWhitneyClassifier(fraction = 0.25, confLevel = 0.99, mw)
    val metricClassification = mannWhitney.classify(cleanedMetricPair)

    //Construct metric classification
    val classification = CanaryJudgeMetricClassification.builder()
      .classification(metricClassification.classification.toString)
      .classificationReason(metricClassification.reason.orNull)
      .build()

    //Construct metric result
    CanaryAnalysisResult.builder()
      .name(metric.getName)
      .tags(metric.getTags)
      .classification(classification)
      .groups(metricConfig.getGroups)
      .experimentMetadata(Map("stats" -> experimentStats.asInstanceOf[Object]).asJava)
      .controlMetadata(Map("stats" -> controlStats.asInstanceOf[Object]).asJava)
      .resultMetadata(Map("ratio" -> metricClassification.ratio.asInstanceOf[Object]).asJava)
      .build()
  }

  /**
    * Metric Group Scoring
    * @param config
    * @param metricResults
    */
  def calculateGroupScore(config: CanaryConfig, metricResults: List[CanaryAnalysisResult]): Unit ={

  }

  def calculateStatistics(metric: Metric): MetricStatistics ={

    //todo: before calculating statistics, check for empty array

    //Calculate summary statistics
    val mean = StatUtils.mean(metric.values)
    val median = StatUtils.percentile(metric.values, 50)
    val min = StatUtils.min(metric.values)
    val max = StatUtils.max(metric.values)
    val count = metric.values.length

    MetricStatistics(min, max, mean, median, count)
  }

  def checkNoData(metricPair: MetricPair): ValidationResult ={
    //todo (csanden): use the metric label instead of hard coding the names

    val experiment = metricPair.experiment
    val control = metricPair.control

    if(experiment.values.isEmpty && control.values.isEmpty){
      val reason = "Empty data array for Baseline and Canary"
      return ValidationResult(valid=false, reason=Some(reason))

    }else if(experiment.values.isEmpty){
      val reason = "Empty data array for Canary"
      return ValidationResult(valid=false, reason=Some(reason))

    } else if(control.values.isEmpty) {
      val reason = "Empty data array for Baseline"
      return ValidationResult(valid = false, reason = Some(reason))
    }

    ValidationResult(valid=true)

  }

  //todo(csanden): fix return statements
  def checkAllNaNs(metricPair: MetricPair): ValidationResult = {
    //todo: refactor out the check for all NaNs

    val experiment = metricPair.experiment
    val control = metricPair.control

    val experimentAllNaNs = experiment.values.forall(_.isNaN)
    val controlAllNaNs = control.values.forall(_.isNaN)

    if (experimentAllNaNs && controlAllNaNs){
      val reason = "No data for Canary and Baseline"
      ValidationResult(valid=false, reason=Some(reason))

    }else if (controlAllNaNs){
      val reason = "No data for Baseline"
      ValidationResult(valid=false, reason=Some(reason))

    }else if (experimentAllNaNs){
      val reason = "No data for Canary"
      ValidationResult(valid=false, reason=Some(reason))

    }else {
      ValidationResult(valid=true)
    }
  }


}

case class Metric(name: String, values: Array[Double], label: String)

case class MetricPair(experiment: Metric, control: Metric)

case class ValidationResult(valid: Boolean, reason: Option[String]=None)

case class MetricStatistics(min: Double, max: Double, mean: Double, median: Double, count: Int)

case class MannWhitneyResult(pValue: Double, lowerConfidence: Double, upperConfidence: Double, estimate: Double)

//todo(csanden): rename?
//todo (csanden): report deviation instead of ratio?
case class MetricClassification(classification: ClassificationLabel, reason: Option[String], ratio: Double)

//todo(csanden): define string name
sealed trait ClassificationLabel
case object Pass extends ClassificationLabel
case object High extends ClassificationLabel
case object Low extends ClassificationLabel
case object Error extends ClassificationLabel