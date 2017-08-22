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

import com.netflix.kayenta.canary.results._
import com.netflix.kayenta.canary.{CanaryClassifierThresholdsConfig, CanaryConfig, CanaryJudge, CombinedCanaryResultStrategy}
import com.netflix.kayenta.judge.netflix.classifiers.metric.MannWhitneyClassifier
import com.netflix.kayenta.judge.netflix.classifiers.score.ThresholdScoreClassifier
import com.netflix.kayenta.judge.netflix.detectors.IQRDetector
import com.netflix.kayenta.judge.netflix.scorers.WeightedSumScorer
import com.netflix.kayenta.judge.netflix.stats.DescriptiveStatistics
import com.netflix.kayenta.metrics.MetricSetPair
import com.netflix.kayenta.r.MannWhitney
import org.springframework.stereotype.Component

import scala.collection.JavaConverters._

case class Metric(name: String, values: Array[Double], label: String)
case class MetricPair(experiment: Metric, control: Metric)

@Component
class NetflixJudge extends CanaryJudge {

  //Open a connection with RServe for the Mann-Whitney U Test
  private final val mw = new MannWhitney()
  private final val judgeName = "doom-v1.0"

  override def getName: String = judgeName

  override def judge(canaryConfig: CanaryConfig,
                     resultStrategy: CombinedCanaryResultStrategy,
                     scoreThresholds: CanaryClassifierThresholdsConfig,
                     metricSetPairList: util.List[MetricSetPair]): CanaryJudgeResult = {

    //Metric Classification
    val metricResults = metricSetPairList.asScala.toList.map{ metricPair => classifyMetric(canaryConfig, metricPair)}

    //Get the group weights from the canary configuration
    val groupWeights = Option(canaryConfig.getClassifier.getGroupWeights) match {
      case Some(groups) => groups.asScala.mapValues(_.toDouble).toMap
      case None => Map[String, Double]()
    }

    //Calculate the summary and group scores based on the metric results
    val weightedSumScorer = new WeightedSumScorer(groupWeights)
    val scores = weightedSumScorer.score(metricResults)

    //Classify the summary score
    val scoreClassifier = new ThresholdScoreClassifier(scoreThresholds.getPass, scoreThresholds.getMarginal)
    val scoreResult = scoreClassifier.classify(scores)

    //todo (csanden) CanaryJudgeGroupScore should define a numeric score
    //todo (csanden) CanaryJudgeGroupScore should define a weight
    //todo (csanden) Remove Group Classification and Reason
    val groupScores = scores.groupScores match {
      case Some(groups) => groups.map{ group =>
        CanaryJudgeGroupScore.builder()
          .name(group.name)
          .score(
            CanaryJudgeMetricClassification.builder()
              .classification("")
              .classificationReason("")
              .build())
          .build()
      }
      case None => List(CanaryJudgeGroupScore.builder().build())
    }

    //Disconnect from RServe
    mw.disconnect()

    val results = metricResults.map( metric => metric.getName -> metric).toMap.asJava
    val score = CanaryJudgeScore.builder()
        .score(scoreResult.score)
        .classification(scoreResult.classification.toString)
        .classificationReason(scoreResult.reason.getOrElse(""))
        .build()

    CanaryJudgeResult.builder()
        .score(score)
        .results(results)
        .groupScores(groupScores.asJava)
        .build()
  }

  /**
    * Metric Transformation
    * @param metricPair
    * @return
    */
  def transformMetric(metricPair: MetricPair): MetricPair ={
    //Remove NaN Values
    val transformedMetricPair = Transforms.removeNaNs(metricPair)

    //Remove Outliers
    val detector = new IQRDetector(factor = 3.0, reduceSensitivity = true)
    Transforms.removeOutliers(transformedMetricPair, detector)
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

    val experiment = Metric(metric.getName, experimentValues, label="canary")
    val control = Metric(metric.getName, controlValues, label="baseline")
    val metricPair = MetricPair(experiment, control)

    //=============================================
    // Metric Validation
    // ============================================
    //todo (csanden) Implement metric validation
    val validateNoData = Validators.checkNoData(metricPair)
    val validateAllNaNs = Validators.checkAllNaNs(metricPair)

    //=============================================
    // Metric Transformation
    // ============================================
    //Transform the metrics (remove NaN values, remove outliers, etc)
    val cleanedMetricPair = transformMetric(metricPair)

    //=============================================
    // Calculate metric statistics
    // ============================================
    //Calculate summary statistics such as mean, median, max, etc.
    val experimentStats = DescriptiveStatistics.summary(cleanedMetricPair.experiment)
    val controlStats = DescriptiveStatistics.summary(cleanedMetricPair.control)

    //=============================================
    // Metric Classification
    // ============================================
    //Use the Mann-Whitney algorithm to compare the experiment and control populations
    val mannWhitney = new MannWhitneyClassifier(fraction = 0.25, confLevel = 0.98, mw)
    val metricClassification = mannWhitney.classify(cleanedMetricPair)

    CanaryAnalysisResult.builder()
      .name(metric.getName)
      .tags(metric.getTags)
      .classification(metricClassification.classification.toString)
      .classificationReason(metricClassification.reason.orNull)
      .groups(metricConfig.getGroups)
      .experimentMetadata(Map("stats" -> experimentStats.asInstanceOf[Object]).asJava)
      .controlMetadata(Map("stats" -> controlStats.asInstanceOf[Object]).asJava)
      .resultMetadata(Map("ratio" -> metricClassification.ratio.asInstanceOf[Object]).asJava)
      .build()

  }

}