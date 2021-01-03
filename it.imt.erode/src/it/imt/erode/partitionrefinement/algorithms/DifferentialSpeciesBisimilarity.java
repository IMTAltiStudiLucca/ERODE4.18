package it.imt.erode.partitionrefinement.algorithms;

import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.auxiliarydatastructures.ArrayListOfReactions;
import it.imt.erode.auxiliarydatastructures.IPartitionAndBoolean;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.label.EmptySetLabel;
import it.imt.erode.crn.label.ILabel;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SpeciesCounterField;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SpeciesCounterHandler;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SpeciesSplayBST;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SplittersGenerator;





public class DifferentialSpeciesBisimilarity {

	/**
	 * 
	 * @param crn
	 * @param labels
	 * @param partition the partition to be refined. It is not modified, but instead a new one is created and returned
	 * @param initializeCounters
	 * @param verbose
	 * @return
	 */
	public static IPartitionAndBoolean computeDSB(ICRN crn, List<ILabel> labels, IPartition partition, boolean verbose,MessageConsoleStream out, BufferedWriter bwOut,Terminator terminator){

		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"FB_old Reducing: "+crn.getName());
		}

		//CRNReducerCommandLine.printWarning("The reduceDSB command is deprecated. It refers to a previous implementation. Please usere reduceFB.");

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

		if(verbose){
			String blocks = " blocks";
			if(obtainedPartition.size()==1){
				blocks = " block";
			}
			CRNReducerCommandLine.println(out,bwOut,"Before partitioning we have "+ obtainedPartition.size() + blocks +" and "+crn.getSpecies().size()+ " species.");
		}

		long begin = System.currentTimeMillis();

		SpeciesCounterHandler speciesCounters[] = new SpeciesCounterHandler[crn.getSpecies().size()];

		//if(!crn.isUnaryProductsForm()){
		obtainedPartition=refineCRRD(crn,obtainedPartition,labels,speciesCounters,terminator);
		CRNReducerCommandLine.print(out,bwOut," (after FB_old pre-partitioning we have "+ obtainedPartition.size() +" blocks) ");

		long end = System.currentTimeMillis();
		//CRNReducerCommandLine.println(out,bwOut,"After DSB pre-partitioning we have "+ obtainedPartition.size() +" blocks. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");
		if(verbose){	
			CRNReducerCommandLine.println(out,bwOut,obtainedPartition);
		}
		/*}
		else{
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"The CRN has unary products only, and thus DSB pre-partitioning is not necessary.");
			}
		}*/

		refinePR(crn,obtainedPartition,labels,speciesCounters,terminator);
		//refinePRWhenConsideringAllLabelsAtOnce(crn,partition,labels);

		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"The final partition:");
			CRNReducerCommandLine.println(out,bwOut,obtainedPartition);
		}

		end = System.currentTimeMillis();
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"FB_old Partitioning completed. From "+ crn.getSpecies().size() +" species to "+ obtainedPartition.size() + " blocks. Time necessary: "+(end-begin)+ " (ms)");
			CRNReducerCommandLine.println(out,bwOut,"");
		}
		return new IPartitionAndBoolean(obtainedPartition,true);
	}


	/**
	 * 
	 * @param crn
	 * @param partition the partition to be refined. If we have at least a label, a new one is created and returned, otherwise the original one is returned. 
	 * @param labels
	 * @param speciesCounters 
	 * @return
	 */
	protected static IPartition refineCRRD(ICRN crn, IPartition partition, List<ILabel> labels, SpeciesCounterHandler[] speciesCounters, Terminator terminator) {
		
		//compute label-crr rates of all species, for each label
		for (ICRNReaction crnReaction : crn.getReactions()) {
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			CRNBisimulations.increaseAllCRRDOfReagents(crnReaction,speciesCounters);
		}

		// refine the initial partition according to the label-crr of each species, for each label
		for (ILabel label : labels) {
			partition = refineAccordingToComputedCRRd(partition,label,crn,speciesCounters);
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
		}
		
		return partition;
	}
	
	/**
	 * 
	 * @param partition the partition to be refined. This is not modified, but instead a new one is created.
	 * @param label
	 * @param crn
	 * @param speciesCounters 
	 * @return
	 */
	private static IPartition refineAccordingToComputedCRRd(IPartition partition, ILabel label,ICRN crn, SpeciesCounterHandler[] speciesCounters) {
		//This is the refinement of partition, computed in each iteration of the while loop 
		IPartition refinement = new Partition(crn.getSpecies().size());
		IBlock currentBlock = partition.getFirstBlock();
		while(currentBlock!=null){
			//The binary search tree used to split each block
			SpeciesSplayBST<BigDecimal, IBlock> bst = new SpeciesSplayBST<BigDecimal, IBlock>();
			for (ISpecies species : currentBlock.getSpecies()) {
				 // Insert the species "species" in the global binary search tree created for the label, so to partition each block of the current partition according to the label-crr. 
				 // This may cause the creation of a new block, in which case it is automatically added to the current partition. In any case, the reference of the species to the own block is updated   
				 // To sum up: we created a new partition (nextPartition), then for each block of partition we throw away it and we populate nextPartition with the sub-blocks of the block. At every iteration I create a new partition, which will be populated with the refinement of the previous one with respect to the current label.
				if(speciesCounters[species.getID()]==null){
					bst.put(BigDecimal.ZERO, species, refinement);
				}
				else{
					bst.put(speciesCounters[species.getID()].getCRRd(label), species, refinement);
				}
			}
			currentBlock=currentBlock.getNext();
		}
		return refinement;
	}	
	
	/**
	 * 
	 * @param crn
	 * @param partition the partition to be refined. Note that the partition is modified, thus first invoke copy if you want to preserve it.
	 * @param labels
	 * @param speciesCounters 
	 * @param terminator 
	 */
	private static void refinePR(ICRN crn, IPartition partition, List<ILabel> labels, SpeciesCounterHandler[] speciesCounters, Terminator terminator) {
		//generate candidate splitters
		//SplittersGenerator splittersGenerator = partition.getOrCreateSplitterGenerator(labels);
		SplittersGenerator splittersGenerator = partition.createSplitterGenerator(labels);
		
		//Initialize the "pr" fields to 0 of all species
		//crn.initializeAllCountersAndBST();
		CRNBisimulationsNAry.initializeAllCounters(crn.getSpecies(),speciesCounters);
		partition.initializeAllBST();
		
		ArrayListOfReactions[] reactionsToConsiderForEachSpecies =  new ArrayListOfReactions[crn.getSpecies().size()]; 
		CRNBisimulations.addToIncomingReactionsOfProducts(crn.getReactions(), reactionsToConsiderForEachSpecies);
		
		
		//int iteration=0;
		//CRNReducerCommandLine.println(out,bwOut,);
		while(splittersGenerator.hasSplittersToConsider()){
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			splitDSB(crn,partition,splittersGenerator,speciesCounters,reactionsToConsiderForEachSpecies);
			//iteration++;
			//CRNReducerCommandLine.print(" "+iteration);
		}

	}

	/**
	 * 
	 * @param crn
	 * @param partition the partition to be refined. Note that the partition is modified, thus first invoke copy if you want to preserve it.
	 * @param splittersGenerator
	 * @param speciesCounters 
	 * @param reactionsToConsiderForEachSpecies 
	 */
	private static void splitDSB(ICRN crn, IPartition partition, SplittersGenerator splittersGenerator, SpeciesCounterHandler[] speciesCounters, ArrayListOfReactions[] reactionsToConsiderForEachSpecies) {
		
		IBlock blockSPL = splittersGenerator.getBlockSpl();
		ILabel labelSPL = splittersGenerator.getLabelSpl();
		
		//Initialize the "pr" fields to 0 of all species having reactions with partners label, towards at least a species of blockSPL// This is now done once before invoking splitDSB, and then at the end of splitDSB, for the splittergenerators only
		//initializeAllLabelPRandBST(crn,labelSPL, blockSPL);

		//Set of species with at least a reaction towards the species of the splitter
		HashSet<ISpecies> splitterGenerators = new HashSet<ISpecies>();
		//compute pr[X,label,blockSPL] for all species X having at least a reaction with partners label towards blockSPL (and build the list of such species. Only their blocks can get split)
		boolean anyReactionFound = computeProductionRatesForGeneratorsOfTheSplitter(crn, labelSPL, blockSPL,splitterGenerators,speciesCounters,reactionsToConsiderForEachSpecies);
		
		if(!anyReactionFound){
			//CRNReducerCommandLine.println(out,bwOut,"No reactions considered at this iteration. I can skip this split iteration");
			splittersGenerator.generateNextSplitter();
			return;
		}

		//Set of blocks to be considered for splitting (blocks where at least a species performs a reaction with partners label towards species of blockSPL)
		HashSet<IBlock> splittedBlocks = new HashSet<IBlock>();
		boolean blockOfSPlitterHasBeenSplitted = CRNBisimulations.partitionBlocksOfGivenSpecies(partition,splitterGenerators,blockSPL,splittedBlocks,SpeciesCounterField.PR,speciesCounters);

		//Now I have to update the splitters according to the newly generated partition
		cleanPartitioAndGetNextSplitterIfNecessary(partition, splittersGenerator, blockSPL, splittedBlocks, blockOfSPlitterHasBeenSplitted);
		
		//Initialize the "pr" fields to 0 of all species having reactions with partners label, towards at least a species of blockSPL
		//crn.initializeAllCountersAndBST(splitterGenerators);
		CRNBisimulationsNAry.initializeAllCounters(splitterGenerators,speciesCounters);
	}

	public static void cleanPartitioAndGetNextSplitterIfNecessary(IPartition partition, SplittersGenerator splittersGenerator, IBlock blockSPL, HashSet<IBlock> splittedBlocks, boolean blockOfSplitterHasBeenSplitted) {
		cleanPartitioAndGetNextSplitterIfNecessary(partition, splittersGenerator, blockSPL, splittedBlocks,  blockOfSplitterHasBeenSplitted,true);
	}

	protected static void cleanPartitioAndGetNextSplitterIfNecessary(IPartition partition, SplittersGenerator splittersGenerator, IBlock blockSPL, HashSet<IBlock> splittedBlocks, boolean blockOfSplitterHasBeenSplitted, boolean updateTheSplitter) {
		
		boolean blockOfSplitHasBeenRemoved = false;
		for (IBlock splittedBlock : splittedBlocks) {
			List<IBlock> subBlocks; // = splittedBlock.getBST().getBlocks();
			if(CRNBisimulationsNAry.USETOLERANCE){
				subBlocks = splittedBlock.getBST(CRNBisimulationsNAry.getTolerance()).getBlocks();
			}
			else{
				subBlocks = splittedBlock.getBST().getBlocks();
			}
			//If I did not actually splitted the block, but I have just moved all its elements in a new one. Thus I replace the new alias block with the original one. This improves performances, because the new alias block will be considered as the old one, without having to recompute all checks already done
			if(splittedBlock.isEmpty()  && subBlocks.size() == 1){
				IBlock aliasBlock = subBlocks.get(0);
				partition.substituteAndDecreaseSize(aliasBlock,splittedBlock);
				//blockOfSPlitterHasBeenSplitted has not really been splitted
				if(blockSPL == splittedBlock){
					blockOfSplitterHasBeenSplitted = false;
				}
			}
			//Otherwise, I have actually split the block. I remove it from the partition if it is empty.
			else{
				//If the original block became empty, remove it from the partition.
				if(splittedBlock.isEmpty()){
					partition.remove(splittedBlock);
					if(blockOfSplitterHasBeenSplitted && blockSPL == splittedBlock){
						blockOfSplitHasBeenRemoved=true;
					}
				}
				else{
					//Otherwise, the original block is not empty, and thus I do not remove it from the partition. However, I have to throw away its bst.
					splittedBlock.throwAwayBST();
					splittedBlock.throwAwayBSTWithTolerance();
				}
			}
		}
		//If the block of the splitter has been removed, splittersGenerator already created the new splitter. If instead I have not removed it, I have to consider the block as a new one, and thus I have to reinizialize the labels
		if(blockOfSplitterHasBeenSplitted){
			if(!blockOfSplitHasBeenRemoved){
				splittersGenerator.currentBlockHasBeenSplitButNotRemoved();
			}
		}
		else{
			if(updateTheSplitter){
				//If the block of the splitter has not been split, I have to generate the next splitter on my own
				splittersGenerator.generateNextSplitter();
			}
		}
	}

	private static boolean computeProductionRatesForGeneratorsOfTheSplitter(
			ICRN crn, ILabel labelSPL, IBlock blockSPL, HashSet<ISpecies> splitterGenerators, SpeciesCounterHandler[] speciesCounters, ArrayListOfReactions[] reactionsToConsiderForEachSpecies) {

		boolean anyReactionConsidered = false;
		Collection<ICRNReaction> consideredReactions=null;
		for (ISpecies aSpeciesOfSplitter : blockSPL.getSpecies()) {
			//consideredReactions = aSpeciesOfSplitter.getIncomingReactions();
			ArrayListOfReactions reactions = reactionsToConsiderForEachSpecies[aSpeciesOfSplitter.getID()];
			if(reactions!=null) {
				consideredReactions=reactions.reactions;
				if(consideredReactions!=null){
					anyReactionConsidered=true;
					for (ICRNReaction incomingReaction : consideredReactions) {
						increasePROfReagents(incomingReaction,labelSPL,splitterGenerators,aSpeciesOfSplitter,speciesCounters);
					}
				}
			}
		}
		return anyReactionConsidered;
	}
	
	private static void increasePROfReagents(ICRNReaction reaction, ILabel labelSPL, HashSet<ISpecies> splitterGenerators, ISpecies targetSpecies, SpeciesCounterHandler[] speciesCounters) {

		IComposite reagents = reaction.getReagents();
		IComposite products = reaction.getProducts();
		BigDecimal rate = reaction.getRate();
		
		if(labelSPL.equals(EmptySetLabel.EMPTYSETLABEL)){
			if(reagents.isUnary()){
				BigDecimal mult = BigDecimal.valueOf(products.getMultiplicityOfSpecies(targetSpecies));
				ISpecies reagent = reagents.getFirstReagent();
				SpeciesCounterHandler.initSpeciesCounters(speciesCounters, reagent);
				speciesCounters[reagent.getID()].addToPR(rate.multiply(mult));
				splitterGenerators.add(reagent);
			}
		}
		//Otherwise the label is a species (and regards binary reactions) 
		else {
			if(reagents.isBinary()){
				ISpecies firstReagent = reagents.getFirstReagent();
				ISpecies secondReagent = reagents.getSecondReagent();
				SpeciesCounterHandler.initSpeciesCounters(speciesCounters, firstReagent);
				SpeciesCounterHandler.initSpeciesCounters(speciesCounters, secondReagent);
				if(firstReagent.equals(labelSPL)){
					BigDecimal mult = BigDecimal.valueOf(products.getMultiplicityOfSpecies(targetSpecies));
					BigDecimal toAdd = rate.multiply(mult);
					speciesCounters[secondReagent.getID()].addToPR(toAdd);
					splitterGenerators.add(secondReagent);
				}
				if(secondReagent.equals(labelSPL)){
					BigDecimal mult = BigDecimal.valueOf(products.getMultiplicityOfSpecies(targetSpecies));
					BigDecimal toAdd = rate.multiply(mult);
					speciesCounters[firstReagent.getID()].addToPR(toAdd);
					splitterGenerators.add(firstReagent);
				}
			}
		}
	}

}


