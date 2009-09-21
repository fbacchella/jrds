package jrds.starter;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class StartersSet {
	static final private Logger logger = Logger.getLogger(StartersSet.class);
	private Map<Object, Starter> allStarters = null;
	private StartersSet up = null;
	private StarterNode level = null;

	public StartersSet(StarterNode level) {
		this.level = level;
	}

	public void startCollect() {
		if(allStarters != null)
			for(Starter s: allStarters.values()) {
				try {
					logger.trace("Starting " + s);
					s.doStart();
				} catch (Exception e) {
					logger.error("Unable to start starter " + s.getKey());
				}
			}
	}

	public void stopCollect() {
		if(allStarters != null)
			for(Starter s: allStarters.values()) {
				try {
					logger.trace("stopping " + s);
					s.doStop();
				} catch (Exception e) {
					logger.error("Unable to stop timer " + s.getKey());
				}
			}
	}

	public Starter registerStarter(Starter s, StarterNode parent) {
		Object key = s.getKey();
		if(allStarters == null)
			allStarters = new LinkedHashMap<Object, Starter>(2);
		if(! allStarters.containsKey(key)) {
			s.initialize(parent, this);
			allStarters.put(key, s);
			return s;
		}
		else {
			return allStarters.get(key);
		}
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
	public StarterNode getLevel() {
		return level;
	}

	/**
	 * @return the up
	 */
	public StartersSet getUp() {
		return up;
	}
}