package it.imt.erode.commandline;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.eclipse.ui.console.MessageConsoleStream;

import com.microsoft.z3.Z3Exception;

import it.imt.erode.auxiliarydatastructures.IPartitionAndBoolean;
import it.imt.erode.auxiliarydatastructures.Reduction;
import it.imt.erode.crn.implementations.InfoCRNReduction;
import it.imt.erode.crn.implementations.InfoReduction;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.expression.evaluator.MathEval;
import it.imt.erode.importing.MatlabODEsImporter;
import it.imt.erode.importing.ODEorNET;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.CRNBisimulationsNAry;
import it.imt.erode.simulation.output.MutableBoolean;
import it.imt.erode.utopic.MatlabODEPontryaginExporter;

public class ProbProgCommandLine extends AbstractCommandLine {

	private ICRN crn,polynomialCRN;
	private ArrayList<ArrayList<ICRNReaction>> reactionsOfEachIf_Guard;
	private ArrayList<ArrayList<ICRNReaction>> reactionsOfEachIf_Dynamics;
	
	private ArrayList<ArrayList<ICRNReaction>> curriedReactionsOfEachIf_Guard;
	private ArrayList<ArrayList<ICRNReaction>> curriedReactionsOfEachIf_Dynamics;
	private ArrayList<ArrayList<ICRNReaction>> RNreactionsOfEachIf_Guard;
	private ArrayList<ArrayList<ICRNReaction>> RNreactionsOfEachIf_Dynamics;
	
	
	private boolean RNificationFailed=false;
	private ArrayList<String> probProgParameters;
	
	private IPartition partitionPolynomialCRN;
	
	public ProbProgCommandLine(CommandsReader commandsReader, ICRN crn, IPartition partition, boolean fromGUI,
			ArrayList<String> probProgParameters, ArrayList<ArrayList<ICRNReaction>> reactionsOfEachGuard,
			ArrayList<ArrayList<ICRNReaction>> reactionsOfEachClause,
			MessageConsoleStream out, BufferedWriter bwOut) {
		super(commandsReader, fromGUI);
		this.crn=crn;
		this.reactionsOfEachIf_Guard=reactionsOfEachGuard;
		this.reactionsOfEachIf_Dynamics=reactionsOfEachClause;
		this.probProgParameters=probProgParameters;
		setPartition(partition);
		
		try {
			HashMap<String, ISpecies> speciesNameToSpecies = applyCurrying(out, bwOut);
			makeRNEncoding(out, bwOut,speciesNameToSpecies);
		} catch (UnsupportedFormatException e) {
			RNificationFailed=true;
		}
	}
	
	private HashMap<String,ISpecies> applyCurrying(MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		
		LinkedHashSet<String> paramsToConsider=new LinkedHashSet<String>(probProgParameters);
		boolean oneBlockPerParam=true;
		boolean addParams=false;
		boolean preserveUserPartition=true;
		//CRNandPartition curriedCRNAndPartition = CRNReducerCommandLine.applyCurry(crn, partition, paramsToConsider, oneBlockPerParam, addParams, preserveUserPartition,true);
		
		
		MathEval math = new MathEval();
		HashMap<String,ISpecies> speciesNameToExpandedSpecies = new HashMap<>(crn.getSpecies().size());
		HashMap<String, ISpecies> parameterToCorrespondingSpecies = new HashMap<>(paramsToConsider.size());
		String prefixNewPar = "p_";
		boolean reuseSpecies=true;
		ICRN crnExpanded = MatlabODEsImporter.expandCRN_createCRNAndCurriedSpecies(crn, speciesNameToExpandedSpecies, reuseSpecies, math,addParams,parameterToCorrespondingSpecies,prefixNewPar,paramsToConsider);
		
		
		boolean simpleRateParamOrConstant=false;
		
		curriedReactionsOfEachIf_Guard = new ArrayList<>(reactionsOfEachIf_Guard.size());
		for(ArrayList<ICRNReaction> reactions : reactionsOfEachIf_Guard) {
			ArrayList<ICRNReaction> curriedReactions=MatlabODEsImporter.curryTheReactions(addParams, speciesNameToExpandedSpecies, simpleRateParamOrConstant, math,parameterToCorrespondingSpecies, prefixNewPar, null, reactions);
			curriedReactionsOfEachIf_Guard.add(curriedReactions);
		}
		curriedReactionsOfEachIf_Dynamics = new ArrayList<>(reactionsOfEachIf_Dynamics.size());
		for(ArrayList<ICRNReaction> reactions : reactionsOfEachIf_Dynamics) {
			ArrayList<ICRNReaction> curriedReactions=MatlabODEsImporter.curryTheReactions(addParams, speciesNameToExpandedSpecies, simpleRateParamOrConstant, math,parameterToCorrespondingSpecies, prefixNewPar, null, reactions);
			curriedReactionsOfEachIf_Dynamics.add(curriedReactions);
		}
		
		IPartition partitionCurried = CRNReducerCommandLine.handlePartitionsAfterCurrying(crn, partition, paramsToConsider,
				oneBlockPerParam, preserveUserPartition, speciesNameToExpandedSpecies, crnExpanded);
		
		
		
		
		
		
		
		crnExpanded.setMdelDefKind(ODEorNET.RN);
		polynomialCRN=crnExpanded;
		partitionPolynomialCRN=partitionCurried;
		
		return speciesNameToExpandedSpecies;
		
		
//		
//		
//		
//		polynomialCRN = new CRN(crn.getName(), crn.getSymbolicParameters(), crn.getConstraints(), crn.getParameters(), crn.getMath(), out, bwOut);
//		HashMap<String, ISpecies> speciesNameToSpecies=new HashMap<String, ISpecies>(crn.getSpeciesSize());
//		for(ISpecies sp : crn.getSpecies()) {
//			speciesNameToSpecies.put(sp.getName(),sp);
//			polynomialCRN.addSpecies(sp);
//		}
//		
//		HashMap<String, ISpecies> paramToSpecies = new HashMap<>(probProgParameters.size());
//		for(String param : probProgParameters) {
//			ISpecies sp_p = new Species(param, null, crn.getSpecies().size(), BigDecimal.ZERO, "0",false);
//			paramToSpecies.put(param, sp_p);
//			speciesNameToSpecies.put(param, sp_p);
//			polynomialCRN.addSpecies(sp_p);
//		}
		
	}

	private boolean makeRNEncoding(MessageConsoleStream out, BufferedWriter bwOut, HashMap<String, ISpecies> speciesNameToSpecies) {
		RNificationFailed=true;
		
		ISpecies I=MatlabODEPontryaginExporter.makeISpeciesWithoutAddingIt(polynomialCRN);
		
		MutableBoolean usedI=new MutableBoolean();
		usedI.setValue(false);
		
		MutableBoolean mutableComputeRNEncoding=new MutableBoolean();
		boolean computeRNEncoding=true;
		mutableComputeRNEncoding.setValue(computeRNEncoding);
		
		MathEval math = crn.getMath();
		
		String errorMessage="";
		RNreactionsOfEachIf_Guard = new ArrayList<>(reactionsOfEachIf_Guard.size());
		for(ArrayList<ICRNReaction> reactionsOfCurrentGuard : reactionsOfEachIf_Guard) {
			ArrayList<ICRNReaction> rnReactions = new ArrayList<>(reactionsOfCurrentGuard.size());
			RNreactionsOfEachIf_Guard.add(rnReactions);
			errorMessage = MatlabODEPontryaginExporter.RNifyReactions(rnReactions, I, speciesNameToSpecies, usedI,
					mutableComputeRNEncoding, reactionsOfCurrentGuard, math);
			if(!mutableComputeRNEncoding.getValue()) {
				break;
			}
		}
		
		RNreactionsOfEachIf_Dynamics = new ArrayList<>(reactionsOfEachIf_Dynamics.size());
		for(ArrayList<ICRNReaction> reactionsOfCurrentDynamics : reactionsOfEachIf_Dynamics) {
			ArrayList<ICRNReaction> rnReactions = new ArrayList<>(reactionsOfCurrentDynamics.size());
			RNreactionsOfEachIf_Dynamics.add(rnReactions);
			errorMessage = MatlabODEPontryaginExporter.RNifyReactions(rnReactions, I, speciesNameToSpecies, usedI,
					mutableComputeRNEncoding, reactionsOfCurrentDynamics, math);
			if(!mutableComputeRNEncoding.getValue()) {
				break;
			}
		}
		
		if(!mutableComputeRNEncoding.getValue()) {
				//CRNReducerCommandLine.print(out,bwOut, "\n The model cannot be encoded as a mass action reaction network.\n I ignore this command");
				CRNReducerCommandLine.println(out,bwOut, "). ");
				CRNReducerCommandLine.print(out,bwOut, "\tThe model cannot be encoded as a mass action reaction network:\n  "+errorMessage);
				return false;
		}
		else {
			if(usedI.getValue()) {
				polynomialCRN.addSpecies(I);
			}
			
			partitionPolynomialCRN = MatlabODEPontryaginExporter.handlePartitionAfterRNificaton(partitionPolynomialCRN, polynomialCRN.getUserDefinedPartition(), I, polynomialCRN, usedI);
		}
		
		RNificationFailed=false;
		return true;
	}

	@Override
	public void executeCommands(boolean print, MessageConsoleStream out, BufferedWriter bwOut)
			throws IOException, InterruptedException, UnsupportedFormatException, Z3Exception {

		boolean updateCRN=false;
		String command = null;

		//try{

		while(!Terminator.hasToTerminate(terminator)){
			command = commandsReader.getNextCommand();

			if(command.equalsIgnoreCase("newline")){
				CRNReducerCommandLine.println(out,bwOut,"");
			}
			else if(command.startsWith("reduceFPE(")){
				handleReduceCommand(command,updateCRN,"fpe",out,bwOut);
			}
			else if(command.equals("")){

			}
			else if(command.equals("NONINTERACTIVE")){
				//System.exit(0);
				return;
			}
			//}

		}
	}

	private IPartitionAndBoolean handleReduceCommand(String command, boolean updateCRN, String reduction, MessageConsoleStream out,
			BufferedWriter bwOut) throws UnsupportedFormatException, IOException {
		
		String partitionInfoFileName=null;
		String computeOnlyPartition=null;
		String prePartitionUserDefined = "false";
		boolean print=true;
		String csvFile=null;
		
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return null;
		}
		
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].equals("")){
				continue;
			}
			else if(parameters[p].startsWith("fileWhereToStorePartition=>")){
				if(parameters[p].length()<="fileWhereToStorePartition=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write information about the computed partition.");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				partitionInfoFileName = parameters[p].substring("fileWhereToStorePartition=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("computeOnlyPartition=>")){
				if(parameters[p].length()<="computeOnlyPartition=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if or not only the partition has to be computed (without thus reducing the model). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				computeOnlyPartition = parameters[p].substring("computeOnlyPartition=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("prePartition=>")){
				if(parameters[p].length()<="prePartition=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if you want to prepartion the species of the model according to their initial conditions or to the views. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				String prep = parameters[p].substring("prePartition=>".length(), parameters[p].length()).trim();
				if(prep.compareToIgnoreCase("USER")==0){
					prePartitionUserDefined = "true";
				}
				else{
					CRNReducerCommandLine.println(out,bwOut,"Unknown prepartitioning option \""+prep+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
					return null;
				}
			}
			else if(parameters[p].startsWith("csvFile=>")){
				if(parameters[p].length()<="csvFile=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write the simulation data. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				csvFile = parameters[p].substring("csvFile=>".length(), parameters[p].length());
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return null;
			}
		}
		
		
		if(crn==null || reactionsOfEachIf_Dynamics==null || reactionsOfEachIf_Guard==null){
			CRNReducerCommandLine.println(out,bwOut,"Before reducing a model it is necessary to load it. "); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return null;
		}
		if(RNificationFailed || polynomialCRN==null || RNreactionsOfEachIf_Dynamics==null || RNreactionsOfEachIf_Guard==null){
			CRNReducerCommandLine.println(out,bwOut,"Before reducing a model it is necessary to RNify it. "); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return null;
		}

		if(reduction ==null || reduction.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the reduction technique. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return null;
		}
		
		ICRN crnToConsider = polynomialCRN;
		IPartition initial = partitionPolynomialCRN;
		ArrayList<HashSet<ISpecies>> userDefinedPartition = crnToConsider.getUserDefinedPartition();
		
		//String originalCRNShort="Original model: "+crnToConsider.toStringShort();
		//String reducedCRNShort;
		//CRNandPartition cp=null;
		
		if(prePartitionUserDefined!=null && prePartitionUserDefined.equalsIgnoreCase("true")){
			initial = CRNReducerCommandLine.pepartitionAccordingToUserPartition(out, bwOut, print, crnToConsider,
				initial, userDefinedPartition, terminator);
		}
		
		String reductionName = reduction.toUpperCase();
		
		String pre = "";
		/*if(reductionName.equals("FB")||reductionName.equals("BB")){
			pre=" ";
		}*/
		if(print){
			if(crnToConsider.getName()!=null){
				CRNReducerCommandLine.print(out,bwOut,pre+reductionName+" partitioning "+crnToConsider.getName()+"...");
			}
			else{
				CRNReducerCommandLine.print(out,bwOut,pre+reductionName+" partitioning...");//CRNReducerCommandLine.print(out,bwOut,pre+reductionName+" partitioning the current model ...");
			}
		}
		
		long begin = System.currentTimeMillis();
		
		LinkedHashMap<String, String> extraColumnsForCSV=new LinkedHashMap<>(3);
		
		IPartitionAndBoolean obtainedPartitionAndBool=new IPartitionAndBoolean(initial, true);
		int prevPartSize=initial.size();
		boolean succeeded=false;
		
		do {
			prevPartSize=obtainedPartitionAndBool.getPartition().size();
			for(ArrayList<ICRNReaction> reactionsOfCurrentGuard : RNreactionsOfEachIf_Guard) {
				obtainedPartitionAndBool = CRNBisimulationsNAry.computeCoarsest(Reduction.FE,
						crnToConsider,reactionsOfCurrentGuard, obtainedPartitionAndBool.getPartition(), 
						CRNReducerCommandLine.verbose,out,bwOut,terminator,messageDialogShower);
				succeeded=obtainedPartitionAndBool.getBool();
				if(!succeeded) {
					break;
				}
			}
			if(succeeded) {
				for(ArrayList<ICRNReaction> reactionsOfCurrentDynamics : RNreactionsOfEachIf_Dynamics) {
					obtainedPartitionAndBool = CRNBisimulationsNAry.computeCoarsest(Reduction.FE,
							crnToConsider,reactionsOfCurrentDynamics, obtainedPartitionAndBool.getPartition(), 
							CRNReducerCommandLine.verbose,out,bwOut,terminator,messageDialogShower);
					succeeded=obtainedPartitionAndBool.getBool();
					if(!succeeded) {
						break;
					}
				}
			}
		}while(succeeded && prevPartSize!=obtainedPartitionAndBool.getPartition().size());
		
		long end = System.currentTimeMillis();
		
		IPartition obtainedPartition=obtainedPartitionAndBool.getPartition();
		
		if(succeeded && CRNReducerCommandLine.SETREPRESENTATIVEBYMINAFTERPARTITIONREFINEMENT){
			obtainedPartition.setMinAsRepresentative();
		}
		if(CRNReducerCommandLine.printPartition){
			CRNReducerCommandLine.println(out,bwOut,obtainedPartition);
		}
		
//		if(succeeded && !Terminator.hasToTerminate(terminator)){
//			CRNReducerCommandLine.printReductionInfoAndComputeReducedModel(updateCRN, reduction, out, bwOut, null,
//					partitionInfoFileName, null, null, null, null,
//					computeOnlyPartition, csvFile, print, true, false, false,
//					false, crnToConsider, initial, originalCRNShort, obtainedPartition, cp, null,
//					reductionName, null, null, null, begin, null, end,null,null,null);
//		}

		if(succeeded) {
			double factor=printReductionInfo(out, bwOut, computeOnlyPartition, print, crnToConsider, begin, end, obtainedPartition);

			CRNReducerCommandLine.writePartitionToFile(reduction, out, bwOut, partitionInfoFileName, print, crnToConsider, obtainedPartition,null);

			InfoCRNReduction infoReduction = new InfoCRNReduction(crnToConsider.getName(), crnToConsider.getSpecies().size(), crnToConsider.getReactions().size(), crnToConsider.getParameters().size(),factor, initial==null?-1:initial.size(),obtainedPartition, end-begin, reduction);

			//here goes the computation of the reduced model

			int addedVars=polynomialCRN.getSpeciesSize()-crn.getSpeciesSize() +1;
			int actualInitPartition=partitionPolynomialCRN.size()- addedVars;
			int actualVars = crn.getSpeciesSize()-1;
			int actualRedVars=obtainedPartition.size()-addedVars;
			String perc = InfoReduction.percRedSizeOverOrigSize(actualRedVars/(double)actualVars); 
			
			extraColumnsForCSV.put("addedSingletonVars",""+addedVars);
			extraColumnsForCSV.put("addedInitPartition",""+actualInitPartition);
			extraColumnsForCSV.put("actualVars",""+actualVars);
			extraColumnsForCSV.put("actualRedVars",""+actualRedVars);
			extraColumnsForCSV.put("actualRedSp/OrigSp",perc);
			
			writeReductionInfoInCSVFile(out, bwOut, csvFile, infoReduction,extraColumnsForCSV);
		}
		else {
			CRNReducerCommandLine.print(out,bwOut,pre+reductionName+" FAILED!!!");
		}
		return new IPartitionAndBoolean(obtainedPartition, succeeded);
	}

	public double printReductionInfo(MessageConsoleStream out, BufferedWriter bwOut, String computeOnlyPartition,
			boolean print, ICRN crnToConsider, long begin, long end, IPartition obtainedPartition) {
		int original = crnToConsider.getSpecies().size();
		int reduced = obtainedPartition.size();
		double factor = (reduced)/((double)original);
		if(print){
			double time = (end-begin)/1000.0;
			String timeString = String.format( CRNReducerCommandLine.MSFORMAT, (time) );
			String msg="\n\tCompleted in "+timeString+ " (s).";
//			if(smtTime!=null){
//				msg+="\n"+smtTime;
//			}
			if(factor== 1.0){
				//CRNReducerCommandLine.println(out,bwOut," completed. No reduction possible. Time necessary: "+timeString+ " (s).");
				//CRNReducerCommandLine.println(out,bwOut," completed in "+timeString+ " (s).\n\tNo reduction possible.");
				msg+="\n\tNo reduction possible.";
				if(!computeOnlyPartition.equalsIgnoreCase("true")){
					computeOnlyPartition="true";
					msg+="\n\tI do not compute the reduced model.";
				}
				//Since there was no reduction, I do not create a reduced model.
				
			}
			else{
				String reductionRatio = String.format( "%.2f", ((factor*100.0)) );
				//CRNReducerCommandLine.println(out,bwOut," completed in "+timeString+ " (s).\n\tFrom "+crn.getSpecies().size()+" species to " +obtainedPartition.size()+" blocks ("+reductionRatio+"% of original size).");
				msg+="\n\tFrom "+crnToConsider.getSpecies().size()+" "+((crnToConsider.getMdelDefKind().equals(ODEorNET.ODE))?"variables":"species")+" to " +CRNReducerCommandLine.blockOrBlocks(obtainedPartition.size())+" ("+reductionRatio+"% of original size).";
			}
			CRNReducerCommandLine.println(out,bwOut,msg);	
		}
		return factor;
	}

}
