package jrds.configuration;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.rrd4j.DsType;
import org.rrd4j.data.IPlottable;
import org.rrd4j.graph.RrdGraph;
import org.rrd4j.graph.RrdGraphDef;
import org.rrd4j.graph.RrdGraphInfo;
import org.slf4j.Logger;
import org.slf4j.event.Level;
import org.w3c.dom.Document;

import jrds.GraphDesc;
import jrds.GraphNode;
import jrds.Log4JRule;
import jrds.Period;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.factories.xml.JrdsDocument;
import jrds.mockobjects.MokeProbe;
import jrds.store.ExtractInfo;

public class TestGraphDescBuilder {

    static final ConfigObjectBuilder<Object> ob = new ConfigObjectBuilder<Object>(ConfigType.GRAPHDESC) {
        @Override
        Object build(JrdsDocument n) {
            return null;
        }
    };

    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);
    private final Logger logger = logrule.getTestlogger();

    @BeforeClass
    static public void configure() throws ParserConfigurationException {
        Tools.configure();
        Tools.prepareXml();
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.GraphDesc", "jrds.Graph");
        logrule.setLevel(Level.INFO, "jrds.factories.xml.CompiledXPath");
    }

    @Test
    public void testGraphDesc() throws Exception {
        PropertiesManager pm = Tools.makePm(testFolder);
        JrdsDocument d = Tools.parseRessource("graphdesc.xml");
        GraphDescBuilder gdbuild = new GraphDescBuilder();
        gdbuild.setPm(pm);
        GraphDesc gd = gdbuild.build(d);
        if(logger.isTraceEnabled()) {
            Document gddom = gd.dumpAsXml();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Map<String, String> prop = new HashMap<String, String>(3);
            prop.put(OutputKeys.OMIT_XML_DECLARATION, "no");
            prop.put(OutputKeys.INDENT, "yes");
            prop.put("{http://xml.apache.org/xslt}indent-amount", "4");
            jrds.Util.serialize(gddom, os, null, prop);
            logger.trace(new String(os.toByteArray(), StandardCharsets.UTF_8));
        }
        MokeProbe<String, Number> p = new MokeProbe<String, Number>();
        p.configure();
        p.getHost().setHostDir(testFolder.getRoot());

        p.setMainStore(pm.defaultStore, new HashMap<String, String>(0));

        ProbeDesc<?> pd = p.getPd();

        pd.add(ProbeDesc.getDataSourceBuilder("space separated", DsType.COUNTER));
        pd.add(ProbeDesc.getDataSourceBuilder("add1", DsType.COUNTER));
        pd.add(ProbeDesc.getDataSourceBuilder("add2", DsType.COUNTER));
        pd.add(ProbeDesc.getDataSourceBuilder("add3", DsType.COUNTER));
        pd.add(ProbeDesc.getDataSourceBuilder("add4", DsType.COUNTER));
        pd.add(ProbeDesc.getDataSourceBuilder("add5", DsType.COUNTER));

        p.checkStore();

        if(logger.isTraceEnabled()) {
            Document gddom = p.dumpAsXml(true);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Map<String, String> prop = new HashMap<String, String>(3);
            prop.put(OutputKeys.OMIT_XML_DECLARATION, "no");
            prop.put(OutputKeys.INDENT, "yes");
            prop.put("{http://xml.apache.org/xslt}indent-amount", "4");
            jrds.Util.serialize(gddom, os, null, prop);
            logger.trace(new String(os.toByteArray(), StandardCharsets.UTF_8));
        }

        logger.trace("Probe preparation done");

        Period pr = new Period();
        ExtractInfo ei = ExtractInfo.of(pr.getBegin(), pr.getEnd());
        Map<String, IPlottable> empty = Collections.emptyMap();

        ImageWriter iw = ImageIO.getImageWritersByFormatName("BMP").next();
        ImageWriteParam iwp = iw.getDefaultWriteParam();
        iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        iwp.unsetCompression();

        RrdGraphDef def = gd.getGraphDef(p, ei, empty);
        RrdGraphInfo gi = new RrdGraph(def, iw, iwp).getRrdGraphInfo();

        Assert.assertEquals("graph name failed", "graphName", gd.getGraphName());
        Assert.assertEquals("graph title failed", "graphTitle", gd.getGraphTitle());
        Assert.assertEquals("graph name failed", "graphdesctest", gd.getName());
        Assert.assertEquals("legend count failed", 6, gd.getLegendLines());
        Assert.assertFalse("Graph unit should be binary", gd.isSiUnit());
        Assert.assertEquals("Graph unit scale should be fixed", 0, gd.getUnitExponent().intValue());

        Assert.assertTrue("graph height invalid", 206 < gi.getHeight());
        Assert.assertTrue("graph width invalid", 578 < gi.getWidth());
        Assert.assertEquals("graph byte count invalid", 678758, gi.getByteCount());

        for(String treename: new String[] { PropertiesManager.HOSTSTAB, PropertiesManager.VIEWSTAB, "tab" }) {
            List<String> tree = gd.getTree(new GraphNode(p, gd), treename);
            Assert.assertEquals("not enough element in tree " + treename, 2, tree.size());
            int i = 1;
            for(String element: tree) {
                Assert.assertEquals("wrong tree element", treename + i++, element);
            }
        }
    }

    @Test(expected = NoSuchMethodException.class)
    public void testBadGraphDescClass() throws Exception {
        JrdsDocument d = Tools.parseRessource("graphdesc.xml");
        d.getRootElement().getElementbyName("graphClass").setTextContent(String.class.getName());
        GraphDescBuilder gdbuild = new GraphDescBuilder();
        gdbuild.setPm(Tools.makePm(testFolder));
        @SuppressWarnings("unused")
        GraphDesc gd = gdbuild.makeGraphDesc(d);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyGraphDescClass() throws Exception {
        JrdsDocument d = Tools.parseRessource("graphdesc.xml");
        d.getRootElement().getElementbyName("graphClass").setTextContent("");
        GraphDescBuilder gdbuild = new GraphDescBuilder();
        gdbuild.setPm(Tools.makePm(testFolder));
        @SuppressWarnings("unused")
        GraphDesc gd = gdbuild.makeGraphDesc(d);
    }

    @Test
    public void testCustomGraph() throws Exception {
        JrdsDocument d = Tools.parseRessource("customgraph.xml");
        GraphDescBuilder gdbuild = new GraphDescBuilder();
        gdbuild.setPm(Tools.makePm(testFolder));
        GraphDesc gd = gdbuild.makeGraphDesc(d);
        Assert.assertEquals("graph name failed", "graphName", gd.getGraphName());
        Assert.assertEquals("graph title failed", "", gd.getGraphTitle());
        Assert.assertEquals("graph name failed", "name", gd.getName());
        Assert.assertEquals("graph height invalid", 800, gd.getHeight());
        Assert.assertEquals("graph width invalid", 600, gd.getWidth());
        Assert.assertTrue("Lower limit is a number (not a NaN)" + gd.getUpperLimit(), Double.isNaN(gd.getUpperLimit()));
        Assert.assertEquals("graph lower limit is invalid", 1000, gd.getLowerLimit(), 0.1);
        Assert.assertFalse("graph is with legend", gd.withLegend());
        Assert.assertFalse("graph is with summary", gd.withSummary());
        Assert.assertEquals("legend count failed", 0, gd.getLegendLines());
    }

    @Test
    public void testGraphDescBuilderParse() throws Exception {
        JrdsDocument d = Tools.parseRessource("graphdesc.xml");
        GraphDescBuilder gdbuild = new GraphDescBuilder();
        gdbuild.setPm(Tools.makePm(testFolder));
        @SuppressWarnings("unused")
        GraphDesc gd = gdbuild.build(d);
    }

    @Test
    public void testFullConfigpath() throws Exception {
        PropertiesManager localpm = Tools.makePm(testFolder);
        ConfigObjectFactory conf = new ConfigObjectFactory(localpm);
        conf.getNodeMap(ConfigType.GRAPHDESC).put("graphdesc", Tools.parseRessource("graphdesc.xml"));
        Assert.assertNotNull("Graphdesc not build", conf.setGraphDescMap().get("graphdesctest"));
    }

}
