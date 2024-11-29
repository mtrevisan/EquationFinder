package io.github.mtrevisan.equationfinder.objectives;

import io.github.mtrevisan.equationfinder.ModelFunction;
import org.apache.commons.math3.analysis.MultivariateFunction;


/**
 * Residual Sum of Squares error
 *
 * <p>
 * Very similar to the Root Mean Squared metric but without taking the average and the square root.
 * </p>
 */
public class ObjectiveRSS implements MultivariateFunction{

	private final ModelFunction function;
	private final double[][] dataTable;


	public ObjectiveRSS(final ModelFunction function, final double[][] dataTable){
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
			final double predicted = function.evaluate(params, row);
			error += StrictMath.pow(expected - predicted, 2.);
		}
		return error / length;
	}

}
