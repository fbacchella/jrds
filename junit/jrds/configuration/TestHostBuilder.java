package jrds.configuration;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.xml.parsers.ParserConfigurationException;

import jrds.HostInfo;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.factories.xml.JrdsDocument;
import jrds.mockobjects.MokeProbeBean;
import jrds.mockobjects.MokeProbeFactory;
import jrds.probe.JMXConnection;
import jrds.starter.ConnectionInfo;
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
        Tools.setLevel(logger, Level.TRACE, "jrds.RdsHost", "jrds.starter", "jrds.Starter");
        Tools.setLevel(Level.INFO,"jrds.factories.xml.CompiledXPath");

        Tools.prepareXml();
    }

    @Test
    public void testFullConfigpath() throws Exception {
        PropertiesManager localpm = Tools.makePm();
        JrdsDocument host = new JrdsDocument(Tools.dbuilder.newDocument());
        host.doRootElement("host", "name=name");
        ConfigObjectFactory conf = new ConfigObjectFactory(localpm, localpm.extensionClassLoader);
        conf.getNodeMap(ConfigType.HOSTS).put("name", host);
        Assert.assertNotNull("Probedesc not build", conf.setHostMap(Tools.getSimpleTimerMap()).get("name"));
    }

    @Test
    public void testNewProbe() throws InvocationTargetException, IllegalArgumentException, IllegalAccessException, IOException {
        PropertiesManager localpm = Tools.makePm();

        HostBuilder hb = new HostBuilder();
        //Generate a probe with a bean hostInfo with a default value of ${host}
        hb.setProbeFactory(new MokeProbeFactory() {
            @Override
            public Probe<?, ?> makeProbe(String type) {
                logger.trace(type);
                ProbeDesc pd = generateProbeDesc(type);
                try {
                    pd.setProbeClass(MokeProbeBean.class);
                } catch (InvocationTargetException e1) {
                    throw new RuntimeException(e1);
                }
                Probe<?, ?> p = new MokeProbeBean(pd);
                try {
                    pd.addDefaultArg("hostInfo", "${host}");
                } catch (Exception e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
                return p;
            }
        });
        hb.setPm(localpm);
        hb.setTimers(Tools.getSimpleTimerMap());

        HostInfo host = new HostInfo("localhost");
        host.setHostDir(testFolder.getRoot());

        JrdsDocument probeNode = new JrdsDocument(Tools.dbuilder.newDocument());
        probeNode.doRootElement("probe", "type=probetype");

        Probe<?, ?> p = hb.makeProbe(probeNode.getRootElement(), host, null);
        Assert.assertEquals("localhost", p.getPd().getBeanMap().get("hostInfo").getReadMethod().invoke(p));
        logger.trace(p.getName());

    }

    @Test
    public void testConnectionInfo() throws Exception {
        PropertiesManager pm = Tools.makePm();

        HostBuilder hb = new HostBuilder();
        hb.setPm(pm);
        hb.setClassLoader(this.getClass().getClassLoader());

        JrdsDocument cnxdoc = new JrdsDocument(Tools.dbuilder.newDocument());
        cnxdoc.doRootElement("host").addElement("connection", "type=jrds.probe.JMXConnection").addElement("attr", "name=port").setTextContent("8999");
        for(ConnectionInfo ci: hb.makeConnexion(cnxdoc.getRootElement(), new HostInfo("localhost"))) {
            logger.trace(ci.getName());
            StarterNode  sn = new StarterNode() {};
            ci.register(sn);
            JMXConnection cnx = sn.find(JMXConnection.class);
            Assert.assertEquals("Attributed not setted", new Integer(8999), cnx.getPort());
        }
    }

}
