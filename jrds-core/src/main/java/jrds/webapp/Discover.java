package jrds.webapp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import jrds.PropertiesManager;
import jrds.Util;
import jrds.configuration.ConfigObjectFactory;
import jrds.configuration.ConfigType;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;
import jrds.probe.IndexedProbe;

/**
 * Servlet implementation class AutoDetect
 */
@ServletSecurity
public class Discover extends JrdsServlet {
    static final private Logger logger = LoggerFactory.getLogger(Discover.class);

    private static final String CONTENT_TYPE = "application/xml";
    private static final long serialVersionUID = 1L;

    public static final class ProbeDescSummary {
        public final Class<?> clazz;
        public final String name;
        public final Map<String, String> specifics = new HashMap<String, String>();
        public final boolean isIndexed;

        ProbeDescSummary(JrdsDocument probdesc, ClassLoader cl) throws ClassNotFoundException {
            JrdsElement root = probdesc.getRootElement();
            JrdsElement buffer = root.getElementbyName("probeClass");
            String probeClass = buffer == null ? null : buffer.getTextContent().trim();
            clazz = cl.loadClass(probeClass);
            isIndexed = IndexedProbe.class.isAssignableFrom(clazz);
            buffer = probdesc.getRootElement().getElementbyName("name");
            name = buffer == null ? null : buffer.getTextContent();
            for(JrdsElement specificElement: root.getChildElementsByName("specific")) {
                specifics.put(specificElement.getAttribute("name"), specificElement.getTextContent().trim());
            }
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {

        String hostname = request.getParameter("host");
        if(hostname == null || "".equals(hostname.trim())) {
            try {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "hostname to scan not provided");
            } catch (IOException e) {
            }
            return;
        }
        hostname = hostname.trim();

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
        } catch (IOException ex) {
            logger.warn("failed to send body: " + ex.getMessage());
        } catch (ParserConfigurationException | TransformerException ex) {
            logger.error("Error rendering content: " + ex.getMessage(), ex);
        }
    }

    private Document generate(String hostname, Map<String, JrdsDocument> probdescs, HttpServletRequest request) throws ParserConfigurationException {

        DocumentBuilder dbuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        JrdsDocument hostDom = new JrdsDocument(dbuilder.newDocument());

        JrdsElement hostEleme = hostDom.doRootElement("host", "name=" + hostname);

        String[] tags = request.getParameterValues("tag");
        if(tags != null)
            for(String tag: tags) {
                JrdsElement tagElem = hostEleme.addElement("tag");
                tagElem.setTextContent(tag);
            }

        List<String> probeDescsName = new ArrayList<String>();
        probeDescsName.addAll(probdescs.keySet());
        Collections.sort(probeDescsName);

        for(DiscoverAgent da: getHostsList().getDiscoverAgent()) {
            da.setTimeout(getPropertiesManager().timeout);
            try {
                if(da.exist(hostname, request)) {
                    da.addConnection(hostEleme, request);
                    da.discoverPre(hostname, hostEleme, probdescs, request);
                    for(String probeDescName: probeDescsName) {
                        JrdsDocument probeDescDocument = probdescs.get(probeDescName);
                        // for(JrdsDocument probeDescDocument:
                        // probdescs.values()) {
                        try {
                            ProbeDescSummary summary = new ProbeDescSummary(probeDescDocument, getPropertiesManager().extensionClassLoader);

                            // Don't discover if asked to don't do
                            if(summary.specifics.get("nodiscover") != null)
                                continue;

                            boolean valid = false;
                            for(Class<?> c: da.validClasses) {
                                valid |= c.isAssignableFrom(summary.clazz);
                            }
                            if(valid && da.isGoodProbeDesc(summary)) {
                                da.addProbe(hostEleme, summary, request);
                            }
                        } catch (Exception e) {
                            logger.error("Can't try to discover probe " + probeDescName + ": " + e.getMessage());
                        }
                    }
                    da.discoverPost(hostname, hostEleme, probdescs, request);
                }
            } catch (NoClassDefFoundError e) {
                logger.error("Discover agent " + da + " failed to load class with " + da.getClass().getClassLoader(), e);
            } catch (Throwable e) {
                logger.error("Discover failed for " + da + ": " + e, e);
            }
        }
        return hostDom;
    }

}
