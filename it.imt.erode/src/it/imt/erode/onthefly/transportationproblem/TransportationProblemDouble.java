package it.imt.erode.onthefly.transportationproblem;

/*
 * Adapted from 
 * https://rosettacode.org/wiki/Transportation_problem#Java
 * We now handle the case of degeneracy where more 'eps' have to be added. We also support double supply/demand 
 */

import java.util.*;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.onthefly.Pair;
import it.imt.erode.partitionrefinement.splittersbstandcounters.VectorOfCoefficientsForEachMonomial;



/*
input1.txt

2 3
25 35
20 30 10
3 5 7
3 2 5

Optimal solution input1.txt

  20   -     5 
  -    30    5 

Total costs: 180.0

 */
/*
 *You interpret the input like this*
2 suppliers 3 consumers
Suppliers have: 25 35
Consumers need: 20 30 10

How much is it to move 1 cargo per kg (e.g., 3$ per kg in the top-left corner)
   C1 C2 C3
S1 3  5  7
S2 3  2  5

 *You interpret the output like this*
How many kg each supplier ships to each consumer (and the total cost)

	C1   C2    C3
S1  20   -     5 
S2  -    30    5 

Total costs: 180.0
20*3+  0*5 + 5*7 +
 0*3+ 30*2 + 5*5 = 180

 */



public class TransportationProblemDouble {

	//TODO: for BE/FE, double were causing problems. We had to move to BigDecimals. We might end up in similar problems if we have 'bad parameters'
	//supply and demand could be changed to BigDecimal
	private double[] demand;
	private double[] supply;
	private double[][] costs;
	private Shipment[][] matrix;
	private double totalCost;
	private static final double eps = Double.MIN_VALUE;
	private static final double max_value=Double.MAX_VALUE;//Integer.MAX_VALUE;
	private boolean appliedFixImbalance=false;

	private TransportationProblemDouble() {

	}
	public TransportationProblemDouble(double[] sources,double[] destinations,double[][] c) {
		this();
		init(sources,destinations,c);
	}

	private void init(double[] sources,double[] destinations,double[][] c) {        
		supply=sources;
		demand=destinations;
		costs=c;//costs = new double[supply.length][demand.length];

		fixImabalance();

		matrix = new Shipment[supply.length][demand.length];
	}

	public boolean hasAppliedFixImbalance() {
		return appliedFixImbalance;
	}

	public void solve() {
		totalCost=0;
		northWestCornerRule();
		//If the cost is already 0, I don't care about further optimizing it.
		computeTotalCost();
		if(totalCost!=0) {
			steppingStone();
			computeTotalCost();
		}
		//printResult();
		//return exportResults();
	}

	private void computeTotalCost() {
		totalCost = 0.0;

		for (int r = 0; r < supply.length; r++) {
			for (int c = 0; c < demand.length; c++) {
				Shipment s = matrix[r][c];
				if (s != null && s.r == r && s.c == c && 
						s.quantity != eps) {
					totalCost += (s.quantity * s.costPerUnit);
				}
			}
		}

	}

	public double getTotalCost() {
		return totalCost;
	}

	private void fixImabalance() {
		// fix imbalance
		int numSources = supply.length;
		int numDestinations = demand.length;

		double totalSrc=0;
		for(int i=0;i<numSources;i++) {
			totalSrc+=supply[i];
		}
		double totalDst=0;
		for(int i=0;i<numDestinations;i++) {
			totalDst+=demand[i];
		}

		if (totalDst > totalSrc) {
			supply=Arrays.copyOf(supply, supply.length+1);
			supply[supply.length-1]=totalDst - totalSrc;
			//I have to add a row to costs.
			//costs = new double[supply.length][demand.length];
			costs=Arrays.copyOf(costs, supply.length);
			costs[supply.length-1]=new double[demand.length];
			//Arrays.fill(costs[supply.length-1], 0);
			appliedFixImbalance=true;
		} else if (totalSrc > totalDst) {
			demand =Arrays.copyOf(demand, demand.length+1);
			demand[demand.length-1]=totalSrc-totalDst;
			//I have to add a column to costs.
			//costs = new double[supply.length][demand.length];
			double[][] new_costs=new double[supply.length][demand.length];
			for(int r=0;r<supply.length;r++) {
				new_costs[r]=Arrays.copyOf(costs[r], demand.length);
				//new_costs[r][demand.length-1]=0;
			}
			costs=new_costs;
			appliedFixImbalance=true;
		} 
	}

	private void northWestCornerRule() {

		for (int r = 0, northwest = 0; r < supply.length; r++)
			for (int c = northwest; c < demand.length; c++) {

				double quantity = Math.min(supply[r], demand[c]);
				if (quantity > 0) {
					matrix[r][c] = new Shipment(quantity, costs[r][c], r, c);

					supply[r] -= quantity;
					demand[c] -= quantity;

					if (supply[r] == 0) {
						northwest = c;
						break;
					}
				}
			}
	}

	private void steppingStone() {
		double maxReduction = 0;
		Shipment[] move = null;
		Shipment leaving = null;

		fixDegenerateCase();

		for (int r = 0; r < supply.length; r++) {
			for (int c = 0; c < demand.length; c++) {

				if (matrix[r][c] != null)
					continue;

				Shipment trial = new Shipment(0, costs[r][c], r, c);
				Shipment[] path = getClosedPath(trial);

				double reduction = 0;
				double lowestQuantity = max_value;
				Shipment leavingCandidate = null;

				boolean plus = true;
				for (Shipment s : path) {
					if (plus) {
						reduction += s.costPerUnit;
					} else {
						reduction -= s.costPerUnit;
						if (s.quantity < lowestQuantity) {
							leavingCandidate = s;
							lowestQuantity = s.quantity;
						}
					}
					plus = !plus;
				}
				if (reduction < maxReduction) {
					move = path;
					leaving = leavingCandidate;
					maxReduction = reduction;
				}
			}
		}

		if (move != null) {
			double q = leaving.quantity;
			boolean plus = true;
//			for (Shipment s : move) {
//				if(s.quantity==eps && (!plus) && q==eps) {
//					s.quantity=0;
//				}
//				else if(s.quantity==eps && (plus) && q==eps) {
//					s.quantity=eps;
//				}
//				else {
//					s.quantity += plus ? q : -q;
//				}
//				matrix[s.r][s.c] = s.quantity == 0  ? null : s;
//				plus = !plus;
//			}
			for (Shipment s : move) {
				//Special care is necessary when we move (q) epsilon. 
				if(q==eps) {
					//Remove it from eps-eps=0, eps+eps=eps, 0+eps=eps 
					if(s.quantity==eps) {
						if(!plus) {
							s.quantity=0;
						}
						else {
							s.quantity=eps;
						}
					}
					else if(s.quantity==0) {
						s.quantity += plus ? q : -q;
					}
					else {
						//Do not add/or remove eps to a number larger than eps.
					}
				}
				else {
					if(s.quantity==eps) {
						//Don't mix epsilons with other numbers
						s.quantity=0;
					}
					s.quantity += plus ? q : -q;
				}
				matrix[s.r][s.c] = s.quantity == 0  ? null : s;
				plus = !plus;
			}
			steppingStone();
		}
	}

	private LinkedList<Shipment> matrixToList() {
		LinkedList<Shipment> ret = new LinkedList<>();
		for(int r=0;r<supply.length;r++) {
			for(int c=0;c<demand.length;c++) {
				if(matrix[r][c]!=null) {
					ret.add(matrix[r][c]);
				}
			}
		}
		return ret;
	}

	private Shipment[] getClosedPath(Shipment s) {
		LinkedList<Shipment> path = matrixToList();
		path.addFirst(s);

		// remove (and keep removing) elements that do not have a
		// vertical AND horizontal neighbor
		while (path.removeIf(e -> {
			Shipment[] nbrs = getNeighbors(e, path);
			return nbrs[0] == null || nbrs[1] == null;
		}));

		// place the remaining elements in the correct plus-minus order
		Shipment[] stones = path.toArray(new Shipment[path.size()]);
		Shipment prev = s;
		for (int i = 0; i < stones.length; i++) {
			stones[i] = prev;
			prev = getNeighbors(prev, path)[i % 2];
		}
		return stones;
	}

	private static Shipment[] getNeighbors(Shipment s, LinkedList<Shipment> lst) {
		Shipment[] nbrs = new Shipment[2];
		for (Shipment o : lst) {
			if (o != s) {
				if (o.r == s.r && nbrs[0] == null)
					nbrs[0] = o;
				else if (o.c == s.c && nbrs[1] == null)
					nbrs[1] = o;
				if (nbrs[0] != null && nbrs[1] != null)
					break;
			}
		}
		return nbrs;
	}

//	private void fixDegenerateCase() {
//
//		if (supply.length + demand.length - 1 != matrixToList().size()) {
//
//			for (int r = 0; r < supply.length; r++)
//				for (int c = 0; c < demand.length; c++) {
//					if (matrix[r][c] == null) {
//						Shipment dummy = new Shipment(eps, costs[r][c], r, c);
//						if (getClosedPath(dummy).length == 0) {
//							matrix[r][c] = dummy;
//							return;
//						}
//					}
//				}
//		}
//	}
	
	private void fixDegenerateCase() {

		int toAdd = Math.abs(supply.length + demand.length - 1 - matrixToList().size());
		if (toAdd>0) {
			for (int r = 0; r < supply.length; r++)
				for (int c = 0; c < demand.length; c++) {
					if (matrix[r][c] == null) {
						Shipment dummy = new Shipment(eps, costs[r][c], r, c);
						//TODO: conjecture, in some sense, by creating an element with value 0 is like creating an element with value epsilon, but I solve the problem of having 'epsilon*10'.
						//Shipment dummy = new Shipment(0, costs[r][c], r, c);
						if (getClosedPath(dummy).length == 0) {
							matrix[r][c] = dummy;
							toAdd--;
							if(toAdd==0) {
								return;
							}
						}
					}
				}
		}
	}

	public void printResult() {
		System.out.println("Optimal solution");
		double totalCosts = 0;

		for (int r = 0; r < supply.length; r++) {
			for (int c = 0; c < demand.length; c++) {

				Shipment s = matrix[r][c];
				if (s != null && s.r == r && s.c == c && s.quantity != eps) {
					System.out.print(" "+ s.quantity + " ");
					totalCosts += (s.quantity * s.costPerUnit);
				} else
					System.out.print("  -  ");
			}
			System.out.println();
		}
		System.out.println("Total costs: "+ totalCosts);
	}

	public double[][] exportResult(){
		double[][] res = new double[supply.length][demand.length];
		for(int r=0;r<supply.length;r++) {
			for(int c=0;c<demand.length;c++) {
				res[r][c]=matrix[r][c].quantity;
			}
		}
		return res;
	}


	public LinkedHashSet<Pair> computeSupportOfSolution(
			VectorOfCoefficientsForEachMonomial f_sources,
			VectorOfCoefficientsForEachMonomial f_destinations) {
		LinkedHashSet<Pair> support=new LinkedHashSet<Pair>();
		for(int r=0;r<supply.length;r++) {
			for(int c=0;c<demand.length;c++) {
				if(matrix[r][c] !=null && matrix[r][c].quantity>eps) {
					IComposite r_comp=f_sources.getMonomial(r);
					IComposite c_comp=f_destinations.getMonomial(c);
					support.add(new Pair(r_comp, c_comp));
				}
			}
		}
		return support;
	}


	public LinkedHashSet<Pair> computeSupportOfSolution(IComposite m_sources, IComposite m_destinations) {
		LinkedHashSet<Pair> support=new LinkedHashSet<Pair>();
		for(int r=0;r<supply.length;r++) {
			for(int c=0;c<demand.length;c++) {
				if(matrix[r][c]!=null && 
				   matrix[r][c].quantity>eps) {
					IComposite r_comp=(IComposite)m_sources.getAllSpecies(r);
					IComposite c_comp=(IComposite)m_destinations.getAllSpecies(c);
					support.add(new Pair(r_comp, c_comp));
				}
			}
		}
		return support;
	}

	private class Shipment {
		final double costPerUnit;
		final int r, c;
		double quantity;

		public Shipment(double q, double cpu, int r, int c) {
			quantity = q;
			costPerUnit = cpu;
			this.r = r;
			this.c = c;
		}
		@Override
		public String toString() {
			String pos="("+r+","+c+")";
			if(quantity==0) {
				return pos+"_"+"-";
			}
			else {
				return pos+"_"+quantity+"*"+costPerUnit;
			}
		}
	}

}