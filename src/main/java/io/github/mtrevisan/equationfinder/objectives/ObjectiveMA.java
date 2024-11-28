package io.github.mtrevisan.equationfinder.objectives;

import io.github.mtrevisan.equationfinder.ModelFunction;
import org.apache.commons.math3.analysis.MultivariateFunction;


/**
 * Mean Absolute error
 *
 * <p>
 * A simple and straightforward metric that calculates the average absolute difference between the predicted and actual values. It treats
 * all errors equally, regardless of their direction (overestimation or underestimation) or magnitude. This makes it a good choice for very
 * sparse data, where we have a lot of zeros and a few non-zero values. In such cases, we might not want to overly penalize large errors,
 * which could be due to the few non-zero values.
 * </p>
 */
public class ObjectiveMA implements MultivariateFunction{

	private final ModelFunction function;
	private final double[][] dataTable;


	public ObjectiveMA(final ModelFunction function, final double[][] dataTable){
		this.function = function;
		this.dataTable = dataTable;
	}


	@Override
	public double value(final double[] params){
		double error = 0.;
		final int length = dataTable.length;
		for(int i = 0; i < length; i ++){
			final double[] row = dataTable[i];

			final double expected = row[row.length - 1];
			final double predicted = function.evaluate(row, params);
			error += Math.abs(expected - predicted);
		}
		return error / length;
	}

}
