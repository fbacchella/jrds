package jrds;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

class StartersSet {
	final Set<Starter> allStarters = new LinkedHashSet<Starter>();
	final Map<Starter, Boolean> started = new HashMap<Starter, Boolean>();
	public void startCollect(RdsHost host) {
		for(Starter s: allStarters) {
			started.put(s, s.start(host));
		}
	}

	public void startCollect() {
		for(Starter s: allStarters) {
			started.put(s, s.start());
		}
	}

	public void stopCollect(RdsHost host) {
		for(Starter s: allStarters) {
			if(started.get(s)) {
				s.stop(host);
				started.put(s, false);
			}
		}
	}

	public void stopCollect() {
		for(Starter s: allStarters) {
			if(started.get(s)) {
				s.stop();
				started.put(s, false);
			}
		}
	}


	public boolean isStarted(Starter s) {
		return started.get(s);
	}

	public void register(Starter s) {
		if(! allStarters.contains(s))
			allStarters.add(s);
	}
}
