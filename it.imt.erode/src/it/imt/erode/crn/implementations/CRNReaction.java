package it.imt.erode.crn.implementations;

import java.math.BigDecimal;

import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;

public class CRNReaction extends CRNReactionAbstract {

	private BigDecimal rate;
	
	/*public CRNReaction(BigDecimal rate,IComposite reagents,
			IComposite products, String rateExpression) {
		this(rate,reagents,products,rateExpression,false);
	}*/

	@Override
	public ICRNReaction cloneReplacingProducts(Composite newProducts) {
		return new CRNReaction(getRate(), getReagents(), newProducts, getRateExpression(),getID());
	}
	
	public CRNReaction(BigDecimal rate,IComposite reagents,
			IComposite products, String rateExpression
			/*,boolean hasHillKinetics*/,String id) {
		super(reagents, products, rateExpression, /*hasHillKinetics,*/false,id);
		this.rate = rate;
	}
	
	@Override
	public void setRate(BigDecimal rate, String rateExpression) {
		this.setRateExpression(rateExpression);
		this.rate=rate;
	}

	@Override
	public BigDecimal getRate() {
		return rate;
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
