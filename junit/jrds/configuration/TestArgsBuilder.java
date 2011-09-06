package jrds.configuration;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Tools;
import jrds.factories.ArgFactory;
import jrds.factories.xml.JrdsNode;
import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

public class TestArgsBuilder {
    static final private Logger logger = Logger.getLogger(TestArgsBuilder.class);

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        logger.setLevel(Level.ERROR);
        Tools.setLevel(new String[] {"jrds"}, logger.getLevel());
        Tools.prepareXml();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test1() throws Exception {
        Document d = Tools.parseRessource("args.xml");
        List<Object> o = ArgFactory.makeArgs(new JrdsNode(d.getFirstChild()));
        Assert.assertEquals(1, o.get(0));
        Assert.assertEquals("string", o.get(1));
        Assert.assertEquals(new URL("http://localhost/"), o.get(2));
        List<Object> a3 = (List<Object>) o.get(3);
        Assert.assertEquals(4, a3.size());
        List<Object> suba3 = (List<Object>) a3.get(3);
        Assert.assertEquals(3, suba3.size());
        List<Object> a4 = (List<Object>) o.get(4);
        Assert.assertEquals(0, a4.size());

    }

}
