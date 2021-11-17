package sbml.conversion.model;

import org.jetbrains.annotations.NotNull;
import org.sbml.jsbml.Model;

import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;

public class ModelManager {
    public static IModelConverter create(
    	@NotNull Model model) {
    	
        return new ModelReader(model);
    }

    public static IModelConverter create(
    	@NotNull IBooleanNetwork booleanNetwork) {

        return new ModelWriter(booleanNetwork);
    }
}
