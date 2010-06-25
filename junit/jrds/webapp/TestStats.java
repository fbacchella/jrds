package jrds.webapp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
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

	@Test(expected=IllegalStateException.class)
	public void testStats() throws ServletException, IOException
	{
		System.out.println(resolv("Servlet API", javax.servlet.ServletContext.class));

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
