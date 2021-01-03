package it.imt.erode.tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import it.imt.erode.auxiliarydatastructures.CRNandPartition;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.importing.AbstractImporter;
import it.imt.erode.importing.BioNetGenImporter;
import it.imt.erode.importing.CRNImporter;
import it.imt.erode.importing.ImporterOfSupportedNetworks;
import it.imt.erode.importing.SupportedFormats;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.ExactFluidBisimilarity;

public class TestEFL {
	
	public static void main(String[] args) {
		
		//System.out.println("Ciao");		
		boolean printInfo=true;
		boolean printCRN=false;
		boolean verbose = false;
		boolean initialiseCounters = false;
		boolean checkIfPartitionIsMSB=false;
		boolean reduce = false;

		/*File folder = new File(BioNetGenImporter.BNGNetworksFolder+"Validate"+File.separator);
		for (File f : folder.listFiles()) {
			try {
				test(folder+File.separator+f.getName(),printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,reduce);
				System.out.println("");
			} catch (Exception e) {
				System.err.println("File: " + f.getName());
				System.out.printStackTrace(e);
			}
		}*/

		
		

		
		try {
			String fileName;
			/*System.out.println("");
			String fileName =  CRNImporter.CRNNetworksFolder+"pippo.crn";
			test(fileName,true,true,initialiseCounters,true,checkIfPartitionIsMSB,true);
			
			
			fileName =  CRNImporter.CRNNetworksFolder+"am.crn";
			test(fileName,true,true,initialiseCounters,true,checkIfPartitionIsMSB,reduce);
			
			System.out.println("");
			fileName =  BioNetGenImporter.BNGNetworksFolder+"YiZi.net";
			test(fileName,true,true,initialiseCounters,true,checkIfPartitionIsMSB,reduce);
			System.out.println("");
			fileName = BioNetGenImporter.BNGNetworksFolder+"YiZiRatesAsInEFLPaper.net";
			test(fileName,true,true,initialiseCounters,true,checkIfPartitionIsMSB,reduce);
			System.out.println("");*/
			fileName = BioNetGenImporter.BNGNetworksFolder+"Mre.net";
			test(fileName,printInfo,true,initialiseCounters,true,checkIfPartitionIsMSB,reduce);
			System.out.println("");
			fileName = BioNetGenImporter.BNGNetworksFolder+"scaff22.net";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,reduce);
			System.out.println("");
			fileName = BioNetGenImporter.BNGNetworksFolder+"tlbr.net";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,reduce);
			System.out.println("");
			//fileName = BioNetGenImporter.BNGNetworksFolder+"Mre2.net";
			//test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,reduce);
			//System.out.println("");
			fileName = BioNetGenImporter.BNGNetworksFolder+"blbr.net";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,reduce);
			System.out.println("");
			fileName = BioNetGenImporter.BNGNetworksFolder+"BioNetGen_CCP.net";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,reduce);
			System.out.println("");

			fileName = BioNetGenImporter.BNGNetworksFolder+"BioNetGen_Fig3.net";
			test(fileName,true,true,initialiseCounters,true,checkIfPartitionIsMSB,reduce);
			System.out.println("");
			/*fileName = BioNetGenImporter.BNGNetworksFolder+"BioNetGen_Fig3-2.net";
		test(fileName,true,true,initialiseCounters,true,checkIfPartitionIsMSB,reduce);
		System.out.println("");
		fileName = BioNetGenImporter.BNGNetworksFolder+"BioNetGen_Fig3-3.net";
		test(fileName,true,true,initialiseCounters,true,checkIfPartitionIsMSB,reduce);
		System.out.println("");*/

			fileName = BioNetGenImporter.BNGNetworksFolder+"mmc1.net";
		test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,reduce);
		System.out.println("");
		fileName = BioNetGenImporter.BNGNetworksFolder+"scaff22.net";
		test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,reduce);
		System.out.println("");
		fileName = BioNetGenImporter.BNGNetworksFolder+"Rabitz.net";
		test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,reduce);
		System.out.println("");
		fileName = BioNetGenImporter.BNGNetworksFolder+"pcbi.1003217.s005.net";
		test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,reduce);
		System.out.println("");
		fileName = BioNetGenImporter.BNGNetworksFolder+"pcbi.1003217.s006.net";
		test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,reduce);
		System.out.println("");
		fileName = BioNetGenImporter.BNGNetworksFolder+"pcbi.1003217.s007.net";
		test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,reduce);
		System.out.println("");
		fileName = BioNetGenImporter.BNGNetworksFolder+"pcbi.1003217.s008.net";
		test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,reduce);
		System.out.println("");
		fileName = BioNetGenImporter.BNGNetworksFolder+"pcbi.1003217.s009.net";
		test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,reduce);
		System.out.println("");
		fileName = BioNetGenImporter.BNGNetworksFolder+"pcbi.1003217.s010.net";
		test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,reduce);
		System.out.println("");
		fileName = BioNetGenImporter.BNGNetworksFolder+"pcbi.1003217.s011.net";
		test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,reduce);
		System.out.println("");
		fileName = BioNetGenImporter.BNGNetworksFolder+"pcbi.1000364.s005.net";
		test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,reduce);
		System.out.println("");
		fileName = BioNetGenImporter.BNGNetworksFolder+"pcbi.1000364.s006.net";
		test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,reduce);
		System.out.println("");


		fileName = BioNetGenImporter.BNGNetworksFolder+"mapk.net";
		test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,reduce);
		System.out.println("");

		fileName = BioNetGenImporter.BNGNetworksFolder+"1471-2105-11-404-s1.net";
		test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,reduce);
		System.out.println("");
		fileName = BioNetGenImporter.BNGNetworksFolder+"fceri_fyn_lig.net";
		test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,reduce);
		System.out.println("");


		fileName = BioNetGenImporter.BNGNetworksFolder+"fceri_gamma2_asym.net";
		test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,reduce);
		System.out.println("");
		fileName = BioNetGenImporter.BNGNetworksFolder+"machine.net";
		test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,reduce);
		System.out.println("");
		} catch (UnsupportedFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	
	private static void test(String fileName, boolean printInfo, boolean printCRN, boolean initialiseCounters, boolean verbose, boolean checkIfPartitionIsMSB, boolean reduce) throws UnsupportedFormatException, IOException, XMLStreamException{
		
		ICRN crn;
		IPartition initialPartition;
		SupportedFormats format = SupportedFormats.BNG;
		if(fileName.endsWith(".crn")){
			format=SupportedFormats.CRN;
		}
		
		AbstractImporter importer = new ImporterOfSupportedNetworks().importSupportedNetwork(fileName, printInfo, printCRN,format,true,null,null,null,false,false);
		crn = importer.getCRN();
		initialPartition = importer.getInitialPartition();
		
		
		long begin = System.currentTimeMillis();
		IPartition obtained = ExactFluidBisimilarity.computeEFL(crn, initialPartition, verbose,null,null,null);
		long end = System.currentTimeMillis();
		System.out.println("EFL Partitioning completed. From "+ crn.getSpecies().size() +" species to "+ obtained.size() + " blocks. Time necessary: "+(end-begin)/1000+ " (s)");
		/*if(checkIfPartitionIsMSB){
			List<ILabel> labels = MarkovianSpeciesBisimilarity.computeUnaryBinaryLabels(crn);
			boolean isMSB = MarkovianSpeciesBisimilarity.checkIfItPartitionIsMSB(crn, labels, obtained);
			if(isMSB){
				System.out.println("The partition is MSB");
			}
			else{
				System.out.println("The partition is NOT MSB");
			}
		}*/
		
		if(reduce){
			String name = crn.getName();
			int pos = name.lastIndexOf(File.separator);
			String path = name.substring(0, (pos>0)? pos:0);
			String relativeName = name.substring(pos+1);
			name = path+File.separator+"eflGrouped"+relativeName;
			
			//BNGImporter.printCRNToNetFile(crn, obtained,name,true,false);
			
			name = crn.getName();
			name = path+File.separator+"eflReduced"+relativeName;
			
			CRNandPartition cp = ExactFluidBisimilarity.computeReducedCRNEFL(crn,name, obtained, crn.getSymbolicParameters(),crn.getConstraints(),crn.getParameters(),null,null,null);
			ICRN reducedCRN = cp.getCRN();
			//IPartition trivialPartition = cp.getPartition();
			if(printCRN){
				System.out.println("The reduced CRN:");
				System.out.println(reducedCRN.toString());
			}
			
			//BNGImporter.printCRNToNetFile(reducedCRN, trivialPartition, name, false, false);
			CRNImporter.printCRNToCRNFile(cp.getCRN(), obtained, name, false, false,null,verbose,"",null,null,false);
			
			System.out.println("The original CRN:\n"+crn.toStringShort());
			System.out.println("The EFL reduced CRN:\n"+reducedCRN.toStringShort());
		}
	}

}
