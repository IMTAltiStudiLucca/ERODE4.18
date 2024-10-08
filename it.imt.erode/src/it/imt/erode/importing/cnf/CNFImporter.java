package it.imt.erode.importing.cnf;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
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
import it.imt.erode.importing.AbstractImporter;
import it.imt.erode.importing.GUICRNImporter;
import it.imt.erode.importing.InfoCRNImporting;
import it.imt.erode.importing.ODEorNET;
import it.imt.erode.importing.UnsupportedFormatException;
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
		return readCnfClauses(comments,"x");
	}
	
	private CNFClauses readCnfClauses(ArrayList<String> comments,String prefixSpeciesName) throws NumberFormatException, IOException, UnsupportedFormatException {

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
			if(line.equals("c")||line.startsWith("c ")||line.equals("%")||line.startsWith("% ")) {
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
									spName=prefixSpeciesName+leafStr;
									leaf = getOrAddUpdateFunction(spName, true, spToUpd,spToNegatedUpd);
									//leaf= new NotBooleanUpdateFunction(new ReferenceToNodeUpdateFunction(spName)); 
								}
								else {
									spName=prefixSpeciesName+leafStr;
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
		//CRNReducerCommandLine.println(out,bwOut,"");
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
	
	
	public static byte[] makeZeroBinaryArray(int nBits)  {
		byte[] A = new byte[nBits];
		return A;
	}
//	public static void resetToZerBinaryArray(int[] A)  {
//		Arrays.fill(A, 0);
//	}

	public static void addOneToBinaryArray(byte[] A) throws Exception {
		//0 000
		//1 001
		//2 010
		//3 011
		//4 100
		//5 101
		//6 110
		//7 111
        for (int i = A.length - 1; i >= 0; i--) {
            if (A[i] == 0) {
                A[i] = 1;
                return;
            }
            A[i] = 0;
            if (i == 0) {
                throw new Exception("Overflow");
            }
        }
        return;
    }

	public InfoCRNImporting readCNFandMakeQuantumOptimization(boolean print,String fileOut) throws FileNotFoundException, IOException, UnsupportedFormatException{

		if(print){
			CRNReducerCommandLine.println(out,bwOut," Reading CNF file"+getFileName());
			CRNReducerCommandLine.println(out,bwOut,"  to make quantum optimization");
		}
		
		initCRNAndMath();
		initInfoImporting();
		getInfoImporting().setLoadedCRN(true);


		createParentDirectories(fileOut);
		BufferedWriter bw=null;
		boolean failed=false;
		try {
			bw = new BufferedWriter(new FileWriter(fileOut));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in CNF 2 QuantumOptimization. Exception raised while creating the filewriter for file: "+fileOut);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			//return;
			failed=true;
		}

		if(!failed) {
			String name = overwriteExtensionIfEnabled(fileOut,"",true);
			int i = name.lastIndexOf(File.separator);
			if(i!=-1) {
				name=name.substring(i+1);
			}
			String modelName=GUICRNImporter.getModelName(name); 
			bw.write("begin Probabilistic Program "+modelName+"\n");


			ArrayList<String> comments = new ArrayList<>();
			long begin = System.currentTimeMillis();

			//
			CNFClauses cnfClausesOBJ = readCnfClauses(comments,"");

			//Each internal array is a CNF clause
			ArrayList<ArrayList<IUpdateFunction>> cnfClauses=cnfClausesOBJ.getCnfClauses();
			int nvars=cnfClausesOBJ.getnVars();
			int nClauses=cnfClausesOBJ.getnClauses();
			
			getInfoImporting().setReadSpecies(nvars);
			getInfoImporting().setReadCRNReactions(-1);
			long end=System.currentTimeMillis();
			getInfoImporting().setRequiredMS(end -begin);
			
			
			if(print){
				CRNReducerCommandLine.println(out,bwOut," reading completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)"+".");
				CRNReducerCommandLine.println(out,bwOut," Making and writing the quantum SAT optimization encoding in file "+fileOut);
			}

			
			
			
			begin=System.currentTimeMillis();

			//I pre-compute the list of clauses where each variables appears with positive sign
			LinkedHashMap<Integer, ArrayList<Integer>> variableToClausesWhereItAppears=new LinkedHashMap<>(nvars);
			LinkedHashMap<Integer, ArrayList<Integer>> variableToClausesWhereItAppears_negated=new LinkedHashMap<>(nvars);
			for(int clause=0;clause<cnfClauses.size();clause++) {
				ArrayList<IUpdateFunction> cnfClause = cnfClauses.get(clause);
				for(IUpdateFunction leaf:cnfClause) {
					if(leaf instanceof ReferenceToNodeUpdateFunction) {
						addClauseToList(variableToClausesWhereItAppears, clause, leaf);
					}
					else {
						//it must be a negated
						IUpdateFunction inner = ((NotBooleanUpdateFunction)leaf).getInnerUpdateFunction();
						addClauseToList(variableToClausesWhereItAppears_negated, clause, inner);
					}
				}
			}



			double q=nvars;
			int n=(int)Math.pow(2, q);// n=2^q, 	2^8=256, 2^10=1024
			double tau=0.1;	//we must have q/tau a natural!
			double q_over_tau_double=q/tau;
			int q_over_tau=(int)q_over_tau_double;
			if(q_over_tau!=q_over_tau_double) {
				CRNReducerCommandLine.println(out,bwOut,"\nq over tau should be an int. I terminate");
				failed=true;
				return null;
			}



			double sqrtn=Math.sqrt(n);
			double oneOverSqrtn=1.0/sqrtn;

			ArrayList<Integer> iSatifsyingTheFormula=new ArrayList<Integer>();

			double lambda=nClauses;		//initialized to the number of clauses in the formula
			int[] H = new int[n+1];//H[0] won't be used
			
			if(print){
				CRNReducerCommandLine.print(out,bwOut,"\tBuilding Hii for i in [1,"+n+"] ... ");
			}
			long beginH=System.currentTimeMillis();
			
			
			byte[] iMinusOne = makeZeroBinaryArray(nvars);
			
			
			for(i=1;i<=n;i++) {
				/*
				int iMinusOne=i-1;
				String bitString=Integer.toBinaryString(iMinusOne);
				while(bitString.length()<nvars) {
					bitString="0"+bitString;
				}
				String[] bitStringArray=bitString.split("");
				*/
				
				HashSet<Integer> clausesSatisfied=new LinkedHashSet<Integer>();
				for(int b=0;b<iMinusOne.length;b++ ) {
					byte bit=iMinusOne[b];


					//100 is meant to be x3,x2,x1 rather than x1,x2,x3 (actually x2,x1,x0 rather than x0,x1,x2)
					int bToVar=(iMinusOne.length-1) - b;
					bToVar = bToVar + 1; //from 0;n-1, to 1;n

					if(bit==1) {
						ArrayList<Integer> clausesWherItAppears = variableToClausesWhereItAppears.get(bToVar);
						if(clausesWherItAppears!=null && clausesWherItAppears.size()>0) {
							clausesSatisfied.addAll(clausesWherItAppears);
						}
					}
					else  {
						//must be 0
						ArrayList<Integer> clausesWherItAppearsNegated = variableToClausesWhereItAppears_negated.get(bToVar);
						if(clausesWherItAppearsNegated!=null && clausesWherItAppearsNegated.size()>0) {
							clausesSatisfied.addAll(clausesWherItAppearsNegated);
						}
					}
				}

				//Hi,i, (here H[i] denotes the number of clauses in the formula that are satisfied by the binary representation of i−1.
				H[i]=clausesSatisfied.size();
				if(H[i]==lambda) {
					iSatifsyingTheFormula.add(i);
				}
				
				if(i<n) {
					try {
						addOneToBinaryArray(iMinusOne);
					} catch (Exception e) {
						CRNReducerCommandLine.println(out,bwOut,"\nIssues in adding 1 to the byte array: overflow. I terminate");
						failed=true;
						return null;
					}
				}
			}
			long endH=System.currentTimeMillis();
			if(print){
				CRNReducerCommandLine.println(out,bwOut," Completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((endH-beginH)/1000.0) )+ " (s)"+".");
			}

			bw.write("//////////////////////////\n");
			bw.write("//Internal parameters used\n");
			bw.write("//tau="+tau+", q="+q+"\n");
			if(n<20) {
				bw.write("//H="+Arrays.toString(H)+"\n//\tH[0] shall be ignored\n");
			}			
			bw.write("//////////////////////////\n\n");


			bw.write("begin init\n");
			String commonString="="+oneOverSqrtn+"\n  b";
			//for(String species : speciesToUpdateFunction.keySet()) {
			for(i=1;i<=n;i++) {
				bw.write("  a"+i+commonString+i+"=0\n");
			}
			bw.write("  c=1\t\t//The counter\n");
			bw.write("  c_odd=1\t//Used to represent it the counter is odd (1) or even (0)\n");
			bw.write("  r=0\t\t//The results\n");
			bw.write("end init\n\n");

			bw.write("begin partition\n");
			bw.write(" {");
			for(i=1;i<=n;i++) {
				bw.write("a"+i);
				if(i<n) {
					bw.write(",");
				}
			}
			bw.write("},\n");
			bw.write(" {c,c_odd,r}\n");
//			bw.write(" {");
//			for(i=1;i<=n;i++) {
//				bw.write("b"+i);
//				if(i<n) {
//					bw.write(",");
//				}
//			}
//			bw.write("}\n");
			bw.write("end partition\n\n");
			

			bw.write("while true do\n");
			bw.write(" begin parameters\n");
			bw.write("  p = Uniform(0,1)\n");
			bw.write(" end parameters\n");

			bw.write("\n");
			bw.write(" //////////////////////////////////////////////////\n");
			bw.write(" //First if: we still need to iterate and c is even\n");
			bw.write(" //////////////////////////////////////////////////\n");
			bw.write(" if c_odd = 0 and c - "+q+"/"+tau+" < 0 then\n");
			for(i=1;i<=n;i++) {
				bw.write("  upd(a"+i+") = a"+i+" + "+tau+"*"+H[i]+"*b"+i+"\n");
			}
			bw.write("\n");
			for(i=1;i<=n;i++) {
				bw.write("  upd(b"+i+") = b"+i+" - "+tau+"*"+H[i]+"*a"+i+"\n");
			}
			bw.write("  upd(c) = c+1\n");
			bw.write("  upd(c_odd) = 1-c_odd\n");
			bw.write("  upd(r) = 0\n");

			bw.write("\n");

			bw.write(" //////////////////////////////////////////////////\n");
			bw.write(" //Second if: we still need to iterate and c is odd\n");
			bw.write(" //////////////////////////////////////////////////\n");
			bw.write(" elif c_odd > 0  and c - "+q+"/"+tau+" < 0 then\n");
			

			StringBuffer sum_aj=new StringBuffer("");
			StringBuffer sum_bj=new StringBuffer("");
			boolean first=true;
			for(i=1;i<=n;i++) {
				if(!first) {
					sum_aj.append(" + ");
					sum_bj.append(" + ");
				}
				first=false;
				
				sum_aj.append("a");
				sum_aj.append(i);

				sum_bj.append("b");
				sum_bj.append(i);
			}
			String sum_a=sum_aj.toString();
			String sum_b=sum_bj.toString();
			for(i=1;i<=n;i++) {
				bw.write("  upd(a"+i+") = a"+i+" + "+tau+"*(");
				bw.write(sum_a);
				bw.write(")\n");
			}
			bw.write("\n");
			for(i=1;i<=n;i++) {
				bw.write("  upd(b"+i+") = b"+i+" + "+tau+"*(");
				bw.write(sum_b);
				bw.write(")\n");
			}
			bw.write("  upd(c) = c+1\n");
			bw.write("  upd(c_odd) = 1-c_odd\n");
			bw.write("  upd(r) = 0\n");


			bw.write("\n");
			bw.write(" //////////////////////////////////////////////////\n");
			bw.write(" //Last if: no more iterations necessary, we can compute r\n");
			bw.write(" //////////////////////////////////////////////////\n");
			bw.write(" //There are "+iSatifsyingTheFormula.size()+" i satisfying the formula\n");
			if(iSatifsyingTheFormula.size()<15) {
				bw.write(" //	"+iSatifsyingTheFormula+"\n");
			}
			
			bw.write(" //////////////////////////////////////////////////\n");

			if(iSatifsyingTheFormula.size()>0) {
				StringBuffer iSatisfyingFormula_a = new StringBuffer("");
				StringBuffer iSatisfyingFormula_b = new StringBuffer("");

				first=true;
				for(Integer iSat : iSatifsyingTheFormula) {
					if(!first) {
						iSatisfyingFormula_a.append(" + ");
						iSatisfyingFormula_b.append(" + ");
					}
					first=false;
					iSatisfyingFormula_a.append("a");
					iSatisfyingFormula_a.append(iSat);

					iSatisfyingFormula_b.append("b");
					iSatisfyingFormula_b.append(iSat);
				}
				String iSatisfyingFormula_a_power=iSatisfyingFormula_a.toString();
				iSatisfyingFormula_a_power="("+iSatisfyingFormula_a_power+")*("+iSatisfyingFormula_a_power+")";
				String iSatisfyingFormula_b_power=iSatisfyingFormula_b.toString();
				iSatisfyingFormula_b_power="("+iSatisfyingFormula_b_power+")*("+iSatisfyingFormula_b_power+")";

				bw.write(" elif c - "+q+"/"+tau+" = 0 and "+"p - ("+iSatisfyingFormula_a_power+" + "//" + \n 							   "
						+iSatisfyingFormula_b_power+")/"+iSatifsyingTheFormula.size()+" <= 0 then\n");
			}
			else {
				bw.write(" elif c - "+q+"/"+tau+" = 0 and "+"p - 0 <= 0 then\n");
			}
			
			for(i=1;i<=n;i++) {
				bw.write("  upd(a"+i+") = a"+i+"\n");
			}
			for(i=1;i<=n;i++) {
				bw.write("  upd(b"+i+") = b"+i+"\n");
			}
			bw.write("  upd(c) = c+1\n");
			bw.write("  upd(c_odd) = 1-c_odd\n");
			bw.write("  upd(r) = 1\n");

			bw.write(" end if\n");

			bw.write("end while\n");
			
			bw.write("\n");
			bw.write("reduceFPE(csvFile=\"FPE.csv\",fileWhereToStorePartition=\"partitions/"+modelName+"_part.txt\",prePartition=USER)\n");
			//
			bw.write("\n");

			bw.write("end Probabilistic Program\n");
			bw.close();
			
			if(print){
				end=System.currentTimeMillis();
				CRNReducerCommandLine.println(out,bwOut," Completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)"+".");
			}
		}
		return getInfoImporting();
	}


	
	
//	public InfoCRNImporting readCNFandMakeQuantumOptimization_stringForBinary(boolean print,String fileOut) throws FileNotFoundException, IOException, UnsupportedFormatException{
//
//		if(print){
//			CRNReducerCommandLine.println(out,bwOut," Reading CNF file"+getFileName());
//			CRNReducerCommandLine.println(out,bwOut,"  to make quantum optimization");
//		}
//		
//		initCRNAndMath();
//		initInfoImporting();
//		getInfoImporting().setLoadedCRN(true);
//
//
//		createParentDirectories(fileOut);
//		BufferedWriter bw=null;
//		boolean failed=false;
//		try {
//			bw = new BufferedWriter(new FileWriter(fileOut));
//		} catch (IOException e) {
//			CRNReducerCommandLine.println(out,bwOut,"Problems in CNF 2 QuantumOptimization. Exception raised while creating the filewriter for file: "+fileOut);
//			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
//			//return;
//			failed=true;
//		}
//
//		if(!failed) {
//			String name = overwriteExtensionIfEnabled(fileOut,"",true);
//			int i = name.lastIndexOf(File.separator);
//			if(i!=-1) {
//				name=name.substring(i+1);
//			}
//			String modelName=GUICRNImporter.getModelName(name); 
//			bw.write("begin Probabilistic Program "+modelName+"\n");
//
//
//			ArrayList<String> comments = new ArrayList<>();
//			long begin = System.currentTimeMillis();
//
//			//
//			CNFClauses cnfClausesOBJ = readCnfClauses(comments,"");
//
//			//Each internal array is a CNF clause
//			ArrayList<ArrayList<IUpdateFunction>> cnfClauses=cnfClausesOBJ.getCnfClauses();
//			int nvars=cnfClausesOBJ.getnVars();
//			int nClauses=cnfClausesOBJ.getnClauses();
//			
//			getInfoImporting().setReadSpecies(nvars);
//			getInfoImporting().setReadCRNReactions(-1);
//			long end=System.currentTimeMillis();
//			getInfoImporting().setRequiredMS(end -begin);
//			
//			
//			if(print){
//				CRNReducerCommandLine.println(out,bwOut," reading completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)"+".");
//				CRNReducerCommandLine.println(out,bwOut,"\nMaking and writing the quantum SAT optimization encoding in file "+fileOut);
//			}
//
//			
//			
//			
//			begin=System.currentTimeMillis();
//
//			//I pre-compute the list of clauses where each variables appears with positive sign
//			LinkedHashMap<Integer, ArrayList<Integer>> variableToClausesWhereItAppears=new LinkedHashMap<>(nvars);
//			LinkedHashMap<Integer, ArrayList<Integer>> variableToClausesWhereItAppears_negated=new LinkedHashMap<>(nvars);
//			for(int clause=0;clause<cnfClauses.size();clause++) {
//				ArrayList<IUpdateFunction> cnfClause = cnfClauses.get(clause);
//				for(IUpdateFunction leaf:cnfClause) {
//					if(leaf instanceof ReferenceToNodeUpdateFunction) {
//						addClauseToList(variableToClausesWhereItAppears, clause, leaf);
//					}
//					else {
//						//it must be a negated
//						IUpdateFunction inner = ((NotBooleanUpdateFunction)leaf).getInnerUpdateFunction();
//						addClauseToList(variableToClausesWhereItAppears_negated, clause, inner);
//					}
//				}
//			}
//
//
//
//			double q=nvars;
//			int n=(int)Math.pow(2, q);// n=2^q, 	2^8=256, 2^10=1024
//			double tau=0.1;	//we must have q/tau a natural!
//			double q_over_tau_double=q/tau;
//			int q_over_tau=(int)q_over_tau_double;
//			if(q_over_tau!=q_over_tau_double) {
//				CRNReducerCommandLine.println(out,bwOut,"\nq over tau should be an int. I terminate");
//				failed=true;
//				return null;
//			}
//
//
//
//			double sqrtn=Math.sqrt(n);
//			double oneOverSqrtn=1.0/sqrtn;
//
//			ArrayList<Integer> iSatifsyingTheFormula=new ArrayList<Integer>();
//
//			double lambda=nClauses;		//initialized to the number of clauses in the formula
//			double[] H = new double[n+1];//H[0] won't be used
//			
//			if(print){
//				CRNReducerCommandLine.print(out,bwOut,"\n\tBuilding Hii for i in [1,"+n+"] ... ");
//			}
//			
//			for(i=1;i<=n;i++) {
//				int iMinusOne=i-1;
//
//				String bitString=Integer.toBinaryString(iMinusOne);
//				while(bitString.length()<nvars) {
//					bitString="0"+bitString;
//				}
//				String[] bitStringArray=bitString.split("");
//
//				HashSet<Integer> clausesSatisfied=new LinkedHashSet<Integer>();
//				for(int b=0;b<bitStringArray.length;b++ ) {
//					String bit=bitStringArray[b];
//
//
//					//100 is meant to be x3,x2,x1 rather than x1,x2,x3 (actually x2,x1,x0 rather than x0,x1,x2)
//					int bToVar=(bitStringArray.length-1) - b;
//					bToVar = bToVar + 1; //from 0;n-1, to 1;n
//
//					if(bit.equals("1")) {
//						ArrayList<Integer> clausesWherItAppears = variableToClausesWhereItAppears.get(bToVar);
//						if(clausesWherItAppears!=null && clausesWherItAppears.size()>0) {
//							clausesSatisfied.addAll(clausesWherItAppears);
//						}
//					}
//					else  {
//						//must be "0"
//						ArrayList<Integer> clausesWherItAppearsNegated = variableToClausesWhereItAppears_negated.get(bToVar);
//						if(clausesWherItAppearsNegated!=null && clausesWherItAppearsNegated.size()>0) {
//							clausesSatisfied.addAll(clausesWherItAppearsNegated);
//						}
//					}
//				}
//
//				//Hi,i, (here H[i] denotes the number of clauses in the formula that are satisfied by the binary representation of i−1.
//				H[i]=clausesSatisfied.size();
//				if(H[i]==lambda) {
//					iSatifsyingTheFormula.add(i);
//				}
//			}
//			if(print){
//				CRNReducerCommandLine.println(out,bwOut," completed");
//			}
//
//			bw.write("//////////////////////////\n");
//			bw.write("//Internal parameters used\n");
//			bw.write("//tau="+tau+", q="+q+"\n");
//			bw.write("//H="+Arrays.toString(H)+"\n//\tH[0] shall be ignored\n");
//			bw.write("//////////////////////////\n\n");
//
//
//			bw.write("begin init\n");
//			//for(String species : speciesToUpdateFunction.keySet()) {
//			for(i=1;i<=n;i++) {
//				bw.write("  a"+i+"="+oneOverSqrtn+"  b"+i+"=0\n");
//			}
//			bw.write("  c=1\t\t//The counter\n");
//			bw.write("  c_odd=1\t//Used to represent it the counter is odd (1) or even (0)\n");
//			bw.write("  r=0\t\t//The results\n");
//			bw.write("end init\n\n");
//
//			bw.write("begin partition\n");
//			bw.write(" {");
//			for(i=1;i<=n;i++) {
//				bw.write("a"+i);
//				if(i<n) {
//					bw.write(",");
//				}
//			}
//			bw.write("},\n");
//			bw.write(" {");
//			for(i=1;i<=n;i++) {
//				bw.write("b"+i);
//				if(i<n) {
//					bw.write(",");
//				}
//			}
//			bw.write("}\n");
//			bw.write("end partition\n\n");
//			
//
//			bw.write("while true do\n");
//			bw.write(" begin parameters\n");
//			bw.write("  p = Uniform(0,1)\n");
//			bw.write(" end parameters\n");
//
//			bw.write("\n");
//			bw.write(" //////////////////////////////////////////////////\n");
//			bw.write(" //First if: we still need to iterate and c is even\n");
//			bw.write(" //////////////////////////////////////////////////\n");
//			bw.write(" if c_odd = 0 and c - "+q+"/"+tau+" < 0 then\n");
//			for(i=1;i<=n;i++) {
//				bw.write("  upd(a"+i+") = a"+i+" + "+tau+"*"+H[i]+"*b"+i+"\n");
//			}
//			bw.write("\n");
//			for(i=1;i<=n;i++) {
//				bw.write("  upd(b"+i+") = b"+i+" - "+tau+"*"+H[i]+"*a"+i+"\n");
//			}
//			bw.write("  upd(c) = c+1\n");
//			bw.write("  upd(c_odd) = 1-c_odd\n");
//			bw.write("  upd(r) = 0\n");
//
//			bw.write("\n");
//
//			bw.write(" //////////////////////////////////////////////////\n");
//			bw.write(" //Second if: we still need to iterate and c is odd\n");
//			bw.write(" //////////////////////////////////////////////////\n");
//			bw.write(" elif c_odd > 0  and c - "+q+"/"+tau+" < 0 then\n");
//			
//
//			StringBuffer sum_aj=new StringBuffer("");
//			StringBuffer sum_bj=new StringBuffer("");
//			boolean first=true;
//			for(i=1;i<=n;i++) {
//				if(!first) {
//					sum_aj.append(" + ");
//					sum_bj.append(" + ");
//				}
//				first=false;
//				
//				sum_aj.append("a");
//				sum_aj.append(i);
//
//				sum_bj.append("b");
//				sum_bj.append(i);
//			}
//			String sum_a=sum_aj.toString();
//			String sum_b=sum_bj.toString();
//			for(i=1;i<=n;i++) {
//				bw.write("  upd(a"+i+") = a"+i+" + "+tau+"*("+sum_a+")\n");
//			}
//			bw.write("\n");
//			for(i=1;i<=n;i++) {
//				bw.write("  upd(b"+i+") = b"+i+" + "+tau+"*("+sum_b+")\n");
//			}
//			bw.write("  upd(c) = c+1\n");
//			bw.write("  upd(c_odd) = 1-c_odd\n");
//			bw.write("  upd(r) = 0\n");
//
//
//			bw.write("\n");
//			bw.write(" //////////////////////////////////////////////////\n");
//			bw.write(" //Last if: no more iterations necessary, we can compute r\n");
//			bw.write(" //////////////////////////////////////////////////\n");
//			bw.write(" //i satisfying the formula:"+iSatifsyingTheFormula+"\n");
//			bw.write(" //////////////////////////////////////////////////\n");
//
//			StringBuffer iSatisfyingFormula_a = new StringBuffer("");
//			StringBuffer iSatisfyingFormula_b = new StringBuffer("");
//
//			first=true;
//			for(Integer iSat : iSatifsyingTheFormula) {
//				if(!first) {
//					iSatisfyingFormula_a.append(" + ");
//					iSatisfyingFormula_b.append(" + ");
//				}
//				first=false;
//				iSatisfyingFormula_a.append("a");
//				iSatisfyingFormula_a.append(iSat);
//
//				iSatisfyingFormula_b.append("b");
//				iSatisfyingFormula_b.append(iSat);
//			}
//			String iSatisfyingFormula_a_power=iSatisfyingFormula_a.toString();
//			iSatisfyingFormula_a_power="("+iSatisfyingFormula_a_power+")*("+iSatisfyingFormula_a_power+")";
//			String iSatisfyingFormula_b_power=iSatisfyingFormula_b.toString();
//			iSatisfyingFormula_b_power="("+iSatisfyingFormula_b_power+")*("+iSatisfyingFormula_b_power+")";
//
//			bw.write(" elif c - "+q+"/"+tau+" = 0 and "+"p - ("+iSatisfyingFormula_a_power+" + \n 							   "+
//																iSatisfyingFormula_b_power+")/"+iSatifsyingTheFormula.size()+" <= 0 then\n");
//
//			for(i=1;i<=n;i++) {
//				bw.write("  upd(a"+i+") = a"+i+"\n");
//			}
//			for(i=1;i<=n;i++) {
//				bw.write("  upd(b"+i+") = b"+i+"\n");
//			}
//			bw.write("  upd(c) = c+1\n");
//			bw.write("  upd(c_odd) = 1-c_odd\n");
//			bw.write("  upd(r) = 1\n");
//
//			bw.write(" end if\n");
//
//			bw.write("end while\n");
//			
//			bw.write("\n");
//			bw.write("reduceFPE(csvFile=\"FPE.csv\",fileWhereToStorePartition=\"partitions/"+modelName+"_part.txt\",prePartition=USER)\n");
//			//
//			bw.write("\n");
//
//			bw.write("end Probabilistic Program\n");
//			bw.close();
//			
//			if(print){
//				end=System.currentTimeMillis();
//				CRNReducerCommandLine.println(out,bwOut," Completed in "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)"+".");
//			}
//		}
//		return getInfoImporting();
//	}


	public void addClauseToList(LinkedHashMap<Integer, ArrayList<Integer>> variableToClausesWhereItAppears, int clause,
			IUpdateFunction leaf) {
		String spName=((ReferenceToNodeUpdateFunction)leaf).toString();
		Integer spNameInt=Integer.parseInt(spName);
		ArrayList<Integer> list = variableToClausesWhereItAppears.get(spNameInt);
		if(list==null) {
			list=new ArrayList<Integer>();
			variableToClausesWhereItAppears.put(spNameInt, list);
		}
		list.add(clause);
	}
	
	

	
	
	
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
