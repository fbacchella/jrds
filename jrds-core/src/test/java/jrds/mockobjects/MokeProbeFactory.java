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
    static Map<String, ProbeDesc<?>> probeDescMap = new HashMap<>();
    static Map<String, GraphDesc> graphDescMap = Collections.emptyMap();
    static PropertiesManager pm = new PropertiesManager();
    static boolean legacymode = false;

    public MokeProbeFactory() {
        super(probeDescMap, graphDescMap);
    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.factories.ProbeFactory#makeProbe(jrds.ProbeDesc,
     * java.util.List)
     */
    @Override
    public <KeyType, ValueType> Probe<KeyType, ValueType> makeProbe(ProbeDesc<KeyType> pd) {
        return (Probe<KeyType, ValueType>) new MokeProbe<KeyType, ValueType>(pd);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Probe<?, ?> makeProbe(String type) {
        ProbeDesc<?> pd = generateProbeDesc(type);
        return new MokeProbe(pd);
    }

    protected <KeyType> ProbeDesc<KeyType> generateProbeDesc(String type) {
        ProbeDesc<KeyType> pd = new ProbeDesc<>();
        pd.setName(type);
        pd.setProbeName("dummyprobe");
        pd.add(ProbeDesc.getDataSourceBuilder("ds0", DsType.COUNTER).setCollectKey("/jrdsstats/stat[@key='a']/@value"));
        pd.add(ProbeDesc.getDataSourceBuilder("ds1", DsType.COUNTER).setCollectKey("/jrdsstats/stat[@key='b']/@value"));
        pd.add(ProbeDesc.getDataSourceBuilder("ds2", DsType.COUNTER));
        return pd;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.factories.ProbeFactory#configure(jrds.Probe, java.util.List)
     */
    @Override
    public boolean configure(Probe<?, ?> p, List<?> constArgs) {
        if(p instanceof MokeProbe) {
            MokeProbe<?, ?> mp = (MokeProbe<?, ?>) p;
            mp.configure();
            mp.setArgs(constArgs);
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jrds.factories.ProbeFactory#getProbeDesc(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public ProbeDesc<String> getProbeDesc(String name) {
        if(!probeDescMap.containsKey(name)) {
            MokeProbe<String, Number> mp = new MokeProbe<>(name);
            mp.configure();
            probeDescMap.put(name, mp.getPd());
        }
        return (ProbeDesc<String>) super.getProbeDesc(name);
    }
}
