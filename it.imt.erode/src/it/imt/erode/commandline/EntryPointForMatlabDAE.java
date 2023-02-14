package it.imt.erode.commandline;

import java.io.IOException;
import java.math.BigDecimal;

import com.microsoft.z3.Z3Exception;

//import it.imt.erode.auxiliarydatastructures.IPartitionAndBoolean;
//import it.imt.erode.auxiliarydatastructures.Reduction;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.partition.interfaces.IPartition;
//import it.imt.erode.partitionrefinement.algorithms.CRNBisimulationsNAry;
//import it.imt.erode.partitionrefinement.algorithms.EpsilonDifferentialEquivalences;
import it.imt.erode.partitionrefinement.algorithms.ExactFluidBisimilarity;


public class EntryPointForMatlabDAE extends EntryPointForMatlabAbstract {

	public EntryPointForMatlabDAE(boolean printPartitions, boolean printCRNs){
		super(printPartitions, printCRNs,false);
		CRNReducerCommandLine.println(out,bwOut,"ERODE instantiated");
	}


	public static void main(String[] args) throws UnsupportedFormatException, Z3Exception, IOException {
		//EntryPointForMatlabDAE entry = new EntryPointForMatlabDAE(true,true);
		
		
		//importAffineSystem( fileIn="uudg1A.csv" , BFile="uudg1B.csv" , ICFile= "uudg1IC.csv")
		EntryPointForMatlabDAE erode = new EntryPointForMatlabDAE(false,false);
		String fileIn="uudg1A.csv";
		String BFile="uudg1B.csv";
		String ICFile="uudg1IC.csv";
		erode.importAffineSystem(fileIn, BFile, ICFile);
		int[] obtainedBE = erode.computeBE();
		IPartition obtainedBEPart = erode.importPartition(obtainedBE);
		System.out.println("The obtained partition has "+obtainedBEPart.size()+" blocks");
		
		/*
		double[] b = new double[17];
		b[0]=5;
		double tolerancePrepartitioning=0;
		
		EntryPointForMatlabDAE erodeA = new EntryPointForMatlabDAE(false,false);
        int nA=erodeA.importCCSV("./A.csv");
        
        EntryPointForMatlabDAE erodeE = new EntryPointForMatlabDAE(false,false);
        int nE=erodeE.importCCSV("./E.csv");
        
        
        int[] H = erodeA.computePrepartition(b, tolerancePrepartitioning);
        IPartition Hpart = erodeA.importPartition(H);
        int prev=Hpart.size();
        H = erodeA.computeBE(H, true);
        H = erodeE.computeBE(H, true);
        Hpart = erodeA.importPartition(H);
        while(Hpart.size() > prev){
        	prev=Hpart.size();
        	H = erodeA.computeBE(H, true);
            H = erodeE.computeBE(H, true);
            Hpart = erodeA.importPartition(H);
        }
        //H = javaMethod('computeBB', erodeE, H,true)';
        */
	}
	
//	private IPartition importPartition(double[] b){
//		//numbersAreIDOfRepresentativeSpecies=false
//		IPartition initialPartition = new Partition(b.length);
//		HashMap<Double,IBlock> initialPartitionHM=new HashMap<>();
//
//		for(int i=0;i<b.length;i++){
//			int speciesId = i;
//			IBlock block = initialPartitionHM.get(b[i]);
//			if(block==null){
//				block = new Block();
//				initialPartition.add(block);
//				initialPartitionHM.put(b[i], block);
//			}
//			block.addSpecies(idToSpecies[speciesId]);
//		}
//		return initialPartition;
//	}
	
	public int[] computeBB(double[] b, double tolerancePrepartitiong/*, int scale*/) throws UnsupportedFormatException, Z3Exception, IOException{
		//numbersAreIDOfRepresentativeSpecies=false
		
		//CRNBisimulationsNAry.SCALE=scale;
		//CRNBisimulationsNAry.TOLERANCE = new BigDecimal("1E-"+(CRNBisimulationsNAry.SCALE));
		
		IPartition initialPartition = erode.getPartition();
		initialPartition=ExactFluidBisimilarity.prepartitionWithTolerance(erode.getCRN(), initialPartition, true, out, bwOut, new Terminator(), b, BigDecimal.valueOf(tolerancePrepartitiong));
		
		erode.setPartition(initialPartition);
		if(printPartitions){
			CRNReducerCommandLine.println(out,bwOut,"Initial partition:\n"+initialPartition);
		}

		return performActualBE();
		
	}


	public int[] computePrepartition(double[] b, double tolerancePrepartitiong) {
		IPartition initialPartition = erode.getPartition();
		initialPartition=ExactFluidBisimilarity.prepartitionWithTolerance(erode.getCRN(), initialPartition, true, out, bwOut, new Terminator(), b, BigDecimal.valueOf(tolerancePrepartitiong));
		
		//crnreducer.setPartition(initialPartition);
		if(printPartitions){
			CRNReducerCommandLine.println(out,bwOut,"Initial partition:\n"+initialPartition);
		}
		
		int[] obtainedPartitionToExport = exportPartition(initialPartition);
		return obtainedPartitionToExport;
	}
	
	public int[] computeEpsBB(double[] b, double tolerancePrepartitiong, double epsilon/*, int scale*/) throws UnsupportedFormatException, Z3Exception, IOException{
		//numbersAreIDOfRepresentativeSpecies=false
		
		//CRNBisimulationsNAry.SCALE=scale;
		//CRNBisimulationsNAry.TOLERANCE = new BigDecimal("1E-"+(CRNBisimulationsNAry.SCALE));
		
		//long begin = System.currentTimeMillis();
		
		IPartition initialPartition = erode.getPartition();
		initialPartition=ExactFluidBisimilarity.prepartitionWithTolerance(erode.getCRN(), initialPartition, true, out, bwOut, new Terminator(), b, BigDecimal.valueOf(tolerancePrepartitiong));
		
		erode.setPartition(initialPartition);
		if(printPartitions){
			CRNReducerCommandLine.println(out,bwOut,"Initial partition:\n"+initialPartition);
		}
		
		//long end = System.currentTimeMillis();
		//double seconds = (end-begin)/1000.0;
		//CRNReducerCommandLine.println(out,bwOut,"Pre-partitioning completed (tol="+tolerancePrepartitiong+"). From "+ crnreducer.getCRN().getSpecies().size() +" species to "+ initialPartition.size() + " blocks. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, (seconds))+ " (s)");
		//CRNReducerCommandLine.println(out,bwOut,"");
		
		
		
		CRNReducerCommandLine.println(out,bwOut,"");
				
		return performActualEpsilonBE(epsilon);
		
	}

}
