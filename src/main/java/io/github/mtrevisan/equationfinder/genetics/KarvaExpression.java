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
import java.util.Map;
import java.util.Random;


//https://en.wikipedia.org/wiki/Gene_expression_programming
//https://www.gene-expression-programming.com/GepBook/Chapter3/Introduction.htm
//https://worldcomp-proceedings.com/proc/p2013/GEM2456.pdf
//https://www.cs.uic.edu/~xli1/papers/PGEP_GECCOLateBreaking05_XLi.pdf
public class KarvaExpression{

	private static final Random RANDOM = new Random(System.currentTimeMillis());


	//list of functions, variables, and constants, in level-order
	private final String[] gene;
	private final int headLength;
	private final int parameterCount;


	public static KarvaExpression create(final String[] gene, final int headLength, final int parameterCount){
		return new KarvaExpression(gene, headLength, parameterCount);
	}

	/**
	 * Generates a random Karva expression.
	 *
	 * @param maxNumberOfOperators	The maximum number of operators.
	 * @return	A Karva expression.
	 */
	public static KarvaExpression createRandom(final int maxNumberOfOperators, final Map<String, Integer> operatorArity,
			final String[] inputs){
		final int operatorCount = operatorArity.size();
		final String[] operators = operatorArity.keySet()
			.toArray(new String[operatorCount]);
		final int minArgs = operatorArity.values()
			.stream()
			.min(Integer::compareTo)
			.get();
		final int maxArgs = operatorArity.values()
			.stream()
			.max(Integer::compareTo)
			.get();

		//ensure at least 2 nodes
		final int headLength = RANDOM.nextInt(maxNumberOfOperators - 2) + 2;
		final int tailLength = headLength * (maxArgs - 1) + 1;
		final int maxWidth = (maxArgs - 1) * headLength + 1;
		final int maxDepth = (headLength + 1) / (minArgs * (minArgs + 1));

		final String[] gene = new String[headLength + tailLength];

		//generate head (operators and functions)
		int index = 0;
		final int inputCount = inputs.length;
		int parameterCount = 1;
		for(int i = 0; i < headLength; i ++){
			final int type = RANDOM.nextInt(3);
			//add function to head
			if(type == 0)
				gene[index ++] = operators[RANDOM.nextInt(operatorCount)];
			//add variable to head
			else if(type == 1)
				gene[index ++] = inputs[RANDOM.nextInt(inputCount)];
			//add constant to head
			else if(type == 2)
				gene[index ++] = "p" + RANDOM.nextInt(parameterCount ++);
		}

		//generate tail (variables and constants)
		for(int i = 0; i < tailLength; i ++)
			gene[index ++] = (RANDOM.nextBoolean()
				? inputs[RANDOM.nextInt(inputCount)]
				: "p" + RANDOM.nextInt(parameterCount ++));

		return create(gene, headLength, parameterCount);
	}


	private KarvaExpression(final String[] gene, final int headLength, final int parameterCount){
		this.gene = gene;
		this.headLength = headLength;
		this.parameterCount = parameterCount;
	}


	public String geneAt(final int index){
		return gene[index];
	}

	//Simple mutation
	public KarvaExpression generateMutation(final int startIndex, final int length, final Map<String, Integer> operatorArity,
			final String[] inputs){
		final int operatorCount = operatorArity.size();
		final String[] operators = operatorArity.keySet()
			.toArray(new String[operatorCount]);

		final String[] newGene = Arrays.copyOf(gene, gene.length);
		if(startIndex >= 0 && length > 0
				&& startIndex + length <= gene.length){
			final int inputCount = inputs.length;
			for(int i = 0; i < length; i ++){
				if(i + startIndex < headLength){
					//mutate head
					final int type = RANDOM.nextInt(3);
					//add function to head
					if(type == 0)
						newGene[i + startIndex] = operators[RANDOM.nextInt(operatorCount)];
					//add variable to head
					else if(type == 1)
						newGene[i + startIndex] = inputs[RANDOM.nextInt(inputCount)];
					//add constant to head
					else if(type == 2)
						newGene[i + startIndex] = "p" + RANDOM.nextInt(parameterCount);
				}
				else
					//mutate tail
					newGene[i + startIndex] = (RANDOM.nextBoolean()
						? inputs[RANDOM.nextInt(inputCount)]
						: "p" + RANDOM.nextInt(parameterCount));
			}
		}
		return new KarvaExpression(newGene, headLength, parameterCount);
	}

	//Reverse a segment of the gene
	public KarvaExpression generateInversion(final int startIndex, final int length){
		final String[] newGene = Arrays.copyOf(gene, gene.length);
		if(startIndex >= 0 && length > 1
				&& startIndex + length <= gene.length){
			int left = startIndex;
			int right = startIndex + length - 1;
			while(left < right){
				final String temp = newGene[left];
				newGene[left] = newGene[right];
				newGene[right] = temp;

				left ++;
				right --;
			}
		}
		return new KarvaExpression(newGene, headLength, parameterCount);
	}

	//Copy a segment of the gene to a different position
	public KarvaExpression generateTransposition(final int originIndex, final int targetIndex, final int length){
		final String[] newGene = Arrays.copyOf(gene, gene.length);
		if(originIndex >= 0 && targetIndex >= 0 && length > 0
				&& originIndex + length <= gene.length && targetIndex + length <= gene.length){
			final String[] segment = new String[length];
			System.arraycopy(newGene, originIndex, segment, 0, length);

			//insert the group at the target position
			System.arraycopy(segment, 0, newGene, targetIndex, length);
		}
		return new KarvaExpression(newGene, headLength, parameterCount);
	}

	//Exchange genetic material between two genes
	public KarvaExpression[] generateRecombinationOnePoint(final KarvaExpression otherExpression, final int crossoverPoint){
		final String[] newGene1 = Arrays.copyOf(gene, otherExpression.gene.length);
		final String[] newGene2 = Arrays.copyOf(otherExpression.gene, gene.length);
		if(crossoverPoint >= 0
				&& crossoverPoint <= gene.length && crossoverPoint <= otherExpression.length()){
			//create temporary arrays to hold the segments to be swapped
			final String[] segment1 = new String[gene.length - crossoverPoint];
			final String[] segment2 = new String[otherExpression.gene.length - crossoverPoint];
			//copy the segments to be swapped
			System.arraycopy(gene, crossoverPoint, segment1, 0, segment1.length);
			System.arraycopy(otherExpression.gene, crossoverPoint, segment2, 0, segment2.length);

			//swap the segments
			System.arraycopy(segment2, 0, newGene1, crossoverPoint, segment2.length);
			System.arraycopy(segment1, 0, newGene2, crossoverPoint, segment1.length);
		}
		return new KarvaExpression[]{
			new KarvaExpression(newGene1, headLength, parameterCount),
			new KarvaExpression(newGene2, otherExpression.headLength, otherExpression.parameterCount)
		};
	}

	//Exchange genetic material between two genes
	public KarvaExpression[] generateRecombinationTwoPoint(final KarvaExpression otherExpression, final int crossoverPoint1,
			final int crossoverPoint2){
		final String[] newGene1 = Arrays.copyOf(gene, gene.length);
		final String[] newGene2 = Arrays.copyOf(otherExpression.gene, otherExpression.gene.length);
		if(crossoverPoint1 >= 0 && crossoverPoint2 >= 0 && crossoverPoint1 + 1 < crossoverPoint2
				&& crossoverPoint1 <= gene.length && crossoverPoint1 <= otherExpression.length()
				&& crossoverPoint2 <= gene.length && crossoverPoint2 <= otherExpression.length()){
			//length of the segment to swap
			final int segmentLength = crossoverPoint2 - crossoverPoint1;

			//temporary buffers to hold the segments to be swapped
			final String[] segment1 = new String[segmentLength];
			final String[] segment2 = new String[segmentLength];

			//copy segments from crossover points
			System.arraycopy(gene, crossoverPoint1, segment1, 0, segmentLength);
			System.arraycopy(otherExpression.gene, crossoverPoint1, segment2, 0, segmentLength);

			//swap segments between offspring
			System.arraycopy(segment2, 0, newGene1, crossoverPoint1, segmentLength);
			System.arraycopy(segment1, 0, newGene2, crossoverPoint1, segmentLength);
		}
		return new KarvaExpression[]{
			new KarvaExpression(newGene1, headLength, parameterCount),
			new KarvaExpression(newGene2, otherExpression.headLength, otherExpression.parameterCount)
		};
	}

	public boolean isEmpty(){
		return (gene == null || gene.length == 0);
	}

	public int headLength(){
		return headLength;
	}

	public int length(){
		return (gene != null? gene.length: 0);
	}


	@Override
	public String toString(){
		return Arrays.toString(gene);
	}

}
