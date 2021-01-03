package it.imt.erode.importing.chemkin;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;

import it.imt.erode.crn.interfaces.ISpecies;

public class ThreeBodyAndPressureDependentReactionInfo {

	private HashMap<ISpecies, Integer> compositeReagentsHM;
	private HashMap<ISpecies, Integer> compositeProductsHM;
	private BigDecimal reactionRate;
	private String rateExpression;
	private BigDecimal reverseReactionRate;
	private String reverseRateExpression;
	private LinkedHashMap<ISpecies, Double> weights;
	private boolean reversible;

	public ThreeBodyAndPressureDependentReactionInfo(
			HashMap<ISpecies, Integer> compositeReagentsHM,
			HashMap<ISpecies, Integer> compositeProductsHM,
			BigDecimal reactionRate, String rateExpression,
			BigDecimal reverseReactionRate, String reverseRateExpression, LinkedHashMap<ISpecies, Double> weights, boolean reversible) {
		this.compositeReagentsHM=compositeReagentsHM;
		this.compositeProductsHM=compositeProductsHM;
		this.reactionRate=reactionRate;
		this.rateExpression=rateExpression;
		this.reverseReactionRate=reverseReactionRate;
		this.reverseRateExpression=reverseRateExpression;
		this.weights=weights;
		this.reversible=reversible;
	}

	protected HashMap<ISpecies, Integer> getCompositeReagentsHM() {
		return compositeReagentsHM;
	}

	protected HashMap<ISpecies, Integer> getCompositeProductsHM() {
		return compositeProductsHM;
	}

	protected BigDecimal getReactionRate() {
		return reactionRate;
	}

	protected String getRateExpression() {
		return rateExpression;
	}
	
	protected BigDecimal getReverseReactionRate() {
		return reverseReactionRate;
	}

	protected String getReverseRateExpression() {
		return reverseRateExpression;
	}

	protected LinkedHashMap<ISpecies, Double> getWeights() {
		return weights;
	}

	public boolean isReversible() {
		return reversible;
	}
	
	

}
