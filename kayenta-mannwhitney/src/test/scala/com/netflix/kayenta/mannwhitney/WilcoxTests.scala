package com.netflix.kayenta.mannwhitney

import org.scalatest.{FunSuite, Matchers}
import org.scalactic.TolerantNumerics

class WilcoxTests extends FunSuite with Matchers {

  test("choose tests") {
    val epsilon = 0.00001
    val expected: Double = 21.0
    val result: Double = Wilcox.choose(21, 20)

    Math.abs(result - expected) should be < epsilon
  }

  test("test log analysis") {

    val epsilon = 1E-6
    val expected = -4.1108738641733114
    val n = 60
    val result = -Math.log(n + 1.0)

    Math.abs(expected - result) should be < epsilon
  }

  test("test lbeta") {

    val epsilon = 1E-6
    val expected = -43.422574590184574
    val n = 60
    val k = 30
    val result = Wilcox.lbeta(n - k + 1, k + 1)

    Math.abs(expected - result) should be < epsilon
  }

  test("test big p and big qs") {

    val epsilon = 1E-6
    val expected = 0.0040320832899632675
    val p = 31
    val q = 31
    val result = Wilcox.lgammacor(p) + Wilcox.lgammacor(q) - Wilcox.lgammacor(p + q)

    Math.abs(expected - result) should be < epsilon
  }

  test("test chebyshev") {

    val epsilon = 1E-6
    val input_x = -0.79188345473465138
    val input_n = 5
    val expected = 0.083330443684465765
    val result = Wilcox.chebyshev_eval(input_x, Wilcox.algmcs, input_n)

    Math.abs(expected - result) should be < epsilon
  }

  test("problem choose test") {
    val epsilon = 1E-4

    val expected01 = 155117520
    val result01 = Wilcox.choose(30, 15)

    Math.abs(expected01 - result01) should be < epsilon

    val expected = 1.1826458156486115E+17
    val result = Wilcox.choose(60, 30)

    Math.abs(expected - result) should be < epsilon
  }

  test("cwilson tests") {
    val epsilon = 0.000000001

    val expected01 = 1
    val cwilcox01 = new CWilcoxJava(15, 15)
    val result01 = cwilcox01.cwilcox(1, 15, 15)

    Math.abs(expected01 - result01) should be < epsilon

    val expected = 0.047619047619047616
    val cwilcox = new CWilcoxJava(0, 1)
    val result = cwilcox.cwilcox(0, 1, 20) / 21

    Math.abs(result - expected) should be < epsilon
  }

  test("lgammacorr tests") {
    val epsilon = 1E-05
    val p = 31
    val expected = 0.0026880788285311538
    val result = Wilcox.lgammacor(p)

    Math.abs(expected - result) should be < epsilon
  }

  test("test hasTies") {
    val ties = Vector[Double](1.5,6,5,2.5,2.5,2,2,3.5,3,3,3,2,3,8,4,2.5,3,2.5,2.5,2,2.5,2,2,2.5,3.5,3,1.5,5,2.5,5.5,2,
      2.5,3.5,2,2.5,4,2,2,2,2,5.5,4.5,4,3,3,3.5,2.5,3,2.5,4,6,2,5,6,3.5,3,54.5,3,5,6,3.5,4.5,2.5,2,6,5,2.5,3.5,2,3,
      2.5,3.5,3.5,2.5,3,4.5,3,15.5,5,2.5,3,4,3.5,4,4,2,4.5,2.5,4,3,2.5,2.5,2,2.5,3.5,2,3.5,1.5,3,3,2.5,3.5,2,2.5,2,2,
      5,3,2,1.5,2,4,3,2.5,3,2,2,2,2.5,3.5,1.5,1.5,3.5,2,2.5,2.5,3,3.5,2.5,6.5,2.5,4,2.5,3,2.5,6.5,2.5,3,1.5,2,2.5,3,
      7.5,1.5,2,4,2.5,2,2.5,3.5,4,2.5,3,2.5,2,3.5,3,16.5,3,3,7,4.5,5,2,3.5,6,1.5,3,2.5,4,3,1.5,5,3,2.5,5.5,3,1.5,2.5,6)

    val result = Wilcox.hasTies(ties)

    result should equal (true)

    val noties = Vector[Double](11.1712434214938,12.8134617078458,8.28666096059123,7.71279167165407,11.7650387099633,
      10.9502638376802,9.22115128261265,8.81599065385009,6.73285839333602,6.14961105288908,7.33015056482783,
      9.62230433268667,10.9215592127399,6.61838422824637,9.30954191831234,13.6193947710285,7.66619790218369,
      9.8335558797703,11.4190577526682,7.99961923524613,14.0930027356285,11.8768871926573,8.90285669969422,
      9.77208152627974,12.3379106227644,8.09110292040677,7.61750464853721,11.074371205607,11.1665320882251,
      8.05900447689263)

    val noTieResult = Wilcox.hasTies(noties)

    noTieResult should equal (false)
  }

  test("cwilcox java tests") {
    val epsilon = 1e-17
    val expected = 0.047619047619047616
    val i = 0
    val mm = 1
    val nn = 20
    val c = 21
    val wilcox = new CWilcoxJava(mm, nn)
    val result = wilcox.cwilcox(i, mm, nn) / c

    Math.abs(result - expected) should be < epsilon
  }

  test("pwilson tests") {
    val epsilon = 0.00001
    implicit val doubleEq = TolerantNumerics.tolerantDoubleEquality(epsilon)

    val expected = 0.02481
    val result = Wilcox.pwilcox(317, 30, 30, true, true)

//    val expected = 0.190476
//    val result = Wilcox.pwilcox(3, 1, 20, true, true)

    Math.abs(expected - result) should be < epsilon
  }

  test("test outer") {
    val decimalPoints = 1E6
    val expected = Vector(-8.42408552733456,-7.8502162383974,-7.59841793650978,-7.58200811784386,-7.52090046239636,
      -7.36820255464843,-7.34785690131534,-7.02454864757262,-7.0081388289067,-6.9470311734592,-6.79433326571127,
      -6.77398761237818,-5.18661336130846,-4.96563377749484,-4.37183848902532,-4.36094577048367,-4.34453595181775,
      -4.28342829637026,-4.13996618667006,-4.13073038862233,-4.12355636800414,-4.11038473528924,-4.06244871255664,
      -3.90975080480871,-3.88940515147562,-3.54617089820054,-3.52976107953462,-3.46865342408713,-3.32341549114282,
      -3.31595551633919,-3.2956098630061,-2.49774790031803,-2.48133808165211,-2.42023042620462,-2.26753251845668,
      -2.2471868651236)

    val expectedRounded = expected.map(x => (math floor x * decimalPoints) / decimalPoints)
    val x: Array[Double] = Array(11.1712434214938,12.8134617078458,8.28666096059123,7.71279167165407,
      11.7650387099633,10.9502638376802)
    val y: Array[Double] = Array(16.1368771989886,15.3112096081638,15.2336921340504,15.0809942263025,
      15.0606485729694,15.2947997894979)
    val result = Wilcox.outer(x, y, Operation.SUBTRACT)
    val resultRounded = result.map(x => (math floor x * decimalPoints) / decimalPoints)

    resultRounded should equal (expectedRounded)
  }

  test("test exact case in the Wilcox test function") {
    val x: Array[Double] = Array(0.0,13.6,26.1,29.8666666666667,29.8833333333334,32.1333333333333,28.9,30.55,
      32.0666666666667,32.1,31.3,29.85,33.2166666666666,32.9333333333333,32.2166666666667)
    val y: Array[Double] = Array(0.0,13.7333333333334,25.35,28.7,29.25,29.75,31.3833333333333,30.6666666666667,30.95,
      31.2,32.2333333333333,32.2333333333333,31.0333333333333,31.95,30.9666666666667)

    val result = Wilcox.test(x, y)

    result.upper should be > 0.0
  }

  test("test trunc") {
    val epsilon = 0.0001
    val expected: Array[Double] = Array(-2, -2, -1, -1, -1, -1, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4)

    val input: Array[Double] = Array(
      -2.999, -2, -1.9, -1.5, -1.12, -1, -0.5,
      0, 0.5, 1, 1.5, 2, 2.5, 3, 3.5, 4
    )

    val result = input.map(x => Wilcox.trunc(x))
    result should equal (expected)
  }
}
