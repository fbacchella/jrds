package jrds.webapp;

import java.io.File;
import java.io.IOException;

import jrds.PropertiesManager;
import jrds.Tools;
import jrds.mockobjects.GetMoke;
import jrds.mockobjects.MokeServletContext;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestConfiguration {
	static final private Logger logger = Logger.getLogger(TestConfiguration.class);

	@BeforeClass
	static public void configure() throws IOException {
		Tools.configure();
		Tools.setLevel(logger, Level.TRACE, "jrds.HostList", "jrds.PropertiesManager", ParamsBean.class.getName(), Configuration.class.getName());
		logger.trace(GetMoke.getResponse(null).getOutputStream().getClass());
	}

	@Test
	public void test1() {
		MokeServletContext sc = new MokeServletContext();
		sc.initParameters.put("tmpdir", "tmp");
		sc.initParameters.put("rrddir", "tmp");
		sc.initParameters.put("configdir", "tmp/config");
		sc.initParameters.put("autocreate", "true");
		Configuration c = new Configuration(sc);
		@SuppressWarnings("unused")
		PropertiesManager pm = c.getPropertiesManager();
		logger.trace(c.getHostsList().getHosts());
		Assert.assertTrue("confid dir not created", new File("tmp/config").isDirectory());
	}
}
