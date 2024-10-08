package it.imt.erode.commandline;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.ui.console.MessageConsoleStream;

import com.microsoft.z3.Z3Exception;

import it.imt.erode.auxiliarydatastructures.IPartitionAndBoolean;
import it.imt.erode.auxiliarydatastructures.Reduction;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.importing.MatlabODEsImporter;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.AXB;
import it.imt.erode.partitionrefinement.algorithms.CRNBisimulationsNAry;
import it.imt.erode.partitionrefinement.algorithms.EpsilonDifferentialEquivalences;

public abstract class EntryPointForMatlabAbstract {

	//private List<String> commands = new ArrayList<String>();
	protected CRNReducerCommandLine erode;
	//protected  ISpecies[] idToSpecies; 
	protected  boolean printPartitions;
	protected  boolean printCRNs;
	protected  MessageConsoleStream out = null;
	protected  BufferedWriter bwOut=null;
	//protected  String[] idToSpeciesNames;
	private boolean fastDegreeOneBE = false;

	protected EpsilonDifferentialEquivalences epsilonDE;
	
	public EntryPointForMatlabAbstract(boolean printPartitions, boolean printCRNs,boolean fastDegreeOneBE){
		erode = new CRNReducerCommandLine(new CommandsReader(new ArrayList<String>(0),out,bwOut));
		this.printPartitions=printPartitions;
		this.printCRNs=printCRNs;
		this.fastDegreeOneBE=fastDegreeOneBE;
	}
	
	public EntryPointForMatlabAbstract(boolean printPartitions, boolean printCRNs, ICRN crn,boolean fastDegreeOneBE){
		this(printPartitions,printCRNs,fastDegreeOneBE);
		erode.setCRN(crn);
	}

	public int load(String fileName){
		if(fileName.endsWith(".crn")){
			return importCRN(fileName);
		}
		else if(fileName.endsWith(".net")){
			return importBNG(fileName);
		}
		else if(fileName.endsWith(".ode")){
			return importCRN(fileName);
		}
		else if(fileName.endsWith("._ode")){
			return importCRN(fileName);
		}
		else if(fileName.endsWith(".csv")){
			return importCCSV(fileName);
		}
		
		throw new UnsupportedOperationException("Either .crn, .ode, .net or .csv formats are supported");
		
	}
	
	//importAffineSystem( fileIn="uudg2A.csv" , BFile="uudg2B.csv" , ICFile= "uudg2IC.csv")
		public int importAffineSystem(String fileIn, String BFile, String ICFIle){
			String command="importAffineSystem(({fileIn=>"+fileIn+",bFile=>"+BFile+",icFile=>"+ICFIle+"})";
			erode.handleImportAffineSystem(command, out, bwOut);
			return completeImporting();
		}
	
	public int importCCSV(String fileName) {
		String command="importLinearSystemAsCCSVMatrix({fileIn=>"+fileName+",form=>AX})";
		erode.handleImportLinearSystemAsCSVMatrixCommand(command,true,out,bwOut);
		return completeImporting();
	}

	private int importBNG(String fileName){
		erode.handleImportBNGCommand("importBNG({fileIn=>"+fileName+"})",out,bwOut);
		return completeImporting();
	}
	
	private int importCRN(String fileName){
		erode.handleLoadCommand("load({fileIn=>"+fileName+"})",false,out,bwOut);
		return completeImporting();
	}

	private int completeImporting() {
		CRNReducerCommandLine.println(out,bwOut);
		/*
		idToSpecies=new ISpecies[erode.getCRN().getSpecies().size()];
		//idToSpeciesNames=new String[idToSpecies.length];
		int i=0;
		for (ISpecies species : erode.getCRN().getSpecies()) {
			idToSpecies[i]=species;
			idToSpeciesNames[i]=species.getName();
			i++;
		}
		*/

		/*for(i=0;i<idToSpecies.length;i++){
			ISpecies species = idToSpecies[i];
			species.setInitialConcentration(BigDecimal.ONE, "1.0");
		}*/

		if(printCRNs){
			CRNReducerCommandLine.println(out,bwOut,erode.getCRN());
		}

		return erode.getCRN().getSpecies().size();
	}

	public int[] computeBE() throws UnsupportedFormatException, Z3Exception, IOException{
		int[] initialPartitionArray = new int[erode.getCRN().getSpecies().size()];
		Arrays.fill(initialPartitionArray, 1);
		return computeBE(initialPartitionArray);
	}
	
	
	
	public int[] computeBE(int[] initialPartitionArray, boolean numbersAreIDOfRepresentativeSpecies) throws UnsupportedFormatException, Z3Exception, IOException{
//		CRNReducerCommandLine cl = new CRNReducerCommandLine(null);
//		//cl.checkLibraries(out, bwOut);
//		if(CRNReducerCommandLine.HASTOCHECKLIBRARIES){
//			if(!CRNReducerCommandLine.getLibrariesPresent()){
//				cl.checkLibraries(out,bwOut);
//			}
//			/*if(!CRNReducerCommandLine.getLibrariesPresent()){
//				return null;
//			}*/
//		}
		
		IPartition initialPartition = importPartition(initialPartitionArray,numbersAreIDOfRepresentativeSpecies);
		erode.setPartition(initialPartition);
		if(printPartitions){
			CRNReducerCommandLine.println(out,bwOut,"Initial partition:\n"+initialPartition);
		}


		return performActualBE();
		
		
	}
	
	public int[] computeBE(boolean userPrep) throws UnsupportedFormatException, Z3Exception, IOException{
		//CRNReducerCommandLine.println(out,bwOut,"USER PREP="+userPrep);
		IPartition initialPartition = erode.getPartition();
		
		if(userPrep){
			initialPartition= CRNBisimulationsNAry.prepartitionUserDefined(erode.getCRN(), true, out,bwOut,new Terminator());
			erode.setPartition(initialPartition);
		}
		
		
		erode.setPartition(initialPartition);
		if(printPartitions){
			CRNReducerCommandLine.println(out,bwOut,"Initial partition:\n"+initialPartition);
		}


		return performActualBE();
		
		
	}
	
	public int[] computeEpsFE(double epsilon,int[] initialPartitionArray) throws UnsupportedFormatException, Z3Exception, IOException{
		return computeEpsFE(epsilon,initialPartitionArray,true);
	}
	
	public int[] computeEpsFE(double epsilon,int[] initialPartitionArray, boolean numbersAreIDOfRepresentativeSpecies) throws UnsupportedFormatException, Z3Exception, IOException{
		IPartition initialPartition = importPartition(initialPartitionArray,numbersAreIDOfRepresentativeSpecies);
		erode.setPartition(initialPartition);
		if(printPartitions){
			CRNReducerCommandLine.println(out,bwOut,"Initial partition:\n"+initialPartition);
		}

		return computeEpsFE(epsilon, false);
	}
	
	public int[] computeEpsFE(double epsilon, boolean userPrep) throws UnsupportedFormatException, Z3Exception, IOException{
		//CRNReducerCommandLine.println(out,bwOut,"USER PREP="+userPrep);
		//numbersAreIDOfRepresentativeSpecies=false
		
		//CRNBisimulationsNAry.SCALE=scale;
		//CRNBisimulationsNAry.TOLERANCE = new BigDecimal("1E-"+(CRNBisimulationsNAry.SCALE));
		
		//long begin = System.currentTimeMillis();
		
		IPartition initialPartition = erode.getPartition();
		
		if(userPrep){
			initialPartition= CRNBisimulationsNAry.prepartitionUserDefined(erode.getCRN(), true, out,bwOut,new Terminator());
			erode.setPartition(initialPartition);
		}
		
		if(printPartitions){
			CRNReducerCommandLine.println(out,bwOut,"Initial partition:\n"+initialPartition);
		}
				
		//CRNReducerCommandLine.println(out,bwOut,"");
				
		return performActualEpsilonNFB(epsilon);
		
	}
	
	protected int[] performActualEpsilonNFB(double epsilon) throws UnsupportedFormatException, IOException {
		//String command="approximateBDE({matlabScript=>/Users/andrea/Copy/TOOLBiology/DeAR-CRN/erode/runtime-ERODE.product3/DAE/mEpsNBB.m,epsilon=>0.1,paramsToPerturb=>ALL,prePartition=>NO})";
		
		int initialBlocks=erode.getPartition().size();
		
		EpsilonDifferentialEquivalences epsilonDE = new EpsilonDifferentialEquivalences();
		long begin = System.currentTimeMillis();
		IPartitionAndBoolean obtainedPartitionAndBool = epsilonDE.computeCoarsest(Reduction.ENFB, BigDecimal.valueOf(epsilon), erode.getCRN(), erode.getPartition(), false, out,bwOut, new Terminator(),false);
		
		//IPartitionAndBoolean obtainedPartitionAndBool = crnreducer.handleReduceCommand("reduceEpsNBB({computeOnlyPartition=>true,print=>false})",false,"enbb",out,bwOut);
		//IPartition obtainedPartition = crnreducer.handleReduceCommand("reduceBDE({computeOnlyPartition=>true,print=>false})",false,"bde",out,bwOut);
		//IPartition obtainedPartition = crnreducer.handleReduceCommand("reduceEFL({computeOnlyPartition=>true,print=>false})",false,"EFL");
		IPartition obtainedPartition = obtainedPartitionAndBool.getPartition();
		boolean succeeded=obtainedPartitionAndBool.getBool();
		if(succeeded){
			if(printPartitions){
				CRNReducerCommandLine.println(out,bwOut,"Obtained partition:\n"+obtainedPartition);
			}

			int[] obtainedPartitionToExport = exportPartition(obtainedPartition);
			
			long end = System.currentTimeMillis();
			double seconds = (end-begin)/1000.0;
			
			CRNReducerCommandLine.println(out,bwOut,epsilon+"-FDE partitioning completed. From "+ initialBlocks +" to "+ obtainedPartition.size() + " blocks ("+erode.getCRN().getSpecies().size()+" species). Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, (seconds))+ " (s)");
			CRNReducerCommandLine.println(out,bwOut,"");
			
			return obtainedPartitionToExport;
		}
		else{
			CRNReducerCommandLine.println(out,bwOut,epsilon+"-FDE partitioning FAILED.");
			return null;
		}
		
		
		
	}
	
	
	
	
	public int[] computeEpsBE(double epsilon,int[] initialPartitionArray) throws UnsupportedFormatException, Z3Exception, IOException{
		return computeEpsBE(epsilon,initialPartitionArray,true);
	}
	
	public int[] computeEpsBE(double epsilon,int[] initialPartitionArray, boolean numbersAreIDOfRepresentativeSpecies) throws UnsupportedFormatException, Z3Exception, IOException{
		IPartition initialPartition = importPartition(initialPartitionArray,numbersAreIDOfRepresentativeSpecies);
		erode.setPartition(initialPartition);
		if(printPartitions){
			CRNReducerCommandLine.println(out,bwOut,"Initial partition:\n"+initialPartition);
		}

		return computeEpsBE(epsilon, false);
	}
	
	public int[] computeEpsBE(double epsilon, boolean userPrep) throws UnsupportedFormatException, Z3Exception, IOException{
		//CRNReducerCommandLine.println(out,bwOut,"USER PREP="+userPrep);
		//numbersAreIDOfRepresentativeSpecies=false
		
		//CRNBisimulationsNAry.SCALE=scale;
		//CRNBisimulationsNAry.TOLERANCE = new BigDecimal("1E-"+(CRNBisimulationsNAry.SCALE));
		
		//long begin = System.currentTimeMillis();
		
		IPartition initialPartition = erode.getPartition();
		
		if(userPrep){
			initialPartition= CRNBisimulationsNAry.prepartitionUserDefined(erode.getCRN(), true, out,bwOut,new Terminator());
			erode.setPartition(initialPartition);
		}
		
		if(printPartitions){
			CRNReducerCommandLine.println(out,bwOut,"Initial partition:\n"+initialPartition);
		}
				
		//CRNReducerCommandLine.println(out,bwOut,"");
				
		return performActualEpsilonBE(epsilon);
		
	}
	
	public AXB computeReferenceTrajectory(Reduction red, int[] obtainedPartitionArray, boolean addConstraintsOnIC,boolean solveSystem,LinkedHashSet<String> paramsToPerturb, boolean printM, boolean printSolution) throws UnsupportedFormatException {
		//EpsilonDifferentialEquivalences epsilonDE = new EpsilonDifferentialEquivalences();
		long begin = System.currentTimeMillis();
		
		CRNReducerCommandLine.println(out,bwOut,"Computing the linear system of constraints relative to the latest eps-DE performed ... ");
		
		boolean numbersAreIDOfRepresentativeSpecies=false;
		IPartition obtainedPartition = importPartition(obtainedPartitionArray,numbersAreIDOfRepresentativeSpecies);
		
		AXB axb = epsilonDE.computeReferenceTrajectory(red,erode.getCRN(), obtainedPartition,out,bwOut,solveSystem,paramsToPerturb,new Terminator(),printM,printSolution);
		if(addConstraintsOnIC) {
			AXB axbWithICConstraints=MatlabODEsImporter.expandWithICConstraints(getCRN(), obtainedPartition, axb);
			axb=axbWithICConstraints;
		}
		
		long end = System.currentTimeMillis();
		double seconds = (end-begin)/1000.0;
		CRNReducerCommandLine.println(out,bwOut,"Completed. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, (seconds))+ " (s)");
		CRNReducerCommandLine.println(out,bwOut,"");
		return axb;
	}

	protected int[] performActualEpsilonBE(double epsilon) throws UnsupportedFormatException, IOException {
		//String command="approximateBDE({matlabScript=>/Users/andrea/Copy/TOOLBiology/DeAR-CRN/erode/runtime-ERODE.product3/DAE/mEpsNBB.m,epsilon=>0.1,paramsToPerturb=>ALL,prePartition=>NO})";
		
		int initialBlocks=erode.getPartition().size();
		
		CRNReducerCommandLine.print(out,bwOut,"Computing the required "+epsilon+"-DE ... ");
		
		epsilonDE = new EpsilonDifferentialEquivalences();
		long begin = System.currentTimeMillis();
		
		IPartitionAndBoolean obtainedPartitionAndBool;
		if(fastDegreeOneBE) {
			obtainedPartitionAndBool =CRNBisimulationsNAry.computeCoarsest(Reduction.BE,erode.getCRN(),getCRN().getReactions(), erode.getPartition(), false,out,bwOut,new Terminator(),null,BigDecimal.valueOf(epsilon));
		}
		else {
			obtainedPartitionAndBool = epsilonDE.computeCoarsest(Reduction.ENBB, BigDecimal.valueOf(epsilon), erode.getCRN(), erode.getPartition(), false, out,bwOut, new Terminator(),false);
		}
		
		
		
		//IPartitionAndBoolean obtainedPartitionAndBool = crnreducer.handleReduceCommand("reduceEpsNBB({computeOnlyPartition=>true,print=>false})",false,"enbb",out,bwOut);
		//IPartition obtainedPartition = crnreducer.handleReduceCommand("reduceBDE({computeOnlyPartition=>true,print=>false})",false,"bde",out,bwOut);
		//IPartition obtainedPartition = crnreducer.handleReduceCommand("reduceEFL({computeOnlyPartition=>true,print=>false})",false,"EFL");
		IPartition obtainedPartition = obtainedPartitionAndBool.getPartition();
		boolean succeeded=obtainedPartitionAndBool.getBool();
		if(succeeded){
			if(printPartitions){
				CRNReducerCommandLine.println(out,bwOut,"Obtained partition:\n"+obtainedPartition);
			}

			int[] obtainedPartitionToExport = exportPartition(obtainedPartition);
			
			long end = System.currentTimeMillis();
			double seconds = (end-begin)/1000.0;
			
			CRNReducerCommandLine.println(out,bwOut,epsilon+"-BDE partitioning completed. From "+ initialBlocks +" to "+ obtainedPartition.size() + " blocks ("+erode.getCRN().getSpecies().size()+" species). Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, (seconds))+ " (s)");
			//CRNReducerCommandLine.println(out,bwOut,"");
			
			return obtainedPartitionToExport;
		}
		else{
			CRNReducerCommandLine.println(out,bwOut,epsilon+"-BDE partitioning FAILED.");
			return null;
		}
		
		
		
	}
	
	protected int[] performActualBE() throws UnsupportedFormatException, IOException {
		//int initialBlocks = crnreducer.getPartition().size();
		//long begin =System.currentTimeMillis();
		IPartitionAndBoolean obtainedPartitionAndBool = erode.handleReduceCommand("reduceBE({computeOnlyPartition=>true,print=>false})",false,"be",out,bwOut);
		//IPartition obtainedPartition = crnreducer.handleReduceCommand("reduceBDE({computeOnlyPartition=>true,print=>false})",false,"bde",out,bwOut);
		//IPartition obtainedPartition = crnreducer.handleReduceCommand("reduceEFL({computeOnlyPartition=>true,print=>false})",false,"EFL");
		IPartition obtainedPartition = obtainedPartitionAndBool.getPartition();
		boolean succeeded=obtainedPartitionAndBool.getBool();
		if(succeeded){
			if(printPartitions){
				CRNReducerCommandLine.println(out,bwOut,"Obtained partition:\n"+obtainedPartition);
			}
			
			//long end = System.currentTimeMillis();
			//double seconds = (end-begin)/1000.0;
			
			//CRNReducerCommandLine.println(out,bwOut,"BB partitioning completed. From "+ initialBlocks +" to "+ obtainedPartition.size() + " blocks ("+crnreducer.getCRN().getSpecies().size()+" species). Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, (seconds))+ " (s)");
			//CRNReducerCommandLine.println(out,bwOut,"");

			int[] obtainedPartitionToExport = exportPartition(obtainedPartition);
			return obtainedPartitionToExport;
		}
		else{
			return null;
		}
	}
	
	
	public int[] computeBE(int[] initialPartitionArray) throws UnsupportedFormatException, Z3Exception, IOException{
		return computeBE(initialPartitionArray,true);
	}
	
	public int[] computeFE() throws UnsupportedFormatException, Z3Exception, IOException{
		int[] initialPartitionArray = new int[erode.getCRN().getSpecies().size()];
		Arrays.fill(initialPartitionArray, 1);
		return computeFE(initialPartitionArray);
	}
	
	public int[] computeFE(int[] initialPartitionArray) throws UnsupportedFormatException, Z3Exception, IOException{

		IPartition initialPartition = importPartition(initialPartitionArray);
		erode.setPartition(initialPartition);
		if(printPartitions){
			CRNReducerCommandLine.println(out,bwOut,"Initial partition:\n"+initialPartition);
		}


		IPartitionAndBoolean obtainedPartitionAndBool = erode.handleReduceCommand("reduceFE({computeOnlyPartition=>true,print=>false})",false,"fe",out,bwOut);
		//IPartition obtainedPartition = crnreducer.handleReduceCommand("reduceEFL({computeOnlyPartition=>true,print=>false})",false,"EFL");
		
		IPartition obtainedPartition=obtainedPartitionAndBool.getPartition();
		boolean succeeded = obtainedPartitionAndBool.getBool();
		
		if(succeeded){
			if(printPartitions){
				CRNReducerCommandLine.println(out,bwOut,"Obtained partition:\n"+obtainedPartition);
			}

			int[] obtainedPartitionToExport = exportPartition(obtainedPartition);
			return obtainedPartitionToExport;
		}
		else{
			return null;
		}
	}
	
//	public String[] getSpeciesNames(){
//		return idToSpeciesNames;
//	}
	
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
					//block.addSpecies(idToSpecies[repSpeciesId]);
					block.addSpecies(erode.getCRN().getSpecies().get(repSpeciesId));
					initialPartitionHM.put(repSpeciesId, block);
				}
				//block.addSpecies(idToSpecies[speciesId]);
				block.addSpecies(erode.getCRN().getSpecies().get(speciesId));
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
				//block.addSpecies(idToSpecies[speciesId]);
				block.addSpecies(erode.getCRN().getSpecies().get(speciesId));
			}
		}
		initialPartition.setMinAsRepresentative();
		return initialPartition;
	}
	
	protected IPartition importPartition(int[] initialPartitionArray){
		return importPartition(initialPartitionArray, true);
	}

	protected int[] exportPartition(IPartition partition){
		int[] partitionArray = new int[erode.getCRN().getSpecies().size()];

		for(int i=0;i<partitionArray.length;i++){
			//IBlock block = partition.getBlockOf(idToSpecies[i]);
			IBlock block = partition.getBlockOf(erode.getCRN().getSpecies().get(i));
			ISpecies rep = block.getRepresentative(true);
			partitionArray[i]=rep.getID()+1;
		}

		return partitionArray;
	}
	
	public List<ISpecies> getSpecies(){
		return erode.getCRN().getSpecies();
	}
	
	public List<ICRNReaction> getReactions(){
		return erode.getCRN().getReactions();
	}
	
	public ICRN getCRN() {
		return erode.getCRN();
	}
	
	public void setPrecision(int scale) {
		//1E-scale
		CRNBisimulationsNAry.setSCALE(scale);
	}
	
	
	
}
