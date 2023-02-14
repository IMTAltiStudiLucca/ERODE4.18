package it.imt.erode.commandline;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;

import org.eclipse.ui.console.MessageConsoleStream;

import com.microsoft.z3.Z3Exception;

import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.simulation.deterministic.apachecommons.CRNVectorFieldApacheCovariances;
import it.imt.erode.simulation.output.DataOutputHandlerAbstract;
import it.imt.erode.simulation.output.MutableBoolean;

public class EntryPointForMatlab extends EntryPointForMatlabAbstract{

	public EntryPointForMatlab(boolean printPartitions, boolean printCRNs, ICRN crn,boolean fastDegreeOneBE){
		super(printPartitions,printCRNs,crn,fastDegreeOneBE);
		CRNReducerCommandLine.println(out,bwOut,"ERODE instantiated");
	}
	
	public EntryPointForMatlab(boolean printPartitions, boolean printCRNs,boolean fastDegreeOneBE){
		super(printPartitions, printCRNs,fastDegreeOneBE);
		CRNReducerCommandLine.println(out,bwOut,"ERODE instantiated");
	}
	
	public EntryPointForMatlab(boolean printPartitions, boolean printCRNs, ICRN crn){
		super(printPartitions,printCRNs,crn,false);
		CRNReducerCommandLine.println(out,bwOut,"ERODE instantiated");
	}
	
	public EntryPointForMatlab(boolean printPartitions, boolean printCRNs){
		super(printPartitions, printCRNs,false);
		CRNReducerCommandLine.println(out,bwOut,"ERODE instantiated");
	}

	public static void main(String[] args) throws UnsupportedFormatException, Z3Exception, IOException {
		//CRNReducerCommandLine.println("BBB");
		//CRNReducerCommandLine.println("BBB");
		
		//Start up ERODE
		boolean printPartitions=true;
		boolean printCRNs=true;
		boolean fastDegreeOneBE=true;
		EntryPointForMatlab entry = new EntryPointForMatlab(printPartitions,printCRNs,fastDegreeOneBE);
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
		//int[] zipped=entry.zip(pippo);
		
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
		
//		entry.load("ERBB2.ode");
//		int[] initialPartitionArray = new int[entry.getSpeciesNames().length];
//		Arrays.fill(initialPartitionArray, 1);
//		initialPartitionArray[13]=14;//14 is id+1
//		initialPartitionArray[20]=21;//21 is id+1
//		entry.computeBE(initialPartitionArray,true);
		
		
		//Load a model
		entry.load("ERBB2.ode");
		int[] initialPartitionArray = new int[entry.getSpecies().size()];
		
		//Create prepartition: all species with same 'number' will belong to the same block
		Arrays.fill(initialPartitionArray, 1);//all in the same block
		//I want these two species to be in their own block
		initialPartitionArray[13]=2;
		initialPartitionArray[20]=3;
		double epsilon=0.1;
		int[] obtainedPartition=entry.computeEpsBE(epsilon, initialPartitionArray, false);
		System.out.println(Arrays.toString(obtainedPartition));
		
		
	}
	
	private int[] zip(int[] array,boolean aroundZero) throws UnsupportedFormatException, Z3Exception, IOException{
		LinkedHashSet<Integer> entries = new LinkedHashSet<>();
		for(int i=0;i<array.length;i++){
			entries.add(array[i]);
		}
		HashMap<Integer, Integer> map = new HashMap<Integer, Integer>(entries.size());
		
		int t=1;
		if(aroundZero){
			t=-entries.size()/2;
		}
		for (Integer integer : entries) {
			if(t==0){
				t++;
			}
			map.put(integer, t);
			t++;
		}
		
		int[] array2 = new int[array.length];
		for(int i=0;i<array.length;i++){
			array2[i]=map.get(array[i]);
		}
		return array2;
	}
	
	public int[] zip(int[] array) throws UnsupportedFormatException, Z3Exception, IOException{
		return zip(array,false);
	}
	
	public int[] zipAroundZero(int[] array) throws UnsupportedFormatException, Z3Exception, IOException{
		return zip(array,true);
	}
	
	public double[][] computeJacobian(){
		double[] populations = new double[getSpecies().size()];
		Arrays.fill(populations, 1);
		return computeJacobian(populations);
		
	}
	
	public double[][] computeJacobian(int[] populations){
		double[] doublePopulations=new double[populations.length];
		for(int i=0;i<populations.length;i++){
			doublePopulations[i]=populations[i];
		}
		return computeJacobian(doublePopulations);
	}
	
	/*public static double NonZeroorMinDouble(double val){
		if(val==0){
			return Double.MIN_VALUE;
		}
		else{
			return val;
		}
		//return val;
	}*/
	
	public double[][] computeJacobianInefficient(double[] populations){
		double[][] populationsOneStep = new double[populations.length][];

		for(int i=0;i<populations.length;i++){
			populationsOneStep[i]=new double[1];
			populationsOneStep[i][0]=populations[i];
		}
		
		MutableBoolean warned = new MutableBoolean();
		int[][] jumps = DataOutputHandlerAbstract.computeJumps(erode.getCRN());
		double[][] Je = DataOutputHandlerAbstract.computeCurrentJacobian(populationsOneStep, 0, erode.getCRN(), jumps,null,null,null,warned);
		
		printJacobian(Je,out,bwOut);
		
		return Je;
	}
	
	public double[][] computeJacobianOldWithDivisionByZeroProblem(double[] populations){
		
		List<ICRNReaction> reactions = erode.getCRN().getReactions();
		//List<ISpecies> allSpecies = crnreducer.getCRN().getSpecies();

		//Compute nv: the actual firing rate of each reaction
		double[] nv = new double[reactions.size()];
		int r=0; 
		for (ICRNReaction reaction : reactions) {
			IComposite lhs = reaction.getReagents();
			// get the total rate in normal mass action
			nv[r] = reaction.getRate().doubleValue();
			for(int i=0;i<lhs.getNumberOfDifferentSpecies();i++){
				ISpecies s = lhs.getAllSpecies(i);
				int mult = lhs.getMultiplicities(i);
				//nv[r] *= mult; //Math.pow(y[s.getLabelID()], mult);
				nv[r] *= Math.pow(populations[s.getID()], mult);//nv[r] *= Math.pow(NonZeroorMinDouble(populations[s.getID()]), mult);
			}
			r++;
		}

		//Build the jump vector of each reaction. This is very space demanding: numberOfReactions*numberOfSpecies. Check if I can instead store an array of composites (the jvComposite).
		int[][] jumps = DataOutputHandlerAbstract.computeJumps(erode.getCRN());
		/*int[][] jumps = new int[reactions.size()][allSpecies.size()];
		r=0;
		for(ICRNReaction reaction : reactions){
			IComposite jvComposite = reaction.computeProductsMinusReagents();
			for(int s =0;s<jvComposite.getNumberOfDifferentSpecies();s++){
				int sId=jvComposite.getAllSpecies(s).getID();
				int mult = jvComposite.getMultiplicities(s);
				jumps[r][sId]=mult;
			}
			r++;
		}*/

		double[][] Je = CRNVectorFieldApacheCovariances.computeCurrentJacobianOldWithDivisionByZeroProblem(populations, nv, erode.getCRN(), jumps);
		double[][] Je2 = CRNVectorFieldApacheCovariances.computeCurrentJacobian(populations, erode.getCRN(), jumps);
		
		/*double[][] Je = new double[idToSpecies.length][idToSpecies.length];
		int j=0;
		for(ICRNReaction reaction : reactions){
			for(int i=0;i<allSpecies.size();i++){
				for(int k=0;k<allSpecies.size();k++){
					//double divisor = populations[k];
					//if(divisor==0){
					//	divisor=Double.MIN_VALUE;
					//}
					//double add = (jumps[j][i] * reaction.getReagents().getMultiplicityOfSpeciesWithId(k) * nv[j]) / divisor;
					//if(populations[k]==0 && add!=0){
					//	System.out,bwOut.println("ciao");
					//}
					//Je[i][k]+= add;
					if(populations[k]!=0){
						Je[i][k]+= (jumps[j][i] * reaction.getReagents().getMultiplicityOfSpeciesWithId(k) * nv[j]) / populations[k];
						//Je[i][k]+= jumps[j][i] * reaction.getReagents().getMultiplicity(k) * nv[j] ; // / y[k];
					}
					//if(populations[k]==0){
					//	System.out,bwOut.println("jumps[j][i]="+jumps[j][i]);
					//	System.out,bwOut.println("reaction.getReagents().getMultiplicityOfSpeciesWithId(k)="+reaction.getReagents().getMultiplicityOfSpeciesWithId(k));
					//	System.out,bwOut.println("nv[j]="+nv[j]);
					//	System.out,bwOut.println("add="+(jumps[j][i] * reaction.getReagents().getMultiplicityOfSpeciesWithId(k) * nv[j]));
					//	System.out,bwOut.println("addDivided="+(jumps[j][i] * reaction.getReagents().getMultiplicityOfSpeciesWithId(k) * nv[j]) / NonZeroorMinDouble(populations[k]));
					//	System.out,bwOut.println();
					//	System.out,bwOut.println();
					//}
					//Je[i][k]+= (jumps[j][i] * reaction.getReagents().getMultiplicityOfSpeciesWithId(k) * nv[j]) / NonZeroorMinDouble(populations[k]);
				}
			}
			j++;
		}
		*/
		
		printJacobian(Je,out,bwOut);
		printJacobian(Je2,out,bwOut);
		
		return Je;
	}
	
	
public double[][] computeJacobian(double[] populations){
		
		//Build the jump vector of each reaction. This is very space demanding: numberOfReactions*numberOfSpecies. Check if I can instead store an array of composites (the jvComposite).
		int[][] jumps = DataOutputHandlerAbstract.computeJumps(erode.getCRN());

		double[][] Je = CRNVectorFieldApacheCovariances.computeCurrentJacobian(populations, erode.getCRN(), jumps);
		
		printJacobian(Je,out,bwOut);
		
		return Je;
	}

	public static void printJacobian(double[][] Je,MessageConsoleStream out,BufferedWriter bwOut) {
		CRNReducerCommandLine.println(out,bwOut);
		CRNReducerCommandLine.println(out,bwOut,"The Jacobian: ");
		for(int i=0;i<Je.length;i++){
			for(int j=0;j<Je[i].length;j++){
				String pre="";
				if(Je[i][j]>=0){
					pre+=" ";
				}
				if(Je[i][j]>-10 && Je[i][j]<10){
					pre+=" ";
				}
				CRNReducerCommandLine.print(out,bwOut,pre+Je[i][j]+" ");
			}
			CRNReducerCommandLine.println(out,bwOut);
		}
	}
	
	/*public double[][] computeJacobianDouble(double[] concentrations){

		double[][] Je = new double[idToSpecies.length][idToSpecies.length];

		List<ICRNReaction> reactions = crnreducer.getCRN().getReactions();
		List<ISpecies> allSpecies = crnreducer.getCRN().getSpecies();

		//Compute nv: the actual firing rate of each reaction
		double[] nv = new double[reactions.size()];
		int r=0; 
		for (ICRNReaction reaction : reactions) {
			IComposite lhs = reaction.getReagents();
			// get the total rate in normal mass action
			nv[r] = reaction.getRate().doubleValue();
			for(int i=0;i<lhs.getNumberOfDifferentSpecies();i++){
				ISpecies s = lhs.getAllSpecies(i);
				int mult = lhs.getMultiplicities(i);
				//nv[r] *= mult; //Math.pow(y[s.getLabelID()], mult);
				nv[r] *= Math.pow(concentrations[s.getID()], mult);
			}
			r++;
		}

		//Build the jump vector of each reaction. This is very space demanding: numberOfReactions*numberOfSpecies. Check if I can instead store an array of composites (the jvComposite).
		int[][] jumps = new int[reactions.size()][allSpecies.size()];
		r=0;
		for(ICRNReaction reaction : reactions){
			IComposite jvComposite = reaction.computeProductsMinusReagents();
			for(int s =0;s<jvComposite.getNumberOfDifferentSpecies();s++){
				int sId=jvComposite.getAllSpecies(s).getID();
				int mult = jvComposite.getMultiplicities(s);
				jumps[r][sId]=mult;
			}
			r++;
		}

		int j=0;
		for(ICRNReaction reaction : reactions){
			for(int i=0;i<allSpecies.size();i++){
				for(int k=0;k<allSpecies.size();k++){
					if(concentrations[k]!=0){
						Je[i][k]+= (jumps[j][i] * reaction.getReagents().getMultiplicityOfSpeciesWithId(k) * nv[j]) / concentrations[k];
						//Je[i][k]+= jumps[j][i] * reaction.getReagents().getMultiplicity(k) * nv[j] ; // / y[k];
					}
				}
			}
			j++;
		}
		
		CRNReducerCommandLine.println(out,bwOut);
		CRNReducerCommandLine.println(out,bwOut,"The Jacobian: ");
		for(int i=0;i<Je.length;i++){
			for(j=0;j<Je[i].length;j++){
				String pre="";
				if(Je[i][j]>=0){
					pre+=" ";
				}
				if(Je[i][j]>-10 && Je[i][j]<10){
					pre+=" ";
				}
				CRNReducerCommandLine.print(out,bwOut,pre+Je[i][j]+" ");
			}
			CRNReducerCommandLine.println(out,bwOut);
		}
		
		return Je;
	}*/
	
	/*public void printMatrix(double[][] matrix) {
		for(int i=0;i<matrix.length;i++){
			for(int j=0;j<matrix[i].length;j++){
				System.out,bwOut.print(" "+matrix[i][j]);
			}
			System.out,bwOut.println();
		}
	}*/
}
