package it.imt.erode.crn.ui.perspective.wizards;

import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

public class NewERODEProjectWizard extends BasicNewProjectResourceWizard{

	//private WizardNewERODEProjectCreationPage page;
	
	public NewERODEProjectWizard(){
		super();
		setWindowTitle("ERODE Project");
	}
	
	@Override
	public void addPages() {
		super.addPages();
		this.getPages()[0].setDescription("Create a new ERODE project");
		this.getPages()[0].setTitle("ERODE project");
		//WizardNewProjectCreationPage page = (WizardNewProjectCreationPage)this.getPages()[0];
		/*page = new WizardNewERODEProjectCreationPage("basicNewProjectPage");
        addPage(page);*/
	}
}
