package com.netflix.kayenta.judge.netflix


case class ValidationResult(valid: Boolean, reason: Option[String]=None)

object Validators {

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

}
