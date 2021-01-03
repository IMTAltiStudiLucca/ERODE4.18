package it.imt.erode.simulation.deterministic;

import java.math.BigDecimal;

import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.simulation.deterministic.apachecommons.CRNVectorFieldApacheCovariances;
import it.imt.erode.simulation.deterministic.apachecommons.CRNVectorFieldApache;
import it.imt.erode.simulation.deterministic.apachecommons.IVectorFieldApache;
import it.imt.erode.simulation.deterministic.jsundials.CRNVectorFieldJSundialsCVODE;
import it.imt.erode.simulation.deterministic.jsundials.CRNVectorFieldJSundialsIDA;

/**
 * @author Andrea Vandin, Mirco Tribastone
 *
 */
public class ODESolver { //extends Simulator {

	private ICRN crn;
	private double[][] solution;
	private double start;
	private double stop;
	private double step;
	private double minStep;
	private double maxStep;
	private double absTol;
	private double relTol;
	private double[] x; 
	private int nSteps;
	private Terminator terminator;
	private boolean covariances;
	private double defaultIC=0.0;
	//private boolean apache;
	private SOLVERLIBRARY library;
	
	public ODESolver(ICRN crn, double start, double stop, double step, int nSteps, double minStep, double maxStep, double absTol, double relTol, Terminator terminator, boolean covariances,double defIC,SOLVERLIBRARY library){
		//super(net)
		this.crn=crn;
		this.start=start;
		this.stop=stop;
		this.step=step;
		this.nSteps=nSteps;
		this.minStep=minStep;
		this.maxStep=maxStep;
		this.absTol=absTol;
		this.relTol=relTol;
		this.terminator=terminator;
		this.covariances=covariances;
		this.defaultIC=defIC;
		this.library=library;
	}

	public void solve() throws NonConsistentInitialConditions{
		//Initial condition of each species
		double[] ic;
		
		switch (library) {
		case APACHE:
			if(covariances){
				//I ALSO SIMULATE THE COVARIANCES
				ic = new double[crn.getSpecies().size() + (crn.getSpecies().size()*crn.getSpecies().size())];
				loadIC(ic);
				//Arrays.fill(ic, crn.getSpecies().size(), ic.length-1, 0.0);
				IVectorFieldApache vf = new CRNVectorFieldApacheCovariances(crn,terminator);
				solution = vf.solve(start, stop, step, ic,minStep, maxStep, absTol, relTol);
			}
			else{
				//I DON'T SIMULATE THE COVARIANCES. I only address the means.
				ic = new double[crn.getSpecies().size()];
				loadIC(ic);
				/*for(int i=0;i<crn.getSpecies().size();i++) {
				ic[i]=crn.getSpecies().get(i).getInitialConcentration().doubleValue();
			}*/
				IVectorFieldApache vf = new CRNVectorFieldApache(crn,terminator);
				//IVectorField vf = new CRNVectorFieldLessEfficientForArbitraryRates(crn,terminator);
				solution = vf.solve(start, stop, step, ic,minStep, maxStep, absTol, relTol);
				
				/*
				System.out.println(System.getProperty("java.library.path"));
				CRNVectorFieldJSundials sundialsSolver = new CRNVectorFieldJSundials(crn, terminator);
				//solution =sundialsSolver.solve(....) 
				int ret = sundialsSolver.solve(crn.getSpecies().size(), ic, start, stop, nSteps, absTol);
				System.out.println("Sundials returned: "+ret);
				*/
			}
			
			break;
		case CVODE:
			ic = new double[crn.getSpecies().size()];
			loadIC(ic);
			//sundials
			CRNVectorFieldJSundialsCVODE cvodeSolver = new CRNVectorFieldJSundialsCVODE(crn, terminator);
			//solution =sundialsSolver.solve(....) 
			solution = cvodeSolver.solve(crn.getSpecies().size(), start, stop, nSteps, ic, absTol, relTol);
			break;
		case IDA:
			ic = new double[crn.getSpecies().size()];
			loadIC(ic);
			/*
			 * WRONG. WE DO NOT NEED IT 
			int alg=0;
			int diff=0;
			for(ISpecies species : crn.getSpecies()) {
				if(species.isAlgebraic()) {
					alg++;
				}
				else {
					diff++;
				}
			}
			ic = new double[diff];
			double[] icAlg = new double[alg];
			loadICDAE(ic, icAlg);
			*/
			
			CRNVectorFieldJSundialsIDA idaSolver = new CRNVectorFieldJSundialsIDA(crn, terminator);
			double icp[] = new double[crn.getSpecies().size()];
			idaSolver.computeICP(ic, icp);
			int i=0;
			BigDecimal tol = new BigDecimal(absTol);
			for(ISpecies species : crn.getSpecies()) {
				if(species.isAlgebraic()){
					boolean equalUpToTolerance=equalUpTolearance(species.getInitialConcentration(),new BigDecimal(icp[i]),tol);
					if(!equalUpToTolerance) {
						throw new NonConsistentInitialConditions("Variable "+species.getName()+" has initial condition "+species.getInitialConcentration().toPlainString() +" while the constraints require "+icp[i]);
					}
				}
				i++;
			}
			solution = idaSolver.solve(crn.getSpecies().size(), start, stop, nSteps, ic, icp, absTol, relTol);
			
			
			
			break;
//		default:
//			break;
		}

		if(!Terminator.hasToTerminate(terminator)){
			x=new double[nSteps];//new double[solution.length];
			for(int i=0;i<x.length;i++){
				x[i]=solution[i][0];
			}
		}
		//double[] line = solution[i]; //all concentrations at ith step
		//line[5] //concentration of species with ide 5 (at ith step)
	}

	private boolean equalUpTolearance(BigDecimal initialConcentration, BigDecimal icp, BigDecimal tol) {
		BigDecimal diff = initialConcentration.subtract(icp).abs();
		if(diff.compareTo(tol)<=0){
			return true;
		}
		else {
			return false;
		}
	}

	private void loadIC(double[] ic) {
		int i=0;
		for(ISpecies species : crn.getSpecies()){
			String icExpr = species.getInitialConcentrationExpr();
			if(icExpr.equals("0")||icExpr.equals("0.0")){
				ic[i]=defaultIC;
			}
			else{
				ic[i]=species.getInitialConcentration().doubleValue();
			}
			i++;
		}
		
		/*
		for(int i=0;i<crn.getSpecies().size();i++) {
			String icExpr = crn.getSpecies().get(i).getInitialConcentrationExpr();
			if(icExpr.equals("0")||icExpr.equals("0.0")){
				ic[i]=defaultIC;
			}
			else{
				ic[i]=crn.getSpecies().get(i).getInitialConcentration().doubleValue();
			}
		}
		*/
	}
	
	/*
	private void loadICDAE(double[] ic, double[] icAlg) {
		int i=0;
		int iAlg=0;
		double val=0;
		for(ISpecies species : crn.getSpecies()){
			String icExpr = species.getInitialConcentrationExpr();
			if(icExpr.equals("0")||icExpr.equals("0.0")){
				val=defaultIC;
			}
			else{
				val=species.getInitialConcentration().doubleValue();
			}
			
			if(species.isAlgebraic()) {
				icAlg[iAlg]=val;
				iAlg++;
			}
			else{
				ic[i]=val;
				i++;
			}
		}
		
		
//		for(int i=0;i<crn.getSpecies().size();i++) {
//			String icExpr = crn.getSpecies().get(i).getInitialConcentrationExpr();
//			if(icExpr.equals("0")||icExpr.equals("0.0")){
//				ic[i]=defaultIC;
//			}
//			else{
//				ic[i]=crn.getSpecies().get(i).getInitialConcentration().doubleValue();
//			}
//		}
		
	}
*/	

	/*public void printSolution(){
		if(solution!=null){
			for(int i=0;i<solution.length;i++){
				CRNReducerCommandLine.println("Solutions at step "+i+": ");
				double[] line = solution[i];
				for(int j=0;j<line.length;j++){
					CRNReducerCommandLine.println("  "+crn.getSpecies().get(j).toStringWithId()+": ");
				}
			}
		}
	}*/

	/**
	 * 
	 * @return the solution of the ODEs: double[] line = solution[i] are all concentrations at ith step (preceded by the x value), while line[5] is the concentration of species with id 4 (at ith step)
	 */
	public double[][] getSolution() {
		return solution;
	}

	public double[] getX() {
		return x;
	}
	
	
	/*
	@Override
	public void reinitialize() {
		//DO NOTHING
		
	}

	@Override
	public void performStep(SimulationController control) {
		solve();
		// advance the time
		t=stop;
		thetaEvent();	
	}

	@Override
	public String getName() {
		return "ODEs solver";
	}*/
	
}
