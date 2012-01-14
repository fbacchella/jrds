package jrds.configuration;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Probe;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.RdsHost;
import jrds.Tools;
import jrds.factories.xml.JrdsDocument;
import jrds.mockobjects.MokeProbeBean;
import jrds.mockobjects.MokeProbeFactory;
import jrds.starter.StarterNode;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestHostBuilder {
    static final private Logger logger = Logger.getLogger(TestProbeDescBuilder.class);

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        Tools.setLevel(logger, Level.TRACE, "jrds.RdsHost");
        Tools.setLevel(Level.INFO,"jrds.factories.xml.CompiledXPath");

        Tools.prepareXml();
    }

    @Test
    public void testFullConfigpath() throws Exception {
        PropertiesManager localpm = Tools.getEmptyProperties();
        JrdsDocument host = new JrdsDocument(Tools.dbuilder.newDocument());
        host.doRootElement("host", "name=name");
        ConfigObjectFactory conf = new ConfigObjectFactory(localpm, localpm.extensionClassLoader);
        conf.getNodeMap(ConfigType.HOSTS).put("name", host);
        Assert.assertNotNull("Probedesc not build", conf.setHostMap().get("name"));
    }

    @Test
    public void testNewProbe() throws InvocationTargetException, IllegalArgumentException, IllegalAccessException {
        PropertiesManager localpm = Tools.getEmptyProperties();

        HostBuilder hb = new HostBuilder();
        //Generate a probe with a bean hostInfo with a default value of ${host}
        hb.setProbeFactory(new MokeProbeFactory() {
            @Override
            public Probe<?, ?> makeProbe(String type) {
                logger.trace(type);
                ProbeDesc pd = generateProbeDesc(type);
                Probe<?, ?> p = new MokeProbeBean<String, Number>(pd);
                try {
                    PropertyDescriptor beanInfo = new PropertyDescriptor("hostInfo", p.getClass());
                    pd.getBeanMap().put(beanInfo.getName(), beanInfo);
                    pd.addDefaultArg("hostInfo", "${host}");
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                return p;
            }
        });
        hb.setPm(localpm);

        RdsHost host = new RdsHost();
        host.setName("localhost");
        host.setHostDir(testFolder.getRoot());

        JrdsDocument probeNode = new JrdsDocument(Tools.dbuilder.newDocument());
        probeNode.doRootElement("probe", "type=probetype");

        StarterNode ns = new StarterNode() {};

        Probe<?, ?> p = hb.makeProbe(probeNode.getRootElement(), host, ns);
        Assert.assertEquals("localhost", p.getPd().getBeanMap().get("hostInfo").getReadMethod().invoke(p));
        logger.trace(p.getName());

    }

}
