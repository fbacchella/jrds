package jrds;

import java.net.URL;

import jrds.factories.Loader;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

public class DtdTest {
	static final private Logger logger = Logger.getLogger(DtdTest.class);
	static final PropertiesManager pm = new PropertiesManager();
	
	@BeforeClass static public void configure() throws Exception {
		Tools.configure();
		Tools.prepareXml();
		logger.setLevel(Level.TRACE);
		Tools.setLevel(new String[] {"jrds.factories.xml", "org.apache"}, logger.getLevel());
	}
	
	@Test
	public void scanPaths() throws Exception {
		Loader l = new Loader();
		
		//pm.join(new File("jrds.properties"));
		pm.libspath.add(new URL("file:build/classes"));
		pm.update();

		for(URL lib: pm.libspath) {
			logger.info("Adding lib " + lib);
			l.importUrl(lib);
		}
	}
	
	@Test
	public void chechHost() throws Exception {
		Tools.parseRessource("goodhost1.xml");
		
	}
	@Test
	public void chechFullDesc() throws Exception {
		Tools.parseRessource("fulldesc.xml");
		
	}
	@Test
	public void chechMacro() throws Exception {
		Tools.parseRessource("macro.xml");
		
	}
	@Test
	public void chechFilter() throws Exception {
		Tools.parseRessource("view1.xml");
		
	}
	
}
