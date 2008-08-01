package jrds.test;

import java.util.Map;

import jrds.Probe;

public class DummyProbe extends Probe {
	Class<? extends Probe> originalProbe;

	public DummyProbe(Class<? extends Probe> originalProbe) {
		this.originalProbe = originalProbe;
	}

	@Override
	public Map getNewSampleValues() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSourceType() {
		// TODO Auto-generated method stub
		return null;
	}

}
