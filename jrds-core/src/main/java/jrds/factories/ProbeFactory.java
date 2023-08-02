package jrds.factories;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jrds.GraphDesc;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.Util;

/**
 * A class to find probe by their names
 * 
 * @author Fabrice Bacchella
 */
public class ProbeFactory {

    private final Logger logger = LoggerFactory.getLogger(ProbeFactory.class);
    private Map<String, ProbeDesc<?>> probeDescMap;
    private Map<String, GraphDesc> graphDescMap;

    /**
     * @param probeDescMap
     * @param graphDescMap
     */
    public ProbeFactory(Map<String, ProbeDesc<?>> probeDescMap, Map<String, GraphDesc> graphDescMap) {
        this.probeDescMap = probeDescMap;
        this.graphDescMap = graphDescMap;
    }

    /**
     * Create an probe, provided the probe name. It will be found in the probe
     * description map already provided
     * 
     * @param probeName the probe name
     * @return A probe
     */
    public Probe<?, ?> makeProbe(String probeName) {
        ProbeDesc<?> pd = probeDescMap.get(probeName);
        if(pd == null) {
            logger.error("Probe named " + probeName + " not found");
            return null;
        }
        return makeProbe(pd);
    }

    /**
     * Create an probe, provided a probe description
     * 
     * @param pd a probe description
     * @return A probe
     */
    public <KeyType, ValueType> Probe<KeyType, ValueType> makeProbe(ProbeDesc<KeyType> pd) {
        @SuppressWarnings("unchecked")
        Class<? extends Probe<KeyType, ValueType>> probeClass = (Class<? extends Probe<KeyType, ValueType>>) pd.getProbeClass();
        if(probeClass == null) {
            logger.error("Invalid probe description " + pd.getName() + ", probe class name not found");
            return null;
        }
        Probe<KeyType, ValueType> retValue;
        try {
            Constructor<? extends Probe<KeyType, ValueType>> c = probeClass.getConstructor();
            retValue = c.newInstance();
        } catch (LinkageError ex) {
            logger.warn("Error creating probe's " + pd.getName() + ": " + ex);
            return null;
        } catch (ClassCastException ex) {
            logger.warn("Error during probe instantiation: " + ex.getMessage());
            return null;
        } catch (Exception ex) {
            Throwable showException = ex;
            Throwable t = ex.getCause();
            if(t != null)
                showException = t;
            logger.warn("Error during probe instantation of type " + pd.getName() + ": ", showException);
            return null;
        }
        retValue.setPd(pd);
        return retValue;
    }

    public boolean configure(Probe<?, ?> p, List<?> constArgs) {
        Class<?>[] constArgsType = new Class[constArgs.size()];
        Object[] constArgsVal = new Object[constArgs.size()];
        int index = 0;
        for(Object arg: constArgs) {
            constArgsType[index] = arg.getClass();
            if(arg instanceof List<?>) {
                constArgsType[index] = List.class;
            }
            constArgsVal[index] = arg;
            index++;
        }
        try {
            Method configurator = p.getClass().getMethod("configure", constArgsType);
            Object result = configurator.invoke(p, constArgsVal);
            if(result != null && result instanceof Boolean) {
                if(logger.isTraceEnabled())
                    logger.trace("Result of configuration for " + p + ": " + result);
                Boolean configured = (Boolean) result;
                if(!configured) {
                    return false;
                }
            }
            String name = p.getName();
            if(name == null)
                name = jrds.Util.parseTemplate(p.getPd().getProbeName(), p);
            p.setName(name);
            for(String graphName: p.getPd().getGraphs()) {
                GraphDesc gd = graphDescMap.get(graphName);
                if(gd != null) {
                    p.addGraph(gd);
                } else {
                    logger.warn("Unknown graph {} for probe {}", graphName, p);
                }
            }
            return true;
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
            logger.warn("Probe arguments not matching configurators for {}: {}", p.getPd().getName(), e.getMessage());
            return false;
        } catch (Exception ex) {
            logger.warn("Error during probe creation of type {} with args {}: {}", p.getPd().getName(), constArgs, Util.resolveThrowableException(ex));
            if(logger.isDebugEnabled()) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                ex.getCause().printStackTrace(new PrintStream(buffer));
                logger.debug("{}", buffer);
            }
            return false;
        }
        return false;
    }

    public ProbeDesc<?> getProbeDesc(String name) {
        return probeDescMap.get(name);
    }

}
