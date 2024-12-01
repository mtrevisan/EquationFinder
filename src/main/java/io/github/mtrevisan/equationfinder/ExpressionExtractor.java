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

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.math3.special.Gamma;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @see <a href="https://github.com/000haitham000/tx2ex/tree/master">tx2ex</a>
 */
public final class ExpressionExtractor{

	private static final Pattern PATTERN_FUNCTION_SUBSTITUTION_JAVA = Pattern.compile(
		"\\\\(pi|e)[\\s+\\-*·×/(]|"
			//trigonometric functions
			+ "(sin|cos|tan|asin|acos|atan|atan2"
			//hyperbolic functions
			+ "|sinh|cosh|tanh"
			//exponential functions
			+ "|exp|log|sqrt|cbrt|pow|hypot"
			//other functions
			+ "|ceil|floor|round|floorDiv|floorMod|ceilDiv|ceilMod|abs|clamp|signum"
			//logical functions
			+ "|max|min"
			+ ")\\s*\\("
	);
	private static final Pattern PATTERN_FUNCTION_SUBSTITUTION_APACHE_GAMMA = Pattern.compile("logGamma\\s*\\(");
	private static final Pattern PATTERN_FUNCTION_SUBSTITUTION_APACHE_ERF = Pattern.compile("erf\\s*\\(");
	private static final String CONTEXT_JAVA_MATH = "_math";
	private static final String CONTEXT_APACHE_GAMMA = "_gamma";
	private static final String CONTEXT_APACHE_ERF = "_erf";
	private static final String PARAMETER_PREFIX = "p";
	private static final String DOT = ".";
	private static final String PARENTHESIS_OPEN = "(";
	private static final String PARENTHESIS_CLOSE = ")";
	private static final String COMMA = ",";
	private static final String FUNCTION_POWER = "pow";
	private static final String POWER = "^";
	private static final String POW_FUNCTION = FUNCTION_POWER + PARENTHESIS_OPEN;
	private static final String MULTIPLICATION = "*";
	private static final String BASE_DELIMITERS = "+-*·×/ ,";
	private static final String BASE_START_DELIMITERS = BASE_DELIMITERS + PARENTHESIS_OPEN;
	private static final String BASE_END_DELIMITERS = BASE_DELIMITERS + PARENTHESIS_CLOSE;
	private static final Pattern PATTERN_MULTIPLICATION = Pattern.compile("[·×]");
	private static final Pattern PATTERN_ASINH = Pattern.compile("asinh\\s*\\(");
	private static final Pattern PATTERN_ACOSH = Pattern.compile("acosh\\s*\\(");
	private static final Pattern PATTERN_ATANH = Pattern.compile("atanhh\\s*\\(");
	private static final Pattern PATTERN_SIGN = Pattern.compile("sign\\s*\\(");
	private static final String SIGNUM = "signum(";
	private static final Pattern PATTERN_X = Pattern.compile("x");
	private static final String ASINH = "ln(x + sqrt(x^2 + 1))";
	private static final String ACOSH = "ln(x + sqrt(x^2 - 1))";
	private static final String ATANH = "0.5 * ln((1 + x) / (1 - x))";


	private static final JexlEngine JEXL_ENGINE = new JexlBuilder()
		.cache(512)
		.strict(true)
		//FIXME to put to true
		.silent(false)
		.create();


	private ExpressionExtractor(){}


	//https://commons.apache.org/proper/commons-jexl/reference/syntax.html
	static ParameterConstraintFunction parseParameterConstraintExpression(final String expression){
		final String updatedExpression = cleanExpression(expression);
		final JexlExpression jexlExpression = JEXL_ENGINE.createExpression(updatedExpression);
		final JexlContext context = createJexlContext();

		return (params) -> {
			setContextParameters(context, params);
			return ((Number)jexlExpression.evaluate(context))
				.doubleValue();
		};
	}

	static ModelFunction parseExpression(final String expression, final String[] dataInput){
		final String updatedExpression = cleanExpression(expression);
		final JexlExpression jexlExpression = JEXL_ENGINE.createExpression(updatedExpression);
		final JexlContext context = createJexlContext();

		return (params, inputs) -> {
			setContextParameters(context, params);
			setContextInputs(context, dataInput, inputs);
			return ((Number)jexlExpression.evaluate(context))
				.doubleValue();
		};
	}

	static List<String> extractVariables(final String expression){
		final JexlScript script = JEXL_ENGINE.createScript(expression);
		final Set<List<String>> variables = script.getVariables();
		final List<String> vars = new ArrayList<>(variables.size());
		for(final List<String> list : variables){
			final StringJoiner sj = new StringJoiner(DOT);
			for(int j = 0, components = list.size(); j < components; j ++)
				sj.add(list.get(j));
			vars.add(sj.toString());
		}
		return vars;
	}

	private static String cleanExpression(String expression){
		expression = PATTERN_MULTIPLICATION.matcher(expression)
			.replaceAll(MULTIPLICATION);
		expression = replaceFunction(expression, PATTERN_ASINH, ASINH);
		expression = replaceFunction(expression, PATTERN_ACOSH, ACOSH);
		expression = replaceFunction(expression, PATTERN_ATANH, ATANH);
		expression = PATTERN_SIGN.matcher(expression)
			.replaceAll(SIGNUM);
		expression = convertPowerToMathPow(expression);
		expression = substituteJavaFunction(expression);
		expression = substituteApacheFunction(expression);
		return expression;
	}

	private static String substituteJavaFunction(final CharSequence expression){
		final Matcher matcher = PATTERN_FUNCTION_SUBSTITUTION_JAVA.matcher(expression);
		return matcher.replaceAll(match -> CONTEXT_JAVA_MATH + DOT + (match.group(1) != null
			? match.group(1).toUpperCase(Locale.ROOT)
			: match.group(2) + PARENTHESIS_OPEN));
	}

	private static String substituteApacheFunction(String expression){
		Matcher matcher = PATTERN_FUNCTION_SUBSTITUTION_APACHE_GAMMA.matcher(expression);
		expression = matcher.replaceAll(match -> CONTEXT_APACHE_GAMMA + DOT + (match.group(1) != null
			? match.group(1).toUpperCase(Locale.ROOT)
			: match.group(2) + PARENTHESIS_OPEN));

		matcher = PATTERN_FUNCTION_SUBSTITUTION_APACHE_ERF.matcher(expression);
		expression = matcher.replaceAll(match -> CONTEXT_APACHE_ERF + DOT + (match.group(1) != null
			? match.group(1).toUpperCase(Locale.ROOT)
			: match.group(2) + PARENTHESIS_OPEN));

		return expression;
	}

	private static String replaceFunction(final String expression, final Pattern functionPattern, final CharSequence substitute){
		final StringBuilder result = new StringBuilder(expression);
		Matcher matcher = functionPattern.matcher(expression);
		while(matcher.find()){
			final int start = matcher.start();
			final int end = matcher.end();
			//find argument
			int argumentEnd;
			int parenthesisOpen = 0;
			final int length = result.length();
			for(argumentEnd = end; argumentEnd < length; argumentEnd ++){
				final char chr = result.charAt(argumentEnd);

				if(chr == '(')
					parenthesisOpen ++;
				else if(chr == ')'){
					if(parenthesisOpen == 0)
						break;

					parenthesisOpen --;
				}
			}
			String argument = PARENTHESIS_OPEN + result.substring(end, argumentEnd) + PARENTHESIS_CLOSE;
			argument = PATTERN_X.matcher(substitute)
				.replaceAll(argument);
			result.replace(start, argumentEnd, argument);
			matcher = functionPattern.matcher(result);
		}
		return result.toString();
	}

	static String convertPowerToMathPow(String expression){
		int powerIndex = -1;
		while((powerIndex = expression.indexOf(POWER, powerIndex + 1)) >= 0){
//			//leave the powers of powers for last
//			//FIXME needed?
//			if(expression.charAt(powerIndex - 1) == ')'){
//				final int blockStartIndex = parenthesizedBlockStartIndex(expression, powerIndex - 1);
//				if(blockStartIndex >= 3 && expression.substring(Math.max(blockStartIndex - 4, 0), blockStartIndex + 1).equals(POW_FUNCTION))
//					continue;
//			}


			//extract base:
			int blockStartIndex = 0;
			if(expression.charAt(powerIndex - 1) == ')')
				blockStartIndex = parenthesizedBlockStartIndex(expression, powerIndex - 1);
			else
				for(int i = powerIndex - 1; i >= 0; i --)
					if(BASE_START_DELIMITERS.indexOf(expression.charAt(i)) >= 0){
						blockStartIndex = i + 1;
						break;
					}
			String base = expression.substring(blockStartIndex, powerIndex);
			if(base.charAt(0) == '(' && parenthesizedBlockEndIndex(base, 0) == base.length() - 1)
				base = base.substring(1, base.length() - 1);


			//extract exponent:
			final int initialPowerIndex = powerIndex;
			int blockEndIndex = expression.length();
			boolean repeat = true;
			while(repeat){
				if(expression.charAt(powerIndex + 1) == '(')
					blockEndIndex = parenthesizedBlockEndIndex(expression, powerIndex + 1) + 1;
				else{
					boolean found = false;
					for(int i = powerIndex + 1, length = expression.length(); i < length; i ++){
						final char chr = expression.charAt(i);
						if(BASE_END_DELIMITERS.indexOf(chr) >= 0){
							blockEndIndex = i;
							found = true;
							break;
						}
					}
					if(!found)
						blockEndIndex = expression.length();
				}

				repeat = false;
				for(int i = blockEndIndex, length = expression.length(); !repeat && i < length; i ++){
					final char chr = expression.charAt(i);
					if(chr == ' ')
						continue;
					if(chr == '('){
						i = parenthesizedBlockEndIndex(expression, i);
						continue;
					}
					if(chr == ')')
						break;

					if(chr == '^'){
						powerIndex = i;
						repeat = true;
					}
				}
			}
			String exponent = expression.substring(initialPowerIndex + 1, blockEndIndex);
			if(exponent.charAt(0) == '(' && parenthesizedBlockEndIndex(exponent, 0) == exponent.length() - 1){
				int index = blockEndIndex;
				if(expression.charAt(index - 1) == ')')
					index ++;
				if(index >= expression.length() || expression.charAt(index - 1) != '^')
					exponent = exponent.substring(1, exponent.length() - 1);
				//otherwise wait for next cycle
			}
			if(exponent.contains(POWER)){
				exponent = convertPowerToProduct(exponent);

				//reset pointer
				powerIndex = blockStartIndex;
			}


			//substitute with function:
			final StringBuilder sb = new StringBuilder(expression);
			sb.replace(blockStartIndex, blockEndIndex, POW_FUNCTION + base.trim() + COMMA + exponent.trim() + PARENTHESIS_CLOSE);
			expression = sb.toString();
		}

//		while((powerIndex = expression.indexOf(POWER, powerIndex + 1)) >= 0)
//			expression = managePowerOfPower(expression);

		return expression;
	}

	private static String convertPowerToProduct(final String expression){
		final StringJoiner multipliers = new StringJoiner(MULTIPLICATION);
		int powerIndex = 0;
		final int length = expression.length();
		while(powerIndex < length){
			int blockEndIndex = (expression.charAt(powerIndex) == '('
				? parenthesizedBlockEndIndex(expression, powerIndex)
				: -1);
			if(blockEndIndex >= 0){
				final String multiplier = expression.substring(powerIndex, blockEndIndex + 1);
				multipliers.add(multiplier);
			}
			else{
				boolean found = false;
				for(int i = powerIndex; !found && i < length; i ++){
					final char chr = expression.charAt(i);
					if(chr == '^'){
						multipliers.add(expression.substring(powerIndex, i));
						blockEndIndex = i - 1;
						found = true;
					}
					else if(BASE_DELIMITERS.indexOf(chr) >= 0)
						break;
				}
				if(!found){
					multipliers.add(expression.substring(powerIndex));
					break;
				}
			}

			powerIndex = blockEndIndex + POWER.length() + 1;
		}
		return (multipliers.length() >= 0? multipliers.toString(): expression);
	}

	private static int parenthesizedBlockStartIndex(final CharSequence expression, final int parenthesisOpenIndex){
		int closeParenthesisCount = 0;
		for(int i = parenthesisOpenIndex; i >= 0; i --){
			final char chr = expression.charAt(i);

			if(chr == ')')
				closeParenthesisCount ++;
			else if(chr == '('){
				closeParenthesisCount --;

				if(closeParenthesisCount == 0)
					return i;
			}
		}
		return -1;
	}

	private static int parenthesizedBlockEndIndex(final CharSequence expression, final int parenthesisOpenIndex){
		int openParenthesisCount = 0;
		for(int i = parenthesisOpenIndex, length = expression.length(); i < length; i ++){
			final char chr = expression.charAt(i);

			if(chr == '(')
				openParenthesisCount ++;
			else if(chr == ')'){
				openParenthesisCount --;

				if(openParenthesisCount == 0)
					return i;
			}
		}
		return -1;
	}

	private static JexlContext createJexlContext(){
		final JexlContext context = new MapContext();
		context.set(CONTEXT_JAVA_MATH, StrictMath.class);
		context.set(CONTEXT_APACHE_GAMMA, Gamma.class);
		return context;
	}

	private static void setContextParameters(final JexlContext context, final double[] params){
		for(int i = 0, length = params.length; i < length; i ++)
			context.set(PARAMETER_PREFIX + i, params[i]);
	}

	private static void setContextInputs(final JexlContext context, final String[] dataInput, final double[] inputs){
		for(int i = 0, length = dataInput.length; i < length; i ++)
			context.set(dataInput[i], inputs[i]);
	}

}
