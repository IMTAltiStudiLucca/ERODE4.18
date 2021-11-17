package it.imt.erode.booleannetwork.implementations;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.Collection;
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
	private LinkedHashMap<String, Integer> nameToMax;
	private String name;
	
	private MessageConsoleStream out;
	private BufferedWriter bwOut;
	private ArrayList<HashSet<ISpecies>> userDefinedInitialPartition=new ArrayList<>(0);
	
	private List<ICommand> commands;
	private boolean multivalued=false;
	
	public BooleanNetwork(String name, MessageConsoleStream out,BufferedWriter bwOut, boolean multivalued) {
		super();
		this.out=out;
		this.bwOut=bwOut;
		this.name=name;
		this.allSpecies = new ArrayList<>();
		this.updateFunctions = new LinkedHashMap<>();
		this.multivalued=multivalued;
		nameToMax=new LinkedHashMap<>();
	}
//	public BooleanNetwork(String name, MessageConsoleStream out,BufferedWriter bwOut) {
//		this(name,out,bwOut,false);
//	}
	
	public boolean isMultiValued() {
		return multivalued;
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

	@Override
	public void setMax(ISpecies newSp, Integer max) {
		if(!multivalued) {
			throw new UnsupportedOperationException("The method setMax should be used only of multivalued networks");
		}
		if(max==null||max==1) {
			//If 1 do nothing, that's the default 
		}
		if(max<1) {
			throw new UnsupportedOperationException("The max value of a multivalued species should be at least 1");
		}
		if(max>1) {
			nameToMax.put(newSp.getName(), max);
		}
	}

	@Override
	public int cumulMax(Collection<ISpecies> species) {
		int cumul=0;
		for(ISpecies sp : species) {
			Integer cur=nameToMax.get(sp.getName());
			if(cur==null) {
				cur=1;
			}
			cumul+=cur;
		}
		return cumul;
	}
	
	@Override
	public LinkedHashMap<String, Integer> getNameToMax(){
		return nameToMax;
	}
	@Override
	public int getNameToMax(String speciesName){
		Integer ret = nameToMax.get(speciesName);
		if(ret==null) {
			return 1;
		}
		else {
			return ret;
		}
	}

}
