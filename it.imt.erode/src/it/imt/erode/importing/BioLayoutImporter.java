package it.imt.erode.importing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.crn.implementations.CRN;
import it.imt.erode.crn.implementations.CRNReaction;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.partitionrefinement.algorithms.CRNBisimulationsNAry;


/**
 * 
 * @author Andrea Vandin
 * This class is used to import state transition graphs encoded in BioLayout formates (extension: layout). 
 * These are just graphs. We transform them in DTMCs by setting to 1/{number of outgoing transitions from s) all transitions ougoing from s. 
 */
public class BioLayoutImporter extends AbstractImporter{

	private boolean normalize=true;
	
	public BioLayoutImporter(String fileName,MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower) {
		super(fileName,out,bwOut,msgDialogShower);
	}

	public InfoCRNImporting importBioLayoutSTG(boolean printInfo, boolean printCRN, boolean print) throws FileNotFoundException, IOException, UnsupportedFormatException{

			if(print){
				CRNReducerCommandLine.println(out,bwOut,"\nImporting BioLayout state transition graph "+getFileName());
			}

			initInfoImporting();
			initCRNAndMath();
			ICRN crn = getCRN();
			getInfoImporting().setLoadedCRN(true);

			BufferedReader br = getBufferedReader();
			String line; 


			long begin = System.currentTimeMillis();
			HashMap<String, ISpecies> speciesNameToSpecies=new HashMap<String, ISpecies>();
			while ((line = br.readLine()) != null) {
				line=line.trim();
				//Any other line is ignored (including comments)
				if(line.equals("")||line.startsWith("#")){
					//if this is a comment line, skip to the next iteration of the loop (i.e. to the next line)
					continue;
				}
				
				//000	001
				//100	110
				StringTokenizer st = new StringTokenizer(line);
				String[] sourceAndTarget=new String[]{st.nextToken(),st.nextToken()};
				//String[] sourceAndTarget = line.split("\t");
				/*String name = sourceAndTarget[0];
				int id = getCRN().getSpecies().size();
				ISpecies source = addSpecies("s"+name, name,(id==0)? "1":"0", speciesNameToSpecies);
				name = sourceAndTarget[1];
				id = getCRN().getSpecies().size();
				ISpecies target = addSpecies("s"+name, name, "0", speciesNameToSpecies);*/
				
				HashMap<ISpecies, Integer> reagentsHM = generateNewSpeciesAndBuildArrayOfNames(speciesNameToSpecies, new String[]{"s"+sourceAndTarget[0]},crn);
				//IComposite compositeReagents = crn.addReagentsIfNew(new Composite(reagentsHM));
				IComposite compositeReagents = CRN.compositeOrSpecies(reagentsHM);
				
				HashMap<ISpecies, Integer> productsHM = generateNewSpeciesAndBuildArrayOfNames(speciesNameToSpecies, new String[]{"s"+sourceAndTarget[1]},crn);
				//IComposite compositeProducts = crn.addProductIfNew(new Composite(productsHM));
				IComposite compositeProducts = CRN.compositeOrSpecies(productsHM);
				
				BigDecimal reactionRate = BigDecimal.ONE;
				ICRNReaction reaction = new CRNReaction(reactionRate, compositeReagents, compositeProducts, "1",null);
				CRNImporter.addReaction(crn, compositeReagents, compositeProducts, reaction);
			}
			
			if(normalize){
				BigDecimal[] outgoingRates = new BigDecimal[crn.getSpecies().size()];
				for (ICRNReaction reaction : getCRN().getReactions()) {
					ISpecies reagent = reaction.getReagents().getFirstReagent();
					if(outgoingRates[reagent.getID()]==null){
						outgoingRates[reagent.getID()]=reaction.getRate();
					}
					else{
						outgoingRates[reagent.getID()]=outgoingRates[reagent.getID()].add(reaction.getRate());
					}
				}
				for (ICRNReaction reaction : getCRN().getReactions()) {
					ISpecies reagent = reaction.getReagents().getFirstReagent();
					BigDecimal totalOut = outgoingRates[reagent.getID()];
					if(totalOut.compareTo(BigDecimal.ONE)!=0){
						BigDecimal normalizedRate = reaction.getRate().divide(totalOut,CRNBisimulationsNAry.getSCALE(),CRNBisimulationsNAry.RM);
						reaction.setRate(normalizedRate, normalizedRate.toPlainString());
					}
				}
			}
			
			
			//The initial state is stored as first species. Since we start from the initial state, we set its concentration to one. All other concentrations are set to 0. 
			crn.getSpecies().get(0).setInitialConcentration(BigDecimal.ONE, "1");
			createInitialPartition();

			getInfoImporting().setReadCRNReactions(getCRN().getReactions().size());
			//getInfoImporting().setReadReagents(getCRN().getReagents().size());
			//getInfoImporting().setReadProducts(getCRN().getProducts().size());
			long end=System.currentTimeMillis();
			getInfoImporting().setRequiredMS(end -begin);

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
	
}
