package jrds.starter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

	//Synchronized because only startCollect or stopCollect can run at the same time
	public synchronized void  startCollect() {
		if(allStarters != null) {
			if(logger.isDebugEnabled())
				logger.debug("Starting " + allStarters.size() + " starters for "+ this.getLevel());
			for(Starter s: allStarters.values()) {
				//If collecte is stopped while we're starting, drop it
				if(level.getParent() !=null && ! level.getParent().isCollectRunning())
					break;
				try {
					logger.trace("Starting " + s);
					s.doStart();
				} catch (Exception e) {
					logger.error("Unable to start starter " + s.getKey());
				}
			}
		}
	}

	public synchronized void stopCollect() {
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

	public <StarterClass extends Starter> StarterClass find(Class<StarterClass> sc, StarterNode node) {
		Object key = sc;
		logger.trace(sc + " " + key);
		try {
			Method m = sc.getMethod("makeKey", StarterNode.class);
			logger.trace(m);
			key = m.invoke(null, node);
		} catch (SecurityException e) {
			logger.error(e,e);
		} catch (NoSuchMethodException e) {
			logger.error(e,e);
		} catch (IllegalArgumentException e) {
			logger.error(e,e);
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
			logger.error(e,e);
		}
		logger.trace(sc + " " + key);
		return find(sc, key);
	}

	public Starter find(Object key) {
		return find(Starter.class, key);
	}

	@SuppressWarnings("unchecked")
	public <StarterClass extends Starter> StarterClass find(Class<StarterClass> sc, Object key) {
		StarterClass s = null;
		if(allStarters != null && allStarters.containsKey(key)) {
			Starter stemp = allStarters.get(key);
			if(logger.isDebugEnabled())
				logger.debug("Found " + key + ": " + stemp);
			if(sc.isInstance(stemp)) {
				s = (StarterClass) stemp;
			}
			else {
				logger.error("Starter key error, got a " + stemp.getClass() + " expecting a " + sc);
				return null;
			}
		}
		else if(up != null)
			s = up.find(sc, key);
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