package it.imt.erode.partitionrefinement.algorithms;

import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.MatrixUtils;
//import org.apache.commons.math3.linear.RectangularCholeskyDecomposition;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.eclipse.ui.console.MessageConsoleStream;
import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.text.parser.ParseException;

import it.imt.erode.auxiliarydatastructures.IPartitionAndBoolean;
import it.imt.erode.auxiliarydatastructures.Reduction;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.implementations.CRNReactionArbitraryGUI;
import it.imt.erode.crn.implementations.Species;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.label.EmptySetLabel;
import it.imt.erode.crn.label.ILabel;
import it.imt.erode.expression.evaluator.MathEval;
import it.imt.erode.expression.parser.IMonomial;
import it.imt.erode.expression.parser.NumberMonomial;
import it.imt.erode.expression.parser.ProductMonomial;
import it.imt.erode.importing.GUICRNImporter;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.importing.UnsupportedReactionNetworkEncodingException;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SplittersGenerator;
import it.imt.erode.partitionrefinement.splittersbstandcounters.VectorOfCoefficientExprForEachMonomial;
import it.imt.erode.partitionrefinement.splittersbstandcounters.VectorOfCoefficientsForEachMonomial;
import it.imt.erode.partitionrefinement.splittersbstandcounters.VectorOfCoefficientsForEachParameter;

public class EpsilonDifferentialEquivalences {

	public static final String NAME_OFSWAPPING_SPECIES = "SWAPPINGSPECIES";
	public static final ISpecies SWAPPING_SPECIES = new Species(NAME_OFSWAPPING_SPECIES, -1, BigDecimal.ZERO, "0",false);
	private double initTime=0;
	private HashMap<ISpecies, ArrayList<IMonomial>> speciesToMonomialODE;
	private HashMap<ISpecies, ASTNode> speciesToDrift;
	private HashMap<String, ISpecies> speciesNameToSpecies;
	private boolean init=false;
	
	
	public IPartitionAndBoolean computeCoarsest(Reduction red,BigDecimal epsilon, ICRN crn, IPartition partition, boolean verbose,MessageConsoleStream out, 
			BufferedWriter bwOut, Terminator terminator,boolean printEps) throws UnsupportedFormatException{
		
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,red.toString()+" (eps="+epsilon.doubleValue()+") Reducing: "+crn.getName());
		}
		
		if(printEps){
			CRNReducerCommandLine.print(out,bwOut, " (eps="+epsilon.doubleValue()+") ... ");
		}
		
		
		IPartition obtainedPartition = partition.copy();
		
		if(crn.isSymbolic()){
			CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it contains symbolic parameters (i.e. parameters with no acutal value assigned) I terminate.");
			return new IPartitionAndBoolean(obtainedPartition, false);
		}
		
		if(verbose){
			String blocks = " blocks";
			if(obtainedPartition.size()==1){
				blocks = " block";
			}
			CRNReducerCommandLine.println(out,bwOut,"Before "+red.toString()+" partitioning we have "+ obtainedPartition.size() + blocks +" and "+crn.getSpecies().size()+ " species.");
		}

		long begin = System.currentTimeMillis();
		long beginInit = System.currentTimeMillis();
		if(red.equals(Reduction.ENBB)) {
			initOnTheFlyENBB(crn,verbose,out,bwOut,terminator);
		}
		else {
			init(red,crn,verbose,out,bwOut,terminator);
		}
		long endInit = System.currentTimeMillis();
		initTime = (double)(endInit-beginInit) / 1000.0;
				
		if(verbose){	
			CRNReducerCommandLine.println(out,bwOut,"Init requred: "+String.format(CRNReducerCommandLine.MSFORMAT,String.valueOf(initTime))+" (s)");
		}
		
		
		refine(red,epsilon,crn,obtainedPartition,terminator);
		
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"The final partition:");
			CRNReducerCommandLine.println(out,bwOut,obtainedPartition);
		}

		long end = System.currentTimeMillis();
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,red+" Partitioning completed. From "+ crn.getSpecies().size() +" species to "+ obtainedPartition.size() + " blocks. Time necessary: "+(end-begin)+ " (ms)");
			CRNReducerCommandLine.println(out,bwOut,"");
		}
		
		obtainedPartition.setMinAsRepresentative();
		
		return new IPartitionAndBoolean(obtainedPartition,true);
		
	}

//	public AXB computeReferenceTrajectory(Reduction red, ICRN crn, IPartition obtainedPartition, MessageConsoleStream out, BufferedWriter bwOut, boolean solveLinearSystem,Terminator terminator,boolean printM) throws UnsupportedFormatException {
//		return computeReferenceTrajectory(red, crn, obtainedPartition, out, bwOut,solveLinearSystem,new LinkedHashSet<String>(),terminator,printM);
//	}
	public AXB computeReferenceTrajectory(Reduction red, ICRN crn, IPartition obtainedPartition, MessageConsoleStream out, 
			BufferedWriter bwOut, boolean solveLinearSystem, LinkedHashSet<String> paramsToPerturb,Terminator terminator, 
			boolean printM) throws UnsupportedFormatException {
		return computeReferenceTrajectory(red, crn, obtainedPartition, out, 
				bwOut, solveLinearSystem, paramsToPerturb,terminator, 
				printM,true);
	}
	
	public AXB computeReferenceTrajectory(Reduction red, ICRN crn, IPartition obtainedPartition, MessageConsoleStream out, 
			BufferedWriter bwOut, boolean solveLinearSystem, LinkedHashSet<String> paramsToPerturb,Terminator terminator, 
			boolean printM, boolean printSolution) throws UnsupportedFormatException {
		List<String> linearEquations = new ArrayList<String>();
		
		if(!init) {
			if(red.equals(Reduction.ENBB)) {
				initOnTheFlyENBB(crn,false,out,bwOut,terminator);
			}
			else {
				init(red,crn,false,out,bwOut,terminator);
			}
		}
		
		computeLineaEquations(red, obtainedPartition, linearEquations,paramsToPerturb,terminator, crn.getMath());

		//We now parse each of the equations computed in the previous loop. 
		HashMap<String, ISpecies> fakeSpeciesNameToSpecies=new HashMap<String, ISpecies>(0);
		/*
		if(red.equals(Reduction.ENBB)){
			System.out.println("The epsilon BDE is a BDE for all parameters satisfying:");
		}
		else {
			System.out.println("The epsilon FDE is an FDE for all parameters satisfying:");
		}
		*/
		
		VectorOfCoefficientsForEachParameter[] eqMonomials = new VectorOfCoefficientsForEachParameter[linearEquations.size()];
		int i =0;
		//LinkedHashSet<String> parameters = new LinkedHashSet<>();
		for (String eq : linearEquations) {
			//System.out.println(eq);
			ASTNode expr;
			try {
				expr = ASTNode.parseFormula(eq);
			} catch (ParseException e) {
				throw new UnsupportedFormatException("Problems in transforming "+eq+" in a parse tree: "+e.getMessage());
			}
			ArrayList<IMonomial> monomials;
			try {
				monomials = GUICRNImporter.parseGUIPolynomialArbitraryRateExpression(expr,fakeSpeciesNameToSpecies,crn.getMath());
			} catch (UnsupportedReactionNetworkEncodingException e) {
				throw new UnsupportedFormatException("Problems in computing the monomials in "+expr+". Message: "+e.getMessage());
			}
			eqMonomials[i]= new VectorOfCoefficientsForEachParameter(monomials);
			//parameters.addAll(eqMonomials[i].getParameters());
			i++;
		}
		
		double[] b = new double[eqMonomials.length];
		
		double[][] linearSystem = new double[eqMonomials.length][];
		
		AXB axb = new AXB(b,linearSystem,paramsToPerturb);
		
		for(int eq=0;eq<eqMonomials.length;eq++){
			linearSystem[eq]=new double[paramsToPerturb.size()];
			VectorOfCoefficientsForEachParameter paramCoeffOfEq = eqMonomials[eq];
			int p=0;
			for (String param : paramsToPerturb) {
				linearSystem[eq][p]=paramCoeffOfEq.getCoefficient(param).doubleValue();
				p++;
			}
			b[eq] = eqMonomials[eq].getBEntry();
		}
		
		if(printM){
			printMatrix(linearSystem,b,paramsToPerturb,out,bwOut);
		}

		if(eqMonomials.length>0 && solveLinearSystem){
			RealMatrix m = MatrixUtils.createRealMatrix(linearSystem);
			
			//CholeskyDecomposition dec = new CholeskyDecomposition(m);//needs square
			SingularValueDecomposition dec = new SingularValueDecomposition(m);
			//QRDecomposition dec = new QRDecomposition(m);//needs m>n
			DecompositionSolver solver = dec.getSolver();
			
			
//			if(solver.isNonSingular()){
				RealVector solution = solver.solve(MatrixUtils.createRealVector(b));
				axb.setSolution(solution.toArray());

				if(printSolution) {
					CRNReducerCommandLine.println(out,bwOut, "\nThe solution:");
					int s=0;
					for (String param : paramsToPerturb) {
						CRNReducerCommandLine.println(out,bwOut, param+" = "+solution.getEntry(s));
						s++;
					}		
				}
//			}
//			else{
//				CRNReducerCommandLine.println(out,bwOut, "\nThe matrix is singular. ");
//			}
		}
		
		return axb;
	}

	private void computeLineaEquations(Reduction red, IPartition obtainedPartition, List<String> linearEquations, Collection<String> paramsToPerturb, Terminator terminator, MathEval math) throws UnsupportedFormatException {
		
//		if(!init) {
//			initOnTheFlyENBB(crn,verbose,out,bwOut,terminator);
//		}
		
		IBlock currentBlock = obtainedPartition.getFirstBlock();

		//Build the linear constraints (the equations) used to compute the values for the parameters that make the partition BDE. We compute an hashmap mapping each aggregate monomial to the expression of its coefficient.
		//if(red.equals(Reduction.ENBB)){
//		if(red.equals(Reduction.ENBB)){
//			while(currentBlock!=null){
//				if(currentBlock.getSpecies().size()>1){
//					computeBackwardLinearEquationForReferenceTrajectory(obtainedPartition, linearEquations, currentBlock,paramsToPerturb);
//				}
//				currentBlock=currentBlock.getNext();
//			}
//		}
//		else{
//			//ENFB
//			
//			//Here I have to: 
//			//	1) compute each cumulative block-drift
//			//	2) swap 0-rep of currentBlock in each cumulativeBlockDrift
//			//HashMap<IBlock, VectorOfCoefficientExprForEachMonomial> blockToCoeffExprOfCurrentBlock = new HashMap<IBlock, VectorOfCoefficientExprForEachMonomial>(obtainedPartition.size());
//			while(currentBlock!=null){
//				HashMap<HashMap<ISpecies, Integer>, String> monomialToCoeffExprOfBlock = new HashMap<>();
//				for (ISpecies species : currentBlock.getSpecies()) {
//					ArrayList<IMonomial> monomialsOfSpecies = speciesToMonomialODE.get(species);
//					computeCoeffExpr(paramsToPerturb, monomialToCoeffExprOfBlock, monomialsOfSpecies);
//				}
//				VectorOfCoefficientExprForEachMonomial coeffExprOfCurrentBlock = new VectorOfCoefficientExprForEachMonomial(monomialToCoeffExprOfBlock);
//				//blockToCoeffExprOfCurrentBlock.put(currentBlock, coeffExprOfCurrentBlock);
//				VectorOfCoefficientExprForEachMonomial coeffExprOfCurrentBlockSwapped = computeAllToRepresentativeCumulativeDriftOfBlock(currentBlock, speciesNameToSpecies, math, terminator, obtainedPartition,paramsToPerturb);
//				
//				
//				currentBlock=currentBlock.getNext();
//			}
//			
//			IBlock blockToSwap = obtainedPartition.getFirstBlock();
//			while(blockToSwap!=null){
//				if(blockToSwap.getSpecies().size()>1){
//					for (Entry<IBlock, VectorOfCoefficientExprForEachMonomial> blockAndItsCoeffExpr : blockToCoeffExprOfCurrentBlock.entrySet()) {
//						VectorOfCoefficientExprForEachMonomial coeffExprOfCurrentBlock=blockAndItsCoeffExpr.getValue();
//						VectorOfCoefficientExprForEachMonomial coeffExprOfCurrentBlockSwapped = computeAllToRepresentativeCumulativeDriftOfBlock(currentBlock, speciesNameToSpecies, math, terminator, obtainedPartition,paramsToPerturb);
//					}
//					
//					
//					coeffExprOfCurrentBlock = blockToCoeffExprOfCurrentBlock.get(blockToSwap)
//					
//					//swap 0-rep of currentBlock in each cumulativeBlockDrift
//					VectorOfCoefficientExprForEachMonomial coeffExprOfCurrentBlockSwapped = computeAllToRepresentativeCumulativeDriftOfBlock(currentBlock, speciesNameToSpecies, math, terminator, obtainedPartition,paramsToPerturb);
//					coeffExprOfCurrentBlock.computeLinearEquations(coeffExprOfCurrentBlockSwapped, linearEquations);
//				}
//				blockToSwap=blockToSwap.getNext();
//			}
//			
//			computeForwardLinearEquationForReferenceTrajectory(obtainedPartition, linearEquations, currentBlock,paramsToPerturb, terminator, math, obtainedPartition);
//		}
		
		
		while(currentBlock!=null){
			if(red.equals(Reduction.ENBB)){
				if(currentBlock.getSpecies().size()>1){
					computeBackwardLinearEquationForReferenceTrajectory(obtainedPartition, linearEquations, currentBlock,paramsToPerturb);
				}
			}
			else{
				//ENFB
				computeForwardLinearEquationForReferenceTrajectory(obtainedPartition, linearEquations, currentBlock,paramsToPerturb, terminator, math, obtainedPartition);
				//computeBackwardLinearEquationForReferenceTrajectory(obtainedPartition, linearEquations, currentBlock,paramsToPerturb);
			}
			currentBlock=currentBlock.getNext();
		}
		/*}
		else{
			HashMap<IBlock, VectorOfCoefficientsForEachMonomial> blockToCumulativeDrift = computeCumulativeDriftOfEachBlock(obtainedPartition,terminator);
		}*/
	}

	private void computeForwardLinearEquationForReferenceTrajectory(IPartition obtainedPartition,
			List<String> linearEquations, IBlock currentBlock, Collection<String> paramsToPerturb, Terminator terminator, MathEval math, IPartition partition) throws UnsupportedFormatException {
		
		//Here I have to: 
		//	1) compute each cumulative block-drift
		//	2) swap 0-rep of currentBlock in each cumulativeBlockDrift
		
		HashMap<HashMap<ISpecies, Integer>, String> monomialToCoeffExprOfBlock = new HashMap<>();
		for (ISpecies species : currentBlock.getSpecies()) {
			ArrayList<IMonomial> monomialsOfSpecies = speciesToMonomialODE.get(species);
			computeCoeffExpr(paramsToPerturb, monomialToCoeffExprOfBlock, monomialsOfSpecies);
		}
		VectorOfCoefficientExprForEachMonomial coeffExprOfCurrentBlock = new VectorOfCoefficientExprForEachMonomial(monomialToCoeffExprOfBlock);
		
		//This vector represents the coefficients of the monomials in the cumulative block drift, after replacing non rep with 0, and rep with the sum of the block
		VectorOfCoefficientExprForEachMonomial coeffExprOfCurrentBlockSwapped = computeAllToRepresentativeCumulativeDriftOfBlock(currentBlock, speciesNameToSpecies, math, terminator, partition,paramsToPerturb);
		
		coeffExprOfCurrentBlock.computeLinearEquations(coeffExprOfCurrentBlockSwapped, linearEquations);
		
		/*
		ArrayList<IMonomial> monomialsOfBlock = new ArrayList<>();
		for (ISpecies species : currentBlock.getSpecies()) {
			monomialsOfBlock.addAll(speciesToMonomialODE.get(species));
		}
		HashMap<HashMap<ISpecies, Integer>, String> cumulativeMonomialToCoeffExpr = new HashMap<>(monomialsOfBlock.size());
		
		for (IMonomial monomial : monomials) {
			String coefficient;
			if(paramsToPerturb==null){
				coefficient = monomial.getOrComputeCoefficientExpression();
			}
			else{
				coefficient = monomial.getOrComputeCoefficientExpression(paramsToPerturb);
			}
			
			HashMap<ISpecies, Integer> speciesOfMonomial = monomial.getOrComputeSpecies();
			HashMap<ISpecies, Integer> representativeSpeciesOfMonomial = computeRepresentativeHashMap(speciesOfMonomial, partition);
			String prev = aggrMonomialToCoeffExpr.get(representativeSpeciesOfMonomial);

			if(prev==null){
				prev = coefficient;
			}
			else{
				prev = prev + " + " + coefficient;
			}
			aggrMonomialToCoeffExpr.put(representativeSpeciesOfMonomial, prev);
		}
		
		devo fare tipo come fatto in refine: devo calcoare "swappedcumulativeMonomialToCoeffExpr".
		
		ora credo che devo prendere 
		
		evalCoefficientExprs(partition, monomialsOfBlock, cumulativeMonomialToCoeffExpr,paramsToPerturb);
		*/
		//for each block H
		//compute f_H and f_H[all guys to rep];
		//get the monomials from f_H - f_H[all guys to rep] = 0
		//add the equations arising from the monomials
		//throw new UnsupportedOperationException("Yet to be implemented.");
	}

	private void computeBackwardLinearEquationForReferenceTrajectory(IPartition obtainedPartition,
			List<String> linearEquations, IBlock currentBlock, Collection<String> paramsToPerturb) {
		LinkedHashMap<ISpecies, VectorOfCoefficientExprForEachMonomial> coeffExpressionsOfBlock = computeAggregateCoefficientExpr(currentBlock, obtainedPartition,paramsToPerturb);
		ISpecies rep = currentBlock.getRepresentative();
		VectorOfCoefficientExprForEachMonomial repCoeffExpr = coeffExpressionsOfBlock.get(rep);
		for (Entry<ISpecies, VectorOfCoefficientExprForEachMonomial> entry : coeffExpressionsOfBlock.entrySet()) {
			if(!entry.getKey().equals(rep)){
				//System.out.println(entry.getKey().getName() + "has aggregate drift:\n\t"+entry.getValue());
				repCoeffExpr.computeLinearEquations(entry.getValue(),linearEquations);
			}
		}
	}
	
	public void printMatrix(double[][] matrix, double[] b, LinkedHashSet<String> columnLabels, MessageConsoleStream out, BufferedWriter bwOut) {
		if(columnLabels.size()>0){
			CRNReducerCommandLine.print(out,bwOut,"\tPrinting the matrix defining the linear system of contstraints.\n\tThe parameters:\n\t");
			for (String label : columnLabels) {
				CRNReducerCommandLine.print(out,bwOut,label +" ");
			}
			CRNReducerCommandLine.println(out,bwOut);
			CRNReducerCommandLine.print(out,bwOut,"\t");
			for(int i=0;i<matrix.length;i++){
				for(int j=0;j<matrix[i].length;j++){
					CRNReducerCommandLine.print(out,bwOut," "+matrix[i][j]);
				}
				CRNReducerCommandLine.println(out,bwOut," = "+b[i]);
			}
		}
	}
	
	public static HashMap<ISpecies, ArrayList<IMonomial>> computeSpeciesMonomials(ICRN crn,HashMap<ISpecies, StringBuilder> speciesToDriftSB,HashMap<String,ISpecies> speciesNameToSpecies, String varsName/*,ISpecies[] speciesIdToSpecies*/) throws UnsupportedFormatException{
		HashMap<ISpecies, ArrayList<IMonomial>> speciesToMonomialODE = new HashMap<ISpecies, ArrayList<IMonomial>>(crn.getSpecies().size());
		
		for (ISpecies speciesOfODE : crn.getSpecies()) {
			StringBuilder drift = speciesToDriftSB.get(speciesOfODE);
			String driftString = "0";
			if(drift!=null){
				driftString=drift.toString();
			}
			
			ArrayList<IMonomial> monomials;
			ASTNode rateLaw;
			//System.out.println("\n"+driftString);
			try {
				rateLaw = ASTNode.parseFormula(driftString);
			} catch (ParseException e1) {
				throw new UnsupportedFormatException(e1.getMessage());
			}
			try {
				monomials = GUICRNImporter.parseGUIPolynomialODE(rateLaw,speciesNameToSpecies,crn.getMath(),varsName/*,speciesIdToSpecies*/,crn);
			} catch (UnsupportedReactionNetworkEncodingException e) {
				throw new UnsupportedFormatException(e.getMessage());
			}
			
			speciesToMonomialODE.put(speciesOfODE, monomials);
		}
		
		return speciesToMonomialODE;
	}

	private void init(Reduction red, ICRN crn, boolean verbose, MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator) throws UnsupportedFormatException {
		init=true;
		//ArrayList<IMonomial> monomials= parseGUIPolynomialODE(reaction.getRateLaw(),speciesNameToSpecies,math);
		MathEval math = crn.getMath();
		speciesToMonomialODE = new HashMap<ISpecies, ArrayList<IMonomial>>(crn.getSpecies().size());
		/*HashMap<String, ISpecies>*/ speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
		for (ISpecies species : crn.getSpecies()) {
			speciesNameToSpecies.put(species.getName(),species);
		}
		
		if(red.equals(Reduction.ENFB)){
			speciesToDrift = new HashMap<>(crn.getSpecies().size());
		}
		

		boolean ignoreI=false;
		HashMap<ISpecies, StringBuilder> speciesToDriftSB = GUICRNImporter.computeDrifts(crn,false,ignoreI,false);
		for (ISpecies speciesOfODE : crn.getSpecies()) {
			StringBuilder drift = speciesToDriftSB.get(speciesOfODE);
			String driftString = "0";
			if(drift!=null && drift.length()>0){
				driftString=drift.toString();
			}

			ArrayList<IMonomial> monomials;
			ASTNode rateLaw;
			//System.out.println("\n"+driftString);
			try {
				rateLaw = ASTNode.parseFormula(driftString);
			} catch (ParseException e1) {
				throw new UnsupportedFormatException(e1.getMessage());
			}
			try {
				monomials = GUICRNImporter.parseGUIPolynomialArbitraryRateExpression(rateLaw,speciesNameToSpecies,math);
			} catch (UnsupportedReactionNetworkEncodingException e) {
				throw new UnsupportedFormatException(e.getMessage());
			}

			speciesToMonomialODE.put(speciesOfODE, monomials);

			if(red.equals(Reduction.ENFB)){
				speciesToDrift.put(speciesOfODE, rateLaw);
			}
		}

		/*
		for (ICRNReaction reaction : crn.getReactions()) {
			if(reaction.hasArbitraryKinetics() && reaction.isODELike()){
				ISpecies speciesOfODE = reaction.getReagents().getAllSpecies(0);
				ArrayList<IMonomial> mon = speciesToMonomialODE.get(speciesOfODE);
				if(mon!=null){
					throw new UnsupportedFormatException("I found more than one ode-like reaction for species "+speciesOfODE.getName());
				}
				
				CRNReactionArbitraryGUI reactionGUI = (CRNReactionArbitraryGUI)reaction; 
				ArrayList<IMonomial> monomials;
				try {
					monomials = GUICRNImporter.parseGUIPolynomialODE(reactionGUI.getRateLaw(),speciesNameToSpecies,math);
				} catch (UnsupportedReactionNetworkEncodingException e) {
					throw new UnsupportedFormatException(e.getMessage());
				}
				
				//I need a way to map a monomial (a multiplication of species), with the list of all its coefficients in this reaction (in monomials)

				speciesToMonomialODE.put(speciesOfODE, monomials);
				
				//for (IMonomial monomial : monomials) {
				//	double coefficient = monomial.getOrComputeCoefficient();
				//	HashMap<ISpecies, Integer> allSpecies = monomial.getOrComputeSpecies();
				//	System.out.println("monomial: "+monomial+" has coefficient: "+coefficient+", and species: "+allSpecies);
				//}
				
				if(red.equals(Reduction.ENFB)){
					speciesToDrift.put(speciesOfODE, reactionGUI.getRateLaw());
				}
					
			}
			else{
				throw new UnsupportedFormatException("We currently support only the ODE format");
			}
		}
		*/
		
	}
	
	private void initOnTheFlyENBB(ICRN crn, boolean verbose, MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator) throws UnsupportedFormatException {
		init=true;
		//Reduction red=Reduction.ENBB;
		/*HashMap<String, ISpecies>*/ speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
		for (ISpecies species : crn.getSpecies()) {
			speciesNameToSpecies.put(species.getName(),species);
		}
		
//		if(red.equals(Reduction.ENFB)){
//			speciesToDrift = new HashMap<>(crn.getSpecies().size());
//		}

		
		speciesToMonomialODE = new HashMap<ISpecies, ArrayList<IMonomial>>(crn.getSpecies().size());
		//boolean keepExpressionInMA=false;DOES NOT WORK WITH AxB of eps-BDE!!!! Becuase parameters are lost from the expressions
		boolean keepExpressionInMA=true;
		computeMonomialsOfAllSpecies(crn,speciesToMonomialODE,speciesNameToSpecies,keepExpressionInMA);
	}
	
	private static final BigDecimal MinusOne=BigDecimal.valueOf(-1);
	private static final String MinusOneStr="-1";
	private static final IMonomial MinusOneMon=new NumberMonomial(MinusOne, MinusOneStr);

	public static void computeMonomialsOfAllSpecies(ICRN crn,HashMap<ISpecies, ArrayList<IMonomial>>speciesToMonomialODE,
			HashMap<String, ISpecies> speciesNameToSpecies, boolean keepExpressionInMA) throws UnsupportedFormatException {
		MathEval math = crn.getMath();
		boolean ignoreI=false;
		for(ICRNReaction reaction : crn.getReactions()) {
			//If the reaction is MA, and I don't need to keep track of the parameters (e.g. the parameters to do Ax=b for eps-DE), I can do this faster computation. 
			if((!reaction.hasArbitraryKinetics()) && !keepExpressionInMA) {
			//if(reaction.hasArbitraryKinetics() && !keepExpressionInMA) {
				IMonomial ma = reaction.getReagents().toMonomials();
				if(reaction.getRate().compareTo(BigDecimal.ONE)!=0) {
					ma=new ProductMonomial(new NumberMonomial(reaction.getRate(), reaction.getRate().toPlainString()), ma);
				}
				IComposite net = reaction.computeProductsMinusReagents();
				for(int s=0;s<net.getNumberOfDifferentSpecies();s++){
					ISpecies sp = net.getAllSpecies(s);
					IMonomial firingRateOfS = ma;
					int mult =net.getMultiplicities(s);
					if(mult!=1) {
						IMonomial multMon = MinusOneMon;
						if(mult!=-1) {
							multMon=new NumberMonomial(BigDecimal.valueOf(mult),String.valueOf(mult));
						}
						firingRateOfS=new ProductMonomial(multMon,ma);
					}
					ArrayList<IMonomial> monomialsOld = speciesToMonomialODE.get(sp);
					if(monomialsOld==null) {
						monomialsOld=new ArrayList<IMonomial>();
						speciesToMonomialODE.put(sp, monomialsOld);
					}
					monomialsOld.add(firingRateOfS);
				}
			}
			else {
				//If the reaction is not MA, or if it is MA but I need to keep the expression in its rate (e.g. the parameters to do Ax=b for eps-DE)
				String firingRate=reaction.getRateExpression();
				if(!reaction.hasArbitraryKinetics()){
					//firingRate =  reaction.getRateExpression()+"*"+reaction.getReagents().getMassActionExpression(false);
					firingRate=GUICRNImporter.computeMassActionFiringRate(reaction,false,ignoreI,keepExpressionInMA);
				}
				//TODO We can improve by computing once the monomials for firingRate, and then generating product monomials for each species in net
				//	see if(type.equals(Type.TIMES)){ in GUICRNImporter.parseGUIPolynomialODE
				IComposite net = reaction.computeProductsMinusReagents();
				for(int s=0;s<net.getNumberOfDifferentSpecies();s++){
					String firingRateOfS=GUICRNImporter.computeFiringRateForInvolvedODEs(net, s, firingRate);
					ISpecies sp = net.getAllSpecies(s);
					ArrayList<IMonomial> monomials;
					ASTNode rateLaw;
					//System.out.println("\n"+driftString);
					try {
						rateLaw = ASTNode.parseFormula(firingRateOfS);
					} catch (ParseException e1) {
						throw new UnsupportedFormatException(e1.getMessage());
					}
					try {
						monomials = GUICRNImporter.parseGUIPolynomialArbitraryRateExpression(rateLaw,speciesNameToSpecies,math);
					} catch (UnsupportedReactionNetworkEncodingException e) {
						throw new UnsupportedFormatException(e.getMessage());
					}

					ArrayList<IMonomial> monomialsOld = speciesToMonomialODE.get(sp);
					if(monomialsOld==null) {
						speciesToMonomialODE.put(sp, monomials);
					}
					else {
						monomialsOld.addAll(monomials);
					}
				}
			}
		}
	}

	private void refine(Reduction red, BigDecimal epsilon,ICRN crn, IPartition obtainedPartition, Terminator terminator) throws UnsupportedFormatException {	
		List<ILabel> fakeLabels=new ArrayList<>();
		fakeLabels.add(EmptySetLabel.EMPTYSETLABEL);

		int previousPartitionSize = obtainedPartition.size();
		do{
			if(Terminator.hasToTerminate(terminator)){
				return;
			}
			previousPartitionSize = obtainedPartition.size();
			//Actually, these will not be the splitter blocks, but the blocks to split.
			//SplittersGenerator blocksToRefine = obtainedPartition.getOrCreateSplitterGenerator(fakeLabels);
			SplittersGenerator blocksToRefine = obtainedPartition.createSplitterGenerator(fakeLabels);
			blocksToRefine.reset(obtainedPartition.getFirstBlock());
			while(blocksToRefine.hasSplittersToConsider()){
				if(Terminator.hasToTerminate(terminator)){
					break;
				}

				IBlock blockToRefine = blocksToRefine.getBlockSpl();
				if(blockToRefine.getSpecies().size()>1){
					HashMap<ISpecies, ArrayList<ISpecies>> speciesToItsEquivalence;
					
					if(red.equals(Reduction.ENBB)){
						speciesToItsEquivalence = computeNonTransitiveEpsilonBDEquivalences(epsilon, obtainedPartition, blockToRefine,terminator);
					}
					else{
						//ENFB
						speciesToItsEquivalence = computeNonTrnasitiveEpsilonFDEquivalences(epsilon, obtainedPartition, blockToRefine,speciesNameToSpecies,crn.getMath(),terminator);
					}
					
					ArrayList<IBlock> subBlocks = splitAccordingToTransitiveClosure(obtainedPartition, blockToRefine,speciesToItsEquivalence);

					if(subBlocks.size()==1){
						//I did not actually split the block:
						obtainedPartition.substituteAndDecreaseSize(subBlocks.get(0), blockToRefine);
						blocksToRefine.generateNextSplitter();
					}
					else{
						obtainedPartition.remove(blockToRefine);
					}
				}
				else{
					blocksToRefine.generateNextSplitter();
				}
			}
		}while(previousPartitionSize!=obtainedPartition.size());
	}

	private HashMap<ISpecies, ArrayList<ISpecies>> computeNonTrnasitiveEpsilonFDEquivalences(BigDecimal epsilon,
			IPartition obtainedPartition, IBlock blockToRefine, HashMap<String, ISpecies> speciesNameToSpecies, MathEval math, Terminator terminator) throws UnsupportedFormatException {
		//We now compute the equivalence class of each species according to epsilon FDE. We might get a non transitive relation.
		//xi = xj if for all blocks H in obtainedPartition we have sum_{xk in H} driftOfK =coeff[eps] sum_{xk in H} driftOfK[ xi = (xi+xj) , xj = 0 ]. Where =coeff[eps] compares the coefficients as for BDE. 
		
		
		HashMap<IBlock, VectorOfCoefficientsForEachMonomial> blockToCumulativeDrift = computeCumulativeDriftOfEachBlock(obtainedPartition,terminator);
		//HashMap<IBlock, ASTNode> blockToASTNOdeOfCumulativeDrift= computeASTNodeCumulativeDriftOfEachBlock(obtainedPartition);
		if(Terminator.hasToTerminate(terminator)){
			return null;
		}
		
		HashMap<ISpecies, ArrayList<ISpecies>> speciesToItsEquivalence = new HashMap<>(blockToRefine.getSpecies().size());
		int current=0;
		for (ISpecies currentSpecies : blockToRefine.getSpecies()) {
			if(Terminator.hasToTerminate(terminator)){
				return null;
			}
			ArrayList<ISpecies> equivalenceOfSP = speciesToItsEquivalence.get(currentSpecies);
			if(equivalenceOfSP==null){
				equivalenceOfSP=new ArrayList<ISpecies>();
				speciesToItsEquivalence.put(currentSpecies, equivalenceOfSP);
				equivalenceOfSP.add(currentSpecies);
			}
			int toCompare=0;
			for (ISpecies speciesToCompare : blockToRefine.getSpecies()) {
				if(toCompare>current){
					BigDecimal totalDistance = BigDecimal.ZERO;
					IBlock currentBlock = obtainedPartition.getFirstBlock();
					while(currentBlock!=null && totalDistance.compareTo(epsilon)<=0){
						VectorOfCoefficientsForEachMonomial cumulativeDriftOfBlock = blockToCumulativeDrift.get(currentBlock);
						//double s=1;
						//VectorOfCoefficientsForEachMonomial swappedCumulativeDriftOfBlock =	computeSwappedCumulativeDriftOfBlockWithConstantS(currentBlock,currentSpecies,speciesToCompare,s,speciesNameToSpecies,math,terminator);
						VectorOfCoefficientsForEachMonomial swappedCumulativeDriftOfBlock =	computeSwappedCumulativeDriftOfBlock(currentBlock,currentSpecies,speciesToCompare,speciesNameToSpecies,math,terminator);
						BigDecimal distance = cumulativeDriftOfBlock.computeDistanceUpToEpsilon(swappedCumulativeDriftOfBlock,epsilon);
						if(distance==null){
							//I already know that the distance is more than epsilon. Hence I want to stop.
							totalDistance=totalDistance.add(BigDecimal.valueOf(Double.MAX_VALUE));
						}
						else{
							totalDistance = totalDistance.add(distance.abs());
						}
						currentBlock = currentBlock.getNext();
					}
					if(totalDistance.compareTo(epsilon)<=0){
						equivalenceOfSP.add(speciesToCompare);
						ArrayList<ISpecies> equivalence = speciesToItsEquivalence.get(speciesToCompare);
						if(equivalence==null){
							equivalence=new ArrayList<>();
							speciesToItsEquivalence.put(speciesToCompare,equivalence);
							equivalence.add(speciesToCompare);
						}
						equivalence.add(currentSpecies);
					}
				}
				toCompare++;
			}
			current++;
		}
		return speciesToItsEquivalence;
	}

	private HashMap<ISpecies, ArrayList<ISpecies>> computeNonTransitiveEpsilonBDEquivalences(BigDecimal epsilon,
			IPartition obtainedPartition, IBlock blockToRefine, Terminator terminator) {
		HashMap<ISpecies, VectorOfCoefficientsForEachMonomial> aggregateCoefficients= computeAggregateCoefficients(blockToRefine, obtainedPartition,terminator);
		//We now compute the equivalence class of each species according to epsilon BDE. We might get a non transitive relation.
		HashMap<ISpecies, ArrayList<ISpecies>> speciesToItsEquivalence = new HashMap<>(blockToRefine.getSpecies().size());

		
		ArrayList<ISpecies> blockToRefineAsList = new ArrayList<>(blockToRefine.getSpecies());
		for(int current=0;current<blockToRefineAsList.size();current++) {
			ISpecies currentSpecies = blockToRefineAsList.get(current);
			VectorOfCoefficientsForEachMonomial aggrCoeffsOfCurrent = aggregateCoefficients.get(currentSpecies);
			//System.out.println(currentSpecies.getName()+" "+ aggrCoeffsOfCurrent);
			if(aggrCoeffsOfCurrent==null) {
				HashMap<HashMap<ISpecies, Integer>, BigDecimal> empty_hmVariablesToCoefficients= new LinkedHashMap<>(0);
				aggrCoeffsOfCurrent=new VectorOfCoefficientsForEachMonomial(empty_hmVariablesToCoefficients);
			}
			ArrayList<ISpecies> equivalenceOfSP = speciesToItsEquivalence.get(currentSpecies);
			if(equivalenceOfSP==null){
				equivalenceOfSP=new ArrayList<ISpecies>();
				speciesToItsEquivalence.put(currentSpecies, equivalenceOfSP);
				equivalenceOfSP.add(currentSpecies);
			}
			for(int toCompare=current+1;toCompare<blockToRefineAsList.size();toCompare++) {
				ISpecies speciesToCompare = blockToRefineAsList.get(toCompare);
				VectorOfCoefficientsForEachMonomial aggrCoeffsOfToCompare = aggregateCoefficients.get(speciesToCompare);
				//System.out.println("\t"+speciesToCompare.getName()+" "+ aggrCoeffsOfToCompare);
				if(aggrCoeffsOfCurrent.atEpsilonDistance(aggrCoeffsOfToCompare, epsilon)){
					equivalenceOfSP.add(speciesToCompare);
					ArrayList<ISpecies> equivalence = speciesToItsEquivalence.get(speciesToCompare);
					if(equivalence==null){
						equivalence=new ArrayList<>();
						speciesToItsEquivalence.put(speciesToCompare,equivalence);
						equivalence.add(speciesToCompare);
					}
					equivalence.add(currentSpecies);
				}
			}
		}
	

		/*
		int current=0;
		for (ISpecies currentSpecies : blockToRefine.getSpecies()) {
			VectorOfCoefficientsForEachMonomial aggrCoeffsOfCurrent = aggregateCoefficients.get(currentSpecies);
			if(aggrCoeffsOfCurrent==null) {
				HashMap<HashMap<ISpecies, Integer>, BigDecimal> empty_hmVariablesToCoefficients= new LinkedHashMap<>(0);
				aggrCoeffsOfCurrent=new VectorOfCoefficientsForEachMonomial(empty_hmVariablesToCoefficients);
			}
			ArrayList<ISpecies> equivalenceOfSP = speciesToItsEquivalence.get(currentSpecies);
			if(equivalenceOfSP==null){
				equivalenceOfSP=new ArrayList<ISpecies>();
				speciesToItsEquivalence.put(currentSpecies, equivalenceOfSP);
				equivalenceOfSP.add(currentSpecies);
			}
			int toCompare=0;
			for (ISpecies speciesToCompare : blockToRefine.getSpecies()) {
				if(toCompare>current){
					VectorOfCoefficientsForEachMonomial aggrCoeffsOfToCompare = aggregateCoefficients.get(speciesToCompare);
					if(aggrCoeffsOfCurrent.atEpsilonDistance(aggrCoeffsOfToCompare, epsilon)){
						equivalenceOfSP.add(speciesToCompare);
						ArrayList<ISpecies> equivalence = speciesToItsEquivalence.get(speciesToCompare);
						if(equivalence==null){
							equivalence=new ArrayList<>();
							speciesToItsEquivalence.put(speciesToCompare,equivalence);
							equivalence.add(speciesToCompare);
						}
						equivalence.add(currentSpecies);
					}
				}
				toCompare++;
			}
			current++;
		}
		*/
		return speciesToItsEquivalence;
	}

	private ArrayList<IBlock> splitAccordingToTransitiveClosure(IPartition obtainedPartition, IBlock blockToRefine,
			HashMap<ISpecies, ArrayList<ISpecies>> speciesToItsEquivalence) {
		//Now we compute the transitive closure of the equivalences
		ArrayList<IBlock> subBlocks = new ArrayList<IBlock>();
		HashMap<ISpecies, Boolean> alreadyConsidered = new HashMap<ISpecies, Boolean>(blockToRefine.getSpecies().size());
		for (Entry<ISpecies, ArrayList<ISpecies>> entry : speciesToItsEquivalence.entrySet()) {
			if(blockToRefine.isEmpty()){
				//I have already removed all the species.
				break;
			}
			ISpecies species = entry.getKey();
			Boolean cons = alreadyConsidered.get(species);
			if(cons==null){
				alreadyConsidered.put(species, true);
				obtainedPartition.getBlockOf(species).removeSpecies(species);
				IBlock reachable = new Block();
				obtainedPartition.add(reachable);
				subBlocks.add(reachable);
				reachable.addSpecies(species);
				ArrayList<ISpecies> next = entry.getValue();
				if(next!=null){
					for (ISpecies nextSpecies : next) {
						addAllReachable(reachable,nextSpecies,speciesToItsEquivalence,alreadyConsidered,obtainedPartition);
					}
				}
			}
		}
		return subBlocks;
	}

	/*
	private boolean atEpsilonDinstance(ISpecies currentSpecies, ISpecies speciesToCompare, IPartition partition, BigDecimal epsilon) {
		ArrayList<IMonomial> monomialsOfCurrent = speciesToMonomialODE.get(currentSpecies);
		ArrayList<IMonomial> monomialsOfToCompare = speciesToMonomialODE.get(speciesToCompare);

		HashMap<HashMap<ISpecies, Integer>, BigDecimal> aggrMonomialToCoeffOfCurrent = new HashMap<>(monomialsOfCurrent.size());
		HashMap<HashMap<ISpecies, Integer>, BigDecimal> aggrMonomialToCoeffOfToCompare = new HashMap<>(monomialsOfToCompare.size());

		//I have to do it once for all species of a block that I want to compare. And not here.
		evalCoefficients(partition, monomialsOfCurrent, aggrMonomialToCoeffOfCurrent);
		evalCoefficients(partition, monomialsOfToCompare, aggrMonomialToCoeffOfToCompare);

		//Now I have to compare monomialsOfCurrent and monomialsOfToCompare
		VectorOfCoefficientsForEachMonomial aggrCoeffsOfCurrent = new VectorOfCoefficientsForEachMonomial(aggrMonomialToCoeffOfCurrent);
		VectorOfCoefficientsForEachMonomial aggrCoeffsOfToCompare = new VectorOfCoefficientsForEachMonomial(aggrMonomialToCoeffOfToCompare);
		return aggrCoeffsOfCurrent.atEpsilonDistance(aggrCoeffsOfToCompare,epsilon);
	}
	 */
	
	/**
	 * Compute the cumulative drift of the block by replacing all occurrences of currentSpecies with s*(currentSpecies + speciesToCompare), and all occurrences of speciesToCompare with (1-s)*(currentSpecies + speciesToCompare)
	 * @param currentBlock
	 * @param currentSpecies
	 * @param speciesToCompare
	 * @param s
	 * @param speciesNameToSpecies 
	 * @return
	 * @throws UnsupportedFormatException 
	 * @throws UnsupportedReactionNetworkEncodingException 
	 */
	@SuppressWarnings("unused")
	private VectorOfCoefficientsForEachMonomial computeSwappedCumulativeDriftOfBlockWithConstantS(IBlock currentBlock,ISpecies currentSpecies, ISpecies speciesToCompare, double s, HashMap<String, ISpecies> speciesNameToSpecies,MathEval math, Terminator terminator) throws UnsupportedFormatException  {
		HashMap<HashMap<ISpecies, Integer>, BigDecimal> monomialToCoeffOfSwappedCumuluativeDriftOfCurrentBlock = new HashMap<>();
		ASTNode factorForFirstSpecies=null;
		ASTNode factorForSecondSpecies=null;
		ASTNode commonFactor=null;
		try {
			factorForFirstSpecies = ASTNode.parseFormula(String.valueOf(s));
			if(s==1){
				factorForSecondSpecies = ASTNode.parseFormula("0");
			}
			else{
				factorForSecondSpecies = ASTNode.parseFormula("(1-"+s+")");
			}
			String sum = "(" + currentSpecies.getName() +" + "+ speciesToCompare.getName() + ")";
			commonFactor = ASTNode.parseFormula(sum);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		for (ISpecies species : currentBlock.getSpecies()) {
			if(Terminator.hasToTerminate(terminator)){
				return null;
			}
			ASTNode drift = speciesToDrift.get(species);
			ASTNode swappedDrift = drift.clone();
			CRNReactionArbitraryGUI.replaceTwoSpeciesWithTwoExprs(swappedDrift, currentSpecies, factorForFirstSpecies,speciesToCompare, factorForSecondSpecies,commonFactor);
			ArrayList<IMonomial> monomials;
			try {
				monomials = GUICRNImporter.parseGUIPolynomialArbitraryRateExpression(swappedDrift, speciesNameToSpecies, math);
			} catch (UnsupportedReactionNetworkEncodingException e) {
				throw new UnsupportedFormatException(e.getMessage());
			}
			evalCoefficients(monomials, monomialToCoeffOfSwappedCumuluativeDriftOfCurrentBlock,terminator);
		}
		return new VectorOfCoefficientsForEachMonomial(monomialToCoeffOfSwappedCumuluativeDriftOfCurrentBlock);
	}
	
	private VectorOfCoefficientsForEachMonomial computeSwappedCumulativeDriftOfBlock(IBlock currentBlock,ISpecies currentSpecies, ISpecies speciesToCompare, HashMap<String, ISpecies> speciesNameToSpecies,MathEval math, Terminator terminator) throws UnsupportedFormatException  {
		HashMap<HashMap<ISpecies, Integer>, BigDecimal> monomialToCoeffOfSwappedCumuluativeDriftOfCurrentBlock = new HashMap<>();
		ASTNode factorForFirstSpecies=null;
		ASTNode factorForSecondSpecies=null;
		ASTNode commonFactor=null;
		String s = NAME_OFSWAPPING_SPECIES;
		try {
			factorForFirstSpecies = ASTNode.parseFormula(s);
			factorForSecondSpecies = ASTNode.parseFormula("(1-"+s+")");
			String sum = "(" + currentSpecies.getName() +" + "+ speciesToCompare.getName() + ")";
			commonFactor = ASTNode.parseFormula(sum);
		} catch (ParseException e1) {
			throw new UnsupportedFormatException(e1.getMessage());
		}

		for (ISpecies species : currentBlock.getSpecies()) {
			if(Terminator.hasToTerminate(terminator)){
				return null;
			}
			ASTNode drift = speciesToDrift.get(species);
			ASTNode swappedDrift = drift.clone();
			CRNReactionArbitraryGUI.replaceTwoSpeciesWithTwoExprs(swappedDrift, currentSpecies, factorForFirstSpecies,speciesToCompare, factorForSecondSpecies,commonFactor);
			ArrayList<IMonomial> monomials;
			try {
				monomials = GUICRNImporter.parseGUIPolynomialArbitraryRateExpression(swappedDrift, speciesNameToSpecies, math);
			} catch (UnsupportedReactionNetworkEncodingException e) {
				throw new UnsupportedFormatException(e.getMessage());
			}
			evalCoefficients(monomials, monomialToCoeffOfSwappedCumuluativeDriftOfCurrentBlock,terminator);
		}
		return new VectorOfCoefficientsForEachMonomial(monomialToCoeffOfSwappedCumuluativeDriftOfCurrentBlock);
	}
	
	/**
	 * Compute the cumulative drift of the block by replacing all occurrences of non representative species with 0, and all occurrences of representative species with the sum of the species in its block
	 * @param currentBlock
	 * @param currentSpecies
	 * @param speciesToCompare
	 * @param s
	 * @param speciesNameToSpecies 
	 * @param paramsToPerturb 
	 * @return
	 * @throws UnsupportedFormatException 
	 * @throws UnsupportedReactionNetworkEncodingException 
	 */
	private VectorOfCoefficientExprForEachMonomial computeAllToRepresentativeCumulativeDriftOfBlock(IBlock currentBlock, HashMap<String, ISpecies> speciesNameToSpecies,MathEval math, Terminator terminator,IPartition partition, Collection<String> paramsToPerturb) throws UnsupportedFormatException  {
		HashMap<HashMap<ISpecies, Integer>, String> monomialToCoeffExprOfBlock = new HashMap<>();
		for (ISpecies species : currentBlock.getSpecies()) {
			if(Terminator.hasToTerminate(terminator)){
				return null;
			}
			ASTNode drift = speciesToDrift.get(species);
			ASTNode swappedDrift = drift.clone();
			CRNReactionArbitraryGUI.replaceNonRepWithZeroAndRepWithBlock(swappedDrift, partition,speciesNameToSpecies);
			ArrayList<IMonomial> monomials;
			try {
				monomials = GUICRNImporter.parseGUIPolynomialArbitraryRateExpression(swappedDrift, speciesNameToSpecies, math);
			} catch (UnsupportedReactionNetworkEncodingException e) {
				throw new UnsupportedFormatException(e.getMessage());
			}
			
			computeCoeffExpr(paramsToPerturb, monomialToCoeffExprOfBlock, monomials);
			
		}
		return new VectorOfCoefficientExprForEachMonomial(monomialToCoeffExprOfBlock);
	}

	/**
	 * Compute the expression of the coefficient of each monomial in monomials (summing coefficients in case of more entries of the same monomial in monomials). These expressions are added to monomialToCoeffExpr. Also, parameters appearing in paramsToPerturb are treated as variables, while the other are treated as numbers (their value) 
	 * @param paramsToPerturb
	 * @param monomialToCoeffExpr
	 * @param monomials
	 */
	private void computeCoeffExpr(Collection<String> paramsToPerturb,
			HashMap<HashMap<ISpecies, Integer>, String> monomialToCoeffExpr, ArrayList<IMonomial> monomials) {
		for (IMonomial monomial : monomials) {
			String coefficient;
			if(paramsToPerturb==null){
				coefficient = monomial.getOrComputeCoefficientExpression();
			}
			else{
				coefficient = monomial.getOrComputeCoefficientExpression(paramsToPerturb);
			}
			
			if(!coefficient.equals("0")){
				HashMap<ISpecies, Integer> speciesOfMonomial = monomial.getOrComputeSpecies();

				String prev = monomialToCoeffExpr.get(speciesOfMonomial);
				if(prev==null){
					prev = coefficient;
				}
				else{
					prev = prev + " + " + coefficient;
				}
				monomialToCoeffExpr.put(speciesOfMonomial, prev);
			}
		}
	}
	
	/*
	private HashMap<IBlock, VectorOfCoefficientsForEachMonomial> computeCumulativeDriftOfEachBlock(IPartition partition, Terminator terminator) {
		return computeCumulativeDriftOfEachBlock(partition, terminator, null);
	}*/
	
	/**
	 * For each block H, compute the cumulative drift xH = sum_{X in H} f_X.
	 * @param paramsToPerturb 
	 * @param obtainedPartition
	 * @return the coefficients in the cumulative drift of the corresponding block.  
	 */
	private HashMap<IBlock, VectorOfCoefficientsForEachMonomial> computeCumulativeDriftOfEachBlock(IPartition partition, Terminator terminator) {
		HashMap<IBlock, VectorOfCoefficientsForEachMonomial> blockToCoefficientsOfCumulativeDrift = new HashMap<IBlock, VectorOfCoefficientsForEachMonomial>(partition.size());
		IBlock currentBlock = partition.getFirstBlock();
		while(currentBlock!=null && !Terminator.hasToTerminate(terminator)){
			HashMap<HashMap<ISpecies, Integer>, BigDecimal> monomialToCoeffOfCumuluativeDriftOfCurrentBlock = new HashMap<>();
			for (ISpecies currentSpecies : currentBlock.getSpecies()) {
				ArrayList<IMonomial> monomialsOfCurrent = speciesToMonomialODE.get(currentSpecies);
				evalCoefficients(monomialsOfCurrent, monomialToCoeffOfCumuluativeDriftOfCurrentBlock,terminator);
			}
			VectorOfCoefficientsForEachMonomial coeffsOfCurrentBlock = new VectorOfCoefficientsForEachMonomial(monomialToCoeffOfCumuluativeDriftOfCurrentBlock);
			blockToCoefficientsOfCumulativeDrift.put(currentBlock, coeffsOfCurrentBlock);
			currentBlock = currentBlock.getNext();
		}
		return blockToCoefficientsOfCumulativeDrift;
	}
	
	private void evalCoefficients(ArrayList<IMonomial> monomials,HashMap<HashMap<ISpecies, Integer>, BigDecimal> monomialToCoeff, Terminator terminator) {
		for (IMonomial monomial : monomials) {
			if(Terminator.hasToTerminate(terminator)){
				return;
			}
			evalCoefficients(monomial, monomialToCoeff);
			/*BigDecimal coefficient = monomial.getOrComputeCoefficient();
			HashMap<ISpecies, Integer> speciesOfMonomial = monomial.getOrComputeSpecies();
			BigDecimal prev = monomialToCoeff.get(speciesOfMonomial);

			if(prev==null){
				prev = coefficient;
			}
			else{
				prev = prev.add(coefficient);
			}
			monomialToCoeff.put(speciesOfMonomial, prev);*/
		}
	}
	
	private void evalCoefficients(IMonomial monomial,HashMap<HashMap<ISpecies, Integer>, BigDecimal> monomialToCoeff) {
		BigDecimal coefficient = monomial.getOrComputeCoefficient();
		if(coefficient.compareTo(BigDecimal.ZERO)!=0){
			HashMap<ISpecies, Integer> speciesOfMonomial = monomial.getOrComputeSpecies();
			BigDecimal prev = monomialToCoeff.get(speciesOfMonomial);

			if(prev==null){
				prev = coefficient;
			}
			else{
				prev = prev.add(coefficient);
			}
			monomialToCoeff.put(speciesOfMonomial, prev);
		}
	}
	
	/**
	 * For each species in blockToRefine, canonize its drift, and computes the coefficient of each canonized monomial.   
	 * @param blockToRefine
	 * @param partition
	 * @return
	 */
	private HashMap<ISpecies, VectorOfCoefficientsForEachMonomial> computeAggregateCoefficients(IBlock blockToRefine, IPartition partition, Terminator terminator) {
		HashMap<ISpecies, VectorOfCoefficientsForEachMonomial> aggregatedCoefficients = new HashMap<ISpecies, VectorOfCoefficientsForEachMonomial>(blockToRefine.getSpecies().size());
		for (ISpecies currentSpecies : blockToRefine.getSpecies()) {
			if(Terminator.hasToTerminate(terminator)){
				return null;
			}
			ArrayList<IMonomial> monomialsOfCurrent = speciesToMonomialODE.get(currentSpecies);
			if(monomialsOfCurrent!=null) {
				HashMap<HashMap<ISpecies, Integer>, BigDecimal> aggrMonomialToCoeffOfCurrent = new HashMap<>(monomialsOfCurrent.size());
				evalAggregateCoefficients(partition, monomialsOfCurrent, aggrMonomialToCoeffOfCurrent);
				VectorOfCoefficientsForEachMonomial aggrCoeffsOfCurrent = new VectorOfCoefficientsForEachMonomial(aggrMonomialToCoeffOfCurrent);
				aggregatedCoefficients.put(currentSpecies, aggrCoeffsOfCurrent);
			}
		}
		return aggregatedCoefficients;
	}
	
	private void evalAggregateCoefficients(IPartition partition, ArrayList<IMonomial> monomials,HashMap<HashMap<ISpecies, Integer>, BigDecimal> aggrMonomialToCoeff) {
		for (IMonomial monomial : monomials) {
			BigDecimal coefficient = monomial.getOrComputeCoefficient();
			HashMap<ISpecies, Integer> speciesOfMonomial = monomial.getOrComputeSpecies();
			HashMap<ISpecies, Integer> representativeSpeciesOfMonomial = computeRepresentativeHashMap(speciesOfMonomial, partition);
			BigDecimal prev = aggrMonomialToCoeff.get(representativeSpeciesOfMonomial);

			if(prev==null){
				prev = coefficient;
			}
			else{
				prev = prev.add(coefficient);
			}
			aggrMonomialToCoeff.put(representativeSpeciesOfMonomial, prev);
		}
	}

	private static HashMap<ISpecies, Integer> computeRepresentativeHashMap(HashMap<ISpecies, Integer> original, IPartition partition) {
		HashMap<ISpecies,Integer> repHashMap = new HashMap<ISpecies, Integer>();

		for (Entry<ISpecies, Integer> entry : original.entrySet()) {
			ISpecies species = entry.getKey();
			int mult = entry.getValue();

			ISpecies rep = partition.getBlockOf(species).getRepresentative();
			Integer multRep = repHashMap.get(rep);
			if(multRep==null){
				multRep = mult;
			}
			else{
				multRep = multRep + mult;
			}
			repHashMap.put(rep, multRep);
		}

		return repHashMap;
	}
	
	private LinkedHashMap<ISpecies, VectorOfCoefficientExprForEachMonomial> computeAggregateCoefficientExpr(IBlock block, IPartition partition, Collection<String> paramsToPerturb) {
		LinkedHashMap<ISpecies, VectorOfCoefficientExprForEachMonomial> aggregatedCoefficientExprs = new LinkedHashMap<ISpecies, VectorOfCoefficientExprForEachMonomial>(block.getSpecies().size());
		ArrayList<IMonomial> dummyMonomials=new ArrayList<IMonomial>(0);
		for (ISpecies currentSpecies : block.getSpecies()) {
			ArrayList<IMonomial> monomialsOfCurrent = speciesToMonomialODE.get(currentSpecies);
			if(monomialsOfCurrent==null) {
				monomialsOfCurrent=dummyMonomials;
			}
			HashMap<HashMap<ISpecies, Integer>, String> aggrMonomialToCoeffExprOfCurrent = new HashMap<>(monomialsOfCurrent.size());
			evalCoefficientExprs(partition, monomialsOfCurrent, aggrMonomialToCoeffExprOfCurrent,paramsToPerturb);
			VectorOfCoefficientExprForEachMonomial aggrCoeffsOfCurrent = new VectorOfCoefficientExprForEachMonomial(aggrMonomialToCoeffExprOfCurrent);
			aggregatedCoefficientExprs.put(currentSpecies, aggrCoeffsOfCurrent);
		}
		return aggregatedCoefficientExprs;
	}
	

	private void evalCoefficientExprs(IPartition partition, ArrayList<IMonomial> monomials,HashMap<HashMap<ISpecies, Integer>, String> aggrMonomialToCoeffExpr, Collection<String> paramsToPerturb) {
		for (IMonomial monomial : monomials) {
			String coefficient;
			if(paramsToPerturb==null){
				coefficient = monomial.getOrComputeCoefficientExpression();
			}
			else{
				coefficient = monomial.getOrComputeCoefficientExpression(paramsToPerturb);
			}
			
			HashMap<ISpecies, Integer> speciesOfMonomial = monomial.getOrComputeSpecies();
			HashMap<ISpecies, Integer> representativeSpeciesOfMonomial = computeRepresentativeHashMap(speciesOfMonomial, partition);
			String prev = aggrMonomialToCoeffExpr.get(representativeSpeciesOfMonomial);

			if(prev==null){
				prev = coefficient;
			}
			else{
				prev = prev + " + " + coefficient;
			}
			aggrMonomialToCoeffExpr.put(representativeSpeciesOfMonomial, prev);
		}
	}

	private static void addAllReachable(IBlock reachable, ISpecies nextSpecies,HashMap<ISpecies, ArrayList<ISpecies>> speciesToItsEquivalence, HashMap<ISpecies, Boolean> alreadyConsidered, IPartition partition) {
		Boolean cons = alreadyConsidered.get(nextSpecies);
		if(cons==null){
			alreadyConsidered.put(nextSpecies, true);
			partition.getBlockOf(nextSpecies).removeSpecies(nextSpecies);
			reachable.addSpecies(nextSpecies);
			ArrayList<ISpecies> next = speciesToItsEquivalence.get(nextSpecies);
			if(next!=null){
				for (ISpecies nextNextSpecies : next) {
					addAllReachable(reachable,nextNextSpecies,speciesToItsEquivalence,alreadyConsidered,partition);
				}
			}
		}
	}

}
