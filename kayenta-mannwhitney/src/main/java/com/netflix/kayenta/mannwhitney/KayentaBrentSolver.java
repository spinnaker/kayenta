/*
 * Copyright 2018 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.kayenta.mannwhitney;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.util.Precision;

public class KayentaBrentSolver {
  /**
   * Search for a zero inside the provided interval.
   *
   * @param ax Lower bound of the search interval.
   * @param bx Higher bound of the search interval.
   * @param fa Function value at the lower bound of the search interval.
   * @param fb Function value at the higher bound of the search interval.
   * @return the value where the function is zero.
   */

  static double brentDirect(double ax, double bx, double fa, double fb,
                     UnivariateFunction func)
  {
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
}
