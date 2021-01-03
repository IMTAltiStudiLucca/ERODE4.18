package it.imt.erode.importing.spaceex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map.Entry;


import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.ui.console.MessageConsoleStream;
import org.xml.sax.SAXException;

import it.imt.erode.auxiliarydatastructures.CRNandPartition;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.crn.implementations.CRN;
import it.imt.erode.crn.implementations.CRNReactionArbitraryGUI;
import it.imt.erode.crn.implementations.Composite;
import it.imt.erode.crn.implementations.Species;
import it.imt.erode.crn.interfaces.ICRN;
import it.imt.erode.crn.interfaces.ICRNReaction;
import it.imt.erode.crn.interfaces.IComposite;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.expression.evaluator.MathEval;
import it.imt.erode.importing.AbstractImporter;
import it.imt.erode.importing.CRNImporter;
import it.imt.erode.importing.InfoCRNImporting;
import it.imt.erode.importing.ODEorNET;
import it.imt.erode.importing.UnsupportedFormatException;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import it.imt.erode.utopic.MatlabODEPontryaginExporter;


/**
 * 
 * @author Andrea Vandin
 * This class is used to import SpaceEx XML files. We import only the flows, and we encode inputs as constant variables 
 */
public class SpaceExImporter extends AbstractImporter{
	
	public SpaceExImporter(String fileName,MessageConsoleStream out,BufferedWriter bwOut,IMessageDialogShower msgDialogShower) {
		super(fileName,out,bwOut,msgDialogShower);
	}

	public InfoCRNImporting importSpaceExXML(boolean printInfo, boolean printCRN, boolean print, String mainComponent, String odesstr) throws FileNotFoundException, IOException, UnsupportedFormatException{

			boolean odes = false;
			if(odesstr!=null && odesstr.equalsIgnoreCase("true")) {
				odes=true;
			}
		
			if(mainComponent==null || mainComponent.equals("")) {
				mainComponent="core_component";
			}
			if(print){
				CRNReducerCommandLine.println(out,bwOut,"\nImporting SpaceEx XML file "+getFileName());
				CRNReducerCommandLine.println(out,bwOut,"\tThe flow will be taken from component "+mainComponent);
			}

			initInfoImporting();
			initCRNAndMath();
			//ICRN crn = getCRN();
			ICRN crn = new CRN(getFileName(), new MathEval(), out, bwOut);
			int fileSep = crn.getName().lastIndexOf(File.separator);
			if(fileSep>0) {
				String name =crn.getName().substring(fileSep+1,crn.getName().length());
				name=overwriteExtensionIfEnabled(name, "", true);
				crn.setName(name);
			}
			getInfoImporting().setLoadedCRN(true);

			crn.setMdelDefKind(ODEorNET.ODE);
			
			//System.out.println(crn.getName());
			
			
			long begin = System.currentTimeMillis();
			
			SpaceExHandler handler = new SpaceExHandler(mainComponent);
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser=null;
			try {
				saxParser = factory.newSAXParser();
				saxParser.parse(getFileName(), handler);
			} catch (ParserConfigurationException|SAXException e) {
				throw new IOException(e);
			}

			System.out.println("All flows have been read: "+handler.getVariableToFlow().size());
			System.out.println("pcc: "+handler.getPcc());
			HashMap<String, ISpecies> speciesNameToSpecies=new HashMap<String, ISpecies>();
			
			for(String sp : handler.getVariableToFlow().keySet()) {
				int id = crn.getSpecies().size();
				ISpecies species = new  Species(sp, null, id, BigDecimal.ZERO,"0",false);
				speciesNameToSpecies.put(sp, species);
				crn.addSpecies(species);
			}
			//int idLastVar = crn.getSpecies().size()-1;
			ArrayList<HashSet<ISpecies>> initialPartition=new ArrayList<HashSet<ISpecies>>(1);
			HashSet<ISpecies> inputsBlock = new LinkedHashSet<>(handler.getPcc().size());
			initialPartition.add(inputsBlock);
			for(String input : handler.getPcc()) {
				if(input.equalsIgnoreCase("stoptime")) {
					//ignore
				}
				else {
					int id = crn.getSpecies().size();
					ISpecies species = new  Species(input, null, id, BigDecimal.ZERO,"0",false);
					speciesNameToSpecies.put(input, species);
					crn.addSpecies(species);
					inputsBlock.add(species);
					
					species.addCommentLine("This is an input");
				}
				
				crn.setUserDefinedPartition(initialPartition);
			}
			
			for(Entry<String, String> pair : handler.getVariableToFlow().entrySet()) {
				String name = pair.getKey();
				String flow = pair.getValue();
				ISpecies species = speciesNameToSpecies.get(name);
				
				IComposite compositeReagents=(IComposite)species;
				IComposite compositeProducts=new Composite(species, species);
				
				ICRNReaction ode = new CRNReactionArbitraryGUI(compositeReagents, compositeProducts, flow, species.getName()+"_flow");
				CRNImporter.addReaction(crn, compositeReagents, compositeProducts, ode);
			}
			for(ISpecies input : inputsBlock) {
				IComposite compositeReagents=(IComposite)input;
				IComposite compositeProducts=new Composite(input, input);
				ICRNReaction ode = new CRNReactionArbitraryGUI(compositeReagents, compositeProducts, "0",null);
				CRNImporter.addReaction(crn, compositeReagents, compositeProducts, ode);
			}
			String name = crn.getName();

			IBlock uniqueBlock = new Block();
			//setInitialPartition(new Partition(uniqueBlock,getCRN().getSpecies().size()));
			IPartition partition = new Partition(uniqueBlock,crn.getSpecies().size());
			for (ISpecies species : crn.getSpecies()) {
				uniqueBlock.addSpecies(species);
			}
			CRNandPartition crnAndSpeciesAndPartition=new CRNandPartition(crn, partition);
			if(odes) {
				getCRN().setMdelDefKind(ODEorNET.ODE);
			}
			else {
				crnAndSpeciesAndPartition=MatlabODEPontryaginExporter.computeRNEncoding(crn, out, bwOut, partition,true);
			}
					
			
			crn = getCRN();
			crn.setName(name);
			for(ISpecies sp : crnAndSpeciesAndPartition.getCRN().getSpecies()) {
				crn.addSpecies(sp);
			}
			for(ICRNReaction r : crnAndSpeciesAndPartition.getCRN().getReactions()) {
				crn.addReaction(r);
			}
			
			
			ArrayList<HashSet<ISpecies>> init=new ArrayList<HashSet<ISpecies>>(1);
			HashSet<ISpecies> inputs = new LinkedHashSet<ISpecies>(crnAndSpeciesAndPartition.getCRN().getUserDefinedPartition().get(0).size());
			inputs.addAll(crnAndSpeciesAndPartition.getCRN().getUserDefinedPartition().get(0));
			init.add(inputs);
			crn.setUserDefinedPartition(init);
			
			
			createInitialPartition();
			
			getInfoImporting().setReadSpecies(crn.getSpeciesSize());
			getInfoImporting().setReadCRNReactions(getCRN().getReactions().size());
			//getInfoImporting().setReadReagents(getCRN().getReagents().size());
			//getInfoImporting().setReadProducts(getCRN().getProducts().size());
			long end=System.currentTimeMillis();
			getInfoImporting().setRequiredMS(end -begin);
			

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
