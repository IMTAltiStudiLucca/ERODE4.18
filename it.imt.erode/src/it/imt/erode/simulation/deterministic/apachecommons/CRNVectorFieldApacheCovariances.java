package it.imt.erode.simulation.deterministic.apachecommons;

import java.util.Arrays;
import java.util.Vector;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.ode.FirstOrderIntegrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math3.ode.sampling.FixedStepHandler;
import org.apache.commons.math3.ode.sampling.StepNormalizer;
import org.apache.commons.math3.ode.sampling.StepNormalizerBounds;

import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
//import it.imt.erode.simulation.deterministic.apachecommons.CRNVectorField.MyFixedStepHandler;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.importing.GUICRNImporter;

public class CRNVectorFieldApacheCovariances extends CRNVectorFieldApache {

	private int[][] jumps;
	private int[][] jumpsTransp;
	private IComposite[] netComposites;

	//private int iteration=0;
	
	public CRNVectorFieldApacheCovariances(ICRN crn, Terminator terminator) {
		super(crn,terminator);
		//iteration=0;
		netComposites=new IComposite[crn.getReactions().size()];
		int r=0;
		for (ICRNReaction reaction : crn.getReactions()) {
			netComposites[r]=reaction.computeProductsMinusReagents();
		}
	}


	@Override
	public double[][] solve(double start, double stop, double step,double[] initialCondition) {
		return solve(start, stop, step, initialCondition,1.0e-8, 100.0, 1.0e-10, 1.0e-10);
	}

	@Override
	public double[][] solve(double start, double stop, double step,
			double[] initialCondition, double minStep,
			double maxStep, double absTol,
			double relTol) {
		FirstOrderIntegrator dp853 = new DormandPrince853Integrator(minStep, maxStep, absTol, relTol);

		final Vector<double[]> sol = new Vector<double[]>();

		FixedStepHandler h = new MyFixedStepHandler(sol);
		/*FixedStepHandler h = new FixedStepHandler() {

			@Override
			public void handleStep(double t, double[] y, double[] ydot,boolean lastStep) {
				double[] line = new double[y.length + 1];
				System.arraycopy(y, 0, line, 1, y.length);
				line[0] = t;
				sol.add(line);
			}

			@Override
			public void init(double arg0, double[] arg1, double arg2) {
			}

		};*/

		//Build the jump vector (and its transpose) of each reaction. This is very space demanding: numberOfReactions*numberOfSpecies. Check if I can instead store an array of composites (the jvComposite).
		jumps=new int[getCRN().getReactions().size()][getCRN().getSpecies().size()];
		jumpsTransp=new int[getCRN().getSpecies().size()][getCRN().getReactions().size()];
		int r=0;
		for(ICRNReaction reaction : getCRN().getReactions()){
			IComposite jvComposite = reaction.computeProductsMinusReagents();
			for(int s =0;s<jvComposite.getNumberOfDifferentSpecies();s++){
				int sId=jvComposite.getAllSpecies(s).getID();
				int mult = jvComposite.getMultiplicities(s);
				jumps[r][sId]=mult;
				jumpsTransp[sId][r]=mult;
			}
			r++;
		}

		dp853.addStepHandler(new StepNormalizer(step, h,StepNormalizerBounds.BOTH));
		dp853.integrate(this, start, initialCondition, stop,initialCondition);
		return sol.toArray(new double[sol.size()][]);
	}

	public double[][] transpose(double[][]  matrix) {
		int origRows=matrix.length;
		int origCols=matrix[0].length;
		double[][] transposedMatrix = new double[origCols][origRows];//do I need to init each line to zero?

		for (int i=0;i<origRows;i++) { 
			for (int j=0;j<origCols;j++) {
				transposedMatrix[j][i]=matrix[i][j]; 
			} 
		} 
		return transposedMatrix; 
	}
	
	public static String[][] transpose(String[][]  matrix) {
		int origRows=matrix.length;
		int origCols=matrix[0].length;
		String[][] transposedMatrix = new String[origCols][origRows];//do I need to init each line to zero?

		for (int i=0;i<origRows;i++) { 
			for (int j=0;j<origCols;j++) {
				transposedMatrix[j][i]=matrix[i][j]; 
			} 
		} 
		return transposedMatrix; 
	}



	@Override
	public int getDimension() {
		return getCRN().getSpecies().size() + (getCRN().getSpecies().size()*getCRN().getSpecies().size());
	}

	
	
	@Override
	public void computeDerivatives(double t, double[] y, double[] ydot)
			throws MaxCountExceededException, DimensionMismatchException {

		double[] nv=computeNextydotOfMeans(t, y, ydot);
		if(Terminator.hasToTerminate(getTerminator())){
			return;
		}
		
		/*//System.out.println("nv[] at iteration "+iteration+" is "+nv[0]);
		System.out.println("y at iteration "+iteration+" is "+printArray(y));
		System.out.println("ydot at iteration "+iteration+" is "+printArray(ydot));
		System.out.println("\n");
		iteration++;
		if(iteration==122){
			System.out.println("ciao");
		}*/

		//System.out.println("t= "+t+"\ty="+Arrays.toString(y)+"\tydot="+Arrays.toString(ydot));

		/*
		 * Right now, ydot[ 0 ... getCRN().getSpecies().size()-1 ] contains the concentrations of all species in the current time t, and y those at time t-delta
		 * Now I resort on y to compute the variances
		 */
		//nv is the "fe" of Mirco's MATLAB code.

		//now I compute the "current" Jacobian
		//double[][] Je = computeCurrentJacobianOldWithDivisionByZeroProblem(y, nv,getCRN(),jumps);
		double[][] Je = computeCurrentJacobian(y, getCRN(),jumps);
		
		/*if(iteration<3){
			EntryPointForMatlab.printJacobian(Je, null);
		}*/
		
		int from=getCRN().getSpecies().size();
		int size=getCRN().getSpecies().size();

		double[][] tot = computeCovariances(y, nv, Je, from, size);

		/*if(checkCovariances){
					checkCoVariances(tot);
				}*/

		//now I copy (back) tot (i.e., the updated C (covariances) or the updated jacobian) in ydot (i.e., the updated y)
		from = getCRN().getSpecies().size();
		for(int r=0;r<getCRN().getSpecies().size();r++){
			System.arraycopy(tot[r], 0, ydot, from, tot[r].length);
			from += getCRN().getSpecies().size();
		}				
	}


	public static String printArray(double[] array) {
		StringBuilder sb = new StringBuilder(array.length);
		for(int i=0;i<array.length;i++){
			sb.append(array[i]);
			sb.append(" ");
		}
		return sb.toString();
	}


	private double[][] computeCovariances(double[] y, double[] nv, double[][] Je, int from, int size) {
		//now I compute the G
		double[][] G=new double[getCRN().getSpecies().size()][getCRN().getSpecies().size()];
		for(int m=0;m<getCRN().getReactions().size();m++){
			//if nv[m]=0 I can skip the iteration!
			if(nv[m]!=0){
				double[][] jumpsmTranspXjumpsm = multiplyTranspWithVector(jumps[m]);
				G = sum(G, multiplyScalar(jumpsmTranspXjumpsm,nv[m]));
			}
		}

		//Now I can finally compute the covariances
		//First I store the entries of y regarding the covariances in a squared matrix C
		double[][] C = toSquareMatrix(y,from,size);
		double[][] JeXC= multiply(Je, C);
		double[][] CXJeTransp= multiply(C,transpose(Je));
		double[][] tot= sum(sum(JeXC,CXJeTransp),G);
		return tot;
	}
	

	public static double[][] computeCurrentJacobianOldWithDivisionByZeroProblem(double[] y, double[] nv, ICRN crn,int[][] jumps) {
		double[][] Je = new double[crn.getSpecies().size()][crn.getSpecies().size()];//do I need to init each line to zero?
		int j=0;
		for(ICRNReaction reaction : crn.getReactions()){
			for(int i=0;i<crn.getSpecies().size();i++){
				/*for(int k=0;k<getCRN().getSpecies().size();k++){
							if(y[k]!=0){
								Je[i][k]+= jumps[j][i] * reaction.getReagents().getMultiplicity(k) * nv[j] / y[k];
							}
						}*/
				/*if(reaction.hasArbitraryKinetics()){
					int k = reaction.getFirstArbitraryReagent().getID();
					if(y[k]!=0){
						Je[i][k]+= jumps[j][i] * 1 * nv[j] / y[k];
					}
					k=reaction.getSecondArbitraryReagent().getID();
					if(y[k]!=0){
						Je[i][k]+= jumps[j][i] * 1 * nv[j] / y[k];
					}
				}
				else{*/
				for(int k=0;k<crn.getSpecies().size();k++){
					//TODO: fix division by zero problem!
					if(y[k]!=0){
						Je[i][k]+= jumps[j][i] * reaction.getReagents().getMultiplicityOfSpeciesWithId(k) * nv[j] / y[k];
					}
				}
				//}
			}
			j++;
		}
		return Je;
	}
	
	public static double[][] computeCurrentJacobian(double[] y, ICRN crn,int[][] jumps) {
		double[][] Je = new double[crn.getSpecies().size()][crn.getSpecies().size()];//do I need to init each line to zero?
		int j=0;
		for(ICRNReaction reaction : crn.getReactions()){
			for(int i=0;i<crn.getSpecies().size();i++){
				/*for(int k=0;k<getCRN().getSpecies().size();k++){
							if(y[k]!=0){
								Je[i][k]+= jumps[j][i] * reaction.getReagents().getMultiplicity(k) * nv[j] / y[k];
							}
						}*/
				/*if(reaction.hasArbitraryKinetics()){
					int k = reaction.getFirstArbitraryReagent().getID();
					if(y[k]!=0){
						Je[i][k]+= jumps[j][i] * 1 * nv[j] / y[k];
					}
					k=reaction.getSecondArbitraryReagent().getID();
					if(y[k]!=0){
						Je[i][k]+= jumps[j][i] * 1 * nv[j] / y[k];
					}
				}
				else{*/
				int k=0;
				for(ISpecies species : crn.getSpecies()){
				//for(int k=0;k<crn.getSpecies().size();k++){
					double nv = computeFiringRateDecreasingMultiplicityOfSpecies(reaction,species,y);
					Je[i][k]+= jumps[j][i] * reaction.getReagents().getMultiplicityOfSpeciesWithId(k) * nv;
					k++;
				}
				//}
			}
			j++;
		}
		return Je;
}

	private static double computeFiringRateDecreasingMultiplicityOfSpecies(ICRNReaction reaction, ISpecies toIgnore,double[] y) {
		IComposite lhs = reaction.getReagents();
		double nv=reaction.getRate().doubleValue();
		for(int i=0;i<lhs.getNumberOfDifferentSpecies();i++){
			ISpecies s = lhs.getAllSpecies(i);
			int mult = lhs.getMultiplicities(i);
			if(s.equals(toIgnore)){
				mult=mult-1;
			}
			nv *= Math.pow(y[s.getID()], mult);
		}
		return nv;
	}

	/*private void checkCoVariances(double[][] tot) {
				for(int i=0;i<getCRN().getSpecies().size();i++){
					for(int j=0;j<getCRN().getSpecies().size();j++){
						if(tot[i][j]!=tot[j][i]){
							System.out.println("Problem: C_"+getCRN().getSpecies().get(i).getName()+"-"+getCRN().getSpecies().get(j).getName()+"!= C_"+getCRN().getSpecies().get(j).getName()+"-"+getCRN().getSpecies().get(i).getName());
						}
					}
				}
			}*/

	/**
	 * Transforms an array (starting from position from) in a squared matrix size-by-size
	 * @param y
	 * @param from
	 * @param size
	 * @return
	 */
	public double[][] toSquareMatrix(double[] y, int from, int size) {
		if(y.length-from!=size*size){
			throw new RuntimeException("Illegal matrix dimensions.");
		}
		double [][] C = new double[size][size];
		int i=from;
		for(int r=0;r<size;r++){
			C[r]=Arrays.copyOfRange(y, i, i+size);
			i+=size;
		}
		return C;
	}

	/**
	 * Takes a vector, and returns its transposed multiple by the vector
	 * @param V : the vector
	 * @return V^T * V
	 */
	public static double[][] multiplyTranspWithVector(int[] V) {
		int rows = V.length;
		int columns=V.length;
		double[][] res = new double[rows][columns];
		for (int r = 0; r < rows; r++)
			for (int c = 0; c < columns; c++)
				res[r][c] =  V[r] * V[c];
		return res;
	}

	/**
	 * Multiplies a matrix per a scalar
	 * @param M
	 * @param scalar
	 * @return
	 */
	public double[][] multiplyScalar(double[][] M, double scalar) {
		int rows = M.length;
		int columns=M[0].length;
		double[][] res = new double[rows][columns];//do I have to init this?
		if(scalar!=0){
			for (int r = 0; r < rows; r++)
				for (int c = 0; c < columns; c++)
					res[r][c] = M[r][c] * scalar;
		}
		return res;
	}
	
	/**
	 * Multiplies a matrix per a scalar - computes the string representation
	 * @param M
	 * @param scalar
	 * @return
	 */
	public static String[][] multiplyScalar(double[][] M, String scalar) {
		int rows = M.length;
		int columns=M[0].length;
		String[][] res = new String[rows][columns];//do I have to init this?
		for (int r = 0; r < rows; r++)
			for (int c = 0; c < columns; c++)
				if(M[r][c]==0) {
					res[r][c]="0";
				}
				else if(M[r][c]==1) {
					res[r][c]=scalar;
				}
				else if(M[r][c]==-1) {
					//res[r][c] = " - (" + scalar +")";
					res[r][c] = " - " + GUICRNImporter.addParIfNecessary(scalar);
				}
				else {
					//res[r][c] = M[r][c] + " * (" + scalar +")";
					res[r][c] = M[r][c] + " * " + GUICRNImporter.addParIfNecessary(scalar);
				}
		return res;
	}


	/**
	 * Multiply two matrices
	 * @param A
	 * @param B
	 * @return A*B
	 */
	public double[][] multiply(double[][] A,double[][] B) {
		int mA = A.length;
		int nA = A[0].length;
		int mB = B.length;
		int nB = A[0].length;
		if (nA != mB) throw new RuntimeException("Illegal matrix dimensions.");
		double[][] C = new double[mA][nB];
		for (int i = 0; i < mA; i++)
			for (int j = 0; j < nB; j++)
				for (int k = 0; k < nA; k++)
					C[i][j] += (A[i][k] * B[k][j]);
		return C;
	}
	

	/**
	 * Multiply two matrices - compute the string expressions
	 * @param A
	 * @param B
	 * @return A*B
	 */
	public static String[][] multiply(String[][] A,String[][] B, boolean addParToFirst, boolean addParToSecond) {
		int mA = A.length;
		int nA = A[0].length;
		int mB = B.length;
		int nB = A[0].length;
		if (nA != mB) throw new RuntimeException("Illegal matrix dimensions.");
		String[][] C = new String[mA][nB];
		for (int i = 0; i < mA; i++)
			for (int j = 0; j < nB; j++)
				for (int k = 0; k < nA; k++) {
					//C[i][j] += (A[i][k] * B[k][j]);
					String first = A[i][k];
					if(addParToFirst) {
						//first = "("+first+")";
						first = GUICRNImporter.addParIfNecessary(first);
					}
					String second = B[k][j];
					if(addParToSecond) {
						//second = "("+second+")";
						second = GUICRNImporter.addParIfNecessary(second);
					}
					//String expr = "("+A[i][k]+")" + " * " + "("+B[k][j]+")";
					String expr = first + " * " + second;
					if(C[i][j]==null ||C[i][j].isEmpty()){
						C[i][j]=expr;
					}
					else {
						C[i][j]= C[i][j] + " + " + expr;
					}
				}
		return C;
	}

	/**
	 * Sums two matrices
	 * @param A1
	 * @param A2
	 * @return A1+A2
	 */
	public double[][] sum(double[][] A1, double[][] A2) {
		int rowsA1 = A1.length;
		int columnsA1 = A1[0].length;
		int rowsA2 = A2.length;
		int columnsA2 = A2[0].length;
		if (rowsA1!=rowsA2 || columnsA1!=columnsA2) throw new RuntimeException("Illegal matrix dimensions.");
		double[][] res = new double[rowsA1][columnsA1];
		for (int r = 0; r < rowsA1; r++)
			for (int c = 0; c < columnsA1; c++)
				res[r][c] = A1[r][c] + A2[r][c]; 
		return res;
	}
	
	/**
	 * Sums two matrices
	 * @param A1
	 * @param A2
	 * @return A1+A2
	 */
	public static String[][] sum(String[][] A1, String[][] A2) {
		int rowsA1 = A1.length;
		int columnsA1 = A1[0].length;
		int rowsA2 = A2.length;
		int columnsA2 = A2[0].length;
		if (rowsA1!=rowsA2 || columnsA1!=columnsA2) throw new RuntimeException("Illegal matrix dimensions.");
		String[][] res = new String[rowsA1][columnsA1];
		for (int r = 0; r < rowsA1; r++)
			for (int c = 0; c < columnsA1; c++) {
				if((A1[r][c]==null || A1[r][c].equals("0")) && (A2[r][c]==null || A2[r][c].equals("0"))) {
					res[r][c] = "0";
				}
				else if((A1[r][c]==null || A1[r][c].equals("0"))) {
					res[r][c] = A2[r][c];
				}
				else if((A2[r][c]==null || A2[r][c].equals("0"))) {
					res[r][c] = A1[r][c];
				} 
				else{
					res[r][c] = A1[r][c] + " + " + A2[r][c];
				}
			}
				 
		return res;
	}

}
