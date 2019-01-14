package jrds.configuration;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import jrds.GraphDesc;
import jrds.GraphNode;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.factories.xml.JrdsDocument;
import jrds.mockobjects.GetMoke;

public class TestDescFactory {
    static final private Logger logger = Logger.getLogger(TestDescFactory.class);

    static Loader l;

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, "jrds.factories");
        Tools.setLevel(Level.INFO, "jrds.factories.xml.CompiledXPath");
        Tools.prepareXml();
    }

    @Test
    public void loadGraph() throws Exception {
        JrdsDocument d = Tools.parseRessource("customgraph.xml");
        GraphDescBuilder builder = new GraphDescBuilder();
        builder.setPm(new PropertiesManager());
        GraphDesc gd = (GraphDesc) builder.build(d);

        Assert.assertEquals("name", gd.getName());
        Assert.assertEquals("graphName", gd.getGraphName());
        Assert.assertEquals("", gd.getGraphTitle());
        Assert.assertTrue(gd.isSiUnit());
        Assert.assertEquals("verticalLabel", gd.getVerticalLabel());
        GraphNode gn = new GraphNode(GetMoke.getProbe(), gd);
        logger.debug(gd.getHostTree(gn));
        logger.debug(gd.getViewTree(gn));
    }

    @Test
    public void loadGraphDesc() throws Exception {
        JrdsDocument d = Tools.parseRessource("graphdesc2.xml");

        GraphDescBuilder builder = new GraphDescBuilder();
        builder.setPm(new PropertiesManager());
        GraphDesc gd = (GraphDesc) builder.build(d);

        Assert.assertEquals("mokegraph", gd.getName());
        Assert.assertEquals("mokegraphname", gd.getGraphName());
        Assert.assertEquals("mokegraphtitle", gd.getGraphTitle());
        Assert.assertTrue(gd.isSiUnit());
        Assert.assertEquals("verticallabel", gd.getVerticalLabel());
        GraphNode gn = new GraphNode(GetMoke.getProbe(), gd);
        logger.debug(gd.getHostTree(gn));
        logger.debug(gd.getViewTree(gn));
    }

    @Test
    public void loadProbeDesc() throws Exception {
        JrdsDocument d = Tools.parseRessource("fulldesc.xml");
        PropertiesManager pm = new PropertiesManager();

        ProbeDescBuilder builder = new ProbeDescBuilder();
        builder.setPm(new PropertiesManager());
        ProbeDesc<?> pd = builder.makeProbeDesc(d);
        Assert.assertEquals("name", pd.getName());
        Assert.assertEquals("probename", pd.getProbeName());
        Assert.assertEquals(jrds.mockobjects.MokeProbe.class, pd.getProbeClass());
        Assert.assertEquals("specificvalue1", pd.getSpecific("specificname1"));
        Assert.assertEquals("specificvalue2", pd.getSpecific("specificname2"));
        Assert.assertEquals(0.5, pd.getUptimefactor(), 0);
        Assert.assertEquals((long) pm.step * 2, pd.getHeartBeatDefault());
        logger.trace(pd.getCollectMapping());
        logger.trace(pd.getDefaultBeans());
        // A collect string "" should not be collected
        Assert.assertEquals(3, pd.getCollectMapping().size());
    }

    @Test
    public void loadBadProbeDesc() throws Exception {
        JrdsDocument d = Tools.parseRessource("baddesc.xml");
        PropertiesManager pm = new PropertiesManager();

        List<LoggingEvent> logged = Tools.getLockChecker("jrds.configuration.ConfigObjectBuilder");

        ProbeDescBuilder builder = new ProbeDescBuilder();
        builder.setPm(pm);
        ProbeDesc<?> pd = builder.makeProbeDesc(d);
        logger.trace("Collect mapping: " + pd.getCollectMapping());
        Assert.assertEquals(1, pd.getCollectMapping().size());
        boolean found = false;
        for(LoggingEvent le: logged) {
            String message = le.getRenderedMessage();
            if(message.contains("Invalid ds type specified")) {
                found = true;
                break;
            }
        }
        Assert.assertTrue("bad probe desc not detected", found);
    }

}
