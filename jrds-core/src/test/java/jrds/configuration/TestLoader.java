package jrds.configuration;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import jrds.Tools;
import jrds.factories.xml.JrdsDocument;

public class TestLoader {
    static final private Logger logger = Logger.getLogger(TestLoader.class);

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, "jrds");
        Tools.prepareXml();
    }

    @Test
    public void doLoadJar() throws ParserConfigurationException, MalformedURLException, URISyntaxException {
        Loader l = new Loader();
        l.importUrl(getClass().getClassLoader().getResource("desc").toURI());
        l.done();
        Assert.assertFalse("graph desc list is empty", l.getRepository(ConfigType.GRAPHDESC).isEmpty());
        Assert.assertFalse("probe desc list is empty", l.getRepository(ConfigType.PROBEDESC).isEmpty());
    }

    @Test
    public void doLoadHost() throws Exception {
        Loader l = new Loader();
        l.importStream(getClass().getClassLoader().getResourceAsStream("host1.xml"), "");
        l.done();

        Assert.assertTrue(l.getRepository(ConfigType.HOSTS).containsKey("name"));
    }

    @Test
    public void doLoadView() throws Exception {
        Loader l = new Loader();

        l.importStream(getClass().getClassLoader().getResourceAsStream("view1.xml"), "");
        l.done();

        Map<String, JrdsDocument> rep = l.getRepository(ConfigType.FILTER);
        logger.trace(rep);
        Assert.assertTrue(rep.containsKey("Test view 1"));
    }

    @Test
    public void doLoadProbeDesc() throws Exception {
        Loader l = new Loader();

        l.importStream(getClass().getClassLoader().getResourceAsStream("fulldesc.xml"), "");
        l.done();

        Map<String, JrdsDocument> rep = l.getRepository(ConfigType.PROBEDESC);
        logger.trace(rep);
        Assert.assertTrue(rep.containsKey("name"));

    }

    @Test
    public void doLoadGraph() throws Exception {
        Loader l = new Loader();
        l.importStream(getClass().getClassLoader().getResourceAsStream("customgraph.xml"), "");
        l.done();

        Map<String, JrdsDocument> rep = l.getRepository(ConfigType.GRAPH);
        logger.trace(rep);
        Assert.assertTrue(rep.containsKey("name"));
    }

    @Test
    public void doLoadTab() throws Exception {
        Loader l = new Loader();
        l.importStream(getClass().getClassLoader().getResourceAsStream("goodtab.xml"), "");
        l.done();

        Map<String, JrdsDocument> rep = l.getRepository(ConfigType.TAB);
        logger.trace(rep);
        Assert.assertTrue(rep.containsKey("goodtab"));
    }

}
