package it.imt.erode.commandline;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.console.MessageConsoleStream;

public class entryPointForSnoopy {

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
			CRNReducerCommandLine.println(out,bwOut,"Please, specify both the input file (original model), and the output one (reduced model).");
			return;
		}
		List<String> commands = new ArrayList<String>();
		commands.add("load({fileIn=>"+fileIn+"})");
		commands.add("this=reduceFE({prePartition=>USER})");
		commands.add("write({fileOut=>"+fileOut+"})");

		CommandsReader commandsReader = new CommandsReader(commands,out,bwOut);
		CRNReducerCommandLine cl = new CRNReducerCommandLine(commandsReader);
		cl.fromGUI=true;
		try {
			cl.executeCommands(true,out,bwOut);
		} catch (Exception e) {
			CRNReducerCommandLine.println(out,bwOut,"Unhandled errors arised while executing the commands. I terminate.");
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}
	}

}
