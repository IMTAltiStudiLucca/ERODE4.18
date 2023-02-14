package it.imt.erode.partitionrefinement.algorithms;

import java.math.BigDecimal;
import java.util.HashMap;

import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.partition.interfaces.IPartition;

public class PartitionAndMappingReactionToNewRate {

	private IPartition obtainedPartition;
	private HashMap<ICRNReaction, BigDecimal> reactionToRateInModelBigM;
	
	public PartitionAndMappingReactionToNewRate(IPartition obtainedPartition,
			HashMap<ICRNReaction, BigDecimal> reactionToRateInModelBigM) {
		this.obtainedPartition=obtainedPartition;
		this.reactionToRateInModelBigM=reactionToRateInModelBigM;
	}

	public IPartition getObtainedPartition() {
		return obtainedPartition;
	}

	public HashMap<ICRNReaction, BigDecimal> getReactionToRateInModelBigM() {
		return reactionToRateInModelBigM;
	}
	
	

	
	
}
