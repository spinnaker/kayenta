package com.netflix.kayenta.mannwhitney

import com.netflix.kayenta.mannwhitney.Alternative.Alternative
import com.netflix.kayenta.mannwhitney.Operation.Operation
import org.apache.commons.math3.special.Gamma
import org.apache.commons.math3.stat.ranking.{NaturalRanking, TiesStrategy}

case class WilcoxResult(lower: Double, upper: Double)

object Alternative extends Enumeration {
  type Alternative = Value

  val TWOSIDED, LESS, GREATER = Value
}

object Operation extends Enumeration {
  type Operation = Value

  val ADD, SUBTRACT, MULTIPLY, DIVIDE = Value
}

object Wilcox {

  //<editor-fold desc="Private Variables">
  private val wilcoxCount: Int = 50
  private val k_small_max: Double = 30
  private val M_LN_SQRT_2PI: Double = 0.918938533204672741780329736406
  private val M_LN_SQRT_PId2: Double = 0.225791352644727432363097614947
  private val epsilon: Double = 1E-9
  private val xmax = 171.61447887182298

  val algmcs: Array[Double] = Array(
    +.1666389480451863247205729650822e+0,
    -.1384948176067563840732986059135e-4,
    +.9810825646924729426157171547487e-8,
    -.1809129475572494194263306266719e-10,
    +.6221098041892605227126015543416e-13,
    -.3399615005417721944303330599666e-15,
    +.2683181998482698748957538846666e-17,
    -.2868042435334643284144622399999e-19,
    +.3962837061046434803679306666666e-21,
    -.6831888753985766870111999999999e-23,
    +.1429227355942498147573333333333e-24,
    -.3547598158101070547199999999999e-26,
    +.1025680058010470912000000000000e-27,
    -.3401102254316748799999999999999e-29,
    +.1276642195630062933333333333333e-30)

  val gamcs: Array[Double] = Array(
    +.8571195590989331421920062399942e-2,
    +.4415381324841006757191315771652e-2,
    +.5685043681599363378632664588789e-1,
    -.4219835396418560501012500186624e-2,
    +.1326808181212460220584006796352e-2,
    -.1893024529798880432523947023886e-3,
    +.3606925327441245256578082217225e-4,
    -.6056761904460864218485548290365e-5,
    +.1055829546302283344731823509093e-5,
    -.1811967365542384048291855891166e-6,
    +.3117724964715322277790254593169e-7,
    -.5354219639019687140874081024347e-8,
    +.9193275519859588946887786825940e-9,
    -.1577941280288339761767423273953e-9,
    +.2707980622934954543266540433089e-10,
    -.4646818653825730144081661058933e-11,
    +.7973350192007419656460767175359e-12,
    -.1368078209830916025799499172309e-12,
    +.2347319486563800657233471771688e-13,
    -.4027432614949066932766570534699e-14,
    +.6910051747372100912138336975257e-15,
    -.1185584500221992907052387126192e-15,
    +.2034148542496373955201026051932e-16,
    -.3490054341717405849274012949108e-17,
    +.5987993856485305567135051066026e-18,
    -.1027378057872228074490069778431e-18,
    +.1762702816060529824942759660748e-19,
    -.3024320653735306260958772112042e-20,
    +.5188914660218397839717833550506e-21,
    -.8902770842456576692449251601066e-22,
    +.1527474068493342602274596891306e-22,
    -.2620731256187362900257328332799e-23,
    +.4496464047830538670331046570666e-24,
    -.7714712731336877911703901525333e-25,
    +.1323635453126044036486572714666e-25,
    -.2270999412942928816702313813333e-26,
    +.3896418998003991449320816639999e-27,
    -.6685198115125953327792127999999e-28,
    +.1146998663140024384347613866666e-28,
    -.1967938586345134677295103999999e-29,
    +.3376448816585338090334890666666e-30,
    -.5793070335782135784625493333333e-31)

  val sferr_halves: Array[Double] = Array(
    0.0, /* n=0 - wrong, place holder only */
    0.1534264097200273452913848, /* 0.5 */
    0.0810614667953272582196702, /* 1.0 */
    0.0548141210519176538961390, /* 1.5 */
    0.0413406959554092940938221, /* 2.0 */
    0.03316287351993628748511048, /* 2.5 */
    0.02767792568499833914878929, /* 3.0 */
    0.02374616365629749597132920, /* 3.5 */
    0.02079067210376509311152277, /* 4.0 */
    0.01848845053267318523077934, /* 4.5 */
    0.01664469118982119216319487, /* 5.0 */
    0.01513497322191737887351255, /* 5.5 */
    0.01387612882307074799874573, /* 6.0 */
    0.01281046524292022692424986, /* 6.5 */
    0.01189670994589177009505572, /* 7.0 */
    0.01110455975820691732662991, /* 7.5 */
    0.010411265261972096497478567, /* 8.0 */
    0.009799416126158803298389475, /* 8.5 */
    0.009255462182712732917728637, /* 9.0 */
    0.008768700134139385462952823, /* 9.5 */
    0.008330563433362871256469318, /* 10.0 */
    0.007934114564314020547248100, /* 10.5 */
    0.007573675487951840794972024, /* 11.0 */
    0.007244554301320383179543912, /* 11.5 */
    0.006942840107209529865664152, /* 12.0 */
    0.006665247032707682442354394, /* 12.5 */
    0.006408994188004207068439631, /* 13.0 */
    0.006171712263039457647532867, /* 13.5 */
    0.005951370112758847735624416, /* 14.0 */
    0.005746216513010115682023589, /* 14.5 */
    0.005554733551962801371038690 /* 15.0 */)
  //</editor-fold>

  def ~=(x: Double, y: Double, precision: Double): Boolean = {
    if ((x - y).abs < precision) true else false
  }

  def chebyshev_eval(x: Double, a: Array[Double], n: Int) : Double = {

    if (n < 1 || n > 1000) {
      return Double.NaN
    }

    if (x < -1.1 || x > 1.1) {
      return Double.NaN
    }

    val twox: Double = x * 2
    var b2: Double = 0.0
    var b1: Double = 0.0
    var b0: Double = 0.0
    for (i <- 1 to n) {
      b2 = b1
      b1 = b0
      b0 = twox * b1 - b2 + a(n - i)
    }

    return (b0 - b2) * 0.5
  }

  def choose(n: Double, k: Double): Double = {

    var r = 0.0
    var local_k = k

    if (local_k < k_small_max) {
      if (n - local_k < local_k && n >= 0 && ~=(local_k, Math.floor(local_k), epsilon) && !local_k.isInfinite)
        local_k = n - local_k
      if (local_k < 0) return 0.0
      if (~=(local_k, 0.0, epsilon)) return 1.0

      r = n
      for (j <- 2 to Math.ceil(local_k).toInt) {
        r *= (n - j + 1) / j
      }

      return r
    }

    n match {
      case x if x < 0 => {
        r = choose(-n + local_k - 1, local_k)
        local_k match {
          case o if isOdd(o) => return -r
          case _ => return r
        }
      }
      case x if ~=(x, Math.floor(x), epsilon) && !x.isInfinite => {
        val in: Int = x.toInt
        in match {
          case o if o < local_k => return 0.0
          case o if o - local_k < k_small_max => return choose(x, x - local_k)
          case _ => return Math.exp(lfastchoose(x, local_k))
        }
      }
      case _ => Double.NaN
    }
  }

  def compare(d1: Double, d2: Double): Int = {
    if (~=(d1, d2, epsilon)) return 0
    else if (d1 < d2) return -1
    return 1
  }

  def gammafn(x: Double): Double = {

    val ngam: Int = 22
    val xmin: Double = -170.5674972726612
    val xsml: Double = 2.2474362225598545E-308
    val dxrel = 1.490116119384765696E-8
    if (x.isNaN) return x
    var value: Double = 0.0

    if (x == 0 || (x < 0 && ~=(x, Math.round(x), epsilon))) return Double.NaN

    var y: Double = Math.abs(x)

    //    x match {
    //      case y if Math.abs(y) <= 10 => {
    //        var n: Int = x.toInt
    //        if (n == 0)
    //          n -= 1
    //        var z = x - n
    //        n -= 1
    //        var value = chebyshev_eval(z * 2 - 1, gamcs, ngam) + 0.9375
    //        n match {
    //          case o if o == 0 => value
    //          case o if o < 0 => {
    //            x match {
    //              case z if z == 0 => value
    //              case z if z < 0 => {
    //
    //              }
    //            }
    //          }
    //        }
    //      }
    //      case _ => {
    //
    //      }
    //    }

    if (y <= 10) {
      var n: Int = x.toInt
      if (x < 0)
        n -= 1
      y = x - n
      n -= 1
      value = chebyshev_eval(y * 2 - 1, gamcs, ngam) + 0.9375
      if (n == 0)
        return value

      if (n < 0) {
        if (x < -0.5 && Math.abs(x - (x - 0.5).toInt / x) < dxrel) return Double.NaN

        if (y < xsml) return Double.NaN

        n = -n

        for (i <- 0 to n) {
          value /= (x + i)
        }

        return value
      }
      else {
        for (i <- 1 to n) {
          value *= (y + i)
        }

        return value
      }
    }
    else {

      if (x > xmax) return Double.PositiveInfinity

      if (x < xmin) return Double.NegativeInfinity

      if (y <= 50 && y == y.toInt) {
        value = 1.0
        var i = 2
        while (i < y) {
          value *= i.toDouble
          i += 1
        }
      }
      else {
        value = Math.exp((y - 0.5) * Math.log(y) - y + M_LN_SQRT_2PI +
          (if (2 * y == 2 * y) stirlerr(y) else lgammacor(y)))
      }
      if (x > 0)
        return value

      if (Math.abs((x - (x - 0.5).toInt) / x) < dxrel) return Double.NaN

      val sinpiy: Double = sinpi(y)
      if (sinpiy == 0.0) {
        return Double.PositiveInfinity
      }

      return -Math.PI / (y * sinpiy * value)
    }
  }

  def hasTies(x: Vector[Double]): Boolean = {
    return x.length != x.distinct.length
  }

  def isEven(number: Double): Boolean = number % 2 == 0

  def isOdd(number: Double): Boolean = !isEven(number)

  def lbeta(a: Double, b: Double): Double = {
    var corr: Double = 0.0

    val p = Math.min(a, b)
    val q = Math.max(a, b)

    // both arguments must be >= 0
    if (p < 0) {
      return Double.NaN
    }
    else if (Math.abs(p - 0.0) < epsilon) {
      return Double.PositiveInfinity
    }

    if (p >= 10) {
      corr = lgammacor(p) + lgammacor(q) - lgammacor(p + q)
      return Math.log(q) * -0.5 + M_LN_SQRT_2PI + corr + (p - 0.5) *
      Math.log(p / (p + q)) + q * Math.log1p(-p / (p + q))
    }
    else if (q >= 10) {
      corr = lgammacor(q) - lgammacor(p + q)
      return lgammafn(p) + corr + p - q * Math.log(p + q) +
        (q - 0.5) * Math.log1p(-p / (p + q))
    }
    else {
      if (p < 1e-306) return Gamma.logGamma(p) + (Gamma.logGamma(q) - Gamma.logGamma(p + q))
      else return Math.log(gammafn(p) * (gammafn(q) / gammafn(p + q)))
    }
  }

  def lgammacor(x: Double): Double = {

    val xmax = 3.745194030963158E+306
    val xbig = 94906265.62425156
    val nalgm = 5

    x match {
      case y if y < 10 => return Double.NaN
      case y if y >= xmax => return Double.NaN
      case y if y < xbig => {
        val tmp = 10 / y
        return chebyshev_eval(tmp * tmp * 2 - 1, algmcs, nalgm) / x
      }
      case _ => return 1 / (x * 12)
    }
  }

  def lgammafn(x: Double): Double = {
    lgammafn_sign(x, None)
  }

  def lgammafn_sign(x: Double, sgn: Option[Int]): Double = {

    var local_sgn: Int = sgn match {
      case Some(value) => {
        if (x < 0 && (Math.floor(-x) % 2.0 == 0.0)) -1
        else 1
      }
      case None => 1
    }

    x match {
      case y if y <= 0 && ~=(y, trunc(y), epsilon) => Double.PositiveInfinity
      case y if Math.abs(y) < 1E-306 => -1 * Math.log(Math.abs(y))
      case y if y <= 10 => Math.log(Math.abs(gammafn(y)))
      case y if y > xmax => Double.PositiveInfinity
      case y if y > 0 => M_LN_SQRT_2PI + (y - 0.5) * Math.log(y) - x + lgammacor(y)
      case _ => Double.NaN
    }
  }

  def lfastchoose(n:Double, k:Double): Double = {
    return -1 * Math.log(n + 1.0) - lbeta(n - k + 1.0, k + 1.0)
  }

  def outer(x: Array[Double], y: Array[Double], operation: Operation): Vector[Double] = {
    val result = scala.collection.mutable.ArrayBuffer.empty[Double]

    for (i <- 0 until x.length) {
      for (j <- 0 until y.length) {
        //        result += x(i) - y(j)
        val tmp = operation match {
          case Operation.ADD => x(i) + y(j)
          case Operation.SUBTRACT => x(i) - y(j)
          case Operation.MULTIPLY => x(i) * y(j)
          case Operation.DIVIDE => x(i) / y(j)
          case _ => x(i) + y(j)
        }
        result += tmp
      }
    }

    return result.sortBy(v => v).toVector
  }

  def pwilcox(q: Double, m: Double, n: Double, lowerTail: Boolean = true, logP: Boolean = false): Double = {

    if (m <= 0.0 || n <= 0.0) return Double.NaN

    val mRounded: Long = Math.round(m)
    val nRounded: Long = Math.round(n)

    val qFloored = Math.floor(q + 1E-7)

    if (qFloored < 0.0) return 0.0
    if (qFloored > mRounded * nRounded) return 1.0

    val mm: Int = mRounded.toInt
    val nn: Int = nRounded.toInt

//    val cwilcox = CWilcox(mm, nn)
    val wilcox = new CWilcoxJava(mm, nn)

    val c = choose(m + n, n)

    var local_p = 0.0
    val qInt = q.toInt
    //    var i = 0

    q match {
      case r if r <= (m * n / 2) => {
        for (i <- 0 to qInt) {
          local_p += wilcox.cwilcox(i, mm, nn) / c
        }
      }
      case _ => {
        val local_q = m * n - q
//        for (i <- 0 until qInt) local_p += cwilcox(i, mm, nn) / c
        for (i <- 0 to qInt) {
          local_p += wilcox.cwilcox(i, mm, nn) / c
        }
      }
    }
    local_p
  }

  def qwilcox(x: Double, m: Double, n: Double, lowerTail: Boolean = true, logP: Boolean = false): Double = {

    var mRounded = Math.round(m)
    var nRounded = Math.round(n)

    if (mRounded <= 0 || n <= 0) return Double.NaN

    if (compare(x, 0.0) == 0) return 0.0
    if (compare(x, 1.0) == 0) return m * n

    var local_x = x
    if (logP || !lowerTail)
      local_x = R_DT_qIv(local_x, lowerTail, logP)

    val mm: Int = m.toInt
    val nn: Int = n.toInt

    val cwilcox = new CWilcoxJava(mm, nn)

    val c: Double = choose(m + n, n)
    var p: Double = 0.0
    var q: Int = 0

    if (local_x <= 0.5) {
      local_x = local_x - 10 * epsilon
      var isComplete: Boolean = false
      while (!isComplete) {
//        p += cwilcox(q, mm, nn) / c
        p += cwilcox.cwilcox(q, mm, nn) / c
        if (p >= local_x)
          isComplete = true
        else
          q += 1
      }
    }
    else {
      local_x = 1 - local_x + 10 - epsilon
      var isComplete: Boolean = false
      while (!isComplete) {
//        p += cwilcox(q, mm, nn) / c
        p += cwilcox.cwilcox(q, mm, nn) / c
        if (p > local_x) {
          q = (m * n - q).toInt
          isComplete = true
        }
        else q += 1
      }
    }

    return q
  }

  def R_DT_qIv(p: Double, lowerTail: Boolean, logP: Boolean): Double = {
    logP match {
      case true => if (lowerTail) Math.exp(p) else -1 * Math.expm1(p)
      case false => R_Q_P01_check(p, logP)
    }
  }

  def R_Q_P01_check(p: Double, logP: Boolean): Double = {
    if ((logP && p > 0.0) || !logP && (p < 0.0 || p > 1.0)) Double.NaN else p
  }

  def sinpi(x: Double): Double = {

    var local_x: Double = x % 2

    if (local_x <= -1) local_x += 2.0
    else if (local_x > 1.0) local_x -= 2.0

    if (local_x == 0.0 || local_x == 1.0) return 0.0
    if (~=(x, 0.5, epsilon)) return 1.0

    if (~=(x, -0.5, epsilon)) -1.0 else Math.sin(Math.PI * local_x)
  }

  def stirlerr(n: Double): Double = {
    val s0: Double = 0.083333333333333333333
    val s1: Double = 0.00277777777777777777778
    val s2: Double = 0.00079365079365079365079365
    val s3: Double = 0.000595238095238095238095238
    val s4: Double = 0.0008417508417508417508417508

    var nn: Double = 0.0

    if (n <= 15.0) {
      nn = n * 2
      if (~=(nn, nn.toInt, epsilon)) sferr_halves(nn.toInt) else lgammafn(n + 1.0) - (n + 0.5) * Math.log(n) + n - M_LN_SQRT_2PI
    }
    else {
      nn = n * n
      n match {
        case x if x > 500 => (s0 - s1 / nn) / n
        case x if x > 80 => (s0 - (s1 - s2 / nn) / nn) / n
        case x if x > 35 => (s0 - (s1 - (s2 - s3 / nn) / nn) / nn) / n
        case _ => (s0 - (s1 - (s2 - (s3 - s4 / nn) / nn) / nn) / nn) / n
      }
    }
  }

  def test(x: Array[Double], y: Array[Double],
           alternative: Alternative = Alternative.TWOSIDED,
           mu: Double = 0.0, paired: Boolean = false,
           exact: Option[Boolean] = None, correct: Boolean = true,
           confidenceLevel: Double = 0.95): WilcoxResult = {

    val exactCase = exact match {
      case Some(exact) => exact
      case _ => x.length < this.wilcoxCount && y.length < this.wilcoxCount
    }

    val xy: Array[Double] = x.map(x => x - mu) ++ y
    val alpha = 1 - confidenceLevel
    val diffs = outer(x, y, Operation.SUBTRACT)
    var rankAlg = new NaturalRanking(TiesStrategy.AVERAGE)
    val r = rankAlg.rank(xy)

    if (exactCase && hasTies(r.toVector)) {

      val confidenceIntervals = alternative match {
        case Alternative.TWOSIDED => {
          val qw = qwilcox(alpha / 2, x.length, y.length)
          val qu = qw match {
            case q if q == 0 => 1
            case _ => qw
          }
          val ql = x.length * y.length - qu
          val achievedAlpha = 2 * pwilcox(trunc(qu) - 1, x.length, y.length)
          WilcoxResult(diffs(qu.toInt - 1), diffs(ql.toInt))
        }
        case Alternative.GREATER => {
          val qw = qwilcox(alpha, x.length, y.length)
          val qu = qw match {
            case q if q == 0 => 1
            case _ => qw
          }
          val achievedAlpha = pwilcox(trunc(qu) - 1, x.length, y.length)
          WilcoxResult(diffs(qu.toInt - 1), Double.PositiveInfinity)
        }
        case Alternative.LESS => {
          val qw = qwilcox(alpha, x.length, y.length)
          val qu = qw match {
            case q if q == 0 => 1
            case _ => qw
          }
          val ql = x.length * y.length - qu
          val achievedAlpha = pwilcox(trunc(qu) - 1, x.length, y.length)
          WilcoxResult(Double.NegativeInfinity, diffs(ql.toInt))
        }
      }

      confidenceIntervals
    }
    else {
      val params = MannWhitneyParams(mu = mu, confidenceLevel = confidenceLevel,
        controlData = x, experimentData = y)
      val mw = new MannWhitney
      val results = mw.eval(params)

      WilcoxResult(results.confidenceInterval.head, results.confidenceInterval.last)
    }
  }

  def trunc(x: Double): Double = {
    if (x >= 0.0 || x.isNaN || x.isInfinite)
      Math.floor(x)
    else if (x < 0)
      Math.ceil(x)
    else
      Double.NaN
  }
}
