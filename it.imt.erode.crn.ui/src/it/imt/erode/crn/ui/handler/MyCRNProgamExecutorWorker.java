package it.imt.erode.crn.ui.handler;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.CommandsReader;
import it.imt.erode.commandline.ICommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.crn.ModelElementsCollector;
import it.imt.erode.crn.symbolic.constraints.IConstraint;
import it.imt.erode.crn.ui.perspective.console.MyConsoleUtil;
import it.imt.erode.crn.ui.perspective.console.MyMessageConsole;
import it.imt.erode.crn.ui.perspective.plot.GUIDataOutputHandler;

public class MyCRNProgamExecutorWorker extends Thread {

	MessageConsoleStream consoleOut;
	BufferedWriter bwOut;
	ICommandLine cl;
	private IProject project;
	private MyMessageConsole console;
	private ThreadBuffer<MyCRNProgamExecutorWorker> threadsBuffer;
	private ModelElementsCollector mec;
	private IMessageDialogShower msgVisualizer;
	private List<IConstraint> constraints;
	private CommandsReader commandsReader;
	private GUIDataOutputHandler guidog;
	private LinkedHashMap<String, IUpdateFunction> booleanUpdateFunctions;
	
	public MyCRNProgamExecutorWorker(ICommandLine cl, MessageConsoleStream consoleOut, BufferedWriter bwOut,IProject project, MyMessageConsole console, ThreadBuffer<MyCRNProgamExecutorWorker> threadsBuffer) {
		this.cl=cl;
		this.consoleOut=consoleOut;
		this.bwOut=bwOut;
		this.project=project;
		this.console=console;
		this.threadsBuffer = threadsBuffer;
	}
	

	public MyCRNProgamExecutorWorker(MessageConsoleStream consoleOut, BufferedWriter bwOut, IProject project, MyMessageConsole console,
			ThreadBuffer<MyCRNProgamExecutorWorker> threadsBuffer, ModelElementsCollector mec, IMessageDialogShower msgVisualizer,
			List<IConstraint> constraints, LinkedHashMap<String, IUpdateFunction> booleanUpdateFunctions, CommandsReader commandsReader, GUIDataOutputHandler guidog) {
		this.consoleOut=consoleOut;
		this.bwOut=bwOut;
		this.project=project;
		this.console=console;
		this.threadsBuffer = threadsBuffer;
		this.mec=mec;
		this.msgVisualizer=msgVisualizer;
		this.constraints=constraints;
		this.commandsReader=commandsReader;
		this.guidog=guidog;
		
		this.booleanUpdateFunctions=booleanUpdateFunctions;
		this.threadsBuffer = threadsBuffer;
	}

	@Override
	public void run() {
		threadsBuffer.addWorker(this);
		System.gc();
		if(cl==null){
			try {
				cl = MyCRNProgramExecutor.loadModel(mec, msgVisualizer, consoleOut, bwOut,constraints,booleanUpdateFunctions, commandsReader, guidog, console);
			} catch (Exception e) {
				CRNReducerCommandLine.println(consoleOut,bwOut, "Unhandled errors arised while loading the model.");
				CRNReducerCommandLine.printStackTrace(consoleOut,bwOut,e);
				//cl.getTerminator().setTerminationFlag();
				console.setTerminationFlag();
			}
		}
		if(cl!=null){
			try {
				cl.executeCommands(true,consoleOut,bwOut);
			} catch (Exception e) {
				CRNReducerCommandLine.println(consoleOut,bwOut, "Unhandled errors arised while executing the commands.");
				CRNReducerCommandLine.printStackTrace(consoleOut,bwOut,e);
				//cl.getTerminator().setTerminationFlag();
				console.setTerminationFlag();
			}
			catch (OutOfMemoryError e) {
				CRNReducerCommandLine.println(consoleOut,bwOut, "An out of memory exception arised while executing the commands.");
				CRNReducerCommandLine.printStackTrace(consoleOut,bwOut,e);
				console.setTerminationFlag();
			}
		}
		try {
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			CRNReducerCommandLine.printStackTrace(consoleOut,bwOut,e);
			//cl.getTerminator().setTerminationFlag();
			console.setTerminationFlag();
		}
		cl=null;
		String goodBye = MyConsoleUtil.computeGoodbye(consoleOut);
		CRNReducerCommandLine.println(consoleOut, bwOut, goodBye);
//		consoleOut.println(goodBye);
//		try {
//			bwOut.write(goodBye+"\n");
//		} catch (IOException e) {
//			CRNReducerCommandLine.printStackTrace(consoleOut, bwOut, e);
//		}
		console.setTerminationFlag();
		if(bwOut!=null){
			try {
				bwOut.close();
			} catch (IOException e) {
				CRNReducerCommandLine.printStackTrace(consoleOut, bwOut, e);
			}
		}
		threadsBuffer.removeFirstWorker();
	}
	
}