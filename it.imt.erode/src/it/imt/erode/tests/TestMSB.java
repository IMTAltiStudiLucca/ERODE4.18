package it.imt.erode.tests;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import it.imt.erode.auxiliarydatastructures.CRNandPartition;
import it.imt.erode.auxiliarydatastructures.IPartitionAndBoolean;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.label.ILabel;
import it.imt.erode.importing.AbstractImporter;
import it.imt.erode.importing.BioNetGenImporter;
import it.imt.erode.importing.ImporterOfSupportedNetworks;
import it.imt.erode.importing.SupportedFormats;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.CRNBisimulationsNAry;
import it.imt.erode.partitionrefinement.algorithms.SyntacticMarkovianBisimilarityOLD;

public class TestMSB {
	
	public static void main(String[] args) {
		
		boolean printInfo=true;
		boolean printCRN=true;
		boolean verbose = false;
		boolean initialiseCounters = false;
		boolean checkIfPartitionIsMSB=false;
		boolean checkIfTheMSBHasTheExactProperty=true;
		boolean printPartition=true;
		boolean reduce=true;
		String fileName=null;
		
		try {
			//fileName = CRNImporter.CRNNetworksFolder+"exampleExpansion.crn";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,checkIfTheMSBHasTheExactProperty,printPartition,reduce);
			System.out.println("");
			/*fileName = BioNetGenImporter.BNGNetworksFolder+"Mre2.net";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,checkIfTheMSBHasTheExactProperty);
			System.out.println("");
			fileName = BioNetGenImporter.BNGNetworksFolder+"TestEMSB3.net";
			test(fileName,true,true,initialiseCounters,true,checkIfPartitionIsMSB,checkIfTheMSBHasTheExactProperty);
			System.out.println("");
			fileName = BioNetGenImporter.BNGNetworksFolder+"Mre.net";
			test(fileName,true,true,initialiseCounters,verbose,checkIfPartitionIsMSB,checkIfTheMSBHasTheExactProperty);
			System.out.println("");
			fileName = BioNetGenImporter.BNGNetworksFolder+"tlbr.net";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,checkIfTheMSBHasTheExactProperty);
			System.out.println("");
			fileName = BioNetGenImporter.BNGNetworksFolder+"blbr.net";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,checkIfTheMSBHasTheExactProperty);
			System.out.println("");
			fileName = BioNetGenImporter.BNGNetworksFolder+"BioNetGen_CCP.net";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,checkIfTheMSBHasTheExactProperty);
			System.out.println("");
			fileName = BioNetGenImporter.BNGNetworksFolder+"BioNetGen_Fig3.net";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,checkIfTheMSBHasTheExactProperty);
			System.out.println("");
			fileName = BioNetGenImporter.BNGNetworksFolder+"mmc1.net";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,checkIfTheMSBHasTheExactProperty);
			System.out.println("");
			fileName = BioNetGenImporter.BNGNetworksFolder+"scaff22.net";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,checkIfTheMSBHasTheExactProperty);
			System.out.println("");
			fileName = BioNetGenImporter.BNGNetworksFolder+"Rabitz.net";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,checkIfTheMSBHasTheExactProperty);
			System.out.println("");
			fileName = BioNetGenImporter.BNGNetworksFolder+"pcbi.1003217.s005.net";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,checkIfTheMSBHasTheExactProperty);
			System.out.println("");
			fileName = BioNetGenImporter.BNGNetworksFolder+"pcbi.1003217.s006.net";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,checkIfTheMSBHasTheExactProperty);
			System.out.println("");
			fileName = BioNetGenImporter.BNGNetworksFolder+"pcbi.1003217.s007.net";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,checkIfTheMSBHasTheExactProperty);
			System.out.println("");
			fileName = BioNetGenImporter.BNGNetworksFolder+"pcbi.1003217.s008.net";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,checkIfTheMSBHasTheExactProperty);
			System.out.println("");
			fileName = BioNetGenImporter.BNGNetworksFolder+"pcbi.1003217.s009.net";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,checkIfTheMSBHasTheExactProperty);
			System.out.println("");
			fileName = BioNetGenImporter.BNGNetworksFolder+"pcbi.1003217.s010.net";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,checkIfTheMSBHasTheExactProperty);
			System.out.println("");
			fileName = BioNetGenImporter.BNGNetworksFolder+"pcbi.1003217.s011.net";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,checkIfTheMSBHasTheExactProperty);
			System.out.println("");
			fileName = BioNetGenImporter.BNGNetworksFolder+"pcbi.1000364.s005.net";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,checkIfTheMSBHasTheExactProperty);
			System.out.println("");
			fileName = BioNetGenImporter.BNGNetworksFolder+"pcbi.1000364.s006.net";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,checkIfTheMSBHasTheExactProperty);
			System.out.println("");

			fileName = BioNetGenImporter.BNGNetworksFolder+"mapk.net";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,checkIfTheMSBHasTheExactProperty);
			System.out.println("");
			fileName = BioNetGenImporter.BNGNetworksFolder+"1471-2105-11-404-s1.net";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,checkIfTheMSBHasTheExactProperty);
			System.out.println("");
			fileName = BioNetGenImporter.BNGNetworksFolder+"fceri_fyn_lig.net";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,checkIfTheMSBHasTheExactProperty);
			System.out.println("");

			fileName = BioNetGenImporter.BNGNetworksFolder+"fceri_gamma2_asym.net";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,checkIfTheMSBHasTheExactProperty);
			System.out.println("");
			fileName = BioNetGenImporter.BNGNetworksFolder+"machine.net";
			test(fileName,printInfo,printCRN,initialiseCounters,verbose,checkIfPartitionIsMSB,checkIfTheMSBHasTheExactProperty);
			System.out.println("");*/
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
	
	private static void test(String fileName, boolean printInfo, boolean printCRN, boolean initialiseCounters, boolean verbose, boolean checkIfPartitionIsMSB, boolean checkIfTheMSBHasTheExactProperty, boolean printPartition, boolean reduce) throws UnsupportedFormatException, IOException, XMLStreamException{
		ICRN crn;
		IPartition initialPartition;
		
		SupportedFormats format = SupportedFormats.BNG;
		if(fileName.endsWith(".crn")){
			format=SupportedFormats.CRN;
		}
		
		AbstractImporter importer = new ImporterOfSupportedNetworks().importSupportedNetwork(fileName, printInfo, printCRN,format,true,null,null,null,false,false);
		crn = importer.getCRN();
		initialPartition = importer.getInitialPartition();

		List<ILabel> labels = SyntacticMarkovianBisimilarityOLD.computeUnaryBinaryLabels(crn);
				
		long begin = System.currentTimeMillis();
		IPartitionAndBoolean obtainedPartitionAndBool = SyntacticMarkovianBisimilarityOLD.computeSMB(crn, labels, initialPartition, verbose,null,null,null,null,false,false);
		IPartition obtained = obtainedPartitionAndBool.getPartition(); 
		long end = System.currentTimeMillis();
		System.out.println("MSB Partitioning completed. From "+ crn.getSpecies().size() +" species to "+ obtained.size() + " blocks. Time necessary: "+(end-begin)/1000+ " (s)");
		
		if(printPartition){
			System.out.println("The obtained partition:");
			System.out.println(obtained.toString());
		}
		
		/*if(checkIfPartitionIsMSB){
			boolean isMSB = MarkovianSpeciesBisimilarity.checkIfItPartitionIsMSB(crn, labels, obtained);
			if(isMSB){
				System.out.println("The partition is MSB");
			}
			else{
				System.out.println("The partition is NOT MSB");
			}
		}*/
		/*if(checkIfTheMSBHasTheExactProperty){
			boolean isEMSB = ExactMarkovianSpeciesBisimilarity.checkIfEachSpeciesHasSameCrrPerProductOfLiftedBlocks(crn, labels, obtained);
			if(isEMSB){
				System.out.println("The MSB has the exact property");
			}
			else{
				System.out.println("The MSB has NOT the exact property");
			}
		}*/
		
		if(reduce){
			String name = crn.getName();
			int pos = name.lastIndexOf(File.separator);
			String path = name.substring(0, (pos>0)? pos:0);
			String relativeName = name.substring(pos+1);
			name = path+File.separator+"dsbGrouped"+relativeName;
			
			//BioNetGenImporter.printCRNToNetFile(crn, obtained, HowToModifyModelWhenWritinInBNGFile.GROUPBLOCKS,name);
			BioNetGenImporter.printCRNToNetFile(crn, obtained, name, false, true,verbose,null,null);
			
			
			name = crn.getName();
			name = path+File.separator+"dsbReduced"+relativeName;
			
			CRNandPartition cp = CRNBisimulationsNAry.computeReducedCRNOrdinary(crn,name, obtained, crn.getSymbolicParameters(),crn.getConstraints(),crn.getParameters(),null,null,null);
			ICRN reducedCRN = cp.getCRN();
			IPartition trivialPartition = cp.getPartition();
			if(printCRN){
				System.out.println("The reduced CRN:");
				System.out.println(reducedCRN.toString());
			}
			
			//BioNetGenImporter.printCRNToNetFile(reducedCRN, trivialPartition, HowToModifyModelWhenWritinInBNGFile.NONE,name);
			BioNetGenImporter.printCRNToNetFile(reducedCRN, trivialPartition, name, false, false,verbose,null,null);
			
			System.out.println("The original CRN:\n"+crn.toStringShort());
			System.out.println("The MSB reduced CRN:\n"+reducedCRN.toStringShort());
		}
	}

}
