package it.imt.erode.crn.ui.perspective.wizards;

import org.eclipse.core.resources.IFile;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.wizards.newresource.BasicNewFileResourceWizard;

public class NewERODEFileWizard  extends BasicNewFileResourceWizard {//extends Wizard implements INewWizard {

	private WizardNewERODEFileCreationPage page;
	
	public NewERODEFileWizard() {
		super();
		setWindowTitle("ERODE File");
	}

	@Override
    public void addPages() {
        page = new WizardNewERODEFileCreationPage(selection);
        addPage(page);
    }

	/*@Override
    public boolean performFinish() {
        IFile file = page.createNewFile();
        if (file != null){
        	 selectAndReveal(file);
        	return true;
        }
        else{
        	return false;
        }
    }*/
	 
	 /* (non-Javadoc)
     * Method declared on IWizard.
     */
    @Override
	public boolean performFinish() {
        IFile file = page.createNewFile();
        if (file == null) {
			return false;
		}

        selectAndReveal(file);

        // Open editor on new file.
        IWorkbenchWindow dw = getWorkbench().getActiveWorkbenchWindow();
        try {
            if (dw != null) {
                IWorkbenchPage page = dw.getActivePage();
                if (page != null) {
                    IDE.openEditor(page, file, true);
                }
            }
        } catch (PartInitException e) {
        	
            //DialogUtil.openError(dw.getShell(), ResourceMessages.FileResource_errorMessage,e.getMessage(), e);
        	e.printStackTrace();
        }

        return true;
    }



}
