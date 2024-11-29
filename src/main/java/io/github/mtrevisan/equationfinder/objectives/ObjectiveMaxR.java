package io.github.mtrevisan.equationfinder.objectives;

import io.github.mtrevisan.equationfinder.ModelFunction;


/** Maximum Relative error */
public class ObjectiveMaxR extends ObjectiveMax{

	public static final String OBJECTIVE_MAXIMUM_RELATIVE_ERROR = "MaxR";


	public ObjectiveMaxR(final ModelFunction function, final double[][] dataTable){
		super(function, dataTable);
	}


	@Override
	public double value(final double[] params){
		return super.value(params) / dataTable.length;
	}

}
