package it.imt.erode.commandline;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.eclipse.ui.console.MessageConsoleStream;

import com.microsoft.z3.Z3Exception;

import it.imt.erode.auxiliarydatastructures.IPartitionAndBoolean;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.AXB;
import it.imt.erode.partitionrefinement.algorithms.CRNBisimulationsNAry;
import py4j.GatewayServer;

public class EntryPointPythonHeadless {

	private CRNReducerCommandLine erode;
	//private SimplePolicyIterationUpperDiagonal metricsComputer;
	private MessageConsoleStream out = null;
	private BufferedWriter bwOut=null;
	private boolean printPartitions;
	private boolean printModels;
	private LinkedHashMap<String, ISpecies> nameToSpecies;
	private ISpecies[] idToSpecies;
	private String[] idToSpeciesNames;
	private boolean modelLoaded=false;
	private IPartition defaultPartition;
	private IPartition userDef;
	private AXB latest_AxB;
	private double[] latest_p0;
	
	public static void main(String[] args) {
		EntryPointPythonHeadless entry = new EntryPointPythonHeadless(false, false);
		
		
		
		if(args.length >0 ) {
			int port=-1;
			if(args!=null && args.length>0) {
				port = Integer.valueOf(args[0]);
			}
			GatewayServer gatewayServer = null;
			if(port==-1){
				gatewayServer=new GatewayServer(entry);
			}
			else{
				gatewayServer=new GatewayServer(entry,port);
			}
					
			gatewayServer.start();
			
			CRNReducerCommandLine.println(entry.out,entry.bwOut,"Gateway server started on port "+gatewayServer.getPort()+ " (while pythonPort is "+gatewayServer.getPythonPort()+", and pythonAddress is "+gatewayServer.getPythonAddress().toString()+")");
			
		}
		else {
			CRNReducerCommandLine.println(entry.out,entry.bwOut,"No arguments/input provided. ERODE terminates.");

			
			//entry.loadModel("/Users/andrea/Library/CloudStorage/OneDrive-ScuolaSuperioreSant'Anna/runtimes/runtime-ERODE.product(9)/bugImportPartition/test_graph_2.ode");
			//entry.loadModel("/Users/andrea/Documents/erode-python/test_graph_2.ode");
			entry.importAffine("/Users/andrea/Documents/erode-python/test2_pert2.csv","/Users/andrea/Documents/erode-python/test2B.csv",true);
			
//			try {
//				int[] a = entry.getUserDefinedPartition();
//				System.out.println(a);
//			} catch (Z3Exception | UnsupportedFormatException | IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			
			try {
				//String[] paramsToPerturb= new String[0];
				String pToPert= "NONE";//p0;p1
				int[] ret = entry.approxBE(1.0,pToPert);
				if(entry.hasAxB()) {
					double[][] A = entry.getLatestAxB_A();
					double[] B = entry.getLatestAxB_B();
					double[] P0= entry.getLatestAxB_p0();
					System.out.println(Arrays.toString(ret));
					System.out.println(Arrays.toString(A));
					System.out.println(Arrays.toString(B));
					System.out.println(Arrays.toString(P0));
				}
				
			} catch (UnsupportedFormatException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
//			boolean upperDiag=false;
//			double time=0;
//			if(upperDiag)
//				CRNReducerCommandLine.println(null,null,"\nConsidering half pairs");
//			else
//				CRNReducerCommandLine.println(null,null,"\nConsidering all pairs");
//			entry.setUpperDiagonalOnly(upperDiag);
//			
//			//entry.importAffine("distr/leontief/ITAData.csv","distr/leontief/ITADemand.csv");
//			//entry.importAffine("distr/testMinus.csv","distr/testB.csv");
//			//entry.importPRISMDTMC("distr/prismdtmc/hermann3_simple.tra", "distr/prismdtmc/hermann3_simple.lab");
//			//entry.importPRISMDTMC("distr/prismdtmc/hermann3.tra", "distr/prismdtmc/hermann3.lab");
//			//entry.importPRISMDTMC("distr/prismdtmc/hermann5.tra", "distr/prismdtmc/hermann5.lab");
//			//entry.importPRISMDTMC("distr/prismdtmc/haddad-monmege.v1/haddad-monmege.v1n5p0.6.tra", "distr/prismdtmc/haddad-monmege.v1/haddad-monmege.v1n5p0.6.lab");
//			entry.importAffine("distr/ambassador_1985_1989A.csv","distr/fakeB.csv");
//			
//			
//			//entry.loadModel("distr/test0.csv");
//			//entry.loadModel("distr/test.csv");
//			//entry.loadModel("distr/test2.csv");
//			//entry.importAffine("distr/test2.csv","distr/test2B.csv");
//			//entry.loadModel("distr/testG.csv");
//			try {
//				//int[] obtained=entry.getDefaultPartition();
//				int[] obtained = entry.computeBB();
//				System.out.println("The partition\n"+Arrays.toString(obtained));
//				
//				//double[] lambdas= {1,2,3};
//				double[] lambdas= {1,/*1,1,1,1,1,1,1,1,1*/};
//				//double[] lambdas= {546053};
//				//double[] lambdas= {1.5,546053};
//				//double[] lambdas= {1};
//				//double[] lambdas= {1.1};
//				for (double lambda : lambdas) {
//					CRNReducerCommandLine.println(null,null,"\nComputing matrix/metrics for lambda "+lambda);
//					long begin = System.currentTimeMillis();
//					double[][] matrix = entry.computeMetrics(obtained, lambda);
//					long end = System.currentTimeMillis();
//					CRNReducerCommandLine.println(null,null,"\nThe obtained matrix/metrics for lambda "+lambda);
//					CRNReducerCommandLine.println(null,null,Arrays.deepToString(matrix));
//					CRNReducerCommandLine.println(null,null,"Milliseconds:"+(end-begin)+"\n");
//					time+=(end-begin);
//					CRNReducerCommandLine.println(null,null,"\n");
//					
////					entry.setUpperDiagonalOnly(true);
////					CRNReducerCommandLine.println(null,null,"\nConsidering half pairs");
////					CRNReducerCommandLine.println(null,null,"\nComputing matrix/metrics for lambda "+lambda);
////					begin = System.currentTimeMillis();
////					matrix = entry.computeMetrics(obtained, lambda);
////					end = System.currentTimeMillis();
////					CRNReducerCommandLine.println(null,null,"\nThe obtained matrix/metrics for lambda "+lambda);
////					CRNReducerCommandLine.println(null,null,Arrays.deepToString(matrix));
////					CRNReducerCommandLine.println(null,null,"Milliseconds:"+(end-begin)+"\n");
////					CRNReducerCommandLine.println(null,null,"\n");
//					
//				}
//				CRNReducerCommandLine.println(null,null,"Average time:"+time/lambdas.length);
//			} catch (Z3Exception | UnsupportedFormatException | IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
		}
	}
	
	public boolean hasAxB() {
		return latest_AxB!=null && latest_AxB.getA()!=null && latest_AxB.getB()!=null;
	}

	private void checkModelLoaded() throws UnsupportedOperationException {
		if(!modelLoaded) {
			CRNReducerCommandLine.println(out,bwOut,"Please first load a model.");
			throw new UnsupportedOperationException("Please first load a model.");
		}
	}
	
	public EntryPointPythonHeadless(boolean printPartitions, boolean printModels) {
		this.printPartitions=printPartitions;
		this.printModels=printModels;
		erode=new CRNReducerCommandLine(new CommandsReader(new ArrayList<String>(0),out,bwOut));
		
		CRNReducerCommandLine.println(out,bwOut,"ERODE instantiated");
	}
	
	public void setScaleAndSpecificTolerance(int scale, double tolerance) {
		CRNBisimulationsNAry.setSCALEandSpecificTolerance(scale, tolerance);
	}
	public void setScaleAndTolerance(int scale) {
		CRNBisimulationsNAry.setSCALE(scale);
	}
	
	public String getModelString() {
		checkModelLoaded();
		return erode.getCRN().toString();
	}
	public String getModelName() {
		checkModelLoaded();
		return erode.getCRN().getName();
	}
	public String[] getVariables() {
		String[] vars=new String[erode.getCRN().getSpecies().size()];
		int i=0;
		for(ISpecies sp : erode.getCRN().getSpecies()) {
			vars[i]=sp.getName();
			i++;
		}
		return vars;
	}
	public String[] getParameters() {
		String[] params=new String[erode.getCRN().getParameters().size()];
		int i=0;
		for(String param : erode.getCRN().getParameters()) {
			int space = param.indexOf(' ');
			String parName=param.substring(0,space).trim();
			params[i]=parName;
			i++;
		}
		return params;
	}
	
	public int getNumberOfBlocks(int[] partitionArray){
		int nBlocks=0;
		HashSet<Integer> blockIds=new LinkedHashSet<Integer>();
		for(int i : partitionArray) {
			if(!blockIds.contains(i)) {
				nBlocks++;
				blockIds.add(i);
			}
		}
		return nBlocks;
	}
	
	public void printPartition(int[] partitionArray){
		CRNReducerCommandLine.println(out,bwOut,getPartitionString(partitionArray));
	}
	public String getPartitionString(int[] partitionArray){
		checkModelLoaded();
		boolean numbersAreIDOfRepresentativeSpecies=false;
		IPartition partition = EntryPointForPython.importPartition(idToSpecies, partitionArray,numbersAreIDOfRepresentativeSpecies);
		return partition.toString();
	}
	
	public void populateAuxiliarySpeciesDataStructures(ICRN crn) {
		nameToSpecies=new LinkedHashMap<>(crn.getSpecies().size());
		idToSpecies=new ISpecies[crn.getSpecies().size()];
		idToSpeciesNames=new String[crn.getSpecies().size()];
		int i=0;
		for(ISpecies sp : crn.getSpecies()) {
			nameToSpecies.put(sp.getName(), sp);
			idToSpecies[i]=sp;
			idToSpeciesNames[i]=sp.getName();
			i++;
		}
	}
	
	private int completeImporting() {
		modelLoaded=true;
		CRNReducerCommandLine.println(out,bwOut);
		populateAuxiliarySpeciesDataStructures(erode.getCRN());
		defaultPartition=erode.getPartition();
		
//		idToSpecies=new ISpecies[erode.getCRN().getSpecies().size()];
//		idToSpeciesNames=new String[idToSpecies.length];
//		int i=0;
//		for (ISpecies species : erode.getCRN().getSpecies()) {
//			idToSpecies[i]=species;
//			idToSpeciesNames[i]=species.getName();
//			i++;
//		}

		/*for(i=0;i<idToSpecies.length;i++){
			ISpecies species = idToSpecies[i];
			species.setInitialConcentration(BigDecimal.ONE, "1.0");
		}*/

		if(printModels){
			CRNReducerCommandLine.println(out,bwOut,erode.getCRN());
		}
		
		return erode.getCRN().getSpecies().size();
	}
	
	
	
	public int[] getDefaultPartition() {
		int[] partitionToExport = EntryPointForPython.exportPartition(idToSpecies,defaultPartition);
		return partitionToExport;
	}
	
	public int loadModel(String fileName){
		int ret=-1;
		if(fileName.endsWith(".csv")) {
			importAffine(fileName);
		}
		if(fileName.endsWith(".tra")) {
			importPRISMDTMC(fileName, fileName.replace(".tra", ".lab"));
		}
		if(fileName.endsWith(".ode")){
			ret= importERODE(fileName);
		}
		else if(fileName.endsWith(".net")){
			ret= importBNG(fileName);
		}
		else {
			throw new UnsupportedOperationException("Model not supported");
		}
		
		return ret;
	}
	
	private int importERODE(String fileName){
		erode.handleLoadCommand("load({fileIn=>"+fileName+"})",false,out,bwOut);
		return completeImporting();
	}
	
	private int importBNG(String fileName){
		erode.handleImportBNGCommand("importBNG({fileIn=>"+fileName+"})",out,bwOut);
		return completeImporting();
	}
	
	public int importAffine(String fileName){
		String bFile = fileName.replace(".csv", "B.csv");
		return importAffine(fileName, bFile,false);
	}
	public int importAffine(String fileName, String bFile,boolean makeEachEntryAParameter){
		String command="importAffineSystem({fileIn=>"+fileName+",bFile=>"+bFile+",icFile=>"+bFile+",addReverseEdges=>false,createParams=>"+makeEachEntryAParameter+"})";
		erode.handleImportAffineSystem(command, out, bwOut);
		//erode.handleImportBNGCommand("importBNG({fileIn=>"+fileName+"})",out,bwOut);
		int ret=completeImporting();
		return ret;
	}
	public int importPRISMDTMC(String fileName, String fileLabels) {
		//importMRMC({fileIn=>prismdtmc/hermann3.tra,labellingFile=>prismdtmc/hermann3.lab,asMatrix=>true})
		String command="importMRMC({fileIn=>"+fileName+",labellingFile=>"+fileLabels+",asMatrix=>true})";
		erode.handleImportMRMCCommand(command, out, bwOut);
		int ret= completeImporting();
		return ret;
	}
	
//	private int importBNG(String fileName){
//		erode.handleImportBNGCommand("importBNG({fileIn=>"+fileName+"})",out,bwOut);
//		return completeImporting();
//	}
//	private int importERODE(String fileName){
//		erode.handleLoadCommand("load({fileIn=>"+fileName+"})",false,out,bwOut);
//		return completeImporting();
//	}
	
//	private int completeImporting() {
//		CRNReducerCommandLine.println(out,bwOut);
//		idToSpecies=new ISpecies[erode.getCRN().getSpecies().size()];
//		idToSpeciesNames=new String[idToSpecies.length];
//		int i=0;
//		for (ISpecies species : erode.getCRN().getSpecies()) {
//			idToSpecies[i]=species;
//			idToSpeciesNames[i]=species.getName();
//			i++;
//		}
//
//		/*for(i=0;i<idToSpecies.length;i++){
//			ISpecies species = idToSpecies[i];
//			species.setInitialConcentration(BigDecimal.ONE, "1.0");
//		}*/
//
//		if(printModels){
//			CRNReducerCommandLine.println(out,bwOut,erode.getCRN());
//		}
//
//		return erode.getCRN().getSpecies().size();
//	}
	
	public int[] computeBE() throws UnsupportedFormatException, Z3Exception, IOException{
		int[] initialPartitionArray = new int[idToSpecies.length];
		Arrays.fill(initialPartitionArray, 1);
		return computeBE(initialPartitionArray,false);
	}
	
	public int[] computeBE(int[] initialPartitionArray) throws UnsupportedFormatException, Z3Exception, IOException{
		return computeBE(initialPartitionArray,false);
	}
	
	public int[] computeBE(int[] initialPartitionArray, boolean numbersAreIDOfRepresentativeSpecies) throws UnsupportedFormatException, Z3Exception, IOException{
		checkModelLoaded();
		CRNReducerCommandLine.println(out,bwOut,"Computing BE reduction");
		
		IPartition initialPartition = EntryPointForPython.importPartition(idToSpecies, initialPartitionArray,numbersAreIDOfRepresentativeSpecies);
		erode.setPartition(initialPartition);
		if(printPartitions){
			CRNReducerCommandLine.println(out,bwOut,"Initial partition:\n"+initialPartition);
		}
		
		


		IPartitionAndBoolean obtainedPartitionAndBool = erode.handleReduceCommand("reduceBE({computeOnlyPartition=>true,print=>false})",false,"be",out,bwOut);
		IPartition obtainedPartition = obtainedPartitionAndBool.getPartition();
		int[] obtainedPartitionToExport=null;
		try {
		//IPartition obtainedPartition = crnreducer.handleReduceCommand("reduceEFL({computeOnlyPartition=>true,print=>false})",false,"EFL");
		if(printPartitions){
			CRNReducerCommandLine.println(out,bwOut,"Obtained partition:\n"+obtainedPartition);
		}
	
		obtainedPartitionToExport = EntryPointForPython.exportPartition(idToSpecies,obtainedPartition);
		CRNReducerCommandLine.println(out,bwOut,"BE reduction completed");
		}finally {
			erode.setPartition(defaultPartition);
		}
		return obtainedPartitionToExport;
	}
	
	public int[] approxBE(Double eps,String paramsToPerturb) throws UnsupportedFormatException, IOException{
		int[] initialPartitionArray = new int[idToSpecies.length];
		Arrays.fill(initialPartitionArray, 1);
		return approxBE(eps,initialPartitionArray,false,paramsToPerturb);
	}
	
	public int[] approxBE(Double eps,int[] initialPartitionArray,String paramsToPerturb) throws UnsupportedFormatException, IOException{
		return approxBE(eps,initialPartitionArray,false,paramsToPerturb);
	}
	public int[] approxBE(Double eps,int[] initialPartitionArray, boolean numbersAreIDOfRepresentativeSpecies,String paramsToPerturb) throws UnsupportedFormatException, Z3Exception, IOException{
		this.latest_AxB=null;
		this.latest_p0=null;
		
		checkModelLoaded();
		CRNReducerCommandLine.println(out,bwOut,"Computing eps-BE reduction");
		
		IPartition initialPartition = EntryPointForPython.importPartition(idToSpecies, initialPartitionArray,numbersAreIDOfRepresentativeSpecies);
		erode.setPartition(initialPartition);
		if(printPartitions){
			CRNReducerCommandLine.println(out,bwOut,"Initial partition:\n"+initialPartition);
		}
		
		


		//handleApproximationCommand(command, "backward", out, bwOut);
//		String pToPert="NONE";
//		if(paramsToPerturb!=null&& paramsToPerturb.length>0) {
//			//paramsToPerturb=>
//			pToPert="";
//			for(int p=0;p<paramsToPerturb.length;p++) {
//				pToPert+=paramsToPerturb[p];
//				pToPert+=";";
//			}
//		}
		IPartitionAndBooleanAndAxB obtainedPartitionAndBoolAndAxB = erode.handleApproximationCommand("approxBDE({computeOnlyPartition=>true,print=>false,fastDegreeOne=>false,epsilon=>"+eps+",paramsToPerturb=>"+paramsToPerturb+"})","backward",out,bwOut);
		IPartition obtainedPartition = obtainedPartitionAndBoolAndAxB.getPartition();
		int[] obtainedPartitionToExport=null;
		try {
		//IPartition obtainedPartition = crnreducer.handleReduceCommand("reduceEFL({computeOnlyPartition=>true,print=>false})",false,"EFL");
		if(printPartitions){
			CRNReducerCommandLine.println(out,bwOut,"Obtained partition:\n"+obtainedPartition);
		}
	
		obtainedPartitionToExport = EntryPointForPython.exportPartition(idToSpecies,obtainedPartition);
		CRNReducerCommandLine.println(out,bwOut,eps+"-BE reduction completed");
		}finally {
			erode.setPartition(defaultPartition);
		}
		
		this.latest_AxB=obtainedPartitionAndBoolAndAxB.getAxb();
		this.latest_p0=obtainedPartitionAndBoolAndAxB.getP0();
		
		return obtainedPartitionToExport;
	}
	
	public boolean hasLatestAxB() {
		return latest_AxB!=null && latest_AxB.getA()!=null && latest_AxB.getB()!=null && latest_p0!=null;
	}
	
	
	public double[][] getLatestAxB_A() {
		if(latest_AxB==null) {
			return null;
		}
		return latest_AxB.getA();
	}
	public double[] getLatestAxB_B() {
		if(latest_AxB==null) {
			return null;
		}
		return latest_AxB.getB();
	}
	public double[] getLatestAxB_p0() {
		if(latest_AxB==null||latest_p0==null) {
			return null;
		}
		return latest_p0;
	}
	
	public int[] computeFE() throws UnsupportedFormatException, Z3Exception, IOException{
		int[] initialPartitionArray = new int[idToSpecies.length];
		Arrays.fill(initialPartitionArray, 1);
		return computeFE(initialPartitionArray,false);
	}
	
	public int[] computeFE(int[] initialPartitionArray) throws UnsupportedFormatException, Z3Exception, IOException{
		return computeFE(initialPartitionArray,false);
	}
	
	public int[] computeFE(int[] initialPartitionArray, boolean numbersAreIDOfRepresentativeSpecies) throws UnsupportedFormatException, Z3Exception, IOException{
		checkModelLoaded();
		CRNReducerCommandLine.println(out,bwOut,"Computing FE reduction");
		
		IPartition initialPartition = EntryPointForPython.importPartition(idToSpecies, initialPartitionArray,numbersAreIDOfRepresentativeSpecies);
		erode.setPartition(initialPartition);
		if(printPartitions){
			CRNReducerCommandLine.println(out,bwOut,"Initial partition:\n"+initialPartition);
		}
		
		


		IPartitionAndBoolean obtainedPartitionAndBool = erode.handleReduceCommand("reduceFE({computeOnlyPartition=>true,print=>false})",false,"be",out,bwOut);
		IPartition obtainedPartition = obtainedPartitionAndBool.getPartition();
		int[] obtainedPartitionToExport=null;
		try {
		//IPartition obtainedPartition = crnreducer.handleReduceCommand("reduceEFL({computeOnlyPartition=>true,print=>false})",false,"EFL");
		if(printPartitions){
			CRNReducerCommandLine.println(out,bwOut,"Obtained partition:\n"+obtainedPartition);
		}
	
		obtainedPartitionToExport = EntryPointForPython.exportPartition(idToSpecies,obtainedPartition);
		CRNReducerCommandLine.println(out,bwOut,"FE reduction completed");
		}finally {
			erode.setPartition(defaultPartition);
		}
		return obtainedPartitionToExport;
	}
	
	public int getVariablesNum() {
		return erode.getCRN().getSpeciesSize();
	}
	
	public int[] getUserDefinedPartition() throws UnsupportedFormatException, Z3Exception, IOException{
		
		if(userDef==null) {
			userDef = CRNBisimulationsNAry.prepartitionUserDefined(erode.getCRN(),false,null,null,new Terminator());
		}
		
		int[] userDefArray = EntryPointForPython.exportPartition(idToSpecies,userDef);
		return userDefArray;
	}
	
	public void simulateODE(double tEnd, int steps, String csvFile) {
		//simulateODE({tEnd=>1000.0,steps=>100,visualizePlot=>NO,library=>APACHE,csvFile=>/Users/andrea/OneDrive - Danmarks Tekniske Universitet/runtimes/runtime-ERODE.product(4)/isabel/epsislon/csvFile})
		try {
			erode.handleSimulateODECommand("simulateODE({tEnd=>"+tEnd+",steps=>"+steps+",visualizePlot=>NO,library=>APACHE,csvFile=>"+csvFile+"})", out, bwOut);
		} catch (IOException e) {
			e.printStackTrace();
		}
		//return crnreducer.simulateODE(tEnd, stepSize, steps, null, csvFile, visualizeVars, visualizeViews, minStep,maxStep,absTol,relTol,false, showLabels,out,bwOut,caption,false,BigDecimal.ZERO,print,apache);
	}
	
}
