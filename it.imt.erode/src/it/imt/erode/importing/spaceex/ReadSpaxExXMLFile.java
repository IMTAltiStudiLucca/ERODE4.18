package it.imt.erode.importing.spaceex;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class ReadSpaxExXMLFile {

	public static void main(String argv[]) {

		try {

			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();

			SpaceExHandler handler = new SpaceExHandler("core_component");

			saxParser.parse("xmlexamples/mcs_8.xml", handler);
			System.out.println("All flows have been read: "+handler.getVariableToFlow().size());
			System.out.println("pcc: "+handler.getPcc());


		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}