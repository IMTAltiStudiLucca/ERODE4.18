package it.imt.erode.importing.automaticallygeneratedmodels;

import java.util.Date;

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;

public class Sampler {

	public static void main(String[] args) {
		RandomEngine randomGenerator = new MersenneTwister(new Date());
		System.out.println("2: "+RandomBNG.nextDouble(randomGenerator, 1.12E-3));
		System.out.println("3: "+RandomBNG.nextDouble(randomGenerator, 8.86E-4));
		System.out.println("4: "+RandomBNG.nextDouble(randomGenerator, 6.67E-4));
		System.out.println("5: "+RandomBNG.nextDouble(randomGenerator, 6.67E-4));
		System.out.println("6: "+RandomBNG.nextDouble(randomGenerator, 6.67E-4));
		System.out.println("7: "+RandomBNG.nextDouble(randomGenerator, 6.67E-4));
		System.out.println("8: "+RandomBNG.nextDouble(randomGenerator, 6.67E-4));
	}

}
