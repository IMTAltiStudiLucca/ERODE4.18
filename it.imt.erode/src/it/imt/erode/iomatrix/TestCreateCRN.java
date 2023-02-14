package it.imt.erode.iomatrix;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashSet;

import com.microsoft.z3.Z3Exception;

import it.imt.erode.auxiliarydatastructures.Reduction;
import it.imt.erode.commandline.EntryPointForMatlab;
import it.imt.erode.crn.implementations.CRN;
import it.imt.erode.crn.implementations.CRNMassActionReactionCompact;
import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.implementations.SpeciesCompact;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.importing.MatlabODEsImporter;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.partitionrefinement.algorithms.AXB;

public class TestCreateCRN {

	public static void main(String[] args) throws Z3Exception, UnsupportedFormatException, IOException {
		//All indexes go from 0 to N-1
		//so, a0_0 refers to entry (1,1) in the matrix


		//Create the CRN
		int N=2;
		int expectedParams=N*N;
		int expectedSpecies=N;
		int expectedReactions=N*N;
		ICRN crn = new CRN("pippo",expectedParams,expectedSpecies,expectedReactions);

		//Add the parameters
		for(int i=0;i<N;i++) {
			for(int j=0;j<N;j++) {
				String pName="p"+i+"_"+j;
				String pExpression="1";//any arithmetic expression of reals or previously declared parameters
				crn.addParameter(pName, pExpression, true);
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
				//d(si) = ai_j*sj
				ISpecies sj =crn.getSpecies().get(j);

				IComposite reagents=(IComposite) sj;
				IComposite products=new Composite(si, sj);

				String pName="p"+i+"_"+j;
				double rate_double =crn.getMath().evaluate(pName);
				BigDecimal rate = BigDecimal.valueOf(rate_double);

				ICRNReaction reaction = new CRNMassActionReactionCompact(rate, reagents, products, pName);
				//use new CRNMassActionReactionCompact(rate, reagents, products); if you don't need to store info on the rate expression

				crn.addReaction(reaction);
			}
		}

		/*
		 * pippo: 4 reactions, 2 species.
			Species:
			[S0, S1]
 			Reactions:
			[S0-- (p0_0) -->2*S0, S1-- (p0_1) -->S0+S1, S0-- (p1_0) -->S0+S1, S1-- (p1_1) -->2*S1]
		 */


		//Now use the CRN programmatically
		boolean printPartitions=false;
		boolean printCRNs=false;
		EntryPointForMatlab entry = new EntryPointForMatlab(printPartitions,printCRNs,crn,false);

		//Create prepartition: all species with same 'number' will belong to the same block
		int[] initialPartitionArray = new int[entry.getSpecies().size()];
		Arrays.fill(initialPartitionArray, 1);//all in the same block

		//I want the first species to be in its own block.  
		//initialPartitionArray[0]=2;	//Just comment this line if you want to use the trivial initial partition

		double epsilon=0.1;
		int[] obtainedPartition=entry.computeEpsBE(epsilon, initialPartitionArray, false);
		System.out.println("Completed with: "+Arrays.toString(obtainedPartition));
		
		LinkedHashSet<String> paramsToPerturb=new LinkedHashSet<String>(1);
		paramsToPerturb.add("ALL");
		MatlabODEsImporter.parseParametersToPerturb(entry.getCRN(), paramsToPerturb);
		boolean printM=true;
		boolean addConstraintsOnIC=false;
		boolean solveSystem=true;
		boolean printSolution=true;
		AXB axb = entry.computeReferenceTrajectory(Reduction.ENBB, obtainedPartition,addConstraintsOnIC,solveSystem,paramsToPerturb,printM,printSolution);
		double[] solution = axb.getSolution();
		int i=0;
		for(String param : paramsToPerturb) {
			System.out.println("Parameter "+param + " from value "+entry.getCRN().getMath().evaluate(param)+" to "+ solution[i]);
			i++;
			entry.getCRN().setParameter(param, String.valueOf(solution[i]));
		}
	}
}
