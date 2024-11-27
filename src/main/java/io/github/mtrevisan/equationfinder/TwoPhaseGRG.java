package io.github.mtrevisan.equationfinder;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.Arrays;


//https://github.com/ishank011/grgdescent/blob/master/reduced_gradient.py
//https://onlinelibrary.wiley.com/doi/10.1155/2010/976529
//http://www.numdam.org/item/RO_1974__8_3_73_0.pdf
//pag 429 https://industri.fatek.unpatti.ac.id/wp-content/uploads/2019/03/018-Engineering-Optimization-Theory-and-Practice-Singiresu-S.-Rao-Edisi-4-2009.pdf

/**
 * Two-Phase Generalized Reduced Gradient
 *
 * <p>
 * 	min f(X)
 * subject to
 * 	h_j(X) <= 0,					for j=1..m
 * 	l_k(X) = 0,						for k=1..l
 * 	x_i^(l) <= x_i <= x_i^(u),	for i=1..n
 *
 * or, equivalently,
 *
 * 	min f(X)
 * subject to
 * 	h_j(X) + x_n+j = 0,			for j=1..m
 * 	h_k(X) = 0,						for k=1..l
 * 	x_i^(l) <= x_i <= x_i^(u),	for i=1..n
 * 	x_n+j >= 0,						for j=1..m
 *
 * or, equivalently,
 *
 * 	min f(X)
 * subject to
 * 	g_j(X) = 0,						for j=1..m+l
 * 	x_i^(l) <= x_i <= x_i^(u),	for i=1..n+m
 * where the lower and upper bounds on the slack variable, x_i, are taken as 0 and a large number (infinity), respectively (i = n + 1,
 * n + 2, ... , n + m).
 * </p>
 */
public class TwoPhaseGRG{

	// Parameters for GRG
	private static final int MAX_ITERATIONS = 50;
	private static final int MAX_INNER_ITERATIONS = 10;
	private static final double ALPHA0 = 1.;
	private static final double GAMMA = 0.4;
	private static final double NORM1_EPSILON = 0.0001;
	private static final double LINEAR_EPSILON = 0.0001;
	private static final double MULTIPLICATIVE_EPSILON = 0.0001;
	private static final double GRADIENT_STEP_SIZE = 1.e-6;


	public static void main(final String[] args) throws Exception{
		// Example: Input-Output table
		final double[][] table = {
			{1, 2, 3, 10},  // x1, x2, x3, output
			{4, 5, 6, 20},
			{7, 8, 9, 30}
		};

		// Example constraints and objective function
		final Constraint[] constraints = {
			vars -> 20 - vars[0] * vars[0] - vars[1] * vars[1],	// Example: x1^2 + x2^2 <= 20
			vars -> 7 - vars[0] - vars[2],								// Example: x1 + x3 <= 7
			vars -> 3 - vars[0]												// Example: x1 <= 3
		};

		//Mean Squared Error
		final ObjectiveFunction objective = vars -> 4 * vars[0] - vars[1] * vars[1] + vars[2] * vars[2] - 12;


		//solve using Two-Phase GRG
		//x1 = 2.5, x2 = 2, x3 = 1.5
		final double[] initialGuess = {2., 4., 5.};
		final double[] solution = optimize(initialGuess, objective, constraints);

		//print the result
		System.out.println("Optimal Solution: [2.500017133244364, 3.708101175446666, 4.499982866755634]");
		System.out.println("Optimal Solution: " + Arrays.toString(solution));
	}

	public static void main2(final String[] args) throws Exception{
		final ModelFunction linearModel = (x, params) -> params[0] + params[1] * x;
		final Constraint[] constraints = {
			params -> 20 - params[0],	// Example: params[0] <= 20
			params -> 7 - params[1]	// Example: params[1] <= 7
		};
		final double[] initialParams = {0., 0.};
		final double[][] data = {
			{1, 2},
			{2, 4.1},
			{3, 6},
			{4, 7.9}
		};
		//Mean Squared Error
		final ObjectiveFunction objective = params -> {
			double error = 0.;
			for(final double[] point : data){
				final double predicted = linearModel.evaluate(point[0], params);
				error += StrictMath.pow(point[1] - predicted, 2);
			}
			return error;
		};
		final double[] solution = optimize(initialParams, objective, constraints);

		//print the result
		System.out.println("Optimal Solution: " + Arrays.toString(solution));
	}

	private static double[] optimize(final double[] initialGuess, final ObjectiveFunction objective, final Constraint[] constraints)
			throws Exception{
		//Phase 1: Find a feasible solution
		final double[] feasibleSolution = findFeasibleSolution(initialGuess, constraints);

		//Phase 2: Optimize the objective function while maintaining feasibility
		return optimizeObjective(feasibleSolution, objective, constraints);
	}

	private static double[] findFeasibleSolution(final double[] initialGuess, final Constraint[] constraints) throws Exception{
		RealVector currentSolution = new ArrayRealVector(initialGuess);
		final int inputDimension = initialGuess.length;
		final int constraintsDimension = constraints.length;
		final double[][] jacobianMatrix = new double[constraintsDimension][inputDimension];

		final ArrayRealVector v = new ArrayRealVector(constraintsDimension);
		for(int iter = 0; iter < MAX_ITERATIONS; iter ++){
			final double[] currentSolutionArray = currentSolution.toArray();

			final double violationSum = getViolationSum(constraints, currentSolutionArray);
			//if no violations, feasible solution is found
			if(violationSum < NORM1_EPSILON)
				return currentSolutionArray;


			//compute gradients of constraints
			final RealMatrix jacobian = computeJacobian(constraints, currentSolutionArray, jacobianMatrix);

			//compute direction for feasibility search
			final RealVector direction = jacobian.transpose()
				.operate(v);

			//line search to minimize violations
			double alpha = ALPHA0;
			while(alpha > MULTIPLICATIVE_EPSILON){
				final RealVector candidateSolution = currentSolution.add(direction.mapMultiply(alpha));

				final double candidateViolationSum = getViolationSum(constraints, candidateSolution.toArray());
				if(candidateViolationSum < violationSum){
					currentSolution = candidateSolution;
					break;
				}

				alpha *= GAMMA;
			}
		}

		throw new Exception("Failed to find a feasible solution.");
	}

	private static double getViolationSum(final Constraint[] constraints, final double[] solution){
		double violationSum = 0.;
		for(int i = 0, constraintsDimension = constraints.length; i < constraintsDimension; i ++)
			violationSum += Math.max(0., -constraints[i].evaluate(solution));
		return violationSum;
	}

	private static double[] optimizeObjective(final double[] initialGuess, final ObjectiveFunction objective,
			final Constraint[] constraints){
		RealVector currentSolution = new ArrayRealVector(initialGuess);

		final int inputDimension = initialGuess.length;
		final int constraintsDimension = constraints.length;
		final int nonBasicVarDimension = Math.max(inputDimension - constraintsDimension, 0);
		final double[] gradientMatrix = new double[inputDimension];
		final double[][] jacobianMatrix = new double[constraintsDimension][inputDimension];
		final double[] constraintsVector = new double[constraintsDimension];

		for(int iter = 0; iter < MAX_ITERATIONS; iter ++){
			final double[] currentSolutionArray = currentSolution.toArray();

			final double currentObjective = objective.evaluate(currentSolutionArray);

			//Step 1:
			//compute gradient (∇F) of the objective function
			final RealVector gradient = computeGradient(objective, currentSolutionArray, gradientMatrix);

			final RealVector direction;
			if(nonBasicVarDimension > 0){
				//compute Jacobian of constraints
				final RealMatrix jacobian = computeJacobian(constraints, currentSolutionArray, jacobianMatrix);

				final RealMatrix jacobianBasicVars = jacobian.getSubMatrix(
					0, constraintsDimension - 1,
					0, nonBasicVarDimension - 1);
				final RealMatrix jacobianNonBasicVars = jacobian.getSubMatrix(
					0, constraintsDimension - 1,
					nonBasicVarDimension, inputDimension - 1);

				//compute reduced gradient
				final RealMatrix jacobianNonBasicVarsInverse = new LUDecomposition(jacobianNonBasicVars)
					.getSolver()
					.getInverse();
				final RealVector deltaFCap = gradient.getSubVector(0, nonBasicVarDimension);
				final RealVector deltaFBar = gradient.getSubVector(nonBasicVarDimension, inputDimension - nonBasicVarDimension);
				final RealVector reducedGradient = deltaFCap.subtract(jacobianNonBasicVarsInverse.multiply(jacobianBasicVars)
					.preMultiply(deltaFBar));


				//Step 2:
				//check for convergence
				if(isConverged(reducedGradient))
					break;

				//search direction
				final RealVector directionBar = reducedGradient.mapMultiply(-1);
				final RealVector directionCap = jacobianNonBasicVarsInverse.operate(jacobianBasicVars.operate(directionBar))
					.mapMultiply(-1);
				direction = directionBar.append(directionCap)
					.getSubVector(0, inputDimension);
			}
			else{
				//Step 2:
				//check for convergence
				if(isConverged(gradient))
					break;

				//search direction
				direction = gradient.mapMultiply(-1);
			}


			//Step 3:
			//line search
			double alpha = ALPHA0;
			boolean improved = false;
			while(!improved && alpha > MULTIPLICATIVE_EPSILON){
				//Step 3a:
				RealVector candidateSolution = currentSolution.add(direction.mapMultiply(alpha));
				final RealVector candidateSolutionBar = candidateSolution.getSubVector(0, nonBasicVarDimension);
				RealVector candidateSolutionCap = candidateSolution.getSubVector(nonBasicVarDimension,
					inputDimension - nonBasicVarDimension);

				for(int j = 0; j < MAX_INNER_ITERATIONS; j ++){
					if(isFeasible(candidateSolution, constraints)){
						final double candidateObjective = objective.evaluate(candidateSolution.toArray());
						if(currentObjective < candidateObjective)
							alpha *= GAMMA;
						else{
							currentSolution = candidateSolution;
							improved = true;
						}
						break;
					}

					//Step 3b:
					final RealMatrix deltaHNext = computeJacobian(constraints, candidateSolution.toArray(), jacobianMatrix);
					final RealMatrix deltaHNextNonBasicVars = deltaHNext.getSubMatrix(
						0, constraintsDimension - 1,
						nonBasicVarDimension, inputDimension - 1);
					final RealMatrix deltaHNextNonBasicVarsInverse = new LUDecomposition(deltaHNextNonBasicVars)
						.getSolver()
						.getInverse();
					final RealVector candidateConstraints = evaluateConstraints(candidateSolution, constraints, constraintsVector);
					final RealVector delta = deltaHNextNonBasicVarsInverse.operate(candidateConstraints);

					//Step 3c:
					candidateSolutionCap = candidateSolutionCap.subtract(delta);
					candidateSolution = concatenate(candidateSolutionBar, candidateSolutionCap, inputDimension, nonBasicVarDimension);
					if(isConverged(delta)){
						//Step 3d:
						if(isFeasible(candidateSolution, constraints)){
							final double candidateObjective = objective.evaluate(candidateSolution.toArray());
							if(currentObjective < candidateObjective)
								alpha *= GAMMA;
							else{
								currentSolution = candidateSolution;
								improved = true;
							}
						}
						else
							alpha *= GAMMA;
						break;
					}
				}
			}
		}
		return currentSolution.toArray();
	}

	private static boolean isConverged(final RealVector array){
		return (array.getL1Norm() < NORM1_EPSILON);
	}

	private static RealVector evaluateConstraints(final RealVector solution, final Constraint[] constraints, final double[] result){
		for(int i = 0, constraintsDimension = constraints.length; i < constraintsDimension; i ++)
			result[i] = constraints[i].evaluate(solution.toArray());
		return new ArrayRealVector(result);
	}

	private static boolean isFeasible(final RealVector solution, final Constraint[] constraints){
		for(int i = 0, constraintsDimension = constraints.length; i < constraintsDimension; i ++)
			if(Math.abs(constraints[i].evaluate(solution.toArray())) >= LINEAR_EPSILON)
				return false;
		return true;
	}

	private static RealMatrix computeJacobian(final Constraint[] constraints, final double[] solution, final double[][] result){
		final int constraintsDimension = constraints.length;
		final int inputDimension = solution.length;
		for(int i = 0; i < constraintsDimension; i ++)
			for(int j = 0; j < inputDimension; j ++){
				final double[] xPlusH = solution.clone();
				xPlusH[j] += GRADIENT_STEP_SIZE;
				final double[] xMinusH = solution.clone();
				xMinusH[j] -= GRADIENT_STEP_SIZE;

				result[i][j] = (constraints[i].evaluate(xPlusH) - constraints[i].evaluate(xMinusH)) / (2. * GRADIENT_STEP_SIZE);
			}
		return new Array2DRowRealMatrix(result);
	}

	private static RealVector computeGradient(final ObjectiveFunction objective, final double[] solution, final double[] result){
		for(int i = 0, inputDimension = solution.length; i < inputDimension; i ++){
			final double[] xPlusH = solution.clone();
			xPlusH[i] += GRADIENT_STEP_SIZE;
			final double[] xMinusH = solution.clone();
			xMinusH[i] -= GRADIENT_STEP_SIZE;

			result[i] = (objective.evaluate(xPlusH) - objective.evaluate(xMinusH)) / (2. * GRADIENT_STEP_SIZE);
		}
		return new ArrayRealVector(result);
	}

	private static RealVector concatenate(final RealVector v1, final RealVector v2, final int varDimension, final int nonBasicVarDimension){
		final double[] array1 = v1.toArray();
		final double[] array2 = v2.toArray();
		final double[] concatenatedArray = new double[varDimension];
		System.arraycopy(array1, 0, concatenatedArray, 0, nonBasicVarDimension);
		System.arraycopy(array2, 0, concatenatedArray, nonBasicVarDimension, varDimension - nonBasicVarDimension);
		return new ArrayRealVector(concatenatedArray);
	}


	@FunctionalInterface
	interface ModelFunction {
		double evaluate(double x, double[] params);
	}

	@FunctionalInterface
	interface ObjectiveFunction{
		double evaluate(double[] vars);
	}

	@FunctionalInterface
	interface Constraint{
		double evaluate(double[] vars);
	}

}
