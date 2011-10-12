package jrds.webapp;

import javax.servlet.ServletContext;

import jrds.Tools;
import jrds.mockobjects.MokeProbe;
import jrds.objects.RdsHost;
import jrds.objects.probe.Probe;
import jrds.standalone.JettyLogger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.testing.HttpTester;
import org.mortbay.jetty.testing.ServletTester;

public class TestStats  {
	static final private Logger logger = Logger.getLogger(TestStats.class);
	static String[] oldvalues;
	static String[] props;
	
	static ServletTester tester = null;
	@Test //(expected=NullPointerException.class)
	public void testStats() throws Exception
	{
		HttpTester request = new HttpTester();
		HttpTester response = new HttpTester();
		request.setMethod("GET");
		request.setHeader("Host","tester");
		request.setURI("/status");
		request.setVersion("HTTP/1.0");

		response.parse(tester.getResponses(request.generate()));

		Assert.assertEquals(200,response.getStatus());
		Assert.assertTrue(response.getContent().contains("Hosts: 1"));
		Assert.assertTrue(response.getContent().contains("Probes: 1"));
		Assert.assertTrue(response.getContent().contains("Last collect:"));
		Assert.assertTrue(response.getContent().contains("Last running duration: 0s"));
		
		logger.trace(response.getContent());
	}

	@BeforeClass
	static public void configure() throws Exception {
		Tools.configure();
		logger.setLevel(Level.TRACE);
		System.setProperty("org.mortbay.log.class", jrds.standalone.JettyLogger.class.getName());
		Tools.setLevel(new String[] {JettyLogger.class.getName(), Status.class.getName()}, logger.getLevel());
		tester=new ServletTester();
		tester.setContextPath("/");
		ServletContext sc =  tester.getContext().getServletContext();

		props = new String[] {"tmpdir", "configdir", "autocreate", "rrddir"};
		String[] values = new String[] {"tmp", "tmp/config", "true", "tmp"};
		oldvalues = new String[values.length];
		for(int i = 0; i < values.length; i++) {
			String prop = "jrds." + props[i];
			oldvalues[i] = System.getProperty(prop);
			System.setProperty(prop, values[i]);
		}

		Configuration c = new Configuration(sc);
		sc.setAttribute(Configuration.class.getName(), c);
		RdsHost h = new RdsHost();
		Probe<?,?> p = new MokeProbe<String, Number>();
		p.setHost(h);
		h.getProbes().add(p);
		c.getHostsList().addHost(h);
		c.getHostsList().addProbe(p);
		tester.addServlet(Status.class, "/status");

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
