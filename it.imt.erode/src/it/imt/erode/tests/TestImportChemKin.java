package it.imt.erode.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.CommandsReader;
import it.imt.erode.importing.chemkin.ChemKinImporter;

public class TestImportChemKin {

	public static void main(String[] args) {

		List<String> commands = new ArrayList<String>();
		String folderPath=null;
		File folder=null;
		
		boolean reduce=true;
		
		folderPath = ChemKinImporter.ChemKinNetworksFolder;
		folder = new File(folderPath);
		for (File f : folder.listFiles()) {
			//if(f.getName().endsWith("H2O2Update.inp")){
			if(f.getName().endsWith(".inp")||f.getName().endsWith(".CKI")){
				try {
					commands.add("importChemKin({fileIn=>"+folderPath+f.getName()+"})");
					commands.add("newLine");
					if(reduce){
						commands.add("reduceFDE({reducedFile=>"+folderPath+f.getName()+"FDE.crn})");
						commands.add("newLine");
						commands.add("reduceEFL({reducedFile=>"+folderPath+f.getName()+"BB.crn})");
						commands.add("newLine");
					}
				} catch (Exception e) {
					System.out.println("Loading of file "+f.getName()+" failed due to unhandled error. This is the exception stack trace.");
					e.printStackTrace();
				}
			}
		}
		
		folderPath = ChemKinImporter.ChemKinNetworksFolder+"CombustionLLNL"+File.separator+"MECHANISMS"+File.separator;
		folder = new File(folderPath);
		for (File f : folder.listFiles()) {
			//if(f.getName().endsWith("H2O2Update.inp")){
			if(f.getName().endsWith(".inp")||f.getName().endsWith(".CKI")){
				try {
					commands.add("importChemKin({fileIn=>"+folderPath+f.getName()+"})");
					commands.add("newLine");
					if(reduce){
						commands.add("reduceFDE({reducedFile=>"+folderPath+f.getName()+"FDE.crn})");
						commands.add("newLine");
						commands.add("reduceEFL({reducedFile=>"+folderPath+f.getName()+"BB.crn})");
						commands.add("newLine");
					}
				} catch (Exception e) {
					System.err.println("File: " + f.getName());
					e.printStackTrace();
				}
			}
		}
		
		folderPath = ChemKinImporter.ChemKinNetworksFolder+"CombustionLLNL"+File.separator+"ARCHIVED-MECHANISMS"+File.separator;
		folder = new File(folderPath);
		for (File f : folder.listFiles()) {
			//if(f.getName().endsWith("H2O2Update.inp")){
			if(f.getName().endsWith(".inp")||f.getName().endsWith(".CKI")){
				try {
					commands.add("importChemKin({fileIn=>"+folderPath+f.getName()+"})");
					commands.add("newLine");
					if(reduce){
						commands.add("reduceFDE({reducedFile=>"+folderPath+f.getName()+"FDE.crn})");
						commands.add("newLine");
						commands.add("reduceEFL({reducedFile=>"+folderPath+f.getName()+"BB.crn})");
						commands.add("newLine");
					}
				} catch (Exception e) {
					System.err.println("File: " + f.getName());
					e.printStackTrace();
				}
			}
		}
		
		
		folderPath = ChemKinImporter.ChemKinNetworksFolder+"POLIMI"+File.separator+"POLIMI_1412"+File.separator+"Kinetics"+File.separator;
		folder = new File(folderPath);
		for (File f : folder.listFiles()) {
			//if(f.getName().endsWith("H2O2Update.inp")){
			if(f.getName().endsWith(".inp")||f.getName().endsWith(".CKI")){
				try {
					commands.add("importChemKin({fileIn=>"+folderPath+f.getName()+"})");
					commands.add("newLine");
					if(reduce){
						commands.add("reduceFDE({reducedFile=>"+folderPath+f.getName()+"FDE.crn})");
						commands.add("newLine");
						commands.add("reduceEFL({reducedFile=>"+folderPath+f.getName()+"BB.crn})");
						commands.add("newLine");
					}
				} catch (Exception e) {
					System.err.println("File: " + f.getName());
					e.printStackTrace();
				}
			}
		}
		
		folderPath = ChemKinImporter.ChemKinNetworksFolder+"POLIMI"+File.separator+"polimi_skeletal_1410"+File.separator;
		folder = new File(folderPath);
		for (File f : folder.listFiles()) {
			//if(f.getName().endsWith("H2O2Update.inp")){
			if(f.getName().endsWith(".inp")||f.getName().endsWith(".CKI")){
				try {
					commands.add("importChemKin({fileIn=>"+folderPath+f.getName()+"})");
					commands.add("newLine");
					if(reduce){
						commands.add("reduceFDE({reducedFile=>"+folderPath+f.getName()+"FDE.crn})");
						commands.add("newLine");
						commands.add("reduceEFL({reducedFile=>"+folderPath+f.getName()+"BB.crn})");
						commands.add("newLine");
					}
				} catch (Exception e) {
					System.err.println("File: " + f.getName());
					e.printStackTrace();
				}
			}
		}
		
		
		folderPath = ChemKinImporter.ChemKinNetworksFolder+"SUPPORTED"+File.separator;
		folder = new File(folderPath);
		for (File f : folder.listFiles()) {
			//if(f.getName().endsWith("H2O2Update.inp")){
			if(f.getName().endsWith(".inp")||f.getName().endsWith(".CKI")){
				try {
					commands.add("importChemKin({fileIn=>"+folderPath+f.getName()+"})");
					commands.add("newLine");
					if(reduce){
						commands.add("reduceFDE({reducedFile=>"+folderPath+f.getName()+"FDE.crn})");
						commands.add("newLine");
						commands.add("reduceEFL({reducedFile=>"+folderPath+f.getName()+"BB.crn})");
						commands.add("newLine");
					}
				} catch (Exception e) {
					System.err.println("File: " + f.getName());
					e.printStackTrace();
				}
			}
		}
		

		CommandsReader commandsReader = new CommandsReader(commands,null,null);

		CRNReducerCommandLine cl = new CRNReducerCommandLine(commandsReader);
		try {
			cl.executeCommands(true,null,null);
		} catch (Exception e) {
			System.out.println("Unhandled errors arised while executing the commands. I terminate.");
			e.printStackTrace();
		}

	}

}
