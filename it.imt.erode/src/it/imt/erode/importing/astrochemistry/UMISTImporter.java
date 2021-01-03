package it.imt.erode.importing.astrochemistry;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.eclipse.ui.console.MessageConsoleStream;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.DialogType;
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
 * This class is used to import reaction networks written in the format used in http://udfa.ajmarkwick.net/index.php?mode=downloads
 * The UMIST Database for Astrochemistry
 * 
 * Q1: Do I need to add the special default species PHOTON, CRP and CRPHOT?
 * 
 */
public class UMISTImporter extends AbstractImporter{

	private String fileNameSpecies;
	
	public UMISTImporter(String fileNameReactions,String fileNameSpecies,MessageConsoleStream out, BufferedWriter bwOut, IMessageDialogShower msgDialogShower) {
		super(fileNameReactions,out,bwOut,msgDialogShower);
		this.fileNameSpecies=fileNameSpecies;
	}
	
	public InfoCRNImporting importUMISTNetwork(boolean printInfo, boolean printCRN,boolean print) throws FileNotFoundException, IOException, UnsupportedFormatException{
		if(print){
			//CRNReducerCommandLine.println(out,"\nImporting the model "+ modelName +" from the editor");
			CRNReducerCommandLine.println(out,bwOut,"\nReading "+ getFileName() +"...");
		}
		
		initInfoImporting();
		initCRNAndMath();
		getInfoImporting().setLoadedCRN(true);
		
		long begin=System.currentTimeMillis();
		
		//HashMap<String, ISpecies> speciesStoredInHashMap = new HashMap<String, ISpecies>();
		HashMap<String, ISpecies> originalName2Species = new HashMap<String, ISpecies>();
		boolean succeed = loadSpecies(print,originalName2Species);
		if(!succeed){
			throw new UnsupportedFormatException("Problems in loading the species from  "+ fileNameSpecies);
		}
		
		loadReactions(originalName2Species,print);
		
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

	private boolean loadSpecies(boolean print, HashMap<String, ISpecies> originalName2Species) throws IOException{
		//1 H          5.00e-05    1.0
		/*
			1 Species number
			2 Species name
			3 Initial abundance
			4 Species mass
		 */
		if(print){
			CRNReducerCommandLine.print(out,bwOut,"\tImporting the species from: "+fileNameSpecies +" ... ");
		}
		BufferedReader br=null;
		try{
			br = getBufferedReader(fileNameSpecies);
		}catch(FileNotFoundException e){
			CRNReducerCommandLine.printWarning(out,bwOut,true,msgDialogShower,"File with species not provided.",DialogType.Error);
			return false;
		}

		String line;
		boolean stop=false;

		int zero=1;
		while ((!stop)&&(line = br.readLine()) != null) {
			line=line.trim();
			StringTokenizer st = new StringTokenizer(line);
			String id = st.nextToken();
			String originalName = st.nextToken();
			String initAbundance = st.nextToken();
			//String speciesMass = st.nextToken();
			if(id.equals("0")) {
				id="0_"+zero;
				zero++;
			}
			ISpecies s = addSpecies("s"+id, originalName, initAbundance);
			originalName2Species.put(originalName, s);
		}
		//Add default species
		String []defaultSpecies = {"PHOTON", "CRP", "CRPHOT"};
		for(int i=0;i<defaultSpecies.length;i++) {
			String id="0_"+zero;
			zero++;
			ISpecies s = addSpecies("s"+id, defaultSpecies[i], "1");
			originalName2Species.put(defaultSpecies[i], s);
		}
		
		
		if(print){
			CRNReducerCommandLine.println(out,bwOut,"completed.");
		}
		return true;

	}

	
	private void loadReactions(HashMap<String, ISpecies> originalName2Species,boolean print) throws IOException{
		//1:AD:C-:C:C2:e-:::1:5.00e-10:0.00:0.0:10:41000:L:C:"10.1086/190665":"rate06_blank_refs_AD.notes":
		/*
			1 Reaction number
			2 Reaction type
			3 Reactant 1
			4 Reactant 2
			5 Product 1
			6 Product 2
			7 Product 3
			8 Product 4
			9 No. of T fields
			10 alpha
			11 beta
			12 gamma
			13 Minimum temperature
			14 Maximum temperature
			15 Source type
			16 Accuracy
			17 Reference
		 */
		
		if(print){
			CRNReducerCommandLine.print(out,bwOut,"\tImporting the reactions ... ");
		}
		
		BufferedReader br = getBufferedReader();
		String line;
		while ((line = br.readLine()) != null) {
			line=line.trim();
			//String reaction="1:AD:C-:C:C2:e-:::1:5.00e-10:0.00:0.0:10:41000:L:C:\"10.1086/190665\":\"rate06_blank_refs_AD.notes\":";
			String[] splitted = line.split(":");
			String ReactionNumber =splitted[1-1]; //1 Reaction number
//			if(ReactionNumber.equals("6042")||ReactionNumber.equals("5706")) {
//				System.out.println(ReactionNumber);
//				//6042:RA:C+:C10:C11+:PHOTON:::1:1.00e-09:0.00:0.0:10:41000:L:C:"OSU09":"A06_8027NS.notes":
//			}
			String ReactionType=splitted[2-1]; //2 Reaction type
			String Reactant1= splitted[3-1]; //3 Reactant 1
			String Reactant2= splitted[4-1]; //4 Reactant 2
			IComposite compositeReagents = (IComposite)originalName2Species.get(Reactant1);
			if(Reactant2!=null && Reactant2.length()>0) {
				ISpecies reag2 = originalName2Species.get(Reactant2);
				compositeReagents=new Composite(compositeReagents.getFirstReagent(), reag2);
			}
			//String Product1= splitted[5-1]; //5 Product 1
			//String Product2= splitted[6-1]; //6 Product 2
			//String Product3= splitted[7-1]; //7 Product 3
			//String Product4= splitted[8-1]; //8 Product 4
			HashMap<ISpecies, Integer> productsHM = new HashMap<ISpecies, Integer>();
			for(int i=5;i<=8;i++) {
				if(splitted[i-1]!=null && splitted[i-1].length()>0) {
					String originalName = splitted[i-1];
					ISpecies prod = originalName2Species.get(originalName);
					Integer prev = productsHM.get(prod);
					productsHM.put(prod, 1 + ((prev==null)?0:prev));
				}
			}
			IComposite compositeProducts = new Composite(productsHM);
			//String NTfields= splitted[9-1]; //9 No. of T fields - number of temperature fields
			//String alpha= splitted[10-1]; //10 alpha
			//String beta= splitted[11-1]; //11 beta
			//String gamma= splitted[12-1]; //12 gamma
			//String minTemp= splitted[13-1]; //13 Minimum temperature
			//String maxTemp= splitted[14-1]; //14 Maximum temperature
			//String sourceType= splitted[15-1]; //15 Source type
			//String Accuracy= splitted[16-1]; //16 Accuracy
			//String Reference= splitted[17-1]; //17 Reference
			//9 gives you the number of temperature fields. 
			//	10-17 repeat the appropriate number of times
			//10-12 are the ones actually used to compute the rates 
			//	depending on 2
			
			//For now I set all the parametersTo1
			String par ="k"+ReactionType+ReactionNumber;
			//getCRN().addParameter(par, "1");
			addParameter(par, "1");
			
			ICRNReaction reaction = new CRNReaction(BigDecimal.ONE, compositeReagents, compositeProducts, par, "R"+ReactionNumber);
			getCRN().addReaction(reaction);
		}
		if(print){
			CRNReducerCommandLine.println(out,bwOut,"completed.");
		}
	}

	

//	public static void loadReactions(){
//		//1:AD:C-:C:C2:e-:::1:5.00e-10:0.00:0.0:10:41000:L:C:"10.1086/190665":"rate06_blank_refs_AD.notes":
//		/*
//			1 Reaction number
//			2 Reaction type
//			3 Reactant 1
//			4 Reactant 2
//			5 Product 1
//			6 Product 2
//			7 Product 3
//			8 Product 4
//			9 No. of T fields
//			10 alpha
//			11 beta
//			12 gamma
//			13 Minimum temperature
//			14 Maximum temperature
//			15 Source type
//			16 Accuracy
//			17 Reference
//		 */
//		String reaction="1:AD:C-:C:C2:e-:::1:5.00e-10:0.00:0.0:10:41000:L:C:\"10.1086/190665\":\"rate06_blank_refs_AD.notes\":";
//		String[] splitted = reaction.split(":");
//		for(int s=0;s<splitted.length;s++) {
//			System.out.println(s+"["+splitted[s]+"]");
//		}
//		String ReactionNumber =splitted[1-1]; //1 Reaction number 
//		String ReactionType=splitted[2-1]; //2 Reaction type
//		String Reactant1= splitted[3-1]; //3 Reactant 1
//		String Reactant2= splitted[4-1]; //4 Reactant 2
//		String Product1= splitted[5-1]; //5 Product 1
//		String Product2= splitted[6-1]; //6 Product 2
//		String Product3= splitted[7-1]; //7 Product 3
//		String Product4= splitted[8-1]; //8 Product 4
//		String NTfields= splitted[9-1]; //9 No. of T fields - number of temperature fields
//		String alpha= splitted[10-1]; //10 alpha
//		String beta= splitted[11-1]; //11 beta
//		String gamma= splitted[12-1]; //12 gamma
//		String minTemp= splitted[13-1]; //13 Minimum temperature
//		String maxTemp= splitted[14-1]; //14 Maximum temperature
//		String sourceType= splitted[15-1]; //15 Source type
//		String Accuracy= splitted[16-1]; //16 Accuracy
//		String Reference= splitted[17-1]; //17 Reference
//		//9 gives you the number of temperature fields. 
//		//	10-17 repeat the appropriate number of times
//		//10-12 are the ones actually used to compute the rates 
//		//	depending on 2
//	}
}

/*
 * For model Rate13 http://udfa.ajmarkwick.net/index.php?mode=downloads
 * -rate13_dc_code.tgz
	 dark cloud chemical model source code
	 rates computed once
 * 	-The reactions are listed in the colon-separated file ‘rate13.rates’ 
 * 	-The species in the file ‘dc.specs’. 
 * 	-By executing rate13dc.pl on rate13.rates we get the actual ODEs in file
 *  	- dcodes.f	
 * 	-The rates constants are calculated in file dcrates.f
 *  - Such fortran files are invoked by the main function 'rate13main.f'
 *  	The formulas seem quite simple (lines 56-72). ALF/BET/GAM contain an alpha/beta/gamma per reaction (10000), for each of the max 5 possible temperatures.
 * 
 *  If you execute 'make' on a terminal, you will generate the executable 'model'
 *  By executing 'model' you get
 *  - rates.txt				a more readable set of chemical reactions (not really more readable)
 *  - rate13steady.state	the initial conditions as well as the fractional abundances of all species at the last time step (10^8 years) - the steady state
 *  - dc.out 			  	main output file. a table which lists the fractional abundance (with respect to H_2 density) of each species as a function of time.
 *  
 *  As an experiment, you can run the model again, switching on the freeze-out of molecules onto the dust grains by setting IFREEZE = 1 in the main programme rate13main.f.
 *  	and there are furhter experiments described in http://udfa.ajmarkwick.net/downloads/rate13_code.pdf
 *  
 *  
 *  1:AD:C-:C:C2:e-:::1:5.00e-10:0.00:0.0:10:41000:L:C:"10.1086/190665":"rate06_blank_refs_AD.notes":
 *  1    C- C C2 e-     5.00E-10 0.00 0.0
 *  
 *  4:AD:C-:CO2:CO:CO:e-::1:4.70e-11:0.00:0.0:10:41000:L:C:"10.1086/190665":"rate06_blank_refs_IN.notes":
 *  4   C-  CO2 CO CO e-    4.70E-11 0.00 0.0
 *  
 *  64:AD:CH:O:HCO+:e-:::1:1.09e-11:-2.19:165.1:10:2500:C:C:"10.10880022-3700/6/1/020":"06_0139D.notes":
 *  64    CH O HCO+ e-     1.09E-11  -2.19     165.1
 *  
 *  133:CD:H+:HNC:HCN:H+:::1:1.00e-09:0.00:0.0:10:41000:L:C:::
 *  133   H+          HNC         HCN         H+                  1.00E-09   0.00       0.0
 *  
 *  147:CE:C+:C10H:C10H+:C:::1:5.00e-09:-0.50:0.0:10:41000:L:C:"OSU09":"A06_8027NS.notes":
 *  147   C+          C10H        C10H+       C                   5.00E-09  -0.50       0.0		
 */
