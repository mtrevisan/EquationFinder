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

import io.github.mtrevisan.equationfinder.ModelFunction;
import org.apache.commons.math3.analysis.MultivariateFunction;


/**
 * Nash-Sutcliffe Efficiency
 *
 * @see <a href="https://hal.science/hal-00296842/document">Comparison of different efficiency criteria for hydrological model assessment</a>
 */
public class ObjectiveNSE implements MultivariateFunction{

	public static final String OBJECTIVE_NASH_SUTCLIFFE_EFFICIENCY = "NSE";


	private final ModelFunction function;
	private final double[][] dataTable;


	public ObjectiveNSE(final ModelFunction function, final double[][] dataTable){
		this.function = function;
		this.dataTable = dataTable;
	}


	@Override
	public double value(final double[] params){
		final int length = dataTable.length;
		final int outputIndex = dataTable[0].length - 1;

		//calculate the sum of the logarithms of the observed values
		double sumLogObserved = 0.;
		for(int i = 0; i < length; i ++)
			sumLogObserved += StrictMath.log(dataTable[i][outputIndex]);
		//Calculate the logarithmic mean
		final double meanLogObserved = sumLogObserved / length;

		//sum of squared errors
		double numerator = 0.;
		//sum of squared deviations from the logarithmic mean
		double denominator = 0.;
		//calculate the numerator and denominator
		for(int i = 0; i < length; i++){
			final double[] row = dataTable[i];

			final double observed = StrictMath.log1p(row[row.length - 1]);
			final double predicted = StrictMath.log1p(function.evaluate(params, row));

			//sum in numerator (squares of logarithmic errors)
			numerator += StrictMath.pow(observed - predicted, 2.);
			//sum in denominator (squares of deviations from the logarithmic mean)
			denominator += StrictMath.pow(observed - meanLogObserved, 2.);
		}
		return 1. - numerator / denominator;
	}

}
