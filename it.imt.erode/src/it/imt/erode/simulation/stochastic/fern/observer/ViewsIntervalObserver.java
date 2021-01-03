package it.imt.erode.simulation.stochastic.fern.observer;

//import java.util.Map.Entry;

//import fern.network.AmountManager;
import fern.simulation.Simulator;
import it.imt.erode.expression.evaluator.MathEval;

/**
 * 
 * Observes amounts of views (e.g., species or arithmetic expressions of them) repeatedly after certain intervals.
 * <p>
 * This observer does take repeats into account. If you repeat the simulation, 
 * you will get an average over the results of each run.
 * 
 * @author Andrea Vandin
 *
 */
public class ViewsIntervalObserver extends IntervalObserverAVGBugFixed {

	
	//private String[] viewNames;
	private String[] viewExpressions;
	private boolean[] viewExpressionsUsesCovariances;
	private MathEval mathEval;
	//	private double timeOfParametrizationOfMathEval;
	//	private int numSpecies;
	//private HashMap<String, Integer> speciesNameSupportedByMathEvalToPos;

	/**
	 * Creates the observer for a given simulator, a given interval and given species names.
	 * @param sim			simulator
	 * @param interval		interval
	 * @param viewExpressionsUsesCovariances 
	 * @param speciesName	species names
	 */
	public ViewsIntervalObserver(Simulator sim, double interval, String[] viewNames, String[] viewExpressions, boolean[] viewExpressionsUsesCovariances, MathEval mathEval) {
		super(sim,interval,viewNames);
		this.mathEval=new MathEval(mathEval);
		this.viewExpressions=viewExpressions;
		this.viewExpressionsUsesCovariances=viewExpressionsUsesCovariances;
	}
	
	@Override
	/**
	 * Gets the styles for the columns. If you don't want styles, just return null!
	 * 
	 * @return	styles for the columns
	 */
	public String[] getStyles() {
		return null;
	}

	@Override
	protected double getEntityValue(int view) {
		//We currently compute the covariances only for the odes.
		if(viewExpressionsUsesCovariances[view]){
			return -1;
		}
		//update species amounts in mathEval
		for (String speciesNameSupportedByMathEval : mathEval.getVariablesWithin(viewExpressions[view])) {
			int pos = getSimulator().getNet().getSpeciesByName(speciesNameSupportedByMathEval);
			//If pos is -1, then the string is not a species but a parameter, and I do not have to update it.
			if(pos!=-1){
				double val = getSimulator().getAmount(pos);
				mathEval.setVariable(speciesNameSupportedByMathEval, val);
			}
		}
		//AmountManager amountManager = this.getSimulator().getNet().getAmountManager();
		//Iterable<Entry<String, Double>> variables = mathEval.getVariables();
		double val = mathEval.evaluate(viewExpressions[view]);
		return val;
	}

}
