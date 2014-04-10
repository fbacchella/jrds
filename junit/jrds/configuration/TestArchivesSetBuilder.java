package jrds.configuration;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import jrds.ArchivesSet;
import jrds.Tools;
import jrds.factories.xml.JrdsDocument;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.rrd4j.ConsolFun;
import org.rrd4j.core.ArcDef;

public class TestArchivesSetBuilder {
    static final private Logger logger = Logger.getLogger(TestArchivesSetBuilder.class);

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        Tools.prepareXml(true);

        Tools.setLevel(logger, Level.TRACE, "jrds.configuration", "jrds.factories");
        Logger.getLogger("jrds.factories.xml.CompiledXPath").setLevel(Level.INFO);
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
