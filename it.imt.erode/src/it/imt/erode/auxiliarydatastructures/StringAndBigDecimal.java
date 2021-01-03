package it.imt.erode.auxiliarydatastructures;

import java.math.BigDecimal;

public class StringAndBigDecimal {

	private String string;
	private BigDecimal bigDecimal;
	
	public StringAndBigDecimal(String string, BigDecimal bigDecimal) {
		super();
		this.string = string;
		this.bigDecimal = bigDecimal;
	}

	public String getString() {
		return string;
	}

	public void setString(String string) {
		this.string = string;
	}

	public BigDecimal getBigDecimal() {
		return bigDecimal;
	}

	public void setBigDecimal(BigDecimal bigDecimal) {
		this.bigDecimal = bigDecimal;
	}
	
	
	
}
