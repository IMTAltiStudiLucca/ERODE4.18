package it.imt.erode.crn.implementations;

import java.util.List;

import it.imt.erode.crn.interfaces.ICommand;
//import it.imt.erode.crn.implementations.CommandParameter;

public class Command implements ICommand {

	private String name;
	private List<CommandParameter> parameters;
	
	public Command(String name, List<CommandParameter> parameters) {
		super();
		this.name = name;
		this.parameters = parameters;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public List<CommandParameter> getParameters() {
		return parameters;
	}

	private String toString(boolean odeFormat) {
		StringBuffer sb = new StringBuffer(getName());
		if(odeFormat){
			sb.append("(");
		}
		else{
			sb.append("({");
		}
		
		//int i=0;
		for (CommandParameter parameter : getParameters()) {
			if(odeFormat && parameter.getName().equals("computeOnlyPartition")) {
				continue;
			}
			else {
				sb.append(parameter.getName());

				if(odeFormat){
					sb.append("=");
				}
				else{
					sb.append("=>");
				}

				sb.append(parameter.getValue());
				sb.append(",");
				//i++;
			}
		}
		sb.delete(sb.length()-1, sb.length());//remove last ","
		
		if(odeFormat){
			sb.append(")");
		}
		else{
			sb.append("})");
		}
		
		return sb.toString();
	}
	
	@Override
	public String toCRNFormat() {
		return toString(false);
	}
	
	@Override
	public String toODEFormat() {
		return toString(true);
	}

}
