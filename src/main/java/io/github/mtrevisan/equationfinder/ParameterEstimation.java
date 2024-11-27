package io.github.mtrevisan.equationfinder;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.Relationship;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.MapContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;


public class ParameterEstimation{

	@FunctionalInterface
	interface ModelFunction{
		double evaluate(double[] inputs, double[] params);
	}


	private static final JexlEngine JEXL_ENGINE = new JexlBuilder()
		.cache(512)
		.strict(true)
		//FIXME to put to true
		.silent(false)
		.create();

	public static void main(final String[] args){
		//Input-Output table: x1, x2, output
		final double[][] dataTable = {
			{1., 2., 10.},
			{2., 3., 20.},
			{3., 4., 30.}
		};

		//model function: f(x, p) = p0 * x0^2 + p1 * sin(x1) + p2
		final String functionExpression = "p0 * x0^2 + p1 * sin(x1) + p2";
		final Set<String> vars = extractVariables(functionExpression);
		//FIXME count inputs and parameters
		for(int i = 0, inputCount = dataTable[0].length - 1; i < inputCount; i ++)
			if(!vars.remove("x" + i))
				throw new IllegalArgumentException("Input variable 'x" + i + "' missing");

		final ModelFunction function = parseFunction(functionExpression);
//		final ModelFunction function = (inputs, params)
//			-> params[0] * StrictMath.pow(inputs[0], 2) + params[1] * StrictMath.sin(inputs[1]) + params[2];

		//linear constraints: p0, p1, p2 >= 0
		final LinearConstraintSet linearConstraints = new LinearConstraintSet(
			new LinearConstraint(new double[]{1, 0, 0}, Relationship.GEQ, 0),
			new LinearConstraint(new double[]{0, 1, 0}, Relationship.GEQ, 0),
			new LinearConstraint(new double[]{0, 0, 1}, Relationship.GEQ, 0)
		);
		//nonlinear constraints: 10 - (p0 + p1) >= 0
		final MultivariateVectorFunction nonLinearConstraint = params -> new double[]{
			10 - (params[0] + params[1])
		};

		//objective function: Mean Squared Error
		final MultivariateFunction objectiveMSE = (params) -> {
			double error = 0.;
			for(int i = 0, length = dataTable.length; i < length; i ++){
				final double[] row = dataTable[i];

				final double actual = row[row.length - 1];
				final double predicted = function.evaluate(row, params);
				error += StrictMath.pow(actual - predicted, 2.);
			}
			error /= dataTable.length;

			final double penalty = calculatePenalty(params, nonLinearConstraint);

			return error + penalty;
		};
		//objective function: Mean Absolute Error
		final MultivariateFunction objectiveMAE = (params) -> {
			double error = 0.;
			for(int i = 0, length = dataTable.length; i < length; i ++){
				final double[] row = dataTable[i];

				final double actual = row[row.length - 1];
				final double predicted = function.evaluate(row, params);
				error += Math.abs(actual - predicted);
			}
			error /= dataTable.length;

			final double penalty = calculatePenalty(params, nonLinearConstraint);

			return error + penalty;
		};

		// Soluzione iniziale per i parametri
		final double[] initialGuess = {1., 1., 1.};

		// Risolvi il problema di ottimizzazione
		final double[] solution = optimize(objectiveMSE, linearConstraints, nonLinearConstraint, initialGuess);


		//Optimal Parameters: [-1.1310510817011101, -17.434973897838685, 26.984627983790357]
		//Optimal Parameters (w/ nlc): [3.7270816439445933, 6.185209157381032, 2.0015957280550607]
		System.out.println("Optimal Parameters: " + Arrays.toString(solution));
	}

	//https://commons.apache.org/proper/commons-jexl/reference/syntax.html
	private static ModelFunction parseFunction(final String expression){
		final String updatedExpression = cleanExpression(expression);
		final JexlExpression jexlExpression = JEXL_ENGINE.createExpression(updatedExpression);
		final JexlContext context = new MapContext();
		context.set("Math", StrictMath.class);

		return (inputs, params) -> {
			for(int i = 0, length = params.length; i < length; i ++)
				context.set("p" + i, params[i]);
			for(int i = 0, length = inputs.length; i < length; i ++)
				context.set("x" + i, inputs[i]);
			return ((Number)jexlExpression.evaluate(context))
				.doubleValue();
		};
	}

	private static Set<String> extractVariables(final String expression){
		final JexlScript script = JEXL_ENGINE.createScript(expression);
		final Set<List<String>> variables = script.getVariables();
		final Set<String> vars = new HashSet<>(variables.size());
		for(final List<String> list : variables){
			final StringJoiner sj = new StringJoiner(".");
			for(int j = 0, components = list.size(); j < components; j ++)
				sj.add(list.get(j));
			vars.add(sj.toString());
		}
		return vars;
	}

	private static String cleanExpression(final String expression){
		return convertPowerToMathPow(expression)
			.replaceAll("\\\\pi", "Math.PI")
			.replaceAll("\\\\e", "Math.E")
			.replaceAll("sin\\s*\\(", "Math.sin(")
			.replaceAll("cos\\s*\\(", "Math.cos(")
			.replaceAll("tan\\s*\\(", "Math.tan(")
			.replaceAll("asin\\s*\\(", "Math.asin(")
			.replaceAll("acos\\s*\\(", "Math.acos(")
			.replaceAll("atan\\s*\\(", "Math.atan(")
			.replaceAll("atan2\\s*\\(", "Math.atan2(")
			.replaceAll("exp\\s*\\(", "Math.exp(")
			.replaceAll("log\\s*\\(", "Math.log(")
			.replaceAll("log10\\s*\\(", "Math.log10(")
			.replaceAll("sqrt\\s*\\(", "Math.sqrt(")
			.replaceAll("cbrt\\s*\\(", "Math.cbrt(")
			.replaceAll("ceil\\s*\\(", "Math.ceil(")
			.replaceAll("floor\\s*\\(", "Math.floor(")
			.replaceAll("pow\\s*\\(", "Math.pow(")
			.replaceAll("round\\s*\\(", "Math.round(")
			.replaceAll("floorDiv\\s*\\(", "Math.floorDiv(")
			.replaceAll("floorMod\\s*\\(", "Math.floorMod(")
			.replaceAll("ceilDiv\\s*\\(", "Math.ceilDiv(")
			.replaceAll("ceilMod\\s*\\(", "Math.ceilMod(")
			.replaceAll("abs\\s*\\(", "Math.abs(")
			.replaceAll("max\\s*\\(", "Math.max(")
			.replaceAll("min\\s*\\(", "Math.min(")
			.replaceAll("clamp\\s*\\(", "Math.clamp(")
			.replaceAll("signum\\s*\\(", "Math.signum(")
			.replaceAll("sinh\\s*\\(", "Math.sinh(")
			.replaceAll("cosh\\s*\\(", "Math.cosh(")
			.replaceAll("tanh\\s*\\(", "Math.tanh(")
			.replaceAll("hypot\\s*\\(", "Math.hypot(")
			;
	}

	private static String convertPowerToMathPow(final String expression){
		final StringBuilder result = new StringBuilder();
		for(int i = 0; i < expression.length(); i ++){
			final char current = expression.charAt(i);
			if(current == '^'){
				final int baseStart = findBaseStart(expression, i - 1);
				final String base = expression.substring(baseStart, i)
					.trim();

				final int exponentEnd = findExponentEnd(expression, i + 1);
				final String exponent = expression.substring(i + 1, exponentEnd)
					.trim();

				result.replace(baseStart, i, "pow(" + base + ", ")
					.append(exponent)
					.append(")");

				i = exponentEnd - 1;
			}
			else
				result.append(current);
		}
		return result.toString();
	}

	private static int findBaseStart(final String expression, final int index){
		int openParentheses = 0;
		for(int i = index; i >= 0; i --){
			final char c = expression.charAt(i);
			if(c == ')')
				openParentheses ++;
			else if(openParentheses > 0 && c == '(')
				openParentheses --;
			else if(openParentheses == 0 && (c == '+' || c == '-' || c == '*' || c == '/' || c == ' ' || c == '('))
				return i + 1;
		}
		return 0;
	}

	private static int findExponentEnd(final String expression, final int index){
		int openParentheses = 0;
		for(int i = index; i < expression.length(); i ++){
			final char c = expression.charAt(i);
			if(c == '(')
				openParentheses ++;
			else if(openParentheses > 0 && c == ')')
				openParentheses --;
			else if(openParentheses == 0 && (c == '+' || c == '-' || c == '*' || c == '/' || c == ' ' || c == ')'))
				return i;
		}
		return expression.length();
	}

	private static double calculatePenalty(final double[] params, final MultivariateVectorFunction nonLinearConstraint){
		double penalty = 0.;
		final double[] nonLinearConstraints = nonLinearConstraint.value(params);
		for(int i = 0, length = nonLinearConstraints.length; i < length; i ++)
			penalty += StrictMath.pow(nonLinearConstraints[i], 2.);
		return penalty;
	}

	//https://stackoverflow.com/questions/16950115/apache-commons-optimization-troubles
	private static double[] optimize(final MultivariateFunction objective, final LinearConstraintSet linearConstraints,
			final MultivariateVectorFunction nonLinearConstraint, final double[] initialGuess){
		//numberOfInterpolationPoints must be in [n + 2, (n + 1) · (n + 2) / 2]
		final BOBYQAOptimizer optimizer = new BOBYQAOptimizer(2 * initialGuess.length + 1);

		final PointValuePair result = optimizer.optimize(
			new ObjectiveFunction(objective),
			GoalType.MINIMIZE,
			new InitialGuess(initialGuess),
			linearConstraints,
//			new NonLinearConstraint(nonLinearConstraint),
			SimpleBounds.unbounded(initialGuess.length),
			new MaxEval(1000)
		);

		return result.getPoint();
	}

}
