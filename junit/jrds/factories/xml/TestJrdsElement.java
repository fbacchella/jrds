package jrds.factories.xml;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import jrds.Tools;
import jrds.Util;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestJrdsElement {
    static final private Logger logger = Logger.getLogger(TestJrdsNode.class);

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        Tools.prepareXml(false);
        Tools.setLevel(logger, Level.TRACE, "jrds.factories.xml");
    }
    
    @Test
    public void testIteration() throws TransformerException, IOException {
        JrdsDocument d = AbstractJrdsNode.build(Tools.dbuilder.newDocument());
        JrdsElement root = AbstractJrdsNode.build(d.appendChild(d.createElement("root")));
        for(int i=0; i<5; i++) {
            root.addElement("key", "val=" + i);
        }
        Util.serialize(d, System.out, null, null);
        System.out.println();
        int val = 0;
        for(JrdsElement je:  root.getChildElementsByName("key")) {
            Assert.assertEquals("missing val " + val, Integer.toString(val++), je.getAttribute("val"));
        }
    }

}
