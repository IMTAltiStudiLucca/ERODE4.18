package it.imt.erode.commandline;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.console.MessageConsoleStream;

public class EntryPointForCONCUR2015 {

	public static void main(String[] args) {
		MessageConsoleStream out = null;
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
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the technique (either FB or BB), the name of the \".net\" file to read the CRN from, and the name of the file where to write the reduced CRN.");
			return;
		}
		List<String> commands = new ArrayList<String>();
		commands.add("importBNG({fileIn=>"+fileIn+"})");
		if(technique.equalsIgnoreCase("FB")){
			commands.add("this=reduceDSB()");
		}
		else if(technique.equalsIgnoreCase("BB")){
			commands.add("this=reduceEFL({icPrePartitioning=>true})");
		}
		else{
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the technique (either FB or BB), the name of the \".net\" file to read the CRN from, and the name of the file where to write the reduced CRN.");
			return;
		}
		commands.add("exportBNG({fileOut=>"+fileOut+"})");

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
