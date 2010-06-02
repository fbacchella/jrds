package jrds.starter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import jrds.HostsList;

import org.apache.log4j.Logger;

public abstract class StarterNode implements StartersSet {

	static final private Logger logger = Logger.getLogger(StartersSet.class);
	private Map<Object, Starter> allStarters = null;

	private HostsList root = null;
	private volatile boolean started = false;
	private StarterNode parent = null;

	public StarterNode() {
		if (this instanceof HostsList) {
			root = (HostsList) this;
		}
	}

	public StarterNode(StarterNode parent) {
		setParent(parent);
	}

	public void setParent(StarterNode parent) {
		root = parent.root;
		this.parent = parent;
	}

	public boolean isCollectRunning() {
		if(parent != null && ! parent.isCollectRunning())
			return false;
		return started && ! Thread.currentThread().isInterrupted();
	}

	public boolean startCollect() {
		if(parent != null && ! parent.isCollectRunning())
			return false;
		if(allStarters != null) {
			if(logger.isDebugEnabled())
				logger.debug("Starting " + allStarters.size() + " starters for "+ this);
			for(Starter s: allStarters.values()) {
				//If collecte is stopped while we're starting, drop it
				if(parent !=null && ! parent.isCollectRunning())
					return false;
				try {
					logger.trace("Starting " + s);
					s.doStart();
				} catch (Exception e) {
					logger.error("Unable to start starter " + s.getKey());
				}
			}
		}
		started = true;
		return isCollectRunning();
	}

	public synchronized void stopCollect() {
		started = false;
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

	public Starter registerStarter(Starter s) {
		Object key = s.getKey();
		if(allStarters == null)
			allStarters = new HashMap<Object, Starter>(2);
		if(! allStarters.containsKey(key)) {
			s.initialize(this);
			allStarters.put(key, s);
			return s;
		}
		else {
			return allStarters.get(key);
		}
	}
	
	public <StarterClass extends Starter> StarterClass find(Class<StarterClass> sc) {
		Object key = null;
		logger.trace(sc + " " + key);
		try {
			Method m = sc.getMethod("makeKey", StarterNode.class);
			logger.trace(m);
			key = m.invoke(null, this);
		} catch (SecurityException e) {
			logger.error(e,e);
		} catch (NoSuchMethodException e) {
			//Not an error, the key is the the class
			key = sc;
		} catch (IllegalArgumentException e) {
			logger.error(e,e);
		} catch (IllegalAccessException e) {
			logger.error(e,e);
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
		else if(parent != null )
			s = parent.find(sc, key);
		return s;
	}

	public boolean isStarted(Object key) {
		boolean s = false;
		Starter st = find(key);
		if(st != null)
			s = st.isStarted();
		return s;
	}

	public HostsList getHostList() {
		return root;
	}

	/**
	 * @return the parent
	 */
	public StarterNode getParent() {
		return parent;
	}

	//Compatibily code
	public StartersSet getStarters() {
		return this;
	}

	public StarterNode getLevel() {
		return this;
	}

	public void setParent(StartersSet s) {
		setParent((StarterNode) s);
	}

	public Starter registerStarter(Starter s, StarterNode parent) {
		return registerStarter(s);
	};

	public <StarterClass extends Starter> StarterClass find(Class<StarterClass> sc, StarterNode nope) {
		return find(sc);
	}

}
