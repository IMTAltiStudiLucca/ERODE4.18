package it.imt.erode.partitionrefinement.algorithms.booleannetworks;

import java.io.BufferedWriter;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.ui.console.MessageConsoleStream;

import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import com.microsoft.z3.Z3Exception;

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;
import it.imt.erode.auxiliarydatastructures.DoubleAndStatus;
import it.imt.erode.auxiliarydatastructures.IntegerAndPartition;
import it.imt.erode.auxiliarydatastructures.PartitionAndStringAndBoolean;
import it.imt.erode.auxiliarydatastructures.StringAndBigDecimal;
import it.imt.erode.booleannetwork.auxiliarydatastructures.BNandPartition;
import it.imt.erode.booleannetwork.implementations.BooleanNetwork;
import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;
import it.imt.erode.booleannetwork.updatefunctions.ArithmeticConnector;
import it.imt.erode.booleannetwork.updatefunctions.BinaryExprIUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.BooleanUpdateFunctionExpr;
import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.Terminator;

import it.imt.erode.crn.implementations.Species;

import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.symbolic.constraints.BooleanConnector;
import it.imt.erode.expression.parser.IMonomial;
import it.imt.erode.expression.parser.SpeciesMonomial;
import it.imt.erode.importing.automaticallygeneratedmodels.RandomBNG;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.CRNBisimulationsNAry;

public class RandomizedForwardBooleanEquivalence_SAT {

	public final static int neutralMax = 0;//Integer.MIN_VALUE


	private FBEAggregationFunctions aggregationFunction;

	boolean initialized=false;
	private HashMap<ISpecies, List<IMonomial>> speciesToODEsDef;//species to polynomial

	private HashMap<IBlock,List<IMonomial>> odeSums;			//block to polynomal of 'sum' (i.e., or/and) of the block

	public static final boolean SHOWTIMEATEACHSTEP = false;
	public static final boolean DOONLYCHECKSWITHWHOLEPARTITION = false;
	public static final int MAXINNERITERATIONS = Integer.MAX_VALUE;

	private double totalSMTChecksSeconds=0.0;
	private double initSMTTime=0.0;
	private List<Double> smtChecksSecondsAtStep;
	public List<Double> getSMTChecksSecondsAtStep(){
		return smtChecksSecondsAtStep;
	}

	public RandomizedForwardBooleanEquivalence_SAT(FBEAggregationFunctions aggrFunc) {
		this.aggregationFunction=aggrFunc;
	}

	/**
	 * 
	 * @param bn
	 * @param partition the partition to be refined. It is not modified, but instead a new one is created and returned
	 * @param verbose
	 * @param terminator 
	 * @return
	 * @throws Z3Exception 
	 * @throws IOException 
	 * @throws RndFMETauMonomialException 
	 */
	public PartitionAndStringAndBoolean computeOFL_rnd_noSMT(IBooleanNetwork bn, IPartition partition, boolean verbose,MessageConsoleStream out, BufferedWriter bwOut,boolean print, Terminator terminator) throws Z3Exception, IOException, RndFMETauMonomialException{

		CRNReducerCommandLine.println(out,bwOut,"rndFBE Reducing: "+bn.getName()+" with aggregation function "+aggregationFunction+" exploiting polynomization and a randomized algorithm");
//		if(verbose){
//			CRNReducerCommandLine.println(out,bwOut,"rndFBE Reducing: "+bn.getName()+" with aggregation function "+aggregationFunction+" exploiting polynomization and a randomized algorithm");
//		}
//		else {
//			CRNReducerCommandLine.print(out,bwOut," using "+aggregationFunction+"...");
//		}

		IPartition obtainedPartition = partition.copy();

		if(verbose){
			String blocks = " blocks";
			if(obtainedPartition.size()==1){
				blocks = " block";
			}
			CRNReducerCommandLine.println(out,bwOut,"Before partitioning we have "+ obtainedPartition.size() + blocks +" and "+bn.getSpecies().size()+ " species.");
		}

		long begin = System.currentTimeMillis();
		long beginInit = System.currentTimeMillis();
		init(bn,verbose,out,bwOut,terminator);
		long endInit = System.currentTimeMillis();
		initSMTTime = (double)(endInit-beginInit) / 1000.0;
		if(SHOWTIMEATEACHSTEP){	
			CRNReducerCommandLine.println(out,bwOut,"Init requred: "+String.format(CRNReducerCommandLine.MSFORMAT,(initSMTTime))+" (s)");
		}

		IntegerAndPartition iterationsAndPartition = refineRNDFBE(bn,obtainedPartition,verbose,begin,out,bwOut,terminator);
		//int iterations = iterationsAndPartition.getInteger();
		obtainedPartition = iterationsAndPartition.getPartition();
		boolean succeeded = iterationsAndPartition.getInteger()>=0;

		dispose();

		//CRNReducerCommandLine.print(out," ("+iterations+" iterations. Total SMT init time: "+initSMTTime+", total SMT check time: "+totalSMTChecksSeconds+" (s) )");
		/*if(print){
			CRNReducerCommandLine.print(out," (Total SMT init time: "+initSMTTime+", total SMT check time: "+String.format(CRNReducerCommandLine.MSFORMAT,(totalSMTChecksSeconds))+" (s) )");
		}*/
		String smtTimes = "\tSMT init time: "+String.format( CRNReducerCommandLine.MSFORMAT, (initSMTTime))+" (s)\n\tSMT check time: "+ String.format( CRNReducerCommandLine.MSFORMAT, (totalSMTChecksSeconds))+" (s)";


		if(verbose){
			if(succeeded) {
				CRNReducerCommandLine.println(out,bwOut,"The partition obtained after interrupting the maximum number of iterations:");
			}
			else {
				CRNReducerCommandLine.println(out,bwOut,"The final partition:");
			}

			CRNReducerCommandLine.println(out,bwOut,obtainedPartition);
		}

		if(verbose){
			long end = System.currentTimeMillis();
			CRNReducerCommandLine.println(out,bwOut,"OFL Partitioning completed. From "+ bn.getSpecies().size() +" species to "+ obtainedPartition.size() + " blocks. Time necessary: "+(end-begin)+ " (ms)");
			CRNReducerCommandLine.println(out,bwOut,"");
		}
		return new PartitionAndStringAndBoolean(obtainedPartition, smtTimes,succeeded);
	}


	/**
	 * 
	 * @param bn
	 * @param partition the partition to be refined. Note that the partition is modified, thus first invoke copy if you want to preserve it.
	 * @param terminator 
	 * @param labels
	 * @throws Z3Exception 
	 */
	private IntegerAndPartition refineRNDFBE(IBooleanNetwork bn, IPartition partition,boolean verbose,long begin,MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator) throws Z3Exception {

		partition.initializeAllBST();
		odeSums = new HashMap<>(partition.size());

		//remove blocks-sums of splitted blocks
		//compute list of new blocks, and compute sums only for them

		int iteration=0;
		int totalInnerIteration=0;
		int prevPartitionSize;
		
//		RandomEngine randomGenerator = new MersenneTwister(new Date());
		RandomEngine randomGenerator = new MersenneTwister(1);

		ArrayList<IBlock> sumsToBeRemoved = new ArrayList<IBlock>();
		int partitionSizeOfPrevDoWhileIter = partition.size();

		do{
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			//I keep trace of the size and of the last element of the partition, so to ignore newly created subblocks. They will be considered in the next invocation of this method.
			partitionSizeOfPrevDoWhileIter = partition.size();
			prevPartitionSize = partition.size();
			IBlock lastBlockOfOfPartition = partition.getLastBlock();
			iteration++;
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"iteration: "+iteration+" blocks:"+partition.size());
			}

			//Remove the sums of the ODEs of removed blocks
			for (IBlock toBeRemoved : sumsToBeRemoved) {
				odeSums.remove(toBeRemoved);
			}
			sumsToBeRemoved=new ArrayList<IBlock>();//Reinitialize the list of blocks whose cumulative ODEs have to be removed.

			IBlock currentSplitterBlock = partition.getFirstBlock();
			/*Bool*/ArrayList<List<IMonomial>> sumsOfTheBlocks = new ArrayList<>(partition.size());
			//int b=0;
			while(currentSplitterBlock!=null){
				if(Terminator.hasToTerminate(terminator)){
					break;
				}
				List<IMonomial> odeSumOfCurrentBlock = odeSums.get(currentSplitterBlock);
				if(odeSumOfCurrentBlock==null){
					odeSumOfCurrentBlock=computeOdeSum(currentSplitterBlock);
				}
				sumsOfTheBlocks.add(odeSumOfCurrentBlock);
				//b++;
				currentSplitterBlock=currentSplitterBlock.getNext();
			}	

			IBlock blockToSplit = partition.getFirstBlock();
			while(blockToSplit!=null && totalInnerIteration < MAXINNERITERATIONS){
				if(Terminator.hasToTerminate(terminator)){
					break;
				}
				if(blockToSplit.getSpecies().size()==1){
					//If I considered all the blocks in the original partition, I stop: I'll consider the newly generated subblocks in the next iteration (of the do-while).
					if(blockToSplit.equals(lastBlockOfOfPartition)){
						blockToSplit=null;
					}
					else {
						blockToSplit=blockToSplit.getNext();
					}
				}
				else{
					//IPartition partitionOfBlock = refineBlockDecomposeBinaryFDEWithOneSplitterAtTime(blockToSplit,crn,partition,redistributor,redistributorApp,oneMinusRedistributor,sumsOfTheBlocks,verbose);
					//IPartition partitionOfBlock = refineBlockAllSplittersAtOnce(blockToSplit,crn,partition,redistributor,redistributorApp,oneMinusRedistributor,sumsOfTheBlocks,verbose);
					//totalInnerIteration contains the sum of all inneriterations
					totalInnerIteration=refineBlockDecomposeBinaryRNDFBEWithOneSplitterAtTime(blockToSplit,bn,partition,
							sumsOfTheBlocks, verbose,partitionSizeOfPrevDoWhileIter,
							out,bwOut,terminator,totalInnerIteration,randomGenerator);
					//refineBlockAllSplittersAtOnce(blockToSplit,crn,partition,redistributor,redistributorApp,oneMinusRedistributor,sumsOfTheBlocks,verbose,prevPartitionSize);				


					//I finished to partition the block

					/*
				//Add the partition of the block to the new refined partition
				IBlock current = partitionOfBlock.getFirstBlock();
				while(current!=null){
					refinedPartition.add(current);
					current.updatePartition(refinedPartition);
					current=current.getNext();
				}*/

					//cleanPartition(partition);
					

					if(partition.size()==prevPartitionSize+1){
						//I did not actually split the block: I remove the newly created copy of blockToSplit
						partition.remove(partition.getLastBlock());
						//blockToSplit.updatePartition(partition);
						blockToSplit=blockToSplit.getNext();
					}
					else{

						//I actually split the block: I remove the original blockToSplit
						//I remove the sume of the ODEs of the block
						//ArithExpr sum = odeSums.remove(blockToSplit);
						//sum.dispose();
						sumsToBeRemoved.add(blockToSplit);

						//Split the next block
						IBlock nextBlockToSplit;
						//If I considered all the blocks in the original partition, I stop: I'll consider the newly generated subblocks in the next iteration (of the do-while).
						if(blockToSplit.equals(lastBlockOfOfPartition)){
							nextBlockToSplit=null;
						}
						else{
							nextBlockToSplit=blockToSplit.getNext();
						}
						partition.remove(blockToSplit);
						prevPartitionSize=partition.size();//NEW
						blockToSplit=nextBlockToSplit;	
					}

					if(totalInnerIteration >= MAXINNERITERATIONS) {
						break;
					}
				}
			}
			//partition=refinedPartition;
			//}while(partition.size()!=prevPartitionSize && totalInnerIteration < MAXINNERITERATIONS);
		}while(partition.size()!=partitionSizeOfPrevDoWhileIter && totalInnerIteration < MAXINNERITERATIONS);

		//System.out.println();

		//I have to update the information "species to block" of the partition.
		IBlock currentBlock = partition.getFirstBlock();
		while(currentBlock!=null){
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			currentBlock.updatePartition(partition);
			currentBlock=currentBlock.getNext();
		}

		if(totalInnerIteration >= MAXINNERITERATIONS) {
			CRNReducerCommandLine.println(out,bwOut,"\n\tINTERRUPTED after "+totalInnerIteration+" SMT checks due to limit on max SMT checks ("+MAXINNERITERATIONS+") ");
			//return new IntegerAndPartition(iteration, partition);
			return new IntegerAndPartition(-1, partition);
		}
		else {
			//CRNReducerCommandLine.print(out,bwOut," ( "+iteration+" iterations, "+innerIteration+"inner iterations) ");
			CRNReducerCommandLine.print(out,bwOut," ( "+totalInnerIteration+" SMT checks ) ");
			//return new IntegerAndPartition(iteration, partition);
			return new IntegerAndPartition(totalInnerIteration, partition);
		}
	}


//	private void cleanPartition(IPartition partition) {
//		IBlock currentBlock=partition.getFirstBlock();
//		//First remove species from a block if they appear in more (they have been moved into a new subblock
//		while(currentBlock!=null) {
//			ArrayList<ISpecies> toRemove=new ArrayList<>(currentBlock.getSpecies().size());
//			for(ISpecies sp : currentBlock.getSpecies()) {
//				IBlock blockOfSp = partition.getBlockOf(sp);
//				if(!blockOfSp.equals(currentBlock)) {
//					toRemove.add(sp);
//				}
//			}
//			for(ISpecies sp : toRemove) {
//				currentBlock.getSpecies().remove(sp);
//			}
//			
//			IBlock next= currentBlock.getNext();
//			if(currentBlock.getSpecies().size()==0) {
//				partition.remove(currentBlock);
//			}
//			currentBlock=next;
//		}
//		
//	}

	private int refineBlockDecomposeBinaryRNDFBEWithOneSplitterAtTime(IBlock blockToSplit,IBooleanNetwork bn, IPartition partition,
			ArrayList<List<IMonomial>> sumsOfTheBlocks, boolean verbose, int sizeOfPartiton,
			MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator, int totalInnerIteration, 
			RandomEngine randomGenerator) throws Z3Exception {
		int indexOfSplitterBlock=0;
		//Add  an empty subblock to the partition, which will be the first subblock of the block to split
		IBlock firstSubBlock = new Block();
		partition.add(firstSubBlock);


		//The new subblock will actually have exactly the same number of species as blockToSplit
		Iterator<ISpecies> iterator = blockToSplit.getSpecies().iterator();
		firstSubBlock.addSpecies(iterator.next());
		//Partition partitionOfBlock = new Partition(firstSubBlock,crn.getSpecies().size());
		
		//List<IMonomial> oneList=new ArrayList<>(1);
		//oneList.add(Monomial.oneMon);
		
		boolean checkIfSpeciesBelongsToNextSubBlock=false;
		while(iterator.hasNext()){
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			ISpecies currentSpecies=iterator.next();
			
			//Check if currentSpecies belongs to an existing block of partitionOfBlock,
			//Otherwise create a new block
			boolean correctSubBlockFoundForCurrentSpecies=false;
			IBlock currentSubBlock = firstSubBlock;
			while(currentSubBlock!=null && !correctSubBlockFoundForCurrentSpecies && totalInnerIteration < MAXINNERITERATIONS){
				if(Terminator.hasToTerminate(terminator)){
					break;
				}
				ISpecies repOfCurrentSubBlock = currentSubBlock.getRepresentative();
				//				if(repOfCurrentSubBlock.equals(currentSpecies)) {
				//					System.out.println("ciao");
				//				}
				indexOfSplitterBlock=0;
				
				List<IMonomial> sumOfThetwo = sumPolynomials(new SpeciesMonomial(currentSpecies),new SpeciesMonomial(repOfCurrentSubBlock));
				  
				IBlock currentSplitterBlock = partition.getFirstBlock();
				while(currentSplitterBlock!=null && !checkIfSpeciesBelongsToNextSubBlock && totalInnerIteration < MAXINNERITERATIONS){
					if(Terminator.hasToTerminate(terminator)){
						break;
					}
					
					/*
					 * Alexander shall modify here
					 * What happens below is the following:
					 * a) we build a z3 formula to be checked to prove that the current partition is not an FBE.
					 * b) in 'check(.) we ask z3 to find an assignment for the variables in the model for which the formula does not hold
					 * 
					 * We shall modify 'b' by: 
					 * 	b1) sampling an assignment for the formulas, 
					 *  b2) ask z3 whether the formula is true for that assignment
					 *  b3.1) if it is not true, we know it in not an FBE. 
					 *  b3.2) if it is true, we need to sample another assignment
					 *  
					 *  For now, we can keep using z3, because it is simple
					 *  - we just need to add more conditions of the form 'xi=j' if we sampled value j for var i. 
					 *  		ArithExpr sampledValue = ctx.mkReal(0);//ctx.mkInt(0)//ctx.mkBool(true)
								Expr cur_population = speciesToPopulation.get(species);
								Expr imposeSampleValue = ctx.mke((ArithExpr)population, sampledValue);
								solver.add(imposeSampleValue); 
								alternatively [better]!!!!!!!!
									megaFormulatocheckWithSampledValue=megaFormulatochech.substitute(species, sampledValue)
						For the future, we can easily create our own constraint solver for check whether a closed-formula is true or not
					 *  	It is 'easy'. It will be super-efficient. But it requires to reimplement several steps.
					 */
					
					/*
					for number of experiments
						for all variables xi:
							sample value for xi
							tell z3 about the sampled value
						ask z3 about truth of closed-formula
						if formula is not true
							the 2 variables should be split
					*/
					
					List<IMonomial> cumulativeODEsOfCSB = sumsOfTheBlocks.get(indexOfSplitterBlock);//odeSums.get(currentSplitterBlock);
					if(cumulativeODEsOfCSB ==null) {
						System.out.println("Ciao");
					}
					//TODO neutral element
//					HashMap<ISpecies, List<IMonomial>> forceReplacement_ij=new HashMap<ISpecies, List<IMonomial>>(2);
//					//Replace currentSpecies with neutral element //TODO actual neutral element
//					forceReplacement_ij.put(currentSpecies      , oneList);
//					//Replace repOfCurrentSubBlock with sum of block
//					forceReplacement_ij.put(repOfCurrentSubBlock, sumOfThetwo);
//					//The converse order has to be checked
//					HashMap<ISpecies, List<IMonomial>> forceReplacement_ji=new HashMap<ISpecies, List<IMonomial>>(2);
//					forceReplacement_ji.put(currentSpecies      , sumOfThetwo);
//					forceReplacement_ji.put(repOfCurrentSubBlock, oneList);
//					//List<IMonomial> swappedSum_phi_ij= cumulativeODEsOfCSB;
//					//List<IMonomial> swappedSum_phi_ji= cumulativeODEsOfCSB;
//					
//					/*Bool*/Expr swappedSum = cumulativeODEsOfCSB.substitute(repOfCurrentSubBlockPop, redistributorApp);
//					// Replace currentSpecies with neutral element
//					Expr neutralElement = neutralElement(bn,currentSpecies,repOfCurrentSubBlock);
//					/*Bool*/Expr swappedSum_phi_ij = swappedSum.substitute(currentSpeciesPop, neutralElement);
//					//Replace repOfCurrentSubBlock with sum of block
//					swappedSum_phi_ij =  swappedSum_phi_ij.substitute(redistributorApp, sumOfThetwo);
//					swappedSum_phi_ij=swappedSum_phi_ij.simplify();
//
//					//The converse order has to be checked
//					//Replace currentSpecies with sum of block
//					/*Bool*/Expr swappedSum_phi_ji = swappedSum.substitute(currentSpeciesPop, sumOfThetwo);
//					//Replace repOfCurrentSubBlock with neutral element
//					
//					swappedSum_phi_ji =  swappedSum_phi_ji.substitute(redistributorApp, neutralElement);
//					swappedSum_phi_ji=swappedSum_phi_ji.simplify();
					
					

// Substitutions go here
					//Expr phi_ij__and__phi_ji = ctx.mkAnd(new BoolExpr[]{ctx.mkEq(cumulativeODEsOfCSB, swappedSum_phi_ij),ctx.mkEq(cumulativeODEsOfCSB, swappedSum_phi_ji)});

					
					int numExperiments = 40;//100;
					boolean notSplit = true;
					int experiment = 0;
					int minNr = 0;//-10;
					int maxNr = 16;//10;
					HashMap<ISpecies, Double> speciesToValue;
					while (experiment < numExperiments && notSplit){
						speciesToValue=new HashMap<ISpecies, Double>(bn.getSpecies().size());
						//Expr phi_comp =  phi_ij__and__phi_ji.translate(ctx);
						for(ISpecies species: bn.getSpecies()) {
							int val = RandomBNG.nextInt(randomGenerator, maxNr-minNr)+minNr;
							speciesToValue.put(species, Double.valueOf(val));
							//phi_comp = phi_comp.substitute(species, sampledValue);
						}
						//phi_comp= phi_comp.simplify();
						//notSplit = notSplit && phi_comp.isTrue() && !phi_comp.isFalse();
						
						//Now I evaluate the value of 'p_{xi op xj}', and I prepare data structures to force the replacements ij, and ji
						double val_sumOfTheTwo=eval(sumOfThetwo,speciesToValue,null);
						
						HashMap<ISpecies, Double> forceReplacement_ij=new HashMap<>(2);
						//Replace currentSpecies with neutral element //TODO actual neutral element
						forceReplacement_ij.put(currentSpecies      , 1.0);
						//Replace repOfCurrentSubBlock with sum of block
						forceReplacement_ij.put(repOfCurrentSubBlock, val_sumOfTheTwo);
						//The converse order has to be checked
						HashMap<ISpecies, Double> forceReplacement_ji=new HashMap<>(2);
						forceReplacement_ji.put(currentSpecies      , val_sumOfTheTwo);
						forceReplacement_ji.put(repOfCurrentSubBlock, 1.0);
						
						
						double val_orig= eval(cumulativeODEsOfCSB,speciesToValue,null);
						double val_swapped_ij=0;
						try {
							val_swapped_ij= eval(cumulativeODEsOfCSB,speciesToValue,forceReplacement_ij);
						}catch (Exception e) {
							System.out.println("ciao");
						}
						double val_swapped_ji=0;
						try {
							val_swapped_ji= eval(cumulativeODEsOfCSB,speciesToValue,forceReplacement_ji);
						}catch (Exception e) {
							System.out.println("ciao");
						}
						boolean congruent_ij=congruentModM(val_orig,val_swapped_ij,maxNr);
						boolean congruent_ji=congruentModM(val_orig,val_swapped_ji,maxNr);
						
						notSplit=notSplit && congruent_ij && congruent_ji;
						
						experiment++;
					}

					totalInnerIteration++;
					System.out.print(totalInnerIteration +" ");
//					if(totalInnerIteration==17) {
//						System.out.println();
//					}
					if(totalInnerIteration%50==0) {
						System.out.println();
					}
					if(totalInnerIteration==MAXINNERITERATIONS) {
						return totalInnerIteration;
					}
					if(notSplit){
						//The two species are equal according to currentBlock. I have to consider the next block. I stop when I considered all original blocks of the partition, and not the subblocks generated in this iteration
						indexOfSplitterBlock++;
						//if(currentSplitterBlock.equals(lastBlockOfPartition)){
						if(indexOfSplitterBlock==sizeOfPartiton){
							currentSplitterBlock=null;
						}
						else{
							currentSplitterBlock=currentSplitterBlock.getNext();
						}
					}
					else{
						//The two species are NOT equal according to currentBlock. 
						//I cannot put currentSpecies in the block currentBlockOfPartitionOfTheBlock.
						//I have to check the next block of partitionOfBlock
						checkIfSpeciesBelongsToNextSubBlock=true;
						//break;//exit while(currentSplitterBlock!=null)
					}
				}

				if(checkIfSpeciesBelongsToNextSubBlock){
					checkIfSpeciesBelongsToNextSubBlock=false;
					currentSubBlock = currentSubBlock.getNext();
					//If I do not have other subBlocks (of the partition of the block) to consider, 
					//I create a new one, and I put the species in it
					if(currentSubBlock==null){
						IBlock newBlockOfTheBlock = new Block();
						partition.add(newBlockOfTheBlock);
						newBlockOfTheBlock.addSpecies(currentSpecies);
						//break;exit while(currentSubBlock!=null && !correctSubBlockFound){
						correctSubBlockFoundForCurrentSpecies=true;
					}
				}
				else{
					//The currentSpecies can be added to currentBlockOfPartitionOfTheBlock
					currentSubBlock.addSpecies(currentSpecies);
					currentSubBlock = currentSubBlock.getNext();//What is this needed for?
					//break;exit while(currentSubBlock!=null && !correctSubBlockFound){
					correctSubBlockFoundForCurrentSpecies=true;
				}
			}
		}
		return totalInnerIteration;
	}


	private boolean congruentModM(double left, double right, int m) {
		double diff=left - right;
		if(diff==0) {
			return true;
		}
		else {
			//38\equiv 14 mod 12
			//because the difference is 38 – 14 = 24 = 2 × 12
			if(diff%m==0) {
				return true;
			}
			else {
				return false;
			}
		}
	}

	private double eval(List<IMonomial> monomials, HashMap<ISpecies, Double> speciesToValue, HashMap<ISpecies,Double> forceReplacement_ij) {
		double sum = 0;
		for(IMonomial mon : monomials) {
			sum+=mon.eval(speciesToValue,forceReplacement_ij);
		}
		return sum;
	}

	//	@SuppressWarnings("unused")
	//	private void refineBlockAllSplittersAtOnce(IBlock blockToSplit,ICRN crn, IPartition partition, ArithExpr redistributor, ArithExpr redistributorApp, ArithExpr oneMinusRedistributor, ArithExpr[] sumsOfTheBlocks, boolean verbose, int sizeOfPartiton,MessageConsoleStream out, BufferedWriter bwOut) throws Z3Exception {
	//		//I add to the partition an empty block, which will be the first subblock of the partition remove the block to split
	//		IBlock firstSubBlock = new Block();
	//		partition.add(firstSubBlock);
	//		//Partition partitionOfBlock = new Partition(firstSubBlock,crn.getSpecies().size());//it will actually have exactly the same number of species as blockToSplit
	//		//I add the first species of blockToSplit to firstSubBlock, and I create an iterator to iterate over all the other species in the block
	//		Iterator<ISpecies> iterator = blockToSplit.getSpecies().iterator();
	//		firstSubBlock.addSpecies(iterator.next());
	//
	//		boolean checkIfSpeciesBelongsToNextSubBlock=false;
	//		while(iterator.hasNext()){
	//			ISpecies currentSpecies=iterator.next();
	//			ArithExpr currentSpeciesPop = speciesToPopulation.get(currentSpecies);
	//			//I now have to check if currentSpecies has to belong to an existing block of partitionOfBlock, or if a new one has to be created
	//			boolean correctSubBlockFoundForCurrentSpecies=false;
	//			//IBlock currentSubBlock = partitionOfBlock.getFirstBlock();
	//			IBlock currentSubBlock = firstSubBlock;
	//			BoolExpr[] conditionForEachSplitter = new BoolExpr[sizeOfPartiton];
	//			while(currentSubBlock!=null && !correctSubBlockFoundForCurrentSpecies){
	//				ISpecies repOfCurrentSubBlock = currentSubBlock.getRepresentative();
	//				ArithExpr repOfCurrentSubBlockPop = speciesToPopulation.get(repOfCurrentSubBlock);
	//				ArithExpr sumOfThetwo = ctx.mkAdd(new ArithExpr[]{currentSpeciesPop,repOfCurrentSubBlockPop});
	//				while(!checkIfSpeciesBelongsToNextSubBlock && !correctSubBlockFoundForCurrentSpecies){
	//					for(int b=0;b<sizeOfPartiton;b++){
	//						//Rename repOfCurrentSubBlock with an auxilairy symbol (necessary, as in classic "swap function")
	//						ArithExpr swappedSum = (ArithExpr) sumsOfTheBlocks[b].substitute(repOfCurrentSubBlockPop, redistributorApp); 
	//						// Replace currentSpecies with s * (currentSpecies + repOfCurrentSubBlock));
	//						swappedSum = (ArithExpr)swappedSum.substitute(currentSpeciesPop, ctx.mkMul(new ArithExpr[]{redistributor,sumOfThetwo}));
	//						//Replace repOfCurrentSubBlock with (1-s) * (currentSpecies + repOfCurrentSubBlock));
	//						swappedSum =  (ArithExpr)swappedSum.substitute(redistributorApp, ctx.mkMul(new ArithExpr[]{oneMinusRedistributor,sumOfThetwo}));
	//						conditionForEachSplitter[b]=ctx.mkEq(sumsOfTheBlocks[b], swappedSum);
	//					}
	//					
	//					solver.reset();
	//					if(IMPOSEpositivePopulationsAssertion)
	//						solver.add(positivePopulationsAssertion);
	//					solver.add(allConstraintAssertion);
	//					solver.add(ctx.mkEq(redistributor, ctx.mkReal("0.5")));
	//					solver.add(ctx.mkNot(ctx.mkAnd(conditionForEachSplitter)));
	//					
	//
	//					DoubleAndStatus timeAndStatus = check(solver,verbose,SHOWTIMEATEACHSTEP,-1,-1,out,bwOut);
	//					totalSMTChecksSeconds+= timeAndStatus.getDouble();
	//					smtChecksSecondsAtStep.add(timeAndStatus.getDouble());
	//
	//					if(timeAndStatus.getStatus()==Status.UNKNOWN){
	//						CRNReducerCommandLine.println(out,bwOut,"z3 returned unknown. This is the reason:");
	//						CRNReducerCommandLine.println(out,bwOut,solver.getReasonUnknown());
	//						throw new Z3Exception("z3 returned unknown. This is the reason:\n"+solver.getReasonUnknown());
	//						//System.exit(-1);
	//						//return null;
	//					}
	//					else if(timeAndStatus.getStatus()==Status.UNSATISFIABLE){
	//						//The two species are equal according to currentBlock.
	//						//The currentSpecies can be added to currentBlockOfPartitionOfTheBlock
	//						currentSubBlock.addSpecies(currentSpecies);
	//						//currentSubBlock = currentSubBlock.getNext();//What is this needed for?
	//						//break;exit while(currentSubBlock!=null && !correctSubBlockFound){
	//						correctSubBlockFoundForCurrentSpecies=true;
	//					}
	//					else{
	//						//The two species are NOT equal according to currentBlock. 
	//						//I cannot put currentSpecies in the block currentBlockOfPartitionOfTheBlock. I have to check the next block of partitionOfBlock
	//						checkIfSpeciesBelongsToNextSubBlock=true;
	//						//break;//exit while(currentSplitterBlock!=null)
	//					}
	//				}
	//
	//				if(checkIfSpeciesBelongsToNextSubBlock){
	//					checkIfSpeciesBelongsToNextSubBlock=false;
	//					currentSubBlock = currentSubBlock.getNext();
	//					//If I do not have other subBlocks (of the partition of the block) to consider, I create a new one, and I put the species in it
	//					if(currentSubBlock==null){
	//						IBlock newBlockOfTheBlock = new Block();
	//						partition.add(newBlockOfTheBlock);
	//						newBlockOfTheBlock.addSpecies(currentSpecies);
	//						//break;exit while(currentSubBlock!=null && !correctSubBlockFound){
	//						correctSubBlockFoundForCurrentSpecies=true;
	//					}
	//				}
	//			}
	//		}	
	//		
	//	}


	private List<IMonomial> computeOdeSum(IBlock block) throws Z3Exception {
		List<IMonomial> odeSum=null;
		if(block.getSpecies().size()==1) {
			ISpecies species =block.getRepresentative();
			odeSum=speciesToODEsDef.get(species);
		}
		else {
			//I need to compute the polynomial of the OR or AND of all update functions in the block
			boolean first=true;
			for (ISpecies species : block.getSpecies()) {
				List<IMonomial> polynomialOfCurrent = speciesToODEsDef.get(species);
				if(first) {
					odeSum=new ArrayList<>(polynomialOfCurrent.size());
					odeSum.addAll(polynomialOfCurrent);
					first=false;
				}
				else {
					odeSum=sumPolynomials(odeSum,polynomialOfCurrent);
				}
			}
		}		
		odeSums.put(block, odeSum);
		return odeSum; 
	}

	
	private List<IMonomial> sumPolynomials(IMonomial left, IMonomial right) {
		List<IMonomial> leftList=new ArrayList<>(1);
		List<IMonomial> rightList=new ArrayList<>(1);
		
		leftList.add(left);
		rightList.add(right);
		
		return sumPolynomials(leftList, rightList);
	}
	private List<IMonomial> sumPolynomials(List<IMonomial> left, List<IMonomial> right) {
		if(aggregationFunction.equals(FBEAggregationFunctions.OR)){
			return BooleanUpdateFunctionExpr.polynomialOfOR(left, right);
		}
		else if(aggregationFunction.equals(FBEAggregationFunctions.AND)){
			return BooleanUpdateFunctionExpr.polynomialOfAND(left, right);
		} 
		else {
			throw new UnsupportedOperationException("Not supported FBE aggregation function: "+aggregationFunction);
		}
	}


//	/**
//	 * Computes the neutral element for an aggregation function (for two compared species)
//	 * @return
//	 */
//	private /*Bool*/Expr neutralElement(IBooleanNetwork bn, ISpecies first,ISpecies second) {
//		if(aggregationFunction.equals(FBEAggregationFunctions.OR)) {
//			return ctx.mkFalse();
//		}
//		else if(aggregationFunction.equals(FBEAggregationFunctions.AND)) {
//			return ctx.mkTrue();
//		}
//		else if(aggregationFunction.equals(FBEAggregationFunctions.XOR)) {
//			return ctx.mkFalse();
//		}
//		else if(aggregationFunction.equals(FBEAggregationFunctions.PLUS)) {
//			if(realSort) {
//				return ctx.mkReal(0);
//			}
//			else{
//				return ctx.mkInt(0);
//			}
//		}
//		else if(aggregationFunction.equals(FBEAggregationFunctions.TIMES)) {
//			if(realSort) {
//				return ctx.mkReal(1);
//			}
//			else{
//				return ctx.mkInt(1);
//			}
//		}
//		else if(aggregationFunction.equals(FBEAggregationFunctions.MIN)) {
//			int m1=bn.getNameToMax(first.getName());
//			int m2=bn.getNameToMax(second.getName());
//			
//			int inner=(m1>m2)? m1 : m2;
//			if(realSort) {
//				return ctx.mkReal(inner);
//			}
//			else {
//				return ctx.mkInt(inner);
//			}
//			
//			//return ctx.mkInt((m1>m2)? m1 : m2);
//			//return ctx.mkInt(Integer.MAX_VALUE);
//		}
//		else if(aggregationFunction.equals(FBEAggregationFunctions.MAX)) {
//			if(realSort) {
//				return ctx.mkReal(neutralMax);
//			}
//			else {
//				return ctx.mkInt(neutralMax);
//			}
//		}
//		else {
//			throw new UnsupportedOperationException("Not supported FBE/FME aggregation function: "+aggregationFunction);
//		}
//	}

//	public static IUpdateFunction neutralElementUpdFunc(FBEAggregationFunctions aggregationFunction, Collection<ISpecies> speciesToCompareWith, IBooleanNetwork originalBN) {
//		if(aggregationFunction.equals(FBEAggregationFunctions.OR)) {
//			return new FalseUpdateFunction();
//		}
//		else if(aggregationFunction.equals(FBEAggregationFunctions.AND)) {
//			return new TrueUpdateFunction();
//		}
//		else if(aggregationFunction.equals(FBEAggregationFunctions.XOR)) {
//			return new FalseUpdateFunction();
//		}
//		else if(aggregationFunction.equals(FBEAggregationFunctions.PLUS)) {
//			return new ValUpdateFunction(0);
//		}
//		else if(aggregationFunction.equals(FBEAggregationFunctions.TIMES)) {
//			return new ValUpdateFunction(1);
//		}
//		else if(aggregationFunction.equals(FBEAggregationFunctions.MIN)) {
//			//return new ValUpdateFunction(Integer.MAX_VALUE);
//			//All other update functions are simple. For min I take the max of the domains of the species I'm comparing (or of the species in the block).
//			int max= Integer.MIN_VALUE;
//			for(ISpecies sp : speciesToCompareWith) {
//				int cur=originalBN.getNameToMax(sp.getName());
//				if(cur>max) {
//					max=cur;
//				}
//			}
//			return new ValUpdateFunction(max);
//		}
//		else if(aggregationFunction.equals(FBEAggregationFunctions.MAX)) {
//			return new ValUpdateFunction(neutralMax);
//		}
//		else {
//			throw new UnsupportedOperationException("Not supported FBE aggregation function: "+aggregationFunction);
//		}
//	}

	private void dispose() throws Z3Exception{
		initialized=false;
	}

	private void init(IBooleanNetwork bn, boolean verbose,MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator) throws Z3Exception, IOException {

		smtChecksSecondsAtStep=new ArrayList<Double>();
		totalSMTChecksSeconds=0.0;
		initSMTTime=0.0;

		if(SHOWTIMEATEACHSTEP){
			CRNReducerCommandLine.println(out,bwOut,"");
		}
		
		speciesToODEsDef = new HashMap<>(bn.getSpecies().size());

		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<>(bn.getSpecies().size());
		for (ISpecies species : bn.getSpecies()) {
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			speciesNameToSpecies.put(species.getName(), species);
		}
		System.out.print("ciao");
		long beginODESBPopulation = System.currentTimeMillis();
		for (Entry<String, IUpdateFunction> entry : bn.getUpdateFunctions().entrySet()) {
			ISpecies species = speciesNameToSpecies.get(entry.getKey());
			IUpdateFunction updateFunction = entry.getValue();
			List<IMonomial> polynomial = updateFunction.toPolynomial(speciesNameToSpecies);
			speciesToODEsDef.put(species, polynomial);
		}
		long endODESBPopulation = System.currentTimeMillis();
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"\nIterating the species to 1) convert the update functions in polynomials required: "+String.format(CRNReducerCommandLine.MSFORMAT,String.valueOf(((double)(endODESBPopulation-beginODESBPopulation) / 1000.0)))+" (s)");
		}
		initialized=true;
	}	

	protected static DoubleAndStatus check(Solver solver, boolean verbose, boolean showTime, int iteration, int partitionSize,MessageConsoleStream out, BufferedWriter bwOut) throws Z3Exception{
		long begin = System.currentTimeMillis();
		//CRNReducerCommandLine.println(out,bwOut,solver);
		Status status = solver.check();
		long end = System.currentTimeMillis();
		double runtimeCheck = (double)(end-begin) / 1000.0;
		//totalSMTChecksSeconds+=runtimeCheck;
		if(showTime){
			CRNReducerCommandLine.println(out,bwOut,"Check at iteration "+iteration+" with "+partitionSize+" blocks requred: "+String.format( CRNReducerCommandLine.MSFORMAT, (runtimeCheck))+" (s)");
		}
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"The formula is: "+status.name());
		}
		//CRNReducerCommandLine.println(out,bwOut,"\n\n");
		if (verbose && status == Status.SATISFIABLE){
			CRNReducerCommandLine.println(out,bwOut,"\n\nThis is the returned model:");
			CRNReducerCommandLine.println(out,bwOut,solver.getModel());
			CRNReducerCommandLine.println(out,bwOut,"\n\n");
		}
		return new DoubleAndStatus(status, runtimeCheck);
	}

	/**
	 * This method reduces the model, assuming the partition regards OFL or ordinary CTMC lumpability
	 * @param name
	 * @param partition
	 * @param parameters 
	 * @param terminator 
	 * @return
	 * @throws IOException 
	 */
	public BNandPartition computeReducedFBE(IBooleanNetwork bn,String name, IPartition partition,String commSymbol,MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator
			,FBEAggregationFunctions aggregationFunction) throws IOException {
		IBooleanNetwork reducedBN = new BooleanNetwork(name, out, bwOut,bn.isMultiValued());


		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(bn.getSpecies().size());
		for (ISpecies species : bn.getSpecies()) {
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			speciesNameToSpecies.put(species.getName(), species);
		}

		ISpecies[] representativeSpecies = CRNBisimulationsNAry.getSortedBlockRepresentatives(partition, terminator);



		//Default block for all reduced species
		//IBlock currentBlock = partition.getFirstBlock();
		//A species per block
		//int i=0;
		LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies = new LinkedHashMap<IBlock, ISpecies>(partition.size());
		LinkedHashMap<IBlock, IUpdateFunction> correspondenceBlock_sumOfUpdateFunctions = new LinkedHashMap<>(partition.size());
		/*
		while(currentBlock!=null){
		 */

		ArithmeticConnector opArith=null;
		BooleanConnector op=null;
		if(aggregationFunction.equals(FBEAggregationFunctions.OR)){
			op=BooleanConnector.OR;
		}
		else if(aggregationFunction.equals(FBEAggregationFunctions.AND)){
			op=BooleanConnector.AND;
		}
		else if(aggregationFunction.equals(FBEAggregationFunctions.XOR)){
			op=BooleanConnector.XOR;
		}
		else if(aggregationFunction.equals(FBEAggregationFunctions.PLUS)) {
			opArith=ArithmeticConnector.SUM;
		}
		else if(aggregationFunction.equals(FBEAggregationFunctions.TIMES)) {
			opArith=ArithmeticConnector.MUL;
		}
		else if(aggregationFunction.equals(FBEAggregationFunctions.MIN)) {
			opArith=ArithmeticConnector.MIN;
		}
		else if(aggregationFunction.equals(FBEAggregationFunctions.MAX)) {
			opArith=ArithmeticConnector.MAX;
		}
		else {
			throw new UnsupportedOperationException("Unsupported aggregation function: "+aggregationFunction);
		}

		for(int i=0;i<representativeSpecies.length;i++) {
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			ISpecies blockRepresentative = representativeSpecies[i];
			IBlock currentBlock = partition.getBlockOf(blockRepresentative);
			/*
			ISpecies blockRepresentative = currentBlock.getRepresentative(CRNReducerCommandLine.COMPUTEREPRESENTATIVEBYMINOUTSIDEPARTITIONREFINEMENT);
			 */
			String nameRep=blockRepresentative.getName();

			StringAndBigDecimal icandExpr = aggregatedIC(currentBlock);
			BigDecimal ic = icandExpr.getBigDecimal();
			String icExpr = icandExpr.getString();

			//			BigDecimal ic = currentBlock.getBlockConcentration();
			//			String icExpr;
			//			if(bn.isMultiValued()) {
			//				icExpr=String.valueOf(ic);
			//			}
			//			else {
			//				ic=cumulIC_BN(ic,currentBlock.getSpecies().size());				
			//				icExpr=ic.compareTo(BigDecimal.ZERO)==0? "false":"true";
			//			}




			ISpecies reducedSpecies;
			reducedSpecies = new Species(nameRep, blockRepresentative.getOriginalName(),i, ic,/*currentBlock.computeBlockConcentrationExpr()*/icExpr,blockRepresentative.isAlgebraic());
			reducedBN.addSpecies(reducedSpecies);
			if(bn.isMultiValued()) {
				int aggrMax = aggregatedMax(currentBlock.getSpecies(),bn,aggregationFunction);
				reducedBN.setMax(reducedSpecies, aggrMax);
			}

			reducedSpecies.addCommentLines(currentBlock.computeBlockComment());
			correspondenceBlock_ReducedSpecies.put(currentBlock, reducedSpecies);
		}

		for( Entry<IBlock, ISpecies> entry : correspondenceBlock_ReducedSpecies.entrySet()) {
			IBlock currentBlock=entry.getKey();
			//ISpecies reducedSpecies=entry.getValue();

			IUpdateFunction sum =computeUpdateFunctionSumPreservingRepresentative(bn,currentBlock,op,opArith,partition, correspondenceBlock_ReducedSpecies, speciesNameToSpecies,aggregationFunction);
//			if(simplify) {
//				Expr z3Sum = sum.toZ3(ctx, speciesNameToSpecies, speciesToODENames,realSort);
//				Expr z3SumSimpl=z3Sum.simplify();
//				IUpdateFunction sumSimplified=compiler.toUpdateFunction(z3SumSimpl);
//				correspondenceBlock_sumOfUpdateFunctions.put(currentBlock, sumSimplified);
//			}
//			else {
//				correspondenceBlock_sumOfUpdateFunctions.put(currentBlock, sum);
//			}
			correspondenceBlock_sumOfUpdateFunctions.put(currentBlock, sum);
		}

		//Default block for all reduced species
		IBlock uniqueBlock = new Block();
		IPartition trivialPartition = new Partition(uniqueBlock,reducedBN.getSpecies().size());
		//uniqueBlock.setPartition(trivialPartition);
		for(ISpecies reducedSpecies : reducedBN.getSpecies()){
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			uniqueBlock.addSpecies(reducedSpecies);
		}
		//trivialPartition.changeNumberOfSpecies(reducedCRN.getSpecies().size());

		for(Entry<IBlock, ISpecies> entry :correspondenceBlock_ReducedSpecies.entrySet()) {
			ISpecies reducedSpecies = entry.getValue();
			IBlock currentBlock=entry.getKey();
			IUpdateFunction sumUpdateFunction = correspondenceBlock_sumOfUpdateFunctions.get(currentBlock);
			reducedBN.addUpdateFunction(reducedSpecies.getName(), sumUpdateFunction);
		}

//		if(simplify) {
//			dispose();
//		}

		return new BNandPartition(reducedBN, trivialPartition);
	}


	/**
	 * This method computes the 'aggregated IC' of a block for the considered aggregation function.
	 *
	 * @param block
	 * @return
	 */
	private StringAndBigDecimal aggregatedIC(IBlock block) {
		if(	aggregationFunction.equals(FBEAggregationFunctions.OR)||
			aggregationFunction.equals(FBEAggregationFunctions.AND)||
			aggregationFunction.equals(FBEAggregationFunctions.XOR)||
			aggregationFunction.equals(FBEAggregationFunctions.PLUS)){
			
			BigDecimal sum= block.getBlockConcentration();
			if(aggregationFunction.equals(FBEAggregationFunctions.OR)){
				if(sum.compareTo(BigDecimal.ZERO)>0) {
					return new StringAndBigDecimal("1", BigDecimal.ONE);
				}
				return new StringAndBigDecimal("0", BigDecimal.ZERO);
			}
			else if(aggregationFunction.equals(FBEAggregationFunctions.AND)){
				if(sum.compareTo(BigDecimal.valueOf(block.getSpecies().size()))==0) {
					return new StringAndBigDecimal("1", BigDecimal.ONE);
				}
				return new StringAndBigDecimal("0", BigDecimal.ZERO);
			}
			else if(aggregationFunction.equals(FBEAggregationFunctions.XOR)){
				if(sum.compareTo(BigDecimal.ONE)==0) {
					return new StringAndBigDecimal("1", BigDecimal.ONE);
				}
				return new StringAndBigDecimal("0", BigDecimal.ZERO);
			} 
			else {
				//FBEAggregationFunctions.PLUS
				return new StringAndBigDecimal(sum.toPlainString(), sum);
			}
		}
		else if(aggregationFunction.equals(FBEAggregationFunctions.TIMES)) {
			BigDecimal prod = BigDecimal.ONE;
			for(ISpecies sp : block.getSpecies()) {
				prod = prod.multiply(sp.getInitialConcentration());
			}
			return new StringAndBigDecimal(prod.toPlainString(), prod);
		}
		else if(aggregationFunction.equals(FBEAggregationFunctions.MIN)||aggregationFunction.equals(FBEAggregationFunctions.MAX)) {
			BigDecimal chosen = block.getSpecies().iterator().next().getInitialConcentration();
			for(ISpecies sp : block.getSpecies()) {
				if(aggregationFunction.equals(FBEAggregationFunctions.MIN)) {
					chosen = chosen.min(sp.getInitialConcentration());
				}
				else {
					chosen = chosen.max(sp.getInitialConcentration());
				}
			}
			return new StringAndBigDecimal(chosen.toPlainString(), chosen);
		}
		else {
			throw new UnsupportedOperationException("Unsupportd FBE/FME aggregation function "+aggregationFunction);
		}
	}

	/**
	 * For mv networks only. To be used to compute the max value of a reduced species
	 * @param species all species to consider 
	 * @param originalBN the BN from which to take the max of each species
	 * @param aggregationFunction the aggr func to use
	 * @return the result of aggregationFunction on all max of the species
	 */
	protected static int aggregatedMax(Collection<ISpecies> species, IBooleanNetwork originalBN, FBEAggregationFunctions aggregationFunction) {
		if(aggregationFunction.equals(FBEAggregationFunctions.PLUS)) {
			int sum = 0;
			for(ISpecies sp : species) {
				sum = sum + originalBN.getNameToMax(sp.getName());
			}
			return sum;
		}
		else if(aggregationFunction.equals(FBEAggregationFunctions.TIMES)) {
			int prod = 1;
			for(ISpecies sp : species) {
				prod = prod * originalBN.getNameToMax(sp.getName());
			}
			return prod;
		}
		else if(aggregationFunction.equals(FBEAggregationFunctions.MIN)||aggregationFunction.equals(FBEAggregationFunctions.MAX)) {
			int chosen = originalBN.getNameToMax(species.iterator().next().getName());
			for(ISpecies sp : species) {
				if(aggregationFunction.equals(FBEAggregationFunctions.MIN)) {
					chosen = Math.min(chosen,originalBN.getNameToMax(sp.getName()));
				}
				else {
					chosen = Math.max(chosen,originalBN.getNameToMax(sp.getName()));
				}
			}
			return chosen;
		}
		else {
			throw new UnsupportedOperationException("Unsupportd FBE/FME aggregation function "+aggregationFunction);
		}
	}



	private static IUpdateFunction computeUpdateFunctionSumPreservingRepresentative(IBooleanNetwork bn, IBlock currentBlock, BooleanConnector op,ArithmeticConnector opArith,
			IPartition partition,
			LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,
			HashMap<String, ISpecies> speciesNameToOriginalSpecies, FBEAggregationFunctions aggregationFunction
			) {
		IUpdateFunction sum=null;

		for(ISpecies sp : currentBlock.getSpecies()) {
			IUpdateFunction c= bn.getUpdateFunctions().get(sp.getName());
			c=c.cloneReplacingNorRepresentativeWithNeutral(partition, correspondenceBlock_ReducedSpecies, speciesNameToOriginalSpecies, aggregationFunction,bn);
			if(sum==null) {
				sum=c;
				/*if(crn.isMultiValued()) {
					ArrayList<IUpdateFunction> addends=new ArrayList<>(1);
					addends.add(c);
					sum=new REMOVE_MVUpdateFunctionSum(addends);
				}
				else {
					sum=c;
				}*/
			}
			else {
				if(bn.isMultiValued()) {
					//((REMOVE_MVUpdateFunctionSum)sum).add(c);
					sum=new BinaryExprIUpdateFunction(sum,c,opArith);
				}
				else {
					sum = new BooleanUpdateFunctionExpr(sum, c, op);
				}	
			}
		}
		return sum;
	}


}
