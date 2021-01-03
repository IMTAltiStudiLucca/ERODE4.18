package it.imt.erode.partition.implementations;

import java.util.Collection;
import java.util.List;

import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.label.ILabel;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.ICompositeBlock;
import it.imt.erode.partition.interfaces.ICompositePartition;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.SyntacticMarkovianBisimilarityOLD;
import it.imt.erode.partitionrefinement.splittersbstandcounters.CompositesSplayBST;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SplittersGeneratorSMB;

public class CompositePartition implements ICompositePartition {

	//private List<ICompositeBlock> blocks;
	private ICompositeBlock headBlock;
	private ICompositeBlock tailBlock;
	private int size;
	private SplittersGeneratorSMB splGenerator;
	
	/**
	 * Creates a partition of the products according to the given products and partition of species
	 * @param composites
	 */
	public CompositePartition(Collection<IComposite> composites, IPartition partition){
		this(new CompositeBlock(composites),partition,0);
	}
	
	public CompositePartition(){
		size=0;
	}
	
	private CompositePartition(ICompositeBlock block,IPartition partition,int iteration) {
		cleanBlockPointers(block);
		headBlock=block;
		tailBlock=block;
		size=1;
		
		switch(SyntacticMarkovianBisimilarityOLD.HOWTOSPLITCOMPOSITEBLOCKS){
		case UseStoredLabelsBlockByBlock:
			//refineViaNewMultiSetLiftingUsingStoredMultisetLabels(partition);break;
			throw new UnsupportedOperationException();
		case ComputeBlockMultiplicitiesAtNeed:
			refineViaNewMultiSetLifting(partition,iteration);	
			break;
		}
	}
	
	public CompositePartition(ICompositeBlock block,List<List<IBlock>> refineRespectToTheseBlocks, IPartition partition, int iteration) {
		cleanBlockPointers(block);
		headBlock=block;
		tailBlock=block;
		size=1;
		
		switch(SyntacticMarkovianBisimilarityOLD.HOWTOSPLITCOMPOSITEBLOCKS){
		case UseStoredLabelsBlockByBlock:
			throw new UnsupportedOperationException();
		case ComputeBlockMultiplicitiesAtNeed:
			refineViaNewMultiSetLifting(partition,refineRespectToTheseBlocks,iteration);	
			break;
		}
	}
	
	@Override
	public int size() {
		return size;
	}
	
	@Override
	public ICompositeBlock getFirstBlock() {
		return headBlock;
	}
	
	@Override
	public ICompositeBlock getLastBlock() {
		return tailBlock;
	}

	@Override
	public void add(ICompositeBlock block) {

		//Special case: this is the first block I insert
		if(size==0){
			headBlock=block;
			tailBlock=block;
		}
		else{
			tailBlock.setNext(block);
			block.setPrev(tailBlock);
			tailBlock=block;
		}
		size++;
	}
	
	@Override
	public void replaceOneBlockWithItsRefinement(ICompositePartition refinement, ICompositeBlock prev, ICompositeBlock next) {
		//blocks.add(block);
		
		if(prev!=null){
			prev.setNext(refinement.getFirstBlock());
		}
		else{
			headBlock=refinement.getFirstBlock();
		}
		refinement.getFirstBlock().setPrev(prev);
		
		if(next!=null){
			next.setPrev(refinement.getLastBlock());
		}
		else{
			tailBlock=refinement.getLastBlock();
		}
		refinement.getLastBlock().setNext(next);
		
		size--;
		size+=refinement.size();
	}

	/*@Override
	public boolean remove(ICompositeBlock upBlock) {
		return blocks.remove(upBlock);
	}*/
	
/*	@Override
	public boolean remove(ICompositeBlock block) {
		return remove(block,true);
	}
	
	@Override
	public boolean remove(ICompositeBlock blockToRemove,boolean notifySplitterGenerator) {
		//checkConsistency("begin of remove(IBlock block)")

		//Special case: empty partition
		if(size==0){
			return false;
		}

		//I notify the splittersGenerator that I am deleting this block
		if(splGenerator!=null && notifySplitterGenerator){
			splGenerator.blockRemotionNotification(blockToRemove);
		}

		//Special case: one block only: headBlock = tailBlock
		if(size==1){
			if(headBlock.equals(blockToRemove)){
				headBlock=null;
				tailBlock=null;
				size=0;
				cleanBlockPointers(blockToRemove);
				return true;
			}
			else{
				System.out.println("Problem: one blocco only, but the one to be removed is not equal to head");
				return false;
			}
		}
		else{

			//I have at least two blocks: I want to remove either the head, or the tail, or an intermediate block  

			//Special case: I want to remove the head
			if(headBlock.equals(blockToRemove)){
				headBlock=headBlock.getNext();
				headBlock.setPrev(null);
				cleanBlockPointers(blockToRemove);
				size--;
				//checkConsistency("end of remove(IBlock block)");
				return true;
			}

			//Special case: I want to remove the tail, and I have at least two blocks, otherwise it was also the head, and I managed it in the previous if 
			if(tailBlock.equals(blockToRemove)){
				tailBlock=tailBlock.getPrev();
				tailBlock.setNext(null);
				cleanBlockPointers(blockToRemove);
				size--;

				//checkConsistency("end of remove(IBlock block)");
				return true;
			}

			//Normal case: I remove a block which is neither head nor tail
			ICompositeBlock prev = blockToRemove.getPrev();
			ICompositeBlock next = blockToRemove.getNext();

			prev.setNext(next);
			next.setPrev(prev);
			cleanBlockPointers(blockToRemove);
			size--;

			//checkConsistency("end of remove(IBlock block)");

			return true;
		}
	}*/
	
	private void cleanBlockPointers(ICompositeBlock blockToRemove){
		blockToRemove.setNext(null);
		blockToRemove.setPrev(null);
	}

	@Override
	public void refineViaNewMultiSetLifting(IPartition partition,int iteration) {
		//int prevBlocks = size;
		
		IBlock currentBlockOfSpecies = partition.getFirstBlock();
		while(currentBlockOfSpecies!=null){
			splitBlocksOfCompositesAccordingToBlockOfSpecies(currentBlockOfSpecies,partition,iteration);
			currentBlockOfSpecies=currentBlockOfSpecies.getNext();
		}
		
		if(splGenerator!=null){ // && size!=prevBlocks){
			splGenerator.setNewBlocks(headBlock);
		}
	}
	
	private void refineViaNewMultiSetLifting/*OneBlockAtATime*/(IPartition partition, List<List<IBlock>> refineRespectToTheseBlocks, int iteration) {

		//System.out.println("\t\t "+refineRespectToTheseBlocks.size() +" "+partition.size());
		for(List<IBlock> consideredBlocks : refineRespectToTheseBlocks){
			for (IBlock currentBlockOfSpecies : consideredBlocks) {
				splitBlocksOfCompositesAccordingToBlockOfSpecies(currentBlockOfSpecies,partition,iteration);
			}
		}

		if(splGenerator!=null){// && size!=prevBlocks){
			splGenerator.setNewBlocks(headBlock);
		}
	}
	
//	private void refineViaNewMultiSetLiftinAllBlocksAtATime(IPartition partition, List<List<IBlock>> refineRespectToTheseBlocks, int iteration) {
//
//		//System.out.println("\t\t "+refineRespectToTheseBlocks.size() +" "+partition.size());
//		splitBlocksOfCompositesAccordingToBlocksOfSpeciesToConsider(refineRespectToTheseBlocks,partition,iteration);
//
//		if(splGenerator!=null){// && size!=prevBlocks){
//			splGenerator.setNewBlocks(headBlock);
//		}
//	}
//	
//
//	private void splitBlocksOfCompositesAccordingToBlocksOfSpeciesToConsider(List<List<IBlock>> refineRespectToTheseBlocks, IPartition partition,int iteration) {
//
//		ICompositeBlock current = headBlock;
//
//		ICompositePartition refinement = new CompositePartition();
//		while(current!=null){
//			ICompositeBlock next = current.getNext();
//			//The binary search tree used to split each block
//			//CompositesSplayBST<Integer, ICompositeBlock> bst = new CompositesSplayBST<Integer, ICompositeBlock>();
//			CompositesSplayBST<VectorOfBigDecimalsForEachLabel, ICompositeBlock> bstForVector = new CompositesSplayBST<>();
//			for (IComposite composite : current.getComposites()) {
//				HashMap<ILabel, BigDecimal> labelAndBigDecimals = new HashMap<>();
//				for(List<IBlock> consideredBlocks : refineRespectToTheseBlocks){
//					for (IBlock currentBlockOfSpecies : consideredBlocks) {
//						labelAndBigDecimals.put(currentBlockOfSpecies.getRepresentative(), BigDecimal.valueOf(getBlockMultiplicity(composite,partition,currentBlockOfSpecies)));
//					}
//				}
//				bstForVector.put(new VectorOfBigDecimalsForEachLabel(labelAndBigDecimals), composite, refinement, iteration);
//				//bst.put(getBlockMultiplicity(composite,partition,currentBlockOfSpecies), composite, refinement,iteration);
//			}
//			current=next;
//		}
//		this.headBlock=refinement.getFirstBlock();
//		this.tailBlock=refinement.getLastBlock();
//		this.size=refinement.size();
//	}
//	
	
	//block.getBSTForVectors().put(new VectorOfBigDecimalsForEachLabel(counters.getSMBCounterVector()), splitterGenerator, partition);


	private void splitBlocksOfCompositesAccordingToBlockOfSpecies(IBlock currentBlockOfSpecies, IPartition partition,int iteration) {

		ICompositeBlock current = headBlock;

		ICompositePartition refinement = new CompositePartition();
		while(current!=null){
			ICompositeBlock next = current.getNext();
			//The binary search tree used to split each block
			CompositesSplayBST<Integer, ICompositeBlock> bst = new CompositesSplayBST<Integer, ICompositeBlock>();
			for (IComposite composite : current.getComposites()) {
				bst.put(getBlockMultiplicity(composite,partition,currentBlockOfSpecies), composite, refinement,iteration);
			}
			current=next;
		}
		this.headBlock=refinement.getFirstBlock();
		this.tailBlock=refinement.getLastBlock();
		this.size=refinement.size();
	}
	
	
	private Integer getBlockMultiplicity(IComposite composite, IPartition partition,IBlock block) {
		if(composite.isUnary()){
			if(partition.getBlockOf(composite.getFirstReagent()).equals(block)){
				return 1;
			}
			else{
				return 0;
			}
		}
		else{
			int mult =0;
			for(int i=0;i<composite.getNumberOfDifferentSpecies();i++){
				if(partition.getBlockOf(composite.getAllSpecies(i)).equals(block)){
					mult+= composite.getMultiplicities(i);
				}
			}
			return mult;
		}
	}
	
	@Override
	public void refineViaNewMultiSetLifting(IPartition partition,List<List<IBlock>> refineRespectToTheseBlocks,ICompositeBlock blockSPL, int iteration) {
		
		ICompositeBlock current = headBlock;
		while(current!=null){
			boolean currentIsBlockSPL=false;
			if(current.equals(blockSPL)){
				currentIsBlockSPL=true;
			}
			ICompositeBlock prevBlock = current.getPrev();
			ICompositeBlock nextBlock = current.getNext();
			current.setPrev(null);
			current.setNext(null);
			ICompositePartition refinementOfTheBlock = new CompositePartition(current, refineRespectToTheseBlocks,partition,iteration);
			
			if(refinementOfTheBlock.size()==1){
				current.setPrev(prevBlock);
				current.setNext(nextBlock);
				if(currentIsBlockSPL && splGenerator!=null){
					splGenerator.generateNextSplitter();
				}
			}
			else{
				replaceOneBlockWithItsRefinement(refinementOfTheBlock, prevBlock, nextBlock);
				if(currentIsBlockSPL && splGenerator!=null){
					splGenerator.notificationCurrentBlockReplacedWithItsRefinement(refinementOfTheBlock.getFirstBlock());
				}
			}
			current=nextBlock;
		}
	}
	
	/*
	 * @Override
	public void refineViaNewMultiSetLifting(IPartition partition,List<List<IBlock>> refineRespectToTheseBlocks,ICompositeBlock blockSPL, int iteration) {
		
		ICompositeBlock current = headBlock;
		//check if necessary
		while(current!=null){
			current.setToBeRefined(true);
			current=current.getNext();
		}
		blockSPL.setToBeRefined(false);//This is ok because I refined blockSPL before treating it.
		
		ICompositeBlock prevBlock = blockSPL.getPrev();
		ICompositeBlock nextBlock = blockSPL.getNext();
		
		//ICompositePartition refinementOfTheBlock = new CompositePartition(blockSPL, partition,iteration);//PROBLEM, HERE I REFINE WRT ALL THE PARTITION, WHILE I SHOULD CONSIDER ONLY THE LIST OF BLOCKS "refineRespectToTheseBlocks"
		ICompositePartition refinementOfTheBlock = new CompositePartition(blockSPL, refineRespectToTheseBlocks,partition,iteration);
		
		
		if(refinementOfTheBlock.size()==1){
			blockSPL.setPrev(prevBlock);
			blockSPL.setNext(nextBlock);
			blockSPL.setToBeRefined(false);
			if(splGenerator!=null){
				splGenerator.generateNextSplitter();
			}
		}
		else{
			replaceOneBlockWithItsRefinement(refinementOfTheBlock, prevBlock, nextBlock);
			if(splGenerator!=null){
				splGenerator.notificationCurrentBlockReplacedWithItsRefinement(refinementOfTheBlock.getFirstBlock());
			}
		}
	}
	 */
	
	/**
	 * This method is invoked at the beginning of splitEFL or splitSMB, in case the current block (blockSPL) has to be refined.
	 */
	@Override
	public void refineViaNewMultiSetLifting(IPartition partition,ICompositeBlock blockSPL, int iteration) {
		
		/*ICompositeBlock current = headBlock;
		while(current!=null){
			current.setToBeRefined(true);
			current=current.getNext();
		}*/
		blockSPL.setToBeRefined(false);
		
		ICompositeBlock prevBlock = blockSPL.getPrev();
		ICompositeBlock nextBlock = blockSPL.getNext();
		
		ICompositePartition refinementOfTheBlock = new CompositePartition(blockSPL, partition,iteration);
		
		if(refinementOfTheBlock.size()==1){
			blockSPL.setPrev(prevBlock);
			blockSPL.setNext(nextBlock);
			blockSPL.setToBeRefined(false);
		}
		else{
			replaceOneBlockWithItsRefinement(refinementOfTheBlock, prevBlock, nextBlock);
			if(splGenerator!=null){
				splGenerator.notificationCurrentBlockReplacedWithItsRefinement(refinementOfTheBlock.getFirstBlock());
			}
		}
	}
	
	
	
	@Override
	public SplittersGeneratorSMB getSplitterGeneratorAndCreateItIfFirstInvocation(List<ILabel> labels) {
		if(splGenerator==null){
			splGenerator = new SplittersGeneratorSMB(headBlock, labels);
		}
		return splGenerator;
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(size);
		sb.append(" ");
		sb.append("blocks:\n");
		ICompositeBlock currentBlock = headBlock;
		while(currentBlock!=null){
			sb.append(currentBlock.toString());
			sb.append("\n");
			currentBlock=currentBlock.getNext();
		}
		return sb.toString();
	}

}
