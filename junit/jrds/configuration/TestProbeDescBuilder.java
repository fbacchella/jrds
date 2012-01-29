package jrds.configuration;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.Tools;
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
        Assert.assertNotNull("Probedesc not build", conf.setProbeDescMap().get("name"));
    }

    @Test
    public void testDefaultArgs() throws Exception {
        PropertiesManager localpm = Tools.makePm();
        ConfigObjectFactory conf = new ConfigObjectFactory(localpm, localpm.extensionClassLoader);
        JrdsDocument pddoc = Tools.parseRessource("fulldesc.xml");
        pddoc.getRootElement().getElementbyName("probeClass").setTextContent("jrds.mockobjects.MokeProbeBean");
        conf.getNodeMap(ConfigType.PROBEDESC).put("name", pddoc);
        ProbeDesc pd = conf.setProbeDescMap().get("name");
        Assert.assertEquals("bean default value not found", "beanvalue", pd.getDefaultArgs().get("hostInfo"));
    }
}
