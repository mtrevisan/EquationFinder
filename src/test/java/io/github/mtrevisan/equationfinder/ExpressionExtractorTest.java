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