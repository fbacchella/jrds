package jrds.webapp;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import jrds.Configuration;
import jrds.Tools;
import jrds.mockobjects.MokeServletContext;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestConfiguration {
    static final private Logger logger = Logger.getLogger(TestConfiguration.class);

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws IOException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, "jrds.HostList", "jrds.PropertiesManager", ParamsBean.class.getName(), Configuration.class.getName());
    }

    @Test
    public void test1() throws IOException {
        MokeServletContext sc = new MokeServletContext();
        File tempRoot = testFolder.getRoot();
        File configDir = new File(tempRoot, "config");
        sc.initParameters.put("tmpdir", tempRoot.getPath());
        sc.initParameters.put("rrddir", tempRoot.getPath());
        sc.initParameters.put("configdir", configDir.getPath());
        sc.initParameters.put("autocreate", "true");
        StartListener sl = new StartListener();
        Properties p = sl.readProperties(sc);
        Configuration.configure(p);
        Configuration.switchConf(p);
        Assert.assertTrue("confid dir " + configDir + " not created", configDir.isDirectory());
    }
}
