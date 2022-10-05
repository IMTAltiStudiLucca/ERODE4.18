package sbml.conversion.document;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.ui.console.MessageConsoleStream;
import org.jetbrains.annotations.NotNull;
import org.sbml.jsbml.SBMLDocument;

import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;
import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.commandline.CRNReducerCommandLine;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.importing.InfoBooleanNetworkImporting;
import it.imt.erode.importing.booleannetwork.GUIBooleanNetworkImporter;
import it.imt.erode.importing.booleannetwork.GuessPrepartitionBN;
import sbml.conversion.model.ModelManager;

class DocumentReader extends SBMLConverter {

    public DocumentReader(@NotNull SBMLDocument sbmlDocument,GuessPrepartitionBN guessPrep, MessageConsoleStream out, BufferedWriter bwOut,String nameFromFile) throws IOException {
        super(sbmlDocument,guessPrep);
        long begin = System.currentTimeMillis();
        this.modelConverter = ModelManager.create(sbmlDocument.getModel(),nameFromFile);
        this.guiBnImporter = new GUIBooleanNetworkImporter(modelConverter.isMV(),out, bwOut, null,false);
        this.infoImporting = createErodeModel();
        this.booleanNetwork = guiBnImporter.getBooleanNetwork();
        buildErodeModel();
        long end = System.currentTimeMillis();
        infoImporting.setRequiredMS(end-begin);
        CRNReducerCommandLine.println(out,bwOut,getInfoImporting().toStringShort());
    }

    private InfoBooleanNetworkImporting createErodeModel() throws IOException {

        InfoBooleanNetworkImporting importing = guiBnImporter.importBooleanNetwork(
                false,false, true,
                modelConverter.getName(),
                new ArrayList<ArrayList<String>>(),
                new LinkedHashMap<String, IUpdateFunction>(),
                new ArrayList<ArrayList<String>>(),
                null);
        return importing;
    }

    private void buildErodeModel() {
        this.initializeSpecies(modelConverter.getErodeSpecies(),modelConverter.getMaxValues());
        initializeUpdateFunctions(booleanNetwork,guessPrepartitionOnInputs,modelConverter.getErodeUpdateFunctions());
    }

    private void initializeSpecies(List<ISpecies> erodeSpecies,HashMap<String, Integer> maxValues) {
        for(ISpecies s : erodeSpecies) {
            this.booleanNetwork.addSpecies(s);
            if(booleanNetwork.isMultiValued()) {
            	booleanNetwork.setMax(s, maxValues.get(s.getName()));
            }
        }
        infoImporting.setReadNodes(booleanNetwork.getSpecies().size());
    }

    public static void initializeUpdateFunctions(IBooleanNetwork booleanNetwork, GuessPrepartitionBN guessPrepartitionOnInputs, LinkedHashMap<String, IUpdateFunction> updateFunctions) {
        booleanNetwork.setAllUpdateFunctions(updateFunctions);
        if(guessPrepartitionOnInputs.equals(GuessPrepartitionBN.INPUTS)) {
        	guessInputs(booleanNetwork,updateFunctions,false);
        }
        else if(guessPrepartitionOnInputs.equals(GuessPrepartitionBN.OUTPUTS)) {
        	guessOutputs(booleanNetwork,updateFunctions,false);
        }
        else if(guessPrepartitionOnInputs.equals(GuessPrepartitionBN.INPUTSONEBLOCK)) {
        	guessInputs(booleanNetwork,updateFunctions,true);
        }
        else if(guessPrepartitionOnInputs.equals(GuessPrepartitionBN.OUTPUTSONEBLOCK)) {
        	guessOutputs(booleanNetwork,updateFunctions,true);
        }
    }

    public static ArrayList<String> guessInputs(LinkedHashMap<String, IUpdateFunction> updateFunctions){
    	ArrayList<String> guessedInputs = new ArrayList<String>();
		for(Entry<String, IUpdateFunction> pair:updateFunctions.entrySet()) {
			boolean guessedInput=pair.getValue().seemsInputSpecies(pair.getKey());
			if(guessedInput) {
				guessedInputs.add(pair.getKey());
			}
		}
		return guessedInputs;
    }
    
	public static void guessInputs(IBooleanNetwork booleanNetwork, LinkedHashMap<String, IUpdateFunction> updateFunctions,boolean oneBlock) {
		ArrayList<String> guessedInputs=guessInputs(updateFunctions);
		setUserPrep(booleanNetwork,guessedInputs,oneBlock);
	}

	public static void setUserPrep(IBooleanNetwork booleanNetwork, Collection<String> singletonSpecies,boolean oneBlock) {
		if(singletonSpecies.size()>0) {
			LinkedHashMap<String, ISpecies> nameToSpecies=new LinkedHashMap<String, ISpecies>(booleanNetwork.getSpecies().size());
			for(ISpecies sp : booleanNetwork.getSpecies()) {
				nameToSpecies.put(sp.getName(), sp);
			}
			ArrayList<HashSet<ISpecies>> inputDistinguishing=new ArrayList<>(singletonSpecies.size());
			if(oneBlock) {
				inputDistinguishing=new ArrayList<>(1);
				HashSet<ISpecies> block = new HashSet<ISpecies>(singletonSpecies.size());
				inputDistinguishing.add(block);
				for(String name : singletonSpecies) {
					block.add(nameToSpecies.get(name));
				}
			}
			else {
				inputDistinguishing=new ArrayList<>(singletonSpecies.size());
				for(String name : singletonSpecies) {
					HashSet<ISpecies> block = new HashSet<ISpecies>(1);
					block.add(nameToSpecies.get(name));
					inputDistinguishing.add(block);
				}
			}
			
			booleanNetwork.setUserDefinedPartition(inputDistinguishing);
		}
	}
	
    public static HashSet<String> guessOutputs(LinkedHashMap<String, IUpdateFunction> updateFunctions){
    	//HashSet<String> guessedOutputs = new HashSet<String>(booleanNetwork.getSpecies().size());
//		for(ISpecies sp:booleanNetwork.getSpecies()) {
//			guessedOutputs.add(sp.getName());
//		}
    	HashSet<String> guessedOutputs = new HashSet<String>(updateFunctions.keySet().size());
    	for(String sp:updateFunctions.keySet()) {
			guessedOutputs.add(sp);
		}
		
		//ArrayList<String> guessedInputs = new ArrayList<String>();
		for(Entry<String, IUpdateFunction> pair:updateFunctions.entrySet()) {
			pair.getValue().dropNonOutputSpecies(pair.getKey(),guessedOutputs);
		}
		return guessedOutputs;
    }	
	
	public static void guessOutputs(IBooleanNetwork booleanNetwork, LinkedHashMap<String, IUpdateFunction> updateFunctions,boolean oneBlock) {
		HashSet<String> guessedOutputs=guessOutputs(updateFunctions);
		setUserPrep(booleanNetwork,guessedOutputs,oneBlock);
	}
}
