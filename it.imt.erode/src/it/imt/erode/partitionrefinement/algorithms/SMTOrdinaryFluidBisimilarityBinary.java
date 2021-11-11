package it.imt.erode.partitionrefinement.algorithms;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.ui.console.MessageConsoleStream;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Status;
import com.microsoft.z3.Symbol;
import com.microsoft.z3.Z3Exception;

import it.imt.erode.auxiliarydatastructures.CRNandPartition;
import it.imt.erode.auxiliarydatastructures.DoubleAndStatus;
import it.imt.erode.auxiliarydatastructures.IntegerAndPartition;
import it.imt.erode.auxiliarydatastructures.PartitionAndStringAndBoolean;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.implementations.CRN;
import it.imt.erode.crn.implementations.CRNReaction;
import it.imt.erode.crn.implementations.CRNReactionArbitraryGUI;
import it.imt.erode.crn.implementations.CRNReactionArbitraryMatlab;
import it.imt.erode.crn.implementations.Species;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.symbolic.constraints.IConstraint;
import it.imt.erode.importing.z3Importer;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;

public class SMTOrdinaryFluidBisimilarityBinary {
	
	private static final boolean IMPOSEpositivePopulationsAssertion=false;
	
	
	private HashMap<String, String> cfg;
	
	private BoolExpr positivePopulationsAssertion;
	private BoolExpr allConstraintAssertion;
	private Context ctx;
	private /*static*/ Solver solver;
	boolean initialized=false;
	private HashMap<ISpecies, ArithExpr> speciesToODEsDef;
	private HashMap<ISpecies, HashSet<ISpecies>> speciesInTheODEs;

	private HashMap<IBlock,ArithExpr> odeSums;

	public static final boolean SHOWTIMEATEACHSTEP = false;
	public static final boolean DOONLYCHECKSWITHWHOLEPARTITION = false;
	public static final int MAXINNERITERATIONS = 30000;//Integer.MAX_VALUE;

	private double totalSMTChecksSeconds=0.0;
	private double initSMTTime=0.0;
	private List<Double> smtChecksSecondsAtStep;
	public List<Double> getSMTChecksSecondsAtStep(){
		return smtChecksSecondsAtStep;
	}
	private HashMap<ISpecies, ArithExpr> speciesToPopulation;
	private HashMap<String, ArithExpr> symbParNameToSymbParZ3;

	/**
	 * 
	 * @param crn
	 * @param partition the partition to be refined. It is not modified, but instead a new one is created and returned
	 * @param verbose
	 * @param terminator 
	 * @return
	 * @throws Z3Exception 
	 * @throws IOException 
	 */
	public PartitionAndStringAndBoolean computeOFLsmt(ICRN crn, IPartition partition, boolean verbose,MessageConsoleStream out, BufferedWriter bwOut,boolean print, Terminator terminator) throws Z3Exception, IOException{

		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"FDE Reducing: "+crn.getName()+" exploiting Microsoft z3");
		}

		IPartition obtainedPartition = partition.copy();

		if(verbose){
			String blocks = " blocks";
			if(obtainedPartition.size()==1){
				blocks = " block";
			}
			CRNReducerCommandLine.println(out,bwOut,"Before partitioning we have "+ obtainedPartition.size() + blocks +" and "+crn.getSpecies().size()+ " species.");
		}

		long begin = System.currentTimeMillis();
		long beginInit = System.currentTimeMillis();
		init(crn,verbose,out,bwOut,terminator);
		long endInit = System.currentTimeMillis();
		initSMTTime = (double)(endInit-beginInit) / 1000.0;
		if(SHOWTIMEATEACHSTEP){	
			CRNReducerCommandLine.println(out,bwOut,"Init requred: "+String.format(CRNReducerCommandLine.MSFORMAT,(initSMTTime))+" (s)");
		}

		IntegerAndPartition iterationsAndPartition = refineOFL(crn,obtainedPartition,verbose,begin,out,bwOut,terminator);
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
			CRNReducerCommandLine.println(out,bwOut,"OFL Partitioning completed. From "+ crn.getSpecies().size() +" species to "+ obtainedPartition.size() + " blocks. Time necessary: "+(end-begin)+ " (ms)");
			CRNReducerCommandLine.println(out,bwOut,"");
		}
		return new PartitionAndStringAndBoolean(obtainedPartition, smtTimes,succeeded);
	}


	/**
	 * 
	 * @param crn
	 * @param partition the partition to be refined. Note that the partition is modified, thus first invoke copy if you want to preserve it.
	 * @param terminator 
	 * @param labels
	 * @throws Z3Exception 
	 */
	private IntegerAndPartition refineOFL(ICRN crn, IPartition partition,boolean verbose,long begin,MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator) throws Z3Exception {

		partition.initializeAllBST();
		odeSums = new HashMap<IBlock, ArithExpr>(partition.size());
		
		//remove blocks-sums of splitted blocks
		//compute list of new blocks, and compute sums only for them

		int iteration=0;
		int totalInnerIteration=0;
		int prevPartitionSize;
		//int ciao=0;

		Symbol redistributorSymbol = ctx.mkSymbol("red");
		ArithExpr redistributor = (ArithExpr) ctx.mkConst(redistributorSymbol, ctx.getRealSort());
		//ArithExpr redistributor = ctx.mkReal("0.5");
		ArithExpr oneMinusRedistributor = ctx.mkSub(new ArithExpr[]{ctx.mkReal(1),redistributor});
		Symbol redistributorAppSymbol = ctx.mkSymbol("app");
		ArithExpr redistributorApp = (ArithExpr) ctx.mkConst(redistributorAppSymbol, ctx.getRealSort());

		ArrayList<IBlock> sumsToBeRemoved = new ArrayList<IBlock>();

		//IBlock firstBlockFromWhichStartComputingCumulativeODEs = partition.getFirstBlock();

		int partitionSizeOfPrevDoWhileIter = partition.size();
		
		do{
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			//I keep trace of the size and of the last element of the partition, so to ignore newly created subblocks. They will be considered in the next invocation of this method.
			partitionSizeOfPrevDoWhileIter = partition.size();//NEW
			prevPartitionSize = partition.size();
			IBlock lastBlockOfOfPartition = partition.getLastBlock();
			iteration++;
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"iteration: "+iteration+" blocks:"+partition.size());
			}

			//Remove the sums of the ODEs of removed blocks
			for (IBlock toBeRemoved : sumsToBeRemoved) {
				odeSums.remove(toBeRemoved);
				//ArithExpr sum = odeSums.remove(toBeRemoved);
				//sum.dispose();
			}
			//System.gc();
			sumsToBeRemoved=new ArrayList<IBlock>();//Reinitialize the list of blocks whose cumulative ODEs have to be removed.

			IBlock currentSplitterBlock = partition.getFirstBlock();
			ArithExpr[] sumsOfTheBlocks = new ArithExpr[partition.size()];
			int b=0;
			while(currentSplitterBlock!=null){
				if(Terminator.hasToTerminate(terminator)){
					break;
				}
				ArithExpr odeSumOfCurrentBlock = odeSums.get(currentSplitterBlock);
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
					//New
					if(blockToSplit.equals(lastBlockOfOfPartition)){
						blockToSplit=null;
					}
					else {
						blockToSplit=blockToSplit.getNext();
					}
					//blockToSplit=blockToSplit.getNext();
				}
				else{
					//IPartition partitionOfBlock = refineBlockDecomposeBinaryFDEWithOneSplitterAtTime(blockToSplit,crn,partition,redistributor,redistributorApp,oneMinusRedistributor,sumsOfTheBlocks,verbose);
					//IPartition partitionOfBlock = refineBlockAllSplittersAtOnce(blockToSplit,crn,partition,redistributor,redistributorApp,oneMinusRedistributor,sumsOfTheBlocks,verbose);
					//totalInnerIteration contains the sum of all inneriterations
					totalInnerIteration=refineBlockDecomposeBinaryFDEWithOneSplitterAtTime(blockToSplit,crn,partition,redistributor,redistributorApp,oneMinusRedistributor,sumsOfTheBlocks,verbose,partitionSizeOfPrevDoWhileIter/*prevPartitionSize*/,out,bwOut,terminator,totalInnerIteration);
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
			
		System.out.println();

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


	private int refineBlockDecomposeBinaryFDEWithOneSplitterAtTime(IBlock blockToSplit,ICRN crn, IPartition partition, ArithExpr redistributor, ArithExpr redistributorApp, ArithExpr oneMinusRedistributor, 
			 ArithExpr[] sumsOfTheBlocks, boolean verbose, int sizeOfPartiton,MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator, int totalInnerIteration) throws Z3Exception {
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
			ArithExpr currentSpeciesPop = speciesToPopulation.get(currentSpecies);
			//I now have to check if currentSpecies has to belong to an existing block of partitionOfBlock, or if a new one has to be create
			boolean correctSubBlockFoundForCurrentSpecies=false;
			IBlock currentSubBlock = firstSubBlock;
			while(currentSubBlock!=null && !correctSubBlockFoundForCurrentSpecies && totalInnerIteration < MAXINNERITERATIONS){
				if(Terminator.hasToTerminate(terminator)){
					break;
				}
				ISpecies repOfCurrentSubBlock = currentSubBlock.getRepresentative();
				ArithExpr repOfCurrentSubBlockPop = speciesToPopulation.get(repOfCurrentSubBlock);
				indexOfSplitterBlock=0;
				ArithExpr sumOfThetwo = ctx.mkAdd(new ArithExpr[]{currentSpeciesPop,repOfCurrentSubBlockPop});
				IBlock currentSplitterBlock = partition.getFirstBlock();
				while(currentSplitterBlock!=null && !checkIfSpeciesBelongsToNextSubBlock && totalInnerIteration < MAXINNERITERATIONS){
					if(Terminator.hasToTerminate(terminator)){
						break;
					}
					//Rename repOfCurrentSubBlock with an auxilairy symbol (necessary, as in classic "swap function")
					//ArithExpr swappedSum = (ArithExpr) sumsOfTheBlocks[b].substitute(repOfCurrentSubBlockPop, redistributorApp);
					ArithExpr cumulativeODEsOfCSB = sumsOfTheBlocks[indexOfSplitterBlock];
					//ArithExpr cumulativeODEsOfCSB = odeSums.get(currentSplitterBlock);
					/*if(cumulativeODEsOfCSB==null){
						CRNReducerCommandLine.println(out,bwOut,"Problema");
					}*/
					ArithExpr swappedSum = (ArithExpr) cumulativeODEsOfCSB.substitute(repOfCurrentSubBlockPop, redistributorApp);
					// Replace currentSpecies with s * (currentSpecies + repOfCurrentSubBlock));
					swappedSum = (ArithExpr)swappedSum.substitute(currentSpeciesPop, ctx.mkMul(new ArithExpr[]{redistributor,sumOfThetwo}));
					//Replace repOfCurrentSubBlock with (1-s) * (currentSpecies + repOfCurrentSubBlock));
					swappedSum =  (ArithExpr)swappedSum.substitute(redistributorApp, ctx.mkMul(new ArithExpr[]{oneMinusRedistributor,sumOfThetwo}));
					swappedSum=(ArithExpr)swappedSum.simplify();
					solver.reset();
					if(IMPOSEpositivePopulationsAssertion) {
						solver.add(positivePopulationsAssertion);
					}
					solver.add(allConstraintAssertion);
					//TODO: test: what happens if I don't specify the value for the redistributor s?
					solver.add(ctx.mkEq(redistributor, ctx.mkReal("0.5")));
					solver.add(ctx.mkNot(ctx.mkEq(cumulativeODEsOfCSB, swappedSum)));
					
					totalInnerIteration++;
					System.out.print(totalInnerIteration +" ");
					if(totalInnerIteration%50==0) {
						System.out.println();
					}
					if(totalInnerIteration==MAXINNERITERATIONS) {
						//System.out.println(totalInnerIteration);
						return totalInnerIteration;
					}
//					else if(totalInnerIteration==33) {
//						System.out.println(totalInnerIteration);
//					}
					

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

	
	@SuppressWarnings("unused")
	private void refineBlockAllSplittersAtOnce(IBlock blockToSplit,ICRN crn, IPartition partition, ArithExpr redistributor, ArithExpr redistributorApp, ArithExpr oneMinusRedistributor, ArithExpr[] sumsOfTheBlocks, boolean verbose, int sizeOfPartiton,MessageConsoleStream out, BufferedWriter bwOut) throws Z3Exception {
		//I add to the partition an empty block, which will be the first subblock of the partition remove the block to split
		IBlock firstSubBlock = new Block();
		partition.add(firstSubBlock);
		//Partition partitionOfBlock = new Partition(firstSubBlock,crn.getSpecies().size());//it will actually have exactly the same number of species as blockToSplit
		//I add the first species of blockToSplit to firstSubBlock, and I create an iterator to iterate over all the other species in the block
		Iterator<ISpecies> iterator = blockToSplit.getSpecies().iterator();
		firstSubBlock.addSpecies(iterator.next());

		boolean checkIfSpeciesBelongsToNextSubBlock=false;
		while(iterator.hasNext()){
			ISpecies currentSpecies=iterator.next();
			ArithExpr currentSpeciesPop = speciesToPopulation.get(currentSpecies);
			//I now have to check if currentSpecies has to belong to an existing block of partitionOfBlock, or if a new one has to be created
			boolean correctSubBlockFoundForCurrentSpecies=false;
			//IBlock currentSubBlock = partitionOfBlock.getFirstBlock();
			IBlock currentSubBlock = firstSubBlock;
			BoolExpr[] conditionForEachSplitter = new BoolExpr[sizeOfPartiton];
			while(currentSubBlock!=null && !correctSubBlockFoundForCurrentSpecies){
				ISpecies repOfCurrentSubBlock = currentSubBlock.getRepresentative();
				ArithExpr repOfCurrentSubBlockPop = speciesToPopulation.get(repOfCurrentSubBlock);
				ArithExpr sumOfThetwo = ctx.mkAdd(new ArithExpr[]{currentSpeciesPop,repOfCurrentSubBlockPop});
				while(!checkIfSpeciesBelongsToNextSubBlock && !correctSubBlockFoundForCurrentSpecies){
					for(int b=0;b<sizeOfPartiton;b++){
						//Rename repOfCurrentSubBlock with an auxilairy symbol (necessary, as in classic "swap function")
						ArithExpr swappedSum = (ArithExpr) sumsOfTheBlocks[b].substitute(repOfCurrentSubBlockPop, redistributorApp); 
						// Replace currentSpecies with s * (currentSpecies + repOfCurrentSubBlock));
						swappedSum = (ArithExpr)swappedSum.substitute(currentSpeciesPop, ctx.mkMul(new ArithExpr[]{redistributor,sumOfThetwo}));
						//Replace repOfCurrentSubBlock with (1-s) * (currentSpecies + repOfCurrentSubBlock));
						swappedSum =  (ArithExpr)swappedSum.substitute(redistributorApp, ctx.mkMul(new ArithExpr[]{oneMinusRedistributor,sumOfThetwo}));
						conditionForEachSplitter[b]=ctx.mkEq(sumsOfTheBlocks[b], swappedSum);
					}
					
					solver.reset();
					if(IMPOSEpositivePopulationsAssertion)
						solver.add(positivePopulationsAssertion);
					solver.add(allConstraintAssertion);
					solver.add(ctx.mkEq(redistributor, ctx.mkReal("0.5")));
					solver.add(ctx.mkNot(ctx.mkAnd(conditionForEachSplitter)));
					

					DoubleAndStatus timeAndStatus = check(solver,verbose,SHOWTIMEATEACHSTEP,-1,-1,out,bwOut);
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
						//The two species are equal according to currentBlock.
						//The currentSpecies can be added to currentBlockOfPartitionOfTheBlock
						currentSubBlock.addSpecies(currentSpecies);
						//currentSubBlock = currentSubBlock.getNext();//What is this needed for?
						//break;exit while(currentSubBlock!=null && !correctSubBlockFound){
						correctSubBlockFoundForCurrentSpecies=true;
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
			}
		}	
		
	}

	private ArithExpr computeOdeSum(IBlock block) throws Z3Exception {
		int s=0;
		ArithExpr[] odesOfBlock = new ArithExpr[block.getSpecies().size()];
		for (ISpecies species : block.getSpecies()) {
			odesOfBlock[s]=speciesToODEsDef.get(species);
			s++;
		}
		ArithExpr odeSum = ctx.mkAdd(odesOfBlock);
		odeSum=(ArithExpr)odeSum.simplify();
		odeSums.put(block, odeSum);
		return odeSum; 
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
	
	private void init(ICRN crn, boolean verbose,MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator) throws Z3Exception, IOException {
		/*try {
			SMTExactFluidBisimilarity.checkNativeSources(getClass());
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}*/
		
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
		/*BoolExpr file = ctx.ParseSMTLIB2File("./z3Encodings/e2EFLCompactAssert.z3", null, null, null,null);
		Solver s = ctx.mkSolver();
		s.add(file);
		check(s);*/

		/*Declare a positive real constant per species
		(declare-const s0 Real)
		(assert (> s0 0.0))*/
		/*Declare one ODE per species. 
        (declare-const ds0 Real)
		 */
		
		speciesToPopulation = new HashMap<ISpecies, ArithExpr>(crn.getSpecies().size());
		speciesToODEsDef = new HashMap<ISpecies, ArithExpr>(crn.getSpecies().size());
		ArithExpr zero = ctx.mkReal("0.0");
		BoolExpr[] positivePopulationAssertions = new BoolExpr[crn.getSpecies().size()];
		int s=0;
		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<>(crn.getSpecies().size());
		for (ISpecies species : crn.getSpecies()) {
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			speciesNameToSpecies.put(species.getName(), species);
			//create the z3 constants for the populations
			String speciesNameInZ3 = z3Importer.nameInZ3(species);
			Symbol declNames = ctx.mkSymbol(speciesNameInZ3);
			ArithExpr population = (ArithExpr) ctx.mkConst(declNames,ctx.mkRealSort());
			speciesToPopulation.put(species,population);
			BoolExpr positivePop = ctx.mkGt(population, zero);
			positivePopulationAssertions[s] = positivePop;
			s++;
			speciesToODEsDef.put(species, zero);
		}
		if(IMPOSEpositivePopulationsAssertion)
			positivePopulationsAssertion = ctx.mkAnd(positivePopulationAssertions);

		symbParNameToSymbParZ3 = new HashMap<>(crn.getSymbolicParameters().size());
		for(String symbPar : crn.getSymbolicParameters()){
			Symbol symbParZ3Decl = ctx.mkSymbol(symbPar);
			ArithExpr symbParZ3 = (ArithExpr) ctx.mkConst(symbParZ3Decl,ctx.mkRealSort());
			symbParNameToSymbParZ3.put(symbPar, symbParZ3);
		}
		int c=0;
		BoolExpr[] constraintAssertions = new BoolExpr[crn.getConstraints().size()];
		for(IConstraint constraintOnSymbPar : crn.getConstraints()){
			constraintAssertions[c] = constraintOnSymbPar.toZ3(ctx,symbParNameToSymbParZ3,crn, speciesNameToSpecies, speciesToPopulation);
			c++;
		}
		allConstraintAssertion = ctx.mkAnd(constraintAssertions);
		
		if(IMPOSEpositivePopulationsAssertion)
			solver.add(positivePopulationsAssertion);//solver.add(positivePopulationAssertions);
		solver.add(allConstraintAssertion);
		Status status = solver.check();
		if(status==Status.UNKNOWN){
			CRNReducerCommandLine.println(out,bwOut,"z3 returned unknown while validating the constraints. This is the reason:");
			CRNReducerCommandLine.println(out,bwOut,solver.getReasonUnknown());
			throw new Z3Exception("z3 returned unknown while validating the constraints. This is the reason:"+solver.getReasonUnknown());
		}
		else if(status==Status.UNSATISFIABLE){
			String message = "The constraints on the symbolic parameters are not satifiable.";
			//CRNReducerCommandLine.printWarning(out, true, msgDialogShower, message, DialogType.Error);
			throw new Z3Exception(message);
		}
		
		/* Provide the body of each ODE
		 * (assert (= ds0 (+ 0.0  (* 6.0  s0 -1) (* 2.0  s0 -1))))
		 */
		long beginODESBPopulation = System.currentTimeMillis();
		
		computeODEs(crn,terminator);
		long endODESBPopulation = System.currentTimeMillis();
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"\nIterating the reactions to build the ODEs definitions required: "+String.format( CRNReducerCommandLine.MSFORMAT, (((double)(endODESBPopulation-beginODESBPopulation) / 1000.0)) )+" (s)");
		}

		long beginODEDefAssertions = System.currentTimeMillis();
		for(ISpecies species : crn.getSpecies()){	
			//It is worth doing this: it requires much more time to build the expressions (e.g. from less than a second to 150 secodns, but then the checks are faster, especially for the whole partition case )
			ArithExpr body = (ArithExpr)speciesToODEsDef.get(species).simplify();
			speciesToODEsDef.put(species,body);
		}
		long endODEDefAssertions = System.currentTimeMillis();
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"Building the assertions for the ODEs definitions required: "+String.format( CRNReducerCommandLine.MSFORMAT, (((double)(endODEDefAssertions-beginODEDefAssertions) / 1000.0)))+" (s)");
		}

		initialized=true;
	}
	
	
	private void computeODEs(ICRN crn, Terminator terminator) throws Z3Exception, IOException {

		speciesInTheODEs = new HashMap<ISpecies, HashSet<ISpecies>>(crn.getSpecies().size());
		HashMap<IComposite, ArithExpr> massActionExpressions = new HashMap<IComposite, ArithExpr>(/*crn.getReagents().size()*/);
		
		ISpecies[] speciesIdToSpecies = new ISpecies[crn.getSpecies().size()];
		crn.getSpecies().toArray(speciesIdToSpecies);
		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
		for (ISpecies species : crn.getSpecies()) {
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			speciesNameToSpecies.put(species.getName(), species);
		}
		
		
		for (ICRNReaction reaction : crn.getReactions()) {
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			addSpeciesInvolvedInTheODEs(reaction);
			ArithExpr rate=	SMTExactFluidBisimilarity.computez3RateExpression(ctx,speciesToPopulation,symbParNameToSymbParZ3,crn, massActionExpressions,reaction,speciesIdToSpecies,speciesNameToSpecies);
			IComposite netStochimetry = reaction.computeProductsMinusReagents();
			for(int i=0;i<netStochimetry.getNumberOfDifferentSpecies();i++){
				ISpecies species = netStochimetry.getAllSpecies(i);
				int stoc = netStochimetry.getMultiplicities(i);
				ArithExpr fluxExpr = ctx.mkMul(new ArithExpr[]{ ctx.mkReal(stoc), rate});
				ArithExpr odeDef = speciesToODEsDef.get(species);
				speciesToODEsDef.put(species, ctx.mkAdd(new ArithExpr[]{odeDef,fluxExpr}));
			}
		}
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
	public static CRNandPartition computeReducedCRNOrdinaryNonMassAction(ICRN crn,String name, IPartition partition, List<String> symbolicParameters, List<IConstraint> constraints,List<String> parameters,String commSymbol,MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator) throws IOException {
		ICRN reducedCRN = new CRN(name,symbolicParameters,constraints,parameters,crn.getMath(),out,bwOut);
		ISpecies[] speciesIdToSpecies= new ISpecies[crn.getSpecies().size()];
		crn.getSpecies().toArray(speciesIdToSpecies);
		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
		for (ISpecies species : crn.getSpecies()) {
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
		HashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies = new HashMap<IBlock, ISpecies>(partition.size());
		/*
		while(currentBlock!=null){
		 */
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
			
			ISpecies reducedSpecies;
			if(crn.isZeroSpecies(blockRepresentative)){
				reducedSpecies = reducedCRN.getCreatingIfNecessaryTheZeroSpecies(nameRep);
				reducedSpecies.setInitialConcentration(ic, currentBlock.computeBlockConcentrationExpr());
			}
			else{
				reducedSpecies = new Species(nameRep, blockRepresentative.getOriginalName(),i, ic,currentBlock.computeBlockConcentrationExpr(),blockRepresentative.isAlgebraic());
				reducedCRN.addSpecies(reducedSpecies);
			}			
			reducedSpecies.addCommentLines(currentBlock.computeBlockComment());
			correspondenceBlock_ReducedSpecies.put(currentBlock, reducedSpecies);
			/*
			currentBlock=currentBlock.getNext();
			i++;
			*/
		}
		//Default block for all reduced species
		IBlock uniqueBlock = new Block();
		IPartition trivialPartition = new Partition(uniqueBlock,reducedCRN.getSpecies().size());
		//uniqueBlock.setPartition(trivialPartition);
		for(ISpecies reducedSpecies : reducedCRN.getSpecies()){
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			uniqueBlock.addSpecies(reducedSpecies);
		}
		//trivialPartition.changeNumberOfSpecies(reducedCRN.getSpecies().size());

		List<ICRNReaction> reducedReactions = new ArrayList<ICRNReaction>();
		for (ICRNReaction reaction : crn.getReactions()) {
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			IComposite reducedReagents = CRNBisimulationsNAry.getNewCompositeReplaceSpeciesWithReducedOneOfBlock(reaction.getReagents(),partition,correspondenceBlock_ReducedSpecies);
			IComposite reducedProducts = CRNBisimulationsNAry.getNewCompositeReplaceSpeciesWithReducedOneOfBlock(reaction.getProducts(),partition,correspondenceBlock_ReducedSpecies);
			
			
			
			addToListReducedReactionsNonMassActionCRN(partition, reducedCRN, speciesIdToSpecies, speciesNameToSpecies,correspondenceBlock_ReducedSpecies, reducedReactions, reaction, reducedReagents, reducedProducts,true);
		}
					
		if(!Terminator.hasToTerminate(terminator)){
			CRN.collapseAndCombineAndAddReactions(reducedCRN,reducedReactions,out,bwOut);
		}
		/*if(CRNReducerCommandLine.collapseReactions){
			CRN.collapseAndCombineAndAddReactions(reducedCRN,reducedReactions,crn.isMassAction(),out,bwOut);
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
	
	protected static void addToListReducedReactionsNonMassActionCRN(IPartition partition, ICRN reducedCRN,
			ISpecies[] speciesIdToSpecies, HashMap<String, ISpecies> speciesNameToSpecies,
			HashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies, List<ICRNReaction> reducedReactions,
			ICRNReaction reaction, IComposite reducedReagents, IComposite reducedProducts,boolean scaleBySizeOfBlock) throws IOException {
		if(! reducedReagents.equals(reducedProducts)){
//			if(CRNReducerCommandLine.univoqueReagents){
//				reducedReagents = reducedCRN.addReagentsIfNew(reducedReagents);
//			}
//			if(CRNReducerCommandLine.univoqueProducts){
//				reducedProducts = reducedCRN.addProductIfNew(reducedProducts);
//			}
			
			if(reaction.hasArbitraryKinetics()){
				if(reaction instanceof CRNReactionArbitraryMatlab){
					CRNReactionArbitraryMatlab arb = (CRNReactionArbitraryMatlab)reaction;
					CRNReactionArbitraryMatlab reducedReaction = new CRNReactionArbitraryMatlab(reducedReagents, reducedProducts, arb.getRateExpression(), arb.getVarsName(),reaction.getID());
					reducedReaction.replaceSpeciesWithRepresentativeInRate(partition,correspondenceBlock_ReducedSpecies,speciesIdToSpecies,scaleBySizeOfBlock);
					reducedReactions.add(reducedReaction);
				}
				else if(reaction instanceof CRNReactionArbitraryGUI){
					CRNReactionArbitraryGUI arb = (CRNReactionArbitraryGUI)reaction;
					CRNReactionArbitraryGUI reducedReaction = new CRNReactionArbitraryGUI(reducedReagents, reducedProducts, arb.getRateExpression(),reaction.getID());
					reducedReaction.replaceSpeciesWithRepresentativeInRate(partition,correspondenceBlock_ReducedSpecies,speciesNameToSpecies,scaleBySizeOfBlock);
					reducedReactions.add(reducedReaction);
				} 
				else{
					throw new UnsupportedOperationException("Unsupported reaction:"+reaction);
				}
			}
			else{
				double scalingFactor = computeScalingFactor(reaction.getReagents(), partition);
				BigDecimal rate = reaction.getRate();
				String rateExpr = reaction.getRateExpression();
				if(scaleBySizeOfBlock && scalingFactor!=1.0){
					rate = rate.divide(BigDecimal.valueOf(scalingFactor),CRNBisimulationsNAry.getSCALE(),CRNBisimulationsNAry.RM);
					rateExpr = "("+rateExpr+")/"+scalingFactor;
				}
				ICRNReaction reducedReaction = new CRNReaction(rate, reducedReagents, reducedProducts, rateExpr,reaction.getID());
				reducedReactions.add(reducedReaction);
			}
		}
	}
	
	/*protected static void addToListOfReducedReactionsNonMassActionCRN(IPartition partition,
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
				throw new UnsupportedOperationException("This is not a massaction CRN. Use method: it.imt.erode.partitionrefinement.algorithms.SMTOrdinaryFluidBisimilarityBinary.computeReducedCRNOrdinaryNonMassAction"+reaction);
			}
			else{
				ICRNReaction reducedReaction = new CRNReaction(reaction.getRate(), reducedReagentsOfReaction, reducedProductsOfReaction, reaction.getRateExpression());
				//reducedCRN.addReaction(reducedReaction);
				//AbstractImporter.addToIncomingReactionsOfProducts(reducedReaction.getArity(),reducedProductsOfReaction, reducedReaction);
				reducedReactions.add(reducedReaction);
			}
		}
	}*/
	
	/**
	 * Returns the products of the sizes of the blocks of the reagents. We scale the rate of massaction reactions by this factor when reducing non-massaction reactions.
	 * @param originalReagents the reagents of the massaction reaction to be reduced 
	 * @param partition the partition of species used to reduce
	 * @return the products of the sizes of the blocks of the reagents
	 */
	public static double computeScalingFactor(IComposite originalReagents, IPartition partition) {
		double factor = 1;
		for(int i=0; i < originalReagents.getNumberOfDifferentSpecies(); i++){
			factor = factor * Math.pow(partition.getBlockOf(originalReagents.getAllSpecies(i)).getSpecies().size(),originalReagents.getMultiplicities(i));
		}
		return factor;
	}
	
/*
	private void computeODEs(ICRN crn) throws Z3Exception {

		ISpecies[] speciesIdToSpecies = new ISpecies[crn.getSpecies().size()];
		int s=0;
		for (ISpecies species : crn.getSpecies()) {
			speciesIdToSpecies[s]=species;
			s++;
		}
		speciesInTheODEs = new HashMap<ISpecies, HashSet<ISpecies>>(crn.getSpecies().size());
		HashMap<IComposite, ArithExpr> massActionExpressions = new HashMap<IComposite, ArithExpr>(crn.getReagents().size());
		for (ICRNReaction reaction : crn.getReactions()) {
			addSpeciesInvolvedInTheODEs(reaction);
			if((!reaction.hasHillKinetics()) && (!reaction.hasArbitraryKinetics())){
				ArithExpr massActionExpr = SMTExactFluidBisimilarity.getOrComputeProductOfReagents(ctx,massActionExpressions,reaction.getReagents(),speciesToPopulation);
				String rate = reaction.getRate().toPlainString();
				massActionExpr = ctx.mkMul(new ArithExpr[]{massActionExpr , ctx.mkReal(rate) });
				IComposite netStochimetry = reaction.computeProductsMinusReagents();
				for(int i=0;i<netStochimetry.getAllSpecies().length;i++){
					ISpecies species = netStochimetry.getAllSpecies()[i];
					int stoc = netStochimetry.getMultiplicities()[i];
					ArithExpr fluxExpr = ctx.mkMul(new ArithExpr[]{ ctx.mkReal(stoc), massActionExpr});

					ArithExpr odeDef = speciesToODEsDef.get(species);
					speciesToODEsDef.put(species, ctx.mkAdd(new ArithExpr[]{odeDef,fluxExpr}));
				}
			} else if(reaction.hasHillKinetics()){
				throw new UnsupportedOperationException("Unsupported reaction:"+reaction);
			}
			else if(reaction.hasArbitraryKinetics()){
				CRNReactionArbitrary arb = (CRNReactionArbitrary)reaction;
				ArithExpr rate = SMTExactFluidBisimilarity.computez3Rate(ctx,speciesToPopulation,arb.getRateLaw(),arb.getVarsName(),crn.getMath(),speciesIdToSpecies);
				IComposite netStochimetry = reaction.computeProductsMinusReagents();
				for(int i=0;i<netStochimetry.getAllSpecies().length;i++){
					ISpecies species = netStochimetry.getAllSpecies()[i];
					int stoc = netStochimetry.getMultiplicities()[i];
					ArithExpr fluxExpr = rate;
					if(stoc!=1){
						fluxExpr = ctx.mkMul(new ArithExpr[]{ ctx.mkReal(stoc), rate});
					}
					ArithExpr odeDef = speciesToODEsDef.get(species);
					speciesToODEsDef.put(species, ctx.mkAdd(new ArithExpr[]{odeDef,fluxExpr}));
				}
			}
			else{
				throw new UnsupportedOperationException("Unsupported reaction:"+reaction);
			}
		}
	}
	*/
	
	private void addSpeciesInvolvedInTheODEs(ICRNReaction reaction) {
		//ISpecies[] reagents = reaction.getReagents().getAllSpecies();
		//ISpecies[] products = reaction.getProducts().getAllSpecies();
		
		if(reaction.getReagents().getNumberOfDifferentSpecies()==1){
			//One reagent or homeo reaction
			addSpeciesInvolvedInTheReaction(reaction.getReagents().getAllSpecies(0),reaction.getReagents().getAllSpecies(0));
			for(int i=0;i<reaction.getProducts().getNumberOfDifferentSpecies();i++){
				addSpeciesInvolvedInTheReaction(reaction.getProducts().getAllSpecies(i), reaction.getReagents().getAllSpecies(0));
			}
		}
		else if(reaction.getReagents().getNumberOfDifferentSpecies()==2){
			//Two distinct reagents
			addSpeciesInvolvedInTheReaction(reaction.getReagents().getAllSpecies(0),reaction.getReagents().getAllSpecies(0));
			addSpeciesInvolvedInTheReaction(reaction.getReagents().getAllSpecies(0),reaction.getReagents().getAllSpecies(1));
			addSpeciesInvolvedInTheReaction(reaction.getReagents().getAllSpecies(1),reaction.getReagents().getAllSpecies(0));
			addSpeciesInvolvedInTheReaction(reaction.getReagents().getAllSpecies(1),reaction.getReagents().getAllSpecies(1));
			for(int i=0;i<reaction.getProducts().getNumberOfDifferentSpecies();i++){
				addSpeciesInvolvedInTheReaction(reaction.getProducts().getAllSpecies(i), reaction.getReagents().getAllSpecies(0));
				addSpeciesInvolvedInTheReaction(reaction.getProducts().getAllSpecies(i), reaction.getReagents().getAllSpecies(1));
			}
		}
		else {
			//TODO: check, added on 28/10/2021
			//N-ary distinct reagents
			IComposite reagents = reaction.getReagents();
			for(int r=0;r<reagents.getNumberOfDifferentSpecies();r++) {
				for(int r2=0;r2<reagents.getNumberOfDifferentSpecies();r2++) {
					addSpeciesInvolvedInTheReaction(reaction.getReagents().getAllSpecies(r),reaction.getReagents().getAllSpecies(r2));
				}
			}
//			addSpeciesInvolvedInTheReaction(reaction.getReagents().getAllSpecies(0),reaction.getReagents().getAllSpecies(0));
//			addSpeciesInvolvedInTheReaction(reaction.getReagents().getAllSpecies(0),reaction.getReagents().getAllSpecies(1));
//			addSpeciesInvolvedInTheReaction(reaction.getReagents().getAllSpecies(1),reaction.getReagents().getAllSpecies(0));
//			addSpeciesInvolvedInTheReaction(reaction.getReagents().getAllSpecies(1),reaction.getReagents().getAllSpecies(1));
			
			for(int i=0;i<reaction.getProducts().getNumberOfDifferentSpecies();i++){
				for(int r2=0;r2<reagents.getNumberOfDifferentSpecies();r2++) {
					addSpeciesInvolvedInTheReaction(reaction.getProducts().getAllSpecies(i), reaction.getReagents().getAllSpecies(r2));
				}
			}
//			for(int i=0;i<reaction.getProducts().getNumberOfDifferentSpecies();i++){
//				addSpeciesInvolvedInTheReaction(reaction.getProducts().getAllSpecies(i), reaction.getReagents().getAllSpecies(0));
//				addSpeciesInvolvedInTheReaction(reaction.getProducts().getAllSpecies(i), reaction.getReagents().getAllSpecies(1));
//			}
		}
		
	}


	private void addSpeciesInvolvedInTheReaction(ISpecies species, ISpecies involvedSpecies) {
		HashSet<ISpecies> involvedSpeciesSet = speciesInTheODEs.get(species);
		if(involvedSpeciesSet==null){
			involvedSpeciesSet = new HashSet<ISpecies>(); 
			speciesInTheODEs.put(species, involvedSpeciesSet);
		}
		involvedSpeciesSet.add(involvedSpecies);
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


}
