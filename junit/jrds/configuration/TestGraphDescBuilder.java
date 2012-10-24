package jrds.configuration;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import jrds.GraphDesc;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.StoreOpener;
import jrds.Tools;
import jrds.factories.xml.JrdsDocument;
import jrds.mockobjects.MokeProbe;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.DsType;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;
import org.rrd4j.graph.RrdGraphInfo;
import org.w3c.dom.Document;


public class TestGraphDescBuilder {
    static final private Logger logger = Logger.getLogger(TestGraphDescBuilder.class);

    static final ConfigObjectBuilder<Object> ob = new ConfigObjectBuilder<Object>(ConfigType.GRAPHDESC) {
        @Override
        Object build(JrdsDocument n) {
            return null;
        }
    };

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        StoreOpener.prepare("MEMORY");
        Tools.setLevel(logger, Level.TRACE, "jrds.GraphDesc", "jrds.Graph");
        Tools.setLevel(Level.INFO,"jrds.factories.xml.CompiledXPath");

        Tools.prepareXml();
    }

    @Test
    public void testGraphDesc() throws Exception {
        JrdsDocument d = Tools.parseRessource("graphdesc.xml");
        GraphDescBuilder gdbuild = new GraphDescBuilder();
        gdbuild.setPm(Tools.makePm());
        GraphDesc gd = gdbuild.makeGraphDesc(d);
        if(logger.isTraceEnabled()) {
            Document gddom = gd.dumpAsXml();
            jrds.Util.serialize(gddom, System.out, null, null);
        }
        MokeProbe<String, Number> p = new MokeProbe<String, Number>();
        p.getHost().setHostDir(testFolder.getRoot());

        ProbeDesc pd = p.getPd();

        Map<String, Object> dsMap = new HashMap<String, Object>(2);
        dsMap.put("dsName", "machin bidule");
        dsMap.put("dsType", DsType.COUNTER);
        pd.add(dsMap);

        dsMap.clear();
        dsMap.put("dsName", "add2");
        dsMap.put("dsType", DsType.COUNTER);
        pd.add(dsMap);

        dsMap.clear();
        dsMap.put("dsName", "add3");
        dsMap.put("dsType", DsType.COUNTER);
        pd.add(dsMap);

        p.checkStore();

        RrdGraphDef def = gd.getGraphDef(p);
        RrdGraphInfo gi = new RrdGraph(def).getRrdGraphInfo();

        logger.debug(Arrays.asList(gi.getPrintLines()));

        Assert.assertEquals("graph name failed", "graphName", gd.getGraphName());
        Assert.assertEquals("graph title failed", "graphTitle", gd.getGraphTitle());
        Assert.assertEquals("graph name failed", "name", gd.getName());
        Assert.assertEquals("legend count failed", 3, gd.getLegendLines());
        Assert.assertFalse("Graph unit should be binary", gd.isSiUnit());
        Assert.assertEquals("Graph unit scale should be fixed", 0, gd.getUnitExponent().intValue());

        Assert.assertTrue("graph height invalid", 206 < gi.getHeight());
        Assert.assertTrue("graph width invalid", 578 < gi.getWidth());
        Assert.assertEquals("graph byte count invalid", 12574 , gi.getByteCount(), 4000);
    }

    @Test
    public void testCustomGraph() throws Exception {
        JrdsDocument d = Tools.parseRessource("customgraph.xml");
        GraphDescBuilder gdbuild = new GraphDescBuilder();
        gdbuild.setPm(Tools.makePm());
        GraphDesc gd = gdbuild.makeGraphDesc(d);
        Assert.assertEquals("graph name failed", "graphName", gd.getGraphName());
        Assert.assertEquals("graph title failed", "", gd.getGraphTitle());
        Assert.assertEquals("graph name failed", "name", gd.getName());
        Assert.assertEquals("graph height invalid", 800, gd.getHeight());
        Assert.assertEquals("graph width invalid", 600, gd.getWidth());
        Assert.assertTrue("Lower limit is a number (not a NaN)" + gd.getUpperLimit() , Double.isNaN(gd.getUpperLimit()));
        Assert.assertEquals("graph lower limit is invalid", 1000, gd.getLowerLimit(),0.1);
        Assert.assertFalse("graph is with legend", gd.withLegend());
        Assert.assertFalse("graph is with summary", gd.withSummary());
        Assert.assertEquals("legend count failed", 0, gd.getLegendLines());
    }

    @Test
    public void testFullConfigpath() throws Exception {
        PropertiesManager localpm = Tools.makePm();
        ConfigObjectFactory conf = new ConfigObjectFactory(localpm);
        conf.getNodeMap(ConfigType.GRAPHDESC).put("graphdesc", Tools.parseRessource("graphdesc.xml"));
        Assert.assertNotNull("Graphdesc not build", conf.setGraphDescMap().get("name"));
    }

}
