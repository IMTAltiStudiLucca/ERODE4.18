package it.imt.erode.simulation.output;

public class MutableBoolean {

	private boolean value;
	
	public MutableBoolean() {
		value=false;
	}

	public boolean getValue() {
		return value;
	}

	public void setValue(boolean value) {
		this.value = value;
	}
	
}
