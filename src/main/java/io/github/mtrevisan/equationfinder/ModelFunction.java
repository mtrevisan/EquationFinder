package io.github.mtrevisan.equationfinder;


@FunctionalInterface
public interface ModelFunction{
	double evaluate(double[] inputs, double[] params);
}
