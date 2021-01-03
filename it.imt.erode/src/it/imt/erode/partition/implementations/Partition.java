package it.imt.erode.partition.implementations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.label.ILabel;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.CRNBisimulationsNAry;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SplittersGenerator;

public class Partition implements IPartition {
	
	private IBlock headBlock;
	private IBlock tailBlock;
	private int size;
	private SplittersGenerator splittersGenerator;
	/*
	 *Each position "i" of this array tells us to which block belongs the species with id "i".  
	 */
	private IBlock[] speciesIdToBlock;
	private int offsetOnId=0;
	
	/*
	 * Generates 
	 */
	public Partition(int numberOfSpecies) {
		super();
		size=0;
		speciesIdToBlock=new IBlock[numberOfSpecies];
	}
	public Partition(int numberOfSpecies, int offsetOnId) {
		this(numberOfSpecies);
		this.offsetOnId=offsetOnId;
	}
	
	@Override
	public void prepareForMoreSpecies(int more) {
		if(more>0) {
			speciesIdToBlock=Arrays.copyOf(speciesIdToBlock, speciesIdToBlock.length+more);
		}
	}
		
	public Partition(IBlock headBlock,IBlock tailBlock, int size, int numberOfSpecies) {
		this(numberOfSpecies);
		this.headBlock=headBlock;
		this.tailBlock=tailBlock;
		headBlock.setPartition(this);
		tailBlock.setPartition(this);
		
		this.size=size;
		
		//checkConsistency("end of Partition(IBlock headBlock,IBlock tailBlock, int size)");
	}
	
	public Partition(IBlock block,int numberOfSpecies) {
		this(block,block,1,numberOfSpecies);
	}
	
	public Partition(IBlock headBlock,IBlock tailBlock, int numberOfSpecies) {
		this(headBlock,tailBlock,2,numberOfSpecies);
		headBlock.setNext(tailBlock);
		tailBlock.setPrev(headBlock);
	}
	
	@Override
	public void updateSpeciesToBlockMapping(ISpecies species, IBlock block) {
		speciesIdToBlock[species.getID()+offsetOnId]=block;		
	}
	
	@Override
	public IPartition copy(){
		IPartition copy = new Partition(speciesIdToBlock.length);
		
		IBlock currentBlock = headBlock;
		while(currentBlock!=null){
			currentBlock.copyAndAddToPartition(copy);
			currentBlock= currentBlock.getNext();
		}
		return copy;
	}
	
//	@Override
//	public SplittersGenerator getOrCreateSplitterGenerator(List<ILabel> labels) {
//		if(splittersGenerator==null){
//			createSplitterGenerator(labels);
//		}
//		return splittersGenerator;
//	}
	
	@Override
	public SplittersGenerator createSplitterGenerator(List<ILabel> labels) {
		splittersGenerator=null;
		if(headBlock!=null){
			splittersGenerator = new SplittersGenerator(headBlock, labels);
		}
		return splittersGenerator;
	}
	
	@Override
	public int size() {
		return size;
	}

	@Override
	public IBlock getFirstBlock() {
		return headBlock;
	}
	
	@Override
	public IBlock getLastBlock() {
		return tailBlock;
	}

	@Override
	public void add(IBlock block) {
		//checkConsistency("begin of add(IBlock block)");
		
		block.setPartition(this);
		
		//Special case: this is the first block I insert
		if(size==0){
			headBlock=block;
			tailBlock=block;
		}
		else{
			tailBlock.setNext(block);
			block.setPrev(tailBlock);
			//block.setNext(null);
			tailBlock=block;
		}
		size++;
		
		//checkConsistency("end of add(IBlock block)");
	}
	
	private void addBetween(IBlock prev, IBlock next, IBlock blockToAdd) {
		//checkConsistency("begin of addBetween(IBlock prev, IBlock next, IBlock blockToAdd)");
		
		blockToAdd.setPartition(this);
		
		if(prev!=null){
			prev.setNext(blockToAdd);
		}
		blockToAdd.setPrev(prev);
		if(next!=null){
			next.setPrev(blockToAdd);
		}
		blockToAdd.setNext(next);
		
		if(headBlock==next){
			headBlock=blockToAdd;
		}
		if(tailBlock==prev){
			tailBlock=blockToAdd;
		}
		size++;
		
		//checkConsistency("end of addBetween(IBlock prev, IBlock next, IBlock blockToAdd)");
	}
	
	@Override
	public boolean remove(IBlock block) {
		return remove(block,true);
	}
	
	@Override
	public boolean remove(IBlock blockToRemove,boolean notifySplitterGenerator) {
		//checkConsistency("begin of remove(IBlock block)")

		//Special case: empty partition
		if(size==0){
			return false;
		}

		//I notify the splittersGenerator that I am deleting this block
		if(splittersGenerator!=null && notifySplitterGenerator){
			splittersGenerator.blockRemotionNotification(blockToRemove);
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
				//CRNReducerCommandLine.println("Problema: 1 blocco solo, ma quello che voglio rimuovere non e' uguale a head");
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
			IBlock prev = blockToRemove.getPrev();
			IBlock next = blockToRemove.getNext();

			prev.setNext(next);
			next.setPrev(prev);
			cleanBlockPointers(blockToRemove);
			size--;

			//checkConsistency("end of remove(IBlock block)");

			return true;
		}
	}
	
	private void cleanBlockPointers(IBlock blockToRemove){
		blockToRemove.setNext(null);
		blockToRemove.setPrev(null);
		blockToRemove.setPartition(null);
	}

	@Override
	public void substituteAndDecreaseSize(IBlock aliasBlock, IBlock toBeRemoved) {

		//checkConsistency("begin of substituteAndDecreaseSize(IBlock aliasBlock, IBlock toBeRemoved)");
		aliasBlock.setHasBeenAlreadyUsedAsSplitter(toBeRemoved.hasBeenAlreadyUsedAsSplitter());
		aliasBlock.setCanBeUsedAsSplitter(toBeRemoved.canBeUsedAsSplitter());
		
		if(aliasBlock.equals(toBeRemoved)){
			//CRNReducerCommandLine.println("You want to substitue a block with itself. I skip this");
			return;
		}

		if(size<=1){
			//CRNReducerCommandLine.println("You want to substitue a block when size="+size+". I skip this");
			return;
		}

		if(splittersGenerator!=null){
			splittersGenerator.aliasBlockSubstitutionNotification(toBeRemoved,aliasBlock);
		}
		
		//size>1
		if(size==2){
			//Then I have that one is head. I  just remove the one to be removed
			remove(toBeRemoved,false);
			return;
		}

		//special case: size >2, and TBR points to AB
		if(toBeRemoved.getNext()!=null && toBeRemoved.getNext().equals(aliasBlock)){
			IBlock prevOfTBR = toBeRemoved.getPrev();
			if(prevOfTBR!=null){
				prevOfTBR.setNext(aliasBlock);
			}
			aliasBlock.setPrev(prevOfTBR);
			size--;
			if(headBlock.equals(toBeRemoved)){
				headBlock=aliasBlock;
			}
			if(tailBlock.equals(toBeRemoved)){
				tailBlock=aliasBlock;
			}
			cleanBlockPointers(toBeRemoved);
		}
		else{
			//special case: size >2, and AB points to TBR
			if(toBeRemoved.getPrev()!=null && toBeRemoved.getPrev().equals(aliasBlock)){
				IBlock nextOfTBR = toBeRemoved.getNext();
				if(nextOfTBR!=null){
					nextOfTBR.setPrev(aliasBlock);
				}
				aliasBlock.setNext(nextOfTBR);
				size--;
				if(headBlock.equals(toBeRemoved)){
					headBlock=aliasBlock;
				}
				if(tailBlock.equals(toBeRemoved)){
					tailBlock=aliasBlock;
				}
				cleanBlockPointers(toBeRemoved);
			}
			//normal case: size >2, and TBR and AB do not point each other/
			else{
				IBlock prevOfTBR = toBeRemoved.getPrev();
				IBlock nextOfTBR = toBeRemoved.getNext();
				remove(toBeRemoved,false);
				remove(aliasBlock,false);
				addBetween(prevOfTBR,nextOfTBR,aliasBlock);
			}
		}
		//checkConsistency("end of substituteAndDecreaseSize(IBlock aliasBlock, IBlock toBeRemoved)");
	}
	
	@Override
	public boolean speciesIsRepresentativeOfItsBlock(ISpecies species) {
		//return speciesIsRepresentativeOfItsBlock(species,false);
		ISpecies rep = getBlockOf(species).getRepresentative(CRNReducerCommandLine.COMPUTEREPRESENTATIVEBYMINOUTSIDEPARTITIONREFINEMENT);
		return species.equals(rep);
	}
	
	
	/*@Override
	public boolean speciesIsRepresentativeOfItsBlock(ISpecies species, boolean computeRepresentativeByMin) {
		ISpecies rep = getBlockOf(species).getRepresentative(computeRepresentativeByMin);
		return species.equals(rep);
	}*/
	
	@Override
	public boolean splitBlockOnlyIfKeyIsNotZero(Collection<IBlock> splittedBlocks,ISpecies species, IBlock block, BigDecimal val) {
		boolean split =false;
		if(val.compareTo(BigDecimal.ZERO)!=0){
			splitBlock(splittedBlocks, species, block, val);
			/*
			if(block==null){
				block = getBlockOf(species);
			}
			//remove the species from its block (i.e., this block will remain with only species not performing reactions towards the splitter)
			block.removeSpecies(species);
			//Add block to the set of splitted blocks
			splittedBlocks.add(block);
			
			// Insert the species splitterGenerator in the tree associated to the block "block". 
			// This method is used to split the block according to the computed generation rates. This may cause the creation of a new block, in which case it is automatically added to the current partition. 
			// In any case, the reference of the species to the own block is updated   
			block.getBST().put(val, species, this);
			*/
			
			split = true;
		}
		return split;
	}
	
	
	@Override
	public void splitBlock(Collection<IBlock> splittedBlocks,ISpecies species, IBlock block, BigDecimal val) {

		if(block==null){
			block = getBlockOf(species);
		}
		//remove the species from its block (i.e., this block will remain with only species not performing reactions towards the splitter)
		block.removeSpecies(species);
		//Add block to the set of splitted blocks
		splittedBlocks.add(block);

		/*
		 * Insert the species splitterGenerator in the tree associated to the block "block". 
		 * This method is used to split the block according to the computed generation rates. This may cause the creation of a new block, in which case it is automatically added to the current partition. 
		 * In any case, the reference of the species to the own block is updated   
		 */
		//block.getBST().put(val, species, this);
		if(CRNBisimulationsNAry.USETOLERANCE){
			block.getBST(CRNBisimulationsNAry.getTolerance()).put(val, species, this);
		}
		else{
			block.getBST().put(val, species, this);
		}

	}
	
	@Override
	public IBlock getBlockOf(ISpecies species) {
		if(speciesIdToBlock[species.getID()+offsetOnId]==null) {
			System.out.println("Ciao");
		}
		return speciesIdToBlock[species.getID()+offsetOnId];
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("The partition has ");
		sb.append(size);
		if(size==1){
			sb.append(" block out of "+speciesIdToBlock.length+" species:\n");
		}
		else{
			sb.append(" blocks out of "+speciesIdToBlock.length+" species:\n");
		}
		
		IBlock currentBlock = headBlock;
		int b=1;
		while(currentBlock!=null){
			sb.append("Block "+b+", ");
			b++;
			sb.append(currentBlock.toString());
			sb.append("\n");
			currentBlock=currentBlock.getNext();
		}
		return sb.toString();
	}
	
	private boolean checkConsistency(){
		
		int realSize = 0;
		IBlock currentBlock = headBlock;
		while(currentBlock!=null){
			realSize++;
			currentBlock=currentBlock.getNext();
		}
		if(size!=realSize){
			//CRNReducerCommandLine.println("Problema: size="+size+", realSize="+realSize);
			return false;
		}
	
		
		if(size==0){
			if(headBlock != null){
				//CRNReducerCommandLine.println("Problema: size=0, ma head non e' nullo");
				return false;
			}
			if(tailBlock != null){
				//CRNReducerCommandLine.println("Problema: size=0, ma tail non e' nullo");
				return false;
			}
		}
		//size>0
		else{
			if(headBlock == null){
				//CRNReducerCommandLine.println("Problema: size>0, ma  head e' nullo");
				return false;
			}
			if(tailBlock == null){
				//CRNReducerCommandLine.println("Problema: size>0, ma tail e' nullo");
				return false;
			}
			if(headBlock.getPrev() != null){
				//CRNReducerCommandLine.println("Problema: prev di head non e' nullo");
				return false;
			}
			if(tailBlock.getNext() != null){
				//CRNReducerCommandLine.println("Problema: next di tail non e' nullo");
				return false;
			}
			if(size==1){
				if(!headBlock.equals(tailBlock)){
					//CRNReducerCommandLine.println("Problema: size =1, ma head =/= tail");
					return false;
				}
			}
			if(size > 1){
				if(headBlock.equals(tailBlock)){
					//CRNReducerCommandLine.println("Problema: size >1, ma head = tail");
					return false;
				}
			}
		}
		return true;
	}
	
	@SuppressWarnings("unused")
	private boolean checkConsistency(String message){
		if(!checkConsistency()){
			//CRNReducerCommandLine.println(message);
			System.exit(-1);
		}
		return true;
	}
	
	@Override
	public void initializeAllBST() {
		IBlock currentBlock = headBlock;
		while(currentBlock!=null){
			currentBlock.throwAwayBST();
			currentBlock.throwAwayBSTWithTolerance();
			currentBlock.throwAwayBSTForVectors();
			currentBlock.setCanBeUsedAsSplitter(true);
			currentBlock.setHasBeenAlreadyUsedAsSplitter(false);
			currentBlock=currentBlock.getNext();
		}
		
	}

	@Override
	public void setMinAsRepresentative() {
		IBlock currentBlock = headBlock;
		while(currentBlock!=null){
			currentBlock.setMinAsRepresentative();
			currentBlock=currentBlock.getNext();
		}
		
	}

	

}
