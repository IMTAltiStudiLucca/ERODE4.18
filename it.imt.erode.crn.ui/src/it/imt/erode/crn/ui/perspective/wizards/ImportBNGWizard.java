package it.imt.erode.crn.ui.perspective.wizards;

import java.util.List;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.wizards.datatransfer.DataTransferMessages;

//import org.eclipse.ui.wizards.datatransfer.FileSystemImportWizard;
/**
 * This class has been created starting from org.eclipse.ui.wizards.datatransfer.FileSystemImportWizard;
 * @author Andrea Vandin
 *
 */
@SuppressWarnings({ "restriction", "rawtypes" })
public class ImportBNGWizard  extends Wizard implements IImportWizard,org.eclipse.ui.INewWizard  {

	private IWorkbench workbench;

    private IStructuredSelection selection;

    private WizardImportBNGCreationPage mainPage;

    /**
     * Creates a wizard for importing resources into the workspace from
     * the file system.
     */
    public ImportBNGWizard() {
        IDialogSettings workbenchSettings = WorkbenchPlugin.getDefault().getDialogSettings();
        IDialogSettings section = workbenchSettings
                .getSection("FileSystemImportWizard");//$NON-NLS-1$
        if (section == null) {
			section = workbenchSettings.addNewSection("FileSystemImportWizard");//$NON-NLS-1$
		}
        setDialogSettings(section);
    }

    /* (non-Javadoc)
     * Method declared on IWizard.
     */
    @Override
	public void addPages() {
        super.addPages();
        mainPage = new WizardImportBNGCreationPage(workbench, selection);
        addPage(mainPage);
    }


    /* (non-Javadoc)
     * Method declared on IWorkbenchWizard.
     */
    @Override
	public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        this.workbench = workbench;
        this.selection = currentSelection;

        List selectedResources = IDE.computeSelectedResources(currentSelection);
        if (!selectedResources.isEmpty()) {
            this.selection = new StructuredSelection(selectedResources);
        }

        setWindowTitle(DataTransferMessages.DataTransfer_importTitle);
        setDefaultPageImageDescriptor(IDEWorkbenchPlugin.getIDEImageDescriptor("wizban/importdir_wiz.png"));//$NON-NLS-1$
        setNeedsProgressMonitor(true);
    }

    /* (non-Javadoc)
     * Method declared on IWizard.
     */
    @Override
	public boolean performFinish() {
        return mainPage.finish();
    }
	
}
