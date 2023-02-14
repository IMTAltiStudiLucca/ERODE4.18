package it.imt.erode.importing.cnf;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Adapted from https://www.geeksforgeeks.org/power-set/
 * Computes the powerset of a set (we get in input an arraylist, assuming it is a set
 * It also computes the empty set. 
 *
 */
public class PowerSet<T> {

	private ArrayList<T> set;
	private long pow_set_size;
	private int counter;
	
	public PowerSet(ArrayList<T> set) {
		this.set=set;
		/*set_size of power set of a set with set_size n is (2**n -1)*/
		pow_set_size =(long)Math.pow(2, set.size());
		counter=0;
	}
	
	public boolean hasMoreSets() {
		return counter < pow_set_size;
	}
	public Set<T> getNextSet(){
		
		if(!hasMoreSets()) {
			throw new IndexOutOfBoundsException("No more sets in the powerset");
		}
		
		Set<T> current = new LinkedHashSet<>();
		for(int j = 0; j < set.size(); j++)
		{
			/* Check if jth bit in the counter is set If set then print jth element from set */
			if((counter & (1 << j)) > 0) {
				current.add(set.get(j));
			}
		}
		
		//Prepare for next iteration
		counter++;
		
		return current;
	}
	
	/**
	 * Just a test
	 */
	public void printPowerSet()
	{
		int set_size=set.size();
		
		/*Run from counter 000..0 to 111..1*/
		for(int counter = 0; counter < pow_set_size; counter++)
		{
			for(int j = 0; j < set_size; j++)
			{
				/* Check if jth bit in the counter is set If set then print jth element from set */
				if((counter & (1 << j)) > 0)
					System.out.print(set.get(j));
			}
			System.out.println();
		}
	}

	// Driver program to test printPowerSet
	public static void main(String[] args)
	{
		ArrayList<String> set=new ArrayList<>();
		set.add("a");
		set.add("b");
		set.add("c");
		PowerSet<String> ps=new PowerSet<>(set);
		ps.printPowerSet();
		
		while(ps.hasMoreSets()) {
			System.out.println(ps.getNextSet());
		}
	}

}
