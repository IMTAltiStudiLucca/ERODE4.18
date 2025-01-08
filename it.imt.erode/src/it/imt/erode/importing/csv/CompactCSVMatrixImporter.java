package it.imt.erode.importing.csv;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
//import java.util.Iterator;
import java.util.HashSet;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.auxiliarydatastructures.ArrayListOfReactions;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.DialogType;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.crn.implementations.CRNReaction;
import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.implementations.Species;
import it.imt.erode.crn.implementations.SpeciesCompact;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.importing.AbstractImporter;
import it.imt.erode.importing.InfoCRNImporting;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.partitionrefinement.algorithms.CRNBisimulations;
import it.imt.erode.partitionrefinement.algorithms.CRNBisimulationsNAry;


/**
 * 
 * @author Andrea Vandin
 * This class is used to import linear systems given as matrices in compact .csv format.
 * 	The first line gives the number of species three times: S,S,S
	Then, each line row,col,val says that in position (row,col) we have value val. All other entries are 0.
 */
public class CompactCSVMatrixImporter extends AbstractImporter{

	public static final String CSVMatricesFolder = "."+File.separator+"CompactMatrices"+File.separator;
	//public static final boolean DECREASECOLANDROWBYONE


	//private String form;//can be either A*X (linear systems), or P*Q (Markov chain), or FEM (to apply Jacobi)
	private MatrixForm form;
	private static final MathContext mc = new MathContext(20, RoundingMode.HALF_UP);
	private boolean normalize=false;
	private String bFile;
	private String icFile;
	private boolean createParameters=false;
	private boolean addReverseEdges=false;
	private boolean speciesIcreated=false;
	//private int inputs=0;



	public CompactCSVMatrixImporter(String fileName, String[] formAndOtherParameters,MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower) {
		super(fileName,out,bwOut,msgDialogShower);
		speciesIcreated=false;

		MatrixForm mf = null;
		/*if(formAndOtherParameters!=null && formAndOtherParameters.length>0){
			mf = MatrixForm.valueOf(formAndOtherParameters[0]);
		}
		if(mf==null){
			throw new UnsupportedOperationException("The form of the martrix can be either AX, PQ or FEM.");
		}
		else if(mf.equals(MatrixForm.AX)){
			this.form=MatrixForm.AX;
		} 
		else if(mf.equals(MatrixForm.PQ)){
			this.form=MatrixForm.PQ;
		}
		else if(mf.equals(MatrixForm.FEM)){
			this.form=MatrixForm.FEM;
		}
		else{
			throw new UnsupportedOperationException("The form of the martrix can be either AX, PQ or FEM.");
		}
		if(formAndOtherParameters!=null && formAndOtherParameters.length>1 && formAndOtherParameters[1].equalsIgnoreCase("true")){
			normalize=true;
		}*/

		if(formAndOtherParameters!=null && formAndOtherParameters.length>0){
			for(int i=0;i<formAndOtherParameters.length;i++){
				if(formAndOtherParameters[i].equalsIgnoreCase("form")){
					mf = MatrixForm.valueOf(formAndOtherParameters[i+1]);
					if(mf==null){
						throw new UnsupportedOperationException("The form of the martrix can be either AX, PQ or FEM.");
					}
					else if(mf.equals(MatrixForm.AX)){
						this.form=MatrixForm.AX;
					} 
					else if(mf.equals(MatrixForm.PQ)){
						this.form=MatrixForm.PQ;
					}
					else if(mf.equals(MatrixForm.FEM)){
						this.form=MatrixForm.FEM;
					}
					i++;
				}
				else if(formAndOtherParameters[i].equalsIgnoreCase("normalize")){
					if(formAndOtherParameters[i+1].equalsIgnoreCase("true")){
						normalize=true;
					}
					else{
						normalize=false;
					}
					i++;
				}
				else if(formAndOtherParameters[i].equalsIgnoreCase("bFile")){
					bFile = formAndOtherParameters[i+1];
					i++;
				}
				//specific for importAffineSystem
				else if(formAndOtherParameters[i].equalsIgnoreCase("icFile")){
					icFile = formAndOtherParameters[i+1];
					i++;
				}
				else if(formAndOtherParameters[i].equalsIgnoreCase("createParameters")){
					createParameters = Boolean.valueOf(formAndOtherParameters[i+1]);
					i++;
				}
				else if(formAndOtherParameters[i].equalsIgnoreCase("addReverseEdges")){
					addReverseEdges = Boolean.valueOf(formAndOtherParameters[i+1]);
					i++;
				}
//				else if(formAndOtherParameters[i].equalsIgnoreCase("inputs")){
//					inputs = Integer.valueOf(formAndOtherParameters[i+1]);
//					i++;
//				}
			}
		}


	}

	public InfoCRNImporting importLinearSystemWithInputs(boolean printInfo, boolean printCRN,boolean print) throws FileNotFoundException, IOException, UnsupportedFormatException{
		CRNReducerCommandLine.println(out,bwOut,"\nImporting linear system with inputs:");
		CRNReducerCommandLine.println(out,bwOut,"\tA : "+getFileName());
		CRNReducerCommandLine.println(out,bwOut,"\tb"+" : "+bFile+".csv");
		
		
		init();
		
		BufferedReader br = createSpecies();
		
		long begin = System.currentTimeMillis();
		BigDecimal[] outgoingRates = loadSimpleMatrix(br/*,speciesIdToSpecies*/);
		br.close();
		CRNReducerCommandLine.println(out,bwOut,"\t"+"Main matrix loaded.");
		
		//loadBAndAddItToDrift(outgoingRates/*, speciesIdToSpecies*/);
		HashSet<ISpecies> inputSpecies = loadBWithInputsAndAddItToDrift(outgoingRates);
		CRNReducerCommandLine.println(out,bwOut,"\t"+"b loaded: added "+inputSpecies.size()+" input variables.");
		
		expandPartitionWithInputs(inputSpecies);
		ArrayList<HashSet<ISpecies>> userDefinedInitialPartition = new ArrayList<HashSet<ISpecies>>(1);
		userDefinedInitialPartition.add(inputSpecies);
		getCRN().setUserDefinedPartition(userDefinedInitialPartition );
		
		
		long end=System.currentTimeMillis();

		CRNReducerCommandLine.println(out,bwOut,"\t"+getCRN().getSpeciesSize()+" variables loaded and "+getCRN().getReactions().size() +" reactions created. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");

		begin = System.currentTimeMillis();

		getInfoImporting().setReadSpecies(getCRN().getSpeciesSize());
		getInfoImporting().setReadCRNReactions(getCRN().getReactions().size());
		//getInfoImporting().setReadReagents(getCRN().getReagents().size());
		//getInfoImporting().setReadProducts(getCRN().getProducts().size());
		end=System.currentTimeMillis();
		getInfoImporting().setRequiredMS(end -begin);

		

//		if(print){
//			if(printInfo){
//				CRNReducerCommandLine.println(out,bwOut,getInfoImporting().toString());
//			}
//			if(printCRN){
//				getCRN().printCRN();
//			}
//		}



		//CRNReducerCommandLine.println(out,bwOut,crn);
		return getInfoImporting();
	}
	
	public InfoCRNImporting importAffineSystem(int numberOfVariables, int[] rows, int[] columns, double[] values, double[] B) throws  UnsupportedFormatException, IOException{
		return importAffineSystem(false,false,false,numberOfVariables, rows, columns, values,B);
	}
	public InfoCRNImporting importAffineSystem(boolean printInfo, boolean printCRN,boolean print,int numberOfVariables, int[] rows, int[] columns, double[] values, double[] B) throws UnsupportedFormatException, IOException{
		
		if(print){
			CRNReducerCommandLine.println(out,bwOut,"\nImporting affine system:");
			CRNReducerCommandLine.println(out,bwOut,"\t from lists");
			if(addReverseEdges) {
				CRNReducerCommandLine.println(out,bwOut,"\tAdding reverse edges: for every edge (i,j,w) we add the reverse one (j,i,w).");
			}
		}
		
		init();
		
		long begin=System.currentTimeMillis();
		
		//load species
		loadSpecies(numberOfVariables,false);
		getInfoImporting().setReadSpecies(getCRN().getSpecies().size());
		
		
		//load A
		BigDecimal[] outgoingRates=null;//DEFAULT NULL. CHANGE IT IF YOU WANT TO SCALE! Not necessary for Ax=b
		for(int i =0; i< rows.length;i++) {
			int row = rows[i]-1;
			int col = columns[i]-1;
			BigDecimal val = BigDecimal.valueOf(values[i]);
			String valExpr = String.valueOf(values[i]);//val.toPlainString();
			addReaction(outgoingRates, i, row, col, val, valExpr,"");
			if(addReverseEdges) {
				addReaction(outgoingRates, i, col, row, val, valExpr,"_rev");
			}
		}
		
		
		//load b
		ISpecies I = null;
		MutableInt row = new MutableInt(0);
		//double[] B=null;
		for(int i=0;i<B.length;i++) {
			I = addBentry(outgoingRates, I, row, B[i]);
		}
		
		//load IC
		//not necessary
		
		
		getInfoImporting().setReadCRNReactions(getCRN().getReactions().size());
		//getInfoImporting().setReadReagents(getCRN().getReagents().size());
		//getInfoImporting().setReadProducts(getCRN().getProducts().size());
		long end=System.currentTimeMillis();
		getInfoImporting().setRequiredMS(end -begin);
		
		return getInfoImporting();
	}
	
	
	public InfoCRNImporting importAffineSystem(boolean printInfo, boolean printCRN,boolean print) throws FileNotFoundException, IOException, UnsupportedFormatException{
		if(print){
			CRNReducerCommandLine.println(out,bwOut,"\nImporting affine system:");
			CRNReducerCommandLine.println(out,bwOut,"\tA : "+getFileName());
			CRNReducerCommandLine.println(out,bwOut,"\tb : "+bFile);
			CRNReducerCommandLine.println(out,bwOut,"\tIC: "+icFile);
			if(addReverseEdges) {
				CRNReducerCommandLine.println(out,bwOut,"\tAdding reverse edges: for every edge (i,j,w) we add the reverse one (j,i,w).");
			}
		}

		init();

		BufferedReader br = createSpecies();


		long begin = System.currentTimeMillis();
		BigDecimal[] outgoingRates = loadSimpleMatrix(br/*,speciesIdToSpecies*/);
		//speciesIdToSpecies=
		loadBAndAddItToDrift(outgoingRates/*, speciesIdToSpecies*/);
		long end=System.currentTimeMillis();

		CRNReducerCommandLine.println(out,bwOut,"\t"+getCRN().getReactions().size() +" reactions loaded. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");

		begin = System.currentTimeMillis();
		loadIC(/*speciesIdToSpecies*/);
		end=System.currentTimeMillis();

		CRNReducerCommandLine.println(out,bwOut,"\tInitial conditions loaded. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");

		getInfoImporting().setReadCRNReactions(getCRN().getReactions().size());
		//getInfoImporting().setReadReagents(getCRN().getReagents().size());
		//getInfoImporting().setReadProducts(getCRN().getProducts().size());
		end=System.currentTimeMillis();
		getInfoImporting().setRequiredMS(end -begin);

		br.close();

		if(print){
			if(printInfo){
				CRNReducerCommandLine.println(out,bwOut,getInfoImporting().toString());
			}
			if(printCRN){
				getCRN().printCRN();
			}
		}



		//CRNReducerCommandLine.println(out,bwOut,crn);
		return getInfoImporting();
	}

	private BufferedReader createSpecies() throws FileNotFoundException, IOException {
		BufferedReader br = getBufferedReader();
		String line; 
		//boolean reactionsLoaded=false;
		//HashMap<Integer, ISpecies> speciesIdToSpecies=new HashMap<Integer, ISpecies>();
		//ISpecies[] speciesIdToSpecies=null;
		//BigDecimal[] sumsOfEachCol=null;
		
		
		long begin = System.currentTimeMillis();
		boolean stop=false;
		while ((!stop)&& (line = br.readLine()) != null) {
			line=line.trim();
			//Any other line is ignored (including comments)
			if(line.equals("")||line.startsWith("#")){
				//if this is a comment line, skip to the next iteration of the loop (i.e. to the next line)
				continue;
			}
			stop=true;

			CompactCSVEntry csvEntry = parseCSVEntry(line);
			int numberOfSpecies= csvEntry.getRow()+1;
			/*
			//We have to add the constant species I to represent constants in the drift
			numberOfSpecies++;
			speciesIdToSpecies = loadSpecies(numberOfSpecies,true);
			*/
			//We don't add the species I yet. 
			//We add it later only if 'b' actually contained any constant
			//speciesIdToSpecies = 
			loadSpecies(numberOfSpecies,false);
			
			long end = System.currentTimeMillis();
			//sumsOfEachCol = new BigDecimal[speciesIdToSpecies.length];
			CRNReducerCommandLine.println(out,bwOut,"\t"+getCRN().getSpecies().size()+" variables created for the main matrix. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");
			getInfoImporting().setReadSpecies(getCRN().getSpecies().size());
		}
		return br;
	}

	private void init() {
		speciesIcreated=false;
		initInfoImporting();
		initCRNAndMath();
		getInfoImporting().setLoadedCRN(true);
	}

	public InfoCRNImporting importCSVMatrix(boolean printInfo, boolean printCRN,boolean print) throws FileNotFoundException, IOException, UnsupportedFormatException{

		if(print){
			if(normalize){
				CRNReducerCommandLine.println(out,bwOut,"\nImporting ("+form+",normalize): "+getFileName());
			}
			else{
				CRNReducerCommandLine.println(out,bwOut,"\nImporting ("+form+"): "+getFileName());
			}

		}

		initInfoImporting();
		initCRNAndMath();
		ICRN crn = getCRN();
		getInfoImporting().setLoadedCRN(true);

		BufferedReader br = getBufferedReader();
		String line; 
		//boolean reactionsLoaded=false;
		//HashMap<Integer, ISpecies> speciesIdToSpecies=new HashMap<Integer, ISpecies>();
		//ISpecies[] speciesIdToSpecies=null;
		//BigDecimal[] sumsOfEachCol=null;

		boolean isFEM = form.equals(MatrixForm.FEM);

		boolean stop=false;
		while ((!stop)&& (line = br.readLine()) != null) {
			line=line.trim();
			//Any other line is ignored (including comments)
			if(line.equals("")||line.startsWith("#")){
				//if this is a comment line, skip to the next iteration of the loop (i.e. to the next line)
				continue;
			}
			stop=true;

			CompactCSVEntry csvEntry = parseCSVEntry(line);
			long begin = System.currentTimeMillis();
			int numberOfSpecies= csvEntry.getRow()+1;
			if(isFEM){
				numberOfSpecies++;
			}
			//speciesIdToSpecies = 
			loadSpecies(numberOfSpecies,isFEM);
			long end = System.currentTimeMillis();
			//sumsOfEachCol = new BigDecimal[speciesIdToSpecies.length];
			CRNReducerCommandLine.println(out,bwOut,getCRN().getSpecies().size()+" species loaded. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");
			getInfoImporting().setReadSpecies(getCRN().getSpecies().size());
		}

		long begin = System.currentTimeMillis();
		if(isFEM){
			applyJacobi(br/*,speciesIdToSpecies*/);
		}
		else{
			loadSimpleMatrix(br/*,speciesIdToSpecies*/);
		}
		long end=System.currentTimeMillis();

		CRNReducerCommandLine.println(out,bwOut,getCRN().getReactions().size() +" reactions loaded. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");

		getInfoImporting().setReadCRNReactions(getCRN().getReactions().size());
		//getInfoImporting().setReadReagents(getCRN().getReagents().size());
		//getInfoImporting().setReadProducts(getCRN().getProducts().size());
		end=System.currentTimeMillis();
		getInfoImporting().setRequiredMS(end -begin);



		/*for(int i=0;i<sumsOfEachCol.length;i++){		
			if(sumsOfEachCol[i]==null){
				CRNReducerCommandLine.println(out,bwOut,"Column "+i+" has NULL value");
				sumsOfEachCol[i]=BigDecimal.ZERO;
			}
			if(BigDecimal.ZERO.compareTo(sumsOfEachCol[i])!=0){
				CRNReducerCommandLine.println(out,bwOut,"Problem: column "+i+" has value "+sumsOfEachCol[i]);
			}
		}*/

		br.close();

		if(print){
			if(printInfo){
				CRNReducerCommandLine.println(out,bwOut,getInfoImporting().toString());
			}
			if(printCRN){
				crn.printCRN();
			}
		}



		//CRNReducerCommandLine.println(out,bwOut,crn);
		return getInfoImporting();


	}



	private void applyJacobi(BufferedReader brMatrix/*, ISpecies[] speciesIdToSpecies*/) throws IOException, UnsupportedFormatException  {
		//AX=b
		long begin=System.currentTimeMillis();
		//BigDecimal[] b = loadB(speciesIdToSpecies.length-1);
		BigDecimal[] b = loadB(getCRN().getSpeciesSize()-1);//why -1?
		//long end=System.currentTimeMillis();
		//CRNReducerCommandLine.println(out,bwOut,getCRN().getSpecies().size()+" b loaded. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");

		String lineMatrix;

		//ISpecies I = speciesIdToSpecies[speciesIdToSpecies.length-1];
		ISpecies I = getCRN().getSpecies().get(getCRN().getSpeciesSize()-1);
		int i=1;
		begin = System.currentTimeMillis();

		int nonZeroDiagonalEntries=0;

		while ((lineMatrix = brMatrix.readLine()) != null) {
			lineMatrix=lineMatrix.trim();

			CompactCSVEntry currentMatrixEntry = parseCSVEntry(lineMatrix);
			int row = currentMatrixEntry.getRow();
			int col = currentMatrixEntry.getCol();
			BigDecimal val = currentMatrixEntry.getVal();
			i++;
			if(i%500000==0){
				CRNReducerCommandLine.println(out,bwOut,i + " reactions loaded after "+(System.currentTimeMillis()-begin)/1000.0+ " seconds");
			}

			if(row==col){
				//nonZeroDiagonalEntries++;
				//System.out,bwOut.println(val);
				if(BigDecimal.ZERO.compareTo(val)!=0){
					nonZeroDiagonalEntries++;
				}
				else{
					System.out.println("!!!!!!!diagional "+row+ " has value 0!");
				}
			}


			if(row==-1 || col==-1){
				throw new UnsupportedFormatException("The file refers to a column or row \"-1\".");
				/*CRNReducerCommandLine.printWarning(out,bwOut,true,msgDialogShower,"Row or col are -1. I terminate.");
				return false;*/
			}
			if(BigDecimal.ZERO.compareTo(val)==0){
				if(row==col){
					throw new UnsupportedFormatException("An element of the diagonal is 0.");
					//CRNReducerCommandLine.printWarning(out,bwOut,true,msgDialogShower,"An element of the diagonal is 0. I terminate.");
					//return false;
				}
				else{
					continue;//I can skip this, because it would not create any new reaction
				}
			}
			else if(row==col){
				//diagonal entry
				if(b[row].compareTo(BigDecimal.ZERO)==0){
					continue;//I can skip this, because it would not create any new reaction
				}
				else{
					//If I am here, then I have the correct B entry, and I have to consider it
					loadReactionDiagonalJacobi(row, val, b[row]/*, speciesIdToSpecies*/, I.getID());
				}
			}
			else{
				//non-diagonal entry
				val = BigDecimal.ZERO.subtract(val);
				loadReactionLinearSystemAX(row, col, val, val.toPlainString()/*, speciesIdToSpecies*/,null);
			}

		}

		if(nonZeroDiagonalEntries<b.length){
			//System.out,bwOut.println("b.length="+b.length+ " species:"+speciesIdToSpecies.length);
			int missingDiagonalEntries = (b.length-nonZeroDiagonalEntries);
			String msg = missingDiagonalEntries+" entries of the diagonal have value 0.";
			if(missingDiagonalEntries==1){
				msg="1 entry of the diagonal has value 0.";
			}
			throw new UnsupportedFormatException(msg);
		}

		//end = System.currentTimeMillis();
		//CRNReducerCommandLine.println(out,bwOut,"Line "+i + " at "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " seconds");
	}


	private BigDecimal[] loadB(int length, String pathOfB) throws IOException, UnsupportedFormatException {
		BigDecimal[] b = new BigDecimal[length];

		/*for(int i=0;i<b.length;i++){
			b[i]=BigDecimal.valueOf(i);
		}*/
		BufferedReader brB;


		CRNReducerCommandLine.println(out,bwOut, "Loading the b vector from: "+pathOfB);

		try {
			brB = getBufferedReader(pathOfB);
		} catch (FileNotFoundException e) {
			brB=null;
			CRNReducerCommandLine.printWarning(out,bwOut,true,msgDialogShower,"I could not find the b file "+pathOfB+".\n I assume that b is empty",DialogType.Error);
			return null;
		}

		int i=0;
		String lineB=null;
		while ((lineB = brB.readLine()) != null) {
			//lineB=lineB.trim();
			if(i > b.length-1){
				//CRNReducerCommandLine.printWarning(out,bwOut,true,msgDialogShower,"The size of b is wrong. It should contain " +b.length+" entries.");
				//return null;
				throw new UnsupportedFormatException("The size of b is wrong. It should contain " +b.length+" entries.");

			}
			else{
				b[i]=BigDecimal.valueOf(Double.valueOf(lineB));
				i++;
			}

		}

		if(brB!=null){
			brB.close();
		}

		return b;
	}

	private BigDecimal[] loadB(int length) throws IOException, UnsupportedFormatException {
		String pathOfB = "";
		if(getFileName().endsWith("_matrix.csv")){
			pathOfB = getFileName().replace("_matrix.csv", "_b.csv");
		}
		else if(getFileName().endsWith(".csv")){
			pathOfB = getFileName().replace(".csv", "_b.csv");
		}

		return loadB(length, pathOfB);
	}

	private CompactCSVEntry parseCSVEntry(String line) {
		String[] entries = line.split(",");
		int row = (int) getMath().evaluate(entries[0]) - 1;
		int col = (int) getMath().evaluate(entries[1]) - 1;
		BigDecimal val = BigDecimal.valueOf(Double.valueOf(entries[2]));
		return new CompactCSVEntry(row, col, val);
	}



	private BigDecimal[] loadSimpleMatrix(BufferedReader br/*,ISpecies[] speciesIdToSpecies*/) throws IOException, UnsupportedFormatException {
		String line;

		BigDecimal[] outgoingRates = null;
		if(normalize){
			outgoingRates = new BigDecimal[/*speciesIdToSpecies.length*/getCRN().getSpecies().size()];
		}

		int i=0;
		while ((line = br.readLine()) != null) {
			line=line.trim();
			//Any other line is ignored (including comments)
			if(line.equals("")||line.startsWith("#")){
				//if this is a comment line, skip to the next iteration of the loop (i.e. to the next line)
				continue;
			}

			CompactCSVEntry currentCSVEntry = parseCSVEntry(line);
			int row = currentCSVEntry.getRow();
			int col = currentCSVEntry.getCol();
			BigDecimal val = currentCSVEntry.getVal();
			String valExpr = val.toPlainString();
			addReaction(outgoingRates, i, row, col, val, valExpr,"");
			if(addReverseEdges) {
				addReaction(outgoingRates, i, col, row, val, valExpr,"_rev");
			}

			/*if(sumsOfEachCol[col]==null){
					sumsOfEachCol[col]=val;
				}
				else{
					sumsOfEachCol[col] = val.add(sumsOfEachCol[col]);
				}*/
			i++;
		}

		if(outgoingRates!=null && normalize){
			for (ICRNReaction reaction : getCRN().getReactions()) {
				ISpecies reagent = reaction.getReagents().getFirstReagent();
				BigDecimal totalOut = outgoingRates[reagent.getID()];
				BigDecimal normalizedRate = reaction.getRate().divide(totalOut,CRNBisimulationsNAry.getSCALE(),CRNBisimulationsNAry.RM);
				reaction.setRate(normalizedRate, normalizedRate.toPlainString());
				
			}

			ArrayListOfReactions[] reactionsToConsiderForEachSpecies =  new ArrayListOfReactions[getCRN().getSpecies().size()];
			CRNBisimulations.addToOutgoingReactionsOfReagents(getCRN().getReactions(), reactionsToConsiderForEachSpecies);
			
			for(ISpecies species : getCRN().getSpecies()){
				BigDecimal totalOut = BigDecimal.ZERO;
				ArrayListOfReactions reactions = reactionsToConsiderForEachSpecies[species.getID()];
				if(reactions!=null && reactions.reactions!=null) {
				//if(species.getOutgoingReactions()!=null){
					//for(ICRNReaction reaction : species.getOutgoingReactions()){
					for(ICRNReaction reaction : reactions.reactions){
						totalOut=totalOut.add(reaction.getRate());
					}
					//CRNReducerCommandLine.println(out,bwOut,species+" "+totalOut);
				}
				else{
					//CRNReducerCommandLine.println(out,bwOut,species+" HAS NO OUTGOING REACTIONS!!!!");
				}

			}
		}

		return outgoingRates;
	}

	public void addReaction(BigDecimal[] outgoingRates, int i, int row, int col, BigDecimal val, String valExpr,String suffixParam)
			throws IOException {
		//TODO: REMOVE REMOVE REMOVE REMOVE REMOVE REMOVE REMOVE REMOVE REMOVE REMOVE
		
		/*
		MathContext mc = new MathContext(1, RoundingMode.HALF_UP);
		val =val.round(mc);
		valExpr=val.toPlainString();
		*/
		/*
		valExpr="1";
		val=BigDecimal.ONE;
		*/
		
		
		/*
		//luca_test2inv
		int scale=1;
		double threshold=0.19;
		//MathContext mc = new MathContext(0, RoundingMode.HALF_UP);
		//val =val.round(mc);
		val =val.setScale(scale,RoundingMode.HALF_UP);
		valExpr=val.toPlainString();
		if(val.compareTo(BigDecimal.ZERO)==0) {
			val=BigDecimal.ZERO;
			valExpr="0";
			return;
		}
		if(val.compareTo(new BigDecimal(threshold))>=0) {
			val=BigDecimal.ZERO;
			valExpr="0";
			return;
		}
		else {
			val=BigDecimal.ONE;
			valExpr="1";
		}
		*/
		
		/*
		//swiss inv
		int scale=2;
		double threshold=0.155;
		//MathContext mc = new MathContext(0, RoundingMode.HALF_UP);
		//val =val.round(mc);
		val =val.setScale(scale,RoundingMode.HALF_UP);
		valExpr=val.toPlainString();
		if(val.compareTo(BigDecimal.ZERO)==0) {
			val=BigDecimal.ZERO;
			valExpr="0";
			return;
		}
		if(val.compareTo(new BigDecimal(threshold))>=0) {
			val=BigDecimal.ZERO;
			valExpr="0";
			return;
		}
		else {
			val=BigDecimal.ONE;
			valExpr="1";
		}
		*/
		
		
		
		//TODO: REMOVE REMOVE REMOVE REMOVE REMOVE REMOVE REMOVE REMOVE REMOVE REMOVE
		
		
		if(createParameters) {
			String paramName = "p" + i+suffixParam;
			addParameter(paramName, valExpr);
			valExpr=paramName;
		}

		if(form.equals(MatrixForm.PQ)){
			loadReactionMarkovChainPQ(row,col,val,valExpr/*,speciesIdToSpecies*/,outgoingRates);
		}
		else{
			loadReactionLinearSystemAX(row, col, val,valExpr/*, speciesIdToSpecies*/,outgoingRates);
		}
		//return valExpr;
	}

	private HashSet<ISpecies> loadBWithInputsAndAddItToDrift(BigDecimal[] outgoingRates/*, ISpecies[] speciesIdToSpecies*/) throws IOException, UnsupportedFormatException {
		//BigDecimal[] b = loadB(getCRN().getSpecies().size(),bFile);
		
		HashMap<Integer, ISpecies> iToUi= new HashMap<>();
		
		BufferedReader brB=null;
		try {
			brB = getBufferedReader(bFile);
		} catch (FileNotFoundException e) {
			throw new UnsupportedFormatException("I could not find the b file"+bFile);
		}

		String line;
		while ((line = brB.readLine()) != null) {
			line=line.trim();
			//Any other line is ignored (including comments)
			if(line.equals("")||line.startsWith("#")){
				//if this is a comment line, skip to the next iteration of the loop (i.e. to the next line)
				continue;
			}

			CompactCSVEntry currentCSVEntry = parseCSVEntry(line);
			int row = currentCSVEntry.getRow();
			int col = currentCSVEntry.getCol();
			if( (speciesIcreated && row > getCRN().getSpecies().size()-2) ||  
					((!speciesIcreated) && row > getCRN().getSpecies().size()-1) ){
				throw new UnsupportedFormatException("The size of b is wrong. It should contain " +(getCRN().getSpecies().size()-1)+" entries.");
			}
			
			col=col+1;
			ISpecies ucol = iToUi.get(col);
			if(ucol==null) {
				ucol = new Species("u"+col, getCRN().getSpeciesSize(), BigDecimal.ONE, "1", false);
				getCRN().addSpecies(ucol);
				iToUi.put(col, ucol);
			}
			
			BigDecimal val = currentCSVEntry.getVal();
			if(val.compareTo(BigDecimal.ZERO)!=0) {
				loadReactionLinearSystemAXFromEntryOfB(row, val, val.toPlainString(), /*speciesIdToSpecies,*/ outgoingRates, ucol);
			}
		}

		if(brB!=null){
			brB.close();
		}
		
		return new HashSet<ISpecies>(iToUi.values());


	}
	
	
	private //ISpecies[] 
			void loadBAndAddItToDrift(BigDecimal[] outgoingRates/*, ISpecies[] speciesIdToSpecies*/) throws IOException, UnsupportedFormatException {
		//BigDecimal[] b = loadB(getCRN().getSpecies().size(),bFile);

		BufferedReader brB=null;
		try {
			brB = getBufferedReader(bFile);
		} catch (FileNotFoundException e) {
			throw new UnsupportedFormatException("I could not find the b file "+bFile);
			/*brB=null;
			CRNReducerCommandLine.printWarning(out,bwOut,true,msgDialogShower,"I could not find the b file "+bFile+".\n",DialogType.Error);
			return false;*/
		}

		ISpecies I = null;
		//ISpecies I = speciesIdToSpecies[speciesIdToSpecies.length-1];

		//int row=0;
		MutableInt row = new MutableInt(0);
		String lineB=null;
		while ((lineB = brB.readLine()) != null) {
			lineB=lineB.trim();
			if(!lineB.isEmpty()) {
				double lineBDouble=Double.valueOf(lineB);
				I = addBentry(outgoingRates, I, row, lineBDouble);
			}
			
		}

		if(brB!=null){
			brB.close();
		}

		//return speciesIdToSpecies;

	}

	public ISpecies addBentry(BigDecimal[] outgoingRates, ISpecies I, MutableInt row, double Bentry)
			throws UnsupportedFormatException, IOException {
			if( (speciesIcreated && row.getVal() > getCRN().getSpecies().size()-2) ||  
				((!speciesIcreated) && row.getVal() > getCRN().getSpecies().size()-1) ){
			//if(row > getCRN().getSpecies().size()-2){
				//CRNReducerCommandLine.printWarning(out,bwOut,true,msgDialogShower,"The size of b is wrong. It should contain " +b.length+" entries.");
				//return null;
				throw new UnsupportedFormatException("The size of b is wrong. It should contain " +(getCRN().getSpecies().size()-1)+" entries.");
			}
			else{
				BigDecimal val=BigDecimal.valueOf(Bentry);
				if(val.compareTo(BigDecimal.ZERO)!=0) {
					if(speciesIcreated && I==null) {
						I = getCRN().getSpecies().get(getCRN().getSpeciesSize()-1);
						//I = speciesIdToSpecies[speciesIdToSpecies.length-1];
					}
					else if(!speciesIcreated){
						//speciesIdToSpecies = addIAtTheEnd(speciesIdToSpecies);
						addIAtTheEnd();
						//I = speciesIdToSpecies[speciesIdToSpecies.length-1];
						I = getCRN().getSpecies().get(getCRN().getSpeciesSize()-1);
						speciesIcreated=true;
					}
					loadReactionLinearSystemAXFromEntryOfB(row.getVal(), val, val.toPlainString(), /*speciesIdToSpecies,*/ outgoingRates, I);
				}
				//row++;
				row.increaseVal();
			}
		return I;
	}

	private void loadIC(/*ISpecies[] speciesIdToSpecies*/) throws IOException, UnsupportedFormatException {

		BufferedReader brIC=null;
		try {
			brIC = getBufferedReader(icFile);
		} catch (FileNotFoundException e) {
			throw new UnsupportedFormatException("I could not find the file containig the initial conditions "+icFile);
		}

		int i=0;
		String lineIC=null;
		while ((lineIC = brIC.readLine()) != null) {
			if( (speciesIcreated && i > getCRN().getSpecies().size()-2) ||  
				((!speciesIcreated) && i > getCRN().getSpecies().size()-1) ){
			//if(i > getCRN().getSpecies().size()-2){
				throw new UnsupportedFormatException("The size of IC is wrong. It should contain " +(getCRN().getSpecies().size()-1)+" entries.");
			}
			else{
				BigDecimal val=BigDecimal.valueOf(Double.valueOf(lineIC));
				//ISpecies species = speciesIdToSpecies[i];
				ISpecies species = getCRN().getSpecies().get(i);
				species.setInitialConcentration(val, val.toPlainString());
				i++;
			}
		}
		if(brIC!=null){
			brIC.close();
		}
	}


	private /*ISpecies[]*/ void loadSpecies(int numberOfSpecies, boolean lastSpeciesIsConstantI) {
		//ISpecies[] speciesIdToSpecies=new ISpecies[numberOfSpecies];

		IBlock blockOfNormalSpecies = new Block();
		IBlock blockOfI = null;

		if(lastSpeciesIsConstantI){
			blockOfI = new Block();
			setInitialPartition(new Partition(blockOfNormalSpecies,blockOfI,numberOfSpecies));
		}
		else{
			setInitialPartition(new Partition(blockOfNormalSpecies,numberOfSpecies));
		}

		for(int i=0;i<numberOfSpecies;i++){
			int id = i;
			ISpecies species;
			if(lastSpeciesIsConstantI&&i==numberOfSpecies-1){
				String speciesName = Species.I_SPECIESNAME;//"I";
				species = new Species(speciesName, id, BigDecimal.ONE,"1.0",false);
				speciesIcreated=true;
			}
			else{
				//String speciesName = "s"+id;
				//species = new Species(speciesName, id, BigDecimal.ZERO,"0.0",false);
				species = new SpeciesCompact(id, BigDecimal.ZERO);
			}
			getCRN().addSpecies(species);
			//speciesIdToSpecies[i]=species;


			
			if(lastSpeciesIsConstantI && i==numberOfSpecies-1){
			//if(lastSpeciesIsConstantI && i==speciesIdToSpecies.length-1){
				blockOfI.addSpecies(species);
			}
			else{
				blockOfNormalSpecies.addSpecies(species);
			}

		}
		//return speciesIdToSpecies;
	}
	
	private //ISpecies[] 
			void addIAtTheEnd(/*ISpecies[] oldSpeciesIdToSpecies*/) {
		int numberOfSpeciesWithoutI = getCRN().getSpeciesSize();//oldSpeciesIdToSpecies.length;
		/*ISpecies[] speciesIdToSpecies=new ISpecies[numberOfSpeciesWithoutI+1];
		for(int i=0;i<oldSpeciesIdToSpecies.length;i++) {
			speciesIdToSpecies[i]=oldSpeciesIdToSpecies[i];
		}
		*/

		IPartition oldPartition=getInitialPartition();
		IPartition newPartition = new Partition(numberOfSpeciesWithoutI+1);
		setInitialPartition(newPartition);
		
		IBlock oldBlock = oldPartition.getFirstBlock();
		while(oldBlock!=null) {
			IBlock newBlock = new Block();
			newPartition.add(newBlock);
			for (ISpecies species : oldBlock.getSpecies()) {
				newBlock.addSpecies(species);
			}
			oldBlock=oldBlock.getNext();
		}
		
		String speciesName = Species.I_SPECIESNAME;//"I";
		int id = numberOfSpeciesWithoutI;
		ISpecies speciesI = new Species(speciesName, id, BigDecimal.ONE,"1.0",false);
		
		getCRN().addSpecies(speciesI);
		//speciesIdToSpecies[speciesIdToSpecies.length-1]=speciesI;
		
		IBlock blockOfI = new Block();
		newPartition.add(blockOfI);
		blockOfI.addSpecies(speciesI);
		
		//return speciesIdToSpecies;
	}
	
	private void expandPartitionWithInputs(Collection<ISpecies> inputSpecies) {
		
		IPartition oldPartition=getInitialPartition();
		IPartition newPartition = new Partition(getCRN().getSpeciesSize());
		setInitialPartition(newPartition);

		IBlock oldBlock = oldPartition.getFirstBlock();
		while(oldBlock!=null) {
			IBlock newBlock = new Block();
			newPartition.add(newBlock);
			for (ISpecies species : oldBlock.getSpecies()) {
				newBlock.addSpecies(species);
			}
			oldBlock=oldBlock.getNext();
		}
		IBlock inputBlock = new Block();
		newPartition.add(inputBlock);
		for(ISpecies input : inputSpecies) {
			inputBlock.addSpecies(input);
		}
	}

	/**
	 * It is assumed that the id of a species corresponds to the order with which it has been inserted: the i_th inserted species has id "i". We have to decrease the id by one because we start to count from 0 
	 * @param entries
	 * @param speciesIdToSpecies 
	 * @param row 
	 * @throws IOException
	 */
	private void loadReactionLinearSystemAX(int row, int col, BigDecimal val, String valExpr/*,ISpecies[] speciesIdToSpecies*/, BigDecimal[] outgoingRates) throws IOException {
		//CORRECT:	An entry A_{row,col} represents 
		//"\dot{x_{row}} = A_{row,col} * x_{col}" =CRN=> 
		//x_{col} -{A_{row,col}}-> x_{col} + x_{row}
		//1882,1882,1882
		//2,1,1.000000

		loadReactionLinearSystemAX(row, col, val, valExpr, outgoingRates,getCRN(),normalize);
//		IComposite compositeReagents = (IComposite)(getCRN().getSpecies().get(col));//speciesIdToSpecies[col];
////		if(CRNReducerCommandLine.univoqueReagents){
////			compositeReagents = getCRN().addReagentsIfNew(compositeReagents);
////		}
//
//		IComposite compositeProducts = new Composite(getCRN().getSpecies()/*speciesIdToSpecies*/,col,row);
////		if(CRNReducerCommandLine.univoqueProducts){
////			compositeProducts = getCRN().addProductIfNew(compositeProducts);
////		}
//
//		ICRNReaction reaction = new CRNReaction(val, compositeReagents, compositeProducts, valExpr,null);
//		loadReaction(reaction, compositeReagents, compositeProducts);
//
//		if(outgoingRates!=null && normalize){
//			if(outgoingRates[col]==null){
//				outgoingRates[col]=val;
//			}
//			else{
//				outgoingRates[col]=outgoingRates[col].add(val);
//			}
//		}

	}
	
	public static void loadReactionLinearSystemAX(int row, int col, BigDecimal val, String valExpr/*,ISpecies[] speciesIdToSpecies*/, BigDecimal[] outgoingRates,
			ICRN crn, boolean normalize) throws IOException {
		//CORRECT:	An entry A_{row,col} represents 
		//"\dot{x_{row}} = A_{row,col} * x_{col}" =CRN=> 
		//x_{col} -{A_{row,col}}-> x_{col} + x_{row}
		//1882,1882,1882
		//2,1,1.000000

		IComposite compositeReagents = (IComposite)(crn.getSpecies().get(col));//speciesIdToSpecies[col];
//		if(CRNReducerCommandLine.univoqueReagents){
//			compositeReagents = getCRN().addReagentsIfNew(compositeReagents);
//		}

		IComposite compositeProducts = new Composite(crn.getSpecies()/*speciesIdToSpecies*/,col,row);
//		if(CRNReducerCommandLine.univoqueProducts){
//			compositeProducts = getCRN().addProductIfNew(compositeProducts);
//		}

		ICRNReaction reaction = new CRNReaction(val, compositeReagents, compositeProducts, valExpr,null);
		loadReaction(reaction, compositeReagents, compositeProducts,crn);

		if(outgoingRates!=null && normalize){
			if(outgoingRates[col]==null){
				outgoingRates[col]=val;
			}
			else{
				outgoingRates[col]=outgoingRates[col].add(val);
			}
		}

	}

	private void loadReactionLinearSystemAXFromEntryOfB(int row, BigDecimal val, String valExpr, BigDecimal[] outgoingRates, 
			ISpecies constantSpeciesIOrInput) throws IOException {

		//CORRECT:	An entry b_{row} represents "\dot{x_{row}} = b_{row}" =CRN=> I -{b_{row}}-> I + x_{row}
		//CORRECT:	An entry A_{row,col} represents "\dot{x_{row}} = A_{row,col} x_{col}" =CRN=> x_{col} -{A_{row,col}}-> x_{col} + x_{row}


		IComposite compositeReagents = (IComposite)constantSpeciesIOrInput;
//		if(CRNReducerCommandLine.univoqueReagents){
//			compositeReagents = getCRN().addReagentsIfNew(compositeReagents);
//		}

		IComposite compositeProducts = new Composite(getCRN().getSpecies(),constantSpeciesIOrInput.getID(),row);
//		if(CRNReducerCommandLine.univoqueProducts){
//			compositeProducts = getCRN().addProductIfNew(compositeProducts);
//		}

		ICRNReaction reaction = new CRNReaction(val, compositeReagents, compositeProducts, valExpr,null);
		loadReaction(reaction, compositeReagents, compositeProducts,getCRN());

	}

	private void loadReactionMarkovChainPQ(int row, int col, BigDecimal val, String valExpr,/*ISpecies[] speciesIdToSpecies,*/ BigDecimal[] outgoingRates) throws IOException {

		//CORRECT:	An entry A_{row,col} represents "\dot{x_{col}} = A_{row,col} x_{row}" =CRN=> x_{row} -{A_{row,col}}-> x_{col} + x_{row}

		IComposite compositeReagents = (IComposite)getCRN().getSpecies().get(row);//speciesIdToSpecies[row];
//		if(CRNReducerCommandLine.univoqueReagents){
//			compositeReagents = getCRN().addReagentsIfNew(compositeReagents);
//		}

		IComposite compositeProducts = new Composite(getCRN().getSpecies(),col,row);
//		if(CRNReducerCommandLine.univoqueProducts){
//			compositeProducts = getCRN().addProductIfNew(compositeProducts);
//		}

		ICRNReaction reaction = new CRNReaction(val, compositeReagents, compositeProducts, valExpr,null);
		loadReaction(reaction, compositeReagents, compositeProducts,getCRN());

		if(outgoingRates!=null && normalize){
			if(outgoingRates[col]==null){
				outgoingRates[col]=val;
			}
			else{
				outgoingRates[col]=outgoingRates[col].add(val);
			}
		}

	}

	/**
	 * I am computing D^{-1}*b, with D the diagonal of the matrix A. 
	 * This method creates a reaction I -D^{-1}_{row}*b_{row}-> X_{row} + I
	 * @param row the current row
	 * @param Drow the entry in position {row,row} of A
	 * @param Brow the entry in position {row} of B
	 * @param speciesIdToSpecies
	 * @throws IOException
	 */
	private void loadReactionDiagonalJacobi(int row, BigDecimal Drow, BigDecimal Brow/*,  ISpecies[] speciesIdToSpecies*/, int idOfI) throws IOException {

		//An entry A_{row,col} represents "\dot{x_{row}} = A_{row,col} x_{col}" =CRN=> x_{col} -{A_{row,col}}-> x_{col} + x_{row}

		//ISpecies I = speciesIdToSpecies[idOfI];
		ISpecies I = getCRN().getSpecies().get(idOfI);

		IComposite compositeReagents = (IComposite)I;
//		if(CRNReducerCommandLine.univoqueReagents){
//			compositeReagents = getCRN().addReagentsIfNew(compositeReagents);
//		}

		//IComposite compositeProducts = new Composite(speciesIdToSpecies,idOfI,row);
		IComposite compositeProducts = new Composite(getCRN().getSpecies(),idOfI,row);
//		if(CRNReducerCommandLine.univoqueProducts){
//			compositeProducts = getCRN().addProductIfNew(compositeProducts);
//		}

		BigDecimal rate = BigDecimal.ONE.divide(Drow,mc);
		rate = rate.multiply(Brow);
		ICRNReaction reaction = new CRNReaction(rate, compositeReagents, compositeProducts, rate.toPlainString(),null);
		loadReaction(reaction, compositeReagents, compositeProducts,getCRN());
	}

	private static void loadReaction(ICRNReaction reaction, IComposite compositeReagents, IComposite compositeProducts, ICRN crn){
		crn.addReaction(reaction);
		/*
		addToIncomingReactionsOfProducts(1,compositeProducts, reaction,CRNReducerCommandLine.addReactionToComposites);
		addToOutgoingReactionsOfReagents(1, compositeReagents, reaction,CRNReducerCommandLine.addReactionToComposites);
		if(CRNReducerCommandLine.hasToAddToReactionsWithNonZeroStoichiometry){
			addToReactionsWithNonZeroStoichiometry(1, reaction.computeProductsMinusReagentsHashMap(),reaction);
		}
		*/
	}

	
	

	public static void printToCSVFIle(ICRN crn, String name, Collection<String> preambleCommentLines, boolean verbose, MessageConsoleStream out,BufferedWriter bwOut){
		String fileName = name;

		fileName=overwriteExtensionIfEnabled(fileName,"",true);
		String fileNameIC=fileName+"IC.csv";
		//String fileNamePart=fileName+"Part.csv";
		fileName=fileName+"A.csv";
		if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Writing model in file "+fileName);
		}


		createParentDirectories(fileName);	
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printToCSVFIle, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}

		try {

			if(preambleCommentLines!=null && preambleCommentLines.size()>0){
				for (String comment : preambleCommentLines) {
					bw.write("#"+comment+"\n");
				}

			}
			//bw.write("\n\n");		
			//First line with number of species/variables
			bw.write(crn.getSpecies().size()+","+crn.getSpecies().size()+","+crn.getSpecies().size());
			bw.write("\n");

			//An entry A_{row,col} represents "\dot{x_{row}} = A_{row,col} x_{col}" =CRN=> x_{col} -{A_{row,col}}-> x_{col} + x_{row}
			//Hence, each reaction must have form:
			//	x_{col} -{A_{row,col}}-> x_{col} + x_{row}

			if(!crn.isElementary()){
				if(!crn.isElementary()){
					CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it is not an elementary CRN. I terminate.");
					return;
				}
			}

			for (ICRNReaction reaction : crn.getReactions()) {
				//writeReaction(bw, reaction);
				if(!reaction.isElementary()){
					CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because it is not an elementary CRN. I terminate.");
					return;
				}
				if(!reaction.getReagents().isUnary()){
					CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because its reaction do not have form 'c -A_{r,c}-> c + r' (e.g.,"+reaction.toString()+").   I terminate.");
					return;
				}
				if(!reaction.getProducts().isBinary()){
					CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because its reaction do not have form 'c -A_{r,c}-> c + r' (e.g.,"+reaction.toString()+").   I terminate.");
					return;
				}
				if(!reaction.getProducts().contains(reaction.getReagents().getFirstReagent())){
					CRNReducerCommandLine.printWarning(out,bwOut,"The model is not supported because its reaction do not have form 'c -A_{r,c}-> c + r' (e.g.,"+reaction.toString()+").   I terminate.");
					return;
				}
				
				
				//CORRECT:	An entry A_{row,col} represents "\dot{x_{row}} = A_{row,col} x_{col}" =CRN=> x_{col} -{A_{row,col}}-> x_{col} + x_{row}
				//1882,1882,1882
				//2,1,1.000000

				/*IComposite compositeReagents = (IComposite)speciesIdToSpecies[col];
				if(CRNReducerCommandLine.univoqueReagents){
					compositeReagents = getCRN().addReagentsIfNew(compositeReagents);
				}

				IComposite compositeProducts = new Composite(speciesIdToSpecies,col,row);
				if(CRNReducerCommandLine.univoqueProducts){
					compositeProducts = getCRN().addProductIfNew(compositeProducts);
				}*/
				
				
				
				//c -A_{r,c}-> c + r
				//r,c,A_{r,c}
				//2,1,1.000000
				ISpecies c = reaction.getReagents().getFirstReagent();
				ISpecies r = reaction.getProducts().getFirstReagent();
				String Arc = ((CRNReaction)reaction).getRate().toPlainString();
				if(r.equals(c)){
					r = reaction.getProducts().getSecondReagent();
				}
				//Note that c and r could be the same. But it is fine

				bw.write((r.getID()+1)+","+(c.getID()+1)+","+Arc+"\n");
			}			
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printToCSVFIle, exception raised while writing in the file: "+fileName);
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
				CRNReducerCommandLine.println(out,bwOut,"Problems in printToCSVFIle, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}

		BufferedWriter bwIC;
		try {
			bwIC = new BufferedWriter(new FileWriter(fileNameIC));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printToCSVFIle, exception raised while creating the filewriter for file: "+fileNameIC);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		try{
			for(ISpecies species : crn.getSpecies()){
				bwIC.write(species.getInitialConcentration().toPlainString()+"\n");
			}
		}catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printToCSVFIle, exception raised while writing in the file: "+fileNameIC);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		finally{
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"Writing model in file "+fileNameIC+" completed");
			}
			try {
				bwIC.flush();
				bwIC.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in printToCSVFIle, exception raised while closing the bufferedwriter of the file: "+fileNameIC);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}
		
		/*
		BufferedWriter bwPart;
		try {
			bwPart = new BufferedWriter(new FileWriter(fileNamePart));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printToCSVFIle, exception raised while creating the filewriter for file: "+fileNamePart);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		try{
			for(ISpecies species : crn.getSpecies()){
				Collection<ISpecies> coll = species.getRepresentedEquivalenceClass();
				if(coll!=null && !coll.isEmpty()) {
					Iterator<ISpecies> it = coll.iterator();	
					while(it.hasNext()) {
						ISpecies representedSpecies = it.next();
						int entryInMatrix=representedSpecies.getID()+1;
						bwPart.write(""+entryInMatrix);
						if(it.hasNext()) {
							bwPart.write(",");
						}
					}
					bwPart.write("\n");
				}
				else {
					bwPart.write("none\n");
				}
					
			}
		}catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printToCSVFIle, exception raised while writing in the file: "+fileNamePart);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		finally{
			if(verbose){
				CRNReducerCommandLine.println(out,bwOut,"Writing model in file "+fileNamePart+" completed");
			}
			try {
				bwPart.flush();
				bwPart.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in printToCSVFIle, exception raised while closing the bufferedwriter of the file: "+fileNamePart);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}
		*/
	}



}
