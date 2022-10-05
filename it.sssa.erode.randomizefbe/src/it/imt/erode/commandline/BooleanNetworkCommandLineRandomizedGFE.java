package it.imt.erode.commandline;

import java.io.BufferedWriter;
import java.io.IOException;

import org.eclipse.ui.console.MessageConsoleStream;

import com.microsoft.z3.Z3Exception;

import it.imt.erode.auxiliarydatastructures.PartitionAndStringAndBoolean;
import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.booleannetworks.FBEAggregationFunctions;
import it.imt.erode.partitionrefinement.algorithms.booleannetworks.RandomizedForwardBooleanEquivalence;

public class BooleanNetworkCommandLineRandomizedGFE extends BooleanNetworkCommandLine {

	public BooleanNetworkCommandLineRandomizedGFE(CommandsReader commandsReader, IBooleanNetwork bn,
			IPartition partition, boolean fromGUI) {
		super(commandsReader, bn, partition, fromGUI);
	}
	
	@Override
	public boolean isSupportedBySubclass(String command) {
		if(command.startsWith("reduceRandomizedFBE(")) {
			//handleExportSBMLQualCommand(command, out, bwOut);
			return true;
		}
		return false;
	}
	
	@Override
	public PartitionAndStringAndBoolean reduceBySubClass(String reduction, FBEAggregationFunctions aggr,IPartition initial, MessageConsoleStream out, BufferedWriter bwOut, boolean print, boolean realIfMV) throws Z3Exception, IOException {		
		boolean simplify=false;
		RandomizedForwardBooleanEquivalence rndFBE = new RandomizedForwardBooleanEquivalence(aggr, simplify);
		PartitionAndStringAndBoolean ps =rndFBE.computeOFLsmt(getBN(), initial, CRNReducerCommandLine.verbose, out, bwOut, print, terminator,false);
		return ps;
	}
	
	@Override
	public boolean isReductionSupportedBySubCalss(String reduction) {
		if(reduction.equalsIgnoreCase("rndfbe")) {
			return true;
		}
		return false;
	}
	
	@Override
	public boolean needsAggregationFunctionSubClass(String reduction) {
		if(reduction.equalsIgnoreCase("rndfbe")) {
			return true;
		}
		return false;
	}

}
