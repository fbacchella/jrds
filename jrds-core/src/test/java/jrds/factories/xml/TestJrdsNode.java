package jrds.factories.xml;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import jrds.Log4JRule;
import jrds.Tools;

public class TestJrdsNode {
    static class Convert {
        @SafeVarargs
        static <T> T[] convert(T... types) {
            return types;
        }
    }

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);

    @BeforeClass
    static public void configure() throws ParserConfigurationException {
        Tools.configure();
        Tools.prepareXml();
    }

    @Test
    public void testFindbyPath() throws Exception {
        JrdsDocument d = Tools.parseRessource("graphdesc.xml");
        Assert.assertNotNull("path not found", d.getRootElement().findByPath("."));
        Assert.assertNull("path not found", d.getRootElement().findByPath("/graph"));
    }

}
