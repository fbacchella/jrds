package jrds;

import java.util.HashMap;
import java.util.Map;

public class StartersSet {
	final Map<Object, Starter> allStarters = new HashMap<Object, Starter>();
	StartersSet up = null;
	Object level = null;
	StartersSet(Object level) {
		this.level = level;
	}
	public void startCollect() {
		for(Starter s: allStarters.values()) {
			s.doStart();
		}
	}

	public void stopCollect() {
		for(Starter s: allStarters.values()) {
			s.doStop();
		}
	}

	public Starter registerStarter(Starter s, Object parent) {
		s.initialize(parent, this);
		Object key = s.getKey();
		if(! allStarters.containsKey(key)) {
			allStarters.put(key, s);
		}
		return find(key);
	}
	public Starter find(Object key) {
		Starter s = null;
		if(allStarters.containsKey(key))
			s = allStarters.get(key);
		else if(up != null)
			s = up.find(key);
		return s;
	}

	public boolean isStarted(Object key) {
		boolean s = false;
		Starter st = find(key);
		if(st != null)
			s = st.isStarted();
		return s;
	}
	
	public StartersSet getRoot() {
		StartersSet ss = null;
		if (up != null)
			ss = up.getRoot();
		else
			ss = this;
		return ss;
	}

	public void setParent(StartersSet s) {
		up = s;
	}
	public Object getLevel() {
		return level;
	}
}