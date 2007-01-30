/*##########################################################################
_##
_##  $Id$
_##
_##########################################################################*/

package jrds;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This class manage a thread that runs in the background and is used to commit to disk the RRD datas modifications.
 *
 * @author Fabrice Bacchella
 * @version $Revision$
 */
public class BackEndCommiter {
	private int syncPeriod = 30; //seconds
	static private BackEndCommiter instance = null;

	private final Timer syncTimer = new Timer(true);
	private final Collection<RrdCachedFileBackend> backEndSet = Collections.synchronizedCollection( new HashSet<RrdCachedFileBackend>());
	/**
	 * 
	 */
	private BackEndCommiter() {
		createSyncTask(syncPeriod);
	}
	
	private void createSyncTask(int syncPeriod) {
		TimerTask syncTask = new TimerTask() {
			public void run() {
				synchronized(backEndSet) {
					for(RrdCachedFileBackend o: backEndSet)
						o.sync();
				}
			}
		};
		syncTimer.schedule(syncTask, syncPeriod * 1000L, syncPeriod * 1000L);
	}
	
	static public BackEndCommiter getInstance() {
		if(instance == null) {
			instance = new BackEndCommiter();
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
		return instance.syncPeriod;
	}
	/**
	 * @param syncPeriod The syncPeriod to set.
	 */
	public static void setSyncPeriod(int syncPeriod) {
		instance.syncPeriod = syncPeriod;
	}

	public static synchronized void commit() {
		if(instance != null) {
			Collection<RrdCachedFileBackend> bes = instance.backEndSet;
			synchronized(bes) {
				for(RrdCachedFileBackend o: bes)
					o.sync();
			}
		}
	}
}
