package it.imt.erode.onthefly.algorithm;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.expression.parser.IMonomial;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.onthefly.Pair;
import it.imt.erode.onthefly.linkedlist.MyLinkedList;
import it.imt.erode.onthefly.linkedlist.Node;
import it.imt.erode.onthefly.transportationproblem.TransportationProblemDouble;
import it.imt.erode.partitionrefinement.algorithms.EpsilonDifferentialEquivalences;
import it.imt.erode.partitionrefinement.splittersbstandcounters.CoefficientToMonomialsPosNeg;
import it.imt.erode.partitionrefinement.splittersbstandcounters.VectorOfCoefficientsForEachMonomial;

public class OnTheFlyBRIterativeDouble implements IOnTheFly{
	
	private final LinkedHashSet<Pair> emptySupport= new LinkedHashSet<Pair>(0);
	
	private HashMap<Pair, HashSet<Pair>> pairToPairsMappedToIByAdj;
	
	private int numberOfUpdates=0;
	private int numberOfExpand=0;
	private int numberOfTranspProblemsSolved=0;
	private int numberOfPairsAddedToAdj=0;
	private int numberOfPairsUpdatedInAdj=0;
	
	private final CoefficientToMonomialsPosNeg emptyMonomials=new CoefficientToMonomialsPosNeg();
	
	private MyLinkedList<Pair> adjKeys;
	
	private HashMap<ISpecies, CoefficientToMonomialsPosNeg> speciesToMonomials;

	public int getNumberOfUpdates() {
		return numberOfUpdates;
	}

	public int getNumberOfExpand() {
		return numberOfExpand;
	}

	public int getTranspProblemsSolved() {
		return numberOfTranspProblemsSolved;
	}

	public int getNumberOfPairsAddedToAdj() {
		return numberOfPairsAddedToAdj;
	}

	public int getNumberOfPairsUpdatedInAdj() {
		return numberOfPairsUpdatedInAdj;
	}

	public ArrayList<Pair> onTheFlyBR(
			ICRN crn,
			LinkedHashSet<Pair> query,
			LinkedHashSet<Pair> constraints,
			MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator, IMessageDialogShower msgDialogShower) { 
		resetCounters();
		pairToPairsMappedToIByAdj= new HashMap<Pair, HashSet<Pair>>();
		
		if(!Collections.disjoint(query, constraints)) {
			CRNReducerCommandLine.printWarning(out,bwOut,"The query and the constraints are not disjoint. I terminate.");
			return null;
		}
		
		MyLinkedList<Pair> R = new MyLinkedList<Pair>(true);
		MyLinkedList<Pair> Q = new MyLinkedList<Pair>(true);
		for(Pair p : query) {
			R.add(p);
		}

		MyLinkedList<Pair> Rhat = new MyLinkedList<Pair>(true);
		MyLinkedList<Pair> Qhat = new MyLinkedList<Pair>(true);
		for(Pair p : constraints) {
			Rhat.add(p);
		}
		
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
		double[][] c = new double[k][h];
		computeCostMatrix(Qhat, fxpfyn, fxnfyp, k, h, c);//(X0+X1,X0+X2)
		
		double[] sources=fxpfyn.toCoefficients();
		double[] destinations=fxnfyp.toCoefficients();
		TransportationProblemDouble problem = new TransportationProblemDouble(sources, destinations, c);

		double totalCost=0;
		if(problem.hasAppliedFixImbalance()) {
			totalCost=Double.MAX_VALUE;
		}
		else{
			numberOfTranspProblemsSolved++;
			problem.solve();
			totalCost=problem.getTotalCost();
		}
		
		if(totalCost==0) {
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
		double[][] c = new double[k][h];
		computeCostMatrix(Rhat, m, n, k, h, c);
		
		double[] sources=toMultiplicities(m);
		double[] destinations=toMultiplicities(n);
		
		TransportationProblemDouble problem = new TransportationProblemDouble(sources, destinations, c);
		double totalCost=0;
		if(problem.hasAppliedFixImbalance()) {
			totalCost=Double.MAX_VALUE;
		}
		else{
			numberOfTranspProblemsSolved++;
			problem.solve();
			totalCost=problem.getTotalCost();
		}
		
		if(totalCost==0) {
			LinkedHashSet<Pair> suppOmega=problem.computeSupportOfSolution(m,n);
			return suppOmega;
		}
		else {
			return null;
		}
	}

	public void resetCounters() {
		numberOfUpdates=0;
		numberOfExpand=0;
		numberOfTranspProblemsSolved=0;
		numberOfPairsAddedToAdj=0;
		numberOfPairsUpdatedInAdj=0;
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
		double[][] c = new double[k][h];
		computeCostMatrix(F, m, n, k, h, c);
		
		double[] sources=toMultiplicities(m);
		
		double[] destinations=toMultiplicities(n);
		
		TransportationProblemDouble problem = new TransportationProblemDouble(sources, destinations, c);
		double totalCost=0;
		
		if(problem.hasAppliedFixImbalance()) {
			totalCost=Double.MAX_VALUE;
		}
		else{
			numberOfTranspProblemsSolved++;
			//long begin = System.currentTimeMillis();
			problem.solve();
			//long end = System.currentTimeMillis();
			//System.out.println("Completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");
			totalCost=problem.getTotalCost();
		}
		
		boolean hasToExecuteElse=false;
		if(totalCost==0) {
			LinkedHashSet<Pair> suppOmega=problem.computeSupportOfSolution(m,n);
			hasToExecuteElse=addAndExpandIfNonTrivialSolution(adj, F, v, suppOmega);
		}
		else {
			//No omega exists that satisfies the if
			hasToExecuteElse=true;
		}
		
		return hasToExecuteElse;
	}

	public void computeCostMatrix(MyLinkedList<Pair> F, IComposite m, IComposite n, int k, int h, double[][] c) {
		if(!F.isEmpty()) {
			for(int i=0;i<k;i++) {
				ISpecies xi = m.getAllSpecies(i);
				for(int j=0;j<h;j++) {
					ISpecies xj = n.getAllSpecies(j);
					if(F.contains(new Pair(xi,xj))) {
						c[i][j]=1.0;
					}
					else {
						c[i][j]=0.0;
					}
				}
			}
		}
	}
	public void computeCostMatrix(LinkedHashSet<Pair> F, IComposite m, IComposite n, int k, int h, double[][] c) {
		if(!F.isEmpty()) {
			for(int i=0;i<k;i++) {
				ISpecies xi = m.getAllSpecies(i);
				for(int j=0;j<h;j++) {
					ISpecies xj = n.getAllSpecies(j);
					if(F.contains(new Pair(xi,xj))) {
						c[i][j]=1.0;
					}
					else {
						c[i][j]=0.0;
					}
				}
			}
		}
	}
	public void computeCostMatrix(MyLinkedList<Pair> F, VectorOfCoefficientsForEachMonomial fxpfyn,
			VectorOfCoefficientsForEachMonomial fxnfyp, int k, int h, double[][] c) {
		if(!F.isEmpty()) {
			for(int i=0;i<k;i++) {
				IComposite mi = fxpfyn.getMonomial(i);
				for(int j=0;j<h;j++) {
					IComposite mj = fxnfyp.getMonomial(j);
					if(F.contains(new Pair(mi, mj))) {
						c[i][j]=1.0;
					}
					else {
						c[i][j]=0.0;
					}
				}
			}
		}
	}
	public void computeCostMatrix(LinkedHashSet<Pair> F, VectorOfCoefficientsForEachMonomial fxpfyn,
			VectorOfCoefficientsForEachMonomial fxnfyp, int k, int h, double[][] c) {
		if(!F.isEmpty()) {
			for(int i=0;i<k;i++) {
				IComposite mi = fxpfyn.getMonomial(i);
				for(int j=0;j<h;j++) {
					IComposite mj = fxnfyp.getMonomial(j);
					if(F.contains(new Pair(mi, mj))) {
						c[i][j]=1.0;
					}
					else {
						c[i][j]=0.0;
					}
				}
			}
		}
	}

	private double[] toMultiplicities(IComposite m) {
		double[] ret = new double[m.getNumberOfDifferentSpecies()];
		for(int i=0;i<m.getNumberOfDifferentSpecies();i++) {
			ret[i]=m.getMultiplicities(i);
		}
		return ret;
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
		double[][] c = new double[k][h];
		computeCostMatrix(F, fxpfyn, fxnfyp, k, h, c);

		double[] sources=fxpfyn.toCoefficients();
		double[] destinations=fxnfyp.toCoefficients();
		TransportationProblemDouble problem = new TransportationProblemDouble(sources, destinations, c);
		double totalCost=0;
		if(problem.hasAppliedFixImbalance()) {
			totalCost=Double.MAX_VALUE;
		}
		else{
			numberOfTranspProblemsSolved++;
			//long begin = System.currentTimeMillis();
			problem.solve();
			//long end = System.currentTimeMillis();
			//System.out.println("Completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");
			totalCost=problem.getTotalCost();
		}
		
		boolean hasToExecuteElse=false;
		if(totalCost!=0) {
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
		if(pairs==null) {
			System.out.println("ciao");
		}
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

	
	
	private void computeMonomials(ICRN crn) throws UnsupportedFormatException {
		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
		for (ISpecies species : crn.getSpecies()) {
			speciesNameToSpecies.put(species.getName(),species);
		}
		LinkedHashMap<ISpecies, ArrayList<IMonomial>> speciesToMonomialODE = new LinkedHashMap<>(crn.getSpecies().size());
		//TODO: this can be made a lot more efficient by getting rid of RateLaw.
		EpsilonDifferentialEquivalences.computeMonomialsOfAllSpecies(crn,speciesToMonomialODE,speciesNameToSpecies,false);

		speciesToMonomials = new HashMap<>(speciesToMonomialODE.size());
		for(Entry<ISpecies, ArrayList<IMonomial>> pair : speciesToMonomialODE.entrySet()) {
			ArrayList<IMonomial> monomials = pair.getValue();
			CoefficientToMonomialsPosNeg vposAndNeg = new CoefficientToMonomialsPosNeg(monomials);
			speciesToMonomials.put(pair.getKey(), vposAndNeg);
		}
	}

	@Override
	public int getTranspProblemsUnbalancedSkept() {
		// TODO Auto-generated method stub
		return 0;
	}
}
