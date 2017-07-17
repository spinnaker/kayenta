package com.netflix.kayenta.judge.netflix.classifiers
import com.netflix.kayenta.judge.netflix.MetricPair

class MeanRatio() extends MetricClassifier{

  override def classify(metrics: MetricPair): MetricClassification = {
    MetricClassification(Pass, None, 1.0)
  }

}
