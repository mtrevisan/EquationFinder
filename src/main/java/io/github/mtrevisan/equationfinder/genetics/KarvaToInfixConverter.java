/**
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.equationfinder.genetics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;


public class KarvaToInfixConverter{

	private static final String PARENTHESIS_OPEN = "(";
	private static final String PARENTHESIS_CLOSE = ")";
	private static final String COMMA = ",";
	private static final String SIMPLE_BINARY_FUNCTIONS = "+-*/";

	private static final Map<String, Integer> OPERATOR_ARITY = new HashMap<>();
	static{
		//basic operators
		OPERATOR_ARITY.put("+", 2);
		OPERATOR_ARITY.put("-", 2);
		OPERATOR_ARITY.put("*", 2);
		OPERATOR_ARITY.put("/", 2);

		//trigonometric functions
		OPERATOR_ARITY.put("sin", 1);
		OPERATOR_ARITY.put("cos", 1);
		OPERATOR_ARITY.put("tan", 1);
		OPERATOR_ARITY.put("asin", 1);
		OPERATOR_ARITY.put("acos", 1);
		OPERATOR_ARITY.put("atan", 1);
		OPERATOR_ARITY.put("atan2", 2);

		//hyperbolic functions
		OPERATOR_ARITY.put("sinh", 1);
		OPERATOR_ARITY.put("cosh", 1);
		OPERATOR_ARITY.put("tanh", 1);

		//exponential and logarithmic functions
		OPERATOR_ARITY.put("exp", 1);
		OPERATOR_ARITY.put("log", 1);
		OPERATOR_ARITY.put("sqrt", 1);
		OPERATOR_ARITY.put("cbrt", 1);
		OPERATOR_ARITY.put("pow", 2);
		OPERATOR_ARITY.put("hypot", 2);

		//other mathematical functions
		OPERATOR_ARITY.put("ceil", 1);
		OPERATOR_ARITY.put("floor", 1);
		OPERATOR_ARITY.put("round", 1);
		OPERATOR_ARITY.put("floorDiv", 2);
		OPERATOR_ARITY.put("floorMod", 2);
		OPERATOR_ARITY.put("ceilDiv", 2);
		OPERATOR_ARITY.put("ceilMod", 2);
		OPERATOR_ARITY.put("abs", 1);
		OPERATOR_ARITY.put("clamp", 3);
		OPERATOR_ARITY.put("signum", 1);

		//logical functions
		OPERATOR_ARITY.put("max", 2);
		OPERATOR_ARITY.put("min", 2);
	}


	/**
	 * Converts a Karva (prefix) expression into an infix expression.
	 *
	 * @param expr	The Karva expression in prefix notation (e.g., "+xy").
	 * @return	The equivalent infix expression (e.g., "(x + y)").
	 * @throws IllegalArgumentException	If the Karva expression is invalid.
	 */
	public static String convertKarvaToInfix(final KarvaExpression expr){
		final Queue<String> queue = new LinkedList<>(Arrays.asList(expr.head));
		final Queue<String> argsQueue = new LinkedList<>(Arrays.asList(expr.tail));
		return buildExpressionRecursive(queue, argsQueue);
	}

	private static String buildExpressionRecursive(final Queue<String> queue, final Queue<String> argsQueue){
		final String current;
		if(!queue.isEmpty())
			//current node from queue
			current = queue.poll();
		else if(!argsQueue.isEmpty())
			//current node from arguments
			current = argsQueue.poll();
		else
			throw new IllegalStateException("Malformed input: no more tokens to process.");

		final Integer arity = OPERATOR_ARITY.get(current);

		if(arity == null)
			//variable or constant: returns directly
			return current;

		//builds children based on arity
		final StringBuilder expression = new StringBuilder();
		if(SIMPLE_BINARY_FUNCTIONS.contains(current)){
			final String left = buildExpressionRecursive(queue, argsQueue);
			final String right = buildExpressionRecursive(queue, argsQueue);

			expression.append(PARENTHESIS_OPEN)
				.append(left)
				.append(current)
				.append(right);
		}
		else{
			expression.append(current)
				.append(PARENTHESIS_OPEN);
			for(int i = 0; i < arity; i ++){
				final String argument = buildExpressionRecursive(queue, argsQueue);

				if(i > 0)
					expression.append(COMMA);
				expression.append(argument);
			}
		}
		expression.append(PARENTHESIS_CLOSE);
		return expression.toString();
	}


	public static void main(final String[] args){
		final KarvaExpression karvaExpression = new KarvaExpression(new String[]{"sin", "*", "-", "+"}, new String[]{"a", "b", "c", "d"});
		try{
			final String infixExpression = convertKarvaToInfix(karvaExpression);
			//sin((a - b) * (c + d))
			System.out.println("Infix expression: " + infixExpression);
		}
		catch(final IllegalArgumentException iae){
			System.err.println("Error: " + iae.getMessage());
		}
	}

}
