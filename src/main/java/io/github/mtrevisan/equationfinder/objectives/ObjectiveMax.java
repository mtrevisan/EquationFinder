package io.github.mtrevisan.equationfinder.objectives;

import io.github.mtrevisan.equationfinder.ModelFunction;
import org.apache.commons.math3.analysis.MultivariateFunction;


/** Maximum error */
public class ObjectiveMax implements MultivariateFunction{

	private final ModelFunction function;
	protected final double[][] dataTable;


	public ObjectiveMax(final ModelFunction function, final double[][] dataTable){
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
			error = Math.max(error, Math.abs(expected - predicted));
		}
		return error;
	}

}
