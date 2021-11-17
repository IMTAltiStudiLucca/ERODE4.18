package sbml.conversion.qualmodel;

import java.util.LinkedHashMap;

import org.jetbrains.annotations.NotNull;
import org.sbml.jsbml.ext.qual.QualModelPlugin;

import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.ReferenceToNodeUpdateFunction;
import it.imt.erode.crn.interfaces.ISpecies;
import sbml.conversion.species.SpeciesManager;
import sbml.conversion.transitions.TransitionManager;

class QualModelReader extends QualModelConverter {

    public QualModelReader(@NotNull QualModelPlugin qualModelPlugin) {
        super(qualModelPlugin);
        this.speciesConverter = SpeciesManager.create(qualModelPlugin.getListOfQualitativeSpecies());
        this.transitionConverter = TransitionManager.create(qualModelPlugin.getListOfTransitions(),speciesConverter.isMultiValued());
        
        for(ISpecies sp: speciesConverter.getErodeSpecies()) {
        	LinkedHashMap<String, IUpdateFunction> updFuncs = transitionConverter.getErodeUpdateFunctions();
        	if(updFuncs.get(sp.getName())==null) {
        		if(speciesConverter.getConstantSpecies().contains(sp.getName())) {
        			updFuncs.put(sp.getName(), new ReferenceToNodeUpdateFunction(sp.getName()));
        		}
        		else {
        			throw new IllegalArgumentException("Non-constant species "+sp.getName()+" has no update function");
        		}
        	}
        }
    }
}
