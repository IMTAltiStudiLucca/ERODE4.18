package it.imt.erode.expression.parser;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;

import it.imt.erode.crn.interfaces.ISpecies;

public class MinusMonomial extends Monomial {

	private IMonomial monomial;
	
	public MinusMonomial(IMonomial monomial) {
		this.monomial=monomial;
	}
	
	@Override
	public boolean needsI() {
		return monomial.needsI();
	}
	
	@Override
	public String toString() {
		return " -"+monomial.toString()+"";
	}

	@Override
	public ICRNReactionAndBoolean toReaction(ISpecies product, ISpecies I) {
		ICRNReactionAndBoolean reactionAndBool = monomial.toReaction(product, I);
		//reaction.setRate(BigDecimal.ZERO.subtract(reaction.getRate()), "( 0 - ("+reaction.getRateExpression()+"))");
		reactionAndBool.getReaction().setRate(getOrComputeCoefficient(), getOrComputeCoefficientExpression());
		return reactionAndBool;
	}

	@Override
	public void computeSpecies(HashMap<ISpecies, Integer> allSpecies) {
		monomial.computeSpecies(allSpecies);
	}

	@Override
	public BigDecimal getOrComputeCoefficient() {
		if(getCoefficient()==null){
			setCoefficient(BigDecimal.ZERO.subtract(monomial.getOrComputeCoefficient()));
		}
		return getCoefficient();
	}
	
	@Override
	public String getOrComputeCoefficientExpression() {
		if(getCoefficientExpr()==null){
			String son = monomial.getOrComputeCoefficientExpression();
			if(son.equals("0")||son.equals("0.0")){
				setCoefficientExpr("0");
			}
			else{
				//setCoefficientExpr("(0 - "+son+")");
				setCoefficientExpr("-("+son+")");
			}
			
			/*if(monomial instanceof NumberMonomial || monomial instanceof SpeciesMonomial){
				setCoefficientExpr("( 0 - "+monomial.getOrComputeCoefficientExpression()+")");
			}
			else{
				setCoefficientExpr("( 0 - ("+monomial.getOrComputeCoefficientExpression()+"))");
			}*/
			
		}
		return getCoefficientExpr();
	}
	
	@Override
	public String getOrComputeCoefficientExpression(Collection<String> parametersToConsider) {
		String son = monomial.getOrComputeCoefficientExpression(parametersToConsider);
		if(son.equals("0")||son.equals("0.0")){
			return "0";
		}
		else{
			return "( 0 - "+son+")";
		}
		
	}
	

	@Override
	public void computeParameters(HashMap<String, Integer> parameters) {
		monomial.computeParameters(parameters);
	}

	@Override
	public BigDecimal getOrComputeCoefficientOfParameter() {
		if(getCoefficientParam()==null){
			setCoefficientParam(BigDecimal.ZERO.subtract(monomial.getOrComputeCoefficientOfParameter()));
		}
		return getCoefficientParam();
	}

	@Override
	public double eval(HashMap<ISpecies, Double> speciesToValue, HashMap<ISpecies, Double> forceReplacement) {
		return 0 - monomial.eval(speciesToValue,forceReplacement);
	}

	
}
