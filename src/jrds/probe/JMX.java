package jrds.probe;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.log4j.Level;

import jrds.ConnectedProbe;
import jrds.ProbeConnected;
import jrds.ProbeDesc;
import jrds.factories.ProbeMeta;
import jrds.probe.jmx.JmxAbstractDataSource;
import jrds.probe.jmx.JmxDiscoverAgent;

/**
 * 
 * @author Fabrice Bacchella
 */
@ProbeMeta(
        discoverAgent=JmxDiscoverAgent.class,
        topStarter=JmxSocketFactory.class
        )
public class JMX extends ProbeConnected<String, Double, JMXConnection> implements ConnectedProbe, SSLProbe {
    private Map<String, String> collectKeys = null;

    public JMX() {
        super(JMXConnection.class.getName());
    }

    @Override
    public Boolean configure() {
        collectKeys = new HashMap<String, String>();
        for(Map.Entry<String, String> e: getPd().getCollectStrings().entrySet()) {
            String dsName = e.getValue();
            String solved = jrds.Util.parseTemplate(e.getKey(), this);
            collectKeys.put(solved, dsName);
        }
        return super.configure();
    }

    @Override
    public Map<String, Double> getNewSampleValuesConnected(JMXConnection cnx) {
        JmxAbstractDataSource<?> mbean = cnx.getConnection();
        try {
            Set<String> collectKeys = getCollectMapping().keySet();
            Map<String, Double> retValues = new HashMap<String, Double>(collectKeys.size());

            log(Level.DEBUG, "will collect: %s", collectKeys);
            for(String collect: collectKeys) {
                int attrSplit = collect.indexOf(':');
                attrSplit = collect.indexOf('/', attrSplit);
                ObjectName mbeanName = new ObjectName(collect.substring(0, attrSplit));
                String[] jmxPath = collect.substring(attrSplit + 1).split("/");
                String attributeName = jmxPath[0];
                log(Level.TRACE, "mbean name = %s, attributeName = %s", mbeanName, attributeName);
                try {
                    Number v = mbean.getValue(mbeanName, attributeName, jmxPath);
                    log(Level.TRACE, "JMX Path: %s = %s", collect, v);
                    if (v != null) {
                        retValues.put(collect, v.doubleValue());
                    }
                } catch (InvocationTargetException e) {
                    if (e.getCause() != null) {
                        try {
                            throw e.getCause();
                        } catch (RemoteException e1) {
                            log(Level.ERROR, e1, "JMX remote exception: %s", e1.getMessage());
                        } catch (AttributeNotFoundException e1) {
                            log(Level.ERROR, e1, "Invalide JMX attribue %s", attributeName);
                        } catch (InstanceNotFoundException e1) {
                            Level l = Level.ERROR;
                            if(isOptional(collect)) {
                                l = Level.DEBUG;
                            }
                            log(l, "JMX instance not found: %s", e1.getMessage());
                        } catch (MBeanException e1) {
                            log(Level.ERROR, e1, "JMX MBeanException: %s", e1.getMessage());
                        } catch (ReflectionException e1) {
                            log(Level.ERROR, e1, "JMX reflection error: %s", e1.getMessage());
                        } catch (IOException e1) {
                            log(Level.ERROR, e1, "JMX IO error: %s", e1.getMessage());
                        } catch (Throwable e1) {
                        }
                    }
                }
            }
            return retValues;
        } catch (MalformedObjectNameException e) {
            log(Level.ERROR, e, "JMX name error: %s", e);
        } catch (NullPointerException e) {
            log(Level.ERROR, e, "JMX error: %s", e);
        }

        return null;
    }

    @Override
    public String getSourceType() {
        return "JMX";
    }


    /*
     * (non-Javadoc)
     * 
     * @see jrds.Probe#setPd(jrds.ProbeDesc)
     */
    @Override
    public void setPd(ProbeDesc pd) {
        super.setPd(pd);
        collectKeys = getPd().getCollectStrings();
    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.Probe#getCollectkeys()
     */
    @Override
    public Map<String, String> getCollectMapping() {
        return collectKeys;
    }
}
