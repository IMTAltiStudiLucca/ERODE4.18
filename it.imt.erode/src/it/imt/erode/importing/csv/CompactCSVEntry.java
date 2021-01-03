package it.imt.erode.importing.csv;

import java.math.BigDecimal;

public class CompactCSVEntry {

	private int row;
	private int col;
	private BigDecimal val;
	
	public CompactCSVEntry(int row, int col, BigDecimal val) {
		super();
		this.row = row;
		this.col = col;
		this.val = val;
	}

	protected int getRow() {
		return row;
	}

	protected int getCol() {
		return col;
	}

	protected BigDecimal getVal() {
		return val;
	}

	@Override
	public String toString() {
		return "CompactCSVEntry [row=" + row + ", col=" + col + ", val=" + val+ "]";
	}
	
	
	
	
	
}
