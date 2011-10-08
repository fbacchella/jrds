package jrds.factories;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Tools;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Node;

public class TestIterator {
    private static final String xmlGoodExample = 
            "<root>" + 
                    "<child >1<subnode /></child>" +
                    "<child >2</child>" +
                    "</root>";
    private static final String xmlEmptyExample = "<root />";

    static final private Logger logger = Logger.getLogger(TestIterator.class);
    
    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, TestIterator.class.getName(), NodeListIterator.class.getName());
        Tools.prepareXml(false);
    }

    @Test
    public void iterateGood() throws Exception {
        JrdsDocument d = Tools.parseString(xmlGoodExample);
        int rank = 1;
        for(JrdsElement n: new NodeListIterator<JrdsElement>(d, Tools.xpather.compile("/root/*"))) {
            int i = Integer.parseInt(n.getTextContent());
            Assert.assertEquals("child", n.getNodeName());
            Assert.assertEquals(i, rank++);
        }
        Assert.assertEquals(3, rank++);

    }
    
    @Test
    public void iterateEmpty() throws Exception {
        JrdsDocument d = Tools.parseString(xmlEmptyExample);
        int rank = 1;
        for(Node n: new NodeListIterator<JrdsDocument>(d, Tools.xpather.compile("/root/*"))) {
            int i = Integer.parseInt(n.getTextContent());
            Assert.assertEquals(rank++, i);
        }
        Assert.assertEquals(1, rank);

    }
}

