/**
 * Copyright (c) 2021 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.equationfinder;

import io.github.mtrevisan.equationfinder.genetics.KarvaExpression;
import io.github.mtrevisan.equationfinder.genetics.KarvaToInfixConverter;
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
import java.util.Random;
import java.util.function.BiFunction;


//https://www.dtreg.com/methodology/view/gene-expression-programming
//	https://www.dtreg.com/uploaded/downloadfile/DownloadFile_5.pdf
//https://github.com/ShuhuaGao/geppy
//gene manipulation: selection, replication,recombination,mutation,inversion,IS/RIS transposition
public class GeneticAlgorithm{

	private static final String[] OPERATORS;
	static{
		final List<String> ops = new ArrayList<>();
		//basic operators
		ops.add("+");
		ops.add("-");
		ops.add("*");
		ops.add("/");

		//trigonometric functions
		ops.add("sin");
		ops.add("cos");
		ops.add("tan");
		ops.add("asin");
		ops.add("acos");
		ops.add("atan");
		ops.add("atan2");

		//hyperbolic functions
		ops.add("sinh");
		ops.add("cosh");
		ops.add("tanh");

		//exponential and logarithmic functions
		ops.add("exp");
		ops.add("log");
		ops.add("sqrt");
		ops.add("cbrt");
		ops.add("pow");
		ops.add("hypot");

		//other mathematical functions
		ops.add("ceil");
		ops.add("floor");
		ops.add("round");
		ops.add("floorDiv");
		ops.add("floorMod");
		ops.add("ceilDiv");
		ops.add("ceilMod");
		ops.add("abs");
		ops.add("clamp");
		ops.add("signum");

		//logical functions
		ops.add("max");
		ops.add("min");

		OPERATORS = ops.toArray(new String[ops.size()]);
	}

	private static final Map<String, BiFunction<ModelFunction, double[][], MultivariateFunction>> OBJECTIVE_FUNCTIONS = new HashMap<>(8);
	static{
		OBJECTIVE_FUNCTIONS.put(ObjectiveMA.OBJECTIVE_MEAN_ABSOLUTE_ERROR, (function, dataTable) -> new ObjectiveMA(function, dataTable));
		OBJECTIVE_FUNCTIONS.put(ObjectiveMAR.OBJECTIVE_MEAN_ABSOLUTE_RELATIVE_ERROR, (function, dataTable) -> new ObjectiveMAR(function, dataTable));
		OBJECTIVE_FUNCTIONS.put(ObjectiveMax.OBJECTIVE_MAXIMUM_ERROR, (function, dataTable) -> new ObjectiveMax(function, dataTable));
		OBJECTIVE_FUNCTIONS.put(ObjectiveMaxR.OBJECTIVE_MAXIMUM_RELATIVE_ERROR, (function, dataTable) -> new ObjectiveMaxR(function, dataTable));
		OBJECTIVE_FUNCTIONS.put(ObjectiveMedA.OBJECTIVE_MEDIANT_ABSOLUTE_ERROR, (function, dataTable) -> new ObjectiveMedA(function, dataTable));
		OBJECTIVE_FUNCTIONS.put(ObjectiveNSE.OBJECTIVE_NASH_SUTCLIFFE_EFFICIENCY, (function, dataTable) -> new ObjectiveNSE(function, dataTable));
		OBJECTIVE_FUNCTIONS.put(ObjectiveRMSL.OBJECTIVE_ROOT_MEAN_SQUARED_LOG_ERROR, (function, dataTable) -> new ObjectiveRMSL(function, dataTable));
		OBJECTIVE_FUNCTIONS.put(ObjectiveRSS.OBJECTIVE_RESIDUAL_SUM_OF_SQUARES_ERROR, (function, dataTable) -> new ObjectiveRSS(function, dataTable));
	}


	private static final int MAX_GENERATIONS = 200;
	private static final int POPULATION_SIZE = 100;
	private static final double CROSSOVER_PROBABILITY = 0.7;
	private static final double MUTATION_PROBABILITY = 0.2;

	private static final Random RANDOM = new Random(System.currentTimeMillis());


	static abstract class Function{
		abstract double evaluate(double[] params, double[] inputs);

		abstract Function mutate();

		abstract Function crossover(Function other);

		abstract Function optimize(double[] params, double[][] inputs, double[] outputs);

		abstract Function prune();
	}

	static class Parameter extends Function{
		int index;

		Parameter(final int index){
			this.index = index;
		}

		@Override
		double evaluate(final double[] params, final double[] inputs){
			return params[index];
		}

		@Override
		Function mutate(){
			//move to next parameter (cycle)
			return new Parameter(index + 1);
		}

		@Override
		Function crossover(final Function other){
			//no changes for simplicity
			return this;
		}

		@Override
		Function optimize(final double[] params, final double[][] inputs, final double[] outputs){
			//the parameters will be optimized globally
			return this;
		}

		@Override
		Function prune(){
			//no simplification possible for a parameter
			return this;
		}

		@Override
		public String toString(){
			return "p" + index;
		}
	}

	static class Variable extends Function{
		int index;

		Variable(final int index){
			this.index = index;
		}

		@Override
		double evaluate(final double[] params, final double[] inputs){
			return inputs[index];
		}

		@Override
		Function mutate(){
			//change variable
			return new Variable(index == 0? 1: 0);
		}

		@Override
		Function crossover(final Function other){
			//no changes for simplicity
			return this;
		}

		@Override
		Function optimize(final double[] params, final double[][] inputs, final double[] outputs){
			//no optimization needed
			return this;
		}

		@Override
		Function prune(){
			//no simplification possible for a variable
			return this;
		}

		@Override
		public String toString(){
			return "x" + index;
		}
	}

	static class Constant extends Function{
		double value;

		Constant(final double value){
			this.value = value;
		}

		@Override
		double evaluate(final double[] params, final double[] inputs){
			return value;
		}

		@Override
		Function mutate(){
			//small random variation
			return new Constant(value + (Math.random() - 0.5));
		}

		@Override
		Function crossover(final Function other){
			//no change
			return this;
		}

		@Override
		Function optimize(final double[] params, final double[][] inputs, final double[] outputs){
			//no optimization needed
			return this;
		}

		@Override
		Function prune(){
			//no simplification possible for a constant
			return this;
		}

		@Override
		public String toString(){
			return String.valueOf(value);
		}
	}

	static class Operation extends Function{
		String operator;
		Function left;
		Function right;

		Operation(final String operator, final Function left, final Function right){
			this.operator = operator;
			this.left = left;
			this.right = right;
		}

		@Override
		double evaluate(final double[] params, final double[] inputs){
			final double leftValue = left.evaluate(params, inputs);
			final double rightValue = (right != null? right.evaluate(params, inputs): 0);
			return switch(operator){
				case "+" -> leftValue + rightValue;
				case "-" -> leftValue - rightValue;
				case "*" -> leftValue * rightValue;
				case "/" -> (rightValue != 0? leftValue / rightValue: 1);
				case "sin" -> Math.sin(leftValue);
				case "cos" -> Math.cos(leftValue);
				case "exp" -> Math.exp(leftValue);
				case "log" -> (leftValue > 0? Math.log(leftValue): 0);
				default -> 0;
			};
		}

		@Override
		Function mutate(){
			final Random random = new Random();
			if(random.nextDouble() < 0.3){
				//change operator randomly
				final String[] operators = {"+", "-", "*", "/", "sin", "cos", "exp", "log"};
				final String newOperator = operators[random.nextInt(operators.length)];
				Function newRightFunction = null;
				if(right != null
						&& !newOperator.equals("sin") && !newOperator.equals("cos") && !newOperator.equals("exp") && !newOperator.equals("log"))
					newRightFunction = right.mutate();
				return new Operation(newOperator, left.mutate(), newRightFunction);
			}
			if(random.nextBoolean())
				//mutation on the left side
				return new Operation(operator, left.mutate(), right);
			//mutation on the right side
			return (right != null? new Operation(operator, left, right.mutate()): this);
		}

		@Override
		Function crossover(final Function other){
			if(other instanceof Operation otherOp){
				Function newLeftFunction = left;
				Function newRightFunction = right;
				if(Math.random() < 0.5 && left != null)
					//crossover on the left side
					newLeftFunction = left.crossover(otherOp.left);
				else if(right != null)
					//crossover on the right side
					newRightFunction = right.crossover(otherOp.right);
				return new Operation(operator, newLeftFunction, newRightFunction);
			}
			return this;
		}

		@Override
		Function optimize(final double[] params, final double[][] inputs, final double[] outputs){
			//recursively optimize subtrees
			left = left.optimize(params, inputs, outputs);
			right = (right != null? right.optimize(params, inputs, outputs): null);
			return this;
		}

		@Override
		Function prune(){
			//pruning to simplify the function
			left = left.prune();
			if(right != null)
				right = right.prune();

			//pruning rules
			if(operator.equals("+") || operator.equals("-")){
				if(right instanceof Constant && ((Constant)right).value == 0)
					//x + 0 = x
					return left;
				if(left instanceof Constant && ((Constant)left).value == 0)
					//0 + x = x
					return right;
			}
			if(operator.equals("*")){
				if(right instanceof Constant && ((Constant)right).value == 1)
					//x * 1 = x
					return left;
				if(left instanceof Constant && ((Constant)left).value == 1)
					//1 * x = x
					return right;
				if(right instanceof Constant && ((Constant)right).value == 0 || left instanceof Constant && ((Constant)left).value == 0)
					// x * 0 = 0, 0 * x = 0
					return new Constant(0);
			}
			if(operator.equals("/")){
				if(right instanceof Constant && ((Constant)right).value == 1)
					//x / 1 = x
					return left;
			}

			//if both branches are constant, calculate the result
			if(left instanceof Constant && (right == null || right instanceof Constant))
				return new Constant(evaluate(new double[0], new double[0]));

			return this;
		}

		@Override
		public String toString(){
			if(operator.equals("sin") || operator.equals("cos") || operator.equals("exp") || operator.equals("log"))
				return operator + "(" + left + ")";
			return "(" + left + " " + operator + " " + right + ")";
		}
	}


	public static void main(final String[] args) throws IOException{
//		final String problemDataURI = "C:\\mauro\\mine\\projects\\EquationFinder\\src\\main\\resources\\test.txt";
		final String problemDataURI = "C:\\Users\\mauro\\Projects\\EquationFinder\\src\\main\\resources\\\\test.txt";
		final ProblemData problemData = ProblemExtractor.readProblemData(Paths.get(problemDataURI));

		final SearchMode searchMode = problemData.searchMode();
//		final String expression = problemData.expression();
		final String[] constraints = problemData.constraints();
		final String[] dataInput = problemData.dataInput();
		final double[][] dataTable = problemData.dataTable();
		final String searchMetric = problemData.searchMetric();


		//initialize population:
		final int populationSize = 10;
		final int maxDepth = 5;
		final List<KarvaExpression> population = generateInitialPopulation(populationSize, maxDepth, dataInput);

		//evaluate population:
		for(int i = 0; i < population.size(); i ++){
			final KarvaExpression karvaExpression = population.get(i);

			final String expression = KarvaToInfixConverter.convertToEquation(karvaExpression);
			System.out.println("Karva expression " + karvaExpression + ": " + expression);


			final ModelFunction function = ExpressionExtractor.parseExpression(expression, dataInput);
			final MultivariateFunction objective = OBJECTIVE_FUNCTIONS.get(searchMetric)
				.apply(function, dataTable);

			final List<String> parameters = ExpressionExtractor.extractVariables(expression);
			final int parameterCount = getParameterCount(parameters, dataInput);
			if(parameterCount < 2)
				//TODO manage
				continue;

			System.out.println("valid expression: " + expression);

			final double[] lowerBounds = createInitialLowerBounds(parameterCount);
			final double[] upperBounds = createInitialUpperBounds(parameterCount);
			final Constraint[] complexConstraints = createComplexConstraints(constraints, lowerBounds, upperBounds);
			final MultivariateFunction objectiveFunction = new ObjectivePenalty(objective, complexConstraints, searchMode,
				function, dataTable);

			final double[] initialGuess = new double[parameterCount];
			Arrays.fill(initialGuess, 1.);

			final SimpleBounds bounds = new SimpleBounds(lowerBounds, upperBounds);
			final double[] bestParameters = optimize(objectiveFunction, bounds, initialGuess, 1_000);

			//TODO evaluate fit
			final double fitness = objectiveFunction.value(bestParameters);
			System.out.println(fitness);
		}

		//S_best = getBestSolution(population)
		//while(!stopCondition()){
		//	parents = selectParents(population, POPULATION_SIZE)
		//	children = empty_set
		//	for(parent1, parent2 in parents){
		//		child1, child2 = crossover(parent1, parent2, CROSSOVER_PROBABILITY)
		//		children = mutate(child1, MUTATION_PROBABILITY)
		//		children = mutate(child2, MUTATION_PROBABILITY)
		//	}
		//	evaluatePopulation(children)
		//	S_best = getBestSolution(children)
		//	population = replace(population, children)
		//}
		//return S_best

		//initialize population:
//		final List<Function> population = new ArrayList<>();
//		population.add(new Parameter(random.nextInt(2)));
//		population.add(new Variable(random.nextInt(2)));
//		population.add(new Constant(random.nextInt(2)));
//		//generate a random operation
//		population.add(new Operation("+", new Parameter(random.nextInt(2)), new Parameter(random.nextInt(2))));

//		final List<Function> population = new ArrayList<>();
//		for(int i = 0; i < POPULATION_SIZE; i ++){
//			if(random.nextDouble() < 0.5){
//				//generate a simple variable or parameter
//				if(random.nextBoolean())
//					//variables x0, x1, etc.
//					population.add(new Variable(random.nextInt(2)));
//				else
//					//parameters p0, p1, etc.
//					population.add(new Parameter(random.nextInt(2)));
//			}
//			else{
//				//generate a random operation
//				final Function left = new Variable(random.nextInt(2));
//				final Function right = new Parameter(random.nextInt(2));
//				final String[] operators = {"+", "-", "*", "/", "sin", "cos", "exp", "log"};
//				final String operator = operators[random.nextInt(operators.length)];
//				if(operator.equals("sin") || operator.equals("cos") || operator.equals("exp") || operator.equals("log"))
//					//unary operators
//					population.add(new Operation(operator, left, null));
//				else
//					//binary operators
//					population.add(new Operation(operator, left, right));
//			}
//		}
//
//		//genetic algorithm
//		for(int generation = 0; generation < MAX_GENERATIONS; generation ++){
//			//evaluation
//			population.sort(Comparator.comparingDouble(f -> fitness(f, inputs, outputs)));
//
//			//selection
//			final List<KarvaExpression> newPopulation = new ArrayList<>();
//			for(int i = 0; i < Math.max(population.size() >> 1, 1); i ++)
//				newPopulation.add(tournamentSelection(population, inputs, outputs));
//
//			//crossover and mutation
//			for(int i = 0; i < Math.max(population.size() >> 1, 1); i ++){
//				if(population.size() > 1 && Math.random() < CROSSOVER_PROBABILITY){
//					final KarvaExpression parent1 = newPopulation.get(random.nextInt(newPopulation.size()));
//					final KarvaExpression parent2 = newPopulation.get(random.nextInt(newPopulation.size()));
//					newPopulation.add(parent1.crossover(parent2));
//				}
//				if(population.size() == 1 || Math.random() < MUTATION_PROBABILITY){
//					final KarvaExpression mutant = newPopulation.get(random.nextInt(newPopulation.size()))
//						.mutate();
//					newPopulation.add(mutant);
//				}
//			}
//
//			//update population
//			population.clear();
//			population.addAll(newPopulation);
//
//			//best solution
//			final KarvaExpression best = population.getFirst();
//			final double error = fitness(best, inputs, outputs);
//			System.out.println("Generation " + generation + ": " + best + " with error " + error);
//
//			if(error < 1.e-6)
//				//stopping criterion
//				break;
//		}
	}

	private static double[] createInitialLowerBounds(final int parameterCount){
		return createBounds(parameterCount, Double.NEGATIVE_INFINITY);
	}

	private static double[] createInitialUpperBounds(final int parameterCount){
		return createBounds(parameterCount, Double.POSITIVE_INFINITY);
	}

	private static double[] createBounds(final int parameterCount, final double initialValue){
		final double[] bounds = new double[parameterCount];
		Arrays.fill(bounds, initialValue);
		return bounds;
	}

	private static Constraint[] createComplexConstraints(final String[] constraints, final double[] lowerBounds,
			final double[] upperBounds){
		final List<Constraint> complexConstraints = new ArrayList<>(0);
		for(int k = 0, constraintCount = constraints.length; k < constraintCount; k ++){
			final String constraintExpression = constraints[k];

			if(!ConstraintExtractor.parseBasicConstraint(constraintExpression, lowerBounds, upperBounds)){
				final Constraint constraint = ConstraintExtractor.parseComplexConstraint(constraintExpression);
				complexConstraints.add(constraint);
			}
		}
		return complexConstraints.toArray(new Constraint[complexConstraints.size()]);
	}

	/**
	 * Generates an initial population of Karva expressions.
	 *
	 * @param populationSize	The size of the population.
	 * @param maxDepth	The maximum depth of the expressions.
	 * @param inputs	The inputs.
	 * @return	A list of Karva expressions.
	 */
	private static List<KarvaExpression> generateInitialPopulation(final int populationSize, final int maxDepth, final String[] inputs){
		final List<KarvaExpression> population = new ArrayList<>();
		for(int i = 0; i < populationSize; i ++)
			population.add(generateKarvaExpression(maxDepth, inputs));
		return population;
	}

	/**
	 * Generates a random Karva expression.
	 *
	 * @param maxNumberOfOperators	The maximum number of operators.
	 * @return	A Karva expression.
	 */
	private static KarvaExpression generateKarvaExpression(final int maxNumberOfOperators, final String[] inputs){
		//ensure at least 2 nodes
		final int h = RANDOM.nextInt(maxNumberOfOperators - 1) + 2;
		final int maxArgs = 3;
		final int t = h * (maxArgs - 1) + 1;

		final String[] gene = new String[h + t];

		//generate head (operators and functions)
		int index = 0;
		final int inputCount = inputs.length;
		for(int i = 0; i < h; i ++){
			final int type = RANDOM.nextInt(3);
			//add function to head
			if(type == 0){
				final int operatorIndex = RANDOM.nextInt(OPERATORS.length);
				gene[index ++] = OPERATORS[operatorIndex];
			}
			//add variable to head
			else if(type == 1)
				gene[index ++] = inputs[RANDOM.nextInt(inputCount)];
			//add constant to head
			else if(type == 2)
				gene[index ++] = "p" + RANDOM.nextInt(t);
		}

		//generate tail (variables and constants)
		for(int i = 0; i < t; i ++)
			gene[index ++] = inputs[RANDOM.nextInt(inputCount)];

		return new KarvaExpression(gene);
	}

//	private static KarvaExpression tournamentSelection(final List<KarvaExpression> population, double[][] inputs, double[] outputs){
//		final Random random = new Random();
//		KarvaExpression best = null;
//		//randomly select a subset of the population for the tournament
//		for(int i = 0; i < 5; i ++){
//			final KarvaExpression contender = population.get(random.nextInt(population.size()));
//			if(best == null || fitness(contender, inputs, outputs) < fitness(best, inputs, outputs))
//				best = contender;
//		}
//		return best;
//	}

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
