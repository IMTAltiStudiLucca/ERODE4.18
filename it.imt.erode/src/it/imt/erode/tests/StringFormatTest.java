package it.imt.erode.tests;

public class StringFormatTest {

	public static void main(String[] args) {
		double piVal = Math.PI;
	      
		   /* returns a formatted string using the specified format
		   string, and arguments */
		 System.out.println( String.format( "%.2f", piVal ) );

	}

}