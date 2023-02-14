package it.imt.erode.expression.parser;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;

import it.imt.erode.crn.implementations.CRNReaction;
import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;

public class DivisionMonomial extends Monomial {

	private IMonomial left;
	private IMonomial right;
	
	public DivisionMonomial(IMonomial left, IMonomial right) {
		this.left=left;
		this.right=right;
		HashMap<ISpecies, Integer> speciesInDividend = right.getOrComputeSpecies();
		if(speciesInDividend.size()>0) {
			throw new UnsupportedOperationException("We can only divide by numeric expressions"); 
		}
	}
	
	@Override
	public boolean needsI() {
		return left.needsI() && right.needsI();
	}
	
	@Override
	public String toString() {
		return "("+left+" / "+right+")";
	}

	@Override
	public ICRNReactionAndBoolean toReaction(ISpecies product, ISpecies I) {
		BigDecimal factor = this.getOrComputeCoefficient();
		String factorExpr = this.getOrComputeCoefficientExpression();
		HashMap<ISpecies, Integer> allSpecies = new HashMap<>();
		computeSpecies(allSpecies);
		
		if(allSpecies.isEmpty()){
			return new NumberMonomial(factor,factorExpr).toReaction(product, I);
		}
		else {
			IComposite reagents = new Composite(allSpecies);
			Integer prev = allSpecies.get(product);
			if(prev==null){
				prev=1;
			}
			else{
				prev+=1;
			}
			allSpecies.put(product, prev);
			IComposite products = new Composite(allSpecies);

			ICRNReaction reaction = new CRNReaction(factor, reagents, products, factorExpr /*String.valueOf(factor)*/,null);
			//I don't need the species I
			return new ICRNReactionAndBoolean(reaction,false);
		}
	}

	@Override
	public BigDecimal getOrComputeCoefficient() {
		//return left.getOrComputeCoefficient() * right.getOrComputeCoefficient();
		BigDecimal bd = getCoefficient();
		if(bd==null){
			setCoefficient(left.getOrComputeCoefficient().multiply(right.getOrComputeCoefficient()));
		}
		return getCoefficient();
	}

	@Override
	public void computeSpecies(HashMap<ISpecies, Integer> allSpecies) {
		left.computeSpecies(allSpecies);
		right.computeSpecies(allSpecies);
	}
	
	@Override
	public String getOrComputeCoefficientExpression() {
		if(getCoefficientExpr()==null){
			String leftExpr = left.getOrComputeCoefficientExpression();
			if(leftExpr.equals("0")  || leftExpr.equals("0.0")){
				setCoefficientExpr("0");
				return getCoefficientExpr();
			}
			
			String rightExpr = right.getOrComputeCoefficientExpression();
			if(rightExpr.equals("0")  || rightExpr.equals("0.0")){
				setCoefficientExpr("0");
				return getCoefficientExpr();
			}
			
			if(leftExpr.equals("1")){
				setCoefficientExpr(rightExpr);
			}
			else if(rightExpr.equals("1")){
				setCoefficientExpr(leftExpr);
			} 
			else{
				setCoefficientExpr("( "+leftExpr+") * ("+rightExpr+")");
			}
		}
		return getCoefficientExpr();
	}
	
	@Override
	public String getOrComputeCoefficientExpression(Collection<String> parametersToConsider) {
		String leftExpr = left.getOrComputeCoefficientExpression(parametersToConsider);
		if(leftExpr.equals("0")  || leftExpr.equals("0.0")){
			return "0";
		}
		String rightExpr = right.getOrComputeCoefficientExpression(parametersToConsider);
		if(rightExpr.equals("0")  || rightExpr.equals("0.0")){
			throw new UnsupportedOperationException("Division by 0");
			//return "0";
		}
		
		String ret;
				
//		if(leftExpr.equals("1")){
//			ret = "1/("rightExpr+")";
//		}
//		else 
		if(rightExpr.equals("1")){
			ret = leftExpr;
		} 
		else{
			ret = "( "+leftExpr+") / ("+rightExpr+")";
		}
		return ret;
	}

	@Override
	public void computeParameters(HashMap<String, Integer> parameters) {
		left.computeParameters(parameters);
		right.computeParameters(parameters);
	}

	@Override
	public BigDecimal getOrComputeCoefficientOfParameter() {
		BigDecimal bd = getCoefficientParam();
		if(bd==null){
			setCoefficientParam(left.getOrComputeCoefficientOfParameter().divide(right.getOrComputeCoefficientOfParameter()));
		}
		return getCoefficientParam();
	}

}
