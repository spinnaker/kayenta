package com.netflix.kayenta.judge.netflix.classifiers.metric

import com.netflix.kayenta.judge.netflix.MetricPair

sealed trait MetricClassificationLabel
case object Pass extends MetricClassificationLabel
case object High extends MetricClassificationLabel
case object Low extends MetricClassificationLabel
case object Nodata extends MetricClassificationLabel
case object Error extends MetricClassificationLabel

//todo (csanden): report deviation instead of ratio?
case class MetricClassification(classification: MetricClassificationLabel, reason: Option[String], ratio: Double)

abstract class BaseMetricClassifier {
  def classify(metrics: MetricPair): MetricClassification
}
