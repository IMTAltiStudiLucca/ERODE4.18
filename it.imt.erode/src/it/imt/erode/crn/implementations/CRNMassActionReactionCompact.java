package it.imt.erode.crn.implementations;

import java.io.IOException;
import java.math.BigDecimal;

import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;

public class CRNMassActionReactionCompact extends CRNReactionAbstractOfAbstract //implements ICRNReaction
{

	private BigDecimal rate;
	private String rateExpression;
	
	public CRNMassActionReactionCompact(BigDecimal rate, IComposite reagents, IComposite products) {
		super(reagents,products);
		this.rate=rate;
	}
	
	public CRNMassActionReactionCompact(BigDecimal rate, IComposite reagents, IComposite products,String rateExpr) {
		this(rate,reagents,products);
		this.rateExpression=rateExpr;		
	}
	
	@Override
	public String getID() {
		return null;
	}
	
	@Override
	public void setID(String newID) {
		throw new UnsupportedOperationException("SpeciesCompact has limited features");
		
	}
	@Override
	public boolean hasID() {
		return false;
	}
	
	@Override
	public BigDecimal getRate() {
		return rate;
	}
	
	@Override
	public String getRateExpression() {
		if(rateExpression==null) {
			return rate.toPlainString();
		}
		else {
			return rateExpression;
		}
		
	}
	
	@Override
	public void setRate(BigDecimal rate, String rateExpr) {
		this.rate=rate;
		this.rateExpression=rateExpr;
	}
	
	
	@Override
	public boolean hasArbitraryKinetics() {
		return false;
	}
	
	@Override
	public ICRNReaction cloneReplacingProducts(Composite newProducts) throws IOException {
		return new CRNMassActionReactionCompact(rate, getReagents(), newProducts,rateExpression);
	}

	@Override
	public int compareTo(ICRNReaction o) {
		int cmp = compareReagentsAndProducts(o);
		if(cmp!=0){
			return cmp;
		}
		else{
			if(o.hasArbitraryKinetics()){
				return -1;
			}
			return rate.compareTo(((ICRNReaction) o).getRate());
		}
	}
	

}
