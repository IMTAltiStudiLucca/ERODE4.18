package it.imt.erode.simulation.deterministic.jsundials;

import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.simulation.deterministic.CRNVectorField;
import jida.DAEHandler;

public class CRNVectorFieldJSundialsIDA extends DAEHandler {

	private CRNVectorField vectorField;

	public CRNVectorFieldJSundialsIDA(ICRN crn, Terminator terminator) {
		super(crn.getSpecies().size());
		this.vectorField=new CRNVectorField(crn,terminator);
	}

	public int computeICP(double[] ic , double[] icp) {
		vectorField.computeNextydotOfMeans(0, ic, icp);
		return 0;
	}
	
	@Override
	public int computeNextStep(double t, double[] y , double[] yp   , double[] ypp) {
		vectorField.computeNextydotOfMeans(t, y, ypp);
		for(ISpecies species : vectorField.getCRN().getSpecies()) {
			if (!species.isAlgebraic())
			{
				int i=species.getID();
				ypp[i] = ypp[i] - yp[i] ;
			}
			else {
				/*
				 * We want to transform
				 * 	Y1 = x1 + x2
				 * in
				 * 	0 =  x1 + x2 - Y1
				 * where Y1 is an algebraic variable
				 */
				int i=species.getID();
				ypp[i] = ypp[i] - y[i];
			}
		}
		return 0;
	}
	
	/*
	@Override
	public int computeDerivative(double t, double[] y, double[] ydot) {
		vectorField.computeNextydotOfMeans(t, y, ydot);
		return 0;
	}*/
	
}
