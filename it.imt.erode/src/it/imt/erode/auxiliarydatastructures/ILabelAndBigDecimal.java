package it.imt.erode.auxiliarydatastructures;

import java.math.BigDecimal;

import it.imt.erode.crn.label.ILabel;

public class ILabelAndBigDecimal {
	
	private ILabel label;
	private BigDecimal bd;
	public ILabelAndBigDecimal(ILabel label, BigDecimal bd) {
		super();
		this.label = label;
		this.bd = bd;
	}
	protected ILabel getLabel() {
		return label;
	}
	protected BigDecimal getBd() {
		return bd;
	}
	
	

}
