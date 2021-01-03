package it.imt.erode.smc.multivesta;

public class FERNState {
	
}

//import java.io.BufferedWriter;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.HashMap;
//import java.util.Map;
//
//import fern.simulation.Simulator;
//import fern.simulation.algorithm.GibsonBruckSimulator;
//import fern.simulation.algorithm.GillespieEnhanced;
//import fern.simulation.algorithm.GillespieSimple;
//import fern.simulation.algorithm.HybridMaximalTimeStep;
//import fern.simulation.algorithm.TauLeapingAbsoluteBoundSimulator;
//import fern.simulation.algorithm.TauLeapingRelativeBoundSimulator;
//import fern.simulation.algorithm.TauLeapingSpeciesPopulationBoundSimulator;
//import fern.simulation.controller.DefaultController;
//import fern.simulation.observer.AmountIntervalObserver;
//import fern.simulation.observer.IntervalObserver;
//import fern.simulation.observer.Observer;
//import fern.simulation.observer.TriggerObserver;
//import fern.tools.Stochastics;
//import it.imt.erode.commandline.CRNReducerCommandLine;
//import it.imt.erode.commandline.CommandsReader;
//import it.imt.erode.crn.interfaces.ICRN;
//import it.imt.erode.crn.interfaces.ISpecies;
//import it.imt.erode.expression.evaluator.MathEval;
//import it.imt.erode.importing.UnsupportedFormatException;
//import it.imt.erode.simulation.stochastic.fern.FernNetworkFromLoadedCRN;
//import vesta.mc.NewState;
//import vesta.mc.ParametersForState;
//
//public class FERNState extends NewState {
//
//	/**
//	 * 
//	 */
//	private static final long serialVersionUID = 7687081013290879726L;
//	private Simulator sim;
//	private Observer[] observer;
//	private DefaultController timeController;
//	private double maxTime;
//	private String method;
//	//private String indexToSpeciesNameSupportedByMathEval[];
//	private String[] indexToSpeciesName;
//	boolean postRunInvoked;
//	private ICRN crn;
//	private FernNetworkFromLoadedCRN net;
//	//private boolean haveViews;
//	//private Map<String, Integer> speciesNameSupportedByMathEvalToIndex;
//	private Map<String, Integer> speciesNameToIndex;
//	private Map<String, Integer> viewNameToIndex;
//	boolean firstSimulation=true;
//		
//	//public static final String allPopulationsMultiQuaTExExpressionFileName="allPopulations.quatex";
//	public static final String allPopulationsMultiQuaTExExpressionFileNameWithoutExtension="allSpecies";
//	public static final String allViewsMultiQuaTExExpressionFileNameWithoutExtension="allViews";
//	public static final String allPopulationsAndViewsMultiQuaTExExpressionFileNameWithoutExtension="allSpeciesAndViews";
//
//	public FERNState(ParametersForState parameters) {
//		super(parameters);
//		
//		
//		String[] args = new String[2];
//		args[0]="-c";
//		args[1]=parameters.getModel();
//		CommandsReader commandsReader = new CommandsReader(args,false,null,null);
//		CRNReducerCommandLine crnReducer = new CRNReducerCommandLine(commandsReader);
//		try {
//			crnReducer.executeCommands(false,null,null);
//		} catch (IOException e) {
//			CRNReducerCommandLine.printStackTrace(null,null,e);
//		} catch (InterruptedException e) {
//			CRNReducerCommandLine.printStackTrace(null,null,e);
//		} catch (UnsupportedFormatException e) {
//			CRNReducerCommandLine.printStackTrace(null,null,e);
//		} catch (Exception e) {
//			CRNReducerCommandLine.printStackTrace(null,null,e);
//		}
//		
//		crn=crnReducer.getCRN();
//		net = new FernNetworkFromLoadedCRN(crn,0);
//		//indexToSpeciesNameSupportedByMathEval = net.getIndexToSpeciesId();
//		//speciesNameSupportedByMathEvalToIndex=net.getSpeciesMapping();
//		indexToSpeciesName= crn.createArrayOfAllSpeciesNames();
//		
//		speciesNameToIndex = new HashMap<String, Integer>(crn.getSpecies().size());
//		int s=0;
//		for (ISpecies species : crn.getSpecies()) {
//			speciesNameToIndex.put(species.getName(), s);
//			s++;
//		}
//		
//		viewNameToIndex = new HashMap<String, Integer>(crn.getViewNames().length);
//		for (int v=0; v<crn.getViewNames().length;v++) {
//			if(!crn.getViewExpressionsUsesCovariances()[v]){
//				viewNameToIndex.put(crn.getViewNames()[v], v);
//			}
//		}
//		
//		String[] otherParams=parameters.getOtherParameters().trim().split(" ");
//		for(int i=0; i< otherParams.length;i++){
//			otherParams[i]=otherParams[i].trim();
//			if(otherParams[i].equals("--maxTime")){
//				maxTime=Double.valueOf(otherParams[i+1].trim());
//				i++;
//			}
//			else if(otherParams[i].equals("--method")){
//				method=otherParams[i+1].trim();
//				i++;
//			}
//		}
//		
//		if(method.equalsIgnoreCase("ssa")){
//			sim= new GillespieSimple(net);
//		}
//		else if(method.equalsIgnoreCase("ssa+")){
//			sim= new GillespieEnhanced(net);
//		}
//		else if(method.equalsIgnoreCase("nextReaction")){
//			sim= new GibsonBruckSimulator(net);
//		}
//		else if(method.equalsIgnoreCase("tauLeapingAbs")){
//			sim= new TauLeapingAbsoluteBoundSimulator(net);
//		}
//		else if(method.equalsIgnoreCase("tauLeapingRelProp")){
//			sim= new TauLeapingRelativeBoundSimulator(net);
//		}
//		else if(method.equalsIgnoreCase("tauLeapingRelPop")){
//			sim= new TauLeapingSpeciesPopulationBoundSimulator(net);
//		}
//		else if(method.equalsIgnoreCase("maximalTimeStep")){
//			sim= new HybridMaximalTimeStep(net);
//		}
//		
//		String plottedSpecies[] = net.getIndexToSpeciesId();
//		IntervalObserver amountObs = (IntervalObserver) sim.addObserver(new AmountIntervalObserver(sim,0.1,plottedSpecies));
//		observer = new Observer[1];
//		observer[0]=amountObs;
//		
//		/*if(crn.getViewNames()!=null && crn.getViewNames().length!=0 && !crnReducer.allViewUseCovariance()){
//			haveViews=true;
//		}
//		else{
//			haveViews=false;
//		}*/
//		
//	}
//	
//	@Override
//	public void setSimulatorForNewSimulation(int randomSeed) {
//		if(firstSimulation){
//			firstSimulation=false;
//		}
//		else if(!postRunInvoked){
//			sim.postRun();
//		}
//		Stochastics.getInstance().setSeed(randomSeed);
//		timeController = new DefaultController(maxTime);
//		postRunInvoked=false;
//		sim.initialize();
//	}
//
//	@Override
//	public void performOneStepOfSimulation() {
//		if(getNumberOfSteps()==0){
//			for (int o=0; o<observer.length; o++)
//				observer[o].started();
//		}
//		for (Observer o : observer) {
//			if (o instanceof TriggerObserver)
//				((TriggerObserver)o).trigger();
//		}
//		for (int o=0; o<observer.length; o++)
//			observer[o].step();
//		
//		sim.performStep(timeController);
//		
//		/*if(!timeController.goOn(sim)){
//			sim.postRun();
//			postRunInvoked=true;
//		}*/
//	}
//
//	@Override
//	public void performWholeSimulation() {
//		sim.start(maxTime);
//		postRunInvoked=true;
//	}
//	
//	@Override
//	public double getTime() {
//		double time = sim.getTime();
//		if(time==Double.POSITIVE_INFINITY){
//			return maxTime;
//		}
//		else if(time==Double.NEGATIVE_INFINITY){
//			return 0;
//		}
//		else{
//			return time;
//		}
//	}
//	
//	/*@Override
//	public boolean getIsSimulationFinished(){
//		return !timeController.goOn(sim);
//	}*/
//
//	@Override
//	public double rval(int observation) {
//		//I assume that it is the id of a species
//		if(observation<0 || observation>= indexToSpeciesName.length){
//			throw new IllegalArgumentException("The observation " + observation + " has not been defined.");
//		}
//		else{
//			return sim.getAmount(observation);
//		}
//		/*switch(observation) {
//		case OBSERVE_TIME: return getTime();
//		case OBSERVE_STEP: return getNumberOfSteps();
//		case OBSERVE_DONE: return getIsSimulationFinished() ? 1.0 : 0.0;
//		//I assume that it is the id of a species
//		default:           
//			
//			throw new IllegalArgumentException("The observation " + observation + " has not been defined.");
//		}*/
//	}
//	
//	@Override
//	public double rval(String observation) {
//		if (observation.equalsIgnoreCase("time"))
//			return getTime();
//		if (observation.equalsIgnoreCase("steps"))
//			return getNumberOfSteps();
//		if (observation.equalsIgnoreCase("completed"))
//			return getIsSimulationFinished() ? 1.0 : 0.0;
//
//		String specieName = observation;
//		//Integer speciesId = this.speciesNameSupportedByMathEvalToIndex.get(specieName);
//		Integer speciesId = speciesNameToIndex.get(specieName);
//		if(speciesId!=null){
//			return sim.getAmount(speciesId);
//		}
//		else{
//			//speciesId==null
//			String viewName = specieName;
//			Integer viewId = viewNameToIndex.get(viewName);
//			if(viewId==null){
//				throw new IllegalArgumentException("The observation " + observation + " has not been defined.");
//			}
//			else{
//				//update species amounts in mathEval
//				String viewExpr = crn.getViewExpressionsSupportedByMathEval()[viewId];
//				MathEval math = crn.getMath();
//				for (String speciesNameSupportedByMathEval : math.getVariablesWithin(viewExpr)) {
//					int pos = sim.getNet().getSpeciesByName(speciesNameSupportedByMathEval);
//					//If pos is -1, then the string is not a species but a parameter, and I do not have to update it.
//					if(pos!=-1){
//						math.setVariable(speciesNameSupportedByMathEval, sim.getAmount(pos));
//					}
//				}
//				return crn.getMath().evaluate(viewExpr);
//			}
//		}
//	}
//	
//	public static void createMultiQuaTExQuery(ICRN crn,double maxTime,double interval, String query) throws IOException {
//		/*popuplationAtTime(x,name_S) = 
//		  if { s.rval("time") >= x }
//		     then { s.rval(name_S) } 
//		     else # popuplationAtTime({x},{name_S}) 
//		  fi ;
//		eval parametric(E[ popuplationAtTime(x,{"x0"}) ],E[ popuplationAtTime(x,{"x1"}) ],E[ popuplationAtTime(x,{"x2"}) ],x,0.0,0.1,5.0) ;*/
//
//		FileWriter multiQuaTExFile=new FileWriter(query+".quatex");
//		BufferedWriter writer= new BufferedWriter(multiQuaTExFile);
//		
//		writer.write("populationAtTime(x,name_S) =\n");
//		writer.write(" if { s.rval(\"time\") >= x }\n");
//		writer.write("  then { s.rval(name_S) }\n");
//		writer.write("  else # populationAtTime({x},{name_S})\n");
//		writer.write(" fi ;\n");
//		
//		
//		/*if(query.equals(allPopulationsMultiQuaTExExpressionFileNameWithoutExtension) || 
//				query.equals(allPopulationsAndViewsMultiQuaTExExpressionFileNameWithoutExtension)){
//			writer.write("speciesPopuplationAtTime(x,name_S) =\n");
//			writer.write(" if { s.rval(\"time\") >= x }\n");
//			writer.write("  then { s.rval(name_S) }\n");
//			writer.write("  else # speciesPopuplationAtTime({x},{name_S})\n");
//			writer.write(" fi ;\n");
//		}
//		if(query.equals(allViewsMultiQuaTExExpressionFileNameWithoutExtension) || 
//				query.equals(allPopulationsAndViewsMultiQuaTExExpressionFileNameWithoutExtension)){
//			writer.write("viewPopulationAtTime(x,name_S) =\n");
//			writer.write(" if { s.rval(\"time\") >= x }\n");
//			writer.write("  then { s.rval(name_S) }\n");
//			writer.write("  else # viewPopulationAtTime({x},{name_S})\n");
//			writer.write(" fi ;\n");
//		}*/
//		writer.write("eval parametric(");
//		if(query.equals(allPopulationsMultiQuaTExExpressionFileNameWithoutExtension) || 
//				query.equals(allPopulationsAndViewsMultiQuaTExExpressionFileNameWithoutExtension)){
//			for (ISpecies species : crn.getSpecies()) {
//				writer.write("E[ populationAtTime(x,{\""+species.getName()+"\"}) ],");
//			}
//		}
//		if(query.equals(allViewsMultiQuaTExExpressionFileNameWithoutExtension) || 
//				query.equals(allPopulationsAndViewsMultiQuaTExExpressionFileNameWithoutExtension)){
//			for(int v=0;v<crn.getViewNames().length;v++){
//				if(!crn.getViewExpressionsUsesCovariances()[v]){
//					writer.write("E[ populationAtTime(x,{\""+crn.getViewNames()[v]+"\"}) ],");
//				}
//			}
//		}
//
//		writer.write("x,0.0,"+interval+","+maxTime+") ;\n");
//
//		writer.close();
//		multiQuaTExFile.close();
//	}
//
//}
