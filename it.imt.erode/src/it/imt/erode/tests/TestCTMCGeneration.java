package it.imt.erode.tests;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.importing.AbstractImporter;
import it.imt.erode.importing.CRNImporter;
import it.imt.erode.importing.ImporterOfSupportedNetworks;
import it.imt.erode.importing.SupportedFormats;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.simulation.stochastic.ctmcgenerator.CTMCGenerator;

public class TestCTMCGeneration {

	public static void main(String[] args) {
		boolean printInfo=true;
		boolean printCRN=false;
		
		/*String fileName = BioNetGenImporter.BNGNetworksFolder+"Mre.net";
		SupportedFormats format=SupportedFormats.BNG;
		test(fileName, format, printInfo, printCRN);*/
		
		String fileName = CRNImporter.CRNNetworksFolder+"AM.crn";
		SupportedFormats format=SupportedFormats.CRN;
		test(fileName, format, printInfo, printCRN);
	}

	private static void test(String fileName, SupportedFormats format, boolean printInfo, boolean printCRN) {
		AbstractImporter importer=null;
		try {
			importer = new ImporterOfSupportedNetworks().importSupportedNetwork(fileName, printInfo, printCRN,format,true,null,null,null,false,false);
		} catch (UnsupportedFormatException | IOException | XMLStreamException e) {
			e.printStackTrace();
		}
		ICRN crn = importer.getCRN();

		
		CTMCGenerator ctmcGen = new CTMCGenerator();
		ICRN ctmc = ctmcGen.generateCTMC(crn, null,null, null,true,new Terminator(),null);
		System.out.println("The generated ctmc: "+ctmc.toStringShort());
		System.out.println("\n"+ctmc.toString());
	}

}
