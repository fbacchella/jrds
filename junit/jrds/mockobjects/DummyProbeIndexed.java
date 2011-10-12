package jrds.mockobjects;

import jrds.objects.probe.IndexedProbe;
import jrds.objects.probe.Probe;

public class DummyProbeIndexed extends DummyProbe implements IndexedProbe {
	Class<? extends Probe<?,?>> originalProbe;

	public void configure(Class<? extends Probe<?,?>> originalProbe) {
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
