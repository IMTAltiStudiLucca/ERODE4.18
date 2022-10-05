package it.imt.erode.importing;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.crn.interfaces.ICRN;
//import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.ISpecies;

public class ModelicaImporter extends AbstractImporter
{
	public ModelicaImporter(String fileName,MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower) {
		super(fileName,out,bwOut,msgDialogShower);
	}
	
	
	public static void printDAEToModelicaFile(ICRN crn, String name , boolean exportICOfAlgebraic , MessageConsoleStream out , BufferedWriter bwOut )
	{
		String fileName = name;
		BufferedWriter bw;
		
		fileName=overwriteExtensionIfEnabled(fileName,".mo",true);
		try{
			bw = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printDAE, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		
		try {
			
			bw.write("model " + crn.getName() + "  \n");
			/*if(crn.getName()!=null&&crn.getName()!=""){
				String nam = getModelName(name);
				bw.write(nam+"\n");
			}
			else{
				bw.write("unnamed\n");
			}*/
			if(crn.getParameters()!=null && crn.getParameters().size()>0){
				
				for(String parameterDefinition : crn.getParameters()){
					String[] p=parameterDefinition.split("\\ ");
					//bw.write("  "+parameterDefinition+"\n");
					bw.write("parameter Real "+p[0]+" = "+p[1]+";\n");
				}
			}	
				for (ISpecies species : crn.getSpecies()) {
					if(species.isAlgebraic() && !exportICOfAlgebraic) {
						bw.write("Real "+species.getName()+";\n");
					}
					else {
						bw.write("Real "+species.getName()+"(start="+species.getInitialConcentration().doubleValue()+");\n");
					}
					
				}
				
				bw.write(" \n equation \n ");
				
				boolean ignoreOnes = false;
				boolean ignoreI=false;
				HashMap<ISpecies, StringBuilder> speciesToDrift = GUICRNImporter.computeDrifts(crn,ignoreOnes,ignoreI,false);
				
				for(ISpecies species : crn.getSpecies()){
					if(!species.isAlgebraic()) {
						String speciesDrift="0";
						StringBuilder speciesDriftSB = speciesToDrift.get(species);
						if(speciesDriftSB!=null){
							speciesDrift=speciesDriftSB.toString();
						}
						bw.write("  der(");
						bw.write(species.getName());
						bw.write(") = ");
						bw.write(speciesDrift);
						bw.write(" ;\n");
					}
					else
					{
						String speciesDrift="0";
						StringBuilder speciesDriftSB = speciesToDrift.get(species);
						if(speciesDriftSB!=null){
							speciesDrift=speciesDriftSB.toString();
						}
						bw.write("  ");
						bw.write(species.getName());
						bw.write(" = ");
						bw.write(speciesDrift);
						bw.write(" ;\n");
					}
				}
				
				/*for (ICRNReaction crnReaction : crn.getReactions()) {
					bw.write("  "+toStringModelicaFormat(crnReaction)+" ;\n");
				}*/
				
				
			
			
			bw.write("\n"+"end " + crn.getName() + ";\n\n");
			
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printDAE, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		finally{
			
			try {
				bw.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in printDAE, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}
	}
	
	/*private static String toStringModelicaFormat(ICRNReaction reaction)
	{
		String reagent;
		String products;
		
		reagent = reaction.getReagents().toMultiSet();
		products = reaction.getReagents().toMultiSet() ;
		
		return "der("+reagent+") = " + products;
	}*/
	
	
}