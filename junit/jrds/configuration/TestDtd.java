package jrds.configuration;

import java.net.URI;

import jrds.PropertiesManager;
import jrds.Tools;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestDtd {
    static final private Logger logger = Logger.getLogger(TestDtd.class);
    static final PropertiesManager pm = new PropertiesManager();

    @BeforeClass static public void configure() throws Exception {
        Tools.configure();
        Tools.prepareXml();
        
        Tools.setLevel(logger, Level.TRACE, "jrds.factories.xml", "org.apache");
    }

    @Test
    public void scanPaths() throws Exception {
        Loader l = new Loader();

        pm.libspath.add(Tools.pathToUrl("build/classes"));
        pm.update();

        for(URI lib: pm.libspath) {
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
