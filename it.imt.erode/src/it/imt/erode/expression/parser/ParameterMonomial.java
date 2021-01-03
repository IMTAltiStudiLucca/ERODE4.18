package it.imt.erode.expression.parser;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;

import it.imt.erode.crn.implementations.CRNReaction;
import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;

public class ParameterMonomial extends Monomial {

	private BigDecimal val;
	private String parameterName;
	
	public ParameterMonomial(BigDecimal val, String parameterName) {
		this.val=val;
		setCoefficientExpr(parameterName);
		this.parameterName=parameterName;
	}
	
	@Override
	public String toString() {
		return parameterName;
	}
	
	@Override
	public ICRNReactionAndBoolean toReaction(ISpecies product, ISpecies I) {
		HashMap<ISpecies, Integer> compositeHM = new HashMap<>(2);
		compositeHM.put(I, 1);
		compositeHM.put(product, 1);
	
		IComposite products = new Composite(compositeHM);
		ICRNReaction reaction = new CRNReaction(val, (IComposite)I, products, getCoefficientExpr(),null);
		//I need the species I
		return new ICRNReactionAndBoolean(reaction, true);
	}

	@Override
	public void computeSpecies(HashMap<ISpecies, Integer> allSpecies) {
		//do nothing
	}

	@Override
	public BigDecimal getOrComputeCoefficient() {
		//return val;
		BigDecimal bd = getCoefficient();
		if(bd==null){
			setCoefficient(val);
		}
		return getCoefficient();
	}

	@Override
	public String getOrComputeCoefficientExpression() {
		return getCoefficientExpr();
	}
	
	@Override
	public String getOrComputeCoefficientExpression(Collection<String> parametersToConsider) {
		if(parametersToConsider.contains(parameterName)){
			return getOrComputeCoefficientExpression();
		}
		else{
			return getOrComputeCoefficient().toPlainString();
		}
		
	}

	@Override
	public void computeParameters(HashMap<String, Integer> parameters) {
		Integer prev = parameters.get(parameterName);
		if(prev==null){
			prev=1;
		}
		else{
			prev+=1;
		}
		parameters.put(parameterName, prev);
		
	}

	@Override
	public BigDecimal getOrComputeCoefficientOfParameter() {
		BigDecimal bd = getCoefficientParam();
		if(bd==null){
			setCoefficientParam(BigDecimal.ONE);
		}
		return getCoefficientParam();
	}


}
