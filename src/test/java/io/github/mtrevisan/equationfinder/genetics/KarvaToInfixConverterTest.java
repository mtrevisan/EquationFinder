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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class KarvaToInfixConverterTest{

	@Test
	void convertToEquation01(){
		KarvaExpression karva = KarvaExpression.create(new String[]{"+", "a", "b"});
		String equation = KarvaToInfixConverter.convertToEquation(karva);

		Assertions.assertEquals("(a+b)", equation);
	}

	@Test
	void convertToEquation02(){
		KarvaExpression karva = KarvaExpression.create(new String[]{"sin", "x"});
		String equation = KarvaToInfixConverter.convertToEquation(karva);

		Assertions.assertEquals("sin(x)", equation);
	}

	@Test
	void convertToEquation03(){
		KarvaExpression karva = KarvaExpression.create(new String[]{"hypot", "a", "b"});
		String equation = KarvaToInfixConverter.convertToEquation(karva);

		Assertions.assertEquals("hypot(a,b)", equation);
	}

	@Test
	void convertToEquation04(){
		KarvaExpression karva = KarvaExpression.create(new String[]{"+", "a", "*", "b", "c"});
		String equation = KarvaToInfixConverter.convertToEquation(karva);

		Assertions.assertEquals("(a+(b*c))", equation);
	}

	@Test
	void convertToEquation05(){
		KarvaExpression karva = KarvaExpression.create(new String[]{"sin", "*", "-", "+", "a", "b", "c", "d"});
		String equation = KarvaToInfixConverter.convertToEquation(karva);

		Assertions.assertEquals("sin(((a-b)*(c+d)))", equation);
	}

	@Test
	void convertToEquation06(){
		KarvaExpression karva = KarvaExpression.create(new String[]{"+", "/", "*", "a", "b", "c", "d"});
		String equation = KarvaToInfixConverter.convertToEquation(karva);

		Assertions.assertEquals("((a/b)+(c*d))", equation);
	}

	@Test
	void convertToEquation08(){
		KarvaExpression karva = KarvaExpression.create(new String[]{"sin", "*", "b", "*", "*", "+", "b", "a", "cos", "b", "a"});
		String equation = KarvaToInfixConverter.convertToEquation(karva);

		Assertions.assertEquals("sin((b*((b*a)*(cos(a)+b))))", equation);
	}

}
