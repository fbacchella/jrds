package jrds.webapp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;

import jrds.Util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class GetDiscoverHtmlCode extends JrdsServlet {

    private static final String CONTENT_TYPE = "application/xml";
    private static final long serialVersionUID = 1L;

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        DocumentBuilder dbuilder;
        try {
            dbuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document hostDom = dbuilder.newDocument();
            Element root = hostDom.createElement("div");
            hostDom.appendChild(root);
            for(DiscoverAgent da: getHostsList().getDiscoverAgent()) {
                da.doHtmlDiscoverFields(hostDom);
            }
            resp.setContentType(CONTENT_TYPE);

            Map<String, String> prop = new HashMap<String, String>(1);
            prop.put(OutputKeys.OMIT_XML_DECLARATION, "no");
            prop.put(OutputKeys.INDENT, "yes");
            prop.put("{http://xml.apache.org/xslt}indent-amount", "4");
            prop.put(OutputKeys.DOCTYPE_PUBLIC, "-//jrds//DTD Host//EN");
            prop.put(OutputKeys.DOCTYPE_SYSTEM, "urn:jrds:host");
            Util.serialize(hostDom, resp.getOutputStream(), null, prop);
        } catch (ParserConfigurationException e) {
        } catch (TransformerException e) {
        }    
    }

}
