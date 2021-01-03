package it.imt.erode.commandline;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.console.MessageConsoleStream;

public class entryPointForCAV2015 {

	public static void main(String[] args) {
		MessageConsoleStream out = null;
		BufferedWriter bwOut=null;
		String fileIn = "";
		String fileOut = "";
		if(args.length>1){
			fileIn=args[0];
			fileOut=args[1];
		}
		else{
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the file to read the CRN, and the name of the file where to write the DSB-reduced CRN.");
			return;
		}
		List<String> commands = new ArrayList<String>();
		commands.add("importBNG({fileIn=>"+fileIn+"})");
		commands.add("this=reduceDSB()");
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
