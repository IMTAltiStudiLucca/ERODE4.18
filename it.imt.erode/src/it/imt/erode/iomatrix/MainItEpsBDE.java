package it.imt.erode.iomatrix;

import java.io.IOException;
import java.util.LinkedHashSet;

import com.microsoft.z3.Z3Exception;

import java.io.FileWriter;

import it.imt.erode.auxiliarydatastructures.Reduction;
import it.imt.erode.commandline.EntryPointForMatlab;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.importing.MatlabODEsImporter;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.partitionrefinement.algorithms.AXB;

//Matlab 66        1190


public class MainItEpsBDE {
	
	
    public static void main(String[] args) throws IOException, Z3Exception, UnsupportedFormatException {
    	
    	boolean printPartitions=false;
    	boolean printCRNs=false;
    	// number of species
        // 3 for exampleData
    	// 34 for ITAData.csv
        // 2484 for ICIOData
    	//int N = 3;
    	//int N = 2484;
    	int N=34;
    	
    	CRNBuilder c = new CRNBuilder();
    	// Build the matrix from a .csv created by python
    	//double[][] matrix = c.readCsv("IOMatrix/exampleData.csv",N);
    	//double[][] matrix = c.readCsv("IOMatrix/ICIOData.csv",N);
    	//double[][] matrix = c.readCsv("IOMatrix/exampleDataC.csv",N);
    	double[][] matrix = c.readCsv("IOMatrix/ITAData.csv",N);
    	ICRN crn = c.createCRN(N,matrix);    	
    	
    	//EntryPointForMatlab entry = new EntryPointForMatlab(printPartitions,printCRNs);
		EntryPointForMatlab entry = new EntryPointForMatlab(printPartitions,printCRNs,crn,false);
    	//entry.load("ITA-RN.ode");
    	
    	// initial partition done by hand based on similar demand
    	// ICIO
		
		// Test: every element in the same partition, just for now
		//int[] initialPartitionArray = new int[N];
		//NO NEED FOR THIS. 
//		for(int i=0; i< N;i++) {
//			
//			initialPartitionArray[i] = 1;
//			
//		}
		
		boolean oneEps=true;
		if(oneEps) {
			int[] initialPartitionArray = {1,2,4,5,6,7,8,9,7,10,1,11,12,1,1,8,7,8,8,13,14,12,5,7,11,15,3,7,11,13,5,13,16,7};
			boolean addConstraintsOnIC=true;
			boolean solveSystem=false;
			boolean printM=false;
			boolean printSolution=false;
			LinkedHashSet<String> paramsToPerturb=new LinkedHashSet<String>(1);
			paramsToPerturb.add("ALL");
			MatlabODEsImporter.parseParametersToPerturb(entry.getCRN(), paramsToPerturb);

			int[] obtainedPartitionArray=entry.computeEpsBE(0.1, initialPartitionArray, false);
			AXB axb=entry.computeReferenceTrajectory(Reduction.ENBB, obtainedPartitionArray, addConstraintsOnIC, solveSystem, paramsToPerturb, printM, printSolution);
			
			if(solveSystem) {
				double[] solution = axb.getSolution();
				int i=0;
				for(String param : paramsToPerturb) {
					System.out.println("Parameter "+param + " from value "+entry.getCRN().getMath().evaluate(param)+" to "+ solution[i]);
					i++;
					entry.getCRN().setParameter(param, String.valueOf(solution[i]));
				}
			}
		}
		else {
			int[] initialPartitionArray = new int[N];
			// You can skip this
			// ITA
			//int[] initialPartitionArray = {1,2,4,5,6,7,8,9,7,10,8,11,12,1,1,8,7,8,8,13,14,12,5,7,11,15,3,7,11,13,5,13,16,7,17};
			//int[] initialPartitionArray = {1,2,4,5,6,7,8,9,7,10,1,11,12,1,1,8,7,8,8,13,14,12,5,7,11,15,3,7,11,13,5,13,16,7,17};
			// ITA without demand parameter
			//int[] initialPartitionArray = {1,2,4,5,6,7,8,9,7,10,1,11,12,1,1,8,7,8,8,13,14,12,5,7,11,15,3,7,11,13,5,13,16,7};    	
			// GER
			//int[] initialPartitionArray = {1,2,3,1,2,4,4,3,4,5,6,7,8,9,9,10,4,1,11,8,10,9,11,4,12,10,2,1,12,13,3,14,15,2,16};
			// GER without demand
			//int[] initialPartitionArray = {1,2,3,1,2,4,4,3,4,5,6,7,8,9,9,10,4,1,11,8,10,9,11,4,12,10,2,1,12,13,3,14,15,2};
			// AUS
			//int[] initialPartitionArray = {1,4,5,2,1,7,3,8,3,2,9,8,6,7,8,9,4,7,8,10,11,6,5,3,9,10,4,1,9,10,6,10,9,12,13};
			// AUS without demand
			//int[] initialPartitionArray = {1,4,5,2,1,7,3,8,3,2,9,8,6,7,8,9,4,7,8,10,11,6,5,3,9,10,4,1,9,10,6,10,9,12};


			// Only few steps for testing
			float epsilon = 0.1f;
			float step = 0.1f;
			float threshold = 0.1f;

			int[] obtainedPartition=null;

			// File for debugging
			FileWriter myWriter = new FileWriter("prova.txt");

			int m = N;
			int actualm = N;
			System.out.println(actualm);
			myWriter.write("Partizione iniziale \n");
			// Write initial partition in the file
			writeFile(myWriter,initialPartitionArray);
			//stampa(initialPartitionArray);

			while(epsilon<=threshold){
				// computed the new partition
				obtainedPartition=entry.computeEpsBE(epsilon, initialPartitionArray, false);
				myWriter.write("Obtained Partition Epsilon "+epsilon+" \n");           	
				//stampa(obtainedPartition);
				writeFile(myWriter,obtainedPartition);
				// if is not the last iteration
				if(epsilon + step <= threshold) {
					// for every block of the initial partition (At maximum 35 blocks)           	
					for(int i=0; i<N;i++) {
						int count=0;
						// for every element of the array obtainedPartition
						for(int j=0;j<N;j++) {
							// Count the number of element in a block
							if(i+1==obtainedPartition[j]&& initialPartitionArray[j]<m) {
								count=count+1;
							}

						}
						// if the block is not a singlet
						if(count>1) {

							for(int j=0;j<N;j++) {
								// set up the block in the initial partition
								if(i+1==obtainedPartition[j]) {
									initialPartitionArray[j]=actualm;
								}

							}           			
							actualm=actualm+1;

						}


					}
				}
				// if is the last iteration
				else {
					System.out.println("Ultima iterazione");
					initialPartitionArray = obtainedPartition;

				}
				stampa(initialPartitionArray);

				myWriter.write("New initial partition \n");
				writeFile(myWriter,initialPartitionArray);
				myWriter.write("\n ###################################################################################################################################################################################################### \n");

				epsilon = epsilon+step;

			}
			myWriter.close();
			System.out.println("FINE");
		}

    }
    
    
    public static void stampa(int[] array) {
    	
    	String s = "";
    	for(int i=0; i<array.length;i++) {
    		
    		s = s+array[i]+" ";
    	}
    	System.out.println(s);
    }
    
    public static int max(int[] array) {
    	
    	int m = 0;
    	for(int i=0; i < array.length;i++) {
    		
    		if(array[i]>m) {
    			
    			m = array[i];
    			
    		}
    		
    	}
    	return m;
    	
    }

    public static void  writeFile(FileWriter myWriter, int[] array) throws IOException {
    	
        
    		boolean found = false;
            int m = max(array);
            for(int i =0; i<m;i++) {
            	found=false;
            	String s = "{ ";
            	for(int j = 0; j<array.length; j++) {
            		
            		if(i+1 == array[j]) {
            			
            			s = s+"x"+(j+1)+ " ";
            			found = true;
            		}
            		
            	}
            	s= s+"},";
            	if(found==true) {
            		myWriter.write(s);
            	}
            }
            myWriter.write("\n\n");
            System.out.println("Successfully wrote to the file.");

    	
    }
	

}
