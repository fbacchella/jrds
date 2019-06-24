package jrds.configuration;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.rrd4j.ConsolFun;
import org.rrd4j.core.ArcDef;
import org.slf4j.event.Level;

import jrds.ArchivesSet;
import jrds.Log4JRule;
import jrds.Tools;
import jrds.factories.xml.JrdsDocument;

public class TestArchivesSetBuilder {
    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        Tools.prepareXml(true);
    }

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);

    @Before
    public void loggers() {
        logrule.setLevel(Level.INFO, "jrds.factories.xml.CompiledXPath");
        logrule.setLevel(Level.TRACE, "jrds.configuration", "jrds.factories");
    }

    @Test
    public void testLoad() throws Exception {
        JrdsDocument d = Tools.parseRessource("goodarchives.xml");

        ArchivesSetBuilder asb = new ArchivesSetBuilder();

        ArchivesSet arcset = asb.build(d);
        ArcDef[] arcs = arcset.getArchives();

        Assert.assertEquals("wrong name for archives set", "newarchives", arcset.getName());

        Assert.assertEquals("not enough archives", 2, arcs.length);
        Assert.assertEquals("not enough archives", ConsolFun.AVERAGE, arcs[0].getConsolFun());
        Assert.assertEquals("not enough archives", ConsolFun.MAX, arcs[1].getConsolFun());
    }

}
