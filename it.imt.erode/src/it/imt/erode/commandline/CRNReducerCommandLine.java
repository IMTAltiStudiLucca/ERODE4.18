package it.imt.erode.commandline;

import java.io.BufferedReader;

//-Xmx8136M

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;

import org.eclipse.ui.console.MessageConsoleStream;
import org.jdom.JDOMException;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.text.parser.ParseException;

import com.eteks.parser.CompilationException;
//import com.eteks.parser.CompilationException;
import com.microsoft.z3.Z3Exception;

import fern.simulation.Simulator;
import fern.simulation.algorithm.GibsonBruckSimulator;
import fern.simulation.algorithm.GillespieEnhanced;
import fern.simulation.algorithm.GillespieSimple;
import fern.simulation.algorithm.HybridMaximalTimeStep;
import fern.simulation.algorithm.TauLeapingAbsoluteBoundSimulator;
import fern.simulation.algorithm.TauLeapingRelativeBoundSimulator;
import fern.simulation.algorithm.TauLeapingSpeciesPopulationBoundSimulator;
import fern.simulation.controller.DefaultController;
import it.imt.erode.auxiliarydatastructures.CRNandPartition;
import it.imt.erode.auxiliarydatastructures.IPartitionAndBoolean;
import it.imt.erode.auxiliarydatastructures.IPartitionsAndBoolean;
import it.imt.erode.auxiliarydatastructures.PartitionAndString;
import it.imt.erode.auxiliarydatastructures.PartitionAndStringAndBoolean;
import it.imt.erode.auxiliarydatastructures.Reduction;
import it.imt.erode.booleannetwork.bnetparser.BNetParser;
import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;
import it.imt.erode.booleannetwork.updatefunctions.BasicModelElementsCollector;
import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.cage.CreateCAGEScript;
import it.imt.erode.causalgraph.CausalGraphCreator;
import it.imt.erode.crn.differentialHull.DifferentialHull;
import it.imt.erode.crn.implementations.CRN;
import it.imt.erode.crn.implementations.Command;
import it.imt.erode.crn.implementations.CommandParameter;
import it.imt.erode.crn.implementations.InfoCRNReduction;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.ICommand;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.label.ILabel;
import it.imt.erode.decode.Decode;
import it.imt.erode.dot.FigureCreator;
import it.imt.erode.expression.evaluator.MathEval;
import it.imt.erode.importing.AbstractImporter;
import it.imt.erode.importing.BNetImporter;
import it.imt.erode.importing.BioNetGenImporter;
import it.imt.erode.importing.BoolCubeImporter;
import it.imt.erode.importing.C2E2Exporter;
import it.imt.erode.importing.CRNImporter;
import it.imt.erode.importing.ExportSingleUseOfParams;
import it.imt.erode.importing.FluidCompilerImporter;
import it.imt.erode.importing.FlyFastImporter;
import it.imt.erode.importing.GUICRNImporter;
import it.imt.erode.importing.ImporterOfSupportedNetworks;
import it.imt.erode.importing.LBSImporter;
import it.imt.erode.importing.MatlabODEsImporter;
import it.imt.erode.importing.ModelicaImporter;
import it.imt.erode.importing.MutableLong;
import it.imt.erode.importing.ODEorNET;
import it.imt.erode.importing.StochKitExporter;
import it.imt.erode.importing.StoichiometricMatrixExporter;
import it.imt.erode.importing.SupportedFormats;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.importing.z3Importer;
import it.imt.erode.importing.automaticallygeneratedmodels.RandomBNG;
import it.imt.erode.importing.booleannetwork.GUIBooleanNetworkImporter;
import it.imt.erode.importing.booleannetwork.GuessPrepartitionBN;
import it.imt.erode.importing.cnf.CNFImporter;
import it.imt.erode.importing.csv.CompactCSVMatrixImporter;
import it.imt.erode.importing.sbml.FluxBalanceAnalysisModel;
import it.imt.erode.importing.sbml.NegativeStoichiometryException;
import it.imt.erode.importing.sbml.NonIntegerStoichiometryException;
import it.imt.erode.importing.sbml.SBMLImporter;
import it.imt.erode.javalibrarypath.JavaLibraryPathHandler;
//import it.imt.erode.onthefly.OnTheFlyBRArrayList;
import it.imt.erode.onthefly.Pair;
import it.imt.erode.onthefly.algorithm.IOnTheFly;
import it.imt.erode.onthefly.algorithm.OnTheFlyBRIterative;
import it.imt.erode.onthefly.algorithm.OnTheFlyBRRecursive;
import it.imt.erode.onthefly.algorithm.OnTheFlyFRIterative;
import it.imt.erode.onthefly.upto.UpToFactory;
import it.imt.erode.onthefly.upto.UpToType;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.CRNBisimulations;
import it.imt.erode.partitionrefinement.algorithms.CRNBisimulationsNAry;
import it.imt.erode.partitionrefinement.algorithms.ControlEquivalences;
import it.imt.erode.partitionrefinement.algorithms.DifferentialSpeciesBisimilarity;
import it.imt.erode.partitionrefinement.algorithms.EpsilonDifferentialEquivalences;
import it.imt.erode.partitionrefinement.algorithms.ExactFluidBisimilarity;
import it.imt.erode.partitionrefinement.algorithms.NetworkControllability;
import it.imt.erode.partitionrefinement.algorithms.PartitionAndMappingReactionToNewRate;
//import it.imt.erode.partitionrefinement.algorithms.SyntacticMarkovianBisimilarityAllLabelsNoSelfLoops;
import it.imt.erode.partitionrefinement.algorithms.SMTExactFluidBisimilarity;
import it.imt.erode.partitionrefinement.algorithms.SMTMetrics;
import it.imt.erode.partitionrefinement.algorithms.SMTOrdinaryFluidBisimilarityBinary;
import it.imt.erode.partitionrefinement.algorithms.SyntacticMarkovianBisimilarityOLD;
import it.imt.erode.partitionrefinement.algorithms.UCTMCLumping;
import it.imt.erode.partitionrefinement.algorithms.corn.CORN_LumpabilityForControlRN;
import it.imt.erode.partitionrefinement.algorithms.corn.HowToComputeMm;
//import it.imt.erode.partitionrefinement.algorithms.SyntacticMarkovianBisimilarityAllLabels;
import it.imt.erode.partitionrefinement.algorithms.SyntacticMarkovianBisimilarityBinary;
import it.imt.erode.partitionrefinement.algorithms.SyntacticMarkovianBisimilarityNary;
import it.imt.erode.simulation.deterministic.NonConsistentInitialConditions;
import it.imt.erode.simulation.deterministic.ODESolver;
import it.imt.erode.simulation.deterministic.SOLVERLIBRARY;
import it.imt.erode.simulation.output.DataOutputHandlerAbstract;
import it.imt.erode.simulation.stochastic.ctmcgenerator.CTMCGenerator;
import it.imt.erode.simulation.stochastic.fern.ControllerWithTerminator;
import it.imt.erode.simulation.stochastic.fern.FernNetworkFromLoadedCRN;
import it.imt.erode.simulation.stochastic.fern.observer.AmountIntervalObserverAVGBugFixed;
import it.imt.erode.simulation.stochastic.fern.observer.IntervalObserverAVGBugFixed;
import it.imt.erode.smc.multivesta.FERNState;
//import it.imt.erode.smc.multivesta.FERNState;
import it.imt.erode.utopic.MatlabODEPontryaginExporter;
import it.imt.erode.utopic.vnodelp.VNODELPExporter;
import sbml.conversion.document.ISBMLConverter;
import sbml.conversion.document.SBMLManager;
//import umontreal.iro.lecuyer.stat.Tally;
//import vesta.mc.InfoMultiQuery;
import vesta.mc.InfoMultiQuery;

// load({file=>./CRNNetworks/am.crn})
// load({file=>./BNGNetworks/dsbGroupedBioNetGen_CCP.net}) reduce({technique=>dsb,reducedFile=>outputFileNameOfReducedCRN.net,groupedFile=>outputFileNameOfGroupedCRN.net})
//load({file=>./BNGNetworks/BioNetGen_CCP.net}) reduce({technique=>dsb,reducedFile=>outputFileNameOfReducedCRN.net,groupedFile=>outputFileNameOfGroupedCRN.net})
//load({file=>./BNGNetworks/BioNetGen_CCP.net}) write({file=>outputfileName.crn}) reduce({technique=>dsb,reducedFile=>outputFileNameOfReducedCRN.net,groupedFile=>outputFileNameOfGroupedCRN.net})
//write({file=>am2.crn})

/*load({file=>./BNGNetworks/BioNetGen_CCP.net}) reduce({technique=>dsb,reducedFile=>outputFileNameOfReducedCRN.net,groupedFile=>outputFileNameOfGroupedCRN.net}) simulate_ctmc({t_end=>200,steps=>200,method=>ssa,repeats=>100,imageFile=>image})*/

//load({file=>./BNGNetworks/BioNetGen_CCP.net}) simulate_ctmc({t_end=>200,steps=>200,method=>ssa,repeats=>100,imageFile=>image}) simulate_ode({t_end=>200,steps=>200,visualizePlot=>true,imageFile=>imageODE})

public class CRNReducerCommandLine extends AbstractCommandLine {

	public static final String TOOLNAME="ERODE";//TOOLNAME="DeAR-CRN";
	public static final String TOOLVERSION="1.0";

	public static final String MSFORMAT = "%.3f";

	public static final boolean SETREPRESENTATIVEBYMINAFTERPARTITIONREFINEMENT=true;
	public static final boolean COMPUTEREPRESENTATIVEBYMINOUTSIDEPARTITIONREFINEMENT=false;
	//public static final boolean MAINTAINORDEROFSPECIESINREDUCTION=true;
	
	public static final boolean COLLAPSEREACTIONS=true;
	public static final boolean FORCELOG = false;


	//private static MessageConsoleStream out;

	//TEST. TO BE REMOVED
	//public static final LinkedHashSet<String> ModelsWithReactionsWithReagentsWithSizeGreaterThanTwo = new LinkedHashSet<String>();
	//public static final LinkedHashSet<String> ModelsWithHomeoReactions = new LinkedHashSet<String>();

	private static final String ANSI_RESET  = ""; //"\u001B[0m";
	private static final String ANSI_YELLOW = ""; //"\u001B[33m";
	//private static final String ANSI_CYAN = "\u001B[36m";
	private static final char WARNINGSYMBOL = '#';

	//public static final boolean addReactionToComposites=false;
//	public static boolean univoqueReagents=false;//public static final boolean univoqueReagents=false;
//	public static final boolean univoqueReagentsDefault=false;
//	public static final boolean univoqueProducts=true;
	//public static final boolean hasToAddToReactionsWithNonZeroStoichiometry=true;
	//public static final boolean collapseReactions=true;

	private static boolean ignoreViews=false;

	private ImporterOfSupportedNetworks importerOfSupportedNetworks=new ImporterOfSupportedNetworks();
	

	private static void printInOut(MessageConsoleStream out, BufferedWriter bwOut,String message){
		if(out!=null){
			out.print(message);
		}
		if(bwOut!=null){
			try {
				bwOut.write(message);
				bwOut.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	public static void printWarning(MessageConsoleStream out, BufferedWriter bwOut, String message/*,DialogType dialogType*/){
		printWarning(out, bwOut, false, null,message,false,DialogType.Warning);
	}
	
	public static void printWarning(MessageConsoleStream out, BufferedWriter bwOut, boolean severe, IMessageDialogShower msgDialogShower, String message,DialogType dialogType){
		printWarning(out, bwOut, severe, msgDialogShower,message,false,dialogType);
	}
	public static void printWarning(MessageConsoleStream out, BufferedWriter bwOut, String message, boolean beginWithNewLine/*,DialogType dialogType*/){
		printWarning(out, bwOut, false, null,message, beginWithNewLine,DialogType.Warning);
	}
	public static void printWarning(MessageConsoleStream out, BufferedWriter bwOut, boolean severe, IMessageDialogShower msgDialogShower, String message, boolean beginWithNewLine,DialogType dialogType){
//		if(severe&&msgDialogShower!=null){
//			out=null;
//		}
		
		System.out.println(ANSI_YELLOW + " " + message + " " + ANSI_RESET);
		if(beginWithNewLine){
			printInOut(out,bwOut,"\n");
		}
		/*String warning = warningLine(out, beginWithNewLine);
		printInOut(out,warning);
		printInOut(out, ""+message+"\n");
		printInOut(out,warning);*/
		printInOut(out, bwOut,"WARNING: "+message+"\n");
		if(severe&&msgDialogShower!=null){
			msgDialogShower.openSimpleDialog(message,dialogType);
		}
	}
	@SuppressWarnings("unused")
	private static String warningLine(MessageConsoleStream out, BufferedWriter bwOut, boolean beginWithNewLine) {
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<100;i++){
			sb.append(WARNINGSYMBOL);
		}
		sb.append("\n");
		String warning = sb.toString();
		return warning;
	}

	public static void println(MessageConsoleStream out, BufferedWriter bwOut,Object message){
		System.out.println(message);
		printInOut(out,bwOut,message+"\n");
	}
	public static void println(MessageConsoleStream out, BufferedWriter bwOut){
		println(out,bwOut,"");
	}
	public static void print(MessageConsoleStream out, BufferedWriter bwOut,String message){
		System.out.print(message);
		printInOut(out,bwOut,message);
	}
	private static void printException(MessageConsoleStream out, BufferedWriter bwOut,Throwable e,boolean printStackTrace){
		e.printStackTrace();
		if(out!=null){
			out.println("Exception: "+e.getClass());
			out.println("Message: "+e.getMessage());
			if(printStackTrace){
				out.println("Stacktrace: ");
				for(int i=0;i<e.getStackTrace().length;i++){
					out.println("\t"+e.getStackTrace()[i].toString());
				}
			}
		}
		if(bwOut!=null){
			try {
				bwOut.write("Exception: "+e.getClass()+"\n");
				bwOut.write("Message: "+e.getMessage()+"\n");
				if(printStackTrace){
					bwOut.write("Stacktrace: \n");
					for(int i=0;i<e.getStackTrace().length;i++){
						bwOut.write("\t"+e.getStackTrace()[i].toString()+"\n");
					}
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	public static void printStackTrace(MessageConsoleStream out, BufferedWriter bwOut,Throwable e){
		printException(out, bwOut, e, true);
	}
	public static void printExceptionShort(MessageConsoleStream out, BufferedWriter bwOut,Throwable e){
		printException(out, bwOut,e, false);
	}

	/*private static void printInOut(String message){
		if(out!=null){
			out.print(message);
		}
	}
	public static void printWarning(String message){
		System.out.println(ANSI_YELLOW + " " + message + " " + ANSI_RESET);
		printInOut(message+"\n");
	}
	public static void println(Object message){
		System.out.println(message);
		printInOut(message+"\n");
	}
	public static void println(){
		println("");
	}
	public static void print(String message){
		System.out.print(message);
		printInOut(message);
	}
	public static void printStackTrace(Exception e){
		e.printStackTrace();
		if(out!=null){
			out.println("Exception: "+e.getClass());
			out.println("Message: "+e.getMessage());
			out.println("Stacktrace: ");
			for(int i=0;i<e.getStackTrace().length;i++){
				out.println("\t"+e.getStackTrace()[i].toString());
			}
		}
	}


	public static void setOutStream(MessageConsoleStream out2){
		out=out2;
	}*/
	/*public void setImporterOfSupportedNetworks(ImporterOfSupportedNetworks importer){
		this.importerOfSupportedNetworks=importer;
	}*/

	/*public static MessageConsoleStream out(){
		return out;
	}*/

	public static final boolean printInfo = true;
	public static final boolean printPartition = false;
	public static final boolean printCRN = false;
	public static final boolean verbose = false;
	public static final boolean writeReducedCRNInFile = true;
	public static final boolean writeGroupedCRNInFile = true;
	public static final boolean KEEPTRACEOFRATEEXPRESSIONS = true;
	private static String[] progressBar;

	private ICRN crn;

	public static void main(String[] args) {
		main(args,false);
	}

	public static void main(String[] args, boolean fromGUI) {		

		//System.load(filename);
		/*String property = System.getProperty("java.library.path");
		System.out.println(property);
		StringTokenizer parser = new StringTokenizer(property, ";");
		//while (parser.hasMoreTokens()) {
		//    System.err.println(parser.nextToken());
		//    }
		System.setProperty("java.library.path", "/Users/andrea/Copy/workspacextext/lib/z3:"+property);
		property = System.getProperty("java.library.path");
		System.out.println(property);*/

		/*File f = new File("./ciaociaociaociao.txt");
		System.out.println(f.getAbsolutePath());
		System.out.println(f.getPath());
		try {
			System.out.println(f.getCanonicalPath());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println(f.getParent());
		System.out.println(f.getParentFile().getParent());*/

		CommandsReader commandsReader = new CommandsReader(args,fromGUI,null,null);
		CRNReducerCommandLine cl = new CRNReducerCommandLine(commandsReader,fromGUI);
		try {
			cl.executeCommands(true,null,null);
		} catch (Exception e) {
			CRNReducerCommandLine.println(null,null,"Unhandled errors arised while executing the commands. I terminate.");
			CRNReducerCommandLine.printStackTrace(null,null,e);
		}
	}

	public CRNReducerCommandLine(CommandsReader commandsReader) {
		this(commandsReader,false);
	}

	public CRNReducerCommandLine(CommandsReader commandsReader, boolean fromGUI) {
		super(commandsReader,fromGUI);
	}

	public CRNReducerCommandLine(CommandsReader commandsReader, ICRN crn, IPartition partition) {
		this(commandsReader,crn,partition,false);
	}
	public CRNReducerCommandLine(CommandsReader commandsReader, ICRN crn, IPartition partition, boolean fromGUI) {
		this(commandsReader,fromGUI);
		this.crn=crn;
		setPartition(partition);
	}


	public ICRN getCRN(){
		return crn;
	}

	public void importModelIgnoringCommands(String command, boolean print, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException{
		importModel(command, print,true,out,bwOut);
	}

	public void importModel(String command, boolean print, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException{
		importModel(command, print,false,out,bwOut);
	}

	public BasicModelElementsCollector importBooleanModel(String command, boolean print, boolean ignoreCommands, MessageConsoleStream out, BufferedWriter bwOut) {
		BasicModelElementsCollector bMec = null;
		if(command.startsWith("importBNet(")){
			crn=null;
			partition=null;
			bMec=handleImportBNetCommand(command,out,bwOut);
		}
		return bMec;
	}
	private BasicModelElementsCollector handleImportBNetCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) {
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return null;
		}
		String fileIn=null;
		GuessPrepartitionBN guessPrep=GuessPrepartitionBN.INPUTS;
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("fileIn=>")){
				if(parameters[p].length()<="fileIn=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file from which to read. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				fileIn = parameters[p].substring("fileIn=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("guessPrep=>")){
				if(parameters[p].length()<="guessPrep=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify INPUTS or OUTPUTS for the type of guessed prepartition. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				String gp = parameters[p].substring("guessPrep=>".length(), parameters[p].length());
				if(gp.equalsIgnoreCase("outputs")) {
					guessPrep=GuessPrepartitionBN.OUTPUTS;
				}
				else if(gp.equalsIgnoreCase("outputsoneblock")) {
					guessPrep=GuessPrepartitionBN.OUTPUTSONEBLOCK;
				}
				else if(gp.equalsIgnoreCase("inputs")) {
					guessPrep=GuessPrepartitionBN.INPUTS;
				}
				else if(gp.equalsIgnoreCase("inputsoneblock")) {
					guessPrep=GuessPrepartitionBN.INPUTSONEBLOCK;
				}
			}
			else if(parameters[p].equals("")){
				continue;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return null;
			}
		}
		if(fileIn ==null ){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the file to be loaded. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return null;
		}
		LinkedHashMap<String, IUpdateFunction> parsed=null;
		try {
			//new String[]{String.valueOf(forceMassAction)},
			
			//File foldOut = new File(folderOut);
			//String foldoutPrefix = foldOut.getAbsolutePath()+File.separator;
			
			File fileInF = new File(fileIn);
			CRNReducerCommandLine.println(out,bwOut,"Loading the BNet file:\n\t"+fileIn);
			parsed = BNetParser.parseFile(fileInF);
		} catch (FileNotFoundException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"File "+fileIn+" not found.\nError message:\n"+e.getMessage(),DialogType.Error);
			return null;
		} catch (it.imt.erode.booleannetwork.bnetparser.ParseException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled parsing errors.\nError message:\n"+e.getMessage(),DialogType.Error);
			return null;
		} 
		CRNReducerCommandLine.println(out,bwOut,"Parsing succeeded.");
		
		
		//initializeUpdateFunctions(booleanNetwork,guessPrepartitionOnInputs,modelConverter.getErodeUpdateFunctions());
		BasicModelElementsCollector bMec = new BasicModelElementsCollector(guessPrep, parsed);
		
		return bMec;
	}
	
	
	public void importModel(String command, boolean print, boolean ignoreCommands, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException{
		if(command.startsWith("load(")){
			crn=null;
			partition=null;
			handleLoadCommand(command,print,ignoreCommands,out,bwOut);
		}
		else if(command.startsWith("importCRN(")||command.startsWith("load(")){
			crn=null;
			partition=null;
			handleLoadCommand(command, print, false, out,bwOut);//handleLoadCommand(command, print, true, out);
			//handleLoadCommand(command, print, out);
		}
		else if(command.startsWith("importBNG(")){
			crn=null;
			partition=null;
			handleImportBNGCommand(command,out,bwOut);
		}
		else if(command.startsWith("importKonect(")){
			crn=null;
			partition=null;
			handleImportKonectCommand(command,out,bwOut);
		}
		else if(command.startsWith("importBoolCubeSBML(")){
			crn=null;
			partition=null;
			handleImportBoolCubeSBMLCommand(command,out,bwOut);
		}
		else if(command.startsWith("importMatlab(")){
			crn=null;
			partition=null;
			handleImportMatlabODEs(command,out,bwOut);
		}
		else if(command.startsWith("exportMatlab(")){
			handleExportMatlabODEsCommand(command,out,bwOut);
		}
		else if(command.startsWith("exportCERENA(")){
			handleExportCERENACommand(command,out,bwOut);
		}
		else if(command.startsWith("exportLNA(")){
			handleExportLNACommand(command,out,bwOut);
		}
		else if(command.startsWith("exportCausalGraph(")){
			handleExportCausalGraph(command,out,bwOut);
		}
		else if(command.startsWith("exportJacobianFunction(")){
			handleExportJacobianFunctionCommand(command,out,bwOut,false);
		}
		else if(command.startsWith("exportScriptEpsCLump(")){
			handleExportJacobianFunctionCommand(command,out,bwOut,true);
		}
		else if(command.startsWith("exportScriptsEpsCLump(")){
			handleExportJacobianFunctionCommand(command,out,bwOut,true);
		}
		else if(command.startsWith("exportEpsilonBoundsScript(")){
			handleExportEpsilonBoundsScriptCommand(command,out,bwOut);
		}
		else if(command.startsWith("exportScriptSolveUCTMC(")){
			handleExportExportScriptSolveUCTMC(command,out,bwOut);
		}
		else if(command.startsWith("exportC2E2(")){
			handleExportE2C2Command(command, out, bwOut);
		}
		else if(command.startsWith("utopic(")){
			handleExportPontryaginMethodCommand(command,out,bwOut,false);
		}
		//FSE
		else if(command.startsWith("decompress(")){
			handleDecompressCommand(command,out,bwOut,false);
		}
		else if(command.startsWith("utopicOLD(")){
			handleExportPontryaginMethodCommand(command,out,bwOut,true);
		}
		else if(command.startsWith("exportCAGEScript(")){
			handleExportCAGEScriptCommand(command,out,bwOut);
		}
		else if(command.startsWith("exportPontryaginPolygonMethod(")){
			handleExportPontryaginPolygonMethodCommand(command,out,bwOut);
		}
		else if(command.startsWith("generateCME(")){
			handleGenerateCMECommand(command, out,bwOut);
		}
		else if(command.startsWith("generateLogs(")){
			handleGenerateLogsCommand(command, out,bwOut);
		}
		else if(command.startsWith("importPalomaMomentClosures(")){
			crn=null;
			partition=null;
			handleImportPalomaMomemntClosures(command, out,bwOut);
		}
		else if(command.startsWith("importMRMC(")){
			crn=null;
			partition=null;
			handleImportMRMCCommand(command,out,bwOut);
		}
		else if(command.startsWith("importSBML(")){
			crn=null;
			partition=null;
			handleImportSBMLCommand(command,out,bwOut);
		}
		else if(command.startsWith("importLinearSystemAsCSVMatrix(")){
			crn=null;
			partition=null;
			handleImportLinearSystemAsCSVMatrixCommand(command,false,out,bwOut);
		}
		else if(command.startsWith("importLinearSystemAsCCSVMatrix(")){
			crn=null;
			partition=null;
			handleImportLinearSystemAsCSVMatrixCommand(command,true,out,bwOut);
		}
		else if(command.startsWith("importAffineSystem(")){
			crn=null;
			partition=null;
			handleImportAffineSystem(command,out,bwOut);
		}
		else if(command.startsWith("importLinearSystemWithInputs(")){
			crn=null;
			partition=null;
			handleImportLinearSystemWithInputs(command, out, bwOut);
		}
		else if(command.startsWith("importBioLayout(")){
			crn=null;
			partition=null;
			handleImportBioLayout(command,out,bwOut);
		}
		else if(command.startsWith("importSpaceEx(")){
			crn=null;
			partition=null;
			handleImportSpaceEx(command,out,bwOut);
		}
		else if(command.startsWith("importUMIST(")){
			crn=null;
			partition=null;
			handleImportUMISTCommand(command,out,bwOut);
			//CRNReducerCommandLine.println(out,bwOut,command);
		}
		else if(command.startsWith("importOSU(")){
			crn=null;
			partition=null;
			handleImportOSUCommand(command,out,bwOut);
			//CRNReducerCommandLine.println(out,bwOut,command);
		}
		else if(command.startsWith("importChemKin(")){
			crn=null;
			partition=null;
			handleImportChemKinCommand(command,out,bwOut);
			//CRNReducerCommandLine.println(out,bwOut,command);
		}
		else if(command.startsWith("importLBS(")){
			crn=null;
			partition=null;
			handleImportLBSCommand(command,out,bwOut);
		}
		else{
			CRNReducerCommandLine.println(out,bwOut,"Unknown command \""+command+"\". I skip it."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut, "Type --help for usage instructions.");
			//usage();
		}
	}
	
	public void importModelsFromFolder(String command, boolean print, boolean ignoreCommands, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException{
		if(command.startsWith("importSBMLFolder(")){
			crn=null;
			partition=null;
			handleImportSBMLFolderCommand(command,out,bwOut);
		}
		else if(command.startsWith("importAndPolyCNFFolder(")){
			crn=null;
			partition=null;
			handleImportCNFFolder_AndPoly_Command(command,out,bwOut);
		}
		else if(command.startsWith("importCNFFolderAsQuantumOpt(")){
			//crn=null;
			//partition=null;
			handleImportCNF_QuantumSATOptimization_FolderCommand(command, out, bwOut);
		}
		
		else{
			CRNReducerCommandLine.println(out,bwOut,"Unknown command \""+command+"\". I skip it."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut, "Type --help for usage instructions.");
			//usage();
		}
	}
	
	public void importBooleanModelsFromFolder(String command, boolean print, boolean ignoreCommands, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException{
		if(command.startsWith("importBNetFolder(")){
			crn=null;
			partition=null;
			handleImportBNetFolderCommand(command,out,bwOut);
		}
		else if(command.startsWith("importSBMLQualFolder(")){
			crn=null;
			partition=null;
			handleImportSBMLQualFolderCommand(command,out,bwOut);
		}
		else if(command.startsWith("importCNFFolder(")){
			crn=null;
			partition=null;
			handleImportCNFFolderCommand(command,out,bwOut);
		}
		else{
			CRNReducerCommandLine.println(out,bwOut,"Unknown command \""+command+"\". I skip it."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut, "Type --help for usage instructions.");
			//usage();
		}
	}
	
	protected void handleCreateDifferentialHullCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException, IOException {
		//hullify(fileO1ut="",delta=0.1,strict=true,format="ODE/NET")
		ODEorNET crnGUIFormat = ODEorNET.ODE;
		SupportedFormats format = SupportedFormats.CRNGUI;
		String fileName = null;
		boolean strict = true;
		double delta = -1;
		boolean hasDelta=false;
		String parameters[] = CRNReducerCommandLine.getParameters(command);
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
			}
			else if(parameter.startsWith("strict=>")){
				if(parameter.length()<="strict=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if the plot has to be visualized. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				strict = Boolean.valueOf(parameter.substring("strict=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("delta=>")){
				if(parameter.length()<="delta=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the delta to use (the step) when looking for similar parameters to collapse. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				delta= Double.valueOf(parameter.substring("delta=>".length(), parameter.length()));
				hasDelta=true;
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

		CRNandPartition hullcrnAndPartition;
		try {
			hullcrnAndPartition = DifferentialHull.computeHull(crn,strict,delta,hasDelta,format,out,bwOut,crnGUIFormat,command);
		} catch (ParseException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in computing the differential Hull.");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			CRNReducerCommandLine.printStackTrace(out,bwOut, e);
			hullcrnAndPartition=null;
			return;
		}
		if(hullcrnAndPartition!=null){
			writeCRN(fileName,hullcrnAndPartition.getCRN(),hullcrnAndPartition.getPartition(),format,null,"",null,out,bwOut,crnGUIFormat,false,null,false,false);
		}

	}

	@Override
	public void executeCommands(boolean print, MessageConsoleStream out, BufferedWriter bwOut) throws IOException, InterruptedException, UnsupportedFormatException, Z3Exception {

//		if(HASTOCHECKLIBRARIES){
//			if(!librariesPresent){
//				checkLibraries(out,bwOut);
//			}
//			if(!librariesPresent){
//				return;
//			}
//		}

		String command = null;
		try{
			while(!Terminator.hasToTerminate(terminator)){
				command = commandsReader.getNextCommand();
				
				if(command.startsWith("reduceBDE")||command.startsWith("reduceFDE")){
					if(HASTOCHECKLIBRARIESZ3){
						if(!librariesPresent){
							checkLibrariesZ3(out,bwOut);
						}
						if(!librariesPresent){
							return;
						}
					}
				}

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
				else if(command.startsWith("-h")||command.startsWith("--help")){
					usageShort(fromGUI,out,bwOut);
				}
				else if(command.startsWith("-m")||command.startsWith("--man")){
					printMan(fromGUI,out,bwOut);
				}
				/*else if(command.startsWith("load(")){
				crn=null;
				partition=null;
				handleLoadCommand(command,print);
			}*/
				else if(command.startsWith("z3Metrics(")){
					handleZ3Metrics(command, out, bwOut,true);
				}
				else if(command.startsWith("onTheFlyBR(")){
					handleOnTheFlyCommandBR(command, out, bwOut,true);
				}
				else if(command.startsWith("onTheFlyFR(")){
					handleOnTheFlyCommandBR(command, out, bwOut,false);
				}
				else if(command.startsWith("onTheFlyBRRecursive(")){
					handleOnTheFlyCommandBRRecursive(command, out, bwOut);
				}
				else if(command.startsWith("approximateFDE(")){
					handleApproximationCommand(command, "forward", out, bwOut);
				}
				else if(command.startsWith("approximateBDE(")){
					handleApproximationCommand(command, "backward", out, bwOut);
				}
				else if(command.startsWith("reduceDSB(")){
					handleReduceCommand(command,updateCRN,"dsb",out,bwOut);
				}
				else if(command.startsWith("reduceFB(")){
					handleReduceCommand(command,updateCRN,"fb",out,bwOut);
				}
				else if(command.startsWith("reduceFE(")){
					handleReduceCommand(command,updateCRN,"fe",out,bwOut);
				}
				else if(command.startsWith("reduceCoRNFE(")){
					handleReduceCommand(command,updateCRN,"CoRNFE",out,bwOut);
				}
				else if(command.startsWith("reduceUncertainFE(")||command.startsWith("reduceUFE(")){
					handleReduceCommand(command,updateCRN,"ufe",out,bwOut);
				}
				else if(command.startsWith("reduceUCTMCFE(")){
					handleReduceCommand(command,updateCRN,"uctmcfe",out,bwOut);
				}
				else if(command.startsWith("reduceUSE(")){
					handleReduceCommand(command,updateCRN,"use",out,bwOut);
				}
				else if(command.startsWith("reduceBE_AAt(")){
					handleReduceCommand(command,updateCRN,"BE_AAt",out,bwOut);
				}
				else if(command.startsWith("justBEReduction(")){
					handleReduceCommand(command,updateCRN,"justBEReduction",out,bwOut);
				}
				else if(command.startsWith("reduceEpsNFB(")){
					handleReduceCommand(command,updateCRN,"enfb",out,bwOut);
				}
				else if(command.startsWith("reduceSMB(")){
					handleReduceCommand(command,updateCRN,"smb",out,bwOut);
				}
				else if(command.startsWith("reduceSE(")){
					handleReduceCommand(command,updateCRN,"se",out,bwOut);
				}
				else if(command.startsWith("reduceEMSB(")){
					handleReduceCommand(command,updateCRN,"emsb",out,bwOut);
				}
				else if(command.startsWith("reduceEFL(")){
					handleReduceCommand(command,updateCRN,"efl",out,bwOut);
				}
				else if(command.startsWith("reduceBB(")){
					handleReduceCommand(command,updateCRN,"bb",out,bwOut);
				}
				else if(command.startsWith("reduceBE(")){
					handleReduceCommand(command,updateCRN,"be",out,bwOut);
				}
				else if(command.startsWith("reduceEpsNBB(")){
					handleReduceCommand(command,updateCRN,"enbb",out,bwOut);
				}
				else if(command.startsWith("reduceBDE(")){//else if(command.startsWith("reduceEFLsmt(")){
					handleReduceCommand(command,updateCRN,"BDE",out,bwOut);//handleReduceCommand(command,updateCRN,"eflsmt");
				}
				else if(command.startsWith("reduceFDE(")){//else if(command.startsWith("reduceOOBsmt(")){
					handleReduceCommand(command,updateCRN,"FDE",out,bwOut);//handleReduceCommand(command,updateCRN,"oobsmt");
				}
				else if(command.startsWith("reduceGEFLsmt(")){
					handleReduceCommand(command,updateCRN,"geflsmt",out,bwOut);
				}
				else if(command.startsWith("curry(")){
					handleCurryCommand(command,updateCRN,out,bwOut);
				}
				else if(command.startsWith("write(")){
					handleWriteCommand(command,out,bwOut);
				}
				else if(command.startsWith("exportCRN(")){
					handleWriteCommand(command,out,bwOut);
				}
				else if(command.startsWith("exportRndPerturbedRN(")){
					handleExportRndPerturbedRN(command,out,bwOut);
				}
				/*else if(command.startsWith("importBNG(")){
				crn=null;
				partition=null;
				handleImportBNGCommand(command);
			}
			else if(command.startsWith("importBoolCubeSBML(")){
				crn=null;
				partition=null;
				handleImportBoolCubeSBMLCommand(command);
			}
			else if(command.startsWith("importMatlab(")){
				crn=null;
				partition=null;
				handleImportMatlabODEs(command);
			}
			else if(command.startsWith("importMRMC(")){
				crn=null;
				partition=null;
				handleImportMRMCCommand(command);
			}
			else if(command.startsWith("importLinearSystemAsCSVMatrix(")){
				crn=null;
				partition=null;
				handleImportLinearSystemAsCSVMatrixCommand(command,false);
			}
			else if(command.startsWith("importLinearSystemAsCCSVMatrix(")){
				crn=null;
				partition=null;
				handleImportLinearSystemAsCSVMatrixCommand(command,true);
			}*/
				else if(command.startsWith("exportMRMCCTMCWithSingleUseOfParams(")){
					handleExportMRMCCTMCWithSingleUseOfParameters(command,out,bwOut);
				}
				else if(command.startsWith("exportBNG(")){
					handleExportBNGCommand(command,out,bwOut);
				}
				else if(command.startsWith("exportMACRN(")){
					handleExportMACRNCommand(command,out,bwOut,true);
				}
				else if(command.startsWith("exportMARN(")){
					handleExportMACRNCommand(command,out,bwOut,false);
				}
				else if(command.startsWith("exportModelica(")){
					//copy it from  handleExportBNGCommand(command,out,bwOut);
					handleExportModelicaCommand(command,out,bwOut);
				}
				else if(command.startsWith("exportAffineSystem(")){
					handleExportAffineSystemCommand(command,out,bwOut);
				}
				else if(command.startsWith("exportStoichiometry(")){
					handleExportStoichiometryCommand(command, out, bwOut);
				}
				/*else if(command.startsWith("importChemKin(")){
				crn=null;
				partition=null;
				handleImportChemKinCommand(command);
				//CRNReducerCommandLine.println(out,bwOut,command);
			}*/
				else if(command.startsWith("exportFluidCompiler(")){
					handleExportFluidCompilerCommand(command,out,bwOut);
				}
				else if(command.startsWith("exportZ3(")){
					handleExportz3Command(command,out,bwOut);
				}
				else if(command.startsWith("exportSBML(")){
					handleExportSBMLCommand(command,out,bwOut);
				}
				else if(command.startsWith("exportStochKit(")){
					handleExportStochKitCommand(command,out,bwOut);
				}
				/*else if(command.startsWith("importLBS(")){
				crn=null;
				partition=null;
				handleImportLBSCommand(command);
			}*/
				else if(command.startsWith("exportLBS(")){
					handleExportLBSCommand(command,out,bwOut);
				}
				else if(command.startsWith("simulateODE(")){
					handleSimulateODECommand(command,out,bwOut);
				}
				else if(command.startsWith("simulateDAE(")){
					handleSimulateDAECommand(command,out,bwOut);
				}
				else if(command.startsWith("simulateCTMC(")){
					handleSimulateCTMCCommand(command,out,bwOut);
				}
				else if(command.startsWith("multivestaSMC(")){
					handleSMCCommand(command,out,bwOut);
				}
				else if(command.startsWith("print")){
					handlePrintCommand(out,bwOut);
				}
				else if(command.startsWith("setParam(")){
					handleSetParam(command,out,bwOut);
				}
				else if(command.startsWith("setIC(")){
					handleSetIC(command,out,bwOut);
				}
				else if(command.startsWith("exportFlyFast(")){
					handleExportFlyFastCommand(command,out,bwOut);
				}
				else if(command.startsWith("computeDifferentialHull(")){
					handleCreateDifferentialHullCommand(command,out,bwOut);
				}
				else if(command.equals("")){
				}
				else {
					importModel(command, print,out,bwOut);
				}
				/*else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown command \""+command+"\". I skip it."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				//usage();
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



	
	protected void /*SimulationSolutions*/ handleSimulateODECommand(String command, MessageConsoleStream out, BufferedWriter bwOut) throws IOException {
		//return 
		handleSimulateODECommand(command, out, bwOut,true);
	}
	protected /*SimulationSolutions*/ void handleSimulateODECommand(String command, MessageConsoleStream out, BufferedWriter bwOut,boolean print) throws IOException {
		//simulateODE({tEnd=>200,steps=>200,visualizePlot=>true});
		double tEnd=0.0;//100.0;
		int steps=100;//0
		double stepSize=0.01;
		String imageFile = null;
		String csvFile = null;
		double minStep=1.0e-8;
		double maxStep=100.0;
		double absTol=1.0e-10;//1.0e-8;//
		double relTol=1.0e-10;//1.0e-8;//
		boolean visualizeVars=true;
		boolean visualizeViews=true;
		boolean showLabels=true;
		boolean computeJacobian=false;
		double defIC=0;
		
		boolean campaign=false,campaign_p=false,campaign_i=false;
		int campaign_n=1;
		int campaign_ic=0;
		int campaign_params=0;
		
		boolean stepsSpecified=false;
		boolean stepSizeSpecified=false;
		boolean tEndSpecifified=false;
		
		boolean apache=true;
		
		boolean covariances=false;
		int i=0;
		while(i<crn.getViewExpressionsUsesCovariances().length && !covariances){
			covariances = covariances || crn.getViewExpressionsUsesCovariances()[i];
			i++;
		}
		if(covariances && !crn.isMassAction()){
			CRNReducerCommandLine.printWarning(out, bwOut, true, messageDialogShower, "(Co)Variances can currently be computed only for mass action reaction networks.\n\tThey will be ignored.",DialogType.Warning);
			covariances=false;
		}

		String parameters[] = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameters[p],out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return;
				}
			}
			else if(parameters[p].equals("")){
				continue;
			}
			else if(parameters[p].startsWith("defaultIC=>")){
				if(parameters[p].length()<="defaultIC=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the value to be used as default initial concentration. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				defIC = Double.valueOf(parameters[p].substring("defaultIC=>".length(), parameters[p].length()));
			}
			else if(parameters[p].startsWith("steps=>")){
				if(parameters[p].length()<="steps=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the number of steps. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				steps = Integer.valueOf(parameters[p].substring("steps=>".length(), parameters[p].length()));
				stepsSpecified=true;
			}
			else if(parameters[p].startsWith("tEnd=>")){
				if(parameters[p].length()<="tEnd=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the maximum simulation time. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				tEnd = Double.valueOf(parameters[p].substring("tEnd=>".length(), parameters[p].length()));
				tEndSpecifified=true;
			}
			else if(parameters[p].startsWith("stepSize=>")){
				if(parameters[p].length()<="stepSize=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the size of the steps. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				stepSize = Double.valueOf(parameters[p].substring("stepSize=>".length(), parameters[p].length()));
				stepSizeSpecified=true;
			}
			/*else if(parameters[p].startsWith("imageFile=>")){
				imageFile = parameters[p].substring(11, parameters[p].length());
			}*/
			else if(parameters[p].startsWith("csvFile=>")){
				if(parameters[p].length()<="csvFile=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write the simulation data. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				csvFile = parameters[p].substring("csvFile=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("minStep=>")){
				if(parameters[p].length()<="minStep=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the minimal step. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				minStep = Double.valueOf(parameters[p].substring("minStep=>".length(), parameters[p].length()));
			}
			else if(parameters[p].startsWith("maxStep=>")){
				if(parameters[p].length()<="maxStep=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the maximal step. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				maxStep = Double.valueOf(parameters[p].substring("maxStep=>".length(), parameters[p].length()));
			}
			else if(parameters[p].startsWith("absTol=>")){
				if(parameters[p].length()<="absTol=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the allowed absolute error. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				absTol = Double.valueOf(parameters[p].substring("absTol=>".length(), parameters[p].length()));
			}
			else if(parameters[p].startsWith("relTol=>")){
				if(parameters[p].length()<="relTol=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the allowed relative error. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				relTol = Double.valueOf(parameters[p].substring("relTol=>".length(), parameters[p].length()));
			}
			else if(parameters[p].startsWith("campaign_n=>")){
				if(parameters[p].length()<="campaign_n=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the number of simulations to perform. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				campaign=true;
				campaign_n = Integer.valueOf(parameters[p].substring("campaign_n=>".length(), parameters[p].length()));
			}
			else if(parameters[p].startsWith("campaign_IC=>")){
				if(parameters[p].length()<="campaign_IC=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the percentage perturbation to apply on the initial conditions. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				campaign_ic = Integer.valueOf(parameters[p].substring("campaign_ic=>".length(), parameters[p].length()));
				if(campaign_ic>0) {
					campaign_i=true;
				}
			}
			else if(parameters[p].startsWith("campaign_Params=>")){
				if(parameters[p].length()<="campaign_Params=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the percentage perturbation to apply on the parameters. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				campaign_params= Integer.valueOf(parameters[p].substring("campaign_params=>".length(), parameters[p].length()));
				if(campaign_params>0) {
					campaign_p=true;
				}
			}
			
			/*else if(parameters[p].startsWith("covariances=>")){
				if(parameters[p].length()<="covariances=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if computed the covariances of the means of the concentrations. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				covariances= Boolean.valueOf(parameters[p].substring("covariances=>".length(), parameters[p].length()));
			}*/
			else if(parameters[p].startsWith("visualizePlot=>")){
				if(parameters[p].length()<="visualizePlot=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if the plot has to be visualized (NO,VARS,VIEWS,VARS&VIEWS). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String param=parameters[p].substring("visualizePlot=>".length(), parameters[p].length());
				if(param.equalsIgnoreCase("NO")){
					visualizeVars=false;
					visualizeViews=false;
				}
				else if(param.equalsIgnoreCase("VARS")){
					visualizeVars=true;
					visualizeViews=false;
				}
				else if(param.equalsIgnoreCase("VIEWS")){
					visualizeVars=false;
					visualizeViews=true;
				}
				else if(param.equalsIgnoreCase("VARS&VIEWS")){
					visualizeVars=true;
					visualizeViews=true;
				}
				
				//visualizePlot = Boolean.valueOf(parameters[p].substring("visualizePlot=>".length(), parameters[p].length()));
			}
			else if(parameters[p].startsWith("library=>")){
				if(parameters[p].length()<="library=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the library to use: 'apache', or 'sundials'.");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String param=parameters[p].substring("library=>".length(), parameters[p].length());
				if(param.equalsIgnoreCase("apache")){
					apache=true;
				}
				else if(param.equalsIgnoreCase("sundials")){
					apache=false;
				}
			}
			else if(parameters[p].startsWith("computeJacobian=>")){
				if(parameters[p].length()<="computeJacobian=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if the jacobian should be computed (and provided in the output). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String computeJacobianString = parameters[p].substring("computeJacobian=>".length(), parameters[p].length());
				if(computeJacobianString.equalsIgnoreCase("true")){
					computeJacobian = true;
					if(!crn.isMassAction()){
						computeJacobian=false;
						CRNReducerCommandLine.printWarning(out, bwOut, true, messageDialogShower, "The Jacobian can currently be computed only for mass action reaction networks.\n\tThis option will be ignored: "+parameters[p],DialogType.Warning);
					}
				}
				
			}
			else if(parameters[p].startsWith("showLabels=>")){
				if(parameters[p].length()<="showLabels=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if the plot labels have to be visualized. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				showLabels = Boolean.valueOf(parameters[p].substring("showLabels=>".length(), parameters[p].length()));
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return;
			}
		}

		//int p = ((tEndSpecifified)?1:0) + ((stepSizeSpecifified)?1:0) + ((stepsSpecifified)?1:0);
		
		//if(p!=2){
		if(stepSizeSpecified && stepsSpecified){
			//CRNReducerCommandLine.println(out,bwOut,"Please, specify exactly two of the following parameters: tEnd, steps and stepSize.");
			CRNReducerCommandLine.println(out,bwOut,"Please, specify either the number of steps or the step size.");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		/*if(tEnd==0.0){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the maximum simulated time. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return null;
		}*/
		if(crn==null){
			CRNReducerCommandLine.println(out,bwOut,"Before simulating a model, it is necessary to load it. "); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		if(crn.getSpecies().size()==0){
			CRNReducerCommandLine.println(out,bwOut,"It is not possible to simulate a model with no species. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		if(crn.getReactions().size()==0){
			CRNReducerCommandLine.println(out,bwOut,"It is not possible to simulate a model with no reactions. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		if(crn.algebraicSpecies() > 0){
			CRNReducerCommandLine.println(out,bwOut,"Command simulateODE cannot be used on models with algebraic constraints. Use simulateDAE.");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		
		/*
		//By default, we use the default step size
		if(!stepsSpecifified){
			//default step size
			stepSizeSpecifified=true;
		}
		*/
		//By default, we use the default number of steps
		if(!stepSizeSpecified){
			//default step size
			stepsSpecified=true;
		}

		if(stepsSpecified && tEndSpecifified){
			if(steps>1){
				stepSize = (tEnd / (double)(steps-1));
			}
			else{
				stepSize = (tEnd / (double)steps);
			}
		}
		else if(stepsSpecified && stepSizeSpecified){
			tEnd = stepSize*steps;
		}
		else{
			steps = (int)(tEnd / (double)stepSize) + 1;
			//steps = (int)(tEnd / (double)stepSize);
		}	
		
		if(!apache) {
			if(HASTOCHECKLIBRARIESSUNDIALSCVODE){
				if(!librariesSundialsCVODEPresent){
					checkLibrariesJSundialsCVODE(out, bwOut);
				}
				if(!librariesSundialsCVODEPresent){
					CRNReducerCommandLine.println(out,bwOut,"It is not possible to simulate using JSundials because the library is not present. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
			}
		}
		
		if(campaign) {
			String ext="";
			if(csvFile!=null) {
				int lastDot=csvFile.lastIndexOf('.');
				if(lastDot>=0) {
					ext=csvFile.substring(lastDot);
					csvFile=AbstractImporter.overwriteExtensionIfEnabled(csvFile, "", true);
				}
			}
			String csvFilen=null;
			if(campaign_p) {
				for(int sim=0;sim<campaign_n;sim++) {
					ICRN crnRndPert=RandomBNG.createRndPerturbedCopy(crn, out, bwOut, campaign_params);
					if(csvFile!=null) {
						csvFilen=csvFile+"_"+sim+ext;
					}
					simulateODE(crnRndPert,tEnd, stepSize, steps, imageFile, csvFilen, visualizeVars, visualizeViews, minStep,maxStep,absTol,relTol,covariances, showLabels,out,bwOut,command,computeJacobian,defIC,print,apache);
				}
			}
			else if(campaign_i) {
				for(int sim=0;sim<campaign_n;sim++) {
					ICRN crnRndPert = CRN.copyCRN(crn, out, bwOut, false, true, true);
					RandomBNG.randomlyPerturbIC(crnRndPert,out, bwOut, campaign_ic);
					if(csvFile!=null) {
						csvFilen=csvFile+"_"+sim+ext;
					}
					simulateODE(crnRndPert,tEnd, stepSize, steps, imageFile, csvFilen, visualizeVars, visualizeViews, minStep,maxStep,absTol,relTol,covariances, showLabels,out,bwOut,command,computeJacobian,defIC,print,apache);
				}
			} 
		}
		else {
			//return 
			simulateODE(crn,tEnd, stepSize, steps, imageFile, csvFile, visualizeVars, visualizeViews, minStep,maxStep,absTol,relTol,covariances, showLabels,out,bwOut,command,computeJacobian,defIC,print,apache);
		}
	}
	
	private /*SimulationSolutions*/ void simulateODE(ICRN crnToConsider,double tEnd, double interval, int nSteps, String imagesName, String csvFile, boolean visualizeVars, boolean visualizeViews, double minStep, double maxStep, double absTol, double relTol, boolean covariances, boolean showLabels, MessageConsoleStream out, BufferedWriter bwOut, String command, boolean computeJacobian,double defIC,boolean print,boolean apache) throws IOException {
		if(print){
			if(crnToConsider.getName()!=null){
				//CRNReducerCommandLine.print(out,bwOut,"Simulating the ODEs of the model with name "+crn.getName()+" ... ");
				CRNReducerCommandLine.print(out,bwOut,"Solving ODEs of "+crnToConsider.getName()+"... ");
			}
			else{
				//CRNReducerCommandLine.print(out,bwOut,"Simulating the ODEs of the current model ... ");
				CRNReducerCommandLine.print(out,bwOut,"Solving ODEs ... ");
			}
		}
		
		long begin = System.currentTimeMillis();
		ODESolver solver = new ODESolver(crnToConsider, 0.0, tEnd, interval, nSteps, minStep, maxStep, absTol, relTol,terminator,covariances,defIC,(apache)?SOLVERLIBRARY.APACHE:SOLVERLIBRARY.CVODE);
		boolean failed=false;
		try{
			solver.solve();
		}catch(org.apache.commons.math3.exception.NumberIsTooSmallException e){
			failed=true;
			//CRNReducerCommandLine.println(out,bwOut,"failed. The model might have a finite explosion time.\nTry to decrease the simulation time.");
			CRNReducerCommandLine.printWarning(out, bwOut, true, messageDialogShower, "Solving ODEs failed. The model might have a finite explosion time.\nTry to decrease the simulation time.",true,DialogType.Warning);
			printExceptionShort(out,bwOut, e);
		} catch (NonConsistentInitialConditions e) {
			failed=true;
			//CRNReducerCommandLine.println(out,bwOut,"failed. The model might have a finite explosion time.\nTry to decrease the simulation time.");
			CRNReducerCommandLine.printWarning(out, bwOut, true, messageDialogShower, "Solving DAEs failed.\n"+e.getMessage(),true,DialogType.Warning);
			printExceptionShort(out,bwOut, e);
		}
		if(!failed &&(!Terminator.hasToTerminate(terminator))){
			long end = System.currentTimeMillis();
			double seconds = (end-begin)/1000.0;
			if(print){
				CRNReducerCommandLine.println(out,bwOut,"completed in "+String.format( CRNReducerCommandLine.MSFORMAT, (seconds))+" (s).");
			}
			/*if((!covariances) && someViewUsesCovariance()){
			//CRNReducerCommandLine.println(out,bwOut,"Warning: the views resorting to the covariances or variances have been ignored.");
			printWarning(out, bwOut,"Warning: the views resorting to the covariances or variances have been ignored.");
		}*/

			boolean drawImages = imagesName!=null;
			boolean writeCSV = csvFile!=null;

			//dog = new DataOutputHandler("ODE solutions",solver.getSolution(), true, solver.getX(), crn, true,covariances);
			double[] xVector = solver.getX();
			xVector[xVector.length-1]=tEnd;
			dog.setData(crnToConsider.getName()+" - "+"ODE solutions",solver.getSolution(), true, xVector, crnToConsider, true,covariances,computeJacobian,command);
			
			dog.setShowLabels(showLabels);
			if(visualizeVars||visualizeViews){
				if(solver.getX().length<=3){
					CRNReducerCommandLine.println(out,bwOut,"The used graphical library does not allow to draw lines basing on less than four points.");
				}
				else{
					dog.showPlots(drawImages,imagesName,visualizeVars,visualizeViews);
				}
			}
			if(writeCSV){
				dog.writeCSV(csvFile);
			}
		}
		//return 
		dog.getSimulationSolutions();
	}
	
	
	protected SimulationSolutions handleSimulateDAECommand(String command, MessageConsoleStream out, BufferedWriter bwOut) throws IOException {
		return handleSimulateDAECommand(command, out, bwOut,true);
	}
	protected SimulationSolutions handleSimulateDAECommand(String command, MessageConsoleStream out, BufferedWriter bwOut,boolean print) throws IOException {
		//simulateODE({tEnd=>200,steps=>200,visualizePlot=>true});
		double tEnd=0.0;//100.0;
		int steps=100;//0
		double stepSize=0.01;
		String imageFile = null;
		String csvFile = null;
		//double minStep=1.0e-8;
		//double maxStep=100.0;
		double absTol=1.0e-10;//1.0e-8;//
		double relTol=1.0e-10;//1.0e-8;//
		boolean visualizeVars=true;
		boolean visualizeViews=true;
		boolean showLabels=true;
		//boolean computeJacobian=false;
		double defIC=0;
		
		boolean stepsSpecifified=false;
		boolean stepSizeSpecifified=false;
		boolean tEndSpecifified=false;
		
		//boolean apache=true;
		
		//boolean covariances=false;
		/*
		int i=0;
		while(i<crn.getViewExpressionsUsesCovariances().length && !covariances){
			covariances = covariances || crn.getViewExpressionsUsesCovariances()[i];
			i++;
		}
		if(covariances && !crn.isMassAction()){
			CRNReducerCommandLine.printWarning(out, bwOut, true, messageDialogShower, "(Co)Variances can currently be computed only for mass action reaction networks.\n\tThey will be ignored.",DialogType.Warning);
			covariances=false;
		}
		*/

		String parameters[] = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return null;
		}
		
		if( crn.algebraicSpecies() == 0 )
		{
			CRNReducerCommandLine.println(out,bwOut,"Warning: There are no algebraic constraints. You can replace simulateDAE with simulateODE ");
		}
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameters[p],out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return null;
				}
			}
			else if(parameters[p].equals("")){
				continue;
			}
			else if(parameters[p].startsWith("defaultIC=>")){
				if(parameters[p].length()<="defaultIC=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the value to be used as default initial concentration. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				defIC = Double.valueOf(parameters[p].substring("defaultIC=>".length(), parameters[p].length()));
			}
			else if(parameters[p].startsWith("steps=>")){
				if(parameters[p].length()<="steps=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the number of steps. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				steps = Integer.valueOf(parameters[p].substring("steps=>".length(), parameters[p].length()));
				stepsSpecifified=true;
			}
			else if(parameters[p].startsWith("tEnd=>")){
				if(parameters[p].length()<="tEnd=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the maximum simulation time. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				tEnd = Double.valueOf(parameters[p].substring("tEnd=>".length(), parameters[p].length()));
				tEndSpecifified=true;
			}
			else if(parameters[p].startsWith("stepSize=>")){
				if(parameters[p].length()<="stepSize=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the size of the steps. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				stepSize = Double.valueOf(parameters[p].substring("stepSize=>".length(), parameters[p].length()));
				stepSizeSpecifified=true;
			}
			/*else if(parameters[p].startsWith("imageFile=>")){
				imageFile = parameters[p].substring(11, parameters[p].length());
			}*/
			else if(parameters[p].startsWith("csvFile=>")){
				if(parameters[p].length()<="csvFile=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write the simulation data. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				csvFile = parameters[p].substring("csvFile=>".length(), parameters[p].length());
			}
			/*else if(parameters[p].startsWith("minStep=>")){
				if(parameters[p].length()<="minStep=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the minimal step. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				minStep = Double.valueOf(parameters[p].substring("minStep=>".length(), parameters[p].length()));
			}
			else if(parameters[p].startsWith("maxStep=>")){
				if(parameters[p].length()<="maxStep=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the maximal step. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				maxStep = Double.valueOf(parameters[p].substring("maxStep=>".length(), parameters[p].length()));
			}*/
			else if(parameters[p].startsWith("absTol=>")){
				if(parameters[p].length()<="absTol=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the allowed absolute error. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				absTol = Double.valueOf(parameters[p].substring("absTol=>".length(), parameters[p].length()));
			}
			else if(parameters[p].startsWith("relTol=>")){
				if(parameters[p].length()<="relTol=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the allowed relative error. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				relTol = Double.valueOf(parameters[p].substring("relTol=>".length(), parameters[p].length()));
			}
			/*else if(parameters[p].startsWith("covariances=>")){
				if(parameters[p].length()<="covariances=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if computed the covariances of the means of the concentrations. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				covariances= Boolean.valueOf(parameters[p].substring("covariances=>".length(), parameters[p].length()));
			}*/
			else if(parameters[p].startsWith("visualizePlot=>")){
				if(parameters[p].length()<="visualizePlot=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if the plot has to be visualized (NO,VARS,VIEWS,VARS&VIEWS). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				String param=parameters[p].substring("visualizePlot=>".length(), parameters[p].length());
				if(param.equalsIgnoreCase("NO")){
					visualizeVars=false;
					visualizeViews=false;
				}
				else if(param.equalsIgnoreCase("VARS")){
					visualizeVars=true;
					visualizeViews=false;
				}
				else if(param.equalsIgnoreCase("VIEWS")){
					visualizeVars=false;
					visualizeViews=true;
				}
				else if(param.equalsIgnoreCase("VARS&VIEWS")){
					visualizeVars=true;
					visualizeViews=true;
				}
				
				//visualizePlot = Boolean.valueOf(parameters[p].substring("visualizePlot=>".length(), parameters[p].length()));
			}
			/*else if(parameters[p].startsWith("library=>")){
				if(parameters[p].length()<="library=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the library to use: 'apache', or 'sundials'.");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				String param=parameters[p].substring("library=>".length(), parameters[p].length());
				if(param.equalsIgnoreCase("apache")){
					apache=true;
				}
				else if(param.equalsIgnoreCase("sundials")){
					apache=false;
				}
			}*/
			/*else if(parameters[p].startsWith("computeJacobian=>")){
				if(parameters[p].length()<="computeJacobian=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if the jacobian should be computed (and provided in the output). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				String computeJacobianString = parameters[p].substring("computeJacobian=>".length(), parameters[p].length());
				if(computeJacobianString.equalsIgnoreCase("true")){
					computeJacobian = true;
					if(!crn.isMassAction()){
						computeJacobian=false;
						CRNReducerCommandLine.printWarning(out, bwOut, true, messageDialogShower, "The Jacobian can currently be computed only for mass action reaction networks.\n\tThis option will be ignored: "+parameters[p],DialogType.Warning);
					}
				}
				
			}*/
			else if(parameters[p].startsWith("showLabels=>")){
				if(parameters[p].length()<="showLabels=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if the plot labels have to be visualized. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				showLabels = Boolean.valueOf(parameters[p].substring("showLabels=>".length(), parameters[p].length()));
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return null;
			}
		}

		//int p = ((tEndSpecifified)?1:0) + ((stepSizeSpecifified)?1:0) + ((stepsSpecifified)?1:0);
		
		//if(p!=2){
		if(stepSizeSpecifified && stepsSpecifified){
			//CRNReducerCommandLine.println(out,bwOut,"Please, specify exactly two of the following parameters: tEnd, steps and stepSize.");
			CRNReducerCommandLine.println(out,bwOut,"Please, specify either the number of steps or the step size.");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return null;
		}
		/*if(tEnd==0.0){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the maximum simulated time. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return null;
		}*/
		if(crn==null){
			CRNReducerCommandLine.println(out,bwOut,"Before simulating a model, it is necessary to load it. "); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return null;
		}
		if(crn.getSpecies().size()==0){
			CRNReducerCommandLine.println(out,bwOut,"It is not possible to simulate a model with no species. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return null;
		}
		if(crn.getReactions().size()==0){
			CRNReducerCommandLine.println(out,bwOut,"It is not possible to simulate a model with no reactions. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return null;
		}
		
		/*
		//By default, we use the default step size
		if(!stepsSpecifified){
			//default step size
			stepSizeSpecifified=true;
		}
		*/
		//By default, we use the default number of steps
		if(!stepSizeSpecifified){
			//default step size
			stepsSpecifified=true;
		}

		if(stepsSpecifified && tEndSpecifified){
			if(steps>1){
				stepSize = (tEnd / (double)(steps-1));
			}
			else{
				stepSize = (tEnd / (double)steps);
			}
		}
		else if(stepsSpecifified && stepSizeSpecifified){
			tEnd = stepSize*steps;
		}
		else{
			steps = (int)(tEnd / (double)stepSize) + 1;
			//steps = (int)(tEnd / (double)stepSize);
		}	
		

		if(HASTOCHECKLIBRARIESSUNDIALSIDA){
			if(!librariesSundialsIDAPresent){
				checkLibrariesJSundialsIDA(out, bwOut);
			}
			if(!librariesSundialsIDAPresent){
				CRNReducerCommandLine.println(out,bwOut,"It is not possible to simulate using JSundials because the library is not present. ");
				CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
				return null;
			}
		}

		
		
		return simulateDAE(tEnd, stepSize, steps, imageFile, csvFile, visualizeVars, visualizeViews, /*minStep,maxStep,*/absTol,relTol/*,covariances*/, showLabels,out,bwOut,command,/*computeJacobian,*/defIC,print/*,apache*/);
	}
	
	private SimulationSolutions simulateDAE(double tEnd, double interval, int nSteps, String imagesName, String csvFile, boolean visualizeVars, boolean visualizeViews/*, double minStep, double maxStep*/, double absTol, double relTol/*, boolean covariances*/, boolean showLabels, MessageConsoleStream out, BufferedWriter bwOut, String command, /*boolean computeJacobian,*/double defIC,boolean print/*,boolean apache*/) throws IOException {
		if(print){
			if(crn.getName()!=null){
				//CRNReducerCommandLine.print(out,bwOut,"Simulating the ODEs of the model with name "+crn.getName()+" ... ");
				CRNReducerCommandLine.print(out,bwOut,"Solving DAEs of "+crn.getName()+"... ");
			}
			else{
				//CRNReducerCommandLine.print(out,bwOut,"Simulating the ODEs of the current model ... ");
				CRNReducerCommandLine.print(out,bwOut,"Solving DAEs ... ");
			}
		}
		
		long begin = System.currentTimeMillis();
		ODESolver solver = new ODESolver(crn, 0.0, tEnd, interval, nSteps, -1, -1, absTol, relTol,terminator,false,defIC,SOLVERLIBRARY.IDA);
		boolean failed=false;
		try{
			solver.solve();
		}catch(org.apache.commons.math3.exception.NumberIsTooSmallException e){
			failed=true;
			//CRNReducerCommandLine.println(out,bwOut,"failed. The model might have a finite explosion time.\nTry to decrease the simulation time.");
			CRNReducerCommandLine.printWarning(out, bwOut, true, messageDialogShower, "Solving DAEs failed. The model might have a finite explosion time.\nTry to decrease the simulation time.",true,DialogType.Warning);
			printExceptionShort(out,bwOut, e);
		} catch (NonConsistentInitialConditions e) {
			failed=true;
			//CRNReducerCommandLine.println(out,bwOut,"failed. The model might have a finite explosion time.\nTry to decrease the simulation time.");
			CRNReducerCommandLine.printWarning(out, bwOut, true, messageDialogShower, "Solving DAEs failed.\n"+e.getMessage(),true,DialogType.Warning);
			printExceptionShort(out,bwOut, e);
		}
		
		if(!failed &&(!Terminator.hasToTerminate(terminator))){
			long end = System.currentTimeMillis();
			double seconds = (end-begin)/1000.0;
			if(print){
				CRNReducerCommandLine.println(out,bwOut,"completed in "+String.format( CRNReducerCommandLine.MSFORMAT, (seconds))+" (s).");
			}
			/*if((!covariances) && someViewUsesCovariance()){
			//CRNReducerCommandLine.println(out,bwOut,"Warning: the views resorting to the covariances or variances have been ignored.");
			printWarning(out, bwOut,"Warning: the views resorting to the covariances or variances have been ignored.");
		}*/

			boolean drawImages = imagesName!=null;
			boolean writeCSV = csvFile!=null;

			//dog = new DataOutputHandler("ODE solutions",solver.getSolution(), true, solver.getX(), crn, true,covariances);
			double[] xVector = solver.getX();
			xVector[xVector.length-1]=tEnd;
			dog.setData(crn.getName()+" - "+"DAE solutions",solver.getSolution(), true, xVector, crn, true,false,false,command);
			
			dog.setShowLabels(showLabels);
			if(visualizeVars||visualizeViews){
				if(solver.getX().length<=3){
					CRNReducerCommandLine.println(out,bwOut,"The used graphical library does not allow to draw lines basing on less than four points.");
				}
				else{
					dog.showPlots(drawImages,imagesName,visualizeVars,visualizeViews);
				}
			}
			if(writeCSV){
				dog.writeCSV(csvFile);
			}
		}
		return dog.getSimulationSolutions();
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	

	private void handleSimulateCTMCCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) throws IOException {
		//simulateCTMC({tEnd=>10000,steps=>10,method=>ssa,repeats=>100,imageFile=>fileNameWhereToPlot,csvFile=>fileNameWhereToSaveCSVValues})
		double tEnd=0;//100.0;
		int steps=100;
		//int simulationSteps=steps-1;
		double stepSize=0.01;
		int repeats=1;
		String method = "nextReaction";//"ssa";
		String imageFile = null;
		String csvFile = null;
		boolean visualizeVars=true;
		boolean visualizeViews=true;
		boolean showLabels=true;
		double defIC=0;
		
		boolean stepsSpecifified=false;
		boolean stepSizeSpecifified=false;
		boolean tEndSpecifified=false;

		String parameters[] = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		if(crn.algebraicSpecies() > 0){
			CRNReducerCommandLine.println(out,bwOut,"Command simulateCTMC cannot be used on models with algebraic constraints. Use simulateDAE.");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}

		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameters[p],out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return;
				}
			}
			else if(parameters[p].equals("")){
				continue;
			}
			/*else if(parameters[p].startsWith("tEnd=>")){
				if(parameters[p].length()<="tEnd=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the maximum simulation time. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				tEnd = Double.valueOf(parameters[p].substring("tEnd=>".length(), parameters[p].length()));
			}*/
			else if(parameters[p].startsWith("steps=>")){
				if(parameters[p].length()<="steps=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the number of steps. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				steps = Integer.valueOf(parameters[p].substring("steps=>".length(), parameters[p].length()));
				stepsSpecifified=true;
				//simulationSteps=steps-1;
			}
			else if(parameters[p].startsWith("tEnd=>")){
				if(parameters[p].length()<="tEnd=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the maximum simulation time. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				tEnd = Double.valueOf(parameters[p].substring("tEnd=>".length(), parameters[p].length()));
				tEndSpecifified=true;
			}
			else if(parameters[p].startsWith("stepSize=>")){
				if(parameters[p].length()<="stepSize=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the size of the steps. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				stepSize = Double.valueOf(parameters[p].substring("stepSize=>".length(), parameters[p].length()));
				stepSizeSpecifified=true;
			}
			else if(parameters[p].startsWith("defaultIC=>")){
				if(parameters[p].length()<="defaultIC=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the value to be used as default initial concentration. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				defIC = Double.valueOf(parameters[p].substring("defaultIC=>".length(), parameters[p].length()));
			}
			/*else if(parameters[p].startsWith("steps=>")){
				if(parameters[p].length()<="steps=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the number of observed steps. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				//The number of points in the output (one per 0, plus one per simulation step)
				steps = Integer.valueOf(parameters[p].substring("steps=>".length(), parameters[p].length()));
				simulationSteps=steps-1;
			}*/
			else if(parameters[p].startsWith("repeats=>")){
				if(parameters[p].length()<="repeats=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the number of simulations to be performed. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				repeats = Integer.valueOf(parameters[p].substring("repeats=>".length(), parameters[p].length()));
			}
			else if(parameters[p].startsWith("method=>")){
				method = parameters[p].substring("method=>".length(), parameters[p].length());
				if(!( 	method.equalsIgnoreCase("ssa")|| 
						method.equalsIgnoreCase("ssa+")||
						method.equalsIgnoreCase("nextReaction")||
						method.equalsIgnoreCase("tauLeapingAbs")||
						method.equalsIgnoreCase("tauLeapingRelProp")||
						method.equalsIgnoreCase("tauLeapingRelPop")||
						method.equalsIgnoreCase("maximalTimeStep")
						)){
					CRNReducerCommandLine.println(out,bwOut,"Unknown simulation method \""+method+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
					return;
				}
			}
			/*else if(parameters[p].startsWith("imageFile=>")){
				imageFile = parameters[p].substring(11, parameters[p].length());
			}*/
			else if(parameters[p].startsWith("csvFile=>")){
				if(parameters[p].length()<="csvFile=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write the simulation data. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				csvFile = parameters[p].substring("csvFile=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("visualizePlot=>")){
				if(parameters[p].length()<="visualizePlot=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if the plot has to be visualized (NO,VARS,VIEWS,VARS&VIEWS). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String param=parameters[p].substring("visualizePlot=>".length(), parameters[p].length());
				if(param.equalsIgnoreCase("NO")){
					visualizeVars=false;
					visualizeViews=false;
				}
				else if(param.equalsIgnoreCase("VARS")){
					visualizeVars=true;
					visualizeViews=false;
				}
				else if(param.equalsIgnoreCase("VIEWS")){
					visualizeVars=false;
					visualizeViews=true;
				}
				else if(param.equalsIgnoreCase("VARS&VIEWS")){
					visualizeVars=true;
					visualizeViews=true;
				}
				
				//visualizePlot = Boolean.valueOf(parameters[p].substring("visualizePlot=>".length(), parameters[p].length()));
			}
			else if(parameters[p].startsWith("showLabels=>")){
				if(parameters[p].length()<="showLabels=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if the plot labels have to be visualized. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				showLabels = Boolean.valueOf(parameters[p].substring("showLabels=>".length(), parameters[p].length()));
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return;
			}
		}

		//int p = ((tEndSpecifified)?1:0) + ((stepSizeSpecifified)?1:0) + ((stepsSpecifified)?1:0);

		//if(p!=2){
		if(stepSizeSpecifified && stepsSpecifified){
			//CRNReducerCommandLine.println(out,bwOut,"Please, specify exactly two of the following parameters: tEnd, steps and stepSize.");
			CRNReducerCommandLine.println(out,bwOut,"Please, specify either the number of steps or the step size.");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		
		/*if(tEnd==0.0){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the maximum simulated time. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}*/
		if(steps<=1.0){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify a number of observed steps greater than one. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		if(crn==null){
			CRNReducerCommandLine.println(out,bwOut,"Before simulating a model it is necessary to load it. "); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		if(crn.getSpecies().size()==0){
			CRNReducerCommandLine.println(out,bwOut,"It is not possible to simulate a model with no species. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		if(crn.getReactions().size()==0){
			CRNReducerCommandLine.println(out,bwOut,"It is not possible to simulate a model with no reactions. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}

		/*
		//By default, we use the default step size
		if(!stepsSpecifified){
			//default step size
			stepSizeSpecifified=true;
		}
		*/
		//By default, we use the default number of steps
		if(!stepSizeSpecifified){
			//default step size
			stepsSpecifified=true;
		}

		/*double interval;
		interval = tEnd / (double)(simulationSteps);*/

		if(stepsSpecifified && tEndSpecifified){
			if(steps>1){
				stepSize = (tEnd / (double)(steps-1));
			}
			else{
				stepSize = (tEnd / (double)steps);
			}
		}
		else if(stepsSpecifified && stepSizeSpecifified){
			tEnd = stepSize*steps;
		}
		else{
			steps = (int)(tEnd / (double)stepSize) + 1;
		}
		
		simulateCTMC(tEnd, steps, stepSize, method, repeats, imageFile, csvFile, visualizeVars,visualizeViews,showLabels,out,bwOut,command,defIC);
	}


	private void simulateCTMC(double maxTime, int stepsInOutput, double interval,  String method, int repeats, String imagesName, String csvFile, boolean visualizeVars, boolean visualizeViews, boolean showLabels, MessageConsoleStream out, BufferedWriter bwOut, String command,double defaultIC) throws IOException  {
		FernNetworkFromLoadedCRN net = new FernNetworkFromLoadedCRN(crn,defaultIC);

		Simulator sim = null;
		if(method.equalsIgnoreCase("ssa")){
			sim= new GillespieSimple(net);
		}
		else if(method.equalsIgnoreCase("ssa+")){
			sim= new GillespieEnhanced(net);
		}
		else if(method.equalsIgnoreCase("nextReaction")){
			sim= new GibsonBruckSimulator(net);
		}
		else if(method.equalsIgnoreCase("tauLeapingAbs")){
			sim= new TauLeapingAbsoluteBoundSimulator(net);
		}
		else if(method.equalsIgnoreCase("tauLeapingRelProp")){
			sim= new TauLeapingRelativeBoundSimulator(net);
		}
		else if(method.equalsIgnoreCase("tauLeapingRelPop")){
			sim= new TauLeapingSpeciesPopulationBoundSimulator(net);
		}
		else if(method.equalsIgnoreCase("maximalTimeStep")){
			sim= new HybridMaximalTimeStep(net);
		}
		
		if(crn.getName()!=null){
			CRNReducerCommandLine.print(out,bwOut,"Stochastic simulation of "+crn.getName()+"... ");
		}
		else{
			CRNReducerCommandLine.print(out,bwOut,"Stochastic simulation... ");
		}

		boolean drawImages = imagesName!=null;
		boolean writeCSV = csvFile!=null;
		boolean haveViews=false;
		if(crn.getViewNames()!=null && crn.getViewNames().length!=0 && !allViewUseCovariance()){
			haveViews=true;
		}
		if(ignoreViews==true){
			haveViews=false;
		}

		String plottedSpecies[] = net.getIndexToSpeciesId();
		AmountIntervalObserverAVGBugFixed meanObserver = new AmountIntervalObserverAVGBugFixed(sim,interval,plottedSpecies);
		meanObserver.setStepsInOutputAndMaxTime(stepsInOutput, maxTime);
		IntervalObserverAVGBugFixed amountObs = (IntervalObserverAVGBugFixed) sim.addObserver(meanObserver);
		//GnuPlot amountgp = new GnuPlot();
		//amountgp.setDefaultStyle("with lines");

		//IntervalObserverAVGBugFixed viewsObs = null;
		//GnuPlot viewsgp = null;

		/*if(haveViews){
			ViewsIntervalObserver viewsObvesrver = new ViewsIntervalObserver(sim, interval, crn.getViewNames(), crn.getViewExpressionsSupportedByMathEval(), crn.getViewExpressionsUsesCovariances(), crn.getMath());
			viewsObvesrver.setStepsInOutputAndMaxTime(stepsInOutput, maxTime);
			viewsObs = (IntervalObserverAVGBugFixed) sim.addObserver(viewsObvesrver);
		}*/
		
		//Tally thinkers = new Tally();

		String[] progrBar= getProgressBar();
		long begin = System.currentTimeMillis();
		CRNReducerCommandLine.println(out,bwOut,"\n\tPercentage of simulations performed:");
		CRNReducerCommandLine.println(out,bwOut,"\t "+progrBar[0]);
		int nextPercentage=0;
		for(int simulation=1;simulation<=repeats;simulation++){
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			//sim.start(maxTime);
			DefaultController timeController = new ControllerWithTerminator(maxTime,terminator);
			timeController.setTime(maxTime);
			sim.start(timeController);
			double percentage = simulation*100 / repeats;
			if( percentage>= nextPercentage+10){
				nextPercentage=((int)((percentage+5)/10))*10;
				CRNReducerCommandLine.println(out,bwOut,"\t "+progrBar[nextPercentage/10]);
			}
			
			/*
			// compute thinkers
			double[][] observation = thinkerObs.getRecentData();
			double[] finalSituation = observation[simTime];
			double thinker = 0;
			for (int j = 1; j < finalSituation.length; j++) {
				thinker = thinker + finalSituation[j];
			}
			thinkers.add(thinker / net.totalPopulation);
			
			if (thinkers.numberObs() > 3) {

				double[] ciThinker = new double[2];
				thinkers.confidenceIntervalStudent(0.95, ciThinker);
				percCiThinker = ciThinker[1] / ciThinker[0];

				double[] ciMobile = new double[2];
				mobility.confidenceIntervalStudent(0.95, ciMobile);
				percCiMobile = ciMobile[1] / ciMobile[0];

				if (percCiThinker < 0.05 || percCiMobile < 0.05) {
					break;
				}
				System.out.printf("c.i. thinker : %3.2f%%\n",
						percCiThinker * 100);
				System.out.printf("c.i. mobile  : %3.2f%%\n",
						percCiMobile * 100);
				System.out.printf("    avg think: %3.2f%%\n",
						thinkers.average() * 100);
			}
			*/
			
			/*System.out.println();
			printMatrix(amountObs.getAvgLog());
			System.out.println();
			printMatrix(amountObs.getRecentData());*/
			
		}
		//CRNReducerCommandLine.println(out,bwOut,);
		long end = System.currentTimeMillis();
		//CRNReducerCommandLine.println(out,bwOut,"Time necessary for the stochastic simulation: "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+" (s).");
		CRNReducerCommandLine.println(out,bwOut,"\tcompleted in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+" (s).");
		/*if(someViewUsesCovariance()){
			//CRNReducerCommandLine.println(out,bwOut,"Warning: the views resorting to the covariances or variances have been ignored.");
			printWarning(out, bwOut,"Warning: the views resorting to the covariances or variances have been ignored.");
		}*/

		if(!Terminator.hasToTerminate(terminator)){
			double[] x;
			double[][] results;
			double[][] resultsView = null;
			int pointsInTheData=amountObs.getAvgLog()[0].length;
			//CRNReducerCommandLine.println(out,bwOut,"Actual points in the data: "+pointsInTheData);
			if(pointsInTheData<stepsInOutput){
				x = Arrays.copyOf(amountObs.getAvgLog()[0], stepsInOutput);
				double currentX=amountObs.getAvgLog()[0][amountObs.getAvgLog()[0].length-1];
				for(int i=amountObs.getAvgLog()[0].length;i<x.length;i++){
					currentX=currentX+interval;
					x[i]=currentX;
				}
				results = new double[amountObs.getAvgLog().length][];
				results[0]=Arrays.copyOf(x, x.length);
				/*if(haveViews){
					resultsView= new double[viewsObs.getAvgLog().length][];
					resultsView[0]=Arrays.copyOf(x, x.length);
				}*/
				for(int i=1;i<results.length;i++){
					results[i]=Arrays.copyOf(amountObs.getAvgLog()[i], x.length);
					Arrays.fill(results[i], amountObs.getAvgLog()[i].length, results[i].length, amountObs.getAvgLog()[i][amountObs.getAvgLog()[i].length-1]);
				}
				/*if(haveViews){
					for(int i=1;i<resultsView.length;i++){
						resultsView[i]=Arrays.copyOf(viewsObs.getAvgLog()[i], x.length);
						Arrays.fill(resultsView[i], viewsObs.getAvgLog()[i].length, resultsView[i].length, viewsObs.getAvgLog()[i][viewsObs.getAvgLog()[i].length-1]);
					}
				}*/
			}
			else{
				x = Arrays.copyOf(amountObs.getAvgLog()[0], stepsInOutput);
				results = amountObs.getAvgLog();
				
				//old
				//double[][] resultsViewOld = viewsObs.getAvgLog();
				//resultsView = viewsObs.getAvgLog();
				if(haveViews){
					CRNReducerCommandLine.print(out,bwOut,"Computing views... ");
					long beginViews = System.currentTimeMillis();
					resultsView = computeViews(sim, x, results);
					long endViews = System.currentTimeMillis();
					CRNReducerCommandLine.println(out,bwOut,"completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((endViews-beginViews)/1000.0) )+" (s).");
				}
			}

			String minimalDescription = "Stochastic simulation (repeats="+repeats+")";
			//dog = new DataOutputHandler(minimalDescription,results, resultsView, false, x, crn,true);
			dog.setData(crn.getName()+" - "+minimalDescription,results, resultsView, false, x, crn,true,command);

			dog.setShowLabels(showLabels);
			if(visualizeVars||visualizeViews){
				if(x.length<=3){
					CRNReducerCommandLine.println(out,bwOut,"The used graphical library does not allow to draw lines basing on less than four points.");
				}
				else{
					dog.showPlots(drawImages,imagesName,visualizeVars,visualizeViews);
				}
			}

			if(writeCSV){
				dog.writeCSV(csvFile);
			}
		}


		//This is the code if we want to use the gnuplot library 
		/*amountgp.getCommands().clear();
		amountgp.addCommand("set title \"PINO time course after "+repeats+" repeats\"");
		amountObs.toGnuplot(amountgp);
		amountgp.plot();
		amountgp.saveImage(new File(String.format("pino%03d.png",repeats)));*/
		//amountgp.saveData(new File(String.format("pino%03d.txt",repeats)));
		/*for(String data : amountgp.getData()){
			CRNReducerCommandLine.println(out,bwOut,data);
		}*/


		/*if(haveViews){
			viewsgp.getCommands().clear();
			viewsgp.addCommand("set title \""+imagesName+" time course after "+repeats+" repeats");
			viewsObs.toGnuplot(viewsgp);
			if(drawImages||writeCSV){
				viewsgp.plot();
			}
		}

		if(drawImages){
			amountgp.saveImage(new File(String.format(imagesName+"%03d.png",repeats)));
			if(haveViews){
				viewsgp.saveImage(new File(String.format(imagesName+"View%03d.png",repeats)));
			}

		}
		if(writeCSV){
			amountgp.saveData(new File(String.format(csvFile +"%03d.cdat",repeats)));
			if(haveViews){
				viewsgp.saveData(new File(String.format(csvFile+"%03d.vdat",repeats)));
			}
		}

		amountgp.clearData();
		if(haveViews){
			viewsgp.clearData();
		}*/

	}
	private double[][] computeViews(Simulator sim, double[] x, double[][] results) {
		double[][] resultsView;
		MathEval mathEval=new MathEval(crn.getMath());
		String[] viewExpressions = crn.getViewExpressionsSupportedByMathEval();
		boolean[] viewExpressionsUsesCovariances = crn.getViewExpressionsUsesCovariances();
		resultsView=new double[viewExpressions.length+1][];
		resultsView[0]=x;
		for(int view=0;view<viewExpressions.length;view++){
			int posOfViewInResults = view+1;
			resultsView[posOfViewInResults]=new double[x.length];
		}
		for(int step=0;step<x.length;step++){
			for(int view=0;view<viewExpressions.length;view++){
				int posOfViewInResults = view+1;
				if(viewExpressionsUsesCovariances[view]){
					//We currently compute the covariances only for the odes.
				}
				else{
					//update species amounts in mathEval
					for (String speciesNameSupportedByMathEval : mathEval.getVariablesWithin(viewExpressions[view])) {
						int pos = sim.getNet().getSpeciesByName(speciesNameSupportedByMathEval);
						//If pos is -1, then the string is not a species but a parameter, and I do not have to update it.
						if(pos!=-1){
							double val = results[pos+1][step];
							mathEval.setVariable(speciesNameSupportedByMathEval, val);
						}
					}
					double val = mathEval.evaluate(viewExpressions[view]);
					resultsView[posOfViewInResults][step]=val;
				}
			}
		}
		return resultsView;
	}

	/*private boolean someViewUsesCovariance() {
		boolean someViewUsesCovariance=false;
		for(int i=0;i<crn.getViewExpressionsUsesCovariances().length;i++){
			if(crn.getViewExpressionsUsesCovariances()[i]){
				someViewUsesCovariance=true;
				break;
			}
		}
		return someViewUsesCovariance;
	}*/

	/*private void printMatrix(double[][] matrix) {
		for(int i=0;i<matrix.length;i++){
			for(int j=0;j<matrix[i].length;j++){
				System.out.print(" "+matrix[i][j]);
			}
			System.out.println();
		}
	}*/
	public boolean allViewUseCovariance() {
		boolean allViewUsesCovariance=true;
		for(int i=0;i<crn.getViewExpressionsUsesCovariances().length;i++){
			if(!crn.getViewExpressionsUsesCovariances()[i]){
				allViewUsesCovariance=false;
				break;
			}
		}
		return allViewUsesCovariance;
	}


	private void handleSMCCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) throws IOException {
		double maxTime=0;//100.0;
		int steps=100;
		int simulationSteps=1;
		String method = "nextReaction";//"ssa";
		String csvFile = null;
		boolean visualizePlot=true;
		boolean showLabels=true;
		String model="";
		double alpha=0.0;
		double delta=0.0;
		String parallelism="1";	

		String query=FERNState.allPopulationsMultiQuaTExExpressionFileNameWithoutExtension;
		

		String parameters[] = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for(int p=0;p<parameters.length;p++){
//			if(parameters[p].startsWith("fileIn=>")){
//				boolean loadingSuccessful = invokeLoad(parameters[p],out,bwOut);
//				model=parameters[p].substring("fileIn=>".length());
//				if(!loadingSuccessful){
//					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
//					return;
//				}
//			}
			if(parameters[p].equals("")){
				continue;
			}
			else if(parameters[p].startsWith("maxTime=>")){
				if(parameters[p].length()<="maxTime=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the maximum simulation time. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				maxTime = Double.valueOf(parameters[p].substring("maxTime=>".length(), parameters[p].length()));
			}
			else if(parameters[p].startsWith("parallelism=>")){
				if(parameters[p].length()<="parallelism=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the degree of parallelism. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				parallelism = parameters[p].substring("parallelism=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("alpha=>")){
				if(parameters[p].length()<="alpha=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the alpha component of the confidence interval. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				alpha= Double.valueOf(parameters[p].substring("alpha=>".length(), parameters[p].length()));
			}
			else if(parameters[p].startsWith("delta=>")){
				if(parameters[p].length()<="delta=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the alpha component of the confidence interval. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				delta= Double.valueOf(parameters[p].substring("delta=>".length(), parameters[p].length()));
			}
			else if(parameters[p].startsWith("steps=>")){
				if(parameters[p].length()<="steps=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the number of observed steps. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				//The number of points in the output (one per 0, plus one per simulation step)
				steps = Integer.valueOf(parameters[p].substring("steps=>".length(), parameters[p].length()));
				simulationSteps=steps-1;
			}
			/*else if(parameters[p].startsWith("repeats=>")){
				if(parameters[p].length()<="repeats=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the number of simulations to be performed. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				repeats = Integer.valueOf(parameters[p].substring("repeats=>".length(), parameters[p].length()));
			}*/
			else if(parameters[p].startsWith("method=>")){
				method = parameters[p].substring("method=>".length(), parameters[p].length());
				if(!( 	method.equalsIgnoreCase("ssa")|| 
						method.equalsIgnoreCase("ssa+")||
						method.equalsIgnoreCase("nextReaction")||
						method.equalsIgnoreCase("tauLeapingAbs")||
						method.equalsIgnoreCase("tauLeapingRelProp")||
						method.equalsIgnoreCase("tauLeapingRelPop")||
						method.equalsIgnoreCase("maximalTimeStep")
						)){
					CRNReducerCommandLine.println(out,bwOut,"Unknown simulation method \""+method+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
					return;
				}
			}
			else if(parameters[p].startsWith("csvFile=>")){
				if(parameters[p].length()<="csvFile=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write the analysis data. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				csvFile = parameters[p].substring("csvFile=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("query=>")){
				if(parameters[p].length()<="query=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write the analysis data. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				query = parameters[p].substring("query=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("model=>")){
				if(parameters[p].length()<="model=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to find the model. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				model = parameters[p].substring("model=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("visualizePlot=>")){
				if(parameters[p].length()<="visualizePlot=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if the plot has to be visualized. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				visualizePlot = Boolean.valueOf(parameters[p].substring("visualizePlot=>".length(), parameters[p].length()));
			}
			else if(parameters[p].startsWith("showLabels=>")){
				if(parameters[p].length()<="showLabels=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if the plot labels have to be visualized. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				showLabels = Boolean.valueOf(parameters[p].substring("showLabels=>".length(), parameters[p].length()));
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return;
			}
		}

		if(model.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the model to analyse. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		if(maxTime==0.0){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the maximum simulated time. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		if(alpha<=0.0||alpha>=1.0){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify a value in (0,1) for the alpha component of the required confidence interval. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		if(delta<=0.0){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify a positive value for the delta component of the required confidence interval. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		if(crn==null){
			CRNReducerCommandLine.println(out,bwOut,"Before analysing with statistical model checking a model it is necessary to load it. "); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		if(crn.getSpecies().size()==0){
			CRNReducerCommandLine.println(out,bwOut,"It is not possible to analyse with statistical model checking a model with no species. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		if(crn.getReactions().size()==0){
			CRNReducerCommandLine.println(out,bwOut,"It is not possible to analyse with statistical model checking a model with no reactions. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}

		double interval;
		interval = maxTime / (double)(simulationSteps);

		if(query.equals(FERNState.allPopulationsMultiQuaTExExpressionFileNameWithoutExtension) ||
				query.equals(FERNState.allPopulationsAndViewsMultiQuaTExExpressionFileNameWithoutExtension) ||
				query.equals(FERNState.allViewsMultiQuaTExExpressionFileNameWithoutExtension)){
			if(steps<=1.0){
				CRNReducerCommandLine.println(out,bwOut,"Please, specify a number of observed steps greater than one. ");
				CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
				return;
			}
			try{
				FERNState.createMultiQuaTExQuery(crn,maxTime,interval,query);
			}catch(IOException e){
				CRNReducerCommandLine.println(out,bwOut,"Problems while writing the MultiQuaTEx expression. ");
				CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
				return;
			}
			query=query+".quatex";
		}
		smc(model, maxTime, steps, interval, alpha, delta, method,csvFile, visualizePlot,parallelism,query,showLabels,out,bwOut,command);
	}

	private void smc(String model, double maxTime, int steps, double interval, double alpha, double delta, String method, String csvFile, boolean visualizePlot, String parallelism, String query, boolean showLabels, MessageConsoleStream out, BufferedWriter bwOut, String command) throws IOException  {

		int maxSimulations=0;
		int seedOfTheSeeds=-1;
		String multiVeStAServerList=parallelism;//"serverlist1";
		String otherParams = "--maxTime "+maxTime + " --method "+method;
		String[] argsMultiVeStA = parametersForMultiVeStAFERNClient(model, query, multiVeStAServerList, 
				otherParams , maxSimulations, alpha, delta, seedOfTheSeeds);

		CRNReducerCommandLine.println(out,bwOut,"Launching the MultiVeStA client."); // to evaluate the obtained MultiQuaTEx expression against the CRN.");
		//CRNReducerCommandLine.println(out,bwOut,ANSI_CYAN);
		long begin = System.currentTimeMillis();
		InfoMultiQuery result = vesta.NewVesta.invokeClient(argsMultiVeStA);
		long end = System.currentTimeMillis();
		CRNReducerCommandLine.println(out,bwOut,ANSI_RESET);
		CRNReducerCommandLine.println(out,bwOut,"MultiVeStA analysis completed. Time necessary "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+" (s). Total simulations "+result.getNumberOfSimulations());

		String minimalDescription = "Statistical Model Checking. Query: "+query;//
		//dog = new DataOutputHandler(minimalDescription,crn, result,alpha,delta);
		dog.setData(crn.getName()+" - "+minimalDescription,crn, result,alpha,delta,command);
		
		dog.setShowLabels(showLabels);

		boolean writeCSV = csvFile!=null;
		if(visualizePlot){
			if(result.getNumberOfX()<=3){
				CRNReducerCommandLine.println(out,bwOut,"The used graphical library does not allow to draw lines basing on less than four points.");
			}
			else{
				dog.showPlots(false,null,true,false);
			}
		}

		if(writeCSV){
			dog.writeCSV(csvFile);
		}

	}

	public static String[] parametersForMultiVeStAFERNClient(String CRNFileName, String multiQuaTExExpressionFileName, String serverAddressesFileName, 
			String optionalFERNParameters, int maxSimulations, double alpha, double delta, int seedOfTheSeeds){
		/*if(CRNFileName.startsWith("./")){
			CRNFileName=CRNFileName.substring(2);
		}
		if(multiQuaTExExpressionFileName.startsWith("./")){
			multiQuaTExExpressionFileName=multiQuaTExExpressionFileName.substring(2);
		}*/
		/*
		String argsMultiVeStAStr = "-sd it.imt.erode.smc.multivesta.FERNState -m " + CRNFileName 
				+ " -f "+ multiQuaTExExpressionFileName  
				+ " -l " + serverAddressesFileName 
				+ " -a " + alpha + " -sots " + seedOfTheSeeds
				+ " -d1 " + delta
				+ " -vp false" 
				+ " -bs 30"
				+ " -verbose false"
				+ " -osws ONESTEP"
				//+ " -osws WHOLESIMULATION"
				//+ " -jn " + CRNReducerCommandLine.NameOfTheJar 
				+ " -distr "+ false //true
				;  

		if(maxSimulations > 0){
			argsMultiVeStAStr = argsMultiVeStAStr + "-ms " + maxSimulations; 
		}
		*/
		ArrayList<String> params = new ArrayList<String>();
		params.add("-sd");
		params.add("it.imt.erode.smc.multivesta.FERNState");
		params.add("-m");
		params.add(CRNFileName);
		
		params.add("-f");
		params.add(multiQuaTExExpressionFileName);  
		params.add("-l");
		params.add(""+serverAddressesFileName);
		params.add("-a");
		params.add(""+alpha);
		params.add("-sots");
		params.add(""+seedOfTheSeeds);
		params.add("-d1");
		params.add(""+delta);
		params.add("-vp");
		params.add("false"); 
		params.add("-bs");
		params.add("30");
		params.add("-verbose");
		params.add("false");
		params.add("-osws");
		params.add("ONESTEP");
		//+ " -osws WHOLESIMULATION"
		//+ " -jn " + CRNReducerCommandLine.NameOfTheJar 
		params.add("-distr");
		params.add("false");
		if(maxSimulations > 0){
			//argsMultiVeStAStr = argsMultiVeStAStr + "-ms " + maxSimulations;
			params.add("-ms");
			params.add(""+maxSimulations);
		}
		

		String[] argsMultiVeStA = params.toArray(new String[]{});//.trim().split(" ");
		String handledOptionalFERNParameters = handleOptionalParameters(optionalFERNParameters);
		if (handledOptionalFERNParameters.equals(""))
			return argsMultiVeStA;
		else {
			String[] argsMultiVeStaWithOptionalFERNParameters = new String[argsMultiVeStA.length + 2];
			for(int i = 0; i < argsMultiVeStA.length;i++){
				argsMultiVeStaWithOptionalFERNParameters[i] = argsMultiVeStA[i]; 
			}
			argsMultiVeStaWithOptionalFERNParameters[argsMultiVeStaWithOptionalFERNParameters.length - 2] = "-o";
			argsMultiVeStaWithOptionalFERNParameters[argsMultiVeStaWithOptionalFERNParameters.length - 1] =		handledOptionalFERNParameters;
			return argsMultiVeStaWithOptionalFERNParameters;	
		}
	}

	private static final String handleOptionalParameters(String optionalFERNParameters){
		if(optionalFERNParameters == null || optionalFERNParameters.equals(""))
			return "";
		return " \"" + optionalFERNParameters + "\"";
	} 


	private static String[] getProgressBar(){
		if(progressBar==null){
			progressBar=new String[11];
			progressBar[0]="[          ]";
			progressBar[1]="[*         ]";
			progressBar[2]="[**        ]";
			progressBar[3]="[***       ]";
			progressBar[4]="[****      ]";
			progressBar[5]="[*****     ]";
			progressBar[6]="[******    ]";
			progressBar[7]="[*******   ]";
			progressBar[8]="[********  ]";
			progressBar[9]="[********* ]";
			progressBar[10]="[**********]";
		}
		return progressBar;
	}
	
	private void handleCurryCommand(String command, boolean updateCRN,MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		LinkedHashSet<String> paramsToCurry=new LinkedHashSet<String>();
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		boolean print=true;
		boolean singleoutParams=true;
		boolean preserveUserPartion=false;
		
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for (String parameter : parameters) {
			if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("paramsToCurry=>")){
				if(parameter.length()<="paramsToCurry=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the set of parameters to curry. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String[] par = parameter.substring("paramsToCurry=>".length(), parameter.length()).split(";");
				for(int i=0;i<par.length;i++){
					paramsToCurry.add(par[i]);
				}
			}
			else if(parameter.startsWith("singleoutParams=>")){
				if(parameter.length()<="singleoutParams=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if the new model should have a user-defined partition consisting of one block per curried parameter (true), or one block only containing all curried parameters (false). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				singleoutParams = Boolean.valueOf(parameter.substring("singleoutParams=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("preserveUserPartion=>")){
				if(parameter.length()<="preserveUserPartion=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if the new model should preserve the blocks from the user-defined partition (true) or if they should be discarded (false). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				preserveUserPartion = Boolean.valueOf(parameter.substring("preserveUserPartion=>".length(), parameter.length()));
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameter+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return;
			}
		}
		
		
		if(paramsToCurry==null || paramsToCurry.isEmpty()){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the set of parameters to curry. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		else if(crn==null){
			CRNReducerCommandLine.println(out,bwOut,"Before applying curry to a model it is necessary to load it. "); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		else {
			long begin = System.currentTimeMillis();
			if(print){
				CRNReducerCommandLine.print(out,bwOut,"Applying currying ...");
			}
			boolean addParams=false;
			CRNandPartition curried = applyCurry(crn, partition, paramsToCurry,singleoutParams,addParams,preserveUserPartion);
			crn=curried.getCRN();
			partition=curried.getPartition();
			long end = System.currentTimeMillis();
			if(print){
				CRNReducerCommandLine.println(out,bwOut," completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+" (s).");
				CRNReducerCommandLine.println(out,bwOut,"\tCurried CRN: "+crn.getParameters().size()+" parameters, "+crn.getSpecies().size()+" species, "+crn.getReactions().size()+" reactions.");

				CRNReducerCommandLine.println(out,bwOut,"\nThe current model is updated with the reduced one.");
			}
		}

	}
	
	private void handleOnTheFlyCommandBRRecursive(String command, MessageConsoleStream out, BufferedWriter bwOut) {
		String parameters[] = CRNReducerCommandLine.getParameters(command);

		//String fileName=null;
		LinkedHashSet<Pair> Q=null;
		LinkedHashSet<Pair> Qbar=null;
		boolean QminusQbar=false;

		HashMap<String, ISpecies> speciesNameToSpecies=new HashMap<String, ISpecies>(crn.getSpecies().size());
		for (ISpecies species : crn.getSpecies()) {
			speciesNameToSpecies.put(species.getName(), species);
		}

		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}

		for (String parameter : parameters) {
			if(parameter.equals("")){
				continue;
			}
//			else if(parameter.startsWith("fileOut=>")){
//				if(parameter.length()<="fileOut=>".length()){
//					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write. ");
//					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
//					return;
//				}
//				fileName = parameter.substring("fileOut=>".length(), parameter.length());
//			}
			else if(parameter.startsWith("Q=>")){
				if(parameter.length()<="Q=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the pairs in Q. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String[] pairs = parameter.substring("Q=>".length()+1, parameter.length()-1).split(";");
				Q = parsePairs(speciesNameToSpecies, pairs);
//				Q = new LinkedHashSet<Pair>(pairs.length);
//				for(int i=0;i<pairs.length;i++){
//					pairs[i]=pairs[i].substring(1, pairs[i].length()-1);
//					String[] pair = pairs[i].split(":");
//					Q.add(new Pair(speciesNameToSpecies.get(pair[0]),speciesNameToSpecies.get(pair[1])));
//				}
			}
			else if(parameter.startsWith("Qbar=>")){
				if(parameter.length()<="Qbar=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the pairs in Qbar. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String[] pairs = parameter.substring("Qbar=>".length()+1, parameter.length()-1).split(";");
				Qbar = parsePairs(speciesNameToSpecies, pairs);
			}
			else if(parameter.startsWith("QminusQbar=>")){
				if(parameter.length()<="QminusQbar=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify whether you want to explicitly remove Qbar from Q. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String boolValue = parameter.substring("QminusQbar=>".length(), parameter.length());
				if(boolValue.equalsIgnoreCase("true")) {
					QminusQbar=true;
				}
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameter+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return;
			}
		}

		if(Q==null) {
			CRNReducerCommandLine.println(out,bwOut,"No pairs in Q have been provided.");
			return;
		}
		if(Qbar ==null) {
			Qbar = new LinkedHashSet<Pair>(0);
		}
		
		if(QminusQbar) {
			Q.removeAll(Qbar);
		}
		
		String qpairs=(Q.size()==1)?" pair":" pairs";
		String qbarpairs=(Qbar.size()==1)?" pair":" pairs";
		
		long begin;
		ArrayList<Pair> br;
		
		OnTheFlyBRRecursive brComputerRecursive = new OnTheFlyBRRecursive();
		begin = System.currentTimeMillis();
		CRNReducerCommandLine.println(out,bwOut,"Computing on-the-fly BR recursively for\n\tQ ("+Q.size()+qpairs+")="+Q+"\n\tQbar ("+Qbar.size()+qbarpairs+")="+Qbar);
		br = brComputerRecursive.onTheFlyBR(crn, Q, Qbar, out, bwOut, terminator, messageDialogShower);
		printOnTheFlyBR(out, bwOut, begin, brComputerRecursive, br,"BR",null);
		
//		CRNReducerCommandLine.println(out,bwOut,"Computing on-the-fly BR iteratively for\n\tQ ("+Q.size()+qpairs+")="+Q+"\n\tQbar ("+Qbar.size()+qbarpairs+")="+Qbar);
//		begin = System.currentTimeMillis();
//		OnTheFlyBRIterative brComputerIterative = new OnTheFlyBRIterative();
//		//OnTheFlyBRIterativeDouble brComputerIterative = new OnTheFlyBRIterativeDouble();
//		br = brComputerIterative.onTheFlyBR(crn, Q, Qbar, out, bwOut, terminator, messageDialogShower);
//		printOnTheFlyBR(out, bwOut, begin, brComputerIterative, br);
		
		

	}
	
	public double[][] handleZ3Metrics(String command, MessageConsoleStream out, BufferedWriter bwOut, boolean verbose) {
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		
		double C = -1;
		double lambda=-1;
		String csvFile = null;
		
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return null;
		}
		for (String parameter : parameters) {
			if(parameter.equals("")){
				continue;
			}
//			else if(parameter.startsWith("fileOut=>")){
//				if(parameter.length()<="fileOut=>".length()){
//					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write. ");
//					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
//					return;
//				}
//				fileName = parameter.substring("fileOut=>".length(), parameter.length());
//			}
			else if(parameter.startsWith("C=>")){
				if(parameter.length()<="C=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify largest C. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				C = Double.valueOf(parameter.substring("C=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("lambda=>")){
				if(parameter.length()<="lambda=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify lambda. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				lambda = Double.valueOf(parameter.substring("lambda=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("csvFile=>")){
				if(parameter.length()<="csvFile=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write the simulation data. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				csvFile = parameter.substring("csvFile=>".length(), parameter.length());
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameter+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return null;
			}
		}
		
		if(lambda<=0 || C <=0) {
			CRNReducerCommandLine.println(out,bwOut,"Please specify positive C and lambda.");
			return null;
		}
		if(csvFile==null) {
			CRNReducerCommandLine.println(out,bwOut,"Please a path for the CSV file where to store the computed metricsspecify positive C and lambda.");
			return null;
		}
		
		//long begin = System.currentTimeMillis();
		//boolean succeeded=true;
		//String smtTime=null;
		//List<Double> smtChecksTime = null;
		
		SMTMetrics metricsComputer = new SMTMetrics(C,lambda);
		try {
			double[][] metrics = metricsComputer.computeZ3Metrics(crn, partition, verbose, out, bwOut, verbose, terminator, messageDialogShower);
			return metrics;
		}catch(Z3Exception | ArithmeticException | IOException e) {
			//String message="The command "+command +" failed:\n"+e.toString();//+e.getMessage();
			//CRNReducerCommandLine.println(out, bwOut, message);
			//CRNReducerCommandLine.printWarning(out, bwOut, true, messageDialogShower, message, true,DialogType.Error);
			//succeeded=false;
			CRNReducerCommandLine.println(out, bwOut, "The metrics computation failed.");
		}
		return null;
	}
	
	private void handleOnTheFlyCommandBR(String command, MessageConsoleStream out, BufferedWriter bwOut, boolean backward) {
		String parameters[] = CRNReducerCommandLine.getParameters(command);

		//String fileName=null;
		LinkedHashSet<Pair> Q=null;
		LinkedHashSet<Pair> Qbar=null;
		boolean QminusQbar=false;
		UpToType upTo=UpToType.NO;
		boolean avoidUnbalancedPairs=true;
		String computeOnlyPartition="false";
		String reducedFileName=null;
		String csvFile=null;

		HashMap<String, ISpecies> speciesNameToSpecies=new HashMap<String, ISpecies>(crn.getSpecies().size());
		for (ISpecies species : crn.getSpecies()) {
			speciesNameToSpecies.put(species.getName(), species);
		}

		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}

		for (String parameter : parameters) {
			if(parameter.equals("")){
				continue;
			}
//			else if(parameter.startsWith("fileOut=>")){
//				if(parameter.length()<="fileOut=>".length()){
//					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write. ");
//					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
//					return;
//				}
//				fileName = parameter.substring("fileOut=>".length(), parameter.length());
//			}
			else if(parameter.startsWith("Q=>")){
				if(parameter.length()<="Q=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the pairs in Q. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String[] pairs = parameter.substring("Q=>".length()+1, parameter.length()-1).split(";");
				Q = parsePairs(speciesNameToSpecies, pairs);
//				Q = new LinkedHashSet<Pair>(pairs.length);
//				for(int i=0;i<pairs.length;i++){
//					pairs[i]=pairs[i].substring(1, pairs[i].length()-1);
//					String[] pair = pairs[i].split(":");
//					Q.add(new Pair(speciesNameToSpecies.get(pair[0]),speciesNameToSpecies.get(pair[1])));
//				}
			}
			else if(parameter.startsWith("Qbar=>")){
				if(parameter.length()<="Qbar=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the pairs in Qbar. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String[] pairs = parameter.substring("Qbar=>".length()+1, parameter.length()-1).split(";");
				Qbar = parsePairs(speciesNameToSpecies, pairs);
			}
			else if(parameter.startsWith("QminusQbar=>")){
				if(parameter.length()<="QminusQbar=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify whether you want to explicitly remove Qbar from Q. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String boolValue = parameter.substring("QminusQbar=>".length(), parameter.length());
				if(boolValue.equalsIgnoreCase("true")) {
					QminusQbar=true;
				}
			}
			else if(parameter.startsWith("upTo=>")){
				if(parameter.length()<="upTo=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the required upTo. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String upToStr = parameter.substring("upTo=>".length(), parameter.length());
				if(upToStr!=null) {
					upTo=UpToFactory.stringToUpToType(upToStr);
				}
			}
			else if(parameter.startsWith("avoidUnbalancedPairs=>")){
				if(parameter.length()<="avoidUnbalancedPairs=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify whether or not you want to explicitly disallow unbalanced pairs in the tp solver. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String avoidUnbalancedPairsStr = parameter.substring("avoidUnbalancedPairs=>".length(), parameter.length());
				if(avoidUnbalancedPairsStr.equalsIgnoreCase("false")) {
					avoidUnbalancedPairs=false;
				}
			}
			else if(parameter.startsWith("reducedFile=>")){
				if(parameter.length()<="reducedFile=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write the reduced model. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				reducedFileName = parameter.substring("reducedFile=>".length(), parameter.length());
			}
			else if(parameter.startsWith("csvFile=>")){
				if(parameter.length()<="csvFile=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write the simulation data. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				csvFile = parameter.substring("csvFile=>".length(), parameter.length());
			}
			else if(parameter.startsWith("computeOnlyPartition=>")){
				if(parameter.length()<="computeOnlyPartition=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if or not only the partition has to be computed (without thus reducing the model). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				computeOnlyPartition = parameter.substring("computeOnlyPartition=>".length(), parameter.length());
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameter+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return;
			}
		}

		if(upTo==null) {
			CRNReducerCommandLine.println(out,bwOut,"Please specify a valid upTo option: "+Arrays.toString(UpToType.values()));
			return;
		}
		if(Q==null) {
			CRNReducerCommandLine.println(out,bwOut,"No pairs in the query have been provided.");
			return;
		}
		if(Qbar ==null) {
			Qbar = new LinkedHashSet<Pair>(0);
		}
		
		if(QminusQbar) {
			Q.removeAll(Qbar);
		}
		
		long begin,beginBegin;
		
		boolean printIntermediate=false;
		
		ArrayList<Pair> R;
		if(backward) {

			//		OnTheFlyBRRecursive brComputerRecursive = new OnTheFlyBRRecursive();
			//		begin = System.currentTimeMillis();
			//		CRNReducerCommandLine.println(out,bwOut,"Computing on-the-fly BR recursively for\n\tQ ("+Q.size()+qpairs+")="+Q+"\n\tQbar ("+Qbar.size()+qbarpairs+")="+Qbar);
			//		br = brComputerRecursive.onTheFlyBR(crn, Q, Qbar, out, bwOut, terminator, messageDialogShower);
			//		printOnTheFlyBR(out, bwOut, begin, brComputerRecursive, br);

			
			begin = System.currentTimeMillis();
			beginBegin=begin;
			OnTheFlyBRIterative brComputerIterative = new OnTheFlyBRIterative();
			//OnTheFlyBRIterativeDouble brComputerIterative = new OnTheFlyBRIterativeDouble();
			MutableLong pre_comp = new MutableLong(0);
			R = brComputerIterative.onTheFlyBR(crn, Q, Qbar, upTo,avoidUnbalancedPairs,out, bwOut, terminator, messageDialogShower,printIntermediate,pre_comp);
			if(R!=null)
				printOnTheFlyBR(out, bwOut, begin, brComputerIterative, R,"BR",pre_comp);
		}
		else {
			begin = System.currentTimeMillis();
			beginBegin=begin;
			OnTheFlyFRIterative frComputer = new OnTheFlyFRIterative();
			R = frComputer.onTheFlyFR(crn, Q, Qbar, upTo,avoidUnbalancedPairs, out, bwOut, terminator, messageDialogShower,false,printIntermediate);
			if(R!=null)
				printOnTheFlyBR(out, bwOut, begin, frComputer, R,"FR",null);
		}
		
		boolean considerEquivalenceClosure=false;
		if((reducedFileName!=null && reducedFileName.length()>0) || (csvFile!=null && csvFile.length()>0) ) {
			considerEquivalenceClosure=true;
		}
		
		if(R!=null&&considerEquivalenceClosure) {
			String otf="FR";
			String reduction="FE";
			String reductionName="FE";
			if(backward) {
				reduction="BE";
				otf="BR";
				reductionName="BE";
			}
			CRNReducerCommandLine.print(out, bwOut,"Computing the "+reduction+" obtained by closing the "+otf+" up to equivalence... ");
			begin = System.currentTimeMillis();
			IPartition obtainedPartition = OnTheFlyBRIterative.closeUpToEquivalence(crn,R);
			long end = System.currentTimeMillis();
			CRNReducerCommandLine.println(out,bwOut,"completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");
			
			
			boolean updateCRN=false;
			String csvSMTTimeFileName=null;
			String partitionInfoFileName=null;
			String sameICFileName=null;
			String typeOfGroupedFile=null;
			String groupedFileName=null;
			boolean print=true;
			boolean sumReductionAlgorithm=true;
			boolean writeReducedCRN = reducedFileName!=null && !reducedFileName.equals("");
			boolean writeGroupedCRN=false;
			boolean writeSameICCRN=false;
			String originalCRNShort=crn.toStringShort();
			String icWarning=null;
			String reducedModelName=crn.getName()+reductionName;
			List<Double> smtChecksTime=null;
			IPartition obtainedPartitionOfParams=null;
			String smtTime=null;
			CRNReducerCommandLine.print(out, bwOut,"\nPrinting overall information and computing reduced model if necessary");
			try {
				printReductionInfoAndComputeReducedModel(updateCRN, reduction, out, bwOut, reducedFileName, 
						partitionInfoFileName, groupedFileName, sameICFileName, csvSMTTimeFileName, typeOfGroupedFile, 
						computeOnlyPartition, csvFile, print, sumReductionAlgorithm, writeReducedCRN, writeGroupedCRN, 
						writeSameICCRN, crn, null, originalCRNShort, obtainedPartition, new CRNandPartition(crn, obtainedPartition), 
						icWarning, reductionName, reducedModelName, smtChecksTime, obtainedPartitionOfParams, beginBegin, smtTime, end,null,BigDecimal.ZERO,null);
			} catch (UnsupportedFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public void printOnTheFlyBR(MessageConsoleStream out, BufferedWriter bwOut, long begin,
			IOnTheFly RComputer, ArrayList<Pair> R, String name, MutableLong pre_comp) {
		if(R!=null) {
			Collections.sort(R);
			long end = System.currentTimeMillis();
			CRNReducerCommandLine.print(out,bwOut,"\tCompleted in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");
			if(pre_comp!=null) {
				CRNReducerCommandLine.println(out,bwOut," - "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-pre_comp.getValue())/1000.0) )+ " (s) ignoring pre-computation");
			}
			else {
				CRNReducerCommandLine.println(out,bwOut,"");
			}
			String p="pairs";
			if(R.size()==1) {
				p="pair";
			}
			CRNReducerCommandLine.println(out, bwOut, "The computed "+name+" has "+ R.size() +" "+p+":\n"+R);
			ArrayList<Pair> brNoIdentity = new ArrayList<Pair>(); 
			for(Pair pair : R) {
				if(!pair.getFirst().equals(pair.getSecond())) {
					brNoIdentity.add(pair);
				}
			}
			p="pairs";
			if(brNoIdentity.size()==1) {
				p="pair";
			}
			CRNReducerCommandLine.println(out, bwOut, "Dropping identity   "+ brNoIdentity.size() +" "+p+":\n"+brNoIdentity);
			
			CRNReducerCommandLine.println(out, bwOut,
					"\tThis required solving "+ RComputer.getTranspProblemsSolved()+ " transportation problems"+" (and "+RComputer.getTranspProblemsUnbalancedSkept()+" unbalanced implicitly solved)\n"
//					"\n\tNumber of update()  : "+RComputer.getNumberOfUpdates()+
//					"\n\tNumber of tp.solve(): "+RComputer.getTranspProblemsSolved()+
//					"\n\tNumber of expand()  : "+RComputer.getNumberOfExpand()+
//					"\n\tPairs added to adj  : "+RComputer.getNumberOfPairsAddedToAdj()+
//					"\n\tPairs updated in adj: "+RComputer.getNumberOfPairsUpdatedInAdj()+
//					"\n"
					);
		}
	}
	public LinkedHashSet<Pair> parsePairs(HashMap<String, ISpecies> speciesNameToSpecies, String[] pairs) {
		LinkedHashSet<Pair> allPairs;
		if(pairs.length==1 && (pairs[0].equals("[ALL]")||pairs[0].equals("ALL"))) {
			allPairs = new LinkedHashSet<Pair>(crn.getSpeciesSize()*crn.getSpeciesSize());
			for(int i=0;i<crn.getSpecies().size();i++) {
				for(int j=0;j<crn.getSpecies().size();j++) {
					Pair p = new Pair(crn.getSpecies().get(i),crn.getSpecies().get(j));
					allPairs.add(p);
				}
			}
		}
		else if(pairs.length==1 && (pairs[0].equals("[NONE]")||pairs[0].equals("NONE"))) {
			allPairs = new LinkedHashSet<Pair>(0);
		} 
		else {
			allPairs = new LinkedHashSet<Pair>(pairs.length);
			for(int i=0;i<pairs.length;i++){
				pairs[i]=pairs[i].substring(1, pairs[i].length()-1);
				String[] pair = pairs[i].split(":");
				ISpecies first =speciesNameToSpecies.get(pair[0]);
				if(first==null) {
					first=crn.getSpecies().get(Integer.valueOf(pair[0]));
				}
				ISpecies second =speciesNameToSpecies.get(pair[1]);
				if(second==null) {
					second=crn.getSpecies().get(Integer.valueOf(pair[1]));
				}
				allPairs.add(new Pair(first,second));
			}
		}
		return allPairs;
	}
	
	private void handleExportRndPerturbedRN(String command, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		//exportPerturbedCRN({file=>outputfileName.ode,from=>from,to=>to,step=>step})
		
		if(crn==null) {
			CRNReducerCommandLine.println(out,bwOut,"No model loaded.");
			return;
		}
		
		ICRN crnToConsider=crn;
		IPartition partitionToConsider=partition;
		
		if(!crnToConsider.isMassAction()){
			CRNandPartition abc = MatlabODEPontryaginExporter.computeRNEncoding(crn, crn.getReactions(), out, bwOut, partition,true);
			crnToConsider=abc.getCRN();
			partitionToConsider=abc.getPartition();
			//CRNReducerCommandLine.println(out,bwOut,"This command should be invoked on mass-action reaction networks.");
			//return;
		}
		
		int from=0,to=30,step=5;
	
		ODEorNET crnGUIFormat = ODEorNET.RN;
		
		String fileName = null;
		SupportedFormats format = null;
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		boolean rnEncoding=false;
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
				format = (fromGUI)?SupportedFormats.CRNGUI:SupportedFormats.CRN;
				/*writeCRN(fileName,crn,partition,format,"","",out,bwOut);
				break;*/
			}
			else if(parameter.startsWith("from=>")){
				if(parameter.length()<="from=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the minimum perturbation. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				from = Integer.valueOf(parameter.substring("from=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("to=>")){
				if(parameter.length()<="to=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the maximum perturbation. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				to = Integer.valueOf(parameter.substring("to=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("step=>")){
				if(parameter.length()<="step=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the step to increase the perturbation. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				step = Integer.valueOf(parameter.substring("step=>".length(), parameter.length()));
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
		
		int dot=fileName.lastIndexOf('.');
		int sep=fileName.lastIndexOf(File.separator);
		String ext="";
		if(dot!=-1) {
			if(dot>sep) {
				ext=fileName.substring(dot);
				fileName=AbstractImporter.overwriteExtensionIfEnabled(fileName, "", true);
			}
		}
		
		for(int maxPerturb=from;maxPerturb<= to;maxPerturb+=step) {
			String fileNamePert=fileName+"_"+maxPerturb+ext;
			ICRN perturbedCRN;
			try {
				perturbedCRN=RandomBNG.createRndPerturbedCopy(crnToConsider, out, bwOut, maxPerturb);
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in perturbing the CRN. I terminate.");
				CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
				e.printStackTrace();
				return;
			}
			writeCRN(fileNamePert,perturbedCRN,partitionToConsider,format,null,"",null,out,bwOut,crnGUIFormat,rnEncoding,null,false,false);
		}
		
		
	}
	
	private void handleWriteCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		//write({file=>outputfileName.crn})
		
		LinkedHashSet<String> paramsToCurry=new LinkedHashSet<String>();
		
		ODEorNET crnGUIFormat = null;
		if(crn!=null){
			crnGUIFormat = crn.getMdelDefKind();
		}
		
		
		String fileName = null;
		SupportedFormats format = null;
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		boolean rnEncoding=false;
		boolean deterministicCorrection=false;
		boolean originalNames=false;
		boolean resetParameters=false;
		//boolean euler=false;
		//boolean euler_write_tau=false;
		EULER euler = EULER.NO;
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for (String parameter : parameters) {
			if(parameter.startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameter,out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return;
				}
			}
			else if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("fileOut=>")){
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				fileName = parameter.substring("fileOut=>".length(), parameter.length());
				format = (fromGUI)?SupportedFormats.CRNGUI:SupportedFormats.CRN;
				/*writeCRN(fileName,crn,partition,format,"","",out,bwOut);
				break;*/
			}
			else if(parameter.startsWith("paramsToCurry=>")){
				if(parameter.length()<="paramsToCurry=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the set of parameters to curry. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String[] par = parameter.substring("paramsToCurry=>".length(), parameter.length()).split(";");
				for(int i=0;i<par.length;i++){
					paramsToCurry.add(par[i]);
				}
			}
			else if(parameter.startsWith("deterministicCorrection=>")){
				if(parameter.length()<="deterministicCorrection=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if deterministic correction should be applied. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String det = parameter.substring("deterministicCorrection=>".length(), parameter.length());
				if(det.equalsIgnoreCase("true")) {
					deterministicCorrection=true;
				}
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
			else if(parameter.startsWith("resetParameters=>")){
				if(parameter.length()<="resetParameters=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if parameters shall be reset. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String det = parameter.substring("resetParameters=>".length(), parameter.length());
				if(det.equalsIgnoreCase("true")) {
					resetParameters=true;
				}
			}
			else if(parameter.startsWith("format=>")){
				if(parameter.length()<="format=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if the model should be stored as ODEs (ODE) or as a reaction network (RN). Use MA-RN for mass-action RN, and EULER for the Euler method");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				/*String fileName = parameter.substring("format=>".length(), parameter.length());
				SupportedFormats format = (fromGUI)?SupportedFormats.CRNGUI:SupportedFormats.CRN;
				writeCRN(fileName,crn,partition,format,"","",out,bwOut);
				break;*/
				String f = parameter.substring("format=>".length(), parameter.length());
				if(f.equals(ODEorNET.ODE.toString())){
					crnGUIFormat = ODEorNET.ODE;
				}
				else if(f.equals(SupportedFormats.CRN.toString())){
					format = SupportedFormats.CRN;
				}
				else if(f.equals("EULER")) {
					crnGUIFormat = ODEorNET.ODE;
					rnEncoding=false;
					//euler=true;
					//euler_write_tau=true;
					euler = EULER.EULER_WITH_TAU;
				}
				else if(f.equals("EULER-NO-TAU")) {
					crnGUIFormat = ODEorNET.ODE;
					rnEncoding=false;
					//euler=true;
					//euler_write_tau=false;
					euler = EULER.EULER_NO_TAU;
				}
				else{
					crnGUIFormat = ODEorNET.RN;
					if(f.equals("MA-RN")){
						rnEncoding=true;
					}
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
		
		writeCRN(fileName,crn,partition,format,null,"",null,out,bwOut,crnGUIFormat,rnEncoding,paramsToCurry,deterministicCorrection,originalNames,euler,resetParameters);
	}

	private void handleExportMRMCCTMCWithSingleUseOfParameters(String command, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		String fileName=null;
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for (String parameter : parameters) {
			if(parameter.startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameter,out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return;
				}
			}
			else if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("fileOut=>")){
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .net file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				fileName = parameter.substring("fileOut=>".length(), parameter.length());
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
		
		if(!(crn.isMassAction() && crn.getMaxArity() <=1)){
			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it is not a unary mass action CRN (i.e., has reactions with more than one reagent, or has reactions with arbitrary rates). I terminate.");
			return;
		}
		else if(crn.isSymbolic()){
			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it contains symbolic parameters (i.e. parameters with no acutal value assigned) I terminate.");
			return;
		}
		else if(crn.algebraicSpecies()>0){
			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it is a system of differential algebraic equations (i.e. it has algebraic species) I terminate.");
			return;
		}
		
		//ICRN crnWithSingleUseOfParams = ExportSingleUseOfParams.expandMRMCCTMCWithSingleUseOfParameters(crn,out, bwOut);
		ExportSingleUseOfParams.expandMRMCCTMCWithSingleUseOfParametersModifyingTheModel(crn,out, bwOut);
		
		//Come faccio con la partizione dei parametri?
			//When reading the parameters, I partition them according to their names 
		//writeCRN(fileName,crnWithSingleUseOfParams,obtainedPartitionForCRNWithSingleUseOfParams,SupportedFormats.CRNGUI,null,"",null,out,bwOut,false,null);
		writeCRN(fileName,crn,partition,SupportedFormats.CRNGUI,null,"",null,out,bwOut,false,null);
		
	}

	
	private void handleExportBNGCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		//exportBNG({file=>outputfileName.crn})
		
		LinkedHashSet<String> paramsToCurry=new LinkedHashSet<String>();
		
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for (String parameter : parameters) {
			if(parameter.startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameter,out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return;
				}
			}
			else if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("fileOut=>")){
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .net file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String fileName = parameter.substring("fileOut=>".length(), parameter.length());
				writeCRN(fileName,crn,partition,SupportedFormats.BNG,null,"",null,out,bwOut,false,paramsToCurry);
				break;
			}
			else if(parameter.startsWith("paramsToCurry=>")){
				if(parameter.length()<="paramsToCurry=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the set of parameters to curry. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String[] par = parameter.substring("paramsToCurry=>".length(), parameter.length()).split(";");
				for(int i=0;i<par.length;i++){
					paramsToCurry.add(par[i]);
				}
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameter+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return;
			}
		}
	}
	
	private void handleExportModelicaCommand(String command, MessageConsoleStream out, BufferedWriter bwOut ) throws UnsupportedFormatException 
	{
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		String fileName = null;
		String exportIC = "false";
		if(parameters == null )
		{
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for (String parameter : parameters )
		{
			if(parameter.startsWith("fileOut=>"))
			{
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .mo file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				fileName = parameter.substring("fileOut=>".length(), parameter.length());
			}
			else if(parameter.startsWith("exportICOfAlgebraic=>"))
			{
				if(parameter.length()<="exportICOfAlgebraic=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if you want to export (true) or not (false) also the IC of the algebraic variables. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				exportIC = parameter.substring("exportICOfAlgebraic=>".length(), parameter.length());
			}
			else if(parameter.equals("")){
				continue;
			}
		}
		
		if(fileName==null || exportIC==null) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command.");
			return;
		}
		
		if(fileName ==null || fileName.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		
		ArrayList<String> furtherParameters = new ArrayList<String>(1);
		furtherParameters.add(exportIC);
		
		writeCRN(fileName,crn,partition,SupportedFormats.Modelica,null,"",furtherParameters,out,bwOut,false,null);
	}
	
	private void handleExportMACRNCommand(String command, MessageConsoleStream out, BufferedWriter bwOut , boolean macrn) throws UnsupportedFormatException 
	{
		
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		String fileName = null;
		String exportIC = "false";
		if(parameters == null )
		{
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for (String parameter : parameters )
		{
			if(parameter.startsWith("fileOut=>"))
			{
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .mo file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				fileName = parameter.substring("fileOut=>".length(), parameter.length());
			}
			else if(parameter.equals("")){
				continue;
			}
		}
		
		if(fileName==null || exportIC==null) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command.");
			return;
		}
		
		if(fileName ==null || fileName.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		
		ICRN ma = null;
		IPartition partitionToWrite = partition;
		
		if(crn.isMassAction()){
			ma = crn;
		}
		else {
			if(macrn) {
				ma = CRN.exportMACRN(crn, out, bwOut);
			}
			else {
				CRNReducerCommandLine.print(out,bwOut,"Converting the model in mass-action reaction network form (potentially with negative rates) ...");
				CRNandPartition crnAndSpeciesAndPartition=MatlabODEPontryaginExporter.computeRNEncoding(crn, crn.getReactions(), out, bwOut, partition,false);
				
				if(crnAndSpeciesAndPartition==null){
					ma=null;
				}
				else {
					ma = crnAndSpeciesAndPartition.getCRN();
					partitionToWrite=crnAndSpeciesAndPartition.getPartition();
					CRNReducerCommandLine.println(out,bwOut," completed.");
				}
			}
		}
		
		
		if(ma==null) {
			CRNReducerCommandLine.println(out,bwOut,"\nI skip this command: "+command);
		}
		else {
			writeCRN(fileName,ma,partitionToWrite,SupportedFormats.CRNGUI,null,"",null,out,bwOut,false,null);
		}
	}
	
	private void handleExportStoichiometryCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		//exportBNG({file=>outputfileName.crn})
		String parameters[] = CRNReducerCommandLine.getParameters(command);
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
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .net file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String fileName = parameter.substring("fileOut=>".length(), parameter.length());
				writeCRN(fileName,crn,partition,SupportedFormats.StoichiometryMatrix,null,"",null,out,bwOut,false,null);
				break;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameter+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return;
			}
		}
	}
	
	private void handleExportAffineSystemCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		//exportBNG({file=>outputfileName.crn})
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for (String parameter : parameters) {
			if(parameter.startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameter,out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return;
				}
			}
			else if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("fileOut=>")){
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .net file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String fileName = parameter.substring("fileOut=>".length(), parameter.length());
				writeCRN(fileName,crn,partition,SupportedFormats.Affine,null,"",null,out,bwOut,false,null);
				break;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameter+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return;
			}
		}
	}
	
	private void handleExportMatlabODEsCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		//exportBNG({file=>outputfileName.crn})
		double tEnd=100;
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		String fileName=null;
		String odeFunc="ode45";
		boolean writeJacobian=false;
		
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for (String parameter : parameters) {
			if(parameter.startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameter,out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return;
				}
			}
			else if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("tEnd=>")){
				if(parameter.length()<="tEnd=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the maximum simulation time. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				tEnd = Double.valueOf(parameter.substring("tEnd=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("fileOut=>")){
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .m file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				fileName = parameter.substring("fileOut=>".length(), parameter.length());
				//writeCRN(fileName,crn,partition,SupportedFormats.MatalbArbitraryODEs,null,"",out,bwOut);
				//writeCRN(fileName,crn, partition, SupportedFormats.MatalbArbitraryODEs, null, "", String.valueOf(tEnd), out,false);
				//break;
			}
			else if(parameter.startsWith("odeFunc=>")){
				if(parameter.length()<="odeFunc=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the ode solver to use. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				odeFunc = parameter.substring("odeFunc=>".length(), parameter.length());
			}
			else if(parameter.startsWith("writeJacobian=>")){
				if(parameter.length()<="writeJacobian=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if the Jacobian matrix has to be written. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				if(parameter.substring("writeJacobian=>".length(), parameter.length()).equalsIgnoreCase("true")){
					writeJacobian=true;
				}
				
				
				//writeCRN(fileName,crn,partition,SupportedFormats.MatalbArbitraryODEs,null,"",out,bwOut);
				//writeCRN(fileName,crn, partition, SupportedFormats.MatalbArbitraryODEs, null, "", String.valueOf(tEnd), out,false);
				//break;
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
		List<String> furtherParameters = new ArrayList<>(3);
		furtherParameters.add(String.valueOf(tEnd));
		furtherParameters.add(String.valueOf(writeJacobian));
		furtherParameters.add(odeFunc);
		writeCRN(fileName,crn, partition, SupportedFormats.MatlabArbitraryODEs, null, "", furtherParameters, out,bwOut,false,null);
	}
	
	private void handleExportCERENACommand(String command, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		String fileName=null;
		
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for (String parameter : parameters) {
			if(parameter.startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameter,out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return;
				}
			}
			else if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("fileOut=>")){
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .m file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				fileName = parameter.substring("fileOut=>".length(), parameter.length());
				//writeCRN(fileName,crn,partition,SupportedFormats.MatalbArbitraryODEs,null,"",out,bwOut);
				//writeCRN(fileName,crn, partition, SupportedFormats.MatalbArbitraryODEs, null, "", String.valueOf(tEnd), out,false);
				//break;
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
		List<String> furtherParameters = null;
		writeCRN(fileName,crn, partition, SupportedFormats.CERENA, null, "", furtherParameters, out,bwOut,false,null);
	}
	
	private void handleExportLNACommand(String command, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		String fileName=null;
		
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for (String parameter : parameters) {
			if(parameter.startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameter,out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return;
				}
			}
			else if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("fileOut=>")){
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .m file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				fileName = parameter.substring("fileOut=>".length(), parameter.length());
				//writeCRN(fileName,crn,partition,SupportedFormats.MatalbArbitraryODEs,null,"",out,bwOut);
				//writeCRN(fileName,crn, partition, SupportedFormats.MatalbArbitraryODEs, null, "", String.valueOf(tEnd), out,false);
				//break;
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
		List<String> furtherParameters = null;
		writeCRN(fileName,crn, partition, SupportedFormats.LNA, null, "", furtherParameters, out,bwOut,false,null);
	}

	
	private void handleExportExportScriptSolveUCTMC(String command, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		String fileName=null;
		String modelWithSmallM=null;
		String modelWithBigM=null;
		boolean minimize=true;
		double tHoriz=-1;
		LinkedHashMap<String, Double> rRewards=null,phiRewards=null,inits=null;
		String deltaStr=null;
		
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for (String parameter : parameters) {
			if(parameter.startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameter,out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return;
				}
			}
			else if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("fileOut=>")){
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .m file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				fileName = parameter.substring("fileOut=>".length(), parameter.length());
				//writeCRN(fileName,crn,partition,SupportedFormats.MatalbArbitraryODEs,null,"",out,bwOut);
				//writeCRN(fileName,crn, partition, SupportedFormats.MatalbArbitraryODEs, null, "", String.valueOf(tEnd), out,false);
				//break;
			}
			else if(parameter.startsWith("modelWithSmallM=>")){
				if(parameter.length()<="modelWithSmallM=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .ode file containing small m. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				modelWithSmallM = parameter.substring("modelWithSmallM=>".length(), parameter.length());
				//writeCRN(fileName,crn,partition,SupportedFormats.MatalbArbitraryODEs,null,"",out,bwOut);
				//writeCRN(fileName,crn, partition, SupportedFormats.MatalbArbitraryODEs, null, "", String.valueOf(tEnd), out,false);
				//break;
			}
			else if(parameter.startsWith("modelWithBigM=>")){
				if(parameter.length()<="modelWithBigM=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .ode file containing big M. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				modelWithBigM = parameter.substring("modelWithBigM=>".length(), parameter.length());
				//writeCRN(fileName,crn,partition,SupportedFormats.MatalbArbitraryODEs,null,"",out,bwOut);
				//writeCRN(fileName,crn, partition, SupportedFormats.MatalbArbitraryODEs, null, "", String.valueOf(tEnd), out,false);
				//break;
			}
			else if(parameter.startsWith("delta=>")){
				if(parameter.length()<="delta=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the delta to be used to compute m and M. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				deltaStr = parameter.substring("delta=>".length(), parameter.length());
				//writeCRN(fileName,crn,partition,SupportedFormats.MatalbArbitraryODEs,null,"",out,bwOut);
				//writeCRN(fileName,crn, partition, SupportedFormats.MatalbArbitraryODEs, null, "", String.valueOf(tEnd), out,false);
				//break;
			}
			else if(parameter.startsWith("tHoriz=>")){
				if(parameter.length()<="tHoriz=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the time horizon to use. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				tHoriz = Double.valueOf(parameter.substring("tHoriz=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("minimize=>")){
				if(parameter.length()<="minimize=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the time horizon to use. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				minimize = Boolean.valueOf(parameter.substring("minimize=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("stateRewards=>")){
				if(parameter.length()<="stateRewards=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the runtime rewards. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String row = parameter.substring("stateRewards=>".length(), parameter.length());
				rRewards = parseSpCoeffs(row,false);
			}
			else if(parameter.startsWith("stateRewardsFile=>")){
				if(parameter.length()<="stateRewardsFile=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the runtime rewards. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String file = parameter.substring("stateRewardsFile=>".length(), parameter.length());
				try {
					rRewards = readPrismRewsFile(file,true);
				} catch (FileNotFoundException e) {
					CRNReducerCommandLine.println(out,bwOut,"File with rewards not found: "+file);
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				} catch (IOException e) {
					CRNReducerCommandLine.println(out,bwOut,"Problems in loading the file with rewards: "+file);
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				//rRewards = parseSpCoeffs(row);
			}
			else if(parameter.startsWith("phi=>")){
				if(parameter.length()<="phi=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the final rewards (at time horizon). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String row = parameter.substring("phi=>".length(), parameter.length());
				if(row.equals("[NONE]"))
					phiRewards = new LinkedHashMap<>(0);
				else
					phiRewards = parseSpCoeffs(row,false);
			}
			else if(parameter.startsWith("inits=>")){
				if(parameter.length()<="inits=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the initial states (paired with their probabilities). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String row = parameter.substring("inits=>".length(), parameter.length());
				inits = parseSpCoeffs(row,false);
			}
			else if(parameter.startsWith("initsIDs=>")){
				if(parameter.length()<="initsIDs=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the ids of the initial states (paired with their probabilities). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String row = parameter.substring("initsIDs=>".length(), parameter.length());
				inits = parseSpCoeffs(row,true);
			}
			
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameter+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return;
			}
		}
		
		if(tHoriz<0) {
			CRNReducerCommandLine.println(out,bwOut,"Please, specify a positive time horizon. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		if(fileName ==null || fileName.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		if((deltaStr==null || deltaStr.length()==0) && (modelWithSmallM ==null || modelWithSmallM.equals(""))&& (modelWithBigM ==null || modelWithBigM.equals(""))){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .ode file containing small M or big M. Alternatively, specify the delta ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		if(rRewards==null || phiRewards==null || inits==null) {
			CRNReducerCommandLine.println(out,bwOut,"Initial states and running/final rewards must be specified (potentially empty). ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		
		if(!(crn.getMaxArity() ==1 && crn.isElementary())) {
			CRNReducerCommandLine.println(out,bwOut,"Not supported CRN: it is not a CTMC. It must contain only mass-action reactions with 1 reagent and 1 product. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		MatlabODEsImporter moi = new MatlabODEsImporter(fileName, out, bwOut, messageDialogShower);
		BigDecimal deltaHalf=null;
		if(deltaStr!=null) {
			deltaHalf=new BigDecimal(deltaStr).divide(new BigDecimal(2));
		}
		moi.printSolveUCTMC(crn, minimize, tHoriz, minimize, modelWithSmallM,modelWithBigM,deltaHalf,
				inits, rRewards, phiRewards, out, bwOut, messageDialogShower, terminator);
		
	}
	
	private LinkedHashMap<String, Double> readPrismRewsFile(String file,boolean decreaseSpId) throws FileNotFoundException, IOException {
		//1 1
		//2 1
		LinkedHashMap<String, Double> rRewards = new LinkedHashMap<>();
		
		BufferedReader br = new BufferedReader(new FileReader(file));
		String current = br.readLine();
		while(current!=null) {
			String[] splitted = current.trim().split("\\ ");
			int s = Integer.valueOf(splitted[0]);
			if(decreaseSpId)
				s-=1;
			double v = Double.valueOf(splitted[1]);
			
			rRewards.put("S"+s, v);
			current = br.readLine()
					;
		}
		br.close();
		
		return rRewards;
	}
	private void handleExportJacobianFunctionCommand(String command, MessageConsoleStream out, BufferedWriter bwOut,
			boolean epsCLump) throws UnsupportedFormatException {
		double tEnd=0.0;
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		String fileName=null;
		String epsilonString=null;
		String fromEpsString=null;
		String toEpsString=null;
		String stepEpsString=null;
		int maxPerturb=0;
		String fromMaxPerturbString=null;
		String toMaxPerturbString=null;
		String stepMaxPerturbString=null;
		ArrayList<LinkedHashMap<String, Double>> M0=null;
		String csvFile=null;
		String M0String=null;
		int M0view=-1;
		int cLump=0;
		//double slope=-1;
		String fromSlope=null;
		String toSlope=null;
		String stepSlope=null;
		boolean writeInnerScript=false;
		boolean writeMainScript=false;
		//String prePartitionWRTIC="false";
		String prePartitionUserDefined="false";
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for (String parameter : parameters) {
			if(parameter.startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameter,out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return;
				}
			}
			else if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("fileOut=>")){
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .m file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				fileName = parameter.substring("fileOut=>".length(), parameter.length());
				//writeCRN(fileName,crn,partition,SupportedFormats.MatalbArbitraryODEs,null,"",out,bwOut);
				//writeCRN(fileName,crn, partition, SupportedFormats.MatalbArbitraryODEs, null, "", String.valueOf(tEnd), out,false);
				//break;
			}
			else if(parameter.startsWith("epsilon=>")){
				if(parameter.length()<="epsilon=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the epsilon value. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				epsilonString = parameter.substring("epsilon=>".length(), parameter.length());
			}
			else if(parameter.startsWith("fromEps=>")){
				if(parameter.length()<="fromEps=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the epsilon value: from ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				fromEpsString = parameter.substring("fromEps=>".length(), parameter.length());
			}
			else if(parameter.startsWith("toEps=>")){
				if(parameter.length()<="toEps=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the epsilon value: to ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				toEpsString = parameter.substring("toEps=>".length(), parameter.length());
			}
			else if(parameter.startsWith("stepEps=>")){
				if(parameter.length()<="stepEps=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the epsilon value: step ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				stepEpsString = parameter.substring("stepEps=>".length(), parameter.length());
			}
			else if(parameter.startsWith("tEnd=>")){
				if(parameter.length()<="tEnd=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the time horizon. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				tEnd = Double.valueOf(parameter.substring("tEnd=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("fromSlope=>")){
				if(parameter.length()<="fromSlope=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the slope to use to compute the maximum allowed error ([0,1]-> 0%,100%. But we can go even above 100%): FROM");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				fromSlope = parameter.substring("fromSlope=>".length(), parameter.length());
			}
			else if(parameter.startsWith("toSlope=>")){
				if(parameter.length()<="toSlope=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the slope to use to compute the maximum allowed error ([0,1]-> 0%,100%. But we can go even above 100%): TO");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				toSlope = parameter.substring("toSlope=>".length(), parameter.length());
			}
			else if(parameter.startsWith("stepSlope=>")){
				if(parameter.length()<="stepSlope=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the slope to use to compute the maximum allowed error ([0,1]-> 0%,100%. But we can go even above 100%): STEP");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				stepSlope = parameter.substring("stepSlope=>".length(), parameter.length());
			}
			else if(parameter.startsWith("writeInnerScript=>")){
				if(parameter.length()<="writeInnerScript=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify whetehr the model independent script should be written. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				writeInnerScript = Boolean.valueOf(parameter.substring("writeInnerScript=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("writeMainScript=>")){
				if(parameter.length()<="writeMainScript=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify whetehr the model independent script should be written. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				writeMainScript = Boolean.valueOf(parameter.substring("writeMainScript=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("maxPerturb=>")){
				if(parameter.length()<="maxPerturb=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the max perturbation on the rates. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				maxPerturb = Integer.valueOf(parameter.substring("maxPerturb=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("prePartition=>")){
				String prep = parameter.substring("prePartition=>".length(), parameter.length()).trim();
				if(prep.compareToIgnoreCase("USER")==0){
					//prePartitionWRTIC = "false";
					prePartitionUserDefined = "true";
				}
				//			else if(prep.compareToIgnoreCase("NO")==0){
				//				prePartitionWRTIC = "false";
				//				prePartitionUserDefined = "false";
				//			}
				//			else if(prep.compareToIgnoreCase("IC")==0){
				//				prePartitionWRTIC = "true";
				//				prePartitionUserDefined = "false";
				//			} 
				//			else if(prep.compareToIgnoreCase("USER_and_IC")==0){
				//				prePartitionWRTIC = "true";
				//				prePartitionUserDefined = "true";
				//			}
				else{
					CRNReducerCommandLine.println(out,bwOut,"Unknown prepartitioning option \""+prep+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
					return;
				}
				if(prePartitionUserDefined.equals("true")){
					M0 = new ArrayList<>(crn.getUserDefinedPartition().size());
					for(int r=0;r<crn.getUserDefinedPartition().size();r++) {
						HashSet<ISpecies> block = crn.getUserDefinedPartition().get(r);
						LinkedHashMap<String, Double> speciesToCoefficient=new LinkedHashMap<String, Double>(block.size());
						M0.add(speciesToCoefficient);
						for(ISpecies species : block){
							String name=species.getName();
							double val=1;
							speciesToCoefficient.put(name, val);
						}
					}
				}
			}
			else if(parameter.startsWith("cLump=>")){
				if(parameter.length()<="cLump=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, the result obtained by cLump on the original model ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				cLump = Integer.valueOf(parameter.substring("cLump=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("fromMaxPerturb=>")){
				if(parameter.length()<="fromMaxPerturb=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the max perturbation on the rates: from ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				fromMaxPerturbString = parameter.substring("fromMaxPerturb=>".length(), parameter.length());
			}
			else if(parameter.startsWith("toMaxPerturb=>")){
				if(parameter.length()<="toMaxPerturb=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the max perturbation on the rates: to ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				toMaxPerturbString = parameter.substring("toMaxPerturb=>".length(), parameter.length());
			}
			else if(parameter.startsWith("stepMaxPerturb=>")){
				if(parameter.length()<="stepMaxPerturb=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the max perturbation on the rates: step ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				stepMaxPerturbString = parameter.substring("stepMaxPerturb=>".length(), parameter.length());
			}
			else if(parameter.startsWith("csvFile=>")){
				if(parameter.length()<="csvFile=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the CSV file. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
				}
				csvFile = parameter.substring("csvFile=>".length(), parameter.length());
			}
			else if(parameter.startsWith("M0view=>")){
				if(parameter.length()<="M0view=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the view to use as M0 - the constraints according to which we should lump. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				M0view = Integer.parseInt(parameter.substring("M0view=>".length(), parameter.length()));
				List<HashMap<ISpecies, Integer>> viewsAsMultiSet = crn.getViewsAsMultiset();
				if(viewsAsMultiSet==null) {
					CRNReducerCommandLine.println(out,bwOut,"There are no views. Specify M0 differently. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				if(viewsAsMultiSet.size()<M0view || viewsAsMultiSet.get(M0view-1)==null) {
					CRNReducerCommandLine.println(out,bwOut,"There are no enough views. Specify M0 differently. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				M0 = new ArrayList<>(1);
				HashMap<ISpecies, Integer> view = viewsAsMultiSet.get(M0view - 1);
				LinkedHashMap<String, Double> speciesToCoefficient=new LinkedHashMap<String, Double>(view.size());
				M0.add(speciesToCoefficient);
				for(Entry<ISpecies, Integer> entry:view.entrySet()) {
					String name=entry.getKey().getName();
					double val=entry.getValue();
					speciesToCoefficient.put(name, val);
				}
			}
			else if(parameter.startsWith("M0=>")){
				if(parameter.length()<="M0=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the M0 - the constraints according to which we should lump. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				M0String = parameter.substring("M0=>".length(), parameter.length());
				String[] rows = M0String.split("---");
				M0 = new ArrayList<>(rows.length);
				for(int r=0;r<rows.length;r++) {
					String row=rows[r];
					LinkedHashMap<String, Double> speciesToCoefficient = parseSpCoeffs(row,false);
					M0.add(speciesToCoefficient);
				}
				

				//TODO: taking inspiration from
				/*
				 else if(parameter.startsWith("coefficients=>")){
				if(parameter.length()<="coefficients=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the coefficients of the variables. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String[] par = parameter.substring("coefficients=>".length(), parameter.length()).split(";");
				speciesToCoefficient=new HashMap<String, Double>(par.length);
				for(int i=0;i<par.length;i++){
					int colon=par[i].indexOf(':');
					String name=par[i].substring(0,colon);
					String val=par[i].substring(colon+1);
					speciesToCoefficient.put(name, crn.getMath().evaluate(val));
				}
			}
				 */
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
		if(!epsCLump) {
			writeCRN(fileName,crn, partition, SupportedFormats.MatlabJacobianFunction, null, "",null, out,bwOut,false,null);
		}
		else {
			if(crn==null){
				//CRNReducerCommandLine.println(out,bwOut,"Before writing a model in a file it is necessary to load it. "); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				//CRNReducerCommandLine.println(out,bwOut,"I skip the write command in file: "+fileName);
				CRNReducerCommandLine.println(out,bwOut,"No model loaded. I skip the write command in file: "+fileName); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return;
			}
			if(fromMaxPerturbString==null && (epsilonString==null || M0==null)) {
				CRNReducerCommandLine.println(out,bwOut,"Both epsilon and M0 have to be provided. I skip the write command in file: "+fileName); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return;
			}

			boolean singleScript=fromMaxPerturbString==null;

			if(singleScript) {
				if(maxPerturb<0 || maxPerturb>100) {
					CRNReducerCommandLine.println(out,bwOut,"The maximum percentual perturbation should be an interval included in [0,100].");
					return;
				}
				double eps = crn.getMath().evaluate(epsilonString);
				CRNReducerCommandLine.print(out,bwOut,"Writing to file "+ fileName+" ...");
				MatlabODEsImporter.printJacobianFunctionToMatlabFIle(crn, fileName, null, verbose, null, out,bwOut,messageDialogShower,true,eps,(int)(Math.round(maxPerturb)),M0);
				CRNReducerCommandLine.println(out,bwOut," completed");

				if(writeMainScript) {
					MatlabODEsImporter moi = new MatlabODEsImporter(fileName, out, bwOut, messageDialogShower);
					moi.printMainEpsCLumpScript(fileName,verbose,out,bwOut,messageDialogShower);
				}

				//writeCRN(fileName,crn, partition, SupportedFormats.MatlabJacobianFunctionEpsCLump, null, "",null, out,bwOut,false,null);
			}
			else {
				boolean cLumpGiven = cLump>0;
				boolean slopeGiven = fromSlope!=null;
				boolean fromToEps=!(cLumpGiven || slopeGiven);

				int from = Integer.valueOf(fromMaxPerturbString);
				int to = Integer.valueOf(toMaxPerturbString);
				int step = Integer.valueOf(stepMaxPerturbString);

				double fromEps=-1;
				double toEps=-1;
				double stepEps=1;
				if(fromToEps) {
					fromEps=crn.getMath().evaluate(fromEpsString);
					toEps=crn.getMath().evaluate(toEpsString);
					stepEps=crn.getMath().evaluate(stepEpsString);
				}

				if(from< 0 || from>100 || to< 0 || to>100 || from>to) {
					CRNReducerCommandLine.println(out,bwOut,"The maximum percentual perturbation should be an interval included in [0,100].");
					return;
				}

				double fromSlop=-1;
				double toSlop=-1;
				double stepSlop=-1;
				if(slopeGiven) {
					fromSlop=crn.getMath().evaluate(fromSlope);
					toSlop=crn.getMath().evaluate(toSlope);
					stepSlop=crn.getMath().evaluate(stepSlope);
					if(fromSlop< 0 || toSlop <0 || stepSlop < 0 || fromSlop >toSlop) {
						CRNReducerCommandLine.println(out,bwOut,"The required slopes should be given as an interval.");
						return;
					}
				}
				

				ArrayList<String> functionNames=new ArrayList<String>();
				//int i=1;
				if(from==0 && from==to && from==step) {
					step=1;
				}
				for(maxPerturb=from;maxPerturb<= to;maxPerturb+=step) {
					String fileNameMP = AbstractImporter.overwriteExtensionIfEnabled(fileName, "",true);
					fileNameMP+= "_"+String.valueOf(maxPerturb).replace('.', '_').replace(',', '_').replace('-', 'm')+".m";
					//fileNameMP+= "_"+String.valueOf(i)+".m";
					CRNReducerCommandLine.print(out,bwOut,"Writing model with max percentual perturbation "+maxPerturb+" to file "+ fileNameMP+" ...");
					//String functionName=MatlabODEsImporter.printJacobianFunctionToMatlabFIle(crn, fileNameMP, null, verbose, null, out,bwOut,messageDialogShower,true,0,maxPerturb,M0);
					String functionName=MatlabODEsImporter.printEpsCLumpScriptsToMatlabFIle(crn, fileNameMP, null, verbose, null, out,bwOut,messageDialogShower,maxPerturb,M0);
					functionNames.add(functionName);
					CRNReducerCommandLine.println(out,bwOut," completed");
					//i++;
				}

				MatlabODEsImporter moi = new MatlabODEsImporter(fileName, out, bwOut, messageDialogShower);
				CRNReducerCommandLine.print(out,bwOut,"Writing analysis campaign file "+ fileName+" ...");
				moi.printEpsCLumpAnalysisCampaignToFile(crn.getName(), fileName, verbose, out, bwOut, messageDialogShower,functionNames,csvFile,
						writeInnerScript,cLump,fromEps,toEps,stepEps,cLumpGiven,fromSlop,toSlop,stepSlop,slopeGiven,tEnd);
				CRNReducerCommandLine.println(out,bwOut," completed");

				//writeCRN(fileName,crn, partition, SupportedFormats.MatlabJacobianFunctionEpsCLump, null, "",null, out,bwOut,false,null);
			}
		}

	}
	public LinkedHashMap<String, Double> parseSpCoeffs(String row,boolean idToSpecies) {
		if(row.startsWith("["))
			row=row.substring(1,row.length());
		if(row.endsWith("]"))
			row=row.substring(0,row.length()-1);
		String[] coefficients = row.split(";");
		LinkedHashMap<String, Double> speciesToCoefficient=new LinkedHashMap<String, Double>(coefficients.length);
		for(int i=0;i<coefficients.length;i++){
			int colon=coefficients[i].indexOf(':');
			String name=coefficients[i].substring(0,colon);
			if(idToSpecies) {
				int id=Integer.valueOf(name)-1;
				name = "S"+id;
			}
			String val=coefficients[i].substring(colon+1);
			speciesToCoefficient.put(name, crn.getMath().evaluate(val));
		}
		return speciesToCoefficient;
	}

	
	private void handleExportCausalGraph(String command, MessageConsoleStream out, BufferedWriter bwOut) {
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		String fileName=null;
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
				//writeCRN(fileName,crn,partition,SupportedFormats.MatalbArbitraryODEs,null,"",out,bwOut);
				//writeCRN(fileName,crn, partition, SupportedFormats.MatalbArbitraryODEs, null, "", String.valueOf(tEnd), out,false);
				//break;
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
		
		if((!crn.getMdelDefKind().equals(ODEorNET.ODE)) && !crn.isMassAction()) {
			CRNReducerCommandLine.print(out,bwOut,"This command should be invoked on ODE models.");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		
		CausalGraphCreator cgc = new CausalGraphCreator();
		cgc.createCausalGraphTrivial(crn,out,bwOut,verbose);
		cgc.writeCausalGraph(AbstractImporter.overwriteExtensionIfEnabled(fileName, ".txt",true), crn.getName(), crn.getSpecies(),out, bwOut, verbose);
		String dotFile = AbstractImporter.overwriteExtensionIfEnabled(fileName, ".dot",true);
		cgc.writeDOTCausalGraph(dotFile, crn.getName(), crn.getSpecies(), out, bwOut, verbose);
		String figureFile = AbstractImporter.overwriteExtensionIfEnabled(fileName, "",true);
		try {
			FigureCreator.generateFigure(dotFile, figureFile, out, bwOut, verbose);
		} catch (IOException e) {
			CRNReducerCommandLine.print(out,bwOut,"Problems in creating the figure.");
			//e.printStackTrace();
		}
		
	}
	
	
	
	private void handleExportEpsilonBoundsScriptCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		double tEnd = 5;
		double deltat = 0.1;
		String epsilonString = "0";
		boolean epsilonSpecified = false;
		double defIC=0;
		LinkedHashSet<String> paramsToPerturb=new LinkedHashSet<String>();
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		
		String prePartitionWRTIC="false";
		String prePartitionUserDefined="false";
		boolean forward=true;
		boolean backward=true;
		
		String fileName=null;
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for (String parameter : parameters) {
			if(parameter.startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameter,out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return;
				}
			}
			else if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("prePartition=>")){
				if(parameter.length()<="prePartition=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if you want to prepartion the species of the model according to their initial conditions or to the views. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String prep = parameter.substring("prePartition=>".length(), parameter.length()).trim();
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
				else if(prep.compareToIgnoreCase("USER_and_IC")==0){
					prePartitionWRTIC = "true";
					prePartitionUserDefined = "true";
				}
				else{
					CRNReducerCommandLine.println(out,bwOut,"Unknown prepartitioning option \""+prep+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
					return;
				}
			}
			else if(parameter.startsWith("reduction=>")){
				if(parameter.length()<="reduction=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the reduction(s) of interest: forward, backward of both. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String red = parameter.substring("reduction=>".length(), parameter.length()).trim();
				if(red.compareToIgnoreCase("forward")==0){
					forward=true;
					backward=false;
				}
				else if(red.compareToIgnoreCase("backward")==0){
					forward=false;
					backward=true;
				}
				else if(red.compareToIgnoreCase("both")==0){
					forward=true;
					backward=true;
				}
				else if(red.compareToIgnoreCase("NO")==0){
					forward=false;
					backward=false;
				}
				else{
					CRNReducerCommandLine.println(out,bwOut,"Unknown prepartitioning option \""+red+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
					return;
				}
			}
			else if(parameter.startsWith("fileOut=>")){
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .m file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				fileName = parameter.substring("fileOut=>".length(), parameter.length());
				//writeCRN(fileName,crn,partition,SupportedFormats.MatalbArbitraryODEs,null,"",out,bwOut);
				//writeCRN(fileName,crn, partition, SupportedFormats.MatalbArbitraryODEs, null, "", String.valueOf(tEnd), out,false);
				//break;
			}
			else if(parameter.startsWith("paramsToPerturb=>")){
				if(parameter.length()<="paramsToPerturb=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the set of parameters to perturb. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String[] par = parameter.substring("paramsToPerturb=>".length(), parameter.length()).split(";");
				for(int i=0;i<par.length;i++){
					paramsToPerturb.add(par[i]);
				}
				//writeCRN(fileName,crn,partition,SupportedFormats.MatalbArbitraryODEs,null,"",out,bwOut);
				//writeCRN(fileName,crn, partition, SupportedFormats.MatalbArbitraryODEs, null, "", String.valueOf(tEnd), out,false);
				//break;
			}
			else if(parameter.startsWith("fileOut=>")){
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .m file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				fileName = parameter.substring("fileOut=>".length(), parameter.length());
				//writeCRN(fileName,crn,partition,SupportedFormats.MatalbArbitraryODEs,null,"",out,bwOut);
				//writeCRN(fileName,crn, partition, SupportedFormats.MatalbArbitraryODEs, null, "", String.valueOf(tEnd), out,false);
				//break;
			}
			else if(parameter.startsWith("tEnd=>")){
				if(parameter.length()<="tEnd=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the time horizon. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				tEnd = Double.valueOf(parameter.substring("tEnd=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("deltat=>")){
				if(parameter.length()<="deltat=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the size of the optimization step. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String deltatString = parameter.substring("deltat=>".length(), parameter.length());
				deltat=crn.getMath().evaluate(deltatString);
			}
			else if(parameter.startsWith("epsilon=>")){
				if(parameter.length()<="epsilon=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the epsilon value. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				epsilonString = parameter.substring("epsilon=>".length(), parameter.length());
				epsilonSpecified=true;
			}
			else if(parameter.startsWith("defaultIC=>")){
				if(parameter.length()<="defaultIC=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the value to be used as default initial concentration. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				defIC = Double.valueOf(parameter.substring("defaultIC=>".length(), parameter.length()));
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
		//writeCRN(fileName,crn, partition, SupportedFormats.MatlabEpsilonBounds, null, "","", out,false);
		
		if(crn==null){
			//CRNReducerCommandLine.println(out,bwOut,"Before writing a model in a file it is necessary to load it. "); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			//CRNReducerCommandLine.println(out,bwOut,"I skip the write command in file: "+fileName);
			CRNReducerCommandLine.println(out,bwOut,"No model loaded. I skip the write command in file: "+fileName); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		
		double epsilon=0;
		if(epsilonSpecified){
			epsilon = crn.getMath().evaluate(epsilonString);
			if(epsilon<0){
				CRNReducerCommandLine.printWarning(out, bwOut, true, messageDialogShower, "Please, specify a non-negative epsilon ("+epsilon+").\n\t I skip this command.",DialogType.Error);
				return;
			}
		}
		
		CRNReducerCommandLine.print(out,bwOut,"Writing the bounds script to file "+ fileName+" ...");
		MatlabODEsImporter moi=new MatlabODEsImporter(null, out, bwOut, messageDialogShower);
		moi.printEpsilonBoundScriptToMatlabFIle(crn, partition, fileName, verbose, out, bwOut, messageDialogShower,tEnd,deltat,paramsToPerturb,terminator,epsilon,defIC,prePartitionUserDefined,prePartitionWRTIC,forward,backward);
		CRNReducerCommandLine.println(out,bwOut," completed");
	}
	
	private void handleExportE2C2Command(String command, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		double kvalue=2000;
		double tEnd = 10;
		/*double delta = 0.01;
		double taylorOrder = 10;*/
		double timeStep = 0.01;
		//double defIC=0;
		//LinkedHashSet<String> paramsToPerturb=new LinkedHashSet<String>();
		
		String[] unsafeSetSplit=new String[0];
		
		String[] paramsToPerturb=null;
		double[] lows=null;
		double[] highs=null;
		HashMap<String, Integer> parameterToPerturbToItsPosition=null;
		
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		
		String fileName=null;
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for (String parameter : parameters) {
			if(parameter.startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameter,out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return;
				}
			}
			else if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("fileOut=>")){
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .m file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				fileName = parameter.substring("fileOut=>".length(), parameter.length());
				//writeCRN(fileName,crn,partition,SupportedFormats.MatalbArbitraryODEs,null,"",out,bwOut);
				//writeCRN(fileName,crn, partition, SupportedFormats.MatalbArbitraryODEs, null, "", String.valueOf(tEnd), out,false);
				//break;
			}
			/*
			else if(parameter.startsWith("paramsToPerturb=>")){
				if(parameter.length()<="paramsToPerturb=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the set of parameters to perturb. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String[] par = parameter.substring("paramsToPerturb=>".length(), parameter.length()).split(";");
				for(int i=0;i<par.length;i++){
					paramsToPerturb.add(par[i]);
				}
				//writeCRN(fileName,crn,partition,SupportedFormats.MatalbArbitraryODEs,null,"",out,bwOut);
				//writeCRN(fileName,crn, partition, SupportedFormats.MatalbArbitraryODEs, null, "", String.valueOf(tEnd), out,false);
				//break;
			}
			*/
			else if(parameter.startsWith("paramsToPerturb=>")){
				if(parameter.length()<="paramsToPerturb=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the set of parameters to perturb. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String[] par = parameter.substring("paramsToPerturb=>".length(), parameter.length()).split(";");
				paramsToPerturb=new String[par.length];
				lows=new double[par.length];
				highs=new double[par.length];
				parameterToPerturbToItsPosition=new HashMap<>(par.length);
				for(int i=0;i<par.length;i++){
					int posOfSB = par[i].indexOf('[');
					int posOfColon = par[i].indexOf(':');
					paramsToPerturb[i]=par[i].substring(0,posOfSB);
					lows[i]=crn.getMath().evaluate(par[i].substring(posOfSB+1, posOfColon));
					highs[i]=crn.getMath().evaluate(par[i].substring(posOfColon+1,par[i].length()-1));
					if(lows[i]>highs[i]){
						String message = "Please, specify a proper bound for parameter "+paramsToPerturb[i]+". The lower bound ("+lows[i]+") is biiger than the higher one ("+highs[i]+").";
						//CRNReducerCommandLine.println(out,bwOut,message);
						CRNReducerCommandLine.printWarning(out, bwOut, true, messageDialogShower, message, DialogType.Error);
						CRNReducerCommandLine.println(out,bwOut,message);
						//CRNReducerCommandLine.println(out,bwOut,"Please, specify a proper bound for parameter: "+paramsToPerturb[i]+" in ["+lows[i]+","+highs[i]+"]. ");
						CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
						return;
					}
					parameterToPerturbToItsPosition.put(paramsToPerturb[i], i);
				}
				//writeCRN(fileName,crn,partition,SupportedFormats.MatalbArbitraryODEs,null,"",out,bwOut);
				//writeCRN(fileName,crn, partition, SupportedFormats.MatalbArbitraryODEs, null, "", String.valueOf(tEnd), out,false);
				//break;
			}
			else if(parameter.startsWith("tEnd=>")){
				if(parameter.length()<="tEnd=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the time horizon. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				tEnd = Double.valueOf(parameter.substring("tEnd=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("kvalue=>")){
				if(parameter.length()<="kvalue=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the kvalue parameter. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				kvalue = Double.valueOf(parameter.substring("kvalue=>".length(), parameter.length()));
			}
			/*else if(parameter.startsWith("delta=>")){
				if(parameter.length()<="delta=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the delta parameter. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				delta = Double.valueOf(parameter.substring("delta=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("taylorOrder=>")){
				if(parameter.length()<="taylorOrder=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the taylor order. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				taylorOrder = Double.valueOf(parameter.substring("taylorOrder=>".length(), parameter.length()));
			}*/
			else if(parameter.startsWith("tStep=>")){
				if(parameter.length()<="tStep=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the time step. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				timeStep = Double.valueOf(parameter.substring("tStep=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("unsafeSet=>")){
				if(parameter.length()<="unsafeSet=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the unsafe set. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String unsafeSet=parameter.substring("unsafeSet=>".length(), parameter.length());
				unsafeSetSplit=unsafeSet.split("\\;");
			}
			/*else if(parameter.startsWith("defaultIC=>")){
				if(parameter.length()<="defaultIC=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the value to be used as default initial concentration. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				defIC = Double.valueOf(parameter.substring("defaultIC=>".length(), parameter.length()));
			}*/
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
		//writeCRN(fileName,crn, partition, SupportedFormats.MatlabEpsilonBounds, null, "","", out,false);
		
		if(crn==null){
			//CRNReducerCommandLine.println(out,bwOut,"Before writing a model in a file it is necessary to load it. "); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			//CRNReducerCommandLine.println(out,bwOut,"I skip the write command in file: "+fileName);
			CRNReducerCommandLine.println(out,bwOut,"No model loaded. I skip the write command in file: "+fileName); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		
		CRNReducerCommandLine.print(out,bwOut,"Writing the C2E2 script to file "+ fileName+" ...");
		//C2E2Exporter.printCRNToHYXMLFile(crn, fileName, null, verbose, out, bwOut, tEnd, kvalue,/*delta, taylorOrder,*/ timeStep, paramsToPerturb, messageDialogShower);
		C2E2Exporter.printCRNToC2E2File(crn, fileName, null, verbose, out, bwOut, tEnd, kvalue,/*delta, taylorOrder,*/ timeStep, 
				parameterToPerturbToItsPosition, paramsToPerturb, lows, highs,
				unsafeSetSplit,
				messageDialogShower);
		CRNReducerCommandLine.println(out,bwOut," completed");
	}
	
	private void handleExportCAGEScriptCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		boolean print = true;
		boolean backward=true;
		boolean writeSymbolicJacobian=false;
		int bound=0;
		String fileName=null;
		String sourceFileName=null;
		String unionFileName=null;
		
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		
		for (String parameter : parameters) {
			if(parameter.startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameter,out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return;
				}
			}
			else if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("fileOut=>")){
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .m file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				fileName = parameter.substring("fileOut=>".length(), parameter.length());
				//writeCRN(fileName,crn,partition,SupportedFormats.MatalbArbitraryODEs,null,"",out,bwOut);
				//writeCRN(fileName,crn, partition, SupportedFormats.MatalbArbitraryODEs, null, "", String.valueOf(tEnd), out,false);
				//break;
			}
			else if(parameter.startsWith("sourceFileName=>")){
				if(parameter.length()<="sourceFileName=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file containing the model. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				sourceFileName = parameter.substring("sourceFileName=>".length(), parameter.length());
			}
			else if(parameter.startsWith("unionFileName=>")){
				if(parameter.length()<="unionFileName=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file containing the union of the source and target models. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				unionFileName = parameter.substring("unionFileName=>".length(), parameter.length());
			}
			else if(parameter.startsWith("bound=>")){
				if(parameter.length()<="bound=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the bound to be used (0 for no bound). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				bound = Integer.valueOf(parameter.substring("bound=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("symbolicJacobian=>")){
				if(parameter.length()<="symbolicJacobian=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if matlab should compute the Jacobian (true), or if thesyntactic algorithm for polynomial ODEs should be used (false). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				writeSymbolicJacobian = Boolean.valueOf(parameter.substring("symbolicJacobian=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("print=>")){
				if(parameter.length()<="print=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if the computed differential equivalences should be printed by matlab. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				print = Boolean.valueOf(parameter.substring("print=>".length(), parameter.length()));
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
		if(sourceFileName ==null || sourceFileName.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file containing the model. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		
		if(crn==null){
			//CRNReducerCommandLine.println(out,bwOut,"Before writing a model in a file it is necessary to load it. "); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			//CRNReducerCommandLine.println(out,bwOut,"I skip the write command in file: "+fileName);
			CRNReducerCommandLine.println(out,bwOut,"No model loaded. I skip the write command in file: "+fileName); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		
		boolean verbose=false;
		CRNReducerCommandLine.print(out,bwOut,"Writing the CAGE script to file "+ fileName+" ...");
		CreateCAGEScript cage = new CreateCAGEScript();
		cage.printCAGEScriptToMatlabFIle(crn, fileName, verbose, out, bwOut, messageDialogShower, terminator, writeSymbolicJacobian, bound, print, backward, sourceFileName, unionFileName);
		CRNReducerCommandLine.println(out,bwOut," completed");
		
	}
	
	private void handleDecompressCommand(String command, MessageConsoleStream out, BufferedWriter bwOut, boolean old) throws UnsupportedFormatException {
		HashMap<String, Integer> speciesToLimit=new HashMap<String, Integer>(0);
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		
		SupportedFormats format = (fromGUI)?SupportedFormats.CRNGUI:SupportedFormats.CRN;
		
		ODEorNET crnGUIFormat = null;
		if(crn!=null){
			crnGUIFormat = crn.getMdelDefKind();
		}
		
		String fileName=null;
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
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .m file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				fileName = parameter.substring("fileOut=>".length(), parameter.length());
				//writeCRN(fileName,crn,partition,SupportedFormats.MatalbArbitraryODEs,null,"",out,bwOut);
				//writeCRN(fileName,crn, partition, SupportedFormats.MatalbArbitraryODEs, null, "", String.valueOf(tEnd), out,false);
				//break;
			}
			else if(parameter.startsWith("limits=>")){
				if(parameter.length()<="limits=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the limits of the variables. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String[] par = parameter.substring("limits=>".length(), parameter.length()).split(";");
				speciesToLimit=new HashMap<String, Integer>(par.length);
				for(int i=0;i<par.length;i++){
					int colon=par[i].indexOf(':');
					String name=par[i].substring(0,colon);
					String val=par[i].substring(colon+1);
					speciesToLimit.put(name, Integer.valueOf(val));
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
		if(speciesToLimit ==null || speciesToLimit.size()==0){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify at least the limit for one species. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		
		if(crn==null){
			//CRNReducerCommandLine.println(out,bwOut,"Before writing a model in a file it is necessary to load it. "); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			//CRNReducerCommandLine.println(out,bwOut,"I skip the write command in file: "+fileName);
			CRNReducerCommandLine.println(out,bwOut,"No model loaded. I skip the write command in file: "+fileName); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		
		long begin = System.currentTimeMillis();
		CRNReducerCommandLine.print(out,bwOut,"Decompressing the model " + crn.getName());
		Decode decode = new Decode();
		ICRN decodedCRN = decode.decode(crn, fileName, verbose, out, bwOut, messageDialogShower, terminator, speciesToLimit);
		long end = System.currentTimeMillis();
		CRNReducerCommandLine.println(out,bwOut,"\tCompleted in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)"+".\n\t  Variables: "+decodedCRN.getSpecies().size() +"\n\t  Transitions: "+decodedCRN.getReactions().size()+".");
		//CRNReducerCommandLine.println(out,bwOut," completed");
		
		//CRNReducerCommandLine.print(out,bwOut,"Writing the decompressed model in "+ fileName+" ...");
		//WE HAVE TO CREATE A NEW PARTITION FOR THE DECODED MODEL
		writeCRN(fileName,decodedCRN,partition,format,null,"",null,out,bwOut,crnGUIFormat,false,null,false,false);
		//CRNReducerCommandLine.println(out,bwOut," completed");
	}
	
	private void handleExportPontryaginMethodCommand(String command, MessageConsoleStream out, BufferedWriter bwOut, boolean old) throws UnsupportedFormatException {
		double tEnd = 5;
		//LinkedHashSet<String> paramsToPerturb=new LinkedHashSet<String>();
		String[] paramsToPerturb=null;
		double[] lows=null;
		double[] highs=null;
		HashMap<String, Integer> parameterToPerturbToItsPosition=null;
		HashMap<String, Double> speciesToCoefficient=new HashMap<String, Double>(0);
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		
		boolean plot=false;
		
		//boolean maximize=true;
		double step=8;
		double epsilon = 0.001;//This is delta in the old utopic
		double delta = 0.001;
		int kMax=400;
		boolean writeSymbolicJacobian=false;
		double maxStep=tEnd/10.0;
		boolean maxStepSet=false;
		
		double integrationStep=tEnd/10.0;
		boolean integrationStepSet=false;
		
		boolean exitMatlab=false;
		boolean runMatlab=true;
		boolean compileAndRunVNODE=true;
		
		String fileName=null;
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for (String parameter : parameters) {
			if(parameter.startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameter,out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return;
				}
			}
			else if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("fileOut=>")){
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .m file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				fileName = parameter.substring("fileOut=>".length(), parameter.length());
				//writeCRN(fileName,crn,partition,SupportedFormats.MatalbArbitraryODEs,null,"",out,bwOut);
				//writeCRN(fileName,crn, partition, SupportedFormats.MatalbArbitraryODEs, null, "", String.valueOf(tEnd), out,false);
				//break;
			}
			else if(parameter.startsWith("paramsToPerturb=>")){
				if(parameter.length()<="paramsToPerturb=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the set of parameters to perturb. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String[] par = parameter.substring("paramsToPerturb=>".length(), parameter.length()).split(";");
				paramsToPerturb=new String[par.length];
				lows=new double[par.length];
				highs=new double[par.length];
				parameterToPerturbToItsPosition=new HashMap<>(par.length);
				for(int i=0;i<par.length;i++){
					int posOfSB = par[i].indexOf('[');
					int posOfColon = par[i].indexOf(':');
					paramsToPerturb[i]=par[i].substring(0,posOfSB);
					lows[i]=crn.getMath().evaluate(par[i].substring(posOfSB+1, posOfColon));
					highs[i]=crn.getMath().evaluate(par[i].substring(posOfColon+1,par[i].length()-1));
					if(lows[i]>=highs[i]){
						String message = "Please, specify a proper bound for parameter "+paramsToPerturb[i]+". The lower bound ("+lows[i]+") is biiger than the higher one ("+highs[i]+").";
						//CRNReducerCommandLine.println(out,bwOut,message);
						CRNReducerCommandLine.printWarning(out, bwOut, true, messageDialogShower, message, DialogType.Error);
						CRNReducerCommandLine.println(out,bwOut,message);
						//CRNReducerCommandLine.println(out,bwOut,"Please, specify a proper bound for parameter: "+paramsToPerturb[i]+" in ["+lows[i]+","+highs[i]+"]. ");
						CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
						return;
					}
					parameterToPerturbToItsPosition.put(paramsToPerturb[i], i);
				}
				//writeCRN(fileName,crn,partition,SupportedFormats.MatalbArbitraryODEs,null,"",out,bwOut);
				//writeCRN(fileName,crn, partition, SupportedFormats.MatalbArbitraryODEs, null, "", String.valueOf(tEnd), out,false);
				//break;
			}
			else if(parameter.startsWith("coefficients=>")){
				if(parameter.length()<="coefficients=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the coefficients of the variables. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String[] par = parameter.substring("coefficients=>".length(), parameter.length()).split(";");
				speciesToCoefficient=new HashMap<String, Double>(par.length);
				for(int i=0;i<par.length;i++){
					int colon=par[i].indexOf(':');
					String name=par[i].substring(0,colon);
					String val=par[i].substring(colon+1);
					speciesToCoefficient.put(name, crn.getMath().evaluate(val));
				}
			}
			else if(parameter.startsWith("tEnd=>")){
				if(parameter.length()<="tEnd=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the time horizon. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				tEnd = Double.valueOf(parameter.substring("tEnd=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("maxStep=>")){
				if(parameter.length()<="maxStep=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the maximum time step. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				maxStep = Double.valueOf(parameter.substring("maxStep=>".length(), parameter.length()));
				maxStepSet=true;
			}
			else if(parameter.startsWith("integrationStep=>")){
				if(parameter.length()<="integrationStep=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the integration time step. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				integrationStep = Double.valueOf(parameter.substring("integrationStep=>".length(), parameter.length()));
				integrationStepSet=true;
			}
			else if(parameter.startsWith("symbolicJacobian=>")){
				if(parameter.length()<="symbolicJacobian=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if matlab should compute the Jacobian (true), or if thesyntactic algorithm for polynomial ODEs should be used (false). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				writeSymbolicJacobian = Boolean.valueOf(parameter.substring("symbolicJacobian=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("kMax=>")){
				if(parameter.length()<="kMax=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the maximum number of iterations. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				kMax = Integer.valueOf(parameter.substring("kMax=>".length(), parameter.length()));
			}
//			else if(parameter.startsWith("maximize=>")){
//				if(parameter.length()<="maximize=>".length()){
//					CRNReducerCommandLine.println(out,bwOut,"Please, specify if interested in maximining or minimizing. ");
//					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
//					return;
//				}
//				maximize = Boolean.valueOf(parameter.substring("maximize=>".length(), parameter.length()));
//			}
			else if(parameter.startsWith("plot=>")){
				if(parameter.length()<="plot=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if interested in plotting or not. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				plot = Boolean.valueOf(parameter.substring("plot=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("exitMatlab=>")){
				if(parameter.length()<="exitMatlab=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if Matlab should be closed after execution. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				exitMatlab = Boolean.valueOf(parameter.substring("exitMatlab=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("runMatlab=>")){
				if(parameter.length()<="runMatlab=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if the Matlab script should be run. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				runMatlab = Boolean.valueOf(parameter.substring("runMatlab=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("compileAndRunVNODE=>")){
				if(parameter.length()<="compileAndRunVNODE=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if the Matlab script should be run. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				compileAndRunVNODE = Boolean.valueOf(parameter.substring("compileAndRunVNODE=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("epsilon=>")){
				if(parameter.length()<="epsilon=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the threshold. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				epsilon = Double.valueOf(parameter.substring("epsilon=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("delta=>")){
				if(parameter.length()<="delta=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the threshold. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				delta = Double.valueOf(parameter.substring("delta=>".length(), parameter.length()));
				if(delta==0){
					delta=-1;
				}
			}
			else if(parameter.startsWith("step=>")){
				if(parameter.length()<="step=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the TODO. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				step = Double.valueOf(parameter.substring("step=>".length(), parameter.length()));
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
		if(paramsToPerturb ==null || paramsToPerturb.length==0){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify at least one parameter to perturb. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		
		if(crn==null){
			//CRNReducerCommandLine.println(out,bwOut,"Before writing a model in a file it is necessary to load it. "); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			//CRNReducerCommandLine.println(out,bwOut,"I skip the write command in file: "+fileName);
			CRNReducerCommandLine.println(out,bwOut,"No model loaded. I skip the write command in file: "+fileName); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		
		if(!maxStepSet){
			maxStep=tEnd/10.0;
		}
		if(!integrationStepSet){
			integrationStep=tEnd/10.0;
		}
		
		String mFileName = AbstractImporter.overwriteExtensionIfEnabled(fileName,".m",true);
		CRNReducerCommandLine.print(out,bwOut,"Writing the Pontryagin script to file "+ mFileName+" ...");
		MatlabODEPontryaginExporter pontryaginExporter = new MatlabODEPontryaginExporter();
		if(old){
			pontryaginExporter.printPontryaginToMatlabFIle(crn, mFileName, verbose, out, bwOut, messageDialogShower,tEnd,parameterToPerturbToItsPosition,paramsToPerturb,lows,highs,terminator,speciesToCoefficient,writeSymbolicJacobian,kMax,epsilon,maxStep);
		}
		else{
			//Create Matlab script
			boolean ok=pontryaginExporter.printConverginPontryaginToMatlabFIle(crn, mFileName, verbose, out, bwOut, messageDialogShower,tEnd,parameterToPerturbToItsPosition,paramsToPerturb,lows,highs,terminator,speciesToCoefficient,writeSymbolicJacobian,kMax,delta,integrationStep,step,plot,exitMatlab,runMatlab,compileAndRunVNODE);
			if(ok) {
				CRNReducerCommandLine.println(out,bwOut," completed");
			}
			else {
				CRNReducerCommandLine.println(out,bwOut," FAILED");
				return;
			}
			CRNReducerCommandLine.println(out,bwOut,"");
			
			if(runMatlab) {
				//Execute Matlab script
				ok = JavaLibraryPathHandler.handleMatlabPath(command, out, bwOut,messageDialogShower);
				if(!ok){
					//there have been problems/exceptions in handling the matlab path. I already printed it inside handleMatlabPath. Now I just have to terminate
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				CRNReducerCommandLine.print(out,bwOut,"Executing the Pontryagin script  "+ mFileName+" ...");
				ok = pontryaginExporter.executeMatlabScript(mFileName, out, bwOut, messageDialogShower);
				if(ok) {
					CRNReducerCommandLine.println(out,bwOut," completed");
				}
				else {
					CRNReducerCommandLine.println(out,bwOut," FAILED");
					return;
				}
				CRNReducerCommandLine.println(out,bwOut);
			}
			
			//Create VNODE c script
			String cFileName = AbstractImporter.overwriteExtensionIfEnabled(fileName,".cc",true);
			CRNReducerCommandLine.print(out,bwOut,"Writing the VNODE_LP script to file "+ cFileName+" ...");
			
			VNODELPExporter vnodelpexp = new VNODELPExporter();
			ok = vnodelpexp.printConverginPontryaginToVNodeLPFIle(crn, cFileName, verbose, out, bwOut, messageDialogShower, tEnd, parameterToPerturbToItsPosition, paramsToPerturb, lows, highs, speciesToCoefficient);
			if(ok) {
				CRNReducerCommandLine.println(out,bwOut," completed");
			}
			else {
				CRNReducerCommandLine.println(out,bwOut," FAILED");
				return;
			}

			//REMOVE REMOVE REMOVE REMOVE REMOVE REMOVE REMOVE REMOVE REMOVE REMOVE REMOVE
			//String fakeFileName = fileName.substring(0,fileName.lastIndexOf(File.separator));
			//fakeFileName= fakeFileName + File.separator + "test.cc";
			//cFileName=fakeFileName;
			//REMOVE REMOVE REMOVE REMOVE REMOVE REMOVE REMOVE REMOVE REMOVE REMOVE REMOVE

			if(compileAndRunVNODE){
				//handleVNODEPath
				String vnodePath = JavaLibraryPathHandler.handleVNODEPath(command, out, bwOut,messageDialogShower);
				ok= vnodePath !=null && (!vnodePath.equals(JavaLibraryPathHandler.UNSPECIFIEDPATH));
				if(!ok){
					//there have been problems/exceptions in handling the matlab path. I already printed it inside handleMatlabPath. Now I just have to terminate
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				if(!vnodePath.endsWith(File.separator)) {
					vnodePath+=File.separator;
				}

				//			///REMOVE
				//			CRNReducerCommandLine.println(out,bwOut,"AAAAAA");
				//			ok = vnodelpexp.executeVNODELPScript("/Users/anvan/OneDrive/OneDrive - Danmarks Tekniske Universitet/runtimes/runtime-ERODE.product(4)/VanderPol/testcc.cc", out, bwOut, messageDialogShower);
				//			CRNReducerCommandLine.println(out,bwOut,"AAAAAA");
				//			///REMOVE


				CRNReducerCommandLine.println(out,bwOut);

				CRNReducerCommandLine.print(out,bwOut,"Compiling the VNODE_LP script "+ cFileName+" ...");
				ok = vnodelpexp.compileVNODELPScript(cFileName, vnodePath,out, bwOut, messageDialogShower);
				if(ok) {
					CRNReducerCommandLine.println(out,bwOut," completed");
				}
				else {
					CRNReducerCommandLine.println(out,bwOut," FAILED");
					return;
				}

				CRNReducerCommandLine.println(out,bwOut);
				String execFileName = AbstractImporter.overwriteExtensionIfEnabled(cFileName,"",true);
				CRNReducerCommandLine.print(out,bwOut,"Executing the VNODE_LP script "+ execFileName+" ...");
				ok = vnodelpexp.executeVNODELPScript(cFileName, out, bwOut, messageDialogShower);
				if(ok) {
					CRNReducerCommandLine.println(out,bwOut,"Execution completed");
				}
				else {
					CRNReducerCommandLine.println(out,bwOut," FAILED");
					return;
				}
			}
		}
		
	}
	
	private void handleExportPontryaginPolygonMethodCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		double tEnd = 5;
		//LinkedHashSet<String> paramsToPerturb=new LinkedHashSet<String>();
		String[] paramsToPerturb=null;
		double[] lows=null;
		double[] highs=null;
		HashMap<String, Integer> parameterToPerturbToItsPosition=null;
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		String coraLibrary=JavaLibraryPathHandler.UNSPECIFIEDPATH;
		boolean cora=false;
		
		double sl1=0.5;
		double sl2=4;
		String firstSpecies=null;
		String secondSpecies=null;
		
		String fileName=null;
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for (String parameter : parameters) {
			if(parameter.startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameter,out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return;
				}
			}
			else if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("fileOut=>")){
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .m file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				fileName = parameter.substring("fileOut=>".length(), parameter.length());
				//writeCRN(fileName,crn,partition,SupportedFormats.MatalbArbitraryODEs,null,"",out,bwOut);
				//writeCRN(fileName,crn, partition, SupportedFormats.MatalbArbitraryODEs, null, "", String.valueOf(tEnd), out,false);
				//break;
			}
			else if(parameter.startsWith("slope1=>")){
				if(parameter.length()<="slope1=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, provide the slope of the first line we use to compute the polyogn of the reachable set. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String s = parameter.substring("slope1=>".length(), parameter.length()).trim();
				sl1=Double.valueOf(s);
			}
			else if(parameter.startsWith("slope2=>")){
				if(parameter.length()<="slope2=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, provide the slope of the second line we use to compute the polyogn of the reachable set. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String s = parameter.substring("slope2=>".length(), parameter.length()).trim();
				sl2=Double.valueOf(s);
			}
			else if(parameter.startsWith("firstSpecies=>")){
				if(parameter.length()<="firstSpecies=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, provide the name of the first species. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				firstSpecies = parameter.substring("firstSpecies=>".length(), parameter.length()).trim();
			}
			else if(parameter.startsWith("secondSpecies=>")){
				if(parameter.length()<="secondSpecies=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, provide the name of the second species. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				secondSpecies = parameter.substring("secondSpecies=>".length(), parameter.length()).trim();
			}
			else if(parameter.startsWith("cora=>")){
				if(parameter.length()<="cora=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if also CORA should be used library. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String c = parameter.substring("cora=>".length(), parameter.length()).trim();
				if(c.equalsIgnoreCase("true")){
					cora=true;
				}
			}
			else if(parameter.startsWith("coraLibrary=>")){
				if(parameter.length()<="coraLibrary=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the path of of the CORA library. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				coraLibrary = parameter.substring("coraLibrary=>".length(), parameter.length());
				if(coraLibrary.endsWith(File.separator)){
					coraLibrary=coraLibrary.substring(0, coraLibrary.length()-1);
				}
			}
			else if(parameter.startsWith("paramsToPerturb=>")){
				if(parameter.length()<="paramsToPerturb=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the set of parameters to perturb. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String[] par = parameter.substring("paramsToPerturb=>".length(), parameter.length()).split(";");
				paramsToPerturb=new String[par.length];
				lows=new double[par.length];
				highs=new double[par.length];
				parameterToPerturbToItsPosition=new HashMap<>(par.length);
				for(int i=0;i<par.length;i++){
					int posOfSB = par[i].indexOf('[');
					int posOfColon = par[i].indexOf(':');
					paramsToPerturb[i]=par[i].substring(0,posOfSB);
					lows[i]=crn.getMath().evaluate(par[i].substring(posOfSB+1, posOfColon));
					highs[i]=crn.getMath().evaluate(par[i].substring(posOfColon+1,par[i].length()-1));
					if(lows[i]>=highs[i]){
						String message = "Please, specify a proper bound for parameter "+paramsToPerturb[i]+". The lower bound ("+lows[i]+") is biiger than the higher one ("+highs[i]+").";
						//CRNReducerCommandLine.println(out,bwOut,message);
						CRNReducerCommandLine.printWarning(out, bwOut, true, messageDialogShower, message, DialogType.Error);
						CRNReducerCommandLine.println(out,bwOut,message);
						//CRNReducerCommandLine.println(out,bwOut,"Please, specify a proper bound for parameter: "+paramsToPerturb[i]+" in ["+lows[i]+","+highs[i]+"]. ");
						CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
						return;
					}
					parameterToPerturbToItsPosition.put(paramsToPerturb[i], i);
				}
				//writeCRN(fileName,crn,partition,SupportedFormats.MatalbArbitraryODEs,null,"",out,bwOut);
				//writeCRN(fileName,crn, partition, SupportedFormats.MatalbArbitraryODEs, null, "", String.valueOf(tEnd), out,false);
				//break;
			}
			else if(parameter.startsWith("tEnd=>")){
				if(parameter.length()<="tEnd=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the time horizon. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				tEnd = Double.valueOf(parameter.substring("tEnd=>".length(), parameter.length()));
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
		if(paramsToPerturb ==null || paramsToPerturb.length==0){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify at least one parameter to perturb. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		if(firstSpecies==null || secondSpecies==null || firstSpecies.equals("")|| secondSpecies.equals("")){
			String message = "Please provide the two species to be considered.";
			CRNReducerCommandLine.printWarning(out, bwOut, true, messageDialogShower, message, DialogType.Error);
			CRNReducerCommandLine.println(out,bwOut,message);
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		if(sl1>=sl2){
			String message = "It is required that sl1<sl2, while we have sl1="+sl1+", and sl2="+sl2+".";
			CRNReducerCommandLine.printWarning(out, bwOut, true, messageDialogShower, message, DialogType.Error);
			CRNReducerCommandLine.println(out,bwOut,message);
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		//writeCRN(fileName,crn, partition, SupportedFormats.MatlabEpsilonBounds, null, "","", out,false);
		if(cora && (coraLibrary==null || coraLibrary.equals(JavaLibraryPathHandler.UNSPECIFIEDPATH))){
			try {
				coraLibrary=JavaLibraryPathHandler.getRequiredLibrary(MatlabODEPontryaginExporter.CORADOWNLOADMESSAGE, MatlabODEPontryaginExporter.CORALOCATEMESSAGE, MatlabODEPontryaginExporter.FILEWITHCORA_2016LOCATION, MatlabODEPontryaginExporter.CORADOWNLOADPATH, MatlabODEPontryaginExporter.CORADOWNLOADPATHSHORT,out,bwOut,messageDialogShower);
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Exception while retrieving the path of the CORA library.");
				CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
				CRNReducerCommandLine.printExceptionShort(out, bwOut, e);
				return;
			}
			if(coraLibrary==null || coraLibrary.equals(JavaLibraryPathHandler.UNSPECIFIEDPATH)){
				CRNReducerCommandLine.println(out,bwOut,"Could not retrieve the path of the CORA library.");
				CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
				return;
			}
		}
		
		if(crn==null){
			//CRNReducerCommandLine.println(out,bwOut,"Before writing a model in a file it is necessary to load it. "); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			//CRNReducerCommandLine.println(out,bwOut,"I skip the write command in file: "+fileName);
			CRNReducerCommandLine.println(out,bwOut,"No model loaded. I skip the write command in file: "+fileName); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		
		//boolean writeSymbolicJacobian=false;
		boolean writeSymbolicJacobian=true;
		CRNReducerCommandLine.print(out,bwOut,"Writing the Pontryagin script to file "+ fileName+" ...");
		MatlabODEPontryaginExporter pointryaginExporter = new MatlabODEPontryaginExporter();
		pointryaginExporter.printPontryaginPolygonToMatlabFIle(crn, fileName, verbose, out, bwOut, messageDialogShower,tEnd,parameterToPerturbToItsPosition,paramsToPerturb,lows,highs,terminator,cora,coraLibrary,sl1,sl2,firstSpecies,secondSpecies,writeSymbolicJacobian);
		CRNReducerCommandLine.println(out,bwOut," completed");
	}
	
	protected IPartitionAndBooleanAndAxB handleApproximationCommand(String command, String reduction, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		String epsilonString = "0";
		boolean epsilonSpecified = false;
		LinkedHashSet<String> paramsToPerturb=new LinkedHashSet<String>();
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		
		boolean fastDegreeOneBDE=false; 
		
		String prePartitionWRTIC="false";
		String prePartitionUserDefined="false";
		
		boolean computeOnlyPartition=false;
		boolean print=true;
		if(print) {
			print=true;
		}
		
		boolean forward=false;
		boolean backward=false;
		if(reduction.equals("forward")){
			forward=true;
		}
		else if(reduction.equals("backward")){
			backward=true;
		} 
		//reduction="forward" or "backward"
	
		String fileName=null;
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return null;
		}
		for (String parameter : parameters) {
			if(parameter.startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameter,out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return null;
				}
			}
			else if(parameter.startsWith("computeOnlyPartition=>")){
				if(parameter.length()<="computeOnlyPartition=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if or not only the partition has to be computed (without thus reducing the model). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				computeOnlyPartition = parameter.substring("computeOnlyPartition=>".length(), parameter.length()).equalsIgnoreCase("true");
			}
			else if(parameter.startsWith("print=>")){
				if(parameter.length()<="print=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if you want to print information.");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				if(parameter.substring("print=>".length(), parameter.length()).equalsIgnoreCase("false")){
					print=false;
				}
			}
			else if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("prePartition=>")){
				if(parameter.length()<="prePartition=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if you want to prepartion the species of the model according to their initial conditions or to the views. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				String prep = parameter.substring("prePartition=>".length(), parameter.length()).trim();
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
				else if(prep.compareToIgnoreCase("USER_and_IC")==0){
					prePartitionWRTIC = "true";
					prePartitionUserDefined = "true";
				}
				else{
					CRNReducerCommandLine.println(out,bwOut,"Unknown prepartitioning option \""+prep+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
					return null;
				}
			}
			else if(parameter.startsWith("matlabScript=>")){
				if(parameter.length()<="matlabScript=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .m file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				fileName = parameter.substring("matlabScript=>".length(), parameter.length());
			}
			else if(parameter.startsWith("paramsToPerturb=>")){
				if(parameter.length()<="paramsToPerturb=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the set of parameters to perturb. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				String[] par = parameter.substring("paramsToPerturb=>".length(), parameter.length()).split(";");
				for(int i=0;i<par.length;i++){
					paramsToPerturb.add(par[i]);
				}
			}
			else if(parameter.startsWith("epsilon=>")){
				if(parameter.length()<="epsilon=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the epsilon value. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				epsilonString = parameter.substring("epsilon=>".length(), parameter.length());
				epsilonSpecified=true;
			}
			else if(parameter.startsWith("fastDegreeOne=>")){
				if(parameter.length()<="fastDegreeOne=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if we should use the fast BDE for degree one models (e.g., networks or affine systems without B). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				fastDegreeOneBDE = parameter.substring("fastDegreeOne=>".length(), parameter.length()).equalsIgnoreCase("true");
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameter+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return null;
			}
		}
		if((!computeOnlyPartition) && (fileName ==null || fileName.equals(""))){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write the matlab script. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return null;
		}
		//writeCRN(fileName,crn, partition, SupportedFormats.MatlabEpsilonBounds, null, "","", out,false);
		
		if(crn==null){
			CRNReducerCommandLine.println(out,bwOut,"No model loaded. I skip the write command in file: "+fileName); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return null;
		}
		
		double epsilon=0;
		if(epsilonSpecified){
			epsilon = crn.getMath().evaluate(epsilonString);
			if(epsilon<0){
				CRNReducerCommandLine.printWarning(out, bwOut, true, messageDialogShower, "Please, specify a non-negative epsilon ("+epsilon+").\n\t I skip this command.",DialogType.Error);
				return null;
			}
		}
		
		String forback="backward";
		if(forward) {
			forback="forward";
		}
		CRNReducerCommandLine.print(out,bwOut,"Computing "+epsilon+"-DE "+forback+"...");
		if(!computeOnlyPartition)
			CRNReducerCommandLine.print(out,bwOut," Writing the matlab script to file "+ fileName+" ...");
		IPartitionAndBooleanAndAxB ret = MatlabODEsImporter.printEpsilonScriptToMatlabFIle(crn, partition, fileName, verbose, out, bwOut, messageDialogShower,paramsToPerturb,terminator,epsilon,prePartitionUserDefined,prePartitionWRTIC,forward,backward,fastDegreeOneBDE,computeOnlyPartition);
		CRNReducerCommandLine.println(out,bwOut," completed");
		return ret;
	}
	
	private void handleGenerateCMECommand(String command, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		//exportBNG({file=>outputfileName.crn})
		String fileName = crn.getName()+"cme._ode"; 
		HashMap<String, Integer> speciesToLimit=new HashMap<String, Integer>(0);
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for (String parameter : parameters) {
			if(parameter.startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameter,out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return;
				}
			}
			else if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("limits=>")){
				if(parameter.length()<="limits=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the limits of the variables. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String[] par = parameter.substring("limits=>".length(), parameter.length()).split(";");
				speciesToLimit=new HashMap<String, Integer>(par.length);
				for(int i=0;i<par.length;i++){
					int colon=par[i].indexOf(':');
					String name=par[i].substring(0,colon);
					String val=par[i].substring(colon+1);
					speciesToLimit.put(name, Integer.valueOf(val));
				}
			}
			else if(parameter.startsWith("fileOut=>")){
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .ode file where to write the generated CTMC. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				fileName = parameter.substring("fileOut=>".length(), parameter.length());
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
		
		if(crn==null){
			//CRNReducerCommandLine.println(out,bwOut,"Before writing a model in a file it is necessary to load it. "); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			//CRNReducerCommandLine.println(out,bwOut,"I skip the write command in file: "+fileName);
			CRNReducerCommandLine.println(out,bwOut,"No model loaded. I skip the command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		
		IComposite initialState=CTMCGenerator.computeInitialState(crn);
		CRNReducerCommandLine.print(out,bwOut,"Generating the CTMC of "+crn.getName()+" for initial state "+initialState+" ...");
		long begin = System.currentTimeMillis();
		
		CTMCGenerator ctmcGen = new CTMCGenerator();
		ICRN ctmc = ctmcGen.generateCTMC(crn, speciesToLimit, out, bwOut,verbose,terminator,messageDialogShower);
		
		if(ctmc!=null){
			IPartition trivialButForInitialState = new Partition(ctmc.getSpecies().size());
			IBlock blockInitial = new Block();
			trivialButForInitialState.add(blockInitial);
			blockInitial.addSpecies(ctmc.getSpecies().get(0));
			
			IBlock block = new Block();
			trivialButForInitialState.add(block);
			boolean first=true;
			for (ISpecies species : ctmc.getSpecies()) {
				if(first){
					first =false;
				}
				else{
					block.addSpecies(species);
				}
			}

			Collection<String> preamble=new ArrayList<String>(10);
			preamble.add(" CTMC Automatically generated from "+crn.getName()+".");
			preamble.add(" Initial state/population: "+initialState);
			preamble.add(" Original number of species: "+crn.getSpecies().size());
			preamble.add(" Original number of reactions: "+crn.getReactions().size());
			preamble.add(" Obtained states: "+ctmc.getSpecies().size());
			preamble.add(" Obtained transitions: "+ctmc.getReactions().size());
								
			/*String commentSymbol=(fromGUI)?"//":"#";
			StringBuilder sb = new StringBuilder();
			sb.append(commentSymbol+" CTMC Automatically generated from ");
			sb.append(crn.getName());
			sb.append(".\n"+commentSymbol+" Initial state/population: "+initialState);
			sb.append(".\n"+commentSymbol+" Original number of species: ");
			sb.append(crn.getSpecies().size());
			sb.append(".\n"+commentSymbol+" Original number of reactions: ");
			sb.append(crn.getReactions().size());
			sb.append(".\n"+commentSymbol+" Obtained states: ");
			sb.append(ctmc.getSpecies().size());
			sb.append(".\n"+commentSymbol+" Obtained transitions: ");
			sb.append(ctmc.getReactions().size());*/
			long end = System.currentTimeMillis();

			//CRNReducerCommandLine.println(out,bwOut," completed. The "+crn.getSpecies().size()+" species have been prepartitioned in "+initial.size()+" blocks. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");
			CRNReducerCommandLine.println(out,bwOut," completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)"+".\n\tStates: "+ctmc.getSpecies().size() +"\n\tTransitions: "+ctmc.getReactions().size()+".");	

			writeCRN(fileName,ctmc,trivialButForInitialState,(fromGUI)?SupportedFormats.CRNGUI:SupportedFormats.CRN,preamble,"",null,out,bwOut,false,null);
		}
	}
	
	private void handleGenerateLogsCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		//exportBNG({file=>outputfileName.crn})
		String fileName = crn.getName()+"cme._ode"; 
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		int sots=0;
		int steps=0;
		int simulations=0;
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for (String parameter : parameters) {
			if(parameter.startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameter,out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return;
				}
			}
			else if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("fileOut=>")){
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .ode file where to write the generated CTMC. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				fileName = parameter.substring("fileOut=>".length(), parameter.length());
			}
			else if(parameter.startsWith("sots=>")){
				if(parameter.length()<="sots=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the seed to be used to generate a seed per simulation. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				sots = Integer.valueOf(parameter.substring("sots=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("simulations=>")){
				if(parameter.length()<="simulations=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the number of simulations to be performed. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				simulations = Integer.valueOf(parameter.substring("simulations=>".length(), parameter.length()));
			}
			else if(parameter.startsWith("steps=>")){
				if(parameter.length()<="steps=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the number of steps of each simulation to be performed. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				steps = Integer.valueOf(parameter.substring("steps=>".length(), parameter.length()));
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

		if(crn==null){
			//CRNReducerCommandLine.println(out,bwOut,"Before writing a model in a file it is necessary to load it. "); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			//CRNReducerCommandLine.println(out,bwOut,"I skip the write command in file: "+fileName);
			CRNReducerCommandLine.println(out,bwOut,"No model loaded. I skip the command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}

		IComposite initialState=CTMCGenerator.computeInitialState(crn);
		CRNReducerCommandLine.print(out,bwOut,"Generating "+simulations+" logs of "+steps+" steps each for "+crn.getName()+" for initial state "+initialState+" ...");
		long begin = System.currentTimeMillis();

		CTMCGenerator ctmcGen = new CTMCGenerator();
		ctmcGen.generateLogs(crn, simulations,steps, sots, fileName, out, bwOut, verbose, terminator, messageDialogShower);
		//ICRN ctmc = ctmcGen.generateCTMC(crn, speciesToLimit, out, bwOut,verbose,terminator,messageDialogShower);

		long end = System.currentTimeMillis();

		CRNReducerCommandLine.println(out,bwOut," completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s).\n");
	}
	
	private void handleExportFluidCompilerCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		//exportBNG({file=>outputfileName.crn})
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for (String parameter : parameters) {
			if(parameter.startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameter,out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return;
				}
			}
			else if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("fileOut=>")){
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .net file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String fileName = parameter.substring("fileOut=>".length(), parameter.length());
				writeCRN(fileName,crn,partition,SupportedFormats.FluidCompiler,null,"",null,out,bwOut,false,null);
				break;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameter+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return;
			}
		}
	}
	
	private void handleExportz3Command(String command, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		//exportBNG({file=>outputfileName.crn})
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		String fileName=null;
		String question=null;
		String whichPartition=null;
		for (String parameter : parameters) {
			if(parameter.startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameter,out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return;
				}
			}
			else if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("fileOut=>")){
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .net file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				fileName = parameter.substring("fileOut=>".length(), parameter.length());
			}
			else if(parameter.startsWith("question=>")){
				if(parameter.length()<="question=>".length()){	
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the condition to be checked on the current partition (OFL,EFL,EROFL,EREFL). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				question = parameter.substring("question=>".length(), parameter.length());
			}
			else if(parameter.startsWith("partition=>")){
				if(parameter.length()<="partition=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the partition to be checked (USER,ICOFL,EFL,EROFL,EREFL). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				whichPartition = parameter.substring("partition=>".length(), parameter.length());
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameter+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return;
			}
		}
		
		if(crn==null){
			CRNReducerCommandLine.println(out,bwOut,"Before exporting a model it is necessary to load it. "); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}

		if(question ==null || question.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the condition to be checked on the current partition (OFL,EFL,EROFL,EREFL). ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		
		if(fileName ==null || fileName.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .net file where to write. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return;
		}
		
		IPartition partitionToConsider=partition;
		if(whichPartition!=null && whichPartition.equalsIgnoreCase("USER")){
			partitionToConsider= CRNBisimulationsNAry.prepartitionUserDefined(crn, false, out,bwOut,terminator);
		}
		if(whichPartition!=null && whichPartition.equalsIgnoreCase("USER_and_IC")){
			partitionToConsider= CRNBisimulationsNAry.prepartitionUserDefined(crn, false, out,bwOut,terminator);
			partitionToConsider=ExactFluidBisimilarity.prepartitionWRTIC(crn,partitionToConsider,false,out,bwOut,terminator);
		}
		
		
		List<String> furtherParameters = new ArrayList<>(1);
		furtherParameters.add(question);
		writeCRN(fileName,crn,partitionToConsider,SupportedFormats.z3,null,"",furtherParameters,out,bwOut,false,null);
		
	}
	
	private void handleExportStochKitCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		//exportSBML({fileOut=>outputfileName})
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for (String parameter : parameters) {
			if(parameter.startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameter,out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return;
				}
			}
			else if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("fileOut=>")){
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .xml file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String fileName = parameter.substring("fileOut=>".length(), parameter.length());
				writeCRN(fileName,crn,partition,SupportedFormats.StochKit,null,"",null,out,bwOut,false,null);
				break;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameter+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return;
			}
		}
	}
	private void handleExportSBMLCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		//exportSBML({fileOut=>outputfileName})
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for (String parameter : parameters) {
			if(parameter.startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameter,out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return;
				}
			}
			else if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("fileOut=>")){
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .xml file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String fileName = parameter.substring("fileOut=>".length(), parameter.length());
				writeCRN(fileName,crn,partition,SupportedFormats.SBML,null,"",null,out,bwOut,false,null);
				break;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameter+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return;
			}
		}
	}

	private void handleExportLBSCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		//exportLBS({fileOut=>outputfileName.crn})
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for (String parameter : parameters) {
			if(parameter.startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameter,out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return;
				}
			}
			else if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("fileOut=>")){
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .lbs file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String fileName = parameter.substring("fileOut=>".length(), parameter.length());
				writeCRN(fileName,crn,partition,SupportedFormats.LBS,null,"",null,out,bwOut,false,null);
				break;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameter+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return;
			}
		}
	}

	private void handleExportFlyFastCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) {
		//exportFlyFast({fileOut=>outputfileName})
		String parameters[] = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		for (String parameter : parameters) {
			if(parameter.startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameter,out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return;
				}
			}
			else if(parameter.equals("")){
				continue;
			}
			else if(parameter.startsWith("fileOut=>")){
				if(parameter.length()<="fileOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the .xml file where to write. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return;
				}
				String fileName = parameter.substring("fileOut=>".length(), parameter.length());
				try {
					writeCRN(fileName,crn,partition,SupportedFormats.FlyFast,null,"",null,out,bwOut,false,null);
				} catch (UnsupportedFormatException e) {
					CRNReducerCommandLine.println(out,bwOut,"The model cannot be exported in FlyFast format. I skip this command.");
					CRNReducerCommandLine.println(out,bwOut,e.getMessage());
					return;
				}
				break;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameter+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return;
			}
		}
	}
	
	private boolean invokeLoad(String parameter, MessageConsoleStream out, BufferedWriter bwOut) {
		if(parameter.length()<=8){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file to read. ");
			return false;
		}
		return handleLoadCommand("load({"+parameter+"})",true,out,bwOut);
	}

	public boolean handleLoadCommand(String command,boolean print, MessageConsoleStream out, BufferedWriter bwOut) {
		return handleLoadCommand(command,print,false,out,bwOut);
	}
	
	protected boolean handleLoadCommand(String command,boolean print, boolean ignoreCommands, MessageConsoleStream out, BufferedWriter bwOut) {
		//sample command: load({fileName=>./CRNNetworks/am.crn})
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return false;
		}
		String fileName=null;
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("fileIn=>")){
				if(parameters[p].length()<="fileIn=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file to read. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				fileName = parameters[p].substring("fileIn=>".length(), parameters[p].length());
			}
			else if(parameters[p].equals("")){
				continue;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return false;
			}
		}
		if(fileName ==null || fileName.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the file to be loaded. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		try {
			loadFile(fileName,SupportedFormats.CRN,print,ignoreCommands,out,bwOut,false);
		} catch (FileNotFoundException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"I could not find the file "+fileName+". Loading failed.",DialogType.Error);
			return false;
		} catch (UnsupportedFormatException e) {
			if(e.getMessage()!=null){
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.\nError message:\n"+e.getMessage(),DialogType.Error);
			}
			else{
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.",DialogType.Error);
			}
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled IO error.\nError message:\n"+e.getMessage(),DialogType.Error);
			//CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled error.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (XMLStreamException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled error.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}
		return true;
	}

	private void handlePrintCommand(MessageConsoleStream out, BufferedWriter bwOut){
		if(crn==null){
			CRNReducerCommandLine.println(out,bwOut,"No model currently loaded. ");
		}
		else{
			//print the CRN
			String originalCRNShort="The current model: "+crn.toStringShort();
			CRNReducerCommandLine.println(out,bwOut,originalCRNShort);
			if(partition==null){
				CRNReducerCommandLine.println(out,bwOut,"No partition currently specified. ");
			}
			else{
				//print the partition
				CRNReducerCommandLine.println(out,bwOut,"The current partition has "+blockOrBlocks(partition.size()) +" blocks.");
			}
		}

	}

	private boolean handleSetParam(String command, MessageConsoleStream out, BufferedWriter bwOut){
		//sample command: setParameter({paramName=>p1,expr>5})
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return false;
		}
		String parameterName=null;
		String parameterExpr=null;
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("paramName=>")){
				if(parameters[p].length()<="paramName=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the parameter. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				parameterName = parameters[p].substring("paramName=>".length(), parameters[p].length());
			}
			else if(parameters[p].equals("")){
				continue;
			}
			else if(parameters[p].startsWith("expr=>")){
				if(parameters[p].length()<="expr=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the expression. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				parameterExpr = parameters[p].substring("expr=>".length(), parameters[p].length());
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return false;
			}
		}

		if(parameterName==null || parameterName.equals("") || parameterExpr==null || parameterExpr.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify both the name of the parameter and the new expression to assign to it.");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}

		if(crn==null){
			CRNReducerCommandLine.println(out,bwOut,"Before setting the value of a parameter it is necessary to load a model. "); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}

		double val = crn.setParameter(parameterName, parameterExpr);
		CRNReducerCommandLine.println(out,bwOut,"Parameter "+parameterName+" set to "/*+parameterExpr+"="*/+val);
		return true;
	}

	private boolean handleSetIC(String command, MessageConsoleStream out, BufferedWriter bwOut){
		////sample command: setIC({speciesName=>p1,value=>5})
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return false;
		}
		String speciesName=null;
		String icExpr=null;
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("speciesName=>")){
				if(parameters[p].length()<="speciesName=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the species. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				speciesName = parameters[p].substring("speciesName=>".length(), parameters[p].length());
			}
			else if(parameters[p].equals("")){
				continue;
			}
			else if(parameters[p].startsWith("expr=>")){
				if(parameters[p].length()<="expr=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the value. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				icExpr = parameters[p].substring("expr=>".length(), parameters[p].length());
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return false;
			}
		}

		if(speciesName==null || speciesName.equals("") || icExpr==null || icExpr.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify both the name of the species and the new expression to assign to it.");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}

		if(crn==null){
			CRNReducerCommandLine.println(out,bwOut,"Before setting the IC of a species it is necessary to load a model. "); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}

		double icValDouble = crn.getMath().evaluate(icExpr);
		boolean success = crn.setIC(speciesName,icValDouble,icExpr);
		if(success){
			CRNReducerCommandLine.println(out,bwOut,"IC of "+speciesName+" set to "+icValDouble);
		}
		else{
			CRNReducerCommandLine.println(out,bwOut,"IC not updated. Please check if the the species "+speciesName+" is defined in the current model.");
		}
		return success;
	}

	protected boolean handleImportBoolCubeSBMLCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) {
		//sample command: importBNG({fileName=>./CRNNetworks/am.net})
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return false;
		}
		String fileName=null;
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("fileIn=>")){
				if(parameters[p].length()<="fileIn=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file to read. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				fileName = parameters[p].substring("fileIn=>".length(), parameters[p].length());
			}
			else if(parameters[p].equals("")){
				continue;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return false;
			}
		}
		if(fileName ==null || fileName.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the file to be loaded. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		try {
			boolean failed = loadFile(fileName,SupportedFormats.BoolCubeSBML,true,out,bwOut,false,false);
			if(failed){
				printWarning(out, bwOut,true,messageDialogShower,"Loading failed:" +fileName,DialogType.Error);
				crn=null;
				partition=null;
			}
			/*else{
				printWarning(out, bwOut,"Loading of the model succeeded:" +fileName);
			}*/
		} catch (FileNotFoundException e) {
			//CRNReducerCommandLine.println(out,bwOut,"File not found: "+fileName,DialogType.Error);
			printWarning(out, bwOut,true,messageDialogShower,"File not found:" +fileName,DialogType.Error);
			return false;
		/*} catch (UnsupportedFormatException e) {
			//CRNReducerCommandLine.println(out,bwOut,"Loading of "+fileName+" failed.",DialogType.Error);
			if(e.getMessage()!=null){
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.\nError message:\n"+e.getMessage(),DialogType.Error);
			}
			else{
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.",DialogType.Error);
			}
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading failed due to unhandled error. This is the exception stack trace.");
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading failed due to unhandled error. This is the exception stack trace.");
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}
		*/
		} catch (UnsupportedFormatException e) {
			if(e.getMessage()!=null){
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.\nError message:\n"+e.getMessage(),DialogType.Error);
			}
			else{
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.",DialogType.Error);
			}
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled IO error.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled error.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}  catch (XMLStreamException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled error.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}

		return true;
	}
	
	protected boolean handleImportMatlabODEs(String command, MessageConsoleStream out, BufferedWriter bwOut) {
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return false;
		}
		String fileName=null;
		boolean polynomialODEs=false;
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("fileIn=>")){
				if(parameters[p].length()<="fileIn=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file to read. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				fileName = parameters[p].substring("fileIn=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("polynomialODEs=>")){
				if(parameters[p].length()<="polynomialODEs=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, if the ODEs to be imported are polynomial ODEs. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				
				String val = parameters[p].substring("polynomialODEs=>".length(), parameters[p].length());
				if(val.equalsIgnoreCase("true")){
					polynomialODEs=true;
				}
			}
			else if(parameters[p].equals("")){
				continue;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return false;
			}
		}
		if(fileName ==null || fileName.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the file to be loaded. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		try {
			boolean failed = true;
			if(polynomialODEs){
				failed = loadFile(fileName,SupportedFormats.MatlabPolynomialODEs,true,out,bwOut,false,false);
			}
			else{
				failed = loadFile(fileName,SupportedFormats.MatlabArbitraryODEs,true,out,bwOut,false,false);
			}
			if(failed){
				printWarning(out, bwOut,true,messageDialogShower,"Loading failed:" +fileName,DialogType.Error);
				crn=null;
				partition=null;
			}
			/*else{
				printWarning(out, bwOut,"Loading of the model succeeded:" +fileName);
			}*/
		/*} catch (FileNotFoundException e) {
			CRNReducerCommandLine.println(out,bwOut,"File not found: "+fileName,DialogType.Error);
			return false;
		} catch (UnsupportedFormatException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading of "+fileName+" failed.",DialogType.Error);
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading failed due to unhandled error. This is the exception stack trace.");
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading failed due to unhandled error. This is the exception stack trace.");
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}*/
		} catch (FileNotFoundException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"File not found: "+fileName,DialogType.Error);
			return false;
		} catch (UnsupportedFormatException e) {
			if(e.getMessage()!=null){
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.\nError message:\n"+e.getMessage(),DialogType.Error);
			}
			else{
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.",DialogType.Error);
			}
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled IO error.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled error.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}  catch (XMLStreamException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled error.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}
		
		
		return true;
	}
	
	protected boolean handleImportPalomaMomemntClosures(String command, MessageConsoleStream out, BufferedWriter bwOut) {
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return false;
		}
		String fluidFlowFile=null;
		/*String firstMomentFile=null;
		String secondMomentFile=null;*/
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("fluidFlow=>")){
				if(parameters[p].length()<="fluidFlow=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file containing the ODEs. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				fluidFlowFile = parameters[p].substring("fluidFlow=>".length(), parameters[p].length());
			}
			/*else if(parameters[p].startsWith("firstMoments=>")){
				if(parameters[p].length()<="firstMoments=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file specifying the first moments of interest. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				firstMomentFile = parameters[p].substring("firstMoment=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("secondMoments=>")){
				if(parameters[p].length()<="secondMoments=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file specifying the second moments of interest. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				secondMomentFile = parameters[p].substring("secondMoment=>".length(), parameters[p].length());
			}*/
			else if(parameters[p].equals("")){
				continue;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return false;
			}
		}
		if(fluidFlowFile ==null || fluidFlowFile.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file containing the ODEs. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		/*else if(firstMomentFile ==null || firstMomentFile.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file specifying the first moments of interest. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		else if(secondMomentFile ==null || secondMomentFile.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file specifying the second moments of interest. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}*/
		try {
			boolean failed = true;
			
			failed = loadFile(fluidFlowFile,SupportedFormats.PALOMAMomentClosure,true,false,new String[]{"firstMoment"},out,bwOut,false,false);
			if(failed){
				printWarning(out, bwOut,true,messageDialogShower,"Loading failed:" +fluidFlowFile,DialogType.Error);
				crn=null;
				partition=null;
			}
			/*else{
				printWarning(out, bwOut,"Loading of the model succeeded:" +fileName);
			}*/
		/*} catch (FileNotFoundException e) {
			CRNReducerCommandLine.println(out,bwOut,"I could not find the file "+fluidFlowFile+". Loading failed.");
			return false;
		} catch (UnsupportedFormatException e) {
			CRNReducerCommandLine.println(out,bwOut,"The model to load from "+fluidFlowFile+" is not supported. Loading failed.");
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading failed due to unhandled error. This is the exception stack trace.");
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading failed due to unhandled error. This is the exception stack trace.");
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}*/
		} catch (FileNotFoundException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"File not found: "+fluidFlowFile,DialogType.Error);
			return false;
		} catch (UnsupportedFormatException e) {
			if(e.getMessage()!=null){
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"File not supported: "+fluidFlowFile+".\nError message:\n"+e.getMessage(),DialogType.Error);
			}
			else{
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"File not supported: "+fluidFlowFile+"\nError message:\n"+e.getMessage(),DialogType.Error);
			}
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to IO errors.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled errors.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}  catch (XMLStreamException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled error.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}
		
		/*
		 	[T,Y]=ode45(@fluidflow,[0 100],[ 178 142 184 150 12 0 0 13 11 10 0 11 11 0 0 0 0 0 0 0 0 0 12 12 31684 20164 33856 22500 144 0 0 169 121 100 0 121 121 0 0 0 0 0 0 0 0 0 144 144 25276 32752 26700 2136 0 0 2314 1958 1780 0 1958 1958 0 0 0 0 0 0 0 0 0 2136 2136 26128 21300 1704 0 0 1846 1562 1420 0 1562 1562 0 0 0 0 0 0 0 0 0 1704 1704 27600 2208 0 0 2392 2024 1840 0 2024 2024 0 0 0 0 0 0 0 0 0 2208 2208 1800 0 0 1950 1650 1500 0 1650 1650 0 0 0 0 0 0 0 0 0 1800 1800 0 0 156 132 120 0 132 132 0 0 0 0 0 0 0 0 0 144 144 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 143 130 0 143 143 0 0 0 0 0 0 0 0 0 156 156 110 0 121 121 0 0 0 0 0 0 0 0 0 132 132 0 110 110 0 0 0 0 0 0 0 0 0 120 120 0 0 0 0 0 0 0 0 0 0 0 0 0 121 0 0 0 0 0 0 0 0 0 132 132 0 0 0 0 0 0 0 0 0 132 132 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 144]);
			plot(T,Y(:,4),'r-','LineWidth',2);
			hold on;
			plot(T,Y(:,2),'g-','LineWidth',2);
			hold on;
			plot(T,Y(:,3),'b-','LineWidth',2);
			hold on;
			plot(T,Y(:,1),'y-','LineWidth',2);
			hold on;
			xlabel('time');
			ylabel('value');
			legend('S(0,0)','S(0,1)','S(1,0)','S(1,1)'); 
		 */
		
		
		return true;
	}
	
	
	public boolean handleImportBNGCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) {
		//sample command: importBNG({fileName=>./CRNNetworks/am.net})
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return false;
		}
		String fileName=null;
		boolean compactNames=false;
		boolean lowMemory=true;
		boolean writeFileWithSpeciesNameCorrespondences=false;
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("fileIn=>")){
				if(parameters[p].length()<="fileIn=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file to read. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				fileName = parameters[p].substring("fileIn=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("writeFileWithSpeciesNameCorrespondences=>")){
				if(parameters[p].length()<="writeFileWithSpeciesNameCorrespondences=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if the file specifying the correspondence between the original species names and the automatically assigned ones has to be created. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				String val = parameters[p].substring("writeFileWithSpeciesNameCorrespondences=>".length(), parameters[p].length());
				if(val.equalsIgnoreCase("true")){
					writeFileWithSpeciesNameCorrespondences=true;
				}
			}
			else if(parameters[p].equals("")){
				continue;
			}
			else if(parameters[p].startsWith("compactNames=>")){
				if(parameters[p].length()<="compactNames=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify compact names sould be used (s + id). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				String val = parameters[p].substring("compactNames=>".length(), parameters[p].length());
				if(val.equalsIgnoreCase("true")){
					compactNames=true;
				}
			}
			else if(parameters[p].startsWith("lowMemory=>")){
				if(parameters[p].length()<="lowMemory=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if data structures with low memory footprint should be used. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				String val = parameters[p].substring("lowMemory=>".length(), parameters[p].length());
				if(val.equalsIgnoreCase("false")){
					lowMemory=false;
				}
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return false;
			}
		}
		if(fileName ==null || fileName.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the file to be loaded. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		try {
			boolean failed = loadFile(fileName,(lowMemory)?SupportedFormats.CompactBNG:SupportedFormats.BNG,true,out,bwOut,compactNames,writeFileWithSpeciesNameCorrespondences);
			
			if(failed){
				printWarning(out, bwOut,true,messageDialogShower,"Loading failed:" +fileName,DialogType.Error);
				crn=null;
				partition=null;
			}
			/*else{
				printWarning(out, bwOut,"Loading of the model succeeded:" +fileName);
			}*/
		/*} catch (FileNotFoundException e) {
			CRNReducerCommandLine.println(out,bwOut,"File not found: "+fileName,DialogType.Error);
			return false;
		} catch (UnsupportedFormatException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading of "+fileName+" failed.",DialogType.Error);
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading failed due to unhandled error. This is the exception stack trace.");
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading failed due to unhandled error. This is the exception stack trace.");
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}*/
		} catch (FileNotFoundException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"File not found:"+fileName+"\nError message:\n"+e.getMessage(),DialogType.Error);
			return false;
		} catch (UnsupportedFormatException e) {
			if(e.getMessage()!=null){
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.\nError message:\n"+e.getMessage(),DialogType.Error);
			}
			else{
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.",DialogType.Error);
			}
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to IO errors.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled errors.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}  catch (XMLStreamException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled error.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}
		
		return true;
	}
	
	protected boolean handleImportKonectCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) {
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return false;
		}
		String fileName=null;
		boolean ensureUndirectedGraph=false;
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("fileIn=>")){
				if(parameters[p].length()<="fileIn=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file to read. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				fileName = parameters[p].substring("fileIn=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("ensureUndirectedGraph=>")){
				if(parameters[p].length()<="ensureUndirectedGraph=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if I should ensure that the model represents an undirected graph or not. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				ensureUndirectedGraph = Boolean.valueOf(parameters[p].substring("ensureUndirectedGraph=>".length(), parameters[p].length()));
			}
			else if(parameters[p].equals("")){
				continue;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return false;
			}
		}
		if(fileName ==null || fileName.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the file to be loaded. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		try {
			boolean failed = loadFile(fileName,(ensureUndirectedGraph)?SupportedFormats.KonectEnsureUndirGraph:SupportedFormats.KonectDoNotEnsureUndirGraph,
					true,out,bwOut);
			
			if(failed){
				printWarning(out, bwOut,true,messageDialogShower,"Loading failed:" +fileName,DialogType.Error);
				crn=null;
				partition=null;
			}
			/*else{
				printWarning(out, bwOut,"Loading of the model succeeded:" +fileName);
			}*/
		/*} catch (FileNotFoundException e) {
			CRNReducerCommandLine.println(out,bwOut,"File not found: "+fileName,DialogType.Error);
			return false;
		} catch (UnsupportedFormatException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading of "+fileName+" failed.",DialogType.Error);
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading failed due to unhandled error. This is the exception stack trace.");
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading failed due to unhandled error. This is the exception stack trace.");
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}*/
		} catch (FileNotFoundException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"File not found:"+fileName+"\nError message:\n"+e.getMessage(),DialogType.Error);
			return false;
		} catch (UnsupportedFormatException e) {
			if(e.getMessage()!=null){
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.\nError message:\n"+e.getMessage(),DialogType.Error);
			}
			else{
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.",DialogType.Error);
			}
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to IO errors.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled errors.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}  catch (XMLStreamException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled error.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}
		
		return true;
	}
	
	protected boolean handleImportLinearSystemAsCSVMatrixCommand(String command, boolean compact, MessageConsoleStream out, BufferedWriter bwOut) {
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return false;
		}
		String fileName=null;
		String form=null;
		String bFile=null;
		String normalize="false";
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("fileIn=>")){
				if(parameters[p].length()<="fileIn=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file to read. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				fileName = parameters[p].substring("fileIn=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("form=>")){
				if(parameters[p].length()<="form=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the form of the matrix: A*X (or AX, for linear systems), or P*Q (or PQ, for Markov chain). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				form = parameters[p].substring("form=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("bFile=>")){
				if(parameters[p].length()<="bFile=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the file containing the b vector. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				bFile = parameters[p].substring("bFile=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("normalize=>")){
				if(parameters[p].length()<="normalize=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the form of the matrix: A*X (or AX, for linear systems), or P*Q (or PQ, for Markov chain). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				normalize = parameters[p].substring("normalize=>".length(), parameters[p].length());
			}
			else if(parameters[p].equals("")){
				continue;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return false;
			}
		}
		
		
		if(fileName ==null || fileName.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the file to be loaded. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		if(form ==null || form.equals("") || !(form.equalsIgnoreCase("pq")||form.equalsIgnoreCase("p*q")||form.equalsIgnoreCase("ax")||form.equalsIgnoreCase("a*x")||form.equalsIgnoreCase("fem"))){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the form of the matrix: A*X (or AX, for linear systems), or P*Q (or PQ, for Markov chain). ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		try {
			boolean failed = false;
			if(normalize.equalsIgnoreCase("true")){
				if(bFile==null){
					failed = loadFile(fileName,          SupportedFormats.CCSV,                     true, new String[]{"form",form,"normalize","true"},out,bwOut,false,false);
				}
				else{
					failed = loadFile(fileName,          SupportedFormats.CCSV,                     true, new String[]{"form",form,"normalize","true","bFile",bFile},out,bwOut,false,false);
				}
			}
			else{
				if(bFile==null){
					failed = loadFile(fileName,(compact)?SupportedFormats.CCSV:SupportedFormats.CSV,true,new String[]{"form",form}                    ,out,bwOut,false,false);
				}
				else{
					failed = loadFile(fileName,(compact)?SupportedFormats.CCSV:SupportedFormats.CSV,true,new String[]{"form",form,"bFile",bFile}                    ,out,bwOut,false,false);
				}
			}
			if(failed){
				printWarning(out, bwOut,true,messageDialogShower,"Loading failed:" +fileName,DialogType.Error);
				crn=null;
				partition=null;
			}
			/*else{
				printWarning(out, bwOut,"Loading of the model succeeded:" +fileName);
			}*/
		/*} catch (FileNotFoundException e) {
			CRNReducerCommandLine.println(out,bwOut,"File not found: "+fileName,DialogType.Error);
			return false;
		} catch (UnsupportedFormatException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading of "+fileName+" failed.",DialogType.Error);
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading failed due to unhandled error. This is the exception stack trace.");
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading failed due to unhandled error. This is the exception stack trace.");
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}*/
		} catch (FileNotFoundException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"File not found: "+fileName,DialogType.Error);
			return false;
		} catch (UnsupportedFormatException e) {
			if(e.getMessage()!=null){
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.\nError message:\n"+e.getMessage(),DialogType.Error);
			}
			else{
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.",DialogType.Error);
			}
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled IO errors.\nError message:\n"+e.getMessage(),DialogType.Error);
			//CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled errors.\nError message:\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}  catch (XMLStreamException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled error.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}
		return true;
	}
	
	protected boolean handleImportBioLayout(String command, MessageConsoleStream out, BufferedWriter bwOut) {
String[] parameters = CRNReducerCommandLine.getParameters(command);
		
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return false;
		}
		String fileName=null;
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("fileIn=>")){
				if(parameters[p].length()<="fileIn=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the BioLayout file. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				fileName = parameters[p].substring("fileIn=>".length(), parameters[p].length());
			}
			else if(parameters[p].equals("")){
				continue;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return false;
			}
		}
		
		
		if(fileName ==null || fileName.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the BioLayout file. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		
		try {
			boolean failed = false;
			failed = loadFile(fileName,SupportedFormats.BioLayout,true,null,out,bwOut,false,false);
			if(failed){
				printWarning(out, bwOut,true,messageDialogShower,"Loading failed:" +fileName,DialogType.Error);
				crn=null;
				partition=null;
			}
		} catch (FileNotFoundException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"File not found: "+fileName,DialogType.Error);
			return false;
		} catch (UnsupportedFormatException e) {
			if(e.getMessage()!=null){
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.\nError message:\n"+e.getMessage(),DialogType.Error);
			}
			else{
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.",DialogType.Error);
			}
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled IO errors.\nError message:\n"+e.getMessage(),DialogType.Error);
			//CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled errors.\nError message:\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}  catch (XMLStreamException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled error.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}
		
		
		
		return true;
	}

	protected boolean handleImportSpaceEx(String command, MessageConsoleStream out, BufferedWriter bwOut) {
		String[] parameters = CRNReducerCommandLine.getParameters(command);

		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return false;
		}
		String fileName=null;
		String mainComponent=null;
		boolean odes=false;
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("fileIn=>")){
				if(parameters[p].length()<="fileIn=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the SpaceEx xml file. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				fileName = parameters[p].substring("fileIn=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("mainComponent=>")){
				if(parameters[p].length()<="mainComponent=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the main component from where the flow should be taken. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				mainComponent = parameters[p].substring("mainComponent=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("odes=>")){
				if(parameters[p].length()<="odes=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify whether you want to export the model as ODEs. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				odes = Boolean.valueOf(parameters[p].substring("odes=>".length(), parameters[p].length()));
			}
			else if(parameters[p].equals("")){
				continue;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return false;
			}
		}


		if(fileName ==null || fileName.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the SpaceEx XML file. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}

		if(mainComponent==null) {
			mainComponent="";
		}
		
		try {
			boolean failed = false;
			
			failed = loadFile(fileName,SupportedFormats.SpaceEx,true,new String[] {mainComponent,odes?"true":"false"},out,bwOut,false,false);
			if(failed){
				printWarning(out, bwOut,true,messageDialogShower,"Loading failed:" +fileName,DialogType.Error);
				crn=null;
				partition=null;
			}
		} catch (FileNotFoundException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"File not found: "+fileName,DialogType.Error);
			return false;
		} catch (UnsupportedFormatException e) {
			if(e.getMessage()!=null){
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.\nError message:\n"+e.getMessage(),DialogType.Error);
			}
			else{
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.",DialogType.Error);
			}
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled IO errors.\nError message:\n"+e.getMessage(),DialogType.Error);
			//CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled errors.\nError message:\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}  catch (XMLStreamException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled error.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}
		
		
		
		return true;
	}
	
	public boolean handleImportAffineSystem(String command, MessageConsoleStream out, BufferedWriter bwOut) {
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return false;
		}
		String fileName=null;
		String icFile=null;
		String bFile=null;
		boolean addReverseEdges=false;
		boolean createParameters=false;
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("fileIn=>")){
				if(parameters[p].length()<="fileIn=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file containing the A component of Ax = b. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				fileName = parameters[p].substring("fileIn=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("bFile=>")){
				if(parameters[p].length()<="bFile=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the file containing the b vector. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				bFile = parameters[p].substring("bFile=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("icFile=>")){
				if(parameters[p].length()<="icFile=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the file containing the initial conditions. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				icFile = parameters[p].substring("icFile=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("createParams=>")){
				if(parameters[p].length()<="createParams=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if a parameter per row of the matrix should be created. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				createParameters = Boolean.valueOf(parameters[p].substring("createParams=>".length(), parameters[p].length()));
			}
			else if(parameters[p].startsWith("addReverseEdges=>")){
				if(parameters[p].length()<="addReverseEdges=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if a for every edge i,j,w, a reverse edge (j,i,w) shall be added. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				addReverseEdges = Boolean.valueOf(parameters[p].substring("addReverseEdges=>".length(), parameters[p].length()));
			}
			else if(parameters[p].equals("")){
				continue;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return false;
			}
		}
		
		
		if(fileName ==null || fileName.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the file containing the A component of Ax = b. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		if(bFile ==null || bFile.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the file containing the b component of Ax = b. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		if(icFile ==null || icFile.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the file containing the initial conditions. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		
		try {
			boolean failed = false;
			failed = loadFile(fileName,SupportedFormats.Affine,true,new String[]{"form","AX","bFile",bFile,"icFile",icFile,"createParameters",String.valueOf(createParameters),"addReverseEdges",String.valueOf(addReverseEdges)},out,bwOut,false,false);
			if(failed){
				printWarning(out, bwOut,true,messageDialogShower,"Loading failed:" +fileName,DialogType.Error);
				crn=null;
				partition=null;
			}
		} catch (FileNotFoundException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"File not found: "+fileName,DialogType.Error);
			return false;
		} catch (UnsupportedFormatException e) {
			if(e.getMessage()!=null){
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.\nError message:\n"+e.getMessage(),DialogType.Error);
			}
			else{
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.",DialogType.Error);
			}
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled IO errors.\nError message:\n"+e.getMessage(),DialogType.Error);
			//CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled errors.\nError message:\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}  catch (XMLStreamException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled error.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}
			
		return true;
	}
	
	protected boolean handleImportLinearSystemWithInputs(String command, MessageConsoleStream out, BufferedWriter bwOut) {
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return false;
		}
		String fileName=null;
		//int inputs=0;
		String bFile=null;
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("fileIn=>")){
				if(parameters[p].length()<="fileIn=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file containing the A component of Ax = b. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				fileName = parameters[p].substring("fileIn=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("bFile=>")){
				if(parameters[p].length()<="bFile=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the prefix of the file containing the b vector. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				bFile = parameters[p].substring("bFile=>".length(), parameters[p].length());
			}
//			else if(parameters[p].startsWith("inputs=>")){
//				if(parameters[p].length()<="inputs=>".length()){
//					CRNReducerCommandLine.println(out,bwOut,"Please, specify the file number of inputs. ");
//					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
//					return false;
//				}
//				inputs = Integer.valueOf(parameters[p].substring("inputs=>".length(), parameters[p].length()));
//			}
			else if(parameters[p].equals("")){
				continue;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return false;
			}
		}
		
		
		if(fileName ==null || fileName.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the file containing the A component of Ax = b. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		
		try {
			boolean failed = false;
			failed = loadFile(fileName,SupportedFormats.LinearWithInputs,true,new String[]{"form","AX","bFile",bFile/*,"inputs",String.valueOf(inputs)*/},out,bwOut,false,false);
			if(failed){
				printWarning(out, bwOut,true,messageDialogShower,"Loading failed:" +fileName,DialogType.Error);
				crn=null;
				partition=null;
			}
		} catch (FileNotFoundException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"File not found: "+fileName,DialogType.Error);
			return false;
		} catch (UnsupportedFormatException e) {
			if(e.getMessage()!=null){
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.\nError message:\n"+e.getMessage(),DialogType.Error);
			}
			else{
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.",DialogType.Error);
			}
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled IO errors.\nError message:\n"+e.getMessage(),DialogType.Error);
			//CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled errors.\nError message:\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}  catch (XMLStreamException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled error.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}
			
		return true;
	}
	
	
	public boolean handleImportMRMCCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) {
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return false;
		}
		String fileName=null;
		String labellingFileName=null;
		String asMatrix = "false";
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("fileIn=>")){
				if(parameters[p].length()<="fileIn=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file to read. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				fileName = parameters[p].substring("fileIn=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("labellingFile=>")){
				if(parameters[p].length()<="labellingFile=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file containing the labelling function of the Markov chain. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				labellingFileName = parameters[p].substring("labellingFile=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("asMatrix=>")){
				if(parameters[p].length()<="asMatrix=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, give either true or false if you want to import the markov chain as a matrix or not [default false]. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				asMatrix = parameters[p].substring("asMatrix=>".length(), parameters[p].length());
			}
			else if(parameters[p].equals("")){
				continue;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return false;
			}
		}
		if(fileName ==null || fileName.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the file to be loaded. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		try {
			boolean failed = loadFile(fileName,SupportedFormats.MRMC,true,new String[]{labellingFileName,asMatrix},out,bwOut,false,false);
			if(failed){
				printWarning(out, bwOut,true,messageDialogShower,"Loading failed:" +fileName,DialogType.Error);
				crn=null;
				partition=null;
			}
			/*else{
				printWarning(out, bwOut,"Loading of the model succeeded:" +fileName);
			}*/
		/*} catch (FileNotFoundException e) {
			CRNReducerCommandLine.println(out,bwOut,"File not found: "+fileName,DialogType.Error);
			return false;
		} catch (UnsupportedFormatException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading of "+fileName+" failed.",DialogType.Error);
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading failed due to unhandled error. This is the exception stack trace.");
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading failed due to unhandled error. This is the exception stack trace.");
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}*/
		} catch (FileNotFoundException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"File not found: "+fileName,DialogType.Error);
			return false;
		} catch (UnsupportedFormatException e) {
			if(e.getMessage()!=null){
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.\nError message:\n"+e.getMessage(),DialogType.Error);
			}
			else{
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.",DialogType.Error);
			}
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled IO errors.\nError message:\n"+e.getMessage(),DialogType.Error);
			//CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled errors.\nError message:\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}  catch (XMLStreamException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled error.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}
		
		return true;
	}

	private boolean handleImportUMISTCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		//sample command: importChemKin({fileName=>./ChemKinNetworks/example1.inp})
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return false;
		}
		String fileName=null;
		String fileInSpecies=null;
		//String crnFileName=null;
		//boolean writeCRN=false;
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("fileIn=>")){
				if(parameters[p].length()<="fileIn=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file to read. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				fileName = parameters[p].substring("fileIn=>".length(), parameters[p].length());
				/*if(crnFileName==null){
					crnFileName=fileName;
				}*/
			}
			else if(parameters[p].startsWith("fileInSpecies=>")){
				if(parameters[p].length()<="fileInSpecies=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file containing the species information. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				fileInSpecies = parameters[p].substring("fileInSpecies=>".length(), parameters[p].length());
			}
			/*else if(parameters[p].startsWith("crnFile=>")){
				if(parameters[p].length()<="crnFile=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, provide the name of the file where to store the model. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				thermoDynamicFileName = parameters[p].substring("thermoDynamicFile=>".length(), parameters[p].length());
			}*/
			else if(parameters[p].equals("")){
				continue;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return false;
			}
		}
		if(fileName ==null || fileName.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the file to be loaded. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		try {
			boolean failed = loadFile(fileName,SupportedFormats.UMIST,true,new String[]{fileInSpecies},out,bwOut,false,false);
			if(failed){
				printWarning(out, bwOut,true,messageDialogShower,"Loading failed:" +fileName,DialogType.Error);
				crn=null;
				partition=null;
			}
			/*else{
				printWarning(out, bwOut,"Loading of the model succeeded:" +fileName);
			}*/
		/*} catch (FileNotFoundException e) {
			CRNReducerCommandLine.println(out,bwOut,"File not found: "+fileName,DialogType.Error);
			return false;
		} catch (UnsupportedFormatException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading of "+fileName+" failed.",DialogType.Error);
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading failed due to unhandled error. This is the exception stack trace.");
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading failed due to unhandled error. This is the exception stack trace.");
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}*/
		} catch (FileNotFoundException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"File not found: "+fileName,DialogType.Error);
			return false;
		} catch (UnsupportedFormatException e) {
			if(e.getMessage()!=null){
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.\nError message:\n"+e.getMessage(),DialogType.Error);
			}
			else{
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.",DialogType.Error);
			}
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled IO errors.\nError message:\n"+e.getMessage(),DialogType.Error);
			//CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled errors.\nError message:\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}  catch (XMLStreamException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled error.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}
		
		/*if(writeCRN){
			writeCRN(crnFileName,crn,partition,SupportedFormats.CRN,"Imported from ChemKin.",null);
		}*/
		return true;
	}
	
	private boolean handleImportOSUCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		//sample command: importChemKin({fileName=>./ChemKinNetworks/example1.inp})
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return false;
		}
		String linesToSkip="0";
		String fileName=null;
		//String crnFileName=null;
		//boolean writeCRN=false;
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("fileIn=>")){
				if(parameters[p].length()<="fileIn=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file to read. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				fileName = parameters[p].substring("fileIn=>".length(), parameters[p].length());
				/*if(crnFileName==null){
					crnFileName=fileName;
				}*/
			}
			else if(parameters[p].startsWith("linesToSkip=>")){
				if(parameters[p].length()<="linesToSkip=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the number of lines to skip at the beginning of the file. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				linesToSkip = parameters[p].substring("linesToSkip=>".length(), parameters[p].length());
			}
			else if(parameters[p].equals("")){
				continue;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return false;
			}
		}
		if(fileName ==null || fileName.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the file to be loaded. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		try {
			boolean failed = loadFile(fileName,SupportedFormats.OSU,true,new String[] {linesToSkip},out,bwOut,false,false);
			if(failed){
				printWarning(out, bwOut,true,messageDialogShower,"Loading failed:" +fileName,DialogType.Error);
				crn=null;
				partition=null;
			}
			/*else{
				printWarning(out, bwOut,"Loading of the model succeeded:" +fileName);
			}*/
		/*} catch (FileNotFoundException e) {
			CRNReducerCommandLine.println(out,bwOut,"File not found: "+fileName,DialogType.Error);
			return false;
		} catch (UnsupportedFormatException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading of "+fileName+" failed.",DialogType.Error);
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading failed due to unhandled error. This is the exception stack trace.");
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading failed due to unhandled error. This is the exception stack trace.");
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}*/
		} catch (FileNotFoundException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"File not found: "+fileName,DialogType.Error);
			return false;
		} catch (UnsupportedFormatException e) {
			if(e.getMessage()!=null){
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.\nError message:\n"+e.getMessage(),DialogType.Error);
			}
			else{
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.",DialogType.Error);
			}
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled IO errors.\nError message:\n"+e.getMessage(),DialogType.Error);
			//CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled errors.\nError message:\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}  catch (XMLStreamException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled error.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}
		
		/*if(writeCRN){
			writeCRN(crnFileName,crn,partition,SupportedFormats.CRN,"Imported from ChemKin.",null);
		}*/
		return true;
	}
	
	
	private boolean handleImportChemKinCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException {
		//sample command: importChemKin({fileName=>./ChemKinNetworks/example1.inp})
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return false;
		}
		String fileName=null;
		String thermoDynamicFileName=null;
		//String crnFileName=null;
		//boolean writeCRN=false;
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("fileIn=>")){
				if(parameters[p].length()<="fileIn=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file to read. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				fileName = parameters[p].substring("fileIn=>".length(), parameters[p].length());
				/*if(crnFileName==null){
					crnFileName=fileName;
				}*/
			}
			else if(parameters[p].startsWith("thermoDynamicFile=>")){
				if(parameters[p].length()<="thermoDynamicFile=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file containing the thermodynamic information. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				thermoDynamicFileName = parameters[p].substring("thermoDynamicFile=>".length(), parameters[p].length());
			}
			/*else if(parameters[p].startsWith("crnFile=>")){
				if(parameters[p].length()<="crnFile=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, provide the name of the file where to store the model. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				thermoDynamicFileName = parameters[p].substring("thermoDynamicFile=>".length(), parameters[p].length());
			}*/
			else if(parameters[p].equals("")){
				continue;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return false;
			}
		}
		if(fileName ==null || fileName.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the file to be loaded. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		try {
			boolean failed = loadFile(fileName,SupportedFormats.ChemKin,true,new String[]{thermoDynamicFileName},out,bwOut,false,false);
			if(failed){
				printWarning(out, bwOut,true,messageDialogShower,"Loading failed:" +fileName,DialogType.Error);
				crn=null;
				partition=null;
			}
			/*else{
				printWarning(out, bwOut,"Loading of the model succeeded:" +fileName);
			}*/
		/*} catch (FileNotFoundException e) {
			CRNReducerCommandLine.println(out,bwOut,"File not found: "+fileName,DialogType.Error);
			return false;
		} catch (UnsupportedFormatException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading of "+fileName+" failed.",DialogType.Error);
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading failed due to unhandled error. This is the exception stack trace.");
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading failed due to unhandled error. This is the exception stack trace.");
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}*/
		} catch (FileNotFoundException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"File not found: "+fileName,DialogType.Error);
			return false;
		} catch (UnsupportedFormatException e) {
			if(e.getMessage()!=null){
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.\nError message:\n"+e.getMessage(),DialogType.Error);
			}
			else{
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.",DialogType.Error);
			}
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled IO errors.\nError message:\n"+e.getMessage(),DialogType.Error);
			//CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled errors.\nError message:\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}  catch (XMLStreamException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled error.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}
		
		/*if(writeCRN){
			writeCRN(crnFileName,crn,partition,SupportedFormats.CRN,"Imported from ChemKin.",null);
		}*/
		return true;
	}
	
	private boolean handleImportSBMLCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) {
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return false;
		}
		boolean forceMassAction=false;
		String fileName=null;
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("fileIn=>")){
				if(parameters[p].length()<="fileIn=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file to read. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				fileName = parameters[p].substring("fileIn=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("forceMassAction=>")){
				if(parameters[p].length()<="forceMassAction=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify whether we should force a mass action intepretation of the model. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				forceMassAction = Boolean.valueOf(parameters[p].substring("forceMassAction=>".length(), parameters[p].length()));
			}
			else if(parameters[p].equals("")){
				continue;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return false;
			}
		}
		if(fileName ==null || fileName.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the file to be loaded. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		try {
			//new String[]{String.valueOf(forceMassAction)},
			loadFile(fileName,SupportedFormats.SBML,true,true,new String[]{String.valueOf(forceMassAction)},out,bwOut,false,false);
		} catch (FileNotFoundException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"File not found: "+fileName,DialogType.Error);
			return false;
		} catch (UnsupportedFormatException e) {
			if(e.getMessage()!=null){
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.\nError message:\n"+e.getMessage(),DialogType.Error);
			}
			else{
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.",DialogType.Error);
			}
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled IO errors.\nError message:\n"+e.getMessage(),DialogType.Error);
			//CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return false;
		} catch (JDOMException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled errors.\nError message:\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return false;
		}  catch (XMLStreamException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled error.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return false;
		} catch (NullPointerException | org.sbml.jsbml.SBMLException e) {
			//CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled error.\nError message:\n"+e.getMessage(),DialogType.Error);
			//CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			CRNReducerCommandLine.println(out, bwOut, "Loading failed due to unhandled error. The model might be not supported.\nError message:\n"+e.getMessage());
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return false;
		}
		
		
		return true;
	}
	
	
	private boolean handleImportBNetFolderCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) {
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return false;
		}
		String folderIn=null;
		String folderOut=null;
		GuessPrepartitionBN guessPrep=GuessPrepartitionBN.INPUTS;
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("folderIn=>")){
				if(parameters[p].length()<="folderIn=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the folder from which to read. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				folderIn = parameters[p].substring("folderIn=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("folderOut=>")){
				if(parameters[p].length()<="folderOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the folder where to write the imported models. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				folderOut = parameters[p].substring("folderOut=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("guessPrep=>")){
				if(parameters[p].length()<="guessPrep=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify INPUTS or OUTPUTS for the type of guessed prepartition. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				String gp = parameters[p].substring("guessPrep=>".length(), parameters[p].length());
				if(gp.equalsIgnoreCase("outputs")) {
					guessPrep=GuessPrepartitionBN.OUTPUTS;
				}
				else if(gp.equalsIgnoreCase("outputsoneblock")) {
					guessPrep=GuessPrepartitionBN.OUTPUTSONEBLOCK;
				}
				else if(gp.equalsIgnoreCase("inputs")) {
					guessPrep=GuessPrepartitionBN.INPUTS;
				}
				else if(gp.equalsIgnoreCase("inputsoneblock")) {
					guessPrep=GuessPrepartitionBN.INPUTSONEBLOCK;
				}
			}
			else if(parameters[p].equals("")){
				continue;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return false;
			}
		}
		if(folderIn ==null || folderIn.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the file to be loaded. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		try {
			//new String[]{String.valueOf(forceMassAction)},
			
			File foldOut = new File(folderOut);
			String foldoutPrefix = foldOut.getAbsolutePath()+File.separator;
			
			File foldIn = new File(folderIn);
			if(foldIn.isDirectory()) {
				String[] allFiles = foldIn.list();
				CRNReducerCommandLine.println(out,bwOut,"Loading all BNet files in folder:");
				CRNReducerCommandLine.println(out,bwOut,"\t"+folderIn);
				CRNReducerCommandLine.println(out,bwOut,"The folder contains "+allFiles.length+" files:");
				for(int i=0;i<allFiles.length;i++) {
					CRNReducerCommandLine.println(out,bwOut,"\t"+allFiles[i]);
				}
				CRNReducerCommandLine.println(out,bwOut,"");
				for(int i=0;i<allFiles.length;i++) {
					String current = foldIn.getAbsolutePath()+File.separator+allFiles[i];
					if(allFiles[i].toLowerCase().endsWith(".bnet")) {
						String fileOut = foldoutPrefix+AbstractImporter.overwriteExtensionIfEnabled(allFiles[i],".ode",true);
						BNetImporter bnet = new BNetImporter(current, out, bwOut, messageDialogShower,guessPrep);
						bnet.readAndExportBNet(true, fileOut);
						CRNReducerCommandLine.println(out,bwOut,"");
					}
				}
			}
		} catch (UnsupportedFormatException e) {
			if(e.getMessage()!=null){
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+folderIn+" failed.\nError message:\n"+e.getMessage(),DialogType.Error);
			}
			else{
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+folderIn+" failed.",DialogType.Error);
			}
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled IO errors.\nError message:\n"+e.getMessage(),DialogType.Error);
			//CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return false;
		} 
		
		return true;
	}
	
	//@SuppressWarnings("unused")
	private boolean handleImportCNFFolderCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) {
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return false;
		}
		String folderIn=null;
		String folderOut=null;
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("folderIn=>")){
				if(parameters[p].length()<="folderIn=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the folder from which to read. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				folderIn = parameters[p].substring("folderIn=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("folderOut=>")){
				if(parameters[p].length()<="folderOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the folder where to write the imported models. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				folderOut = parameters[p].substring("folderOut=>".length(), parameters[p].length());
			}
			else if(parameters[p].equals("")){
				continue;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return false;
			}
		}
		if(folderIn ==null || folderIn.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the folder to be loaded. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		if(folderOut ==null || folderOut.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the target folder. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		try {
			//new String[]{String.valueOf(forceMassAction)},

			File foldOut = new File(folderOut);
			String foldoutPrefix = foldOut.getAbsolutePath()+File.separator;

			File foldIn = new File(folderIn);
			if(foldIn.isDirectory()) {
				String[] allFiles = foldIn.list();
				CRNReducerCommandLine.println(out,bwOut,"Loading all CNF files in folder:");
				CRNReducerCommandLine.println(out,bwOut,"\t"+folderIn);
				CRNReducerCommandLine.println(out,bwOut,"The folder contains "+allFiles.length+" files:");
				for(int i=0;i<allFiles.length;i++) {
					CRNReducerCommandLine.println(out,bwOut,"\t"+allFiles[i]);
				}
				CRNReducerCommandLine.println(out,bwOut,"");
				for(int i=0;i<allFiles.length;i++) {
					String current = foldIn.getAbsolutePath()+File.separator+allFiles[i];
					if(allFiles[i].toLowerCase().endsWith(".cnf")) {
						String fileOut = foldoutPrefix+AbstractImporter.overwriteExtensionIfEnabled(allFiles[i],".ode",true);

						CNFImporter cnfImporter = new CNFImporter(current, out, bwOut, messageDialogShower);

						ArrayList<String> comments=cnfImporter.readCNF(true);
						IBooleanNetwork bn = cnfImporter.getBN();
						Collection<String> preambleComments=new ArrayList<String>(3+comments.size());
						preambleComments.add("Automatically imported by "+TOOLNAME+" from");
						preambleComments.add(current);
						preambleComments.add("User partition obtained by singling out the extra 'output' variable");
						preambleComments.addAll(comments);

						Collection<String> commands = new ArrayList<String>(2);
						Collection<String> commandsCL = new ArrayList<String>(2);

						String preserving="OutputPreserving";

						//				        commands.add("reduceBBE(fileWhereToStorePartition=\"red"+File.separator+bn.getName()+"BBE.txt\",csvFile=\"reductionsMaximalBBE.csv\",reducedFile=\"red"+File.separator+bn.getName()+"BBE.ode\")");
						//				        if(bn.getUserDefinedPartition()!=null && bn.getUserDefinedPartition().size()>0) {
						//				        	commands.add("reduceBBE(fileWhereToStorePartition=\"red"+File.separator+bn.getName()+preserving+"BBE.txt\",csvFile=\"reductions"+preserving+"BBE.csv\",reducedFile=\"red"+File.separator+bn.getName()+preserving+"BBE.ode\",prePartition=USER)\n");
						//				        }

						ArrayList<String> aggrs=new ArrayList<>(4);
						String technique="FBE";
						if(bn.isMultiValued()) {
							technique="FME";
							aggrs.add("PLUS");
							aggrs.add("TIMES");
							aggrs.add("MIN");
							aggrs.add("MAX");
						}
						else {
							aggrs.add("OR");
							aggrs.add("AND");

							//				        	commands.add("reduceFBE(aggregationFunction=OR,fileWhereToStorePartition=\""+bn.getName()+"FBE_OR.txt\",csvFile=\"reductionsMaximalFBE_OR.csv\",reducedFile=\""+bn.getName()+"FBE_OR.ode\")");
							//					        if(bn.getUserDefinedPartition()!=null && bn.getUserDefinedPartition().size()>0) {
							//					        	commands.add("reduceFBE(aggregationFunction=OR,fileWhereToStorePartition=\""+bn.getName()+preserving+"FBE_OR.txt\",csvFile=\"reductions"+preserving+"FBE_OR.csv\",reducedFile=\""+bn.getName()+preserving+"FBE_OR.ode\",prePartition=USER)\n");
							//					        }
							//					        commands.add("reduceFBE(aggregationFunction=AND,fileWhereToStorePartition=\""+bn.getName()+"FBE_AND.txt\",csvFile=\"reductionsMaximalFBE_AND.csv\",reducedFile=\""+bn.getName()+"FBE_AND.ode\")");
							//					        if(bn.getUserDefinedPartition()!=null && bn.getUserDefinedPartition().size()>0) {
							//					        	commands.add("reduceFBE(aggregationFunction=AND,fileWhereToStorePartition=\""+bn.getName()+preserving+"FBE_AND.txt\",csvFile=\"reductions"+preserving+"FBE_AND.csv\",reducedFile=\""+bn.getName()+preserving+"FBE_AND.ode\",prePartition=USER)\n");
							//					        }
						}
						for(String aggr:aggrs) {
							//commands.add("reduce"+technique+"(aggregationFunction="+aggr+",fileWhereToStorePartition=\"red"+File.separator+bn.getName()+technique+"_"+aggr+".txt\",csvFile=\"reductionsMaximal"+technique+"_"+aggr+".csv\",reducedFile=\"red"+File.separator+bn.getName()+technique+"_"+aggr+".ode\")");
							if(bn.getUserDefinedPartition()!=null && bn.getUserDefinedPartition().size()>0) {
								commands.add("reduce"+technique+"(aggregationFunction="+aggr+",fileWhereToStorePartition=\"red"+File.separator+bn.getName()+preserving+technique+"_"+aggr+".txt\",csvFile=\"reductions"+preserving+technique+"_"+aggr+".csv\",reducedFile=\"red"+File.separator+bn.getName()+preserving+technique+"_"+aggr+"._ode\",prePartition=USER)\n");
								String outPathRed = foldOut.getAbsolutePath()+ File.separator +"red"+File.separator;
								commandsCL.add("reduce"+technique+"({aggregationFunction=>"+aggr+",fileWhereToStorePartition=>"+outPathRed+bn.getName()+preserving+technique+"_"+aggr+".txt,csvFile=>"+outPathRed+"reductions"+preserving+technique+"_"+aggr+".csv,reducedFile=>"+outPathRed+bn.getName()+preserving+technique+"_"+aggr+"._ode,prePartition=>USER})\n");
							}
						}

						BooleanNetworkCommandLine bnCL = new BooleanNetworkCommandLine(null, bn, partition);
						for(String com : commandsCL) {
							bnCL.handleReduceCommand(com, false, technique, out, bwOut);
						}


						boolean writeFile=true;
						if(writeFile) {
							GUIBooleanNetworkImporter.printToBNERODEFIle(bn, cnfImporter.getInitialPartition(),
									fileOut,preambleComments, true, out, bwOut, false,commands);
						}

						CRNReducerCommandLine.println(out,bwOut,"");

					}
				}
			}
		} catch (UnsupportedFormatException e) {
			if(e.getMessage()!=null){
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+folderIn+" failed.\nError message:\n"+e.getMessage(),DialogType.Error);
			}
			else{
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+folderIn+" failed.",DialogType.Error);
			}
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled IO errors.\nError message:\n"+e.getMessage(),DialogType.Error);
			//CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return false;
		} 

		return true;
	}

	private boolean handleImportSBMLQualFolderCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) {
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return false;
		}
		String folderIn=null;
		String folderOut=null;
		GuessPrepartitionBN guessPrep=GuessPrepartitionBN.INPUTS;
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("folderIn=>")){
				if(parameters[p].length()<="folderIn=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the folder from which to read. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				folderIn = parameters[p].substring("folderIn=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("folderOut=>")){
				if(parameters[p].length()<="folderOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the folder where to write the imported models. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				folderOut = parameters[p].substring("folderOut=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("guessPrep=>")){
				if(parameters[p].length()<="guessPrep=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify INPUTS or OUTPUTS for the type of guessed prepartition. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				String gp = parameters[p].substring("guessPrep=>".length(), parameters[p].length());
				if(gp.equalsIgnoreCase("outputs")) {
					guessPrep=GuessPrepartitionBN.OUTPUTS;
				}
				else if(gp.equalsIgnoreCase("outputsoneblock")) {
					guessPrep=GuessPrepartitionBN.OUTPUTSONEBLOCK;
				}
				else if(gp.equalsIgnoreCase("inputs")) {
					guessPrep=GuessPrepartitionBN.INPUTS;
				}
				else if(gp.equalsIgnoreCase("inputsoneblock")) {
					guessPrep=GuessPrepartitionBN.INPUTSONEBLOCK;
				}
			}
			else if(parameters[p].equals("")){
				continue;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return false;
			}
		}
		if(folderIn ==null || folderIn.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the file to be loaded. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		try {
			//new String[]{String.valueOf(forceMassAction)},
			
			File foldOut = new File(folderOut);
			String foldoutPrefix = foldOut.getAbsolutePath()+File.separator;
			
			File foldIn = new File(folderIn);
			if(foldIn.isDirectory()) {
				String[] allFiles = foldIn.list();
				CRNReducerCommandLine.println(out,bwOut,"Loading all SBML Qual files in folder:");
				CRNReducerCommandLine.println(out,bwOut,"\t"+folderIn);
				CRNReducerCommandLine.println(out,bwOut,"The folder contains "+allFiles.length+" files:");
				for(int i=0;i<allFiles.length;i++) {
					CRNReducerCommandLine.println(out,bwOut,"\t"+allFiles[i]);
				}
				CRNReducerCommandLine.println(out,bwOut,"");
				for(int i=0;i<allFiles.length;i++) {
					String current = foldIn.getAbsolutePath()+File.separator+allFiles[i];
					if(allFiles[i].toLowerCase().endsWith(".sbml")) {
						String fileOut = foldoutPrefix+AbstractImporter.overwriteExtensionIfEnabled(allFiles[i],".ode",true);
						
						String nameFromFile=GUICRNImporter.getModelName(current);
				        SBMLDocument sbmlDocument = (SBMLDocument) SBMLManager.read(current);
				        ISBMLConverter sbmlConverter = SBMLManager.create(sbmlDocument,guessPrep,out,bwOut,nameFromFile);
				        GUIBooleanNetworkImporter guiBooleanNetworkImporter = sbmlConverter.getGuiBnImporter();
				        IBooleanNetwork bn = guiBooleanNetworkImporter.getBooleanNetwork();
				        Collection<String> preambleComments=new ArrayList<String>(2);
				        preambleComments.add("Automatically imported by "+TOOLNAME+" from");
				        preambleComments.add(current);
				        String gp="inputs";
				        if(guessPrep.equals(GuessPrepartitionBN.OUTPUTS)) {
				        	gp="outputs";
				        }
				        preambleComments.add("User partition obtained by singling out guessed "+gp);
				        
				        Collection<String> commands = new ArrayList<String>(2);
				        
				        String preserving="InputPreserving";
				        if(guessPrep.equals(GuessPrepartitionBN.OUTPUTS)) {
				        	preserving="OutputPreserving";
				        }
				        else if(guessPrep.equals(GuessPrepartitionBN.OUTPUTSONEBLOCK)) {
				        	preserving="OutputPreservingOneBlock";
				        }
				        else if(guessPrep.equals(GuessPrepartitionBN.INPUTSONEBLOCK)) {
				        	preserving="InputPreservingOneBlock";
				        }
				        
//				        commands.add("reduceBBE(fileWhereToStorePartition=\"red"+File.separator+bn.getName()+"BBE.txt\",csvFile=\"reductionsMaximalBBE.csv\",reducedFile=\"red"+File.separator+bn.getName()+"BBE.ode\")");
//				        if(bn.getUserDefinedPartition()!=null && bn.getUserDefinedPartition().size()>0) {
//				        	commands.add("reduceBBE(fileWhereToStorePartition=\"red"+File.separator+bn.getName()+preserving+"BBE.txt\",csvFile=\"reductions"+preserving+"BBE.csv\",reducedFile=\"red"+File.separator+bn.getName()+preserving+"BBE.ode\",prePartition=USER)\n");
//				        }
				        
				        ArrayList<String> aggrs=new ArrayList<>(4);
				        String technique="FBE";
				        if(bn.isMultiValued()) {
				        	technique="FME";
				        	//aggrs.add("PLUS");
				        	//aggrs.add("TIMES");
				        	aggrs.add("MIN");
				        	aggrs.add("MAX");
				        }
				        else {
				        	aggrs.add("AND");
				        	aggrs.add("OR");
				        	//aggrs.add("XOR");
//				        	commands.add("reduceFBE(aggregationFunction=OR,fileWhereToStorePartition=\""+bn.getName()+"FBE_OR.txt\",csvFile=\"reductionsMaximalFBE_OR.csv\",reducedFile=\""+bn.getName()+"FBE_OR.ode\")");
//					        if(bn.getUserDefinedPartition()!=null && bn.getUserDefinedPartition().size()>0) {
//					        	commands.add("reduceFBE(aggregationFunction=OR,fileWhereToStorePartition=\""+bn.getName()+preserving+"FBE_OR.txt\",csvFile=\"reductions"+preserving+"FBE_OR.csv\",reducedFile=\""+bn.getName()+preserving+"FBE_OR.ode\",prePartition=USER)\n");
//					        }
//					        commands.add("reduceFBE(aggregationFunction=AND,fileWhereToStorePartition=\""+bn.getName()+"FBE_AND.txt\",csvFile=\"reductionsMaximalFBE_AND.csv\",reducedFile=\""+bn.getName()+"FBE_AND.ode\")");
//					        if(bn.getUserDefinedPartition()!=null && bn.getUserDefinedPartition().size()>0) {
//					        	commands.add("reduceFBE(aggregationFunction=AND,fileWhereToStorePartition=\""+bn.getName()+preserving+"FBE_AND.txt\",csvFile=\"reductions"+preserving+"FBE_AND.csv\",reducedFile=\""+bn.getName()+preserving+"FBE_AND.ode\",prePartition=USER)\n");
//					        }
				        }
				        for(String aggr:aggrs) {
				        	//commands.add("reduce"+technique+"(aggregationFunction="+aggr+",fileWhereToStorePartition=\"red"+File.separator+bn.getName()+technique+"_"+aggr+".txt\",csvFile=\"reductionsMaximal"+technique+"_"+aggr+".csv\",reducedFile=\"red"+File.separator+bn.getName()+technique+"_"+aggr+".ode\")");
					        if(bn.getUserDefinedPartition()!=null && bn.getUserDefinedPartition().size()>0) {
					        	commands.add("reduce"+technique+"(aggregationFunction="+aggr+",fileWhereToStorePartition=\"red"+File.separator+bn.getName()+preserving+technique+"_"+aggr+".txt\",csvFile=\"reductions"+preserving+technique+"_"+aggr+".csv\",reducedFile=\"red"+File.separator+bn.getName()+preserving+technique+"_"+aggr+".ode\",prePartition=USER)\n");
					        }
				        }
				        
						GUIBooleanNetworkImporter.printToBNERODEFIle(bn,guiBooleanNetworkImporter.getInitialPartition(),
				        		fileOut,preambleComments, true, out, bwOut, false,commands);
						
						//BNetImporter bnet = new BNetImporter(current, out, bwOut, messageDialogShower);
						//bnet.readAndExportBNet(true, fileOut);
						CRNReducerCommandLine.println(out,bwOut,"");
					}
				}
			}
		} catch (IllegalArgumentException e) {
			if(e.getMessage()!=null){
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+folderIn+" failed.\nError message:\n"+e.getMessage(),DialogType.Error);
			}
			else{
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+folderIn+" failed.",DialogType.Error);
			}
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled IO errors.\nError message:\n"+e.getMessage(),DialogType.Error);
			//CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return false;
		} catch (XMLStreamException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled IO errors while reading the SBML file.\nError message:\n"+e.getMessage(),DialogType.Error);
			e.printStackTrace();
			return false;
		} 
		return true;
	}
	
	
	private boolean handleImportCNF_QuantumSATOptimization_FolderCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) {
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return false;
		}
		String folderIn=null;
		String folderOut=null;
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("folderIn=>")){
				if(parameters[p].length()<="folderIn=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the folder from which to read. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				folderIn = parameters[p].substring("folderIn=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("folderOut=>")){
				if(parameters[p].length()<="folderOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the folder where to write the imported models. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				folderOut = parameters[p].substring("folderOut=>".length(), parameters[p].length());
			}
			else if(parameters[p].equals("")){
				continue;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return false;
			}
		}
		if(folderIn ==null || folderIn.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the file to be loaded. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		try {
			
			
			
			File foldOut = new File(folderOut);
			String foldoutPrefix = foldOut.getAbsolutePath()+File.separator;
			//String fileOut = foldoutPrefix+AbstractImporter.overwriteExtensionIfEnabled(allFiles[i],".ode",true);
					
			File foldIn = new File(folderIn);
			if(foldIn.isDirectory()) {
				String[] allFiles = foldIn.list();
				CRNReducerCommandLine.println(out,bwOut,"Loading all CNF files in folder:");
				CRNReducerCommandLine.println(out,bwOut,"\t"+folderIn);
				CRNReducerCommandLine.println(out,bwOut,"The folder contains "+allFiles.length+" files:");
				for(int i=0;i<allFiles.length;i++) {
					CRNReducerCommandLine.println(out,bwOut,"\t"+allFiles[i]);
				}
				CRNReducerCommandLine.println(out,bwOut,"");
				for(int i=0;i<allFiles.length;i++) {
					String current = foldIn.getAbsolutePath()+File.separator+allFiles[i];
					if(allFiles[i].toLowerCase().endsWith(".cnf")) {
						String fileOut = foldoutPrefix+AbstractImporter.overwriteExtensionIfEnabled(allFiles[i],"._ode",true);
						CNFImporter importer = new CNFImporter(current, out, bwOut, messageDialogShower);
						importer.readCNFandMakeQuantumOptimization(true,fileOut);
						CRNReducerCommandLine.println(out,bwOut,"");
					}
				}
			}
		} catch (UnsupportedFormatException e) {
			if(e.getMessage()!=null){
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+folderIn+" failed.\nError message:\n"+e.getMessage(),DialogType.Error);
			}
			else{
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+folderIn+" failed.",DialogType.Error);
			}
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled IO errors.\nError message:\n"+e.getMessage(),DialogType.Error);
			//CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return false;
		} 
		
		return true;
	}
	
	//importAndPolyCNFFolder
	private boolean handleImportCNFFolder_AndPoly_Command(String command, MessageConsoleStream out, BufferedWriter bwOut) {
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return false;
		}
		String folderIn=null;
		String folderOut=null;
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("folderIn=>")){
				if(parameters[p].length()<="folderIn=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the folder from which to read. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				folderIn = parameters[p].substring("folderIn=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("folderOut=>")){
				if(parameters[p].length()<="folderOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the folder where to write the imported models. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				folderOut = parameters[p].substring("folderOut=>".length(), parameters[p].length());
			}
			else if(parameters[p].equals("")){
				continue;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return false;
			}
		}
		if(folderIn ==null || folderIn.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the file to be loaded. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		try {			
			File foldOut = new File(folderOut);
			String writeCommandPrefix = "write({fileOut=>"+foldOut.getAbsolutePath()+File.separator;
			File foldIn = new File(folderIn);
			if(foldIn.isDirectory()) {
				String[] allFiles = foldIn.list();
				CRNReducerCommandLine.println(out,bwOut,"Loading all CNF files in folder:");
				CRNReducerCommandLine.println(out,bwOut,"\t"+folderIn);
				CRNReducerCommandLine.println(out,bwOut,"The folder contains "+allFiles.length+" files:");
				for(int i=0;i<allFiles.length;i++) {
					CRNReducerCommandLine.println(out,bwOut,"\t"+allFiles[i]);
				}
				CRNReducerCommandLine.println(out,bwOut,"");
				//boolean skip=true;
				for(int i=0;i<allFiles.length;i++) {
					String current = foldIn.getAbsolutePath()+File.separator+allFiles[i];
					if(allFiles[i].toLowerCase().endsWith(".cnf") ) {
//						if(skip) {
//							//if(allFiles[i].endsWith("vlsat2_33582_4529625.cnf")) {
//							//if(allFiles[i].endsWith("vlsat2_45150_7165285.cnf")) {
//							//if(allFiles[i].endsWith("vlsat2_27507_3314450.cnf")) {
//							//if(allFiles[i].endsWith("vlsat2_5568_1124240.cnf")) {
//							if(allFiles[i].endsWith("vlsat2_28930_6497511.cnf")) {
//								skip=false;
//							}
//							else {
//								continue;
//							}
//						}
						boolean failed=false;
						try { 
							SupportedFormats format=SupportedFormats.CNFasPoly;
							failed=loadFile(current,format,true,true,null,out,bwOut,false,false);
											//failed=loadFile(current,SupportedFormats.CNFasPoly,true,true,null,out,bwOut,false,false);
							//CRNReducerCommandLine.println(out,bwOut,"");
						} catch (FileNotFoundException e) {
							CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"File not found: "+folderIn,DialogType.Error);
						} catch(UnsupportedFormatException ue) {
							String errorMSG = "Loading failed because we got a clause too large.\n\t"+ue.getMessage();
							CRNReducerCommandLine.printWarning(out, bwOut,errorMSG);
							//return false;
							continue;
						}
						
						if(crn.getReactions().size()==0 || crn.getSpecies().size()==0) {
							String errorMSG = "Loading failed because we got a model with no dynamics. The model might be not supported or there might be problems in the importer.";
							CRNReducerCommandLine.printWarning(out, bwOut,errorMSG);
							//return false;
							continue;
						}
						else {
							if(!failed) {
								boolean prep=false;
								boolean reducedModel=false;
								addReduceCommand(writeCommandPrefix, allFiles[i],"FE",prep,reducedModel);
//								if(crn.getUserDefinedPartition()!=null && crn.getUserDefinedPartition().size()>0) {
//									addReduceCommand(writeCommandPrefix, allFiles[i],"FE",true,false);
//								}
								
								
								addReduceCommand(writeCommandPrefix, allFiles[i],"BE",prep,reducedModel);
								if(crn.getUserDefinedPartition()!=null && crn.getUserDefinedPartition().size()>0) {
									addReduceCommand(writeCommandPrefix, allFiles[i],"BE",true,false);
								}
								
								
								//write({fileOut=>/Users/andrea/OneDrive - Danmarks Tekniske Universitet/runtimes/runtime-ERODE.product(4)/TCS_CMSB/BioModels_Database-r31_pub-sbml_files/Imported_BioModels_Database-r31_pub-sbml_files/curated/BIOMD0000000002.ode})
								String writeCommand = writeCommandPrefix+AbstractImporter.overwriteExtensionIfEnabled(allFiles[i], "_ode", true) + "})";
								handleWriteCommand(writeCommand, out, bwOut);
								CRNReducerCommandLine.println(out,bwOut,"");
								
								
								
								for(ICommand cmd : crn.getCommands()){
									String cmdStr=cmd.toCRNFormat();
									String reduction=cmd.getName().replace("reduce", "");
									this.handleReduceCommand(cmdStr, false, reduction, out, bwOut);
								}
							}
						}
						CRNReducerCommandLine.println(out,bwOut,"");

//						if(!failed) {
//							String writeCommand = writeCommandPrefix+AbstractImporter.overwriteExtensionIfEnabled(allFiles[i], "ode", true) + "})";
//							handleWriteCommand(writeCommand, out, bwOut);
//							CRNReducerCommandLine.println(out,bwOut,"");
//						}

					}
				}
			}
		} catch (UnsupportedFormatException e) {
			if(e.getMessage()!=null){
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+folderIn+" failed.\nError message:\n"+e.getMessage(),DialogType.Error);
			}
			else{
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+folderIn+" failed.",DialogType.Error);
			}
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled IO errors.\nError message:\n"+e.getMessage(),DialogType.Error);
			//CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return false;
		} catch (JDOMException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled errors.\nError message:\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}  catch (XMLStreamException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled error.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}
		
		return true;
	}
	public void addReduceCommand(String writeCommandPrefix, String file, String redCommand,boolean userPrep,boolean reducedFile) {
		List<CommandParameter> params=new ArrayList<>();
		String redFile = writeCommandPrefix.replace("write({fileOut=>","");
		int sep=redFile.lastIndexOf(File.separator);
		String redPref=redFile.substring(0,sep);
		redFile=redFile.substring(sep+1);
		redFile="red"+File.separator+redFile;
		redFile=redPref+File.separator+redFile;
		redFile+=AbstractImporter.overwriteExtensionIfEnabled(file, "", true)+"_"+redCommand;
		if(userPrep) {
			redFile+="_userPrep";
		}
		redFile="\""+redFile+"._ode\"";
		
		CommandParameter cp;
		if(reducedFile) {
			cp = new CommandParameter("reducedFile", redFile);
			params.add(cp);
		}
		else {
			cp = new CommandParameter("computeOnlyPartition", "true");
			params.add(cp);
		}
		
		if(userPrep) {
			cp=new CommandParameter("prePartition", "USER");
			params.add(cp);
		}
		
		
		String csvFile=redPref+File.separator+"red"+File.separator+redCommand+((userPrep)?"user":"");
		csvFile="\""+csvFile+".csv\"";
		cp=new CommandParameter("csvFile", csvFile);
		params.add(cp);
		
		Command cmd= new Command("reduce"+redCommand, params);
		getCRN().addCommand(cmd);
		
		//"reduceFE"
//		if(crn.getUserDefinedPartition()!=null && crn.getUserDefinedPartition().size()>0) {
//			params=new ArrayList<>();
//			redFile = writeCommandPrefix.replace("write({fileOut=>","");
//			redFile+=AbstractImporter.overwriteExtensionIfEnabled(file, "", true)+"_FE_UserPrep.ode" + "})";
//			cp = new CommandParameter("reducedFile", redFile);
//			params.add(cp);
//			cmd= new Command("reduceFE", params);
//			getCRN().addCommand(cmd);
//		}
	}
	
	private boolean handleImportSBMLFolderCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) {
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return false;
		}
		boolean forceMassAction=false;
		String folderIn=null;
		String folderOut=null;
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("folderIn=>")){
				if(parameters[p].length()<="folderIn=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the folder from which to read. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				folderIn = parameters[p].substring("folderIn=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("folderOut=>")){
				if(parameters[p].length()<="folderOut=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the folder where to write the imported models. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				folderOut = parameters[p].substring("folderOut=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("forceMassAction=>")){
				if(parameters[p].length()<="forceMassAction=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify whether we should force a mass action intepretation of the model. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				forceMassAction = Boolean.valueOf(parameters[p].substring("forceMassAction=>".length(), parameters[p].length()));
			}
			else if(parameters[p].equals("")){
				continue;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return false;
			}
		}
		if(folderIn ==null || folderIn.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the file to be loaded. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		try {
			//new String[]{String.valueOf(forceMassAction)},
			
			File foldOut = new File(folderOut);
			String writeCommandPrefix = "write({fileOut=>"+foldOut.getAbsolutePath()+File.separator;
			
			File foldIn = new File(folderIn);
			if(foldIn.isDirectory()) {
				String[] allFiles = foldIn.list();
				CRNReducerCommandLine.println(out,bwOut,"Loading all SBML files in folder:");
				CRNReducerCommandLine.println(out,bwOut,"\t"+folderIn);
				CRNReducerCommandLine.println(out,bwOut,"The folder contains "+allFiles.length+" files:");
				for(int i=0;i<allFiles.length;i++) {
					CRNReducerCommandLine.println(out,bwOut,"\t"+allFiles[i]);
				}
				CRNReducerCommandLine.println(out,bwOut,"");
				String problem;
				for(int i=0;i<allFiles.length;i++) {
					String current = foldIn.getAbsolutePath()+File.separator+allFiles[i];
					if(allFiles[i].toLowerCase().endsWith(".xml") || allFiles[i].toLowerCase().endsWith(".sbml")) {
						boolean failed=false;
						boolean fba=false;
						boolean nonIntStoich=false;
						boolean negativeStoich=false;
						try { 
							failed = loadFile(current,SupportedFormats.SBML,true,true,new String[]{String.valueOf(forceMassAction)},out,bwOut,false,false);
							CRNReducerCommandLine.println(out,bwOut,"");
						} catch (FileNotFoundException e) {
							CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"File not found: "+folderIn,DialogType.Error);
							failed=true;
						} catch (NullPointerException | org.sbml.jsbml.SBMLException | java.lang.IndexOutOfBoundsException | IOException | java.lang.IllegalArgumentException | com.ctc.wstx.exc.WstxParsingException e ) {
							//CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled error.\nError message:\n"+e.getMessage(),DialogType.Error);
							//CRNReducerCommandLine.printStackTrace(out,bwOut,e);
							CRNReducerCommandLine.println(out, bwOut, "Loading failed due to unhandled error. The model might be not supported.\nError message:\n"+e.getMessage());
							CRNReducerCommandLine.printStackTrace(out,bwOut,e);
							failed=true;
						} catch(FluxBalanceAnalysisModel e) {
							CRNReducerCommandLine.println(out, bwOut, "Loading failed: this is a flux balance analysis model.\nError message:\n"+e.getMessage()+"\n");
							//CRNReducerCommandLine.printStackTrace(out,bwOut,e);
							failed=true;
							fba=true;
						} catch(NonIntegerStoichiometryException e) {
							CRNReducerCommandLine.println(out, bwOut, "Loading failed: the model has reactions with non integer stoichiometries.\nError message:\n"+e.getMessage()+"\n");
							//CRNReducerCommandLine.printStackTrace(out,bwOut,e);
							failed=true;
							nonIntStoich = true;
						} catch(NegativeStoichiometryException e) {
							CRNReducerCommandLine.println(out, bwOut, "Loading failed: the model has reactions with non integer stoichiometries.\nError message:\n"+e.getMessage()+"\n");
							//CRNReducerCommandLine.printStackTrace(out,bwOut,e);
							failed=true;
							negativeStoich = true;
						}
						
						if(failed) {
							String copyFolder = foldIn.getAbsolutePath()+File.separator+"NotSupported"+File.separator+"UnknownProblems"+File.separator;
							if(fba) {
								copyFolder = foldIn.getAbsolutePath()+File.separator+"NotSupported"+File.separator+"FBA"+File.separator;
							} else if(nonIntStoich) {
								copyFolder = foldIn.getAbsolutePath()+File.separator+"NotSupported"+File.separator+"NonIntStoichiometries"+File.separator;
							} else if(negativeStoich) {
								copyFolder = foldIn.getAbsolutePath()+File.separator+"NotSupported"+File.separator+"NegativeStoichiometries"+File.separator;
							}
							handleFailedLoadingOfSBML(out, bwOut, foldIn, allFiles, i, current,"",copyFolder);
							/*
							String copy=copyFolder+allFiles[i];
							CRNReducerCommandLine.println(out, bwOut, "Copying the file in "+copyFolder);
							try {
								copyFile(current,copy);
							} catch (IOException e) {
								CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Problems in copyin the not supported file.\nError message:\n"+e.getMessage(),DialogType.Error);
								CRNReducerCommandLine.printStackTrace(out,bwOut,e);
							}
							*/
						}
						else if(crn.getReactions().size()==0 || crn.getSpecies().size()==0) {
							failed=true;
							String errorMSG = "Loading failed because we got a model with no dynamics. The model might be not supported or there might be problems in the importer.";
							String copyFolder = foldIn.getAbsolutePath()+File.separator+"NotSupported"+File.separator+"ProblemsInImporter"+File.separator+"NoDynamics"+File.separator;
							handleFailedLoadingOfSBML(out, bwOut, foldIn, allFiles, i, current,errorMSG,copyFolder);
						}
						else if(!(problem = crn.reactionKineticsReferToMissingSpeciesOrParameter()).equals("NONE")) {
							failed=true;
							String errorMSG = "";// "Loading failed because reaction kinetics used unsupported functions ("+problem.replace("UNDEFFUNCTION:", "")+").\nThe model might be not supported or there might be problems in the importer.";
							String copyFolder = foldIn.getAbsolutePath()+File.separator+"NotSupported"+File.separator;
							if(problem.startsWith("UNDEFFUNCTION:")){
								errorMSG = "Loading failed because reaction kinetics use unsupported functions ("+problem.replace("UNDEFFUNCTION:", "")+").\nThe model might be not supported or there might be problems in the importer.";
								copyFolder += "FunctionsOrSimilar"+File.separator;
							}
							else if(problem.equals("JUST_ZERO_KINETICS")) {
								errorMSG = "Loading failed because we got a model with no dynamics: all kinetic laws evaluate to 0 for species concentrations=1+IC.\nThe model might be not supported or there might be problems in the importer.";
								copyFolder += "ProblemsInImporter"+File.separator+"NoDynamics"+File.separator + "ZeroKineticLaws"+File.separator;
							}
							else {
								errorMSG = "Loading failed because reaction kinetics refer to missing species or parameters ("+problem+").\nThe model might be not supported or there might be problems in the importer.";
								//copyFolder = foldIn.getAbsolutePath()+File.separator+"NotSupported"+File.separator+"ProblemsInImporter"+File.separator+"KineticsWithUndeclaredSpeciesOrParameters"+File.separator;
								copyFolder += "ProblemsInImporter"+File.separator+"KineticsWithUndeclaredSpeciesOrParameters"+File.separator;
							}
							//copyFolder = foldIn.getAbsolutePath()+File.separator+"NotSupported"+File.separator+"ProblemsInImporter"+File.separator+"KineticsWithUndeclaredSpeciesOrParameters"+File.separator;
							handleFailedLoadingOfSBML(out, bwOut, foldIn, allFiles, i, current,errorMSG,copyFolder);
							
							//REMOVE
							//String writeCommand = writeCommandPrefix+AbstractImporter.overwriteExtensionIfEnabled(allFiles[i], "ode", true) + "})";
							//handleWriteCommand(writeCommand, out, bwOut);
							//CRNReducerCommandLine.println(out,bwOut,"");
						}
						else {
							//write({fileOut=>/Users/andrea/OneDrive - Danmarks Tekniske Universitet/runtimes/runtime-ERODE.product(4)/TCS_CMSB/BioModels_Database-r31_pub-sbml_files/Imported_BioModels_Database-r31_pub-sbml_files/curated/BIOMD0000000002.ode})
							String writeCommand = writeCommandPrefix+AbstractImporter.overwriteExtensionIfEnabled(allFiles[i], "ode", true) + "})";
							handleWriteCommand(writeCommand, out, bwOut);
							CRNReducerCommandLine.println(out,bwOut,"");
						}
						CRNReducerCommandLine.println(out,bwOut,"");
					}
				}
			}
		} catch (UnsupportedFormatException e) {
			if(e.getMessage()!=null){
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+folderIn+" failed.\nError message:\n"+e.getMessage(),DialogType.Error);
			}
			else{
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+folderIn+" failed.",DialogType.Error);
			}
			return false;
//		} catch (IOException e) {
//			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled IO errors.\nError message:\n"+e.getMessage(),DialogType.Error);
//			//CRNReducerCommandLine.printStackTrace(out,bwOut,e);
//			return false;
		} catch (JDOMException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled errors.\nError message:\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}  catch (XMLStreamException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled error.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}
		
		return true;
	}
	private void handleFailedLoadingOfSBML(MessageConsoleStream out, BufferedWriter bwOut, File foldIn,
			String[] allFiles, int i, String current, String errorMSG,String copyFolder) {
		/*
		String copyFolder = foldIn.getAbsolutePath()+File.separator+"NotSupported"+File.separator+"UnknownProblems"+File.separator;
		String copy=copyFolder+allFiles[i];
		CRNReducerCommandLine.println(out, bwOut, "Copying the file in "+copyFolder);
		try {
			copyFile(current,copy);
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Problems in copyin the not supported file.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}
		*/
		//String errorMSG = "Loading failed because we got a model with no dynamics. The model might be not supported or there might be problems in the importer.";
		if(errorMSG!=null && errorMSG.length()>0) {
			CRNReducerCommandLine.println(out, bwOut, errorMSG);
		}
		//String copyFolder = foldIn.getAbsolutePath()+File.separator+"NotSupported"+File.separator+"ProblemsInImporter"+File.separator;
		String copy=copyFolder+allFiles[i];
		CRNReducerCommandLine.println(out, bwOut, "Copying the file in "+copyFolder);
		try {
			copyFile(current,copy);
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Problems in copying the not supported file.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}
	}
	
	private boolean handleImportLBSCommand(String command, MessageConsoleStream out, BufferedWriter bwOut) {
		//sample command: importLBS({fileName=>./LBSNetworks/am.lbs})
		boolean ignoreCommands=false;
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return false;
		}
		String fileName=null;
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("fileIn=>")){
				if(parameters[p].length()<="fileIn=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file to read. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				fileName = parameters[p].substring("fileIn=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("ignoreCommands=>")){
				if(parameters[p].length()<="ignoreCommands=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if the commands have to be ignored (true) or not (false). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return false;
				}
				String ignoreCommandsSTR = parameters[p].substring("ignoreCommands=>".length(), parameters[p].length());
				if(ignoreCommandsSTR.equalsIgnoreCase("true")){
					ignoreCommands=true;
				}
				
			}
			else if(parameters[p].equals("")){
				continue;
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return false;
			}
		}
		if(fileName ==null || fileName.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the file to be loaded. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return false;
		}
		try {
			loadFile(fileName,SupportedFormats.LBS,true,ignoreCommands,out,bwOut,false);
		/*} catch (FileNotFoundException e) {
			CRNReducerCommandLine.println(out,bwOut,"File not found: "+fileName,DialogType.Error);
			return false;
		} catch (UnsupportedFormatException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading of "+fileName+" failed.",DialogType.Error);
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading failed due to unhandled error. This is the exception stack trace.");
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.println(out,bwOut,"Loading failed due to unhandled error. This is the exception stack trace.");
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}*/
		} catch (FileNotFoundException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"File not found: "+fileName,DialogType.Error);
			return false;
		} catch (UnsupportedFormatException e) {
			if(e.getMessage()!=null){
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.\nError message:\n"+e.getMessage(),DialogType.Error);
			}
			else{
				CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading of "+fileName+" failed.",DialogType.Error);
			}
			return false;
		} catch (IOException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled IO errors.\nError message:\n"+e.getMessage(),DialogType.Error);
			//CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		} catch (JDOMException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled errors.\nError message:\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}  catch (XMLStreamException e) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Loading failed due to unhandled error.\nError message:\n"+e.getMessage(),DialogType.Error);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}
			
		return true;
	}

//	public Matrix handleMetrics(IPartition partition, double lambda, MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException, IOException {
//		//HashMap<Pair,Double> pairsToMetric = new LinkedHashMap<Pair, Double>();
//		//String msg=BasicExample.myBasicExample();
//		//return new StringAndPairs(pairsToMetric, msg);
//		
//		
//	}
	
	public IPartitionAndBoolean handleReduceCommand(String command, boolean updateCRN, String reduction, MessageConsoleStream out, BufferedWriter bwOut) 
			throws UnsupportedFormatException, Z3Exception, IOException {
		//sample command: reduceDSB({reducedFile=>outputFileNameOfReducedCRN.net,groupedFile=>outputFileNameOfGroupedCRN.net})
		//reduceMSB({reducedFile=>outputFileNameOfReducedCRN.net,groupedFile=>outputFileNameOfGroupedCRN.net})
		//reduceEFL({icPrePartitioning=>true,reducedFile=>outputFileNameOfReducedCRN.net,groupedFile=>outputFileNameOfGroupedCRN.net})
		String[] parameters = CRNReducerCommandLine.getParameters(command);
		if(parameters==null){
			CRNReducerCommandLine.println(out,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return null;
		}
		//String reduction=null;
		String reducedFileName=null;
		String partitionInfoFileName=null;
		String groupedFileName=null;
		String sameICFileName=null;
		String csvSMTTimeFileName=null;
		String typeOfGroupedFile="CRN";
		String prePartitionWRTIC="false";
		String prePartitionWRTVIEWS="false";
		String computeOnlyPartition="false";
		String prePartitionUserDefined="false";
		String prePartitionOutputs="false";
		String prePartitionOutputsSingleton="false";
		String certainConstants="true";
		String epsilonString = "0";
		String deltaString = "0";
		String deltaPercentageString="0";
		boolean doNotAddDeltaToRawConstants=false;
		String modelWithBigM=null;//for UCTMCfe and uncertainSE
		boolean epsilonSpecified = false;
		boolean addSelfLoops=false;
		boolean oneLabelAtATime=false;
		boolean 	halveRatesOfHomeoReactions = false;
		String csvFile=null;
		
		HowToComputeMm hotToComputeMm=HowToComputeMm.PERCEPSCLOSURE;
		double percentagePertCoRN=-1;
		double absolutePertCoRN=-1;
		double percentageClosureCoRN=-1;
		boolean onlyFEonCentreBounds=false;
		double absoluteClosureCoRN=-1;
		double lowerBoundFactorCoRN=-1;
		double upperBoundFactorCoRN=-1;
		
		/*if(reduction.equalsIgnoreCase("EFL")||reduction.equalsIgnoreCase("BDE")||reduction.equalsIgnoreCase("BB")||reduction.equalsIgnoreCase("NBB")){
			prePartitionWRTIC="true";
		}
		else{
			prePartitionWRTVIEWS="true";
		}*/
		
		boolean print=true;
		boolean sumReductionAlgorithm=false;
		
		if(reduction.equalsIgnoreCase(Reduction.FE.name())||reduction.equalsIgnoreCase(Reduction.BE.name()) || reduction.equalsIgnoreCase("BE_AAt") /*|| reduction.equalsIgnoreCase("JUSTBEREDUCTION")*//*||reduction.equalsIgnoreCase(Reduction.SE.name())*/){
			sumReductionAlgorithm=true;
		}
		
		for(int p=0;p<parameters.length;p++){
			if(parameters[p].startsWith("fileIn=>")){
				boolean loadingSuccessful = invokeLoad(parameters[p],out,bwOut);
				if(!loadingSuccessful){
					CRNReducerCommandLine.println(out,bwOut,"The loading of the file failed. I skip this command: "+command);
					return null;
				}
			}
			else if(parameters[p].equals("")){
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
			else if(parameters[p].startsWith("reducedFile=>")){
				if(parameters[p].length()<="reducedFile=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write the reduced model. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				if(reduction.equals("CoRNFE")) {
					partitionInfoFileName = parameters[p].substring("reducedFile=>".length(), parameters[p].length());
				}
				else {
					reducedFileName = parameters[p].substring("reducedFile=>".length(), parameters[p].length());
				}
				
			}
			else if(parameters[p].startsWith("epsilon=>")){
				if(parameters[p].length()<="epsilon=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the epsilon value. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				epsilonString = parameters[p].substring("epsilon=>".length(), parameters[p].length());
				epsilonSpecified=true;
			}
			else if(parameters[p].startsWith("percentagePerturbation=>")){
				if(parameters[p].length()<="percentagePerturbation=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the percentage perturbation for CoRN. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				percentagePertCoRN = Double.valueOf(parameters[p].substring("percentagePerturbation=>".length(), parameters[p].length()));
				hotToComputeMm=HowToComputeMm.PERCPERT;
			}
			else if(parameters[p].startsWith("absolutePerturbation=>")){
				if(parameters[p].length()<="absolutePerturbation=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the percentage perturbation for CoRN. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				absolutePertCoRN = Double.valueOf(parameters[p].substring("absolutePerturbation=>".length(), parameters[p].length()));
				hotToComputeMm=HowToComputeMm.ABSPERT;
			}
			else if(parameters[p].startsWith("percentageClosure=>")){
				if(parameters[p].length()<="percentageClosure=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the percentage value of epsilon to compute the closure or reaction dynamics for CoRN. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				percentageClosureCoRN = Double.valueOf(parameters[p].substring("percentageClosure=>".length(), parameters[p].length()));
				hotToComputeMm=HowToComputeMm.PERCEPSCLOSURE;
			}
			else if(parameters[p].startsWith("onlyFEonCentreBounds=>")){
				if(parameters[p].length()<="onlyFEonCentreBounds=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify is only FE shall be run on the center of the bounds (corn is not actually run on m and M. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				onlyFEonCentreBounds=parameters[p].substring("onlyFEonCentreBounds=>".length(), parameters[p].length()).equals("true");
				hotToComputeMm=HowToComputeMm.PERCEPSCLOSURE;
			}
			else if(parameters[p].startsWith("absoluteClosure=>")){
				if(parameters[p].length()<="absoluteClosure=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the absolute value of epsilon to compute the closure or reaction dynamics for CoRN. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				absoluteClosureCoRN = Double.valueOf(parameters[p].substring("absoluteClosure=>".length(), parameters[p].length()));
				hotToComputeMm=HowToComputeMm.ABSEPSCLOSURE;
			}
			else if(parameters[p].startsWith("lowerFactor=>")){
				if(parameters[p].length()<="lowerFactor=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the factor by which multiplying the rate to get the lower bounds for CoRN. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				lowerBoundFactorCoRN = Double.valueOf(parameters[p].substring("lowerFactor=>".length(), parameters[p].length()));
				hotToComputeMm=HowToComputeMm.LowerUpperBoundRatios;
			}
			else if(parameters[p].startsWith("upperFactor=>")){
				if(parameters[p].length()<="upperFactor=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the factor by which multiplying the rate to get the upper bounds for CoRN. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				upperBoundFactorCoRN = Double.valueOf(parameters[p].substring("upperFactor=>".length(), parameters[p].length()));
				hotToComputeMm=HowToComputeMm.LowerUpperBoundRatios;
			}
			else if(parameters[p].startsWith("certainConstants=>")){
				if(parameters[p].length()<="certainConstants=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify whether the constants should be considered certain (true) or uncertain (false). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				certainConstants=parameters[p].substring("certainConstants=>".length(), parameters[p].length());
			}
			
			else if(parameters[p].startsWith("delta=>")){
				if(parameters[p].length()<="delta=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the delta value. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				deltaString = parameters[p].substring("delta=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("deltaPercentage=>")){
				if(parameters[p].length()<="deltaPercentage=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the deltaPercentage value (from 0 to 1). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				deltaPercentageString = parameters[p].substring("deltaPercentage=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("doNotAddDeltaToRawConstants=>")){
				if(parameters[p].length()<="doNotAddDeltaToRawConstants=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if we should not perturb reactions with raw numeric constants. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				doNotAddDeltaToRawConstants = Boolean.valueOf(parameters[p].substring("doNotAddDeltaToRawConstants=>".length(), parameters[p].length()));
			}
			else if(parameters[p].startsWith("modelWithBigM=>")){
				if(parameters[p].length()<="modelWithBigM=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to find the model with parameters the upper extreme of the intervals (M). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				modelWithBigM = parameters[p].substring("modelWithBigM=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("reductionAlgorithm=>")){
				if(parameters[p].length()<="reductionAlgorithm=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if the old or new reduction algorithm should be used to reduce the model. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				String reductionAlgorithmString = parameters[p].substring("reductionAlgorithm=>".length(), parameters[p].length());
				if(reductionAlgorithmString.equalsIgnoreCase("NEW")){
					sumReductionAlgorithm=true;
				}
				else if(reductionAlgorithmString.equalsIgnoreCase("OLD")){
					sumReductionAlgorithm=false;
				} 
			}
			else if(parameters[p].startsWith("groupedFile=>")){
				if(parameters[p].length()<="groupedFile=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write the grouped model. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				groupedFileName = parameters[p].substring("groupedFile=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("csvFile=>")){
				if(parameters[p].length()<="csvFile=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write the simulation data. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				csvFile = parameters[p].substring("csvFile=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("sameICFile=>")){
				if(parameters[p].length()<="sameICFile=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file where to write the model with same IC for the species of each block. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				sameICFileName = parameters[p].substring("sameICFile=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("computeOnlyPartition=>")){
				if(parameters[p].length()<="computeOnlyPartition=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if or not only the partition has to be computed (without thus reducing the model). ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				computeOnlyPartition = parameters[p].substring("computeOnlyPartition=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("typeOfGroupedFile=>")){
				if(parameters[p].length()<="typeOfGroupedFile=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify the type of the grouped file.");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				typeOfGroupedFile= parameters[p].substring("typeOfGroupedFile=>".length(), parameters[p].length());
			}
			else if(parameters[p].startsWith("addSelfLoops=>")){
				if(parameters[p].length()<="addSelfLoops=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if self loops shall be added (true) or not (false) for SMB.");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				addSelfLoops = Boolean.valueOf(parameters[p].substring("addSelfLoops=>".length(), parameters[p].length()));
			}
			else if(parameters[p].startsWith("oneLabelAtATime=>")){
				if(parameters[p].length()<="oneLabelAtATime=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if one label at a time should be used for SMB.");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				oneLabelAtATime = Boolean.valueOf(parameters[p].substring("oneLabelAtATime=>".length(), parameters[p].length()));
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
					prePartitionWRTVIEWS="false";
					prePartitionUserDefined = "false";
				}
				else if(prep.compareToIgnoreCase("VIEWS")==0){
					prePartitionWRTVIEWS = "true";
					prePartitionWRTIC = "false";
					prePartitionUserDefined = "false";
				}
				else if(prep.compareToIgnoreCase("NO")==0){
					prePartitionWRTVIEWS = "false";
					prePartitionWRTIC = "false";
					prePartitionUserDefined = "false";
				}
				else if(prep.compareToIgnoreCase("USER")==0){
					prePartitionWRTVIEWS = "false";
					prePartitionWRTIC = "false";
					prePartitionUserDefined = "true";
				}
				else if(prep.compareToIgnoreCase("Outputs")==0){
					prePartitionWRTVIEWS = "false";
					prePartitionWRTIC = "false";
					prePartitionUserDefined = "false";
					prePartitionOutputs = "true";
					prePartitionOutputsSingleton = "false";
				}
				else if(prep.compareToIgnoreCase("Outputs_singleton")==0){
					prePartitionWRTVIEWS = "false";
					prePartitionWRTIC = "false";
					prePartitionUserDefined = "false";
					prePartitionOutputs = "false";
					prePartitionOutputsSingleton = "true";
				}
				else if(prep.compareToIgnoreCase("USER_and_IC")==0){
					prePartitionWRTVIEWS = "false";
					prePartitionWRTIC = "true";
					prePartitionUserDefined = "true";
				}
				else{
					CRNReducerCommandLine.println(out,bwOut,"Unknown prepartitioning option \""+prep+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
					return null;
				}
			}
			else if(reduction.equals("smb") && parameters[p].startsWith("halveRatesOfHomeoReactions=>")){
				if(parameters[p].length()<="halveRatesOfHomeoReactions=>".length()){
					CRNReducerCommandLine.println(out,bwOut,"Please, specify if or not I should halve the rates of homeoreactions. ");
					CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
					return null;
				}
				halveRatesOfHomeoReactions = Boolean.valueOf(parameters[p].substring("halveRatesOfHomeoReactions=>".length(), parameters[p].length()));
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				return null;
			}
		}
		
		HashMap<ICRNReaction, BigDecimal> reactionToRateInModelBigM=null;

		if(crn==null){
			CRNReducerCommandLine.println(out,bwOut,"Before reducing a model it is necessary to load it. "); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return null;
		}

		if(reduction ==null || reduction.equals("")){
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the reduction technique. ");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return null;
		}
		
		if( crn.algebraicSpecies() > 0 && !(reduction.equalsIgnoreCase("bde") || reduction.equalsIgnoreCase("be")) ){
			CRNReducerCommandLine.println(out,bwOut,"Only reduceBDE and reduceBE are supported with DAEs");
			CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
			return null;
		}


		//groupedFileName = crn.getName()+"grouped.net";
		
		boolean writeReducedCRN = reducedFileName!=null && !reducedFileName.equals("");
		boolean writeGroupedCRN = groupedFileName!=null && !groupedFileName.equals("");
		boolean writeSameICCRN = sameICFileName!=null && !sameICFileName.equals("");
		
		double epsilon=0;
		if(epsilonSpecified){
			epsilon = crn.getMath().evaluate(epsilonString);
			if(epsilon<0){
				CRNReducerCommandLine.printWarning(out, bwOut, true, messageDialogShower, "Please, specify a non-negative epsilon ("+epsilon+").\n\t I skip this command.",DialogType.Error);
				return null;
			}
		}
		
		boolean forcePrintingOfPartition=false;
		ICRN crnToConsider = crn;
		IPartition initial = partition;
		ArrayList<HashSet<ISpecies>> userDefinedPartition = crnToConsider.getUserDefinedPartition();
		
		if(crnToConsider.algebraicSpecies()>0){
			if(reduction.equalsIgnoreCase("BDE") || reduction.equalsIgnoreCase("BE")) {
				//We don't have an algorithm to compute reduced DAEs. 
				//computeOnlyPartition="true";
				//I just write the partition in a file
				////partitionInfoFileName=crnToConsider.getName()+reduction+".txt";
				//forcePrintingOfPartition=true;
				//Split blocks of initial partition separating algebraic and non-algebraic variables
				IPartition initialPartitionAfterSplittingAlgebraic = new Partition(crnToConsider.getSpecies().size());
				{
					IBlock current = initial.getFirstBlock();
					while(current!=null) {
						IBlock currentNonAlg = new Block();
						boolean addedNonAlg=false;
						IBlock currentAlg = new Block();
						boolean addedAlg=false;
						for(ISpecies species : current.getSpecies()) {
							if(species.isAlgebraic()) {
								if(!addedAlg) {
									initialPartitionAfterSplittingAlgebraic.add(currentAlg);
									addedAlg=true;
								}
								currentAlg.addSpecies(species);
							}
							else {
								if(!addedNonAlg) {
									initialPartitionAfterSplittingAlgebraic.add(currentNonAlg);
									addedNonAlg=true;
								}
								currentNonAlg.addSpecies(species);
							}
						}
						current=current.getNext();
					}
					initial = initialPartitionAfterSplittingAlgebraic;
				}
				
				//Split blocks of user partition separating algebraic and non-algebraic variables
				if(prePartitionUserDefined!=null && prePartitionUserDefined.equalsIgnoreCase("true")){
					ArrayList<HashSet<ISpecies>> userDefinedPartitionAfterSplittingAlgebraic = new ArrayList<>(userDefinedPartition.size());
					for(HashSet<ISpecies> current : userDefinedPartition) {
						HashSet<ISpecies> currentNonAlg = new HashSet<>();
						HashSet<ISpecies> currentAlg = new HashSet<>();
						for(ISpecies species : current) {
							if(species.isAlgebraic()) {
								currentAlg.add(species);
							}
							else {
								currentNonAlg.add(species);
							}
						}
						if(currentNonAlg.size()>0) {
							userDefinedPartitionAfterSplittingAlgebraic.add(currentNonAlg);
						}
						if(currentAlg.size()>0) {
							userDefinedPartitionAfterSplittingAlgebraic.add(currentAlg);
						}
					}
					userDefinedPartition=userDefinedPartitionAfterSplittingAlgebraic;
				}

				//BE works only with RN. We hence try to encode the DAE in an RN.
				if(reduction.equalsIgnoreCase("BE")) {
					//compute RN encoding
					if(print){
						//CRNReducerCommandLine.print(out,bwOut,"Prepartinioning with respect to the initial concentrations of the species...");
						CRNReducerCommandLine.print(out,bwOut,"Converting the model in reaction network form...");
					}
					long begin = System.currentTimeMillis();
					CRNandPartition crnAndSpecies=MatlabODEPontryaginExporter.computeRNEncoding(crnToConsider, crnToConsider.getReactions(), out, bwOut, initial,userDefinedPartition,false);
					long end = System.currentTimeMillis();
					if(print){
						//CRNReducerCommandLine.println(out,bwOut," completed. The "+crn.getSpecies().size()+" species have been prepartitioned in "+initial.size()+" blocks. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");
						CRNReducerCommandLine.println(out,bwOut," completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)"+".");
					}
					if(crnAndSpecies==null){
						CRNReducerCommandLine.printWarning(out, bwOut, true, messageDialogShower, "Problems in computing the RN encoding of the model.\n\t I skip this command.",DialogType.Error);
						return null;
					}
					crnToConsider = crnAndSpecies.getCRN();
					if(crnToConsider==null){
						CRNReducerCommandLine.printWarning(out, bwOut, true, messageDialogShower, "Problems in computing the RN encoding of the model.\n\t I skip this command.",DialogType.Error);
						return null;
					}
					initial=crnAndSpecies.getPartition();
					userDefinedPartition=crnToConsider.getUserDefinedPartition();
					
				}
			}
			else {
				CRNReducerCommandLine.printWarning(out, bwOut, true, messageDialogShower, "DAEs can be reduced only up to BDE or BE (via an implicit RN encoding). In both cases, the DAEs are treated as ODEs, after splitting algebraic and non-algebraic variables in the initial partitions.\n\t I skip this command.",DialogType.Error);
				return null;
			}
		}

		

		List<ILabel> labels;
		String originalCRNShort="Original model: "+crnToConsider.toStringShort();
		//String reducedCRNShort;
		IPartition obtainedPartition;
		CRNandPartition cp=null;
		String icWarning="";

		if(prePartitionWRTVIEWS!=null && prePartitionWRTVIEWS.equalsIgnoreCase("true")){
			if(print){
				//CRNReducerCommandLine.print(out,bwOut,"Prepartinioning with respect to the views/groups specified in the original file...");
				CRNReducerCommandLine.print(out,bwOut,"Views prepartinioning...");
			}
			if(getCRN().getViewNames()==null||getCRN().getViewNames().length==0){
				if(print){
					CRNReducerCommandLine.println(out,bwOut," trivially completed (NO VIEWS).");//+".\n\tThe "+crn.getSpecies().size()+" species have been prepartitioned in "+initial.size()+" blocks."
				}
			}
			else if(getCRN().getViewsAsMultiset()!=null){
				long begin = System.currentTimeMillis();
				initial= CRNBisimulationsNAry.prepartitionWRTVIEWS(crnToConsider,initial,false,out,bwOut,terminator);
				long end = System.currentTimeMillis();
				if(print){
					//CRNReducerCommandLine.println(out,bwOut," completed. The "+crn.getSpecies().size()+" species have been prepartitioned in "+initial.size()+" blocks. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");
					CRNReducerCommandLine.println(out,bwOut," completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)"+".\n\tThe "+crnToConsider.getSpecies().size()+" "+((crnToConsider.getMdelDefKind().equals(ODEorNET.ODE))?"variables":"species")+" have been prepartitioned in "+ blockOrBlocks(initial.size()));
				}					
			}
			else{
				if(print){
					CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Prepartitioning failed: only views which only sum species (e.g., \"s1 + s2\") are supported.",true,DialogType.Error);
					//CRNReducerCommandLine.println(out,bwOut," FAILED: the views do not allow it.");
				}
			}
		}
		else if(prePartitionOutputs!=null && prePartitionOutputs.equalsIgnoreCase("true")) {
			if(print){
				CRNReducerCommandLine.print(out,bwOut,"Prepartinioning with one block for all outputs...");
			}
			long begin = System.currentTimeMillis();
			HashSet<ISpecies> possibleOutputs = BoolCubeImporter.computePossibleOutputs(crnToConsider);
			ArrayList<HashSet<ISpecies>> possibleOutputsPartition = new ArrayList<>(1);
			possibleOutputsPartition.add(possibleOutputs);
			initial = CRNBisimulationsNAry.prepartitionUserDefined(crnToConsider.getSpecies(), possibleOutputsPartition, false, out, bwOut, terminator);
			long end = System.currentTimeMillis();
			if(print){
				//CRNReducerCommandLine.println(out,bwOut," completed. The "+crn.getSpecies().size()+" species have been prepartitioned in "+initial.size()+" blocks. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");
				CRNReducerCommandLine.println(out,bwOut," completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)"+".\n\tThe "+crnToConsider.getSpecies().size()+" "+((crnToConsider.getMdelDefKind().equals(ODEorNET.ODE))?"variables":"species")+" have been prepartitioned in "+ blockOrBlocks(initial.size()));
			}
		}
		else if(prePartitionOutputsSingleton!=null && prePartitionOutputsSingleton.equalsIgnoreCase("true")) {
			if(print){
				CRNReducerCommandLine.print(out,bwOut,"Prepartinioning with one per output...");
			}
			long begin = System.currentTimeMillis();
			HashSet<ISpecies> possibleOutputs = BoolCubeImporter.computePossibleOutputs(crnToConsider);
			ArrayList<HashSet<ISpecies>> possibleOutputsPartition = new ArrayList<>(possibleOutputs.size());
			for(ISpecies output : possibleOutputs) {
				HashSet<ISpecies> current = new LinkedHashSet<ISpecies>(1);
				current.add(output);
				possibleOutputsPartition.add(current);
			}
			initial = CRNBisimulationsNAry.prepartitionUserDefined(crnToConsider.getSpecies(), possibleOutputsPartition, false, out, bwOut, terminator);
			long end = System.currentTimeMillis();
			if(print){
				//CRNReducerCommandLine.println(out,bwOut," completed. The "+crn.getSpecies().size()+" species have been prepartitioned in "+initial.size()+" blocks. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");
				CRNReducerCommandLine.println(out,bwOut," completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)"+".\n\tThe "+crnToConsider.getSpecies().size()+" "+((crnToConsider.getMdelDefKind().equals(ODEorNET.ODE))?"variables":"species")+" have been prepartitioned in "+ blockOrBlocks(initial.size()));
			}
		}
		else {
			if(prePartitionUserDefined!=null && prePartitionUserDefined.equalsIgnoreCase("true")){
			initial = pepartitionAccordingToUserPartition(out, bwOut, print, crnToConsider,
					initial, userDefinedPartition, terminator);
			}

			if(prePartitionWRTIC!=null && prePartitionWRTIC.equalsIgnoreCase("true")){
				if(print){
					//CRNReducerCommandLine.print(out,bwOut,"Prepartinioning with respect to the initial concentrations of the species...");
					CRNReducerCommandLine.print(out,bwOut,"IC prepartinioning...");
				}
				long begin = System.currentTimeMillis();
				initial=ExactFluidBisimilarity.prepartitionWRTIC(crnToConsider,initial,false,out,bwOut,terminator);
				long end = System.currentTimeMillis();
				if(print){
					//CRNReducerCommandLine.println(out,bwOut," completed. The "+crn.getSpecies().size()+" species have been prepartitioned in "+initial.size()+" blocks. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");
					String speciesOrVar = "species";
					if(crnToConsider.getMdelDefKind().equals(ODEorNET.ODE)) {
						speciesOrVar = "variables";
					}
					CRNReducerCommandLine.println(out,bwOut," completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)"+".\n\tThe "+crnToConsider.getSpecies().size()+" "+speciesOrVar+" have been prepartitioned in "+ blockOrBlocks(initial.size()));
				}
			}
			else{
				if(reduction.equalsIgnoreCase("EFL")||reduction.equalsIgnoreCase("BDE")||reduction.equalsIgnoreCase("BB")||reduction.equalsIgnoreCase("BE")||reduction.equalsIgnoreCase("ENBB")
						||reduction.equalsIgnoreCase("BE_AAt")){
					//icWarning="Warning: each species of the reduced model has the initial concentration of the representative species of the corresponding partition block.";
					//icWarning="Warning: reduction performed without pre-partitioning according to the initial conditions. The partition might not be consistent with the initial conditions. Each species in the reduced model has the initial concentration of the representative element of the partition block.";
					if(sumReductionAlgorithm){
						icWarning="The partition might not be consistent with the initial conditions.\n          Each species in the reduced model has the sum of the initial concentrations of its partition block.";
					}
					else{
						icWarning="The partition might not be consistent with the initial conditions.\n          Each species in the reduced model has the initial concentration of the representative of its partition block.";
					}

				}
			}
		}
		


		String reductionName = reduction.toUpperCase();
		if(reductionName.equals("EFL")){
			reductionName="BB_old";
		}
		else if(reductionName.equals("DSB")){
			reductionName="FB_old";
		} 
		
		
		if(reductionName.equals("FB") || reductionName.equals("FB_old")){
			CRNReducerCommandLine.printWarning(out, bwOut, "The reduction technique "+reductionName+" has been superseded by a new one. Consider using the command 'reduceFE' instead.");
		}
		else if(reductionName.equals("BB") || reductionName.equals("BB_old")){
			CRNReducerCommandLine.printWarning(out, bwOut, "The reduction technique "+reductionName+" has been superseded by a new one. Consider using the command 'reduceBE' instead.");
		}
		
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
		
		String reducedModelName = null;
		if(reducedFileName!=null){
			reducedModelName = GUICRNImporter.getModelName(reducedFileName);
		}
		else{
			reducedModelName = GUICRNImporter.getModelName(crnToConsider.getName());
			reducedModelName = reducedModelName + reduction;
		}
		
		EpsilonDifferentialEquivalences epsilonDE=null;
		List<Double> smtChecksTime = null;
		
		IPartition obtainedPartitionOfParams = null;
		
		LinkedHashMap<String, String> extraColumnsForCSV=new LinkedHashMap<>(3);
		
		//Compute the partition
		long begin = System.currentTimeMillis();
		boolean succeeded=true;
		String smtTime=null;
		
		if(reduction.equalsIgnoreCase("DSB")||reduction.equalsIgnoreCase("smb")||reduction.equalsIgnoreCase("se")||/*reduction.equalsIgnoreCase("EMSB")||*/reduction.equalsIgnoreCase("fb")||reduction.equalsIgnoreCase("fe")||reduction.equalsIgnoreCase("enfb")||reduction.equalsIgnoreCase("ufe") || reduction.equalsIgnoreCase("uctmcfe") || reduction.equalsIgnoreCase("use") || reduction.equalsIgnoreCase("CoRNFE")){
			IPartitionAndBoolean obtainedPartitionAndBool=null;
			
			//FB, slower algorithm at time of CONCUR
			if(reduction.equalsIgnoreCase("DSB")){
				labels = SyntacticMarkovianBisimilarityOLD.computeUnaryBinaryLabels(crnToConsider);
				obtainedPartitionAndBool = DifferentialSpeciesBisimilarity.computeDSB(crnToConsider, labels, initial, verbose,out,bwOut,terminator);
			}
			//FB, TACAS algorithm
			else if(reduction.equalsIgnoreCase("FB")){
				//labels = crn.computeUnaryBinaryLabels();
				obtainedPartitionAndBool = CRNBisimulations.computeCoarsest(Reduction.FB,crnToConsider, initial, verbose,out,bwOut,print,terminator);
			}
			//N-ary FB which characterizes OFL. PNAS
			else if(reduction.equalsIgnoreCase("FE")){
				obtainedPartitionAndBool = CRNBisimulationsNAry.computeCoarsest(Reduction.FE,crnToConsider, initial, verbose,out,bwOut,terminator,messageDialogShower);
				//CORN_LumpabilityForControlRN.driverNodes_dn(out, bwOut, extraColumnsForCSV, obtainedPartitionAndBool.getPartition());
			}
			else if(reduction.equalsIgnoreCase("UCTMCFE")){
				
				begin = System.currentTimeMillis();
				PartitionAndMappingReactionToNewRate ret = UCTMCLumping.computeCoarsestUCTMCLumpingOrUncertainSE(Reduction.UCTMCFE, crnToConsider, initial, new BigDecimal(deltaString),BigDecimal.ZERO, false,modelWithBigM, verbose, out, bwOut, terminator, messageDialogShower);
				obtainedPartition= ret.getObtainedPartition();
				reactionToRateInModelBigM=ret.getReactionToRateInModelBigM();
				obtainedPartitionAndBool = new IPartitionAndBoolean(obtainedPartition, true);
				//computeOnlyPartition="true";
				if(reducedFileName!=null && reducedFileName.length()>0) {
					partitionInfoFileName = AbstractImporter.overwriteExtensionIfEnabled(reducedFileName,".txt",true);
				}
				//For computing bounds info on cyclin
				//sumReductionAlgorithm=true;
				//reducedFileName = crn.getName().replace(".tra", "")+"UCTMC_M.tra";
				
				
			}
			else if(reduction.equalsIgnoreCase("USE")){

				BigDecimal delt =new BigDecimal(deltaString);
				BigDecimal deltPerc =new BigDecimal(deltaPercentageString);
				
				begin = System.currentTimeMillis();
				PartitionAndMappingReactionToNewRate ret = UCTMCLumping.computeCoarsestUCTMCLumpingOrUncertainSE(Reduction.USE, crnToConsider, initial, 
						delt,deltPerc,doNotAddDeltaToRawConstants, modelWithBigM, verbose, out, bwOut, terminator, messageDialogShower);
				obtainedPartition = ret.getObtainedPartition();
				obtainedPartitionAndBool = new IPartitionAndBoolean(obtainedPartition, true);
				//computeOnlyPartition="true";
				if(reducedFileName!=null && reducedFileName.length()>0) {
					partitionInfoFileName = AbstractImporter.overwriteExtensionIfEnabled(reducedFileName,".txt",true);
				}
				//For computing bounds info on cyclin
				//sumReductionAlgorithm=true;
				//reducedFileName = crn.getName().replace(".tra", "")+"UCTMC_M.tra";


			}
			// Forward Control Equivalence: FE for linear systems with uncertain parameters - OLD REJECTED FROM CONCUR2019
			else if(reduction.equalsIgnoreCase("UFE")){
				//boolean transformConstantRatesInParams=true;
				//IPartitionsAndBoolean obtainedPartitionsAndBool = ControlEquivalences.computeCoarsest(Reduction.FCE,crnToConsider, initial, verbose,out,bwOut,terminator,messageDialogShower,transformConstantRatesInParams);
				
//				CRNReducerCommandLine.println(out,bwOut,"");
//				ExportSingleUseOfParams.expandMRMCCTMCWithSingleUseOfParametersModifyingTheModel(crn, out, bwOut);
				begin = System.currentTimeMillis();
				IPartitionsAndBoolean obtainedPartitionsAndBool = ControlEquivalences.computeCoarsestModifyingModelExpandingParams(Reduction.UFE,crnToConsider, initial, verbose,out,bwOut,terminator,messageDialogShower);
				//IPartitionsAndBoolean obtainedPartitionsAndBool = ControlEquivalencesArrays.computeCoarsestModifyingModelExpandingParams(Reduction.FCE,crnToConsider, initial, verbose,out,bwOut,terminator,messageDialogShower);
				
				obtainedPartitionAndBool = new IPartitionAndBoolean(obtainedPartitionsAndBool.getPartition(), obtainedPartitionsAndBool.getBool());
				obtainedPartitionOfParams = obtainedPartitionsAndBool.getSecondPartition();
				computeOnlyPartition="true";
				partitionInfoFileName = AbstractImporter.overwriteExtensionIfEnabled(reducedFileName,".txt",true);
			}
			else if(reduction.equalsIgnoreCase("ENFB")){
				//obtainedPartitionAndBool = CRNBisimulationsNAry.computeCoarsest(Reduction.ENFB,crn, initial, verbose,out,bwOut,terminator);
				epsilonDE = new EpsilonDifferentialEquivalences();
				obtainedPartitionAndBool = epsilonDE.computeCoarsest(Reduction.ENFB, BigDecimal.valueOf(epsilon),crnToConsider, initial, verbose, out,bwOut,terminator,true);
				//obtainedPartition = obtainedPartitionAndBool.getPartition();
			}
			else if(reduction.equalsIgnoreCase("smb"))
			{
				/*
				 * 
				 */
				
				if(oneLabelAtATime) {
					labels = SyntacticMarkovianBisimilarityOLD.computeUnaryBinaryLabels(crnToConsider);
					obtainedPartitionAndBool = SyntacticMarkovianBisimilarityOLD.computeSMB(crnToConsider, labels, initial, verbose,out,bwOut,terminator, messageDialogShower,addSelfLoops,halveRatesOfHomeoReactions);
				}
				else {
					//obtainedPartitionAndBool = SyntacticMarkovianBisimilarityAllLabels.computeSMB(crnToConsider, /*labels,*/ initial, verbose,out,bwOut,terminator, messageDialogShower,addSelfLoops,halveRatesOfHomeoReactions);
					obtainedPartitionAndBool = SyntacticMarkovianBisimilarityBinary.computeSMB(crnToConsider, /*labels,*/ initial, verbose,out,bwOut,terminator, messageDialogShower,/*addSelfLoops,*/halveRatesOfHomeoReactions);
					//obtainedPartitionAndBool = SyntacticMarkovianBisimilarityNary.computeSMB(crnToConsider, /*labels,*/ initial, verbose,out,bwOut,terminator, messageDialogShower,/*addSelfLoops,*/halveRatesOfHomeoReactions);
					/* The following does not work
					 * obtainedPartitionAndBool = SyntacticMarkovianBisimilarityAllLabelsNoSelfLoops.computeSMB(crnToConsider, initial, verbose,out,bwOut,terminator, messageDialogShower,halveRatesOfHomeoReactions);
					 */
				}
			}
			else if(reduction.equalsIgnoreCase("se"))
			{
				obtainedPartitionAndBool = SyntacticMarkovianBisimilarityNary.computeSE(crnToConsider, /*labels,*/ initial, verbose,out,bwOut,terminator, messageDialogShower,/*addSelfLoops,*/halveRatesOfHomeoReactions);
			}
			else if(reduction.equalsIgnoreCase("CoRNFE"))
			{
				//Control FE for Uncertain MAR-RN
				CORN_LumpabilityForControlRN CoRN = new CORN_LumpabilityForControlRN();
				obtainedPartitionAndBool =CoRN.computeCoarsest(crnToConsider, initial, verbose, out, bwOut, terminator, messageDialogShower,
						percentagePertCoRN,absolutePertCoRN,percentageClosureCoRN,absoluteClosureCoRN,lowerBoundFactorCoRN,upperBoundFactorCoRN,
						hotToComputeMm,certainConstants.equalsIgnoreCase("true"),onlyFEonCentreBounds,extraColumnsForCSV);
			}
			//2-to-2 Bisimulation for Exact CTMC lumpability
			else //if(reduction.equalsIgnoreCase("EMSB"))
			{
				if(!crnToConsider.hasAtoMostBinaryProducts()){
					CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"The model is not currently supported for EMSB reduction because it contains ternary or more products (i.e. has at least a reaction with more than 2 products) I terminate.",DialogType.Error);
					obtainedPartitionAndBool = new IPartitionAndBoolean(initial.copy(), false);
				}
				else{
					//rr[M,pi]=r[M,pi'] for all (pi,pi')in up(R) and blocks M in up(partition)
					/*labels = MarkovianSpeciesBisimilarity.computeUnaryBinaryLabelsForProducts(crn);
					obtainedPartitionAndBool = MarkovianSpeciesBisimilarity.computeEMSB(crn, labels, initial, verbose,out,bwOut,terminator);*/
					
					//rr[rho,pi]=r[rho,pi'] for all (pi,pi')in up(R) and multisets rho
					/*ICompositePartition trivialCompositePartition = new CompositePartition();
					for (IComposite reagent : crn.getReagents()) {
						ICompositeBlock singleton = new CompositeBlock(reagent);
						trivialCompositePartition.add(singleton);
					}
					labels = MarkovianSpeciesBisimilarity.computeUnaryBinaryLabelsForProducts(crn);
					obtainedPartitionAndBool = MarkovianSpeciesBisimilarity.computeEMSB(crn, labels, initial,trivialCompositePartition, verbose,out,bwOut,terminator);*/
					
					//rr[M,pi]=r[M,pi'] for all (pi,pi')in up(R) and blocks M in up(partition) and
					//rr[rho,pi'']=r[rho',pi''] for all (rho,rho') in up(R) and multiset rho
					/*ICompositePartition trivialCompositePartition = new CompositePartition();
					for (IComposite product : crn.getProducts()) {
						ICompositeBlock singleton = new CompositeBlock(product);
						trivialCompositePartition.add(singleton);
					}
					
					List<ILabel> labelsProducts = MarkovianSpeciesBisimilarity.computeUnaryBinaryLabelsForProducts(crn);
					List<ILabel> labelsReagents = MarkovianSpeciesBisimilarity.computeUnaryBinaryLabels(crn);
					IPartition emsbPart=null;
					IPartition msbPart=null;
					IPartition current = initial;
					do{
						obtainedPartitionAndBool = MarkovianSpeciesBisimilarity.computeEMSB(crn, labelsProducts, current, verbose,out,bwOut,terminator);
						emsbPart = obtainedPartitionAndBool.getPartition(); 
						obtainedPartitionAndBool = MarkovianSpeciesBisimilarity.computeMSB(crn, labelsReagents, emsbPart, trivialCompositePartition, verbose,out,bwOut,terminator);
						msbPart = obtainedPartitionAndBool.getPartition();
						current=msbPart;
					}while(emsbPart.size() != msbPart.size());*/
					
					//rr[M,pi]=r[M,pi'] for all (pi,pi')in up(R) and blocks M in up(partition) and
					//rr[rho,M]=r[rho',M] for all (rho,rho')in up(R) and blocks M in up(partition)
					/*List<ILabel> labelsProducts = MarkovianSpeciesBisimilarity.computeUnaryBinaryLabelsForProducts(crn);
					List<ILabel> labelsReagents = MarkovianSpeciesBisimilarity.computeUnaryBinaryLabels(crn);
					IPartition emsbPart=null;
					IPartition msbPart=null;
					IPartition current = initial;
					do{
						obtainedPartitionAndBool = MarkovianSpeciesBisimilarity.computeEMSB(crn, labelsProducts, current, verbose,out,bwOut,terminator);
						emsbPart = obtainedPartitionAndBool.getPartition(); 
						obtainedPartitionAndBool = MarkovianSpeciesBisimilarity.computeMSB(crn, labelsReagents, emsbPart, verbose,out,bwOut,terminator);
						msbPart = obtainedPartitionAndBool.getPartition();
						current=msbPart;
					}while(emsbPart.size() != msbPart.size());*/
				}
			}
		
			succeeded=obtainedPartitionAndBool.getBool();
			obtainedPartition=obtainedPartitionAndBool.getPartition();
		}
		else if(reduction.equalsIgnoreCase("JUSTBEREDUCTION")){
			obtainedPartition = initial;
		}
		//BB, slower algorithm at time of CONCUR
		else if(reduction.equalsIgnoreCase("EFL")){
			obtainedPartition = ExactFluidBisimilarity.computeEFL(crnToConsider, initial, verbose,out,bwOut,messageDialogShower);
		}
		//BB, TACAS algorithm	
		else if(reduction.equalsIgnoreCase("BB")){
			IPartitionAndBoolean obtainedPartitionAndBool = CRNBisimulations.computeCoarsest(Reduction.BB,crnToConsider, initial, verbose,out,bwOut,print,terminator); 
			obtainedPartition = obtainedPartitionAndBool.getPartition();
		} 
		//N-ary BB. PNAS
		else if(reduction.equalsIgnoreCase("BE")){
			IPartitionAndBoolean obtainedPartitionAndBool = CRNBisimulationsNAry.computeCoarsest(Reduction.BE,crnToConsider,initial, verbose,out,bwOut,terminator,messageDialogShower);
			obtainedPartition = obtainedPartitionAndBool.getPartition();
			succeeded=obtainedPartitionAndBool.getBool();
		}
		else if(reduction.equalsIgnoreCase("ENBB")){
			/*IPartitionAndBoolean obtainedPartitionAndBool = CRNBisimulationsNAry.computeCoarsest(Reduction.ENBB,crnToConsider, initial, verbose,out,bwOut,terminator);
			obtainedPartition = obtainedPartitionAndBool.getPartition();*/
			epsilonDE = new EpsilonDifferentialEquivalences();
			IPartitionAndBoolean obtainedPartitionAndBool = epsilonDE.computeCoarsest(Reduction.ENBB, BigDecimal.valueOf(epsilon),crnToConsider, initial, verbose, out,bwOut,terminator,true);
			obtainedPartition = obtainedPartitionAndBool.getPartition();
		}
		//EFL reduction of POPL
		else if(reduction.equalsIgnoreCase("BDE")){
			SMTExactFluidBisimilarity smtEFL = new SMTExactFluidBisimilarity();
			//SMTExactFluidBisimilarityEpsilon smtEFL = new SMTExactFluidBisimilarityEpsilon();
			PartitionAndString ps = null;
			try {
				ps= smtEFL.computeEFLsmt(crnToConsider, initial, verbose,out,bwOut,print,terminator,messageDialogShower);
				obtainedPartition = ps.getPartition();
				smtTime = ps.getString();
				smtChecksTime=smtEFL.getSMTChecksSecondsAtStep();
			}catch(Z3Exception | ArithmeticException e) {
				//String message="The command "+command +" failed:\n"+e.toString();//+e.getMessage();
				//CRNReducerCommandLine.println(out, bwOut, message);
				//CRNReducerCommandLine.printWarning(out, bwOut, true, messageDialogShower, message, true,DialogType.Error);
				succeeded=false;
				obtainedPartition=partition;
				CRNReducerCommandLine.println(out, bwOut, "The reduction failed.");
			}
			smtEFL=null;
		}
		//OFL reduction of POPL
		else if(reduction.equalsIgnoreCase("FDE")){
			//Previous implementation of OFL reduction of POPL where we were not using the binary characterization, but we were using a counterexample-based apporach similar to the EFL case. It is not guaranteed to give the coarsest reduction (but we didn't find any model for which this happens). 
			//SMTOrdinaryFluidBisimilarity9 smtOFL = new SMTOrdinaryFluidBisimilarity9();
			//obtainedPartition = smtOFL.computeOFLsmt(crn, initial, verbose);			
			SMTOrdinaryFluidBisimilarityBinary smtOFLBinary = new SMTOrdinaryFluidBisimilarityBinary();
			try {
			PartitionAndStringAndBoolean psb = smtOFLBinary.computeOFLsmt(crnToConsider, initial, verbose,out,bwOut,print,terminator); 
			obtainedPartition = psb.getPartition();
			smtTime=psb.getString();
			succeeded = psb.booleanValue();
			smtChecksTime=smtOFLBinary.getSMTChecksSecondsAtStep();
			}catch(Z3Exception e){
				succeeded=false;
				obtainedPartition=partition;
				CRNReducerCommandLine.println(out, bwOut, "The reduction failed.");
			}
			smtOFLBinary=null;
		}
		/*else if(reduction.equalsIgnoreCase("GEFLsmt")){
			SMTGeneralizedExactFluidBisimilarity  smtGEFL = new SMTGeneralizedExactFluidBisimilarity();
			obtainedPartition = smtGEFL.computeGEFLsmt(crn, initial, verbose);
			smtGEFL=null;
			if(obtainedPartition==null){
				return null;
			}
		}*/
		else if(reduction.equalsIgnoreCase("BE_AAt")) {
			begin = System.currentTimeMillis();
			obtainedPartition = NetworkControllability.computeCoarsestBEOnA_and_ATransposed(crnToConsider, initial, verbose, out, bwOut, terminator, messageDialogShower);
		}
		else{
			printWarning(out, bwOut,true,messageDialogShower,"The reduction techinque "+reduction+ " does not exist.",DialogType.Error);
					//+ "" Please use one among NFB, NBB, BB, FB, DSB, SMB, WFB, EFL, BDE, FDE or GEFL.");
			return null;
		}
		long end = System.currentTimeMillis();
		
		if(succeeded && SETREPRESENTATIVEBYMINAFTERPARTITIONREFINEMENT){
			obtainedPartition.setMinAsRepresentative();
		}
		
		if(printPartition){
			CRNReducerCommandLine.println(out,bwOut,obtainedPartition);
		}
		
		if(succeeded && !Terminator.hasToTerminate(terminator)){
			BigDecimal delta = BigDecimal.ZERO;
			if(deltaString!=null) {
				delta=new BigDecimal(deltaString);
			}
			printReductionInfoAndComputeReducedModel(updateCRN, reduction, out, bwOut, reducedFileName,
					partitionInfoFileName, groupedFileName, sameICFileName, csvSMTTimeFileName, typeOfGroupedFile,
					computeOnlyPartition, csvFile, print, sumReductionAlgorithm, writeReducedCRN, writeGroupedCRN,
					writeSameICCRN, crnToConsider, initial, originalCRNShort, obtainedPartition, cp, icWarning,
					reductionName, reducedModelName, smtChecksTime, obtainedPartitionOfParams, begin, smtTime, end,extraColumnsForCSV,delta,reactionToRateInModelBigM);
		}
		else {
			//MessageConsoleStream out, BufferedWriter bwOut, String csvFile, ICRN crn, String reduction, long timeInMS,int initPartitionSize
			writeReductionNotSucceededInfoInCSVFile(out, bwOut, csvFile, crn,reduction,initial.size());
		}
		
		
		if(printPartition||forcePrintingOfPartition){
			CRNReducerCommandLine.println(out,bwOut,"Information about the computed partition:");
			CRNReducerCommandLine.println(out,bwOut,obtainedPartition);
		}
				
		
		return new IPartitionAndBoolean(obtainedPartition, succeeded);
	}
	public static IPartition pepartitionAccordingToUserPartition(MessageConsoleStream out, BufferedWriter bwOut,
			boolean print, ICRN crnToConsider, IPartition initial,
			ArrayList<HashSet<ISpecies>> userDefinedPartition, Terminator terminator) {
			if(print){
				//CRNReducerCommandLine.print(out,bwOut,"Prepartinioning with respect to the views/groups specified in the original file...");
				CRNReducerCommandLine.print(out,bwOut,"User-defined prepartinioning...");
			}
			long begin = System.currentTimeMillis();
			//initial = CRNBisimulationsNAry.prepartitionUserDefined(crn, false, out,bwOut,terminator);
			//initial = CRNBisimulationsNAry.prepartitionUserDefined(crn.getSpecies(), crn.getUserDefinedPartition(), print, out, bwOut, terminator);
			initial = CRNBisimulationsNAry.prepartitionUserDefined(crnToConsider.getSpecies(), userDefinedPartition, false, out, bwOut, terminator);

			long end = System.currentTimeMillis();
			if(print){
				//CRNReducerCommandLine.println(out,bwOut," completed. The "+crn.getSpecies().size()+" species have been prepartitioned in "+initial.size()+" blocks. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");
				CRNReducerCommandLine.println(out,bwOut," completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)"+".\n\tThe "+crnToConsider.getSpecies().size()+" "+((crnToConsider.getMdelDefKind().equals(ODEorNET.ODE))?"variables":"species")+" have been prepartitioned in "+ blockOrBlocks(initial.size()));
			}
		return initial;
	}
	protected void printReductionInfoAndComputeReducedModel(boolean updateCRN, String reduction, MessageConsoleStream out,
			BufferedWriter bwOut, String reducedFileName, String partitionInfoFileName, String groupedFileName,
			String sameICFileName, String csvSMTTimeFileName, String typeOfGroupedFile, String computeOnlyPartition,
			String csvFile, boolean print, boolean sumReductionAlgorithm, boolean writeReducedCRN,
			boolean writeGroupedCRN, boolean writeSameICCRN, ICRN crnToConsider, IPartition initial,
			String originalCRNShort, IPartition obtainedPartition, CRNandPartition cp, String icWarning,
			String reductionName, String reducedModelName, List<Double> smtChecksTime,
			IPartition obtainedPartitionOfParams, long begin, String smtTime, long end, LinkedHashMap<String, String> extraColumnsForCSV,BigDecimal deltaReduced, HashMap<ICRNReaction, BigDecimal> reactionToRateInModelBigM
			//,ICRN crn,boolean fromGUI, Terminator terminator,IMessageDialogShower messageDialogShower			
			)
			throws UnsupportedFormatException, IOException {
		String reducedCRNShort;
		/*
		//This snippet allows us to copy the files on which the reduction succeeded into a new folder
		///Users/andrea/OneDrive - Danmarks Tekniske Universitet/runtimes/runtime-ERODE.product(4)/TCS_CMSB/ERODE models/arbitrary/FE/BIOMD0000000003.ode
		String toRemove = File.separator + "arbitrary"+File.separator+reduction.toUpperCase();
		String toAdd = File.separator + "arbitrary";
		String originalFileName = reducedFileName.replace(toRemove, toAdd);
		toAdd = File.separator + "arbitrary" + File.separator + "polynomial_non_mass_action";
		String whereToCopyFile = reducedFileName.replace(toRemove, toAdd);
		copyFile(originalFileName,whereToCopyFile);
		*/
		CRNandPartition cp_m=null;
		
		int original = crnToConsider.getSpecies().size();
		int reduced = obtainedPartition.size();
		double factor = (reduced)/((double)original);
		if(print){
			double time = (end-begin)/1000.0;
			String timeString = String.format( CRNReducerCommandLine.MSFORMAT, (time) );
			String msg="\n\tCompleted in "+timeString+ " (s).";
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
				//Since there was no reduction, I do not create a reduced model.
				
			}
			else{
				String reductionRatio = String.format( "%.2f", ((factor*100.0)) );
				//CRNReducerCommandLine.println(out,bwOut," completed in "+timeString+ " (s).\n\tFrom "+crn.getSpecies().size()+" species to " +obtainedPartition.size()+" blocks ("+reductionRatio+"% of original size).");
				msg+="\n\tFrom "+crnToConsider.getSpecies().size()+" "+((crnToConsider.getMdelDefKind().equals(ODEorNET.ODE))?"variables":"species")+" to " +blockOrBlocks(obtainedPartition.size())+" ("+reductionRatio+"% of original size).";
			}
			CRNReducerCommandLine.println(out,bwOut,msg);	
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

		writePartitionToFile(reduction, out, bwOut, partitionInfoFileName, print, crnToConsider, obtainedPartition,
				obtainedPartitionOfParams);

		/*HashMap<String, ISpecies> speciesNameToSpecies=new HashMap<String, ISpecies>(crn.getSpecies().size());
		for (ISpecies species : crn.getSpecies()) {
			speciesNameToSpecies.put(species.getName(), species);
		}
		for (ICRNReaction reaction : crn.getReactions()) {
			CRNReactionArbitraryGUI r=(CRNReactionArbitraryGUI)reaction;
			r.replaceSAndBWithMin(speciesNameToSpecies);
		}*/
		

		InfoCRNReduction infoReduction = new InfoCRNReduction(crnToConsider.getName(), crnToConsider.getSpecies().size(), crnToConsider.getReactions().size(), crnToConsider.getParameters().size(),factor, initial==null?-1:initial.size(),obtainedPartition, end-begin, reduction);
		
		/*CRNReducerCommandLine.println(out,bwOut,);
		CRNReducerCommandLine.println(out,bwOut,crn);
		CRNReducerCommandLine.println(out,bwOut,);
		CRNReducerCommandLine.println(out,bwOut,obtainedPartition);
		CRNReducerCommandLine.println(out,bwOut,);*/

		if(writeGroupedCRN){
			SupportedFormats format = (fromGUI)?SupportedFormats.CRNGUI:SupportedFormats.CRN;
			////
			//format = SupportedFormats.BNG;
			////
			if(typeOfGroupedFile.equalsIgnoreCase("FluidCompiler")){
				format = SupportedFormats.FluidCompiler;
			}
			else if(typeOfGroupedFile.equalsIgnoreCase("z3")){
				format = SupportedFormats.z3;
			}
			String question="";
			if(reduction.equalsIgnoreCase("DSB")||reduction.equalsIgnoreCase("FDE")){
				question="OFL";
			}
			else if(reduction.equalsIgnoreCase("EFL")||reduction.equalsIgnoreCase("BDE")){
				question="EFL";
			}
			List<String> furtherParameters = new ArrayList<>(1);
			furtherParameters.add(question);
			writeCRN(groupedFileName,crnToConsider,obtainedPartition,format,true,false,null,"",furtherParameters,out,bwOut,crnToConsider.getMdelDefKind(),false,null,messageDialogShower,false);
		}
		//Even though it does not make much sense to assign same IC when using EFL
		if(writeSameICCRN){
			SupportedFormats format = (fromGUI)?SupportedFormats.CRNGUI:SupportedFormats.CRN;
			writeCRN(sameICFileName,crnToConsider,obtainedPartition,format,false,true,null,"",null,out,bwOut,crnToConsider.getMdelDefKind(),false,null,messageDialogShower,false);
		}
		
		
		/*if((reduction.equalsIgnoreCase("ENBB")||reduction.equalsIgnoreCase("ENFB"))&& epsilonDE!=null){
			epsilonDE.computeReferenceTrajectory(Reduction.valueOf(reduction.toUpperCase()),crn, obtainedPartition,out,true,terminator,true);
		}*/
		
		else if(!computeOnlyPartition.equalsIgnoreCase("true")){
			
			//Now compute the reduced model
			if(print){
				//CRNReducerCommandLine.print(out,bwOut,"Reducing the model with respect to the obtained partition ... ");
				CRNReducerCommandLine.print(out,bwOut,"Creating reduced model... ");
			}
			
			begin = System.currentTimeMillis();
			
			if(reduction.equalsIgnoreCase("DSB")||reduction.equalsIgnoreCase("SMB")||reduction.equalsIgnoreCase("SE")||reduction.equalsIgnoreCase("EMSB")||reduction.equalsIgnoreCase("FDE")||
					reduction.equalsIgnoreCase("FB")||reduction.equalsIgnoreCase("FE")||reduction.equalsIgnoreCase("ENFB")
					|| reduction.equalsIgnoreCase("uctmcfe") || reduction.equalsIgnoreCase("use")
					){
//				BigDecimal delta = BigDecimal.ZERO;
//				if(deltaString!=null) {
//					delta=BigDecimal.valueOf(deltaString);
//				}
				cp=computeReducedCRN_DSBSMB(crnToConsider, fromGUI,terminator,reduction.toLowerCase(),reducedModelName,obtainedPartition,out,bwOut,sumReductionAlgorithm,deltaReduced);
				if(reduction.equalsIgnoreCase("uctmcfe")) {
					if(deltaReduced.compareTo(BigDecimal.ZERO)!=0) {
						cp_m=computeReducedCRN_DSBSMB(crnToConsider, fromGUI,terminator,reduction.toLowerCase(),reducedModelName,obtainedPartition,out,bwOut,sumReductionAlgorithm,BigDecimal.ZERO.subtract(deltaReduced));
					}
					else {
						CRNandPartition cp_M=computeReducedCRN_DSBSMB(crnToConsider, fromGUI,terminator,reduction.toLowerCase(),reducedModelName,obtainedPartition,out,bwOut,sumReductionAlgorithm,BigDecimal.ZERO,reactionToRateInModelBigM);
						cp_m=cp;
						cp=cp_M;
					}
				}
				
				
				//UCTMCLu computeOnlyMeasuresForBound
				//it.imt.erode.partitionrefinement.algorithms.UCTMCLumping.computeOnlyMeasuresForBound(cp.getCRN(), BigDecimal.valueOf(1.0e-4), out, bwOut, terminator);
			}
			else if(reduction.equalsIgnoreCase("EFL") || reduction.equalsIgnoreCase("BDE") || reduction.equalsIgnoreCase("GEFLsmt")||reduction.equalsIgnoreCase("BB")||reduction.equalsIgnoreCase("BE")||reduction.equalsIgnoreCase("ENBB")
					||reduction.equalsIgnoreCase("BE_AAt") || reduction.equalsIgnoreCase("JUSTBEREDUCTION")){
				/*CRNReducerCommandLine.println(out,bwOut,"\n\n");
				CRNReducerCommandLine.println(out,bwOut,crn);
				CRNReducerCommandLine.println(out,bwOut,"\n\n");
				CRNReducerCommandLine.println(out,bwOut,obtainedPartition);
				CRNReducerCommandLine.println(out,bwOut,"");*/
				cp=computeReducedCRN_EFSB(crnToConsider, fromGUI,reducedModelName,obtainedPartition,out,bwOut,sumReductionAlgorithm,terminator);
			}
			end = System.currentTimeMillis();
			if(print){
				CRNReducerCommandLine.println(out,bwOut,"completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+" (s).");
			}
			if(cp!=null){
				infoReduction.setReducedSpecies(cp.getCRN().getSpecies().size());
				infoReduction.setReducedReactions(cp.getCRN().getReactions().size());
			}

			/*CRNReducerCommandLine.println(out,bwOut,);
			CRNReducerCommandLine.println(out,bwOut,cp.getCRN());
			CRNReducerCommandLine.println(out,bwOut,);*/
			
			
			//cp.getCRN().setMoleculeTpyesLoadedFromBNGLFIle(crn.getMoleculeTpyesLoadedFromBNGLFIle());
			//cp.getCRN().setSeedSpeciesLoadedFromBNGLFIle(crn.getSeedSpeciesLoadedFromBNGLFIle());

			//CRNReducerCommandLine.println(out,bwOut,"Reduction of "+crn.getName()+ " completed.");
			reducedCRNShort=reductionName+" reduced model: "+cp.getCRN().toStringShort();
			if(print){
				CRNReducerCommandLine.println(out,bwOut,originalCRNShort);
				//CRNReducerCommandLine.println(out,bwOut," Size of the obtained partition: "+sizeOfObtainedPartition);
				CRNReducerCommandLine.println(out,bwOut,reducedCRNShort);

				if(icWarning!=null && !icWarning.equals("")){
					printWarning(out, bwOut,false,messageDialogShower,icWarning,DialogType.Warning);
				}
			}

			if(writeReducedCRN && !Terminator.hasToTerminate(terminator)){
				/*
				 *  # Automatically generated from <original filename> via <technique>
			# Original number of species:Size
		    # Original number of reactions:
		    # Reduced number of species:
		    # Reduced number of reactions:
		    # Time taken: 
				 */
				SupportedFormats format = (fromGUI)?SupportedFormats.CRNGUI:SupportedFormats.CRN;
				//String commentSymbol =(fromGUI)?"//":"#";
				if(cp_m==null) {
					writeCRN(reducedFileName,cp.getCRN(),cp.getPartition(),format,infoReduction.toCRNComment(),icWarning,null,out,bwOut,false,null);
				}
				else {
					String reducedFileNameM=reducedFileName.replace(".ode", "_bigM.ode");
					reducedFileNameM = reducedFileNameM.replace("._ode", "_bigM._ode");
					writeCRN(reducedFileNameM,cp.getCRN(),cp.getPartition(),format,infoReduction.toCRNComment(),icWarning,null,out,bwOut,false,null);
					
					String reducedFileNamem=reducedFileName.replace(".ode", "_smallm.ode");
					reducedFileNamem = reducedFileNamem.replace("._ode", "_smallm._ode");
					writeCRN(reducedFileNamem,cp_m.getCRN(),cp_m.getPartition(),format,infoReduction.toCRNComment(),icWarning,null,out,bwOut,false,null);
				}
				
			}
			
			

			if(updateCRN && ! Terminator.hasToTerminate(terminator)){
				crnToConsider = cp.getCRN();
				crn=crnToConsider;
				partition=cp.getPartition();
				if(print){
					CRNReducerCommandLine.println(out,bwOut,"The current model is updated with the reduced one.");
				}
			}
		}
		
		writeReductionInfoInCSVFile(out, bwOut, csvFile, infoReduction,extraColumnsForCSV);
	}
	protected static void writePartitionToFile(String reduction, MessageConsoleStream out, BufferedWriter bwOut,
			String partitionInfoFileName, boolean print, ICRN crnToConsider, IPartition obtainedPartition,
			IPartition obtainedPartitionOfParams) {
		if(partitionInfoFileName!=null && !partitionInfoFileName.equals("")){
			if(print){
				CRNReducerCommandLine.print(out,bwOut,"Writing the partition to file "+partitionInfoFileName+" ...");
			}
			CRNImporter.printPartition(crnToConsider, obtainedPartition, partitionInfoFileName, verbose, reduction,out,bwOut,obtainedPartitionOfParams);
			
			if(print){
				CRNReducerCommandLine.println(out,bwOut," completed");
			}
		}
	}
	
	

	
	
	protected static String blockOrBlocks(int numberOfBlocks) {
		if(numberOfBlocks==1){
			return numberOfBlocks + " block";
		}
		else{
			return numberOfBlocks + " blocks";
		}
	}
//	private void writeCRN(String fileName, ICRN crnToWrite, IPartition partitionToWrite, SupportedFormats format, Collection<String> preambleCommentLines, String icComment, List<String> furtherParameters, MessageConsoleStream out, BufferedWriter bwOut,boolean rnEncoding, LinkedHashSet<String> paramsToCurry) throws UnsupportedFormatException{
//		writeCRN(fileName,crnToWrite,partitionToWrite,format,false,false, preambleCommentLines,icComment,furtherParameters,out,bwOut,crnToWrite.getMdelDefKind(),rnEncoding,paramsToCurry,messageDialogShower);
//	}

//	private void writeCRN(String fileName, ICRN crnToWrite, IPartition partitionToWrite, SupportedFormats format, Collection<String> preambleCommentLines, String icComment, MessageConsoleStream out, BufferedWriter bwOut, LinkedHashSet<String> paramsToCurry) throws UnsupportedFormatException{
//		writeCRN(fileName, crnToWrite, partitionToWrite, format, preambleCommentLines, icComment, out,bwOut,false,paramsToCurry);
//	}
	private void writeCRN(String fileName, ICRN crnToWrite, IPartition partitionToWrite, SupportedFormats format, Collection<String> preambleCommentLines, String icComment, List<String> furtherParameters,MessageConsoleStream out, BufferedWriter bwOut,boolean rnEncoding, LinkedHashSet<String> paramsToCurry) throws UnsupportedFormatException{
		ODEorNET crnGUIFormat = crnToWrite.getMdelDefKind();
		//I transformed a DAE system in ODE->RN. I print it as a DAE again
		if(crnGUIFormat.equals(ODEorNET.RN) && crnToWrite.algebraicSpecies()>0) {
			crnGUIFormat=ODEorNET.ODE;
		}
		writeCRN(fileName, crnToWrite, partitionToWrite, format, preambleCommentLines, icComment, furtherParameters,out,bwOut, crnGUIFormat,rnEncoding,paramsToCurry,false,false);
	}
	private void writeCRN(String fileName, ICRN crnToWrite, IPartition partitionToWrite, SupportedFormats format, Collection<String> preambleCommentLines, String icComment, List<String> furtherParameters, MessageConsoleStream out, BufferedWriter bwOut, ODEorNET crnGUIFormat,boolean rnEncoding, LinkedHashSet<String> paramsToCurry, boolean deterministicCorrection, boolean originalNames) throws UnsupportedFormatException{
		writeCRN(fileName, crnToWrite, partitionToWrite, format, preambleCommentLines, icComment, furtherParameters, out, bwOut, crnGUIFormat,rnEncoding, paramsToCurry, deterministicCorrection, originalNames,EULER.NO);
	}
	private void writeCRN(String fileName, ICRN crnToWrite, IPartition partitionToWrite, SupportedFormats format, Collection<String> preambleCommentLines, String icComment, List<String> furtherParameters, MessageConsoleStream out, BufferedWriter bwOut, ODEorNET crnGUIFormat,boolean rnEncoding, LinkedHashSet<String> paramsToCurry, boolean deterministicCorrection, boolean originalNames,EULER euler,boolean resetParameters) throws UnsupportedFormatException{
		writeCRN(fileName,crnToWrite,partitionToWrite,format,false,false, preambleCommentLines,icComment,furtherParameters,out,bwOut,crnGUIFormat,rnEncoding,paramsToCurry,messageDialogShower,deterministicCorrection,originalNames,euler,resetParameters);
	}
	private void writeCRN(String fileName, ICRN crnToWrite, IPartition partitionToWrite, SupportedFormats format, Collection<String> preambleCommentLines, String icComment, List<String> furtherParameters, MessageConsoleStream out, BufferedWriter bwOut, ODEorNET crnGUIFormat,boolean rnEncoding, LinkedHashSet<String> paramsToCurry, boolean deterministicCorrection, boolean originalNames,EULER euler) throws UnsupportedFormatException{
		writeCRN(fileName,crnToWrite,partitionToWrite,format,false,false, preambleCommentLines,icComment,furtherParameters,out,bwOut,crnGUIFormat,rnEncoding,paramsToCurry,messageDialogShower,deterministicCorrection,originalNames,euler,false);
	}

	private static void writeCRN(String fileName, ICRN crnToWrite, IPartition partitionToWrite, SupportedFormats format,boolean groupAccordingToCurrentPartition, boolean assignSameICToBlocks, Collection<String> preambleCommentLines, String icComment, List<String> furtherParameters, MessageConsoleStream out, BufferedWriter bwOut, ODEorNET crnGUIFormat,boolean rnEncoding, LinkedHashSet<String> paramsToCurry, IMessageDialogShower messageDialogShower, boolean originalNames) throws UnsupportedFormatException{
		writeCRN(fileName, crnToWrite, partitionToWrite, format, groupAccordingToCurrentPartition, assignSameICToBlocks, preambleCommentLines, icComment, furtherParameters, out, bwOut, crnGUIFormat,rnEncoding, paramsToCurry, messageDialogShower, false,originalNames,EULER.NO,false);
	}
	
	private static void writeCRN(String fileName, ICRN crnToWrite, IPartition partitionToWrite, SupportedFormats format,
			boolean groupAccordingToCurrentPartition, boolean assignSameICToBlocks, Collection<String> preambleCommentLines,
			String icComment, List<String> furtherParameters, MessageConsoleStream out, BufferedWriter bwOut, ODEorNET crnGUIFormat,
			boolean rnEncoding, LinkedHashSet<String> paramsToCurry, IMessageDialogShower messageDialogShower,
			boolean deterministicCorrection, boolean originalNames,EULER euler,boolean resetParameters) throws UnsupportedFormatException{
		//fileName = fileName.rec place('-', '_').replace(" ", "");
		if(crnToWrite==null){
			//CRNReducerCommandLine.println(out,bwOut,"Before writing a model in a file it is necessary to load it. "); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			//CRNReducerCommandLine.println(out,bwOut,"I skip the write command in file: "+fileName);
			CRNReducerCommandLine.println(out,bwOut,"No model loaded. I skip the write command in file: "+fileName); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
			return;
		}
		if(printCRN){
			CRNReducerCommandLine.println(out,bwOut,"The model to write:");
			CRNReducerCommandLine.println(out,bwOut,crnToWrite.toString());
		}
		
		if(paramsToCurry!=null && paramsToCurry.size()>0) {
			boolean addParams=false;
			CRNandPartition curried = applyCurry(crnToWrite, partitionToWrite, paramsToCurry,false,addParams,true);
			crnToWrite=curried.getCRN();
			partitionToWrite=curried.getPartition();
		}
		
		if(deterministicCorrection) {
			if(!crnToWrite.isMassAction()) {
				CRNReducerCommandLine.println(out,bwOut," failed. Deterministic correction can be applied only to mass action CRNs.");
				return;
			}
			try {
				crnToWrite = CRN.deterministicCorrection(crnToWrite, out, bwOut);
			} catch (IOException e) {
				//throw new UnsupportedFormatException(e.getMessage());
				CRNReducerCommandLine.println(out,bwOut," failed. Problems while applying the deterministic correction: "+e.getMessage());
				return;
			}
		}
		
		if(format.equals(SupportedFormats.LNA)) {
			CRNReducerCommandLine.print(out,bwOut,"Computing the LNA equations ...");
			HashMap<String,ISpecies> speciesNameToExpandedSpecies = new HashMap<>(crnToWrite.getSpecies().size() + crnToWrite.getSpecies().size()*crnToWrite.getSpecies().size());
			ICRN crnLNA;
			String[][] C = new String[crnToWrite.getSpecies().size()][crnToWrite.getSpecies().size()];
			try {
				crnLNA = MatlabODEsImporter.expandLNA(crnToWrite, speciesNameToExpandedSpecies,out,bwOut,messageDialogShower,verbose,C);
			} catch (IOException e) {
				throw new UnsupportedFormatException("Problems in creating the LNA: "+e.getMessage());
			}
			
			IPartition partitionExpanded = expandPartition(crnToWrite, partitionToWrite, speciesNameToExpandedSpecies,crnLNA,C);
			
			boolean[] viewExpressionsUsesCovariances=new boolean[crnToWrite.getSpecies().size()*crnToWrite.getSpecies().size()];
			String[] viewExprs = new String[crnToWrite.getSpecies().size()*crnToWrite.getSpecies().size()];
			String[] viewNames = new String[crnToWrite.getSpecies().size()*crnToWrite.getSpecies().size()];
			String[] viewExprsSupportedByMathEval = new String[crnToWrite.getSpecies().size()*crnToWrite.getSpecies().size()];
			int p=0;
			for(int i=0;i<crnToWrite.getSpecies().size();i++) {
				for(int j=0;j<crnToWrite.getSpecies().size();j++) {
					viewExpressionsUsesCovariances[p]=false;
					//String name="C_"+i+"_"+j;
					String name=C[i][j];
					viewExprs[p]=name;
					viewNames[p]="v"+name;
					viewExprsSupportedByMathEval[p]=speciesNameToExpandedSpecies.get(name).getNameAlphanumeric();
					p++;
				}
			}
			crnLNA.setViews(viewNames, viewExprs, viewExprsSupportedByMathEval, viewExpressionsUsesCovariances);
			
			format = SupportedFormats.CRNGUI;
			crnGUIFormat = ODEorNET.ODE;
			crnToWrite = crnLNA;
			partitionToWrite = partitionExpanded;
			CRNReducerCommandLine.println(out,bwOut," completed");
		}
		
		CRNReducerCommandLine.print(out,bwOut,"Writing to file "+ fileName+" ...");
		if(format.equals(SupportedFormats.BNG)){
			//BioNetGenImporter.printCRNToNetFile(crn, partition, HowToModifyModelWhenWritinInBNGFile.GROUPBLOCKS,name);
			BioNetGenImporter.printCRNToNetFile(crnToWrite, partitionToWrite,fileName,assignSameICToBlocks,groupAccordingToCurrentPartition,false,out,bwOut);
		}
		else if(format.equals(SupportedFormats.StoichiometryMatrix)){
			//BioNetGenImporter.printCRNToNetFile(crn, partition, HowToModifyModelWhenWritinInBNGFile.GROUPBLOCKS,name);
			StoichiometricMatrixExporter.printCRNToStoichiometricMatrixCSVFile(crnToWrite, fileName, verbose, out, bwOut);
		}
		else if(format.equals(SupportedFormats.CERENA)){
			//BioNetGenImporter.printCRNToNetFile(crn, partition, HowToModifyModelWhenWritinInBNGFile.GROUPBLOCKS,name);
			MatlabODEsImporter.printToCERENAFIle(crnToWrite, fileName, preambleCommentLines, verbose, icComment, out,bwOut, furtherParameters.get(0),(furtherParameters==null||furtherParameters.size()==1)?"false":furtherParameters.get(1),messageDialogShower);
		}
//		else if(format.equals(SupportedFormats.LNA)){
//			MatlabODEsImporter.printLNA(crnToWrite, fileName, preambleCommentLines, verbose, icComment, out,bwOut,messageDialogShower);
//		}
		else if(format.equals(SupportedFormats.Modelica)){
			//BioNetGenImporter.printCRNToNetFile(crn, partition, HowToModifyModelWhenWritinInBNGFile.GROUPBLOCKS,name);
			//How many algebraic variables do I have?
			/*crnToWrite.algebraicSpecies();
			//Which ones are algebraic?
			for(ISpecies species : crnToWrite.getSpecies()) {
				if(species.isAlgebraic()) {
					
				}
				else {
					
				}
			}*/
			
			//Copy from: it.imt.erode.importing.GUICRNImporter (in particular printToERODEFIle)
			
			boolean exportICOfAlgebraic = true;
			if(furtherParameters!=null && furtherParameters.size()>0) {
				String exportIC = furtherParameters.get(0);
				if(exportIC.equalsIgnoreCase("false")) {
					exportICOfAlgebraic=false;
				}
			}
			ModelicaImporter.printDAEToModelicaFile(crnToWrite, fileName,exportICOfAlgebraic,out,bwOut);
		}
		else if(format.equals(SupportedFormats.Affine)){
			CompactCSVMatrixImporter.printToCSVFIle(crnToWrite, fileName, preambleCommentLines, rnEncoding, out, bwOut);
		}
		else if(format.equals(SupportedFormats.CRN)){
			boolean writeSpecies=true;
			CRNImporter.printCRNToCRNFile(crnToWrite, partitionToWrite, fileName, assignSameICToBlocks, groupAccordingToCurrentPartition,preambleCommentLines,false,icComment,out,bwOut,writeSpecies);
		}
		else if(format.equals(SupportedFormats.CRNGUI)){
			GUICRNImporter.printToERODEFIle(crnToWrite, partitionToWrite, fileName, assignSameICToBlocks, groupAccordingToCurrentPartition,preambleCommentLines,false,icComment,out,bwOut,crnGUIFormat,rnEncoding,originalNames,euler,resetParameters);
		}
		else if(format.equals(SupportedFormats.FluidCompiler)){
			FluidCompilerImporter.printCRNToFluidCompilerFile(crnToWrite, fileName, partitionToWrite, false,out,bwOut);			
		}
		else if(format.equals(SupportedFormats.z3)){
			//z3Importer.printCRNToz3File(crnToWrite, fileName, partitionToWrite, false,(furtherParameters==null||furtherParameters.size()==0)?null:furtherParameters.get(0),out,bwOut);
			z3Importer.printCRNToz3FileODEsASCONSTANTS(crnToWrite, fileName, partitionToWrite, false,(furtherParameters==null||furtherParameters.size()==0)?null:furtherParameters.get(0),out,bwOut);
			/*try {
				z3Importer.sendToToz3ODEsASCONSTANTS(crnToWrite, fileName, partitionToWrite, false, furtherParameter);
			} catch (Z3Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
		}
		else if(format.equals(SupportedFormats.LBS)){
			LBSImporter.printCRNToLBSFile(crnToWrite, partitionToWrite, fileName, assignSameICToBlocks, groupAccordingToCurrentPartition,preambleCommentLines,false,icComment,out,bwOut,messageDialogShower);
		}
		else if(format.equals(SupportedFormats.SBML)){
			try {
				SBMLImporter.printCRNToSBMLFile(crnToWrite, fileName,preambleCommentLines,false,out,bwOut);
			} catch (CompilationException e) {
				CRNReducerCommandLine.println(out,bwOut," failed. Problems while encoding the expressions of the views in MathML format.");
				return;
			}
		}
		else if(format.equals(SupportedFormats.StochKit)){
			StochKitExporter.printCRNToStochKitXMLFile(crnToWrite, fileName,preambleCommentLines,false,out,bwOut);
		}
		else if(format.equals(SupportedFormats.FlyFast)){
			FlyFastImporter.printCRNToFlyFastFile(crnToWrite, fileName, false,out,bwOut);
		}
		else if(format.equals(SupportedFormats.MatlabArbitraryODEs)){
			MatlabODEsImporter.printODEsToMatlabFIle(crnToWrite, fileName, preambleCommentLines, verbose, icComment, out,bwOut, 
					furtherParameters.get(0),
					(furtherParameters==null||furtherParameters.size()==1)?"false":furtherParameters.get(1),
					furtherParameters.get(2),
					messageDialogShower,
					false);
		}
		else if(format.equals(SupportedFormats.MatlabJacobianFunction)){
			MatlabODEsImporter.printJacobianFunctionToMatlabFIle(crnToWrite, fileName, preambleCommentLines, verbose, icComment, out,bwOut,messageDialogShower);
		}
		/*else if(format.equals(SupportedFormats.MatlabJacobianFunctionEpsCLump)){
			double eps = Double.valueOf(furtherParameters.get(0));
			MatlabODEsImporter.printJacobianFunctionToMatlabFIle(crnToWrite, fileName, preambleCommentLines, verbose, icComment, out,bwOut,messageDialogShower,true,eps);
		}*/
		/*else if(format.equals(SupportedFormats.MatlabEpsilonBounds)){
			MatlabODEsImporter.printJacobianFunctionToMatlabFIle(crnToWrite, fileName, preambleCommentLines, verbose, icComment, out,messageDialogShower);
		}*/
		CRNReducerCommandLine.println(out,bwOut," completed");
	}
	public static CRNandPartition applyCurry(ICRN crnToConsider, IPartition partitionToConsider, LinkedHashSet<String> paramsToCurry,boolean oneBlockPerParam,boolean addParams,boolean preserveUserPartition)
			throws UnsupportedFormatException {
		return applyCurry(crnToConsider, partitionToConsider, paramsToCurry,oneBlockPerParam,addParams,preserveUserPartition,false);
	}
	public static CRNandPartition applyCurry(ICRN crnToConsider, IPartition partitionToConsider, LinkedHashSet<String> paramsToCurry,boolean oneBlockPerParam,boolean addParams,boolean preserveUserPartition,
			boolean reuseSpecies)
			throws UnsupportedFormatException {
		MatlabODEsImporter.parseParametersToPerturb(crnToConsider, paramsToCurry);
		HashMap<String,ISpecies> speciesNameToExpandedSpecies = new HashMap<>(crnToConsider.getSpecies().size());
		ICRN crnCurried;
		crnCurried = MatlabODEsImporter.expandCRN(addParams,crnToConsider, paramsToCurry, speciesNameToExpandedSpecies/*,false*/,reuseSpecies);
		
		IPartition partitionCurried = handlePartitionsAfterCurrying(crnToConsider, partitionToConsider, paramsToCurry,
				oneBlockPerParam, preserveUserPartition, speciesNameToExpandedSpecies, crnCurried);
		
		
//		crnToWrite=crnCurried;
//		partitionToWrite=partitionCurried;
		return new CRNandPartition(crnCurried, partitionCurried);
	}
	public static IPartition handlePartitionsAfterCurrying(ICRN crnToConsider, IPartition partitionToConsider,
			LinkedHashSet<String> paramsToCurry, boolean oneBlockPerParam, boolean preserveUserPartition,
			HashMap<String, ISpecies> speciesNameToExpandedSpecies, ICRN crnCurried) {
		IPartition partitionCurried = new Partition(crnCurried.getSpecies().size());
		IBlock currentBlock = partitionToConsider.getFirstBlock();
		while(currentBlock!=null) {
			IBlock curriedBlock = new Block();
			partitionCurried.add(curriedBlock);
			for (ISpecies species : currentBlock.getSpecies()) {
				ISpecies curriedSpecies = speciesNameToExpandedSpecies.get(species.getName());
				curriedBlock.addSpecies(curriedSpecies);
			}
			currentBlock=currentBlock.getNext();
		}
		
		if(oneBlockPerParam) {
			for(String curriedParam :paramsToCurry) {
				IBlock curriedBlock = new Block();
				partitionCurried.add(curriedBlock);
				ISpecies curriedSpecies = speciesNameToExpandedSpecies.get(curriedParam);
				curriedBlock.addSpecies(curriedSpecies);
			}
		}
		else {
			IBlock curriedBlock = new Block();
			partitionCurried.add(curriedBlock);
			for(String curriedParam :paramsToCurry) {
				ISpecies curriedSpecies = speciesNameToExpandedSpecies.get(curriedParam);
				curriedBlock.addSpecies(curriedSpecies);
			}
		}
		
		ArrayList<HashSet<ISpecies>> userPartition = crnToConsider.getUserDefinedPartition();
		ArrayList<HashSet<ISpecies>> userPartitionCurried=new ArrayList<>();
		if(userPartition!=null && preserveUserPartition) {
			for (HashSet<ISpecies> block : userPartition) {
				HashSet<ISpecies> blockCurried=new HashSet<>(block.size());
				for (ISpecies iSpecies : block) {
					blockCurried.add(speciesNameToExpandedSpecies.get(iSpecies.getName()));
				}
				if(!blockCurried.isEmpty()) {
					userPartitionCurried.add(blockCurried);
				}
			}
		}
		if(paramsToCurry.size()>0) {
			if(oneBlockPerParam) {
			for(String curriedParam :paramsToCurry) {
				HashSet<ISpecies> blockCurried=new HashSet<>(1);
				ISpecies curriedSpecies = speciesNameToExpandedSpecies.get(curriedParam);
				blockCurried.add(curriedSpecies);
				userPartitionCurried.add(blockCurried);
			}
			}
			else {
				HashSet<ISpecies> blockCurried=new HashSet<>(paramsToCurry.size());
				userPartitionCurried.add(blockCurried);
				for(String curriedParam :paramsToCurry) {
					ISpecies curriedSpecies = speciesNameToExpandedSpecies.get(curriedParam);
					blockCurried.add(curriedSpecies);
				}
			}
		}
		crnCurried.setUserDefinedPartition(userPartitionCurried);
		return partitionCurried;
	}
	private static IPartition expandPartition(ICRN crnToWrite, IPartition partitionToWrite,
			HashMap<String, ISpecies> speciesNameToExpandedSpecies, ICRN crnLNA, String [][] C) {
		IPartition partitionExpanded = new Partition(crnLNA.getSpecies().size());

		IBlock currentBlock = partitionToWrite.getFirstBlock();
		while(currentBlock!=null) {
			IBlock curriedBlock = new Block();
			partitionExpanded.add(curriedBlock);
			for (ISpecies species : currentBlock.getSpecies()) {
				ISpecies curriedSpecies = speciesNameToExpandedSpecies.get(species.getName());
				curriedBlock.addSpecies(curriedSpecies);
			}
			currentBlock=currentBlock.getNext();
		}
		IBlock covarBlock = new Block();
		partitionExpanded.add(covarBlock);
		for(int i=0;i<crnToWrite.getSpecies().size();i++){
			for(int j=0;j<crnToWrite.getSpecies().size();j++){
				//String name = "C_"+i+"_"+j;
				String name = C[i][j];
				ISpecies covarij = speciesNameToExpandedSpecies.get(name);
				covarBlock.addSpecies(covarij);
			}
		}

		ArrayList<HashSet<ISpecies>> userPartition = crnToWrite.getUserDefinedPartition();
		ArrayList<HashSet<ISpecies>> userPartitionCurried=new ArrayList<>();
		if(userPartition!=null) {
			for (HashSet<ISpecies> block : userPartitionCurried) {
				HashSet<ISpecies> blockCurried=new HashSet<>(block.size());
				for (ISpecies iSpecies : block) {
					blockCurried.add(speciesNameToExpandedSpecies.get(iSpecies.getName()));
				}
			}
		}
		HashSet<ISpecies> blockCovar=new HashSet<>(crnToWrite.getSpecies().size()*crnToWrite.getSpecies().size());
		userPartitionCurried.add(blockCovar);
		for(int i=0;i<crnToWrite.getSpecies().size();i++){
			for(int j=0;j<crnToWrite.getSpecies().size();j++){
				//String name = "C_"+i+"_"+j;
				String name = C[i][j];
				ISpecies covarij = speciesNameToExpandedSpecies.get(name);
				blockCovar.add(covarij);
			}
		}
		crnLNA.setUserDefinedPartition(userPartitionCurried);
		return partitionExpanded;
	}
	
	/*private void printGroupedCRN(String red){
		String name = crn.getName();
		int pos = name.lastIndexOf(File.separator);
		String path = name.substring(0, (pos>0)? pos:0);
		String relativeName = name.substring(pos+1);
		name = path+File.separator+red+"Grouped"+relativeName;

		if(printCRN){
			CRNReducerCommandLine.println(out,bwOut,"The grouped model:");
			CRNReducerCommandLine.println(out,bwOut,crn.toString());
		}
		if(writeGroupedCRNInFile){
			if(relativeName.endsWith(".net")){
				//BioNetGenImporter.printCRNToNetFile(crn, partition, HowToModifyModelWhenWritinInBNGFile.GROUPBLOCKS,name);
				BioNetGenImporter.printCRNToNetFile(crn, partition,name,false,true);
			}
			else if(relativeName.endsWith(".crn")){
				//TODO CRNImporter.printCRNToCRNFile(crn, name, false,true);
			}
		}
	}*/

	private static CRNandPartition computeReducedCRN_DSBSMB(ICRN crn, boolean fromGUI,Terminator terminator, String red, String nameOfCRN, IPartition obtainedPartition, MessageConsoleStream out, BufferedWriter bwOut,boolean newReductionAlgorithm,BigDecimal delta) throws IOException {
		return computeReducedCRN_DSBSMB(crn, fromGUI,terminator, red, nameOfCRN, obtainedPartition, out, bwOut,newReductionAlgorithm,delta,null);
	}
	private static CRNandPartition computeReducedCRN_DSBSMB(ICRN crn, boolean fromGUI,Terminator terminator, String red, String nameOfCRN, IPartition obtainedPartition, MessageConsoleStream out, BufferedWriter bwOut,boolean newReductionAlgorithm,
			BigDecimal delta,HashMap<ICRNReaction, BigDecimal> reactionToRateInModelBigM) throws IOException {
		
		/*String name = nameOfCRN;
		if(name==null || name.equals("")){
			name = crn.getName();
			int pos = name.lastIndexOf(File.separator);
			String path = name.substring(0, (pos>0)? pos:0);
			String relativeName = name.substring(pos+1);
			name = path+File.separator+red+"Reduced"+relativeName;
		}*/

		String commentSymbol =(fromGUI)?"//":"#";
		CRNandPartition cp;
		if(crn.isMassAction()){
			if(newReductionAlgorithm){
				cp = CRNBisimulationsNAry.computeReducedCRNQuotient(crn,nameOfCRN, obtainedPartition, crn.getSymbolicParameters(),crn.getConstraints(),crn.getParameters(),commentSymbol,out,bwOut,terminator);
			}
			else{
				cp = CRNBisimulationsNAry.computeReducedCRNOrdinary(crn,nameOfCRN, obtainedPartition, crn.getSymbolicParameters(),crn.getConstraints(),crn.getParameters(),commentSymbol,out,bwOut,terminator,delta,reactionToRateInModelBigM);
			}
		}
		else{
			cp = SMTOrdinaryFluidBisimilarityBinary.computeReducedCRNOrdinaryNonMassAction(crn,nameOfCRN, obtainedPartition, crn.getSymbolicParameters(),crn.getConstraints(),crn.getParameters(),commentSymbol,out,bwOut,terminator);
		}
		
		if(cp!=null&&cp.getCRN()!=null){
			cp.getCRN().setMdelDefKind(crn.getMdelDefKind());
		}
		
				
		if(printCRN){
			CRNReducerCommandLine.println(out,bwOut,"The reduced model:");
			CRNReducerCommandLine.println(out,bwOut,cp.getCRN().toString());
		}
		/*if(writeReducedCRNInFile){
			if(relativeName.endsWith(".net")){
				//BioNetGenImporter.printCRNToNetFile(crn, partition, HowToModifyModelWhenWritinInBNGFile.GROUPBLOCKS,name);
				BioNetGenImporter.printCRNToNetFile(reducedCRN, trivialPartitionOfReducedCRN,name,false,false);
			}
			else if(relativeName.endsWith(".crn")){
				CRNImporter.printCRNToCRNFile(reducedCRN, name);
			}
		}*/
		//crn = reducedCRN;
		//partition=trivialPartitionOfReducedCRN;
		return cp;
	}

	private static CRNandPartition computeReducedCRN_EFSB(ICRN crn, boolean fromGUI,String nameOfCRN, IPartition obtainedPartition, MessageConsoleStream out, BufferedWriter bwOut, boolean newReductionAlgorithm,Terminator terminator) throws IOException{
		/*String name = nameOfCRN;
		if(name==null || name.equals("")){
			name = crn.getName();
			int pos = name.lastIndexOf(File.separator);
			String path = name.substring(0, (pos>0)? pos:0);
			String relativeName = name.substring(pos+1);
			name = path+File.separator+"efsbReduced"+relativeName;
		}*/

		String commentSymbol =(fromGUI)?"//":"#";
		CRNandPartition cp;
		if(crn.isMassAction()){
			if(newReductionAlgorithm){
				cp = CRNBisimulationsNAry.computeReducedCRNQuotient(crn,nameOfCRN, obtainedPartition, crn.getSymbolicParameters(),crn.getConstraints(),crn.getParameters(),commentSymbol,out,bwOut,terminator);
			}
			else{
				cp = ExactFluidBisimilarity.computeReducedCRNEFL(crn,nameOfCRN, obtainedPartition, crn.getSymbolicParameters(),crn.getConstraints(),crn.getParameters(),commentSymbol,out,bwOut,terminator);
			}
			
		}
		else{
			cp = SMTExactFluidBisimilarity.computeReducedCRNEFLNONMassAction(crn,nameOfCRN, obtainedPartition, crn.getSymbolicParameters(),crn.getConstraints(),crn.getParameters(),commentSymbol,out,bwOut,terminator);
		}

		if(cp!=null&&cp.getCRN()!=null){
			cp.getCRN().setMdelDefKind(crn.getMdelDefKind());
		}	

		if(printCRN){
			CRNReducerCommandLine.println(out,bwOut,"The reduced model:");
			CRNReducerCommandLine.println(out,bwOut,cp.getCRN().toString());
		}
		return cp;
	}

	
	private boolean loadFile(String fileName, SupportedFormats format, boolean print,MessageConsoleStream out, BufferedWriter bwOut) throws UnsupportedFormatException, IOException, JDOMException, XMLStreamException {
		return loadFile(fileName, format, print,out,bwOut,false,false);
	}
	
	private boolean loadFile(String fileName, SupportedFormats format, boolean print,MessageConsoleStream out, BufferedWriter bwOut,boolean compactBNGNames,boolean writeFileWithSpeciesNameCorrespondences) throws UnsupportedFormatException, IOException, JDOMException, XMLStreamException {
		return loadFile(fileName, format, print,false,null,out,bwOut,compactBNGNames,writeFileWithSpeciesNameCorrespondences);
	}
	private boolean loadFile(String fileName, SupportedFormats format, boolean print,boolean ignoreCommands,MessageConsoleStream out, BufferedWriter bwOut,boolean compactBNGNames) throws UnsupportedFormatException, IOException, JDOMException, XMLStreamException {
		return loadFile(fileName, format, print,ignoreCommands,null,out,bwOut,compactBNGNames,false);
	}
	private boolean loadFile(String fileName, SupportedFormats format, boolean print,String[] optionalParameters,MessageConsoleStream out, BufferedWriter bwOut,boolean compactBNGNames,boolean writeFileWithSpeciesNameCorrespondences) throws UnsupportedFormatException, IOException, JDOMException, XMLStreamException {
		return loadFile(fileName, format, print,false,optionalParameters,out,bwOut,compactBNGNames,writeFileWithSpeciesNameCorrespondences);
	}
	private boolean loadFile(String fileName, SupportedFormats format, boolean print, boolean ignoreCommands, String[] optionalParameters,MessageConsoleStream out, BufferedWriter bwOut,boolean compactBNGNames,boolean writeFileWithSpeciesNameCorrespondences) throws UnsupportedFormatException, IOException, JDOMException, XMLStreamException {
		AbstractImporter importer = importerOfSupportedNetworks.importSupportedNetwork(fileName, printInfo, printCRN,format,print,optionalParameters,out,bwOut,messageDialogShower, compactBNGNames,writeFileWithSpeciesNameCorrespondences);
		crn = importer.getCRN();
		partition = importer.getInitialPartition();
		if(!ignoreCommands){
			commandsReader.addToHead(importer.getFurtherCommands());
		}
		return importer.getInfoImporting().getLoadingCRNFailed();
	}

	public static String[] getParameters(String command){
		if(command.indexOf("{")==-1){
			if((command.indexOf('(')==-1 && command.indexOf(')')==-1)||  command.indexOf('(') == command.indexOf(')')-1){
				return new String[0];
			}
			else{
				return null;
			}
		}
		if(command.indexOf("{")!=-1 && command.indexOf("}")==-1){
			return null;
		}
		String[] parameters = command.substring(command.indexOf("{")+1,command.indexOf("}")).split(",");
		for(int i=0;i<parameters.length;i++){
			parameters[i]=parameters[i].trim();
			if(parameters[i].startsWith("monomer")){
				parameters[i]=parameters[i].replace(";", ",");
			}
		}
		return parameters;
	}

	public static void printMan(boolean fromGUI, MessageConsoleStream out, BufferedWriter bwOut) {
		CRNReducerCommandLine.println(out,bwOut,"Manual:\n");
		if(fromGUI){
			CRNImporter.printSampleCommands(out,bwOut);
		}
		else{
			GUICRNImporter.printSampleCommands(out,bwOut);
		}
	}

	public static void usageShort(boolean fromGUI, MessageConsoleStream out, BufferedWriter bwOut) {
		CRNReducerCommandLine.println(out,bwOut,"Usage:\n");
		if(fromGUI){
			CRNImporter.printSampleCommandsShort(out,bwOut);
		}
		else{
			GUICRNImporter.printSampleCommandsShort(out,bwOut);
		}
	}
	protected void setCRN(ICRN crn) {
		this.crn=crn;
		this.partition=new Partition(crn.getSpecies().size());
		IBlock trivialBlock = new Block();
		partition.add(trivialBlock);
		for(ISpecies sp: crn.getSpecies()) {
			trivialBlock.addSpecies(sp);
		}
		
	}

	/*private boolean hasToTerminate(){
		if(terminator==null){
			return false;
		}
		else{
			return terminator.hasToTerminate();
		}
	}*/

}
