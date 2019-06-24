package jrds.configuration;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.event.Level;

import jrds.HostsList;
import jrds.Log4JRule;
import jrds.PropertiesManager;
import jrds.Tab;
import jrds.Tools;
import jrds.factories.xml.JrdsDocument;

public class TestTab {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public Log4JRule logrule = new Log4JRule(this);

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        Tools.prepareXml(true);
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.configuration", "jrds.factories");
        logrule.setLevel(Level.INFO, "jrds.factories.xml.CompiledXPath");
    }


    @Test
    public void testLoad() throws Exception {
        PropertiesManager pm = Tools.makePm(testFolder, "security=true");
        HostsList hl = new HostsList();
        hl.configure(pm);

        JrdsDocument d = Tools.parseRessource("goodtab.xml");

        TabBuilder tb = new TabBuilder();
        tb.setPm(pm);

        Tab tab = tb.build(d);
        tab.setHostlist(hl);
        Assert.assertEquals("goodtab", tab.getName());
        Assert.assertNotNull("No graph tree generated", tab.getGraphTree());
    }

}
