package it.imt.erode.tests;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestRegex {

	public static void main(String[] args) {
		String s= "014212*X0Antani";//"5*X0";
		Pattern p = Pattern.compile("[0-9]*\\*.*");
		Matcher m = p.matcher(s);
		boolean b=m.matches();
		System.out.println(b);

	}

}
