package it.imt.erode.importing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.booleannetwork.implementations.BooleanNetwork;
import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;
import it.imt.erode.booleannetwork.updatefunctions.BooleanUpdateFunctionExpr;
import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.NotBooleanUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.ReferenceToNodeUpdateFunction;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.crn.implementations.Species;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.crn.symbolic.constraints.BooleanConnector;
import it.imt.erode.expression.parser.ICRNReactionAndBoolean;
import it.imt.erode.expression.parser.IMonomial;
import it.imt.erode.expression.parser.Monomial;
import it.imt.erode.expression.parser.NumberMonomial;
import it.imt.erode.expression.parser.ParameterMonomial;
import it.imt.erode.expression.parser.ProductMonomial;
import it.imt.erode.expression.parser.SpeciesMonomial;
import it.imt.erode.importing.cnf.PowerSet;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;


/**
 * 
 * @author Andrea Vandin
 * This class is used to import SAT problems in CNF format. (extension: cnf).
 * A SAT problem is a boolean expression over a N variables. We create a BN with N+1 variables with variables and update functions xi=xi for i in 1...N, and an addition variable y = the sat formula
 * A CNF file has this format:
p cnf 50  218 
 40 -9 17 0
-30 -45 35 0
...
We have 50 variables and 218 clauses. The formula will be clause1 and clause2 and ... clause2018
Clause 1 is (x40 or (not x9) or x17)
Clause 2 is ((not x30) or (not x45) or x35)
'0' is a separator of clauses. In principle, a clause could be given  in more lines, and more clauses could be given in the same line.
We allow for more clauses in the same line, but we assume that a clause is not divided in more lines    
Comment lines start with c  
 */
public class CNFImporter extends AbstractImporter{

	private IBooleanNetwork bn;
	//private ICRN crn;
	public CNFImporter(String fileName,MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower) {
		super(fileName,out,bwOut,msgDialogShower);
	}
	
	
	/**
	 * Reads a CNF formula.
	 * Creates a BN with y= the formula, and a variable per variable in the formula, with var = var 		
	 * @param print
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws UnsupportedFormatException
	 */
	public ArrayList<String> readCNF(boolean print) throws FileNotFoundException, IOException, UnsupportedFormatException{

		if(print){
			CRNReducerCommandLine.println(out,bwOut,"Reading CNF file"+getFileName());
		}
		
		//initCRNAndMath();
		initInfoImporting();
		//getInfoImporting().setLoadedCRN(true);
		//getInfoImporting().setLoadedCRN(false);
		

		ArrayList<String> comments = new ArrayList<>();
		long begin = System.currentTimeMillis();
		
		//
		
		CNFClauses cnfClausesOBJ = readCnfClauses(comments);
		ArrayList<ArrayList<IUpdateFunction>> cnfClauses=cnfClausesOBJ.getCnfClauses();
		int nvars=cnfClausesOBJ.getnVars();
		
		
		IUpdateFunction formula = makeCNFFormula(cnfClauses);

		bn = new BooleanNetwork(GUICRNImporter.getModelName(getFileName()), out, bwOut, false);
		IPartition initPart=new Partition(nvars+1);
		setInitialPartition(initPart);

		ISpecies y = new Species("y", 0, BigDecimal.ZERO, "0");
		bn.addSpecies(y);
		bn.addUpdateFunction("y", formula);
		IBlock yblockPart=new Block();
		initPart.add(yblockPart);
		yblockPart.addSpecies(y);

		IBlock xblockPart=new Block();
		initPart.add(xblockPart);
		for(int id=1;id<=nvars;id++) {
			String name="x"+id;
			ISpecies xi = new Species(name, id, BigDecimal.ZERO, "0");
			bn.addSpecies(xi);
			bn.addUpdateFunction(name, new ReferenceToNodeUpdateFunction(name));
			xblockPart.addSpecies(xi);
		}

		ArrayList<HashSet<ISpecies>> userPart = new ArrayList<>(1);
		HashSet<ISpecies> yBlock=new HashSet<>(1);
		yBlock.add(y);
		userPart.add(yBlock);
		bn.setUserDefinedPartition(userPart);

		getInfoImporting().setLoadedCRNFormat(ODEorNET.RN);
		getInfoImporting().setLoadedCRN(true);
		getInfoImporting().setReadSpecies(nvars+1);
		getInfoImporting().setReadCRNReactions(nvars+1);
		long end=System.currentTimeMillis();
		getInfoImporting().setRequiredMS(end -begin);

		if(print) {
			CRNReducerCommandLine.println(out,bwOut,getInfoImporting().toStringShort());
		}

		return comments;
	}
	
	private CNFClauses readCnfClauses(ArrayList<String> comments) throws NumberFormatException, IOException, UnsupportedFormatException {

		//initCRNAndMath();
		//initInfoImporting();
		//getInfoImporting().setLoadedCRN(true);
		//getInfoImporting().setLoadedCRN(false);

		BufferedReader br = getBufferedReader();
		String line;
		
		CNFClauses cnfClauses=null;
		 //ArrayList<ArrayList<IUpdateFunction>> cnfClauses=null;
		HashMap<String, IUpdateFunction> spToUpd=null;
		HashMap<String, IUpdateFunction> spToNegatedUpd=null;
		
		boolean keepComments=false;
		int nvars=-1;
		int nclauses=-1;
		boolean preambleFound=false;
		//We first search for the preamble line
		while ((line = br.readLine()) != null && !preambleFound) {
			line=line.trim();
			if(line.equals("c")||line.startsWith("c ")) {
				if(keepComments)
					comments.add(line);
			}
			else {
				//p cnf 50  218 
				if(line.startsWith("p ")) {
					String[] tokens = line.split("\\s+");
					//tokens[0] is p
					if(!tokens[1].equalsIgnoreCase("cnf")) {
						throw new UnsupportedFormatException("The problem appears to be non-CNF: "+line);
					}
					nvars= Integer.valueOf(tokens[2]);
					nclauses= Integer.valueOf(tokens[3]);
					//cnfClauses=new ArrayList<>(nclauses);
					cnfClauses=new CNFClauses(nvars, nclauses);
					preambleFound=true;
					spToUpd = new LinkedHashMap<String, IUpdateFunction>(nvars);
					spToNegatedUpd = new LinkedHashMap<String, IUpdateFunction>(nvars);
					break;
				}
			}
		}

		if(!preambleFound) {
			throw new UnsupportedFormatException("I could not find the preamble line");
		}
		if(cnfClauses.getnVars()<=0 || cnfClauses.getnClauses() <= 0) {
			throw new UnsupportedFormatException("There must be positive numbers of vars ("+
					cnfClauses.getnVars()+") and clauses ("+cnfClauses.getnClauses()+").");
		}
		
		CRNReducerCommandLine.println(out,bwOut," Reading the "+nclauses+" clauses (on "+nvars+" variables)...");
		CRNReducerCommandLine.print(out,bwOut,"\t");

		//IUpdateFunction formula = null;
		//boolean keepComments=false;
		int c=1;
		while ((line = br.readLine()) != null) {
			line=line.trim();
			//System.out.println(line);
			//Any other line is ignored (including comments)
			if(line.equals("")||line.equals("%")){
				//bw.write("\n");
			}
			else if(line.startsWith("c")){
				if(keepComments)
					comments.add(line);
				//					bw.write("//");
				//					bw.write(line);
				//					bw.write("\n");
			} 
			else {
				// 40 -9 17 0
				//-30 -45 35 0
				if(line.equals("0")) {
					//Do nothing
				}
				else {
					String[] clauses = line.replace("\t"," ").split(" 0");
					//String[] clauses = line.split("\\s+");
					for(String clauseStr : clauses) {
						if(!clauseStr.equals("0")) {
							if(c%2000==0) {
								CRNReducerCommandLine.print(out,bwOut,c+ " ");
								if(c%20000==0) {
									CRNReducerCommandLine.print(out,bwOut,"\n\t");
								}
							}
							ArrayList<IUpdateFunction> currentClause=new ArrayList<>();
							cnfClauses.add(currentClause);
							//-30 -45 35
							String[] leaves = clauseStr.split("\\s+");
							if(leaves.length>17) {
								throw new UnsupportedFormatException("I found a clause that is too large: ("+leaves.length+") "+ clauseStr.toString());
							}
							for(String leafStr : leaves) {
								IUpdateFunction leaf;
								String spName=null;
								if(leafStr.startsWith("-")) {
									leafStr=leafStr.substring(1);
									spName="x"+leafStr;
									leaf = getOrAddUpdateFunction(spName, true, spToUpd,spToNegatedUpd);
									//leaf= new NotBooleanUpdateFunction(new ReferenceToNodeUpdateFunction(spName)); 
								}
								else {
									spName="x"+leafStr;
									//leaf= new ReferenceToNodeUpdateFunction(spName);
									leaf = getOrAddUpdateFunction(spName, false, spToUpd,spToNegatedUpd);
								}
								currentClause.add(leaf);
							}
							c++;
						}
					}
				}
			}
		}
		CRNReducerCommandLine.println(out,bwOut,"");
		return cnfClauses;
	}
	
	public static IUpdateFunction getOrAddUpdateFunction(String spName, boolean negated, HashMap<String, IUpdateFunction> spToUpd, HashMap<String, IUpdateFunction> spToNegatedUpd) {
		IUpdateFunction ret;
		if(negated) {
			ret = spToNegatedUpd.get(spName);
			if(ret==null) {
				IUpdateFunction innerRet = getOrAddUpdateFunction(spName, false, spToUpd,spToNegatedUpd);
				ret = new NotBooleanUpdateFunction(innerRet);
				spToNegatedUpd.put(spName, ret);
			}
		}
		else {
			ret = spToUpd.get(spName);
			if(ret==null) {
				ret = new ReferenceToNodeUpdateFunction(spName);
				spToUpd.put(spName, ret);
			}
		}
		return ret;
	}

	private IUpdateFunction makeCNFFormula(ArrayList<ArrayList<IUpdateFunction>> cnfClauses) {
		//Each entry of the outer array is a list of  variables or not variables appearing in a CNF clause. 
		//The whole out array represents the cnf clauses in AND
		IUpdateFunction formula=null;
		for(ArrayList<IUpdateFunction> currentClause:cnfClauses) {
			IUpdateFunction clause = null;
			for(IUpdateFunction leaf : currentClause) {
				if(clause==null) {
					clause=leaf;
				}
				else {
					clause=new BooleanUpdateFunctionExpr(clause, leaf, BooleanConnector.OR);
				}
			}
			if(formula==null) {
				formula=clause;
			}
			else {
				formula=new BooleanUpdateFunctionExpr(formula,clause, BooleanConnector.AND);
			}
			
		}
		
		return formula;
	}
	
	

	/*
	public ArrayList<String> readCNF(boolean print) throws FileNotFoundException, IOException, UnsupportedFormatException{

		if(print){
			CRNReducerCommandLine.println(out,bwOut,"Reading CNF file"+getFileName());
		}

		initInfoImporting();
		getInfoImporting().setLoadedCRN(true);
		getInfoImporting().setLoadedCRN(false);

		BufferedReader br = getBufferedReader();
		String line; 

		long begin = System.currentTimeMillis();

		ArrayList<String> comments = new ArrayList<>();

		int nvars=-1;
		int nclauses=-1;
		boolean preambleFound=false;
		//We first search for the preamble line
		while ((line = br.readLine()) != null && !preambleFound) {
			line=line.trim();
			if(line.startsWith("c ")) {
				comments.add(line);
			}
			else {
				//p cnf 50  218 
				if(line.startsWith("p ")) {
					String[] tokens = line.split("\\s+");
					//tokens[0] is p
					if(!tokens[1].equalsIgnoreCase("cnf")) {
						throw new UnsupportedFormatException("The problem appears to be non-CNF: "+line);
					}
					nvars= Integer.valueOf(tokens[2]);
					nclauses= Integer.valueOf(tokens[3]);
					preambleFound=true;
					break;
				}
			}
		}

		if(!preambleFound) {
			throw new UnsupportedFormatException("I could not find the preamble line");
		}
		if(nvars<=0 || nclauses <= 0) {
			throw new UnsupportedFormatException("There must be positive numbers of vars ("+nvars+") and clauses ("+nclauses+").");
		}

		IUpdateFunction formula = null;

		while ((line = br.readLine()) != null) {
			line=line.trim();
			//System.out.println(line);
			//Any other line is ignored (including comments)
			if(line.equals("")||line.equals("%")){
				//bw.write("\n");
			}
			else if(line.startsWith("c")){
				comments.add(line);
				//					bw.write("//");
				//					bw.write(line);
				//					bw.write("\n");
			} 
			else {
				// 40 -9 17 0
				//-30 -45 35 0
				if(line.equals("0")) {
					//Do nothing
				}
				else {
					String[] clauses = line.split(" 0");
					for(String clauseStr : clauses) {
						if(!clauseStr.equals("0")) {
							//-30 -45 35
							String[] leaves = clauseStr.split("\\s+");
							IUpdateFunction clause = null;
							for(String leafStr : leaves) {
								IUpdateFunction leaf;
								if(leafStr.startsWith("-")) {
									leafStr=leafStr.substring(1);
									leaf= new NotBooleanUpdateFunction(new ReferenceToNodeUpdateFunction("x"+leafStr)); 
								}
								else {
									leaf= new ReferenceToNodeUpdateFunction("x"+leafStr);
								}

								if(clause==null) {
									clause=leaf;
								}
								else {
									clause=new BooleanUpdateFunctionExpr(clause, leaf, BooleanConnector.OR);
								}
							}

							if(formula==null) {
								formula=clause;
							}
							else {
								formula=new BooleanUpdateFunctionExpr(formula,clause, BooleanConnector.AND);
							}
						}
					}
				}
			}
		}

		bn = new BooleanNetwork(GUICRNImporter.getModelName(getFileName()), out, bwOut, false);
		IPartition initPart=new Partition(nvars+1);
		setInitialPartition(initPart);

		ISpecies y = new Species("y", 0, BigDecimal.ZERO, "0");
		bn.addSpecies(y);
		bn.addUpdateFunction("y", formula);
		IBlock yblockPart=new Block();
		initPart.add(yblockPart);
		yblockPart.addSpecies(y);

		IBlock xblockPart=new Block();
		initPart.add(xblockPart);
		for(int id=1;id<=nvars;id++) {
			String name="x"+id;
			ISpecies xi = new Species(name, id, BigDecimal.ZERO, "0");
			bn.addSpecies(xi);
			bn.addUpdateFunction(name, new ReferenceToNodeUpdateFunction(name));
			xblockPart.addSpecies(xi);
		}

		ArrayList<HashSet<ISpecies>> userPart = new ArrayList<>(1);
		HashSet<ISpecies> yBlock=new HashSet<>(1);
		yBlock.add(y);
		userPart.add(yBlock);
		bn.setUserDefinedPartition(userPart);

		getInfoImporting().setLoadedCRNFormat(ODEorNET.RN);
		getInfoImporting().setLoadedCRN(true);
		getInfoImporting().setReadSpecies(nvars+1);
		getInfoImporting().setReadCRNReactions(nvars+1);
		long end=System.currentTimeMillis();
		getInfoImporting().setRequiredMS(end -begin);

		if(print) {
			CRNReducerCommandLine.println(out,bwOut,getInfoImporting().toStringShort());
		}

		return comments;
	}
	*/

	
	public void readCNFandPolynomiaze(boolean print) throws FileNotFoundException, IOException, UnsupportedFormatException{

		if(print){
			CRNReducerCommandLine.println(out,bwOut,"Reading CNF file"+getFileName());
			CRNReducerCommandLine.println(out,bwOut," to polinomiaze it");
		}
		
		initCRNAndMath();
		initInfoImporting();
		getInfoImporting().setLoadedCRN(true);

		String eta="eta";
		BigDecimal etaVal=BigDecimal.ONE;
		getCRN().addParameter(eta, etaVal.toPlainString());
		
		ArrayList<String> comments = new ArrayList<>();
		long begin = System.currentTimeMillis();
		
		//
		CNFClauses cnfClausesOBJ = readCnfClauses(comments);
		ArrayList<ArrayList<IUpdateFunction>> cnfClauses=cnfClausesOBJ.getCnfClauses();
		int nvars=cnfClausesOBJ.getnVars();
		
		LinkedHashMap<String, ISpecies> nameToSpecies=new LinkedHashMap<>(nvars);
		CRNReducerCommandLine.println(out,bwOut," Creating the species...");
		for(int id=1;id<=nvars;id++) {
			String name="x"+id;
			ISpecies sp= addSpecies(name,null, "0");
			nameToSpecies.put(name,sp);
		}
		CRNReducerCommandLine.print(out,bwOut," Completed\n");
		HashMap<String, NumberMonomial> bdToMon=new LinkedHashMap<String, NumberMonomial>();
		HashMap<ISpecies, SpeciesMonomial> spToMon=new LinkedHashMap<ISpecies, SpeciesMonomial>(getCRN().getSpeciesSize());
		HashMap<ISpecies, ProductMonomial> spToNegMon=new LinkedHashMap<ISpecies, ProductMonomial>(getCRN().getSpeciesSize());
		
		BigDecimal one = BigDecimal.ONE;
		BigDecimal two = one.add(one);
		
		ArrayList<ArrayList<IMonomial>> polynomialOfClauses=new ArrayList<>(cnfClauses.size());
		int c=1;
		CRNReducerCommandLine.print(out,bwOut," Transforming clauses in polynomials...\n\t");
		for(ArrayList<IUpdateFunction> currentClause : cnfClauses) {
//			if(c==113729) {
//				CRNReducerCommandLine.print(out,bwOut,"ciao ");
//			}
			if(c%2000==0) {
			//if(true) {
				CRNReducerCommandLine.print(out,bwOut,c+" ");
				//if(c%100==0) {
				if(c%20000==0) {
					CRNReducerCommandLine.print(out,bwOut,"\n\t");
				}
			}
			//TODO: I assume that n is the number of terms in OR (not the set of different variables in it. E.g. for 'x1 or x1 or not x1' I get n=3 
			int n=currentClause.size();
			
			BigDecimal denum = two.pow(n-1);
			BigDecimal an=one.divide(denum);
			NumberMonomial anMon=Monomial.getAndAddIfNecessary(an, bdToMon);//new NumberMonomial(an, an.toPlainString());
			//BigDecimal bn=an.subtract(one);
			
			ArrayList<IMonomial> monomialsOfClause = makePolynomial(anMon,/*bn,*/currentClause,nameToSpecies,spToMon,spToNegMon);
			polynomialOfClauses.add(monomialsOfClause);
			c++;
		}
		CRNReducerCommandLine.print(out,bwOut,"\n Completed\n");

		cnfClausesOBJ=null;
		cnfClauses=null;
		
		ParameterMonomial etaMon = new ParameterMonomial(etaVal, eta);
		IMonomial minusEtaMon = new ProductMonomial(Monomial.minusOneMon, etaMon);
		boolean needsI=false;
		
		
		CRNReducerCommandLine.print(out,bwOut," Computing species derivatives iterating over (the monomials of) each clause...\n\t");
		LinkedHashMap<ISpecies, ArrayList<IMonomial>> speciesToDerivative=new LinkedHashMap<>(getCRN().getSpeciesSize());
		//Slow: for all sp, for all monomials
		/*for(ISpecies sp : getCRN().getSpecies()) {
			ArrayList<IMonomial> derivativeOfSp=new ArrayList<>();
			speciesToDerivative.put(sp, derivativeOfSp);
			for(ArrayList<IMonomial> monomialsOfClause : polynomialOfClauses) {
				for(IMonomial monomial : monomialsOfClause) {
					IMonomial derivedMon=Monomial.deriveInSpecies(monomial,sp, bdToMon, spToMon);
					if(derivedMon==Monomial.zeroMonomial) {
						//do nothing
					}
					else {
						derivedMon= makeProduct(minusEtaMon, derivedMon);
						derivativeOfSp.add(derivedMon);
						
						if((!needsI)&&derivedMon.needsI()) {
							needsI=true;
						}
					}
					
				}
			}
		}*/
		
		//Faster: for all monomials I consider only the species appearing in there (in all other cases the derivative is zero
		c=1;
		for(ArrayList<IMonomial> monomialsOfClause : polynomialOfClauses) {
			if(c%2000==0) {
				//if(true) {
				CRNReducerCommandLine.print(out,bwOut,c+" ");
				//if(c%100==0) {
				if(c%20000==0) {
					CRNReducerCommandLine.print(out,bwOut,"\n\t");
				}
			}
			for(IMonomial monomial : monomialsOfClause) {
				HashMap<ISpecies, Integer> speciesInMonomial=monomial.getOrComputeSpecies();
				for(Entry<ISpecies, Integer> entry : speciesInMonomial.entrySet()) {
					ISpecies sp=entry.getKey();
					if(entry.getValue()>0) {
						ArrayList<IMonomial> derivativeOfSp=speciesToDerivative.get(sp);
						if(derivativeOfSp==null) {
							derivativeOfSp=new ArrayList<>();
							speciesToDerivative.put(sp, derivativeOfSp);
						}
						IMonomial derivedMon=Monomial.deriveInSpecies(monomial,sp, bdToMon, spToMon);
						if(derivedMon==Monomial.zeroMonomial) {
							//do nothing
						}
						else {
							derivedMon= makeProduct(minusEtaMon, derivedMon);
							derivativeOfSp.add(derivedMon);

							if((!needsI)&&derivedMon.needsI()) {
								needsI=true;
							}
						}
					}
				}
			}
			c++;
		}
		CRNReducerCommandLine.print(out,bwOut,"\n Completed\n");
		
		//x1=[((-1 * eta) * (1 * 0.5)), ((-1 * eta) * ((1 * 0.5) * x2))]
		//x2=[((-1 * eta) * (1 * 0.5)), ((-1 * eta) * ((1 * 0.5) * x1))]
		polynomialOfClauses=null;
		
		CRNReducerCommandLine.print(out,bwOut,"\n Creating a reaction per monomial appearing in the derivative of each species. Species ...\n\t");
		ISpecies I=null;
		if(needsI) {
			I= addSpecies("I", null, "1");
		}
		
		int csp=1;
		for(Entry<ISpecies, ArrayList<IMonomial>> entry : speciesToDerivative.entrySet()) {
			if(csp%1000==0) {
				CRNReducerCommandLine.print(out,bwOut,csp+" ");
				if(csp%10000==0) {
					CRNReducerCommandLine.print(out,bwOut,"\n\t");
				}
			}
			ISpecies sp=entry.getKey();
			ArrayList<IMonomial> derivativeOfSp=entry.getValue();
			for(IMonomial monomial : derivativeOfSp) {
				ICRNReactionAndBoolean reaction = monomial.toReaction(sp, I);
				addReaction(reaction.getReaction());
			}
			csp++;
		}
		
		if(needsI) {
			ArrayList<HashSet<ISpecies>> p=new ArrayList<>(1);
			HashSet<ISpecies> s = new LinkedHashSet<ISpecies>(1);
			s.add(I);
			p.add(s);
			getCRN().setUserDefinedPartition(p);
			nvars+=1;
		}
		CRNReducerCommandLine.print(out,bwOut,"\n Completed\n");
		this.createInitialPartition();
		
		
		getInfoImporting().setLoadedCRNFormat(ODEorNET.RN);
		getInfoImporting().setLoadedCRN(true);
		getInfoImporting().setReadSpecies(nvars);
		getInfoImporting().setReadParameters(getCRN().getParameters().size());
		getInfoImporting().setReadCRNReactions(getCRN().getReactions().size());
		long end=System.currentTimeMillis();
		getInfoImporting().setRequiredMS(end -begin);

		if(print) {
			CRNReducerCommandLine.println(out,bwOut,getInfoImporting().toStringShort());
		}
		
	}
	
	


	private ArrayList<IMonomial> makePolynomial(NumberMonomial anMon, /*BigDecimal bn,*/ ArrayList<IUpdateFunction> currentClause, LinkedHashMap<String, ISpecies> nameToSpecies, 
			HashMap<ISpecies,SpeciesMonomial> spToMon, HashMap<ISpecies,ProductMonomial> spToNegMon) throws UnsupportedFormatException {
		
		//String anStr=an.toPlainString();
		//String bnStr=bn.toPlainString();
		ArrayList<IMonomial> sum=new ArrayList<>(currentClause.size()+1);
		//IMonomial anMon=new NumberMonomial(an, anStr);
		
		HashSet<ISpecies> negated=new LinkedHashSet<>();
		ArrayList<ISpecies> involvedSpecies=new ArrayList<>(currentClause.size());
		for(IUpdateFunction upd:currentClause) {
			ISpecies sp=null;
			if(upd instanceof ReferenceToNodeUpdateFunction) {
				sp=nameToSpecies.get(upd.toString());
				//involvedSpecies.add(upd.toString());
			}
			else if(upd instanceof NotBooleanUpdateFunction) {
				IUpdateFunction inner=((NotBooleanUpdateFunction) upd).getInnerUpdateFunction();
				if(inner instanceof ReferenceToNodeUpdateFunction) {
					//involvedSpecies.add(inner.toString());
					sp=nameToSpecies.get(inner.toString());
					negated.add(sp);
				}
			}
			
			if(sp!=null) {
				involvedSpecies.add(sp);
			}
			else {
				throw new UnsupportedFormatException("A CNF clause must containt only species or negation of species. "+upd);
			}
		}
		
		if(involvedSpecies.size()>17) {
			throw new UnsupportedFormatException("I found a clause that is too large: "+involvedSpecies.size());
		}
		
		PowerSet<ISpecies> ps = new PowerSet<>(involvedSpecies);
		while(ps.hasMoreSets()) {
			Set<ISpecies> set = ps.getNextSet();
			if(set.isEmpty()) {
				//do nothing
			}
			else {
				IMonomial product=anMon;
				for(ISpecies sp : set) {
					IMonomial spMon;
					//IMonomial spMon =Monomial.getAndAddIfNecessary(sp, spToMon);
					//IMonomial spMon = new SpeciesMonomial(sp);
					if(negated.contains(sp)) {
						//check if works
						//spMon=new ProductMonomial(minusOneMOn, spMon);
						spMon=Monomial.getNegatedAndAddIfNecessary(sp, spToMon,spToNegMon);
					}
					else {
						//check if works
						spMon=Monomial.getAndAddIfNecessary(sp, spToMon);
					}
					product= makeProduct(product, spMon);
				}
				sum.add(product);
			}
		}
		
		//IMonomial bnMon=new NumberMonomial(bn, bnStr);
		//sum.add(bnMon);
		return sum;
	}


	public IMonomial makeProduct(IMonomial left, IMonomial right) {
		if(left instanceof NumberMonomial) {
			if (left.getOrComputeCoefficient().compareTo(BigDecimal.ZERO)==0) {
				return Monomial.zeroMonomial;
			}
			else if (left.getOrComputeCoefficient().compareTo(BigDecimal.ONE)==0) {
				return right;
			}
		}
		else if(right instanceof NumberMonomial) {
			if (right.getOrComputeCoefficient().compareTo(BigDecimal.ZERO)==0) {
				return Monomial.zeroMonomial;
			}
			else if (right.getOrComputeCoefficient().compareTo(BigDecimal.ONE)==0) {
				return left;
			}
		}
		
		return new ProductMonomial(left, right);
	}


	public IBooleanNetwork getBN() {
		return bn;
	}

}
