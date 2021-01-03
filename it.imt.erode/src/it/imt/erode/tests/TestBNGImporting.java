package it.imt.erode.tests;

import java.io.IOException;

import it.imt.erode.importing.BioNetGenImporter;

public class TestBNGImporting {
	
	public static void main(String[] args) {
		
		boolean printInfo=true;
		boolean printCRN=true;
		
		String fileName = BioNetGenImporter.BNGNetworksFolder+"Mre.net";
		BioNetGenImporter bng = new BioNetGenImporter(fileName,null,null,null,false);
		try {
			bng.importBioNetGenNetwork(printInfo,printCRN,true,false,false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		bng = new BioNetGenImporter(fileName,null,null,null,true);
		try {
			bng.importBioNetGenNetwork(printInfo,printCRN,true,false,false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}

}
