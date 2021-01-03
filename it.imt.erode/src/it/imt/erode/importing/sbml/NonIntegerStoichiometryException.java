package it.imt.erode.importing.sbml;

import it.imt.erode.importing.UnsupportedFormatException;

public class NonIntegerStoichiometryException extends UnsupportedFormatException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NonIntegerStoichiometryException(String message) {
		super(message);
	}

}
