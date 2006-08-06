package jrds;

import java.util.HashMap;
import java.util.Map;

public class StartersSet {
	private Map<Object, Starter> allStarters = null;
	private StartersSet up = null;
	private Object level = null;

	StartersSet(Object level) {
		this.level = level;
	}
	public void startCollect() {
		if(allStarters != null)
			for(Starter s: allStarters.values()) {
				s.doStart();
			}
	}

	public void stopCollect() {
		if(allStarters != null)
			for(Starter s: allStarters.values()) {
				s.doStop();
			}
	}

	public Starter registerStarter(Starter s, Object parent) {
		if(allStarters == null)
			allStarters = new HashMap<Object, Starter>(2);
		s.initialize(parent, this);
		Object key = s.getKey();
		if(! allStarters.containsKey(key)) {
			allStarters.put(key, s);
		}
		return find(key);
	}
	
	public Starter find(Object key) {
		Starter s = null;
		if(allStarters != null && allStarters.containsKey(key))
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