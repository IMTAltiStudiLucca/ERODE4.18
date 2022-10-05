package sbml.conversion.functionterm;

import java.util.HashSet;
import java.util.LinkedHashMap;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.ext.qual.FunctionTerm;

import it.imt.erode.booleannetwork.updatefunctions.FalseUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.MVUpdateFunctionByCases;
import it.imt.erode.booleannetwork.updatefunctions.NotBooleanUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.Otherwise;
import it.imt.erode.booleannetwork.updatefunctions.TrueUpdateFunction;
import sbml.conversion.nodes.INodeConverter;
import sbml.conversion.nodes.NodeManager;

public class FunctionTermReader {

	private boolean multivalued;

	public FunctionTermReader(boolean multivalued) {
		this.multivalued=multivalued;
	}

	public IUpdateFunction convert(ListOf<FunctionTerm> functionTerms) {
		if(multivalued) {
			int max=0;
			boolean otherwise=false;
			HashSet<Integer> values=new HashSet<>(functionTerms.size());
			LinkedHashMap<Integer, IUpdateFunction> cases=new LinkedHashMap<>(functionTerms.size());
			for(FunctionTerm functionTerm : functionTerms) {
				if(!functionTerm.isSetResultLevel()) {
					throw new IllegalArgumentException("The function term has setResultLevel=false");
				}
				IUpdateFunction cur_case;
				if(functionTerm.isDefaultTerm()) {
					if(otherwise) {
						throw new IllegalArgumentException("I found more than one otherwise case.\n"+functionTerms);
					}
					otherwise=true;
					cur_case=new Otherwise();
				}
				else {
					ASTNode node = functionTerm.getMath();
					INodeConverter converter = NodeManager.create(node,multivalued);
					cur_case=converter.getUpdateFunction();
				}
				//int val=functionTerm.getLevel();NO!
				int val=functionTerm.getResultLevel();
				if(val<0) {
					throw new IllegalArgumentException("I found a negative activation value:"+val+"\n"+functionTerms);
				}
				boolean newVal=values.add(val);
				if(!newVal) {
					throw new IllegalArgumentException("I found more cases for the same activation value:"+val+"\n"+functionTerms);
				}
				cases.put(val, cur_case);
				max=Math.max(val, max);
			}
			MVUpdateFunctionByCases upFuncCases=new MVUpdateFunctionByCases(cases, max);
			return upFuncCases;
		}
		else {
			FunctionTerm functionTerm = getResultLevel(functionTerms,1,false);
			if(functionTerm==null) {
				//I need to check for 0
				functionTerm = getResultLevel(functionTerms,0,true);
				IUpdateFunction toBeNegated = extracted(functionTerm);
				if(toBeNegated instanceof TrueUpdateFunction) {
					return new FalseUpdateFunction();
				}
				else {
					return new NotBooleanUpdateFunction(toBeNegated);
				}
			}
			else {
				return extracted(functionTerm);
			}
			
			
		}
	}

	private IUpdateFunction extracted(FunctionTerm functionTerm) {
		if(functionTerm.isDefaultTerm()) {
			return new TrueUpdateFunction();
		}
		else {
			ASTNode node = functionTerm.getMath();
			INodeConverter converter = NodeManager.create(node,multivalued);
			return converter.getUpdateFunction();
		}
	}
	
	private FunctionTerm getResultLevel(ListOf<FunctionTerm> functionTerms, int level,boolean throwException) {
		for(FunctionTerm f : functionTerms) {
			if(f.isSetResultLevel() && f.getResultLevel() == level)
				return f;
		}
		if(throwException) {
			throw new IllegalArgumentException("No function term with result level " + level + " found");
		}
		else {
			return null;
		}
		
	}
}
