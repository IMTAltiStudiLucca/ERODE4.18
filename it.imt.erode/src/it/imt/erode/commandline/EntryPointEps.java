package it.imt.erode.commandline;

import java.io.IOException;
//import java.io.IOException;
import java.util.LinkedHashSet;

//import com.microsoft.z3.Z3Exception;

import it.imt.erode.importing.MatlabODEsImporter;
import it.imt.erode.importing.UnsupportedFormatException;
//import it.imt.erode.partition.interfaces.IPartition;
//import it.imt.erode.partitionrefinement.algorithms.CRNBisimulationsNAry;

public class EntryPointEps extends EntryPointForMatlabAbstract{

	public EntryPointEps(boolean printPartitions, boolean printModels) {
		super(printPartitions, printModels,false);
		CRNReducerCommandLine.println(out,bwOut,"ERODE instantiated");
	}
	
	public void approximateDE(String fileName, LinkedHashSet<String> paramsToPerturb,double epsilon,String prePartitionUserDefined, String prePartitionWRTIC, boolean forward, boolean backward,boolean fastDegreeOneBE) {
		boolean verbose=false;
		CRNReducerCommandLine.print(out,bwOut,"Writing the matlab script to file "+ fileName+" ...");
		
		try {
			MatlabODEsImporter.printEpsilonScriptToMatlabFIle(erode.getCRN(), erode.getPartition(), fileName, verbose, out, bwOut, null,paramsToPerturb,new Terminator(),epsilon,prePartitionUserDefined,prePartitionWRTIC,forward,backward,fastDegreeOneBE);
		} catch (UnsupportedFormatException e) {
			e.printStackTrace();
		}
		CRNReducerCommandLine.println(out,bwOut," completed");
	}
	
	public void simulateODE(double tEnd, int steps, String csvFile) {
		//simulateODE({tEnd=>1000.0,steps=>100,visualizePlot=>NO,library=>APACHE,csvFile=>/Users/andrea/OneDrive - Danmarks Tekniske Universitet/runtimes/runtime-ERODE.product(4)/isabel/epsislon/csvFile})
		try {
			erode.handleSimulateODECommand("simulateODE({tEnd=>"+tEnd+",steps=>"+steps+",visualizePlot=>NO,library=>APACHE,csvFile=>"+csvFile+"})", out, bwOut);
		} catch (IOException e) {
			e.printStackTrace();
		}
		//return crnreducer.simulateODE(tEnd, stepSize, steps, null, csvFile, visualizeVars, visualizeViews, minStep,maxStep,absTol,relTol,false, showLabels,out,bwOut,caption,false,BigDecimal.ZERO,print,apache);
	}
	
	

	public static void main(String[] args) {
		
		boolean printPartitions=false;
		boolean printModels=false;
		
		EntryPointEps erode = new EntryPointEps(printPartitions, printModels);
		//int numberOfSpecies = 
		erode.load("/Users/andrea/OneDrive - Danmarks Tekniske Universitet/runtimes/runtime-ERODE.product(4)/isabel/epsislon/BIOMD0000000030.ode");
		double epsilon=2;

		String fileName="/Users/andrea/OneDrive - Danmarks Tekniske Universitet/runtimes/runtime-ERODE.product(4)/isabel/epsislon/matalbScriptTwoConsole.m";
		LinkedHashSet<String> paramsToPerturb=new LinkedHashSet<String>();
		paramsToPerturb.add("ALL");
		/*paramsToPerturb.add("h1");
		paramsToPerturb.add("h10");*/
		String prePartitionUserDefined = "false";//"true"
		String prePartitionWRTIC = "false";//true
		boolean forward =false;
		boolean backward = true;
		boolean fastDegreeOneBE=true;
		erode.approximateDE(fileName, paramsToPerturb,epsilon, prePartitionUserDefined, prePartitionWRTIC, forward, backward,fastDegreeOneBE);
		
		//simulateODE(tEnd=1000, steps=100, viewPlot = NO,library=APACHE,csvFile="csvFile")
		double tEnd = 1000;
		int steps = 100;
		String csvFile = "/Users/andrea/OneDrive - Danmarks Tekniske Universitet/runtimes/runtime-ERODE.product(4)/isabel/epsislon/csvFileConsole";
		erode.simulateODE(tEnd, steps, csvFile);
		
	}

}
