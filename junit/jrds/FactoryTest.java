package jrds;

import java.io.IOException;
import java.net.URL;

import jrds.factories.ArgFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class FactoryTest {
    static final private Logger logger = Logger.getLogger(FactoryTest.class);

    @BeforeClass
    static public void configure() throws IOException {
        Tools.configure();
        StoreOpener.prepare("FILE");
        Tools.setLevel(logger, Level.TRACE, "jrds.factories");
    }

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
