package io.github.mtrevisan.equationfinder.objectives;

import io.github.mtrevisan.equationfinder.ModelFunction;


/** Maximum Relative error */
public class ObjectiveMaxR extends ObjectiveMax{

	public ObjectiveMaxR(final ModelFunction function, final double[][] dataTable){
		super(function, dataTable);
	}


	@Override
	public double value(final double[] params){
		return super.value(params) / dataTable.length;
	}

}
