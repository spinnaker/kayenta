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

import com.netflix.kayenta.canary.results.CanaryJudgeResult
import com.netflix.kayenta.canary.{CanaryClassifierThresholdsConfig, CanaryConfig, CanaryJudge, CombinedCanaryResultStrategy}
import com.netflix.kayenta.metrics.MetricSetPair
import com.netflix.kayenta.r.{MannWhitney, MannWhitneyParams}
import org.apache.commons.math3.stat.StatUtils

import scala.collection.JavaConverters._

class NetflixJudge extends CanaryJudge {

  //Open a connection with RServe for the Mann-Whitney U Test
  val mw = new MannWhitney()

  override def judge(canaryConfig: CanaryConfig, metricSetPairList: util.List[MetricSetPair]): CanaryJudgeResult = {

    val result = CanaryJudgeResult.builder().build()

    //todo: check to ensure the data is aligned (i.e., the same length)

    for(metric <- metricSetPairList.asScala){

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
      val transformedMetricPair = removeNaNs(metricPair)

      //Remove Anomalies
      val cleanedMetricPair = removeAnomalies(transformedMetricPair)

      //=============================================
      // Metric Statistics
      // ============================================
      val experimentStats = calculateStatistics(cleanedMetricPair.experiment.values)
      val controlStats = calculateStatistics(cleanedMetricPair.control.values)

      //=============================================
      // Metric Classification
      // ============================================
      //Use the Mann-Whitney MCA Algorithm to compare the experiment and control populations
      val metricClassification = mcaMannWhitney(cleanedMetricPair)

    }

    //Disconnect from RServe
    mw.disconnect()

    result
  }

  /** Metric Comparison Algorithm (MCA)
    *
    * Uses the Mann-Whitney U test to compare metrics from the canary and control groups
    */
  def mcaMannWhitney(metricPair: MetricPair, fraction: Double = 0.25): MetricClassification ={

    //todo(csanden): classification label should not be a string

    val experiment = metricPair.experiment
    val control = metricPair.control

    //Check if the experiment and control data are equal
    if (experiment.values.sameElements(control.values)){
      MetricClassification(Pass, None, 0.0)
    }

    //Perform Mann-Whitney U Test
    val mwResult = MannWhitneyUTest(experiment.values, control.values)

    //todo(csanden): use the Hodge Lehmann estimate
    val delta = math.abs(StatUtils.mean(experiment.values) - StatUtils.mean(control.values))
    val criticalValue = fraction * delta

    val ratio = StatUtils.mean(experiment.values)/StatUtils.mean(control.values)

    val lowerBound = -1 * criticalValue
    val upperBound = criticalValue

    //todo(csanden): improve the reason
    if(mwResult.lowerConfidence > upperBound){
      val reason = "The metric was classified as High ..."
      return MetricClassification(High, Some(reason), ratio)

    }else if(mwResult.upperConfidence < lowerBound){
      val reason = "The metric was classified as Low"
      return MetricClassification(Low, Some(reason), ratio)
    }

    MetricClassification(Pass, None, ratio)

  }


  /**
    * Mann-Whitney U Test
    *
    * An implementation of the Mann-Whitney U test (also called Wilcoxon rank-sum test).
    */
  def MannWhitneyUTest(experimentValues: Array[Double], controlValues: Array[Double]): MannWhitneyResult ={

    val params = MannWhitneyParams.builder()
      .mu(0)
      .confidenceLevel(0.99)
      .controlData(controlValues)
      .experimentData(experimentValues)
      .build()

    val mwResult = mw.eval(params)
    val confInterval = mwResult.getConfidenceInterval
    val pValue = mwResult.getPValue
    val estimate = mwResult.getEstimate

    MannWhitneyResult(pValue, confInterval(0), confInterval(1), estimate)
  }

  def calculateStatistics(data: Array[Double]): Unit ={

    //todo: before calculating statistics, check for empty array

    //Calculate summary statistics
    val mean = StatUtils.mean(data)
    val median = StatUtils.percentile(data, 50)
    val min = StatUtils.min(data)
    val max = StatUtils.max(data)
    val count = data.length

    MetricStatistics(min, max, mean, median, count)
  }

  def removeAnomalies(metricPair: MetricPair): MetricPair ={

    val experiment = metricPair.experiment
    val control = metricPair.control

    //Remove Anomalous Values
    val transformedExperiment = removeAnomalousValues(experiment.values)
    val transformedControl = removeAnomalousValues(control.values)

    val transformedExperimentMetric = Metric(experiment.name, transformedExperiment, label = experiment.label)
    val transformedControlMetric = Metric(control.name, transformedControl, label = control.label)

    MetricPair(transformedExperimentMetric, transformedControlMetric)
  }

  def removeNaNs(metricPair: MetricPair): MetricPair ={

    val experiment = metricPair.experiment
    val control = metricPair.control

    //Remove NaN Values
    val transformedExperiment = removeNaNValues(experiment.values)
    val transformedControl = removeNaNValues(control.values)

    val transformedExperimentMetric = Metric(experiment.name, transformedExperiment, label = experiment.label)
    val transformedControlMetric = Metric(control.name, transformedControl, label = control.label)

    MetricPair(transformedExperimentMetric, transformedControlMetric)
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

  def removeNaNValues(values: Array[Double]): Array[Double] ={
    values.filter(x => !x.isNaN)
  }

  def removeAnomalousValues(values: Array[Double]): Array[Double] ={
    val (lower, upper) = iqr(values)
    values.filter(x => x >= lower && x <= upper)
  }

  def iqr(values: Array[Double], multiplier: Double = 3.0): (Double, Double) ={
    // Calculate the interquartile range (IQR)
    // To reduce sensitivity, take the max of the IQR or the 99th percentile

    //Calculate the 25th and 75th percentiles
    val p75 = StatUtils.percentile(values, 75)
    val p25 = StatUtils.percentile(values, 25)

    //Calculate the 1st and 99th percentiles
    val p01 = StatUtils.percentile(values, 1)
    val p99 = StatUtils.percentile(values, 99)

    //Calculate the Interquartile Range (IQR)
    val iqr = p75-p25

    //Calculate the lower fence
    val lowerIQR = p25 - (multiplier * iqr)
    val lowerFence = math.min(p01, lowerIQR)

    //Calculate the upper fence
    val upperIQR = p75 + (multiplier * iqr)
    val upperFence = math.max(p99, upperIQR)

    (lowerFence, upperFence)
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