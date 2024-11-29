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
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;


public class ParameterEstimation{

	public static void main(final String[] args) throws IOException{
		final String problemDataURI = "C:\\mauro\\mine\\projects\\EquationFinder\\src\\main\\resources\\test.txt";
		final ProblemData problemData = ProblemExtractor.readProblemData(Paths.get(problemDataURI));

		final SearchMode searchMode = problemData.searchMode();
		final String expression = problemData.expression();
		final String[] constraints = problemData.constraints();
		final String[] dataInput = problemData.dataInput();
		final double[][] dataTable = problemData.dataTable();
		final String searchMetric = problemData.searchMetric();

		final Map<String, Supplier<MultivariateFunction>> objectiveFunctions = new HashMap<>(8);
		final ModelFunction function = ExpressionExtractor.parseExpression(expression, dataInput);
		objectiveFunctions.put(ObjectiveMA.OBJECTIVE_MEAN_ABSOLUTE_ERROR, () -> new ObjectiveMA(function, dataTable));
		objectiveFunctions.put(ObjectiveMAR.OBJECTIVE_MEAN_ABSOLUTE_RELATIVE_ERROR, () -> new ObjectiveMAR(function, dataTable));
		objectiveFunctions.put(ObjectiveMax.OBJECTIVE_MAXIMUM_ERROR, () -> new ObjectiveMax(function, dataTable));
		objectiveFunctions.put(ObjectiveMaxR.OBJECTIVE_MAXIMUM_RELATIVE_ERROR, () -> new ObjectiveMaxR(function, dataTable));
		objectiveFunctions.put(ObjectiveMedA.OBJECTIVE_MEDIANT_ABSOLUTE_ERROR, () -> new ObjectiveMedA(function, dataTable));
		objectiveFunctions.put(ObjectiveNSE.OBJECTIVE_NASH_SUTCLIFFE_EFFICIENCY, () -> new ObjectiveNSE(function, dataTable));
		objectiveFunctions.put(ObjectiveRMSL.OBJECTIVE_ROOT_MEAN_SQUARED_LOG_ERROR, () -> new ObjectiveRMSL(function, dataTable));
		objectiveFunctions.put(ObjectiveRSS.OBJECTIVE_RESIDUAL_SUM_OF_SQUARES_ERROR, () -> new ObjectiveRSS(function, dataTable));
		final MultivariateFunction objective = objectiveFunctions.get(searchMetric)
			.get();

		final List<String> parameters = ExpressionExtractor.extractVariables(expression);
		final int parameterCount = getParameterCount(parameters, dataInput);
		final double[] lowerBounds = new double[parameterCount];
		final double[] upperBounds = new double[parameterCount];
		Arrays.fill(lowerBounds, Double.NEGATIVE_INFINITY);
		Arrays.fill(upperBounds, Double.POSITIVE_INFINITY);
		final List<Constraint> complexConstraints = new ArrayList<>(0);
		for(int i = 0, constraintCount = constraints.length; i < constraintCount; i ++){
			final String constraintExpression = constraints[i];

			if(!ConstraintExtractor.parseBasicConstraint(constraintExpression, lowerBounds, upperBounds)){
				final Constraint constraint = ConstraintExtractor.parseComplexConstraint(constraintExpression);
				complexConstraints.add(constraint);
			}
		}
		final SimpleBounds bounds = new SimpleBounds(lowerBounds, upperBounds);

		final double[] initialGuess = {1., 1., 1.};

		final Constraint[] complexConstraintsArray = complexConstraints.toArray(new Constraint[complexConstraints.size()]);
		final MultivariateFunction objectiveFunction = new ObjectivePenalty(objective, complexConstraintsArray, searchMode,
			function, dataTable);
		final double[] solution = optimize(objectiveFunction, bounds, initialGuess, 1_000);


		//Optimal Parameters: [-1.1310510817011101, -17.434973897838685, 26.984627983790357]
		//Optimal Parameters: [3.7270816439445933, 6.185209157381032, 2.0015957280550607]
		System.out.println("Optimal Parameters: [3.727081633493055, 6.185209165354761, 2.0015957927716186]");
		System.out.println("Optimal Parameters: " + Arrays.toString(solution));
	}

	private static int getParameterCount(final List<String> parameters, final String[] dataInput){
		final Collection<String> params = new HashSet<>(parameters);
		for(int i = 0, inputCount = dataInput.length; i < inputCount; i ++)
			params.remove(dataInput[i]);
		return params.size();
	}

	//https://stackoverflow.com/questions/16950115/apache-commons-optimization-troubles
	private static double[] optimize(final MultivariateFunction objectiveFunction, final SimpleBounds bounds, final double[] initialGuess,
			final int maxIterations){
		//numberOfInterpolationPoints must be in [n + 2, (n + 1) · (n + 2) / 2]
		final BOBYQAOptimizer optimizer = new BOBYQAOptimizer(2 * initialGuess.length + 1);

		final PointValuePair result = optimizer.optimize(
			GoalType.MINIMIZE,
			new ObjectiveFunction(objectiveFunction),
			bounds,
			new InitialGuess(initialGuess),
			new MaxEval(maxIterations)
		);

		return result.getPoint();
	}

}
