package jrds.webapp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletException;

import jrds.HostsList;
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.mockobjects.GetMoke;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestStats {
	static final private Logger logger = Logger.getLogger(TestStats.class);

	@Test(expected=NullPointerException.class)
	public void testStats() throws ServletException, IOException
	{
		jrds.webapp.Status s = new jrds.webapp.Status();
		s.getServletContext().setAttribute(HostsList.class.getName(), new HostsList(new PropertiesManager()));
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		s.doGet(GetMoke.getRequest(new HashMap<String,String[]>()), GetMoke.getResponse(buffer));
		String output = buffer.toString();
		System.out.print(buffer.toString());
		Assert.assertTrue(output.contains("Hosts:"));
		Assert.assertTrue(output.contains("Probes:"));
		Assert.assertTrue(output.contains("Last collect:"));
		Assert.assertTrue(output.contains("Last running duration:"));
	}

	@BeforeClass
	static public void configure() throws IOException {
		Tools.configure();
		Tools.setLevel(new String[] {ParamsBean.class.getName()}, logger.getLevel());
		logger.trace(GetMoke.getResponse(null).getOutputStream().getClass());
	}

}
