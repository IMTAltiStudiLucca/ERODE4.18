package it.imt.erode.partitionrefinement.algorithms;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.auxiliarydatastructures.ArrayListOfReactions;
import it.imt.erode.auxiliarydatastructures.CRNandPartition;
import it.imt.erode.auxiliarydatastructures.IPartitionAndBoolean;
import it.imt.erode.auxiliarydatastructures.Reduction;
import it.imt.erode.commandline.CRNReducerCommandLine;
//import it.imt.erode.commandline.DialogType;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.implementations.CRN;
import it.imt.erode.crn.implementations.CRNReaction;
import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.implementations.Species;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.label.EmptySetLabel;
import it.imt.erode.crn.label.ILabel;
import it.imt.erode.crn.label.NAryLabelBuilder;
import it.imt.erode.crn.symbolic.constraints.IConstraint;
import it.imt.erode.expression.evaluator.MathEval;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SpeciesCounterField;
import it.imt.erode.partitionrefinement.splittersbstandcounters.CRNBisNAryOrSENAry;
import it.imt.erode.partitionrefinement.splittersbstandcounters.ISpeciesCounterHandler;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SpeciesCounterHandlerCRNBIsimulationNAry;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SpeciesCounterHandlerSENAry;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SpeciesSplayBST;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SplittersGenerator;
import it.imt.erode.partitionrefinement.splittersbstandcounters.VectorOfBigDecimalsForEachLabel;
import it.imt.erode.utopic.MatlabODEPontryaginExporter;





public class CRNBisimulationsNAry {

	public static final RoundingMode RM = RoundingMode.HALF_DOWN;//if changed, update also copy in matheval
	//public static final RoundingMode RM = RoundingMode.DOWN;
	private static int SCALE = 20;
	public static final int SCALEDefault = 20;
	protected static BigDecimal TOLERANCE = new BigDecimal("1E-"+(SCALE));
	public static int getSCALE() {
		return SCALE;
	}
	public static void setSCALE(int s) {
		SCALE = s;
		TOLERANCE = new BigDecimal("1E-"+(SCALE));
		MathEval.setSCALE(s);
	}
	public static BigDecimal getTolerance() {
		return TOLERANCE;
	}
	//public static final int SCALE = 30;
	
	//

	public static final boolean USETOLERANCE = true;
	
	public static final boolean POSTPONESCALINGOFRATESWHENCOMPUTINGREDUCEDQUOTIENTMODEL = true;
	//public static final boolean POSTPONESCALINGOFRATESWHENCOMPUTINGREDUCEDQUOTIENTMODEL = false;
	
	
	//public static final MathContext MC = new MathContext(30, RoundingMode.HALF_DOWN);
	//public static final BigDecimal TOLERANCE = new BigDecimal("0.00001");
	//public static final MathContext MC = new MathContext(30, RoundingMode.HALF_DOWN);
//public static final BigDecimal EPSILON = BigDecimal.valueOf(0.001);
	
	/**
	 * 
	 * @param crn
	 * @param labels
	 * @param partition the partition to be refined. It is not modified, but instead a new one is created and returned
	 * @param initializeCounters
	 * @param verbose
	 * @param terminator 
	 * @return
	 */
	public static IPartitionAndBoolean computeCoarsest(Reduction red,ICRN crn, IPartition partition, boolean verbose,MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator, IMessageDialogShower msgDialogShower){

		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,red.toString()+" Reducing: "+crn.getName());
		}
		IPartition obtainedPartition = partition.copy();

		/*if(!crn.isElementary()){
			CRNReducerCommandLine.printWarning("The CRN is not supported because it is not elementary (i.e., it has ternary or more reactions). I terminate.");
			return new IPartitionAndBoolean(obtainedPartition, false);
		}*/
		/*if(!crn.isMassAction()){
			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it is not a mass action CRN (i.e., it has reactions with arbitrary rates). I terminate.");
			return new IPartitionAndBoolean(obtainedPartition, false);
		}*/
		if(!crn.isMassAction()){
			CRNandPartition crnAndSpeciesAndPartition=MatlabODEPontryaginExporter.computeRNEncoding(crn, out, bwOut, partition,true);
			if(crnAndSpeciesAndPartition==null){
				CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it is not a mass action CRN (i.e., it has reactions with arbitrary rates). I terminate.");
				return new IPartitionAndBoolean(obtainedPartition, false);
			}
			crn = crnAndSpeciesAndPartition.getCRN();
			if(crn==null){
				CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it is not a mass action CRN (i.e., it has reactions with arbitrary rates). I terminate.");
				return new IPartitionAndBoolean(obtainedPartition, false);
			}
			obtainedPartition=crnAndSpeciesAndPartition.getPartition();
		}
		else if(crn.isSymbolic()){
			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it contains symbolic parameters (i.e. parameters with no acutal value assigned) I terminate.");
			return new IPartitionAndBoolean(obtainedPartition, false);
		}
		
		if(!(red.equals(Reduction.FE)||red.equals(Reduction.BE) /*|| red.equals(Reduction.ENFB)||red.equals(Reduction.ENBB)*/)){
			CRNReducerCommandLine.printWarning(out,bwOut,"Please invoke this method using FE or BE.  I terminate.");
			return new IPartitionAndBoolean(obtainedPartition, false);
		}

		if(verbose){
			String blocks = " blocks";
			if(obtainedPartition.size()==1){
				blocks = " block";
			}
			CRNReducerCommandLine.println(out,bwOut,"Before "+red.toString()+" partitioning we have "+ obtainedPartition.size() + blocks +" and "+crn.getSpeciesSize()+ " species.");
		}
		
//		if(!(CRNReducerCommandLine.hasToAddToReactionsWithNonZeroStoichiometry)){
//			//CRNReducerCommandLine.printWarning(out,bwOut,"Not all necessary data structure have been filled. CRNReducerCommandLine.hasToAddToReactionsWithNonZeroStoichiometry must be set to true.");
//			CRNReducerCommandLine.printWarning(out, bwOut, true, msgDialogShower, "Not all necessary data structure have been filled. CRNReducerCommandLine.hasToAddToReactionsWithNonZeroStoichiometry must be set to true.", DialogType.Error);
//		}

		long begin = System.currentTimeMillis();

//		for(int i=0;i<crn.getReactions().size();i++) {
//			ICRNReaction current =crn.getReactions().get(i);
//			if(i==163) {
//				CRNReducerCommandLine.println(out,bwOut,"\t"+i+" : "+current.getRate());
//			}
//			CRNReducerCommandLine.println(out,bwOut,i+" : "+current.getRate());
//		}
		
		//ISpeciesCounterHandler speciesCounters[] = new ISpeciesCounterHandler[crn.getSpeciesSize()];
		ISpeciesCounterHandler speciesCounters[] = null;
		HashMap<ISpecies, ISpeciesCounterHandler> speciesCountersHM = new HashMap<>(); 
				

		//int maxArity = crn.getMaxArity();
		
//		HashMap<IComposite, BigDecimal> multisetCoefficients = null;
//		if(red.equals(Reduction.FE)/*||red.equals(Reduction.ENFB)*/){
//			multisetCoefficients = computeMultisetCoefficients(crn, terminator, crn.getMaxArity());
//		}

		refine(red,crn,obtainedPartition,/*multisetCoefficients,*/speciesCounters,speciesCountersHM,terminator,out,bwOut);

		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"The final partition:");
			CRNReducerCommandLine.println(out,bwOut,obtainedPartition);
		}

		long end = System.currentTimeMillis();
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,red+" Partitioning completed. From "+ crn.getSpeciesSize() +" species to "+ obtainedPartition.size() + " blocks. Time necessary: "+(end-begin)+ " (ms)");
			CRNReducerCommandLine.println(out,bwOut,"");
		}
		return new IPartitionAndBoolean(obtainedPartition,true);
	}
	
	/*public static int FACTORIAL(int n) {
		if(n<0){
			CRNReducerCommandLine.printWarning("Factorial: n should be non-negative:"+n);
		}
		if(n==0||n==1){
			return 1;
		}
		else{
			return n * FACTORIAL(n-1);
		}
	}*/
	
	protected static void refine(Reduction red, ICRN crn, IPartition partition, /*HashMap<IComposite, BigDecimal> multisetCoefficients,*/ ISpeciesCounterHandler[] speciesCounters, HashMap<ISpecies, ISpeciesCounterHandler> speciesCountersHM, Terminator terminator, MessageConsoleStream out, BufferedWriter bwOut) {
		refine(red, crn, partition, /*HashMap<IComposite, BigDecimal> multisetCoefficients,*/ speciesCounters, speciesCountersHM,terminator,  out, bwOut,false);
	}
	
	protected static void refine(Reduction red, ICRN crn, IPartition partition, /*HashMap<IComposite, BigDecimal> multisetCoefficients,*/ ISpeciesCounterHandler[] speciesCounters, Terminator terminator, MessageConsoleStream out, BufferedWriter bwOut) {
		refine(red, crn, partition, /*HashMap<IComposite, BigDecimal> multisetCoefficients,*/ speciesCounters, null,terminator,  out, bwOut,false);
	}
	
	protected static void refine(Reduction red, ICRN crn, IPartition partition, /*HashMap<IComposite, BigDecimal> multisetCoefficients,*/ ISpeciesCounterHandler[] speciesCounters, HashMap<ISpecies, ISpeciesCounterHandler> speciesCountersHM, Terminator terminator, MessageConsoleStream out, BufferedWriter bwOut, boolean extraTab) {
		refine(red, crn, partition, speciesCounters, speciesCountersHM, terminator, out, bwOut, extraTab,true,null,null);
	}
	
	
	
	
	protected static void refine(Reduction red, ICRN crn, IPartition partition, /*HashMap<IComposite, BigDecimal> multisetCoefficients,*/ ISpeciesCounterHandler[] speciesCounters, HashMap<ISpecies, ISpeciesCounterHandler> speciesCountersHM, Terminator terminator, MessageConsoleStream out, BufferedWriter bwOut, 
			boolean extraTab, boolean print, BigDecimal deltaHalf,HashMap<ICRNReaction, BigDecimal> reactionToRateToConsider) {
		refine(red, crn, partition, speciesCounters, speciesCountersHM, terminator, out, bwOut, 
				extraTab, print, deltaHalf,reactionToRateToConsider,null,null);
	}
	
	/**
	 * 
	 * @param red 
	 * @param crn
	 * @param partition the partition to be refined. Note that the partition is modified, thus first invoke copy if you want to preserve it.
	 * @param labels
	 * @param multisetCoefficients 
	 * @param speciesCounters 
	 * @param terminator 
	 * @param bwOut 
	 * @param out 
	 */
	protected static void refine(Reduction red, ICRN crn, IPartition partition, /*HashMap<IComposite, BigDecimal> multisetCoefficients,*/ ISpeciesCounterHandler[] speciesCounters, HashMap<ISpecies, ISpeciesCounterHandler> speciesCountersHM, Terminator terminator, MessageConsoleStream out, BufferedWriter bwOut, 
			boolean extraTab, boolean print, BigDecimal deltaHalf,HashMap<ICRNReaction, BigDecimal> reactionToRateToConsider,
			ArrayListOfReactions[] reactionsToConsiderForEachSpecies, HashMap<IComposite, BigDecimal> multisetCoefficients
			) {
		String pref = "";
		if(extraTab) {
			pref="\t\t";
		}
		if(!(red.equals(Reduction.FE)||red.equals(Reduction.BE))) {
			CRNReducerCommandLine.printWarning(out,bwOut,"The method CRNBisimulationNAry.refine should be invoked with either Reduction.FE or Reduction.BE as first parameter.");
		}
		if(speciesCounters!=null && speciesCountersHM!=null) {
			CRNReducerCommandLine.printWarning(out,bwOut,"Only one out of speciesCounters and speciesCountersHM should be NOT NULL.");
		}
		//generate candidate splitters
		List<ILabel> fakeLabels=new ArrayList<>();
		fakeLabels.add(EmptySetLabel.EMPTYSETLABEL);
		//SplittersGenerator splittersGenerator = partition.getOrCreateSplitterGenerator(fakeLabels);
		SplittersGenerator splittersGenerator = partition.createSplitterGenerator(fakeLabels);
		//SplittersGenerator splittersGenerator = partition.getSplitterGeneratorAndCreateItIfFirstInvocation(labels);
		
		//Initialize the "pr" fields to 0 of all species
		//crn.initializeAllCountersAndBST();
		//CRNBisimulationsNAry.initializeAllCounters(crn);
		partition.initializeAllBST();
		
//		HashMap<IComposite, BigDecimal> multisetCoefficients = null;
//		if(red.equals(Reduction.FE)/*||red.equals(Reduction.ENFB)*/){
//			multisetCoefficients = computeMultisetCoefficients(crn, terminator, crn.getMaxArity());
//		}
		boolean computeReactions=reactionsToConsiderForEachSpecies==null;
		if(print && computeReactions) {
			CRNReducerCommandLine.print(out,bwOut,"\n"+pref+"\tScanning the reactions once to pre-compute informations necessary to the partitioning ... ");
		}
		long begin = System.currentTimeMillis();
		//ArrayListOfReactions[] reactionsToConsiderForEachSpecies =  new ArrayListOfReactions[crn.getSpeciesSize()];
		
		
		HashMap<ICRNReaction,Integer> consideredAtIteration=null;
//		if(red.equals(Reduction.FE)) {
//			addToReactionsWithNonZeroStoichiometry(crn, reactionsToConsiderForEachSpecies);
//		}
		//HashMap<IComposite, BigDecimal> multisetCoefficients = null;
		if(reactionsToConsiderForEachSpecies==null) {
			reactionsToConsiderForEachSpecies =  new ArrayListOfReactions[crn.getSpeciesSize()];
			if(red.equals(Reduction.FE)) {
				multisetCoefficients = new HashMap<IComposite, BigDecimal>();
				addToReactionsWithNonZeroStoichiometryAndComputeMultisetCoefficients(crn.getReactions(), terminator, reactionsToConsiderForEachSpecies, multisetCoefficients, crn.getMaxArity());
			}
			else if(red.equals(Reduction.BE)) {
				CRNBisimulations.addToOutgoingReactionsOfReagents(crn.getReactions(), reactionsToConsiderForEachSpecies);
				consideredAtIteration = new HashMap<>(crn.getReactions().size());
			}
		}
		if(red.equals(Reduction.BE)) {
			consideredAtIteration = new HashMap<>(crn.getReactions().size());
		}
		long end = System.currentTimeMillis();
		if(print  && computeReactions) {
			CRNReducerCommandLine.print(out,bwOut,"completed in "+ String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0))+" (s)");
		}
		
		
		/*
		ISpecies species1=null;
		ISpecies species2=null;
		//Smallest model
		//String n1="s4Y0_2Z0_2Y2_4Z2";
		//String n2="s4x0_2x2";
		//Largest model
		//String n1="s22Y0_16Z0_16Y2_22Z2";
		//String n2="s22x0_16x2";
		//MI1616_1010
		String n1="s16Y0_10Z0_10Y2_16Z2";
		String n2="s16x0_10x2";
		//MI11_11 AM2_2
		//String n1="sY0_Z0_Y2_Z2";
		//String n2="s2x0_2x2";
		for(ISpecies species : crn.getSpecies()) {
			if(species.getName().equals(n1)) {
				species1=species;
			}
			else if(species.getName().equals(n2)) {
				species2=species;
			} 
			if(species1!=null && species2!=null) {
				break;
			}
		}
		*/
		
		//long beginPT = System.currentTimeMillis();
		//boolean split=false;
		
		
		Integer iteration=1;
		if(print) {
			CRNReducerCommandLine.print(out,bwOut,"\n"+pref+"\tPerforming the actual "+red+" partition refinement ... ");
		}
		//CRNReducerCommandLine.println(out,bwOut,);
		while(splittersGenerator.hasSplittersToConsider()){
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			//System.out.println(" "+iteration);
			split(red,crn,partition,splittersGenerator,multisetCoefficients,iteration,consideredAtIteration,speciesCounters,speciesCountersHM,reactionsToConsiderForEachSpecies,deltaHalf,reactionToRateToConsider);
			if(speciesCountersHM!=null) {
				speciesCountersHM=new HashMap<>();
			}
			iteration++;
			//System.out.println(" "+iteration);
			splittersGenerator.skipSplittersWhichShouldBeIgnored();
			/*
			if(!split) {
				IBlock block1=partition.getBlockOf(species1);
				IBlock block2=partition.getBlockOf(species2);
				if(!block1.equals(block2)) {
					String time = String.format( CRNReducerCommandLine.MSFORMAT, ((System.currentTimeMillis()-beginPT)/1000.0))+" (s)";
					CRNReducerCommandLine.print(out,bwOut,"\n"+pref+"\tSpecies "+n1+ " and " +n2+" split at iteration "+iteration +" in "+time +" current partition size "+partition.size());
					split=true;
				}
			}
			*/
		}
		if(print) {
			CRNReducerCommandLine.print(out,bwOut,"\n"+pref+"\t"+iteration+" iterations performed");
		}

	}
	
	/*
	private static HashMap<IComposite, BigDecimal> computeMultisetCoefficients(ICRN crn, Terminator terminator,
			int maxArity) {
		HashMap<IComposite, BigDecimal> multisetCoefficients;
		int[] factorials = new int[maxArity+1];
		factorials[0]=1;
		for(int arity=1;arity<factorials.length;arity++){
			factorials[arity]=arity*factorials[arity-1];
		}
		
		multisetCoefficients = new HashMap<IComposite, BigDecimal>();
		for(ICRNReaction reaction : crn.getReactions()){
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			if((!reaction.isUnary()) && !multisetCoefficients.containsKey(reaction.getReagents())){
				//int[] multiplicities = reaction.getReagents().getMultiplicities();
				int multisetCoefficient =1;
				for(int i=0;i<reaction.getReagents().getNumberOfDifferentSpecies();i++){
					multisetCoefficient=multisetCoefficient*factorials[reaction.getReagents().getMultiplicities(i)];
				}
				multisetCoefficients.put(reaction.getReagents(), BigDecimal.valueOf(multisetCoefficient));
			}
		}
		return multisetCoefficients;
	}
	private static void addToReactionsWithNonZeroStoichiometry(ICRN crn,
			ArrayListOfReactions[] reactionsToConsiderForEachSpecies) {
		for(ICRNReaction reaction : crn.getReactions()) {
			HashMap<ISpecies, Integer> productsMinusReagents = reaction.computeProductsMinusReagentsHashMap();
			for (Entry<ISpecies, Integer> entry : productsMinusReagents.entrySet()) {
				//entry.getKey().addReactionsWithNonZeroStoichiometry(reaction);
				ISpecies species = entry.getKey();
				CRNBisimulations.addReactionToConsider(reactionsToConsiderForEachSpecies, reaction, species);
			}
			
		}
		
	}
	*/
	protected static void addToReactionsWithNonZeroStoichiometryAndComputeMultisetCoefficients(Collection<ICRNReaction> reactions, Terminator terminator,
			ArrayListOfReactions[] reactionsToConsiderForEachSpecies,HashMap<IComposite, BigDecimal> multisetCoefficients, int maxArity) {
		
		//For multisetCoefficients
		int[] factorials = computeFactorials(maxArity);
		
		for(ICRNReaction reaction : reactions) {
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			//For multisetCoefficients
			extractMultisetCoefficients(multisetCoefficients, factorials, reaction);
			
			//To add ReactionsWithNonZeroStoichiometry
			HashMap<ISpecies, Integer> productsMinusReagents = reaction.computeProductsMinusReagentsHashMap();
			for (Entry<ISpecies, Integer> entry : productsMinusReagents.entrySet()) {
				//entry.getKey().addReactionsWithNonZeroStoichiometry(reaction);
				ISpecies species = entry.getKey();
				CRNBisimulations.addReactionToConsider(reactionsToConsiderForEachSpecies, reaction, species);
			}
		}
		
	}
	static int[] computeFactorials(int maxArity) {
		int[] factorials = new int[maxArity+1];
		factorials[0]=1;
		for(int arity=1;arity<factorials.length;arity++){
			factorials[arity]=arity*factorials[arity-1];
		}
		return factorials;
	}
	protected static void extractMultisetCoefficients(HashMap<IComposite, BigDecimal> multisetCoefficients,
			int[] factorials, ICRNReaction reaction) {
		if(reaction.isUnary()) {
			//I should put BigDecimal.ONE but I don't do it to save memory
		}
		else {
			if(!multisetCoefficients.containsKey(reaction.getReagents())){
				int multisetCoefficient =1;
				for(int i=0;i<reaction.getReagents().getNumberOfDifferentSpecies();i++){
					multisetCoefficient=multisetCoefficient*factorials[reaction.getReagents().getMultiplicities(i)];
				}
				multisetCoefficients.put(reaction.getReagents(), BigDecimal.valueOf(multisetCoefficient));
			}
		}
	}
	public static void initializeAllCounters(Collection<ISpecies> consideredSpecies, ISpeciesCounterHandler[] speciesCounters) {
		for (ISpecies species : consideredSpecies) {
			if(speciesCounters[species.getID()]!=null){
				speciesCounters[species.getID()].initializeAllCounters();
			}
		}
	}

	/**
	 * 
	 * @param red 
	 * @param crn
	 * @param partition the partition to be refined. Note that the partition is modified, thus first invoke copy if you want to preserve it.
	 * @param splittersGenerator
	 * @param labels 
	 * @param multisetCoefficients 
	 * @param consideredAtIteration 
	 * @param iteration 
	 * @param speciesCounters 
	 * @param reactionsToConsiderForEachSpecies 
	 * @param reactionToRateToConsider 
	 */
	private static void split(Reduction red, ICRN crn, IPartition partition, SplittersGenerator splittersGenerator, HashMap<IComposite, BigDecimal> multisetCoefficients, Integer iteration, HashMap<ICRNReaction,Integer> consideredAtIteration, ISpeciesCounterHandler[] speciesCounters, HashMap<ISpecies, ISpeciesCounterHandler> speciesCountersHM,ArrayListOfReactions[] reactionsToConsiderForEachSpecies, BigDecimal deltaHalf, HashMap<ICRNReaction, BigDecimal> reactionToRateToConsider) {
		
		IBlock blockSPL = splittersGenerator.getBlockSpl();
		//ILabel labelSPL = splittersGenerator.getLabelSpl();
		
		//Initialize the "pr" fields to 0 of all species having reactions with partners label, towards at least a species of blockSPL// This is now done once before invoking splitDSB, and then at the end of splitDSB, for the splittergenerators only
		//initializeAllLabelPRandBST(crn,labelSPL, blockSPL);

		//Set of species with at least a reaction towards the species of the splitter
		LinkedHashSet<ISpecies> splitterGenerators = new LinkedHashSet<ISpecies>();
		
		boolean anyReactionFound=false;

		//If FB compute pr[X,partnerLabel,blockSPL] for all species X having at least a reaction with at least a partner partnerLabel (for all partnerLabels) towards blockSPL (and build the list of such species. Only their blocks can get split)
		//If BB compute fr[X,blockLabel,blockSPL] for all species X and all blocks blockLabel  (and build the list of species having it positive. Only their blocks can get split)
		anyReactionFound = computeMeasuresUsedToSplitWithRespectToTheSplitter(red,crn,  blockSPL,splitterGenerators,partition,multisetCoefficients,iteration,consideredAtIteration,speciesCounters,speciesCountersHM,reactionsToConsiderForEachSpecies,deltaHalf,reactionToRateToConsider);
		
		if(!anyReactionFound){
			//CRNReducerCommandLine.println(out,bwOut,"No reactions considered at this iteration. I can skip this split iteration");
			splittersGenerator.generateNextSplitter();
			return;
		}
		
		/*int i=0;
		for (IBlock block : labelBlocks) {
			block.setLabelId(i);
			i++;
		}*/
		

		boolean hasOnlyUnaryReactions = crn.getMaxArity()==1; 
		
		//Set of blocks to be considered for splitting (blocks where at least a species performs a reaction with partners label towards species of blockSPL)
		LinkedHashSet<IBlock> splittedBlocks = new LinkedHashSet<IBlock>();
		SpeciesCounterField splitWRT = (red.equals(Reduction.FE)/*||red.equals(Reduction.ENFB)*/)? SpeciesCounterField.NRVECTOR : SpeciesCounterField.FRVECTOR;
		if(hasOnlyUnaryReactions){
			splitWRT = (red.equals(Reduction.FE)/*||red.equals(Reduction.ENFB)*/)? SpeciesCounterField.NR : SpeciesCounterField.FR;
		}
		boolean blockOfSPlitterHasBeenSplitted;
		
		blockOfSPlitterHasBeenSplitted = partitionBlocksOfGivenSpecies(partition,splitterGenerators,blockSPL,splittedBlocks,splitWRT,speciesCounters,speciesCountersHM);
		/*if(red.equals(Reduction.NFB)||red.equals(Reduction.NBB)){
			blockOfSPlitterHasBeenSplitted = partitionBlocksOfGivenSpecies(partition,splitterGenerators,blockSPL,splittedBlocks,splitWRT,speciesCounters);
		}
		else{
			//blockOfSPlitterHasBeenSplitted = partitionBlocksOfGivenSpeciesEpsilon(partition,splitterGenerators,blockSPL,splittedBlocks,splitWRT,speciesCounters);
			blockOfSPlitterHasBeenSplitted = partitionBlocksOfGivenSpeciesEpsilon_SumOfCoefficientDifferencesIsAtMostEpsilon(partition,splitterGenerators,blockSPL,splittedBlocks,splitWRT,speciesCounters);
		}*/

		//blockSPL.setHasBeenAlreadyUsedAsSplitter(true);
		
		//Now I have to update the splitters according to the newly generated partition
		cleanPartitioAndGetNextSplitterIfNecessary(partition, splittersGenerator, blockSPL, splittedBlocks, blockOfSPlitterHasBeenSplitted,hasOnlyUnaryReactions);
		
		
		//Initialize the "pr" fields to 0 of all species having reactions with partners label, towards at least a species of blockSPL
		//crn.initializeAllCountersAndBST(splitterGenerators);
		//System.out.println(splitterGenerators.size()+" splitterGenerators out of "+speciesCounters.length);
		if(speciesCounters!=null) {
			CRNBisimulationsNAry.initializeAllCounters(splitterGenerators,speciesCounters);
		}
		else {
			//....I do it in split
			//speciesCountersHM.
		}
		
	}
	
	protected static boolean partitionBlocksOfGivenSpecies(IPartition partition, HashSet<ISpecies> consideredSpecies,IBlock blockToCheckIfSplit, HashSet<IBlock> splittedBlocks, SpeciesCounterField keyField, ISpeciesCounterHandler[] speciesCounters, HashMap<ISpecies, ISpeciesCounterHandler> speciesCountersHM) {
		
		//If "blockToCheckIfSplit" is ANYBLOCK, I assume that it means that I have to return true if at least a block has been split. 
		boolean blocktoCheckHasBeenSplit=false;

		//split each block of partition according to the computed pr[X,labelSPL,blockSPL] values. 
		for (ISpecies splitterGenerator : consideredSpecies) {
			//If no measure has been computed for the species....
			ISpeciesCounterHandler counters =getOrAddSpeciesCounterHandler(speciesCounters, speciesCountersHM, splitterGenerator,false,CRNBisNAryOrSENAry.FEBE);
			if(counters==null) {
				continue;
			}
//			if(speciesCounters[splitterGenerator.getID()]==null){
//				continue;
//			}
//			ISpeciesCounterHandler counters = speciesCounters[splitterGenerator.getID()];
			//If the computed measure is zero....
			if(keyField.equals(SpeciesCounterField.FRVECTOR)&&(counters.getFRVector()==null || counters.getFRVector().size()==0)){
				continue;
			}
			else if(keyField.equals(SpeciesCounterField.NRVECTOR)&&(counters.getNRVector()==null||counters.getNRVector().size()==0)){
				continue;
			}
			else if(keyField.equals(SpeciesCounterField.FR)||keyField.equals(SpeciesCounterField.NR)){
				if(BigDecimal.ZERO.compareTo(counters.get(keyField))==0){
					continue;
				}
			}
			else if(keyField.equals(SpeciesCounterField.SMBVECTOR)){
				if(counters.getSMBCounterVector()==null||counters.getSMBCounterVector().size()==0){
					continue;
				}
			}
			
			//TODO: maybe we can modify so that splitting is done only if the block is not a singleton
			IBlock block = partition.getBlockOf(splitterGenerator);
			//remove the species from its block (i.e., this block will remain with only species not performing reactions towards the splitter)
			block.removeSpecies(splitterGenerator);
			//Add block to the set of splitted blocks
			splittedBlocks.add(block);
			if(blockToCheckIfSplit != null && block.equals(blockToCheckIfSplit)){
				blocktoCheckHasBeenSplit=true;
			}
			/*
			 * Insert the species splitterGenerator in the tree associated to the block "block". 
			 * This method is used to split the block according to the computed generation rates. This may cause the creation of a new block, in which case it is automatically added to the current partition. 
			 * In any case, the reference of the species to the own block is updated   
			 */

			if(keyField.equals(SpeciesCounterField.FRVECTOR)){
				block.getBSTForVectors().put(new VectorOfBigDecimalsForEachLabel(counters.getFRVector()), splitterGenerator, partition);
			}
			else if(keyField.equals(SpeciesCounterField.NRVECTOR)){
				block.getBSTForVectors().put(new VectorOfBigDecimalsForEachLabel(counters.getNRVector()), splitterGenerator, partition);
			}
			else if(keyField.equals(SpeciesCounterField.SMBVECTOR)){
				block.getBSTForVectors().put(new VectorOfBigDecimalsForEachLabel(counters.getSMBCounterVector()), splitterGenerator, partition);
			}
			else{
				if(CRNBisimulationsNAry.USETOLERANCE){
					block.getBST(CRNBisimulationsNAry.TOLERANCE).put(counters.get(keyField), splitterGenerator, partition);
				}
				else{
					block.getBST().put(counters.get(keyField), splitterGenerator, partition);
				}
			}
		}
		return blocktoCheckHasBeenSplit;
	}
 
/*	
   private static boolean partitionBlocksOfGivenSpeciesEpsilon_SumOfCoefficientDifferencesIsAtMostEpsilon(IPartition partition, HashSet<ISpecies> consideredSpecies,IBlock blockToCheckIfSplit, HashSet<IBlock> splittedBlocks, SpeciesCounterField keyField, ISpeciesCounterHandler[] speciesCounters) {
		
		//I have to pay attention to which vector (frvector or prvector) or bigdecimal (FR or NR. In this case we also have to check the tolerance).
		//At the moment I use by default fr.
	
		//If "blockToCheckIfSplit" is ANYBLOCK, I assume that it means that I have to return true if at least a block has been split. ???
		boolean blocktoCheckHasBeenSplit=false;

		LinkedHashMap<ISpecies, VectorOfBigDecimalsForEachLabel> speciesToVectorOfBigDecimalForEachLabel = new LinkedHashMap<ISpecies, VectorOfBigDecimalsForEachLabel>(consideredSpecies.size());
		
		for (ISpecies splitterGenerator : consideredSpecies) {
			ISpeciesCounterHandler counters = speciesCounters[splitterGenerator.getID()];
			if(counters==null){
				continue;
			}
			
			if(keyField.equals(SpeciesCounterField.FRVECTOR) || keyField.equals(SpeciesCounterField.NRVECTOR)){
				HashMap<ILabel, BigDecimal> measures = null;
				if(keyField.equals(SpeciesCounterField.FRVECTOR)){
					measures=counters.getFRVector();
				}
				else{
					measures=counters.getNRVector();
				}
				if(measures==null || measures.size()==0){
					continue;
				}
				else{
					VectorOfBigDecimalsForEachLabel v = new VectorOfBigDecimalsForEachLabel(measures);
					speciesToVectorOfBigDecimalForEachLabel.put(splitterGenerator, v);
				}
			}
			else{
				//TODO
				throw new UnsupportedOperationException("We currently support only vectors for the epsilon bisimulations");
			}
		}
		
		//We now compute the equivalence class of each species
		HashMap<ISpecies, ArrayList<ISpecies>> speciesToItsEquivalence = new HashMap<>(consideredSpecies.size());
		
		{
			int i=0;
			for (ISpecies splitterGenerator : consideredSpecies) {
				ArrayList<ISpecies> equivalenceOfSP = speciesToItsEquivalence.get(splitterGenerator);
				if(equivalenceOfSP==null){
					equivalenceOfSP=new ArrayList<ISpecies>();
					equivalenceOfSP.add(splitterGenerator);
					speciesToItsEquivalence.put(splitterGenerator, equivalenceOfSP);
				}
				VectorOfBigDecimalsForEachLabel splitterGeneratorMeasure = speciesToVectorOfBigDecimalForEachLabel.get(splitterGenerator);
				int j=0;
				for (Entry<ISpecies,VectorOfBigDecimalsForEachLabel> entry : speciesToVectorOfBigDecimalForEachLabel.entrySet()) {
					if(j>i){
						ISpecies species = entry.getKey();
						VectorOfBigDecimalsForEachLabel measure = entry.getValue();
						//TODO: change atEspilonDistance!
						if(splitterGeneratorMeasure.atEpsilonDistance(measure)){
							equivalenceOfSP.add(species);
							ArrayList<ISpecies> equivalence = speciesToItsEquivalence.get(species);
							if(equivalence==null){
								equivalence=new ArrayList<>();
								speciesToItsEquivalence.put(species,equivalence);
								equivalence.add(species);
							}
							equivalence.add(splitterGenerator);
						}
					}
					j++;
				}
				i++;
			}
		}
		
		//Now we compute the transitive closure of the equivalences
		ArrayList<IBlock> refinement = new ArrayList<IBlock>();
		HashMap<ISpecies, Boolean> alreadyConsidered = new HashMap<ISpecies, Boolean>(consideredSpecies.size());
		for (Entry<ISpecies, ArrayList<ISpecies>> entry : speciesToItsEquivalence.entrySet()) {
			ISpecies species = entry.getKey();
			Boolean cons = alreadyConsidered.get(species);
			if(cons==null){
				alreadyConsidered.put(species, true);
				IBlock reachable = new Block();
				partition.add(reachable);
				reachable.addSpecies(species);
				refinement.add(reachable);
				ArrayList<ISpecies> next = entry.getValue();
				if(next!=null){
					for (ISpecies nextSpecies : next) {
						addAllReachable(reachable,nextSpecies,speciesToItsEquivalence,alreadyConsidered);
					}
				}
			}
		}
		
		System.out.println("We have "+refinement.size()+" blocks");
		System.out.println(refinement.toString());
		
		return blocktoCheckHasBeenSplit;
		
	}
	

   private static void addAllReachable(IBlock reachable, ISpecies nextSpecies,HashMap<ISpecies, ArrayList<ISpecies>> speciesToItsEquivalence, HashMap<ISpecies, Boolean> alreadyConsidered) {
	   Boolean cons = alreadyConsidered.get(nextSpecies);
	   if(cons==null){
		   alreadyConsidered.put(nextSpecies, true);
		   reachable.addSpecies(nextSpecies);
		   ArrayList<ISpecies> next = speciesToItsEquivalence.get(nextSpecies);
		   if(next!=null){
			   for (ISpecies nextNextSpecies : next) {
				   addAllReachable(reachable,nextNextSpecies,speciesToItsEquivalence,alreadyConsidered);
			   }
		   }
	   }
   }
   
private static boolean partitionBlocksOfGivenSpeciesEpsilonCheckingThatEachCoefficientDiffersEpsilon(IPartition partition, HashSet<ISpecies> consideredSpecies,IBlock blockToCheckIfSplit, HashSet<IBlock> splittedBlocks, SpeciesCounterField keyField, ISpeciesCounterHandler[] speciesCounters) {
		
		//I have to pay attention to which vector (frvector or prvector) or bigdecimal (FR or NR. In this case we also have to check the tolerance).
		//At the moment I use by default fr.
	
		//If "blockToCheckIfSplit" is ANYBLOCK, I assume that it means that I have to return true if at least a block has been split. 
		boolean blocktoCheckHasBeenSplit=false;

		//Sort the vectors of bigdecimal (one vector per species, one entry per label)  
		ArrayList<SpeciesAndVectorOfBigDecimalsForEachLabel> sortedMeasures = new ArrayList<SpeciesAndVectorOfBigDecimalsForEachLabel>(consideredSpecies.size());
		for (ISpecies splitterGenerator : consideredSpecies) {
			ISpeciesCounterHandler counters = speciesCounters[splitterGenerator.getID()];
			if(counters==null){
				continue;
			}
			
			if(keyField.equals(SpeciesCounterField.FRVECTOR) || keyField.equals(SpeciesCounterField.NRVECTOR)){
				HashMap<ILabel, BigDecimal> measures = null;
				if(keyField.equals(SpeciesCounterField.FRVECTOR)){
					measures=counters.getFRVector();
				}
				else{
					measures=counters.getNRVector();
				}
				if(measures==null || measures.size()==0){
					continue;
				}
				sortedMeasures.add(new SpeciesAndVectorOfBigDecimalsForEachLabel(splitterGenerator,new VectorOfBigDecimalsForEachLabel(measures)));
			}
			else{
				//TODO
				throw new UnsupportedOperationException("We currently support only vectors for the epsilon bisimulations");
			}
		}
		
		Collections.sort(sortedMeasures);
		//split each block of partition according to the epsilon closure of the vector of bigdecimal for each label.
		//This is the "last" vector is sequence of epsilon-close vectors. It is used to check if the next measure is epsilon-close.  
		VectorOfBigDecimalsForEachLabel previousBigDecimalsForEachLabelToCheckEpsilonClosure = null;//sortedMeasures.get(0).getVectorOfBigDecimalsForEachLabel();
		//This is the "first" vector is sequence of epsilon-close vectors. It is the same value used to split all epsilon-close species. 
		VectorOfBigDecimalsForEachLabel previousBigDecimalsForEachLabelToSplitTheBlock = null;//sortedMeasures.get(0).getVectorOfBigDecimalsForEachLabel();
		boolean zeroMeasureBlock=true;
		VectorOfBigDecimalsForEachLabel zeroMeasure = new VectorOfBigDecimalsForEachLabel(new HashMap<ILabel,BigDecimal>(0));
		ArrayList<ISpecies> currentEpsilonEquivalentSpecies=new ArrayList<ISpecies>();
		for (SpeciesAndVectorOfBigDecimalsForEachLabel speciesAndVectorOfBigDecimalsForEachLabel : sortedMeasures) {
			ISpecies splitterGenerator = speciesAndVectorOfBigDecimalsForEachLabel.getSpecies();
			VectorOfBigDecimalsForEachLabel measure = speciesAndVectorOfBigDecimalsForEachLabel.getVectorOfBigDecimalsForEachLabel();
			
			//Check if this species has to go in the same block as the previous one
			if(previousBigDecimalsForEachLabelToCheckEpsilonClosure==null || !previousBigDecimalsForEachLabelToCheckEpsilonClosure.atEpsilonDistance(measure)){
				if(previousBigDecimalsForEachLabelToSplitTheBlock!=null && currentEpsilonEquivalentSpecies.size()>0 && !zeroMeasureBlock){
					//add all species to the BSTs
					for(ISpecies speciesToAddToBST : currentEpsilonEquivalentSpecies){
						//TODO: maybe we can modify so that splitting is done only if the block is not a singleton
						IBlock block = partition.getBlockOf(speciesToAddToBST);
						//remove the species from its block (i.e., this block will remain with only species not performing reactions towards the splitter)
						block.removeSpecies(speciesToAddToBST);
						//Add block to the set of splitted blocks
						splittedBlocks.add(block);
						if(blockToCheckIfSplit != null && block.equals(blockToCheckIfSplit)){
							blocktoCheckHasBeenSplit=true;
						}
						//
						//  Insert the species splitterGenerator in the tree associated to the block "block". 
						//  This method is used to split the block according to the computed generation rates. This may cause the creation of a new block, in which case it is automatically added to the current partition. 
						// In any case, the reference of the species to the own block is updated   
						//
						block.getBSTForVectors().put(previousBigDecimalsForEachLabelToSplitTheBlock, speciesToAddToBST, partition);
					}
				}
				currentEpsilonEquivalentSpecies=new ArrayList<ISpecies>();
				previousBigDecimalsForEachLabelToCheckEpsilonClosure=measure;
				previousBigDecimalsForEachLabelToSplitTheBlock=measure;
				//If I am epsilon-distant from 0, I don't actually remove from their blocks all species which are "epsilon-close" to me 
				zeroMeasureBlock=zeroMeasure.atEpsilonDistance(measure);
			}
			
			//If I am epsilon-distant from 0, I don't actually remove from their blocks all species which are "epsilon-close" to me 
			zeroMeasureBlock= zeroMeasureBlock || zeroMeasure.atEpsilonDistance(measure);
			
			//In case we are still considering the first block (with 0 measure, or epslison-close to zero), then we don't actually remove the species from its block. Note that in the previous if we checked that we are at epsilon-close distance from 0.
			if(zeroMeasureBlock ){
				previousBigDecimalsForEachLabelToCheckEpsilonClosure = measure;
				continue;
			}
			else{
				currentEpsilonEquivalentSpecies.add(splitterGenerator);
				
//				
//				//TODO: maybe we can modify so that splitting is done only if the block is not a singleton
//				IBlock block = partition.getBlockOf(splitterGenerator);
//				//remove the species from its block (i.e., this block will remain with only species not performing reactions towards the splitter)
//				block.removeSpecies(splitterGenerator);
//				//Add block to the set of splitted blocks
//				splittedBlocks.add(block);
//				if(blockToCheckIfSplit != null && block.equals(blockToCheckIfSplit)){
//					blocktoCheckHasBeenSplit=true;
//				}
//				
//				//
//				//  Insert the species splitterGenerator in the tree associated to the block "block". 
//				//  This method is used to split the block according to the computed generation rates. This may cause the creation of a new block, in which case it is automatically added to the current partition. 
//				// In any case, the reference of the species to the own block is updated   
//				//
//				block.getBSTForVectors().put(previousBigDecimalsForEachLabelToSplitTheBlock, splitterGenerator, partition);
				
				
				previousBigDecimalsForEachLabelToCheckEpsilonClosure = measure;
			}
		}
		return blocktoCheckHasBeenSplit;
		
	}
*/

	protected static void cleanPartitioAndGetNextSplitterIfNecessary(IPartition partition, SplittersGenerator splittersGenerator, IBlock blockSPL, HashSet<IBlock> splittedBlocks, boolean blockOfSplitterHasBeenSplitted,boolean hasOnlyUnaryReactions) {
		cleanPartitioAndGetNextSplitterIfNecessary(partition, splittersGenerator, blockSPL, splittedBlocks,  blockOfSplitterHasBeenSplitted, hasOnlyUnaryReactions,true);
	}

	protected static void cleanPartitioAndGetNextSplitterIfNecessary(IPartition partition, SplittersGenerator splittersGenerator, IBlock blockSPL, HashSet<IBlock> splittedBlocks, boolean blockOfSplitterHasBeenSplitted,boolean hasOnlyUnaryReactions, boolean updateTheSplitter) {
		
		boolean blockOfSplitHasBeenRemoved = false;
		//boolean blockOfSplitterBecameItsBiggestSubBlock = false;
		
		for (IBlock splittedBlock : splittedBlocks) {
			List<IBlock> subBlocks = null;
			if(hasOnlyUnaryReactions){
				if(CRNBisimulationsNAry.USETOLERANCE){
					subBlocks = splittedBlock.getBST(TOLERANCE).getBlocks();
				}
				else{
					subBlocks = splittedBlock.getBST().getBlocks();
				}
				
			}
			else{
				subBlocks = splittedBlock.getBSTForVectors().getBlocks();
			}
					
			//If I did not actually split the block, but I have just moved all its elements in a new one. Thus I replace the new alias block with the original one. This improves performances, because the new alias block will be considered as the old one, without having to recompute all checks already done
			if(splittedBlock.isEmpty()  && subBlocks.size() == 1){
				IBlock aliasBlock = subBlocks.get(0);
				partition.substituteAndDecreaseSize(aliasBlock,splittedBlock);
				//blockOfSPlitterHasBeenSplitted has not really been splitted
				if(blockSPL == splittedBlock){
					blockOfSplitterHasBeenSplitted = false;
					aliasBlock.setHasBeenAlreadyUsedAsSplitter(true); //Before it was done by the splittersGenerator.shiftToNextBlock(). But now we don't do it anymore before it was leading to wrond reductions
				}
			}
			//Otherwise, I have actually split the block. I remove it from the partition if it is empty.
			else{
				IBlock biggestSubBlock = null;
				if(hasOnlyUnaryReactions){
					if(CRNBisimulationsNAry.USETOLERANCE){
						biggestSubBlock = splittedBlock.getBST(CRNBisimulationsNAry.TOLERANCE).getBiggestSubBlock();
					}
					else{
						biggestSubBlock = splittedBlock.getBST().getBiggestSubBlock();
					}
					
				}
				else{
					biggestSubBlock = splittedBlock.getBSTForVectors().getBiggestSubBlock();
				}
				
				
				//If the original block became empty, remove it from the partition.
				if(splittedBlock.isEmpty()){
					partition.remove(splittedBlock);
					if(blockOfSplitterHasBeenSplitted && blockSPL == splittedBlock){
						blockOfSplitHasBeenRemoved=true;
					}
					if(splittedBlock.hasBeenAlreadyUsedAsSplitter() || blockSPL == splittedBlock){
						biggestSubBlock.setCanBeUsedAsSplitter(false);
					}
				}
				else{
					//Otherwise, the original block is not empty, and thus I do not remove it from the partition. However, I have to throw away its bst.
					splittedBlock.setCanBeUsedAsSplitter(true);
					splittedBlock.setHasBeenAlreadyUsedAsSplitter(false);
					if(splittedBlock.hasBeenAlreadyUsedAsSplitter()  || blockSPL == splittedBlock){
						if(splittedBlock.getSpecies().size()>biggestSubBlock.getSpecies().size()){
							biggestSubBlock = splittedBlock;
							/*if(blockSPL==splittedBlock){
								blockOfSplitterBecameItsBiggestSubBlock=true;
							}*/
						}
						biggestSubBlock.setCanBeUsedAsSplitter(false);
					}
					if(hasOnlyUnaryReactions){
						if(CRNBisimulationsNAry.USETOLERANCE){
							splittedBlock.throwAwayBSTWithTolerance();
						}
						else{
							splittedBlock.throwAwayBST();
						}
						
					}
					else{
						splittedBlock.throwAwayBSTForVectors();
					}
					
					//splittedBlock.setHasBeenAlreadyUsedAsSplitter(false);
				}
			}
		}
		//If the block of the splitter has been removed, splittersGenerator already created the new splitter. If instead I have not removed it, I have to consider the block as a new one, and thus I have to reinizialize the labels
		if(splittersGenerator!=null) {
			if(blockOfSplitterHasBeenSplitted){
				if(!blockOfSplitHasBeenRemoved){
					splittersGenerator.currentBlockHasBeenSplitButNotRemoved();
					/*if(blockOfSplitterBecameItsBiggestSubBlock && updateTheSplitter){
					splittersGenerator.generateNextSplitter();
				}*/
					splittersGenerator.skipSplittersWhichShouldBeIgnored();
				}
			}
			else{
				if(updateTheSplitter){
					//If the block of the splitter has not been split, I have to generate the next splitter on my own
					splittersGenerator.generateNextSplitter();
				}
			}
		}
		
	}

	private static boolean computeMeasuresUsedToSplitWithRespectToTheSplitter(
			Reduction red, ICRN crn, IBlock blockSPL, HashSet<ISpecies> splitterGenerators,IPartition partition, HashMap<IComposite, BigDecimal> multisetCoefficients, Integer iteration, HashMap<ICRNReaction, Integer> consideredAtIteration, ISpeciesCounterHandler[] speciesCounters,HashMap<ISpecies, ISpeciesCounterHandler> speciesCountersHM, ArrayListOfReactions[] reactionsToConsiderForEachSpecies, BigDecimal deltaHalf, HashMap<ICRNReaction, BigDecimal> reactionToRateToConsider) {
		
		boolean hasOneLabelOnly = crn.getMaxArity()==1;
		boolean anyReactionConsidered = false;
		//Collection<ICRNReaction> consideredReactions=null;
		for (ISpecies aSpeciesOfSplitter : blockSPL.getSpecies()) {
			ArrayListOfReactions reactions = reactionsToConsiderForEachSpecies[aSpeciesOfSplitter.getID()];
			if(reactions!=null) {
				if(red.equals(Reduction.FE)/*||red.equals(Reduction.ENFB)*/){
					//boolean anyReactionConsidered2 = computeNetProductionRateForGivenReactions(splitterGenerators, aSpeciesOfSplitter.getReactionsWithNonZeroStoichiometry(), aSpeciesOfSplitter,hasOneLabelOnly,multisetCoefficients,speciesCounters);
					boolean anyReactionConsidered2 = computeNetProductionRateForGivenReactions(splitterGenerators, reactions.reactions, aSpeciesOfSplitter,hasOneLabelOnly,multisetCoefficients,speciesCounters,speciesCountersHM,deltaHalf,reactionToRateToConsider);
					anyReactionConsidered = anyReactionConsidered2 || anyReactionConsidered;
				}
				else{//NBB,ENBB
					//boolean anyReactionConsidered2 = computeFluxRateForGivenReactions(splitterGenerators, aSpeciesOfSplitter.getOutgoingReactions(), aSpeciesOfSplitter,partition,hasOneLabelOnly,iteration,consideredAtIteration,speciesCounters);
					boolean anyReactionConsidered2 = computeFluxRateForGivenReactions(splitterGenerators, reactions.reactions, aSpeciesOfSplitter,partition,hasOneLabelOnly,iteration,consideredAtIteration,speciesCounters,speciesCountersHM);
					anyReactionConsidered = anyReactionConsidered2 || anyReactionConsidered;
				}
			}
		}
		return anyReactionConsidered;
	}
	
	private static void updateNROfReagents(ICRNReaction reaction, HashSet<ISpecies> splitterGenerators, ISpecies splitterSpecies,boolean onlyOneLabel, HashMap<IComposite, BigDecimal> multisetCoefficients, int scale, RoundingMode rm, ISpeciesCounterHandler[] speciesCounters, HashMap<ISpecies,ISpeciesCounterHandler> speciesCountersHM, BigDecimal deltaHalf,BigDecimal rateToConsider) {

		IComposite reagents = reaction.getReagents();
		IComposite products = reaction.getProducts();
		
		BigDecimal netStocOfsplitterSpecies = BigDecimal.valueOf(products.getMultiplicityOfSpecies(splitterSpecies)-reagents.getMultiplicityOfSpecies(splitterSpecies));
		BigDecimal rate = rateToConsider;
		if(deltaHalf!=null) {
			rate = rate.add(deltaHalf);
		}
		BigDecimal val = rate.multiply(netStocOfsplitterSpecies);
		
		if(!reagents.isUnary()){
			//val = val.divide(multisetCoefficients.get(getReagents()), scale, rm);
			val = val.multiply(multisetCoefficients.get(reagents));
		}

		if(reagents.isUnary()){	
			ISpecies reagent = reagents.getFirstReagent();
			ISpeciesCounterHandler currentSpeciesCounter =getOrAddSpeciesCounterHandler(speciesCounters, speciesCountersHM, reagent,CRNBisNAryOrSENAry.FEBE);
			
//			if(speciesCounters[reagent.getID()]==null) {
//				speciesCounters[reagent.getID()]= new SpeciesCounterHandlerCRNBIsimulationNAry();
//			}
			if(onlyOneLabel){
				//speciesCounters[reagent.getID()].addToNRWithScale(val,scale,rm);
				currentSpeciesCounter.addToNRWithScale(val,scale,rm);
			}
			else{
				//speciesCounters[reagent.getID()].addToNRWithScale(EmptySetLabel.EMPTYSETLABEL,val,scale,rm);
				currentSpeciesCounter.addToNRWithScale(EmptySetLabel.EMPTYSETLABEL,val,scale,rm);
			}
			splitterGenerators.add(reagent);
		}
		//Otherwise it is an nary reaction (and hence I cannot have just unary labels)
		else{
			NAryLabelBuilder nAryLabel = new NAryLabelBuilder((Composite)reagents);
			for(int i=0;i<reagents.getNumberOfDifferentSpecies();i++){
				ISpecies reagent = reagents.getAllSpecies(i);
				ISpeciesCounterHandler currentSpeciesCounter =getOrAddSpeciesCounterHandler(speciesCounters, speciesCountersHM, reagent,CRNBisNAryOrSENAry.FEBE);
//				if(speciesCounters[reagent.getID()]==null) {
//					speciesCounters[reagent.getID()]= new SpeciesCounterHandlerCRNBIsimulationNAry();
//				}
				nAryLabel.setSpeciesToDecrease(i);
				currentSpeciesCounter.addToNRWithScale(nAryLabel.getObtainedLabel(),val,scale,rm);
				nAryLabel.resetSpeciesToDecrease();
				splitterGenerators.add(reagent);
			}
		}
	}
	
	protected static ISpeciesCounterHandler getOrAddSpeciesCounterHandler(ISpeciesCounterHandler[] speciesCounters,
			HashMap<ISpecies, ISpeciesCounterHandler> speciesCountersHM, ISpecies reagent,CRNBisNAryOrSENAry whichCounterHandler) {
		return getOrAddSpeciesCounterHandler(speciesCounters, speciesCountersHM, reagent,true,whichCounterHandler);
	}
	
	protected static ISpeciesCounterHandler getOrAddSpeciesCounterHandler(ISpeciesCounterHandler[] speciesCounters,
			HashMap<ISpecies, ISpeciesCounterHandler> speciesCountersHM, ISpecies reagent, boolean add,CRNBisNAryOrSENAry whichCounterHandler) {
		ISpeciesCounterHandler currentSpeciesCounter;
		if(speciesCounters!=null) {
			if(speciesCounters[reagent.getID()]==null && add) {
				if(whichCounterHandler.equals(CRNBisNAryOrSENAry.FEBE)) {
					speciesCounters[reagent.getID()]= new SpeciesCounterHandlerCRNBIsimulationNAry();
				}
				else if(whichCounterHandler.equals(CRNBisNAryOrSENAry.SE)) {
					speciesCounters[reagent.getID()]= new SpeciesCounterHandlerSENAry();
				} 
				else {
					throw new UnsupportedOperationException(whichCounterHandler+" is not supported in getOrAddSpeciesCounterHandler");
				}
			}
			currentSpeciesCounter=speciesCounters[reagent.getID()];
		}
		else {
			currentSpeciesCounter=speciesCountersHM.get(reagent);
			if(currentSpeciesCounter==null && add) {
				//currentSpeciesCounter= new SpeciesCounterHandlerCRNBIsimulationNAry();
				if(whichCounterHandler.equals(CRNBisNAryOrSENAry.FEBE)) {
					currentSpeciesCounter= new SpeciesCounterHandlerCRNBIsimulationNAry();
				}
				else if(whichCounterHandler.equals(CRNBisNAryOrSENAry.SE)) {
					currentSpeciesCounter= new SpeciesCounterHandlerSENAry();
				} 
				else {
					throw new UnsupportedOperationException(whichCounterHandler+" is not supported in getOrAddSpeciesCounterHandler");
				}
				
				speciesCountersHM.put(reagent, currentSpeciesCounter);
			}
		}
		return currentSpeciesCounter;
	}
	

	private static boolean computeNetProductionRateForGivenReactions(HashSet<ISpecies> splitterGenerators,Collection<ICRNReaction> consideredReactions,ISpecies aSpeciesOfSplitter, boolean hasOneLabelOnly, HashMap<IComposite, BigDecimal> multisetCoefficients, ISpeciesCounterHandler[] speciesCounters, HashMap<ISpecies, ISpeciesCounterHandler> speciesCountersHM, BigDecimal deltaHalf, HashMap<ICRNReaction, BigDecimal> reactionToRateToConsider) {
		boolean anyReactionConsidered=false;
		BigDecimal minusDeltaHalf=null;
		if(deltaHalf!=null) {
			minusDeltaHalf=BigDecimal.ZERO.subtract(deltaHalf);
		}
		if(consideredReactions!=null){
			anyReactionConsidered=true;
			for (ICRNReaction reactionsWithNonZeroStoichiometry : consideredReactions) {
				if((deltaHalf==null && reactionsWithNonZeroStoichiometry.getRate().compareTo(BigDecimal.ZERO)!=0) ||
				   (deltaHalf!=null && reactionsWithNonZeroStoichiometry.getRate().compareTo(minusDeltaHalf)!=0)){
					if(reactionToRateToConsider!=null) {
						BigDecimal rateToConsider = reactionToRateToConsider.get(reactionsWithNonZeroStoichiometry);
						if(rateToConsider!=null && rateToConsider.compareTo(BigDecimal.ZERO)!=0) {
							updateNROfReagents(reactionsWithNonZeroStoichiometry,splitterGenerators,aSpeciesOfSplitter,hasOneLabelOnly,multisetCoefficients,SCALE,RM,speciesCounters,speciesCountersHM,deltaHalf,rateToConsider);
						}
					}
					else {
						updateNROfReagents(reactionsWithNonZeroStoichiometry,splitterGenerators,aSpeciesOfSplitter,hasOneLabelOnly,multisetCoefficients,SCALE,RM,speciesCounters,speciesCountersHM,deltaHalf,reactionsWithNonZeroStoichiometry.getRate());
					}
					
				}
			}
		}
		return anyReactionConsidered;
	}
	
	private static boolean computeFluxRateForGivenReactions(HashSet<ISpecies> splitterGenerators,Collection<ICRNReaction> consideredReactions,ISpecies aSpeciesOfSplitter,IPartition partition, boolean hasOneLabelOnly, Integer iteration, HashMap<ICRNReaction,Integer> consideredAtIteration, ISpeciesCounterHandler[] speciesCounters, HashMap<ISpecies, ISpeciesCounterHandler> speciesCountersHM) {
		boolean anyReactionConsidered=false;
		if(consideredReactions!=null){
			for (ICRNReaction outgoingReaction : consideredReactions) {
				//This if is to account for the alpha/alpha' of the definition of splitterBB
				if(!iteration.equals(consideredAtIteration.get(outgoingReaction))){
					consideredAtIteration.put(outgoingReaction, iteration);
					anyReactionConsidered=true;
					//CRNReducerCommandLine.println(out,bwOut,"source: "+aSpeciesOfSplitter+", reaction: "+outgoingReaction);
					if(outgoingReaction.getRate().compareTo(BigDecimal.ZERO)!=0){
						CRNBisimulations.updateFR(outgoingReaction,splitterGenerators, partition, aSpeciesOfSplitter,hasOneLabelOnly,speciesCounters,speciesCountersHM);
					}
				}
			}
		}
		return anyReactionConsidered;
	}
	
	private static boolean allReagentsAreBlockRepresentatives(IComposite composite, IPartition partition) {
		if(composite.isUnary()){
			return partition.speciesIsRepresentativeOfItsBlock(composite.getFirstReagent());
		}
		else{
			for(int i=0;i<composite.getNumberOfDifferentSpecies();i++){
				ISpecies species = composite.getAllSpecies(i);
				if(!partition.speciesIsRepresentativeOfItsBlock(species)){
					return false;
				}
			}
			return true;
		}
	}


	public static CRNandPartition computeReducedCRNOrdinary(ICRN crn,String name, IPartition partition, List<String> symbolicParameters, List<IConstraint> constraints,List<String> parameters,MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator) throws IOException {
		return computeReducedCRNOrdinary(crn,name,partition,symbolicParameters,constraints,parameters,"#",out,bwOut,terminator);
	}
	/**
	 * This method reduces the model, assuming the partition regards OFL or ordinary CTMC lumpability
	 * @param name
	 * @param partition
	 * @param parameters 
	 * @return
	 * @throws IOException 
	 */
	public static CRNandPartition computeReducedCRNOrdinary(ICRN crn,String name, IPartition partition, List<String> symbolicParameters, List<IConstraint> constraints,List<String> parameters,String commSymbol,MessageConsoleStream out, BufferedWriter bwOut,Terminator terminator) throws IOException {
		ICRN reducedCRN = new CRN(name,symbolicParameters,constraints,parameters,crn.getMath(),out,bwOut);
//		ISpecies[] speciesIdToSpecies= new ISpecies[crn.getSpeciesSize()];
//		crn.getSpecies().toArray(speciesIdToSpecies);
//		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpeciesSize());
//		for (ISpecies species : crn.getSpecies()) {
//			if(Terminator.hasToTerminate(terminator)){
//				break;
//			}
//			speciesNameToSpecies.put(species.getName(), species);
//		}
		
		//Default block for all reduced species
		//IBlock uniqueBlock = new Block();
		//IPartition trivialPartition = new Partition(uniqueBlock,getSpeciesSize());//The reduced CRNcan have at most |getSpecies()| species.
		
		IBlock currentBlock = partition.getFirstBlock();
		//A species per block
		int i=0;
		HashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies = new HashMap<IBlock, ISpecies>(partition.size());
		while(currentBlock!=null){
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			ISpecies blockRepresentative = currentBlock.getRepresentative(CRNReducerCommandLine.COMPUTEREPRESENTATIVEBYMINOUTSIDEPARTITIONREFINEMENT);
			String nameRep=blockRepresentative.getName();
			BigDecimal ic = currentBlock.getBlockConcentration();
			
			ISpecies reducedSpecies;
			/*boolean isZeroSpecies = false;
			if(crn.containsTheZeroSpecies()){
				isZeroSpecies=blockRepresentative.equals(crn.getCreatingIfNecessaryTheZeroSpecies());
			}*/
			if(crn.isZeroSpecies(blockRepresentative)){
			//if(crn.containsTheZeroSpecies() && nameRep.equals(Species.ZEROSPECIESNAME)){
				reducedSpecies = reducedCRN.getCreatingIfNecessaryTheZeroSpecies(nameRep);
				reducedSpecies.setInitialConcentration(ic, currentBlock.computeBlockConcentrationExpr());
			}
			else{
				reducedSpecies = new Species(nameRep, blockRepresentative.getOriginalName(), i, ic,currentBlock.computeBlockConcentrationExpr(),blockRepresentative.isAlgebraic());
				reducedCRN.addSpecies(reducedSpecies);
			}			
			//ISpecies reducedSpecies = new Species(blockRepresentative.getName(), i, currentBlock.getBlockConcentration(),currentBlock.getBlockConcentrationExpr());
			//reducedSpecies.addComment("This species is the reduction of these original species: "+currentBlock.toStringSeparatedSpeciesNames(", "));
			reducedSpecies.addCommentLines(currentBlock.computeBlockComment());
			//reducedSpecies.setRepresentedEquivalenceClass(currentBlock.getSpecies());
			//reducedCRN.addSpecies(reducedSpecies);
			correspondenceBlock_ReducedSpecies.put(currentBlock, reducedSpecies);
			currentBlock=currentBlock.getNext();
			i++;
			//uniqueBlock.addSpecies(reducedSpecies);
		}
		//Default block for all reduced species
		IBlock uniqueBlock = new Block();
		IPartition trivialPartition = new Partition(uniqueBlock,reducedCRN.getSpeciesSize());
		//uniqueBlock.setPartition(trivialPartition);
		for(ISpecies reducedSpecies : reducedCRN.getSpecies()){
			uniqueBlock.addSpecies(reducedSpecies);
		}
		//trivialPartition.changeNumberOfSpecies(reducedCRN.getSpeciesSize());
		
		List<ICRNReaction> reducedReactions = new ArrayList<ICRNReaction>();
		//Store only reactions where all reagents are block representatives (and canonize the products)
		for (ICRNReaction reaction : crn.getReactions()) {
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			if(allReagentsAreBlockRepresentatives(reaction.getReagents(),partition)){
				BigDecimal reactionRate=null;
				String rateExpression = reaction.getRateExpression();
				try{
				 reactionRate = BigDecimal.valueOf(crn.getMath().evaluate(rateExpression));
				}catch(java.lang.ArithmeticException e){
					//System.out.println("Symbolic rate? "+rateExpression);
				}
				if(BigDecimal.ZERO.compareTo(reactionRate)!=0) {
					IComposite reducedReagents = getNewCompositeReplaceSpeciesWithReducedOneOfBlock(reaction.getReagents(),partition,correspondenceBlock_ReducedSpecies);
					IComposite reducedProducts = getNewCompositeReplaceSpeciesWithReducedOneOfBlock(reaction.getProducts(),partition,correspondenceBlock_ReducedSpecies);
					//ExactFluidBisimilarity.addToListOfReducedReactions(partition, reducedCRN, speciesIdToSpecies, speciesNameToSpecies, correspondenceBlock_ReducedSpecies, reducedReactions, reaction, reducedReagents, reducedProducts);
					
					ICRNReaction reducedReaction = new CRNReaction(reactionRate, reducedReagents, reducedProducts, reaction.getRateExpression(),reaction.getID());
					reducedReactions.add(reducedReaction);
				}
			}
		}
		
		//CRN.collapseAndAddReactions(reducedCRN,reducedReactions);
		
		//In this method, I'm sure that the CRN has to be mass action
		if(!crn.isMassAction()){
			throw new UnsupportedOperationException("This is not a massaction CRN. Use method: it.imt.erode.partitionrefinement.algorithms.SMTOrdinaryFluidBisimilarityBinary.computeReducedCRNOrdinaryNonMassAction");
		}
		if(CRNReducerCommandLine.COLLAPSEREACTIONS){
			CRN.collapseAndCombineAndAddReactions(reducedCRN,reducedReactions,out,bwOut);
		}
		else{
			CRN.addReactionsWithReagentsDifferentFromProducts(reducedCRN, reducedReactions);
		}
		
		/*if(CRNReducerCommandLine.collapseReactions && crn.isMassAction()){
			CRN.collapseAndCombineAndAddReactions(reducedCRN,reducedReactions,true,out,bwOut);
		}
		else{
			for (ICRNReaction reducedReaction : reducedReactions) {
				reducedCRN.addReaction(reducedReaction);
				AbstractImporter.addToIncomingReactionsOfProducts(reducedReaction.getArity(),reducedReaction.getProducts(), reducedReaction,CRNReducerCommandLine.addReactionToComposites);
				AbstractImporter.addToOutgoingReactionsOfReagents(reducedReaction.getArity(), reducedReaction.getReagents(), reducedReaction);
				if(CRNReducerCommandLine.hasToAddToReactionsWithNonZeroStoichiometry){
					AbstractImporter.addToReactionsWithNonZeroStoichiometry(reducedReaction.getArity(), reducedReaction.computeProductsMinusReagentsHashMap(),reducedReaction);
				}
			}
		}*/

		return new CRNandPartition(reducedCRN, trivialPartition);
	}
	
	protected static IComposite getNewCompositeReplaceSpeciesWithReducedOneOfBlock(IComposite composite, IPartition partition, HashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies) {
		
		if(composite.isUnary()){
			return (IComposite)correspondenceBlock_ReducedSpecies.get(partition.getBlockOf(composite.getFirstReagent()));
		}
		else{
			//ISpecies[] allSpecies = composite.getAllSpecies();
			//int[] multiplicities = composite.getMultiplicities();
			HashMap<ISpecies, Integer> reducedSpeciesMultiplicities = computeReducedHashMap(composite, partition,correspondenceBlock_ReducedSpecies);

			return new Composite(reducedSpeciesMultiplicities);
		}
	}

	private static HashMap<ISpecies, Integer> computeReducedHashMap(IComposite composite, IPartition partition,
			HashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies) {
		HashMap<ISpecies,Integer> reducedSpeciesMultiplicities = new HashMap<ISpecies, Integer>();

		for(int i=0; i < composite.getNumberOfDifferentSpecies(); i++){
			ISpecies reducedSpecies = correspondenceBlock_ReducedSpecies.get(partition.getBlockOf(composite.getAllSpecies(i)));
			Integer mult = reducedSpeciesMultiplicities.get(reducedSpecies);
			if(mult==null){
				mult = composite.getMultiplicities(i);
			}
			else{
				mult = mult + composite.getMultiplicities(i);
			}
			reducedSpeciesMultiplicities.put(reducedSpecies, mult);
		}
		return reducedSpeciesMultiplicities;
	}

	/**
	 * This method refines the input partition splitting its blocks in blocks of species appearing in the same views. 
	 * The method succeeds only if the views correspond to a partition, i.e. views are just disjoint sums of distinct species
	 * @param crn
	 * @param partition the input partition
	 * @param printInfo print information
	 * @return
	 */
	public static IPartition prepartitionWRTVIEWS(ICRN crn, IPartition partition,boolean printInfo,MessageConsoleStream out, BufferedWriter bwOut,Terminator  terminator) {

		long begin = System.currentTimeMillis();

		IPartition refinement = partition;

		if(crn.getViewsAsMultiset()!=null){
			for (HashMap<ISpecies,Integer> speciesOfView : crn.getViewsAsMultiset()) {
				if(Terminator.hasToTerminate(terminator)){
					break;
				}
				IPartition prevPartition=refinement; 
				refinement = new Partition(crn.getSpeciesSize());
				IBlock currentBlock = prevPartition.getFirstBlock();
				while(currentBlock!=null){
					//The binary search tree used to split each block
					SpeciesSplayBST<Integer, IBlock> bst = new SpeciesSplayBST<Integer, IBlock>();
					for (ISpecies species : currentBlock.getSpecies()) {
						// Insert the species "species" in the global binary search tree created, so to partition each block of the current partition according to the initial concentrantions. 
						// This may cause the creation of a new block, in which case it is automatically added to refinement.
						Integer mult = speciesOfView.get(species);
						if(mult==null){
							mult=0;
						}
						bst.put(mult, species, refinement);
					}
					currentBlock=currentBlock.getNext();
				}
			}
		}
		long end = System.currentTimeMillis();
		if(printInfo){
			if(crn.getViewsAsMultiset()!=null){
				CRNReducerCommandLine.println(out,bwOut,"Refinement of the input partition with respect to the views/groups completed. From "+partition.size()+" to "+refinement.size()+" blocks. Time necessary: "+(end-begin)+ " (ms)");
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Refinement of the input partition with respect to the views/groups FAILED: the specified views does not allow it.");
			}
		}
		return refinement;
	}
	
	public static IPartition prepartitionUserDefined(List<ISpecies> allSpecies, ArrayList<HashSet<ISpecies>> userDefinedPartition, boolean printInfo,MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator) {
		long begin = System.currentTimeMillis();

		IBlock uniqueBlock = new Block();
		IPartition partition = new Partition(uniqueBlock,allSpecies.size());
		for (ISpecies species : allSpecies) {
			uniqueBlock.addSpecies(species);
		}

		ArrayList<HashSet<ISpecies>> initialPartition = userDefinedPartition;

		for (HashSet<ISpecies> currentBlockSet : initialPartition) {
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			IBlock currentBlock = new Block();
			partition.add(currentBlock);
			for (ISpecies currentSpecies : currentBlockSet) {
				IBlock oldBlock = partition.getBlockOf(currentSpecies);
				oldBlock.removeSpecies(currentSpecies);
				currentBlock.addSpecies(currentSpecies);
				if(oldBlock.getSpecies().size()==0){
					partition.remove(oldBlock);
				}
			}
		}

		long end = System.currentTimeMillis();
		if(printInfo){
			double seconds = (end-begin)/1000.0;			
			CRNReducerCommandLine.println(out,bwOut,"User-defined prepartitioning completed. The "+allSpecies.size()+" species have been prepartitioned in "+partition.size()+" blocks. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, (seconds))+ " (s)");
		}
		return partition;
	}
	
	/**
	 * This method sets the user defined partition as current partition 
	 * @param crn
	 * @param printInfo print information
	 * @param terminator 
	 * @return
	 */
	public static IPartition prepartitionUserDefined(ICRN crn, boolean printInfo,MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator) {

		return prepartitionUserDefined(crn.getSpecies(),crn.getUserDefinedPartition(),printInfo,out,bwOut,terminator);
		
//		long begin = System.currentTimeMillis();
//		
//		IBlock uniqueBlock = new Block();
//		IPartition partition = new Partition(uniqueBlock,crn.getSpeciesSize());
//		for (ISpecies species : crn.getSpecies()) {
//			uniqueBlock.addSpecies(species);
//		}
//		
//		ArrayList<HashSet<ISpecies>> initialPartition = crn.getUserDefinedPartition();
//		
//		for (HashSet<ISpecies> currentBlockSet : initialPartition) {
//			if(Terminator.hasToTerminate(terminator)){
//				break;
//			}
//			IBlock currentBlock = new Block();
//			partition.add(currentBlock);
//			for (ISpecies currentSpecies : currentBlockSet) {
//				IBlock oldBlock = partition.getBlockOf(currentSpecies);
//				oldBlock.removeSpecies(currentSpecies);
//				currentBlock.addSpecies(currentSpecies);
//				if(oldBlock.getSpeciesSize()==0){
//					partition.remove(oldBlock);
//				}
//			}
//		}
//		
//		long end = System.currentTimeMillis();
//		if(printInfo){
//			double seconds = (end-begin)/1000.0;			
//			CRNReducerCommandLine.println(out,bwOut,"User-defined prepartitioning completed. The "+crn.getSpeciesSize()+" species have been prepartitioned in "+partition.size()+" blocks. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, (seconds))+ " (s)");
//		}
//		return partition;
	}
	
	public static CRNandPartition computeReducedCRNQuotient(ICRN crn, String name, IPartition partition, List<String> symbolicParameters, List<IConstraint> constraints,List<String> parameters,MessageConsoleStream out, BufferedWriter bwOut,Terminator terminator) throws IOException {
		return computeReducedCRNQuotient(crn,name,partition,symbolicParameters,constraints,parameters, "#",out,bwOut,terminator);
	}
	
/*	
	@SuppressWarnings("unused")
	public static CRNandPartition computeReducedCRNQuotient(ICRN crn, String name, IPartition partition, List<String> symbolicParameters,List<IConstraint> constraints,List<String> parameters, String commentSymbol,MessageConsoleStream out, BufferedWriter bwOut,Terminator terminator) throws IOException {
		ICRN reducedCRN = new CRN(name,symbolicParameters,constraints,parameters,crn.getMath(),out,bwOut);
		ISpecies[] speciesIdToSpecies= new ISpecies[crn.getSpeciesSize()];
		crn.getSpecies().toArray(speciesIdToSpecies);
		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpeciesSize());
		for (ISpecies species : crn.getSpecies()) {
			speciesNameToSpecies.put(species.getName(), species);
		}
				
		IBlock currentBlock = partition.getFirstBlock();
		//A species per block
		int i=0;
		HashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies = new HashMap<IBlock, ISpecies>(partition.size());
		while(currentBlock!=null){
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			ISpecies blockRepresentative = currentBlock.getRepresentative(CRNReducerCommandLine.COMPUTEREPRESENTATIVEBYMINOUTSIDEPARTITIONREFINEMENT);
			String nameRep=blockRepresentative.getName();
			BigDecimal ic = currentBlock.getBlockConcentration();
			
			ISpecies reducedSpecies;
			
//			boolean isZeroSpecies = false;
//			if(crn.containsTheZeroSpecies()){
//				isZeroSpecies=blockRepresentative.equals(crn.getCreatingIfNecessaryTheZeroSpecies());
//			}
			if(crn.isZeroSpecies(blockRepresentative)){
			//if(crn.containsTheZeroSpecies() && nameRep.equals(Species.ZEROSPECIESNAME)){
				reducedSpecies = reducedCRN.getCreatingIfNecessaryTheZeroSpecies(nameRep);
				reducedSpecies.setInitialConcentration(ic, currentBlock.computeBlockConcentrationExpr());
			}
			else{
				reducedSpecies = new Species(nameRep, blockRepresentative.getOriginalName(),i, ic,currentBlock.computeBlockConcentrationExpr());
				reducedCRN.addSpecies(reducedSpecies);
			}			
			reducedSpecies.addCommentLines(currentBlock.computeBlockComment());
			correspondenceBlock_ReducedSpecies.put(currentBlock, reducedSpecies);
			currentBlock=currentBlock.getNext();
			i++;
		}
		//Default block for all reduced species
		IBlock uniqueBlock = new Block();
		IPartition trivialPartition = new Partition(uniqueBlock,reducedCRN.getSpeciesSize());
		for(ISpecies reducedSpecies : reducedCRN.getSpecies()){
			uniqueBlock.addSpecies(reducedSpecies);
		}
		
		List<ICRNReaction> reducedReactions = new ArrayList<ICRNReaction>();
		for (ICRNReaction reaction : crn.getReactions()) {
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			IComposite reducedReagents = CRNBisimulationsNAry.getNewCompositeReplaceSpeciesWithReducedOneOfBlock(reaction.getReagents(),partition,correspondenceBlock_ReducedSpecies);
			IComposite reducedProducts = CRNBisimulationsNAry.getNewCompositeReplaceSpeciesWithReducedOneOfBlock(reaction.getProducts(),partition,correspondenceBlock_ReducedSpecies);
			if(!reducedReagents.equals(reducedProducts)){
				//ExactFluidBisimilarity.addToListOfReducedReactions(partition, reducedCRN, speciesIdToSpecies, speciesNameToSpecies, correspondenceBlock_ReducedSpecies, reducedReactions, reaction, reducedReagents, reducedProducts);
				double productOfBlockSizesOfReagents = multiplySizeOfBlocks(reaction.getReagents(),partition);
				BigDecimal rate = reaction.getRate();
				String rateExpression = reaction.getRateExpression();
				if(productOfBlockSizesOfReagents!=1){
					if(rate!=null){
						rate = rate.divide(BigDecimal.valueOf(productOfBlockSizesOfReagents), CRNBisimulationsNAry.SCALE,CRNBisimulationsNAry.RM);
					}
					if(rate==null || CRNReducerCommandLine.KEEPTRACEOFRATEEXPRESSIONS){
						rateExpression = "("+rateExpression+")/"+productOfBlockSizesOfReagents;
					}
					else{
						rateExpression = String.valueOf(rate.doubleValue());
					}

				}
				ICRNReaction reducedReaction = new CRNReaction(rate, reducedReagents, reducedProducts, rateExpression,reaction.getID());
				reducedReactions.add(reducedReaction);
			}
		}

		//In this method, I'm sure that the CRN has to be mass action
		if(!crn.isMassAction()){
			throw new UnsupportedOperationException("This is not a massaction CRN. Use method: it.imt.erode.partitionrefinement.algorithms.SMTOrdinaryFluidBisimilarityBinary.computeReducedCRNOrdinaryNonMassAction");
		}
		if(CRNReducerCommandLine.COLLAPSEREACTIONS){
			CRN.collapseAndCombineAndAddReactions(reducedCRN,reducedReactions,out,bwOut);
		}
		else{
			for(ICRNReaction reducedReaction : reducedReactions){
				reducedCRN.addReaction(reducedReaction);
				//zeroSpeciesAppearsInReactions = zeroSpeciesAppearsInReactions || appears;
				AbstractImporter.addToIncomingReactionsOfProducts(reducedReaction.getArity(),reducedReaction.getProducts(), reducedReaction,CRNReducerCommandLine.addReactionToComposites);
				AbstractImporter.addToOutgoingReactionsOfReagents(reducedReaction.getArity(), reducedReaction.getReagents(), reducedReaction,CRNReducerCommandLine.addReactionToComposites);
				if(CRNReducerCommandLine.hasToAddToReactionsWithNonZeroStoichiometry){
					AbstractImporter.addToReactionsWithNonZeroStoichiometry(reducedReaction.getArity(), reducedReaction.computeProductsMinusReagentsHashMap(),reducedReaction);
				}
			}
		}

		return new CRNandPartition(reducedCRN, trivialPartition);
	}
	
*/
	
	public static ISpecies[] getSortedBlockRepresentatives(IPartition partition, Terminator terminator) {
		IBlock currentBlock = partition.getFirstBlock();
		int i=0;
		ISpecies[] representativeSpecies = new ISpecies[partition.size()];
		while(currentBlock!=null){
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			ISpecies blockRepresentative = currentBlock.getRepresentative(CRNReducerCommandLine.COMPUTEREPRESENTATIVEBYMINOUTSIDEPARTITIONREFINEMENT);
			representativeSpecies[i]=blockRepresentative;
			currentBlock=currentBlock.getNext();
			i++;
		}

		Arrays.sort(representativeSpecies);
		return representativeSpecies;
	}
	
	public static CRNandPartition computeReducedCRNQuotient(ICRN crn, String name, IPartition partition, List<String> symbolicParameters,List<IConstraint> constraints,List<String> parameters, String commentSymbol,MessageConsoleStream out, BufferedWriter bwOut,Terminator terminator) throws IOException {
		ICRN reducedCRN = new CRN(name,symbolicParameters,constraints,parameters,crn.getMath(),out,bwOut);
		//ISpecies[] speciesIdToSpecies= new ISpecies[crn.getSpeciesSize()];
		//crn.getSpecies().toArray(speciesIdToSpecies);
		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpeciesSize());
		//int pos=0;
		for (ISpecies species : crn.getSpecies()) {
			speciesNameToSpecies.put(species.getName(), species);
			//speciesIdToSpecies[pos]=species;
			//pos++;
		}

		HashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies = new HashMap<IBlock, ISpecies>(partition.size());
		//if(CRNReducerCommandLine.MAINTAINORDEROFSPECIESINREDUCTION) {
			ISpecies[] representativeSpecies = getSortedBlockRepresentatives(partition, terminator);
			
			for(int i=0;i<representativeSpecies.length;i++) {
				if(Terminator.hasToTerminate(terminator)){
					break;
				}
				ISpecies blockRepresentative = representativeSpecies[i];
				IBlock currentBlock = partition.getBlockOf(blockRepresentative);
				String nameRep=blockRepresentative.getName();
				BigDecimal ic = currentBlock.getBlockConcentration();

				ISpecies reducedSpecies;
				if(crn.isZeroSpecies(blockRepresentative)){
					//if(crn.containsTheZeroSpecies() && nameRep.equals(Species.ZEROSPECIESNAME)){
					reducedSpecies = reducedCRN.getCreatingIfNecessaryTheZeroSpecies(nameRep);
					reducedSpecies.setInitialConcentration(ic, currentBlock.computeBlockConcentrationExpr());
				}
				else{
					reducedSpecies = new Species(nameRep, blockRepresentative.getOriginalName(),i, ic,currentBlock.computeBlockConcentrationExpr(),blockRepresentative.isAlgebraic());
					reducedCRN.addSpecies(reducedSpecies);
				}			
				reducedSpecies.addCommentLines(currentBlock.computeBlockComment());
				//reducedSpecies.setRepresentedEquivalenceClass(currentBlock.getSpecies());
				correspondenceBlock_ReducedSpecies.put(currentBlock, reducedSpecies);
			}
		/*}
		else {
			IBlock currentBlock = partition.getFirstBlock();
			//A species per block
			int i=0;
			//HashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies = new HashMap<IBlock, ISpecies>(partition.size());
			while(currentBlock!=null){
				if(Terminator.hasToTerminate(terminator)){
					break;
				}
				ISpecies blockRepresentative = currentBlock.getRepresentative(CRNReducerCommandLine.COMPUTEREPRESENTATIVEBYMINOUTSIDEPARTITIONREFINEMENT);
				String nameRep=blockRepresentative.getName();
				BigDecimal ic = currentBlock.getBlockConcentration();

				ISpecies reducedSpecies;

				//boolean isZeroSpecies = false;
				//if(crn.containsTheZeroSpecies()){
				//	isZeroSpecies=blockRepresentative.equals(crn.getCreatingIfNecessaryTheZeroSpecies());
				//}
				if(crn.isZeroSpecies(blockRepresentative)){
					//if(crn.containsTheZeroSpecies() && nameRep.equals(Species.ZEROSPECIESNAME)){
					reducedSpecies = reducedCRN.getCreatingIfNecessaryTheZeroSpecies(nameRep);
					reducedSpecies.setInitialConcentration(ic, currentBlock.computeBlockConcentrationExpr());
				}
				else{
					reducedSpecies = new Species(nameRep, blockRepresentative.getOriginalName(),i, ic,currentBlock.computeBlockConcentrationExpr(),blockRepresentative.isAlgebraic());
					reducedCRN.addSpecies(reducedSpecies);
				}			
				reducedSpecies.addCommentLines(currentBlock.computeBlockComment());
				//reducedSpecies.setRepresentedEquivalenceClass(currentBlock.getSpecies());
				correspondenceBlock_ReducedSpecies.put(currentBlock, reducedSpecies);
				currentBlock=currentBlock.getNext();
				i++;
			}

		}*/
		//Default block for all reduced species
		IBlock uniqueBlock = new Block();
		IPartition trivialPartition = new Partition(uniqueBlock,reducedCRN.getSpeciesSize());
		for(ISpecies reducedSpecies : reducedCRN.getSpecies()){
			uniqueBlock.addSpecies(reducedSpecies);
		}

		List<ICRNReaction> reducedReactions = new ArrayList<ICRNReaction>();
		for (ICRNReaction reaction : crn.getReactions()) {
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			BigDecimal rate = reaction.getRate();
			String rateExpression = reaction.getRateExpression();
			if(BigDecimal.ZERO.compareTo(rate)!=0) {
				IComposite reducedReagents = CRNBisimulationsNAry.getNewCompositeReplaceSpeciesWithReducedOneOfBlock(reaction.getReagents(),partition,correspondenceBlock_ReducedSpecies);
				IComposite reducedProducts = CRNBisimulationsNAry.getNewCompositeReplaceSpeciesWithReducedOneOfBlock(reaction.getProducts(),partition,correspondenceBlock_ReducedSpecies);
				//Skip reactions where the reagents are equal to the products  
				if(!reducedReagents.equals(reducedProducts)){
					//ExactFluidBisimilarity.addToListOfReducedReactions(partition, reducedCRN, speciesIdToSpecies, speciesNameToSpecies, correspondenceBlock_ReducedSpecies, reducedReactions, reaction, reducedReagents, reducedProducts);
					/*
				double productOfBlockSizesOfReagents = multiplySizeOfBlocks(reaction.getReagents(),partition);
				if(productOfBlockSizesOfReagents!=1){
					if(rate!=null){
						rate = rate.divide(BigDecimal.valueOf(productOfBlockSizesOfReagents), CRNBisimulationsNAry.SCALE,CRNBisimulationsNAry.RM);
					}
					if(rate==null || CRNReducerCommandLine.KEEPTRACEOFRATEEXPRESSIONS){
						rateExpression = "("+rateExpression+")/"+productOfBlockSizesOfReagents;
					}
					else{
						rateExpression = String.valueOf(rate.doubleValue());
					}

				}
					 */
					ICRNReaction reducedReaction = new CRNReaction(rate, reducedReagents, reducedProducts, rateExpression,reaction.getID());
					if(!POSTPONESCALINGOFRATESWHENCOMPUTINGREDUCEDQUOTIENTMODEL){
						scaleRateOfReactionAccordingToProductOfSizesOfBlocksOfReagents(reaction, reducedReaction, partition);
					}
					reducedReactions.add(reducedReaction);
				}
			}
		}

		//In this method, I'm sure that the CRN has to be mass action
		if(!crn.isMassAction()){
			throw new UnsupportedOperationException("This is not a massaction CRN. Use method: it.imt.erode.partitionrefinement.algorithms.SMTOrdinaryFluidBisimilarityBinary.computeReducedCRNOrdinaryNonMassAction");
		}
		if(CRNReducerCommandLine.COLLAPSEREACTIONS){
			CRN.collapseAndCombineAndAddReactions(reducedCRN,reducedReactions,out,bwOut);
		}
		else{
			for(ICRNReaction reducedReaction : reducedReactions){
				reducedCRN.addReaction(reducedReaction);
				//zeroSpeciesAppearsInReactions = zeroSpeciesAppearsInReactions || appears;
				/*
				AbstractImporter.addToIncomingReactionsOfProducts(reducedReaction.getArity(),reducedReaction.getProducts(), reducedReaction,CRNReducerCommandLine.addReactionToComposites);
				AbstractImporter.addToOutgoingReactionsOfReagents(reducedReaction.getArity(), reducedReaction.getReagents(), reducedReaction,CRNReducerCommandLine.addReactionToComposites);
				if(CRNReducerCommandLine.hasToAddToReactionsWithNonZeroStoichiometry){
					AbstractImporter.addToReactionsWithNonZeroStoichiometry(reducedReaction.getArity(), reducedReaction.computeProductsMinusReagentsHashMap(),reducedReaction);
				}
				*/
			}
		}
		
		if(POSTPONESCALINGOFRATESWHENCOMPUTINGREDUCEDQUOTIENTMODEL){
			for (ICRNReaction reducedReaction : reducedCRN.getReactions()) {
				scaleRateOfReducedReactionAccordingToProductOfSizesOfBlocksOfOriginalReagents(reducedReaction, partition, speciesNameToSpecies);
			}
		}
		
		String[] viewNames = new String[correspondenceBlock_ReducedSpecies.size()];
		String[] viewExprs = new String[correspondenceBlock_ReducedSpecies.size()];
		String[] viewExprsSupportedByMathEval = new String[correspondenceBlock_ReducedSpecies.size()];
		boolean[] viewExprsUsesCovariances = new boolean[correspondenceBlock_ReducedSpecies.size()];
		int v=0;
		for (Entry<IBlock, ISpecies> pair : correspondenceBlock_ReducedSpecies.entrySet()) {
			IBlock b = pair.getKey();
			ISpecies s = pair.getValue();
			viewNames[v]="v"+s.getName();
			viewExprs[v]=s.getName() + "/" + b.getSpecies().size();
			viewExprsSupportedByMathEval[v]=s.getNameAlphanumeric() + "/" + b.getSpecies().size();
			v++;
		}
		reducedCRN.setViews(viewNames, viewExprs, viewExprsSupportedByMathEval, viewExprsUsesCovariances);

		return new CRNandPartition(reducedCRN, trivialPartition);
	}
	
	@SuppressWarnings("unused")
	private static void scaleRateOfReactionAccordingToProductOfSizesOfBlocksOfReagents(ICRNReaction originalReaction, ICRNReaction reducedReaction, IPartition partitionOnOriginalCRN){
		BigDecimal rate = originalReaction.getRate();
		String rateExpression = originalReaction.getRateExpression();
		double productOfBlockSizesOfReagents = multiplySizeOfBlocks(originalReaction.getReagents(),partitionOnOriginalCRN);
		if(productOfBlockSizesOfReagents!=1){
			if(rate!=null){
				rate = rate.divide(BigDecimal.valueOf(productOfBlockSizesOfReagents), CRNBisimulationsNAry.SCALE,CRNBisimulationsNAry.RM);
			}
			if(rate==null || CRNReducerCommandLine.KEEPTRACEOFRATEEXPRESSIONS){
				rateExpression = "("+rateExpression+")/"+productOfBlockSizesOfReagents;
			}
			else{
				rateExpression = String.valueOf(rate.doubleValue());
			}

		}
		reducedReaction.setRate(rate, rateExpression);
	}
	
	private static double multiplySizeOfBlocks(IComposite reagents, IPartition partition) {
		double ret = 1.0;
		for(int r = 0; r<reagents.getNumberOfDifferentSpecies();r++){
			//ret*=partition.getBlockOf(reagents.getAllSpecies(r)).getSpeciesSize();//Ignoring multiplicities
			int blockSize=partition.getBlockOf(reagents.getAllSpecies(r)).getSpecies().size();
			int mult = reagents.getMultiplicities(r);
			//ret*= (blockSize * mult);
			if(mult==1){
				ret *=blockSize;
			}
			else{
				ret *= Math.pow(blockSize,mult);
			}
		}
		return ret;
	}
	
	@SuppressWarnings("unused")
	private static void scaleRateOfReducedReactionAccordingToProductOfSizesOfBlocksOfOriginalReagents(ICRNReaction reducedReaction, IPartition partitionComputedOnOriginalCRN,HashMap<String, ISpecies> speciesNameToOriginalSpecies){
		double productOfBlockSizesOfReagents = multiplySizeOfBlocks(reducedReaction.getReagents(),partitionComputedOnOriginalCRN,speciesNameToOriginalSpecies);
		BigDecimal rate = reducedReaction.getRate();
		String rateExpression = reducedReaction.getRateExpression();
		if(productOfBlockSizesOfReagents!=1){
			if(rate!=null){
				rate = rate.divide(BigDecimal.valueOf(productOfBlockSizesOfReagents), CRNBisimulationsNAry.SCALE,CRNBisimulationsNAry.RM);
			}
			if(rate==null || CRNReducerCommandLine.KEEPTRACEOFRATEEXPRESSIONS){
				rateExpression = "("+rateExpression+")/"+productOfBlockSizesOfReagents;
			}
			else{
				rateExpression = String.valueOf(rate.doubleValue());
			}
			reducedReaction.setRate(rate, rateExpression);
		}
	}

	private static double multiplySizeOfBlocks(IComposite reducedReagents, IPartition partitionComputedOnOriginalCRN,HashMap<String, ISpecies> speciesNameToOriginalSpecies) {
		double ret = 1.0;
		for(int r = 0; r<reducedReagents.getNumberOfDifferentSpecies();r++){
			ISpecies orignalSpecies = speciesNameToOriginalSpecies.get(reducedReagents.getAllSpecies(r).getName());
			int blockSize=partitionComputedOnOriginalCRN.getBlockOf(orignalSpecies).getSpecies().size();
			int mult = reducedReagents.getMultiplicities(r);
			if(mult==1){
				ret *=blockSize;
			}
			else{
				ret *= Math.pow(blockSize,mult);
			}
		}
		return ret;
	}
	
}


