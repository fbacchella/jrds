package jrds.standalone;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Map;

import jrds.GraphDesc;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.configuration.ConfigObjectFactory;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;

public class CheckJar extends CommandStarterImpl {
    static private final Logger logger = Logger.getLogger(CheckJar.class);

    public void start(String[] args) throws Exception {
        PropertiesManager pm = new PropertiesManager();
        pm.update();
        jrds.JrdsLoggerConfiguration.putAppender( new ConsoleAppender(new org.apache.log4j.SimpleLayout(), ConsoleAppender.SYSTEM_OUT));

        System.getProperties().setProperty("java.awt.headless","true");

        logger.debug("Starting parsing descriptions");
        ConfigObjectFactory conf = new ConfigObjectFactory(pm, pm.extensionClassLoader);

        Map<String, GraphDesc> grapMap = conf.setGraphDescMap();

        for(String jarfile: args) {

            URI jarfileurl = new File(jarfile).toURI();
            ClassLoader cl = URLClassLoader.newInstance(new URL[]{jarfileurl.toURL()}, getClass().getClassLoader());
            ConfigObjectFactory confjar = new ConfigObjectFactory(pm, cl);
            confjar.addUrl(jarfileurl);

            Map<String, GraphDesc> grapMapjar = confjar.setGraphDescMap();
            for(ProbeDesc pd: confjar.setProbeDescMap().values()) {
                for(String ds: pd.getDs()) {
                    if(ds.length() > 20) {
                        System.out.println(String.format("DS name %s too long for probe description %s", ds, pd.getName() ));
                    }
                }
                Collection<String> graphs = pd.getGraphClasses();
                if(graphs.size() == 0) {
                    System.out.println("no graphs for probe desc: " + pd.getName());
                    continue;
                }
                for(String graph: graphs) {
                    if(! grapMap.containsKey(graph) && ! grapMapjar.containsKey(graph)) {
                        System.out.println("Unknown graph " + graph + " for probe desc: " + pd.getName());
                        continue;
                    }
                }
            }
        }
    }
}
