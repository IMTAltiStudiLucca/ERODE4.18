package it.imt.erode.partitionrefinement.algorithms;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.auxiliarydatastructures.IPartitionsAndBoolean;
import it.imt.erode.auxiliarydatastructures.Reduction;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.implementations.CRN;
import it.imt.erode.crn.implementations.CRNReaction;
import it.imt.erode.crn.implementations.Species;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.label.ILabel;
import it.imt.erode.crn.label.SimpleLabel;
import it.imt.erode.expression.evaluator.MathEval;
import it.imt.erode.importing.CRNImporter;
import it.imt.erode.importing.MatlabODEsImporter;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SpeciesCounterHandler;
import it.imt.erode.partitionrefinement.splittersbstandcounters.VectorOfBigDecimalsForEachLabel;

public class ControlEquivalencesLessEfficient {

	public static IPartitionsAndBoolean computeCoarsest(Reduction red, ICRN crn, IPartition partition,
			boolean verbose, MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator,
			IMessageDialogShower msgDialogShower, boolean transformConstantRatesInNewParams) {
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,red.toString()+" Reducing: "+crn.getName());
		}
		IPartition obtainedPartition = partition.copy();
		
		//Double all parameters so that the conditions of Remark 1 are satisfied.
		
		//int maxArity = crn.getMaxArity();
		if(!(crn.isMassAction() && crn.getMaxArity() <=1)){
			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it is not a unary mass action CRN (i.e., has reactions with more than one reagent, or has reactions with arbitrary rates). I terminate.");
			return new IPartitionsAndBoolean(obtainedPartition, false,null);
		}
		/*
		if(!crn.isMassAction()){
			CRNandAndPartition crnAndSpeciesAndPartition=MatlabODEPontryaginExporter.computeRNEncoding(crn, out, bwOut, partition,true);
			if(crnAndSpeciesAndPartition==null){
				CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it is not a mass action CRN (i.e., it has reactions with arbitrary rates). I terminate.");
				return new IPartitionsAndBoolean(obtainedPartition, false,null);
			}
			crn = crnAndSpeciesAndPartition.getCRN();
			if(crn==null){
				CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it is not a mass action CRN (i.e., it has reactions with arbitrary rates). I terminate.");
				return new IPartitionsAndBoolean(obtainedPartition, false,null);
			}
			obtainedPartition=crnAndSpeciesAndPartition.getPartition();
		}
		*/
		else if(crn.isSymbolic()){
			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it contains symbolic parameters (i.e. parameters with no acutal value assigned) I terminate.");
			return new IPartitionsAndBoolean(obtainedPartition, false,null);
		}
		else if(crn.algebraicSpecies()>0){
			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it is a system of differential algebraic equations (i.e. it has algebraic species) I terminate.");
			return new IPartitionsAndBoolean(obtainedPartition, false,null);
		}
		
		if(!(red.equals(Reduction.UFE))){
			CRNReducerCommandLine.printWarning(out,bwOut,"Please invoke this method using FCE.  I terminate.");
			return new IPartitionsAndBoolean(obtainedPartition, false,null);
		}

		if(verbose){
			String blocks = " blocks";
			if(obtainedPartition.size()==1){
				blocks = " block";
			}
			CRNReducerCommandLine.println(out,bwOut,"Before "+red.toString()+" partitioning we have "+ obtainedPartition.size() + blocks +" and "+crn.getSpecies().size()+ " species.");
		}
		
		/*
		if(!(CRNReducerCommandLine.hasToAddToReactionsWithNonZeroStoichiometry)){
			//CRNReducerCommandLine.printWarning(out,bwOut,"Not all necessary data structure have been filled. CRNReducerCommandLine.hasToAddToReactionsWithNonZeroStoichiometry must be set to true.");
			CRNReducerCommandLine.printWarning(out, bwOut, true, msgDialogShower, "Not all necessary data structure have been filled. CRNReducerCommandLine.hasToAddToReactionsWithNonZeroStoichiometry must be set to true.", DialogType.Error);
		}
		*/

		//long begin = System.currentTimeMillis();
		HashMap<String,ISpecies> speciesNameToSpeciesCRN=null;
		LinkedHashMap<String, Integer> originalParamToOccurrences = new LinkedHashMap<>(crn.getParameters().size());
		LinkedHashMap<Double, Integer> constantToId = new LinkedHashMap<>();
		LinkedHashMap<Double, Integer> constantToOccurrences = new LinkedHashMap<>();
		
		//this should be a parameter
		//boolean isAGeneratedCME=true;
		
		{
			long begin = System.currentTimeMillis();
			CRNReducerCommandLine.print(out,bwOut,"\n\tExpanding the parameters so that each parameter appears in one reaction... ");
			//Step 0: I want each parameter to appear in one reaction only. Therefore, I rename every occurrence of the same parameter appearing in a different reaction
			ICRN crnWithSingleUseOfParams = null;
			try {
				crnWithSingleUseOfParams = CRN.copyCRN(crn, out, bwOut,false,true);
			} catch (IOException e1) {
				CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because I had problems in renaming the occurrences of the same parameter in different reactions: "+e1.getMessage()+". I terminate.");
				return new IPartitionsAndBoolean(obtainedPartition, false,null);
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
							String newParamName="c_"+currentId+"_occ"+occ;
							reaction.setRate(BigDecimal.valueOf(constant), newParamName);
							crnWithSingleUseOfParams.addParameter(newParamName, rateExpr);
							crnWithSingleUseOfParams.getMath().setVariable(newParamName, constant);
						}
						if(constant==null) {
							//if isAGeneratedCME==true, remove '(,),*' and add as new param
							CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because reactions must have only either constants of parameters as rates: "+rateExpr+". I terminate.");
							return new IPartitionsAndBoolean(obtainedPartition, false,null);
						}
					}
					catch(NumberFormatException e) {
						//if isAGeneratedCME==true, remove '(,),*' and add as new param
						CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because reactions must have only either constants of parameters as rates: "+rateExpr+". I terminate.");
						return new IPartitionsAndBoolean(obtainedPartition, false,null);
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
			
			//Now I have to copy the partition for the new CRN.
			speciesNameToSpeciesCRN = new HashMap<>(crnWithSingleUseOfParams.getSpecies().size());
			for(ISpecies species : crnWithSingleUseOfParams.getSpecies()) {
				speciesNameToSpeciesCRN.put(species.getName(), species);
			}
			IPartition obtainedPartitionForCRNWithSingleUseOfParams = copyPartitionUsingCopiedCRN(obtainedPartition,speciesNameToSpeciesCRN, crnWithSingleUseOfParams);
			
			long end = System.currentTimeMillis();
			String timeString = String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) );
			CRNReducerCommandLine.println(out,bwOut,"completed. From "+crn.getParameters().size()+" to " +crnWithSingleUseOfParams.getParameters().size()+" params. Time necessary: "+timeString+ " (s).");
			crn = crnWithSingleUseOfParams;
			obtainedPartition=obtainedPartitionForCRNWithSingleUseOfParams;
		}
		
		
//		if(speciesNameToSpeciesCRN==null) {
//			speciesNameToSpeciesCRN = new HashMap<>(crn.getSpecies().size());
//			for(ISpecies species : crn.getSpecies()) {
//				speciesNameToSpeciesCRN.put(species.getName(), species);
//			}
//		}
		
		long begin = System.currentTimeMillis();
		CRNReducerCommandLine.print(out,bwOut,"\tCreating a side model for handling parameters partitions... ");
		LinkedHashMap<String, BigDecimal> paramToValue = new LinkedHashMap<>(crn.getParameters().size());
		LinkedHashMap<String, ISpecies> paramToParamSpecies = new LinkedHashMap<>(crn.getParameters().size());
		MathEval mathParams=new MathEval();
		ICRN fakeCRNForParams = new CRN(crn.getName()+"FakeForParams", mathParams, out, bwOut);
		int id =0;
		IPartition partitionOfParams = new Partition(crn.getParameters().size());
		
		for(String param : crn.getParameters()) {
			int space = param.indexOf(' ');
			String parName=param.substring(0,space).trim();
			double paramValue = crn.getMath().evaluate(parName);
			BigDecimal paramValueBD = BigDecimal.valueOf(paramValue);
			paramToValue.put(parName, paramValueBD);
			ISpecies paramAsSpecies = new Species(parName, id, paramValueBD, String.valueOf(paramValue), false);
			fakeCRNForParams.addSpecies(paramAsSpecies);
			paramToParamSpecies.put(parName, paramAsSpecies);
			id++;
		}
		if(originalParamToOccurrences.size()==0 && constantToOccurrences.size()==0) {
			IBlock uniqueBlockOfParams = new Block();
			partitionOfParams.add(uniqueBlockOfParams);
			for(ISpecies paramAsSpecies : fakeCRNForParams.getSpecies()) {
				uniqueBlockOfParams.addSpecies(paramAsSpecies);
			}
		}
		else {
			for(Entry<String, Integer> entry:originalParamToOccurrences.entrySet()) {
				String originalParam = entry.getKey();
				int occ = entry.getValue();
				IBlock currentBlockOfParams = new Block();
				partitionOfParams.add(currentBlockOfParams);
				for(int i=1;i<=occ;i++) {
					ISpecies paramAsSpecies = paramToParamSpecies.get(originalParam+"_"+i);
					currentBlockOfParams.addSpecies(paramAsSpecies);
				}
			}
			for(Entry<Double, Integer> entry:constantToOccurrences.entrySet()) {
				Double constant = entry.getKey();
				int occ = entry.getValue();
				int idOfConstant = constantToId.get(constant);
				String constantAsParamName_Prefix = "c_"+idOfConstant+"_occ";//+occ;
				IBlock currentBlockOfParams = new Block();
				partitionOfParams.add(currentBlockOfParams);
				for(int i=1;i<=occ;i++) {
					ISpecies paramAsSpecies = paramToParamSpecies.get(constantAsParamName_Prefix+i);
					currentBlockOfParams.addSpecies(paramAsSpecies);
				}
			}
		}
		CRNReducerCommandLine.print(out,bwOut,"("+fakeCRNForParams.getSpecies().size()+ " parameters pre-partitioned in "+partitionOfParams.size()+" blocks)... ");
		//We are restricting to the case in which each uncertain parameter is the rate of one reaction only.
		LinkedHashMap<String, ICRNReaction> paramToReactionOff = new LinkedHashMap<>(crn.getParameters().size());
		for(ICRNReaction reaction : crn.getReactions()) {
			String rateExpr = reaction.getRateExpression();
			BigDecimal value = paramToValue.get(rateExpr);
			if(value!=null) {
				//This reaction has an uncertain parameter as rate
				ICRNReaction old = paramToReactionOff.get(rateExpr);
				if(old!=null) {
					//ERROR! The rate appears in two reactions
					CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it the uncertain parameter "+rateExpr+" appears in more than one reaction. I terminate.");
					return new IPartitionsAndBoolean(obtainedPartition, false,null);
				}
				else {
					if(reaction.getProducts().computeArity()!=1) {
						//ERROR! The rate appears in reaction which is not a transition of an LTS
						CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because the uncertain parameter "+rateExpr+" appears in reaction which does not have exactly one product. I terminate.");
						return new IPartitionsAndBoolean(obtainedPartition, false,null);
					}
					paramToReactionOff.put(rateExpr, reaction);
				}

			}
			else {
				//The rate was not a parameter. If it is a constant I can ignore it, otherwise the model is not supported.
				try {
					Double constant = Double.valueOf(rateExpr);
					if(constant==null) {
						CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because reactions must have only either constants of parameters as rates: "+rateExpr+". I terminate.");
						return new IPartitionsAndBoolean(obtainedPartition, false,null);
					}
				}
					catch(NumberFormatException e) {
						CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because reactions must have only either constants of parameters as rates: "+rateExpr+". I terminate.");
						return new IPartitionsAndBoolean(obtainedPartition, false,null);
					}
				}
			}
		
		long end = System.currentTimeMillis();
		String timeString = String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) );
		CRNReducerCommandLine.println(out,bwOut,"completed. Time necessary: "+timeString+ " (s).");

		begin = System.currentTimeMillis();
		CRNReducerCommandLine.print(out,bwOut,"\tComputing the coarsest forward verification equivalence... ");
		int sizeOfPrevPartition = obtainedPartition.size();
		int sizeOfPrevParamPartition = partitionOfParams.size();
		do {
			sizeOfPrevPartition = obtainedPartition.size();
			sizeOfPrevParamPartition = partitionOfParams.size();
			
			//Step 1: compute coarsest FDE on curried-f[G]
			//Step 1.1 I have to apply the partition of parameters to the CRN (f[G])
			ICRN fG;
			try {
				//fG = CRN.copyCRN(crn, out, bwOut,true,false);
				fG = new CRN(crn.getName(),crn.getMath(),out,bwOut);
				CRN.copyParameters(crn, fG);
				ISpecies[] idToNewSpecies = CRN.copySpecies(crn, fG);
					//copy reactions, and at the same time replace parameters with representative.
				copyReactionsReplacingParamsWithRepresentative(crn, fG, idToNewSpecies,fakeCRNForParams,paramToParamSpecies,paramToValue,partitionOfParams);
			} catch (IOException e1) {
				CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because I had problems in computing f[G] (step: copying crn): "+e1.getMessage()+". I terminate.");
				return new IPartitionsAndBoolean(obtainedPartition, false,null);
			} catch (UnsupportedFormatException e) {
				CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because I had problems in computing f[G] (step: replacing parameter with representative): "+e.getMessage()+". I terminate.");
				return new IPartitionsAndBoolean(obtainedPartition, false,null);
			}
			
			/*
			if(partitionOfParams.size()!=crn.getParameters().size()) {
				try {
					replaceParamsWithRepresentative(fG,fakeCRNForParams,paramToParamSpecies,paramToValue,partitionOfParams);
				} catch (UnsupportedFormatException e) {
					CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because I had problems in computing f[G]: "+e.getMessage()+". I terminate.");
					return new IPartitionsAndBoolean(obtainedPartition, false,null);
				}
			}
			*/
			
			//Step 1.2 I have to compute the currying of f[G].
			HashMap<String,ISpecies> speciesNameToSpeciesOfCurriedfG = new HashMap<>(fG.getSpecies().size());
			LinkedHashSet<String> representativeParams = computeSetOfRepresentativeParams(partitionOfParams);
			ICRN curriedfG;
			try {
				curriedfG = MatlabODEsImporter.expandCRN(false,fG, representativeParams, speciesNameToSpeciesOfCurriedfG/*,false*/,false,true,true);
			} catch (UnsupportedFormatException e) {
				CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because an exception has been generated while currying it. I terminate.");
				CRNReducerCommandLine.printStackTrace(out, bwOut, e);
				return new IPartitionsAndBoolean(obtainedPartition, false,null);
			}
			
			//Step 1.3 Now I have to create the partition corresponding to 'obtainedPartition' for curriedFG
			//1.3.1 Copy each block replacing the original species with the new one
			IPartition obtainedPartitionCurriedfG = copyPartitionUsingCopiedCRN(obtainedPartition,speciesNameToSpeciesOfCurriedfG, curriedfG);
			//1.3.2 Create a new singleton block for each added parameter/species (each representing an equivalence class of the parameters
			for(String repParamAsSpecies : representativeParams) {
				IBlock currentfG = new Block();
				obtainedPartitionCurriedfG.add(currentfG);
				ISpecies speciesfG = speciesNameToSpeciesOfCurriedfG.get(repParamAsSpecies);
				currentfG.addSpecies(speciesfG);
			}
			
			// Step 1.4 Now I computed the coarsest FDE for curried-f[G] 
			//HashMap<IComposite, BigDecimal> multisetCoefficients = CRNBisimulationsNAry.computeMultisetCoefficients(curriedfG, terminator, curriedfG.getMaxArity());
			SpeciesCounterHandler speciesCounters[] = new SpeciesCounterHandler[curriedfG.getSpecies().size()];
			CRNBisimulationsNAry.refine(Reduction.FE, curriedfG, obtainedPartitionCurriedfG, /*multisetCoefficients,*/ speciesCounters, terminator,out,bwOut);
			
			//Step 1.5 Now I have to update 'obtainedPartition' for the original f according to the partition 'obtainedPartitionCurriedfG' computed for curried-f[G]
			obtainedPartition = copyPartitionUsingCopiedCRN(obtainedPartitionCurriedfG,speciesNameToSpeciesCRN, crn);
			
			//Step 2: I have to refine the partition of parameters of f using the computed FDE for curried-f[G]
			//Now, using the original system f, I have to refine the partition of parameters according to remark 1
			//The original reactions of interest are stored in paramToReaction
			refinePartitionOfParameters(obtainedPartitionCurriedfG,fakeCRNForParams,partitionOfParams,paramToReactionOff);
		}while(	sizeOfPrevPartition      != obtainedPartition.size() ||
				sizeOfPrevParamPartition != partitionOfParams.size()    );
		
		CRNReducerCommandLine.print(out,bwOut,"(parameters partitioned in "+ partitionOfParams.size()+" blocks)... ");
		
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"The final partition:");
			CRNReducerCommandLine.println(out,bwOut,obtainedPartition);
		}

		end = System.currentTimeMillis();
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,red+" Partitioning completed. From "+ crn.getSpecies().size() +" species to "+ obtainedPartition.size() + " blocks. Time necessary: "+(end-begin)+ " (ms)");
			CRNReducerCommandLine.println(out,bwOut,"");
		}
		return new IPartitionsAndBoolean(obtainedPartition,true,partitionOfParams);
	}

	private static IPartition copyPartitionUsingCopiedCRN(IPartition obtainedPartition,
			HashMap<String, ISpecies> speciesNameToSpeciesOfCopyCRN, ICRN copyCRN) {
		IPartition obtainedPartitionCopiedCRN = new Partition(copyCRN.getSpecies().size()); 
		IBlock current = obtainedPartition.getFirstBlock();
		while(current!=null) {
			IBlock currentCopiedBlock = new Block();
			obtainedPartitionCopiedCRN.add(currentCopiedBlock);
			for (ISpecies species : current.getSpecies()) {
				ISpecies speciesOfCopiedCRN = speciesNameToSpeciesOfCopyCRN.get(species.getName());
				if(speciesOfCopiedCRN!=null) {
					currentCopiedBlock.addSpecies(speciesOfCopiedCRN);
				}
			}
			if(currentCopiedBlock.getSpecies().isEmpty()) {
				obtainedPartitionCopiedCRN.remove(currentCopiedBlock,false);
			}
			current=current.getNext();
		}
		return obtainedPartitionCopiedCRN;
	}
	
	/*
	private static void replaceParamsWithRepresentative(ICRN crn, ICRN fakeCRNForParams, LinkedHashMap<String, ISpecies> paramToParamSpecies, LinkedHashMap<String, BigDecimal> paramToValue, IPartition partitionOfParams) throws UnsupportedFormatException {
		for(ICRNReaction reaction : crn.getReactions()) {
			String parName = reaction.getRateExpression();
			ISpecies paramSpecies = paramToParamSpecies.get(parName);
			//Each rate must be precisely one parameter
			if(paramSpecies==null) {
				throw new UnsupportedFormatException("I found a reaction without a parameter: "+reaction.toString());
			}
			else {
				String representativeParam=partitionOfParams.getBlockOf(paramSpecies).getRepresentative().getName();
				reaction.setRate(paramToValue.get(representativeParam), representativeParam);
			}
		}
	}
	*/
	
	public static void copyReactionsReplacingParamsWithRepresentative(ICRN crn, ICRN crnNew, ISpecies[] idToNewSpecies, ICRN fakeCRNForParams, LinkedHashMap<String, ISpecies> paramToParamSpecies, LinkedHashMap<String, BigDecimal> paramToValue, IPartition partitionOfParams) throws IOException, UnsupportedFormatException {
		//I have to copy each reaction, after replacing the param with its representative 
		for (ICRNReaction reaction : crn.getReactions()) {
			IComposite oldReagents =reaction.getReagents();
			HashMap<ISpecies, Integer> reagentsHM = new HashMap<>(oldReagents.getNumberOfDifferentSpecies());
			for(int i=0;i<oldReagents.getNumberOfDifferentSpecies();i++) {
				ISpecies newSpecies = idToNewSpecies[oldReagents.getAllSpecies(i).getID()];
				reagentsHM.put(newSpecies, oldReagents.getMultiplicities(i));
			}
			//IComposite newReagents = crnNew.addReagentsIfNew(new Composite(reagentsHM));
			IComposite newReagents = CRN.compositeOrSpecies(reagentsHM);

			IComposite oldProducts =reaction.getProducts();
			HashMap<ISpecies, Integer> productsHM = new HashMap<>(oldProducts.getNumberOfDifferentSpecies());
			for(int i=0;i<oldProducts.getNumberOfDifferentSpecies();i++) {
				ISpecies newSpecies = idToNewSpecies[oldProducts.getAllSpecies(i).getID()];
				productsHM.put(newSpecies, oldProducts.getMultiplicities(i));
			}
			//IComposite newProducts = crnNew.addProductIfNew(new Composite(productsHM));
			IComposite newProducts = CRN.compositeOrSpecies(productsHM);
			ICRNReaction newReaction;
			
			//String rateExpr = reaction.getRateExpression();
			String parName = reaction.getRateExpression();
			ISpecies paramSpecies = paramToParamSpecies.get(parName);
			//Each rate must be precisely one parameter
			if(paramSpecies==null) {
				throw new UnsupportedFormatException("I found a reaction without a parameter: "+reaction.toString());
			}
			String representativeParam=partitionOfParams.getBlockOf(paramSpecies).getRepresentative().getName();
			BigDecimal representativeValue = paramToValue.get(representativeParam); 
			
			if(reaction.hasArbitraryKinetics()) {
				//arbitrary
				throw new UnsupportedFormatException("I found a reaction with arbitrary kinetics: "+reaction.toString());
			}
			//mass action
			newReaction = new CRNReaction(representativeValue, newReagents, newProducts, representativeParam,reaction.getID());
			CRNImporter.addReaction(crnNew, newReagents, newProducts, newReaction);
		}
	}

	private static LinkedHashSet<String> computeSetOfRepresentativeParams(IPartition partitionOfParams) {
		LinkedHashSet<String> representativeParams = new LinkedHashSet<>(partitionOfParams.size());
		IBlock currentBlockOfParams = partitionOfParams.getFirstBlock();
		while(currentBlockOfParams!=null) {
			representativeParams.add(currentBlockOfParams.getRepresentative().getName());
			currentBlockOfParams=currentBlockOfParams.getNext();
		}
		return representativeParams;
	}

	private static void refinePartitionOfParameters(
			/*ICRN crn,*/ IPartition partitionOfSpecies, 
			ICRN fakeCRNForParams, IPartition partitionOfParams, 
			LinkedHashMap<String, ICRNReaction> paramToReaction) {
		//I have to split each block of partitionOfParams. 
		//	For each rate in the block, I take its reaction (there must be exactly one), and I compute the id of the blocks of its reagent and of its product. I split according to these ids.
		//		The id is just the id of the representative species of the block
		//E' un partitioning semplice. Non mi serve considerare splitter generators.
		
		HashSet<IBlock> splittedBlocksOfParams = new LinkedHashSet<>(partitionOfParams.size());
		
		for (ISpecies paramAsSpecies : fakeCRNForParams.getSpecies()) {
			IBlock block = partitionOfParams.getBlockOf(paramAsSpecies);
			//I can't do this check, becuase if you have p1 and p2 in a block, as soon as you remove p1, p2 believes to be in a singleton block. Instead, p2 should be handled as well as p1, because p2 might end up in the same subblock of p1. 
			/*if(block.getSpecies().size()==1) {
				continue;
			}
			else {*/
				splittedBlocksOfParams.add(block);
				ICRNReaction reactionOfTheRate = paramToReaction.get(paramAsSpecies.getName());
				HashMap<ILabel, BigDecimal> labelOfReaction = new HashMap<>(2);
				labelOfReaction.put(SimpleLabel.SOURCELABEL, BigDecimal.valueOf(partitionOfSpecies.getBlockOf(reactionOfTheRate.getReagents().getFirstReagent()).getRepresentative().getID()));
				labelOfReaction.put(SimpleLabel.TARGETLABEL, BigDecimal.valueOf(partitionOfSpecies.getBlockOf(reactionOfTheRate.getProducts().getFirstReagent()).getRepresentative().getID()));
				block.removeSpecies(paramAsSpecies);
				/*
				 * Insert the species in the tree associated to the block. 
				 * This method is used to split the block according to the computed labelOfReaction. This may cause the creation of a new block, in which case it is automatically added to the current partition.
				 * In any case, the reference of the species to the own block is updated   
				 */
				block.getBSTForVectors().put(new VectorOfBigDecimalsForEachLabel(labelOfReaction), paramAsSpecies, partitionOfParams);
			//}
		}
		
		//Now I have to update the splitters according to the newly generated partition
		boolean hasOnlyUnaryReactions = false;//this has to stay false
		CRNBisimulationsNAry.cleanPartitioAndGetNextSplitterIfNecessary(partitionOfParams, null, null, splittedBlocksOfParams, false,hasOnlyUnaryReactions,false);


		//Initialize the "pr" fields to 0 of all species having reactions with partners label, towards at least a species of blockSPL
		//crn.initializeAllCountersAndBST(splitterGenerators);
		//CRNBisimulationsNAry.initializeAllCounters(splitterGenerators,speciesCounters);
		
		
		
		/*
		List<ILabel> fakeLabels=new ArrayList<>();
		fakeLabels.add(EmptySetLabel.EMPTYSETLABEL);
		SplittersGenerator splittersGenerator = partitionOfParams.getOrCreateSplitterGenerator(fakeLabels);
		partitionOfParams.initializeAllBST();
		IBlock blockSPL = splittersGenerator.getBlockSpl();
		*/
		
		
		/*
		IBlock last = partitionOfParams.getLastBlock();
		IBlock current = partitionOfParams.getFirstBlock();
		while(current!=null) {
			for(ISpecies param : current.get)
			
			if(current==last) {
				current=null;
			}
			else {
				current=current.getNext();
			}
		}
		*/
		
		 
		
	}

}
