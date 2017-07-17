package com.netflix.kayenta.judge.netflix.detectors

abstract class OutlierDetector{

  /**
    * Determine which data points are outliers
    * @param data array of samples
    * @return boolean array indicating which data points are anomalies
    */
  def detect(data: Array[Double]): Array[Boolean]

}