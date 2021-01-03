package it.imt.erode.iomatrix;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.math.BigDecimal;

import it.imt.erode.crn.implementations.CRN;

import it.imt.erode.crn.implementations.CRNMassActionReactionCompact;

import it.imt.erode.crn.implementations.Composite;

import it.imt.erode.crn.implementations.SpeciesCompact;

import it.imt.erode.crn.interfaces.ICRN;

import it.imt.erode.crn.interfaces.ICRNReaction;

import it.imt.erode.crn.interfaces.IComposite;

import it.imt.erode.crn.interfaces.ISpecies;

public class CRNBuilder {

	
	public double[][] readCsv(String name, int N) throws IOException {
		
		String row= "";
		BufferedReader csvReader = new BufferedReader(new FileReader(name));
		double [][] matrix = new double[N][N];
		//useless in Java
//		for(int i = 0; i<N;i++) {
//			
//			for(int j = 0; j<N;j++) {
//				
//				matrix[i][j] = 0.0;
//				
//			}
//			
//		}
		csvReader.readLine();
		while ((row = csvReader.readLine()) != null) {
		    String[] data = row.split(",");
		    int x = Integer.parseInt(data[0])-1;
		    int y = Integer.parseInt(data[1])-1;
		    matrix[x][y] = Double.parseDouble(data[2]);
		    //System.out.println(data[0]);
		    // do something with the data
		}
		csvReader.close();
		return matrix;
		
		
	}
	
	public ICRN createCRN(int N, double[][] matrix) {		

		int expectedParams=N*N;

		int expectedSpecies=N;

		int expectedReactions=N*N;

		ICRN crn = new CRN("RN",expectedParams,expectedSpecies,expectedReactions);



		//Add the parameters

		for(int i=0;i<N;i++) {

			for(int j=0;j<N;j++) {

				if(matrix[i][j]!=0) {
					String pName="a"+i+"_"+j;

					String pExpression=String.valueOf(matrix[i][j]);//any arithmetic expression of reals or previously declared parameters

					crn.addParameter(pName, pExpression, true);
				}

			}

		}



		//Add the species

		for(int id=0;id<N;id++) {

			//Species representation using low memory. You cannot set a spefici name. It will be sid

			//It is suggested to start ids at 0

			SpeciesCompact sp = new SpeciesCompact(id, BigDecimal.ZERO);



			//String name="species"+id;//Any legal name: do not use parenthesis or punctuation. For example, s_1_2 would work.

			//Species sp = new Species(name, id, BigDecimal.ZERO, "0");



			crn.addSpecies(sp);

		}



		//Add the reactions

		for(int i=0;i<N;i++) {

			ISpecies si =crn.getSpecies().get(i);
			for(int j=0;j<N;j++) {
				//boolean zeroParam=false;
				//Create a reaction that corresponds to: d(si) = ai_j*sj

				ISpecies sj =crn.getSpecies().get(j);



				IComposite reagents=(IComposite) sj;

				IComposite products=new Composite(si, sj);



				String pName="a"+i+"_"+j;

				double rate_double=0;
				try {
					rate_double =crn.getMath().evaluate(pName);
				}catch(Exception e) {
					//zeroParam=true;
					//System.out.println("Parameter "+pName+" not present");
				}

				if(rate_double!=0) {
					BigDecimal rate = BigDecimal.valueOf(rate_double);



					ICRNReaction reaction = new CRNMassActionReactionCompact(rate, reagents, products, pName);

					//use new CRNMassActionReactionCompact(rate, reagents, products); if you don't need to store info on the rate expression



					crn.addReaction(reaction);
				}

			}

		}

		return crn;

	}

	
		
	
	/*
	
    public stati0c void main(String[] args) throws IOException, Z3Exception, UnsupportedFormatException  {

    	CRNBuilder c = new CRNBuilder();
    	double[][] matrix = c.readCsv("ITAData.csv",34);
    	ICRN crn = c.createCRN(34,matrix);
   
		boolean printPartitions=false;

		boolean printCRNs=false;

		EntryPointForMatlab entry = new EntryPointForMatlab(printPartitions,printCRNs,crn);



		//Create prepartition: all species with same 'number' will belong to the same block

		//int[] initialPartitionArray = new int[entry.getSpecies().size()];

		//Arrays.fill(initialPartitionArray, 1);//all in the same block
        int [] initialPartitionArray = {1,2,4,5,6,7,8,9,7,10,1,11,12,1,1,8,7,8,8,13,14,12,5,7,11,15,3,7,11,13,5,13,16,7};    	



		//I want the first species to be in its own block.  

		//initialPartitionArray[0]=2;	//Just comment this line if you want to use the trivial initial partition



		double epsilon=0.1;

		int[] obtainedPartition=entry.computeEpsBE(epsilon, initialPartitionArray, false);

		System.out.println("Completed with: "+Arrays.toString(obtainedPartition));
	

    	
    }
	*/
}
