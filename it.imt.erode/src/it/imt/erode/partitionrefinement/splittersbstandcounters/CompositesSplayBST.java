package it.imt.erode.partitionrefinement.splittersbstandcounters;

import java.util.ArrayList;
import java.util.List;

import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.partition.implementations.CompositeBlock;
import it.imt.erode.partition.interfaces.ICompositeBlock;
import it.imt.erode.partition.interfaces.ICompositePartition;

public class CompositesSplayBST<Key extends Comparable<Key>, Value extends ICompositeBlock> extends SplayBST<Key, Value> {

	private List<ICompositeBlock> blocks;
    
    public CompositesSplayBST() {
		super();
		blocks = new ArrayList<ICompositeBlock>();
	}
    
    public List<ICompositeBlock> getBlocks(){
    	return blocks;
    }
    
     
    @Override
    public String toString(){
    	String ret = "bst:";
    	for (ICompositeBlock block : blocks) {
			ret += " " + block.toString(); 
		}
    	return ret;
    			
    }
	
	 /*************************************************************************
     *  splay insertion
     *  Add the composite to the block with same key (create such block if it does not exist).
     *  A new block is created if no block with the same key exists. Such block is also added to the provided partition. 
     *  This method is used to split the block according to the computed multiplicity   
     *************************************************************************/
     @SuppressWarnings("unchecked")
	public void put(Key key, IComposite composite, ICompositePartition partition, int iteration) {
         // splay key to root
         if (root == null) {
        	 ICompositeBlock newlyCreatedSubBlock = new CompositeBlock(composite);
        	 newlyCreatedSubBlock.setCreatedAtStep(iteration);
        	 partition.add(newlyCreatedSubBlock);
     
        	 blocks.add(newlyCreatedSubBlock);
             root = new Node(key, (Value)newlyCreatedSubBlock);
             
             return;
         }
         
         root = splay(root, key);
         int cmp = key.compareTo(root.key);
         
         // Insert new node at root
         if (cmp < 0) {
        	 ICompositeBlock newlyCreatedSubBlock = new CompositeBlock(composite);
        	 newlyCreatedSubBlock.setCreatedAtStep(iteration);
        	 partition.add(newlyCreatedSubBlock);
        	 
        	 blocks.add(newlyCreatedSubBlock);
        	 Node n = new Node(key, (Value)newlyCreatedSubBlock);
             n.left = root.left;
             n.right = root;
             root.left = null;
             root = n;
         }

         // Insert new node at root
         else if (cmp > 0) {
        	 ICompositeBlock newlyCreatedSubBlock = new CompositeBlock(composite);
        	 newlyCreatedSubBlock.setCreatedAtStep(iteration);
        	 partition.add(newlyCreatedSubBlock);
        	 
        	 blocks.add(newlyCreatedSubBlock);
        	 Node n = new Node(key, (Value)newlyCreatedSubBlock);
             n.right = root.right;
             n.left = root;
             root.right = null;
             root = n;
         }

         // It was a duplicate key: i.e. the tree already contains a block with such key: I have to add the species to the block
         else if (cmp == 0) {
        	 ICompositeBlock B = root.value;
             B.addComposite(composite);
         }

     }

	
}
