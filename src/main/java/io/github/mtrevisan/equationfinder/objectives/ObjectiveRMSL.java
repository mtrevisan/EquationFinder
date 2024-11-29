package io.github.mtrevisan.equationfinder.objectives;

import io.github.mtrevisan.equationfinder.ModelFunction;
import org.apache.commons.math3.analysis.MultivariateFunction;


/**
 * (Root) Mean Squared Log error
 *
 * <p>
 * Less sensitive to large errors and more sensitive to the relative difference between the predicted and actual values. It can be a good
 * choice when the data is not very sparse and the errors’ relative difference is more important.
 * </p>
 */
public class ObjectiveRMSL implements MultivariateFunction{

	private final ModelFunction function;
	private final double[][] dataTable;


	public ObjectiveRMSL(final ModelFunction function, final double[][] dataTable){
		this.function = function;
		this.dataTable = dataTable;
	}


	@Override
	public double value(final double[] params){
		double error = 0.;
		final int length = dataTable.length;
		for(int i = 0; i < length; i ++){
			final double[] row = dataTable[i];

			final double expected = StrictMath.log1p(row[row.length - 1]);
			final double predicted = StrictMath.log1p(function.evaluate(params, row));
			error += StrictMath.pow(expected - predicted, 2.);
		}
		return error / length;
	}

}
