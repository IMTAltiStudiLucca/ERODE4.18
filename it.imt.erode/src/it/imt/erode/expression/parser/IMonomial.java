package it.imt.erode.expression.parser;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;

import it.imt.erode.crn.interfaces.ISpecies;

public interface IMonomial {

	//ICRNReaction toReaction(ISpecies product, ISpecies I);
	ICRNReactionAndBoolean toReaction(ISpecies product, ISpecies I);

	HashMap<ISpecies, Integer> getOrComputeSpecies();
	void computeSpecies(HashMap<ISpecies, Integer> allSpecies);
	
	BigDecimal getOrComputeCoefficient();
	String getOrComputeCoefficientExpression();
	String getOrComputeCoefficientExpression(Collection<String> parametersToConsider);

	HashMap<String, Integer> getOrComputeParameters();
	void computeParameters(HashMap<String, Integer> parameters);

	BigDecimal getOrComputeCoefficientOfParameter();
	
	//String toStringPrefixingSpeciesName(String prefix);

	
}
