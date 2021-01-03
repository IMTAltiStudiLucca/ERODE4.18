package it.imt.erode.simulation.stochastic.fern;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.sbml.jsbml.ASTNode;

import fern.network.AbstractKineticConstantPropensityCalculator;
import fern.network.AmountManager;
import fern.network.ComplexDependenciesPropensityCalculator;
import fern.simulation.Simulator;
import it.imt.erode.crn.implementations.CRNReactionArbitraryAbstract;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.expression.evaluator.MathEval;

/**
 * Propensity calculator which is used for CRNs with reactions with arbitrary kinetics. The propensities are 
 * calculated by using the object representing the kinetic law for each reaction.
 * 
 * This file has been written by taking inspiration from fern.network.sbml.SBMLPropensityCalculator of Florian Erhard
 * 
 * @author Andrea Vandin 
 *
 */
public class ArbitraryKineticsCRNPropensityCalculator extends AbstractKineticConstantPropensityCalculator implements ComplexDependenciesPropensityCalculator {

	private ICRNReaction[] reactions;
	private String[] rateLawsString;
	private ISpecies[][] speciesInRateExpression;
	private List<Integer>[] speciesIDsInRateExpression;
	private FernNetworkFromLoadedCRN net;
	private MathEval math;
	
	double[] kineticConstants;
	/*private int[][] reactantHistosKeys = null;
	private int[][] reactantHistosVals = null;
	private int[][] reactants;*/
	
	@SuppressWarnings("unchecked")
	public ArbitraryKineticsCRNPropensityCalculator(FernNetworkFromLoadedCRN net, ICRN loadedCRN, int[][] reactants) {
		super(reactants);
		this.net=net;
		this.math = loadedCRN.getMath();
		rateLawsString=new String[loadedCRN.getReactions().size()];

		speciesInRateExpression = new ISpecies[net.getNumReactions()][];
		speciesIDsInRateExpression = (List<Integer>[]) Array.newInstance(new ArrayList<Integer>().getClass(), net.getNumReactions());
		
		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(loadedCRN.getSpecies().size());
		ISpecies[] speciesIdToSpecies = new ISpecies[loadedCRN.getSpecies().size()];
		int i=0;
		for (ISpecies species : loadedCRN.getSpecies()) {
			speciesNameToSpecies.put(species.getName(), species);
			speciesIdToSpecies[i]=species;
			i++;
		}
		
		kineticConstants=new double[loadedCRN.getReactions().size()];
		reactions=new ICRNReaction[loadedCRN.getReactions().size()];
		
		i=0;
		for(ICRNReaction reaction : loadedCRN.getReactions()){
			reactions[i]=reaction;
			if(reaction instanceof CRNReactionArbitraryAbstract){
				CRNReactionArbitraryAbstract arb = (CRNReactionArbitraryAbstract)reaction;
				rateLawsString[i]=arb.getRateExpression();
				List<ASTNode> app = arb.getSpeciesInRateLaw(arb.getRateLaw(),speciesNameToSpecies);
				//rateLaws[i] = arb.getRateLaw().clone();
				//speciesASTNodeInRateExpression[i] = arb.getSpeciesInRateLaw(rateLaws[i],speciesNameToSpecies);
				speciesInRateExpression[i]=new ISpecies[app.size()];
				speciesIDsInRateExpression[i]=new ArrayList<Integer>(app.size());
				int p=0;
				for (ASTNode speciesNode : app) {
					ISpecies species = arb.getSpecies(speciesNode,speciesNameToSpecies,speciesIdToSpecies);
					speciesInRateExpression[i][p]=species;
					speciesIDsInRateExpression[i].add(species.getID());
					p++;
				}
			}
			else{
				kineticConstants[i]=reaction.getRate().doubleValue();
				/*rateLaws[i]=new ASTNode(reaction.getRate().doubleValue());
				speciesASTNodeInRateExpression[i]=new ArrayList<ASTNode>(0);
				speciesInRateExpression[i]=new ArrayList<ISpecies>(0);
				speciesIDsInRateExpression[i]=new ArrayList<Integer>();*/
				speciesIDsInRateExpression[i]=new ArrayList<Integer>(reaction.getReagents().computeArity());
				for(int s=0;s<reaction.getReagents().getNumberOfDifferentSpecies();s++){
					ISpecies species = reaction.getReagents().getAllSpecies(s);
					for(int m=0;m<reaction.getReagents().getMultiplicities(s);m++){
						speciesIDsInRateExpression[i].add(species.getID());
					}
				}
			}
			i++;
		}
	}
	
	public double getConstant(int i) {
		return kineticConstants[i];
	}
	
	public void setConstant(int i, double value) {
		kineticConstants[i] = value;
	}
	
	@Override
	public double calculatePropensity(int reaction, AmountManager amount, Simulator sim) { 
		double re=-1;
		if(reactions[reaction] instanceof CRNReactionArbitraryAbstract){
			for(int s=0;s<speciesInRateExpression[reaction].length;s++){
				ISpecies species = speciesInRateExpression[reaction][s];
				int id = net.getSpeciesMapping().get(species.getNameAlphanumeric());
				
				//speciesASTNodeInRateExpression[reaction].get(s).setValue(amount.getAmount(id));
				//TODO: PAY ATTENTION: FOR NAMES NOT SUPPORTED BY MATHEVAL WE WILL HAVE PROBLEMS
				String name=species.getName();
				math.setVariable(name, amount.getAmount(id));
			}
			
			re = math.evaluate(rateLawsString[reaction]);
		}
		else{
			// get the total rate in normal mass action
			/*re = kineticConstants[reaction];
			IComposite lhs=reactions[reaction].getReagents();
			for(int i=0;i<lhs.getNumberOfDifferentSpecies();i++){
				ISpecies s = lhs.getAllSpecies(i);
				int id = net.getSpeciesMapping().get(s.getNameAlphanumeric());
				int mult = lhs.getMultiplicities(i);
				if(mult==1){
					re*=amount.getAmount(id);
				}
				else{
					re *= Math.pow(amount.getAmount(id), mult);
				}
			}*/
			
			re = super.calculatePropensity(reaction, amount, sim);
		}
		if (re<0){ 
			throw new RuntimeException("The propensity of reaction "+sim.getNet().getReactionName(reaction)+" is negative");
		}
		//return Math.abs(re);
		return re;
	}

	@Override
	public List<Integer> getKineticLawSpecies(int reaction) {
		return speciesIDsInRateExpression[reaction];
	}
		
}
