package it.imt.erode.importing.automaticallygeneratedmodels;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;

import org.eclipse.ui.console.MessageConsoleStream;

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;
import it.imt.erode.crn.implementations.CRN;
import it.imt.erode.crn.implementations.CRNReactionArbitraryGUI;
import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.implementations.Species;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.expression.evaluator.MathEval;
import it.imt.erode.importing.C2E2Exporter;
import it.imt.erode.importing.CRNImporter;

public class HTreeCircuit {

	private static final boolean C2E2 = false;

	public static ICRN createHTreeCiruit(int N, double vs,double delta,MessageConsoleStream out, BufferedWriter bwOut,boolean perturbParameters,
			double percentageOfPerturbation, String suffix) throws IOException {
		double actualPerturbationFactor=percentageOfPerturbation/100;
		
		if(N>8){
			System.out.println("N can be at most 8.");
			return null;
		}
		MathEval math = new MathEval();
		String name="hTreeCircuit"+N+"_"+suffix;
		ICRN crn = new CRN(name, math, out, bwOut);
		
		int[] maxK= new int[N+1];
		for(int i=2;i<=N;i++){
			maxK[i]=(int)Math.pow(2.0, i-1);
		}
				
		
		/*
		BigDecimal[] R = new BigDecimal[9];
		BigDecimal scale = BigDecimal.valueOf(10).pow(-21);
		
		R[0]=BigDecimal.valueOf(-1);
		R[1]=BigDecimal.valueOf(3.00).multiply(scale);
		R[2]=BigDecimal.valueOf(6.00).multiply(scale);
		R[3]=BigDecimal.valueOf(12.00).multiply(scale);
		R[4]=BigDecimal.valueOf(24.00).multiply(scale);
		R[5]=BigDecimal.valueOf(48.00).multiply(scale);
		R[6]=BigDecimal.valueOf(96.00).multiply(scale);
		R[7]=BigDecimal.valueOf(192.00).multiply(scale);
		R[8]=BigDecimal.valueOf(384.00).multiply(scale);
		
		BigDecimal[] C = new BigDecimal[9];
		C[0]=BigDecimal.valueOf(-1);
		C[1]=BigDecimal.valueOf(0.3).multiply(scale);
		C[2]=BigDecimal.valueOf(0.3).multiply(scale);
		C[3]=BigDecimal.valueOf(0.15).multiply(scale);
		C[4]=BigDecimal.valueOf(0.15).multiply(scale);
		C[5]=BigDecimal.valueOf(0.075).multiply(scale);
		C[6]=BigDecimal.valueOf(0.075).multiply(scale);
		C[7]=BigDecimal.valueOf(0.038).multiply(scale);
		C[8]=BigDecimal.valueOf(0.038).multiply(scale);
		 */
		
		double[] R = new double[9];
		R[0]=-1;
		R[1]=3.19;
		R[2]=6.37;
		R[3]=12.75;
		R[4]=25.50;
		R[5]=50.00;
		R[6]=100.00;
		R[7]=200.00;
		R[8]=400.00;
		/*R[1]=3.00;
		R[2]=6.00;
		R[3]=12.00;
		R[4]=24.00;
		R[5]=48.00;
		R[6]=96.00;
		R[7]=192.00;
		R[8]=384.00;*/
		
		double[] C = new double[9];
		C[0]=-1;
		C[1]=0.280;
		C[2]=0.300;
		C[3]=0.130;
		C[4]=0.140;
		C[5]=0.070;
		C[6]=0.070;
		C[7]=0.035;
		C[8]=0.035;
		/*C[1]=0.3;
		C[2]=0.3;
		C[3]=0.15;
		C[4]=0.15;
		C[5]=0.075;
		C[6]=0.075;
		C[7]=0.038;
		C[8]=0.038;*/
		
		RandomEngine randomGenerator = new MersenneTwister(new Date());
		double[] Rp = R;
		double[] Cp = C;
		if(perturbParameters){
			Rp=perturbArray(actualPerturbationFactor, randomGenerator, R);
			Cp=perturbArray(actualPerturbationFactor, randomGenerator, C);
		}
				
		//Create parameters
		double b1=setNumberOfDigitsIfC2E2(vs);
		double b2=setNumberOfDigitsIfC2E2(1.0/(Rp[2]*Cp[1]));
		double b3=setNumberOfDigitsIfC2E2(1.0/(Rp[2]*Cp[1]));
		crn.addParameter("b1", String.valueOf(b1));
		crn.addParameter("b2", String.valueOf(b2));
		crn.addParameter("b3", String.valueOf(b3));
		
		double a11 = setNumberOfDigitsIfC2E2(1.0/(R[1]*C[1]));
		crn.addParameter("a"+suffix(1,1), String.valueOf(a11));
		
		for(int i=2;i<=N;i++){
			for(int k=1;k<=maxK[i];k++){
				Rp = R;
				Cp = C;
				if(perturbParameters){
					Rp=perturbArray(actualPerturbationFactor, randomGenerator, R);
					Cp=perturbArray(actualPerturbationFactor, randomGenerator, C);
				}
				double aik=setNumberOfDigitsIfC2E2(1.0/(Rp[i]*Cp[i]));
				crn.addParameter("a"+suffix(i,k), String.valueOf(aik));
			}
		}
		
		
		
		/*for (String param : crn.getParameters()) {
			int space = param.indexOf(' ');
			String paramName = param.substring(0, space);
			String paramExpr = param.substring(space,param.length());
			double val = Double.valueOf(paramExpr);
			System.out.println("   "+paramName+" in [ "+(val-delta)+" , "+(val+delta)+"]");
		}*/
		
		for (String param : crn.getParameters()) {
			int space = param.indexOf(' ');
			String paramName = param.substring(0, space);
			System.out.print(" "+paramName+" in [ "+paramName+"-"+delta+" , "+paramName+"+"+delta+"] , ");
		}
		
		//Create species
		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>();
		int id=0;
		{
			String speciesName="v"+suffix(1,1);
			ISpecies species = new Species(speciesName, id, BigDecimal.ZERO, "0",false);
			crn.addSpecies(species);
			speciesNameToSpecies.put(speciesName, species);
			id++;
		}
		for(int i=2;i<=N;i++){
			for(int k=1;k<=maxK[i];k++){
				String speciesName="v"+suffix(i,k);
				ISpecies species = new Species(speciesName, id, BigDecimal.ZERO, "0",false);
				crn.addSpecies(species);
				speciesNameToSpecies.put(speciesName, species);
				id++;
			}
		}
		
		//Create ODEs
		{
		//v1_1
			String speciesName="v"+suffix(1, 1);
			ISpecies species=speciesNameToSpecies.get(speciesName);
			String drift="a"+suffix(1, 1)+" * (b1 - v"+suffix(1, 1)+") - b2 * (v"+suffix(1, 1)+" - v"+suffix(2, 1)+") - b3 * (v"+suffix(1, 1)+" - v"+suffix(2, 2)+")";
			
			IComposite reagents=(IComposite)species;
			IComposite products=new Composite(species, species);
			ICRNReaction reaction = new CRNReactionArbitraryGUI((IComposite)species,products, drift,null);
			CRNImporter.addReaction(crn, reagents, products, reaction);
		}
		
		
		for(int i=2;i<=N;i++){
			for(int k=1;k<=maxK[i];k++){
				String speciesName="v"+suffix(i,k);
				ISpecies species=speciesNameToSpecies.get(speciesName);
				String a="a"+suffix(i,k);
				int l=(int)(((double)k)/2.0 + 0.5);
				String firstV="v"+suffix(i-1,l);
				String secondV="v"+suffix(i,k);
				
				String drift=a+" * ( "+firstV+" - "+secondV+" )";
				
				IComposite reagents=(IComposite)species;
				IComposite products=new Composite(species, species);
				ICRNReaction reaction = new CRNReactionArbitraryGUI((IComposite)species,products, drift,null);
				CRNImporter.addReaction(crn, reagents, products, reaction);
			}
		}

		return crn;
		
	}

	private static double setNumberOfDigitsIfC2E2(double d) {
		if(C2E2){
			return C2E2Exporter.setNumberOfDigits(d);
		}
		else{
			return d;
		}
	}

	public static double[] perturbArray(double actualPerturbationFactor, RandomEngine randomGenerator, double[] array) {
		double[] ret=new double[array.length];
		for (int i = 0; i < array.length; i++) {
			boolean sign = RandomBNG.nextBoolean(randomGenerator, 0.5);//true=+, false=-
			//boolean sign=true;
			double maxVariation=array[i]*actualPerturbationFactor;
			double perturbation = RandomBNG.nextDouble(randomGenerator, maxVariation);
			if(sign){
				ret[i]=array[i]+perturbation;
			}
			else{
				ret[i]=array[i]-perturbation;
			}
		}
		return ret;
	}

	public static String suffix(int level,int k){
		return "n"+level+"k"+k;
	}
}


