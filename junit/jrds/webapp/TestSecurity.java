package jrds.webapp;

import javax.servlet.ServletContext;

import jrds.HostInfo;
import jrds.Probe;
import jrds.Tools;
import jrds.mockobjects.MokeProbe;
import jrds.standalone.JettyLogger;
import jrds.starter.HostStarter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;

public class TestSecurity  {
	static final private Logger logger = Logger.getLogger(TestSecurity.class);
	static String[] oldvalues;
	static String[] props;
	
	static ServletTester tester = null;
	
	@Test
	public void testStatus() throws Exception
	{
		HttpTester request = new HttpTester();
		HttpTester response = new HttpTester();
		request.setMethod("GET");
		request.setHeader("Host","tester");
		request.setURI("/status");
		request.setVersion("HTTP/1.0");

		response.parse(tester.getResponses(request.generate()));

		Assert.assertEquals(200,response.getStatus());
		
		logger.trace(response.getContent());
	}

	@Test
	public void testWhich() throws Exception
	{
		HttpTester request = new HttpTester();
		HttpTester response = new HttpTester();
		request.setMethod("GET");
		request.setHeader("Host","tester");
		request.setURI("/which");
		request.setVersion("HTTP/1.0");

		response.parse(tester.getResponses(request.generate()));

		Assert.assertEquals(403,response.getStatus());
		
		logger.trace(response.getContent());
	}

	@BeforeClass
	static public void configure() throws Exception {
		Tools.configure();
		System.setProperty("org.mortbay.log.class", jrds.standalone.JettyLogger.class.getName());
		Tools.setLevel(logger, Level.TRACE, JettyLogger.class.getName(), Status.class.getName(), "jrds.webapp.Configuration", "jrds.webapp.JrdsServlet");
		tester = new ServletTester();
		tester.setContextPath("/");
		ServletContext sc =  tester.getContext().getServletContext();

		props = new String[] {"tmpdir", "configdir", "autocreate", "rrddir", "security", "defaultroles", "adminrole"};
		String[] values = new String[] {"tmp", "tmp/config", "true", "tmp", "true", "ANONYMOUS", "admin"};
		oldvalues = new String[values.length];
		for(int i = 0; i < values.length; i++) {
			String prop = "jrds." + props[i];
			oldvalues[i] = System.getProperty(prop);
			System.setProperty(prop, values[i]);
		}

		Configuration c = new Configuration(sc);
		sc.setAttribute(Configuration.class.getName(), c);
        HostStarter h = new HostStarter(new HostInfo("localhost"));
		Probe<?,?> p = new MokeProbe<String, Number>();
		p.setHost(h);
        h.addProbe(p);
        c.getHostsList().addHost(h.getHost());
		c.getHostsList().addProbe(p);
		tester.addServlet(Status.class, "/status");
		tester.addServlet(WhichLibs.class, "/which");
		
		logger.trace("Security: ");
		logger.trace(c.getPropertiesManager().security);
		logger.trace(c.getPropertiesManager().adminrole);

		tester.start();
	}
	
	@AfterClass
	static public void finish() {
		for(int i = 0; i < oldvalues.length; i++) {
			String prop = "jrds." + props[i];
			oldvalues[i] = System.getProperty(prop);
			System.setProperty(prop, oldvalues[i]);
		}
		
	}
}
