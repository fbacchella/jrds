package jrds;

public abstract class Starter {
	public boolean start(RdsHost host) {
		return false;
	}
	public void stop(RdsHost host) {
	}
	public boolean start() {
		return false;
	}
	public void stop() {
	}
}
