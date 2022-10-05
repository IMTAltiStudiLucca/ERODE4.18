package it.imt.erode.booleannetwork.bnetparser;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedHashMap;

import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;
import it.imt.erode.booleannetwork.updatefunctions.BasicModelElementsCollector;
import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.importing.booleannetwork.GUIBooleanNetworkImporter;
import it.imt.erode.importing.booleannetwork.GuessPrepartitionBN;
import it.imt.erode.partition.interfaces.IPartition;

public class BNetLoaderTest {

	public static void main(String[] args) throws FileNotFoundException  {
		
//		try {
//			LinkedHashMap<String, IUpdateFunction> parsed = BNetParser.parseString(BNetParser.bnetExample);
//			System.out.println("Ciao from string!");
//			System.out.println(parsed);
//		} catch (ParseException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		 
		
		try {
			String modelName="ap-1_else-0_wt";
			File f = new File("ap-1_else-0_wt.bnet");
			LinkedHashMap<String, IUpdateFunction> parsed = BNetParser.parseFile(f);
			System.out.println("Ciao from file");
			System.out.println(parsed);
			
			
			GuessPrepartitionBN guessPrep=GuessPrepartitionBN.INPUTS;
			BasicModelElementsCollector bMec = new BasicModelElementsCollector(guessPrep, parsed);
			
			GUIBooleanNetworkImporter bnImporter = new GUIBooleanNetworkImporter(false,null,null,null,false);
			bnImporter.importBooleanNetwork(true, false, true, modelName, bMec.getInitialConcentrations(), bMec.getBooleanUpdateFunctions(), bMec.getUserPartition(), null);
			IBooleanNetwork bn=bnImporter.getBooleanNetwork();
			IPartition initial=bnImporter.getInitialPartition();
			
			System.out.println("Done");
			System.out.println(bn);
			System.out.println(initial);
			
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}


//