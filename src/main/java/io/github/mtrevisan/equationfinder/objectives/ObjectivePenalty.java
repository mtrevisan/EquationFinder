package io.github.mtrevisan.equationfinder.objectives;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;


public class ObjectivePenalty implements MultivariateFunction{

	private final MultivariateFunction multivariateFunction;
	private final MultivariateVectorFunction nonLinearConstraint;


	public ObjectivePenalty(final MultivariateFunction multivariateFunction, final MultivariateVectorFunction nonLinearConstraint){
		this.multivariateFunction = multivariateFunction;
		this.nonLinearConstraint = nonLinearConstraint;
	}


	@Override
	public double value(final double[] params){
		final double error = multivariateFunction.value(params);

		//FIXME manage <= or >=
		double penalty = 0.;
		final double[] nonLinearConstraints = nonLinearConstraint.value(params);
		for(int i = 0, length = nonLinearConstraints.length; i < length; i ++)
			penalty += StrictMath.pow(nonLinearConstraints[i], 2.);

		return error + penalty;
	}

}
