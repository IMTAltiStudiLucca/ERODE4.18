package it.imt.erode.importing.csv;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.crn.implementations.CRNReaction;
import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.implementations.Species;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.importing.AbstractImporter;
import it.imt.erode.importing.InfoCRNImporting;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;


/**
 * 
 * @author Andrea Vandin
 * This class is used to import linear systems given as matrices in .csv format.
 */
public class CSVMatrixAsLinearSystemImporter extends AbstractImporter{

 	public static final String CSVMatricesFolder = "."+File.separator+"Matrices"+File.separator;
 	//private static final boolean addReactionToComposites=false;

	public CSVMatrixAsLinearSystemImporter(String fileName,MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower) {
		super(fileName,out,bwOut,msgDialogShower);
	}
	
	public InfoCRNImporting importLinearSystemAsCSVMatrix(boolean printInfo, boolean printCRN,boolean print) throws FileNotFoundException, IOException{

		if(print){
			CRNReducerCommandLine.println(out,bwOut,"\nImporting: "+getFileName());
		}
		
		initInfoImporting();
		initCRNAndMath();
		ICRN crn = getCRN();
		getInfoImporting().setLoadedCRN(true);
				
		BufferedReader br = getBufferedReader();
		String line;

		boolean speciesGenerated = false;
		//boolean reactionsLoaded=false;
		//HashMap<Integer, ISpecies> speciesIdToSpecies=new HashMap<Integer, ISpecies>();
		ISpecies[] speciesIdToSpecies=null;

		int row=0;
		
		long beginReactions=0;
		while ((line = br.readLine()) != null) {
			line=line.trim();
			//Any other line is ignored (including comments)
			if(line.equals("")||line.startsWith("#")){
				//if this is a comment line, skip to the next iteration of the loop (i.e. to the next line)
				continue;
			}
			String[] entries = line.split(",");
			if(!speciesGenerated){
				long begin = System.currentTimeMillis();
				speciesGenerated=true;
				speciesIdToSpecies=new ISpecies[entries.length];
				for(int i=0;i<entries.length;i++){
					int id = i;
					String speciesName = "s"+id;
					ISpecies species = new Species(speciesName, id, BigDecimal.ZERO,"0.0",false);
					getCRN().addSpecies(species);
					speciesIdToSpecies[i]=species;
				}
				long end = System.currentTimeMillis();
				beginReactions=end;
				CRNReducerCommandLine.println(out,bwOut,getCRN().getSpecies().size()+" species loaded. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");
				getInfoImporting().setReadSpecies(getCRN().getSpecies().size());
			}
			
			loadReaction(entries,speciesIdToSpecies,row);
			row++;
		}
		long end=System.currentTimeMillis();
		CRNReducerCommandLine.println(out,bwOut,getCRN().getReactions().size() +" reactions loaded. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-beginReactions)/1000.0) )+ " (s)");
		getInfoImporting().setReadCRNReactions(getCRN().getReactions().size());
		//getInfoImporting().setReadReagents(getCRN().getReagents().size());
		//getInfoImporting().setReadProducts(getCRN().getProducts().size());
		
		IBlock uniqueBlock = new Block();
		setInitialPartition(new Partition(uniqueBlock,getCRN().getSpecies().size()));
		for (ISpecies species : getCRN().getSpecies()) {
			uniqueBlock.addSpecies(species);
		}
		
		br.close();
		
		if(print){
			if(printInfo){
				CRNReducerCommandLine.println(out,bwOut,getInfoImporting().toString());
			}
			if(printCRN){
				crn.printCRN();
			}
		}
		
		return getInfoImporting();
	}
	
	
	/**
	 * It is assumed that the id of a species corresponds to the order with which it has been inserted: the i_th inserted species has id "i". We have to decrease the id by one because we start to count from 0 
	 * @param entries
	 * @param speciesIdToSpecies 
	 * @param row 
	 * @throws IOException
	 */
	private void loadReaction(String[] entries, ISpecies[] speciesIdToSpecies, int row) throws IOException {
		
		//WRONG: 	An entry A_{row,col} represents "\dot{x_{col}} = A_{row,col} x_{row}" =CRN=> x_{row} -{A_{row,col}}-> x_{row} + x_{col} 
		//CORRECT:	An entry A_{row,col} represents "\dot{x_{row}} = A_{row,col} x_{col}" =CRN=> x_{col} -{A_{row,col}}-> x_{col} + x_{row}
		
		/*IComposite compositeReagents = getCRN().addReagentsIfNew((IComposite)speciesIdToSpecies[row]);
		
		for(int col=0;col<entries.length;col++){
			//compute rate of the reaction 
			String rateExpression = entries[col];
			BigDecimal reactionRate = new BigDecimal(rateExpression);
			
			if(reactionRate.compareTo(BigDecimal.ZERO)!=0){
				IComposite compositeProducts = getCRN().addProductIfNew(new Composite(speciesIdToSpecies,col,row));

				ICRNReaction reaction = new CRNReaction(reactionRate, compositeReagents, compositeProducts, rateExpression,false,false);
				getCRN().addReaction(reaction);
				addToIncomingReactionsOfProducts(1,compositeProducts, reaction,addReactionToComposites);
				addToOutgoingReactionsOfReagents(1, compositeReagents, reaction,addReactionToComposites);
			}
			
		}*/
		
		for(int col=0;col<entries.length;col++){
			//compute rate of the reaction 
			String rateExpression = entries[col];
			BigDecimal reactionRate = BigDecimal.valueOf(Double.valueOf(rateExpression));
			
			if(reactionRate.compareTo(BigDecimal.ZERO)!=0){
				//IComposite compositeReagents = getCRN().addReagentsIfNew((IComposite)speciesIdToSpecies[col]);
				IComposite compositeReagents = (IComposite)speciesIdToSpecies[col];
				//IComposite compositeProducts = getCRN().addProductIfNew(new Composite(speciesIdToSpecies,col,row));
				IComposite compositeProducts = new Composite(speciesIdToSpecies,col,row);

				ICRNReaction reaction = new CRNReaction(reactionRate, compositeReagents, compositeProducts, rateExpression,null);
				getCRN().addReaction(reaction);
				/*
				addToIncomingReactionsOfProducts(1,compositeProducts, reaction,CRNReducerCommandLine.addReactionToComposites);
				addToOutgoingReactionsOfReagents(1, compositeReagents, reaction,CRNReducerCommandLine.addReactionToComposites);
				addToReactionsWithNonZeroStoichiometry(1, reaction.computeProductsMinusReagentsHashMap(),reaction);
				*/
			}
			
		}
	}

}
