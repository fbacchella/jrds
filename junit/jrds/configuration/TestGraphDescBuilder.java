package jrds.configuration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;

import jrds.GraphDesc;
import jrds.Period;
import jrds.GraphNode;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.factories.xml.JrdsDocument;
import jrds.mockobjects.GenerateProbe;
import jrds.mockobjects.GenerateProbe.ChainedMap;
import jrds.mockobjects.MokeProbe;
import jrds.store.ExtractInfo;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.DsType;
import org.rrd4j.data.Plottable;
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
        Tools.setLevel(logger, Level.TRACE, "jrds.GraphDesc", "jrds.Graph");
        Tools.setLevel(Level.INFO,"jrds.factories.xml.CompiledXPath");

        Tools.prepareXml();
    }

    @Test
    public void testGraphDesc() throws Exception {

        PropertiesManager pm = Tools.makePm();
        JrdsDocument d = Tools.parseRessource("graphdesc.xml");
        GraphDescBuilder gdbuild = new GraphDescBuilder();
        gdbuild.setPm(Tools.makePm());
        GraphDesc gd = gdbuild.build(d);
        if(logger.isTraceEnabled()) {
            Document gddom = gd.dumpAsXml();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Map<String, String> prop = new HashMap<String, String>(3);
            prop.put(OutputKeys.OMIT_XML_DECLARATION, "no");
            prop.put(OutputKeys.INDENT, "yes");
            prop.put("{http://xml.apache.org/xslt}indent-amount", "4");
            jrds.Util.serialize(gddom, os, null, prop);
            logger.trace(new String(os.toByteArray(),"UTF-8"));
        }
        MokeProbe<String, Number> p = new MokeProbe<String, Number>();
        p.getHost().setHostDir(testFolder.getRoot());

        p.setMainStore(pm.defaultStore, new HashMap<String, String>(0));

        ProbeDesc pd = p.getPd();

        ChainedMap<Object> dsMap = GenerateProbe.ChainedMap.start();
        dsMap.set("dsName", "space separated").set("dsType", DsType.COUNTER);
        pd.add(dsMap);

        dsMap.clear();
        dsMap.set("dsName", "add1").set("dsType", DsType.COUNTER);
        pd.add(dsMap);

        dsMap.clear();
        dsMap.set("dsName", "add2").set("dsType", DsType.COUNTER);
        pd.add(dsMap);

        dsMap.clear();
        dsMap.set("dsName", "add3").set("dsType", DsType.COUNTER);
        pd.add(dsMap);

        p.checkStore();

        if(logger.isTraceEnabled()) {
            Document gddom = p.dumpAsXml(true);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Map<String, String> prop = new HashMap<String, String>(3);
            prop.put(OutputKeys.OMIT_XML_DECLARATION, "no");
            prop.put(OutputKeys.INDENT, "yes");
            prop.put("{http://xml.apache.org/xslt}indent-amount", "4");
            jrds.Util.serialize(gddom, os, null, prop);
            logger.trace(new String(os.toByteArray(),"UTF-8"));
        }

        logger.trace("Probe preparation done");

        Period pr = new Period();
        ExtractInfo ei = ExtractInfo.get().make(pr.getBegin(), pr.getEnd());
        Map<String, Plottable> empty = Collections.emptyMap();
        RrdGraphDef def = gd.getGraphDef(p, ei, empty);
        RrdGraphInfo gi = new RrdGraph(def).getRrdGraphInfo();

        logger.debug(Arrays.asList(gi.getPrintLines()));

        Assert.assertEquals("graph name failed", "graphName", gd.getGraphName());
        Assert.assertEquals("graph title failed", "graphTitle", gd.getGraphTitle());
        Assert.assertEquals("graph name failed", "graphdesctest", gd.getName());
        Assert.assertEquals("legend count failed", 5, gd.getLegendLines());
        Assert.assertFalse("Graph unit should be binary", gd.isSiUnit());
        Assert.assertEquals("Graph unit scale should be fixed", 0, gd.getUnitExponent().intValue());

        Assert.assertTrue("graph height invalid", 206 < gi.getHeight());
        Assert.assertTrue("graph width invalid", 578 < gi.getWidth());
        Assert.assertEquals("graph byte count invalid", 12574 , gi.getByteCount(), 4000);

        for(String treename: new String[]{PropertiesManager.HOSTSTAB, PropertiesManager.VIEWSTAB, "tab"}) {
            List<String> tree = gd.getTree(new GraphNode(p, gd), treename);
            Assert.assertEquals("not enough element in tree " +  treename, 2, tree.size());
            int i = 1;
            for(String element: tree) {
                Assert.assertEquals("wrong tree element", treename + i++, element);
            }
        }
    }

    @Test(expected=NoSuchMethodException.class)
    public void testBadGraphDescClass()  throws Exception {
        JrdsDocument d = Tools.parseRessource("graphdesc.xml");
        d.getRootElement().getElementbyName("graphClass").setTextContent(String.class.getName());
        GraphDescBuilder gdbuild = new GraphDescBuilder();
        gdbuild.setPm(Tools.makePm());
        @SuppressWarnings("unused")
        GraphDesc gd = gdbuild.makeGraphDesc(d);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testEmptyGraphDescClass()  throws Exception {
        JrdsDocument d = Tools.parseRessource("graphdesc.xml");
        d.getRootElement().getElementbyName("graphClass").setTextContent("");
        GraphDescBuilder gdbuild = new GraphDescBuilder();
        gdbuild.setPm(Tools.makePm());
        @SuppressWarnings("unused")
        GraphDesc gd = gdbuild.makeGraphDesc(d);
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
    public void testGraphDescBuilderParse()
            throws Exception {
        JrdsDocument d = Tools.parseRessource("graphdesc.xml");
        GraphDescBuilder gdbuild = new GraphDescBuilder();
        gdbuild.setPm(Tools.makePm());
        @SuppressWarnings("unused")
        GraphDesc gd = gdbuild.build(d);
    }

    @Test
    public void testFullConfigpath() throws Exception {
        PropertiesManager localpm = Tools.makePm();
        ConfigObjectFactory conf = new ConfigObjectFactory(localpm);
        conf.getNodeMap(ConfigType.GRAPHDESC).put("graphdesc", Tools.parseRessource("graphdesc.xml"));
        Assert.assertNotNull("Graphdesc not build", conf.setGraphDescMap().get("graphdesctest"));
    }

}
