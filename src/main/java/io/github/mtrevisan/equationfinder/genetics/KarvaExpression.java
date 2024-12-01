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


//https://en.wikipedia.org/wiki/Gene_expression_programming
//https://www.gene-expression-programming.com/GepBook/Chapter3/Introduction.htm
//https://worldcomp-proceedings.com/proc/p2013/GEM2456.pdf
public class KarvaExpression{

	//list of functions, variables, and constants
	String[] head;
	//list of variables and constants
	//size is: `t = h · (MaxArg - 1) + 1`, where `t` is the number of symbols in the tail, `h` is the number of symbols in the head, and
	//`MaxArg` is the maximum number of arguments required by any function that is allowed to be used in the expression
	String[] tail;


	public KarvaExpression(final String[] head, final String[] tail){
		this.head = head;
		this.tail = tail;
	}


	public String headAt(final int index){
		return head[index];
	}

	public String tailAt(final int index){
		return tail[index];
	}

	public int headLength(){
		return head.length;
	}

	public int tailLength(){
		return tail.length;
	}

	public int length(){
		return headLength() + tailLength();
	}


	@Override
	public String toString(){
		return Arrays.toString(head) + ',' + Arrays.toString(tail);
	}

}
