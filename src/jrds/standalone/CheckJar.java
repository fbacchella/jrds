package jrds.standalone;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import jrds.GraphDesc;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.factories.ConfigObjectFactory;
import jrds.factories.Loader;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;

public class CheckJar extends CommandStarterImpl {
	static private final Logger logger = Logger.getLogger(CheckJar.class);

	public void start(String[] args) throws Exception {
		PropertiesManager pm = new PropertiesManager();
		pm.update();
		jrds.JrdsLoggerConfiguration.putAppender( new ConsoleAppender(new org.apache.log4j.SimpleLayout(), ConsoleAppender.SYSTEM_OUT));

		System.getProperties().setProperty("java.awt.headless","true");

		Loader l;
		try {
			l = new Loader();
			URL descUrl = getClass().getResource("/desc");
			if(descUrl != null)
				l.importUrl(descUrl);
			else {
				logger.fatal("Default probes not found");
			}
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Loader initialisation error",e);
		}

		logger.debug("Starting parsing descriptions");
		ConfigObjectFactory conf = new ConfigObjectFactory(pm, pm.extensionClassLoader);

		Map<String, GraphDesc> grapMap = conf.setGraphDescMap(l.getRepository(Loader.ConfigType.GRAPHDESC));

		for(String jarfile: args) {

			URL jarfileurl = new File(jarfile).toURI().toURL();
			ClassLoader cl = URLClassLoader.newInstance(new URL[]{jarfileurl}, getClass().getClassLoader());

			Loader jarload = new Loader(true);
			
			jarload.importUrl(jarfileurl);

			ConfigObjectFactory confjar = new ConfigObjectFactory(pm, cl);

			Map<String, GraphDesc> grapMapjar = confjar.setGraphDescMap(jarload.getRepository(Loader.ConfigType.GRAPHDESC));
			for(ProbeDesc pd: confjar.setProbeDescMap(jarload.getRepository(Loader.ConfigType.PROBEDESC)).values()) {
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
