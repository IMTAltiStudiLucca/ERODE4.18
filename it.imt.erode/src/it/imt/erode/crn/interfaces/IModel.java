package it.imt.erode.crn.interfaces;

import java.util.List;

public interface IModel {

	public List<ISpecies> getSpecies();
	public String getName();
	
	public List<String> getParameters();
}
