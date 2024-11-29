package io.github.mtrevisan.equationfinder;


@FunctionalInterface
public interface ParameterConstraintFunction{

	double evaluate(double[] params);

}
