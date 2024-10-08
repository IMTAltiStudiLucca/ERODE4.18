package it.imt.erode.expression.parser;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;

import it.imt.erode.crn.interfaces.ISpecies;

public interface IMonomial {

	//ICRNReaction toReaction(ISpecies product, ISpecies I);
	/**
	 * Creates a reaction that adds a term equal to this monomial to the derivation of variable product. If necessary, it will use species I which represents 1 
	 * @param product the variable to whose derivative we should add this monomial 
	 * @param I the special species I that represents the constant 1  
	 * @return the reaction adding this monomial to the derivative of product. The boolean is true if I was needed
	 */
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

	
	boolean isParameter();

	boolean needsI();
	
	double eval(HashMap<ISpecies, Double> speciesToValue, HashMap<ISpecies,Double> forceReplacement);
	
}
