package it.imt.erode.crn.differentialHull;

import java.util.LinkedHashSet;

public class ParametersBlock {

	private LinkedHashSet<EvaluatedParameter> parameters;
	private EvaluatedParameter min;
	private EvaluatedParameter max;
	
	public ParametersBlock(){
		parameters=new LinkedHashSet<EvaluatedParameter>();
	}
	
	public void addParameter(EvaluatedParameter param){
		parameters.add(param);
		param.setBlock(this);
		if(min==null || param.getValue()<min.getValue()){
			min=param;
		}
		if(max==null || param.getValue()>max.getValue()){
			max=param;
		}
	}
	
	public LinkedHashSet<EvaluatedParameter> getParameters() {
		return parameters;
	}
	
	public EvaluatedParameter getMin() {
		return min;
	}
	
	public EvaluatedParameter getMax() {
		return max;
	}
	
	@Override
	public String toString() {
		return parameters.toString();
	}
	
}
