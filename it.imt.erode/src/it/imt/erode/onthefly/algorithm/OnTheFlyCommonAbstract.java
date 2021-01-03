package it.imt.erode.onthefly.algorithm;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;

import it.imt.erode.auxiliarydatastructures.TPCounter;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.expression.parser.IMonomial;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.onthefly.Pair;
import it.imt.erode.partitionrefinement.algorithms.EpsilonDifferentialEquivalences;
import it.imt.erode.partitionrefinement.splittersbstandcounters.CoefficientToMonomialsPosNeg;
import it.imt.erode.partitionrefinement.splittersbstandcounters.VectorOfCoefficientsForEachMonomial;

public class OnTheFlyCommonAbstract implements IOnTheFly {

	protected final LinkedHashSet<Pair> emptySupport= new LinkedHashSet<Pair>(0);
	protected int numberOfUpdates=0;
	protected int numberOfExpand=0;
	protected TPCounter numberOfTranspProblemsSolved;
	protected int numberOfPairsAddedToAdj=0;
	protected int numberOfPairsUpdatedInAdj=0;
	
	protected HashMap<ISpecies, CoefficientToMonomialsPosNeg> speciesToMonomials;
	protected HashMap<ISpecies, VectorOfCoefficientsForEachMonomial> speciesToMonomialsNotSeparated;
	protected static final CoefficientToMonomialsPosNeg emptyMonomials=new CoefficientToMonomialsPosNeg();
	
	public OnTheFlyCommonAbstract() {
		resetCounters();
	}
	
	public int getNumberOfUpdates() {
		return numberOfUpdates;
	}

	public int getNumberOfExpand() {
		return numberOfExpand;
	}

	public int getTranspProblemsSolved() {
		return numberOfTranspProblemsSolved.getTPSolved();
	}
	public int getTranspProblemsUnbalancedSkept() {
		return numberOfTranspProblemsSolved.getTPUnbalancedSkept();
	}

	public int getNumberOfPairsAddedToAdj() {
		return numberOfPairsAddedToAdj;
	}

	public int getNumberOfPairsUpdatedInAdj() {
		return numberOfPairsUpdatedInAdj;
	}
	
	public void resetCounters() {
		numberOfUpdates=0;
		numberOfExpand=0;
		numberOfTranspProblemsSolved=new TPCounter();
		numberOfPairsAddedToAdj=0;
		numberOfPairsUpdatedInAdj=0;
	}
	
	protected void computeMonomials(ICRN crn) throws UnsupportedFormatException {
		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
		for (ISpecies species : crn.getSpecies()) {
			speciesNameToSpecies.put(species.getName(),species);
		}
		LinkedHashMap<ISpecies, ArrayList<IMonomial>> speciesToMonomialODE = new LinkedHashMap<>(crn.getSpecies().size());
		//TODO: this can be made a lot more efficient by getting rid of RateLaw.
		EpsilonDifferentialEquivalences.computeMonomialsOfAllSpecies(crn,speciesToMonomialODE,speciesNameToSpecies,false);

		speciesToMonomialsNotSeparated = new HashMap<ISpecies, VectorOfCoefficientsForEachMonomial>(speciesToMonomialODE.size());
		speciesToMonomials = new HashMap<>(speciesToMonomialODE.size());
		for(Entry<ISpecies, ArrayList<IMonomial>> pair : speciesToMonomialODE.entrySet()) {
			ArrayList<IMonomial> monomials = pair.getValue();
			VectorOfCoefficientsForEachMonomial v = new VectorOfCoefficientsForEachMonomial(monomials);
			speciesToMonomialsNotSeparated.put(pair.getKey(), v);
			
			//CoefficientToMonomialsPosNeg vposAndNeg = new CoefficientToMonomialsPosNeg(monomials);
			CoefficientToMonomialsPosNeg vposAndNeg = new CoefficientToMonomialsPosNeg(v);
			speciesToMonomials.put(pair.getKey(), vposAndNeg);
			
		}
	}
	
	protected static BigDecimal[] toMultiplicitiesBD(IComposite m) {
		BigDecimal[] ret = new BigDecimal[m.getNumberOfDifferentSpecies()];
		for(int i=0;i<m.getNumberOfDifferentSpecies();i++) {
			ret[i]=BigDecimal.valueOf(m.getMultiplicities(i));
		}
		return ret;
	}
	
//	private double[] toMultiplicities(IComposite m) {
//		double[] ret = new double[m.getNumberOfDifferentSpecies()];
//		for(int i=0;i<m.getNumberOfDifferentSpecies();i++) {
//			ret[i]=m.getMultiplicities(i);
//		}
//		return ret;
//	}
}
