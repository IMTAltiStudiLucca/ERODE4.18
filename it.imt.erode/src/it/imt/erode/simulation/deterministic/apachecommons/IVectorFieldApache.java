package it.imt.erode.simulation.deterministic.apachecommons;

import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;

/**
 * Vector field associated with an ERODE model
 * @author Mirco Tribastone, Andrea Vandin
 *
 */
public interface IVectorFieldApache extends FirstOrderDifferentialEquations{

	/**
	 * First element is time, other elements are species concentrations,
	 * ordered according to the array given by getSpecies.
	 * @param start  start time
	 * @param stop   stop time
	 * @param step   fixed time step
	 * @param initialCondition initial condition, ordered according to getSpecies
	 * @return
	 */
	public double[][] solve(double start, double stop, double step,double[] initialCondition);
	
	/**
	 * First element is time, other elements are species concentrations,
	 * ordered according to the array given by getSpecies.
	 * @param start  start time
	 * @param stop   stop time
	 * @param step   fixed time step
	 * @param initialCondition initial condition, ordered according to getSpecies
	 * @param maxStep
	 * @param scalRelativeTolerance
	 * @param scalAbsoluteTolerance
	 * @param minStep
	 * @return
	 */
	public double[][] solve(double start, double stop, double step,double[] initialCondition, double maxStep, double scalRelativeTolerance, double scalAbsoluteTolerance, double minStep);
	
}
