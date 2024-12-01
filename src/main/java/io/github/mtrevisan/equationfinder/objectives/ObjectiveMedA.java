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

import java.util.Arrays;


/**
 * Median Absolute error
 *
 * <p>
 * It is less sensitive to outliers than mean-based metrics. Therefore, it’s a good choice for data that is not sparse at all, where we
 * have very few or no zeros and lots of non-zero values. In such cases, we might want to focus on the typical error (as given by the
 * median) rather than being influenced by a few large errors (which would affect the mean).
 * </p>
 */
public class ObjectiveMedA implements MultivariateFunction{

	public static final String OBJECTIVE_MEDIANT_ABSOLUTE_ERROR = "MedA";


	private final ModelFunction function;
	private final double[][] dataTable;


	public ObjectiveMedA(final ModelFunction function, final double[][] dataTable){
		this.function = function;
		this.dataTable = dataTable;
	}


	@Override
	public double value(final double[] params){
		final int length = dataTable.length;
		final double[] errors = new double[length];
		for(int i = 0; i < length; i ++){
			final double[] row = dataTable[i];

			final double expected = row[row.length - 1];
			final double predicted = function.evaluate(params, row);
			errors[i] = Math.abs(expected - predicted);
		}

		Arrays.sort(errors);

		final int mid = length >> 1;
		return (length % 2 == 1
			? errors[mid]
			: (errors[mid - 1] + errors[mid]) / 2.);
	}

}
