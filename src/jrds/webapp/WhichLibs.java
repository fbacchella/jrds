/*##########################################################################
 _##
 _##  $Id: Graph.java 360 2006-08-23 09:31:58 +0000 (mer., 23 août 2006) fbacchella $
 _##
 _##########################################################################*/

package jrds.webapp;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import jrds.HostsList;
import jrds.PropertiesManager;
import jrds.StoreOpener;

import org.apache.log4j.Logger;
import org.snmp4j.transport.DefaultUdpTransportMapping;


/**
 * A servlet wich generate a png for a graph
 * @author Fabrice Bacchella
 * @version $Revision: 360 $
 */
public final class WhichLibs extends JrdsServlet {
	static final private Logger logger = Logger.getLogger(WhichLibs.class);
	/**
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@SuppressWarnings("unchecked")
	public void doGet(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		HostsList hl = getHostsList();

		ParamsBean params = new ParamsBean();
		params.parseReq(req, hl);
		if(! allowed(params, getPropertiesManager().adminrole)) {
			res.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		try {
			ServletOutputStream out = res.getOutputStream();
			res.setContentType("text/plain");
			res.addHeader("Cache-Control", "no-cache");
			
			ServletContext ctxt = getServletContext();
//
//			out.println("Dumping attributes");
//			for(String attr: jrds.Util.iterate((Enumeration<String>)ctxt.getAttributeNames())) {
//				Object o = ctxt.getAttribute(attr);
//				out.println(attr + " = (" + o.getClass().getName() + ") " + o);
//			}
//			out.println("Dumping init parameters");
//			for(String attr: jrds.Util.iterate((Enumeration<String>)ctxt.getInitParameterNames())) {
//				String o = ctxt.getInitParameter(attr);
//				out.println(attr + " = " + o);
//			}
//			out.println("Dumping system properties");
//			Properties p = System.getProperties();
//			for(String attr: jrds.Util.iterate((Enumeration<String>)p.propertyNames())) {
//				Object o = p.getProperty(attr);
//				out.println(attr + " = " + o);
//			}		

			out.println("Server info: ");
			out.println("    Servlet API: " + ctxt.getMajorVersion() + "." + ctxt.getMinorVersion());
			out.println("    Server info: " + ctxt.getServerInfo());
			out.println();
			
			String[] openned = StoreOpener.getInstance().getOpenFiles();
			out.println("" + StoreOpener.getInstance().getOpenFileCount() + " opened rrd: ");
			for(String rrdPath: openned) {
				out.println("   " + rrdPath);
			}
			out.println();

			PropertiesManager pm = getPropertiesManager();
			out.println("Temp dir:" + pm.tmpdir);
			out.println(resolv("String", ""));
			out.println(resolv("jrds", this));
			String transformerFactory = System.getProperties().getProperty("javax.xml.transform.TransformerFactory");
			try {
				out.print(resolv("Xml Transformer", javax.xml.transform.TransformerFactory.newInstance()));
			} catch (TransformerFactoryConfigurationError e) {
				out.print("no xml transformer factory ");
			}
			if(transformerFactory != null) {
				out.println("Set by sytem property javax.xml.transform.TransformerFactory: " + transformerFactory);
			}
			else {
				out.println();
			}
			try {
				out.println(resolv("DOM implementation",  DocumentBuilderFactory.newInstance().newDocumentBuilder().getDOMImplementation()));
			} catch (ParserConfigurationException e) {
				out.println("Invalid DOM parser configuration");
			}
			out.println(resolv("Servlet API", javax.servlet.ServletContext.class));
			out.println(resolv("SNMP4J", DefaultUdpTransportMapping.class));
			out.println(resolv("Log4j",logger.getClass()));
			out.println("Generation:" + getConfig().thisgeneration);
//			out.println(resolv("common logging API", org.apache.commons.logging.LogFactory.class));
//			out.println(resolv("common logging", org.apache.commons.logging.LogFactory.getLog(this.getClass())));
//			out.println(resolv("JAI", JAI.class));


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
		return retValue.replaceFirst("!.*", "").replaceFirst("file:", "");
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
