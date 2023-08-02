package jrds.factories;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.w3c.dom.Node;

import jrds.Log4JRule;
import jrds.Tools;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;
import jrds.factories.xml.NodeListIterator;

public class TestIterator {
    private static final String xmlGoodExample = 
            "<root>" + 
                    "<child >1<subnode /></child>" +
                    "<child >2</child>" +
                    "</root>";
    private static final String xmlEmptyExample = "<root />";

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);

    @BeforeClass
    static public void configure() throws ParserConfigurationException {
        Tools.configure();
        Tools.prepareXml(false);
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, TestIterator.class.getName(), NodeListIterator.class.getName());
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
