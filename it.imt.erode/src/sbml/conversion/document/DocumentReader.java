package sbml.conversion.document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.jetbrains.annotations.NotNull;
import org.sbml.jsbml.SBMLDocument;

import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.crn.interfaces.ISpecies;
import it.imt.erode.importing.InfoBooleanNetworkImporting;
import it.imt.erode.importing.booleannetwork.GUIBooleanNetworkImporter;
import sbml.conversion.model.ModelManager;

class DocumentReader extends SBMLConverter {

    public DocumentReader(@NotNull SBMLDocument sbmlDocument) throws IOException {
        super(sbmlDocument);
        this.modelConverter = ModelManager.create(sbmlDocument.getModel());
        this.guiBnImporter = new GUIBooleanNetworkImporter(modelConverter.isMV(),null, null, null);
        this.infoImporting = createErodeModel();
        this.booleanNetwork = guiBnImporter.getBooleanNetwork();
        buildErodeModel();
    }

    private InfoBooleanNetworkImporting createErodeModel() throws IOException {

        InfoBooleanNetworkImporting importing = guiBnImporter.importBooleanNetwork(
                true,true, true,
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
        
    }

    private void initializeUpdateFunctions(LinkedHashMap<String, IUpdateFunction> updateFunctions) {
        this.booleanNetwork.setAllUpdateFunctions(updateFunctions);
        ArrayList<String> guessedInputs = new ArrayList<String>();
        for(Entry<String, IUpdateFunction> pair:updateFunctions.entrySet()) {
        	boolean guessedInput=pair.getValue().seemsInputSpecies(pair.getKey());
        	if(guessedInput) {
        		guessedInputs.add(pair.getKey());
        	}
        }
        
        if(guessedInputs.size()>0) {
        	LinkedHashMap<String, ISpecies> nameToSpecies=new LinkedHashMap<String, ISpecies>(booleanNetwork.getSpecies().size());
        	for(ISpecies sp : booleanNetwork.getSpecies()) {
        		nameToSpecies.put(sp.getName(), sp);
        	}
        	ArrayList<HashSet<ISpecies>> inputDistinguishing=new ArrayList<>(guessedInputs.size());
        	for(String name : guessedInputs) {
        		HashSet<ISpecies> block = new HashSet<ISpecies>(1);
        		block.add(nameToSpecies.get(name));
        		inputDistinguishing.add(block);
        	}
        	
    		booleanNetwork.setUserDefinedPartition(inputDistinguishing);
        }
    }
}
