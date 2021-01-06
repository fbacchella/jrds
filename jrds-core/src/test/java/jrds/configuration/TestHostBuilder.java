package jrds.configuration;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import jrds.GraphDesc;
import jrds.HostInfo;
import jrds.Log4JRule;
import jrds.Macro;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.Tools;
import jrds.Util;
import jrds.factories.ProbeFactory;
import jrds.factories.xml.JrdsDocument;
import jrds.mockobjects.MokeProbeBean;
import jrds.mockobjects.MokeProbeFactory;
import jrds.probe.JMXConnection;
import jrds.starter.ConnectionInfo;
import jrds.starter.StarterNode;
import jrds.starter.Timer;

public class TestHostBuilder {

    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public final Log4JRule logrule = new Log4JRule(this);
    private final Logger logger = logrule.getTestlogger();

    @BeforeClass
    static public void configure() throws ParserConfigurationException, IOException {
        Tools.configure();
        Tools.prepareXml(false);
    }

    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.RdsHost", "jrds.starter", "jrds.Starter", "jrds.configuration.HostBuilder");
        logrule.setLevel(Level.INFO, "jrds.factories.xml.CompiledXPath");
    }

    @Test
    public void testFullConfigpath() throws Exception {
        PropertiesManager localpm = Tools.makePm(testFolder);
        JrdsDocument host = new JrdsDocument(Tools.dbuilder.newDocument());
        host.doRootElement("host", "name=name");
        ConfigObjectFactory conf = new ConfigObjectFactory(localpm, localpm.extensionClassLoader);
        conf.getNodeMap(ConfigType.HOSTS).put("name", host);
        Assert.assertNotNull("Probedesc not build", conf.setHostMap(Tools.getSimpleTimerMap()).get("name"));
    }

    @Test
    public void testNewProbe() throws InvocationTargetException, IllegalArgumentException, IllegalAccessException, IOException {
        PropertiesManager localpm = Tools.makePm(testFolder);

        HostBuilder hb = new HostBuilder();
        // Generate a probe with a bean hostInfo with a default value of ${host}
        hb.setProbeFactory(new MokeProbeFactory() {
            @Override
            public Probe<?, ?> makeProbe(String type) {
                logger.trace(type);
                ProbeDesc<String> pd = generateProbeDesc(type);
                try {
                    pd.setProbeClass(MokeProbeBean.class);
                } catch (InvocationTargetException e1) {
                    throw new RuntimeException(e1);
                }
                Probe<?, ?> p = new MokeProbeBean(pd);
                try {
                    pd.addDefaultBean("hostInfo", "${host}", false);
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
        Assert.assertEquals("localhost", p.getPd().getBean("hostInfo").get(p));
        logger.trace(p.getName());

    }

    @Test
    public void testConnectionInfo() throws Exception {
        PropertiesManager pm = Tools.makePm(testFolder);

        HostBuilder hb = new HostBuilder();
        hb.setPm(pm);
        hb.setClassLoader(this.getClass().getClassLoader());

        JrdsDocument cnxdoc = new JrdsDocument(Tools.dbuilder.newDocument());
        cnxdoc.doRootElement("host").addElement("connection", "type=jrds.probe.JMXConnection").addElement("attr", "name=port").setTextContent("8999");
        for(ConnectionInfo ci: hb.makeConnexion(cnxdoc.getRootElement(), new HostInfo("localhost"), new HashMap<String, String>(0))) {
            logger.trace(ci.getName());
            StarterNode  sn = new StarterNode() {
                @Override
                public Logger getInstanceLogger() {
                    return logrule.getTestlogger();
                }
            };
            ci.register(sn);
            JMXConnection cnx = sn.find(JMXConnection.class);
            Assert.assertEquals("Attributed not setted", new Integer(8999), cnx.getPort());
        }
    }

    @Test
    public void testAttributesParsing() throws Exception {
        PropertiesManager pm = Tools.makePm(testFolder);
        File descpath = new File("desc");
        if(descpath.exists())
            pm.libspath.add(descpath.toURI());

        ConfigObjectFactory conf = new ConfigObjectFactory(pm);
        JrdsDocument pddoc = Tools.parseRessource("beans.xml");
        conf.getNodeMap(ConfigType.PROBEDESC).put("name", pddoc);

        Map<String, GraphDesc> graphDescMap = conf.setGraphDescMap();
        Map<String, ProbeDesc<? extends Object>> probeDescMap = conf.setProbeDescMap();
        ProbeFactory pf = new ProbeFactory(probeDescMap, graphDescMap);

        HostBuilder hb = new HostBuilder();
        hb.setPm(pm);
        hb.setClassLoader(this.getClass().getClassLoader());
        hb.setMacros(new HashMap<String, Macro>(0));
        hb.setProbeFactory(pf);
        Map<String, Timer> timerMap = Tools.getSimpleTimerMap();
        timerMap.put("another", timerMap.get(Timer.DEFAULTNAME));
        hb.setTimers(timerMap);

        JrdsDocument fullhost = Tools.parseRessource("attrhost.xml");

        HostInfo hi = hb.build(fullhost);

        Probe<?, ?> p = hi.getProbes().iterator().next();
        Assert.assertEquals("${attr.customattr1} failed", "defaultattr1", Util.parseTemplate("${attr.customattr1}", p));
        Assert.assertEquals("${attr.customattr3} failed", "defaultattr2", Util.parseTemplate("${attr.customattr2}", p));
        Assert.assertEquals("${attr.customattr3} failed", "defaultattr1", Util.parseTemplate("${attr.customattr3}", p));
        Assert.assertEquals("${attr.customattr4} failed", "value4", Util.parseTemplate("${attr.customattr4}", p));
    }

}
