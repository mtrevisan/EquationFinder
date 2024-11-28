package io.github.mtrevisan.equationfinder;

import io.github.mtrevisan.equationfinder.objectives.ObjectiveMA;
import io.github.mtrevisan.equationfinder.objectives.ObjectiveMAR;
import io.github.mtrevisan.equationfinder.objectives.ObjectiveMax;
import io.github.mtrevisan.equationfinder.objectives.ObjectiveMaxR;
import io.github.mtrevisan.equationfinder.objectives.ObjectiveMedA;
import io.github.mtrevisan.equationfinder.objectives.ObjectiveNSE;
import io.github.mtrevisan.equationfinder.objectives.ObjectivePenalty;
import io.github.mtrevisan.equationfinder.objectives.ObjectiveRMSL;
import io.github.mtrevisan.equationfinder.objectives.ObjectiveRSS;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.analysis.MultivariateVectorFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;


//TODO upper/lower bound search
public class ParameterEstimation{

	public static void main(final String[] args) throws IOException{
		final String problemDataURI = "C:\\mauro\\mine\\projects\\EquationFinder\\src\\main\\resources\\test.txt";
		final ProblemData problemData = ProblemExtractor.readProblemData(Paths.get(problemDataURI));

		//model function: f(x, p) = p0 * x0^2 + p1 * sin(x1) + p2
		final String expression = problemData.expression();
		//linear constraints: p0, p1, p2 >= 0
//		final String[] constraints = {
//			"p0 >= 0",
//			"p1 >= 0",
//			"p0 + p1 = 10",
//			"p2 >= 0"
//		};
		final String[] constraints = problemData.constraints();
		//Input-Output table: x1, x2, output
		final String[] dataInput = problemData.dataInput();
		final double[][] dataTable = problemData.dataTable();
		final String searchMetric = problemData.searchMetric();

		final ModelFunction function = ExpressionExtractor.parseExpression(expression, dataInput);
		final int parameterCount = getParameterCount(expression, dataInput);

		final int constraintCount = constraints.length;
		final LinearConstraint[] linearConstraints = new LinearConstraint[constraintCount];
		for(int i = 0; i < constraintCount; i ++)
			linearConstraints[i] = ConstraintExtractor.parseConstraint(constraints[i], parameterCount);
		final LinearConstraintSet linearConstraintsSet = new LinearConstraintSet(linearConstraints);
//		final LinearConstraintSet linearConstraintsSet = new LinearConstraintSet(
//			new LinearConstraint(new double[]{1, 0, 0}, Relationship.GEQ, 0),
//			new LinearConstraint(new double[]{0, 1, 0}, Relationship.GEQ, 0),
//			new LinearConstraint(new double[]{1, 1, 0}, Relationship.LEQ, 10),
//			new LinearConstraint(new double[]{0, 0, 1}, Relationship.GEQ, 0)
//		);
		//nonlinear constraints: 10 - (p0 + p1) >= 0
		final MultivariateVectorFunction nonLinearConstraint = params -> new double[]{
//			10 - (params[0] + params[1])
		};

		final Map<String, Supplier<MultivariateFunction>> objectiveFunctions = new HashMap<>(2);
		objectiveFunctions.put("MA", () -> new ObjectiveMA(function, dataTable));
		objectiveFunctions.put("MAR", () -> new ObjectiveMAR(function, dataTable));
		objectiveFunctions.put("Max", () -> new ObjectiveMax(function, dataTable));
		objectiveFunctions.put("MaxR", () -> new ObjectiveMaxR(function, dataTable));
		objectiveFunctions.put("MedA", () -> new ObjectiveMedA(function, dataTable));
		objectiveFunctions.put("NSE", () -> new ObjectiveNSE(function, dataTable));
		objectiveFunctions.put("RMSL", () -> new ObjectiveRMSL(function, dataTable));
		objectiveFunctions.put("RSS", () -> new ObjectiveRSS(function, dataTable));
		final MultivariateFunction objectiveFunction = objectiveFunctions.get(searchMetric)
			.get();

		// Soluzione iniziale per i parametri
		final double[] initialGuess = {1., 1., 1.};

		// Risolvi il problema di ottimizzazione
		final double[] solution = optimize(objectiveFunction, linearConstraintsSet, nonLinearConstraint, initialGuess);


		//Optimal Parameters: [-1.1310510817011101, -17.434973897838685, 26.984627983790357]
		//Optimal Parameters (w/ nlc): [3.7270816439445933, 6.185209157381032, 2.0015957280550607]
		System.out.println("Optimal Parameters: [3.7270816439445933, 6.185209157381032, 2.0015957280550607]");
		System.out.println("Optimal Parameters: " + Arrays.toString(solution));
	}

	private static int getParameterCount(final String expression, final String[] dataInput){
		final Set<String> parameters = ExpressionExtractor.extractVariables(expression);
		for(int i = 0, inputCount = dataInput.length; i < inputCount; i ++)
			parameters.remove(dataInput[i]);
		return parameters.size();
	}

	//https://stackoverflow.com/questions/16950115/apache-commons-optimization-troubles
	private static double[] optimize(final MultivariateFunction objective, final LinearConstraintSet linearConstraints,
			final MultivariateVectorFunction nonLinearConstraint, final double[] initialGuess){
		//numberOfInterpolationPoints must be in [n + 2, (n + 1) · (n + 2) / 2]
		final BOBYQAOptimizer optimizer = new BOBYQAOptimizer(2 * initialGuess.length + 1);

		final MultivariateFunction nonLinearObjectiveFunction = new ObjectivePenalty(objective, nonLinearConstraint);

		final PointValuePair result = optimizer.optimize(
			GoalType.MINIMIZE,
			new ObjectiveFunction(nonLinearObjectiveFunction),
			linearConstraints,
			SimpleBounds.unbounded(initialGuess.length),
			new InitialGuess(initialGuess),
			new MaxEval(1000)
		);

		return result.getPoint();
	}

}
