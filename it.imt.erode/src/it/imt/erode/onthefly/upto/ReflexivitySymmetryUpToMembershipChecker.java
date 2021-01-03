package it.imt.erode.onthefly.upto;

import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.onthefly.Pair;
import it.imt.erode.onthefly.linkedlist.MyLinkedList;

public class ReflexivitySymmetryUpToMembershipChecker extends AbstractUpToMembershipChecker {

	private ReflexivityUpToMembershipChecker reflexivity;
	private SymmetryUpToMembershipChecker symmetry;

	public ReflexivitySymmetryUpToMembershipChecker() {
		reflexivity = new ReflexivityUpToMembershipChecker();
		symmetry = new SymmetryUpToMembershipChecker();
	}

	@Override
	public boolean canDerive(ISpecies first, ISpecies second, MyLinkedList<Pair> R) {
		boolean derived = reflexivity.canDerive(first, second, R);
		if(derived) {
			return true;
		}
		else {
			return symmetry.canDerive(first, second, R);
		}
	}

}
