package sbml.conversion.transitions;

import java.util.LinkedHashMap;
import java.util.Map;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.ext.qual.FunctionTerm;
import org.sbml.jsbml.ext.qual.Input;
import org.sbml.jsbml.ext.qual.Output;
import org.sbml.jsbml.ext.qual.Transition;

import it.imt.erode.booleannetwork.implementations.BooleanNetwork;
import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import sbml.conversion.functionterm.FunctionTermWriter;

class TransitionWriter extends TransitionConverter {

    private InputBuilder inputBuilder;
    private OutputBuilder outputBuilder;
    private TransitionBuilder transitionBuilder;
    private FunctionTermWriter writer;

    public TransitionWriter(LinkedHashMap<String, IUpdateFunction> updateFunctions,LinkedHashMap<String, Integer> nameToMax,boolean mv) {
        super(updateFunctions);
        this.inputBuilder = new InputBuilder();
        this.outputBuilder = new OutputBuilder();
        this.transitionBuilder = new TransitionBuilder();
        this.writer = new FunctionTermWriter();
        this.sbmlTransitions = convertERODEUpdateFunctions(nameToMax,mv);
    }

    private ListOf<Transition> convertERODEUpdateFunctions(LinkedHashMap<String, Integer> nameToMax,boolean mv) {
        ListOf<Transition> sbmlTransitions = new ListOf<>(CONFIG.getLevel(),CONFIG.getVersion());
        int id = 0;
        for (Map.Entry<String, IUpdateFunction> e : erodeUpdateFunctions.entrySet()) {
            ListOf<Output> outputs = outputBuilder.build(e.getKey(), id);
            ListOf<Input> inputs = inputBuilder.buildAll(e.getValue());
            ListOf<FunctionTerm> functionTerms = writer.convert(e.getValue(), BooleanNetwork.getNameToMax(e.getKey(),nameToMax),mv);
            Transition transition = transitionBuilder.createTransition(inputs,outputs,functionTerms);
            sbmlTransitions.add(transition);
            id++;
        }
        return sbmlTransitions;
    }

}
