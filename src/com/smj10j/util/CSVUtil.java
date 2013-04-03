package com.smj10j.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.smj10j.conf.FatalException;
import com.smj10j.model.APIRequest;

public abstract class CSVUtil {
	
	private static Logger logger = Logger.getLogger(CSVUtil.class);
	
	private static List<String> SHOW_NODE_ATTRIBUTES_AS_COLUMNS = new ArrayList<String>();
	static {
		SHOW_NODE_ATTRIBUTES_AS_COLUMNS.add("Invoice");
	}
	
	private static List<String> IGNORE_NODES = new ArrayList<String>();
	static {
		IGNORE_NODES.add("#text");
	}	
	private static List<String> HIDE_NODE_NAMES = new ArrayList<String>();
	static {
		HIDE_NODE_NAMES.add("Site");
		HIDE_NODE_NAMES.add("Purchases");
	}	
	
	public static <T> void marshal(APIRequest request, JAXBElement<T> responseRoot, Writer writer) throws FatalException, IOException {

		//get xml output in the writer
		Writer xmlWriter = new StringWriter();
		JAXBUtil.marshal(request, responseRoot, xmlWriter);


		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();

			logger.debug("Converting xml to csv - " + xmlWriter.toString());
			Document dom = db.parse(new ByteArrayInputStream(xmlWriter.toString().getBytes()));
			Element documentElement = dom.getDocumentElement();

			writer.write(parseElement(documentElement));
			
		} catch (ParserConfigurationException e) {
			throw new FatalException(e);
		} catch (SAXException e) {
			throw new FatalException(e);
		}			
	}

	private static String parseElement(Node node) {

		String csv = "";
		if(node != null) {
			NodeList elems = node.getChildNodes();
			Map<String,Boolean> seenNodes = new HashMap<String, Boolean>();
			if(elems != null) {
				for(int i = 0 ; i < elems.getLength(); i++) {
	
					//get the child
					Node child = elems.item(i);
					NamedNodeMap attributes = child.getAttributes();
					
					if(IGNORE_NODES.contains(child.getNodeName()))
						continue;
					
					if(!SHOW_NODE_ATTRIBUTES_AS_COLUMNS.contains(child.getNodeName())) {
						//normal row-based output
						
						//output a header row
						if(!seenNodes.containsKey(child.getNodeName())) {
							seenNodes.put(child.getNodeName(), true);
							csv+= child.getNodeName();
							if(attributes != null) {
								for(int j = 0; j < attributes.getLength(); j++) {
									Node attribute = attributes.item(j);
									csv+= ",";
									csv+= attribute == null ? "Unknown" : attribute.getNodeName();
								}
							}
							csv+= "\n";
						}
						
						//output its name and attributes
						if(!HIDE_NODE_NAMES.contains(child.getNodeName()))
							csv+= child.getNodeName();
						if(attributes != null) {
							for(int j = 0; j < attributes.getLength(); j++) {
								Node attribute = attributes.item(j);
								csv+= ",";
								csv+= attribute == null ? "" : attribute.getNodeValue();
							}
						}
						if(!HIDE_NODE_NAMES.contains(child.getNodeName()) || attributes != null)
							csv+= "\n";
					}else {
						//column-based output
												
						//output its name and attributes
						if(!HIDE_NODE_NAMES.contains(child.getNodeName()))
							csv+= child.getNodeName() + "\n";
						if(attributes != null) {
							for(int j = 0; j < attributes.getLength(); j++) {
								Node attribute = attributes.item(j);
								csv+= attribute == null ? " - " : attribute.getNodeName();
								csv+= "\n";
								csv+= attribute == null ? "" : attribute.getNodeValue();
								csv+= "\n";
							}
						}				
					}
					
					//else
					csv+= parseElement(child);
				}
			}		
		}
		return csv;
	}
}
