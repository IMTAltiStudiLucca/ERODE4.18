package it.imt.erode.onthefly.upto;

import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.onthefly.Pair;
import it.imt.erode.onthefly.linkedlist.MyLinkedList;

public class ReflexivityUpToMembershipChecker extends AbstractUpToMembershipChecker {

	@Override
	public boolean canDerive(ISpecies first, ISpecies second, MyLinkedList<Pair> R) {
		if(first.equals(second)) {
			return true;
		}
		else {
			return R.contains(new Pair(first,second));
		}
	}
	
}
