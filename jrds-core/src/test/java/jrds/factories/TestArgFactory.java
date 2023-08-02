package jrds.factories;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import jrds.Log4JRule;
import jrds.Tools;
import jrds.factories.xml.JrdsDocument;

public class TestArgFactory {

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);

    @BeforeClass
    static public void configure() throws ParserConfigurationException {
        Tools.configure();
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

    @Test
    public void testArgsRecurse() throws Exception {
        JrdsDocument d = Tools.parseString("<body><list><arg type=\"Integer\">1</arg><arg type=\"String\">1</arg></list><arg type=\"Integer\">1</arg></body>");
        List<Object> l = ArgFactory.makeArgs(d.getRootElement());
        Assert.assertEquals("Wrong number of elements", 2, l.size());
        Assert.assertTrue("Wrong value", l.get(0) instanceof List);
        Assert.assertEquals("Wrong value", new Integer(1), l.get(1));
    }

    @Test
    public void testResolve() {
        Class<?> c = ArgFactory.resolvClass("Integer");
        Assert.assertEquals("can't resolve type Integer", c, Integer.class);

        c = ArgFactory.resolvClass("int");
        Assert.assertEquals("can't resolve type File", c, Integer.TYPE);

        c = ArgFactory.resolvClass("URL");
        Assert.assertEquals("can't resolve type URL", c, URL.class);

        c = ArgFactory.resolvClass("File");
        Assert.assertEquals("can't resolve type File", c, File.class);
    }

    @Test
    public void testConstruct() throws InvocationTargetException {
        Object o;

        o = ArgFactory.ConstructFromString(Integer.class, "1");
        Assert.assertEquals("can't build type Integer", o.getClass(), Integer.class);

        o = ArgFactory.ConstructFromString(Integer.TYPE, "1");
        Assert.assertEquals("can't build type Integer", o.getClass(), Integer.class);

        o = ArgFactory.ConstructFromString(URL.class, "http://localhost");
        Assert.assertEquals("can't build type URL", o.getClass(), URL.class);

        o = ArgFactory.ConstructFromString(File.class, "/");
        Assert.assertEquals("can't build type File", o.getClass(), File.class);
    }

}
