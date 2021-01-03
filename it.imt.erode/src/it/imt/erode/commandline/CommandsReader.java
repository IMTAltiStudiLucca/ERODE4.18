package it.imt.erode.commandline;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.ui.console.MessageConsoleStream;

public class CommandsReader {

	private LinkedList<String> commands;
	private BufferedReader console;
	private boolean interactive;
	//private boolean fromGUI=false;
	private MessageConsoleStream out;
	private BufferedWriter bwOut;
	
	public static boolean PRINTHELPADVICE=false;
	
	private CommandsReader(){
		commands = new LinkedList<String>();
		interactive = false;
	}
	
	public CommandsReader(Collection<String> commands,MessageConsoleStream out,BufferedWriter bwOut){
		this();
		this.out=out;
		this.bwOut=bwOut;
		for (String command : commands) {
			this.commands.add(command);
		}
	}
	
	public CommandsReader(String[] args,boolean fromGUI, MessageConsoleStream out,BufferedWriter bwOut) {
		this();
		this.out=out;
		for(int i=0;i<args.length;i++){
			String arg=args[i].trim();
			if(arg.equalsIgnoreCase("-i")|| arg.equalsIgnoreCase("--interactive")){
				interactive=true;
			}
			else if(arg.equalsIgnoreCase("-c")|| arg.equalsIgnoreCase("--command")){
				i++;
				if(i<args.length){
					arg=args[i].trim();
					if(arg.startsWith("load({")){
						commands.add(arg);
					}
					else{
						commands.add("load({fileIn=>"+arg+"})");
					}
				}
			}
			else{
				//CRNReducerCommandLine.println("Unknown command \""+arg+"\". I skip it. Type --help for usage instructions.");
				CRNReducerCommandLine.println(out,bwOut,"Unknown command \""+arg+"\". I skip it.");
				if(CommandsReader.PRINTHELPADVICE) 
					CRNReducerCommandLine.println(out,bwOut,"Type --help for usage instructions.");
				//CRNReducerCommandLine.usage();
			}
		}
		if(commands.isEmpty()&&!interactive){
			CRNReducerCommandLine.println(out,bwOut,"When the tool is executed in non interactive mode, it is mandatory to provide a command, e.g., \"java -jar CRNReducerVERSION.jar -c commands.crn\", where commands.crn is a file containing a list of commands exemplified below.");
			CRNReducerCommandLine.usageShort(fromGUI,out,bwOut);
		}
	}
	
	public String getNextCommand() throws IOException {
		if(console==null){
			console = new BufferedReader(new InputStreamReader(System.in));
		}
		while(commands.isEmpty()){
			boolean terminate = true;
			if(interactive){
				terminate = readCommandsWhenEmpty();
				if(terminate){
					return "quit()";
				}
			}
			else{
				return "NONINTERACTIVE";
			}
			
		}
		return commands.removeFirst();
	}
	
	public boolean readCommandsWhenEmpty() {

		CRNReducerCommandLine.println(out,bwOut,"\nWaiting for commands...\n");
		String line;
		try {
			line = console.readLine();
			line=line.replace(" ", "");

			if(line.equalsIgnoreCase("quit")||line.equalsIgnoreCase("quit()")){
				//CRNReducerCommandLine.println("I am terminating.");
				console.close();
				console=null;
				return true;
			}
			else{
				commands.add(line);
			}
			
			//Only one command per line
			/*String commandsArray[] = line.split(" ");
			for(int i=0;i<commandsArray.length;i++){
				commands.add(commandsArray[i].trim());
			}*/
			return false;
		} catch (IOException e) {
			//e.printStackTrace();
			return true;
		} catch (NullPointerException e) {
			//e.printStackTrace();
			return true;
		}
	}

	public void addToHead(List<String> furtherCommands) {
		commands.addAll(0, furtherCommands);
	}
	
	public void addToHead(String furtherCommand) {
		commands.add(0, furtherCommand);
	}
	
	public Collection<String> getCommands(){
		return commands;
	}
	
	/*public void addToTail(List<String> furtherCommands) {
		commands.addAll(furtherCommands);
	}*/

}
