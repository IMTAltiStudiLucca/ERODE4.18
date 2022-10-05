package sbml.conversion.species;

import org.sbml.jsbml.ext.qual.QualitativeSpecies;

import it.imt.erode.crn.interfaces.ISpecies;
import sbml.configurations.SBMLConfiguration;

class SBMLSpeciesBuilder {
    private static final SBMLConfiguration CONFIG = SBMLConfiguration.getConfiguration();

    public QualitativeSpecies createSpecies(ISpecies species,int max) {
        QualitativeSpecies q = new QualitativeSpecies(CONFIG.getLevel(),CONFIG.getVersion());
        q.setId(species.getName());
        q.setName(species.getOriginalName());
        q.setCompartment(CONFIG.getDefaultCompartment());
        q.setMaxLevel(max);
        q.setInitialLevel(species.getInitialConcentration().intValue());
        q.setConstant(false);
        return q;
    }
}
