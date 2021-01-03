package it.imt.erode.tests;

import java.util.ArrayList;
import java.util.List;


public class PrintAllPermutations {

	public static void main(String[] args) {
//		int[] xpromotes=new int[4];
//		xpromotes[0]=0;
//		xpromotes[1]=1;
//		xpromotes[2]=2;
//		xpromotes[3]=3;
		
		int[] xpromotes=new int[3];
		xpromotes[0]=0;
		xpromotes[1]=1;
		xpromotes[2]=2;
		
		PrintAllPermutations pap = new PrintAllPermutations();
		List<List<Integer>> permuations = pap.permute(xpromotes);
		System.out.println("There are "+permuations.size()+" permutations");
		for(List<Integer> perm : permuations){
			//int[] p = new int[perm.size()];
			System.out.println(perm);
		}

	}
	
	
	public List<List<Integer>> permute(int[] nums) {
	    List<List<Integer>> results = new ArrayList<List<Integer>>();
	    if (nums == null || nums.length == 0) {
	        return results;
	    }
	    List<Integer> result = new ArrayList<>();
	    dfs(nums, results, result);
	    return results;
	}

	public void dfs(int[] nums, List<List<Integer>> results, List<Integer> result) {
	    if (nums.length == result.size()) {
	        List<Integer> temp = new ArrayList<>(result);
	        results.add(temp);
	    }        
	    for (int i=0; i<nums.length; i++) {
	        if (!result.contains(nums[i])) {
	            result.add(nums[i]);
	            dfs(nums, results, result);
	            result.remove(result.size() - 1);
	        }
	    }
	}

}
