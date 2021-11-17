package sbml.conversion.qualmodel;

import org.jetbrains.annotations.NotNull;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ext.qual.QualModelPlugin;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Transition;

import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;
import sbml.conversion.species.SpeciesManager;
import sbml.conversion.transitions.TransitionManager;

class QualModelWriter extends QualModelConverter {

    public QualModelWriter(@NotNull IBooleanNetwork booleanNetwork, Model model) {
        super();
        this.speciesConverter = SpeciesManager.create(booleanNetwork.getSpecies());
        this.transitionConverter = TransitionManager.create(booleanNetwork.getUpdateFunctions());
        this.sbmlQualModel = new QualModelPlugin(model);
        this.buildSBMLQualModel(speciesConverter.getSbmlSpecies(),
                transitionConverter.getSbmlTransitions());
    }

    private void buildSBMLQualModel(ListOf<QualitativeSpecies> sbmlSpecies,
                                    ListOf<Transition> sbmlTransitions) {
        sbmlSpecies.setParent(sbmlQualModel.getModel());
        sbmlQualModel.setListOfQualitativeSpecies(sbmlSpecies);
        sbmlTransitions.setParent(sbmlQualModel.getModel());
        sbmlQualModel.setListOfTransitions(sbmlTransitions);
    }
}
