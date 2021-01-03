package it.imt.erode.expression.parser;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;

import it.imt.erode.crn.implementations.CRNReaction;
import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;

public class NumberMonomial extends Monomial {

	private BigDecimal val;
	
	public NumberMonomial(BigDecimal val, String expr) {
		this.val=val;
		setCoefficientExpr(expr);
	}
	
	@Override
	public String toString() {
		return String.valueOf(val);
	}
	
	@Override
	public ICRNReactionAndBoolean toReaction(ISpecies product, ISpecies I) {
		HashMap<ISpecies, Integer> compositeHM = new HashMap<>(2);
		compositeHM.put(I, 1);
		compositeHM.put(product, 1);
	
		IComposite products = new Composite(compositeHM);
		ICRNReaction reaction = new CRNReaction(val, (IComposite)I, products, getCoefficientExpr(),null);
		//I need the species I
		boolean needI = true;
		return new ICRNReactionAndBoolean(reaction, needI);
	}

	@Override
	public void computeSpecies(HashMap<ISpecies, Integer> allSpecies) {
		//do nothing
	}
	
	@Override
	public void computeParameters(HashMap<String, Integer> parameters) {
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
		return getOrComputeCoefficientExpression();
	}

	@Override
	public BigDecimal getOrComputeCoefficientOfParameter() {
		BigDecimal bd = getCoefficientParam();
		if(bd==null){
			setCoefficientParam(val);
		}
		return getCoefficientParam();
	}



}
