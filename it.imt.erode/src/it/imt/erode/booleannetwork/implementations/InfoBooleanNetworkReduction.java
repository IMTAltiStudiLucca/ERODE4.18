package it.imt.erode.booleannetwork.implementations;

import it.imt.erode.crn.implementations.InfoReduction;

public class InfoBooleanNetworkReduction extends InfoReduction {

	public InfoBooleanNetworkReduction(String originalNetwork, String reductionTechnique, int originalSpecies, double redSizeOverOrigSize,
			int reducedSpecies, long timeInMS, int initPartitionSize, int partitionSize) {
		super(originalNetwork, reductionTechnique, originalSpecies, 0,redSizeOverOrigSize,reducedSpecies, timeInMS, initPartitionSize,partitionSize);
	}

}
