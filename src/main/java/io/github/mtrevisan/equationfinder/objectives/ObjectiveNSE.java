package io.github.mtrevisan.equationfinder.objectives;

import io.github.mtrevisan.equationfinder.ModelFunction;
import org.apache.commons.math3.analysis.MultivariateFunction;


/**
 * Nash-Sutcliffe Efficiency
 *
 * @see <a href="https://hal.science/hal-00296842/document">Comparison of different efficiency criteria for hydrological model assessment</a>
 */
public class ObjectiveNSE implements MultivariateFunction{

	private final ModelFunction function;
	private final double[][] dataTable;


	public ObjectiveNSE(final ModelFunction function, final double[][] dataTable){
		this.function = function;
		this.dataTable = dataTable;
	}


	@Override
	public double value(final double[] params){
		final int length = dataTable.length;
		final int outputIndex = dataTable[0].length - 1;

		//calculate the sum of the logarithms of the observed values
		double sumLogObserved = 0.;
		for(int i = 0; i < length; i ++)
			sumLogObserved += StrictMath.log(dataTable[i][outputIndex]);
		//Calculate the logarithmic mean
		final double meanLogObserved = sumLogObserved / length;

		//sum of squared errors
		double numerator = 0.;
		//sum of squared deviations from the logarithmic mean
		double denominator = 0.;
		//calculate the numerator and denominator
		for(int i = 0; i < length; i++){
			final double[] row = dataTable[i];

			final double observed = StrictMath.log1p(row[row.length - 1]);
			final double predicted = StrictMath.log1p(function.evaluate(row, params));

			//sum in numerator (squares of logarithmic errors)
			numerator += StrictMath.pow(observed - predicted, 2.);
			//sum in denominator (squares of deviations from the logarithmic mean)
			denominator += StrictMath.pow(observed - meanLogObserved, 2.);
		}
		return 1. - numerator / denominator;
	}

}
