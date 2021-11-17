package sbml.conversion.transitions;

import java.util.LinkedHashMap;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.ext.qual.Transition;

import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;

public interface ITransitionConverter {

    LinkedHashMap<String, IUpdateFunction> getErodeUpdateFunctions();

    ListOf<Transition> getSbmlTransitions();

}
