package it.imt.erode.importing.automaticallygeneratedmodels;

import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.Date;
import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;

import org.eclipse.ui.console.MessageConsoleStream;
import it.imt.erode.crn.implementations.CRN;
import it.imt.erode.crn.implementations.CRNReaction;
import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.implementations.Species;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.expression.evaluator.MathEval;
import it.imt.erode.importing.AbstractImporter;

public class RandomlyConnectedXYInfluenceNetwork {
	
	public ICRN createXYInfluenceNetwork(int N,String suffix,MessageConsoleStream out, BufferedWriter bwOut){	
		//Compute promotions and inhibitions
		RandomEngine randomGenerator = new MersenneTwister(new Date());
		//RandomBNG.nextInt(randomGenerator, N);
		int[] yinhibites=new int[N];
		int[] xpromotes=new int[N];
		computePromotionsAndInhibitions(N, randomGenerator, yinhibites, xpromotes);
		return createXYInfluenceNetwork(N, suffix, out, bwOut, yinhibites, xpromotes);
	}

	public ICRN createXYInfluenceNetwork(int N,String suffix,MessageConsoleStream out, BufferedWriter bwOut,int[] yinhibites,int[] xpromotes){

		//Generate the CRN
		MathEval math = new MathEval();
		String name="XYNetwork"+N+suffix;
		ICRN crn = new CRN(name, math, out, bwOut);

		String[] names=new String[10];
		names[0]="a";
		names[1]="b";
		names[2]="c";
		names[3]="d";
		names[4]="e";
		names[5]="f";
		names[6]="g";
		names[7]="h";
		names[8]="i";
		names[9]="l";

		//Create species
		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>();
		int id=0;

		for(int i=0;i<N;i++){
			String speciesName="x"+names[i]+"0";
			ISpecies species = new Species(speciesName, id, BigDecimal.ONE, "1",false);
			crn.addSpecies(species);
			speciesNameToSpecies.put(speciesName, species);
			id++;

			speciesName="x"+names[i]+"1";
			species = new Species(speciesName, id, BigDecimal.ZERO, "0",false);
			crn.addSpecies(species);
			speciesNameToSpecies.put(speciesName, species);
			id++;

			speciesName="x"+names[i]+"2";
			species = new Species(speciesName, id, BigDecimal.ZERO, "0",false);
			crn.addSpecies(species);
			speciesNameToSpecies.put(speciesName, species);
			id++;


			speciesName="y"+names[i]+"0";
			species = new Species(speciesName, id, BigDecimal.ZERO, "0",false);
			crn.addSpecies(species);
			speciesNameToSpecies.put(speciesName, species);
			id++;

			speciesName="y"+names[i]+"1";
			species = new Species(speciesName, id, BigDecimal.ZERO, "0",false);
			crn.addSpecies(species);
			speciesNameToSpecies.put(speciesName, species);
			id++;

			speciesName="y"+names[i]+"2";
			species = new Species(speciesName, id, BigDecimal.ONE, "1",false);
			crn.addSpecies(species);
			speciesNameToSpecies.put(speciesName, species);
			id++;
		}

		//Create reactions
		for(int i=0;i<N;i++){
			//xa promotes yb
			// yb2 + xa0 -> xa0 + yb1 , 1.0
			IComposite reagents=computeComposite(speciesNameToSpecies,i,0,xpromotes[i],2,crn,true,names);
			IComposite products=computeComposite(speciesNameToSpecies,i,0,xpromotes[i],1,crn,false,names);
			ICRNReaction firstPromotion = new CRNReaction(BigDecimal.ONE , reagents,products, "1",null);
			AbstractImporter.addReaction(crn,2,firstPromotion);
			// yb1 + xa0 -> xa0 + yb0 , 1.0
			reagents=computeComposite(speciesNameToSpecies,i,0,xpromotes[i],1,crn,true,names);
			products=computeComposite(speciesNameToSpecies,i,0,xpromotes[i],0,crn,false,names);
			ICRNReaction secondPromotion = new CRNReaction(BigDecimal.ONE , reagents,products, "1",null);
			AbstractImporter.addReaction(crn,2,secondPromotion);



			//yc inhibites xc
			//xc0 + yc0 -> yc0 + xc1 , 1.0
			reagents=computeComposite(speciesNameToSpecies,yinhibites[i],0,i,0,crn,true,names);
			products=computeComposite(speciesNameToSpecies,yinhibites[i],1,i,0,crn,false,names);
			ICRNReaction firstInhibition = new CRNReaction(BigDecimal.ONE , reagents,products, "1",null);
			AbstractImporter.addReaction(crn,2,firstInhibition);

			//xc1 + yc0 -> yc0 + xc2 , 1.0
			reagents=computeComposite(speciesNameToSpecies,yinhibites[i],1,i,0,crn,true,names);
			products=computeComposite(speciesNameToSpecies,yinhibites[i],2,i,0,crn,false,names);
			ICRNReaction secondInhibition = new CRNReaction(BigDecimal.ONE , reagents,products, "1",null);
			AbstractImporter.addReaction(crn,2,secondInhibition);
		}

		/*

		 //yc inhibites xc
		 xc0 + yc0 -> yc0 + xc1 , 1.0
		 xc1 + yc0 -> yc0 + xc2 , 1.0

		 //xa promotes yb
		 xa0 + yb2 -> xa0 + yb1 , 1.0
		 xa0 + yb1 -> xa0 + yb0 , 1.0

		 */

		/*System.out.println(crn.toStringShort());
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println(crn.toString());*/
		return crn;

	}

	private void computePromotionsAndInhibitions(int N, RandomEngine randomGenerator, int[] yinhibites,
			int[] xpromotes) {
		ArrayList<Integer> promotions=new ArrayList<>(N);
		ArrayList<Integer> inhibitions=new ArrayList<>(N);
		for(int i=0;i<N;i++){
			promotions.add(i);
			inhibitions.add(i);
		}
		for(int i=0;i<N;i++){

			int posOfIn=RandomBNG.nextInt(randomGenerator, (N-1)-i);
			int in =inhibitions.remove(posOfIn);
			yinhibites[i]=in;

			int posOfProm=RandomBNG.nextInt(randomGenerator, (N-1)-i);
			int prom =promotions.remove(posOfProm);
			xpromotes[i]=prom;
		}
	}

	private IComposite computeComposite(HashMap<String, ISpecies> speciesNameToSpecies, int xindex, int xstatus,int yindex, int ystatus,ICRN crn,boolean reagents, String[] names) {
		ISpecies x = speciesNameToSpecies.get("x"+names[xindex]+xstatus);
		ISpecies y = speciesNameToSpecies.get("y"+names[yindex]+ystatus);
		IComposite c =  new Composite(x, y);
		if(reagents){
//			if(CRNReducerCommandLine.univoqueReagents){
//				c = crn.addReagentsIfNew(c);
//			}
		}
		else{
//			if(CRNReducerCommandLine.univoqueProducts){
//				c = crn.addProductIfNew(c);
//			}
		}

		return c;
	}

}
