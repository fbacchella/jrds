package jrds;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringBufferInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;

import javax.servlet.ServletContext;

import org.apache.jasper.JspC;
import org.apache.jasper.JspCompilationContext;
import org.apache.jasper.compiler.Compiler;
import org.apache.jasper.compiler.JspConfig;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.apache.jasper.compiler.TagPluginManager;
import org.apache.jasper.compiler.TldLocationsCache;
import org.apache.jasper.servlet.JspCServletContext;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.meterware.servletunit.ServletRunner;



public class JspTest extends JrdsTester {
	static com.meterware.servletunit.ServletRunner runner;
	static String webxml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
			"<!DOCTYPE web-app PUBLIC \"-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN\"\n" +
			"\"http://java.sun.com/dtd/web-app_2_3.dtd\">\n" +
			"<web-app>\n" +
			"<taglib>\n" +
			"<taglib-uri>http://java.sun.com/jstl/core</taglib-uri>\n" +
			"<taglib-location>/WEB-INF/c.tld</taglib-location>\n" +
			"</taglib>\n" +
			"</web-app>\n";
	static final private Logger logger = Logger.getLogger(JspTest.class);
	static InputStream webxmlStream;

	@Test public void indexjsp() throws MalformedURLException, IOException, SAXException {
		/*javax.servlet.http.HttpSession ses = runner.getSession(true);
		ServletContext ctxt = ses.getServletContext();
		logger.debug(ctxt.getServletContextName());
		logger.debug(ctxt.getServerInfo());
		Enumeration e = ctxt.getServletNames();
		logger.debug(ctxt.getRealPath("/web/index.jsp"));
		logger.debug(ctxt.getRequestDispatcher("/web/index.jsp"));
		while(e.hasMoreElements())
			logger.debug(e.nextElement());
		logger.debug(runner.JASPER_DESCRIPTOR.getClassName());
		runner.newClient().newInvocation("http://localhost/web/index.jsp");*/
		//
		runner.getResponse("http://localhost/index.jsp");
	}

	@BeforeClass static public void configure() {
		JrdsTester.configure();
		Logger.getLogger(JspTest.class).setLevel(Level.DEBUG);
		try {
			webxmlStream = new StringBufferInputStream(webxml);
			//runner = new ServletRunner(webxmlStream);
			runner = new ServletRunner(new File("web/WEB-INF/web.xml"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
