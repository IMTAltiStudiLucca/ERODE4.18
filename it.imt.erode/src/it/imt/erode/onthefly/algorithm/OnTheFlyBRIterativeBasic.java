package it.imt.erode.onthefly.algorithm;
import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
//import java.util.HashMap;
//import java.util.HashSet;
import java.util.LinkedHashSet;

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
import it.imt.erode.onthefly.linkedlist.Node;
import it.imt.erode.onthefly.transportationproblem.TransportationProblemBigDecimal;
import it.imt.erode.onthefly.upto.UpToType;
import it.imt.erode.partitionrefinement.splittersbstandcounters.CoefficientToMonomialsPosNeg;
import it.imt.erode.partitionrefinement.splittersbstandcounters.VectorOfCoefficientsForEachMonomial;

public class OnTheFlyBRIterativeBasic extends OnTheFlyCommonAbstract{
	
	//private HashMap<Pair, HashSet<Pair>> pairToPairsMappedToIByAdj;
	
	//private MyLinkedList<Pair> adjKeys;

	public ArrayList<Pair> onTheFlyBR(
			ICRN crn,
			LinkedHashSet<Pair> query,
			LinkedHashSet<Pair> constraints, UpToType upTo,
			MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator, IMessageDialogShower msgDialogShower) { 
		//pairToPairsMappedToIByAdj= new HashMap<Pair, HashSet<Pair>>();
		
		String qpairs=(query.size()==1)?" pair":" pairs";
		String qbarpairs=(constraints.size()==1)?" pair":" pairs";
		CRNReducerCommandLine.println(out,bwOut,"Computing on-the-fly BR for"+
		"\n\tQuery ("+ query.size()+qpairs+")="+query+
		"\n\tConstraints ("+constraints.size()+qbarpairs+")="+constraints);
		if(!upTo.equals(UpToType.NO)){
			CRNReducerCommandLine.println(out,bwOut,"\tUpTo "+upTo);
		}
		//TODO: use upTop
		
		if(!Collections.disjoint(query, constraints)) {
			CRNReducerCommandLine.printWarning(out,bwOut,"The query and the constraints are not disjoint. I terminate.");
			return null;
		}
		
		MyLinkedList<Pair> R = new MyLinkedList<Pair>(true);
		for(Pair p : query) {
			R.add(p);
		}
		MyLinkedList<Pair> Q = new MyLinkedList<Pair>(true);

		MyLinkedList<Pair> Rhat = new MyLinkedList<Pair>(true);
		for(Pair p : constraints) {
			Rhat.add(p);
		}
		MyLinkedList<Pair> Qhat = new MyLinkedList<Pair>(true);
		
		try {
			computeMonomials(crn);
		} catch (UnsupportedFormatException e) {
			CRNReducerCommandLine.printWarning(out,bwOut,"Problems in computing the monomials of the species. I terminate."+e.getMessage());
			return null;
		}
		
		//int prevR=R.size();
		//int prevRhat=Rhat.size();
		boolean Rmodified=false;
		boolean Qmodified=false;
		int iteration=1;
		do {
			System.out.println("Iteration "+iteration);
//			if(iteration==12) {
//				System.out.println("Iteration "+iteration);
//			}
			iteration++;
			Rmodified=false;
			Qmodified=false;				
				
			
			R.reset();
			while(R.hasNext()) {
				Pair xy =R.moveToNextNode();//(Z1,X1)
//				if(xy.getFirst().getFirstReagent().getName().equals("Z1") &&xy.getSecond().getFirstReagent().getName().equals("X1")) {
//					System.out.println("Eccola");
//				}
				ISpecies x = xy.getFirst().getFirstReagent();
				ISpecies y = xy.getSecond().getFirstReagent();
				LinkedHashSet<Pair> support = solveTransportationProblem(x,y,Qhat);
				if(support!=null) {
					for(Pair p: support) {
						boolean added = Q.add(p);
						Qmodified=added||Qmodified;
					}
				}
				else {
					//No coupling found. Move the pair from Q to Qhat
					Node<Pair> xyNode = R.getCurrentNode();
					R.remove(xyNode);
					Rhat.add(xyNode);
					Rmodified=true;
				}
				if(terminator.hasToTerminate()) {
					CRNReducerCommandLine.println(out, bwOut, "I terminate, as required" );
					return null;
				}
			}
			
			
			Q.reset();
			while(Q.hasNext()) {
				Pair mn =Q.moveToNextNode();
				IComposite m=mn.getFirst();
				IComposite n=mn.getSecond();
				LinkedHashSet<Pair> support = solveTransportationProblem(m,n,Rhat);
				if(support!=null) {
					//We found a coupling. We add the support minus g(R) to R.
					//remove g(R) from suppRho
					//...
					for(Pair p: support) {
						boolean added = R.add(p);
						Rmodified=added||Rmodified;
					}
				}
				else {
					//No coupling found. Move the pair from Q to Qhat
					Node<Pair> mnNode = Q.getCurrentNode();
					Q.remove(mnNode);
					Qhat.add(mnNode);
					Qmodified=true;
				}
				if(terminator.hasToTerminate()) {
					CRNReducerCommandLine.println(out, bwOut, "I terminate, as required" );
					return null;
				}
			}
			
		}while(Rmodified || Qmodified);
		 
		ArrayList<Pair> computedBR=new ArrayList<Pair>(R.size());
		R.reset();
		while(R.hasNext()) {
			Pair v =R.moveToNextNode();
			computedBR.add(v);
		}
		
		return computedBR;
	}

	private LinkedHashSet<Pair> solveTransportationProblem(ISpecies x, ISpecies y, MyLinkedList<Pair> Qhat) {
		//TODO check conjecture: if I have x=y, then I just leave unchanged R and Q.
		if(x.equals(y)) {
			//TODO: Do nothing, or should I add (m,n) to R?
			return new LinkedHashSet<Pair>(0);
		}	

		// Here omega is chosen among the polynomial couplings for (fxp + fyn , fxn + fyp)
		CoefficientToMonomialsPosNeg fx = speciesToMonomials.get(x);
		CoefficientToMonomialsPosNeg fy = speciesToMonomials.get(y);
		//TODO what happens if fx or fy only have a constant in their drift? 
		//	If they both are like this, they can be equal only if the constant is 'equal'.
		//	In all other cases no omega can be created for them
		if(fx==null && fy==null) {
			//If they both have no drift, they are just equal.
			//TODO: Check if it is fine to do nothing.
			return new LinkedHashSet<Pair>(0);
		}

		if(fx==null) {
			fx=emptyMonomials;
		}
		if(fy==null) {
			fy=emptyMonomials;
		}

		VectorOfCoefficientsForEachMonomial fxp = fx.getPositiveMonomials();
		VectorOfCoefficientsForEachMonomial fxn = fx.getNegativeMonomials();

		VectorOfCoefficientsForEachMonomial fyp = fy.getPositiveMonomials();
		VectorOfCoefficientsForEachMonomial fyn = fy.getNegativeMonomials();

		VectorOfCoefficientsForEachMonomial fxpfyn = fxp.sum(fyn);
		VectorOfCoefficientsForEachMonomial fxnfyp = fxn.sum(fyp);

		int k=fxpfyn.numberOfMonomials();
		int h=fxnfyp.numberOfMonomials();
		BigDecimal[][] c = new BigDecimal[k][h];
		computeCostMatrix(Qhat, fxpfyn, fxnfyp, k, h, c);//(X0+X1,X0+X2)
		
		BigDecimal[] sources=fxpfyn.toCoefficientsBD();
		BigDecimal[] destinations=fxnfyp.toCoefficientsBD();
		TransportationProblemBigDecimal problem = new TransportationProblemBigDecimal(sources, destinations, c);

		BigDecimal totalCost=BigDecimal.ZERO;
		if(problem.hasAppliedFixImbalance()) {
			totalCost=BigDecimal.valueOf(Double.MAX_VALUE);
		}
		else{
			numberOfTranspProblemsSolved.increaseTPSolved();
			problem.solve();
			totalCost=problem.getTotalCost();
		}
		
		if(totalCost.compareTo(BigDecimal.ZERO)==0) {
			LinkedHashSet<Pair> suppOmega=problem.computeSupportOfSolution(fxpfyn,fxnfyp);
			return suppOmega;
		}
		else {
			return null;
		}
	}

	/**
	 * Solves the transportation problem for m and n. The cost matrix is built using Rhat
	 * Returns the support of the optimal coupling. 
	 * Special cases:
	 * 	Returns null if no coupling exists with cost 0
	 * 	Returns empty if m=n, as we want to force that no more checks are necessary to establish the equivalence 
	 * 	Returns the obtained support otherwise
	 * @param m the first monomial
	 * @param n the second monomial
	 * @param Rhat the collection of 'bad pairs' used to compute the cost matrix
	 * @return
	 */
	private LinkedHashSet<Pair> solveTransportationProblem(IComposite m, IComposite n, MyLinkedList<Pair> Rhat) {
		//TODO check conjecture: if I have m=n, then I just leave unchanged R and Q.
		if(m.equals(n)) {
			//TODO: Do nothing, or should I add (m,n) to R?
			return new LinkedHashSet<Pair>(0);
		}
		
		//Here omega is chosen among the monomial couplings for (m,n)
		int k=m.getNumberOfDifferentSpecies();
		int h=n.getNumberOfDifferentSpecies();
		BigDecimal[][] c = new BigDecimal[k][h];
		computeCostMatrix(Rhat, m, n, k, h, c);
		
		BigDecimal[] sources=toMultiplicitiesBD(m);
		BigDecimal[] destinations=toMultiplicitiesBD(n);
		
		TransportationProblemBigDecimal problem = new TransportationProblemBigDecimal(sources, destinations, c);
		BigDecimal totalCost=BigDecimal.ZERO;
		if(problem.hasAppliedFixImbalance()) {
			totalCost=BigDecimal.valueOf(Double.MAX_VALUE);
		}
		else{
			numberOfTranspProblemsSolved.increaseTPSolved();;
			problem.solve();
			totalCost=problem.getTotalCost();
		}
		
		if(totalCost.compareTo(BigDecimal.ZERO)==0) {
			LinkedHashSet<Pair> suppOmega=problem.computeSupportOfSolution(m,n);
			return suppOmega;
		}
		else {
			return null;
		}
	}

	public void computeCostMatrix(MyLinkedList<Pair> F, IComposite m, IComposite n, int k, int h, BigDecimal[][] c) {
		if(!F.isEmpty()) {
			for(int i=0;i<k;i++) {
				ISpecies xi = m.getAllSpecies(i);
				for(int j=0;j<h;j++) {
					ISpecies xj = n.getAllSpecies(j);
					if(F.contains(new Pair(xi,xj))) {
						c[i][j]=BigDecimal.ONE;
					}
					else {
						c[i][j]=BigDecimal.ZERO;
					}
				}
			}
		}
	}
	public void computeCostMatrix(MyLinkedList<Pair> F, VectorOfCoefficientsForEachMonomial fxpfyn,
			VectorOfCoefficientsForEachMonomial fxnfyp, int k, int h, BigDecimal[][] c) {
		if(!F.isEmpty()) {
			for(int i=0;i<k;i++) {
				IComposite mi = fxpfyn.getMonomial(i);
				for(int j=0;j<h;j++) {
					IComposite mj = fxnfyp.getMonomial(j);
					if(F.contains(new Pair(mi, mj))) {
						c[i][j]=BigDecimal.ONE;
					}
					else {
						c[i][j]=BigDecimal.ZERO;
					}
				}
			}
		}
	}

	
		
}
