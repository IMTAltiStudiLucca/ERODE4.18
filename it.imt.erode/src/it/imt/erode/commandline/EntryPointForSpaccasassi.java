package it.imt.erode.commandline;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.console.MessageConsoleStream;

public class EntryPointForSpaccasassi {

	public static void main(String[] args) {
		MessageConsoleStream out=null;
		BufferedWriter bwOut=null;
		//String technique = "FE";
		String fileIn = "";
		String fileOut = "";
		if(args.length>0){
			//technique=args[0];
			fileIn=args[0];
			if(args.length>1)
			fileOut=args[1];
		}
		else{
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the \".ode/.lbs/.net/.tra/csv\" file to read the CRN from. Optionally also the name of the file where to write the reduced CRN.");
			return;
		}
		List<String> commands = new ArrayList<String>();

		boolean isCTMC=false;
		boolean isFEM=false;
		boolean isBNG=false;
		boolean isCRN=false;
		boolean isLBS=false;

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
		else if(fileInLC.endsWith(".ode")){
			isCRN=true;
		}
		else if(fileInLC.endsWith(".lbs")){
			isLBS=true;
		}  

		if(!(isCTMC || isFEM || isBNG || isCRN || isLBS)){
			CRNReducerCommandLine.printWarning(out,bwOut,"Unsupported input file "+fileIn+". It must be either a .ode, a .lbs, a .csv, a .net, or a .tra. I terminate");
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
			else if(isLBS){
				commands.add("importLBS({fileIn=>"+fileIn+"})");
			}

			//commands.add("newline");
			commands.add("this=reduceFE()");
			//commands.add("newline");

			if(fileOut!=null && fileOut.length()>0) {
				if(isCRN){
					commands.add("write({fileOut=>"+fileOut+"})");
				}
				else{
					commands.add("exportLBS({fileOut=>"+fileOut+"})");
				}
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
