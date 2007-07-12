package jrds;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;


public abstract class Starter {
	static final private Logger logger = Logger.getLogger(Starter.class);
	long uptime = Long.MAX_VALUE;
	
	public static class Resolver extends Starter {
		String hostname = "";
		InetAddress address = null;
		
		public Resolver(String hostname) {
			super();
			this.hostname = hostname;
		}

		public Resolver() {
			super();
		}

		public void setHostname(String hostname) {
			this.hostname = hostname;
		}

		@Override
		public boolean start() {
			boolean started = false;
			try {
				address = InetAddress.getByName(hostname);
				started = true;
			} catch (UnknownHostException e) {
				logger.error("Host name " + hostname + " can't be solved");
			}
			return started;
		}
		@Override
		public void stop() {
			address = null;
		}

		public InetAddress getInetAddress() {
			return address;
		}
		@Override
		public Object getKey() {
			return "resolver:" + hostname;
		}
		
		public static Object buildKey(String hostname) {
			return "resolver:" + hostname;
		}

		/* (non-Javadoc)
		 * @see jrds.Starter#register(jrds.StarterNode)
		 */
		@Override
		public Starter register(StarterNode node) {
			StartersSet ss = node.getStarters();
			if(ss.find(getKey()) == null)
				super.register(node);
			return ss.find(getKey());
		}
	}

	private StartersSet level = null;
	private Object parent;	
	private boolean started = false;
	
	public void initialize(Object parent, StartersSet level) {
		this.level = level;
		this.parent = parent;
	}
	public void doStart() {
		logger.trace("Starting " + this);
		started = start();
		if(! started)
			logger.error("Start " + this + " for " + parent + " failed");
			
	}
	public void doStop() {
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
	public Object getParent() {
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
	
	public Starter register(StarterNode node) {
		node.getStarters().registerStarter(this, node);
		return this;
	}
}
