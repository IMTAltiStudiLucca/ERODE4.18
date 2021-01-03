package it.imt.erode.onthefly.upto;

import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.onthefly.Pair;
import it.imt.erode.onthefly.linkedlist.MyLinkedList;

public interface IUpToMembershipChecker {

	//boolean canDerive(ISpecies first, ISpecies second, MyLinkedList<Pair> R,HashMap<ISpecies,HashSet<ISpecies>> speciesToRelatedByR,HashMap<ISpecies,HashSet<ISpecies>> speciesToRelatedByRUnorderedPair);
	boolean canDerive(ISpecies first, ISpecies second, MyLinkedList<Pair> R);
	
	void addedToR(Pair p);
	void removedFromR(Pair p,MyLinkedList<Pair> R);
}
