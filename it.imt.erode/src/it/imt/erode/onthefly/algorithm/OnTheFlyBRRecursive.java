package it.imt.erode.onthefly.algorithm;
import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import it.imt.erode.onthefly.transportationproblem.TransportationProblemBigDecimal;
import it.imt.erode.partitionrefinement.splittersbstandcounters.CoefficientToMonomialsPosNeg;
import it.imt.erode.partitionrefinement.splittersbstandcounters.VectorOfCoefficientsForEachMonomial;

public class OnTheFlyBRRecursive  extends OnTheFlyCommonAbstract{
	
	private final boolean noCopiesAreAddedToAdjKeys=true;
	
	private HashMap<Pair, HashSet<Pair>> pairToPairsMappedToIByAdj;
		
	private MyLinkedList<Pair> adjKeys;
	
	public ArrayList<Pair> onTheFlyBR(
			ICRN crn,
			LinkedHashSet<Pair> Q,
			LinkedHashSet<Pair> Qbar,
			MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator, IMessageDialogShower msgDialogShower) { 
		pairToPairsMappedToIByAdj= new HashMap<Pair, HashSet<Pair>>();
		
		if(!Collections.disjoint(Q, Qbar)) {
			CRNReducerCommandLine.printWarning(out,bwOut,"Q and QBar are not disjoint. I terminate.");
			return null;
		}

		//sigma
		LinkedHashMap<Pair, LinkedHashSet<Pair>> adj=new LinkedHashMap<Pair, LinkedHashSet<Pair>>();
		adjKeys = new MyLinkedList<Pair>(noCopiesAreAddedToAdjKeys);

		//Contains all pairs which are discovered not to be in BR 
		//AKA v1
		LinkedHashSet<Pair> F = new LinkedHashSet<Pair>(Qbar);

		//Compute the positive and negative monomials associated to each species
		try {
			computeMonomials(crn);
		} catch (UnsupportedFormatException e) {
			CRNReducerCommandLine.printWarning(out,bwOut,"Problems in computing the monomials of the species. I terminate."+e.getMessage());
			return null;
		}

		//Construct a strategy which contains elements of Q in the domain (and that is closed)
		for(Pair v : Q) {
			update(adj,F,v);
			if(terminator.hasToTerminate()) {
				CRNReducerCommandLine.println(out, bwOut, "I terminate, as required" );
				return null;
			}
		}

		CRNReducerCommandLine.println(out,bwOut,"After the firs loop of update on Q we have "+adj.size()+" pairs in sigma, and "+F.size()+" in F.");
		
			adjKeys.reset();
			//Iteratively update the strategy until saturation
			while(adjKeys.hasNext()) {
				Pair v =adjKeys.moveToNextNode();
				LinkedHashSet<Pair> adjv = adj.get(v);
				if(!Collections.disjoint(adjv,F)) {
					update(adj,F,v);
				}
				if(terminator.hasToTerminate()) {
					CRNReducerCommandLine.println(out, bwOut, "I terminate, as required" );
					return null;
				}
			}

		//The algorithm has terminated. I drop the monomial pairs from adj and I return it.
		adj=null;
		ArrayList<Pair> computedBR=new ArrayList<Pair>(adjKeys.size()/2);
		adjKeys.reset();
		while(adjKeys.hasNext()) {
			Pair v =adjKeys.moveToNextNode();
			if(v.isSpeciesPair()) {
				computedBR.add(v);
			}
		}
		
		return computedBR;
	}

	/**
	 * Update selects an optimal move based on the current value. Furthermore, if no move exists or we discover that the optimal move is losing, we update the value
	 * 
	 * This method actually delegates to the correct 'update_if_then' (for a pair of species or of monomials) the computation of the guard of the if, and the then branch if it is true.
	 * Executes the else branch in case the guard of the if evaluated to false
	 * @param adj
	 * @param adjKeys
	 * @param F
	 * @param v
	 * @param speciesToMonomials 
	 * @return true if the else branch has been executed (adding an entry to F and removing one from Adj)
	 */
	private void update(LinkedHashMap<Pair, LinkedHashSet<Pair>> adj,LinkedHashSet<Pair> F, Pair v) {
		numberOfUpdates++;
		boolean doElseBranch = false;
		
		if(v.isMonomialPair()) {
			doElseBranch=update_if_then(adj,F,v,v.getFirst(),v.getSecond());
		}
		else{
			doElseBranch=update_if_then(adj,F,v,v.getFirst().getFirstReagent(),v.getSecond().getFirstReagent());
		}

		if(doElseBranch) {
			//I need a data structure that tells me which entries of adj contain v. Because I need to check them again. 
			removeFromAdj(adj, v);
			addToF(F,v);
		}
	}

	private void addToF(LinkedHashSet<Pair> F, Pair v) {
		F.add(v);
		HashSet<Pair> pairsToMoveAtEnd = pairToPairsMappedToIByAdj.get(v);
		if(pairsToMoveAtEnd!=null) {
			for(Pair pairToMoveAtEnd:pairsToMoveAtEnd) {
				adjKeys.moveAtEnd(pairToMoveAtEnd);
			}
		}
	}

	/**
	 * The choice of the optimal move is done solving a transportation problem
	 * Case 1: pair of monomials
	 * This method actually performs the check of the if and the 'then branch' of update for a pair of monomials
	 * 	if exists rho in Gamma_M(m,n).supp(rho) intersect F = empty
	 * 		Adj(m,n)=supp(rho)
	 * 		Expand(Adj,F,(m,n))
	 * If the solver finds a rho, then we know that this rho satisfies the guard (conjecture: if the cost function of rho is zero!)
	 * @param adj
	 * @param adjKeys
	 * @param F
	 * @param m
	 * @param n
	 * @param speciesToMonomials 
	 * @return true if the else branch should be executed 
	 */
	private boolean update_if_then(LinkedHashMap<Pair, LinkedHashSet<Pair>> adj,  LinkedHashSet<Pair> F, 
			Pair v, IComposite m, IComposite n) {
		//TODO we should handle also here the case with empty suppport. And what if a monomial does not have species (it is the empty composite)?
		
		//TODO check conjecture: if I have m=n, then I just do Adj(m,n) = empty and return hasToExecuteElse=false
		if(v.getFirst().equals(v.getSecond())) {
			addToAdj(adj, v, emptySupport);
			return false;
		}
		
		//Here omega is chosen among the monomial couplings for (m,n)
		int k=m.getNumberOfDifferentSpecies();
		int h=n.getNumberOfDifferentSpecies();
		BigDecimal[][] c = new BigDecimal[k][h];
		computeCostMatrix(F, m, n, k, h, c);
		
		BigDecimal[] sources=toMultiplicitiesBD(m);
		
		BigDecimal[] destinations=toMultiplicitiesBD(n);
		
		TransportationProblemBigDecimal problem = new TransportationProblemBigDecimal(sources, destinations, c);
		BigDecimal totalCost=BigDecimal.ZERO;
		
		if(problem.hasAppliedFixImbalance()) {
			totalCost=BigDecimal.valueOf(Double.MAX_VALUE);
		}
		else{
			numberOfTranspProblemsSolved.increaseTPSolved();;
			//long begin = System.currentTimeMillis();
			problem.solve();
			//long end = System.currentTimeMillis();
			//System.out.println("Completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");
			totalCost=problem.getTotalCost();
		}
		
		boolean hasToExecuteElse=false;
		if(totalCost.compareTo(BigDecimal.ZERO)==0) {
			LinkedHashSet<Pair> suppOmega=problem.computeSupportOfSolution(m,n);
			hasToExecuteElse=addAndExpandIfNonTrivialSolution(adj, F, v, suppOmega);
		}
		else {
			//No omega exists that satisfies the if
			hasToExecuteElse=true;
		}
		
		return hasToExecuteElse;
	}

	public void computeCostMatrix(LinkedHashSet<Pair> F, IComposite m, IComposite n, int k, int h, BigDecimal[][] c) {
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
	public void computeCostMatrix(LinkedHashSet<Pair> F, VectorOfCoefficientsForEachMonomial fxpfyn,
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

	/**
	 * The choice of the optimal move is done solving a transportation problem
	 * Case 2: pair of species
	 * This method actually performs the check of the if and the 'then branch' of update for a pair of species
	 * 	if exists omega in Gamma_P(fx,fy).supp(omega) intersect F = empty
	 * 		Adj(x,y)=supp(omega)
	 * 		Expand(Adj,F,(x,y) 
	 * If the solver finds an omega, then we know that this omega satisfies the guard (conjecture: if the cost function of omega is zero!)
	 * @param adj
	 * @param adjKeys
	 * @param F
	 * @param x
	 * @param y
	 * @param speciesToMonomials 
	 * @return true if the else branch should be executed 
	 */
	private boolean update_if_then(LinkedHashMap<Pair, LinkedHashSet<Pair>> adj,  
			LinkedHashSet<Pair> F, Pair v, ISpecies x, ISpecies y) {
		//TODO check conjecture: if I have x=y, then I just do Adj(x,y) = empty and return hasToExecuteElse=false
		if(v.getFirst().equals(v.getSecond())) {
			addToAdj(adj, v, emptySupport);
			return false;
		}
		
		
		// Here omega is chosen among the polynomial couplings for (fxp + fyn , fxn + fyp)
		CoefficientToMonomialsPosNeg fx = speciesToMonomials.get(x);
		CoefficientToMonomialsPosNeg fy = speciesToMonomials.get(y);
		//TODO what happens if fx or fy only have a constant in their drift? 
		//	If they both are like this, they can be equal only if the constant is 'equal'.
		//	In all other cases no omega can be created for them
		if(fx==null && fy==null) {
			//If they both have no drift, they are just equal.
			LinkedHashSet<Pair> emptySupport= new LinkedHashSet<Pair>();
			addToAdj(adj, v, emptySupport);
			return false;
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
		computeCostMatrix(F, fxpfyn, fxnfyp, k, h, c);

		BigDecimal[] sources=fxpfyn.toCoefficientsBD();
		BigDecimal[] destinations=fxnfyp.toCoefficientsBD();
		TransportationProblemBigDecimal problem = new TransportationProblemBigDecimal(sources, destinations, c);
		BigDecimal totalCost=BigDecimal.ZERO;
		if(problem.hasAppliedFixImbalance()) {
			totalCost=BigDecimal.valueOf(Double.MAX_VALUE);
		}
		else{
			numberOfTranspProblemsSolved.increaseTPSolved();;
			//long begin = System.currentTimeMillis();
			problem.solve();
			//long end = System.currentTimeMillis();
			//System.out.println("Completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");
			totalCost=problem.getTotalCost();
		}
		
		boolean hasToExecuteElse=false;
		if(totalCost.compareTo(BigDecimal.ZERO)!=0) {
			//No omega exists that satisfies the if
			hasToExecuteElse=true;
		}
		else {
			LinkedHashSet<Pair> suppOmega=problem.computeSupportOfSolution(fxpfyn,fxnfyp);
			hasToExecuteElse=addAndExpandIfNonTrivialSolution(adj, F, v, suppOmega);
		}
		return hasToExecuteElse;
	}

	private boolean addAndExpandIfNonTrivialSolution(LinkedHashMap<Pair, LinkedHashSet<Pair>> adj, 
			LinkedHashSet<Pair> F, Pair v, 
			LinkedHashSet<Pair> suppOmega) {
		boolean hasToExecuteElse=false;
		//TODO check conjecture: if the support is empty, then we didn't actually find a solution WRONG
		//	The conjecture is wrong. It can happen when both species have empty drift. We treat this case before
		//	It could happen for species that have a constant as drift. This is not handled at the moment
		if(suppOmega.isEmpty()) {
			//No omega exists that satisfies the if
			System.out.println("Empty support for "+v.toString());
			hasToExecuteElse=true;
		}
		else {
			boolean adjHasChanged=addToAdj(adj, v, suppOmega);
			if(adjHasChanged) {
				expand(adj,F, v, speciesToMonomials);
			}
			else {
				//TODO: Bacci's conjecture: this should not happen
				//	the fact is that we invoke the main while many times, as long as F changes. Therefore we might considre many times a pair
				//System.out.println("We have updated adj("+v+") with set equal to previous ("+suppOmega+")\n\tThis should not happen.");
			}
			hasToExecuteElse=false;
		}
		return hasToExecuteElse;
	}

	
	/**
	 * Expand is used to close the strategy.
	 * To save computation, expansion is performed only on nodes not cotained in F
	 * @param adj
	 * @param adjKeys
	 * @param F
	 * @param v
	 */
	private void expand(LinkedHashMap<Pair, LinkedHashSet<Pair>> adj,  LinkedHashSet<Pair> F, Pair v, HashMap<ISpecies,CoefficientToMonomialsPosNeg> speciesToMonomials) {
		LinkedHashSet<Pair> adjv = adj.get(v);
		numberOfExpand++;
		for(Pair u : adjv) {
			//I want to encode u in Adj(v)\(dom(Adj) U F)
			//	I know that u in Adj(v)
			//	Therefore the check is true iff it is not removed by '\'. I.e, if it does not belong neither to dom(Adj) nor to F.
			if(adj.get(u)==null && !F.contains(u)) {
				update(adj,F,u);
			}
		}
	}

	private boolean addToAdj(LinkedHashMap<Pair, LinkedHashSet<Pair>> adj,  Pair v,LinkedHashSet<Pair> pairs) {
		boolean adjHasChanged=true;
		LinkedHashSet<Pair> prev = adj.put(v, pairs);
		
		if(prev==null) {
			numberOfPairsAddedToAdj++;
			adjKeys.add(v);
			addPointingPairs(v, pairs);
		}
		else {	
			if(prev.equals(pairs)) {
				//We did not actually update adj(v)
				adjHasChanged=false;
			}
			else {
				//We have updated the pairs pointed by v with a new set. I have to handle this change be sure to reconsider all necessary pairs for computing BR.
				numberOfPairsUpdatedInAdj++;
				adjKeys.moveAtEnd(v);//We need this only in the while, but it does not cost much anyway. 
				//I keep track of pairs pointed by v. So that if those pairs become part of F, I can go check the pair v moving it to the end of adjkey
				removePointingPairs(v, prev);
				addPointingPairs(v, pairs);
			}
		}
		if(adj.size()!=adjKeys.size()) {
			throw new UnsupportedOperationException("Adj and adjKeys should always have same size.");
		}
		return adjHasChanged;
	}

	public void addPointingPairs(Pair v, LinkedHashSet<Pair> pairs) {
		for(Pair pointedPair : pairs) {
			HashSet<Pair> pointingPairs = pairToPairsMappedToIByAdj.get(pointedPair);
			if(pointingPairs==null) {
				pointingPairs = new HashSet<Pair>();
				pairToPairsMappedToIByAdj.put(pointedPair, pointingPairs);
			}
			pointingPairs.add(v);
		}
	}
	
	public void removePointingPairs(Pair v, LinkedHashSet<Pair> prev) {
		if(prev==null) {
			System.out.println("ciao");
		}
		for(Pair pointedPair : prev) {
			HashSet<Pair> pointingPairs = pairToPairsMappedToIByAdj.get(pointedPair);
			if(pointingPairs!=null) {
				pointingPairs.remove(v);
			}
		}
	}

	
	public void removeFromAdj(LinkedHashMap<Pair, LinkedHashSet<Pair>> adj,  Pair v) {
		LinkedHashSet<Pair> prev = adj.remove(v);
		if(prev!=null) {
			adjKeys.remove(v);
			removePointingPairs(v, prev);
		}
		if(adj.size()!=adjKeys.size()) {
			throw new UnsupportedOperationException("Adj and adjKeys should always have same size.");
		}
	}

}
