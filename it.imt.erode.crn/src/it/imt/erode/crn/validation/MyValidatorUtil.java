package it.imt.erode.crn.validation;

import it.imt.erode.crn.chemicalReactionNetwork.Import;
import it.imt.erode.crn.chemicalReactionNetwork.MassActionReduction;
import it.imt.erode.crn.chemicalReactionNetwork.importMatlab;
import it.imt.erode.crn.chemicalReactionNetwork.reduceBB;
import it.imt.erode.crn.chemicalReactionNetwork.reduceBE;
//import it.imt.erode.crn.chemicalReactionNetwork.reduceEMSB;
//import it.imt.erode.crn.chemicalReactionNetwork.reduceEpsNBB;
//import it.imt.erode.crn.chemicalReactionNetwork.reduceEpsNFB;
import it.imt.erode.crn.chemicalReactionNetwork.reduceFB;
import it.imt.erode.crn.chemicalReactionNetwork.reduceDSB;
import it.imt.erode.crn.chemicalReactionNetwork.reduceEFL;
import it.imt.erode.crn.chemicalReactionNetwork.reduceSMB;

/**
 * 
 * @author Andrea Vandin 
 *
 */
public class MyValidatorUtil {

	public static String[] checkMassActionReduction(MassActionReduction red, int nODEsList, int nonMassActionReactions, int nonElementaryReactions, int nSymbolicParameters, Import importCommand, int nAlgConstraints) {
		String name =  red.getClass().getName();
		name=name.substring(name.lastIndexOf('.')+1,name.lastIndexOf("Impl"));
		//I can do BE on DAEs
		if(nAlgConstraints>0 && red instanceof reduceBE) {
			return tryRNEncoding(name,red);
		}
		if(nODEsList>0 || nonMassActionReactions>0 || nSymbolicParameters>0){
			return needSMTReduction(name, red);
			/*
			String msg = "Reduction "+name+" can be applied only to mass action CRNs or polynomial ODEs with no symbolic parameters.";
			if(! (red instanceof MassActionEpsilonReduction || red instanceof reduceSMB)){
				return new String[]{msg, ChemicalReactionNetworkValidator.REDUCTION_MASSCTION};
			}
			else{
				return new String[]{msg, null};
			}*/
		}
		else if(nonElementaryReactions>0){
			if(red instanceof reduceBB || red instanceof reduceFB || red instanceof reduceSMB || red instanceof reduceDSB || red instanceof reduceEFL /*|| red instanceof reduceEMSB*/ ){
				return needNaryBisimulation(name,red);
			}
		}
		else if(importCommand != null){
			if(importCommand instanceof importMatlab){
				/*importMatlabODEs imp = (importMatlabODEs) importCommand;
				  if(!imp.isPolynomialODEs()){
					needSMTReduction(name);
				  }*/
				return needSMTReduction(name, red);
			}
			//still, we might have ternary or more CRNs for importMatlabODEs and importBoolCubeSBML
		}
		return null;
	}
	
	private static String[] tryRNEncoding(String name, MassActionReduction red){
		String msg = "Reduction "+name+" requires mass-action dynamics. The model will be encoded into a mass-action reaction networks, but this encoding will fail if the model is not linear or polynomial.";
			return new String[]{msg, "WARNING"};
	}
	
	private static String[] needNaryBisimulation(String name, MassActionReduction red){
		String msg = "Reduction "+name+" can be applied only to elementary mass action CRNs, or polynomial ODEs of degree at most two.";
		if(!(red instanceof reduceSMB /*|| red instanceof reduceEMSB*/)){
			return new String[]{msg, ChemicalReactionNetworkValidator.REDUCTION_TERNARYORMORE};
		}
		else{
			return new String[]{msg, null};
		}
	}
	
	private static String[] needSMTReduction(String name, MassActionReduction red){
		String msg = "Reduction "+name+" can be applied only to mass action CRNs or polynomial ODEs with no symbolic parameters.";
		
		if(! (/*red instanceof MassActionEpsilonReduction ||*/ red instanceof reduceSMB /*|| red instanceof reduceEMSB*/)){
			return new String[]{msg, ChemicalReactionNetworkValidator.REDUCTION_MASSCTION};
		}
		else{
			return new String[]{msg, null};
		}
	}
	
}
