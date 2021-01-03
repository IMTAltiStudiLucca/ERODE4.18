package it.imt.erode.crn.implementations;

public class CommandParameter {

	private String name;
	private String value;
	
	public CommandParameter(String name,String value) {
		this.name=name;
		this.value=value;
	}
	
	public String getName(){
		return name;
	}
	
	public String getValue(){
		return value;
	}
	
}
