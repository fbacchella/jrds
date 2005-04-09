package jrds;

// ----------------------------------------------------------------------------
// $Id$

import java.awt.Color;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import jrds.GraphDesc.ConsFunc;
import jrds.GraphDesc.GraphType;
import jrds.snmp.TargetFactory;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;


/**
 * <p>Title: </p>
 *
 * <p>Description: This class read an XML file and return a GraphDesc </p>
 *
 * <p>Copyright: Copyright (c) 2005</p>
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public class GraphDescParser extends DefaultHandler {
	
	static private final Logger logger = JrdsLogger.getLogger(HostConfigParser.class);
	
	//Tag names
	private final static String GRAPHDESC="graphdesc";
	private final static String ADD="add";
	private final static String ADD_NAME="name";
	private final static String ADD_TYPE="type";
	private final static String ADD_COLOR="color";
	private final static String ADD_DSNAME="dsname";
	private final static String ADD_RPN="rpn";

	private final static String FILENAME="filename";
	private final static String GRAPHTITLE="graphtitle";
	private final static String VERTICALLABEL="verticallabel";
	
	private final static SAXParserFactory saxFactory = SAXParserFactory.newInstance();
	private final static TargetFactory targetFactory = TargetFactory.getInstance();
	static {
		saxFactory.setNamespaceAware(true);
		saxFactory.setValidating(true);
	}
	
	private String xmlRessourceName;
	private GraphDesc currGd;
	
	public GraphDescParser(String xmlRessourceName)
	{
		this.xmlRessourceName = xmlRessourceName;
	}
	
	public GraphDesc parse()
	{
		//Class.getResourceAsStream("/monfichier.prop");
		GraphDesc retValue = new GraphDesc();
		try {
			SAXParser saxParser = saxFactory.newSAXParser();
			logger.debug("Parsing " + xmlRessourceName + " with " + saxParser);
			saxParser.parse(this.getClass().getResourceAsStream(xmlRessourceName), this);
		} catch (Exception e) {
			logger.warn("error during parsing of host config file " + xmlRessourceName +
					": ", e);
		}
		return retValue;
	}
	/**
	 * Receive notification of the start of an element.
	 * @param namespaceURI - The Namespace URI, or the empty string if the element has no Namespace URI or if Namespace processing is not being performed.
	 * @param localName - The local name (without prefix), or the empty string if Namespace processing is not being performed.
	 * @param qName - The qualified name (with prefix), or the empty string if qualified names are not available.
	 * @param atts - The attributes attached to the element. If there are no attributes, it shall be an empty Attributes object.
	 */
	public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
	{
		if(GRAPHDESC.equals(localName	)){
			currGd = new GraphDesc();
		}
		if(ADD.equals(localName)) {
			String name = atts.getValue(ADD_NAME);
	         String dsName = atts.getValue(ADD_DSNAME);
	         String rpn = atts.getValue(ADD_RPN);
	         GraphType graphType;
	         Color color = Color.getColor(atts.getValue(ADD_COLOR));
	         String legend;
	         ConsFunc cf;
		}
	}
	
	public void endElement(String namespaceURI, String localName, String qName)
	{
	}
}
