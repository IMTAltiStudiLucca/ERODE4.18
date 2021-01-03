package it.imt.erode.simulation.deterministic.apachecommons;

import java.util.Vector;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math3.ode.sampling.FixedStepHandler;
import org.apache.commons.math3.ode.sampling.StepNormalizer;
import org.apache.commons.math3.ode.sampling.StepNormalizerBounds;

import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.simulation.deterministic.CRNVectorField;

public class CRNVectorFieldApache extends CRNVectorField implements IVectorFieldApache {

//	private ICRN crn;
//	private String[] rateLawsString;
//	private double[] kineticConstants;
//	private ISpecies[][] speciesInRateExpression;
//	private Terminator terminator;

	
	
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

	//private int iteration=0;
	
//	public Terminator getTerminator(){
//		return terminator;
//	}
	
	
	public CRNVectorFieldApache(ICRN crn, Terminator terminator) {
		super(crn,terminator);
//		this.crn=crn;
//		this.terminator=terminator;
//		init(crn);
	}


//	private void init(ICRN crn) {
//		ISpecies[] speciesIdToSpecies = new ISpecies[crn.getSpecies().size()];
//		//crn.getSpecies().toArray(speciesIdToSpecies);
//		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
//		int i=0;
//		for (ISpecies species : crn.getSpecies()) {
//			speciesNameToSpecies.put(species.getName(), species);
//			speciesIdToSpecies[i]=species;
//			i++;
//		}
//		
//		if(!crn.isMassAction()){
//			rateLawsString = new String[crn.getReactions().size()];
//			speciesInRateExpression= new ISpecies[crn.getReactions().size()][];
//			i=0;
//			for(ICRNReaction reaction : crn.getReactions()){
//				if(reaction instanceof CRNReactionArbitraryAbstract){
//					CRNReactionArbitraryAbstract arb = (CRNReactionArbitraryAbstract)reaction;
//					rateLawsString[i]=arb.getRateExpression();
//					List<ASTNode> app = arb.getSpeciesInRateLaw(arb.getRateLaw(),speciesNameToSpecies);
//					speciesInRateExpression[i]=new ISpecies[app.size()];
//					int p=0;
//					for (ASTNode astNode : app) {
//						speciesInRateExpression[i][p]=speciesNameToSpecies.get(astNode.getName());
//						p++;
//					}
//				}
//				else{
//					rateLawsString[i]=reaction.getRateExpression();
//					speciesInRateExpression[i]=new ISpecies[0];
//				}
//				i++;
//			}
//		}
//
//		kineticConstants=new double[crn.getReactions().size()];
//		int r=0;
//		for (ICRNReaction reaction : crn.getReactions()) {
//			if(! (reaction instanceof CRNReactionArbitraryAbstract)){
//				kineticConstants[r]=reaction.getRate().doubleValue();
//			}
//			r++;
//		}
//	}
	
//	protected ICRN getCRN(){
//		return crn;
//	}

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
		return getCRN().getSpecies().size();
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

//	protected double[] computeNextydotOfMeans(double t,double[] y, double[] ydot) {
//		Arrays.fill(ydot, 0);
//
//		double[] nv = new double[crn.getReactions().size()];
//		
//		if(Terminator.hasToTerminate(terminator)){
//			return nv;
//		}
//		
//		int r=0; 
//
//		for (ICRNReaction reaction : crn.getReactions()) {
//			if(Terminator.hasToTerminate(terminator)){
//				return nv;
//			}
//			IComposite lhs = reaction.getReagents();
//
//			/*if(reaction.hasArbitraryKinetics()){
//				// get the total rate in arbitrary kinetics (K S1 S2)
//				StringTokenizer st = new StringTokenizer(reaction.getRateExpression());
//				st.nextToken();
//				String K = st.nextToken();
//				nv[r] = crn.getMath().evaluate(K);
//				nv[r] *= y[reaction.getFirstArbitraryReagent().getID()];
//				nv[r] *= y[reaction.getSecondArbitraryReagent().getID()];	    			
//			}
//			else */
//			if(reaction.hasArbitraryKinetics()){
//				for(int s=0;s<speciesInRateExpression[r].length;s++){
//					ISpecies species = speciesInRateExpression[r][s];
//					//TODO: PAY ATTENTION: FOR NAMES NOT SUPPORTED BY MATHEVAL WE WILL HAVE PROBLEMS
//					String name=species.getName();
//					getCRN().getMath().setVariable(name, y[species.getID()]);
//				}
//				
//				nv[r] = getCRN().getMath().evaluate(rateLawsString[r]);
//			}
//			else{
//				// get the total rate in normal mass action
//				//nv[r] = reaction.getRate().doubleValue();
//				nv[r] = kineticConstants[r];
//				for(int i=0;i<lhs.getNumberOfDifferentSpecies();i++){
//					ISpecies s = lhs.getAllSpecies(i);
//					int mult = lhs.getMultiplicities(i);
//					if(mult==1){
//						nv[r]*=y[s.getID()];
//					}
//					else{
//						nv[r] *= Math.pow(y[s.getID()], mult);
//					}
//				}
//			}
//			
//			IComposite rhs = reaction.getProducts();
//
//			//If it is an "ODE reaction"
//			if(lhs.getNumberOfDifferentSpecies()==1 && rhs.getNumberOfDifferentSpecies()==1 && 
//					lhs.getAllSpecies(0).equals(rhs.getAllSpecies(0)) && 
//					lhs.getMultiplicities(0)==1 && rhs.getMultiplicities(0)==2){
//				ISpecies s = lhs.getAllSpecies(0);
//				ydot[s.getID()] += nv[r];
//			}
//			else{
//				// apply rate
//				for(int i=0;i<lhs.getNumberOfDifferentSpecies();i++){
//					ISpecies s = lhs.getAllSpecies(i);
//					int mult = lhs.getMultiplicities(i);
//					ydot[s.getID()] -= nv[r] * mult;
//				}
//
//				for(int i=0;i<rhs.getNumberOfDifferentSpecies();i++){
//					ISpecies p = rhs.getAllSpecies(i);
//					int mult = rhs.getMultiplicities(i);
//					ydot[p.getID()] += mult * nv[r];
//				}
//			}
//			r++;
//		}
//
//		return nv;
//	}

}
