package it.imt.erode.simulation.deterministic.apachecommons;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math3.ode.sampling.FixedStepHandler;
import org.apache.commons.math3.ode.sampling.StepNormalizer;
import org.apache.commons.math3.ode.sampling.StepNormalizerBounds;
import org.sbml.jsbml.ASTNode;

import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.implementations.CRNReactionArbitraryAbstract;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;

public class CRNVectorFieldApacheLessEfficientForArbitraryRates implements IVectorFieldApache {

	protected final class MyFixedStepHandler implements FixedStepHandler {
		private final Vector<double[]> sol;

		MyFixedStepHandler(Vector<double[]> sol) {
			this.sol = sol;
		}

		@Override
		public void handleStep(double t, double[] y, double[] ydot,boolean lastStep) {
			double[] line = new double[y.length + 1];
			System.arraycopy(y, 0, line, 1, y.length);
			line[0] = t;
			sol.add(line);
		}

		@Override
		public void init(double arg0, double[] arg1, double arg2) {
		}
	}

	private ICRN crn;
	//private ISpecies[] speciesIdToSpecies;
	//private HashMap<String, ISpecies> speciesNameToSpecies;
	
	private ASTNode[] rateLaws;
	private double[] kineticConstants;
	private List<ASTNode>[] speciesASTNodeInRateExpression;
	private List<ISpecies>[] speciesInRateExpression;

	private Terminator terminator;

	//private int iteration=0;
	
	public Terminator getTerminator(){
		return terminator;
	}
	
	@SuppressWarnings("unchecked")
	public CRNVectorFieldApacheLessEfficientForArbitraryRates(ICRN crn, Terminator terminator) {
		//iteration=0;
		this.crn=crn;
		this.terminator=terminator;
		ISpecies[] speciesIdToSpecies = new ISpecies[crn.getSpecies().size()];
		//crn.getSpecies().toArray(speciesIdToSpecies);
		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
		int i=0;
		for (ISpecies species : crn.getSpecies()) {
			speciesNameToSpecies.put(species.getName(), species);
			speciesIdToSpecies[i]=species;
			i++;
		}
		
		if(!crn.isMassAction()){
			rateLaws = new ASTNode[crn.getReactions().size()];
			speciesASTNodeInRateExpression = (List<ASTNode>[]) Array.newInstance(new ArrayList<ASTNode>().getClass(), crn.getReactions().size());
			speciesInRateExpression = (ArrayList<ISpecies>[]) Array.newInstance(new ArrayList<ISpecies>().getClass(), crn.getReactions().size());
			i=0;
			for(ICRNReaction reaction : crn.getReactions()){
				if(reaction instanceof CRNReactionArbitraryAbstract){
					CRNReactionArbitraryAbstract arb = (CRNReactionArbitraryAbstract)reaction;
					rateLaws[i] = arb.getRateLaw().clone();
					speciesASTNodeInRateExpression[i] = arb.getSpeciesInRateLaw(rateLaws[i],speciesNameToSpecies);
					speciesInRateExpression[i]=new ArrayList<ISpecies>(speciesASTNodeInRateExpression[i].size());
					for (ASTNode speciesNode : speciesASTNodeInRateExpression[i]) {
						ISpecies species = arb.getSpecies(speciesNode,speciesNameToSpecies,speciesIdToSpecies);
						speciesInRateExpression[i].add(species);
					}
				}
				else{
					rateLaws[i]=new ASTNode(reaction.getRate().doubleValue());
					speciesASTNodeInRateExpression[i]=new ArrayList<ASTNode>(0);
					speciesInRateExpression[i]=new ArrayList<ISpecies>(0);
				}
				i++;
			}
		}

		kineticConstants=new double[crn.getReactions().size()];
		int r=0;
		for (ICRNReaction reaction : crn.getReactions()) {
			if(! (reaction instanceof CRNReactionArbitraryAbstract)){
				kineticConstants[r]=reaction.getRate().doubleValue();
			}
			r++;
		}

	}
	
	protected ICRN getCRN(){
		return crn;
	}

	@Override
	public double[][] solve(double start, double stop, double step,double[] initialCondition) {
		return solve(start, stop, step, initialCondition,1.0e-8, 100.0, 1.0e-10, 1.0e-10);
	}

	@Override
	public double[][] solve(double start, double stop, double step,
			double[] initialCondition, double minStep,
			double maxStep, double absTol,
			double relTol) {
		FirstOrderIntegrator dp853 = new DormandPrince853Integrator(minStep, maxStep, absTol, relTol);

		final Vector<double[]> sol = new Vector<double[]>();

		/*FixedStepHandler h = new FixedStepHandler() {

			@Override
			public void handleStep(double t, double[] y, double[] ydot,boolean lastStep) {
				double[] line = new double[y.length + 1];
				System.arraycopy(y, 0, line, 1, y.length);
				line[0] = t;
				sol.add(line);
			}

			@Override
			public void init(double arg0, double[] arg1, double arg2) {
			}

		};*/
		FixedStepHandler h = new MyFixedStepHandler(sol);
		
		//http://grepcode.com/file/repo1.maven.org/maven2/org.apache.commons/commons-math3/3.0/org/apache/commons/math3/ode/ODEIntegrator.java
		dp853.addStepHandler(new StepNormalizer(step, h,StepNormalizerBounds.BOTH));
		dp853.integrate(this, start, initialCondition, stop,initialCondition); // now y contains final state at time t=16.0
		return sol.toArray(new double[sol.size()][]);
	}

	@Override
	public int getDimension() {
		return crn.getSpecies().size();
	}

	@Override
	public void computeDerivatives(double t, double[] y, double[] ydot)
			throws MaxCountExceededException, DimensionMismatchException {

			computeNextydotOfMeans(t, y, ydot);
			
			/*//System.out.println("nv[] at iteration "+iteration+" is "+nv[0]);
			System.out.println("y at iteration "+iteration+" is "+CRNVectorFieldCovariances.printArray(y));
			System.out.println("ydot at iteration "+iteration+" is "+CRNVectorFieldCovariances.printArray(ydot));
			System.out.println("\n");
			iteration++;
			if(iteration==122){
				System.out.println("ciao");
			}*/
	}

	protected double[] computeNextydotOfMeans(double t,double[] y, double[] ydot) {
		Arrays.fill(ydot, 0);

		double[] nv = new double[crn.getReactions().size()];
		
		if(Terminator.hasToTerminate(terminator)){
			return nv;
		}
		
		int r=0; 

		for (ICRNReaction reaction : crn.getReactions()) {
			if(Terminator.hasToTerminate(terminator)){
				return nv;
			}
			IComposite lhs = reaction.getReagents();

			/*if(reaction.hasArbitraryKinetics()){
				// get the total rate in arbitrary kinetics (K S1 S2)
				StringTokenizer st = new StringTokenizer(reaction.getRateExpression());
				st.nextToken();
				String K = st.nextToken();
				nv[r] = crn.getMath().evaluate(K);
				nv[r] *= y[reaction.getFirstArbitraryReagent().getID()];
				nv[r] *= y[reaction.getSecondArbitraryReagent().getID()];	    			
			}
			else */
			if(reaction.hasArbitraryKinetics()){
				for(int s=0;s<speciesASTNodeInRateExpression[r].size();s++){
					ISpecies species = speciesInRateExpression[r].get(s);
					speciesASTNodeInRateExpression[r].get(s).setValue(y[species.getID()]);
				}
				nv[r] = getCRN().getMath().evaluate(rateLaws[r].toString());
				/*ASTNode copiedRateLaw = ((CRNReactionArbitraryGUI)reaction).getRateLaw().clone();
				if(reaction instanceof CRNReactionArbitraryMatlab){
					CRNReactionArbitraryMatlab.replaceVarWithValue(copiedRateLaw, ((CRNReactionArbitraryMatlab) reaction).getVarsName(),speciesIdToSpecies, y);
					String val =  copiedRateLaw.toString();
					//System.out.println(val);
					nv[r] = getCRN().getMath().evaluate(val);
				}
				else if(reaction instanceof CRNReactionArbitraryGUI){
					CRNReactionArbitraryGUI.replaceVarWithValue(copiedRateLaw, speciesNameToSpecies, y);
					String val =  copiedRateLaw.toString();
					//System.out.println(val);
					nv[r] = getCRN().getMath().evaluate(val);
					
				}
				else{
					CRNReducerCommandLine.printWarning("UNSUPPORTED: ODE simulations of arbitrary kinetics: "+reaction);
					throw new UnsupportedOperationException("UNSUPPORTED: ODE simulations of arbitrary kinetics: "+reaction);
					//System.exit(-1);
				}*/
			}
			else{
				// get the total rate in normal mass action
				//nv[r] = reaction.getRate().doubleValue();
				nv[r] = kineticConstants[r];
				for(int i=0;i<lhs.getNumberOfDifferentSpecies();i++){
					ISpecies s = lhs.getAllSpecies(i);
					int mult = lhs.getMultiplicities(i);
					if(mult==1){
						nv[r]*=y[s.getID()];
					}
					else{
						nv[r] *= Math.pow(y[s.getID()], mult);
					}
				}
			}
			
			IComposite rhs = reaction.getProducts();

			//If it is an "ODE reaction"
			if(lhs.getNumberOfDifferentSpecies()==1 && rhs.getNumberOfDifferentSpecies()==1 && 
					lhs.getAllSpecies(0).equals(rhs.getAllSpecies(0)) && 
					lhs.getMultiplicities(0)==1 && rhs.getMultiplicities(0)==2){
				ISpecies s = lhs.getAllSpecies(0);
				ydot[s.getID()] += nv[r];
			}
			else{
				// apply rate
				for(int i=0;i<lhs.getNumberOfDifferentSpecies();i++){
					ISpecies s = lhs.getAllSpecies(i);
					int mult = lhs.getMultiplicities(i);
					ydot[s.getID()] -= nv[r] * mult;
				}

				for(int i=0;i<rhs.getNumberOfDifferentSpecies();i++){
					ISpecies p = rhs.getAllSpecies(i);
					int mult = rhs.getMultiplicities(i);
					ydot[p.getID()] += mult * nv[r];
				}
			}
			r++;
		}

		return nv;
	}

}
