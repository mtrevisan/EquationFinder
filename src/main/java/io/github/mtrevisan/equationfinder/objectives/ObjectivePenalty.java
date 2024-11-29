package io.github.mtrevisan.equationfinder.objectives;

import io.github.mtrevisan.equationfinder.Constraint;
import org.apache.commons.math3.analysis.MultivariateFunction;


public class ObjectivePenalty implements MultivariateFunction{

	private final MultivariateFunction multivariateFunction;
	private final Constraint[] constraints;


	public ObjectivePenalty(final MultivariateFunction multivariateFunction, final Constraint[] constraints){
		this.multivariateFunction = multivariateFunction;
		this.constraints = constraints;
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
		}

		return error + penalty;
	}

}
