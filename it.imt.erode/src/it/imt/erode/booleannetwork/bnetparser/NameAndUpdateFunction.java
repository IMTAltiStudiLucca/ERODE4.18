package it.imt.erode.booleannetwork.bnetparser;

import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;

public class NameAndUpdateFunction {

	private String name;
	private IUpdateFunction updateFunction;
	
	public NameAndUpdateFunction(String name, IUpdateFunction updateFunction) {
		this.name=name;
		this.updateFunction=updateFunction;
	}

	public String getName() {
		return name;
	}

	public IUpdateFunction getUpdateFunction() {
		return updateFunction;
	}
	
	

}
