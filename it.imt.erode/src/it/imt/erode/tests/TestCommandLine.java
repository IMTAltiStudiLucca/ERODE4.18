package it.imt.erode.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.CommandsReader;
import it.imt.erode.importing.BioNetGenImporter;

public class TestCommandLine {

	public static void main(String[] args) {
		//String folderName=BioNetGenImporter.BNGNetworksFolder;
		//String folderName="."+File.separator+"BNGNetworks2"+File.separator;
		String folderName= BioNetGenImporter.BNGNetworksFolder;
		List<String> commands = new ArrayList<String>();
		/*commands.add("importBNG({fileIn=>"+folderName+"energy_example1.net"+"})");
		commands.add("reduceEFL({computeOnlyPartition=>true})");
		commands.add("importBNG({fileIn=>"+folderName+"Haugh2b.net"+"})");
		commands.add("reduceEFL({computeOnlyPartition=>true})");
		commands.add("importBNG({fileIn=>"+folderName+"mapk-monomers.net"+"})");
		commands.add("reduceEFL({computeOnlyPartition=>true})");
		commands.add("importBNG({fileIn=>"+folderName+"mmc1.net"+"})");
		commands.add("reduceEFL({computeOnlyPartition=>true})");
		commands.add("importBNG({fileIn=>"+folderName+"pcbi.1003217.s005.net"+"})");
		commands.add("reduceEFL({computeOnlyPartition=>true})");
		commands.add("importBNG({fileIn=>"+folderName+"pcbi.1003217.s006.net"+"})");
		commands.add("reduceEFL({computeOnlyPartition=>true})");
		commands.add("importBNG({fileIn=>"+folderName+"pcbi.1003217.s007.net"+"})");
		commands.add("reduceEFL({computeOnlyPartition=>true})");
		commands.add("importBNG({fileIn=>"+folderName+"pcbi.1003217.s008.net"+"})");
		commands.add("reduceEFL({computeOnlyPartition=>true})");
		commands.add("importBNG({fileIn=>"+folderName+"pcbi.1003217.s009.net"+"})");
		commands.add("reduceEFL({computeOnlyPartition=>true})");
		commands.add("importBNG({fileIn=>"+folderName+"pcbi.1003217.s010.net"+"})");
		commands.add("reduceEFL({computeOnlyPartition=>true})");
		commands.add("importBNG({fileIn=>"+folderName+"pcbi.1003217.s011.net"+"})");
		commands.add("reduceEFL({computeOnlyPartition=>true})");
		commands.add("importBNG({fileIn=>"+folderName+"pcbi.1003217.s012.net"+"})");
		commands.add("reduceEFL({computeOnlyPartition=>true})");
		commands.add("importBNG({fileIn=>"+folderName+"pcbi.1003217.s013.net"+"})");
		commands.add("reduceEFL({computeOnlyPartition=>true})");
		commands.add("importBNG({fileIn=>"+folderName+"Repressilator.net"+"})");
		commands.add("reduceEFL({computeOnlyPartition=>true})");
		commands.add("importBNG({fileIn=>"+folderName+"S1BIS.net"+"})");
		commands.add("reduceEFL({computeOnlyPartition=>true})");
		commands.add("importBNG({fileIn=>"+folderName+"scaff11.net"+"})");
		commands.add("reduceEFL({computeOnlyPartition=>true})");
		commands.add("importBNG({fileIn=>"+folderName+"scaff22.net"+"})");
		commands.add("reduceEFL({computeOnlyPartition=>true})");
		commands.add("importBNG({fileIn=>"+folderName+"simple_system.net"+"})");
		commands.add("reduceEFL({computeOnlyPartition=>true})");
		commands.add("importBNG({fileIn=>"+folderName+"Tcr_tomek.net"+"})");
		commands.add("reduceEFL({computeOnlyPartition=>true})");
		commands.add("importBNG({fileIn=>"+folderName+"tlbr.net"+"})");
		commands.add("reduceEFL({computeOnlyPartition=>true})");
		commands.add("importBNG({fileIn=>"+folderName+"wnt.net"+"})");
		commands.add("reduceEFL({computeOnlyPartition=>true})");
		commands.add("importBNG({fileIn=>"+folderName+"YMC.net"+"})");
		commands.add("reduceEFL({computeOnlyPartition=>true})");*/


		//commands.add("importBNG({fileIn=>"+folderName+"1471-2105-11-404-s1.net"+"})");
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
		if(args.length>0){
			folderName=args[0];
		}
		final File folder = new File(folderName);
		for (final File fileEntry : folder.listFiles()) {
			String fileName = fileEntry.getName();
			if(fileName.endsWith(".net") && (!fileName.endsWith("e8.net")) && (!fileName.endsWith("e9.net")) ) {
				commands.add("importBNG({fileIn=>"+folderName+fileEntry.getName()+"})");
				commands.add("reduceDSB({computeOnlyPartition=>true})");
				commands.add("reduceWFB({computeOnlyPartition=>true})");
				//commands.add("reduceMSB({computeOnlyPartition=>true})");
				//commands.add("reduceEFL({computeOnlyPartition=>true})");
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
