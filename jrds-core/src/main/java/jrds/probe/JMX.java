package jrds.probe;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.MalformedObjectNameException;

import org.slf4j.event.Level;

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
        timerStarter=JmxSocketFactory.class
        )
public class JMX extends ProbeConnected<String, Double, JMXConnection> implements ConnectedProbe, SSLProbe {
    private Map<String, String> collectKeys = null;

    public JMX() {
        super(JMXConnection.class.getName());
    }

    @Override
    public Boolean configure() {
        collectKeys = new HashMap<>();
        for(Map.Entry<String, String> e: getPd().getCollectMapping().entrySet()) {
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
            Map<String, Double> retValues = new HashMap<>(collectKeys.size());

            log(Level.DEBUG, "will collect: %s", collectKeys);
            for(String collect: collectKeys) {
                Number v = mbean.collect(this, collect);
                log(Level.TRACE, "JMX Path: %s = %s", collect, v);
                if (v != null) {
                    retValues.put(collect, v.doubleValue());
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
    public void setPd(ProbeDesc<String> pd) {
        super.setPd(pd);
        collectKeys = getPd().getCollectMapping();
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
