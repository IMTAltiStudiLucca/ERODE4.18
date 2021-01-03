package it.imt.erode.partitionrefinement.algorithms;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.auxiliarydatastructures.IPartitionsAndBoolean;
import it.imt.erode.auxiliarydatastructures.Reduction;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.implementations.CRN;
import it.imt.erode.crn.implementations.CRNMassActionReactionCompact;
import it.imt.erode.crn.implementations.FakeSpeciesCompact;
import it.imt.erode.crn.implementations.SpeciesCompact;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.label.ILabel;
import it.imt.erode.crn.label.SimpleLabel;
import it.imt.erode.importing.CRNImporter;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SpeciesCounterHandlerCRNBIsimulationNAry;
import it.imt.erode.partitionrefinement.splittersbstandcounters.VectorOfBigDecimalsForEachLabel;

public class ControlEquivalencesNewCopyOfParamsAsSpeciesInCurriedFg {


	public static IPartitionsAndBoolean computeCoarsestModifyingModelExpandingParams(Reduction red, ICRN crn, IPartition partition,
			boolean verbose, MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator,
			IMessageDialogShower msgDialogShower) {
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,red.toString()+" Reducing: "+crn.getName());
		}
		//I avoid copying the partition because I'll copy it later anyway
		//IPartition obtainedPartition = partition.copy();
		IPartition obtainedPartition = partition;
		
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

//		CRNReducerCommandLine.println(out,bwOut,"");
//		ExportSingleUseOfParams.expandMRMCCTMCWithSingleUseOfParametersModifyingTheModel(crn, out, bwOut);
		
		//long begin = System.currentTimeMillis();
		//Check if I really need this
		//HashMap<String,ISpecies> speciesNameToSpeciesCRN=new HashMap<>(crn.getSpecies().size());
		//I don't need 'idOfSpeciesToSpeciesCRN'. ArrayList.get takes constant time!
//		ISpecies[] idOfSpeciesToSpeciesCRN=new ISpecies[crn.getSpecies().size()];
//		int ids=0;
//		for(ISpecies species:crn.getSpecies()) {
//			//speciesNameToSpeciesCRN.put(species.getName(), species);
//			idOfSpeciesToSpeciesCRN[ids]=species;
//			ids++;
//		}
		//LinkedHashMap<String, Integer> originalParamToOccurrences = new LinkedHashMap<>(crn.getParameters().size());
		//LinkedHashMap<Double, Integer> constantToId = new LinkedHashMap<>();
		//LinkedHashMap<Double, Integer> constantToOccurrences = new LinkedHashMap<>();
		
		
		long begin = System.currentTimeMillis();
		CRNReducerCommandLine.print(out,bwOut,"\tCreating the data structures for partitioning parameters... ");
		//LinkedHashMap<String, BigDecimal> paramToValue = new LinkedHashMap<>(crn.getParameters().size());
		LinkedHashMap<String, ISpecies> paramToParamSpecies = new LinkedHashMap<>(crn.getParameters().size());
		//Can I get rid of fakeSpeciesForParams and iterate only on paramToParamSpecies?? 
		//ArrayList<ISpecies> fakeSpeciesForParams = new ArrayList<>(crn.getParameters().size());
		
		//int offsetOnIdOfParams=crn.getSpecies().size();
		IPartition partitionOfParams = new Partition(crn.getParameters().size());
		{
			int id =0;
			//int id =offsetOnIdOfParams;
			HashMap<Integer, IBlock> occurrenceToBlock = new HashMap<Integer, IBlock>();
			for(String param : crn.getParameters()) {
				int space = param.indexOf(' ');
				String parName=param.substring(0,space).trim();
				//double paramValue = crn.getMath().evaluate(parName);
				//BigDecimal paramValueBD = BigDecimal.valueOf(paramValue);
				//paramToValue.put(parName, paramValueBD);
				//I don't care about the value of the parameter
				//ISpecies paramAsSpecies =  new Species(parName, id, paramValueBD, String.valueOf(paramValue), false);
				//ISpecies paramAsSpecies =  new Species(parName, id, BigDecimal.ZERO, "0", false);
				ISpecies paramAsSpecies =  new FakeSpeciesCompact(parName,id); 
				//fakeSpeciesForParams.add(paramAsSpecies);
				paramToParamSpecies.put(parName, paramAsSpecies);
				id++;
				//c_constant_occoccurrence
				//c_0_occ1
				int firstUnder = parName.indexOf('_');
				int secondUnder = parName.indexOf('_',firstUnder+1);
				String constantStr =  parName.substring(firstUnder+1,secondUnder);
				Integer constant =  Integer.valueOf(constantStr);
				IBlock block = occurrenceToBlock.get(constant);
				if(block==null) {
					block=new Block();
					partitionOfParams.add(block);
					occurrenceToBlock.put(constant, block);
				}
				block.addSpecies(paramAsSpecies);
			}
		}
		
		CRNReducerCommandLine.print(out,bwOut,"("+paramToParamSpecies.size()+ " parameters pre-partitioned in "+partitionOfParams.size()+" blocks)... ");
		//We are restricting to the case in which each uncertain parameter is the rate of one reaction only.
		/*
		LinkedHashMap<String, ICRNReaction> paramToReactionOff = new LinkedHashMap<>(crn.getParameters().size());
		for(ICRNReaction reaction : crn.getReactions()) {
			String rateExpr = reaction.getRateExpression();
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
//			String rateExpr = reaction.getRateExpression();
//			
//			
//			BigDecimal value = paramToValue.get(rateExpr);
//			if(value!=null) {
//				//This reaction has an uncertain parameter as rate
//				ICRNReaction old = paramToReactionOff.get(rateExpr);
//				if(old!=null) {
//					//ERROR! The rate appears in two reactions
//					CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it the uncertain parameter "+rateExpr+" appears in more than one reaction. I terminate.");
//					return new IPartitionsAndBoolean(obtainedPartition, false,null);
//				}
//				else {
//					if(reaction.getProducts().computeArity()!=1) {
//						//ERROR! The rate appears in reaction which is not a transition of an LTS
//						CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because the uncertain parameter "+rateExpr+" appears in reaction which does not have exactly one product. I terminate.");
//						return new IPartitionsAndBoolean(obtainedPartition, false,null);
//					}
//					paramToReactionOff.put(rateExpr, reaction);
//				}
//
//			}
//			else {
//				//The rate was not a parameter. If it is a constant I can ignore it, otherwise the model is not supported.
//				try {
//					Double constant = Double.valueOf(rateExpr);
//					if(constant==null) {
//						CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because reactions must have only either constants of parameters as rates: "+rateExpr+". I terminate.");
//						return new IPartitionsAndBoolean(obtainedPartition, false,null);
//					}
//				}
//					catch(NumberFormatException e) {
//						CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because reactions must have only either constants of parameters as rates: "+rateExpr+". I terminate.");
//						return new IPartitionsAndBoolean(obtainedPartition, false,null);
//					}
//				}
			}
		*/
		
		//paramToValue=null;
		
		long end = System.currentTimeMillis();
		String timeString = String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) );
		CRNReducerCommandLine.println(out,bwOut,"completed. Time necessary: "+timeString+ " (s).");

		obtainedPartition.prepareForMoreSpecies(paramToParamSpecies.size());
		
		begin = System.currentTimeMillis();
		CRNReducerCommandLine.print(out,bwOut,"\tComputing the coarsest forward verification equivalence... ");
		CRNReducerCommandLine.print(out,bwOut,"\n\t\tInitially: "+obtainedPartition.size()+" blocks of species, and "+partitionOfParams.size()+" of params.");
		int sizeOfPrevPartition = obtainedPartition.size();
		int sizeOfPrevParamPartition = partitionOfParams.size();
		int iteration=1;
		do {
			sizeOfPrevPartition = obtainedPartition.size();
			sizeOfPrevParamPartition = partitionOfParams.size();
			
			//Step 1: compute coarsest FDE on curried-f[G]
			//In one go I:
			//	Step 1.1 I have to apply the partition of parameters to the CRN (f[G])
			//	Step 1.2 I apply the currying on f[G] 
			ICRN curriedfG;
			//HashMap<String,ISpecies> speciesNameToSpeciesOfCurriedfG = new HashMap<>(crn.getSpecies().size()+partitionOfParams.size());
			//I don't need this array. ArrayList.get takes constant time!
			//ISpecies[] idToSpeciesCurriedFG=new ISpecies[crn.getSpecies().size()+partitionOfParams.size()];
			HashMap<ISpecies, ISpecies> representativeOfBlockOfPartitionOfParamsToSpeciesFG=null;
			try {
				//TODO Potrei creare una volta sola curriedFG con tutte le specie di CRN. E ogni volta aggiungo e poi levo le curried species (serve che le levo, o i parametri aggiunti rimarranno???
				//	devo buttare via tutte le reazioni
				curriedfG = new CRN(crn.getName(),crn.getMath(),out,bwOut);
				curriedfG.setExpectedNumberOfSpecies(crn.getSpecies().size()+partitionOfParams.size());
				curriedfG.setExpectedNumberOfReactions(crn.getReactions().size());
				for (ISpecies species : crn.getSpecies()) {
					/*
					ISpecies newSpecies = species.cloneWithoutReactions();
					curriedfG.addSpecies(newSpecies);
					*/
					curriedfG.addSpecies(species);
					//idToSpeciesCurriedFG[species.getID()]=newSpecies;
					//speciesNameToSpeciesOfCurriedfG.put(newSpecies.getName(), newSpecies);
				}
					//copy reactions, and at the same time replace parameters with representative.
					//apply currying
				representativeOfBlockOfPartitionOfParamsToSpeciesFG=addOneSpeciesPerRepresentativeParam(curriedfG, /*idToSpeciesCurriedFG,*/ partitionOfParams);
				copyReactionsReplacingParamsWithRepresentativeAndCurrying(representativeOfBlockOfPartitionOfParamsToSpeciesFG,crn, curriedfG, /*idToSpeciesCurriedFG,*/ /*speciesNameToSpeciesOfCurriedfG,*/paramToParamSpecies,/*paramToValue,*/partitionOfParams);
			} catch (IOException e1) {
				CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because I had problems in computing f[G] (step: copying crn): "+e1.getMessage()+". I terminate.");
				return new IPartitionsAndBoolean(obtainedPartition, false,null);
			} catch (UnsupportedFormatException e) {
				CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because I had problems in computing f[G] (step: replacing parameter with representative): "+e.getMessage()+". I terminate.");
				return new IPartitionsAndBoolean(obtainedPartition, false,null);
			}
			
			//Step 1.3 Now I have to create the partition corresponding to 'obtainedPartition' for curriedFG
			//1.3.1 Copy each block replacing the original species with the new one
			//IPartition obtainedPartitionCurriedfG = copyPartitionUsingCopiedCRN(obtainedPartition,speciesNameToSpeciesOfCurriedfG, curriedfG);
			//IPartition obtainedPartitionCurriedfG = copyPartitionUsingCopiedCRN(obtainedPartition,/*idToSpeciesCurriedFG,*/ curriedfG);
			//Here I don't need to properly copy the partition. I just get the blocks
			//	It does not work because in this way I don't update the array idToBlock of the partition.
			//IPartition obtainedPartitionCurriedfG = getBlocksOfSpeciesPresent(obtainedPartition, curriedfG.getSpecies().size());
			IPartition obtainedPartitionCurriedfG = obtainedPartition;
			//1.3.2 Create a new singleton block for each added parameter/species (each representing an equivalence class of the parameters
			IBlock current = partitionOfParams.getFirstBlock();
			while(current!=null) {
				ISpecies repParam = current.getRepresentative();
				//ISpecies speciesfG = speciesNameToSpeciesOfCurriedfG.get(repParam.getName());
				ISpecies speciesfG = representativeOfBlockOfPartitionOfParamsToSpeciesFG.get(repParam); //wrong: idToSpeciesCurriedFG[repParam.getID()+crn.getSpecies().size()];
				IBlock currentfG = new Block();
				obtainedPartitionCurriedfG.add(currentfG);
				currentfG.addSpecies(speciesfG);
				current=current.getNext();
			}
			
			// Step 1.4 Now I computed the coarsest FE for curried-f[G] 
			//HashMap<IComposite, BigDecimal> multisetCoefficients = CRNBisimulationsNAry.computeMultisetCoefficients(curriedfG, terminator, curriedfG.getMaxArity());
			CRNReducerCommandLine.print(out,bwOut,"\n\t\tComputing FE on the current fG ("+curriedfG.getReactions().size()+" reactions and "+curriedfG.getSpecies().size()+" species)");
			long beginfe=System.currentTimeMillis();
			//Meglio fare un hashmap di speciesCounters. Spesso sono pochi gli splitterGenerators. Invece qui creiamo un array di species+reactions
			//	Poi occupiamo un sacco di memoria per creare le reazioni che entrano nelle specie.
			SpeciesCounterHandlerCRNBIsimulationNAry	 speciesCounters[] = new SpeciesCounterHandlerCRNBIsimulationNAry	[curriedfG.getSpecies().size()];
			CRNBisimulationsNAry.refine(Reduction.FE, curriedfG, obtainedPartitionCurriedfG/*, multisetCoefficients*/, speciesCounters, null,terminator,out,bwOut,true);
			long endfe=System.currentTimeMillis();
			CRNReducerCommandLine.println(out,bwOut,"\n\t\t\tComputation of FE completed in: "+String.format( CRNReducerCommandLine.MSFORMAT, ((endfe-beginfe)/1000.0) )+ " (s).");
			
			//Step 1.5 Now I have to update 'obtainedPartition' for the original f according to the partition 'obtainedPartitionCurriedfG' computed for curried-f[G]
			//obtainedPartition = copyPartitionUsingCopiedCRN(obtainedPartitionCurriedfG,speciesNameToSpeciesCRN, crn);
			CRNReducerCommandLine.println(out,bwOut,"\t\tUpdating the partition of the original model using the computed FE on fG");
			//Here I don't need to properly copy the partition. I just get the blocks that do not contain parameters 
			//obtainedPartition = copyPartitionUsingCopiedCRN(obtainedPartitionCurriedfG, crn);
			//obtainedPartition = getBlocksOfSpeciesPresent(obtainedPartitionCurriedfG, crn.getSpecies().size());
			maintainBlocksOfSpeciesPresent(obtainedPartitionCurriedfG, crn.getSpecies().size());
			obtainedPartition = obtainedPartitionCurriedfG;
			
			//Step 2: I have to refine the partition of parameters of f using the computed FDE for curried-f[G]
			//Now, using the original system f, I have to refine the partition of parameters according to remark 1
			//The original reactions of interest are stored in paramToReaction
			CRNReducerCommandLine.println(out,bwOut,"\t\tRefining the partition of parameters");
			refinePartitionOfParameters(crn,obtainedPartitionCurriedfG,/*fakeSpeciesForParams,*/partitionOfParams,paramToParamSpecies);
			CRNReducerCommandLine.println(out,bwOut,"\t\tAfter iteration "+iteration+": "+obtainedPartition.size()+" blocks of species, and "+partitionOfParams.size()+" of params.");
			iteration++;
		}while(	sizeOfPrevPartition      != obtainedPartition.size() ||
				sizeOfPrevParamPartition != partitionOfParams.size()    );
		
		//CRNReducerCommandLine.print(out,bwOut,"\n\tPartitioning completed (parameters partitioned in "+ partitionOfParams.size()+" blocks)... ");
		//CRNReducerCommandLine.print(out,bwOut,"\n\tPartitioning");
		
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
	
//	public static IPartition copyPartitionUsingCopiedCRN(IPartition obtainedPartition,
//			HashMap<String, ISpecies> speciesNameToSpeciesOfCopyCRN, ICRN copyCRN) {
//		IPartition obtainedPartitionCopiedCRN = new Partition(copyCRN.getSpecies().size()); 
//		IBlock current = obtainedPartition.getFirstBlock();
//		while(current!=null) {
//			IBlock currentCopiedBlock = new Block();
//			obtainedPartitionCopiedCRN.add(currentCopiedBlock);
//			for (ISpecies species : current.getSpecies()) {
//				ISpecies speciesOfCopiedCRN = speciesNameToSpeciesOfCopyCRN.get(species.getName());
//				if(speciesOfCopiedCRN!=null) {
//					currentCopiedBlock.addSpecies(speciesOfCopiedCRN);
//				}
//			}
//			if(currentCopiedBlock.getSpecies().isEmpty()) {
//				obtainedPartitionCopiedCRN.remove(currentCopiedBlock,false);
//			}
//			current=current.getNext();
//		}
//		return obtainedPartitionCopiedCRN;
//	}
	
	public static IPartition copyPartitionUsingCopiedCRN(IPartition obtainedPartition,
			/*ISpecies[] idOfspeciesToSpeciesOfCopyCRN,*/ ICRN copyCRN) {
		IPartition obtainedPartitionCopiedCRN = new Partition(copyCRN.getSpecies().size()); 
		IBlock current = obtainedPartition.getFirstBlock();
		while(current!=null) {
			IBlock currentCopiedBlock = new Block();
			obtainedPartitionCopiedCRN.add(currentCopiedBlock);
			for (ISpecies species : current.getSpecies()) {
				//ISpecies speciesOfCopiedCRN = speciesNameToSpeciesOfCopyCRN.get(species.getName());
				if(species.getID()<copyCRN.getSpecies().size()) {//I discard extra/additional that might be present in obtainedPartition (e.., the parameters transformed in species.
				//if(species.getID()<idOfspeciesToSpeciesOfCopyCRN.length) {//I discard extra/additional that might be present in obtainedPartition (e.., the parameters transformed in species. 
					//ISpecies speciesOfCopiedCRN = idOfspeciesToSpeciesOfCopyCRN[species.getID()]; //speciesNameToSpeciesOfCopyCRN.get(species.getName());
					ISpecies speciesOfCopiedCRN = copyCRN.getSpecies().get(species.getID());
					if(speciesOfCopiedCRN!=null) {
						currentCopiedBlock.addSpecies(speciesOfCopiedCRN);
					}
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
	public static IPartition getBlocksOfSpeciesPresent(IPartition obtainedPartition,int numberOfSpecies) {
		IPartition obtainedPartitionCopiedCRN = new Partition(numberOfSpecies); 
		IBlock current = obtainedPartition.getFirstBlock();
		while(current!=null) {
			//IBlock currentCopiedBlock = new Block();
			if(current.getRepresentative().getID()<numberOfSpecies) {
				obtainedPartitionCopiedCRN.add(current);
			}
			current=current.getNext();
			//TODO: quando raggiungo l'ultimo blocco devo mettergli next=null
		}
		return obtainedPartitionCopiedCRN;
	}
	*/
	
	public static void maintainBlocksOfSpeciesPresent(IPartition obtainedPartition,int numberOfSpecies) { 
		IBlock current = obtainedPartition.getFirstBlock();
		while(current!=null) {
			//IBlock currentCopiedBlock = new Block();
			IBlock next=current.getNext();
			if(current.getRepresentative().getID()<numberOfSpecies) {
				//obtainedPartitionCopiedCRN.add(current);
				//Do nothing, I want to keep it
			}
			else {
				obtainedPartition.remove(current,false);
			}
			current=next;
		}
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
	
	public static HashMap<ISpecies, ISpecies> addOneSpeciesPerRepresentativeParam(ICRN crnNew, /*ISpecies[] idToSpeciesCurriedFG,*/ IPartition partitionOfParams) throws IOException, UnsupportedFormatException {
		int id=crnNew.getSpecies().size();
		//Transform the parameters to be perturbed in species
		
		//I add one species per representative param
		IBlock current = partitionOfParams.getFirstBlock();
		//TODO questa hashmap non mi serve piu'. Riusco direttamente la stessa specieasparam
		HashMap<ISpecies, ISpecies> representativeOfBlockOfPartitionOfParamsToSpeciesFG=new HashMap<>(partitionOfParams.size());
		while(current!=null) {
			ISpecies paramAsSpecies = current.getRepresentative();
			//String p=paramAsSpecies.getName();
			//double val = crn.getMath().evaluate(p);
			//ISpecies sp =new Species(p, id, BigDecimal.valueOf(val), String.valueOf(val),false);
			//I don't care about the IC
			//TODO Non ho bisogno di creare una nuova species. Posso usare paramAsSpecies. Ma devo fare partire gli id di paramaspecies da getCRN().getSpecies().size()
																//Attento, partition usa l'id delle specie per prendere il blocco a cui appartengono.
			ISpecies sp =new SpeciesCompact(id, BigDecimal.ZERO);
			crnNew.addSpecies(sp);
			//speciesNameToSpeciesOfCurriedfG.put(p, sp);
			representativeOfBlockOfPartitionOfParamsToSpeciesFG.put(paramAsSpecies, sp);
			//idToSpeciesCurriedFG[id]=sp;
			id++;
			current=current.getNext();
		}
		return representativeOfBlockOfPartitionOfParamsToSpeciesFG;
	}
	
	public static void copyReactionsReplacingParamsWithRepresentativeAndCurrying(HashMap<ISpecies, ISpecies> representativeOfBlockOfPartitionOfParamsToSpeciesFG,ICRN crn, ICRN crnNew, /*ISpecies[] idToSpeciesCurriedFG,*/ /*HashMap<String,ISpecies> speciesNameToSpeciesOfCurriedfG,*/ LinkedHashMap<String, ISpecies> paramToParamSpecies, IPartition partitionOfParams) throws IOException, UnsupportedFormatException {
		//TODO Qui potrei fare il currying lasciando i parametri normali. E poi di volta in volta considero il representativo dei paramAsSpecies
		
		//I have to copy each reaction, after replacing the param with its representative and currying it 
		for (ICRNReaction reaction : crn.getReactions()) {
			String r = reaction.getRateExpression();
			ISpecies paramToSp = paramToParamSpecies.get(r);
			BigDecimal rateWithoutParametersToPerturbAndWithEvaluatedParameters;
			if(paramToSp!=null) {
				ISpecies repParamToSp = partitionOfParams.getBlockOf(paramToSp).getRepresentative();
				//paramToSp=speciesNameToSpeciesOfCurriedfG.get(repParamToSp.getName());
				paramToSp= representativeOfBlockOfPartitionOfParamsToSpeciesFG.get(repParamToSp);
				rateWithoutParametersToPerturbAndWithEvaluatedParameters=BigDecimal.ONE;
			}
			else {
				//The reaction had a constant rate
				rateWithoutParametersToPerturbAndWithEvaluatedParameters=reaction.getRate();
			}
			
			IComposite oldReagents =reaction.getReagents();
			HashMap<ISpecies, Integer> reagentsHM = new HashMap<>(oldReagents.getNumberOfDifferentSpecies()+1);
			for(int i=0;i<oldReagents.getNumberOfDifferentSpecies();i++) {
				//ISpecies newSpecies = idToSpeciesCurriedFG[oldReagents.getAllSpecies(i).getID()];
				ISpecies newSpecies = crnNew.getSpecies().get(oldReagents.getAllSpecies(i).getID());
				reagentsHM.put(newSpecies, oldReagents.getMultiplicities(i));
			}
			if(paramToSp!=null) {
				reagentsHM.put(paramToSp, 1);
			}
			
			IComposite newReagents = CRN.compositeOrSpecies(reagentsHM);

			IComposite oldProducts =reaction.getProducts();
			HashMap<ISpecies, Integer> productsHM = new HashMap<>(oldProducts.getNumberOfDifferentSpecies()+1);
			for(int i=0;i<oldProducts.getNumberOfDifferentSpecies();i++) {
				//ISpecies newSpecies = idToSpeciesCurriedFG[oldProducts.getAllSpecies(i).getID()];
				ISpecies newSpecies = crnNew.getSpecies().get(oldProducts.getAllSpecies(i).getID());
				productsHM.put(newSpecies, oldProducts.getMultiplicities(i));
			}
			if(paramToSp!=null) {
				productsHM.put(paramToSp, 1);
			}
			IComposite newProducts = CRN.compositeOrSpecies(productsHM);
			
			if(reaction.hasArbitraryKinetics()) {
				//arbitrary
				throw new UnsupportedFormatException("I found a reaction with arbitrary kinetics: "+reaction.toString());
			}
			//mass action
			//ICRNReaction newReaction = new CRNReaction(rateWithoutParametersToPerturbAndWithEvaluatedParameters, newReagents, newProducts, "1",reaction.getID());
			ICRNReaction newReaction = new CRNMassActionReactionCompact(rateWithoutParametersToPerturbAndWithEvaluatedParameters, newReagents, newProducts);
			//add flags to disable addincoming and addoutgoing from species
			CRNImporter.addReaction(crnNew, newReagents, newProducts, newReaction);
		}
		
	}

	/*
	private static LinkedHashSet<String> computeSetOfRepresentativeParams(IPartition partitionOfParams) {
		LinkedHashSet<String> representativeParams = new LinkedHashSet<>(partitionOfParams.size());
		IBlock currentBlockOfParams = partitionOfParams.getFirstBlock();
		while(currentBlockOfParams!=null) {
			representativeParams.add(currentBlockOfParams.getRepresentative().getName());
			currentBlockOfParams=currentBlockOfParams.getNext();
		}
		return representativeParams;
	}
	*/

	private static void refinePartitionOfParameters(
			ICRN crn, IPartition partitionOfSpecies, 
			/*Collection<ISpecies> fakeSpeciesForParams,*/ IPartition partitionOfParams, LinkedHashMap<String, ISpecies> paramToParamSpecies) {
		//I have to split each block of partitionOfParams. 
		//	For each rate in the block, I take its reaction (there must be exactly one), and I compute the id of the blocks of its reagent and of its product. I split according to these ids.
		//		The id is just the id of the representative species of the block
		//E' un partitioning semplice. Non mi serve considerare splitter generators.
		
		HashSet<IBlock> splittedBlocksOfParams = new LinkedHashSet<>(partitionOfParams.size());

		//I assume that every paramAsSpecies appears exactly in a reaction. And each reaction has a paramAsSpecies as parameter. This works for MRMC models
		for(ICRNReaction reactionOfTheRate : crn.getReactions()) {
			ISpecies paramAsSpecies = paramToParamSpecies.get(reactionOfTheRate.getRateExpression());
		//for (ISpecies paramAsSpecies : fakeSpeciesForParams) {
			IBlock block = partitionOfParams.getBlockOf(paramAsSpecies);
			//I can't do this check, becuase if you have p1 and p2 in a block, as soon as you remove p1, p2 believes to be in a singleton block. Instead, p2 should be handled as well as p1, because p2 might end up in the same subblock of p1. 
			/*if(block.getSpecies().size()==1) {
				continue;
			}
			else {*/
				splittedBlocksOfParams.add(block);
				//ICRNReaction reactionOfTheRate = paramToReaction.get(paramAsSpecies.getName());
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
