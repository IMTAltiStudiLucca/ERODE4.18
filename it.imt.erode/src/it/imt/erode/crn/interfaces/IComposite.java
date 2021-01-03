package it.imt.erode.crn.interfaces;

//import java.util.Collection;
import java.util.HashMap;

import it.imt.erode.expression.parser.IMonomial;

public interface IComposite extends Comparable<Object> {

	/*public ISpecies[] getAllSpecies();
	public int[] getMultiplicities();*/
	/**
	 * Returns the posth different species, independently from the multiplicities
	 * @param pos
	 * @return
	 */
	public ISpecies getAllSpecies(int pos);
	public int getMultiplicities(int pos);
	public int getNumberOfDifferentSpecies();
	
	public boolean isUnary();
	public boolean isBinary();
	public boolean isTernaryOrMore();
	public ISpecies getSecondReagent();
	public ISpecies getFirstReagent();
	public int getMultiplicityOfSpecies(ISpecies species);
	public int getMultiplicityOfSpeciesWithId(int speciesId);
	
	@Override
	public int hashCode();
	
	@Override
	public boolean equals(Object obj);
	
	//public void addIncomingReactions(ICRNReaction incomingReaction);
	//public void addOutgoingReactions(ICRNReaction outgoingReaction);
	
	//public Collection<ICRNReaction> getIncomingReactions();
	//public Collection<ICRNReaction> getOutgoingReactions();

	public int computeArity();

	public int getTotalMultiplicity();
	
	public String toMultiSet();
	public String toMultiSetCompact();
	public String toMultiSetWithStoichiometries(boolean separateSpeciesWithSpaces);
	public String toMultiSetWithStoichiometriesOrigNames(boolean separateSpeciesWithSpaces);
	
	public String toMultiSetWithAlphaNumericNames();

	public String getSpaceSeparatedMultisetAlphaNumeric();
	
	public String getSpaceSeparatedMultisetUsingIdsAsNames(String prefix,String suffix);
	
	public String getMassActionExpression(boolean withAlphaNumericNames,boolean ignoreI);
	public String getMassActionExpression(boolean withAlphaNumericNames,String prefix,boolean ignoreI);
	public String getMassActionExpression(boolean withAlphaNumericNames,String prefix,ISpecies special, String specialPrefix,boolean ignoreI);
	
	public String getMassActionExpressionReplacingSpeciesNames(String variableAsFunction, int idIncrement, boolean writeParenthesis, String suffix, boolean squaredParenthesis);
	public String getMassActionExpressionReplacingSpeciesNames(String variableAsFunction, int idIncrement, boolean writeParenthesis, String suffix);
	public String getMassActionExpressionReplacingSpeciesNames(String variablePrefix, int idIncrement, String variableSuffix);
		
	/**
	 * 
	 * @param pos
	 * @return then nth reagent, from 1 (first) to totalMultiplicities() 
	 */
	ISpecies getNthReagent(int pos);
	
	boolean contains(ISpecies species);
	public void setMultiplicities(int pos, int val);
	public int getPosOfSpecies(ISpecies species);
	public boolean contains(IComposite other);
	public IComposite apply(ICRNReaction reaction);
	HashMap<ISpecies, Integer> toHashMap();
	public boolean isHomeo();
	public IComposite copyDecreasingMultiplicityOf(ISpecies xj);
	public IMonomial toMonomials();
	
}
