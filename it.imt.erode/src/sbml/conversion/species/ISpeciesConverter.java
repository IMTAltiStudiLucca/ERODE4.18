package sbml.conversion.species;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;

import it.imt.erode.crn.interfaces.ISpecies;

public interface ISpeciesConverter {

    List<ISpecies> getErodeSpecies();

    ListOf<QualitativeSpecies> getSbmlSpecies();
    
    boolean isMultiValued();
    HashMap<String,Integer> getMaxValues();
    HashSet<String> getConstantSpecies();
}
