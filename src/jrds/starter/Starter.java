package jrds.starter;

import org.apache.log4j.Logger;


public abstract class Starter {
	static final private Logger logger = Logger.getLogger(Starter.class);
	long uptime = Long.MAX_VALUE;

	private StartersSet level = null;
	private StarterNode parent;	
	private boolean started = false;

	public void initialize(StarterNode parent, StartersSet level) {
		this.level = level;
		this.parent = parent;
	}
	public final void doStart() {
		logger.trace("Starting " + this);
		started = start();
		if(! started)
			logger.error("Start " + this + " for " + parent + " failed");

	}
	public final void doStop() {
		if(started) {
			logger.trace("Stopping " + this);
			stop();
			started = false;
		}
	}
	public boolean start() {
		return true;
	}
	public void stop() {
	}
	public Object getKey() {
		return this;
	}
	public StartersSet getLevel() {
		return level;
	}
	public boolean isStarted() {
		return started;
	}
	public StarterNode getParent() {
		return parent;
	}
	@Override
	public String toString() {
		return getKey().toString();
	}

	/**
	 * Return uptime for the starter, default value is max value
	 * @return
	 */
	public long getUptime() {
		return uptime;
	}

	public void setUptime(long uptime) {
		this.uptime = uptime;
	}

	/**
	 * Register a node, keep the last one for a key
	 * @param node
	 * @return
	 */
	public Starter register(StarterNode node) {
		node.getStarters().registerStarter(this, node);
		return this;
	}

	static public Object makeKey(StarterNode node) {
		throw new UnsupportedOperationException("Make key not defined"); 
	}
}
