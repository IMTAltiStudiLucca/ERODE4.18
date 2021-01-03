package it.imt.erode.partitionrefinement.algorithms;

import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.partitionrefinement.splittersbstandcounters.VectorOfBigDecimalsForEachLabel;

public class SpeciesAndVectorOfBigDecimalsForEachLabel  implements Comparable<SpeciesAndVectorOfBigDecimalsForEachLabel> {

	private ISpecies species;
	public ISpecies getSpecies() {
		return species;
	}

	public VectorOfBigDecimalsForEachLabel getVectorOfBigDecimalsForEachLabel() {
		return vectorOfBigDecimalsForEachLabel;
	}

	private VectorOfBigDecimalsForEachLabel vectorOfBigDecimalsForEachLabel;

	public SpeciesAndVectorOfBigDecimalsForEachLabel(ISpecies species,
			VectorOfBigDecimalsForEachLabel vectorOfBigDecimalsForEachLabel) {
		this.species = species;
		this.vectorOfBigDecimalsForEachLabel=vectorOfBigDecimalsForEachLabel;
	}

	@Override
	public int compareTo(SpeciesAndVectorOfBigDecimalsForEachLabel o) {
		return vectorOfBigDecimalsForEachLabel.compareTo(o.getVectorOfBigDecimalsForEachLabel());
		//return vectorOfBigDecimalsForEachLabel.compareToIgnoringEpsilonEntries(o.getVectorOfBigDecimalsForEachLabel());
	}
	
	@Override
	public String toString() {
		return species.toString() + " has " +vectorOfBigDecimalsForEachLabel.toString();
	}

}
