package sbml.conversion.transitions;

import java.util.LinkedHashMap;

import org.jetbrains.annotations.NotNull;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.ext.qual.Transition;

import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import sbml.configurations.SBMLConfiguration;

abstract class TransitionConverter implements ITransitionConverter {
    protected static final SBMLConfiguration CONFIG = SBMLConfiguration.getConfiguration();

    protected ListOf<Transition> sbmlTransitions;

    protected LinkedHashMap<String, IUpdateFunction> erodeUpdateFunctions;

    public TransitionConverter(@NotNull ListOf<Transition> listOfTransitions) {
        this.sbmlTransitions = listOfTransitions;
    }

    public TransitionConverter(LinkedHashMap<String, IUpdateFunction> updateFunctions) {
        this.erodeUpdateFunctions = updateFunctions;
    }

    public LinkedHashMap<String, IUpdateFunction> getErodeUpdateFunctions() {
        return this.erodeUpdateFunctions;
    }

    public ListOf<Transition> getSbmlTransitions() {
        return sbmlTransitions;
    }
}
