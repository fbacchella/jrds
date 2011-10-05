package jrds;


import java.io.IOException;
import java.util.Map;

import jrds.starter.ChainedProperties;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestPropertyStarter {
    @BeforeClass
    static public void configure() throws IOException {
        Tools.configure();
    }

    @Test
    public void test1() {
        String prop = "__TOTEST__";
        System.setProperty(prop, prop);
        ChainedProperties ps1 = new ChainedProperties();
        ps1.put("key1." + prop, "value1." + prop);
        Map<String, String> ps2 = new ChainedProperties(ps1);
        ps2.put("key2." + prop, "value2." + prop);
        ps2.put("key3." + prop, "value3." + prop);
        Assert.assertEquals("value1." + prop, ps2.get("key1." + prop));
        Assert.assertEquals("value2." + prop, ps2.get("key2." + prop));
        Assert.assertEquals("value3." + prop, ps2.get("key3." + prop));
    }
}
