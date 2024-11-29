package io.github.mtrevisan.equationfinder;

import org.apache.commons.math3.optim.linear.Relationship;


public class Constraint{

	private final ParameterConstraintFunction function;
	private final Relationship relationship;


	Constraint(final ParameterConstraintFunction function, final Relationship relationship){
		this.function = function;
		this.relationship = relationship;
	}

	public double evaluate(final double[] params){
		return function.evaluate(params);
	}

	public boolean isFeasible(final double value){
		return switch(relationship){
			case LEQ -> value <= 0.;
			case EQ -> value == 0.;
			case GEQ -> value >= 0.;
		};
	}

}
