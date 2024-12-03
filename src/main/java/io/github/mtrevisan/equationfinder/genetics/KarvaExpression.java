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
import java.util.Random;


//https://en.wikipedia.org/wiki/Gene_expression_programming
//https://www.gene-expression-programming.com/GepBook/Chapter3/Introduction.htm
//https://worldcomp-proceedings.com/proc/p2013/GEM2456.pdf
//https://www.cs.uic.edu/~xli1/papers/PGEP_GECCOLateBreaking05_XLi.pdf
public class KarvaExpression{

	private static final Random RANDOM = new Random(System.currentTimeMillis());


	//list of functions, variables, and constants, in level-order
	private String[] gene;


	public KarvaExpression(final String[] gene){
		this.gene = gene;
	}


	public String geneAt(final int index){
		return gene[index];
	}

	//Simple mutation
	public KarvaExpression generateMutation(final int originIndex, final int length, final String[] operators){
		final String[] newGene = Arrays.copyOf(gene, gene.length);
		if(originIndex >= 0 && length > 0
				&& originIndex + length <= gene.length){
			for(int i = 0; i < length; i ++)
				newGene[i + originIndex] = operators[RANDOM.nextInt(operators.length)];
		}
		return new KarvaExpression(newGene);
	}

	//Reverse a section of the gene
	public KarvaExpression generateInversion(final int originIndex, final int targetIndex, final int length){
		final String[] newGene = Arrays.copyOf(gene, gene.length);
		if(originIndex >= 0 && targetIndex >= 0 && length > 0
				&& originIndex + length <= gene.length && targetIndex + length <= gene.length && originIndex < targetIndex){
			final String[] segment = new String[length];
			System.arraycopy(newGene, originIndex, segment, 0, length);
			System.arraycopy(newGene, targetIndex, newGene, originIndex, length);
			System.arraycopy(segment, 0, newGene, targetIndex, length);
		}
		return new KarvaExpression(newGene);
	}

	//Move a group of symbols to a different position
	public KarvaExpression generateTransposition(final int originIndex, final int targetIndex, final int length){
		final String[] newGene = Arrays.copyOf(gene, gene.length);
		if(originIndex >= 0 && targetIndex >= 0 && length > 0
				&& originIndex + length <= gene.length && targetIndex + length <= gene.length){
			final String[] segment = new String[length];
			System.arraycopy(newGene, originIndex, segment, 0, length);

			//shift elements to make space
			if(targetIndex < originIndex)
				System.arraycopy(newGene, targetIndex, newGene, targetIndex + length, originIndex - targetIndex);
			else
				System.arraycopy(newGene, originIndex + length, newGene, originIndex, targetIndex - originIndex);

			//insert the group at the target position
			System.arraycopy(segment, 0, newGene, targetIndex, length);
		}
		return new KarvaExpression(newGene);
	}

	//Exchange genetic material between two genes
	public KarvaExpression[] generateRecombinationOnePoint(final KarvaExpression otherExpression, final int crossoverPoint){
		final String[] newGene1 = Arrays.copyOf(gene, gene.length);
		final String[] newGene2 = Arrays.copyOf(otherExpression.gene, otherExpression.gene.length);
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
			new KarvaExpression(newGene1),
			new KarvaExpression(newGene2)
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
			new KarvaExpression(newGene1),
			new KarvaExpression(newGene2)
		};
	}

	public boolean isEmpty(){
		return (gene == null || gene.length == 0);
	}

	public int length(){
		return (gene != null? gene.length: 0);
	}


	@Override
	public String toString(){
		return Arrays.toString(gene);
	}

}
