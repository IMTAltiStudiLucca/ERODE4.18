package it.imt.erode.partitionrefinement.algorithms;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.auxiliarydatastructures.CRNandPartition;
import it.imt.erode.auxiliarydatastructures.CompositeAndBoolean;
import it.imt.erode.commandline.CRNReducerCommandLine;
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
import it.imt.erode.crn.symbolic.constraints.IConstraint;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.CompositePartition;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.ICompositeBlock;
import it.imt.erode.partition.interfaces.ICompositePartition;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SpeciesCounterHandler;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SpeciesSplayBST;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SpeciesSplayBSTWithTolerance;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SplittersGeneratorSMB;

public class ExactFluidBisimilarity {
	
	
	/**
	 * 
	 * @param crn
	 * @param partition the partition to be refined. It is not modified, but instead a new one is created and returned
	 * @param initializeCounters
	 * @param verbose
	 * @return
	 */
	public static IPartition computeEFL(ICRN crn, IPartition partition, boolean verbose,MessageConsoleStream out, BufferedWriter bwOut, IMessageDialogShower msgDialogShower){
		
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"BB_old Reducing: "+crn.getName());
		}

		IPartition obtainedPartition = partition.copy();
		
		if(!crn.isElementary()){
			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it is not an elementary CRN (i.e., it has ternary or more reactions). I terminate.");
			return obtainedPartition;
		}
		else if(!crn.isMassAction()){
			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it is not a mass action CRN (i.e., it has reactions with arbitrary rates). I terminate.");
			return obtainedPartition;
		}
		else if(crn.isSymbolic()){
			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it contains symbolic parameters (i.e. parameters with no acutal value assigned) I terminate.");
			return obtainedPartition;
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
		
//		if(!(CRNReducerCommandLine.univoqueReagents)){
//			if(msgDialogShower==null) {
//				CRNReducerCommandLine.printWarning(out,bwOut,"Not all necessary data structure have been filled. Add 'unique reagents = true' at the beginning of the file.");
//			}
//			else{
//				CRNReducerCommandLine.printWarning(out,bwOut,true,msgDialogShower,"Not all necessary data structure have been filled. Add 'unique reagents = true' at the beginning of the file.",DialogType.Warning);
//			}
//			
//		}
//		else{
//			
//		}
		refineEFL(crn,obtainedPartition,speciesCounters);
		//refineEFL(crn,obtainedPartition);
		
		//CRNReducerCommandLine.println(out,bwOut,"\n"+obtainedPartition+"\n");
		
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"The final partition:");
			CRNReducerCommandLine.println(out,bwOut,obtainedPartition);
		}

		if(verbose){
			long end = System.currentTimeMillis();
			CRNReducerCommandLine.println(out,bwOut,"BB_old Partitioning completed. From "+ crn.getSpecies().size() +" species to "+ obtainedPartition.size() + " blocks. Time necessary: "+(end-begin)+ " (ms)");
			CRNReducerCommandLine.println(out,bwOut,"");
		}
		return obtainedPartition;
	}

	private static void refineEFL(ICRN crn, IPartition partition, SpeciesCounterHandler[] speciesCounters) {
		/* DOES NOT WORK FOR THIS NETWORK: */
//		 begin reactions
//		  xa0 + ya0 -> ya0 + xa1 , 1.0
//		  ya2 + xc0 -> xc0 + ya1 , 1.0
//		  
//		  
//		  xa0 + yb2 -> xa0 + yb1 , 1.0
//		  xa0 + yb1 -> xa0 + yb0 , 1.0
//		  
//		  ya0 + xa1 -> ya0 + xa2 , 1.0
//		  yb0 + xb0 -> yb0 + xb1 , 1.0
//		  yb0 + xb1 -> yb0 + xb2 , 1.0
//		  xb0 + yc2 -> xb0 + yc1 , 1.0
//		  xb0 + yc1 -> xb0 + yc0 , 1.0
//		  yc0 + xd0 -> yc0 + xd1 , 1.0
//		  yc0 + xd1 -> yc0 + xd2 , 1.0
//		  xc0 + ya1 -> ya0 + xc0 , 1.0
//		  xc0 + yd0 -> yd0 + xc1 , 1.0
//		  yd0 + xc1 -> yd0 + xc2 , 1.0
//		  xd0 + yd2 -> xd0 + yd1 , 1.0
//		  xd0 + yd1 -> xd0 + yd0 , 1.0
//		 end reactions
		
		
		ICompositePartition compositesPartition = new CompositePartition(crn.computeSetOfReagents(),partition);

		List<ILabel> labels=new ArrayList<>();
		labels.add(EmptySetLabel.EMPTYSETLABEL);
		//generate candidate splitters
		SplittersGeneratorSMB splittersGenerator = compositesPartition.getSplitterGeneratorAndCreateItIfFirstInvocation(labels);
		
		//Initialize all counter fields and the bst of the species
		CRNBisimulationsNAry.initializeAllCounters(crn.getSpecies(),speciesCounters);
		partition.initializeAllBST();
		
		//Compute all cumulative flux terms once
		computeAllCumulativeFluxTerms(crn,speciesCounters);
		
		int prevSize = -1;
		int iteration=0;
		while(prevSize!=partition.size()){
			splittersGenerator.restart(compositesPartition.getFirstBlock());
			prevSize=partition.size();
			while(splittersGenerator.hasSplittersToConsider()){
				splitEFL(crn,partition,splittersGenerator,compositesPartition,true,iteration,speciesCounters);
			}
			iteration++;
		}
	}
	
	private static void computeAllCumulativeFluxTerms(ICRN crn, SpeciesCounterHandler[] speciesCounters) {
		for (ICRNReaction reaction : crn.getReactions()) {
			increaseCumulativeFluxTerms(reaction,speciesCounters);
		}
	}
	
	private static void increaseCumulativeFluxTerms(ICRNReaction reaction, SpeciesCounterHandler[] speciesCounters) {
		IComposite productsMinusReagents = reaction.computeProductsMinusReagents();
		//ISpecies[] allSpecies = productsMinusReagents.getAllSpecies();
		//int[] allMultiplicities = productsMinusReagents.getMultiplicities();
		for(int i=0;i<productsMinusReagents.getNumberOfDifferentSpecies();i++){
			if(productsMinusReagents.getMultiplicities(i)!=0){
				SpeciesCounterHandler.initSpeciesCounters(speciesCounters, productsMinusReagents.getAllSpecies(i));
				speciesCounters[productsMinusReagents.getAllSpecies(i).getID()].addToCFT(reaction.getReagents(), productsMinusReagents.getMultiplicities(i),reaction.getRate());
			}
		}
	}
	
	
	private static void splitEFL(ICRN crn, IPartition partition, SplittersGeneratorSMB splittersGenerator, ICompositePartition compositesPartition, boolean refineUpBlocksAfterAnySplit, int iteration, SpeciesCounterHandler[] speciesCounters) {

		ICompositeBlock  blockSPL = splittersGenerator.getBlockSpl();

		//Set of blocks to be considered for splitting (blocks where at least a species has non null stoichiometry in reactions with reagents a composite of blockSPL)
		LinkedHashSet<IBlock> splittedBlocks = new LinkedHashSet<IBlock>();
		
		partitionBlocksOfGivenSpecies(partition,crn,splittedBlocks,blockSPL,speciesCounters);

		//Now I have to update the splitters according to the newly generated partition. I also reinitialize the bst of blocks which have been splitted, but that remain in the partition because not empty.
		//cleanPartitioAndGetNextSplitterIfNecessary(partition, splittersGenerator, splittedBlocks, compositesPartition,refineUpBlocksAfterAnySplit);
		SyntacticMarkovianBisimilarityOLD.cleanPartitioAndGetNextSplitterIfNecessaryFocusingOnTheCurrentCompBlockOnly(partition, splittersGenerator, splittedBlocks, compositesPartition,blockSPL,iteration);

		//Initialize the BST of all species
		//crn.initializeAllBST(consideredSpecies);
	}
	
	private static void partitionBlocksOfGivenSpecies(IPartition partition,ICRN crn,Collection<IBlock> splittedBlocks, ICompositeBlock compositeBlock, SpeciesCounterHandler[] speciesCounters) {

		//NON MI PIACE CHE QUI USO DIRETTAMENTE CFT. DOVREI FARE UN METODO CHE PRENDE UN VALORE, ED UNO CHE PRENDE UN VECTOR. E POI LI CHIAMO DAGLI ALGORITMI DI PARTIZIONAMENTO
		
		//species with non null stoichiometry in reactions with reagents a composite of blockSPL
		//Set<ISpecies> speciesWhichHaveBeenConsidered = new LinkedHashSet<ISpecies>();
		
		//split each block of partition according to the cumulative flux rate summed for all composites in compositeBlock 
		for (ISpecies species : crn.getSpecies()) {
			IBlock block = partition.getBlockOf(species);
			
			BigDecimal cft = BigDecimal.ZERO;
			if(speciesCounters[species.getID()]!=null){
				cft = speciesCounters[species.getID()].getCumulativeFluxRate(compositeBlock.getComposites());
			}
			partition.splitBlockOnlyIfKeyIsNotZero(splittedBlocks, species,block, cft);
		}
	
		
		
		
		//return speciesWhichHaveBeenConsidered;

	}
	
	
	public static IPartition prepartitionWRTIC(List<ISpecies> allSpecies, IPartition partition,
			boolean printInfo,MessageConsoleStream out, BufferedWriter bwOut,Terminator terminator) {
		long begin = System.currentTimeMillis();

		//This is the refinement of partition, computed in each iteration of the while loop 
		IPartition refinement = new Partition(allSpecies.size());
		IBlock currentBlock = partition.getFirstBlock();
		while(currentBlock!=null && !Terminator.hasToTerminate(terminator)){
			//The binary search tree used to split each block
			SpeciesSplayBST<BigDecimal, IBlock> bst = new SpeciesSplayBST<BigDecimal, IBlock>();
			for (ISpecies species : currentBlock.getSpecies()) {
				// Insert the species "species" in the global binary search tree created, so to partition each block of the current partition according to the initial concentrantions. 
				// This may cause the creation of a new block, in which case it is automatically added to refinement.   
				bst.put(species.getInitialConcentration(), species, refinement);
			}
			currentBlock=currentBlock.getNext();
		}
		long end = System.currentTimeMillis();
		if(printInfo){
			CRNReducerCommandLine.println(out,bwOut,"Refinement of the input partition with respect to the initial concentrations of the species completed. From "+partition.size()+" to "+refinement.size()+" blocks. Time necessary: "+(end-begin)+ " (ms)");
		}
		return refinement;
	}
	
	/**
	 * This method refines the input partition splitting its blocks in blocks of species with same initial conditions
	 * @param crn
	 * @param partition the input partition
	 * @param printInfo print information
	 * @return
	 */
	public static IPartition prepartitionWRTIC(ICRN crn, IPartition partition,
			boolean printInfo,MessageConsoleStream out, BufferedWriter bwOut,Terminator terminator) {
		return prepartitionWRTIC(crn.getSpecies(), partition, printInfo, out, bwOut, terminator);
	}
	
	/**
	 * This method refines the input partition splitting its blocks in blocks of species with same corresponiding value in the provided array. Comparisons are done up to a given tolerance
	 * @param crn
	 * @param partition the input partition
	 * @param printInfo print information
	 * @param weights the values according to which the partition should be refined
	 * @param tolerance the tolerance
	 * @return
	 */
	public static IPartition prepartitionWithTolerance(ICRN crn, IPartition partition,
			boolean printInfo,MessageConsoleStream out, BufferedWriter bwOut,Terminator terminator, double[] weights, BigDecimal tolerance) {
		long begin = System.currentTimeMillis();
		
		//This is the refinement of partition, computed in each iteration of the while loop 
		IPartition refinement = new Partition(crn.getSpecies().size());
		IBlock currentBlock = partition.getFirstBlock();
		while(currentBlock!=null && !Terminator.hasToTerminate(terminator)){
			//The binary search tree used to split each block
			SpeciesSplayBSTWithTolerance<BigDecimal, IBlock> bst = new SpeciesSplayBSTWithTolerance<>(tolerance);
			for (ISpecies species : currentBlock.getSpecies()) {
				// Insert the species "species" in the global binary search tree created, so to partition each block of the current partition according to the initial concentrantions. 
				// This may cause the creation of a new block, in which case it is automatically added to refinement.   
				bst.put(BigDecimal.valueOf(weights[species.getID()]), species, refinement);
			}
			currentBlock=currentBlock.getNext();
		}
		long end = System.currentTimeMillis();
		double seconds = (end-begin)/1000.0;
		if(printInfo){
			CRNReducerCommandLine.println(out,bwOut,"Refinement of the input partition with respect to the weights (modulo the tolerance "+tolerance+") completed. From "+partition.size()+" to "+refinement.size()+" blocks. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, (seconds))+ " (s)");
		}
		return refinement;
	}
	
	public static CRNandPartition computeReducedCRNEFL(ICRN crn, String name, IPartition partition, List<String> symbolicParameters,List<IConstraint> constraints, List<String> parameters,MessageConsoleStream out, BufferedWriter bwOut,Terminator terminator) throws IOException {
		return computeReducedCRNEFL(crn,name,partition,symbolicParameters,constraints,parameters, "#",out,bwOut,terminator);
	}
	public static CRNandPartition computeReducedCRNEFL(ICRN crn, String name, IPartition partition, List<String> symbolicParameters,List<IConstraint> constraints, List<String> parameters, String commentSymbol,MessageConsoleStream out, BufferedWriter bwOut,Terminator terminator) throws IOException {
		ICRN reducedCRN = new CRN(name,symbolicParameters,constraints,parameters,crn.getMath(),out,bwOut);
		
		if(CRNReducerCommandLine.COMPUTEREPRESENTATIVEBYMINOUTSIDEPARTITIONREFINEMENT){
			partition.setMinAsRepresentative();
		}
		
		/*ISpecies[] speciesIdToSpecies= new ISpecies[crn.getSpecies().size()];
		crn.getSpecies().toArray(speciesIdToSpecies);*/
		/*HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
		for (ISpecies species : crn.getSpecies()) {
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			speciesNameToSpecies.put(species.getName(), species);
		}*/

		//IBlock uniqueBlock = new Block();
		//IPartition trivialPartition = new Partition(uniqueBlock,partition.size());
		
		//Create the set of reduced species: a species per block.
		IBlock currentBlock = partition.getFirstBlock();
		int i=0;
		LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies = new LinkedHashMap<IBlock, ISpecies>(partition.size());
		boolean originalModelHasZeroSpecies=false;
		IBlock blockOfZeroOfOriginalModel=null;
		while(currentBlock!=null){
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			ISpecies blockRepresentative = currentBlock.getRepresentative(CRNReducerCommandLine.COMPUTEREPRESENTATIVEBYMINOUTSIDEPARTITIONREFINEMENT);
			String nameRep=blockRepresentative.getName();
			BigDecimal ic = blockRepresentative.getInitialConcentration();

			ISpecies reducedSpecies;
			if(crn.isZeroSpecies(blockRepresentative)){
			//if(crn.containsTheZeroSpecies() && nameRep.equals(Species.ZEROSPECIESNAME)){
				//The original model already had the zero species. I'll add it as last species of the reduced model
				originalModelHasZeroSpecies=true;
				blockOfZeroOfOriginalModel=currentBlock;
				currentBlock=currentBlock.getNext();
			}
			else{
				reducedSpecies = new Species(nameRep, blockRepresentative.getOriginalName(), i, ic,blockRepresentative.getInitialConcentrationExpr(),blockRepresentative.getNameAlphanumeric(),blockRepresentative.isAlgebraic());
				reducedCRN.addSpecies(reducedSpecies);
				reducedSpecies.addCommentLines(currentBlock.computeBlockComment());
				//reducedSpecies.setRepresentedEquivalenceClass(currentBlock.getSpecies());
				correspondenceBlock_ReducedSpecies.put(currentBlock, reducedSpecies);
				currentBlock=currentBlock.getNext();
				i++;
			}			
		}
		if(originalModelHasZeroSpecies){
			//Now I add the zero species (contained in the original model), as last species.
			ISpecies blockRepresentative = blockOfZeroOfOriginalModel.getRepresentative(/*true*/);
			ISpecies zeroSpecies = reducedCRN.getCreatingIfNecessaryTheZeroSpecies(crn.getCreatingIfNecessaryTheZeroSpecies().getName());
			zeroSpecies.setInitialConcentration(blockRepresentative.getInitialConcentration(), blockRepresentative.getInitialConcentrationExpr());
			correspondenceBlock_ReducedSpecies.put(blockOfZeroOfOriginalModel, zeroSpecies);
		}

		//Create the reduced reactions
		List<ICRNReaction> reducedReactions = new ArrayList<ICRNReaction>();
		for (ICRNReaction reaction : crn.getReactions()) {
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			IComposite reagentsOfReaction = reaction.getReagents();
			IComposite productsOfReaction = reaction.getProducts();

			IComposite reducedReagentsOfReaction = CRNBisimulationsNAry.getNewCompositeReplaceSpeciesWithReducedOneOfBlock(reagentsOfReaction,partition,correspondenceBlock_ReducedSpecies);
			CompositeAndBoolean compAndBol = getNewCompositeMaintainigRepresentativesAndAddingNonRepresentativeReagents(productsOfReaction,reagentsOfReaction,crn,partition,reducedCRN,correspondenceBlock_ReducedSpecies);
			IComposite reducedProductsOfReaction = compAndBol.getComposite();

			if(reaction.hasArbitraryKinetics()){
				throw new UnsupportedOperationException("This is not a massaction CRN. Use method: it.imt.erode.partitionrefinement.algorithms.SMTOrdinaryFluidBisimilarityBinary.addToListOfReducedReactionsNonMassAction");
			}
			else{
				ICRNReaction reducedReaction = new CRNReaction(reaction.getRate(), reducedReagentsOfReaction, reducedProductsOfReaction, reaction.getRateExpression(),reaction.getID());
				reducedReactions.add(reducedReaction);
			}
		}

		if(!Terminator.hasToTerminate(terminator)){
			if(CRNReducerCommandLine.COLLAPSEREACTIONS){
				CRN.collapseAndCombineAndAddReactions(reducedCRN,reducedReactions,out,bwOut);
			}
			else{
				//boolean zeroSpeciesAppearsInReactions=false;
				CRN.addReactionsWithReagentsDifferentFromProducts(reducedCRN, reducedReactions);
			}
			
		}
		
		/*if(CRNReducerCommandLine.collapseReactions){
			CRN.collapseAndCombineAndAddReactions(reducedCRN,reducedReactions,true,out);
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
	
		
		IBlock uniqueBlock = new Block();
		IPartition trivialPartition = new Partition(uniqueBlock,reducedCRN.getSpecies().size());
		for (ISpecies species : reducedCRN.getSpecies()) {
			uniqueBlock.addSpecies(species);
		}
		
		return new CRNandPartition(reducedCRN, trivialPartition);
	}

	

	/*protected static void addToListOfReducedReactions(IPartition partition,
			ICRN reducedCRN, ISpecies[] speciesIdToSpecies,
			HashMap<String, ISpecies> speciesNameToSpecies, HashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,
			List<ICRNReaction> reducedReactions, ICRNReaction reaction,
			IComposite reducedReagentsOfReaction,
			IComposite reducedProductsOfReaction) throws IOException {
		
		if(! reducedReagentsOfReaction.equals(reducedProductsOfReaction)){
			if(CRNReducerCommandLine.univoqueReagents){
				reducedReagentsOfReaction = reducedCRN.addReagentsIfNew(reducedReagentsOfReaction);
			}
			if(CRNReducerCommandLine.univoqueProducts){
				reducedProductsOfReaction = reducedCRN.addProductIfNew(reducedProductsOfReaction);
			}
			if(reaction.hasArbitraryKinetics()){
				throw new UnsupportedOperationException("This is not a massaction CRN. Use method: it.imt.erode.partitionrefinement.algorithms.SMTOrdinaryFluidBisimilarityBinary.addToListOfReducedReactionsNonMassAction");
			}
			else{
				ICRNReaction reducedReaction = new CRNReaction(reaction.getRate(), reducedReagentsOfReaction, reducedProductsOfReaction, reaction.getRateExpression());
				//reducedCRN.addReaction(reducedReaction);
				//AbstractImporter.addToIncomingReactionsOfProducts(reducedReaction.getArity(),reducedProductsOfReaction, reducedReaction);
				reducedReactions.add(reducedReaction);
			}
		}
	}*/
	
	/*protected static void addToListOfReducedReactions(IPartition partition,
			ISpecies[] speciesIdToSpecies,
			HashMap<String, ISpecies> speciesNameToSpecies, HashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,
			List<ICRNReaction> reducedReactions, ICRNReaction reaction,
			IComposite reducedReagentsOfReaction,
			IComposite reducedProductsOfReaction) throws IOException {
		
		if(reaction.hasArbitraryKinetics()){
			throw new UnsupportedOperationException("This is not a massaction CRN. Use method: it.imt.erode.partitionrefinement.algorithms.SMTOrdinaryFluidBisimilarityBinary.addToListOfReducedReactionsNonMassAction");
		}
		else{
			ICRNReaction reducedReaction = new CRNReaction(reaction.getRate(), reducedReagentsOfReaction, reducedProductsOfReaction, reaction.getRateExpression());
			reducedReactions.add(reducedReaction);
		}
	}*/
	
	
	protected static CompositeAndBoolean getNewCompositeMaintainigRepresentativesAndAddingNonRepresentativeReagents(IComposite composite, IComposite reagents, ICRN originalCRN, IPartition partition, ICRN reducedCRN, HashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies) {

		HashMap<ISpecies, Integer> newSpeciesAndMult = new HashMap<ISpecies, Integer>();
		
		if(composite.isUnary()){
			if(partition.speciesIsRepresentativeOfItsBlock(composite.getFirstReagent())){			
				newSpeciesAndMult.put(composite.getFirstReagent(), 1);
			}
		}
		else{
			//ISpecies[] allSpecies = composite.getAllSpecies();
			//int[] multiplicities = composite.getMultiplicities();
			
			//I first maintain the multiplicites of the representative
			for(int i=0; i < composite.getNumberOfDifferentSpecies(); i++){
				if(partition.speciesIsRepresentativeOfItsBlock(composite.getAllSpecies(i))){
					newSpeciesAndMult.put(composite.getAllSpecies(i), composite.getMultiplicities(i));
				}
			}
		}
		
		//Then I add the multiplicity of the non-representativeReagents
		if(reagents.isUnary()){
			ISpecies reagentSpecies = reagents.getFirstReagent();
			if(!partition.speciesIsRepresentativeOfItsBlock(reagentSpecies)){
				newSpeciesAndMult.put(reagentSpecies, 1);
			}
		}
		else{
			//ISpecies reagentsSpecies[] = reagents.getAllSpecies();
			//int[] reagentsMultiplicities = reagents.getMultiplicities();
			for(int i=0; i < reagents.getNumberOfDifferentSpecies(); i++){
				if(!partition.speciesIsRepresentativeOfItsBlock(reagents.getAllSpecies(i))){
					//newSpeciesList.add(reagentsSpecies[i]);
					//newMultiplicitiesList.add(reagentsMultiplicities[i]);
					newSpeciesAndMult.put(reagents.getAllSpecies(i), reagents.getMultiplicities(i));
				}
			}
		}
		//In case the obtained reagents are empty, I have to add the special zero species
		boolean addedZeroSpecies=false;
		//if(newSpeciesList.isEmpty()){
		if(newSpeciesAndMult.isEmpty()){
			ISpecies zeroSpecies = reducedCRN.getCreatingIfNecessaryTheZeroSpecies();
			//newSpeciesList.add(zeroSpecies);
			//newMultiplicitiesList.add(1);
			newSpeciesAndMult.put(zeroSpecies, 1);
			addedZeroSpecies=true;
			return new CompositeAndBoolean(new Composite(newSpeciesAndMult), addedZeroSpecies);
		}
		else{
			//If it is an unary composite I return the species representing it
			if(newSpeciesAndMult.size()==1){
				Entry<ISpecies, Integer> entry = newSpeciesAndMult.entrySet().iterator().next();
				if(entry.getValue()==1){
					ISpecies species = entry.getKey();
					ISpecies reducedSpecies = correspondenceBlock_ReducedSpecies.get(partition.getBlockOf(species));
					return new CompositeAndBoolean((IComposite)(reducedSpecies), addedZeroSpecies);
				}
			}
		}

		//Finally, I replace the obtained species with the reduced ones.
		HashMap<ISpecies, Integer> redSpeciesAndMult = new HashMap<ISpecies, Integer>();
		for (ISpecies species : newSpeciesAndMult.keySet()) {
			ISpecies reducedSpecies = correspondenceBlock_ReducedSpecies.get(partition.getBlockOf(species));
			Integer prevMult = redSpeciesAndMult.get(reducedSpecies);
			if(prevMult==null){
				redSpeciesAndMult.put(reducedSpecies, newSpeciesAndMult.get(species));
			}
			else{
				redSpeciesAndMult.put(reducedSpecies, newSpeciesAndMult.get(species)+prevMult);
			}
		}

		return new CompositeAndBoolean(new Composite(redSpeciesAndMult), addedZeroSpecies);

	}
	

}
