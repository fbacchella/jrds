package jrds;

import java.net.URL;

import jrds.factories.ArgFactory;

import org.junit.Assert;
import org.junit.Test;

public class FactoryTest {
    @Test
    public void argFactory() {
        try {
            Object o = ArgFactory.makeArg("Integer", "1");
            Assert.assertEquals(o, new Integer(1));
            o = ArgFactory.makeArg("URL", "http://localhost/");
            Assert.assertEquals(o, new URL("http://localhost/"));
            o = ArgFactory.makeArg(Integer.class.getName(), "1");
            Assert.assertEquals(o, new Integer(1));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }


}
