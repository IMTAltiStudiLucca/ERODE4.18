package sbml.conversion.transitions;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.ext.qual.FunctionTerm;
import org.sbml.jsbml.ext.qual.Input;
import org.sbml.jsbml.ext.qual.Output;
import org.sbml.jsbml.ext.qual.Transition;

import sbml.configurations.SBMLConfiguration;

class TransitionBuilder {
    protected static final SBMLConfiguration CONFIG = SBMLConfiguration.getConfiguration();

    public Transition createTransition(ListOf<Input> inputs, ListOf<Output> outputs, ListOf<FunctionTerm> functionTerms) {
        Transition t = new Transition(CONFIG.getLevel(),CONFIG.getVersion());
        t.setListOfInputs(inputs);
        t.setListOfOutputs(outputs);
        t.setListOfFunctionTerms(functionTerms);
        return t;
    }
}
