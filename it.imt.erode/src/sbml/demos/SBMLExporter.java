package sbml.demos;

import java.io.File;
import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLWriter;

import it.imt.erode.importing.booleannetwork.GUIBooleanNetworkImporter;
import it.imt.erode.importing.booleannetwork.GuessPrepartitionBN;
import sbml.conversion.document.ISBMLConverter;
import sbml.conversion.document.SBMLManager;

public class SBMLExporter {
    /**
     * Small demo converting an .sbml file to .ode format
     * and back
     * @param args
     */
    public static void main(String[] args) throws IOException, XMLStreamException {
    	String path = "D:/Repositories/SBML-Converter-for-ERODE/src/main/resources/sbml/demos/DemoNetwork.sbml";
        SBMLDocument sbmlDocument = (SBMLDocument) SBMLManager.read(path);


        ISBMLConverter converter = SBMLManager.create(sbmlDocument,GuessPrepartitionBN.INPUTS,null,null,null);
        GUIBooleanNetworkImporter guiBooleanNetworkImporter = converter.getGuiBnImporter();
        GUIBooleanNetworkImporter.printToBNERODEFIle(guiBooleanNetworkImporter.getBooleanNetwork(),guiBooleanNetworkImporter.getInitialPartition(),
                "DemoNetwork.ode", null, true, null, null, false,null);
        //--------------------------------------------------------------------------------------
        converter = SBMLManager.create(guiBooleanNetworkImporter.getBooleanNetwork());
        sbmlDocument = converter.getSbmlDocument();
        print(sbmlDocument);
    }

    private static void print(SBMLDocument sbmlDocument) {
        String writePath = System.getProperty("user.dir");
        System.out.println("Working Directory = " + writePath);
        try {
            File sbmlFile = new File("DemoSBMLFile.sbml");
            if(sbmlFile.createNewFile()) {
                System.out.println("Created file: " + sbmlFile.getName() + "at path: " + sbmlFile.getPath());
            }
            System.out.println("Writing to file...");
            SBMLWriter.write(sbmlDocument, sbmlFile, "SBMLConverter", "1.0");
            System.out.println("Finished");
        } catch (IOException | XMLStreamException e) {
            System.out.println("An error occurred");
            e.printStackTrace();
        }
    }
}
