package jrds;

import org.apache.log4j.Logger;


public abstract class Starter {
	static final private Logger logger = Logger.getLogger(Starter.class);

	private StartersSet level = null;
	private Object parent;	
	private boolean started = false;
	
	public void initialize(Object parent, StartersSet level) {
		this.level = level;
		this.parent = parent;
	}
	public void doStart() {
		started = start();
		if(! started)
			logger.error("Start " + this + " for " + parent + " failed");
			
	}
	public void doStop() {
		logger.trace("trying to stop starter " + this );
		if(started) {
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
	public Object getParent() {
		return parent;
	}
}
