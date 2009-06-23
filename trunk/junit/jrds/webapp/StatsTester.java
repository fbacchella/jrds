package jrds.webapp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletException;

import jrds.GetMoke;
import jrds.JrdsTester;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class StatsTester  extends JrdsTester {
	@Test
	public void testStats() throws ServletException, IOException
	{
		jrds.webapp.Status s = new jrds.webapp.Status();
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		s.doGet(GetMoke.getRequest(new HashMap<String,String[]>()), GetMoke.getResponse(buffer));
		String output = buffer.toString();
		System.out.print(buffer.toString());
		Assert.assertTrue(output.contains("Hosts:"));
		Assert.assertTrue(output.contains("Probes:"));
		Assert.assertTrue(output.contains("Last collect:"));
		Assert.assertTrue(output.contains("Last running duration:"));
	}
	
	@BeforeClass static public void configure() {
		JrdsTester.configure();
		Logger.getLogger(ParamsBean.class).setLevel(Level.TRACE);
		try {
			System.out.println(GetMoke.getResponse(null).getOutputStream().getClass());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@After public void tearDown() {
	}

}
