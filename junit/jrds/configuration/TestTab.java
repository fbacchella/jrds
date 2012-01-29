package jrds.configuration;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import jrds.HostsList;
import jrds.PropertiesManager;
import jrds.Tab;
import jrds.Tools;
import jrds.factories.xml.JrdsDocument;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestTab {
    static final private Logger logger = Logger.getLogger(TestTab.class);

    static private PropertiesManager pm = null;
    static final private HostsList hl = new HostsList();

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        Tools.prepareXml(true);

        pm = Tools.makePm("security=true");

        hl.configure(pm);

        Tools.setLevel(logger, Level.TRACE, "jrds.configuration", "jrds.factories");
        Logger.getLogger("jrds.factories.xml.CompiledXPath").setLevel(Level.INFO);
    }

    @Test
    public void testLoad() throws Exception {
        JrdsDocument d = Tools.parseRessource("goodtab.xml");

        TabBuilder tb = new TabBuilder();
        tb.setPm(pm);

        Tab tab = tb.build(d);
        tab.setHostlist(hl);
        Assert.assertEquals("goodtab", tab.getName());
        Assert.assertNotNull("No graph tree generated", tab.getGraphTree());
    }

}
