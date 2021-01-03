package it.imt.erode.importing;

public class MutableValue<UnmutableType> {

	private UnmutableType value;
	
	public MutableValue(UnmutableType value) {
		this.value=value;
	}
	
	public UnmutableType getValue(){
		return value;
	}
	
	public void setValue(UnmutableType value){
		this.value=value;
	}
	
}