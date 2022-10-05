package it.imt.erode.partitionrefinement.algorithms;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.ui.console.MessageConsoleStream;
import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.ASTNode.Type;
import org.sbml.jsbml.text.parser.ParseException;

import com.microsoft.z3.ArithExpr;
import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Model;
import com.microsoft.z3.Quantifier;
import com.microsoft.z3.RealSort;
import com.microsoft.z3.Solver;
import com.microsoft.z3.Sort;
import com.microsoft.z3.Status;
import com.microsoft.z3.Symbol;
import com.microsoft.z3.Z3Exception;

import it.imt.erode.auxiliarydatastructures.CRNandPartition;
import it.imt.erode.auxiliarydatastructures.CompositeAndBoolean;
import it.imt.erode.auxiliarydatastructures.DoubleAndStatus;
import it.imt.erode.auxiliarydatastructures.PartitionAndString;
import it.imt.erode.auxiliarydatastructures.SolverAndStatus;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.implementations.CRN;
import it.imt.erode.crn.implementations.CRNReactionArbitraryGUI;
import it.imt.erode.crn.implementations.CRNReactionArbitraryMatlab;
import it.imt.erode.crn.implementations.Species;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.label.EmptySetLabel;
import it.imt.erode.crn.label.ILabel;
import it.imt.erode.crn.symbolic.constraints.IConstraint;
import it.imt.erode.expression.evaluator.MathEval;
import it.imt.erode.importing.z3Importer;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.splittersbstandcounters.SplittersGenerator;

public class SMTMetrics {
	
	/**
	 * In the uk.ac.soton.crn implementation this is named: SMTExactFluidBisimilarity2
	 */
	
	private HashMap<String, String> cfg;
	private BoolExpr absPopulationDomainAssertion;
	private BoolExpr allConstraintAssertion;
	private Context ctx;
	private /*static*/ Solver solver;
	boolean initialized=false;
	private HashMap<ISpecies, ArithExpr> speciesToODENames;
	private HashMap<ISpecies, ArithExpr> speciesToODEsDef;
	private BoolExpr allODEsDef;
	//EREFL
	/*private BoolExpr allODEsFunDef;
	private Sort[] sortsOfodesAndSymbolicParameters;
	private Sort[] sortsOfodes;
	private Sort[] sortsOfSymbolicParameters;
	private Symbol[] namesSpeciesAndSymbolicParameters;
	private Symbol[] namesOfSpecies;
	private Symbol[] namesOfSymbolicParameters;
	private Expr[] invocationParameters;
	private HashMap<ISpecies, FuncDecl> speciesToODEFuncDecl;
	private HashMap<ISpecies, ArithExpr> speciesToODEInvocation;*/
	
	public static final boolean SHOWTIMEATEACHSTEP = false;
	public static final boolean DOONLYCHECKSWITHWHOLEPARTITION = false;
	
	private double totalSMTChecksSeconds=0.0;
	private double initSMTTime=0.0;
	private List<Double> smtChecksSecondsAtStep;
	public SMTMetrics(double C, double lambda) {
		this.C=C;
		this.lambda=lambda;
	}

	public List<Double> getSMTChecksSecondsAtStep(){
		return smtChecksSecondsAtStep;
	}
	
	private MathEval math;
	private HashMap<ISpecies, ArithExpr> speciesToPopulation;
	private HashMap<String, ArithExpr> symbParNameToSymbParZ3;
	
	private ArithExpr[][] dMetrics;
	private double C,lambda;
	private BoolExpr dMetricsDomainAssertion,absi_jLeDij,absdi_djLeDij;
	private ArithExpr[] populations;
	private RealSort[] populationsSorts;
	private Symbol[] declPopulations;
	public static final String METRICSFORMAT = "%.5f";
	
	protected static boolean partitionBlocksAccordingToz3Model(IPartition partition, ICRN crn, Model model, Collection<IBlock> splittedBlocks, IBlock blockSPL, HashMap<ISpecies, ArithExpr> speciesToSpliitingExpression, MathEval math) throws Z3Exception {
 
		boolean blockSPLHasBeenSplit=false;

		//TODO: check if it now works
		while(!blockSPL.getSpecies().isEmpty()){
			blockSPLHasBeenSplit=true;
			ISpecies species = blockSPL.getSpecies().iterator().next();
			Expr expr=null;
			double val=0;

			expr = model.eval(speciesToSpliitingExpression.get(species), false);
			val = math.evaluate(expr.toString());
			//CRNReducerCommandLine.println(out,bwOut,val);
			
			partition.splitBlock(splittedBlocks, species,blockSPL, BigDecimal.valueOf(val));
			//Now I am sure that a species has been removed from the block 
		}
		return blockSPLHasBeenSplit;
	}
	
	protected void partitionBlocksAccordingToz3Model(IPartition partition, ICRN crn, Model model, Collection<IBlock> splittedBlocks, HashMap<ISpecies, ArithExpr> speciesToSpliitingExpression, MathEval math,
			MessageConsoleStream out, BufferedWriter bwOut) throws Z3Exception {
		
		for(int i=0;i<crn.getSpeciesSize();i++) {
			for(int j=0;j<crn.getSpeciesSize();j++) {
				Expr expr = model.eval(dMetrics[i][j], false);
				double val = math.evaluate(expr.toString());
				String valStr= String.format(METRICSFORMAT,val);
				CRNReducerCommandLine.print(out, bwOut,valStr+" ");
			}
			CRNReducerCommandLine.println(out, bwOut);
		}
		
		CRNReducerCommandLine.println(out, bwOut);
		
		
//		//for (ISpecies species : blockSPL.getSpecies()) {
//		for(ISpecies species : crn.getSpecies()){
//			Expr expr=null;
//			double val=0;
//
//			expr = model.eval(speciesToSpliitingExpression.get(species), false);
//			try{
//			val = math.evaluate(expr.toString());
//			}catch(ArithmeticException e){
//				ArithmeticException ae = new ArithmeticException("Z3 had problems in evaluating an arithmetic expression. It returned an expression that could not be evaluated.\n"+e.getMessage());
//				ae.initCause(e);
//				ae.setStackTrace(e.getStackTrace());
//				throw ae;
//			}
//			partition.splitBlockOnlyIfKeyIsNotZero(splittedBlocks, species,null, BigDecimal.valueOf(val));
//		}
	}
	
	//EREFL
	/*public PartitionAndString checkBDE(ICRN crn, IPartition partition, boolean verbose,MessageConsoleStream out, BufferedWriter bwOut, boolean printInfo, Terminator terminator, IMessageDialogShower messageDialogShower) throws Z3Exception, IOException{
		 //This method checks if a partition is BDE. If the model has symbolic parameters, then it will be checked if there exists an assignment for the symbolic parameters that satisfies the constraints on them such that the partition to be checked is BDE.
		 // @param crn the model
		 // @param partition the partition to be checked
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"Checking BDE condition on "+crn.getName());
		}
		
		IPartition obtainedPartition = partition;
		
		if(verbose){
			String blocks = " blocks";
			if(obtainedPartition.size()==1){
				blocks = " block";
			}
			CRNReducerCommandLine.println(out,bwOut,"The input partition has "+ obtainedPartition.size() + blocks +" out of "+crn.getSpecies().size()+ " species.");
		}
		
		long begin = System.currentTimeMillis();
		long beginInit = System.currentTimeMillis();
		init(crn,verbose,out,bwOut,terminator);
		long endInit = System.currentTimeMillis();
		initSMTTime = (double)(endInit-beginInit) / 1000.0;
		
		if(SHOWTIMEATEACHSTEP){	
			CRNReducerCommandLine.println(out,bwOut,"Init requred: "+String.format(CRNReducerCommandLine.MSFORMAT,String.valueOf(initSMTTime))+" (s)");
		}
		
		Model model=null;
		
		solver.reset();
		solver.add(allODEsFunDef);
		computAssertionToCheckExistsRateSuchThatEFLOnCurrentWholePartition(crn, obtainedPartition, solver);
		
		DoubleAndStatus timeAndStatus = check(solver,verbose,SHOWTIMEATEACHSTEP,0,partition.size(),out,bwOut);
		totalSMTChecksSeconds+= timeAndStatus.getDouble();
		
		if(timeAndStatus.getStatus()==Status.UNKNOWN){
			CRNReducerCommandLine.println(out,bwOut,"z3 returned unknown at iteration "+0+". This is the reason:");
			CRNReducerCommandLine.println(out,bwOut,solver.getReasonUnknown());
			//System.exit(-1);
			throw new Z3Exception("z3 returned unknown at iteration "+0+". This is the reason: "+solver.getReasonUnknown());
			//return null;
		}
		else if(timeAndStatus.getStatus()==Status.UNSATISFIABLE){
			model = null;
		}
		else{
			model = solver.getModel();
			//model.eval(speciesToODEs.get(null), true);//What is the second argument?
			//Now I have to refine the partition according to the obtained model
			HashSet<IBlock> splittedBlocks = new HashSet<IBlock>();
			partitionBlocksAccordingToz3Model(obtainedPartition,crn,model,splittedBlocks,speciesToODENames,math);
			//Now I have to update the splitters according to the newly generated partition. I also reinitialize the bst of blocks which have been splitted, but that remain in the partition because not empty.
			//cleanPartitioAndGetNextSplitterIfNecessary(partition, splittersGenerator, splittedBlocks, compositesPartition,refineUpBlocksAfterAnySplit);
			SyntacticMarkovianBisimilarity.cleanPartitioAndGetNextSplitterIfNecessaryFocusingOnTheCurrentCompBlockOnly(obtainedPartition, null, splittedBlocks, null,null,0);
		}
		
		//solver.Push();check(solver);//VERY BAD PERFORMANCES WITH PUSH
		//return new SolverAndStatus(solver,timeAndStatus.getStatus());
		
		//
		//solver.add(positivePopulationsAssertion);
		//solver.add(allODEsFunDef);
		

			//if(Terminator.hasToTerminate(terminator)){
			//	break;
			//}
			//model = checkBDE(crn, verbose,obtainedPartition,out,bwOut,terminator,messageDialogShower);
		//Stop if model =null, as we already reached the EFL, or if in this iteration we obtained only 1 or 2 blocks more.			
		
		//if(verbose){
		//	CRNReducerCommandLine.println(out,bwOut,"After the first phase we have: " + obtainedPartition.size()+" blocks. (current time: " + (double)(System.currentTimeMillis() - begin)/1000.0+")");
		//}
		
		
		return null;
	}*/
	

	/**
	 * 
	 * @param crn
	 * @param partition the partition to be refined. It is not modified, but instead a new one is created and returned
	 * @param verbose
	 * @param terminator 
	 * @param messageDialogShower 
	 * @return
	 * @throws Z3Exception 
	 * @throws IOException 
	 */
	public double[][] computeZ3Metrics(ICRN crn, IPartition partition, boolean verbose,MessageConsoleStream out, BufferedWriter bwOut, boolean printInfo, Terminator terminator, IMessageDialogShower messageDialogShower) throws Z3Exception, IOException{

		//checkBDE(crn, partition, verbose,out,printInfo,terminator, messageDialogShower);
		
		/*(define-fun absolute ((x Int)) Int
		(ite (>= x 0) x (- x)))
		
		(define-fun myMin ((x Real) (y Real)) Real
		  (ite (< x y) x y))*/
		
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"Computing metrics for: "+crn.getName()+" using Microsoft z3");
		}

//		if(verbose){
//			String blocks = " blocks";
//			if(obtainedPartition.size()==1){
//				blocks = " block";
//			}
//			CRNReducerCommandLine.println(out,bwOut,"Before partitioning we have "+ obtainedPartition.size() + blocks +" and "+crn.getSpecies().size()+ " species.");
//		}

		long begin = System.currentTimeMillis();
		long beginInit = System.currentTimeMillis();
		init(crn,verbose,out,bwOut,terminator);
		long endInit = System.currentTimeMillis();
		initSMTTime = (double)(endInit-beginInit) / 1000.0;
		
		if(SHOWTIMEATEACHSTEP){	
			CRNReducerCommandLine.println(out,bwOut,"Init requred: "+String.format(CRNReducerCommandLine.MSFORMAT,String.valueOf(initSMTTime))+" (s)");
		}
		
		//I first compute a few steps where z3 is able to refine many blocks at once. Then, after this z3 starts refinining a block at time. And thus I simplify the formula focusing on splitting one block at time.
		// FIRST PHASE
		//CRNReducerCommandLine.println(out,bwOut,"First phase");
		
		double[][] metrics = computeAndParseMetrics(crn, verbose,out,bwOut,terminator,messageDialogShower);
		CRNReducerCommandLine.println(out,bwOut,"Time: " + (double)(System.currentTimeMillis() - begin)/1000.0+"");
		String smtTimes = "\tSMT init time: "+String.format( CRNReducerCommandLine.MSFORMAT, (initSMTTime))+" (s)\n\tSMT check time: "+ String.format( CRNReducerCommandLine.MSFORMAT, (totalSMTChecksSeconds))+" (s)";
		CRNReducerCommandLine.println(out,bwOut,smtTimes);
		
		if(metrics!=null){
			//
		}
		dispose();

		//CRNReducerCommandLine.print(out," ("+iteration+" iterations. Total SMT init time: "+initSMTTime+", total SMT check time: "+totalSMTChecksSeconds+" (s) )");
		/*if(printInfo){
			CRNReducerCommandLine.print(out," (Total SMT init time: "+String.format( CRNReducerCommandLine.MSFORMAT, (initSMTTime))+", total SMT check time: "+String.format( CRNReducerCommandLine.MSFORMAT, (totalSMTChecksSeconds))+" (s) )");
		}*/
		

		//return new PartitionAndString(obtainedPartition,smtTimes);
		return metrics;
	}

	private void dispose() throws Z3Exception{
		if(ctx!=null){
			//ctx.dispose();
			ctx.close();
		}
		//positivePopulationsAssertion.dispose();
		//allConstraintAssertion.dispose();
		//solver.dispose();
		
		//declNames=null;
		//decls=null;
		
		initialized=false;
	}
	
	/*protected static void checkNativeSources(Class<? extends Object> myClass) throws IOException {
		String p = System.getProperty("os.name");
		
		String libExtension = ".dylib";
		if(p.contains("Windows")){
			libExtension=".dll";
		}
		else if(p.contains("Linux")){
			libExtension=".so";
		}
		
		//libz3.a
		String fileName = "libz3"+libExtension;
		File f = new File(fileName);
		if(!f.exists()){
			String localNesting ="necessaryNativeSources"+File.separator;
			//copyFile("","buttonScaled.jpg");
			//copyFile(localNesting,"libz3.a");
			copyFile(localNesting,"libz3"+libExtension,myClass);
			copyFile(localNesting,"libz3java"+libExtension,myClass);
			if(p.contains("Windows")){
				copyFile(localNesting,"vcomp110.dll",myClass);
				copyFile(localNesting,"msvcr110.dll",myClass);
				copyFile(localNesting,"msvcp110.dll",myClass);
				
			}
		}
	}
	
	private static void copyFile(String localNesting,String relativePath,Class<? extends Object> myClass) throws IOException {
		
		URL sourceURL = myClass.getResource(localNesting+relativePath);
		InputStream is = sourceURL.openStream();
		
		Path targetPath = FileSystems.getDefault().getPath(relativePath);
		Files.copy(is, targetPath,StandardCopyOption.REPLACE_EXISTING);
		is.close();
	}*/
	
	private void init(ICRN crn,boolean verbose,MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator) throws Z3Exception, IOException {
		/*String property = System.getProperty("java.library.path");
		//System.out.println("\n"+property+"\n");
		CRNReducerCommandLine.println(out,bwOut, "\n"+property+"\n");*/
		/*//StringTokenizer parser = new StringTokenizer(property, ";");
		//while (parser.hasMoreTokens()) {
		//    System.err.println(parser.nextToken());
		//    }
		System.setProperty("java.library.path", ".:"+property);
		property = System.getProperty("java.library.path");
		System.out.println("\n"+property+"\n");*/
		//ClassLoader.getSystemClassLoader().clearAssertionStatus();
		//System.loadLibrary("z3java");
		//System.load("/Users/andrea/Copy/workspacextext/lib/z3/z3java.dylib");
		//System.load("/Users/andrea/Copy/workspacextext/lib/z3/libz3java.dylib");
		
		//System.loadLibrary("z3java");
		
		/*String fileName = "buttonScaled.jpg";
		File f = new File(fileName);
		//System.out.println(f.getAbsolutePath());
		CRNReducerCommandLine.println(out,bwOut, "\n"+f.getAbsolutePath()+"\n");*/
		
		/*try {
			checkNativeSources(getClass());
		} catch (IOException e) {
			CRNReducerCommandLine.printStackTrace(out, e);
		}*/
		
		smtChecksSecondsAtStep=new ArrayList<Double>();
		totalSMTChecksSeconds=0.0;
		initSMTTime=0.0;
		
		cfg = new HashMap<String, String>();
		cfg.put("model", "true");
		//cfg.put("NLSAT", "true");//NLSAT=true
		ctx = new Context(cfg);
		solver = ctx.mkSolver();
		//positivePopulationsAssertion = ctx.mkTrue();

		math = new MathEval();
		
		if(SHOWTIMEATEACHSTEP){
			CRNReducerCommandLine.println(out,bwOut,"");
		}
		/*BoolExpr file = ctx.ParseSMTLIB2File("./z3Encodings/e2EFLCompactAssert.z3", null, null, null,null);
		Solver s = ctx.mkSolver();
		s.add(file);
		check(s);*/
		
		Sort realSort = ctx.mkRealSort();
		
		//EREFL
		/*int speciesAndSymbolicParameters=crn.getSpecies().size()+crn.getSymbolicParameters().size();
		sortsOfodesAndSymbolicParameters = new Sort[speciesAndSymbolicParameters];
		sortsOfodes = new Sort[crn.getSpecies().size()];
		sortsOfSymbolicParameters = new Sort[crn.getSymbolicParameters().size()];
		for(int i=0;i<sortsOfodesAndSymbolicParameters.length;i++){
			sortsOfodesAndSymbolicParameters[i]=realSort;
			if(i<crn.getSpecies().size()){
				sortsOfodes[i]=realSort;
			}
			else{
				sortsOfSymbolicParameters[i-crn.getSpecies().size()]=realSort;
			}
		}
		invocationParameters=new Expr[speciesAndSymbolicParameters];
		namesSpeciesAndSymbolicParameters = new Symbol[speciesAndSymbolicParameters];
		namesOfSpecies = new Symbol[crn.getSpecies().size()];
		namesOfSymbolicParameters = new Symbol[crn.getSymbolicParameters().size()];
		int ip=0;
		speciesToODEFuncDecl = new HashMap<ISpecies, FuncDecl>(crn.getSpecies().size());
		speciesToODEInvocation = new HashMap<ISpecies, ArithExpr>(crn.getSpecies().size());*/
		
		/*Declare a positive real constant per species
		(declare-const s0 Real)
		(assert (> s0 0.0))*/
		/*Declare one ODE per species. 
        (declare-const ds0 Real)
		 */
		speciesToPopulation = new HashMap<ISpecies, ArithExpr>(crn.getSpecies().size());//s0-> s0
		speciesToODENames = new HashMap<ISpecies, ArithExpr>(crn.getSpecies().size());	//s0-> ds0
		speciesToODEsDef = new HashMap<ISpecies, ArithExpr>(crn.getSpecies().size());	//s0-> update function/derivative
		
		dMetrics = new ArithExpr[crn.getSpecies().size()][crn.getSpecies().size()];
		populations = new ArithExpr[crn.getSpecies().size()];
		populationsSorts = new RealSort[crn.getSpecies().size()];
		declPopulations = new Symbol[crn.getSpecies().size()];
		
		//declNames = new Symbol[crn.getSpecies().size() * 2];
		//decls = new FuncDecl[crn.getSpecies().size() * 2];
		//int i=0;
		ArithExpr zero = ctx.mkReal("0.0");
		ArithExpr Cz3= ctx.mkReal(C+"");
		ArithExpr lambdaz3= ctx.mkReal(lambda+"");
		//HashMap<ISpecies,StringBuilder> odeBodies = new HashMap<ISpecies, StringBuilder>(crn.getSpecies().size());
		BoolExpr[] positivePopulationAssertions = new BoolExpr[crn.getSpecies().size()];
		int s=0;
		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<>(crn.getSpecies().size());
		for (ISpecies species : crn.getSpecies()) {
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			speciesNameToSpecies.put(species.getName(), species);
			//create the z3 constants for the populations
			String speciesNameInZ3 = z3Importer.nameInZ3(species);
			Symbol declPopulation = ctx.mkSymbol(speciesNameInZ3);
			//FuncDecl decls = ctx.mkConstDecl(declNames,ctx.mkRealSort());
			ArithExpr population = (ArithExpr) ctx.mkConst(declPopulation,realSort);
			speciesToPopulation.put(species,population);
			populations[s]=population;
			populationsSorts[s]=(RealSort)realSort;
			declPopulations[s]=declPopulation;
			
			ArithExpr abs_population= (ArithExpr)ctx.mkITE(ctx.mkGe(population, zero), population, ctx.mkSub(new ArithExpr[]{zero,population}));
			BoolExpr positivePop = ctx.mkGe(abs_population, zero);
			BoolExpr smallerLambda = ctx.mkLe(abs_population, lambdaz3);
			//positivePopulationsAssertion = ctx.mkAnd(new BoolExpr[] { positivePop, positivePopulationsAssertion });
			positivePopulationAssertions[s] = ctx.mkAnd(positivePop,smallerLambda);
			s++;
			//i++;
			//Create the z3 constants for the ode vars
			String odeVar = z3Importer.odeVarNameInZ3(species);
			Symbol declNames=ctx.mkSymbol(odeVar);
			//decls=ctx.mkConstDecl(declNames,ctx.mkRealSort());
			
			//EREFL
			/*FuncDecl odeFuncDecl = ctx.mkFuncDecl(declNames, sortsOfodesAndSymbolicParameters, ctx.mkRealSort());
			speciesToODEFuncDecl.put(species, odeFuncDecl);
			invocationParameters[ip]=population;
			namesSpeciesAndSymbolicParameters[ip]=declPopulation;
			namesOfSpecies[ip]=declPopulation;
			ip++;*/
			
			ArithExpr ode = (ArithExpr) ctx.mkConst(declNames,realSort);
			speciesToODENames.put(species, ode);
			//i++;
			//Initialize the stringbuilders used to store the ode bodies
			//odeBodies.put(species, new StringBuilder("(assert (= "+ odeVar +" (+ 0.0 "));
			//species.setSB(new StringBuilder("(assert (= "+ odeVar +" (+ 0.0 "));
			
			speciesToODEsDef.put(species, zero);
		}
		absPopulationDomainAssertion = ctx.mkAnd(positivePopulationAssertions);
		
		BoolExpr[] domainDAssertions = new BoolExpr[crn.getSpecies().size()*crn.getSpecies().size()];
		BoolExpr[] absij_le_dij = new BoolExpr[crn.getSpecies().size()*crn.getSpecies().size()];
		BoolExpr[] absdidj_le_dij = new BoolExpr[crn.getSpecies().size()*crn.getSpecies().size()];
		int k=0;
		for (int i=0;i<crn.getSpeciesSize();i++) {
			ISpecies spi = crn.getSpecies().get(i);
			ArithExpr popi = speciesToPopulation.get(spi);
			ArithExpr odeNamei = speciesToODENames.get(spi); 
			for (int j=0;j<crn.getSpeciesSize();j++) {				
				Symbol dij_symbol = ctx.mkSymbol("d_"+i+"_"+j);
				ArithExpr dij = (ArithExpr) ctx.mkConst(dij_symbol,realSort);
				dMetrics[i][j]=dij;
				BoolExpr domain_ij = ctx.mkAnd(ctx.mkGe(dij, zero),ctx.mkLe(dij, Cz3));
				domainDAssertions[k]=domain_ij;		
				
				ISpecies spj = crn.getSpecies().get(j);
				ArithExpr popj = speciesToPopulation.get(spj);
				ArithExpr odeNamej = speciesToODENames.get(spj);
				ArithExpr iminusj = ctx.mkSub(new ArithExpr[]{popi,popj});
				ArithExpr abs_iminusj =(ArithExpr)ctx.mkITE(ctx.mkGe(iminusj, zero), iminusj, ctx.mkSub(new ArithExpr[]{zero,iminusj}));
				BoolExpr abs_ij_leLambda_ij = ctx.mkLe(abs_iminusj, dij);
				absij_le_dij[k]=abs_ij_leLambda_ij;
				
				
				iminusj = ctx.mkSub(new ArithExpr[]{odeNamei,odeNamej});
				abs_iminusj =(ArithExpr)ctx.mkITE(ctx.mkGe(iminusj, zero), iminusj, ctx.mkSub(new ArithExpr[]{zero,iminusj}));
				BoolExpr abs_didj_leLambda_ij = ctx.mkLe(abs_iminusj, dij);
				absdidj_le_dij[k]=abs_didj_leLambda_ij;
				
				k++;
			}
		}
		dMetricsDomainAssertion = ctx.mkAnd(domainDAssertions);
		absi_jLeDij=ctx.mkAnd(absij_le_dij);
		absdi_djLeDij=ctx.mkAnd(absdidj_le_dij);
				
		/* Provide the body of each ODE
		 * (assert (= ds0 (+ 0.0  (* 6.0  s0 -1) (* 2.0  s0 -1))))
		 */
		long beginODESBPopulation = System.currentTimeMillis();
		computeODEs(crn,terminator);
		//computeODEsSB(crn,"",odeBodies);
		long endODESBPopulation = System.currentTimeMillis();
		if(verbose){
			double time = ((double)(endODESBPopulation-beginODESBPopulation) / 1000.0);
			String formattedTime = String.format(CRNReducerCommandLine.MSFORMAT,time);
			CRNReducerCommandLine.println(out,bwOut,"\nIterating the reactions to build the update functions required: "+formattedTime+" (s)");
		}
			
		long beginODEDefAssertions = System.currentTimeMillis();
		BoolExpr[] allODEsDefArray = new BoolExpr[crn.getSpecies().size()];
		//EREFL
		//BoolExpr[] allODEsFuncDefArray = new BoolExpr[crn.getSpecies().size()];
		
		int j=0;
		for (ISpecies species : crn.getSpecies()) {
			//It is worth doing this: it requires much more time to build the expressions (e.g. from less than a second to 150 secodns, but then the checks are faster, especially for the whole partition case )
			ArithExpr body = (ArithExpr)speciesToODEsDef.get(species).simplify();
			//ArithExpr body = (ArithExpr)speciesToODEsDef.get(species);
			speciesToODEsDef.put(species,body);
			allODEsDefArray[j]=ctx.mkEq(speciesToODENames.get(species), body);
			
			//EREFL
			/*ArithExpr odeInvocation = (ArithExpr) speciesToODEFuncDecl.get(species).apply(invocationParameters);
			speciesToODEInvocation.put(species,odeInvocation);
			allODEsFuncDefArray[j] = ctx.mkEq(odeInvocation, body);*/
			
			j++;
		}
//		for(int j =0;j<allODEsDefArray.length;j++){
//			ISpecies species = crn.getSpecies().get(j);
//			//It is worth doing this: it requires much more time to build the expressions (e.g. from less than a second to 150 secodns, but then the checks are faster, especially for the whole partition case )
//			ArithExpr body = (ArithExpr)speciesToODEsDef.get(species).simplify();
//			//ArithExpr body = (ArithExpr)speciesToODEsDef.get(species);
//			speciesToODEsDef.put(species,body);
//			allODEsDefArray[j]=ctx.mkEq(speciesToODENames.get(species), body);
//			
//			//EREFL
//			/*ArithExpr odeInvocation = (ArithExpr) speciesToODEFuncDecl.get(species).apply(invocationParameters);
//			speciesToODEInvocation.put(species,odeInvocation);
//			allODEsFuncDefArray[j] = ctx.mkEq(odeInvocation, body);*/
//		}
		allODEsDef = ctx.mkAnd(allODEsDefArray);
		//EREFL
		//allODEsFunDef = ctx.mkAnd(allODEsFuncDefArray);
		
		long endODEDefAssertions = System.currentTimeMillis();
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"Building the assertions for the update functions / derivatives definitions required: "+String.format(CRNReducerCommandLine.MSFORMAT,(((double)(endODEDefAssertions-beginODEDefAssertions) / 1000.0)))+" (s)");
		}
		
		
		initialized=true;
	}
	

	public static String computez3RateExpressionString(ICRN crn, ICRNReaction reaction, String suffix, ISpecies[] speciesIdToSpecies, HashMap<String, ISpecies> speciesNameToSpecies, HashSet<String> symbolicParameters){
		String rateExpr = "";
		
		if((!reaction.hasArbitraryKinetics())){
			rateExpr = "(* "+reaction.getRate().toPlainString() + " " + reaction.getReagents().getSpaceSeparatedMultisetUsingIdsAsNames("s",suffix) +")" ;
		}
		else if(reaction instanceof CRNReactionArbitraryMatlab){
			CRNReactionArbitraryMatlab arb = (CRNReactionArbitraryMatlab)reaction;
			rateExpr = SMTMetrics.computeArbitraryz3RateExpressionString(arb.getRateLaw(),arb.getVarsName(),null,crn.getMath(),speciesIdToSpecies,suffix,symbolicParameters);
		}
		else if(reaction instanceof CRNReactionArbitraryGUI){
			CRNReactionArbitraryGUI arb = (CRNReactionArbitraryGUI)reaction;
			rateExpr = SMTMetrics.computeArbitraryz3RateExpressionString(arb.getRateLaw(),null,speciesNameToSpecies,crn.getMath(),null,suffix,symbolicParameters);
		}
		else{
			throw new UnsupportedOperationException("Unsupported reaction:"+reaction);
		}
		return rateExpr;
	}
	
	protected static ArithExpr computez3RateExpression(Context ctx, HashMap<ISpecies, ArithExpr> speciesToPopulation, HashMap<String, ArithExpr> symbParNameToSymbParZ3, ICRN crn, HashMap<IComposite, ArithExpr> massActionExpressions,
			ICRNReaction reaction,ISpecies[] speciesIdToSpecies,HashMap<String, ISpecies> speciesNameToSpecies) throws Z3Exception, IOException {
		ArithExpr rate;
		if((!reaction.hasArbitraryKinetics())){
			rate = SMTMetrics.getOrComputeProductOfReagents(ctx,massActionExpressions,reaction.getReagents(),speciesToPopulation);
			/*String rateSTR = reaction.getRate().toPlainString();
			rate = ctx.mkMul(new ArithExpr[]{rate , ctx.mkReal(rateSTR) });*/
			ASTNode rateLaw=null;
			try {
				rateLaw = ASTNode.parseFormula(reaction.getRateExpression());
			} catch (ParseException e) {
				throw new IOException("Problems while parsing the rate: "+reaction.getRateExpression()+"\n"+e.getMessage());
				//System.out.println(e.getMessage());
			}
			ArithExpr kinrate = SMTMetrics.computeArbitraryz3RateExpression(ctx,speciesToPopulation,symbParNameToSymbParZ3,rateLaw,null,speciesNameToSpecies,crn.getMath(),null);
			rate = ctx.mkMul(new ArithExpr[]{rate , kinrate });
		}
		/*else if(reaction.hasHillKinetics()){
			//I assume that HILL reactions are binary
			// x + xT -> x + x + xT, Hill K R1 R2 n1 n2
			// x + xT ->         xT, Hill K R1 R2 n1 n2
			if(!reaction.isBinary()){
				CRNReducerCommandLine.println(out,bwOut,"Only binary reactions are supported for Hill kinetics. ");
				System.exit(-1);
			}

			StringTokenizer st = new StringTokenizer(reaction.getRateExpression());
			st.nextToken();
			String K = st.nextToken();
			K = String.valueOf(crn.getMath().evaluate(K));
			String R1 = st.nextToken();
			R1 = String.valueOf(crn.getMath().evaluate(R1));
			String R2 = st.nextToken();
			R2 = String.valueOf(crn.getMath().evaluate(R2));
			String n1 = st.nextToken();
			int n1n = (int)crn.getMath().evaluate(n1);
			String n2 = st.nextToken();
			int n2n = (int) crn.getMath().evaluate(n2);


			ISpecies firstReagent = reaction.getReagents().getFirstReagent();
			ISpecies secondReagent = reaction.getReagents().getSecondReagent();

			ArithExpr num = ctx.mkMul(new ArithExpr[]{ ctx.mkReal(K) , ctx.mkPower(speciesToPopulation.get(firstReagent), ctx.mkInt(n1n)) , 
					                                                   ctx.mkPower(speciesToPopulation.get(secondReagent), ctx.mkInt(n2n)) } );
			ArithExpr denum = ctx.mkMul(new ArithExpr[]{ 
					ctx.mkAdd(new ArithExpr[]{ ctx.mkReal(R1),ctx.mkPower(speciesToPopulation.get(firstReagent), ctx.mkInt(n1n))}),
					ctx.mkAdd(new ArithExpr[]{ ctx.mkReal(R2),ctx.mkPower(speciesToPopulation.get(secondReagent), ctx.mkInt(n2n))})
			});
			rate = ctx.mkDiv(num, denum);
		}*/
		else if(reaction instanceof CRNReactionArbitraryMatlab){
			/*ISpecies[] speciesIdToSpecies = new ISpecies[crn.getSpecies().size()];
			crn.getSpecies().toArray(speciesIdToSpecies);*/
			CRNReactionArbitraryMatlab arb = (CRNReactionArbitraryMatlab)reaction;
			rate = SMTMetrics.computeArbitraryz3RateExpression(ctx,speciesToPopulation,symbParNameToSymbParZ3,arb.getRateLaw(),arb.getVarsName(),null,crn.getMath(),speciesIdToSpecies);
		}
		else if(reaction instanceof CRNReactionArbitraryGUI){
			/*HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
			for (ISpecies species : crn.getSpecies()) {
				speciesNameToSpecies.put(species.getName(), species);
			}*/
			CRNReactionArbitraryGUI arb = (CRNReactionArbitraryGUI)reaction;
			rate = SMTMetrics.computeArbitraryz3RateExpression(ctx,speciesToPopulation,symbParNameToSymbParZ3,arb.getRateLaw(),null,speciesNameToSpecies,crn.getMath(),null);
		} 
		else{
			throw new UnsupportedOperationException("Unsupported reaction:"+reaction);
		}
		return rate;
	}
	
	private void computeODEs(ICRN crn, Terminator terminator) throws Z3Exception, IOException {
		/*ISpecies[] speciesIdToSpecies = new ISpecies[crn.getSpecies().size()];
		//crn.getSpecies().toArray(speciesIdToSpecies);
		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
		int s=0;
		for (ISpecies species : crn.getSpecies()) {
			speciesIdToSpecies[s]=species;
			speciesNameToSpecies.put(species.getName(), species);
			s++;
		}*/
		
		ISpecies[] speciesIdToSpecies = new ISpecies[crn.getSpecies().size()];
		crn.getSpecies().toArray(speciesIdToSpecies);
		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
		for (ISpecies species : crn.getSpecies()) {
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			speciesNameToSpecies.put(species.getName(), species);
		}
		
		HashMap<IComposite, ArithExpr> massActionExpressions = new HashMap<IComposite, ArithExpr>(/*crn.getReagents().size()*/);
		for (ICRNReaction reaction : crn.getReactions()) {
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			ArithExpr rateExpr = computez3RateExpression(ctx, speciesToPopulation, symbParNameToSymbParZ3, crn, massActionExpressions, reaction,speciesIdToSpecies,speciesNameToSpecies);
			IComposite netStochimetry = reaction.computeProductsMinusReagents();
			for(int i=0;i<netStochimetry.getNumberOfDifferentSpecies();i++){
				ISpecies species = netStochimetry.getAllSpecies(i);
				int stoc = netStochimetry.getMultiplicities(i);
				ArithExpr fluxExpr = ctx.mkMul(new ArithExpr[]{ ctx.mkReal(stoc), rateExpr});

				ArithExpr odeDef = speciesToODEsDef.get(species);
				speciesToODEsDef.put(species, ctx.mkAdd(new ArithExpr[]{odeDef,fluxExpr}));
			}
		}
	}
	
	
	/*private void computeODEs(ICRN crn) throws Z3Exception {
		ISpecies[] speciesIdToSpecies = new ISpecies[crn.getSpecies().size()];
		crn.getSpecies().toArray(speciesIdToSpecies);

		HashMap<IComposite, ArithExpr> massActionExpressions = new HashMap<IComposite, ArithExpr>(crn.getReagents().size());
		for (ICRNReaction reaction : crn.getReactions()) {
			if((!reaction.hasHillKinetics()) && (!reaction.hasArbitraryKinetics())){
				ArithExpr massActionExpr = getOrComputeProductOfReagents(ctx,massActionExpressions,reaction.getReagents(),speciesToPopulation);
				String rate = reaction.getRate().toPlainString();
				massActionExpr = ctx.mkMul(new ArithExpr[]{massActionExpr , ctx.mkReal(rate) });
				IComposite netStochimetry = reaction.computeProductsMinusReagents();
				for(int i=0;i<netStochimetry.getAllSpecies().length;i++){
					ISpecies species = netStochimetry.getAllSpecies()[i];
					int stoc = netStochimetry.getMultiplicities()[i];
					ArithExpr fluxExpr = ctx.mkMul(new ArithExpr[]{ ctx.mkReal(stoc), massActionExpr});

					ArithExpr odeDef = speciesToODEsDef.get(species);
					speciesToODEsDef.put(species, ctx.mkAdd(new ArithExpr[]{odeDef,fluxExpr}));
				}
			}
			else if(reaction.hasHillKinetics()){
				//I assume that HILL reactions are binary
				// x + xT -> x + x + xT, Hill K R1 R2 n1 n2
				// x + xT ->         xT, Hill K R1 R2 n1 n2
				if(!reaction.isBinary()){
					CRNReducerCommandLine.println(out,bwOut,"Only binary reactions are supported for Hill kinetics. ");
					System.exit(-1);
				}
				
				StringTokenizer st = new StringTokenizer(reaction.getRateExpression());
				st.nextToken();
				String K = st.nextToken();
				K = String.valueOf(crn.getMath().evaluate(K));
				String R1 = st.nextToken();
				R1 = String.valueOf(crn.getMath().evaluate(R1));
				String R2 = st.nextToken();
				R2 = String.valueOf(crn.getMath().evaluate(R2));
				String n1 = st.nextToken();
				int n1n = (int)crn.getMath().evaluate(n1);
				String n2 = st.nextToken();
				int n2n = (int) crn.getMath().evaluate(n2);
				
				
				ISpecies firstReagent = reaction.getReagents().getFirstReagent();
				ISpecies secondReagent = reaction.getReagents().getSecondReagent();
				
				ArithExpr num = ctx.mkMul(new ArithExpr[]{ ctx.mkReal(K) , ctx.mkPower(speciesToPopulation.get(firstReagent), ctx.mkInt(n1n)) , 
						                                                   ctx.mkPower(speciesToPopulation.get(secondReagent), ctx.mkInt(n2n)) } );
				ArithExpr denum = ctx.mkMul(new ArithExpr[]{ 
						ctx.mkAdd(new ArithExpr[]{ ctx.mkReal(R1),ctx.mkPower(speciesToPopulation.get(firstReagent), ctx.mkInt(n1n))}),
						ctx.mkAdd(new ArithExpr[]{ ctx.mkReal(R2),ctx.mkPower(speciesToPopulation.get(secondReagent), ctx.mkInt(n2n))})
				});
				ArithExpr rate = ctx.mkDiv(num, denum);

				
				IComposite netStochimetry = reaction.computeProductsMinusReagents();
				for(int i=0;i<netStochimetry.getAllSpecies().length;i++){
					ISpecies species = netStochimetry.getAllSpecies()[i];
					int stoc = netStochimetry.getMultiplicities()[i];
					ArithExpr fluxExpr = ctx.mkMul(new ArithExpr[]{ ctx.mkReal(stoc), rate});

					ArithExpr odeDef = speciesToODEsDef.get(species);
					speciesToODEsDef.put(species, ctx.mkAdd(new ArithExpr[]{odeDef,fluxExpr}));
				}
			}
			else if(reaction.hasArbitraryKinetics()){
				ArithExpr rate = computez3Rate(ctx,speciesToPopulation,((CRNReactionArbitrary)reaction).getRateLaw(),((CRNReactionArbitrary)reaction).getVarsName(),crn.getMath(),speciesIdToSpecies);
				IComposite netStochimetry = reaction.computeProductsMinusReagents();
				for(int i=0;i<netStochimetry.getAllSpecies().length;i++){
					ISpecies species = netStochimetry.getAllSpecies()[i];
					int stoc = netStochimetry.getMultiplicities()[i];
					ArithExpr fluxExpr = rate;
					if(stoc!=1){
						fluxExpr = ctx.mkMul(new ArithExpr[]{ ctx.mkReal(stoc), rate});
					}
					ArithExpr odeDef = speciesToODEsDef.get(species);
					speciesToODEsDef.put(species, ctx.mkAdd(new ArithExpr[]{odeDef,fluxExpr}));
				}
			}
			else{
				throw new UnsupportedOperationException("Unsupported reaction:"+reaction);
			}
		}
	}*/
	
	private static String spaceSeparated(String[] childs){
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < childs.length; i++) {
			sb.append(childs[i]);
			sb.append(' ');
		}
		return sb.toString();
	}
	
	//protected static String computeArbitraryz3RateExpressionString(Context ctx, HashMap<ISpecies, ArithExpr> speciesToPopulation,HashMap<String, ArithExpr> symbParNameToSymbParZ3, ASTNode node,  String varsName, HashMap<String, ISpecies> speciesNameToSpecies, MathEval math, ISpecies[] speciesIdToSpecies,Stirng suffix) throws Z3Exception {
	protected static String computeArbitraryz3RateExpressionString(ASTNode node,  String varsName, HashMap<String, ISpecies> speciesNameToSpecies, MathEval math, ISpecies[] speciesIdToSpecies,String suffix,HashSet<String> symbolicParameters){
		//A variable is actually a parameter, while a function (e.g., varsName(1)) is an ODE variable (with id 0).
		if(node.isOperator() && node.getChildCount()>=2){
			Type type = node.getType();
			String[] childs = new String[node.getChildCount()];
			for(int i=0;i<node.getChildCount();i++){
				childs[i]=computeArbitraryz3RateExpressionString(node.getChild(i), varsName,speciesNameToSpecies,math,speciesIdToSpecies,suffix,symbolicParameters);
			}
			if(type.equals(Type.TIMES)){
				return "( * " + spaceSeparated(childs) + " )";
				//return ctx.mkMul(childs);
			}
			else if(type.equals(Type.MINUS) ){
				if(node.getChildCount()>2){
					throw new UnsupportedOperationException("What does an n-ary substraction mean? "+node.toString());
				}
				else{
					return "( - " + spaceSeparated(childs) + " )";
					//return ctx.mkSub(childs);
				}
			}
			else if(type.equals(Type.PLUS) ){
				return "( + " + spaceSeparated(childs) + " )";
				//return ctx.mkAdd(childs);
			}
			else if(type.equals(Type.DIVIDE) ){
				if(node.getChildCount()>2){
					throw new UnsupportedOperationException("What does an n-ary division mean? "+node.toString());
				}
				else{
					return "( / " + spaceSeparated(childs) + " )";
					//return ctx.mkDiv(childs[0], childs[1]);
				}
			}
			else if(type.equals(Type.POWER)){
				if(node.getChildCount()>2){
					throw new UnsupportedOperationException("What does an n-ary power mean? "+node.toString());
				}
				else{
					return "( ^ " + spaceSeparated(childs) + " )";
					//return ctx.mkPower(childs[0], childs[1]);
				}
			}
			/*else if(type.equals(Type.FUNCTION_ROOT) ){
				return ctx.mkPower(childs[0], ctx.mkDiv(ctx.mkReal(1), childs[1]));
			}*/
		}
		else if(node.isOperator() && node.getChildCount()==1 && node.getType().equals(Type.MINUS)){
			String child = computeArbitraryz3RateExpressionString(node.getLeftChild(), varsName,speciesNameToSpecies,math,speciesIdToSpecies,suffix,symbolicParameters);
			return "(- 0 "+child+")";
			//return ctx.mkSub(new ArithExpr[]{ctx.mkReal(0),child});
		}
		else if((node.isOperator() && node.getChildCount()==1 && node.getType().equals(Type.FUNCTION_ABS)) ||
				(node.isFunction() && node.getType().equals(Type.FUNCTION_ABS))){
			//(define-fun absolute ((x Int)) Int (ite (>= x 0) x (- x)))
			String child = computeArbitraryz3RateExpressionString(node.getLeftChild(), varsName,speciesNameToSpecies,math,speciesIdToSpecies,suffix,symbolicParameters);
			return "(ite (>= "+child+" 0) "+child+" (- "+child+"))";
			//ArithExpr z3Zero = ctx.mkReal(0);
			//return (ArithExpr)ctx.mkITE(ctx.mkGe(child, z3Zero), child, ctx.mkSub(new ArithExpr[]{z3Zero,child}));
		}
		//This is used if the ODEs have been imported from matlab (x(1),x(2),...)
		else if(node.isFunction() && varsName !=null && node.getName().equals(varsName)){
			int id = node.getListOfNodes().get(0).getInteger();
			ISpecies species = speciesIdToSpecies[id-1];
			return "s"+species.getID()+suffix;
			//ArithExpr speciesPop = speciesToPopulation.get(species);
			//return speciesPop;
		}
		else if(node.isFunction() && node.getName().equalsIgnoreCase("min")){
			String[] childs = new String[node.getChildCount()];
			for(int i=0;i<node.getChildCount();i++){
				childs[i]=computeArbitraryz3RateExpressionString(node.getChild(i), varsName,speciesNameToSpecies,math,speciesIdToSpecies,suffix,symbolicParameters);
			}
			if(node.getChildCount()!=2){
				throw new UnsupportedOperationException("min currently implemented only as a binary operator "+node.toString());
			}
			//(define-fun myMin ((x Real) (y Real)) Real (ite (< x y) x y))
			return "(ite (< "+childs[0]+" "+childs[1]+") "+childs[0]+" "+childs[1]+")";
			//return (ArithExpr)ctx.mkITE(ctx.mkLe(childs[0], childs[1]), childs[0], childs[1]); 
		}
		else if(node.isFunction() && node.getName().equalsIgnoreCase("max")){
			String[] childs = new String[node.getChildCount()];
			for(int i=0;i<node.getChildCount();i++){
				childs[i]=computeArbitraryz3RateExpressionString(node.getChild(i), varsName,speciesNameToSpecies,math,speciesIdToSpecies,suffix,symbolicParameters);
			}
			if(node.getChildCount()!=2){
				throw new UnsupportedOperationException("max currently implemented only as a binary operator "+node.toString());
			}
			//(define-fun myMax ((x Real) (y Real)) Real (ite (< x y) y x))
			return "(ite (< "+childs[0]+" "+childs[1]+") "+childs[1]+" "+childs[0]+")";
			//return (ArithExpr)ctx.mkITE(ctx.mkLe(childs[0], childs[1]), childs[1], childs[0]); 
		}
		else if(node.isVariable()){
			//This is used if the odes are loaded from the GUI (species names are not x(0),x(1), but just names)
			ISpecies species = null;
			if(varsName==null && speciesNameToSpecies!=null && ((species=speciesNameToSpecies.get(node.getName()))!=null)){
				//ArithExpr speciesPop = speciesToPopulation.get(species);
				//return speciesPop;
				return "s"+species.getID()+suffix;
			}
			else if(symbolicParameters!=null && (symbolicParameters.contains(node.getName()))){
				return node.getName();
				//return symbolicParameter;
			}
			else{
				double paramValue = math.evaluate(node.getName());
				return String.valueOf(BigDecimal.valueOf(paramValue).toPlainString());
				//return ctx.mkReal(BigDecimal.valueOf(paramValue).toPlainString());
			}
		}
		else if(node.isNumber()){
			return String.valueOf(BigDecimal.valueOf(node.getReal()).toPlainString());
			//return ctx.mkReal(BigDecimal.valueOf(node.getReal()).toPlainString());
		}
		throw new UnsupportedOperationException(node.toString());
	}
	
	/**
	 * This method is used to compute the z3 encoding of the rate of a reaction. The method supports mass action reactions, and reactions with arbitrary rate. THe latter can have been imported from Matlab (varsName is not null and species names are varsName(1),...,varsName(n)), or from the GUI (varsName is null and species have normal names) 
	 * @param ctx
	 * @param speciesToPopulation
	 * @param node
	 * @param varsName
	 * @param speciesNameToSpecies
	 * @param math
	 * @param speciesIdToSpecies
	 * @return
	 * @throws Z3Exception
	 */
	public static ArithExpr computeArbitraryz3RateExpression(Context ctx, HashMap<ISpecies, ArithExpr> speciesToPopulation,HashMap<String, ArithExpr> symbParNameToSymbParZ3, ASTNode node,  String varsName, HashMap<String, ISpecies> speciesNameToSpecies, MathEval math, ISpecies[] speciesIdToSpecies) throws Z3Exception {
		//A variable is actually a parameter, while a function (e.g., varsName(1)) is an ODE variable (with id 0).
		if(node.isOperator() && node.getChildCount()>=2){
			Type type = node.getType();
			ArithExpr[] childs = new ArithExpr[node.getChildCount()];
			for(int i=0;i<node.getChildCount();i++){
				childs[i]=computeArbitraryz3RateExpression(ctx, speciesToPopulation,symbParNameToSymbParZ3, node.getChild(i), varsName,speciesNameToSpecies,math,speciesIdToSpecies);
			}
			if(type.equals(Type.TIMES)){
				return ctx.mkMul(childs);
			}
			else if(type.equals(Type.MINUS) ){
				if(node.getChildCount()>2){
					throw new UnsupportedOperationException("What does an n-ary substraction mean? "+node.toString());
				}
				else{
					return ctx.mkSub(childs);
				}
			}
			else if(type.equals(Type.PLUS) ){
				return ctx.mkAdd(childs);
			}
			else if(type.equals(Type.DIVIDE) ){
				if(node.getChildCount()>2){
					throw new UnsupportedOperationException("What does an n-ary division mean? "+node.toString());
				}
				else{
					return ctx.mkDiv(childs[0], childs[1]);
				}
			}
			else if(type.equals(Type.POWER)){
				if(node.getChildCount()>2){
					throw new UnsupportedOperationException("What does an n-ary power mean? "+node.toString());
				}
				else{
					return ctx.mkPower(childs[0], childs[1]);
				}
			}
			/*else if(type.equals(Type.FUNCTION_ROOT) ){
				return ctx.mkPower(childs[0], ctx.mkDiv(ctx.mkReal(1), childs[1]));
			}*/
		}
		else if(node.isOperator() && node.getChildCount()==1 && node.getType().equals(Type.MINUS)){
			ArithExpr child = computeArbitraryz3RateExpression(ctx, speciesToPopulation,symbParNameToSymbParZ3, node.getLeftChild(), varsName,speciesNameToSpecies,math,speciesIdToSpecies);
			return ctx.mkSub(new ArithExpr[]{ctx.mkReal(0),child});
		}
		else if((node.isOperator() && node.getChildCount()==1 && node.getType().equals(Type.FUNCTION_ABS)) ||
				(node.isFunction() && node.getType().equals(Type.FUNCTION_ABS))){
			//(define-fun absolute ((x Int)) Int (ite (>= x 0) x (- x)))
			ArithExpr child = computeArbitraryz3RateExpression(ctx, speciesToPopulation, symbParNameToSymbParZ3,node.getLeftChild(), varsName,speciesNameToSpecies,math,speciesIdToSpecies);
			ArithExpr z3Zero = ctx.mkReal(0);
			return (ArithExpr)ctx.mkITE(ctx.mkGe(child, z3Zero), child, ctx.mkSub(new ArithExpr[]{z3Zero,child}));
		}
		//This is used if the ODEs have been imported from matlab (x(1),x(2),...)
		else if(node.isFunction() && varsName !=null && node.getName().equals(varsName)){
			int id = node.getListOfNodes().get(0).getInteger();
			ISpecies species = speciesIdToSpecies[id-1];
			ArithExpr speciesPop = speciesToPopulation.get(species);
			return speciesPop;
		}
		else if(node.isFunction() && node.getName().equalsIgnoreCase("min")){
			ArithExpr[] childs = new ArithExpr[node.getChildCount()];
			for(int i=0;i<node.getChildCount();i++){
				childs[i]=computeArbitraryz3RateExpression(ctx, speciesToPopulation, symbParNameToSymbParZ3,node.getChild(i), varsName,speciesNameToSpecies,math,speciesIdToSpecies);
			}
			if(node.getChildCount()!=2){
				throw new UnsupportedOperationException("min currently implemented only as a binary operator "+node.toString());
			}
			//(define-fun myMin ((x Real) (y Real)) Real (ite (< x y) x y))
			return (ArithExpr)ctx.mkITE(ctx.mkLe(childs[0], childs[1]), childs[0], childs[1]); 
		}
		else if(node.isFunction() && node.getName().equalsIgnoreCase("max")){
			ArithExpr[] childs = new ArithExpr[node.getChildCount()];
			for(int i=0;i<node.getChildCount();i++){
				childs[i]=computeArbitraryz3RateExpression(ctx, speciesToPopulation, symbParNameToSymbParZ3,node.getChild(i), varsName,speciesNameToSpecies,math,speciesIdToSpecies);
			}
			if(node.getChildCount()!=2){
				throw new UnsupportedOperationException("max currently implemented only as a binary operator "+node.toString());
			}
			//(define-fun myMax ((x Real) (y Real)) Real (ite (< x y) y x))
			return (ArithExpr)ctx.mkITE(ctx.mkLe(childs[0], childs[1]), childs[1], childs[0]); 
		}
		else if(node.isVariable()){
			//This is used if the odes are loaded from the GUI (species names are not x(0),x(1), but just names)
			ISpecies species = null;
			ArithExpr symbolicParameter = null;
			if(varsName==null && speciesNameToSpecies!=null && ((species=speciesNameToSpecies.get(node.getName()))!=null)){
				ArithExpr speciesPop = speciesToPopulation.get(species);
				return speciesPop;
			}
			else if(symbParNameToSymbParZ3!=null && ((symbolicParameter=symbParNameToSymbParZ3.get(node.getName()))!=null)){
				return symbolicParameter;
			}
			else{
				double paramValue = math.evaluate(node.getName());
				return ctx.mkReal(BigDecimal.valueOf(paramValue).toPlainString());
			}
		}
		else if(node.isNumber()){
			return ctx.mkReal(BigDecimal.valueOf(node.getReal()).toPlainString());
		}
		throw new UnsupportedOperationException(node.toString());
	}

	protected static ArithExpr getOrComputeProductOfReagents(Context ctx, HashMap<IComposite, ArithExpr> massActionExpressions,IComposite reagents,HashMap<ISpecies, ArithExpr> speciesToPopulation) throws Z3Exception {
		ArithExpr massActionExpr = massActionExpressions.get(reagents);
		if(massActionExpr==null){
			massActionExpr = ctx.mkReal(1);
			
			ArithExpr[] factors;
			if(reagents.isUnary()){
				factors = new ArithExpr[1];
				ISpecies species = reagents.getFirstReagent();
				ArithExpr population = speciesToPopulation.get(species);
				ArithExpr powReagents = population;
				factors[0]=powReagents;
			}
			else{
				factors = new ArithExpr[reagents.getNumberOfDifferentSpecies()];
				for(int i=0;i<reagents.getNumberOfDifferentSpecies();i++){
					ISpecies species = reagents.getAllSpecies(i);
					int multiplicity = reagents.getMultiplicities(i);
					ArithExpr population = speciesToPopulation.get(species);
					ArithExpr powReagents = population;
					if(multiplicity>1){
						powReagents = ctx.mkPower(population, ctx.mkInt(multiplicity)) ;
					}
					
					//massActionExpr = ctx.mkMul(new ArithExpr[]{ powReagents, massActionExpr});
					factors[i]=powReagents;
				}
			}
			massActionExpr=ctx.mkMul(factors);
			massActionExpressions.put(reagents, massActionExpr);
		}
		//CRNReducerCommandLine.println(out,bwOut,reagents.toMultiSet() +" "+massActionExpr.toString());
		return massActionExpr;
	}
	
	
	protected static DoubleAndStatus check(Solver solver, boolean verbose, boolean showTime, MessageConsoleStream out, BufferedWriter bwOut) throws Z3Exception{
		long begin = System.currentTimeMillis();
		//CRNReducerCommandLine.println(out,bwOut,solver);
		Status status = solver.check();
		long end = System.currentTimeMillis();
		double runtimeCheck = (double)(end-begin) / 1000.0;
		//totalSMTChecksSeconds+=runtimeCheck;
		if(showTime){
			CRNReducerCommandLine.println(out,bwOut,"Check requred: "+String.format(CRNReducerCommandLine.MSFORMAT,String.valueOf(runtimeCheck))+" (s)");
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

		
	

	private double[][] computeAndParseMetrics(ICRN crn,
			boolean verbose, MessageConsoleStream out, BufferedWriter bwOut,Terminator terminator, IMessageDialogShower messageDialogShower) throws Z3Exception, IOException {
		double[][] metrics=null;
		Model model=null;
		SolverAndStatus ss = actuallyComputeMetrics(crn, crn.getName(), verbose,out,bwOut,terminator,messageDialogShower);
		CRNReducerCommandLine.println(out,bwOut,"SMT check completed ");
		if(ss.getStatus()==Status.UNKNOWN){
			CRNReducerCommandLine.println(out,bwOut,"z3 returned unknown. This is the reason:");
			CRNReducerCommandLine.println(out,bwOut,ss.getSolver().getReasonUnknown());
			//System.exit(-1);
			throw new Z3Exception("z3 returned unknown. This is the reason: "+solver.getReasonUnknown());
			//return null;
		}
		else if(ss.getStatus()==Status.UNSATISFIABLE){
			CRNReducerCommandLine.println(out,bwOut,"UNSAT!");
			model = null;
			metrics=null;
		}
		else{
			metrics = new double[crn.getSpeciesSize()][crn.getSpeciesSize()];
			model = ss.getSolver().getModel();
			for(int i=0;i<crn.getSpeciesSize();i++) {
				for(int j=0;j<crn.getSpeciesSize();j++) {
					Expr expr = model.eval(dMetrics[i][j], false);
					double val = math.evaluate(expr.toString());
					metrics[i][j]=val;
					String valStr= String.format(METRICSFORMAT,val);
					CRNReducerCommandLine.print(out, bwOut,valStr);
					if(j<crn.getSpeciesSize()-1) {
						CRNReducerCommandLine.print(out, bwOut,",");
					}
				}
				CRNReducerCommandLine.println(out, bwOut);
			}
			
			CRNReducerCommandLine.println(out, bwOut);
		}
		//return model;
		return metrics;
	}
	
	public SolverAndStatus actuallyComputeMetrics(ICRN crn, String name, boolean verbose, MessageConsoleStream out, BufferedWriter bwOut,Terminator terminator, IMessageDialogShower msgDialogShower) throws Z3Exception, IOException{

		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"Actually computing the metrics for " + name);
		}

		if(!initialized){
			init(crn,verbose,out,bwOut,terminator);
		}

		//Solver solver = ctx.mkSolver();
		solver.reset();
		
		
		//First we add: d(x) = x
		solver.add(allODEsDef);
		//now we add 0 <= d(i,j) <= C
		solver.add(dMetricsDomainAssertion);
		
		//Now we create psi
		BoolExpr psi=ctx.mkImplies(absi_jLeDij, absdi_djLeDij);
		//We make 0 <= abs(x0) <= lambda ... 0 <= abs(xn) <= lambda  -> psi
		BoolExpr absPopDomain_psi=ctx.mkImplies(absPopulationDomainAssertion, psi);
		
		//Finally, we create the big formula with the forall quantifier
		BoolExpr bigFormula = ctx.mkForall(populations, absPopDomain_psi, 1, null, null, null, null);
		//Quantifier bigFormula = ctx.mkForall(populationsSorts, declPopulations, 1, null, null, null, null);
		
		//as a last step, we check the sat of  the formula
		//solver.add(absPopDomain_psi);
		solver.add(bigFormula);
		
		
		DoubleAndStatus timeAndStatus = check(solver,verbose,SHOWTIMEATEACHSTEP,out,bwOut);
		totalSMTChecksSeconds+= timeAndStatus.getDouble();
		smtChecksSecondsAtStep.add(timeAndStatus.getDouble());
		//solver.Push();check(solver);//VERY BAD PERFORMANCES WITH PUSH
		return new SolverAndStatus(solver,timeAndStatus.getStatus());
			
	}
	
	private void computAssertionToCheckEFLOnCurrentWholePartition(ICRN crn,
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
				ArithExpr popRep = speciesToPopulation.get(rep);
				ArithExpr odeNameRep = speciesToODENames.get(rep);
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
	
	//EREFL
	/*private void computAssertionToCheckExistsRateSuchThatEFLOnCurrentWholePartition(ICRN crn,
			IPartition partition, Solver solver) throws Z3Exception {
				
		IBlock currentBlock = partition.getFirstBlock();
		BoolExpr[] conditionsOfblocks = new BoolExpr[partition.size()];
		BoolExpr[] ICconditionsOfblocks = new BoolExpr[partition.size()];
		int b=0;
		BoolExpr z3True = ctx.mkTrue();
		while(currentBlock!=null){
			if(currentBlock.getSpecies().size()!=1){
				BoolExpr[] equalODEs = new BoolExpr[currentBlock.getSpecies().size()-1];
				BoolExpr[] equalICs = new BoolExpr[currentBlock.getSpecies().size()-1];
				int s=0;
				ISpecies rep = currentBlock.getRepresentative();
				ArithExpr popRep = speciesToPopulation.get(rep);
				ArithExpr odeInvocationRep = speciesToODEInvocation.get(rep);//ArithExpr odeNameRep = speciesToODENames.get(rep);
				for (ISpecies species : currentBlock.getSpecies()) {
					if(!species.equals(rep)){
						equalICs[s]=  ctx.mkEq(popRep, speciesToPopulation.get(species));
						equalODEs[s]= ctx.mkEq(odeInvocationRep, speciesToODEInvocation.get(species));//equalODEs[s]= ctx.mkEq(odeNameRep, speciesToODENames.get(species));
						s++;
					}
				} 
				conditionsOfblocks[b]= ctx.mkAnd(equalODEs);
				ICconditionsOfblocks[b] = ctx.mkAnd(equalICs);
			}
			else{
				conditionsOfblocks[b] = z3True;
				ICconditionsOfblocks[b] = z3True;
			}
			currentBlock=currentBlock.getNext();
			b++;
		}

		BoolExpr bodyOfForall = ctx.mkImplies(ctx.mkAnd(ICconditionsOfblocks),ctx.mkAnd(conditionsOfblocks));
		//https://github.com/Z3Prover/z3/blob/master/src/api/java/Context.java
		BoolExpr bodyOfExists = ctx.mkForall(sortsOfodes, namesOfSpecies, bodyOfForall ,0, null, null, null, null);
		BoolExpr existsSymbolicParameters = ctx.mkExists(sortsOfSymbolicParameters, namesOfSymbolicParameters, bodyOfExists, 0, null, null, null,null);
		//solver.add(existsSymbolicParameters);
		solver.add(bodyOfExists);
		//System.out.println("ciao");
	}*/
	
	/*private void computAssertionToCheckEFLOnCurrentWholePartition(ICRN crn,
			IPartition partition, Solver solver) throws Z3Exception {
		
		BoolExpr negationOfEFL = ctx.mkTrue();		
		IBlock currentBlock = partition.getFirstBlock();
		while(currentBlock!=null){
			if(currentBlock.getSpecies().size()!=1){
				BoolExpr[] equalODEs = new BoolExpr[currentBlock.getSpecies().size()+1];
				int s=0;
				ISpecies rep = currentBlock.getRepresentative();
				for (ISpecies species : currentBlock.getSpecies()) {
					BoolExpr ic = ctx.mkEq(speciesToPopulation.get(rep), speciesToPopulation.get(species));
					solver.add(ic);
					equalODEs[s]= ctx.mkEq(speciesToODENames.get(rep), speciesToODENames.get(species));
					s++;
				};
				equalODEs[s]=negationOfEFL;
				//TODO Maybe I should do an array of boolexpr, with a pos per block. And then I do an and among all the conditions of each block 
				negationOfEFL= ctx.mkAnd(equalODEs);
			}
			currentBlock=currentBlock.getNext();
		}

		solver.add(ctx.mkNot(negationOfEFL));
	}*/
	
	private void computAssertionToCheckEFLOnCurrentPartitionOneBlock(ICRN crn,
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
				ArithExpr popRep = speciesToPopulation.get(rep);
				ArithExpr odeNameRep = speciesToODENames.get(rep);
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

	/*public static CRNandPartition computeReducedCRNEFLNONMassAction(ICRN crn, String name, IPartition partition, List<String> parameters, String commentSymbol,MessageConsoleStream out, BufferedWriter bwOut)  throws IOException {
		return SMTOrdinaryFluidBisimilarityBinary.computeReducedCRNOrdinaryNonMassAction(crn, name, partition, parameters, commentSymbol, out,bwOut);
	}*/
	public static CRNandPartition computeReducedCRNEFLNONMassAction(ICRN crn, String name, IPartition partition, List<String> symbolicParameters,List<IConstraint> constraints,List<String> parameters, String commentSymbol,MessageConsoleStream out, BufferedWriter bwOut, Terminator terminator) throws IOException {
		ICRN reducedCRN = new CRN(name,symbolicParameters,constraints,parameters,crn.getMath(),out,bwOut);
		
		ISpecies[] speciesIdToSpecies= new ISpecies[crn.getSpecies().size()];
		crn.getSpecies().toArray(speciesIdToSpecies);
		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
		for (ISpecies species : crn.getSpecies()) {
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			speciesNameToSpecies.put(species.getName(), species);
		}

		IBlock uniqueBlock = new Block();
		IPartition trivialPartition = new Partition(uniqueBlock,partition.size());
		IBlock uniqueAlgebraicBlock = new Block();
		boolean addedAlgebraicBlock=false;
		
		ISpecies[] representativeSpecies = CRNBisimulationsNAry.getSortedBlockRepresentatives(partition, terminator);

		
		//long begin=System.currentTimeMillis();
		//Create the set of reduced species: a species per block
		//IBlock currentBlock = partition.getFirstBlock();
		//int i=0;
		HashMap<IBlock, ISpecies> correspondenceBlock_ReducedSpecies = new HashMap<IBlock, ISpecies>(partition.size());
		/*
		while(currentBlock!=null){
		*/
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
			BigDecimal ic = blockRepresentative.getInitialConcentration();

			ISpecies reducedSpecies;
			if(crn.isZeroSpecies(blockRepresentative)){
				reducedSpecies = reducedCRN.getCreatingIfNecessaryTheZeroSpecies();
				reducedSpecies.setInitialConcentration(ic, blockRepresentative.getInitialConcentrationExpr());
			}
			else{
				reducedSpecies = new Species(nameRep, blockRepresentative.getOriginalName(),i, ic,blockRepresentative.getInitialConcentrationExpr(),blockRepresentative.getNameAlphanumeric(),blockRepresentative.isAlgebraic());
				reducedSpecies.setIsAlgebraic(blockRepresentative.isAlgebraic());
				reducedCRN.addSpecies(reducedSpecies);
			}
			reducedSpecies.addCommentLines(currentBlock.computeBlockComment());
			if(reducedSpecies.isAlgebraic()) {
				if(!addedAlgebraicBlock) {
					trivialPartition.add(uniqueAlgebraicBlock);
					addedAlgebraicBlock=true;
				}
				uniqueAlgebraicBlock.addSpecies(reducedSpecies);
			}
			else {
				uniqueBlock.addSpecies(reducedSpecies);
			}
			
			correspondenceBlock_ReducedSpecies.put(currentBlock, reducedSpecies);
			/*
			currentBlock=currentBlock.getNext();
			i++;
			*/
		}
		//long end = System.currentTimeMillis();
		//CRNReducerCommandLine.println(out,bwOut,"Species reduced in "+((end-begin)/1000.0)+" seconds");

		//begin=System.currentTimeMillis();
		//Create the reduced reactions
		List<ICRNReaction> reducedReactions = new ArrayList<ICRNReaction>();
		for (ICRNReaction reaction : crn.getReactions()) {
			if(Terminator.hasToTerminate(terminator)){
				break;
			}
			IComposite reagentsOfReaction = reaction.getReagents();
			IComposite productsOfReaction = reaction.getProducts();

			IComposite reducedReagentsOfReaction = CRNBisimulationsNAry.getNewCompositeReplaceSpeciesWithReducedOneOfBlock(reagentsOfReaction,partition,correspondenceBlock_ReducedSpecies);
			CompositeAndBoolean compAndBol = ExactFluidBisimilarity.getNewCompositeMaintainigRepresentativesAndAddingNonRepresentativeReagents(productsOfReaction,reagentsOfReaction,crn,partition,reducedCRN,correspondenceBlock_ReducedSpecies);
			IComposite reducedProductsOfReaction = compAndBol.getComposite();
			/*if(!compAndBol.isAddedZeroSpecies()){
				reducedProductsOfReaction = reducedProductsOfReaction.getNewCompositeReplaceSpeciesWithReducedOneOfBlock(partition,correspondenceBlock_ReducedSpecies);
			}*/

			//now I discard this reaction if has same LHS and RHS
			SMTOrdinaryFluidBisimilarityBinary.addToListReducedReactionsNonMassActionCRN(partition, reducedCRN, speciesIdToSpecies, speciesNameToSpecies, correspondenceBlock_ReducedSpecies, reducedReactions, reaction, reducedReagentsOfReaction, reducedProductsOfReaction,false);
		}

		//end = System.currentTimeMillis();
		//CRNReducerCommandLine.println(out,bwOut,"Reactions reduced in "+((end-begin)/1000.0)+" seconds");
		
		//begin=System.currentTimeMillis();
		if(!Terminator.hasToTerminate(terminator)){
			CRN.collapseAndCombineAndAddReactions(reducedCRN,reducedReactions,out,bwOut);
		}
		/*if(CRNReducerCommandLine.collapseReactions){
			CRN.collapseAndCombineAndAddReactions(reducedCRN,reducedReactions,crn.isMassAction(),out,bwOut);
		}
		else{
			for (ICRNReaction reducedReaction : reducedReactions) {
				reducedCRN.addReaction(reducedReaction);
				AbstractImporter.addToIncomingReactionsOfProducts(reducedReaction.getArity(),reducedReaction.getProducts(), reducedReaction,CRNReducerCommandLine.addReactionToComposites);
				AbstractImporter.addToOutgoingReactionsOfReagents(reducedReaction.getArity(), reducedReaction.getReagents(), reducedReaction);
				if(CRNReducerCommandLine.hasToAddToReactionsWithNonZeroStoichiometry){
					AbstractImporter.addToReactionsWithNonZeroStoichiometry(reducedReaction.getArity(), reducedReaction.computeProductsMinusReagentsHashMap(),reducedReaction);
				}
				
			}
		}*/
	
		return new CRNandPartition(reducedCRN, trivialPartition);
	}
		
		
	/*private void computAssertionToCheckEFLOnCurrentPartitionOneBlock(ICRN crn,
			IPartition partition, IBlock blockSPL, StringBuilder initialConditions, StringBuilder equalODEs) throws Z3Exception {
		
		//The negation of EFL conjecture
		equalODEs.append("(assert (not (and true ");
		
		IBlock currentBlock = partition.getFirstBlock();
		while(currentBlock!=null){
			if(currentBlock.getSpecies().size()!=1){
				boolean isBlockSPL = currentBlock.equals(blockSPL);
				initialConditions.append("(assert (= ");
				if(isBlockSPL){
					equalODEs.append(" (= ");
				}
				for (ISpecies species : currentBlock.getSpecies()) {
					initialConditions.append(z3Importer.nameInZ3(species));
					initialConditions.append(" ");
					//BoolExpr expr = ctx.mkEq(currentBlock.getRepresentative().getOdeVarSMT(), species.getOdeVarSMT());solver.add(arg0);
					
					if(isBlockSPL){
						equalODEs.append(z3Importer.odeVarNameInZ3(species)+" ");
					}
				}
				initialConditions.append("))");
				if(isBlockSPL){
					equalODEs.append(")");
				}
			}
			currentBlock=currentBlock.getNext();
		}

		equalODEs.append(")))"); 

	}*/

}
