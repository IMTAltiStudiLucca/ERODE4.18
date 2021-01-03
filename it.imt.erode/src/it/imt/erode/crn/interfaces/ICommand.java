package it.imt.erode.crn.interfaces;

import java.util.List;

import it.imt.erode.crn.implementations.CommandParameter;

public interface ICommand {

	String getName();
	List<CommandParameter> getParameters();
	String toCRNFormat();
	String toODEFormat();
	
}
