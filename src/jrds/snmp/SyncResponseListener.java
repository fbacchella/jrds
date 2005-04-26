/*
 * Created on 20 avr. 2005
 *
 * TODO 
 */
package jrds.snmp;

import jrds.JrdsLogger;

import org.apache.log4j.Logger;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;

/**
 * @author bacchell
 *
 * TODO 
 */
public class SyncResponseListener implements ResponseListener {
	static private final Logger logger = JrdsLogger.getLogger(SnmpRequester.class);
	
	private ResponseEvent response = null;
	private Object lock;
	
	public SyncResponseListener(Object lock) {
		this.lock = lock;
	}
	/**
	 * onResponse
	 *
	 * @param event ResponseEvent
	 */
	public void onResponse(ResponseEvent event) {
		synchronized(lock) {
			response = event;
			lock.notify();
		}
	}
	public ResponseEvent getResponse() {
		return response;
	}
	
}
