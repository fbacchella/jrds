package jrds.configuration;

import java.net.URI;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import jrds.Log4JRule;
import jrds.PropertiesManager;
import jrds.Tools;

public class TestDtd {

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
        Tools.prepareXml();
    }

    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);
    private final Logger logger = logrule.getTestlogger();

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.factories.xml", "org.apache");
    }

    @Test
    public void scanPaths() throws Exception {
        Loader l = new Loader();

        PropertiesManager pm = Tools.makePm(testFolder, "strict=true");

        for(URI lib: pm.libspath) {
            logger.info("Adding lib " + lib);
            l.importUrl(lib);
        }
    }

    @Test
    public void checkHost() throws Exception {
        Tools.parseRessource("goodhost1.xml");
    }

    @Test
    public void checkPropeDesc() throws Exception {
        Tools.parseRessource("fulldesc.xml");
    }

    @Test
    public void checkCustomGraph() throws Exception {
        Tools.parseRessource("customgraph.xml");
    }

    @Test
    public void checkGraphDesc() throws Exception {
        Tools.parseRessource("graphdesc.xml");
    }

    @Test
    public void checkMacro() throws Exception {
        Tools.parseRessource("macro.xml");
    }

    @Test
    public void checkFilter() throws Exception {
        Tools.parseRessource("view1.xml");
    }

    @Test
    public void checkListener() throws Exception {
        Tools.parseRessource("listener.xml");
    }

}
