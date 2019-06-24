package jrds.configuration;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import jrds.Log4JRule;
import jrds.Tools;
import jrds.factories.xml.JrdsDocument;

public class TestLoader {
    
    @Rule
    public final Log4JRule logrule = new Log4JRule(this);
    private final Logger logger = logrule.getTestlogger();

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        Tools.prepareXml();
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds");
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
        logger.trace("{}", rep);
        Assert.assertTrue(rep.containsKey("Test view 1"));
    }

    @Test
    public void doLoadProbeDesc() throws Exception {
        Loader l = new Loader();

        l.importStream(getClass().getClassLoader().getResourceAsStream("fulldesc.xml"), "");
        l.done();

        Map<String, JrdsDocument> rep = l.getRepository(ConfigType.PROBEDESC);
        logger.trace("{}", rep);
        Assert.assertTrue(rep.containsKey("name"));

    }

    @Test
    public void doLoadGraph() throws Exception {
        Loader l = new Loader();
        l.importStream(getClass().getClassLoader().getResourceAsStream("customgraph.xml"), "");
        l.done();

        Map<String, JrdsDocument> rep = l.getRepository(ConfigType.GRAPH);
        logger.trace("{}", rep);
        Assert.assertTrue(rep.containsKey("name"));
    }

    @Test
    public void doLoadTab() throws Exception {
        Loader l = new Loader();
        l.importStream(getClass().getClassLoader().getResourceAsStream("goodtab.xml"), "");
        l.done();

        Map<String, JrdsDocument> rep = l.getRepository(ConfigType.TAB);
        logger.trace("{}", rep);
        Assert.assertTrue(rep.containsKey("goodtab"));
    }

}
