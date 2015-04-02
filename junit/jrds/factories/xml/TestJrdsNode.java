package jrds.factories.xml;


import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Tools;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestJrdsNode {
    static class Convert {
        static <T> T[] convert(T... types) {
            return types;
        }
    }

    static final private Logger logger = Logger.getLogger(TestJrdsNode.class);

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        logger.setLevel(Level.TRACE);

        Tools.prepareXml();
    }
    
    @Test
    public void testFindbyPath() throws Exception {
        JrdsDocument d = Tools.parseRessource("graphdesc.xml");
        Assert.assertNotNull("path not found", d.getRootElement().findByPath("."));
        Assert.assertNull("path not found", d.getRootElement().findByPath("/graph"));
    }

}
