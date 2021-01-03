package it.imt.erode.simulation.deterministic.jsundials;

import cvode.ODEHandler;
import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.simulation.deterministic.CRNVectorField;

public class CRNVectorFieldJSundialsCVODE extends ODEHandler {

	private CRNVectorField vectorField;

	public CRNVectorFieldJSundialsCVODE(ICRN crn, Terminator terminator) {
		super(crn.getSpecies().size());
		this.vectorField=new CRNVectorField(crn,terminator);
	}
	
	@Override
	public int computeDerivative(double t, double[] y, double[] ydot) {
		vectorField.computeNextydotOfMeans(t, y, ydot);
		return 0;
	}
	
}
