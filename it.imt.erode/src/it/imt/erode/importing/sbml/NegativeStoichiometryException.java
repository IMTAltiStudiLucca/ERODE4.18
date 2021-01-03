package it.imt.erode.importing.sbml;

import it.imt.erode.importing.UnsupportedFormatException;

public class NegativeStoichiometryException extends UnsupportedFormatException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public NegativeStoichiometryException(String message) {
		super(message);
	}

}
