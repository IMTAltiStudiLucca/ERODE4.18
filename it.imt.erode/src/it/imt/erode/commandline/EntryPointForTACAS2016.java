package it.imt.erode.commandline;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.console.MessageConsoleStream;

public class EntryPointForTACAS2016 {

	public static void main(String[] args) {
		MessageConsoleStream out=null;
		BufferedWriter bwOut=null;
		String technique = "";
		String fileIn = "";
		String fileOut = "";
		if(args.length>1){
			technique=args[0];
			fileIn=args[1];
			fileOut=args[2];
		}
		else{
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the technique (either FB, BB, FB_old or BB_old), the name of the \".net/.tra/csv\" file to read the CRN from, and the name of the file where to write the reduced CRN.");
			return;
		}
		List<String> commands = new ArrayList<String>();

		boolean isCTMC=false;
		boolean isFEM=false;
		boolean isBNG=false;
		boolean isCRN=false;

		String fileInLC = fileIn.toLowerCase();
		if(fileInLC.endsWith(".tra")){
			isCTMC=true;
		}
		else if(fileInLC.endsWith(".csv")){
			isFEM=true;
		} 
		else if(fileInLC.endsWith(".net")){
			isBNG=true;
		} 
		else if(fileInLC.endsWith(".crn")){
			isCRN=true;
		}  

		if(!(isCTMC || isFEM || isBNG || isCRN)){
			CRNReducerCommandLine.printWarning(out,bwOut,"Unsupported input file "+fileIn+". It must be either a .csv, a .net, a .crn or a .tra. I terminate");
		}
		else{

			if(isBNG){
				commands.add("importBNG({fileIn=>"+fileIn+"})");
			}
			else if(isCRN){
				commands.add("load({fileIn=>"+fileIn+"})");
			}
			else if(isFEM){
				commands.add("importLinearSystemAsCCSVMatrix({fileIn=>"+fileIn+",form=>FEM})");
			}
			else if(isCTMC){
				commands.add("importMRMC({fileIn=>"+fileIn+",labellingFile=>same})");
			}

			commands.add("newline");
						
			if(technique.equalsIgnoreCase("FB_old")){
				if(isCTMC){
					commands.add("this=reduceDSB({prePartition=>views})");
				}
				else{
					commands.add("this=reduceDSB()");
				}
			}
			else if(technique.equalsIgnoreCase("FB")){
				if(isCTMC){
					commands.add("this=reduceFB({prePartition=>views})");
				}
				else{
					commands.add("this=reduceFB()");
				}
			}
			else if(technique.equalsIgnoreCase("BB_old")){
				if(isCTMC){
					commands.add("this=reduceEFL({prePartition=>views})");
				}
				else if(isBNG){
					commands.add("this=reduceEFL({prePartition=>IC})");
				}
				else {
					commands.add("this=reduceEFL()");
				}
			}
			else if(technique.equalsIgnoreCase("BB")){
				if(isCTMC){
					commands.add("this=reduceBB({prePartition=>views})");
				}
				else if(isBNG){
					commands.add("this=reduceBB({prePartition=>IC})");
				}
				else {
					commands.add("this=reduceBB()");
				}
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"Please, specify the technique (either FB, BB, FB_old or BB_old), the name of the \".net/.tra/csv\" file to read the CRN from, and the name of the file where to write the reduced CRN.");
				return;
			}
			
			commands.add("newline");

			if(isCRN){
				commands.add("write({fileOut=>"+fileOut+"})");
			}
			else{
				commands.add("exportBNG({fileOut=>"+fileOut+"})");
			}

			CommandsReader commandsReader = new CommandsReader(commands,out,bwOut);
			CRNReducerCommandLine cl = new CRNReducerCommandLine(commandsReader);
			try {
				cl.executeCommands(true,out,bwOut);
			} catch (Exception e) {
				CRNReducerCommandLine.println(out,bwOut,"Unhandled errors arised while executing the commands. I terminate.");
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}
	}

}
