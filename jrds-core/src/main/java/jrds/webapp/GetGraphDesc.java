package jrds.webapp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import jrds.Graph;
import jrds.GraphDesc;
import jrds.HostsList;
import jrds.Util;

public class GetGraphDesc extends JrdsServlet {

    static final private Logger logger = LoggerFactory.getLogger(GetGraphDesc.class);
    static final private String CONTENT_TYPE = "application/xml";

    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {

        HostsList hl = getHostsList();

        ParamsBean p = new ParamsBean(req, hl, "host", "graphname");
        Graph graph = p.getGraph(this);

        if(graph == null) {
            res.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid graph id");
            return;
        }
        GraphDesc gd = graph.getNode().getGraphDesc();

        try {
            Document d = gd.dumpAsXml();
            res.setContentType(CONTENT_TYPE);
            Map<String, String> prop = new HashMap<String, String>(5);
            prop.put(OutputKeys.OMIT_XML_DECLARATION, "no");
            prop.put(OutputKeys.INDENT, "yes");
            prop.put("{http://xml.apache.org/xslt}indent-amount", "4");
            prop.put(OutputKeys.DOCTYPE_PUBLIC, "-//jrds//DTD Graph Description//EN");
            prop.put(OutputKeys.DOCTYPE_SYSTEM, "urn:jrds:graphdesc");
            Util.serialize(d, res.getOutputStream(), null, prop);
        } catch (ParserConfigurationException | TransformerException e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            logger.error("Can't generate graph description: " + e, e);
        }
    }
}
