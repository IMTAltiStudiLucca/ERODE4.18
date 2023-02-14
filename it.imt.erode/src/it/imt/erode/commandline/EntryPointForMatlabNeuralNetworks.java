package it.imt.erode.commandline;

import java.io.IOException;
import java.math.BigDecimal;
//import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.microsoft.z3.Z3Exception;

import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.importing.UnsupportedFormatException;


public class EntryPointForMatlabNeuralNetworks extends EntryPointForMatlabAbstract {

	private HashMap<String, ISpecies> speciesNameToSpecies;
	//private SimulationSolutions solutions;
	public EntryPointForMatlabNeuralNetworks(boolean printPartitions, boolean printCRNs){
		super(printPartitions, printCRNs,false);
		CRNReducerCommandLine.println(out,bwOut,"ERODE instantiated");
	}
	
	@Override
	public int load(String fileName) {
		int ret= super.load(fileName);
		
		List<ISpecies> allSpecies = erode.getCRN().getSpecies();
		speciesNameToSpecies = new HashMap<String, ISpecies>(allSpecies.size());
		for (ISpecies iSpecies : allSpecies) {
			speciesNameToSpecies.put(iSpecies.getName(), iSpecies);
		}
		
		return ret;
	}

	public static void main(String[] args) throws UnsupportedFormatException, Z3Exception, IOException {
		//CRNReducerCommandLine.println("BBB");
		//CRNReducerCommandLine.println("BBB");
		EntryPointForMatlabNeuralNetworks entry = new EntryPointForMatlabNeuralNetworks(true,true);
		//entry.importBNG("./BNGNetworks/Mre.net");
		//entry.importBNG("./BNGNetworks/max.net");
		//entry.computeBB(new int[]{2,2,3,3,3});
		//entry.computeBB(new int[]{3,3,3,3,3});
		//crnreducer = new CRNReducerCommandLine(new CommandsReader(new ArrayList<String>(0)));
		//CRNReducerCommandLine.println("CRNReducer instantiated");
		
		
		int[] pippo = new int[4];
		pippo[0]=2;
		pippo[1]=20;
		pippo[2]=5;
		pippo[3]=100;
		
		
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
		
		//entry.loadCRN("./CRNNetworks/ccr_am.crn");
		//@SuppressWarnings("unused")
		//int[] refinement = entry.computeBB(new int[]{11, 10, 12, 10, 11, 12, 10, 11, 12, 10, 11, 12});
		
//		entry.loadCRN("XYNetwork2_0.ode");
//		int[] refinement = entry.computeBB();
//		entry.computeJacobian();
		//System.out.println(refinement);
		
//		entry.loadCRN("XYNetwork2_0_simpleNetwork.ode");
//		//int[] refinement = entry.computeBB();
//		entry.computeJacobian();
		
//		entry.loadCRN("ERBB2.ode");
//		int[] initialPartitionArray = new int[entry.getSpeciesNames().length];
//		Arrays.fill(initialPartitionArray, 1);
//		initialPartitionArray[13]=14;//14 is id+1
//		initialPartitionArray[20]=21;//21 is id+1
//		entry.computeBB(initialPartitionArray);
//		
//		double[] paramValues=new double[2];
//		paramValues[0]=10;
//		paramValues[1]=10;
//		String[] paramNames=new String[2];
//		paramNames[0]="p1";
//		paramNames[1]="p2";
//		//entry.setParam(paramValues, paramNames);
//		
//		double[][] sols;
//		
//		entry.loadCRN("abalone2_rn.ode");
//		sols = entry.simulateODE(50);
//		
//		
//		
//		entry.loadCRN("abalone_rn.ode");
//		sols = entry.simulateODE(50);
//		
//		String[] icNames=new String[8];
//		for(int i=0;i<icNames.length;i++){
//			icNames[i]="x"+(i+1);
//		}
//		double[] icValues=new double[8];
//		for(int i=0;i<icValues.length;i++){
//			icValues[i]=1;
//		}
//		
//		entry.setIC(icValues,icNames);
//		
//		sols = entry.simulateODE(3);
//		for(int i=0;i<icValues.length;i++){
//			icValues[i]=2;
//		}
//		entry.setIC(icValues,icNames);
//		sols=entry.simulateODE(3);
//		System.out.println("done");
		
		entry.load("simplefit_crn.ode");
		entry.computeBE(true);
		entry.computeEpsBE(0.1, true);
		
	}
	
	//x1 = 0.000000 x2 = -0.310811 x3 = -0.327731 x4 = -0.858407 x5 = -0.856207 x6 = -0.880968 x7 = -0.897301 x8 = -0.893373
	public boolean setIC(double[] icValues, String[] speciesNames) throws UnsupportedFormatException, Z3Exception, IOException{
		boolean ret=true;
		for(int p = 0;p<speciesNames.length;p++){
			String icExpr = String.valueOf(icValues[p]);
			BigDecimal ic = BigDecimal.valueOf(icValues[p]);
			ISpecies species=speciesNameToSpecies.get(speciesNames[p]);
			if(species!=null){
				species.setInitialConcentration(ic,icExpr);
			}
			else{
				ret=false;
			}
		}	
		return ret;
	}
	/*public boolean setIC(double[] icValues){
		for(int p = 0;p<icValues.length;p++){
			String icExpr = String.valueOf(icValues[p]);
			BigDecimal ic = BigDecimal.valueOf(icValues[p]);
			ISpecies species=speciesNameToSpecies.get("x"+(p+1));
			species.setInitialConcentration(ic,icExpr);
		}			
		return true;
	}*/
//	public void setIC(int[] icValues){
//		for(int p = 0;p<icValues.length;p++){
//			String icExpr = String.valueOf(icValues[p]);
//			BigDecimal ic = BigDecimal.valueOf(icValues[p]);
//			ISpecies species=speciesNameToSpecies.get("x"+(p+1));
//			species.setInitialConcentration(ic,icExpr);
//		}			
//	}
	
	public void setParam(double[] paramValues, String[] paramNames) throws UnsupportedFormatException, Z3Exception, IOException{
		ICRN crn = erode.getCRN();
		for(int p = 0;p<paramNames.length;p++){
			crn.setParameter(paramNames[p], String.valueOf(paramValues[p]));
		}			
	}
	
//	public double[][] simulateODE(double timeHorizon) throws IOException {
//
//		solutions=null;
//		String command = "simulateODE({tEnd=>"+timeHorizon+",visualizePlot=>NO,steps=>10})";
//		
//		solutions = erode.handleSimulateODECommand(command, out, bwOut,false);
//		
//		//System.out.println("Simulation done");
//		
//		//plotsAll[species][step]
//		return solutions.getPlotsViews();
//	}
	
}
