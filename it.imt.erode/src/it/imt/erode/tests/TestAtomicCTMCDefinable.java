package it.imt.erode.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.CommandsReader;
import it.imt.erode.importing.BioNetGenImporter;

public class TestAtomicCTMCDefinable {

	public static void main(String[] args) {
		String folderName=BioNetGenImporter.BNGNetworksFolder;
		//String folderName="."+File.separator+"BNGNetworks2"+File.separator+"net"+File.separator;
		//String folderName=BioNetGenImporter.BNGNetworksFolder;
		List<String> commands = new ArrayList<String>();
	    //commands.add("importBNG({fileIn=>"+folderName+"1471-2105-11-404-s1.net"+"})");
		commands.add("importBNG({fileIn=>"+folderName+"pcbi.1003217.s005.net"+"})");
		//commands.add("importBNG({fileIn=>"+folderName+"LotkaVolterra.net"+"})");
		//commands.add("importBNG({fileIn=>"+folderName+"catalysis.net"+"})");
		//commands.add("importBNG({fileIn=>"+folderName+"pcbi.1000578.s008.net"+"})");
		//commands.add("importBNG({fileIn=>"+folderName+"Motivating_example.net"+"})");
		//commands.add("importBNG({fileIn=>"+folderName+"machine.net"+"})");
		//commands.add("importBNG({fileIn=>"+folderName+"energy_example1.net"+"})");
		//commands.add("importBNG({fileIn=>"+folderName+"Tcr_tomek.net"+"})");
		//commands.add("importBNG({fileIn=>"+folderName+"fceri_gamma2_asym.net"+"})");
		//commands.add("importBNG({fileIn=>"+folderName+"machine.net"+"})");
		//commands.add("load({fileIn=>"+CRNImporter.CRNNetworksFolder+"exampleAtomicCTMC2.crn"+"})");
		//commands.add("load({fileIn=>"+CRNImporter.CRNNetworksFolder+"fceri_gamma2_asym.crn"+"})");
		//commands.add("write({fileOut=>"+CRNImporter.CRNNetworksFolder+"YiZiRatesAsInEFLPaperMonomer.crn"+"})");
		/*if(args.length>0){
			folderName=args[0];
		}
		final File folder = new File(folderName);
		for (final File fileEntry : folder.listFiles()) {
			if(fileEntry.getName().endsWith(".net")){
				commands.add("importBNG({fileIn=>"+folderName+fileEntry.getName()+"})");
			}
	    }*/
		
		CommandsReader commandsReader = new CommandsReader(commands,null,null);
		CRNReducerCommandLine cl = new CRNReducerCommandLine(commandsReader);
		try {
			cl.executeCommands(true,null,null);
		} catch (Exception e) {
			System.out.println("Unhandled errors arised while executing the commands. I terminate.");
			e.printStackTrace();
		}
	}
	
	public static void listFilesForFolder(final File folder) {
	    for (final File fileEntry : folder.listFiles()) {
	        if (fileEntry.isDirectory()) {
	            listFilesForFolder(fileEntry);
	        } else {
	        	System.out.println(fileEntry.getName());
	        }
	    }
	}

}
