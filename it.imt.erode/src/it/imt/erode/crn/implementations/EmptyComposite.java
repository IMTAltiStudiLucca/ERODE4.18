package it.imt.erode.crn.implementations;

import java.math.BigDecimal;
import java.util.HashMap;

import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.expression.parser.IMonomial;
import it.imt.erode.expression.parser.NumberMonomial;
//import it.imt.erode.expression.parser.SpeciesMonomial;

public class EmptyComposite implements IComposite {

	public static final EmptyComposite EMPTYCOMPOSITE = new EmptyComposite();
	
	@Override
	public int compareTo(Object o) {
		if(o == null) {
			return 1;
		}
		if(o instanceof EmptyComposite) {
			return 0;
		}
		return -1;
	}
	
	@Override
	public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + "empty".hashCode();
			return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj ==null) {
			return false;
		}
//		if(obj instanceof Composite) {
//			return ((Composite) obj).isEmpty();
//		}
		return obj instanceof EmptyComposite;
	}

	@Override
	public ISpecies getAllSpecies(int pos) {
		throw new UnsupportedOperationException("Empty composites do not have species");
	}

	@Override
	public int getMultiplicities(int pos) {
		throw new UnsupportedOperationException("Empty composites do not have species");
	}

	@Override
	public int getNumberOfDifferentSpecies() {
		return 0;
	}

	@Override
	public boolean isUnary() {
		return false;
	}

	@Override
	public boolean isBinary() {
		return false;
	}

	@Override
	public boolean isTernaryOrMore() {
		return false;
	}

	@Override
	public ISpecies getSecondReagent() {
		throw new UnsupportedOperationException("Empty composites do not have species");
	}

	@Override
	public ISpecies getFirstReagent() {
		throw new UnsupportedOperationException("Empty composites do not have species");
	}

	@Override
	public int getMultiplicityOfSpecies(ISpecies species) {
		throw new UnsupportedOperationException("Empty composites do not have species");
	}

	@Override
	public int getMultiplicityOfSpeciesWithId(int speciesId) {
		throw new UnsupportedOperationException("Empty composites do not have species");
	}

	@Override
	public int computeArity() {
		return 0;
	}

	@Override
	public int getTotalMultiplicity() {
		return 0;
	}

	@Override
	public String toMultiSet() {
		return "empty";
	}

	@Override
	public String toMultiSetCompact() {
		return "empty";
	}

	@Override
	public String toMultiSetWithStoichiometries(boolean separateSpeciesWithSpaces) {
		return "empty";
	}
	@Override
	public String toMultiSetWithStoichiometriesOrigNames(boolean separateSpeciesWithSpaces) {
		return "empty";
	}

	@Override
	public String toMultiSetWithAlphaNumericNames() {
		return "empty";
	}

	@Override
	public String getSpaceSeparatedMultisetAlphaNumeric() {
		return "empty";
	}

	@Override
	public String getSpaceSeparatedMultisetUsingIdsAsNames(String prefix, String suffix) {
		return "empty";
	}

	@Override
	public String getMassActionExpression(boolean withAlphaNumericNames, boolean ignoreI) {
		return "1";
	}

	@Override
	public String getMassActionExpression(boolean withAlphaNumericNames, String prefix, boolean ignoreI) {
		return "1";
	}

	@Override
	public String getMassActionExpression(boolean withAlphaNumericNames, String prefix, ISpecies special,
			String specialPrefix, boolean ignoreI) {
		return "1";
	}

	@Override
	public String getMassActionExpressionReplacingSpeciesNames(String variableAsFunction, int idIncrement,
			boolean writeParenthesis, String suffix, boolean squaredParenthesis) {
		return "1";
	}

	@Override
	public String getMassActionExpressionReplacingSpeciesNames(String variableAsFunction, int idIncrement,
			boolean writeParenthesis, String suffix) {
		return "1";
	}

	@Override
	public String getMassActionExpressionReplacingSpeciesNames(String variablePrefix, int idIncrement,
			String variableSuffix) {
		return "1";
	}

	@Override
	public ISpecies getNthReagent(int pos) {
		throw new UnsupportedOperationException("Empty composites do not have species");
	}

	@Override
	public boolean contains(ISpecies species) {
		return false;
	}

	@Override
	public void setMultiplicities(int pos, int val) {
		throw new UnsupportedOperationException("Empty composites do not have species");
	}

	@Override
	public int getPosOfSpecies(ISpecies species) {
		throw new UnsupportedOperationException("Empty composites do not have species");
	}

	@Override
	public boolean contains(IComposite other) {
		if(other instanceof EmptyComposite) {
			return true;
		}
		return false;
	}

	/**
	 * Applies the reaction: removes the reagents and adds the products. It is assumed that the reaction can be fired by this composite (i.e., it contains the reagents)
	 */
	@Override
	public IComposite apply(ICRNReaction reaction) {
		IComposite net = Composite.createFirstMinusSecond(reaction.getProducts(), reaction.getReagents());
		HashMap<ISpecies, Integer> resultHM = new HashMap<>(net.getNumberOfDifferentSpecies());
		for(int s=0;s<net.getNumberOfDifferentSpecies();s++){
			ISpecies species = net.getAllSpecies(s);
			Integer prev = resultHM.get(species);
			int resMult = (prev==null)?net.getMultiplicities(s) : prev+net.getMultiplicities(s);
			if(resMult!=0){
				resultHM.put(species, resMult);
			}
			else{
				resultHM.remove(species);
			}
		}
		return new Composite(resultHM);
	}

	@Override
	public HashMap<ISpecies, Integer> toHashMap() {
		return new HashMap<>(0);
	}

	@Override
	public boolean isHomeo() {
		return false;
	}

	@Override
	public IComposite copyDecreasingMultiplicityOf(ISpecies xj) {
		return this;
	}
	
	@Override
	public String toString() {
		return "empty";
	}
	
	@Override
	public IMonomial toMonomials() {
		return new NumberMonomial(BigDecimal.ONE,"1");
	}

}
