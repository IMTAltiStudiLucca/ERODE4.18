package it.imt.erode.onthefly.algorithm;
import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.auxiliarydatastructures.TPCounter;
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
import it.imt.erode.onthefly.upto.IUpToMembershipChecker;
import it.imt.erode.onthefly.upto.UpToFactory;
import it.imt.erode.onthefly.upto.UpToType;
import it.imt.erode.partitionrefinement.splittersbstandcounters.CoefficientToMonomialsPosNeg;
import it.imt.erode.partitionrefinement.splittersbstandcounters.VectorOfCoefficientsForEachMonomial;

/**
 * The lastest version (15/12/2020) before extending to the algorithm for multiple drifts
 * @author andrea
 *
 */
public class OnTheFlyBRIterativeSingleDrift extends OnTheFlyCommonAbstract{

	public static final boolean ACCEPT_REFLEXIVE_PAIRS=false;
	
	//private HashMap<Pair, HashSet<Pair>> pairToPairsMappedToIByAdj;
	
	//private MyLinkedList<Pair> adjKeys;

	public ArrayList<Pair> onTheFlyBR(
			ICRN crn,
			LinkedHashSet<Pair> query,
			LinkedHashSet<Pair> constraints, UpToType upTo,
			MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator, IMessageDialogShower msgDialogShower) { 
		//pairToPairsMappedToIByAdj= new HashMap<Pair, HashSet<Pair>>();
		
		printOpeningText(query, constraints, upTo, out, bwOut);

		IUpToMembershipChecker upToChecker = UpToFactory.buildUpToMembershipChecker(upTo);
		
		try {
			computeMonomials(crn);
		} catch (UnsupportedFormatException e) {
			CRNReducerCommandLine.printWarning(out,bwOut,"Problems in computing the monomials of the species. I terminate."+e.getMessage());
			return null;
		}
		
		MyLinkedList<Pair> R = performOnTheFlyBR(query, constraints, out, bwOut, terminator, upToChecker,speciesToMonomials,speciesToMonomialsNotSeparated,numberOfTranspProblemsSolved);
		 
		ArrayList<Pair> computedBR=new ArrayList<Pair>(R.size());
		R.reset();
		while(R.hasNext()) {
			Pair v =R.moveToNextNode();
			computedBR.add(v);
		}
		
		return computedBR;
	}

	protected static MyLinkedList<Pair> performOnTheFlyBR(LinkedHashSet<Pair> query, LinkedHashSet<Pair> constraints,
			MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator, IUpToMembershipChecker upToChecker, 
			HashMap<ISpecies,CoefficientToMonomialsPosNeg> speciesToMonomials, 
			HashMap<ISpecies,VectorOfCoefficientsForEachMonomial> speciesToMonomialsNotSeparated, 
			TPCounter numberOfTranspProblemsSolved) {
		if(!Collections.disjoint(query, constraints)) {
			CRNReducerCommandLine.printWarning(out,bwOut,"The query and the constraints are not disjoint. I terminate.");
			return null;
		}
		
		MyLinkedList<Pair> R = new MyLinkedList<Pair>(true);
		//We can't do addToR(R, false, query, upTo,speciesToRelatedByR);
		//Suppose you have Query={p1,p2,p3}. With p3 bisimilar and derivable by p1 and p2 (therefore we remove p3 using this trick). If p2 is removed, then p3 will not be considered anymore (will not belong to the final g(R)) while it should.
		for(Pair p : query) {
			actualAddToR(R, p, upToChecker);
		}
		MyLinkedList<Pair> Q = new MyLinkedList<Pair>(true);

		MyLinkedList<Pair> Rhat = new MyLinkedList<Pair>(true);
		for(Pair p : constraints) {
			Rhat.add(p);
		}
		MyLinkedList<Pair> Qhat = new MyLinkedList<Pair>(true);
		
		//int prevR=R.size();
		//int prevRhat=Rhat.size();
		boolean Rmodified=false;
		boolean Qmodified=false;
		int iteration=1;
		do {
			System.out.println("Iteration "+iteration);
			
			iteration++;
			Rmodified=false;
			Qmodified=false;				
				
			
			R.reset();
			while(R.hasNext()) {
				Pair xy =R.moveToNextNode();
				ISpecies x = xy.getFirst().getFirstReagent();
				ISpecies y = xy.getSecond().getFirstReagent();
				LinkedHashSet<Pair> support = solveTransportationProblem(x,y,Qhat,speciesToMonomials,speciesToMonomialsNotSeparated,numberOfTranspProblemsSolved);
				if(support!=null) {
					//Q \cup supp(omega)
					/*
					for(Pair p: support) {
						boolean added = Q.add(p);
						Qmodified=added||Qmodified;
					}
					*/
					//Q \cup (supp(omega)\setminus M[g(R)])
					for(Pair p: support) {
						IComposite m = p.getFirst();
						IComposite n = p.getSecond();
						if(/*upTo.equals(UpToType.Identity)||*/shouldAddThePair_solveTransportationProblem_upto_Mgr(m,n,R,upToChecker,numberOfTranspProblemsSolved)) {
							boolean added = Q.add(p);
							Qmodified=added||Qmodified;
						}
						//...
						//I have to solve a transportation problem for each pair p.
						//The cost matrix is: for each entry i,j, 1 if xi,xj not in g(R)
						// 	The same cost matrix is used for all p. Compute it before the for
					}
				}
				else {
					//No coupling found. Move the pair from Q to Qhat
					Node<Pair> xyNode = R.getCurrentNode();
					removeFromR(R,xyNode,upToChecker);//R.remove(xyNode);
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
				LinkedHashSet<Pair> support = solveTransportationProblem(m,n,Rhat,numberOfTranspProblemsSolved,speciesToMonomialsNotSeparated);
				if(support!=null) {
					//We found a coupling. We add the support minus g(R) to R.
					boolean added =addToR(R, Rmodified, support,upToChecker);
					Rmodified = Rmodified || added;
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
		return R;
	}



	public static void printOpeningText(LinkedHashSet<Pair> query, LinkedHashSet<Pair> constraints, UpToType upTo,
			MessageConsoleStream out, BufferedWriter bwOut) {
		String qpairs=(query.size()==1)?" pair":" pairs";
		String qbarpairs=(constraints.size()==1)?" pair":" pairs";
		CRNReducerCommandLine.println(out,bwOut,"Computing on-the-fly BR for"+
		"\n\tQuery ("+ query.size()+qpairs+")="+query+
		"\n\tConstraints ("+constraints.size()+qbarpairs+")="+constraints);
		if(!upTo.equals(UpToType.NO)){
			CRNReducerCommandLine.println(out,bwOut,"\tUpTo "+upTo);
		}
	}

	

	private static void removeFromR(MyLinkedList<Pair> R, Node<Pair> xyNode, 
			IUpToMembershipChecker upToChecker) {
		boolean removed = R.remove(xyNode);
		if(removed) {
			upToChecker.removedFromR(xyNode.getData(),R);
		}
//		if(speciesToRelatedByR!=null) {
//			ISpecies first = xyNode.getData().getFirst().getFirstReagent();
//			ISpecies second = xyNode.getData().getSecond().getFirstReagent();
//			speciesToRelatedByR.get(first).remove(second);
//		}
//		if(speciesToRelatedByRUnorderedPair!=null) {
//			ISpecies first = xyNode.getData().getFirst().getFirstReagent();
//			ISpecies second = xyNode.getData().getSecond().getFirstReagent();
//			speciesToRelatedByRUnorderedPair.get(first).remove(second);
//			speciesToRelatedByRUnorderedPair.get(second).remove(first);
//		}
	}

//	private void updateSpeciesToRelatedSet(HashMap<ISpecies, HashSet<ISpecies>> speciesToSet, Pair p) {
//		HashSet<ISpecies> relatedSet = speciesToSet.get(p.getFirst().getFirstReagent());
//		if(relatedSet==null) {
//			relatedSet=new HashSet<ISpecies>();
//			speciesToSet.put(p.getFirst().getFirstReagent(), relatedSet);
//		}
//		relatedSet.add(p.getSecond().getFirstReagent());
//	}

	public static boolean addToR(MyLinkedList<Pair> R, boolean Rmodified, LinkedHashSet<Pair> support, 
			IUpToMembershipChecker upToChecker) {
		
		for(Pair p: support) {
			boolean toAdd = !upToChecker.canDerive((ISpecies)p.getFirst(), (ISpecies)p.getSecond(), R);
					
//			boolean toAdd=false;
//			if(upTo.equals(UpToType.Identity)) {
//				//No smart trick to decide whether we can avoid to add p
//				toAdd=true;
//			}
//			else if(upTo.equals(UpToType.Symmetry)) {
//				//R = R \cup (supp(\rho) \setminus symmetry(R))
//				if(R.contains(p) ||R.contains(new Pair(p.getSecond(),p.getFirst()))) {
//					toAdd=false;
//				}
//				else {
//					toAdd=true;
//				}
//			}
//			else if(upTo.equals(UpToType.Transitivity)){
//				//Transitivity
//				//R = R \cup (supp(\rho) \setminus transitivity(R))
//				toAdd=!canDerive(p,speciesToRelatedByR);
//			}
//			else {
//				//TODO Equivalence!!!
//			}
			
			//Now I can actually add the pair if needed
			if(toAdd) {
				boolean added = actualAddToR(R, p,upToChecker);
				Rmodified=added||Rmodified;
			}
		}
		
		
		return Rmodified;
	}

	protected static boolean actualAddToR(MyLinkedList<Pair> R, Pair p,IUpToMembershipChecker upToChecker) {
		boolean added = R.add(p);
		if(added) {
			upToChecker.addedToR(p);
		}
//		if(speciesToRelatedByR!=null) {
//			updateSpeciesToRelatedSet(speciesToRelatedByR,p);
//		}
//		if(speciesToRelatedByRUnorderedPair!=null) {
//			updateSpeciesToRelatedSet(speciesToRelatedByRUnorderedPair,p);
//			updateSpeciesToRelatedSet(speciesToRelatedByRUnorderedPair,new Pair(p.getSecond(), p.getFirst()));
//		}
		return added;
	}

//	/**
//	 * A simple breadth-first visit of R to decided whether we can derive the pair p by closing R by transitivity
//	 * @param p
//	 * @param speciesToRelatedByR
//	 * @return
//	 */
//	private boolean canDerive(Pair p, HashMap<ISpecies, HashSet<ISpecies>> speciesToRelatedByR) {
//		ISpecies first =p.getFirst().getFirstReagent();
//		ISpecies second =p.getSecond().getFirstReagent();
//
//		HashSet<ISpecies> related = speciesToRelatedByR.get(first);
//		return canFind(second,related,speciesToRelatedByR);
//	}
//
//	private boolean canFind(ISpecies second, HashSet<ISpecies> related,HashMap<ISpecies, HashSet<ISpecies>> speciesToRelatedByR) {
//		if(related==null || related.size()==0) {
//			return false;
//		}
//		if(related.contains(second)) {
//			return true;
//		}
//		for(ISpecies relSpecies : related) {
//			boolean found=canFind(second, speciesToRelatedByR.get(relSpecies),speciesToRelatedByR);
//			if(found) {
//				return true;
//			}
//		}
//		return false;
//	}

	@SuppressWarnings("unused")
	private static LinkedHashSet<Pair> solveTransportationProblem(ISpecies x, ISpecies y, MyLinkedList<Pair> Qhat,
			HashMap<ISpecies, CoefficientToMonomialsPosNeg> speciesToMonomials,
			HashMap<ISpecies,VectorOfCoefficientsForEachMonomial> speciesToMonomialsNotSeparated,
			TPCounter numberOfTranspProblemsSolved) {
		//TODO check conjecture: if I have x=y, then I just leave unchanged R and Q.
		if(ACCEPT_REFLEXIVE_PAIRS && x.equals(y)) {
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
	@SuppressWarnings("unused")
	private static LinkedHashSet<Pair> solveTransportationProblem(IComposite m, IComposite n, MyLinkedList<Pair> Rhat,TPCounter numberOfTranspProblemsSolved,HashMap<ISpecies,VectorOfCoefficientsForEachMonomial> speciesToMonomialsNotSeparated) {
		//TODO check conjecture: if I have m=n, then I just leave unchanged R and Q.
		if(ACCEPT_REFLEXIVE_PAIRS && m.equals(n)) {
			//TODO: Do nothing, or should I add (m,n) to R?
			return new LinkedHashSet<Pair>(0);
		}
		
		//Here omega is chosen among the monomial couplings for (m,n)
		int k=m.getNumberOfDifferentSpecies();
		int h=n.getNumberOfDifferentSpecies();
		BigDecimal[][] c = new BigDecimal[k][h];
		computeCostMatrix(Rhat, m, n, k, h, c,speciesToMonomialsNotSeparated);
		
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
	@SuppressWarnings("unused")
	private static boolean shouldAddThePair_solveTransportationProblem_upto_Mgr(IComposite m, IComposite n, MyLinkedList<Pair> R, 
			IUpToMembershipChecker upToChecker, TPCounter numberOfTranspProblemsSolved) {
		
		if(ACCEPT_REFLEXIVE_PAIRS && m.equals(n)) {
			return true;
		}
		
		//Here omega is chosen among the monomial couplings for (m,n)
		int k=m.getNumberOfDifferentSpecies();
		int h=n.getNumberOfDifferentSpecies();
		BigDecimal[][] c = new BigDecimal[k][h];
		boolean allZeros=computeCostMatrix_Mgr(R, m, n, k, h, c,upToChecker);
		//TODO: NON FARE TP.SOLVE SE LA COST MATRIX E' ZERO
//		if(allZeros){
//			return true;
//		}
		
		BigDecimal[] sources=toMultiplicitiesBD(m);
		BigDecimal[] destinations=toMultiplicitiesBD(n);
		
		TransportationProblemBigDecimal problem = new TransportationProblemBigDecimal(sources, destinations, c);
		BigDecimal totalCost=BigDecimal.ZERO;
		if(problem.hasAppliedFixImbalance()) {
			//totalCost=BigDecimal.valueOf(Double.MAX_VALUE);
			//I want to keep this pair, because this could be a counterexample later to remove R.
			return true;
		}
		else if(allZeros){
			return true;
		}
		else{
			numberOfTranspProblemsSolved.increaseTPSolved();;
			problem.solve();
			totalCost=problem.getTotalCost();
		}
		
		if(totalCost.compareTo(BigDecimal.ZERO)==0) {
			//LinkedHashSet<Pair> suppOmega=problem.computeSupportOfSolution(m,n);
			return true;
		}
		else {
			return false;
		}
		
	}

	public static boolean computeCostMatrix_Mgr(MyLinkedList<Pair> R, IComposite m, IComposite n, int k, int h, BigDecimal[][] c,IUpToMembershipChecker upToChecker) {
		boolean allZeros=true;
		for(int i=0;i<k;i++) {
			ISpecies xi = m.getAllSpecies(i);
			for(int j=0;j<h;j++) {
				ISpecies xj = n.getAllSpecies(j);
				if(upToChecker.canDerive(xi, xj, R)) {
					c[i][j]=BigDecimal.ONE;
					allZeros=false;
				}
				else {
					c[i][j]=BigDecimal.ZERO;
				}
			}
		}
		return allZeros;
	}
	public static void computeCostMatrix(MyLinkedList<Pair> F, IComposite m, IComposite n, int k, int h, BigDecimal[][] c,HashMap<ISpecies,VectorOfCoefficientsForEachMonomial> speciesToMonomialsNotSeparated) {
		if(!F.isEmpty()) {
			for(int i=0;i<k;i++) {
				ISpecies xi = m.getAllSpecies(i);
				for(int j=0;j<h;j++) {
					ISpecies xj = n.getAllSpecies(j);
					if(F.contains(new Pair(xi,xj))) {
						c[i][j]=BigDecimal.ONE;
					}
					else {
						if(equalCumulativeCoefficients(xi, xj,speciesToMonomialsNotSeparated)) {
							c[i][j]=BigDecimal.ZERO;
						}
						else {
							//I want to force the solver to choose a balanced pair
							c[i][j]=BigDecimal.ONE;
						}
					}
				}
			}
		}
	}

	public static boolean equalCumulativeCoefficients(ISpecies xi, ISpecies xj,HashMap<ISpecies,VectorOfCoefficientsForEachMonomial> speciesToMonomialsNotSeparated) {
		VectorOfCoefficientsForEachMonomial fxi = speciesToMonomialsNotSeparated.get(xi);
		VectorOfCoefficientsForEachMonomial fxj = speciesToMonomialsNotSeparated.get(xj);
		BigDecimal coeffs_fxi=BigDecimal.ZERO;
		if(fxi!=null) {
			coeffs_fxi=fxi.sumCoefficientsBD();
		}
		BigDecimal coeffs_fxj=BigDecimal.ZERO;
		if(fxj!=null) {
			coeffs_fxj=fxj.sumCoefficientsBD();
		}
		return coeffs_fxi.compareTo(coeffs_fxj)==0;
	}
	public static void computeCostMatrix(MyLinkedList<Pair> F, VectorOfCoefficientsForEachMonomial fxpfyn,
			VectorOfCoefficientsForEachMonomial fxnfyp, int k, int h, BigDecimal[][] c) {
		if(!F.isEmpty()) {
			for(int i=0;i<k;i++) {
				IComposite mi = fxpfyn.getMonomial(i);
				for(int j=0;j<h;j++) {
					IComposite mj = fxnfyp.getMonomial(j);
					if(F.contains(new Pair(mi, mj))) {
						c[i][j]=BigDecimal.ONE;
					}
					else if(mi.computeArity()!=mj.computeArity()) {
						//I want to force the solver to choose a balanced pair
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
