package jrds.configuration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Probe;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestBeans {
    static final private Logger logger = Logger.getLogger(TestBeans.class);

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        logger.setLevel(Level.DEBUG);
        Tools.setLevel(logger, Level.TRACE, "jrds.Util");
    }

    @Test
    public void enumerateProbe() throws IOException {
        PropertiesManager pm = Tools.makePm(testFolder, "autocreate=false", "strictparsing=true");
        // = new PropertiesManager();
        pm.update();
        pm.libspath.clear();
        //pm.strictparsing = true;
        //pm.rrdbackend = "FILE";
        File descpath = new File("desc");
        if(descpath.exists())
            pm.libspath.add(descpath.toURI());
        ConfigObjectFactory conf = new ConfigObjectFactory(pm);
        Set<String> failed = new HashSet<String>();
        for(JrdsDocument n: conf.getNodeMap(ConfigType.PROBEDESC).values()) {
            JrdsElement root = n.getRootElement();
            JrdsElement classElem = root.getElementbyName("probeClass");
            ProbeDesc pd = new ProbeDesc();
            String name = "";
            try {
                String className = classElem.getTextContent().trim();
                name = root.getElementbyName("name").getTextContent();
                @SuppressWarnings("unchecked")
                Class<? extends Probe<?,?>> c = (Class<? extends Probe<?,?>>) pm.extensionClassLoader.loadClass(className);
                pd.setProbeClass(c);
            } catch (Exception e) {
                logger.error("Failed probedesc " + name + ": " + e.getMessage());
                failed.add(name);
            }
        }
        if(! failed.isEmpty()) {
            Assert.fail("Invalid class:" + failed);
        }
    }

}
