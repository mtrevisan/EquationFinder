package io.github.mtrevisan.equationfinder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;


public final class ProblemExtractor{

	private static final String BEST_FIT_BOUND_SEARCH = "best fit search";
	private static final String UPPER_BOUND_SEARCH = "upper bound search";
	private static final String LOWER_BOUND_SEARCH = "lower bound search";
	private static final String SUBJECT_TO = "subject to";
	private static final String WITH_INPUT = "with input";
	private static final String WITH_DATA = "with data";
	private static final String WITH_SEARCH_METRIC = "with search metric";

	private static final int SECTION_NONE = 0;
	private static final int SECTION_OBJECTIVE = 1;
	private static final int SECTION_CONSTRAINTS = 2;
	private static final int SECTION_INPUT = 3;
	private static final int SECTION_DATA = 4;
	private static final int SECTION_SEARCH_METRIC = 5;

	private static final Pattern PATTERN_DATA = Pattern.compile("\\s+");


	private ProblemExtractor(){}


	static ProblemData readProblemData(final Path problemDataFile) throws IOException{
		final List<String> lines = Files.readAllLines(problemDataFile);

		SearchMode searchMode = null;
		String expression = null;
		final List<String> constraints = new ArrayList<>(0);
		String[] dataInput = null;
		final List<double[]> dataTableList = new ArrayList<>(1);
		String objectiveSearchMetric = null;

		int section = SECTION_NONE;
		for(String line : lines){
			line = line.trim();
			if(line.isEmpty() || line.charAt(0) == '#')
				continue;

			if(section == SECTION_NONE){
				if(line.equals(UPPER_BOUND_SEARCH))
					searchMode = SearchMode.UPPER_BOUND;
				else if(line.equals(LOWER_BOUND_SEARCH))
					searchMode = SearchMode.LOWER_BOUND;
				else if(line.equals(BEST_FIT_BOUND_SEARCH))
					searchMode = SearchMode.APPROXIMATE;

				section = SECTION_OBJECTIVE;
				continue;
			}
			else if(line.startsWith(SUBJECT_TO)){
				section = SECTION_CONSTRAINTS;
				continue;
			}
			else if(line.startsWith(WITH_INPUT)){
				section = SECTION_INPUT;
				continue;
			}
			else if(line.startsWith(WITH_DATA)){
				section = SECTION_DATA;
				continue;
			}
			else if(line.startsWith(WITH_SEARCH_METRIC)){
				section = SECTION_SEARCH_METRIC;
				continue;
			}

			if(section == SECTION_OBJECTIVE)
				expression = line;
			else if(section == SECTION_CONSTRAINTS)
				constraints.add(line);
			else if(section == SECTION_INPUT)
				dataInput = PATTERN_DATA.split(line, -1);
			else if(section == SECTION_DATA){
				final String[] parts = PATTERN_DATA.split(line, -1);
				final double[] row = Arrays.stream(parts)
					.mapToDouble(Double::parseDouble)
					.toArray();
				dataTableList.add(row);
			}
			else if(section == SECTION_SEARCH_METRIC)
				objectiveSearchMetric = line;
		}

		final int totalSize = dataTableList.size();
		final double[][] dataTable = new double[totalSize][];
		for(int i = 0; i < totalSize; i ++)
			dataTable[i] = dataTableList.get(i);

		final String[] constraintsArray = constraints.toArray(new String[constraints.size()]);
		return new ProblemData(searchMode, expression, constraintsArray, dataInput, dataTable, objectiveSearchMetric);
	}

}
