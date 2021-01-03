package it.imt.erode.commandline;

import java.util.ArrayList;

public interface IMessageDialogShower {

	//public void showMessage(String message, String optionsLabel, ArrayList<String> options);
	public void openMissingZ3LibrariesDialog(String link32, String link64, String link32Short, String link64Short, ArrayList<String> paths,String OS);
	public void openSimpleDialog(String message,DialogType type);
	public void openMissingRequiredLibraryDialog(String downloadMessage, String locateMessage,String fileWithLibraryFileLocation,String downloadPath, String downloadPathShort, boolean folder);
	
	//public void openMissingMatlabLibraryDialog();

}
