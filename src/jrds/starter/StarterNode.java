package jrds.starter;

import org.apache.log4j.Logger;

import jrds.HostsList;

public abstract class StarterNode {
	static private final Logger logger = Logger.getLogger(StarterNode.class);
	
	private HostsList root = null;
	private volatile boolean started = false;
	private StarterNode parent = null;
	private final StartersSet starters = new StartersSet(this);

	public StarterNode() {
		if (this instanceof HostsList) {
			root = (HostsList) this;
		}
	}
	
	public StarterNode(StarterNode parent) {
		setParent(parent);
	}
	
	public void setParent(StarterNode parent) {
		starters.setParent(parent.getStarters());
		root = parent.root;
		this.parent = parent;
	}
	
	public boolean isCollectRunning() {
		if(parent != null && ! parent.isCollectRunning())
			return false;
		return started && ! Thread.currentThread().isInterrupted();
	}

	public StartersSet getStarters() {
		return starters;
	}
	
	public boolean startCollect() {
		if(parent != null && ! parent.isCollectRunning())
			return false;
		if(starters != null)
			starters.startCollect();
		started = true;
		return isCollectRunning();
	}

	public void stopCollect() {
		if(starters != null)
			starters.stopCollect();
		started = false;
		return;
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

}
	