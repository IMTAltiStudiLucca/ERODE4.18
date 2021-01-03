package it.imt.erode.partition.interfaces;

import java.util.List;

import it.imt.erode.crn.label.ILabel;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SplittersGeneratorSMB;

public interface ICompositePartition {

	public int size();

	public void add(ICompositeBlock upBlock);

	//public boolean remove(ICompositeBlock upBlock);
	
	public void refineViaNewMultiSetLifting(IPartition partition,int iteration);

	public SplittersGeneratorSMB getSplitterGeneratorAndCreateItIfFirstInvocation(List<ILabel> labels);

	//public void refineViaNewMultiSetLifting(IPartition partition,List<List<IBlock>> refineRespectToTheseBlocks);

	//public Collection<ICompositeBlock> getBlocks();

	
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//public void refineViaNewMultiSetLiftingUsingStoredMultisetLabels(IPartition partition,List<List<IBlock>> refineRespectToTheseBlocks);
	//public void refineViaNewMultiSetLiftingUsingStoredMultisetLabels(IPartition partition);
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	void refineViaNewMultiSetLifting(IPartition partition,
			List<List<IBlock>> refineRespectToTheseBlocks,
			ICompositeBlock blockSPL,int iteration);

	void refineViaNewMultiSetLifting(IPartition partition, ICompositeBlock blockSPL, int iteration);

	ICompositeBlock getFirstBlock();

	ICompositeBlock getLastBlock();

	//boolean remove(ICompositeBlock blockToRemove,boolean notifySplitterGenerator);

	/**
	 * This method allows to replace a block with its refinement
	 * @param refinement the refinement of the block to replace 
	 * @param prev the prev block of the block to replace
	 * @param next the prev block of the block to replace
	 */
	void replaceOneBlockWithItsRefinement(ICompositePartition refinement, ICompositeBlock prev,
			ICompositeBlock next);

	/*void replaceOneBlockWithItsRefinementAddingToTail(
			ICompositePartition refinement, ICompositeBlock prev,
			ICompositeBlock next);*/
	
}
