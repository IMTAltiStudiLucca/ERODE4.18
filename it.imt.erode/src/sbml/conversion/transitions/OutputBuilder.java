package sbml.conversion.transitions;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.ext.qual.Output;
import org.sbml.jsbml.ext.qual.OutputTransitionEffect;

import sbml.configurations.SBMLConfiguration;

class OutputBuilder {
    private static final SBMLConfiguration CONFIG = SBMLConfiguration.getConfiguration();
    private static final String DEFAULT_NAME = "Output";

    public ListOf<Output> build(String speciesId, int id) {
        ListOf<Output> outputs = new ListOf<>(CONFIG.getLevel(), CONFIG.getVersion());
        Output o = new Output(CONFIG.getLevel(), CONFIG.getVersion());
        o.setId(DEFAULT_NAME + id);
        o.setQualitativeSpecies(speciesId);
        o.setTransitionEffect(OutputTransitionEffect.assignmentLevel);
        outputs.add(o);
        return outputs;
    }
}
