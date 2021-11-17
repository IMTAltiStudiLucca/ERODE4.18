package sbml.conversion.model;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.ext.qual.QualModelPlugin;

import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;
import sbml.configurations.SBMLConfiguration;
import sbml.conversion.qualmodel.QualModelManager;

class ModelWriter extends ModelConverter {
    private static final SBMLConfiguration CONFIG = 
        SBMLConfiguration.getConfiguration();

    public ModelWriter(IBooleanNetwork booleanNetwork) {
        super(booleanNetwork);
        this.model = new Model(CONFIG.getLevel(), 
            CONFIG.getVersion());
        this.model.setId(this.name);
        this.qualModelConverter = QualModelManager.create(
            booleanNetwork, model);
        this.packageSBML();
    }

    private void packageSBML() {
        QualModelPlugin qualModelPlugin = 
            qualModelConverter.getSbmlQualModel();
        model.addExtension(EXTENSION_NAME,qualModelPlugin);
    }
}
