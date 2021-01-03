package it.imt.erode.crn.implementations;

import it.imt.erode.crn.interfaces.IComposite;

public abstract class CRNReactionAbstract extends CRNReactionAbstractOfAbstract//implements ICRNReaction 
{

	private String rateExpression;
	private boolean hasArbitraryKinetics;
	private String id;
	
	public CRNReactionAbstract(IComposite reagents,IComposite products, String rateExpression,/*boolean hasHillKinetics,*/ boolean hasArbitraryKinetics, String id) {
		super(reagents,products);
		this.rateExpression=rateExpression;
		this.hasArbitraryKinetics=hasArbitraryKinetics;
		this.id=id;
	}
	
	
	@Override
	public boolean hasID() {
		return (id!=null&& (!"".equals(id)));
	}
	
	@Override
	public String getID() {
		return id;
	}
	@Override
	public void setID(String newID) {
		id=newID;
	}

	@Override
	public String getRateExpression() {
		return rateExpression;
	}
	
	protected void setRateExpression(String rateExpression){
		this.rateExpression=rateExpression;
	}
	
	@Override
	public boolean hasArbitraryKinetics() {
		return hasArbitraryKinetics;
	}
	

}
