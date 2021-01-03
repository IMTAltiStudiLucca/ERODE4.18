package it.imt.erode.expression.parser;

import it.imt.erode.crn.interfaces.ICRNReaction;

public class ICRNReactionAndBoolean {

	private ICRNReaction reaction;
	private boolean boolValue;
	
	public ICRNReactionAndBoolean(ICRNReaction reaction, boolean val) {
		this.reaction=reaction;
		this.boolValue=val;
	}
	public ICRNReaction getReaction() {
		return reaction;
	}
	public void setReaction(ICRNReaction reaction) {
		this.reaction = reaction;
	}
	public boolean isBoolValue() {
		return boolValue;
	}
	public void setBoolValue(boolean boolValue) {
		this.boolValue = boolValue;
	}
	
	
}
