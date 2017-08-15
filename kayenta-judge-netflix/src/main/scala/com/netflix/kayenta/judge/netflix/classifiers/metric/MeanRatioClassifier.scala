package com.netflix.kayenta.judge.netflix.classifiers.metric

import com.netflix.kayenta.judge.netflix.MetricPair

class MeanRatioClassifier() extends BaseMetricClassifier{

  override def classify(metrics: MetricPair): MetricClassification = {
    MetricClassification(Pass, None, 1.0)
  }

}
