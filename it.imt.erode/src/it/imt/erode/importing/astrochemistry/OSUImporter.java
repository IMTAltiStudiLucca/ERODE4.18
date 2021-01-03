package it.imt.erode.importing.astrochemistry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.crn.implementations.CRNReaction;
import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.importing.AbstractImporter;
import it.imt.erode.importing.InfoCRNImporting;
import it.imt.erode.importing.UnsupportedFormatException;

/**
 * 
 * @author Andrea Vandin
 * This class is used to import reaction networks written in the format used in https://faculty.virginia.edu/.archived/ericherb/research.html
 * The Ohio State University Astrophysical Chemistry Group - OSU 
 * The UMIST Database for Astrochemistry
 * 
 * H       H               H2                                       4.95e-17 5.00e-01 0.00e+00 0   1         *   1*  1.00
 * HCO+    GRAIN-          H       CO      GRAIN0                   3.10e-17 5.00e-01 0.00e+00 0  14         *  14*  1.00
 * C                       C+      E                                1.02e+03 0.00e+00 0.00e+00 1   1         *  15*  2.00
 * 
 */
public class OSUImporter extends AbstractImporter{

	private int linesToSkip;
	public OSUImporter(String fileNameReactions,int linesToSkip,MessageConsoleStream out, BufferedWriter bwOut, IMessageDialogShower msgDialogShower) {
		super(fileNameReactions,out,bwOut,msgDialogShower);
		this.linesToSkip=linesToSkip;
	}
	
	public InfoCRNImporting importOSUNetwork(boolean printInfo, boolean printCRN,boolean print) throws FileNotFoundException, IOException, UnsupportedFormatException{
		if(print){
			//CRNReducerCommandLine.println(out,"\nImporting the model "+ modelName +" from the editor");
			CRNReducerCommandLine.println(out,bwOut,"\nReading "+ getFileName() +"...");
		}
		
		initInfoImporting();
		initCRNAndMath();
		getInfoImporting().setLoadedCRN(true);
		
		long begin=System.currentTimeMillis();
		
		HashMap<String, ISpecies> speciesStoredInHashMap = new HashMap<String, ISpecies>();
		HashMap<String, ISpecies> originalName2Species = new HashMap<String, ISpecies>();

		
		loadReactions(speciesStoredInHashMap,originalName2Species,print);
		createInitialPartition();
		
		getInfoImporting().setReadParameters(getCRN().getParameters().size());
		getInfoImporting().setReadSpecies(getCRN().getSpecies().size());
		getInfoImporting().setReadCRNReactions(getCRN().getReactions().size());
		getInfoImporting().setRequiredMS(System.currentTimeMillis()-begin);
		
		if(printInfo&&print){
			CRNReducerCommandLine.println(out,bwOut,getInfoImporting().toString());
		}
		if(printCRN&&print){
			getCRN().printCRN();
		}
		return getInfoImporting();
	}

	private void loadReactions(HashMap<String, ISpecies> speciesStoredInHashMap,
			HashMap<String, ISpecies> originalName2Species, boolean print) throws IOException, UnsupportedFormatException {
//		if(print){
//			CRNReducerCommandLine.print(out,bwOut,"\tImporting the reactions ... ");
//		}
		
		BufferedReader br = getBufferedReader();
		String line;
		int r=0;
		int l=1;
		boolean stop=false;
		while ((line = br.readLine()) != null && !stop) {
			if(l<=linesToSkip) {
				//do nothing
			}
			else {
				if(line.startsWith(" ")) {
					CRNReducerCommandLine.println(out,bwOut,"\tI ignore from line "+(l+1) +" onwards because it starts with an empty space. ");
					stop=true;
				}
				else {
					line=line.trim();
					parseAndAddReaction(line,r,speciesStoredInHashMap,originalName2Species,l);
					r++;
				}
				
			}
			l++;
		}
	}

	private void parseAndAddReaction(String line, int rNumber,HashMap<String, ISpecies> speciesStoredInHashMap,
			HashMap<String, ISpecies> originalName2Species, int l) throws UnsupportedFormatException {
/*
H       H               H2                                       4.95e-17 5.00e-01 0.00e+00 0   1         *   1*  1.00
HCO+    GRAIN-          H       CO      GRAIN0                   3.10e-17 5.00e-01 0.00e+00 0  14         *  14*  1.00
C                       C+      E                                1.02e+03 0.00e+00 0.00e+00 1   1         *  15*  2.00

H       H        GRAIN0 H2       GRAIN0                          4.95e-17 5.00e-01 0.00e+00 0   1         *   1*  1.00
*/		
		//I MADE A MISTAKE. THERE CAN BE 3 REAGENTS!
		String reag1 = line.substring(0, 7).trim();
		String reag2 = line.substring(8, 16).trim();
		String reag3 = line.substring(17, 23).trim();
		ArrayList<ISpecies> reagents = new ArrayList<>();
		if(reag1!=null && reag1.length()>0) {
			reagents.add(getAndAddSpeciesIfNecessary(reag1, speciesStoredInHashMap, originalName2Species));
		}
		if(reag2!=null && reag2.length()>0) {
			reagents.add(getAndAddSpeciesIfNecessary(reag2, speciesStoredInHashMap, originalName2Species));
		}
		if(reag3!=null && reag3.length()>0) {
			reagents.add(getAndAddSpeciesIfNecessary(reag3, speciesStoredInHashMap, originalName2Species));
		}
		
		IComposite compositeReagents = (IComposite)reagents.get(0);
		if(reagents.size()>1) {
			compositeReagents = new Composite(reagents);
		}
		
		ArrayList<ISpecies> products = new ArrayList<>();
		String prod1 = line.substring(24, 31).trim();
		String prod2 = line.substring(32, 39).trim();
		String prod3 = line.substring(40, 47).trim();
		String prod4 = line.substring(48, 64).trim();
		if(prod4.indexOf(' ')!=-1) {
			throw new UnsupportedFormatException("A species name with a space in line "+l+": "+prod4);
		}
		if(prod1!=null && prod1.length()>0) {
			products.add(getAndAddSpeciesIfNecessary(prod1, speciesStoredInHashMap, originalName2Species));
		}
		if(prod2!=null && prod2.length()>0) {
			products.add(getAndAddSpeciesIfNecessary(prod2, speciesStoredInHashMap, originalName2Species));
		}
		if(prod3!=null && prod3.length()>0) {
			products.add(getAndAddSpeciesIfNecessary(prod3, speciesStoredInHashMap, originalName2Species));
		}
		if(prod4!=null && prod4.length()>0) {
			products.add(getAndAddSpeciesIfNecessary(prod4, speciesStoredInHashMap, originalName2Species));
		}
		IComposite compositeProducts = (IComposite)products.get(0);
		if(products.size()>1) {
			compositeProducts = new Composite(products);
		}
		
		String par ="k"+rNumber;
		addParameter(par, "1");
		
		ICRNReaction reaction = new CRNReaction(BigDecimal.ONE, compositeReagents, compositeProducts, par, "R"+rNumber);
		getCRN().addReaction(reaction);
	}
	
	private ISpecies getAndAddSpeciesIfNecessary(String originalName, HashMap<String, ISpecies> speciesStoredInHashMap,
			HashMap<String, ISpecies> originalName2Species) {
		ISpecies sp = originalName2Species.get(originalName);
		if(sp==null) {
			sp=addSpecies("s"+getCRN().getSpeciesSize(), originalName, "1",speciesStoredInHashMap);
			originalName2Species.put(originalName, sp);
		}
		return sp;
	}
	
}
