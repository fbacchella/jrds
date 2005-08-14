/*
 * Created on 29 juil. 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package jrds;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

/**
 * @author bacchell
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class BackEndCommiter {
	static final private Logger logger = JrdsLogger.getLogger(BackEndCommiter.class);
	
	private static int syncPeriod = 30; //seconds
	static private BackEndCommiter instance = null;

	private final Timer syncTimer = new Timer(true);
	private final Collection backEndSet = Collections.synchronizedCollection( new HashSet());
	/**
	 * 
	 */
	private BackEndCommiter() {
		instance = this;
		createSyncTask(syncPeriod);
	}
	
	private void createSyncTask(int syncPeriod) {
		TimerTask syncTask = new TimerTask() {
			//Just to keep an active reference to the singloton
			BackEndCommiter linstance = instance;
			public void run() {
				synchronized(backEndSet) {
					for(Iterator i = backEndSet.iterator(); i.hasNext() ;) {
						Object o = i.next();
						((RrdCachedFileBackend) o).sync();
					}
				}
			}
		};
		syncTimer.schedule(syncTask, syncPeriod * 1000L, syncPeriod * 1000L);
	}
	
	static public BackEndCommiter getInstance() {
		if(instance == null) {
			new BackEndCommiter();
		}
		return instance;
	}
	
	public void addBackEnd(RrdCachedFileBackend be) {
		backEndSet.add(be);
	}
	
	public void removeBackEnd(RrdCachedFileBackend be) {
		backEndSet.remove(be);
	}
	
	/**
	 * @return Returns the syncPeriod.
	 */
	public static int getSyncPeriod() {
		return syncPeriod;
	}
	/**
	 * @param syncPeriod The syncPeriod to set.
	 */
	public static void setSyncPeriod(int syncPeriod) {
		BackEndCommiter.syncPeriod = syncPeriod;
	}
	
}
