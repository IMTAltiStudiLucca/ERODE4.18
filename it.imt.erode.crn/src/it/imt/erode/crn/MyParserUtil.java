package it.imt.erode.crn;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import it.imt.erode.crn.chemicalReactionNetwork.*;

/**
 * 
 * @author Andrea Vandin
 * 
 */
public class MyParserUtil {

	public static String visitExpr(Expression expr){
		return MyParserUtil.visitExpr(expr,null);
	}
	
	/**
	 * Parse an arithmetic expression enriched with parameters or species
	 * @param expr
	 * @return
	 */
	public static String visitExpr(Expression expr, Set<String> undefinedSpeciesNames){
		String leftVisited;  
		String rightVisited;
		if(expr instanceof NumberLiteral){
			NumberLiteral val = (NumberLiteral)expr;
			return String.valueOf(val.getValue());
		}
		else if(expr instanceof Parameter){
			Parameter val = (Parameter)expr;
			return val.getName();
		} 
		/*else if(expr instanceof Species){
			Species val = (Species)expr;
			if(val.getName()==null){
				//I HAVE TO OBTAIN SOMEHOW THE TEXT WRITTEN IN THE EXPRESSION....
				//addUndefinedSpecies(ode,undefinedSpecies)
			}
			return val.getName();
		} */
		else if(expr instanceof ReferenceToParameterOrSymbolicParameterOrSpecies || expr instanceof ReferenceToParameterOrSymbolicParameter){
			String name = null;
			if(expr instanceof ReferenceToParameterOrSymbolicParameterOrSpecies){
				ParameterOrSymbolicParameterOrSpecies ref = ((ReferenceToParameterOrSymbolicParameterOrSpecies)expr).getReference();
				if(ref instanceof Parameter){
					name = ((Parameter) ref).getName();
				}
				else if(ref instanceof SymbolicParameter){
					name = ((SymbolicParameter) ref).getName();
				}
				else if(ref instanceof Species){
					name = ((Species) ref).getName();
				}
				else if(ref instanceof AlgSpecies){
					name = ((AlgSpecies) ref).getName();
				}
			}
			else{
				ParameterOrSymbolicParameter ref = ((ReferenceToParameterOrSymbolicParameter)expr).getReference();
				if(ref instanceof Parameter){
					name = ((Parameter) ref).getName();
				}
				else if(ref instanceof SymbolicParameter){
					name = ((SymbolicParameter) ref).getName();
				}
			}
			
			if(name!=null){
				return name;
			}
			else{
				ICompositeNode node = NodeModelUtils.findActualNodeFor(expr);
				int offset=node.getOffset();
				String text = node.getText();
				text = text.substring(offset-node.getTotalOffset());
				if(undefinedSpeciesNames!=null){
					undefinedSpeciesNames.add(text);
				}
				return text;
			}
		} 
		/*
		else if(expr instanceof ReferenceToParameterOrSymbolicParameterOrSpecies){
			ParameterOrSymbolicParameterOrSpecies ref = ((ReferenceToParameterOrSymbolicParameterOrSpecies)expr).getReference();
			String name = null;
			if(ref instanceof Parameter){
				name = ((Parameter) ref).getName();
			}
			else if(ref instanceof SymbolicParameter){
				name = ((SymbolicParameter) ref).getName();
			}
			else if(ref instanceof Species){
				name = ((Species) ref).getName();
			}
			else{
				ICompositeNode node = NodeModelUtils.findActualNodeFor(expr);
				int offset=node.getOffset();
				String text = node.getText();
				text = text.substring(offset-node.getTotalOffset());
				if(undefinedSpeciesNames!=null){
					undefinedSpeciesNames.add(text);
				}
				return text;
			}
			return name;
		} 
		else if(expr instanceof ReferenceToParameterOrSymbolicParameter){
			ParameterOrSymbolicParameter ref = ((ReferenceToParameterOrSymbolicParameter)expr).getReference();
			String name = null;
			if(ref instanceof Parameter){
				name = ((Parameter) ref).getName();
			}
			else if(ref instanceof SymbolicParameter){
				name = ((SymbolicParameter) ref).getName();
			}
			else{
				ICompositeNode node = NodeModelUtils.findActualNodeFor(expr);
				int offset=node.getOffset();
				String text = node.getText();
				text = text.substring(offset-node.getTotalOffset());
				if(undefinedSpeciesNames!=null){
					undefinedSpeciesNames.add(text);
				}
				return text;
			}
			return name;
		} 
		*/
		/*else if(expr instanceof ReferenceToParameterOrSpecies){
			ParameterOrSpecies ref = ((ReferenceToParameterOrSpecies)expr).getReference();
			String name = ((ParameterOrSymbolicParameterOrSpecies)ref).getName();
			if(name==null){
				ICompositeNode node = NodeModelUtils.findActualNodeFor(expr);
				int offset=node.getOffset();
				String text = node.getText();
				text = text.substring(offset-node.getTotalOffset());
				if(undefinedSpeciesNames!=null){
					undefinedSpeciesNames.add(text);
				}
				return text;
			}
			else{
				return name;
			}
		}*/ 
		else if(expr instanceof ParameterValue){
			ParameterValue val = (ParameterValue)expr;
			if(val.getValue()==null || val.getValue().getName()==null){
				return "undef";
			}
			else{
				//System.out.println(val.toString());
				return val.getValue().getName();
			}
		} 
		else if(expr instanceof Addition || expr instanceof AdditionWithSpecies  || expr instanceof AdditionWithSpeciesAndCov || expr instanceof AdditionWithSymbPar){
			if(expr instanceof Addition){
				leftVisited = visitExpr(((Addition)expr).getLeft());
				rightVisited = visitExpr(((Addition)expr).getRight());
			}
			else if(expr instanceof AdditionWithSymbPar){
				leftVisited = visitExpr(((AdditionWithSymbPar)expr).getLeft());
				rightVisited = visitExpr(((AdditionWithSymbPar)expr).getRight());
			}
			else if(expr instanceof AdditionWithSpeciesAndCov){
				leftVisited = visitExpr(((AdditionWithSpeciesAndCov)expr).getLeft());
				rightVisited = visitExpr(((AdditionWithSpeciesAndCov)expr).getRight());
			}
			else{
				leftVisited = visitExpr(((AdditionWithSpecies)expr).getLeft());
				rightVisited = visitExpr(((AdditionWithSpecies)expr).getRight());
			}
			leftVisited = addParIfNotTerminal(leftVisited);
			rightVisited = addParIfNotTerminal(rightVisited);
			String ret = leftVisited + " + " + rightVisited;
			return ret;
		}
		else if(expr instanceof Subtraction || expr instanceof SubstractionWithSpecies || expr instanceof SubstractionWithSpeciesAndCov || expr instanceof SubtractionWithSymbPar){
			if(expr instanceof Subtraction){
				leftVisited = visitExpr(((Subtraction)expr).getLeft());
				rightVisited = visitExpr(((Subtraction)expr).getRight());
			}
			else if(expr instanceof SubtractionWithSymbPar){
				leftVisited = visitExpr(((SubtractionWithSymbPar)expr).getLeft());
				rightVisited = visitExpr(((SubtractionWithSymbPar)expr).getRight());
			}
			else if(expr instanceof SubstractionWithSpeciesAndCov){
				leftVisited = visitExpr(((SubstractionWithSpeciesAndCov)expr).getLeft());
				rightVisited = visitExpr(((SubstractionWithSpeciesAndCov)expr).getRight());
			}
			else{
				leftVisited = visitExpr(((SubstractionWithSpecies)expr).getLeft());
				rightVisited = visitExpr(((SubstractionWithSpecies)expr).getRight());
			}
			leftVisited = addParIfNotTerminal(leftVisited);
			rightVisited = addParIfNotTerminal(rightVisited);
			String ret = leftVisited + " - " + rightVisited;
			return ret;
		}
		else if(expr instanceof Multiplication || expr instanceof MultiplicationWithSpecies || expr instanceof MultiplicationWithSpeciesAndCov || expr instanceof MultiplicationWithSymbPar){
			if(expr instanceof Multiplication){
				leftVisited = visitExpr(((Multiplication)expr).getLeft());
				rightVisited = visitExpr(((Multiplication)expr).getRight());
			}
			else if(expr instanceof MultiplicationWithSymbPar){
				leftVisited = visitExpr(((MultiplicationWithSymbPar)expr).getLeft());
				rightVisited = visitExpr(((MultiplicationWithSymbPar)expr).getRight());
			}
			else if(expr instanceof MultiplicationWithSpeciesAndCov){
				leftVisited = visitExpr(((MultiplicationWithSpeciesAndCov)expr).getLeft());
				rightVisited = visitExpr(((MultiplicationWithSpeciesAndCov)expr).getRight());
			}
			else{
				leftVisited = visitExpr(((MultiplicationWithSpecies)expr).getLeft());
				rightVisited = visitExpr(((MultiplicationWithSpecies)expr).getRight());
			}
			leftVisited = addParIfNotTerminal(leftVisited);
			rightVisited = addParIfNotTerminal(rightVisited);
			String ret = leftVisited + " * " + rightVisited;
			return ret;
		}
		else if(expr instanceof Division || expr instanceof DivisionWithSpecies || expr instanceof DivisionWithSpeciesAndCov || expr instanceof DivisionWithSymbPar){
			if(expr instanceof Division){
				leftVisited = visitExpr(((Division)expr).getLeft());
				rightVisited = visitExpr(((Division)expr).getRight());
			}
			else if(expr instanceof DivisionWithSymbPar){
				leftVisited = visitExpr(((DivisionWithSymbPar)expr).getLeft());
				rightVisited = visitExpr(((DivisionWithSymbPar)expr).getRight());
			}
			else if(expr instanceof DivisionWithSpeciesAndCov){
				leftVisited = visitExpr(((DivisionWithSpeciesAndCov)expr).getLeft());
				rightVisited = visitExpr(((DivisionWithSpeciesAndCov)expr).getRight());
			}
			else{
				leftVisited = visitExpr(((DivisionWithSpecies)expr).getLeft());
				rightVisited = visitExpr(((DivisionWithSpecies)expr).getRight());
			}
			leftVisited = addParIfNotTerminal(leftVisited);
			rightVisited = addParIfNotTerminal(rightVisited);
			String ret = leftVisited + " / " + rightVisited;
			return ret;
		}
		else if(expr instanceof Power || expr instanceof PowerWithSpecies  || expr instanceof PowerWithSpeciesAndCov  || expr instanceof PowerWithSymbPar){
			if(expr instanceof Power){
				leftVisited = visitExpr(((Power)expr).getLeft());
				rightVisited = visitExpr(((Power)expr).getRight());
			}
			else if(expr instanceof PowerWithSymbPar){
				leftVisited = visitExpr(((PowerWithSymbPar)expr).getLeft());
				rightVisited = visitExpr(((PowerWithSymbPar)expr).getRight());
			}
			else if(expr instanceof PowerWithSpeciesAndCov){
				leftVisited = visitExpr(((PowerWithSpeciesAndCov)expr).getLeft());
				rightVisited = visitExpr(((PowerWithSpeciesAndCov)expr).getRight());
			}
			else{
				leftVisited = visitExpr(((PowerWithSpecies)expr).getLeft());
				rightVisited = visitExpr(((PowerWithSpecies)expr).getRight());
			}
			leftVisited = addParIfNotTerminal(leftVisited);
			rightVisited = addParIfNotTerminal(rightVisited);
			String ret = leftVisited + " ^ " + rightVisited;
			return ret;
		}
		else if(expr instanceof Max || expr instanceof MaxWithSpecies  || expr instanceof MaxWithSpeciesAndCov || expr instanceof MaxWithSymbPar){
			if(expr instanceof Max){
				leftVisited = visitExpr(((Max)expr).getLeft());
				rightVisited = visitExpr(((Max)expr).getRight());
			}
			else if(expr instanceof MaxWithSymbPar){
				leftVisited = visitExpr(((MaxWithSymbPar)expr).getLeft());
				rightVisited = visitExpr(((MaxWithSymbPar)expr).getRight());
			}
			else if(expr instanceof MaxWithSpeciesAndCov){
				leftVisited = visitExpr(((MaxWithSpeciesAndCov)expr).getLeft());
				rightVisited = visitExpr(((MaxWithSpeciesAndCov)expr).getRight());
			}
			else{
				leftVisited = visitExpr(((MaxWithSpecies)expr).getLeft());
				rightVisited = visitExpr(((MaxWithSpecies)expr).getRight());
			}
			String ret = "max("+leftVisited + " , " + rightVisited+")";
			return ret;
		}
		else if(expr instanceof Min || expr instanceof MinWithSpecies || expr instanceof MinWithSpeciesAndCov || expr instanceof MinWithSymbPar){
			if(expr instanceof Min){
				leftVisited = visitExpr(((Min)expr).getLeft());
				rightVisited = visitExpr(((Min)expr).getRight());
			}
			if(expr instanceof MinWithSymbPar){
				leftVisited = visitExpr(((MinWithSymbPar)expr).getLeft());
				rightVisited = visitExpr(((MinWithSymbPar)expr).getRight());
			}
			else if(expr instanceof MinWithSpeciesAndCov){
				leftVisited = visitExpr(((MinWithSpeciesAndCov)expr).getLeft());
				rightVisited = visitExpr(((MinWithSpeciesAndCov)expr).getRight());
			}
			else{
				leftVisited = visitExpr(((MinWithSpecies)expr).getLeft());
				rightVisited = visitExpr(((MinWithSpecies)expr).getRight());
			}
			String ret = "min("+leftVisited + " , " + rightVisited+")";
			return ret;
		}
		else if(expr instanceof Abs || expr instanceof AbsWithSpecies || expr instanceof AbsWithSpeciesAndCov || expr instanceof AbsWithSymbPar){
			if(expr instanceof Abs){
				leftVisited = visitExpr(((Abs)expr).getLeft());
			}
			else if(expr instanceof AbsWithSymbPar){
				leftVisited = visitExpr(((AbsWithSymbPar)expr).getLeft());
			}
			else if(expr instanceof AbsWithSpeciesAndCov){
				leftVisited = visitExpr(((AbsWithSpeciesAndCov)expr).getLeft());
			}
			else{
				leftVisited = visitExpr(((AbsWithSpecies)expr).getLeft());
			}
			//TODO: check
			String ret = "abs("+leftVisited + ")";
			return ret;
		}
		else if(expr instanceof MinusPrimary || expr instanceof MinusPrimaryWithSpecies || expr instanceof MinusPrimaryWithSpeciesAndCov || expr instanceof MinusPrimaryWithSymbPar){
			if(expr instanceof MinusPrimary){
				leftVisited = visitExpr(((MinusPrimary)expr).getLeft());
			}
			else if(expr instanceof MinusPrimaryWithSymbPar){
				leftVisited = visitExpr(((MinusPrimaryWithSymbPar)expr).getLeft());
			}
			else if(expr instanceof MinusPrimaryWithSpeciesAndCov){
				leftVisited = visitExpr(((MinusPrimaryWithSpeciesAndCov)expr).getLeft());
			}
			else{
				leftVisited = visitExpr(((MinusPrimaryWithSpecies)expr).getLeft());
			}
			leftVisited = addParIfNotTerminal(leftVisited);
			String ret = "-"+leftVisited;
			return ret;
		}
		/*else if(expr instanceof MinusSign){
			MinusSign val = (MinusSign)expr;
			leftVisited = visitExpr((val).getLeft());
			return "-(" + leftVisited+")";
		}*/
		//BEGIN VARIANCES AND COVARIANCES
		else if(expr instanceof VarExpr){
			//leftVisited = visitExpr(((VarExpr)expr).getLeftSpecies());
			Species sp = ((VarExpr)expr).getLeftSpecies();
			String ret = "var("+getName(undefinedSpeciesNames, sp) + ")";
			return ret;
		}
		else if(expr instanceof CovExpr){
			Species leftSpecies = ((CovExpr)expr).getLeftSpecies();
			Species rightSpecies = ((CovExpr)expr).getRightSpecies();
			String ret = "covar("+getName(undefinedSpeciesNames, leftSpecies)+","+ getName(undefinedSpeciesNames, rightSpecies)+ ")";
			return ret;
		}
		/*else if(expr instanceof ReferenceToSpecies){
			Species sp = ((ReferenceToSpecies)expr).getReference();
			return getName(undefinedSpeciesNames, sp);
		} */
		//END VARIANCES AND COVARIANCES
		else{
			throw new UnsupportedOperationException("Unsupported expression: " + expr.toString());
		}
	}

	private static String getName(Set<String> undefinedSpeciesNames, Species sp) {
		if(sp.getName()==null){
			ICompositeNode node = NodeModelUtils.findActualNodeFor(sp);
			int offset=node.getOffset();
			String text = node.getText();
			text = text.substring(offset-node.getTotalOffset());
			if(undefinedSpeciesNames!=null){
				undefinedSpeciesNames.add(text);
			}
			return text;
		}
		else{
			return sp.getName();
		}
	}
	
	private static String addParIfNotTerminal(String expr) {
		boolean add=false;
		String[] operators = new String[]{"+","*","-","/","^","max(","min(","abs("};
		for (String operator : operators) {
			if(expr.contains(operator)){
				add=true;
				break;
			}
		}
		if(add){
			return "("+expr+")";
		}
		else{
			return expr;
		}
	}

	public static String visitComposite(Composite composite) {
		String ret = visitSpeciesWithMult(composite.getSpeciesOfComposite().get(0));
		int i=0;
		for(SpeciesWithMultiplicity speciesWithMult : composite.getSpeciesOfComposite()){
			if(i!=0){
				ret = ret + '+' + visitSpeciesWithMult(speciesWithMult);
			}
			i++;
		}
		return ret;
	}
	
	public static final String UNDEFSPECIESNAME = "undef";
	
	public static String visitSpeciesWithMult(SpeciesWithMultiplicity speciesWithMult) {
		String name =  speciesWithMult.getSpecies().getName();
		if(name==null){
			name=UNDEFSPECIESNAME;
		}
		if(speciesWithMult.getMultiplicity() == 0 || speciesWithMult.getMultiplicity() == 1){
			return name;
		}
		else{
			String ret = String.valueOf(speciesWithMult.getMultiplicity());
			ret = ret + "*" + name;
			return ret;
		}
	}
	
	public static boolean isWindows() {
		String p = System.getProperty("os.name");
		if(p.contains("Windows")){
			return true;
		}
		else {
			return false;
		}
	}
	
	
	public static ModelElementsCollector getModelElements(ModelDefinition modelDef,String absoluteParentPath,String absolutePath){
		ArrayList<String> symbolicParameters = new ArrayList<String>(0);
		ArrayList<ArrayList<String>> parameters = new ArrayList<ArrayList<String>>(0);
		ArrayList<LinkedHashMap<String, String>> reactions = new ArrayList<>(0);
		ArrayList<LinkedHashMap<String, String>> algebraicConstraints = new ArrayList<>(0);
		ArrayList<ArrayList<String>> views = new ArrayList<ArrayList<String>>(0);
		ArrayList<ArrayList<String>> initialConcentrations = new ArrayList<ArrayList<String>>(0);
		ArrayList<ArrayList<String>> initialAlgConcentrations = new ArrayList<ArrayList<String>>(0);
		ArrayList<Command> commandsList = new ArrayList<>(0);
		ArrayList<BooleanCommand> booleanCommandsList = new ArrayList<>(0);
		ArrayList<ArrayList<String>> initialPartition = new ArrayList<ArrayList<String>>(0);
		String importString=null;
		Import importCommand=null;
		ImportFolder importFolderCommand=null;
		BooleanImportFolder booleanImportFolderCommand=null;
		String importFolderString=null;
		String importName = null;
		boolean synchEditor=false;
		ModelDefKind modelDefKind=null;
		
		//boolean isBooleanNetwork= false;
		//I decided to parse them later on, as done for the constraints.
		EList<NodeDefinition> nodeDefinitions=null;
		EList<MVNodeDefinition> mvNodeDefinitions=null;
		
		//I cannot parse them here, because I don't see the core tool. I just pass the constraints to the GUI.
		EList<BoolExpr> constraintsList= null;
		
		String modelName = modelDef.getName();
		

		for (EObject element : modelDef.getElements()) {
			if(element instanceof Command){
				commandsList.add((Command)element);
			}
			else if(element instanceof Import){
				importName = element.toString();
				importName = importName.substring(importName.lastIndexOf(".impl.")+6);
				importName = importName.substring(0,importName.lastIndexOf("Impl"));
				importCommand =(Import)element;
				importString = parseImport(importCommand,importName,absoluteParentPath);
				modelDefKind=ModelDefKind.IMPORT;
				synchEditor=false;//synchEditor=importCommand.getParams().isSynchEditor();
			}
			else if (element instanceof ImportFolder) {
				importName = element.toString();
				importName = importName.substring(importName.lastIndexOf(".impl.")+6);
				importName = importName.substring(0,importName.lastIndexOf("Impl"));
				importFolderCommand =(ImportFolder)element;
				importFolderString = parseImportFolder(importFolderCommand,importName,absoluteParentPath);
				modelDefKind=ModelDefKind.IMPORTFOLDER;
				synchEditor=false;//synchEditor=importCommand.getParams().isSynchEditor();
			}
			else if (element instanceof BooleanImportFolder) {
				importName = element.toString();
				importName = importName.substring(importName.lastIndexOf(".impl.")+6);
				importName = importName.substring(0,importName.lastIndexOf("Impl"));
				booleanImportFolderCommand =(BooleanImportFolder)element;
				importFolderString = parseBooleanImportFolder(booleanImportFolderCommand,importName,absoluteParentPath);
				modelDefKind=ModelDefKind.BOOLEANIMPORTFOLDER;
				synchEditor=false;//synchEditor=importCommand.getParams().isSynchEditor();
			}
			else if(element instanceof SymbolicParametersList){
				EList<SymbolicParameter> symbolicParametersList = ((SymbolicParametersList)element).getSymbolicParameters();
				symbolicParameters = new ArrayList<String>(symbolicParametersList.size());
				for (SymbolicParameter param : symbolicParametersList) {
					//name
					symbolicParameters.add(param.getName());
				}
			}
			else if(element instanceof ConstraintsOnSymbolicParametersList){
				//I cannot parse them here, because I don't see the core tool. I just pass the constraints to the GUI.
				constraintsList = ((ConstraintsOnSymbolicParametersList) element).getConstraints();
			}
			else if(element instanceof ParametersList){
				EList<Parameter> parametersList = ((ParametersList)element).getParameters();
				parameters = new ArrayList<ArrayList<String>>(parametersList.size());
				for (Parameter param : parametersList) {
					//name,value
					ArrayList<String> par = new ArrayList<>(2);
					parameters.add(par);
					par.add(param.getName());
					//String val = MyParserUtil.visitExpr(param.getParamValue().getValue());
					String val = MyParserUtil.visitExpr(param.getParamValue());
					par.add(val);
				}
			}
			else if(element instanceof InitPartition){
				EList<Block> blocks = ((InitPartition) element).getAllBlocks();
				initialPartition = new ArrayList<ArrayList<String>>(blocks.size());
				for (Block block : blocks){
					ArrayList<String> currentBlock=new ArrayList<>(block.getAllSpecies().size());
					initialPartition.add(currentBlock);
					//for (Species species : block.getAllSpecies()) {
					
					//for (ReferenceToSpeciesOrNode species : block.getAllSpecies()) {
					//species.getReference().g
					for (SpeciesOrNode species : block.getAllSpecies()) {
						if(species.getName()!=null){
							currentBlock.add(species.getName());
						}
					}
				}
			}
			else if(element instanceof ReactionsList){
				modelDefKind=ModelDefKind.RN;
				EList<Reaction> reactionsList = ((ReactionsList)element).getAllReactions();
				reactions = new ArrayList<>(reactionsList.size());
				for(Reaction reac : reactionsList){
					//reagent,product,rate,kind of reaction
					LinkedHashMap<String, String> r = new LinkedHashMap<String, String>(5);
					reactions.add(r);
					r.put("reagents", MyParserUtil.visitComposite(reac.getReagents()));
					r.put("products", MyParserUtil.visitComposite(reac.getProducts()));
					if(reac.getRate() instanceof MassActionRate){
						//r.add(reac.getRate().getValue().toString());
						//reactions.put(reac.getReagents().getSpeciesOfComposite().get(0).getSpecies().getName(), value)
						String val = MyParserUtil.visitExpr(((MassActionRate)reac.getRate()).getRate());
						r.put("rate", val);
						r.put("kind", "massaction");
					}
					else if(reac.getRate() instanceof ArbitraryRate){
						String val = MyParserUtil.visitExpr(((ArbitraryRate)reac.getRate()).getRate());
						r.put("rate", val);
						r.put("kind", "arbitrary");
					}
					else if(reac.getRate() instanceof HillRate){
						//K = ArithmeticExpression 'R1' R1 = ArithmeticExpression 'R2' R2 = ArithmeticExpression 'n1' n1 = ArithmeticExpression 'n2' n2 = ArithmeticExpression
						String K = MyParserUtil.visitExpr(((HillRate)reac.getRate()).getK());
						String R1 = MyParserUtil.visitExpr(((HillRate)reac.getRate()).getR1());
						String R2 = MyParserUtil.visitExpr(((HillRate)reac.getRate()).getR2());
						String n1 = String.valueOf(((HillRate)reac.getRate()).getN1());
						String n2 = String.valueOf(((HillRate)reac.getRate()).getN2());
						//Hill K R1 R2 n1 n2
						r.put("rate", "Hill "+K+ " "+R1+ " "+R2+ " "+n1+ " "+n2);
						r.put("kind", "hill");
					}
					
					if(reac.getName()!=null && reac.getName().size()>0){
						StringBuilder sb = new StringBuilder();
						for (String id : reac.getName()) {
							sb.append(id);
							sb.append(' ');
						}
						r.put("id", sb.toString().trim());
					}
				}
			}
			else if(element instanceof ODEsList){
				modelDefKind=ModelDefKind.ODE;
				EList<ODE> odes = ((ODEsList)element).getOdes();
				reactions = new ArrayList<>(odes.size());
				for(ODE ode : odes){
					//Species speciesOfOde = ode.getName().getAllSpecies().get(0); AAA
					Species speciesOfOde = ode.getName();
					Expression drift = ode.getDrift();
					String driftString = MyParserUtil.visitExpr(drift);
					//IComposite products = new Composite(speciesOfODE,speciesOfODE);
					//ICRNReaction reaction = new CRNReactionArbitrary((IComposite)speciesOfODE, products, body,varsName);
					//speciesOfTheODE,arbitrary,rate
					//ArrayList<String> r = new ArrayList<>(4);
					LinkedHashMap<String, String> r = new LinkedHashMap<String, String>(5);
					reactions.add(r);
					r.put("reagents",speciesOfOde.getName());//r.add(2*speciesOfOde.getName());
					r.put("products",speciesOfOde.getName()+"+"+speciesOfOde.getName());
					r.put("rate",driftString);
					r.put("kind","ode");
					/*
					 * It would make sense to assign 'd(s)' as id of the ODE of species 's'. 
					 * We have to decide if we want this or not  
					if(ode.getName()!=null){
						r.put("id", "d("+ode.getName()+")");
					}
					*/
				}
			}
			else if(element instanceof AlgebraicList){
				//modelDefKind=ModelDefKind.DAE;
				EList<ALG> algs = ((AlgebraicList)element).getAlgs();
				algebraicConstraints = new ArrayList<>(algs.size());
				for(ALG alg : algs){
					//Species speciesOfOde = alg.getName().getAllSpecies().get(0); AAA
					AlgSpecies speciesOfAlg = alg.getName();
					Expression drift = alg.getDrift();
					String driftString = MyParserUtil.visitExpr(drift);
					//IComposite products = new Composite(speciesOfODE,speciesOfODE);
					//ICRNReaction reaction = new CRNReactionArbitrary((IComposite)speciesOfODE, products, body,varsName);
					//speciesOfTheODE,arbitrary,rate
					//ArrayList<String> r = new ArrayList<>(4);
					LinkedHashMap<String, String> r = new LinkedHashMap<String, String>(5);
					algebraicConstraints.add(r);
					r.put("reagents",speciesOfAlg.getName());//r.add(2*speciesOfOde.getName());
					r.put("products",speciesOfAlg.getName()+"+"+speciesOfAlg.getName());
					r.put("rate",driftString);
					r.put("kind","ode");
					/*
					 * It would make sense to assign 'd(s)' as id of the ODE of species 's'. 
					 * We have to decide if we want this or not  
					if(ode.getName()!=null){
						r.put("id", "d("+ode.getName()+")");
					}
					*/
				}
			}
			else if(element instanceof ViewsList){
				EList<View> viewsList = ((ViewsList)element).getAllViews();
				views = new ArrayList<ArrayList<String>>(viewsList.size());
				for (View v : viewsList) {
					//name,value
					ArrayList<String> view = new ArrayList<>(2);
					views.add(view);
					view.add(v.getName());
					String val = MyParserUtil.visitExpr(v.getExpr());
					view.add(val);
				}
			}
			else if(element instanceof ICList){
				EList<Species> icList = ((ICList)element).getAllSpecies();
				initialConcentrations = new ArrayList<ArrayList<String>>(icList.size());
				for (Species ic : icList) {
					//name,value
					ArrayList<String> initialConcentration = new ArrayList<>(3);
					initialConcentrations.add(initialConcentration);
					initialConcentration.add(ic.getName());
					if(ic.getIc()!=null){
						String val = MyParserUtil.visitExpr(ic.getIc());
						initialConcentration.add(val);
					}
					else{
						initialConcentration.add("0.0");
					}
					if(ic.getOriginalName()!=null){
						initialConcentration.add(ic.getOriginalName());
					}
				}
			}
			else if(element instanceof AlgICList){
				EList<AlgSpecies> algICList = ((AlgICList)element).getAllAlgSpecies();
				initialAlgConcentrations = new ArrayList<ArrayList<String>>(algICList.size());
				for (AlgSpecies ic : algICList) {
					//name,value
					ArrayList<String> initialConcentration = new ArrayList<>(3);
					initialAlgConcentrations.add(initialConcentration);
					initialConcentration.add(ic.getName());
					if(ic.getIc()!=null){
						String val = MyParserUtil.visitExpr(ic.getIc());
						initialConcentration.add(val);
					}
					else{
						initialConcentration.add("0.0");
					}
					if(ic.getOriginalName()!=null){
						initialConcentration.add(ic.getOriginalName());
					}
				}
			}
			/*else if(element instanceof SpeciesList){
				EList<SpeciesForCRN> speciesList = ((SpeciesList)element).getAllSpecies();
				initialConcentrations = new ArrayList<ArrayList<String>>(speciesList.size());
				for (SpeciesForCRN sp : speciesList) {
					//name,value (of the IC) 
					ArrayList<String> initialConcentration = new ArrayList<>(2);
					initialConcentrations.add(initialConcentration);
					initialConcentration.add(sp.getName());
					//System.out.println(sp.getName());
					Expression ic = sp.getIc();
					if(ic==null){
						initialConcentration.add("0.0");
					}
					else{
						String val = MyParserUtil.visitExpr(ic);
						initialConcentration.add(val);
					}

				}
			}*/
			
			//MultiValued network
			else if(element instanceof MVNodeDeclarations){
				modelDefKind=ModelDefKind.BOOLEANMV;
				
				EList<MVNode> icList = ((MVNodeDeclarations)element).getAllMVNodes();
				
				initialConcentrations = new ArrayList<ArrayList<String>>(icList.size());
				for (MVNode ic : icList) {
					//name,value,originalName
					ArrayList<String> initialConcentration = new ArrayList<>(3);
					initialConcentrations.add(initialConcentration);
					//Name,ic,max
					initialConcentration.add(ic.getName());
					initialConcentration.add(String.valueOf(ic.getIc()));
					int max=ic.getMax();
					if(ic.getMax()==0) {
						max=1;
					}
					initialConcentration.add(String.valueOf(max));
					//initialConcentration.add(String.valueOf(ic.isIc()));
					if(ic.getOriginalName()!=null){
						initialConcentration.add(ic.getOriginalName());
					}
				}
			}
			else if(element instanceof MVNodeDefinitions){
				mvNodeDefinitions = ((MVNodeDefinitions)element).getMvNodeDefinitions();
			}
//			else if(element instanceof BooleanCommand){
//				booleanCommandsList.add((BooleanCommand)element);
//			}
			
			
			//Boolean network
			else if(element instanceof NodeDeclarations){
				//isBooleanNetwork=true;
				modelDefKind=ModelDefKind.BOOLEAN;
				
				EList<Node> icList = ((NodeDeclarations)element).getAllNodes();
				
				initialConcentrations = new ArrayList<ArrayList<String>>(icList.size());
				for (Node ic : icList) {
					//name,value,originalName
					ArrayList<String> initialConcentration = new ArrayList<>(3);
					initialConcentrations.add(initialConcentration);
					initialConcentration.add(ic.getName());
					//initialConcentration.add(String.valueOf(booleanValueBNToBoolean(ic.getIc())));
					if(ic.getIc()==null) {
						initialConcentration.add("false");
					}
					else if(ic.getIc() instanceof it.imt.erode.crn.chemicalReactionNetwork.False) {
						initialConcentration.add("false");
					}
					else if(ic.getIc() instanceof it.imt.erode.crn.chemicalReactionNetwork.True) {
						initialConcentration.add("true");
					}
					else {
						throw new UnsupportedOperationException("The initial condition can be one of false|0|true|1");
					}
					//initialConcentration.add(String.valueOf(ic.isIc()));
					if(ic.getOriginalName()!=null){
						initialConcentration.add(ic.getOriginalName());
					}
				}
			}
			else if(element instanceof NodeDefinitions){
				nodeDefinitions = ((NodeDefinitions)element).getNodeDefinitions();
				/*booleanUpdateFunctions = new LinkedHashMap<>(nodeDefs.size());
				for(NodeDefinition nodeDef : nodeDefs){
					booleanUpdateFunctions.put(nodeDef.getName(), nodeDef.getUpdateFunction());
				}*/
			}
			else if(element instanceof BooleanCommand){
				booleanCommandsList.add((BooleanCommand)element);
			}
			
		}
	
		ModelElementsCollector mec = new ModelElementsCollector(modelName,symbolicParameters,constraintsList,parameters, reactions, algebraicConstraints,views, initialConcentrations, initialAlgConcentrations,
				initialPartition, commandsList, booleanCommandsList, importString, 
				importCommand, importFolderString, importFolderCommand,booleanImportFolderCommand,importName, modelDefKind,
				nodeDefinitions,mvNodeDefinitions,
				synchEditor,absolutePath);
		return mec;
	
	}

	
//	private static boolean booleanValueBNToBoolean(String ic) {
//		if(ic==null) {
//			return false;
//		}
//		else if (ic.equalsIgnoreCase("true")||ic.equalsIgnoreCase("1")) {
//			return true;
//		}
//		else{
//			return false;
//		}
//	}

	public static List<String> parseCommands(ModelElementsCollector mec, String absoluteParentPath) {
		List<String> commands = new ArrayList<String>();
		commands.add("newline");
		for(BooleanCommand command : mec.getBooleanCommandsList()){
			String commandName = command.toString();
			commandName = commandName.substring(commandName.lastIndexOf(".impl.")+6);
			commandName = commandName.substring(0,commandName.lastIndexOf("Impl"));
			if(command instanceof BooleanReduction){
				String red = parseBooleanReduce((BooleanReduction)command,commandName,absoluteParentPath,false);
				commands.add(red);
			}
			else if(command instanceof UpdateStatusBooleanCommand){
				BooleanReduction reduction = ((UpdateStatusBooleanCommand)command).getReduction();
				if(reduction!=null) {
					commandName = reduction.toString();
					commandName = commandName.substring(commandName.lastIndexOf('.')+1,commandName.lastIndexOf("Impl"));
					String red = parseBooleanReduce(reduction,commandName,absoluteParentPath,true);
					commands.add("this="+red);
				}
				else {
					curry curry=((UpdateStatusCommand)command).getCurrying();
					commandName = curry.toString();
					commandName = commandName.substring(commandName.lastIndexOf('.')+1,commandName.lastIndexOf("Impl"));
					String cur = parseCurry(curry,commandName);
					commands.add("this="+cur);
				}
				
			}
			else if(command instanceof ExportBN) {
				String imp = parseExportBN((ExportBN)command,commandName,absoluteParentPath,mec.getAbsolutePath());
				commands.add(imp);
			}
			else if(command instanceof newline){
				commands.add("newLine");
			}
			commands.add("newline");
		}
		for(Command command : mec.getCommandsList()){
			String commandName = command.toString();
			commandName = commandName.substring(commandName.lastIndexOf(".impl.")+6);
			commandName = commandName.substring(0,commandName.lastIndexOf("Impl"));
			//commandName = commandName.substring(commandName.lastIndexOf('.')+1,commandName.lastIndexOf("Impl"));
			//commands.add(commandName+"({})");

			if(command instanceof Reduction){
				String red = parseReduce((Reduction)command,commandName,absoluteParentPath,false);
				commands.add(red);
			}
			else if(command instanceof Approximation){
				String red = parseApproximation((Approximation)command,commandName,absoluteParentPath,false);
				commands.add(red);
			}
//			else if(command instanceof UpdateStatusReduction){
//				Reduction reduction = ((UpdateStatusReduction)command).getReduction();
//				commandName = reduction.toString();
//				commandName = commandName.substring(commandName.lastIndexOf('.')+1,commandName.lastIndexOf("Impl"));
//				String red = parseReduce(reduction,commandName,absoluteParentPath,true);
//				commands.add("this="+red);
//			}
			else if(command instanceof UpdateStatusCommand){
				Reduction reduction = ((UpdateStatusCommand)command).getReduction();
				if(reduction!=null) {
					commandName = reduction.toString();
					commandName = commandName.substring(commandName.lastIndexOf('.')+1,commandName.lastIndexOf("Impl"));
					String red = parseReduce(reduction,commandName,absoluteParentPath,true);
					commands.add("this="+red);
				}
				else {
					curry curry=((UpdateStatusCommand)command).getCurrying();
					commandName = curry.toString();
					commandName = commandName.substring(commandName.lastIndexOf('.')+1,commandName.lastIndexOf("Impl"));
					String cur = parseCurry(curry,commandName);
					commands.add("this="+cur);
				}
				
			}
			else if(command instanceof Analysis){
				String an = parseAnalysis((Analysis)command,commandName,absoluteParentPath);
				commands.add(an);
			}
			/*else if(command instanceof Import){
						String imp = parseImport((Import)command,commandName);
						commands.add(imp);
					}*/
			else if(command instanceof Export){
				String imp = parseExport((Export)command,commandName,absoluteParentPath,mec.getAbsolutePath());
				commands.add(imp);
			}
			else if(command instanceof onTheFlyBR) {
				String imp = parseOnTheFlyBR((onTheFlyBR)command,commandName,absoluteParentPath,mec.getAbsolutePath());
				commands.add(imp);
			}
			else if(command instanceof onTheFlyFR) {
				String imp = parseOnTheFlyFR((onTheFlyFR)command,commandName,absoluteParentPath,mec.getAbsolutePath());
				commands.add(imp);
			}
			else if(command instanceof onTheFlyBRRecursive) {
				String imp = parseOnTheFlyBR((onTheFlyBRRecursive)command,commandName,absoluteParentPath,mec.getAbsolutePath());
				commands.add(imp);
			}
			else if(command instanceof setIC){
				setIC com = (setIC)command;
				StringBuilder sb = new StringBuilder(commandName);
				sb.append("({");
				sb.append("speciesName=>");
				sb.append(com.getSpecies().getName());
				sb.append(',');
				sb.append("expr=>");
				sb.append(MyParserUtil.visitExpr(com.getExpr()));
				sb.append("})");
				commands.add(sb.toString());
			}
			else if(command instanceof setParam){
				setParam com = (setParam)command;
				StringBuilder sb = new StringBuilder(commandName);
				sb.append("({");
				sb.append("paramName=>");
				Parameter par = com.getParamName();
				if(par!=null){
					sb.append(par.getName());
				}
				else{
					String p = com.getParamNameString();
					sb.append(p);
				}
				
				sb.append(',');
				sb.append("expr=>");
				sb.append(MyParserUtil.visitExpr(com.getExpr()));
				sb.append("})");
				commands.add(sb.toString());
			}
			/*else if(command instanceof man){
				commands.add("--man");
			}
			else if(command instanceof print){
				commands.add("print()");
			}
			else if(command instanceof help){
				commands.add("--help");
			}*/
			else if(command instanceof newline){
				commands.add("newLine");
			}
			else {
				/*String commandName = command.toString();
						commandName = commandName.substring(commandName.lastIndexOf('.')+1,commandName.lastIndexOf("Impl"));
						commands.add(commandName+"({})");*/
			}
			commands.add("newline");
		}
		
		
		
		//Add here commands that you want to add automatically when  loading a model addcommands commandstoadd
		//
		//String redFile ="/Users/andrea/OneDrive - Danmarks Tekniske Universitet/runtimes/runtime-ERODE.product(4)/TCS_CMSB/ERODE models/mass-action kinetics/maExplicit/BIOMD0000000002FE.ode";
		
		//automatic CoRNFE
		//generateCommands(mec, absoluteParentPath, commands, new String[]{"CoRNFE"},false);
		
		//automatical reduceFE reduceSE reduction generate
		//generateCommands(mec, absoluteParentPath, commands, new String[]{"SE"},false);
		//generateCommands(mec, absoluteParentPath, commands, new String[]{"FE"},false);
		//generateCommands(mec, absoluteParentPath, commands, new String[]{"SMB"},false);
		/*
		boolean applyCurrying=false;
		//String [] reductions= {"FE","BE", "SMB"};	//MRN
		String [] reductions= {"FE","BE"};		//PRN
		//String [] reductions= {"BDE","FDE"};		//NRN
		//String [] reductions= {"BDE"};		//NRN
		//String [] reductions= {"FDE"};		//NRN
		
		//String [] reductions= {"BE"};
		//String [] reductions= {"FE","BE","SE"};
		//String [] reductions= {"BDE"};
		//String [] reductions= {"SMB"}; applyCurrying=false;
		//String [] reductions= {"FDE"};
		//reduceSMB(reducedFile= "./reductionThesisExtra/MODEL8262229752.SMB.ode", addSelfLoops = true) why true? With true it becomes as SE
		//String [] reductions= {"FDE"};
		generateCommands(mec, absoluteParentPath, commands, reductions,applyCurrying);
		*/
		
		/*
		//String folder = "curated_erode_mass_action";
		String folder = "non_curated_erode_mass_action";
		String outFile = computeFileName(".."+File.separator+folder+File.separator+mec.getModelName()+".ode",absoluteParentPath);
		String com ="exportMACRN({fileOut=>"+outFile+"})";
		commands.add(com);
		*/
		
		
		//String folder = "curated_erode_arbitrary_polynomial";
		//String folder = "non_curated_erode_arbitrary_polynomial";
		/*
		String folder = "erode_MARN";
		String outFile = computeFileName(".."+File.separator+folder+File.separator+mec.getModelName()+".ode",absoluteParentPath);
		String com ="exportMARN({fileOut=>"+outFile+"})";
		commands.add(com);
		*/
		
		
		
		
		//commands.add("exportZ3({question=>EFL,fileOut=>/Users/andrea/Desktop/pippo.z3})");
		
		return commands;
	}

	
	@SuppressWarnings("unused")
	private static void generateCommands(final ModelElementsCollector mec, final String absoluteParentPath, final List<String> commands,
			final String[] reductions,final boolean applyCurrying) {
		
		generateCommands(mec, absoluteParentPath, commands,reductions,false,"");
		
		if(applyCurrying) {
			String com ="this=curry({paramsToCurry=>ALL,singleoutParams=>true,preserveUserPartion=>false})";
			commands.add(com);
			commands.add("newline");
			String suffix="Curried";
			generateCommands(mec, absoluteParentPath, commands,reductions,applyCurrying,suffix);
		}
	}
	
	private static void generateCommands(ModelElementsCollector mec, String absoluteParentPath, List<String> commands,
			String[] reductions,boolean applyCurrying,String suffix) {
		for(String red : reductions ) {
			String redFile = computeFileName(red+suffix+File.separator+mec.getModelName()+".ode",absoluteParentPath);
			String com ="reduce"+red+"({reducedFile=>"+redFile;
			// reduceFE(reducedFile="fe/aaa.ode",csvFile="fe.csv")
			String csvFile = computeFileName("reductionsInfo"+File.separator+red+suffix+".csv",absoluteParentPath);
			com+= ",csvFile=>"+csvFile;
			if(red.equals("BE") ||red.equals("BDE")) {
				if(applyCurrying) {
					//I have to prepartition according to the IC, but also according to the user-defined partition.
					//	In fact, the user-defined partition has been created by the currying so that it contains one block per curried parameter.
					com+=", prePartition=>USER_and_IC})";
				}
				else {
					com+=", prePartition=>IC})";
				}
			}
			else if(red.equals("SMB")) {
				com+=", oneLabelAtATime=>true,addSelfLoops=>false})";
			}
			else if(red.equals("CoRNFE")) {
				//com+=", computeOnlyPartition=>true,percentagePerturbation=>101.0})";
				//com+=", computeOnlyPartition=>true,absolutePerturbation=>1.0})";
				//com+=", computeOnlyPartition=>true,absolutePerturbation=>2.0})";
				
				//com+=", computeOnlyPartition=>true,percentagePerturbation=>101.0,prePartition=>Outputs})";
				//com+=", computeOnlyPartition=>true,absolutePerturbation=>1.0,prePartition=>Outputs})";
				//com+=", computeOnlyPartition=>true,absolutePerturbation=>2.0,prePartition=>Outputs})";
				
				//com+=", computeOnlyPartition=>true,percentagePerturbation=>101.0,prePartition=>Outputs_singleton})";
				com+=", computeOnlyPartition=>true,absolutePerturbation=>2.0,prePartition=>Outputs_singleton})";
				
			}
			else {
				com+="})";
			}
			commands.add(com);
			commands.add("newline");
		}
	}
	private static String parseImportFolder(ImportFolder imp, String commandName, String absoluteParentPath) {
		StringBuilder sb = new StringBuilder(commandName);
		sb.append("({");
		if(imp instanceof importSBMLFolder) {
			//BEGIN To be moved outside if when we will have more 'ImportFolder'
			sb.append("folderIn=>");
			sb.append(computeFileName(((importSBMLFolder) imp).getParams().getFolderIn(), absoluteParentPath,false));
			sb.append(',');
			sb.append("folderOut=>");
			sb.append(computeFileName(((importSBMLFolder) imp).getParams().getFolderOut(), absoluteParentPath,false));
			sb.append(',');
			//END To be moved outside if when we will have more 'ImportFolder'
			if(((importSBMLFolder) imp).isForceMassAction()){
				sb.append("forceMassAction=>true");
				sb.append(',');
			}
		}
		
		sb.deleteCharAt(sb.length()-1);
		sb.append("})");
		return sb.toString();
	}
	private static String parseBooleanImportFolder(BooleanImportFolder imp, String commandName, String absoluteParentPath) {
		StringBuilder sb = new StringBuilder(commandName);
		sb.append("({");
		if(imp instanceof importBNetFolder|| imp instanceof importSBMLQualFolder) {
			//imp.
			ParametersImportFolder params = imp.getParams();
			//BEGIN To be moved outside if when we will have more 'ImportFolder'
			sb.append("folderIn=>");
			sb.append(computeFileName(params.getFolderIn(), absoluteParentPath,false));
			sb.append(',');
			sb.append("folderOut=>");
			sb.append(computeFileName(params.getFolderOut(), absoluteParentPath,false));
			sb.append(',');
			//END To be moved outside if when we will have more 'ImportFolder'
		}
		
		sb.deleteCharAt(sb.length()-1);
		sb.append("})");
		return sb.toString();
	}
	
	private static String parseImport(Import imp, String commandName, String absoluteParentPath) {
		StringBuilder sb = new StringBuilder(commandName);
		sb.append("({");
		sb.append("fileIn=>");
		sb.append(computeFileName(imp.getParams().getFileIn(), absoluteParentPath,false));
		//sb.append(imp.getParams().getFileIn());
		sb.append(',');
		if(imp instanceof importMRMC){
			String labellingFile = ((importMRMC)imp).getLabellingFile();
			if(labellingFile!=null){
				sb.append("labellingFile=>");
				sb.append(computeFileName(labellingFile, absoluteParentPath,false));
				sb.append(',');
			}
		}
		else if(imp instanceof importSpaceEx){
			String mainComponent = ((importSpaceEx)imp).getMainComponent();
			if(mainComponent!=null){
				sb.append("mainComponent=>");
				sb.append(mainComponent);
				sb.append(',');
			}
			sb.append("odes=>");
			sb.append(((importSpaceEx)imp).isOdes());
			sb.append(',');
		}
		else if(imp instanceof importUMIST){
			String fileInSpecies = ((importUMIST)imp).getFileInSpecies();
			if(fileInSpecies!=null){
				sb.append("fileInSpecies=>");
				sb.append(computeFileName(fileInSpecies, absoluteParentPath,false));
				sb.append(',');
			}
		}
		else if(imp instanceof importOSU){
			int linesToSkip = ((importOSU)imp).getLinesToSkip();
			sb.append("linesToSkip=>");
			sb.append(linesToSkip);
			sb.append(',');
		}
		else if(imp instanceof importChemKin){
			String thermoFile = ((importChemKin)imp).getThermoDynamicFile();
			if(thermoFile!=null){
				sb.append("thermoDynamicFile=>");
				sb.append(computeFileName(thermoFile, absoluteParentPath,false));
				sb.append(',');
			}
		}
		else if(imp instanceof importLinearSystemAsCCSVMatrix){
			String matrixForm = ((importLinearSystemAsCCSVMatrix)imp).getMatrixForm();
			sb.append("form=>");
			sb.append(matrixForm);
			sb.append(',');
		}
		else if(imp instanceof importAffineSystem){
			/*String aFile = ((importAffineSystem)imp).getAFile();
			sb.append("AFile=>");
			sb.append(aFile);
			sb.append(',');*/
			
			String bFile = ((importAffineSystem)imp).getBFile();
			sb.append("bFile=>");
			sb.append(computeFileName(bFile, absoluteParentPath,false));
			sb.append(',');
			
			String icFile = ((importAffineSystem)imp).getIcFile();
			sb.append("icFile=>");
			sb.append(computeFileName(icFile, absoluteParentPath,false));
			sb.append(',');
			
			sb.append("createParams=>"+((importAffineSystem)imp).isCreateParams());
			sb.append(',');
		}
		else if(imp instanceof importLinearSystemWithInputs) {
//			int inputs = ((importLinearSystemWithInputs)imp).getInputs();
//			sb.append("inputs=>");
//			sb.append(inputs);
//			sb.append(',');
			
			String bFile = ((importLinearSystemWithInputs)imp).getBFile();
			sb.append("bFile=>");
			sb.append(computeFileName(bFile, absoluteParentPath,false));
			sb.append(',');
		}
		else if(imp instanceof importSBML){
			if(((importSBML) imp).isForceMassAction()){
				sb.append("forceMassAction=>true");
				sb.append(',');
			}
		} 
		else if(imp instanceof importBNG){
			if(((importBNG) imp).isFullSpeciesNames()){
				sb.append("compactNames=>false");
			}
			else{
				sb.append("compactNames=>true");
				sb.append(',');
				sb.append("writeFileWithSpeciesNameCorrespondences=>true");
			}
			sb.append(',');
			if(((importBNG) imp).isHighMemory()){
				sb.append("lowMemory=>false");
			}
			else {
				sb.append("lowMemory=>true");
			}
			sb.append(',');
		} 
		else if(imp instanceof importKonect){
			if(((importKonect) imp).isDoNotPreProcessEdges()){
				sb.append("ensureUndirectedGraph=>false");
			}
			else{
				sb.append("ensureUndirectedGraph=>true");
			}
			sb.append(',');
		}
		/*else if(imp instanceof importMatlabODEs){
			boolean polyODEs = ((importMatlabODEs)imp).isPolynomialODEs();
			sb.append("polynomialODEs=>");
			sb.append(polyODEs);
			sb.append(',');
		}*/



		sb.deleteCharAt(sb.length()-1);
		sb.append("})");
		return sb.toString();
	}

	private static String parseCurry(curry curry, String commandName) {
		StringBuilder sb = new StringBuilder(commandName);
		sb.append("({");
		paramsToCurry parsToCurry = curry.getParamsToCurry();
		parseParamsToCurry(parsToCurry, sb);
		sb.deleteCharAt(sb.length()-1);
		
		sb.append(',');
		sb.append("singleoutParams=>");
		sb.append(curry.isSingleoutParams());
		
		sb.append(',');
		sb.append("preserveUserPartion=>");
		sb.append(curry.isPreserveUserPartion());
		
		
		sb.append("})");
		return sb.toString();
	}
	
	private static String parseExportBN(ExportBN exp, String commandName, String absoluteParentPath, String absolutePath) {
		StringBuilder sb = new StringBuilder(commandName);
		sb.append("({");
		sb.append("fileOut=>");
		sb.append(computeFileName(exp.getParams().getFileOut(), absoluteParentPath));
		sb.append(',');
		
		
		if(exp instanceof writeBN){
			boolean originalNames = ((writeBN)exp).isOriginalNames();
			if(originalNames) {
				sb.append("originalNames=>true,");
			}
		}
		else if(exp instanceof exportBoolNet){
			boolean originalNames = ((exportBoolNet)exp).isOriginalNames();
			if(originalNames) {
				sb.append("originalNames=>true,");
			}
		}
		else if(exp instanceof exportSBMLQual) {
			
		}
		
		sb.deleteCharAt(sb.length()-1);
		sb.append("})");
		return sb.toString();
	}
	
	private static String parseOnTheFlyBR(onTheFlyBRRecursive exp, String commandName, String absoluteParentPath, String absolutePath) {
		StringBuilder sb = new StringBuilder(commandName);
		sb.append("({");
		sb.append("Q=>");
		parsePairs(sb, exp.getQ());
		sb.append(",");
		
		sb.append("Qbar=>");
		parsePairs(sb, exp.getQbar());
		sb.append(",");
		
		sb.append("QminusQbar=>"+exp.isQeqQminusQbar());
		sb.append(",");
		
		
//		sb.append("fileOut=>");
//		sb.append(computeFileName(exp.getFileOut(), absoluteParentPath));
//		sb.append(',');
		
		sb.deleteCharAt(sb.length()-1);
		sb.append("})");
		return sb.toString();
	}
	
	private static String parseOnTheFlyParams(ParametersOnTheFly parameters, String commandName, String absoluteParentPath, String absolutePath) {
		StringBuilder sb = new StringBuilder(commandName);
		sb.append("({");
		sb.append("Q=>");
		parsePairs(sb, parameters.getQ());
		sb.append(",");
		
		sb.append("Qbar=>");
		parsePairs(sb, parameters.getQbar());
		sb.append(",");
		
		sb.append("QminusQbar=>"+parameters.isQeqQminusQbar());
		sb.append(",");
		
		sb.append("avoidUnbalancedPairs=>"+parameters.isAvoidUnbalancedPairs());
		sb.append(",");
		
		sb.append("upTo=>");
		String upTo=parameters.getUpTo();
		if(upTo==null||upTo.length()==0) {
			sb.append("NO");
		}
		else {
			sb.append(upTo);
		}
		sb.append(",");
		
		boolean hasToReduce=false;
		if(parameters.getFileRed()!=null){
			hasToReduce=true;
			sb.append("reducedFile=>");
			String p = computeFileName(parameters.getFileRed().getFileOut(),absoluteParentPath);
			sb.append(p);
			sb.append(',');
		}
		if(!hasToReduce){
			sb.append("computeOnlyPartition=>true,");
		}
		
		if(parameters.getCsvFile()!=null){
			sb.append("csvFile=>");
			String fileName = computeFileName(parameters.getCsvFile().getCsv(),absoluteParentPath);
			String p = fileName;
			sb.append(p);
			sb.append(',');
		}
		
		
//		sb.append("fileOut=>");
//		sb.append(computeFileName(exp.getFileOut(), absoluteParentPath));
//		sb.append(',');
		
		sb.deleteCharAt(sb.length()-1);
		sb.append("})");
		return sb.toString();
	}
	
	private static String parseOnTheFlyBR(onTheFlyBR exp, String commandName, String absoluteParentPath, String absolutePath) {
		return parseOnTheFlyParams(exp.getParameters(), commandName, absoluteParentPath, absolutePath);
//		StringBuilder sb = new StringBuilder(commandName);
//		sb.append("({");
//		sb.append("Q=>");
//		parsePairs(sb, exp.getQ());
//		sb.append(",");
//		
//		sb.append("Qbar=>");
//		parsePairs(sb, exp.getQbar());
//		sb.append(",");
//		
//		sb.append("QminusQbar=>"+exp.isQeqQminusQbar());
//		sb.append(",");
//		
//		sb.append("avoidUnbalancedPairs=>"+exp.isAvoidUnbalancedPairs());
//		sb.append(",");
//		
//		sb.append("upTo=>");
//		String upTo=exp.getUpTo();
//		if(upTo==null||upTo.length()==0) {
//			sb.append("NO");
//		}
//		else {
//			sb.append(upTo);
//		}
//		sb.append(",");
//		
//		
////		sb.append("fileOut=>");
////		sb.append(computeFileName(exp.getFileOut(), absoluteParentPath));
////		sb.append(',');
//		
//		sb.deleteCharAt(sb.length()-1);
//		sb.append("})");
//		return sb.toString();
	}
	
	
	
	private static String parseOnTheFlyFR(onTheFlyFR exp, String commandName, String absoluteParentPath, String absolutePath) {
		return parseOnTheFlyParams(exp.getParameters(), commandName, absoluteParentPath, absolutePath);
		
//		StringBuilder sb = new StringBuilder(commandName);
//		sb.append("({");
//		sb.append("Q=>");
//		parsePairs(sb, exp.getQ());
//		sb.append(",");
//		
//		sb.append("Qbar=>");
//		parsePairs(sb, exp.getQbar());
//		sb.append(",");
//		
//		sb.append("QminusQbar=>"+exp.isQeqQminusQbar());
//		sb.append(",");
//		
//		sb.append("avoidUnbalancedPairs=>"+exp.isAvoidUnbalancedPairs());
//		sb.append(",");
//		
//		sb.append("upTo=>");
//		String upTo=exp.getUpTo();
//		if(upTo==null||upTo.length()==0) {
//			sb.append("NO");
//		}
//		else {
//			sb.append(upTo);
//		}
//		sb.append(",");
//		
//		
////		sb.append("fileOut=>");
////		sb.append(computeFileName(exp.getFileOut(), absoluteParentPath));
////		sb.append(',');
//		
//		sb.deleteCharAt(sb.length()-1);
//		sb.append("})");
//		return sb.toString();
	}

	public static void parsePairs(StringBuilder sb, pairsOrMeta pOrM) {
		setOfPairs pairs = pOrM.getPairs();
		String metaParams=pOrM.getMetaPairs();
		sb.append('[');
		if(pairs!=null) {
			for(pairOfSpecies p : pairs.getPairs()) {
				sb.append('(');
				sb.append((p.getFirst().getSpecies()==null)?p.getFirst().getId():p.getFirst().getSpecies().getName());
				sb.append(':');
				sb.append((p.getSecond().getSpecies()==null)?p.getSecond().getId():p.getSecond().getSpecies().getName());
				sb.append(')');
				sb.append(';');
			}
			sb.deleteCharAt(sb.length()-1);
		}
		else if(metaParams!=null) {
			sb.append(metaParams);
		}
		else {
			crossProduct crossProd = pOrM.getCrossProduct();
			setOfSpeciesOrID first = crossProd.getFirst();
			setOfSpeciesOrID second=crossProd.getSecond();
			for(RefToSpeciesOrID f : first.getSpecies()) {
				for(RefToSpeciesOrID s : second.getSpecies()) {
					String fName = (f.getSpecies()==null)?""+f.getId():f.getSpecies().getName();
					String sName = (s.getSpecies()==null)?""+s.getId():s.getSpecies().getName();
					//if(!(crossProd.isAvoidReflexivePairs()&&f.getName().equals(s.getName()))) {
					if(!(crossProd.isAvoidReflexivePairs()&&fName.equals(sName))) {
						sb.append('(');
						sb.append(fName);
						sb.append(':');
						sb.append(sName);
						sb.append(')');
						sb.append(';');
					}
				}
					
			}
		}
		sb.append(']');
		//
	}
	
	private static String parseExport(Export exp, String commandName, String absoluteParentPath, String absolutePath) {
		double tEnd = 100;
		StringBuilder sb = new StringBuilder(commandName);
		sb.append("({");
		sb.append("fileOut=>");
		sb.append(computeFileName(exp.getParams().getFileOut(), absoluteParentPath));
		sb.append(',');
		/*String fileIn = exp.getParams().getFileIn();
		if(fileIn!=null){
			sb.append("fileIn=>");
			sb.append(fileIn);
			sb.append(',');
		}*/
		if(exp instanceof write){
			String format = ((write) exp).getFormat();
			if(format!=null){
				sb.append("format=>");
				sb.append(format);
				sb.append(',');
			}
			paramsToCurry parsToCurry = (((write) exp).getParamsToCurry());
			parseParamsToCurry(parsToCurry, sb);
			
			boolean deterministicCorrection = (((write) exp).isDeterministicCorrection());
			if(deterministicCorrection) {
				sb.append("deterministicCorrection=>true,");
			}
			
			boolean originalNames = (((write) exp).isOriginalNames());
			if(originalNames) {
				sb.append("originalNames=>true,");
			}
		}
		else if(exp instanceof exportModelica){
			boolean exportICOfAlgebraic  = (((exportModelica) exp).isExportICOfAlgebraic());
			sb.append("exportICOfAlgebraic=>");
			sb.append(String.valueOf(exportICOfAlgebraic));
			sb.append(',');
		}
		else if(exp instanceof generateLogs){
			generateLogs gl = (generateLogs) exp;
			gl.getSimulations();
			gl.getSots();
			sb.append("sots=>"+gl.getSots());
			sb.append(',');
			sb.append("simulations=>"+gl.getSimulations());
			sb.append(',');
			sb.append("steps=>"+gl.getSteps());
			sb.append(',');
		}
		else if(exp instanceof exportBNG){
			paramsToCurry parsToCurry = (((exportBNG) exp).getParamsToCurry());
			parseParamsToCurry(parsToCurry, sb);
		}
		else if(exp instanceof exportCRN){
			sb.append("format=>");
			sb.append("CRN");
			sb.append(',');
		}
		else if(exp instanceof exportRndPerturbedRN) {
			int from=((exportRndPerturbedRN) exp).getFromMaxPerturb();
			int to=((exportRndPerturbedRN) exp).getToMaxPerturb();
			int step=((exportRndPerturbedRN) exp).getStepMaxPerturb();
			
			sb.append("from=>"+from+",");
			sb.append("to=>"+to+",");
			sb.append("step=>"+step+",");
		}
		else if(exp instanceof computeDifferentialHull){
			//',' 'strict' '=' strict = BooleanValue (',' 'delta' '=' delta = POSITIVEINTORREAL)?
			computeDifferentialHull hullify = (computeDifferentialHull) exp;
			sb.append("strict=>"+hullify.isStrict());
			sb.append(',');
			if(hullify.getDelta()!=0.0){
				sb.append("delta=>"+hullify.getDelta());
				sb.append(',');
			}
		}
		else if(exp instanceof exportMatlab){
			tEnd = ((exportMatlab) exp).getTEnd();
			sb.append("tEnd=>"+tEnd);
			sb.append(',');
			
			String odeFunc = ((exportMatlab) exp).getOdeFunc();
			if(odeFunc==null) {
				odeFunc="ode45";
			}
			sb.append("odeFunc=>"+odeFunc);
			sb.append(',');
			
			boolean writeJacobian = ((exportMatlab) exp).isWriteJacobian();
			if(writeJacobian){
				sb.append("writeJacobian=>true");
			}
			else{
				sb.append("writeJacobian=>false");
			}
			sb.append(',');
		}
		else if(exp instanceof exportCAGEScript){
			int bound=((exportCAGEScript) exp).getCompBound();
			boolean print=((exportCAGEScript) exp).isPrint();
			sb.append("bound=>"+bound);
			sb.append(',');
			boolean jaco=((exportCAGEScript) exp).isSymbolicJacobian();
			sb.append("symbolicJacobian=>"+jaco);
			sb.append(',');
			sb.append("print=>"+print);
			sb.append(',');
			String unionFileName = ((exportCAGEScript) exp).getUnionModel();
			if(unionFileName!=null){
				sb.append("unionFileName=>");
				sb.append(computeFileName(unionFileName, absoluteParentPath));
				sb.append(',');
			}
			sb.append("sourceFileName=>"+absolutePath);
			sb.append(',');
		}
		else if(exp instanceof decompress){
			EList<SpeciesWithNumber>  limits = ((decompress) exp).getLimits().getLimitOfSpecies();
			handleLimits(sb, limits);
		}
		/*
		else if(exp instanceof generateCME){
			Limits l = ((generateCME) exp).getLimits();
			if(l!=null) {
				EList<SpeciesWithNumber>  limits = ((generateCME) exp).getLimits().getLimitOfSpecies();
				if(limits!=null) {
					handleLimits(sb, limits);
				}
			}
		}
		*/
		else if(exp instanceof utopic){
			tEnd = ((utopic) exp).getTEnd();
			sb.append("tEnd=>"+tEnd);
			sb.append(',');
			
			parseOptionalParametersUtopic(sb,((utopic) exp).getOptionalParams());

			parseParametersToPerturb(sb, ((utopic) exp).getParamsToPerturb().getParamsToPerturb());
			
			double delta=((utopic) exp).getDelta();
			if(delta!=0){
				sb.append("delta=>"+delta);
				sb.append(',');
			}
			else{
				sb.append("delta=>-1");
				sb.append(',');
			}
		
		
			double step=((utopic) exp).getStep();
			if(step>0){
				sb.append("step=>"+step);
				sb.append(',');
			}
			
			
//			boolean maximize=((utopic) exp).isMaximize();
//			sb.append("maximize=>"+maximize);
//			sb.append(',');
			
			EList<SpeciesAndExpression> coefficients = ((utopic) exp).getCoefficients().getCoefficients();
			if(coefficients!=null && coefficients.size()>0){
				sb.append("coefficients=>");
				int i=0;
				for(SpeciesAndExpression coeff : coefficients){
					sb.append(coeff.getSpecies().getName());
					sb.append(":");
					sb.append(visitExpr(coeff.getCoeff()));
					if(i<coefficients.size()-1){
						sb.append(';');
					}
					i++;
				}
				sb.append(',');
			}
		}
		else if(exp instanceof utopicOLD){
			tEnd = ((utopicOLD) exp).getTEnd();
			sb.append("tEnd=>"+tEnd);
			sb.append(',');
			
			parseOptionalParametersUtopicOLD(sb,((utopicOLD) exp).getOptionalParams());

			parseParametersToPerturb(sb, ((utopicOLD) exp).getParamsToPerturb().getParamsToPerturb());
			EList<SpeciesAndExpression> coefficients = ((utopicOLD) exp).getCoefficients().getCoefficients();
			if(coefficients!=null && coefficients.size()>0){
				sb.append("coefficients=>");
				int i=0;
				for(SpeciesAndExpression coeff : coefficients){
					sb.append(coeff.getSpecies().getName());
					sb.append(":");
					sb.append(visitExpr(coeff.getCoeff()));
					if(i<coefficients.size()-1){
						sb.append(';');
					}
					i++;
				}
				sb.append(',');
			}
		}
		else if(exp instanceof exportPontryaginPolygonMethod){
			tEnd = ((exportPontryaginPolygonMethod) exp).getTEnd();
			sb.append("tEnd=>"+tEnd);
			sb.append(',');
			EList<ParameterWithBound> paramsToPerturb = ((exportPontryaginPolygonMethod) exp).getParamsToPerturb().getParamsToPerturb();
			if(paramsToPerturb!=null && paramsToPerturb.size()>0){
				StringBuffer parsb = new StringBuffer();
				for (ParameterWithBound parameter : paramsToPerturb) {
					parsb.append(parameter.getParam().getName());
					parsb.append("[");
					parsb.append(visitExpr(parameter.getLow()));
					parsb.append(":");
					parsb.append(visitExpr(parameter.getHigh()));
					parsb.append("]");
					parsb.append(';');
				}
				parsb.deleteCharAt(parsb.length()-1);
				sb.append("paramsToPerturb=>"+parsb.toString());
				sb.append(',');
			}
			sb.append("firstSpecies=>");
			sb.append(((exportPontryaginPolygonMethod) exp).getFirstSpecies().getName());
			sb.append(',');
			sb.append("secondSpecies=>");
			sb.append(((exportPontryaginPolygonMethod) exp).getSecondSpecies().getName());
			sb.append(',');
			if(((exportPontryaginPolygonMethod)exp).isCora()){
				sb.append("cora=>true");
				sb.append(',');
				String coraLibrary = ((exportPontryaginPolygonMethod)exp).getCoraLibrary();
				if(!(coraLibrary==null || coraLibrary.equals(""))){
					sb.append("coraLibrary=>"+coraLibrary);
					sb.append(',');
				}
			}
			else{
				sb.append("cora=>false");
				sb.append(',');
			}
			//sb.append(b)
			Expression sl1 = ((exportPontryaginPolygonMethod) exp).getSl1();
			if(sl1!=null){
				sb.append("slope1=>"+visitExpr(sl1));
				sb.append(',');
			}
			Expression sl2 = ((exportPontryaginPolygonMethod) exp).getSl2();
			if(sl2!=null ){
				sb.append("slope2=>"+visitExpr(sl2));
				sb.append(',');
			}
		}
		else if(exp instanceof exportC2E2){
			/*double delta = ((exportC2E2) exp).getDelta();
			sb.append("delta=>"+delta);
			sb.append(',');
			double taylorOrder = ((exportC2E2) exp).getTaylorOrder();
			sb.append("taylorOrder=>"+taylorOrder);
			sb.append(',');*/
			double kvalue = ((exportC2E2) exp).getKvalue();
			sb.append("kvalue=>"+kvalue);
			sb.append(',');
			tEnd = ((exportC2E2) exp).getTEnd();
			sb.append("tEnd=>"+tEnd);
			sb.append(',');
			double timeStep = ((exportC2E2) exp).getTimeStep();
			sb.append("tStep=>"+timeStep);
			sb.append(',');
			/*
			String metaParams = (((exportC2E2) exp).getMetaParamsToPerturb());
			if(metaParams!=null){
				sb.append("paramsToPerturb=>"+metaParams);
				sb.append(',');
			}
			else{
				EList<Parameter> paramsToPerturb = ((exportC2E2) exp).getParamsToPerturb();
				parseParameters(paramsToPerturb, sb);
			}*/
			
			parseParametersToPerturb(sb, ((exportC2E2) exp).getParamsToPerturb().getParamsToPerturb());
			
			EList<SpeciesCompExpression> unsafeSet = ((exportC2E2) exp).getUnsafeSet().getConjuncts();
			parseUnsafeSet(sb,unsafeSet);
			
			/*double defaultIC = ((exportC2E2) exp).getDefaultIC();
			sb.append("defaultIC=>"+String.valueOf(defaultIC));
			sb.append(',');*/
		}
		else if(exp instanceof exportScriptEpsCLump){
			sb.append("epsilon=>");
			sb.append(MyParserUtil.visitExpr(((exportScriptEpsCLump) exp).getEpsilon()));
			sb.append(',');
			
			Integer perturb = ((exportScriptEpsCLump) exp).getMaxPerturb();
			if(perturb!=null) {
				sb.append("maxPerturb=>");
				sb.append(perturb);
				sb.append(',');
			}
			
//			sb.append("M0=>");
//			 EList<ListOfCoefficientsSpNat> M0rows = ((exportScriptEpsCLump) exp).getM0Rows();
//			for(ListOfCoefficientsSpNat row : M0rows) {
//				 EList<SpeciesOrNatAndExpression> coefficients = row.getCoefficients();
//				if(coefficients!=null && coefficients.size()>0){
//					//sb.append("[");
//					int i=0;
//					for(SpeciesOrNatAndExpression coeff : coefficients){
//						if(coeff.getSpecies()!=null) {
//							sb.append(coeff.getSpecies().getName());
//						}
//						else {
//							sb.append(coeff.getId());
//						}
//						sb.append(":");
//						sb.append(visitExpr(coeff.getCoeff()));
//						if(i<coefficients.size()-1){
//							sb.append(';');
//						}
//						i++;
//					}
//					//sb.append("]");
//					sb.append("_");
//				}
//			}
			
			if(((exportScriptEpsCLump) exp).getM0Rows()!=null && ((exportScriptEpsCLump) exp).getM0Rows().size()>0) {
				parseM0(((exportScriptEpsCLump) exp).getM0Rows(), sb);
			}
			else if(((exportScriptEpsCLump) exp).getPrep()!=null){
				sb.append("prePartition=>");
				sb.append(((exportScriptEpsCLump) exp).getPrep());
			}
			else {
				sb.append("M0view=>");
				sb.append(((exportScriptEpsCLump) exp).getM0View());
			}
			
			sb.append(',');
			
			
			sb.append("writeMainScript=>");
			sb.append(((exportScriptEpsCLump) exp).isWriteMainScript());
			sb.append(',');
			
		}
		else if(exp instanceof exportScriptsEpsCLump){
			sb.append("fromMaxPerturb=>");
			sb.append(((exportScriptsEpsCLump) exp).getFromMaxPerturb());
			sb.append(',');
			sb.append("toMaxPerturb=>");
			sb.append(((exportScriptsEpsCLump) exp).getToMaxPerturb());
			sb.append(',');
			sb.append("stepMaxPerturb=>");
			sb.append(((exportScriptsEpsCLump) exp).getStepMaxPerturb());
			sb.append(',');
			
			Expression e=((exportScriptsEpsCLump) exp).getFromEps();
			if(e==null) {
				e = ((exportScriptsEpsCLump) exp).getFromSlope();
				if(e==null) {
					sb.append("cLump=>");
					sb.append(((exportScriptsEpsCLump) exp).getClump());
					sb.append(',');
				}
				else {
					sb.append("fromSlope=>");
					sb.append(MyParserUtil.visitExpr(((exportScriptsEpsCLump) exp).getFromSlope()));
					sb.append(',');
					sb.append("toSlope=>");
					sb.append(MyParserUtil.visitExpr(((exportScriptsEpsCLump) exp).getToSlope()));
					sb.append(',');
					sb.append("stepSlope=>");
					sb.append(MyParserUtil.visitExpr(((exportScriptsEpsCLump) exp).getStepSlope()));
					sb.append(',');
				}
			}
			else {
				sb.append("fromEps=>");
				sb.append(MyParserUtil.visitExpr(((exportScriptsEpsCLump) exp).getFromEps()));
				sb.append(',');
				sb.append("toEps=>");
				sb.append(MyParserUtil.visitExpr(((exportScriptsEpsCLump) exp).getToEps()));
				sb.append(',');
				sb.append("stepEps=>");
				sb.append(MyParserUtil.visitExpr(((exportScriptsEpsCLump) exp).getStepEps()));
				sb.append(',');
			}
			
			sb.append("tEnd=>");
			sb.append(((exportScriptsEpsCLump) exp).getTEnd());
			sb.append(',');
			
			if(((exportScriptsEpsCLump) exp).getM0Rows()!=null && ((exportScriptsEpsCLump) exp).getM0Rows().size()>0) {
				parseM0(((exportScriptsEpsCLump) exp).getM0Rows(), sb);
			}
			
			else if(((exportScriptsEpsCLump) exp).getPrep()!=null){
				sb.append("prePartition=>");
				sb.append(((exportScriptsEpsCLump) exp).getPrep());
			}
			else {
				sb.append("M0view=>");
				sb.append(((exportScriptsEpsCLump) exp).getM0View());
			}
			sb.append(',');
			
			CSVFile par = ((exportScriptsEpsCLump) exp).getCsvFile();
			sb.append("csvFile=>" + computeFileName(String.valueOf(((CSVFile) par).getCsv()),absoluteParentPath));
			sb.append(',');
			
			sb.append("writeInnerScript=>");
			sb.append(((exportScriptsEpsCLump) exp).isWriteInnerScript());
			sb.append(',');
		}
		else if(exp instanceof exportEpsilonBoundsScript){
			/*TreeIterator<EObject> contents = exp.eAllContents();
			while(contents.hasNext()){
				EObject content = contents.next();
			}*/
			tEnd = ((exportEpsilonBoundsScript) exp).getTEnd();
			sb.append("tEnd=>"+tEnd);
			sb.append(',');
			
			sb.append("deltat=>"+MyParserUtil.visitExpr(((exportEpsilonBoundsScript) exp).getDeltat()));
			sb.append(',');
			String metaParams = (((exportEpsilonBoundsScript) exp).getMetaParamsToPerturb());
			if(metaParams!=null){
				sb.append("paramsToPerturb=>"+metaParams);
				sb.append(',');
			}
			else{
				EList<Parameter> paramsToPerturb = ((exportEpsilonBoundsScript) exp).getParamsToPerturb();
				parseParameters("paramsToPerturb",paramsToPerturb, sb);
			}
			
			double defaultIC = ((exportEpsilonBoundsScript) exp).getDefaultIC();
			sb.append("defaultIC=>"+String.valueOf(defaultIC));
			sb.append(',');
			sb.append("epsilon=>");
			sb.append(MyParserUtil.visitExpr(((exportEpsilonBoundsScript) exp).getEpsilon()));
			sb.append(',');
			
			
			String p="NO";
			sb.append("prePartition=>");
			String prep=((exportEpsilonBoundsScript) exp).getPrep();
			if(prep!=null){
				p = setPrep(p, prep);
			}
			sb.append(p);
			sb.append(',');
			
			sb.append("reduction=>");
			String red=((exportEpsilonBoundsScript) exp).getRed();
			//String red="both";
			if(red!=null){
				if(red.equals("forward")){
					red="forward";
				}
				else if(red.equals("backward")){
					red="backward";
				}
				else if(red.equals("backward")){
					red="backward";
				}
				else if(red.equals("NO")){
					red="NO";
				}
			}
			else{
				red="both";
			}
			sb.append(red);
			sb.append(',');
			
		}
		if(exp instanceof exportZ3){
			exportZ3 export = (exportZ3) exp;
			String question = (export.getQuestion().equalsIgnoreCase("FDE"))? "OFL":"EFL";
			sb.append("question=>"+question);
			sb.append(',');
			sb.append("partition=>USER");
			sb.append(',');
		}
		sb.deleteCharAt(sb.length()-1);
		sb.append("})");
		return sb.toString();
	}

	private static void parseM0(EList<ListOfCoefficientsSpNat> M0rows, StringBuilder sb) {
		sb.append("M0=>");
		 //EList<ListOfCoefficientsSpNat> M0rows = ((exportScriptsEpsCLump) exp).getM0Rows();
		for(ListOfCoefficientsSpNat row : M0rows) {
			 EList<SpeciesOrNatAndExpression> coefficients = row.getCoefficients();
			if(coefficients!=null && coefficients.size()>0){
				//sb.append("[");
				int i=0;
				for(SpeciesOrNatAndExpression coeff : coefficients){
					if(coeff.getSpecies()!=null) {
						sb.append(coeff.getSpecies().getName());
					}
					else {
						sb.append(coeff.getId());
					}
					sb.append(":");
					sb.append(visitExpr(coeff.getCoeff()));
					if(i<coefficients.size()-1){
						sb.append(';');
					}
					i++;
				}
				//sb.append("]");
				sb.append("---");
			}
		}
	}

	private static void handleLimits(StringBuilder sb, EList<SpeciesWithNumber> limits) {
		if(limits!=null && limits.size()>0){
			sb.append("limits=>");
			int i=0;
			for(SpeciesWithNumber lim : limits){
				sb.append(lim.getSpecies().getName());
				sb.append(":");
				sb.append(""+lim.getNumber());
				if(i<limits.size()-1){
					sb.append(';');
				}
				i++;
			}
			sb.append(',');
		}
	}

	private static void parseParamsToCurry(paramsToCurry parToCurry, StringBuilder sb) {
		if(parToCurry!=null) {
			String metaParams =parToCurry.getMetaParamsToCurry();
			if(metaParams!=null) {
				sb.append("paramsToCurry=>"+metaParams);
				sb.append(',');
			}
			else{
				EList<Parameter> paramsToPerturb = parToCurry.getParamsToCurry();
				parseParameters("paramsToCurry",paramsToPerturb, sb);
			}
		}
	}

	private static void parseOptionalParametersUtopic(StringBuilder sb,
			EList<OptionalParametersUtopic> optionalParams) {
		sb.append("plot=>"+true);
		sb.append(',');
		sb.append("exitMatlab=>"+false);
		sb.append(',');
		for (OptionalParametersUtopic optionalParamUtopic : optionalParams) {
			if(optionalParamUtopic instanceof utopicKMax){
				int kMax=((utopicKMax) optionalParamUtopic).getKMax();
				if(kMax!=0){
					sb.append("kMax=>"+kMax);
					sb.append(',');
				}
			}
			/*else if(optionalParamUtopic instanceof utopicPlot){
				boolean plot=((utopicPlot) optionalParamUtopic).isPlot();
				sb.append("plot=>"+plot);
				sb.append(',');
			}
			else if(optionalParamUtopic instanceof exitMatlab){
				boolean exitMatlab=((exitMatlab) optionalParamUtopic).isExitMatlab();
				sb.append("exitMatlab=>"+exitMatlab);
				sb.append(',');
			}*/
			/*else if(optionalParamUtopic instanceof utopicDelta){
				double delta=((utopicDelta) optionalParamUtopic).getDelta();
				if(delta!=0){
					sb.append("delta=>"+delta);
					sb.append(',');
				}
				else{
					sb.append("delta=>-1");
					sb.append(',');
				}
			}
			else if(optionalParamUtopic instanceof utopicStep){
				double step=((utopicStep) optionalParamUtopic).getStep();
				if(step>0){
					sb.append("step=>"+step);
					sb.append(',');
				}
			}
			*/
			else if(optionalParamUtopic instanceof utopicIntegrationStep){
				double integrationStep=((utopicIntegrationStep) optionalParamUtopic).getIntegrationStep();
				if(integrationStep>0){
					sb.append("integrationStep=>"+integrationStep);
					sb.append(',');
				}
			}
			else if(optionalParamUtopic instanceof utopicRunMatlab){
				//utopicRunMatlab
				boolean runMatlab=((utopicRunMatlab) optionalParamUtopic).isRunMatlab();
				sb.append("runMatlab=>"+runMatlab);
				sb.append(',');
			}
			else if(optionalParamUtopic instanceof utopicRunAndCompileVNODELP){
				//utopicRunAndCompileVNODELP
				boolean runVNODE=((utopicRunAndCompileVNODELP) optionalParamUtopic).isRunVNODE();
				sb.append("compileAndRunVNODE=>"+runVNODE);
				sb.append(',');
			}
		}
		
	}
	
	private static void parseOptionalParametersUtopicOLD(StringBuilder sb,
			EList<OptionalParametersUtopicOld> optionalParams) {
		for (OptionalParametersUtopicOld optionalParamUtopic : optionalParams) {
			if(optionalParamUtopic instanceof utopicKMax){
				int kMax=((utopicKMax) optionalParamUtopic).getKMax();
				if(kMax!=0){
					sb.append("kMax=>"+kMax);
					sb.append(',');
				}
			}
			else if(optionalParamUtopic instanceof utopicEpsilon){
				double epsilon=((utopicEpsilon) optionalParamUtopic).getEpsilon();
				if(epsilon!=0){
					sb.append("epsilon=>"+epsilon);
					sb.append(',');
				}
			}
			else if(optionalParamUtopic instanceof utopicMaxStep){
				double maxStep=((utopicMaxStep) optionalParamUtopic).getMaxStep();
				if(maxStep>0){
					sb.append("maxStep=>"+maxStep);
					sb.append(',');
				}
			}
			else if(optionalParamUtopic instanceof utopicSymbolicJacobian){
				boolean symbolicJacobian=((utopicSymbolicJacobian) optionalParamUtopic).isSymbolicJacobian();
				sb.append("symbolicJacobian=>"+symbolicJacobian);
				sb.append(',');
			}
		}
		
	}

	private static void parseUnsafeSet(StringBuilder sb, EList<SpeciesCompExpression> unsafeSet) {
			if(unsafeSet!=null && unsafeSet.size()>0){
				sb.append("unsafeSet=>");
				for (SpeciesCompExpression sce : unsafeSet) {
					sb.append(sce.getSpecies().getName());
					sb.append(';');
					sb.append(sce.getComp());
					sb.append(';');
					sb.append(visitExpr(sce.getExpr()));
					sb.append(';');
				}
				sb.deleteCharAt(sb.length()-1);
				sb.append(',');
			}
		}

	public static void parseParameters(String label,EList<Parameter> paramsToPerturb, StringBuilder sb) {
		if(paramsToPerturb!=null && paramsToPerturb.size()>0){
			StringBuffer parsb = new StringBuffer();
			for (Parameter parameter : paramsToPerturb) {
				parsb.append(parameter.getName());
				parsb.append(';');
			}
			parsb.deleteCharAt(parsb.length()-1);
			//sb.append("paramsToPerturb=>"+parsb.toString());
			sb.append(label+"=>"+parsb.toString());
			sb.append(',');
		}
	}

	public static void parseParametersToPerturb(StringBuilder sb, EList<ParameterWithBound> paramsToPerturb) {
		if(paramsToPerturb!=null && paramsToPerturb.size()>0){
			StringBuffer parsb = new StringBuffer();
			for (ParameterWithBound parameter : paramsToPerturb) {
				parsb.append(parameter.getParam().getName());
				parsb.append("[");
				parsb.append(visitExpr(parameter.getLow()));
				parsb.append(":");
				parsb.append(visitExpr(parameter.getHigh()));
				parsb.append("]");
				parsb.append(';');
			}
			parsb.deleteCharAt(parsb.length()-1);
			sb.append("paramsToPerturb=>"+parsb.toString());
			sb.append(',');
		}
	}
	
	private static String parseApproximation(Approximation command, String commandName, String absoluteParentPath,
			boolean b) {
		ParamsApprox params = command.getParams();
		StringBuilder sb = new StringBuilder(commandName);
		sb.append("({");
		sb.append("matlabScript=>");
		sb.append(computeFileName(params.getMatalbScript(), absoluteParentPath));
		sb.append(',');

		sb.append("epsilon=>");
		sb.append(MyParserUtil.visitExpr(params.getEpsilon()));
		sb.append(',');

		String metaParams = params.getMetaParamsToPerturb();
		if(metaParams!=null){
			sb.append("paramsToPerturb=>"+metaParams);
			sb.append(',');
		}
		else{
			EList<Parameter> paramsToPerturb = params.getParamsToPerturb();

			if(paramsToPerturb!=null && paramsToPerturb.size()>0){
				StringBuffer parsb = new StringBuffer();
				for (Parameter parameter : paramsToPerturb) {
					parsb.append(parameter.getName());
					parsb.append(';');
				}
				parsb.deleteCharAt(parsb.length()-1);
				sb.append("paramsToPerturb=>"+parsb.toString());
				sb.append(',');
			}
		}

		String p="NO";
		sb.append("prePartition=>");
		if(params.getPrep()!=null&&params.getPrep().getPrep()!=null){
			String prep=params.getPrep().getPrep();
			if(prep!=null){
				p = setPrep(p, prep);
			}
		}
		sb.append(p);
		sb.append(',');


		sb.deleteCharAt(sb.length()-1);
		sb.append("})");
		return sb.toString();

	}

	public static String getOnlyAlphaNumericAndDot(String s) {
	    Pattern pattern = Pattern.compile("[^0-9 a-z A-Z _ .]");
	    Matcher matcher = pattern.matcher(s.replace('-', '_'));
	    String number = matcher.replaceAll("");
	    return number;
	 }
	
	public static String computeFileName(String fileName, String absoluteParentPath) {
		return computeFileName(fileName, absoluteParentPath,false);
	}
	
	public static String computeFileName(String fileName, String absoluteParentPath,boolean computeOnlyAlphaNumeric) {
		File f = new File(fileName);
		if(f.isAbsolute()){
			if(computeOnlyAlphaNumeric){
				int lastSep = fileName.lastIndexOf(File.separator);
				String path = fileName.substring(0,lastSep+1);
				String relativeName = fileName.substring(lastSep);
				fileName=path+getOnlyAlphaNumericAndDot(relativeName);
			}
		}
		else{
			if(fileName.startsWith("./")||fileName.startsWith(".\\")){
				fileName=fileName.substring(2);
			}
			/*while(fileName.startsWith(".."+File.separator)){
				fileName=fileName.substring(3);
				int lastSep = absoluteParentPath.lastIndexOf(File.separator);
				if(lastSep>0){
					absoluteParentPath=absoluteParentPath.substring(0, lastSep);
				}
			}*/
			while(fileName.startsWith("../")){
				fileName=fileName.substring(3);
				int lastSep = absoluteParentPath.lastIndexOf(File.separator);
				if(lastSep>0){
					absoluteParentPath=absoluteParentPath.substring(0, lastSep);
				}
			}
			while(fileName.startsWith("..\\")){
				fileName=fileName.substring(3);
				int lastSep = absoluteParentPath.lastIndexOf(File.separator);
				if(lastSep>0){
					absoluteParentPath=absoluteParentPath.substring(0, lastSep);
				}
			}
			if(computeOnlyAlphaNumeric){
				fileName = getOnlyAlphaNumericAndDot(fileName);
			}
			
			fileName=absoluteParentPath+File.separator+fileName;
		}
		
		return fileName;
		
		
		/*
		 		if(!fileName.startsWith(File.separator)){
			if(fileName.startsWith("."+File.separator)){
				fileName=fileName.substring(2);
			}
			while(fileName.startsWith(".."+File.separator)){
				fileName=fileName.substring(3);
				int lastSep = absoluteParentPath.lastIndexOf(File.separator);
				if(lastSep>0){
					absoluteParentPath=absoluteParentPath.substring(0, lastSep);
				}
			}
			if(computeOnlyAlphaNumeric){
				fileName = getOnlyAlphaNumericAndDot(fileName);
			}
			
			fileName=absoluteParentPath+File.separator+fileName;
		}
		else{
			int lastSep = fileName.lastIndexOf(File.separator);
			String path = fileName.substring(0,lastSep+1);
			String relativeName = fileName.substring(lastSep);
			fileName=path+getOnlyAlphaNumericAndDot(relativeName);
		}
		return fileName;
		 */
	}


	private static String parseOptionalParametersSimulateCommon(OptionalParametersSimulateCommon par, String absoluteParentPath) {
		String p = null;
		/*if(par instanceof FileIn){
			p="fileIn=>" + ((FileIn) par).getFileIn();
		}
		else*/ 
		if(par instanceof Steps){
			p = "steps=>" + String.valueOf(((Steps) par).getSteps());
		}
		else if(par instanceof StepSize){
			p = "stepSize=>" + String.valueOf(((StepSize) par).getStepSize());
		}
		else if(par instanceof VisualizePlot){
			//p = "visualizePlot=>" + String.valueOf(((VisualizePlot) par).isPlot());
			p = "visualizePlot=>" + ((VisualizePlot) par).getPlot();
		}
		else if(par instanceof ShowLabels){
			p = "showLabels=>" + String.valueOf(((ShowLabels) par).isPlotLabels());
		}
		else if(par instanceof CSVFile){
			p = "csvFile=>" + computeFileName(String.valueOf(((CSVFile) par).getCsv()),absoluteParentPath);
		}
		else if(par instanceof DefaultIC){
			p = "defaultIC=>" + String.valueOf(((DefaultIC) par).getDefaultIC());
		}
		return p;
	}


	private static String parseAnalysis(Analysis analysisCommand, String commandName,String absoluteParentPath) {
		StringBuilder sb = new StringBuilder(commandName);
		sb.append("({");
		if(analysisCommand instanceof simulateCTMC){
			//toBeFilled.add(" simulateCTMC({fileIn=>inputfileName,tEnd=>200,steps=>10,method=>ssa,repeats=>100,visualizePlot=>false,csvFile=>fileNameWhereToSaveCSVValues})
			simulateCTMC simCTMC = (simulateCTMC)analysisCommand;
			sb.append("tEnd=>");
			sb.append(simCTMC.getTEnd());
			sb.append(',');
			String p = null;
			for (ParametersSimulateCTMC par : simCTMC.getParams()) {
				if(par instanceof OptionalParametersSimulateCommon){
					p = parseOptionalParametersSimulateCommon((OptionalParametersSimulateCommon)par,absoluteParentPath);
				}
				else if(par instanceof Method){
					sb.append("method=>");
					p = ((Method) par).getMethod();
				}
				else if(par instanceof Repeats){
					sb.append("repeats=>");
					p = String.valueOf(((Repeats) par).getRepeats());
				} 
				/*else if(par instanceof StepSize){
					sb.append("stepSize=>");
					p = String.valueOf(((StepSize) par).getStepSize());
				}*/
				sb.append(p);
				sb.append(',');
			}	
		}
		else if(analysisCommand instanceof simulateODE){
			simulateODE simODE = (simulateODE)analysisCommand;
			sb.append("tEnd=>");
			sb.append(simODE.getTEnd());
			sb.append(',');
			String p = null;
			for (ParametersSimulateODE par : simODE.getParams()) {
				if(par instanceof OptionalParametersSimulateCommon){
					p = parseOptionalParametersSimulateCommon((OptionalParametersSimulateCommon)par,absoluteParentPath);
				}
				//simulateODE({fileIn=>inputfileName,tEnd=>200,steps=>10,minStep=>1.0e-8,maxStep=>100.0,absTol=>1.0e-10,relTol=>1.0e-10,visualizePlot=>false,csvFile=>fileNameWhereToSaveCSVValues,covariances=>true});
				else if(par instanceof MinStep){
					sb.append("minStep=>");
					p = String.valueOf(((MinStep) par).getMinStep());
				}
				else if(par instanceof MaxStep){
					sb.append("maxStep=>");
					p = String.valueOf(((MaxStep) par).getMaxStep());
				}
				else if(par instanceof ComputeJacobian){
					sb.append("computeJacobian=>");
					p = String.valueOf(((ComputeJacobian) par).isJacobian());
				}
				else if(par instanceof AbsTol){
					sb.append("absTol=>");
					p = String.valueOf(((AbsTol) par).getAbsTol());
				}
				else if(par instanceof RelsTol){
					sb.append("relTol=>");
					p = String.valueOf(((RelsTol) par).getRelTol());
				}
				else if(par instanceof SolverLibrary){
					sb.append("library=>");
					p = String.valueOf(((SolverLibrary) par).getLibrary());
				}
				else if(par instanceof SimulationCampaign) {
					SimulationCampaign sc = (SimulationCampaign)par;
					sb.append("campaign_n=>"+sc.getN()+",");
					sb.append("campaign_IC=>"+sc.getIc()+",");
					sb.append("campaign_Params=>");
					p=""+sc.getParams();
				}
				
				/*else if(par instanceof Cov){
					sb.append("covariances=>");
					p = String.valueOf(((Cov) par).isCov());
				}*/ 
				sb.append(p);
				sb.append(',');
			}
		}
		else if(analysisCommand instanceof simulateDAE){
			simulateDAE simDAE = (simulateDAE)analysisCommand;
			sb.append("tEnd=>");
			sb.append(simDAE.getTEnd());
			sb.append(',');
			String p = null;
			for (ParametersSimulateODE par : simDAE.getParams()) {
				if(par instanceof OptionalParametersSimulateCommon){
					p = parseOptionalParametersSimulateCommon((OptionalParametersSimulateCommon)par,absoluteParentPath);
				}
				//simulateODE({fileIn=>inputfileName,tEnd=>200,steps=>10,minStep=>1.0e-8,maxStep=>100.0,absTol=>1.0e-10,relTol=>1.0e-10,visualizePlot=>false,csvFile=>fileNameWhereToSaveCSVValues,covariances=>true});
				/*
				else if(par instanceof MinStep){
					sb.append("minStep=>");
					p = String.valueOf(((MinStep) par).getMinStep());
				}
				else if(par instanceof MaxStep){
					sb.append("maxStep=>");
					p = String.valueOf(((MaxStep) par).getMaxStep());
				}
				*/
				/*else if(par instanceof ComputeJacobian){
					sb.append("computeJacobian=>");
					p = String.valueOf(((ComputeJacobian) par).isJacobian());
				}*/
				else if(par instanceof AbsTol){
					sb.append("absTol=>");
					p = String.valueOf(((AbsTol) par).getAbsTol());
				}
				else if(par instanceof RelsTol){
					sb.append("relTol=>");
					p = String.valueOf(((RelsTol) par).getRelTol());
				}
				/*
				else if(par instanceof SolverLibrary){
					sb.append("library=>");
					p = String.valueOf(((SolverLibrary) par).getLibrary());
				}
				*/
				/*else if(par instanceof Cov){
					sb.append("covariances=>");
					p = String.valueOf(((Cov) par).isCov());
				}*/ 
				sb.append(p);
				sb.append(',');
			}
		}
		//BEGIN MultiVeStA
		else if(analysisCommand instanceof multivestaSMC){
			//multivestaSMC({fileIn=>inputfileName,alpha=>0.05,delta=>0.2,maxTime=>200,query=>query.quatex,steps=>10,method=>ssa,parallelism=>2,visualizePlot=>false,csvFile=>fileNameWhereToSaveCSVValues})
			multivestaSMC multivestaCommand= (multivestaSMC)analysisCommand;
			sb.append("alpha=>");
			sb.append(String.valueOf(multivestaCommand.getAlpha()));
			sb.append(',');
			sb.append("delta=>");
			sb.append(String.valueOf(multivestaCommand.getDelta()));
			sb.append(',');
			String p = null;
			for (ParametersMultiVeStA par : multivestaCommand.getParams()) {
				if(par instanceof OptionalParametersSimulateCommon){
					p = parseOptionalParametersSimulateCommon((OptionalParametersSimulateCommon)par,absoluteParentPath);
				}
				else if(par instanceof FileOfModel){
					p="model=>" + computeFileName(((FileOfModel) par).getModel(),absoluteParentPath);
				}
				else if(par instanceof MaxTime){
					sb.append("maxTime=>");
					p = String.valueOf(((MaxTime) par).getMaxTime());
				}
				else if(par instanceof QueryFile){
					sb.append("query=>");
					p = computeFileName(String.valueOf(((QueryFile) par).getQuery()),absoluteParentPath);
				}
				else if(par instanceof Method){
					sb.append("method=>");
					p = ((Method) par).getMethod();
				}
				else if(par instanceof Parallelism){
					sb.append("parallelism=>");
					p = String.valueOf(((Parallelism) par).getParallelism());
				}
				sb.append(p);
				sb.append(',');
			}
		}
		//END MultiVeStA
		sb.deleteCharAt(sb.length()-1);
		sb.append("})");
		//System.out.println(sb.toString());
		return sb.toString();
	}

	private static String parseBooleanReduce(BooleanReduction red, String commandName, String absoluteParentPath,boolean isUpdate) {
		StringBuilder sb = new StringBuilder(commandName);
		boolean hasToReduce=isUpdate;
		sb.append("({");
		EList<OptionalParametersBooleanReductions> optinalPars=null;
		if(red.getOptionalParametersFormatted()!=null){
			optinalPars = red.getOptionalParametersFormatted().getOptionalParameters();
			if(optinalPars!=null){
				for (OptionalParametersBooleanReductions par : optinalPars) {
					String p = null;	

					if(par instanceof FileRed){
						hasToReduce=true;
						sb.append("reducedFile=>");
						p = computeFileName(((FileRed) par).getFileOut(),absoluteParentPath);
					}
					else if(par instanceof Prep){
						sb.append("prePartition=>");

						String prep = ((Prep) par).getPrep();
						p = "NO";
						p = setPrep(p, prep);
					}
					else if(par instanceof FilePart){
						sb.append("fileWhereToStorePartition=>");
						p = computeFileName(((FilePart) par).getFilePartition(),absoluteParentPath);
					}
					else if(par instanceof CSVFile){
						sb.append("csvFile=>");
						String fileName = computeFileName(((CSVFile) par).getCsv(),absoluteParentPath);
						p = fileName;
					}
					else if(par instanceof Simplify) {
						sb.append("simplify=>");
						p = ""+ ((Simplify) par).isSimplify();
					}
					sb.append(p);
					sb.append(',');
				}
			}
		}
		if(!hasToReduce){
			sb.append("computeOnlyPartition=>true,");
		}
		
		if(red instanceof reduceFBE) {
			String aggrFunc=((reduceFBE) red).getAggregationFunction();
			sb.append("aggregationFunction=>");
			sb.append(aggrFunc);
			sb.append(',');
			
//			boolean simplify=((reduceFBE) red).isSimplify();
//			sb.append("simplify=>"+simplify);
//			sb.append(',');
		}
		else if(red instanceof reduceFME) {
			String aggrFunc=((reduceFME) red).getAggregationFunction();
			sb.append("aggregationFunction=>");
			sb.append(aggrFunc);
			sb.append(',');
			
//			boolean simplify=((reduceFME) red).isSimplify();
//			sb.append("simplify=>"+simplify);
//			sb.append(',');
		}
		
		if(sb.charAt(sb.length()-1)==','){
			sb.deleteCharAt(sb.length()-1);
		}
		sb.append("})");
		//System.out.println(sb.toString());
		return sb.toString();
	}
	
	private static String parseReduce(Reduction red, String commandName, String absoluteParentPath,boolean isUpdate) {
		boolean hasToReduce=isUpdate;
		StringBuilder sb = new StringBuilder(commandName);
		sb.append("({");
		//EList<OptionalParametersReductionsWithComma> a = red.getOptionalParametersFormatted().getOptionalParameters();
		EList<OptionalParametersReductions> optinalPars=null;
		if(red.getOptionalParametersFormatted()!=null){
			optinalPars = red.getOptionalParametersFormatted().getOptionalParameters();
			/*sb.append("groupedFile=>"+computeFileName("z3Question.smt2",projectPath)+",");
		sb.append("typeOfGroupedFile=>z3,");*/
			if(optinalPars!=null){
				for (OptionalParametersReductions par : optinalPars) {
					//fileIn=>inputfileName.crn,prePartition=>NONE,reducedFile=>outputFileNameOfReducedCRN.crn,
					//groupedFile=>outputFileNameOfGroupedCRN.crn,sameICFile=>outputFileNameOfCRNWithSameICPerBlock.crn,
					//computeOnlyPartition=>false,fileWhereToStorePartition=>outputFileToStorePartition.txt,print=>true,print=>true
					String p = null;
					if(par instanceof FileRed){
						hasToReduce=true;
						sb.append("reducedFile=>");
						p = computeFileName(((FileRed) par).getFileOut(),absoluteParentPath);
					}
					else if(par instanceof Prep){
						sb.append("prePartition=>");

						String prep = ((Prep) par).getPrep();
						p = "NO";
						p = setPrep(p, prep);
						/*
				 boolean toPrep = ((Prep) par).isPrep();
				 if(toPrep){
					if(red instanceof reduceBB || red instanceof reduceBDE || red instanceof reduceNBB){
						p = "IC";
					}
					else{
						p = "VIEWS";
					}
				}*/
					}
					else if(par instanceof ReductionAlgorithm){
						hasToReduce=true;
						sb.append("reductionAlgorithm=>");
						String prep = ((ReductionAlgorithm) par).getRedAlgorithm();
						p = prep;
					}
					else if(par instanceof AddSelfLoops){
						//hasToReduce=true;
						sb.append("addSelfLoops=>");
						String selfLoops = String.valueOf((((AddSelfLoops) par).isSelfLoops()));
						p = selfLoops;
					}
					else if(par instanceof OneLabelAtATime){
						//hasToReduce=true;
						sb.append("oneLabelAtATime=>");
						String selfLoops = String.valueOf((((OneLabelAtATime) par).isOneLabel()));
						p = selfLoops;
					}
					else if(par instanceof CSVFile){
						sb.append("csvFile=>");
						String fileName = computeFileName(((CSVFile) par).getCsv(),absoluteParentPath);
						p = fileName;
					}
					else if(par instanceof SMTTimesCSVFile){
						sb.append("fileWhereToCSVSMTTimes=>");
						String fileName = computeFileName(((SMTTimesCSVFile) par).getCsv(),absoluteParentPath);
						p = fileName;
					}
					/*else if(par instanceof FileIn){
				sb.append("fileIn=>");
				p = ((FileIn) par).getFileIn();
			}*/
					//					else if(par instanceof FileGrouped){
					//						sb.append("groupedFile=>");
					//						p = computeFileName(((FileGrouped) par).getFileGrouped(),absoluteParentPath);
					//					} 
					/*					else if(par instanceof FilePart){
						sb.append("fileWhereToStorePartition=>");
						p = computeFileName(((FilePart) par).getFilePartition(), absoluteParentPath);
					}*/
					//					else if(par instanceof FileSameIC){
					//						sb.append("sameICFile=>");
					//						p = computeFileName(((FileSameIC) par).getFileSameIC(), absoluteParentPath);
					//					}
					//					else if(par instanceof OnlyPart){
					//						sb.append("computeOnlyPartition=>");
					//						p= ((OnlyPart) par).isOnlyPartition() ? "true" : "false";
					//					}
					//					else if(par instanceof PrintInfo){
					//						sb.append("print=>");
					//						p= ((PrintInfo) par).isPrint() ? "true" : "false";
					//					}
					sb.append(p);
					sb.append(',');
				}
			}
		}
		//		sb.append("computeOnlyPartition=>true");
		//		if(optinalPars!=null && optinalPars.size()>0){
		//			sb.append(",");
		//		}
		if(!hasToReduce){
			sb.append("computeOnlyPartition=>true,");
		}
		if(red instanceof reduceSMB){
			if(((reduceSMB) red).isHalveRatesOfHomeoReactions()) {
				sb.append("halveRatesOfHomeoReactions=>true,");
			}
		}
		if(red instanceof reduceCoRNFE) {
			sb.append("computeOnlyPartition=>true,");
			CoRNParams cornParams = ((reduceCoRNFE)red).getCornParams();
			if(cornParams.getPercentagePert()>0) {
				sb.append("percentagePerturbation=>"+cornParams.getPercentagePert());
			}
			else if(cornParams.getAbsolutePert()>0){
				sb.append("absolutePerturbation=>"+cornParams.getAbsolutePert());
			}
			else if(cornParams.getPercentageClos()>0) {
				sb.append("percentageClosure=>"+cornParams.getPercentageClos());
			}
			else if(cornParams.getAbsoluteClos()>0){
				sb.append("absoluteClosure=>"+cornParams.getAbsoluteClos());
			}
			else if(cornParams.getLowerFactor()>0) {
				sb.append("lowerFactor=>"+cornParams.getLowerFactor());
				sb.append(",");
				sb.append("upperFactor=>"+cornParams.getUpperFactor());
			}
			sb.append(",");
			sb.append("certainConstants=>"+cornParams.isCertainConstants());
			sb.append(",");
		}
		if(red instanceof reduceUCTMCFE){
			String modelWithBigM = ((reduceUCTMCFE)red).getModelWithBigM();
			if(modelWithBigM!=null){
				sb.append("modelWithBigM=>");
				sb.append(computeFileName(modelWithBigM, absoluteParentPath,false));
				sb.append(',');
			}
			else {
				String delta = visitExpr(((reduceUCTMCFE) red).getDelta()); 
				sb.append("delta=>"+delta+",");
			}
		}
		/*if(red instanceof MassActionEpsilonReduction){
			sb.append("epsilon=>");
			sb.append(MyParserUtil.visitExpr(((MassActionEpsilonReduction) red).getEpsilon()));
		}*/
		/*else if(optinalPars!=null && optinalPars.size()>0){
				sb.deleteCharAt(sb.length()-1);
		}*/
		if(sb.charAt(sb.length()-1)==','){
			sb.deleteCharAt(sb.length()-1);
		}
		sb.append("})");
		//System.out.println(sb.toString());
		return sb.toString();
	}

	private static String setPrep(String p, String prep) {
		if(prep.equals("USER")){
			p="USER";
		}
		else if(prep.equals("IC")){
			p="IC";
		} 
		else if(prep.equals("USER_and_IC")){
			p="USER_and_IC";
		}
		else if(prep.equals("Outputs")) {
			p="Outputs";
		}
		else if(prep.equals("Outputs_singleton")) {
			p="Outputs_singleton";
		}
		return p;
	}



}