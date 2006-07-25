/*
 * Created on 20 avr. 2005
 *
 * TODO 
 */
package jrds.snmp;

import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;

/**
 * @author bacchell
 *
 * TODO 
 */
public class SyncResponseListener implements ResponseListener {

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
		response = event;
		//the synchronized is here to own the lock monitor
		synchronized(lock) {
			lock.notify();
		}
	}
	public ResponseEvent getResponse() {
		return response;
	}
	
}
