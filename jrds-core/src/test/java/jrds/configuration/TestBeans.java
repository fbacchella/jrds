package jrds.configuration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import jrds.Log4JRule;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;

public class TestBeans {

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
    }

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);
    private final Logger logger = logrule.getTestlogger();

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.Util");
    }

    @Test
    public void enumerateProbe() throws IOException {
        PropertiesManager pm = Tools.makePm(testFolder, "autocreate=false", "strictparsing=true");
        pm.update();
        pm.libspath.clear();
        File descpath = new File("desc");
        if(descpath.exists())
            pm.libspath.add(descpath.toURI());
        ConfigObjectFactory conf = new ConfigObjectFactory(pm);
        Set<String> failed = new HashSet<String>();
        for(JrdsDocument n: conf.getNodeMap(ConfigType.PROBEDESC).values()) {
            JrdsElement root = n.getRootElement();
            JrdsElement classElem = root.getElementbyName("probeClass");
            ProbeDesc<String> pd = new ProbeDesc<>();
            String name = "";
            try {
                String className = classElem.getTextContent().trim();
                name = root.getElementbyName("name").getTextContent();
                @SuppressWarnings("unchecked")
                Class<? extends Probe<String, ?>> c = (Class<? extends Probe<String, ?>>) pm.extensionClassLoader.loadClass(className);
                pd.setProbeClass(c);
            } catch (Exception e) {
                logger.error("Failed probedesc " + name + ": " + e.getMessage());
                failed.add(name);
            }
        }
        if(!failed.isEmpty()) {
            Assert.fail("Invalid class:" + failed);
        }
    }

}
