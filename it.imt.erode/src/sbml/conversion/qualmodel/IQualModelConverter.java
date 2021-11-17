package sbml.conversion.qualmodel;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.sbml.jsbml.ext.qual.QualModelPlugin;

import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.crn.interfaces.ISpecies;

public interface IQualModelConverter {

    QualModelPlugin getSbmlQualModel();

    List<ISpecies> getErodeSpecies();

    LinkedHashMap<String, IUpdateFunction> getUpdateFunctions();
    
    boolean isMV();

	HashMap<String,Integer> getMaxValues();

}
