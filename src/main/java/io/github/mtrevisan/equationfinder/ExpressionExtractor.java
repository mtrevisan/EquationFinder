package io.github.mtrevisan.equationfinder;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.math3.special.Gamma;

import java.util.HashSet;
import java.util.LinkedHashSet;
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
	private static final String MULTIPLICATION_CHAR = "*";
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
	static ModelFunction parseExpression(final String expression, final String[] dataInput){
		final String updatedExpression = cleanExpression(expression);
		final JexlExpression jexlExpression = JEXL_ENGINE.createExpression(updatedExpression);
		final JexlContext context = createJexlContext();

		return (inputs, params) -> {
			setContextParameters(context, params);
			setContextInputs(context, dataInput, inputs);
			return ((Number)jexlExpression.evaluate(context))
				.doubleValue();
		};
	}

	static Set<String> extractVariables(final String expression){
		final JexlScript script = JEXL_ENGINE.createScript(expression);
		final Set<List<String>> variables = script.getVariables();
		final Set<String> vars = new LinkedHashSet<>(variables.size());
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
			.replaceAll(MULTIPLICATION_CHAR);
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

	private static String convertPowerToMathPow(final String expression){
		final StringBuilder result = new StringBuilder();
		for(int i = 0, length = expression.length(); i < length; i ++){
			final char current = expression.charAt(i);

			if(current == '^'){
				final int baseStart = findBaseStart(expression, i - 1);
				final String base = expression.substring(baseStart, i)
					.trim();

				final int exponentEnd = findExponentEnd(expression, i + 1);
				final String exponent = expression.substring(i + 1, exponentEnd)
					.trim();

				result.replace(baseStart, i, FUNCTION_POWER + PARENTHESIS_OPEN + base + COMMA)
					.append(exponent)
					.append(PARENTHESIS_CLOSE);

				i = exponentEnd - 1;
			}
			else
				result.append(current);
		}
		return result.toString();
	}

	private static int findBaseStart(final CharSequence expression, final int index){
		int openParentheses = 0;
		for(int i = index; i >= 0; i --){
			final char chr = expression.charAt(i);
			if(chr == ')')
				openParentheses ++;
			else if(openParentheses > 0 && chr == '(')
				openParentheses --;
			else if(openParentheses == 0 && BASE_START_DELIMITERS.indexOf(chr) >= 0)
				return i + 1;
		}
		return 0;
	}

	private static int findExponentEnd(final CharSequence expression, final int index){
		int openParentheses = 0;
		for(int i = index; i < expression.length(); i ++){
			final char chr = expression.charAt(i);
			if(chr == '(')
				openParentheses ++;
			else if(openParentheses > 0 && chr == ')')
				openParentheses --;
			else if(openParentheses == 0 && BASE_END_DELIMITERS.indexOf(chr) >= 0)
				return i;
		}
		return expression.length();
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
