package com.netflix.kayenta.judge.netflix.classifiers

import com.netflix.kayenta.judge.netflix.MetricPair

//todo(csanden): define string name
sealed trait ClassificationLabel
case object Pass extends ClassificationLabel
case object High extends ClassificationLabel
case object Low extends ClassificationLabel
case object Error extends ClassificationLabel

//todo (csanden): report deviation instead of ratio?
case class MetricClassification(classification: ClassificationLabel, reason: Option[String], ratio: Double)

abstract class MetricClassifier {

  def classify(metrics: MetricPair): MetricClassification
}
