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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;


public class GeneticAlgorithm{

//	private static final int POPULATION_SIZE = 100;
	private static final int MAX_GENERATIONS = 200;
	private static final double MUTATION_RATE = 0.2;
	private static final double CROSSOVER_RATE = 0.7;


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


	public static void main(final String[] args){
		final Random random = new Random();

		// Dati di input e output
		final double[][] inputs = {
			{1, 2},
			{2, 3},
			{3, 4}
		};
		// Ad esempio, una funzione lineare f(x) = x0 + x1
		final double[] outputs = {3, 5, 7};

		//initialize population:
		final List<Function> population = new ArrayList<>();
		population.add(new Parameter(random.nextInt(2)));
		population.add(new Variable(random.nextInt(2)));
		population.add(new Constant(random.nextInt(2)));
		//generate a random operation
		population.add(new Operation("+", new Parameter(random.nextInt(2)), new Parameter(random.nextInt(2))));
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

		//genetic algorithm
		for(int generation = 0; generation < MAX_GENERATIONS; generation ++){
			//evaluation
			population.sort(Comparator.comparingDouble(f -> fitness(f, inputs, outputs)));

			//selection
			final List<Function> newPopulation = new ArrayList<>();
			for(int i = 0; i < Math.max(population.size() >> 1, 1); i ++)
				newPopulation.add(tournamentSelection(population, inputs, outputs));

			//crossover and mutation
			for(int i = 0; i < Math.max(population.size() >> 1, 1); i ++){
				if(population.size() > 1 && Math.random() < CROSSOVER_RATE){
					final Function parent1 = newPopulation.get(random.nextInt(newPopulation.size()));
					final Function parent2 = newPopulation.get(random.nextInt(newPopulation.size()));
					newPopulation.add(parent1.crossover(parent2));
				}
				if(population.size() == 1 || Math.random() < MUTATION_RATE){
					final Function mutant = newPopulation.get(random.nextInt(newPopulation.size()))
						.mutate();
					newPopulation.add(mutant);
				}
			}

			//update population
			population.clear();
			population.addAll(newPopulation);

			//best solution
			final Function best = population.getFirst();
			final double error = fitness(best, inputs, outputs);
			System.out.println("Generation " + generation + ": " + best + " with error " + error);

			if(error < 1.e-6)
				//stopping criterion
				break;
		}
	}

	// Calcola l'errore (fitness)
	private static double fitness(final Function f, final double[][] inputs, final double[] outputs){
		try{
			double error = 0.;
			// Inizializzazione dei parametri
			final double[] p = {1, 1};
			for(int i = 0; i < inputs.length; i ++)
				error += Math.pow(f.evaluate(p, inputs[i]) - outputs[i], 2);
			return error;
		}
		catch(final Exception e){
//			System.err.println("Error while evaluating function: " + f);
//			e.printStackTrace();

			//heavily penalizes invalid functions
			return Double.MAX_VALUE;
		}
	}

	private static Function tournamentSelection(final List<Function> population, double[][] inputs, double[] outputs){
		final Random random = new Random();
		Function best = null;
		//randomly select a subset of the population for the tournament
		for(int i = 0; i < 5; i ++){
			final Function contender = population.get(random.nextInt(population.size()));
			if(best == null || fitness(contender, inputs, outputs) < fitness(best, inputs, outputs))
				best = contender;
		}
		return best;
	}

}
