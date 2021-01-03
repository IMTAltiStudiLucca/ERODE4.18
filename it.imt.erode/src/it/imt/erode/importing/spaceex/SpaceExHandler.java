package it.imt.erode.importing.spaceex;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class SpaceExHandler extends org.xml.sax.helpers.DefaultHandler{

	private boolean flow = false;
	private LinkedHashMap<String, String> variableToFlow;
	private LinkedHashSet<String> pcc;
	
	private boolean core_component = false;
	
	private boolean invariant = false;
	private boolean skipToAndInv=false;
	private String mainComponent;
	
	private String currentVar=null;

	public SpaceExHandler(String mainComponent) {
		this.mainComponent=mainComponent;
	}

	public LinkedHashMap<String, String> getVariableToFlow() {
		return variableToFlow;
	}
	
	public LinkedHashSet<String> getPcc() {
		return pcc;
	}
	
	public void startElement(String uri, String localName,String qName,
                Attributes attributes) throws SAXException {
		if(qName.equalsIgnoreCase("component")) {
			String id = attributes.getValue("id");
			if(id.equals(mainComponent)) {
				core_component=true;
				pcc=new LinkedHashSet<String>();
			}
		}
		if(core_component && qName.equalsIgnoreCase("flow")) {
			//System.out.println("Start Element :" + qName);
			variableToFlow=new LinkedHashMap<String, String>();
			flow = true;
		}
		if(core_component && qName.equalsIgnoreCase("param")) {
			String name = attributes.getValue("name");
			pcc.add(name);
		}
		if(core_component && qName.equalsIgnoreCase("invariant")) {
			//String name = attributes.getValue("name");
			//pcc.add(name);
			invariant=true;
			skipToAndInv=false;
		}
	}

	public void endElement(String uri, String localName,
		String qName) throws SAXException {

		if (core_component && qName.equalsIgnoreCase("component")) {
			core_component=false;
		}
		else if (flow && qName.equalsIgnoreCase("flow")) {
			//System.out.println("End Element :" + qName);
			flow = false;
			currentVar=null;
		}
		else if(invariant && qName.equalsIgnoreCase("invariant")) {
			invariant=false;
			skipToAndInv=false;
		}

	}

	public void characters(char ch[], int start, int length) throws SAXException {

		if (flow) {
			//System.out.println("Flow:" + new String(ch, start, length));
			//flow = false;
			String str =new String(ch, start, length).trim();
			if(str.startsWith("&")) {
				//System.out.println("Skip &");
				currentVar=null;
			}
			else if(str.startsWith("t'")) {
				//System.out.println("Skip t");
			}
			else if(str.length()==0) {
				//System.out.println("Skip empty line");
			}
			else {
				if(str.indexOf("'")<0) {
					//System.out.println(str);
					if(currentVar!=null) {
						String prevFlow = variableToFlow.get(currentVar);
						variableToFlow.put(currentVar, prevFlow+str);
					}
					else {
						throw new SAXException("Unrecognized characters: "+str);
					}
				}
				else {
					String s = str.substring(0,str.indexOf("'"));
					String flow = str.substring(str.indexOf("==")+3);
					//System.out.println(s+" = "+flow);
					variableToFlow.put(s, flow);
					pcc.remove(s);
					currentVar=s;
				}
			}
		}
		else if(invariant) {
			String str =new String(ch, start, length).trim();
			if(skipToAndInv) {
				if(str.startsWith("&")) {
					skipToAndInv=false;
				}
			}
			else{
				int space = str.indexOf(' ');
				if(space>0) {
					str=str.substring(0,space);
					skipToAndInv=true;
				}
				pcc.remove(str);
			}
			
			//System.out.println("I:"+str);
		}
	}
	
}
