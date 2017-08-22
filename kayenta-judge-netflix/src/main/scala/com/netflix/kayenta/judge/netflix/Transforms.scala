package com.netflix.kayenta.judge.netflix

import com.netflix.kayenta.judge.netflix.detectors.OutlierDetector


object Transforms {

  /**
    * Remove NaN values from the input array
    * @param data
    * @return
    */
  def removeNaNs(data: Array[Double]): Array[Double] ={
    data.filter(x => !x.isNaN)
  }

  /**
    * Remove NaN values from the input metric
    * @param metric
    */
  def removeNaNs(metric: Metric): Metric = {
    metric.copy(values = removeNaNs(metric.values))
  }

  /**
    * Remove NaN values from the input metric pair
    * @param metrics
    * @return
    */
  def removeNaNs(metrics: MetricPair): MetricPair = {
    metrics.copy(experiment = removeNaNs(metrics.experiment), control = removeNaNs(metrics.control))
  }

  /**
    * Replace NaN values from the input array
    * @param data
    * @param value
    * @return
    */
  def replaceNaNs(data: Array[Double], value: Double): Array[Double]={
    data.map(x => if(x.isNaN) value else x)
  }

  /**
    * Remove outliers from the input array
    * @param data
    * @param detector
    */
  def removeOutliers(data: Array[Double], detector: OutlierDetector): Array[Double] ={
    val outliers = detector.detect(data)
    data.zip(outliers).collect{case (v, false) => v}
  }

  /**
    * Remove outliers from the input metric
    * @param metric
    * @param detector
    * @return
    */
  def removeOutliers(metric: Metric, detector: OutlierDetector): Metric = {
    metric.copy(values = removeOutliers(metric.values, detector))
  }

  /**
    * Remove outliers from the input metric pair
    * @param metrics
    * @param detector
    * @return
    */
  def removeOutliers(metrics: MetricPair,  detector: OutlierDetector): MetricPair = {
    metrics.copy(
      experiment = removeOutliers(metrics.experiment, detector),
      control = removeOutliers(metrics.control, detector)
    )
  }


}
