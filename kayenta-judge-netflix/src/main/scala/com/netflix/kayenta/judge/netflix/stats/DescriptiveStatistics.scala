package com.netflix.kayenta.judge.netflix.stats

import com.netflix.kayenta.judge.netflix.Metric
import org.apache.commons.math3.stat.StatUtils

case class MetricStatistics(min: Double, max: Double, mean: Double, median: Double, count: Int)

object DescriptiveStatistics {

  def mean(metric: Metric): Double = {
    StatUtils.mean(metric.values)
  }

  def median(metric: Metric): Double = {
    StatUtils.percentile(metric.values, 50)
  }

  def min(metric: Metric): Double = {
    StatUtils.min(metric.values)
  }

  def max(metric: Metric): Double = {
    StatUtils.max(metric.values)
  }

  def summary(metric: Metric): MetricStatistics = {
    //todo (csanden) validate input before calculating statistics
    val mean = this.mean(metric)
    val median = this.median(metric)
    val min = this.min(metric)
    val max = this.max(metric)
    val count = metric.values.length

    MetricStatistics(min, max, mean, median, count)
  }

}
