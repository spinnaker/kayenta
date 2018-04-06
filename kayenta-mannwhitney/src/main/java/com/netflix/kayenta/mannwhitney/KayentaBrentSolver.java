/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.kayenta.mannwhitney;


import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.AbstractUnivariateSolver;
import org.apache.commons.math3.exception.NoBracketingException;
import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.util.Precision;

import java.util.function.Function;

/**
 * This class implements the <a href="http://mathworld.wolfram.com/BrentsMethod.html">
 * Brent algorithm</a> for finding zeros of real univariate functions.
 * The function should be continuous but not necessarily smooth.
 * The {@code solve} method returns a zero {@code x} of the function {@code f}
 * in the given interval {@code [a, b]} to within a tolerance
 * {@code 2 eps abs(x) + t} where {@code eps} is the relative accuracy and
 * {@code t} is the absolute accuracy.
 * <p>The given interval must bracket the root.</p>
 * <p>
 *  The reference implementation is given in chapter 4 of
 *  <blockquote>
 *   <b>Algorithms for Minimization Without Derivatives</b>,
 *   <em>Richard P. Brent</em>,
 *   Dover, 2002
 *  </blockquote>
 *
 * @see BaseAbstractUnivariateSolver
 */
public class KayentaBrentSolver extends AbstractUnivariateSolver {

  /** Default absolute accuracy. */
  private static final double DEFAULT_ABSOLUTE_ACCURACY = 1e-6;

  /**
   * Construct a solver with default absolute accuracy (1e-6).
   */
  public KayentaBrentSolver() {
    this(DEFAULT_ABSOLUTE_ACCURACY);
  }
  /**
   * Construct a solver.
   *
   * @param absoluteAccuracy Absolute accuracy.
   */
  public KayentaBrentSolver(double absoluteAccuracy) {
    super(absoluteAccuracy);
  }
  /**
   * Construct a solver.
   *
   * @param relativeAccuracy Relative accuracy.
   * @param absoluteAccuracy Absolute accuracy.
   */
  public KayentaBrentSolver(double relativeAccuracy,
                     double absoluteAccuracy) {
    super(relativeAccuracy, absoluteAccuracy);
  }
  /**
   * Construct a solver.
   *
   * @param relativeAccuracy Relative accuracy.
   * @param absoluteAccuracy Absolute accuracy.
   * @param functionValueAccuracy Function value accuracy.
   *
   * @see BaseAbstractUnivariateSolver#BaseAbstractUnivariateSolver(double,double,double)
   */
  public KayentaBrentSolver(double relativeAccuracy,
                     double absoluteAccuracy,
                     double functionValueAccuracy) {
    super(relativeAccuracy, absoluteAccuracy, functionValueAccuracy);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected double doSolve()
    throws NoBracketingException,
    TooManyEvaluationsException,
    NumberIsTooLargeException {
    double min = getMin();
    double max = getMax();
    final double initial = getStartValue();
    final double functionValueAccuracy = getFunctionValueAccuracy();

    verifySequence(min, initial, max);

    double yMin = computeObjectiveValue(min);
    double yMax = computeObjectiveValue(max);
    return brent(min, max, yMin, yMax);
  }

  /**
   * Search for a zero inside the provided interval.
   * This implementation is based on the algorithm described at page 58 of
   * the book
   * <blockquote>
   *  <b>Algorithms for Minimization Without Derivatives</b>,
   *  <it>Richard P. Brent</it>,
   *  Dover 0-486-41998-3
   * </blockquote>
   *
   * @param lo Lower bound of the search interval.
   * @param hi Higher bound of the search interval.
   * @param fLo Function value at the lower bound of the search interval.
   * @param fHi Function value at the higher bound of the search interval.
   * @return the value where the function is zero.
   */

  static double brentDirect(double ax, double bx, double fa, double fb,
                     UnivariateFunction func)
  {
    System.out.println("ax, bx: " + ax + ", " + bx);
    System.out.println("Starting values: " + fa + ", " + fb);
    double a,b,c, fc;
    double tol;
    int maxit;

    a = ax;  b = bx;
    c = a;   fc = fa;
    maxit = 1000 + 1; tol = 1E-4;

    /* First test if we have found a root at an endpoint */
    if (fa == 0.0) {
      return a;
    }
    if (fb == 0.0) {
      return b;
    }

    while (maxit-- > 0)	{	/* Main iteration loop	*/
      System.out.println("Iteration " + (maxit));
      double prev_step = b - a;		/* Distance from the last but one
					   to the last approximation	*/
      double tol_act;			/* Actual tolerance		*/
      double p;			/* Interpolation step is calcu- */
      double q;			/* lated in the form p/q; divi-
       * sion operations is delayed
       * until the last moment	*/
      double new_step;		/* Step at this iteration	*/

      if (Math.abs(fc) < Math.abs(fb)) {
        /* Swap data for b to be the	*/
        a = b;  b = c;  c = a;	/* best approximation		*/
        fa=fb;  fb=fc;  fc=fa;
      }
      tol_act = 2 * (2.220446049250313E-16) * Math.abs(b) + tol / 2;
      new_step = (c-b)/2;

      if (Math.abs(new_step) <= tol_act || Precision.equals(fb, 0)) {
        return b;
      }

      if (Math.abs(prev_step) >= tol_act
        && Math.abs(fa) > Math.abs(fb) ) {
        double t1, cb, t2;
        cb = c-b;
        if (a == c) {		/* If we have only two distinct	*/
          /* points linear interpolation	*/
          t1 = fb/fa;		/* can only be applied		*/
          p = cb*t1;
          q = 1.0 - t1;
        }
        else {			/* Quadric inverse interpolation*/

          q = fa/fc;  t1 = fb/fc;	 t2 = fb/fa;
          p = t2 * ( cb*q*(q-t1) - (b-a)*(t1-1.0) );
          q = (q-1.0) * (t1-1.0) * (t2-1.0);
        }
        if (p > 0.0)		/* p was calculated with the */
          q = -q;			/* opposite sign; make p positive */
        else			/* and assign possible minus to	*/
          p = -p;			/* q				*/

        if( p < (0.75*cb*q-Math.abs(tol_act*q)/2) /* If b+p/q falls in [b,c]*/
          && p < Math.abs(prev_step*q/2) )	/* and isn't too large	*/
          new_step = p/q;			/* it is accepted
           * If p/q is too large then the
           * bisection procedure can
           * reduce [b,c] range to more
           * extent */
      }

      if( Math.abs(new_step) < tol_act) {	/* Adjust the step to be not less*/
        if( new_step > (double)0 )	/* than tolerance		*/
          new_step = tol_act;
        else
          new_step = -tol_act;
      }
      a = b;	fa = fb;			/* Save the previous approx. */
      b += new_step;	fb = func.value(b);
      if( (fb > 0 && fc > 0) || (fb < 0 && fc < 0) ) {
        /* Adjust c for it to have a sign opposite to that of b */
        c = a;  fc = fa;
      }

    }
    /* failed! */
    return b;
  }

  double brent(double ax, double bx,
                       double fa, double fb) {


    double a,b,c, fc;
    double tol;
    int maxit;

    a = ax;  b = bx;
    c = a;   fc = fa;
    maxit = 1000 + 1; tol = 1E-6;

    /* First test if we have found a root at an endpoint */
    if (fa == 0.0) {
      return a;
    }
    if (fb ==  0.0) {
      return b;
    }

    while (maxit-- > 0)	{	/* Main iteration loop	*/
      double prev_step = b-a;		/* Distance from the last but one
					   to the last approximation	*/
      double tol_act;			/* Actual tolerance		*/
      double p;			/* Interpolation step is calcu- */
      double q;			/* lated in the form p/q; divi-
       * sion operations is delayed
       * until the last moment	*/
      double new_step;		/* Step at this iteration	*/

      if (Math.abs(fc) < Math.abs(fb)) {
        /* Swap data for b to be the	*/
        a = b;  b = c;  c = a;	/* best approximation		*/
        fa=fb;  fb=fc;  fc=fa;
      }
      tol_act = 2*(2.220446049250313E-16)*Math.abs(b) + tol/2;
      new_step = (c-b)/2;

      if (Math.abs(new_step) <= tol_act || fb == 0) {
        return b;			/* Acceptable approx. is found	*/
      }

      /* Decide if the interpolation can be tried	*/
      if (Math.abs(prev_step) >= tol_act	/* If prev_step was large enough*/
        && Math.abs(fa) > Math.abs(fb) ) {	/* and was in true direction,
       * Interpolation may be tried	*/
        double t1, cb, t2;
        cb = c-b;
        if (a == c) {		/* If we have only two distinct	*/
          /* points linear interpolation	*/
          t1 = fb/fa;		/* can only be applied		*/
          p = cb*t1;
          q = 1.0 - t1;
        }
        else {			/* Quadric inverse interpolation*/

          q = fa/fc;  t1 = fb/fc;	 t2 = fb/fa;
          p = t2 * ( cb*q*(q-t1) - (b-a)*(t1-1.0) );
          q = (q-1.0) * (t1-1.0) * (t2-1.0);
        }
        if (p > 0.0)		/* p was calculated with the */
          q = -q;			/* opposite sign; make p positive */
        else			/* and assign possible minus to	*/
          p = -p;			/* q				*/

        if( p < (0.75*cb*q-Math.abs(tol_act*q)/2) /* If b+p/q falls in [b,c]*/
          && p < Math.abs(prev_step*q/2) )	/* and isn't too large	*/
          new_step = p/q;			/* it is accepted
           * If p/q is too large then the
           * bisection procedure can
           * reduce [b,c] range to more
           * extent */
      }

      if( Math.abs(new_step) < tol_act) {	/* Adjust the step to be not less*/
        if( new_step > (double)0 )	/* than tolerance		*/
          new_step = tol_act;
        else
          new_step = -tol_act;
      }
      a = b;	fa = fb;			/* Save the previous approx. */
      b += new_step;	fb = computeObjectiveValue(b);
      if( (fb > 0 && fc > 0) || (fb < 0 && fc < 0) ) {
        /* Adjust c for it to have a sign opposite to that of b */
        c = a;  fc = fa;
      }

    }
    /* failed! */
    return b;
  }
}
