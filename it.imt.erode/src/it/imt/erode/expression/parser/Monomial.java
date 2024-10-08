package it.imt.erode.expression.parser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

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
	
	@Override
	public boolean isParameter() {
		return false;
	}
	
	public static final IMonomial zeroMonomial=new NumberMonomial(BigDecimal.ZERO, "0");
	public static NumberMonomial getAndAddIfNecessary(String bd,HashMap<String, NumberMonomial> bdToMon) {
		NumberMonomial mon= bdToMon.get(bd);
		if(mon==null) {
			mon=new NumberMonomial(new BigDecimal(bd) , bd);
			bdToMon.put(bd, mon);
		}
		return mon;
	}
	public static NumberMonomial getAndAddIfNecessary(BigDecimal bd,HashMap<String, NumberMonomial> bdToMon) {
		String bdStr=bd.toPlainString();
		NumberMonomial mon= bdToMon.get(bdStr);
		if(mon==null) {
			mon=new NumberMonomial(bd , bdStr);
			bdToMon.put(bdStr, mon);
		}
		return mon;
	}
	public static SpeciesMonomial getAndAddIfNecessary(ISpecies sp,HashMap<ISpecies, SpeciesMonomial> spToMon) {
		SpeciesMonomial mon= spToMon.get(sp);
		if(mon==null) {
			mon=new SpeciesMonomial(sp);
			spToMon.put(sp, mon);
		}
		return mon;
	}
	public static final NumberMonomial minusOneMon=new NumberMonomial(new BigDecimal(-1), "-1");
	public static final NumberMonomial oneMon=new NumberMonomial(new BigDecimal(1), "1");
	public static ProductMonomial getNegatedAndAddIfNecessary(ISpecies sp,HashMap<ISpecies, SpeciesMonomial> spToMon,HashMap<ISpecies, ProductMonomial> spToNegMon) {
		ProductMonomial mon= spToNegMon.get(sp);
		if(mon==null) {
			IMonomial spMon= getAndAddIfNecessary(sp, spToMon);
			mon= new ProductMonomial(minusOneMon, spMon);
			spToNegMon.put(sp, mon);
		}
		return mon;
	}
	public static IMonomial deriveInSpecies(IMonomial monomial, ISpecies spToDerive,HashMap<String, NumberMonomial> bdToMon,HashMap<ISpecies, SpeciesMonomial> spToMon) {
		HashMap<ISpecies, Integer> appearingSpecies = monomial.getOrComputeSpecies();
		Integer countsp=appearingSpecies.get(spToDerive);
		if(countsp==null || countsp==0) {
			return zeroMonomial;
		}
		else {
			BigDecimal coeff = monomial.getOrComputeCoefficient().multiply(new BigDecimal(countsp));
			IMonomial derivedMon=getAndAddIfNecessary(coeff, bdToMon);
			
			//NumberMonomial coeffDeriv= new NumberMonomial(BigDecimal.valueOf(countsp), countsp+"");
			//BigDecimal coeff = monomial.getOrComputeCoefficient();
			//String coeffExpr = monomial.getOrComputeCoefficientExpression();
			//IMonomial derivedMon = new ProductMonomial(coeffDeriv, new NumberMonomial(coeff, coeffExpr));
			
			//HashMap<ISpecies, Integer> derivedAppearingSpecies= new LinkedHashMap<ISpecies, Integer>(appearingSpecies);
			//derivedAppearingSpecies.put(spToDerive, countsp-1);

			for(Entry<ISpecies, Integer> entry:appearingSpecies.entrySet()) {
				int count=entry.getValue();
				ISpecies sp=entry.getKey();
				if(sp.equals(spToDerive)) {
					count=count-1;
				}
				IMonomial spMon = getAndAddIfNecessary(sp, spToMon); //new SpeciesMonomial(entry.getKey());
				for(int i=0;i<count;i++) {
					derivedMon=new ProductMonomial(derivedMon, spMon);
				}
			}

			return derivedMon;
		}
	}
	
	public static List<IMonomial> multiplyMonomials(List<IMonomial> leftMonomials, List<IMonomial> rightMonomials) {
		List<IMonomial> retMonomials;
		retMonomials=new ArrayList<>(leftMonomials.size()*rightMonomials.size());
		for(IMonomial left : leftMonomials) {
			for(IMonomial right : rightMonomials) {
				IMonomial prod = new ProductMonomial(left, right);
				retMonomials.add(prod);
			}
		}
		return retMonomials;
	}
	

}
