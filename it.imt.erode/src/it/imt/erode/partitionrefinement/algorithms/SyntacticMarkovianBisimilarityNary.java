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
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.implementations.CRN;
import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.label.EmptySetLabel;
import it.imt.erode.crn.label.ILabel;
import it.imt.erode.crn.label.NAryLabelBuilder;
import it.imt.erode.partition.implementations.CompositePartition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.ICompositeBlock;
import it.imt.erode.partition.interfaces.ICompositePartition;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.splittersbstandcounters.CRNBisNAryOrSENAry;
import it.imt.erode.partitionrefinement.splittersbstandcounters.HowToSplitCompositeBlocks;
import it.imt.erode.partitionrefinement.splittersbstandcounters.ISpeciesCounterHandler;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SpeciesCounterField;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SpeciesCounterHandler;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SplittersGeneratorSMB;

public class SyntacticMarkovianBisimilarityNary {

	public static final HowToSplitCompositeBlocks HOWTOSPLITCOMPOSITEBLOCKS=HowToSplitCompositeBlocks.ComputeBlockMultiplicitiesAtNeed;;
	//public static final HowToSplitCompositeBlocks HOWTOSPLITCOMPOSITEBLOCKS=HowToSplitCompositeBlocks.UseStoredLabelsBlockByBlock;
	
	/*public static boolean checkIfItPartitionIsMSB(ICRN crn, List<ILabel> labels, IPartition partition){

		int i =0;
		//Compute the blocks of products
		ICompositePartition compositesPartition = new CompositePartition(crn.getProducts(),partition);
		//consider each upBlock as target
		ICompositeBlock current = compositesPartition.getFirstBlock();
		while(current!=null){
		//for (ICompositeBlock productsBlock : compositesPartition.getBlocks()) {
			//for the current upBlock target, consider each label
			for (ILabel label : labels) {
				//Initialize all counters of each species
				for (ISpecies species : crn.getSpecies()) {
					species.initializeAllCounters();
				}
				//compute conditional reaction rate crr[sourceSpecies,blockTarget,label] for all species
				computeConditionalRatesTowardsTheSplitterBlock(crn, label, current,null);
				boolean speciesOfEachBlockHaveSameGenerationRate = partition.checkIfSpeciesOfEachBlockHaveSameField(String.valueOf(i),SpeciesCounterField.CRRM);
				if(!speciesOfEachBlockHaveSameGenerationRate){
					return false;
				}
			}
			current=current.getNext();
			i++;
		}
		return true;
	}*/
	
	/*public static IPartitionAndBoolean computeMSB(ICRN crn, List<ILabel> labels, IPartition partition, boolean verbose,MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator){
		ICompositePartition compositesPartition = new CompositePartition(crn.getProducts(),partition);
		return computeMSB(crn, labels, partition, compositesPartition, verbose,out, terminator);
	}*/
	
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
	public static IPartitionAndBoolean computeSE(ICRN crn, /*List<ILabel> labels,*/ IPartition partition, /*ICompositePartition compositePartition,*/ boolean verbose,MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator, IMessageDialogShower msgDialogShower, /*boolean addSelfLoops,*/ boolean halveRatesOfHomeoReactions) throws IOException{
		
		List<ILabel> fakeLabels=new ArrayList<>();
		fakeLabels.add(EmptySetLabel.EMPTYSETLABEL);
		
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"SE Reducing: "+crn.getName());
		}
		
		IPartition obtainedPartition = partition.copy();
		
//		if(!crn.isElementary()){
//			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it is not an elementary CRN (i.e., it has ternary or more reactions). I terminate.");
//			return new IPartitionAndBoolean(obtainedPartition, false);
//		}
//		else 
		if(!crn.isMassAction()){
			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it is not a mass action CRN (i.e., it has reactions with arbitrary rates). I terminate.");
			return new IPartitionAndBoolean(obtainedPartition, false);
		}
		else if(crn.isSymbolic()){
			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it contains symbolic parameters (i.e. parameters with no acutal value assigned). I terminate.");
			return new IPartitionAndBoolean(obtainedPartition, false);
		}
		else if(crn.algebraicSpecies()>0){
			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it contains algebraic variables. I terminate.");
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
		
		SpeciesCounterHandler speciesCounters[] = null;//new SpeciesCounterHandler[crn.getSpecies().size()];
		HashMap<ISpecies, ISpeciesCounterHandler> speciesCountersHM = new HashMap<>();
		
		ICRN crnNew = crn;
//		if(addSelfLoops){
//			crnNew = addSelfLoops(crn, out, bwOut);
//		}
		if(halveRatesOfHomeoReactions) {
			crnNew = CRN.halveRatesOfHomeoReactions(crnNew, out, bwOut);
		}
		CRNReducerCommandLine.print(out,bwOut," (creating initial partition of products) ");
		ICompositePartition compositesPartition = new CompositePartition(crnNew.computeSetOfProducts(),partition);
		//CRNReducerCommandLine.print(out,bwOut," (refining up to SMB) ");
		refineSE(crnNew,obtainedPartition,compositesPartition,fakeLabels,speciesCounters,speciesCountersHM,terminator,out,bwOut);
		
		/*
		ICRN crnNew = new CRN(crn.getName(),crn.getMath(),out);
		for (ISpecies species : crn.getSpecies()) {
			crnNew.addSpecies(species);
		}
		for(IComposite reagents : crn.getReagents()){
			crnNew.addReagent(reagents);
		}
		for(IComposite products : crn.getProducts()){
			crnNew.addProduct(products);
		}		
		for (String param : crn.getParameters()) {
			int space = param.indexOf(' ');
			String paramName = param.substring(0, space);
			String paramExpr = param.substring(space,param.length());
			crnNew.addParameter(paramName, paramExpr);
		}
		HashMap<IComposite, BigDecimal> outGoingRates = new HashMap<IComposite, BigDecimal>(crnNew.getReagents().size());
		for (ICRNReaction reaction : crn.getReactions()) {
			crnNew.addReaction(reaction);
			
			IComposite reagents = reaction.getReagents();
			BigDecimal bd = outGoingRates.get(reagents);
			if(bd==null){
				outGoingRates.put(reagents,reaction.getRate());
			}
			else{
				outGoingRates.put(reagents,bd.add(reaction.getRate()));
			}
		}
		for (Entry<IComposite, BigDecimal> pair : outGoingRates.entrySet()) {
			IComposite reagents = pair.getKey();
			BigDecimal minusTotalOut = BigDecimal.ZERO.subtract(pair.getValue()); 
			IComposite products = crnNew.addProductIfNew(reagents);
			ICRNReaction totalOutReaction = new CRNReaction(minusTotalOut, reagents, products, minusTotalOut.toPlainString());
			CRNImporter.addReaction(crnNew, reagents, products, totalOutReaction);
		}
		
		ICompositePartition compositesPartition = new CompositePartition(crnNew.getProducts(),partition);
		refineCRRM(crnNew,obtainedPartition,compositesPartition,labels,speciesCounters,terminator);*/
		
		
		/*
		ICompositePartition compositesPartition = new CompositePartition(crn.getProducts(),partition);
		refineCRRM(crn,obtainedPartition,compositesPartition,labels,speciesCounters,terminator);
		*/
		
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"The final partition:");
			CRNReducerCommandLine.println(out,bwOut,obtainedPartition);
		}
		
		long end = System.currentTimeMillis();
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"SE Partitioning completed. From "+ crn.getSpecies().size() +" species to "+ obtainedPartition.size() + " blocks. Time necessary: "+(end-begin)+ " (ms)");
			CRNReducerCommandLine.println(out,bwOut,"");
		}
		return new IPartitionAndBoolean(obtainedPartition, true);
	}
	
	
	/*private static void refineCRRM(ICRN crn, IPartition partition, List<ILabel> labels, SpeciesCounterHandler[] speciesCounters, Terminator terminator) {
		ICompositePartition compositesPartition = new CompositePartition(crn.getProducts(),partition);
		refineCRRM(crn, partition, compositesPartition, labels, speciesCounters, terminator);
	}*/
	
	private static void refineSE(ICRN crn, IPartition partition, ICompositePartition compositesPartition, List<ILabel> fakeLabels, SpeciesCounterHandler[] speciesCounters, HashMap<ISpecies, ISpeciesCounterHandler> speciesCountersHM, Terminator terminator, MessageConsoleStream out, BufferedWriter bwOut) {
		
		//generate candidate splitters
		SplittersGeneratorSMB splittersGenerator = compositesPartition.getSplitterGeneratorAndCreateItIfFirstInvocation(fakeLabels);

		//Initialize all counter fields and the bst of the species
		if(speciesCounters!=null) {
			CRNBisimulationsNAry.initializeAllCounters(crn.getSpecies(),speciesCounters);
		}
		partition.initializeAllBST();

		CRNReducerCommandLine.print(out,bwOut,"\n\tScanning the reactions once to pre-compute informations necessary to the partitioning ... ");
		long begin = System.currentTimeMillis();
		HashMap<IComposite, ArrayList<ICRNReaction>> reactionsToConsiderForEachComposite=new HashMap<>(); 
		SyntacticMarkovianBisimilarityOLD.addToIncomingReactionsOfProducts(crn,reactionsToConsiderForEachComposite);
		long end = System.currentTimeMillis();
		CRNReducerCommandLine.print(out,bwOut,"completed in "+ String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0))+" (s)");
		
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
				splitSE(crn,partition,splittersGenerator,compositesPartition,true,iteration,speciesCounters,speciesCountersHM,reactionsToConsiderForEachComposite);
				if(speciesCountersHM!=null) {
					speciesCountersHM=new HashMap<>();
				}
			}
			iteration++;
			//CRNReducerCommandLine.println(out,bwOut,"iteration "+iteration+" blocks:"+partition.size());
		}
	}
	
	private static void splitSE(ICRN crn, IPartition partition, SplittersGeneratorSMB splittersGenerator, ICompositePartition compositesPartition, boolean refineUpBlocksAfterAnySplit, int iteration, SpeciesCounterHandler[] speciesCounters, HashMap<ISpecies,ISpeciesCounterHandler> speciesCountersHM, HashMap<IComposite,ArrayList<ICRNReaction>> reactionsToConsiderForEachComposite) {
		
		ICompositeBlock blockSPL = splittersGenerator.getBlockSpl();
		
		boolean hasOnlyUnaryReactions = crn.getMaxArity()==1;
		//ILabel labelSPL = splittersGenerator.getLabelSpl();
		
		//Initialize the "pr" fields to 0 of all species having reactions with partners label, towards at least a species of blockSPL// This is now done once before invoking splitDSB, and then at the end of splitDSB, for the splittergenerators only
		//initializeAllLabelPRandBST(crn,labelSPL, blockSPL);

		//Set of species with at least a reaction towards the species of the splitter
		HashSet<ISpecies> splitterGenerators = new HashSet<ISpecies>();
		//compute pr[X,label,blockSPL] for all species X having at least a reaction with partners label towards blockSPL (and build the list of such species. Only their blocks can get split)
		boolean anyReactionFound = computeConditionalRatesTowardsTheSplitterBlock(crn, /*labelSPL,*/ blockSPL,splitterGenerators,speciesCounters,speciesCountersHM,hasOnlyUnaryReactions,reactionsToConsiderForEachComposite,partition);
		
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
		CRNBisimulationsNAry.partitionBlocksOfGivenSpecies(partition,splitterGenerators,null,splittedBlocks,splitWRT,speciesCounters,speciesCountersHM);
		
		//Now I have to update the splitters according to the newly generated partition
		//cleanPartitioAndGetNextSplitterIfNecessary(partition, splittersGenerator, splittedBlocks, compositesPartition,refineUpBlocksAfterAnySplit);
		cleanPartitioAndGetNextSplitterIfNecessaryFocusingOnTheCurrentCompBlockOnly(partition, splittersGenerator, splittedBlocks, compositesPartition,blockSPL,iteration,hasOnlyUnaryReactions);

		if(speciesCounters!=null) {
			//Initialize the "pr" fields to 0 of all species having reactions with partners label, towards at least a species of blockSPL
			CRNBisimulationsNAry.initializeAllCounters(splitterGenerators,speciesCounters);
		}
		else {
			//I do it in split for speciesCountersHM
		}
		
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
			switch (SyntacticMarkovianBisimilarityNary.HOWTOSPLITCOMPOSITEBLOCKS) {
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

	private static boolean multisetEquivalent(IComposite reagents, IComposite products, IPartition partition) {
		if(reagents.isUnary()) {
			if(products.isUnary()) {
				return partition.getBlockOf(reagents.getFirstReagent()).equals(partition.getBlockOf(products.getFirstReagent()));
			}
			else {
				return false;
			}
		}
		else if(reagents.isBinary()) {
			if(products.isBinary()) {
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
				return false;
			}
		}
		else {
			//Both reagents and products are ternary or more
			HashMap<IBlock, Integer> reagentsBlocks=new HashMap<>(reagents.getNumberOfDifferentSpecies());
			computeBlocks(reagents, partition, reagentsBlocks);
			HashMap<IBlock, Integer> productsBlocks=new HashMap<>(products.getNumberOfDifferentSpecies());
			computeBlocks(products, partition, productsBlocks);
			return reagentsBlocks.equals(productsBlocks);
		}
		
//		if(reagents.isUnary() && products.isUnary()) {
//			return partition.getBlockOf(reagents.getFirstReagent()).equals(partition.getBlockOf(products.getFirstReagent()));
//		}
//		else if(reagents.isBinary() && products.isBinary()) {
//			//firstfirst & secondsecond
//			boolean firstfirst = partition.getBlockOf(reagents.getFirstReagent()).equals(partition.getBlockOf(products.getFirstReagent()));
//			if(firstfirst) {
//				return partition.getBlockOf(reagents.getSecondReagent()).equals(partition.getBlockOf(products.getSecondReagent()));
//			}
//			//firstsecond & secondfirst
//			else {
//				return partition.getBlockOf(reagents.getFirstReagent() ).equals(partition.getBlockOf(products.getSecondReagent())) &&
//					   partition.getBlockOf(reagents.getSecondReagent()).equals(partition.getBlockOf(products.getFirstReagent() ));
//			}
//		} 
//		else {
//			il problema e' qui
//			//unary vs binary or binary vs unary
//			return false;
//		}
	}

	private static void computeBlocks(IComposite composite, IPartition partition,HashMap<IBlock, Integer> blocks) {
		for(int s=0; s<composite.getNumberOfDifferentSpecies();s++) {
			IBlock b = partition.getBlockOf(composite.getAllSpecies(s));
			Integer mult = composite.getMultiplicities(s);
			Integer oldMult = blocks.get(b);
			if(oldMult==null) {
				oldMult=mult;
			}
			else {
				oldMult+=mult;
			}
			blocks.put(b, oldMult);
		}
	}
	
	private static boolean computeConditionalRatesTowardsTheSplitterBlock(ICRN crn, /*ILabel labelSPL,*/ ICompositeBlock blockSPL, HashSet<ISpecies> splitterGenerators, SpeciesCounterHandler[] speciesCounters, HashMap<ISpecies,ISpeciesCounterHandler> speciesCountersHM, boolean hasOneLabelOnly, HashMap<IComposite,ArrayList<ICRNReaction>> reactionsToConsiderForEachComposite,IPartition partition) {
	
		boolean anyReactionConsidered=false;
		for (IComposite aCompositeOfSplitter : blockSPL.getComposites()) {
			Collection<ICRNReaction> consideredReactions = reactionsToConsiderForEachComposite.get(aCompositeOfSplitter);
			//Collection<ICRNReaction> consideredReactions = aCompositeOfSplitter.getIncomingReactions();
			if(consideredReactions!=null){
				anyReactionConsidered=true;
				for (ICRNReaction incomingReaction : consideredReactions) {
					//I added this if. By adding it we get SE. Otherwise we get SMB
					if(!multisetEquivalent(incomingReaction.getReagents(), incomingReaction.getProducts(), partition)) {
						if(incomingReaction.getRate().compareTo(BigDecimal.ZERO)!=0){
							increaseSMBCounterOfReagents(incomingReaction,/*labelSPL,*/ splitterGenerators,speciesCounters,speciesCountersHM,hasOneLabelOnly);
						}
					}
				}
			}
		}
		return anyReactionConsidered;
	}
	
	private static void increaseSMBCounterOfReagents(ICRNReaction reaction, /*ILabel labelSPL,*/HashSet<ISpecies> splitterGenerators, SpeciesCounterHandler[] speciesCounters, HashMap<ISpecies,ISpeciesCounterHandler> speciesCountersHM, boolean onlyOneLabel) {
		IComposite reagents = reaction.getReagents();
		BigDecimal rate = reaction.getRate();
		
		if(reagents.isUnary()){
			ISpecies reagent = reagents.getFirstReagent();
			//SpeciesCounterHandler.initSpeciesCounters(speciesCounters, reagent);
			ISpeciesCounterHandler currentSpeciesCounter =CRNBisimulationsNAry.getOrAddSpeciesCounterHandler(speciesCounters, speciesCountersHM, reagent,CRNBisNAryOrSENAry.SE);
			if(onlyOneLabel){
				currentSpeciesCounter.addToSMBCounter(rate);
			}
			else{
				currentSpeciesCounter.addToSMBCounter(EmptySetLabel.EMPTYSETLABEL,rate);
			}
			
			if(splitterGenerators!=null){
				splitterGenerators.add(reagent);
			}
		}
//		//Otherwise it is a binary reaction (and hence I cannot have just unary labels)
//		else{
//			ISpecies firstReagent = reagents.getFirstReagent();
//			ISpecies secondReagent = reagents.getSecondReagent();
//			
//			SpeciesCounterHandler.initSpeciesCounters(speciesCounters, firstReagent);
//			speciesCounters[firstReagent.getID()].addToSMBCounter(secondReagent,rate);
//			if(splitterGenerators!=null){
//				splitterGenerators.add(firstReagent);
//			}
//			
//			//The following if is to handle homeoreactions 'x+x->pi'
//			//I want to consider only 'x+rho' and not rho 'rho+x'. Otherwise I would add twice the same value
//			if(!firstReagent.equals(secondReagent)) {
//				SpeciesCounterHandler.initSpeciesCounters(speciesCounters, secondReagent);
//				speciesCounters[secondReagent.getID()].addToSMBCounter(firstReagent,rate);
//				if(splitterGenerators!=null){
//					splitterGenerators.add(secondReagent);
//				}
//			}
//		}
		//Otherwise it is an nary reaction (and hence I cannot have just unary labels)
		else{
			NAryLabelBuilder nAryLabel = new NAryLabelBuilder((Composite)reagents);
			for(int i=0;i<reagents.getNumberOfDifferentSpecies();i++){
				ISpecies reagent = reagents.getAllSpecies(i);
				//SpeciesCounterHandler.initSpeciesCounters(speciesCounters, reagent);//ISpeciesCounterHandler currentSpeciesCounter =getOrAddSpeciesCounterHandler(speciesCounters, speciesCountersHM, reagent);
				//						if(speciesCounters[reagent.getID()]==null) {
				//							speciesCounters[reagent.getID()]= new SpeciesCounterHandlerCRNBIsimulationNAry();
				//						}
				ISpeciesCounterHandler currentSpeciesCounter =CRNBisimulationsNAry.getOrAddSpeciesCounterHandler(speciesCounters, speciesCountersHM, reagent,CRNBisNAryOrSENAry.SE);
				nAryLabel.setSpeciesToDecrease(i);
				currentSpeciesCounter.addToSMBCounter(nAryLabel.getObtainedLabel(),rate);//currentSpeciesCounter.addToNRWithScale(nAryLabel.getObtainedLabel(),val,scale,rm);
				nAryLabel.resetSpeciesToDecrease();
				if(splitterGenerators!=null){
					splitterGenerators.add(reagent);
				}
			}
		}
		
		
		/*
		if(labelSPL.equals(EmptySetLabel.EMPTYSETLABEL)){
			if(reagents.isUnary()){
				ISpecies reagent = reagents.getFirstReagent();
				SpeciesCounterHandler.initSpeciesCounters(speciesCounters, reagent);
				speciesCounters[reagent.getID()].addToCRRM(rate);
				if(splitterGenerators!=null){
					splitterGenerators.add(reagent);
				}
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
					speciesCounters[secondReagent.getID()].addToCRRM(rate);
					if(splitterGenerators!=null){
						splitterGenerators.add(secondReagent);
					}
				}
				//For the case in which we consider the real semantics of CTMCs which halves the rates for homeoreactions (i.e., when crr does not have (rho(x)+1))
				else if(secondReagent.equals(labelSPL)){
				//For the case in which we consider the WYSWYG semantics of CTMCs where we assume that rates of homeoreactions have been already halved (i.e., when crr has (rho(x)+1), and implies DSB) 
				//if(secondReagent.equals(labelSPL)){
					speciesCounters[firstReagent.getID()].addToCRRM(rate);
					if(splitterGenerators!=null){
						splitterGenerators.add(firstReagent);
					}
				}
			}
		}
		*/

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


