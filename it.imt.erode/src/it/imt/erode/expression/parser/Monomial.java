package it.imt.erode.expression.parser;

import java.math.BigDecimal;
import java.util.HashMap;

import it.imt.erode.crn.interfaces.ISpecies;

public abstract class Monomial implements IMonomial {	

	private BigDecimal coefficient;
	private HashMap<ISpecies, Integer> allSpecies;
	private String coefficientExpr;
	
	private HashMap<String, Integer> parameters;
	private BigDecimal coefficientParam;
	
	protected BigDecimal getCoefficient() {
		return coefficient;
	}

	protected void setCoefficient(BigDecimal coefficient) {
		this.coefficient = coefficient;
	}

	protected HashMap<ISpecies, Integer> getAllSpecies() {
		return allSpecies;
	}

	protected void setAllSpecies(HashMap<ISpecies, Integer> allSpecies) {
		this.allSpecies = allSpecies;
	}

	@Override
	public HashMap<ISpecies, Integer> getOrComputeSpecies() {
		if(getAllSpecies()==null){
			HashMap<ISpecies, Integer> allSpecies = new HashMap<ISpecies, Integer>(); 
			computeSpecies(allSpecies);
			setAllSpecies(allSpecies);
		}
		return getAllSpecies();
	}
	
	@Override
	public HashMap<String, Integer> getOrComputeParameters() {
		if(getParameters()==null){
			HashMap<String, Integer> parameters = new HashMap<>(); 
			computeParameters(parameters);
			setParameters(parameters);
		}
		return getParameters();
	}

	protected void setParameters(HashMap<String, Integer> parameters) {
		this.parameters=parameters;
		
	}

	protected HashMap<String, Integer> getParameters() {
		return parameters;
	}

	protected String getCoefficientExpr() {
		return coefficientExpr;
	}

	protected void setCoefficientExpr(String coefficientExpr) {
		this.coefficientExpr = coefficientExpr;
	}

	protected BigDecimal getCoefficientParam() {
		return coefficientParam;
	}

	protected void setCoefficientParam(BigDecimal coefficientParam) {
		this.coefficientParam = coefficientParam;
	}

}
