package jrds.factories;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import jrds.Tools;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class TestIterator {
    private final String xmlGoodExample = 
            "<root>" + 
                    "<child >1<subnode /></child>" +
                    "<child >2</child>" +
                    "</root>";
    private final String xmlEmptyExample = "<root />";

    static final private Logger logger = Logger.getLogger(TestIterator.class);
    @BeforeClass static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, TestIterator.class.getName(), NodeListIterator.class.getName());
        Tools.prepareXml(false);
    }

    @Test
    public void iterateGood() throws SAXException, IOException, XPathExpressionException {
        InputStream is = new ByteArrayInputStream(xmlGoodExample.getBytes()) ;
        Document d = Tools.dbuilder.parse(is); 
        int rank = 1;
        for(Node n: new NodeListIterator(d, Tools.xpather.compile("/root/*"))) {
            int i = Integer.parseInt(n.getTextContent());
            Assert.assertEquals("child", n.getNodeName());
            Assert.assertEquals(i, rank++);
        }
        Assert.assertEquals(3, rank++);

    }
    public void iterateEmpty() throws SAXException, IOException, XPathExpressionException {
        InputStream is = new ByteArrayInputStream(xmlEmptyExample.getBytes()) ;
        Document d = Tools.dbuilder.parse(is); 
        int rank = 1;
        for(Node n: new NodeListIterator(d, Tools.xpather.compile("/root/*"))) {
            int i = Integer.parseInt(n.getTextContent());
            Assert.assertEquals(rank++, i);
        }
        Assert.assertEquals(1, rank);

    }
    public void iterateBad() throws SAXException, IOException, XPathExpressionException {
        InputStream is = new ByteArrayInputStream(xmlGoodExample.getBytes()) ;
        Document d = Tools.dbuilder.parse(is); 
        for(Node n: new NodeListIterator(d, Tools.xpather.compile("--"))) {
            n.getTextContent();
        }
    }
}

