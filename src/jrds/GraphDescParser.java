package jrds;

// ----------------------------------------------------------------------------
// $Id$

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import jrds.snmp.TargetFactory;

import org.apache.log4j.Logger;
import org.snmp4j.Target;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
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

    private final static SAXParserFactory saxFactory = SAXParserFactory.newInstance();
    private final static TargetFactory targetFactory = TargetFactory.getInstance();
    static {
        saxFactory.setNamespaceAware(true);
        saxFactory.setValidating(true);
    }

    String xmlRessourceName;

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
    }

    public void endElement(String namespaceURI, String localName, String qName)
    {
    }
}
