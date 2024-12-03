/**
 * Copyright (c) 2024 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.equationfinder.objectives;

import io.github.mtrevisan.equationfinder.Constraint;
import io.github.mtrevisan.equationfinder.ModelFunction;
import io.github.mtrevisan.equationfinder.SearchMode;
import org.apache.commons.math3.analysis.MultivariateFunction;


public class ObjectivePenalty implements MultivariateFunction{

	private final MultivariateFunction multivariateFunction;
	private final Constraint[] constraints;
	private final SearchMode searchMode;
	private final ModelFunction function;
	private final double[][] dataTable;


	public ObjectivePenalty(final MultivariateFunction multivariateFunction, final Constraint[] constraints, final SearchMode searchMode,
			final ModelFunction function, final double[][] dataTable){
		this.multivariateFunction = multivariateFunction;
		this.constraints = constraints;
		this.searchMode = searchMode;
		this.function = function;
		this.dataTable = dataTable;
	}


	@Override
	public double value(final double[] params){
		try{
			final double error = multivariateFunction.value(params);

			double penalty = 0.;
			for(int i = 0, length = constraints.length; i < length; i ++){
				final Constraint constraint = constraints[i];

				final double penaltyError = constraint.evaluate(params);
				if(!constraint.isFeasible(penaltyError))
					penalty += StrictMath.pow(penaltyError, 2.);

				if(searchMode != SearchMode.APPROXIMATE)
					penalty += addSearchModePenalty(params);
			}
			return error + penalty;
		}
		catch(final Exception ignored){
			return Double.POSITIVE_INFINITY;
		}
	}

	private double addSearchModePenalty(final double[] params){
		double error = 0.;
		final int length = dataTable.length;
		for(int i = 0; i < length; i ++){
			final double[] row = dataTable[i];

			final double expected = row[row.length - 1];
			final double predicted = function.evaluate(params, row);
			final double delta = expected - predicted;
			error += Math.max(0., (searchMode == SearchMode.UPPER_BOUND? delta: -delta));
		}
		return error;
	}

}
