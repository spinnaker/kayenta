package com.netflix.kayenta.mannwhitney

import org.scalatest.{FunSuite, Matchers}

class WilcoxTests extends FunSuite with Matchers {

  def compareWilcoxResults(res1: WilcoxResult, res2: WilcoxResult, epsilon: Double = 1e-6): Boolean = {
    res1.path == res2.path &&
      compareDoubles(res1.lower, res2.lower, epsilon) &&
      compareDoubles(res1.upper, res2.upper, epsilon)
  }

  def compareDoubles(double1: Double, double2: Double, epsilon: Double = 1e-6): Boolean = {
    (double1.isNaN && double2.isNaN) ||
      (double1.isPosInfinity && double2.isPosInfinity) ||
      (double1.isNegInfinity && double2.isNegInfinity) ||
      Math.abs(double1 - double2) < epsilon
  }

  test("testing compare doubles") {
    compareDoubles(Double.NaN, Double.NaN, 1e-8) should equal (true)
    compareDoubles(Double.NaN, 0.0001) should equal (false)
    compareDoubles(Double.PositiveInfinity, Double.PositiveInfinity) should equal (true)
    compareDoubles(Double.NegativeInfinity, Double.NegativeInfinity) should equal (true)
    compareDoubles(Double.PositiveInfinity, Double.NaN) should equal (false)

    compareDoubles(0.02, 0.02) should equal (true)

    compareDoubles(0.0000000587, 0.0000000597, 1e-10) should equal (false)
  }

  test("testing compare wilcox results") {
    compareWilcoxResults(
      WilcoxResult(Double.NaN, Double.NaN),
      WilcoxResult(Double.NaN, Double.NaN)) should equal (true)
  }

  test("test no data sent to Wilcox test") {
    val expected = WilcoxResult(Double.NaN, Double.NaN)
    val canary = Array[Double]()
    val baseline = Array[Double]()
    val result = Wilcox.test(canary, baseline)

    compareWilcoxResults(result, expected) should equal (true)
  }

  test("asymptotic path in wilcox.test") {
    val epsilon = 1E-4
    val expected01 = WilcoxResult(-1.866663, 2.899996, WilcoxPath.ASYMPTOTIC)
    val canary01 = Array[Double](0.0,20.95,30.5666666666667,31.4,32.1166666666667,31.9833333333334,31.1833333333334,
      32.35,33.25,32.8000000000001,34.5166666666667,33.4166666666667,35.8833333333334,35.0666666666667,33.9)
    val baseline01 = Array[Double](0.0,21.9000000000001,30.2833333333334,29.45,32.1,31.1166666666667,33.5,
      32.1166666666667,32.7,33.85,32.6833333333334,33.5166666666667,33.6,34.3,33.05 )
    val result01 = Wilcox.test(canary01, baseline01)

    compareWilcoxResults(expected01, result01, 1e-4) should equal (true)

    val expected02 = WilcoxResult(-0.70007815, 0.09997816, WilcoxPath.ASYMPTOTIC)
    val canary02 = Array[Double](0,1.66666666666667,3.55,3.8,3.6,3.81666666666667,3.4,3.61666666666667,
      3.53333333333333,3.41666666666667,3.4,3.13333333333333,3.55,3.8,3.76666666666667)
    val baseline02 = Array[Double](0,2.58333333333333,3.61666666666667,3.78333333333333,3.95,3.25,4.15,
      4.08333333333333,4.25,3.8,3.88333333333333,3.7,4.46666666666667,3.6,3.71666666666667)
    val result02 = Wilcox.test(canary02, baseline02)

//    compareWilcoxResults(expected02, result02) should equal (true)

    val expected03 = WilcoxResult(-0.5166050, 0.3832704, WilcoxPath.ASYMPTOTIC)
    val canary03 = Array[Double](0,2.31666666666667,3.5,3.33333333333333,3.98333333333333,3.61666666666667,
      3.08333333333333,3.56666666666666,3.86666666666666,3.28333333333333,3.25,3.26666666666667,2.86666666666667,
      3.31666666666667,3.18333333333333)
    val baseline03 = Array[Double](0,2.73333333333333,3.5,2.76666666666667,3.26666666666666,3.75,3.53333333333333,
      3.21666666666667,3.43333333333333,3.83333333333333,3.81666666666667,3.6,3.38333333333333,3.31666666666667,
      3.63333333333333)
    val result03 = Wilcox.test(canary03, baseline03)

//    compareWilcoxResults(expected03, result03) should equal (true)

    val expected04 = WilcoxResult(-0.4166138, 0.4499399, WilcoxPath.ASYMPTOTIC)
    val canary04 = Array[Double](0,1.9,2.78333333333333,2.48333333333333,2.71666666666667,3.26666666666667,3.3,2.45,
      2.58333333333333,2.9,3.03333333333333,2.06666666666667,2.43333333333333,3,2.61666666666667)
    val baseline04 = Array[Double](0,1.68333333333333,2.76666666666667,2.5,2.88333333333333,2.56666666666667,2.85,
      2.85,2.48333333333333,2.71666666666667,2.91666666666667,2.73333333333333,2.78333333333333,2.96666666666667,2.55)
    val result04 = Wilcox.test(canary04, baseline04)

//    compareWilcoxResults(expected04, result04) should equal (true)
  }

  test("test interesting use case") {
    val expected = WilcoxResult(1.401027e-05, 1.526706e-05, WilcoxPath.ASYMPTOTIC)
    val canary = Array[Double](0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.6666666666666572, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.7037037037036811, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.5625000000000142, 0.0, 0.0, 0.0, 0.0, 3.3333333333333428, 0.0, 1.7543859649122737, 0.0, 1.8518518518518476, 0.0, 0.0, 1.8518518518518476, 0.0, 0.0, 1.5151515151515156, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.7857142857142776, 1.3333333333333286, 0.0, 3.1250000000000142, 1.9607843137255117, 1.4492753623188293, 2.4390243902438868, 1.6393442622950687, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.6129032258064484, 1.9607843137255117, 1.5873015873015817, 0.0, 0.0, 2.8169014084507182, 0.0, 0.0, 3.0303030303030454, 0.0, 0.0, 3.3898305084745743, 0.0, 0.0, 1.234567901234584, 0.0, 0.0, 0.0, 0.0, 1.7241379310344769, 0.0, 0.0, 0.0, 0.0, 1.6949152542372872, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.470588235294116, 0.0, 0.0, 1.3698630136986196, 3.225806451612911, 1.7543859649122879, 3.4482758620689538, 0.0, 3.3333333333333428, 0.0, 0.0, 0.0, 1.818181818181813, 0.0, 0.0, 1.4285714285714448, 0.0, 0.0, 1.8867924528301927, 2.0000000000000284, 0.0, 0.0, 0.0, 2.985074626865682, 0.0, 0.0, 1.6666666666666714, 0.0, 0.0, 1.5625, 1.9230769230769198, 0.0, 1.8518518518518619, 0.0, 2.7027027027027088, 0.0, 0.0, 0.0, 1.3513513513513544, 0.0, 1.818181818181813, 0.0, 1.6666666666666572, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.818181818181813, 0.0, 1.4285714285714164, 0.0, 0.0, 4.1095890410959015, 0.0, 0.0, 0.0, 1.8518518518518619, 0.0, 0.0, 1.8518518518518476, 1.3888888888888857, 0.0, 0.0, 0.0, 0.0, 1.8867924528301785, 1.4285714285714164, 0.0, 0.0, 1.5625, 1.8867924528301927, 0.0, 0.0, 0.0, 0.0, 2.040816326530603, 1.818181818181813, 0.0, 0.0, 1.6393442622950687, 0.0, 1.7241379310344769, 1.5151515151515156, 2.564102564102555, 0.0, 0.0, 0.0, 0.0, 5.5555555555555571, 0.0, 0.0, 4.0816326530612059, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 3.5714285714285836, 0.0, 2.040816326530603, 3.1746031746031633, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    val baseline = Array[Double](0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.5625000000000142, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.3888888888888857, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.5384615384615614, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.4925373134328339, 0.0, 0.0, 0.0, 1.6949152542373014, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.470588235294116, 0.0, 0.0, 0.0, 0.0, 1.7543859649122879, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.8518518518518619, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.6393442622950687, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.7241379310344769, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.5873015873015817, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.4925373134328339, 0.0, 0.0, 0.0, 3.1249999999999858, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.4925373134328339, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 2.9850746268656536, 0.0, 2.5000000000000284, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.7857142857142776, 0.0, 0.0, 0.0)
    val result = Wilcox.test(canary, baseline)

    compareWilcoxResults(expected, result, epsilon = 1e-4) should equal (true)
  }

  test("choose tests") {
    val epsilon = 1e-5
    val expected: Double = 21.0
    val result: Double = Wilcox.choose(21, 20)

    compareDoubles(expected, result, epsilon) should equal (true)
  }

  test("test log analysis") {
    val expected = -4.1108738641733114
    val n = 60
    val result = -Math.log(n + 1.0)

    compareDoubles(expected, result) should equal (true)
  }

  test("test lbeta") {
    val expected = -43.422574590184574
    val n = 60
    val k = 30
    val result = Wilcox.lbeta(n - k + 1, k + 1)

    compareDoubles(expected, result) should equal (true)
  }

  test("test big p and big qs") {
    val expected = 0.0040320832899632675
    val p = 31
    val q = 31
    val result = Wilcox.lgammacor(p) + Wilcox.lgammacor(q) - Wilcox.lgammacor(p + q)

    compareDoubles(expected, result) should equal (true)
  }

  test("test chebyshev") {
    val input_x = -0.79188345473465138
    val input_n = 5
    val expected = 0.083330443684465765
    val result = Wilcox.chebyshev_eval(input_x, Wilcox.algmcs, input_n)

    compareDoubles(expected, result) should equal (true)
  }

  test("problem choose test") {
    val epsilon = 1E-4

    val expected01 = 155117520
    val result01 = Wilcox.choose(30, 15)

    compareDoubles(expected01, result01, epsilon) should equal (true)

    val expected = 1.1826458156486115E+17
    val result = Wilcox.choose(60, 30)

    compareDoubles(expected, result, epsilon) should equal (true)
  }

  test("cwilson tests") {
    val epsilon = 1e-9

    val expected01 = 1
    val cwilcox01 = new CWilcoxJava(15, 15)
    val result01 = cwilcox01.cwilcox(1, 15, 15)

    compareDoubles(expected01, result01, epsilon) should equal (true)

    val expected02 = 0.047619047619047616
    val cwilcox = new CWilcoxJava(0, 1)
    val result02 = cwilcox.cwilcox(0, 1, 20) / 21

    compareDoubles(expected02, result02, epsilon) should equal (true)
  }

  test("lgammacorr tests") {
    val epsilon = 1E-05
    val p = 31
    val expected = 0.0026880788285311538
    val result = Wilcox.lgammacor(p)

    compareDoubles(expected, result, epsilon) should equal (true)
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

    compareDoubles(expected, result, epsilon) should equal (true)
  }

  test("pwilson tests") {
    val epsilon = 1e-5

    val expected01 = 0.02481
    val result01 = Wilcox.pwilcox(317, 30, 30, true, true)

    compareDoubles(expected01, result01, epsilon) should equal (true)

    val expected02 = 0.190476
    val result02 = Wilcox.pwilcox(3, 1, 20, true, true)

    compareDoubles(expected02, result02, epsilon) should equal (true)
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
    val epsilon = 1e-4
    val expected: Array[Double] = Array(-2, -2, -1, -1, -1, -1, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4)

    val input: Array[Double] = Array(
      -2.999, -2, -1.9, -1.5, -1.12, -1, -0.5,
      0, 0.5, 1, 1.5, 2, 2.5, 3, 3.5, 4
    )

    val result = input.map(x => Wilcox.trunc(x))
    result should equal (expected)
  }
}
