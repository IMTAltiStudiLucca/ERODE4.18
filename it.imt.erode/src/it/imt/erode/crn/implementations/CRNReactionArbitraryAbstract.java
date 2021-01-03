package it.imt.erode.crn.implementations;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.text.parser.ParseException;

import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;

public abstract class CRNReactionArbitraryAbstract extends CRNReactionAbstract {

	private ASTNode rateLaw;
	
	public CRNReactionArbitraryAbstract(IComposite reagents, IComposite products,String body,String id) throws IOException {
		super(reagents, products, body/*, false*/,true,id);
		try {
			ASTNode rateLaw = ASTNode.parseFormula(body);
			this.setRateLaw(rateLaw,body);
		} catch (ParseException e) {
			throw new IOException(e.getMessage());
		}
	}
	public CRNReactionArbitraryAbstract(IComposite reagents, IComposite products,String body,ASTNode rateLaw,String id){
		super(reagents, products, body/*, false*/,true,id);
		this.setRateLaw(rateLaw,body);
	}
	
	
	

	public BigDecimal getRate() {
		throw new UnsupportedOperationException("Arbitrary reactions do not have a real-valued rate, but a function");
	}

	@Override
	public void setRate(BigDecimal rate, String rateExpr) {
		throw new UnsupportedOperationException("Arbitrary reactions do not have a real-valued rate, but a function");
	}

	public ASTNode getRateLaw() {
		return rateLaw;
	}

	public void setRateLaw(ASTNode rateLaw) {
		this.setRateLaw(rateLaw,rateLaw.toFormula());
	}
	
	public void setRateLaw(ASTNode rateLaw, String rateExpression) {
		this.rateLaw = rateLaw;
		setRateExpression(rateExpression);
	}

	@Override
	public int compareTo(ICRNReaction o) {
		int cmp = compareReagentsAndProducts(o);
		if(cmp!=0){
			return cmp;
		}
		else{
			if(!o.hasArbitraryKinetics()){
				return 1;
			}
			//I have to compare the rate ...
			throw new UnsupportedOperationException("I still have to implement the comparison between two CRNReactionArbitrary");
		}
	}
	
	@Override
	public String toString(){	
		return getReagents() + "-- [arb: "+getRateExpression()+"] -->" + getProducts();
	}

	public abstract ASTNode replaceVar(String variablePrefix,String variableSuffix,int idIncrement, HashMap<String, ISpecies> speciesNameToSpecies);
	
	public abstract List<ASTNode> getSpeciesInRateLaw(ASTNode consideredRateLaw, HashMap<String, ISpecies> speciesNameToSpecies);

	//public abstract List<Integer> getSpeciesIDsInRateLaw(ASTNode consideredRateLaw, HashMap<String, ISpecies> speciesNameToSpecies);

	public abstract ISpecies getSpecies(ASTNode speciesNode, HashMap<String, ISpecies> speciesNameToSpecies, ISpecies[] speciesIdToSpecies);

}
