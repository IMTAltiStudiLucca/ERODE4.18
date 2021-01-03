package it.imt.erode.crn.interfaces;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;

import it.imt.erode.crn.implementations.Composite;

public interface ICRNReaction extends Comparable<ICRNReaction> {
	
	public boolean hasUnaryProduct();

	public IComposite getReagents();
	
	public IComposite getProducts();
	
	public boolean isBinary();

	boolean isElementary();

	public boolean isUnary();

	public int getArity();
	
	public String getID();
	public void setID(String newID);
	public boolean hasID();

	//TODO: remove, and use it only in CRNREACTION
	public BigDecimal getRate();
	
	public String getRateExpression();
	
	//TODO: remove, and use it only in CRNREACTION
	public void setRate(BigDecimal rate, String rateExpr);

	public IComposite computeProductsMinusReagents();
	
	public HashMap<ISpecies, Integer> computeProductsMinusReagentsHashMap();
	
	//boolean hasHillKinetics();
	
	boolean hasArbitraryKinetics();

	public ICRNReaction cloneReplacingProducts(Composite composite) throws IOException;

	boolean isODELike();

	//public void combineProducts(IComposite products);

	
	public int compareReagentsAndProducts(ICRNReaction o);

	void addCommentLines(Collection<String> commentLines);
	public void addCommentLine(String commentLine);
	public Collection<String> getComments();
}
