package it.imt.erode.commandline;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;

import java.io.IOException;
import java.io.InputStream;

import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.commandline.necessaryNativeSources.RequireNativeSources;
import it.imt.erode.crn.implementations.InfoCRNReduction;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.IModel;
import it.imt.erode.importing.AbstractImporter;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.simulation.output.DataOutputHandler;
import it.imt.erode.simulation.output.DataOutputHandlerAbstract;
import it.imt.erode.simulation.output.IDataOutputHandler;

public abstract class AbstractCommandLine implements ICommandLine {

	static HashMap<RequireNativeSources, HashMap<String, String>> downoloadLinks;
	
	static {
		downoloadLinks = new HashMap<>(3);
		
		HashMap<String, String> downloadLinksZ3 = new HashMap<String, String>(4);
		downoloadLinks.put(RequireNativeSources.Z3, downloadLinksZ3);
		
		downloadLinksZ3.put("win32", "https://github.com/IMTAltiStudiLucca/ERODE-Libraries/blob/master/z3SourceLibraries/windows32.zip?raw=true");
		downloadLinksZ3.put("win64", "https://github.com/IMTAltiStudiLucca/ERODE-Libraries/blob/master/z3SourceLibraries/windows64.zip?raw=true");
		downloadLinksZ3.put("linux32", "https://github.com/IMTAltiStudiLucca/ERODE-Libraries/blob/master/z3SourceLibraries/linux32.zip?raw=true");
		downloadLinksZ3.put("linux64", "https://github.com/IMTAltiStudiLucca/ERODE-Libraries/blob/master/z3SourceLibraries/linux64.zip?raw=true");
				
		HashMap<String, String> downloadLinksCVODE = new HashMap<String, String>(4);
		downoloadLinks.put(RequireNativeSources.CVODE, downloadLinksCVODE);
		downloadLinksCVODE.put("win32", "https://github.com/IMTAltiStudiLucca/ERODE-Libraries/blob/master/jsundialsSourceLibraries/cvode/windows32.zip?raw=true");
		downloadLinksCVODE.put("win64", "https://github.com/IMTAltiStudiLucca/ERODE-Libraries/blob/master/jsundialsSourceLibraries/cvode/windows64.zip?raw=true");
		downloadLinksCVODE.put("linux32", "https://github.com/IMTAltiStudiLucca/ERODE-Libraries/blob/master/jsundialsSourceLibraries/cvode/linux32.zip?raw=true");
		downloadLinksCVODE.put("linux64", "https://github.com/IMTAltiStudiLucca/ERODE-Libraries/blob/master/jsundialsSourceLibraries/cvode/linux64.zip?raw=true");
		
		HashMap<String, String> downloadLinksIDA = new HashMap<String, String>(4);
		downoloadLinks.put(RequireNativeSources.IDA, downloadLinksIDA);
		downloadLinksIDA.put("win32", "https://github.com/IMTAltiStudiLucca/ERODE-Libraries/blob/master/jsundialsSourceLibraries/dae/windows32.zip?raw=true");
		downloadLinksIDA.put("win64", "https://github.com/IMTAltiStudiLucca/ERODE-Libraries/blob/master/jsundialsSourceLibraries/dae/windows64.zip?raw=true");
		downloadLinksIDA.put("linux32", "https://github.com/IMTAltiStudiLucca/ERODE-Libraries/blob/master/jsundialsSourceLibraries/dae/linux32.zip?raw=true");
		downloadLinksIDA.put("linux64", "https://github.com/IMTAltiStudiLucca/ERODE-Libraries/blob/master/jsundialsSourceLibraries/dae/linux64.zip?raw=true");
	}
		
	protected CommandsReader commandsReader;
	protected IPartition partition;
	protected boolean fromGUI;
	protected IMessageDialogShower messageDialogShower;
	protected Terminator terminator;
	
	protected IDataOutputHandler dog=new DataOutputHandler();
	
	protected static boolean librariesPresent=false;
	protected static final boolean HASTOCHECKLIBRARIESZ3 = true;
	
	protected static boolean librariesSundialsCVODEPresent=false;
	protected static final boolean HASTOCHECKLIBRARIESSUNDIALSCVODE = true;
	
	protected static boolean librariesSundialsIDAPresent=false;
	protected static final boolean HASTOCHECKLIBRARIESSUNDIALSIDA = true;
	
	public static boolean getLibrariesPresent(){
		return librariesPresent;
	}
	public static void setLibrariesPresent(boolean librariesPresent2){
		librariesPresent=librariesPresent2;
	}
	

	
	public AbstractCommandLine(CommandsReader commandsReader, boolean fromGUI) {
		this.fromGUI=fromGUI;
		this.commandsReader=commandsReader;
	}
	
	protected IPartition getPartition() {
		return partition;
	}

	protected void setPartition(IPartition partition){
		this.partition=partition;
	}
	

	@Override
	public void setDataOutputHandler(IDataOutputHandler dog){
		this.dog=dog;
	}
	
	@Override
	public void setMessageDialogShower(IMessageDialogShower msgShower) {
		this.messageDialogShower=msgShower;
	}
	
	@Override
	public void setTerminator(Terminator terminator) {
		this.terminator = terminator;
	}
	
	
	protected void checkLibrariesJSundialsCVODE(MessageConsoleStream out, BufferedWriter bwOut) throws IOException {
		String libName="jniODE";
		checkLibrariesJSundials(out, bwOut,libName,RequireNativeSources.CVODE);
	}
	protected void checkLibrariesJSundialsIDA(MessageConsoleStream out, BufferedWriter bwOut) throws IOException {
		String libName="jniDAE";
		checkLibrariesJSundials(out, bwOut,libName,RequireNativeSources.IDA);
	}
	
	protected void checkLibrariesJSundials(MessageConsoleStream out, BufferedWriter bwOut,String libName, RequireNativeSources library) throws IOException {
		//String libName="jniODE";
		boolean alsoWithJavaPostfix=false;
		String libExtension = ".jnilib";
		String p = System.getProperty("os.name");
		boolean win=false;
		boolean lin=false;
		boolean mac=false;
		if(p.contains("Windows")||p.contains("Linux")||p.contains("Mac")){
			if(p.contains("Windows")){
				libExtension=".dll";
				win=true;
			}
			else if(p.contains("Linux")){
				libExtension=".so";
				lin=true;
			}
			else{
				mac=true;
			}
			//mac=false;win=true;
			if(mac){
				String fileName = "lib"+libName+libExtension;
				File f = new File(fileName);
				if(!f.exists()){
					String localNesting ="necessaryNativeSources"+File.separator;
					copyFile(localNesting,"lib"+libName+libExtension,getClass());
					if(alsoWithJavaPostfix) {
						copyFile(localNesting,"lib"+libName+"java"+libExtension,getClass());
					}
					
				}
				/*else {
					///Applications/Eclipse-SDK-4.7.1a.app/Contents/MacOS/libjniODE.jnilib
					System.out.println(f.getAbsolutePath());
					System.out.println(f.getCanonicalPath());
					System.out.println(f.getName());
					System.out.println(f.getPath());
					System.out.println(f.getParentFile());
					
				}*/
				if(library.equals(RequireNativeSources.CVODE)) {
					librariesSundialsCVODEPresent=true;
				}
				else {
					librariesSundialsIDAPresent=true;
				}
				//librariesPresent=true;
			}
			else{
				searchForLibraryInJavaLibraryPath(out, bwOut, libName, libExtension, win, lin, mac,library);
			}
		}
		
	}
	private void searchForLibraryInJavaLibraryPath(MessageConsoleStream out, BufferedWriter bwOut, String libName, String libExtension,
			boolean win, boolean lin, boolean mac,RequireNativeSources library) {

		String property = System.getProperty("java.library.path");
		//System.out.println("\n"+property+"\n");
		//CRNReducerCommandLine.println(out,bwOut, "\n"+property+"\n");
		StringTokenizer st = new StringTokenizer(property);
		ArrayList<String> paths = new ArrayList<>();
		while(st.hasMoreTokens()){
			String sep =File.pathSeparator;
			String path = st.nextToken(sep);
			paths.add(path);
		}
		boolean exists=false;
		for (String path : paths) {
			//String fileName = path+File.separator+"lib"+libName+libExtension;
			String fileName = path+File.separator;
			if(!win) {
				fileName+="lib"+libName+libExtension;
			}
			else {
				fileName+=libName+libExtension;
			}
			
			File f = new File(fileName);
			if(f.exists()){
				exists=true;
				break;
			}
		}
		if(!exists){
			String link32="";
			String link64="";
			String link32Short="";
			String link64Short="";
			String msg = "";
			if(win){
				//link32="https://www.dropbox.com/s/w35iy8ktbovwyd9/windows32.zip?dl=0";
				//link64= "https://www.dropbox.com/s/7upchpuf544l078/windows64.zip?dl=0";
				link32=downoloadLinks.get(library).get("win32");
				link64=downoloadLinks.get(library).get("win64");
				
				
				//link32= "https://www.dropbox.com/s/iits68sq6uvksc9/windows32.zip?dl=0";//"https://copy.com/9qQPXiAhbbFSVCyv"; //"http://sysma.imtlucca.it/erode/windows32.zip";
				//link64= "https://www.dropbox.com/s/czkwcdpjczp241l/windows64.zip?dl=0";//"https://copy.com/4k8in9MIg7V3tnZF"; //"http://sysma.imtlucca.it/erode/windows64.zip";
				//msg = "Please download the archive \n   http://sysma.imtlucca.it/erode/windows32.zip or \n   http://sysma.imtlucca.it/erode/windows64.zip\n"+"and add its files to one of the following locations:";
				link32Short="windows32.zip ";
				link64Short="windows64.zip ";
			}
			else if(lin){
				//msg = "Please download the archive \n   http://sysma.imtlucca.it/erode/linux32.zip or \n   http://sysma.imtlucca.it/erode/linux64.zip\n"+"and add its files to one of the following locations:";
				//msg = "Please download the archive \n   https://www.dropbox.com/s/qvyqcart38dol2j/linux32.zip or \n   https://www.dropbox.com/s/qzo8uh04pgf65ny/linux64.zip\n"+"and add its files to one of the following locations:";
				//link32="https://www.dropbox.com/s/p4l3a3d64hxuug9/linux32.zip?dl=0";
				//link64="https://www.dropbox.com/s/wkh7qqspwgwoaud/linux64.zip?dl=0";
				link32=downoloadLinks.get(library).get("linux32");
				link64=downoloadLinks.get(library).get("linux64");
				
				
				//link32= "https://www.dropbox.com/s/qvyqcart38dol2j/linux32.zip?dl=0";//"https://copy.com/x6V6jqhzApcXqPqi";//"http://sysma.imtlucca.it/erode/linux32.zip";
				//link64= "https://www.dropbox.com/s/qzo8uh04pgf65ny/linux64.zip?dl=0";//"https://copy.com/8HY8DS7zq0g98xLc";//"http://sysma.imtlucca.it/erode/linux64.zip";
				link32Short="linux32.zip ";
				link64Short="linux64.zip ";
			}
			msg = "Please download the archive \n   "+link32 +" or \n   "+link64+"\n"+"and add its files to one of the following locations:"; 
			CRNReducerCommandLine.println(out,bwOut, msg);
			for (String path : paths) {
				CRNReducerCommandLine.println(out,bwOut, "   "+path);
			}
			String OS = "";
			if(mac){
				OS="Mac";
			}
			else if(win){
				OS="Windows";
			}
			else{
				OS = "Linux";
			}
			//msgVisualizer.showMessage(msg,"and add its files to one of the following locations:",paths);
			if(messageDialogShower!=null){
				messageDialogShower.openMissingZ3LibrariesDialog(link32,link64,link32Short,link64Short,paths,OS);
			}
			CRNReducerCommandLine.println(out,bwOut, "\nI terminate");
		}
		else{
			//librariesPresent=true;
			if(library.equals(RequireNativeSources.CVODE)) {
				librariesSundialsCVODEPresent=true;
			}
			else {
				librariesSundialsIDAPresent=true;
			}
		}
	}
	
	
	protected void checkLibrariesZ3(MessageConsoleStream out, BufferedWriter bwOut) throws IOException {
		String libExtension = ".dylib";
		String p = System.getProperty("os.name");
		boolean win=false;
		boolean lin=false;
		boolean mac=false;
		if(p.contains("Windows")||p.contains("Linux")||p.contains("Mac")){
			if(p.contains("Windows")){
				libExtension=".dll";
				win=true;
			}
			else if(p.contains("Linux")){
				libExtension=".so";
				lin=true;
			}
			else{
				mac=true;
			}
			//mac=false;win=true;
			if(mac){
				/*File fee = new File("aaaaaaaaaa.txt");
				fee.createNewFile();
				System.out.println(fee.getAbsolutePath());
				System.out.println(fee.getCanonicalPath());*/
				String fileName = "libz3"+libExtension;
				File f = new File(fileName);
				if(!f.exists()){
					String localNesting ="necessaryNativeSources"+File.separator;
					copyFile(localNesting,"libz3"+libExtension,getClass());
					copyFile(localNesting,"libz3java"+libExtension,getClass());
				}
				//String s = f.getAbsolutePath();
				librariesPresent=true;
			}
			else{
				String property = System.getProperty("java.library.path");
				//System.out.println("\n"+property+"\n");
				//CRNReducerCommandLine.println(out,bwOut, "\n"+property+"\n");
				StringTokenizer st = new StringTokenizer(property);
				ArrayList<String> paths = new ArrayList<>();
				while(st.hasMoreTokens()){
					String sep =File.pathSeparator;
					String path = st.nextToken(sep);
					paths.add(path);
				}
				boolean exists=false;
				for (String path : paths) {
					String fileName = path+File.separator+"libz3"+libExtension;
					File f = new File(fileName);
					if(f.exists()){
						exists=true;
						break;
					}
				}
				if(!exists){
					String link32="";
					String link64="";
					String link32Short="";
					String link64Short="";
					String msg = "";
					if(win){
						//link32="https://www.dropbox.com/s/o2vghpteuhrrhz2/windows32.zip?dl=0";
						//link64= "https://www.dropbox.com/s/d45lyfgfvlg6ps3/windows64.zip?dl=0";
						link32= downoloadLinks.get(RequireNativeSources.Z3).get("win32");
						link64= downoloadLinks.get(RequireNativeSources.Z3).get("win64");
						
						
						//link32= "https://www.dropbox.com/s/iits68sq6uvksc9/windows32.zip?dl=0";//"https://copy.com/9qQPXiAhbbFSVCyv"; //"http://sysma.imtlucca.it/erode/windows32.zip";
						//link64= "https://www.dropbox.com/s/czkwcdpjczp241l/windows64.zip?dl=0";//"https://copy.com/4k8in9MIg7V3tnZF"; //"http://sysma.imtlucca.it/erode/windows64.zip";
						//msg = "Please download the archive \n   http://sysma.imtlucca.it/erode/windows32.zip or \n   http://sysma.imtlucca.it/erode/windows64.zip\n"+"and add its files to one of the following locations:";
						link32Short="windows32.zip ";
						link64Short="windows64.zip ";
						msg = "Please download the archive \n   "+link32 +" or \n   "+link64+"\n"+"and add its files to one of the following locations:";
					}
					else if(lin){
						//msg = "Please download the archive \n   http://sysma.imtlucca.it/erode/linux32.zip or \n   http://sysma.imtlucca.it/erode/linux64.zip\n"+"and add its files to one of the following locations:";
						//msg = "Please download the archive \n   https://www.dropbox.com/s/qvyqcart38dol2j/linux32.zip or \n   https://www.dropbox.com/s/qzo8uh04pgf65ny/linux64.zip\n"+"and add its files to one of the following locations:";
						//link32="https://www.dropbox.com/s/7jiam79dnxvq1au/linux32.zip?dl=0";
						//link64="https://www.dropbox.com/s/6mpkyzaj1uaen5k/linux64.zip?dl=0";
						//link32= downoloadLinks.get(RequireNativeSources.Z3).get("linux32");
						link32=null;
						link64= downoloadLinks.get(RequireNativeSources.Z3).get("linux64");
						
						
						
						
						//link32= "https://www.dropbox.com/s/qvyqcart38dol2j/linux32.zip?dl=0";//"https://copy.com/x6V6jqhzApcXqPqi";//"http://sysma.imtlucca.it/erode/linux32.zip";
						//link64= "https://www.dropbox.com/s/qzo8uh04pgf65ny/linux64.zip?dl=0";//"https://copy.com/8HY8DS7zq0g98xLc";//"http://sysma.imtlucca.it/erode/linux64.zip";
						//link32Short="linux32.zip ";
						link32Short=null;
						link64Short="linux64.zip ";
						msg = "Please download the archive \n   "+link64+"\n"+"and add its files to one of the following locations:";
					}
					 
					CRNReducerCommandLine.println(out,bwOut, msg);
					for (String path : paths) {
						CRNReducerCommandLine.println(out,bwOut, "   "+path);
					}
					String OS = "";
					if(mac){
						OS="Mac";
					}
					else if(win){
						OS="Windows";
					}
					else{
						OS = "Linux";
					}
					//msgVisualizer.showMessage(msg,"and add its files to one of the following locations:",paths);
					if(messageDialogShower!=null){
						messageDialogShower.openMissingZ3LibrariesDialog(link32,link64,link32Short,link64Short,paths,OS);
					}
					CRNReducerCommandLine.println(out,bwOut, "\nI terminate");
				}
				else{
					librariesPresent=true;
				}
				
			}
		}
		
	}

	private static void copyFile(String localNesting,String relativePath,Class<? extends Object> myClass) throws IOException {
		
		URL sourceURL = myClass.getResource(localNesting+relativePath);
		InputStream is = sourceURL.openStream();

		Path targetPath = FileSystems.getDefault().getPath(relativePath);
		//System.out.println("targetPath: "+targetPath.toString());
		Files.copy(is, targetPath,StandardCopyOption.REPLACE_EXISTING);
		is.close();
	}
	
	protected void copyFile(String originalFileName, String whereToCopyFile) throws IOException {
		//if(whereToCopyFile.contains("MODEL1506230002")) {System.out.println("Ciao");}
		FileInputStream is = new FileInputStream(new File(originalFileName));
		
		AbstractImporter.createParentDirectories(whereToCopyFile);
		File fileCopy = new File(whereToCopyFile);
		Path targetPath = fileCopy.toPath();
		
		Files.copy(is, targetPath,StandardCopyOption.REPLACE_EXISTING);
		
		is.close();
		
	}

	private List<String> createCsvReductionLabels() {
		List<String> csvLabels = new ArrayList<String>();
		csvLabels.add("ModelName");
		csvLabels.add("Reduction");
		csvLabels.add("ReductionSucceeded");
		csvLabels.add("Time(ms)");
		csvLabels.add("InitPartition");
		csvLabels.add("Species");
		csvLabels.add("ReducedSpecies");
		csvLabels.add("RedSp/OrigSp");
		csvLabels.add("Reactions");
		csvLabels.add("ReducedReactions");
		//csvLabels.add("Parameters");
		csvLabels.add("Parameters");
		return csvLabels;
	}
	
	protected void writeReductionInfoInCSVFile(MessageConsoleStream out, BufferedWriter bwOut, String csvFile,
			InfoCRNReduction infoReduction, LinkedHashMap<String, String> extraColumnsForCSV) {
		if(csvFile!=null) {
			List<String> csvLabels = createCsvReductionLabels();
			
			List<String> csvValues = new ArrayList<String>();
			
			csvValues.add(infoReduction.getOriginalNetwork());
			csvValues.add(infoReduction.getReductionTechnique());
			csvValues.add("1");
			csvValues.add(String.valueOf(infoReduction.getTimeInMS()));
			
			csvValues.add(String.valueOf(infoReduction.getInitPartitionSize()));
			
			csvValues.add(String.valueOf(infoReduction.getOriginalSpecies()));
			csvValues.add(String.valueOf(infoReduction.getReducedSpecies()));
			csvValues.add(infoReduction.getPercRedSizeOverOrigSize());
			
			csvValues.add(String.valueOf(infoReduction.getOriginalReactions()));
			csvValues.add(String.valueOf(infoReduction.getReducedReactions()));
			
			csvValues.add(String.valueOf(infoReduction.getParametersSize()));
													
			
			if(extraColumnsForCSV!=null) {
				for(Entry<String, String> entry: extraColumnsForCSV.entrySet()) {
					csvLabels.add(entry.getKey());
					csvValues.add(entry.getValue());
				}
			}
			
			
			DataOutputHandlerAbstract.writeOneLineCSV(csvFile, "csv", csvLabels, csvValues, out, bwOut);
		}
	}

	protected void writeReductionNotSucceededInfoInCSVFile(MessageConsoleStream out, BufferedWriter bwOut, String csvFile, IModel crn, 
			String reduction,int initPartitionSize) {
		if(csvFile!=null) {
			List<String> csvLabels = createCsvReductionLabels();
			List<String> csvValues = new ArrayList<String>();
			
			csvValues.add(crn.getName());
			csvValues.add(reduction);
			csvValues.add("0");
			csvValues.add(String.valueOf(-1));
			
			csvValues.add(String.valueOf(initPartitionSize));
			
			csvValues.add(String.valueOf(crn.getSpecies().size()));
			csvValues.add("-1");
			csvValues.add("-1");
			
			if(crn instanceof ICRN) {
				csvValues.add(String.valueOf(((ICRN)crn).getReactions().size()));
			}
			else {
				csvValues.add(String.valueOf(crn.getSpecies().size()));
			}
			
			csvValues.add(String.valueOf(-1));
			
			csvValues.add(String.valueOf(crn.getParameters().size()));
			
			DataOutputHandlerAbstract.writeOneLineCSV(csvFile, "csv", csvLabels, csvValues, out, bwOut);
		}
	}
	
}
