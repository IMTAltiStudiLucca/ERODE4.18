package it.imt.erode.importing.booleannetwork;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
//import java.util.List;
import java.util.Map.Entry;

import javax.xml.stream.XMLStreamException;

import org.eclipse.ui.console.MessageConsoleStream;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLWriter;

import it.imt.erode.booleannetwork.implementations.BooleanNetwork;
import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;
import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.commandline.IMessageDialogShower;
import it.imt.erode.crn.implementations.Species;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.importing.AbstractImporter;
import it.imt.erode.importing.GUICRNImporter;
import it.imt.erode.importing.InfoBooleanNetworkImporting;
import it.imt.erode.partition.implementations.Block;
import it.imt.erode.partition.implementations.Partition;
import it.imt.erode.partition.interfaces.IBlock;
import it.imt.erode.partition.interfaces.IPartition;
import sbml.conversion.document.ISBMLConverter;
import sbml.conversion.document.SBMLManager;

public class GUIBooleanNetworkImporter {
	
	//private List<String> furtherCommands;
	protected MessageConsoleStream out;
	protected BufferedWriter bwOut;
	protected IMessageDialogShower msgDialogShower;
	private InfoBooleanNetworkImporting infoImporting;
	private BooleanNetwork booleanNetwork;
	private boolean mv=false;
	
	private IPartition initialPartition;

	public GUIBooleanNetworkImporter(boolean mv,MessageConsoleStream out, BufferedWriter bwOut,
			IMessageDialogShower msgDialogShower) {
		this.mv=mv;
		this.out=out;
		this.bwOut=bwOut;
		//furtherCommands=new ArrayList<String>();
		this.msgDialogShower=msgDialogShower;
	}
	
	protected void initInfoImporting(String name) {
		this.infoImporting = new InfoBooleanNetworkImporting(0, name,0);
	}
	
	public InfoBooleanNetworkImporting importBooleanNetwork(boolean printInfo, boolean printBooleanNetwork,boolean print, 
			String modelName, 
			ArrayList<ArrayList<String>> initialConcentrations, 
			LinkedHashMap<String, IUpdateFunction> booleanUpdateFunctions, 
			ArrayList<ArrayList<String>> initialPartition, MessageConsoleStream consoleOut) throws IOException{
		if(print){
			//CRNReducerCommandLine.println(out,"\nImporting the model "+ modelName +" from the editor");
			CRNReducerCommandLine.println(out,bwOut,"\nReading "+ modelName +"...");
		}

		initInfoImporting(modelName +" from editor");

		initBooleanNetwork(modelName);
		getInfoImporting().setLoadedBooleanNetwork(true);
		
		HashMap<String, ISpecies> nodesStoredInHashMap = new HashMap<>();
		for (ArrayList<String> initialConcentration : initialConcentrations) {
			if(mv) {
				//name,ic,max,[origname] -> name,[origname],ic - max
				if(initialConcentration.size()==4){
					addMVNode(initialConcentration.get(0), initialConcentration.get(3),Integer.valueOf(initialConcentration.get(1)),Integer.valueOf(initialConcentration.get(2)),  nodesStoredInHashMap);
				}
				else{
					addMVNode(initialConcentration.get(0), null                       ,Integer.valueOf(initialConcentration.get(1)),Integer.valueOf(initialConcentration.get(2)), nodesStoredInHashMap);
				}
			}
			else {
				//name,ic,[origname] -> name,[origname],ic
				if(initialConcentration.size()==3){
					addNode(initialConcentration.get(0), initialConcentration.get(2),Boolean.valueOf(initialConcentration.get(1)), nodesStoredInHashMap);
				}
				else{
					addNode(initialConcentration.get(0), null,Boolean.valueOf(initialConcentration.get(1)), nodesStoredInHashMap);
				}
			}
			
			
		}
		
		booleanNetwork.setAllUpdateFunctions(booleanUpdateFunctions);
		
		IBlock uniqueBlock = new Block();
		IPartition partition = new Partition(uniqueBlock,getBooleanNetwork().getSpecies().size());
		setInitialPartition(partition);

		for (ISpecies node : getBooleanNetwork().getSpecies()) {
			uniqueBlock.addSpecies(node);
		}
		readPartition(initialPartition, nodesStoredInHashMap,getBooleanNetwork());
		
		
		getInfoImporting().setReadNodes(getBooleanNetwork().getSpecies().size());


		if(printInfo&&print){
			CRNReducerCommandLine.println(out,bwOut,getInfoImporting().toStringShort());
		}
		if(printBooleanNetwork&&print){
			getBooleanNetwork().printBooleanNetwork();
		}

		return getInfoImporting();
	}
	
	public static void readPartition(ArrayList<ArrayList<String>> initialPartition,
			HashMap<String, ISpecies> speciesStoredInHashMap,IBooleanNetwork bn) {
		//if(!(initialPartition.size()==0 ||(initialPartition.size()==1 && initialPartition.get(0).equals("")))){
		if(!(initialPartition.size()==0 ||(initialPartition.size()==1 && initialPartition.get(0).size()==0))){
			ArrayList<HashSet<ISpecies>> userDefinedInitialPartition = new ArrayList<HashSet<ISpecies>>(initialPartition.size());
			bn.setUserDefinedPartition(userDefinedInitialPartition);
			for (ArrayList<String> currentBlockString : initialPartition) {
				HashSet<ISpecies> currentBlock = new HashSet<>(currentBlockString.size());
				userDefinedInitialPartition.add(currentBlock);
				for (String currentSpeciesName : currentBlockString) {
					ISpecies currentSpecies = speciesStoredInHashMap.get(currentSpeciesName);
					if(currentSpecies!=null){
						currentBlock.add(currentSpecies);
					}
				}
			}
		}
	}
	
	protected void setInitialPartition(IPartition initialPartition) {
		this.initialPartition = initialPartition;
	}
	
	protected ISpecies addMVNode(String name,String originalName, int ic, int max, HashMap<String, ISpecies> nodesStoredInHashMap){
		int id = booleanNetwork.getSpecies().size();
		ISpecies node = new Species(name, originalName,id, BigDecimal.valueOf(ic),String.valueOf(ic),false);
		nodesStoredInHashMap.put(name, node);
		booleanNetwork.addSpecies(node);
		booleanNetwork.setMax(node, max);
		return node;
	}
	
	protected ISpecies addNode(String name,String originalName, boolean ic, HashMap<String, ISpecies> nodesStoredInHashMap){
			int id = booleanNetwork.getSpecies().size();
			BigDecimal icBD = BigDecimal.ZERO;
			if(ic){
				icBD=BigDecimal.ONE;
			}
			ISpecies node = new Species(name, originalName,id, icBD,String.valueOf(ic),false);
			nodesStoredInHashMap.put(name, node);
			booleanNetwork.addSpecies(node);
			return node;
	}
	
	public IBooleanNetwork getBooleanNetwork() {
		return booleanNetwork;
	}

	private InfoBooleanNetworkImporting getInfoImporting() {
		return infoImporting;
	}

	protected void initBooleanNetwork(String name) {
		booleanNetwork = new BooleanNetwork(name, out, bwOut,mv);
	}

	public IPartition getInitialPartition() {
		return initialPartition;
	}
	
	public static void printToBNBoolNetFIle(IBooleanNetwork bn,IPartition partition, String name, Collection<String> preambleCommentLines, boolean verbose, MessageConsoleStream out,BufferedWriter bwOut, boolean originalNames){
		String fileName = name;
		
		fileName=AbstractImporter.overwriteExtensionIfEnabled(fileName,".bnet");
		if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Writing model in file "+fileName);
		}

		AbstractImporter.createParentDirectories(fileName);
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printToBNBoolNetFIle, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}

		try {

			if(preambleCommentLines!=null && preambleCommentLines.size()>0){
				for (String comment : preambleCommentLines) {
					bw.write("#"+comment+"\n");
				}
			}
			
			bw.write("# model in BoolNet format\n");
			bw.write("# the header targets, factors is mandatory to be importable in the R package BoolNet\n");
			bw.write("\n");
			bw.write("targets, factors\n");

			//bw.write("\n\n");
			
			for(Entry<String, IUpdateFunction> entry:bn.getUpdateFunctions().entrySet()) {
				String speciesName =entry.getKey();
				IUpdateFunction update = entry.getValue();
				bw.write(speciesName + ", ");
				bw.write(update.toString());
				bw.write("\n");
			}
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printToBNBoolNetFIle, exception raised while writing in the file: "+fileName);
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
				CRNReducerCommandLine.println(out,bwOut,"Problems in printToBNBoolNetFIle, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}


		
	}

	public static void printToSBMLQualFIle(IBooleanNetwork bn, IPartition partition, String name,
			Collection<String> preambleCommentLines, boolean verbose, MessageConsoleStream out,
			BufferedWriter bwOut) {
	//public static void printToSBMLQualFIle(IBooleanNetwork bn,IPartition partition, String name, 
		//			Collection<String> preambleCommentLines, boolean verbose, MessageConsoleStream out,BufferedWriter bwOut){
		String fileName = name;

		fileName=AbstractImporter.overwriteExtensionIfEnabled(fileName,".sbml");
		if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Writing model in file "+fileName);
		}

		AbstractImporter.createParentDirectories(fileName);

		ISBMLConverter converter = SBMLManager.create(bn);
		SBMLDocument sbmlDocument = converter.getSbmlDocument();

		try {
			File sbmlFile = new File(fileName);
			if(sbmlFile.createNewFile()) {
				System.out.println("Created file: " + sbmlFile.getName() + "at path: " + sbmlFile.getPath());
			}
			System.out.println("Writing to file...");
			SBMLWriter.write(sbmlDocument, sbmlFile, "SBMLConverter", "1.0");
			System.out.println("Finished");
		} catch (IOException | XMLStreamException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printToSBMLQualFIle, exception raised while writing the SBML file for: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		if(verbose){
			CRNReducerCommandLine.println(out,bwOut,"Writing model in file "+fileName+" completed");
		}
	}
	
	public static void printToBNERODEFIle(IBooleanNetwork bn,IPartition partition, String name, Collection<String> preambleCommentLines, 
			boolean verbose, MessageConsoleStream out,BufferedWriter bwOut, boolean originalNames, Collection<String> commands){
		String fileName = name;
		
		fileName=AbstractImporter.overwriteExtensionIfEnabled(fileName,".ode");
		if(verbose){
			CRNReducerCommandLine.print(out,bwOut,"Writing model in file "+fileName);
		}

		AbstractImporter.createParentDirectories(fileName);
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(fileName));
		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printToBNERODEFIle, exception raised while creating the filewriter for file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}

		try {

			if(preambleCommentLines!=null && preambleCommentLines.size()>0){
				for (String comment : preambleCommentLines) {
					bw.write("//"+comment+"\n");
				}
				
			}
			//bw.write("\n\n");
			
			if(bn.isMultiValued()) {
				bw.write("begin Multivalued Boolean network ");
			}
			else {
				bw.write("begin Boolean network ");
			}
			
			if(bn.getName()!=null&&bn.getName()!=""){
				String nam = GUICRNImporter.getModelName(name);
				bw.write(nam+"\n");
			}
			else{
				bw.write("unnamed\n");
			}
			
			GUICRNImporter.writeInitBlock(bw, bn.getSpecies(), originalNames, null, false,bn.getNameToMax());
			GUICRNImporter.writeInitPartition(bw, bn.getUserDefinedPartition(), null, false);
			
			bw.write("begin update functions\n");
			for(Entry<String, IUpdateFunction> entry:bn.getUpdateFunctions().entrySet()) {
				String speciesName =entry.getKey();
				IUpdateFunction update = entry.getValue();
				bw.write("  "+speciesName + " = ");
				bw.write(update.toString());
				bw.write("\n");
			}
			bw.write("end update functions\n");
			
			if(commands!=null) {
				for(String command: commands) {
					bw.write(" "+command+"\n");
				}
			}
			
			boolean printedBeginEndComments = false;
			//bw.write("\n //Comments associated to the species\n");
			for (ISpecies species : bn.getSpecies()) {
				boolean speciesWritten=false;
				if(species.getComments()!=null && species.getComments().size()>0){
					for(String comment : species.getComments()){
						if(!comment.equals("")){
							if(!printedBeginEndComments){
								printedBeginEndComments=true;
								//writeCommentLine(br, " Comments associated to the species");
								//bw.write("\n //Comments associated to the species\n");
								//br.write("begin comments\n");
								bw.write("\n//Comments associated to the species\n");
							}
							//bw.write("\n //"+species.getName()+":  \n");
							if(!speciesWritten){
								bw.write("//"+species.getName()+":  \n");
								speciesWritten=true;
							}
							bw.write("  //" +comment+"\n");
						}
					}
				}
			}
			
//			if(bn.getCommands()!=null && crn.getCommands().size()>0){
//				for (ICommand command : crn.getCommands()) {
//					bw.write(command.toODEFormat()+"\n");
//				}
//			}

			if(bn.isMultiValued()) {
				bw.write("end Multivalued Boolean network ");
			}
			else {
				bw.write("end Boolean network ");
			}

			

		} catch (IOException e) {
			CRNReducerCommandLine.println(out,bwOut,"Problems in printToBNERODEFIle, exception raised while writing in the file: "+fileName);
			CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			return;
		}
		finally{
			if(verbose){
				//CRNReducerCommandLine.println(out,bwOut,"Writing model in file "+fileName+" completed");
				CRNReducerCommandLine.println(out,bwOut," completed");
			}
			try {
				bw.flush();
				bw.close();
			} catch (IOException e) {
				CRNReducerCommandLine.println(out,bwOut,"Problems in printToBNERODEFIle, exception raised while closing the bufferedwriter of the file: "+fileName);
				CRNReducerCommandLine.printStackTrace(out,bwOut,e);
			}
		}


		
	}

	

	

}
