package it.imt.erode.simulation.deterministic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.sbml.jsbml.ASTNode;

import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.implementations.CRNReactionArbitraryAbstract;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;

public class CRNVectorField {

	private ICRN crn;
	private Terminator terminator;
	private String[] rateLawsString;
	private double[] kineticConstants;
	private ISpecies[][] speciesInRateExpression;
	
	
	public CRNVectorField(ICRN crn, Terminator terminator) {
		this.crn=crn;
		this.terminator=terminator;
		init(crn);
	}

	private void init(ICRN crn) {
		ISpecies[] speciesIdToSpecies = new ISpecies[crn.getSpecies().size()];
		//crn.getSpecies().toArray(speciesIdToSpecies);
		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
		int i=0;
		for (ISpecies species : crn.getSpecies()) {
			speciesNameToSpecies.put(species.getName(), species);
			speciesIdToSpecies[i]=species;
			i++;
		}
		
		if(!crn.isMassAction()){
			rateLawsString = new String[crn.getReactions().size()];
			speciesInRateExpression= new ISpecies[crn.getReactions().size()][];
			i=0;
			for(ICRNReaction reaction : crn.getReactions()){
				if(reaction instanceof CRNReactionArbitraryAbstract){
					CRNReactionArbitraryAbstract arb = (CRNReactionArbitraryAbstract)reaction;
					rateLawsString[i]=arb.getRateExpression();
					List<ASTNode> app = arb.getSpeciesInRateLaw(arb.getRateLaw(),speciesNameToSpecies);
					speciesInRateExpression[i]=new ISpecies[app.size()];
					int p=0;
					for (ASTNode astNode : app) {
						speciesInRateExpression[i][p]=speciesNameToSpecies.get(astNode.getName());
						p++;
					}
				}
				else{
					rateLawsString[i]=reaction.getRateExpression();
					speciesInRateExpression[i]=new ISpecies[0];
				}
				i++;
			}
		}

		kineticConstants = computeKineticConstants(crn);
	}

	private static String[] computeKineticConstantsString(ICRN crn) {
		String[] kineticConstants=new String[crn.getReactions().size()];
		int r=0;
		for (ICRNReaction reaction : crn.getReactions()) {
			if(! (reaction instanceof CRNReactionArbitraryAbstract)){
				kineticConstants[r]=reaction.getRateExpression();
			}
			else {
				//I assume it is mass-action.
				return null;
			}
			r++;
		}
		return kineticConstants;
	}
	
	private static double[] computeKineticConstants(ICRN crn) {
		double[] kineticConstants=new double[crn.getReactions().size()];
		int r=0;
		for (ICRNReaction reaction : crn.getReactions()) {
			if(! (reaction instanceof CRNReactionArbitraryAbstract)){
				kineticConstants[r]=reaction.getRate().doubleValue();
			}
			r++;
		}
		return kineticConstants;
	}

	//protected ICRN getCRN(){
	public ICRN getCRN(){
		return crn;
	}
	
	public Terminator getTerminator(){
		return terminator;
	}

	public double[] computeNextydotOfMeans(double t,double[] y, double[] ydot) {
		Arrays.fill(ydot, 0);

		double[] nv = new double[crn.getReactions().size()];
		
		if(Terminator.hasToTerminate(terminator)){
			return nv;
		}
		
		int r=0; 

		for (ICRNReaction reaction : crn.getReactions()) {
			if(Terminator.hasToTerminate(terminator)){
				return nv;
			}
			IComposite lhs = reaction.getReagents();

			/*if(reaction.hasArbitraryKinetics()){
				// get the total rate in arbitrary kinetics (K S1 S2)
				StringTokenizer st = new StringTokenizer(reaction.getRateExpression());
				st.nextToken();
				String K = st.nextToken();
				nv[r] = crn.getMath().evaluate(K);
				nv[r] *= y[reaction.getFirstArbitraryReagent().getID()];
				nv[r] *= y[reaction.getSecondArbitraryReagent().getID()];	    			
			}
			else */
			if(reaction.hasArbitraryKinetics()){
				for(int s=0;s<speciesInRateExpression[r].length;s++){
					ISpecies species = speciesInRateExpression[r][s];
					//TODO: PAY ATTENTION: FOR NAMES NOT SUPPORTED BY MATHEVAL WE WILL HAVE PROBLEMS
					String name=species.getName();
					getCRN().getMath().setVariable(name, y[species.getID()]);
				}
				
				nv[r] = getCRN().getMath().evaluate(rateLawsString[r]);
			}
			else{
				// get the total rate in normal mass action
				//nv[r] = reaction.getRate().doubleValue();
				nv[r] = kineticConstants[r];
				for(int i=0;i<lhs.getNumberOfDifferentSpecies();i++){
					ISpecies s = lhs.getAllSpecies(i);
					int mult = lhs.getMultiplicities(i);
					if(mult==1){
						nv[r]*=y[s.getID()];
					}
					else{
						nv[r] *= Math.pow(y[s.getID()], mult);
					}
				}
			}
			
			IComposite rhs = reaction.getProducts();

			//If it is an "ODE reaction"
			if(lhs.getNumberOfDifferentSpecies()==1 && rhs.getNumberOfDifferentSpecies()==1 && 
					lhs.getAllSpecies(0).equals(rhs.getAllSpecies(0)) && 
					lhs.getMultiplicities(0)==1 && rhs.getMultiplicities(0)==2){
				ISpecies s = lhs.getAllSpecies(0);
				ydot[s.getID()] += nv[r];
			}
			else{
				// apply rate
				for(int i=0;i<lhs.getNumberOfDifferentSpecies();i++){
					ISpecies s = lhs.getAllSpecies(i);
					int mult = lhs.getMultiplicities(i);
					ydot[s.getID()] -= nv[r] * mult;
				}

				for(int i=0;i<rhs.getNumberOfDifferentSpecies();i++){
					ISpecies p = rhs.getAllSpecies(i);
					int mult = rhs.getMultiplicities(i);
					ydot[p.getID()] += mult * nv[r];
				}
			}
			r++;
		}

		return nv;
	}
	
	
	public static String[] computeNVString(ICRN crn, Terminator terminator) {
		String[] nv = new String[crn.getReactions().size()];
		
		if(Terminator.hasToTerminate(terminator)){
			return nv;
		}
		
		String[] kineticConstants=computeKineticConstantsString(crn);
		
		int r=0; 

		for (ICRNReaction reaction : crn.getReactions()) {
			if(Terminator.hasToTerminate(terminator)){
				return nv;
			}
			IComposite lhs = reaction.getReagents();

			//I ASSUME MASS-ACTION
			// get the total rate in normal mass action
			//nv[r] = reaction.getRate().doubleValue();
			nv[r] = kineticConstants[r];
			for(int i=0;i<lhs.getNumberOfDifferentSpecies();i++){
				ISpecies s = lhs.getAllSpecies(i);
				int mult = lhs.getMultiplicities(i);
				for(int m = 1; m<=mult;m++) {
					nv[r] = nv[r] + " * " +s.getName();
				}
			}
			r++;
		}
		return nv;
	}
	
}
