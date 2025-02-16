package it.imt.erode.partitionrefinement.algorithms;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.stream.XMLStreamException;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.auxiliarydatastructures.ArrayListOfReactions;
import it.imt.erode.auxiliarydatastructures.IPartitionAndBoolean;
import it.imt.erode.auxiliarydatastructures.Reduction;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.DialogType;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.importing.AbstractImporter;
import it.imt.erode.importing.ImporterOfSupportedNetworks;
import it.imt.erode.importing.SupportedFormats;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.splittersbstandcounters.ISpeciesCounterHandler;

public class UCTMCLumping {

	public static PartitionAndMappingReactionToNewRate computeCoarsestUCTMCLumpingOrUncertainSE(Reduction red, ICRN crn, IPartition partition,
			BigDecimal delta,BigDecimal deltaPerc,boolean doNotAddDeltaToRawConstants, String modelWithBigM, boolean verbose, MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator,
			IMessageDialogShower msgDialogShower) {
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,red.toString()+" Reducing: "+crn.getName());
		}
		IPartition obtainedPartition = partition.copy();

		if(red.equals(Reduction.UCTMCFE) && !(crn.isMassAction() && crn.getMaxArity() <=1)){
			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it is not a unary mass action RN (i.e., has reactions with more than one reagent, or has reactions with arbitrary rates). I terminate.");
			return new PartitionAndMappingReactionToNewRate(obtainedPartition, null);
		}
		/*
		if(!crn.isMassAction()){
			CRNandAndPartition crnAndSpeciesAndPartition=MatlabODEPontryaginExporter.computeRNEncoding(crn, out, bwOut, partition,true);
			if(crnAndSpeciesAndPartition==null){
				CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it is not a mass action CRN (i.e., it has reactions with arbitrary rates). I terminate.");
				return obtainedPartition;
			}
			crn = crnAndSpeciesAndPartition.getCRN();
			if(crn==null){
				CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it is not a mass action CRN (i.e., it has reactions with arbitrary rates). I terminate.");
				return obtainedPartition;
			}
			obtainedPartition=crnAndSpeciesAndPartition.getPartition();
		}
		 */
		else if(crn.isSymbolic()){
			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it contains symbolic parameters (i.e. parameters with no acutal value assigned) I terminate.");
			return new PartitionAndMappingReactionToNewRate(obtainedPartition, null);
		}
		else if(crn.algebraicSpecies()>0){
			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it is a system of differential algebraic equations (i.e. it has algebraic species) I terminate.");
			return new PartitionAndMappingReactionToNewRate(obtainedPartition, null);
		}

		if(!(red.equals(Reduction.UCTMCFE)||red.equals(Reduction.USE))){
			CRNReducerCommandLine.printWarning(out,bwOut,"Please invoke this method using UCTMCFE or USE. I terminate.");
			return new PartitionAndMappingReactionToNewRate(obtainedPartition, null);
		}

		int uncertainties=0;
		//boolean useExplicitModelWithBigM=false;
		boolean useDelta=false;
		boolean useDeltaPerc=false;
		if(modelWithBigM!=null && modelWithBigM.length()>0) {
			uncertainties++;
			//useExplicitModelWithBigM=true;
		}
		if(delta.compareTo(BigDecimal.ZERO)!=0) {
			uncertainties++;
			useDelta=true;
		}
		if(deltaPerc.compareTo(BigDecimal.ZERO)!=0) {
			uncertainties++;
			useDeltaPerc=true;
		}
		if(uncertainties!=1) {
			CRNReducerCommandLine.printWarning(out,bwOut,"You should either provide a delta (absolute value) or a delta percentage (from 0 to 1) to implicitly build an interval of this size around each transition rate, or provide the path to another model containing parameters to build the M matrix. I terminate.");
			return new PartitionAndMappingReactionToNewRate(obtainedPartition, null);
		}

//		if(red.equals(Reduction.UCTMCFE) && !useDelta) {
//			CRNReducerCommandLine.printWarning(out,bwOut,Reduction.UCTMCFE+" can be computed only with explicit absolute delta. I terminate.");
//			return new PartitionAndMappingReactionToNewRate(obtainedPartition, null);
//		}
		
		if(red.equals(Reduction.USE) && !(useDelta||useDeltaPerc)) {
			CRNReducerCommandLine.printWarning(out,bwOut,"Uncertain SE can be computed only with explicit delta (absolute or perc). I terminate.");
			return new PartitionAndMappingReactionToNewRate(obtainedPartition, null);
		}


		if(verbose){
			String blocks = " blocks";
			if(obtainedPartition.size()==1){
				blocks = " block";
			}
			CRNReducerCommandLine.println(out,bwOut,"Before "+red.toString()+" partitioning we have "+ obtainedPartition.size() + blocks +" and "+crn.getSpecies().size()+ " species.");
		}

		long begin = System.currentTimeMillis();		
		//int iteration =1;

		
//		if(red.equals(Reduction.UCTMCFE)){
//			// USE THIS TO COMPUTE MEASURES FOR THE BOUNDS
//			computeOnlyMeasuresForBound(crn, delta, out, bwOut, terminator);
//		}
		/*
		boolean onlyMeasures=true;
		if(onlyMeasures) {
			computeOnlyMeasuresForBound(crn, delta, out, bwOut, terminator);
			return obtainedPartition;
		}
		*/
		HashMap<ICRNReaction, BigDecimal> reactionToRateInModelBigM=null;
		
				
		if(useDelta||useDeltaPerc) {
			try {
				obtainedPartition=useExplicitDelta(crn, red,useDelta?delta:deltaPerc,useDelta,doNotAddDeltaToRawConstants,out, bwOut, terminator, obtainedPartition);
			} catch (IOException e) {
				CRNReducerCommandLine.printWarning(out,bwOut,"Problems in performing the internal exact reductions. The model is not supported. I terminate.");
				return new PartitionAndMappingReactionToNewRate(obtainedPartition, reactionToRateInModelBigM);
			}
		}
		else {
			CRNReducerCommandLine.print(out,bwOut,"\n\tLoading the model with 'M' transition rates from "+modelWithBigM+" ...");
			ImporterOfSupportedNetworks importerOfSupportedNetworks=new ImporterOfSupportedNetworks();
			AbstractImporter importer=null;
			try {
				if(modelWithBigM.endsWith(".tra")) {
					importer = importerOfSupportedNetworks.importSupportedNetwork(modelWithBigM, false, false,SupportedFormats.MRMC,false,new String[]{"same"},out,bwOut,msgDialogShower, false,false);
				}
				else  if(modelWithBigM.endsWith(".ode")||modelWithBigM.endsWith("._ode")) {
					importer = importerOfSupportedNetworks.importSupportedNetwork(modelWithBigM, false, false,SupportedFormats.CRN,false,null,out,bwOut,msgDialogShower, false,false);
				}
			} catch (UnsupportedFormatException | IOException | XMLStreamException e) {
				
				CRNReducerCommandLine.printWarning(out,bwOut,"\n\tProblems in loading the model with M transition rates. I terminate.");
				msgDialogShower.openSimpleDialog("Problems in loading the model with M transition rates. I terminate.\n"+modelWithBigM, DialogType.Error);
				return new PartitionAndMappingReactionToNewRate(obtainedPartition, reactionToRateInModelBigM);
			}
			CRNReducerCommandLine.print(out,bwOut," completed.");
			ICRN crnBigM = importer.getCRN();
			//partition = importer.getInitialPartition();
			reactionToRateInModelBigM=new HashMap<>(crn.getReactions().size());
			for(int i=0;i<crn.getReactions().size();i++) {
				reactionToRateInModelBigM.put(crn.getReactions().get(i), crnBigM.getReactions().get(i).getRate());

			}
			crnBigM=null;
			importer=null;
			useExplicitModelBigM(crn, reactionToRateInModelBigM, out, bwOut, terminator, obtainedPartition);
		}

		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"The final partition:");
			CRNReducerCommandLine.println(out,bwOut,obtainedPartition);
		}

		if(verbose){
			long end = System.currentTimeMillis();
			CRNReducerCommandLine.println(out,bwOut,red+" Partitioning completed. From "+ crn.getSpecies().size() +" species to "+ obtainedPartition.size() + " blocks. Time necessary: "+(end-begin)+ " (ms)");
			CRNReducerCommandLine.println(out,bwOut,"");
		}
		return new PartitionAndMappingReactionToNewRate(obtainedPartition,reactionToRateInModelBigM);
	}

	public static void computeOnlyMeasuresForBound(ICRN crn, BigDecimal delta, MessageConsoleStream out, BufferedWriter bwOut,Terminator terminator) {
		
		BigDecimal deltaHalf = delta.divide(BigDecimal.valueOf(2));
		
		long begin = System.currentTimeMillis();
		CRNReducerCommandLine.print(out,bwOut,"\n\tScanning the reactions once to pre-compute informations necessary to the partitioning ... ");
		ArrayListOfReactions[] reactionsToConsiderForEachSpecies =  new ArrayListOfReactions[crn.getSpeciesSize()];
		HashMap<IComposite, BigDecimal> multisetCoefficients = new HashMap<IComposite, BigDecimal>();
		CRNBisimulationsNAry.addToReactionsWithNonZeroStoichiometryAndComputeMultisetCoefficients(crn.getReactions(), terminator, reactionsToConsiderForEachSpecies, multisetCoefficients, crn.getMaxArity());
		long end = System.currentTimeMillis();
		CRNReducerCommandLine.print(out,bwOut,"completed in "+ String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0))+" (s)");
		
		CRNReducerCommandLine.print(out,bwOut,"\n\tComputing information required by the bound ... ");
		computeMeasuresForBound(crn,deltaHalf,reactionsToConsiderForEachSpecies,out,bwOut);
	}
	
	private static IPartition useExplicitDelta(ICRN crn, Reduction red,BigDecimal deltaAbsOrPerc, boolean absoluteDelta, boolean doNotAddDeltaToRawConstants,MessageConsoleStream out, BufferedWriter bwOut,
			Terminator terminator, IPartition obtainedPartition) throws IOException  {
		int iteration =1;
		int sizeOfPrevPartition=0;
		int sizeOfPartitionAfterLowerBound=0;
		BigDecimal deltaAbsOrPercHalf = deltaAbsOrPerc.divide(BigDecimal.valueOf(2));
		BigDecimal minusDeltaAbsOrPercHalf = BigDecimal.ZERO.subtract(deltaAbsOrPercHalf);

		if(absoluteDelta) {
			CRNReducerCommandLine.print(out,bwOut," (delta "+deltaAbsOrPerc+")... ");
		}
		else {
			CRNReducerCommandLine.print(out,bwOut," (percentage delta [0;1] "+deltaAbsOrPerc+")... ");
		}
		
		//CRNReducerCommandLine.print(out,bwOut,"\n\tInitially: "+obtainedPartition.size()+" blocks of species.");
		boolean completed = false;
		//
		
		
		long begin = System.currentTimeMillis();
		CRNReducerCommandLine.print(out,bwOut,"\n\tScanning the reactions once to pre-compute informations necessary to the partitioning ... ");
		ArrayListOfReactions[] reactionsToConsiderForEachSpecies =  new ArrayListOfReactions[crn.getSpeciesSize()];
		HashMap<IComposite, BigDecimal> multisetCoefficients = new HashMap<IComposite, BigDecimal>();
		CRNBisimulationsNAry.addToReactionsWithNonZeroStoichiometryAndComputeMultisetCoefficients(crn.getReactions(), terminator, reactionsToConsiderForEachSpecies, multisetCoefficients, crn.getMaxArity());
		long end = System.currentTimeMillis();
		CRNReducerCommandLine.print(out,bwOut,"completed in "+ String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0))+" (s)");
		
		do {
			sizeOfPrevPartition = obtainedPartition.size();

			CRNReducerCommandLine.print(out,bwOut,"\n\tIteration "+iteration);
			CRNReducerCommandLine.print(out,bwOut,"\n\tComputing exact reduction for lower bounds...");
			long beginfe=System.currentTimeMillis();
			HashMap<ISpecies, ISpeciesCounterHandler> speciesCountersHM=new HashMap<>();
			
			if(red.equals(Reduction.USE)) {
				//minusdeltaHalf
				IPartitionAndBoolean obtainedPartitionAndBool = SyntacticMarkovianBisimilarityNary.computeSE(crn, /*labels,*/ obtainedPartition, false,out,bwOut,terminator, null,/*addSelfLoops,*/false,minusDeltaAbsOrPercHalf,absoluteDelta, doNotAddDeltaToRawConstants);
				obtainedPartition=obtainedPartitionAndBool.getPartition();
			}
			else {
				//Here I know that delta is absolute
				CRNBisimulationsNAry.refine(Reduction.FE,  crn, crn.getReactions(), obtainedPartition, null,speciesCountersHM, terminator,out,bwOut,false,false,minusDeltaAbsOrPercHalf,null,reactionsToConsiderForEachSpecies,multisetCoefficients,null);
			}
			long endfe=System.currentTimeMillis();
			CRNReducerCommandLine.println(out,bwOut," completed in: "+String.format( CRNReducerCommandLine.MSFORMAT, ((endfe-beginfe)/1000.0) )+ " (s).");

			sizeOfPartitionAfterLowerBound = obtainedPartition.size();

			CRNReducerCommandLine.print(out,bwOut,"\tComputing exact reduction for upper bounds...");
			beginfe=System.currentTimeMillis();
			speciesCountersHM=new HashMap<>();
			if(red.equals(Reduction.USE)) {
				//deltaHalf
				IPartitionAndBoolean obtainedPartitionAndBool = SyntacticMarkovianBisimilarityNary.computeSE(crn, /*labels,*/ obtainedPartition, false,out,bwOut,terminator, null,/*addSelfLoops,*/false,deltaAbsOrPercHalf,absoluteDelta, doNotAddDeltaToRawConstants);
				obtainedPartition=obtainedPartitionAndBool.getPartition();
			}
			else {
				CRNBisimulationsNAry.refine(Reduction.FE,  crn,crn.getReactions(), obtainedPartition, null,speciesCountersHM, terminator,out,bwOut,false,false,deltaAbsOrPercHalf,null,reactionsToConsiderForEachSpecies,multisetCoefficients,null);
			}
			endfe=System.currentTimeMillis();
			CRNReducerCommandLine.println(out,bwOut," completed in: "+String.format( CRNReducerCommandLine.MSFORMAT, ((endfe-beginfe)/1000.0) )+ " (s).");

			CRNReducerCommandLine.print(out,bwOut,"\tAfter iteration "+iteration+": "+obtainedPartition.size()+" blocks.");
			iteration++;
			//I stop if prev == obtained or afterLower == obtained
			//}while(sizeOfPrevPartition != obtainedPartition.size());
			if(sizeOfPrevPartition == obtainedPartition.size()) {
				completed = true;
			}
			if(sizeOfPartitionAfterLowerBound == obtainedPartition.size()) {
				completed=true;
			}
		}while(!completed);
		
		return obtainedPartition;
	}

	private static boolean computeMeasuresForBound(ICRN crn, BigDecimal deltaHalf, ArrayListOfReactions[] reactionsToConsiderForEachSpecies, MessageConsoleStream out, BufferedWriter bwOut) {
		
		BigDecimal Lambda= new BigDecimal(Double.MIN_VALUE);
		//non-deterministic transitions coming in and leaving state i
		int[] deg=new int[crn.getSpecies().size()];
		int maxDeg=0;
		for(int i=0;i<reactionsToConsiderForEachSpecies.length;i++) {
			if(reactionsToConsiderForEachSpecies[i]!=null) {
				ArrayList<ICRNReaction> ri = reactionsToConsiderForEachSpecies[i].reactions;
				deg[i]=0;
				if(ri!=null && ri.size()>0) {
					BigDecimal temp = BigDecimal.ZERO; 
					for(ICRNReaction r : ri) {
						if(!r.getReagents().getFirstReagent().equals(r.getProducts().getFirstReagent())) {
							//All transitions are non-determinstic
							deg[i]+=1;
							temp=temp.add(r.getRate());
							temp=temp.add(deltaHalf);
						}
					}
					if(temp.compareTo(Lambda)>0) {
						Lambda=temp;
					}
				}
				if(deg[i]>maxDeg) {
					maxDeg=deg[i];
				}
			}
		}
		
		//Outgoing transitions from state i
		int[] degoAll=new int[crn.getSpecies().size()];
		//Outgoing non-determinstic transitions from state i. In our experiments, this coincides with degoAll
		int[] dego=new int[crn.getSpecies().size()];
		int maxDego=0;
		int sumDegoAll=0;
		int sumTwoTotheDego=0;
		
		for(ICRNReaction r : crn.getReactions()) {
			if(r.getRate().compareTo(BigDecimal.ZERO)!=0) {
				ISpecies source = r.getReagents().getFirstReagent();
				degoAll[source.getID()]+=1;
				dego[source.getID()]+=1;
			}
		}
		for(int i=0;i<dego.length;i++) {
			if(dego[i]>maxDego) {
				maxDego=dego[i];
			}
		}
		for(int i=0;i<degoAll.length;i++) {
			sumDegoAll+=degoAll[i];
		}
		for(int i=0;i<dego.length;i++) {
			sumTwoTotheDego+=Math.pow(2, dego[i]);
		}
		
		CRNReducerCommandLine.print(out,bwOut,"\n\t Lambda="+ Lambda.doubleValue() + " max_i deg(i)=" + maxDeg + " max_i deg_o(i)=" + maxDego);
		CRNReducerCommandLine.print(out,bwOut,"\n\t Time to solve the DTMDP=k*"+ BigDecimal.valueOf(sumDegoAll).multiply(BigDecimal.valueOf(sumTwoTotheDego)));
		
		return true;
	}

	private static void useExplicitModelBigM(ICRN crn, HashMap<ICRNReaction, BigDecimal> reactionToRateInModelBigM, MessageConsoleStream out, BufferedWriter bwOut,
			Terminator terminator, IPartition obtainedPartition) {
		int iteration =1;
		int sizeOfPrevPartition=0;
		int sizeOfPartitionAfterLowerBound=0;

		boolean completed = false;
		//
		do {
			sizeOfPrevPartition = obtainedPartition.size();

			CRNReducerCommandLine.print(out,bwOut,"\n\tIteration "+iteration);
			CRNReducerCommandLine.print(out,bwOut,"\n\tComputing FE for lower bounds...");
			long beginfe=System.currentTimeMillis();
			HashMap<ISpecies, ISpeciesCounterHandler> speciesCountersHM=new HashMap<>();
			CRNBisimulationsNAry.refine(Reduction.FE,  crn,crn.getReactions(), obtainedPartition, null,speciesCountersHM, terminator,out,bwOut,false,null);
			long endfe=System.currentTimeMillis();
			CRNReducerCommandLine.println(out,bwOut," completed in: "+String.format( CRNReducerCommandLine.MSFORMAT, ((endfe-beginfe)/1000.0) )+ " (s).");

			sizeOfPartitionAfterLowerBound = obtainedPartition.size();

			CRNReducerCommandLine.print(out,bwOut,"\tComputing FE for upper bounds...");
			beginfe=System.currentTimeMillis();
			speciesCountersHM=new HashMap<>();
			CRNBisimulationsNAry.refine(Reduction.FE,  crn,crn.getReactions(), obtainedPartition, null,speciesCountersHM, terminator,out,bwOut,false,false,null,reactionToRateInModelBigM);
			endfe=System.currentTimeMillis();
			CRNReducerCommandLine.println(out,bwOut," completed in: "+String.format( CRNReducerCommandLine.MSFORMAT, ((endfe-beginfe)/1000.0) )+ " (s).");

			CRNReducerCommandLine.print(out,bwOut,"\tAfter iteration "+iteration+": "+obtainedPartition.size()+" blocks.");
			iteration++;
			//I stop if prev == obtained or afterLower == obtained
			//}while(sizeOfPrevPartition != obtainedPartition.size());
			if(sizeOfPrevPartition == obtainedPartition.size()) {
				completed = true;
			}
			if(sizeOfPartitionAfterLowerBound == obtainedPartition.size()) {
				completed=true;
			}
		}while(!completed);
	}
}
