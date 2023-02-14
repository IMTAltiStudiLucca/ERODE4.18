package it.imt.erode.partitionrefinement.algorithms.fastepsbe;

import java.math.BigDecimal;

import it.imt.erode.crn.interfaces.ISpecies;

public class SpeciesAndBigDecimal  implements Comparable<SpeciesAndBigDecimal> {

	private ISpecies species;
	private BigDecimal bd;
	
	public SpeciesAndBigDecimal(ISpecies species, BigDecimal bd)  {
		this.species=species;
		this.bd=bd;
		if(this.bd==null) {
			this.bd=BigDecimal.ZERO;
		}
	}

	@Override
	public int compareTo(SpeciesAndBigDecimal o) {
		return bd.compareTo(o.bd);
	}
	
	public ISpecies getSpecies() {
		return species;
	}
	
	public BigDecimal getBD(){
		return bd;
	}
	
	@Override
	public String toString() {
		return species.getName() +" = "+bd; 
				
	}

}
