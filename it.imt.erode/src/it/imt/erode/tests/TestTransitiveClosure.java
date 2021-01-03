package it.imt.erode.tests;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class TestTransitiveClosure {

	public static void main(String[] args) {
		String x0="x0";
		String x1="x1";
		String x2="x2";
		String x3="x3";
		String x4="x4";
		String x5="x5";
		String x6="x6";
		String x7="x7";
		String x8="x8";
		

		ArrayList<String> x0Next = new ArrayList<String>();
		ArrayList<String> x1Next = new ArrayList<String>();
		ArrayList<String> x2Next = new ArrayList<String>();
		ArrayList<String> x3Next = new ArrayList<String>();
		ArrayList<String> x4Next = new ArrayList<String>();
		ArrayList<String> x5Next = new ArrayList<String>();
		ArrayList<String> x6Next = new ArrayList<String>();
		ArrayList<String> x7Next = new ArrayList<String>();
		ArrayList<String> x8Next = new ArrayList<String>();
		

		LinkedHashMap<String, ArrayList<String>> speciesToNext = new LinkedHashMap<String, ArrayList<String>>(7);
		speciesToNext.put(x0, x0Next);
		speciesToNext.put(x1, x1Next);
		speciesToNext.put(x2, x2Next);
		speciesToNext.put(x3, x3Next);
		speciesToNext.put(x4, x4Next);
		speciesToNext.put(x5, x5Next);
		speciesToNext.put(x6, x6Next);
		speciesToNext.put(x7, x7Next);
		speciesToNext.put(x8, x8Next);
		
		
		//x1 equal to x2 ; x2 equal to x3 and x4
		x1Next.add(x2); x2Next.add(x1);
		
		x2Next.add(x3); x3Next.add(x2);
		x2Next.add(x4); x4Next.add(x2);
		
		
		/*
		//x0 equal to x1 ; x1 equal to x2 ; x2 equal to x3 and x4
		x0Next.add(x1); x1Next.add(x0);
		
		x1Next.add(x2); x2Next.add(x1);
		
		x2Next.add(x3); x3Next.add(x2); 
		x2Next.add(x4); x4Next.add(x2);
		*/
		
		/*
		//x0 = x1 ; x1 = x2 ; x2 = x3 and x4 ; x4 = x6 ; x5 = x6 
		x0Next.add(x1); x1Next.add(x0); 
		
		x1Next.add(x2); x2Next.add(x1);
		
		x2Next.add(x3); x3Next.add(x2);
		x2Next.add(x4); x4Next.add(x2);
		
		x4Next.add(x6); x6Next.add(x4);
		
		x5Next.add(x6); x6Next.add(x5);
		
		//x7Next.add(x8); x8Next.add(x7);
		 */

		ArrayList<ArrayList<String>> refinement = new ArrayList<ArrayList<String>>();

		boolean[] alreadyConsidered = new boolean[9];
		for(int i=0;i<alreadyConsidered.length;i++){
			if(!alreadyConsidered[i]){
				alreadyConsidered[i]=true;
				String species = "x"+String.valueOf(i);
				ArrayList<String> reachable = new ArrayList<>();
				refinement.add(reachable);

				reachable.add(species);
				ArrayList<String> next = speciesToNext.get(species);
				if(next!=null){
					for (String nextSpecies : next) {
						addAllReachable(reachable,nextSpecies,speciesToNext,alreadyConsidered);
					}
				}
			}
		}
		System.out.println("We have "+refinement.size()+" blocks");
		System.out.println(refinement.toString());
	}

	private static void addAllReachable(ArrayList<String> reachable, String nextSpecies,LinkedHashMap<String, ArrayList<String>> speciesToNext, boolean[] alreadyConsidered) {
		int id = getId(nextSpecies);
		if(!alreadyConsidered[id]){
			alreadyConsidered[id]=true;
			reachable.add(nextSpecies);
			ArrayList<String> next = speciesToNext.get(nextSpecies);
			if(next!=null){
				for (String nextNextSpecies : next) {
					addAllReachable(reachable,nextNextSpecies,speciesToNext,alreadyConsidered);
				}
			}

		}
	}

	private static int getId(String nextSpecies) {
		return Integer.valueOf(nextSpecies.substring(1, 2));
	}


}
