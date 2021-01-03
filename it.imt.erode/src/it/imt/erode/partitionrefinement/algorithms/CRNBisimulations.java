package it.imt.erode.partitionrefinement.algorithms;

import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.auxiliarydatastructures.ArrayListOfReactions;
import it.imt.erode.auxiliarydatastructures.IPartitionAndBoolean;
import it.imt.erode.auxiliarydatastructures.Reduction;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.label.EmptySetLabel;
import it.imt.erode.crn.label.ILabel;
import it.imt.erode.crn.label.NAryLabelBuilder;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.splittersbstandcounters.CRNBisNAryOrSENAry;
import it.imt.erode.partitionrefinement.splittersbstandcounters.ISpeciesCounterHandler;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SpeciesCounterField;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SpeciesCounterHandler;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SpeciesSplayBST;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SplittersGenerator;
import it.imt.erode.partitionrefinement.splittersbstandcounters.VectorOfBigDecimalsForEachLabel;





public class CRNBisimulations {

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
	public static IPartitionAndBoolean computeCoarsest(Reduction red,ICRN crn, IPartition partition, boolean verbose,MessageConsoleStream out, BufferedWriter bwOut, boolean printInfo, Terminator terminator){

		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,red.toString()+" Reducing: "+crn.getName());
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
		
		if(!(red.equals(Reduction.FB)||red.equals(Reduction.BB))){
			CRNReducerCommandLine.printWarning(out,bwOut,"Please invoke this method using FB or BB.  I terminate.");
			return new IPartitionAndBoolean(obtainedPartition, false);
		}
		
		//public static void printWarning(MessageConsoleStream out, BufferedWriter bwOut, String message, boolean beginWithNewLine/*,DialogType dialogType*/)

		if(verbose){
			String blocks = " blocks";
			if(obtainedPartition.size()==1){
				blocks = " block";
			}
			CRNReducerCommandLine.println(out,bwOut,"Before "+red.toString()+" partitioning we have "+ obtainedPartition.size() + blocks +" and "+crn.getSpecies().size()+ " species.");
		}

		long begin = System.currentTimeMillis();
		
		SpeciesCounterHandler speciesCounters[] = new SpeciesCounterHandler[crn.getSpecies().size()];

		if(red.equals(Reduction.FB)){
			obtainedPartition= refineCRRD(crn,obtainedPartition,speciesCounters,terminator);
			//long end = System.currentTimeMillis();
			//CRNReducerCommandLine.println(out,bwOut,"After  FB pre-partitioning we have "+ obtainedPartition.size() +" blocks. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");
			/*if(printInfo){
				CRNReducerCommandLine.print(out,bwOut," (after FB pre-partitioning we have "+ obtainedPartition.size() +" blocks in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)) ...");
			}*/
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,obtainedPartition);
			}
		}

		refine(out,bwOut,red,crn,obtainedPartition,speciesCounters,terminator);
		//refinePRWhenConsideringAllLabelsAtOnce(crn,partition,labels);

		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"The final partition:");
			CRNReducerCommandLine.println(out,bwOut,obtainedPartition);
		}

		long end = System.currentTimeMillis();
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,red+" Partitioning completed. From "+ crn.getSpecies().size() +" species to "+ obtainedPartition.size() + " blocks. Time necessary: "+(end-begin)+ " (ms)");
			CRNReducerCommandLine.println(out,bwOut,"");
		}
		return new IPartitionAndBoolean(obtainedPartition,true);
	}
	
	
	protected static void increaseAllCRRDOfReagents(ICRNReaction reaction, SpeciesCounterHandler[] speciesCounters){
		increaseAllCRRDOfReagents(reaction,false, speciesCounters);
	}
	
	protected static void increaseAllCRRDOfReagents(ICRNReaction reaction, boolean hasOneLabelOnly, SpeciesCounterHandler[] speciesCounters){
		IComposite reagents = reaction.getReagents();
		BigDecimal rate = reaction.getRate();
		if(reagents.isUnary()){
			ISpecies reagent =  reagents.getFirstReagent();
			SpeciesCounterHandler.initSpeciesCounters(speciesCounters,reagent);
			if(hasOneLabelOnly){
				speciesCounters[reagent.getID()].addToSMBCounter(rate);
			}
			else{
				speciesCounters[reagent.getID()].addToCRRD(EmptySetLabel.EMPTYSETLABEL, rate);
			}
		}
		else{
			if(reagents.isBinary()){
				//it is a binary reaction
				ISpecies firstReagent = reagents.getFirstReagent();
				ISpecies secondReagent = reagents.getSecondReagent();
				SpeciesCounterHandler.initSpeciesCounters(speciesCounters,firstReagent);
				SpeciesCounterHandler.initSpeciesCounters(speciesCounters,secondReagent);
				speciesCounters[firstReagent.getID()].addToCRRD(secondReagent, rate);
				speciesCounters[secondReagent.getID()].addToCRRD(firstReagent, rate);
			}
			else {
				throw new UnsupportedOperationException("Ternary or more reaction");
			}
		}
	}

	/**
	 * 
	 * @param crn
	 * @param partition the partition to be refined. If we have at least a label, a new one is created and returned, otherwise the original one is returned. 
	 * @param speciesCounters 
	 * @param terminator 
	 * @param labels
	 * @return
	 */
	protected static IPartition refineCRRD(ICRN crn, IPartition partition, SpeciesCounterHandler[] speciesCounters, Terminator terminator) {
		
		VectorOfBigDecimalsForEachLabel zeroVector = new VectorOfBigDecimalsForEachLabel(new HashMap<ILabel, BigDecimal>());
		boolean hasOneLabelOnly = crn.getMaxArity()==1;
		//compute label-crr rates of all species, for each label
		for (ICRNReaction crnReaction : crn.getReactions()) {
			//crnReaction.increaseAllCRRDOfReagents(hasOneLabelOnly);
			increaseAllCRRDOfReagents(crnReaction, hasOneLabelOnly,speciesCounters);
		}

		// refine the initial partition according to the {label_1,...,label_n}-crr of each species
		//This is the refinement of partition, computed in each iteration of the while loop 
		IPartition refinement = new Partition(crn.getSpecies().size());
		IBlock currentBlock = partition.getFirstBlock();
		while(currentBlock!=null){
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			//The binary search tree used to split each block
			//SpeciesSplayBST<BigDecimal, IBlock> bst = new SpeciesSplayBST<BigDecimal, IBlock>();
			if(hasOneLabelOnly){
				SpeciesSplayBST<BigDecimal, IBlock> bstForBigDecimals = new SpeciesSplayBST<>();
				for (ISpecies species : currentBlock.getSpecies()) {
					 // Insert the species "species" in the global binary search tree created for the label, so to partition each block of the current partition according to the label-crr. 
					 // This may cause the creation of a new block, in which case it is automatically added to the current partition. In any case, the reference of the species to the own block is updated   
					 // To sum up: we created a new partition (nextPartition), then for each block of partition we throw away it and we populate nextPartition with the sub-blocks of the block. At every iteration I create a new partition, which will be populated with the refinement of the previous one with respect to the current label.
					if(speciesCounters[species.getID()]==null){
						bstForBigDecimals.put(BigDecimal.ZERO, species, refinement); 
					}
					else{
						bstForBigDecimals.put(speciesCounters[species.getID()].getSMBCounter(), species, refinement);
					}
					
				}
			}
			else{
				SpeciesSplayBST<VectorOfBigDecimalsForEachLabel, IBlock> bstForVectors = new SpeciesSplayBST<>();
				
				for (ISpecies species : currentBlock.getSpecies()) {
					// Insert the species "species" in the global binary search tree created for the label, so to partition each block of the current partition according to the label-crr. 
					// This may cause the creation of a new block, in which case it is automatically added to the current partition. In any case, the reference of the species to the own block is updated   
					// To sum up: we created a new partition (nextPartition), then for each block of partition we throw away it and we populate nextPartition with the sub-blocks of the block. At every iteration I create a new partition, which will be populated with the refinement of the previous one with respect to the current label.
					
					SpeciesCounterHandler counters = speciesCounters[species.getID()]; 
					
					if(counters==null || counters.getCRRVector()==null || counters.getCRRVector().isEmpty()){
						bstForVectors.put(zeroVector, species, refinement);
					}
					else{
						bstForVectors.put(new VectorOfBigDecimalsForEachLabel(counters.getCRRVector()), species, refinement);
					}
				}
			}
			
			currentBlock=currentBlock.getNext();
		}

		return refinement;		
	}
		
	
	/**
	 * 
	 * @param out 
	 * @param red 
	 * @param crn
	 * @param partition the partition to be refined. Note that the partition is modified, thus first invoke copy if you want to preserve it.
	 * @param speciesCounters 
	 * @param terminator 
	 * @param labels
	 */
	private static void refine(MessageConsoleStream out, BufferedWriter bwOut, Reduction red, ICRN crn, IPartition partition, SpeciesCounterHandler[] speciesCounters, Terminator terminator) {
		//generate candidate splitters
		List<ILabel> fakeLabels=new ArrayList<>();
		fakeLabels.add(EmptySetLabel.EMPTYSETLABEL);
		//SplittersGenerator splittersGenerator = partition.getOrCreateSplitterGenerator(fakeLabels);
		SplittersGenerator splittersGenerator = partition.createSplitterGenerator(fakeLabels);
		//SplittersGenerator splittersGenerator = partition.getSplitterGeneratorAndCreateItIfFirstInvocation(labels);
		
		//Initialize the "pr" fields to 0 of all species
		//crn.initializeAllCountersAndBST();
		CRNBisimulationsNAry.initializeAllCounters(crn.getSpecies(),speciesCounters);
		partition.initializeAllBST();
		
		ArrayListOfReactions[] reactionsToConsiderForEachSpecies =  new ArrayListOfReactions[crn.getSpecies().size()]; 
		if(red.equals(Reduction.FB)) {
			addToIncomingReactionsOfProducts(crn.getReactions(), reactionsToConsiderForEachSpecies);
		}
		else if(red.equals(Reduction.BB)) {
			addToOutgoingReactionsOfReagents(crn.getReactions(), reactionsToConsiderForEachSpecies);
		}
		
		Integer iteration=1;
		HashMap<ICRNReaction,Integer> consideredAtIteration = new HashMap<>(crn.getReactions().size());
		while(splittersGenerator.hasSplittersToConsider()){
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			split(red,crn,partition,splittersGenerator,iteration,consideredAtIteration,speciesCounters,reactionsToConsiderForEachSpecies);
			iteration++;
			//CRNReducerCommandLine.print(" "+iteration);
			splittersGenerator.skipSplittersWhichShouldBeIgnored();
		}
		CRNReducerCommandLine.print(out,bwOut," ( "+iteration+" iterations) ");

	}


	public static void addToOutgoingReactionsOfReagents(Collection<ICRNReaction> reactions,
			ArrayListOfReactions[] reactionsToConsiderForEachSpecies) {
		for(ICRNReaction reaction : reactions) {
			addToOutgoingReactionsOfReagents(reaction,reactionsToConsiderForEachSpecies);
		}
	}


	public static void addToOutgoingReactionsOfReagents(ICRNReaction reaction,ArrayListOfReactions[] reactionsToConsiderForEachSpecies) {
		IComposite compositeReagents = reaction.getReagents();
		if(compositeReagents instanceof Composite){
//					if(addToComposite){
//						compositeReagents.addOutgoingReactions(reaction);
//					}
			for(int i=0;i<compositeReagents.getNumberOfDifferentSpecies();i++){
				ISpecies species = compositeReagents.getAllSpecies(i);
				//species.addOutgoingReactions(reaction);
				addReactionToConsider(reactionsToConsiderForEachSpecies, reaction, species);
			}
		}
		else if(compositeReagents instanceof ISpecies){
			ISpecies species = (ISpecies)compositeReagents;
			//species.addOutgoingReactions(reaction);
			addReactionToConsider(reactionsToConsiderForEachSpecies, reaction, species);
		}
		else{
			throw new UnsupportedOperationException();
		}
	}


	protected static void addToIncomingReactionsOfProducts(Collection<ICRNReaction> reactions,
			ArrayListOfReactions[] reactionsToConsiderForEachSpecies) {
		for(ICRNReaction reaction : reactions) {
			IComposite compositeProducts = reaction.getProducts();
			if(compositeProducts instanceof Composite){
//					if(addToComposite){
//						compositeReagents.addOutgoingReactions(reaction);
//					}
				for(int i=0;i<compositeProducts.getNumberOfDifferentSpecies();i++){
					ISpecies species = compositeProducts.getAllSpecies(i);
					addReactionToConsider(reactionsToConsiderForEachSpecies, reaction, species);
				}
			}
			else if(compositeProducts instanceof ISpecies){
				ISpecies species = (ISpecies)compositeProducts;
				//species.addOutgoingReactions(reaction);
				addReactionToConsider(reactionsToConsiderForEachSpecies, reaction, species);
			}
			else{
				throw new UnsupportedOperationException();
			}
		}
	}


	protected static void addReactionToConsider(ArrayListOfReactions[] reactionsToConsiderForEachSpecies,
			ICRNReaction reaction, ISpecies species) {
		ArrayListOfReactions reactions = reactionsToConsiderForEachSpecies[species.getID()];
		if(reactions==null) {
			reactions=new ArrayListOfReactions();
			reactionsToConsiderForEachSpecies[species.getID()]=reactions;
		}
		reactions.reactions.add(reaction);
	}
	
	protected static boolean partitionBlocksOfGivenSpecies(IPartition partition, HashSet<ISpecies> consideredSpecies,IBlock blockToCheckIfSplit, HashSet<IBlock> splittedBlocks, SpeciesCounterField keyField, SpeciesCounterHandler[] speciesCounters) {
	
		//If "blockToCheckIfSplit" is ANYBLOCK, I assume that it means that I have to return true if at least a block has been split. 
		boolean blocktoCheckHasBeenSplit=false;

		//split each block of partition according to the computed pr[X,labelSPL,blockSPL] values. 
		for (ISpecies splitterGenerator : consideredSpecies) {
			//If no measure has been computed for the species....
			if(speciesCounters[splitterGenerator.getID()]==null){
				continue;
			}
			SpeciesCounterHandler counters = speciesCounters[splitterGenerator.getID()];
			//If the computed measure is zero....
			if(keyField.equals(SpeciesCounterField.FRVECTOR)){
				if(counters.getFRVector()==null || counters.getFRVector().size()==0){
					continue;
				}
			}
			else if(keyField.equals(SpeciesCounterField.PRVECTOR)){
				if(counters.getPRVector()==null||counters.getPRVector().size()==0){
					continue;
				}
			}
			else if(keyField.equals(SpeciesCounterField.SMBVECTOR)){
				if(counters.getSMBCounterVector()==null||counters.getSMBCounterVector().size()==0){
					continue;
				}
			}
			//Do we need this? No. Because these are the concur/tacas bisimulations.
			/*else if(keyField.equals(SpeciesCounterField.NRVECTOR)){
				if(counters.getNRVector()==null||counters.getNRVector().size()==0){
					continue;
				}
			}*/
			else{//if(keyField.equals(SpeciesCounterField.PR)||keyField.equals(SpeciesCounterField.FR)||keyField.equals(SpeciesCounterField.CRRM)){
				if(BigDecimal.ZERO.compareTo(counters.get(keyField))==0){
					continue;
				}
			}
			
			/*if(keyField.equals(SpeciesCounterField.FRVECTOR)&&(counters.getFRVector()==null || counters.getFRVector().size()==0)){
				continue;
			}
			else if(keyField.equals(SpeciesCounterField.PRVECTOR)&&(counters.getPRVector()==null||counters.getPRVector().size()==0)){
				continue;
			}
			else //if(keyField.equals(SpeciesCounterField.PR)||keyField.equals(SpeciesCounterField.FR)||keyField.equals(SpeciesCounterField.CRRM)){
				if(BigDecimal.ZERO.compareTo(counters.get(keyField))==0){
					continue;
				}
			//}
			 */
			//TODO: maybe we can modify so that splitting is done only if the block is not a singleton
			IBlock block = partition.getBlockOf(splitterGenerator);
			//remove the species from its block (i.e., this block will remain with only species not performing reactions towards the splitter)
			block.removeSpecies(splitterGenerator);
			//Add block to the set of splitted blocks
			splittedBlocks.add(block);
			if(blockToCheckIfSplit != null && block == blockToCheckIfSplit){
				blocktoCheckHasBeenSplit=true;
			}
			/*
			 * Insert the species splitterGenerator in the tree associated to the block "block". 
			 * This method is used to split the block according to the computed generation rates. This may cause the creation of a new block, in which case it is automatically added to the current partition. 
			 * In any case, the reference of the species to the own block is updated   
			 */

			if(keyField.equals(SpeciesCounterField.PRVECTOR)){
				block.getBSTForVectors().put(new VectorOfBigDecimalsForEachLabel(counters.getPRVector()), splitterGenerator, partition);
			}
			else if(keyField.equals(SpeciesCounterField.FRVECTOR)){
				block.getBSTForVectors().put(new VectorOfBigDecimalsForEachLabel(counters.getFRVector()), splitterGenerator, partition);
			}
			else if(keyField.equals(SpeciesCounterField.SMBVECTOR)){
				block.getBSTForVectors().put(new VectorOfBigDecimalsForEachLabel(counters.getSMBCounterVector()), splitterGenerator, partition);
			}
			else{
				if(CRNBisimulationsNAry.USETOLERANCE){
					block.getBST(CRNBisimulationsNAry.getTolerance()).put(counters.get(keyField), splitterGenerator, partition);
				}
				else{
					block.getBST().put(counters.get(keyField), splitterGenerator, partition);
				}
			}
		}
		return blocktoCheckHasBeenSplit;
	}
	


	/**
	 * 
	 * @param red 
	 * @param crn
	 * @param partition the partition to be refined. Note that the partition is modified, thus first invoke copy if you want to preserve it.
	 * @param splittersGenerator
	 * @param speciesCounters 
	 * @param reactionsToConsiderForEachSpecies 
	 * @param labels 
	 */
	private static void split(Reduction red, ICRN crn, IPartition partition, SplittersGenerator splittersGenerator, Integer iteration, HashMap<ICRNReaction,Integer> consideredAtIteration,SpeciesCounterHandler[] speciesCounters, ArrayListOfReactions[] reactionsToConsiderForEachSpecies) {
		
		IBlock blockSPL = splittersGenerator.getBlockSpl();
		//ILabel labelSPL = splittersGenerator.getLabelSpl();
		
		//Initialize the "pr" fields to 0 of all species having reactions with partners label, towards at least a species of blockSPL// This is now done once before invoking splitDSB, and then at the end of splitDSB, for the splittergenerators only
		//initializeAllLabelPRandBST(crn,labelSPL, blockSPL);

		//Set of species with at least a reaction towards the species of the splitter
		LinkedHashSet<ISpecies> splitterGenerators = new LinkedHashSet<ISpecies>();
		
		boolean anyReactionFound=false;

		//If FB compute pr[X,partnerLabel,blockSPL] for all species X having at least a reaction with at least a partner partnerLabel (for all partnerLabels) towards blockSPL (and build the list of such species. Only their blocks can get split)
		//If BB compute fr[X,blockLabel,blockSPL] for all species X and all blocks blockLabel  (and build the list of species having it positive. Only their blocks can get split)
		anyReactionFound = computeMeasuresUsedToSplitWithRespectToTheSplitter(red,crn, blockSPL,splitterGenerators,partition,iteration,consideredAtIteration,speciesCounters,reactionsToConsiderForEachSpecies);
		
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
		SpeciesCounterField splitWRT = (red.equals(Reduction.FB))? SpeciesCounterField.PRVECTOR : SpeciesCounterField.FRVECTOR;
		if(hasOnlyUnaryReactions){
			splitWRT = (red.equals(Reduction.FB))? SpeciesCounterField.PR : SpeciesCounterField.FR;
		}
		boolean blockOfSPlitterHasBeenSplitted = partitionBlocksOfGivenSpecies(partition,splitterGenerators,blockSPL,splittedBlocks,splitWRT,speciesCounters);

		//blockSPL.setHasBeenAlreadyUsedAsSplitter(true);
		
		//Now I have to update the splitters according to the newly generated partition
		cleanPartitioAndGetNextSplitterIfNecessary(partition, splittersGenerator, blockSPL, splittedBlocks, blockOfSPlitterHasBeenSplitted,hasOnlyUnaryReactions);
		
		
		//Initialize the "pr" fields to 0 of all species having reactions with partners label, towards at least a species of blockSPL
		//crn.initializeAllCountersAndBST(splitterGenerators);
		CRNBisimulationsNAry.initializeAllCounters(splitterGenerators,speciesCounters);
	}

	protected static void cleanPartitioAndGetNextSplitterIfNecessary(IPartition partition, SplittersGenerator splittersGenerator, IBlock blockSPL, HashSet<IBlock> splittedBlocks, boolean blockOfSplitterHasBeenSplitted,boolean hasOnlyUnaryReactions) {
		cleanPartitioAndGetNextSplitterIfNecessary(partition, splittersGenerator, blockSPL, splittedBlocks,  blockOfSplitterHasBeenSplitted, hasOnlyUnaryReactions,true);
	}

	protected static void cleanPartitioAndGetNextSplitterIfNecessary(IPartition partition, SplittersGenerator splittersGenerator, IBlock blockSPL, HashSet<IBlock> splittedBlocks, boolean blockOfSplitterHasBeenSplitted,boolean hasOnlyUnaryReactions, boolean updateTheSplitter) {
		
		boolean blockOfSplitHasBeenRemoved = false;
		//boolean blockOfSplitterBecameItsBiggestSubBlock = false;
		
		for (IBlock splittedBlock : splittedBlocks) {
			List<IBlock> subBlocks = null;
			if(hasOnlyUnaryReactions){
				//AAA
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
					//AAA
					//biggestSubBlock = splittedBlock.getBST().getBiggestSubBlock();
					if(CRNBisimulationsNAry.USETOLERANCE){
						biggestSubBlock = splittedBlock.getBST(CRNBisimulationsNAry.getTolerance()).getBiggestSubBlock();
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
					//if((!splittedBlock.canBeUsedAsSplitter()) || blockSPL ==splittedBlock){
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
						//AAA
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

	private static boolean computeMeasuresUsedToSplitWithRespectToTheSplitter(
			Reduction red, ICRN crn, IBlock blockSPL, HashSet<ISpecies> splitterGenerators,IPartition partition, Integer iteration, HashMap<ICRNReaction,Integer> consideredAtIteration, SpeciesCounterHandler[] speciesCounters, ArrayListOfReactions[] reactionsToConsiderForEachSpecies) {

		boolean hasOneLabelOnly = crn.getMaxArity()==1;
		boolean anyReactionConsidered = false;
		for (ISpecies aSpeciesOfSplitter : blockSPL.getSpecies()) {
			//First option: all incoming reactions stay in a unique list
			ArrayListOfReactions reactions = reactionsToConsiderForEachSpecies[aSpeciesOfSplitter.getID()];
			if(reactions!=null) {
				if(red.equals(Reduction.FB)){
					//boolean anyReactionConsidered2 = computeProductionRateForGivenReactions(splitterGenerators, aSpeciesOfSplitter.getIncomingReactions(), aSpeciesOfSplitter,hasOneLabelOnly,speciesCounters);
					boolean anyReactionConsidered2 = computeProductionRateForGivenReactions(splitterGenerators, reactions.reactions, aSpeciesOfSplitter,hasOneLabelOnly,speciesCounters);
					anyReactionConsidered = anyReactionConsidered2 || anyReactionConsidered;
				}
				else{
					//boolean anyReactionConsidered2 = computeFluxRateForGivenReactions(splitterGenerators, aSpeciesOfSplitter.getOutgoingReactions(), aSpeciesOfSplitter,partition,hasOneLabelOnly,iteration,consideredAtIteration,speciesCounters);
					boolean anyReactionConsidered2 = computeFluxRateForGivenReactions(splitterGenerators, reactions.reactions, aSpeciesOfSplitter,partition,hasOneLabelOnly,iteration,consideredAtIteration,speciesCounters);
					anyReactionConsidered = anyReactionConsidered2 || anyReactionConsidered;
				}
			}
		}
		return anyReactionConsidered;
	}

	private static boolean computeProductionRateForGivenReactions(HashSet<ISpecies> splitterGenerators,Collection<ICRNReaction> consideredReactions,ISpecies aSpeciesOfSplitter, boolean hasOneLabelOnly, SpeciesCounterHandler[] speciesCounters) {
		boolean anyReactionConsidered=false;
		if(consideredReactions!=null){
			anyReactionConsidered=true;
			for (ICRNReaction incomingReaction : consideredReactions) {
				if(incomingReaction.getRate().compareTo(BigDecimal.ZERO)!=0){
					increasePROfReagents(incomingReaction,splitterGenerators,aSpeciesOfSplitter,hasOneLabelOnly,speciesCounters);
				}
			}
		}
		return anyReactionConsidered;
	}
	
	
	
	private static void increasePROfReagents(ICRNReaction reaction, HashSet<ISpecies> splitterGenerators, ISpecies targetSpecies,boolean onlyOneLabel, SpeciesCounterHandler[] speciesCounters) {
		BigDecimal mult = BigDecimal.valueOf(reaction.getProducts().getMultiplicityOfSpecies(targetSpecies));
		mult = mult.multiply(reaction.getRate());
		
		IComposite reagents = reaction.getReagents();
		
		if(reagents.isUnary()){	
			ISpecies reagent = reagents.getFirstReagent();
			SpeciesCounterHandler.initSpeciesCounters(speciesCounters, reagent);
			if(onlyOneLabel){
				speciesCounters[reagent.getID()].addToPR(mult);
			}
			else{
				speciesCounters[reagent.getID()].addToPR(EmptySetLabel.EMPTYSETLABEL,mult);
			}
			
			splitterGenerators.add(reagent);
		}
		//Otherwise it is a binary reaction (and hence I cannot have just unary labels)
		else{
			ISpecies firstReagent = reagents.getFirstReagent();
			ISpecies secondReagent = reagents.getSecondReagent();
			SpeciesCounterHandler.initSpeciesCounters(speciesCounters, firstReagent);
			SpeciesCounterHandler.initSpeciesCounters(speciesCounters, secondReagent);
			speciesCounters[secondReagent.getID()].addToPR(firstReagent,mult);
			splitterGenerators.add(secondReagent);
			speciesCounters[firstReagent.getID()].addToPR(secondReagent,mult);
			splitterGenerators.add(firstReagent);
		}
	}
	
	private static boolean computeFluxRateForGivenReactions(HashSet<ISpecies> splitterGenerators,Collection<ICRNReaction> consideredReactions,ISpecies aSpeciesOfSplitter,IPartition partition, boolean hasOneLabelOnly, Integer iteration,  HashMap<ICRNReaction,Integer> consideredAtIteration, SpeciesCounterHandler[] speciesCounters) {
		boolean anyReactionConsidered=false;
		if(consideredReactions!=null){
			anyReactionConsidered=true;
			for (ICRNReaction outgoingReaction : consideredReactions) {
				//This if is to account for the alpha/alpha' of the definition of splitterBB
				if(!iteration.equals(consideredAtIteration.get(outgoingReaction))){
					consideredAtIteration.put(outgoingReaction, iteration);
					if(outgoingReaction.getRate().compareTo(BigDecimal.ZERO)!=0){
						updateFR(outgoingReaction,splitterGenerators, partition, aSpeciesOfSplitter,hasOneLabelOnly,speciesCounters,null);
					}
				}
			}
		}
		return anyReactionConsidered;
	}
		
	
	protected static void updateFR(ICRNReaction reaction, HashSet<ISpecies> splitterGenerators, IPartition partition,ISpecies sourceSpecies, boolean hasOneLabelOnly, ISpeciesCounterHandler[] speciesCounters, HashMap<ISpecies,ISpeciesCounterHandler> speciesCountersHM){

		BigDecimal rate = reaction.getRate();
		IComposite reagents = reaction.getReagents();
		IComposite products = reaction.getProducts();
		
		BigDecimal negativeRate = BigDecimal.ZERO.subtract(rate);
		BigDecimal consideredRate = rate;

		ILabel blockLabel=null;
		
		if(reagents.isUnary()){
			ISpecies reagent = reagents.getFirstReagent();
			ISpeciesCounterHandler currentSpeciesCounter =CRNBisimulationsNAry.getOrAddSpeciesCounterHandler(speciesCounters, speciesCountersHM, reagent,CRNBisNAryOrSENAry.FEBE);
			//SpeciesCounterHandler.initSpeciesCounters(speciesCounters, reagent);
			if(hasOneLabelOnly){
				//speciesCounters[reagent.getID()].addToFR(negativeRate);
				currentSpeciesCounter.addToFR(negativeRate);
			}
			else{
				//speciesCounters[reagent.getID()].addToFR(EmptySetLabel.EMPTYSETLABEL,negativeRate);
				currentSpeciesCounter.addToFR(EmptySetLabel.EMPTYSETLABEL,negativeRate);
				blockLabel= EmptySetLabel.EMPTYSETLABEL;
			}
			splitterGenerators.add(reagent);
		}
		//If it is a binary reaction
		else if(reagents.isBinary()){
			ISpecies firstReagent = reagents.getFirstReagent();
			ISpecies secondReagent = reagents.getSecondReagent();
			//SpeciesCounterHandler.initSpeciesCounters(speciesCounters, firstReagent);
			ISpeciesCounterHandler firstSpeciesCounter =CRNBisimulationsNAry.getOrAddSpeciesCounterHandler(speciesCounters, speciesCountersHM, firstReagent,CRNBisNAryOrSENAry.FEBE);
			//SpeciesCounterHandler.initSpeciesCounters(speciesCounters, secondReagent);
			ISpeciesCounterHandler secondSpeciesCounter =CRNBisimulationsNAry.getOrAddSpeciesCounterHandler(speciesCounters, speciesCountersHM, secondReagent,CRNBisNAryOrSENAry.FEBE);
			
			/*if((!firstReagent.equals(secondReagent)) && (partition.getBlockOf(firstReagent).equals(partition.getBlockOf(secondReagent)))){
				negativeRate=negativeRate.divide(BDTWO);
				consideredRate=consideredRate.divide(BDTWO);
			}*/
			
			if(firstReagent.equals(sourceSpecies)){
				//blockLabel= partition.getBlockOf(secondReagent).;
				blockLabel= partition.getBlockOf(secondReagent).getRepresentative();
			}
			else{
				//blockLabel= partition.getBlockOf(firstReagent);
				blockLabel= partition.getBlockOf(firstReagent).getRepresentative();
			}
			//labelBlocks.add((IBlock)blockLabel);
			
			
			//speciesCounters[firstReagent.getID()].addToFR(blockLabel,negativeRate);
			firstSpeciesCounter.addToFR(blockLabel,negativeRate);
			//speciesCounters[secondReagent.getID()].addToFR(blockLabel,negativeRate);
			secondSpeciesCounter.addToFR(blockLabel,negativeRate);
			splitterGenerators.add(firstReagent);
			splitterGenerators.add(secondReagent);
		}
		//Otherwise it is an nary reaction, (n>=3)
		else{
			/*int count =0;
			for(int i=0;i<reagents.getAllSpecies().length;i++){
				if(partition.getBlockOf(sourceSpecies).equals(partition.getBlockOf(reagents.getAllSpecies()[i]))){
					count++;
				}
			}
			BigDecimal countBD=BigDecimal.valueOf(count);
			negativeRate=negativeRate.divide(countBD,scale,rm);
			consideredRate=consideredRate.divide(countBD,scale,rm);*/
			
			NAryLabelBuilder nAryBlockLabelBuilder = new NAryLabelBuilder((Composite)reagents);
			//int posOfSplitterSpecies = Arrays.binarySearch(reagents.getAllSpecies(), sourceSpecies);
			int posOfSplitterSpecies = reagents.getPosOfSpecies(sourceSpecies);
			nAryBlockLabelBuilder.setSpeciesToDecrease(posOfSplitterSpecies);
			blockLabel = nAryBlockLabelBuilder.getObtainedBlockLabel(partition);
			nAryBlockLabelBuilder.resetSpeciesToDecrease();
			for(int i=0;i<reagents.getNumberOfDifferentSpecies();i++){
				ISpecies reagent = reagents.getAllSpecies(i);
				//SpeciesCounterHandler.initSpeciesCounters(speciesCounters, reagent);
				ISpeciesCounterHandler currentSpeciesCounter =CRNBisimulationsNAry.getOrAddSpeciesCounterHandler(speciesCounters, speciesCountersHM, reagent,CRNBisNAryOrSENAry.FEBE);
				splitterGenerators.add(reagent);
				BigDecimal multiplicity = BigDecimal.valueOf(reagents.getMultiplicities(i));
				//speciesCounters[reagent.getID()].addToFR(blockLabel,negativeRate.multiply(multiplicity));
				currentSpeciesCounter.addToFR(blockLabel,negativeRate.multiply(multiplicity));
			}
			//throw new UnsupportedOperationException("THIS DOES NOT WORK. BECAUSE WE COUNT TOO MANY TIMES REACTIONS WITH DIFFERENT SPECIES IN THE SAME BLOCK (ONCE PER SUCH OF THESE SPECIES). WE HAVE TO NORMALIZE AS DONE FOR THE BINARY CASE");
		}
		
		if(products.isUnary()){
			ISpecies product = products.getFirstReagent();
			//SpeciesCounterHandler.initSpeciesCounters(speciesCounters, product);
			ISpeciesCounterHandler currentSpeciesCounter =CRNBisimulationsNAry.getOrAddSpeciesCounterHandler(speciesCounters, speciesCountersHM, product,CRNBisNAryOrSENAry.FEBE);

			BigDecimal inc = consideredRate;//rate.multiply(BigDecimal.ONE);
			if(hasOneLabelOnly){
				//speciesCounters[product.getID()].addToFR(inc);
				currentSpeciesCounter.addToFR(inc);
			}
			else{
				//speciesCounters[product.getID()].addToFR(blockLabel,inc);
				currentSpeciesCounter.addToFR(blockLabel,inc);
			}
			splitterGenerators.add(product);
		}
		else{
			for(int i=0;i<products.getNumberOfDifferentSpecies();i++){
				ISpecies product = products.getAllSpecies(i);
				//SpeciesCounterHandler.initSpeciesCounters(speciesCounters, product);
				ISpeciesCounterHandler currentSpeciesCounter =CRNBisimulationsNAry.getOrAddSpeciesCounterHandler(speciesCounters, speciesCountersHM, product,CRNBisNAryOrSENAry.FEBE);
				BigDecimal inc = consideredRate.multiply(BigDecimal.valueOf(products.getMultiplicities(i)));
				if(hasOneLabelOnly){
					currentSpeciesCounter.addToFR(inc);
				}
				else{
					currentSpeciesCounter.addToFR(blockLabel,inc);
				}
				splitterGenerators.add(product);
			}
		}
	}

	
	
	/*private static void refinePRWhenConsideringAllLabelsAtOnce(ICRN crn, IPartition partition, List<ILabel> labels) {
		//generate candidate splitters
		SplittersGenerator splittersGenerator = partition.getSplitterGeneratorAndCreateItIfFirstInvocation(labels);

		//Initialize the "pr" fields to 0 of all species
		initializeAllLabelPRandBST(crn);

		while(splittersGenerator.getBlockSpl()!=null){
			splitDSBWhenConsideringAllLabelsAtOnce(crn,partition,splittersGenerator);
		}

	}

	private static void splitDSBWhenConsideringAllLabelsAtOnce(ICRN crn, IPartition partition, SplittersGenerator splittersGenerator) {

		IBlock blockSPL = splittersGenerator.getBlockSpl();

		//Initialize the "pr" fields to 0 of all species having reactions with partners label, towards at least a species of blockSPL// This is now done once before invoking splitDSB, and then at the end of splitDSB, for the splittergenerators only
		//initializeAllLabelPRandBST(crn,labelSPL, blockSPL);

		//Set of species with at least a reaction towards the species of the splitter
		HashSet<ISpecies> splitterGenerators = new HashSet<ISpecies>();
		//compute pr[X,label,blockSPL] for all species X having at least a reaction with partners label towards blockSPL (and build the list of such species. Only their blocks can get split). For all labels
		Collection<ILabel> consideredLabels = computeProductionRatesForGeneratorsOfTheSplitterWhenConsideringAllLabelsAtOnce(crn, blockSPL,splitterGenerators);

		//Set of blocks to be considered for splitting (blocks where at least a species performs a reaction with partners label towards species of blockSPL), for all labels
		HashSet<IBlock> splittedBlocks = new HashSet<IBlock>();
		boolean blockOfSPlitterHasBeenSplitted = partitionBlocksOfSplitterGeneratorsWhenConsideringAllLabelsAtOnce(partition, splitterGenerators, blockSPL, consideredLabels, splittedBlocks);

		//Now I have to update the splitters according to the newly generated partition
		cleanPartitioAndGetNextSplitterIfNecessarynWhenConsideringAllLabelsAtOnce(partition, splittersGenerator, blockSPL, splittedBlocks, blockOfSPlitterHasBeenSplitted);

		//Initialize the "pr" fields to 0 of all species having reactions with partners label, towards at least a species of blockSPL
		initializeAllLabelPRandBST(splitterGenerators);
	}



	private static boolean partitionBlocksOfSplitterGeneratorsWhenConsideringAllLabelsAtOnce(IPartition partition, HashSet<ISpecies> splitterGenerators,
			IBlock blockSPL, Collection<ILabel> consideredLabels, HashSet<IBlock> splittedBlocks) {

		boolean blockOfSplitterHasBeenSplitted=false;

		for (ILabel label : consideredLabels) {
			//split each block of partition according to the computed pr[X,labelSPL,blockSPL] values. 
			for (ISpecies splitterGenerator : splitterGenerators) {
				IBlock block = splitterGenerator.getBlock();
				//remove the species from its block (i.e., this block will remain with only species not performing reactions towards the splitter)
				block.removeSpecies(splitterGenerator);
				//Add block to the set of splitted blocks
				splittedBlocks.add(block);
				if(blockSPL == block){
					blockOfSplitterHasBeenSplitted=true;
				}
				
				 // Insert the species splitterGenerator in the tree associated to the block "block". 
				 // This method is used to split the block according to the computed generation rates. This may cause the creation of a new block, in which case it is automatically added to the current partition. 
				 // In any case, the reference of the species to the own block is updated   
				block.getBST().put(splitterGenerator.getCRRd(label), splitterGenerator, partition);
			}
		}

		return blockOfSplitterHasBeenSplitted;
	}

	private static Collection<ILabel> computeProductionRatesForGeneratorsOfTheSplitterWhenConsideringAllLabelsAtOnce(
			ICRN crn, IBlock blockSPL, HashSet<ISpecies> splitterGenerators) {

		Collection<ICRNReaction> consideredReactions=null;
		Collection<ILabel> consideredLabels= new LinkedHashSet<ILabel>();
		for (ISpecies aSpeciesOfSplitter : blockSPL.getSpecies()) {
			consideredReactions = aSpeciesOfSplitter.getIncomingReactions();
			if(consideredReactions!=null){
				for (ICRNReaction incomingReaction : consideredReactions) {
					incomingReaction.increasePROfReagentsWhenConsideringAllLabelsAtOnce(splitterGenerators,aSpeciesOfSplitter,consideredLabels);
				}
			}
		}
		return consideredLabels;
	}

	protected static void cleanPartitioAndGetNextSplitterIfNecessarynWhenConsideringAllLabelsAtOnce(IPartition partition, SplittersGenerator splittersGenerator, IBlock blockSPL, HashSet<IBlock> splittedBlocks, boolean blockOfSplitterHasBeenSplitted) {

		for (IBlock splittedBlock : splittedBlocks) {
			if(splittedBlock.isEmpty()){
				partition.remove(splittedBlock);
			}
			else{
				//Otherwise, the original block is not empty, and thus I do not remove it from the partition. However, I have to throw away its bst.
				splittedBlock.throwAwayBST();
			}
		}
		//If the block of the splitter has been removed, splittersGenerator already created the new splitter. If instead I have not removed it, I have to consider the block as a new one, and thus I have to reinizialize the labels
		if(!blockOfSplitterHasBeenSplitted){
			//If the block of the splitter has been split, I have to generate the next splitter on my own. Since we are considering all labels at once, I go to the next block
			splittersGenerator.shiftToNextBlockWhenConsideringAllLabelsAtOnce();
		}
	}*/

}


