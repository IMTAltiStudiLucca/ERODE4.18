package it.imt.erode.partitionrefinement.splittersbstandcounters;

import java.util.List;

import it.imt.erode.crn.label.ILabel;
import it.imt.erode.partition.interfaces.ICompositeBlock;

public class SplittersGeneratorSMB {

	private List<ILabel> labels;
	private int currentLabelPos;
	private ILabel currentLabel;
	//private List<ICompositeBlock> compositeBlocks;
	//private int currentBlockPos;
	private ICompositeBlock currentBlock;
	private boolean noMoreSplitters=false;

	public SplittersGeneratorSMB(ICompositeBlock firstBlock, List<ILabel> labels) {
		super();
		currentBlock=firstBlock;
		this.labels = labels;
		initLabel();
		//this.compositeBlocks=compositeBlocks;
		resetNoMoreSplitters();
	}

	private void initLabel() {
		if(labels==null || labels.isEmpty()){
			currentLabelPos=-1;
			currentLabel = null;
			resetNoMoreSplitters();
		}
		else{
			currentLabelPos=0;
			currentLabel = labels.get(0);
			resetNoMoreSplitters();
		}
	}

	private void resetNoMoreSplitters() {
		if((labels==null || labels.isEmpty())||(currentBlock==null)) {
			noMoreSplitters=true;
		}
		else{
			noMoreSplitters=false;
		}
		
	}

	/*private void initBlock() {
		if(compositeBlocks==null || compositeBlocks.isEmpty()){
			currentBlockPos=-1;
			currentBlock = null;
			resetNoMoreSplitters();
		}
		else{
			currentBlockPos=0;
			currentBlock = compositeBlocks.get(0);
			resetNoMoreSplitters();
		}
	}*/

	public ILabel getLabelSpl(){
		return currentLabel;
	}

	public ICompositeBlock getBlockSpl(){
		return currentBlock;
	}


	private boolean shiftToNextPrefix(){
		currentLabelPos++;
		if(currentLabelPos<labels.size()){
			currentLabel = labels.get(currentLabelPos);
			return true;
		}
		else{
			currentLabel = null;
			return false;
		}
	}

	private boolean shiftToNextBlock(){
		if(currentBlock!=null && currentBlock.getNext()!=null){
			currentBlock=currentBlock.getNext();
			initLabel();
			return true;
		}
		else{
			currentBlock=null;
			noMoreSplitters=true;
			return false;
		}
		
	}

	public boolean generateNextSplitter(){
		//I first try to increase the label
		boolean hasMoreLabelsToConsider=shiftToNextPrefix();
		if(hasMoreLabelsToConsider){
			return true;
		}
		else{
			//I was already considering the last label. I have to increase the block and initialize the label.
			return shiftToNextBlock();
		}
	}
	
	public boolean generateNextSplitterWithNextBlock(){
			return shiftToNextBlock();
	}
	

	public boolean hasSplittersToConsider() {
		return !noMoreSplitters;
	}

	public void setNewBlocks(ICompositeBlock firstBlock) {
		currentBlock=firstBlock;
		initLabel();
		resetNoMoreSplitters();
	}
	
	/*public void replaceCurrentBlockWithItsRefinement(ICompositeBlock block, List<ICompositeBlock> refinement){
		///if(!currentBlock.equals(block)){
		//	//TODO: handle this case
		//	System.out.println("Inconsistent block!!!");
		//	System.exit(-1);
		//}
		compositeBlocks.remove(currentBlockPos);
		//compositeBlocks.addAll(0, refinement);
		//initBlock();
		compositeBlocks.addAll(currentBlockPos, refinement);
		currentBlock = refinement.get(0);
		initLabel();
		
		if(!currentBlock.equals(compositeBlocks.get(currentBlockPos))){
			System.out.println("Inconsistent block!!!");
			System.exit(-1);
		}
	}*/
	
	public void notificationCurrentBlockReplacedWithItsRefinement(ICompositeBlock firstBlockOfrefinement){
		currentBlock = firstBlockOfrefinement;
		initLabel();
		resetNoMoreSplitters();
		/*if(!currentBlock.equals(compositeBlocks.get(currentBlockPos))){
			System.out.println("Inconsistent block!!!");
			System.exit(-1);
		}*/
	}
	
	/*public void notificationCurrentBlockReplacedWithEquivalentObject(ICompositeBlock alias){
		currentBlock = alias;
	}*/

	public void restart(ICompositeBlock firstBlock) {
		currentBlock=firstBlock;
		initLabel();
		resetNoMoreSplitters();
	}

	@Override
	public String toString() {
		return currentLabelPos+" "+this.currentBlock.toString();
	}

}
