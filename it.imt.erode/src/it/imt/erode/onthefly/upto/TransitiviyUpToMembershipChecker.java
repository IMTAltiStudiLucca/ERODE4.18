package it.imt.erode.onthefly.upto;

import java.util.HashSet;

import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.onthefly.Pair;
import it.imt.erode.onthefly.linkedlist.MyLinkedList;

public class TransitiviyUpToMembershipChecker extends AbstractUpToMembershipChecker {

	@Override
	public void addedToR(Pair p) {
		//I store a graph-representation of R.
		updateSpeciesToRelatedSet(p,true);
	}

	@Override
	public void removedFromR(Pair p,MyLinkedList<Pair> R) {
		updateSpeciesToRelatedSet(p,false);
	}
	
	@Override
	public boolean canDerive(ISpecies first, ISpecies second, MyLinkedList<Pair> R) {
		HashSet<ISpecies> considered = new HashSet<ISpecies>();
		HashSet<ISpecies> related = speciesToSet.get(first);
		considered.add(first);
		return canFind(second,related,considered);
	}
	
	
	/**
	 * A simple breadth-first visit of R to decided whether we can derive the pair p by closing R by transitivity
	 * @param second the second species in the pair
	 * @param related the species appearing as second (right-part) in pairs with a given first element
	 * @param considered 
	 * @param speciesToRelatedByR graph representation of the current R
	 * @return
	 */
	private boolean canFind(ISpecies second, HashSet<ISpecies> related, HashSet<ISpecies> considered) {
		if(related==null || related.size()==0) {
			return false;
		}
		if(related.contains(second)) {
			return true;
		}
		for(ISpecies relSpecies : related) {
			if(!considered.contains(relSpecies)) {
				considered.add(relSpecies);
				boolean found=canFind(second, speciesToSet.get(relSpecies),considered);
				if(found) {
					return true;
				}
			}
		}
		return false;
	}

}
