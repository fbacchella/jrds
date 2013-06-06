package jrds.mockobjects;

import java.lang.reflect.InvocationTargetException;

import jrds.Probe;
import jrds.probe.IndexedProbe;

public class DummyProbeIndexed extends DummyProbe implements IndexedProbe {
	Class<? extends Probe<?,?>> originalProbe;

	public void configure(Class<? extends Probe<?,?>> originalProbe) throws InvocationTargetException {
		super.configure(originalProbe);
	}

	public String getIndexName() {
		return "EmptyIndex";
	}

	public String getLabel() {
		return "EmptyLabel";
	}

	public void setLabel(String label) {
	}
}
