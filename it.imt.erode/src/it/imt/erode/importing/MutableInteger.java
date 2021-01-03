package it.imt.erode.importing;

public class MutableInteger {

	private int value=0;
	
	public MutableInteger(int value) {
		this.value=value;
	}
	
	public int getValue(){
		return value;
	}
	
	public void setValue(int value){
		this.value=value;
	}
	
}
