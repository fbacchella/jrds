package jrds.factories.xml;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;

import jrds.Log4JRule;
import jrds.Tools;

public class TestJrdsElement {
    
    @Rule
    public Log4JRule logrule = new Log4JRule(this);

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        Tools.prepareXml(false);
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.factories.xml");
    }

    @Test
    public void testIteration() throws TransformerException, IOException {
        JrdsDocument d = AbstractJrdsNode.build(Tools.dbuilder.newDocument());
        JrdsElement root = AbstractJrdsNode.build(d.appendChild(d.createElement("root")));
        for(int i = 0; i < 5; i++) {
            root.addElement("key", "val=" + i);
        }
        int val = 0;
        for(JrdsElement je: root.getChildElementsByName("key")) {
            Assert.assertEquals("missing val " + val, Integer.toString(val++), je.getAttribute("val"));
        }
    }

}
