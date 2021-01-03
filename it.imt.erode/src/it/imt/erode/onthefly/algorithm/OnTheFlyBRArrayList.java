package it.imt.erode.onthefly.algorithm;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import it.imt.erode.onthefly.transportationproblem.TransportationProblemDouble;
import it.imt.erode.partitionrefinement.algorithms.EpsilonDifferentialEquivalences;
import it.imt.erode.partitionrefinement.splittersbstandcounters.CoefficientToMonomialsPosNeg;
import it.imt.erode.partitionrefinement.splittersbstandcounters.VectorOfCoefficientsForEachMonomial;

public class OnTheFlyBRArrayList {

	public ArrayList<Pair> onTheFlyBR(ICRN crn,LinkedHashSet<Pair> Q,LinkedHashSet<Pair> Qbar,
			MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator, IMessageDialogShower msgDialogShower) {
		resetCounters();
		
		//TODO: for 'supp(something_stored_in_hashmap)', we check if there is an entry in the hashmap. 
		//			We should be sure that we don't have entries associated with 0. 
		
		if(!Collections.disjoint(Q, Qbar)) {
			CRNReducerCommandLine.printWarning(out,bwOut,"Q and QBar are not disjoint. I terminate.");
			return null;
		}

		//sigma
		LinkedHashMap<Pair, LinkedHashSet<Pair>> adj=new LinkedHashMap<Pair, LinkedHashSet<Pair>>();
		//TODO: Replace with my own implementation of linked list
		ArrayList<Pair> adjKeys = new ArrayList<Pair>();

		//Contains all pairs which are discovered not to be in BR 
		//AKA v1
		LinkedHashSet<Pair> F = new LinkedHashSet<Pair>(Qbar);

		//Compute the positive and negative monomials associated to each species
		//TODO: optimize here. Also, we can use and array, where the id of the species is its position 
		HashMap<ISpecies, CoefficientToMonomialsPosNeg> speciesToMonomials;
		try {
			speciesToMonomials = computeMonomials(crn);
		} catch (UnsupportedFormatException e) {
			CRNReducerCommandLine.printWarning(out,bwOut,"Problems in computing the monomials of the species. I terminate."+e.getMessage());
			return null;
		}

		//Construct a strategy which contains elements of Q in the domain (and that is closed)
		for(Pair v : Q) {
			update(adj,adjKeys,F,v,speciesToMonomials);
		}

		//Iteratively update the strategy until saturation
		for(int i=0;i<adjKeys.size();i++) {
			Pair v =adjKeys.get(i);
			LinkedHashSet<Pair> adjv = adj.get(v);
			if(!Collections.disjoint(adjv,F)) {
				boolean adjEntryRemoved = update(adj,adjKeys,F,v,speciesToMonomials);
				//TODO this if is most probably wrong. The problem is that expand could call more updates, therefore we might delete some adj we don't know about
				//We need something more smart to recover the case in which we removed v or entries before it, to avoid skipping the next pair 
				//	I will fix this using my implementation of linkedlist
				if(adjEntryRemoved) {
					//If an entry of adj has been removed, I don't want to increase i.
					i=i-1;
				}
			}
		}

		//The algorithm has terminated. I drop the monomial pairs from adj and I return it.
		adj=null;
		ArrayList<Pair> computedBR=new ArrayList<Pair>(adjKeys.size()/2);
		for(Pair v : adjKeys) {
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
	private boolean update(LinkedHashMap<Pair, LinkedHashSet<Pair>> adj,ArrayList<Pair> adjKeys,LinkedHashSet<Pair> F, Pair v, 
			HashMap<ISpecies,CoefficientToMonomialsPosNeg> speciesToMonomials) {
		numberOfUpdates++;
		boolean doElseBranch = false;
		if(v.isMonomialPair()) {
			doElseBranch=update_if_then(adj,adjKeys,F,v,v.getFirst(),v.getSecond(),speciesToMonomials);
		}
		else{
			doElseBranch=update_if_then(adj,adjKeys,F,v,(ISpecies)v.getFirst(),(ISpecies)v.getSecond(),speciesToMonomials);
		}

		if(doElseBranch) {
			F.add(v);
			removeFromAdj(adj, adjKeys, v);
		}
		return doElseBranch;
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
	private boolean update_if_then(LinkedHashMap<Pair, LinkedHashSet<Pair>> adj, ArrayList<Pair> adjKeys, LinkedHashSet<Pair> F, 
			Pair v, IComposite m, IComposite n, HashMap<ISpecies,CoefficientToMonomialsPosNeg> speciesToMonomials) {
		//Here omega is chosen among the monomial couplings for (m,n)
		
		int k=m.getNumberOfDifferentSpecies();
		int h=n.getNumberOfDifferentSpecies();
		double[][] c = new double[k][h];
		computeCostMatrix(F, m, n, k, h, c);
		
		double[] sources=toMultiplicities(m);
		double[] destinations=toMultiplicities(n);
		
		//TODO I am not applying the constraint wij>=0
		TransportationProblemDouble problem = new TransportationProblemDouble(sources, destinations, c);
		double totalCost=0;
		
		if(problem.hasAppliedFixImbalance()) {
			//TODO check if it is correct to ignore cases in which we had to apply 'fiximbalance'
			System.out.println(v+" required to apply fiximbalance");
			totalCost=Double.MAX_VALUE;
		}
		else{
			numberOfTranspProblemsSolved++;
			problem.solve();
			totalCost=problem.getTotalCost();
		}
		
		boolean hasToExecuteElse=false;
		if(totalCost==0) {
			LinkedHashSet<Pair> suppOmega=problem.computeSupportOfSolution(m,n);
			hasToExecuteElse=addAndExpandIfNonTrivialSolution(adj, adjKeys, F, v, speciesToMonomials,suppOmega);
		}
		else {
			//No omega exists that satisfies the if
			hasToExecuteElse=true;
		}
		
		return hasToExecuteElse;
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
	private boolean update_if_then(LinkedHashMap<Pair, LinkedHashSet<Pair>> adj, ArrayList<Pair> adjKeys, 
			LinkedHashSet<Pair> F, Pair v, ISpecies x, ISpecies y, HashMap<ISpecies,CoefficientToMonomialsPosNeg> speciesToMonomials) {
		// Here omega is chosen among the polynomial couplings for (fxp + fyn , fxn + fyp)
		CoefficientToMonomialsPosNeg fx = speciesToMonomials.get(x);
		CoefficientToMonomialsPosNeg fy = speciesToMonomials.get(y);

		VectorOfCoefficientsForEachMonomial fxp = fx.getPositiveMonomials();
		VectorOfCoefficientsForEachMonomial fxn = fx.getNegativeMonomials();

		VectorOfCoefficientsForEachMonomial fyp = fy.getPositiveMonomials();
		VectorOfCoefficientsForEachMonomial fyn = fy.getNegativeMonomials();

		VectorOfCoefficientsForEachMonomial fxpfyn = fxp.sum(fyn);
		VectorOfCoefficientsForEachMonomial fxnfyp = fxn.sum(fyp);
		
		
//		if(fxpfyn.numberOfMonomials()==1 && fxpfyn.getCoefficient(0).compareTo(BigDecimal.valueOf(2))==0
//				&& fxpfyn.getMonomial(0).getFirstReagent().getName().equals("x0")
//				&& fxpfyn.getMonomial(0).getSecondReagent().getName().equals("x2")
//				) {
//			System.out.println("ciao");
//		}

		int k=fxpfyn.numberOfMonomials();
		int h=fxnfyp.numberOfMonomials();
		double[][] c = new double[k][h];
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

		double[] sources=fxpfyn.toCoefficients();
		double[] destinations=fxnfyp.toCoefficients();
		TransportationProblemDouble problem = new TransportationProblemDouble(sources, destinations, c);
		double totalCost=0;
		if(problem.hasAppliedFixImbalance()) {
			//TODO check if it is correct to ignore cases in which we had to apply 'fiximbalance'
			System.out.println(v+" required to apply fiximbalance");
			totalCost=Double.MAX_VALUE;
		}
		else{
			numberOfTranspProblemsSolved++;
			problem.solve();
			totalCost=problem.getTotalCost();
		}
		
		boolean hasToExecuteElse=false;
		//TODO check if it is correct to discard omegas with positive cost
		if(totalCost!=0) {
			//No omega exists that satisfies the if
			hasToExecuteElse=true;
		}
		else {
			LinkedHashSet<Pair> suppOmega=problem.computeSupportOfSolution(fxpfyn,fxnfyp);
			hasToExecuteElse=addAndExpandIfNonTrivialSolution(adj, adjKeys, F, v, speciesToMonomials,suppOmega);
		}
		return hasToExecuteElse;
	}

	private boolean addAndExpandIfNonTrivialSolution(LinkedHashMap<Pair, LinkedHashSet<Pair>> adj, ArrayList<Pair> adjKeys,
			LinkedHashSet<Pair> F, Pair v, HashMap<ISpecies, CoefficientToMonomialsPosNeg> speciesToMonomials,
			LinkedHashSet<Pair> suppOmega) {
		boolean hasToExecuteElse=false;
		//TODO check conjecture: if the support is empty, then we didn't actually find a solution
		if(suppOmega.isEmpty()) {
			//No omega exists that satisfies the if
			hasToExecuteElse=true;
		}
		else {
			addToAdj(adj, adjKeys, v, suppOmega);
			expand(adj, adjKeys, F, v, speciesToMonomials);
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
	private void expand(LinkedHashMap<Pair, LinkedHashSet<Pair>> adj, ArrayList<Pair> adjKeys, LinkedHashSet<Pair> F, Pair v, HashMap<ISpecies,CoefficientToMonomialsPosNeg> speciesToMonomials) {
		numberOfExpand++;
		LinkedHashSet<Pair> adjv = adj.get(v);
		for(Pair u : adjv) {
			//I want to encode u in Adj(v)\(dom(Adj) U F)
			//	I know that u in Adj(v)
			//	Therefore the check is true iff it is not removed by '\'. I.e, if it does not belong neither to dom(Adj) nor to F.
			if(adj.get(u)==null && !F.contains(u)) {
				update(adj,adjKeys,F,u,speciesToMonomials);
			}
		}
	}

	private boolean addToAdj(LinkedHashMap<Pair, LinkedHashSet<Pair>> adj, ArrayList<Pair> adjKeys, Pair v,LinkedHashSet<Pair> pairs) {
		LinkedHashSet<Pair> prev = adj.put(v, pairs);
		boolean added=true;
		if(prev==null) {
			numberOfPairsAddedToAdj++;
			adjKeys.add(v);
		}
		else {
			//TODO check if it can happen that we change the previous value sigma(u)
			//	Max said yes. Check if you nee to do something smart in adjKeys
			numberOfPairsUpdatedInAdj++;
			added=false;
		}
		if(adj.size()!=adjKeys.size()) {
			throw new UnsupportedOperationException("Adj and adjKeys should always have same size.");
		}
		return added;
	}
	
	public void removeFromAdj(LinkedHashMap<Pair, LinkedHashSet<Pair>> adj, ArrayList<Pair> adjKeys, Pair v) {
		adj.remove(v);
		//TODO use and handle fast remove of my linked-list on adjKeys 
		adjKeys.remove(v);
		if(adj.size()!=adjKeys.size()) {
			throw new UnsupportedOperationException("Adj and adjKeys should always have same size.");
		}
	}
	
	



	//	private void addToF(ArrayList<Pair> F, LinkedHashSet<Pair> Fset,Pair v) {
	//		F.add(v);
	//		Fset.add(v);
	//		if(F.size()!=Fset.size()) {
	//			throw new UnsupportedOperationException("F and Fset should always have same size.");
	//		}
	//	}


	private HashMap<ISpecies, CoefficientToMonomialsPosNeg> computeMonomials(ICRN crn) throws UnsupportedFormatException {
		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
		for (ISpecies species : crn.getSpecies()) {
			speciesNameToSpecies.put(species.getName(),species);
		}
		LinkedHashMap<ISpecies, ArrayList<IMonomial>> speciesToMonomialODE = new LinkedHashMap<>(crn.getSpecies().size());
		EpsilonDifferentialEquivalences.computeMonomialsOfAllSpecies(crn,speciesToMonomialODE,speciesNameToSpecies,false);

		HashMap<ISpecies, CoefficientToMonomialsPosNeg> speciesToMonomials = new HashMap<>(speciesToMonomialODE.size());
		for(Entry<ISpecies, ArrayList<IMonomial>> pair : speciesToMonomialODE.entrySet()) {
			ArrayList<IMonomial> monomials = pair.getValue();
			CoefficientToMonomialsPosNeg vposAndNeg = new CoefficientToMonomialsPosNeg(monomials);
			speciesToMonomials.put(pair.getKey(), vposAndNeg);
		}

		return speciesToMonomials;
	}
	
	private int numberOfUpdates=0;
	private int numberOfExpand=0;
	private int numberOfTranspProblemsSolved=0;
	private int numberOfPairsAddedToAdj=0;
	private int numberOfPairsUpdatedInAdj=0;

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
	public void resetCounters() {
		numberOfUpdates=0;
		numberOfExpand=0;
		numberOfTranspProblemsSolved=0;
		numberOfPairsAddedToAdj=0;
		numberOfPairsUpdatedInAdj=0;
	}
}
