package it.imt.erode.partitionrefinement.algorithms.fastepsbe;

import java.util.Comparator;

import it.imt.erode.partitionrefinement.algorithms.SpeciesAndVectorOfBigDecimalsForEachLabel;

public class SpeciesAndVectorOfBigDecimalsForEachLabelComparator implements Comparator<SpeciesAndVectorOfBigDecimalsForEachLabel> {

	@Override
	public int compare(SpeciesAndVectorOfBigDecimalsForEachLabel o1, SpeciesAndVectorOfBigDecimalsForEachLabel o2) {
		return o1.compareTo(o2);
	}
	

}
