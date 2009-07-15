/*##########################################################################
 _##
 _##  $Id: Graph.java 360 2006-08-23 09:31:58 +0000 (mer., 23 août 2006) fbacchella $
 _##
 _##########################################################################*/

package jrds.webapp;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import javax.media.jai.JAI;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.log4j.Logger;
import org.snmp4j.transport.DefaultUdpTransportMapping;


/**
 * A servlet wich generate a png for a graph
 * @author Fabrice Bacchella
 * @version $Revision: 360 $
 */
public final class WhichLibs extends HttpServlet {
	static final private Logger logger = Logger.getLogger(WhichLibs.class);
	/**
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		try {
			ServletOutputStream out = res.getOutputStream();
			res.addHeader("Cache-Control", "no-cache");
			
			ServletContext ctxt = getServletContext();

			for (Enumeration<?> e = ctxt.getAttributeNames() ; e.hasMoreElements() ;)
			{
				String attr = (String) e.nextElement();
				Object o = ctxt.getAttribute(attr);
				out.println(attr + " = " + o);
			}

			out.println(resolv("String", ""));
			out.println(resolv("jrds", this));
			try {
				out.println(resolv("Xml Transformer", javax.xml.transform.TransformerFactory.newInstance()));
				out.println("System's property " + System.getProperties().getProperty("javax.xml.transform.TransformerFactory"));
			} catch (TransformerFactoryConfigurationError e) {
				out.println("no xml transformer factory ");
				out.println("System's property" + System.getProperties().getProperty("javax.xml.transform.TransformerFactory"));
			}
			try {
				out.println(resolv("DOM implementation",  DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation()));
			} catch (ParserConfigurationException e) {
				out.println("Invalid DOM parser configuration");
			}
			out.println(resolv("SNMP4J", DefaultUdpTransportMapping.class));
			out.println(resolv("Log4j",logger.getClass()));
//			out.println(resolv("common logging API", org.apache.commons.logging.LogFactory.class));
//			out.println(resolv("common logging", org.apache.commons.logging.LogFactory.getLog(this.getClass())));
			out.println(resolv("JAI", JAI.class));


		} catch (RuntimeException e) {
			logger.error(e, e);
		}							
	}
	
	private String resolv(String name, Object o) {
		String retValue = "";
		if(o != null)
			retValue = resolv(name, o.getClass());
		else
			retValue = name + " not found";
		return retValue;
	}
	

	private String resolv(String name, Class<?> c) {
		String retValue = "";
		try {
			retValue = name + " found in " + locateJar(c);
		} catch (RuntimeException e) {
			retValue = "Problem with " + c + ": " + e.getMessage();
		}
		return retValue;
	}
	
	private String locateJar(Class<?> c ) {
		String retValue="Not found";
		String cName = c.getName();
		int lastDot = cName.lastIndexOf('.');
		if(lastDot > 1) {
			String scn = cName.substring(lastDot + 1);
			URL jarUrl = c.getResource(scn + ".class");
			if(jarUrl != null)
				retValue = jarUrl.getPath();
			else
				retValue = scn + " not found";
		}
		return retValue;
	}
}
