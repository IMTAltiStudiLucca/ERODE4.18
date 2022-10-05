package it.imt.erode.crn.implementations;

import java.util.Collection;

import it.imt.erode.partition.interfaces.IPartition;

public class InfoCRNReduction extends InfoReduction{
	
	private int originalReactions;
	private int reducedReactions=-1;
	
	
	
	public InfoCRNReduction(String originalCRN, int originalSpecies, int originalReactions, int parameters, double redSizeOverOrigSize, int initPartitionSize, IPartition obtainedPartition, /*int partitionSize,*/ long timeInMS, String reduction) {
		super(originalCRN,reduction,originalSpecies,parameters,redSizeOverOrigSize,obtainedPartition.size(),timeInMS,initPartitionSize,obtainedPartition);
		this.originalReactions=originalReactions;
	}
	
	public int getOriginalReactions() {
		return originalReactions;
	}
	public void setOriginalReactions(int originalReactions) {
		this.originalReactions = originalReactions;
	}

	public int getReducedReactions() {
		return reducedReactions;
	}
	public void setReducedReactions(int reducedReactions) {
		this.reducedReactions = reducedReactions;
	}
	
	/*
	 *  # Automatically generated from <original filename> via <technique>
		# Original number of species:
        # Original number of reactions:
        # Reduced number of species:
        # Reduced number of reactions:
        # Time taken: 
	 */
	
	
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder(super.toString());
		if(!(getReductionTechnique().equalsIgnoreCase("bde")||getReductionTechnique().equalsIgnoreCase("fde"))) {
			sb.append(".\nOriginal number of reactions: ");
			sb.append(originalReactions);
			if(reducedReactions>=0) {
				sb.append(".\nReduced number of reactions: ");
				sb.append(reducedReactions);
			}
		}
		
		return sb.toString();
	}
	
	public Collection<String> toCRNComment(){
		Collection<String> comments=super.toComments();
		
		if(!(getReductionTechnique().equalsIgnoreCase("bde")||getReductionTechnique().equalsIgnoreCase("fde"))) {
			comments.add(" Original number of reactions: "+originalReactions);
			comments.add(" Reduced number of reactions: "+reducedReactions);
		}
		
		return comments;
	}
	
	
	
}
