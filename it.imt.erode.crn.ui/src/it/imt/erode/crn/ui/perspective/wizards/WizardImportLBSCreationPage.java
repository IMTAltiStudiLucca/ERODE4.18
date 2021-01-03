package it.imt.erode.crn.ui.perspective.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

public class WizardImportLBSCreationPage extends WizardImportBNGCreationPage {

	protected WizardImportLBSCreationPage(String name, IWorkbench aWorkbench, IStructuredSelection selection) {
		super(name, aWorkbench, selection);
	}

	protected WizardImportLBSCreationPage(IWorkbench aWorkbench, IStructuredSelection selection) {
		super(aWorkbench, selection);
	}
	
	@Override
	protected String getImportCommand(String absolutePathInputFile){
		return "importLBS({fileIn=>"+absolutePathInputFile+",ignoreCommands=>true})";
	}
	
	@Override
	protected String getExtension(){
		return ".lbs";
	}
	
	@Override
	protected String getOutputFormat(){
		return "RN";
	}
	
}
