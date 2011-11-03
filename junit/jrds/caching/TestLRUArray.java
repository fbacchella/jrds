package jrds.caching;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Tools;
import junit.framework.Assert;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestLRUArray {
    static final private Logger logger = Logger.getLogger(TestLRUArray.class);
    static final private int numElems = 5;

    @BeforeClass
    static public void configure() throws IOException, ParserConfigurationException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, "jrds.caching.LRUMap");
    }
    
    @Test
    public void test1() {
        LRUArray<Integer> m = new LRUArray<Integer>(5);
        int i=0;
        m.put(++i % numElems, i);
        m.put(++i % numElems, i);
        m.put(++i % numElems, i);
        m.put(++i % numElems, i);
        m.put(++i % numElems, i);
        m.put(++i % numElems, i);
        Assert.assertEquals("", new Integer(4), m.get(4));
        Assert.assertEquals("", new Integer(2), m.removeEldest());
        m.putLast(++i % numElems, i);
        Assert.assertEquals("", new Integer(i), m.removeEldest());
        m.dumpMap();
    }

}
