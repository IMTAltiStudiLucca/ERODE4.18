package it.imt.erode.crn.validation;

import org.eclipse.xtext.validation.NamesAreUniqueValidationHelper;

public class MyNamesAreUniqueValidator extends NamesAreUniqueValidationHelper {
	
	@Override
	protected String getErrorCode() {
		return ChemicalReactionNetworkValidator.DUPLICATE_NAMES;
	}
	
}