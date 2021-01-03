package it.imt.erode.commandline;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.console.MessageConsoleStream;

public class EntryPointForPOPL2016 {

	public static void main(String[] args) {
		MessageConsoleStream out=null;
		BufferedWriter bwOut=null;
		String fileIn = "";
		String technique = "";
		String fileOut = "";
		if(args.length>1){
			fileIn=args[0];
			technique=args[1];
			fileOut=args[2];
		}
		else{
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the \".net\" or \".crn\" file to read the CRN from, the technique (either FDE or BDE),  and the name of the text file where to write information about the computed partition.");
			return;
		}
		List<String> commands = new ArrayList<String>();
		if(fileIn.endsWith(".net")){
			commands.add("importBNG({fileIn=>"+fileIn+"})");
		}
		else if(fileIn.endsWith(".crn")){
			commands.add("load({fileIn=>"+fileIn+"})");
		}
		else{
			throw new UnsupportedOperationException("Please, provide a Chemical Reaction Network encoded using either a .net or .crn file.");
		}
		
		if(technique.equalsIgnoreCase("FDE")){
			if((fileOut!=null) && (! fileOut.equals(""))){
				commands.add("this=reduceFDE({computeOnlyPartition=>true,fileWhereToStorePartition=>"+fileOut+"})");
			}
			else{
				commands.add("this=reduceFDE({computeOnlyPartition=>true})");
			}
			//store partition in fileOut if it is not null...
		}
		else if(technique.equalsIgnoreCase("BDE")){
			if((fileOut!=null) && (! fileOut.equals(""))){
				commands.add("this=reduceBDE({computeOnlyPartition=>true,prePartition=>IC,fileWhereToStorePartition=>"+fileOut+"})");
			}
			else{
				commands.add("this=reduceBDE({computeOnlyPartition=>true,prePartition=>IC})");
			}
			
			//store partition in fileOut if it is not null...
		}
		else{
			CRNReducerCommandLine.println(out,bwOut,"Please, specify the name of the \".net\" or \".crn\" file to read the CRN from, the technique (either FDE or BDE),  and the name of the text file where to write information about the computed partition.");
			return;
		}
		//commands.add("exportBNG({fileOut=>"+fileOut+"})");

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
