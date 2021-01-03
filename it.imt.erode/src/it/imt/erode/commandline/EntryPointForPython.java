package it.imt.erode.commandline;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.eclipse.ui.console.MessageConsoleStream;

import com.microsoft.z3.Z3Exception;

import it.imt.erode.auxiliarydatastructures.IPartitionAndBoolean;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;

import py4j.GatewayServer;

/**
 * This class can be used to invoke the command line of ERODE from pyhton
 * 
 * This is a sample python code that interacts with ERODE (assuming an executable jar has been created pointing to the main of class, and that it has been launched)
 * 
 *

from py4j.java_gateway import JavaGateway

import numpy as np

p = np.zeros(18)

gateway = JavaGateway()

n = gateway.entry_point.loadCRN("ncc.crn")

h = gateway.entry_point.computeBB()

#p = [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0]

int_class = gateway.jvm.int
int_array = gateway.new_array(int_class,18)
#int_array = 
print int_array[17]

hi = gateway.entry_point.computeBB(int_array,False)

#gateway.entry_point.printPartition(h)

print hi[0]

 * 
 * @author andrea vandin
 *
 */
public class EntryPointForPython {

	//private List<String> commands = new ArrayList<String>();
	private CRNReducerCommandLine crnreducer;
	private ISpecies[] idToSpecies; 
	private boolean printPartitions;
	private boolean printCRNs;
	private MessageConsoleStream out = null;
	private BufferedWriter bwOut=null;
	private String[] idToSpeciesNames;

	public EntryPointForPython(boolean printPartitions, boolean printCRNs){
		crnreducer = new CRNReducerCommandLine(new CommandsReader(new ArrayList<String>(0),out,bwOut));
		this.printPartitions=printPartitions;
		this.printCRNs=printCRNs;
		CRNReducerCommandLine.println(out,bwOut,"ERODE instantiated");

	}

	public static void main(String[] args) throws UnsupportedFormatException, Z3Exception, IOException {
		int port=-1;
		if(args.length==1){
			port=Integer.valueOf(args[0]);
		}
		
		//CRNReducerCommandLine.println("BBB");
		//CRNReducerCommandLine.println("BBB");
		EntryPointForPython entry = new EntryPointForPython(false,true);
		//entry.importBNG("./BNGNetworks/Mre.net");
		//entry.importBNG("./BNGNetworks/max.net");
		//entry.computeBB(new int[]{2,2,3,3,3});
		//entry.computeBB(new int[]{3,3,3,3,3});
		//crnreducer = new CRNReducerCommandLine(new CommandsReader(new ArrayList<String>(0)));
		//CRNReducerCommandLine.println("CRNReducer instantiated");
		
		GatewayServer gatewayServer = null;
		if(port==-1){
			gatewayServer=new GatewayServer(entry);
		}
		else{
			gatewayServer=new GatewayServer(entry,port);
		}
				
		gatewayServer.start();
		
		
		CRNReducerCommandLine.println(entry.out,entry.bwOut,"Gateway server started on port "+gatewayServer.getPort()+ " (while pythonPort is "+gatewayServer.getPythonPort()+", and pythonAddress is "+gatewayServer.getPythonAddress().toString()+")");
		
		//entry.loadCRN("./CRNNetworks/mi1.crn");
		//entry.loadCRN("./BNGNetworks/mi1.net");
		//double[][] Je = entry.computeJacobian();
		
		
		/*entry.loadCRN("./CRNNetworks/gw.crn");
		int[] bb = entry.computeBB();
		entry.computeJacobian(bb);
		
		entry.loadCRN("./CRNNetworks/mi.crn");
		bb = entry.computeBB();
		entry.computeJacobian(bb);
		
		entry.loadCRN("./CRNNetworks/ncc.crn");
		bb = entry.computeBB();
		entry.computeJacobian(bb);*/
		
		/*entry.loadCRN("./CRNNetworks/mi.crn");
		int[] nfb = entry.computeNFB();
		entry.computeJacobian(nfb);*/
		
		//entry.loadCRN("./CRNNetworks/am.crn");
		//entry.computeJacobian(new double[]{1.0 , 0.0, 2.0});
		//entry.computeJacobian(new double[]{1.0 , 2.0, 3.0});
		//entry.computeJacobian(new double[]{0.0 , 4.0, 3.0});
		//double[][] j2 = entry.computeJacobian(new double[]{1.0 , 0.001 , 2.0});
		//System.out,bwOut.println("ciao");
		
		//entry.loadCRN("./CRNNetworks/simpncc.crn");
		//entry.computeBB();
		
	}

	public int loadCRN(String fileName){
		if(fileName.endsWith(".crn")){
			return importCRN(fileName);
		}
		else if(fileName.endsWith(".net")){
			return importBNG(fileName);
		}
		
		throw new UnsupportedOperationException("Either .crn or .net formats are supported");
		
	}
	
	
	private int importBNG(String fileName){
		crnreducer.handleImportBNGCommand("importBNG({fileIn=>"+fileName+"})",out,bwOut);
		return completeImporting();
	}
	
	private int importCRN(String fileName){
		crnreducer.handleLoadCommand("load({fileIn=>"+fileName+"})",false,out,bwOut);
		return completeImporting();
	}

	private int completeImporting() {
		CRNReducerCommandLine.println(out,bwOut);
		idToSpecies=new ISpecies[crnreducer.getCRN().getSpecies().size()];
		idToSpeciesNames=new String[idToSpecies.length];
		int i=0;
		for (ISpecies species : crnreducer.getCRN().getSpecies()) {
			idToSpecies[i]=species;
			idToSpeciesNames[i]=species.getName();
			i++;
		}

		/*for(i=0;i<idToSpecies.length;i++){
			ISpecies species = idToSpecies[i];
			species.setInitialConcentration(BigDecimal.ONE, "1.0");
		}*/

		if(printCRNs){
			CRNReducerCommandLine.println(out,bwOut,crnreducer.getCRN());
		}

		return crnreducer.getCRN().getSpecies().size();
	}

	public int[] computeBB() throws UnsupportedFormatException, Z3Exception, IOException{
		int[] initialPartitionArray = new int[idToSpecies.length];
		Arrays.fill(initialPartitionArray, 1);
		return computeBB(initialPartitionArray);
	}
	
	public int[] computeBB(int[] initialPartitionArray, boolean numbersAreIDOfRepresentativeSpecies) throws UnsupportedFormatException, Z3Exception, IOException{

		IPartition initialPartition = importPartition(initialPartitionArray,numbersAreIDOfRepresentativeSpecies);
		crnreducer.setPartition(initialPartition);
		if(printPartitions){
			CRNReducerCommandLine.println(out,bwOut,"Initial partition:\n"+initialPartition);
		}


		IPartitionAndBoolean obtainedPartitionAndBool = crnreducer.handleReduceCommand("reduceBE({computeOnlyPartition=>true,print=>false})",false,"be",out,bwOut);
		IPartition obtainedPartition = obtainedPartitionAndBool.getPartition(); 
		//IPartition obtainedPartition = crnreducer.handleReduceCommand("reduceEFL({computeOnlyPartition=>true,print=>false})",false,"EFL");
		if(printPartitions){
			CRNReducerCommandLine.println(out,bwOut,"Obtained partition:\n"+obtainedPartition);
		}

		int[] obtainedPartitionToExport = exportPartition(obtainedPartition);
		return obtainedPartitionToExport;
	}
	
	public int[] computeBB(int[] initialPartitionArray) throws UnsupportedFormatException, Z3Exception, IOException{
		return computeBB(initialPartitionArray,true);
	}
	
	public int[] computeFE() throws UnsupportedFormatException, Z3Exception, IOException{
		int[] initialPartitionArray = new int[idToSpecies.length];
		Arrays.fill(initialPartitionArray, 1);
		return computeFE(initialPartitionArray);
	}
	
	public int[] computeFE(int[] initialPartitionArray) throws UnsupportedFormatException, Z3Exception, IOException{

		IPartition initialPartition = importPartition(initialPartitionArray);
		crnreducer.setPartition(initialPartition);
		if(printPartitions){
			CRNReducerCommandLine.println(out,bwOut,"Initial partition:\n"+initialPartition);
		}


		IPartitionAndBoolean obtainedPartitionAndBool = crnreducer.handleReduceCommand("reduceFE({computeOnlyPartition=>true,print=>false})",false,"fe",out,bwOut);
		IPartition obtainedPartition = obtainedPartitionAndBool.getPartition();
		//IPartition obtainedPartition = crnreducer.handleReduceCommand("reduceEFL({computeOnlyPartition=>true,print=>false})",false,"EFL");
		if(printPartitions){
			CRNReducerCommandLine.println(out,bwOut,"Obtained partition:\n"+obtainedPartition);
		}

		int[] obtainedPartitionToExport = exportPartition(obtainedPartition);
		return obtainedPartitionToExport;
	}
	
	public String[] getSpeciesNames(){
		return idToSpeciesNames;
	}
	
	public void printPartition(int[] partitionArray){
		//IPartition partition = importPartition(partitionArray);
		//CRNReducerCommandLine.println(out,bwOut,partition);
		printPartition(partitionArray,true);
	}
	
	public void printPartition(int[] partitionArray, boolean numbersAreIDOfRepresentativeSpecies){
		IPartition partition = importPartition(partitionArray,numbersAreIDOfRepresentativeSpecies);
		CRNReducerCommandLine.println(out,bwOut,partition);
	}
	
	private IPartition importPartition(int[] initialPartitionArray, boolean numbersAreIDOfRepresentativeSpecies){
		IPartition initialPartition = new Partition(initialPartitionArray.length);
		HashMap<Integer,IBlock> initialPartitionHM=new HashMap<>();
		if(numbersAreIDOfRepresentativeSpecies){
			for(int i=0;i<initialPartitionArray.length;i++){
				int speciesId = i;
				int repSpeciesId = initialPartitionArray[i]-1;
				IBlock block = initialPartitionHM.get(repSpeciesId);
				if(block==null){
					block = new Block();
					initialPartition.add(block);
					block.addSpecies(idToSpecies[repSpeciesId]);
					initialPartitionHM.put(repSpeciesId, block);
				}
				block.addSpecies(idToSpecies[speciesId]);
			}
		}
		else{
			for(int i=0;i<initialPartitionArray.length;i++){
				int speciesId = i;
				IBlock block = initialPartitionHM.get(initialPartitionArray[i]);
				if(block==null){
					block = new Block();
					initialPartition.add(block);
					initialPartitionHM.put(initialPartitionArray[i], block);
				}
				block.addSpecies(idToSpecies[speciesId]);
			}
		}
		return initialPartition;
	}
	
	private IPartition importPartition(int[] initialPartitionArray){
		return importPartition(initialPartitionArray, true);
	}

	private int[] exportPartition(IPartition partition){
		int[] partitionArray = new int[idToSpecies.length];

		for(int i=0;i<partitionArray.length;i++){
			IBlock block = partition.getBlockOf(idToSpecies[i]);
			ISpecies rep = block.getRepresentative(true);
			partitionArray[i]=rep.getID()+1;
		}

		return partitionArray;
	}
}
