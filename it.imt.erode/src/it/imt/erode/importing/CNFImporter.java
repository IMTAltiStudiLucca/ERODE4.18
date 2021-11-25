package it.imt.erode.importing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;

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
	public CNFImporter(String fileName,MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower) {
		super(fileName,out,bwOut,msgDialogShower);
	}

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

	public IBooleanNetwork getBN() {
		return bn;
	}

}
