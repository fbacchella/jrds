package jrds.test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import jrds.DirXmlParser;
import jrds.PropertiesManager;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

public class DtdTest extends JrdsTester {
	static final private Logger logger = Logger.getLogger(DtdTest.class);
	static final PropertiesManager pm = new PropertiesManager();
	static final DirXmlParser validateParser = new DirXmlParser() {
		public void init() {
			System.out.println( digester.getClass().getResource("/graphdesc.dtd"));
			logger.debug(digester.getClass().getResource("/graphdesc.dtd"));
			digester.register("-//jrds//DTD Graph Description//EN", digester.getClass().getResource("/graphdesc.dtd").toString());
			digester.register("-//jrds//DTD Probe Description//EN", digester.getClass().getResource("/probedesc.dtd").toString());
			digester.setValidating(true);
		}
	};
	

	@BeforeClass static public void configure() {
		JrdsTester.configure();
	}
	
	@Test public void scanPaths() throws MalformedURLException, IOException {
		logger.setLevel(Level.TRACE);
		Logger.getLogger("jrds.DirXmlParser").setLevel(Level.TRACE);
		Logger.getLogger("org.apache").setLevel(Level.TRACE);

		pm.libspath.add(new URL("file:/Users/bacchell/Devl/jrds/build/probes.jar"));
		pm.libspath.add(new URL("file:/Users/bacchell/Devl/jrdsExalead/build/jrdsexalead.jar"));
		pm.libspath.add(new URL("file:/Users/bacchell/Devl/jrdsAgent/build/jrdsagent.jar"));
		for(URL lib: pm.libspath) {
			logger.info("Adding lib " + lib);
			validateParser.importDescUrl(lib);
		}
	}
}
