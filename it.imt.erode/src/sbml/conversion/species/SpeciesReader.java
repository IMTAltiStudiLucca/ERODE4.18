package sbml.conversion.species;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;

import org.jetbrains.annotations.NotNull;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;

import it.imt.erode.crn.interfaces.ISpecies;

class SpeciesReader extends SpeciesConverter {

    private ErodeSpeciesBuilder erodeSpeciesBuilder;

    public SpeciesReader(@NotNull ListOf<QualitativeSpecies> listOfQualitativeSpecies) {
        super(listOfQualitativeSpecies);
        this.erodeSpeciesBuilder = new ErodeSpeciesBuilder();
        convertSBMLSpecies();
    }

    private void convertSBMLSpecies() {
        erodeSpecies = new ArrayList<>(sbmlSpecies.size());
        constantSpecies= new LinkedHashSet<>();
        maxValues = new HashMap<>(sbmlSpecies.size());
        int id = 0;
        for(QualitativeSpecies q : this.sbmlSpecies) {
        	int init=0;
        	if(q.isSetConstant()) {
        		constantSpecies.add(q.getId());
        	}
        	if(q.isSetInitialLevel()) {
        		init=q.getInitialLevel();
        	}
        	else {
        		init=0;
        	}
        	//q.getInitialLevel();
            ISpecies s = erodeSpeciesBuilder.createSpecies(id, q.getId(), init);
            erodeSpecies.add(s);
            setMaxValue(s.getName(),q.getMaxLevel());
            id++;
        }
        
        if(!multivalued) {
        	for(ISpecies sp:erodeSpecies) {
        		erodeSpeciesBuilder.setBooleanICExprs(sp);
        	}
        }
    }

}
