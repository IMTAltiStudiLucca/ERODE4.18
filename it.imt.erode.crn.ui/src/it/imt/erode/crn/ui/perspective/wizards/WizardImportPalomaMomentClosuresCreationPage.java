package it.imt.erode.crn.ui.perspective.wizards;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;

public class WizardImportPalomaMomentClosuresCreationPage extends WizardImportBNGCreationPage {

	protected WizardImportPalomaMomentClosuresCreationPage(String name, IWorkbench aWorkbench, IStructuredSelection selection) {
		super(name, aWorkbench, selection);
	}

	protected WizardImportPalomaMomentClosuresCreationPage(IWorkbench aWorkbench, IStructuredSelection selection) {
		super(aWorkbench, selection);
	}
	
	@Override
	protected String getImportCommand(String absolutePathInputFile){
		return "importPalomaMomentClosures({fluidFlow=>"+absolutePathInputFile+"})";
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
