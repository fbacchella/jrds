package jrds.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import jrds.Filter;
import jrds.HostInfo;
import jrds.Log4JRule;
import jrds.Macro;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.Tab;
import jrds.Tools;
import jrds.factories.ProbeFactory;
import jrds.factories.xml.JrdsDocument;
import jrds.mockobjects.MokeProbeFactory;

public class TestLoadConfiguration {

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);
    private final Logger logger = logrule.getTestlogger();

    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    static final private String goodProbeXml2 = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
                    "<!DOCTYPE probe PUBLIC \"-//jrds//DTD Host//EN\" \"urn:jrds:host\">" +
                    "<probe type = \"PartitionSpace\">" +
                    "<arg type=\"String\" value=\"/\" />" +
                    "</probe>";

    static final String goodMacroXml =
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
                    "<!DOCTYPE macrodef PUBLIC \"-//jrds//DTD Host//EN\" \"urn:jrds:host\">" +
                    "<macrodef name=\"macrodef\">" +
                    "<probe type = \"MacroProbe1\">" +
                    "<arg type=\"String\" value=\"${a}\" />" +
                    "</probe>" + 
                    "<probe type = \"MacroProbe2\">" +
                    "<arg type=\"String\" value=\"/\" />" +
                    "</probe>" +
                    "</macrodef>";

    static final String goodHostXml = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
                    "<!DOCTYPE host PUBLIC \"-//jrds//DTD Host//EN\" \"urn:jrds:host\">" +
                    "<host name=\"myhost\">" +
                    "<probe type = \"PartitionSpace\">" +
                    "<arg type=\"String\" value=\"/\" />" +
                    "</probe>" +
                    "<probe type = \"TcpSnmp\">" +
                    "</probe>" +
                    "</host>";

    static DocumentBuilder dbuilder;

    @BeforeClass
    static public void configure() throws ParserConfigurationException {
        Tools.configure();
        Tools.prepareXml(false);
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.configuration", "jrds.Probe.DummyProbe", "jrds.factories");
        logrule.setLevel(Level.INFO, "jrds.factories.xml.CompiledXPath");
    }

    @Test
    public void testFilter() throws Exception {
        JrdsDocument d = Tools.parseRessource("view1.xml");
        PropertiesManager pm = Tools.makePm(testFolder);
        FilterBuilder fb = new FilterBuilder();
        fb.setPm(pm);
        Filter f = fb.makeFilter(d);
        Assert.assertEquals("Test view 1", f.getName());
    }

    @Test
    public void testProbe2() throws Exception {
        JrdsDocument d = Tools.parseString(goodProbeXml2);
        PropertiesManager pm = Tools.makePm(testFolder);
        HostBuilder hb = new HostBuilder();
        hb.setProbeFactory(new MokeProbeFactory());
        hb.setPm(pm);
        hb.setTimers(Tools.getSimpleTimerMap());

        HostInfo host = new HostInfo("testProbe2");
        host.setHostDir(pm.rrddir);

        Probe<?, ?> p = hb.makeProbe(d.getRootElement(), host, null);
        Assert.assertNotNull(p);
        Assert.assertEquals(host.getName() + "/" + p.getName(), p.toString());
    }

    @Test
    public void testDsreplace() throws Exception {
        JrdsDocument d = Tools.parseRessource("dsoverride.xml");
        PropertiesManager pm = Tools.makePm(testFolder);
        HostBuilder hb = new HostBuilder();
        ProbeFactory pf = new MokeProbeFactory();
        hb.setProbeFactory(pf);
        hb.setPm(pm);
        hb.setTimers(Tools.getSimpleTimerMap());

        HostInfo host = new HostInfo("testDsreplace");
        host.setHostDir(pm.rrddir);

        Probe<?, ?> p = hb.makeProbe(d.getRootElement().getElementbyName("probe"), host, null);
        ProbeDesc<?> pd = p.getPd();
        Assert.assertNotNull(pd);
        Assert.assertEquals(1, pd.getSize());
        Assert.assertNotSame(pf.getProbeDesc(pd.getName()), pd.getSize());
    }

    @Test
    public void testMacroLoad() throws Exception {
        JrdsDocument d = Tools.parseString(goodMacroXml);

        MacroBuilder b = new MacroBuilder();

        Macro m = b.makeMacro(d);
        int macroProbesNumber = m.getDf().getChildNodes().getLength();
        Assert.assertEquals("macrodef", m.getName());
        Assert.assertEquals("Macro$macrodef", m.toString());
        Assert.assertEquals(1, macroProbesNumber);
        Assert.assertEquals(2, m.getDf().getChildNodes().item(0).getChildNodes().getLength());
    }

    @Test
    public void testMacroFill() throws Exception {
        JrdsDocument d = Tools.parseString(goodMacroXml);

        MacroBuilder b = new MacroBuilder();

        Macro m = b.makeMacro(d);

        Map<String, Macro> macroMap = new HashMap<>();
        macroMap.put(m.getName(), m);

        JrdsDocument hostdoc = Tools.parseString(goodHostXml);
        hostdoc.setDocumentURI("-//jrds//DTD Graph Description//EN");

        hostdoc.getRootElement().addElement("macro", "name=macrodef");
        PropertiesManager pm = Tools.makePm(testFolder);
        HostBuilder hb = new HostBuilder();
        hb.setPm(pm);
        hb.setMacros(macroMap);
        hb.setProbeFactory(new MokeProbeFactory());
        hb.setTimers(Tools.getSimpleTimerMap());

        HostInfo host = hb.makeHost(hostdoc);

        logger.debug("probes:" + host.getProbes());
        Collection<String> probesName = new ArrayList<>();
        for(Probe<?, ?> p: host.getProbes()) {
            probesName.add(p.toString());
        }
        Assert.assertTrue("MacroProbe1 not found", probesName.contains("myhost/MacroProbe1"));
        Assert.assertTrue("MacroProbe2 not found", probesName.contains("myhost/MacroProbe2"));
    }

    @Test
    public void testTab() throws Exception {
        JrdsDocument tabNode = Tools.parseRessource("goodtab.xml");

        TabBuilder tb = new TabBuilder();
        Tab tab = tb.build(tabNode);

        Assert.assertEquals("Tab name not set", "goodtab", tab.getName());
    }

}
