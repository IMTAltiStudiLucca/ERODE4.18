package sbml.conversion.model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.sbml.jsbml.Model;

import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.crn.interfaces.ISpecies;

public interface IModelConverter {

    Model getModel();

    String getName();

    List<ISpecies> getErodeSpecies();

    LinkedHashMap<String, IUpdateFunction> getErodeUpdateFunctions();

	boolean isMV();

	HashMap<String,Integer> getMaxValues();
}
