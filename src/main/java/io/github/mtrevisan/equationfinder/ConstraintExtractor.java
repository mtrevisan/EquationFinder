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
package io.github.mtrevisan.equationfinder;

import org.apache.commons.math3.optim.linear.Relationship;

import java.util.regex.Pattern;


public final class ConstraintExtractor{

	private static final Pattern PATTERN_SPLIT = Pattern.compile("[<>]?=");

	private static final String GREATER_OR_EQUAL = ">=";
	private static final String LOWER_OR_EQUAL = "<=";
	private static final String EQUAL = "=";
	private static final String ZERO = "0";
	private static final String MINUS = "-";
	private static final String PARENTHESIS_OPEN = "(";
	private static final String PARENTHESIS_CLOSE = ")";
	private static final String EMPTY = "";


	private ConstraintExtractor(){}


	static boolean parseBasicConstraint(final String expression, final double[] lowerBounds, final double[] upperBounds){
		final String[] parts = PATTERN_SPLIT.splitWithDelimiters(expression, -1);
		try{
			if(parts.length != 3 || !isValidParameter(parts[0]))
				throw new IllegalArgumentException("Invalid constraint format: " + expression);
		}
		catch(final NumberFormatException ignored){
			return false;
		}

		final int parameterIndex = Integer.parseInt(parts[0].trim().substring(1));
		final String operator = parts[1].trim();
		final double rhs = Double.parseDouble(parts[2].trim());

		switch(operator){
			case GREATER_OR_EQUAL -> lowerBounds[parameterIndex] = rhs;
			case LOWER_OR_EQUAL -> upperBounds[parameterIndex] = rhs;
			default -> throw new IllegalArgumentException("Invalid operator: " + operator);
		}
		return true;
	}

	private static boolean isValidParameter(final String parameter){
		return (parameter.length() > 1
			&& parameter.charAt(0) == 'p'
			&& Integer.parseInt(parameter.substring(1)) >= 0);
	}

	static Constraint parseComplexConstraint(final String expression){
		final String[] parts = PATTERN_SPLIT.splitWithDelimiters(expression, -1);
		if(parts.length != 3)
			throw new IllegalArgumentException("Invalid constraint format: " + expression);

		final String lhs = parts[0].trim();
		final String operator = parts[1].trim();
		final String rhs = parts[2].trim();

		final Relationship relationship = switch(operator){
			case GREATER_OR_EQUAL -> Relationship.GEQ;
			case LOWER_OR_EQUAL -> Relationship.LEQ;
			case EQUAL -> Relationship.EQ;
			default -> throw new IllegalArgumentException("Invalid operator: " + operator);
		};

		final String constraintExpression = lhs
			+ (!rhs.equals(ZERO)? MINUS + PARENTHESIS_OPEN + rhs + PARENTHESIS_CLOSE: EMPTY);
		final ParameterConstraintFunction function = ExpressionExtractor.parseParameterConstraintExpression(constraintExpression);
		return new Constraint(function, relationship);
	}

}
