package jrds.webapp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;

import jrds.PropertiesManager;
import jrds.Util;
import jrds.configuration.ConfigObjectFactory;
import jrds.configuration.ConfigType;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

/**
 * Servlet implementation class AutoDetect
 */
public class Discover extends JrdsServlet {
    static final private Logger logger = Logger.getLogger(Discover.class);

    private static final String CONTENT_TYPE = "application/xml";
    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {

        String hostname = request.getParameter("host");

        PropertiesManager pm = getPropertiesManager();

        ConfigObjectFactory conf = new ConfigObjectFactory(pm);

        try {
            Document hostDom = generate(hostname, conf.getNodeMap(ConfigType.PROBEDESC), request);
            response.setContentType(CONTENT_TYPE);
            response.addHeader("Cache-Control", "no-cache");

            Map<String, String> prop = new HashMap<String, String>(1);
            prop.put(OutputKeys.OMIT_XML_DECLARATION, "no");
            prop.put(OutputKeys.INDENT, "yes");
            prop.put("{http://xml.apache.org/xslt}indent-amount", "4");
            prop.put(OutputKeys.DOCTYPE_PUBLIC, "-//jrds//DTD Host//EN");
            prop.put(OutputKeys.DOCTYPE_SYSTEM, "urn:jrds:host");
            Util.serialize(hostDom, response.getOutputStream(), null, prop);
        } catch (IOException e) {
            logger.error(e);
        } catch (ParserConfigurationException e) {
            logger.error(e);
        } catch (TransformerException e) {
            logger.error(e);
        }
    }

    private Document generate(String hostname, Map<String, JrdsDocument> probdescs, HttpServletRequest request) throws IOException, ParserConfigurationException {

        DocumentBuilder dbuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        JrdsDocument hostDom = new JrdsDocument(dbuilder.newDocument());

        JrdsElement hostEleme = hostDom.doRootElement("host", "name=" + hostname);

        String[] tags = request.getParameterValues("tag");
        if(tags != null)
            for(String tag: tags) {
                JrdsElement tagElem = hostEleme.addElement("tag");
                tagElem.setTextContent(tag);
            }

        for(DiscoverAgent da: getHostsList().getDiscoverAgent()) {
            try {
                da.discover(hostname, hostEleme, probdescs, request);
            } catch (Exception e) {
                logger.error("Discover failed for " + da + ": " + e);
            }
        }
        return hostDom;
    }

}
