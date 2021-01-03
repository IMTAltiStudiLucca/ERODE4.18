package it.imt.erode.crn.ui.perspective.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

public class WizardImportMatlabODEsCreationPage extends WizardImportBNGCreationPage {

	protected WizardImportMatlabODEsCreationPage(String name, IWorkbench aWorkbench, IStructuredSelection selection) {
		super(name, aWorkbench, selection);
	}

	protected WizardImportMatlabODEsCreationPage(IWorkbench aWorkbench, IStructuredSelection selection) {
		super(aWorkbench, selection);
	}
	
	@Override
	protected String getImportCommand(String absolutePathInputFile){
		return "importMatlab({fileIn=>"+absolutePathInputFile+",polynomialODEs=>false})";
	}
	
	@Override
	protected String getExtension(){
		return ".m";
	}
	
	@Override
	protected String getOutputFormat(){
		return "ODE";
	}
	
}
