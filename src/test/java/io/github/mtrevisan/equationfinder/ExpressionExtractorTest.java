package io.github.mtrevisan.equationfinder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class ExpressionExtractorTest{

	@Test
	void powerOfPower(){
		String equation = "x^y";
		String result = ExpressionExtractor.convertPowerToMathPow(equation);
		Assertions.assertEquals("pow(x,y)", result);

		equation = "(x + 1)^2";
		result = ExpressionExtractor.convertPowerToMathPow(equation);
		Assertions.assertEquals("pow(x + 1,2)", result);

		equation = "(a + b^2)^(c + d)";
		result = ExpressionExtractor.convertPowerToMathPow(equation);
		Assertions.assertEquals("pow(a + pow(b,2),c + d)", result);

		equation = "(a^2 + b)^(c + d)";
		result = ExpressionExtractor.convertPowerToMathPow(equation);
		Assertions.assertEquals("pow(pow(a,2) + b,c + d)", result);

		equation = "(x + y)^(a + b^2)^3";
		result = ExpressionExtractor.convertPowerToMathPow(equation);
		Assertions.assertEquals("pow(x + y,(a + pow(b,2))*3)", result);

		equation = "x^(2+1)^3";
		result = ExpressionExtractor.convertPowerToMathPow(equation);
		Assertions.assertEquals("pow(x,(2+1)*3)", result);

		equation = "x^2^3";
		result = ExpressionExtractor.convertPowerToMathPow(equation);
		Assertions.assertEquals("pow(x,2*3)", result);

		equation = "x + 1^(2 + x)";
		result = ExpressionExtractor.convertPowerToMathPow(equation);
		Assertions.assertEquals("x + pow(1,2 + x)", result);

		equation = "x^(2 + x^2)";
		result = ExpressionExtractor.convertPowerToMathPow(equation);
		Assertions.assertEquals("pow(x,2 + pow(x,2))", result);

		equation = "x1^(2+(x+1)^2)";
		result = ExpressionExtractor.convertPowerToMathPow(equation);
		Assertions.assertEquals("pow(x1,2+pow(x+1,2))", result);
	}

}