/*##########################################################################
 _##
 _##  $Id: Graph.java 236 2006-03-02 15:59:34 +0100 (jeu., 02 mars 2006) fbacchella $
 _##
 _##########################################################################*/

package jrds.webapp;

import java.io.IOException;

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
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		res.setContentType("text/html");
		res.addHeader("Cache-Control", "no-cache");

		ParamsBean params = new ParamsBean(req);
		Probe probe = params.getProbe();

		if(probe != null)
			try {
				TransformerFactory tFactory = TransformerFactory.newInstance();
				tFactory.setErrorListener(el);
				Source stylesource = new StreamSource(jrds.xmlResources.ResourcesLocator.getResource("probe.xsl"));
				Transformer transformer = tFactory.newTransformer(stylesource);

				Document xmlDesc = probe.dumpAsXml(true);
				Source source = new DOMSource(xmlDesc);

				StreamResult result = new StreamResult(res.getOutputStream());
				transformer.transform(source, result);
				res.getOutputStream().flush();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TransformerConfigurationException e) {
				logger.error("probe.xsl is invalid: " + e.getMessage());
			} catch (TransformerException e) {
				logger.error("probe.xsl is invalid: " + e.getMessage());
			}
	}
}
