package it.imt.erode.onthefly.upto;

import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.onthefly.Pair;
import it.imt.erode.onthefly.linkedlist.MyLinkedList;

public class EquivalenceUpToMembershipChecker extends TransitiviyUpToMembershipChecker {	
	
	@Override
	public void addedToR(Pair p) {
		//I store a graph-representation of R closed up to symmetry.
		updateSpeciesToRelatedSet(p,true);
		updateSpeciesToRelatedSet(new Pair(p.getSecond(), p.getFirst()),true);
	}

	@Override
	public void removedFromR(Pair p,MyLinkedList<Pair> R) {
		Pair reversed = new Pair(p.getSecond(), p.getFirst());
		if(!R.contains(reversed)) {
			updateSpeciesToRelatedSet(p,false);
			updateSpeciesToRelatedSet(reversed,false);
		}
	}
	
	@Override
	public boolean canDerive(ISpecies first, ISpecies second, MyLinkedList<Pair> R) {
		if(first.equals(second)) {
			return true;
		}
		return super.canDerive(first, second, R);
	}
	
}
