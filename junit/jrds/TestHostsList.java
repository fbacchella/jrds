package jrds;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestHostsList {
    static final private Logger logger = Logger.getLogger(TestHostsList.class);
    static final private String[] optionalstabs = {"@", "sumstab", "customgraph" }; 

    @BeforeClass
    static public void configure() throws Exception {
        Tools.configure();
        logger.setLevel(Level.TRACE);
        Tools.setLevel(new String[] {HostsList.class.getName()}, logger.getLevel());
        System.setProperty("java.io.tmpdir",  "tmp");
    }

    @Test
    public void testDefault() {
        PropertiesManager pm = Tools.getCleanPM();
        pm.update();
        HostsList hl = new HostsList(pm);
        Assert.assertEquals("First tab not found", pm.tabsList.get(0), hl.getFirstTab());
        Assert.assertEquals("Missing tabs", pm.tabsList.size() - optionalstabs.length, hl.getTabsId().size());
    }
    @Test
    public void testOther() {
        PropertiesManager pm = Tools.getCleanPM();
        pm.setProperty("tabs", "filtertab");
        pm.update();
        HostsList hl = new HostsList(pm);
        Assert.assertEquals("First tab not found", pm.tabsList.get(0), hl.getFirstTab());
        Assert.assertEquals("Missing tabs", pm.tabsList.size(), hl.getTabsId().size());
    }
}
