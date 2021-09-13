package it.imt.erode.crn.ui.perspective.wizards;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.internal.ide.IIDEHelpContextIds;

@SuppressWarnings("restriction")
public class WizardNewERODEProjectCreationPage extends WizardNewProjectCreationPage {

	public WizardNewERODEProjectCreationPage(String pageName) {
		super(pageName);
		setTitle("New ERODE project");
		setDescription("Create a new ERODE project");
	}
	
	
	@Override
	public void createControl(Composite parent) {
		// TODO Auto-generated method stub
		//super.createControl(parent);

		Composite composite = new Composite(parent, SWT.NULL);


		initializeDialogUnits(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite,IIDEHelpContextIds.NEW_PROJECT_WIZARD_PAGE);

		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));

		/*createProjectNameGroup(composite);
		locationArea = new ProjectContentsLocationArea(getErrorReporter(), composite);
		if(initialProjectFieldValue != null) {
			locationArea.updateProjectName(initialProjectFieldValue);
		}

		// Scale the button based on the rest of the dialog
		setButtonLayoutData(locationArea.getBrowseButton());*/

		setPageComplete(validatePage());
		// Show description on opening
		setErrorMessage(null);
		setMessage(null);
		setControl(composite);
		Dialog.applyDialogFont(composite);
	}
	

}
