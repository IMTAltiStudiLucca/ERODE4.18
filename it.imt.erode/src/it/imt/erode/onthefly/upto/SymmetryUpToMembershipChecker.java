package it.imt.erode.onthefly.upto;

import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.onthefly.Pair;
import it.imt.erode.onthefly.linkedlist.MyLinkedList;

public class SymmetryUpToMembershipChecker extends AbstractUpToMembershipChecker {

	@Override
	public boolean canDerive(ISpecies first, ISpecies second, MyLinkedList<Pair> R) {
		Pair p = new Pair(first,second);
		if(R.contains(p) ||R.contains(new Pair(p.getSecond(),p.getFirst()))) {
			return true;
		}
		else {
			return false;
		}
	}

}
