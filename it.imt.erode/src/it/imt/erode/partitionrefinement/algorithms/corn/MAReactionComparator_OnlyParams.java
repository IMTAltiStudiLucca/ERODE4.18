package it.imt.erode.partitionrefinement.algorithms.corn;

import java.util.Comparator;

import it.imt.erode.crn.implementations.CRNMassActionReactionCompact;
import it.imt.erode.crn.implementations.CRNReaction;
import it.imt.erode.crn.interfaces.ICRNReaction;

public class MAReactionComparator_OnlyParams implements Comparator<ICRNReaction> {

	@Override
	public int compare(ICRNReaction o1, ICRNReaction o2) {
		if((o1 instanceof CRNReaction || o1 instanceof CRNMassActionReactionCompact) && (o2 instanceof CRNReaction ||o2 instanceof CRNMassActionReactionCompact)){
			return o1.getRate().compareTo(o2.getRate());
		}
		else{
			throw new UnsupportedOperationException("Unsupported reactions: "+o1.toString()+" and "+o2.toString());
		}
	}
}
