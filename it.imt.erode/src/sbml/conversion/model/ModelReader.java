package sbml.conversion.model;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.ext.qual.QualModelPlugin;

import sbml.conversion.qualmodel.QualModelManager;

class ModelReader extends ModelConverter {

    public ModelReader(Model model) {
        super(model);
        this.qualModelConverter = QualModelManager.create(this.tryGetQualModel());
    }

    private QualModelPlugin tryGetQualModel() {
        try {
            QualModelPlugin qualModelPlugin = 
                (QualModelPlugin) model.getExtension(EXTENSION_NAME);
            if (qualModelPlugin == null)
                throw new NullPointerException();
            else
                return qualModelPlugin;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid input, the SBML-model " +
                "is not an SBML-qual model");
        }
    }
}
