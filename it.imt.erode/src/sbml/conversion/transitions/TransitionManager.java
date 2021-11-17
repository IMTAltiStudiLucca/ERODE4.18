package sbml.conversion.transitions;

import java.util.LinkedHashMap;

import org.jetbrains.annotations.NotNull;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.ext.qual.Transition;

import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;

public class TransitionManager {
    public static ITransitionConverter create(@NotNull ListOf<Transition> transitions,boolean multivalued) {
        return new TransitionReader(transitions,multivalued);
    }

    public static ITransitionConverter create(@NotNull LinkedHashMap<String, IUpdateFunction> updateFunctions) {
        return new TransitionWriter(updateFunctions);
    }
}
