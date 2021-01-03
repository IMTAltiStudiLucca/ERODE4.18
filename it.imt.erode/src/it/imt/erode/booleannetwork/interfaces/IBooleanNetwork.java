package it.imt.erode.booleannetwork.interfaces;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.crn.implementations.Command;
import it.imt.erode.crn.interfaces.ICommand;
import it.imt.erode.crn.interfaces.ISpecies;

public interface IBooleanNetwork {

	List<ICommand> getCommands();

	void addCommand(Command command);

	void addSpecies(ISpecies node);

	void addUpdateFunction(String nodeName, IUpdateFunction updateFunction);
	
	LinkedHashMap<String, IUpdateFunction> getUpdateFunctions();

	void setAllUpdateFunctions(LinkedHashMap<String, IUpdateFunction> nodesToUpdateFunctions);

	String getName();

	void printBooleanNetwork();

	String toStringShort();

	void setUserDefinedPartition(ArrayList<HashSet<ISpecies>> userDefinedInitialPartition);

	ArrayList<HashSet<ISpecies>> getUserDefinedPartition();

	MessageConsoleStream getOut();

	BufferedWriter getBWOut();

	public List<ISpecies> getSpecies();

}
