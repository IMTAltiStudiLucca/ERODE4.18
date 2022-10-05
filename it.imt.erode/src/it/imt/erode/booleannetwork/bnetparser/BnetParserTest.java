package it.imt.erode.booleannetwork.bnetparser;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedHashMap;

import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;

public class BnetParserTest {

	
	public static void main(String[] args) throws FileNotFoundException  {
		
		try {
			LinkedHashMap<String, IUpdateFunction> parsed = BNetParser.parseString(BNetParser.bnetExample);
			System.out.println("Ciao from string!");
			System.out.println(parsed);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 
		
		try {
			File f = new File("ap-1_else-0_wt.bnet");
			LinkedHashMap<String, IUpdateFunction> parsed = BNetParser.parseFile(f);
			System.out.println("Ciao from file");
			System.out.println(parsed);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
