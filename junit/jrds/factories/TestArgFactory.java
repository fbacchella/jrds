package jrds.factories;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Tools;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.NodeListIterator;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestArgFactory {
    static final private Logger logger = Logger.getLogger(TestArgFactory.class);
    
    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, TestIterator.class.getName(), NodeListIterator.class.getName());
        Tools.prepareXml(false);
    }
    
    @Test
    public void testArgsOldStyle() throws Exception {
        JrdsDocument d = Tools.parseString("<body><arg type=\"Integer\" value=\"1\" /></body>");
        List<Object> l = ArgFactory.makeArgs(d.getRootElement());
        Assert.assertEquals("Wrong number of elements", 1, l.size());
        Assert.assertEquals("Wrong value", new Integer(1), l.get(0));
    }

    @Test
    public void testArgsNewStyle() throws Exception {
        JrdsDocument d = Tools.parseString("<body><arg type=\"Integer\">1</arg><arg type=\"String\">1</arg></body>");
        List<Object> l = ArgFactory.makeArgs(d.getRootElement());
        Assert.assertEquals("Wrong number of elements", 2, l.size());
        Assert.assertEquals("Wrong value", new Integer(1), l.get(0));
        Assert.assertEquals("Wrong value", "1", l.get(1));
    }
    
}
