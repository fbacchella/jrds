package jrds.factories.xml;


import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Tools;
import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestJrdsNode {
    static class Convert {
        static <T> T[] convert(T... types) {
            return types;
        }
    };

    static final private Logger logger = Logger.getLogger(TestJrdsNode.class);

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        logger.setLevel(Level.TRACE);

        Tools.prepareXml();
    }

    @Test
    public void testCheckPath() throws Exception {
        JrdsDocument d = Tools.parseRessource("graphdesc.xml");
        Assert.assertTrue("path not found", d.checkPath(CompiledXPath.get("/graphdesc")));
        Assert.assertFalse("path not found", d.checkPath(CompiledXPath.get("/graph")));
    }

    @Test
    public void testCallIfExist() throws Exception {
        JrdsDocument d = Tools.parseRessource("graphdesc.xml");
        String dummy = "";
        Class<?>[] argsTypes = {String.class};
        Object[] argsValues = {"toto"};
        Object value = d.callIfExist(dummy, CompiledXPath.get("/graphdesc"), "concat", argsTypes, argsValues);
        Assert.assertEquals("path not found", value, "toto");
        Assert.assertNull("path found", d.callIfExist(dummy, CompiledXPath.get("/graph"), "concat", argsTypes, argsValues));
    }

    @Test
    public void testSetMethod() throws Exception {
        JrdsDocument d = Tools.parseRessource("customgraph.xml");
        StringBuffer dummy = new StringBuffer();
        d.setMethod(dummy, CompiledXPath.get("/graph/height"), "append", Integer.TYPE);
        Assert.assertEquals("path not found", dummy.toString(), "800");
    }

}
