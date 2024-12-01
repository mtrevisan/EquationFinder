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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;


public class KarvaToInfixConverter{

	private static final String PARENTHESIS_OPEN = "(";
	private static final String PARENTHESIS_CLOSE = ")";
	private static final String SPACE = " ";
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
	 * @param karvaExpression	The Karva expression in prefix notation (e.g., "+xy").
	 * @return	The equivalent infix expression (e.g., "(x + y)").
	 * @throws IllegalArgumentException	If the Karva expression is invalid.
	 */
	public static String convertKarvaToInfix(final KarvaExpression karvaExpression){
		final Deque<String> stack = new ArrayDeque<>();
		final StringBuilder infix = new StringBuilder();

		//process the Karva expression tail from right to left
		fillTail(karvaExpression, stack);

		//pointer for tail variables
		int tailIndex = 0;
		//TODO cannot proceed last to first, is has to start from the first
		//process the Karva expression head from right to left
		for(int i = karvaExpression.headLength() - 1; i >= 0; i --){
			final String token = karvaExpression.headAt(i);

			if(!OPERATOR_ARITY.containsKey(token)){
				//if token is not an operator or function, use it as a variable
				if(tailIndex >= karvaExpression.headLength())
					throw new IllegalArgumentException("Not enough variables in the tail for the head expression.");

				//if the token is a variable or number, push it to the stack
//				stack.push(karvaExpression.tailAt(tailIndex ++));
				stack.addFirst(token);
			}
			else{
				//determine the arity of the operator
				final int arity = OPERATOR_ARITY.get(token);

				//check if there are enough operands in the stack
				if(stack.size() < arity)
					throw new IllegalArgumentException("Invalid Karva expression: insufficient operands for operator '" + token + "'");

				//pop operands based on the operator's arity
				final String[] operands = new String[arity];
				for(int j = 0; j < arity; j ++)
					operands[j] = stack.remove();
				reverseArray(operands);

				//combine operands into an infix expression
				combineOperands(arity, token, operands, infix);

				//push the new infix expression back onto the stack
				stack.addLast(infix.toString());
			}
		}
		return stack.pop();
	}

	private static void fillTail(final KarvaExpression karvaExpression, final Deque<String> stack){
		for(int i = 0, length = karvaExpression.tailLength(); i < length; i ++)
			stack.push(karvaExpression.tailAt(i));
	}

	/**
	 * Reverses the order of elements in a String[].
	 *
	 * @param array	The input array to reverse.
	 */
	public static void reverseArray(final String[] array){
		int left = 0;
		int right = array.length - 1;
		//swap elements from start to end
		while(left < right){
			final String temp = array[left];
			array[left] = array[right];
			array[right] = temp;

			left ++;
			right --;
		}
	}

	private static void combineOperands(final int arity, final String token, final String[] operands, final StringBuilder infix){
		infix.setLength(0);
		if(arity == 1)
			infix.append(token)
				.append(PARENTHESIS_OPEN)
				.append(operands[0]);
		else if(arity == 2 && SIMPLE_BINARY_FUNCTIONS.contains(token))
			infix.append(PARENTHESIS_OPEN)
				.append(operands[0])
				.append(SPACE)
				.append(token)
				.append(SPACE)
				.append(operands[1]);
		else
			infix.append(token)
				.append(PARENTHESIS_OPEN)
				.append(String.join(COMMA, operands));
		infix.append(PARENTHESIS_CLOSE);
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
