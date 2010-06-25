package jrds.mockobjects;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rrd4j.DsType;

import jrds.Probe;
import jrds.ProbeDesc;
import jrds.RdsHost;

public class MokeProbe<A,B> extends Probe<A,B> {

	Class<? extends Probe<?,?>> originalProbe;
	String probeType = "DummyProbe";
	ProbeDesc pd = null;
	List<?> args;
	
	public MokeProbe(String probeType) {
		this.probeType = probeType;
	}

	public MokeProbe(ProbeDesc pd) {
		this.pd = pd;
	}

	public MokeProbe() {
		configure();
	}

	public void configure(Class<? extends Probe<?,?>> originalProbe) {
		this.originalProbe = originalProbe;
		configure();
	}

	public void configure() {
		if(pd == null) {
			ProbeDesc pd = new ProbeDesc();
			pd.setName(probeType);
			pd.setProbeName("dummyprobe");
			setPd(pd);
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
		}
		if(getHost() == null) {
			RdsHost host = new RdsHost();
			host.setName("DummyHost");
			host.setHostDir(new File("tmp"));
			setHost(host);
		}
	}

	@Override
	public Map<A, B> getNewSampleValues() {
		return Collections.emptyMap();
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
