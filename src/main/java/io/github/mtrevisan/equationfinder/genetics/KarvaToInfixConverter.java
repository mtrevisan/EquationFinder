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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;


public final class KarvaToInfixConverter{

	private static final String PARENTHESIS_OPEN = "(";
	private static final String PARENTHESIS_CLOSE = ")";
	private static final String COMMA = ",";
	private static final String EMPTY = "";

	private static final Map<String, Integer> OPERATOR_ARITY = new HashMap<>();
	private static final Set<String> SIMPLE_BINARY_FUNCTIONS = new HashSet<>(4);
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


		SIMPLE_BINARY_FUNCTIONS.add("+");
		SIMPLE_BINARY_FUNCTIONS.add("-");
		SIMPLE_BINARY_FUNCTIONS.add("*");
		SIMPLE_BINARY_FUNCTIONS.add("/");
	}


	private KarvaToInfixConverter(){}


	//TODO remove this class, to the conversion level-order to post-order directly
	static class Node{
		String value;
		LinkedList<Node> children;

		// Constructor for the Node class
		Node(final String value){
			this.value = value;
			children = new LinkedList<>();
		}
	}

	/**
	 * Converts a Karva (prefix) expression into an infix expression.
	 *
	 * @param karva The Karva expression in prefix notation (e.g., "+xy").
	 * @throws IllegalArgumentException If the Karva expression is invalid.
	 * @return The equivalent infix expression (e.g., "(x + y)").
	 */
	public static String convertToEquation(final KarvaExpression karva){
		if(karva.isEmpty())
			return EMPTY;

		final Node root = buildTree(karva);

		final List<String> result = postOrderTraversal(root);

		return toExpression(result);
	}

	private static Node buildTree(final KarvaExpression karva){
		//construct the tree using level-order and arity information
		final LinkedList<Node> queue = new LinkedList<>();
		int index = 0;
		final Node root = new Node(karva.geneAt(index ++));
		queue.add(root);

		//build the N-ary tree
		for(int i = 0, length = karva.length(); i < length; i ++){
			final Node parent = queue.poll();

			final int childrenCount = OPERATOR_ARITY.getOrDefault(karva.geneAt(i), 0);
			for(int j = 0; j < childrenCount; j ++)
				if(index < length){
					final Node child = new Node(karva.geneAt(index ++));
					parent.children
						.add(child);
					queue.add(child);
				}
		}
		return root;
	}

	//Extract Reverse Polish Notation
	private static List<String> postOrderTraversal(final Node root){
		final List<String> result = new LinkedList<>();
		if(root != null){
			final Deque<Node> stack = new ArrayDeque<>(1);
			stack.push(root);
			while(!stack.isEmpty()){
				final Node current = stack.pop();

				result.addFirst(current.value);

				final LinkedList<Node> children = current.children;
				for(int i = 0, length = children.size(); i < length; i ++)
					stack.push(children.get(i));
			}
		}
		return result;
	}

	private static String toExpression(final List<String> result){
		final Deque<String> tokenStack = new ArrayDeque<>(1);
		final StringBuilder expression = new StringBuilder();
		for(int i = 0; i < result.size(); i ++){
			final String token = result.get(i);
			final Integer arity = OPERATOR_ARITY.get(token);
			if(arity == null){
				tokenStack.push(token);
				continue;
			}

			//otherwise, it's an operator or function, so we need to handle it accordingly
			expression.setLength(0);

			//builds children based on arity
			if(SIMPLE_BINARY_FUNCTIONS.contains(token)){
				final String rightOperand = tokenStack.pop();
				final String leftOperand = tokenStack.pop();
				expression.append(PARENTHESIS_OPEN)
					.append(leftOperand)
					.append(token)
					.append(rightOperand)
					.append(PARENTHESIS_CLOSE);
			}
			else{
				final String[] operands = new String[arity];
				for(int j = 0; j < arity; j ++)
					operands[arity - j - 1] = tokenStack.pop();
				final StringJoiner sj = new StringJoiner(COMMA, PARENTHESIS_OPEN, PARENTHESIS_CLOSE);
				for(int j = 0; j < arity; j ++)
					sj.add(operands[j]);
				expression.append(token)
					.append(sj);
			}

			tokenStack.push(expression.toString());
		}

		return (tokenStack.size() == 1? tokenStack.pop(): EMPTY);
	}

}
