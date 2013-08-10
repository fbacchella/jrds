package jrds.configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;

import jrds.HostInfo;
import jrds.Macro;
import jrds.Probe;
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.factories.xml.JrdsDocument;
import jrds.mockobjects.MokeProbe;
import jrds.mockobjects.MokeProbeFactory;
import jrds.starter.ConnectionInfo;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.DocumentFragment;

public class TestMacro {
    static final private Logger logger = Logger.getLogger(TestMacro.class);

    static final private String goodMacroXml =
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

    static final private String goodHostXml = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
                    "<!DOCTYPE host PUBLIC \"-//jrds//DTD Host//EN\" \"urn:jrds:host\">" +
                    "<host name=\"myhost\">" +
                    "<macro name=\"macrodef\" />" +
                    "</host>";

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        Tools.prepareXml(false);

        Tools.setLevel(logger, Level.TRACE, "jrds.configuration.HostBuilder", "jrds.factories", "jrds.starter.ChainedProperties", "jrds.factories.xml");
        Logger.getLogger("jrds.factories.xml.CompiledXPath").setLevel(Level.INFO);
    }

    static private Macro doMacro(JrdsDocument d, String name) {
        DocumentFragment df = d.createDocumentFragment();
        df.appendChild(d.removeChild(d.getDocumentElement()));

        Macro m = new Macro();
        m.setDf(df);
        m.setName(name);
        return m;
    }

    private HostBuilder getBuilder(Macro... macros) throws IOException {
        HostBuilder hb = new HostBuilder();
        hb.setPm(Tools.makePm(testFolder));
        hb.setTimers(Tools.getSimpleTimerMap());
        Map<String, Macro> mmap = new HashMap<String, Macro>(macros.length);
        for(Macro m: macros) {
            mmap.put(m.getName(), m);
        }
        hb.setMacros(mmap);
        hb.setProbeFactory(new MokeProbeFactory());
        return hb;
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
        Macro m = doMacro(d, "macrodef");

        JrdsDocument hostdoc = Tools.parseString(goodHostXml);

        HostBuilder hb = getBuilder(m);

        HostInfo host = hb.makeHost(hostdoc);

        Collection<String> probesName = new ArrayList<String>();
        for(Probe<?,?> p: host.getProbes()) {
            probesName.add(p.toString());
        }
        logger.trace(probesName);
        Assert.assertTrue("MacroProbe1 not found", probesName.contains("myhost/MacroProbe1"));
        Assert.assertTrue("MacroProbe2 not found", probesName.contains("myhost/MacroProbe2"));
    }

    @Test
    public void testMacroStarter() throws Exception {
        JrdsDocument d = Tools.parseString(goodMacroXml);
        d.getRootElement().addElement("snmp", "community=public", "version=2");

        Macro m = doMacro(d, "macrodef");

        HostBuilder hb = getBuilder(m);

        JrdsDocument hostdoc = Tools.parseString(goodHostXml);
        HostInfo host = hb.makeHost(hostdoc);

        ConnectionInfo found = null;
        for(ConnectionInfo ci: host.getConnections()) {
            if("jrds.snmp.SnmpConnection".equals(ci.getName())) {
                found = ci;
                break;
            }
        }
        Assert.assertNotNull("SNMP starter not found", found);
        Assert.assertEquals("Starter not found", "jrds.snmp.SnmpConnection", found.getName());
    }

    @Test
    public void testMacroTag() throws Exception {
        JrdsDocument d = Tools.parseString(goodMacroXml);
        String tagname = "mytag";
        d.getRootElement().addElement("tag").setTextContent(tagname);

        Macro m = doMacro(d, "macrodef");
        HostBuilder hb = getBuilder(m);

        JrdsDocument hostdoc = Tools.parseString(goodHostXml);
        HostInfo host = hb.makeHost(hostdoc);
        Assert.assertTrue("tag not found", host.getTags().contains(tagname));
    }

    @Test
    public void testMacroFillwithProps1() throws Exception {
        JrdsDocument d = Tools.parseString(goodMacroXml);
        d.getRootElement().addElement("entry", "key=a").setTextContent("bidule");
        Macro m = doMacro(d, "macrodef");

        JrdsDocument hostdoc = Tools.parseString(goodHostXml);
        hostdoc.getRootElement().addElement("probe", "type=MacroProbe3").addElement("arg", "type=String", "value=${a}");

        HostBuilder hb = getBuilder(m);
        HostInfo host = hb.makeHost(hostdoc);

        boolean found = false;
        for(Probe<?,?> p: host.getProbes()) {
            if("myhost/MacroProbe3".equals(p.toString()) ) {
                MokeProbe<?,?> mp = (MokeProbe<?,?>) p;
                logger.trace("Args:" + mp.getArgs());
                Assert.assertFalse(mp.getArgs().contains("bidule"));
                found = true;
            }
        }
        Assert.assertTrue("macro probe with properties not found", found);
    }

    @Test
    public void testMacroFillwithProps2() throws Exception {
        Map<String, String> prop = new HashMap<String, String>();
        prop.put(OutputKeys.INDENT, "yes");
        prop.put(OutputKeys.INDENT, "yes");
        prop.put("{http://xml.apache.org/xslt}indent-amount", "4");

        JrdsDocument d = Tools.parseString(goodMacroXml);
        d.getRootElement().addElement("probe", "type=MacroProbe3").addElement("attr", "name=val").addTextNode("${a}");
        jrds.Util.serialize(d, System.out, null, prop);
        System.out.println();

        Macro m = doMacro(d, "macrodef");
        m.getDf();

        JrdsDocument hostdoc = Tools.parseString(goodHostXml);
        hostdoc.getRootElement().getChildElementsByName("macro").iterator().next().addElement("arg", "type=String", "value=${a}");
        jrds.Util.serialize(hostdoc, System.out, null, prop);
        System.out.println();

        HostBuilder hb = getBuilder(m);
        HostInfo host = hb.makeHost(hostdoc);

        boolean found = false;
        for(Probe<?,?> p: host.getProbes()) {
            if("myhost/MacroProbe1".equals(p.toString()) ) {
                MokeProbe<?,?> mp = (MokeProbe<?,?>) p;
                logger.trace("Args:" + mp.getArgs());
                Assert.assertFalse(mp.getArgs().contains("bidule"));
                found = true;
            }
            else if ("myhost/MacroProbe2".equals(p.toString())) {
                @SuppressWarnings("unused")
                MokeProbe<?,?> mp = (MokeProbe<?,?>) p;
            }
        }
        Assert.assertTrue("macro probe with properties not found", found);
    }

    @Test
    public void testCollection() throws Exception {
        JrdsDocument d = Tools.parseString(goodMacroXml);
        Tools.appendString(Tools.appendString(Tools.appendString(d.getDocumentElement(), "<for var=\"a\" collection=\"c\"/>"),"<probe type = \"MacroProbe3\" />"), "<arg type=\"String\" value=\"${a}\" />");
        jrds.Util.serialize(d, System.err, null, null);

        Macro m = doMacro(d, "macrodef");

        JrdsDocument hostdoc = Tools.parseString(goodHostXml);
        Tools.appendString(Tools.appendString(hostdoc.getDocumentElement(), "<collection name=\"c\"/>"), "<element>bidule</element>");

        HostBuilder hb = getBuilder(m);
        HostInfo host = hb.makeHost(hostdoc);

        boolean found = false;
        for(Probe<?,?> p: host.getProbes()) {
            if("myhost/MacroProbe3".equals(p.toString()) ) {
                MokeProbe<?,?> mp = (MokeProbe<?,?>) p;
                logger.trace("Args:" + mp.getArgs());
                Assert.assertTrue(mp.getArgs().contains("bidule"));
                found = true;
            }
        }
        Assert.assertTrue("collection not found", found);
    }

    @Test
    public void testRecursive() throws Exception {
        JrdsDocument d1 = Tools.parseString(goodMacroXml);
        Tools.appendString(d1.getDocumentElement(), "<macro name=\"macrodef2\" />");
        Macro m1 = doMacro(d1, "macrodef");

        JrdsDocument d2 = Tools.parseString(goodMacroXml);
        Tools.appendString(Tools.appendString(d2.getDocumentElement(), "<probe type = \"MacroProbe3\" />"), "<arg type=\"String\" value=\"bidule\" />");
        Macro m2 = doMacro(d2, "macrodef2");

        HostBuilder hb = getBuilder(m1, m2);
        JrdsDocument hostdoc = Tools.parseString(goodHostXml);
        HostInfo host = hb.makeHost(hostdoc);

        boolean found = false;
        for(Probe<?,?> p: host.getProbes()) {
            if("myhost/MacroProbe3".equals(p.toString()) ) {
                MokeProbe<?,?> mp = (MokeProbe<?,?>) p;
                logger.trace("Args:" + mp.getArgs());
                Assert.assertTrue(mp.getArgs().contains("bidule"));
                found = true;
            }
        }
        Assert.assertTrue("macro not recursive", found);
    }

    @Test
    public void testFullConfigpath() throws Exception {
        PropertiesManager localpm = Tools.makePm();
        ConfigObjectFactory conf = new ConfigObjectFactory(localpm, localpm.extensionClassLoader);
        conf.getNodeMap(ConfigType.MACRODEF).put("macrodef", Tools.parseString(goodMacroXml));
        Assert.assertNotNull("Macro not build", conf.setMacroMap().get("macrodef"));
    }

}
