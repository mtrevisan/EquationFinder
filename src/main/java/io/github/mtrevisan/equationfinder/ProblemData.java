package io.github.mtrevisan.equationfinder;


record ProblemData(
	SearchMode searchMode,
	String expression,
	String[] constraints,
	String[] dataInput,
	double[][] dataTable,
	String searchMetric){
}
