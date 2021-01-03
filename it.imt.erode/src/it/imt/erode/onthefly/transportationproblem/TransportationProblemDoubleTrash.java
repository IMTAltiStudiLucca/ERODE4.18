package it.imt.erode.onthefly.transportationproblem;

/*
 * Taken from 
 * https://rosettacode.org/wiki/Transportation_problem#Java
 */

import java.util.*;
//import static java.util.Arrays.stream;
//import static java.util.stream.Collectors.toCollection;

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



public class TransportationProblemDoubleTrash {
 
    private double[] demand;
    private double[] supply;
    private double[][] costs;
    private Shipment[][] matrix;
	private int totalCost;
	private static final double eps = Double.MIN_VALUE;
	private static final double max_value=Double.MAX_VALUE;//Integer.MAX_VALUE;
	//private boolean allowsFixImbalance=false;
	private boolean appliedFixImbalance=false;
 
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
        	if(quantity==0) {
        		return "-";
        	}
        	else {
        		return quantity+"*"+costPerUnit;
        	}
        }
    }
 
    public boolean hasAppliedFixImbalance() {
    	return appliedFixImbalance;
    }
    
    public void solve() {
    	totalCost=0;
    	northWestCornerRule();
        steppingStone();
        computeTotalCost();
        printResult();
        //return exportResults();
    }
    
    private void computeTotalCost() {
    	totalCost = 0;
    	 
        for (int r = 0; r < supply.length; r++) {
            for (int c = 0; c < demand.length; c++) {
                Shipment s = matrix[r][c];
                if (s != null && s.r == r && s.c == c && s.quantity != eps) {
                    totalCost += (s.quantity * s.costPerUnit);
                }
            }
        }
		
	}
    
    public double getTotalCost() {
    	return totalCost;
    }

	private TransportationProblemDoubleTrash() {
    	
    }
    public TransportationProblemDoubleTrash(double[] sources,double[] destinations,double[][] c) {
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
    
//    private void init(String filename) throws Exception {
// 
//        try (Scanner sc = new Scanner(new File(filename))) {
//            int numSources = sc.nextInt();
//            int numDestinations = sc.nextInt();
// 
//            List<Double> src = new ArrayList<>();
//            List<Double> dst = new ArrayList<>();
// 
//            for (int i = 0; i < numSources; i++)
//                src.add(sc.nextDouble());
// 
//            for (int i = 0; i < numDestinations; i++)
//                dst.add(sc.nextDouble());
// 
//            // fix imbalance
//            double totalSrc=0;
//            for(Double s:src) {
//            	totalSrc+=s;
//            }
//            double totalDst=0;
//            for(Double d:dst) {
//            	totalDst+=d;
//            }
//           
//            if (totalSrc > totalDst)
//                dst.add(totalSrc - totalDst);
//            else if (totalDst > totalSrc)
//                src.add(totalDst - totalSrc);
// 
//            supply = new double[src.size()];
//            for(int i=0;i<src.size();i++) {
//            	supply[i]=src.get(i);
//            }
//            demand = new double[dst.size()];
//            for(int i=0;i<dst.size();i++) {
//            	demand[i]=dst.get(i);
//            }
//         
//            costs = new double[supply.length][demand.length];
//            matrix = new Shipment[supply.length][demand.length];
// 
//            for (int i = 0; i < numSources; i++)
//                for (int j = 0; j < numDestinations; j++)
//                    costs[i][j] = sc.nextDouble();
//        }
//    }
 
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
            for (Shipment s : move) {
                s.quantity += plus ? q : -q;
                matrix[s.r][s.c] = s.quantity == 0 ? null : s;
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
//        return stream(matrix)
//                .flatMap(row -> stream(row))
//                .filter(s -> s != null)
//                .collect(toCollection(LinkedList::new));
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
 
    private void fixDegenerateCase() {
 
        if (supply.length + demand.length - 1 != matrixToList().size()) {
 
            for (int r = 0; r < supply.length; r++)
                for (int c = 0; c < demand.length; c++) {
                    if (matrix[r][c] == null) {
                        Shipment dummy = new Shipment(eps, costs[r][c], r, c);
                        if (getClosedPath(dummy).length == 0) {
                            matrix[r][c] = dummy;
                            return;
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
    			if(matrix[r][c]!=null && matrix[r][c].quantity>eps) {
    				IComposite r_comp=(IComposite)m_sources.getAllSpecies(r);
    				IComposite c_comp=(IComposite)m_destinations.getAllSpecies(c);
    				support.add(new Pair(r_comp, c_comp));
    			}
    		}
    	}
    	return support;
	}

 
    public static void main(String[] args) throws Exception {
    	
    	
    	
    	testInput1();
    	System.out.println();
    	testInput2();
    	System.out.println();
    	testInput3();
    	System.out.println();
    	
    	/*
    	TransportationProblemDoubleSimple problem = new TransportationProblemDoubleSimple();
        for (String filename : new String[]{"input1.txt", "input2.txt","input3.txt"}) {
            problem.init(filename);
            problem.northWestCornerRule();
            problem.steppingStone();
            problem.printResult(filename);
        }
        */
    }

	private static void testInput1() {
		/*
			2 3
			25 35
			20 30 10
			3 5 7
			3 2 5
		 */
		System.out.println("Optimal solution input1");
    	int nsuppliers=2;
    	int nconsumers=3;
    	double[] suppliers= {25,35};
    	double[] consumers= {20,30,10};
    	double[][] c =new double[nsuppliers][nconsumers];
    	c[0][0]=3;	c[0][1]=5;	c[0][2]=7;
    	c[1][0]=3;	c[1][1]=2;	c[1][2]=5;
    	
    	TransportationProblemDoubleTrash problem = new TransportationProblemDoubleTrash(suppliers,consumers,c);
    	problem.solve();
	}
	
	private static void testInput2() {
		/*
			3 3
			12 40 33
			20 30 10
			3 5 7
			2 4 6
			9 1 8
		 */
		System.out.println("Optimal solution input2");
    	int nsuppliers=3;
    	int nconsumers=3;
    	double[] suppliers= {12,40,33};
    	double[] consumers= {20,30,10};
    	double[][] c =new double[nsuppliers][nconsumers];
    	c[0][0]=3;	c[0][1]=5;	c[0][2]=7;
    	c[1][0]=2;	c[1][1]=4;	c[1][2]=6;
    	c[2][0]=9;	c[2][1]=1;	c[2][2]=8;
    	
    	TransportationProblemDoubleTrash problem = new TransportationProblemDoubleTrash(suppliers,consumers,c);
    	problem.solve();
	}
	
	private static void testInput3() {
		/*
			4 4
			14 10 15 12
			10 15 12 15
			
			10 30 25 15
			20 15 20 10
			10 30 20 20
			30 40 35 45
		 */
		System.out.println("Optimal solution input3");
    	int nsuppliers=4;
    	int nconsumers=4;
    	double[] suppliers= {14,10,15,12};
    	double[] consumers= {10,15,12,15};
    	double[][] c =new double[nsuppliers][nconsumers];
    	c[0][0]=10;	c[0][1]=30;	c[0][2]=25;	c[0][3]=15;
    	c[1][0]=20;	c[1][1]=15;	c[1][2]=20;	c[1][3]=10;
    	c[2][0]=10;	c[2][1]=30;	c[2][2]=20;	c[2][3]=20;
    	c[3][0]=30;	c[3][1]=40;	c[3][2]=35;	c[3][3]=45;
    	
    	TransportationProblemDoubleTrash problem = new TransportationProblemDoubleTrash(suppliers,consumers,c);
    	problem.solve();
	}


}