package it.imt.erode.crn.ui.perspective.dialogs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import it.imt.erode.commandline.DialogType;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.importing.AbstractImporter;

public class MessageDialogShower implements IMessageDialogShower {

	private Shell parentShell;
	
	public MessageDialogShower(Shell parentShell){
		this.parentShell=parentShell;
	}
	
	@Override
	//public void showMessage(String message, String optionsLabel, ArrayList<String> options){
	public void openMissingZ3LibrariesDialog(String link32, String link64, String link32Short, String link64Short, ArrayList<String> paths,String OS){
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				MissingNativeLibrariesDialog dialog = new MissingNativeLibrariesDialog(parentShell,link32,link64,link32Short,link64Short, paths,OS);
				dialog.open();
			}
		});
	}
	
	@Override
	public void openMissingRequiredLibraryDialog(String downloadMessage, String locateMessage, String fileWithLibraryFileLocation, String downloadPath, String downloadPathShort, boolean folder){
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				MissingRequiredLibraryDialog dialog = new MissingRequiredLibraryDialog(parentShell,downloadPath,downloadPathShort,downloadMessage,locateMessage,folder);
				dialog.open();
				System.out.println("The library is in: "+dialog.getLibraryPath());
				BufferedWriter bw=null;
				try {
					AbstractImporter.createParentDirectories(fileWithLibraryFileLocation);
					bw = new BufferedWriter(new FileWriter(new File(fileWithLibraryFileLocation)));
					bw.write(dialog.getLibraryPath()+"\n");
				} catch (IOException e) {
					e.printStackTrace();
				}
				finally{
					if(bw!=null){
						try {
							bw.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}

				}
				/*String path = dialog.getLibraryPath();
				if(path==null || path==""){
					return "UNSPECIFIED";
				}
				else{
					return path;
				}*/
			}
		});
	}

	@Override
	public void openSimpleDialog(String message,DialogType dialogType) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				String title = null;
				String[] buttons = null;
				int dt=-1;
				if(dialogType.equals(DialogType.Question)){
					title = "Question";
					dt = MessageDialog.QUESTION;
					buttons = new String[]{"Yes","No"};
				}
				else if(dialogType.equals(DialogType.Warning)){
					title = "Warning";
					dt = MessageDialog.WARNING;
					buttons = new String[]{"Ok"};
				}
				else if(dialogType.equals(DialogType.Error)){
					title = "Error";
					dt = MessageDialog.ERROR;
					buttons = new String[]{"Ok"};
				}
				MessageDialog dialog = new MessageDialog(parentShell, title, null, message, dt, buttons, 0);
				if(dialog!=null){
					dialog.open();
				}
				/*MessageBox mb = new MessageBox(parentShell);
				mb.setText("Action required");
				mb.setMessage(message);
				mb.open();*/
			}
		});
		
	}
	
	/*
	@Override
	public void openMissingMatlabLibraryDialog() {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				String title = "Missing Matlab library";
				String[] buttons = new String[]{"Ok"};
				int dt=-1;
				MessageDialog dialog = new MessageDialog(parentShell, title, null, message, dt, buttons, 0);
				if(dialog!=null){
					dialog.open();
				}
			}
		});
	}
	*/
	
}
