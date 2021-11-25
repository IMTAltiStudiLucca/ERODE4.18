package sbml.conversion.document;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.eclipse.ui.console.MessageConsoleStream;
import org.jetbrains.annotations.NotNull;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBase;

import it.imt.erode.booleannetwork.interfaces.IBooleanNetwork;
import it.imt.erode.importing.booleannetwork.GuessPrepartitionBN;

public class SBMLManager {

    public static SBase read(String path) throws IOException, XMLStreamException {
        return SBMLReader.read(new File(path));
    }

    public static ISBMLConverter create(@NotNull SBMLDocument sbmlDocument,GuessPrepartitionBN guessPrep, MessageConsoleStream out, BufferedWriter bwOut,String nameFromFile) throws IOException {
        return new DocumentReader(sbmlDocument,guessPrep,out,bwOut,nameFromFile);
    }

    public static ISBMLConverter create(@NotNull IBooleanNetwork booleanNetwork) {
        return new DocumentWriter(booleanNetwork);
    }

}
