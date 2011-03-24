package jrds.webapp;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
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
import jrds.factories.Loader;
import jrds.factories.xml.JrdsNode;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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

		Loader l;
		try {
			l = new Loader();
			URL graphUrl = getClass().getResource("/desc");
			if(graphUrl != null)
				l.importUrl(graphUrl);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Loader initialisation error",e);
		}

		logger.debug("Scanning " + pm.libspath + " for probes libraries");
		for(URL lib: pm.libspath) {
			logger.info("Adding lib " + lib);
			l.importUrl(lib);
		}

		l.importDir(pm.configdir);

		try {
			Document hostDom = generate(hostname, l.getRepository(Loader.ConfigType.PROBEDESC), request);
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

	private Document generate(String hostname, Map<String,JrdsNode> probdescs, HttpServletRequest request) throws IOException, ParserConfigurationException {
	    
		DocumentBuilder dbuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document hostDom = dbuilder.newDocument();

		Element hostEleme = hostDom.createElement("host");
		hostEleme.setAttribute("name", hostname);
		hostDom.appendChild(hostEleme);

		String[] tags = request.getParameterValues("tag");
		if(tags != null)
			for(String tag: tags) {
				Element tagElem = hostDom.createElement("tag");
				tagElem.setTextContent(tag);
				hostEleme.appendChild(tagElem);
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
	
//	@SuppressWarnings("unchecked")
//    private void runDiscoverClass(String discoverClass, String hostname, Document hostDom,
//            Collection<JrdsNode> probdescs, HttpServletRequest request) {
//        Class<? extends DiscoverAgent> dac;
//        try {
//            dac = (Class<? extends DiscoverAgent>) getPropertiesManager().extensionClassLoader.loadClass("jrds.snmp.SnmpDiscoverAgent");
//        } catch (ClassNotFoundException e1) {
//            logger.error("Discover class not found: " + discoverClass);
//            return;
//        }
//        try {
//            DiscoverAgent da = dac.getConstructor().newInstance();
//            da.discover(hostname, hostDom, probdescs, request); 
//        } catch (Exception e) {
//            logger.error("Generation Failed: ",e);
//        }
//	    
//	}
}
