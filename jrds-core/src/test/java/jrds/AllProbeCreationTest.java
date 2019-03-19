package jrds;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import jrds.configuration.ConfigObjectFactory;
import jrds.factories.ProbeFactory;
import jrds.starter.HostStarter;

public class AllProbeCreationTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Rule
    public Log4JRule logrule = new Log4JRule(this);
    private final Logger logger = logrule.getTestlogger();

    @BeforeClass
    static public void configure() throws IOException {
        Tools.configure();
    }
    
    @Before
    public void loggers() {
        logrule.setLevel(Level.TRACE, "jrds.Util");
    }

    @Test
    public void makeProbe() throws ParserConfigurationException, IOException, URISyntaxException, InvocationTargetException {
        PropertiesManager pm = Tools.makePm(testFolder);

        File descpath = new File("desc");
        if(descpath.exists())
            pm.libspath.add(descpath.toURI());

        Assert.assertTrue(pm.rrddir.isDirectory());

        ConfigObjectFactory conf = new ConfigObjectFactory(pm);

        Map<String, GraphDesc> graphDescMap = conf.setGraphDescMap();
        Map<String, ProbeDesc<?>> probeDescMap = conf.setProbeDescMap();

        ProbeFactory pf = new ProbeFactory(probeDescMap, graphDescMap);

        Set<GraphNode> allgraphs = new HashSet<GraphNode>();
        HostInfo host = new HostInfo("Empty");
        host.setHostDir(pm.rrddir);
        HostStarter hs = new HostStarter(host);
        for(ProbeDesc<?> pd: probeDescMap.values()) {
            logger.trace("Will create probedesc " + pd.getName());
            Probe<?, ?> p = pf.makeProbe(pd.getName());
            p.setHost(hs);
            for(GenericBean bean: pd.getBeans()) {
                try {
                    if("index".equals(bean.getName())) {
                        bean.set(p, "index");
                    }
                } catch (IllegalArgumentException e) {
                    logger.error("bean read error for '" + bean.getName() + "': " + e.getMessage());
                }
            }
            p.configureStarters(pm);
            for(String graphName: p.getPd().getGraphs()) {
                try {
                    GraphDesc gd = graphDescMap.get(graphName);
                    p.addGraph(gd);
                    GraphNode gn = new GraphNode(p, gd);
                    allgraphs.add(gn);
                } catch (Exception e) {
                }
            }
        }
    }

}
