package jrds.configuration;

import java.net.URL;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;

import jrds.Log4JRule;
import jrds.Tools;
import jrds.factories.ArgFactory;
import jrds.factories.xml.JrdsDocument;

public class TestArgsBuilder {

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);

    @BeforeClass
    static public void configure() throws ParserConfigurationException {
        Tools.configure();
        Tools.prepareXml(false);
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.factories");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test1() throws Exception {
        JrdsDocument d = Tools.parseRessource("args.xml");
        List<Object> o = ArgFactory.makeArgs(d.getRootElement());
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
