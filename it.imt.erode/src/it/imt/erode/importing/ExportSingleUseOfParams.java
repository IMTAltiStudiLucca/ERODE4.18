package it.imt.erode.importing;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.crn.implementations.CRN;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;

public class ExportSingleUseOfParams {

	public static ICRN expandMRMCCTMCWithSingleUseOfParameters(ICRN crn,MessageConsoleStream out, BufferedWriter bwOut) {
		long begin = System.currentTimeMillis();
		CRNReducerCommandLine.print(out,bwOut,"\tExpanding the parameters so that each parameter appears in one reaction... ");
		//
		//begin required params
		//IPartition obtainedPartition = partition;
		//HashMap<String,ISpecies> speciesNameToSpeciesCRN=null;
		LinkedHashMap<String, Integer> originalParamToOccurrences = new LinkedHashMap<>(crn.getParameters().size());
		boolean transformConstantRatesInNewParams=true;
		LinkedHashMap<Double, Integer> constantToId = new LinkedHashMap<>();
		LinkedHashMap<Double, Integer> constantToOccurrences = new LinkedHashMap<>();
		//end required params
		//Step 0: I want each parameter to appear in one reaction only. Therefore, I rename every occurrence of the same parameter appearing in a different reaction
		ICRN crnWithSingleUseOfParams = null;
		try {
			crnWithSingleUseOfParams = CRN.copyCRN(crn, out, bwOut,false,true);
		} catch (IOException e1) {
			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because I had problems in renaming the occurrences of the same parameter in different reactions: "+e1.getMessage()+". I terminate.");
			return null;
		}
		//LinkedHashMap<String, String> originalParamToExpr = new LinkedHashMap<>(crn.getParameters().size());
		for(String param : crn.getParameters()) {
			int space = param.indexOf(' ');
			String parName=param.substring(0,space).trim();
			originalParamToOccurrences.put(parName, 0);
			//String parExpr = param.substring(space+1,param.length()).trim();
			//originalParamToExpr.put(parName, parExpr);
		}
		int idConstant=0;
		for(ICRNReaction reaction : crnWithSingleUseOfParams.getReactions()) {
			String rateExpr = reaction.getRateExpression();
			Integer occurrences = originalParamToOccurrences.get(rateExpr);
			if(occurrences==null) {
				//The rate was not a parameter. If it is a constant I can ignore it, otherwise the model is not supported.
				try {
					Double constant = Double.valueOf(rateExpr);
					if(transformConstantRatesInNewParams) {
						Integer currentId=constantToId.get(constant);
						if(currentId==null) {
							constantToId.put(constant, idConstant);
							currentId=idConstant;
							idConstant++;
						}
						Integer occ = constantToOccurrences.get(constant);
						if(occ==null) {
							occ=0;
						}
						occ=occ+1;
						constantToOccurrences.put(constant, occ);
						String newParamName="c_"+currentId+"_"+occ;
						reaction.setRate(BigDecimal.valueOf(constant), newParamName);
						crnWithSingleUseOfParams.addParameter(newParamName, rateExpr);
						crnWithSingleUseOfParams.getMath().setVariable(newParamName, constant);
					}
					if(constant==null) {
						//if isAGeneratedCME==true, remove '(,),*' and add as new param
						CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because reactions must have only either constants of parameters as rates: "+rateExpr+". I terminate.");
						return null;
					}
				}
				catch(NumberFormatException e) {
					//if isAGeneratedCME==true, remove '(,),*' and add as new param
					CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because reactions must have only either constants of parameters as rates: "+rateExpr+". I terminate.");
					return null;
				}
			}
			else {
				//The rate was a parameter
				occurrences=occurrences+1;
				originalParamToOccurrences.put(rateExpr, occurrences);
				String newParamName = rateExpr+"_"+occurrences; 
				double newParamValue = crn.getMath().evaluate(rateExpr);
				reaction.setRate(BigDecimal.valueOf(newParamValue), newParamName);
				crnWithSingleUseOfParams.addParameter(newParamName, String.valueOf(newParamValue));
				crnWithSingleUseOfParams.getMath().setVariable(newParamName, newParamValue);
			}
		}

		crnWithSingleUseOfParams.setUserDefinedPartition(crn.getUserDefinedPartition());

		//		//Now I have to copy the partition for the new CRN.
		//		speciesNameToSpeciesCRN = new HashMap<>(crnWithSingleUseOfParams.getSpecies().size());
		//		for(ISpecies species : crnWithSingleUseOfParams.getSpecies()) {
		//			speciesNameToSpeciesCRN.put(species.getName(), species);
		//		}
		//		IPartition obtainedPartitionForCRNWithSingleUseOfParams = ControlEquivalences.copyPartitionUsingCopiedCRN(obtainedPartition,speciesNameToSpeciesCRN, crnWithSingleUseOfParams);

		long end = System.currentTimeMillis();
		String timeString = String.format(CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) );
		CRNReducerCommandLine.println(out,bwOut,"completed. From "+crn.getParameters().size()+" to " +crnWithSingleUseOfParams.getParameters().size()+" params. Time necessary: "+timeString+ " (s).");
		return crnWithSingleUseOfParams;
	}


	public static void expandMRMCCTMCWithSingleUseOfParametersModifyingTheModel(ICRN crn,MessageConsoleStream out, BufferedWriter bwOut) {
		long begin = System.currentTimeMillis();
		CRNReducerCommandLine.print(out,bwOut,"\tExpanding the parameters so that each parameter appears in one reaction... ");
		CRNReducerCommandLine.println(out,bwOut,"\n\tBeware: this functionality modifies the current model.");
		//
		//begin required params
		//IPartition obtainedPartition = partition;
		//HashMap<String,ISpecies> speciesNameToSpeciesCRN=null;
		//LinkedHashMap<String, Integer> originalParamToOccurrences = new LinkedHashMap<>(crn.getParameters().size());
		//boolean transformConstantRatesInNewParams=true;
		LinkedHashMap<Double, Integer> constantToId = new LinkedHashMap<>();
		int occ=0;
		//end required params
		//Step 0: I want each parameter to appear in one reaction only. Therefore, I rename every occurrence of the same parameter appearing in a different reaction
		//I assume there are no parameters.
		int idConstant=0;
		for(ICRNReaction reaction : crn.getReactions()) {
			double constant = reaction.getRate().doubleValue();

			Integer currentId=constantToId.get(constant);
			if(currentId==null) {
				constantToId.put(constant, idConstant);
				currentId=idConstant;
				idConstant++;
			}
			occ=occ+1;
			String newParamName="c_"+currentId+"_"+occ;
			reaction.setRate(BigDecimal.valueOf(constant), newParamName);
			crn.addParameter(newParamName, String.valueOf(constant));
			crn.getMath().setVariable(newParamName, constant);
		}

		long end = System.currentTimeMillis();
		String timeString = String.format(CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) );
		CRNReducerCommandLine.println(out,bwOut,"\tCompleted: "+crn.getParameters().size()+" params. Time necessary: "+timeString+ " (s).");
	}

}
