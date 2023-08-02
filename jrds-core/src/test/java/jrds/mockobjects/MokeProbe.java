package jrds.mockobjects;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.rrd4j.DsType;

import jrds.HostInfo;
import jrds.Probe;
import jrds.ProbeDesc;
import jrds.starter.HostStarter;

public class MokeProbe<A, B> extends Probe<A, B> {

    public static class SelfDescMokeProbe<A, B> extends MokeProbe<A, B> {
        public SelfDescMokeProbe() {
            configure();
        }
    }

    Class<? extends Probe<?, ?>> originalProbe;
    String probeType = "DummyProbe";
    List<?> args;
    Map<A, B> values = Collections.emptyMap();

    public MokeProbe(String probeType) {
        this.probeType = probeType;
        setStep(300);
    }

    public MokeProbe(ProbeDesc<A> pd) {
        probeType = pd.getName();
        setPd(pd);
        setStep(300);
    }

    public MokeProbe() {
        setStep(300);
    }

    public void configure(Class<? extends Probe<?, ?>> originalProbe) {
        this.originalProbe = originalProbe;
        configure();
    }

    @SuppressWarnings("unchecked")
    public void configure() {
        ProbeDesc<A> pd = getPd();
        if(pd == null) {
            pd = new ProbeDesc<>();
            pd.setName(probeType);
            pd.setProbeName("dummyprobe");
            pd.add(ProbeDesc.getDataSourceBuilder("ds0", DsType.COUNTER).setCollectKey("/jrdsstats/stat[@key='a']/@value"));
            pd.add(ProbeDesc.getDataSourceBuilder("ds1", DsType.COUNTER).setCollectKey("/jrdsstats/stat[@key='b']/@value"));
            pd.add(ProbeDesc.getDataSourceBuilder("ds2", DsType.COUNTER));
            setPd(pd);
        }
        if(pd.getProbeClass() == null)
            try {
                pd.setProbeClass((Class<? extends Probe<A, B>>) this.getClass());
            } catch (InvocationTargetException e) {
                throw new RuntimeException("Can't build moke probe", e);
            }
        if(getHost() == null) {
            HostInfo host = new HostInfo("DummyHost");
            host.setHostDir(new File("tmp"));
            setHost(new HostStarter(host));
        }
    }

    public void injectValues(Map<A, B> values) {
        this.values = values;
    }

    @Override
    public Map<A, B> getNewSampleValues() {
        Map<A, B> retValues = values;
        values = Collections.emptyMap();
        return retValues;
    }

    @Override
    public String getSourceType() {
        return "MokeSourceType";
    }

    @Override
    public String getName() {
        return probeType;
    }

    @Override
    public Date getLastUpdate() {
        return new Date();
    }

    /**
     * @return the args
     */
    public List<?> getArgs() {
        return args;
    }

    /**
     * @param args the args to set
     */
    public void setArgs(List<?> args) {
        this.args = args;
    }

}
