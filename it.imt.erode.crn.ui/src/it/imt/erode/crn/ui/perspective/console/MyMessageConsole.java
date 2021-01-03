package it.imt.erode.crn.ui.perspective.console;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.console.MessageConsole;
import it.imt.erode.commandline.Terminator;


public class MyMessageConsole extends MessageConsole{

	
	private Terminator terminator;
	private ConsoleActions consoleActions;
	
	public MyMessageConsole(String name, ImageDescriptor imageDescriptor) {
		super(name, imageDescriptor);
		terminator=new Terminator();
	}

	public MyMessageConsole(String name, ImageDescriptor imageDescriptor, boolean autoLifecycle) {
		super(name, imageDescriptor, autoLifecycle);
		terminator=new Terminator();
	}

	public MyMessageConsole(String name, String consoleType, ImageDescriptor imageDescriptor, boolean autoLifecycle) {
		super(name, consoleType, imageDescriptor, autoLifecycle);
		terminator=new Terminator();
	}

	public MyMessageConsole(String name, String consoleType, ImageDescriptor imageDescriptor, String encoding,boolean autoLifecycle) {
		super(name, consoleType, imageDescriptor, encoding, autoLifecycle);
		terminator=new Terminator();
	}

	public Terminator getTerminator() {
		return terminator;
	}

	public void setTerminationFlag() {
		terminator.setTerminationFlag();
		if(consoleActions!=null){
			consoleActions.disableButtonProgrammatically();
		}
		
	}

	public void setConsoleActions(ConsoleActions consoleActions) {
		this.consoleActions= consoleActions;
		if(terminator.hasToTerminate()){
			consoleActions.disableButtonProgrammatically();
		}
	}
	
	/*
	private CRNReducerCommandLine cl;
	public CRNReducerCommandLine getCRNReducerCommandline() {
		return cl;
	}

	public void setCRNReducerCommandline(CRNReducerCommandLine cl) {
		this.cl = cl;
	}
	*/

}
