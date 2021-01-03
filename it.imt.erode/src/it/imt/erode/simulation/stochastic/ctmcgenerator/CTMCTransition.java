package it.imt.erode.simulation.stochastic.ctmcgenerator;

import java.math.BigDecimal;
import java.util.HashMap;

import it.imt.erode.crn.implementations.CRNReaction;
import it.imt.erode.crn.implementations.Species;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;

public class CTMCTransition {

	private IComposite source;
	private IComposite target;
	private BigDecimal rate;
	private String rateExpression;
	//private String id;
	private ICRNReaction reaction;

	public CTMCTransition(IComposite source, BigDecimal rate, String rateExpression,IComposite target, ICRNReaction reaction) {
		this.source=source;
		this.target=target;
		this.rate=rate;
		this.rateExpression=rateExpression;
		this.reaction=reaction;
		//id=null;
	}
//	public CTMCTransition(IComposite source, BigDecimal rate, String rateExpression,IComposite target, String id) {
//		this(source,rate,rateExpression,target);
//		this.id=id;
//	}

	public IComposite getTarget() {
		return target;
	}
	
	public BigDecimal getRate() {
		return rate;
	}
	
	public String getRateExpression() {
		return rateExpression;
	}
	
	public ICRNReaction getReaction() {
		return reaction;
	}
	
	public IComposite getSource() {
		return source;
	}

	public ICRNReaction toCRNReaction(HashMap<String, ISpecies> nameToSpecies) {
		ISpecies sourceSpecies = compositeToSpecies(nameToSpecies,source);
		ISpecies targetSpecies = compositeToSpecies(nameToSpecies,target);
		return new CRNReaction(rate, (IComposite)sourceSpecies, (IComposite)targetSpecies, rateExpression,null);
	}

	public static ISpecies compositeToSpecies(HashMap<String, ISpecies> nameToSpecies, IComposite sourceOrTarget) {
		String sourceOrTargetName = sourceOrTarget.toMultiSetWithStoichiometries(false);
		sourceOrTargetName=sourceOrTargetName.replace("*", "");
		sourceOrTargetName=sourceOrTargetName.replace("+", "_");
		sourceOrTargetName="s"+sourceOrTargetName;
		ISpecies sourceOrTargetSpecies = nameToSpecies.get(sourceOrTargetName);
		if(sourceOrTargetSpecies==null){
			sourceOrTargetSpecies = new Species(sourceOrTargetName, nameToSpecies.size(), BigDecimal.ZERO, "0", true,false);
			nameToSpecies.put(sourceOrTargetName, sourceOrTargetSpecies);
		}
		return sourceOrTargetSpecies;
	}

}
