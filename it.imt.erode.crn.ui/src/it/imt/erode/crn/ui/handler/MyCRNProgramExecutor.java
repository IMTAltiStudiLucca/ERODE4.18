package it.imt.erode.crn.ui.handler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
//import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.EList;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsoleStream;
/*import org.eclipse.jface.text.BadLocationException;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.ui.editor.XtextEditor;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;
import org.eclipse.xtext.ui.editor.utils.EditorUtils;
import org.eclipse.xtext.util.ITextRegion;*/

import it.imt.erode.commandline.BooleanNetworkCommandLine;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.CommandsReader;
import it.imt.erode.commandline.ICommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.crn.ModelDefKind;
import it.imt.erode.crn.ModelElementsCollector;
import it.imt.erode.crn.MyParserUtil;
import it.imt.erode.crn.chemicalReactionNetwork.BoolExpr;
import it.imt.erode.crn.chemicalReactionNetwork.ModelDefinition;
import it.imt.erode.crn.chemicalReactionNetwork.Node;
import it.imt.erode.crn.chemicalReactionNetwork.NodeDefinition;
import it.imt.erode.crn.chemicalReactionNetwork.SymbolicParameter;
import it.imt.erode.booleannetwork.updatefunctions.BooleanUpdateFunctionExpr;
import it.imt.erode.booleannetwork.updatefunctions.FalseUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.NotBooleanUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.ReferenceToNodeUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.TrueUpdateFunction;
import it.imt.erode.crn.symbolic.constraints.BasicConstraint;
import it.imt.erode.crn.symbolic.constraints.BasicConstraintComparator;
import it.imt.erode.crn.symbolic.constraints.BooleanConnector;
import it.imt.erode.crn.symbolic.constraints.BooleanConstraintExpr;
import it.imt.erode.crn.symbolic.constraints.FalseConstraint;
import it.imt.erode.crn.symbolic.constraints.IConstraint;
import it.imt.erode.crn.symbolic.constraints.NotConstraintExpr;
import it.imt.erode.crn.symbolic.constraints.TrueConstraint;
import it.imt.erode.crn.ui.perspective.console.MyConsoleUtil;
import it.imt.erode.crn.ui.perspective.console.MyMessageConsole;
import it.imt.erode.crn.ui.perspective.dialogs.MessageDialogShower;
import it.imt.erode.crn.ui.perspective.plot.GUIDataOutputHandler;
import it.imt.erode.importing.AbstractImporter;
import it.imt.erode.importing.GUICRNImporter;
import it.imt.erode.importing.ODEorNET;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.importing.booleannetwork.GUIBooleanNetworkImporter;
import it.imt.erode.simulation.output.IDataOutputHandler;

/**
 * 
 * @author Andrea Vandin
 *
 */
public class MyCRNProgramExecutor {

	//
	public void readAndExecuteMultiThreaded(ModelDefinition modelDef, boolean canSynchEditor, IProject project, IPath erodeFileParentLocationRelativeToWS, IPath erodeFileLocationRelativeToWS, ThreadBuffer<MyCRNProgamExecutorWorker> threadsBuffer) {
		readAndExecute(modelDef,canSynchEditor,true,project,erodeFileParentLocationRelativeToWS,erodeFileLocationRelativeToWS,threadsBuffer);
	}
	public void readAndExecute(ModelDefinition modelDef, boolean canSynchEditor, IProject project, IPath erodeFileParentLocationRelativeToWS,IPath erodeFileLocationRelativeToWS) {
		readAndExecute(modelDef,canSynchEditor,false,project,erodeFileParentLocationRelativeToWS,erodeFileLocationRelativeToWS,null);
	}
	
	private void readAndExecute(ModelDefinition modelDef, boolean canSynchEditor,boolean multithreaded, IProject project,IPath erodeFileParentLocationRelativeToWS,IPath erodeFileLocationRelativeToWS,  ThreadBuffer<MyCRNProgamExecutorWorker> threadsBuffer) {
		MyMessageConsole console = MyConsoleUtil.generateConsole(modelDef.getName());
		MessageConsoleStream consoleOut = console.newMessageStream();
		String welcome =MyConsoleUtil.computeWelcome(consoleOut);
		//consoleOut.println(welcome);
		//CRNReducerCommandLine.println(consoleOut,MyConsoleUtil.computeWelcome(consoleOut));
		
		String actualPathOfProject = project.getLocation().toOSString();
		String workspacePath = actualPathOfProject.substring(0,actualPathOfProject.lastIndexOf(File.separator));
	     String parentPath = erodeFileParentLocationRelativeToWS.toFile().getPath();
	     String absoluteParentPath = workspacePath+parentPath;
	     String erodeFilePath = erodeFileLocationRelativeToWS.toFile().getPath();
	     String erodeFileAbsolutePath = workspacePath+erodeFilePath;
	     
	     //String fileName = MyParserUtil.computeFileName(console.getName().replace('/', '-'), absoluteParentPath).replace(' ', '_')+".txt";
	     String fileName = MyParserUtil.computeFileName(console.getName().replace('/', '-').replace(' ', '_'), absoluteParentPath)+".txt";
	     //fileName = MyParserUtil.computeFileName("pippo[-_/]", absoluteParentPath).replace(' ', '_')+".txt";
	     BufferedWriter bwOut=null;
	     if(modelDef.isLog() || CRNReducerCommandLine.FORCELOG){
	    	 AbstractImporter.createParentDirectories(fileName);
	    	 try {
	    		 bwOut = new BufferedWriter(new FileWriter(fileName));
//	    		 bwOut.write(welcome+"\n");
//	    		 bwOut.flush();
	    	 } catch (IOException e1) {
	    		 e1.printStackTrace();
	    		 CRNReducerCommandLine.printStackTrace(consoleOut,bwOut,e1);
	    	 }
	     }
	     
	     CRNReducerCommandLine.println(consoleOut, bwOut, welcome);
	     
	     /*CRNReducerCommandLine.println(consoleOut, "workspacePath="+workspacePath);
	     CRNReducerCommandLine.println(consoleOut, "oldParentPath="+oldParentPath);
	     CRNReducerCommandLine.println(consoleOut, "parentPath="+parentPath);
	     CRNReducerCommandLine.println(consoleOut, "absoluteParentPath="+absoluteParentPath);*/
	     
	    /*workspacePath=/C:/Program Files (x86)/ERODE/workspace
	    parentPath=C:\am
	    absoluteParentPath=/C:/Program Files (x86)/ERODE/workspaceC:\am*/
	     
		/*PlotView plotView = null;
		try {
			IViewPart v = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("it.imt.erode.crn.ui.perspective.PlotView",console.getName(),org.eclipse.ui.IWorkbenchPage.VIEW_ACTIVATE);
			plotView = (PlotView)v;
		} catch (PartInitException e2) {
			e2.printStackTrace();
		}*/
		
	    IMessageDialogShower msgVisualizer = new MessageDialogShower(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
	     
		GUIDataOutputHandler guidog = new GUIDataOutputHandler(console,msgVisualizer,consoleOut,bwOut);

		ModelElementsCollector mec = MyParserUtil.getModelElements(modelDef,/*projectPath*/absoluteParentPath,erodeFileAbsolutePath);
		List<String> commands = MyParserUtil.parseCommands(mec,/*projectPath*/absoluteParentPath);
		
		List<IConstraint> constraints = parseConstraints(mec.getConstraintsListXTEXT(),consoleOut,bwOut);
		LinkedHashMap<String, IUpdateFunction> booleanUpdateFunctions = parseBooleanUpdateFunctions(mec.getBooleanUpdateFunctionsXTEXT(), consoleOut, bwOut);
				
				/*load constraints
				parse constraints as in qflan. steal classes, so that we reuse the parsing to z3
				check constraints in validation and other places
				else if(element instanceof ConstraintsOnSymbolicParametersList){
						ArrayList<ArrayList<String>> constraints = new ArrayList<ArrayList<String>>(0);
						EList<BoolExpr> constraintsList = ((ConstraintsOnSymbolicParametersList) element).getConstraints();
						for (BoolExpr constraint : constraintsList) {
							IConst
							visitConstraint()
						}
					}
				
				*/

		CommandsReader commandsReader = new CommandsReader(commands,consoleOut,bwOut);
		//CRNReducerCommandLine cl;
		boolean failed=false;
		
		/*CRNReducerCommandLine cl = loadModel(mec, msgVisualizer, consoleOut, constraints, commandsReader, guidog, console);
		if(cl==null){
			failed=true;
		}*/
		
//		//I have to launch the new thread starting from here
//		if(mec.getModelDefKind().equals(ModelDefKind.RN)||mec.getModelDefKind().equals(ModelDefKind.ODE)){
//			GUICRNImporter crnImporter = new GUICRNImporter(consoleOut,msgVisualizer);
//			try {
//				boolean printCRN=false;
//				ODEorNET format = mec.getModelDefKind().equals(ModelDefKind.RN)? ODEorNET.RN : ODEorNET.ODE;
//				crnImporter.importCRNNetwork(true, printCRN, true, mec.getModelName(), mec.getSymbolicParameters(),constraints,mec.getParameters(), mec.getReactions(),mec.getViews(),mec.getInitialConcentrations(),mec.getInitialPartition(),format, consoleOut);
//			} catch (IOException e1) {
//				CRNReducerCommandLine.println(consoleOut, "Unhandled errors arised while executing the commands.");
//				CRNReducerCommandLine.printStackTrace(consoleOut,e1);
//				failed=true;
//			}
//			catch (ArithmeticException e1) {
//				CRNReducerCommandLine.println(consoleOut, "Unhandled errors arised while executing the commands.");
//				CRNReducerCommandLine.printExceptionShort(consoleOut,e1);
//				failed=true;
//			}
//			cl = new CRNReducerCommandLine(commandsReader,crnImporter.getCRN(),crnImporter.getInitialPartition(),true);
//			//cl.setImporterOfSupportedNetworks(new ImporterOfSupportedNetworksWithCRNGUI());
//			cl.setDataOutputHandler(guidog);
//			cl.setMessageDialogShower(msgVisualizer);
//			cl.setTerminator(console.getTerminator());
//		}
//		else{
//			//commandsReader.addToHead(importString);
//			cl = new CRNReducerCommandLine(commandsReader,true);
//			//cl.setImporterOfSupportedNetworks(new ImporterOfSupportedNetworksWithCRNGUI());
//			cl.setDataOutputHandler(guidog);
//			cl.setMessageDialogShower(msgVisualizer);
//			cl.setTerminator(console.getTerminator());
//			try {
//				boolean ignoreCommands=true;//Note that there are no commands, because I forbid to use load 
//				cl.importModel(mec.getImportString(), true,ignoreCommands,consoleOut);
//				/* ICRN crn = cl.getCRN();
//				if(canSynchEditor && mec.isSyncEditor()){
//					//replaceText(mec, crn);
//					replaceText(mec, crn);
//				}*/
//			} catch (UnsupportedFormatException e) {
//				CRNReducerCommandLine.println(consoleOut, "Unhandled errors arised while executing the commands.");
//				CRNReducerCommandLine.printStackTrace(consoleOut,e);
//				failed=true;
//			}/* catch (BadLocationException e) {
//				CRNReducerCommandLine.println(consoleOut,"Importing of model in the text editor failed.");
//				CRNReducerCommandLine.printStackTrace(consoleOut, e);
//			}*/
//		}

		if(!failed){
			if(multithreaded){
				//CRNReducerCommandLine cl = loadModel(mec, msgVisualizer, constraints, commandsReader, guidog);
				//MyCRNProgamExecutorWorker worker = new MyCRNProgamExecutorWorker(cl,consoleOut,project,console,threadsBuffer);
				MyCRNProgamExecutorWorker worker = new MyCRNProgamExecutorWorker(consoleOut,bwOut,project,console,threadsBuffer , mec, msgVisualizer, constraints, booleanUpdateFunctions, commandsReader, guidog);
				worker.start();
				//System.out.println("Done");
				/*Thread t = new Thread(
						new Runnable() {
							public void run() {
								try {
									cl.executeCommands(true,consoleOut);
								} catch (Exception e) {
									CRNReducerCommandLine.println(consoleOut, "Unhandled errors arised while executing the commands.");
									CRNReducerCommandLine.printStackTrace(consoleOut,e);
								}
								consoleOut.println(MyConsoleUtil.computeGoodbye());
							}
						});
				t.start();*/
				/*//Schedule a job for the event dispatch thread:
				//creating and showing this application's GUI.
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						try {
							cl.executeCommands(true,consoleOut);
						} catch (Exception e) {
							CRNReducerCommandLine.println(consoleOut, "Unhandled errors arised while executing the commands.");
							CRNReducerCommandLine.printStackTrace(consoleOut,e);
						}
						consoleOut.println(MyConsoleUtil.computeGoodbye());
					}
				});*/
				
			}
			else{
				ICommandLine cl = loadModel(mec, msgVisualizer, consoleOut,bwOut, constraints, booleanUpdateFunctions,commandsReader, guidog, console);
				if(cl==null){
					failed=true;
				}
				if(!failed){
					try {
						cl.executeCommands(true,consoleOut,bwOut);
					} catch (Exception e) {
						CRNReducerCommandLine.println(consoleOut,bwOut, "Unhandled errors arised while executing the commands.");
						CRNReducerCommandLine.printStackTrace(consoleOut,bwOut,e);
					}
					catch (OutOfMemoryError e) {
						CRNReducerCommandLine.println(consoleOut,bwOut, "An out of memory exception arised while executing the commands.");
						CRNReducerCommandLine.printStackTrace(consoleOut,bwOut,e);
					}
				}
				String goodBye = MyConsoleUtil.computeGoodbye(consoleOut);
				CRNReducerCommandLine.println(consoleOut, bwOut, goodBye);
				console.setTerminationFlag();
			}
		}
		//console.setTerminationFlag();
		//console.setCompleted();
	}
	
	private LinkedHashMap<String,IUpdateFunction> parseBooleanUpdateFunctions(EList<it.imt.erode.crn.chemicalReactionNetwork.NodeDefinition> booleanUpdateFunctionsXTEXT, MessageConsoleStream out,BufferedWriter bwOut) {
		LinkedHashMap<String,IUpdateFunction> booleanUpdateFunctions=null;
		if(booleanUpdateFunctionsXTEXT==null){
			return new LinkedHashMap<>(0);
		}
		else{
			booleanUpdateFunctions=new LinkedHashMap<String,IUpdateFunction>(booleanUpdateFunctionsXTEXT.size());
			for (NodeDefinition nodeDef: booleanUpdateFunctionsXTEXT) {
				Node node = nodeDef.getName();
				BoolExpr booleanUpdateFunctionXText = nodeDef.getUpdateFunction();
				booleanUpdateFunctions.put(node.getName(), visitUpdateFunction(booleanUpdateFunctionXText,out,bwOut));
			}
			return booleanUpdateFunctions;
		}
	}
	private IUpdateFunction visitUpdateFunction(BoolExpr booleanUpdateFunctionXText, MessageConsoleStream out,BufferedWriter bwOut) {
		if(booleanUpdateFunctionXText instanceof it.imt.erode.crn.chemicalReactionNetwork.False){
			return new FalseUpdateFunction();
		}
		else if(booleanUpdateFunctionXText instanceof it.imt.erode.crn.chemicalReactionNetwork.True){
			return new TrueUpdateFunction();
		}
		else if(booleanUpdateFunctionXText instanceof it.imt.erode.crn.chemicalReactionNetwork.NotBooleanExpr){
			BoolExpr left = ((it.imt.erode.crn.chemicalReactionNetwork.NotBooleanExpr)booleanUpdateFunctionXText).getLeft();
			return new NotBooleanUpdateFunction(visitUpdateFunction(left,out,bwOut));
		}
		else if(booleanUpdateFunctionXText instanceof it.imt.erode.crn.chemicalReactionNetwork.AndBooleanExpr){
			IUpdateFunction visitedLeft = visitUpdateFunction(((it.imt.erode.crn.chemicalReactionNetwork.AndBooleanExpr) booleanUpdateFunctionXText).getLeft(),out,bwOut);
			IUpdateFunction visitedRight = visitUpdateFunction(((it.imt.erode.crn.chemicalReactionNetwork.AndBooleanExpr) booleanUpdateFunctionXText).getRight(),out,bwOut);
			return new BooleanUpdateFunctionExpr(visitedLeft,visitedRight,BooleanConnector.AND);
		}
		else if(booleanUpdateFunctionXText instanceof it.imt.erode.crn.chemicalReactionNetwork.OrBooleanExpr){
			IUpdateFunction visitedLeft = visitUpdateFunction(((it.imt.erode.crn.chemicalReactionNetwork.OrBooleanExpr) booleanUpdateFunctionXText).getLeft(),out,bwOut);
			IUpdateFunction visitedRight = visitUpdateFunction(((it.imt.erode.crn.chemicalReactionNetwork.OrBooleanExpr) booleanUpdateFunctionXText).getRight(),out,bwOut);
			return new BooleanUpdateFunctionExpr(visitedLeft,visitedRight,BooleanConnector.OR);
		}
		else if(booleanUpdateFunctionXText instanceof it.imt.erode.crn.chemicalReactionNetwork.ImpliesBooleanExpr){
			IUpdateFunction visitedLeft = visitUpdateFunction(((it.imt.erode.crn.chemicalReactionNetwork.ImpliesBooleanExpr) booleanUpdateFunctionXText).getLeft(),out,bwOut);
			IUpdateFunction visitedRight = visitUpdateFunction(((it.imt.erode.crn.chemicalReactionNetwork.ImpliesBooleanExpr) booleanUpdateFunctionXText).getRight(),out,bwOut);
			return new BooleanUpdateFunctionExpr(visitedLeft,visitedRight,BooleanConnector.IMPLIES);
		}
		else if(booleanUpdateFunctionXText instanceof it.imt.erode.crn.chemicalReactionNetwork.ReferenceToNode){
			Node node = ((it.imt.erode.crn.chemicalReactionNetwork.ReferenceToNode) booleanUpdateFunctionXText).getReference();
			return new ReferenceToNodeUpdateFunction(node.getName());
		}
		else{
			throw new UnsupportedOperationException("The model has a not supported update function.");
		}
	}
	
	private List<IConstraint> parseConstraints(EList<BoolExpr> constraintsListXTEXT, MessageConsoleStream out,BufferedWriter bwOut) {
		List<IConstraint> constraints=null;
		if(constraintsListXTEXT==null){
			return new ArrayList<>(0);
		}
		else{
			constraints=new ArrayList<IConstraint>(constraintsListXTEXT.size());
			for (BoolExpr constraintXText : constraintsListXTEXT) {
				constraints.add(visitConstraint(constraintXText,out,bwOut));
			}
			return constraints;
		}
	}
	private IConstraint visitConstraint(BoolExpr constraintXText, MessageConsoleStream out,BufferedWriter bwOut) {
		if(constraintXText instanceof it.imt.erode.crn.chemicalReactionNetwork.FalseConstraint){
			return new FalseConstraint();
		}
		else if(constraintXText instanceof it.imt.erode.crn.chemicalReactionNetwork.TrueConstraint){
			return new TrueConstraint();
		}
		else if(constraintXText instanceof it.imt.erode.crn.chemicalReactionNetwork.NotConstraintExpr){
			BoolExpr left = ((it.imt.erode.crn.chemicalReactionNetwork.NotConstraintExpr)constraintXText).getLeft();
			return new NotConstraintExpr(visitConstraint(left,out,bwOut));
		}
		else if(constraintXText instanceof it.imt.erode.crn.chemicalReactionNetwork.AndBoolConstraintExpr){
			IConstraint visitedLeft = visitConstraint(((it.imt.erode.crn.chemicalReactionNetwork.AndBoolConstraintExpr) constraintXText).getLeft(),out,bwOut);
			IConstraint visitedRight = visitConstraint(((it.imt.erode.crn.chemicalReactionNetwork.AndBoolConstraintExpr) constraintXText).getRight(),out,bwOut);
			return new BooleanConstraintExpr(visitedLeft,visitedRight,BooleanConnector.AND);
		}
		else if(constraintXText instanceof it.imt.erode.crn.chemicalReactionNetwork.OrBoolConstraintExpr){
			IConstraint visitedLeft = visitConstraint(((it.imt.erode.crn.chemicalReactionNetwork.OrBoolConstraintExpr) constraintXText).getLeft(),out,bwOut);
			IConstraint visitedRight = visitConstraint(((it.imt.erode.crn.chemicalReactionNetwork.OrBoolConstraintExpr) constraintXText).getRight(),out,bwOut);
			return new BooleanConstraintExpr(visitedLeft,visitedRight,BooleanConnector.OR);
		}
		else if(constraintXText instanceof it.imt.erode.crn.chemicalReactionNetwork.ImpliesBoolConstraintExpr){
			IConstraint visitedLeft = visitConstraint(((it.imt.erode.crn.chemicalReactionNetwork.ImpliesBoolConstraintExpr) constraintXText).getLeft(),out,bwOut);
			IConstraint visitedRight = visitConstraint(((it.imt.erode.crn.chemicalReactionNetwork.ImpliesBoolConstraintExpr) constraintXText).getRight(),out,bwOut);
			return new BooleanConstraintExpr(visitedLeft,visitedRight,BooleanConnector.IMPLIES);
		}
		else if(constraintXText instanceof it.imt.erode.crn.chemicalReactionNetwork.BasicConstraint){
			SymbolicParameter symbolicParameter = ((it.imt.erode.crn.chemicalReactionNetwork.BasicConstraint) constraintXText).getSymbolicParameter();
			String expr = MyParserUtil.visitExpr(((it.imt.erode.crn.chemicalReactionNetwork.BasicConstraint) constraintXText).getConstraint());
			
			//var writtenrhs = writeExpr(constraint.rhs)
			String comp = ((it.imt.erode.crn.chemicalReactionNetwork.BasicConstraint) constraintXText).getComp();
			return new BasicConstraint(symbolicParameter.getName(), expr,toComparator(comp),out,bwOut);
		}
		else{
			throw new UnsupportedOperationException("The model has a not supported constraint.");
		}
	}
	private BasicConstraintComparator toComparator(String comp) {
		if(comp.equals(">")){
			return BasicConstraintComparator.GE;
		}
		else if(comp.equals("<")){
			return BasicConstraintComparator.LE;
		}
		else if(comp.equals(">=")){
			return BasicConstraintComparator.GEQ;
		}
		else if(comp.equals("<=")){
			return BasicConstraintComparator.LEQ;
		}
		else if(comp.equals("=")){
			return BasicConstraintComparator.EQ;
		} 
		else if(comp.equals("!=")){
			return BasicConstraintComparator.NOTEQ;
		}
		else{
			throw new UnsupportedOperationException("Unsupported comparator: " + comp);
		}
	}
	
	public static ICommandLine loadModel(ModelElementsCollector mec, IMessageDialogShower msgVisualizer, MessageConsoleStream consoleOut, BufferedWriter bwOut,List<IConstraint> constraints, LinkedHashMap<String, IUpdateFunction> booleanUpdateFunctions, CommandsReader commandsReader, IDataOutputHandler guidog, MyMessageConsole console ){
		ICommandLine cl=null;
		boolean failed=false;
		//I have to launch the new thread starting from here
		if(mec.getModelDefKind().equals(ModelDefKind.BOOLEAN)){
			boolean printBooleanNetwork=false;
			GUIBooleanNetworkImporter bnImporter = new GUIBooleanNetworkImporter(consoleOut,bwOut,msgVisualizer);
			try {
				bnImporter.importBooleanNetwork(true, printBooleanNetwork, true, mec.getModelName(), mec.getInitialConcentrations(), booleanUpdateFunctions, mec.getUserPartition(), consoleOut);
			} catch (IOException e) {
				CRNReducerCommandLine.println(consoleOut,bwOut, "Unhandled errors arised while executing the commands.");
				CRNReducerCommandLine.printStackTrace(consoleOut,bwOut,e);
				failed=true;
			}
			
			cl = new BooleanNetworkCommandLine(commandsReader,bnImporter.getBooleanNetwork(),bnImporter.getInitialPartition(),true);
			cl.setDataOutputHandler(guidog);
			cl.setMessageDialogShower(msgVisualizer);
			cl.setTerminator(console.getTerminator());
			
		}
		else if(mec.getModelDefKind().equals(ModelDefKind.RN)||mec.getModelDefKind().equals(ModelDefKind.ODE)){
			GUICRNImporter crnImporter = new GUICRNImporter(consoleOut,bwOut,msgVisualizer);
			try {
				boolean printCRN=false;
				ODEorNET format = mec.getModelDefKind().equals(ModelDefKind.RN)? ODEorNET.RN : ODEorNET.ODE;
				crnImporter.importCRNNetwork(true, printCRN, true, mec.getModelName(), mec.getSymbolicParameters(),constraints,mec.getParameters(), mec.getReactions(),mec.getAlgebraicConstraints(),mec.getViews(),mec.getInitialConcentrations(),mec.getInitialAlgConcentrations(),mec.getUserPartition(),format, consoleOut);
			} catch (IOException e1) {
				CRNReducerCommandLine.println(consoleOut,bwOut, "Unhandled errors arised while executing the commands.");
				CRNReducerCommandLine.printStackTrace(consoleOut,bwOut,e1);
				failed=true;
			}
			catch (ArithmeticException e1) {
				CRNReducerCommandLine.println(consoleOut,bwOut, "Unhandled errors arised while executing the commands.");
				CRNReducerCommandLine.printExceptionShort(consoleOut,bwOut,e1);
				failed=true;
			}
			cl = new CRNReducerCommandLine(commandsReader,crnImporter.getCRN(),crnImporter.getInitialPartition(),true);
			//cl.setImporterOfSupportedNetworks(new ImporterOfSupportedNetworksWithCRNGUI());
			cl.setDataOutputHandler(guidog);
			cl.setMessageDialogShower(msgVisualizer);
			cl.setTerminator(console.getTerminator());
		}
		else if(mec.getModelDefKind().equals(ModelDefKind.IMPORT) || mec.getModelDefKind().equals(ModelDefKind.IMPORTFOLDER) || mec.getModelDefKind().equals(ModelDefKind.BOOLEANIMPORTFOLDER)){
			//commandsReader.addToHead(importString);
			cl = new CRNReducerCommandLine(commandsReader,true);
			//cl.setImporterOfSupportedNetworks(new ImporterOfSupportedNetworksWithCRNGUI());
			cl.setDataOutputHandler(guidog);
			cl.setMessageDialogShower(msgVisualizer);
			cl.setTerminator(console.getTerminator());
			try {
				boolean ignoreCommands=true;//Note that there are no commands, because I forbid to use load
				if(mec.getModelDefKind().equals(ModelDefKind.IMPORT)) {
					((CRNReducerCommandLine)cl).importModel(mec.getImportString(), true,ignoreCommands,consoleOut,bwOut);
				}
				else if (mec.getModelDefKind().equals(ModelDefKind.IMPORTFOLDER)){
					((CRNReducerCommandLine)cl).importModelsFromFolder(mec.getImportFolderString(), true,ignoreCommands,consoleOut,bwOut);
				}
				else { //mec.getModelDefKind().equals(ModelDefKind.BOOLEANIMPORTFOLDER)
					((CRNReducerCommandLine)cl).importBooleanModelsFromFolder(mec.getImportFolderString(), true,ignoreCommands,consoleOut,bwOut);
				}
				/* ICRN crn = cl.getCRN();
						if(canSynchEditor && mec.isSyncEditor()){
							//replaceText(mec, crn);
							replaceText(mec, crn);
						}*/
			} catch (UnsupportedFormatException e) {
				CRNReducerCommandLine.println(consoleOut,bwOut, "Unhandled errors arised while executing the commands.");
				CRNReducerCommandLine.printStackTrace(consoleOut,bwOut,e);
				failed=true;
			}/* catch (BadLocationException e) {
						CRNReducerCommandLine.println(consoleOut,"Importing of model in the text editor failed.");
						CRNReducerCommandLine.printStackTrace(consoleOut, e);
					}*/
		}
		else {
			failed = true;
		}
			
		if(failed){
			return null;
		}
		else{
			return cl;
		}
	}

	/*private void replaceText(ModelElementsCollector mec, ICRN crn) throws BadLocationException {
		
		XtextEditor xtextEditor = EditorUtils.getActiveXtextEditor();
		IXtextDocument xtextDocument = xtextEditor.getDocument();
		
		ICompositeNode importNode = NodeModelUtils.findActualNodeFor(mec.getImportCommand());
		ITextRegion importTextRegionWithoutComments = importNode.getTextRegion();
		
		String textToReplace = xtextDocument.get(importTextRegionWithoutComments.getOffset(), importTextRegionWithoutComments.getLength());
		
		String crnString = GUICRNImporter.buildXtextCRNString(crn,crn.getMdelDefKind());
		textToReplace = textToReplace.replace("\n", "\n//");
		xtextDocument.replace(importTextRegionWithoutComments.getOffset(), importTextRegionWithoutComments.getLength(), "//"+textToReplace+"\n"+crnString);
	}*/



	/*	private String buildXtextCRNString(ICRN crn) {
		StringBuilder sb = new StringBuilder();
		if(crn!=null){
			sb.append(" begin parameters\n");
			for (String param : crn.getParameters()) {
				int space = param.indexOf(' ');
				String parName=param.substring(0,space);
				String parExpr = param.substring(space+1);
				sb.append("  ");
				sb.append(parName.trim());
				sb.append(" = ");
				sb.append(parExpr.trim());
				sb.append(";\n");
			}
			sb.append(" end parameters\n");

			sb.append(" begin CRN\n");
			sb.append("  begin species\n");
			for (ISpecies species : crn.getSpecies()) {
				sb.append("   ");
				sb.append(species.getName());
				sb.append(" = ");
				sb.append(species.getInitialConcentrationExpr());
				sb.append("\n");
			}
			sb.append("  end species\n");
			sb.append("  begin reactions\n");
			for (ICRNReaction reaction : crn.getReactions()) {
				sb.append("   ");
				sb.append(reaction.getReagents().toMultiSet());
				sb.append(" -> ");
				sb.append(reaction.getProducts().toMultiSet());
				sb.append(" , ");
				if(reaction.hasArbitraryKinetics()){
					sb.append("arbitrary ");
				}
				sb.append(reaction.getRateExpression());
				sb.append(";\n");
			}
			sb.append("  end reactions\n");
			sb.append(" end CRN\n");
			sb.append(" begin views\n");
			for(int i=0; i<crn.getViewNames().length;i++){
				sb.append("  ");
				sb.append(crn.getViewNames()[i]);
				sb.append(" = ");
				sb.append(crn.getViewExpressions()[i]);
				sb.append(";\n");
			}
			sb.append(" end views");
		}
		else{
			sb.append("//UNDEFINED CRN");
		}
		return sb.toString();
	}*/

	/*private String getCommandText(IXtextDocument xtextDocument,FindReplaceDocumentAdapter frda, String importName) throws BadLocationException {
		IRegion start = frda.find(0,importName+"({",true,true,false,false);
		IRegion end = frda.find(start.getOffset()+start.getLength(),"})",true,true,false,false);
		String commandInEditor=xtextDocument.get(start.getOffset(), end.getOffset()+start.getLength()-start.getOffset());
		return commandInEditor;
	}*/

	/*public static String getText(FileIn par){
		return "fileIn=>"+par.getFileIn();
	}*/


	//public void read(EList<Parameter> parametersList, EList<Species> speciesList, EList<Reaction> reactionsList, EList<ODE> odes, EList<View> viewsList, EList<InitialConcentration> icList, Iterable<Command> commandsList) {
	//public void read(Iterable<ParametersList> parametersListIt, Iterable<SpeciesList> speciesListIt, Iterable<ReactionsList> reactionsListIt, Iterable<ODEsList> odesIt, Iterable<ViewsList> viewsListIt, Iterable<ICList> icListIt, Iterable<Command> commandsList) {
	/*public void read(Iterable<ParametersList> parametersListIt, Iterable<SpeciesList> speciesListIt, Iterable<ReactionsList> reactionsListIt, Iterable<ODEsList> odesIt, Iterable<ViewsList> viewsListIt, Iterable<ICList> icListIt, Iterable<Command> commandsList) {

		//ArrayList<ArrayList<String>> parameters = new ArrayList<ArrayList<String>>(parametersList.size());
		ArrayList<ArrayList<String>> parameters = new ArrayList<ArrayList<String>>(0);
		if(parametersListIt!=null && parametersListIt.iterator().hasNext()){
			EList<Parameter> parametersList = parametersListIt.iterator().next().getParameters();
			parameters = new ArrayList<ArrayList<String>>(parametersList.size());
			for (Parameter param : parametersList) {
				//name,value
				ArrayList<String> par = new ArrayList<>(2);
				parameters.add(par);
				par.add(param.getName());
				String val = MyArithmeticExpressionUtil.visitExpr(param.getParamValue().getValue());
				par.add(val);
			}
		}

		ArrayList<ArrayList<String>> reactions = new ArrayList<ArrayList<String>>(0);
		if(reactionsListIt!=null && reactionsListIt.iterator().hasNext()){
			EList<Reaction> reactionsList = reactionsListIt.iterator().next().getAllReactions();
			reactions = new ArrayList<ArrayList<String>>(reactionsList.size());
			for(Reaction reac : reactionsList){
				//reagent,product,rate
				ArrayList<String> r = new ArrayList<>(3);
				reactions.add(r);
				//reac.getReagents()
				r.add(reac.getReagents().getSpeciesOfComposite().get(0).getSpecies().getName());
				r.add(reac.getProducts().getSpeciesOfComposite().get(0).getSpecies().getName());
				//r.add(reac.getRate().getValue().toString());
				//reactions.put(reac.getReagents().getSpeciesOfComposite().get(0).getSpecies().getName(), value)
				String val = MyArithmeticExpressionUtil.visitExpr(reac.getRate().getValue().getValue());//TODO: works only for mass action reactions
				r.add(val);
			}
		}
		else {
			//either reactions or odes must be defined
			EList<ODE> odes = odesIt.iterator().next().getOdes();
			reactions = new ArrayList<ArrayList<String>>(odes.size());
			int i=0;
			for(ODE ode : odes){
				Species speciesOfOde = ode.getName().getAllSpecies().get(i);
				ArithmeticExpressionWithSpecies drift = ode.getDrift();
				String driftString = MyArithmeticExpressionUtil.visitExpr(drift.getValue());
				i++;
				//IComposite products = new Composite(speciesOfODE,speciesOfODE);
				//ICRNReaction reaction = new CRNReactionArbitrary((IComposite)speciesOfODE, products, body,varsName);
				//speciesOfTheODE,arbitrary,rate
				ArrayList<String> r = new ArrayList<>(3);
				reactions.add(r);
				r.add(speciesOfOde.getName());//r.add(2*speciesOfOde.getName());
				r.add("arbitrary");
				r.add(driftString);
			}
		}

		//ArrayList<ArrayList<String>> views = new ArrayList<ArrayList<String>>(viewsList.size());
		ArrayList<ArrayList<String>> views = new ArrayList<ArrayList<String>>(0);
		if(viewsListIt!=null && viewsListIt.iterator().hasNext()){
			EList<View> viewsList = viewsListIt.iterator().next().getAllViews();
			views = new ArrayList<ArrayList<String>>(viewsList.size());
			for (View v : viewsList) {
				//name,value
				ArrayList<String> view = new ArrayList<>(2);
				views.add(view);
				view.add(v.getName());
				String val = MyArithmeticExpressionUtil.visitExpr(v.getExpr().getValue());
				view.add(val);
			}
		}

		//ArrayList<ArrayList<String>> initialConcentrations = new ArrayList<ArrayList<String>>(icList.size());
		ArrayList<ArrayList<String>> initialConcentrations = new ArrayList<ArrayList<String>>(0);
		if(icListIt!=null && icListIt.iterator().hasNext()){
			EList<InitialConcentration> icList = icListIt.iterator().next().getAllIC();
			initialConcentrations = new ArrayList<ArrayList<String>>(icList.size());
			for (InitialConcentration ic : icList) {
				//name,value
				ArrayList<String> initialConcentration = new ArrayList<>(2);
				initialConcentrations.add(initialConcentration);
				initialConcentration.add(ic.getName().getName());
				String val = MyArithmeticExpressionUtil.visitExpr(ic.getIc().getValue());
				initialConcentration.add(val);
			}
		}

		CRNImporter crnImporter = new CRNImporter();
		try {
			crnImporter.importCRNNetwork(true, true, true, parameters, reactions,views,initialConcentrations);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			System.exit(-1);
		}

		System.out.println();
		List<String> commands = new ArrayList<String>();
		for(Command command : commandsList){
			commands.add(command.toString());
			commands.add("newline");
		}
		CommandsReader commandsReader = new CommandsReader(commands);
		CRNReducerCommandLine cl = new CRNReducerCommandLine(commandsReader,crnImporter.getCRN(),crnImporter.getInitialPartition());
		try {
			cl.executeCommands(true);
		} catch (Exception e) {
			System.out.println("Unhandled errors arised while executing the commands. I terminate.");
			e.printStackTrace();
		}
	}*/



	/*public void read(Iterable<EObject> contents) {
		EList<Parameter> parametersList = contents.
				(typeOf ParametersList).get(0).parameters;
	}*/



}
