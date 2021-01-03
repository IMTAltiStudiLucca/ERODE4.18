package it.imt.erode.importing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.IMessageDialogShower;


/**
 * 
 * @author Andrea Vandin
 * This class is used to import Boolean Networks encoded in the BNet format (extension: bnet).  
 */
public class BNetImporter extends AbstractImporter{
	
	public BNetImporter(String fileName,MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower) {
		super(fileName,out,bwOut,msgDialogShower);
	}

	public InfoCRNImporting readAndExportBNet(boolean print, String fileOut) throws FileNotFoundException, IOException, UnsupportedFormatException{

			if(print){
				CRNReducerCommandLine.println(out,bwOut,"\nReading BNet file"+getFileName());
			}
			
			createParentDirectories(fileOut);
			BufferedWriter bw;
			try {
				bw = new BufferedWriter(new FileWriter(fileOut));
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in readAndExportBNet, exception raised while creating the filewriter for file: "+fileOut);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
				return null;
			}

			initInfoImporting();
			getInfoImporting().setLoadedCRN(true);
			getInfoImporting().setLoadedCRN(false);

			BufferedReader br = getBufferedReader();
			String line; 


			long begin = System.currentTimeMillis();			
			LinkedHashMap<String, String> speciesToUpdateFunction=new LinkedHashMap<String, String>();
			
			
			while ((line = br.readLine()) != null) {
				line=line.trim();
				//Any other line is ignored (including comments)
				if(line.equals("")){
					bw.write("\n");
				}
				else if(line.startsWith("#") || line.startsWith("targets, factors")){
					bw.write("//");
					bw.write(line);
					bw.write("\n");
				} 
				else {
					
					int endOfSpecies = line.indexOf(',');
					if(endOfSpecies==-1) {
						endOfSpecies = line.indexOf(':');
					}
					if(endOfSpecies==-1) {
						endOfSpecies = line.indexOf('=');
					}
					
					if(endOfSpecies==-1) {
//						bw.write("//");
//						bw.write(line);
//						bw.write("\n");
					}
					else {
						String speciesName = line.substring(0,endOfSpecies);
						String updateFunction = line.substring(endOfSpecies+1).trim();
						speciesToUpdateFunction.put(speciesName, updateFunction);
					}
				}
			}
			
			
			getInfoImporting().setReadSpecies(speciesToUpdateFunction.size());
			getInfoImporting().setReadCRNReactions(speciesToUpdateFunction.size());
			long end=System.currentTimeMillis();
			getInfoImporting().setRequiredMS(end -begin);

			br.close();
			
			if(print){
				CRNReducerCommandLine.println(out,bwOut,"\nWriting the corresponding ERODE file in\n"+fileOut);
			}
			
			String name = overwriteExtensionIfEnabled(fileOut,"",true);
			int i = name.lastIndexOf(File.separator);
			if(i!=-1) {
				name=name.substring(i+1);
			}
			bw.write("begin Boolean network "+name+"\n");
			
			bw.write("begin init\n");
			for(String species : speciesToUpdateFunction.keySet()) {
				bw.write("  "+species+"\n");	
			}
			bw.write("end init\n\n");
			
			ArrayList<String> guessedInputs = new ArrayList<String>();
			for(Entry<String, String> pair : speciesToUpdateFunction.entrySet()) {
				String species = pair.getKey().trim();
				String updateFunction = pair.getValue().trim();
				if(species.equals(updateFunction)) {
					guessedInputs.add(species);
				}
				else if("0".equals(updateFunction)) {
					guessedInputs.add(species);
				} 
				else if("1".equals(updateFunction)) {
					guessedInputs.add(species);
				} 
			}
			
			
			bw.write(" /*\n" + 
					"  * An optional partition of species. It can be used as initial partition when applying any of the supported reductions. \n" + 
					"  * Unspecified species belong to a default block. \n" + 
					"  */\n");
			if(guessedInputs.size()>0) {
				bw.write("//I identified "+guessedInputs+" as possible input species (they have 'dummy' update function).\n//I prepare an initial partition preserving them.\n");
				bw.write(" begin partition\n");
				for(int s=0;s<guessedInputs.size();s++) {
					String species = guessedInputs.get(s);
					bw.write("{"+species+"}");
					if(s<guessedInputs.size()-1) {
						bw.write(",\n");
					}
					else {
						bw.write("\n");
					}
				}
				bw.write(" end partition\n\n");
			}
			else {
			bw.write(" begin partition\n" + 
					"  //{s1}\n" + 
					" end partition\n\n");
			}
			bw.write("begin update functions\n");
			for(Entry<String, String> pair : speciesToUpdateFunction.entrySet()) {
				String species = pair.getKey();
				String updateFunction = pair.getValue();
				bw.write("  "+species+ " = ");
				bw.write(updateFunction);
				bw.write("\n");
			}
			bw.write("end update functions\n\n");
			
			bw.write("reduceBBE(fileWhereToStorePartition=\""+name+"BBE.txt\",csvFile=\"reductionsMaximal.csv\",reducedFile=\""+name+"BBE.ode\")\n");
			if(guessedInputs.size()>0) {
				bw.write("reduceBBE(fileWhereToStorePartition=\""+name+"InputPreservingBBE.txt\",csvFile=\"reductionsIP.csv\",reducedFile=\""+name+"InputPreservingBBE.ode\",prePartition=USER)\n");
			}
			bw.write("\n");
			
			bw.write("end Boolean network\n");
			bw.close();

			if(print){
//				if(printInfo){
//					CRNReducerCommandLine.println(out,bwOut,getInfoImporting().toString());
//				}
			}

			//CRNReducerCommandLine.println(out,bwOut,crn);
			return getInfoImporting();
		}
	
}
