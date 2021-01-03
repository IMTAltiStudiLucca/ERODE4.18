package it.imt.erode.javalibrarypath;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.DialogType;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.utopic.MatlabODEPontryaginExporter;
import it.imt.erode.utopic.vnodelp.VNODELPExporter;

public class JavaLibraryPathHandler {
	
	public static final String UNSPECIFIEDPATH = "UNSPECIFIED";
	
	public static String getRequiredLibrary(String downloadMessage, String locateMessage,String fileWithLibraryFileLocation, String downloadPath, String downloadPathShort,MessageConsoleStream out, BufferedWriter bwOut,IMessageDialogShower messageDialogShower) throws IOException {
		//downloadMessage
		//downloadPath
		//downloadPathShort

		File f = new File(fileWithLibraryFileLocation);
		//System.out.println(f.getAbsolutePath());
		
		if(f.exists()){
			BufferedReader br = new BufferedReader(new FileReader(f));
			String path = br.readLine();
			br.close();
			File library = new File(path);
			if(library.exists()){
				return path;
			}
		}
		if(messageDialogShower!=null){
			messageDialogShower.openMissingRequiredLibraryDialog(downloadMessage, locateMessage,fileWithLibraryFileLocation,downloadPath,downloadPathShort,true);
			f = new File(fileWithLibraryFileLocation);
			if(f.exists()){
				BufferedReader br = new BufferedReader(new FileReader(f));
				String path = br.readLine();
				br.close();
				if(path!=null){
					File library = new File(path);
					if(library.exists()){
						return path;
					}
				}
			}
		}
		else{
			CRNReducerCommandLine.println(out, bwOut, "Please download the required library ("+downloadPathShort+") from: "+downloadPath+" and store its path in the first line of "+fileWithLibraryFileLocation);
		}
		return UNSPECIFIEDPATH;

	}
	
	public static boolean handleMatlabPath(String command, MessageConsoleStream out, BufferedWriter bwOut,IMessageDialogShower messageDialogShower) {
		boolean containsMatlabPath = false;
		try {
			containsMatlabPath = JavaLibraryPathHandler.checkForPathContaining("MATLAB");
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e2) {
			CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Problems in checking if the path \"matlabroot/bin/<arch>\" appears in the user paths.\nError message:\n"+e2.getMessage(),DialogType.Warning);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e2);
		}
		
		if(!containsMatlabPath){
			String matlabPath=null;
			try {
				matlabPath=JavaLibraryPathHandler.getRequiredLibrary(null, MatlabODEPontryaginExporter.MATLABROOT_ARCH_LOCATEMESSAGE, MatlabODEPontryaginExporter.FILEWITHMATLABROOT_ARCH, null, null,out,bwOut,messageDialogShower);
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Exception while retrieving the path \"matlabroot/bin/<arch>\"");
				//CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
				CRNReducerCommandLine.printExceptionShort(out, bwOut, e);
				return false;
			}
			if(matlabPath==null || matlabPath.equals(UNSPECIFIEDPATH)){
				CRNReducerCommandLine.println(out,bwOut,"Could not retrieve the path \"matlabroot/bin/<arch>\"");
				//CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
				return false;
			}
			try {
				JavaLibraryPathHandler.addLibraryPath(matlabPath);
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				//CRNReducerCommandLine.printWarning(out, bwOut,true,messageDialogShower,"Problems in adding  \"matlabroot/bin/<arch>\" to the java library path.\nError message:\n"+e.getMessage(),DialogType.Error);
				//CRNReducerCommandLine.printStackTrace(out,bwOut,e);
				CRNReducerCommandLine.println(out,bwOut,"Could not add \"matlabroot/bin/<arch>\" to the java library path");
				//CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
				return false;
			}
		}
		
		return true;
	}
	
	public static String handleVNODEPath(String command, MessageConsoleStream out, BufferedWriter bwOut,IMessageDialogShower messageDialogShower) {
			String vnodePath=null;
			try {
				vnodePath=JavaLibraryPathHandler.getRequiredLibrary(null, VNODELPExporter.VNODE_LOCATEMESSAGE, VNODELPExporter.FILEWITHVNODEROOT, null, null,out,bwOut,messageDialogShower);
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Exception while retrieving the VNODE path");
				//CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
				CRNReducerCommandLine.printExceptionShort(out, bwOut, e);
				return UNSPECIFIEDPATH;
			}
			if(vnodePath==null || vnodePath.equals(UNSPECIFIEDPATH)){
				CRNReducerCommandLine.println(out,bwOut,"Could not retrieve the path \"matlabroot/bin/<arch>\"");
				//CRNReducerCommandLine.println(out,bwOut,"I skip this command: "+command);
				return UNSPECIFIEDPATH;
			}
		
		return vnodePath;
	}

	public static void addLibraryPath(String pathToAdd) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
	    Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
	    usrPathsField.setAccessible(true);

	    String[] paths = (String[]) usrPathsField.get(null);

	    for (String path : paths) {
	    		//System.out.println(path);
	    		if (path.equals(pathToAdd)) {
	    			return;
	    		}
	    }
	        

	    String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
	    newPaths[newPaths.length - 1] = pathToAdd;
	    usrPathsField.set(null, newPaths);
	}

	public static boolean checkForPathContaining(String s) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		s=s.trim().toLowerCase();
		Field usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
	    usrPathsField.setAccessible(true);

	    String[] paths = (String[]) usrPathsField.get(null);

	    for (String path : paths) {
	    		//System.out.println(path);
	    		String p = path.toLowerCase(); 
	    		if (p.contains(s)) {
	    			return true;
	    		}
	    }
		return false;
	}
	
}
