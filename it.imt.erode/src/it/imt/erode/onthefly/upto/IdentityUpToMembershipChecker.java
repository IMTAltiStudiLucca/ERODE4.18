package it.imt.erode.onthefly.upto;

import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.onthefly.Pair;
import it.imt.erode.onthefly.linkedlist.MyLinkedList;

public class IdentityUpToMembershipChecker extends AbstractUpToMembershipChecker {

	@Override
	public boolean canDerive(ISpecies first, ISpecies second, MyLinkedList<Pair> R) {
		Pair p = new Pair(first,second);
		return R.contains(p);
	}

}
