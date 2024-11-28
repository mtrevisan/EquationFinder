package io.github.mtrevisan.equationfinder.objectives;

import io.github.mtrevisan.equationfinder.ModelFunction;
import org.apache.commons.math3.analysis.MultivariateFunction;


/**
 * Mean Absolute Relative error
 *
 * <p>
 * This metric can be problematic when the actual values are close to or equal to zero. In these cases, the MAR can become very large or
 * undefined, which can distort the average error metric. Since our sales data could potentially contain many zero values (high sparsity),
 * using MAR could lead to misleading results.
 * </p>
 */
public class ObjectiveMAR implements MultivariateFunction{

	private final ModelFunction function;
	private final double[][] dataTable;


	public ObjectiveMAR(final ModelFunction function, final double[][] dataTable){
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
			error += Math.abs(1. - predicted / expected);
		}
		return error / length;
	}

}
