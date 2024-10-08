package it.imt.erode.partitionrefinement.algorithms;

import java.io.BufferedWriter;
import java.util.Collection;
import java.util.HashMap;


import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.auxiliarydatastructures.ArrayListOfReactions;
import it.imt.erode.auxiliarydatastructures.Reduction;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.implementations.CRNReaction;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.splittersbstandcounters.ISpeciesCounterHandler;

public class NetworkControllability {
	public static IPartition computeCoarsestBEOnA_and_ATransposed(ICRN crn, IPartition partition,
			boolean verbose, MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator,
			IMessageDialogShower msgDialogShower) {
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"BE Reducing: "+crn.getName() + " for A and Atransposed");
		}
		IPartition obtainedPartition = partition.copy();

		if(!(crn.isMassAction() && crn.getMaxArity() <=1)){
			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it is not a unary mass action RN (i.e., has reactions with more than one reagent, or has reactions with arbitrary rates). I terminate.");
			return obtainedPartition;
		}
		else if(crn.isSymbolic()){
			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it contains symbolic parameters (i.e. parameters with no acutal value assigned) I terminate.");
			return obtainedPartition;
		}
		else if(crn.algebraicSpecies()>0){
			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it is a system of differential algebraic equations (i.e. it has algebraic species) I terminate.");
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
		
		ArrayListOfReactions[] reactionsToConsiderForEachSpecies=new ArrayListOfReactions[crn.getSpecies().size()];
		CRNReducerCommandLine.print(out,bwOut,"\n\tPreparing data structures for the reactions corresponding to A ...");
		CRNBisimulations.addToOutgoingReactionsOfReagents(crn.getReactions(), reactionsToConsiderForEachSpecies);
		CRNReducerCommandLine.print(out,bwOut," completed.");
		
		CRNReducerCommandLine.print(out,bwOut,"\n\tComputing the reactions corresponding to A transposed ...");
		ArrayListOfReactions[] transposedReactionsToConsiderForEachSpecies=new ArrayListOfReactions[crn.getSpecies().size()];
		addToOutgoingReactionsOfReagentsTransposingOriginalA(crn.getReactions(), transposedReactionsToConsiderForEachSpecies, out, bwOut);
		CRNReducerCommandLine.print(out,bwOut," completed.");
		
		
	
		useExplicitModelBigM(crn, reactionsToConsiderForEachSpecies,transposedReactionsToConsiderForEachSpecies, out, bwOut, terminator, obtainedPartition);

		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"The final partition:");
			CRNReducerCommandLine.println(out,bwOut,obtainedPartition);
		}

		if(verbose){
			long end = System.currentTimeMillis();
			CRNReducerCommandLine.println(out,bwOut," Partitioning completed. From "+ crn.getSpecies().size() +" species to "+ obtainedPartition.size() + " blocks. Time necessary: "+(end-begin)+ " (ms)");
			CRNReducerCommandLine.println(out,bwOut,"");
		}
		return obtainedPartition;
	}
	
	public static boolean addToOutgoingReactionsOfReagentsTransposingOriginalA(Collection<ICRNReaction> reactions,
			ArrayListOfReactions[] reactionsToConsiderForEachSpecies,MessageConsoleStream out, BufferedWriter bwOut) {
		for(ICRNReaction reaction : reactions) {
			if((!reaction.getReagents().isUnary()) || (!reaction.getProducts().isBinary())) {
				CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because the following reaction does not represent an entry of an adjacency list [1-ary reagents,2-ary products]:"+reaction);
				return false;
			}
			if(reaction.getProducts().getMultiplicityOfSpecies(reaction.getReagents().getFirstReagent())!=1) {
				CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because the following reaction does not represent an entry of an adjacency list [reagents appears once in products]:"+reaction);
				return false;
			}
			
			ISpecies newReagent = reaction.getProducts().getFirstReagent();
			if(reaction.getReagents().getFirstReagent().equals(reaction.getProducts().getFirstReagent())) {
				newReagent = reaction.getProducts().getSecondReagent();
			}
			ICRNReaction reactionOnTransposed = new CRNReaction(reaction.getRate(), (IComposite)newReagent, reaction.getProducts(), reaction.getRateExpression(), reaction.getID());
			CRNBisimulations.addToOutgoingReactionsOfReagents(reactionOnTransposed,reactionsToConsiderForEachSpecies);			
		}
		return true;
	}

	private static void useExplicitModelBigM(ICRN crn, ArrayListOfReactions[] reactionsToConsiderForEachSpecies,ArrayListOfReactions[] transposedReactionsToConsiderForEachSpecies, MessageConsoleStream out, BufferedWriter bwOut,
			Terminator terminator, IPartition obtainedPartition) {
		int iteration =1;
		int sizeOfPrevPartition=0;
		int sizeOfPartitionAfterLowerBound=0;

		boolean completed = false;
		//
		do {
			sizeOfPrevPartition = obtainedPartition.size();

			CRNReducerCommandLine.print(out,bwOut,"\n\tIteration "+iteration);
			CRNReducerCommandLine.print(out,bwOut,"\n\tComputing BE on original model...");
			long beginfe=System.currentTimeMillis();
			HashMap<ISpecies, ISpeciesCounterHandler> speciesCountersHM=new HashMap<>();
			//CRNBisimulationsNAry.refine(Reduction.BE,  crn, obtainedPartition, null,speciesCountersHM, terminator,out,bwOut,false);
			CRNBisimulationsNAry.refine(Reduction.BE,  crn, crn.getReactions(),obtainedPartition, null,speciesCountersHM, terminator,out,bwOut,false,true,null,null,reactionsToConsiderForEachSpecies,null,null);
			long endfe=System.currentTimeMillis();
			CRNReducerCommandLine.println(out,bwOut," completed in: "+String.format( CRNReducerCommandLine.MSFORMAT, ((endfe-beginfe)/1000.0) )+ " (s).");

			sizeOfPartitionAfterLowerBound = obtainedPartition.size();

			CRNReducerCommandLine.print(out,bwOut,"\tComputing BE on model for transposed matrix...");
			beginfe=System.currentTimeMillis();
			speciesCountersHM=new HashMap<>();
			CRNBisimulationsNAry.refine(Reduction.BE,  crn, crn.getReactions(), obtainedPartition, null,speciesCountersHM, terminator,out,bwOut,false,true,null,null,transposedReactionsToConsiderForEachSpecies,null,null);
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