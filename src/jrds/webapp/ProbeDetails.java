/*##########################################################################
 _##
 _##  $Id: Graph.java 236 2006-03-02 15:59:34 +0100 (jeu., 02 mars 2006) fbacchella $
 _##
 _##########################################################################*/

package jrds.webapp;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import jrds.Probe;
import jrds.Util;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

public class ProbeDetails extends JrdsServlet {
	static final private Logger logger = Logger.getLogger(ProbeDetails.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException{
		res.setContentType("application/xhtml+xml");
		res.addHeader("Cache-Control", "no-cache");

		ParamsBean params = getParamsBean(req);
		Probe<?,?> probe = params.getProbe();
		
		OutputStream os = null;
		try {
			os = res.getOutputStream();
		} catch (IOException e) {
			logger.error("No output stream availaible");
		}

		if(probe != null) {
			dump(probe, os);
		}
	}
	
	public void dump(Probe<?,?> probe, OutputStream os) {
		try {
			Document xmlDesc = probe.dumpAsXml(true);
			Util.serialize(xmlDesc, os, jrds.xmlResources.ResourcesLocator.getResourceUrl("probe.xsl"), null);

		} catch (ParserConfigurationException e) {
			logger.fatal("Fatal parser error: " + e);
		} catch (TransformerConfigurationException e) {
			logger.error("probe.xsl is invalid: " + e.getMessage());
		} catch (TransformerException e) {
			logger.error("probe.xsl is invalid: " + e.getMessage());
		} catch (IOException e) {
			logger.error("Unable to flush to " + os);
		}

	}
}
