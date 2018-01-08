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

package com.netflix.kayenta.judge.evaluation

import com.netflix.kayenta.judge.Metric
import com.netflix.kayenta.judge.classifiers.metric._
import com.netflix.kayenta.r.MannWhitney

import scala.util.Random

/**
  * Class that represents an instance of data and truth labels.
  * @param experiment experiment metric
  * @param control control metric
  * @param truth ground truth label
  */
case class LabeledInstance(experiment: Metric, control: Metric, truth: MetricClassificationLabel)


/**
  * Evaluation - Evaluation functionality.
  */
object Evaluate {

  /**
    *
    * @param truth
    * @param predictions
    * @return
    */
  def calculateMetrics(truth: Array[Int], predictions: Array[Int]): Map[String, Double] ={
    require(predictions.length == truth.length, "the prediction vector and truth vector must be the same size")

    //Calculate the evaluation metrics
    val precision = Metrics.precision(truth, predictions)
    val recall = Metrics.recall(truth, predictions)
    val f1 = Metrics.fMeasure(truth, predictions)
    val acc = Metrics.accuracy(truth, predictions)

    //Return a default value of -1.0
    Map(("Precision", precision), ("Recall", recall), ("FMeasure", f1), ("Accuracy", acc)).withDefaultValue(-1.0)
  }

  /**
    *
    * @param truth
    * @return
    */
  def convertLabel(truth: MetricClassificationLabel): Int ={
    truth match {
      case High => 1
      case Low => 1
      case Nodata => 0
      case Pass => 0
    }
  }

  /**
    * Evaluate - evaluate a metric classification algorithm
    * @param classifier metric classification algorithm
    * @param dataset input data set with labels to evaluate
    */
  def evaluate[T <: BaseMetricClassifier](classifier: T, dataset: List[LabeledInstance]): Map[String, Double] ={

    val truth = dataset.map(x => convertLabel(x.truth))
    val predictions = dataset.map{
      instance => convertLabel(classifier.classify(instance.control, instance.experiment, MetricDirection.Either).classification)
    }

    val result = calculateMetrics(truth.toArray, predictions.toArray)

    result
  }

}

object Test extends App{

  //Connect to RServe to perform the Mann-Whitney U Test
  val mw = new MannWhitney()
  val mannWhitney = new MannWhitneyClassifier(fraction = 0.25, confLevel = 0.98, mw)

  trait Distribution[A] {
    def get: A
    def sample(n: Int): List[A] = {
      List.fill(n)(this.get)
    }
  }

  def normal[B](mean: Double, stdev: Double): Distribution[Double] = new Distribution[Double] {
    override def get: Double = Random.nextGaussian() * stdev + mean
  }

  def getPassInstance(numSamples: Int): LabeledInstance = {
    val experiment = normal(25, 1).sample(numSamples)
    val control = normal(25, 1).sample(numSamples)

    val experimentMetric = Metric("metric", experiment.toArray, "canary")
    val controlMetric = Metric("metric", control.toArray, "baseline")

    LabeledInstance(experimentMetric, controlMetric, Pass)
  }

  def getFailInstance(numSamples: Int): LabeledInstance = {
    val experiment = normal(26, 1).sample(numSamples)
    val control = normal(25, 1).sample(numSamples)

    val experimentMetric = Metric("metric", experiment.toArray, "canary")
    val controlMetric = Metric("metric", control.toArray, "baseline")

    LabeledInstance(experimentMetric, controlMetric, High)
  }

  def getInstances(numInstance: Int, numSamples: Int): List[LabeledInstance] ={
    List.fill(numInstance)(getPassInstance(numSamples)) ++ List.fill(numInstance)(getFailInstance(numSamples))
  }

  val dataset = getInstances(1000, 60)
  val evaluation = Evaluate.evaluate(mannWhitney, dataset)

  evaluation

  mw.disconnect()

}



