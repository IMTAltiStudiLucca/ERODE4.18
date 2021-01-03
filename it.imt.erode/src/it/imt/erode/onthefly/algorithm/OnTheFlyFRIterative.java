package it.imt.erode.onthefly.algorithm;

import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.onthefly.Pair;
import it.imt.erode.onthefly.linkedlist.MyLinkedList;
import it.imt.erode.onthefly.upto.IdentityUpToMembershipChecker;
import it.imt.erode.onthefly.upto.UpToType;
import it.imt.erode.partitionrefinement.splittersbstandcounters.CoefficientToMonomialsPosNeg;
import it.imt.erode.partitionrefinement.splittersbstandcounters.VectorOfCoefficientsForEachMonomial;

public class OnTheFlyFRIterative extends OnTheFlyCommonAbstract{

	public ArrayList<Pair> onTheFlyFR(
			ICRN crn,
			LinkedHashSet<Pair> query,
			LinkedHashSet<Pair> constraints, UpToType upTo,boolean avoidUnbalancedPairs,
			MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator, IMessageDialogShower msgDialogShower, boolean verbose
			, boolean printIntermediate) {

		boolean extraTab=false;
		String pref = "";
		if(extraTab) {
			pref="\t\t";
		}
		
		OnTheFlyBRIterative.printOpeningText(query, constraints, upTo, out, bwOut,"FR");

		CRNReducerCommandLine.print(out,bwOut,"\n"+pref+"\tScanning the reactions once to pre-compute informations necessary to the algorithm ... ");
		long begin = System.currentTimeMillis();
		try {
			computeMonomials(crn);
		} catch (UnsupportedFormatException e) {
			CRNReducerCommandLine.printWarning(out,bwOut,"Problems in computing the monomials of the species. I terminate."+e.getMessage());
			return null;
		}
		
		long end = System.currentTimeMillis();
		CRNReducerCommandLine.println(out,bwOut,"completed in "+ String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0))+" (s)");
		
		LinkedHashMap<IComposite, LinkedHashMap<Pair, BigDecimal>> totalDer=computeTotalDerivativeJ(crn,speciesToMonomialsNotSeparated);
		if(verbose) {
			CRNReducerCommandLine.println(out, bwOut,"The list of Ji:");
			printTotalDer(crn.getSpecies(),totalDer,out,bwOut,"Jn");
		}
		int totEntries=0;
		for(LinkedHashMap<Pair, BigDecimal> a:totalDer.values()) {
			totEntries+=a.size();
		}
//		LinkedHashSet<LinkedHashMap<Pair, BigDecimal>> jacobians = new LinkedHashSet<>(totalDer.values());
//		int totEntriesClean=0;
//		for(LinkedHashMap<Pair, BigDecimal> a:jacobians) {
//			totEntriesClean+=a.size();
//		}
		CRNReducerCommandLine.println(out,bwOut,"\t"+totalDer.size()+" Jacobians have been computed containing in total "+totEntries+" non-empty entries.");
		
		LinkedHashMap<IComposite, LinkedHashMap<Pair, BigDecimal>> totalDerTransp=transposeTotalDer(totalDer);
		//LinkedHashMap<IComposite, LinkedHashMap<Pair, BigDecimal>> totalDerTransp=transposeTotalDer(jacobians);
		
		if(verbose) {
			CRNReducerCommandLine.println(out, bwOut,"The list of transposed Ji:");
			printTotalDer(crn.getSpecies(),totalDerTransp,out,bwOut,"JnT");
		}
		
		ArrayList<HashMap<ISpecies, VectorOfCoefficientsForEachMonomial>> speciesToMonomialsNotSepForEachJT = computeDriftsForEachJk(totalDerTransp);
		//LinkedHashSet<HashMap<ISpecies, VectorOfCoefficientsForEachMonomial>> speciesToMonomialsNotSepForEachJT_set = new LinkedHashSet<HashMap<ISpecies,VectorOfCoefficientsForEachMonomial>>(speciesToMonomialsNotSepForEachJT); 
		ArrayList<HashMap<ISpecies, CoefficientToMonomialsPosNeg>> speciesToMonomialsForEachJT = new ArrayList<>(speciesToMonomialsNotSepForEachJT.size());
		for(HashMap<ISpecies, VectorOfCoefficientsForEachMonomial> speciesToMonomialNotSep : speciesToMonomialsNotSepForEachJT) {
			HashMap<ISpecies, CoefficientToMonomialsPosNeg> speciesToMonomials = new HashMap<ISpecies, CoefficientToMonomialsPosNeg>(speciesToMonomialNotSep.size());
			speciesToMonomialsForEachJT.add(speciesToMonomials);
			for(Entry<ISpecies, VectorOfCoefficientsForEachMonomial> entry : speciesToMonomialNotSep.entrySet()) {
				speciesToMonomials.put(entry.getKey(), new CoefficientToMonomialsPosNeg(entry.getValue()));
			}
		}
		
		MyLinkedList<Pair> R =  OnTheFlyBRIterative.performOnTheFlyBR(
				query, constraints, out, bwOut, terminator, new IdentityUpToMembershipChecker(), 
				speciesToMonomialsForEachJT, speciesToMonomialsNotSepForEachJT, numberOfTranspProblemsSolved,avoidUnbalancedPairs,printIntermediate);
		
		ArrayList<Pair> computedFR=new ArrayList<Pair>(R.size());
		R.reset();
		while(R.hasNext()) {
			Pair v =R.moveToNextNode();
			computedFR.add(v);
		}
		
		return computedFR;
	}

	public ArrayList<HashMap<ISpecies, VectorOfCoefficientsForEachMonomial>> computeDriftsForEachJk(
			LinkedHashMap<IComposite, LinkedHashMap<Pair, BigDecimal>> totalDerTransp) {
		ArrayList<HashMap<ISpecies, VectorOfCoefficientsForEachMonomial>> speciesToMonomialsForEachJT= new ArrayList<HashMap<ISpecies,VectorOfCoefficientsForEachMonomial>>(totalDerTransp.size());
		for(LinkedHashMap<Pair, BigDecimal> Jk : totalDerTransp.values()) {
			HashMap<ISpecies, LinkedHashMap<IComposite, BigDecimal>> speciesToMonHM_Jk = new LinkedHashMap<>(Jk.size());
			for(Entry<Pair, BigDecimal> entry : Jk.entrySet()) {
				//Jk(i,j) = c is transformed into d(x_i) = c * x_j 
				ISpecies xi =(ISpecies)entry.getKey().getFirst();
				ISpecies xj =(ISpecies)entry.getKey().getSecond();
				BigDecimal c = entry.getValue();
				if(c.compareTo(BigDecimal.ZERO)!=0) {
					add(c,xj,speciesToMonHM_Jk,xi);
				}
			}
			HashMap<ISpecies, VectorOfCoefficientsForEachMonomial> speciesToMononialsOfJk = new HashMap<ISpecies, VectorOfCoefficientsForEachMonomial>(speciesToMonHM_Jk.size());
			speciesToMonomialsForEachJT.add(speciesToMononialsOfJk);
			for(Entry<ISpecies, LinkedHashMap<IComposite, BigDecimal>> entry : speciesToMonHM_Jk.entrySet()) {
				speciesToMononialsOfJk.put(entry.getKey(), new VectorOfCoefficientsForEachMonomial(entry.getValue()));
			}
		}
		return speciesToMonomialsForEachJT;
	}

	private void add(BigDecimal c, ISpecies xj, 
			HashMap<ISpecies, LinkedHashMap<IComposite, BigDecimal>> speciesToMonHM_Jk,
			ISpecies xi) {
		//Jk(i,j) = c is transformed into d(x_i) = c * x_j 
		LinkedHashMap<IComposite, BigDecimal> monToCoeff = speciesToMonHM_Jk.get(xi);
		if(monToCoeff==null) {
			monToCoeff= new LinkedHashMap<IComposite, BigDecimal>();
			speciesToMonHM_Jk.put(xi, monToCoeff);
		}
		IComposite monomial = (IComposite)xj;
		BigDecimal prevCoeff = monToCoeff.get(monomial);
		BigDecimal newCoeff = (prevCoeff==null)?c:c.add(prevCoeff);
		monToCoeff.put(monomial, newCoeff);
		
	}

	private LinkedHashMap<IComposite, LinkedHashMap<Pair, BigDecimal>> transposeTotalDer(LinkedHashMap<IComposite, LinkedHashMap<Pair, BigDecimal>> totalDer) {
		LinkedHashMap<IComposite, LinkedHashMap<Pair, BigDecimal>> totalDerTransp = new LinkedHashMap<IComposite, LinkedHashMap<Pair,BigDecimal>>(totalDer.size());
		
		for(Entry<IComposite, LinkedHashMap<Pair, BigDecimal>> entry : totalDer.entrySet()) {
			IComposite n = entry.getKey();
			LinkedHashMap<Pair, BigDecimal> Jn = entry.getValue();
			if(!Jn.isEmpty()) {
				LinkedHashMap<Pair, BigDecimal> JnT = new LinkedHashMap<Pair, BigDecimal>(Jn.size());
				totalDerTransp.put(n, JnT);
				for(Entry<Pair, BigDecimal> JnEntry : Jn.entrySet()) {
					Pair p= JnEntry.getKey();
					JnT.put(new Pair(p.getSecond(),p.getFirst()), JnEntry.getValue());
				}
			}
		}
		
		return totalDerTransp;
		
	}

	private void printTotalDer(List<ISpecies> species, LinkedHashMap<IComposite, LinkedHashMap<Pair, BigDecimal>> totalDer,MessageConsoleStream out, BufferedWriter bwOut, String name) {
		for(Entry<IComposite, LinkedHashMap<Pair, BigDecimal>> entry: totalDer.entrySet()) {
			IComposite n = entry.getKey();
			CRNReducerCommandLine.println(out, bwOut, "\n"+name+" of "+n);
			LinkedHashMap<Pair, BigDecimal> Jn = entry.getValue();
			for(int i=0;i<species.size();i++) {
				ISpecies xi=species.get(i);
				for(int j=0;j<species.size();j++) {
					ISpecies xj=species.get(j);
					BigDecimal val=Jn.get(new Pair(xi,xj));
					if(val==null) {
						CRNReducerCommandLine.print(out, bwOut, " 0.0");
					}
					else {
						if(val.compareTo(BigDecimal.ZERO)>0) {
							CRNReducerCommandLine.print(out, bwOut, " ");
						}
						CRNReducerCommandLine.print(out, bwOut, val.toString());
					}
					if(j==species.size()-1) {
						CRNReducerCommandLine.print(out, bwOut, ";\n");
					}
					else {
						CRNReducerCommandLine.print(out, bwOut, ",");
					}
				}
				
			}
			//CRNReducerCommandLine.println(out, bwOut, "\n");
		}
		
	}

	public static LinkedHashMap<IComposite, LinkedHashMap<Pair, BigDecimal>> computeTotalDerivativeJ(ICRN crn,HashMap<ISpecies, VectorOfCoefficientsForEachMonomial> speciesToMonomialsNotSeparated) {
		//HashSet<IComposite> M = new HashSet<IComposite>();
		VectorOfCoefficientsForEachMonomial zeroDrift = new VectorOfCoefficientsForEachMonomial();
		//We represent the total derivative of a drift as a sum \sum_{k=1}^k m_k(v)*J^{(k)}
		//	totalDer contains all the addendum of the sum, associating each monomial appearing as first multiplicand with the matrix J^{(k)}
		LinkedHashMap<IComposite, LinkedHashMap<Pair, BigDecimal>> totalDer=new LinkedHashMap<>();
		for(int i=0;i<crn.getSpecies().size();i++) {
			ISpecies xi = crn.getSpecies().get(i);
			VectorOfCoefficientsForEachMonomial fxi = speciesToMonomialsNotSeparated.get(xi);
			if(fxi==null) {
				fxi=zeroDrift;
			}
			int Li=fxi.numberOfMonomials();
			for(int j=0;j<crn.getSpecies().size();j++) {
				ISpecies xj = crn.getSpecies().get(j);
				VectorOfCoefficientsForEachMonomial fxj = speciesToMonomialsNotSeparated.get(xj);
				if(fxj==null) {
					fxj=zeroDrift;
				}
				for(int l=0;l<Li;l++) {
					IComposite mil = fxi.getMonomial(l);
					BigDecimal alphail = fxi.getCoefficient(l);
					int milxj=mil.getMultiplicityOfSpecies(xj);
					if(milxj>0) {
						IComposite n = mil.copyDecreasingMultiplicityOf(xj);
						/*
						IComposite n=new Composite(mil);
						int posj=n.getPosOfSpecies(xj);
						n.setMultiplicities(posj, n.getMultiplicities(posj)-1);
						 */
						LinkedHashMap<Pair, BigDecimal> Jn = totalDer.get(n);
						if(Jn==null) {
							//M.add(n);
							Jn= new LinkedHashMap<Pair, BigDecimal>();
							totalDer.put(n, Jn);
						}
						Pair p = new Pair(xi,xj);
						BigDecimal Jnxixj = Jn.get(p);
						if(Jnxixj==null) {
							Jnxixj=BigDecimal.ZERO;
						}
						Jnxixj=Jnxixj.add(BigDecimal.valueOf(milxj).multiply(alphail));
						Jn.put(p, Jnxixj);
					}
				}


			}
		}
		return totalDer;
	}
}
