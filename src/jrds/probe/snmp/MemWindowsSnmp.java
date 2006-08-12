/*##########################################################################
 _##
 _##  $Id: NumProcesses.java 187 2006-01-18 19:08:14 +0100 (mer., 18 janv. 2006) fbacchella $
 _##
 _##########################################################################*/

package jrds.probe.snmp;

import jrds.RdsHost;

/**
 * @author Fabien SEISEN
 * @version $Revision: 187 $,  $Date: 2006-01-18 19:08:14 +0100 (mer., 18 janv. 2006) $
 */
public class MemWindowsSnmp extends PartitionSpaceWindows {

	public MemWindowsSnmp(RdsHost monitoredHost) {
		super(monitoredHost, "Physical Memory");
		getPd().setGraphClasses(new Object[] {"memwindowsgraph"});
	}
}