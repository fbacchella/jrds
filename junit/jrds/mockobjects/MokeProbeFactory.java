package jrds.mockobjects;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rrd4j.DsType;

import jrds.GraphDesc;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.PropertiesManager;
import jrds.factories.ProbeFactory;

public class MokeProbeFactory extends ProbeFactory {
    static Map<String, ProbeDesc> probeDescMap = new HashMap<String, ProbeDesc>();
    static Map<String, GraphDesc> graphDescMap = Collections.emptyMap();
    static PropertiesManager  pm = new PropertiesManager();
    static boolean legacymode = false;

    public MokeProbeFactory() {
        super(probeDescMap, graphDescMap);
    }

    /* (non-Javadoc)
     * @see jrds.factories.ProbeFactory#makeProbe(jrds.ProbeDesc, java.util.List)
     */
    public Probe<?,?> makeProbe(ProbeDesc pd) {
        return new MokeProbe<String, Number>(pd);
    }
    
    public Probe<?,?> makeProbe(String type) {
        ProbeDesc pd = generateProbeDesc(type);
        return new MokeProbe<String, Number>(pd);
    }
    
    protected ProbeDesc generateProbeDesc(String type) {
        ProbeDesc pd = new ProbeDesc();
        pd = new ProbeDesc();
        pd.setName(type);
        pd.setProbeName("dummyprobe");
        Map<String, Object> dsMap = new HashMap<String, Object>();
        dsMap.put("dsName", "ds0");
        dsMap.put("dsType", DsType.COUNTER);
        dsMap.put("collectKey", "/jrdsstats/stat[@key='a']/@value");
        pd.add(dsMap);
        dsMap = new HashMap<String, Object>();
        dsMap.put("dsName", "ds1");
        dsMap.put("dsType", DsType.COUNTER);
        dsMap.put("collectKey", "/jrdsstats/stat[@key='b']/@value");
        pd.add(dsMap);
        dsMap = new HashMap<String, Object>();
        dsMap.put("dsName", "ds2");
        dsMap.put("dsType", DsType.COUNTER);
        pd.add(dsMap);
        return pd;
    }

    /* (non-Javadoc)
     * @see jrds.factories.ProbeFactory#configure(jrds.Probe, java.util.List)
     */
    @Override
    public boolean configure(Probe<?, ?> p, List<?> constArgs) {
        if(p instanceof MokeProbe) {
            MokeProbe<?,?> mp = (MokeProbe<?,?>)p;
            mp.configure();
            mp.setArgs(constArgs);
        }
        return true;
    }

    /* (non-Javadoc)
     * @see jrds.factories.ProbeFactory#getProbeDesc(java.lang.String)
     */
    @Override
    public ProbeDesc getProbeDesc(String name) {
        if(!probeDescMap.containsKey(name) ) {
            MokeProbe <String, Number> mp = new MokeProbe<String, Number>(name);
            mp.configure();
            probeDescMap.put(name, mp.getPd());
        }
        return super.getProbeDesc(name);
    }
}
