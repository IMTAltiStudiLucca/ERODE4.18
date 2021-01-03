package it.imt.erode.partitionrefinement.algorithms;


import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.auxiliarydatastructures.IPartitionAndBoolean;
import it.imt.erode.auxiliarydatastructures.LabelToBigDecimal;
import it.imt.erode.auxiliarydatastructures.SetOfLabels;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.implementations.CRN;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.label.EmptySetLabel;
import it.imt.erode.crn.label.ILabel;
import it.imt.erode.partition.implementations.CompositePartition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.ICompositeBlock;
import it.imt.erode.partition.interfaces.ICompositePartition;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.splittersbstandcounters.HowToSplitCompositeBlocks;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SpeciesCounterField;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SpeciesCounterHandler;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SplittersGeneratorSMB;





public class SyntacticMarkovianBisimilarityAllLabelsNoSelfLoopsOLD {

	public static final HowToSplitCompositeBlocks HOWTOSPLITCOMPOSITEBLOCKS=HowToSplitCompositeBlocks.ComputeBlockMultiplicitiesAtNeed;;
	//public static final HowToSplitCompositeBlocks HOWTOSPLITCOMPOSITEBLOCKS=HowToSplitCompositeBlocks.UseStoredLabelsBlockByBlock;
	

	
	/**
	 * 
	 * @param crn
	 * @param labels
	 * @param partition the partition to be refined. It is not modified, but instead a new one is created and returned
	 * @param initializeCounters
	 * @param verbose
	 * @param terminator 
	 * @param msgDialogShower 
	 * @param halveRatesOfHomeoReactions 
	 * @return
	 * @throws IOException 
	 */
	public static IPartitionAndBoolean computeSMB(ICRN crn, /*List<ILabel> labels,*/ IPartition partition, /*ICompositePartition compositePartition,*/ boolean verbose,MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator, IMessageDialogShower msgDialogShower, boolean halveRatesOfHomeoReactions) throws IOException{
		
		//This algorithm does not work. This is a counterexample:
		/*
		 a + c -> c   , 1
		 b + c -> c   , 1
		 b + c -> a + c   , 1
		 
		 Intuitively, the reason is that we are able to 'kind of add' 
		 	b + c -> b + c   , -2
		 But we do not add
		 	a + c -> a + c   , -1
		 Therefore we split a and b, while we should not. 
		 */
		
		
		List<ILabel> fakeLabels=new ArrayList<>();
		fakeLabels.add(EmptySetLabel.EMPTYSETLABEL);
		
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"SMB Reducing: "+crn.getName());
		}
		
		IPartition obtainedPartition = partition.copy();
		
		if(!crn.isElementary()){
			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it is not an elementary CRN (i.e., it has ternary or more reactions). I terminate.");
			return new IPartitionAndBoolean(obtainedPartition, false);
		}
		else if(!crn.isMassAction()){
			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it is not a mass action CRN (i.e., it has reactions with arbitrary rates). I terminate.");
			return new IPartitionAndBoolean(obtainedPartition, false);
		}
		else if(crn.isSymbolic()){
			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it contains symbolic parameters (i.e. parameters with no acutal value assigned) I terminate.");
			return new IPartitionAndBoolean(obtainedPartition, false);
		}
//		else if(!(CRNReducerCommandLine.univoqueProducts)){
//			//CRNReducerCommandLine.printWarning(out,bwOut,"Not all necessary data structure have been filled. CRNReducerCommandLine.hasToAddToReactionsWithNonZeroStoichiometry must be set to true.");
//			CRNReducerCommandLine.printWarning(out, bwOut, true, msgDialogShower, "Not all necessary data structure have been filled. CRNReducerCommandLine.univoqueProducts must be set to true.", DialogType.Error);
//			return new IPartitionAndBoolean(obtainedPartition, false);
//		}
//		else if(!(CRNReducerCommandLine.addReactionToComposites)){
//			//CRNReducerCommandLine.printWarning(out,bwOut,"Not all necessary data structure have been filled. CRNReducerCommandLine.hasToAddToReactionsWithNonZeroStoichiometry must be set to true.");
//			CRNReducerCommandLine.printWarning(out, bwOut, true, msgDialogShower, "Not all necessary data structure have been filled. CRNReducerCommandLine.addReactionToComposites must be set to true.", DialogType.Error);
//		}
		
		
		if(verbose){
			String blocks = " blocks";
			if(obtainedPartition.size()==1){
				blocks = " block";
			}
			CRNReducerCommandLine.println(out,bwOut,"Before partitioning we have "+ obtainedPartition.size() + blocks +" and "+crn.getSpecies().size()+ " species.");
		}

		long begin = System.currentTimeMillis();
		
		SpeciesCounterHandler speciesCounters[] = new SpeciesCounterHandler[crn.getSpecies().size()];
		
		ICRN crnNew = crn;
		if(halveRatesOfHomeoReactions) {
			crnNew = CRN.halveRatesOfHomeoReactions(crnNew, out, bwOut);
		}
		
		long beginPreComp=System.currentTimeMillis();
		//int homeo=0;
		CRNReducerCommandLine.print(out,bwOut,"\n\tScanning the reactions once to pre-compute informations necessary to the partitioning ... ");
		Set<IComposite> products = new HashSet<>();
		HashMap<IComposite, ArrayList<ICRNReaction>> reactionsToConsiderForEachComposite=new HashMap<>();
		LabelToBigDecimal[] outRates = new LabelToBigDecimal[crnNew.getSpecies().size()];
		for (ICRNReaction reaction : crnNew.getReactions()) {
			IComposite productsOfReaction = reaction.getProducts();
			products.add(productsOfReaction);
			//out...
			IComposite reag = reaction.getReagents();
			if(reag.isUnary()) {
				add(outRates,reag.getFirstReagent().getID(),EmptySetLabel.EMPTYSETLABEL,reaction.getRate());
				//outRates[reag.getFirstReagent().getID()].add(EmptySetLabel.EMPTYSETLABEL, reaction.getRate());
			}
			//binary
			else {
				//outRates[reag.getFirstReagent().getID()].add(reag.getSecondReagent(), reaction.getRate());
				add(outRates,reag.getFirstReagent().getID(),reag.getSecondReagent(), reaction.getRate());
				if(!reag.isHomeo()) {
					//outRates[reag.getSecondReagent().getID()].add(reag.getFirstReagent(), reaction.getRate());
					add(outRates,reag.getSecondReagent().getID(),reag.getFirstReagent(), reaction.getRate());
				}
			}
			SyntacticMarkovianBisimilarityOLD.addReactionToConsider(reactionsToConsiderForEachComposite, reaction, productsOfReaction);
		}
		long endPreComp=System.currentTimeMillis();
		CRNReducerCommandLine.println(out,bwOut,"completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((endPreComp-beginPreComp)/1000.0))+" (s)");
	
		long beginPartitionProducts=System.currentTimeMillis();
		CRNReducerCommandLine.print(out,bwOut,"\tCreating initial partition of products... ");
		ICompositePartition compositesPartition = new CompositePartition(products,partition);
		long endPartitionProducts=System.currentTimeMillis();
		CRNReducerCommandLine.println(out,bwOut,"completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((endPartitionProducts-beginPartitionProducts)/1000.0))+" (s)");
		//CRNReducerCommandLine.print(out,bwOut," (refining up to SMB) ");
		
		CRNReducerCommandLine.print(out,bwOut,"\tRefining up to SMB... ");
		refineSMB(crnNew,obtainedPartition,compositesPartition,fakeLabels,speciesCounters,terminator,out,bwOut,reactionsToConsiderForEachComposite,outRates);
		
		
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"The final partition:");
			CRNReducerCommandLine.println(out,bwOut,obtainedPartition);
		}
		
		long end = System.currentTimeMillis();
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"SMB Partitioning completed. From "+ crn.getSpecies().size() +" species to "+ obtainedPartition.size() + " blocks. Time necessary: "+(end-begin)+ " (ms)");
			CRNReducerCommandLine.println(out,bwOut,"");
		}
		return new IPartitionAndBoolean(obtainedPartition, true);
	}
	
	private static void add(LabelToBigDecimal[] outRates, int id, ILabel label, BigDecimal rate) {
		if(outRates[id]==null) {
			outRates[id]=new LabelToBigDecimal();
		}
		outRates[id].add(label, rate);
		
	}

	/*private static void refineCRRM(ICRN crn, IPartition partition, List<ILabel> labels, SpeciesCounterHandler[] speciesCounters, Terminator terminator) {
		ICompositePartition compositesPartition = new CompositePartition(crn.getProducts(),partition);
		refineCRRM(crn, partition, compositesPartition, labels, speciesCounters, terminator);
	}*/
	
	private static void refineSMB(ICRN crn, IPartition partition, ICompositePartition compositesPartition, List<ILabel> fakeLabels, SpeciesCounterHandler[] speciesCounters, Terminator terminator, MessageConsoleStream out, BufferedWriter bwOut, HashMap<IComposite, ArrayList<ICRNReaction>> reactionsToConsiderForEachComposite, LabelToBigDecimal[] outRates) {
		
		//generate candidate splitters
		SplittersGeneratorSMB splittersGenerator = compositesPartition.getSplitterGeneratorAndCreateItIfFirstInvocation(fakeLabels);

		//Initialize all counter fields and the bst of the species
		CRNBisimulationsNAry.initializeAllCounters(crn.getSpecies(),speciesCounters);
		partition.initializeAllBST();

//		CRNReducerCommandLine.print(out,bwOut,"\n\tScanning the reactions once to pre-compute informations necessary to the partitioning ... ");
//		long begin = System.currentTimeMillis();
//		HashMap<IComposite, ArrayList<ICRNReaction>> reactionsToConsiderForEachComposite=new HashMap<>(); 
//		SyntacticMarkovianBisimilarity.addToIncomingReactionsOfProducts(crn,reactionsToConsiderForEachComposite);
//		long end = System.currentTimeMillis();
//		CRNReducerCommandLine.print(out,bwOut,"completed in "+ String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0))+" (s)");
		
		int prevSize = -1;
		int iteration=0;
		while(prevSize!=partition.size()){
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			splittersGenerator.restart(compositesPartition.getFirstBlock());
			prevSize=partition.size();
			while(splittersGenerator.hasSplittersToConsider()){
				if(Terminator.hasToTerminate(terminator)){
					break;
				}
				splitSMB(crn,partition,splittersGenerator,compositesPartition,true,iteration,speciesCounters,reactionsToConsiderForEachComposite,outRates);
			}
			iteration++;
			//CRNReducerCommandLine.println(out,bwOut,"iteration "+iteration+" blocks:"+partition.size());
		}
	}
	
	private static void splitSMB(ICRN crn, IPartition partition, SplittersGeneratorSMB splittersGenerator, ICompositePartition compositesPartition, boolean refineUpBlocksAfterAnySplit, int iteration, SpeciesCounterHandler[] speciesCounters, HashMap<IComposite,ArrayList<ICRNReaction>> reactionsToConsiderForEachComposite, LabelToBigDecimal[] outRates) {
		
		ICompositeBlock blockSPL = splittersGenerator.getBlockSpl();
		
		boolean hasOnlyUnaryReactions = crn.getMaxArity()==1;
		//ILabel labelSPL = splittersGenerator.getLabelSpl();
		
		//Initialize the "pr" fields to 0 of all species having reactions with partners label, towards at least a species of blockSPL// This is now done once before invoking splitDSB, and then at the end of splitDSB, for the splittergenerators only
		//initializeAllLabelPRandBST(crn,labelSPL, blockSPL);

		//Set of species with at least a reaction towards the species of the splitter
		HashSet<ISpecies> splitterGenerators = new HashSet<ISpecies>();
		//compute pr[X,label,blockSPL] for all species X having at least a reaction with partners label towards blockSPL (and build the list of such species. Only their blocks can get split)
		boolean anyReactionFound = computeConditionalRatesTowardsTheSplitterBlock(crn, /*labelSPL,*/ blockSPL,splitterGenerators,speciesCounters,hasOnlyUnaryReactions,reactionsToConsiderForEachComposite,outRates,partition);
		
		if(!anyReactionFound){
			//CRNReducerCommandLine.println(out,bwOut,"No reactions considered at this iteration. I can skip this split iteration");
			splittersGenerator.generateNextSplitter();
			return;
		}

		//Set of blocks to be considered for splitting (blocks where at least a species performs a reaction with partners label towards species of blockSPL)
		HashSet<IBlock> splittedBlocks = new HashSet<IBlock>();
		SpeciesCounterField splitWRT = SpeciesCounterField.SMBVECTOR;
		if(hasOnlyUnaryReactions){
			splitWRT = SpeciesCounterField.SMBCOUNTER;
		}
		CRNBisimulations.partitionBlocksOfGivenSpecies(partition,splitterGenerators,null,splittedBlocks,splitWRT,speciesCounters);
		
		//Now I have to update the splitters according to the newly generated partition
		//cleanPartitioAndGetNextSplitterIfNecessary(partition, splittersGenerator, splittedBlocks, compositesPartition,refineUpBlocksAfterAnySplit);
		cleanPartitioAndGetNextSplitterIfNecessaryFocusingOnTheCurrentCompBlockOnly(partition, splittersGenerator, splittedBlocks, compositesPartition,blockSPL,iteration,hasOnlyUnaryReactions);

		//Initialize the "pr" fields to 0 of all species having reactions with partners label, towards at least a species of blockSPL
		CRNBisimulationsNAry.initializeAllCounters(splitterGenerators,speciesCounters);
	}

	
	public static void cleanPartitioAndGetNextSplitterIfNecessaryFocusingOnTheCurrentCompBlockOnly(IPartition partition, 
			SplittersGeneratorSMB splittersGenerator, Set<IBlock> splittedBlocks, ICompositePartition compositesPartition, ICompositeBlock blockSPL,int iteration,boolean hasOnlyUnaryReactions) {
		
		List<List<IBlock>> refineRespectToTheseBlocks = new ArrayList<List<IBlock>>();
		List<IBlock> nonemptySplittedBlocks = new ArrayList<>();
		if(compositesPartition!=null){
			refineRespectToTheseBlocks.add(nonemptySplittedBlocks);
		}
		boolean actuallysplit=false;
		for (IBlock splittedBlock : splittedBlocks) {
			List<IBlock> subBlocks = null; //splittedBlock.getBST().getBlocks();
			if(hasOnlyUnaryReactions){
				if(CRNBisimulationsNAry.USETOLERANCE){
					subBlocks = splittedBlock.getBST(CRNBisimulationsNAry.getTolerance()).getBlocks();
				}
				else{
					subBlocks = splittedBlock.getBST().getBlocks();
				}
			}
			else{
				subBlocks = splittedBlock.getBSTForVectors().getBlocks();
			}
			//If I did not actually splitted the block, but I have just moved all its elements in a new one. Thus I replace the new alias block with the original one. This improves performances, because the new alias block will be considered as the old one, without having to recompute all checks already done
			if(splittedBlock.isEmpty()  && subBlocks.size() == 1){
				IBlock aliasBlock = subBlocks.get(0);
				partition.substituteAndDecreaseSize(aliasBlock,splittedBlock);
				//aliasBlock.throwAwayBST();
			}
			//Otherwise, I have actually split the block. I remove it from the partition if it is empty.
			else{
				if(compositesPartition!=null){
					refineRespectToTheseBlocks.add(subBlocks);
				}
				actuallysplit=true;
				//If the original block became empty, remove it from the partition.
				if(splittedBlock.isEmpty()){
					partition.remove(splittedBlock);
				}
				else{
					//Otherwise, the original block is not empty, and thus I do not remove it from the partition. However, I have to throw away its bst.
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
					if(compositesPartition!=null){
						nonemptySplittedBlocks.add(splittedBlock);
					}
				}
			}
		}
		//If I really split a block, I have to refine the partition of products. Actually, I only refine the current block, and mark the other as "to be splitted". 
		//The sub-blocks obtained splitting the current one are added in the head, and the splitter generator will get reinitialized.
		if(actuallysplit){
			switch (SyntacticMarkovianBisimilarityAllLabelsNoSelfLoopsOLD.HOWTOSPLITCOMPOSITEBLOCKS) {
			case UseStoredLabelsBlockByBlock:
				throw new UnsupportedOperationException();
				/*//TODO: add blockSPL
				compositesPartition.refineViaNewMultiSetLiftingUsingStoredMultisetLabels(partition,refineRespectToTheseBlocks);
				break;*/
			case ComputeBlockMultiplicitiesAtNeed:
				//compositesPartition.refineViaNewMultiSetLifting(partition,refineRespectToTheseBlocks);
				if(compositesPartition!=null){
					compositesPartition.refineViaNewMultiSetLifting(partition,refineRespectToTheseBlocks,blockSPL,iteration);
				}
				break;			
			}
		}
		else{
			if(splittersGenerator!=null){
				//If I have not done any split, I generate the next splitter. 
				splittersGenerator.generateNextSplitter();
			}
		}
	}


/*	protected static void cleanPartitioAndGetNextSplitterIfNecessary(IPartition partition, SplittersGeneratorMSB splittersGenerator, Set<IBlock> splittedBlocks, ICompositePartition compositesPartition, boolean refineUpBlocksAfterAnySplit) {

		List<List<IBlock>> refineRespectToTheseBlocks = new ArrayList<List<IBlock>>();
		List<IBlock> nonemptySplittedBlocks = new ArrayList<>();
		refineRespectToTheseBlocks.add(nonemptySplittedBlocks);
		boolean actuallysplit=false;
		for (IBlock splittedBlock : splittedBlocks) {
			List<IBlock> subBlocks = splittedBlock.getBST().getBlocks();
			//If I did not actually splitted the block, but I have just moved all its elements in a new one. Thus I replace the new alias block with the original one. This improves performances, because the new alias block will be considered as the old one, without having to recompute all checks already done
			if(splittedBlock.isEmpty()  && subBlocks.size() == 1){
				IBlock aliasBlock = subBlocks.get(0);
				partition.substituteAndDecreaseSize(aliasBlock,splittedBlock);
			}
			//Otherwise, I have actually split the block. I remove it from the partition if it is empty.
			else{
				refineRespectToTheseBlocks.add(subBlocks);
				actuallysplit=true;
				//If the original block became empty, remove it from the partition.
				if(splittedBlock.isEmpty()){
					partition.remove(splittedBlock);
				}
				else{
					//Otherwise, the original block is not empty, and thus I do not remove it from the partition. However, I have to throw away its bst.
					splittedBlock.throwAwayBST();
					nonemptySplittedBlocks.add(splittedBlock);
				}
			}
		}
		//If I really split a block, I have to refine the partition of products. This will reinitialize the splitter generator
		if(actuallysplit && refineUpBlocksAfterAnySplit){
			switch (MarkovianSpeciesBisimilarity.HOWTOSPLITCOMPOSITEBLOCKS) {
			case UseStoredLabelsBlockByBlock:
				throw new UnsupportedOperationException();
				//compositesPartition.refineViaNewMultiSetLiftingUsingStoredMultisetLabels(partition,refineRespectToTheseBlocks);break;
			case ComputeBlockMultiplicitiesAtNeed:
				compositesPartition.refineViaNewMultiSetLifting(partition,refineRespectToTheseBlocks);
				break;			
			}
		}
		else{
			//If I have not done any split, I generate the next splitter. 
			splittersGenerator.generateNextSplitter();
		}
	}*/

	private static boolean computeConditionalRatesTowardsTheSplitterBlock(ICRN crn, /*ILabel labelSPL,*/ ICompositeBlock blockSPL, HashSet<ISpecies> splitterGenerators, SpeciesCounterHandler[] speciesCounters, boolean hasOneLabelOnly, HashMap<IComposite,ArrayList<ICRNReaction>> reactionsToConsiderForEachComposite, LabelToBigDecimal[] outRates, IPartition partition) {
		boolean anyReactionConsidered=false;
		SetOfLabels[] consideredForLabels = new SetOfLabels[crn.getSpecies().size()];
		for (IComposite aCompositeOfSplitter : blockSPL.getComposites()) {
			Collection<ICRNReaction> consideredReactions = reactionsToConsiderForEachComposite.get(aCompositeOfSplitter);
			//Collection<ICRNReaction> consideredReactions = aCompositeOfSplitter.getIncomingReactions();
			if(consideredReactions!=null){
				anyReactionConsidered=true;
				for (ICRNReaction incomingReaction : consideredReactions) {
					//I added this if
					if(incomingReaction.getRate().compareTo(BigDecimal.ZERO)!=0){
						boolean multisetEquiv = multisetEquivalent(incomingReaction.getReagents(),incomingReaction.getProducts(),partition);
						increaseSMBCounterOfReagents(incomingReaction,/*labelSPL,*/ splitterGenerators,speciesCounters,hasOneLabelOnly,outRates,multisetEquiv,consideredForLabels);
					}
				}
			}
		}
		return anyReactionConsidered;
	}
	
	private static void increaseSMBCounterOfReagents(ICRNReaction reaction, /*ILabel labelSPL,*/HashSet<ISpecies> splitterGenerators, SpeciesCounterHandler[] speciesCounters, boolean onlyOneLabel, LabelToBigDecimal[] outRates, boolean multisetEquiv, SetOfLabels[] consideredForLabels) {
		IComposite reagents = reaction.getReagents();
		BigDecimal rate = reaction.getRate();
		
		if(reagents.isUnary()){
			ISpecies reagent = reagents.getFirstReagent();
			if(multisetEquiv && outRates[reagent.getID()]!=null && add(consideredForLabels,reagent.getID(),EmptySetLabel.EMPTYSETLABEL)) {
				rate=rate.subtract(outRates[reagent.getID()].getValue(EmptySetLabel.EMPTYSETLABEL));
			}
			
			SpeciesCounterHandler.initSpeciesCounters(speciesCounters, reagent);
			if(onlyOneLabel){
				speciesCounters[reagent.getID()].addToSMBCounter(rate);
			}
			else{
				speciesCounters[reagent.getID()].addToSMBCounter(EmptySetLabel.EMPTYSETLABEL,rate);
			}
			if(splitterGenerators!=null){
				splitterGenerators.add(reagent);
			}
		}
		//Otherwise it is a binary reaction (and hence I cannot have just unary labels)
		else{
			ISpecies firstReagent = reagents.getFirstReagent();
			ISpecies secondReagent = reagents.getSecondReagent();
			
			SpeciesCounterHandler.initSpeciesCounters(speciesCounters, firstReagent);
			if(multisetEquiv && outRates[firstReagent.getID()]!=null && add(consideredForLabels,firstReagent.getID(),secondReagent)) {
				speciesCounters[firstReagent.getID()].addToSMBCounter(secondReagent,
						rate.subtract(outRates[firstReagent.getID()].getValue(secondReagent)));
			}
			else {
				speciesCounters[firstReagent.getID()].addToSMBCounter(secondReagent,
						rate);
			}
			
			if(splitterGenerators!=null){
				splitterGenerators.add(firstReagent);
			}
			
			//The following if is to handle homeoreactions 'x+x->pi'
			//I want to consider only 'x+rho' and not rho 'rho+x'. Otherwise I would add twice the same value
			if(!firstReagent.equals(secondReagent)) {
				SpeciesCounterHandler.initSpeciesCounters(speciesCounters, secondReagent);
				if(multisetEquiv && outRates[secondReagent.getID()]!=null && add(consideredForLabels,secondReagent.getID(),firstReagent)) {
					speciesCounters[secondReagent.getID()].addToSMBCounter(firstReagent,
							rate.subtract(outRates[secondReagent.getID()].getValue(firstReagent)));
				}
				else {
					speciesCounters[secondReagent.getID()].addToSMBCounter(firstReagent,
							rate);
				}
				if(splitterGenerators!=null){
					splitterGenerators.add(secondReagent);
				}
			}
		}
	}
	
	private static boolean add(SetOfLabels[] consideredForLabels, int id, ILabel label) {
		if(consideredForLabels[id]==null) {
			consideredForLabels[id]=new SetOfLabels();
		}
		return consideredForLabels[id].add(label);
		
	}

	private static boolean multisetEquivalent(IComposite reagents, IComposite products, IPartition partition) {
		if(reagents.isUnary() && products.isUnary()) {
			return partition.getBlockOf(reagents.getFirstReagent()).equals(partition.getBlockOf(products.getFirstReagent()));
		}
		else if(reagents.isBinary() && products.isBinary()) {
			//firstfirst & secondsecond
			boolean firstfirst = partition.getBlockOf(reagents.getFirstReagent()).equals(partition.getBlockOf(products.getFirstReagent()));
			if(firstfirst) {
				return partition.getBlockOf(reagents.getSecondReagent()).equals(partition.getBlockOf(products.getSecondReagent()));
			}
			//firstsecond & secondfirst
			else {
				return partition.getBlockOf(reagents.getFirstReagent() ).equals(partition.getBlockOf(products.getSecondReagent())) &&
					   partition.getBlockOf(reagents.getSecondReagent()).equals(partition.getBlockOf(products.getFirstReagent() ));
			}
		} 
		else {
			//unary vs binary or binary vs unary
			return false;
		}
	}

	public static List<ILabel> computeUnaryBinaryLabels(ICRN crn) {
		Set<ILabel> labelsSet = new HashSet<ILabel>();
		List<ILabel> labels = new ArrayList<ILabel>();
		labelsSet.add(EmptySetLabel.EMPTYSETLABEL);
		labels.add(EmptySetLabel.EMPTYSETLABEL);

		for (ICRNReaction reaction : crn.getReactions()) {
			if(reaction.isBinary()){
				addLabel(labelsSet, labels, reaction.getReagents().getFirstReagent());
				addLabel(labelsSet, labels, reaction.getReagents().getSecondReagent());
			}
		}
		return labels;
	}
	
	public static List<ILabel> computeUnaryBinaryLabelsForProducts(ICRN crn) {
		Set<ILabel> labelsSet = new HashSet<ILabel>();
		List<ILabel> labels = new ArrayList<ILabel>();
		labelsSet.add(EmptySetLabel.EMPTYSETLABEL);
		labels.add(EmptySetLabel.EMPTYSETLABEL);

		for (ICRNReaction reaction : crn.getReactions()) {
			IComposite products = reaction.getProducts();
			if(products.isBinary()){
				addLabel(labelsSet, labels, products.getFirstReagent());
				addLabel(labelsSet, labels, products.getSecondReagent());
			}
		}
		return labels;
	}
	
	private static void addLabel(Set<ILabel> labelSet,List<ILabel> labelList, ILabel label){
		boolean added = labelSet.add(label);
		if(added){
			labelList.add(label);
		}
	}
	
}


