package jrds.configuration;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jrds.Probe;
import jrds.ProbeDesc;
import jrds.Util;
import jrds.factories.ArgFactory;
import jrds.factories.xml.JrdsDocument;
import jrds.factories.xml.JrdsElement;

import org.apache.log4j.Logger;

public class ProbeDescBuilder extends ConfigObjectBuilder<ProbeDesc> {
    static final private Logger logger = Logger.getLogger(ProbeDescBuilder.class);

    private ClassLoader classLoader = ProbeDescBuilder.class.getClassLoader();

    public ProbeDescBuilder() {
        super(ConfigType.PROBEDESC);
    }

    @Override
    ProbeDesc build(JrdsDocument n) throws InvocationTargetException {
        try {
            return makeProbeDesc(n);
        } catch (SecurityException e) {
            throw new InvocationTargetException(e, ProbeDescBuilder.class.getName());
        } catch (IllegalArgumentException e) {
            throw new InvocationTargetException(e, ProbeDescBuilder.class.getName());
        } catch (NoSuchMethodException e) {
            throw new InvocationTargetException(e, ProbeDescBuilder.class.getName());
        } catch (IllegalAccessException e) {
            throw new InvocationTargetException(e, ProbeDescBuilder.class.getName());
        } catch (InvocationTargetException e) {
            throw new InvocationTargetException(e, ProbeDescBuilder.class.getName());
        } catch (ClassNotFoundException e) {
            throw new InvocationTargetException(e, ProbeDescBuilder.class.getName());
        } catch (NoClassDefFoundError e) {
            throw new InvocationTargetException(e, ProbeDescBuilder.class.getName());
        } catch (InstantiationException e) {
            throw new InvocationTargetException(e, ProbeDescBuilder.class.getName());
        }
    }

    @SuppressWarnings("unchecked")
    public ProbeDesc makeProbeDesc(JrdsDocument n) throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, InstantiationException {
        ProbeDesc pd = new ProbeDesc();

        JrdsElement root = n.getRootElement();
        setMethod(root.getElementbyName("probeName"), pd, "setProbeName");
        setMethod(root.getElementbyName("name"), pd, "setName");
        setMethod(root.getElementbyName("index"), pd, "setIndex");
        
        logger.trace(Util.delayedFormatString("Creating probe description %s", pd.getName()));

        JrdsElement classElem = root.getElementbyName("probeClass");
        if(classElem == null) {
            throw new RuntimeException("Probe " + pd.getProbeName() + "defined without class");
        }
        String className = classElem.getTextContent().trim();
        Class<? extends Probe<?,?>> c = (Class<? extends Probe<?,?>>) classLoader.loadClass(className);
        pd.setProbeClass(c);

        pd.setHeartBeatDefault(pm.step * 2);

        setMethod(root.getElementbyName("uptimefactor"), pd, "setUptimefactor", Float.TYPE);
        
        JrdsElement graphsElement= root.getElementbyName("graphs");
        if(graphsElement != null) {
            List<String> graphs =  new ArrayList<String>();
            for(JrdsElement e: graphsElement.getChildElementsByName("name")) {
                graphs.add(e.getTextContent());
                logger.trace(Util.delayedFormatString("Adding graph: %s", e.getTextContent()));
            }
            pd.setGraphClasses(graphs);
        }

        for(JrdsElement specificNode: root.getChildElementsByName("specific")) {
            Map<String, String> m = specificNode.attrMap();
            if(m != null) {
                String name = m.get("name");
                String value = specificNode.getTextContent().trim();
                pd.addSpecific(name, value);
                logger.trace(Util.delayedFormatString("Specific added: %s='%s'", name, value));
            }
        }

        JrdsElement requesterElement = root.getElementbyName("snmpRequester");
        if(requesterElement != null) {
            String snmpRequester = requesterElement.getTextContent().trim();
            if(snmpRequester != null && ! "".equals(snmpRequester)) {
                pd.addSpecific("requester", snmpRequester);
                logger.trace(Util.delayedFormatString("Specific added: requester='%s'", snmpRequester));

            }
        }

        //Populating default argument vector
        JrdsElement argsNode = root.getElementbyName("defaultargs");
        if(argsNode != null)
            for(Object o:  ArgFactory.makeArgs(argsNode)) {
                pd.addDefaultArg(o);
            }

        for(Map<String, Object> dsMap: doDsList(pd.getName(), root)) {
            pd.add(dsMap);			
        }

        Map<String, String> props = makeProperties(root);
        if(props != null)
            pd.setProperties(props);

        return pd;
    }

    /**
     * @param classLoader the classLoader to set
     */
    void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

}
