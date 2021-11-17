package sbml.conversion.document;

import java.io.File;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.jetbrains.annotations.NotNull;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBase;

import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;

public class SBMLManager {

    public static SBase read(String path) throws IOException, XMLStreamException {
        return SBMLReader.read(new File(path));
    }

    public static ISBMLConverter create(@NotNull SBMLDocument sbmlDocument) throws IOException {
        return new DocumentReader(sbmlDocument);
    }

    public static ISBMLConverter create(@NotNull IBooleanNetwork booleanNetwork) {
        return new DocumentWriter(booleanNetwork);
    }

}
