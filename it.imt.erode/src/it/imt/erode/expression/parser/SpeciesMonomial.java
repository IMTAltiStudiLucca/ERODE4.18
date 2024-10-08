package it.imt.erode.expression.parser;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;

import it.imt.erode.crn.implementations.CRNReaction;
import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;

public class SpeciesMonomial extends Monomial {

	private ISpecies species;
	
	public SpeciesMonomial(ISpecies species) {
		this.species=species;
	}
	
	@Override
	public boolean needsI() {
		return false;
	}
	
	@Override
	public String toString() {
		return species.getName();
	}

	@Override
	public ICRNReactionAndBoolean toReaction(ISpecies product, ISpecies I) {
		HashMap<ISpecies, Integer> compositeHM = new HashMap<>(2);
		if(species.equals(product)){
			compositeHM.put(species, 2);
		}
		else{
			compositeHM.put(species, 1);
			compositeHM.put(product, 1);
		}
		IComposite products = new Composite(compositeHM);
		ICRNReaction reaction = new CRNReaction(BigDecimal.ONE, (IComposite)species, products, "1",null);
		//I don't need the species I
		return new ICRNReactionAndBoolean(reaction,false);
	}

	@Override
	public void computeSpecies(HashMap<ISpecies, Integer> allSpecies) {
		Integer prev = allSpecies.get(species);
		if(prev==null){
			prev=1;
		}
		else{
			prev+=1;
		}
		allSpecies.put(species, prev);
	}

	@Override
	public BigDecimal getOrComputeCoefficient() {
		BigDecimal bd = getCoefficient();
		if(bd==null){
			setCoefficient(BigDecimal.ONE);
		}
		return getCoefficient();
	}
	
	@Override
	public String getOrComputeCoefficientExpression() {
		String expr = getCoefficientExpr();
		if(expr==null){
			setCoefficientExpr("1");
		}
		return getCoefficientExpr();
	}
	
	@Override
	public String getOrComputeCoefficientExpression(Collection<String> parametersToConsider) {
		return getOrComputeCoefficientExpression();
	}

	@Override
	public HashMap<String, Integer> getOrComputeParameters() {
		throw new UnsupportedOperationException("No ODE variables can appear in the equations of the parameters.");
	}

	@Override
	public BigDecimal getOrComputeCoefficientOfParameter() {
		throw new UnsupportedOperationException("No ODE variables can appear in the equations of the parameters.");
	}

	@Override
	public void computeParameters(HashMap<String, Integer> parameters) {
		throw new UnsupportedOperationException("No ODE variables can appear in the equations of the parameters.");
	}

	@Override
	public double eval(HashMap<ISpecies, Double> speciesToValue, HashMap<ISpecies, Double> forceReplacement) {
		//By default, a non-def variable has value zero
		Double val = speciesToValue.get(species);
		if(val==null) {
			val=0.0;
		}
		
		//I replace the value with the forced one, if any
		if(forceReplacement!=null) {
			Double forcedValue=forceReplacement.get(species);
			if(forcedValue!=null) {
				val=forcedValue;
			}
		}
		
		return val;
		
//		if(forcedReplacement==null) {
//			Double val = speciesToValue.get(species);
//			if(val==0) {
//				//By detault, a non-def variable has value zero
//				return 0;
//			}
//			else {
//				return val;
//			}
//		}
//		else {
//			double val = 0.0;
//			for(IMonomial mon : forcedReplacement) {
//				val += mon.eval(speciesToValue, null);
//			}
//			return val;
//		}
	}

}
