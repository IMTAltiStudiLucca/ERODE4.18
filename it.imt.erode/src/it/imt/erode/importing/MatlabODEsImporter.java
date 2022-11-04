package it.imt.erode.importing;

//import static org.matheclipse.core.expression.F.D;
//import org.matheclipse.core.eval.ExprEvaluator;
////import org.matheclipse.core.expression.AST;
//import org.matheclipse.core.expression.AbstractAST;
//import org.matheclipse.core.expression.F;
//import org.matheclipse.core.expression.Symbol;
//import org.matheclipse.core.interfaces.IAST;
//import org.matheclipse.core.interfaces.IExpr;
//import org.matheclipse.core.interfaces.ISymbol;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
//import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.eclipse.ui.console.MessageConsoleStream;
import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.ASTNode.Type;
import org.sbml.jsbml.text.parser.ParseException;
import org.sbml.jsbml.util.compilers.ASTNodeValue;
import org.sbml.jsbml.util.compilers.FormulaCompiler;

import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;
import it.imt.erode.auxiliarydatastructures.CRNandPartition;
import it.imt.erode.auxiliarydatastructures.IPartitionAndBoolean;
import it.imt.erode.auxiliarydatastructures.Matrix;
import it.imt.erode.auxiliarydatastructures.Reduction;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.DialogType;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.commandline.Terminator;
import it.imt.erode.crn.implementations.CRN;
import it.imt.erode.crn.implementations.CRNReaction;
import it.imt.erode.crn.implementations.CRNReactionArbitraryGUI;
import it.imt.erode.crn.implementations.CRNReactionArbitraryMatlab;
import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.implementations.Species;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.expression.evaluator.MathEval;
import it.imt.erode.expression.parser.IMonomial;
import it.imt.erode.expression.parser.MinusMonomial;
import it.imt.erode.expression.parser.NumberMonomial;
import it.imt.erode.expression.parser.ParameterMonomial;
import it.imt.erode.expression.parser.ProductMonomial;
import it.imt.erode.expression.parser.SpeciesMonomial;
import it.imt.erode.importing.automaticallygeneratedmodels.RandomBNG;
import it.imt.erode.onthefly.Pair;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.AXB;
import it.imt.erode.partitionrefinement.algorithms.CRNBisimulationsNAry;
import it.imt.erode.partitionrefinement.algorithms.EpsilonDifferentialEquivalences;
import it.imt.erode.partitionrefinement.algorithms.ExactFluidBisimilarity;
import it.imt.erode.simulation.deterministic.CRNVectorField;
import it.imt.erode.simulation.deterministic.apachecommons.CRNVectorFieldApacheCovariances;
import it.imt.erode.simulation.output.DataOutputHandlerAbstract;
import it.imt.erode.utopic.MatlabODEPontryaginExporter;
import it.imt.erode.utopic.vnodelp.MyFormulaCompilerForDouble;

/**
 * 
 * @author Andrea Vandin
 * This class is used to import a system of ordinary differential equations written in a matlab file (file extension: .m). 
 * If it is a polynomial ODE system, then it is converted in a mass action reaction network. Otherwise we just store a reaction per ODE variable X (X -> X + X) with the drift as arbitrary rate.
 */
public class MatlabODEsImporter  extends AbstractImporter {

	public static final String MatlabODEsFolder = "."+File.separator+"MatlabODEs"+File.separator;

	private static final boolean computeDE = true;
	//private static final boolean computeFDE = true;

	protected ISpecies[] loadedSpeciesIdToSpecies;

	public MatlabODEsImporter(String fileName,MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower) {
		super(fileName,out,bwOut,msgDialogShower);
	}

	public InfoCRNImporting importMatlabPolinomialODEs(boolean printInfo, boolean printCRN,boolean print) throws FileNotFoundException, IOException{
		return importMatlabODEs(printInfo, printCRN, print,true);
	}

	public InfoCRNImporting importMatlabArbitraryODEs(boolean printInfo, boolean printCRN,boolean print) throws FileNotFoundException, IOException{
		return importMatlabODEs(printInfo, printCRN, print,false);
	}

	public InfoCRNImporting importMatlabODEs(boolean printInfo, boolean printCRN,boolean print, boolean polynomialODEs) throws FileNotFoundException, IOException{
		return importMatlabODEs(printInfo, printCRN,print, polynomialODEs,false);
	}

	public InfoCRNImporting importMatlabODEs(boolean printInfo, boolean printCRN,boolean print, boolean polynomialODEs,boolean addEpsPar) throws FileNotFoundException, IOException{
		if(print){
			CRNReducerCommandLine.println(out,bwOut,"\nImporting: "+getFileName());
		}

		initInfoImporting();
		initMath();

		BufferedReader br = getBufferedReader();
		String line;

		char comment = '%';
		String commentSTR = "%";
		boolean functionNameLoaded=false;
		boolean sizeDeclared=false;

		String eqsName = null;
		String functionName = null;
		String varsName = null;

		/*function dx = ode(t,x)
		  % Parameters
		  r1 = 1.0; r2 = 2.0;
		  dx = zeros(5,1); % Size declaration

		  dx(1) = -r1*x(1) + r2*x(2) - ... 
		  	 3.0 * x(1)*x(3) + 4.0*x(4);
		  dx(2) = ... ;
		  ...
		  dx(5) = ... ;	 
		  end
		 */

		boolean stop = false;
		while ((line = br.readLine()) != null && ! stop) {
			line=line.trim();
			line=removeCommentAtTheEnd(line,comment);

			//Skip comments and empty lines
			if(line.equals("")||line.startsWith(commentSTR)){
				continue;
			}

			if(line.equals("end")){
				stop=true;
			}
			else if(!functionNameLoaded){
				if(line.startsWith("function ")){

					String[] tokens = line.split("\\=");
					StringTokenizer st = new StringTokenizer(tokens[0].trim());
					st.nextToken(); //discard function
					eqsName = st.nextToken(); //dx

					String rest = tokens[1].trim(); //ode(t,x)
					functionName = rest.substring(0, rest.indexOf('(')); // ode
					String args = rest.substring(rest.indexOf('(')+1, rest.lastIndexOf(')')); //t,x
					args = args.substring(args.indexOf(',')+1, args.length()); //x (possibly followed by other parameters) //TODO: problem, I might have "t,y"
					varsName = removeCommentAtTheEnd(args, ','); //x

					functionNameLoaded=true;
					initCRN(functionName);
					getInfoImporting().setLoadedCRN(true);
					if(addEpsPar){
						addParameter("eps", String.valueOf(Double.MIN_VALUE));
					}

				}
				else {
					CRNReducerCommandLine.printWarning(out,bwOut,"Line \""+line +"\" found outside a function - end block. It will be ignored.");
				}
			}
			else if((!sizeDeclared) && line.startsWith(eqsName)){
				//dx = zeros(5,1); % Size declaration
				String[] tokens = line.split("\\=");
				String numberOfSpecies = tokens[1].substring(tokens[1].indexOf('(')+1, tokens[1].indexOf(',')).trim();
				loadSpecies(Integer.valueOf(numberOfSpecies.trim()),varsName,polynomialODEs);
				sizeDeclared=true;
			}
			else if(sizeDeclared && line.startsWith(eqsName)){
				//dx(1) = -r1*x(1) + r2*x(2) - ... 
				//	  	 3.0 * x(1)*x(3) + 4.0*x(4);
				// dx(2) = ... ;
				//I assume that each ODE is defined in its own line.
				String idStr = line.substring(line.indexOf('(')+1, line.indexOf(')'));
				String body = line.substring(line.indexOf('=')+1,line.length()-1).trim();
				if(!body.equals("0") || body.equals("0.0")){
					//CRNReducerCommandLine.println(out,bwOut,"ode "+idStr+" = "+body);
					int id = Integer.valueOf(idStr) - 1 ;
					ISpecies speciesOfODE = loadedSpeciesIdToSpecies[id];


					ASTNode rateLaw=null;
					try {
						rateLaw = ASTNode.parseFormula(body);
					} catch (ParseException e) {
						throw new IOException(e.getMessage());
					}

					if(polynomialODEs){
						computeRNEncoding(varsName, speciesOfODE, rateLaw);
					}
					else{
						//General ODEs. We don't import them as a reaction network, but we just maintain a reaction with arbitrary rate per drift.
						storeArbitraryReaction(rateLaw, speciesOfODE, varsName);
					}
				}

			}
			else {
				//% Parameters
				//r1 = 1.0; r2 = 2.0;
				//I assume that we can have more parameters in one line. Each definition is contained in a line
				String[] parameters = line.split("\\;");
				for(int p =0; p<parameters.length;p++){		
					String parameterName = parameters[p].substring(0, parameters[p].indexOf('=')).trim();
					String parameterExpression = parameters[p].substring(parameters[p].indexOf('=')+1,parameters[p].length()).trim();
					addParameter(parameterName, parameterExpression);
				}
			}
		}



		IBlock uniqueBlock = new Block();
		setInitialPartition(new Partition(uniqueBlock,getCRN().getSpecies().size()));
		for (ISpecies species : getCRN().getSpecies()) {
			uniqueBlock.addSpecies(species);
		}

		getInfoImporting().setReadParameters(getCRN().getParameters().size());
		getInfoImporting().setReadSpecies(getCRN().getSpecies().size());
		getInfoImporting().setReadCRNReactions(getCRN().getReactions().size());
		//getInfoImporting().setReadProducts(getCRN().getProducts().size());
		//getInfoImporting().setReadReagents(getCRN().getReagents().size());

		if(print){
			if(printInfo){
				CRNReducerCommandLine.println(out,bwOut,getInfoImporting().toString());
			}
			if(printCRN){
				getCRN().printCRN();
			}
		}

		return getInfoImporting();
	}

	private void storeArbitraryReaction(ASTNode rateLaw, ISpecies speciesOfODE,String varsName) throws IOException {
		//create reaction speciesOfODE -> speciesOfODE + speciesOfODE, arbitrary rateLaw         //create reaction I -> I + speciesOfODE, arbitrary rateLaw NO!!!

		replaceODEVarWithSpecies(rateLaw,varsName);

		String body = rateLaw.toFormula();

		IComposite products = new Composite(speciesOfODE,speciesOfODE);
		ICRNReaction reaction = new CRNReactionArbitraryMatlab((IComposite)speciesOfODE, products, body,rateLaw,varsName,null);
		getCRN().addReaction(reaction);
		/*
		addToIncomingReactionsOfProducts(reaction.getArity(),(IComposite)speciesOfODE, reaction,CRNReducerCommandLine.addReactionToComposites);
		//addToOutgoingReactionsOfReagents(reaction.getArity(), reaction.getReagents(), reaction);
		if(CRNReducerCommandLine.hasToAddToReactionsWithNonZeroStoichiometry){
			addToReactionsWithNonZeroStoichiometry(reaction.getArity(), reaction.computeProductsMinusReagentsHashMap(),reaction);
		}
		 */
	}

	private void computeRNEncoding(String varsName, ISpecies speciesOfODE,ASTNode rateLaw) {
		ArrayList<IMonomial> monomials= parseMatlabPolynomialODE(rateLaw,varsName);
		for (IMonomial monomial : monomials) {
			//CRNReducerCommandLine.print(out,bwOut,monomial+" + ");
			ICRNReaction reaction = monomial.toReaction(speciesOfODE,loadedSpeciesIdToSpecies[loadedSpeciesIdToSpecies.length-1]).getReaction();
			if(reaction.getRate().compareTo(BigDecimal.ZERO)!=0){
				getCRN().addReaction(reaction);
				/*
				addToIncomingReactionsOfProducts(reaction.getArity(),reaction.getProducts(), reaction,CRNReducerCommandLine.addReactionToComposites);
				addToOutgoingReactionsOfReagents(reaction.getArity(), reaction.getReagents(), reaction,CRNReducerCommandLine.addReactionToComposites);
				if(CRNReducerCommandLine.hasToAddToReactionsWithNonZeroStoichiometry){
					addToReactionsWithNonZeroStoichiometry(reaction.getArity(), reaction.computeProductsMinusReagentsHashMap(),reaction);
				}
				 */
			}
		}
	}

	private ArrayList<IMonomial> parseMatlabPolynomialODE(ASTNode node,  String varsName) {
		//A variable is actually a parameter, while a function (e.g., varsName(1)) is an ODE variable (with id 0).
		if(node.isOperator() && node.getChildCount()==2){
			Type type = node.getType();
			ArrayList<IMonomial> monomialsLeft = parseMatlabPolynomialODE(node.getLeftChild(),varsName);
			ArrayList<IMonomial> monomialsRight = parseMatlabPolynomialODE(node.getRightChild(),varsName);
			if(type.equals(Type.TIMES)){	
				//combine the two
				ArrayList<IMonomial> products = new ArrayList<>(monomialsLeft.size()*monomialsRight.size());
				for (IMonomial left : monomialsLeft) {
					for (IMonomial right : monomialsRight) {
						products.add(new ProductMonomial(left, right));
					}
				}
				return products;
			}
			else if(type.equals(Type.MINUS) ){
				for (IMonomial right : monomialsRight) {
					monomialsLeft.add(new MinusMonomial(right));
				}
				return monomialsLeft;
			}
			else if(type.equals(Type.PLUS) ){
				monomialsLeft.addAll(monomialsRight);
				return monomialsLeft;
			}
		}
		else if(node.isOperator() && node.getChildCount()==1 && node.getType().equals(Type.MINUS)){

			ArrayList<IMonomial> monomials = parseMatlabPolynomialODE(node.getLeftChild(),varsName);

			ArrayList<IMonomial> ret = new ArrayList<>(monomials.size());
			for (IMonomial mon : monomials) {
				ret.add(new MinusMonomial(mon));
			}
			return ret;
		}
		else if(node.isFunction() && node.getName().equals(varsName)){
			int id = node.getListOfNodes().get(0).getInteger();
			ArrayList<IMonomial> ret = new ArrayList<IMonomial>(1);
			ret.add(new SpeciesMonomial(loadedSpeciesIdToSpecies[id-1]));
			return ret;
		}
		else if(node.isVariable()){
			ArrayList<IMonomial> ret = new ArrayList<IMonomial>(1);
			ret.add(new ParameterMonomial(BigDecimal.valueOf(getMath().evaluate(node.getName())),node.getName()));
			return ret;
		}
		else if(node.isNumber()){
			ArrayList<IMonomial> ret = new ArrayList<IMonomial>(1);
			ret.add(new NumberMonomial(BigDecimal.valueOf(node.getReal()),String.valueOf(node.getReal())));
			return ret;
		}

		throw new UnsupportedOperationException(node.toString());

	}

	private void replaceODEVarWithSpecies(ASTNode node,  String varsName) {
		int idIncrement = 1;
		//A variable is actually a parameter, while a function (e.g., varsName(1)) is an ODE variable (with id 0).
		//System.out.println(node);
		if(node.isOperator()){
			for(int i=0;i<node.getChildCount();i++){
				replaceODEVarWithSpecies(node.getChild(i), varsName);
			}
		}
		else if(node.isFunction() && !node.getName().equals(varsName)){
			for(int i=0;i<node.getChildCount();i++){
				replaceODEVarWithSpecies(node.getChild(i), varsName);
			}
		}
		else if(node.isFunction() && node.getName().equals(varsName)){
			int id = node.getListOfNodes().get(0).getInteger();
			int actualId = id-idIncrement;
			node.removeChild(0);
			node.setType(ASTNode.Type.NAME);
			node.setName(loadedSpeciesIdToSpecies[actualId].getName());
		}
		else if(node.isVariable()){
			//DO NOTHING	
			//Pay attention: functions are considered variables  in isVariables()...
		}
		else if(node.isNumber()){
			//DO NOTHING
		}
		else{
			throw new UnsupportedOperationException(node.toString());
		}
	}

	//
	//	private static IExpr replaceSpeciesWithFunction(IExpr iExpr, ISymbol varName, HashMap<String, ISpecies> speciesNameLowerCaseToSpecies) {
	////		if(iExpr.toString().equals("1/(1.0+k4-y(1)-y(2))")){
	////			System.out.println(iExpr);
	////		}
	//		
	//		int idIncrement=1;
	//		
	//		if(iExpr.isAST()){
	//			//ISymbol varName = F.$s("y");
	//			AbstractAST ast = (AbstractAST)iExpr;
	//			for(int i=0;i<ast.size();i++){
	//				ast.set(i,replaceSpeciesWithFunction(ast.getAt(i), varName, speciesNameLowerCaseToSpecies));
	//			}
	//		}
	//		else if(iExpr.isSymbol()) {
	//			Symbol symb = (Symbol)iExpr;
	//			ISpecies species = speciesNameLowerCaseToSpecies.get(symb.getSymbolName());
	//			if(species!=null){
	//				iExpr=varName.apply(F.ZZ(species.getID()+idIncrement));
	//			}
	//		}
	//		
	//		
	//		return iExpr;
	//		
	//		/*if(testAST.getAt(i).isSymbol()){
	//			Symbol symb = (Symbol)testAST.getAt(i);
	//			String name = symb.getSymbolName();
	//			if(name.equals("x0")){
	//				testAST.set(i,varName.apply(F.ZZ(1)));
	//			}
	//			
	//		}
	//	}*/
	//		
	//		
	//		
	//		/*
	//		 IExpr test= util.evaluate("1*x0*y(0)");
	//			AST testAST = (AST)test;
	//			
	//			ISymbol varName = F.$s("y");
	//			for(int i=0;i<testAST.size();i++){
	//				if(testAST.getAt(i).isSymbol()){
	//					Symbol symb = (Symbol)testAST.getAt(i);
	//					String name = symb.getSymbolName();
	//					if(name.equals("x0")){
	//						testAST.set(i,varName.apply(F.ZZ(1)));
	//					}
	//					
	//				}
	//			}
	//			System.out.println(test);
	//		 */
	//		
	//		
	//		
	//	}

	private static void replaceSpeciesWithODEVar(ASTNode node,  String varsName, HashMap<String, ISpecies> speciesNameToSpecies) {
		int idIncrement = 1;
		//A variable is actually a parameter, while a function (e.g., varsName(1)) is an ODE variable (with id 0).
		//System.out.println(node);
		if(node.isOperator()){
			for(int i=0;i<node.getChildCount();i++){
				replaceSpeciesWithODEVar(node.getChild(i), varsName,speciesNameToSpecies);
			}
		}
		else if(node.isFunction() ){
			for(int i=0;i<node.getChildCount();i++){
				replaceSpeciesWithODEVar(node.getChild(i), varsName,speciesNameToSpecies);
			}
		}
		/*else if(node.isFunction() && node.getName().equals(varsName)){
			int id = node.getListOfNodes().get(0).getInteger();
			int actualId = id-idIncrement;
			node.removeChild(0);
			node.setType(ASTNode.Type.NAME);
			node.setName(speciesIdToSpecies[actualId].getName());
		}*/
		else if(node.isVariable()){
			ISpecies species = speciesNameToSpecies.get(node.getName()); 
			if(species!=null){
				//species of xi with id j -> varsName(j+1)
				node.addChild(new ASTNode(species.getID()+idIncrement));
				node.setName(varsName);

				node.setType(Type.FUNCTION);
			}
			//DO NOTHING	
			//Pay attention: functions are considered variables  in isVariables()...
		}
		else if(node.isNumber()){
			//DO NOTHING
		}
		else{
			throw new UnsupportedOperationException(node.toString());
		}
	}

	private static void replaceSpeciesWithODEVarSquaredParenthesis(ASTNode node,  String varsName, HashMap<String, ISpecies> speciesNameToSpecies,int idIncrement) {
		//A variable is actually a parameter, while a function (e.g., varsName(1)) is an ODE variable (with id 0).
		//System.out.println(node);
		if(node.isOperator()){
			for(int i=0;i<node.getChildCount();i++){
				replaceSpeciesWithODEVarSquaredParenthesis(node.getChild(i), varsName,speciesNameToSpecies,idIncrement);
			}
		}
		else if(node.isFunction() ){
			for(int i=0;i<node.getChildCount();i++){
				replaceSpeciesWithODEVarSquaredParenthesis(node.getChild(i), varsName,speciesNameToSpecies,idIncrement);
			}
		}
		/*else if(node.isFunction() && node.getName().equals(varsName)){
			int id = node.getListOfNodes().get(0).getInteger();
			int actualId = id-idIncrement;
			node.removeChild(0);
			node.setType(ASTNode.Type.NAME);
			node.setName(speciesIdToSpecies[actualId].getName());
		}*/
		else if(node.isVariable()){
			ISpecies species = speciesNameToSpecies.get(node.getName());
			if(species!=null){
				node.setName(varsName+"["+(species.getID()+idIncrement)+"]");
			}
			//DO NOTHING	
			//Pay attention: functions are considered variables  in isVariables()...
		}
		else if(node.isNumber()){
			//DO NOTHING
		}
		else{
			throw new UnsupportedOperationException(node.toString());
		}
	}

	private void loadSpecies(int numberOfSpecies,String varsName, boolean polynomialODEs){
		if(polynomialODEs){
			loadedSpeciesIdToSpecies = new ISpecies[numberOfSpecies+1];
			ISpecies I = new Species(Species.I_SPECIESNAME, numberOfSpecies, BigDecimal.ONE, "1",false);
			getCRN().addSpecies(I);
			loadedSpeciesIdToSpecies[numberOfSpecies]=I;
		}
		else{
			loadedSpeciesIdToSpecies = new ISpecies[numberOfSpecies];
		}

		for(int i=0;i<numberOfSpecies;i++){
			int id = i;
			String speciesName = varsName+(id+1);
			ISpecies species = new Species(speciesName, id, BigDecimal.ZERO,"0.0",false);
			getCRN().addSpecies(species);
			loadedSpeciesIdToSpecies[i]=species;
		}
	}

	public static void printODEsToMatlabFIle(ICRN crn, String name, Collection<String> preambleCommentLines, boolean verbose, 
			String icComment,MessageConsoleStream out,BufferedWriter bwOut, String tEnd,String printJacobian,String odeFunc, 
			IMessageDialogShower msgDialogShower,boolean writeOnlyDrift) throws UnsupportedFormatException{

		String fileName = name;

		fileName=overwriteExtensionIfEnabled(fileName,".m");
		if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Writing ODEs in file "+fileName);
		}

		createParentDirectories(fileName);
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printODEsToMatlabFIle, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}

		try {

			if(preambleCommentLines!=null && preambleCommentLines.size()>0){
				for (String comment : preambleCommentLines) {
					bw.write("%"+comment+"\n");
				}
			}
			bw.write("\n\n");

			bw.write("% Automatically generated from "+crn.getName()+".\n");
			bw.write("% Original number of species: "+crn.getSpecies().size()+".\n");
			bw.write("% Original number of reactions: "+crn.getReactions().size()+".\n");			
			bw.write("\n% Correspondence with original names:\n");
			int incr=1;
			for (ISpecies species : crn.getSpecies()) {
				bw.write("%     y(" +(species.getID()+incr)+") = " + ((species.getOriginalName()==null)?species.getName():species.getOriginalName())+"\n");
			}
			bw.write("\n\n");

			String odeFuncName = "";
			odeFuncName = GUICRNImporter.getModelName(name);
//			if(crn.getName()!=null&&crn.getName()!=""){
//				odeFuncName = GUICRNImporter.getModelName(name);
//			}
//			else{
//				odeFuncName="model";
//			}
			
			//boolean writeOnlyDrift=true;
			String odeName;
			HashMap<String,ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
			if(!writeOnlyDrift) {
				odeName="ode";
				bw.write("function "+odeFuncName+"\n");

				//ISpecies[] speciesIdToSpecies = new ISpecies[crn.getSpecies().size()];
				//int s=0;
				//[T,Y]=ode45(@fluidflow,[0 100],[ 178 142 184 150 12 0 0 13 11 10 0 11 11 0 0 0 0 0 0 0 0 0 12 12 31684 20164 33856 22500 144 0 0 169 121 100 0 121 121 0 0 0 0 0 0 0 0 0 144 144 25276 32752 26700 2136 0 0 2314 1958 1780 0 1958 1958 0 0 0 0 0 0 0 0 0 2136 2136 26128 21300 1704 0 0 1846 1562 1420 0 1562 1562 0 0 0 0 0 0 0 0 0 1704 1704 27600 2208 0 0 2392 2024 1840 0 2024 2024 0 0 0 0 0 0 0 0 0 2208 2208 1800 0 0 1950 1650 1500 0 1650 1650 0 0 0 0 0 0 0 0 0 1800 1800 0 0 156 132 120 0 132 132 0 0 0 0 0 0 0 0 0 144 144 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 143 130 0 143 143 0 0 0 0 0 0 0 0 0 156 156 110 0 121 121 0 0 0 0 0 0 0 0 0 132 132 0 110 110 0 0 0 0 0 0 0 0 0 120 120 0 0 0 0 0 0 0 0 0 0 0 0 0 121 0 0 0 0 0 0 0 0 0 132 132 0 0 0 0 0 0 0 0 0 132 132 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 144]);
				//bw.write(" [T,Y]=ode45(@ode,[0 "+tEnd+"],[ ");
				//ode45 - ode15s
				//String odeFunc="ode45";
				bw.write(" [T,Y]="+odeFunc+"(@ode,[0 "+tEnd+"],[ ");
				for(ISpecies species : crn.getSpecies()){
					bw.write(" "+species.getInitialConcentration());
					speciesNameToSpecies.put(species.getName(), species);
					//speciesIdToSpecies[s]=species;
					//s++;
				}
				bw.write("]);\n ");

				bw.write("function array =  getSpecies(y)\n");
				bw.write("  array =  Y(:,y);\n");
				bw.write(" end\n\n");

				//ISpecies firstSpecies = crn.getSpecies().get(0);
				StringBuffer legend = new StringBuffer("");

				boolean viewsAdded=false;
				if(crn.getViewNames()!=null && crn.getViewNames().length>0){
					bw.write("% Output is restricted to Views.\n");

					//writeParams(crn,bw,"");

					boolean first=true;
					int vv=1;
					for(int v=0;v<crn.getViewNames().length;v++){
						//bw.write("% View "+v+":\n");
						//String vExpr = ;
						ASTNode node = ASTNode.parseFormula(crn.getViewExpressions()[v]);
						replaceSpeciesWithODEVar(node,"getSpecies",speciesNameToSpecies);
						String vExpr = node.toFormula();//parseViewExpression(vExpr);

						if(vExpr!=null){
							bw.write("view"+vv+"= "+vExpr+";\n");//"+v+"
							bw.write("plot(T,"+"view"+vv+");\n");//"+v+"
							vv++;
							bw.write("hold on;\n");
							if(first){
								first=false;
							}
							else{
								legend.append(",");
							}
							legend.append("'"+crn.getViewNames()[v]+"'");
							viewsAdded=true;
						}
					}
				}

				if(!viewsAdded){
					bw.write("% Output contains population trajectory of all species:\n");
					boolean first=true;
					for(ISpecies species : crn.getSpecies()){
						//plot(T,Y(:,3));
						bw.write("plot(T,Y(:,"+(species.getID()+1)+"));\n");
						bw.write("hold on;\n");
						if(first){
							first=false;
						}
						else{
							legend.append(",");
						}
						legend.append("'"+((species.getOriginalName()==null)?species.getName():species.getOriginalName())+"'");
					}
				}
				bw.write("xlabel('time');\n");
				bw.write("ylabel('value');\n");
				bw.write("legend("+legend.toString());
				bw.write(");\n");
				bw.write("end\n\n");

			}
			else {
				odeName=odeFuncName;
				for(ISpecies species : crn.getSpecies()){
					speciesNameToSpecies.put(species.getName(), species);
				}
			}

			// here the ODE starts
			int idIncrement=1;
			writeODEFunction(idIncrement,crn, bw, speciesNameToSpecies,"  ",true,true,true,true,true,null,odeName);

			if(printJacobian.equalsIgnoreCase("true")){
				//I have to print the jacobian
				//writeJacobianFunction(ICRN crn, MessageConsoleStream out, IMessageDialogShower msgDialogShower,BufferedWriter bw, String functionName,boolean ignoreZeros,boolean printCaption)
				bw.write("\n\n\n");
				writeJacobianFunction(crn, out, msgDialogShower, bw, "jacobian", false, false,true,true,"\t"/*,speciesIdToSpecies*/);
			}
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printODEsToMatlabFIle, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		} catch (ParseException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printODEsToMatlabFIle, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.println(out,bwOut,"A View expression could not be parsed.");
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		finally{
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"Writing model in file "+fileName+" completed");
			}
			try {
				bw.flush();
				bw.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in printODEsToMatlabFIle, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}	
	}





	public static HashMap<ISpecies, StringBuilder> writeODEFunction(int idIncrement,ICRN crn, BufferedWriter bw, HashMap<String, ISpecies> speciesNameToSpecies,String prefixSpace,boolean writeParameters, boolean writeT, boolean writeFunction, boolean initZeros,boolean writeParenthesis,String suffix,String odeName)
			throws IOException, UnsupportedFormatException {
		return writeODEFunction(idIncrement,crn, bw, speciesNameToSpecies, prefixSpace, writeParameters, writeT, writeFunction, initZeros,writeParenthesis,suffix,true,"y","dy",odeName);
	}


	public static HashMap<ISpecies, StringBuilder> writeODEFunction(int idIncrement,ICRN crn, BufferedWriter bw, HashMap<String, ISpecies> speciesNameToSpecies,String prefixSpace,boolean writeParameters, boolean writeT,
			boolean writeFunction, boolean initZeros,boolean writeParenthesis,String suffix,boolean writEnd,String varName,String derivName, String odeName)
					throws IOException, UnsupportedFormatException {
		writeFunctionAndParamsAndInitZeros(crn, bw, prefixSpace, writeParameters, writeT, writeFunction, initZeros,varName,derivName,odeName);

		HashMap<ISpecies, StringBuilder> speciesToDrift=null;
		if(crn.isMassAction()){
			speciesToDrift = GUICRNImporter.computeDriftsReplacingSpeciesNames(crn, false, varName, 1,writeParenthesis,suffix);
			writeDriftsOfMassActionWithSpeciesNameAlreadyReplacedWithY(derivName,speciesToDrift, crn, bw, prefixSpace);
		}
		else{
			boolean ignoreI=false;
			speciesToDrift = GUICRNImporter.computeDrifts(crn,false,ignoreI,false);
			writeDriftsOfArbitraryODEReplacingSpeciesNamesWithY(idIncrement,varName,derivName,speciesToDrift, crn, bw, speciesNameToSpecies,prefixSpace);
		}

		if(writEnd){
			bw.write("end\n");
		}

		return speciesToDrift;
	}

	private static HashMap<ISpecies, StringBuilder> computeODEFunction(ICRN crn, String variablePrefix,int idIncrement, String variableSuffix,HashMap<String, ISpecies> speciesNameToSpecies) throws ParseException{
		HashMap<ISpecies, StringBuilder> speciesToDrift=GUICRNImporter.computeDriftsReplacingSpeciesNames(crn,variablePrefix,idIncrement,variableSuffix,speciesNameToSpecies);
		return speciesToDrift;
	}

	public static void writeDriftsOfArbitraryODEReplacingSpeciesNamesWithY(int idIncrement,String varName, String derivName,
			HashMap<ISpecies, StringBuilder> speciesToDrift, ICRN crn, BufferedWriter bw,
			HashMap<String, ISpecies> speciesNameToSpecies, String prefixSpace)
					throws IOException, UnsupportedFormatException {
		writeDriftsOfArbitraryODEReplacingSpeciesNamesWithY(idIncrement,varName, derivName,
				speciesToDrift, crn, bw,
				speciesNameToSpecies, prefixSpace,false,false);
	}

	public static void writeDriftsOfArbitraryODEReplacingSpeciesNamesWithY(int idIncrement,String varName, String derivName,
			HashMap<ISpecies, StringBuilder> speciesToDrift, ICRN crn, BufferedWriter bw,
			HashMap<String, ISpecies> speciesNameToSpecies, String prefixSpace,boolean squaredParenthesis,boolean printDotZeroForInt)
					throws IOException, UnsupportedFormatException {
		writeDriftsOfArbitraryODEReplacingSpeciesNamesWithY(idIncrement,varName,derivName,speciesToDrift, crn, bw,speciesNameToSpecies,prefixSpace,"",squaredParenthesis,printDotZeroForInt);
	}

	public static void writeDriftsOfArbitraryODEReplacingSpeciesNamesWithY(int idIncrement,String varName, String derivName,
			HashMap<ISpecies, StringBuilder> speciesToDrift, ICRN crn, BufferedWriter bw,
			HashMap<String, ISpecies> speciesNameToSpecies, String prefixSpace, String secondEntry)
					throws IOException, UnsupportedFormatException {
		writeDriftsOfArbitraryODEReplacingSpeciesNamesWithY(idIncrement,varName, derivName,
				speciesToDrift, crn, bw,
				speciesNameToSpecies, prefixSpace, secondEntry,false,false);
	}

	public static void writeDriftsOfArbitraryODEReplacingSpeciesNamesWithY(int idIncrement,String varName, String derivName,
			HashMap<ISpecies, StringBuilder> speciesToDrift, ICRN crn, BufferedWriter bw,
			HashMap<String, ISpecies> speciesNameToSpecies, String prefixSpace, String secondEntry,boolean squaredParenthesis,boolean printDotZeroForInt)
					throws IOException, UnsupportedFormatException {
		for(ISpecies species : crn.getSpecies()){
			//for(Entry<ISpecies, StringBuilder> pair : speciesToDrift.entrySet()){
			String openPar = "(";
			String closedPar = ")";
			if(squaredParenthesis){
				openPar = "[";
				closedPar = "]";
			}

			bw.write(prefixSpace+derivName+openPar);
			bw.write(String.valueOf(species.getID()+idIncrement));
			bw.write(secondEntry+closedPar+" = ");
			StringBuilder value = speciesToDrift.get(species);
			if(value==null || value.length()==0){
				bw.write("0;\n");
			}
			else{
				ASTNode ratelaw;
				String s = value.toString();
				try {
					ratelaw = ASTNode.parseFormula(s);
				} catch (ParseException e) {
					System.out.println(s);
					throw new UnsupportedFormatException("Problems in parsing the rate expression "+s);
				}
				if(squaredParenthesis){
					replaceSpeciesWithODEVarSquaredParenthesis(ratelaw,varName,speciesNameToSpecies,idIncrement);	
				}
				else{
					replaceSpeciesWithODEVar(ratelaw,varName,speciesNameToSpecies);
				}

				if(printDotZeroForInt) {
					FormulaCompiler fc = new MyFormulaCompilerForDouble();
					ASTNodeValue compiledRateLaw = ratelaw.compile(fc);
					//System.out.println(compiledRateLaw.toString());
					bw.write(compiledRateLaw.toString());
				}
				else {
					bw.write(ratelaw.toFormula());
				}





				bw.write(";\n");
			}
		}
	}

	public static void writeDriftsOfMassActionWithSpeciesNameAlreadyReplacedWithY(String derivName,
			HashMap<ISpecies, StringBuilder> speciesToDrift, ICRN crn, BufferedWriter bw, String prefixSpace)
					throws IOException {
		writeDriftsOfMassActionWithSpeciesNameAlreadyReplacedWithY(derivName,speciesToDrift,crn,bw,prefixSpace,false);
	}

	public static void writeDriftsOfMassActionWithSpeciesNameAlreadyReplacedWithY(String derivName,
			HashMap<ISpecies, StringBuilder> speciesToDrift, ICRN crn, BufferedWriter bw, String prefixSpace,boolean squaredParenthesis)
					throws IOException {
		writeDriftsOfMassActionWithSpeciesNameAlreadyReplacedWithY(derivName,speciesToDrift, crn, bw, prefixSpace, "",squaredParenthesis);
	}

	public static void writeDriftsOfMassActionWithSpeciesNameAlreadyReplacedWithY(String derivName,
			HashMap<ISpecies, StringBuilder> speciesToDrift, ICRN crn, BufferedWriter bw, String prefixSpace, String secondEntry)
					throws IOException {
		writeDriftsOfMassActionWithSpeciesNameAlreadyReplacedWithY(derivName,
				speciesToDrift, crn, bw, prefixSpace, secondEntry,false);
	}

	public static void writeDriftsOfMassActionWithSpeciesNameAlreadyReplacedWithY(String derivName,
			HashMap<ISpecies, StringBuilder> speciesToDrift, ICRN crn, BufferedWriter bw, String prefixSpace, String secondEntry,boolean squaredParenthesis)
					throws IOException {
		for(ISpecies species : crn.getSpecies()){
			String openPar = "(";
			String closedPar = ")";
			if(squaredParenthesis){
				openPar = "[";
				closedPar = "]";
			}
			bw.write(prefixSpace+derivName+openPar);
			bw.write(String.valueOf(species.getID()+1));
			bw.write(secondEntry+closedPar+" = ");
			StringBuilder value = speciesToDrift.get(species);
			if(value==null || value.length()==0){
				bw.write("0;\n");
			}
			else{
				bw.write(value.toString());
				/*ASTNode ratelaw;
				String s = value.toString();
				try {
					ratelaw = ASTNode.parseFormula(s);
				} catch (ParseException e) {
					System.out.println(s);
					throw new UnsupportedFormatException("Problems in parsing the rate expression "+s);
				}
				replaceSpeciesWithODEVar(ratelaw,"y",speciesNameToSpecies);
				bw.write(ratelaw.toFormula());*/
				bw.write(";\n");					
			}
		}
	}

	public static void writeFunctionAndParamsAndInitZeros(ICRN crn, BufferedWriter bw, String prefixSpace,
			boolean writeParameters, boolean writeT, boolean writeFunction, boolean initZeros) throws IOException {
		writeFunctionAndParamsAndInitZeros(crn, bw, prefixSpace,
				writeParameters, writeT, writeFunction, initZeros,"y","dy","ode");
	}

	public static void writeFunctionAndParamsAndInitZeros(ICRN crn, BufferedWriter bw, String prefixSpace,
			boolean writeParameters, boolean writeT, boolean writeFunction, boolean initZeros,String varName,String derivName, String odeName) throws IOException {
		if(writeFunction){
			if(writeT){
				bw.write("function "+derivName+" =  "+odeName+"(t,"+varName+") \n");
			}
			else{
				bw.write("function "+derivName+" =  "+odeName+"("+varName+") \n");
			}
		}

		if(writeParameters){
			writeParams(crn,bw,prefixSpace);
			//			for (String param : crn.getParameters()) {
			//				int space = param.indexOf(' ');
			//				String paramName = param.substring(0, space).trim();
			//				String paramExpr = param.substring(space,param.length()).trim();
			//				//p=0.5;
			//				if(!paramName.equalsIgnoreCase("eps")){
			//					bw.write(prefixSpace+paramName +" = "+paramExpr+";\n");
			//				}
			//			}
		}

		//bw.write("\n");
		//		bw.write("    % Correspondence with original names:\n");
		//		int incr=1;
		//		for (ISpecies species : crn.getSpecies()) {
		//			bw.write("    %\t y(" +(species.getID()+incr)+") = " + ((species.getOriginalName()==null)?species.getName():species.getOriginalName())+"\n");
		//		}
		bw.write("\n");

		if(initZeros){
			bw.write(prefixSpace+derivName+"=zeros("+crn.getSpecies().size()+",1);\n\n");
		}
		//dy(1)=0-1*0.01*0.5*1/(y(1)+y(8)+y(12))*y(55)-1*0.03*y(1)-1*0.03*y(1)+1*0.03*y(2)+1*0.01*y(3);
	}

	public static ICRN expandLNA(ICRN crn, HashMap<String, ISpecies> speciesNameToExpandedSpecies,MessageConsoleStream out, BufferedWriter bwOut,IMessageDialogShower msgDialogShower,boolean verbose,String[][] C) throws IOException {
		if((!crn.isMassAction()) || crn.isSymbolic()){
			CRNReducerCommandLine.printWarning(out,bwOut, true, msgDialogShower, "The LNA function can be computed only for mass-action CRNs without symbolic parameters.", DialogType.Error);
			return null;
		}

		if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Creating the LNA equations");
		}

		//		ISpecies[] speciesIdToSpecies = new ISpecies[crn.getSpecies().size()];
		//		int s=0;
		//		for (ISpecies species : crn.getSpecies()) {
		//			speciesIdToSpecies[s]=species;
		//			s++;
		//		}


		MathEval math = new MathEval();
		ICRN crnExpanded = new CRN(crn.getName(),math,crn.getOut(),crn.getBWOut());

		CRN.copyParameters(crn, crnExpanded);
		CRN.copySpecies(crn, crnExpanded);

		for(ISpecies species : crnExpanded.getSpecies()){
			speciesNameToExpandedSpecies.put(species.getName(), species);
		}

		//Copy the parameters
		/*
		for (String p : crn.getParameters()) {
			String[] nameAndVal = p.split("\\ ");
			crnExpanded.addParameter(nameAndVal[0], nameAndVal[1]);
			math.setVariable(nameAndVal[0], Double.valueOf(nameAndVal[1]));
		}
		 */

		//Add the covariances species
		int id=crn.getSpecies().size();
		String zero="0";
		//String[][] C = new String[crn.getSpecies().size()][crn.getSpecies().size()];
		//HashMap<String, ISpecies> covarToSpecies = new HashMap<>(crn.getSpecies().size()*crn.getSpecies().size());
		{
			int i=0;
			for(ISpecies spi :crn.getSpecies()) {
				int j=0;
				for(ISpecies spj :crn.getSpecies()) {
					//String name = "C_"+i+"_"+j;
					String name = "C_"+spi.getName()+"_"+spj.getName();
					C[i][j] = name;
					//ISpecies sp =new Species(name, id, BigDecimal.ZERO, zero,false);
					ISpecies sp =new Species(name, "C_"+i+"_"+j, id, BigDecimal.ZERO, zero,true,false);
					crnExpanded.addSpecies(sp);
					id++;
					//covarToSpecies.put(name, sp);
					speciesNameToExpandedSpecies.put(name, sp);
					j++;
				}
				i++;
			}
		}
		/*
		for(int i=0;i<crn.getSpecies().size();i++) {
			for(int j=0;j<crn.getSpecies().size();j++) {
				String name = "C_"+i+"_"+j;
				C[i][j] = name;
				ISpecies sp =new Species(name, id, BigDecimal.ZERO, zero,false);
				crnExpanded.addSpecies(sp);
				id++;
				//covarToSpecies.put(name, sp);
				speciesNameToExpandedSpecies.put(name, sp);
			}
		}
		 */
		//Copy the reactions
		for(ICRNReaction reaction : crn.getReactions()){
			IComposite reagents = reaction.getReagents();
			IComposite products = reaction.getProducts();

			IComposite expandedReagents = replaceSpecies(speciesNameToExpandedSpecies, reagents);
			IComposite expandedProducts = replaceSpecies(speciesNameToExpandedSpecies, products);
			ICRNReaction expandedReaction = new CRNReaction(reaction.getRate(), expandedReagents, expandedProducts, reaction.getRateExpression(),reaction.getID());
			//crnExpanded.addReaction(expandedReaction);
			CRNImporter.addReaction(crnExpanded, expandedReagents, expandedProducts, expandedReaction);
		}

		//Add the new 'reactions' for the covariances
//		ISpecies[] speciesIdToSpecies = new ISpecies[crn.getSpecies().size()];
//		int s=0;
//		for (ISpecies species : crn.getSpecies()) {
//			speciesIdToSpecies[s]=species;
//			s++;
//		}
		int[][] jumps = DataOutputHandlerAbstract.computeJumps(crn);
		boolean ignoreZeros=true;
		boolean resolveParams=false;
		String[][] je = DataOutputHandlerAbstract.computeJacobianFunction(crn, jumps, msgDialogShower, out,ignoreZeros/*,speciesIdToSpecies*/,resolveParams);
		String[] nv = CRNVectorField.computeNVString(crn, /*terminator*/null);

		//now I compute the G
		String[][] G=new String[crn.getSpecies().size()][crn.getSpecies().size()];
		for(int m=0;m<crn.getReactions().size();m++){
			//if nv[m]=0 I can skip the iteration!
			if(nv[m]!=null && !nv[m].isEmpty()){
				double[][] jumpsmTranspXjumpsm = CRNVectorFieldApacheCovariances.multiplyTranspWithVector(jumps[m]);
				G = CRNVectorFieldApacheCovariances.sum(G, CRNVectorFieldApacheCovariances.multiplyScalar(jumpsmTranspXjumpsm,nv[m]));
			}
		}

		String[][] JeXC= CRNVectorFieldApacheCovariances.multiply(je, C,true,false);
		String[][] CXJeTransp= CRNVectorFieldApacheCovariances.multiply(C,CRNVectorFieldApacheCovariances.transpose(je),false,true);
		String[][] tot= CRNVectorFieldApacheCovariances.sum(CRNVectorFieldApacheCovariances.sum(JeXC,CXJeTransp),G);



		for(int i=0;i<crn.getSpecies().size();i++){
			for(int j=0;j<crn.getSpecies().size();j++){
				//ISpecies covarij = speciesNameToExpandedSpecies.get("C_"+i+"_"+j);
				ISpecies covarij = speciesNameToExpandedSpecies.get(C[i][j]);

				//create reaction covarij -> covarij + covarij, arbitrary rateLaw
				IComposite products = new Composite(covarij,covarij);
				ICRNReaction reaction = new CRNReactionArbitraryGUI((IComposite)covarij, products, tot[i][j],null);
				CRNImporter.addReaction(crnExpanded, (IComposite)covarij, products, reaction);
			}
		}


		return crnExpanded;
	}

	private static IComposite replaceSpecies(HashMap<String, ISpecies> speciesNameToReplacedSpecies, IComposite composite) {
		IComposite replacedComposite;
		if(composite.isUnary()){
			replacedComposite=(IComposite)speciesNameToReplacedSpecies.get(composite.getFirstReagent().getName());
		}
		else{
			HashMap<ISpecies, Integer> compositeHM=new HashMap<>(composite.getNumberOfDifferentSpecies());
			for(int i=0;i<composite.getNumberOfDifferentSpecies();i++) {
				ISpecies s = speciesNameToReplacedSpecies.get(composite.getAllSpecies(i).getName());
				compositeHM.put(s, composite.getMultiplicities(i));
			}
			replacedComposite=new Composite(compositeHM);
		}
		return replacedComposite;
	}

	/*
	public static void printLNA(ICRN crn, String name, Collection<String> preambleCommentLines, boolean verbose, String icComment,MessageConsoleStream out, BufferedWriter bwOut,IMessageDialogShower msgDialogShower) throws UnsupportedFormatException{
		String fileName = name;

		if((!crn.isMassAction()) || crn.isSymbolic()){
			CRNReducerCommandLine.printWarning(out,bwOut, true, msgDialogShower, "The LNA function can be computed only for mass-action CRNs without symbolic parameters.", DialogType.Error);
			return;
		}

		fileName=overwriteExtensionIfEnabled(fileName,".m");
		if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Writing the LNA in file "+fileName);
		}

		createParentDirectories(fileName);
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printLNA, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}

		try {

			if(preambleCommentLines!=null && preambleCommentLines.size()>0){
				for (String comment : preambleCommentLines) {
					bw.write("%"+comment+"\n");
				}
			}
			bw.write("\n\n");

//			HashMap<String,ISpecies> speciesNameToSpecies = new HashMap<>(crn.getSpecies().size());
//			for(ISpecies species : crn.getSpecies()){
//				speciesNameToSpecies.put(species.getName(), species);
//			}

			String functionName = fileName.replace(".m", "");
			int indexOfLastSep = functionName.lastIndexOf(File.separator);
			if(indexOfLastSep>=0){
				functionName=functionName.substring(indexOfLastSep+1);
			}

			ISpecies[] speciesIdToSpecies = new ISpecies[crn.getSpecies().size()];
			int s=0;
			for (ISpecies species : crn.getSpecies()) {
				speciesIdToSpecies[s]=species;
				s++;
			}

			writeLNA(crn, out, msgDialogShower, bw, functionName,true,true,false,true,"\t",speciesIdToSpecies);

		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printJacobianFunctionToMatlabFIle, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		finally{
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"Writing the Jacobian function in file "+fileName+" completed");
			}
			try {
				bw.flush();
				bw.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in printJacobianFunctionToMatlabFIle, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}	
	}
	 */

	public void printEpsCLumpAnalysisCampaignToFile(String crnName,String fileName, boolean verbose, MessageConsoleStream out, BufferedWriter bwOut,IMessageDialogShower msgDialogShower, 
			ArrayList<String> functionNames, String csvFile, boolean writeInnerScript, int cLump
			,double fromEps,double toEps,double stepEps,boolean cLumpGiven, 
			double fromSlope, double toSlope, double stepSlope, boolean slopeGiven,double tEnd) throws UnsupportedFormatException{
		/*function runAnalysisCampagin()
	    csvFile="eiModels.csv";
	    %erodesMatlabScripts=["e2maxPert0_1","e2maxPert0_2","e2maxPert0_3","e2maxPert0_4","e2maxPert0_5","e2maxPert0_6"];
	    erodesMatlabScripts=["e2maxPert0_1"];
	    for i=1:length(erodesMatlabScripts)
	        erodesMatlabScript=erodesMatlabScripts(i);
	        epsCLumpNewErrorFunction(erodesMatlabScript,csvFile);
	    end
	end*/
		
		fileName=overwriteExtensionIfEnabled(fileName,".m");
		if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Writing the analysis campaign in file "+fileName);
		}

		createParentDirectories(fileName);
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printEpsCLumpAnalysisCampaignToFile, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		
		try {
			bw.write("%Automatically generated by ERODE\n\n");

			String functionName = fileName.replace(".m", "");
			int indexOfLastSep = functionName.lastIndexOf(File.separator);
			if(indexOfLastSep>=0){
				functionName=functionName.substring(indexOfLastSep+1);
			}
			
			bw.write("function "+functionName+"()\n");
			bw.write("\tbeginTime=now;\n");
			bw.write("\tfprintf('Script invoked at time %s\\n',datestr(beginTime,'HH:MM:SS.FFF'))\n");
			bw.write("\ttEnd="+tEnd+";\n");
			bw.write("\t"+"csvFile=\""+csvFile+"\";"+"\n");
			bw.write("\t"+"%Delete old version of CSV file if it exists\n");
			bw.write("\t"+"if isfile(csvFile)\n");
			bw.write("\t\t"+"%fid = fopen(csvFile, 'w');\n");
			bw.write("\t\t"+"%fclose(fid);\n");
			bw.write("\t\t"+"delete(csvFile);\n");
			bw.write("\t"+"end\n");
			if(slopeGiven) {
				//bw.write("\t"+"max_allowed_slope_percentage="+slope+";"+"\n\n");
				bw.write("\t"+"max_allowed_slopes=");
				bw.write("("+fromSlope+":"+stepSlope+":"+toSlope+");\n");
			}
			bw.write("\t"+"erodeMatlabScripts=[");
			//\"e2maxPert0_1\",\"e2maxPert0_2\",\"e2maxPert0_3\",\"e2maxPert0_4\",\"e2maxPert0_5\",\"e2maxPert0_6\"];"+"\n
			for(int i=0;i<functionNames.size();i++) {
				bw.write("\""+functionNames.get(i)+"\"");
				if(i<functionNames.size()-1) {
					bw.write(",");
				}
			}
			bw.write("];\n");
			if(cLumpGiven) {
				bw.write("\t"+"requiredRows="+cLump+";"+"\n");
			}
			bw.write("\t"+"for i=1:length(erodeMatlabScripts)"+"\n");
			bw.write("\t"+"\terodeMatlabScript=erodeMatlabScripts(i);"+"\n");
			//bw.write("\t"+"\tepsCLumpNewErrorFunction(erodesMatlabScript,csvFile);"+"\n");
			if(cLumpGiven) {
			bw.write("\t"+"\tepsCLump(erodeMatlabScript,csvFile,requiredRows);"+"\n");
			}
			else if(slopeGiven) {
				bw.write("\t\tepsForMaxError(erodeMatlabScript,csvFile,max_allowed_slopes,tEnd)\n");
			}
			else {
				bw.write("\t"+"\tfor epsilon="+fromEps+":"+stepEps+":"+toEps+"\n");
				bw.write("\t"+"\t\tepsCLump(erodeMatlabScript,csvFile,epsilon);"+"\n");
				bw.write("\t"+"\tend"+"\n");
			}
			bw.write("\t"+"end"+"\n");
			bw.write("end\n");
			
			if(writeInnerScript) {
				bw.write("\n");
				if(cLumpGiven) {
					MatlabODEPontryaginExporter.printResource("epsCLump_eps2Clump.txt", bw,getClass());
				}
				else if(slopeGiven){
					MatlabODEPontryaginExporter.printResource("epsCLumpForMaxErrorSlope.txt", bw,getClass());
				}
				else {
					MatlabODEPontryaginExporter.printResource("epsCLumpForScriptsMoreEps.txt", bw,getClass());
				}
				bw.write("\n\n");
				MatlabODEPontryaginExporter.printResource("epsCLumpCommon.txt", bw,getClass());
				bw.write("\n\n");
			}
			
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printEpsCLumpAnalysisCampaignToFile, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		finally{
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"Writing the analysis campaign file "+fileName+" completed");
			}
			try {
				bw.flush();
				bw.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in printEpsCLumpAnalysisCampaignToFile, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}

	}
	
	public void printMainEpsCLumpScript(String otherFileName, boolean verbose, MessageConsoleStream out,
			BufferedWriter bwOut, IMessageDialogShower messageDialogShower) {
		// TODO Auto-generated method stub
		otherFileName=overwriteExtensionIfEnabled(otherFileName,".m",true);
		
		String epsCLumpFileName = otherFileName;
		int lastSep = epsCLumpFileName.lastIndexOf(File.separator);
		if(lastSep==-1) {
			epsCLumpFileName="epsCLump.m";
		}
		else {
			epsCLumpFileName=epsCLumpFileName.substring(0,lastSep+1);
			epsCLumpFileName+="epsCLump.m";
		}
		
		//if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Writing main epsCLump script in file "+epsCLumpFileName);
		//}
		
		createParentDirectories(epsCLumpFileName);
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(epsCLumpFileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printMainEpsCLumpScript, exception raised while creating the filewriter for file: "+epsCLumpFileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}

		try {
			
			String functionName = otherFileName.replace(".m", "");
			int indexOfLastSep = functionName.lastIndexOf(File.separator);
			if(indexOfLastSep>=0){
				functionName=functionName.substring(indexOfLastSep+1);
			}

			bw.write("function epsCLump()\n");
			bw.write("\t%diary('myDiary')\n");
			bw.write("\t\n");
			bw.write("\tbeginTime = now;\n");
			bw.write("\tfprintf('epsCLump invoked at time %s\\n', datestr(beginTime,'HH:MM:SS.FFF'))\n");
			bw.write("\t%J is a list of J0, J1, J2, ...\n");
			bw.write("\t%N is the number of variables\n");
			bw.write("\t%epsilon is the epsilon\n");
			bw.write("\t%M0 are the constraints to use in constrained lumping\n");
			bw.write("\t\n");
			bw.write("\t[J,N,epsilon,M0,driftName,modelName] = "+functionName+"();\n");
			bw.write("\t\n");
			MatlabODEPontryaginExporter.printResource("epsCLumpSingle.txt", bw,getClass());
			bw.write("\n\n");

			
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printMainEpsCLumpScript, exception raised while writing in the file: "+ epsCLumpFileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
		}
		finally{
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"Writing the Jacobian function in file "+epsCLumpFileName+" completed");
			}
			try {
				bw.flush();
				bw.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in printMainEpsCLumpScript, exception raised while closing the bufferedwriter of the file: "+epsCLumpFileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}	
		
	}
	
	public static void printJacobianFunctionToMatlabFIle(ICRN crn, String name, Collection<String> preambleCommentLines, boolean verbose, String icComment,MessageConsoleStream out, BufferedWriter bwOut,IMessageDialogShower msgDialogShower) throws UnsupportedFormatException{
		printJacobianFunctionToMatlabFIle(crn, name, preambleCommentLines, verbose, icComment,out, bwOut,msgDialogShower,false,-1,-1,null);
	}
	public static String printJacobianFunctionToMatlabFIle(ICRN crn, String name, 
			Collection<String> preambleCommentLines, boolean verbose, String icComment,MessageConsoleStream out, BufferedWriter bwOut,
			IMessageDialogShower msgDialogShower,boolean epsCLump,double eps,int maxPercPerturb, ArrayList<LinkedHashMap<String, Double>> M0) throws UnsupportedFormatException{
		String fileName = name;

		if((!crn.isMassAction()) || crn.isSymbolic()){
			CRNReducerCommandLine.printWarning(out,bwOut, true, msgDialogShower, "The Jacobian function can be computed only for mass-action CRNs without symbolic parameters.", DialogType.Error);
			return null;
		}
		
		ICRN crnToConsider=crn;
		try {
			if(maxPercPerturb>0) {
				crnToConsider = CRN.copyCRN(crn, out, bwOut);
				Date d = new Date();
				RandomEngine randomGenerator = new MersenneTwister(d);
				for(ICRNReaction reaction : crnToConsider.getReactions()) {
					int percentage =RandomBNG.nextInt(randomGenerator, maxPercPerturb);//nextDouble(randomGenerator, maxPercPerturb);
					//int percentage=(int) Math.round(dirtyPercentage);
					double factor=percentage/100.0;
					
					BigDecimal actualPert = BigDecimal.valueOf(factor).multiply(reaction.getRate());
					reaction.setRate(reaction.getRate().add(actualPert), reaction.getRateExpression()+"+"+actualPert.toPlainString());
				}
			}
		} catch (IOException e1) {
			throw new UnsupportedFormatException("IOException while copying the CRN: "+e1.getMessage());
		}

		fileName=overwriteExtensionIfEnabled(fileName,".m",true);
		if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Writing the Jacobian function in file "+fileName);
		}

		createParentDirectories(fileName);
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printJacobianFunctionToMatlabFIle, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return null;
		}

		try {

			if(preambleCommentLines!=null && preambleCommentLines.size()>0){
				for (String comment : preambleCommentLines) {
					bw.write("%"+comment+"\n");
				}
				bw.write("\n\n");
			}

			/*HashMap<String,ISpecies> speciesNameToSpecies = new HashMap<>(crn.getSpecies().size());
			for(ISpecies species : crn.getSpecies()){
				speciesNameToSpecies.put(species.getName(), species);
			}*/

			String functionName = fileName.replace(".m", "");
			int indexOfLastSep = functionName.lastIndexOf(File.separator);
			if(indexOfLastSep>=0){
				functionName=functionName.substring(indexOfLastSep+1);
			}
			
			String functionNameToReturn = functionName;
			
			if(epsCLump) {
				String suffix="Function";
				writeJi(crnToConsider, out, msgDialogShower, bw, functionName,functionName+suffix,true,"\t"/*,speciesIdToSpecies*/,eps,M0,maxPercPerturb);
				functionName+=suffix;
			}

//			ISpecies[] speciesIdToSpecies = new ISpecies[crn.getSpecies().size()];
//			int s=0;
//			for (ISpecies species : crn.getSpecies()) {
//				speciesIdToSpecies[s]=species;
//				s++;
//			}
			
			writeJacobianFunction(crnToConsider, out, msgDialogShower, bw, functionName,true,true,false,true,"\t"/*,speciesIdToSpecies*/);
			
			if(epsCLump) {
				String driftFile = overwriteExtensionIfEnabled(fileName, "",true);
				driftFile+="Drift.m";
				printODEsToMatlabFIle(crnToConsider, driftFile, preambleCommentLines, verbose, icComment, out,bwOut, "0","false","ode45",msgDialogShower,true);
			}
			
			return functionNameToReturn;

		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printJacobianFunctionToMatlabFIle, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return null;
		}
		finally{
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"Writing the Jacobian function in file "+fileName+" completed");
			}
			try {
				bw.flush();
				bw.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in printJacobianFunctionToMatlabFIle, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}	
	}
	
	public static String printEpsCLumpScriptsToMatlabFIle(ICRN crn, String name, 
			Collection<String> preambleCommentLines, boolean verbose, String icComment,MessageConsoleStream out, BufferedWriter bwOut,
			IMessageDialogShower msgDialogShower,int maxPercPerturb, ArrayList<LinkedHashMap<String, Double>> M0) throws UnsupportedFormatException{
		String fileName = name;

		if((!crn.isMassAction()) || crn.isSymbolic()){
			CRNReducerCommandLine.printWarning(out,bwOut, true, msgDialogShower, "The epsilonCLump script can be computed only for mass-action CRNs without symbolic parameters.", DialogType.Error);
			return null;
		}
		
		ICRN crnToConsider=crn;
		try {
			if(maxPercPerturb>0) {
				crnToConsider = RandomBNG.createRndPerturbedCopy(crn, out, bwOut, maxPercPerturb);
			}
		} catch (IOException e1) {
			throw new UnsupportedFormatException("IOException while copying the CRN: "+e1.getMessage());
		}

		fileName=overwriteExtensionIfEnabled(fileName,".m",true);
		if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Writing the Jacobian function in file "+fileName);
		}

		createParentDirectories(fileName);
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printJacobianFunctionToMatlabFIle, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return null;
		}

		try {

			if(preambleCommentLines!=null && preambleCommentLines.size()>0){
				for (String comment : preambleCommentLines) {
					bw.write("%"+comment+"\n");
				}
				bw.write("\n\n");
			}

			/*HashMap<String,ISpecies> speciesNameToSpecies = new HashMap<>(crn.getSpecies().size());
			for(ISpecies species : crn.getSpecies()){
				speciesNameToSpecies.put(species.getName(), species);
			}*/

			String functionName = fileName.replace(".m", "");
			int indexOfLastSep = functionName.lastIndexOf(File.separator);
			if(indexOfLastSep>=0){
				functionName=functionName.substring(indexOfLastSep+1);
			}
			
			String functionNameToReturn = functionName;

			String suffix="Function";
			writeJi(crnToConsider, out, msgDialogShower, bw, functionName,functionName+suffix,true,"\t"/*,speciesIdToSpecies*/,0,M0,maxPercPerturb);
			functionName+=suffix;

//			ISpecies[] speciesIdToSpecies = new ISpecies[crn.getSpecies().size()];
//			int s=0;
//			for (ISpecies species : crn.getSpecies()) {
//				speciesIdToSpecies[s]=species;
//				s++;
//			}
			
			writeJacobianFunction(crnToConsider, out, msgDialogShower, bw, functionName,true,true,false,true,"\t"/*,speciesIdToSpecies*/);
			
			String driftFile = overwriteExtensionIfEnabled(fileName, "",true);
			driftFile+="Drift.m";
			printODEsToMatlabFIle(crnToConsider, driftFile, preambleCommentLines, verbose, icComment, out,bwOut, "0","false","ode45",msgDialogShower,true);

			return functionNameToReturn;

		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printJacobianFunctionToMatlabFIle, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return null;
		}
		finally{
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"Writing the Jacobian function in file "+fileName+" completed");
			}
			try {
				bw.flush();
				bw.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in printJacobianFunctionToMatlabFIle, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}	
	}

	public void printSolveUCTMC(ICRN crn, boolean verbose, double tHoriz, boolean minimize,String modelWithSmallM,BigDecimal deltaHalf,
			LinkedHashMap<String,Double> inits, LinkedHashMap<String,Double> rRewards, LinkedHashMap<String,Double> phiRewards, MessageConsoleStream out, BufferedWriter bwOut, IMessageDialogShower msgDialogShower, Terminator terminator) {
		//String fileName = name;
		String fileName = getFileName();
		fileName=overwriteExtensionIfEnabled(fileName,".m");
		if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Writing the epsilon-bound script in file "+fileName);
		}

		createParentDirectories(fileName);
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printSolveUCTMC, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		
		
		HashMap<String,ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
		for(ISpecies species : crn.getSpecies()){
			speciesNameToSpecies.put(species.getName(), species);
		}

		try {
			//bw.write("\n\n");
			bw.write("% Automatically generated from "+crn.getName()+".\n");
			if(modelWithSmallM!=null && modelWithSmallM.length()>0) {
				bw.write("%   Reading small m from  "+modelWithSmallM+"\n");
			}
			else {
				bw.write("%   Computing small m and big M removing/adding "+deltaHalf+"\n");
			}
			bw.write("% Number of states: "+crn.getSpecies().size()+".\n");
			bw.write("% Number of transitions: "+crn.getReactions().size()+".\n");			
			bw.write("\n% Correspondence with original names:\n");
			int incr=1;
			for (ISpecies species : crn.getSpecies()) {
				bw.write("%     " +(species.getID()+incr)+" = " + ((species.getOriginalName()==null)?species.getName():species.getOriginalName())+"\n");
			}
			bw.write("\n\n");
			//bw.write("\n\n");
			
			String fName = fileName.replace(".m", "");
			fName=fName.substring(fName.lastIndexOf(File.separator)+1);
			bw.write("function "+fName+"()\n");
			bw.write("\n");
			bw.write("fprintf('Analysis started at time %s\\n', datetime)");

			bw.write("    % Model name\n");
			bw.write("    mname = '"+fName+"';\n");
			bw.write("    fprintf('We have "+ crn.getSpeciesSize()+" states\\n');");
			bw.write("\n");
			//bw.write("\n");
			//bw.write("\n\n");
			
			HashMap<ICRNReaction, BigDecimal> reactionToRateInModelSmallm=new HashMap<>(crn.getReactions().size());
			HashMap<ICRNReaction, BigDecimal> reactionToRateInModelBigM=new HashMap<>(crn.getReactions().size());
			if(modelWithSmallM!=null && modelWithSmallM.length()>0)
			{
				CRNReducerCommandLine.print(out,bwOut,"\n\tLoading the model with small 'm' transition rates from "+modelWithSmallM+" ...");
				ImporterOfSupportedNetworks importerOfSupportedNetworks=new ImporterOfSupportedNetworks();
				AbstractImporter importer=null;
				try {
					if(modelWithSmallM.endsWith(".tra")) {
						importer = importerOfSupportedNetworks.importSupportedNetwork(modelWithSmallM, false, false,SupportedFormats.MRMC,false,new String[]{"same"},out,bwOut,msgDialogShower, false,false);
					}
					else  if(modelWithSmallM.endsWith(".ode")||modelWithSmallM.endsWith("._ode")) {
						importer = importerOfSupportedNetworks.importSupportedNetwork(modelWithSmallM, false, false,SupportedFormats.CRN,false,null,out,bwOut,msgDialogShower, false,false);
					}
				} catch (UnsupportedFormatException | IOException | XMLStreamException e) {
					
					CRNReducerCommandLine.printWarning(out,bwOut,"\n\tProblems in loading the model with small m transition rates. I terminate.");
					msgDialogShower.openSimpleDialog("Problems in loading the model with small m transition rates. I terminate.\n"+modelWithSmallM, DialogType.Error);
					return;
				}
				ICRN crnSmallM = importer.getCRN();
				//partition = importer.getInitialPartition();
				for(int i=0;i<crn.getReactions().size();i++) {
					reactionToRateInModelSmallm.put(crn.getReactions().get(i), crnSmallM.getReactions().get(i).getRate());
					reactionToRateInModelBigM.put(  crn.getReactions().get(i), crn.getReactions().get(i).getRate());
				}
				CRNReducerCommandLine.print(out,bwOut," completed.");
				//TODO: here we assume that the model has same structure in file for m and M: they contain same species in order, and same reactions in same order 
			}
			else {
				CRNReducerCommandLine.print(out,bwOut,"\n\tExplictly computing m and M subtracting/adding "+deltaHalf+" ...");
				for(int i=0;i<crn.getReactions().size();i++) {
					reactionToRateInModelSmallm.put(crn.getReactions().get(i), crn.getReactions().get(i).getRate().subtract(deltaHalf));
					reactionToRateInModelBigM.put(crn.getReactions().get(i), crn.getReactions().get(i).getRate().add(deltaHalf));
				}
				CRNReducerCommandLine.print(out,bwOut," completed.");
			}
			
			CRNReducerCommandLine.print(out,bwOut,"\n\tCreating m.");
			Matrix matrix_smallm = new Matrix(0, crn.getSpecies());
			for(ICRNReaction reaction : crn.getReactions()) {
				//source=row, target=column: a_{r,c} is the prob of r going to c 
				ISpecies rs = reaction.getReagents().getFirstReagent();
				ISpecies ct = reaction.getProducts().getFirstReagent();
				matrix_smallm.setValue(rs, ct, reactionToRateInModelSmallm.get(reaction).doubleValue());
			}
			CRNReducerCommandLine.print(out,bwOut,"\n\tWriting m.");
			bw.write("    m = zeros("+crn.getSpeciesSize()+");\n");
			Set<Entry<Pair, Double>> data = matrix_smallm.getNonDefaultData();
			for(Entry<Pair, Double> entry : data) {
				int r=entry.getKey().getFirst().getFirstReagent().getID()+1;
				int c=entry.getKey().getSecond().getFirstReagent().getID()+1;
				bw.write("    m("+r+","+c+")="+entry.getValue()+";\n");
			}
//			bw.write("    m = [");
//			for(int r=0;r<crn.getSpeciesSize();r++) {
//				for(int c=0;c<crn.getSpeciesSize();c++) {
//					bw.write(" "+matrix_smallm.getValue(r, c));
//				}
//				bw.write(";");
//			}
//			//bw.write("];");
//			bw.write("];\n");
			matrix_smallm=null;
			
			CRNReducerCommandLine.print(out,bwOut,"\n\tCreating M.");
			Matrix matrix_bigm = new Matrix(0, crn.getSpecies());
			for(ICRNReaction reaction : crn.getReactions()) {
				//source=row, target=column: a_{r,c} is the prob of r going to c 
				ISpecies rs = reaction.getReagents().getFirstReagent();
				ISpecies ct = reaction.getProducts().getFirstReagent();
				matrix_bigm.setValue(rs, ct, reactionToRateInModelBigM.get(reaction).doubleValue());//matrix_bigm.setValue(rs, ct, reaction.getRate().doubleValue());
			}
			CRNReducerCommandLine.print(out,bwOut,"\n\tWriting M.");
			bw.write("    M = zeros("+crn.getSpeciesSize()+");\n");
			data = matrix_bigm.getNonDefaultData();
			for(Entry<Pair, Double> entry : data) {
				int r=entry.getKey().getFirst().getFirstReagent().getID()+1;
				int c=entry.getKey().getSecond().getFirstReagent().getID()+1;
				bw.write("    M("+r+","+c+")="+entry.getValue()+";\n");
			}
//			bw.write("    M = [");
//			for(int r=0;r<crn.getSpeciesSize();r++) {
//				for(int c=0;c<crn.getSpeciesSize();c++) {
//					bw.write(" "+matrix_bigm.getValue(r, c));
//				}
//				bw.write(";");
//			}
//			bw.write("];\n");
			matrix_bigm=null;
			data=null;
//			StringBuilder m = new StringBuilder("    m = [");
//			StringBuilder M = new StringBuilder("    M = [");
//			for(int r=0;r<crn.getSpeciesSize();r++) {
//				for(int c=0;c<crn.getSpeciesSize();c++) {
//					m.append(" "+matrix_smallm.getValue(r, c));
//					M.append(" "+matrix_bigm.getValue(r, c));
//				}
//				//bw.write(";");
//				m.append(";");
//				M.append(";");
//			}
//			//bw.write("];");
//			m.append("];\n");
//			M.append("];\n");
//			
//			bw.write(m.toString());
//			bw.write(M.toString());
			bw.write("\n");
			
			bw.write("    %r and phi are costs of being in a state (running and final, resp). Some of Prism's rewards can be interpreted as 'r'\n");
			bw.write("    %r are the running costs. For prism models, we can encode one reward structure into it (those adding constants for each state with a given label\n");
			bw.write("    r = [");
			for(ISpecies s : crn.getSpecies()) {
				writeValue(rRewards, bw, s);
			}
			bw.write("]';\n");

			bw.write("    %phi are the final costs\n");
			bw.write("    phi = [");
			for(ISpecies s : crn.getSpecies()) {
				writeValue(phiRewards, bw, s);
			}
			bw.write("]';\n");
			bw.write("\n");
			
			bw.write("    %pi0 is the initial distribution of probability of being in the states\n");
			
			bw.write("    pi0 = [");
			for(ISpecies s : crn.getSpecies()) {
				writeValue(inits, bw, s);
			}
			bw.write("]';	%initial state. Take the init from lab\n");
			bw.write("\n");
			
			bw.write("    T = "+tHoriz+";\n");
			if(minimize)
				bw.write("    minimize = 1;\n");
			else
				bw.write("    minimize = 0;\n");
			bw.write("\n");
			/*
% Dummy function demonstrating the actual routine
function UCTMC()

    % Dummy test inputs
    
    % Model name
    mname = 'fifi';
    
    % Original UCTMC
%     m = [0 1 1; 1 0 0; 1 0 0];
%     M = [0 1 1; 2 0 1; 2 1 0];
%     r = [0 0 0]';
%     phi = [0 1 1]';
%     pi0 = [1,0,0]';
    % Lumped UCTMC
    m = [ [0 2]; [1 0] ];
    M = [ [0 2]; [2 0] ];
    %r and phi are costs of being in a state (running and final, resp). We always keep r to 0.
    %for cluster, we could make r=1 for all states !minimum, and phi=0
    r = [0 0]';
    phi = [0 1]';
    pi0 = [1,0]';	%initial state. take the 'init' from lab
    
    % Time horizon and min/max     
    T = 3;
    minimize = 1; 
  
  
			 */
//aaaa			
			
			MatlabODEPontryaginExporter.printResource("solveUCTMC.txt", bw,getClass());
			
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printSolveUCTMC, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		finally{
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"Writing the script for epsilon bounds in file "+fileName+" completed");
			}
			try {
				bw.flush();
				bw.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in printSolveUCTMC, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}	
	}

	public void writeValue(LinkedHashMap<String, Double> speciesToValue, BufferedWriter bw, ISpecies s) throws IOException {
		Double value= speciesToValue.get(s.getName());
		if(value==null) {
			bw.write(" 0");
		}
		else {
			bw.write(" "+value);
		}
	}
	
	public void printEpsilonBoundScriptToMatlabFIle(ICRN crn, IPartition initial, String name, /*Collection<String> preambleCommentLines,*/ 
			boolean verbose, /*String icComment,*/MessageConsoleStream out, BufferedWriter bwOut, IMessageDialogShower msgDialogShower, 
			double tEnd, double deltat, LinkedHashSet<String> paramsToPerturb, Terminator terminator,double epsilon, double defIC, 
			String prePartitionUserDefined, String prePartitionWRTIC, boolean forward, boolean backward) throws UnsupportedFormatException{
		//forward=false;
		//backward=true;

		String fileName = name;
		BigDecimal defaultIC = BigDecimal.valueOf(defIC);

		if(crn.isSymbolic()){
			CRNReducerCommandLine.printWarning(out,bwOut, true, msgDialogShower, "The epsilon-bound script can be created only for models without symbolic parameters.", DialogType.Error);
			return;
		}

		if(!crn.isMassAction()){
			ArrayList<HashSet<ISpecies>> userDefinedPartition = crn.getUserDefinedPartition();
			HashMap<String,ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
			for (ISpecies species : crn.getSpecies()) {
				speciesNameToSpecies.put(species.getName(), species);
			}
			CRNandPartition crnAndSpeciesAndPartition = MatlabODEPontryaginExporter.computeRNEncoding(crn, out, bwOut, false,initial,true);
			if(crnAndSpeciesAndPartition==null||crnAndSpeciesAndPartition.getCRN()==null){
				return;
			}
			else{
				crn = crnAndSpeciesAndPartition.getCRN();
				crn.setUserDefinedPartition(userDefinedPartition);

				initial = crnAndSpeciesAndPartition.getPartition();

			}
		}

		/*if((!crn.isMassAction()) || crn.isSymbolic()){
			CRNReducerCommandLine.printWarning(out,bwOut, true, msgDialogShower, "The epsilon-bound script can be created only for mass-action CRNs without symbolic parameters.", DialogType.Error);
			return;
		}*/

		fileName=overwriteExtensionIfEnabled(fileName,".m");
		if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Writing the epsilon-bound script in file "+fileName);
		}

		createParentDirectories(fileName);
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printEpsilonBoundScriptToMatlabFIle, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}

		try {

			parseParametersToPerturb(crn, paramsToPerturb);

			HashMap<String,ISpecies> speciesNameToExpandedSpecies = new HashMap<>(crn.getSpecies().size());
			ICRN crnExpanded = expandCRN(false,crn, paramsToPerturb, speciesNameToExpandedSpecies);

			/*
			if(preambleCommentLines!=null && preambleCommentLines.size()>0){
				for (String comment : preambleCommentLines) {
					bw.write("%"+comment+"\n");
				}
			}
			bw.write("\n\n");
			 */

			String linearizationFunctionName = fileName.replace(".m", "");
			int indexOfLastSep = linearizationFunctionName.lastIndexOf(File.separator);
			if(indexOfLastSep>=0){
				linearizationFunctionName=linearizationFunctionName.substring(indexOfLastSep+1);
			}

			//Write the model independent code
			writeModelIndependentCodeForEpsilonBoundScript(bw,tEnd,deltat,paramsToPerturb,linearizationFunctionName,crnExpanded,crn.getSpecies(),defaultIC,forward,backward);

			bw.write("\n\n");
			//bw.write("%ODEs of model +"crn.getName()+" ("+crn.getName()4+" species and 4 reactions)\n");


			//Write the ODEs
			int idIncrement=1;
			HashMap<ISpecies, StringBuilder> speciesToExpandedDriftSB = writeODEFunction(idIncrement, crnExpanded, bw, speciesNameToExpandedSpecies,"    ",false,false,true,true,true,null,"ode");

			bw.write("\n\n");

//			ISpecies[] expandedSpeciesIdToSpecies = new ISpecies[crnExpanded.getSpecies().size()];
//			for (ISpecies iSpecies : crnExpanded.getSpecies()) {
//				expandedSpeciesIdToSpecies[iSpecies.getID()]=iSpecies;
//			}
			// Largest coefficient other info about the expanded CRN
			HashMap<ISpecies, ArrayList<IMonomial>> speciesToExpandedMonomials = EpsilonDifferentialEquivalences.computeSpeciesMonomials(crnExpanded, speciesToExpandedDriftSB, speciesNameToExpandedSpecies,"y"/*,expandedSpeciesIdToSpecies*/);

			//BigDecimal maxSumOfCoeffOfDegreeThreeMonomials = BigDecimal.ZERO;
			BigDecimal maxCoeff = BigDecimal.ZERO;
			int totMaxDegree=0;
			int totNumberOfMonomialsOfMaxDegree = 0;
			ISpecies totSpecies=null;

			for (ISpecies species : crn.getSpecies()) {
				//BigDecimal sumOfCoeffOfDegreeThreeMonomials = BigDecimal.ZERO;
				ArrayList<IMonomial> monomials = speciesToExpandedMonomials.get(species);
				if(monomials!=null){
					int maxDegree=0;
					int numberOfMonomialsOfMaxDegree = 0;
					for (IMonomial monomial : monomials) {
						BigDecimal coeff = monomial.getOrComputeCoefficient();
						if(coeff.compareTo(maxCoeff)>0){
							maxCoeff=coeff;
						}

						HashMap<ISpecies, Integer> vars = monomial.getOrComputeSpecies();
						Integer degree = computeDegree(vars);
						if(degree>=2){
							if(degree>maxDegree){
								maxDegree=degree;
								numberOfMonomialsOfMaxDegree=1;
							}
							else if(degree==maxDegree){
								numberOfMonomialsOfMaxDegree++;
							} 
						}
						/*if(degree==3){
							sumOfCoeffOfDegreeThreeMonomials = sumOfCoeffOfDegreeThreeMonomials.add(coeff); 
						}*/
					}

					if(numberOfMonomialsOfMaxDegree>totNumberOfMonomialsOfMaxDegree){
						totMaxDegree=maxDegree;
						totNumberOfMonomialsOfMaxDegree=numberOfMonomialsOfMaxDegree;
						totSpecies=species;
					}
				}
				/*if(maxSumOfCoeffOfDegreeThreeMonomials.compareTo(sumOfCoeffOfDegreeThreeMonomials)<0){
					maxSumOfCoeffOfDegreeThreeMonomials=sumOfCoeffOfDegreeThreeMonomials;
				}*/
			}

			if(totSpecies!=null){
				String message = "\nMax-max number of monomials = "+totNumberOfMonomialsOfMaxDegree+" (from degree "+totMaxDegree+" of species "+totSpecies.getName()+").\nMax coefficient = "+maxCoeff;
				CRNReducerCommandLine.println(out,bwOut, message);
			}
			else{
				String message = "\nMax-max number of monomials = 0. Max coefficient = "+maxCoeff;
				CRNReducerCommandLine.println(out,bwOut, message);
			}
			//CRNReducerCommandLine.println(out,bwOut, "Max sum of coefficients of degree three monomials: "+maxSumOfCoeffOfDegreeThreeMonomials);



			//Write the jacobian
			String functionName = "jacobian";
			writeJacobianFunction(crnExpanded, out, msgDialogShower, bw, functionName,true,false,false,true,"\t"/*,expandedSpeciesIdToSpecies*/);

			CRNReducerCommandLine.println(out,bwOut," completed");

			//boolean computeDE=false;
			if(computeDE){
				if(forward){
					CRNReducerCommandLine.print(out,bwOut,"Computing the "+(epsilon)+"-FDE and writing the obtained linear equations ...");
					//Compute the epsilon FDE, and write the linear system of constraints
					computeEpsilonFDEAndWriteLinearSystemOfConstraints(crn, initial,out,bwOut, msgDialogShower, bw, "closest_fde",/*functionName,*/verbose,terminator,epsilon/*,defaultIC*/,paramsToPerturb,prePartitionUserDefined,prePartitionWRTIC);
					CRNReducerCommandLine.println(out,bwOut," completed");

					bw.write("\n");
					bw.write("\n");
					bw.write("\n");
				}
				if(backward){
					CRNReducerCommandLine.print(out,bwOut,"Computing the "+(epsilon)+"-BDE and writing the obtained linear equations ...");
					//Compute the epsilon BDE, and write the linear system of constraints
					computeEpsilonBDEAndWriteLinearSystemOfConstraints(crn, initial,out, bwOut,msgDialogShower, bw, "closest_bde",verbose,terminator,epsilon,defaultIC,paramsToPerturb,prePartitionUserDefined,prePartitionWRTIC);
					bw.write("\n");
				}
			}
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printEpsilonBoundScriptToMatlabFIle, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		finally{
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"Writing the script for epsilon bounds in file "+fileName+" completed");
			}
			try {
				bw.flush();
				bw.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in printEpsilonBoundScriptToMatlabFIle, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}	

	}

	public static void parseParametersToPerturb(ICRN crn, LinkedHashSet<String> paramsToPerturb) {
		if(paramsToPerturb!=null && paramsToPerturb.size()==1){
			String first = paramsToPerturb.iterator().next(); 
			if(first.equals("ALL")){
				paramsToPerturb.clear();
				for (String p : crn.getParameters()) {
					String[] nameAndVal = p.split("\\ ");
					paramsToPerturb.add(nameAndVal[0]);
				}
			}
			else if(first.equals("NONE")){
				paramsToPerturb.clear();
			}
		}
	}

	public static void printEpsilonScriptToMatlabFIle(ICRN crn, IPartition initial, String name, boolean verbose, MessageConsoleStream out, BufferedWriter bwOut, 
			IMessageDialogShower msgDialogShower, LinkedHashSet<String> paramsToPerturb, Terminator terminator,double epsilon, String prePartitionUserDefined, 
			String prePartitionWRTIC, boolean forward, boolean backward) throws UnsupportedFormatException{
		String fileName = name;

		fileName=overwriteExtensionIfEnabled(fileName,".m");
		if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Writing the epsilon script in file "+fileName);
		}

		createParentDirectories(fileName);
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printEpsilonScriptToMatlabFIle, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}

		String functionName = fileName.replace(".m", "");
		int indexOfLastSep = functionName.lastIndexOf(File.separator);
		if(indexOfLastSep>=0){
			functionName=functionName.substring(indexOfLastSep+1);
		}

		parseParametersToPerturb(crn, paramsToPerturb);

		try {
			if(forward){
				CRNReducerCommandLine.print(out,bwOut,"\nComputing the "+(epsilon)+"-FDE and writing the obtained linear equations ...");
				//Compute the epsilon FDE, and write the linear system of constraints
				computeEpsilonFDEAndWriteLinearSystemOfConstraints(crn, initial,out,bwOut, msgDialogShower, bw, functionName,verbose,terminator,epsilon/*,defaultIC*/,paramsToPerturb,prePartitionUserDefined,prePartitionWRTIC);
				CRNReducerCommandLine.println(out,bwOut," completed");

				bw.write("\n");
				bw.write("\n");
				bw.write("\n");
			}
			if(backward){
				CRNReducerCommandLine.print(out,bwOut,"\nComputing the "+(epsilon)+"-BDE and writing the obtained linear equations ...");
				//Compute the epsilon BDE, and write the linear system of constraints
				computeEpsilonBDEAndWriteLinearSystemOfConstraints(crn, initial,out, bwOut,msgDialogShower, bw, functionName,verbose,terminator,epsilon,BigDecimal.ZERO,paramsToPerturb, prePartitionUserDefined, prePartitionWRTIC);
				bw.write("\n");
			}

		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printEpsilonScriptToMatlabFIle, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		finally{
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"Writing the script for epsilon bounds in file "+fileName+" completed");
			}
			try {
				bw.flush();
				bw.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in printEpsilonScriptToMatlabFIle, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}



		/*
//		if((!crn.isMassAction()) || crn.isSymbolic()){
//			CRNReducerCommandLine.printWarning(out,bwOut, true, msgDialogShower, "The Jacobian function can be computed only for mass-action CRNs without symbolic parameters.", DialogType.Error);
//			return;
//		}

		fileName=overwriteExtensionIfEnabled(fileName,".m");
		if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Writing the epsilon script in file "+fileName);
		}

		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printEpsilonScriptToMatlabFIle, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}

		try {

			HashMap<String,ISpecies> speciesNameToExpandedSpecies = new HashMap<>(crn.getSpecies().size());


			ICRN crnExpanded = expandCRN(crn, paramsToPerturb, speciesNameToExpandedSpecies);

//			if(preambleCommentLines!=null && preambleCommentLines.size()>0){
//				for (String comment : preambleCommentLines) {
//					bw.write("%"+comment+"\n");
//				}
//			}
//			bw.write("\n\n");

			String linearizationFunctionName = fileName.replace(".m", "");
			int indexOfLastSep = linearizationFunctionName.lastIndexOf(File.separator);
			if(indexOfLastSep>=0){
				linearizationFunctionName=linearizationFunctionName.substring(indexOfLastSep+1);
			}

			//Write the model independent code
			writeModelIndependentCode(bw,tEnd,deltat,paramsToPerturb,linearizationFunctionName,crnExpanded,crn.getSpecies(),defaultIC,forward,backward);

			bw.write("\n\n");
			//bw.write("%ODEs of model +"crn.getName()+" ("+crn.getName()4+" species and 4 reactions)\n");


			//Write the ODEs
			HashMap<ISpecies, StringBuilder> speciesToExpandedDriftSB = writeODEFunction(crnExpanded, bw, speciesNameToExpandedSpecies,"    ",false,false);

			bw.write("\n\n");


			// Largest coefficient other info about the expanded CRN
			HashMap<ISpecies, ArrayList<IMonomial>> speciesToExpandedMonomials = EpsilonDifferentialEquivalences.computeSpeciesMonomials(crnExpanded, speciesToExpandedDriftSB, speciesNameToExpandedSpecies);

			//BigDecimal maxSumOfCoeffOfDegreeThreeMonomials = BigDecimal.ZERO;
			BigDecimal maxCoeff = BigDecimal.ZERO;
			int totMaxDegree=0;
			int totNumberOfMonomialsOfMaxDegree = 0;
			ISpecies totSpecies=null;

			for (ISpecies species : crn.getSpecies()) {
				//BigDecimal sumOfCoeffOfDegreeThreeMonomials = BigDecimal.ZERO;
				ArrayList<IMonomial> monomials = speciesToExpandedMonomials.get(species);
				if(monomials!=null){
					int maxDegree=0;
					int numberOfMonomialsOfMaxDegree = 0;
					for (IMonomial monomial : monomials) {
						BigDecimal coeff = monomial.getOrComputeCoefficient();
						if(coeff.compareTo(maxCoeff)>0){
							maxCoeff=coeff;
						}

						HashMap<ISpecies, Integer> vars = monomial.getOrComputeSpecies();
						Integer degree = computeDegree(vars);
						if(degree>=2){
							if(degree>maxDegree){
								maxDegree=degree;
								numberOfMonomialsOfMaxDegree=1;
							}
							else if(degree==maxDegree){
								numberOfMonomialsOfMaxDegree++;
							} 
						}
//						if(degree==3){
//							sumOfCoeffOfDegreeThreeMonomials = sumOfCoeffOfDegreeThreeMonomials.add(coeff); 
//						}
					}

					if(numberOfMonomialsOfMaxDegree>totNumberOfMonomialsOfMaxDegree){
						totMaxDegree=maxDegree;
						totNumberOfMonomialsOfMaxDegree=numberOfMonomialsOfMaxDegree;
						totSpecies=species;
					}
				}
//				if(maxSumOfCoeffOfDegreeThreeMonomials.compareTo(sumOfCoeffOfDegreeThreeMonomials)<0){
//					maxSumOfCoeffOfDegreeThreeMonomials=sumOfCoeffOfDegreeThreeMonomials;
//				}
			}

			if(totSpecies!=null){
				String message = "\nMax-max number of monomials = "+totNumberOfMonomialsOfMaxDegree+" (from degree "+totMaxDegree+" of species "+totSpecies.getName()+").\nMax coefficient = "+maxCoeff;
				CRNReducerCommandLine.println(out,bwOut, message);
			}
			else{
				String message = "\nMax-max number of monomials = 0. Max coefficient = "+maxCoeff;
				CRNReducerCommandLine.println(out,bwOut, message);
			}
			//CRNReducerCommandLine.println(out,bwOut, "Max sum of coefficients of degree three monomials: "+maxSumOfCoeffOfDegreeThreeMonomials);



			//Write the jacobian
			String functionName = "jacobian";
			writeJacobianFunction(crnExpanded, out, msgDialogShower, bw, functionName,true,false,false);

			CRNReducerCommandLine.println(out,bwOut," completed");

			//boolean computeDE=false;
			if(computeDE){
				if(forward){
					CRNReducerCommandLine.print(out,bwOut,"Computing the "+(epsilon)+"-FDE and writing the obtained linear equations ...");
					//Compute the epsilon FDE, and write the linear system of constraints
					computeEpsilonFDEAndWriteLinearSystemOfConstraints(crn, initial,out,bwOut, msgDialogShower, bw, functionName,verbose,terminator,epsilon,defaultIC,paramsToPerturb,prePartitionUserDefined,prePartitionWRTIC);
					CRNReducerCommandLine.println(out,bwOut," completed");

					bw.write("\n");
					bw.write("\n");
					bw.write("\n");
				}
				if(backward){
					CRNReducerCommandLine.print(out,bwOut,"Computing the "+(epsilon)+"-BDE and writing the obtained linear equations ...");
					//Compute the epsilon BDE, and write the linear system of constraints
					computeEpsilonBDEAndWriteLinearSystemOfConstraints(crn, initial,out, bwOut,msgDialogShower, bw, functionName,verbose,terminator,epsilon,defaultIC,paramsToPerturb);
					bw.write("\n");
				}
			}
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printEpsilonBoundScriptToMatlabFIle, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		finally{
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"Writing the script for epsilon bounds in file "+fileName+" completed");
			}
			try {
				bw.flush();
				bw.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in printEpsilonBoundScriptToMatlabFIle, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}	
		 */
	}

	/*public static ICRN expandCRN(ICRN crn, LinkedHashSet<String> paramsToPerturb,
			HashMap<String, ISpecies> speciesNameToExpandedSpecies) throws UnsupportedFormatException {
		return expandCRN(crn,  paramsToPerturb, speciesNameToExpandedSpecies,true);
	}*/

	public static ICRN expandCRN(boolean addParams,ICRN crn, LinkedHashSet<String> paramsToPerturb,
			HashMap<String, ISpecies> speciesNameToExpandedSpecies/*,boolean eliminateRemainingRates*/) throws UnsupportedFormatException {
		return expandCRN(addParams,crn, paramsToPerturb,speciesNameToExpandedSpecies,false,false);
	}

	public static ICRN expandCRN(boolean addParams,ICRN crn, LinkedHashSet<String> paramsToTransformInSpecies,
			HashMap<String, ISpecies> speciesNameToExpandedSpecies/*,boolean eliminateRemainingRates*/, boolean ignoreRemainingRates,boolean simpleRateParamOrConstant) throws UnsupportedFormatException{

		MathEval math = new MathEval();
		ICRN crnExpanded = new CRN(crn.getName(),math,crn.getOut(),crn.getBWOut());
		crnExpanded.setMdelDefKind(crn.getMdelDefKind());
		//CRN.copySpecies(crn, crnExpanded);
		for (ISpecies species : crn.getSpecies()) {
			ISpecies newSpecies = species.cloneWithoutReactions();
			newSpecies.setInitialConcentration(species.getInitialConcentration(), species.getInitialConcentration().toPlainString());
			crnExpanded.addSpecies(newSpecies);
			speciesNameToExpandedSpecies.put(newSpecies.getName(), newSpecies);
		}

		/*
		for(ISpecies species : crnExpanded.getSpecies()){
			speciesNameToExpandedSpecies.put(species.getName(), species);
		}
		 */

		if(!ignoreRemainingRates) {
			//Copy the parameters that do not have to be perturbed
			for (String p : crn.getParameters()) {
				String[] nameAndVal = p.split("\\ ");
				if(!paramsToTransformInSpecies.contains(nameAndVal[0])){
					AbstractImporter.addParameter(nameAndVal[0], nameAndVal[1], true, crnExpanded);
//					crnExpanded.addParameter(nameAndVal[0], nameAndVal[1]);
//					math.setVariable(nameAndVal[0], Double.valueOf(nameAndVal[1]));
				}
			}
		}

		//Copy the species
		/*
		int id=0;
		for (ISpecies species : crn.getSpecies()) {
			crnExpanded.addSpecies(species);
			id++;
		}
		 */
		//boolean addParams = true;
		String prefixNewPar = "p_";
		int id=crnExpanded.getSpecies().size();
		//Transform the parameters to be perturbed in species
		HashMap<String, ISpecies> parameterToCorrespondingSpecies = new HashMap<>(paramsToTransformInSpecies.size());
		for (String p : paramsToTransformInSpecies) {
			double val = crn.getMath().evaluate(p);
			ISpecies sp =new Species(p, id, BigDecimal.valueOf(val), String.valueOf(val),false);
			crnExpanded.addSpecies(sp);
			parameterToCorrespondingSpecies.put(p, sp);
			id++;
			speciesNameToExpandedSpecies.put(p, sp);

			if(addParams) {
				//crnExpanded.addParameter(prefixNewPar+p, "1");
				AbstractImporter.addParameter(prefixNewPar+p, "1", true, crnExpanded);
			}
		}
		//Copy the reactions transforming the parameters to be perturbed in species
		for(ICRNReaction reaction : crn.getReactions()){
			if(reaction.hasArbitraryKinetics()) {
				//This is the simple case where I don't have to do anything.
				IComposite reagents = reaction.getReagents();
				IComposite products = reaction.getProducts();
				HashMap<ISpecies, Integer> expandedReagents = toHashMapReplacingSpeciesWithExpandedSpecies(speciesNameToExpandedSpecies,reagents);
				HashMap<ISpecies, Integer> expandedProducts = toHashMapReplacingSpeciesWithExpandedSpecies(speciesNameToExpandedSpecies,products);
				IComposite expandedReag = CRN.compositeOrSpecies(expandedReagents);
				IComposite expandedProd = CRN.compositeOrSpecies(expandedProducts);
				ICRNReaction expandedReaction=null;
				try {
					expandedReaction = new CRNReactionArbitraryGUI(expandedReag, expandedProd , reaction.getRateExpression(), reaction.getID());
				}catch(IOException e) {
					throw new UnsupportedFormatException("Problems in parsing the rate of this arbitrary reaction: "+reaction.toString());
				}
				//crnExpanded.addReaction(expandedReaction);
				CRNImporter.addReaction(crnExpanded, expandedReag, expandedProd, expandedReaction);
			}
			else {
				IComposite reagents = reaction.getReagents();
				IComposite products = reaction.getProducts();
				
				List<HashMap<String, Integer>> curriedParamsToAddAsCatalystsForEachMon = new ArrayList<HashMap<String,Integer>>();
				List<BigDecimal> rateWithoutParametersToPerturbAndWithEvaluatedParametersForEachMon = new ArrayList<BigDecimal>();
				
				
				//HashMap<String, Integer> curriedParamsToAddAsCatalysts = new HashMap<String, Integer>(1);
				//BigDecimal rateWithoutParametersToPerturbAndWithEvaluatedParameters;
				if(simpleRateParamOrConstant) {
					String r = reaction.getRateExpression();
					ISpecies paramToSp = parameterToCorrespondingSpecies.get(r);
					if(paramToSp!=null) {
						rateWithoutParametersToPerturbAndWithEvaluatedParametersForEachMon.add(BigDecimal.ONE);
						HashMap<String, Integer> curriedParamsToAddAsCatalysts = new HashMap<String, Integer>(1);
						curriedParamsToAddAsCatalysts.put(paramToSp.getName(), 1);
						curriedParamsToAddAsCatalystsForEachMon.add(curriedParamsToAddAsCatalysts);
						//rateWithoutParametersToPerturbAndWithEvaluatedParameters=BigDecimal.ONE;
						//curriedParamsToAddAsCatalysts.put(paramToSp.getName(), 1);
					}
					else {
						//The reaction had a constant rate
						rateWithoutParametersToPerturbAndWithEvaluatedParametersForEachMon.add(reaction.getRate());
						//rateWithoutParametersToPerturbAndWithEvaluatedParameters=reaction.getRate();
					}
				}
				else {
					ASTNode rate;
					try {
						rate = ASTNode.parseFormula(reaction.getRateExpression());
					} catch (ParseException e) {
						throw new UnsupportedFormatException("Problems in parsing the rate "+reaction.getRateExpression());
					}
					rateWithoutParametersToPerturbAndWithEvaluatedParametersForEachMon = transformParameterInSpecies(rate,parameterToCorrespondingSpecies,curriedParamsToAddAsCatalystsForEachMon,speciesNameToExpandedSpecies,math);
				}
				
				/* 
				if(rateWithoutParametersToPerturbAndWithEvaluatedParametersForEachMon.size()>1) {
					//there might be unexpected consequences in applying currying to a mass-action reaction whose rate consists of an expression with more monomials.
				}
				*/
				for(int r=0;r<rateWithoutParametersToPerturbAndWithEvaluatedParametersForEachMon.size();r++) {
					HashMap<ISpecies, Integer> expandedReagents = toHashMapReplacingSpeciesWithExpandedSpecies(speciesNameToExpandedSpecies,reagents);
					HashMap<ISpecies, Integer> expandedProducts = toHashMapReplacingSpeciesWithExpandedSpecies(speciesNameToExpandedSpecies,products);

					HashMap<String, Integer> curriedParamsToAddAsCatalysts = curriedParamsToAddAsCatalystsForEachMon.get(r);
					BigDecimal rateWithoutParametersToPerturbAndWithEvaluatedParameters= rateWithoutParametersToPerturbAndWithEvaluatedParametersForEachMon.get(r);

					String rateExpr = rateWithoutParametersToPerturbAndWithEvaluatedParameters.toPlainString();
					for (Entry<String, Integer> entry : curriedParamsToAddAsCatalysts.entrySet()) {
						ISpecies entrysp = parameterToCorrespondingSpecies.get(entry.getKey());
						add(expandedReagents, entry.getValue(), entrysp);
						add(expandedProducts, entry.getValue(), entrysp);

						if(addParams) {
							for(int i=0;i<entry.getValue();i++) {
								rateExpr += " * " + prefixNewPar+entrysp.getName();
							}
						}
					}
					//String expr = rate.toFormula();
					//BigDecimal val = BigDecimal.valueOf(math.evaluate(expr));
					//			IComposite expandedReag = crnExpanded.addReagentsIfNew(new Composite(expandedReagents));
					//			IComposite expandedProd = crnExpanded.addProductIfNew(new Composite(expandedProducts));
					IComposite expandedReag = CRN.compositeOrSpecies(expandedReagents);
					IComposite expandedProd = CRN.compositeOrSpecies(expandedProducts);



					//			if(addParams) {
					//				//rateExpr = "p"+reaction.getRateExpression();//THIS IS WRONG
					//			}

					//ICRNReaction expandedReaction = new CRNReaction(rateWithoutParametersToPerturbAndWithEvaluatedParameters, expandedReag, expandedProd, rateWithoutParametersToPerturbAndWithEvaluatedParameters.toPlainString(),null);
					ICRNReaction expandedReaction = new CRNReaction(rateWithoutParametersToPerturbAndWithEvaluatedParameters, expandedReag, expandedProd, rateExpr,reaction.getID());
					//crnExpanded.addReaction(expandedReaction);
					CRNImporter.addReaction(crnExpanded, expandedReag, expandedProd, expandedReaction);
				}
			}
		}
		//}
		return crnExpanded;
	}

	private static HashMap<ISpecies, Integer> toHashMapReplacingSpeciesWithExpandedSpecies(
			HashMap<String, ISpecies> speciesNameToExpandedSpecies, IComposite composite) {
		HashMap<ISpecies, Integer> compositeOfExpandedSpecies = new HashMap<>(composite.getNumberOfDifferentSpecies()+1);
		for(int i=0;i<composite.getNumberOfDifferentSpecies();i++) {
			ISpecies oldSpecies = composite.getAllSpecies(i);
			int mult = composite.getMultiplicities(i);
			ISpecies newSpecies = speciesNameToExpandedSpecies.get(oldSpecies.getName());
			compositeOfExpandedSpecies.put(newSpecies, mult);
		}
		return compositeOfExpandedSpecies;
	}

	private static int computeDegree(HashMap<ISpecies, Integer> vars) {
		int d = 0;
		for (Integer i : vars.values()) {
			d+=i;
		}
		return d;
	}

	private static void computeEpsilonBDEAndWriteLinearSystemOfConstraints(ICRN crn, IPartition initial,MessageConsoleStream out,BufferedWriter bwOut,
			IMessageDialogShower msgDialogShower, BufferedWriter bw, String functionName, boolean verbose,Terminator terminator,double epsilon,BigDecimal defaultIC, LinkedHashSet<String> paramsToPerturb, String prePartitionUserDefined, String prePartitionWRTIC) throws UnsupportedFormatException, IOException {


		/*
		 function closest_bde()

    % The linear system to be satisfied for BDE
    A = [ 
            [1, 3, 2]; 
            [2, 5, 6]; 
            [4, 7, 1]; 
        ];

    % The parameters of the reference trajectory
    p0 = [1, 1, 1];

    % Quadratic programming

    % To be done
end
		 */

		EpsilonDifferentialEquivalences epsilonDE = new EpsilonDifferentialEquivalences();
		long begin = System.currentTimeMillis();

		if(prePartitionWRTIC!=null && prePartitionWRTIC.equalsIgnoreCase("true")){
			initial=ExactFluidBisimilarity.prepartitionWRTIC(crn,initial,false,out,bwOut,terminator);
		}
		if(prePartitionUserDefined!=null && prePartitionUserDefined.equalsIgnoreCase("true")){
			initial= CRNBisimulationsNAry.prepartitionUserDefined(crn, false, out,bwOut,terminator);
		}


		IPartitionAndBoolean obtainedPartitionAndBool = epsilonDE.computeCoarsest(Reduction.ENBB, BigDecimal.valueOf(epsilon), crn, initial, verbose, out,bwOut, terminator,false);
		long end = System.currentTimeMillis();
		double time = (end-begin)/1000.0;
		String timeString = String.format( CRNReducerCommandLine.MSFORMAT, (time) );
		String msg=" eps-BDE time "+timeString+ " (s) ... ";
		CRNReducerCommandLine.print(out,bwOut, msg);
		IPartition obtainedPartition = obtainedPartitionAndBool.getPartition();

		AXB axb = epsilonDE.computeReferenceTrajectory(Reduction.ENBB,crn, obtainedPartition,out,bwOut,false,paramsToPerturb,terminator,false);

		//double[][] A = axb.getA();
		//double[] b = axb.getB();
		LinkedHashSet<String> columns = axb.getColumns();

		//int nonSingletonBlocks=0;
		//int extraConstraints=0;
		int incr=1;
		bw.write("\n");
		//bw.write("function closest_bde()\n");
		bw.write("function "+functionName+"()\n");
		bw.write("\n");
		bw.write("    % The obtained partition\n");
		IBlock currentBlock=obtainedPartition.getFirstBlock();
		int bl=1;
		ArrayList<IComposite> icConstraints = new ArrayList<IComposite>();
		while(currentBlock!=null){
			int size = currentBlock.getSpecies().size();
			if(size>1){
				ISpecies rep = currentBlock.getRepresentative();
				for (ISpecies species : currentBlock.getSpecies()) {
					if(!species.equals(rep)){
						//ic of rep = ic of species
						icConstraints.add(new Composite(rep, species));
					}
				}
			}
			bw.write("    % Block "+bl+", Size: "+size+"\n");
			for (ISpecies species : currentBlock.getSpecies()) {
				bw.write("    %   y("+(species.getID()+incr)+")  "+species.getName()+"\n");
			}
			bl++;
			currentBlock=currentBlock.getNext();
		}


		AXB axbWithIC = expandWithICConstraints(crn, obtainedPartition, axb, icConstraints);

		double[][] AwithICConstraints = axbWithIC.getA();
		double[] bwithICConstraints = axbWithIC.getB();
		
		/*
		//The parameters followed by the species
		r=0;
		for(;r<A.length;r++){
			AwithICConstraints[r] = new double[A[r].length+crn.getSpecies().size()];
			for(int c=0;c<A[r].length;c++){
				AwithICConstraints[r][c]=A[r][c];
			}
			bwithICConstraints[r]=b[r];
		}
		for (IComposite icConstraint : icConstraints) {
			AwithICConstraints[r] = new double[A[0].length+crn.getSpecies().size()];
			ISpecies rep = icConstraint.getFirstReagent();
			ISpecies other = icConstraint.getSecondReagent();
			//If the representative is the second...
			if(!rep.equals(obtainedPartition.getBlockOf(rep).getRepresentative())){
				rep = icConstraint.getSecondReagent();
				other = icConstraint.getFirstReagent();
			}

			int cRep = A[0].length+rep.getID();
			int cOther = A[0].length+other.getID();
			AwithICConstraints[r][cRep]=1;
			AwithICConstraints[r][cOther]=-1;
			r++;
		}
		 */

		//bw.write("    %\t "+obtainedPartition.toString()+"\n");
		/*bw.write("\n");
		bw.write("    % The parameters\n");
		bw.write("    %\t "+columns.toString()+"\n");
		bw.write("\n");
		bw.write("    % The linear system to be satisfied for BDE\n");
		bw.write("    A = [\n");
		for(r=0;r<A.length;r++){
			//bw.write("    \t"+A[r].toString()+";\n");
			bw.write("    \t\t[");
			for(int c=0;c<A[r].length;c++){
				bw.write(String.valueOf(A[r][c]));
				if(c<A[r].length-1){
					bw.write(", ");
				}
			}
			bw.write("];\n");
		}
		bw.write("        ];\n");
		bw.write("\n");
		//bw.write("    b = "+b.toString()+";\n");
		bw.write("    b = [");
		for(int i=0;i<b.length;i++){
			bw.write(String.valueOf(b[i]));
			if(i<b.length-1){
				bw.write(", ");
			}
		}
		bw.write("];\n");
		bw.write("\n");

		bw.write("    % The parameters of the reference trajectory\n");
		//bw.write("p0 = [1, 1, 1];\n");
		bw.write("    p0 = [");
		{
		int i=0;
		for(String par : columns){
			double p = crn.getMath().evaluate(par);
			bw.write(String.valueOf(p));
			if(i<columns.size()-1){
				bw.write(", ");
			}
			i++;
		}
		}
		bw.write("];\n");
		bw.write("\n");

		bw.write("\n");
		bw.write("\n");
		bw.write("%\t A extended with constraints on initial concentrations: rep of a block = each other species in the block\n");
		bw.write("\n");
		bw.write("\n");*/

		bw.write("\n");
		/*bw.write("    % The parameters followed by the species\n");
		  bw.write("    %\t "+columns.toString()+"  ,  ");
		  bw.write(crn.getSpecies().toString()+"\n");
		 */
		bw.write("    % The species followed by the parameters\n");
		bw.write("    %\t "+crn.getSpecies().toString()+"  ,  ");
		bw.write(columns.toString()+"\n");
		bw.write("\n");
		bw.write("    % The linear system Ap = b to be satisfied for BDE\n");
		bw.write("    A = [\n");
		for(int r=0;r<AwithICConstraints.length;r++){
			//bw.write("    \t"+A[r].toString()+";\n");
			if(AwithICConstraints[r].length>1){
				bw.write("    \t\t[");
			}
			else{
				bw.write("    \t\t ");
			}
			for(int c=0;c<AwithICConstraints[r].length;c++){
				bw.write(String.valueOf(AwithICConstraints[r][c]));
				if(c<AwithICConstraints[r].length-1){
					bw.write(", ");
				}
			}
			if(AwithICConstraints[r].length>1){
				bw.write("];\n");
			}
			else{
				bw.write(" ;\n");
			}
		}
		bw.write("        ];\n");
		bw.write("\n");

		//bw.write("    b = "+b.toString()+";\n");
		if(bwithICConstraints.length!=1){
			bw.write("    b = [");
		}
		else{
			bw.write("    b = ");
		}
		for(int i=0;i<bwithICConstraints.length;i++){
			bw.write(String.valueOf(bwithICConstraints[i]));
			if(i<bwithICConstraints.length-1){
				bw.write(", ");
			}
		}
		if(bwithICConstraints.length!=1){
			bw.write("];\n");
		}
		else{
			bw.write(" ;\n");
		}
		bw.write("\n");

		bw.write("    % Check whether the BDE constraints can be satisfied\n");
		bw.write("    p = A\\b';\n");
		//bw.write("    if(norm(A*p - b') > eps)\n");
		//bw.write("    if(sum(isinf(p) + isnan(p)) > 0 || norm(A*p - b') > eps)\n");
		bw.write("    if(sum(isinf(p) + isnan(p)) > 0 || norm(A*p - b') > (10^-10))\n");

		bw.write("        fprintf('\\n======== BDE constraints cannot be satisfied ========\\n');\n");
		bw.write("        return;\n");
		bw.write("    end\n");
		bw.write("\n");
		bw.write("\n");

		bw.write("    % The the initial concentrations of the species, followed by parameters of the reference trajectory\n");
		//bw.write("p0ic = [1, 1, 1];\n");

		bw.write("    p0 = [");
		int s=0;
		for(ISpecies species : crn.getSpecies()){
			BigDecimal ic = species.getInitialConcentration();
			if(ic.compareTo(BigDecimal.ZERO)==0){
				ic=defaultIC;
			}
			bw.write(ic.toPlainString());
			if(!(columns.size()==0 && s==crn.getSpecies().size()-1)){
				bw.write(", ");
			}
			s++;
		}
		int i=0;
		for(String par : columns){
			double p = crn.getMath().evaluate(par);
			bw.write(String.valueOf(p));
			if(i<columns.size()-1){
				bw.write(", ");
			}
			i++;
		}
		bw.write("];\n");


		bw.write("\n");
		bw.write("\n");

		bw.write("    % Find a point p that satisfies the BDE constraints and that minimizes\n"); 
		bw.write("    % the Euclidian distance to p0, i.e., solve the quadratic\n");
		bw.write("    % program min_{p : Ap = b} \\lVert p - p0 \\rVert_2\n");
		//bw.write("    fprintf('\\n====== Closest BDE attained at =======================\\n');\n");    
		//bw.write("    quadprog(eye(size(p0,2)), -p0, zeros(size(A,1),size(A,2)), zeros(1,size(b,2)), A, b)\n");
		bw.write("    pOpt = quadprog(eye(size(p0,2)), -p0, zeros(size(A,1),size(A,2)), zeros(1,size(b,2)), A, b);\n");

		bw.write("    fprintf('====== The approximate BDE is made exact by setting the following initial concentrations: =======================\\n')\n");
		int p=1;
		for (ISpecies species : crn.getSpecies()) {
			bw.write("    fprintf('"+species.getName()+"=');\n");
			bw.write("    disp(pOpt("+p+"))\n");
			p++;
		}
		//bw.write("    fprintf('k13=%f\n',pOpt(2));\n");
		bw.write("    fprintf('\\n\\n');\n");

		bw.write("    fprintf('====== and parameter values: =======================\\n')\n");
		//		for (String param : columns) {
		//			bw.write("    fprintf('"+param+"=%f\\n',pOpt("+p+"));\n");
		//			p++;
		//		}
		for (String param : columns) {
			bw.write("    fprintf('"+param+"=');\n");
			bw.write("    disp(pOpt("+p+"))\n");
			p++;
		}
		//bw.write("    fprintf('k13=%f\n',pOpt(2));\n");
		bw.write("    fprintf('\\n\\n');\n");



		bw.write("    dist = pOpt' - p0;\n");
		bw.write("    fprintf('The euclidian norm \\n');\n");
		bw.write("    distEucl = norm(dist)\n");
		bw.write("    fprintf('The inf norm       \\n');\n");
		bw.write("    distInf = max(dist)\n");
		bw.write("    		\n");
		bw.write("\n");
		bw.write("end\n");

	}

	public static AXB expandWithICConstraints(ICRN crn, IPartition obtainedPartition, AXB axb) {
		IBlock currentBlock=obtainedPartition.getFirstBlock();
		ArrayList<IComposite> icConstraints = new ArrayList<IComposite>();
		while(currentBlock!=null){
			int size = currentBlock.getSpecies().size();
			if(size>1){
				ISpecies rep = currentBlock.getRepresentative();
				for (ISpecies species : currentBlock.getSpecies()) {
					if(!species.equals(rep)){
						//ic of rep = ic of species
						icConstraints.add(new Composite(rep, species));
					}
				}
			}
			currentBlock=currentBlock.getNext();
		}
		return expandWithICConstraints(crn, obtainedPartition, axb,icConstraints);
	}
	
	public static AXB expandWithICConstraints(ICRN crn, IPartition obtainedPartition, AXB axb, 
			ArrayList<IComposite> icConstraints) {
		double[][] A=axb.getA();
		double[] b=axb.getB();
		double[][] AwithICConstraints = new double[A.length+icConstraints.size()][];
		double[] bwithICConstraints = new double[b.length+icConstraints.size()];
		LinkedHashSet<String> columnswithICConstraints = new LinkedHashSet<String>(crn.getSpeciesSize()+axb.getColumns().size());
		for(ISpecies species : crn.getSpecies()) {
			columnswithICConstraints.add(species.getName());
		}
		columnswithICConstraints.addAll(axb.getColumns());
		
		//The species followed by the parameters
		int r=0;
		for (IComposite icConstraint : icConstraints) {
			AwithICConstraints[r] = new double[A[0].length+crn.getSpecies().size()];
			ISpecies rep = icConstraint.getFirstReagent();
			ISpecies other = icConstraint.getSecondReagent();
			//If the representative is the second...
			if(!obtainedPartition.speciesIsRepresentativeOfItsBlock(rep)){//if(!rep.equals(obtainedPartition.getBlockOf(rep).getRepresentative())){
				rep = icConstraint.getSecondReagent();
				other = icConstraint.getFirstReagent();
			}

			int cRep = rep.getID();
			int cOther = other.getID();
			AwithICConstraints[r][cRep]=1;
			AwithICConstraints[r][cOther]=-1;
			r++;
		}
		int lastColumnOfICConstraints = (crn.getSpecies().size()-1);
		int lastRowOfICConstraints = r;
		for(;r<AwithICConstraints.length;r++){
			int rOfA = r - lastRowOfICConstraints;
			AwithICConstraints[r] = new double[A[rOfA].length+crn.getSpecies().size()];
			for(int c=0;c<A[rOfA].length;c++){
				int cOfAwithIC=c+lastColumnOfICConstraints+1;
				AwithICConstraints[r][cOfAwithIC]=A[rOfA][c];
			}
			bwithICConstraints[r]=b[rOfA];
		}
		AXB axbWithIC = new AXB(bwithICConstraints, AwithICConstraints, columnswithICConstraints);
		return axbWithIC;
	}

	private static void computeEpsilonFDEAndWriteLinearSystemOfConstraints(ICRN crn, IPartition initial,MessageConsoleStream out,BufferedWriter bwOut,
			IMessageDialogShower msgDialogShower, BufferedWriter bw, String functionName, boolean verbose,Terminator terminator,double epsilon/*,BigDecimal defaultIC*/, LinkedHashSet<String> paramsToPerturb, String prePartitionUserDefined, String prePartitionWRTIC) throws UnsupportedFormatException, IOException {

		{	

			EpsilonDifferentialEquivalences epsilonDE = new EpsilonDifferentialEquivalences();
			long begin = System.currentTimeMillis();

			if(prePartitionWRTIC!=null && prePartitionWRTIC.equalsIgnoreCase("true")){
				initial=ExactFluidBisimilarity.prepartitionWRTIC(crn,initial,false,out,bwOut,terminator);
			}
			if(prePartitionUserDefined!=null && prePartitionUserDefined.equalsIgnoreCase("true")){
				initial= CRNBisimulationsNAry.prepartitionUserDefined(crn, false, out,bwOut,terminator);
			}

			IPartitionAndBoolean obtainedPartitionAndBool = epsilonDE.computeCoarsest(Reduction.ENFB, BigDecimal.valueOf(epsilon), crn, initial, verbose, out,bwOut,terminator,false);
			long end = System.currentTimeMillis();
			double time = (end-begin)/1000.0;
			String timeString = String.format( CRNReducerCommandLine.MSFORMAT, (time) );
			String msg=" eps-FDE time "+timeString+ " (s) ... ";
			CRNReducerCommandLine.print(out,bwOut, msg);
			IPartition obtainedPartition = obtainedPartitionAndBool.getPartition();

			AXB axb = epsilonDE.computeReferenceTrajectory(Reduction.ENFB,crn, obtainedPartition,out,bwOut,false,paramsToPerturb,terminator,false);

			double[][] A = axb.getA();
			double[] b = axb.getB();
			LinkedHashSet<String> columns = axb.getColumns();

			bw.write("\n");
			//bw.write("function closest_fde()\n");
			bw.write("function "+functionName+"()\n");
			bw.write("\n");

			//bw.write("\n");
			bw.write("    % The obtained partition\n");
			IBlock currentBlock=obtainedPartition.getFirstBlock();
			int bl=1;
			int incr=1;
			while(currentBlock!=null){
				int size = currentBlock.getSpecies().size();
				bw.write("    % Block "+bl+", Size: "+size+"\n");
				for (ISpecies species : currentBlock.getSpecies()) {
					bw.write("    %   y("+(species.getID()+incr)+")  "+species.getName()+"\n");
				}
				bl++;
				currentBlock=currentBlock.getNext();
			}
			bw.write("\n");

			bw.write("    % The parameters\n");
			bw.write("    %\t "+columns.toString()+"\n");
			bw.write("\n");

			bw.write("    % The linear system to be satisfied for FDE\n");
			bw.write("    A = [\n");
			for(int r=0;r<A.length;r++){
				//bw.write("    \t"+A[r].toString()+";\n");
				if(A[r].length>1){
					bw.write("    \t\t[");
				}
				else{
					bw.write("    \t\t ");
				}
				for(int c=0;c<A[r].length;c++){
					bw.write(String.valueOf(A[r][c]));
					if(c<A[r].length-1){
						bw.write(", ");
					}
				}
				if(A[r].length>1){
					bw.write("];\n");
				}
				else{
					bw.write(" ;\n");
				}
			}
			bw.write("        ];\n");
			bw.write("\n");

			//bw.write("    b = "+b.toString()+";\n");
			if(b.length!=1){
				bw.write("    b = [");
			}
			else{
				bw.write("    b =  ");
			}
			for(int i=0;i<b.length;i++){
				bw.write(String.valueOf(b[i]));
				if(i<b.length-1){
					bw.write(", ");
				}
			}
			if(b.length!=1){
				bw.write("];\n");
			}
			else{
				bw.write(" ;\n");
			}
			bw.write("\n");

			bw.write("    % Check whether the FDE constraints can be satisfied\n");
			bw.write("    p = A\\b';\n");
			//bw.write("    if(norm(A*p - b') > eps)\n");
			//bw.write("    if(sum(isinf(p) + isnan(p)) > 0 || norm(A*p - b') > eps)\n");
			bw.write("    if(sum(isinf(p) + isnan(p)) > 0 || norm(A*p - b') > (10^-10))\n");
			bw.write("        fprintf('\\n======== FDE constraints cannot be satisfied ========\\n');\n");
			bw.write("        return;\n");
			bw.write("    end    \n");
			bw.write("\n");

			bw.write("    % The parameters of the reference trajectory\n");
			//bw.write("p0 = [1, 1, 1];\n");
			if(columns.size()!=1){
				bw.write("    p0 = [");
			}
			else{
				bw.write("    p0 =  ");
			}

			{
				int i=0;
				for(String par : columns){
					double p = crn.getMath().evaluate(par);
					bw.write(String.valueOf(p));
					if(i<columns.size()-1){
						bw.write(", ");
					}
					i++;
				}
			}
			if(columns.size()!=1){
				bw.write("];\n");
			}
			else{
				bw.write(" ;\n");
			}
			bw.write("\n");

			bw.write("    % Find a point p that satisfies the FDE constraints and that minimizes\n"); 
			bw.write("    % the Euclidian distance to p0, i.e., solve the quadratic\n");
			bw.write("    % program min_{p : Ap = b} \\lVert p - p0 \\rVert_2\n");
			//bw.write("    fprintf('\\n====== Closest FDE attained at =======================\\n');\n");        
			//bw.write("    p = quadprog(eye(size(p0,2)), -p0, zeros(size(A,1),size(A,2)), zeros(1,size(b,2)), A, b)\n");
			bw.write("    pOpt = quadprog(eye(size(p0,2)), -p0, zeros(size(A,1),size(A,2)), zeros(1,size(b,2)), A, b);\n\n");

			bw.write("    fprintf('====== The approximate FDE is made exact by setting the following parameter values: =======================\\n')\n");
			int p=1;
			for (String param : columns) {
				bw.write("    fprintf('"+param+"=');\n");
				bw.write("    disp(pOpt("+p+"))\n");
				p++;
			}
			//bw.write("    fprintf('k13=%f\n',pOpt(2));\n");
			bw.write("    fprintf('\\n\\n');\n");



			bw.write("    dist = pOpt' - p0;\n");
			bw.write("    fprintf('The euclidian norm \\n');\n");
			bw.write("    distEucl = norm(dist)\n");
			bw.write("    fprintf('The inf norm       \\n');\n");
			bw.write("    distInf = max(dist)\n");
			bw.write("    		\n");
			bw.write("\n");
			bw.write("end\n");
		}

	}

	private static void add(HashMap<ISpecies, Integer> hm, Integer value,ISpecies entrysp) {
		Integer prev = hm.get(entrysp);
		if(prev==null){
			prev=value;
		}
		else{
			prev=prev+value;
		}
		hm.put(entrysp, prev);
	}

	private static List<BigDecimal> transformParameterInSpecies(ASTNode rate,HashMap<String, ISpecies> parameterToPerturbToSpecies, List<HashMap<String, Integer>> curriedParamsToAddAsCatalystsForEachMon, HashMap<String, ISpecies> speciesNameToSpecies, MathEval math) throws UnsupportedFormatException {


		//1) Devo creare i monomials di rate
		ArrayList<IMonomial> monomials;
		try {
			monomials= GUICRNImporter.parseGUIPolynomialArbitraryRateExpression(rate,speciesNameToSpecies,math);
		} catch (UnsupportedReactionNetworkEncodingException e) {
			throw new UnsupportedFormatException("Problems in computing the monomials of "+rate.toFormula());
		}

		List<BigDecimal> coefficients = new ArrayList<BigDecimal>();
		
		for(int m=0;m<monomials.size();m++) {
			IMonomial monomial = monomials.get(m);
			BigDecimal coefficient = monomial.getOrComputeCoefficient();
			if(BigDecimal.ZERO.compareTo(coefficient)==0){
				//I skip 0-monomials
				continue;
			}
			
			HashMap<String, Integer> curriedParamsToAddAsCatalysts = new HashMap<String, Integer>();
			curriedParamsToAddAsCatalystsForEachMon.add(curriedParamsToAddAsCatalysts);
			
			HashMap<ISpecies, Integer> appearingSpecies = monomial.getOrComputeSpecies();
			
			//3) Devo rimpiazzare le occorrenze dei parametri da perturbare con 1. 
			//		Per ogni occorrenza di un parametro da perturbare, aggiungo il parametro come reagente e come prodotto. 
			//		Se un parametro appare a denominatore, la reazione non e' supportata.
			for(Entry<ISpecies, Integer> entry : appearingSpecies.entrySet()){
				ISpecies sp = entry.getKey();
				if(parameterToPerturbToSpecies.get(sp.getName())!=null){
					Integer prev = curriedParamsToAddAsCatalysts.get(sp.getName());
					if(prev==null){
						prev=entry.getValue();
					}
					else{
						prev=prev+entry.getValue();
					}
					curriedParamsToAddAsCatalysts.put(sp.getName(), prev);
				}
				else{
					throw new UnsupportedFormatException("No variables shall appear in rates "+rate.toFormula());
				}
			}

			coefficients.add(coefficient);
		}
		return coefficients;
	}
	
	/*
	private static BigDecimal transformParameterInSpecies(ASTNode rate,HashMap<String, ISpecies> parameterToPerturbToSpecies, HashMap<String, Integer> toBeAdded, HashMap<String, ISpecies> speciesNameToSpecies, MathEval math) throws UnsupportedFormatException {


		//1) Devo creare i monomials di rate
		ArrayList<IMonomial> monomials;
		try {
			monomials= GUICRNImporter.parseGUIPolynomialArbitraryRateExpression(rate,speciesNameToSpecies,math);
		} catch (UnsupportedReactionNetworkEncodingException e) {
			throw new UnsupportedFormatException("Problems in computing the monomials of "+rate.toFormula());
		}

		IMonomial monomial=monomials.get(0);
		//2) Se ho piu' di un monomial, la reazione non e' supportata.
		if(monomials.size()!=1){
			int nonZero=0;
			for (IMonomial iMonomial : monomials) {
				BigDecimal coeff = iMonomial.getOrComputeCoefficient();
				if(BigDecimal.ZERO.compareTo(coeff)!=0){
					nonZero++;
					monomial=iMonomial;
				}
			}
			if(nonZero!=1){
				throw new UnsupportedFormatException("Currying is currently supported only for rate expressions made of one monomial "+rate.toFormula());
			}
		}
		BigDecimal coefficient = monomial.getOrComputeCoefficient();
		HashMap<ISpecies, Integer> appearingSpecies = monomial.getOrComputeSpecies();
		//3) Devo rimpiazzare le occorrenze dei parametri da perturbare con 1. 
		//		Per ogni occorrenza di un parametro da perturbare, aggiungo il parametro come reagente e come prodotto. 
		//		Se un parametro appare a denominatore, la reazione non e' supportata.
		for(Entry<ISpecies, Integer> entry : appearingSpecies.entrySet()){
			ISpecies sp = entry.getKey();
			if(parameterToPerturbToSpecies.get(sp.getName())!=null){
				Integer prev = toBeAdded.get(sp.getName());
				if(prev==null){
					prev=entry.getValue();
				}
				else{
					prev=prev+entry.getValue();
				}
				toBeAdded.put(sp.getName(), prev);
			}
			else{
				throw new UnsupportedFormatException("No variables shall appear in rates "+rate.toFormula());
			}
		}

		return coefficient;
	}
	*/

	private void writeModelIndependentCodeForEpsilonBoundScript(BufferedWriter bw, double tEnd, double deltat, LinkedHashSet<String> paramsToPerturb, String linearizationFunctionName, ICRN crnExpanded, List<ISpecies> listOfOriginalSpecies,BigDecimal defaultIC, boolean forward, boolean backward) throws IOException {
		//bw.write("function lambdaQuad = "+linearizationFunctionName+"()\n");
		int incr = 1;		
		bw.write("function "+linearizationFunctionName+"()\n");
		bw.write("    \n");
		bw.write("    global numvars\n"); 
		//bw.write("    global t0\n");
		bw.write("    \n");

		bw.write("    % Correspondence with original names:\n");
		int i=0;
		bw.write("    %\t The original variables\n");
		for (ISpecies species : crnExpanded.getSpecies()) {
			if(i==listOfOriginalSpecies.size()){
				bw.write("\n    %\t The perturbed parameters\n");
			}
			bw.write("    %\t y(" +(species.getID()+incr)+") = " + species.getName()+"\n");
			i++;
		}
		bw.write("\n");
		bw.write("\n");
		bw.write("    %%% Input %%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
		bw.write("    % t1: Time horizon\n");
		bw.write("    t1 = "+tEnd+";\n");
		bw.write("    % y0: the initial condition of the reference trajectory.\n");

		bw.write("    % That is, the initial concentrantion of the "+listOfOriginalSpecies.size()+" variables:\n");
		bw.write("    %\t "+listOfOriginalSpecies.toString()+"\n");
		if(paramsToPerturb.size()>0){
			if(paramsToPerturb.size()>1){
				bw.write("    % and the actual value of the "+paramsToPerturb.size()+" perturbed parameters:\n");
			}
			else{
				bw.write("    % and the actual value of the perturbed parameter:\n");
			}

			bw.write("    %\t "+paramsToPerturb.toString()+"\n");
		}

		//bw.write("    y0 = [1,0,3];\n");
		bw.write("    y0 = [");
		int s=0;
		for (ISpecies species : crnExpanded.getSpecies()) {
			BigDecimal ic = species.getInitialConcentration();
			if(ic.compareTo(BigDecimal.ZERO)==0){
				ic=defaultIC;
			}
			if(s==crnExpanded.getSpecies().size()-1){
				bw.write(ic.toPlainString());
			}
			else{
				bw.write(ic.toPlainString()+",");
			}
			s++;
		}
		/*
 bw.write("    % y0: we first write the initial concentrantion of each species and then the value of the parameters.\n");
 for (ISpecies species : crn.getSpecies()) {
	if(s==crn.getSpecies().size()-1 && crn.getParameters().size()==0){
		bw.write(species.getInitialConcentration().toPlainString());
	}
	else{
		bw.write(species.getInitialConcentration().toPlainString()+",");
	}
	s++;
}
int p=0;
for(String param : crn.getParameters()){
	String[] nameAndValue = param.split(" ");
	if(p==crn.getParameters().size()-1){
		bw.write(nameAndValue[1]);
	}
	else{
		bw.write(nameAndValue[1]+",");
	}
	p++;
}*/
		bw.write("];\n");

		bw.write("    \n");
		bw.write("    numvars = size(y0,2);\n");
		bw.write("    \n");

		bw.write("    % uindx: the indices of all variables that are subject to uncertainty,\n");
		bw.write("    % e.g., [1,3] means that y(1) and y(3) are subject to uncertainty,\n");
		bw.write("    % whereas y(2) is not.\n");
		//bw.write("    uindx = [1,2,3];       \n");
		bw.write("    uindx = [");
		int tot = crnExpanded.getSpecies().size()/*+crn.getParameters().size()*/;

		int startFrom=listOfOriginalSpecies.size()+1;


		//for(i=1;i<=tot;i++){
		for(i=startFrom;i<=tot;i++){
			if(i==tot){
				bw.write(String.valueOf(i));
			}
			else{
				bw.write(i+",");
			}
		}
		bw.write("];       \n");
		//bw.write("    \n");


		//bw.write("    %%% Output %%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n");
		//bw.write("    % lambda: The error amplifier around the reference trajectory\n");
		bw.write("    \n");
		bw.write("    \n");
		bw.write("    % Optimization step (smaller values lead to more reliable results but\n");
		bw.write("    % come at the price of longer running time).\n");
		bw.write("    deltat = "+deltat+";\n");
		bw.write("    \n");
		bw.write("    \n");

		if(computeDE){
			bw.write("    % Compute the closest approximate differential equivalences\n");
			if(forward){
				bw.write("    closest_fde();\n");
			}
			if(backward){
				bw.write("    closest_bde();\n");
			}
		}
		else{
			bw.write("    %closest_fde();\n");
			bw.write("    %closest_bde();\n");
		}
		bw.write("    \n");
		bw.write("    \n");



		
		InputStream is = getClass().getResourceAsStream("epsilonBoundScript.txt");
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		String line=br.readLine();
		while(line!=null){
			bw.write(line);
			bw.write("\n");
			line=br.readLine();
		}
		br.close();




		//completeOldLessEfficientMATLABScript(bw);

	}

	//	private static void completeOldLessEfficientMATLABScript(BufferedWriter bw) throws IOException {
	//		bw.write("    % Compute the error amplifier\n");
	//		bw.write("    lambda = 1;\n");
	//		bw.write("    X = zeros(size(0:deltat:t1,2),2*numvars,size(uindx,2));\n");    
	//		bw.write("    for t0 = 0:deltat:t1        \n");
	//		bw.write("        % For each ODE variable y(i) that is uncertain, compute the\n");
	//		bw.write("        % i-th columns of the transition matrices \\Lambda(t0,t), \n");
	//		bw.write("        % where t0 <= t <= t1\n");
	//		bw.write("        for i = 1 : size(uindx,2)\n");
	//		bw.write("            z0 = zeros(1,numvars);\n");
	//		bw.write("            z0(uindx(i)) = 1;\n");
	//		bw.write("            \n");
	//		bw.write("            [T,tmpX]=ode45(@drift, 0:deltat:t1, [y0,z0]);\n");                
	//		bw.write("            X(:,:,i) = tmpX;\n");
	//		bw.write("        end\n");
	//		bw.write("        \n");
	//
	//		bw.write("        % Compute the maximum norm of all transition matrices \\Lambda(0,t),\n");
	//		bw.write("        % where t <= t1\n");
	//		bw.write("        for t = 1 : size(T,1) \n");          
	//		bw.write("            aux = zeros(numvars, size(uindx,2));\n");
	//		bw.write("            for i = 1 : size(uindx,2)\n");
	//		bw.write("                aux2 = X(t,numvars+1:2*numvars,i);\n");
	//		bw.write("                aux(:,i) = aux2';\n");
	//		bw.write("            end\n");
	//		bw.write("            aux = norm(aux,'inf');\n");
	//		bw.write("            %aux = norm(aux,2);\n");
	//		bw.write("            if(lambda < aux)\n");
	//		bw.write("                lambda = aux;\n");
	//		bw.write("            end        \n");
	//		bw.write("        end\n");
	//		bw.write("    end\n");
	//		//
	//		/*
	//bw.write("    for t0 = 0:deltat:t1\n");//bw.write("    for t0 = [0:deltat:t1]\n");
	//bw.write("        % For each ODE variable y(i) that is uncertain, compute the\n");
	//bw.write("        % i-th columns of the transition matrices \\Lambda(t0,t), \n");
	//bw.write("        % where t0 <= t <= t1\n");
	//bw.write("        X = [];\n");
	//bw.write("        %X = zeros(numvars,1);\n");
	//bw.write("        for i = 1 : size(uindx,2)\n");
	//bw.write("            z0 = zeros(1,numvars);\n");
	//bw.write("            z0(uindx(i)) = 1;\n");
	//bw.write("            \n");
	//bw.write("            [T,tmpX]=ode45(@drift, 0:deltat:t1, [y0,z0]);\n");//bw.write("            [T,tmpX]=ode45(@drift, [0:deltat:t1], [y0,z0]);\n");                
	//bw.write("            X(:,:,i) = tmpX;\n");
	//bw.write("        end\n");
	//bw.write("        \n");
	//bw.write("        % Compute the maximum norm of all transition matrices \\Lambda(0,t),\n");
	//bw.write("        % where t <= t1\n");
	//bw.write("        for t = 1 : size(T,1)\n");
	//bw.write("            aux = [];\n");
	//bw.write("            for i = 1 : size(uindx,2)\n");
	//bw.write("                aux2 = X(t,numvars+1:2*numvars,i);\n");
	//bw.write("                aux = [aux, aux2'];\n");
	//bw.write("            end\n");
	//bw.write("            %aux = norm(aux,'inf');\n");
	//bw.write("            aux = norm(aux,2);\n");
	//bw.write("            if(lambda < aux)\n");
	//bw.write("                lambda = aux;\n");
	//bw.write("            end        \n");
	//bw.write("        end\n");
	//bw.write("    end\n");
	//		 */
	//		bw.write("\n");
	//
	//		bw.write("    fprintf('\\n====== Lambda Value =======================\\n');    \n");
	//		bw.write("    lambda\n");
	//
	//		bw.write("end\n");
	//		bw.write("\n");
	//		bw.write("\n");
	//
	//		bw.write("function dx = drift(t,x)\n"); 
	//		bw.write("    global numvars\n");
	//		bw.write("    global t0\n");
	//		bw.write("    \n");
	//
	//		bw.write("    dy = ode(x(1:numvars));   \n");
	//		bw.write("    \n");
	//
	//		bw.write("    if(t >= t0)\n");
	//		bw.write("        jac = jacobian(x(1:numvars));\n");    
	//		bw.write("        dz = jac * x(numvars+1:2*numvars);\n");
	//		bw.write("    else\n");
	//		bw.write("        dz = zeros(1,numvars)';\n");
	//		bw.write("    end\n");
	//		bw.write("    \n");
	//
	//		bw.write("    dx = [dy;dz];\n");        
	//		bw.write("end\n");
	//
	//		//BUGGY
	//		/*
	//bw.write("    y = x(1:numvars);\n");
	//bw.write("    dy = ode(y);   \n");
	//bw.write("    \n");
	//
	//bw.write("    if(t >= t0)\n");
	//bw.write("        jac = jacobian(x(1:numvars));\n");    
	//bw.write("        dz = jac * y;\n");
	//bw.write("    else\n");
	//bw.write("        dz = zeros(1,numvars)';\n");
	//bw.write("    end\n");
	//bw.write("    \n");
	//
	//bw.write("    dx = [dy;dz];\n");        
	//bw.write("end\n");
	//		 */
	//	}

	public static void writeInitJaco(ICRN crn, MessageConsoleStream out, IMessageDialogShower msgDialogShower,
			BufferedWriter bw, String prefixSpace, HashMap<String, ISpecies> speciesNameToSpecies) throws IOException, ParseException {

		bw.write("function initJaco()\n");

		bw.write(prefixSpace+"global jaco;\n\n");

		String variablePrefix="y";
		String variableSuffix="v";
		int idIncrement=1;

		StringBuilder sb = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		for(int i=0;i<crn.getSpecies().size();i++){
			String species=variablePrefix+(i+idIncrement)+variableSuffix;

			sb.append(" "+species);

			sb2.append(species);
			if(i<crn.getSpecies().size()-1){
				sb2.append(",");
			}
			sb2.append(" ");
		}
		for(String param : crn.getParameters()){
			String[] nameAndVal = param.split("\\ ");
			sb.append(" "+nameAndVal[0]+variableSuffix);
		}

		String speciesAndParams=sb.toString();

		//syms y1v y2v y3v y4v y5v y6v kBind1v kBind2v kUnbind1v kUnbind2v
		bw.write(prefixSpace+"syms");
		bw.write(speciesAndParams);
		bw.write("\n\n");

		/*
		 symjaco = jacobian(   [ ((0 - kBind2v)+(0 - kBind1v))*y1v*y2v + kUnbind1v*y3v + kUnbind2v*y4v, ...
                            ((0 - kBind2v)+(0 - kBind1v))*y1v*y2v + ((0 - kBind2v))*y2v*y3v + ((0 - kBind1v))*y2v*y4v + kUnbind1v*y3v + kUnbind2v*y4v + (kUnbind1v+kUnbind2v)*y5v, ...
                            kBind1v*y1v*y2v + ((0 - kBind2v))*y2v*y3v + ((0 - kUnbind1v))*y3v + kUnbind2v*y5v, ...
                            kBind2v*y1v*y2v + ((0 - kBind1v))*y2v*y4v + ((0 - kUnbind2v))*y4v + kUnbind1v*y5v, ...
                            kBind2v*y2v*y3v + kBind1v*y2v*y4v + ((0 - kUnbind1v)+(0 - kUnbind2v))*y5v, ...
                            0
                          ] , ...
                          [ y1v, y2v, y3v, y4v, y5v, y6v ] );
		 */
		bw.write(prefixSpace+"symjaco = jacobian( [");

		//write the drift...
		HashMap<ISpecies, StringBuilder> speciesToDriftSB;

		speciesToDriftSB = MatlabODEsImporter.computeODEFunction(crn, variablePrefix,idIncrement,variableSuffix, speciesNameToSpecies);

		int s=1;
		for(ISpecies species : crn.getSpecies()){
			StringBuilder driftsb=speciesToDriftSB.get(species);
			String drift="0";
			if(driftsb!=null){
				drift=driftsb.toString();
			}
			if(s>1){
				bw.write("                          ");
			}
			else{
				bw.write(" ");
			}
			bw.write(drift);
			if(s<crn.getSpecies().size()){
				bw.write(", ...\n");
			}
			s++;
		}
		bw.write("\n");

		bw.write(prefixSpace+"                    ] , ...\n");
		bw.write(prefixSpace+"                    [ ");
		bw.write(sb2.toString());
		bw.write("]);\n\n");


		//jaco = matlabFunction(symjaco,'Vars',[y1v y2v y3v y4v y5v y6v kBind1v kBind2v kUnbind1v kUnbind2v]);
		bw.write(prefixSpace+"jaco = matlabFunction(symjaco,'Vars',[");
		bw.write(speciesAndParams);
		bw.write("]);\n\n");

		bw.write("end\n");

	}

	public static void writeSymbolicJacobianFunction(ICRN crn, MessageConsoleStream out, IMessageDialogShower msgDialogShower,
			BufferedWriter bw, String functionName,boolean ignoreZeros,boolean printCaption,boolean writeParameters, boolean writeFunctionNameAndEnd, String prefixSpace) throws IOException {


		if(printCaption){
			bw.write("%Jacobian function of model "+crn.getName()+" (" +crn.getSpecies().size()+" species and "+crn.getReactions().size()+" reactions)\n");
		}

		if(writeFunctionNameAndEnd){
			bw.write("function J = "+functionName+"(y)\n");
		}

		if(writeParameters){
			writeParams(crn, bw, prefixSpace);
		}

		//J = feval(jaco,y(1),y(2),y(3),y(4),y(5),y(6),kBind1,kBind2,kUnbind1,kUnbind2);
		bw.write(prefixSpace+"J = feval(jaco");
		for(int i=0;i<crn.getSpecies().size();i++){
			bw.write(",y("+(i+1)+")");
		}
		for(String param : crn.getParameters()){
			String[] nameAndVal = param.split("\\ ");
			bw.write(","+nameAndVal[0]);
		}
		bw.write(");\n");

		if(writeFunctionNameAndEnd){
			bw.write("end\n");
		}
	}

	//	public static void writeSymjaJacobianFunction(ICRN crn, MessageConsoleStream out, IMessageDialogShower msgDialogShower,
	//			BufferedWriter bw, String functionName,boolean ignoreZeros,boolean printCaption,boolean writeParameters, boolean writeFunctionNameAndEnd, String prefixSpace/*, HashMap<String, ISpecies> speciesNameToSpecies*/) throws IOException {
	//		
	//		LinkedHashMap<String, ISpecies> speciesNameLowerCaseToSpecies = new LinkedHashMap<String, ISpecies>(crn.getSpecies().size());
	//		for(ISpecies species : crn.getSpecies()){
	//			speciesNameLowerCaseToSpecies.put(symjaName(species.getName()),species);
	//		}
	//
	//		if(printCaption){
	//			bw.write("%Jacobian function of model "+crn.getName()+" (" +crn.getSpecies().size()+" species and "+crn.getReactions().size()+" reactions)\n");
	//		}
	//
	//		if(writeFunctionNameAndEnd){
	//			bw.write("function J = "+functionName+"(y)\n");
	//		}
	//
	//		if(writeParameters){
	//			writeParams(crn, bw, prefixSpace);
	//		}
	//
	//		bw.write(prefixSpace+"J = zeros("+crn.getSpecies().size()+","+crn.getSpecies().size()+");\n\n");
	//
	//		LinkedHashMap<ISpecies, StringBuilder> speciesToDriftSB = GUICRNImporter.computeDrifts(crn, false, true);
	//		
	//		boolean print=false;
	//		IExpr[][] Jacobian=computeSymbolicJacobian(crn,speciesToDriftSB,print);
	//		
	//		ISymbol varName = F.$s("y");
	//		for(int i=0;i<Jacobian.length;i++){
	//			for(int j=0;j<Jacobian[i].length;j++){
	//				//System.out.println("i="+i+", j="+j);
	//				Jacobian[i][j]=replaceSpeciesWithFunction(Jacobian[i][j],varName,speciesNameLowerCaseToSpecies);
	//			}
	//		}
	//		
	//		
	//		int idIncrement = 1;
	//		
	//
	//		/*J(1,1) = -y(3) + y(2);
	//		J(1,2) = y(1);
	//		J(1,3) = -y(1);
	//
	//		J(2,1) = y(3) - y(2) + y(3);
	//		J(2,2) = - y(1) - y(3);
	//		J(2,3) = y(1) + y(1) - y(2);
	//
	//		J(3,1) = -y(3);
	//		J(3,2) = y(3);
	//		J(3,3) = -y(1) + y(2);*/
	//
	//		for(int i=0;i<crn.getSpecies().size();i++){
	//			for(int j=0;j<crn.getSpecies().size();j++){
	//				int iincr = i+idIncrement;
	//				int jincr = j+idIncrement;
	//				String JacEntry=Jacobian[i][j].toString();
	//				JacEntry=JacEntry.replace("\n", "");
	//				String line=prefixSpace+"J("+iincr+","+jincr+") = "+JacEntry+";\n";
	//				bw.write(line);
	//			}
	//			bw.write("\n");
	//		}
	//
	//		if(writeFunctionNameAndEnd){
	//			bw.write("end\n");
	//		}
	//		
	//	}
	//	
	//
	public static String symjaName(String name) {
		if(name.length()==1){
			return name;
		}
		else{
			return name.toLowerCase();
		}
	}
	//
	//	private static IExpr[][] computeSymbolicJacobian(ICRN crn, HashMap<ISpecies, StringBuilder> speciesToDriftSB,boolean print) {
	//		int N=crn.getSpecies().size();
	//		ExprEvaluator util = new ExprEvaluator();
	//		
	//		/*for (String param : crn.getParameters()) {
	//			String[] p = param.split("\\ ");
	//			util.defineVariable(p[0], Double.valueOf(p[1]));
	//		}*/
	//		
	//		ISymbol[] variables=new ISymbol[N];
	//		IExpr[] drifts=new IExpr[N];
	//		int s=0;
	//		for(ISpecies species : crn.getSpecies()){
	//		//for(Entry<ISpecies, StringBuilder> pair : speciesToDriftSB.entrySet()){
	//			//ISpecies species=pair.getKey();
	//			//String drift = pair.getValue().toString();
	//			StringBuilder driftSB = speciesToDriftSB.get(species);
	//			String drift="0";
	//			if(driftSB!=null){
	//				drift = driftSB.toString();
	//				
	//			}
	//			variables[s]=F.$s(species.getName());
	//			drifts[s]= util.evaluate(drift);
	//			
	//			s++;
	//		}
	//		
	//		
	//		
	//		IExpr[][] Jacobian = new IExpr[N][];
	//		for(int i=0;i<N;i++){
	//			Jacobian[i]=new IExpr[N];
	//			if(print){
	//				System.out.println("\n");
	//			}
	//			for(int j=0;j<N;j++){
	//				IAST function = D(drifts[i],variables[j]);
	//				Jacobian[i][j] = util.evaluate(function);
	//				if(print){
	//					System.out.print(Jacobian[i][j]);
	//					System.out.print("  ");
	//				}
	//			}
	//		}
	//		
	//		
	//		return Jacobian;
	//		
	//		
	//		/*
	//		System.out.println("\n#######");
	//			System.out.println("\nHESSIAN");
	//			System.out.println("#######");
	//			//dfidfi_In_dxidxj
	//			IExpr[][] Hessian=new IExpr[N][];
	//			for(int i=0;i<N;i++){
	//				Hessian[i]=new IExpr[N];
	//				//System.out.println("\nRow "+i+"\n\t");
	//				System.out.println("\n");
	//				for(int j=0;j<N;j++){
	//					IAST function = D(Jacobian[i][j],variables[i]);
	//					Hessian[i][j] = util.evaluate(function);
	//					System.out.print(Hessian[i][j]);
	//					System.out.print("  ");
	//				}
	//			}
	//		 */
	//	}

	/*
	public static void writeLNA(ICRN crn, MessageConsoleStream out, IMessageDialogShower msgDialogShower,
			BufferedWriter bw, String functionName,boolean ignoreZeros,boolean printCaption,boolean writeParameters, boolean writeFunctionNameAndEnd, String prefixSpace,ISpecies[] speciesIDToSpecies) throws IOException, UnsupportedFormatException {
		///
		/// BEWARE: it works only with mass-action CRNs!
		///
		if((!crn.isMassAction()) || crn.isSymbolic()){
			throw new UnsupportedFormatException("The LNA can be computed only for mass-action CRNs.");
		}

		if(printCaption){
			bw.write("%LNA of model "+crn.getName()+" (" +crn.getSpecies().size()+" species and "+crn.getReactions().size()+" reactions)\n");
		}

		if(writeFunctionNameAndEnd){
			bw.write("function J = "+functionName+"(y)\n");
		}

		if(writeParameters){
			writeParams(crn, bw, prefixSpace);
		}

		bw.write(prefixSpace+"J = zeros("+crn.getSpecies().size()+","+crn.getSpecies().size()+");\n\n");

		//compute and write the Jacobian
		//Build the jump vector (and its transpose) of each reaction. This is very space demanding: numberOfReactions*numberOfSpecies. Check if I can instead store an array of composites (the jvComposite).
		int[][] jumps = DataOutputHandlerAbstract.computeJumps(crn);
		int idIncrement = 0;
		String[][] je = DataOutputHandlerAbstract.computeJacobianFunction(crn, jumps, msgDialogShower, out,ignoreZeros,speciesIDToSpecies);

//		J(1,1) = -y(3) + y(2);
//		J(1,2) = y(1);
//		J(1,3) = -y(1);
//
//		J(2,1) = y(3) - y(2) + y(3);
//		J(2,2) = - y(1) - y(3);
//		J(2,3) = y(1) + y(1) - y(2);
//
//		J(3,1) = -y(3);
//		J(3,2) = y(3);
//		J(3,3) = -y(1) + y(2);

		//String[] nv = CRNVectorField.computeNVString(crn, terminator);
		String[] nv = CRNVectorField.computeNVString(crn,   null);

		//now I compute the G
		String[][] G=new String[crn.getSpecies().size()][crn.getSpecies().size()];
		for(int m=0;m<crn.getReactions().size();m++){
			//if nv[m]=0 I can skip the iteration!
			if(nv[m]!=null && !nv[m].isEmpty()){
				double[][] jumpsmTranspXjumpsm = CRNVectorFieldApacheCovariances.multiplyTranspWithVector(jumps[m]);
				G = CRNVectorFieldApacheCovariances.sum(G, CRNVectorFieldApacheCovariances.multiplyScalar(jumpsmTranspXjumpsm,nv[m]));
			}
		}


		String[][] C = new String[crn.getSpecies().size()][crn.getSpecies().size()];
		for(int i=0;i<crn.getSpecies().size();i++) {
			for(int j=0;j<crn.getSpecies().size();j++) {
				C[i][j] = "C_"+i+"_"+j;
			}
		}

		String[][] JeXC= CRNVectorFieldApacheCovariances.multiply(je, C,true,false);
		String[][] CXJeTransp= CRNVectorFieldApacheCovariances.multiply(C,CRNVectorFieldApacheCovariances.transpose(je),false,true);
		String[][] tot= CRNVectorFieldApacheCovariances.sum(CRNVectorFieldApacheCovariances.sum(JeXC,CXJeTransp),G);


		for(int i=0;i<crn.getSpecies().size();i++){
			for(int j=0;j<crn.getSpecies().size();j++){
				int iincr = i+idIncrement;
				int jincr = j+idIncrement;
				bw.write(prefixSpace+"d(C_"+iincr+"_"+jincr+") = ");
				if(je[i][j]==null){
					bw.write("0\n");
				}
				else{
					bw.write(tot[i][j]+"\n");
				}

			}
			bw.write("\n");
		}

		if(writeFunctionNameAndEnd){
			bw.write("end\n");
		}
	}
	 */

	public static void writeJi(ICRN crn, MessageConsoleStream out, IMessageDialogShower msgDialogShower,
			BufferedWriter bw, String functionName,String jacobianFunctionName,boolean printCaption, String prefixSpace/*,ISpecies[] speciesIDToSpecies*/,double eps, ArrayList<LinkedHashMap<String, Double>> M0,double maxPerturbation) throws IOException, UnsupportedFormatException {
		/**
		 * BEWARE: it works only with elementary mass-action CRNs!
		 */
		if((!crn.isMassAction()) || (!crn.isElementary()) || crn.isSymbolic()){
			throw new UnsupportedFormatException("The Ji decomposition of the jacobian function can at the moment be decomposed in the Ji form only for elementary mass-action CRNs.");
		}

		if(printCaption){
			bw.write("%Decomposing the Jacobian function of model "+crn.getName()+" (" +crn.getSpecies().size()+" species and "+crn.getReactions().size()+" reactions)\n% with one Ji per different monomial in the drift\n");
		}
		
		int N=crn.getSpecies().size();
		bw.write("function [IC,J,N,epsilon,M0,driftName,modelName,maxPerturbation] = "+functionName+"()\n");
		bw.write(prefixSpace+"driftName = '"+functionName+"Drift';\n");
		bw.write(prefixSpace+"modelName = '"+functionName+"';\n");
		bw.write(prefixSpace+"maxPerturbation = "+maxPerturbation+";\n");
		bw.write(prefixSpace+"epsilon = "+eps+";\n\n");
		
		bw.write(prefixSpace+"N = "+N+";\n\n");
		
		bw.write(prefixSpace+"%We declare the initial conditions\n");
		bw.write(prefixSpace+"IC=zeros(N,1);\n");
		int s=0;
		for(ISpecies species : crn.getSpecies()){
			s++;
			if(species.getInitialConcentration().compareTo(BigDecimal.ZERO)!=0) {
				bw.write(prefixSpace+"IC("+s+")="+species.getInitialConcentration().toPlainString()+";\n");
			}
		}
		
		
		//M0
		HashMap<String,ISpecies> speciesNameToSpecies = new HashMap<>();		
		for(ISpecies species : crn.getSpecies()){
			speciesNameToSpecies.put(species.getName(), species);
		}
		bw.write(prefixSpace+"%We now declare M0\n");
		bw.write(prefixSpace+"M0 = zeros("+M0.size()+",N);\n");
		int idIncrement = 1;
		int r=1;
		for(HashMap<String, Double> row : M0) {
			bw.write(prefixSpace+"%Row "+r+": "+row+"\n");
			for(Entry<String, Double> entry : row.entrySet()) {
				ISpecies sp = speciesNameToSpecies.get(entry.getKey());
				if(sp==null) {
					int id = Integer.valueOf(entry.getKey());
					sp=crn.getSpecies().get(id);
				}
				double coeff = entry.getValue();
				int column=sp.getID()+idIncrement;
				//M0(r,N-1)=1;
				bw.write(prefixSpace+"M0("+r+","+column+")="+coeff+";\n");
				//bw.write(" %"+sp.getName()+"="+coeff+"\n");
			}
			//bw.write(prefixSpace+"\n");
			r++;
		}
		
		bw.write(prefixSpace+"\n\n");
		bw.write(prefixSpace+"J = cell(N+1,1);\n");
		bw.write(prefixSpace+"y=zeros(N,1);\n\n");
		
		
		bw.write(prefixSpace+"J0 = "+jacobianFunctionName+"(y);\n");
		bw.write(prefixSpace+"J{1}=J0;\n");
		bw.write(prefixSpace+"%fprintf('J0\\n')\n");
		bw.write(prefixSpace+"%disp(J0)\n\n");
	    
		bw.write(prefixSpace+"for i = 1:1:N\n");
		bw.write(prefixSpace+"	y(i)=1;\n");
		bw.write(prefixSpace+"	if i>1\n");
		bw.write(prefixSpace+"		y(i-1)=0;\n");
		bw.write(prefixSpace+"	end\n");
		bw.write(prefixSpace+"	Ji = "+jacobianFunctionName+"(y) - J0;\n");
		bw.write(prefixSpace+"	J{i+1}=Ji;\n");
		bw.write(prefixSpace+"	%fprintf('J%d\\n',i)\n");
		bw.write(prefixSpace+"	%disp(Ji)\n");
		bw.write(prefixSpace+"end\n\n");
		
		bw.write(prefixSpace+"%fprintf('\\n\\nAt the end\\n')\n");
		bw.write(prefixSpace+"%for i=0:1:N\n");
		bw.write(prefixSpace+"%	fprintf('J%d\\n',i);\n");
		bw.write(prefixSpace+"%	disp(J{i+1})\n");
		bw.write(prefixSpace+"%end\n\n");
		
		bw.write(prefixSpace+"end\n\n");
		
		
		
	}
	
	public static void writeJacobianFunction(ICRN crn, MessageConsoleStream out, IMessageDialogShower msgDialogShower,
			BufferedWriter bw, String functionName,boolean ignoreZeros,boolean printCaption,boolean writeParameters, boolean writeFunctionNameAndEnd, String prefixSpace/*,ISpecies[] speciesIDToSpecies*/) throws IOException, UnsupportedFormatException {
		/**
		 * BEWARE: it works only with mass-action CRNs!
		 */
		if((!crn.isMassAction()) || crn.isSymbolic()){
			throw new UnsupportedFormatException("The Jacobian function can be computed only for mass-action CRNs.");
		}

		if(printCaption){
			bw.write("%Jacobian function of model "+crn.getName()+" (" +crn.getSpecies().size()+" species and "+crn.getReactions().size()+" reactions)\n");
		}

		if(writeFunctionNameAndEnd){
			bw.write("function J = "+functionName+"(y)\n");
		}

		if(writeParameters){
			writeParams(crn, bw, prefixSpace);
		}

		//bw.write(prefixSpace+"J = zeros("+crn.getSpecies().size()+","+crn.getSpecies().size()+");\n\n");
		bw.write(prefixSpace+"J = sparse("+crn.getSpecies().size()+","+crn.getSpecies().size()+");\n\n");

		//compute and write the Jacobian
		//Build the jump vector (and its transpose) of each reaction. This is very space demanding: numberOfReactions*numberOfSpecies. Check if I can instead store an array of composites (the jvComposite).
		int[][] jumps = DataOutputHandlerAbstract.computeJumps(crn);
		int idIncrement = 1;
		boolean resolveParams=!writeParameters;
		String[][] je = DataOutputHandlerAbstract.computeJacobianFunction(crn, jumps, msgDialogShower, out,idIncrement,ignoreZeros/*,speciesIDToSpecies*/,resolveParams);

		/*J(1,1) = -y(3) + y(2);
		J(1,2) = y(1);
		J(1,3) = -y(1);

		J(2,1) = y(3) - y(2) + y(3);
		J(2,2) = - y(1) - y(3);
		J(2,3) = y(1) + y(1) - y(2);

		J(3,1) = -y(3);
		J(3,2) = y(3);
		J(3,3) = -y(1) + y(2);*/

		for(int i=0;i<crn.getSpecies().size();i++){
			for(int j=0;j<crn.getSpecies().size();j++){
				int iincr = i+idIncrement;
				int jincr = j+idIncrement;
				if(je[i][j]==null /*|| je[i][j].equals("0") || je[i][j].equals("0.0")*/){
					//bw.write(prefixSpace+"J("+iincr+","+jincr+") = 0;\n");
				}
				else{
					bw.write(prefixSpace+"J("+iincr+","+jincr+") = "+je[i][j]+";\n");
				}

			}
			bw.write("\n");
		}

		if(writeFunctionNameAndEnd){
			bw.write("end\n");
		}
	}

	protected static void writeParams(ICRN crn, BufferedWriter bw, String prefixSpace) throws IOException {
		for (String param : crn.getParameters()) {
			int space = param.indexOf(' ');
			String paramName = param.substring(0, space).trim();
			String paramExpr = param.substring(space,param.length()).trim();
			//p=0.5;
			if(!paramName.equalsIgnoreCase("eps")){
				bw.write(prefixSpace+paramName +" = "+paramExpr+";\n");
			}
		}
	}

	public static void printToCERENAFIle(ICRN crn, String name, Collection<String> preambleCommentLines, boolean verbose, String icComment,MessageConsoleStream out,BufferedWriter bwOut, String tEnd,String printJacobian, IMessageDialogShower msgDialogShower) throws UnsupportedFormatException{
		//TODO TABEA: fix MATLAB EXPORTER
		String fileName = name;

		fileName=overwriteExtensionIfEnabled(fileName,".m");
		if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Writing model in file "+fileName);
		}

		createParentDirectories(fileName);
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printODEsToMatlabFIle, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}

		try {

			if(preambleCommentLines!=null && preambleCommentLines.size()>0){
				for (String comment : preambleCommentLines) {
					bw.write("%"+comment+"\n");
				}
			}
			bw.write("\n\n");

			bw.write("% Automatically generated from "+crn.getName()+".\n");
			//bw.write("% Original number of species: "+crn.getSpecies().size()+".\n");
			//bw.write("% Original number of reactions: "+crn.getReactions().size()+".\n");			

			bw.write("\n\n");

			/*
			String odeFuncName = "";
			if(crn.getName()!=null&&crn.getName()!=""){
				odeFuncName = GUICRNImporter.getModelName(name);
			}
			else{
				odeFuncName="model";
			}
			 */

			ISpecies[] speciesIdToSpecies = new ISpecies[crn.getSpecies().size()];
			int s=0;
			HashMap<String,ISpecies> speciesNameToSpecies = new HashMap<String, ISpecies>(crn.getSpecies().size());
			//BASIC SETUP DONE

			bw.write("[T,Y]=ode45(@ode,[0 "+tEnd+"],[ ");
			for(ISpecies species : crn.getSpecies()){
				bw.write(" "+species.getInitialConcentration());
				speciesNameToSpecies.put(species.getName(), species);
				speciesIdToSpecies[s]=species;
				s++;
			}
			bw.write("]);\n ");

			//ISpecies firstSpecies = crn.getSpecies().get(0);
			//StringBuffer legend = new StringBuffer("");

			//speciesNameToSpecies

			//OUTPUT
			if(crn.getViewNames()!=null && crn.getViewNames().length>0){
				boolean first=true;

				for(int v=0;v<crn.getViewNames().length;v++){
					String vExpr = crn.getViewExpressions()[v];
					if(first){
						bw.write("output = [");
						first=false;
					}
					else{
						bw.write(";");
					}
					bw.write(vExpr);
				}
				bw.write("];\n");
			}
			else{
				bw.write("% output = [];\n");
			}

		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printODEsToMatlabFIle, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		finally{
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"Writing model in file "+fileName+" completed");
			}
			try {
				bw.flush();
				bw.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in printODEsToMatlabFIle, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}	
	}

	

}

