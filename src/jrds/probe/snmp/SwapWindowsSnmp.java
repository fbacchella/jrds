/*##########################################################################
 _##
 _##  $Id: CiscoCpu.java 186 2006-01-18 18:06:48 +0100 (mer., 18 janv. 2006) fbacchella $
 _##
 _##########################################################################*/

package jrds.probe.snmp;

import jrds.RdsHost;

/**
 * @author Fabien SEISEN
 * @version $Revision: 187 $,  $Date: 2006-01-18 19:08:14 +0100 (mer., 18 janv. 2006) $
 */
public class SwapWindowsSnmp extends PartitionSpaceWindows {
	public SwapWindowsSnmp(RdsHost monitoredHost) {
		super(monitoredHost, "Virtual Memory");
		getPd().setGraphClasses(new Object[] {"swapwindowsgraph"});		
	}
}