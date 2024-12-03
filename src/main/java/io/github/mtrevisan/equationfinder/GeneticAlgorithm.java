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
	private static final double MATING_RATIO = 0.5;
	private static final double MUTATION_PROBABILITY = 0.044;
	private static final double INVERSION_PROBABILITY = 0.1;
	private static final double TRANSPOSITION_PROBABILITY = 0.1;
	private static final double ONE_POINT_RECOMBINATION_PROBABILITY = 0.3;
	private static final double TWO_POINT_RECOMBINATION_PROBABILITY = 0.3;
	private static final double CROSSOVER_PROBABILITY = 0.7;

	private static final Random RANDOM = new Random(System.currentTimeMillis());


	public static void main(final String[] args) throws IOException{
		final String problemDataURI = "C:\\mauro\\mine\\projects\\EquationFinder\\src\\main\\resources\\test.txt";
//		final String problemDataURI = "C:\\Users\\mauro\\Projects\\EquationFinder\\src\\main\\resources\\\\test.txt";
		final ProblemData problemData = ProblemExtractor.readProblemData(Paths.get(problemDataURI));

		final SearchMode searchMode = problemData.searchMode();
//		final String expression = problemData.expression();
		final String[] constraints = problemData.constraints();
		final String[] dataInput = problemData.dataInput();
		final double[][] dataTable = problemData.dataTable();
		final String searchMetric = problemData.searchMetric();


		//initialize population
		final int maxDepth = 5;
		List<KarvaExpression> population = generateInitialPopulation(POPULATION_SIZE, maxDepth, dataInput);

		//initialize problem
		List<OptimizationProblem> optimizationProblems = generateOptimizationProblems(problemData, population);

		//evaluate population
		Map<OptimizationProblem, Double> fitnessScore = evaluate(optimizationProblems);

		//get best solution
		Map.Entry<OptimizationProblem, Double> bestSolution = getBestSolution(fitnessScore);
		System.out.println("best solution: " + bestSolution.getKey().expression + ", fitness: "+ bestSolution.getValue());

		//apply genetic algorithm:
		for(int generation = 0; bestSolution.getValue() >= 1.e-6 && generation < MAX_GENERATIONS; generation ++){
			//select parents:
			final int tournamentSize = (int)Math.max(optimizationProblems.size() * MATING_RATIO, 1);
			final List<OptimizationProblem> children = tournamentSelection(optimizationProblems, tournamentSize);

			//generate new offsprings (via mutation, inversion, transposition, or recombination):
//			parents = selectParents(population, POPULATION_SIZE)
//			for(parent1, parent2 in parents){
//				child1, child2 = crossover(parent1, parent2, CROSSOVER_PROBABILITY)
//				children = mutate(child1, MUTATION_PROBABILITY)
//				children = mutate(child2, MUTATION_PROBABILITY)
//			}

			//evaluate children population
			fitnessScore = evaluate(children);

			//get best solution
			bestSolution = getBestSolution(fitnessScore);
			System.out.println("best solution: " + bestSolution.getKey().expression + ", fitness: "+ bestSolution.getValue());

			//update population:
			optimizationProblems.clear();
			optimizationProblems.addAll(children);
		}
		//return bestSolution


//			//crossover and mutation
//			for(int i = 0; i < Math.max(population.size() >> 1, 1); i ++){
//				if(population.size() > 1 && Math.random() < CROSSOVER_PROBABILITY){
//					final KarvaExpression parent1 = newPopulation.get(RANDOM.nextInt(newPopulation.size()));
//					final KarvaExpression parent2 = newPopulation.get(RANDOM.nextInt(newPopulation.size()));
//					newPopulation.add(parent1.crossover(parent2));
//				}
//				if(population.size() == 1 || Math.random() < MUTATION_PROBABILITY){
//					final KarvaExpression mutant = newPopulation.get(RANDOM.nextInt(newPopulation.size()))
//						.mutate();
//					newPopulation.add(mutant);
//				}
//			}
	}

	private static List<OptimizationProblem> generateOptimizationProblems(final ProblemData problemData,
			final List<KarvaExpression> population){
		final SearchMode searchMode = problemData.searchMode();
//		final String expression = problemData.expression();
		final String[] constraints = problemData.constraints();
		final String[] dataInput = problemData.dataInput();
		final double[][] dataTable = problemData.dataTable();
		final String searchMetric = problemData.searchMetric();

		final List<OptimizationProblem> optimizationProblems = new ArrayList<>(POPULATION_SIZE);
		for(int i = 0; i < POPULATION_SIZE; i ++){
			final KarvaExpression karvaExpression = population.get(i);

			final String expression = KarvaToInfixConverter.convertToEquation(karvaExpression);
//			System.out.println("Karva expression " + karvaExpression + ": " + expression);


			final ModelFunction function = ExpressionExtractor.parseExpression(expression, dataInput);
			final MultivariateFunction objective = OBJECTIVE_FUNCTIONS.get(searchMetric)
				.apply(function, dataTable);

			final List<String> parameters = ExpressionExtractor.extractVariables(expression);
			final int parameterCount = getParameterCount(parameters, dataInput);
			if(parameterCount < 2)
				//TODO manage
				continue;

//			System.out.println("valid expression: " + expression);

			final double[] lowerBounds = createInitialLowerBounds(parameterCount);
			final double[] upperBounds = createInitialUpperBounds(parameterCount);
			final Constraint[] complexConstraints = createComplexConstraints(constraints, lowerBounds, upperBounds);
			final MultivariateFunction objectiveFunction = new ObjectivePenalty(objective, complexConstraints, searchMode,
				function, dataTable);

			final double[] initialGuess = new double[parameterCount];
			Arrays.fill(initialGuess, 1.);

			final SimpleBounds bounds = new SimpleBounds(lowerBounds, upperBounds);
			final OptimizationProblem optimizationProblem = new OptimizationProblem(karvaExpression, expression, objectiveFunction, bounds,
				initialGuess, 1_000);
			optimizationProblems.add(optimizationProblem);
		}
		return optimizationProblems;
	}

	private static Map<OptimizationProblem, Double> evaluate(final List<OptimizationProblem> optimizationProblems){
		final Map<OptimizationProblem, Double> fitnessScore = new HashMap<>(POPULATION_SIZE);
		for(int i = 0, length = optimizationProblems.size(); i < length; i ++){
			final OptimizationProblem optimizationProblem = optimizationProblems.get(i);

			final double[] bestParameters = optimize(optimizationProblem);
			optimizationProblem.setBestParameters(bestParameters);

			final double fitness = calculateFitness(optimizationProblem);
			fitnessScore.put(optimizationProblem, fitness);
		}
		return fitnessScore;
	}

	private static double calculateFitness(final OptimizationProblem optimizationProblem){
		return optimizationProblem.objectiveFunction.value(optimizationProblem.bestParameters);
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
		for(int k = 0, constraintCount = lowerBounds.length; k < constraintCount; k ++){
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
		int parameterCount = 1;
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
				gene[index ++] = "p" + RANDOM.nextInt(parameterCount ++);
		}

		//generate tail (variables and constants)
		for(int i = 0; i < t; i ++)
			gene[index ++] = (RANDOM.nextBoolean()
				? inputs[RANDOM.nextInt(inputCount)]
				: "p" + RANDOM.nextInt(parameterCount));

		return new KarvaExpression(gene);
	}

	//https://en.wikipedia.org/wiki/Tournament_selection
	//https://www.baeldung.com/cs/ga-tournament-selection
	private static List<OptimizationProblem> tournamentSelection(final List<OptimizationProblem> population, final int tournamentSize){
		final List<OptimizationProblem> winningIndividuals = new ArrayList<>(tournamentSize);
		for(int i = 0; i < tournamentSize; i ++){
			//pick `selectionPressure` individuals from `population` at random, with or without replacement, and add them to
			// `competingIndividuals`
			final List<OptimizationProblem> competingIndividuals = selectCompetingIndividuals(population);

			final OptimizationProblem best = tournament(competingIndividuals);
			winningIndividuals.add(best);

			competingIndividuals.clear();
		}
		return winningIndividuals;
	}

	//randomly select the competing individuals
	private static List<OptimizationProblem> selectCompetingIndividuals(final List<OptimizationProblem> population){
		final int selectionPressure = 5;
		final int populationSize = population.size();
		final List<OptimizationProblem> chosenPopulation = new ArrayList<>(population);
		final List<OptimizationProblem> competingIndividuals = new ArrayList<>(selectionPressure);
		for(int i = 0; i < selectionPressure; i ++){
			final OptimizationProblem chosenIndividual = chosenPopulation.get(RANDOM.nextInt(populationSize));
			competingIndividuals.add(chosenIndividual);
			chosenPopulation.remove(chosenIndividual);
		}
		return competingIndividuals;
	}

	/** Select the fittest individual (or chromosome) from a randomly selected list of individuals. */
	private static OptimizationProblem tournament(final List<OptimizationProblem> competingIndividuals){
		OptimizationProblem best = competingIndividuals.getFirst();
		for(int i = 1, length = competingIndividuals.size(); i < length; i ++){
			final OptimizationProblem next = competingIndividuals.get(i);
			if(calculateFitness(next) < calculateFitness(best))
				best = next;
		}
		return best;
	}

	private static int getParameterCount(final List<String> parameters, final String[] dataInput){
	final Collection<String> params = new HashSet<>(parameters);
	for(int i = 0, inputCount = dataInput.length; i < inputCount; i ++)
		params.remove(dataInput[i]);
	return params.size();
}

	//https://stackoverflow.com/questions/16950115/apache-commons-optimization-troubles
	private static double[] optimize(final OptimizationProblem optimizationProblem){
		final MultivariateFunction objectiveFunction = optimizationProblem.objectiveFunction;
		final SimpleBounds bounds = optimizationProblem.bounds;
		final double[] initialGuess = optimizationProblem.initialGuess;
		final int maxIterations = optimizationProblem.maxIterations;

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

	private static Map.Entry<OptimizationProblem, Double> getBestSolution(final Map<OptimizationProblem, Double> fitnessScore){
		return fitnessScore.entrySet()
			.stream()
			.min(Map.Entry.comparingByValue())
			.orElse(null);
	}

	private static KarvaExpression mutate(final KarvaExpression karvaExpression){
		final int geneLength = karvaExpression.length();
		final int originIndex = RANDOM.nextInt(geneLength);
		final int length = RANDOM.nextInt(geneLength - originIndex - 1) + 1;
		return karvaExpression.generateMutation(originIndex, length, OPERATORS);
	}

	public static KarvaExpression invert(final KarvaExpression karvaExpression){
		final int geneLength = karvaExpression.length();
		final int originIndex = RANDOM.nextInt(geneLength);
		final int targetIndex = RANDOM.nextInt(geneLength - originIndex) + originIndex;
		final int length = RANDOM.nextInt(Math.min(geneLength - targetIndex, targetIndex - originIndex) - 1) + 1;
		return karvaExpression.generateInversion(originIndex, targetIndex, length);
	}

	public static KarvaExpression transpose(final KarvaExpression karvaExpression){
		final int geneLength = karvaExpression.length();
		final int length = RANDOM.nextInt(geneLength >> 1) + 1;
		final int originIndex = RANDOM.nextInt(geneLength - length);
		final int targetIndex = RANDOM.nextInt(geneLength - length);
		return karvaExpression.generateTransposition(originIndex, targetIndex, length);
	}

	public static KarvaExpression[] recombineOnePoint(final KarvaExpression karvaExpression1, final KarvaExpression karvaExpression2){
		final int gene1Length = karvaExpression1.length();
		final int gene2Length = karvaExpression2.length();
		final int length = Math.min(gene1Length, gene2Length);
		final int crossoverPoint = RANDOM.nextInt(length);
		return karvaExpression1.generateRecombinationOnePoint(karvaExpression2, crossoverPoint);
	}

	public static KarvaExpression[] recombineTwoPoint(final KarvaExpression karvaExpression1, final KarvaExpression karvaExpression2){
		final int gene1Length = karvaExpression1.length();
		final int gene2Length = karvaExpression2.length();
		final int length = Math.min(gene1Length, gene2Length);
		final int crossoverPoint1 = RANDOM.nextInt(length);
		final int crossoverPoint2 = RANDOM.nextInt(length - crossoverPoint1 - 2) + 2;
		return karvaExpression1.generateRecombinationTwoPoint(karvaExpression2, crossoverPoint1, crossoverPoint2);
	}

}
