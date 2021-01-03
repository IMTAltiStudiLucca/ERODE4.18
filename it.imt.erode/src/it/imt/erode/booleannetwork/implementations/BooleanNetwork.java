package it.imt.erode.booleannetwork.implementations;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;
import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.crn.implementations.Command;
import it.imt.erode.crn.interfaces.ICommand;
import it.imt.erode.crn.interfaces.ISpecies;

public class BooleanNetwork implements IBooleanNetwork {

	private List<ISpecies> allSpecies;
	private LinkedHashMap<String, IUpdateFunction> updateFunctions;
	private String name;
	
	private MessageConsoleStream out;
	private BufferedWriter bwOut;
	private ArrayList<HashSet<ISpecies>> userDefinedInitialPartition=new ArrayList<>(0);
	
	private List<ICommand> commands;
	
	public BooleanNetwork(String name, MessageConsoleStream out,BufferedWriter bwOut) {
		super();
		this.out=out;
		this.bwOut=bwOut;
		this.name=name;
		this.allSpecies = new ArrayList<>();
		this.updateFunctions = new LinkedHashMap<>();
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public List<ICommand> getCommands() {
		return commands;
	}
	
	@Override
	public void addCommand(Command command) {
		if(commands==null){
			commands = new ArrayList<>();
		}
		commands.add(command);
	}

	@Override
	public List<ISpecies> getSpecies() {
		return allSpecies;
	}
	
	@Override
	public void addSpecies(ISpecies species) {
		if(species.getName().equalsIgnoreCase("e")){
			CRNReducerCommandLine.printWarning(out,bwOut,"The use of \"e\" or \"E\" as name is deprecated.");
		}
		allSpecies.add(species);
	}
	
	@Override
	public void addUpdateFunction(String speciesName, IUpdateFunction updateFunction){
		updateFunctions.put(speciesName, updateFunction);
	}
	
	@Override
	public void setAllUpdateFunctions(LinkedHashMap<String, IUpdateFunction> speciesToUpdateFunctions){
		updateFunctions=speciesToUpdateFunctions;
	}
	
	
	@Override
	public void printBooleanNetwork(){
		CRNReducerCommandLine.println(out,bwOut, toString());
		//System.out.println(toString());
	}
	
	@Override
	public String toString() {
		StringBuilder sb;
		if(getName()==null){
			sb = new StringBuilder(getSpecies().size()+" species.\n");
		}
		else{
			sb = new StringBuilder(getName()+": "+getSpecies().size()+" species.\n");
		}
		sb.append("Species:\n");
		sb.append(getSpecies().toString());
		return sb.toString();
	}

	@Override
	public String toStringShort() {
		String in="";
		if(getName()==null){
			in= in+getSpecies().size()+" species";
		}
		else{
			in = getName()+".\n\t"+getSpecies().size()+" species";
		}
		
		in+=". ";
		
		return in;
	}
	
	@Override
	public void setUserDefinedPartition(ArrayList<HashSet<ISpecies>> userDefinedInitialPartition) {
		this.userDefinedInitialPartition=userDefinedInitialPartition;
	}
	
	@Override
	public ArrayList<HashSet<ISpecies>> getUserDefinedPartition() {
		return userDefinedInitialPartition;
	}
	
	@Override
	public MessageConsoleStream getOut() {
		return out;
	}
	@Override
	public BufferedWriter getBWOut() {
		return bwOut;
	}

	@Override
	public LinkedHashMap<String, IUpdateFunction> getUpdateFunctions() {
		return updateFunctions;
	}

}
