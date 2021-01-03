package it.imt.erode.commandline;

import java.util.Comparator;

import it.imt.erode.crn.differentialHull.EvaluatedParameter;

public class EvaluatedParametersComparator implements Comparator<EvaluatedParameter> {

	@Override
	public int compare(EvaluatedParameter o1, EvaluatedParameter o2) {
		return Double.compare(o1.getValue(), o2.getValue());
	}

}
