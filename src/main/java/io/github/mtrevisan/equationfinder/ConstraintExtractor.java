package io.github.mtrevisan.equationfinder;

import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.Relationship;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class ConstraintExtractor{

	private static final String EMPTY_STRING = "";
	private static final Pattern PATTERN_SPACE = Pattern.compile("\\s+");
	private static final Pattern PATTERN_TERM = Pattern.compile("([+-]?\\d*\\.?\\d*)\\*?p(\\d+)");
	private static final Pattern PATTERN_SPLIT = Pattern.compile("[<>]?=");


	private ConstraintExtractor(){}


	static LinearConstraint parseConstraint(String expression, final Set<String> parameters){
		expression = PATTERN_SPACE.matcher(expression)
			.replaceAll(EMPTY_STRING);
		final String[] parts = PATTERN_SPLIT.splitWithDelimiters(expression, -1);
		if(parts.length != 3)
			throw new IllegalArgumentException("Invalid constraint format: " + expression);

		final String lhs = parts[0];
		final String operator = parts[1];
		final double rhs = Double.parseDouble(parts[2]);

		final double[] coefficients = new double[parameters.size()];
		//TODO put the coefficient in the right index
		final Matcher matcher = PATTERN_TERM.matcher(lhs);
		while(matcher.find()){
			final String parameter = matcher.group(1);
			final int parameterIndex = Integer.parseInt(matcher.group(2));

			final double coefficient = (parameter.isEmpty() || parameter.equals("+")
				? 1
				: (parameter.equals("-")? -1: Double.parseDouble(parameter)));

			coefficients[parameterIndex] = coefficient;
		}

		// Determina la relazione
		Relationship relationship = switch(operator){
			case ">=" -> Relationship.GEQ;
			case "<=" -> Relationship.LEQ;
			case "=" -> Relationship.EQ;
			default -> throw new IllegalArgumentException("Invalid operator: " + operator);
		};

		return new LinearConstraint(coefficients, relationship, rhs);
	}

}
