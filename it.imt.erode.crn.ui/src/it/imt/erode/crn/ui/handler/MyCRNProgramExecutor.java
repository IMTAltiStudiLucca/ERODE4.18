package it.imt.erode.crn.ui.handler;

import java.io.BufferedWriter;
import java.io.File;
//import java.io.FileNotFoundException;
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
import it.imt.erode.commandline.ProbProgCommandLine;
import it.imt.erode.crn.ModelDefKind;
import it.imt.erode.crn.ModelElementsCollector;
import it.imt.erode.crn.MyParserUtil;
import it.imt.erode.crn.chemicalReactionNetwork.BoolExpr;
import it.imt.erode.crn.chemicalReactionNetwork.ExprReferenceToMVNodeOrValue;
import it.imt.erode.crn.chemicalReactionNetwork.MVExpr;
import it.imt.erode.crn.chemicalReactionNetwork.MVNode;
import it.imt.erode.crn.chemicalReactionNetwork.MVNodeDefinition;
import it.imt.erode.crn.chemicalReactionNetwork.MVUpdateFunctions;
import it.imt.erode.crn.chemicalReactionNetwork.MaxArithExprReferenceToMVNodeOrValue;
import it.imt.erode.crn.chemicalReactionNetwork.MinArithExprReferenceToMVNodeOrValue;
import it.imt.erode.crn.chemicalReactionNetwork.ModelDefinition;
import it.imt.erode.crn.chemicalReactionNetwork.MulReferenceToMVNodeOrValue;
import it.imt.erode.crn.chemicalReactionNetwork.Node;
import it.imt.erode.crn.chemicalReactionNetwork.NodeDefinition;
import it.imt.erode.crn.chemicalReactionNetwork.SumReferenceToMVNodeOrValue;
import it.imt.erode.crn.chemicalReactionNetwork.SymbolicParameter;
import it.imt.erode.crn.chemicalReactionNetwork.caseMV;
import it.imt.erode.booleannetwork.updatefunctions.ArithmeticConnector;
import it.imt.erode.booleannetwork.updatefunctions.BasicModelElementsCollector;
import it.imt.erode.booleannetwork.updatefunctions.BinaryExprIUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.BooleanUpdateFunctionExpr;
import it.imt.erode.booleannetwork.updatefunctions.FalseUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.MVComparison;
import it.imt.erode.booleannetwork.updatefunctions.MVUpdateFunctionByCases;
import it.imt.erode.booleannetwork.updatefunctions.NotBooleanUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.Otherwise;
import it.imt.erode.booleannetwork.updatefunctions.ReferenceToNodeUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.TrueUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.ValUpdateFunction;
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
import it.imt.erode.importing.probprog.GUIProbProgImporter;
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
		if(modelDef.getReal()!=null) {
			mec.setRealSortMVNet(true);
		}
		
		List<IConstraint> constraints = parseConstraints(mec.getConstraintsListXTEXT(),consoleOut,bwOut);
		LinkedHashMap<String, IUpdateFunction> booleanUpdateFunctions = new LinkedHashMap<>(0);
		if(mec.getBooleanUpdateFunctionsXTEXT()!=null) {
			booleanUpdateFunctions=parseBooleanUpdateFunctions(mec.getBooleanUpdateFunctionsXTEXT(), consoleOut, bwOut);
		}
		else if(mec.getMVBooleanUpdateFunctionsXTEXT() !=null) {
			booleanUpdateFunctions=parseMVUpdateFunctions(mec.getMVBooleanUpdateFunctionsXTEXT(), consoleOut, bwOut);
		}
						
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
	
	private LinkedHashMap<String,IUpdateFunction> parseMVUpdateFunctions(EList<it.imt.erode.crn.chemicalReactionNetwork.MVNodeDefinition> mvBoleanUpdateFunctionsXTEXT, MessageConsoleStream out,BufferedWriter bwOut) {
		LinkedHashMap<String,IUpdateFunction> mvUpdateFunctions=null;
		if(mvBoleanUpdateFunctionsXTEXT==null){
			return new LinkedHashMap<>(0);
		}
		else{
			mvUpdateFunctions=new LinkedHashMap<String,IUpdateFunction>(mvBoleanUpdateFunctionsXTEXT.size());
			for (MVNodeDefinition nodeDef: mvBoleanUpdateFunctionsXTEXT) {
				MVNode node = nodeDef.getName();
				
				ExprReferenceToMVNodeOrValue updFunc = nodeDef.getUpdateFunction();
				int max=Math.max(nodeDef.getName().getMax(),1);
				IUpdateFunction parsedUpdateFunc=visitArithExprMVUpdateFunc_xtextToCore(updFunc,max);
				//IUpdateFunction parsedUpdateFunc=visitMVUpdateFunction(upFunc,out,bwOut);
				mvUpdateFunctions.put(node.getName(), parsedUpdateFunc);
				
				/*
				EList<MVUpdateFunctions> addends = nodeDef.getUpdateFunction().getCaseFunction();
				ArrayList<IUpdateFunction> addends_parsed=new ArrayList<>(addends.size());
				for(MVUpdateFunctions addend: addends) {
					MVUpdateFunctions mvUpdFunc = addend;//nodeDef.getUpdateFunction();
					EList<caseMV> cases = mvUpdFunc.getCasesMV();
					LinkedHashMap<Integer, IUpdateFunction> parsedCases = new LinkedHashMap<>(cases.size());
					for(caseMV cur_case : cases) {
						Integer cur_val= cur_case.getVal();
						IUpdateFunction parsedCase=null;
						if(cur_case.getACaseMV()==null) {
							parsedCase = new Otherwise();
						}
						else {
							parsedCase = visitMVUpdateFunction(cur_case.getACaseMV(),out,bwOut);
						}
						parsedCases.put(cur_val, parsedCase);
					}
					MVUpdateFunction addend_parsed = new MVUpdateFunction(parsedCases, node.getMax());
					addends_parsed.add(addend_parsed);
				}
				mvUpdateFunctions.put(node.getName(), new MVUpdateFunctionSum(addends_parsed));
				//mvUpdateFunctions.put(node.getName(), new MVUpdateFunction(parsedCases, node.getMax()));
				*/
			}
			return mvUpdateFunctions;
		}
	}
	
	private IUpdateFunction/*_ArithExprRefToNode_Value*/ visitArithExprMVUpdateFunc_xtextToCore(/*ReferenceToMVNodeOrValue*//*SumReferenceToMVNodeOrValue*/ExprReferenceToMVNodeOrValue arithExprUpdFunc,int max) {
		if(arithExprUpdFunc instanceof it.imt.erode.crn.chemicalReactionNetwork.ReferenceToMVNode) {
			return new ReferenceToNodeUpdateFunction(((it.imt.erode.crn.chemicalReactionNetwork.ReferenceToMVNode) arithExprUpdFunc).getReference().getName());
		}
		else if(arithExprUpdFunc instanceof it.imt.erode.crn.chemicalReactionNetwork.Value) {
			return new ValUpdateFunction(((it.imt.erode.crn.chemicalReactionNetwork.Value) arithExprUpdFunc).getVal().getValue());
		}
		else if(arithExprUpdFunc instanceof it.imt.erode.crn.chemicalReactionNetwork.MinusPrimaryExprRefNodeOrValue) {
			IUpdateFunction inner = visitArithExprMVUpdateFunc_xtextToCore(((it.imt.erode.crn.chemicalReactionNetwork.MinusPrimaryExprRefNodeOrValue) arithExprUpdFunc).getLeft(),max);
			return new BinaryExprIUpdateFunction(
					new ValUpdateFunction(0),
					inner,
					ArithmeticConnector.SUB);
		}
		else if(arithExprUpdFunc instanceof MulReferenceToMVNodeOrValue) {
			return new BinaryExprIUpdateFunction(
					visitArithExprMVUpdateFunc_xtextToCore(((MulReferenceToMVNodeOrValue)arithExprUpdFunc).getLeft(),max),
					visitArithExprMVUpdateFunc_xtextToCore(((MulReferenceToMVNodeOrValue)arithExprUpdFunc).getRight(),max),
					ArithmeticConnector.MUL);
		}
		else if(arithExprUpdFunc instanceof SumReferenceToMVNodeOrValue) {
			ArithmeticConnector conn = ArithmeticConnector.SUM;
			if(((SumReferenceToMVNodeOrValue)arithExprUpdFunc).getSign().equals("-")) {
				conn = ArithmeticConnector.SUB;
			}
			return new BinaryExprIUpdateFunction(
					visitArithExprMVUpdateFunc_xtextToCore(((SumReferenceToMVNodeOrValue)arithExprUpdFunc).getLeft(),max),
					visitArithExprMVUpdateFunc_xtextToCore(((SumReferenceToMVNodeOrValue)arithExprUpdFunc).getRight(),max),
					conn);
		}
		else if(arithExprUpdFunc instanceof MinArithExprReferenceToMVNodeOrValue) {
			return new BinaryExprIUpdateFunction(
					visitArithExprMVUpdateFunc_xtextToCore(((MinArithExprReferenceToMVNodeOrValue)arithExprUpdFunc).getLeft(),max),
					visitArithExprMVUpdateFunc_xtextToCore(((MinArithExprReferenceToMVNodeOrValue)arithExprUpdFunc).getRight(),max),
					ArithmeticConnector.MIN);
		}
		else if(arithExprUpdFunc instanceof MaxArithExprReferenceToMVNodeOrValue) {
			return new BinaryExprIUpdateFunction(
					visitArithExprMVUpdateFunc_xtextToCore(((MaxArithExprReferenceToMVNodeOrValue)arithExprUpdFunc).getLeft(),max),
					visitArithExprMVUpdateFunc_xtextToCore(((MaxArithExprReferenceToMVNodeOrValue)arithExprUpdFunc).getRight(),max),
					ArithmeticConnector.MAX);
		}
		else if(arithExprUpdFunc instanceof MVUpdateFunctions) {
			EList<caseMV> cases = ((MVUpdateFunctions) arithExprUpdFunc).getCasesMV();
			LinkedHashMap<Integer, IUpdateFunction> parsedCases = new LinkedHashMap<>(cases.size());
			for(caseMV cur_case : cases) {
				Integer cur_val= cur_case.getVal();
				IUpdateFunction parsedCase=null;
				if(cur_case.getACaseMV()==null) {
					parsedCase = new Otherwise();
				}
				else {
					parsedCase = visitMVUpdateFunction_BooleanPart(cur_case.getACaseMV(),max);
				}
				parsedCases.put(cur_val, parsedCase);
			}
			return new MVUpdateFunctionByCases(parsedCases, max);
		}
		else {
			throw new UnsupportedOperationException("Unsupported ref to node or val:"+arithExprUpdFunc);
		}
	}
	
	private IUpdateFunction visitMVUpdateFunction_BooleanPart(MVExpr caseMVUpdateFunctionXText,int max/*, MessageConsoleStream out, BufferedWriter bwOut*/) {
		if(caseMVUpdateFunctionXText instanceof it.imt.erode.crn.chemicalReactionNetwork.False){
			return new FalseUpdateFunction();
		}
		else if(caseMVUpdateFunctionXText instanceof it.imt.erode.crn.chemicalReactionNetwork.True){
			return new TrueUpdateFunction();
		}
//		else if(caseMVUpdateFunctionXText instanceof it.imt.erode.crn.chemicalReactionNetwork.SumReferenceToMVNodeOrValue){
//			left = ((it.imt.erode.crn.chemicalReactionNetwork.SumReferenceToMVNodeOrValue) caseMVUpdateFunctionXText).getLeft();
//			right= ((it.imt.erode.crn.chemicalReactionNetwork.SumReferenceToMVNodeOrValue) caseMVUpdateFunctionXText).getRight();
//		}
		else if(caseMVUpdateFunctionXText instanceof it.imt.erode.crn.chemicalReactionNetwork.PrimaryMVComparison){
			ExprReferenceToMVNodeOrValue left = ((it.imt.erode.crn.chemicalReactionNetwork.PrimaryMVComparison) caseMVUpdateFunctionXText).getLeft();
			ExprReferenceToMVNodeOrValue right = ((it.imt.erode.crn.chemicalReactionNetwork.PrimaryMVComparison) caseMVUpdateFunctionXText).getRight();
			String comp = ((it.imt.erode.crn.chemicalReactionNetwork.PrimaryMVComparison) caseMVUpdateFunctionXText).getComp();
			return new MVComparison(visitArithExprMVUpdateFunc_xtextToCore(left,max),visitArithExprMVUpdateFunc_xtextToCore(right,max),toComparator(comp));
		}
		else  if(caseMVUpdateFunctionXText instanceof it.imt.erode.crn.chemicalReactionNetwork.NotMVExpr){
			MVExpr left = ((it.imt.erode.crn.chemicalReactionNetwork.NotMVExpr)caseMVUpdateFunctionXText).getLeft();
			return new NotBooleanUpdateFunction(visitMVUpdateFunction_BooleanPart(left,max));
		}
//		else if(caseMVUpdateFunctionXText instanceof it.imt.erode.crn.chemicalReactionNetwork.NEqMVExpr){
//			IUpdateFunction visitedLeft = visitUpdateFunction(((it.imt.erode.crn.chemicalReactionNetwork.EqBooleanExpr) caseMVUpdateFunctionXText).getLeft(),out,bwOut);
//			IUpdateFunction visitedRight = visitUpdateFunction(((it.imt.erode.crn.chemicalReactionNetwork.EqBooleanExpr) caseMVUpdateFunctionXText).getRight(),out,bwOut);
//			return new BooleanBinaryExprUpdateFunction(visitedLeft,visitedRight,BooleanConnector.NEQ);
//		}
//		else if(caseMVUpdateFunctionXText instanceof it.imt.erode.crn.chemicalReactionNetwork.EqBooleanExpr){
//			IUpdateFunction visitedLeft = visitUpdateFunction(((it.imt.erode.crn.chemicalReactionNetwork.EqBooleanExpr) caseMVUpdateFunctionXText).getLeft(),out,bwOut);
//			IUpdateFunction visitedRight = visitUpdateFunction(((it.imt.erode.crn.chemicalReactionNetwork.EqBooleanExpr) caseMVUpdateFunctionXText).getRight(),out,bwOut);
//			return new BooleanBinaryExprUpdateFunction(visitedLeft,visitedRight,BooleanConnector.EQ);
//		}
		else if(caseMVUpdateFunctionXText instanceof it.imt.erode.crn.chemicalReactionNetwork.AndMVExpr){
			IUpdateFunction visitedLeft = visitMVUpdateFunction_BooleanPart(((it.imt.erode.crn.chemicalReactionNetwork.AndMVExpr) caseMVUpdateFunctionXText).getLeft(),max);
			IUpdateFunction visitedRight = visitMVUpdateFunction_BooleanPart(((it.imt.erode.crn.chemicalReactionNetwork.AndMVExpr) caseMVUpdateFunctionXText).getRight(),max);
			return new BooleanUpdateFunctionExpr(visitedLeft,visitedRight,BooleanConnector.AND);
		}
		else if(caseMVUpdateFunctionXText instanceof it.imt.erode.crn.chemicalReactionNetwork.OrMVExpr){
			IUpdateFunction visitedLeft = visitMVUpdateFunction_BooleanPart(((it.imt.erode.crn.chemicalReactionNetwork.OrMVExpr) caseMVUpdateFunctionXText).getLeft(),max);
			IUpdateFunction visitedRight = visitMVUpdateFunction_BooleanPart(((it.imt.erode.crn.chemicalReactionNetwork.OrMVExpr) caseMVUpdateFunctionXText).getRight(),max);
			return new BooleanUpdateFunctionExpr(visitedLeft,visitedRight,BooleanConnector.OR);
		}
		else if(caseMVUpdateFunctionXText instanceof it.imt.erode.crn.chemicalReactionNetwork.XorMVExpr){
			IUpdateFunction visitedLeft = visitMVUpdateFunction_BooleanPart(((it.imt.erode.crn.chemicalReactionNetwork.XorMVExpr) caseMVUpdateFunctionXText).getLeft(),max);
			IUpdateFunction visitedRight = visitMVUpdateFunction_BooleanPart(((it.imt.erode.crn.chemicalReactionNetwork.XorMVExpr) caseMVUpdateFunctionXText).getRight(),max);
			return new BooleanUpdateFunctionExpr(visitedLeft,visitedRight,BooleanConnector.XOR);
		}
		else if(caseMVUpdateFunctionXText instanceof it.imt.erode.crn.chemicalReactionNetwork.ImpliesMVExpr){
			IUpdateFunction visitedLeft = visitMVUpdateFunction_BooleanPart(((it.imt.erode.crn.chemicalReactionNetwork.ImpliesMVExpr) caseMVUpdateFunctionXText).getLeft(),max);
			IUpdateFunction visitedRight = visitMVUpdateFunction_BooleanPart(((it.imt.erode.crn.chemicalReactionNetwork.ImpliesMVExpr) caseMVUpdateFunctionXText).getRight(),max);
			return new BooleanUpdateFunctionExpr(visitedLeft,visitedRight,BooleanConnector.IMPLIES);
		}
//		else if(caseMVUpdateFunctionXText instanceof it.imt.erode.crn.chemicalReactionNetwork.ReferenceToNode){
//			Node node = ((it.imt.erode.crn.chemicalReactionNetwork.ReferenceToNode) caseMVUpdateFunctionXText).getReference();
//			return new ReferenceToNodeUpdateFunction(node.getName());
//		}
		else{
			throw new UnsupportedOperationException("The model has a not supported update function.");
		}
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
			//it.imt.erode.crn.ChemicalReactionNetwork.BooleanValueBN
			return new FalseUpdateFunction();
		}
		else if(booleanUpdateFunctionXText instanceof it.imt.erode.crn.chemicalReactionNetwork.True){
			return new TrueUpdateFunction();
		}
		else  if(booleanUpdateFunctionXText instanceof it.imt.erode.crn.chemicalReactionNetwork.NotBooleanExpr){
			BoolExpr left = ((it.imt.erode.crn.chemicalReactionNetwork.NotBooleanExpr)booleanUpdateFunctionXText).getLeft();
			return new NotBooleanUpdateFunction(visitUpdateFunction(left,out,bwOut));
		}
		else if(booleanUpdateFunctionXText instanceof it.imt.erode.crn.chemicalReactionNetwork.NEqBooleanExpr){
			IUpdateFunction visitedLeft = visitUpdateFunction(((it.imt.erode.crn.chemicalReactionNetwork.EqBooleanExpr) booleanUpdateFunctionXText).getLeft(),out,bwOut);
			IUpdateFunction visitedRight = visitUpdateFunction(((it.imt.erode.crn.chemicalReactionNetwork.EqBooleanExpr) booleanUpdateFunctionXText).getRight(),out,bwOut);
			return new BooleanUpdateFunctionExpr(visitedLeft,visitedRight,BooleanConnector.NEQ);
		}
		else if(booleanUpdateFunctionXText instanceof it.imt.erode.crn.chemicalReactionNetwork.EqBooleanExpr){
			IUpdateFunction visitedLeft = visitUpdateFunction(((it.imt.erode.crn.chemicalReactionNetwork.EqBooleanExpr) booleanUpdateFunctionXText).getLeft(),out,bwOut);
			IUpdateFunction visitedRight = visitUpdateFunction(((it.imt.erode.crn.chemicalReactionNetwork.EqBooleanExpr) booleanUpdateFunctionXText).getRight(),out,bwOut);
			return new BooleanUpdateFunctionExpr(visitedLeft,visitedRight,BooleanConnector.EQ);
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
		else if(booleanUpdateFunctionXText instanceof it.imt.erode.crn.chemicalReactionNetwork.XorBooleanExpr){
			IUpdateFunction visitedLeft = visitUpdateFunction(((it.imt.erode.crn.chemicalReactionNetwork.XorBooleanExpr) booleanUpdateFunctionXText).getLeft(),out,bwOut);
			IUpdateFunction visitedRight = visitUpdateFunction(((it.imt.erode.crn.chemicalReactionNetwork.XorBooleanExpr) booleanUpdateFunctionXText).getRight(),out,bwOut);
			return new BooleanUpdateFunctionExpr(visitedLeft,visitedRight,BooleanConnector.XOR);
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
			return BasicConstraintComparator.GT;
		}
		else if(comp.equals("<")){
			return BasicConstraintComparator.LT;
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
		if(mec.getModelDefKind().equals(ModelDefKind.BOOLEAN)||mec.getModelDefKind().equals(ModelDefKind.BOOLEANMV) || mec.getModelDefKind().equals(ModelDefKind.BOOLEANIMPORT)){
			boolean printBooleanNetwork=false;
			GUIBooleanNetworkImporter bnImporter = new GUIBooleanNetworkImporter(mec.getModelDefKind().equals(ModelDefKind.BOOLEANMV),consoleOut,bwOut,msgVisualizer,mec.getRealSortMVNet());
			if(mec.getModelDefKind().equals(ModelDefKind.BOOLEANIMPORT)) {
				cl = new CRNReducerCommandLine(commandsReader,true);
				//cl.setImporterOfSupportedNetworks(new ImporterOfSupportedNetworksWithCRNGUI());
				cl.setDataOutputHandler(guidog);
				cl.setMessageDialogShower(msgVisualizer);
				cl.setTerminator(console.getTerminator());
				BasicModelElementsCollector bMec = ((CRNReducerCommandLine)cl).importBooleanModel(mec.getImportString(), true,false,consoleOut,bwOut);
				mec.setInitialConcentrations(bMec.getInitialConcentrations());
				booleanUpdateFunctions=bMec.getBooleanUpdateFunctions();
				mec.setUserPartition(bMec.getUserPartition());
				//initializeUpdateFunctions(booleanNetwork,guessPrepartitionOnInputs,modelConverter.getErodeUpdateFunctions());
			}
			//try {
				bnImporter.importBooleanNetwork(true, printBooleanNetwork, true, mec.getModelName(), mec.getInitialConcentrations(), booleanUpdateFunctions, mec.getUserPartition(), consoleOut);
//			} catch (IOException e) {
//				CRNReducerCommandLine.println(consoleOut,bwOut, "Unhandled errors arised while executing the commands.");
//				CRNReducerCommandLine.printStackTrace(consoleOut,bwOut,e);
//				failed=true;
//			}
			
			cl = new BooleanNetworkCommandLine(commandsReader,bnImporter.getBooleanNetwork(),bnImporter.getInitialPartition(),true);
			cl.setDataOutputHandler(guidog);
			cl.setMessageDialogShower(msgVisualizer);
			cl.setTerminator(console.getTerminator());
			
		}
		else if(mec.getModelDefKind().equals(ModelDefKind.ProbProg)) {
			GUIProbProgImporter probProgImporter = new GUIProbProgImporter(consoleOut,bwOut,msgVisualizer);
			
			ODEorNET format=ODEorNET.ODE;
			boolean printModel=false;
			try {
			probProgImporter.importProbProg(true,printModel,true,mec.getModelName(),
					mec.getProbProgParameters(),
					mec.getParsedConditionsProbProg(),
					mec.getReactionsProbProg(),
					mec.getInitialConcentrations(),mec.getUserPartition(),format, consoleOut);
			} catch (IOException e1) {
				CRNReducerCommandLine.println(consoleOut,bwOut, "Unhandled errors arised while loading the prob progexecuting the commands.");
				CRNReducerCommandLine.printStackTrace(consoleOut,bwOut,e1);
				failed=true;
			}
			
			cl = new ProbProgCommandLine(commandsReader,probProgImporter.getCRN(),probProgImporter.getInitialPartition(), true,
					probProgImporter.getProbProgParameters(),
					probProgImporter.getReactionsOfEachGuard(), probProgImporter.getReactionsOfEachClause()
					, consoleOut, bwOut
					);
			cl.setDataOutputHandler(guidog);
			cl.setMessageDialogShower(msgVisualizer);
			cl.setTerminator(console.getTerminator());
		}
		else if(mec.getModelDefKind().equals(ModelDefKind.PROBPROGIMPORT)) {
			
			String command=mec.getImportString();
			String[] parameters = CRNReducerCommandLine.getParameters(command);
			if(parameters==null){
				CRNReducerCommandLine.println(consoleOut,bwOut,"Problems in loading the parameters of command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(consoleOut,bwOut,"Type --help for usage instructions.");
				return null;
			}
			String fileName=null;
			for(int p=0;p<parameters.length;p++){
				if(parameters[p].startsWith("fileIn=>")){
					if(parameters[p].length()<="fileIn=>".length()){
						CRNReducerCommandLine.println(consoleOut,bwOut,"Please, specify the name of the file to read. ");
						CRNReducerCommandLine.println(consoleOut,bwOut,"I skip this command: "+command);
						return null;
					}
					fileName = parameters[p].substring("fileIn=>".length(), parameters[p].length());
				}
				else if(parameters[p].equals("")){
					continue;
				}
				else{
					CRNReducerCommandLine.println(consoleOut,bwOut,"Unknown parameter \""+parameters[p]+"\" in command "+command+". I skip this command."); if(CommandsReader.PRINTHELPADVICE) CRNReducerCommandLine.println(consoleOut,bwOut,"Type --help for usage instructions.");
					return null;
				}
			}
			if(fileName ==null || fileName.equals("")){
				CRNReducerCommandLine.println(consoleOut,bwOut,"Please, specify the file to be loaded. ");
				CRNReducerCommandLine.println(consoleOut,bwOut,"I skip this command: "+command);
				return null;
			}
			
			
			GUIProbProgImporter probProgImporter = new GUIProbProgImporter(fileName,consoleOut,bwOut,msgVisualizer);
			//boolean printModel=false;
			ODEorNET format=ODEorNET.ODE;
			try {
				probProgImporter.importProbProgNetwork(false, false, true,format);
			} catch (IOException e) {
				CRNReducerCommandLine.println(consoleOut,bwOut, "Unhandled errors arised while executing the commands.");
				CRNReducerCommandLine.printStackTrace(consoleOut,bwOut,e);
				failed=true;
			}
			if(!failed) {
				cl = new ProbProgCommandLine(commandsReader,probProgImporter.getCRN(),probProgImporter.getInitialPartition(), true,
						probProgImporter.getProbProgParameters(),
						probProgImporter.getReactionsOfEachGuard(), probProgImporter.getReactionsOfEachClause()
						, consoleOut, bwOut
						);
				cl.setDataOutputHandler(guidog);
				cl.setMessageDialogShower(msgVisualizer);
				cl.setTerminator(console.getTerminator());
			}
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
		else if(mec.getModelDefKind().equals(ModelDefKind.IMPORT) || mec.getModelDefKind().equals(ModelDefKind.IMPORTFOLDER) || mec.getModelDefKind().equals(ModelDefKind.BOOLEANIMPORTFOLDER)|| mec.getModelDefKind().equals(ModelDefKind.BOOLEANIMPORT)){
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
				else if (mec.getModelDefKind().equals(ModelDefKind.BOOLEANIMPORTFOLDER)){
					((CRNReducerCommandLine)cl).importBooleanModelsFromFolder(mec.getImportFolderString(), true,ignoreCommands,consoleOut,bwOut);
				}
//				else if(mec.getModelDefKind().equals(ModelDefKind.BOOLEANIMPORT)) {
//					((CRNReducerCommandLine)cl).importBooleanModel(mec.getImportString(), true,ignoreCommands,consoleOut,bwOut);
//				}
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
