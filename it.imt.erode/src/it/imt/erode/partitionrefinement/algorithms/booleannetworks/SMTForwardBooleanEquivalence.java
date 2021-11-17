package it.imt.erode.partitionrefinement.algorithms.booleannetworks;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.ui.console.MessageConsoleStream;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Sort;
import com.microsoft.z3.Status;
import com.microsoft.z3.Symbol;
import com.microsoft.z3.Z3Exception;

import it.imt.erode.auxiliarydatastructures.DoubleAndStatus;
import it.imt.erode.auxiliarydatastructures.IntegerAndPartition;
import it.imt.erode.auxiliarydatastructures.PartitionAndStringAndBoolean;
import it.imt.erode.booleannetwork.auxiliarydatastructures.BNandPartition;
import it.imt.erode.booleannetwork.implementations.BooleanNetwork;
import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;
import it.imt.erode.booleannetwork.updatefunctions.ArithmeticConnector;
import it.imt.erode.booleannetwork.updatefunctions.BinaryExprIUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.BooleanUpdateFunctionExpr;
import it.imt.erode.booleannetwork.updatefunctions.FalseUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.TrueUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.UpdateFunctionCompiler;
import it.imt.erode.booleannetwork.updatefunctions.ValUpdateFunction;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.Terminator;

import it.imt.erode.crn.implementations.Species;

import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.symbolic.constraints.BooleanConnector;

import it.imt.erode.importing.z3Importer;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.CRNBisimulationsNAry;

public class SMTForwardBooleanEquivalence {



	private FBEAggregationFunctions aggregationFunction;

	private HashMap<String, String> cfg;

	//private BoolExpr positivePopulationsAssertion;
	//private BoolExpr allConstraintAssertion;
	private Context ctx;
	private /*static*/ Solver solver;
	boolean initialized=false;
	private HashMap<ISpecies, /*Bool*/Expr> speciesToODENames;
	private HashMap<ISpecies, /*Bool*/Expr> speciesToODEsDef;
	//private BoolExpr allODEsDef;
	//private HashMap<ISpecies, HashSet<ISpecies>> speciesInTheODEs;

	private HashMap<IBlock,/*Bool*/Expr> odeSums;
	private Sort sort;

	public static final boolean SHOWTIMEATEACHSTEP = false;
	public static final boolean DOONLYCHECKSWITHWHOLEPARTITION = false;
	public static final int MAXINNERITERATIONS = 30000;//Integer.MAX_VALUE;
	
	/**
	 * For MV networks, I want to explicitly state the domain of each species (from 0 to max)
	 */
	private BoolExpr speciesDomainsAssertion;

	private double totalSMTChecksSeconds=0.0;
	private double initSMTTime=0.0;
	private List<Double> smtChecksSecondsAtStep;
	public List<Double> getSMTChecksSecondsAtStep(){
		return smtChecksSecondsAtStep;
	}
	private HashMap<ISpecies, /*Bool*/Expr> speciesToPopulation;
	//private HashMap<String, BoolExpr> symbParNameToSymbParZ3;
	private boolean simplify;

	public SMTForwardBooleanEquivalence(FBEAggregationFunctions aggrFunc,boolean simplify) {
		this.aggregationFunction=aggrFunc;
		this.simplify=simplify;
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
	 */
	public PartitionAndStringAndBoolean computeOFLsmt(IBooleanNetwork bn, IPartition partition, boolean verbose,MessageConsoleStream out, BufferedWriter bwOut,boolean print, Terminator terminator) throws Z3Exception, IOException{

		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"FBE Reducing: "+bn.getName()+" with aggregation function "+aggregationFunction+" exploiting Microsoft z3");
		}
		else {
			CRNReducerCommandLine.print(out,bwOut," using "+aggregationFunction+"...");
		}


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

		IntegerAndPartition iterationsAndPartition = refineFBE(bn,obtainedPartition,verbose,begin,out,bwOut,terminator);
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
	private IntegerAndPartition refineFBE(IBooleanNetwork bn, IPartition partition,boolean verbose,long begin,MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator) throws Z3Exception {

		partition.initializeAllBST();
		odeSums = new HashMap<>(partition.size());

		//remove blocks-sums of splitted blocks
		//compute list of new blocks, and compute sums only for them

		int iteration=0;
		int totalInnerIteration=0;
		int prevPartitionSize;
		Symbol redistributorAppSymbol = ctx.mkSymbol("app");
		Expr redistributorApp = ctx.mkConst(redistributorAppSymbol, sort);

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
			/*Bool*/Expr[] sumsOfTheBlocks = new /*Bool*/Expr[partition.size()];
			int b=0;
			while(currentSplitterBlock!=null){
				if(Terminator.hasToTerminate(terminator)){
					break;
				}
				/*Bool*/Expr odeSumOfCurrentBlock = odeSums.get(currentSplitterBlock);
				if(odeSumOfCurrentBlock==null){
					odeSumOfCurrentBlock=computeOdeSum(currentSplitterBlock);
				}
				sumsOfTheBlocks[b]=odeSumOfCurrentBlock;
				b++;
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
					totalInnerIteration=refineBlockDecomposeBinaryFBEWithOneSplitterAtTime(blockToSplit,bn,partition,/*,redistributor,*/redistributorApp,/*oneMinusRedistributor*/
							sumsOfTheBlocks,verbose,partitionSizeOfPrevDoWhileIter/*prevPartitionSize*/,out,bwOut,terminator,totalInnerIteration);
					//refineBlockAllSplittersAtOnce(blockToSplit,crn,partition,redistributor,redistributorApp,oneMinusRedistributor,sumsOfTheBlocks,verbose,prevPartitionSize);				

					if(totalInnerIteration >= MAXINNERITERATIONS) {
						break;
					}
					//I finished to partition the block

					/*
				//Add the partition of the block to the new refined partition
				IBlock current = partitionOfBlock.getFirstBlock();
				while(current!=null){
					refinedPartition.add(current);
					current.updatePartition(refinedPartition);
					current=current.getNext();
				}*/


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
			CRNReducerCommandLine.println(out,bwOut," INTERRUPTED after "+totalInnerIteration+" SMT checks due to limit on max SMT checks ("+MAXINNERITERATIONS+") ");
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


	private int refineBlockDecomposeBinaryFBEWithOneSplitterAtTime(IBlock blockToSplit,IBooleanNetwork crn, IPartition partition, /*ArithExpr redistributor,*/ /*Bool*/Expr redistributorApp, /*ArithExpr oneMinusRedistributor,*/ 
			/*Bool*/Expr[] sumsOfTheBlocks, boolean verbose, int sizeOfPartiton,MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator, int totalInnerIteration) throws Z3Exception {
		int indexOfSplitterBlock=0;
		//int innerIteration=0;
		//IBlock lastBlockOfPartition = partition.getLastBlock();
		//I add to the partition an empty block, which will be the first subblock of the block to split
		IBlock firstSubBlock = new Block();
		partition.add(firstSubBlock);

		//Partition partitionOfBlock = new Partition(firstSubBlock,crn.getSpecies().size());//it will actually have exactly the same number of species as blockToSplit
		Iterator<ISpecies> iterator = blockToSplit.getSpecies().iterator();
		firstSubBlock.addSpecies(iterator.next());

		boolean checkIfSpeciesBelongsToNextSubBlock=false;
		while(iterator.hasNext()){
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			ISpecies currentSpecies=iterator.next();
			/*Bool*/Expr currentSpeciesPop = speciesToPopulation.get(currentSpecies);
			//I now have to check if currentSpecies has to belong to an existing block of partitionOfBlock, or if a new one has to be create
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
				/*Bool*/Expr repOfCurrentSubBlockPop = speciesToPopulation.get(repOfCurrentSubBlock);
				indexOfSplitterBlock=0;
				Expr sumOfThetwo;
				if(isBooleanAggrFunc()) {
					sumOfThetwo = sumExpressions(new BoolExpr[]{(BoolExpr)currentSpeciesPop,(BoolExpr)repOfCurrentSubBlockPop});
				}
				else {
					sumOfThetwo = sumExpressions(new ArithExpr[]{(ArithExpr)currentSpeciesPop,(ArithExpr)repOfCurrentSubBlockPop});
				}
				IBlock currentSplitterBlock = partition.getFirstBlock();
				while(currentSplitterBlock!=null && !checkIfSpeciesBelongsToNextSubBlock && totalInnerIteration < MAXINNERITERATIONS){
					if(Terminator.hasToTerminate(terminator)){
						break;
					}
					/*Bool*/Expr cumulativeODEsOfCSB = sumsOfTheBlocks[indexOfSplitterBlock];
					/*Bool*/Expr swappedSum = cumulativeODEsOfCSB.substitute(repOfCurrentSubBlockPop, redistributorApp);
					// Replace currentSpecies with neutral element
					/*Bool*/Expr swappedSum_phi_ij = swappedSum.substitute(currentSpeciesPop, neutralElement());
					//Replace repOfCurrentSubBlock with sum of block
					swappedSum_phi_ij =  swappedSum_phi_ij.substitute(redistributorApp, sumOfThetwo);
					swappedSum_phi_ij=swappedSum_phi_ij.simplify();
					
					//I also have to do the other way around:
					//Replace currentSpecies with sum of block
					/*Bool*/Expr swappedSum_phi_ji = swappedSum.substitute(currentSpeciesPop, sumOfThetwo);
					//Replace repOfCurrentSubBlock with neutral element
					swappedSum_phi_ji =  swappedSum_phi_ji.substitute(redistributorApp, neutralElement());
					swappedSum_phi_ji=swappedSum_phi_ji.simplify();
					
					solver.reset();
					
					if(crn.isMultiValued()) {
						solver.add(speciesDomainsAssertion);
					}
					
					BoolExpr phi_ij__and__phi_ji = ctx.mkAnd(new BoolExpr[]{ctx.mkEq(cumulativeODEsOfCSB, swappedSum_phi_ij),ctx.mkEq(cumulativeODEsOfCSB, swappedSum_phi_ji)});
					solver.add(ctx.mkNot(phi_ij__and__phi_ji));
					//solver.add(ctx.mkNot(ctx.mkEq(cumulativeODEsOfCSB, swappedSum)));

					totalInnerIteration++;
					System.out.print(totalInnerIteration +" ");
					if(totalInnerIteration%50==0) {
						System.out.println();
					}
					if(totalInnerIteration==MAXINNERITERATIONS) {
						return totalInnerIteration;
					}

					DoubleAndStatus timeAndStatus = check(solver,verbose,SHOWTIMEATEACHSTEP,-1,-1,out,bwOut);
					if(totalInnerIteration==1) {
						System.out.println("First SMT check completed");
					}
					totalSMTChecksSeconds+= timeAndStatus.getDouble();
					smtChecksSecondsAtStep.add(timeAndStatus.getDouble());

					if(timeAndStatus.getStatus()==Status.UNKNOWN){
						CRNReducerCommandLine.println(out,bwOut,"z3 returned unknown. This is the reason:");
						CRNReducerCommandLine.println(out,bwOut,solver.getReasonUnknown());
						throw new Z3Exception("z3 returned unknown. This is the reason:\n"+solver.getReasonUnknown());
						//System.exit(-1);
						//return null;
					}
					else if(timeAndStatus.getStatus()==Status.UNSATISFIABLE){
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
						//I cannot put currentSpecies in the block currentBlockOfPartitionOfTheBlock. I have to check the next block of partitionOfBlock
						checkIfSpeciesBelongsToNextSubBlock=true;
						//break;//exit while(currentSplitterBlock!=null)
					}
				}

				if(checkIfSpeciesBelongsToNextSubBlock){
					checkIfSpeciesBelongsToNextSubBlock=false;
					currentSubBlock = currentSubBlock.getNext();
					//If I do not have other subBlocks (of the partition of the block) to consider, I create a new one, and I put the species in it
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


	private /*Bool*/Expr computeOdeSum(IBlock block) throws Z3Exception {
		int s=0;
		Expr odeSum;
		
		if(isBooleanAggrFunc()) {
			BoolExpr[] odesOfBlock = new BoolExpr[block.getSpecies().size()];
			for (ISpecies species : block.getSpecies()) {
				odesOfBlock[s]=(BoolExpr)speciesToODEsDef.get(species);
				s++;
			}
			odeSum = sumExpressions(odesOfBlock);
		}
		else if(isArithAggrFunc()) {
			ArithExpr[] odesOfBlock = new ArithExpr[block.getSpecies().size()];
			for (ISpecies species : block.getSpecies()) {
				odesOfBlock[s]=(ArithExpr)speciesToODEsDef.get(species);
				s++;
			}
			odeSum = sumExpressions(odesOfBlock);
		} 
		else {
			throw new UnsupportedOperationException("Not supported FBE aggregation function: "+aggregationFunction);
		}

		odeSum=odeSum.simplify();
		odeSums.put(block, odeSum);
		return odeSum; 
	}

	private boolean isBooleanAggrFunc() {
		if(aggregationFunction.equals(FBEAggregationFunctions.OR)||aggregationFunction.equals(FBEAggregationFunctions.AND)) {
			return true;
		}
		else {
			return false;
		} 
	}
	private boolean isArithAggrFunc() {
		if(aggregationFunction.equals(FBEAggregationFunctions.PLUS)) {
			return true;
		}
		else {
			return false;
		} 
	}

	private BoolExpr sumExpressions(BoolExpr[] expressions) {
		BoolExpr odeSum=null;
		if(aggregationFunction.equals(FBEAggregationFunctions.OR)) {
			odeSum = ctx.mkOr(expressions);
		}
		else if(aggregationFunction.equals(FBEAggregationFunctions.AND)) {
			odeSum = ctx.mkAnd(expressions);
		} 
		else {
			throw new UnsupportedOperationException("Not supported FBE aggregation function: "+aggregationFunction);
		}
		return odeSum;
	}
	private ArithExpr sumExpressions(ArithExpr[] expressions) {
		ArithExpr odeSum=null;
		if(aggregationFunction.equals(FBEAggregationFunctions.PLUS)) {
			odeSum = ctx.mkAdd(expressions);
		}
		else {
			throw new UnsupportedOperationException("Not supported FBE aggregation function: "+aggregationFunction);
		}
		return odeSum;
	}

	private /*Bool*/Expr neutralElement() {
		if(aggregationFunction.equals(FBEAggregationFunctions.OR)) {
			return ctx.mkFalse();
		}
		else if(aggregationFunction.equals(FBEAggregationFunctions.AND)) {
			return ctx.mkTrue();
		}
		else if(aggregationFunction.equals(FBEAggregationFunctions.PLUS)) {
			return ctx.mkInt(0);
		}
		else {
			throw new UnsupportedOperationException("Not supported FBE/FME aggregation function: "+aggregationFunction);
		}
	}

	public static IUpdateFunction neutralElementUpdFunc(FBEAggregationFunctions aggregationFunction) {
		if(aggregationFunction.equals(FBEAggregationFunctions.OR)) {
			return new FalseUpdateFunction();
		}
		else if(aggregationFunction.equals(FBEAggregationFunctions.AND)) {
			return new TrueUpdateFunction();
		} 
		else if(aggregationFunction.equals(FBEAggregationFunctions.PLUS)) {
			return new ValUpdateFunction(0);
		}
		else {
			throw new UnsupportedOperationException("Not supported FBE aggregation function: "+aggregationFunction);
		}
	}

	private void dispose() throws Z3Exception{
		if(ctx!=null){
			//ctx.dispose();
			ctx.close();
		}
		//positivePopulationsAssertion.dispose();
		//allConstraintAssertion.dispose();
		//solver.dispose();
		initialized=false;
	}

	private void init(IBooleanNetwork bn, boolean verbose,MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator) throws Z3Exception, IOException {

		smtChecksSecondsAtStep=new ArrayList<Double>();
		totalSMTChecksSeconds=0.0;
		initSMTTime=0.0;

		cfg = new HashMap<String, String>();
		cfg.put("model", "true");
		ctx = new Context(cfg);
		solver = ctx.mkSolver();

		if(SHOWTIMEATEACHSTEP){
			CRNReducerCommandLine.println(out,bwOut,"");
		}
	
		if(bn.isMultiValued()) {
			sort= ctx.mkIntSort();
		}
		else {
			sort= ctx.mkBoolSort();
		}

		speciesToODENames = new HashMap<>(bn.getSpecies().size());
		speciesToPopulation = new HashMap<>(bn.getSpecies().size());
		speciesToODEsDef = new HashMap<>(bn.getSpecies().size());

		BoolExpr[] speciesDomainsAssertions = null;
		int s=0;
		ArithExpr zero=null;
		if(bn.isMultiValued()) {
			speciesDomainsAssertions=new BoolExpr[bn.getSpecies().size()];
			zero = ctx.mkInt(0);
		}
		
		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<>(bn.getSpecies().size());
		for (ISpecies species : bn.getSpecies()) {
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			speciesNameToSpecies.put(species.getName(), species);
			//create the z3 constants for the populations
			String speciesNameInZ3 = z3Importer.nameInZ3(species);
			Symbol declNames = ctx.mkSymbol(speciesNameInZ3);
			/*Bool*/Expr population = ctx.mkConst(declNames,sort);
			speciesToPopulation.put(species,population);
			
			if(bn.isMultiValued()) {
				BoolExpr domain_min = ctx.mkGe((ArithExpr)population, zero);
				int max=bn.getNameToMax(species.getName());
				BoolExpr domain_max = ctx.mkLe((ArithExpr)population, ctx.mkInt(max));
				speciesDomainsAssertions[s]=ctx.mkAnd(domain_min,domain_max);
				s++;
			}

			/*Bool*/Expr ode = ctx.mkConst(declNames,sort);
			speciesToODENames.put(species, ode);
			//speciesToODEsDef.put(species, zero);
		}
		if(bn.isMultiValued())
			speciesDomainsAssertion = ctx.mkAnd(speciesDomainsAssertions);

		//		symbParNameToSymbParZ3 = new HashMap<>(crn.getSymbolicParameters().size());
		//		for(String symbPar : crn.getSymbolicParameters()){
		//			Symbol symbParZ3Decl = ctx.mkSymbol(symbPar);
		//			ArithExpr symbParZ3 = (ArithExpr) ctx.mkConst(symbParZ3Decl,ctx.mkRealSort());
		//			symbParNameToSymbParZ3.put(symbPar, symbParZ3);
		//		}
		//		int c=0;
		//		BoolExpr[] constraintAssertions = new BoolExpr[crn.getConstraints().size()];
		//		for(IConstraint constraintOnSymbPar : crn.getConstraints()){
		//			constraintAssertions[c] = constraintOnSymbPar.toZ3(ctx,symbParNameToSymbParZ3,crn, speciesNameToSpecies, speciesToPopulation);
		//			c++;
		//		}
		//		allConstraintAssertion = ctx.mkAnd(constraintAssertions);

		//		if(IMPOSEpositivePopulationsAssertion)
		//			solver.add(positivePopulationsAssertion);//solver.add(positivePopulationAssertions);
		//		solver.add(allConstraintAssertion);
		//		Status status = solver.check();
		//		if(status==Status.UNKNOWN){
		//			CRNReducerCommandLine.println(out,bwOut,"z3 returned unknown while validating the constraints. This is the reason:");
		//			CRNReducerCommandLine.println(out,bwOut,solver.getReasonUnknown());
		//			throw new Z3Exception("z3 returned unknown while validating the constraints. This is the reason:"+solver.getReasonUnknown());
		//		}
		//		else if(status==Status.UNSATISFIABLE){
		//			String message = "The constraints on the symbolic parameters are not satifiable.";
		//			//CRNReducerCommandLine.printWarning(out, true, msgDialogShower, message, DialogType.Error);
		//			throw new Z3Exception(message);
		//		}

		/* Provide the body of each ODE
		 * (assert (= ds0 (+ 0.0  (* 6.0  s0 -1) (* 2.0  s0 -1))))
		 */
		long beginODESBPopulation = System.currentTimeMillis();
		/*Bool*/Expr[] allODEsDefArray = new /*Bool*/Expr[bn.getSpecies().size()];
		int j=0;
		for (Entry<String, IUpdateFunction> entry : bn.getUpdateFunctions().entrySet()) {
			ISpecies species = speciesNameToSpecies.get(entry.getKey());
			IUpdateFunction updateFunction = entry.getValue();
			/*Bool*/Expr updateFunctionZ3 = updateFunction.toZ3(ctx, speciesNameToSpecies, speciesToPopulation);
			updateFunctionZ3=updateFunctionZ3.simplify();
			speciesToODEsDef.put(species, updateFunctionZ3);
			allODEsDefArray[j]=ctx.mkEq(speciesToODENames.get(species), updateFunctionZ3);
			j++;
		}
		//computeODEs(bn,terminator);
		//allODEsDef = ctx.mkAnd(allODEsDefArray);
		long endODESBPopulation = System.currentTimeMillis();
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"\nIterating the species to 1) convert the update functions in Z3 format and 2) build the assertions to connect the left-hand-side and the right-hand-side (x_i(t+1) = B(t)) required: "+String.format(CRNReducerCommandLine.MSFORMAT,String.valueOf(((double)(endODESBPopulation-beginODESBPopulation) / 1000.0)))+" (s)");
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
		else if(aggregationFunction.equals(FBEAggregationFunctions.PLUS)) {
			opArith=ArithmeticConnector.SUM;
		}
		else {
			throw new UnsupportedOperationException("Unsupported aggregation function: "+aggregationFunction);
		}

		UpdateFunctionCompiler compiler=null;
		if(simplify) {
			init(bn,false,out,bwOut,terminator);
			compiler = new UpdateFunctionCompiler(bn, speciesToPopulation);
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
			BigDecimal ic = currentBlock.getBlockConcentration();
			
			String icExpr;
			if(bn.isMultiValued()) {
				icExpr=String.valueOf(ic);
			}
			else {
				ic=cumulIC_BN(ic,currentBlock.getSpecies().size());				
				icExpr=ic.compareTo(BigDecimal.ZERO)==0? "false":"true";
			}
			

			ISpecies reducedSpecies;
			reducedSpecies = new Species(nameRep, blockRepresentative.getOriginalName(),i, ic,/*currentBlock.computeBlockConcentrationExpr()*/icExpr,blockRepresentative.isAlgebraic());
			reducedBN.addSpecies(reducedSpecies);
			if(bn.isMultiValued()) {
				reducedBN.setMax(reducedSpecies, bn.cumulMax(currentBlock.getSpecies()));
			}
			
			reducedSpecies.addCommentLines(currentBlock.computeBlockComment());
			correspondenceBlock_ReducedSpecies.put(currentBlock, reducedSpecies);
		}
		
		for( Entry<IBlock, ISpecies> entry : correspondenceBlock_ReducedSpecies.entrySet()) {
			IBlock currentBlock=entry.getKey();
			//ISpecies reducedSpecies=entry.getValue();
			
			IUpdateFunction sum =computeUpdateFunctionSumPreservingRepresentative(bn,currentBlock,op,opArith,partition, correspondenceBlock_ReducedSpecies, speciesNameToSpecies,aggregationFunction);
			if(simplify) {
				Expr z3Sum = sum.toZ3(ctx, speciesNameToSpecies, speciesToODENames);
				Expr z3SumSimpl=z3Sum.simplify();
				IUpdateFunction sumSimplified=compiler.toUpdateFunction(z3SumSimpl);
				correspondenceBlock_sumOfUpdateFunctions.put(currentBlock, sumSimplified);
			}
			else {
				correspondenceBlock_sumOfUpdateFunctions.put(currentBlock, sum);
			}
		}
		
//		for(int i=0;i<representativeSpecies.length;i++) {
//			if(Terminator.hasToTerminate(terminator)){
//				break;
//			}
//			ISpecies blockRepresentative = representativeSpecies[i];
//			IBlock currentBlock = partition.getBlockOf(blockRepresentative);
//			/*
//			ISpecies blockRepresentative = currentBlock.getRepresentative(CRNReducerCommandLine.COMPUTEREPRESENTATIVEBYMINOUTSIDEPARTITIONREFINEMENT);
//			 */
//			String nameRep=blockRepresentative.getName();
//			BigDecimal ic = currentBlock.getBlockConcentration();
//			String icExpr="false";
//			if(ic.compareTo(BigDecimal.ZERO)>0) {
//				ic=BigDecimal.ONE;
//				icExpr="true";
//			}
//
//			ISpecies reducedSpecies;
//			reducedSpecies = new Species(nameRep, blockRepresentative.getOriginalName(),i, ic,/*currentBlock.computeBlockConcentrationExpr()*/icExpr,blockRepresentative.isAlgebraic());
//			reducedBN.addSpecies(reducedSpecies);
//
//			reducedSpecies.addCommentLines(currentBlock.computeBlockComment());
//			correspondenceBlock_ReducedSpecies.put(currentBlock, reducedSpecies);
//
//			IUpdateFunction sum =computeUpdateFunctionSumPreservingRepresentative(bn,currentBlock,op,partition, correspondenceBlock_ReducedSpecies, speciesNameToSpecies,aggregationFunction);
//			if(simplify) {
//				BoolExpr z3Sum = sum.toZ3(ctx, speciesNameToSpecies, speciesToODENames);
//				BoolExpr z3SumSimpl=(BoolExpr)z3Sum.simplify();
//				IUpdateFunction sumSimplified=compiler.toUpdateFunction(z3SumSimpl);
//				correspondenceBlock_sumOfUpdateFunctions.put(currentBlock, sumSimplified);
//			}
//			else {
//				correspondenceBlock_sumOfUpdateFunctions.put(currentBlock, sum);
//			}
//
//			/*
//			currentBlock=currentBlock.getNext();
//			i++;
//			 */
//		}



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

		if(simplify) {
			dispose();
		}

		return new BNandPartition(reducedBN, trivialPartition);
	}

	/**
	 * for boolean networks (no multivalued). This method transforms the cumulative ic of a block in 1 (true) or 0 (false) depending on the aggregation function 
	 * @param cumulIC
	 * @param spInTheBlock
	 * @return
	 */
	private BigDecimal cumulIC_BN(BigDecimal cumulIC, int spInTheBlock) {
		if(this.aggregationFunction.equals(FBEAggregationFunctions.OR)) {
			if(cumulIC.compareTo(BigDecimal.ZERO)>0) {
				return BigDecimal.ONE;
			}
			return BigDecimal.ZERO;
		}
		else if(this.aggregationFunction.equals(FBEAggregationFunctions.AND)) {
			if(cumulIC.compareTo(BigDecimal.valueOf(spInTheBlock))==0) {
				return BigDecimal.ONE;
			}
			return BigDecimal.ZERO;
		} 
		return null;
	}

	private static IUpdateFunction computeUpdateFunctionSumPreservingRepresentative(IBooleanNetwork crn, IBlock currentBlock, BooleanConnector op,ArithmeticConnector opArith,
			IPartition partition,
			LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies,
			HashMap<String, ISpecies> speciesNameToOriginalSpecies, FBEAggregationFunctions aggregationFunction
			) {
		IUpdateFunction sum=null;
		
		for(ISpecies sp : currentBlock.getSpecies()) {
			IUpdateFunction c= crn.getUpdateFunctions().get(sp.getName());
			c=c.cloneReplacingNorRepresentativeWithNeutral(partition, correspondenceBlock_ReducedSpecies, speciesNameToOriginalSpecies, aggregationFunction);
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
				if(crn.isMultiValued()) {
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
