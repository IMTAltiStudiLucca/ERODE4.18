package it.imt.erode.onthefly.upto;

import java.util.HashMap;
import java.util.HashSet;

import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.onthefly.Pair;
import it.imt.erode.onthefly.linkedlist.MyLinkedList;

public abstract class AbstractUpToMembershipChecker implements IUpToMembershipChecker {

	protected HashMap<ISpecies, HashSet<ISpecies>> speciesToSet;
	
	public AbstractUpToMembershipChecker() {
		speciesToSet=new HashMap<ISpecies, HashSet<ISpecies>>();
	}

	@Override
	public void addedToR(Pair p) {
		// DO NOTHING
	}

	@Override
	public void removedFromR(Pair p,MyLinkedList<Pair> R) {
		// DO NOTHING
	}
	
	protected void updateSpeciesToRelatedSet(Pair p, boolean added) {
		if(added) {
			HashSet<ISpecies> relatedSet = speciesToSet.get(p.getFirst().getFirstReagent());
			if(relatedSet==null) {
				relatedSet=new HashSet<ISpecies>();
				speciesToSet.put(p.getFirst().getFirstReagent(), relatedSet);
			}
			relatedSet.add(p.getSecond().getFirstReagent());
		}
		else {
			//Removed
			ISpecies first = p.getFirst().getFirstReagent();
			ISpecies second = p.getSecond().getFirstReagent();
			speciesToSet.get(first).remove(second);
		}
	}

}
