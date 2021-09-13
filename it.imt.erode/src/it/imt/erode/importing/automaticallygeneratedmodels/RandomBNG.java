package it.imt.erode.importing.automaticallygeneratedmodels;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;

import org.eclipse.ui.console.MessageConsoleStream;

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.crn.implementations.CRN;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.importing.BioNetGenImporter;

public class RandomBNG {

	public static final double maxParameterValue=10000;
	public static final int numberOfParameters=100;
	
	public static void createRandomNet(String path,int numberOfSpecies, int numberOfReactions, int maxNumberOfProducts,double nonLinearityFactor, int nlArity,String suffix) {
		String name=path+"random_S"+numberOfSpecies+"_R"+numberOfReactions+"_NLF"+nonLinearityFactor+"_NLArity"+nlArity+"_MP"+maxNumberOfProducts+"_"+suffix+".net";
		RandomBNG.printRandomCRNToNetFile(name, numberOfSpecies, numberOfReactions, nonLinearityFactor, nlArity, maxNumberOfProducts, numberOfParameters, maxParameterValue, null, null);
	}
	
	public static ICRN createRndPerturbedCopy(ICRN crn, MessageConsoleStream out, BufferedWriter bwOut,
			int maxPercPerturb) throws IOException {
		ICRN crnToConsider;
		crnToConsider = CRN.copyCRN(crn, out, bwOut);
		Date d = new Date();
		RandomEngine randomGenerator = new MersenneTwister(d);
		for(ICRNReaction reaction : crnToConsider.getReactions()) {
			boolean plus = RandomBNG.nextBoolean(randomGenerator, 0.5);
			int percentage =RandomBNG.nextInt(randomGenerator, maxPercPerturb);//nextDouble(randomGenerator, maxPercPerturb);
			//int percentage=(int) Math.round(dirtyPercentage);
			double factor=percentage/100.0;
			
			BigDecimal actualPert = BigDecimal.valueOf(factor).multiply(reaction.getRate());
			if(plus) {
				reaction.setRate(reaction.getRate().add(actualPert), reaction.getRateExpression()+"+"+actualPert.toPlainString());
			}
			else {
				reaction.setRate(reaction.getRate().subtract(actualPert), reaction.getRateExpression()+"-"+actualPert.toPlainString());
			}
			
		}
		return crnToConsider;
	}
	
	public static void randomlyPerturbIC(ICRN crn, MessageConsoleStream out, BufferedWriter bwOut,
			int maxPercPerturb) {
		Date d = new Date();
		RandomEngine randomGenerator = new MersenneTwister(d);
		for(ISpecies sp:crn.getSpecies()) {
			BigDecimal ic=sp.getInitialConcentration();
			String icExpr=sp.getInitialConcentrationExpr();
			boolean plus = RandomBNG.nextBoolean(randomGenerator, 0.5);
			int percentage =RandomBNG.nextInt(randomGenerator, maxPercPerturb);//nextDouble(randomGenerator, maxPercPerturb);
			double factor=percentage/100.0;
			BigDecimal actualPert = BigDecimal.valueOf(factor).multiply(ic);
			BigDecimal newVal=BigDecimal.ZERO;
			String newExpr;
			if(plus) {
				newVal=ic.add(actualPert);
				newExpr=icExpr+" + "+newVal.toPlainString();
			}
			else {
				newVal=ic.subtract(actualPert);
				newExpr=icExpr+" - "+newVal.toPlainString();
			}
			sp.setInitialConcentration(newVal, newExpr);
		}
//		for(String p:crn.getParameters()) {
//			String[] param = p.split("\\");
//			double val=crn.getMath().evaluate(param[0]);
//			boolean plus = RandomBNG.nextBoolean(randomGenerator, 0.5);
//			int percentage =RandomBNG.nextInt(randomGenerator, maxPercPerturb);//nextDouble(randomGenerator, maxPercPerturb);
//			//int percentage=(int) Math.round(dirtyPercentage);
//			double factor=percentage/100.0;
//			
//			double actualPert=factor*val;
//			String newVal=val+((plus)?"+ ":"- ")+actualPert;
//			crn.setParameter(param[0], newVal);
//		}
		
	}

	
	/**
	 * Randomly generates an elementary CRN, and writes it in a file 
	 * @param name
	 * @param numberOfSpecies
	 * @param numberOfReactions
	 * @param nonLinearityFactor probability of having a binary reaction
	 * @param maxNumberOfProducts
	 * @param numberOfParameters
	 * @param maxParameterValue
	 * @param out
	 * @param bwOut
	 */
	public static void printRandomCRNToNetFile(String name, int numberOfSpecies, int numberOfReactions, double nonLinearityFactor, int nlArity, int maxNumberOfProducts, int numberOfParameters, double maxParameterValue,MessageConsoleStream out,BufferedWriter bwOut){
		String fileName = name;
		fileName=BioNetGenImporter.overwriteExtensionIfEnabled(fileName,".net");
		int numberOfNary=0;
		int[] productsArityToItsOccurrencies = new int[maxNumberOfProducts+1]; 

		CRNReducerCommandLine.println(out,bwOut,"Creating random CRN with " +numberOfSpecies+" species, "+ numberOfReactions+" reactions "+nonLinearityFactor+" non-linearity factor, "+maxNumberOfProducts+" maximum number of products, and " +numberOfParameters+" parameters (max "+maxParameterValue+"). It will be written in: "+fileName);

		BioNetGenImporter.createParentDirectories(fileName);
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printRandomCRNToNetFile, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}

		try {
			Date d = new Date();
			//Date d2 = new Date();
			//int cmp = d.compareTo(d2);
			RandomEngine randomGenerator = new MersenneTwister(d);
			bw.write("begin parameters\n");
			for(int parameterId=1;parameterId<numberOfParameters+1;parameterId++){
				double paramValue = nextDouble(randomGenerator, maxParameterValue);
				//double paramValue=1;
				bw.write("    "+parameterId+" k"+parameterId+" "+((int)paramValue)+"\n");
			}

			bw.write("end parameters\n\n");

			bw.write("begin species\n");
			for(int id=0;id<numberOfSpecies;id++){
				int idSpeciesInNetFile=id+1;
				bw.write("    "+idSpeciesInNetFile+" s"+id+" 1\n");
			}
			bw.write("end species\n\n");

			bw.write("begin reactions\n");
			for(int id=0;id<numberOfReactions;id++){
				int idReactionInNetFile=id+1;
				boolean nary = !nextBoolean(randomGenerator, nonLinearityFactor);
				if(nary){
					numberOfNary++;
				}
				int arity = (nary)?nlArity:1;
				String reagents = computeRandomMultiSet(numberOfSpecies, randomGenerator, arity);
				arity = nextInt(randomGenerator,maxNumberOfProducts-1)+1;//At most three reagents
				productsArityToItsOccurrencies[arity]++;
				String products = computeRandomMultiSet(numberOfSpecies, randomGenerator, arity);
				int idRate = nextInt(randomGenerator, numberOfParameters-1)+1;
				bw.write("    "+idReactionInNetFile+" "+reagents+" "+ products+" k"+idRate+"\n");
			}
			bw.write("end reactions\n\n");
			
			bw.write("\n//Nary reactions: "+numberOfNary+".\n");
			for(int i=0;i<productsArityToItsOccurrencies.length;i++){
				bw.write("Reactions with "+i+" products: "+productsArityToItsOccurrencies[i]+"\n");
			}
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printToNetFile, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		finally{
			CRNReducerCommandLine.println(out,bwOut,"Writing reactions in file "+fileName+" completed.\n Binary reactions: "+numberOfNary+".");
			for(int i=0;i<productsArityToItsOccurrencies.length;i++){
				System.out.println(" Reactions with "+i+" products: "+productsArityToItsOccurrencies[i]);
			}
			try {
				bw.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in printToNetFile, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}
	}

	protected static String computeRandomMultiSet(int numberOfSpecies, RandomEngine randomGenerator, int arity) {
		int idSpecies = nextInt(randomGenerator, numberOfSpecies-1);
		int idSpeciesInNetFile = idSpecies+1;
		String multiset=String.valueOf(idSpeciesInNetFile);
		for(int i=1;i<arity;i++){
			idSpecies = nextInt(randomGenerator, numberOfSpecies-1);
			idSpeciesInNetFile = idSpecies+1;
			multiset+=","+idSpeciesInNetFile;
		}
		return multiset;
	}

	public static boolean nextBoolean(RandomEngine randomGenerator, double threshold) {
		//Sample from the open interval (0.0,1.0)
		double next = randomGenerator.nextDouble();
		//Skip the threshold case 
		while(next==threshold){
			next = randomGenerator.nextDouble();
		}
		return next > threshold;
	}
	
	/**
	 * 
	 * @param randomGenerator
	 * @param max
	 * @return a number pseudo-uniformly distributed in the interval [0,max]
	 */
	public static int nextInt(RandomEngine randomGenerator,int max) {
		//Sample from the open interval (0.0,1.0)
		double next = randomGenerator.nextDouble();
		double ret = next*(max+1);
		return (int)Math.round(ret);
	}
	
	/**
	 * 
	 * @param randomGenerator
	 * @param max
	 * @return a number pseudo-uniformly distributed in the interval (0,max)
	 */
	public static double nextDouble(RandomEngine randomGenerator,double max) {
		//Sample from the open interval (0.0,1.0)
		double next = randomGenerator.nextDouble();
		double ret = next*max;
		return ret;
	}	
	
	
	
	
}
