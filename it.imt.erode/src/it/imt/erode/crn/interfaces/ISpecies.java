package it.imt.erode.crn.interfaces;

import java.math.BigDecimal;
import java.util.Collection;

import it.imt.erode.crn.label.ILabel;

public interface ISpecies extends ILabel, Comparable<Object>,Cloneable {
	
	public String getName();
	
	public String getOriginalName();

	public ISpecies cloneWithoutReactions();
	
	public int getID();
	public void setId(int id);
	
	public void setInitialConcentration(BigDecimal initialConcentration, String initConcentrationExpr);
	
	public String getInitialConcentrationExpr();
	
	public BigDecimal getInitialConcentration();

	public String toStringWithId();

	//public void addIncomingReactions(ICRNReaction incomingReaction);
	
	//public Collection<ICRNReaction> getIncomingReactions();
	
	//public Collection<ICRNReaction> getReactionsWithNonZeroStoichiometry();
	
	@Override
	public int hashCode();
	
	@Override
	public boolean equals(Object obj);

	public void addCommentLine(String commentLine);

	public Collection<String> getComments();

	public String getNameAlphanumeric();

	//public void addReactionsWithNonZeroStoichiometry(ICRNReaction reaction);

	//public void addOutgoingReactions(ICRNReaction outgoingReaction);

	//public Collection<ICRNReaction> getOutgoingReactions();

	public void decreaseID();

	void addCommentLines(Collection<String> commentLines);

	public void setName(String string);

	//public void setRepresentedEquivalenceClass(Collection<ISpecies> species);
	//public Collection<ISpecies> getRepresentedEquivalenceClass();

	public void setIsAlgebraic(boolean algebraic);
	public boolean isAlgebraic();
	
}
