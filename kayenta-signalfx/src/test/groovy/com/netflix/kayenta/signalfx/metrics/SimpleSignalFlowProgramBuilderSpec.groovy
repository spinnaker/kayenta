package com.netflix.kayenta.signalfx.metrics

import spock.lang.Shared
import spock.lang.Unroll

import java.time.Instant
import java.time.temporal.ChronoUnit

class SimpleSignalFlowProgramBuilderSpec {

  @Shared
  SignalFxQueryBuilderService signalFxQueryBuilderService = new SignalFxQueryBuilderService();

  @Shared
  Instant start = Instant.now()

  @Shared
  Instant end = start.plus(60, ChronoUnit.MINUTES)

  @Shared
  Map<String, String> templates = [:]

  @Shared
  def testCases = [
    [
      description: 'no location key is provided'
    ]
  ]

  @Unroll
  void "When #useCase then the SignalFxQueryBuilderService generates the expected query"() {
    given:

    when:

    then:

    where:
    [
      useCase
    ] << testCases.collect { testCase ->
      return [
        testCase.description
      ]
    }
  }

}
