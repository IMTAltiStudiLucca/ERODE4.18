package it.imt.erode.auxiliarydatastructures;

import it.imt.erode.crn.interfaces.IComposite;

public class CompositeAndBoolean {

	private IComposite composite;
	private boolean addedZeroSpecies;
	
	public CompositeAndBoolean(IComposite composite, boolean addedZeroSpecies) {
		this.composite=composite;
		this.addedZeroSpecies=addedZeroSpecies;
	}
	
	public IComposite getComposite() {
		return composite;
	}

	public boolean isAddedZeroSpecies() {
		return addedZeroSpecies;
	}

}
