package it.imt.erode.partitionrefinement.splittersbstandcounters;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import it.imt.erode.expression.parser.IMonomial;

public class VectorOfCoefficientsForEachParameter {

	private final String[] keys;
	private final HashMap<String, BigDecimal> parametersToCoefficients;
	private BigDecimal bEntry=BigDecimal.ZERO;

	protected String[] getKeys() {
		return keys;
	}
	
	public Collection<String> getParameters(){
		return parametersToCoefficients.keySet();
	}
	
	public BigDecimal getCoefficient(String parameter) {
		BigDecimal val = parametersToCoefficients.get(parameter);
		if(val==null){
			return BigDecimal.ZERO;
		}
		else{
			return val;
		}
	}

/*	public VectorOfCoefficientsForEachParameter(HashMap<String, BigDecimal> parametersToCoefficients) {
		this.parametersToCoefficients = parametersToCoefficients;
		keys = new String[parametersToCoefficients.size()];
		parametersToCoefficients.keySet().toArray(keys);
		Arrays.sort(keys);
	}*/
	
	public VectorOfCoefficientsForEachParameter(ArrayList<IMonomial> monomials) {
		parametersToCoefficients = new HashMap<>(monomials.size());
		for (IMonomial monomial : monomials) {
			BigDecimal coefficient = monomial.getOrComputeCoefficientOfParameter();
			HashMap<String, Integer> parametersOfMonomial = monomial.getOrComputeParameters();
			if(parametersOfMonomial.size()==1 && parametersOfMonomial.values().iterator().next()==1){
				String parameter = parametersOfMonomial.keySet().iterator().next();
				BigDecimal prev = parametersToCoefficients.get(parameter);
				if(prev==null){
					prev = coefficient;
				}
				else{
					prev = prev.add(coefficient);
				}
				parametersToCoefficients.put(parameter, prev);
			}
			else if(parametersOfMonomial.size()==0){
				bEntry=bEntry.add(coefficient);
			}
			else{
				throw new UnsupportedOperationException("Non linear equations for parameters!");
			}
		}
		
		bEntry=BigDecimal.ZERO.subtract(bEntry);
		keys = new String[parametersToCoefficients.size()];
		parametersToCoefficients.keySet().toArray(keys);
		Arrays.sort(keys);
	}
	

	@Override
	public String toString() {
		return parametersToCoefficients.toString() +" = " + bEntry.toPlainString();
	}

	public double getBEntry() {
		return bEntry.doubleValue();
	}


}
