package jrds.standalone;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.rrd4j.core.DsDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jrds.GenericBean;
import jrds.Probe;
import jrds.ProbeConnected;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.configuration.ConfigObjectFactory;
import jrds.probe.PassiveProbe;

public class EnumerateWikiProbes extends CommandStarterImpl {
    static private final Logger logger = LoggerFactory.getLogger(EnumerateWikiProbes.class);

    static final private String JAVADOCURLTEMPLATES = "http://jrds.fr/apidoc-core/index.html?%s.html";

    String propFileName = "jrds.properties";

    public void configure(Properties configuration) {
        propFileName = configuration.getProperty("propertiesFile", propFileName);
    }

    private String classToLink(Class<?> c) {
        String className = c.getName();
        String classurlpath = className.replace('.', '/');
        String newurl = String.format(JAVADOCURLTEMPLATES, classurlpath);
        return String.format("[[%s|%s]]", newurl, className);
    }

    public void start(String[] args) throws Exception {
        PropertiesManager pm = new PropertiesManager();
        File propFile = new File(propFileName);
        if(propFile.isFile()) {
            pm.join(propFile);
        }
        pm.importSystemProps();
        try {
            pm.update();
        } catch (IllegalArgumentException e) {
            System.err.println("invalid configuration, can't start: " + e.getMessage());
            System.exit(1);
        }

        System.getProperties().setProperty("java.awt.headless", "true");

        logger.debug("Starting parsing descriptions");
        ConfigObjectFactory conf = new ConfigObjectFactory(pm, pm.extensionClassLoader);
        // Needed for the probe's graph list
        conf.setGraphDescMap();
        Map<String, ProbeDesc<?>> probesMap = conf.setProbeDescMap();
        if(args.length == 0) {
            dumpAll(probesMap.values());
        } else {
            @SuppressWarnings("unchecked")
            ProbeDesc<Object> pd = (ProbeDesc<Object>) probesMap.get(args[0]);
            if(pd != null)
                dumpProbe(pd);
            else {
                System.out.println("Unknwon probe");
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.standalone.CommandStarterImpl#help()
     */
    @Override
    public void help() {
        System.out.println("Dump all the probes in http://wiki.jrds.fr/probes format if not argument if given");
        System.out.println("If a probe name is provided, dump more details about it, style in wiki format");
    }

    private void dumpAll(Collection<ProbeDesc<?>> probes) {
        for(ProbeDesc<?> pd: probes) {
            @SuppressWarnings("unchecked")
            ProbeDesc<Object> castedpd = (ProbeDesc<Object>) pd;
            try {
                Class<? extends Probe<Object, ?>> c = (Class<? extends Probe<Object, ?>>) castedpd.getProbeClass();
                //Don't dump passive probes, nothing to show
                if (PassiveProbe.class.isAssignableFrom(c)) {
                    continue;
                }
                Probe<Object, ?> p = c.newInstance();
                p.setPd(castedpd);
                System.out.println(oneLine(p));
            } catch (InstantiationException  | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private String getSourceTypeLink(Probe<?, ?> p, boolean withProbe) {
        String sourceType = p.getSourceType();
        String probePath = "";
        if(withProbe) {
            probePath = p.getPd().getName().toLowerCase();
        }
        return String.format("[[sourcetype:%s:%s|%s]]", sourceType, probePath, p.getPd().getName());
    }

    private String oneLine(Probe<?, ?> p) {
        ProbeDesc<?> pd = p.getPd();

        String description = pd.getSpecific("description");
        if(description == null)
            description = "";
        return "| " + getSourceTypeLink(p, true) + " | " + description + " | " + classToLink(p.getClass()) + " | ";
    }

    private void dumpProbe(ProbeDesc<Object> pd) throws InstantiationException, IllegalAccessException {
        Class<? extends Probe<Object, ?>> c = (Class<? extends Probe<Object, ?>>) pd.getProbeClass();
        Probe<Object, ?> p = c.newInstance();
        p.setPd(pd);
        System.out.println(oneLine(p));

        System.out.println(doTitle(pd.getName()));
        System.out.println();
        System.out.println(doTitle("Source type"));
        System.out.println();
        System.out.println(getSourceTypeLink(p, false));
        System.out.println();
        System.out.println(doTitle("Probe class"));
        System.out.println();
        System.out.println(classToLink(pd.getProbeClass()));
        System.out.println();
        System.out.println(doTitle("Arguments"));
        System.out.println();

        for(Method m: c.getMethods()) {
            if("configure".equals(m.getName())) {
                System.out.println("^ Type ^ Description ^");
                for(Class<?> arg: m.getParameterTypes()) {
                    System.out.println("| " + arg.getSimpleName() + " | | ");
                }
                System.out.println();
            }
        }

        // Enumerates the beans informations
        List<GenericBean> tryBeans = new ArrayList<>();
        for(GenericBean bean: pd.getBeans()) {
            tryBeans.add(bean);
        }
        Map<String, ProbeDesc.DefaultBean> defaultBeans = pd.getDefaultBeans();
        if(!tryBeans.isEmpty()) {
            System.out.println(doTitle("Attributes"));
            System.out.println();
            System.out.println("^ Name ^ Default value ^ Description ^");
            for(GenericBean bean: tryBeans) {
                String defaultValue = "";
                ProbeDesc.DefaultBean currentAttribute = defaultBeans.get(bean.getName());
                if(currentAttribute != null && !currentAttribute.delayed) {
                    defaultValue = currentAttribute.value;
                }
                if(bean != null)
                    System.out.println("| " + bean.getName() + " | " + defaultValue + " | | ");
            }
        }
        System.out.println();

        System.out.println(doTitle("Data stores"));
        System.out.println();
        System.out.println("^ Name ^ Type ^ Description ^");
        for(DsDef ds: pd.getDsDefs(1)) {
            System.out.println(String.format("| %s | %s | |", ds.getDsName(), ds.getDsType()));
        }
        System.out.println();
        System.out.println(doTitle("Graph provided"));
        System.out.println();
        System.out.println("^ Name ^ Description ^");
        for(String graphs: pd.getGraphs()) {
            System.out.println(String.format("| %s | |", graphs));
        }
        System.out.println();
        if(ProbeConnected.class.isAssignableFrom(c)) {
            System.out.println(doTitle("Connection class"));

            Class<?> typeArg;
            Class<?> curs = c;
            while (!ParameterizedType.class.isAssignableFrom(curs.getGenericSuperclass().getClass()))
                curs = curs.getSuperclass();

            ParameterizedType t = (ParameterizedType) curs.getGenericSuperclass();
            typeArg = (Class<?>) t.getActualTypeArguments()[2];

            System.out.println(classToLink(typeArg));
            System.out.println("");
        }
        System.out.println("=====Example=====");
        System.out.println();
        System.out.println("<code xml>");
        System.out.println("</code>");
    }

    private String doTitle(String title) {
        return String.format("=====%s=====", title);
    }

    @Override
    public String getName() {
        return "wikidoc";
    }

}
