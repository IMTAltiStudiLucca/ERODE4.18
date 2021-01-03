package it.imt.erode.importing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.ui.console.MessageConsoleStream;
import org.sbml.jsbml.ASTNode;

import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.crn.implementations.CRNReactionArbitraryMatlab;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.ISpecies;

/**
 * 
 * @author Andrea Vandin
 * This class is used to import a system of ordinary differential equations written in a matlab file (file extension: .m). 
 * If it is a polynomial ODE system, then it is converted in a mass action reaction network. Otherwise we just store a reaction per ODE variable X (X -> X + X) with the drift as arbitrary rate.
 */
public class PALOMAMomentClosureImporter  extends MatlabODEsImporter {
			
	public PALOMAMomentClosureImporter(String fileName,MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower) {
		super(fileName,out,bwOut,msgDialogShower);
		
	}
	
	public InfoCRNImporting importPalomaMomentClosures(boolean printInfo, boolean printCRN,boolean print, String[] optionalParameters) throws FileNotFoundException, IOException{
		InfoCRNImporting info = importMatlabODEs(printInfo, printCRN, printCRN, false,true);
		
		/*String momentFileName = getFileName();
		
		if(momentFileName.contains(File.separator)){
			momentFileName=momentFileName.substring(0, momentFileName.lastIndexOf(File.separator)+1);
			momentFileName += "firstMoment.m";
		}
		else{
			momentFileName = "firstMoment.m";
		}
		
		importLabellingFunction(print, momentFileName);*/
		
		String momentFilePaths = getFileName();
		if(momentFilePaths.contains(File.separator)){
			momentFilePaths=momentFilePaths.substring(0, momentFilePaths.lastIndexOf(File.separator)+1);
		}
		else{
			momentFilePaths = "";
		}
		
		importMomentFiles(print, momentFilePaths);
		
		return info;
	}
	
	public void importMomentFiles(boolean print, String momentFilePaths) throws IOException{

		String momentFileName=momentFilePaths+"firstMoment.m";
		if(print){
			CRNReducerCommandLine.println(out,bwOut,"\nImporting: "+momentFileName);
		}
		
		BufferedReader br=null;
		try{
			br = getBufferedReader(momentFileName);
		}catch(FileNotFoundException e){
			CRNReducerCommandLine.printWarning(out,bwOut,"File with first moment data not provided.");
			return;
		}

		
		//[T,Y]=ode45(@fluidflow,[0 100],[ 178 142 184 150 12 0 0 13 11 10 0 11 11 0 0 0 0 0 0 0 0 0 12 12 31684 20164 33856 22500 144 0 0 169 121 100 0 121 121 0 0 0 0 0 0 0 0 0 144 144 25276 32752 26700 2136 0 0 2314 1958 1780 0 1958 1958 0 0 0 0 0 0 0 0 0 2136 2136 26128 21300 1704 0 0 1846 1562 1420 0 1562 1562 0 0 0 0 0 0 0 0 0 1704 1704 27600 2208 0 0 2392 2024 1840 0 2024 2024 0 0 0 0 0 0 0 0 0 2208 2208 1800 0 0 1950 1650 1500 0 1650 1650 0 0 0 0 0 0 0 0 0 1800 1800 0 0 156 132 120 0 132 132 0 0 0 0 0 0 0 0 0 144 144 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 143 130 0 143 143 0 0 0 0 0 0 0 0 0 156 156 110 0 121 121 0 0 0 0 0 0 0 0 0 132 132 0 110 110 0 0 0 0 0 0 0 0 0 120 120 0 0 0 0 0 0 0 0 0 0 0 0 0 121 0 0 0 0 0 0 0 0 0 132 132 0 0 0 0 0 0 0 0 0 132 132 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 144]);
		//[T,Y]=ode45(@fluidflow,[0 100],[ 0 5 5 0 0 0 35 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 5 0 0 0 0 0 69 0 0 0 0 1 1 0 0 0 0 0 35 0 52 54 0 0 0 0 0 1 0 0 0 0 0 0 0 5 35 0 0 35 0 64 0 0 0 0 0 0 0 0 0 0 0 0 0 25 25 0 0 0 1225 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 25 0 0 0 0 0 4761 0 0 0 0 1 1 0 0 0 0 0 1225 0 2704 2916 0 0 0 0 0 1 0 0 0 0 0 0 0 25 1225 0 0 1225 0 4096 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 25 0 0 0 175 0 0 0 0 0 0 5 0 0 0 0 0 0 0 0 25 0 0 0 0 0 345 0 0 0 0 5 5 0 0 0 0 0 175 0 260 270 0 0 0 0 0 5 0 0 0 0 0 0 0 25 175 0 0 175 0 320 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 175 0 0 0 0 0 0 5 0 0 0 0 0 0 0 0 25 0 0 0 0 0 345 0 0 0 0 5 5 0 0 0 0 0 175 0 260 270 0 0 0 0 0 5 0 0 0 0 0 0 0 25 175 0 0 175 0 320 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 35 0 0 0 0 0 0 0 0 175 0 0 0 0 0 2415 0 0 0 0 35 35 0 0 0 0 0 1225 0 1820 1890 0 0 0 0 0 35 0 0 0 0 0 0 0 175 1225 0 0 1225 0 2240 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 5 0 0 0 0 0 69 0 0 0 0 1 1 0 0 0 0 0 35 0 52 54 0 0 0 0 0 1 0 0 0 0 0 0 0 5 35 0 0 35 0 64 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 345 0 0 0 0 5 5 0 0 0 0 0 175 0 260 270 0 0 0 0 0 5 0 0 0 0 0 0 0 25 175 0 0 175 0 320 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 69 69 0 0 0 0 0 2415 0 3588 3726 0 0 0 0 0 69 0 0 0 0 0 0 0 345 2415 0 0 2415 0 4416 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 35 0 52 54 0 0 0 0 0 1 0 0 0 0 0 0 0 5 35 0 0 35 0 64 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 35 0 52 54 0 0 0 0 0 1 0 0 0 0 0 0 0 5 35 0 0 35 0 64 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1820 1890 0 0 0 0 0 35 0 0 0 0 0 0 0 175 1225 0 0 1225 0 2240 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 2808 0 0 0 0 0 52 0 0 0 0 0 0 0 260 1820 0 0 1820 0 3328 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 54 0 0 0 0 0 0 0 270 1890 0 0 1890 0 3456 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 5 35 0 0 35 0 64 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 175 0 0 175 0 320 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1225 0 2240 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 2240 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0]);
		String line = br.readLine();
		line = line.substring(24);
		String simTime = line.substring(0,line.indexOf(']'));
		simTime= simTime.substring(simTime.indexOf(' ')).trim();
		line = line.substring(line.indexOf(']')+3);
		line = line.substring(0,line.indexOf(']')).trim();
		
		String[] ic = line.split("\\ "); 
		
		HashMap<String, ISpecies> speciesNameToSpecies = new HashMap<>(getCRN().getSpecies().size());
		int s=0;
		for (ISpecies species : getCRN().getSpecies()) {
			if(ic[s].equals("0")){
				species.setInitialConcentration(BigDecimal.ZERO, "0");
			}
			else{
				species.setInitialConcentration(BigDecimal.valueOf(Double.valueOf(ic[s])), ic[s]);
			}
			s++;
			
			speciesNameToSpecies.put(species.getName(),species);
		}
		
		ArrayList<HashSet<ISpecies>> userDefinedInitialPartition=new ArrayList<>();
		
		List<ISpecies> speciesToPlot = new ArrayList<>();
		String[] labels=null;
		
		while((line = br.readLine())!=null){
			if(line.startsWith("plot(T")){
				//plot(T,Y(:,2),'g-','LineWidth',2);
				String id = line.substring(line.indexOf(":,")+2);
				id=id.substring(0,id.indexOf(')'));
				int idInt = Integer.valueOf(id);
				speciesToPlot.add(loadedSpeciesIdToSpecies[idInt-1]);
				HashSet<ISpecies> block=new HashSet<>(1);
				block.add(loadedSpeciesIdToSpecies[idInt-1]);
				userDefinedInitialPartition.add(block);
			}
			else if(line.startsWith("legend")){
				if(!line.equals("legend(")){
					//legend('S(0,0)','S(0,1)','S(1,0)','S(1,1)');
					line = line.substring(line.indexOf('\'')+1);
					line = line.substring(0,line.lastIndexOf(')')-1);
					labels=line.split("\',\'");
				}
			}
		}
		
		
		List<String> viewNames = new ArrayList<String>();
		List<String> viewExpressions = new ArrayList<String>();
		List<String> viewExpressionsSupportedByMathEval = new ArrayList<String>();
		List<Boolean> viewExpressionUsesCovariance = new ArrayList<Boolean>();
		boolean viewsSupportedForPrepartitioning=true;
		List<HashMap<ISpecies,Integer>> setsOfSpeciesOfViews = new ArrayList<HashMap<ISpecies,Integer>>();
		
		int labelsLength=0;
		if(labels!=null){
			labelsLength=2*labels.length;
		}
		
		HashMap<String, String> replacedNames = new HashMap<>(labelsLength);
		
		int l =0;
		for (ISpecies species : speciesToPlot) {
			String lab = (labels[l].replace("(", "").replace(")", "").replace(",", "_"));
			replacedNames.put(species.getName(), "E"+lab);
			species.setName("E"+lab);
			speciesNameToSpecies.put(species.getName(), species);
			speciesNameToSpecies.put(species.getName(), species);
			CRNImporter.loadAView(viewNames, viewExpressions, viewExpressionsSupportedByMathEval, viewExpressionUsesCovariance, "Eof"+lab, species.getName(), viewsSupportedForPrepartitioning, speciesNameToSpecies, setsOfSpeciesOfViews);
			l++;
		}
		
		
		br.close();
		
		momentFileName=momentFilePaths+"secondMoment.m";
		if(print){
			CRNReducerCommandLine.println(out,bwOut,"\nImporting: "+momentFileName);
		}
		
		try{
			br = getBufferedReader(momentFileName);
		}catch(FileNotFoundException e){
			CRNReducerCommandLine.printWarning(out,bwOut,"File with second moment data not provided.");
			return;
		}
		
		speciesToPlot = new ArrayList<>();
		while((line = br.readLine())!=null){
			if(line.startsWith("plot(T")){
				//plot(T,Y(:,2),'g-','LineWidth',2);
				String id = line.substring(line.indexOf(":,")+2);
				id=id.substring(0,id.indexOf(')'));
				int idInt = Integer.valueOf(id);
				speciesToPlot.add(loadedSpeciesIdToSpecies[idInt-1]);
				HashSet<ISpecies> block=new HashSet<>(1);
				block.add(loadedSpeciesIdToSpecies[idInt-1]);
				userDefinedInitialPartition.add(block);
			}
		}
		
		l =0;
		for (ISpecies species : speciesToPlot) {
			String lab = (labels[l].replace("(", "").replace(")", "").replace(",", "_"));
			replacedNames.put(species.getName(), "V"+lab);
			species.setName("V"+lab);
			speciesNameToSpecies.put(species.getName(), species);
			CRNImporter.loadAView(viewNames, viewExpressions, viewExpressionsSupportedByMathEval, viewExpressionUsesCovariance, "Vof"+lab, species.getName(), viewsSupportedForPrepartitioning, speciesNameToSpecies, setsOfSpeciesOfViews);
			l++;
		}
		
		getCRN().setUserDefinedPartition(userDefinedInitialPartition);
		
		for (ICRNReaction reaction : getCRN().getReactions()) {
			CRNReactionArbitraryMatlab react = (CRNReactionArbitraryMatlab)reaction;
			ASTNode rateLaw = react.getRateLaw();
			replaceSpeciesNames(rateLaw,replacedNames);
			react.setRateLaw(rateLaw);
			//react.setRateLaw(rateLaw);
			/*
			replaceODEVarWithSpecies(rateLaw,varsName);
			
			String body = rateLaw.toFormula();
			
			IComposite products = new Composite(speciesOfODE,speciesOfODE);
			ICRNReaction reaction = new CRNReactionArbitraryMatlab((IComposite)speciesOfODE, products, body,rateLaw,varsName);*/
		}
		
		br.close();
		
		CRNImporter.finalizeToLoadViews(viewNames, viewExpressions,
				viewExpressionsSupportedByMathEval,
				viewExpressionUsesCovariance, viewsSupportedForPrepartitioning,
				setsOfSpeciesOfViews,getCRN());
			
	}

	private void replaceSpeciesNames(ASTNode node,HashMap<String, String> replacedNames) {
		//A variable is actually a parameter, while a function (e.g., varsName(1)) is an ODE variable (with id 0).
		//System.out.println(node);
		if(node.isOperator()){
			for(int i=0;i<node.getChildCount();i++){
				replaceSpeciesNames(node.getChild(i), replacedNames);
			}
		}
		else if(node.isFunction() /*&& !node.getName().equals(replacedNames)*/){
			for(int i=0;i<node.getChildCount();i++){
				replaceSpeciesNames(node.getChild(i), replacedNames);
			}
		}
		else if(node.isVariable()){
			String replacedName = replacedNames.get(node.getName());
			if(replacedName!=null){
				node.setName(replacedName);
			}
			//DO NOTHING	
			//Pay attention: functions are considered variables  in isVariables()...
		}
		else if(node.isNumber()){
			//DO NOTHING
		}
		else{
			throw new UnsupportedOperationException(node.toString());
		}
	}
	

	
}

