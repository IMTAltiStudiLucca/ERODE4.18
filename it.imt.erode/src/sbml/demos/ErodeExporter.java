package sbml.demos;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.SBMLDocument;

import it.imt.erode.importing.booleannetwork.GUIBooleanNetworkImporter;
import sbml.conversion.document.ISBMLConverter;
import sbml.conversion.document.SBMLManager;

public class ErodeExporter {

    /**
     * Small demo creating a .ode file that can be read by ERODE
     * @param args - Not needed
     * @throws IOException
     * @throws XMLStreamException
     */

    public static void main(String[] args) throws IOException, XMLStreamException {
        //String path = "D:/Repositories/SBML-Converter-for-ERODE/src/main/resources/sbml/demos/DemoNetwork.sbml";
    	//String path="demo/DemoNetwork.sbml";
    	//String path="demo/CorticalAreaDevelopment.sbml";
    	//String path="demo/DemoSBMLFile.sbml";
    	String path="demo/Trp_reg.sbml";
        SBMLDocument sbmlDocument = (SBMLDocument) SBMLManager.read(path);


        ISBMLConverter sbmlConverter = SBMLManager.create(sbmlDocument);

        GUIBooleanNetworkImporter guiBooleanNetworkImporter = sbmlConverter.getGuiBnImporter();
        System.out.println(guiBooleanNetworkImporter.getBooleanNetwork().getSpecies().toString());
        GUIBooleanNetworkImporter.printToBNERODEFIle(guiBooleanNetworkImporter.getBooleanNetwork(),guiBooleanNetworkImporter.getInitialPartition(),
        		//"demo/DemoNetworkNew4.ode",
        		//"demo/CorticalAreaDevelopmentNew2.ode",
        		//"demo/DemoSBMLFile2.ode",
        		"demo/Trp_reg3.ode",
                null, true, null, null, false,null);
    }
}
