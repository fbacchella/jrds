package jrds.configuration;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Probe;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.Util;
import jrds.factories.xml.JrdsDocument;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestProbeDescBuilder {
    static final private Logger logger = Logger.getLogger(TestProbeDescBuilder.class);

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, "jrds.ProbeDesc");
        Tools.setLevel(Level.INFO,"jrds.factories.xml.CompiledXPath");

        Tools.prepareXml();
    }

    @Test
    public void testFullConfigpath() throws Exception {
        PropertiesManager localpm = Tools.makePm();
        ConfigObjectFactory conf = new ConfigObjectFactory(localpm, localpm.extensionClassLoader);
        conf.getNodeMap(ConfigType.PROBEDESC).put("name", Tools.parseRessource("httpxmlprobedesc.xml"));

        ProbeDesc pd = conf.setProbeDescMap().get("name");        
        Assert.assertNotNull("Probedesc not build", pd);
    }

    @Test
    public void testOptional() throws Exception {
        PropertiesManager localpm = Tools.makePm();
        ConfigObjectFactory conf = new ConfigObjectFactory(localpm, localpm.extensionClassLoader);
        JrdsDocument pddoc = Tools.parseRessource("httpxmlprobedesc.xml");
        pddoc.getRootElement().getElementbyName("probeClass").setTextContent("jrds.mockobjects.MokeProbeBean");

        conf.getNodeMap(ConfigType.PROBEDESC).put("name", pddoc);

        ProbeDesc pd = conf.setProbeDescMap().get("name");        
        Assert.assertNotNull("Probedesc not build", pd);

        @SuppressWarnings("unchecked")
        Probe<String, String> p = (Probe<String, String>) pd.getProbeClass().getConstructor().newInstance();
        p.setPd(pd);
        p.setLabel("goodlabel");
        Assert.assertTrue("optional resolution broken", p.isOptional("goodlabel"));
    }

    @Test
    public void testDefaultArgs() throws Exception {
        PropertiesManager localpm = Tools.makePm();
        ConfigObjectFactory conf = new ConfigObjectFactory(localpm, localpm.extensionClassLoader);
        JrdsDocument pddoc = Tools.parseRessource("beans.xml");
        pddoc.getRootElement().getElementbyName("probeClass").setTextContent("jrds.mockobjects.MokeProbeBean");
        conf.getNodeMap(ConfigType.PROBEDESC).put("name", pddoc);
        ProbeDesc pd = conf.setProbeDescMap().get("name");
        Assert.assertEquals("bean default value not found", "defaultattr1", pd.getDefaultBeans().get("customattr1").value);
        Assert.assertEquals("default attribute not delayed", true, pd.getDefaultBeans().get("customattr2").delayed);
    }

    @Test
    public void testCustomBeans() throws Exception {
        PropertiesManager localpm = Tools.makePm();
        ConfigObjectFactory conf = new ConfigObjectFactory(localpm, localpm.extensionClassLoader);
        JrdsDocument pddoc = Tools.parseRessource("beans.xml");
        pddoc.getRootElement().getElementbyName("probeClass").setTextContent("jrds.mockobjects.MokeProbeBean");
        conf.getNodeMap(ConfigType.PROBEDESC).put("name", pddoc);
        ProbeDesc pd = conf.setProbeDescMap().get("name");
        Assert.assertNotNull("custom bean customattr1 not found", pd.getBean("customattr1"));
        Assert.assertNotNull("custom bean customattr2 not found", pd.getBean("customattr2"));

        @SuppressWarnings("unchecked")
        Probe<String, String> p = (Probe<String, String>) pd.getProbeClass().getConstructor().newInstance();
        p.setPd(pd);        
        pd.getBean("customattr1").set(p, "value1");
        pd.getBean("customattr2").set(p, "value2");

        Assert.assertEquals("value1", pd.getBean("customattr1").get(p));
        Assert.assertEquals("value2", pd.getBean("customattr2").get(p));

        Assert.assertEquals("value1", Util.parseTemplate("${attr.customattr1}", p));
        Assert.assertEquals("value2", Util.parseTemplate("${attr.customattr2}", p));

    }
}
