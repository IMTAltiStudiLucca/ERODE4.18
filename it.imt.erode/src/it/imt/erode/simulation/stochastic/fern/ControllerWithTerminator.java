package it.imt.erode.simulation.stochastic.fern;

import fern.simulation.Simulator;
import fern.simulation.controller.DefaultController;
import it.imt.erode.commandline.Terminator;

public class ControllerWithTerminator extends DefaultController {

	private Terminator terminator;
	
	public ControllerWithTerminator(double maxTime,Terminator terminator) {
		super(maxTime);
		this.terminator=terminator;
	}
	
	@Override
	public boolean goOn(Simulator sim) {
		boolean goon = super.goOn(sim);
		return goon && ! terminator.hasToTerminate();
	}

}
