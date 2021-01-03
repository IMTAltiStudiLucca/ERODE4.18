package it.imt.erode.tests;

import java.util.Arrays;

public class TestSplitString {

	public static void main(String[] args) {
		
		String speciesName = "ciao-ciao";
		String[] split = speciesName.split("\\s+");
		System.out.println(Arrays.toString(split));

	}

}
