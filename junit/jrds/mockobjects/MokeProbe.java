package jrds.mockobjects;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jrds.HostInfo;
import jrds.starter.HostStarter;
import jrds.Probe;
import jrds.ProbeDesc;

import org.rrd4j.DsType;

public class MokeProbe<A,B> extends Probe<A,B> {

	Class<? extends Probe<?,?>> originalProbe;
	String probeType = "DummyProbe";
	List<?> args;
	Map<A, B> values = Collections.emptyMap();
	
	public MokeProbe(String probeType) {
		this.probeType = probeType;
        setStep(300);
	}

	public MokeProbe(ProbeDesc pd) {
	    probeType = pd.getName();
		setPd(pd);
        setStep(300);
	}

	public MokeProbe() {
		configure();
		setStep(300);
	}

	public void configure(Class<? extends Probe<?,?>> originalProbe) {
		this.originalProbe = originalProbe;
		configure();
	}

	@SuppressWarnings("unchecked")
    public void configure() {
		ProbeDesc pd = getPd();
		if(pd == null) {
			pd = new ProbeDesc();
			pd.setName(probeType);
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
			setPd(pd);
		}
		if(pd.getProbeClass() == null)
            try {
                pd.setProbeClass((Class<? extends Probe<?,?>>) this.getClass());
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
