package jrds.caching;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Tools;
import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestLRUMap {
    static final private Logger logger = Logger.getLogger(TestLRUMap.class);

    @BeforeClass
    static public void configure() throws IOException, ParserConfigurationException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, "jrds.caching.LRUMap");
    }
    
    @Test
    public void test1() {
        LRUMap<Integer, Integer> m = new LRUMap<Integer,Integer>(5);
        int i=0;
        m.put(++i, i);
        m.put(++i, i);
        m.put(++i, i);
        m.put(++i, i);
        m.put(++i, i);
        m.put(++i, i);
        Assert.assertEquals("", new Integer(4), m.get(4));
        Assert.assertEquals("", new Integer(1), m.removeEldest());
        m.putLast(++i, i);
        Assert.assertEquals("", new Integer(i), m.removeEldest());
        m.dumpMap();
    }

}
