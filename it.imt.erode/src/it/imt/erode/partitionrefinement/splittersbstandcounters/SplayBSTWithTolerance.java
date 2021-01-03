package it.imt.erode.partitionrefinement.splittersbstandcounters;

import java.math.BigDecimal;

/*************************************************************************
 *  Compilation:  javac SplayBST.java
 *  Execution:    java SplayBST
 *  
 *  Splay tree. Supports splay-insert, -search, and -delete.
 *  Splays on every operation, regardless of the presence of the associated
 *  key prior to that operation.
 *
 *  Written by Josh Israel.
 *  
 *  Modified by Andrea Vandin to deal with blocks (sets of ISpecies or of composites): elements of the splay tree are sets of species, but what we do is adding species to a block in the tree, or we create a new block containing the inserted species
 *
 *
 *************************************************************************/


//public class SplayBST<Key extends Comparable<Key>, Value>  {
public abstract class SplayBSTWithTolerance<Key extends BigDecimal, Value> extends SplayBST<BigDecimal, Value>  {
	//SpeciesSplayBSTWithTolerance<Key extends BigDecimal, Value extends IBlock> extends SplayBST<BigDecimal, Value>
	
	private Key tolerance;
	
	public SplayBSTWithTolerance(Key tolerance){
		this.tolerance=tolerance;
	}
	
	protected Key getTolerance(){
		return tolerance;
	}
    
	protected int compareToWithTolerance(BigDecimal key1, BigDecimal key2){
		
		int cmp = key1.compareTo(key2);
		if(cmp==0){
			return cmp;
		}
		else{
			BigDecimal diff = key1.subtract(key2);
	        diff=diff.abs();
	        if(diff.compareTo(getTolerance())<0){
	        	//System.out.println("equal up-to-tolerance: "+key1 +" "+key2);
	        	cmp=0;
	        }    
	        return cmp;
		}
	}
	
   /************************************************************************
    * splay function modified to compore elements up to a given tolerance
    * **********************************************************************/
    // splay key in the tree rooted at Node h. If a node with that key exists,
    //   it is splayed to the root of the tree. If it does not, the last node
    //   along the search path for the key is splayed to the root.
	@Override
	protected Node splay(Node h, BigDecimal key) {
        if (h == null) return null;

        //int cmp1 = key.compareTo(h.key);
        int cmp1 = compareToWithTolerance(key,h.key);

        if (cmp1 < 0) {
            // key not in tree, so we're done
            if (h.left == null) {
                return h;
            }
            //int cmp2 = key.compareTo(h.left.key);
            int cmp2 = compareToWithTolerance(key,h.left.key);
            if (cmp2 < 0) {
                h.left.left = splay(h.left.left, key);
                h = rotateRight(h);
            }
            else if (cmp2 > 0) {
                h.left.right = splay(h.left.right, key);
                if (h.left.right != null)
                    h.left = rotateLeft(h.left);
            }
            
            if (h.left == null) return h;
            else                return rotateRight(h);
        }

        else if (cmp1 > 0) { 
            // key not in tree, so we're done
            if (h.right == null) {
                return h;
            }

            //int cmp2 = key.compareTo(h.right.key);
            int cmp2 = compareToWithTolerance(key,h.right.key);
            if (cmp2 < 0) {
                h.right.left  = splay(h.right.left, key);
                if (h.right.left != null)
                    h.right = rotateRight(h.right);
            }
            else if (cmp2 > 0) {
                h.right.right = splay(h.right.right, key);
                h = rotateLeft(h);
            }
            
            if (h.right == null) return h;
            else                 return rotateLeft(h);
        }

        else return h;
    }

}
