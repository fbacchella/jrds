/*##########################################################################
 _##
 _##  $Id: Graph.java 236 2006-03-02 15:59:34 +0100 (jeu., 02 mars 2006) fbacchella $
 _##
 _##########################################################################*/

package jrds.webapp;

import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.ErrorListener;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import jrds.Probe;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

public class ProbeDetails extends HttpServlet {
	static final private Logger logger = Logger.getLogger(ProbeDetails.class);
	static final private ErrorListener el = new ErrorListener() {

		public void error(TransformerException e) throws TransformerException {
			logger.error("Invalid xsl: " + e.getMessageAndLocation());
		}
		public void fatalError(TransformerException e) throws TransformerException {
			logger.fatal("Invalid xsl: " + e.getMessageAndLocation());
		}
		public void warning(TransformerException e) throws TransformerException {
			logger.warn("Invalid xsl: " + e.getMessageAndLocation());
		}

	};

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException{
		res.setContentType("text/html");
		res.addHeader("Cache-Control", "no-cache");

		ParamsBean params = new ParamsBean(req);
		Probe probe = params.getProbe();
		
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
	
	public void dump(Probe probe, OutputStream os) {
		try {
			Document xmlDesc = probe.dumpAsXml(true);
			Source source = new DOMSource(xmlDesc);

			TransformerFactory tFactory = TransformerFactory.newInstance();
			tFactory.setErrorListener(el);
			Source stylesource = new StreamSource(jrds.xmlResources.ResourcesLocator.getResource("probe.xsl"));
			Transformer transformer = tFactory.newTransformer(stylesource);


			StreamResult result = new StreamResult(os);
			transformer.transform(source, result);
			os.flush();
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
