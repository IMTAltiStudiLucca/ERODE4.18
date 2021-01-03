package it.imt.erode.importing.konect;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.crn.implementations.CRNReaction;
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
 * This class is used to import symmetric Konect networks.
 * I assume that for every edge 'i j' there is a corresponding 'j i' (when adding reagents and products).
 * 	If ensureUndirectedGraph is true, I explicitly guarantee it 
 */
public class KonectNetworksImporterImportandoLaStrutturaDelGrafoENonAffineSystem extends AbstractImporter{

	private static final String zero = "0";
	private static final String one = "1";

	public KonectNetworksImporterImportandoLaStrutturaDelGrafoENonAffineSystem(String fileName, MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower) {
		super(fileName,out,bwOut,msgDialogShower);
	}
	
	public InfoCRNImporting importKonectNetwork(boolean printInfo, boolean printCRN,boolean print,boolean ensureUndirectedGraph) throws FileNotFoundException, IOException{
		
		if(print){
			if(ensureUndirectedGraph) {
				CRNReducerCommandLine.println(out,bwOut,"\nImporting (ensuring an undirected graph): "+getFileName());
			}
			else {
				CRNReducerCommandLine.println(out,bwOut,"\nImporting (without ensuring an undirected graph): "+getFileName());
			}
			
		}
		
		initInfoImporting();
		initCRNAndMath();
		ICRN crn = getCRN();
		getInfoImporting().setLoadedCRN(true);
				
		BufferedReader br = getBufferedReader();
				
		HashMap<String, ISpecies> nameToSpecies = new HashMap<>();
		long begin = System.currentTimeMillis();
		if(ensureUndirectedGraph) {
			addReactionsEnsuringUndirectedGraph(br, nameToSpecies, begin);
		}
		else {
			addReactionsWithoutEnsuringUndirectedGraph(br, nameToSpecies, begin);
		}
		
		br.close();
		long end = System.currentTimeMillis();
//		if(print&&printInfo){
//			CRNReducerCommandLine.println(out,bwOut,"Completed: "+getCRN().getSpecies().size()+" species and "+getCRN().getReactions().size()+" reactions loaded. Time necessary: "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-begin)/1000.0) )+ " (s)");
//		}
		
		//Collapsing
		//Collections.sort(reducedReactions, new ICRNReactionComparatorToCollapse());
		

		long beginPartition = System.currentTimeMillis();
		if(print){
			CRNReducerCommandLine.print(out,bwOut,"\tImporting completed.\n\tCreating the trivial initial partition with one block only ... ");
		}
		IBlock uniqueBlock = new Block();
		setInitialPartition(new Partition(uniqueBlock,getCRN().getSpecies().size()));
		for (ISpecies species : getCRN().getSpecies()) {
			uniqueBlock.addSpecies(species);
		}
		end = System.currentTimeMillis();
		if(print&&printInfo){
			CRNReducerCommandLine.println(out,bwOut,"completed in "+" "+String.format( CRNReducerCommandLine.MSFORMAT, ((end-beginPartition)/1000.0) )+ " (s)");
		}
		
		getInfoImporting().setReadSpecies(getCRN().getSpecies().size());
		getInfoImporting().setReadParameters(getCRN().getParameters().size());
		getInfoImporting().setReadCRNReactions(getCRN().getReactions().size());
		//getInfoImporting().setReadReagents(getCRN().getReagents().size());
		//getInfoImporting().setReadProducts(getCRN().getProducts().size());
		getInfoImporting().setRequiredMS(end-begin);
		
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

	private void addReactionsEnsuringUndirectedGraph(BufferedReader br, HashMap<String, ISpecies> nameToSpecies,long begin) throws IOException {
		String line;
		int r=0;
		int ignoredReactions = 0;
		HashSet<PairOfStrings> existingEdges = new LinkedHashSet<PairOfStrings>();
		while ((line = br.readLine()) != null) {
			line=line.trim();
			
			//Any other line is ignored (including comments)
			if(line.equals("")||line.startsWith("%")){
				//if this is a comment line, skip to the next iteration of the loop (i.e. to the next line)
				continue;
			}
			else {
				String[] tokens = line.split("\\s");
				PairOfStrings newPair = new PairOfStrings(tokens[0], tokens[1]);
				boolean added = existingEdges.add(newPair);
				if(!added) {
					//do nothing because we have already added both directions
					ignoredReactions++;
				}
				else {
					ISpecies sourceSpecies = getOrAddSpecies(tokens[0],nameToSpecies);
					ISpecies targetSpecies = getOrAddSpecies(tokens[1],nameToSpecies);
					ICRNReaction reaction = null;
					ICRNReaction reverseReaction = null;
					if(tokens.length==2) {
						reaction = new CRNReaction(BigDecimal.ONE, (IComposite)sourceSpecies, (IComposite)targetSpecies, one,null);
						reverseReaction = new CRNReaction(BigDecimal.ONE, (IComposite)targetSpecies, (IComposite)sourceSpecies, one,null);
					}
					else {
						reaction = new CRNReaction(new BigDecimal(tokens[2]), (IComposite)sourceSpecies, (IComposite)targetSpecies, tokens[2],null);
						reverseReaction = new CRNReaction(new BigDecimal(tokens[2]), (IComposite)targetSpecies, (IComposite)sourceSpecies,tokens[2],null);
					}
					getCRN().addReaction(reaction);
					getCRN().addReaction(reverseReaction);
				}
				r+=2;
				if(r%500000==0){
					CRNReducerCommandLine.println(out,bwOut,"\t"+r + " reactions loaded after "+(System.currentTimeMillis()-begin)/1000.0+ " seconds");
				}
			}
		}
		CRNReducerCommandLine.println(out,bwOut,"\t"+ignoredReactions + " reactions ignored because already added as reversed.");
	}
	
	private void addReactionsWithoutEnsuringUndirectedGraph(BufferedReader br, HashMap<String, ISpecies> nameToSpecies,
			long begin) throws IOException {
		String line;
		int r=0;
		while ((line = br.readLine()) != null) {
			line=line.trim();
			
			//Any other line is ignored (including comments)
			if(line.equals("")||line.startsWith("%")){
				//if this is a comment line, skip to the next iteration of the loop (i.e. to the next line)
				continue;
			}
			else {
				String[] tokens = line.split("\\s");
				ISpecies sourceSpecies = getOrAddSpecies(tokens[0],nameToSpecies);
				ISpecies targetSpecies = getOrAddSpecies(tokens[1],nameToSpecies);
				ICRNReaction reaction = null;
				if(tokens.length==2) {
					reaction = new CRNReaction(BigDecimal.ONE, (IComposite)sourceSpecies, (IComposite)targetSpecies, one,null);
				}
				else {
					reaction = new CRNReaction(new BigDecimal(tokens[2]), (IComposite)sourceSpecies, (IComposite)targetSpecies, tokens[2],null);
				}
				getCRN().addReaction(reaction);
			}
			
			r++;
			if(r%500000==0){
				CRNReducerCommandLine.println(out,bwOut,"\t"+r + " reactions loaded after "+(System.currentTimeMillis()-begin)/1000.0+ " seconds");
			}
		}
	}

	private ISpecies getOrAddSpecies(String nodeId, HashMap<String, ISpecies> nameToSpecies) {
		ISpecies species = nameToSpecies.get(nodeId);
		if(species==null) {
			species = new Species("N"+nodeId, null, getCRN().getSpecies().size(), BigDecimal.ZERO,zero,false,false);
			getCRN().addSpecies(species);
			nameToSpecies.put(nodeId, species);
			//Every species is used as reagents and as products. When I create a new species, I add it to the set of reagents and products.
//			if(CRNReducerCommandLine.univoqueReagents){
//				//getCRN().addReagent((IComposite)species);
//				getCRN().addReagentsIfNew((IComposite)species);
//			}
//			if(CRNReducerCommandLine.univoqueProducts){
//				//getCRN().addProduct((IComposite)species);
//				getCRN().addProductIfNew((IComposite)species);
//			}
			
		}
		return species;
	}

}
