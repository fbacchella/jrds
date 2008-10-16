package jrds;

import java.util.Map;

import jrds.probe.IndexedProbe;

public class DummyProbeIndexed extends DummyProbe implements IndexedProbe {
	Class originalProbe;

	public DummyProbeIndexed(Class originalProbe) {
		super(originalProbe);
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

	public String getIndexName() {
		return "EmptyIndex";
	}

	public String getLabel() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setLabel(String label) {
		// TODO Auto-generated method stub
		
	}

}
