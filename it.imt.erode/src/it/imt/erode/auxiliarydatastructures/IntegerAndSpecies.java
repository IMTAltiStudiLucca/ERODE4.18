package it.imt.erode.auxiliarydatastructures;

import it.imt.erode.crn.interfaces.ISpecies;

public class IntegerAndSpecies {

	private int multiplicity;
	private ISpecies species;
	
	public int getMultiplicity() {
		return multiplicity;
	}
	public void setMultiplicity(int multiplicity) {
		this.multiplicity = multiplicity;
	}
	public ISpecies getSpecies() {
		return species;
	}
	public void setSpecies(ISpecies species) {
		this.species = species;
	}
	
	@Override
	public String toString() {
		if(species!=null){
			return multiplicity+species.getName();
		}
		else{
			return "NOMORE";
		}
	}
	
	
	
}
