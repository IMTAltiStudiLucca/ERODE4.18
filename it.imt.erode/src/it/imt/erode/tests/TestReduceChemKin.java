package it.imt.erode.tests;

import java.util.ArrayList;
import java.util.List;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.CommandsReader;

public class TestReduceChemKin {

	public static void main(String[] args) {

		List<String> commands = new ArrayList<String>();
		if(args.length>=2){
			commands.add("importChemKin({fileIn=>"+args[0]+",thermoDynamicFile=>"+args[1]+"})");
		}
		else{
			commands.add("importChemKin({fileIn=>"+args[0]+"})");
		}
		commands.add("write({fileOut=>"+args[0]+".crn})");
		commands.add("newLine");
		commands.add("reduceWFB({reducedFile=>"+args[0]+"WFBWithTolerance.crn})");
		commands.add("newLine");
		/*commands.add("reduceFDE({reducedFile=>"+args[0]+"FDEInvertedVki.crn})");
		commands.add("newLine");*/
		//commands.add("reduceEFL({reducedFile=>"+args[0]+"BBInvertedVki.crn})");
		//commands.add("newLine");

		CommandsReader commandsReader = new CommandsReader(commands,null,null);

		CRNReducerCommandLine cl = new CRNReducerCommandLine(commandsReader);
		try {
			cl.executeCommands(true,null,null);
		} catch (Exception e) {
			System.out.println("Unhandled errors arised while executing the commands. I terminate.");
			e.printStackTrace();
		}

	}

}
