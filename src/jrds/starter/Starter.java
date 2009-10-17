package jrds.starter;

import org.apache.log4j.Logger;


public abstract class Starter {
	static final private Logger logger = Logger.getLogger(Starter.class);
	long uptime = Long.MAX_VALUE;

	private StartersSet level = null;
	private StarterNode parent;	
	volatile private boolean started = false;

	/**
	 * This method is called when the started is really registred<p/>
	 * It can be overriden to contains delayed initialization but it must begin with a call to super.initialize(parent, level)
	 * @param parent
	 * @param level
	 */
	public void initialize(StarterNode parent, StartersSet level) {
		this.level = level;
		this.parent = parent;
	}

	public final void doStart() {
		if(logger.isTraceEnabled())
			logger.trace("Starting " + getKey() + "@" + hashCode() +  " for " + parent);
		started = start();
	}
	
	public final void doStop() {
		if(started) {
			logger.trace("Stopping " + this);
			started = false;
			stop();
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
	public final Starter register(StarterNode node) {
		return node.getStarters().registerStarter(this, node);
	}
}
