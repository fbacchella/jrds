package jrds.standalone;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import jrds.Probe;
import jrds.ProbeConnected;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.factories.ConfigObjectFactory;
import jrds.factories.Loader;

import org.apache.log4j.Logger;
import org.rrd4j.core.DsDef;

public class EnumerateWikiProbes extends CommandStarterImpl {
	static private final Logger logger = Logger.getLogger(EnumerateWikiProbes.class);
	
	static final private String JAVADOCURLTEMPLATES = "http://jrds.fr/apidoc-core/index.html?%s.html";

	String propFile = "jrds.properties";

	public void configure(Properties configuration) {
		propFile =  configuration.getProperty("propertiesFile", propFile);
	}
	
	private String classToLink(Class<?> c) {
		String className = c.getName();
		String classurlpath = className.replace('.', '/');
		String newurl = String.format(JAVADOCURLTEMPLATES, classurlpath);
		String classLine = String.format("[[%s|%s]]", newurl, className);
		return classLine;
	}

	public void start(String args[]) throws Exception {

		PropertiesManager pm = new PropertiesManager(new File(propFile));
		pm.update();
		jrds.JrdsLoggerConfiguration.configure(pm);

		System.getProperties().setProperty("java.awt.headless","true");

		Loader l;
		try {
			l = new Loader();
			URL graphUrl = getClass().getResource("/desc");
			if(graphUrl != null)
				l.importUrl(graphUrl);
			else {
				logger.fatal("Default probes not found");
			}
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Loader initialisation error",e);
		}

		logger.debug("Scanning " + pm.libspath + " for probes libraries");
		System.out.println(pm.libspath);
		for(URL lib: pm.libspath) {
			logger.info("Adding lib " + lib);
			l.importUrl(lib);
		}

		l.importDir(pm.configdir);

		logger.debug("Starting parsing descriptions");
		ConfigObjectFactory conf = new ConfigObjectFactory(pm, pm.extensionClassLoader);
		Map<String, ProbeDesc> probesMap = conf.setProbeDescMap(l.getRepository(Loader.ConfigType.PROBEDESC));
		if(args.length == 0) {
			dumpAll(probesMap.values());
		}
		else {
			ProbeDesc pd = probesMap.get(args[0]);
			if(pd != null)
				dumpProbe(pd);
			else {
				System.out.println("Unknwon probe");
			}
		}

	}

	/* (non-Javadoc)
	 * @see jrds.standalone.CommandStarterImpl#help()
	 */
	@Override
	public void help() {
		System.out.println("Dump all the probes in http://wiki.jrds.fr/probes format if not argument if given");
		System.out.println("If a probe name is provided, dump more details about it, style in wiki format");
	}

	private void dumpAll(Collection<ProbeDesc> probes) {
		for(ProbeDesc pd: probes) {
			try {
				Class<? extends Probe<?, ?>> c = pd.getProbeClass();
				Probe<?, ?> p = c.newInstance();
				p.setPd(pd);
				System.out.println(oneLine(p));
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	private String oneLine(Probe<?, ?> p) {
		ProbeDesc pd = p.getPd();
		String sourceType = p.getSourceType();
		String probeName = pd.getName();
		String description = pd.getSpecific("description");
		if (description == null)
			description = "";
		String link= "[[sourcetype:" + sourceType + ":" + probeName.toLowerCase() + "|" + probeName + "]]";
		return new String("| " + sourceType + " | " + link + " | " + description + " | " + classToLink(p.getClass()) + " | ");
		
	}
	private void dumpProbe(ProbeDesc pd) throws InstantiationException, IllegalAccessException {
		Class<? extends Probe<?, ?>> c = pd.getProbeClass();
		Probe<?,?> p = c.newInstance();
		p.setPd(pd);
		System.out.println(oneLine(p));

		System.out.println(doTitle(pd.getName()));
		System.out.println("");
		System.out.println(doTitle("Source type"));
		System.out.println("");
		System.out.println(p.getSourceType());
		System.out.println(doTitle("Probe class"));
		System.out.println("");
		System.out.println(classToLink(pd.getProbeClass()));
		System.out.println("");
		System.out.println(doTitle("Arguments"));
		System.out.println("");

		for(Method m: c.getMethods()) {
			if("configure".equals(m.getName())) {
				System.out.println("^ Type ^ Description ^");
				for(Class<?> arg: m.getParameterTypes()) {
					System.out.println("| " + arg.getSimpleName() + " | | ");
				}
				System.out.println();
			}
		}
		System.out.println(doTitle("Data stores"));
		System.out.println("");
		System.out.println("^ Name ^ Type ^ Description ^");
		for(DsDef ds: pd.getDsDefs()) {
			System.out.println(String.format("| %s | %s | |",ds.getDsName(), ds.getDsType()));
		}
		System.out.println(doTitle("Graph provided"));
		System.out.println("");
		System.out.println("^ Name ^ Description ^");
		for(String graphs: pd.getGraphClasses()) {
			System.out.println(String.format("| %s | |",graphs));
		}
		System.out.println("");		
		if(ProbeConnected.class.isAssignableFrom(c)) {
			System.out.println(doTitle("Connection class"));

			ParameterizedType t = (ParameterizedType) c.getGenericSuperclass();
			Class<?> typeArg = (Class<?>)t.getActualTypeArguments()[2];
			System.out.println(classToLink(typeArg));
			System.out.println("");
		}
		System.out.println("=====Example=====");
		System.out.println("");
		System.out.println("<code xml>");
		System.out.println("</code>");
	}
	
	private String doTitle(String title) {
		return String.format("=====%s=====", title);
	}
}
