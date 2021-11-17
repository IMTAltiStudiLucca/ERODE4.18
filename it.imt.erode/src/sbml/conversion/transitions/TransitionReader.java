package sbml.conversion.transitions;

import java.util.LinkedHashMap;

import org.jetbrains.annotations.NotNull;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.ext.qual.Output;
import org.sbml.jsbml.ext.qual.Transition;

import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import sbml.conversion.functionterm.FunctionTermReader;

class TransitionReader extends TransitionConverter {

    private FunctionTermReader reader;

    public TransitionReader(@NotNull ListOf<Transition> listOfTransitions,boolean multivalued) {
        super(listOfTransitions);
        this.reader = new FunctionTermReader(multivalued);
        convertSBMLTransitions();
    }

    private void convertSBMLTransitions() {
        erodeUpdateFunctions = new LinkedHashMap<>();
        for(Transition t : this.sbmlTransitions) {
            ListOf<Output> outputs = t.getListOfOutputs();
            IUpdateFunction updateFunction = reader.convert(t.getListOfFunctionTerms());
            for(Output o : outputs) {
            	if(erodeUpdateFunctions.containsKey(o.getQualitativeSpecies())) {
            		throw new IllegalArgumentException("More than one update function for "+o.getQualitativeSpecies());
            	}
                erodeUpdateFunctions.put(o.getQualitativeSpecies(),updateFunction);
            }
        }
    }
}
