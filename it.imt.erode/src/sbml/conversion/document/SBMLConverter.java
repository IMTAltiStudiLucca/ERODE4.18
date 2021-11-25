package sbml.conversion.document;

import java.io.IOException;

import org.jetbrains.annotations.NotNull;
import org.sbml.jsbml.SBMLDocument;

import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;
import it.imt.erode.importing.InfoBooleanNetworkImporting;
import it.imt.erode.importing.booleannetwork.GUIBooleanNetworkImporter;
import it.imt.erode.importing.booleannetwork.GuessPrepartitionBN;
import sbml.configurations.SBMLConfiguration;
import sbml.conversion.model.IModelConverter;

abstract class SBMLConverter implements ISBMLConverter {

    protected static final SBMLConfiguration CONFIG = SBMLConfiguration.getConfiguration();

    protected IModelConverter modelConverter;

    protected SBMLDocument sbmlDocument;

    protected InfoBooleanNetworkImporting infoImporting;
    protected GUIBooleanNetworkImporter guiBnImporter;
    protected IBooleanNetwork booleanNetwork;
    protected GuessPrepartitionBN guessPrepartitionOnInputs=GuessPrepartitionBN.INPUTS;

    public SBMLConverter(@NotNull SBMLDocument sbmlDocument,GuessPrepartitionBN guessPrep) throws IOException {
        this.sbmlDocument = sbmlDocument;
        this.guessPrepartitionOnInputs=guessPrep;
    }

    public SBMLConverter(IBooleanNetwork booleanNetwork) {
        this.booleanNetwork = booleanNetwork;
    }

    @Override
    public GUIBooleanNetworkImporter getGuiBnImporter() {
        return this.guiBnImporter;
    }

    @Override
    public IModelConverter getSBMLModel() {
        return this.modelConverter;
    }

    @Override
    public InfoBooleanNetworkImporting getInfoImporting() {
        return this.infoImporting;
    }

    @Override
    public SBMLDocument getSbmlDocument() {
        return this.sbmlDocument;
    }
    
}
