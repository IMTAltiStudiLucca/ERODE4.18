package it.imt.erode.partitionrefinement.algorithms.booleannetworks;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.ui.console.MessageConsoleStream;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Sort;
import com.microsoft.z3.Status;
import com.microsoft.z3.Symbol;
import com.microsoft.z3.Z3Exception;

import it.imt.erode.auxiliarydatastructures.DoubleAndStatus;
import it.imt.erode.auxiliarydatastructures.PartitionAndString;
import it.imt.erode.auxiliarydatastructures.SolverAndStatus;
import it.imt.erode.booleannetwork.auxiliarydatastructures.BNandPartition;
import it.imt.erode.booleannetwork.implementations.BooleanNetwork;
import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;
import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.implementations.Species;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.label.EmptySetLabel;
import it.imt.erode.crn.label.ILabel;
import it.imt.erode.importing.z3Importer;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.CRNBisimulationsNAry;
import it.imt.erode.partitionrefinement.algorithms.DifferentialSpeciesBisimilarity;
import it.imt.erode.partitionrefinement.algorithms.SyntacticMarkovianBisimilarityOLD;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SplittersGenerator;

public class SMTBackwardBooleanEquivalence {
	
	private HashMap<String, String> cfg;
	//private BoolExpr positivePopulationsAssertion;
	//private BoolExpr allConstraintAssertion;
	private Context ctx;
	private /*static*/ Solver solver;
	boolean initialized=false;
	private HashMap<ISpecies, BoolExpr> speciesToODENames;
	private HashMap<ISpecies, BoolExpr> speciesToODEsDef;
	private BoolExpr allODEsDef;
	
	public static final boolean SHOWTIMEATEACHSTEP = false;
	public static final boolean DOONLYCHECKSWITHWHOLEPARTITION = false;
	
	private double totalSMTChecksSeconds=0.0;
	private double initSMTTime=0.0;
	private List<Double> smtChecksSecondsAtStep;
	public List<Double> getSMTChecksSecondsAtStep(){
		return smtChecksSecondsAtStep;
	}
	
	//private MathEval math;
	private HashMap<ISpecies, BoolExpr> speciesToPopulation;
	//private HashMap<String, BoolExpr> symbParNameToSymbParZ3;	

	/**
	 * 
	 * @param bn
	 * @param partition the partition to be refined. It is not modified, but instead a new one is created and returned
	 * @param verbose
	 * @param terminator 
	 * @param messageDialogShower 
	 * @return
	 * @throws Z3Exception 
	 * @throws IOException 
	 */
	public PartitionAndString computeBBEsmt(IBooleanNetwork bn, IPartition partition, boolean verbose,MessageConsoleStream out, BufferedWriter bwOut, boolean printInfo, Terminator terminator, IMessageDialogShower messageDialogShower) throws Z3Exception, IOException{

		//checkBDE(crn, partition, verbose,out,printInfo,terminator, messageDialogShower);
		
		/*
		(define-fun absolute ((x Int)) Int
		(ite (>= x 0) x (- x)))
		
		(define-fun myMin ((x Real) (y Real)) Real
		  (ite (< x y) x y))
		 */
		

		
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"Boolean BDE Reducing: "+bn.getName()+" using Microsoft z3");
		}

		IPartition obtainedPartition = partition.copy();

		if(verbose){
			String blocks = " blocks";
			if(obtainedPartition.size()==1){
				blocks = " block";
			}
			CRNReducerCommandLine.println(out,bwOut,"Before partitioning we have "+ obtainedPartition.size() + blocks +" and "+bn.getSpecies().size()+ " species.");
		}

		long begin = System.currentTimeMillis();
		long beginInit = System.currentTimeMillis();
		init(bn,verbose,out,bwOut,terminator);
		long endInit = System.currentTimeMillis();
		initSMTTime = (double)(endInit-beginInit) / 1000.0;
		
		if(SHOWTIMEATEACHSTEP){	
			CRNReducerCommandLine.println(out,bwOut,"Init requred: "+String.format(CRNReducerCommandLine.MSFORMAT,String.valueOf(initSMTTime))+" (s)");
		}
		
		int iteration=0;
		Model model=null;
		
		//I first compute a few steps where z3 is able to refine many blocks at once. Then, after this z3 starts refinining a block at time. And thus I simplify the formula focusing on splitting one block at time.
		// FIRST PHASE
		//CRNReducerCommandLine.println(out,bwOut,"First phase");
		
		int prevPartizionSize;
		do{
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			prevPartizionSize = obtainedPartition.size();
			model = refinementIterationCheckingEFLAgainstWholePartition(bn, verbose,obtainedPartition, iteration,out,bwOut,terminator,messageDialogShower);
			iteration++;
		//Stop if model =null, as we already reached the EFL, or if in this iteration we obtained only 1 or 2 blocks more.	
		}while(model!=null && (DOONLYCHECKSWITHWHOLEPARTITION || (obtainedPartition.size() - prevPartizionSize > 2)) );		
		
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"After the first phase we have: " + obtainedPartition.size()+" blocks. (current time: " + (double)(System.currentTimeMillis() - begin)/1000.0+")");
		}
		
		if(model!=null){
			// SECOND PHASE
			//At each iteration consider a block only
			//CRNReducerCommandLine.println(out,bwOut,"Second phase");
			
			List<ILabel> labels=new ArrayList<>();
			labels.add(EmptySetLabel.EMPTYSETLABEL);
			//generate candidate splitters
			//SplittersGenerator splittersGenerator = obtainedPartition.getOrCreateSplitterGenerator(labels);
			SplittersGenerator splittersGenerator = obtainedPartition.createSplitterGenerator(labels);
			int loop =-1;
			do{
				if(Terminator.hasToTerminate(terminator)){
					break;
				}
				loop++;
				prevPartizionSize = obtainedPartition.size();
				splittersGenerator.reset(obtainedPartition.getFirstBlock());
				while(splittersGenerator.hasSplittersToConsider()){
					if(Terminator.hasToTerminate(terminator)){
						break;
					}
					IBlock blockSPL = splittersGenerator.getBlockSpl();
					if(blockSPL.getSpecies().size()==1){
						splittersGenerator.generateNextSplitter();
					}
					else{
						SolverAndStatus ss = checkIfIsEFLUsingZ3(blockSPL,bn, bn.getName(), obtainedPartition, verbose,iteration,out,bwOut,terminator,messageDialogShower);
						if(ss.getStatus()==Status.UNKNOWN){
							CRNReducerCommandLine.println(out,bwOut,"z3 returned unknown at iteration "+iteration+". This is the reason:");
							CRNReducerCommandLine.println(out,bwOut,ss.getSolver().getReasonUnknown());
							throw new Z3Exception("z3 returned unknown at iteration "+iteration+". This is the reason: "+ss.getSolver().getReasonUnknown());
						}
						else if(ss.getStatus()==Status.UNSATISFIABLE){
							model = null;
							splittersGenerator.generateNextSplitter();
						}
						else{
							model = ss.getSolver().getModel();
							//model.eval(speciesToODEs.get(null), true);//What is the second argument?
							//Now I have to refine the partition according to the obtained model
							HashSet<IBlock> splittedBlocks = new HashSet<IBlock>();
							boolean blockOfSPlitterHasBeenSplitted = partitionBlocksAccordingToz3Model(obtainedPartition,bn,model,splittedBlocks,blockSPL,speciesToODENames);
							//Now I have to update the splitters according to the newly generated partition. I also reinitialize the bst of blocks which have been splitted, but that remain in the partition because not empty.
							//cleanPartitioAndGetNextSplitterIfNecessary(partition, splittersGenerator, splittedBlocks, compositesPartition,refineUpBlocksAfterAnySplit);
							//MarkovianSpeciesBisimilarity.cleanPartitioAndGetNextSplitterIfNecessaryFocusingOnTheCurrentCompBlockOnly(obtainedPartition, null, splittedBlocks, null,null,iteration);
							DifferentialSpeciesBisimilarity.cleanPartitioAndGetNextSplitterIfNecessary(obtainedPartition, splittersGenerator, blockSPL, splittedBlocks,blockOfSPlitterHasBeenSplitted);
						}
						iteration++;
					}
				}
				if(verbose){
					CRNReducerCommandLine.println(out,bwOut,"At loop "+loop+" of the second phase we have "+obtainedPartition.size()+" blocks. (current time: " + (double)(System.currentTimeMillis() - begin)/1000.0+")");
				}
				if(obtainedPartition.size()==prevPartizionSize){
					model=null;
				}
				else{
					model = refinementIterationCheckingEFLAgainstWholePartition(bn, verbose,obtainedPartition, iteration,out,bwOut,terminator,messageDialogShower);
					iteration++;
					if(verbose){
						CRNReducerCommandLine.println(out,bwOut,"At loop "+loop+" of the second phase we have "+obtainedPartition.size()+" blocks after whole split. (current time: " + (double)(System.currentTimeMillis() - begin)/1000.0+")");
					}
				}
			//}while(obtainedPartition.size()!=prevPartizionSize && model!=null);
			}while(model!=null);
		}
		CRNReducerCommandLine.print(out,bwOut," ( "+iteration+" iterations) ");
		dispose();

		//CRNReducerCommandLine.print(out," ("+iteration+" iterations. Total SMT init time: "+initSMTTime+", total SMT check time: "+totalSMTChecksSeconds+" (s) )");
		/*if(printInfo){
			CRNReducerCommandLine.print(out," (Total SMT init time: "+String.format( CRNReducerCommandLine.MSFORMAT, (initSMTTime))+", total SMT check time: "+String.format( CRNReducerCommandLine.MSFORMAT, (totalSMTChecksSeconds))+" (s) )");
		}*/
		String smtTimes = "\tSMT init time: "+String.format( CRNReducerCommandLine.MSFORMAT, (initSMTTime))+" (s)\n\tSMT check time: "+ String.format( CRNReducerCommandLine.MSFORMAT, (totalSMTChecksSeconds))+" (s)";

		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"The final partition:");
			CRNReducerCommandLine.println(out,bwOut,obtainedPartition);
		}

		if(verbose){
			long end = System.currentTimeMillis();
			CRNReducerCommandLine.println(out,bwOut,"Boolean BDE Partitioning completed. From "+ bn.getSpecies().size() +" species to "+ obtainedPartition.size() + " blocks. Time necessary: "+(end-begin)+ " (ms)");
			CRNReducerCommandLine.println(out,bwOut,"");
		}
		return new PartitionAndString(obtainedPartition,smtTimes);
	}

	public static BNandPartition computeReducedBBE(IBooleanNetwork bn, String name, IPartition partition,String commentSymbol,MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator) {
		IBooleanNetwork reducedBN = new BooleanNetwork(name, out, bwOut);
		
		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(bn.getSpecies().size());
		for (ISpecies species : bn.getSpecies()) {
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			speciesNameToSpecies.put(species.getName(), species);
		}
		
		IBlock uniqueBlock = new Block();
		IPartition trivialPartition = new Partition(uniqueBlock,partition.size());
		
		ISpecies[] representativeSpecies = CRNBisimulationsNAry.getSortedBlockRepresentatives(partition, terminator);
		
		LinkedHashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies = new LinkedHashMap<IBlock, ISpecies>(partition.size());
		
		for(int i=0;i<representativeSpecies.length;i++) {
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			ISpecies blockRepresentative = representativeSpecies[i];
			IBlock currentBlock = partition.getBlockOf(blockRepresentative);
			/*
			ISpecies blockRepresentative = currentBlock.getRepresentative(CRNReducerCommandLine.COMPUTEREPRESENTATIVEBYMINOUTSIDEPARTITIONREFINEMENT);
			 */
			String nameRep=blockRepresentative.getName();
			//BigDecimal ic = blockRepresentative.getInitialConcentration();

			
			ISpecies reducedSpecies;
			
			reducedSpecies = new Species(nameRep, blockRepresentative.getOriginalName(),i, BigDecimal.ZERO,"0",blockRepresentative.getNameAlphanumeric(),false);
			reducedBN.addSpecies(reducedSpecies);
			
			reducedSpecies.addCommentLines(currentBlock.computeBlockComment());
			uniqueBlock.addSpecies(reducedSpecies);
			
			correspondenceBlock_ReducedSpecies.put(currentBlock, reducedSpecies);
		}
		
		for(Entry<IBlock, ISpecies> entry :correspondenceBlock_ReducedSpecies.entrySet()) {
			ISpecies reducedSpecies = entry.getValue();
			IUpdateFunction updateFunctionOfRep = bn.getUpdateFunctions().get(reducedSpecies.getName());
			IUpdateFunction reducedUpdateFunction = updateFunctionOfRep.cloneReplacingWithRepresentative(partition,correspondenceBlock_ReducedSpecies,speciesNameToSpecies);
			reducedBN.addUpdateFunction(reducedSpecies.getName(), reducedUpdateFunction);
		}
		
		return new BNandPartition(reducedBN, trivialPartition);
	}

	private void init(IBooleanNetwork bn,boolean verbose,MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator) throws Z3Exception, IOException {
		
		smtChecksSecondsAtStep=new ArrayList<Double>();
		totalSMTChecksSeconds=0.0;
		initSMTTime=0.0;
		
		cfg = new HashMap<String, String>();
		cfg.put("model", "true");
		//cfg.put("NLSAT", "true");//NLSAT=true
		ctx = new Context(cfg);
		solver = ctx.mkSolver();
		//positivePopulationsAssertion = ctx.mkTrue();

		//math = new MathEval();
		
		if(SHOWTIMEATEACHSTEP){
			CRNReducerCommandLine.println(out,bwOut,"");
		}
		/*BoolExpr file = ctx.ParseSMTLIB2File("./z3Encodings/e2EFLCompactAssert.z3", null, null, null,null);
		Solver s = ctx.mkSolver();
		s.add(file);
		check(s);*/
		
		Sort boolSort = ctx.mkBoolSort();
		

		
		/*Declare a positive real constant per species
		(declare-const s0 Real)
		(assert (> s0 0.0))*/
		/*Declare one ODE per species. 
        (declare-const ds0 Real)
		 */
		speciesToPopulation = new HashMap<>(bn.getSpecies().size());
		speciesToODENames = new HashMap<>(bn.getSpecies().size());
		speciesToODEsDef = new HashMap<>(bn.getSpecies().size());
		
		//ArithExpr zero = ctx.mkReal("0.0");
		//BoolExpr[] positivePopulationAssertions = new BoolExpr[crn.getSpecies().size()];
		//int s=0;
		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<>(bn.getSpecies().size());
		for (ISpecies species : bn.getSpecies()) {
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			speciesNameToSpecies.put(species.getName(), species);
			//create the z3 constants for the populations
			String speciesNameInZ3 = z3Importer.nameInZ3(species);
			Symbol declPopulation = ctx.mkSymbol(speciesNameInZ3);
			BoolExpr population = (BoolExpr) ctx.mkConst(declPopulation,boolSort);
			speciesToPopulation.put(species,population);
			//BoolExpr positivePop = ctx.mkGt(population, zero);
			//positivePopulationsAssertion = ctx.mkAnd(new BoolExpr[] { positivePop, positivePopulationsAssertion });
			//positivePopulationAssertions[s] = positivePop;
			//s++;
			//Create the z3 constants for the ode vars
			String odeVar = z3Importer.odeVarNameInZ3(species);
			Symbol declNames=ctx.mkSymbol(odeVar);
			//decls=ctx.mkConstDecl(declNames,ctx.mkRealSort());
			
			
			BoolExpr ode = (BoolExpr) ctx.mkConst(declNames,boolSort);
			speciesToODENames.put(species, ode);
			
			//speciesToODEsDef.put(species, falseZ3);
		}
		//positivePopulationsAssertion = ctx.mkAnd(positivePopulationAssertions);
		
		/*symbParNameToSymbParZ3 = new HashMap<>(crn.getSymbolicParameters().size());
		for(String symbPar : crn.getSymbolicParameters()){
			Symbol symbParZ3Decl = ctx.mkSymbol(symbPar);
			ArithExpr symbParZ3 = (ArithExpr) ctx.mkConst(symbParZ3Decl,boolSort);
			symbParNameToSymbParZ3.put(symbPar, symbParZ3);
		}*/
		
		/*
		int c=0;
		if(crn.getConstraints().size()>0){
			BoolExpr[] constraintAssertions = new BoolExpr[crn.getConstraints().size()];
			for(IConstraint constraintOnSymbPar : crn.getConstraints()){
				constraintAssertions[c] = constraintOnSymbPar.toZ3(ctx,symbParNameToSymbParZ3,crn, speciesNameToSpecies, speciesToPopulation);
				c++;
			}
			allConstraintAssertion = ctx.mkAnd(constraintAssertions);
		}
		else{
			allConstraintAssertion=ctx.mkTrue();
		}
		
		
		solver.add(positivePopulationAssertions);
		solver.add(allConstraintAssertion);
		Status status = solver.check();
		if(status==Status.UNKNOWN){
			CRNReducerCommandLine.println(out,bwOut,"z3 returned unknown while validating the constraints. This is the reason:");
			CRNReducerCommandLine.println(out,bwOut,solver.getReasonUnknown());
			throw new Z3Exception("z3 returned unknown while validating the constraints. This is the reason:"+solver.getReasonUnknown());
		}
		else if(status==Status.UNSATISFIABLE){
			String message = "The constraints on the symbolic parameters are not satifiable.";
			//CRNReducerCommandLine.printWarning(out, true, msgDialogShower, message, DialogType.Error);
			throw new Z3Exception(message);
		}
		*/
		
		/* Provide the body of each ODE
		 * (assert (= ds0 (+ 0.0  (* 6.0  s0 -1) (* 2.0  s0 -1))))
		 */
		long beginODESBPopulation = System.currentTimeMillis();
		BoolExpr[] allODEsDefArray = new BoolExpr[bn.getSpecies().size()];
		int j=0;
		for (Entry<String, IUpdateFunction> entry : bn.getUpdateFunctions().entrySet()) {
			ISpecies species = speciesNameToSpecies.get(entry.getKey());
			IUpdateFunction updateFunction = entry.getValue();
			BoolExpr updateFunctionZ3 = updateFunction.toZ3(ctx, bn, speciesNameToSpecies, speciesToPopulation);
			updateFunctionZ3=(BoolExpr)updateFunctionZ3.simplify();
			speciesToODEsDef.put(species, updateFunctionZ3);
			allODEsDefArray[j]=ctx.mkEq(speciesToODENames.get(species), updateFunctionZ3);
			j++;
		}
		//computeODEs(bn,terminator);
		allODEsDef = ctx.mkAnd(allODEsDefArray);
		long endODESBPopulation = System.currentTimeMillis();
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"\nIterating the species to 1) convert the update functions in Z3 format and 2) build the assertions to connect the left-hand-side and the right-hand-side (x_i(t+1) = B(t)) required: "+String.format(CRNReducerCommandLine.MSFORMAT,String.valueOf(((double)(endODESBPopulation-beginODESBPopulation) / 1000.0)))+" (s)");
		}
			
		/*
		long beginODEDefAssertions = System.currentTimeMillis();
		BoolExpr[] allODEsDefArray = new BoolExpr[bn.getSpecies().size()];
		
		for(int j =0;j<allODEsDefArray.length;j++){
			ISpecies species = bn.getSpecies().get(j);
			//It is worth doing this: it requires much more time to build the expressions (e.g. from less than a second to 150 secodns, but then the checks are faster, especially for the whole partition case )
			BoolExpr body = (BoolExpr)speciesToODEsDef.get(species).simplify();
			speciesToODEsDef.put(species,body);
			allODEsDefArray[j]=ctx.mkEq(speciesToODENames.get(species), body);
			
		}
		allODEsDef = ctx.mkAnd(allODEsDefArray);
		
		long endODEDefAssertions = System.currentTimeMillis();
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"Building the assertions for the ODEs definitions required: "+String.format(CRNReducerCommandLine.MSFORMAT,String.valueOf(((double)(endODEDefAssertions-beginODEDefAssertions) / 1000.0)))+" (s)");
		}
		*/
		
		initialized=true;
	}
	

	
	

	protected static boolean partitionBlocksAccordingToz3Model(IPartition partition, IBooleanNetwork crn, Model model, Collection<IBlock> splittedBlocks, IBlock blockSPL, HashMap<ISpecies, BoolExpr> speciesToSpliitingExpression) throws Z3Exception {
 
		boolean blockSPLHasBeenSplit=false;

		//TODO: check if it now works
		while(!blockSPL.getSpecies().isEmpty()){
			blockSPLHasBeenSplit=true;
			ISpecies species = blockSPL.getSpecies().iterator().next();
			Expr expr=null;
			//double val=0;
			boolean val=false;

			expr = model.eval(speciesToSpliitingExpression.get(species), false);
			val = Boolean.valueOf(expr.toString());
			//val = math.evaluate(expr.toString());
			//CRNReducerCommandLine.println(out,bwOut,val);
			
			
			BigDecimal valBD = (val)? BigDecimal.ONE:BigDecimal.ZERO;
			partition.splitBlock(splittedBlocks, species,blockSPL, valBD);
			//Now I am sure that a species has been removed from the block 
		}
		return blockSPLHasBeenSplit;
	}
	
	protected static void partitionBlocksAccordingToz3Model(IPartition partition, IBooleanNetwork crn, Model model, Collection<IBlock> splittedBlocks, HashMap<ISpecies, BoolExpr> speciesToSpliitingExpression) throws Z3Exception {
		
		//for (ISpecies species : blockSPL.getSpecies()) {
		for(ISpecies species : crn.getSpecies()){
			Expr expr=null;
			//double val=0;
			boolean val = false;

			expr = model.eval(speciesToSpliitingExpression.get(species), false);
			val = Boolean.valueOf(expr.toString()); 
			//val = math.evaluate(expr.toString());
			
			BigDecimal valBD = (val)? BigDecimal.ONE:BigDecimal.ZERO;

			partition.splitBlockOnlyIfKeyIsNotZero(splittedBlocks, species,null, valBD);
		}
	}
	
	
	public SolverAndStatus checkIfIsEFLUsingZ3(IBlock blockSPL, IBooleanNetwork bn, String name, IPartition partition, boolean verbose, int iteration,MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator, IMessageDialogShower msgDialogShower) throws Z3Exception, IOException{

		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"Checking if " + name +  " is Boolean BDE using z3");
		}

		if(!initialized){
			init(bn,verbose,out,bwOut,terminator);
		}

		solver.reset();
		//solver.add(positivePopulationsAssertion);
		//solver.add(allConstraintAssertion);

		for (ISpecies species : blockSPL.getSpecies()) {
			BoolExpr odeDef = ctx.mkEq(speciesToODENames.get(species), speciesToODEsDef.get(species));
			solver.add(odeDef);
		}

		//EFL-SPECIFIC CODE TWICE: it is specific for EFL, and it has be redone at anu iteration 
		//Write the question for OFL, EFL, of the current partition
		
		computAssertionToCheckEFLOnCurrentPartitionOneBlock(bn, partition, blockSPL, solver);
		
		DoubleAndStatus timeAndStatus = check(solver,verbose,SHOWTIMEATEACHSTEP,iteration,partition.size(),out,bwOut);
		totalSMTChecksSeconds+= timeAndStatus.getDouble();
		smtChecksSecondsAtStep.add(timeAndStatus.getDouble());
		//solver.Push();check(solver);//VERY BAD PERFORMANCES WITH PUSH
		return new SolverAndStatus(solver,timeAndStatus.getStatus());
		
	}

	private void dispose() throws Z3Exception{
		if(ctx!=null){
			//ctx.dispose();
			ctx.close();
		}
		////positivePopulationsAssertion.dispose();
		////allConstraintAssertion.dispose();
		//solver.dispose();
		
		initialized=false;
	}
	

	

	
	protected static DoubleAndStatus check(Solver solver, boolean verbose, boolean showTime, int iteration, int partitionSize,MessageConsoleStream out, BufferedWriter bwOut) throws Z3Exception{
		long begin = System.currentTimeMillis();
		//CRNReducerCommandLine.println(out,bwOut,solver);
		Status status = solver.check();
		long end = System.currentTimeMillis();
		double runtimeCheck = (double)(end-begin) / 1000.0;
		//totalSMTChecksSeconds+=runtimeCheck;
		if(showTime){
			CRNReducerCommandLine.println(out,bwOut,"Check at iteration "+iteration+" with "+partitionSize+" blocks requred: "+String.format(CRNReducerCommandLine.MSFORMAT,String.valueOf(runtimeCheck))+" (s)");
		}
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"The formula is: "+status.name());
		}
		//CRNReducerCommandLine.println(out,bwOut,"\n\n");
		if (verbose && status == Status.SATISFIABLE){
			CRNReducerCommandLine.println(out,bwOut,"\n\nThis is the returned model:");
			CRNReducerCommandLine.println(out,bwOut,solver.getModel());
			CRNReducerCommandLine.println(out,bwOut,"\n\n");
		}
		return new DoubleAndStatus(status, runtimeCheck);
	}

		
	

	private Model refinementIterationCheckingEFLAgainstWholePartition(IBooleanNetwork bn,
			boolean verbose, IPartition obtainedPartition, int iteration,MessageConsoleStream out, BufferedWriter bwOut,Terminator terminator, IMessageDialogShower messageDialogShower) throws Z3Exception, IOException {
		Model model=null;
		SolverAndStatus ss = checkIfIsEFLUsingZ3WholePartition(bn, bn.getName(), obtainedPartition, verbose,iteration,out,bwOut,terminator,messageDialogShower);
		if(ss.getStatus()==Status.UNKNOWN){
			CRNReducerCommandLine.println(out,bwOut,"z3 returned unknown at iteration "+iteration+". This is the reason:");
			CRNReducerCommandLine.println(out,bwOut,ss.getSolver().getReasonUnknown());
			//System.exit(-1);
			throw new Z3Exception("z3 returned unknown at iteration "+iteration+". This is the reason: "+solver.getReasonUnknown());
			//return null;
		}
		else if(ss.getStatus()==Status.UNSATISFIABLE){
			model = null;
		}
		else{
			model = ss.getSolver().getModel();
			//model.eval(speciesToODEs.get(null), true);//What is the second argument?
			//Now I have to refine the partition according to the obtained model
			HashSet<IBlock> splittedBlocks = new HashSet<IBlock>();
			partitionBlocksAccordingToz3Model(obtainedPartition,bn,model,splittedBlocks,speciesToODENames);
			//Now I have to update the splitters according to the newly generated partition. I also reinitialize the bst of blocks which have been splitted, but that remain in the partition because not empty.
			//cleanPartitioAndGetNextSplitterIfNecessary(partition, splittersGenerator, splittedBlocks, compositesPartition,refineUpBlocksAfterAnySplit);
			SyntacticMarkovianBisimilarityOLD.cleanPartitioAndGetNextSplitterIfNecessaryFocusingOnTheCurrentCompBlockOnly(obtainedPartition, null, splittedBlocks, null,null,iteration);
		}
		return model;
	}
	
	public SolverAndStatus checkIfIsEFLUsingZ3WholePartition(IBooleanNetwork bn, String name, IPartition partition, boolean verbose, int iteration,MessageConsoleStream out, BufferedWriter bwOut,Terminator terminator, IMessageDialogShower msgDialogShower) throws Z3Exception, IOException{

		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"Checking if " + name +  " is Boolean BDE using z3");
		}

		if(!initialized){
			init(bn,verbose,out,bwOut,terminator);
		}

		solver.reset();
		//solver.add(positivePopulationsAssertion);
		//solver.add(allConstraintAssertion);
		
		solver.add(allODEsDef);
		
		//EFL-SPECIFIC CODE TWICE: it is specific for EFL, and it has be redone at each iteration 
		//Write the question for OFL, EFL of the current partition
		
		computAssertionToCheckEFLOnCurrentWholePartition(bn, partition, solver);
		
		DoubleAndStatus timeAndStatus = check(solver,verbose,SHOWTIMEATEACHSTEP,iteration,partition.size(),out,bwOut);
		totalSMTChecksSeconds+= timeAndStatus.getDouble();
		smtChecksSecondsAtStep.add(timeAndStatus.getDouble());
		//solver.Push();check(solver);//VERY BAD PERFORMANCES WITH PUSH
		return new SolverAndStatus(solver,timeAndStatus.getStatus());
			
	}
	
	private void computAssertionToCheckEFLOnCurrentWholePartition(IBooleanNetwork bn,
			IPartition partition, Solver solver) throws Z3Exception {
				
		IBlock currentBlock = partition.getFirstBlock();
		BoolExpr[] conditionsOfblocks = new BoolExpr[partition.size()];
		int b=0;
		BoolExpr z3True = ctx.mkTrue();
		while(currentBlock!=null){
			if(currentBlock.getSpecies().size()!=1){
				BoolExpr[] equalODEs = new BoolExpr[currentBlock.getSpecies().size()-1];
				int s=0;
				ISpecies rep = currentBlock.getRepresentative();
				BoolExpr popRep = speciesToPopulation.get(rep);
				BoolExpr odeNameRep = speciesToODENames.get(rep);
				for (ISpecies species : currentBlock.getSpecies()) {
					if(!species.equals(rep)){
						BoolExpr ic = ctx.mkEq(popRep, speciesToPopulation.get(species));
						solver.add(ic);
						equalODEs[s]= ctx.mkEq(odeNameRep, speciesToODENames.get(species));
						s++;
					}
				} 
				conditionsOfblocks[b]= ctx.mkAnd(equalODEs);
			}
			else{
				conditionsOfblocks[b] = z3True;
			}
			currentBlock=currentBlock.getNext();
			b++;
		}

		solver.add(ctx.mkNot(ctx.mkAnd(conditionsOfblocks)));
	}
	

	
	private void computAssertionToCheckEFLOnCurrentPartitionOneBlock(IBooleanNetwork bn,
			IPartition partition, IBlock blockSPL, Solver solver) throws Z3Exception {
		
		BoolExpr negationOfEFL = ctx.mkTrue();		
		IBlock currentBlock = partition.getFirstBlock();
		while(currentBlock!=null){
			if(currentBlock.getSpecies().size()!=1){
				boolean isBlockSPL = currentBlock.equals(blockSPL);
				BoolExpr[] equalODEs = null;
				if(isBlockSPL){
					equalODEs = new BoolExpr[currentBlock.getSpecies().size()-1];
				}
				int s=0;
				ISpecies rep = currentBlock.getRepresentative();
				BoolExpr popRep = speciesToPopulation.get(rep);
				BoolExpr odeNameRep = speciesToODENames.get(rep);
				for (ISpecies species : currentBlock.getSpecies()) {
					if(!species.equals(rep)){
						BoolExpr ic = ctx.mkEq(popRep, speciesToPopulation.get(species));
						solver.add(ic);
						if(isBlockSPL){
							equalODEs[s]= ctx.mkEq(odeNameRep, speciesToODENames.get(species));
						}
						s++;
					}
				}
				if(isBlockSPL){
					negationOfEFL= ctx.mkAnd(equalODEs);
				}
			}
			currentBlock=currentBlock.getNext();
		}

		solver.add(ctx.mkNot(negationOfEFL));
		
	}

		


}
