package jrds.starter;

import org.apache.log4j.Logger;


public abstract class Starter {
	static final private Logger logger = Logger.getLogger(Starter.class);
	long uptime = Long.MAX_VALUE;

	private StarterNode level;	
	volatile private boolean started = false;

	/**
	 * This method is called when the started is really registred<p/>
	 * It can be overriden to contains delayed initialization but it must begin with a call to super.initialize(parent)
	 * @param parent
	 * @param level
	 */
	public void initialize(StarterNode level) {
		this.level = level;
	}

	/**
	 * @param level
	 * @param none
	 * @deprecated
	 * The second argument is ignored, just remove it
	 */
	@SuppressWarnings("deprecation")
	public void initialize(jrds.starter.StarterNode level ,jrds.starter.StartersSet none) {
		this.level = level;
	}

	public final void doStart() {
		if(logger.isTraceEnabled())
			logger.trace("Starting " + getKey() + "@" + hashCode() +  " for " + level);
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
		return getClass();
	}

	public StarterNode getLevel() {
		return level;
	}

	public boolean isStarted() {
		return started;
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
		logger.trace("Registering " + this);
		return node.registerStarter(this);
	}

	/**
	 * @deprecated
	 * Use getLevel instead.
	 * @return the StarterNode for this starter
	 */
	@Deprecated
	public StartersSet getParent() {
		return level;
	}

	@Deprecated
	/**
	 * @deprecated
	 * Use a StarterNode type argument instead.
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public final Starter register(StartersSet node) {
		return register((StarterNode)node);
	}

}
