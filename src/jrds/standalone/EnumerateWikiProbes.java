package jrds.standalone;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import jrds.Probe;
import jrds.ProbeConnected;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.configuration.ConfigObjectFactory;

import org.apache.log4j.Logger;
import org.rrd4j.core.DsDef;

public class EnumerateWikiProbes extends CommandStarterImpl {
    static private final Logger logger = Logger.getLogger(EnumerateWikiProbes.class);

    static final private String JAVADOCURLTEMPLATES = "http://jrds.fr/apidoc-core/index.html?%s.html";

    static final private Map<String, String> sourceTypeMapping = new HashMap<String, String>();

    static {
        sourceTypeMapping.put("JRDS Agent", "jrds_agent");
        sourceTypeMapping.put("SNMP", "SNMP");
        sourceTypeMapping.put("Munin", "munin");
        sourceTypeMapping.put("HttpXml", "httpxml");
        sourceTypeMapping.put("kstat", "kstat");
        sourceTypeMapping.put("JMX", "jmx");
    }
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

        logger.debug("Starting parsing descriptions");
        ConfigObjectFactory conf = new ConfigObjectFactory(pm, pm.extensionClassLoader);
        Map<String, ProbeDesc> probesMap = conf.setProbeDescMap();
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
    
    private String getSourceTypeLink(Probe<?, ?> p) {
        String sourceType = p.getSourceType();
        return "[[sourcetype:" + sourceTypeMapping.get(sourceType) + ":|" + sourceType + "]]";

    }

    private String oneLine(Probe<?, ?> p) {
        ProbeDesc pd = p.getPd();
        String sourceType = p.getSourceType();

        String probeName = pd.getName();
        String description = pd.getSpecific("description");
        if (description == null)
            description = "";
        String link= "[[sourcetype:" + sourceType + ":" + probeName.toLowerCase() + "|" + probeName + "]]";
        return "| " + getSourceTypeLink(p) + " | " + link + " | " + description + " | " + classToLink(p.getClass()) + " | ";

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
        System.out.println(getSourceTypeLink(p));
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

            Class<?> typeArg = null;
            Class<?> curs = c;
            while(! ParameterizedType.class.isAssignableFrom(curs.getGenericSuperclass().getClass()))
                curs = curs.getSuperclass();

            ParameterizedType t = (ParameterizedType) curs.getGenericSuperclass();
                typeArg = (Class<?>)t.getActualTypeArguments()[2];

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
