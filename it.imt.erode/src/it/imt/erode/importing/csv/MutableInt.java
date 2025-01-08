package it.imt.erode.importing.csv;

public class MutableInt {
	
	private int val;
	
	public MutableInt(int val) {
		this.val=val;
	}
	
	public int getVal() {
		return val;
	}
	
	public void increaseVal() {
		val=val+1;
	}
	

}
