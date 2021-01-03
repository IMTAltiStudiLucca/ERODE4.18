package it.imt.erode.crn.differentialHull;

public class EvaluatedParameter {

	private String name;
	private double value;
	private ParametersBlock block;
	
	public EvaluatedParameter(String name,double value){
		this.name=name;
		this.value=value;
	}
	
	public String getName(){
		return name;
	}
	public double getValue(){
		return value;
	}

	public void setBlock(ParametersBlock parametersBlock) {
		this.block=parametersBlock;
	}
	public ParametersBlock getBlock(){
		return block;
	}
	
	@Override
	public String toString() {
		return name +"="+ value;
	}
	
	
}
