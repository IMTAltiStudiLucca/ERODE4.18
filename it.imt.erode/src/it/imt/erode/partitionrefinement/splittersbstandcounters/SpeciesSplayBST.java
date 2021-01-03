package it.imt.erode.partitionrefinement.splittersbstandcounters;

import java.util.ArrayList;
import java.util.List;

import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;

public class SpeciesSplayBST<Key extends Comparable<Key>, Value extends IBlock> extends SplayBST<Key, Value> {

	private List<IBlock> subBlocks;
	private IBlock biggestSubBlock=null;
    
    public SpeciesSplayBST() {
		super();
		subBlocks = new ArrayList<IBlock>();
	}
    
    public List<IBlock> getBlocks(){
    	return subBlocks;
    }
    
    public IBlock getBiggestSubBlock(){
    	return biggestSubBlock;
    }
    
    public int getSizeOfBiggestSubBlock(){
    	if(biggestSubBlock==null){
    		return -1;
    	}
    	else{
    		return biggestSubBlock.getSpecies().size();
    	}
    }
    
     
    @Override
    public String toString(){
    	String ret = "bst:";
    	for (IBlock block : subBlocks) {
			ret += " " + block.toString(); 
		}
    	return ret;
    			
    }
	
    
	 /*************************************************************************
     *  splay insertion
     *  Add the species to the block with same key (create such block if it does not exist).
     *  A new block is created if no block with the same key exists. Such block is also added to the provided partition. In any case, the reference of the species to the own block is updated
     *  This method is used to split the block according to the computed generation rates.   
     *************************************************************************/
     @SuppressWarnings("unchecked")
	public void put(Key key, ISpecies species, IPartition refinedPartition) {
         // splay key to root
         if (root == null) {
        	 IBlock newlyCreatedSubBlock = new Block();
        	 refinedPartition.add(newlyCreatedSubBlock);
        	 newlyCreatedSubBlock.addSpecies(species);
        	 
        	 subBlocks.add(newlyCreatedSubBlock);
             root = new Node(key, (Value)newlyCreatedSubBlock);
             
             /*if(!useAsSplitter){
        		 newlyCreatedSubBlock.doNotUseAsSplitter();
        	 }*/
             if(newlyCreatedSubBlock.getSpecies().size() > getSizeOfBiggestSubBlock()){
            	 biggestSubBlock=newlyCreatedSubBlock;
             }
             
             return;
         }
         
         root = splay(root, key);
         int cmp = key.compareTo(root.key);
         
         // Insert new node at root
         if (cmp < 0) {
        	 IBlock newlyCreatedSubBlock = new Block();
        	 refinedPartition.add(newlyCreatedSubBlock);
        	 newlyCreatedSubBlock.addSpecies(species);
        	 
        	 subBlocks.add(newlyCreatedSubBlock);
        	 Node n = new Node(key, (Value)newlyCreatedSubBlock);
             n.left = root.left;
             n.right = root;
             root.left = null;
             root = n;
             
             /*if(!useAsSplitter){
        		 newlyCreatedSubBlock.doNotUseAsSplitter();
        	 }*/
             if(newlyCreatedSubBlock.getSpecies().size() > getSizeOfBiggestSubBlock()){
            	 biggestSubBlock=newlyCreatedSubBlock;
             }
         }

         // Insert new node at root
         else if (cmp > 0) {
        	 IBlock newlyCreatedSubBlock = new Block();
        	 refinedPartition.add(newlyCreatedSubBlock);
        	 newlyCreatedSubBlock.addSpecies(species);
        	 
        	 subBlocks.add(newlyCreatedSubBlock);
        	 Node n = new Node(key, (Value)newlyCreatedSubBlock);
             n.right = root.right;
             n.left = root;
             root.right = null;
             root = n;
             
             /*if(!useAsSplitter){
        		 newlyCreatedSubBlock.doNotUseAsSplitter();
        	 }*/
             if(newlyCreatedSubBlock.getSpecies().size() > getSizeOfBiggestSubBlock()){
            	 biggestSubBlock=newlyCreatedSubBlock;
             }
         }

         // It was a duplicate key: i.e. the tree already contains a block with such key: I have to add the species to the block
         else if (cmp == 0) {
        	 IBlock B = root.value;
             B.addSpecies(species);
             
             if(B.getSpecies().size() > getSizeOfBiggestSubBlock()){
            	 biggestSubBlock=B;
             }
         }

     }

	
}
