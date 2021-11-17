package sbml.conversion.transitions;

import java.util.HashSet;

import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.ext.qual.Input;
import org.sbml.jsbml.ext.qual.InputTransitionEffect;

import it.imt.erode.booleannetwork.updatefunctions.BooleanUpdateFunctionExpr;
import it.imt.erode.booleannetwork.updatefunctions.IUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.NotBooleanUpdateFunction;
import it.imt.erode.booleannetwork.updatefunctions.ReferenceToNodeUpdateFunction;
import sbml.configurations.SBMLConfiguration;

class InputBuilder {
    private static final SBMLConfiguration CONFIG = SBMLConfiguration.getConfiguration();
    private static final String DEFAULT_NAME = "Input";

    private int id;
    public InputBuilder() {
        this.id = 0;
    }

    public HashSet<ReferenceToNodeUpdateFunction> ExtractInputs(IUpdateFunction updateFunction) {
        if(updateFunction.getClass().equals(ReferenceToNodeUpdateFunction.class)) {
            HashSet<ReferenceToNodeUpdateFunction> hashSet = new HashSet<>();
            hashSet.add((ReferenceToNodeUpdateFunction) updateFunction);
            return hashSet;
        }
        else if(updateFunction.getClass().equals(BooleanUpdateFunctionExpr.class)) {
            BooleanUpdateFunctionExpr bExpr = (BooleanUpdateFunctionExpr) updateFunction;
            return Merge(ExtractInputs(bExpr.getFirst()),ExtractInputs(bExpr.getSecond()));
        }
        else if(updateFunction.getClass().equals(NotBooleanUpdateFunction.class)) {
            NotBooleanUpdateFunction notExpr = (NotBooleanUpdateFunction) updateFunction;
            return ExtractInputs(notExpr.getInnerUpdateFunction());
        }
        else {
            return new HashSet<>();
        }
    }

    private HashSet<ReferenceToNodeUpdateFunction> Merge(HashSet<ReferenceToNodeUpdateFunction> a,
                                                         HashSet<ReferenceToNodeUpdateFunction> b) {
        a.addAll(b);
        return a;
    }

    public ListOf<Input> buildAll(IUpdateFunction updateFunction) {
        HashSet<ReferenceToNodeUpdateFunction> references = ExtractInputs(updateFunction);
        ListOf<Input> inputs = new ListOf<>(CONFIG.getLevel(),CONFIG.getVersion());
        for (ReferenceToNodeUpdateFunction r : references) {
            Input i = CreateInput(r, id);
            inputs.add(i);
            id++;
        }
        return inputs;
    }

    private Input CreateInput(ReferenceToNodeUpdateFunction r, int id) {
        Input input = new Input(CONFIG.getLevel(),CONFIG.getVersion());
        input.setId(DEFAULT_NAME+id);
        input.setQualitativeSpecies(r.toString());
        input.setTransitionEffect(InputTransitionEffect.none);
        return input;
    }
}
