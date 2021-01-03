package it.imt.erode.partitionrefinement.splittersbstandcounters;

import java.util.List;

import it.imt.erode.crn.label.ILabel;
import it.imt.erode.partition.interfaces.IBlock;

public class SplittersGenerator {

	private List<ILabel> labels;
	private int currentLabelPos;
	private ILabel currentLabel;
	private IBlock currentBlock;
	private boolean noMoreSplitters=false;
	
	@Override
	public String toString() {
		return currentLabelPos+" "+this.currentBlock.toString();
	}
	
	public SplittersGenerator(IBlock firstBlock, List<ILabel> labels) {
		super();
		this.labels = labels;
		initLabel();
		currentBlock=firstBlock;
	}
	
	public void reset(IBlock firstBlock){
		initLabel();
		currentBlock=firstBlock;
	}
	
	public ILabel getLabelSpl(){
		return currentLabel;
	}

	public IBlock getBlockSpl(){
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
	
	public void skipSplittersWhichShouldBeIgnored() {
		while(currentBlock!=null && !currentBlock.canBeUsedAsSplitter() && currentBlock.getNext()!=null){
			shiftToNextBlock();
			
		}
	}
	
	private boolean shiftToNextBlock(){
		/*if(currentBlock!=null){
			currentBlock.setHasBeenAlreadyUsedAsSplitter(true);
		}*/
		
		if(currentBlock!=null && currentBlock.getNext()!=null){
			currentBlock=currentBlock.getNext();
			if(!currentBlock.canBeUsedAsSplitter()){
				//System.out.println("I skip a splitter of size "+currentBlock.getSpecies().size());
				return shiftToNextBlock();
			}
			initLabel();
			return true;
		}
		noMoreSplitters=true;
		return false;
		
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

	public void blockRemotionNotification(IBlock blockToRemove) {
		if(currentBlock!=null && currentBlock.equals(blockToRemove)){
			shiftToNextBlock();
		}
	}	

	private void initLabel() {
		if(labels==null || labels.isEmpty()){
			currentLabelPos=-1;
			currentLabel = null;
			noMoreSplitters=true;
		}
		else{
			currentLabelPos=0;
			currentLabel = labels.get(0);
			noMoreSplitters=false;
		}
	}

	/**
	 * The current block has been split, but it is not empty, and thus has not been removed. I consider it as a new block: I restart from the first label.
	 */
	public void currentBlockHasBeenSplitButNotRemoved() {
		initLabel();
	}

	public boolean hasSplittersToConsider() {
		return !noMoreSplitters;
	}

	/*
	 * A block is substituted with an "equal one". If it is the one I am considering, I have to update this reference.
	 */
	public void aliasBlockSubstitutionNotification(IBlock toBeRemoved,
			IBlock aliasBlock) {
		if(currentBlock!=null && currentBlock.equals(toBeRemoved)){
			currentBlock=aliasBlock;
		}
		
	}

	
	

	
	
	
	
	
	
	
	
	
	
	/*public boolean shiftToNextBlockWhenConsideringAllLabelsAtOnce(){
		return shiftToNextBlock();
	}*/
}
