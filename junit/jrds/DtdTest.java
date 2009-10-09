package jrds;

import java.net.URL;

import jrds.factories.Loader;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

public class DtdTest {
	static final private Logger logger = Logger.getLogger(DtdTest.class);
	static final PropertiesManager pm = new PropertiesManager();
	
	@BeforeClass static public void configure() throws Exception {
		Tools.configure();
		Tools.prepareXml();
		Tools.setLevel(new String[] {"jrds", "org.apache"}, logger.getLevel());
	}
	
	@Test public void scanPaths() throws Exception {
		Loader l = new Loader();
		
		//pm.join(new File("jrds.properties"));
		pm.libspath.add(new URL("file:build/classes"));
		pm.update();

		for(URL lib: pm.libspath) {
			logger.info("Adding lib " + lib);
			l.importUrl(lib);
		}
	}
}
