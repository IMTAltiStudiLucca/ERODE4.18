package it.imt.erode.simulation.stochastic.ctmcgenerator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Stack;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.DialogType;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.implementations.CRN;
import it.imt.erode.crn.implementations.CRNReaction;
import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.importing.AbstractImporter;
import it.imt.erode.partitionrefinement.algorithms.CRNBisimulationsNAry;
//import it.imtlucca.util.RandomEngineFacilities;
import cern.jet.random.engine.MersenneTwister;

public class CTMCGenerator {
	

	public void generateLogs(ICRN crn,int simulations, int steps, int sots, String fileName,MessageConsoleStream out,BufferedWriter bwOut,boolean verbose, Terminator terminator, IMessageDialogShower msgDialogShower){
		if(terminator==null){
			terminator=new Terminator();
		}
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"Logs generation of: "+crn.getName());
		}
		if(!crn.isElementary()){
			CRNReducerCommandLine.printWarning(out,bwOut, true, msgDialogShower, "The model is not supported because it is not an elementary CRN (i.e., it has ternary or more reactions). I terminate.",DialogType.Error);
		}
		else if(!crn.isMassAction()){
			CRNReducerCommandLine.printWarning(out,bwOut,true,msgDialogShower,"The model is not supported because it is not a mass action CRN (i.e., it has reactions with arbitrary rates). I terminate.",DialogType.Error);
		}
		else if(crn.isSymbolic()){
			CRNReducerCommandLine.printWarning(out,bwOut,true,msgDialogShower,"The model is not supported because it contains symbolic parameters (i.e. parameters with no acutal value assigned) I terminate.",DialogType.Error);
		}

		//Case ID,Activity,Start Timestamp,Complete Timestamp,Variant,Variant index,Agent Position,Customer ID,Product,Service Type,Resource

		long initTime = System.currentTimeMillis();
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");

		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printToNetFile, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}

		try {
			bw.write("Case ID,Activity,Start Timestamp,Complete Timestamp\n");

			if(sots==0) {
				sots=(int)System.currentTimeMillis();
			}
			MersenneTwister mtForSeeds = new MersenneTwister(sots);
			//RandomEngineFacilities randomGeneratorForSeeds = new RandomEngineFacilities(mt);

			for(int sim = 0;sim<simulations;sim++) {
				IComposite initialState = computeInitialState(crn);
				IComposite currentState = initialState;
				double currentTime = 0;
				long scaledEndTime=0;
				for(int step=0;step<steps;step++) {
					int seed = mtForSeeds.nextInt();
					//RandomEngineFacilities randomGeneratorOfSimulation = new RandomEngineFacilities(new MersenneTwister(seed));
					MersenneTwister mtForSim = new MersenneTwister(seed);
					LinkedHashMap<Double, CTMCTransition> cumulProbToReaction = new LinkedHashMap<>();
					double cumulProb=0;
					for (int r=0;r<crn.getReactions().size();r++) {
						ICRNReaction reaction = crn.getReactions().get(r);
						if(terminator.hasToTerminate()){
							break;
						}
						CTMCTransition ctmcTransition = fire(reaction,currentState,out,bwOut);
						if(ctmcTransition!=null){
							BigDecimal firingRate = ctmcTransition.getRate();
							if(firingRate.compareTo(BigDecimal.ZERO)!=0) {
								cumulProb+=firingRate.doubleValue();
								cumulProbToReaction.put(cumulProb, ctmcTransition);
							}
						}
					}
					
					CTMCTransition chosen = null;
					if(cumulProb==0) {
						//We got in an absorbing state.  
						break;
					}
					else {
						double sample = mtForSim.nextDouble()*cumulProb;

						for(Entry<Double, CTMCTransition> entry : cumulProbToReaction.entrySet()) {
							if(sample <= entry.getKey()) {
								chosen=entry.getValue();
								break;
							}
						}
						//Case ID,Activity,Start Timestamp,Complete Timestamp,Variant,Variant index,Agent Position,Customer ID,Product,Service Type,Resource
						double elapsedTime=sampleFromExponential(cumulProb,mtForSim.nextDouble());//sample from exponential
						double endTime = currentTime+elapsedTime;
						scaledEndTime = (long)(initTime + endTime*1000);
						long scaledCurrentTime = (long)(initTime+ currentTime*1000);
						String startDate = dateFormat.format(new Date(scaledCurrentTime));
						String endDate = dateFormat.format(new Date(scaledEndTime));


						//String myDateStr = new SimpleDateFormat("dd-MM-yyyy").format(itemDate);
						//2010/03/20 10:48:00.000
						bw.write("Case "+sim+","+chosen.getReaction().getID()+","+startDate+","+endDate+"\n");
						currentState = chosen.getTarget();
						currentTime=endTime;
					}
				}
				//The next simulation will begin 1 second after the previous one
				initTime = scaledEndTime + 1000;
			}
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in generateLogs, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		finally{
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"Writing logs in file "+fileName+" completed");
			}
			try {
				bw.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in generateLogs, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}
	}
	
	/**
	 * 
	 * @param rate the rate of the exponential
	 * @param nextDouble a sampled double in (0,1) 
	 * @return a sample from the exponential distribution with provided rate 
	 */
	private static double sampleFromExponential(double rate, double nextDouble){
		return - Math.log(1 - nextDouble) / rate;
	}

	public ICRN generateCTMC(ICRN crn,HashMap<String, Integer> speciesToLimit, MessageConsoleStream out,BufferedWriter bwOut,boolean verbose, Terminator terminator, IMessageDialogShower msgDialogShower){
		if(terminator==null){
			terminator=new Terminator();
		}
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"CTMC generation of: "+crn.getName());
		}
		if(!crn.isElementary()){
			CRNReducerCommandLine.printWarning(out,bwOut, true, msgDialogShower, "The model is not supported because it is not an elementary CRN (i.e., it has ternary or more reactions). I terminate.",DialogType.Error);
			return null;
		}
		else if(!crn.isMassAction()){
			CRNReducerCommandLine.printWarning(out,bwOut,true,msgDialogShower,"The model is not supported because it is not a mass action CRN (i.e., it has reactions with arbitrary rates). I terminate.",DialogType.Error);
			return null;
		}
		else if(crn.isSymbolic()){
			CRNReducerCommandLine.printWarning(out,bwOut,true,msgDialogShower,"The model is not supported because it contains symbolic parameters (i.e. parameters with no acutal value assigned) I terminate.",DialogType.Error);
			return null;
		}
		
		//long begin = System.currentTimeMillis();
		
		ICRN ctmc = new CRN(crn.getName()+"ctmc", crn.getMath(),out,bwOut);
		for (String param : crn.getParameters()) {
			int space = param.indexOf(' ');
			String paramName = param.substring(0, space);
			String paramExpr = param.substring(space,param.length());
			//ctmc.addParameter(paramName, paramExpr);
			AbstractImporter.addParameter(paramName, paramExpr, true, ctmc);
		}

		LinkedHashSet<IComposite> ctmcStates = new LinkedHashSet<>();
		ArrayList<CTMCTransition> ctmcTransitions = new ArrayList<>();
		LinkedHashMap<String, ISpecies> nameToSpecies = new LinkedHashMap<>();

		IComposite initialState = computeInitialState(crn);
		ctmcStates.add(initialState);
		ISpecies initialStateSpecies = CTMCTransition.compositeToSpecies(nameToSpecies, initialState);
		initialStateSpecies.setInitialConcentration(BigDecimal.ONE, "1");
		//initialStateSpecies.addCommentLine("Initial state");
		
		Stack<IComposite> ctmcStatesToConsider= new Stack<>();
		ctmcStatesToConsider.push(initialState);
		
		boolean printed=false;
		int interval = 10000;
		int nextPrint=interval;
		//for (IComposite currentState : ctmcStates) {
		while((!ctmcStatesToConsider.isEmpty())&&!terminator.hasToTerminate()){
			if(ctmcStates.size()>=nextPrint){
				if(!printed){
					printed=true;
					CRNReducerCommandLine.print(out,bwOut, "\n\t ( states: ");
				}
				CRNReducerCommandLine.print(out,bwOut, nextPrint+" ");
				nextPrint+=interval;
			}
			IComposite currentState = ctmcStatesToConsider.pop();
			//System.out.println("Current state: "+currentState);
			for (ICRNReaction reaction : crn.getReactions()) {
				if(terminator.hasToTerminate()){
					break;
				}
				CTMCTransition ctmcTransition = fire(reaction,currentState,out,bwOut);
				if(ctmcTransition!=null){
					ctmcTransitions.add(ctmcTransition);
					IComposite targetState = ctmcTransition.getTarget();
					if(!ctmcStates.contains(targetState)){
						ctmcStates.add(targetState);
						ctmcStatesToConsider.push(targetState);
					}
					ICRNReaction crnReaction = ctmcTransition.toCRNReaction(nameToSpecies);
//					IComposite compositeReagents = ctmc.addReagentsIfNew(crnReaction.getReagents());
//					IComposite compositeProducts = ctmc.addProductIfNew(crnReaction.getProducts());
					IComposite compositeReagents = crnReaction.getReagents();
					IComposite compositeProducts = crnReaction.getProducts();
					String id=null;
					if(reaction.getID()!=null) {
						ISpecies sourceState = (ISpecies)crnReaction.getReagents();
						id=reaction.getID()+"_on_"+sourceState.getName();
					}
					crnReaction = new CRNReaction(crnReaction.getRate(), compositeReagents, compositeProducts, crnReaction.getRateExpression(),id);
					ctmc.addReaction(crnReaction);
					/*
					int arity = compositeReagents.getTotalMultiplicity();
					AbstractImporter.addToIncomingReactionsOfProducts(arity,compositeProducts, reaction,CRNReducerCommandLine.addReactionToComposites);
					AbstractImporter.addToOutgoingReactionsOfReagents(arity, compositeReagents, reaction,CRNReducerCommandLine.addReactionToComposites);
					if(CRNReducerCommandLine.hasToAddToReactionsWithNonZeroStoichiometry){
						AbstractImporter.addToReactionsWithNonZeroStoichiometry(arity, reaction.computeProductsMinusReagentsHashMap(),reaction);
					}
					*/
				}
			}
		}
		
		if(printed){
			CRNReducerCommandLine.print(out,bwOut, ")\n\t ...");
		}
		
		if(terminator.hasToTerminate()){
			return null;
		}
		
		for (ISpecies species : nameToSpecies.values()) {
			ctmc.addSpecies(species);
		}
		
		if(terminator.hasToTerminate()){
			return null;
		}
		else{
			return ctmc;
		}
	}

	private CTMCTransition fire(ICRNReaction reaction, IComposite currentState,MessageConsoleStream out,BufferedWriter bwOut) {
		CTMCTransition oneStepTransition=null;
		if(canFire(reaction, currentState)){
			BigDecimal rate = reaction.getRate();
			String rateExpreassion = reaction.getRateExpression();
			IComposite reagents = reaction.getReagents();
			if(reagents.isHomeo()){
				rate = rate.divide(BigDecimal.valueOf(2),CRNBisimulationsNAry.getSCALE(),CRNBisimulationsNAry.RM);
				rateExpreassion = "(" + rateExpreassion + ")/2";
			}
			
			if(reagents.isUnary()){
				int mult = currentState.getMultiplicityOfSpecies(reagents.getFirstReagent());
				rate = rate.multiply(BigDecimal.valueOf(mult));
				if(mult!=1) {
					rateExpreassion = "(" + rateExpreassion + ")*"+mult;
				}
			}
			else if(reagents.isBinary()){
				int mult1=currentState.getMultiplicityOfSpecies(reagents.getFirstReagent());
				int mult2;
				if(reagents.isHomeo()) {
					mult2=mult1-1;
				}
				else {
					mult2=currentState.getMultiplicityOfSpecies(reagents.getSecondReagent());
				}
				
				
				rate = rate.multiply(BigDecimal.valueOf(mult1));
				rate = rate.multiply(BigDecimal.valueOf(mult2));
				if(mult1!=1 && mult2!=1) {
					rateExpreassion = "(" + rateExpreassion + ")*"+mult1+"*"+mult2;
				}
				else if(mult1!=1) {
					rateExpreassion = "(" + rateExpreassion + ")*"+mult1;
				}
				else if(mult2!=1) {
					rateExpreassion = "(" + rateExpreassion + ")*"+mult2;
				}
				/*else {
					rateExpreassion = rateExpreassion;
				}*/
				
			}
			else{
				CRNReducerCommandLine.printWarning(out,bwOut,"The reaction "+ reaction+ "  is not supported because it is not elementary.");
				return null;
			}
			oneStepTransition = new CTMCTransition(currentState,rate,rateExpreassion,currentState.apply(reaction),reaction);
		}
		return oneStepTransition;
	}

	private boolean canFire(ICRNReaction reaction, IComposite currentState) {
		return currentState.contains(reaction.getReagents());
	}

	public static IComposite computeInitialState(ICRN crn) {
		HashMap<ISpecies, Integer> initialStateHM=new HashMap<>();
		for (ISpecies species : crn.getSpecies()) {
			int ip = (int)(species.getInitialConcentration().doubleValue()+0.5);
			if(ip!=0){
				initialStateHM.put(species, ip);
			}
		}
		return new Composite(initialStateHM);
	}

}
