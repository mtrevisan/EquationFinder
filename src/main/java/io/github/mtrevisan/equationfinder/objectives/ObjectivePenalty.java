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
