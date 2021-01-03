package it.imt.erode.crn.ui.perspective.wizards;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.dialogs.FileSystemElement;
import org.eclipse.ui.internal.wizards.datatransfer.WizardFileSystemResourceImportPage1;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.CommandsReader;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.crn.MyParserUtil;
import it.imt.erode.crn.ui.handler.MyCRNProgamExecutorWorker;
import it.imt.erode.crn.ui.handler.ThreadBuffer;
import it.imt.erode.crn.ui.perspective.console.MyConsoleUtil;
import it.imt.erode.crn.ui.perspective.console.MyMessageConsole;
import it.imt.erode.crn.ui.perspective.dialogs.MessageDialogShower;

@SuppressWarnings({ "restriction", "unchecked" })
public class WizardImportBNGCreationPage extends WizardFileSystemResourceImportPage1 {

	protected WizardImportBNGCreationPage(String name, IWorkbench aWorkbench, IStructuredSelection selection) {
		super(name, aWorkbench, selection);
	}

	protected WizardImportBNGCreationPage(IWorkbench aWorkbench, IStructuredSelection selection) {
		super(aWorkbench, selection);
		this.selectedTypes=new ArrayList<String>();
		selectedTypes.add(getExtension());
	}
	
	protected String getImportCommand(String absolutePathInputFile){
		return "importBNG({fileIn=>"+absolutePathInputFile+",compactNames=>true,writeFileWithSpeciesNameCorrespondences=>true})";
	}
	
	protected String getExtension(){
		return ".net";
	}
	
	protected String getOutputFormat(){
		return null;
	}

	@Override
	public boolean finish() {
		if (!ensureSourceIsValid()) {
			return false;
		}

		saveWidgetValues();

		IPath resourcePath = getResourcePath();
		IWorkspace workspace= ResourcesPlugin.getWorkspace();
		//IPath location= Path.fromOSString(myfile.getAbsolutePath());
		String proj = resourcePath.toString();
		String remainingInternalPath = "";
		int posOfSep = proj.indexOf(File.separator, 1);
		//int posOfSep = proj.indexOf(File.separator);
		if(posOfSep!=-1){
			remainingInternalPath=proj.substring(posOfSep);
			proj=proj.substring(0,posOfSep);
		}
		IProject project = workspace.getRoot().getProject(proj);
		URI projectURI = project.getLocationURI();
		String projectPath=projectURI.getPath();
		String targetPath = projectPath+remainingInternalPath;
		/*IFile file= workspace.getRoot().getFileForLocation(resourcePath);
  	    IProject project = file.getProject();*/

		List<String> commands = new ArrayList<>();

		String name ="";
		Iterator<FileSystemElement> resourcesEnum = getSelectedResources().iterator();
		//List<File> fileSystemObjects = new ArrayList<File>();
		while (resourcesEnum.hasNext()) {
			File f = (File)(resourcesEnum.next().getFileSystemObject());
			//fileSystemObjects.add(f);
			commands.add(getImportCommand(f.getAbsolutePath()));//commands.add("importBNG({fileIn=>"+f.getAbsolutePath()+",compactNames=>true})");
			String outFile = f.getName().replace(getExtension(), "")+".ode";
			name = f.getName().replace(getExtension(), "");
			if(name.contains(File.separator)){
				name=name.substring(name.lastIndexOf(File.separator)+1);
			}
			if(getOutputFormat()==null){
				commands.add("write({fileOut=>"+MyParserUtil.computeFileName(outFile, targetPath)+"})");
			}
			else{
				commands.add("write({fileOut=>"+MyParserUtil.computeFileName(outFile, targetPath)+",format=>"+getOutputFormat()+"})");
			}
			
		}

		MyMessageConsole console = MyConsoleUtil.generateConsole(name);
		MessageConsoleStream consoleOut = console.newMessageStream();
		String welcome = MyConsoleUtil.computeWelcome(consoleOut);
		//consoleOut.println(welcome);
		String fileName = MyParserUtil.computeFileName(console.getName().replace('/', '-'), targetPath).replace(' ', '_')+".txt";
	     BufferedWriter bwOut=null;
	     try {
	    	 bwOut = new BufferedWriter(new FileWriter(fileName));
	    	 //bwOut.write(welcome+"\n");
	     } catch (IOException e1) {
	    	 CRNReducerCommandLine.printStackTrace(consoleOut, bwOut, e1);
	     }
	     
	     CRNReducerCommandLine.println(consoleOut, bwOut, welcome);
		
		CommandsReader commandsReader = new CommandsReader(commands,consoleOut,bwOut);
		CRNReducerCommandLine cl = new CRNReducerCommandLine(commandsReader,true);
		IMessageDialogShower msgVisualizer = new MessageDialogShower(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
		cl.setMessageDialogShower(msgVisualizer);



		ThreadBuffer<MyCRNProgamExecutorWorker> threadsBuffer = new ThreadBuffer<MyCRNProgamExecutorWorker>(1);
		MyCRNProgamExecutorWorker worker = new MyCRNProgamExecutorWorker(cl,consoleOut,bwOut,project,console,threadsBuffer);
		worker.start();


		/*for (File file : fileSystemObjects) {
			System.out.println(file.getAbsolutePath());
			//commandsReader.addToHead(importString);
			cl = new CRNReducerCommandLine(commandsReader,true);
			//cl.setImporterOfSupportedNetworks(new ImporterOfSupportedNetworksWithCRNGUI());
			cl.setDataOutputHandler(guidog);
			cl.setMessageDialogShower(msgVisualizer);
			try {
				boolean ignoreCommands=true;//Note that there are no commands, because I forbid to use load 
				cl.importModel(mec.getImportString(), true,ignoreCommands,consoleOut);
				ICRN crn = cl.getCRN();
				if(canSynchEditor && mec.isSyncEditor()){
					//replaceText(mec, crn);
					replaceText(mec, crn);
				}
			} catch (UnsupportedFormatException e) {
				CRNReducerCommandLine.println(consoleOut, "Unhandled errors arised while executing the commands.");
				CRNReducerCommandLine.printStackTrace(consoleOut,e);
				failed=true;
			} catch (BadLocationException e) {
				CRNReducerCommandLine.println(consoleOut,"Importing of model in the text editor failed.");
				CRNReducerCommandLine.printStackTrace(consoleOut, e);
			}
		}*/

		//return super.finish();
		return true;
	}

	@Override
	protected void createOptionsGroupButtons(Group optionsGroup) {

	}
	@Override
	protected void createOptionsGroup(Composite parent) {
		
	}

	protected String getStoreSourceNamesID(){
		return "WizardFileSystemResourceImportPage1.STORE_SOURCE_NAMES_ID";
	}
	
	//private final static String STORE_SOURCE_NAMES_ID = "WizardFileSystemResourceImportPage1.STORE_SOURCE_NAMES_ID";
	@Override
	protected void restoreWidgetValues() {
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			String[] sourceNames = settings.getArray(getStoreSourceNamesID());
			if (sourceNames == null) {
				return; // ie.- no values stored, so stop
			}

			// set filenames history
			for (int i = 0; i < sourceNames.length; i++) {
				sourceNameField.add(sourceNames[i]);
			}
			updateWidgetEnablements();
		}
	}

	/**
	 *	Answer the directory name specified as being the import source.
	 *	Note that if it ends with a separator then the separator is first
	 *	removed so that java treats it as a proper directory
	 */
	private String getSourceDirectoryName() {
		return getSourceDirectoryName(this.sourceNameField.getText());
	}
	/**
	 *	Answer the directory name specified as being the import source.
	 *	Note that if it ends with a separator then the separator is first
	 *	removed so that java treats it as a proper directory
	 */
	private String getSourceDirectoryName(String sourceName) {
		IPath result = new Path(sourceName.trim());

		if (result.getDevice() != null && result.segmentCount() == 0) {
			result = result.addTrailingSeparator();
		} else {
			result = result.removeTrailingSeparator();
		}

		return result.toOSString();
	}

	@Override
	protected void saveWidgetValues() {
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			// update source names history
			String[] sourceNames = settings.getArray(getStoreSourceNamesID());
			if (sourceNames == null) {
				sourceNames = new String[0];
			}

			sourceNames = addToHistory(sourceNames, getSourceDirectoryName());
			settings.put(getStoreSourceNamesID(), sourceNames);
		}
	}
	
	
}
