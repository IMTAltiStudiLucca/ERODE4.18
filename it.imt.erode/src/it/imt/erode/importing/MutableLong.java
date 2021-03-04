package it.imt.erode.importing;

public class MutableLong {

	private long value=0;
	
	public MutableLong(long value) {
		this.value=value;
	}
	
	public long getValue(){
		return value;
	}
	
	public void setValue(long value){
		this.value=value;
	}
	
}
