package sbml.conversion.species;

import java.util.LinkedHashMap;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;

import it.imt.erode.booleannetwork.implementations.BooleanNetwork;
import it.imt.erode.crn.interfaces.ISpecies;

class SpeciesWriter extends SpeciesConverter {

    private SBMLSpeciesBuilder speciesBuilder;

    public SpeciesWriter(@NotNull List<ISpecies> species, LinkedHashMap<String, Integer> nameToMax) {
        super(species);
        this.speciesBuilder = new SBMLSpeciesBuilder();
        this.sbmlSpecies = convertERODESpecies(nameToMax);
    }

    private ListOf<QualitativeSpecies> convertERODESpecies(LinkedHashMap<String, Integer> nameToMax) {
        ListOf<QualitativeSpecies> sbmlSpecies = new ListOf<>(CONFIG.getLevel(), CONFIG.getVersion());
        for(ISpecies s : this.erodeSpecies) {
        	int max=BooleanNetwork.getNameToMax(s.getName(), nameToMax);
            QualitativeSpecies q = speciesBuilder.createSpecies(s,max);
            sbmlSpecies.add(q);
        }
        return sbmlSpecies;
    }
}
