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
        this.guiBnImporter = new GUIBooleanNetworkImporter(modelConverter.isMV(),out, bwOut, null);
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
        this.initializeUpdateFunctions(modelConverter.getErodeUpdateFunctions());
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

    private void initializeUpdateFunctions(LinkedHashMap<String, IUpdateFunction> updateFunctions) {
        this.booleanNetwork.setAllUpdateFunctions(updateFunctions);
        if(this.guessPrepartitionOnInputs.equals(GuessPrepartitionBN.INPUTS)) {
        	guessInputs(updateFunctions);
        }
        else if(this.guessPrepartitionOnInputs.equals(GuessPrepartitionBN.OUTPUTS)) {
        	guessOutputs(updateFunctions);
        }
    }

	private void guessInputs(LinkedHashMap<String, IUpdateFunction> updateFunctions) {
		ArrayList<String> guessedInputs = new ArrayList<String>();
		for(Entry<String, IUpdateFunction> pair:updateFunctions.entrySet()) {
			boolean guessedInput=pair.getValue().seemsInputSpecies(pair.getKey());
			if(guessedInput) {
				guessedInputs.add(pair.getKey());
			}
		}

		setUserPrep(guessedInputs);
	}

	private void setUserPrep(Collection<String> singletonSpecies) {
		if(singletonSpecies.size()>0) {
			LinkedHashMap<String, ISpecies> nameToSpecies=new LinkedHashMap<String, ISpecies>(booleanNetwork.getSpecies().size());
			for(ISpecies sp : booleanNetwork.getSpecies()) {
				nameToSpecies.put(sp.getName(), sp);
			}
			ArrayList<HashSet<ISpecies>> inputDistinguishing=new ArrayList<>(singletonSpecies.size());
			for(String name : singletonSpecies) {
				HashSet<ISpecies> block = new HashSet<ISpecies>(1);
				block.add(nameToSpecies.get(name));
				inputDistinguishing.add(block);
			}

			booleanNetwork.setUserDefinedPartition(inputDistinguishing);
		}
	}
	
	private void guessOutputs(LinkedHashMap<String, IUpdateFunction> updateFunctions) {
		HashSet<String> guessedOutputs = new HashSet<String>(booleanNetwork.getSpecies().size());
		for(ISpecies sp:booleanNetwork.getSpecies()) {
			guessedOutputs.add(sp.getName());
		}
		
		//ArrayList<String> guessedInputs = new ArrayList<String>();
		for(Entry<String, IUpdateFunction> pair:updateFunctions.entrySet()) {
			pair.getValue().dropNonOutputSpecies(pair.getKey(),guessedOutputs);
		}
		
		setUserPrep(guessedOutputs);
	}
}
