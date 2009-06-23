/*##########################################################################
 _##
 _##  $Id: NumProcesses.java 187 2006-01-18 19:08:14 +0100 (mer., 18 janv. 2006) fbacchella $
 _##
 _##########################################################################*/

package jrds.probe.snmp;

import jrds.RdsHost;

import org.apache.log4j.Logger;

/**
 * @author Fabrice Bacchella 
 * @version $Revision: 187 $,  $Date: 2006-01-18 19:08:14 +0100 (mer., 18 janv. 2006) $
 */
public class PartitionSpaceWindows extends PartitionSpace {
	static final private Logger logger = Logger.getLogger(PartitionSpaceWindows.class);
	static final private char separator=' ';
	
	/**
	 * A extention of the partitionSpace probe, used tom manager the long naming convention
	 * of disks in windows
	 * For example :
	 * HOST-RESOURCES-MIB::hrStorageDescr.2 = STRING: C:\ Label:Win2003  Serial Number 123abc
	 * But we only want c:\
	 * @param monitoredHost
	 * @param indexKey
	 */
	public PartitionSpaceWindows(RdsHost monitoredHost, String indexKey) {
		super(indexKey);
	}
	
	/**
	 *only compare with String found before " " 
	 * @see jrds.probe.snmp.RdsIndexedSnmpRrd#matchIndex(java.lang.String, java.lang.String)
	 */
	public boolean matchIndex(String key) {
		int nameIndex = key.indexOf(separator);
		
		if(logger.isDebugEnabled())
			logger.debug("index split: found separator=\""+ separator + "\" in \"" + key + "\" index=" + key);
		
		if (nameIndex != -1) {
			key = key.substring(0, nameIndex);
			logger.debug("index split: new name=\""+key+"\"");
		}
		return super.matchIndex(key);
	}
}
