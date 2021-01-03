package it.imt.erode.crn.implementations;

import java.util.ArrayList;
import java.util.Collection;

public abstract class InfoReduction {

	private String originalNetwork;
	private String reductionTechnique;
	private int originalSpecies;
	private int reducedSpecies;
	private long timeInMS;
	private int initPartitionSize;
	private int partitionSize;
	private int parameters;
	private double redSizeOverOrigSize;
	private String percRedSizeOverOrigSize;
	
	public InfoReduction(String originalNetwork, String reductionTechnique, int originalSpecies,  int parameters, double redSizeOverOrigSize,int reducedSpecies,
			long timeInMS, int initPartitionSize,int partitionSize) {
		super();
		this.originalNetwork = originalNetwork;
		this.reductionTechnique = reductionTechnique;
		this.originalSpecies = originalSpecies;
		this.reducedSpecies = reducedSpecies;
		this.parameters=parameters;
		this.redSizeOverOrigSize=redSizeOverOrigSize;
		percRedSizeOverOrigSize = String.format( "%.2f", ((redSizeOverOrigSize*100.0)) );
		
		this.timeInMS = timeInMS;
		this.initPartitionSize=initPartitionSize;
		this.partitionSize = partitionSize;
	}
	
	public int getInitPartitionSize() {
		return initPartitionSize;
	}
	public int getPartitionSize() {
		return partitionSize;
	}
	public void setPartitionSize(int partitionSize) {
		this.partitionSize = partitionSize;
	}
	public String getOriginalNetwork() {
		return originalNetwork;
	}
	public void setOriginalNetwork(String originalCRN) {
		this.originalNetwork = originalCRN;
	}
	public String getReductionTechnique() {
		return reductionTechnique;
	}
	public void setReductionTechnique(String reductionTechnique) {
		this.reductionTechnique = reductionTechnique;
	}
	public int getOriginalSpecies() {
		return originalSpecies;
	}
	public void setOriginalSpecies(int originalSpecies) {
		this.originalSpecies = originalSpecies;
	}
	public int getReducedSpecies() {
		return reducedSpecies;
	}
	public void setReducedSpecies(int reducedSpecies) {
		this.reducedSpecies = reducedSpecies;
	}
	public long getTimeInMS() {
		return timeInMS;
	}
	public void setTimeInMS(long timeInMS) {
		this.timeInMS = timeInMS;
	}
	
	public int getParametersSize() {
		return parameters;
	}
	
	public double getRedSizeOverOrigSize() {
		return redSizeOverOrigSize;
	}
	public String getPercRedSizeOverOrigSize() {
		return percRedSizeOverOrigSize;
	}
	
	public Collection<String> toComments(){
		Collection<String> comments=new ArrayList<String>(10);
		
		comments.add(" Automatically generated from "+getOriginalNetwork()+" via "+reductionTechnique);
		comments.add(" Size of initial partition: "+getInitPartitionSize());
		comments.add(" Original number of species: "+getOriginalSpecies());
		comments.add(" Reduced number of species: "+getReducedSpecies() + " ("+percRedSizeOverOrigSize+"%)");
		

		return comments;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("Automatically generated from ");
		sb.append(getOriginalNetwork());
		sb.append(" with initial partition containing "+getInitPartitionSize()+" blocks ");
		sb.append(" via ");
		sb.append(reductionTechnique);
		sb.append(".\nOriginal number of species: ");
		sb.append(getOriginalSpecies());
		sb.append(".\nReduced number of species: ");
		sb.append(getReducedSpecies());
		sb.append(" ("+percRedSizeOverOrigSize+"%)");
		
		return sb.toString();
	}
	
	
	
}
