package jrds;

import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestStoreOpener {
    static final private Logger logger = Logger.getLogger(TestStoreOpener.class);

    @BeforeClass
    static public void configure() throws IOException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, "jrds.StoreOpener");
    }

    @Test
    public void test1() {
        StoreOpener.prepare("MEMORY");
    }
    
    @Test
    public void test2() {
        StoreOpener.prepare("NIO");
    }

    @Test
    public void test3() {
        StoreOpener.prepare(1, 1, "NIO");
        StoreOpener.prepare(1, 1, "FILE");
        StoreOpener.prepare(1, 1, "MEMORY");
    }

}
