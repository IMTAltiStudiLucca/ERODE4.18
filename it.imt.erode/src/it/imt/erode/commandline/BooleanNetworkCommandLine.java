package it.imt.erode.commandline;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.eclipse.ui.console.MessageConsoleStream;

import com.microsoft.z3.Z3Exception;

import it.imt.erode.auxiliarydatastructures.IPartitionAndBoolean;
import it.imt.erode.auxiliarydatastructures.PartitionAndString;
import it.imt.erode.auxiliarydatastructures.PartitionAndStringAndBoolean;
import it.imt.erode.booleannetwork.auxiliarydatastructures.BNandPartition;
import it.imt.erode.booleannetwork.implementations.InfoBooleanNetworkReduction;
import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;
import it.imt.erode.crn.implementations.InfoCRNReduction;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.importing.CRNImporter;
import it.imt.erode.importing.GUICRNImporter;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.importing.booleannetwork.GUIBooleanNetworkImporter;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.CRNBisimulationsNAry;
import it.imt.erode.partitionrefinement.algorithms.ExactFluidBisimilarity;
import it.imt.erode.partitionrefinement.algorithms.booleannetworks.FBEAggregationFunctions;
import it.imt.erode.partitionrefinement.algorithms.booleannetworks.RandomizedForwardBooleanEquivalence;
import it.imt.erode.partitionrefinement.algorithms.booleannetworks.RandomizedForwardBooleanEquivalenceTau;
import it.imt.erode.partitionrefinement.algorithms.booleannetworks.RndFMETauMonomialException;
import it.imt.erode.partitionrefinement.algorithms.booleannetworks.SMTBackwardBooleanEquivalence;
import it.imt.erode.partitionrefinement.algorithms.booleannetworks.SMTForwardBooleanEquivalence;
import it.imt.erode.simulation.output.DataOutputHandlerAbstract;

public class BooleanNetworkCommandLine extends AbstractCommandLine {

	private IBooleanNetwork bn;
	
	public BooleanNetworkCommandLine(CommandsReader commandsReader, boolean fromGUI) {
		super(commandsReader,fromGUI);
	}

	public BooleanNetworkCommandLine(CommandsReader commandsReader, IBooleanNetwork bn, IPartition partition) {
		this(commandsReader,bn,partition,false);
	}
	public BooleanNetworkCommandLine(CommandsReader commandsReader, IBooleanNetwork bn, IPartition partition, boolean fromGUI) {
		this(commandsReader,fromGUI);
		this.bn=bn;
		this.setPartition(partition);
	}
	
	@Override
	public void executeCommands(boolean print, MessageConsoleStream out, BufferedWriter bwOut)
			throws IOException, InterruptedException, UnsupportedFormatException, Z3Exception {
		
		//defaultReduction(print, out, bwOut);
		
		if(HASTOCHECKLIBRARIESZ3){
			if(!librariesPresent){
				checkLibrariesZ3(out,bwOut);
			}
			if(!librariesPresent){
				return;
			}
		}
		
		String command = null;
		try{
			while(!Terminator.hasToTerminate(terminator)){
				command = commandsReader.getNextCommand();

				boolean updateCRN=false;
				if(command.startsWith("this")){
					command=command.substring(command.indexOf('=')+1).trim();
					updateCRN=true;
				}
				if(command.equals("quit")||command.equals("quit()")){
					CRNReducerCommandLine.println(out,bwOut,"I received quit() and thus terminate.");
					//System.exit(0);
					return;
				}
				else if(command.equals("NONINTERACTIVE")){
					//System.exit(0);
					return;
				}
				else if(command.equalsIgnoreCase("newline")){
					CRNReducerCommandLine.println(out,bwOut,"");
				}
				/*else if(command.startsWith("-h")||command.startsWith("--help")){
					usageShort(fromGUI,out,bwOut);
				}
				else if(command.startsWith("-m")||command.startsWith("--man")){
					printMan(fromGUI,out,bwOut);
				}*/
				else if(command.startsWith("reduceBBE(")){//else if(command.startsWith("reduceEFLsmt(")){
					handleReduceCommand(command,updateCRN,"BBE",out,bwOut);//handleReduceCommand(command,updateCRN,"eflsmt");
				}
				else if(command.startsWith("reduceFBE(")){//else if(command.startsWith("reduceOOBsmt(")){
					handleReduceCommand(command,updateCRN,"FBE",out,bwOut);//handleReduceCommand(command,updateCRN,"oobsmt");
				}
				else if(command.startsWith("reduceFME(")){//else if(command.startsWith("reduceOOBsmt(")){
					handleReduceCommand(command,updateCRN,"FME",out,bwOut);//handleReduceCommand(command,updateCRN,"oobsmt");
				}
				else if(command.startsWith("reduceRndFME(")){//else if(command.startsWith("reduceOOBsmt(")){
					handleReduceCommand(command,updateCRN,"rndFME",out,bwOut);//handleReduceCommand(command,updateCRN,"oobsmt");
				}
				else if(command.startsWith("writeBN")) {
					handleWriteCommand(command,out,bwOut);
				}
				else if(command.startsWith("exportBoolNet")) {
					handleExportBoolNetCommand(command, out, bwOut);
				}
				else if(command.startsWith("exportSBMLQual")) {
					handleExportSBMLQualCommand(command, out, bwOut);
				}
				else if(isSupportedBySubclass(command)) {
					handleExportSBMLQualCommand(command, out, bwOut);
				}
				/*else if(command.startsWith("setIC(")){
					handleSetIC(command,out,bwOut);
				}*/
				else if(command.equals("")){
				}
				/*else {
					importModel(command, print,out,bwOut);
				}*/
			}
		}catch(UnsupportedOperationException | ArithmeticException e){
			String message="The command "+command +" failed:\n"+e.getMessage();
			CRNReducerCommandLine.printWarning(out, bwOut, true, messageDialogShower, message, true,DialogType.Error);
			CRNReducerCommandLine.printStackTrace(null,null, e);
		}catch(Exception e){
			String message="The command "+command +" failed:\n"+e.toString();//+e.getMessage();
			CRNReducerCommandLine.printWarning(out, bwOut, true, messageDialogShower, message, true,DialogType.Error);
		}
		if(Terminator.hasToTerminate(terminator)){
			CRNReducerCommandLine.println(out,bwOut, "I terminate, as required");
		}
		
	}
	
	/**
	 * To be defined in classes extending this to support further commands
	 * @param command
	 * @return
	 */
	public boolean isSupportedBySubclass(String command) {
		return false;
	}

	private void handleWriteCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) {
		String fileName = null;
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		boolean originalNames=false;
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for (String parameter : parameters) {
			if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("fileOut=>")){
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				fileName = parameter.substring("fileOut=>".length(), parameter.length());
				//format = (fromGUI)?SupportedFormats.CRNGUI:SupportedFormats.CRN;
			}
			else if(parameter.startsWith("originalNames=>")){
				if(parameter.length()<="originalNames=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify whether if orginal names should be used or not. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String orn = parameter.substring("originalNames=>".length(), parameter.length());
				if(orn.equalsIgnoreCase("true")) {
					originalNames=true;
				}
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameter+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return;
			}
		}
		if(fileName ==null || fileName.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		
		GUIBooleanNetworkImporter.printToBNERODEFIle(bn, partition, fileName, null, true, out, bwOut, originalNames,null);
	}
	
	public void handleExportBoolNetCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) {
		//exportBoolNet({fileOut=>aaa.bnet,originalNames=>true})
		String fileName = null;
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		boolean originalNames=false;
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for (String parameter : parameters) {
			if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("fileOut=>")){
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				fileName = parameter.substring("fileOut=>".length(), parameter.length());
				//format = (fromGUI)?SupportedFormats.CRNGUI:SupportedFormats.CRN;
			}
			else if(parameter.startsWith("originalNames=>")){
				if(parameter.length()<="originalNames=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify whether if orginal names should be used or not. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String orn = parameter.substring("originalNames=>".length(), parameter.length());
				if(orn.equalsIgnoreCase("true")) {
					originalNames=true;
				}
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameter+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return;
			}
		}
		if(fileName ==null || fileName.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		
		GUIBooleanNetworkImporter.printToBNBoolNetFIle(bn, partition, fileName, null, true, out, bwOut, originalNames);
	}
	
	private void handleExportSBMLQualCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) {
		String fileName = null;
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		//boolean originalNames=false;
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for (String parameter : parameters) {
			if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("fileOut=>")){
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				fileName = parameter.substring("fileOut=>".length(), parameter.length());
				//format = (fromGUI)?SupportedFormats.CRNGUI:SupportedFormats.CRN;
			}
//			else if(parameter.startsWith("originalNames=>")){
//				if(parameter.length()<="originalNames=>".length()){
//					CRNReducerCommandLine.println(out,bwOut,"Please, specify whether if orginal names should be used or not. ");
//					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
//					return;
//				}
//				String orn = parameter.substring("originalNames=>".length(), parameter.length());
//				if(orn.equalsIgnoreCase("true")) {
//					originalNames=true;
//				}
//			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameter+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return;
			}
		}
		if(fileName ==null || fileName.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		
		Collection<String> preambleCommentLines=null;
		//GUIBooleanNetworkImporter.printToBNBoolNetFIle(bn, partition, fileName, null, true, out, bwOut, originalNames);
		GUIBooleanNetworkImporter.printToSBMLQualFIle(bn, partition, fileName, preambleCommentLines, true, out, bwOut);
	}

	public IPartitionAndBoolean handleReduceCommand(String command, boolean updateCRN, String reduction, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException, Z3Exception, IOException {
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return null;
		}
		String computeOnlyPartition="false";
		String simplify="false";
		
		//String reduction=null;
		String reducedFileName=null;
		String partitionInfoFileName=null;
		String csvSMTTimeFileName=null;
		String prePartitionWRTIC="false";
		//String computeOnlyPartition="false";
		String prePartitionUserDefined="false";
		String csvFile=null;
		String aggregationFunction=null;
		boolean attemptDropTauN=false;

		boolean print=true;
		//boolean newReductionAlgorithm=false;
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
			else if(parameters[p].startsWith("fileWhereToCSVSMTTimes=>")){
				if(parameters[p].length()<="fileWhereToCSVSMTTimes=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write the comma-separated SMT time checks of each iteration.");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				csvSMTTimeFileName = parameters[p].substring("fileWhereToCSVSMTTimes=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("print=>")){
				if(parameters[p].length()<="print=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if you want to print information.");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				if(parameters[p].substring("print=>".length(), parameters[p].length()).equalsIgnoreCase("false")){
					print=false;
				}
			}
			else if(parameters[p].startsWith("attemptDropTauN=>")){
				if(parameters[p].length()<="attemptDropTauN=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if you want to attempt dropping monomials with tau^n, n >1.");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				if(parameters[p].substring("attemptDropTauN=>".length(), parameters[p].length()).equalsIgnoreCase("true")){
					attemptDropTauN=true;
				}
			}
			else if(parameters[p].startsWith("reducedFile=>")){
				if(parameters[p].length()<="reducedFile=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write the reduced model. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				reducedFileName = parameters[p].substring("reducedFile=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("computeOnlyPartition=>")){
				if(parameters[p].length()<="computeOnlyPartition=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if or not only the partition has to be computed (without thus reducing the model). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				computeOnlyPartition = parameters[p].substring("computeOnlyPartition=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("simplify=>")){
				if(parameters[p].length()<="simplify=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if the reduced update functins should be simplified. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				simplify = parameters[p].substring("simplify=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("prePartition=>")){
				if(parameters[p].length()<="prePartition=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if you want to prepartion the species of the model according to their initial conditions or to the views. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				String prep = parameters[p].substring("prePartition=>".length(), parameters[p].length()).trim();
				if(prep.compareToIgnoreCase("IC")==0){
					prePartitionWRTIC = "true";
					prePartitionUserDefined = "false";
				}
				else if(prep.compareToIgnoreCase("NO")==0){
					prePartitionWRTIC = "false";
					prePartitionUserDefined = "false";
				}
				else if(prep.compareToIgnoreCase("USER")==0){
					prePartitionWRTIC = "false";
					prePartitionUserDefined = "true";
				}
				else if(prep.compareToIgnoreCase("USER_AND_IC")==0){
					prePartitionWRTIC = "true";
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
			else if(parameters[p].startsWith("aggregationFunction=>")){
				if(parameters[p].length()<="aggregationFunction=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the aggregation function to use. It can be either OR or AND. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				aggregationFunction = parameters[p].substring("aggregationFunction=>".length(), parameters[p].length());
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return null;
			}
		}

		if(bn==null){
			CRNReducerCommandLine.println(out,bwOut,"Before reducing a model it is necessary to load it. "); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return null;
		}

		if(reduction ==null || reduction.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the reduction technique. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return null;
		}
		FBEAggregationFunctions aggr=null;
		if(reduction.equals("FBE") ||reduction.equals("FME") ||reduction.equals("rndFME") || needsAggregationFunctionSubClass(reduction)) {
			if(aggregationFunction==null || aggregationFunction.length()==0) {
				CRNReducerCommandLine.println(out,bwOut,"Please, specify an aggregation function. ");
				CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
				return null;
			}
			else {
				aggr= FBEAggregationFunctions.valueOf(aggregationFunction);
				if(aggr==null) {
					CRNReducerCommandLine.println(out,bwOut,"Please, specify an aggregation function. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
			}
		}

		//groupedFileName = crn.getName()+"grouped.net";

		//boolean writeReducedCRN = reducedFileName!=null && !reducedFileName.equals("");

		//String originalCRNShort="Original model: "+bn.toStringShort();
		//String reducedCRNShort;
		IPartition obtainedPartition;
		//BNandPartition cp=null;

		String icWarning="";

		IPartition initial = partition;
		if(prePartitionWRTIC!=null && prePartitionWRTIC.equalsIgnoreCase("true")){
			if(print){
				//CRNReducerCommandLine.print(out,bwOut,"Prepartinioning with respect to the initial concentrations of the species...");
				CRNReducerCommandLine.print(out,bwOut,"IC prepartinioning...");
			}
			long begin = System.currentTimeMillis();
			initial=ExactFluidBisimilarity.prepartitionWRTIC(bn.getSpecies(),partition,false,out,bwOut,terminator);
			long end = System.currentTimeMillis();
			if(print){
				//CRNReducerCommandLine.println(out,bwOut," completed. The "+crn.getSpecies().size()+" species have been prepartitioned in "+initial.size()+" blocks. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");
				CRNReducerCommandLine.println(out,bwOut," completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)"+".\n\tThe "+bn.getSpecies().size()+" species have been prepartitioned in "+ blockOrBlocks(initial.size()));
			}
		}
		else{
			if(reduction.equalsIgnoreCase("BBE")){
				icWarning="The partition might not be consistent with the initial conditions.\n          Each species in the reduced model has the initial value of the representative of its partition block.";
//				if(newReductionAlgorithm){
//					//icWarning="The partition might not be consistent with the initial conditions.\n          Each species in the reduced model has the sum of the initial concentrations of its partition block.";
//				}
//				else{
//					//icWarning="The partition might not be consistent with the initial conditions.\n          Each species in the reduced model has the initial concentration of the representative of its partition block.";
//				}

			}
		}

		if(prePartitionUserDefined!=null && prePartitionUserDefined.equalsIgnoreCase("true")){
			if(print){
				//CRNReducerCommandLine.print(out,bwOut,"Pre-partinioning with respect to the views/groups specified in the original file...");
				CRNReducerCommandLine.print(out,bwOut,"User-defined prepartinioning...");
			}
			long begin = System.currentTimeMillis();
			initial= CRNBisimulationsNAry.prepartitionUserDefined(bn.getSpecies(),bn.getUserDefinedPartition(), false, out,bwOut,terminator);
			long end = System.currentTimeMillis();
			setPartition(initial);
			if(print){
				//CRNReducerCommandLine.println(out,bwOut," completed. The "+crn.getSpecies().size()+" species have been prepartitioned in "+initial.size()+" blocks. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");
				CRNReducerCommandLine.println(out,bwOut," completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)"+".\n\tThe "+bn.getSpecies().size()+" species have been prepartitioned in "+ blockOrBlocks(initial.size()));
			}
		}
		else {
			if(getPartition()==null) {
				setPartition();
			}
		}


		String reductionName = reduction.toUpperCase();

		String pre = "";
		if(print){
			if(bn.getName()!=null){
				CRNReducerCommandLine.print(out,bwOut,pre+reductionName+" partitioning of "+bn.getName()+"...");
			}
			else{
				CRNReducerCommandLine.print(out,bwOut,pre+reductionName+" partitioning...");//CRNReducerCommandLine.print(out,bwOut,pre+reductionName+" partitioning the current model ...");
			}
		}

		String reducedModelName = null;
		if(reducedFileName!=null){
			reducedModelName = GUICRNImporter.getModelName(reducedFileName);
		}
		else{
			reducedModelName = GUICRNImporter.getModelName(bn.getName());
			reducedModelName = reducedModelName + reduction;
		}

		List<Double> smtChecksTime = null;

		SMTForwardBooleanEquivalence smtFBE =null;
		SMTBackwardBooleanEquivalence smtBBE=null;
		RandomizedForwardBooleanEquivalence smtRndFME=null;
		//RandomizedForwardBooleanEquivalenceTau smtRndFME=null;
		
		//Compute the partition
		long begin = System.currentTimeMillis();
		boolean succeeded=true;
		String smtTime=null;
		if(reduction.equalsIgnoreCase("BBE")){
			smtBBE = new SMTBackwardBooleanEquivalence(simplify.equalsIgnoreCase("true"));
			PartitionAndString ps = smtBBE.computeBBEsmt(bn,initial,CRNReducerCommandLine.verbose,out,bwOut,print,terminator,messageDialogShower);
			obtainedPartition = ps.getPartition();
			smtTime = ps.getString();
			smtChecksTime=smtBBE.getSMTChecksSecondsAtStep();
			//smtBBE=null;
		}
		else if(reduction.equalsIgnoreCase("fbe")){
			obtainedPartition=initial;
			smtFBE = new SMTForwardBooleanEquivalence(aggr/*FBEAggregationFunctions.OR*/,simplify.equalsIgnoreCase("true"));
			PartitionAndStringAndBoolean ps = smtFBE.computeOFLsmt(bn, initial, CRNReducerCommandLine.verbose, out, bwOut, print, terminator,false);
			 
			obtainedPartition = ps.getPartition();
			smtTime=ps.getString();
			smtChecksTime=smtFBE.getSMTChecksSecondsAtStep();
			succeeded=ps.booleanValue();
						 
		}
		else if(reduction.equalsIgnoreCase("fme")){
			obtainedPartition=initial;
			
			//Previous implementation of OFL reduction of POPL where we were not using the binary characterization, but we were using a counterexample-based apporach similar to the EFL case. It is not guaranteed to give the coarsest reduction (but we didn't find any model for which this happens). 
			//SMTOrdinaryFluidBisimilarity9 smtOFL = new SMTOrdinaryFluidBisimilarity9();
			//obtainedPartition = smtOFL.computeOFLsmt(crn, initial, verbose);			
			smtFBE = new SMTForwardBooleanEquivalence(aggr/*FBEAggregationFunctions.OR*/,simplify.equalsIgnoreCase("true"));
			boolean realIfMV=bn.getRealSortIfMV();
			PartitionAndStringAndBoolean ps = smtFBE.computeOFLsmt(bn, initial, CRNReducerCommandLine.verbose, out, bwOut, print, terminator,realIfMV);
			 
			obtainedPartition = ps.getPartition();
			smtTime=ps.getString();
			smtChecksTime=smtFBE.getSMTChecksSecondsAtStep();
			succeeded=ps.booleanValue();
			//smtFBE=null;
			 
		}
		else if(reduction.equalsIgnoreCase("rndfme")) {
			if(attemptDropTauN) {
				smtRndFME = new RandomizedForwardBooleanEquivalenceTau(aggr, succeeded);
			}
			else {
				smtRndFME = new RandomizedForwardBooleanEquivalence(aggr, succeeded);
			}
			//
			
			
			boolean realIfMV=bn.getRealSortIfMV();
			
			PartitionAndStringAndBoolean ps;
			try {
				ps = smtRndFME.computeOFLsmt(bn, initial, CRNReducerCommandLine.verbose, out, bwOut, print, terminator,realIfMV);
				obtainedPartition = ps.getPartition();
				smtTime=ps.getString();
				smtChecksTime=smtRndFME.getSMTChecksSecondsAtStep();
				succeeded=ps.booleanValue();
			} catch (/*Z3Exception | IOException |*/ RndFMETauMonomialException e) {
				succeeded=false;
				obtainedPartition=partition;
				CRNReducerCommandLine.println(out, bwOut, "The reduction failed.");
			}
		}
		else if(isReductionSupportedBySubCalss(reduction)) {
			 boolean realIfMV=false;//bn.getRealSortIfMV();
			 PartitionAndStringAndBoolean ps = reduceBySubClass(reduction,aggr,initial,out,bwOut,print,realIfMV);
			 obtainedPartition = ps.getPartition();
			 smtTime=ps.getString();
			 //smtChecksTime=smtFBE.getSMTChecksSecondsAtStep();
			 succeeded=ps.booleanValue();
		}
		else{
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"The reduction techinque "+reduction+ " does not exist.",DialogType.Error);
			//+ "" Please use one among NFB, NBB, BB, FB, DSB, SMB, WFB, EFL, BDE, FDE or GEFL.");
			return null;
		}
		long end = System.currentTimeMillis();

		if(CRNReducerCommandLine.SETREPRESENTATIVEBYMINAFTERPARTITIONREFINEMENT){
			obtainedPartition.setMinAsRepresentative();
		}

		if(CRNReducerCommandLine.printPartition){
			CRNReducerCommandLine.println(out,bwOut,obtainedPartition);
		}


		if(succeeded && !Terminator.hasToTerminate(terminator)){
			int original = bn.getSpecies().size();
			int reduced = obtainedPartition.size();
			double factor = (reduced)/((double)original);
			if(print){
				double time = (end-begin)/1000.0;
				String timeString = String.format( CRNReducerCommandLine.MSFORMAT, (time) );
				String msg=" completed in "+timeString+ " (s).";
				if(smtTime!=null){
					msg+="\n"+smtTime;
				}
				if(factor== 1.0){
					//CRNReducerCommandLine.println(out,bwOut," completed. No reduction possible. Time necessary: "+timeString+ " (s).");
					//CRNReducerCommandLine.println(out,bwOut," completed in "+timeString+ " (s).\n\tNo reduction possible.");
					msg+="\n\tNo reduction possible.";
					if(!computeOnlyPartition.equalsIgnoreCase("true")){
						computeOnlyPartition="true";
						msg+="\n\tI do not compute the reduced model.";
					}
				}
				else{
					String reductionRatio = String.format( "%.2f", ((factor*100.0)) );
					//CRNReducerCommandLine.println(out,bwOut," completed in "+timeString+ " (s).\n\tFrom "+crn.getSpecies().size()+" species to " +obtainedPartition.size()+" blocks ("+reductionRatio+"% of original size).");
					msg+="\n\tFrom "+bn.getSpecies().size()+" species to " +blockOrBlocks(obtainedPartition.size())+" ("+reductionRatio+"% of original size).";
				}
				CRNReducerCommandLine.println(out,bwOut,msg);	

				InfoCRNReduction infoReduction = new InfoCRNReduction(bn.getName(), bn.getSpecies().size(), bn.getSpecies().size(), 0,factor, initial.size(),obtainedPartition, end-begin, reduction);
				infoReduction.setReducedReactions(obtainedPartition.size());
				writeReductionInfoInCSVFile(out, bwOut, csvFile, infoReduction,null);
			}
			//If I reduce of at least the 20%
			/*if(factor <= 0.80){
				if(print){
					CRNReducerCommandLine.println(out,bwOut,"************************************************* REDUCED TO "+factor+"*SIZE *************************************************");
				}
			}*/

			if(smtChecksTime!=null && csvSMTTimeFileName!=null){
				DataOutputHandlerAbstract.writeCSV(csvSMTTimeFileName, "smtdat", "Iteration", "SMT check seconds", 1, 1, smtChecksTime, out, bwOut);
			}


			if(partitionInfoFileName!=null && !partitionInfoFileName.equals("")){
				if(print){
					CRNReducerCommandLine.print(out,bwOut,"Writing the partition to file "+partitionInfoFileName+" ...");
				}
				CRNImporter.printPartition(bn.getName(), obtainedPartition, partitionInfoFileName, CRNReducerCommandLine.verbose, reduction,out,bwOut,null);

				if(print){
					CRNReducerCommandLine.println(out,bwOut," completed");
				}
			}

			boolean writeReducedCRN = reducedFileName!=null && !reducedFileName.equals("");

			double redSizeOverOrigSize=((double)obtainedPartition.size())/bn.getSpecies().size();
			InfoBooleanNetworkReduction infoReduction = new InfoBooleanNetworkReduction(bn.getName(), reduction, 
					bn.getSpecies().size(),redSizeOverOrigSize,obtainedPartition.size(), end-begin,getPartition().size(),obtainedPartition);

			if(!computeOnlyPartition.equalsIgnoreCase("true")){
				computeReducedModel(updateCRN, reduction, out, bwOut, reducedFileName, print, aggr, obtainedPartition,
						icWarning, reductionName, reducedModelName, smtFBE, smtBBE,smtRndFME, writeReducedCRN, infoReduction);
			}	


			if(print){
				if(icWarning!=null && !icWarning.equals("")){
					CRNReducerCommandLine.printWarning(out, bwOut,false,messageDialogShower,icWarning,DialogType.Warning);
				}
			}

		}
		else if(!succeeded) {
			writeReductionNotSucceededInfoInCSVFile(out, bwOut, csvFile, bn, reductionName, initial.size());
		}
		smtBBE=null;
		smtFBE=null;
		return new IPartitionAndBoolean(obtainedPartition, succeeded);
	}

	public boolean needsAggregationFunctionSubClass(String reduction) {
		return false;
	}

	public PartitionAndStringAndBoolean reduceBySubClass(String reduction, FBEAggregationFunctions aggr,IPartition initial, MessageConsoleStream out, BufferedWriter bwOut, boolean print, boolean realIfMV) throws Z3Exception, IOException{
		throw new UnsupportedOperationException(reduction);
	}

	public boolean isReductionSupportedBySubCalss(String reduction) {
		return false;
	}

	public void computeReducedModel(boolean updateCRN, String reduction, MessageConsoleStream out, BufferedWriter bwOut,
			String reducedFileName, boolean print, FBEAggregationFunctions aggr, IPartition obtainedPartition,
			String icWarning, String reductionName, String reducedModelName, SMTForwardBooleanEquivalence smtFBE,
			SMTBackwardBooleanEquivalence smtBBE, RandomizedForwardBooleanEquivalence smtRndFME, boolean writeReducedCRN, InfoBooleanNetworkReduction infoReduction)
			throws IOException {
		long begin;
		long end;
		//Now compute the reduced model
		if(print){
			//CRNReducerCommandLine.print(out,bwOut,"Reducing the model with respect to the obtained partition ... ");
			CRNReducerCommandLine.print(out,bwOut,"Creating reduced model... ");
		}

		BNandPartition bp=null;
		
		begin = System.currentTimeMillis();
		if(reduction.equalsIgnoreCase("bbe")) {
			bp = smtBBE.computeReducedBBE(bn, reducedModelName, obtainedPartition, "//", out, bwOut, terminator);
			smtBBE=null;
		}
		else if(reduction.equalsIgnoreCase("fbe")||reduction.equalsIgnoreCase("fme")) {
			bp = smtFBE.computeReducedFBE(bn, reducedModelName, obtainedPartition, "//",  out, bwOut, terminator, aggr);
			smtFBE=null;
		}
		else if(reduction.equalsIgnoreCase("rndfme")) {
			bp = smtRndFME.computeReducedFBE(bn, reducedModelName, obtainedPartition, "//",  out, bwOut, terminator, aggr);
			smtFBE=null;
		}

		end = System.currentTimeMillis();
		if(print){
			CRNReducerCommandLine.println(out,bwOut,"completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+" (s).");
		}
		if(bp!=null && infoReduction!=null){
			infoReduction.setReducedSpecies(bp.getBN().getSpecies().size());
		}

		String originalBNShort="Original model: "+bn.toStringShort();
		String reducedBNShort=reductionName+" reduced model: "+bp.getBN().toStringShort();
		if(print){
			CRNReducerCommandLine.println(out,bwOut,originalBNShort);
			//CRNReducerCommandLine.println(out,bwOut," Size of the obtained partition: "+sizeOfObtainedPartition);
			CRNReducerCommandLine.println(out,bwOut,reducedBNShort);

			if(icWarning!=null && !icWarning.equals("")){
				CRNReducerCommandLine.printWarning(out, bwOut,false,messageDialogShower,icWarning,DialogType.Warning);
			}
		}

		if(writeReducedCRN && !Terminator.hasToTerminate(terminator)){
			boolean originalNames=false;
			boolean verbose=true;
			Collection<String> preambleCommentLines=infoReduction.toComments();
			/*
			 *  # Automatically generated from <original filename> via <technique>
		# Original number of species:Size
		# Original number of reactions:
		# Reduced number of species:
		# Reduced number of reactions:
		# Time taken: 
			 */
			//SupportedFormats format = (fromGUI)?SupportedFormats.CRNGUI:SupportedFormats.CRN;
			//String commentSymbol =(fromGUI)?"//":"#";
			//CRNReducerCommandLine.print(out, bwOut, "Writing reduced file in "+reducedFileName+" ... ");
			GUIBooleanNetworkImporter.printToBNERODEFIle(bp.getBN(), bp.getPartition(), reducedFileName, preambleCommentLines, verbose, out, bwOut, originalNames,null);
			//CRNReducerCommandLine.print(out, bwOut, "completed.");
			//printToERODEFIle(crn, obtainedPartition, name, assignPopulationOfRepresentative, groupAccordingToCurrentPartition, preambleCommentLines, verbose, icComment, out, bwOut, type, rnEncoding, originalNames);
			//writeCRN(reducedFileName,bp.getBN(),bp.getPartition(),format,infoReduction.toComments(),icWarning,null,out,bwOut,false,null);
			
		}

		if(updateCRN && ! Terminator.hasToTerminate(terminator)){
			bn = bp.getBN();
			partition=bp.getPartition();
			if(print){
				CRNReducerCommandLine.println(out,bwOut,"The current model is updated with the reduced one.");
			}
		}
	}
	
	
	
	
	
	public IBooleanNetwork getBN() {
		return this.bn;
	}
	
	
	private void setPartition() {
		IPartition def = new Partition(bn.getSpecies().size());
		IBlock b = new Block();
		def.add(b);
		for(ISpecies sp : bn.getSpecies()) {
			b.addSpecies(sp);
		}
		setPartition(def);
	}

	//
//	@SuppressWarnings("unused")
//	private void defaultReduction(boolean print, MessageConsoleStream out, BufferedWriter bwOut) throws IOException {
//		boolean verbose=false;
//		boolean printPartition=true;
//		
//		
//		
//		//handleReduceCommand(command,updateCRN,"BDE",out,bwOut);
//		//SMTExactFluidBisimilarity smtEFL = new SMTExactFluidBisimilarity();
//		//PartitionAndString ps = smtEFL.computeEFLsmt(crn, initial, verbose,out,bwOut,print,terminator,messageDialogShower);
//
//		IPartition initial = this.partition;
//		String prePartitionUserDefined="true";
//		if(prePartitionUserDefined!=null && prePartitionUserDefined.equalsIgnoreCase("true")){
//			if(print){
//				//CRNReducerCommandLine.print(out,bwOut,"Prepartinioning with respect to the views/groups specified in the original file...");
//				CRNReducerCommandLine.print(out,bwOut,"User-defined prepartinioning...");
//			}
//			long begin = System.currentTimeMillis();
//			initial = CRNBisimulationsNAry.prepartitionUserDefined(bn.getSpecies(),bn.getUserDefinedPartition(), false, out,bwOut,terminator);
//			long end = System.currentTimeMillis();
//			if(print){
//				//CRNReducerCommandLine.println(out,bwOut," completed. The "+crn.getSpecies().size()+" species have been prepartitioned in "+initial.size()+" blocks. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");
//				CRNReducerCommandLine.println(out,bwOut," completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)"+".\n\tThe "+bn.getSpecies().size()+" species have been prepartitioned in "+ blockOrBlocks(initial.size()));
//			}
//		}
//		
//		
//		
//		String pre = "";
//		String reductionName = "BoolBDE";
//		if(print){
//			if(bn.getName()!=null){
//				CRNReducerCommandLine.print(out,bwOut,pre+reductionName+" partitioning "+bn.getName()+"...");
//			}
//			else{
//				CRNReducerCommandLine.print(out,bwOut,pre+reductionName+" partitioning...");//CRNReducerCommandLine.print(out,bwOut,pre+reductionName+" partitioning the current model ...");
//			}
//		}
//		
//		
//		
//		
//		
//		
//		long begin = System.currentTimeMillis();
//		SMTBackwardBooleanEquivalence smtBBE = new SMTBackwardBooleanEquivalence();
//		PartitionAndString ps = smtBBE.computeBBEsmt(bn,initial,verbose,out,bwOut,print,terminator,messageDialogShower);
//		String smtTime = ps.getString();
//		//List<Double> smtChecksTime = smtBEFL.getSMTChecksSecondsAtStep();
//		long end = System.currentTimeMillis();
//		IPartition obtainedPartition = ps.getPartition();
//		
//		int original = bn.getSpecies().size();
//		int reduced = obtainedPartition.size();
//		double factor = (reduced)/((double)original);
//		if(print){
//			double time = (end-begin)/1000.0;
//			String timeString = String.format( CRNReducerCommandLine.MSFORMAT, (time) );
//			String msg=" completed in "+timeString+ " (s).";
//			if(smtTime!=null){
//				msg+="\n"+smtTime;
//			}
//			if(factor== 1.0){
//				//CRNReducerCommandLine.println(out,bwOut," completed. No reduction possible. Time necessary: "+timeString+ " (s).");
//				//CRNReducerCommandLine.println(out,bwOut," completed in "+timeString+ " (s).\n\tNo reduction possible.");
//				msg+="\n\tNo reduction possible.";
//			}
//			else{
//				String reductionRatio = String.format( "%.2f", ((factor*100.0)) );
//				//CRNReducerCommandLine.println(out,bwOut," completed in "+timeString+ " (s).\n\tFrom "+crn.getSpecies().size()+" species to " +obtainedPartition.size()+" blocks ("+reductionRatio+"% of original size).");
//				msg+="\n\tFrom "+bn.getSpecies().size()+" species to " +blockOrBlocks(obtainedPartition.size())+" ("+reductionRatio+"% of original size).";
//			}
//			CRNReducerCommandLine.println(out,bwOut,msg);
//			
//			if(printPartition){
//				CRNReducerCommandLine.println(out,bwOut,"\n");
//				CRNReducerCommandLine.println(out,bwOut,obtainedPartition);
//			}
//		}
//	}
//	
	private String blockOrBlocks(int numberOfBlocks) {
		if(numberOfBlocks==1){
			return numberOfBlocks + " block";
		}
		else{
			return numberOfBlocks + " blocks";
		}
	}
	
	public static void main(String[] args) {
		
	}

}
